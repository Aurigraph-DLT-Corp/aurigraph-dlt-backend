package io.aurigraph.v11.transaction.services;

import io.aurigraph.v11.transaction.models.Transaction;
import io.aurigraph.v11.transaction.models.PerformanceMetrics;
import io.aurigraph.v11.transaction.repositories.PerformanceMetricsRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
@Transactional
public class MonitoringService {

    @Inject
    PerformanceMetricsRepository metricsRepository;

    private static final AtomicLong transactionCounter = new AtomicLong(0);
    private static final AtomicLong totalLatencyMs = new AtomicLong(0);

    public void recordTransactionSubmitted(Transaction transaction) {
        transactionCounter.incrementAndGet();
        Log.debugf("Transaction submitted: %s", transaction.txHash);
    }

    public void recordTransactionConfirmed(Transaction transaction) {
        Log.debugf("Transaction confirmed: %s", transaction.txHash);
    }

    public void recordTransactionFinalized(Transaction transaction) {
        Log.debugf("Transaction finalized: %s", transaction.txHash);
    }

    public void recordTransactionFailed(Transaction transaction) {
        Log.warnf("Transaction failed: %s - %s", transaction.txHash, transaction.errorMessage);

        recordMetric(
            PerformanceMetrics.MetricType.ERROR_RATE,
            BigDecimal.valueOf(1),
            "count"
        );
    }

    public void recordFinality(long finalityMs) {
        recordMetric(
            PerformanceMetrics.MetricType.FINALITY_TIME,
            BigDecimal.valueOf(finalityMs),
            "ms"
        );
        totalLatencyMs.addAndGet(finalityMs);
    }

    public void recordLatency(long latencyMs) {
        recordMetric(
            PerformanceMetrics.MetricType.TRANSACTION_LATENCY,
            BigDecimal.valueOf(latencyMs),
            "ms"
        );
    }

    public void recordThroughput(long tpsCount) {
        recordMetric(
            PerformanceMetrics.MetricType.THROUGHPUT,
            BigDecimal.valueOf(tpsCount),
            "tps"
        );
    }

    public void recordMemoryUsage(long memoryMb) {
        recordMetric(
            PerformanceMetrics.MetricType.MEMORY_USAGE,
            BigDecimal.valueOf(memoryMb),
            "MB"
        );
    }

    public void recordCpuUsage(double cpuPercentage) {
        recordMetric(
            PerformanceMetrics.MetricType.CPU_USAGE,
            BigDecimal.valueOf(cpuPercentage),
            "percent"
        );
    }

    public void recordNetworkLatency(long latencyMs) {
        recordMetric(
            PerformanceMetrics.MetricType.NETWORK_LATENCY,
            BigDecimal.valueOf(latencyMs),
            "ms"
        );
    }

    private void recordMetric(
        PerformanceMetrics.MetricType metricType,
        BigDecimal value,
        String unit
    ) {
        try {
            PerformanceMetrics metric = PerformanceMetrics.builder()
                .metricType(metricType)
                .value(value)
                .unit(unit)
                .timestamp(LocalDateTime.now())
                .sampleCount(1L)
                .build();

            metricsRepository.persist(metric);
        } catch (Exception e) {
            Log.warnf("Failed to record metric: %s", e.getMessage());
        }
    }

    public long getTotalTransactions() {
        return transactionCounter.get();
    }

    public double getAverageLatencyMs() {
        long count = transactionCounter.get();
        if (count == 0) return 0.0;
        return (double) totalLatencyMs.get() / count;
    }

    public void recordConsensusTime(long timeMs) {
        recordMetric(
            PerformanceMetrics.MetricType.CONSENSUS_TIME,
            BigDecimal.valueOf(timeMs),
            "ms"
        );
    }

    public void recordBlockSize(long bytes) {
        recordMetric(
            PerformanceMetrics.MetricType.BLOCK_SIZE,
            BigDecimal.valueOf(bytes),
            "bytes"
        );
    }

    public void recordGasUsage(long gas) {
        recordMetric(
            PerformanceMetrics.MetricType.GAS_USAGE,
            BigDecimal.valueOf(gas),
            "units"
        );
    }
}
