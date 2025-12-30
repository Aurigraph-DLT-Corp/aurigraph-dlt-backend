package io.aurigraph.v11.contracts.models;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public class ExecutionRequest {

    @NotBlank
    private String triggerId;

    private Map<String, Object> inputData;

    @NotBlank
    private String executorAddress;

    // Legacy fields for backward compatibility
    private String contractId;
    private Map<String, Object> executionParameters;
    private String transactionHash;
    private long gasLimit = 1000000L;
    private long gasPrice = 1000000000L; // 1 Gwei
    private boolean simulate = false;

    // New fields for contract execution
    private String contractAddress;
    private String methodName;
    private Object[] parameters;
    private String caller;

    // Constructors
    public ExecutionRequest() {
    }

    public ExecutionRequest(String contractAddress, String methodName, Object[] parameters, long gasLimit, String caller) {
        this.contractAddress = contractAddress;
        this.methodName = methodName;
        this.parameters = parameters;
        this.gasLimit = gasLimit;
        this.caller = caller;
        this.executorAddress = caller; // Set for backward compatibility
    }

    public ExecutionRequest(String triggerId, Map<String, Object> inputData, String executorAddress,
                           String contractId, Map<String, Object> executionParameters, String transactionHash,
                           long gasLimit, long gasPrice, boolean simulate, String contractAddress,
                           String methodName, Object[] parameters, String caller) {
        this.triggerId = triggerId;
        this.inputData = inputData;
        this.executorAddress = executorAddress;
        this.contractId = contractId;
        this.executionParameters = executionParameters;
        this.transactionHash = transactionHash;
        this.gasLimit = gasLimit;
        this.gasPrice = gasPrice;
        this.simulate = simulate;
        this.contractAddress = contractAddress;
        this.methodName = methodName;
        this.parameters = parameters;
        this.caller = caller;
    }

    // Getters (including backward compatibility methods)
    public String getTriggerId() {
        return triggerId;
    }

    public Map<String, Object> getInputData() {
        return inputData;
    }

    public String getExecutorAddress() {
        return executorAddress;
    }

    public String getContractId() {
        return contractId;
    }

    public Map<String, Object> getExecutionParameters() {
        return executionParameters;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public long getGasLimit() {
        return gasLimit;
    }

    public long getGasPrice() {
        return gasPrice;
    }

    public boolean isSimulate() {
        return simulate;
    }

    public String getContractAddress() {
        return contractAddress != null ? contractAddress : contractId;
    }

    public String getMethodName() {
        return methodName;
    }

    /**
     * Get method name (alias for getMethodName for backward compatibility)
     */
    public String getMethod() {
        return methodName;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public String getCaller() {
        return caller != null ? caller : executorAddress;
    }

    // Setters
    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
    }

    public void setInputData(Map<String, Object> inputData) {
        this.inputData = inputData;
    }

    public void setExecutorAddress(String executorAddress) {
        this.executorAddress = executorAddress;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public void setExecutionParameters(Map<String, Object> executionParameters) {
        this.executionParameters = executionParameters;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public void setGasLimit(long gasLimit) {
        this.gasLimit = gasLimit;
    }

    public void setGasPrice(long gasPrice) {
        this.gasPrice = gasPrice;
    }

    public void setSimulate(boolean simulate) {
        this.simulate = simulate;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }
}