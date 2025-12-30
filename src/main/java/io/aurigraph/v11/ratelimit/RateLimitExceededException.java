package io.aurigraph.v11.ratelimit;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Exception thrown when rate limit is exceeded
 *
 * Converts to HTTP 429 Too Many Requests by default
 *
 * @author DevOps & Security Team
 * @since V11.3.2
 */
public class RateLimitExceededException extends WebApplicationException {

    public RateLimitExceededException(String message, int statusCode) {
        super(message, Response.status(statusCode)
            .entity(new ErrorResponse(
                "Rate Limit Exceeded",
                message,
                "Please try again later or contact support for higher rate limits"
            ))
            .header("Retry-After", "60") // Suggest retry after 60 seconds
            .header("X-RateLimit-Reset", System.currentTimeMillis() + 60000)
            .build());
    }

    public static class ErrorResponse {
        public final String error;
        public final String message;
        public final String suggestion;

        public ErrorResponse(String error, String message, String suggestion) {
            this.error = error;
            this.message = message;
            this.suggestion = suggestion;
        }
    }
}
