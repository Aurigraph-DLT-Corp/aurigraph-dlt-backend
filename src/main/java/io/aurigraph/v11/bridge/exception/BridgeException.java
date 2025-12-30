package io.aurigraph.v11.bridge.exception;

/**
 * Exception thrown during cross-chain bridge operations
 * Used for adapter creation, configuration, and transaction failures
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 1.0.0
 */
public class BridgeException extends Exception {

    public BridgeException(String message) {
        super(message);
    }

    public BridgeException(String message, Throwable cause) {
        super(message, cause);
    }

    public BridgeException(Throwable cause) {
        super(cause);
    }
}
