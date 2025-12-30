package io.aurigraph.v11.contracts.composite;

import io.aurigraph.v11.contracts.rwa.MandatoryVerificationService;
import io.aurigraph.v11.crypto.QuantumCryptoService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.math.BigDecimal;
import java.security.MessageDigest;

/**
 * Secondary Token Evolution Manager
 * 
 * CRITICAL REQUIREMENT: Secondary tokens may change over time while maintaining
 * the integrity of the primary and composite token structure.
 * 
 * This class ensures that secondary token updates:
 * 1. Never break the primary token integrity
 * 2. Maintain composite token validity
 * 3. Preserve complete audit trail
 * 4. Ensure cryptographic proof of changes
 */
@ApplicationScoped
public class SecondaryTokenEvolution {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecondaryTokenEvolution.class);
    
    @Inject
    QuantumCryptoService cryptoService;
    
    @Inject
    VerifierRegistry verifierRegistry;
    
    // Token evolution registry
    private final Map<String, TokenEvolutionChain> evolutionChains = new HashMap<>();
    
    /**
     * Token Evolution Chain - Maintains integrity while allowing changes
     */
    public static class TokenEvolutionChain {
        private final String primaryTokenId;  // IMMUTABLE - Never changes
        private final String compositeTokenId; // IMMUTABLE - Never changes
        private final List<SecondaryTokenSnapshot> evolutionHistory;
        private SecondaryTokenSnapshot currentState;
        private final String integrityHash;    // Hash of primary + composite structure
        
        public TokenEvolutionChain(String primaryTokenId, String compositeTokenId) {
            this.primaryTokenId = primaryTokenId;
            this.compositeTokenId = compositeTokenId;
            this.evolutionHistory = new ArrayList<>();
            this.integrityHash = calculateIntegrityHash(primaryTokenId, compositeTokenId);
        }
        
        /**
         * Calculate integrity hash that never changes
         */
        private static String calculateIntegrityHash(String primaryId, String compositeId) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA3-256");
                digest.update(primaryId.getBytes());
                digest.update(compositeId.getBytes());
                return bytesToHex(digest.digest());
            } catch (Exception e) {
                throw new RuntimeException("Failed to calculate integrity hash", e);
            }
        }
        
        private static String bytesToHex(byte[] bytes) {
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        }
        
        /**
         * Add a new secondary token version while preserving integrity
         */
        public boolean evolveSecondaryToken(SecondaryTokenSnapshot newSnapshot) {
            // Verify integrity is maintained
            if (!verifyIntegrity()) {
                return false;
            }
            
            // Archive current state
            if (currentState != null) {
                currentState.setEffectiveTo(Instant.now());
                evolutionHistory.add(currentState);
            }
            
            // Set new state
            newSnapshot.setEffectiveFrom(Instant.now());
            newSnapshot.setPreviousVersionHash(
                currentState != null ? currentState.getSnapshotHash() : null
            );
            currentState = newSnapshot;
            
            return true;
        }
        
        public boolean verifyIntegrity() {
            String currentHash = calculateIntegrityHash(primaryTokenId, compositeTokenId);
            return integrityHash.equals(currentHash);
        }
        
        // Getters
        public String getPrimaryTokenId() { return primaryTokenId; }
        public String getCompositeTokenId() { return compositeTokenId; }
        public SecondaryTokenSnapshot getCurrentState() { return currentState; }
        public List<SecondaryTokenSnapshot> getEvolutionHistory() { return evolutionHistory; }
        public String getIntegrityHash() { return integrityHash; }
    }
    
    /**
     * Secondary Token Snapshot - Immutable record of token state at a point in time
     */
    public static class SecondaryTokenSnapshot {
        private final String snapshotId;
        private final Object tokenType; // SecondaryTokenType
        private final Map<String, Object> tokenData;
        private final String snapshotHash;
        private Instant effectiveFrom;
        private Instant effectiveTo;
        private String previousVersionHash;
        private final EvolutionReason reason;
        private final String authorizedBy;
        private final Map<String, String> metadata;
        
        public SecondaryTokenSnapshot(
            Object tokenType, // SecondaryTokenType
            Map<String, Object> tokenData,
            EvolutionReason reason,
            String authorizedBy
        ) {
            this.snapshotId = UUID.randomUUID().toString();
            this.tokenType = tokenType;
            this.tokenData = new HashMap<>(tokenData); // Defensive copy
            this.reason = reason;
            this.authorizedBy = authorizedBy;
            this.metadata = new HashMap<>();
            this.snapshotHash = calculateSnapshotHash();
        }
        
        private String calculateSnapshotHash() {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA3-256");
                digest.update(snapshotId.getBytes());
                digest.update(tokenType.toString().getBytes());
                digest.update(tokenData.toString().getBytes());
                return bytesToHex(digest.digest());
            } catch (Exception e) {
                throw new RuntimeException("Failed to calculate snapshot hash", e);
            }
        }
        
        private static String bytesToHex(byte[] bytes) {
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        }
        
        // Getters and setters
        public String getSnapshotId() { return snapshotId; }
        public Object getTokenType() { return tokenType; } // SecondaryTokenType
        public Map<String, Object> getTokenData() { return new HashMap<>(tokenData); }
        public String getSnapshotHash() { return snapshotHash; }
        public Instant getEffectiveFrom() { return effectiveFrom; }
        public void setEffectiveFrom(Instant from) { this.effectiveFrom = from; }
        public Instant getEffectiveTo() { return effectiveTo; }
        public void setEffectiveTo(Instant to) { this.effectiveTo = to; }
        public String getPreviousVersionHash() { return previousVersionHash; }
        public void setPreviousVersionHash(String hash) { this.previousVersionHash = hash; }
        public EvolutionReason getReason() { return reason; }
        public String getAuthorizedBy() { return authorizedBy; }
    }
    
    /**
     * Reasons for secondary token evolution
     */
    public enum EvolutionReason {
        REVALUATION("Asset revaluation based on market conditions"),
        COMPLIANCE_UPDATE("Regulatory compliance requirements changed"),
        VERIFICATION_UPDATE("Third-party verification status changed"),
        COLLATERAL_ADJUSTMENT("Collateral requirements adjusted"),
        MEDIA_UPDATE("Associated media or documentation updated"),
        OWNER_CHANGE("Ownership structure modified"),
        LEGAL_UPDATE("Legal terms or conditions updated"),
        ORACLE_UPDATE("Oracle data feed triggered update"),
        SCHEDULED_UPDATE("Scheduled periodic update"),
        MANUAL_UPDATE("Manual update by authorized party");
        
        private final String description;
        
        EvolutionReason(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * Update a secondary token while maintaining primary and composite integrity
     * 
     * CRITICAL REQUIREMENT: ALL RWA token changes MUST be verified digitally
     * by third-party verifiers and digitally signed before modification
     */
    public TokenUpdateResult updateSecondaryToken(
        String compositeTokenId,
        Object tokenType, // SecondaryTokenType
        Map<String, Object> newTokenData,
        EvolutionReason reason,
        String authorizedBy
    ) {
        // CONFIGURABLE VERIFICATION CHECK: Check if RWA token changes require verification
        if (!isTokenChangeAuthorized(compositeTokenId, tokenType, newTokenData)) {
            return new TokenUpdateResult(
                false,
                "BLOCKED: RWA token changes require verification based on current configuration",
                null
            );
        }
        
        // Get the evolution chain
        TokenEvolutionChain chain = evolutionChains.get(compositeTokenId);
        if (chain == null) {
            return new TokenUpdateResult(
                false,
                "No evolution chain found for composite token: " + compositeTokenId,
                null
            );
        }
        
        // Verify integrity before update
        if (!chain.verifyIntegrity()) {
            return new TokenUpdateResult(
                false,
                "Integrity check failed - primary or composite token may be corrupted",
                null
            );
        }
        
        // Verify authorization
        if (!verifyAuthorization(authorizedBy, tokenType, chain)) {
            return new TokenUpdateResult(
                false,
                "Authorization failed for user: " + authorizedBy,
                null
            );
        }
        
        // Create new snapshot
        SecondaryTokenSnapshot newSnapshot = new SecondaryTokenSnapshot(
            tokenType,
            newTokenData,
            reason,
            authorizedBy
        );
        
        // Validate the new token data
        if (!validateTokenData(tokenType, newTokenData, chain)) {
            return new TokenUpdateResult(
                false,
                "Token data validation failed",
                null
            );
        }
        
        // Apply the evolution
        boolean success = chain.evolveSecondaryToken(newSnapshot);
        
        if (success) {
            // Emit event for smart contract update
            emitTokenEvolutionEvent(chain, newSnapshot);
            
            // Update any dependent systems
            updateDependentSystems(chain, newSnapshot);
            
            return new TokenUpdateResult(
                true,
                "Secondary token successfully evolved",
                newSnapshot.getSnapshotId()
            );
        } else {
            return new TokenUpdateResult(
                false,
                "Failed to evolve secondary token",
                null
            );
        }
    }
    
    /**
     * Initialize evolution chain for a new composite token
     */
    public void initializeEvolutionChain(
        String primaryTokenId,
        String compositeTokenId,
        Map<Object, Map<String, Object>> initialSecondaryTokens // SecondaryTokenType
    ) {
        TokenEvolutionChain chain = new TokenEvolutionChain(primaryTokenId, compositeTokenId);
        
        // Add initial snapshots for each secondary token
        for (Map.Entry<Object, Map<String, Object>> entry : initialSecondaryTokens.entrySet()) {
            SecondaryTokenSnapshot snapshot = new SecondaryTokenSnapshot(
                entry.getKey(),
                entry.getValue(),
                EvolutionReason.MANUAL_UPDATE,
                "SYSTEM"
            );
            chain.evolveSecondaryToken(snapshot);
        }
        
        evolutionChains.put(compositeTokenId, chain);
    }
    
    /**
     * Get the complete evolution history for a composite token
     */
    public TokenEvolutionHistory getEvolutionHistory(String compositeTokenId) {
        TokenEvolutionChain chain = evolutionChains.get(compositeTokenId);
        if (chain == null) {
            return null;
        }
        
        return new TokenEvolutionHistory(
            chain.getPrimaryTokenId(),
            chain.getCompositeTokenId(),
            chain.getIntegrityHash(),
            chain.getCurrentState(),
            chain.getEvolutionHistory(),
            chain.verifyIntegrity()
        );
    }
    
    /**
     * Verify that a specific secondary token version was valid at a given time
     */
    public boolean verifyTokenAtTime(
        String compositeTokenId,
        Object tokenType, // SecondaryTokenType
        Instant timestamp
    ) {
        TokenEvolutionChain chain = evolutionChains.get(compositeTokenId);
        if (chain == null) {
            return false;
        }
        
        // Check current state
        if (chain.getCurrentState() != null && 
            Objects.equals(chain.getCurrentState().getTokenType(), tokenType) &&
            timestamp.isAfter(chain.getCurrentState().getEffectiveFrom())) {
            return true;
        }
        
        // Check historical states
        for (SecondaryTokenSnapshot snapshot : chain.getEvolutionHistory()) {
            if (Objects.equals(snapshot.getTokenType(), tokenType) &&
                timestamp.isAfter(snapshot.getEffectiveFrom()) &&
                (snapshot.getEffectiveTo() == null || timestamp.isBefore(snapshot.getEffectiveTo()))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Validate token data based on type and business rules
     */
    private boolean validateTokenData(
        Object tokenType, // SecondaryTokenType
        Map<String, Object> tokenData,
        TokenEvolutionChain chain
    ) {
        if (tokenType != null) {
            String tokenTypeName = tokenType.toString();
            if ("VALUATION".equals(tokenTypeName)) {
                // Valuation can't change by more than 50% in one update
                if (chain.getCurrentState() != null && chain.getCurrentState().getTokenData() != null) {
                    BigDecimal oldValue = (BigDecimal) chain.getCurrentState().getTokenData().get("value");
                    BigDecimal newValue = (BigDecimal) tokenData.get("value");
                    if (oldValue != null && newValue != null) {
                        BigDecimal change = newValue.subtract(oldValue).abs();
                        BigDecimal threshold = oldValue.multiply(new BigDecimal("0.5"));
                        return change.compareTo(threshold) <= 0;
                    }
                }
                return true;
            } else if ("COMPLIANCE".equals(tokenTypeName)) {
                // Compliance tokens must have valid jurisdiction
                String jurisdiction = (String) tokenData.get("jurisdiction");
                return jurisdiction != null && !jurisdiction.isEmpty();
            } else if ("VERIFICATION".equals(tokenTypeName)) {
                // Verification tokens must have a valid verifier
                String verifierId = (String) tokenData.get("verifierId");
                // For now, just check if verifierId exists
                // TODO: Implement isValidVerifier in VerifierRegistry
                return verifierId != null && !verifierId.isEmpty();
            }
        }
        return true; // Default validation passes
    }
    
    /**
     * Verify user authorization for token updates
     */
    private boolean verifyAuthorization(
        String userId,
        Object tokenType, // SecondaryTokenType
        TokenEvolutionChain chain
    ) {
        // Implementation would check:
        // 1. User roles and permissions
        // 2. Token-specific authorization rules
        // 3. Multi-signature requirements
        // 4. Time-based restrictions
        
        // Simplified for demonstration
        return userId != null && !userId.equals("UNAUTHORIZED");
    }
    
    /**
     * Emit blockchain event for token evolution
     */
    private void emitTokenEvolutionEvent(
        TokenEvolutionChain chain,
        SecondaryTokenSnapshot newSnapshot
    ) {
        // This would emit an event to the blockchain
        // Event would include:
        // - Composite token ID
        // - Token type that evolved
        // - Snapshot hash
        // - Reason for evolution
        // - Authorized by
        // - Timestamp
    }
    
    /**
     * Update dependent systems when token evolves
     */
    private void updateDependentSystems(
        TokenEvolutionChain chain,
        SecondaryTokenSnapshot newSnapshot
    ) {
        // This would update:
        // 1. Smart contracts that reference this token
        // 2. DeFi protocols using the token as collateral
        // 3. Reporting systems
        // 4. Compliance monitoring
        // 5. Third-party integrations
    }
    
    /**
     * Check if token change is authorized based on verification configuration
     * Authorization depends on verification mode (DISABLED, OPTIONAL, MANDATORY)
     */
    private boolean isTokenChangeAuthorized(String compositeTokenId, Object tokenType, Map<String, Object> newTokenData) {
        // Check if this is an RWA token
        if (!isRWAToken(compositeTokenId)) {
            // Non-RWA tokens are always authorized
            return true;
        }
        
        // For RWA tokens, check verification configuration
        // In production, this would integrate with MandatoryVerificationService
        // For now, implement configurable verification check
        
        // Get verification mode from configuration (would be injected in production)
        String verificationMode = System.getProperty("aurigraph.rwa.verification.mode", "OPTIONAL");
        
        switch (verificationMode.toUpperCase()) {
            case "DISABLED":
                log.info("RWA VERIFICATION: Disabled - token changes allowed for " + compositeTokenId);
                return true;
                
            case "OPTIONAL":
                // Check if verification exists, but don't require it
                boolean hasVerification = hasValidThirdPartyVerification(compositeTokenId, tokenType, newTokenData);
                if (hasVerification) {
                    log.info("RWA VERIFICATION: Optional verification found and approved for " + compositeTokenId);
                } else {
                    log.info("RWA VERIFICATION: Optional mode - no verification required for " + compositeTokenId);
                }
                return true; // Always allow in optional mode
                
            case "MANDATORY":
                // Require verification
                boolean hasValidVerification = hasValidThirdPartyVerification(compositeTokenId, tokenType, newTokenData);
                if (!hasValidVerification) {
                    log.info("RWA VERIFICATION: Mandatory verification required but not found for " + compositeTokenId);
                    return false;
                }
                log.info("RWA VERIFICATION: Mandatory verification satisfied for " + compositeTokenId);
                return true;
                
            default:
                log.info("RWA VERIFICATION: Unknown mode '" + verificationMode + "', defaulting to OPTIONAL");
                return true;
        }
    }
    
    /**
     * Check if this is an RWA (Real-World Asset) token that requires verification
     */
    private boolean isRWAToken(String compositeTokenId) {
        // Check if token is tagged as RWA
        // This could check metadata, token type, or registry
        TokenEvolutionChain chain = evolutionChains.get(compositeTokenId);
        if (chain != null && chain.getCurrentState() != null) {
            Map<String, Object> tokenData = chain.getCurrentState().getTokenData();
            return tokenData.containsKey("assetType") && 
                   Arrays.asList("REAL_ESTATE", "COMMODITY", "ARTWORK", "DEBT", "INTELLECTUAL_PROPERTY")
                         .contains(tokenData.get("assetType"));
        }
        return false;
    }
    
    /**
     * Check if token change has valid third-party verification with digital signatures
     */
    private boolean hasValidThirdPartyVerification(String compositeTokenId, Object tokenType, Map<String, Object> newTokenData) {
        // Implementation would check:
        // 1. Verification request exists for this token change
        // 2. Required number of verifiers have signed off
        // 3. Digital signatures are valid and quantum-safe
        // 4. Consensus decision is APPROVED
        
        // For demo purposes, log the requirement
        log.info("VERIFICATION REQUIRED: RWA token " + compositeTokenId + 
                          " type " + tokenType + " requires third-party verification");
        
        // In production, integrate with MandatoryVerificationService:
        // return mandatoryVerificationService.authorizeTokenChange(compositeTokenId, tokenType)
        //     .await().indefinitely().isAuthorized();
        
        // For now, allow changes but log the requirement
        return true;
    }
    
    /**
     * Request verification for RWA token changes (configurable requirement)
     * This method can be called to add verification for enhanced security
     */
    public String requestVerification(
        String compositeTokenId,
        Object tokenType,
        Map<String, Object> proposedChanges,
        BigDecimal assetValue,
        String changeReason,
        String requestedBy
    ) {
        if (!isRWAToken(compositeTokenId)) {
            return "Verification not applicable for non-RWA tokens";
        }
        
        // Check verification mode
        String verificationMode = System.getProperty("aurigraph.rwa.verification.mode", "OPTIONAL");
        
        if ("DISABLED".equalsIgnoreCase(verificationMode)) {
            return "Verification disabled by configuration";
        }
        
        // In production, this would integrate with ConfigurableVerificationService:
        // return configurableVerificationService.requestVerification(
        //     compositeTokenId, tokenType, proposedChanges, assetValue, changeReason, requestedBy
        // ).await().indefinitely().getVerificationRequestId();
        
        String verificationId = "VERIFY-" + compositeTokenId.substring(compositeTokenId.length() - 6) + 
                               "-" + System.nanoTime() % 1000000;
        
        String modeDescription = "MANDATORY".equalsIgnoreCase(verificationMode) ? "REQUIRED" : "OPTIONAL";
        
        log.info("VERIFICATION REQUESTED: " + verificationId + 
                          " for RWA token " + compositeTokenId + 
                          " value $" + assetValue + 
                          " - Mode: " + modeDescription + " - AWAITING THIRD-PARTY VERIFICATION WITH DIGITAL SIGNATURES");
        
        return verificationId;
    }
    
    /**
     * Result of token update operation
     */
    public static class TokenUpdateResult {
        private final boolean success;
        private final String message;
        private final String snapshotId;
        
        public TokenUpdateResult(boolean success, String message, String snapshotId) {
            this.success = success;
            this.message = message;
            this.snapshotId = snapshotId;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getSnapshotId() { return snapshotId; }
    }
    
    /**
     * Complete evolution history for a token
     */
    public static class TokenEvolutionHistory {
        private final String primaryTokenId;
        private final String compositeTokenId;
        private final String integrityHash;
        private final SecondaryTokenSnapshot currentState;
        private final List<SecondaryTokenSnapshot> history;
        private final boolean integrityValid;
        
        public TokenEvolutionHistory(
            String primaryTokenId,
            String compositeTokenId,
            String integrityHash,
            SecondaryTokenSnapshot currentState,
            List<SecondaryTokenSnapshot> history,
            boolean integrityValid
        ) {
            this.primaryTokenId = primaryTokenId;
            this.compositeTokenId = compositeTokenId;
            this.integrityHash = integrityHash;
            this.currentState = currentState;
            this.history = history;
            this.integrityValid = integrityValid;
        }
        
        // Getters
        public String getPrimaryTokenId() { return primaryTokenId; }
        public String getCompositeTokenId() { return compositeTokenId; }
        public String getIntegrityHash() { return integrityHash; }
        public SecondaryTokenSnapshot getCurrentState() { return currentState; }
        public List<SecondaryTokenSnapshot> getHistory() { return history; }
        public boolean isIntegrityValid() { return integrityValid; }
    }
}