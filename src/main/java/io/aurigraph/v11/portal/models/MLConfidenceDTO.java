package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class MLConfidenceDTO {
    @JsonProperty("overall_confidence")
    private Double overallConfidence;
    @JsonProperty("tps_confidence")
    private Double tpsConfidence;
    @JsonProperty("block_time_confidence")
    private Double blockTimeConfidence;
    @JsonProperty("network_load_confidence")
    private Double networkLoadConfidence;
    @JsonProperty("transaction_volume_confidence")
    private Double transactionVolumeConfidence;
    @JsonProperty("user_activity_confidence")
    private Double userActivityConfidence;
    @JsonProperty("validator_count_confidence")
    private Double validatorCountConfidence;
    @JsonProperty("contract_deployment_confidence")
    private Double contractDeploymentConfidence;
    @JsonProperty("token_transfer_confidence")
    private Double tokenTransferConfidence;
    @JsonProperty("gas_usage_confidence")
    private Double gasUsageConfidence;
    @JsonProperty("fee_estimate_confidence")
    private Double feeEstimateConfidence;
    @JsonProperty("consensus_health_confidence")
    private Double consensusHealthConfidence;
    @JsonProperty("anomaly_detection_confidence")
    private Double anomalyDetectionConfidence;
    @JsonProperty("prediction_accuracy")
    private Double predictionAccuracy;
    @JsonProperty("backtest_score")
    private Double backtestScore;
    @JsonProperty("validation_score")
    private Double validationScore;
    @JsonProperty("last_confidence_update")
    private Instant lastConfidenceUpdate;
    @JsonProperty("confidence_trend")
    private String confidenceTrend;
    @JsonProperty("confidence_change")
    private Double confidenceChange;
    @JsonProperty("error")
    private String error;

    public MLConfidenceDTO() {}

    private MLConfidenceDTO(Builder builder) {
        this.overallConfidence = builder.overallConfidence;
        this.tpsConfidence = builder.tpsConfidence;
        this.blockTimeConfidence = builder.blockTimeConfidence;
        this.networkLoadConfidence = builder.networkLoadConfidence;
        this.transactionVolumeConfidence = builder.transactionVolumeConfidence;
        this.userActivityConfidence = builder.userActivityConfidence;
        this.validatorCountConfidence = builder.validatorCountConfidence;
        this.contractDeploymentConfidence = builder.contractDeploymentConfidence;
        this.tokenTransferConfidence = builder.tokenTransferConfidence;
        this.gasUsageConfidence = builder.gasUsageConfidence;
        this.feeEstimateConfidence = builder.feeEstimateConfidence;
        this.consensusHealthConfidence = builder.consensusHealthConfidence;
        this.anomalyDetectionConfidence = builder.anomalyDetectionConfidence;
        this.predictionAccuracy = builder.predictionAccuracy;
        this.backtestScore = builder.backtestScore;
        this.validationScore = builder.validationScore;
        this.lastConfidenceUpdate = builder.lastConfidenceUpdate;
        this.confidenceTrend = builder.confidenceTrend;
        this.confidenceChange = builder.confidenceChange;
        this.error = builder.error;
    }

    public Double getOverallConfidence() { return overallConfidence; }
    public Double getTpsConfidence() { return tpsConfidence; }
    public Double getBlockTimeConfidence() { return blockTimeConfidence; }
    public Double getNetworkLoadConfidence() { return networkLoadConfidence; }
    public Double getTransactionVolumeConfidence() { return transactionVolumeConfidence; }
    public Double getUserActivityConfidence() { return userActivityConfidence; }
    public Double getValidatorCountConfidence() { return validatorCountConfidence; }
    public Double getContractDeploymentConfidence() { return contractDeploymentConfidence; }
    public Double getTokenTransferConfidence() { return tokenTransferConfidence; }
    public Double getGasUsageConfidence() { return gasUsageConfidence; }
    public Double getFeeEstimateConfidence() { return feeEstimateConfidence; }
    public Double getConsensusHealthConfidence() { return consensusHealthConfidence; }
    public Double getAnomalyDetectionConfidence() { return anomalyDetectionConfidence; }
    public Double getPredictionAccuracy() { return predictionAccuracy; }
    public Double getBacktestScore() { return backtestScore; }
    public Double getValidationScore() { return validationScore; }
    public Instant getLastConfidenceUpdate() { return lastConfidenceUpdate; }
    public String getConfidenceTrend() { return confidenceTrend; }
    public Double getConfidenceChange() { return confidenceChange; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Double overallConfidence;
        private Double tpsConfidence;
        private Double blockTimeConfidence;
        private Double networkLoadConfidence;
        private Double transactionVolumeConfidence;
        private Double userActivityConfidence;
        private Double validatorCountConfidence;
        private Double contractDeploymentConfidence;
        private Double tokenTransferConfidence;
        private Double gasUsageConfidence;
        private Double feeEstimateConfidence;
        private Double consensusHealthConfidence;
        private Double anomalyDetectionConfidence;
        private Double predictionAccuracy;
        private Double backtestScore;
        private Double validationScore;
        private Instant lastConfidenceUpdate;
        private String confidenceTrend;
        private Double confidenceChange;
        private String error;

        public Builder overallConfidence(Double overallConfidence) { this.overallConfidence = overallConfidence; return this; }
        public Builder tpsConfidence(Double tpsConfidence) { this.tpsConfidence = tpsConfidence; return this; }
        public Builder blockTimeConfidence(Double blockTimeConfidence) { this.blockTimeConfidence = blockTimeConfidence; return this; }
        public Builder networkLoadConfidence(Double networkLoadConfidence) { this.networkLoadConfidence = networkLoadConfidence; return this; }
        public Builder transactionVolumeConfidence(Double transactionVolumeConfidence) { this.transactionVolumeConfidence = transactionVolumeConfidence; return this; }
        public Builder userActivityConfidence(Double userActivityConfidence) { this.userActivityConfidence = userActivityConfidence; return this; }
        public Builder validatorCountConfidence(Double validatorCountConfidence) { this.validatorCountConfidence = validatorCountConfidence; return this; }
        public Builder contractDeploymentConfidence(Double contractDeploymentConfidence) { this.contractDeploymentConfidence = contractDeploymentConfidence; return this; }
        public Builder tokenTransferConfidence(Double tokenTransferConfidence) { this.tokenTransferConfidence = tokenTransferConfidence; return this; }
        public Builder gasUsageConfidence(Double gasUsageConfidence) { this.gasUsageConfidence = gasUsageConfidence; return this; }
        public Builder feeEstimateConfidence(Double feeEstimateConfidence) { this.feeEstimateConfidence = feeEstimateConfidence; return this; }
        public Builder consensusHealthConfidence(Double consensusHealthConfidence) { this.consensusHealthConfidence = consensusHealthConfidence; return this; }
        public Builder anomalyDetectionConfidence(Double anomalyDetectionConfidence) { this.anomalyDetectionConfidence = anomalyDetectionConfidence; return this; }
        public Builder predictionAccuracy(Double predictionAccuracy) { this.predictionAccuracy = predictionAccuracy; return this; }
        public Builder backtestScore(Double backtestScore) { this.backtestScore = backtestScore; return this; }
        public Builder validationScore(Double validationScore) { this.validationScore = validationScore; return this; }
        public Builder lastConfidenceUpdate(Instant lastConfidenceUpdate) { this.lastConfidenceUpdate = lastConfidenceUpdate; return this; }
        public Builder confidenceTrend(String confidenceTrend) { this.confidenceTrend = confidenceTrend; return this; }
        public Builder confidenceChange(Double confidenceChange) { this.confidenceChange = confidenceChange; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public MLConfidenceDTO build() { return new MLConfidenceDTO(this); }
    }
}
