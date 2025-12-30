package io.aurigraph.v11.performance;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for high-performance batch transaction processing in Aurigraph V11.
 * 
 * This service provides advanced batch processing capabilities designed to achieve
 * the platform's 2M+ TPS target through optimized transaction batching, parallel
 * processing, and intelligent workload distribution.
 * 
 * Key Features:
 * - Multi-threaded batch processing with configurable batch sizes
 * - Intelligent transaction grouping and dependency resolution
 * - Priority-based processing queues
 * - Real-time performance monitoring and adaptive optimization
 * - Memory-efficient streaming processing for large transaction volumes
 * - Failed transaction retry mechanisms with exponential backoff
 * 
 * Performance Requirements:
 * - Process batches of up to 100K transactions
 * - Achieve sub-second batch processing times
 * - Support concurrent processing of multiple batches
 * - Maintain 99.9% batch success rate
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface BatchProcessor {

    /**
     * Submits a batch of transactions for processing.
     * 
     * @param transactions the list of transactions to process
     * @param batchConfig configuration for batch processing behavior
     * @return Uni containing the batch processing result
     */
    Uni<BatchProcessingResult> submitBatch(
        List<Transaction> transactions,
        BatchProcessingConfig batchConfig
    );

    /**
     * Submits a high-priority batch with expedited processing.
     * 
     * @param transactions the list of high-priority transactions
     * @param maxProcessingTime maximum time allowed for processing in milliseconds
     * @return Uni containing the expedited batch result
     */
    Uni<BatchProcessingResult> submitHighPriorityBatch(
        List<Transaction> transactions,
        long maxProcessingTime
    );

    /**
     * Processes transactions in streaming mode for continuous high-throughput.
     * 
     * @param transactionStream the stream of incoming transactions
     * @param batchSize the size of batches to form from the stream
     * @param processingConfig configuration for stream processing
     * @return Multi streaming batch processing results
     */
    Multi<BatchProcessingResult> processTransactionStream(
        Multi<Transaction> transactionStream,
        int batchSize,
        StreamProcessingConfig processingConfig
    );

    /**
     * Processes a large batch using parallel processing techniques.
     * 
     * @param transactions the list of transactions to process
     * @param parallelism the number of parallel processing threads
     * @param partitionStrategy strategy for partitioning transactions
     * @return Uni containing the parallel processing result
     */
    Uni<ParallelBatchResult> processParallelBatch(
        List<Transaction> transactions,
        int parallelism,
        PartitionStrategy partitionStrategy
    );

    /**
     * Retries failed transactions from a previous batch.
     * 
     * @param batchId the identifier of the batch containing failed transactions
     * @param retryConfig configuration for retry behavior
     * @return Uni containing the retry processing result
     */
    Uni<BatchProcessingResult> retryFailedTransactions(
        String batchId,
        RetryConfig retryConfig
    );

    /**
     * Gets the current status of a processing batch.
     * 
     * @param batchId the batch identifier
     * @return Uni containing the current batch status
     */
    Uni<BatchStatus> getBatchStatus(String batchId);

    /**
     * Gets real-time processing metrics for performance monitoring.
     * 
     * @return Uni containing current processing metrics
     */
    Uni<ProcessingMetrics> getCurrentMetrics();

    /**
     * Monitors processing performance and provides optimization recommendations.
     * 
     * @param monitoringDuration duration to monitor in milliseconds
     * @return Multi streaming performance analysis updates
     */
    Multi<PerformanceAnalysis> monitorProcessingPerformance(long monitoringDuration);

    /**
     * Optimizes batch processing parameters based on historical performance.
     * 
     * @param optimizationTarget the target metric to optimize (throughput, latency, etc.)
     * @return Uni containing optimization recommendations
     */
    Uni<OptimizationRecommendations> optimizeProcessingParameters(OptimizationTarget optimizationTarget);

    /**
     * Schedules recurring batch processing for regular workloads.
     * 
     * @param schedule the processing schedule configuration
     * @param batchTemplate template for generating batches
     * @return Uni containing the scheduling result
     */
    Uni<ScheduledProcessingResult> scheduleRecurringBatch(
        ProcessingSchedule schedule,
        BatchTemplate batchTemplate
    );

    /**
     * Cancels a scheduled batch processing job.
     * 
     * @param scheduleId the identifier of the scheduled processing
     * @return Uni containing the cancellation result
     */
    Uni<Boolean> cancelScheduledProcessing(String scheduleId);

    /**
     * Gets historical batch processing statistics for analysis.
     * 
     * @param fromTimestamp start timestamp for historical data
     * @param toTimestamp end timestamp for historical data
     * @param aggregationInterval interval for data aggregation
     * @return Multi streaming historical statistics
     */
    Multi<BatchStatistics> getHistoricalStatistics(
        long fromTimestamp,
        long toTimestamp,
        AggregationInterval aggregationInterval
    );

    /**
     * Validates a batch before processing to identify potential issues.
     * 
     * @param transactions the list of transactions to validate
     * @param validationLevel the level of validation to perform
     * @return Uni containing the batch validation result
     */
    Uni<BatchValidationResult> validateBatch(
        List<Transaction> transactions,
        ValidationLevel validationLevel
    );

    /**
     * Estimates processing time and resource requirements for a batch.
     * 
     * @param transactions the list of transactions to analyze
     * @param processingConfig the proposed processing configuration
     * @return Uni containing processing estimates
     */
    Uni<ProcessingEstimate> estimateBatchProcessing(
        List<Transaction> transactions,
        BatchProcessingConfig processingConfig
    );

    // Inner classes and enums for data transfer objects

    /**
     * Represents a transaction to be processed in a batch.
     */
    public static class Transaction {
        public String transactionId;
        public String fromAddress;
        public String toAddress;
        public String amount;
        public String data;
        public long timestamp;
        public TransactionPriority priority;
        public Map<String, Object> metadata;
        
        public Transaction(String transactionId, String fromAddress, String toAddress, String amount) {
            this.transactionId = transactionId;
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
            this.priority = TransactionPriority.NORMAL;
        }
    }

    /**
     * Priority levels for transaction processing.
     */
    public enum TransactionPriority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    /**
     * Configuration for batch processing behavior.
     */
    public static class BatchProcessingConfig {
        public int maxBatchSize = 10000;
        public long processingTimeoutMs = 5000;
        public boolean enableParallelProcessing = true;
        public int parallelThreads = 16;
        public boolean enableRetries = true;
        public int maxRetries = 3;
        public long retryDelayMs = 100;
        public boolean enableOptimization = true;
        public ProcessingMode processingMode = ProcessingMode.BALANCED;
    }

    /**
     * Processing modes for different optimization strategies.
     */
    public enum ProcessingMode {
        THROUGHPUT_OPTIMIZED, // Maximize transactions per second
        LATENCY_OPTIMIZED,    // Minimize processing time
        BALANCED,             // Balance throughput and latency
        RESOURCE_EFFICIENT    // Minimize resource usage
    }

    /**
     * Result of batch processing operation.
     */
    public static class BatchProcessingResult {
        public String batchId;
        public int totalTransactions;
        public int successfulTransactions;
        public int failedTransactions;
        public long processingTimeMs;
        public double throughputTPS;
        public List<TransactionResult> transactionResults;
        public ProcessingStatistics statistics;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of individual transaction processing.
     */
    public static class TransactionResult {
        public String transactionId;
        public TransactionStatus status;
        public String resultHash;
        public long processingTimeMs;
        public String errorMessage;
        public Map<String, Object> resultData;
    }

    /**
     * Status of individual transactions.
     */
    public enum TransactionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        RETRYING
    }

    /**
     * Configuration for stream processing.
     */
    public static class StreamProcessingConfig {
        public int bufferSize = 1000;
        public long flushIntervalMs = 1000;
        public boolean enableBackpressure = true;
        public int maxConcurrentBatches = 5;
        public ProcessingMode mode = ProcessingMode.THROUGHPUT_OPTIMIZED;
    }

    /**
     * Result of parallel batch processing.
     */
    public static class ParallelBatchResult {
        public String batchId;
        public int partitions;
        public List<BatchProcessingResult> partitionResults;
        public ProcessingStatistics aggregatedStats;
        public long totalProcessingTimeMs;
        public boolean allPartitionsSuccessful;
    }

    /**
     * Strategies for partitioning transactions for parallel processing.
     */
    public enum PartitionStrategy {
        ROUND_ROBIN,      // Distribute evenly
        BY_SENDER,        // Group by sender address
        BY_AMOUNT,        // Group by transaction amount
        BY_TYPE,          // Group by transaction type
        DEPENDENCY_AWARE  // Consider transaction dependencies
    }

    /**
     * Configuration for retry behavior.
     */
    public static class RetryConfig {
        public int maxRetries = 3;
        public long initialDelayMs = 100;
        public double backoffMultiplier = 2.0;
        public long maxDelayMs = 5000;
        public boolean enableExponentialBackoff = true;
        public List<String> retryableErrors;
    }

    /**
     * Current status of a processing batch.
     */
    public static class BatchStatus {
        public String batchId;
        public BatchProcessingState state;
        public int totalTransactions;
        public int processedTransactions;
        public int failedTransactions;
        public double completionPercentage;
        public long elapsedTimeMs;
        public long estimatedRemainingTimeMs;
        public ProcessingStatistics currentStats;
    }

    /**
     * Processing states for batches.
     */
    public enum BatchProcessingState {
        QUEUED,
        VALIDATING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        RETRYING
    }

    /**
     * Real-time processing metrics.
     */
    public static class ProcessingMetrics {
        public double currentThroughputTPS;
        public double averageLatencyMs;
        public double cpuUtilization;
        public double memoryUtilization;
        public int queuedBatches;
        public int processingBatches;
        public int completedBatches;
        public int failedBatches;
        public long totalTransactionsProcessed;
        public long timestamp;
    }

    /**
     * Performance analysis results.
     */
    public static class PerformanceAnalysis {
        public double averageThroughputTPS;
        public double peakThroughputTPS;
        public double averageLatencyMs;
        public double p95LatencyMs;
        public double p99LatencyMs;
        public double successRate;
        public List<PerformanceBottleneck> bottlenecks;
        public List<String> optimizationSuggestions;
        public long analysisTimestamp;
    }

    /**
     * Identified performance bottlenecks.
     */
    public static class PerformanceBottleneck {
        public String component;
        public BottleneckType type;
        public double severity; // 0.0 to 1.0
        public String description;
        public List<String> recommendations;
    }

    /**
     * Types of performance bottlenecks.
     */
    public enum BottleneckType {
        CPU_BOUND,
        MEMORY_BOUND,
        IO_BOUND,
        NETWORK_BOUND,
        LOCK_CONTENTION,
        QUEUE_CONGESTION
    }

    /**
     * Optimization targets for parameter tuning.
     */
    public enum OptimizationTarget {
        MAXIMIZE_THROUGHPUT,
        MINIMIZE_LATENCY,
        MINIMIZE_RESOURCE_USAGE,
        MAXIMIZE_SUCCESS_RATE,
        BALANCE_ALL_METRICS
    }

    /**
     * Optimization recommendations.
     */
    public static class OptimizationRecommendations {
        public BatchProcessingConfig recommendedConfig;
        public Map<String, Object> parameterChanges;
        public double expectedThroughputImprovement;
        public double expectedLatencyImprovement;
        public String rationale;
        public List<String> warnings;
    }

    /**
     * Processing statistics.
     */
    public static class ProcessingStatistics {
        public long totalProcessingTime;
        public double averageTransactionTime;
        public double throughputTPS;
        public int memoryUsageMB;
        public double cpuTimeMs;
        public int threadPoolUtilization;
        public Map<String, Object> additionalMetrics;
    }

    /**
     * Scheduled processing configuration.
     */
    public static class ProcessingSchedule {
        public String scheduleId;
        public ScheduleType scheduleType;
        public long intervalMs; // For INTERVAL type
        public String cronExpression; // For CRON type
        public long startTime;
        public long endTime; // Optional, for finite schedules
        public boolean enabled;
    }

    /**
     * Types of processing schedules.
     */
    public enum ScheduleType {
        ONCE,
        INTERVAL,
        CRON
    }

    /**
     * Template for generating batches in scheduled processing.
     */
    public static class BatchTemplate {
        public String templateId;
        public BatchProcessingConfig processingConfig;
        public TransactionTemplate transactionTemplate;
        public int estimatedBatchSize;
        public Map<String, Object> templateParameters;
    }

    /**
     * Template for generating transactions.
     */
    public static class TransactionTemplate {
        public String templateType;
        public Map<String, Object> defaultValues;
        public List<String> requiredFields;
        public Map<String, String> validationRules;
    }

    /**
     * Result of scheduled processing setup.
     */
    public static class ScheduledProcessingResult {
        public String scheduleId;
        public ProcessingSchedule schedule;
        public long nextExecutionTime;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Historical batch statistics.
     */
    public static class BatchStatistics {
        public long timestamp;
        public int batchesProcessed;
        public long totalTransactions;
        public double averageThroughputTPS;
        public double averageLatencyMs;
        public double successRate;
        public double resourceUtilization;
        public AggregationInterval interval;
    }

    /**
     * Intervals for data aggregation.
     */
    public enum AggregationInterval {
        MINUTE,
        HOUR,
        DAY,
        WEEK
    }

    /**
     * Batch validation result.
     */
    public static class BatchValidationResult {
        public boolean isValid;
        public List<ValidationError> errors;
        public List<ValidationWarning> warnings;
        public ProcessingEstimate estimate;
        public List<String> recommendations;
    }

    /**
     * Validation error information.
     */
    public static class ValidationError {
        public String transactionId;
        public ErrorType errorType;
        public String errorMessage;
        public String suggestedFix;
    }

    /**
     * Types of validation errors.
     */
    public enum ErrorType {
        INVALID_FORMAT,
        INSUFFICIENT_BALANCE,
        DUPLICATE_TRANSACTION,
        INVALID_SIGNATURE,
        DEPENDENCY_VIOLATION
    }

    /**
     * Validation warning information.
     */
    public static class ValidationWarning {
        public String transactionId;
        public WarningType warningType;
        public String warningMessage;
        public double severityLevel; // 0.0 to 1.0
    }

    /**
     * Types of validation warnings.
     */
    public enum WarningType {
        HIGH_GAS_USAGE,
        POTENTIAL_MEV,
        UNUSUAL_PATTERN,
        PERFORMANCE_CONCERN
    }

    /**
     * Levels of validation to perform.
     */
    public enum ValidationLevel {
        BASIC,      // Format and signature validation
        STANDARD,   // Basic + balance and dependency checks
        THOROUGH,   // Standard + pattern analysis and optimization
        COMPREHENSIVE // Thorough + security and MEV analysis
    }

    /**
     * Processing time and resource estimates.
     */
    public static class ProcessingEstimate {
        public long estimatedProcessingTimeMs;
        public double estimatedThroughputTPS;
        public int estimatedMemoryUsageMB;
        public double estimatedCPUUsage;
        public int recommendedParallelism;
        public ProcessingMode recommendedMode;
        public double confidenceLevel; // 0.0 to 1.0
    }
}