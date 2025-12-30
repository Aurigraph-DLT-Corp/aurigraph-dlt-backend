package io.aurigraph.v11.contracts.models;

import io.aurigraph.v11.contracts.RicardianContract;

/**
 * Execution context for smart contract operations
 * Contains all necessary information for contract execution
 */
public class ExecutionContext {
    
    private final RicardianContract contract;
    private final ExecutionRequest request;
    private final ContractState state;
    private final GasTracker gasTracker;
    private final long startTime;
    
    private ExecutionContext(Builder builder) {
        this.contract = builder.contract;
        this.request = builder.request;
        this.state = builder.state;
        this.gasTracker = builder.gasTracker;
        this.startTime = builder.startTime;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public RicardianContract getContract() { return contract; }
    public ExecutionRequest getRequest() { return request; }
    public ContractState getState() { return state; }
    public GasTracker getGasTracker() { return gasTracker; }
    public long getStartTime() { return startTime; }
    
    /**
     * Get input data from the execution request
     */
    public java.util.Map<String, Object> getInputData() {
        if (request == null) {
            return new java.util.HashMap<>();
        }
        
        // First try to get inputData map
        if (request.getInputData() != null) {
            return request.getInputData();
        }
        
        // If inputData is null, try executionParameters
        if (request.getExecutionParameters() != null) {
            return request.getExecutionParameters();
        }
        
        // If both are null, convert parameters array to map
        Object[] params = request.getParameters();
        java.util.Map<String, Object> paramMap = new java.util.HashMap<>();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                paramMap.put("param" + i, params[i]);
            }
        }
        
        return paramMap;
    }
    
    /**
     * Get execution ID
     */
    public String getExecutionId() {
        return "EX_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
    
    // Builder pattern
    public static class Builder {
        private RicardianContract contract;
        private ExecutionRequest request;
        private ContractState state;
        private GasTracker gasTracker;
        private long startTime;
        
        public Builder contract(RicardianContract contract) {
            this.contract = contract;
            return this;
        }
        
        public Builder request(ExecutionRequest request) {
            this.request = request;
            return this;
        }
        
        public Builder state(ContractState state) {
            this.state = state;
            return this;
        }
        
        public Builder gasTracker(GasTracker gasTracker) {
            this.gasTracker = gasTracker;
            return this;
        }
        
        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public ExecutionContext build() {
            return new ExecutionContext(this);
        }
    }
}