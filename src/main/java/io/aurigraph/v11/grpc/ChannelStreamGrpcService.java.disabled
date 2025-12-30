package io.aurigraph.v11.grpc;

import io.aurigraph.v11.proto.*;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC Streaming Service for Channel Monitoring
 *
 * Replaces WebSocket /ws/channels endpoint with gRPC-Web bidirectional streaming.
 * Provides real-time streaming of channel creation, updates, and transactions.
 *
 * Benefits over WebSocket+JSON:
 * - 60-70% bandwidth reduction (Protobuf vs JSON)
 * - Type-safe client generation (TypeScript + Java)
 * - Built-in flow control and backpressure
 * - HTTP/2 multiplexing (multiple streams, one connection)
 *
 * @author Backend Development Agent (BDA)
 * @since V12.0.0
 */
@GrpcService
@ApplicationScoped
public class ChannelStreamGrpcService implements ChannelStreamService {

    private static final Logger LOG = Logger.getLogger(ChannelStreamGrpcService.class);

    // Active subscriptions for interactive monitoring
    private final Map<String, UnicastProcessor<ChannelEventStream>> activeSubscriptions = new ConcurrentHashMap<>();

    // Simulated channels
    private final List<String> channelIds = Arrays.asList(
            "main-channel", "trading-channel", "settlement-channel",
            "audit-channel", "governance-channel"
    );

    /**
     * Get channel information (unary RPC)
     */
    @Override
    public Uni<ChannelInfo> getChannelInfo(ChannelSubscribeRequest request) {
        String channelId = request.getChannelIdsList().isEmpty() ?
                channelIds.get(0) : request.getChannelIds(0);
        LOG.infof("Getting channel info for: %s", channelId);
        return Uni.createFrom().item(() -> buildChannelInfo(channelId));
    }

    /**
     * List all accessible channels (unary RPC)
     */
    @Override
    public Uni<ChannelListUpdate> listChannels(ChannelSubscribeRequest request) {
        LOG.infof("Listing channels for client: %s", request.getClientId());
        return Uni.createFrom().item(this::buildChannelListUpdate);
    }

    /**
     * Stream all channel events (server streaming)
     */
    @Override
    public Multi<ChannelEventStream> streamChannelEvents(ChannelSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 1000;
        LOG.infof("Starting channel event stream for client %s with interval %dms",
                request.getClientId(), intervalMs);

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildChannelEventStream())
                .onSubscription().invoke(() -> LOG.info("Channel event stream subscription started"))
                .onCancellation().invoke(() -> LOG.info("Channel event stream cancelled"));
    }

    /**
     * Stream channel transactions (server streaming)
     */
    @Override
    public Multi<ChannelTransactionEvent> streamChannelTransactions(ChannelSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 500;

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildChannelTransactionEvent());
    }

    /**
     * Stream channel blocks (server streaming)
     */
    @Override
    public Multi<ChannelBlockEvent> streamChannelBlocks(ChannelSubscribeRequest request) {
        return Multi.createFrom().ticks()
                .every(Duration.ofSeconds(1))
                .onItem().transform(tick -> buildChannelBlockEvent());
    }

    /**
     * Stream channel performance metrics (server streaming)
     */
    @Override
    public Multi<ChannelPerformanceUpdate> streamChannelPerformance(ChannelSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 2000;

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildChannelPerformanceUpdate());
    }

    /**
     * Stream participant events (server streaming)
     */
    @Override
    public Multi<ParticipantEvent> streamParticipantEvents(ChannelSubscribeRequest request) {
        return Multi.createFrom().ticks()
                .every(Duration.ofSeconds(30))
                .onItem().transform(tick -> buildParticipantEvent());
    }

    /**
     * Interactive channel monitoring (bidirectional streaming)
     */
    @Override
    public Multi<ChannelEventStream> interactiveChannelMonitor(Multi<ChannelCommand> commands) {
        String subscriptionId = UUID.randomUUID().toString();
        UnicastProcessor<ChannelEventStream> processor = UnicastProcessor.create();
        activeSubscriptions.put(subscriptionId, processor);

        // Handle incoming commands
        commands.subscribe().with(
                command -> handleChannelCommand(subscriptionId, command, processor),
                error -> {
                    LOG.errorf(error, "Error in channel command stream: %s", subscriptionId);
                    activeSubscriptions.remove(subscriptionId);
                    processor.onComplete();
                },
                () -> {
                    LOG.infof("Channel command stream completed: %s", subscriptionId);
                    activeSubscriptions.remove(subscriptionId);
                    processor.onComplete();
                }
        );

        // Start default event stream
        Multi.createFrom().ticks()
                .every(Duration.ofMillis(1000))
                .subscribe().with(
                        tick -> {
                            if (activeSubscriptions.containsKey(subscriptionId)) {
                                processor.onNext(buildChannelEventStream());
                            }
                        }
                );

        return processor;
    }

    /**
     * Query historical channel events
     */
    @Override
    public Uni<ChannelHistoricalResponse> queryHistoricalEvents(ChannelHistoricalQuery query) {
        LOG.infof("Querying historical channel events for channel %s", query.getChannelId());

        List<ChannelEventStream> events = new ArrayList<>();
        for (int i = 0; i < Math.min(query.getLimit(), 100); i++) {
            events.add(buildChannelEventStream());
        }

        return Uni.createFrom().item(
                ChannelHistoricalResponse.newBuilder()
                        .setChannelId(query.getChannelId())
                        .addAllEvents(events)
                        .setTotalEvents(events.size())
                        .setHasMore(false)
                        .setQueryExecutedAt(buildTimestamp())
                        .build()
        );
    }

    /**
     * Get channel analytics
     */
    @Override
    public Uni<ChannelAnalytics> getChannelAnalytics(ChannelHistoricalQuery query) {
        String channelId = query.getChannelId().isEmpty() ? channelIds.get(0) : query.getChannelId();

        return Uni.createFrom().item(
                ChannelAnalytics.newBuilder()
                        .setChannelId(channelId)
                        .setPeriodStart(query.getStartTime())
                        .setPeriodEnd(query.getEndTime())
                        .setTotalTransactions(125000L)
                        .setSuccessfulTransactions(124500L)
                        .setFailedTransactions(500L)
                        .setSuccessRatePercent(99.6)
                        .setTotalParticipants(25)
                        .setActiveParticipants(20)
                        .putTransactionsByParticipant("participant-1", 25000L)
                        .putTransactionsByParticipant("participant-2", 20000L)
                        .putTransactionsByParticipant("participant-3", 18000L)
                        .setTotalStorageUsedBytes(5000000000L)
                        .setBandwidthUsedBytes(2500000000L)
                        .setAverageBlockSizeKb(256.0)
                        .build()
        );
    }

    /**
     * Unsubscribe from streaming
     */
    @Override
    public Uni<ChannelUnsubscribeResponse> unsubscribe(ChannelUnsubscribeRequest request) {
        String subscriptionId = request.getSubscriptionId();
        UnicastProcessor<ChannelEventStream> processor = activeSubscriptions.remove(subscriptionId);

        if (processor != null) {
            processor.onComplete();
            return Uni.createFrom().item(
                    ChannelUnsubscribeResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("Successfully unsubscribed from channel stream")
                            .setTotalEventsDelivered(100L)
                            .setEndedAt(buildTimestamp())
                            .build()
            );
        }

        return Uni.createFrom().item(
                ChannelUnsubscribeResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscription not found: " + subscriptionId)
                        .build()
        );
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private void handleChannelCommand(String subscriptionId, ChannelCommand command,
                                      UnicastProcessor<ChannelEventStream> processor) {
        LOG.infof("Handling channel command: %s for subscription: %s",
                command.getCommand(), subscriptionId);

        switch (command.getCommand()) {
            case SUBSCRIBE_CHANNEL:
                LOG.infof("Subscribing to channel: %s", command.getChannelId());
                break;
            case UNSUBSCRIBE_CHANNEL:
                LOG.infof("Unsubscribing from channel: %s", command.getChannelId());
                break;
            case REQUEST_SNAPSHOT:
                processor.onNext(buildChannelEventStream());
                break;
            case PAUSE:
                LOG.infof("Pausing channel stream: %s", subscriptionId);
                break;
            case RESUME:
                LOG.infof("Resuming channel stream: %s", subscriptionId);
                break;
            default:
                LOG.debugf("Unhandled command type: %s", command.getCommand());
        }
    }

    private ChannelInfo buildChannelInfo(String channelId) {
        return ChannelInfo.newBuilder()
                .setChannelId(channelId)
                .setChannelName(channelId.replace("-", " ").toUpperCase())
                .setChannelType(ChannelType.CONSORTIUM)
                .setStatus(ChannelStatus.ACTIVE)
                .addParticipantIds("participant-1")
                .addParticipantIds("participant-2")
                .addParticipantIds("participant-3")
                .setParticipantCount(3)
                .setCreatorId("admin")
                .setCreatedAt(buildTimestamp())
                .setLastActivity(buildTimestamp())
                .setDescription("Enterprise channel for " + channelId)
                .setStatistics(ChannelStatistics.newBuilder()
                        .setTotalTransactions(125000L + (long) (Math.random() * 10000))
                        .setTotalBlocks(5000L + (long) (Math.random() * 500))
                        .setStorageUsedBytes(5000000000L)
                        .setAverageTps(776.0 + Math.random() * 50)
                        .setLastBlockTime(buildTimestamp())
                        .setActiveContracts(12)
                        .build())
                .setConfig(ChannelConfig.newBuilder()
                        .setMaxParticipants(100)
                        .setRequireApproval(true)
                        .setAllowPublicRead(false)
                        .setTransactionLimitPerSecond(10000)
                        .setStorageLimitBytes(10000000000L)
                        .addAllowedTransactionTypes("transfer")
                        .addAllowedTransactionTypes("contract_call")
                        .addAllowedTransactionTypes("asset_create")
                        .build())
                .build();
    }

    private ChannelListUpdate buildChannelListUpdate() {
        ChannelListUpdate.Builder builder = ChannelListUpdate.newBuilder()
                .setTotalChannels(channelIds.size())
                .setActiveChannels(channelIds.size())
                .setSuspendedChannels(0)
                .setTimestamp(buildTimestamp());

        for (String channelId : channelIds) {
            builder.addChannels(buildChannelInfo(channelId));
        }

        return builder.build();
    }

    private ChannelEventStream buildChannelEventStream() {
        String channelId = channelIds.get((int) (Math.random() * channelIds.size()));
        int eventType = (int) (Math.random() * 4);

        ChannelEventStream.Builder builder = ChannelEventStream.newBuilder()
                .setTimestamp(buildTimestamp())
                .setEventId(UUID.randomUUID().toString())
                .setChannelId(channelId);

        switch (eventType) {
            case 0:
                builder.setTransaction(buildChannelTransactionEvent());
                break;
            case 1:
                builder.setBlock(buildChannelBlockEvent());
                break;
            case 2:
                builder.setPerformance(buildChannelPerformanceUpdate());
                break;
            case 3:
                builder.setChannelList(buildChannelListUpdate());
                break;
        }

        return builder.build();
    }

    private ChannelTransactionEvent buildChannelTransactionEvent() {
        String channelId = channelIds.get((int) (Math.random() * channelIds.size()));

        return ChannelTransactionEvent.newBuilder()
                .setChannelId(channelId)
                .setTransaction(Transaction.newBuilder()
                        .setTransactionId(UUID.randomUUID().toString())
                        .setTransactionHash("0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 64))
                        .setSender("0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 40))
                        .setReceiver("0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 40))
                        .setAmount(100.0 + Math.random() * 1000)
                        .setFee(0.001)
                        .setNonce((int) (Math.random() * 1000))
                        .setTimestamp(buildTimestamp())
                        .build())
                .setStatus(TransactionStatus.CONFIRMED)
                .setSubmitterId("participant-" + (1 + (int) (Math.random() * 3)))
                .setBlockHeight(1000000L + (long) (Math.random() * 1000))
                .setBlockHash("0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 64))
                .setSubmittedAt(buildTimestamp())
                .setConfirmedAt(buildTimestamp())
                .setConfirmationTimeMs(45 + (long) (Math.random() * 30))
                .setTimestamp(buildTimestamp())
                .build();
    }

    private ChannelBlockEvent buildChannelBlockEvent() {
        String channelId = channelIds.get((int) (Math.random() * channelIds.size()));

        return ChannelBlockEvent.newBuilder()
                .setChannelId(channelId)
                .setBlock(Block.newBuilder()
                        .setBlockHash("0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 64))
                        .setBlockNumber(1000000L + (long) (Math.random() * 1000))
                        .setPreviousBlockHash("0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 64))
                        .setTransactionCount(100 + (int) (Math.random() * 100))
                        .setTimestamp(buildTimestamp())
                        .build())
                .setBlockHeight(1000000L + (long) (Math.random() * 1000))
                .setTransactionCount(100 + (int) (Math.random() * 100))
                .setProposerId("validator-1")
                .setCreatedAt(buildTimestamp())
                .build();
    }

    private ChannelPerformanceUpdate buildChannelPerformanceUpdate() {
        String channelId = channelIds.get((int) (Math.random() * channelIds.size()));

        return ChannelPerformanceUpdate.newBuilder()
                .setChannelId(channelId)
                .setCurrentTps(776.0 + Math.random() * 50)
                .setAverageTpsLastMinute(780.0 + Math.random() * 40)
                .setPeakTps(850.0 + Math.random() * 50)
                .setTransactionsLastMinute(46000L + (long) (Math.random() * 3000))
                .setAverageTransactionLatencyMs(45.0 + Math.random() * 20)
                .setP95TransactionLatencyMs(80.0 + Math.random() * 30)
                .setP99TransactionLatencyMs(120.0 + Math.random() * 50)
                .setStorageUsedBytes(5000000000L + (long) (Math.random() * 100000000))
                .setStorageLimitBytes(10000000000L)
                .setStorageUtilizationPercent(50.0 + Math.random() * 5)
                .setActiveParticipants(20 + (int) (Math.random() * 5))
                .setPendingTransactions((int) (Math.random() * 100))
                .setFailedTransactionsLastHour((int) (Math.random() * 10))
                .setHealthScore(95.0 + Math.random() * 5)
                .setTimestamp(buildTimestamp())
                .build();
    }

    private ParticipantEvent buildParticipantEvent() {
        String channelId = channelIds.get((int) (Math.random() * channelIds.size()));

        return ParticipantEvent.newBuilder()
                .setChannelId(channelId)
                .setParticipantId("participant-" + (1 + (int) (Math.random() * 10)))
                .setEventType(ParticipantEvent.EventType.ROLE_CHANGED)
                .setOldRole("MEMBER")
                .setNewRole("MEMBER")
                .setInitiatedBy("admin")
                .setReason("Regular activity")
                .setTimestamp(buildTimestamp())
                .build();
    }

    private com.google.protobuf.Timestamp buildTimestamp() {
        Instant now = Instant.now();
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }
}
