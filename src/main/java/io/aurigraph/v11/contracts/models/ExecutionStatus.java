package io.aurigraph.v11.contracts.models;

/**
 * Execution status for smart contract execution results
 */
public enum ExecutionStatus {
    SUCCESS,
    FAILED,
    PENDING,
    TIMEOUT,
    REVERTED,
    OUT_OF_GAS
}