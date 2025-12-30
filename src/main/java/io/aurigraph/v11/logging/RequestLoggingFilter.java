package io.aurigraph.v11.logging;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * HTTP Request/Response Logging Filter
 *
 * Automatically logs all HTTP requests and responses with correlation IDs,
 * timing information, and structured data for ELK stack analysis.
 *
 * Attached to all REST endpoints via @Provider annotation.
 */
@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_START_TIME = "request.start.time";
    private static final String CORRELATION_ID = "request.correlation.id";

    @Inject
    LoggingService loggingService;

    /**
     * Log incoming HTTP request
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        long startTime = System.currentTimeMillis();
        requestContext.setProperty(REQUEST_START_TIME, startTime);

        // Generate or extract correlation ID
        String correlationId = requestContext.getHeaderString("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = loggingService.generateCorrelationId();
        }

        requestContext.setProperty(CORRELATION_ID, correlationId);
        loggingService.setCorrelationId(correlationId);

        // Log request
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        String remoteAddr = requestContext.getHeaderString("X-Forwarded-For");
        if (remoteAddr == null) {
            remoteAddr = "unknown";
        }

        LOG.infof("-> %s %s [%s] from %s", method, path, correlationId, remoteAddr);
    }

    /**
     * Log outgoing HTTP response
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                      ContainerResponseContext responseContext) throws IOException {
        Long startTime = (Long) requestContext.getProperty(REQUEST_START_TIME);
        String correlationId = (String) requestContext.getProperty(CORRELATION_ID);

        if (startTime != null && correlationId != null) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            String method = requestContext.getMethod();
            String path = requestContext.getUriInfo().getPath();
            int statusCode = responseContext.getStatus();

            // Add correlation ID to response headers
            responseContext.getHeaders().add("X-Correlation-ID", correlationId);

            // Log response with structured data
            loggingService.logHttpRequest(method, path, statusCode, duration, correlationId);

            // Log warnings for slow requests (>1s)
            if (duration > 1000) {
                LOG.warnf("SLOW REQUEST: %s %s took %dms [%s]", method, path, duration, correlationId);
            }

            // Log errors for 4xx and 5xx responses
            if (statusCode >= 400) {
                LOG.warnf("ERROR RESPONSE: %s %s -> %d [%s]", method, path, statusCode, correlationId);
            }
        }

        // Clear correlation ID from MDC
        loggingService.clearCorrelationId();
    }
}
