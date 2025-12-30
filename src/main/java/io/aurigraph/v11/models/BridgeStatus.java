package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Bridge Status Model
 * Represents cross-chain bridge health and operational status
 *
 * Used by /api/v11/bridge/status endpoint
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
public class BridgeStatus {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("overall_status")
    private String overallStatus; // "healthy", "degraded", "critical"

    @JsonProperty("bridges")
    private List<ChainBridge> bridges;

    @JsonProperty("statistics")
    private BridgeStatistics statistics;

    @JsonProperty("performance")
    private BridgePerformance performance;

    @JsonProperty("alerts")
    private List<BridgeAlert> alerts;

    // Constructor
    public BridgeStatus() {
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public List<ChainBridge> getBridges() {
        return bridges;
    }

    public void setBridges(List<ChainBridge> bridges) {
        this.bridges = bridges;
    }

    public BridgeStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(BridgeStatistics statistics) {
        this.statistics = statistics;
    }

    public BridgePerformance getPerformance() {
        return performance;
    }

    public void setPerformance(BridgePerformance performance) {
        this.performance = performance;
    }

    public List<BridgeAlert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<BridgeAlert> alerts) {
        this.alerts = alerts;
    }

    /**
     * Individual Chain Bridge Status
     */
    public static class ChainBridge {
        @JsonProperty("bridge_id")
        private String bridgeId;

        @JsonProperty("source_chain")
        private String sourceChain;

        @JsonProperty("target_chain")
        private String targetChain;

        @JsonProperty("status")
        private String status; // "active", "degraded", "offline"

        @JsonProperty("bridge_type")
        private String bridgeType; // "lock-mint", "burn-mint", "liquidity"

        @JsonProperty("health")
        private BridgeHealth health;

        @JsonProperty("capacity")
        private BridgeCapacity capacity;

        @JsonProperty("last_transfer")
        private Instant lastTransfer;

        public ChainBridge() {}

        // Getters and Setters
        public String getBridgeId() { return bridgeId; }
        public void setBridgeId(String bridgeId) { this.bridgeId = bridgeId; }

        public String getSourceChain() { return sourceChain; }
        public void setSourceChain(String sourceChain) { this.sourceChain = sourceChain; }

        public String getTargetChain() { return targetChain; }
        public void setTargetChain(String targetChain) { this.targetChain = targetChain; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getBridgeType() { return bridgeType; }
        public void setBridgeType(String bridgeType) { this.bridgeType = bridgeType; }

        public BridgeHealth getHealth() { return health; }
        public void setHealth(BridgeHealth health) { this.health = health; }

        public BridgeCapacity getCapacity() { return capacity; }
        public void setCapacity(BridgeCapacity capacity) { this.capacity = capacity; }

        public Instant getLastTransfer() { return lastTransfer; }
        public void setLastTransfer(Instant lastTransfer) { this.lastTransfer = lastTransfer; }
    }

    /**
     * Bridge Health Metrics
     */
    public static class BridgeHealth {
        @JsonProperty("uptime_seconds")
        private long uptimeSeconds;

        @JsonProperty("success_rate")
        private double successRate; // 0.0 - 1.0

        @JsonProperty("error_rate")
        private double errorRate; // errors per 1000 transfers

        @JsonProperty("average_latency_ms")
        private double averageLatencyMs;

        @JsonProperty("pending_transfers")
        private long pendingTransfers;

        @JsonProperty("stuck_transfers")
        private long stuckTransfers;

        public BridgeHealth() {}

        // Getters and Setters
        public long getUptimeSeconds() { return uptimeSeconds; }
        public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }

        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }

        public double getAverageLatencyMs() { return averageLatencyMs; }
        public void setAverageLatencyMs(double averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }

        public long getPendingTransfers() { return pendingTransfers; }
        public void setPendingTransfers(long pendingTransfers) { this.pendingTransfers = pendingTransfers; }

        public long getStuckTransfers() { return stuckTransfers; }
        public void setStuckTransfers(long stuckTransfers) { this.stuckTransfers = stuckTransfers; }
    }

    /**
     * Bridge Capacity Information
     */
    public static class BridgeCapacity {
        @JsonProperty("total_locked_value_usd")
        private double totalLockedValueUsd;

        @JsonProperty("available_liquidity_usd")
        private double availableLiquidityUsd;

        @JsonProperty("utilization_percent")
        private double utilizationPercent;

        @JsonProperty("max_transfer_amount_usd")
        private double maxTransferAmountUsd;

        @JsonProperty("supported_assets")
        private List<String> supportedAssets;

        public BridgeCapacity() {}

        // Getters and Setters
        public double getTotalLockedValueUsd() { return totalLockedValueUsd; }
        public void setTotalLockedValueUsd(double totalLockedValueUsd) { this.totalLockedValueUsd = totalLockedValueUsd; }

        public double getAvailableLiquidityUsd() { return availableLiquidityUsd; }
        public void setAvailableLiquidityUsd(double availableLiquidityUsd) { this.availableLiquidityUsd = availableLiquidityUsd; }

        public double getUtilizationPercent() { return utilizationPercent; }
        public void setUtilizationPercent(double utilizationPercent) { this.utilizationPercent = utilizationPercent; }

        public double getMaxTransferAmountUsd() { return maxTransferAmountUsd; }
        public void setMaxTransferAmountUsd(double maxTransferAmountUsd) { this.maxTransferAmountUsd = maxTransferAmountUsd; }

        public List<String> getSupportedAssets() { return supportedAssets; }
        public void setSupportedAssets(List<String> supportedAssets) { this.supportedAssets = supportedAssets; }
    }

    /**
     * Bridge Statistics
     */
    public static class BridgeStatistics {
        @JsonProperty("total_bridges")
        private int totalBridges;

        @JsonProperty("active_bridges")
        private int activeBridges;

        @JsonProperty("total_transfers")
        private long totalTransfers;

        @JsonProperty("total_volume_usd")
        private double totalVolumeUsd;

        @JsonProperty("transfers_24h")
        private long transfers24h;

        @JsonProperty("volume_24h_usd")
        private double volume24hUsd;

        @JsonProperty("unique_users_24h")
        private long uniqueUsers24h;

        @JsonProperty("chain_distribution")
        private Map<String, Long> chainDistribution; // chain -> transfer count

        public BridgeStatistics() {}

        // Getters and Setters
        public int getTotalBridges() { return totalBridges; }
        public void setTotalBridges(int totalBridges) { this.totalBridges = totalBridges; }

        public int getActiveBridges() { return activeBridges; }
        public void setActiveBridges(int activeBridges) { this.activeBridges = activeBridges; }

        public long getTotalTransfers() { return totalTransfers; }
        public void setTotalTransfers(long totalTransfers) { this.totalTransfers = totalTransfers; }

        public double getTotalVolumeUsd() { return totalVolumeUsd; }
        public void setTotalVolumeUsd(double totalVolumeUsd) { this.totalVolumeUsd = totalVolumeUsd; }

        public long getTransfers24h() { return transfers24h; }
        public void setTransfers24h(long transfers24h) { this.transfers24h = transfers24h; }

        public double getVolume24hUsd() { return volume24hUsd; }
        public void setVolume24hUsd(double volume24hUsd) { this.volume24hUsd = volume24hUsd; }

        public long getUniqueUsers24h() { return uniqueUsers24h; }
        public void setUniqueUsers24h(long uniqueUsers24h) { this.uniqueUsers24h = uniqueUsers24h; }

        public Map<String, Long> getChainDistribution() { return chainDistribution; }
        public void setChainDistribution(Map<String, Long> chainDistribution) { this.chainDistribution = chainDistribution; }
    }

    /**
     * Bridge Performance Metrics
     */
    public static class BridgePerformance {
        @JsonProperty("average_transfer_time_seconds")
        private double averageTransferTimeSeconds;

        @JsonProperty("fastest_transfer_seconds")
        private double fastestTransferSeconds;

        @JsonProperty("slowest_transfer_seconds")
        private double slowestTransferSeconds;

        @JsonProperty("transfers_per_hour")
        private double transfersPerHour;

        @JsonProperty("gas_efficiency")
        private GasEfficiency gasEfficiency;

        public BridgePerformance() {}

        // Getters and Setters
        public double getAverageTransferTimeSeconds() { return averageTransferTimeSeconds; }
        public void setAverageTransferTimeSeconds(double averageTransferTimeSeconds) { this.averageTransferTimeSeconds = averageTransferTimeSeconds; }

        public double getFastestTransferSeconds() { return fastestTransferSeconds; }
        public void setFastestTransferSeconds(double fastestTransferSeconds) { this.fastestTransferSeconds = fastestTransferSeconds; }

        public double getSlowestTransferSeconds() { return slowestTransferSeconds; }
        public void setSlowestTransferSeconds(double slowestTransferSeconds) { this.slowestTransferSeconds = slowestTransferSeconds; }

        public double getTransfersPerHour() { return transfersPerHour; }
        public void setTransfersPerHour(double transfersPerHour) { this.transfersPerHour = transfersPerHour; }

        public GasEfficiency getGasEfficiency() { return gasEfficiency; }
        public void setGasEfficiency(GasEfficiency gasEfficiency) { this.gasEfficiency = gasEfficiency; }
    }

    /**
     * Gas Efficiency Metrics
     */
    public static class GasEfficiency {
        @JsonProperty("average_gas_cost_usd")
        private double averageGasCostUsd;

        @JsonProperty("total_gas_spent_24h_usd")
        private double totalGasSpent24hUsd;

        @JsonProperty("gas_optimization_percent")
        private double gasOptimizationPercent;

        public GasEfficiency() {}

        // Getters and Setters
        public double getAverageGasCostUsd() { return averageGasCostUsd; }
        public void setAverageGasCostUsd(double averageGasCostUsd) { this.averageGasCostUsd = averageGasCostUsd; }

        public double getTotalGasSpent24hUsd() { return totalGasSpent24hUsd; }
        public void setTotalGasSpent24hUsd(double totalGasSpent24hUsd) { this.totalGasSpent24hUsd = totalGasSpent24hUsd; }

        public double getGasOptimizationPercent() { return gasOptimizationPercent; }
        public void setGasOptimizationPercent(double gasOptimizationPercent) { this.gasOptimizationPercent = gasOptimizationPercent; }
    }

    /**
     * Bridge Alert
     */
    public static class BridgeAlert {
        @JsonProperty("alert_id")
        private String alertId;

        @JsonProperty("severity")
        private String severity; // "info", "warning", "critical"

        @JsonProperty("bridge_id")
        private String bridgeId;

        @JsonProperty("message")
        private String message;

        @JsonProperty("timestamp")
        private Instant timestamp;

        @JsonProperty("acknowledged")
        private boolean acknowledged;

        public BridgeAlert() {
            this.timestamp = Instant.now();
        }

        public BridgeAlert(String alertId, String severity, String bridgeId, String message) {
            this();
            this.alertId = alertId;
            this.severity = severity;
            this.bridgeId = bridgeId;
            this.message = message;
        }

        // Getters and Setters
        public String getAlertId() { return alertId; }
        public void setAlertId(String alertId) { this.alertId = alertId; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getBridgeId() { return bridgeId; }
        public void setBridgeId(String bridgeId) { this.bridgeId = bridgeId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        public boolean isAcknowledged() { return acknowledged; }
        public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    }
}
