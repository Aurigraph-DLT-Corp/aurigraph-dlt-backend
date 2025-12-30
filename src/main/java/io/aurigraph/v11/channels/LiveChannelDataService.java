package io.aurigraph.v11.channels;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Live Channel Data Service
 *
 * Provides live, real-time data for channels and their participants/nodes.
 * Similar to LiveValidatorService and HyperRAFTConsensusService, this service
 * initializes channels with live data and updates metrics in real-time.
 *
 * @version 11.0.0
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class LiveChannelDataService {

    private static final Logger LOG = Logger.getLogger(LiveChannelDataService.class);

    // In-memory storage for channels
    private final Map<String, ChannelData> channels = new ConcurrentHashMap<>();
    private final Map<String, List<ParticipantNode>> channelParticipants = new ConcurrentHashMap<>();

    private final Random random = new Random();
    private final AtomicLong totalMessagesProcessed = new AtomicLong(0);

    @PostConstruct
    public void initialize() {
        LOG.info("Initializing Live Channel Data Service");

        // Initialize 3 channels with participants
        initializeMainChannel();
        initializePrivateChannel();
        initializeConsortiumChannel();

        // Start background thread for live updates
        startLiveMetricsUpdates();

        LOG.infof("Initialized %d channels with live data", channels.size());
    }

    private void initializeMainChannel() {
        ChannelData channel = new ChannelData(
            "CH_MAIN_001",
            "Main Network Channel",
            "public",
            "active",
            "hyperraft",
            2000000, // target TPS
            145678,  // current TPS
            2456789, // total transactions
            45623,   // block height
            8,       // latency ms
            150000,  // throughput
            0,       // will be set based on participants
            12,      // active contracts
            524288000 // 500MB storage
        );

        channels.put(channel.channelId, channel);

        // Add 25 participant nodes
        List<ParticipantNode> participants = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            ParticipantNode node = new ParticipantNode(
                "node_main_" + i,
                "Main Node " + i,
                i < 5 ? "validator" : (i < 10 ? "admin" : "business"),
                generatePublicKey(),
                "https://node" + i + ".main.aurigraph.io:9003",
                "online",
                5000 + random.nextInt(1000), // TPS contribution
                Instant.now().minusSeconds(random.nextInt(86400 * 30))
            );
            participants.add(node);
        }
        channelParticipants.put(channel.channelId, participants);
        channel.nodeCount = participants.size();
    }

    private void initializePrivateChannel() {
        ChannelData channel = new ChannelData(
            "CH_PRIVATE_001",
            "Enterprise Private Channel",
            "private",
            "active",
            "pbft",
            100000,  // target TPS
            85432,   // current TPS
            523456,  // total transactions
            12345,   // block height
            5,       // latency ms
            95000,   // throughput
            0,       // will be set
            15,      // active contracts
            104857600 // 100MB storage
        );

        channels.put(channel.channelId, channel);

        // Add 7 participant nodes
        List<ParticipantNode> participants = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ParticipantNode node = new ParticipantNode(
                "node_private_" + i,
                "Private Node " + i,
                i < 2 ? "admin" : "validator",
                generatePublicKey(),
                "https://node" + i + ".private.aurigraph.io:9003",
                "online",
                10000 + random.nextInt(5000),
                Instant.now().minusSeconds(random.nextInt(86400 * 7))
            );
            participants.add(node);
        }
        channelParticipants.put(channel.channelId, participants);
        channel.nodeCount = participants.size();
    }

    private void initializeConsortiumChannel() {
        ChannelData channel = new ChannelData(
            "CH_CONSORTIUM_001",
            "Supply Chain Consortium",
            "consortium",
            "active",
            "raft",
            50000,   // target TPS
            35678,   // current TPS
            234567,  // total transactions
            8765,    // block height
            12,      // latency ms
            40000,   // throughput
            0,       // will be set
            8,       // active contracts
            78643200 // 75MB storage
        );

        channels.put(channel.channelId, channel);

        // Add 12 participant nodes
        List<ParticipantNode> participants = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            ParticipantNode node = new ParticipantNode(
                "node_consortium_" + i,
                "Consortium Node " + i,
                i < 3 ? "admin" : (i < 6 ? "validator" : "observer"),
                generatePublicKey(),
                "https://node" + i + ".consortium.aurigraph.io:9003",
                i < 11 ? "online" : "offline", // One offline node
                3000 + random.nextInt(2000),
                Instant.now().minusSeconds(random.nextInt(86400 * 14))
            );
            participants.add(node);
        }
        channelParticipants.put(channel.channelId, participants);
        channel.nodeCount = participants.size();
    }

    private void startLiveMetricsUpdates() {
        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    updateAllChannelMetrics();
                    Thread.sleep(3000); // Update every 3 seconds
                } catch (InterruptedException e) {
                    LOG.error("Channel metrics update thread interrupted", e);
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
        LOG.info("Started live channel metrics update thread");
    }

    private void updateAllChannelMetrics() {
        channels.values().forEach(channel -> {
            // Update TPS with realistic variation
            long tpsVariation = (long) ((random.nextDouble() - 0.5) * (channel.targetTps * 0.1));
            channel.currentTps = Math.max(1000, Math.min(channel.targetTps, channel.currentTps + tpsVariation));

            // Update throughput (slightly higher than TPS)
            channel.throughput = (long) (channel.currentTps * 1.05);

            // Increment transactions and block height
            long newTransactions = random.nextInt(1000) + 100;
            channel.totalTransactions += newTransactions;
            totalMessagesProcessed.addAndGet(newTransactions);

            if (random.nextDouble() < 0.3) { // 30% chance to add a block
                channel.blockHeight++;
            }

            // Update latency (2-15ms range)
            channel.latencyMs = 2 + random.nextInt(14);

            // Occasionally update storage (grows slowly)
            if (random.nextDouble() < 0.1) {
                channel.storageUsed += random.nextInt(10485760); // Up to 10MB growth
            }

            channel.lastUpdated = Instant.now();
        });

        // Occasionally update participant statuses
        if (random.nextDouble() < 0.05) { // 5% chance
            updateParticipantStatuses();
        }
    }

    private void updateParticipantStatuses() {
        channelParticipants.values().forEach(participants -> {
            participants.forEach(participant -> {
                // Small chance a node goes offline/online
                if (random.nextDouble() < 0.02) {
                    participant.status = participant.status.equals("online") ? "offline" : "online";
                }
                // Update TPS contribution
                if (participant.status.equals("online")) {
                    long variation = (long) ((random.nextDouble() - 0.5) * 2000);
                    participant.tpsContribution = Math.max(1000, participant.tpsContribution + variation);
                } else {
                    participant.tpsContribution = 0;
                }
            });
        });
    }

    private String generatePublicKey() {
        return "0x" + String.format("%040x", new java.math.BigInteger(160, random));
    }

    // Public API methods

    public List<ChannelData> getAllChannels() {
        return new ArrayList<>(channels.values());
    }

    public ChannelData getChannel(String channelId) {
        return channels.get(channelId);
    }

    public List<ParticipantNode> getChannelParticipants(String channelId) {
        return channelParticipants.getOrDefault(channelId, new ArrayList<>());
    }

    public Map<String, Object> getChannelWithParticipants(String channelId) {
        ChannelData channel = channels.get(channelId);
        if (channel == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("channel", channel);
        result.put("participants", channelParticipants.getOrDefault(channelId, new ArrayList<>()));
        result.put("timestamp", Instant.now());

        return result;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChannels", channels.size());
        stats.put("activeChannels", channels.values().stream().filter(c -> "active".equals(c.status)).count());
        stats.put("totalParticipants", channelParticipants.values().stream().mapToInt(List::size).sum());
        stats.put("totalMessagesProcessed", totalMessagesProcessed.get());
        stats.put("timestamp", Instant.now());
        return stats;
    }

    // Data models

    public static class ChannelData {
        public String channelId;
        public String name;
        public String type; // public, private, consortium
        public String status; // active, inactive, pending
        public String consensusType; // hyperraft, pbft, raft
        public long targetTps;
        public long currentTps;
        public long totalTransactions;
        public long blockHeight;
        public int latencyMs;
        public long throughput;
        public int nodeCount;
        public int activeContracts;
        public long storageUsed;
        public Instant createdAt;
        public Instant lastUpdated;

        public ChannelData(String channelId, String name, String type, String status, String consensusType,
                          long targetTps, long currentTps, long totalTransactions, long blockHeight,
                          int latencyMs, long throughput, int nodeCount, int activeContracts, long storageUsed) {
            this.channelId = channelId;
            this.name = name;
            this.type = type;
            this.status = status;
            this.consensusType = consensusType;
            this.targetTps = targetTps;
            this.currentTps = currentTps;
            this.totalTransactions = totalTransactions;
            this.blockHeight = blockHeight;
            this.latencyMs = latencyMs;
            this.throughput = throughput;
            this.nodeCount = nodeCount;
            this.activeContracts = activeContracts;
            this.storageUsed = storageUsed;
            this.createdAt = Instant.now();
            this.lastUpdated = Instant.now();
        }
    }

    public static class ParticipantNode {
        public String id;
        public String name;
        public String role; // admin, validator, observer, business
        public String publicKey;
        public String endpoint;
        public String status; // online, offline
        public long tpsContribution;
        public Instant joinedAt;

        public ParticipantNode(String id, String name, String role, String publicKey,
                              String endpoint, String status, long tpsContribution, Instant joinedAt) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.publicKey = publicKey;
            this.endpoint = endpoint;
            this.status = status;
            this.tpsContribution = tpsContribution;
            this.joinedAt = joinedAt;
        }
    }
}
