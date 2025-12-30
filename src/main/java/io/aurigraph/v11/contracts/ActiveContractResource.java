package io.aurigraph.v11.contracts;

import io.aurigraph.v11.contracts.models.*;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aurigraph ActiveContracts REST API
 *
 * Unified REST interface for the ActiveContracts platform providing:
 * - Contract deployment and lifecycle management
 * - Smart contract execution
 * - Multi-party signature management
 * - RWA tokenization
 * - State queries and updates
 *
 * @version 11.3.0
 * @since 2025-10-13
 */
@Path("/api/v11/activecontracts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActiveContractResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveContractResource.class);

    @Inject
    ActiveContractService contractService;

    /**
     * Deploy a new ActiveContract
     *
     * POST /api/v11/activecontracts/deploy
     *
     * @param contract Contract to deploy
     * @return Deployed contract with ID
     */
    @POST
    @Path("/deploy")
    public Uni<Response> deployContract(ActiveContract contract) {
        LOGGER.info("REST: Deploy contract request - {}", contract.getName());

        return contractService.deployContract(contract)
            .map(deployed -> Response.ok(deployed).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Deploy failed: {}", error.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Activate a deployed contract (after all signatures collected)
     *
     * POST /api/v11/activecontracts/{id}/activate
     *
     * @param contractId Contract ID
     * @return Activated contract
     */
    @POST
    @Path("/{contractId}/activate")
    public Uni<Response> activateContract(@PathParam("contractId") String contractId) {
        LOGGER.info("REST: Activate contract - {}", contractId);

        return contractService.activateContract(contractId)
            .map(activated -> Response.ok(activated).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Activation failed: {}", error.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Execute a contract method
     *
     * POST /api/v11/activecontracts/{id}/execute
     *
     * Request body:
     * {
     *   "method": "transfer",
     *   "parameters": {"to": "address", "amount": 100},
     *   "caller": "caller_address"
     * }
     *
     * @param contractId Contract ID
     * @param request Execution request
     * @return Execution result
     */
    @POST
    @Path("/{contractId}/execute")
    public Uni<Response> executeContract(
            @PathParam("contractId") String contractId,
            ExecutionRequest request
    ) {
        LOGGER.info("REST: Execute contract - {} method: {}", contractId, request.getMethod());

        // Convert Object[] parameters to Map<String, Object> if needed
        Map<String, Object> parametersMap = new HashMap<>();
        if (request.getExecutionParameters() != null) {
            parametersMap = request.getExecutionParameters();
        } else if (request.getParameters() != null) {
            // Convert Object[] to Map with indexed keys
            Object[] params = request.getParameters();
            for (int i = 0; i < params.length; i++) {
                parametersMap.put("param" + i, params[i]);
            }
        }

        return contractService.executeContract(
                contractId,
                request.getMethod(),
                parametersMap,
                request.getCaller()
            )
            .map(execution -> Response.ok(execution).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Execution failed: {}", error.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Sign a contract (multi-party contracts)
     *
     * POST /api/v11/activecontracts/{id}/sign
     *
     * @param contractId Contract ID
     * @param signature Contract signature
     * @return Updated contract
     */
    @POST
    @Path("/{contractId}/sign")
    public Uni<Response> signContract(
            @PathParam("contractId") String contractId,
            ContractSignature signature
    ) {
        LOGGER.info("REST: Sign contract - {}", contractId);

        return contractService.signContract(contractId, signature)
            .map(contract -> Response.ok(contract).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Signing failed: {}", error.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Get contract by ID
     *
     * GET /api/v11/activecontracts/{id}
     *
     * @param contractId Contract ID
     * @return Contract details
     */
    @GET
    @Path("/{contractId}")
    public Uni<Response> getContract(@PathParam("contractId") String contractId) {
        LOGGER.info("REST: Get contract - {}", contractId);

        return contractService.getContract(contractId)
            .map(contract -> Response.ok(contract).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Get contract failed: {}", error.getMessage());
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * List contracts
     *
     * GET /api/v11/activecontracts?owner=xxx&type=yyy
     *
     * @param owner Filter by owner (optional)
     * @param type Filter by contract type (optional)
     * @return List of contracts
     */
    @GET
    public Uni<Response> listContracts(
            @QueryParam("owner") String owner,
            @QueryParam("type") String type
    ) {
        LOGGER.info("REST: List contracts - owner: {}, type: {}", owner, type);

        Uni<List<ActiveContract>> contractsUni;

        if (owner != null && !owner.isEmpty()) {
            contractsUni = contractService.listContractsByOwner(owner);
        } else if (type != null && !type.isEmpty()) {
            contractsUni = contractService.listContractsByType(type);
        } else {
            contractsUni = contractService.listContracts();
        }

        return contractsUni
            .map(contracts -> Response.ok(contracts).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("List contracts failed: {}", error.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Get execution history for a contract
     *
     * GET /api/v11/activecontracts/{id}/executions
     *
     * @param contractId Contract ID
     * @return List of executions
     */
    @GET
    @Path("/{contractId}/executions")
    public Uni<Response> getExecutionHistory(@PathParam("contractId") String contractId) {
        LOGGER.info("REST: Get execution history - {}", contractId);

        return contractService.getExecutionHistory(contractId)
            .map(executions -> Response.ok(executions).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Get executions failed: {}", error.getMessage());
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Get contract signatures
     *
     * GET /api/v11/activecontracts/{id}/signatures
     *
     * @param contractId Contract ID
     * @return List of signatures
     */
    @GET
    @Path("/{contractId}/signatures")
    public Uni<Response> getSignatures(@PathParam("contractId") String contractId) {
        LOGGER.info("REST: Get signatures - {}", contractId);

        return contractService.getContract(contractId)
            .map(contract -> Response.ok(contract.getSignatures()).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Get signatures failed: {}", error.getMessage());
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Check if contract is fully signed
     *
     * GET /api/v11/activecontracts/{id}/fully-signed
     *
     * @param contractId Contract ID
     * @return {fullySigned: boolean}
     */
    @GET
    @Path("/{contractId}/fully-signed")
    public Uni<Response> isFullySigned(@PathParam("contractId") String contractId) {
        LOGGER.info("REST: Check fully signed - {}", contractId);

        return contractService.isFullySigned(contractId)
            .map(fullySigned -> Response.ok(Map.of("fullySigned", fullySigned)).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Check fully signed failed: {}", error.getMessage());
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Get contract state
     *
     * GET /api/v11/activecontracts/{id}/state
     *
     * @param contractId Contract ID
     * @return Contract state
     */
    @GET
    @Path("/{contractId}/state")
    public Uni<Response> getContractState(@PathParam("contractId") String contractId) {
        LOGGER.info("REST: Get contract state - {}", contractId);

        return contractService.getContractState(contractId)
            .map(state -> Response.ok(state).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Get state failed: {}", error.getMessage());
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Update contract state
     *
     * PUT /api/v11/activecontracts/{id}/state
     *
     * @param contractId Contract ID
     * @param newState New state values
     * @return Updated contract
     */
    @PUT
    @Path("/{contractId}/state")
    public Uni<Response> updateContractState(
            @PathParam("contractId") String contractId,
            Map<String, Object> newState
    ) {
        LOGGER.info("REST: Update contract state - {}", contractId);

        return contractService.updateContractState(contractId, newState)
            .map(contract -> Response.ok(contract).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Update state failed: {}", error.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Pause a contract
     *
     * POST /api/v11/activecontracts/{id}/pause
     *
     * @param contractId Contract ID
     * @return Paused contract
     */
    @POST
    @Path("/{contractId}/pause")
    public Uni<Response> pauseContract(@PathParam("contractId") String contractId) {
        LOGGER.info("REST: Pause contract - {}", contractId);

        return contractService.pauseContract(contractId)
            .map(contract -> Response.ok(contract).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Pause failed: {}", error.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Resume a paused contract
     *
     * POST /api/v11/activecontracts/{id}/resume
     *
     * @param contractId Contract ID
     * @return Resumed contract
     */
    @POST
    @Path("/{contractId}/resume")
    public Uni<Response> resumeContract(@PathParam("contractId") String contractId) {
        LOGGER.info("REST: Resume contract - {}", contractId);

        return contractService.resumeContract(contractId)
            .map(contract -> Response.ok(contract).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Resume failed: {}", error.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Tokenize an asset (RWA feature)
     *
     * POST /api/v11/activecontracts/{id}/tokenize
     *
     * @param contractId Contract ID
     * @param request Tokenization request
     * @return Tokenized contract
     */
    @POST
    @Path("/{contractId}/tokenize")
    public Uni<Response> tokenizeAsset(
            @PathParam("contractId") String contractId,
            AssetTokenizationRequest request
    ) {
        LOGGER.info("REST: Tokenize asset - {}", contractId);

        return contractService.tokenizeAsset(contractId, request)
            .map(contract -> Response.ok(contract).build())
            .onFailure().recoverWithItem(error -> {
                LOGGER.error("Tokenization failed: {}", error.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Get platform metrics
     *
     * GET /api/v11/activecontracts/metrics
     *
     * @return Platform metrics
     */
    @GET
    @Path("/metrics")
    public Response getMetrics() {
        LOGGER.info("REST: Get platform metrics");

        Map<String, Long> metrics = contractService.getMetrics();
        return Response.ok(metrics).build();
    }

    /**
     * Get SDK/API information
     *
     * GET /api/v11/activecontracts/sdk/info
     *
     * @return SDK information
     */
    @GET
    @Path("/sdk/info")
    public Response getSdkInfo() {
        LOGGER.info("REST: Get SDK info");

        Map<String, Object> sdkInfo = new HashMap<>();
        sdkInfo.put("name", "Aurigraph ActiveContracts Platform");
        sdkInfo.put("version", "12.0.0");
        sdkInfo.put("description", "Unified smart contract platform combining legal contracts, multi-language SDK, and RWA tokenization");
        sdkInfo.put("features", List.of(
            "Multi-language smart contracts (Solidity, Java, JavaScript, WASM, Python, Custom)",
            "Legal contracts with quantum-safe signatures",
            "RWA tokenization (Carbon Credits, Real Estate, Financial Assets)",
            "Multi-party contract execution",
            "Gas metering and execution tracking",
            "State management and queries"
        ));
        sdkInfo.put("supportedLanguages", List.of("SOLIDITY", "JAVA", "JAVASCRIPT", "WASM", "PYTHON", "CUSTOM"));
        sdkInfo.put("endpoints", List.of(
            "POST /api/v11/activecontracts/deploy - Deploy contract",
            "POST /api/v11/activecontracts/{id}/execute - Execute method",
            "POST /api/v11/activecontracts/{id}/sign - Sign contract",
            "GET /api/v11/activecontracts/{id} - Get contract",
            "GET /api/v11/activecontracts - List contracts",
            "GET /api/v11/activecontracts/{id}/executions - Execution history",
            "POST /api/v11/activecontracts/{id}/tokenize - Tokenize asset"
        ));

        return Response.ok(sdkInfo).build();
    }

    /**
     * Health check endpoint
     *
     * GET /api/v11/activecontracts/health
     *
     * @return Health status
     */
    @GET
    @Path("/health")
    public Response healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("platform", "Aurigraph ActiveContracts");
        health.put("version", "12.0.0");
        health.put("timestamp", java.time.Instant.now().toString());

        Map<String, Long> metrics = contractService.getMetrics();
        health.put("metrics", metrics);

        return Response.ok(health).build();
    }
}
