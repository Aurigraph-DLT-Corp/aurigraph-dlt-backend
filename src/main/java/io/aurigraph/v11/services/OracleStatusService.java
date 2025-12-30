package io.aurigraph.v11.services;

import io.aurigraph.v11.models.OracleStatus;
import io.aurigraph.v11.models.OracleStatus.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Oracle Status Service
 * Provides oracle service health monitoring and performance metrics
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@ApplicationScoped
public class OracleStatusService {

    private static final Logger LOG = Logger.getLogger(OracleStatusService.class);

    /**
     * Get overall oracle status
     */
    public Uni<OracleStatus> getOracleStatus() {
        return Uni.createFrom().item(() -> {
            OracleStatus status = new OracleStatus();

            List<OracleNode> oracles = buildOracleNodes();
            status.setOracles(oracles);
            status.setSummary(buildOracleSummary(oracles));
            status.setHealthScore(calculateHealthScore(oracles));

            LOG.debugf("Generated oracle status: %d oracles, %.1f%% health score",
                    (Object) oracles.size(),
                    (Object) status.getHealthScore());

            return status;
        });
    }

    /**
     * Get status for specific oracle
     */
    public Uni<OracleNode> getOracleById(String oracleId) {
        return Uni.createFrom().item(() -> {
            List<OracleNode> oracles = buildOracleNodes();

            return oracles.stream()
                    .filter(o -> o.getOracleId().equals(oracleId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Oracle not found: " + oracleId));
        });
    }

    /**
     * Build oracle nodes with simulated data
     */
    private List<OracleNode> buildOracleNodes() {
        List<OracleNode> oracles = new ArrayList<>();

        // Chainlink Price Feed Oracles
        oracles.add(new OracleNode(
                "oracle-chainlink-001",
                "Chainlink Price Feed - US East",
                "price_feed",
                "active",
                99.8,
                45,
                17280,
                12,
                150,
                "1.12.0",
                "US-East-1",
                "Chainlink"
        ));

        oracles.add(new OracleNode(
                "oracle-chainlink-002",
                "Chainlink Price Feed - EU West",
                "price_feed",
                "active",
                99.5,
                52,
                16800,
                18,
                145,
                "1.12.0",
                "EU-West-1",
                "Chainlink"
        ));

        // Band Protocol Oracles
        oracles.add(new OracleNode(
                "oracle-band-001",
                "Band Protocol - Asia Pacific",
                "price_feed",
                "active",
                98.9,
                38,
                14400,
                28,
                120,
                "2.5.1",
                "AP-Southeast-1",
                "Band Protocol"
        ));

        // Pyth Network Oracles (high frequency)
        oracles.add(new OracleNode(
                "oracle-pyth-001",
                "Pyth Network - US West",
                "price_feed",
                "active",
                99.7,
                25,
                86400,
                45,
                200,
                "3.2.0",
                "US-West-2",
                "Pyth Network"
        ));

        oracles.add(new OracleNode(
                "oracle-pyth-002",
                "Pyth Network - EU Central",
                "price_feed",
                "degraded",
                96.5,
                85,
                82000,
                520,
                195,
                "3.2.0",
                "EU-Central-1",
                "Pyth Network"
        ));

        // API3 Data Feed Oracles
        oracles.add(new OracleNode(
                "oracle-api3-001",
                "API3 Data Feed - US East",
                "data_feed",
                "active",
                99.2,
                55,
                12000,
                22,
                80,
                "0.9.5",
                "US-East-1",
                "API3"
        ));

        // Chainlink VRF (Verifiable Random Function)
        oracles.add(new OracleNode(
                "oracle-chainlink-vrf-001",
                "Chainlink VRF - Global",
                "vrf",
                "active",
                99.9,
                120,
                2400,
                3,
                1,
                "1.12.0",
                "Global",
                "Chainlink"
        ));

        // Chainlink Automation (Keepers)
        oracles.add(new OracleNode(
                "oracle-chainlink-keeper-001",
                "Chainlink Keeper - US",
                "automation",
                "active",
                99.6,
                95,
                5800,
                8,
                1,
                "1.12.0",
                "US-East-1",
                "Chainlink"
        ));

        // DIA Data Oracle
        oracles.add(new OracleNode(
                "oracle-dia-001",
                "DIA Data Oracle - EU",
                "data_feed",
                "active",
                98.5,
                65,
                9600,
                35,
                65,
                "1.8.2",
                "EU-West-1",
                "DIA"
        ));

        // Tellor Oracle
        oracles.add(new OracleNode(
                "oracle-tellor-001",
                "Tellor Oracle - Global",
                "data_feed",
                "active",
                97.8,
                78,
                7200,
                48,
                45,
                "6.1.0",
                "Global",
                "Tellor"
        ));

        return oracles;
    }

    /**
     * Build oracle summary from oracle nodes
     */
    private OracleSummary buildOracleSummary(List<OracleNode> oracles) {
        OracleSummary summary = new OracleSummary();

        int total = oracles.size();
        int active = (int) oracles.stream().filter(o -> "active".equals(o.getStatus())).count();
        int degraded = (int) oracles.stream().filter(o -> "degraded".equals(o.getStatus())).count();
        int offline = (int) oracles.stream().filter(o -> "offline".equals(o.getStatus())).count();

        long totalRequests = oracles.stream().mapToLong(OracleNode::getRequests24h).sum();
        long totalErrors = oracles.stream().mapToLong(OracleNode::getErrors24h).sum();
        double avgUptime = oracles.stream().mapToDouble(OracleNode::getUptimePercent).average().orElse(0.0);
        long avgResponseTime = (long) oracles.stream().mapToLong(OracleNode::getResponseTimeMs).average().orElse(0.0);

        // Count oracle types
        Map<String, Integer> types = new HashMap<>();
        oracles.forEach(o -> {
            String type = o.getOracleType();
            types.put(type, types.getOrDefault(type, 0) + 1);
        });

        summary.setTotalOracles(total);
        summary.setActiveOracles(active);
        summary.setDegradedOracles(degraded);
        summary.setOfflineOracles(offline);
        summary.setTotalRequests24h(totalRequests);
        summary.setTotalErrors24h(totalErrors);
        summary.setAverageUptimePercent(avgUptime);
        summary.setAverageResponseTimeMs(avgResponseTime);
        summary.setOracleTypes(types);

        return summary;
    }

    /**
     * Calculate overall health score
     */
    private double calculateHealthScore(List<OracleNode> oracles) {
        if (oracles.isEmpty()) {
            return 0.0;
        }

        double uptimeScore = oracles.stream()
                .mapToDouble(OracleNode::getUptimePercent)
                .average()
                .orElse(0.0);

        double statusScore = oracles.stream()
                .mapToDouble(o -> {
                    switch (o.getStatus()) {
                        case "active": return 100.0;
                        case "degraded": return 50.0;
                        case "offline": return 0.0;
                        default: return 75.0;
                    }
                })
                .average()
                .orElse(0.0);

        double errorScore = oracles.stream()
                .mapToDouble(o -> {
                    double errorRate = o.getErrorRate();
                    return Math.max(0, 100.0 - (errorRate * 10)); // 10% error = 0 score
                })
                .average()
                .orElse(0.0);

        // Weighted average: 40% uptime, 40% status, 20% error rate
        return (uptimeScore * 0.4) + (statusScore * 0.4) + (errorScore * 0.2);
    }
}
