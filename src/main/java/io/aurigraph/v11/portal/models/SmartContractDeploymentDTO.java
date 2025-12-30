package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class SmartContractDeploymentDTO {
    @JsonProperty("total_deployments")
    private Integer totalDeployments;
    @JsonProperty("active_contracts")
    private Integer activeContracts;
    @JsonProperty("paused_contracts")
    private Integer pausedContracts;
    @JsonProperty("deprecated_contracts")
    private Integer deprecatedContracts;
    @JsonProperty("total_deployment_cost")
    private String totalDeploymentCost;
    @JsonProperty("average_deployment_cost")
    private String averageDeploymentCost;
    @JsonProperty("average_gas_used")
    private Integer averageGasUsed;
    @JsonProperty("deployments_last24h")
    private Integer deploymentsLast24h;
    @JsonProperty("deployments_last7d")
    private Integer deploymentsLast7d;
    @JsonProperty("top_deployed_template")
    private String topDeployedTemplate;
    @JsonProperty("success_rate")
    private Double successRate;
    @JsonProperty("avg_verification_time")
    private Integer avgVerificationTime;
    @JsonProperty("security_audits_passed")
    private Integer securityAuditsPassed;
    @JsonProperty("security_audits_failed")
    private Integer securityAuditsFailed;
    @JsonProperty("average_contract_size")
    private Double averageContractSize;
    @JsonProperty("error")
    private String error;

    public SmartContractDeploymentDTO() {}

    private SmartContractDeploymentDTO(Builder builder) {
        this.totalDeployments = builder.totalDeployments;
        this.activeContracts = builder.activeContracts;
        this.pausedContracts = builder.pausedContracts;
        this.deprecatedContracts = builder.deprecatedContracts;
        this.totalDeploymentCost = builder.totalDeploymentCost;
        this.averageDeploymentCost = builder.averageDeploymentCost;
        this.averageGasUsed = builder.averageGasUsed;
        this.deploymentsLast24h = builder.deploymentsLast24h;
        this.deploymentsLast7d = builder.deploymentsLast7d;
        this.topDeployedTemplate = builder.topDeployedTemplate;
        this.successRate = builder.successRate;
        this.avgVerificationTime = builder.avgVerificationTime;
        this.securityAuditsPassed = builder.securityAuditsPassed;
        this.securityAuditsFailed = builder.securityAuditsFailed;
        this.averageContractSize = builder.averageContractSize;
        this.error = builder.error;
    }

    public Integer getTotalDeployments() { return totalDeployments; }
    public Integer getActiveContracts() { return activeContracts; }
    public Integer getPausedContracts() { return pausedContracts; }
    public Integer getDeprecatedContracts() { return deprecatedContracts; }
    public String getTotalDeploymentCost() { return totalDeploymentCost; }
    public String getAverageDeploymentCost() { return averageDeploymentCost; }
    public Integer getAverageGasUsed() { return averageGasUsed; }
    public Integer getDeploymentsLast24h() { return deploymentsLast24h; }
    public Integer getDeploymentsLast7d() { return deploymentsLast7d; }
    public String getTopDeployedTemplate() { return topDeployedTemplate; }
    public Double getSuccessRate() { return successRate; }
    public Integer getAvgVerificationTime() { return avgVerificationTime; }
    public Integer getSecurityAuditsPassed() { return securityAuditsPassed; }
    public Integer getSecurityAuditsFailed() { return securityAuditsFailed; }
    public Double getAverageContractSize() { return averageContractSize; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer totalDeployments;
        private Integer activeContracts;
        private Integer pausedContracts;
        private Integer deprecatedContracts;
        private String totalDeploymentCost;
        private String averageDeploymentCost;
        private Integer averageGasUsed;
        private Integer deploymentsLast24h;
        private Integer deploymentsLast7d;
        private String topDeployedTemplate;
        private Double successRate;
        private Integer avgVerificationTime;
        private Integer securityAuditsPassed;
        private Integer securityAuditsFailed;
        private Double averageContractSize;
        private String error;

        public Builder totalDeployments(Integer totalDeployments) { this.totalDeployments = totalDeployments; return this; }
        public Builder activeContracts(Integer activeContracts) { this.activeContracts = activeContracts; return this; }
        public Builder pausedContracts(Integer pausedContracts) { this.pausedContracts = pausedContracts; return this; }
        public Builder deprecatedContracts(Integer deprecatedContracts) { this.deprecatedContracts = deprecatedContracts; return this; }
        public Builder totalDeploymentCost(String totalDeploymentCost) { this.totalDeploymentCost = totalDeploymentCost; return this; }
        public Builder averageDeploymentCost(String averageDeploymentCost) { this.averageDeploymentCost = averageDeploymentCost; return this; }
        public Builder averageGasUsed(Integer averageGasUsed) { this.averageGasUsed = averageGasUsed; return this; }
        public Builder deploymentsLast24h(Integer deploymentsLast24h) { this.deploymentsLast24h = deploymentsLast24h; return this; }
        public Builder deploymentsLast7d(Integer deploymentsLast7d) { this.deploymentsLast7d = deploymentsLast7d; return this; }
        public Builder topDeployedTemplate(String topDeployedTemplate) { this.topDeployedTemplate = topDeployedTemplate; return this; }
        public Builder successRate(Double successRate) { this.successRate = successRate; return this; }
        public Builder avgVerificationTime(Integer avgVerificationTime) { this.avgVerificationTime = avgVerificationTime; return this; }
        public Builder securityAuditsPassed(Integer securityAuditsPassed) { this.securityAuditsPassed = securityAuditsPassed; return this; }
        public Builder securityAuditsFailed(Integer securityAuditsFailed) { this.securityAuditsFailed = securityAuditsFailed; return this; }
        public Builder averageContractSize(Double averageContractSize) { this.averageContractSize = averageContractSize; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public SmartContractDeploymentDTO build() { return new SmartContractDeploymentDTO(this); }
    }
}
