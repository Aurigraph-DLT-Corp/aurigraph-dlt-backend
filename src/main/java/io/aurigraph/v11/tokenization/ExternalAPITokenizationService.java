package io.aurigraph.v11.tokenization;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.aurigraph.v11.tokenization.models.*;
import io.aurigraph.v11.storage.LevelDBStorageService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;

/**
 * External API Tokenization Service
 *
 * Core service for:
 * - Managing external API sources
 * - Fetching data from external APIs
 * - Tokenizing external data into blockchain transactions
 * - Storing tokenized data in LevelDB (all nodes)
 * - Streaming data to channels
 * - Real-time monitoring and statistics
 *
 * @version 11.3.0
 * @author Backend Development Agent (BDA)
 */
@ApplicationScoped
public class ExternalAPITokenizationService {

    private static final Logger LOG = Logger.getLogger(ExternalAPITokenizationService.class);

    @Inject
    LevelDBStorageService levelDBStorage;

    @ConfigProperty(name = "tokenization.api.base-path", defaultValue = "/data/tokenization")
    String basePath;

    @ConfigProperty(name = "tokenization.api.max-concurrent", defaultValue = "10")
    int maxConcurrent;

    private final Map<String, APISource> sources = new ConcurrentHashMap<>();
    private final Map<String, TokenizedTransaction> transactions = new ConcurrentHashMap<>();
    private final Map<String, ChannelStats> channelStats = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    // ==================== API SOURCE MANAGEMENT ====================

    public Uni<List<APISource>> getAllSources() {
        return Uni.createFrom().item(() -> new ArrayList<>(sources.values()));
    }

    public Uni<APISource> getSource(String sourceId) {
        APISource source = sources.get(sourceId);
        if (source == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Source not found: " + sourceId));
        }
        return Uni.createFrom().item(source);
    }

    public Uni<APISource> addSource(
        String name,
        String url,
        String method,
        Map<String, String> headers,
        String channel,
        int pollInterval
    ) {
        return Uni.createFrom().item(() -> {
            String sourceId = "src-" + UUID.randomUUID().toString();

            APISource source = new APISource();
            source.id = sourceId;
            source.name = name;
            source.url = url;
            source.method = method;
            source.headers = headers != null ? headers : new HashMap<>();
            source.channel = channel;
            source.status = "active";
            source.pollInterval = pollInterval;
            source.lastFetch = null;
            source.totalTokenized = 0;
            source.errorCount = 0;

            sources.put(sourceId, source);

            // Initialize channel stats
            channelStats.putIfAbsent(channel, new ChannelStats(
                channel,
                name + " Channel",
                0,
                0L,
                Instant.now().toString(),
                "active"
            ));

            // Schedule polling if active
            if ("active".equals(source.status)) {
                schedulePolling(source);
            }

            LOG.infof("Added API source: %s (%s)", name, sourceId);
            return source;
        });
    }

    public Uni<APISource> updateSourceStatus(String sourceId, String status) {
        return Uni.createFrom().item(() -> {
            APISource source = sources.get(sourceId);
            if (source == null) {
                throw new IllegalArgumentException("Source not found: " + sourceId);
            }

            source.status = status;

            if ("active".equals(status)) {
                schedulePolling(source);
            }

            LOG.infof("Updated source %s status to: %s", sourceId, status);
            return source;
        });
    }

    public Uni<Boolean> deleteSource(String sourceId) {
        return Uni.createFrom().item(() -> {
            APISource removed = sources.remove(sourceId);
            if (removed == null) {
                throw new IllegalArgumentException("Source not found: " + sourceId);
            }

            LOG.infof("Deleted API source: %s", sourceId);
            return true;
        });
    }

    // ==================== TOKENIZATION LOGIC ====================

    private void schedulePolling(APISource source) {
        scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    if ("active".equals(source.status)) {
                        fetchAndTokenize(source).subscribe().with(
                            result -> LOG.infof("Tokenized data from %s: %s", source.name, result.transactionId),
                            error -> {
                                LOG.errorf(error, "Failed to tokenize from %s", source.name);
                                source.errorCount++;
                            }
                        );
                    }
                } catch (Exception e) {
                    LOG.errorf(e, "Error in scheduled tokenization for %s", source.name);
                    source.errorCount++;
                }
            },
            0,
            source.pollInterval,
            TimeUnit.SECONDS
        );
    }

    private Uni<TokenizationResult> fetchAndTokenize(APISource source) {
        return Uni.createFrom().item(() -> {
            // Build HTTP request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(source.url))
                .timeout(Duration.ofSeconds(30));

            // Add headers
            if (source.headers != null) {
                source.headers.forEach(requestBuilder::header);
            }

            // Set method
            switch (source.method.toUpperCase()) {
                case "GET":
                    requestBuilder.GET();
                    break;
                case "POST":
                    requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                    break;
                case "PUT":
                    requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
                    break;
                case "DELETE":
                    requestBuilder.DELETE();
                    break;
                default:
                    requestBuilder.GET();
            }

            HttpRequest request = requestBuilder.build();

            // Fetch data
            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to fetch data from " + source.url + ": " + e.getMessage(), e);
            }

            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }

            String data = response.body();
            byte[] dataBytes = data.getBytes();

            // Calculate hash
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (Exception e) {
                throw new RuntimeException("SHA-256 not available: " + e.getMessage(), e);
            }
            byte[] hashBytes = digest.digest(dataBytes);
            String dataHash = "0x" + bytesToHex(hashBytes);

            // Create tokenized transaction
            String txId = "tx-" + UUID.randomUUID().toString();

            TokenizedTransaction tx = new TokenizedTransaction();
            tx.id = txId;
            tx.sourceId = source.id;
            tx.sourceName = source.name;
            tx.channel = source.channel;
            tx.timestamp = Instant.now().toString();
            tx.dataHash = dataHash;
            tx.size = dataBytes.length;
            tx.status = "pending";
            tx.leveldbPath = null;

            // Store in LevelDB (ALL NODES implementation)
            String leveldbPath = levelDBStorage.storeTokenizedData(
                source.channel,
                txId,
                dataHash,
                dataBytes
            ).await().indefinitely();

            tx.leveldbPath = leveldbPath;
            tx.status = "stored";

            // Update source stats
            source.lastFetch = Instant.now().toString();
            source.totalTokenized++;

            // Update channel stats
            ChannelStats stats = channelStats.get(source.channel);
            if (stats != null) {
                stats.transactionCount++;
                stats.totalSize += dataBytes.length;
                stats.lastUpdated = Instant.now().toString();
            }

            // Store transaction
            transactions.put(txId, tx);

            LOG.infof("Tokenized %d bytes from %s to channel %s", dataBytes.length, source.name, source.channel);

            return new TokenizationResult(
                true,
                txId,
                dataHash,
                dataBytes.length,
                source.channel,
                leveldbPath,
                "Data tokenized and stored successfully"
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    public Uni<TokenizationResult> manualTokenize(String sourceId) {
        return getSource(sourceId).flatMap(this::fetchAndTokenize);
    }

    // ==================== TRANSACTION QUERIES ====================

    public Uni<List<TokenizedTransaction>> getTokenizedTransactions(int limit, String channel) {
        return Uni.createFrom().item(() -> {
            List<TokenizedTransaction> result = transactions.values().stream()
                .filter(tx -> channel == null || channel.equals(tx.channel))
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .limit(limit)
                .toList();
            return result;
        });
    }

    public Uni<TokenizedTransaction> getTransaction(String txId) {
        TokenizedTransaction tx = transactions.get(txId);
        if (tx == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Transaction not found: " + txId));
        }
        return Uni.createFrom().item(tx);
    }

    // ==================== CHANNEL STATISTICS ====================

    public Uni<List<ChannelStats>> getChannelStats() {
        return Uni.createFrom().item(() -> new ArrayList<>(channelStats.values()));
    }

    public Uni<ChannelStats> getChannelStat(String channelId) {
        ChannelStats stats = channelStats.get(channelId);
        if (stats == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Channel not found: " + channelId));
        }
        return Uni.createFrom().item(stats);
    }

    // ==================== REAL-TIME STREAMING ====================

    public Multi<TokenizationEvent> streamTokenizationEvents(String channel) {
        return Multi.createFrom().ticks()
            .every(Duration.ofSeconds(1))
            .onItem().transform(tick -> {
                // Get latest transaction for channel
                Optional<TokenizedTransaction> latestTx = transactions.values().stream()
                    .filter(tx -> channel == null || channel.equals(tx.channel))
                    .max((a, b) -> a.timestamp.compareTo(b.timestamp));

                if (latestTx.isPresent()) {
                    TokenizedTransaction tx = latestTx.get();
                    return new TokenizationEvent(
                        "transaction_created",
                        tx.id,
                        tx.channel,
                        tx.dataHash,
                        tx.size,
                        tx.status,
                        Instant.now().toString()
                    );
                } else {
                    return new TokenizationEvent(
                        "heartbeat",
                        null,
                        channel,
                        null,
                        0,
                        "active",
                        Instant.now().toString()
                    );
                }
            });
    }

    // ==================== STORAGE INFORMATION ====================

    public Uni<StorageInfo> getStorageInfo() {
        return levelDBStorage.getStorageInfo();
    }

    // ==================== UTILITY METHODS ====================

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
