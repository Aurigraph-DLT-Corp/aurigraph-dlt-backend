package io.aurigraph.v11.services;

import io.aurigraph.v11.models.SystemInfo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * System Information Service
 * Provides platform version and configuration details
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@ApplicationScoped
public class SystemInfoService {

    private static final Logger LOG = Logger.getLogger(SystemInfoService.class);

    private final Instant startTime = Instant.now();

    /**
     * Get comprehensive system information
     */
    public Uni<SystemInfo> getSystemInfo() {
        return Uni.createFrom().item(() -> {
            SystemInfo info = new SystemInfo();

            // Platform information
            info.setPlatform(buildPlatformInfo());

            // Runtime information
            info.setRuntime(buildRuntimeInfo());

            // Features information
            info.setFeatures(buildFeaturesInfo());

            // Network information
            info.setNetwork(buildNetworkInfo());

            // Build information
            info.setBuild(buildBuildInfo());

            LOG.debugf("Generated system info for platform: %s v%s",
                    info.getPlatform().getName(),
                    info.getPlatform().getVersion());

            return info;
        });
    }

    /**
     * Build platform information
     */
    private SystemInfo.PlatformInfo buildPlatformInfo() {
        return new SystemInfo.PlatformInfo(
                "Aurigraph V12",
                "12.0.0",
                "High-performance blockchain platform with quantum-resistant cryptography",
                getEnvironment()
        );
    }

    /**
     * Build runtime information
     */
    private SystemInfo.RuntimeInfo buildRuntimeInfo() {
        SystemInfo.RuntimeInfo runtime = new SystemInfo.RuntimeInfo();

        // Java version
        runtime.setJavaVersion(System.getProperty("java.version"));

        // Quarkus version (from system properties or default)
        runtime.setQuarkusVersion(getQuarkusVersion());

        // GraalVM version (if available)
        runtime.setGraalvmVersion(getGraalVMVersion());

        // Native mode detection
        runtime.setNativeMode(isNativeMode());

        // Uptime
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        runtime.setUptimeSeconds(uptime);
        runtime.setStartTime(startTime);

        return runtime;
    }

    /**
     * Build features information
     */
    private SystemInfo.FeaturesInfo buildFeaturesInfo() {
        SystemInfo.FeaturesInfo features = new SystemInfo.FeaturesInfo();

        features.setConsensus("HyperRAFT++");
        features.setCryptography("Quantum-Resistant (CRYSTALS-Kyber, Dilithium)");
        features.setApiVersion("v11");

        // Enabled modules
        List<String> modules = Arrays.asList(
                "blockchain",
                "consensus",
                "cryptography",
                "smart_contracts",
                "cross_chain_bridge",
                "analytics",
                "live_monitoring",
                "governance",
                "staking",
                "channels"
        );
        features.setEnabledModules(modules);

        // Supported protocols
        List<String> protocols = Arrays.asList("REST", "HTTP/2", "gRPC");
        features.setSupportedProtocols(protocols);

        return features;
    }

    /**
     * Build network information (non-sensitive)
     */
    private SystemInfo.NetworkInfo buildNetworkInfo() {
        SystemInfo.NetworkInfo network = new SystemInfo.NetworkInfo();

        network.setNodeType("validator");
        network.setNetworkId("aurigraph-mainnet");
        network.setClusterSize(7); // HyperRAFT++ 7-node cluster
        network.setApiEndpoint("http://localhost:9003");

        // Ports configuration
        Map<String, Integer> ports = new HashMap<>();
        ports.put("http", 9003);
        ports.put("grpc", 9004);
        ports.put("metrics", 9090);
        network.setPorts(ports);

        return network;
    }

    /**
     * Build build information
     */
    private SystemInfo.BuildInfo buildBuildInfo() {
        SystemInfo.BuildInfo build = new SystemInfo.BuildInfo();

        build.setVersion("12.0.0");
        build.setBuildTimestamp(Instant.now().toString());
        build.setCommitHash(getGitCommitHash());
        build.setBranch("main");
        build.setBuildType(getEnvironment().equals("production") ? "release" : "development");

        return build;
    }

    /**
     * Get environment (development, staging, production)
     */
    private String getEnvironment() {
        String env = System.getenv("AURIGRAPH_ENV");
        if (env == null || env.isEmpty()) {
            env = System.getProperty("quarkus.profile", "dev");
        }

        // Map Quarkus profiles to environment names
        switch (env) {
            case "prod":
                return "production";
            case "staging":
                return "staging";
            default:
                return "development";
        }
    }

    /**
     * Get Quarkus version
     */
    private String getQuarkusVersion() {
        // Try to get from system property
        String version = System.getProperty("quarkus.version");
        if (version != null && !version.isEmpty()) {
            return version;
        }

        // Default known version
        return "3.28.2";
    }

    /**
     * Get GraalVM version if available
     */
    private String getGraalVMVersion() {
        String vmName = System.getProperty("java.vm.name");
        String vmVersion = System.getProperty("java.vm.version");

        if (vmName != null && vmName.contains("GraalVM")) {
            return vmVersion != null ? vmVersion : "Unknown";
        }

        return "N/A";
    }

    /**
     * Check if running in native mode
     */
    private boolean isNativeMode() {
        return "Substrate VM".equals(System.getProperty("java.vm.name"));
    }

    /**
     * Get Git commit hash (from environment or build-time property)
     */
    private String getGitCommitHash() {
        String hash = System.getenv("GIT_COMMIT");
        if (hash == null || hash.isEmpty()) {
            hash = System.getProperty("git.commit.id.abbrev", "unknown");
        }
        return hash;
    }

    /**
     * Get system uptime in seconds
     */
    public long getUptimeSeconds() {
        return ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
    }
}
