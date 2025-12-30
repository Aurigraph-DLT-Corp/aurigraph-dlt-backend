package io.aurigraph.v11.demo.services;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo Channel Simulation Service with Merkle Tree Registry and Data Feed Tokenization
 *
 * Standard configuration:
 * - Validator Nodes: 5
 * - Business Nodes: 10
 * - Slim Nodes: 5 (one for each external API data feed)
 *
 * Features:
 * - Real-time Merkle tree registry updates
 * - Data feed tokenization for external APIs
 * - Transaction processing simulation
 * - Real-time metrics collection
 */
@ApplicationScoped
public class DemoChannelSimulationService {

    private static final Logger LOG = Logger.getLogger(DemoChannelSimulationService.class);

    private final DataFeedRegistry dataFeedRegistry = new DataFeedRegistry();
    private final Map<String, DemoChannelSimulation> activeSimulations = new ConcurrentHashMap<>();

    // Standard configuration
    private static final int STANDARD_VALIDATORS = 5;
    private static final int STANDARD_BUSINESS_NODES = 10;
    private static final int STANDARD_SLIM_NODES = 5; // One per external API

    /**
     * Demo channel simulation with real-time metrics and Merkle tree
     */
    public static class DemoChannelSimulation {
        public String channelId;
        public String channelName;
        public int validatorNodes;
        public int businessNodes;
        public int slimNodes;
        public Instant startTime;
        public Instant endTime;
        public boolean running;

        // Real-time metrics
        public long totalTransactions;
        public long successfulTransactions;
        public long failedTransactions;
        public double peakTPS;
        public double averageLatency;
        public long blockHeight;
        public String merkleRoot;

        // Data feed statistics
        public int registeredDataFeeds;
        public long totalTokens;
        public long lastMerkleUpdate;

        public DemoChannelSimulation(String channelId, String channelName) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.validatorNodes = STANDARD_VALIDATORS;
            this.businessNodes = STANDARD_BUSINESS_NODES;
            this.slimNodes = STANDARD_SLIM_NODES;
            this.startTime = Instant.now();
            this.running = true;
            this.totalTransactions = 0;
            this.successfulTransactions = 0;
            this.failedTransactions = 0;
            this.peakTPS = 0;
            this.averageLatency = 0;
            this.blockHeight = 0;
            this.registeredDataFeeds = 0;
            this.totalTokens = 0;
        }

        public double getSuccessRate() {
            if (totalTransactions == 0) return 0;
            return (successfulTransactions * 100.0) / totalTransactions;
        }

        public long getDuration() {
            Instant end = running ? Instant.now() : endTime;
            return (end.toEpochMilli() - startTime.toEpochMilli()) / 1000; // seconds
        }
    }

    /**
     * Create and start a new demo channel simulation
     */
    public Uni<DemoChannelSimulation> createAndStartDemo(String channelName) {
        return Uni.createFrom().item(() -> {
            String channelId = "demo-" + UUID.randomUUID().toString().substring(0, 8);
            DemoChannelSimulation simulation = new DemoChannelSimulation(channelId, channelName);

            LOG.infof("Starting demo channel: %s with config (Validators: %d, Business: %d, Slim: %d)",
                channelName, STANDARD_VALIDATORS, STANDARD_BUSINESS_NODES, STANDARD_SLIM_NODES);

            activeSimulations.put(channelId, simulation);

            // Start simulation
            simulateTransactions(simulation);

            return simulation;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Simulate transaction processing and data feed updates
     */
    private void simulateTransactions(DemoChannelSimulation simulation) {
        Thread.startVirtualThread(() -> {
            try {
                // Simulate for 30 seconds
                long startTime = System.currentTimeMillis();
                int txPerBatch = 100;
                int batchInterval = 100; // ms

                while (simulation.running && (System.currentTimeMillis() - startTime) < 30000) {
                    // Process transaction batch
                    for (int i = 0; i < txPerBatch; i++) {
                        simulation.totalTransactions++;

                        // Simulate success rate (95%)
                        if (Math.random() < 0.95) {
                            simulation.successfulTransactions++;
                        } else {
                            simulation.failedTransactions++;
                        }

                        // Simulate latency (10-50ms)
                        double latency = 10 + Math.random() * 40;
                        simulation.averageLatency = (simulation.averageLatency * (simulation.totalTransactions - 1) + latency) / simulation.totalTransactions;

                        // Update block height every 10 transactions
                        if (simulation.totalTransactions % 10 == 0) {
                            simulation.blockHeight++;
                        }

                        // Tokenize data feed periodically
                        if (i % 20 == 0) {
                            tokenizeDataFeed(simulation, i);
                        }
                    }

                    // Calculate TPS for this batch
                    long elapsedMs = System.currentTimeMillis() - startTime;
                    if (elapsedMs > 0) {
                        double currentTPS = (simulation.totalTransactions * 1000.0) / elapsedMs;
                        if (currentTPS > simulation.peakTPS) {
                            simulation.peakTPS = currentTPS;
                        }
                    }

                    LOG.debugf("Demo %s: TX=%d, TPS=%.0f, Latency=%.1f ms, Blocks=%d",
                        simulation.channelId, simulation.totalTransactions, simulation.peakTPS,
                        simulation.averageLatency, simulation.blockHeight);

                    // Update merkle root from registry
                    String root = dataFeedRegistry.getRootHash().await().indefinitely();
                    simulation.merkleRoot = root;
                    simulation.lastMerkleUpdate = System.currentTimeMillis();

                    Thread.sleep(batchInterval);
                }

                // Simulation complete
                simulation.running = false;
                simulation.endTime = Instant.now();

                // Get final Merkle tree stats
                DataFeedRegistry.DataFeedRegistryStats stats = dataFeedRegistry.getStats().await().indefinitely();
                simulation.registeredDataFeeds = stats.apiCount;
                simulation.totalTokens = stats.totalTokens;
                simulation.merkleRoot = stats.rootHash;

                LOG.infof("Demo %s completed: TX=%d (Success: %.1f%%), Peak TPS=%.0f, Blocks=%d, Merkle Root=%s",
                    simulation.channelId, simulation.totalTransactions, simulation.getSuccessRate(),
                    simulation.peakTPS, simulation.blockHeight, simulation.merkleRoot.substring(0, 16));

            } catch (Exception e) {
                LOG.error("Error in demo simulation", e);
                simulation.running = false;
            }
        });
    }

    /**
     * Tokenize data feed from external APIs
     */
    private void tokenizeDataFeed(DemoChannelSimulation simulation, int batchNumber) {
        Thread.startVirtualThread(() -> {
            try {
                // Simulate data from all 5 APIs
                String[] apiIds = {
                    "api-0-price-feed",
                    "api-1-market-data",
                    "api-2-weather-station",
                    "api-3-iot-sensors",
                    "api-4-supply-chain"
                };

                String apiId = apiIds[batchNumber % apiIds.length];

                // Simulate feed data based on API type
                Object feedData = generateFeedData(apiId, batchNumber);

                // Tokenize and register in Merkle tree
                String tokenId = dataFeedRegistry.registerAndTokenizeFeed(apiId, feedData)
                    .await().indefinitely();

                // Update simulation stats
                DataFeedRegistry.DataFeedRegistryStats stats = dataFeedRegistry.getStats().await().indefinitely();
                simulation.totalTokens = stats.totalTokens;
                simulation.registeredDataFeeds = stats.apiCount;
                simulation.merkleRoot = stats.rootHash;

                LOG.debugf("Tokenized feed from %s: %s", apiId, tokenId);

            } catch (Exception e) {
                LOG.debug("Error tokenizing feed", e);
            }
        });
    }

    /**
     * Generate realistic data for each API type
     */
    private Object generateFeedData(String apiId, int batchNumber) {
        Map<String, Object> data = new HashMap<>();
        long timestamp = System.currentTimeMillis();

        switch (apiId) {
            case "api-0-price-feed":
                // Price feed data
                data.put("timestamp", timestamp);
                data.put("symbol", "AURI");
                data.put("price", 100 + (Math.random() * 20));
                data.put("volume", (long)(1000000 + Math.random() * 500000));
                data.put("change24h", -2 + (Math.random() * 4));
                break;

            case "api-1-market-data":
                // Market data
                data.put("timestamp", timestamp);
                data.put("index", "DLT-100");
                data.put("value", 5000 + (Math.random() * 500));
                data.put("trend", Math.random() > 0.5 ? "up" : "down");
                data.put("volatility", Math.random() * 0.1);
                break;

            case "api-2-weather-station":
                // Weather data
                data.put("timestamp", timestamp);
                data.put("location", "Global");
                data.put("temperature", 15 + (Math.random() * 15));
                data.put("humidity", 40 + (Math.random() * 40));
                data.put("pressure", 1010 + (Math.random() * 10));
                data.put("conditions", new String[]{"sunny", "cloudy", "rainy"}[(int)(Math.random() * 3)]);
                break;

            case "api-3-iot-sensors":
                // IoT sensor data
                data.put("timestamp", timestamp);
                data.put("sensorId", "sensor-" + (batchNumber % 10));
                data.put("temperature", 20 + (Math.random() * 10));
                data.put("humidity", 30 + (Math.random() * 40));
                data.put("powerUsage", 100 + (Math.random() * 500));
                data.put("status", "active");
                break;

            case "api-4-supply-chain":
                // Supply chain data
                data.put("timestamp", timestamp);
                data.put("shipmentId", "SHIP-" + batchNumber);
                data.put("status", new String[]{"pending", "in-transit", "delivered"}[(int)(Math.random() * 3)]);
                data.put("location", "Warehouse-" + (batchNumber % 5));
                data.put("temperature", 18 + (Math.random() * 4));
                data.put("items", (int)(10 + Math.random() * 100));
                break;

            default:
                data.put("timestamp", timestamp);
                data.put("value", Math.random());
                break;
        }

        return data;
    }

    /**
     * Get simulation by channel ID
     */
    public Uni<DemoChannelSimulation> getSimulation(String channelId) {
        return Uni.createFrom().item(() -> {
            DemoChannelSimulation sim = activeSimulations.get(channelId);
            if (sim == null) {
                throw new IllegalArgumentException("Channel not found: " + channelId);
            }
            return sim;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get data feed registry
     */
    public DataFeedRegistry getDataFeedRegistry() {
        return dataFeedRegistry;
    }

    /**
     * Get all active simulations
     */
    public Uni<List<DemoChannelSimulation>> getActiveSimulations() {
        return Uni.createFrom().<List<DemoChannelSimulation>>item(() -> new ArrayList<>(activeSimulations.values()))
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Stop a simulation
     */
    public Uni<Boolean> stopSimulation(String channelId) {
        return Uni.createFrom().item(() -> {
            DemoChannelSimulation sim = activeSimulations.get(channelId);
            if (sim != null) {
                sim.running = false;
                sim.endTime = Instant.now();
                LOG.infof("Stopped demo channel: %s", channelId);
                return true;
            }
            return false;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
}
