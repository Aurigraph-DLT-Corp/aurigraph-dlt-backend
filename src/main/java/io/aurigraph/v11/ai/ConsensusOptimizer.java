package io.aurigraph.v11.ai;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI-Driven Consensus Optimizer
 *
 * Phase 4: AI Optimization
 * - Predictive leader election (ML-based)
 * - Adaptive timeout tuning
 * - Network partition detection
 * - Performance monitoring
 * - Self-healing mechanisms
 *
 * Performance Target: 10% throughput improvement, 20% latency reduction
 */
@ApplicationScoped
public class ConsensusOptimizer {

    private static final Logger LOG = Logger.getLogger(ConsensusOptimizer.class);

    @ConfigProperty(name = "ai.consensus.optimization.enabled", defaultValue = "true")
    boolean optimizationEnabled;

    @ConfigProperty(name = "ai.consensus.learning.rate", defaultValue = "0.01")
    double learningRate;

    // Performance tracking
    private final Map<String, PerformanceHistory> nodePerformance = new ConcurrentHashMap<>();
    private final AtomicLong optimizationsApplied = new AtomicLong(0);
    private final AtomicReference<OptimizationRecommendation> currentRecommendation =
            new AtomicReference<>();

    // ML models (simplified for initial implementation)
    private final LeaderElectionPredictor leaderPredictor = new LeaderElectionPredictor();
    private final TimeoutOptimizer timeoutOptimizer = new TimeoutOptimizer();
    private final PartitionDetector partitionDetector = new PartitionDetector();

    @PostConstruct
    public void initialize() {
        if (optimizationEnabled) {
            LOG.info("AI Consensus Optimizer initialized with learning rate: " + learningRate);
        }
    }

    /**
     * Predict optimal leader based on node performance
     */
    public Uni<LeaderPrediction> predictOptimalLeader(Set<String> candidates) {
        return Uni.createFrom().item(() -> {
            if (!optimizationEnabled || candidates.isEmpty()) {
                return new LeaderPrediction(null, 0.0, "Optimization disabled");
            }

            // Analyze performance history
            Map<String, Double> scores = new HashMap<>();
            for (String nodeId : candidates) {
                PerformanceHistory history = nodePerformance.get(nodeId);
                double score = history != null ?
                        leaderPredictor.calculateLeadershipScore(history) : 0.5;
                scores.put(nodeId, score);
            }

            // Select node with highest score
            String optimalLeader = scores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            double confidence = scores.getOrDefault(optimalLeader, 0.0);

            LOG.debugf("Predicted optimal leader: %s (confidence: %.2f)", optimalLeader, confidence);

            return new LeaderPrediction(optimalLeader, confidence,
                    "ML-based leader prediction");
        });
    }

    /**
     * Optimize election timeout based on network conditions
     */
    public Uni<TimeoutRecommendation> optimizeElectionTimeout(
            long currentTimeout, double avgLatency, double latencyVariance) {
        return Uni.createFrom().item(() -> {
            if (!optimizationEnabled) {
                return new TimeoutRecommendation(currentTimeout, "No change",
                        TimeoutAdjustment.NONE);
            }

            TimeoutOptimizer.TimeoutAnalysis analysis =
                    timeoutOptimizer.analyzeTimeout(currentTimeout, avgLatency, latencyVariance);

            long recommendedTimeout = analysis.recommendedTimeout;
            TimeoutAdjustment adjustment = analysis.adjustment;

            if (recommendedTimeout != currentTimeout) {
                optimizationsApplied.incrementAndGet();
                LOG.infof("Timeout optimization: %dms -> %dms (adjustment: %s)",
                        currentTimeout, recommendedTimeout, adjustment);
            }

            return new TimeoutRecommendation(recommendedTimeout,
                    analysis.reason, adjustment);
        });
    }

    /**
     * Detect network partition
     */
    public Uni<PartitionDetectionResult> detectNetworkPartition(
            Map<String, Long> nodeLastSeen, long currentTime) {
        return Uni.createFrom().item(() -> {
            if (!optimizationEnabled) {
                return new PartitionDetectionResult(false, Collections.emptySet(),
                        "Detection disabled");
            }

            PartitionDetector.PartitionAnalysis analysis =
                    partitionDetector.analyzePartition(nodeLastSeen, currentTime);

            if (analysis.partitionDetected) {
                LOG.warnf("Network partition detected! Unreachable nodes: %s",
                        analysis.unreachableNodes);
            }

            return new PartitionDetectionResult(
                    analysis.partitionDetected,
                    analysis.unreachableNodes,
                    analysis.description
            );
        });
    }

    /**
     * Record performance metrics for a node
     */
    public void recordPerformance(String nodeId, PerformanceMetric metric) {
        if (!optimizationEnabled) return;

        nodePerformance.compute(nodeId, (k, history) -> {
            if (history == null) {
                history = new PerformanceHistory(nodeId);
            }
            history.addMetric(metric);
            return history;
        });
    }

    /**
     * Get optimization recommendations
     */
    public Uni<OptimizationRecommendation> getRecommendations() {
        return Uni.createFrom().item(() -> {
            OptimizationRecommendation recommendation = currentRecommendation.get();
            if (recommendation == null) {
                return new OptimizationRecommendation(
                        Collections.emptyList(),
                        "No recommendations available"
                );
            }
            return recommendation;
        });
    }

    /**
     * Get optimizer statistics
     */
    public Uni<OptimizerStats> getStats() {
        return Uni.createFrom().item(() -> {
            return new OptimizerStats(
                    optimizationEnabled,
                    optimizationsApplied.get(),
                    nodePerformance.size(),
                    learningRate
            );
        });
    }

    // Inner classes for ML models

    /**
     * Leader Election Predictor
     */
    static class LeaderElectionPredictor {
        double calculateLeadershipScore(PerformanceHistory history) {
            // Factors: uptime, latency, throughput, stability
            double uptimeScore = history.getUptimeRatio();
            double latencyScore = 1.0 / (1.0 + history.getAvgLatency() / 100.0);
            double throughputScore = Math.min(1.0, history.getAvgThroughput() / 1_000_000.0);
            double stabilityScore = 1.0 - Math.min(1.0, history.getLatencyVariance() / 50.0);

            // Weighted combination
            return 0.3 * uptimeScore +
                   0.3 * latencyScore +
                   0.2 * throughputScore +
                   0.2 * stabilityScore;
        }
    }

    /**
     * Timeout Optimizer
     */
    static class TimeoutOptimizer {
        static class TimeoutAnalysis {
            final long recommendedTimeout;
            final String reason;
            final TimeoutAdjustment adjustment;

            TimeoutAnalysis(long recommendedTimeout, String reason, TimeoutAdjustment adjustment) {
                this.recommendedTimeout = recommendedTimeout;
                this.reason = reason;
                this.adjustment = adjustment;
            }
        }

        TimeoutAnalysis analyzeTimeout(long currentTimeout, double avgLatency,
                                      double latencyVariance) {
            // Base timeout should be 2x average latency plus variance buffer
            double recommendedBase = (avgLatency * 2.0) + (latencyVariance * 3.0);

            // Add safety margin (20%)
            long recommended = (long) (recommendedBase * 1.2);

            // Clamp to reasonable range
            recommended = Math.max(100, Math.min(500, recommended));

            TimeoutAdjustment adjustment;
            String reason;

            if (recommended > currentTimeout * 1.1) {
                adjustment = TimeoutAdjustment.INCREASE;
                reason = "High latency variance detected";
            } else if (recommended < currentTimeout * 0.9) {
                adjustment = TimeoutAdjustment.DECREASE;
                reason = "Low latency, can reduce timeout";
            } else {
                adjustment = TimeoutAdjustment.NONE;
                reason = "Current timeout is optimal";
            }

            return new TimeoutAnalysis(recommended, reason, adjustment);
        }
    }

    /**
     * Partition Detector
     */
    static class PartitionDetector {
        static class PartitionAnalysis {
            final boolean partitionDetected;
            final Set<String> unreachableNodes;
            final String description;

            PartitionAnalysis(boolean partitionDetected, Set<String> unreachableNodes,
                            String description) {
                this.partitionDetected = partitionDetected;
                this.unreachableNodes = unreachableNodes;
                this.description = description;
            }
        }

        PartitionAnalysis analyzePartition(Map<String, Long> nodeLastSeen, long currentTime) {
            Set<String> unreachable = new HashSet<>();
            long partitionThreshold = 5000; // 5 seconds

            for (Map.Entry<String, Long> entry : nodeLastSeen.entrySet()) {
                long timeSinceLastSeen = currentTime - entry.getValue();
                if (timeSinceLastSeen > partitionThreshold) {
                    unreachable.add(entry.getKey());
                }
            }

            boolean partitionDetected = !unreachable.isEmpty();
            String description = partitionDetected ?
                    String.format("Partition detected: %d nodes unreachable", unreachable.size()) :
                    "No partition detected";

            return new PartitionAnalysis(partitionDetected, unreachable, description);
        }
    }

    // Data classes

    /**
     * Performance metric
     */
    public static class PerformanceMetric {
        public final long timestamp;
        public final double latency;
        public final long throughput;
        public final boolean available;

        public PerformanceMetric(long timestamp, double latency, long throughput, boolean available) {
            this.timestamp = timestamp;
            this.latency = latency;
            this.throughput = throughput;
            this.available = available;
        }
    }

    /**
     * Performance history for a node
     */
    static class PerformanceHistory {
        final String nodeId;
        final List<PerformanceMetric> metrics;
        final int maxHistory = 100;

        PerformanceHistory(String nodeId) {
            this.nodeId = nodeId;
            this.metrics = new ArrayList<>();
        }

        void addMetric(PerformanceMetric metric) {
            synchronized (metrics) {
                metrics.add(metric);
                if (metrics.size() > maxHistory) {
                    metrics.remove(0);
                }
            }
        }

        double getUptimeRatio() {
            if (metrics.isEmpty()) return 0.0;
            long available = metrics.stream().filter(m -> m.available).count();
            return (double) available / metrics.size();
        }

        double getAvgLatency() {
            if (metrics.isEmpty()) return 0.0;
            return metrics.stream()
                    .mapToDouble(m -> m.latency)
                    .average()
                    .orElse(0.0);
        }

        double getLatencyVariance() {
            if (metrics.isEmpty()) return 0.0;
            double avg = getAvgLatency();
            double variance = metrics.stream()
                    .mapToDouble(m -> Math.pow(m.latency - avg, 2))
                    .average()
                    .orElse(0.0);
            return Math.sqrt(variance);
        }

        double getAvgThroughput() {
            if (metrics.isEmpty()) return 0.0;
            return metrics.stream()
                    .mapToDouble(m -> m.throughput)
                    .average()
                    .orElse(0.0);
        }
    }

    /**
     * Leader prediction result
     */
    public static class LeaderPrediction {
        public final String predictedLeader;
        public final double confidence;
        public final String reasoning;

        public LeaderPrediction(String predictedLeader, double confidence, String reasoning) {
            this.predictedLeader = predictedLeader;
            this.confidence = confidence;
            this.reasoning = reasoning;
        }
    }

    /**
     * Timeout recommendation
     */
    public static class TimeoutRecommendation {
        public final long recommendedTimeout;
        public final String reasoning;
        public final TimeoutAdjustment adjustment;

        public TimeoutRecommendation(long recommendedTimeout, String reasoning,
                                    TimeoutAdjustment adjustment) {
            this.recommendedTimeout = recommendedTimeout;
            this.reasoning = reasoning;
            this.adjustment = adjustment;
        }
    }

    /**
     * Timeout adjustment type
     */
    public enum TimeoutAdjustment {
        INCREASE,
        DECREASE,
        NONE
    }

    /**
     * Partition detection result
     */
    public static class PartitionDetectionResult {
        public final boolean partitionDetected;
        public final Set<String> unreachableNodes;
        public final String description;

        public PartitionDetectionResult(boolean partitionDetected,
                                       Set<String> unreachableNodes,
                                       String description) {
            this.partitionDetected = partitionDetected;
            this.unreachableNodes = unreachableNodes;
            this.description = description;
        }
    }

    /**
     * Optimization recommendation
     */
    public static class OptimizationRecommendation {
        public final List<String> recommendations;
        public final String summary;

        public OptimizationRecommendation(List<String> recommendations, String summary) {
            this.recommendations = recommendations;
            this.summary = summary;
        }
    }

    /**
     * Optimizer statistics
     */
    public static class OptimizerStats {
        public final boolean enabled;
        public final long optimizationsApplied;
        public final int nodesTracked;
        public final double learningRate;

        public OptimizerStats(boolean enabled, long optimizationsApplied,
                             int nodesTracked, double learningRate) {
            this.enabled = enabled;
            this.optimizationsApplied = optimizationsApplied;
            this.nodesTracked = nodesTracked;
            this.learningRate = learningRate;
        }
    }
}
