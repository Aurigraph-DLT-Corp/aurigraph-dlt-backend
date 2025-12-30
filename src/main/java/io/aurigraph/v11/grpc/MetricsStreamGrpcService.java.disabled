package io.aurigraph.v11.grpc;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * gRPC Streaming Service for Real-Time Metrics
 *
 * Replaces WebSocket /ws/metrics endpoint with gRPC-Web streaming.
 * Provides high-performance, type-safe, binary protocol for metrics delivery.
 *
 * Features:
 * - Server streaming for real-time metrics updates
 * - Bidirectional streaming for interactive monitoring
 * - Protocol Buffer serialization (75% smaller than JSON)
 * - HTTP/2 multiplexing for efficient connections
 * - Automatic backpressure handling
 *
 * @author Aurigraph V12 gRPC Migration Team
 * @since V12.0.0
 */
@GrpcService
@ApplicationScoped
public class MetricsStreamGrpcService {

    private static final Logger LOG = Logger.getLogger(MetricsStreamGrpcService.class);

    // Connection tracking
    private final ConcurrentHashMap<String, StreamObserver<?>> activeStreams = new ConcurrentHashMap<>();
    private final AtomicLong streamIdCounter = new AtomicLong(0);

    // Simulated metrics state (in production, inject actual services)
    private final Random random = new Random();
    private long totalTransactions = 0;
    private double currentTps = 0;

    /**
     * Get current metrics snapshot (unary RPC)
     */
    public PerformanceMetricsUpdate getCurrentMetrics(MetricsRequest request) {
        LOG.infof("[gRPC] GetCurrentMetrics called for nodes: %s", request.getNodeIdsList());
        return buildCurrentMetrics();
    }

    /**
     * Get aggregated cluster metrics (unary RPC)
     */
    public AggregatedMetrics getAggregatedMetrics(AggregatedMetricsRequest request) {
        LOG.infof("[gRPC] GetAggregatedMetrics called for type: %s", request.getAggregationType());
        return buildAggregatedMetrics(request.getAggregationType());
    }

    /**
     * Stream real-time metrics updates (server streaming)
     *
     * This is the primary replacement for WebSocket /ws/metrics
     */
    public Multi<PerformanceMetricsUpdate> streamMetrics(MetricsSubscription subscription) {
        String streamId = "metrics-" + streamIdCounter.incrementAndGet();
        LOG.infof("[gRPC] StreamMetrics started: streamId=%s, clientId=%s, interval=%dms",
                streamId, subscription.getClientId(), subscription.getUpdateIntervalMs());

        int intervalMs = Math.max(100, Math.min(60000, subscription.getUpdateIntervalMs()));
        if (intervalMs == 0) intervalMs = 1000; // Default 1 second

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> {
                    updateSimulatedMetrics();
                    return buildCurrentMetrics();
                })
                .onSubscription().invoke(() -> LOG.infof("[gRPC] Client subscribed to metrics stream: %s", streamId))
                .onTermination().invoke(() -> LOG.infof("[gRPC] Metrics stream terminated: %s", streamId));
    }

    /**
     * Stream aggregated metrics (server streaming)
     */
    public Multi<AggregatedMetrics> streamAggregatedMetrics(MetricsSubscription subscription) {
        String streamId = "aggregated-" + streamIdCounter.incrementAndGet();
        LOG.infof("[gRPC] StreamAggregatedMetrics started: streamId=%s", streamId);

        int intervalMs = Math.max(1000, subscription.getUpdateIntervalMs());
        if (intervalMs == 0) intervalMs = 5000; // Default 5 seconds for aggregated

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildAggregatedMetrics("cluster"))
                .onSubscription().invoke(() -> LOG.infof("[gRPC] Client subscribed to aggregated metrics: %s", streamId))
                .onTermination().invoke(() -> LOG.infof("[gRPC] Aggregated metrics stream terminated: %s", streamId));
    }

    /**
     * Get historical time-series data (unary RPC)
     */
    public TimeSeriesResponse getTimeSeriesMetrics(TimeSeriesRequest request) {
        LOG.infof("[gRPC] GetTimeSeriesMetrics called for metrics: %s", request.getMetricNamesList());
        return buildTimeSeriesResponse(request);
    }

    /**
     * Bidirectional streaming for interactive monitoring
     *
     * Allows clients to dynamically change subscription parameters
     */
    public Multi<PerformanceMetricsUpdate> interactiveMetrics(Multi<MetricsCommand> commands) {
        String streamId = "interactive-" + streamIdCounter.incrementAndGet();
        LOG.infof("[gRPC] InteractiveMetrics started: streamId=%s", streamId);

        UnicastProcessor<PerformanceMetricsUpdate> processor = UnicastProcessor.create();

        // Default interval
        final int[] currentIntervalMs = {1000};
        final boolean[] isPaused = {false};

        // Start background emitter
        Multi.createFrom().ticks()
                .every(Duration.ofMillis(100)) // Check every 100ms
                .subscribe().with(tick -> {
                    if (!isPaused[0] && tick % (currentIntervalMs[0] / 100) == 0) {
                        updateSimulatedMetrics();
                        processor.onNext(buildCurrentMetrics());
                    }
                });

        // Process incoming commands
        commands.subscribe().with(
                command -> {
                    LOG.infof("[gRPC] Received command: %s", command.getCommand());
                    switch (command.getCommand()) {
                        case CHANGE_INTERVAL:
                            String intervalStr = command.getParametersOrDefault("interval_ms", "1000");
                            currentIntervalMs[0] = Integer.parseInt(intervalStr);
                            LOG.infof("[gRPC] Changed interval to %d ms", currentIntervalMs[0]);
                            break;
                        case PAUSE:
                            isPaused[0] = true;
                            LOG.info("[gRPC] Stream paused");
                            break;
                        case RESUME:
                            isPaused[0] = false;
                            LOG.info("[gRPC] Stream resumed");
                            break;
                        default:
                            LOG.infof("[gRPC] Unhandled command: %s", command.getCommand());
                    }
                },
                error -> LOG.errorf(error, "[gRPC] Command stream error"),
                () -> {
                    LOG.infof("[gRPC] Command stream completed: %s", streamId);
                    processor.onComplete();
                }
        );

        return Multi.createFrom().publisher(processor);
    }

    // ================== Helper Methods ==================

    private void updateSimulatedMetrics() {
        // Simulate TPS fluctuations around 2M
        currentTps = 2_000_000 + (random.nextGaussian() * 100_000);
        totalTransactions += (long) (currentTps / 10); // Assuming 100ms intervals
    }

    private PerformanceMetricsUpdate buildCurrentMetrics() {
        Instant now = Instant.now();

        return PerformanceMetricsUpdate.newBuilder()
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .setNodeId("validator-1")
                .setTransactions(buildTransactionMetrics())
                .setConsensus(buildConsensusPerformance())
                .setNetwork(buildNetworkMetrics())
                .setSystem(buildSystemMetrics())
                .setStorage(buildStorageMetrics())
                .setAiMetrics(buildAIMetrics())
                .build();
    }

    private TransactionMetrics buildTransactionMetrics() {
        return TransactionMetrics.newBuilder()
                .setTps(TPSMetrics.newBuilder()
                        .setCurrent((long) currentTps)
                        .setAvg1M((long) (currentTps * 0.95))
                        .setAvg5M((long) (currentTps * 0.90))
                        .setAvg1H((long) (currentTps * 0.85))
                        .setPeak1M((long) (currentTps * 1.1))
                        .setPeak1H((long) (currentTps * 1.2))
                        .setPeak24H((long) (currentTps * 1.3))
                        .setTrendPercent(random.nextDouble() * 10 - 5)
                        .setTargetTps(3_000_000)
                        .setAchievementPercent((currentTps / 3_000_000) * 100)
                        .build())
                .setLatency(LatencyMetrics.newBuilder()
                        .setAvg(0.5 + random.nextDouble() * 0.2)
                        .setMin(0.1)
                        .setMax(2.0 + random.nextDouble())
                        .setP50(0.4)
                        .setP95(0.8)
                        .setP99(1.2)
                        .setP999(1.8)
                        .setFinalityAvgMs(50 + random.nextDouble() * 10)
                        .setFinalityP99Ms(80 + random.nextDouble() * 20)
                        .setValidationMs(0.1)
                        .setConsensusMs(0.2)
                        .setExecutionMs(0.15)
                        .setCommitMs(0.05)
                        .build())
                .setQueue(QueueMetrics.newBuilder()
                        .setPending(random.nextInt(1000))
                        .setProcessing(random.nextInt(500))
                        .setCompleted((int) totalTransactions % 1000000)
                        .setFailed(random.nextInt(10))
                        .setAvgWaitTimeMs(0.1)
                        .setMaxWaitTimeMs(1.0)
                        .setQueueCapacity(10000000)
                        .setUtilizationPercent(random.nextDouble() * 30)
                        .build())
                .setSuccessRate(99.9 + random.nextDouble() * 0.1)
                .setFailureRate(0.01)
                .setRejectedCount(random.nextInt(5))
                .setGas(GasMetrics.newBuilder()
                        .setAvgGasPrice(25.0)
                        .setMinGasPrice(21.0)
                        .setMaxGasPrice(50.0)
                        .setTotalGasUsed(totalTransactions * 21000)
                        .setGasLimitPerBlock(30000000)
                        .setGasUtilizationPercent(70 + random.nextDouble() * 20)
                        .build())
                .build();
    }

    private ConsensusPerformance buildConsensusPerformance() {
        return ConsensusPerformance.newBuilder()
                .setLeaderNodeId("validator-1")
                .setTerm(12345)
                .setCommitIndex(totalTransactions)
                .setLastApplied(totalTransactions - 10)
                .setAvgBlockTimeMs(100 + random.nextDouble() * 20)
                .setBlocksProduced1M(600 + random.nextInt(50))
                .setBlocksProduced1H(36000 + random.nextInt(500))
                .setAvgVotingTimeMs(5 + random.nextDouble() * 2)
                .setSuccessfulVotes(99)
                .setFailedVotes(1)
                .setVoteSuccessRate(99.0)
                .setLeaderChangesLastHour(0)
                .setLeadershipStabilityScore(100.0)
                .setConsensusOverheadPercent(5.0 + random.nextDouble() * 2)
                .setPendingProposals(random.nextInt(5))
                .build();
    }

    private NetworkMetrics buildNetworkMetrics() {
        return NetworkMetrics.newBuilder()
                .setConnectedPeers(7)
                .setMaxPeers(12)
                .setPeerUtilizationPercent(58.3)
                .setBytesSentPerSec(10_000_000 + random.nextInt(1_000_000))
                .setBytesReceivedPerSec(10_000_000 + random.nextInt(1_000_000))
                .setMessagesSentPerSec(100_000 + random.nextInt(10_000))
                .setMessagesReceivedPerSec(100_000 + random.nextInt(10_000))
                .setAvgPeerLatencyMs(5 + random.nextDouble() * 3)
                .setP95PeerLatencyMs(10 + random.nextDouble() * 5)
                .setDroppedConnections1M(0)
                .setConnectionSuccessRate(100.0)
                .setBandwidthUtilizationPercent(30 + random.nextDouble() * 20)
                .setBandwidthLimitMbps(1000)
                .build();
    }

    private SystemMetrics buildSystemMetrics() {
        return SystemMetrics.newBuilder()
                .setCpuUsagePercent(30 + random.nextDouble() * 20)
                .setMemoryUsedBytes(2_000_000_000L + random.nextInt(500_000_000))
                .setMemoryTotalBytes(8_000_000_000L)
                .setMemoryUsagePercent(35 + random.nextDouble() * 15)
                .setGcPauseMs(1 + random.nextDouble())
                .setActiveThreads(1500 + random.nextInt(500))
                .setVirtualThreadsActive(100_000 + random.nextInt(50_000))
                .build();
    }

    private StorageMetrics buildStorageMetrics() {
        return StorageMetrics.newBuilder()
                .setBlockchainSizeBytes(50_000_000_000L)
                .setStateDbSizeBytes(10_000_000_000L)
                .setRocksdbSizeBytes(20_000_000_000L)
                .setDiskReadsPerSec(10_000 + random.nextInt(5_000))
                .setDiskWritesPerSec(5_000 + random.nextInt(2_000))
                .setDiskReadLatencyMs(0.1 + random.nextDouble() * 0.1)
                .setDiskWriteLatencyMs(0.2 + random.nextDouble() * 0.1)
                .setBlockCacheHitRate(98 + random.nextDouble() * 2)
                .setStateCacheHitRate(95 + random.nextDouble() * 5)
                .setCacheSizeBytes(4_000_000_000L)
                .setPendingCompactions(random.nextInt(3))
                .setCompactionCpuPercent(2 + random.nextDouble() * 3)
                .build();
    }

    private AIMetrics buildAIMetrics() {
        return AIMetrics.newBuilder()
                .setAiEnabled(true)
                .setModelVersion("v12.0.0-quantum")
                .setAiTpsImprovementPercent(15 + random.nextDouble() * 5)
                .setAiLatencyReductionPercent(20 + random.nextDouble() * 5)
                .setModelAccuracy(99.5 + random.nextDouble() * 0.5)
                .setModelLatencyMs(0.5 + random.nextDouble() * 0.2)
                .setPredictionsPerSec(500_000 + random.nextInt(100_000))
                .setOptimizationsApplied1M(120 + random.nextInt(30))
                .setSuccessfulOptimizations(118 + random.nextInt(3))
                .setFailedOptimizations(random.nextInt(3))
                .setOptimizationSuccessRate(98 + random.nextDouble() * 2)
                .setAiCpuUsagePercent(5 + random.nextDouble() * 3)
                .setAiMemoryBytes(500_000_000L + random.nextInt(100_000_000))
                .build();
    }

    private AggregatedMetrics buildAggregatedMetrics(String aggregationType) {
        Instant now = Instant.now();

        return AggregatedMetrics.newBuilder()
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .setAggregationType(aggregationType)
                .setCluster(ClusterMetrics.newBuilder()
                        .setTotalNodes(7)
                        .setActiveNodes(7)
                        .setTotalTps((long) (currentTps * 7))
                        .setAvgTpsPerNode((long) currentTps)
                        .setClusterHealthScore(99.5)
                        .setAvgCpuUsage(35.0)
                        .setAvgMemoryUsage(40.0)
                        .build())
                .addChannels(ChannelMetrics.newBuilder()
                        .setChannelId("main")
                        .setChannelName("Main Channel")
                        .setTps((long) currentTps)
                        .setActiveNodes(7)
                        .setAvgLatencyMs(0.5)
                        .setHealthScore(99.9)
                        .build())
                .putNodeTypes("validator", NodeTypeMetrics.newBuilder()
                        .setNodeType("validator")
                        .setCount(4)
                        .setAvgTps((long) currentTps)
                        .setAvgLatencyMs(0.4)
                        .setAvgCpuPercent(40.0)
                        .setAvgMemoryPercent(45.0)
                        .build())
                .putNodeTypes("business", NodeTypeMetrics.newBuilder()
                        .setNodeType("business")
                        .setCount(2)
                        .setAvgTps((long) (currentTps * 0.8))
                        .setAvgLatencyMs(0.6)
                        .setAvgCpuPercent(30.0)
                        .setAvgMemoryPercent(35.0)
                        .build())
                .putNodeTypes("slim", NodeTypeMetrics.newBuilder()
                        .setNodeType("slim")
                        .setCount(1)
                        .setAvgTps((long) (currentTps * 0.5))
                        .setAvgLatencyMs(0.8)
                        .setAvgCpuPercent(20.0)
                        .setAvgMemoryPercent(25.0)
                        .build())
                .build();
    }

    private TimeSeriesResponse buildTimeSeriesResponse(TimeSeriesRequest request) {
        TimeSeriesResponse.Builder response = TimeSeriesResponse.newBuilder();

        int granularity = request.getGranularitySeconds() > 0 ? request.getGranularitySeconds() : 60;
        int points = 60; // 1 hour of data at default granularity

        for (String metricName : request.getMetricNamesList()) {
            TimeSeriesMetrics.Builder series = TimeSeriesMetrics.newBuilder()
                    .setMetricName(metricName)
                    .setUnit(getMetricUnit(metricName));

            Instant now = Instant.now();
            for (int i = points - 1; i >= 0; i--) {
                Instant pointTime = now.minusSeconds((long) i * granularity);
                series.addPoints(TimeSeriesPoint.newBuilder()
                        .setTimestamp(Timestamp.newBuilder()
                                .setSeconds(pointTime.getEpochSecond())
                                .setNanos(pointTime.getNano())
                                .build())
                        .setValue(getSimulatedValue(metricName, i))
                        .build());
            }

            response.addSeries(series.build());
        }

        response.setTotalPoints(request.getMetricNamesCount() * points);
        return response.build();
    }

    private String getMetricUnit(String metricName) {
        return switch (metricName.toLowerCase()) {
            case "tps" -> "tps";
            case "latency" -> "ms";
            case "cpu" -> "percent";
            case "memory" -> "bytes";
            default -> "units";
        };
    }

    private double getSimulatedValue(String metricName, int offset) {
        double base = switch (metricName.toLowerCase()) {
            case "tps" -> 2_000_000;
            case "latency" -> 0.5;
            case "cpu" -> 35;
            case "memory" -> 40;
            default -> 50;
        };
        return base + (random.nextGaussian() * base * 0.1) - (offset * 0.001);
    }

    /**
     * Get the number of active streams
     */
    public int getActiveStreamCount() {
        return activeStreams.size();
    }
}
