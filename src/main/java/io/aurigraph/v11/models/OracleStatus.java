package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Oracle Status Model
 * Provides oracle service health monitoring and performance metrics
 *
 * Used by /api/v11/oracles/status endpoint
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
public class OracleStatus {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("oracles")
    private List<OracleNode> oracles;

    @JsonProperty("summary")
    private OracleSummary summary;

    @JsonProperty("health_score")
    private double healthScore; // 0.0 to 100.0

    // Constructor
    public OracleStatus() {
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public List<OracleNode> getOracles() { return oracles; }
    public void setOracles(List<OracleNode> oracles) { this.oracles = oracles; }

    public OracleSummary getSummary() { return summary; }
    public void setSummary(OracleSummary summary) { this.summary = summary; }

    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }

    /**
     * Oracle Node
     */
    public static class OracleNode {
        @JsonProperty("oracle_id")
        private String oracleId;

        @JsonProperty("oracle_name")
        private String oracleName;

        @JsonProperty("oracle_type")
        private String oracleType; // "price_feed", "vrf", "automation", "data_feed"

        @JsonProperty("status")
        private String status; // "active", "degraded", "offline", "maintenance"

        @JsonProperty("uptime_percent")
        private double uptimePercent;

        @JsonProperty("response_time_ms")
        private long responseTimeMs;

        @JsonProperty("requests_24h")
        private long requests24h;

        @JsonProperty("errors_24h")
        private long errors24h;

        @JsonProperty("error_rate")
        private double errorRate; // percentage

        @JsonProperty("data_feeds_count")
        private int dataFeedsCount;

        @JsonProperty("last_update")
        private Instant lastUpdate;

        @JsonProperty("version")
        private String version;

        @JsonProperty("location")
        private String location; // geographic location

        @JsonProperty("provider")
        private String provider; // "Chainlink", "Band", "Pyth", etc.

        public OracleNode() {}

        public OracleNode(String id, String name, String type, String status, double uptime,
                         long responseTime, long requests, long errors, int feeds,
                         String version, String location, String provider) {
            this.oracleId = id;
            this.oracleName = name;
            this.oracleType = type;
            this.status = status;
            this.uptimePercent = uptime;
            this.responseTimeMs = responseTime;
            this.requests24h = requests;
            this.errors24h = errors;
            this.errorRate = (requests > 0) ? (errors * 100.0 / requests) : 0.0;
            this.dataFeedsCount = feeds;
            this.lastUpdate = Instant.now();
            this.version = version;
            this.location = location;
            this.provider = provider;
        }

        // Getters and Setters
        public String getOracleId() { return oracleId; }
        public void setOracleId(String oracleId) { this.oracleId = oracleId; }

        public String getOracleName() { return oracleName; }
        public void setOracleName(String oracleName) { this.oracleName = oracleName; }

        public String getOracleType() { return oracleType; }
        public void setOracleType(String oracleType) { this.oracleType = oracleType; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public double getUptimePercent() { return uptimePercent; }
        public void setUptimePercent(double uptimePercent) { this.uptimePercent = uptimePercent; }

        public long getResponseTimeMs() { return responseTimeMs; }
        public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

        public long getRequests24h() { return requests24h; }
        public void setRequests24h(long requests24h) { this.requests24h = requests24h; }

        public long getErrors24h() { return errors24h; }
        public void setErrors24h(long errors24h) { this.errors24h = errors24h; }

        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }

        public int getDataFeedsCount() { return dataFeedsCount; }
        public void setDataFeedsCount(int dataFeedsCount) { this.dataFeedsCount = dataFeedsCount; }

        public Instant getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(Instant lastUpdate) { this.lastUpdate = lastUpdate; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
    }

    /**
     * Oracle Summary
     */
    public static class OracleSummary {
        @JsonProperty("total_oracles")
        private int totalOracles;

        @JsonProperty("active_oracles")
        private int activeOracles;

        @JsonProperty("degraded_oracles")
        private int degradedOracles;

        @JsonProperty("offline_oracles")
        private int offlineOracles;

        @JsonProperty("total_requests_24h")
        private long totalRequests24h;

        @JsonProperty("total_errors_24h")
        private long totalErrors24h;

        @JsonProperty("average_uptime_percent")
        private double averageUptimePercent;

        @JsonProperty("average_response_time_ms")
        private long averageResponseTimeMs;

        @JsonProperty("oracle_types")
        private Map<String, Integer> oracleTypes; // type -> count

        public OracleSummary() {}

        // Getters and Setters
        public int getTotalOracles() { return totalOracles; }
        public void setTotalOracles(int totalOracles) { this.totalOracles = totalOracles; }

        public int getActiveOracles() { return activeOracles; }
        public void setActiveOracles(int activeOracles) { this.activeOracles = activeOracles; }

        public int getDegradedOracles() { return degradedOracles; }
        public void setDegradedOracles(int degradedOracles) { this.degradedOracles = degradedOracles; }

        public int getOfflineOracles() { return offlineOracles; }
        public void setOfflineOracles(int offlineOracles) { this.offlineOracles = offlineOracles; }

        public long getTotalRequests24h() { return totalRequests24h; }
        public void setTotalRequests24h(long totalRequests24h) { this.totalRequests24h = totalRequests24h; }

        public long getTotalErrors24h() { return totalErrors24h; }
        public void setTotalErrors24h(long totalErrors24h) { this.totalErrors24h = totalErrors24h; }

        public double getAverageUptimePercent() { return averageUptimePercent; }
        public void setAverageUptimePercent(double averageUptimePercent) { this.averageUptimePercent = averageUptimePercent; }

        public long getAverageResponseTimeMs() { return averageResponseTimeMs; }
        public void setAverageResponseTimeMs(long averageResponseTimeMs) { this.averageResponseTimeMs = averageResponseTimeMs; }

        public Map<String, Integer> getOracleTypes() { return oracleTypes; }
        public void setOracleTypes(Map<String, Integer> oracleTypes) { this.oracleTypes = oracleTypes; }
    }
}
