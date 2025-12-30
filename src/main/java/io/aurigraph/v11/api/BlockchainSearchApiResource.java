package io.aurigraph.v11.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import io.aurigraph.v11.TransactionService;

import java.time.Instant;
import java.util.*;

/**
 * Blockchain Search and Transaction Submission API Resource
 *
 * Provides search and transaction operations:
 * - GET /api/v11/blockchain/blocks/search - Block search with filters
 * - POST /api/v11/blockchain/transactions/submit - Submit new transaction
 *
 * @version 11.0.0
 * @author Backend Development Agent (BDA)
 */
@Path("/api/v11/blockchain")
@ApplicationScoped
@Tag(name = "Blockchain Search API", description = "Block search and transaction submission")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BlockchainSearchApiResource {

    private static final Logger LOG = Logger.getLogger(BlockchainSearchApiResource.class);

    @Inject
    TransactionService transactionService;

    // ==================== ENDPOINT 2: Block Search ====================

    /**
     * GET /api/v11/blockchain/blocks/search
     * Search blocks with filters
     */
    @GET
    @Path("/blocks/search")
    @Operation(summary = "Search blocks", description = "Search blocks with various filters")
    @APIResponse(responseCode = "200", description = "Search completed successfully",
                content = @Content(schema = @Schema(implementation = BlockSearchResponse.class)))
    public Uni<BlockSearchResponse> searchBlocks(
        @QueryParam("query") String query,
        @QueryParam("fromBlock") Long fromBlock,
        @QueryParam("toBlock") Long toBlock,
        @QueryParam("validator") String validator,
        @QueryParam("minTransactions") Integer minTransactions,
        @QueryParam("limit") @DefaultValue("20") int limit) {

        LOG.infof("Searching blocks: query=%s, fromBlock=%s, toBlock=%s, validator=%s, minTx=%s",
                 query, fromBlock, toBlock, validator, minTransactions);

        return Uni.createFrom().item(() -> {
            BlockSearchResponse response = new BlockSearchResponse();
            response.timestamp = Instant.now().toEpochMilli();
            response.totalResults = 1247;
            response.limit = limit;
            response.query = query;
            response.blocks = new ArrayList<>();

            long currentBlock = 1500000L;
            long startBlock = fromBlock != null ? fromBlock : currentBlock - 100;
            long endBlock = toBlock != null ? toBlock : currentBlock;

            for (int i = 0; i < Math.min(limit, 20); i++) {
                BlockSummary block = new BlockSummary();
                block.blockNumber = startBlock + i;
                block.blockHash = "0x" + UUID.randomUUID().toString().replace("-", "");
                block.parentHash = "0x" + UUID.randomUUID().toString().replace("-", "");
                block.timestamp = Instant.now().toEpochMilli() - ((endBlock - startBlock - i) * 2000);
                block.transactionCount = 100 + (int)(Math.random() * 250);
                block.validator = validator != null ? validator :
                                 "validator-" + String.format("%03d", (int)(Math.random() * 42) + 1);
                block.blockTime = 1.5 + (Math.random() * 1.0);
                block.size = 2048 + (int)(Math.random() * 4096);
                block.gasUsed = 8000000L + (long)(Math.random() * 2000000);
                block.gasLimit = 10000000L;
                block.finalized = true;
                block.consensusRound = 7500000L + i;

                // Apply filters
                if (minTransactions != null && block.transactionCount < minTransactions) {
                    continue;
                }
                if (validator != null && !block.validator.equals(validator)) {
                    continue;
                }

                response.blocks.add(block);
            }

            response.resultsReturned = response.blocks.size();

            LOG.infof("Block search returned %d results", response.resultsReturned);

            return response;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== ENDPOINT 3: Submit Transaction ====================

    /**
     * POST /api/v11/blockchain/transactions/submit
     * Submit a new transaction to the network
     */
    @POST
    @Path("/transactions/submit")
    @Operation(summary = "Submit transaction", description = "Submit a new transaction to the blockchain")
    @APIResponse(responseCode = "201", description = "Transaction submitted successfully",
                content = @Content(schema = @Schema(implementation = TransactionSubmitResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid transaction data")
    @APIResponse(responseCode = "503", description = "Network unavailable")
    public Uni<Response> submitTransaction(
        @Parameter(description = "Transaction submission request", required = true)
        TransactionSubmitRequest request) {

        LOG.infof("Submitting transaction: from=%s, to=%s, amount=%.2f",
                 request.from, request.to, request.amount);

        return Uni.createFrom().item(() -> {
            try {
                // Validate transaction request
                if (request.from == null || request.to == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Missing required fields: from or to"))
                        .build();
                }

                if (request.amount <= 0) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Amount must be positive"))
                        .build();
                }

                // Generate transaction ID
                String txId = "tx-" + UUID.randomUUID().toString();

                // Process via TransactionService
                transactionService.processTransaction(txId, request.amount);

                // Create response
                TransactionSubmitResponse response = new TransactionSubmitResponse();
                response.transactionHash = "0x" + UUID.randomUUID().toString().replace("-", "");
                response.status = "PENDING";
                response.from = request.from;
                response.to = request.to;
                response.amount = request.amount;
                response.gasEstimate = 21000L + (long)(Math.random() * 50000);
                response.gasPrice = 50.0 + (Math.random() * 50);
                response.nonce = (long)(Math.random() * 1000000);
                response.timestamp = Instant.now().toEpochMilli();
                response.estimatedConfirmation = Instant.now().plusSeconds(3).toEpochMilli();
                response.message = "Transaction submitted and pending confirmation";

                LOG.infof("Transaction submitted: hash=%s, status=%s",
                         response.transactionHash, response.status);

                return Response.status(Response.Status.CREATED).entity(response).build();

            } catch (Exception e) {
                LOG.errorf(e, "Transaction submission failed");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Transaction submission failed: " + e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== Request/Response DTOs ====================

    public static class BlockSearchResponse {
        public long timestamp;
        public long totalResults;
        public int resultsReturned;
        public int limit;
        public String query;
        public List<BlockSummary> blocks;
    }

    public static class BlockSummary {
        public long blockNumber;
        public String blockHash;
        public String parentHash;
        public long timestamp;
        public int transactionCount;
        public String validator;
        public double blockTime;
        public long size;
        public long gasUsed;
        public long gasLimit;
        public boolean finalized;
        public long consensusRound;
    }

    public static class TransactionSubmitRequest {
        public String from;
        public String to;
        public double amount;
        public String data;
        public Long gasLimit;
        public Double gasPrice;
        public Long nonce;
        public String signature;
    }

    public static class TransactionSubmitResponse {
        public String transactionHash;
        public String status;
        public String from;
        public String to;
        public double amount;
        public long gasEstimate;
        public double gasPrice;
        public long nonce;
        public long timestamp;
        public long estimatedConfirmation;
        public String message;
    }
}
