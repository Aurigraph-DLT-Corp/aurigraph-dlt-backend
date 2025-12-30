package io.aurigraph.v11.grpc;

import io.grpc.*;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * gRPC Metrics Interceptor for Aurigraph V11
 *
 * Collects metrics for all gRPC service calls without external dependencies.
 *
 * Metrics Collected:
 * - Total requests per method
 * - Total errors per method
 * - Request duration (min, max, average)
 * - Active calls per method
 * - Error counts by status code
 *
 * Features:
 * - Minimal overhead with lock-free atomics
 * - Per-method tracking for detailed analytics
 * - Real-time active call counting
 * - Error rate calculation
 *
 * Usage:
 * Get metrics via REST API: GET /q/metrics -> grpc.* metrics
 * Or programmatically via metricsCollector.getMetrics()
 *
 * Integration:
 * - Exposed through Quarkus metrics endpoint
 * - Compatible with Prometheus scraping
 * - Lightweight with minimal memory footprint
 */
@GlobalInterceptor
@ApplicationScoped
public class MetricsInterceptor implements ServerInterceptor {

    // Per-method metrics tracking
    private final ConcurrentHashMap<String, MethodMetrics> methodMetrics = new ConcurrentHashMap<>();

    // Global metrics
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong totalErrors = new AtomicLong();
    private final AtomicLong totalDuration = new AtomicLong();

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        long startTime = System.nanoTime();

        // Get or create method metrics
        MethodMetrics metrics = methodMetrics.computeIfAbsent(methodName, k -> new MethodMetrics());

        // Increment request counter and active calls
        metrics.totalRequests.incrementAndGet();
        metrics.activeCalls.incrementAndGet();
        totalRequests.incrementAndGet();

        if (Log.isDebugEnabled()) {
            Log.debugf("[GRPC_METRICS_START] Method: %s, ActiveCalls: %d",
                    methodName, metrics.activeCalls.get());
        }

        // Wrap the server call to record metrics on close
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {

                    @Override
                    public void close(Status status, Metadata trailers) {
                        long durationNanos = System.nanoTime() - startTime;
                        long durationMs = durationNanos / 1_000_000;

                        // Update metrics
                        metrics.updateDuration(durationMs);
                        metrics.activeCalls.decrementAndGet();
                        totalDuration.addAndGet(durationMs);

                        // Record error if status is not OK
                        if (!status.isOk()) {
                            metrics.errors.incrementAndGet();
                            metrics.errorsByStatus.compute(status.getCode().toString(),
                                    (k, v) -> v == null ? 1L : v + 1);
                            totalErrors.incrementAndGet();

                            if (Log.isDebugEnabled()) {
                                Log.debugf("[GRPC_METRICS_ERROR] Method: %s, Status: %s, Duration: %dms",
                                        methodName, status.getCode(), durationMs);
                            }
                        } else {
                            if (Log.isDebugEnabled()) {
                                Log.debugf("[GRPC_METRICS_OK] Method: %s, Duration: %dms",
                                        methodName, durationMs);
                            }
                        }

                        super.close(status, trailers);
                    }
                }, headers)) {

            @Override
            public void onCancel() {
                metrics.errors.incrementAndGet();
                metrics.errorsByStatus.compute("CANCELLED",
                        (k, v) -> v == null ? 1L : v + 1);
                metrics.activeCalls.decrementAndGet();
                totalErrors.incrementAndGet();

                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                Log.warnf("[GRPC_METRICS_CANCEL] Method: %s, Duration: %dms",
                        methodName, durationMs);

                super.onCancel();
            }
        };
    }

    /**
     * Get all collected metrics
     */
    public ConcurrentHashMap<String, MethodMetrics> getMetrics() {
        return methodMetrics;
    }

    /**
     * Get total request count
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * Get total error count
     */
    public long getTotalErrors() {
        return totalErrors.get();
    }

    /**
     * Get average request duration in milliseconds
     */
    public long getAverageDuration() {
        long requests = totalRequests.get();
        return requests > 0 ? totalDuration.get() / requests : 0;
    }

    /**
     * Get error rate as percentage
     */
    public double getErrorRate() {
        long requests = totalRequests.get();
        long errors = totalErrors.get();
        return requests > 0 ? (double) errors / requests * 100 : 0;
    }

    /**
     * Get total active calls across all methods
     */
    public int getTotalActiveCalls() {
        return methodMetrics.values().stream()
                .mapToInt(m -> (int) m.activeCalls.get())
                .sum();
    }

    /**
     * Reset all metrics (useful for testing)
     */
    public void resetMetrics() {
        methodMetrics.clear();
        totalRequests.set(0);
        totalErrors.set(0);
        totalDuration.set(0);
        Log.info("All gRPC metrics reset");
    }

    /**
     * Inner class to hold per-method metrics
     */
    public static class MethodMetrics {
        public final AtomicLong totalRequests = new AtomicLong();
        public final AtomicLong errors = new AtomicLong();
        public final AtomicInteger activeCalls = new AtomicInteger();
        public final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
        public final AtomicLong maxDuration = new AtomicLong(0);
        public final AtomicLong totalDuration = new AtomicLong();
        public final AtomicLong requestCount = new AtomicLong();
        public final ConcurrentHashMap<String, Long> errorsByStatus = new ConcurrentHashMap<>();

        /**
         * Update duration metrics
         */
        public void updateDuration(long durationMs) {
            totalDuration.addAndGet(durationMs);
            requestCount.incrementAndGet();

            // Update min/max
            long currentMin;
            long currentMax;
            do {
                currentMin = minDuration.get();
            } while (durationMs < currentMin && !minDuration.compareAndSet(currentMin, durationMs));

            do {
                currentMax = maxDuration.get();
            } while (durationMs > currentMax && !maxDuration.compareAndSet(currentMax, durationMs));
        }

        /**
         * Get average duration for this method
         */
        public long getAverageDuration() {
            long count = requestCount.get();
            return count > 0 ? totalDuration.get() / count : 0;
        }

        /**
         * Get error rate for this method
         */
        public double getErrorRate() {
            long total = totalRequests.get();
            long errs = errors.get();
            return total > 0 ? (double) errs / total * 100 : 0;
        }
    }
}
