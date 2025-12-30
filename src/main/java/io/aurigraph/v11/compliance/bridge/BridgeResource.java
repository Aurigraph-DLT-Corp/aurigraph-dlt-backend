package io.aurigraph.v11.compliance.bridge;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;
import java.math.BigDecimal;
import java.util.Map;

/**
 * REST API endpoints for ERC-3643 Smart Contract Bridge
 * Allows Solidity contracts to interact with Java compliance engine
 */
@Path("/api/v11/compliance/bridge")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BridgeResource {

    @Inject
    ERC3643ContractBridge bridge;

    /**
     * Register a Solidity contract
     * POST /api/v11/compliance/bridge/contracts/register
     */
    @POST
    @Path("/contracts/register")
    public Uni<Response> registerContract(Map<String, String> request) {
        return Uni.createFrom().deferred(() -> {
            String contractAddress = request.get("contractAddress");
            String tokenId = request.get("tokenId");

            Log.infof("Registering contract %s for token %s", contractAddress, tokenId);

            bridge.registerContract(contractAddress, tokenId);

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "message", "Contract registered",
                    "contractAddress", contractAddress,
                    "tokenId", tokenId
                )).build()
            );
        });
    }

    /**
     * Process transfer approval request
     * POST /api/v11/compliance/bridge/transfers/approve
     */
    @POST
    @Path("/transfers/approve")
    public Uni<Response> processTransferApproval(Map<String, Object> request) {
        return Uni.createFrom().deferred(() -> {
            String contractAddress = (String) request.get("contractAddress");
            String from = (String) request.get("from");
            String to = (String) request.get("to");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            Log.infof("Processing transfer approval for contract %s", contractAddress);

            ERC3643ContractBridge.ContractResponse response = bridge.processTransferApproval(
                contractAddress, from, to, amount
            );

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", response.isSuccess(),
                    "message", response.getMessage(),
                    "data", response.getData() != null ? response.getData() : Map.of()
                )).build()
            );
        });
    }

    /**
     * Sync identity verification from contract
     * POST /api/v11/compliance/bridge/identities/sync
     */
    @POST
    @Path("/identities/sync")
    public Uni<Response> syncIdentity(Map<String, Object> request) {
        return Uni.createFrom().deferred(() -> {
            String contractAddress = (String) request.get("contractAddress");
            String address = (String) request.get("address");
            @SuppressWarnings("unchecked")
            Map<String, String> identityData = (Map<String, String>) request.get("identityData");

            Log.infof("Syncing identity from contract %s for address %s", contractAddress, address);

            ERC3643ContractBridge.ContractResponse response = bridge.syncIdentityVerification(
                contractAddress, address, identityData
            );

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", response.isSuccess(),
                    "message", response.getMessage(),
                    "data", response.getData() != null ? response.getData() : Map.of()
                )).build()
            );
        });
    }

    /**
     * Sync identity revocation from contract
     * POST /api/v11/compliance/bridge/identities/revoke
     */
    @POST
    @Path("/identities/revoke")
    public Uni<Response> revokeIdentity(Map<String, String> request) {
        return Uni.createFrom().deferred(() -> {
            String contractAddress = request.get("contractAddress");
            String address = request.get("address");
            String reason = request.getOrDefault("reason", "Contract revocation");

            Log.infof("Revoking identity via bridge for address %s", address);

            ERC3643ContractBridge.ContractResponse response = bridge.syncIdentityRevocation(
                contractAddress, address, reason
            );

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", response.isSuccess(),
                    "message", response.getMessage(),
                    "data", response.getData() != null ? response.getData() : Map.of()
                )).build()
            );
        });
    }

    /**
     * Sync country restriction from contract
     * POST /api/v11/compliance/bridge/countries/sync
     */
    @POST
    @Path("/countries/sync")
    public Uni<Response> syncCountryRestriction(Map<String, Object> request) {
        return Uni.createFrom().deferred(() -> {
            String contractAddress = (String) request.get("contractAddress");
            String countryCode = (String) request.get("countryCode");
            boolean restrict = (boolean) request.get("restrict");

            Log.infof("Syncing country %s restriction via bridge", countryCode);

            ERC3643ContractBridge.ContractResponse response = bridge.syncCountryRestriction(
                contractAddress, countryCode, restrict
            );

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", response.isSuccess(),
                    "message", response.getMessage(),
                    "data", response.getData() != null ? response.getData() : Map.of()
                )).build()
            );
        });
    }

    /**
     * Get contract state
     * GET /api/v11/compliance/bridge/contracts/{contractAddress}/state
     */
    @GET
    @Path("/contracts/{contractAddress}/state")
    public Uni<Response> getContractState(@PathParam("contractAddress") String contractAddress) {
        return Uni.createFrom().deferred(() -> {
            Log.infof("Getting contract state for %s", contractAddress);

            Map<String, Object> state = bridge.getContractState(contractAddress);

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", !state.containsKey("error"),
                    "state", state
                )).build()
            );
        });
    }

    /**
     * Create pending operation
     * POST /api/v11/compliance/bridge/operations/create
     */
    @POST
    @Path("/operations/create")
    public Uni<Response> createPendingOperation(Map<String, Object> request) {
        return Uni.createFrom().deferred(() -> {
            String contractAddress = (String) request.get("contractAddress");
            String operationType = (String) request.get("operationType");
            @SuppressWarnings("unchecked")
            Map<String, Object> operationData = (Map<String, Object>) request.get("operationData");

            String operationId = bridge.createPendingOperation(contractAddress, operationType, operationData);

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "operationId", operationId,
                    "contractAddress", contractAddress,
                    "operationType", operationType
                )).build()
            );
        });
    }

    /**
     * Get operation status
     * GET /api/v11/compliance/bridge/operations/{operationId}
     */
    @GET
    @Path("/operations/{operationId}")
    public Uni<Response> getOperationStatus(@PathParam("operationId") String operationId) {
        return Uni.createFrom().deferred(() -> {
            Log.infof("Getting status for operation %s", operationId);

            ERC3643ContractBridge.ContractOperation operation = bridge.getPendingOperation(operationId);

            if (operation == null) {
                return Uni.createFrom().item(
                    Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Operation not found"))
                        .build()
                );
            }

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "operationId", operation.getOperationId(),
                    "contractAddress", operation.getContractAddress(),
                    "operationType", operation.getOperationType(),
                    "status", operation.getStatus().toString(),
                    "result", operation.getResult(),
                    "createdAt", operation.getCreatedAt().toString(),
                    "completedAt", operation.getCompletedAt() != null ? operation.getCompletedAt().toString() : null
                )).build()
            );
        });
    }

    /**
     * Get bridge statistics
     * GET /api/v11/compliance/bridge/stats
     */
    @GET
    @Path("/stats")
    public Uni<Response> getBridgeStats() {
        return Uni.createFrom().deferred(() -> {
            Log.info("Getting bridge statistics");

            ERC3643ContractBridge.BridgeStats stats = bridge.getStats();

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "registeredContracts", stats.getRegisteredContracts(),
                    "pendingOperations", stats.getPendingOperations(),
                    "contractCallsReceived", stats.getContractCallsReceived(),
                    "contractCallsProcessed", stats.getContractCallsProcessed(),
                    "syncErrors", stats.getSyncErrors()
                )).build()
            );
        });
    }
}
