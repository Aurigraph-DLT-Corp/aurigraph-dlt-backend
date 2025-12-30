package io.aurigraph.v11.contracts.composite;

/**
 * Token Type Enum
 *
 * Defines the token standards supported by Aurigraph V11 platform.
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 12
 */
public enum TokenType {
    /**
     * ERC20 - Fungible tokens
     * Standard for fungible digital assets (coins, utility tokens, etc.)
     */
    ERC20,

    /**
     * ERC721 - Non-Fungible Tokens (NFTs)
     * Standard for unique digital assets with distinct properties
     */
    ERC721,

    /**
     * ERC1155 - Multi-Token Standard
     * Supports both fungible and non-fungible tokens in a single contract
     */
    ERC1155,

    /**
     * RWA - Real-World Asset Token
     * Specialized token type for tokenized real-world assets
     */
    RWA,

    /**
     * GOVERNANCE - Governance Token
     * Tokens used for platform governance and voting
     */
    GOVERNANCE,

    /**
     * UTILITY - Utility Token
     * Platform utility tokens for service payments
     */
    UTILITY
}