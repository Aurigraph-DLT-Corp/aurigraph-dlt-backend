package io.aurigraph.v11.contracts.models;

import java.math.BigDecimal;
import java.time.Instant;

public class ContractMetrics {

    private long totalContracts;
    private long activeContracts;
    private long completedContracts;
    private long failedContracts;
    private BigDecimal totalValue;
    private BigDecimal averageExecutionTime;
    private BigDecimal successRate;
    private long totalExecutions;
    private long totalGasUsed;
    private Instant calculatedAt;

    // Constructors
    public ContractMetrics() {
    }

    public ContractMetrics(long totalContracts, long activeContracts, long completedContracts,
                          long failedContracts, BigDecimal totalValue, BigDecimal averageExecutionTime,
                          BigDecimal successRate, long totalExecutions, long totalGasUsed, Instant calculatedAt) {
        this.totalContracts = totalContracts;
        this.activeContracts = activeContracts;
        this.completedContracts = completedContracts;
        this.failedContracts = failedContracts;
        this.totalValue = totalValue;
        this.averageExecutionTime = averageExecutionTime;
        this.successRate = successRate;
        this.totalExecutions = totalExecutions;
        this.totalGasUsed = totalGasUsed;
        this.calculatedAt = calculatedAt;
    }

    // Getters
    public long getTotalContracts() {
        return totalContracts;
    }

    public long getActiveContracts() {
        return activeContracts;
    }

    public long getCompletedContracts() {
        return completedContracts;
    }

    public long getFailedContracts() {
        return failedContracts;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public BigDecimal getAverageExecutionTime() {
        return averageExecutionTime;
    }

    public BigDecimal getSuccessRate() {
        return successRate;
    }

    public long getTotalExecutions() {
        return totalExecutions;
    }

    public long getTotalGasUsed() {
        return totalGasUsed;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    // Setters
    public void setTotalContracts(long totalContracts) {
        this.totalContracts = totalContracts;
    }

    public void setActiveContracts(long activeContracts) {
        this.activeContracts = activeContracts;
    }

    public void setCompletedContracts(long completedContracts) {
        this.completedContracts = completedContracts;
    }

    public void setFailedContracts(long failedContracts) {
        this.failedContracts = failedContracts;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public void setAverageExecutionTime(BigDecimal averageExecutionTime) {
        this.averageExecutionTime = averageExecutionTime;
    }

    public void setSuccessRate(BigDecimal successRate) {
        this.successRate = successRate;
    }

    public void setTotalExecutions(long totalExecutions) {
        this.totalExecutions = totalExecutions;
    }

    public void setTotalGasUsed(long totalGasUsed) {
        this.totalGasUsed = totalGasUsed;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalContracts;
        private long activeContracts;
        private long completedContracts;
        private long failedContracts;
        private BigDecimal totalValue = BigDecimal.ZERO;
        private BigDecimal averageExecutionTime = BigDecimal.ZERO;
        private BigDecimal successRate = BigDecimal.ZERO;
        private long totalExecutions;
        private long totalGasUsed;
        private Instant calculatedAt;

        public Builder totalContracts(long totalContracts) {
            this.totalContracts = totalContracts;
            return this;
        }

        public Builder activeContracts(long activeContracts) {
            this.activeContracts = activeContracts;
            return this;
        }

        public Builder completedContracts(long completedContracts) {
            this.completedContracts = completedContracts;
            return this;
        }

        public Builder failedContracts(long failedContracts) {
            this.failedContracts = failedContracts;
            return this;
        }

        public Builder totalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
            return this;
        }

        public Builder averageExecutionTime(BigDecimal averageExecutionTime) {
            this.averageExecutionTime = averageExecutionTime;
            return this;
        }

        public Builder successRate(BigDecimal successRate) {
            this.successRate = successRate;
            return this;
        }

        public Builder totalExecutions(long totalExecutions) {
            this.totalExecutions = totalExecutions;
            return this;
        }

        public Builder totalGasUsed(long totalGasUsed) {
            this.totalGasUsed = totalGasUsed;
            return this;
        }

        public Builder calculatedAt(Instant calculatedAt) {
            this.calculatedAt = calculatedAt;
            return this;
        }

        public ContractMetrics build() {
            return new ContractMetrics(
                totalContracts,
                activeContracts,
                completedContracts,
                failedContracts,
                totalValue,
                averageExecutionTime,
                successRate,
                totalExecutions,
                totalGasUsed,
                calculatedAt
            );
        }
    }
}