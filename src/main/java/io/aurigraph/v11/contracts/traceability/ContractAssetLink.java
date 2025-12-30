package io.aurigraph.v11.contracts.traceability;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Contract-to-Asset Traceability Link
 *
 * Creates bidirectional links between ActiveContracts and underlying RWA assets.
 * Tracks the full lineage from contract deployment through asset tokenization.
 *
 * @version 1.0.0
 * @author Aurigraph V11 Development Team
 */
public class ContractAssetLink {

    @JsonProperty("linkId")
    private String linkId;

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("contractName")
    private String contractName;

    @JsonProperty("contractStatus")
    private String contractStatus;

    // Asset References
    @JsonProperty("assetId")
    private String assetId;

    @JsonProperty("assetType")
    private String assetType;

    @JsonProperty("assetName")
    private String assetName;

    @JsonProperty("assetValuation")
    private Double assetValuation;

    @JsonProperty("assetCurrency")
    private String assetCurrency;

    // Tokenization Details
    @JsonProperty("tokenId")
    private String tokenId;

    @JsonProperty("tokenSymbol")
    private String tokenSymbol;

    @JsonProperty("totalShares")
    private Long totalShares;

    @JsonProperty("sharesOutstanding")
    private Long sharesOutstanding;

    // Parties Involved
    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("custodian")
    private String custodian;

    @JsonProperty("assetManager")
    private String assetManager;

    // Lifecycle
    @JsonProperty("linkedAt")
    private Instant linkedAt;

    @JsonProperty("tokenizedAt")
    private Instant tokenizedAt;

    @JsonProperty("completedAt")
    private Instant completedAt;

    @JsonProperty("lastUpdatedAt")
    private Instant lastUpdatedAt;

    // Execution Metrics
    @JsonProperty("executionCount")
    private Long executionCount = 0L;

    @JsonProperty("failureCount")
    private Long failureCount = 0L;

    @JsonProperty("successRate")
    private Double successRate = 100.0;

    // Compliance & Metadata
    @JsonProperty("complianceStatus")
    private String complianceStatus;

    @JsonProperty("riskLevel")
    private String riskLevel;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = new HashMap<>();

    // Constructors
    public ContractAssetLink() {
        this.linkedAt = Instant.now();
    }

    public ContractAssetLink(String contractId, String assetId, String tokenId) {
        this();
        this.contractId = contractId;
        this.assetId = assetId;
        this.tokenId = tokenId;
        this.linkId = generateLinkId(contractId, assetId);
    }

    // Getters and Setters
    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(String contractStatus) {
        this.contractStatus = contractStatus;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public Double getAssetValuation() {
        return assetValuation;
    }

    public void setAssetValuation(Double assetValuation) {
        this.assetValuation = assetValuation;
    }

    public String getAssetCurrency() {
        return assetCurrency;
    }

    public void setAssetCurrency(String assetCurrency) {
        this.assetCurrency = assetCurrency;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenSymbol() {
        return tokenSymbol;
    }

    public void setTokenSymbol(String tokenSymbol) {
        this.tokenSymbol = tokenSymbol;
    }

    public Long getTotalShares() {
        return totalShares;
    }

    public void setTotalShares(Long totalShares) {
        this.totalShares = totalShares;
    }

    public Long getSharesOutstanding() {
        return sharesOutstanding;
    }

    public void setSharesOutstanding(Long sharesOutstanding) {
        this.sharesOutstanding = sharesOutstanding;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getCustodian() {
        return custodian;
    }

    public void setCustodian(String custodian) {
        this.custodian = custodian;
    }

    public String getAssetManager() {
        return assetManager;
    }

    public void setAssetManager(String assetManager) {
        this.assetManager = assetManager;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }

    public Instant getTokenizedAt() {
        return tokenizedAt;
    }

    public void setTokenizedAt(Instant tokenizedAt) {
        this.tokenizedAt = tokenizedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Long getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Long executionCount) {
        this.executionCount = executionCount;
    }

    public Long getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Long failureCount) {
        this.failureCount = failureCount;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public String getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Helper Methods
    private static String generateLinkId(String contractId, String assetId) {
        return String.format("LINK_%s_%s", contractId, assetId)
            .replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public void recordExecution(boolean success) {
        this.executionCount++;
        if (!success) {
            this.failureCount++;
        }
        this.successRate = ((executionCount - failureCount) * 100.0) / executionCount;
        this.lastUpdatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return String.format(
            "ContractAssetLink{linkId=%s, contractId=%s, assetId=%s, tokenId=%s, status=%s}",
            linkId, contractId, assetId, tokenId, complianceStatus
        );
    }
}
