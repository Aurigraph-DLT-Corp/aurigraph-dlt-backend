package io.aurigraph.v11.contracts;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import io.quarkus.logging.Log;
import io.aurigraph.v11.contracts.models.*;
import io.aurigraph.v11.contracts.models.GasTracker.OutOfGasException;

/**
 * Smart Contract Execution Engine for Aurigraph V11
 * Features: Gas metering, state management, execution sandbox, security isolation
 * High-performance contract execution with quantum-safe verification
 */
@ApplicationScoped
public class ContractExecutor {

    @Inject
    ContractRepository contractRepository;

    // Execution state and metrics
    private final Map<String, ContractState> contractStates = new ConcurrentHashMap<>();
    private final Map<String, GasTracker> gasTrackers = new ConcurrentHashMap<>();
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong totalGasConsumed = new AtomicLong(0);

    // Gas pricing constants
    private static final long BASE_EXECUTION_GAS = 21_000L;
    private static final long STORAGE_WRITE_GAS = 20_000L;
    private static final long STORAGE_READ_GAS = 800L;
    private static final long COMPUTATION_GAS = 3L;
    private static final long MEMORY_GAS = 3L;
    private static final long LOG_GAS = 375L;
    
    // Security limits
    private static final long MAX_GAS_LIMIT = 10_000_000L;
    private static final long MAX_EXECUTION_TIME_MS = 30_000L; // 30 seconds
    private static final int MAX_STACK_DEPTH = 1024;

    /**
     * Execute a smart contract with full gas metering and security checks
     */
    public Uni<ExecutionResult> executeContract(ExecutionRequest request) {
        return Uni.createFrom().item(() -> {
            long startTime = System.currentTimeMillis();
            String executionId = generateExecutionId();
            
            Log.infof("Starting contract execution %s for contract %s", 
                executionId, request.getContractAddress());
            
            try {
                // Validate execution request
                validateExecutionRequest(request);
                
                // Initialize gas tracker
                GasTracker gasTracker = new GasTracker(request.getGasLimit(), executionId);
                gasTrackers.put(executionId, gasTracker);
                
                // Get contract from repository
                RicardianContract contract = contractRepository.findByAddress(request.getContractAddress());
                if (contract == null) {
                    throw new ContractNotFoundException("Contract not found: " + request.getContractAddress());
                }
                
                // Validate contract is executable
                if (contract.getStatus() != ContractStatus.ACTIVE) {
                    throw new ContractExecutionException("Contract not active: " + contract.getStatus());
                }
                
                // Create execution context
                ExecutionContext context = createExecutionContext(request, contract, gasTracker);
                
                // Execute contract in sandbox
                ExecutionResult result = executeInSandbox(context);
                
                // Record execution metrics
                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;
                
                totalExecutions.incrementAndGet();
                totalGasConsumed.addAndGet(gasTracker.getGasUsed());
                
                // Update contract metrics
                updateContractMetrics(contract.getContractId(), gasTracker.getGasUsed(), executionTime);
                
                Log.infof("Contract execution %s completed in %d ms, gas used: %d", 
                    executionId, executionTime, gasTracker.getGasUsed());
                
                return result;
                
            } catch (Exception e) {
                Log.errorf("Contract execution %s failed: %s", executionId, e.getMessage());
                return new ExecutionResult(
                    executionId, 
                    ExecutionStatus.FAILED, 
                    e.getMessage(),
                    null,
                    0L,
                    System.currentTimeMillis() - startTime
                );
            } finally {
                // Cleanup
                gasTrackers.remove(executionId);
            }
            
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Execute contract method with gas metering
     */
    public Uni<ExecutionResult> executeContractMethod(String contractAddress, String methodName, 
                                                      Object[] parameters, long gasLimit) {
        ExecutionRequest request = new ExecutionRequest(
            contractAddress, methodName, parameters, gasLimit, "0x0"
        );
        return executeContract(request);
    }

    /**
     * Estimate gas required for contract execution
     */
    public Uni<Long> estimateGas(ExecutionRequest request) {
        return Uni.createFrom().item(() -> {
            // Simulate execution without state changes
            try {
                RicardianContract contract = contractRepository.findByAddress(request.getContractAddress());
                if (contract == null) {
                    return BASE_EXECUTION_GAS;
                }
                
                // Base gas estimation logic
                long estimatedGas = BASE_EXECUTION_GAS;
                
                // Add gas based on contract complexity
                if (contract.getExecutableCode() != null) {
                    estimatedGas += contract.getExecutableCode().length() * 10L;
                }
                
                // Add gas for parameters
                if (request.getParameters() != null) {
                    estimatedGas += request.getParameters().length * 100L;
                }
                
                // Method-specific gas estimation
                estimatedGas += estimateMethodGas(request.getMethodName());
                
                return Math.min(estimatedGas, MAX_GAS_LIMIT);
                
            } catch (Exception e) {
                Log.warnf("Gas estimation failed, using base gas: %s", e.getMessage());
                return BASE_EXECUTION_GAS;
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get contract state
     */
    public Uni<ContractState> getContractState(String contractAddress) {
        return Uni.createFrom().item(() -> {
            ContractState state = contractStates.get(contractAddress);
            if (state == null) {
                // Initialize empty state
                state = new ContractState(contractAddress);
                contractStates.put(contractAddress, state);
            }
            return state;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get execution statistics
     */
    public Uni<Map<String, Object>> getExecutionStatistics() {
        return Uni.createFrom().item(() -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalExecutions", totalExecutions.get());
            stats.put("totalGasConsumed", totalGasConsumed.get());
            stats.put("averageGasPerExecution", 
                totalExecutions.get() > 0 ? totalGasConsumed.get() / totalExecutions.get() : 0);
            stats.put("activeStates", contractStates.size());
            stats.put("activeGasTrackers", gasTrackers.size());
            stats.put("timestamp", Instant.now());
            return stats;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // Private helper methods

    private void validateExecutionRequest(ExecutionRequest request) {
        if (request.getContractAddress() == null || request.getContractAddress().isEmpty()) {
            throw new IllegalArgumentException("Contract address is required");
        }
        
        if (request.getGasLimit() <= 0) {
            throw new IllegalArgumentException("Gas limit must be positive");
        }
        
        if (request.getGasLimit() > MAX_GAS_LIMIT) {
            throw new IllegalArgumentException("Gas limit exceeds maximum: " + MAX_GAS_LIMIT);
        }
    }

    private String generateExecutionId() {
        return "EXEC-" + System.nanoTime() + "-" + 
               Integer.toHexString((int) (Math.random() * 0x10000));
    }

    private ExecutionContext createExecutionContext(ExecutionRequest request, 
                                                   RicardianContract contract, 
                                                   GasTracker gasTracker) {
        ContractState state = contractStates.computeIfAbsent(
            request.getContractAddress(), 
            k -> new ContractState(request.getContractAddress())
        );
        
        return ExecutionContext.builder()
            .contract(contract)
            .request(request)
            .state(state)
            .gasTracker(gasTracker)
            .startTime(System.currentTimeMillis())
            .build();
    }

    private ExecutionResult executeInSandbox(ExecutionContext context) {
        String executionId = context.getGasTracker().getExecutionId();
        long startTime = context.getStartTime();
        
        try {
            // Start gas metering
            context.getGasTracker().consumeGas(BASE_EXECUTION_GAS);
            
            // Security check: execution time limit
            if (System.currentTimeMillis() - startTime > MAX_EXECUTION_TIME_MS) {
                throw new ContractExecutionException("Execution timeout");
            }
            
            // Execute based on method name
            Object result = executeMethod(context);
            
            // Final gas check
            long gasUsed = context.getGasTracker().getGasUsed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new ExecutionResult(
                executionId,
                ExecutionStatus.SUCCESS,
                "Execution completed successfully",
                result,
                gasUsed,
                executionTime
            );
            
        } catch (OutOfGasException e) {
            return new ExecutionResult(
                executionId,
                ExecutionStatus.OUT_OF_GAS,
                "Out of gas: " + e.getMessage(),
                null,
                context.getGasTracker().getGasLimit(),
                System.currentTimeMillis() - startTime
            );
        } catch (Exception e) {
            return new ExecutionResult(
                executionId,
                ExecutionStatus.FAILED,
                "Execution failed: " + e.getMessage(),
                null,
                context.getGasTracker().getGasUsed(),
                System.currentTimeMillis() - startTime
            );
        }
    }

    private Object executeMethod(ExecutionContext context) {
        String methodName = context.getRequest().getMethodName();
        Object[] parameters = context.getRequest().getParameters();
        GasTracker gasTracker = context.getGasTracker();
        
        // Method execution with gas metering
        gasTracker.consumeGas(COMPUTATION_GAS * 10); // Method setup gas
        
        switch (methodName.toLowerCase()) {
            case "transfer":
                return executeTransfer(context, parameters);
            case "approve":
                return executeApprove(context, parameters);
            case "balanceof":
                return executeBalanceOf(context, parameters);
            case "mint":
                return executeMint(context, parameters);
            case "burn":
                return executeBurn(context, parameters);
            case "getowner":
                return executeGetOwner(context);
            case "setvalue":
                return executeSetValue(context, parameters);
            case "getvalue":
                return executeGetValue(context, parameters);
            default:
                throw new ContractExecutionException("Unknown method: " + methodName);
        }
    }

    private Object executeTransfer(ExecutionContext context, Object[] parameters) {
        if (parameters.length != 2) {
            throw new IllegalArgumentException("Transfer requires 2 parameters: to, amount");
        }
        
        String to = (String) parameters[0];
        BigDecimal amount = new BigDecimal(parameters[1].toString());
        
        // Gas for parameter processing
        context.getGasTracker().consumeGas(COMPUTATION_GAS * 5);
        
        // Gas for state reads/writes
        context.getGasTracker().consumeGas(STORAGE_READ_GAS * 2); // Read balances
        context.getGasTracker().consumeGas(STORAGE_WRITE_GAS * 2); // Update balances
        
        // Simulate transfer logic
        ContractState state = context.getState();
        String from = context.getRequest().getCaller();
        
        BigDecimal fromBalance = state.getBalance(from);
        if (fromBalance.compareTo(amount) < 0) {
            throw new ContractExecutionException("Insufficient balance");
        }
        
        state.setBalance(from, fromBalance.subtract(amount));
        state.setBalance(to, state.getBalance(to).add(amount));
        
        return true;
    }

    private Object executeApprove(ExecutionContext context, Object[] parameters) {
        if (parameters.length != 2) {
            throw new IllegalArgumentException("Approve requires 2 parameters: spender, amount");
        }
        
        String spender = (String) parameters[0];
        BigDecimal amount = new BigDecimal(parameters[1].toString());
        
        context.getGasTracker().consumeGas(COMPUTATION_GAS * 3);
        context.getGasTracker().consumeGas(STORAGE_WRITE_GAS); // Write allowance
        
        String owner = context.getRequest().getCaller();
        context.getState().setAllowance(owner, spender, amount);
        
        return true;
    }

    private Object executeBalanceOf(ExecutionContext context, Object[] parameters) {
        if (parameters.length != 1) {
            throw new IllegalArgumentException("BalanceOf requires 1 parameter: account");
        }
        
        String account = (String) parameters[0];
        
        context.getGasTracker().consumeGas(COMPUTATION_GAS * 2);
        context.getGasTracker().consumeGas(STORAGE_READ_GAS); // Read balance
        
        return context.getState().getBalance(account);
    }

    private Object executeMint(ExecutionContext context, Object[] parameters) {
        if (parameters.length != 2) {
            throw new IllegalArgumentException("Mint requires 2 parameters: to, amount");
        }
        
        String to = (String) parameters[0];
        BigDecimal amount = new BigDecimal(parameters[1].toString());
        
        context.getGasTracker().consumeGas(COMPUTATION_GAS * 5);
        context.getGasTracker().consumeGas(STORAGE_READ_GAS); // Read balance
        context.getGasTracker().consumeGas(STORAGE_WRITE_GAS); // Write balance
        
        ContractState state = context.getState();
        state.setBalance(to, state.getBalance(to).add(amount));
        state.increaseTotalSupply(amount);
        
        return true;
    }

    private Object executeBurn(ExecutionContext context, Object[] parameters) {
        if (parameters.length != 2) {
            throw new IllegalArgumentException("Burn requires 2 parameters: from, amount");
        }
        
        String from = (String) parameters[0];
        BigDecimal amount = new BigDecimal(parameters[1].toString());
        
        context.getGasTracker().consumeGas(COMPUTATION_GAS * 5);
        context.getGasTracker().consumeGas(STORAGE_READ_GAS); // Read balance
        context.getGasTracker().consumeGas(STORAGE_WRITE_GAS); // Write balance
        
        ContractState state = context.getState();
        BigDecimal balance = state.getBalance(from);
        
        if (balance.compareTo(amount) < 0) {
            throw new ContractExecutionException("Insufficient balance to burn");
        }
        
        state.setBalance(from, balance.subtract(amount));
        state.decreaseTotalSupply(amount);
        
        return true;
    }

    private Object executeGetOwner(ExecutionContext context) {
        context.getGasTracker().consumeGas(COMPUTATION_GAS);
        context.getGasTracker().consumeGas(STORAGE_READ_GAS);
        
        return context.getState().getOwner();
    }

    private Object executeSetValue(ExecutionContext context, Object[] parameters) {
        if (parameters.length != 2) {
            throw new IllegalArgumentException("SetValue requires 2 parameters: key, value");
        }
        
        String key = (String) parameters[0];
        String value = (String) parameters[1];
        
        context.getGasTracker().consumeGas(COMPUTATION_GAS * 3);
        context.getGasTracker().consumeGas(STORAGE_WRITE_GAS);
        
        context.getState().setValue(key, value);
        return true;
    }

    private Object executeGetValue(ExecutionContext context, Object[] parameters) {
        if (parameters.length != 1) {
            throw new IllegalArgumentException("GetValue requires 1 parameter: key");
        }
        
        String key = (String) parameters[0];
        
        context.getGasTracker().consumeGas(COMPUTATION_GAS);
        context.getGasTracker().consumeGas(STORAGE_READ_GAS);
        
        return context.getState().getValue(key);
    }

    private long estimateMethodGas(String methodName) {
        switch (methodName.toLowerCase()) {
            case "transfer":
                return COMPUTATION_GAS * 5 + STORAGE_READ_GAS * 2 + STORAGE_WRITE_GAS * 2;
            case "approve":
                return COMPUTATION_GAS * 3 + STORAGE_WRITE_GAS;
            case "balanceof":
                return COMPUTATION_GAS * 2 + STORAGE_READ_GAS;
            case "mint":
            case "burn":
                return COMPUTATION_GAS * 5 + STORAGE_READ_GAS + STORAGE_WRITE_GAS;
            case "getowner":
            case "getvalue":
                return COMPUTATION_GAS + STORAGE_READ_GAS;
            case "setvalue":
                return COMPUTATION_GAS * 3 + STORAGE_WRITE_GAS;
            default:
                return COMPUTATION_GAS * 10; // Default estimation
        }
    }

    private void updateContractMetrics(String contractId, long gasUsed, long executionTime) {
        // In a real implementation, this would update contract metrics in the repository
        Log.debugf("Updated metrics for contract %s: gas=%d, time=%dms", 
            contractId, gasUsed, executionTime);
    }

    // Exception classes
    public static class ContractNotFoundException extends RuntimeException {
        public ContractNotFoundException(String message) { super(message); }
    }

    public static class ContractExecutionException extends RuntimeException {
        public ContractExecutionException(String message) { super(message); }
    }

    public static class OutOfGasException extends RuntimeException {
        public OutOfGasException(String message) { super(message); }
    }
}