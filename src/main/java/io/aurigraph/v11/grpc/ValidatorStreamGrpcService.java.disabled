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
 * gRPC Streaming Service for Validator Monitoring
 *
 * Replaces WebSocket /ws/validators endpoint with gRPC-Web bidirectional streaming.
 * Provides real-time streaming of validator status, performance, and health metrics.
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
public class ValidatorStreamGrpcService implements ValidatorStreamService {

    private static final Logger LOG = Logger.getLogger(ValidatorStreamGrpcService.class);

    // Active subscriptions for interactive monitoring
    private final Map<String, UnicastProcessor<ValidatorEventStream>> activeSubscriptions = new ConcurrentHashMap<>();

    // Simulated validator data (replace with actual service injection)
    private final List<String> validatorIds = Arrays.asList(
            "validator-1", "validator-2", "validator-3", "validator-4", "validator-5"
    );

    /**
     * Get current validator status (unary RPC)
     */
    @Override
    public Uni<ValidatorStatusUpdate> getValidatorStatus(ValidatorSubscribeRequest request) {
        LOG.infof("Getting validator status for client: %s", request.getClientId());
        return Uni.createFrom().item(this::buildValidatorStatusUpdate);
    }

    /**
     * Get validator set information (unary RPC)
     */
    @Override
    public Uni<ValidatorSetUpdate> getValidatorSet(ValidatorSubscribeRequest request) {
        LOG.infof("Getting validator set for client: %s", request.getClientId());
        return Uni.createFrom().item(this::buildValidatorSetUpdate);
    }

    /**
     * Get validator rankings (unary RPC)
     */
    @Override
    public Uni<ValidatorRankingUpdate> getValidatorRankings(ValidatorSubscribeRequest request) {
        LOG.infof("Getting validator rankings for client: %s", request.getClientId());
        return Uni.createFrom().item(this::buildValidatorRankingUpdate);
    }

    /**
     * Stream all validator events (server streaming)
     */
    @Override
    public Multi<ValidatorEventStream> streamValidatorEvents(ValidatorSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 2000;
        LOG.infof("Starting validator event stream for client %s with interval %dms",
                request.getClientId(), intervalMs);

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildValidatorEventStream())
                .onSubscription().invoke(() -> LOG.info("Validator event stream subscription started"))
                .onCancellation().invoke(() -> LOG.info("Validator event stream cancelled"));
    }

    /**
     * Stream validator status updates (server streaming)
     */
    @Override
    public Multi<ValidatorStatusUpdate> streamValidatorStatus(ValidatorSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 2000;
        LOG.infof("Starting validator status stream for client %s", request.getClientId());

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildValidatorStatusUpdate());
    }

    /**
     * Stream block proposal activities (server streaming)
     */
    @Override
    public Multi<BlockProposalActivity> streamBlockProposals(ValidatorSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 5000;

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildBlockProposalActivity());
    }

    /**
     * Stream voting activities (server streaming)
     */
    @Override
    public Multi<VotingActivity> streamVotingActivity(ValidatorSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 3000;

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildVotingActivity());
    }

    /**
     * Stream validator rewards (server streaming)
     */
    @Override
    public Multi<ValidatorRewardEvent> streamRewards(ValidatorSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 10000;

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildValidatorRewardEvent());
    }

    /**
     * Interactive validator monitoring (bidirectional streaming)
     * Allows clients to send commands while receiving updates
     */
    @Override
    public Multi<ValidatorEventStream> interactiveValidatorMonitor(Multi<ValidatorCommand> commands) {
        String subscriptionId = UUID.randomUUID().toString();
        UnicastProcessor<ValidatorEventStream> processor = UnicastProcessor.create();
        activeSubscriptions.put(subscriptionId, processor);

        // Handle incoming commands
        commands.subscribe().with(
                command -> handleValidatorCommand(subscriptionId, command, processor),
                error -> {
                    LOG.errorf(error, "Error in validator command stream: %s", subscriptionId);
                    activeSubscriptions.remove(subscriptionId);
                    processor.onComplete();
                },
                () -> {
                    LOG.infof("Validator command stream completed: %s", subscriptionId);
                    activeSubscriptions.remove(subscriptionId);
                    processor.onComplete();
                }
        );

        // Start default event stream
        Multi.createFrom().ticks()
                .every(Duration.ofMillis(2000))
                .subscribe().with(
                        tick -> {
                            if (activeSubscriptions.containsKey(subscriptionId)) {
                                processor.onNext(buildValidatorEventStream());
                            }
                        }
                );

        return processor;
    }

    /**
     * Query historical validator events
     */
    @Override
    public Uni<ValidatorHistoricalResponse> queryHistoricalEvents(ValidatorHistoricalQuery query) {
        LOG.infof("Querying historical validator events from %s to %s",
                query.getStartTime(), query.getEndTime());

        List<ValidatorEventStream> events = new ArrayList<>();
        for (int i = 0; i < Math.min(query.getLimit(), 100); i++) {
            events.add(buildValidatorEventStream());
        }

        return Uni.createFrom().item(
                ValidatorHistoricalResponse.newBuilder()
                        .addAllEvents(events)
                        .setTotalEvents(events.size())
                        .setHasMore(false)
                        .setQueryExecutedAt(buildTimestamp())
                        .build()
        );
    }

    /**
     * Get validator analytics
     */
    @Override
    public Uni<ValidatorAnalytics> getValidatorAnalytics(ValidatorHistoricalQuery query) {
        String validatorId = query.getValidatorIdsList().isEmpty() ? "validator-1" : query.getValidatorIds(0);

        return Uni.createFrom().item(
                ValidatorAnalytics.newBuilder()
                        .setValidatorId(validatorId)
                        .setPeriodStart(query.getStartTime())
                        .setPeriodEnd(query.getEndTime())
                        .setTotalBlocksProposed(1250L)
                        .setTotalVotesCast(5000L)
                        .setTotalTransactionsProcessed(125000L)
                        .setTotalRewardsEarned(1500.0)
                        .setTotalPenaltiesIncurred(5.0)
                        .setNetEarnings(1495.0)
                        .build()
        );
    }

    /**
     * Compare multiple validators
     */
    @Override
    public Uni<ValidatorComparison> compareValidators(ValidatorHistoricalQuery query) {
        ValidatorComparison.Builder comparison = ValidatorComparison.newBuilder()
                .addAllValidatorIds(query.getValidatorIdsList())
                .setTimestamp(buildTimestamp());

        for (String validatorId : query.getValidatorIdsList()) {
            comparison.putPerformanceComparison(validatorId, buildPerformanceMetrics());
            comparison.putHealthComparison(validatorId, buildHealthMetrics());
            comparison.putReputationComparison(validatorId, 85.0 + Math.random() * 15);
        }

        return Uni.createFrom().item(comparison.build());
    }

    /**
     * Unsubscribe from streaming
     */
    @Override
    public Uni<ValidatorUnsubscribeResponse> unsubscribe(ValidatorUnsubscribeRequest request) {
        String subscriptionId = request.getSubscriptionId();
        UnicastProcessor<ValidatorEventStream> processor = activeSubscriptions.remove(subscriptionId);

        if (processor != null) {
            processor.onComplete();
            return Uni.createFrom().item(
                    ValidatorUnsubscribeResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("Successfully unsubscribed from validator stream")
                            .setTotalEventsDelivered(100L)
                            .setEndedAt(buildTimestamp())
                            .build()
            );
        }

        return Uni.createFrom().item(
                ValidatorUnsubscribeResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscription not found: " + subscriptionId)
                        .build()
        );
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private void handleValidatorCommand(String subscriptionId, ValidatorCommand command,
                                        UnicastProcessor<ValidatorEventStream> processor) {
        LOG.infof("Handling validator command: %s for subscription: %s",
                command.getCommand(), subscriptionId);

        switch (command.getCommand()) {
            case REQUEST_SNAPSHOT:
                processor.onNext(buildValidatorEventStream());
                break;
            case PAUSE:
                LOG.infof("Pausing validator stream: %s", subscriptionId);
                break;
            case RESUME:
                LOG.infof("Resuming validator stream: %s", subscriptionId);
                break;
            default:
                LOG.debugf("Unhandled command type: %s", command.getCommand());
        }
    }

    private ValidatorStatusUpdate buildValidatorStatusUpdate() {
        String validatorId = validatorIds.get((int) (Math.random() * validatorIds.size()));

        return ValidatorStatusUpdate.newBuilder()
                .setValidator(buildValidatorInfo(validatorId))
                .setHealth(buildHealthMetrics())
                .setPerformance(buildPerformanceMetrics())
                .setReputation(buildReputationInfo())
                .setTimestamp(buildTimestamp())
                .build();
    }

    private ValidatorSetUpdate buildValidatorSetUpdate() {
        ValidatorSetUpdate.Builder builder = ValidatorSetUpdate.newBuilder()
                .setTotalValidators(validatorIds.size())
                .setActiveValidators(validatorIds.size())
                .setInactiveValidators(0)
                .setTimestamp(buildTimestamp());

        for (String validatorId : validatorIds) {
            builder.addValidatorList(buildValidatorInfo(validatorId));
        }

        return builder.build();
    }

    private ValidatorRankingUpdate buildValidatorRankingUpdate() {
        ValidatorRankingUpdate.Builder builder = ValidatorRankingUpdate.newBuilder()
                .setRankingCriteria("reputation")
                .setTimestamp(buildTimestamp());

        for (int i = 0; i < validatorIds.size(); i++) {
            builder.addRankings(
                    ValidatorRanking.newBuilder()
                            .setRank(i + 1)
                            .setValidatorId(validatorIds.get(i))
                            .setScore(95.0 - (i * 2))
                            .setStake(10000.0 - (i * 500))
                            .setReputation(90.0 - (i * 1.5))
                            .setPerformanceScore(92.0 - i)
                            .build()
            );
        }

        return builder.build();
    }

    private ValidatorEventStream buildValidatorEventStream() {
        return ValidatorEventStream.newBuilder()
                .setTimestamp(buildTimestamp())
                .setEventId(UUID.randomUUID().toString())
                .setValidatorId(validatorIds.get((int) (Math.random() * validatorIds.size())))
                .setStatusUpdate(buildValidatorStatusUpdate())
                .build();
    }

    private ValidatorInfo buildValidatorInfo(String validatorId) {
        return ValidatorInfo.newBuilder()
                .setValidatorId(validatorId)
                .setAddress("0x" + validatorId.hashCode())
                .setName("Validator " + validatorId.substring(validatorId.length() - 1))
                .setStake(10000.0)
                .setRole(ValidatorRole.VALIDATOR)
                .setStatus(NodeStatus.ACTIVE)
                .setReputation(85.0 + Math.random() * 15)
                .build();
    }

    private ValidatorHealthMetrics buildHealthMetrics() {
        return ValidatorHealthMetrics.newBuilder()
                .setStatus(ValidatorHealthMetrics.HealthStatus.HEALTHY)
                .setHealthScore(90.0 + Math.random() * 10)
                .setSystemMetrics(SystemMetrics.newBuilder()
                        .setCpuUsagePercent(25.0 + Math.random() * 30)
                        .setMemoryUsedMb((long) (512 + Math.random() * 256))
                        .setMemoryTotalMb(2048)
                        .setDiskUsedGb((long) (50 + Math.random() * 20))
                        .setDiskTotalGb(200)
                        .build())
                .setNetworkLatencyMs(10.0 + Math.random() * 20)
                .setPeerConnections(12 + (int) (Math.random() * 8))
                .setNetworkBandwidthMbps(100.0 + Math.random() * 100)
                .setCurrentBlockHeight(1000000L + (long) (Math.random() * 1000))
                .setNetworkBlockHeight(1000500L)
                .setBlocksBehind(0)
                .setIsSynced(true)
                .setUptimePercent(99.9)
                .setLastHeartbeat(buildTimestamp())
                .setMissedHeartbeatsLastHour(0)
                .build();
    }

    private ValidatorPerformanceMetrics buildPerformanceMetrics() {
        return ValidatorPerformanceMetrics.newBuilder()
                .setBlocksProposed(125L + (long) (Math.random() * 25))
                .setBlocksAccepted(120L + (long) (Math.random() * 20))
                .setBlocksRejected((long) (Math.random() * 5))
                .setBlockAcceptanceRate(96.0 + Math.random() * 4)
                .setAverageBlockProposalTimeMs(50.0 + Math.random() * 30)
                .setVotesCast(500L + (long) (Math.random() * 100))
                .setVotesOnTime(480L + (long) (Math.random() * 80))
                .setVotesLate((long) (Math.random() * 15))
                .setVotesMissed((long) (Math.random() * 5))
                .setVotingParticipationRate(98.0 + Math.random() * 2)
                .setTransactionsProcessed(12500L + (long) (Math.random() * 2500))
                .setTransactionsValidated(12400L + (long) (Math.random() * 2400))
                .setTransactionsRejected((long) (Math.random() * 100))
                .setTransactionValidationRate(99.0 + Math.random())
                .setAverageTransactionProcessingTimeMs(2.0 + Math.random() * 3)
                .setConsensusRoundsParticipated(100 + (int) (Math.random() * 20))
                .setConsensusRoundsMissed((int) (Math.random() * 2))
                .setConsensusParticipationRate(99.0 + Math.random())
                .setOverallPerformanceScore(92.0 + Math.random() * 8)
                .setResponsivenessScore(90.0 + Math.random() * 10)
                .setReliabilityScore(95.0 + Math.random() * 5)
                .setMeasurementPeriodStart(buildTimestamp())
                .setMeasurementPeriodEnd(buildTimestamp())
                .build();
    }

    private ValidatorReputationInfo buildReputationInfo() {
        return ValidatorReputationInfo.newBuilder()
                .setCurrentReputation(85.0 + Math.random() * 15)
                .setReputationChange24H(Math.random() * 2 - 0.5)
                .setReputationRank(1 + (int) (Math.random() * 5))
                .setUptimeFactor(99.0 + Math.random())
                .setPerformanceFactor(92.0 + Math.random() * 8)
                .setReliabilityFactor(95.0 + Math.random() * 5)
                .setCommunityFactor(80.0 + Math.random() * 20)
                .build();
    }

    private BlockProposalActivity buildBlockProposalActivity() {
        return BlockProposalActivity.newBuilder()
                .setValidatorId(validatorIds.get((int) (Math.random() * validatorIds.size())))
                .setBlockHash("0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 64))
                .setBlockHeight(1000000L + (long) (Math.random() * 1000))
                .setTransactionCount(50 + (int) (Math.random() * 150))
                .setResult(BlockProposalActivity.ProposalResult.ACCEPTED)
                .setProposalTimeMs(45 + (long) (Math.random() * 30))
                .setProposedAt(buildTimestamp())
                .build();
    }

    private VotingActivity buildVotingActivity() {
        return VotingActivity.newBuilder()
                .setValidatorId(validatorIds.get((int) (Math.random() * validatorIds.size())))
                .setBlockHash("0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 64))
                .setVoteChoice(true)
                .setVoteLatencyMs(15 + (long) (Math.random() * 20))
                .setOnTime(true)
                .setVotedAt(buildTimestamp())
                .build();
    }

    private ValidatorRewardEvent buildValidatorRewardEvent() {
        return ValidatorRewardEvent.newBuilder()
                .setValidatorId(validatorIds.get((int) (Math.random() * validatorIds.size())))
                .setRewardType(ValidatorRewardEvent.RewardType.BLOCK_REWARD)
                .setRewardAmount(0.5 + Math.random() * 0.5)
                .setRewardCurrency("AUR")
                .setBlockHeight(1000000L + (long) (Math.random() * 1000))
                .setAwardedAt(buildTimestamp())
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
