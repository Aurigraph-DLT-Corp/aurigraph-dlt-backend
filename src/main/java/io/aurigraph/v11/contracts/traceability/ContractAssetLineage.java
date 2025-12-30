package io.aurigraph.v11.contracts.traceability;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Contract Asset Lineage DTO
 *
 * Represents the complete lineage from an ActiveContract through its linked assets
 * and tokenized shares, with aggregated metrics across all assets.
 *
 * @version 1.0.0
 * @author Aurigraph V11 Development Team
 */
public class ContractAssetLineage {

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("assets")
    private List<ContractAssetLink> assets;

    @JsonProperty("totalAssetValuation")
    private Double totalAssetValuation;

    @JsonProperty("totalTokensIssued")
    private Long totalTokensIssued;

    // Constructors
    public ContractAssetLineage() {
    }

    public ContractAssetLineage(String contractId) {
        this.contractId = contractId;
    }

    // Getters and Setters
    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public List<ContractAssetLink> getAssets() {
        return assets;
    }

    public void setAssets(List<ContractAssetLink> assets) {
        this.assets = assets;
    }

    public Double getTotalAssetValuation() {
        return totalAssetValuation;
    }

    public void setTotalAssetValuation(Double totalAssetValuation) {
        this.totalAssetValuation = totalAssetValuation;
    }

    public Long getTotalTokensIssued() {
        return totalTokensIssued;
    }

    public void setTotalTokensIssued(Long totalTokensIssued) {
        this.totalTokensIssued = totalTokensIssued;
    }

    @Override
    public String toString() {
        return String.format(
            "ContractAssetLineage{contractId=%s, assetCount=%d, totalValuation=%s, totalTokens=%d}",
            contractId,
            assets != null ? assets.size() : 0,
            totalAssetValuation,
            totalTokensIssued
        );
    }
}
