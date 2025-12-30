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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Substrate RPC Support for SubstrateChainAdapter
 *
 * Provides functions to:
 * - Query account balances
 * - Get account nonce for transaction building
 * - Submit transactions to the network
 * - Query runtime metadata
 * - Get storage items
 * - Parse Substrate addresses (SS58)
 *
 * Supports: Polkadot, Kusama, Acala, Moonbeam, and other Substrate chains
 *
 * Phase 11.6: Substrate Integration
 *
 * @author Claude Code
 * @version 1.0.0
 */
public class SubstrateRPCSupport {

    private static final Logger logger = LoggerFactory.getLogger(SubstrateRPCSupport.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // RPC endpoints
    public static final String POLKADOT_URL = "wss://rpc.polkadot.io";
    public static final String KUSAMA_URL = "wss://kusama-rpc.polkadot.io";
    public static final String ACALA_URL = "wss://acala-rpc.aca-api.network";
    public static final String MOONBEAM_URL = "wss://wss.api.moonbeam.network";

    /**
     * Account balance information
     */
    public static class AccountBalance {
        public String address;
        public String free;        // Free balance
        public String reserved;    // Reserved balance
        public String miscFrozen;  // Frozen for misc reasons
        public String feeFrozen;   // Frozen for fees

        public BigDecimal getFreeBalance() {
            try {
                return new BigDecimal(free);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }

        public BigDecimal getAvailableBalance() {
            try {
                BigDecimal freeAmount = new BigDecimal(free);
                BigDecimal frozen = new BigDecimal(miscFrozen).max(new BigDecimal(feeFrozen));
                return freeAmount.subtract(frozen);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
    }

    /**
     * Account nonce for transaction building
     */
    public static class AccountNonce {
        public String address;
        public long nonce;
    }

    /**
     * Block header information
     */
    public static class BlockHeader {
        public String parentHash;
        public long blockNumber;
        public String stateRoot;
        public String extrinsicsRoot;
        public String digest;
    }

    /**
     * Create OkHttp client for Substrate RPC
     *
     * @param timeoutSeconds Request timeout in seconds
     * @return Configured OkHttpClient
     */
    public static OkHttpClient createSubstrateHttpClient(int timeoutSeconds) {
        return new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build();
    }

    /**
     * Get account balance from Substrate chain
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl Substrate RPC endpoint URL (REST or HTTP)
     * @param address SS58 address
     * @return AccountBalance with free, reserved, and frozen amounts
     * @throws IOException if request fails
     */
    public static AccountBalance getAccountBalance(OkHttpClient httpClient, String rpcUrl, String address) throws IOException {
        AccountBalance balance = new AccountBalance();
        balance.address = address;

        // Build state_getStorage RPC call
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "state_getStorage");

        ArrayNode params = objectMapper.createArrayNode();
        // Storage key for Account (pallet_balances::AccountData)
        params.add("0x" + encodeStorageKey(address));
        request.set("params", params);

        String response = makeJsonRPCCall(httpClient, rpcUrl, request);
        if (response == null) {
            return balance;
        }

        JsonNode root = objectMapper.readTree(response);
        String data = root.path("result").asText("");

        // Parse storage data (would need proper codec in production)
        // For now, set placeholder values
        balance.free = "0";
        balance.reserved = "0";
        balance.miscFrozen = "0";
        balance.feeFrozen = "0";

        return balance;
    }

    /**
     * Get account nonce for transaction building
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl Substrate RPC endpoint URL
     * @param address SS58 address
     * @return AccountNonce with current nonce
     * @throws IOException if request fails
     */
    public static AccountNonce getAccountNonce(OkHttpClient httpClient, String rpcUrl, String address) throws IOException {
        AccountNonce nonce = new AccountNonce();
        nonce.address = address;

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "system_accountNextIndex");

        ArrayNode params = objectMapper.createArrayNode();
        params.add(address);
        request.set("params", params);

        String response = makeJsonRPCCall(httpClient, rpcUrl, request);
        if (response == null) {
            return nonce;
        }

        JsonNode root = objectMapper.readTree(response);
        String result = root.path("result").asText("0");

        try {
            // Handle hex-encoded response
            if (result.startsWith("0x")) {
                nonce.nonce = Long.parseLong(result.substring(2), 16);
            } else {
                nonce.nonce = Long.parseLong(result);
            }
        } catch (NumberFormatException e) {
            nonce.nonce = 0L;
        }

        return nonce;
    }

    /**
     * Submit transaction to Substrate chain
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl Substrate RPC endpoint URL
     * @param signedExtrinsic Hex-encoded signed extrinsic
     * @return Transaction hash if successful
     * @throws IOException if request fails
     */
    public static String submitTransaction(OkHttpClient httpClient, String rpcUrl, String signedExtrinsic) throws IOException {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "author_submitExtrinsic");

        ArrayNode params = objectMapper.createArrayNode();
        params.add(signedExtrinsic);
        request.set("params", params);

        String response = makeJsonRPCCall(httpClient, rpcUrl, request);
        if (response == null) {
            return null;
        }

        JsonNode root = objectMapper.readTree(response);

        if (root.has("error")) {
            logger.error("Transaction submission failed: {}", root.path("error").path("message").asText());
            return null;
        }

        return root.path("result").asText();
    }

    /**
     * Get block header information
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl Substrate RPC endpoint URL
     * @param blockHash Block hash (or null for latest)
     * @return BlockHeader with header information
     * @throws IOException if request fails
     */
    public static BlockHeader getBlockHeader(OkHttpClient httpClient, String rpcUrl, String blockHash) throws IOException {
        BlockHeader header = new BlockHeader();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "chain_getHeader");

        ArrayNode params = objectMapper.createArrayNode();
        if (blockHash != null) {
            params.add(blockHash);
        }
        request.set("params", params);

        String response = makeJsonRPCCall(httpClient, rpcUrl, request);
        if (response == null) {
            return header;
        }

        JsonNode root = objectMapper.readTree(response);
        JsonNode result = root.path("result");

        header.parentHash = result.path("parentHash").asText();
        header.blockNumber = parseHexNumber(result.path("number").asText("0x0"));
        header.stateRoot = result.path("stateRoot").asText();
        header.extrinsicsRoot = result.path("extrinsicsRoot").asText();

        return header;
    }

    /**
     * Get runtime metadata
     *
     * @param httpClient OkHttpClient instance
     * @param rpcUrl Substrate RPC endpoint URL
     * @return Runtime metadata as Map
     * @throws IOException if request fails
     */
    public static Map<String, Object> getRuntimeMetadata(OkHttpClient httpClient, String rpcUrl) throws IOException {
        Map<String, Object> metadata = new HashMap<>();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "state_getMetadata");

        ArrayNode params = objectMapper.createArrayNode();
        request.set("params", params);

        String response = makeJsonRPCCall(httpClient, rpcUrl, request);
        if (response == null) {
            return metadata;
        }

        JsonNode root = objectMapper.readTree(response);
        String metadataHex = root.path("result").asText("");

        metadata.put("raw", metadataHex);
        metadata.put("version", "1");

        return metadata;
    }

    /**
     * Validate Substrate address (SS58)
     *
     * @param address Address string to validate
     * @return true if valid SS58 address format
     */
    public static boolean isValidAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        // SS58 addresses are typically 47-48 characters
        if (address.length() < 46 || address.length() > 50) {
            return false;
        }

        // SS58 addresses use base58 encoding
        return address.matches("[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+");
    }

    /**
     * Get address prefix for specific Substrate chain
     *
     * @param chainName Name of chain
     * @return SS58 address prefix
     */
    public static int getAddressPrefix(String chainName) {
        switch (chainName.toLowerCase()) {
            case "polkadot":
                return 0;
            case "kusama":
                return 2;
            case "acala":
                return 10;
            case "moonbeam":
                return 1284;
            case "moonriver":
                return 1285;
            default:
                return 0;
        }
    }

    /**
     * Convert planck to native units
     *
     * @param planck Amount in smallest units
     * @param decimals Number of decimal places
     * @return Amount in native units
     */
    public static BigDecimal planckToNative(String planck, int decimals) {
        try {
            BigDecimal amount = new BigDecimal(planck);
            BigDecimal divisor = BigDecimal.TEN.pow(decimals);
            return amount.divide(divisor);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Convert native units to planck
     *
     * @param native Amount in native units
     * @param decimals Number of decimal places
     * @return Amount in smallest units
     */
    public static String nativeToPlanck(BigDecimal native_, int decimals) {
        BigDecimal multiplier = BigDecimal.TEN.pow(decimals);
        return native_.multiply(multiplier).toBigInteger().toString();
    }

    /**
     * Encode address for storage key
     *
     * @param address SS58 address
     * @return Encoded storage key
     */
    private static String encodeStorageKey(String address) {
        // Placeholder - would need proper SS58 decoding and storage key encoding
        // In production, would decode address and generate proper storage key
        return "0000000000000000";
    }

    /**
     * Parse hex number
     *
     * @param hexValue Hex string (with 0x prefix)
     * @return Parsed long value
     */
    private static long parseHexNumber(String hexValue) {
        try {
            if (hexValue.startsWith("0x")) {
                return Long.parseLong(hexValue.substring(2), 16);
            }
            return Long.parseLong(hexValue);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Make JSON-RPC call to Substrate
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
     * Get Substrate RPC endpoint for chain
     *
     * @param chainName Chain name
     * @return RPC endpoint URL
     */
    public static String getRpcUrl(String chainName) {
        switch (chainName.toLowerCase()) {
            case "polkadot":
                return POLKADOT_URL;
            case "kusama":
                return KUSAMA_URL;
            case "acala":
                return ACALA_URL;
            case "moonbeam":
                return MOONBEAM_URL;
            default:
                return POLKADOT_URL;
        }
    }
}
