package io.aurigraph.v11.services;

import io.aurigraph.v11.models.HSMStatus;
import io.aurigraph.v11.models.HSMStatus.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * HSM Status Service
 * Provides Hardware Security Module health and status information
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@ApplicationScoped
public class HSMStatusService {

    private static final Logger LOG = Logger.getLogger(HSMStatusService.class);

    private final Instant startTime = Instant.now();
    private long totalOperations = 0;
    private long successfulOperations = 0;
    private long failedOperations = 0;

    /**
     * Get HSM status
     */
    public Uni<HSMStatus> getHSMStatus() {
        return Uni.createFrom().item(() -> {
            HSMStatus status = new HSMStatus();

            // Modules
            status.setModules(buildModules());

            // Overall status
            status.setOverallStatus(calculateOverallStatus(status.getModules()));

            // Key storage
            status.setKeyStorage(buildKeyStorageInfo());

            // Operations
            status.setOperations(buildOperationStats());

            // Alerts
            status.setAlerts(buildAlerts());

            LOG.debugf("Generated HSM status: %s, %d modules",
                    status.getOverallStatus(), status.getModules().size());

            return status;
        });
    }

    /**
     * Build HSM modules list
     */
    private List<HSMModule> buildModules() {
        List<HSMModule> modules = new ArrayList<>();

        // Simulated HSM modules
        HSMModule module1 = new HSMModule();
        module1.setModuleId("hsm-001");
        module1.setName("Primary HSM");
        module1.setType("hardware");
        module1.setStatus("online");
        module1.setManufacturer("Thales");
        module1.setModel("Luna SA-7");
        module1.setFirmwareVersion("7.8.4");
        module1.setConnection(buildConnectionInfo("10.0.1.100", 3001, 86400));
        module1.setHealth(buildHealthMetrics(45.5, 35.0, 42.0, 0.05, 2.5));
        modules.add(module1);

        HSMModule module2 = new HSMModule();
        module2.setModuleId("hsm-002");
        module2.setName("Backup HSM");
        module2.setType("hardware");
        module2.setStatus("online");
        module2.setManufacturer("Gemalto");
        module2.setModel("SafeNet ProtectServer");
        module2.setFirmwareVersion("3.40.03");
        module2.setConnection(buildConnectionInfo("10.0.1.101", 3001, 86200));
        module2.setHealth(buildHealthMetrics(47.2, 28.0, 38.5, 0.03, 2.8));
        modules.add(module2);

        return modules;
    }

    /**
     * Build connection info
     */
    private ConnectionInfo buildConnectionInfo(String host, int port, long uptime) {
        ConnectionInfo conn = new ConnectionInfo();
        conn.setProtocol("PKCS#11");
        conn.setHost(host);
        conn.setPort(port);
        conn.setSecure(true);
        conn.setLastConnected(Instant.now().minusSeconds(10));
        conn.setUptimeSeconds(uptime);
        return conn;
    }

    /**
     * Build health metrics
     */
    private HealthMetrics buildHealthMetrics(double temp, double cpu, double mem, double errRate, double respTime) {
        HealthMetrics health = new HealthMetrics();
        health.setTemperatureCelsius(temp + (Math.random() * 5.0));
        health.setCpuUsagePercent(cpu + (Math.random() * 10.0));
        health.setMemoryUsagePercent(mem + (Math.random() * 10.0));
        health.setErrorRate(errRate + (Math.random() * 0.02));
        health.setResponseTimeMs(respTime + (Math.random() * 1.0));
        return health;
    }

    /**
     * Calculate overall status
     */
    private String calculateOverallStatus(List<HSMModule> modules) {
        long onlineModules = modules.stream()
                .filter(m -> "online".equals(m.getStatus()))
                .count();

        if (onlineModules == modules.size()) return "online";
        else if (onlineModules > 0) return "degraded";
        else return "offline";
    }

    /**
     * Build key storage info
     */
    private KeyStorageInfo buildKeyStorageInfo() {
        KeyStorageInfo storage = new KeyStorageInfo();

        long total = 175 + (long)(Math.random() * 50);
        storage.setTotalKeys(total);

        Map<String, Long> keyTypes = new HashMap<>();
        keyTypes.put("rsa", 100L);
        keyTypes.put("ecc", 50L);
        keyTypes.put("quantum", 25L);
        storage.setKeyTypes(keyTypes);

        storage.setStorageCapacityKeys(10000L);
        storage.setStorageUsedPercent((double)total / 10000.0 * 100.0);
        storage.setBackedUp(true);
        storage.setLastBackup(Instant.now().minusSeconds(3600));

        return storage;
    }

    /**
     * Build operation stats
     */
    private OperationStats buildOperationStats() {
        OperationStats stats = new OperationStats();

        long uptime = java.time.Duration.between(startTime, Instant.now()).getSeconds();
        totalOperations = 5000 + (long)(Math.random() * 1000);
        successfulOperations = (long)(totalOperations * 0.9995);
        failedOperations = totalOperations - successfulOperations;

        stats.setTotalOperations(totalOperations);
        stats.setSuccessfulOperations(successfulOperations);
        stats.setFailedOperations(failedOperations);
        stats.setSuccessRate(totalOperations > 0 ? (double)successfulOperations / totalOperations : 1.0);
        stats.setOperationsPerSecond(uptime > 0 ? (double)totalOperations / uptime : 0.0);
        stats.setAverageLatencyMs(2.5 + (Math.random() * 1.5));

        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("sign", (long)(totalOperations * 0.4));
        breakdown.put("verify", (long)(totalOperations * 0.35));
        breakdown.put("encrypt", (long)(totalOperations * 0.15));
        breakdown.put("decrypt", (long)(totalOperations * 0.10));
        stats.setOperationBreakdown(breakdown);

        return stats;
    }

    /**
     * Build alerts list
     */
    private List<Alert> buildAlerts() {
        List<Alert> alerts = new ArrayList<>();

        // Simulated alerts (empty for healthy system)
        // In production, this would come from actual monitoring

        return alerts;
    }

    /**
     * Get service uptime
     */
    public long getUptimeSeconds() {
        return java.time.Duration.between(startTime, Instant.now()).getSeconds();
    }
}
