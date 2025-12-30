package io.aurigraph.v11.bridge.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Solana RPC Support for SolanaChainAdapter
 *
 * Provides functions to:
 * - Query account balances in lamports
 * - Get account information
 * - Submit transactions to the network
 * - Get recent blockhash for transaction building
 * - Query program state
 * - Parse Solana addresses (base58)
 *
 * Supports: Solana mainnet, testnet, devnet
 *
 * Phase 11.5: Solana Integration
 *
 * @author Claude Code
 * @version 1.0.0
 */
public class SolanaRPCSupport {

    private static final Logger logger = LoggerFactory.getLogger(SolanaRPCSupport.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Solana RPC endpoints
    public static final String MAINNET_URL = "https://api.mainnet-beta.solana.com";
    public static final String TESTNET_URL = "https://api.testnet.solana.com";
    public static final String DEVNET_URL = "https://api.devnet.solana.com";

    // Lamports conversion
    public static final long LAMPORTS_PER_SOL = 1_000_000_000L;

    /**
     * Account information response
     */
    public static class AccountInfo {
        public String address;
        public long lamports;
        public String owner;
        public boolean executable;
        public long rentEpoch;

        public BigDecimal getSolAmount() {
            return new BigDecimal(lamports).divide(new BigDecimal(LAMPORTS_PER_SOL));
        }
    }

    /**
     * Recent blockhash response
     */
    public static class RecentBlockhash {
        public String blockhash;
        public long lastValidBlockHeight;
    }

    /**
     * Create OkHttp client for Solana RPC
     *
     * @param timeoutSeconds Request timeout in seconds
     * @return Configured OkHttpClient
     */
    public static OkHttpClient createSolanaHttpClient(int timeoutSeconds) {
        return new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build();
    }

    /**
     * Get account balance from Solana via RPC
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl Solana RPC endpoint URL
     * @param publicKey Public key (base58) to query
     * @return Balance in lamports
     * @throws IOException if request fails
     */
    public static long getAccountBalance(OkHttpClient httpClient, String rpcUrl, String publicKey) throws IOException {
        // Build JSON-RPC request
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "getBalance");

        ArrayNode params = objectMapper.createArrayNode();
        params.add(publicKey);
        request.set("params", params);

        String response = makeJsonRPCCall(httpClient, rpcUrl, request);
        if (response == null) {
            return 0L;
        }

        JsonNode root = objectMapper.readTree(response);
        return root.path("result").path("value").asLong(0L);
    }

    /**
     * Get account information
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl Solana RPC endpoint URL
     * @param publicKey Public key (base58) to query
     * @return AccountInfo with lamports and metadata
     * @throws IOException if request fails
     */
    public static AccountInfo getAccountInfo(OkHttpClient httpClient, String rpcUrl, String publicKey) throws IOException {
        AccountInfo info = new AccountInfo();
        info.address = publicKey;

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "getAccountInfo");

        ArrayNode params = objectMapper.createArrayNode();
        params.add(publicKey);
        ObjectNode encoding = objectMapper.createObjectNode();
        encoding.put("encoding", "base64");
        params.add(encoding);
        request.set("params", params);

        String response = makeJsonRPCCall(httpClient, rpcUrl, request);
        if (response == null) {
            return info;
        }

        JsonNode root = objectMapper.readTree(response);
        JsonNode value = root.path("result").path("value");

        info.lamports = value.path("lamports").asLong(0L);
        info.owner = value.path("owner").asText();
        info.executable = value.path("executable").asBoolean(false);
        info.rentEpoch = value.path("rentEpoch").asLong(0L);

        return info;
    }

    /**
     * Get recent blockhash for transaction building
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl Solana RPC endpoint URL
     * @return RecentBlockhash with hash and validity
     * @throws IOException if request fails
     */
    public static RecentBlockhash getRecentBlockhash(OkHttpClient httpClient, String rpcUrl) throws IOException {
        RecentBlockhash result = new RecentBlockhash();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "getLatestBlockhash");

        ArrayNode params = objectMapper.createArrayNode();
        ObjectNode commitment = objectMapper.createObjectNode();
        commitment.put("commitment", "finalized");
        params.add(commitment);
        request.set("params", params);

        String response = makeJsonRPCCall(httpClient, rpcUrl, request);
        if (response == null) {
            return result;
        }

        JsonNode root = objectMapper.readTree(response);
        JsonNode value = root.path("result").path("value");

        result.blockhash = value.path("blockhash").asText();
        result.lastValidBlockHeight = value.path("lastValidBlockHeight").asLong(0L);

        return result;
    }

    /**
     * Submit transaction to Solana network
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl Solana RPC endpoint URL
     * @param signedTransaction Base58-encoded signed transaction
     * @return Transaction signature (hash)
     * @throws IOException if request fails
     */
    public static String sendTransaction(OkHttpClient httpClient, String rpcUrl, String signedTransaction) throws IOException {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "sendTransaction");

        ArrayNode params = objectMapper.createArrayNode();
        params.add(signedTransaction);
        ObjectNode options = objectMapper.createObjectNode();
        options.put("skipPreflight", false);
        params.add(options);
        request.set("params", params);

        String response = makeJsonRPCCall(httpClient, rpcUrl, request);
        if (response == null) {
            return null;
        }

        JsonNode root = objectMapper.readTree(response);
        JsonNode result = root.path("result");

        // Check for error
        if (root.has("error")) {
            logger.error("Transaction submission failed: {}", root.path("error").path("message").asText());
            return null;
        }

        return result.asText();
    }

    /**
     * Get transaction status
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl Solana RPC endpoint URL
     * @param signature Transaction signature
     * @return true if transaction is confirmed, false if pending/failed
     * @throws IOException if request fails
     */
    public static boolean isTransactionConfirmed(OkHttpClient httpClient, String rpcUrl, String signature) throws IOException {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "getSignatureStatus");

        ArrayNode params = objectMapper.createArrayNode();
        ArrayNode signatures = objectMapper.createArrayNode();
        signatures.add(signature);
        params.add(signatures);
        request.set("params", params);

        String response = makeJsonRPCCall(httpClient, rpcUrl, request);
        if (response == null) {
            return false;
        }

        JsonNode root = objectMapper.readTree(response);
        JsonNode value = root.path("result").path("value").get(0);

        if (value.isNull()) {
            return false; // Signature not found
        }

        return value.path("confirmationStatus").asText("").equals("finalized");
    }

    /**
     * Validate Solana address (base58)
     *
     * @param address Address string to validate
     * @return true if valid Solana address format
     */
    public static boolean isValidAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        // Solana addresses are base58 encoded
        // Typical length is 44 characters
        if (address.length() != 44) {
            return false;
        }

        // Check if only contains valid base58 characters
        return address.matches("[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{44}");
    }

    /**
     * Convert lamports to SOL
     *
     * @param lamports Amount in lamports
     * @return Amount in SOL as BigDecimal
     */
    public static BigDecimal lamportsToSol(long lamports) {
        return new BigDecimal(lamports).divide(new BigDecimal(LAMPORTS_PER_SOL));
    }

    /**
     * Convert SOL to lamports
     *
     * @param sol Amount in SOL
     * @return Amount in lamports
     */
    public static long solToLamports(BigDecimal sol) {
        return sol.multiply(new BigDecimal(LAMPORTS_PER_SOL)).longValue();
    }

    /**
     * Get Solana RPC endpoint URL for cluster
     *
     * @param cluster Cluster name (mainnet-beta, testnet, devnet)
     * @return RPC endpoint URL
     */
    public static String getRpcUrl(String cluster) {
        switch (cluster.toLowerCase()) {
            case "mainnet":
            case "mainnet-beta":
                return MAINNET_URL;
            case "testnet":
                return TESTNET_URL;
            case "devnet":
                return DEVNET_URL;
            default:
                return MAINNET_URL;
        }
    }

    /**
     * Make JSON-RPC call to Solana
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl RPC endpoint URL
     * @param request JSON-RPC request object
     * @return Response body as string
     */
    private static String makeJsonRPCCall(OkHttpClient httpClient, String rpcUrl, ObjectNode request) {
        try {
            String body = objectMapper.writeValueAsString(request);

            Request httpRequest = new Request.Builder()
                .url(rpcUrl)
                .post(okhttp3.RequestBody.create(body, okhttp3.MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("RPC call failed: HTTP {}", response.code());
                    return null;
                }

                return response.body() != null ? response.body().string() : null;
            }
        } catch (IOException e) {
            logger.error("Error making RPC call", e);
            return null;
        }
    }

    /**
     * Get SOL price in USD (for display purposes)
     *
     * @param httpClient OkHttpClient instance
     * @return SOL/USD price as BigDecimal
     */
    public static BigDecimal getSolPrice(OkHttpClient httpClient) {
        // In production, would call a price oracle like CoinGecko or similar
        // For now, return placeholder
        return new BigDecimal("20.00");
    }
}
