package io.aurigraph.v11.bridge;

/**
 * Bridge Event Listener - Functional interface for bridge event callbacks
 */
@FunctionalInterface
public interface BridgeEventListener {
    void onEvent(BridgeEvent event);
}
