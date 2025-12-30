package io.aurigraph.v11.api;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Sprint 9: Transaction & Validator Analytics REST API
 *
 * Provides analytics endpoints for:
 * - Transaction volume, TPS, fees, success rates
 * - Validator performance, uptime, stake distribution
 *
 * Story Points: 26 (13 + 13)
 * JIRA: AV11-177 (Transaction Analytics) + AV11-178 (Validator Analytics)
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 9
 */
@Path("/api/v11/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Sprint9AnalyticsResource {

    private static final Logger LOG = Logger.getLogger(Sprint9AnalyticsResource.class);

    /**
     * Get transaction analytics for specified time range
     * GET /api/v11/analytics/transactions?timeRange=24h|7d|30d|90d
     */
    @GET
    @Path("/transactions")
    public Uni<TransactionAnalytics> getTransactionAnalytics(
            @QueryParam("timeRange") @DefaultValue("24h") String timeRange) {

        LOG.infof("Fetching transaction analytics for time range: %s", timeRange);

        return Uni.createFrom().item(() -> {
            TransactionAnalytics analytics = new TransactionAnalytics();

            // Mock data - replace with real repository queries in production
            analytics.timeRange = timeRange;
            analytics.totalTransactions = 8_650_000L;
            analytics.peakTPS = 1_850_000L;
            analytics.averageTPS = 950_000L;
            analytics.successRate = 99.98;
            analytics.averageFee = new BigDecimal("0.000012");
            analytics.totalFees = new BigDecimal("103.82");
            analytics.medianFee = new BigDecimal("0.000008");
            analytics.feeTrend = -5.2;

            // Transaction volume data (hourly for 24h)
            analytics.volumeData = generateVolumeData(timeRange);

            // Transaction type distribution
            analytics.typeDistribution = Map.of(
                "transfer", 4_500_000L,
                "smart_contract", 2_100_000L,
                "token", 1_500_000L,
                "nft", 420_000L,
                "governance", 132_000L
            );

            // Peak periods
            analytics.peakPeriods = Arrays.asList(
                new PeakPeriod("2025-10-06T14:00:00Z", 1_850_000L, 1_850_000L, 99.99, 45.2),
                new PeakPeriod("2025-10-06T10:00:00Z", 1_720_000L, 1_720_000L, 99.98, 46.8),
                new PeakPeriod("2025-10-06T16:00:00Z", 1_680_000L, 1_680_000L, 99.97, 47.1),
                new PeakPeriod("2025-10-06T12:00:00Z", 1_620_000L, 1_620_000L, 99.98, 48.2),
                new PeakPeriod("2025-10-06T18:00:00Z", 1_580_000L, 1_580_000L, 99.96, 49.5)
            );

            return analytics;
        });
    }

    /**
     * Get validator analytics
     * GET /api/v11/analytics/validators
     */
    @GET
    @Path("/validators")
    public Uni<ValidatorAnalytics> getValidatorAnalytics() {

        LOG.info("Fetching validator analytics");

        return Uni.createFrom().item(() -> {
            ValidatorAnalytics analytics = new ValidatorAnalytics();

            // Mock data
            analytics.totalValidators = 127;
            analytics.activeValidators = 121;
            analytics.totalStake = new BigDecimal("2450000000"); // 2.45B AUR
            analytics.networkUptime = 99.98;

            // Stake distribution (top 5 + others)
            analytics.stakeDistribution = Map.of(
                "AurigraphValidator-01", new BigDecimal("250000000"),
                "SecureNode-Prime", new BigDecimal("180000000"),
                "QuantumValidator-X", new BigDecimal("150000000"),
                "HyperNode-Alpha", new BigDecimal("120000000"),
                "Others", new BigDecimal("1750000000")
            );

            // Proposal success rate (7 days)
            analytics.proposalSuccessRate = Arrays.asList(
                new DailyMetric("2025-09-30", 99.8),
                new DailyMetric("2025-10-01", 99.7),
                new DailyMetric("2025-10-02", 99.9),
                new DailyMetric("2025-10-03", 99.8),
                new DailyMetric("2025-10-04", 99.9),
                new DailyMetric("2025-10-05", 99.8),
                new DailyMetric("2025-10-06", 99.9)
            );

            // Top validators
            analytics.topValidators = Arrays.asList(
                new ValidatorStats("AurigraphValidator-01", 98.5, 99.99, new BigDecimal("250000000"), 12_450, 99.9, 1_250),
                new ValidatorStats("SecureNode-Prime", 97.8, 99.98, new BigDecimal("180000000"), 11_200, 99.8, 980),
                new ValidatorStats("QuantumValidator-X", 97.2, 99.97, new BigDecimal("150000000"), 10_800, 99.7, 850),
                new ValidatorStats("HyperNode-Alpha", 96.9, 99.96, new BigDecimal("120000000"), 10_200, 99.7, 720),
                new ValidatorStats("EliteValidator-5", 96.5, 99.95, new BigDecimal("100000000"), 9_800, 99.6, 680)
            );

            // Daily rewards (7 days)
            analytics.dailyRewards = Arrays.asList(
                new DailyMetric("2025-09-30", 45_200),
                new DailyMetric("2025-10-01", 46_800),
                new DailyMetric("2025-10-02", 44_500),
                new DailyMetric("2025-10-03", 47_100),
                new DailyMetric("2025-10-04", 45_900),
                new DailyMetric("2025-10-05", 46_300),
                new DailyMetric("2025-10-06", 48_200)
            );

            // Uptime analysis
            analytics.uptimeCategories = Map.of(
                "99-100%", 95,
                "95-99%", 26,
                "<95%", 6
            );

            return analytics;
        });
    }

    /**
     * Generate volume data based on time range
     */
    private List<VolumeDataPoint> generateVolumeData(String timeRange) {
        List<VolumeDataPoint> data = new ArrayList<>();
        Random rand = new Random(42); // Fixed seed for consistent mock data

        int dataPoints = switch (timeRange) {
            case "24h" -> 24;
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            default -> 24;
        };

        Instant now = Instant.now();
        for (int i = dataPoints - 1; i >= 0; i--) {
            long volume = 800_000 + rand.nextInt(400_000); // 800K - 1.2M range
            Instant timestamp = now.minusSeconds(i * 3600L); // Hourly intervals
            data.add(new VolumeDataPoint(timestamp.toString(), volume));
        }

        return data;
    }

    // DTOs

    public static class TransactionAnalytics {
        public String timeRange;
        public long totalTransactions;
        public long peakTPS;
        public long averageTPS;
        public double successRate;
        public BigDecimal averageFee;
        public BigDecimal totalFees;
        public BigDecimal medianFee;
        public double feeTrend;
        public List<VolumeDataPoint> volumeData;
        public Map<String, Long> typeDistribution;
        public List<PeakPeriod> peakPeriods;
    }

    public static class ValidatorAnalytics {
        public int totalValidators;
        public int activeValidators;
        public BigDecimal totalStake;
        public double networkUptime;
        public Map<String, BigDecimal> stakeDistribution;
        public List<DailyMetric> proposalSuccessRate;
        public List<ValidatorStats> topValidators;
        public List<DailyMetric> dailyRewards;
        public Map<String, Integer> uptimeCategories;
    }

    public static class VolumeDataPoint {
        public String timestamp;
        public long volume;

        public VolumeDataPoint(String timestamp, long volume) {
            this.timestamp = timestamp;
            this.volume = volume;
        }
    }

    public static class PeakPeriod {
        public String time;
        public long tps;
        public long totalTx;
        public double successRate;
        public double avgLatency;

        public PeakPeriod(String time, long tps, long totalTx, double successRate, double avgLatency) {
            this.time = time;
            this.tps = tps;
            this.totalTx = totalTx;
            this.successRate = successRate;
            this.avgLatency = avgLatency;
        }
    }

    public static class DailyMetric {
        public String date;
        public double value;

        public DailyMetric(String date, double value) {
            this.date = date;
            this.value = value;
        }
    }

    public static class ValidatorStats {
        public String validator;
        public double performanceScore;
        public double uptime;
        public BigDecimal stake;
        public long blocksProposed;
        public double successRate;
        public long rewards;

        public ValidatorStats(String validator, double performanceScore, double uptime,
                            BigDecimal stake, long blocksProposed, double successRate, long rewards) {
            this.validator = validator;
            this.performanceScore = performanceScore;
            this.uptime = uptime;
            this.stake = stake;
            this.blocksProposed = blocksProposed;
            this.successRate = successRate;
            this.rewards = rewards;
        }
    }
}
