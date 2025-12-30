package io.aurigraph.v11.logging;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.util.UUID;

/**
 * Correlation ID Filter
 * Automatically adds correlation IDs to all requests for distributed tracing
 *
 * Features:
 * - Generates or extracts correlation IDs from headers
 * - Adds to MDC for logging
 * - Propagates in response headers
 * - Cleans up after request completion
 *
 * @author Aurigraph Production Readiness Agent
 * @version 11.0.0
 */
@Provider
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Extract or generate correlation ID
        String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Store in MDC for logging
        MDC.put(MDC_CORRELATION_ID_KEY, correlationId);

        // Store in request context for access by handlers
        requestContext.setProperty(MDC_CORRELATION_ID_KEY, correlationId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Get correlation ID from request context
        String correlationId = (String) requestContext.getProperty(MDC_CORRELATION_ID_KEY);

        // Add to response headers
        if (correlationId != null) {
            responseContext.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        }

        // Clean up MDC
        MDC.remove(MDC_CORRELATION_ID_KEY);
    }
}
