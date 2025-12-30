package io.aurigraph.v11.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Global Exception Handler
 * Provides centralized exception handling for all REST endpoints
 *
 * Features:
 * - Prevents stack trace leakage to clients
 * - Structured error responses with correlation IDs
 * - Proper HTTP status codes
 * - Security-conscious error messages
 *
 * @author Aurigraph Production Readiness Agent
 * @version 11.0.0
 */
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Override
    public Response toResponse(Exception exception) {
        String correlationId = UUID.randomUUID().toString();

        // Log full exception details server-side with correlation ID
        LOG.errorf(exception, "[CorrelationId: %s] Unhandled exception occurred", correlationId);

        // Determine appropriate HTTP status and client message
        ErrorResponse errorResponse = buildErrorResponse(exception, correlationId);

        return Response.status(errorResponse.httpStatus())
            .entity(errorResponse)
            .build();
    }

    private ErrorResponse buildErrorResponse(Exception exception, String correlationId) {
        // Handle specific exception types
        if (exception instanceof IllegalArgumentException) {
            return new ErrorResponse(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Invalid request parameters",
                exception.getMessage(),
                correlationId,
                Instant.now()
            );
        }

        if (exception instanceof IllegalStateException) {
            return new ErrorResponse(
                Response.Status.CONFLICT.getStatusCode(),
                "Operation cannot be completed in current state",
                sanitizeErrorMessage(exception.getMessage()),
                correlationId,
                Instant.now()
            );
        }

        if (exception instanceof SecurityException) {
            return new ErrorResponse(
                Response.Status.FORBIDDEN.getStatusCode(),
                "Access denied",
                "Insufficient permissions to perform this operation",
                correlationId,
                Instant.now()
            );
        }

        // Default to 500 Internal Server Error with generic message
        return new ErrorResponse(
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "Internal server error",
            "An unexpected error occurred. Please contact support with correlation ID: " + correlationId,
            correlationId,
            Instant.now()
        );
    }

    /**
     * Sanitize error messages to prevent information leakage
     * Removes stack traces, file paths, and sensitive data
     */
    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "Operation failed";
        }

        // Remove potential file paths
        String sanitized = message.replaceAll("(/[\\w./]+)", "[path]");

        // Remove stack trace patterns
        sanitized = sanitized.replaceAll("at [\\w.]+\\([^)]+\\)", "");

        // Limit length to prevent verbose error messages
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 197) + "...";
        }

        return sanitized;
    }

    /**
     * Structured error response DTO
     */
    public record ErrorResponse(
        int status,
        String error,
        String message,
        String correlationId,
        Instant timestamp
    ) {
        public int httpStatus() {
            return status;
        }
    }
}
