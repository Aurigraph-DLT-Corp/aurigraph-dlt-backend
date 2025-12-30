package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class MLPredictionsDTO {
    @JsonProperty("prediction_period")
    private String predictionPeriod;
    @JsonProperty("predicted_tps")
    private Integer predictedTps;
    @JsonProperty("tps_confidence")
    private Double tpsConfidence;
    @JsonProperty("predicted_avg_block_time")
    private Double predictedAvgBlockTime;
    @JsonProperty("block_time_confidence")
    private Double blockTimeConfidence;
    @JsonProperty("predicted_network_load")
    private Double predictedNetworkLoad;
    @JsonProperty("network_load_confidence")
    private Double networkLoadConfidence;
    @JsonProperty("predicted_transaction_volume")
    private String predictedTransactionVolume;
    @JsonProperty("volume_confidence")
    private Double volumeConfidence;
    @JsonProperty("predicted_user_growth")
    private Double predictedUserGrowth;
    @JsonProperty("user_growth_confidence")
    private Double userGrowthConfidence;
    @JsonProperty("predicted_validator_count")
    private Integer predictedValidatorCount;
    @JsonProperty("predicted_mem_pool_size")
    private Integer predictedMemPoolSize;
    @JsonProperty("predicted_congestion")
    private String predictedCongestion;
    @JsonProperty("congestion_confidence")
    private Double congestionConfidence;
    @JsonProperty("predicted_average_gas_price")
    private String predictedAverageGasPrice;
    @JsonProperty("gas_price_confidence")
    private Double gasPriceConfidence;
    @JsonProperty("predicted_fee_volume")
    private String predictedFeeVolume;
    @JsonProperty("fee_confidence")
    private Double feeConfidence;
    @JsonProperty("recommended_gas_limit")
    private String recommendedGasLimit;
    @JsonProperty("recommended_block_time")
    private String recommendedBlockTime;
    @JsonProperty("error")
    private String error;

    public MLPredictionsDTO() {}

    private MLPredictionsDTO(Builder builder) {
        this.predictionPeriod = builder.predictionPeriod;
        this.predictedTps = builder.predictedTps;
        this.tpsConfidence = builder.tpsConfidence;
        this.predictedAvgBlockTime = builder.predictedAvgBlockTime;
        this.blockTimeConfidence = builder.blockTimeConfidence;
        this.predictedNetworkLoad = builder.predictedNetworkLoad;
        this.networkLoadConfidence = builder.networkLoadConfidence;
        this.predictedTransactionVolume = builder.predictedTransactionVolume;
        this.volumeConfidence = builder.volumeConfidence;
        this.predictedUserGrowth = builder.predictedUserGrowth;
        this.userGrowthConfidence = builder.userGrowthConfidence;
        this.predictedValidatorCount = builder.predictedValidatorCount;
        this.predictedMemPoolSize = builder.predictedMemPoolSize;
        this.predictedCongestion = builder.predictedCongestion;
        this.congestionConfidence = builder.congestionConfidence;
        this.predictedAverageGasPrice = builder.predictedAverageGasPrice;
        this.gasPriceConfidence = builder.gasPriceConfidence;
        this.predictedFeeVolume = builder.predictedFeeVolume;
        this.feeConfidence = builder.feeConfidence;
        this.recommendedGasLimit = builder.recommendedGasLimit;
        this.recommendedBlockTime = builder.recommendedBlockTime;
        this.error = builder.error;
    }

    public String getPredictionPeriod() { return predictionPeriod; }
    public Integer getPredictedTps() { return predictedTps; }
    public Double getTpsConfidence() { return tpsConfidence; }
    public Double getPredictedAvgBlockTime() { return predictedAvgBlockTime; }
    public Double getBlockTimeConfidence() { return blockTimeConfidence; }
    public Double getPredictedNetworkLoad() { return predictedNetworkLoad; }
    public Double getNetworkLoadConfidence() { return networkLoadConfidence; }
    public String getPredictedTransactionVolume() { return predictedTransactionVolume; }
    public Double getVolumeConfidence() { return volumeConfidence; }
    public Double getPredictedUserGrowth() { return predictedUserGrowth; }
    public Double getUserGrowthConfidence() { return userGrowthConfidence; }
    public Integer getPredictedValidatorCount() { return predictedValidatorCount; }
    public Integer getPredictedMemPoolSize() { return predictedMemPoolSize; }
    public String getPredictedCongestion() { return predictedCongestion; }
    public Double getCongestionConfidence() { return congestionConfidence; }
    public String getPredictedAverageGasPrice() { return predictedAverageGasPrice; }
    public Double getGasPriceConfidence() { return gasPriceConfidence; }
    public String getPredictedFeeVolume() { return predictedFeeVolume; }
    public Double getFeeConfidence() { return feeConfidence; }
    public String getRecommendedGasLimit() { return recommendedGasLimit; }
    public String getRecommendedBlockTime() { return recommendedBlockTime; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String predictionPeriod;
        private Integer predictedTps;
        private Double tpsConfidence;
        private Double predictedAvgBlockTime;
        private Double blockTimeConfidence;
        private Double predictedNetworkLoad;
        private Double networkLoadConfidence;
        private String predictedTransactionVolume;
        private Double volumeConfidence;
        private Double predictedUserGrowth;
        private Double userGrowthConfidence;
        private Integer predictedValidatorCount;
        private Integer predictedMemPoolSize;
        private String predictedCongestion;
        private Double congestionConfidence;
        private String predictedAverageGasPrice;
        private Double gasPriceConfidence;
        private String predictedFeeVolume;
        private Double feeConfidence;
        private String recommendedGasLimit;
        private String recommendedBlockTime;
        private String error;

        public Builder predictionPeriod(String predictionPeriod) { this.predictionPeriod = predictionPeriod; return this; }
        public Builder predictedTps(Integer predictedTps) { this.predictedTps = predictedTps; return this; }
        public Builder tpsConfidence(Double tpsConfidence) { this.tpsConfidence = tpsConfidence; return this; }
        public Builder predictedAvgBlockTime(Double predictedAvgBlockTime) { this.predictedAvgBlockTime = predictedAvgBlockTime; return this; }
        public Builder blockTimeConfidence(Double blockTimeConfidence) { this.blockTimeConfidence = blockTimeConfidence; return this; }
        public Builder predictedNetworkLoad(Double predictedNetworkLoad) { this.predictedNetworkLoad = predictedNetworkLoad; return this; }
        public Builder networkLoadConfidence(Double networkLoadConfidence) { this.networkLoadConfidence = networkLoadConfidence; return this; }
        public Builder predictedTransactionVolume(String predictedTransactionVolume) { this.predictedTransactionVolume = predictedTransactionVolume; return this; }
        public Builder volumeConfidence(Double volumeConfidence) { this.volumeConfidence = volumeConfidence; return this; }
        public Builder predictedUserGrowth(Double predictedUserGrowth) { this.predictedUserGrowth = predictedUserGrowth; return this; }
        public Builder userGrowthConfidence(Double userGrowthConfidence) { this.userGrowthConfidence = userGrowthConfidence; return this; }
        public Builder predictedValidatorCount(Integer predictedValidatorCount) { this.predictedValidatorCount = predictedValidatorCount; return this; }
        public Builder predictedMemPoolSize(Integer predictedMemPoolSize) { this.predictedMemPoolSize = predictedMemPoolSize; return this; }
        public Builder predictedCongestion(String predictedCongestion) { this.predictedCongestion = predictedCongestion; return this; }
        public Builder congestionConfidence(Double congestionConfidence) { this.congestionConfidence = congestionConfidence; return this; }
        public Builder predictedAverageGasPrice(String predictedAverageGasPrice) { this.predictedAverageGasPrice = predictedAverageGasPrice; return this; }
        public Builder gasPriceConfidence(Double gasPriceConfidence) { this.gasPriceConfidence = gasPriceConfidence; return this; }
        public Builder predictedFeeVolume(String predictedFeeVolume) { this.predictedFeeVolume = predictedFeeVolume; return this; }
        public Builder feeConfidence(Double feeConfidence) { this.feeConfidence = feeConfidence; return this; }
        public Builder recommendedGasLimit(String recommendedGasLimit) { this.recommendedGasLimit = recommendedGasLimit; return this; }
        public Builder recommendedBlockTime(String recommendedBlockTime) { this.recommendedBlockTime = recommendedBlockTime; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public MLPredictionsDTO build() { return new MLPredictionsDTO(this); }
    }
}
