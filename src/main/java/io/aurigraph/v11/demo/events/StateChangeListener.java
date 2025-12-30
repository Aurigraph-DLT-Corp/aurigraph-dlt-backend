package io.aurigraph.v11.demo.events;

/**
 * StateChangeListener - Functional interface for state change event listeners
 *
 * Implementations of this interface can be registered with the NodeStateManager
 * to receive notifications when node states change.
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-216
 */
@FunctionalInterface
public interface StateChangeListener {

    /**
     * Called when a node state changes
     *
     * @param event the state change event
     */
    void onStateChange(StateChangeEvent event);
}
