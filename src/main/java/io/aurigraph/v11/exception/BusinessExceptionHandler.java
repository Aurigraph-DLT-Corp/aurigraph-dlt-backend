package io.aurigraph.v11.exception;

import io.aurigraph.v11.oracle.ConsensusNotReachedException;
import io.aurigraph.v11.oracle.InsufficientOraclesException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Business Exception Handler
 * Handles domain-specific exceptions with appropriate HTTP status codes
 *
 * @author Aurigraph Production Readiness Agent
 * @version 11.0.0
 */
@Provider
public class BusinessExceptionHandler implements ExceptionMapper<RuntimeException> {

    private static final Logger LOG = Logger.getLogger(BusinessExceptionHandler.class);

    @Override
    public Response toResponse(RuntimeException exception) {
        String correlationId = UUID.randomUUID().toString();

        // Log exception with correlation ID
        LOG.warnf(exception, "[CorrelationId: %s] Business exception occurred: %s",
            correlationId, exception.getClass().getSimpleName());

        // Handle oracle-specific exceptions
        if (exception instanceof InsufficientOraclesException) {
            InsufficientOraclesException ex = (InsufficientOraclesException) exception;
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new ErrorResponse(
                    Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
                    "Insufficient oracles available",
                    ex.getMessage(),
                    correlationId,
                    Instant.now()
                ))
                .build();
        }

        if (exception instanceof ConsensusNotReachedException) {
            ConsensusNotReachedException ex = (ConsensusNotReachedException) exception;
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse(
                    Response.Status.CONFLICT.getStatusCode(),
                    "Oracle consensus not reached",
                    ex.getMessage(),
                    correlationId,
                    Instant.now()
                ))
                .build();
        }

        // Default to 500 for unhandled runtime exceptions
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Business operation failed",
                "An error occurred during operation. Correlation ID: " + correlationId,
                correlationId,
                Instant.now()
            ))
            .build();
    }

    public record ErrorResponse(
        int status,
        String error,
        String message,
        String correlationId,
        Instant timestamp
    ) {}
}
