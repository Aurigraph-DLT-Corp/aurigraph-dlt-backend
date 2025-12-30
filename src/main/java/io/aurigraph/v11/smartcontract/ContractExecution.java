package io.aurigraph.v11.smartcontract;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Smart Contract Execution Record
 *
 * Represents a single execution of a smart contract, including inputs,
 * outputs, gas usage, and execution status.
 *
 * @version 11.2.1
 * @since 2025-10-12
 */
public class ContractExecution {

    @JsonProperty("executionId")
    private String executionId;

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("caller")
    private String caller;

    @JsonProperty("method")
    private String method;

    @JsonProperty("parameters")
    private Map<String, Object> parameters;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("status")
    private ExecutionStatus status;

    @JsonProperty("gasUsed")
    private Long gasUsed;

    @JsonProperty("executionTimeMs")
    private Long executionTimeMs;

    @JsonProperty("error")
    private String error;

    @JsonProperty("logs")
    private String[] logs;

    @JsonProperty("startedAt")
    private Instant startedAt;

    @JsonProperty("completedAt")
    private Instant completedAt;

    @JsonProperty("stateChanges")
    private Map<String, Object> stateChanges;

    /**
     * Contract execution status
     */
    public enum ExecutionStatus {
        PENDING,       // Execution queued
        RUNNING,       // Currently executing
        SUCCESS,       // Completed successfully
        FAILED,        // Execution failed
        REVERTED,      // Transaction reverted
        TIMEOUT        // Execution timed out
    }

    // Constructors
    public ContractExecution() {
        this.executionId = UUID.randomUUID().toString();
        this.startedAt = Instant.now();
        this.status = ExecutionStatus.PENDING;
    }

    public ContractExecution(String contractId, String caller, String method, Map<String, Object> parameters) {
        this();
        this.contractId = contractId;
        this.caller = caller;
        this.method = method;
        this.parameters = parameters;
    }

    // Getters and Setters
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(Long gasUsed) {
        this.gasUsed = gasUsed;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String[] getLogs() {
        return logs;
    }

    public void setLogs(String[] logs) {
        this.logs = logs;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Map<String, Object> getStateChanges() {
        return stateChanges;
    }

    public void setStateChanges(Map<String, Object> stateChanges) {
        this.stateChanges = stateChanges;
    }

    /**
     * Complete the execution with success
     */
    public void completeSuccess(Object result, Long gasUsed) {
        this.status = ExecutionStatus.SUCCESS;
        this.result = result;
        this.gasUsed = gasUsed;
        this.completedAt = Instant.now();
        this.executionTimeMs = completedAt.toEpochMilli() - startedAt.toEpochMilli();
    }

    /**
     * Complete the execution with failure
     */
    public void completeFailed(String error) {
        this.status = ExecutionStatus.FAILED;
        this.error = error;
        this.completedAt = Instant.now();
        this.executionTimeMs = completedAt.toEpochMilli() - startedAt.toEpochMilli();
    }

    @Override
    public String toString() {
        return String.format(
            "ContractExecution{id='%s', contractId='%s', method='%s', status=%s, gasUsed=%d}",
            executionId, contractId, method, status, gasUsed
        );
    }
}
