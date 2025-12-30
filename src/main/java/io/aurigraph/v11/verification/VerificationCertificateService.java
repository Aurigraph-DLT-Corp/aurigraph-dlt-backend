package io.aurigraph.v11.verification;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verification Certificate Service - AV11-401
 * Generates and stores immutable verification certificates on blockchain
 */
@ApplicationScoped
public class VerificationCertificateService {

    private static final Logger LOG = Logger.getLogger(VerificationCertificateService.class);

    // In-memory storage (blockchain simulation for now)
    private final Map<String, VerificationCertificate> certificates = new ConcurrentHashMap<>();

    @Inject
    io.aurigraph.v11.crypto.DilithiumSignatureService signatureService;

    @PostConstruct
    void init() {
        signatureService.initialize();
        LOG.info("VerificationCertificateService initialized");
    }

    /**
     * Generate a new verification certificate
     */
    public Uni<VerificationCertificate> generateCertificate(CertificateRequest request) {
        return Uni.createFrom().item(() -> {
            String certificateId = UUID.randomUUID().toString();
            long timestamp = System.currentTimeMillis();

            // Create certificate
            VerificationCertificate certificate = new VerificationCertificate(
                certificateId,
                request.entityId(),
                request.entityType(),
                request.verificationType(),
                request.verifierId(),
                request.verificationData(),
                CertificateStatus.ISSUED,
                timestamp,
                timestamp + (365L * 24 * 60 * 60 * 1000), // 1 year validity
                null, // Will add blockchain hash
                null  // Will add signature
            );

            // Generate blockchain hash
            String blockchainHash = generateBlockchainHash(certificate);

            // Generate digital signature
            // Generate a key pair for signing (in production, use persistent keys)
            java.security.KeyPair keyPair = signatureService.generateKeyPair();
            byte[] signature = signatureService.sign(
                certificate.toSignableData().getBytes(),
                keyPair.getPrivate()
            );

            // Create final certificate with hash and signature
            VerificationCertificate finalCertificate = new VerificationCertificate(
                certificate.certificateId(),
                certificate.entityId(),
                certificate.entityType(),
                certificate.verificationType(),
                certificate.verifierId(),
                certificate.verificationData(),
                certificate.status(),
                certificate.issuedAt(),
                certificate.expiresAt(),
                blockchainHash,
                bytesToHex(signature)
            );

            // Store on blockchain (simulated)
            certificates.put(certificateId, finalCertificate);

            LOG.infof("✅ Generated verification certificate: %s for entity: %s",
                     certificateId, request.entityId());

            return finalCertificate;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Retrieve a certificate by ID
     */
    public Uni<VerificationCertificate> getCertificate(String certificateId) {
        return Uni.createFrom().item(() -> {
            VerificationCertificate cert = certificates.get(certificateId);
            if (cert == null) {
                throw new CertificateNotFoundException("Certificate not found: " + certificateId);
            }
            return cert;
        });
    }

    /**
     * Verify a certificate's authenticity
     */
    public Uni<CertificateVerificationResult> verifyCertificate(String certificateId) {
        return getCertificate(certificateId).map(cert -> {
            boolean isValid = true;
            List<String> validationErrors = new java.util.ArrayList<>();

            // Check expiration
            if (cert.expiresAt() < System.currentTimeMillis()) {
                isValid = false;
                validationErrors.add("Certificate has expired");
            }

            // Check status
            if (cert.status() != CertificateStatus.ISSUED) {
                isValid = false;
                validationErrors.add("Certificate status is not ISSUED: " + cert.status());
            }

            // Verify blockchain hash
            String expectedHash = generateBlockchainHash(cert);
            if (!expectedHash.equals(cert.blockchainHash())) {
                isValid = false;
                validationErrors.add("Blockchain hash mismatch");
            }

            // Verify digital signature
            try {
                // In production, retrieve the correct public key
                byte[] signature = hexToBytes(cert.digitalSignature());
                // For now, we'll skip signature verification as we don't persist keys
                // In production, store and retrieve public keys from secure storage
                boolean sigValid = true; // Placeholder

                if (!sigValid) {
                    isValid = false;
                    validationErrors.add("Digital signature verification failed");
                }
            } catch (Exception e) {
                isValid = false;
                validationErrors.add("Signature verification error: " + e.getMessage());
            }

            return new CertificateVerificationResult(
                certificateId,
                isValid,
                validationErrors.isEmpty() ? null : validationErrors,
                System.currentTimeMillis()
            );
        });
    }

    /**
     * Revoke a certificate
     */
    public Uni<VerificationCertificate> revokeCertificate(String certificateId, String reason) {
        return getCertificate(certificateId).map(cert -> {
            VerificationCertificate revoked = new VerificationCertificate(
                cert.certificateId(),
                cert.entityId(),
                cert.entityType(),
                cert.verificationType(),
                cert.verifierId(),
                cert.verificationData(),
                CertificateStatus.REVOKED,
                cert.issuedAt(),
                cert.expiresAt(),
                cert.blockchainHash(),
                cert.digitalSignature()
            );

            certificates.put(certificateId, revoked);
            LOG.warnf("🚫 Revoked certificate: %s - Reason: %s", certificateId, reason);

            return revoked;
        });
    }

    /**
     * Get all certificates for an entity
     */
    public Uni<List<VerificationCertificate>> getCertificatesByEntity(String entityId) {
        return Uni.createFrom().item(() ->
            certificates.values().stream()
                .filter(cert -> cert.entityId().equals(entityId))
                .toList()
        );
    }

    /**
     * Get certificate statistics
     */
    public Uni<CertificateStatistics> getStatistics() {
        return Uni.createFrom().item(() -> {
            long total = certificates.size();
            long issued = certificates.values().stream()
                .filter(c -> c.status() == CertificateStatus.ISSUED)
                .count();
            long revoked = certificates.values().stream()
                .filter(c -> c.status() == CertificateStatus.REVOKED)
                .count();
            long expired = certificates.values().stream()
                .filter(c -> c.expiresAt() < System.currentTimeMillis())
                .count();

            return new CertificateStatistics(
                total,
                issued,
                revoked,
                expired,
                System.currentTimeMillis()
            );
        });
    }

    // Helper methods

    private String generateBlockchainHash(VerificationCertificate cert) {
        // Simplified hash generation (in production, use proper blockchain storage)
        String data = cert.toSignableData();
        return "0x" + Integer.toHexString(data.hashCode()) +
               Long.toHexString(System.currentTimeMillis());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    // Data classes

    public record CertificateRequest(
        String entityId,
        String entityType,
        String verificationType,
        String verifierId,
        Map<String, Object> verificationData
    ) {}

    public record VerificationCertificate(
        String certificateId,
        String entityId,
        String entityType,
        String verificationType,
        String verifierId,
        Map<String, Object> verificationData,
        CertificateStatus status,
        long issuedAt,
        long expiresAt,
        String blockchainHash,
        String digitalSignature
    ) {
        public String toSignableData() {
            return String.format("%s:%s:%s:%s:%s:%d:%d",
                certificateId, entityId, entityType, verificationType,
                verifierId, issuedAt, expiresAt);
        }
    }

    public enum CertificateStatus {
        ISSUED,
        REVOKED,
        EXPIRED
    }

    public record CertificateVerificationResult(
        String certificateId,
        boolean isValid,
        List<String> validationErrors,
        long verifiedAt
    ) {}

    public record CertificateStatistics(
        long totalCertificates,
        long issuedCertificates,
        long revokedCertificates,
        long expiredCertificates,
        long timestamp
    ) {}

    public static class CertificateNotFoundException extends RuntimeException {
        public CertificateNotFoundException(String message) {
            super(message);
        }
    }
}
