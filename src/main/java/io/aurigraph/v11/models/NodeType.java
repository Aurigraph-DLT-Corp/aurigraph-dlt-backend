package io.aurigraph.v11.models;

/**
 * Node Type Enum
 *
 * Represents the different types of nodes in the Aurigraph V11 network.
 *
 * Part of Sprint 9 - Story 3 (AV11-053)
 *
 * @author Claude Code
 * @version 11.0.0
 * @since Sprint 9
 */
public enum NodeType {
    /**
     * Full node that stores complete blockchain data
     */
    FULL_NODE("Full Node", "Stores complete blockchain data"),

    /**
     * Validator node that participates in consensus
     */
    VALIDATOR("Validator", "Participates in block validation and consensus"),

    /**
     * Light client that stores minimal blockchain data
     */
    LIGHT_CLIENT("Light Client", "Stores only essential blockchain data"),

    /**
     * Archive node that stores all historical states
     */
    ARCHIVE("Archive Node", "Stores complete historical blockchain data"),

    /**
     * Boot node used for network discovery
     */
    BOOT_NODE("Boot Node", "Helps new nodes discover the network"),

    /**
     * RPC node that provides API access
     */
    RPC_NODE("RPC Node", "Provides JSON-RPC API access");

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

    /**
     * Check if node type can validate blocks
     */
    public boolean canValidate() {
        return this == VALIDATOR;
    }

    /**
     * Check if node type stores full blockchain
     */
    public boolean storesFullChain() {
        return this == FULL_NODE || this == VALIDATOR || this == ARCHIVE || this == RPC_NODE;
    }
}
