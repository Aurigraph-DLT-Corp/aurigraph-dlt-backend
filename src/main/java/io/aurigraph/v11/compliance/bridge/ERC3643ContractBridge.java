package io.aurigraph.v11.compliance.bridge;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.aurigraph.v11.compliance.erc3643.IdentityRegistry;
import io.aurigraph.v11.compliance.erc3643.TransferManager;
import io.aurigraph.v11.compliance.erc3643.IdentityVerification;
import io.aurigraph.v11.compliance.persistence.ComplianceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ERC-3643 Smart Contract Bridge
 * Provides bridge between Java compliance engine and Solidity ERC-3643 smart contracts
 * Allows bidirectional communication and state synchronization
 */
@ApplicationScoped
public class ERC3643ContractBridge {

    @Inject
    IdentityRegistry identityRegistry;

    @Inject
    TransferManager transferManager;

    @Inject
    ComplianceRepository complianceRepository;

    // Mapping of Solidity contract addresses to Java token IDs
    private final Map<String, String> contractTokenMapping = new ConcurrentHashMap<>();

    // Pending contract operations
    private final Map<String, ContractOperation> pendingOperations = new ConcurrentHashMap<>();

    // Bridge statistics
    private volatile long contractCallsReceived = 0;
    private volatile long contractCallsProcessed = 0;
    private volatile long syncErrors = 0;

    /**
     * Register a Solidity contract with the bridge
     */
    public void registerContract(String contractAddress, String tokenId) {
        contractTokenMapping.put(contractAddress, tokenId);
        Log.infof("Registered contract %s for token %s", contractAddress, tokenId);
    }

    /**
     * Process transfer approval request from Solidity contract
     */
    public ContractResponse processTransferApproval(String contractAddress, String from, String to, BigDecimal amount) {
        contractCallsReceived++;

        String tokenId = contractTokenMapping.get(contractAddress);
        if (tokenId == null) {
            syncErrors++;
            return new ContractResponse(false, "Contract not registered", null);
        }

        Log.infof("Processing transfer approval from contract %s: %s -> %s, amount: %s",
            contractAddress, from, to, amount);

        try {
            // Check compliance
            var transferResult = transferManager.canTransfer(tokenId, from, to, amount);

            if (transferResult.isAllowed()) {
                contractCallsProcessed++;
                return new ContractResponse(
                    true,
                    "Transfer approved",
                    Map.of(
                        "nonce", UUID.randomUUID().toString(),
                        "timestamp", Instant.now().getEpochSecond(),
                        "violations", transferResult.getViolations()
                    )
                );
            } else {
                return new ContractResponse(
                    false,
                    "Transfer rejected: " + String.join(", ", transferResult.getViolations()),
                    null
                );
            }
        } catch (Exception e) {
            syncErrors++;
            Log.errorf("Error processing transfer approval: %s", e.getMessage());
            return new ContractResponse(false, "Error: " + e.getMessage(), null);
        }
    }

    /**
     * Sync identity verification from Solidity contract
     */
    public ContractResponse syncIdentityVerification(String contractAddress, String address, Map<String, String> identityData) {
        contractCallsReceived++;

        String tokenId = contractTokenMapping.get(contractAddress);
        if (tokenId == null) {
            syncErrors++;
            return new ContractResponse(false, "Contract not registered", null);
        }

        Log.infof("Syncing identity verification from contract %s for address %s", contractAddress, address);

        try {
            // Create verification object from Solidity data
            var verification = mapSolidityIdentityData(identityData);

            // Register identity in Java
            var record = identityRegistry.registerIdentity(address, verification);

            contractCallsProcessed++;
            return new ContractResponse(
                true,
                "Identity registered",
                Map.of(
                    "address", record.getAddress(),
                    "kycLevel", record.getKycLevel(),
                    "registeredAt", record.getRegistrationDate().toString()
                )
            );
        } catch (Exception e) {
            syncErrors++;
            Log.errorf("Error syncing identity verification: %s", e.getMessage());
            return new ContractResponse(false, "Error: " + e.getMessage(), null);
        }
    }

    /**
     * Sync identity revocation from Solidity contract
     */
    public ContractResponse syncIdentityRevocation(String contractAddress, String address, String reason) {
        contractCallsReceived++;

        String tokenId = contractTokenMapping.get(contractAddress);
        if (tokenId == null) {
            syncErrors++;
            return new ContractResponse(false, "Contract not registered", null);
        }

        Log.infof("Syncing identity revocation from contract %s for address %s", contractAddress, address);

        try {
            boolean success = identityRegistry.revokeIdentity(address, reason);

            if (success) {
                contractCallsProcessed++;
                return new ContractResponse(
                    true,
                    "Identity revoked",
                    Map.of("address", address, "reason", reason)
                );
            } else {
                return new ContractResponse(
                    false,
                    "Identity not found",
                    null
                );
            }
        } catch (Exception e) {
            syncErrors++;
            Log.errorf("Error syncing identity revocation: %s", e.getMessage());
            return new ContractResponse(false, "Error: " + e.getMessage(), null);
        }
    }

    /**
     * Sync country restriction from Solidity contract
     */
    public ContractResponse syncCountryRestriction(String contractAddress, String countryCode, boolean restrict) {
        contractCallsReceived++;

        String tokenId = contractTokenMapping.get(contractAddress);
        if (tokenId == null) {
            syncErrors++;
            return new ContractResponse(false, "Contract not registered", null);
        }

        Log.infof("Syncing country %s restriction from contract %s: %s",
            countryCode, contractAddress, restrict ? "restricted" : "unrestricted");

        try {
            if (restrict) {
                identityRegistry.restrictCountry(countryCode);
            } else {
                identityRegistry.unrestrictCountry(countryCode);
            }

            contractCallsProcessed++;
            return new ContractResponse(
                true,
                "Country restriction updated",
                Map.of("countryCode", countryCode, "restricted", restrict)
            );
        } catch (Exception e) {
            syncErrors++;
            Log.errorf("Error syncing country restriction: %s", e.getMessage());
            return new ContractResponse(false, "Error: " + e.getMessage(), null);
        }
    }

    /**
     * Get contract state for Solidity synchronization
     */
    public Map<String, Object> getContractState(String contractAddress) {
        String tokenId = contractTokenMapping.get(contractAddress);
        if (tokenId == null) {
            return Map.of("error", "Contract not registered");
        }

        var complianceEntity = complianceRepository.findByTokenId(tokenId).orElse(null);

        return Map.of(
            "contractAddress", contractAddress,
            "tokenId", tokenId,
            "complianceStatus", complianceEntity != null ? complianceEntity.complianceStatus : "UNKNOWN",
            "jurisdiction", complianceEntity != null ? complianceEntity.jurisdiction : "UNKNOWN",
            "syncTimestamp", Instant.now().getEpochSecond()
        );
    }

    /**
     * Store pending contract operation for async processing
     */
    public String createPendingOperation(String contractAddress, String operationType, Map<String, Object> operationData) {
        String operationId = UUID.randomUUID().toString();

        ContractOperation operation = new ContractOperation(
            operationId,
            contractAddress,
            operationType,
            operationData,
            ContractOperationStatus.PENDING,
            Instant.now()
        );

        pendingOperations.put(operationId, operation);
        Log.infof("Created pending operation %s of type %s", operationId, operationType);

        return operationId;
    }

    /**
     * Get pending operation status
     */
    public ContractOperation getPendingOperation(String operationId) {
        return pendingOperations.get(operationId);
    }

    /**
     * Mark operation as completed
     */
    public void completeOperation(String operationId, ContractOperationStatus status, String result) {
        ContractOperation operation = pendingOperations.get(operationId);
        if (operation != null) {
            operation.setStatus(status);
            operation.setResult(result);
            operation.setCompletedAt(Instant.now());
            Log.infof("Completed operation %s with status %s", operationId, status);
        }
    }

    /**
     * Map Solidity identity data to Java IdentityVerification
     */
    private IdentityVerification mapSolidityIdentityData(
        Map<String, String> solidityData) {

        IdentityVerification verification =
            new IdentityVerification();

        verification.setAddress(solidityData.getOrDefault("address", ""));
        verification.setFirstName(solidityData.getOrDefault("firstName", ""));
        verification.setLastName(solidityData.getOrDefault("lastName", ""));
        verification.setCountry(solidityData.getOrDefault("country", "US"));
        verification.setDocumentHash(solidityData.getOrDefault("documentHash", ""));
        verification.setDocumentType(solidityData.getOrDefault("documentType", "PASSPORT"));
        verification.setKycLevel(solidityData.getOrDefault("kycLevel", "ENHANCED"));
        verification.setVerifierName(solidityData.getOrDefault("verifier", "UNKNOWN"));

        return verification;
    }

    /**
     * Get bridge statistics
     */
    public BridgeStats getStats() {
        return new BridgeStats(
            contractTokenMapping.size(),
            pendingOperations.size(),
            contractCallsReceived,
            contractCallsProcessed,
            syncErrors
        );
    }

    // Inner classes

    public static class ContractResponse {
        private final boolean success;
        private final String message;
        private final Map<String, Object> data;

        public ContractResponse(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    public enum ContractOperationStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }

    public static class ContractOperation {
        private final String operationId;
        private final String contractAddress;
        private final String operationType;
        private final Map<String, Object> operationData;
        private ContractOperationStatus status;
        private String result;
        private final Instant createdAt;
        private Instant completedAt;

        public ContractOperation(String operationId, String contractAddress, String operationType,
                                Map<String, Object> operationData, ContractOperationStatus status, Instant createdAt) {
            this.operationId = operationId;
            this.contractAddress = contractAddress;
            this.operationType = operationType;
            this.operationData = operationData;
            this.status = status;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public String getOperationId() { return operationId; }
        public String getContractAddress() { return contractAddress; }
        public String getOperationType() { return operationType; }
        public Map<String, Object> getOperationData() { return operationData; }
        public ContractOperationStatus getStatus() { return status; }
        public void setStatus(ContractOperationStatus status) { this.status = status; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getCompletedAt() { return completedAt; }
        public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    }

    public static class BridgeStats {
        private final int registeredContracts;
        private final int pendingOperations;
        private final long contractCallsReceived;
        private final long contractCallsProcessed;
        private final long syncErrors;

        public BridgeStats(int registeredContracts, int pendingOperations, long contractCallsReceived,
                          long contractCallsProcessed, long syncErrors) {
            this.registeredContracts = registeredContracts;
            this.pendingOperations = pendingOperations;
            this.contractCallsReceived = contractCallsReceived;
            this.contractCallsProcessed = contractCallsProcessed;
            this.syncErrors = syncErrors;
        }

        public int getRegisteredContracts() { return registeredContracts; }
        public int getPendingOperations() { return pendingOperations; }
        public long getContractCallsReceived() { return contractCallsReceived; }
        public long getContractCallsProcessed() { return contractCallsProcessed; }
        public long getSyncErrors() { return syncErrors; }
    }
}
