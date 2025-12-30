package io.aurigraph.v11.logging;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Performance Logging Interceptor
 * Automatically logs method execution time for performance monitoring
 *
 * Usage: Add @Timed annotation to methods or classes
 *
 * @author Aurigraph Production Readiness Agent
 * @version 11.0.0
 */
@Interceptor
@Timed
@Priority(Interceptor.Priority.APPLICATION)
public class PerformanceLoggingInterceptor {

    private static final Logger LOG = Logger.getLogger(PerformanceLoggingInterceptor.class);

    @AroundInvoke
    public Object logExecutionTime(InvocationContext context) throws Exception {
        String methodName = context.getMethod().getDeclaringClass().getSimpleName() +
            "." + context.getMethod().getName();

        long startTime = System.nanoTime();

        try {
            // Store method name in MDC for structured logging
            MDC.put("method", methodName);

            Object result = context.proceed();

            long executionTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds

            // Log performance metrics
            LOG.infof("Method %s executed in %d ms", methodName, executionTime);

            // Log structured metrics for monitoring
            if (executionTime > 1000) {
                LOG.warnf("Slow method execution detected: %s took %d ms", methodName, executionTime);
            }

            return result;

        } catch (Exception e) {
            long executionTime = (System.nanoTime() - startTime) / 1_000_000;
            LOG.errorf(e, "Method %s failed after %d ms", methodName, executionTime);
            throw e;

        } finally {
            MDC.remove("method");
        }
    }
}

/**
 * @Timed annotation
 * Marks methods or classes for automatic performance logging
 */
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface Timed {
}
