package io.aurigraph.v11.token.secondary;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Secondary Token Versioning Service
 * Manages version lifecycle: creation, activation, VVB approval, archival
 * @version 12.0.0
 */
@ApplicationScoped
@Slf4j
public class SecondaryTokenVersioningService {

    @Inject SecondaryTokenVersionRepository versionRepository;
    @Inject SecondaryTokenVersionStateMachine stateMachine;
    @Inject Event<VersionCreatedEvent> versionCreatedEvent;
    @Inject Event<VersionActivatedEvent> versionActivatedEvent;
    @Inject Event<VersionRejectedEvent> versionRejectedEvent;
    @Inject Event<VersionArchivedEvent> versionArchivedEvent;

    @Transactional
    public Uni<SecondaryTokenVersion> createVersion(UUID tokenId, String content, boolean vvbRequired, UUID previousVersionId) {
        return Uni.createFrom().item(() -> {
            SecondaryTokenVersion v = new SecondaryTokenVersion();
            v.setId(UUID.randomUUID());
            v.setSecondaryTokenId(tokenId);
            v.setContent(content);
            v.setVvbRequired(vvbRequired);
            v.setPreviousVersionId(previousVersionId);
            v.setCreatedAt(LocalDateTime.now());
            v.setUpdatedAt(LocalDateTime.now());
            v.setStatus(SecondaryTokenVersionStatus.CREATED);
            v.setVersionNumber(SecondaryTokenVersion.getNextVersionNumber(tokenId));
            v.validate();
            versionRepository.persist(v);
            fireVersionCreatedEvent(v);
            return v;
        });
    }

    @Transactional
    public Uni<SecondaryTokenVersion> activateVersion(UUID versionId) {
        return Uni.createFrom().item(() -> {
            SecondaryTokenVersion v = SecondaryTokenVersion.findById(versionId);
            if (v == null) throw new IllegalArgumentException("Version not found");
            v.setMerkleHash(generateMerkleHash(v.getContent()));
            stateMachine.transitionState(v, SecondaryTokenVersionStatus.ACTIVE);
            v.setUpdatedAt(LocalDateTime.now());
            versionRepository.persist(v);
            fireVersionActivatedEvent(v);
            return v;
        });
    }

    @Transactional
    public Uni<SecondaryTokenVersion> submitForVVBApproval(UUID versionId) {
        return Uni.createFrom().item(() -> {
            SecondaryTokenVersion v = SecondaryTokenVersion.findById(versionId);
            if (v == null) throw new IllegalArgumentException("Version not found");
            if (!Boolean.TRUE.equals(v.getVvbRequired())) throw new IllegalStateException("VVB not required");
            stateMachine.transitionState(v, SecondaryTokenVersionStatus.PENDING_VVB);
            v.setUpdatedAt(LocalDateTime.now());
            versionRepository.persist(v);
            return v;
        });
    }

    @Transactional
    public Uni<SecondaryTokenVersion> approveWithVVB(UUID versionId, String approver) {
        return Uni.createFrom().item(() -> {
            SecondaryTokenVersion v = SecondaryTokenVersion.findById(versionId);
            if (v == null) throw new IllegalArgumentException("Version not found");
            v.setVvbApprovedAt(LocalDateTime.now());
            v.setVvbApprovedBy(approver);
            return v;
        }).flatMap(v -> activateVersion(v.getId()));
    }

    @Transactional
    public Uni<SecondaryTokenVersion> rejectVersion(UUID versionId, String reason, String rejector) {
        return Uni.createFrom().item(() -> {
            SecondaryTokenVersion v = SecondaryTokenVersion.findById(versionId);
            if (v == null) throw new IllegalArgumentException("Version not found");
            v.setRejectionReason(reason);
            stateMachine.transitionState(v, SecondaryTokenVersionStatus.REJECTED);
            v.setUpdatedAt(LocalDateTime.now());
            versionRepository.persist(v);
            fireVersionRejectedEvent(v, rejector);
            return v;
        });
    }

    public Uni<SecondaryTokenVersion> getActiveVersion(UUID tokenId) {
        return Uni.createFrom().item(() -> SecondaryTokenVersion.findActiveVersion(tokenId));
    }

    public Uni<List<SecondaryTokenVersion>> getVersionHistory(UUID tokenId) {
        return Uni.createFrom().item(() -> SecondaryTokenVersion.findBySecondaryTokenId(tokenId));
    }

    public Uni<SecondaryTokenVersion> getVersion(UUID versionId) {
        return Uni.createFrom().item(() -> SecondaryTokenVersion.findById(versionId));
    }

    public String generateMerkleHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(content.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Transactional
    public Uni<SecondaryTokenVersion> archiveVersion(UUID versionId) {
        return Uni.createFrom().item(() -> {
            SecondaryTokenVersion v = SecondaryTokenVersion.findById(versionId);
            if (v == null) throw new IllegalArgumentException("Version not found");
            String oldStatus = v.getStatus().name();
            stateMachine.transitionState(v, SecondaryTokenVersionStatus.ARCHIVED);
            v.setUpdatedAt(LocalDateTime.now());
            versionRepository.persist(v);
            fireVersionArchivedEvent(v, oldStatus);
            return v;
        });
    }

    private void fireVersionCreatedEvent(SecondaryTokenVersion v) {
        versionCreatedEvent.fire(new VersionCreatedEvent(v.getId(), v.getSecondaryTokenId(), v.getVersionNumber(), v.getContent(), v.getCreatedAt()));
    }

    private void fireVersionActivatedEvent(SecondaryTokenVersion v) {
        versionActivatedEvent.fire(new VersionActivatedEvent(v.getId(), v.getSecondaryTokenId(), v.getVersionNumber(), v.getMerkleHash(), LocalDateTime.now()));
    }

    private void fireVersionRejectedEvent(SecondaryTokenVersion v, String rejector) {
        versionRejectedEvent.fire(new VersionRejectedEvent(v.getId(), v.getSecondaryTokenId(), v.getVersionNumber(), v.getRejectionReason(), rejector, LocalDateTime.now()));
    }

    private void fireVersionArchivedEvent(SecondaryTokenVersion v, String oldStatus) {
        versionArchivedEvent.fire(new VersionArchivedEvent(v.getId(), v.getSecondaryTokenId(), v.getVersionNumber(), oldStatus, v.getArchivedAt()));
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }
}
