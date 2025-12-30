package io.aurigraph.v11.compliance.erc3643;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ERC-3643 Transfer Manager - Enforces compliance rules during token transfers
 * Validates sender, receiver, and token eligibility before allowing transfers
 *
 * Compliance: SEC Regulation D, Regulation S, accredited investor requirements
 * Reference: https://eips.ethereum.org/EIPS/eip-3643
 */
@ApplicationScoped
public class TransferManager {

    @Inject
    IdentityRegistry identityRegistry;

    // Transfer rules per token
    private final Map<String, TransferRules> transferRules = new ConcurrentHashMap<>();

    // Approved transfer operators
    private final Set<String> approvedOperators = ConcurrentHashMap.newKeySet();

    // Transfer history for audit
    private final List<TransferRecord> transferHistory = Collections.synchronizedList(new ArrayList<>());

    // Statistics
    private final AtomicLong totalTransfers = new AtomicLong(0);
    private final AtomicLong rejectedTransfers = new AtomicLong(0);
    private final AtomicLong approvedTransfers = new AtomicLong(0);

    /**
     * Check if transfer is allowed
     */
    public TransferResult canTransfer(String tokenId, String from, String to, BigDecimal amount) {
        List<String> violations = new ArrayList<>();

        // Check sender identity
        if (!identityRegistry.isValidIdentity(from)) {
            violations.add("Sender not KYC verified");
        }

        // Check receiver identity
        if (!identityRegistry.isValidIdentity(to)) {
            violations.add("Receiver not KYC verified");
        }

        // Check if sender and receiver are in same country restriction
        IdentityRegistry.IdentityRecord senderIdentity = identityRegistry.getIdentity(from);
        IdentityRegistry.IdentityRecord receiverIdentity = identityRegistry.getIdentity(to);

        if (senderIdentity != null && receiverIdentity != null) {
            if (identityRegistry.isCountryRestricted(senderIdentity.getCountry())) {
                violations.add("Sender country restricted");
            }
            if (identityRegistry.isCountryRestricted(receiverIdentity.getCountry())) {
                violations.add("Receiver country restricted");
            }
        }

        // Check transfer rules for this token
        TransferRules rules = transferRules.getOrDefault(tokenId, new TransferRules(tokenId));

        // Check minimum and maximum transfer amounts
        if (amount.compareTo(rules.getMinimumTransferAmount()) < 0) {
            violations.add("Transfer amount below minimum: " +
                rules.getMinimumTransferAmount());
        }

        if (rules.getMaximumTransferAmount() != null &&
            amount.compareTo(rules.getMaximumTransferAmount()) > 0) {
            violations.add("Transfer amount exceeds maximum: " +
                rules.getMaximumTransferAmount());
        }

        // Check if transfer is whitelist-restricted
        if (rules.isWhitelistRequired()) {
            if (!rules.isWhitelisted(from) || !rules.isWhitelisted(to)) {
                violations.add("Sender or receiver not on whitelist");
            }
        }

        // Check if token is locked
        if (rules.isLocked()) {
            violations.add("Token is locked for transfers");
        }

        // Check accredited investor requirements if applicable
        if (rules.isAccreditedInvestorRequired()) {
            if (senderIdentity != null && !isAccreditedInvestor(senderIdentity)) {
                violations.add("Sender not accredited investor");
            }
            if (receiverIdentity != null && !isAccreditedInvestor(receiverIdentity)) {
                violations.add("Receiver not accredited investor");
            }
        }

        // Record the transfer attempt
        boolean allowed = violations.isEmpty();
        TransferRecord record = new TransferRecord(
            tokenId, from, to, amount, allowed,
            allowed ? null : String.join("; ", violations),
            Instant.now()
        );
        transferHistory.add(record);

        if (allowed) {
            approvedTransfers.incrementAndGet();
        } else {
            rejectedTransfers.incrementAndGet();
        }

        totalTransfers.incrementAndGet();

        return new TransferResult(allowed, violations);
    }

    /**
     * Execute a transfer (after compliance check)
     */
    public Uni<TransferRecord> executeTransfer(String tokenId, String from,
                                               String to, BigDecimal amount) {
        return Uni.createFrom().item(() -> {
            TransferResult result = canTransfer(tokenId, from, to, amount);

            if (!result.isAllowed()) {
                String violations = String.join("; ", result.getViolations());
                Log.warnf("Transfer rejected: %s", violations);
                throw new ComplianceException("Transfer not allowed: " + violations);
            }

            // Create transfer record
            TransferRecord record = new TransferRecord(
                tokenId, from, to, amount, true, null, Instant.now()
            );

            Log.infof("Transfer executed: %s from %s to %s, amount: %s",
                tokenId, from, to, amount);

            return record;
        });
    }

    /**
     * Add transfer rules for a token
     */
    public void setTransferRules(String tokenId, TransferRules rules) {
        transferRules.put(tokenId, rules);
        Log.infof("Transfer rules set for token: %s", tokenId);
    }

    /**
     * Get transfer rules for a token
     */
    public TransferRules getTransferRules(String tokenId) {
        return transferRules.getOrDefault(tokenId, new TransferRules(tokenId));
    }

    /**
     * Add address to whitelist
     */
    public void whitelistAddress(String tokenId, String address) {
        transferRules.computeIfAbsent(tokenId, TransferRules::new)
            .addToWhitelist(address);
        Log.infof("Address %s whitelisted for token %s", address, tokenId);
    }

    /**
     * Remove address from whitelist
     */
    public void removeFromWhitelist(String tokenId, String address) {
        TransferRules rules = transferRules.get(tokenId);
        if (rules != null) {
            rules.removeFromWhitelist(address);
        }
    }

    /**
     * Lock a token (prevent all transfers)
     */
    public void lockToken(String tokenId) {
        transferRules.computeIfAbsent(tokenId, TransferRules::new)
            .setLocked(true);
        Log.warnf("Token locked: %s", tokenId);
    }

    /**
     * Unlock a token
     */
    public void unlockToken(String tokenId) {
        TransferRules rules = transferRules.get(tokenId);
        if (rules != null) {
            rules.setLocked(false);
        }
    }

    /**
     * Approve a transfer operator
     */
    public void approveOperator(String operator) {
        approvedOperators.add(operator);
        Log.infof("Operator approved: %s", operator);
    }

    /**
     * Revoke operator approval
     */
    public void revokeOperator(String operator) {
        approvedOperators.remove(operator);
        Log.infof("Operator revoked: %s", operator);
    }

    /**
     * Check if operator is approved
     */
    public boolean isApprovedOperator(String operator) {
        return approvedOperators.contains(operator);
    }

    /**
     * Get transfer history
     */
    public List<TransferRecord> getTransferHistory() {
        return new ArrayList<>(transferHistory);
    }

    /**
     * Get transfer history for specific token
     */
    public List<TransferRecord> getTokenTransferHistory(String tokenId) {
        return transferHistory.stream()
            .filter(record -> record.getTokenId().equals(tokenId))
            .toList();
    }

    /**
     * Get transfer statistics
     */
    public TransferStats getStats() {
        return new TransferStats(
            totalTransfers.get(),
            approvedTransfers.get(),
            rejectedTransfers.get(),
            transferHistory.size()
        );
    }

    /**
     * Check if identity is accredited investor
     */
    private boolean isAccreditedInvestor(IdentityRegistry.IdentityRecord identity) {
        // Implementation: Check accredited investor status
        // Could check against KYC level or additional registry
        return "CERTIFIED".equals(identity.getKycLevel()) ||
               "ENHANCED".equals(identity.getKycLevel());
    }

    // Inner classes

    /**
     * Transfer result with violations list
     */
    public static class TransferResult {
        private final boolean allowed;
        private final List<String> violations;

        public TransferResult(boolean allowed, List<String> violations) {
            this.allowed = allowed;
            this.violations = violations;
        }

        public boolean isAllowed() { return allowed; }
        public List<String> getViolations() { return violations; }
    }

    /**
     * Transfer rules for a token
     */
    public static class TransferRules {
        private final String tokenId;
        private BigDecimal minimumTransferAmount = BigDecimal.ZERO;
        private BigDecimal maximumTransferAmount = null;
        private final Set<String> whitelist = ConcurrentHashMap.newKeySet();
        private boolean whitelistRequired = false;
        private boolean locked = false;
        private boolean accreditedInvestorRequired = false;

        public TransferRules(String tokenId) {
            this.tokenId = tokenId;
        }

        public String getTokenId() { return tokenId; }
        public BigDecimal getMinimumTransferAmount() { return minimumTransferAmount; }
        public void setMinimumTransferAmount(BigDecimal amount) {
            this.minimumTransferAmount = amount;
        }
        public BigDecimal getMaximumTransferAmount() { return maximumTransferAmount; }
        public void setMaximumTransferAmount(BigDecimal amount) {
            this.maximumTransferAmount = amount;
        }
        public boolean isWhitelistRequired() { return whitelistRequired; }
        public void setWhitelistRequired(boolean required) {
            this.whitelistRequired = required;
        }
        public void addToWhitelist(String address) { whitelist.add(address); }
        public void removeFromWhitelist(String address) { whitelist.remove(address); }
        public boolean isWhitelisted(String address) { return whitelist.contains(address); }
        public boolean isLocked() { return locked; }
        public void setLocked(boolean locked) { this.locked = locked; }
        public boolean isAccreditedInvestorRequired() { return accreditedInvestorRequired; }
        public void setAccreditedInvestorRequired(boolean required) {
            this.accreditedInvestorRequired = required;
        }
    }

    /**
     * Record of a transfer attempt
     */
    public static class TransferRecord {
        private final String tokenId;
        private final String from;
        private final String to;
        private final BigDecimal amount;
        private final boolean success;
        private final String rejectReason;
        private final Instant timestamp;

        public TransferRecord(String tokenId, String from, String to,
                             BigDecimal amount, boolean success, String rejectReason,
                             Instant timestamp) {
            this.tokenId = tokenId;
            this.from = from;
            this.to = to;
            this.amount = amount;
            this.success = success;
            this.rejectReason = rejectReason;
            this.timestamp = timestamp;
        }

        public String getTokenId() { return tokenId; }
        public String getFrom() { return from; }
        public String getTo() { return to; }
        public BigDecimal getAmount() { return amount; }
        public boolean isSuccess() { return success; }
        public String getRejectReason() { return rejectReason; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Transfer statistics
     */
    public static class TransferStats {
        private final long totalTransfers;
        private final long approvedTransfers;
        private final long rejectedTransfers;
        private final int historySize;

        public TransferStats(long totalTransfers, long approvedTransfers,
                            long rejectedTransfers, int historySize) {
            this.totalTransfers = totalTransfers;
            this.approvedTransfers = approvedTransfers;
            this.rejectedTransfers = rejectedTransfers;
            this.historySize = historySize;
        }

        public long getTotalTransfers() { return totalTransfers; }
        public long getApprovedTransfers() { return approvedTransfers; }
        public long getRejectedTransfers() { return rejectedTransfers; }
        public int getHistorySize() { return historySize; }
        public double getApprovalRate() {
            return totalTransfers == 0 ? 0 : (approvedTransfers * 100.0) / totalTransfers;
        }
    }
}
