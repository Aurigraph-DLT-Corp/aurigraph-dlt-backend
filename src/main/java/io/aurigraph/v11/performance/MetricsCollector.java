package io.aurigraph.v11.performance;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Interface for comprehensive metrics collection and monitoring in Aurigraph V11.
 * 
 * This service provides advanced metrics collection capabilities to monitor
 * all aspects of the platform's performance, from transaction processing to
 * resource utilization, supporting the platform's 2M+ TPS operational requirements.
 * 
 * Key Features:
 * - Real-time metrics collection across all system components
 * - Custom metric definitions and aggregations
 * - Performance baseline establishment and anomaly detection
 * - Historical metrics storage and analysis
 * - Multi-dimensional metric tagging and filtering
 * - Prometheus and OpenTelemetry integration
 * 
 * Performance Requirements:
 * - Collect 100K+ metrics per second with minimal overhead
 * - Sub-millisecond metric recording latency
 * - Support for 1M+ unique metric series
 * - Real-time aggregation and alerting capabilities
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface MetricsCollector {

    /**
     * Records a counter metric (monotonically increasing value).
     * 
     * @param metricName the name of the metric
     * @param value the value to add to the counter
     * @param tags optional tags for metric categorization
     * @return Uni indicating success or failure of the operation
     */
    Uni<Boolean> recordCounter(String metricName, double value, Map<String, String> tags);

    /**
     * Records a gauge metric (current value that can increase or decrease).
     * 
     * @param metricName the name of the metric
     * @param value the current value
     * @param tags optional tags for metric categorization
     * @return Uni indicating success or failure of the operation
     */
    Uni<Boolean> recordGauge(String metricName, double value, Map<String, String> tags);

    /**
     * Records a histogram metric for timing and size distributions.
     * 
     * @param metricName the name of the metric
     * @param value the value to record
     * @param tags optional tags for metric categorization
     * @return Uni indicating success or failure of the operation
     */
    Uni<Boolean> recordHistogram(String metricName, double value, Map<String, String> tags);

    /**
     * Records a timer metric with automatic duration calculation.
     * 
     * @param metricName the name of the metric
     * @param duration the duration to record
     * @param tags optional tags for metric categorization
     * @return Uni indicating success or failure of the operation
     */
    Uni<Boolean> recordTimer(String metricName, Duration duration, Map<String, String> tags);

    /**
     * Records a custom metric with specified type and metadata.
     * 
     * @param metricDefinition the definition of the custom metric
     * @param value the value to record
     * @param timestamp the timestamp for the metric (null for current time)
     * @return Uni indicating success or failure of the operation
     */
    Uni<Boolean> recordCustomMetric(CustomMetricDefinition metricDefinition, double value, Long timestamp);

    /**
     * Records multiple metrics in a single batch operation.
     * 
     * @param metricBatch the batch of metrics to record
     * @return Uni containing the batch recording result
     */
    Uni<MetricBatchResult> recordBatch(List<MetricRecord> metricBatch);

    /**
     * Gets current metric values for specified metrics.
     * 
     * @param metricNames the list of metric names to retrieve
     * @param tags optional tags for filtering
     * @return Uni containing the current metric values
     */
    Uni<Map<String, MetricValue>> getCurrentMetrics(List<String> metricNames, Map<String, String> tags);

    /**
     * Gets aggregated metric values over a time range.
     * 
     * @param metricName the name of the metric
     * @param aggregationType the type of aggregation to perform
     * @param fromTimestamp start timestamp for aggregation
     * @param toTimestamp end timestamp for aggregation
     * @param tags optional tags for filtering
     * @return Uni containing the aggregated metric value
     */
    Uni<AggregatedMetric> getAggregatedMetric(
        String metricName,
        AggregationType aggregationType,
        long fromTimestamp,
        long toTimestamp,
        Map<String, String> tags
    );

    /**
     * Gets time series data for a metric over a specified period.
     * 
     * @param metricName the name of the metric
     * @param fromTimestamp start timestamp for the time series
     * @param toTimestamp end timestamp for the time series
     * @param interval the interval for data points
     * @param tags optional tags for filtering
     * @return Multi streaming time series data points
     */
    Multi<TimeSeriesDataPoint> getTimeSeries(
        String metricName,
        long fromTimestamp,
        long toTimestamp,
        Duration interval,
        Map<String, String> tags
    );

    /**
     * Creates a custom dashboard for monitoring specific metrics.
     * 
     * @param dashboardConfig configuration for the dashboard
     * @return Uni containing the dashboard creation result
     */
    Uni<DashboardCreationResult> createDashboard(DashboardConfig dashboardConfig);

    /**
     * Sets up alerting rules based on metric thresholds.
     * 
     * @param alertRule the alerting rule configuration
     * @return Uni containing the alert setup result
     */
    Uni<AlertSetupResult> setupAlert(AlertRule alertRule);

    /**
     * Gets all active alerts based on current metric values.
     * 
     * @return Multi streaming active alerts
     */
    Multi<MetricAlert> getActiveAlerts();

    /**
     * Establishes performance baselines for anomaly detection.
     * 
     * @param baselineConfig configuration for baseline establishment
     * @return Uni containing the baseline establishment result
     */
    Uni<BaselineResult> establishBaseline(BaselineConfig baselineConfig);

    /**
     * Detects anomalies in metric values compared to established baselines.
     * 
     * @param metricName the name of the metric to analyze
     * @param detectionConfig configuration for anomaly detection
     * @return Multi streaming anomaly detection results
     */
    Multi<AnomalyDetectionResult> detectAnomalies(String metricName, AnomalyDetectionConfig detectionConfig);

    /**
     * Gets comprehensive system health metrics.
     * 
     * @return Uni containing system health status based on metrics
     */
    Uni<SystemHealthMetrics> getSystemHealth();

    /**
     * Monitors specific metrics in real-time with streaming updates.
     * 
     * @param metricNames the list of metrics to monitor
     * @param updateInterval the interval between updates
     * @param tags optional tags for filtering
     * @return Multi streaming real-time metric updates
     */
    Multi<MetricUpdate> monitorMetrics(
        List<String> metricNames,
        Duration updateInterval,
        Map<String, String> tags
    );

    /**
     * Exports metrics in various formats for external systems.
     * 
     * @param exportConfig configuration for metric export
     * @return Uni containing the export result
     */
    Uni<MetricExportResult> exportMetrics(MetricExportConfig exportConfig);

    /**
     * Gets metric collection statistics and performance information.
     * 
     * @return Uni containing collection statistics
     */
    Uni<CollectionStatistics> getCollectionStatistics();

    /**
     * Configures metric retention policies for storage optimization.
     * 
     * @param retentionPolicy the retention policy to apply
     * @return Uni indicating success or failure of the configuration
     */
    Uni<Boolean> configureRetention(MetricRetentionPolicy retentionPolicy);

    /**
     * Creates custom metric aggregations for complex calculations.
     * 
     * @param aggregationDefinition the definition of the custom aggregation
     * @return Uni containing the aggregation creation result
     */
    Uni<AggregationCreationResult> createCustomAggregation(CustomAggregationDefinition aggregationDefinition);

    /**
     * Gets performance insights and optimization recommendations.
     * 
     * @param analysisConfig configuration for performance analysis
     * @return Uni containing performance insights
     */
    Uni<PerformanceInsights> analyzePerformance(PerformanceAnalysisConfig analysisConfig);

    // Inner classes and enums for data transfer objects

    /**
     * Definition for custom metrics.
     */
    public static class CustomMetricDefinition {
        public String metricName;
        public MetricType metricType;
        public String description;
        public String unit;
        public Map<String, String> defaultTags;
        public Map<String, Object> metadata;
    }

    /**
     * Types of metrics supported.
     */
    public enum MetricType {
        COUNTER,        // Monotonically increasing
        GAUGE,          // Current value
        HISTOGRAM,      // Distribution of values
        TIMER,          // Duration measurements
        RATE,           // Rate of change
        RATIO,          // Ratio between values
        CUSTOM          // Custom metric type
    }

    /**
     * Individual metric record for batch operations.
     */
    public static class MetricRecord {
        public String metricName;
        public MetricType metricType;
        public double value;
        public Map<String, String> tags;
        public long timestamp;
        
        public MetricRecord(String metricName, MetricType metricType, double value) {
            this.metricName = metricName;
            this.metricType = metricType;
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Result of batch metric recording.
     */
    public static class MetricBatchResult {
        public int totalMetrics;
        public int successfulRecords;
        public int failedRecords;
        public long processingTimeMs;
        public List<String> errors;
        public boolean success;
    }

    /**
     * Current value of a metric with metadata.
     */
    public static class MetricValue {
        public String metricName;
        public double value;
        public Map<String, String> tags;
        public long timestamp;
        public MetricType metricType;
        public Map<String, Object> metadata;
    }

    /**
     * Types of aggregation operations.
     */
    public enum AggregationType {
        SUM,
        AVERAGE,
        MIN,
        MAX,
        COUNT,
        PERCENTILE_50,
        PERCENTILE_95,
        PERCENTILE_99,
        STANDARD_DEVIATION,
        CUSTOM
    }

    /**
     * Aggregated metric result.
     */
    public static class AggregatedMetric {
        public String metricName;
        public AggregationType aggregationType;
        public double value;
        public long dataPoints;
        public long fromTimestamp;
        public long toTimestamp;
        public Map<String, String> tags;
        public Map<String, Object> additionalStats;
    }

    /**
     * Time series data point.
     */
    public static class TimeSeriesDataPoint {
        public long timestamp;
        public double value;
        public Map<String, String> tags;
        public Map<String, Object> metadata;
    }

    /**
     * Configuration for creating dashboards.
     */
    public static class DashboardConfig {
        public String dashboardName;
        public String description;
        public List<DashboardPanel> panels;
        public Duration refreshInterval;
        public Map<String, String> globalTags;
        public boolean isPublic;
    }

    /**
     * Panel configuration for dashboards.
     */
    public static class DashboardPanel {
        public String panelTitle;
        public PanelType panelType;
        public List<String> metricNames;
        public Map<String, String> tags;
        public Duration timeRange;
        public Map<String, Object> visualizationOptions;
    }

    /**
     * Types of dashboard panels.
     */
    public enum PanelType {
        LINE_CHART,
        BAR_CHART,
        PIE_CHART,
        GAUGE,
        TABLE,
        HEATMAP,
        STAT,
        TEXT
    }

    /**
     * Result of dashboard creation.
     */
    public static class DashboardCreationResult {
        public String dashboardId;
        public String dashboardUrl;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Alert rule configuration.
     */
    public static class AlertRule {
        public String ruleName;
        public String metricName;
        public AlertCondition condition;
        public double threshold;
        public Duration evaluationInterval;
        public Duration forDuration; // Alert only after condition is true for this duration
        public Map<String, String> tags;
        public List<AlertAction> actions;
        public AlertSeverity severity;
    }

    /**
     * Alert conditions.
     */
    public enum AlertCondition {
        GREATER_THAN,
        LESS_THAN,
        EQUALS,
        NOT_EQUALS,
        INCREASE_RATE,
        DECREASE_RATE,
        ANOMALY_DETECTED
    }

    /**
     * Actions to take when alerts trigger.
     */
    public static class AlertAction {
        public ActionType actionType;
        public Map<String, String> parameters;
    }

    /**
     * Types of alert actions.
     */
    public enum ActionType {
        EMAIL,
        SLACK,
        WEBHOOK,
        SMS,
        PAGERDUTY,
        CUSTOM
    }

    /**
     * Alert severity levels.
     */
    public enum AlertSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /**
     * Result of alert setup.
     */
    public static class AlertSetupResult {
        public String alertRuleId;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Active metric alert.
     */
    public static class MetricAlert {
        public String alertId;
        public String ruleName;
        public String metricName;
        public AlertSeverity severity;
        public double currentValue;
        public double threshold;
        public long triggeredTimestamp;
        public Duration activeDuration;
        public Map<String, String> tags;
        public String description;
    }

    /**
     * Configuration for baseline establishment.
     */
    public static class BaselineConfig {
        public List<String> metricNames;
        public long fromTimestamp;
        public long toTimestamp;
        public Map<String, String> tags;
        public StatisticalMethod method;
        public double confidenceLevel;
    }

    /**
     * Statistical methods for baseline calculation.
     */
    public enum StatisticalMethod {
        MEAN,
        MEDIAN,
        PERCENTILE_BASED,
        SEASONAL_DECOMPOSITION,
        MACHINE_LEARNING
    }

    /**
     * Result of baseline establishment.
     */
    public static class BaselineResult {
        public Map<String, MetricBaseline> baselines;
        public long calculationTimestamp;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Baseline information for a metric.
     */
    public static class MetricBaseline {
        public String metricName;
        public double baselineValue;
        public double upperBound;
        public double lowerBound;
        public double standardDeviation;
        public long dataPoints;
        public Map<String, Object> additionalStatistics;
    }

    /**
     * Configuration for anomaly detection.
     */
    public static class AnomalyDetectionConfig {
        public AnomalyDetectionMethod method;
        public double sensitivityLevel;
        public Duration detectionWindow;
        public boolean enableSeasonalityDetection;
        public Map<String, Object> methodParameters;
    }

    /**
     * Methods for anomaly detection.
     */
    public enum AnomalyDetectionMethod {
        STATISTICAL_OUTLIER,
        Z_SCORE,
        ISOLATION_FOREST,
        LSTM_AUTOENCODER,
        SEASONAL_HYBRID,
        ENSEMBLE
    }

    /**
     * Result of anomaly detection.
     */
    public static class AnomalyDetectionResult {
        public String metricName;
        public long timestamp;
        public double actualValue;
        public double expectedValue;
        public double anomalyScore; // 0.0 to 1.0
        public AnomalySeverity severity;
        public String description;
        public Map<String, Object> detectionDetails;
    }

    /**
     * Severity levels for anomalies.
     */
    public enum AnomalySeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * System health metrics overview.
     */
    public static class SystemHealthMetrics {
        public HealthStatus overallHealth;
        public Map<String, ComponentHealth> componentHealth;
        public List<HealthIssue> activeIssues;
        public SystemPerformanceMetrics performanceMetrics;
        public long lastUpdated;
    }

    /**
     * Overall health status.
     */
    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        CRITICAL,
        UNKNOWN
    }

    /**
     * Health status for individual components.
     */
    public static class ComponentHealth {
        public String componentName;
        public HealthStatus status;
        public double healthScore; // 0.0 to 1.0
        public List<String> issues;
        public Map<String, Double> keyMetrics;
    }

    /**
     * Health issues affecting the system.
     */
    public static class HealthIssue {
        public String issueId;
        public String component;
        public String description;
        public HealthSeverity severity;
        public long detectedTimestamp;
        public Map<String, Object> context;
    }

    /**
     * Severity levels for health issues.
     */
    public enum HealthSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /**
     * System performance metrics summary.
     */
    public static class SystemPerformanceMetrics {
        public double throughputTPS;
        public double averageLatencyMs;
        public double cpuUtilization;
        public double memoryUtilization;
        public double diskUtilization;
        public double networkUtilization;
        public int activeConnections;
        public double errorRate;
    }

    /**
     * Real-time metric update.
     */
    public static class MetricUpdate {
        public String metricName;
        public double currentValue;
        public double previousValue;
        public double changeRate;
        public long timestamp;
        public Map<String, String> tags;
        public TrendDirection trend;
    }

    /**
     * Trend directions for metrics.
     */
    public enum TrendDirection {
        INCREASING,
        DECREASING,
        STABLE,
        VOLATILE
    }

    /**
     * Configuration for metric export.
     */
    public static class MetricExportConfig {
        public ExportFormat format;
        public List<String> metricNames;
        public long fromTimestamp;
        public long toTimestamp;
        public Map<String, String> tags;
        public String destination;
        public Map<String, Object> exportOptions;
    }

    /**
     * Supported export formats.
     */
    public enum ExportFormat {
        JSON,
        CSV,
        PROMETHEUS,
        INFLUX_LINE_PROTOCOL,
        OPENTELEMETRY,
        CUSTOM
    }

    /**
     * Result of metric export operation.
     */
    public static class MetricExportResult {
        public String exportId;
        public ExportFormat format;
        public long recordsExported;
        public long fileSizeBytes;
        public String downloadUrl;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Statistics about metric collection performance.
     */
    public static class CollectionStatistics {
        public long totalMetricsCollected;
        public double collectionRatePerSecond;
        public double averageCollectionLatencyMs;
        public long storageUsageBytes;
        public Map<MetricType, Long> metricsByType;
        public List<String> topMetricsByVolume;
        public double systemOverhead; // 0.0 to 1.0
        public long lastResetTimestamp;
    }

    /**
     * Metric retention policy configuration.
     */
    public static class MetricRetentionPolicy {
        public String policyName;
        public List<RetentionRule> rules;
        public boolean enableCompression;
        public boolean enableDownsampling;
    }

    /**
     * Individual retention rule.
     */
    public static class RetentionRule {
        public String metricPattern;
        public Duration retentionPeriod;
        public Duration downsampleInterval;
        public AggregationType downsampleMethod;
        public Map<String, String> tagFilters;
    }

    /**
     * Definition for custom aggregations.
     */
    public static class CustomAggregationDefinition {
        public String aggregationName;
        public List<String> inputMetrics;
        public String formula; // Mathematical formula for aggregation
        public Duration calculationInterval;
        public Map<String, Object> parameters;
        public boolean enableAlerting;
    }

    /**
     * Result of custom aggregation creation.
     */
    public static class AggregationCreationResult {
        public String aggregationId;
        public String aggregationName;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Configuration for performance analysis.
     */
    public static class PerformanceAnalysisConfig {
        public Duration analysisWindow;
        public List<String> focusMetrics;
        public AnalysisDepth depth;
        public boolean includeRecommendations;
        public Map<String, Object> analysisParameters;
    }

    /**
     * Depth levels for performance analysis.
     */
    public enum AnalysisDepth {
        BASIC,
        DETAILED,
        COMPREHENSIVE,
        EXPERT
    }

    /**
     * Performance insights and recommendations.
     */
    public static class PerformanceInsights {
        public PerformanceSummary summary;
        public List<PerformanceBottleneck> bottlenecks;
        public List<OptimizationRecommendation> recommendations;
        public Map<String, TrendAnalysis> trendAnalyses;
        public double overallPerformanceScore; // 0.0 to 1.0
        public long analysisTimestamp;
    }

    /**
     * Summary of system performance.
     */
    public static class PerformanceSummary {
        public double averageThroughput;
        public double peakThroughput;
        public double averageLatency;
        public double p95Latency;
        public double p99Latency;
        public double uptimePercentage;
        public double errorRate;
        public ResourceUtilization resourceUtilization;
    }

    /**
     * Resource utilization metrics.
     */
    public static class ResourceUtilization {
        public double cpuAverage;
        public double cpuPeak;
        public double memoryAverage;
        public double memoryPeak;
        public double diskIOAverage;
        public double diskIOPeak;
        public double networkAverage;
        public double networkPeak;
    }

    /**
     * Performance bottleneck identification.
     */
    public static class PerformanceBottleneck {
        public String component;
        public String bottleneckType;
        public double severity; // 0.0 to 1.0
        public String description;
        public List<String> affectedMetrics;
        public List<String> suggestedActions;
    }

    /**
     * Optimization recommendation.
     */
    public static class OptimizationRecommendation {
        public String recommendationType;
        public String description;
        public double expectedImprovement; // 0.0 to 1.0
        public String implementation;
        public RecommendationPriority priority;
        public List<String> prerequisites;
    }

    /**
     * Priority levels for recommendations.
     */
    public enum RecommendationPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Trend analysis for metrics.
     */
    public static class TrendAnalysis {
        public String metricName;
        public TrendDirection overallTrend;
        public double trendStrength; // 0.0 to 1.0
        public List<SeasonalPattern> seasonalPatterns;
        public double volatility;
        public String trendDescription;
    }

    /**
     * Seasonal patterns in metrics.
     */
    public static class SeasonalPattern {
        public String patternType; // "daily", "weekly", "monthly"
        public double strength; // 0.0 to 1.0
        public String description;
        public Map<String, Double> patternData;
    }
}