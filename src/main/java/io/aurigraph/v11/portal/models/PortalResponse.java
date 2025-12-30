package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Standard response wrapper for all Portal API endpoints
 * Ensures consistent response structure across all endpoints
 *
 * @param <T> The data type of the response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortalResponse<T> {
    private int status;
    private String message;
    private T data;
    private String timestamp;
    private String requestId;
    private ErrorDetails error;

    // Constructors
    public PortalResponse() {
    }

    public PortalResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toString();
    }

    public PortalResponse(int status, String message, T data, String requestId) {
        this(status, message, data);
        this.requestId = requestId;
    }

    public static <T> PortalResponse<T> success(T data) {
        return new PortalResponse<>(200, "Success", data);
    }

    public static <T> PortalResponse<T> success(T data, String message) {
        return new PortalResponse<>(200, message, data);
    }

    public static <T> PortalResponse<T> error(int status, String message) {
        PortalResponse<T> response = new PortalResponse<>();
        response.status = status;
        response.message = message;
        response.timestamp = Instant.now().toString();
        response.error = new ErrorDetails(message);
        return response;
    }

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }

    /**
     * Error details wrapper for error responses
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private String message;
        private String code;
        private String details;

        public ErrorDetails(String message) {
            this.message = message;
        }

        public ErrorDetails(String message, String code) {
            this.message = message;
            this.code = code;
        }

        public ErrorDetails(String message, String code, String details) {
            this.message = message;
            this.code = code;
            this.details = details;
        }

        // Getters and Setters
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }
    }
}
