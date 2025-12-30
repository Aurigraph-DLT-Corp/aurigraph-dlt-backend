package io.aurigraph.v11.registries.smartcontract;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Smart Contract Deployment Information Data Transfer Object
 *
 * Contains detailed information about contract deployment including network,
 * transaction, and execution metrics. Used for tracking contract deployment history
 * and verifying contract authenticity on the blockchain.
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
public class ContractDeploymentInfo {

    @JsonProperty("deploymentAddress")
    private String deploymentAddress; // Contract address on blockchain

    @JsonProperty("deploymentTxHash")
    private String deploymentTxHash; // Transaction hash of deployment

    @JsonProperty("deploymentDate")
    private Instant deploymentDate; // When contract was deployed

    @JsonProperty("deploymentNetwork")
    private String deploymentNetwork; // Network name (e.g., "mainnet", "testnet", "devnet")

    @JsonProperty("gasUsed")
    private long gasUsed; // Gas consumed during deployment

    @JsonProperty("codeHash")
    private String codeHash; // SHA-256 hash of compiled contract code

    @JsonProperty("compilerVersion")
    private String compilerVersion; // Version of compiler used

    @JsonProperty("gasPrice")
    private long gasPrice; // Gas price at time of deployment

    @JsonProperty("deployer")
    private String deployer; // Address/account that deployed contract

    @JsonProperty("totalCost")
    private double totalCost; // Total deployment cost in network currency

    @JsonProperty("blockHeight")
    private long blockHeight; // Block number where contract was deployed

    @JsonProperty("confirmations")
    private int confirmations; // Number of block confirmations

    @JsonProperty("status")
    private String status; // Deployment status (success, failed, pending)

    // Constructors

    /**
     * Default constructor
     */
    public ContractDeploymentInfo() {
        this.deploymentDate = Instant.now();
        this.status = "PENDING";
        this.confirmations = 0;
    }

    /**
     * Full constructor with all deployment details
     */
    public ContractDeploymentInfo(
            String deploymentAddress,
            String deploymentTxHash,
            String deploymentNetwork,
            long gasUsed,
            String codeHash,
            String compilerVersion) {
        this();
        this.deploymentAddress = deploymentAddress;
        this.deploymentTxHash = deploymentTxHash;
        this.deploymentNetwork = deploymentNetwork;
        this.gasUsed = gasUsed;
        this.codeHash = codeHash;
        this.compilerVersion = compilerVersion;
    }

    // Getters and Setters

    public String getDeploymentAddress() {
        return deploymentAddress;
    }

    public void setDeploymentAddress(String deploymentAddress) {
        this.deploymentAddress = deploymentAddress;
    }

    public String getDeploymentTxHash() {
        return deploymentTxHash;
    }

    public void setDeploymentTxHash(String deploymentTxHash) {
        this.deploymentTxHash = deploymentTxHash;
    }

    public Instant getDeploymentDate() {
        return deploymentDate;
    }

    public void setDeploymentDate(Instant deploymentDate) {
        this.deploymentDate = deploymentDate;
    }

    public String getDeploymentNetwork() {
        return deploymentNetwork;
    }

    public void setDeploymentNetwork(String deploymentNetwork) {
        this.deploymentNetwork = deploymentNetwork;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = gasUsed;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public String getCompilerVersion() {
        return compilerVersion;
    }

    public void setCompilerVersion(String compilerVersion) {
        this.compilerVersion = compilerVersion;
    }

    public long getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(long gasPrice) {
        this.gasPrice = gasPrice;
    }

    public String getDeployer() {
        return deployer;
    }

    public void setDeployer(String deployer) {
        this.deployer = deployer;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(int confirmations) {
        this.confirmations = confirmations;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Update deployment status to successful
     */
    public void markAsSuccessful() {
        this.status = "SUCCESS";
    }

    /**
     * Update deployment status to failed
     */
    public void markAsFailed() {
        this.status = "FAILED";
    }

    /**
     * Calculate total deployment cost based on gas metrics
     *
     * @return Calculated cost or pre-set cost
     */
    public double calculateCost() {
        if (gasUsed > 0 && gasPrice > 0) {
            return (gasUsed * gasPrice) / 1e18; // Assuming standard blockchain units
        }
        return totalCost;
    }

    /**
     * Check if deployment has sufficient confirmations
     *
     * @param requiredConfirmations Minimum confirmations needed
     * @return true if confirmations threshold met
     */
    public boolean hasRequiredConfirmations(int requiredConfirmations) {
        return confirmations >= requiredConfirmations;
    }

    /**
     * Verify contract code hash matches expected value
     *
     * @param expectedHash Expected code hash
     * @return true if hash matches
     */
    public boolean verifyCodeHash(String expectedHash) {
        if (codeHash == null || expectedHash == null) {
            return false;
        }
        return codeHash.equalsIgnoreCase(expectedHash);
    }

    @Override
    public String toString() {
        return String.format(
            "ContractDeployment{address='%s', hash='%s', network='%s', status='%s', confirmations=%d}",
            deploymentAddress, deploymentTxHash, deploymentNetwork, status, confirmations
        );
    }
}
