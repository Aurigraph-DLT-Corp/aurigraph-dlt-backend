package io.aurigraph.v11.smartcontract.sdk;

import io.aurigraph.v11.smartcontract.SmartContract;
import io.aurigraph.v11.smartcontract.ContractExecution;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Aurigraph Smart Contract SDK Client
 *
 * Java client library for interacting with Aurigraph DLT Smart Contract API.
 * Provides convenient methods for deploying, executing, and managing smart contracts.
 *
 * @version 11.2.1
 * @since 2025-10-12
 *
 * @example
 * <pre>
 * // Initialize client
 * AurigraphSDKClient client = new AurigraphSDKClient("https://dlt.aurigraph.io/api/v11");
 *
 * // Deploy contract
 * SmartContract contract = new SmartContract("MyContract", code, ContractLanguage.JAVA, "owner123");
 * SmartContract deployed = client.deployContract(contract).join();
 *
 * // Execute contract
 * Map<String, Object> params = Map.of("amount", 100, "recipient", "user456");
 * ContractExecution execution = client.executeContract(deployed.getContractId(), "transfer", params, "caller123").join();
 * </pre>
 */
public class AurigraphSDKClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Create SDK client with default configuration
     *
     * @param baseUrl Base URL of Aurigraph DLT API (e.g., "https://dlt.aurigraph.io/api/v11")
     */
    public AurigraphSDKClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Deploy a smart contract
     *
     * @param contract Contract to deploy
     * @return CompletableFuture with deployed contract
     */
    public CompletableFuture<SmartContract> deployContract(SmartContract contract) {
        try {
            String json = objectMapper.writeValueAsString(contract);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/contracts/deploy"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> contractData = (Map<String, Object>) result.get("contract");
                        return objectMapper.convertValue(contractData, SmartContract.class);
                    } catch (Exception e) {
                        throw new SDKException("Failed to parse deploy response", e);
                    }
                });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new SDKException("Failed to deploy contract", e));
        }
    }

    /**
     * Execute a smart contract method
     *
     * @param contractId Contract ID
     * @param method Method name
     * @param parameters Method parameters
     * @param caller Caller address
     * @return CompletableFuture with execution result
     */
    public CompletableFuture<ContractExecution> executeContract(
            String contractId,
            String method,
            Map<String, Object> parameters,
            String caller
    ) {
        try {
            Map<String, Object> request = Map.of(
                "method", method,
                "parameters", parameters,
                "caller", caller
            );
            String json = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/contracts/" + contractId + "/execute"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> executionData = (Map<String, Object>) result.get("execution");
                        return objectMapper.convertValue(executionData, ContractExecution.class);
                    } catch (Exception e) {
                        throw new SDKException("Failed to parse execution response", e);
                    }
                });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new SDKException("Failed to execute contract", e));
        }
    }

    /**
     * Get contract by ID
     *
     * @param contractId Contract ID
     * @return CompletableFuture with contract
     */
    public CompletableFuture<SmartContract> getContract(String contractId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/contracts/" + contractId))
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contractData = (Map<String, Object>) result.get("contract");
                    return objectMapper.convertValue(contractData, SmartContract.class);
                } catch (Exception e) {
                    throw new SDKException("Failed to parse contract response", e);
                }
            });
    }

    /**
     * List all contracts
     *
     * @return CompletableFuture with list of contracts
     */
    public CompletableFuture<List<SmartContract>> listContracts() {
        return listContracts(null);
    }

    /**
     * List contracts by owner
     *
     * @param owner Owner address (optional)
     * @return CompletableFuture with list of contracts
     */
    public CompletableFuture<List<SmartContract>> listContracts(String owner) {
        String url = baseUrl + "/contracts" + (owner != null ? "?owner=" + owner : "");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> contractsData = (List<Map<String, Object>>) result.get("contracts");
                    return contractsData.stream()
                        .map(data -> objectMapper.convertValue(data, SmartContract.class))
                        .toList();
                } catch (Exception e) {
                    throw new SDKException("Failed to parse contracts list response", e);
                }
            });
    }

    /**
     * Get contract execution history
     *
     * @param contractId Contract ID
     * @return CompletableFuture with list of executions
     */
    public CompletableFuture<List<ContractExecution>> getExecutionHistory(String contractId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/contracts/" + contractId + "/executions"))
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> executionsData = (List<Map<String, Object>>) result.get("executions");
                    return executionsData.stream()
                        .map(data -> objectMapper.convertValue(data, ContractExecution.class))
                        .toList();
                } catch (Exception e) {
                    throw new SDKException("Failed to parse execution history response", e);
                }
            });
    }

    /**
     * Pause contract execution
     *
     * @param contractId Contract ID
     * @return CompletableFuture with updated contract
     */
    public CompletableFuture<SmartContract> pauseContract(String contractId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/contracts/" + contractId + "/pause"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contractData = (Map<String, Object>) result.get("contract");
                    return objectMapper.convertValue(contractData, SmartContract.class);
                } catch (Exception e) {
                    throw new SDKException("Failed to parse pause response", e);
                }
            });
    }

    /**
     * Resume contract execution
     *
     * @param contractId Contract ID
     * @return CompletableFuture with updated contract
     */
    public CompletableFuture<SmartContract> resumeContract(String contractId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/contracts/" + contractId + "/resume"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contractData = (Map<String, Object>) result.get("contract");
                    return objectMapper.convertValue(contractData, SmartContract.class);
                } catch (Exception e) {
                    throw new SDKException("Failed to parse resume response", e);
                }
            });
    }

    /**
     * SDK Exception
     */
    public static class SDKException extends RuntimeException {
        public SDKException(String message) {
            super(message);
        }

        public SDKException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
