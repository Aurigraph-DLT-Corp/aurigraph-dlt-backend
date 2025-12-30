package io.aurigraph.v11.models;

/**
 * Transaction Type Enumeration
 *
 * Defines the different types of transactions supported by Aurigraph V11.
 *
 * @author Claude Code
 * @version 11.0.0
 * @since Sprint 9
 */
public enum TransactionType {
    /**
     * Standard token transfer
     */
    TRANSFER,

    /**
     * Smart contract deployment
     */
    CONTRACT_DEPLOY,

    /**
     * Smart contract invocation
     */
    CONTRACT_INVOKE,

    /**
     * Asset tokenization transaction
     */
    TOKENIZATION,

    /**
     * Cross-chain bridge transaction
     */
    BRIDGE,

    /**
     * Staking transaction
     */
    STAKE,

    /**
     * Unstaking transaction
     */
    UNSTAKE,

    /**
     * Governance vote transaction
     */
    VOTE,

    /**
     * System/administrative transaction
     */
    SYSTEM
}
