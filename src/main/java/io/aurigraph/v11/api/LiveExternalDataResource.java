package io.aurigraph.v11.api;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;

import io.aurigraph.v11.live.LiveExternalDataService;
import io.aurigraph.v11.live.LiveExternalDataService.*;

import java.util.List;

/**
 * Live External Data API Resource
 *
 * Provides REST endpoints for accessing REAL external data:
 * - GET /api/v11/live/prices - Get all live cryptocurrency prices
 * - GET /api/v11/live/prices/{symbol} - Get specific price
 * - GET /api/v11/live/prices/stream/{symbol} - Stream price updates (SSE)
 * - GET /api/v11/live/market - Get aggregated market data
 * - POST /api/v11/live/tokenize/{symbol} - Tokenize live price data
 *
 * All data comes from REAL external APIs (CoinGecko, Binance)
 * NO mock/cached data - refreshes every 30 seconds
 *
 * @version 12.0.0
 * @author Backend Development Agent (BDA)
 */
@Path("/api/v11/live")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Live External Data API", description = "Real-time data from external APIs (CoinGecko, Binance)")
public class LiveExternalDataResource {

    private static final Logger LOG = Logger.getLogger(LiveExternalDataResource.class);

    @Inject
    LiveExternalDataService liveDataService;

    /**
     * GET /api/v11/live/prices
     * Returns all live cryptocurrency prices from external APIs
     */
    @GET
    @Path("/prices")
    @Operation(
        summary = "Get all live prices",
        description = "Returns real-time cryptocurrency prices from CoinGecko/Binance APIs. Updates every 30 seconds."
    )
    @APIResponse(
        responseCode = "200",
        description = "Live prices retrieved successfully",
        content = @Content(schema = @Schema(implementation = LivePriceData[].class))
    )
    public Uni<Response> getAllLivePrices() {
        LOG.info("API Request: GET /api/v11/live/prices - Fetching REAL live prices");

        return liveDataService.getAllLivePrices()
            .map(prices -> {
                LOG.infof("Returning %d live prices from external APIs", prices.size());
                return Response.ok(new LivePricesResponse(
                    prices.size(),
                    prices,
                    "real-time",
                    "Data from CoinGecko/Binance APIs"
                )).build();
            })
            .onFailure().recoverWithItem(error -> {
                LOG.errorf(error, "Failed to fetch live prices: %s", error.getMessage());
                return Response.serverError()
                    .entity(new ErrorResponse("FETCH_ERROR", error.getMessage()))
                    .build();
            });
    }

    /**
     * GET /api/v11/live/prices/{symbol}
     * Returns live price for a specific cryptocurrency
     */
    @GET
    @Path("/prices/{symbol}")
    @Operation(
        summary = "Get live price for symbol",
        description = "Returns real-time price for a specific cryptocurrency (BTC, ETH, SOL, etc.)"
    )
    @APIResponse(responseCode = "200", description = "Price retrieved successfully")
    @APIResponse(responseCode = "404", description = "Symbol not supported")
    public Uni<Response> getLivePrice(
        @Parameter(description = "Cryptocurrency symbol (BTC, ETH, SOL, etc.)", required = true)
        @PathParam("symbol") String symbol
    ) {
        LOG.infof("API Request: GET /api/v11/live/prices/%s - Fetching REAL price", symbol);

        return liveDataService.getLivePrice(symbol)
            .map(priceData -> {
                if (priceData.source.equals("unavailable")) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("NOT_FOUND", "Symbol not supported: " + symbol))
                        .build();
                }
                LOG.infof("Returning live price for %s: %s (source: %s)",
                    symbol, priceData.getFormattedPrice(), priceData.source);
                return Response.ok(priceData).build();
            })
            .onFailure().recoverWithItem(error -> {
                LOG.errorf(error, "Failed to fetch price for %s: %s", symbol, error.getMessage());
                return Response.serverError()
                    .entity(new ErrorResponse("FETCH_ERROR", error.getMessage()))
                    .build();
            });
    }

    /**
     * GET /api/v11/live/prices/stream/{symbol}
     * Server-Sent Events stream of price updates
     */
    @GET
    @Path("/prices/stream/{symbol}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Stream live price updates",
        description = "Server-Sent Events stream of real-time price updates for a cryptocurrency"
    )
    public Multi<LivePriceData> streamPriceUpdates(
        @Parameter(description = "Cryptocurrency symbol", required = true)
        @PathParam("symbol") String symbol
    ) {
        LOG.infof("API Request: GET /api/v11/live/prices/stream/%s - Starting SSE stream", symbol);
        return liveDataService.streamPriceUpdates(symbol);
    }

    /**
     * GET /api/v11/live/market
     * Returns aggregated market data
     */
    @GET
    @Path("/market")
    @Operation(
        summary = "Get live market data",
        description = "Returns aggregated market data including total market cap, volume, and service statistics"
    )
    @APIResponse(responseCode = "200", description = "Market data retrieved successfully")
    public Uni<Response> getLiveMarketData() {
        LOG.info("API Request: GET /api/v11/live/market - Fetching market data");

        return liveDataService.getLiveMarketData()
            .map(marketData -> {
                LOG.infof("Returning market data: %d assets tracked, source: %s",
                    marketData.trackedAssets(), marketData.activeSource());
                return Response.ok(marketData).build();
            })
            .onFailure().recoverWithItem(error -> {
                LOG.errorf(error, "Failed to fetch market data: %s", error.getMessage());
                return Response.serverError()
                    .entity(new ErrorResponse("FETCH_ERROR", error.getMessage()))
                    .build();
            });
    }

    /**
     * POST /api/v11/live/tokenize/{symbol}
     * Tokenizes live price data and stores on blockchain
     */
    @POST
    @Path("/tokenize/{symbol}")
    @Operation(
        summary = "Tokenize live price data",
        description = "Fetches real-time price and tokenizes it for blockchain storage"
    )
    @APIResponse(responseCode = "201", description = "Price data tokenized successfully")
    @APIResponse(responseCode = "400", description = "Invalid symbol")
    public Uni<Response> tokenizeLivePrice(
        @Parameter(description = "Cryptocurrency symbol to tokenize", required = true)
        @PathParam("symbol") String symbol
    ) {
        LOG.infof("API Request: POST /api/v11/live/tokenize/%s - Tokenizing live data", symbol);

        return liveDataService.tokenizeLivePrice(symbol)
            .map(tokenizedData -> {
                LOG.infof("Successfully tokenized %s price data: sourceId=%s",
                    symbol, tokenizedData.sourceId());
                return Response.status(Response.Status.CREATED)
                    .entity(tokenizedData)
                    .build();
            })
            .onFailure().recoverWithItem(error -> {
                LOG.errorf(error, "Failed to tokenize %s: %s", symbol, error.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("TOKENIZE_ERROR", error.getMessage()))
                    .build();
            });
    }

    /**
     * GET /api/v11/live/status
     * Returns service status and health
     */
    @GET
    @Path("/status")
    @Operation(
        summary = "Get live data service status",
        description = "Returns health and status information for the live data service"
    )
    public Uni<Response> getServiceStatus() {
        return liveDataService.getLiveMarketData()
            .map(marketData -> {
                boolean isHealthy = marketData.successfulFetches() > 0;
                String status = isHealthy ? "healthy" : "degraded";

                return Response.ok(new ServiceStatusResponse(
                    status,
                    marketData.activeSource(),
                    marketData.trackedAssets(),
                    marketData.lastUpdate(),
                    marketData.totalFetches(),
                    marketData.successfulFetches(),
                    marketData.failedFetches(),
                    "Live external data service operational"
                )).build();
            });
    }

    // ==================== Response DTOs ====================

    public record LivePricesResponse(
        int count,
        List<LivePriceData> prices,
        String dataType,
        String description
    ) {}

    public record ErrorResponse(
        String error,
        String message
    ) {}

    public record ServiceStatusResponse(
        String status,
        String activeSource,
        int trackedAssets,
        String lastUpdate,
        long totalFetches,
        long successfulFetches,
        long failedFetches,
        String message
    ) {}
}
