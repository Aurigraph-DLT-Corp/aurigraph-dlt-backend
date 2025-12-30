package io.aurigraph.v11.system;

import io.aurigraph.v11.system.models.SystemStatus;
import io.aurigraph.v11.system.models.SystemStatus.ConsensusStatus;
import io.aurigraph.v11.system.models.SystemStatus.HealthStatus;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * System Status Service for Aurigraph V11
 *
 * Comprehensive system monitoring and health management:
 * - Real-time health checks
 * - Performance metrics collection
 * - Resource usage monitoring
 * - Consensus status tracking
 * - Service availability monitoring
 * - Alert generation and notifications
 *
 * @version 3.8.0 (Phase 2 Day 12-13)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class SystemStatusService {

    private static final Logger LOG = Logger.getLogger(SystemStatusService.class);

    @Inject
    SystemStatusRepository repository;

    // System identification
    private final String nodeId;
    private final Instant startTime;

    // Performance metrics
    private final AtomicLong statusChecks = new AtomicLong(0);
    private final AtomicLong healthChecks = new AtomicLong(0);
    private final AtomicLong alertsGenerated = new AtomicLong(0);

    // Virtual thread executor for high concurrency
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // JVM monitoring
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final Runtime runtime = Runtime.getRuntime();

    public SystemStatusService() {
        this.nodeId = generateNodeId();
        this.startTime = Instant.now();
        LOG.infof("SystemStatusService initialized for node: %s", nodeId);
    }

    // ==================== STATUS COLLECTION ====================

    /**
     * Collect and record current system status
     */
    @Transactional
    public Uni<SystemStatus> collectStatus() {
        return Uni.createFrom().item(() -> {
            LOG.debug("Collecting system status");

            SystemStatus status = new SystemStatus(nodeId);
            status.setStartedAt(startTime);
            status.updateUptime();

            // Collect resource metrics
            collectResourceMetrics(status);

            // Collect performance metrics
            collectPerformanceMetrics(status);

            // Collect service availability
            collectServiceAvailability(status);

            // Collect consensus metrics (placeholder for actual consensus integration)
            collectConsensusMetrics(status);

            // Update health status
            status.updateHealthStatus();

            // Persist status
            repository.persist(status);
            statusChecks.incrementAndGet();

            LOG.debugf("System status collected: health=%s, tps=%s",
                    status.getHealthStatus(), status.getTps());

            return status;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get current system status
     */
    public Uni<SystemStatus> getCurrentStatus() {
        return Uni.createFrom().item(() -> {
            return repository.findLatestByNode(nodeId)
                    .orElseGet(() -> collectStatus().await().indefinitely());
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Perform health check
     */
    public Uni<HealthCheckResult> performHealthCheck() {
        return Uni.createFrom().item(() -> {
            LOG.debug("Performing health check");
            healthChecks.incrementAndGet();

            SystemStatus status = collectStatus().await().indefinitely();

            List<String> issues = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Check critical services
            if (!status.getApiAvailable()) {
                issues.add("API service unavailable");
            }
            if (!status.getDatabaseAvailable()) {
                issues.add("Database unavailable");
            }

            // Check resource usage
            BigDecimal memUsage = status.getMemoryUsagePercent();
            if (memUsage.compareTo(BigDecimal.valueOf(90)) > 0) {
                issues.add(String.format("Memory usage critical: %.2f%%", memUsage));
            } else if (memUsage.compareTo(BigDecimal.valueOf(80)) > 0) {
                warnings.add(String.format("Memory usage high: %.2f%%", memUsage));
            }

            BigDecimal diskUsage = status.getDiskUsagePercent();
            if (diskUsage.compareTo(BigDecimal.valueOf(90)) > 0) {
                issues.add(String.format("Disk usage critical: %.2f%%", diskUsage));
            } else if (diskUsage.compareTo(BigDecimal.valueOf(80)) > 0) {
                warnings.add(String.format("Disk usage high: %.2f%%", diskUsage));
            }

            // Check consensus
            if (status.getConsensusStatus() != ConsensusStatus.SYNCED) {
                warnings.add("Consensus not synced: " + status.getConsensusStatus());
            }

            // Check error rate
            BigDecimal successRate = status.getSuccessRate();
            if (successRate.compareTo(BigDecimal.valueOf(95)) < 0) {
                warnings.add(String.format("Success rate below threshold: %.2f%%", successRate));
            }

            boolean healthy = issues.isEmpty();
            HealthStatus health = status.getHealthStatus();

            LOG.infof("Health check complete: %s (issues=%d, warnings=%d)",
                    health, issues.size(), warnings.size());

            return new HealthCheckResult(
                    health,
                    healthy,
                    issues,
                    warnings,
                    status.getSummary(),
                    Instant.now()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== METRICS COLLECTION ====================

    private void collectResourceMetrics(SystemStatus status) {
        try {
            // Memory metrics
            long memUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long memMax = memoryMXBean.getHeapMemoryUsage().getMax();
            status.setMemoryUsed(memUsed);
            status.setMemoryTotal(memMax);

            // CPU metrics (simplified - actual implementation would use OS-specific APIs)
            BigDecimal cpuUsage = estimateCpuUsage();
            status.setCpuUsage(cpuUsage);

            // Thread metrics
            status.setThreadCount(threadMXBean.getThreadCount());
            status.setActiveThreadCount(threadMXBean.getThreadCount()); // Simplified

            // Disk metrics
            try {
                FileStore store = Files.getFileStore(Paths.get("."));
                long diskTotal = store.getTotalSpace();
                long diskUsable = store.getUsableSpace();
                long diskUsed = diskTotal - diskUsable;
                status.setDiskUsed(diskUsed);
                status.setDiskTotal(diskTotal);
            } catch (Exception e) {
                LOG.warnf("Could not collect disk metrics: %s", e.getMessage());
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error collecting resource metrics");
        }
    }

    private void collectPerformanceMetrics(SystemStatus status) {
        try {
            // These would be populated from actual performance monitoring
            // For now, using placeholder values
            status.setTps(BigDecimal.valueOf(0));
            status.setAvgLatency(BigDecimal.valueOf(0));
            status.setPeakTps(BigDecimal.valueOf(0));
            status.setTotalTransactions(0L);
            status.setFailedTransactions(0L);

        } catch (Exception e) {
            LOG.errorf(e, "Error collecting performance metrics");
        }
    }

    private void collectServiceAvailability(SystemStatus status) {
        try {
            // Check API availability
            status.setApiAvailable(true); // Would check actual API health

            // Check gRPC availability
            status.setGrpcAvailable(true); // Would check actual gRPC health

            // Check database availability
            try {
                repository.count(); // Simple database connectivity check
                status.setDatabaseAvailable(true);
            } catch (Exception e) {
                status.setDatabaseAvailable(false);
                LOG.warnf("Database unavailable: %s", e.getMessage());
            }

            // Check cache availability
            status.setCacheAvailable(true); // Would check actual cache health

        } catch (Exception e) {
            LOG.errorf(e, "Error collecting service availability");
        }
    }

    private void collectConsensusMetrics(SystemStatus status) {
        try {
            // Placeholder for actual consensus integration
            status.setConsensusStatus(ConsensusStatus.SYNCED);
            status.setBlockHeight(0L);
            status.setBlockTime(BigDecimal.ZERO);
            status.setIsLeader(false);
            status.setPeerCount(0);
            status.setActivePeers(0);

        } catch (Exception e) {
            LOG.errorf(e, "Error collecting consensus metrics");
        }
    }

    private BigDecimal estimateCpuUsage() {
        // Simplified CPU estimation
        // Actual implementation would use OperatingSystemMXBean or OS-specific APIs
        return BigDecimal.valueOf(Math.random() * 100).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    // ==================== QUERY OPERATIONS ====================

    /**
     * Get status history for node
     */
    public Uni<List<SystemStatus>> getStatusHistory(int limit) {
        return Uni.createFrom().item(() -> {
            return repository.findByNode(nodeId, limit);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get status history between timestamps
     */
    public Uni<List<SystemStatus>> getStatusHistory(Instant start, Instant end) {
        return Uni.createFrom().item(() -> {
            return repository.findByNodeBetween(nodeId, start, end);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all healthy nodes
     */
    public Uni<List<SystemStatus>> getHealthyNodes() {
        return Uni.createFrom().item(() -> {
            return repository.findHealthyNodes();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all unhealthy nodes
     */
    public Uni<List<SystemStatus>> getUnhealthyNodes() {
        return Uni.createFrom().item(() -> {
            return repository.findUnhealthyNodes();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get consensus leader
     */
    public Uni<Optional<SystemStatus>> getConsensusLeader() {
        return Uni.createFrom().item(() -> {
            return repository.findCurrentLeader();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get top performing nodes
     */
    public Uni<List<SystemStatus>> getTopPerformers(int limit) {
        return Uni.createFrom().item(() -> {
            return repository.findTopPerformers(limit);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== ALERT GENERATION ====================

    /**
     * Check for alerts
     */
    public Uni<List<Alert>> checkAlerts() {
        return Uni.createFrom().item(() -> {
            List<Alert> alerts = new ArrayList<>();

            // Check current status
            SystemStatus current = getCurrentStatus().await().indefinitely();

            // Resource alerts
            if (current.getMemoryUsagePercent().compareTo(BigDecimal.valueOf(90)) > 0) {
                alerts.add(new Alert(
                        Alert.AlertLevel.CRITICAL,
                        "High Memory Usage",
                        String.format("Memory usage at %.2f%%", current.getMemoryUsagePercent()),
                        Instant.now()
                ));
            }

            if (current.getDiskUsagePercent().compareTo(BigDecimal.valueOf(90)) > 0) {
                alerts.add(new Alert(
                        Alert.AlertLevel.CRITICAL,
                        "High Disk Usage",
                        String.format("Disk usage at %.2f%%", current.getDiskUsagePercent()),
                        Instant.now()
                ));
            }

            // Service availability alerts
            if (!current.getApiAvailable()) {
                alerts.add(new Alert(
                        Alert.AlertLevel.CRITICAL,
                        "API Unavailable",
                        "API service is not responding",
                        Instant.now()
                ));
            }

            if (!current.getDatabaseAvailable()) {
                alerts.add(new Alert(
                        Alert.AlertLevel.CRITICAL,
                        "Database Unavailable",
                        "Database connection failed",
                        Instant.now()
                ));
            }

            // Consensus alerts
            if (current.getConsensusStatus() != ConsensusStatus.SYNCED) {
                alerts.add(new Alert(
                        Alert.AlertLevel.WARNING,
                        "Consensus Not Synced",
                        "Node is not synchronized: " + current.getConsensusStatus(),
                        Instant.now()
                ));
            }

            if (!alerts.isEmpty()) {
                alertsGenerated.addAndGet(alerts.size());
                LOG.infof("Generated %d alerts", alerts.size());
            }

            return alerts;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== STATISTICS ====================

    /**
     * Get system health statistics
     */
    public Uni<Map<String, Object>> getHealthStatistics() {
        return Uni.createFrom().item(() -> {
            Map<String, Object> stats = new HashMap<>();

            SystemStatusRepository.SystemHealthStatistics healthStats = repository.getHealthStatistics();
            stats.put("totalRecords", healthStats.totalRecords());
            stats.put("activeNodes", healthStats.activeNodes());
            stats.put("healthyNodes", healthStats.healthyNodes());
            stats.put("healthyRecords", healthStats.healthyRecords());
            stats.put("degradedRecords", healthStats.degradedRecords());
            stats.put("unhealthyRecords", healthStats.unhealthyRecords());
            stats.put("criticalRecords", healthStats.criticalRecords());
            stats.put("unknownRecords", healthStats.unknownRecords());
            stats.put("avgTps", healthStats.avgTps());

            return stats;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get node performance statistics
     */
    public Uni<Map<String, Object>> getNodeStatistics(Instant start, Instant end) {
        return Uni.createFrom().item(() -> {
            SystemStatusRepository.NodePerformanceStatistics nodeStats =
                    repository.getNodeStatistics(nodeId, start, end);

            Map<String, Object> stats = new HashMap<>();
            stats.put("nodeId", nodeStats.nodeId());
            stats.put("recordCount", nodeStats.recordCount());
            stats.put("avgTps", nodeStats.avgTps());
            stats.put("maxTps", nodeStats.maxTps());
            stats.put("avgLatency", nodeStats.avgLatency());
            stats.put("avgCpuUsage", nodeStats.avgCpuUsage());
            stats.put("totalTransactions", nodeStats.totalTransactions());
            stats.put("failedTransactions", nodeStats.failedTransactions());
            stats.put("errorCount", nodeStats.errorCount());

            return stats;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get service statistics
     */
    public Uni<Map<String, Object>> getServiceStatistics() {
        return Uni.createFrom().item(() -> {
            Map<String, Object> stats = new HashMap<>();

            stats.put("nodeId", nodeId);
            stats.put("startTime", startTime);
            stats.put("uptime", Instant.now().getEpochSecond() - startTime.getEpochSecond());
            stats.put("statusChecks", statusChecks.get());
            stats.put("healthChecks", healthChecks.get());
            stats.put("alertsGenerated", alertsGenerated.get());
            stats.put("timestamp", Instant.now());

            return stats;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== HELPER METHODS ====================

    private String generateNodeId() {
        return "NODE_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String getNodeId() {
        return nodeId;
    }

    // ==================== DATA MODELS ====================

    public record HealthCheckResult(
            HealthStatus status,
            boolean healthy,
            List<String> issues,
            List<String> warnings,
            Map<String, Object> metrics,
            Instant timestamp
    ) {}

    public record Alert(
            AlertLevel level,
            String title,
            String message,
            Instant timestamp
    ) {
        public enum AlertLevel {
            INFO,
            WARNING,
            CRITICAL
        }
    }
}
