package io.aurigraph.v11.wallet;

import io.aurigraph.v11.crypto.DilithiumSignatureService;
import io.aurigraph.v11.crypto.DilithiumSignatureService.MultiSignature;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Multi-Signature Wallet Service
 *
 * Implements enterprise multi-signature wallet functionality with quantum-resistant
 * signatures. Supports m-of-n threshold signatures and role-based signing.
 *
 * Features:
 * - Multi-sig wallet creation (2-of-3, 3-of-5, m-of-n)
 * - Transaction proposal and approval workflow
 * - Role-based signing (CEO, CFO, etc.)
 * - Quantum-resistant Dilithium signatures
 *
 * @author SCA (Security & Cryptography Agent)
 * @version 11.0.0
 * @since Sprint 8 - Governance & Quantum Signing
 */
@ApplicationScoped
public class MultiSigWalletService {

    private static final Logger LOG = Logger.getLogger(MultiSigWalletService.class);

    @Inject
    DilithiumSignatureService signatureService;

    // In-memory storage (in production, would use database)
    private final Map<String, MultiSigWallet> wallets = new ConcurrentHashMap<>();
    private final Map<String, TransactionProposal> proposals = new ConcurrentHashMap<>();

    // Performance metrics
    private long walletCount = 0;
    private long proposalCount = 0;
    private long executionCount = 0;

    /**
     * Create a new multi-signature wallet
     *
     * @param name Wallet name
     * @param signers List of signer addresses
     * @param threshold Required number of signatures (m)
     * @param roles Optional role assignments
     * @return Created wallet
     */
    public Uni<MultiSigWallet> createWallet(String name, List<String> signers,
                                           int threshold, Map<String, String> roles) {
        return Uni.createFrom().item(() -> {
            // Validate inputs
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Wallet name cannot be empty");
            }

            if (signers == null || signers.isEmpty()) {
                throw new IllegalArgumentException("Signers list cannot be empty");
            }

            if (threshold < 1 || threshold > signers.size()) {
                throw new IllegalArgumentException("Threshold must be between 1 and " + signers.size());
            }

            // Generate wallet ID
            String walletId = "WALLET-" + System.currentTimeMillis() + "-" +
                            UUID.randomUUID().toString().substring(0, 8);

            // Create wallet
            MultiSigWallet wallet = new MultiSigWallet(
                walletId,
                name,
                new ArrayList<>(signers),
                threshold,
                BigDecimal.ZERO,
                roles != null ? new HashMap<>(roles) : new HashMap<>()
            );

            wallet.setCreationTime(Instant.now());
            wallet.setActive(true);

            // Store wallet
            wallets.put(walletId, wallet);
            walletCount++;

            LOG.infof("Created multi-sig wallet %s: %s (%d-of-%d)",
                     walletId, name, threshold, signers.size());

            return wallet;

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Add a signer to the wallet
     *
     * @param walletId Wallet ID
     * @param newSigner New signer address
     * @param requesterSignatures Signatures from existing signers approving the addition
     * @return Updated wallet
     */
    public Uni<MultiSigWallet> addSigner(String walletId, String newSigner,
                                        MultiSignature requesterSignatures) {
        return Uni.createFrom().item(() -> {
            MultiSigWallet wallet = wallets.get(walletId);
            if (wallet == null) {
                throw new IllegalArgumentException("Wallet not found: " + walletId);
            }

            if (!wallet.isActive()) {
                throw new IllegalStateException("Wallet is not active");
            }

            // Verify that enough signers approved this addition
            // (in production, would verify signatures against signer public keys)
            if (requesterSignatures.getSignatureCount() < wallet.getThreshold()) {
                throw new IllegalStateException("Insufficient signatures for adding signer");
            }

            // Check if signer already exists
            if (wallet.getSigners().contains(newSigner)) {
                throw new IllegalArgumentException("Signer already exists in wallet");
            }

            // Add signer
            wallet.getSigners().add(newSigner);

            LOG.infof("Added signer %s to wallet %s", newSigner, walletId);

            return wallet;

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Remove a signer from the wallet
     *
     * @param walletId Wallet ID
     * @param signerToRemove Signer to remove
     * @param requesterSignatures Signatures from other signers approving the removal
     * @return Updated wallet
     */
    public Uni<MultiSigWallet> removeSigner(String walletId, String signerToRemove,
                                           MultiSignature requesterSignatures) {
        return Uni.createFrom().item(() -> {
            MultiSigWallet wallet = wallets.get(walletId);
            if (wallet == null) {
                throw new IllegalArgumentException("Wallet not found: " + walletId);
            }

            if (!wallet.isActive()) {
                throw new IllegalStateException("Wallet is not active");
            }

            // Verify signatures
            if (requesterSignatures.getSignatureCount() < wallet.getThreshold()) {
                throw new IllegalStateException("Insufficient signatures for removing signer");
            }

            // Check if removing would violate threshold
            if (wallet.getSigners().size() - 1 < wallet.getThreshold()) {
                throw new IllegalStateException("Cannot remove signer: would violate threshold requirement");
            }

            // Remove signer
            boolean removed = wallet.getSigners().remove(signerToRemove);
            if (!removed) {
                throw new IllegalArgumentException("Signer not found in wallet");
            }

            LOG.infof("Removed signer %s from wallet %s", signerToRemove, walletId);

            return wallet;

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Create a transaction proposal
     *
     * @param walletId Wallet ID
     * @param recipient Recipient address
     * @param amount Transaction amount
     * @param description Transaction description
     * @param proposer Proposer address
     * @return Created proposal
     */
    public Uni<TransactionProposal> proposeTransaction(String walletId, String recipient,
                                                      BigDecimal amount, String description,
                                                      String proposer) {
        return Uni.createFrom().item(() -> {
            MultiSigWallet wallet = wallets.get(walletId);
            if (wallet == null) {
                throw new IllegalArgumentException("Wallet not found: " + walletId);
            }

            if (!wallet.isActive()) {
                throw new IllegalStateException("Wallet is not active");
            }

            // Verify proposer is a signer
            if (!wallet.getSigners().contains(proposer)) {
                throw new IllegalArgumentException("Proposer is not a signer of this wallet");
            }

            // Check wallet balance
            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient wallet balance");
            }

            // Generate proposal ID
            String proposalId = "TXPROP-" + System.currentTimeMillis() + "-" +
                              UUID.randomUUID().toString().substring(0, 8);

            // Create proposal
            TransactionProposal proposal = new TransactionProposal(
                proposalId,
                walletId,
                recipient,
                amount,
                description,
                proposer,
                wallet.getThreshold()
            );

            proposal.setProposalTime(Instant.now());
            proposal.setStatus(ProposalStatus.PENDING);

            // Store proposal
            proposals.put(proposalId, proposal);
            proposalCount++;

            LOG.infof("Created transaction proposal %s: %s -> %s, amount: %s",
                     proposalId, walletId, recipient, amount);

            return proposal;

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Approve a transaction proposal
     *
     * @param proposalId Proposal ID
     * @param approver Approver address
     * @param privateKey Approver's private key for signing
     * @return Updated proposal
     */
    public Uni<TransactionProposal> approveProposal(String proposalId, String approver,
                                                   PrivateKey privateKey) {
        return Uni.createFrom().item(() -> {
            TransactionProposal proposal = proposals.get(proposalId);
            if (proposal == null) {
                throw new IllegalArgumentException("Proposal not found: " + proposalId);
            }

            if (proposal.getStatus() != ProposalStatus.PENDING) {
                throw new IllegalStateException("Proposal is not in pending status");
            }

            // Get wallet
            MultiSigWallet wallet = wallets.get(proposal.getWalletId());
            if (wallet == null) {
                throw new IllegalArgumentException("Wallet not found: " + proposal.getWalletId());
            }

            // Verify approver is a signer
            if (!wallet.getSigners().contains(approver)) {
                throw new IllegalArgumentException("Approver is not a signer of this wallet");
            }

            // Check if already approved
            if (proposal.getApprovers().contains(approver)) {
                throw new IllegalStateException("Already approved by this signer");
            }

            // Generate signature
            byte[] proposalData = (proposalId + proposal.getRecipient() + proposal.getAmount()).getBytes();
            byte[] signature = signatureService.sign(proposalData, privateKey);

            // Add approval
            proposal.getApprovers().add(approver);
            proposal.getSignatures().put(approver, signature);

            LOG.infof("Proposal %s approved by %s (%d/%d approvals)",
                     proposalId, approver, proposal.getApprovers().size(), proposal.getRequiredApprovals());

            // Check if threshold met
            if (proposal.getApprovers().size() >= proposal.getRequiredApprovals()) {
                proposal.setStatus(ProposalStatus.APPROVED);
                LOG.infof("Proposal %s has reached approval threshold", proposalId);
            }

            return proposal;

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Execute an approved transaction proposal
     *
     * @param proposalId Proposal ID
     * @return Execution result
     */
    public Uni<TransactionExecution> executeProposal(String proposalId) {
        return Uni.createFrom().item(() -> {
            TransactionProposal proposal = proposals.get(proposalId);
            if (proposal == null) {
                throw new IllegalArgumentException("Proposal not found: " + proposalId);
            }

            if (proposal.getStatus() != ProposalStatus.APPROVED) {
                throw new IllegalStateException("Proposal is not approved");
            }

            // Get wallet
            MultiSigWallet wallet = wallets.get(proposal.getWalletId());
            if (wallet == null) {
                throw new IllegalArgumentException("Wallet not found: " + proposal.getWalletId());
            }

            // Verify wallet balance
            if (wallet.getBalance().compareTo(proposal.getAmount()) < 0) {
                proposal.setStatus(ProposalStatus.FAILED);
                throw new IllegalStateException("Insufficient wallet balance");
            }

            try {
                // Execute transaction (in production, would interact with blockchain)
                wallet.setBalance(wallet.getBalance().subtract(proposal.getAmount()));

                // Update proposal status
                proposal.setStatus(ProposalStatus.EXECUTED);
                proposal.setExecutionTime(Instant.now());

                executionCount++;

                LOG.infof("Executed proposal %s: sent %s to %s",
                         proposalId, proposal.getAmount(), proposal.getRecipient());

                String txHash = "0x" + UUID.randomUUID().toString().replace("-", "");
                return new TransactionExecution(
                    txHash,
                    proposalId,
                    proposal.getWalletId(),
                    proposal.getRecipient(),
                    proposal.getAmount(),
                    Instant.now(),
                    true,
                    "Transaction executed successfully"
                );

            } catch (Exception e) {
                proposal.setStatus(ProposalStatus.FAILED);
                LOG.errorf(e, "Failed to execute proposal %s", proposalId);

                return new TransactionExecution(
                    null,
                    proposalId,
                    proposal.getWalletId(),
                    proposal.getRecipient(),
                    proposal.getAmount(),
                    Instant.now(),
                    false,
                    "Execution failed: " + e.getMessage()
                );
            }

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get wallet by ID
     */
    public Uni<MultiSigWallet> getWallet(String walletId) {
        return Uni.createFrom().item(() -> {
            MultiSigWallet wallet = wallets.get(walletId);
            if (wallet == null) {
                throw new IllegalArgumentException("Wallet not found: " + walletId);
            }
            return wallet;
        });
    }

    /**
     * List all wallets
     */
    public Uni<List<MultiSigWallet>> listWallets() {
        return Uni.createFrom().item(() -> new ArrayList<>(wallets.values()));
    }

    /**
     * Get proposal by ID
     */
    public Uni<TransactionProposal> getProposal(String proposalId) {
        return Uni.createFrom().item(() -> {
            TransactionProposal proposal = proposals.get(proposalId);
            if (proposal == null) {
                throw new IllegalArgumentException("Proposal not found: " + proposalId);
            }
            return proposal;
        });
    }

    /**
     * List proposals for a wallet
     */
    public Uni<List<TransactionProposal>> listProposals(String walletId, ProposalStatus status) {
        return Uni.createFrom().item(() -> {
            return proposals.values().stream()
                    .filter(p -> p.getWalletId().equals(walletId))
                    .filter(p -> status == null || p.getStatus() == status)
                    .sorted((p1, p2) -> p2.getProposalTime().compareTo(p1.getProposalTime()))
                    .collect(Collectors.toList());
        });
    }

    /**
     * Get service metrics
     */
    public MultiSigMetrics getMetrics() {
        int activeWallets = (int) wallets.values().stream()
                .filter(MultiSigWallet::isActive)
                .count();

        int pendingProposals = (int) proposals.values().stream()
                .filter(p -> p.getStatus() == ProposalStatus.PENDING)
                .count();

        return new MultiSigMetrics(
            walletCount,
            proposalCount,
            executionCount,
            activeWallets,
            pendingProposals
        );
    }

    // ==================== Data Models ====================

    /**
     * Multi-signature wallet
     */
    public static class MultiSigWallet {
        private final String id;
        private String name;
        private List<String> signers;
        private int threshold;
        private BigDecimal balance;
        private Map<String, String> roles;
        private Instant creationTime;
        private boolean active;

        public MultiSigWallet(String id, String name, List<String> signers, int threshold,
                            BigDecimal balance, Map<String, String> roles) {
            this.id = id;
            this.name = name;
            this.signers = signers;
            this.threshold = threshold;
            this.balance = balance;
            this.roles = roles;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getSigners() { return signers; }
        public void setSigners(List<String> signers) { this.signers = signers; }
        public int getThreshold() { return threshold; }
        public void setThreshold(int threshold) { this.threshold = threshold; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
        public Map<String, String> getRoles() { return roles; }
        public void setRoles(Map<String, String> roles) { this.roles = roles; }
        public Instant getCreationTime() { return creationTime; }
        public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    /**
     * Transaction proposal
     */
    public static class TransactionProposal {
        private final String id;
        private final String walletId;
        private final String recipient;
        private final BigDecimal amount;
        private String description;
        private String proposer;
        private int requiredApprovals;
        private List<String> approvers;
        private Map<String, byte[]> signatures;
        private Instant proposalTime;
        private Instant executionTime;
        private ProposalStatus status;

        public TransactionProposal(String id, String walletId, String recipient, BigDecimal amount,
                                  String description, String proposer, int requiredApprovals) {
            this.id = id;
            this.walletId = walletId;
            this.recipient = recipient;
            this.amount = amount;
            this.description = description;
            this.proposer = proposer;
            this.requiredApprovals = requiredApprovals;
            this.approvers = new ArrayList<>();
            this.signatures = new HashMap<>();
        }

        public String getId() { return id; }
        public String getWalletId() { return walletId; }
        public String getRecipient() { return recipient; }
        public BigDecimal getAmount() { return amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getProposer() { return proposer; }
        public void setProposer(String proposer) { this.proposer = proposer; }
        public int getRequiredApprovals() { return requiredApprovals; }
        public void setRequiredApprovals(int requiredApprovals) { this.requiredApprovals = requiredApprovals; }
        public List<String> getApprovers() { return approvers; }
        public void setApprovers(List<String> approvers) { this.approvers = approvers; }
        public Map<String, byte[]> getSignatures() { return signatures; }
        public void setSignatures(Map<String, byte[]> signatures) { this.signatures = signatures; }
        public Instant getProposalTime() { return proposalTime; }
        public void setProposalTime(Instant proposalTime) { this.proposalTime = proposalTime; }
        public Instant getExecutionTime() { return executionTime; }
        public void setExecutionTime(Instant executionTime) { this.executionTime = executionTime; }
        public ProposalStatus getStatus() { return status; }
        public void setStatus(ProposalStatus status) { this.status = status; }
    }

    /**
     * Transaction execution result
     */
    public record TransactionExecution(
        String transactionHash,
        String proposalId,
        String walletId,
        String recipient,
        BigDecimal amount,
        Instant executionTime,
        boolean success,
        String message
    ) {}

    /**
     * Multi-sig wallet metrics
     */
    public record MultiSigMetrics(
        long totalWallets,
        long totalProposals,
        long totalExecutions,
        int activeWallets,
        int pendingProposals
    ) {}

    /**
     * Proposal status
     */
    public enum ProposalStatus {
        PENDING,    // Awaiting approvals
        APPROVED,   // Threshold met, ready for execution
        EXECUTED,   // Successfully executed
        REJECTED,   // Explicitly rejected
        FAILED,     // Execution failed
        EXPIRED     // Expired without execution
    }
}
