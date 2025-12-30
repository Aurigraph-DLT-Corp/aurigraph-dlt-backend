package io.aurigraph.v11.contracts;

/**
 * Contract programming language enum
 * Defines the supported programming languages for smart contracts in Aurigraph V11
 */
public enum ContractLanguage {
    SOLIDITY,    // Ethereum-compatible smart contracts
    JAVA,        // Native Quarkus/GraalVM contracts
    JAVASCRIPT,  // V8-based execution
    WASM,        // WebAssembly high-performance contracts
    PYTHON,      // AI/ML-focused contracts
    CUSTOM       // Aurigraph-specific DSL
}
