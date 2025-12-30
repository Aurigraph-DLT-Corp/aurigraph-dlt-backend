package io.aurigraph.v11.monitoring;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * System Monitoring Service
 * Sprint 19 - Workstream 1: Production Ready
 *
 * Comprehensive monitoring with:
 * - Prometheus/Grafana metrics export
 * - Health checks and readiness probes
 * - Performance metrics collection
 * - Alert generation
 * - High availability monitoring
 *
 * Metrics Categories:
 * - System: CPU, Memory, Disk, Network
 * - Application: TPS, Latency, Errors
 * - Blockchain: Block height, Validators, Consensus
 */
@ApplicationScoped
public class SystemMonitoringService {

    private static final Logger LOG = Logger.getLogger(SystemMonitoringService.class);

    private final MetricsCollector metricsCollector;
    private final HealthChecker healthChecker;
    private final AlertEngine alertEngine;
    private final PerformanceMonitor performanceMonitor;
    private final ScheduledExecutorService scheduler;

    // Monitoring state
    private volatile boolean monitoring = false;
    private final Map<String, MetricTimeSeries> metrics;
    private final List<ScheduledFuture<?>> scheduledTasks = new CopyOnWriteArrayList<>();

    // Lazy initialization flag
    private volatile boolean initialized = false;

    public SystemMonitoringService() {
        this.metricsCollector = new MetricsCollector();
        this.healthChecker = new HealthChecker();
        this.alertEngine = new AlertEngine();
        this.performanceMonitor = new PerformanceMonitor();
        this.scheduler = Executors.newScheduledThreadPool(4,
            r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);  // Make threads daemon so they don't block shutdown
                return t;
            });
        this.metrics = new ConcurrentHashMap<>();
    }

    @PostConstruct
    void init() {
        initialized = true;
        LOG.info("System Monitoring Service initialized");
    }

    /**
     * Start monitoring
     * Sprint 19 - Monitoring initiation
     */
    public void startMonitoring() {
        if (monitoring) {
            LOG.warn("Monitoring already started");
            return;
        }

        monitoring = true;
        LOG.info("Starting system monitoring...");

        // Schedule metric collection (every 10 seconds)
        scheduledTasks.add(scheduler.scheduleAtFixedRate(
            this::collectMetrics,
            0, 10, TimeUnit.SECONDS
        ));

        // Schedule health checks (every 30 seconds)
        scheduledTasks.add(scheduler.scheduleAtFixedRate(
            this::performHealthChecks,
            0, 30, TimeUnit.SECONDS
        ));

        // Schedule alert checking (every 60 seconds)
        scheduledTasks.add(scheduler.scheduleAtFixedRate(
            this::checkAlerts,
            60, 60, TimeUnit.SECONDS
        ));

        // Schedule performance analysis (every 5 minutes)
        scheduledTasks.add(scheduler.scheduleAtFixedRate(
            this::analyzePerformance,
            300, 300, TimeUnit.SECONDS
        ));

        LOG.info("System monitoring started");
    }

    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        monitoring = false;

        // Cancel all scheduled tasks
        for (ScheduledFuture<?> task : scheduledTasks) {
            task.cancel(false);
        }
        scheduledTasks.clear();

        LOG.info("System monitoring stopped");
    }

    /**
     * Collect system and application metrics
     * Sprint 19 - Metrics collection
     */
    private void collectMetrics() {
        try {
            long timestamp = System.currentTimeMillis();

            // System metrics
            recordMetric("system.cpu.usage", metricsCollector.getCPUUsage(), timestamp);
            recordMetric("system.memory.used", metricsCollector.getMemoryUsed(), timestamp);
            recordMetric("system.memory.total", metricsCollector.getMemoryTotal(), timestamp);
            recordMetric("system.gc.count", metricsCollector.getGCCount(), timestamp);
            recordMetric("system.gc.time", metricsCollector.getGCTime(), timestamp);
            recordMetric("system.threads.count", metricsCollector.getThreadCount(), timestamp);

            // Application metrics
            recordMetric("app.tps.current", metricsCollector.getCurrentTPS(), timestamp);
            recordMetric("app.transactions.total", metricsCollector.getTotalTransactions(), timestamp);
            recordMetric("app.blocks.height", metricsCollector.getBlockHeight(), timestamp);
            recordMetric("app.validators.active", metricsCollector.getActiveValidators(), timestamp);
            recordMetric("app.latency.avg", metricsCollector.getAverageLatency(), timestamp);
            recordMetric("app.errors.count", metricsCollector.getErrorCount(), timestamp);

            LOG.debug("Metrics collected successfully");

        } catch (Exception e) {
            LOG.error("Failed to collect metrics", e);
        }
    }

    /**
     * Perform health checks
     * Sprint 19 - Health monitoring
     */
    private void performHealthChecks() {
        try {
            HealthStatus status = healthChecker.performChecks();

            if (!status.healthy) {
                LOG.warnf("Health check failed: %s", status.issues);
                alertEngine.triggerAlert(AlertLevel.WARNING,
                    "Health check failed: " + status.issues);
            } else {
                LOG.debug("Health checks passed");
            }

            recordMetric("health.status", status.healthy ? 1.0 : 0.0,
                System.currentTimeMillis());

        } catch (Exception e) {
            LOG.error("Health check failed with exception", e);
        }
    }

    /**
     * Check for alert conditions
     * Sprint 19 - Alert monitoring
     */
    private void checkAlerts() {
        try {
            // Check CPU usage
            double cpuUsage = metricsCollector.getCPUUsage();
            if (cpuUsage > 90.0) {
                alertEngine.triggerAlert(AlertLevel.CRITICAL,
                    String.format("High CPU usage: %.1f%%", cpuUsage));
            }

            // Check memory usage
            double memoryUsage = metricsCollector.getMemoryUsagePercent();
            if (memoryUsage > 85.0) {
                alertEngine.triggerAlert(AlertLevel.WARNING,
                    String.format("High memory usage: %.1f%%", memoryUsage));
            }

            // Check TPS
            double currentTPS = metricsCollector.getCurrentTPS();
            if (currentTPS < 100000) { // Below expected
                alertEngine.triggerAlert(AlertLevel.INFO,
                    String.format("Low TPS: %.0f", currentTPS));
            }

            // Check error rate
            long errorCount = metricsCollector.getErrorCount();
            if (errorCount > 1000) {
                alertEngine.triggerAlert(AlertLevel.ERROR,
                    String.format("High error count: %d", errorCount));
            }

            LOG.debug("Alert checks completed");

        } catch (Exception e) {
            LOG.error("Alert checking failed", e);
        }
    }

    /**
     * Analyze performance
     * Sprint 19 - Performance analysis
     */
    private void analyzePerformance() {
        try {
            PerformanceReport report = performanceMonitor.generateReport();

            LOG.infof("Performance Report: TPS=%.0f, Latency=%.2fms, Memory=%.1f%%, CPU=%.1f%%",
                report.averageTPS, report.averageLatency, report.memoryUsage, report.cpuUsage);

            // Check if performance is degrading
            if (report.performanceDegradation > 20.0) {
                alertEngine.triggerAlert(AlertLevel.WARNING,
                    String.format("Performance degradation detected: %.1f%%",
                        report.performanceDegradation));
            }

        } catch (Exception e) {
            LOG.error("Performance analysis failed", e);
        }
    }

    /**
     * Record metric value
     */
    private void recordMetric(String name, double value, long timestamp) {
        MetricTimeSeries series = metrics.computeIfAbsent(name,
            k -> new MetricTimeSeries(name));
        series.addDataPoint(value, timestamp);
    }

    /**
     * Get metric value
     */
    public MetricValue getMetric(String name) {
        MetricTimeSeries series = metrics.get(name);
        if (series == null) {
            return null;
        }
        return series.getLatestValue();
    }

    /**
     * Get all metrics (for Prometheus export)
     * Sprint 19 - Prometheus integration
     */
    public Map<String, MetricValue> getAllMetrics() {
        Map<String, MetricValue> result = new HashMap<>();
        metrics.forEach((name, series) -> {
            MetricValue value = series.getLatestValue();
            if (value != null) {
                result.put(name, value);
            }
        });
        return result;
    }

    /**
     * Get monitoring status
     */
    public MonitoringStatus getStatus() {
        if (!initialized) {
            // Return default status if not initialized yet
            return new MonitoringStatus(
                false,
                0,
                new HealthStatus(true, new ArrayList<>()),
                0
            );
        }

        return new MonitoringStatus(
            monitoring,
            metrics.size(),
            healthChecker.getLastCheckStatus(),
            alertEngine.getActiveAlertCount()
        );
    }

    // Inner classes

    /**
     * Metrics Collector
     * Collects various system and application metrics
     */
    static class MetricsCollector {
        private final OperatingSystemMXBean osBean;
        private final MemoryMXBean memoryBean;
        private final List<GarbageCollectorMXBean> gcBeans;
        private final ThreadMXBean threadBean;

        public MetricsCollector() {
            this.osBean = ManagementFactory.getOperatingSystemMXBean();
            this.memoryBean = ManagementFactory.getMemoryMXBean();
            this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            this.threadBean = ManagementFactory.getThreadMXBean();
        }

        public double getCPUUsage() {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
                return sunBean.getProcessCpuLoad() * 100;
            }
            return osBean.getSystemLoadAverage();
        }

        public long getMemoryUsed() {
            return memoryBean.getHeapMemoryUsage().getUsed();
        }

        public long getMemoryTotal() {
            return memoryBean.getHeapMemoryUsage().getMax();
        }

        public double getMemoryUsagePercent() {
            MemoryUsage heap = memoryBean.getHeapMemoryUsage();
            return (heap.getUsed() / (double) heap.getMax()) * 100;
        }

        public long getGCCount() {
            return gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        }

        public long getGCTime() {
            return gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        }

        public int getThreadCount() {
            return threadBean.getThreadCount();
        }

        // Application metrics (simplified - would integrate with actual services)
        public double getCurrentTPS() {
            return 500000.0; // Placeholder
        }

        public long getTotalTransactions() {
            return 1000000L; // Placeholder
        }

        public long getBlockHeight() {
            return 10000L; // Placeholder
        }

        public int getActiveValidators() {
            return 10; // Placeholder
        }

        public double getAverageLatency() {
            return 5.0; // 5ms placeholder
        }

        public long getErrorCount() {
            return 0L; // Placeholder
        }
    }

    /**
     * Health Checker
     * Sprint 19 - Health monitoring
     */
    static class HealthChecker {
        private volatile HealthStatus lastStatus = new HealthStatus(true, new ArrayList<>());

        public HealthStatus performChecks() {
            List<String> issues = new ArrayList<>();

            // Check JVM health
            if (!checkJVMHealth()) {
                issues.add("JVM health check failed");
            }

            // Check database connectivity
            if (!checkDatabaseHealth()) {
                issues.add("Database connectivity failed");
            }

            // Check consensus
            if (!checkConsensusHealth()) {
                issues.add("Consensus not operational");
            }

            // Check network
            if (!checkNetworkHealth()) {
                issues.add("Network connectivity issues");
            }

            HealthStatus status = new HealthStatus(issues.isEmpty(), issues);
            lastStatus = status;
            return status;
        }

        public HealthStatus getLastCheckStatus() {
            return lastStatus;
        }

        private boolean checkJVMHealth() {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap = memoryBean.getHeapMemoryUsage();
            return heap.getUsed() < heap.getMax() * 0.95; // Less than 95% used
        }

        private boolean checkDatabaseHealth() {
            return true; // Simplified
        }

        private boolean checkConsensusHealth() {
            return true; // Simplified
        }

        private boolean checkNetworkHealth() {
            return true; // Simplified
        }
    }

    /**
     * Alert Engine
     * Sprint 19 - Alert management
     */
    static class AlertEngine {
        private final List<Alert> activeAlerts = new CopyOnWriteArrayList<>();
        private final AtomicLong alertCounter = new AtomicLong(0);

        public void triggerAlert(AlertLevel level, String message) {
            Alert alert = new Alert(
                alertCounter.incrementAndGet(),
                level,
                message,
                System.currentTimeMillis()
            );

            activeAlerts.add(alert);

            LOG.warnf("ALERT [%s]: %s", level, message);

            // In production, would send to alerting system (PagerDuty, Slack, etc.)
        }

        public int getActiveAlertCount() {
            return activeAlerts.size();
        }

        public List<Alert> getActiveAlerts() {
            return new ArrayList<>(activeAlerts);
        }

        public void clearAlert(long alertId) {
            activeAlerts.removeIf(alert -> alert.id == alertId);
        }
    }

    /**
     * Performance Monitor
     * Sprint 19 - Performance analysis
     */
    static class PerformanceMonitor {
        private final Deque<PerformanceSample> samples = new ConcurrentLinkedDeque<>();
        private static final int MAX_SAMPLES = 100;

        public void recordSample(PerformanceSample sample) {
            samples.addLast(sample);
            if (samples.size() > MAX_SAMPLES) {
                samples.removeFirst();
            }
        }

        public PerformanceReport generateReport() {
            if (samples.isEmpty()) {
                return new PerformanceReport(0, 0, 0, 0, 0);
            }

            double avgTPS = samples.stream()
                .mapToDouble(s -> s.tps)
                .average()
                .orElse(0);

            double avgLatency = samples.stream()
                .mapToDouble(s -> s.latency)
                .average()
                .orElse(0);

            double avgMemory = samples.stream()
                .mapToDouble(s -> s.memoryUsage)
                .average()
                .orElse(0);

            double avgCPU = samples.stream()
                .mapToDouble(s -> s.cpuUsage)
                .average()
                .orElse(0);

            // Calculate performance degradation
            double degradation = calculateDegradation();

            return new PerformanceReport(avgTPS, avgLatency, avgMemory, avgCPU, degradation);
        }

        private double calculateDegradation() {
            // Compare recent vs. older samples
            if (samples.size() < 20) {
                return 0.0;
            }

            List<PerformanceSample> recent = samples.stream()
                .skip(samples.size() - 10)
                .toList();

            List<PerformanceSample> older = samples.stream()
                .limit(10)
                .toList();

            double recentAvgTPS = recent.stream().mapToDouble(s -> s.tps).average().orElse(0);
            double olderAvgTPS = older.stream().mapToDouble(s -> s.tps).average().orElse(0);

            if (olderAvgTPS == 0) return 0.0;

            return ((olderAvgTPS - recentAvgTPS) / olderAvgTPS) * 100;
        }
    }

    /**
     * Metric Time Series
     * Stores time series data for a metric
     */
    static class MetricTimeSeries {
        private final String name;
        private final Deque<DataPoint> dataPoints;
        private static final int MAX_DATA_POINTS = 1000;

        public MetricTimeSeries(String name) {
            this.name = name;
            this.dataPoints = new ConcurrentLinkedDeque<>();
        }

        public void addDataPoint(double value, long timestamp) {
            dataPoints.addLast(new DataPoint(value, timestamp));
            if (dataPoints.size() > MAX_DATA_POINTS) {
                dataPoints.removeFirst();
            }
        }

        public MetricValue getLatestValue() {
            DataPoint latest = dataPoints.peekLast();
            if (latest == null) {
                return null;
            }
            return new MetricValue(name, latest.value, latest.timestamp);
        }

        record DataPoint(double value, long timestamp) {}
    }

    // Data structures

    record HealthStatus(boolean healthy, List<String> issues) {}

    record Alert(long id, AlertLevel level, String message, long timestamp) {}

    record PerformanceSample(double tps, double latency, double memoryUsage,
                            double cpuUsage, long timestamp) {}

    record PerformanceReport(double averageTPS, double averageLatency,
                            double memoryUsage, double cpuUsage,
                            double performanceDegradation) {}

    public record MetricValue(String name, double value, long timestamp) {}

    public record MonitoringStatus(boolean active, int metricsCount,
                                   HealthStatus healthStatus, int activeAlerts) {}

    enum AlertLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}
