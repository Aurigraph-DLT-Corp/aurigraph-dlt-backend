package io.aurigraph.v11.services;

import io.aurigraph.v11.models.BridgeStatus;
import io.aurigraph.v11.models.BridgeStatus.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge Status Service
 * Provides cross-chain bridge health and operational status
 *
 * Simulates multi-chain bridge network with:
 * - Ethereum Bridge
 * - BSC Bridge
 * - Polygon Bridge
 * - Avalanche Bridge
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@ApplicationScoped
public class BridgeStatusService {

    private static final Logger LOG = Logger.getLogger(BridgeStatusService.class);

    private final Instant startTime = Instant.now();
    private final Map<String, ChainBridge> bridgeMap = new ConcurrentHashMap<>();

    // Simulated statistics
    private long totalTransfers = 0;
    private double totalVolumeUsd = 0;

    public BridgeStatusService() {
        initializeBridges();
    }

    /**
     * Initialize simulated bridge network
     */
    private void initializeBridges() {
        // Ethereum Bridge
        bridgeMap.put("bridge-eth-001", createBridge(
                "bridge-eth-001",
                "Aurigraph",
                "Ethereum",
                "lock-mint",
                Arrays.asList("AUR", "ETH", "USDT", "USDC", "DAI")
        ));

        // BSC Bridge
        bridgeMap.put("bridge-bsc-001", createBridge(
                "bridge-bsc-001",
                "Aurigraph",
                "BSC",
                "burn-mint",
                Arrays.asList("AUR", "BNB", "BUSD", "CAKE")
        ));

        // Polygon Bridge
        bridgeMap.put("bridge-matic-001", createBridge(
                "bridge-matic-001",
                "Aurigraph",
                "Polygon",
                "liquidity",
                Arrays.asList("AUR", "MATIC", "USDC", "WETH")
        ));

        // Avalanche Bridge
        bridgeMap.put("bridge-avax-001", createBridge(
                "bridge-avax-001",
                "Aurigraph",
                "Avalanche",
                "lock-mint",
                Arrays.asList("AUR", "AVAX", "USDT", "WAVAX")
        ));

        LOG.infof("Initialized %d cross-chain bridges", bridgeMap.size());
    }

    /**
     * Create a bridge instance
     */
    private ChainBridge createBridge(String id, String source, String target, String type, List<String> assets) {
        ChainBridge bridge = new ChainBridge();
        bridge.setBridgeId(id);
        bridge.setSourceChain(source);
        bridge.setTargetChain(target);
        bridge.setStatus("active");
        bridge.setBridgeType(type);
        bridge.setHealth(createBridgeHealth());
        bridge.setCapacity(createBridgeCapacity(assets));
        bridge.setLastTransfer(Instant.now().minusSeconds((long)(Math.random() * 300)));
        return bridge;
    }

    /**
     * Create bridge health metrics
     */
    private BridgeHealth createBridgeHealth() {
        BridgeHealth health = new BridgeHealth();

        long uptime = java.time.Duration.between(startTime, Instant.now()).getSeconds();
        health.setUptimeSeconds(uptime + (long)(Math.random() * 1000));
        health.setSuccessRate(0.998 + (Math.random() * 0.002)); // 99.8-100%
        health.setErrorRate(0.5 + (Math.random() * 1.5)); // 0.5-2.0 errors per 1000
        health.setAverageLatencyMs(15000 + (Math.random() * 10000)); // 15-25 seconds
        health.setPendingTransfers((long)(Math.random() * 50));
        health.setStuckTransfers((long)(Math.random() * 3));

        return health;
    }

    /**
     * Create bridge capacity info
     */
    private BridgeCapacity createBridgeCapacity(List<String> assets) {
        BridgeCapacity capacity = new BridgeCapacity();

        double tvl = 1000000 + (Math.random() * 5000000); // $1M - $6M
        capacity.setTotalLockedValueUsd(tvl);
        capacity.setAvailableLiquidityUsd(tvl * (0.6 + Math.random() * 0.3)); // 60-90% available
        capacity.setUtilizationPercent((1.0 - (capacity.getAvailableLiquidityUsd() / tvl)) * 100);
        capacity.setMaxTransferAmountUsd(100000 + (Math.random() * 400000)); // $100K - $500K
        capacity.setSupportedAssets(assets);

        return capacity;
    }

    /**
     * Get bridge status
     */
    public Uni<BridgeStatus> getBridgeStatus() {
        return Uni.createFrom().item(() -> {
            BridgeStatus status = new BridgeStatus();

            // Update bridge states
            List<ChainBridge> bridges = new ArrayList<>(bridgeMap.values());
            bridges.forEach(bridge -> {
                bridge.setHealth(createBridgeHealth());
                bridge.setLastTransfer(Instant.now().minusSeconds((long)(Math.random() * 300)));
            });

            status.setBridges(bridges);
            status.setStatistics(buildStatistics());
            status.setPerformance(buildPerformance());
            status.setAlerts(buildAlerts());
            status.setOverallStatus(calculateOverallStatus(bridges));

            LOG.debugf("Generated bridge status: %s, %d active bridges",
                    status.getOverallStatus(),
                    status.getStatistics().getActiveBridges());

            return status;
        });
    }

    /**
     * Build bridge statistics
     */
    private BridgeStatistics buildStatistics() {
        BridgeStatistics stats = new BridgeStatistics();

        int activeBridges = (int) bridgeMap.values().stream()
                .filter(b -> "active".equals(b.getStatus()))
                .count();

        // Simulated statistics
        totalTransfers = 50000 + (long)(Math.random() * 10000);
        totalVolumeUsd = 150000000 + (Math.random() * 50000000); // $150M - $200M

        stats.setTotalBridges(bridgeMap.size());
        stats.setActiveBridges(activeBridges);
        stats.setTotalTransfers(totalTransfers);
        stats.setTotalVolumeUsd(totalVolumeUsd);
        stats.setTransfers24h((long)(totalTransfers * 0.05)); // 5% in last 24h
        stats.setVolume24hUsd(totalVolumeUsd * 0.03); // 3% volume in last 24h
        stats.setUniqueUsers24h(500 + (long)(Math.random() * 500));

        // Chain distribution
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("Ethereum", (long)(totalTransfers * 0.4));
        distribution.put("BSC", (long)(totalTransfers * 0.25));
        distribution.put("Polygon", (long)(totalTransfers * 0.20));
        distribution.put("Avalanche", (long)(totalTransfers * 0.15));
        stats.setChainDistribution(distribution);

        return stats;
    }

    /**
     * Build performance metrics
     */
    private BridgePerformance buildPerformance() {
        BridgePerformance performance = new BridgePerformance();

        performance.setAverageTransferTimeSeconds(18.5 + (Math.random() * 10.0)); // 18.5-28.5s
        performance.setFastestTransferSeconds(8.0 + (Math.random() * 4.0)); // 8-12s
        performance.setSlowestTransferSeconds(45.0 + (Math.random() * 35.0)); // 45-80s

        long uptime = java.time.Duration.between(startTime, Instant.now()).getSeconds();
        double transfersPerHour = uptime > 0 ? (double)totalTransfers / (uptime / 3600.0) : 0.0;
        performance.setTransfersPerHour(transfersPerHour);

        // Gas efficiency
        GasEfficiency gasEff = new GasEfficiency();
        gasEff.setAverageGasCostUsd(2.5 + (Math.random() * 5.0)); // $2.5-$7.5
        gasEff.setTotalGasSpent24hUsd(5000 + (Math.random() * 3000)); // $5K-$8K per day
        gasEff.setGasOptimizationPercent(35.0 + (Math.random() * 15.0)); // 35-50% optimization
        performance.setGasEfficiency(gasEff);

        return performance;
    }

    /**
     * Build alerts list
     */
    private List<BridgeAlert> buildAlerts() {
        List<BridgeAlert> alerts = new ArrayList<>();

        // Simulated alerts for demonstration
        for (ChainBridge bridge : bridgeMap.values()) {
            if (bridge.getHealth().getStuckTransfers() > 0) {
                alerts.add(new BridgeAlert(
                        "alert-" + UUID.randomUUID().toString().substring(0, 8),
                        "warning",
                        bridge.getBridgeId(),
                        String.format("%d stuck transfers detected on %s bridge",
                                bridge.getHealth().getStuckTransfers(),
                                bridge.getTargetChain())
                ));
            }

            if (bridge.getHealth().getPendingTransfers() > 30) {
                alerts.add(new BridgeAlert(
                        "alert-" + UUID.randomUUID().toString().substring(0, 8),
                        "info",
                        bridge.getBridgeId(),
                        String.format("High pending transfers (%d) on %s bridge",
                                bridge.getHealth().getPendingTransfers(),
                                bridge.getTargetChain())
                ));
            }

            if (bridge.getCapacity().getUtilizationPercent() > 80) {
                alerts.add(new BridgeAlert(
                        "alert-" + UUID.randomUUID().toString().substring(0, 8),
                        "warning",
                        bridge.getBridgeId(),
                        String.format("High capacity utilization (%.1f%%) on %s bridge",
                                bridge.getCapacity().getUtilizationPercent(),
                                bridge.getTargetChain())
                ));
            }
        }

        return alerts;
    }

    /**
     * Calculate overall bridge network status
     */
    private String calculateOverallStatus(List<ChainBridge> bridges) {
        long activeBridges = bridges.stream()
                .filter(b -> "active".equals(b.getStatus()))
                .count();

        double activePercent = (double) activeBridges / bridges.size();

        if (activePercent >= 0.9) return "healthy";
        else if (activePercent >= 0.7) return "degraded";
        else return "critical";
    }

    /**
     * Get individual bridge status
     */
    public Uni<ChainBridge> getBridgeById(String bridgeId) {
        return Uni.createFrom().item(() -> {
            ChainBridge bridge = bridgeMap.get(bridgeId);
            if (bridge != null) {
                bridge.setHealth(createBridgeHealth());
            }
            return bridge;
        });
    }

    /**
     * Get service uptime
     */
    public long getUptimeSeconds() {
        return java.time.Duration.between(startTime, Instant.now()).getSeconds();
    }
}
