package io.aurigraph.v11.contracts.composite;

import io.aurigraph.v11.merkle.MerkleProof;
import io.aurigraph.v11.merkle.MerkleTreeRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Verifier Registry Service with Merkle Tree Support
 *
 * Manages third-party verifiers and verification requests with cryptographic verification.
 * Implements 4-tier verifier system with reputation scoring and automated assignment.
 *
 * Features:
 * - 4-tier verifier system (TIER_1 to TIER_4)
 * - Reputation scoring and tracking
 * - Automated verifier assignment
 * - Credential expiration tracking
 * - Merkle tree cryptographic verification
 * - Proof generation and verification
 * - Root hash tracking for verifier trust
 *
 * @version 11.5.0
 * @since 2025-10-25 - AV11-459: VerifierRegistry Merkle Tree
 */
@ApplicationScoped
public class VerifierRegistry extends MerkleTreeRegistry<ThirdPartyVerifier> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerifierRegistry.class);

    private final Map<String, VerificationRequest> activeRequests = new ConcurrentHashMap<>();
    private final Map<String, List<String>> verifiersByTier = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> verifierReputation = new ConcurrentHashMap<>();
    private final Map<String, Instant> credentialExpiration = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);

    public VerifierRegistry() {
        super();
        initializeDefaultVerifiers();
    }

    @Override
    protected String serializeValue(ThirdPartyVerifier verifier) {
        return String.format("%s|%s|%s|%s|%s|%d|%s",
            verifier.getVerifierId(),
            verifier.getName(),
            verifier.getTier(),
            verifier.getSpecialization(),
            verifier.getStatus(),
            verifier.getCompletedVerifications(),
            verifier.getSuccessRate()
        );
    }

    /**
     * Register a new third-party verifier
     */
    public Uni<String> registerVerifier(ThirdPartyVerifier verifier) {
        return Uni.createFrom().item(() -> {
            String verifierId = generateVerifierId(verifier);
            verifier.setVerifierId(verifierId);
            verifier.setRegisteredAt(Instant.now());
            verifier.setStatus(VerifierStatus.PENDING_APPROVAL);

            // Initialize reputation score
            verifierReputation.put(verifierId, BigDecimal.valueOf(50)); // Start with neutral score

            // Set credential expiration (2 years from now)
            credentialExpiration.put(verifierId, Instant.now().plusSeconds(2 * 365 * 24 * 3600L));

            LOGGER.info("Registered new verifier: {} ({}) - {}",
                verifier.getName(), verifierId, verifier.getTier());

            return verifierId;
        }).flatMap(id -> add(id, verifier).map(success -> id));
    }

    /**
     * Approve a verifier for active duty
     */
    public Uni<Boolean> approveVerifier(String verifierId) {
        return get(verifierId).flatMap(verifier -> {
            if (verifier == null) {
                return Uni.createFrom().item(false);
            }

            verifier.setStatus(VerifierStatus.ACTIVE);
            verifier.setApprovedAt(Instant.now());

            // Add to tier-based lookup
            String tierKey = verifier.getTier().name();
            verifiersByTier.computeIfAbsent(tierKey, k -> new ArrayList<>()).add(verifierId);

            LOGGER.info("Approved verifier: {} for {} tier", verifier.getName(), verifier.getTier());

            return add(verifierId, verifier).map(success -> true);
        });
    }

    /**
     * Check and renew credentials for verifier
     */
    public Uni<Boolean> renewCredentials(String verifierId, Instant newExpirationDate) {
        return get(verifierId).flatMap(verifier -> {
            if (verifier == null) {
                return Uni.createFrom().item(false);
            }

            credentialExpiration.put(verifierId, newExpirationDate);
            LOGGER.info("Renewed credentials for verifier: {} until {}", verifierId, newExpirationDate);

            return add(verifierId, verifier).map(success -> true);
        });
    }

    /**
     * Get verifiers with expired credentials
     */
    public Uni<List<ThirdPartyVerifier>> getExpiredVerifiers() {
        return getAll().map(verifiers -> {
            Instant now = Instant.now();
            return verifiers.stream()
                .filter(v -> {
                    Instant expiration = credentialExpiration.get(v.getVerifierId());
                    return expiration != null && expiration.isBefore(now);
                })
                .collect(Collectors.toList());
        });
    }

    /**
     * Generate Merkle proof for a verifier
     */
    public Uni<MerkleProof.ProofData> getVerifierProof(String verifierId) {
        return generateProof(verifierId).map(MerkleProof::toProofData);
    }

    /**
     * Verify a Merkle proof for verifier trust
     */
    public Uni<VerificationResponse> verifyMerkleProof(MerkleProof.ProofData proofData) {
        return verifyProof(proofData.toMerkleProof()).map(valid ->
            new VerificationResponse(valid, valid ? "Verifier proof verified successfully" : "Invalid verifier proof")
        );
    }

    /**
     * Get current Merkle root hash for verifier registry
     */
    public Uni<RootHashResponse> getMerkleRootHash() {
        return getRootHash().flatMap(rootHash ->
            getTreeStats().map(stats -> new RootHashResponse(
                rootHash,
                Instant.now(),
                stats.getEntryCount(),
                stats.getTreeHeight()
            ))
        );
    }

    /**
     * Get Merkle tree statistics
     */
    public Uni<MerkleTreeStats> getMerkleTreeStats() {
        return getTreeStats();
    }

    /**
     * Request verification for a composite token
     */
    public Uni<String> requestVerification(String compositeId, String assetType, 
                                         VerificationLevel requiredLevel, int verifierCount) {
        return Uni.createFrom().item(() -> {
            String requestId = generateRequestId(compositeId);
            
            // Find qualified verifiers for the required level
            List<String> qualifiedVerifiers = findQualifiedVerifiers(requiredLevel, assetType, verifierCount);
            
            if (qualifiedVerifiers.size() < verifierCount) {
                throw new InsufficientVerifiersException(
                    String.format("Only %d qualified verifiers available, need %d", 
                        qualifiedVerifiers.size(), verifierCount));
            }
            
            VerificationRequest request = new VerificationRequest(
                requestId,
                compositeId,
                assetType,
                requiredLevel,
                qualifiedVerifiers.subList(0, verifierCount),
                Instant.now()
            );
            
            activeRequests.put(requestId, request);
            
            // Notify assigned verifiers
            for (String verifierId : qualifiedVerifiers.subList(0, verifierCount)) {
                notifyVerifier(verifierId, request);
            }
            
            LOGGER.info("Created verification request %s for composite %s with %d verifiers", 
                requestId, compositeId, verifierCount);
            
            return requestId;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Submit verification result
     */
    public Uni<Boolean> submitVerificationResult(String requestId, String verifierId, 
                                               VerificationResult result) {
        return Uni.createFrom().item(() -> {
            VerificationRequest request = activeRequests.get(requestId);
            if (request == null) {
                return false;
            }
            
            if (!request.getAssignedVerifiers().contains(verifierId)) {
                throw new UnauthorizedVerifierException("Verifier not assigned to this request");
            }
            
            // Add result to request
            request.addVerificationResult(result);
            
            // Update verifier performance metrics
            updateVerifierPerformance(verifierId, result);
            
            // Check if request is complete
            if (request.isComplete()) {
                processCompletedRequest(request);
            }
            
            LOGGER.info("Received verification result from %s for request %s", verifierId, requestId);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get verifier statistics
     */
    public Uni<VerifierStats> getVerifierStats() {
        return Uni.createFrom().item(() -> {
            Map<VerifierTier, Integer> tierCounts = new HashMap<>();
            Map<VerifierStatus, Integer> statusCounts = new HashMap<>();
            
            for (ThirdPartyVerifier verifier : registry.values()) {
                tierCounts.merge(verifier.getTier(), 1, Integer::sum);
                statusCounts.merge(verifier.getStatus(), 1, Integer::sum);
            }
            
            int activeRequests = (int) this.activeRequests.values().stream()
                .filter(req -> !req.isComplete())
                .count();
            
            return new VerifierStats(
                registry.size(),
                tierCounts,
                statusCounts,
                activeRequests,
                calculateAverageReputation()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get verifiers by tier
     */
    public Uni<List<ThirdPartyVerifier>> getVerifiersByTier(VerifierTier tier) {
        return Uni.createFrom().item(() -> {
            List<String> verifierIds = verifiersByTier.getOrDefault(tier.name(), new ArrayList<>());

            return verifierIds.stream()
                .map(registry::get)
                .filter(Objects::nonNull)
                .filter(v -> v.getStatus() == VerifierStatus.ACTIVE)
                .toList();
        });
    }

    /**
     * Get verifier performance metrics
     */
    public Uni<VerifierPerformance> getVerifierPerformance(String verifierId) {
        return Uni.createFrom().item(() -> {
            ThirdPartyVerifier verifier = registry.get(verifierId);
            if (verifier == null) {
                return null;
            }
            
            BigDecimal reputation = verifierReputation.getOrDefault(verifierId, BigDecimal.ZERO);
            
            return new VerifierPerformance(
                verifierId,
                verifier.getName(),
                verifier.getTier(),
                reputation,
                verifier.getCompletedVerifications(),
                verifier.getSuccessRate(),
                verifier.getAverageResponseTime()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // Private helper methods

    private void initializeDefaultVerifiers() {
        // Initialize some default verifiers for testing
        List<ThirdPartyVerifier> defaultVerifiers = Arrays.asList(
            new ThirdPartyVerifier("ACME Appraisals", VerifierTier.TIER_1, "Real Estate", 
                                 "Licensed real estate appraisers", "contact@acme-appraisals.com"),
            new ThirdPartyVerifier("Global Certification Corp", VerifierTier.TIER_2, "Multi-Asset", 
                                 "Regional certification specialists", "info@globalcert.com"),
            new ThirdPartyVerifier("Platinum Valuations", VerifierTier.TIER_3, "High-Value Assets", 
                                 "National certification firm", "contact@platinumval.com"),
            new ThirdPartyVerifier("Big Four Consulting", VerifierTier.TIER_4, "Institutional", 
                                 "Major institutional verification services", "enterprise@bigfour.com")
        );

        for (ThirdPartyVerifier verifier : defaultVerifiers) {
            registerVerifier(verifier).await().indefinitely();
            approveVerifier(verifier.getVerifierId()).await().indefinitely();
        }
    }

    private String generateVerifierId(ThirdPartyVerifier verifier) {
        return String.format("VER-%s-%s-%d",
            verifier.getTier().name().substring(5), // Remove "TIER_"
            verifier.getName().replaceAll("[^a-zA-Z0-9]", "").substring(0, Math.min(6, verifier.getName().length())),
            System.nanoTime() % 100000);
    }

    private String generateRequestId(String compositeId) {
        return String.format("REQ-%s-%d",
            compositeId.substring(compositeId.lastIndexOf('-') + 1),
            requestCounter.incrementAndGet());
    }

    private List<String> findQualifiedVerifiers(VerificationLevel requiredLevel, String assetType, int count) {
        VerifierTier requiredTier = mapVerificationLevelToTier(requiredLevel);
        
        List<String> candidates = new ArrayList<>();
        
        // Get verifiers from required tier and higher
        for (VerifierTier tier : VerifierTier.values()) {
            if (tier.ordinal() >= requiredTier.ordinal()) {
                List<String> tierVerifiers = verifiersByTier.getOrDefault(tier.name(), new ArrayList<>());
                candidates.addAll(tierVerifiers);
            }
        }
        
        // Filter by asset type specialization and active status
        List<String> qualified = candidates.stream()
            .filter(id -> {
                ThirdPartyVerifier verifier = registry.get(id);
                return verifier != null && 
                       verifier.getStatus() == VerifierStatus.ACTIVE &&
                       (verifier.getSpecialization().equals("Multi-Asset") || 
                        verifier.getSpecialization().contains(assetType));
            })
            .toList();
        
        // Sort by reputation score (highest first)
        qualified.sort((id1, id2) -> {
            BigDecimal rep1 = verifierReputation.getOrDefault(id1, BigDecimal.ZERO);
            BigDecimal rep2 = verifierReputation.getOrDefault(id2, BigDecimal.ZERO);
            return rep2.compareTo(rep1);
        });
        
        return qualified;
    }

    private VerifierTier mapVerificationLevelToTier(VerificationLevel level) {
        return switch (level) {
            case NONE, BASIC -> VerifierTier.TIER_1;
            case ENHANCED -> VerifierTier.TIER_2;
            case CERTIFIED -> VerifierTier.TIER_3;
            case INSTITUTIONAL -> VerifierTier.TIER_4;
        };
    }

    private void notifyVerifier(String verifierId, VerificationRequest request) {
        // In a real implementation, this would send notifications via email, API, etc.
        LOGGER.info("Notifying verifier %s of new verification request %s", verifierId, request.getRequestId());
    }

    private void updateVerifierPerformance(String verifierId, VerificationResult result) {
        ThirdPartyVerifier verifier = registry.get(verifierId);
        if (verifier != null) {
            verifier.incrementCompletedVerifications();
            
            // Update reputation based on result quality
            BigDecimal currentReputation = verifierReputation.getOrDefault(verifierId, BigDecimal.valueOf(50));
            BigDecimal adjustment = calculateReputationAdjustment(result);
            BigDecimal newReputation = currentReputation.add(adjustment);
            
            // Keep reputation between 0 and 100
            newReputation = newReputation.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
            verifierReputation.put(verifierId, newReputation);
        }
    }

    private BigDecimal calculateReputationAdjustment(VerificationResult result) {
        // Simple reputation adjustment based on result completeness and timeliness
        BigDecimal adjustment = BigDecimal.valueOf(1); // Base positive adjustment
        
        // Bonus for detailed reports
        if (result.getReportSummary() != null && result.getReportSummary().length() > 100) {
            adjustment = adjustment.add(BigDecimal.valueOf(0.5));
        }
        
        // Bonus for quick response (within 24 hours)
        if (result.getVerifiedAt().isAfter(Instant.now().minusSeconds(24 * 60 * 60))) {
            adjustment = adjustment.add(BigDecimal.valueOf(0.5));
        }
        
        return adjustment;
    }

    private void processCompletedRequest(VerificationRequest request) {
        // Process completed verification request
        LOGGER.info("Verification request %s completed with %d results", 
            request.getRequestId(), request.getVerificationResults().size());
        
        // Remove from active requests
        activeRequests.remove(request.getRequestId());
    }

    private BigDecimal calculateAverageReputation() {
        if (verifierReputation.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = verifierReputation.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(verifierReputation.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    // Exception classes
    public static class InsufficientVerifiersException extends RuntimeException {
        public InsufficientVerifiersException(String message) { super(message); }
    }

    public static class UnauthorizedVerifierException extends RuntimeException {
        public UnauthorizedVerifierException(String message) { super(message); }
    }

    // Response Classes for Merkle tree operations
    public static class VerificationResponse {
        private final boolean valid;
        private final String message;

        public VerificationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class RootHashResponse {
        private final String rootHash;
        private final Instant timestamp;
        private final int entryCount;
        private final int treeHeight;

        public RootHashResponse(String rootHash, Instant timestamp, int entryCount, int treeHeight) {
            this.rootHash = rootHash;
            this.timestamp = timestamp;
            this.entryCount = entryCount;
            this.treeHeight = treeHeight;
        }

        public String getRootHash() {
            return rootHash;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public int getEntryCount() {
            return entryCount;
        }

        public int getTreeHeight() {
            return treeHeight;
        }
    }
}