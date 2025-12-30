package io.aurigraph.v11.api;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import io.aurigraph.v11.tokenization.ExternalAPITokenizationService;
import io.aurigraph.v11.tokenization.models.*;

import java.util.List;
import java.time.Duration;

/**
 * External API Tokenization REST Resource
 *
 * Provides endpoints for:
 * - Managing external API sources
 * - Tokenizing external API data
 * - Monitoring tokenization processes
 * - Channel-based data streaming
 * - LevelDB storage integration
 *
 * @version 11.3.0
 * @author Backend Development Agent (BDA)
 */
@Path("/api/v11/tokenization")
@ApplicationScoped
@Tag(name = "External API Tokenization", description = "Tokenize external API data and stream to channels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExternalAPITokenizationResource {

    private static final Logger LOG = Logger.getLogger(ExternalAPITokenizationResource.class);

    @Inject
    ExternalAPITokenizationService tokenizationService;

    // ==================== API SOURCE MANAGEMENT ====================

    /**
     * Get all API sources
     * GET /api/v11/tokenization/sources
     */
    @GET
    @Path("/sources")
    @Operation(summary = "Get all API sources", description = "Retrieve all configured external API sources")
    @APIResponse(responseCode = "200", description = "Sources retrieved successfully")
    public Uni<APISourcesResponse> getAPISources() {
        LOG.info("Fetching all API sources");
        return tokenizationService.getAllSources()
            .map(sources -> new APISourcesResponse(sources, sources.size(), System.currentTimeMillis()));
    }

    /**
     * Get specific API source
     * GET /api/v11/tokenization/sources/{id}
     */
    @GET
    @Path("/sources/{id}")
    @Operation(summary = "Get API source by ID", description = "Retrieve specific API source details")
    @APIResponse(responseCode = "200", description = "Source found")
    @APIResponse(responseCode = "404", description = "Source not found")
    public Uni<Response> getAPISource(@PathParam("id") String sourceId) {
        LOG.infof("Fetching API source: %s", sourceId);
        return tokenizationService.getSource(sourceId)
            .map(source -> Response.ok(source).build())
            .onFailure().recoverWithItem(
                error -> Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Source not found", error.getMessage()))
                    .build()
            );
    }

    /**
     * Add new API source
     * POST /api/v11/tokenization/sources
     */
    @POST
    @Path("/sources")
    @Operation(summary = "Add API source", description = "Configure a new external API source for tokenization")
    @APIResponse(responseCode = "201", description = "Source created successfully")
    @APIResponse(responseCode = "400", description = "Invalid source configuration")
    public Uni<Response> addAPISource(AddAPISourceRequest request) {
        LOG.infof("Adding new API source: %s", request.name);

        return tokenizationService.addSource(
            request.name,
            request.url,
            request.method,
            request.headers,
            request.channel,
            request.pollInterval
        ).map(source -> Response.status(Response.Status.CREATED)
            .entity(new APISourceResponse(source, "Source created successfully", System.currentTimeMillis()))
            .build()
        ).onFailure().recoverWithItem(
            error -> Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Failed to create source", error.getMessage()))
                .build()
        );
    }

    /**
     * Update API source status
     * PUT /api/v11/tokenization/sources/{id}/status
     */
    @PUT
    @Path("/sources/{id}/status")
    @Operation(summary = "Update source status", description = "Activate or pause API source")
    @APIResponse(responseCode = "200", description = "Status updated")
    public Uni<Response> updateSourceStatus(
        @PathParam("id") String sourceId,
        UpdateStatusRequest request
    ) {
        LOG.infof("Updating source %s status to: %s", sourceId, request.status);

        return tokenizationService.updateSourceStatus(sourceId, request.status)
            .map(source -> Response.ok(new APISourceResponse(source, "Status updated", System.currentTimeMillis())).build())
            .onFailure().recoverWithItem(
                error -> Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Failed to update status", error.getMessage()))
                    .build()
            );
    }

    /**
     * Delete API source
     * DELETE /api/v11/tokenization/sources/{id}
     */
    @DELETE
    @Path("/sources/{id}")
    @Operation(summary = "Delete API source", description = "Remove API source configuration")
    @APIResponse(responseCode = "200", description = "Source deleted")
    public Uni<Response> deleteAPISource(@PathParam("id") String sourceId) {
        LOG.infof("Deleting API source: %s", sourceId);

        return tokenizationService.deleteSource(sourceId)
            .map(deleted -> Response.ok(new DeleteResponse(deleted, "Source deleted successfully", System.currentTimeMillis())).build())
            .onFailure().recoverWithItem(
                error -> Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Failed to delete source", error.getMessage()))
                    .build()
            );
    }

    // ==================== TOKENIZED TRANSACTIONS ====================

    /**
     * Get tokenized transactions
     * GET /api/v11/tokenization/transactions
     */
    @GET
    @Path("/transactions")
    @Operation(summary = "Get tokenized transactions", description = "Retrieve recent tokenized transactions")
    @APIResponse(responseCode = "200", description = "Transactions retrieved")
    public Uni<TokenizedTransactionsResponse> getTokenizedTransactions(
        @QueryParam("limit") @DefaultValue("50") int limit,
        @QueryParam("channel") String channel
    ) {
        LOG.infof("Fetching tokenized transactions (limit: %d, channel: %s)", limit, channel);

        return tokenizationService.getTokenizedTransactions(limit, channel)
            .map(transactions -> new TokenizedTransactionsResponse(
                transactions,
                transactions.size(),
                limit,
                channel,
                System.currentTimeMillis()
            ));
    }

    /**
     * Get transaction by ID
     * GET /api/v11/tokenization/transactions/{id}
     */
    @GET
    @Path("/transactions/{id}")
    @Operation(summary = "Get transaction details", description = "Retrieve specific tokenized transaction")
    @APIResponse(responseCode = "200", description = "Transaction found")
    @APIResponse(responseCode = "404", description = "Transaction not found")
    public Uni<Response> getTransaction(@PathParam("id") String txId) {
        LOG.infof("Fetching transaction: %s", txId);

        return tokenizationService.getTransaction(txId)
            .map(tx -> Response.ok(tx).build())
            .onFailure().recoverWithItem(
                error -> Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Transaction not found", error.getMessage()))
                    .build()
            );
    }

    /**
     * Stream real-time tokenization events
     * GET /api/v11/tokenization/stream
     */
    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(summary = "Stream tokenization events", description = "Real-time stream of tokenization events")
    public Multi<TokenizationEvent> streamTokenizationEvents(
        @QueryParam("channel") String channel
    ) {
        LOG.infof("Starting tokenization event stream (channel: %s)", channel);

        return tokenizationService.streamTokenizationEvents(channel)
            .onItem().invoke(event -> LOG.debugf("Streaming event: %s", event.eventType));
    }

    // ==================== CHANNEL STATISTICS ====================

    /**
     * Get channel statistics
     * GET /api/v11/tokenization/channels/stats
     */
    @GET
    @Path("/channels/stats")
    @Operation(summary = "Get channel statistics", description = "Retrieve statistics for all channels")
    @APIResponse(responseCode = "200", description = "Statistics retrieved")
    public Uni<ChannelStatsResponse> getChannelStats() {
        LOG.info("Fetching channel statistics");

        return tokenizationService.getChannelStats()
            .map(channels -> new ChannelStatsResponse(channels, channels.size(), System.currentTimeMillis()));
    }

    /**
     * Get specific channel statistics
     * GET /api/v11/tokenization/channels/{channelId}/stats
     */
    @GET
    @Path("/channels/{channelId}/stats")
    @Operation(summary = "Get channel stats by ID", description = "Retrieve statistics for specific channel")
    @APIResponse(responseCode = "200", description = "Stats found")
    @APIResponse(responseCode = "404", description = "Channel not found")
    public Uni<Response> getChannelStats(@PathParam("channelId") String channelId) {
        LOG.infof("Fetching stats for channel: %s", channelId);

        return tokenizationService.getChannelStat(channelId)
            .map(stats -> Response.ok(stats).build())
            .onFailure().recoverWithItem(
                error -> Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Channel not found", error.getMessage()))
                    .build()
            );
    }

    // ==================== MANUAL TOKENIZATION ====================

    /**
     * Manually trigger tokenization for a source
     * POST /api/v11/tokenization/sources/{id}/tokenize
     */
    @POST
    @Path("/sources/{id}/tokenize")
    @Operation(summary = "Trigger tokenization", description = "Manually trigger data fetch and tokenization")
    @APIResponse(responseCode = "200", description = "Tokenization triggered")
    public Uni<Response> triggerTokenization(@PathParam("id") String sourceId) {
        LOG.infof("Manual tokenization triggered for source: %s", sourceId);

        return tokenizationService.manualTokenize(sourceId)
            .map(result -> Response.ok(new TokenizationResultResponse(
                result.success,
                result.transactionId,
                result.dataHash,
                result.size,
                result.channel,
                result.leveldbPath,
                result.message,
                System.currentTimeMillis()
            )).build())
            .onFailure().recoverWithItem(
                error -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Tokenization failed", error.getMessage()))
                    .build()
            );
    }

    // ==================== LEVELDB STORAGE ====================

    /**
     * Get LevelDB storage info
     * GET /api/v11/tokenization/storage/info
     */
    @GET
    @Path("/storage/info")
    @Operation(summary = "Get storage info", description = "Retrieve LevelDB storage information")
    @APIResponse(responseCode = "200", description = "Storage info retrieved")
    public Uni<StorageInfoResponse> getStorageInfo() {
        LOG.info("Fetching LevelDB storage information");

        return tokenizationService.getStorageInfo()
            .map(info -> new StorageInfoResponse(
                info.basePath,
                info.totalSize,
                info.slimNodeCount,
                info.channelCount,
                info.compressionEnabled,
                info.encryptionEnabled,
                System.currentTimeMillis()
            ));
    }

    // ==================== DATA MODELS ====================

    public record APISourcesResponse(
        List<APISource> sources,
        int count,
        long timestamp
    ) {}

    public record APISourceResponse(
        APISource source,
        String message,
        long timestamp
    ) {}

    public record TokenizedTransactionsResponse(
        List<TokenizedTransaction> transactions,
        int count,
        int limit,
        String channel,
        long timestamp
    ) {}

    public record ChannelStatsResponse(
        List<ChannelStats> channels,
        int count,
        long timestamp
    ) {}

    public record TokenizationResultResponse(
        boolean success,
        String transactionId,
        String dataHash,
        long size,
        String channel,
        String leveldbPath,
        String message,
        long timestamp
    ) {}

    public record StorageInfoResponse(
        String basePath,
        long totalSize,
        int slimNodeCount,
        int channelCount,
        boolean compressionEnabled,
        boolean encryptionEnabled,
        long timestamp
    ) {}

    public record ErrorResponse(
        String error,
        String message
    ) {}

    public record DeleteResponse(
        boolean deleted,
        String message,
        long timestamp
    ) {}

    // Request DTOs
    public static class AddAPISourceRequest {
        public String name;
        public String url;
        public String method;
        public java.util.Map<String, String> headers;
        public String channel;
        public int pollInterval;
    }

    public static class UpdateStatusRequest {
        public String status; // "active", "paused", "error"
    }
}
