package io.aurigraph.v11.demo.config;

import io.aurigraph.v11.demo.models.NodeType;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Configuration specific to Business Nodes.
 *
 * Business Nodes are responsible for:
 * - Processing transaction requests
 * - Executing smart contracts (Ricardian contracts)
 * - Handling business logic workflows
 * - Integrating with enterprise systems
 * - Tracking business metrics
 *
 * Performance Targets:
 * - Transaction execution: <20ms
 * - Contract execution: <100ms
 * - Throughput: 100K transactions/sec per node
 * - Contract call throughput: 50K calls/sec per node
 */
@RegisterForReflection
public class BusinessNodeConfig extends NodeConfiguration {

    /**
     * Maximum concurrent transactions this node can process.
     * Default: 10,000 transactions
     */
    private int maxConcurrentTransactions = 10000;

    /**
     * Smart contract execution timeout in milliseconds.
     * Prevents runaway contracts from blocking the system.
     * Default: 5,000ms (5 seconds)
     */
    private int contractExecutionTimeout = 5000;

    /**
     * Workflow engine to use for business process automation.
     * Options: "Camunda", "Flowable", "Activiti", "builtin"
     * Default: "Camunda"
     */
    private String workflowEngine = "Camunda";

    /**
     * Enable Ricardian contract support.
     * Ricardian contracts combine legal prose with executable code.
     * Default: true
     */
    private boolean enableRicardianContracts = true;

    /**
     * Compliance mode for regulatory requirements.
     * Options: "strict", "moderate", "permissive"
     * Default: "strict"
     */
    private String complianceMode = "strict";

    /**
     * Enable comprehensive audit logging.
     * Logs all transaction and contract executions for compliance.
     * Default: true
     */
    private boolean auditLogging = true;

    /**
     * Maximum contract execution gas limit.
     * Prevents expensive operations from consuming excessive resources.
     * Default: 10,000,000 gas units
     */
    private long maxGasLimit = 10000000L;

    /**
     * Enable contract sandboxing for security.
     * Isolates contract execution in a secure environment.
     * Default: true
     */
    private boolean enableSandboxing = true;

    /**
     * Contract state database backend.
     * Options: "leveldb", "rocksdb", "postgres"
     * Default: "leveldb"
     */
    private String stateDatabase = "leveldb";

    /**
     * Enable contract state caching for performance.
     * Default: true
     */
    private boolean enableStateCache = true;

    /**
     * State cache size in megabytes.
     * Default: 1024MB (1GB)
     */
    private int stateCacheSize = 1024;

    /**
     * Enable business metrics collection.
     * Tracks transaction volumes, contract calls, execution times, etc.
     * Default: true
     */
    private boolean enableMetrics = true;

    /**
     * Metrics aggregation interval in seconds.
     * Default: 60 seconds
     */
    private int metricsIntervalSeconds = 60;

    /**
     * Enable integration with external enterprise systems.
     * Default: true
     */
    private boolean enableEnterpriseIntegration = true;

    /**
     * Enterprise integration protocols supported.
     * Format: comma-separated list (e.g., "REST,SOAP,gRPC")
     * Default: "REST,gRPC"
     */
    private String enterpriseProtocols = "REST,gRPC";

    // Constructors

    public BusinessNodeConfig() {
        super();
        setNodeType(NodeType.BUSINESS);
    }

    public BusinessNodeConfig(String nodeId) {
        super(nodeId, NodeType.BUSINESS);
    }

    // Getters and Setters

    public int getMaxConcurrentTransactions() {
        return maxConcurrentTransactions;
    }

    public void setMaxConcurrentTransactions(int maxConcurrentTransactions) {
        this.maxConcurrentTransactions = maxConcurrentTransactions;
    }

    public int getContractExecutionTimeout() {
        return contractExecutionTimeout;
    }

    public void setContractExecutionTimeout(int contractExecutionTimeout) {
        this.contractExecutionTimeout = contractExecutionTimeout;
    }

    public String getWorkflowEngine() {
        return workflowEngine;
    }

    public void setWorkflowEngine(String workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    public boolean isEnableRicardianContracts() {
        return enableRicardianContracts;
    }

    public void setEnableRicardianContracts(boolean enableRicardianContracts) {
        this.enableRicardianContracts = enableRicardianContracts;
    }

    public String getComplianceMode() {
        return complianceMode;
    }

    public void setComplianceMode(String complianceMode) {
        this.complianceMode = complianceMode;
    }

    public boolean isAuditLogging() {
        return auditLogging;
    }

    public void setAuditLogging(boolean auditLogging) {
        this.auditLogging = auditLogging;
    }

    public long getMaxGasLimit() {
        return maxGasLimit;
    }

    public void setMaxGasLimit(long maxGasLimit) {
        this.maxGasLimit = maxGasLimit;
    }

    public boolean isEnableSandboxing() {
        return enableSandboxing;
    }

    public void setEnableSandboxing(boolean enableSandboxing) {
        this.enableSandboxing = enableSandboxing;
    }

    public String getStateDatabase() {
        return stateDatabase;
    }

    public void setStateDatabase(String stateDatabase) {
        this.stateDatabase = stateDatabase;
    }

    public boolean isEnableStateCache() {
        return enableStateCache;
    }

    public void setEnableStateCache(boolean enableStateCache) {
        this.enableStateCache = enableStateCache;
    }

    public int getStateCacheSize() {
        return stateCacheSize;
    }

    public void setStateCacheSize(int stateCacheSize) {
        this.stateCacheSize = stateCacheSize;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    public int getMetricsIntervalSeconds() {
        return metricsIntervalSeconds;
    }

    public void setMetricsIntervalSeconds(int metricsIntervalSeconds) {
        this.metricsIntervalSeconds = metricsIntervalSeconds;
    }

    public boolean isEnableEnterpriseIntegration() {
        return enableEnterpriseIntegration;
    }

    public void setEnableEnterpriseIntegration(boolean enableEnterpriseIntegration) {
        this.enableEnterpriseIntegration = enableEnterpriseIntegration;
    }

    public String getEnterpriseProtocols() {
        return enterpriseProtocols;
    }

    public void setEnterpriseProtocols(String enterpriseProtocols) {
        this.enterpriseProtocols = enterpriseProtocols;
    }

    @Override
    public void validate() {
        // Validate base configuration first
        super.validate();

        // Validate business node-specific configuration
        if (maxConcurrentTransactions <= 0) {
            throw new IllegalArgumentException("Max concurrent transactions must be greater than 0");
        }

        if (maxConcurrentTransactions > 100000) {
            throw new IllegalArgumentException("Max concurrent transactions should not exceed 100,000");
        }

        if (contractExecutionTimeout <= 0) {
            throw new IllegalArgumentException("Contract execution timeout must be greater than 0");
        }

        if (contractExecutionTimeout > 60000) {
            throw new IllegalArgumentException("Contract execution timeout should not exceed 60 seconds");
        }

        if (!workflowEngine.matches("Camunda|Flowable|Activiti|builtin")) {
            throw new IllegalArgumentException("Workflow engine must be one of: Camunda, Flowable, Activiti, builtin");
        }

        if (!complianceMode.matches("strict|moderate|permissive")) {
            throw new IllegalArgumentException("Compliance mode must be one of: strict, moderate, permissive");
        }

        if (maxGasLimit <= 0) {
            throw new IllegalArgumentException("Max gas limit must be greater than 0");
        }

        if (!stateDatabase.matches("leveldb|rocksdb|postgres")) {
            throw new IllegalArgumentException("State database must be one of: leveldb, rocksdb, postgres");
        }

        if (enableStateCache && stateCacheSize <= 0) {
            throw new IllegalArgumentException("State cache size must be greater than 0 when caching is enabled");
        }

        if (enableMetrics && metricsIntervalSeconds <= 0) {
            throw new IllegalArgumentException("Metrics interval must be greater than 0 when metrics are enabled");
        }

        if (enterpriseProtocols != null && !enterpriseProtocols.isEmpty()) {
            String[] protocols = enterpriseProtocols.split(",");
            for (String protocol : protocols) {
                if (!protocol.trim().matches("REST|SOAP|gRPC|GraphQL|WebSocket")) {
                    throw new IllegalArgumentException("Invalid enterprise protocol: " + protocol.trim() +
                        ". Must be one of: REST, SOAP, gRPC, GraphQL, WebSocket");
                }
            }
        }
    }

    @Override
    public String toString() {
        return "BusinessNodeConfig{" +
               "nodeId='" + getNodeId() + '\'' +
               ", maxConcurrentTransactions=" + maxConcurrentTransactions +
               ", contractExecutionTimeout=" + contractExecutionTimeout +
               ", workflowEngine='" + workflowEngine + '\'' +
               ", enableRicardianContracts=" + enableRicardianContracts +
               ", complianceMode='" + complianceMode + '\'' +
               ", auditLogging=" + auditLogging +
               '}';
    }
}
