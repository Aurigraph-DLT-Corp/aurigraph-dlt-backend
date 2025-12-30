package io.aurigraph.v11.grpc;

import io.grpc.*;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * gRPC Exception Handling Interceptor for Aurigraph V11
 *
 * Catches and converts Java exceptions to appropriate gRPC Status codes.
 *
 * Behavior:
 * - Catches unhandled exceptions during gRPC method execution
 * - Maps Java exceptions to gRPC Status codes:
 *   - IllegalArgumentException -> INVALID_ARGUMENT
 *   - SecurityException -> PERMISSION_DENIED
 *   - IllegalStateException -> FAILED_PRECONDITION
 *   - IOException -> UNAVAILABLE
 *   - TimeoutException -> DEADLINE_EXCEEDED
 *   - Others -> INTERNAL
 * - Logs full stack traces for debugging
 * - Returns user-friendly error messages to clients
 *
 * Prevents:
 * - Unhandled exceptions crashing the gRPC server
 * - Stack trace information leaking to clients
 * - Inconsistent error handling across services
 *
 * Example:
 * If a service method throws IllegalArgumentException("Invalid asset ID"),
 * the client receives Status.INVALID_ARGUMENT with description "Invalid asset ID"
 */
@GlobalInterceptor
@ApplicationScoped
public class ExceptionInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        // If there was an exception, it's already handled by next handler
                        super.close(status, trailers);
                    }
                }, headers)) {

            @Override
            public void onMessage(ReqT message) {
                try {
                    super.onMessage(message);
                } catch (Exception e) {
                    handleException(call, methodName, e);
                }
            }

            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Exception e) {
                    handleException(call, methodName, e);
                }
            }

            @Override
            public void onCancel() {
                try {
                    super.onCancel();
                } catch (Exception e) {
                    Log.warnf("Exception during cancel for %s: %s", methodName, e.getMessage());
                }
            }

            @Override
            public void onComplete() {
                try {
                    super.onComplete();
                } catch (Exception e) {
                    Log.warnf("Exception during complete for %s: %s", methodName, e.getMessage());
                }
            }
        };
    }

    /**
     * Convert Java exceptions to gRPC Status codes
     */
    @SuppressWarnings("unchecked")
    private <ReqT, RespT> void handleException(
            ServerCall<ReqT, RespT> call,
            String methodName,
            Exception exception) {

        String exceptionType = exception.getClass().getSimpleName();
        String errorMessage = exception.getMessage() != null ? exception.getMessage() : exceptionType;

        Status status = mapExceptionToStatus(exception, errorMessage);

        Log.warnf("[GRPC_EXCEPTION] Method: %s, Type: %s, Message: %s",
                methodName, exceptionType, errorMessage);

        // Log stack trace only in development/debug mode
        if (Log.isDebugEnabled()) {
            Log.debug("Exception stack trace:", exception);
        }

        // Close the call with appropriate status
        call.close(status, new Metadata());
    }

    /**
     * Map Java exception types to gRPC Status codes
     */
    private Status mapExceptionToStatus(Exception exception, String message) {
        if (exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(message);
        } else if (exception instanceof SecurityException) {
            return Status.PERMISSION_DENIED.withDescription(message);
        } else if (exception instanceof IllegalStateException) {
            return Status.FAILED_PRECONDITION.withDescription(message);
        } else if (exception instanceof java.io.IOException) {
            return Status.UNAVAILABLE.withDescription(message);
        } else if (exception instanceof java.util.concurrent.TimeoutException) {
            return Status.DEADLINE_EXCEEDED.withDescription(message);
        } else if (exception instanceof NullPointerException) {
            return Status.INTERNAL.withDescription("Null pointer exception: " + message);
        } else if (exception instanceof UnsupportedOperationException) {
            return Status.UNIMPLEMENTED.withDescription(message);
        } else {
            // Default to INTERNAL for unknown exceptions
            return Status.INTERNAL.withDescription("Internal server error: " + message);
        }
    }

    /**
     * Check if exception is a validation error
     */
    private boolean isValidationError(Exception exception) {
        return exception instanceof IllegalArgumentException ||
                exception instanceof NullPointerException ||
                exception instanceof IllegalStateException;
    }

    /**
     * Check if exception is a permission/security error
     */
    private boolean isPermissionError(Exception exception) {
        return exception instanceof SecurityException ||
                exception instanceof java.nio.file.AccessDeniedException;
    }

    /**
     * Check if exception is a system/resource error
     */
    private boolean isSystemError(Throwable exception) {
        return exception instanceof java.io.IOException ||
                exception instanceof OutOfMemoryError ||
                exception instanceof StackOverflowError;
    }
}
