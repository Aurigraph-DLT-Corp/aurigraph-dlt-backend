package io.aurigraph.v11.token.vvb;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Lifecycle Governance Service
 * Enforces governance rules for token state transitions
 */
@ApplicationScoped
public class TokenLifecycleGovernance {

    @Inject
    Event<LifecycleGovernanceEvent> governanceEvent;

    private final Map<String, TokenHierarchy> tokenHierarchy = new ConcurrentHashMap<>();

    /**
     * Validate primary token retirement
     * Prevents retirement if active secondary tokens exist
     */
    public Uni<GovernanceValidation> validateRetirement(String primaryTokenId) {
        return Uni.createFrom().item(() -> {
            TokenHierarchy hierarchy = tokenHierarchy.get(primaryTokenId);

            if (hierarchy == null) {
                return new GovernanceValidation(primaryTokenId, true, "No token hierarchy found");
            }

            // Check for active secondary tokens
            List<String> blockingTokens = hierarchy.getActiveSecondaryTokens();

            if (!blockingTokens.isEmpty()) {
                Log.warnf("Cannot retire primary token %s: %d active secondary tokens",
                    primaryTokenId, blockingTokens.size());

                governanceEvent.fire(new LifecycleGovernanceEvent(
                    primaryTokenId,
                    GovernanceEventType.RETIREMENT_BLOCKED,
                    "Active secondary tokens prevent retirement",
                    blockingTokens
                ));

                return new GovernanceValidation(
                    primaryTokenId,
                    false,
                    "Cannot retire: " + blockingTokens.size() + " active secondary tokens",
                    blockingTokens
                );
            }

            governanceEvent.fire(new LifecycleGovernanceEvent(
                primaryTokenId,
                GovernanceEventType.RETIREMENT_ALLOWED,
                "All secondary tokens retired or redeemed",
                Collections.emptyList()
            ));

            return new GovernanceValidation(primaryTokenId, true, "Retirement allowed");
        });
    }

    /**
     * Validate token suspension
     */
    public Uni<GovernanceValidation> validateSuspension(String tokenId) {
        return Uni.createFrom().item(() -> {
            TokenHierarchy hierarchy = tokenHierarchy.get(tokenId);

            if (hierarchy == null || hierarchy.getStatus() == TokenStatus.SUSPENDED) {
                return new GovernanceValidation(tokenId, true, "Already suspended or not found");
            }

            // Check preconditions for suspension
            if (hierarchy.hasActiveTransactions()) {
                List<String> blockingTransactions = hierarchy.getActiveTransactionIds();
                return new GovernanceValidation(
                    tokenId,
                    false,
                    "Cannot suspend: active transactions",
                    blockingTransactions
                );
            }

            governanceEvent.fire(new LifecycleGovernanceEvent(
                tokenId,
                GovernanceEventType.SUSPENSION_ALLOWED,
                "Token can be suspended",
                Collections.emptyList()
            ));

            return new GovernanceValidation(tokenId, true, "Suspension allowed");
        });
    }

    /**
     * Validate token reactivation
     */
    public Uni<GovernanceValidation> validateReactivation(String tokenId) {
        return Uni.createFrom().item(() -> {
            TokenHierarchy hierarchy = tokenHierarchy.get(tokenId);

            if (hierarchy == null || hierarchy.getStatus() != TokenStatus.SUSPENDED) {
                return new GovernanceValidation(tokenId, false, "Token not suspended");
            }

            // Check reactivation preconditions
            List<String> blockers = new ArrayList<>();

            if (hierarchy.hasFailedValidation()) {
                blockers.add("FAILED_VALIDATION");
            }

            if (hierarchy.isInDispute()) {
                blockers.add("IN_DISPUTE");
            }

            if (!blockers.isEmpty()) {
                return new GovernanceValidation(
                    tokenId,
                    false,
                    "Cannot reactivate: " + String.join(", ", blockers),
                    blockers
                );
            }

            governanceEvent.fire(new LifecycleGovernanceEvent(
                tokenId,
                GovernanceEventType.REACTIVATION_ALLOWED,
                "Token can be reactivated",
                Collections.emptyList()
            ));

            return new GovernanceValidation(tokenId, true, "Reactivation allowed");
        });
    }

    /**
     * Get blocking child tokens for primary token retirement
     */
    public Uni<List<String>> getBlockingChildTokens(String primaryTokenId) {
        return Uni.createFrom().item(() -> {
            TokenHierarchy hierarchy = tokenHierarchy.get(primaryTokenId);

            if (hierarchy == null) {
                return Collections.emptyList();
            }

            return hierarchy.getActiveSecondaryTokens();
        });
    }

    /**
     * Register token hierarchy (called when secondary token is created)
     */
    public void registerSecondaryToken(String primaryTokenId, String secondaryTokenId, TokenStatus status) {
        TokenHierarchy hierarchy = tokenHierarchy.computeIfAbsent(
            primaryTokenId,
            k -> new TokenHierarchy(primaryTokenId)
        );

        hierarchy.addSecondaryToken(secondaryTokenId, status);
        Log.debugf("Registered secondary token %s under primary %s", secondaryTokenId, primaryTokenId);
    }

    /**
     * Update secondary token status
     */
    public void updateSecondaryTokenStatus(String primaryTokenId, String secondaryTokenId, TokenStatus newStatus) {
        TokenHierarchy hierarchy = tokenHierarchy.get(primaryTokenId);

        if (hierarchy != null) {
            hierarchy.updateSecondaryTokenStatus(secondaryTokenId, newStatus);
            Log.debugf("Updated secondary token %s status to %s", secondaryTokenId, newStatus);
        }
    }

    // ============= INNER CLASSES =============

    public enum TokenStatus {
        CREATED,
        ACTIVE,
        REDEEMED,
        EXPIRED,
        SUSPENDED,
        RETIRED
    }

    public enum GovernanceEventType {
        RETIREMENT_BLOCKED,
        RETIREMENT_ALLOWED,
        SUSPENSION_ALLOWED,
        REACTIVATION_ALLOWED,
        CASCADE_OPERATION
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class GovernanceValidation {
        private String tokenId;
        private boolean isValid;
        private String message;
        private List<String> blockingTokens;

        public GovernanceValidation(String tokenId, boolean isValid, String message) {
            this(tokenId, isValid, message, Collections.emptyList());
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class LifecycleGovernanceEvent {
        private String tokenId;
        private GovernanceEventType eventType;
        private String details;
        private List<String> relatedTokens;
    }

    @lombok.Data
    public static class TokenHierarchy {
        private String primaryTokenId;
        private TokenStatus status = TokenStatus.CREATED;
        private final Map<String, TokenStatus> secondaryTokens = new ConcurrentHashMap<>();
        private final Set<String> activeTransactions = ConcurrentHashMap.newKeySet();
        private boolean failedValidation = false;
        private boolean inDispute = false;

        public TokenHierarchy(String primaryTokenId) {
            this.primaryTokenId = primaryTokenId;
        }

        public void addSecondaryToken(String secondaryTokenId, TokenStatus status) {
            secondaryTokens.put(secondaryTokenId, status);
        }

        public void updateSecondaryTokenStatus(String secondaryTokenId, TokenStatus newStatus) {
            secondaryTokens.put(secondaryTokenId, newStatus);
        }

        public List<String> getActiveSecondaryTokens() {
            return secondaryTokens.entrySet().stream()
                .filter(e -> e.getValue() != TokenStatus.REDEEMED &&
                           e.getValue() != TokenStatus.EXPIRED &&
                           e.getValue() != TokenStatus.RETIRED)
                .map(Map.Entry::getKey)
                .toList();
        }

        public boolean hasActiveTransactions() {
            return !activeTransactions.isEmpty();
        }

        public List<String> getActiveTransactionIds() {
            return new ArrayList<>(activeTransactions);
        }

        public void addActiveTransaction(String transactionId) {
            activeTransactions.add(transactionId);
        }

        public void removeActiveTransaction(String transactionId) {
            activeTransactions.remove(transactionId);
        }

        public void markFailedValidation() {
            this.failedValidation = true;
        }

        public void markInDispute() {
            this.inDispute = true;
        }

        public void clearDispute() {
            this.inDispute = false;
        }

        public boolean hasFailedValidation() {
            return failedValidation;
        }

        public boolean isInDispute() {
            return inDispute;
        }

        public TokenStatus getStatus() {
            return status;
        }

        public void setStatus(TokenStatus status) {
            this.status = status;
        }
    }
}
