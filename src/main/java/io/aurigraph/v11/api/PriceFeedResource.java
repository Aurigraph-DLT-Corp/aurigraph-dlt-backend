package io.aurigraph.v11.api;

import io.aurigraph.v11.models.PriceFeed;
import io.aurigraph.v11.services.PriceFeedService;
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
 * Price Feed API
 * Provides real-time price aggregation from multiple oracle sources
 *
 * AV11-284: Implement Price Feed Display API
 *
 * Endpoints:
 * - GET /api/v11/datafeeds/prices - Get all asset prices
 * - GET /api/v11/datafeeds/prices/{symbol} - Get specific asset price
 * - GET /api/v11/datafeeds/sources - Get price source status
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@Path("/api/v11/datafeeds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Price Feeds", description = "Real-time price aggregation from multiple oracle sources")
public class PriceFeedResource {

    private static final Logger LOG = Logger.getLogger(PriceFeedResource.class);

    @Inject
    PriceFeedService priceFeedService;

    /**
     * Get all asset prices
     *
     * Returns real-time prices for all supported assets from multiple oracle sources
     */
    @GET
    @Path("/prices")
    @Operation(summary = "Get all asset prices",
               description = "Returns real-time prices for all supported assets aggregated from multiple oracle sources")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Price feed retrieved successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                          schema = @Schema(implementation = PriceFeed.class))),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getAllPrices() {
        LOG.info("GET /api/v11/datafeeds/prices - Fetching all asset prices");

        return priceFeedService.getPriceFeed()
                .map(feed -> {
                    LOG.debugf("Price feed retrieved: %d assets from %d sources",
                            feed.getPrices().size(),
                            feed.getSources().size());

                    return Response.ok(feed).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving price feed", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve price feed",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get specific asset price
     *
     * Returns real-time price for a specific asset
     */
    @GET
    @Path("/prices/{symbol}")
    @Operation(summary = "Get specific asset price",
               description = "Returns real-time price for a specific asset (e.g., BTC, ETH, MATIC)")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Asset price retrieved successfully"),
            @APIResponse(responseCode = "404",
                         description = "Asset not found"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getAssetPrice(@PathParam("symbol") String symbol) {
        LOG.debugf("GET /api/v11/datafeeds/prices/%s - Fetching asset price", symbol);

        return priceFeedService.getAssetPrice(symbol)
                .map(price -> {
                    LOG.debugf("Asset price retrieved: %s = $%.2f",
                            price.getAssetSymbol(),
                            price.getPriceUsd());

                    return Response.ok(price).build();
                })
                .onFailure(IllegalArgumentException.class).recoverWithItem(throwable -> {
                    LOG.warnf("Asset not found: %s", symbol);
                    return (Response) Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of(
                                    "error", "Asset not found",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving asset price", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve asset price",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get price sources status
     *
     * Returns status of all price oracle sources
     */
    @GET
    @Path("/sources")
    @Operation(summary = "Get price sources",
               description = "Returns status and reliability of all price oracle sources")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Price sources retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getPriceSources() {
        LOG.debug("GET /api/v11/datafeeds/sources - Fetching price sources");

        return priceFeedService.getPriceFeed()
                .map(feed -> {
                    Map<String, Object> sourcesInfo = Map.of(
                            "sources", feed.getSources(),
                            "total_sources", feed.getSources().size(),
                            "aggregation_method", feed.getAggregationMethod(),
                            "update_frequency_ms", feed.getUpdateFrequencyMs()
                    );

                    return Response.ok(sourcesInfo).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving price sources", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve price sources",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }
}
