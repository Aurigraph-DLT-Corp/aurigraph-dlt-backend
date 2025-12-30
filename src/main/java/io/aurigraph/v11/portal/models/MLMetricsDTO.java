package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class MLMetricsDTO {
    @JsonProperty("model_version")
    private String modelVersion;
    @JsonProperty("training_data_points")
    private Long trainingDataPoints;
    @JsonProperty("model_accuracy")
    private Double modelAccuracy;
    @JsonProperty("model_precision")
    private Double modelPrecision;
    @JsonProperty("model_recall")
    private Double modelRecall;
    @JsonProperty("f1_score")
    private Double f1Score;
    @JsonProperty("rmse")
    private Double rmse;
    @JsonProperty("mape")
    private Double mape;
    @JsonProperty("roc_auc_score")
    private Double rocAucScore;
    @JsonProperty("overall_model_health")
    private String overallModelHealth;
    @JsonProperty("last_training_time")
    private Instant lastTrainingTime;
    @JsonProperty("next_training_schedule")
    private Instant nextTrainingSchedule;
    @JsonProperty("training_duration")
    private Long trainingDuration;
    @JsonProperty("dataset_version")
    private String datasetVersion;
    @JsonProperty("feature_count")
    private Integer featureCount;
    @JsonProperty("anomalies_detected")
    private Integer anomaliesDetected;
    @JsonProperty("anomaly_detection_rate")
    private Double anomalyDetectionRate;
    @JsonProperty("predictions_generated")
    private Integer predictionsGenerated;
    @JsonProperty("prediction_accuracy")
    private Double predictionAccuracy;
    @JsonProperty("error")
    private String error;

    public MLMetricsDTO() {}

    private MLMetricsDTO(Builder builder) {
        this.modelVersion = builder.modelVersion;
        this.trainingDataPoints = builder.trainingDataPoints;
        this.modelAccuracy = builder.modelAccuracy;
        this.modelPrecision = builder.modelPrecision;
        this.modelRecall = builder.modelRecall;
        this.f1Score = builder.f1Score;
        this.rmse = builder.rmse;
        this.mape = builder.mape;
        this.rocAucScore = builder.rocAucScore;
        this.overallModelHealth = builder.overallModelHealth;
        this.lastTrainingTime = builder.lastTrainingTime;
        this.nextTrainingSchedule = builder.nextTrainingSchedule;
        this.trainingDuration = builder.trainingDuration;
        this.datasetVersion = builder.datasetVersion;
        this.featureCount = builder.featureCount;
        this.anomaliesDetected = builder.anomaliesDetected;
        this.anomalyDetectionRate = builder.anomalyDetectionRate;
        this.predictionsGenerated = builder.predictionsGenerated;
        this.predictionAccuracy = builder.predictionAccuracy;
        this.error = builder.error;
    }

    public String getModelVersion() { return modelVersion; }
    public Long getTrainingDataPoints() { return trainingDataPoints; }
    public Double getModelAccuracy() { return modelAccuracy; }
    public Double getModelPrecision() { return modelPrecision; }
    public Double getModelRecall() { return modelRecall; }
    public Double getF1Score() { return f1Score; }
    public Double getRmse() { return rmse; }
    public Double getMape() { return mape; }
    public Double getRocAucScore() { return rocAucScore; }
    public String getOverallModelHealth() { return overallModelHealth; }
    public Instant getLastTrainingTime() { return lastTrainingTime; }
    public Instant getNextTrainingSchedule() { return nextTrainingSchedule; }
    public Long getTrainingDuration() { return trainingDuration; }
    public String getDatasetVersion() { return datasetVersion; }
    public Integer getFeatureCount() { return featureCount; }
    public Integer getAnomaliesDetected() { return anomaliesDetected; }
    public Double getAnomalyDetectionRate() { return anomalyDetectionRate; }
    public Integer getPredictionsGenerated() { return predictionsGenerated; }
    public Double getPredictionAccuracy() { return predictionAccuracy; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String modelVersion;
        private Long trainingDataPoints;
        private Double modelAccuracy;
        private Double modelPrecision;
        private Double modelRecall;
        private Double f1Score;
        private Double rmse;
        private Double mape;
        private Double rocAucScore;
        private String overallModelHealth;
        private Instant lastTrainingTime;
        private Instant nextTrainingSchedule;
        private Long trainingDuration;
        private String datasetVersion;
        private Integer featureCount;
        private Integer anomaliesDetected;
        private Double anomalyDetectionRate;
        private Integer predictionsGenerated;
        private Double predictionAccuracy;
        private String error;

        public Builder modelVersion(String modelVersion) { this.modelVersion = modelVersion; return this; }
        public Builder trainingDataPoints(Long trainingDataPoints) { this.trainingDataPoints = trainingDataPoints; return this; }
        public Builder modelAccuracy(Double modelAccuracy) { this.modelAccuracy = modelAccuracy; return this; }
        public Builder modelPrecision(Double modelPrecision) { this.modelPrecision = modelPrecision; return this; }
        public Builder modelRecall(Double modelRecall) { this.modelRecall = modelRecall; return this; }
        public Builder f1Score(Double f1Score) { this.f1Score = f1Score; return this; }
        public Builder rmse(Double rmse) { this.rmse = rmse; return this; }
        public Builder mape(Double mape) { this.mape = mape; return this; }
        public Builder rocAucScore(Double rocAucScore) { this.rocAucScore = rocAucScore; return this; }
        public Builder overallModelHealth(String overallModelHealth) { this.overallModelHealth = overallModelHealth; return this; }
        public Builder lastTrainingTime(Instant lastTrainingTime) { this.lastTrainingTime = lastTrainingTime; return this; }
        public Builder nextTrainingSchedule(Instant nextTrainingSchedule) { this.nextTrainingSchedule = nextTrainingSchedule; return this; }
        public Builder trainingDuration(Long trainingDuration) { this.trainingDuration = trainingDuration; return this; }
        public Builder datasetVersion(String datasetVersion) { this.datasetVersion = datasetVersion; return this; }
        public Builder featureCount(Integer featureCount) { this.featureCount = featureCount; return this; }
        public Builder anomaliesDetected(Integer anomaliesDetected) { this.anomaliesDetected = anomaliesDetected; return this; }
        public Builder anomalyDetectionRate(Double anomalyDetectionRate) { this.anomalyDetectionRate = anomalyDetectionRate; return this; }
        public Builder predictionsGenerated(Integer predictionsGenerated) { this.predictionsGenerated = predictionsGenerated; return this; }
        public Builder predictionAccuracy(Double predictionAccuracy) { this.predictionAccuracy = predictionAccuracy; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public MLMetricsDTO build() { return new MLMetricsDTO(this); }
    }
}
