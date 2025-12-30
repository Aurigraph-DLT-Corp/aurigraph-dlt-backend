package io.aurigraph.v11.api.phase2;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Sprint 19: Token/NFT Marketplace REST API (21 pts)
 *
 * Endpoints for NFTs, trading orders, order book, and market analytics.
 * Extracted from Phase2BlockchainResource for better maintainability.
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 19
 */
@Path("/api/v11/blockchain")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MarketplaceResource {

    private static final Logger LOG = Logger.getLogger(MarketplaceResource.class);

    /**
     * List all NFTs
     * GET /api/v11/blockchain/marketplace/nfts
     */
    @GET
    @Path("/marketplace/nfts")
    public Uni<NFTList> getAllNFTs(@QueryParam("collection") String collection,
                                     @QueryParam("owner") String owner,
                                     @QueryParam("limit") @DefaultValue("50") int limit) {
        LOG.infof("Fetching all NFTs (collection: %s, owner: %s, limit: %d)", collection, owner, limit);

        return Uni.createFrom().item(() -> {
            NFTList list = new NFTList();
            list.totalNFTs = 5000;
            list.collections = 50;
            list.nfts = new ArrayList<>();

            String[] collections = {"Aurigraph Genesis", "Crypto Punks", "Bored Apes", "Art Blocks", "Cool Cats"};
            String[] statuses = {"LISTED", "UNLISTED", "AUCTION"};

            for (int i = 1; i <= Math.min(limit, 10); i++) {
                NFTSummary nft = new NFTSummary();
                nft.tokenId = "NFT-" + String.format("%05d", i);
                nft.collection = collections[i % collections.length];
                nft.name = collections[i % collections.length] + " #" + i;
                nft.owner = "0xowner-" + String.format("%03d", i);
                nft.price = new BigDecimal(String.valueOf(10.0 + (i * 0.5)));
                nft.currency = "AUR";
                nft.status = statuses[i % statuses.length];
                nft.imageUrl = "https://ipfs.io/ipfs/QmNFT" + i;
                nft.rarity = i % 4 == 0 ? "LEGENDARY" : i % 3 == 0 ? "RARE" : "COMMON";
                list.nfts.add(nft);
            }

            return list;
        });
    }

    /**
     * Create trading order
     * POST /api/v11/blockchain/marketplace/orders
     */
    @POST
    @Path("/marketplace/orders")
    public Uni<Response> createOrder(OrderCreation order) {
        LOG.infof("Creating %s order for %s", order.orderType, order.tokenAddress);

        return Uni.createFrom().item(() -> Response.ok(Map.of(
            "status", "success",
            "orderId", UUID.randomUUID().toString(),
            "orderType", order.orderType,
            "tokenAddress", order.tokenAddress,
            "amount", order.amount,
            "price", order.price,
            "createdAt", Instant.now().toString(),
            "message", "Order created successfully"
        )).build());
    }

    /**
     * Get order book
     * GET /api/v11/blockchain/marketplace/orderbook/{tokenAddress}
     */
    @GET
    @Path("/marketplace/orderbook/{tokenAddress}")
    public Uni<OrderBook> getOrderBook(@PathParam("tokenAddress") String tokenAddress) {
        LOG.infof("Fetching order book for: %s", tokenAddress);

        return Uni.createFrom().item(() -> {
            OrderBook book = new OrderBook();
            book.tokenAddress = tokenAddress;
            book.bids = new ArrayList<>();
            book.asks = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                book.bids.add(new OrderLevel(
                    new BigDecimal("10.00").subtract(new BigDecimal(String.valueOf(i * 0.1))),
                    new BigDecimal("1000").add(new BigDecimal(String.valueOf(i * 100)))));
                book.asks.add(new OrderLevel(
                    new BigDecimal("10.10").add(new BigDecimal(String.valueOf(i * 0.1))),
                    new BigDecimal("950").add(new BigDecimal(String.valueOf(i * 80)))));
            }

            book.lastPrice = new BigDecimal("10.05");
            book.volume24h = new BigDecimal("150000");
            book.spread = 0.10;
            return book;
        });
    }

    /**
     * Get market analytics
     * GET /api/v11/blockchain/marketplace/analytics
     */
    @GET
    @Path("/marketplace/analytics")
    public Uni<MarketAnalytics> getMarketAnalytics() {
        LOG.info("Fetching market analytics");

        return Uni.createFrom().item(() -> {
            MarketAnalytics analytics = new MarketAnalytics();
            analytics.totalVolume24h = new BigDecimal("2500000000");
            analytics.totalTrades24h = 125000;
            analytics.uniqueTraders24h = 15000;
            analytics.averageTradeSize = new BigDecimal("20000");
            analytics.topTradedToken = "USDA";
            analytics.mostActiveTrader = "0xtrader-1-address";
            analytics.marketCap = new BigDecimal("45000000000");
            return analytics;
        });
    }

    // ==================== DTOs ====================

    public static class OrderCreation {
        public String orderType;
        public String tokenAddress;
        public String amount;
        public String price;
    }

    public static class OrderBook {
        public String tokenAddress;
        public List<OrderLevel> bids;
        public List<OrderLevel> asks;
        public BigDecimal lastPrice;
        public BigDecimal volume24h;
        public double spread;
    }

    public static class OrderLevel {
        public BigDecimal price;
        public BigDecimal amount;

        public OrderLevel(BigDecimal price, BigDecimal amount) {
            this.price = price;
            this.amount = amount;
        }
    }

    public static class MarketAnalytics {
        public BigDecimal totalVolume24h;
        public long totalTrades24h;
        public int uniqueTraders24h;
        public BigDecimal averageTradeSize;
        public String topTradedToken;
        public String mostActiveTrader;
        public BigDecimal marketCap;
    }

    public static class NFTList {
        public int totalNFTs;
        public int collections;
        public List<NFTSummary> nfts;
    }

    public static class NFTSummary {
        public String tokenId;
        public String collection;
        public String name;
        public String owner;
        public BigDecimal price;
        public String currency;
        public String status;
        public String imageUrl;
        public String rarity;
    }
}
