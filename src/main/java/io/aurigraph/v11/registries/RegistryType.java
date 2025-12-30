package io.aurigraph.v11.registries;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry Type Enumeration
 *
 * Defines all supported registry types in the Aurigraph V11 platform.
 * Used for filtering and categorizing registry entries across different subsystems.
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
public enum RegistryType {
    /**
     * Smart Contract Registry
     * Tracks deployed ActiveContracts, Ricardian contracts, and contract templates
     */
    SMART_CONTRACT("smart-contract", "Smart Contracts"),

    /**
     * Token Registry
     * Tracks ERC20, ERC721, ERC1155 tokens and RWA-backed tokens
     */
    TOKEN("token", "Tokens"),

    /**
     * Real-World Asset Token Registry
     * Tracks tokenized real-world assets (RWATs)
     */
    RWA("rwa", "Real-World Assets"),

    /**
     * Merkle Tree Registry
     * Tracks cryptographic proofs and Merkle tree states
     */
    MERKLE_TREE("merkle-tree", "Merkle Trees"),

    /**
     * Compliance Registry
     * Tracks compliance attestations, certifications, and audit trails
     */
    COMPLIANCE("compliance", "Compliance");

    private final String id;
    private final String displayName;

    // Static map for reverse lookups
    private static final Map<String, RegistryType> ID_MAP = new HashMap<>();

    static {
        for (RegistryType type : RegistryType.values()) {
            ID_MAP.put(type.id, type);
        }
    }

    RegistryType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Get the registry type identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Get the human-readable display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convert string identifier to RegistryType enum
     *
     * @param typeId the registry type identifier (e.g., "smart-contract")
     * @return the corresponding RegistryType
     * @throws IllegalArgumentException if the type ID is not recognized
     */
    public static RegistryType fromString(String typeId) {
        if (typeId == null || typeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Registry type ID cannot be null or empty");
        }

        RegistryType type = ID_MAP.get(typeId.toLowerCase());
        if (type == null) {
            throw new IllegalArgumentException(
                String.format("Unknown registry type: %s. Valid types are: %s",
                    typeId, String.join(", ", ID_MAP.keySet()))
            );
        }
        return type;
    }

    /**
     * Check if a type ID is valid
     */
    public static boolean isValid(String typeId) {
        return typeId != null && ID_MAP.containsKey(typeId.toLowerCase());
    }

    /**
     * Get all valid type IDs
     */
    public static String[] getAllIds() {
        return ID_MAP.keySet().toArray(new String[0]);
    }

    @Override
    public String toString() {
        return id;
    }
}
