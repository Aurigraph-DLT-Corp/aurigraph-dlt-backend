package io.aurigraph.v11.demo.models;

/**
 * Enumeration of possible states for a channel in the Aurigraph V11 network.
 *
 * <p>Channel lifecycle progression:
 * <pre>
 *   CREATED → ACTIVE ⇄ SUSPENDED → CLOSED
 *                ↓
 *              ERROR
 * </pre>
 * </p>
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since 2025-10-11
 */
public enum ChannelState {
    /**
     * Channel has been created but not yet activated.
     */
    CREATED,

    /**
     * Channel is active and processing messages.
     */
    ACTIVE,

    /**
     * Channel is temporarily suspended but can be reactivated.
     */
    SUSPENDED,

    /**
     * Channel has been permanently closed.
     */
    CLOSED,

    /**
     * Channel has encountered an error and requires intervention.
     */
    ERROR
}
