package io.aurigraph.v11.ratelimit;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate Limiting Annotation for Public Endpoints
 *
 * Usage:
 * <pre>
 * {@code
 * @GET
 * @RateLimited(requestsPerMinute = 100)
 * public Response publicEndpoint() { ... }
 * }
 * </pre>
 *
 * Default: 1000 requests per minute per IP address
 *
 * @author DevOps & Security Team
 * @since V11.3.2
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RateLimited {

    /**
     * Maximum requests allowed per minute per IP address
     * @return requests per minute limit
     */
    int requestsPerMinute() default 1000;

    /**
     * Whether to return HTTP 429 (Too Many Requests) or HTTP 503 (Service Unavailable)
     * @return HTTP status code for rate limit exceeded
     */
    int statusCode() default 429;
}
