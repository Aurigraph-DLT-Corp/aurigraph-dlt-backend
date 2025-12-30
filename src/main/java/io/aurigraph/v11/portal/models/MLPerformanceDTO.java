package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class MLPerformanceDTO {
    @JsonProperty("model_version")
    private String modelVersion;
    @JsonProperty("performance_score")
    private Double performanceScore;
    @JsonProperty("consensus_optimization")
    private Double consensusOptimization;
    @JsonProperty("transaction_ordering")
    private Double transactionOrdering;
    @JsonProperty("block_proposal_efficiency")
    private Double blockProposalEfficiency;
    @JsonProperty("validator_selection_accuracy")
    private Double validatorSelectionAccuracy;
    @JsonProperty("prediction_latency")
    private Integer predictionLatency;
    @JsonProperty("inference_time")
    private Double inferenceTime;
    @JsonProperty("throughput_predictions")
    private Integer throughputPredictions;
    @JsonProperty("cpu_optimization")
    private Double cpuOptimization;
    @JsonProperty("memory_optimization")
    private Double memoryOptimization;
    @JsonProperty("energy_efficiency")
    private Double energyEfficiency;
    @JsonProperty("cost_savings")
    private String costSavings;
    @JsonProperty("cost_savings_percentage")
    private Double costSavingsPercentage;
    @JsonProperty("performance_gain")
    private String performanceGain;
    @JsonProperty("error")
    private String error;

    public MLPerformanceDTO() {}

    private MLPerformanceDTO(Builder builder) {
        this.modelVersion = builder.modelVersion;
        this.performanceScore = builder.performanceScore;
        this.consensusOptimization = builder.consensusOptimization;
        this.transactionOrdering = builder.transactionOrdering;
        this.blockProposalEfficiency = builder.blockProposalEfficiency;
        this.validatorSelectionAccuracy = builder.validatorSelectionAccuracy;
        this.predictionLatency = builder.predictionLatency;
        this.inferenceTime = builder.inferenceTime;
        this.throughputPredictions = builder.throughputPredictions;
        this.cpuOptimization = builder.cpuOptimization;
        this.memoryOptimization = builder.memoryOptimization;
        this.energyEfficiency = builder.energyEfficiency;
        this.costSavings = builder.costSavings;
        this.costSavingsPercentage = builder.costSavingsPercentage;
        this.performanceGain = builder.performanceGain;
        this.error = builder.error;
    }

    public String getModelVersion() { return modelVersion; }
    public Double getPerformanceScore() { return performanceScore; }
    public Double getConsensusOptimization() { return consensusOptimization; }
    public Double getTransactionOrdering() { return transactionOrdering; }
    public Double getBlockProposalEfficiency() { return blockProposalEfficiency; }
    public Double getValidatorSelectionAccuracy() { return validatorSelectionAccuracy; }
    public Integer getPredictionLatency() { return predictionLatency; }
    public Double getInferenceTime() { return inferenceTime; }
    public Integer getThroughputPredictions() { return throughputPredictions; }
    public Double getCpuOptimization() { return cpuOptimization; }
    public Double getMemoryOptimization() { return memoryOptimization; }
    public Double getEnergyEfficiency() { return energyEfficiency; }
    public String getCostSavings() { return costSavings; }
    public Double getCostSavingsPercentage() { return costSavingsPercentage; }
    public String getPerformanceGain() { return performanceGain; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String modelVersion;
        private Double performanceScore;
        private Double consensusOptimization;
        private Double transactionOrdering;
        private Double blockProposalEfficiency;
        private Double validatorSelectionAccuracy;
        private Integer predictionLatency;
        private Double inferenceTime;
        private Integer throughputPredictions;
        private Double cpuOptimization;
        private Double memoryOptimization;
        private Double energyEfficiency;
        private String costSavings;
        private Double costSavingsPercentage;
        private String performanceGain;
        private String error;

        public Builder modelVersion(String modelVersion) { this.modelVersion = modelVersion; return this; }
        public Builder performanceScore(Double performanceScore) { this.performanceScore = performanceScore; return this; }
        public Builder consensusOptimization(Double consensusOptimization) { this.consensusOptimization = consensusOptimization; return this; }
        public Builder transactionOrdering(Double transactionOrdering) { this.transactionOrdering = transactionOrdering; return this; }
        public Builder blockProposalEfficiency(Double blockProposalEfficiency) { this.blockProposalEfficiency = blockProposalEfficiency; return this; }
        public Builder validatorSelectionAccuracy(Double validatorSelectionAccuracy) { this.validatorSelectionAccuracy = validatorSelectionAccuracy; return this; }
        public Builder predictionLatency(Integer predictionLatency) { this.predictionLatency = predictionLatency; return this; }
        public Builder inferenceTime(Double inferenceTime) { this.inferenceTime = inferenceTime; return this; }
        public Builder throughputPredictions(Integer throughputPredictions) { this.throughputPredictions = throughputPredictions; return this; }
        public Builder cpuOptimization(Double cpuOptimization) { this.cpuOptimization = cpuOptimization; return this; }
        public Builder memoryOptimization(Double memoryOptimization) { this.memoryOptimization = memoryOptimization; return this; }
        public Builder energyEfficiency(Double energyEfficiency) { this.energyEfficiency = energyEfficiency; return this; }
        public Builder costSavings(String costSavings) { this.costSavings = costSavings; return this; }
        public Builder costSavingsPercentage(Double costSavingsPercentage) { this.costSavingsPercentage = costSavingsPercentage; return this; }
        public Builder performanceGain(String performanceGain) { this.performanceGain = performanceGain; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public MLPerformanceDTO build() { return new MLPerformanceDTO(this); }
    }
}
