package io.aurigraph.v11.contracts;

import io.aurigraph.v11.contracts.models.*;
import java.time.Instant;
import java.util.*;

/**
 * Represents the execution state and result of a contract
 */
public class ContractExecution {

    /**
     * Execution status enum
     */
    public enum ExecutionStatus {
        PENDING,
        EXECUTING,
        SUCCESS,
        COMPLETED,
        FAILED,
        TIMEOUT,
        REVERTED
    }

    private String executionId;
    private String contractId;
    private ExecutionStatus status;
    private Map<String, Object> inputs;
    private Map<String, Object> outputs;
    private String executedBy;
    private Instant startedAt;
    private Instant completedAt;
    private Long gasUsed;
    private String errorMessage;
    private String transactionHash;
    private List<String> logs;
    private String method;
    private Map<String, Object> parameters;
    private String caller;
    private Instant timestamp;
    private Object result;
    private Long executionTimeMs;
    private String error;

    // Default constructor
    public ContractExecution() {
        this.executionId = UUID.randomUUID().toString();
        this.status = ExecutionStatus.PENDING;
        this.inputs = new HashMap<>();
        this.outputs = new HashMap<>();
        this.startedAt = Instant.now();
        this.logs = new ArrayList<>();
        this.gasUsed = 0L;
    }

    // Constructor with contract ID
    public ContractExecution(String contractId) {
        this();
        this.contractId = contractId;
    }

    // Full constructor
    public ContractExecution(String executionId, String contractId, ExecutionStatus status) {
        this();
        this.executionId = executionId != null ? executionId : UUID.randomUUID().toString();
        this.contractId = contractId;
        this.status = status != null ? status : ExecutionStatus.PENDING;
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

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public Map<String, Object> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
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

    public Long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(Long gasUsed) {
        this.gasUsed = gasUsed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
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

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
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

    // Business methods
    public void addInput(String key, Object value) {
        this.inputs.put(key, value);
    }

    public void addOutput(String key, Object value) {
        this.outputs.put(key, value);
    }

    public void addLog(String log) {
        this.logs.add(log);
    }

    public void markCompleted() {
        this.status = ExecutionStatus.SUCCESS;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public boolean isCompleted() {
        return this.status == ExecutionStatus.SUCCESS;
    }

    public boolean isFailed() {
        return this.status == ExecutionStatus.FAILED;
    }

    public boolean isPending() {
        return this.status == ExecutionStatus.PENDING;
    }

    public long getDurationMs() {
        if (completedAt != null && startedAt != null) {
            return completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
        return 0L;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractExecution that = (ContractExecution) o;
        return Objects.equals(executionId, that.executionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId);
    }

    @Override
    public String toString() {
        return "ContractExecution{" +
                "executionId='" + executionId + '\'' +
                ", contractId='" + contractId + '\'' +
                ", status=" + status +
                ", executedBy='" + executedBy + '\'' +
                ", gasUsed=" + gasUsed +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
