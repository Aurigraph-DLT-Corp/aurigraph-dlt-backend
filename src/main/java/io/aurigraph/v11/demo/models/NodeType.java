package io.aurigraph.v11.demo.models;

/**
 * Enumeration of node types supported in the Aurigraph V11 network architecture.
 *
 * <p>The Aurigraph V11 platform supports four distinct node types, each with specific
 * responsibilities in the distributed blockchain network:
 *
 * <ul>
 *   <li><b>CHANNEL</b> - Manages multi-channel data flows and participant coordination</li>
 *   <li><b>VALIDATOR</b> - Participates in HyperRAFT++ consensus and block validation</li>
 *   <li><b>BUSINESS</b> - Executes business logic and smart contract operations</li>
 *   <li><b>API_INTEGRATION</b> - Integrates with external APIs and data sources</li>
 * </ul>
 *
 * <p>Each node type is optimized for its specific role and can be horizontally scaled
 * to meet performance requirements. The network targets 2M+ TPS through coordinated
 * operation of multiple node types.
 *
 * @author Aurigraph V11 Platform
 * @version 11.0.0
 * @since 2025-10-11
 * @see io.aurigraph.v11.demo.nodes.Node
 * @see io.aurigraph.v11.demo.nodes.AbstractNode
 */
public enum NodeType {

    /**
     * Channel Node - Coordinates multi-channel data flows and participant management.
     *
     * <p><b>Responsibilities:</b>
     * <ul>
     *   <li>Manage channel lifecycle (create, update, close)</li>
     *   <li>Track channel participants and permissions</li>
     *   <li>Route messages between channel members</li>
     *   <li>Maintain channel state consistency</li>
     *   <li>Handle off-chain data when appropriate</li>
     * </ul>
     *
     * <p><b>Performance Targets:</b>
     * <ul>
     *   <li>Channel creation: &lt;10ms</li>
     *   <li>Message routing: &lt;5ms</li>
     *   <li>Concurrent channels: 10,000+ per node</li>
     *   <li>Throughput: 500K messages/sec per node</li>
     * </ul>
     */
    CHANNEL("Channel Node", "Manages multi-channel data flows and participant coordination"),

    /**
     * Validator Node - Participates in HyperRAFT++ consensus and validates transactions.
     *
     * <p><b>Responsibilities:</b>
     * <ul>
     *   <li>Participate in consensus rounds</li>
     *   <li>Validate transaction batches</li>
     *   <li>Propose and vote on blocks</li>
     *   <li>Maintain blockchain state</li>
     *   <li>Execute HyperRAFT++ consensus algorithm</li>
     * </ul>
     *
     * <p><b>Performance Targets:</b>
     * <ul>
     *   <li>Block proposal time: &lt;500ms</li>
     *   <li>Consensus finality: &lt;1s</li>
     *   <li>TPS per validator: 200K+</li>
     *   <li>Network TPS: 2M+ (with 10+ validators)</li>
     * </ul>
     */
    VALIDATOR("Validator Node", "Participates in HyperRAFT++ consensus and validates transactions"),

    /**
     * Business Node - Executes business logic and smart contract operations.
     *
     * <p><b>Responsibilities:</b>
     * <ul>
     *   <li>Process transaction requests</li>
     *   <li>Execute smart contracts (Ricardian contracts)</li>
     *   <li>Handle business logic workflows</li>
     *   <li>Integrate with enterprise systems</li>
     *   <li>Track business metrics</li>
     * </ul>
     *
     * <p><b>Performance Targets:</b>
     * <ul>
     *   <li>Transaction execution: &lt;20ms</li>
     *   <li>Contract execution: &lt;100ms</li>
     *   <li>Throughput: 100K transactions/sec per node</li>
     *   <li>Contract calls: 50K calls/sec per node</li>
     * </ul>
     */
    BUSINESS("Business Node", "Executes business logic and smart contract operations"),

    /**
     * API Integration Node - Integrates with external APIs and data sources.
     *
     * <p><b>Responsibilities:</b>
     * <ul>
     *   <li>Connect to external APIs (Alpaca, Weather, etc.)</li>
     *   <li>Fetch and validate external data</li>
     *   <li>Cache external data</li>
     *   <li>Provide oracle services</li>
     *   <li>Handle API rate limiting</li>
     * </ul>
     *
     * <p><b>Performance Targets:</b>
     * <ul>
     *   <li>API call latency: &lt;100ms (with caching)</li>
     *   <li>Cache hit rate: &gt;90%</li>
     *   <li>Data freshness: &lt;5s for critical data</li>
     *   <li>Throughput: 10K external API calls/sec</li>
     * </ul>
     */
    API_INTEGRATION("API Integration Node", "Integrates with external APIs and data sources");

    private final String displayName;
    private final String description;

    /**
     * Constructs a NodeType with the specified display name and description.
     *
     * @param displayName the human-readable name of the node type
     * @param description a brief description of the node type's purpose
     */
    NodeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human-readable display name for this node type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the description of this node type's responsibilities and purpose.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this node type participates in consensus operations.
     *
     * @return true if this is a VALIDATOR node, false otherwise
     */
    public boolean isConsensusNode() {
        return this == VALIDATOR;
    }

    /**
     * Checks if this node type handles external integrations.
     *
     * @return true if this is an API_INTEGRATION node, false otherwise
     */
    public boolean isExternalIntegration() {
        return this == API_INTEGRATION;
    }

    /**
     * Checks if this node type executes business logic.
     *
     * @return true if this is a BUSINESS node, false otherwise
     */
    public boolean isBusinessNode() {
        return this == BUSINESS;
    }

    /**
     * Checks if this node type manages data channels.
     *
     * @return true if this is a CHANNEL node, false otherwise
     */
    public boolean isChannelNode() {
        return this == CHANNEL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
