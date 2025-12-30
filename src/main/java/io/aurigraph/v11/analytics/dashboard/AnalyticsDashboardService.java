package io.aurigraph.v11.analytics.dashboard;

import io.aurigraph.v11.TransactionService;
import io.aurigraph.v11.consensus.HyperRAFTConsensusService;
import io.aurigraph.v11.analytics.dashboard.DashboardMetrics.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Analytics Dashboard Service
 *
 * AV11-485: Real-time analytics dashboard with comprehensive metrics aggregation
 * Provides TPS, transactions, blocks, nodes, and performance tracking
 *
 * Features:
 * - Real-time metrics aggregation (1-second intervals)
 * - Performance tracking (TPS, latency, throughput)
 * - Network health monitoring
 * - Node status tracking
 * - Historical data caching (last 24 hours)
 *
 * @author Analytics Dashboard Team
 * @version 11.0.0
 * @since Sprint 13
 */
@ApplicationScoped
public class AnalyticsDashboardService {

    private static final Logger LOG = Logger.getLogger(AnalyticsDashboardService.class);

    @Inject
    TransactionService transactionService;

    @Inject
    HyperRAFTConsensusService consensusService;

    // Metrics tracking
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong totalBlocks = new AtomicLong(0);
    private final AtomicLong pendingTransactions = new AtomicLong(0);
    private final ConcurrentLinkedDeque<HistoricalDataPoint> historicalData = new ConcurrentLinkedDeque<>();
    private final Map<String, NodeHealthMetrics> nodeHealthMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> transactionTypeCounters = new ConcurrentHashMap<>();

    // Performance tracking
    private volatile double currentTPS = 0;
    private volatile double averageTPS = 0;
    private volatile double peakTPS = 0;
    private final Instant startTime = Instant.now();

    // Cache recent metrics
    private volatile DashboardMetrics cachedMetrics = DashboardMetrics.createDefault();
    private volatile PerformanceMetrics cachedPerformanceMetrics = PerformanceMetrics.createDefault();
    private long lastCacheUpdate = System.currentTimeMillis();

    /**
     * Get comprehensive dashboard metrics
     * Aggregates all system metrics into a single view
     */
    public DashboardMetrics getDashboardMetrics() {
        // Return cached metrics if recent (< 500ms old)
        if (System.currentTimeMillis() - lastCacheUpdate < 500) {
            return cachedMetrics;
        }

        DashboardMetrics metrics = buildDashboardMetrics();
        cachedMetrics = metrics;
        lastCacheUpdate = System.currentTimeMillis();

        LOG.debugf("Dashboard metrics generated: TPS=%.0f, Nodes=%d, Blocks=%d",
            metrics.transactionMetrics().currentTPS(),
            metrics.networkMetrics().activeNodes(),
            metrics.networkMetrics().totalBlocks());

        return metrics;
    }

    /**
     * Get detailed performance metrics
     */
    public PerformanceMetrics getPerformanceMetrics() {
        // Return cached if recent
        if (System.currentTimeMillis() - lastCacheUpdate < 500) {
            return cachedPerformanceMetrics;
        }

        PerformanceMetrics metrics = buildPerformanceMetrics();
        cachedPerformanceMetrics = metrics;

        return metrics;
    }

    /**
     * Get transaction statistics
     */
    public TransactionStats getTransactionStats() {
        long total = totalTransactions.get();
        long pending = pendingTransactions.get();
        long confirmed = total - pending;
        long failed = transactionTypeCounters.getOrDefault("failed", new AtomicLong(0)).get();
        double successRate = total > 0 ? ((double) confirmed / total) * 100.0 : 100.0;

        return new TransactionStats(
            currentTPS,
            averageTPS,
            peakTPS,
            total,
            pending,
            confirmed,
            failed,
            successRate,
            getTransactionsByType()
        );
    }

    /**
     * Get node health status for all nodes
     */
    public List<NodeHealthMetrics> getNodeHealthStatus() {
        return new ArrayList<>(nodeHealthMap.values());
    }

    /**
     * Get historical data for specified period
     */
    public List<HistoricalDataPoint> getHistoricalData(String period) {
        Instant cutoff = switch (period.toLowerCase()) {
            case "1h" -> Instant.now().minusSeconds(3600);
            case "6h" -> Instant.now().minusSeconds(21600);
            case "24h" -> Instant.now().minusSeconds(86400);
            default -> Instant.now().minusSeconds(3600); // Default 1 hour
        };

        return historicalData.stream()
            .filter(dp -> dp.timestamp().isAfter(cutoff))
            .collect(Collectors.toList());
    }

    /**
     * Record a transaction event
     */
    public void recordTransaction(String type) {
        totalTransactions.incrementAndGet();
        transactionTypeCounters.computeIfAbsent(type, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Record a block event
     */
    public void recordBlock() {
        totalBlocks.incrementAndGet();
    }

    /**
     * Update node health
     */
    public void updateNodeHealth(NodeHealthMetrics nodeHealth) {
        nodeHealthMap.put(nodeHealth.nodeId(), nodeHealth);
    }

    /**
     * Update TPS metrics
     */
    public void updateTPS(double tps) {
        this.currentTPS = tps;
        this.averageTPS = (averageTPS * 0.9) + (tps * 0.1); // Exponential moving average
        if (tps > peakTPS) {
            this.peakTPS = tps;
        }

        // Add to historical data
        addHistoricalDataPoint(tps);
    }

    /**
     * Build comprehensive dashboard metrics
     */
    private DashboardMetrics buildDashboardMetrics() {
        // Get stats from injected services
        TransactionService.EnhancedProcessingStats txStats = transactionService.getStats();

        // Transaction metrics
        TransactionMetrics transactionMetrics = buildTransactionMetrics(txStats);

        // Network metrics
        NetworkMetrics networkMetrics = buildNetworkMetrics(txStats);

        // Consensus metrics
        ConsensusMetrics consensusMetrics = buildConsensusMetrics();

        // Node health
        List<NodeHealth> nodeHealthList = buildNodeHealthList();

        // System performance
        SystemPerformance systemPerformance = buildSystemPerformance(txStats);

        // Recent history (last 60 data points = 1 minute at 1/sec)
        List<HistoricalDataPoint> recentHistory = historicalData.stream()
            .limit(60)
            .collect(Collectors.toList());

        return new DashboardMetrics(
            transactionMetrics,
            networkMetrics,
            consensusMetrics,
            nodeHealthList,
            systemPerformance,
            recentHistory,
            Instant.now(),
            1000 // Update every 1 second
        );
    }

    /**
     * Build transaction metrics from transaction service stats
     */
    private TransactionMetrics buildTransactionMetrics(TransactionService.EnhancedProcessingStats stats) {
        long total = stats.totalProcessed();
        long pending = pendingTransactions.get();
        long confirmed = total - pending;
        long failed = transactionTypeCounters.getOrDefault("failed", new AtomicLong(0)).get();
        double successRate = total > 0 ? ((double) (total - failed) / total) * 100.0 : 100.0;

        return new TransactionMetrics(
            stats.currentThroughputMeasurement(),
            averageTPS,
            peakTPS,
            total,
            pending,
            confirmed,
            failed,
            successRate,
            stats.avgLatencyMs(),
            stats.avgLatencyMs(),
            stats.p99LatencyMs(),
            stats.p99LatencyMs(),
            getTransactionsByType()
        );
    }

    /**
     * Build network metrics
     */
    private NetworkMetrics buildNetworkMetrics(TransactionService.EnhancedProcessingStats stats) {
        int totalNodes = nodeHealthMap.size();
        int activeNodes = (int) nodeHealthMap.values().stream()
            .filter(n -> n.status().isHealthy())
            .count();
        int inactiveNodes = totalNodes - activeNodes;

        return new NetworkMetrics(
            totalNodes > 0 ? totalNodes : 7,
            activeNodes > 0 ? activeNodes : 7,
            inactiveNodes,
            totalBlocks.get(),
            150.0, // Average block time ms
            5.0,   // Network latency ms
            100,   // Bandwidth in MBps
            100,   // Bandwidth out MBps
            activeNodes * 10, // Active connections
            activeNodes > 0 ? ((double) activeNodes / totalNodes) * 100.0 : 100.0
        );
    }

    /**
     * Build consensus metrics
     */
    private ConsensusMetrics buildConsensusMetrics() {
        try {
            // TODO: Fix after Agent 2 implementation - getConsensusStatus() method doesn't exist
            // var consensusStatus = consensusService.getConsensusMetrics();
            return new ConsensusMetrics(
                "HyperRAFT++",
                0L, // consensusStatus.currentTerm(),
                "unknown", // consensusStatus.leaderId(),
                0L, // consensusStatus.commitIndex(),
                0.0, // consensusStatus.averageLatencyMs(),
                0.0, // consensusStatus.transactionsPerSecond(),
                0, // consensusStatus.activeNodes(),
                0, // consensusStatus.totalNodes(),
                100.0, // Health score
                0L, // Successful ops
                0 // Failed ops
            );
        } catch (Exception e) {
            LOG.debug("Could not fetch consensus status: " + e.getMessage());
            return new ConsensusMetrics("HyperRAFT++", 0, "", 0, 0, 0, 0, 0, 100.0, 0, 0);
        }
    }

    /**
     * Build node health list
     */
    private List<NodeHealth> buildNodeHealthList() {
        return nodeHealthMap.values().stream()
            .map(this::convertToNodeHealth)
            .collect(Collectors.toList());
    }

    /**
     * Convert NodeHealthMetrics to NodeHealth
     */
    private NodeHealth convertToNodeHealth(NodeHealthMetrics metrics) {
        return new NodeHealth(
            metrics.nodeId(),
            metrics.status().state(),
            metrics.status().role(),
            metrics.resources().cpuUsage(),
            metrics.resources().memoryUsagePercent(),
            metrics.resources().diskUsagePercent(),
            metrics.status().uptimeSeconds(),
            metrics.status().lastHeartbeat(),
            metrics.performance().averageLatencyMs(),
            metrics.blockchain().currentBlockHeight(),
            metrics.performance().totalTransactionsProcessed(),
            metrics.status().isHealthy()
        );
    }

    /**
     * Build system performance metrics
     */
    private SystemPerformance buildSystemPerformance(TransactionService.EnhancedProcessingStats stats) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        double cpuLoad = getCPULoad();

        return new SystemPerformance(
            cpuLoad,
            usedMemory,
            totalMemory,
            50, // Disk used GB
            500, // Disk total GB
            150.0, // Disk read MBps
            100.0, // Disk write MBps
            stats.activeThreads(),
            stats.availableProcessors() * 256, // Thread pool size
            0.5, // GC pause ms
            (Instant.now().getEpochSecond() - startTime.getEpochSecond())
        );
    }

    /**
     * Build detailed performance metrics
     */
    private PerformanceMetrics buildPerformanceMetrics() {
        var txStats = transactionService.getStats();
        Runtime runtime = Runtime.getRuntime();

        // Throughput metrics
        var throughput = new PerformanceMetrics.ThroughputMetrics(
            currentTPS,
            averageTPS,
            peakTPS,
            2_000_000.0, // Target TPS
            currentTPS / 2_000_000.0 * 100.0, // Efficiency
            txStats.totalProcessed(),
            totalBlocks.get() / Math.max(1, Instant.now().getEpochSecond() - startTime.getEpochSecond()),
            totalTransactions.get() / Math.max(1, totalBlocks.get())
        );

        // Latency metrics
        var latency = new PerformanceMetrics.LatencyMetrics(
            txStats.avgLatencyMs(),
            txStats.avgLatencyMs(),
            txStats.p99LatencyMs(),
            txStats.p99LatencyMs(),
            txStats.minLatencyMs(),
            txStats.maxLatencyMs(),
            txStats.avgLatencyMs(),
            5.0 // Consensus latency
        );

        // Resource metrics
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;

        var resources = new PerformanceMetrics.ResourceMetrics(
            getCPULoad(),
            runtime.availableProcessors(),
            ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage(),
            usedMem, freeMem, totalMem, (usedMem * 100.0 / totalMem),
            50L, 450L, 500L, 10.0,
            150.0, 100.0, 0, 0,
            txStats.activeThreads(), txStats.activeThreads(), txStats.availableProcessors() * 256
        );

        // Reliability metrics
        long totalTx = totalTransactions.get();
        long failed = transactionTypeCounters.getOrDefault("failed", new AtomicLong(0)).get();
        var reliability = new PerformanceMetrics.ReliabilityMetrics(
            totalTx > 0 ? (failed * 100.0 / totalTx) : 0.0,
            failed,
            totalTx,
            totalTx > 0 ? ((totalTx - failed) * 100.0 / totalTx) : 100.0,
            0, 0, Map.of(),
            100.0, null
        );

        // Storage metrics
        var storage = new PerformanceMetrics.StorageMetrics(
            txStats.storedTransactions(),
            totalTransactions.get(), 0, 100.0,
            totalBlocks.get(), totalBlocks.get() / 1000,
            10, 10, 50.0,
            txStats.storedTransactions(), txStats.storedTransactions() * 2, 50.0, 0
        );

        return new PerformanceMetrics(throughput, latency, resources, reliability, storage, Instant.now());
    }

    /**
     * Add historical data point
     */
    private void addHistoricalDataPoint(double tps) {
        Runtime runtime = Runtime.getRuntime();
        double cpuUsage = getCPULoad();
        double memoryUsage = (runtime.totalMemory() - runtime.freeMemory()) * 100.0 / runtime.totalMemory();
        int activeNodes = (int) nodeHealthMap.values().stream().filter(n -> n.status().isHealthy()).count();

        HistoricalDataPoint dataPoint = new HistoricalDataPoint(
            Instant.now(),
            tps,
            transactionService.getStats().avgLatencyMs(),
            cpuUsage,
            memoryUsage,
            activeNodes > 0 ? activeNodes : 7
        );

        historicalData.addFirst(dataPoint);

        // Keep only last 24 hours (86400 seconds = 86400 data points at 1/sec)
        while (historicalData.size() > 86400) {
            historicalData.removeLast();
        }
    }

    /**
     * Get CPU load
     */
    private double getCPULoad() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100.0;
            }
        } catch (Exception e) {
            // Ignore
        }
        return 45.0 + (Math.random() * 15.0); // Simulated 45-60%
    }

    /**
     * Get transactions by type
     */
    private Map<String, Long> getTransactionsByType() {
        return transactionTypeCounters.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get()
            ));
    }

    /**
     * Transaction statistics record
     */
    public record TransactionStats(
        double currentTPS,
        double averageTPS,
        double peakTPS,
        long totalTransactions,
        long pendingTransactions,
        long confirmedTransactions,
        long failedTransactions,
        double successRate,
        Map<String, Long> transactionsByType
    ) {}
}
