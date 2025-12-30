package io.aurigraph.v11.demo.state;

/**
 * NodeType - Enumeration of node types in the Aurigraph V11 network
 *
 * Based on the architecture design, there are four distinct node types:
 * - CHANNEL: Multi-channel data flow coordination
 * - VALIDATOR: HyperRAFT++ consensus participation
 * - BUSINESS: Business logic and smart contract execution
 * - API_INTEGRATION: External data source integration
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-216
 */
public enum NodeType {
    /**
     * Channel Node - Manages multi-channel data flows and participant routing
     * Performance: 500K msg/sec, 10K concurrent channels
     */
    CHANNEL("Channel Node", "Manages data flow coordination"),

    /**
     * Validator Node - Participates in HyperRAFT++ consensus
     * Performance: 200K+ TPS per validator
     */
    VALIDATOR("Validator Node", "Participates in consensus"),

    /**
     * Business Node - Executes business logic and smart contracts
     * Performance: 100K tx/sec per node
     */
    BUSINESS("Business Node", "Executes business logic"),

    /**
     * API Integration Node - Integrates with external APIs (Alpaca, Weather, etc.)
     * Performance: 10K API calls/sec
     */
    API_INTEGRATION("API Integration Node", "External API integration");

    private final String displayName;
    private final String description;

    NodeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
