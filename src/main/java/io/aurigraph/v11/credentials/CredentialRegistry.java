package io.aurigraph.v11.credentials;

import io.aurigraph.v11.credentials.models.Credential;
import io.aurigraph.v11.merkle.MerkleProof;
import io.aurigraph.v11.merkle.MerkleTreeRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Credential Registry with Merkle Tree Support
 *
 * Registry for managing verifiable credentials with cryptographic
 * verification and proof generation/verification.
 *
 * Features:
 * - Credential storage and retrieval
 * - Credential status management
 * - Expiration tracking
 * - Revocation management
 * - Merkle tree cryptographic verification
 * - Proof generation and verification
 * - Issuer verification
 * - Audit trail tracking
 *
 * @version 11.5.0
 * @since 2025-10-30 - AV11-457: CredentialRegistry Merkle Tree
 */
@ApplicationScoped
public class CredentialRegistry extends MerkleTreeRegistry<Credential> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialRegistry.class);

    @Override
    protected String serializeValue(Credential credential) {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s",
            credential.getCredentialId(),
            credential.getUserId(),
            credential.getCredentialType(),
            credential.getIssuer(),
            credential.getStatus(),
            credential.getIssuedAt(),
            credential.getExpiresAt(),
            credential.getVerificationHash()
        );
    }

    /**
     * Register a new credential
     */
    public Uni<Credential> registerCredential(Credential credential) {
        return Uni.createFrom().item(() -> {
            LOGGER.info("Registering credential: {} (type: {}, issuer: {})",
                credential.getCredentialId(), credential.getCredentialType(), credential.getIssuer());

            if (credential.getIssuedAt() == null) {
                credential.setIssuedAt(Instant.now());
            }

            return credential;
        }).flatMap(c -> add(c.getCredentialId(), c).map(success -> c));
    }

    /**
     * Get credential by ID
     */
    public Uni<Credential> getCredential(String credentialId) {
        return get(credentialId).onItem().ifNull().failWith(() ->
            new CredentialNotFoundException("Credential not found: " + credentialId));
    }

    /**
     * Revoke a credential
     */
    public Uni<Credential> revokeCredential(String credentialId, String revocationReason) {
        return getCredential(credentialId).map(credential -> {
            credential.setStatus(Credential.CredentialStatus.REVOKED);
            credential.setMetadata(String.format("Revoked: %s | Reason: %s", Instant.now(), revocationReason));
            credential.setRevisionNumber(credential.getRevisionNumber() + 1);
            registry.put(credentialId, credential);
            LOGGER.info("Revoked credential: {} | Reason: {}", credentialId, revocationReason);
            return credential;
        });
    }

    /**
     * Verify credential validity
     */
    public Uni<CredentialValidationResult> verifyCredential(String credentialId) {
        return getCredential(credentialId).map(credential -> {
            boolean isValid = credential.isValid();
            String reason = null;

            if (credential.getStatus() == Credential.CredentialStatus.REVOKED) {
                reason = "Credential has been revoked";
            } else if (credential.isExpired()) {
                reason = "Credential has expired";
            } else if (credential.getStatus() == Credential.CredentialStatus.SUSPENDED) {
                reason = "Credential is suspended";
            }

            return new CredentialValidationResult(
                isValid,
                credential.getStatus().toString(),
                credential.isExpired(),
                isValid ? "Credential is valid" : reason
            );
        });
    }

    /**
     * Get credentials by user
     */
    public Uni<List<Credential>> getCredentialsByUser(String userId) {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                return registry.values().stream()
                    .filter(c -> c.getUserId().equals(userId))
                    .collect(Collectors.toList());
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get credentials by issuer
     */
    public Uni<List<Credential>> getCredentialsByIssuer(String issuer) {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                return registry.values().stream()
                    .filter(c -> c.getIssuer().equals(issuer))
                    .collect(Collectors.toList());
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get credentials by type
     */
    public Uni<List<Credential>> getCredentialsByType(String credentialType) {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                return registry.values().stream()
                    .filter(c -> c.getCredentialType().equals(credentialType))
                    .collect(Collectors.toList());
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get active credentials for user
     */
    public Uni<List<Credential>> getActiveCredentialsForUser(String userId) {
        return getCredentialsByUser(userId).map(credentials ->
            credentials.stream()
                .filter(Credential::isValid)
                .collect(Collectors.toList())
        );
    }

    /**
     * Count credentials by status
     */
    public Uni<CredentialCountByStatus> countCredentialsByStatus() {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                Map<Credential.CredentialStatus, Long> counts = registry.values().stream()
                    .collect(Collectors.groupingBy(Credential::getStatus, Collectors.counting()));

                return new CredentialCountByStatus(
                    counts.getOrDefault(Credential.CredentialStatus.ACTIVE, 0L),
                    counts.getOrDefault(Credential.CredentialStatus.EXPIRED, 0L),
                    counts.getOrDefault(Credential.CredentialStatus.REVOKED, 0L),
                    counts.getOrDefault(Credential.CredentialStatus.SUSPENDED, 0L),
                    counts.getOrDefault(Credential.CredentialStatus.ARCHIVED, 0L)
                );
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Generate Merkle proof for a credential
     */
    public Uni<MerkleProof.ProofData> getProof(String credentialId) {
        return generateProof(credentialId).map(MerkleProof::toProofData);
    }

    /**
     * Verify a Merkle proof
     */
    public Uni<CredentialVerificationResponse> verifyMerkleProof(MerkleProof.ProofData proofData) {
        return verifyProof(proofData.toMerkleProof()).map(valid ->
            new CredentialVerificationResponse(valid, valid ? "Merkle proof verified" : "Invalid Merkle proof")
        );
    }

    /**
     * Get Merkle root hash for audit
     */
    public Uni<CredentialRootHashResponse> getMerkleRootHash() {
        return getRootHash().flatMap(rootHash ->
            getTreeStats().map(stats -> new CredentialRootHashResponse(
                rootHash,
                Instant.now(),
                stats.getEntryCount(),
                stats.getTreeHeight()
            ))
        );
    }

    /**
     * Get registry statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCredentials", registry.size());

        long activeCount = registry.values().stream()
            .filter(Credential::isValid)
            .count();
        stats.put("activeCredentials", activeCount);

        long expiredCount = registry.values().stream()
            .filter(c -> c.getStatus() == Credential.CredentialStatus.EXPIRED)
            .count();
        stats.put("expiredCredentials", expiredCount);

        long revokedCount = registry.values().stream()
            .filter(c -> c.getStatus() == Credential.CredentialStatus.REVOKED)
            .count();
        stats.put("revokedCredentials", revokedCount);

        return stats;
    }

    // Custom Exceptions
    public static class CredentialNotFoundException extends RuntimeException {
        public CredentialNotFoundException(String message) {
            super(message);
        }
    }

    // Response Classes
    public static class CredentialValidationResult {
        private final boolean valid;
        private final String status;
        private final boolean expired;
        private final String message;

        public CredentialValidationResult(boolean valid, String status, boolean expired, String message) {
            this.valid = valid;
            this.status = status;
            this.expired = expired;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getStatus() { return status; }
        public boolean isExpired() { return expired; }
        public String getMessage() { return message; }
    }

    public static class CredentialVerificationResponse {
        private final boolean valid;
        private final String message;

        public CredentialVerificationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    public static class CredentialRootHashResponse {
        private final String rootHash;
        private final Instant timestamp;
        private final int entryCount;
        private final int treeHeight;

        public CredentialRootHashResponse(String rootHash, Instant timestamp, int entryCount, int treeHeight) {
            this.rootHash = rootHash;
            this.timestamp = timestamp;
            this.entryCount = entryCount;
            this.treeHeight = treeHeight;
        }

        public String getRootHash() { return rootHash; }
        public Instant getTimestamp() { return timestamp; }
        public int getEntryCount() { return entryCount; }
        public int getTreeHeight() { return treeHeight; }
    }

    public static class CredentialCountByStatus {
        private final long active;
        private final long expired;
        private final long revoked;
        private final long suspended;
        private final long archived;

        public CredentialCountByStatus(long active, long expired, long revoked, long suspended, long archived) {
            this.active = active;
            this.expired = expired;
            this.revoked = revoked;
            this.suspended = suspended;
            this.archived = archived;
        }

        public long getActive() { return active; }
        public long getExpired() { return expired; }
        public long getRevoked() { return revoked; }
        public long getSuspended() { return suspended; }
        public long getArchived() { return archived; }
    }
}
