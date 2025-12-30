package io.aurigraph.v11.api;

import io.aurigraph.v11.models.QuantumCryptoStatus;
import io.aurigraph.v11.services.QuantumCryptoService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

/**
 * Quantum Cryptography Status API
 * Provides post-quantum cryptography status and metrics
 *
 * AV11-286: Implement Quantum Cryptography Status API
 *
 * Endpoints:
 * - GET /api/v11/security/quantum - Get quantum cryptography status
 * - GET /api/v11/security/quantum/algorithms - Get supported algorithms
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@Path("/api/v11/security/quantum")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Quantum Cryptography", description = "Post-quantum cryptography status and metrics")
public class QuantumCryptoResource {

    private static final Logger LOG = Logger.getLogger(QuantumCryptoResource.class);

    @Inject
    QuantumCryptoService quantumCryptoService;

    /**
     * Get quantum cryptography status
     *
     * Returns comprehensive post-quantum cryptography information including:
     * - Supported algorithms (CRYSTALS-Kyber, Dilithium)
     * - Key generation statistics
     * - Digital signature performance
     * - Real-time performance metrics
     * - NIST security level information
     *
     * @return QuantumCryptoStatus with all quantum crypto details
     */
    @GET
    @Operation(summary = "Get quantum cryptography status",
               description = "Returns comprehensive post-quantum cryptography status including algorithms, statistics, and performance metrics")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Quantum crypto status retrieved successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                          schema = @Schema(implementation = QuantumCryptoStatus.class))),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getQuantumCryptoStatus() {
        LOG.info("GET /api/v11/security/quantum - Fetching quantum cryptography status");

        return quantumCryptoService.getQuantumCryptoStatus()
                .map(status -> {
                    LOG.debugf("Quantum crypto status retrieved: %s, security level: NIST-%d",
                            status.getStatus(),
                            status.getSecurityLevel().getNistLevel());

                    return Response.ok(status).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving quantum crypto status", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve quantum cryptography status",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get supported quantum algorithms
     *
     * Lightweight endpoint that returns only algorithm information
     *
     * @return Supported quantum-resistant algorithms
     */
    @GET
    @Path("/algorithms")
    @Operation(summary = "Get supported quantum algorithms",
               description = "Returns list of supported post-quantum cryptographic algorithms")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Algorithm information retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getSupportedAlgorithms() {
        LOG.debug("GET /api/v11/security/quantum/algorithms - Fetching supported algorithms");

        return quantumCryptoService.getQuantumCryptoStatus()
                .map(status -> {
                    Map<String, Object> algorithms = Map.of(
                            "key_encapsulation", Map.of(
                                    "name", status.getAlgorithms().getKeyEncapsulation().getName(),
                                    "variant", status.getAlgorithms().getKeyEncapsulation().getVariant(),
                                    "nist_level", status.getAlgorithms().getKeyEncapsulation().getNistLevel(),
                                    "enabled", status.getAlgorithms().getKeyEncapsulation().isEnabled()
                            ),
                            "digital_signature", Map.of(
                                    "name", status.getAlgorithms().getDigitalSignature().getName(),
                                    "variant", status.getAlgorithms().getDigitalSignature().getVariant(),
                                    "nist_level", status.getAlgorithms().getDigitalSignature().getNistLevel(),
                                    "enabled", status.getAlgorithms().getDigitalSignature().isEnabled()
                            ),
                            "supported_list", status.getAlgorithms().getSupportedAlgorithms()
                    );

                    return Response.ok(algorithms).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving supported algorithms", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve supported algorithms",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get security level information
     *
     * Returns NIST security level and compliance information
     *
     * @return Security level details
     */
    @GET
    @Path("/security-level")
    @Operation(summary = "Get security level information",
               description = "Returns NIST security level and compliance standards")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Security level information retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getSecurityLevel() {
        LOG.debug("GET /api/v11/security/quantum/security-level - Fetching security level");

        return quantumCryptoService.getQuantumCryptoStatus()
                .map(status -> {
                    Map<String, Object> securityInfo = Map.of(
                            "nist_level", status.getSecurityLevel().getNistLevel(),
                            "quantum_resistance", status.getSecurityLevel().getQuantumResistance(),
                            "classical_equivalent_bits", status.getSecurityLevel().getClassicalEquivalentBits(),
                            "compliance", status.getSecurityLevel().getCompliance(),
                            "certification_status", status.getSecurityLevel().getCertificationStatus()
                    );

                    return Response.ok(securityInfo).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving security level", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve security level information",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get performance metrics
     *
     * Returns real-time performance metrics for quantum crypto operations
     *
     * @return Performance metrics
     */
    @GET
    @Path("/performance")
    @Operation(summary = "Get performance metrics",
               description = "Returns real-time performance metrics for quantum cryptography operations")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Performance metrics retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getPerformanceMetrics() {
        LOG.debug("GET /api/v11/security/quantum/performance - Fetching performance metrics");

        return quantumCryptoService.getQuantumCryptoStatus()
                .map(status -> {
                    Map<String, Object> performance = Map.of(
                            "throughput_ops_per_second", status.getPerformance().getThroughputOpsPerSecond(),
                            "cpu_usage_percent", status.getPerformance().getCpuUsagePercent(),
                            "memory_usage_mb", status.getPerformance().getMemoryUsageMb(),
                            "latency_p50_ms", status.getPerformance().getLatencyP50Ms(),
                            "latency_p95_ms", status.getPerformance().getLatencyP95Ms(),
                            "latency_p99_ms", status.getPerformance().getLatencyP99Ms(),
                            "uptime_seconds", quantumCryptoService.getUptimeSeconds()
                    );

                    return Response.ok(performance).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving performance metrics", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve performance metrics",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }
}
