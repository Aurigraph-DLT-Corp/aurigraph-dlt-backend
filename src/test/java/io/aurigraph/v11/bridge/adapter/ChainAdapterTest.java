package io.aurigraph.v11.bridge.adapter;

import io.aurigraph.v11.bridge.ChainAdapter;
import io.aurigraph.v11.bridge.exception.BridgeException;
import io.aurigraph.v11.bridge.model.BridgeChainConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for all ChainAdapter implementations
 * Tests basic adapter functionality across all families
 */
@QuarkusTest
class ChainAdapterTest {

    private BridgeChainConfig config;

    @BeforeEach
    void setUp() {
        config = new BridgeChainConfig();
        config.setChainName("test-chain");
        config.setChainId("test-1");
        config.setRpcUrl("https://rpc.example.com");
        config.setConfirmationsRequired(1);
        config.setMinBridgeAmount(BigDecimal.ZERO);
        config.setMaxBridgeAmount(BigDecimal.TEN);
        config.setMetadata(new HashMap<>());
    }

    // ==================== Web3jChainAdapter Tests ====================

    @Test
    void testWeb3jAdapterInitialization() throws BridgeException {
        Web3jChainAdapter adapter = new Web3jChainAdapter();
        adapter.initialize(config);

        assertEquals("test-1", adapter.getChainId());
        assertNotNull(adapter.getConfig());
    }

    @Test
    void testWeb3jAdapterGetChainInfo() throws BridgeException {
        Web3jChainAdapter adapter = new Web3jChainAdapter();
        adapter.initialize(config);

        UniAssertSubscriber<ChainAdapter.ChainInfo> subscriber =
            adapter.getChainInfo().subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.ChainInfo info = subscriber.getItem();
        assertNotNull(info);
        assertEquals("test-1", info.chainId);
        assertEquals("test-chain", info.chainName);
        assertEquals("ETH", info.nativeCurrency);
    }

    @Test
    void testWeb3jAdapterCheckConnection() throws BridgeException {
        Web3jChainAdapter adapter = new Web3jChainAdapter();
        adapter.initialize(config);

        UniAssertSubscriber<ChainAdapter.ConnectionStatus> subscriber =
            adapter.checkConnection().subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.ConnectionStatus status = subscriber.getItem();
        assertNotNull(status);
        assertTrue(status.isConnected);
    }

    @Test
    void testWeb3jAdapterValidateAddress() throws BridgeException {
        Web3jChainAdapter adapter = new Web3jChainAdapter();
        adapter.initialize(config);

        // Valid Ethereum address
        UniAssertSubscriber<ChainAdapter.AddressValidationResult> subscriber =
            adapter.validateAddress("0x1234567890123456789012345678901234567890")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.AddressValidationResult result = subscriber.getItem();
        assertTrue(result.isValid);

        // Invalid address
        subscriber = adapter.validateAddress("invalid").subscribe()
            .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        result = subscriber.getItem();
        assertFalse(result.isValid);
    }

    @Test
    void testWeb3jAdapterGetCurrentBlockHeight() throws BridgeException {
        Web3jChainAdapter adapter = new Web3jChainAdapter();
        adapter.initialize(config);

        UniAssertSubscriber<Long> subscriber =
            adapter.getCurrentBlockHeight().subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        Long blockHeight = subscriber.getItem();
        assertNotNull(blockHeight);
    }

    // ==================== SolanaChainAdapter Tests ====================

    @Test
    void testSolanaAdapterInitialization() throws BridgeException {
        SolanaChainAdapter adapter = new SolanaChainAdapter();
        adapter.initialize(config);

        assertEquals("test-1", adapter.getChainId());
    }

    @Test
    void testSolanaAdapterGetChainInfo() throws BridgeException {
        SolanaChainAdapter adapter = new SolanaChainAdapter();
        config.setChainName("solana");
        adapter.initialize(config);

        UniAssertSubscriber<ChainAdapter.ChainInfo> subscriber =
            adapter.getChainInfo().subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.ChainInfo info = subscriber.getItem();
        assertEquals("SOL", info.nativeCurrency);
        assertEquals(9, info.decimals);
    }

    @Test
    void testSolanaAdapterValidateAddress() throws BridgeException {
        SolanaChainAdapter adapter = new SolanaChainAdapter();
        adapter.initialize(config);

        // Valid Solana address (44 chars Base58)
        String validAddr = "1111111111111111111111111111111111111111111";
        UniAssertSubscriber<ChainAdapter.AddressValidationResult> subscriber =
            adapter.validateAddress(validAddr).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.AddressValidationResult result = subscriber.getItem();
        assertTrue(result.isValid);

        // Invalid address
        subscriber = adapter.validateAddress("short").subscribe()
            .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        result = subscriber.getItem();
        assertFalse(result.isValid);
    }

    // ==================== CosmosChainAdapter Tests ====================

    @Test
    void testCosmosAdapterInitialization() throws BridgeException {
        CosmosChainAdapter adapter = new CosmosChainAdapter();
        adapter.initialize(config);

        assertEquals("test-1", adapter.getChainId());
    }

    @Test
    void testCosmosAdapterGetChainInfo() throws BridgeException {
        CosmosChainAdapter adapter = new CosmosChainAdapter();
        config.setChainName("cosmos");
        adapter.initialize(config);

        UniAssertSubscriber<ChainAdapter.ChainInfo> subscriber =
            adapter.getChainInfo().subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.ChainInfo info = subscriber.getItem();
        assertEquals("ATOM", info.nativeCurrency);
        assertEquals(6, info.decimals);
    }

    @Test
    void testCosmosAdapterValidateAddress() throws BridgeException {
        CosmosChainAdapter adapter = new CosmosChainAdapter();
        adapter.initialize(config);

        // Valid Cosmos address (Bech32 starting with cosmos)
        String validAddr = "cosmos1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqpxpdxy";
        UniAssertSubscriber<ChainAdapter.AddressValidationResult> subscriber =
            adapter.validateAddress(validAddr).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.AddressValidationResult result = subscriber.getItem();
        assertTrue(result.isValid);
    }

    // ==================== SubstrateChainAdapter Tests ====================

    @Test
    void testSubstrateAdapterInitialization() throws BridgeException {
        SubstrateChainAdapter adapter = new SubstrateChainAdapter();
        adapter.initialize(config);

        assertEquals("test-1", adapter.getChainId());
    }

    @Test
    void testSubstrateAdapterGetChainInfo() throws BridgeException {
        SubstrateChainAdapter adapter = new SubstrateChainAdapter();
        config.setChainName("polkadot");
        adapter.initialize(config);

        UniAssertSubscriber<ChainAdapter.ChainInfo> subscriber =
            adapter.getChainInfo().subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.ChainInfo info = subscriber.getItem();
        assertEquals("DOT", info.nativeCurrency);
        assertEquals(10, info.decimals);
    }

    @Test
    void testSubstrateAdapterValidateAddress() throws BridgeException {
        SubstrateChainAdapter adapter = new SubstrateChainAdapter();
        adapter.initialize(config);

        // Valid SS58 address (47-48 chars)
        String validAddr = "1QQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQ";
        UniAssertSubscriber<ChainAdapter.AddressValidationResult> subscriber =
            adapter.validateAddress(validAddr).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.AddressValidationResult result = subscriber.getItem();
        assertTrue(result.isValid);
    }

    // ==================== Layer2ChainAdapter Tests ====================

    @Test
    void testLayer2AdapterInitialization() throws BridgeException {
        Layer2ChainAdapter adapter = new Layer2ChainAdapter();
        adapter.initialize(config);

        assertEquals("test-1", adapter.getChainId());
    }

    @Test
    void testLayer2AdapterGetChainInfo() throws BridgeException {
        Layer2ChainAdapter adapter = new Layer2ChainAdapter();
        config.setChainName("arbitrum");
        adapter.initialize(config);

        UniAssertSubscriber<ChainAdapter.ChainInfo> subscriber =
            adapter.getChainInfo().subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.ChainInfo info = subscriber.getItem();
        assertEquals("ETH", info.nativeCurrency);
        assertTrue(info.supportsEIP1559);
    }

    @Test
    void testLayer2AdapterValidateAddress() throws BridgeException {
        Layer2ChainAdapter adapter = new Layer2ChainAdapter();
        adapter.initialize(config);

        // Valid Ethereum address
        UniAssertSubscriber<ChainAdapter.AddressValidationResult> subscriber =
            adapter.validateAddress("0x1234567890123456789012345678901234567890")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.AddressValidationResult result = subscriber.getItem();
        assertTrue(result.isValid);
    }

    // ==================== UTXOChainAdapter Tests ====================

    @Test
    void testUTXOAdapterInitialization() throws BridgeException {
        UTXOChainAdapter adapter = new UTXOChainAdapter();
        adapter.initialize(config);

        assertEquals("test-1", adapter.getChainId());
    }

    @Test
    void testUTXOAdapterGetChainInfo() throws BridgeException {
        UTXOChainAdapter adapter = new UTXOChainAdapter();
        config.setChainName("bitcoin");
        adapter.initialize(config);

        UniAssertSubscriber<ChainAdapter.ChainInfo> subscriber =
            adapter.getChainInfo().subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.ChainInfo info = subscriber.getItem();
        assertEquals("BTC", info.nativeCurrency);
        assertEquals(8, info.decimals);
        assertFalse(info.supportsEIP1559);
    }

    @Test
    void testUTXOAdapterValidateAddressP2PKH() throws BridgeException {
        UTXOChainAdapter adapter = new UTXOChainAdapter();
        adapter.initialize(config);

        // Valid P2PKH address (starts with 1)
        String validAddr = "1A1z7agoat4aGEg759QJCbjSQbCAh5gda";
        UniAssertSubscriber<ChainAdapter.AddressValidationResult> subscriber =
            adapter.validateAddress(validAddr).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.AddressValidationResult result = subscriber.getItem();
        assertTrue(result.isValid);
        assertEquals(ChainAdapter.AddressFormat.BITCOIN_P2PKH, result.format);
    }

    @Test
    void testUTXOAdapterValidateAddressP2SH() throws BridgeException {
        UTXOChainAdapter adapter = new UTXOChainAdapter();
        adapter.initialize(config);

        // Valid P2SH address (starts with 3)
        String validAddr = "3JZttKvsEsktxSo6UHuc3dFAReJxcfoHcF";
        UniAssertSubscriber<ChainAdapter.AddressValidationResult> subscriber =
            adapter.validateAddress(validAddr).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.AddressValidationResult result = subscriber.getItem();
        assertTrue(result.isValid);
        assertEquals(ChainAdapter.AddressFormat.BITCOIN_P2SH, result.format);
    }

    @Test
    void testUTXOAdapterValidateAddressBech32() throws BridgeException {
        UTXOChainAdapter adapter = new UTXOChainAdapter();
        adapter.initialize(config);

        // Valid Bech32 address (starts with bc1)
        String validAddr = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4";
        UniAssertSubscriber<ChainAdapter.AddressValidationResult> subscriber =
            adapter.validateAddress(validAddr).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.AddressValidationResult result = subscriber.getItem();
        assertTrue(result.isValid);
        assertEquals(ChainAdapter.AddressFormat.BITCOIN_BECH32, result.format);
    }

    // ==================== Common Adapter Tests ====================

    @Test
    void testAdapterShutdown() throws BridgeException {
        Web3jChainAdapter adapter = new Web3jChainAdapter();
        adapter.initialize(config);

        UniAssertSubscriber<Boolean> subscriber =
            adapter.shutdown().subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        Boolean result = subscriber.getItem();
        assertTrue(result);
    }

    @Test
    void testAdapterGetBalance() throws BridgeException {
        Web3jChainAdapter adapter = new Web3jChainAdapter();
        adapter.initialize(config);

        UniAssertSubscriber<BigDecimal> subscriber =
            adapter.getBalance("0x1234567890123456789012345678901234567890", "eth")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        BigDecimal balance = subscriber.getItem();
        assertNotNull(balance);
    }

    @Test
    void testAdapterEstimateFee() throws BridgeException {
        Web3jChainAdapter adapter = new Web3jChainAdapter();
        adapter.initialize(config);

        ChainAdapter.ChainTransaction tx = new ChainAdapter.ChainTransaction();
        tx.from = "0x1234567890123456789012345678901234567890";
        tx.to = "0x0987654321098765432109876543210987654321";
        tx.value = BigDecimal.ONE;

        UniAssertSubscriber<ChainAdapter.FeeEstimate> subscriber =
            adapter.estimateTransactionFee(tx).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.FeeEstimate estimate = subscriber.getItem();
        assertNotNull(estimate);
        assertNotNull(estimate.totalFee);
    }

    @Test
    void testAdapterGetNetworkFeeInfo() throws BridgeException {
        Web3jChainAdapter adapter = new Web3jChainAdapter();
        adapter.initialize(config);

        UniAssertSubscriber<ChainAdapter.NetworkFeeInfo> subscriber =
            adapter.getNetworkFeeInfo().subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        ChainAdapter.NetworkFeeInfo feeInfo = subscriber.getItem();
        assertNotNull(feeInfo);
        assertNotNull(feeInfo.standardGasPrice);
    }
}
