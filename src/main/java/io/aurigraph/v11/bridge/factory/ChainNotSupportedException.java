package io.aurigraph.v11.bridge.factory;

/**
 * Exception thrown when attempting to use an unsupported blockchain
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 1.0.0
 */
public class ChainNotSupportedException extends Exception {

    public ChainNotSupportedException(String message) {
        super(message);
    }

    public ChainNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChainNotSupportedException(Throwable cause) {
        super(cause);
    }
}
