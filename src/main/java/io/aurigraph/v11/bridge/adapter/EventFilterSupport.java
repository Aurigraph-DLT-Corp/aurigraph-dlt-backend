package io.aurigraph.v11.bridge.adapter;

import io.aurigraph.v11.bridge.ChainAdapter;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event Filter Support for Web3j Chain Adapter
 *
 * Provides functions to:
 * - Subscribe to blockchain events (logs)
 * - Filter events by contract address and topics
 * - Query historical events
 * - Parse event logs into ChainAdapter.BlockchainEvent objects
 *
 * Phase 11.2: Web3j Enhancement
 *
 * @author Claude Code
 * @version 1.0.0
 */
public class EventFilterSupport {

    private static final Logger logger = LoggerFactory.getLogger(EventFilterSupport.class);

    /**
     * Build EthFilter from ChainAdapter.EventFilter specification
     * Converts event signatures to topic hashes for filtering
     *
     * @param eventFilter ChainAdapter.EventFilter with event signatures
     * @return EthFilter with converted topics, or null if no valid signatures
     */
    public static EthFilter buildEthFilterFromSignatures(ChainAdapter.EventFilter eventFilter) {
        if (eventFilter.eventSignatures == null || eventFilter.eventSignatures.isEmpty()) {
            return null;
        }

        // Create filter with earliest and latest blocks
        EthFilter filter = new EthFilter("earliest", "latest");

        // Add event signatures (topics) if specified
        for (String signature : eventFilter.eventSignatures) {
            // Convert event signature to topic hash
            String topic = buildEventSignatureTopic(signature);
            if (topic != null) {
                filter = filter.addSingleTopic(topic);
            }
        }

        return filter;
    }

    /**
     * Query historical events from blockchain
     *
     * @param web3j Web3j instance
     * @param filter EthFilter with addresses and topics
     * @return List of blockchain events
     * @throws Exception if RPC call fails
     */
    public static List<ChainAdapter.BlockchainEvent> queryHistoricalEvents(Web3j web3j, EthFilter filter) throws Exception {
        List<ChainAdapter.BlockchainEvent> events = new ArrayList<>();

        try {
            EthLog ethLog = web3j.ethGetLogs(filter).send();

            if (ethLog.hasError()) {
                logger.error("Error querying logs: {}", ethLog.getError().getMessage());
                return events;
            }

            // Convert logs to BlockchainEvent objects
            for (EthLog.LogResult<?> logResult : ethLog.getLogs()) {
                if (logResult instanceof EthLog.LogObject) {
                    EthLog.LogObject logObject = (EthLog.LogObject) logResult;
                    ChainAdapter.BlockchainEvent event = convertLogToEvent(logObject);
                    events.add(event);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to query historical events", e);
            throw e;
        }

        return events;
    }

    /**
     * Convert Web3j Log to BlockchainEvent
     *
     * @param logObject Web3j log result
     * @return BlockchainEvent object
     */
    public static ChainAdapter.BlockchainEvent convertLogToEvent(EthLog.LogObject logObject) {
        ChainAdapter.BlockchainEvent event = new ChainAdapter.BlockchainEvent();

        // Set basic log properties
        event.contractAddress = logObject.getAddress();
        event.blockNumber = logObject.getBlockNumber().longValue();
        event.blockHash = logObject.getBlockHash();
        event.transactionHash = logObject.getTransactionHash();
        event.logIndex = logObject.getLogIndex().intValue();
        event.timestamp = System.currentTimeMillis(); // Would be retrieved from block header in production

        // Extract and set event signature
        event.eventSignature = extractEventName(logObject);

        // Determine event type based on signature
        if (event.eventSignature.equals("Transfer")) {
            event.eventType = ChainAdapter.EventType.TRANSFER;
        } else if (event.eventSignature.equals("Approval")) {
            event.eventType = ChainAdapter.EventType.APPROVAL;
        } else {
            event.eventType = ChainAdapter.EventType.CUSTOM;
        }

        // Store raw log data in eventData (List<Object> format)
        event.eventData = new ArrayList<>();
        if (logObject.getData() != null) {
            event.eventData.add(logObject.getData());
        }

        // Store indexed parameters in indexedData map
        event.indexedData = new HashMap<>();
        if (logObject.getTopics() != null && !logObject.getTopics().isEmpty()) {
            for (int i = 0; i < logObject.getTopics().size(); i++) {
                event.indexedData.put("topic" + i, logObject.getTopics().get(i));
            }
        }

        return event;
    }

    /**
     * Extract event name from log (typically from first topic/event signature)
     *
     * @param logObject Web3j log object
     * @return Extracted event name or "UnknownEvent"
     */
    private static String extractEventName(EthLog.LogObject logObject) {
        if (logObject.getTopics() != null && !logObject.getTopics().isEmpty()) {
            String eventSignature = logObject.getTopics().get(0);
            // Common event signatures
            if (eventSignature.startsWith("0xddf252ad")) {
                return "Transfer"; // ERC20/ERC721 Transfer event
            } else if (eventSignature.startsWith("0x8c5be1e5")) {
                return "Approval"; // ERC20 Approval event
            } else if (eventSignature.startsWith("0xf341246c")) {
                return "Swap"; // Uniswap Swap event
            } else if (eventSignature.startsWith("0x1c411e9a")) {
                return "LiquidityAdd"; // Uniswap Mint event
            }
            // Return first 16 chars of signature as fallback
            return "Event_" + eventSignature.substring(0, Math.min(16, eventSignature.length()));
        }
        return "UnknownEvent";
    }

    /**
     * Build topic filter for specific event
     * Standard ERC20 Transfer event: keccak256("Transfer(address,address,uint256)")
     *
     * @param eventSignature Event signature (e.g., "Transfer(address,address,uint256)")
     * @return Keccak-256 hash of event signature
     */
    public static String buildEventSignatureTopic(String eventSignature) {
        // In production, this would compute keccak256(eventSignature)
        // For now, return known event hashes
        switch (eventSignature) {
            case "Transfer(address,address,uint256)":
                return "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
            case "Approval(address,address,uint256)":
                return "0x8c5be1e5ebee5841edf4ba467f28b8ed87e0a6ca1d8eca67ad99476c42dc3e26";
            case "Swap(address,uint256,uint256,uint256,uint256,address)":
                return "0xd78ad95fa46c994b6551d0da85fc275fe1d04529b4f0acb7f054eadf3e69a5d0";
            default:
                logger.warn("Unknown event signature: {}", eventSignature);
                return null;
        }
    }

    /**
     * Create filter for ERC20 Transfer events
     *
     * @param tokenAddress ERC20 token contract address
     * @return Keccak-256 hash of ERC20 Transfer event signature
     */
    public static String getERC20TransferTopic(String tokenAddress) {
        // Standard ERC20 Transfer(address,address,uint256) event topic
        return "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
    }

    /**
     * Create filter for Swap events
     *
     * @param poolAddress Pool contract address
     * @return Keccak-256 hash of Swap event signature
     */
    public static String getSwapEventTopic(String poolAddress) {
        // Standard Uniswap Swap event topic
        return "0xd78ad95fa46c994b6551d0da85fc275fe1d04529b4f0acb7f054eadf3e69a5d0";
    }
}
