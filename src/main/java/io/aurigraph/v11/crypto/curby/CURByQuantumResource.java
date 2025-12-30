package io.aurigraph.v11.crypto.curby;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * CURBy Quantum REST API Resource
 *
 * Provides RESTful endpoints for CURBy quantum cryptography operations.
 * Enables external clients to leverage quantum-resistant cryptographic services.
 *
 * Features:
 * - Quantum key pair generation (CRYSTALS-Dilithium, CRYSTALS-Kyber)
 * - Quantum digital signatures
 * - Signature verification
 * - Service health monitoring
 * - Performance metrics
 *
 * Endpoints:
 * - POST /api/v11/curby/keypair - Generate quantum key pair
 * - POST /api/v11/curby/sign - Generate quantum signature
 * - POST /api/v11/curby/verify - Verify quantum signature
 * - GET /api/v11/curby/health - Service health status
 * - GET /api/v11/curby/metrics - Performance metrics
 * - GET /api/v11/curby/algorithms - Supported algorithms
 *
 * @author Aurigraph V11
 * @version 11.0.0
 */
@Path("/api/v11/curby")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "CURBy Quantum Service", description = "Quantum-resistant cryptography operations via CURBy service")
public class CURByQuantumResource {

    private static final Logger LOG = Logger.getLogger(CURByQuantumResource.class);

    @Inject
    CURByQuantumClient curbyClient;

    /**
     * Generate quantum key pair
     *
     * POST /api/v11/curby/keypair
     * Body: { "algorithm": "CRYSTALS-Dilithium", "securityLevel": 5 }
     */
    @POST
    @Path("/keypair")
    @Operation(
        summary = "Generate quantum key pair",
        description = "Generates a post-quantum cryptographic key pair using CURBy quantum service. " +
                     "Supports CRYSTALS-Dilithium (signatures) and CRYSTALS-Kyber (encryption) at NIST Level 5."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Key pair generated successfully",
            content = @Content(schema = @Schema(implementation = CURByQuantumClient.QuantumKeyPairResponse.class))
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid algorithm or parameters"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Service unavailable or internal error"
        ),
        @APIResponse(
            responseCode = "503",
            description = "CURBy service unavailable (circuit breaker open)"
        )
    })
    public Uni<Response> generateKeyPair(
        @Parameter(description = "Key generation request", required = true)
        KeyPairRequest request
    ) {
        LOG.infof("Generating quantum key pair: algorithm=%s", request.algorithm);

        if (request.algorithm == null || request.algorithm.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Algorithm is required"))
                    .build()
            );
        }

        return curbyClient.generateKeyPair(request.algorithm)
            .onItem().transform(result -> {
                if (result.success()) {
                    LOG.infof("Key pair generated: keyId=%s, publicKeySize=%d bytes, privateKeySize=%d bytes",
                        result.keyId(), result.publicKeySize(), result.privateKeySize());
                    return Response.ok(result).build();
                } else {
                    LOG.error("Key pair generation failed");
                    return Response.serverError()
                        .entity(Map.of("error", "Key generation failed"))
                        .build();
                }
            })
            .onFailure().recoverWithItem(error -> {
                LOG.errorf(error, "Key pair generation error: %s", error.getMessage());
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Service unavailable: " + error.getMessage()))
                    .build();
            });
    }

    /**
     * Generate quantum signature
     *
     * POST /api/v11/curby/sign
     * Body: { "data": "Hello World", "privateKey": "...", "algorithm": "CRYSTALS-Dilithium" }
     */
    @POST
    @Path("/sign")
    @Operation(
        summary = "Generate quantum signature",
        description = "Creates a quantum-resistant digital signature for the provided data using CURBy service. " +
                     "Returns a post-quantum signature that is secure against quantum computer attacks."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Signature generated successfully",
            content = @Content(schema = @Schema(implementation = CURByQuantumClient.QuantumSignatureResponse.class))
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid data or private key"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Signature generation failed"
        )
    })
    public Uni<Response> generateSignature(
        @Parameter(description = "Signature generation request", required = true)
        SignatureRequest request
    ) {
        LOG.infof("Generating quantum signature: algorithm=%s, dataLength=%d",
            request.algorithm, request.data != null ? request.data.length() : 0);

        // Validate request
        if (request.data == null || request.data.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Data is required"))
                    .build()
            );
        }

        if (request.privateKey == null || request.privateKey.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Private key is required"))
                    .build()
            );
        }

        return curbyClient.generateSignature(request.data, request.privateKey, request.algorithm)
            .onItem().transform(result -> {
                if (result.success()) {
                    LOG.infof("Signature generated: algorithm=%s, source=%s",
                        result.algorithm(), result.source());
                    return Response.ok(result).build();
                } else {
                    LOG.error("Signature generation failed");
                    return Response.serverError()
                        .entity(Map.of("error", "Signature generation failed"))
                        .build();
                }
            })
            .onFailure().recoverWithItem(error -> {
                LOG.errorf(error, "Signature generation error: %s", error.getMessage());
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Service unavailable: " + error.getMessage()))
                    .build();
            });
    }

    /**
     * Verify quantum signature
     *
     * POST /api/v11/curby/verify
     * Body: { "data": "Hello World", "signature": "...", "publicKey": "...", "algorithm": "CRYSTALS-Dilithium" }
     */
    @POST
    @Path("/verify")
    @Operation(
        summary = "Verify quantum signature",
        description = "Verifies a quantum-resistant digital signature using CURBy service. " +
                     "Returns verification result indicating whether the signature is valid."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Verification completed successfully",
            content = @Content(schema = @Schema(implementation = CURByQuantumClient.QuantumVerificationResponse.class))
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid data, signature, or public key"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Verification failed"
        )
    })
    public Uni<Response> verifySignature(
        @Parameter(description = "Signature verification request", required = true)
        VerificationRequest request
    ) {
        LOG.infof("Verifying quantum signature: algorithm=%s", request.algorithm);

        // Validate request
        if (request.data == null || request.data.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Data is required"))
                    .build()
            );
        }

        if (request.signature == null || request.signature.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Signature is required"))
                    .build()
            );
        }

        if (request.publicKey == null || request.publicKey.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Public key is required"))
                    .build()
            );
        }

        return curbyClient.verifySignature(request.data, request.signature, request.publicKey, request.algorithm)
            .onItem().transform(result -> {
                if (result.success()) {
                    LOG.infof("Signature verification completed: valid=%s, source=%s",
                        result.valid(), result.source());
                    return Response.ok(result).build();
                } else {
                    LOG.error("Signature verification failed");
                    return Response.serverError()
                        .entity(Map.of("error", "Verification failed"))
                        .build();
                }
            })
            .onFailure().recoverWithItem(error -> {
                LOG.errorf(error, "Signature verification error: %s", error.getMessage());
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Service unavailable: " + error.getMessage()))
                    .build();
            });
    }

    /**
     * Get CURBy service health status
     *
     * GET /api/v11/curby/health
     */
    @GET
    @Path("/health")
    @Operation(
        summary = "Get service health status",
        description = "Returns CURBy service health status including circuit breaker state, " +
                     "request statistics, and cache metrics."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Health status retrieved successfully",
            content = @Content(schema = @Schema(implementation = CURByQuantumClient.CURByHealthStatus.class))
        ),
        @APIResponse(
            responseCode = "503",
            description = "Service unhealthy"
        )
    })
    public Response getHealthStatus() {
        LOG.debug("Retrieving CURBy health status");

        CURByQuantumClient.CURByHealthStatus health = curbyClient.getHealthStatus();

        if (health.healthy()) {
            LOG.debugf("CURBy health: HEALTHY (success rate: %.2f%%)", health.successRate() * 100);
            return Response.ok(health).build();
        } else {
            LOG.warnf("CURBy health: UNHEALTHY (success rate: %.2f%%)", health.successRate() * 100);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(health)
                .build();
        }
    }

    /**
     * Get CURBy service metrics
     *
     * GET /api/v11/curby/metrics
     */
    @GET
    @Path("/metrics")
    @Operation(
        summary = "Get service metrics",
        description = "Returns detailed performance metrics including request counts, success/failure rates, " +
                     "cache hit ratio, and response times."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Metrics retrieved successfully"
        )
    })
    public Response getMetrics() {
        LOG.debug("Retrieving CURBy metrics");

        CURByQuantumClient.CURByHealthStatus health = curbyClient.getHealthStatus();

        Map<String, Object> metrics = Map.of(
            "totalRequests", health.totalRequests(),
            "successfulRequests", health.successfulRequests(),
            "failedRequests", health.failedRequests(),
            "successRate", String.format("%.2f%%", health.successRate() * 100),
            "cachedResponses", health.cachedResponses(),
            "cacheSize", health.cacheSize(),
            "circuitBreakerOpen", !health.healthy(),
            "serviceEnabled", health.enabled(),
            "timestamp", health.timestamp()
        );

        LOG.debugf("CURBy metrics: requests=%d, success=%.2f%%, cache=%d",
            Long.valueOf(health.totalRequests()), Double.valueOf(health.successRate() * 100), Integer.valueOf(health.cacheSize()));

        return Response.ok(metrics).build();
    }

    /**
     * Get supported quantum algorithms
     *
     * GET /api/v11/curby/algorithms
     */
    @GET
    @Path("/algorithms")
    @Operation(
        summary = "Get supported algorithms",
        description = "Returns list of supported post-quantum cryptographic algorithms with descriptions."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Algorithms list retrieved successfully"
        )
    })
    public Response getSupportedAlgorithms() {
        LOG.debug("Retrieving supported quantum algorithms");

        Map<String, Object> algorithms = Map.of(
            "signatures", Map.of(
                "CRYSTALS-Dilithium", Map.of(
                    "type", "Digital Signature",
                    "securityLevel", "NIST Level 5",
                    "publicKeySize", "2,592 bytes",
                    "privateKeySize", "4,896 bytes",
                    "signatureSize", "3,309 bytes",
                    "description", "Post-quantum digital signature algorithm based on lattice cryptography"
                ),
                "SPHINCS+", Map.of(
                    "type", "Digital Signature",
                    "securityLevel", "NIST Level 5",
                    "publicKeySize", "64 bytes",
                    "privateKeySize", "128 bytes",
                    "signatureSize", "49,856 bytes",
                    "description", "Stateless hash-based signature scheme (backup algorithm)"
                )
            ),
            "encryption", Map.of(
                "CRYSTALS-Kyber", Map.of(
                    "type", "Key Encapsulation",
                    "securityLevel", "NIST Level 5",
                    "publicKeySize", "1,568 bytes",
                    "privateKeySize", "3,168 bytes",
                    "ciphertextSize", "1,568 bytes",
                    "description", "Post-quantum key encapsulation mechanism based on module lattices"
                )
            ),
            "default", "CRYSTALS-Dilithium",
            "recommendedSecurityLevel", 5,
            "quantumSafe", true
        );

        return Response.ok(algorithms).build();
    }

    /**
     * Batch key generation endpoint
     *
     * POST /api/v11/curby/keypair/batch
     * Body: { "algorithm": "CRYSTALS-Dilithium", "count": 10 }
     */
    @POST
    @Path("/keypair/batch")
    @Operation(
        summary = "Generate multiple quantum key pairs",
        description = "Generates multiple quantum key pairs in a single request for improved efficiency."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Batch key generation completed"
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request or count exceeds limit"
        )
    })
    public Uni<Response> generateKeyPairBatch(
        @Parameter(description = "Batch key generation request", required = true)
        BatchKeyPairRequest request
    ) {
        LOG.infof("Batch generating %d quantum key pairs: algorithm=%s", request.count, request.algorithm);

        // Validate request
        if (request.count < 1 || request.count > 100) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Count must be between 1 and 100"))
                    .build()
            );
        }

        // Generate multiple key pairs
        return Uni.combine().all().unis(
            java.util.stream.IntStream.range(0, request.count)
                .mapToObj(i -> curbyClient.generateKeyPair(request.algorithm))
                .toList()
        ).with(results -> {
            LOG.infof("Batch generation completed: %d key pairs generated", results.size());
            return Response.ok(Map.of(
                "success", true,
                "count", results.size(),
                "keyPairs", results
            )).build();
        })
        .onFailure().recoverWithItem(error -> {
            LOG.errorf(error, "Batch key generation error: %s", error.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Batch generation failed: " + error.getMessage()))
                .build();
        });
    }

    // Request DTOs

    public static class KeyPairRequest {
        public String algorithm = "CRYSTALS-Dilithium";
        public int securityLevel = 5;
    }

    public static class SignatureRequest {
        public String data;
        public String privateKey;
        public String algorithm = "CRYSTALS-Dilithium";
    }

    public static class VerificationRequest {
        public String data;
        public String signature;
        public String publicKey;
        public String algorithm = "CRYSTALS-Dilithium";
    }

    public static class BatchKeyPairRequest {
        public String algorithm = "CRYSTALS-Dilithium";
        public int count = 1;
    }
}
