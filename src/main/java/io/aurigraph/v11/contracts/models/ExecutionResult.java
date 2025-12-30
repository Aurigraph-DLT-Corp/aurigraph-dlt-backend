package io.aurigraph.v11.contracts.models;

import java.time.Instant;
import java.util.Map;

public class ExecutionResult {

    private String executionId;
    private String contractId;
    private String transactionHash;
    private boolean success;
    private Object result; // Changed from String to Object for flexibility
    private String error;
    private long gasUsed;
    private Instant executedAt;
    private Map<String, Object> outputData;
    private String[] eventLogs;
    private ExecutionStatus status;
    private long executionTime;

    // Constructors
    public ExecutionResult() {
    }

    public ExecutionResult(String executionId, ExecutionStatus status, String error, Object result, long gasUsed, long executionTime) {
        this.executionId = executionId;
        this.status = status;
        this.error = error;
        this.result = result;
        this.gasUsed = gasUsed;
        this.executionTime = executionTime;
        this.success = (status == ExecutionStatus.SUCCESS);
        this.executedAt = Instant.now();
    }

    /**
     * Simple constructor for success/error cases
     * Added for BUG-003 fix
     */
    public ExecutionResult(String executionId, String method, Instant timestamp, String message) {
        this.executionId = executionId;
        this.result = method;
        this.executedAt = timestamp;
        this.error = message;
        this.success = "SUCCESS".equalsIgnoreCase(message);
        this.status = this.success ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
        this.gasUsed = 0L;
        this.executionTime = 0L;
    }

    public ExecutionResult(String executionId, String contractId, String transactionHash, boolean success,
                          Object result, String error, long gasUsed, Instant executedAt,
                          Map<String, Object> outputData, String[] eventLogs, ExecutionStatus status,
                          long executionTime) {
        this.executionId = executionId;
        this.contractId = contractId;
        this.transactionHash = transactionHash;
        this.success = success;
        this.result = result;
        this.error = error;
        this.gasUsed = gasUsed;
        this.executedAt = executedAt;
        this.outputData = outputData;
        this.eventLogs = eventLogs;
        this.status = status;
        this.executionTime = executionTime;
    }

    // Getters
    public String getExecutionId() {
        return executionId;
    }

    public String getContractId() {
        return contractId;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public Map<String, Object> getOutputData() {
        return outputData;
    }

    public String[] getEventLogs() {
        return eventLogs;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    // Setters
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = gasUsed;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public void setOutputData(Map<String, Object> outputData) {
        this.outputData = outputData;
    }

    public void setEventLogs(String[] eventLogs) {
        this.eventLogs = eventLogs;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
}