package io.aurigraph.v11.bridge.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Cosmos REST Support for CosmosChainAdapter
 *
 * Provides functions to:
 * - Query account balances via REST API
 * - Get account sequence and nonce
 * - Submit transactions
 * - Query contract state
 * - Parse Cosmos addresses and denominations
 *
 * Supports: Cosmos Hub, Osmosis, Akash, and other Cosmos-based chains
 *
 * Phase 11.4: Cosmos Integration
 *
 * @author Claude Code
 * @version 1.0.0
 */
public class CosmosRESTSupport {

    private static final Logger logger = LoggerFactory.getLogger(CosmosRESTSupport.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Cosmos denomination types
    public static final String UATOM = "uatom";      // Cosmos Hub
    public static final String UOSMO = "uosmo";      // Osmosis
    public static final String UAKT = "uakt";        // Akash
    public static final String UJUNO = "ujuno";      // Juno
    public static final String USTARS = "ustars";    // Stargaze

    /**
     * Account balance information
     */
    public static class AccountBalance {
        public String denom;
        public String amount;

        public AccountBalance(String denom, String amount) {
            this.denom = denom;
            this.amount = amount;
        }

        public BigDecimal getAmount() {
            try {
                return new BigDecimal(amount);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
    }

    /**
     * Account information
     */
    public static class AccountInfo {
        public String address;
        public long sequence;
        public String accountNumber;
        public List<AccountBalance> balances;
    }

    /**
     * Create OkHttp client for Cosmos REST API
     *
     * @param timeoutSeconds Request timeout in seconds
     * @return Configured OkHttpClient
     */
    public static OkHttpClient createCosmosHttpClient(int timeoutSeconds) {
        return new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build();
    }

    /**
     * Get account balance from Cosmos REST API
     *
     * Example URL: https://rest.cosmos.directory/cosmoshub/cosmos/bank/v1beta1/balances/{address}
     *
     * @param httpClient OkHttpClient instance
     * @param restUrl Base REST API URL
     * @param address Cosmos bech32 address
     * @return List of account balances
     * @throws IOException if request fails
     */
    public static List<AccountBalance> getAccountBalance(OkHttpClient httpClient, String restUrl, String address) throws IOException {
        List<AccountBalance> balances = new ArrayList<>();

        String url = restUrl + "/cosmos/bank/v1beta1/balances/" + address;
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to get balance: HTTP {}", response.code());
                return balances;
            }

            String body = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(body);

            // Parse balances from response
            JsonNode balancesNode = root.path("balances");
            if (balancesNode.isArray()) {
                for (JsonNode balance : balancesNode) {
                    String denom = balance.path("denom").asText();
                    String amount = balance.path("amount").asText();
                    balances.add(new AccountBalance(denom, amount));
                }
            }
        }

        return balances;
    }

    /**
     * Get account information including sequence number
     *
     * @param httpClient OkHttpClient instance
     * @param restUrl Base REST API URL
     * @param address Cosmos bech32 address
     * @return AccountInfo with sequence and balances
     * @throws IOException if request fails
     */
    public static AccountInfo getAccountInfo(OkHttpClient httpClient, String restUrl, String address) throws IOException {
        AccountInfo info = new AccountInfo();
        info.address = address;
        info.balances = new ArrayList<>();

        String url = restUrl + "/cosmos/auth/v1beta1/accounts/" + address;
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to get account info: HTTP {}", response.code());
                return info;
            }

            String body = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(body);

            // Parse account info
            JsonNode account = root.path("account");
            info.sequence = account.path("sequence").asLong(0);
            info.accountNumber = account.path("account_number").asText("0");
        }

        return info;
    }

    /**
     * Submit transaction to Cosmos blockchain via REST API
     *
     * @param httpClient OkHttpClient instance
     * @param restUrl Base REST API URL
     * @param txJson Signed transaction as JSON
     * @return Transaction hash if successful, or error message
     * @throws IOException if request fails
     */
    public static String submitTransaction(OkHttpClient httpClient, String restUrl, String txJson) throws IOException {
        String url = restUrl + "/cosmos/tx/v1beta1/txs";

        Request request = new Request.Builder()
            .url(url)
            .post(okhttp3.RequestBody.create(txJson, okhttp3.MediaType.parse("application/json")))
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(body);

            if (!response.isSuccessful()) {
                String error = root.path("message").asText("Unknown error");
                logger.error("Transaction submission failed: {}", error);
                return null;
            }

            // Return transaction hash
            return root.path("tx_response").path("txhash").asText();
        }
    }

    /**
     * Get transaction status
     *
     * @param httpClient OkHttpClient instance
     * @param restUrl Base REST API URL
     * @param txHash Transaction hash
     * @return Transaction status object
     * @throws IOException if request fails
     */
    public static TransactionStatus getTransactionStatus(OkHttpClient httpClient, String restUrl, String txHash) throws IOException {
        TransactionStatus status = new TransactionStatus();
        status.txHash = txHash;

        String url = restUrl + "/cosmos/tx/v1beta1/txs/" + txHash;
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                status.confirmed = false;
                logger.error("Failed to get transaction status: HTTP {}", response.code());
                return status;
            }

            String body = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(body);

            JsonNode txResponse = root.path("tx_response");
            status.blockHeight = txResponse.path("height").asLong(0);
            status.confirmed = txResponse.path("code").asInt(-1) == 0;
            status.logs = txResponse.path("logs").asText();
        }

        return status;
    }

    /**
     * Query smart contract state (for Cosmos wasm chains)
     *
     * @param httpClient OkHttpClient instance
     * @param restUrl Base REST API URL
     * @param contractAddress Contract address
     * @param queryMsg Query message as JSON
     * @return Contract state response
     * @throws IOException if request fails
     */
    public static String queryContract(OkHttpClient httpClient, String restUrl, String contractAddress, String queryMsg) throws IOException {
        String encodedMsg = java.util.Base64.getEncoder().encodeToString(queryMsg.getBytes());
        String url = restUrl + "/cosmwasm/wasm/v1/contract/" + contractAddress + "/smart/" + encodedMsg;

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to query contract: HTTP {}", response.code());
                return null;
            }

            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * Validate Cosmos address format (bech32)
     *
     * @param address Address string
     * @param expectedPrefix Expected address prefix (e.g., "cosmos", "osmo")
     * @return true if valid Cosmos address
     */
    public static boolean isValidCosmosAddress(String address, String expectedPrefix) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        // Cosmos addresses are bech32 encoded
        // Format: {prefix}1{encoded_data}
        if (!address.startsWith(expectedPrefix + "1")) {
            return false;
        }

        // Length check: bech32 addresses typically 42-44 characters
        return address.length() >= 42 && address.length() <= 50;
    }

    /**
     * Get address prefix for specific Cosmos chain
     *
     * @param chainName Name of chain
     * @return Address prefix
     */
    public static String getAddressPrefix(String chainName) {
        switch (chainName.toLowerCase()) {
            case "cosmos":
            case "cosmoshub":
                return "cosmos";
            case "osmosis":
                return "osmo";
            case "akash":
                return "akash";
            case "juno":
                return "juno";
            case "stargaze":
                return "stars";
            default:
                return "cosmos";
        }
    }

    /**
     * Convert denomination to standard units
     *
     * @param amountInSmallestUnit Amount in smallest units (e.g., uatom)
     * @param decimals Number of decimal places
     * @return Amount in standard units as BigDecimal
     */
    public static BigDecimal toStandardUnits(String amountInSmallestUnit, int decimals) {
        try {
            BigDecimal amount = new BigDecimal(amountInSmallestUnit);
            BigDecimal divisor = BigDecimal.TEN.pow(decimals);
            return amount.divide(divisor);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Convert denomination from standard units to smallest units
     *
     * @param amountInStandardUnits Amount in standard units
     * @param decimals Number of decimal places
     * @return Amount in smallest units
     */
    public static String fromStandardUnits(BigDecimal amountInStandardUnits, int decimals) {
        BigDecimal multiplier = BigDecimal.TEN.pow(decimals);
        return amountInStandardUnits.multiply(multiplier).toBigInteger().toString();
    }

    /**
     * Transaction status information
     */
    public static class TransactionStatus {
        public String txHash;
        public long blockHeight;
        public boolean confirmed;
        public String logs;
    }
}
