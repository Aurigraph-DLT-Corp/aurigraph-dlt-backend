package io.aurigraph.v11.bridge.factory;

import io.aurigraph.v11.bridge.ChainAdapter;
import io.aurigraph.v11.bridge.adapter.*;
import io.aurigraph.v11.bridge.exception.BridgeException;
import io.aurigraph.v11.bridge.model.BridgeChainConfig;
import io.aurigraph.v11.bridge.repository.BridgeConfigurationRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChainAdapterFactory
 * Tests adapter creation, caching, and configuration loading
 */
@QuarkusTest
class ChainAdapterFactoryTest {

    @Mock
    private BridgeConfigurationRepository configRepository;

    @InjectMocks
    private ChainAdapterFactory factory;

    private BridgeChainConfig ethereumConfig;
    private BridgeChainConfig solanaConfig;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Setup Ethereum config
        ethereumConfig = new BridgeChainConfig();
        ethereumConfig.setChainName("ethereum");
        ethereumConfig.setChainId("1");
        ethereumConfig.setRpcUrl("https://eth-mainnet.example.com");
        ethereumConfig.setFamily(ChainFamily.EVM);
        ethereumConfig.setConfirmationsRequired(12);
        ethereumConfig.setMinBridgeAmount(BigDecimal.valueOf(0.1));
        ethereumConfig.setMaxBridgeAmount(BigDecimal.valueOf(1000.0));
        ethereumConfig.setMetadata(new HashMap<>());

        // Setup Solana config
        solanaConfig = new BridgeChainConfig();
        solanaConfig.setChainName("solana");
        solanaConfig.setChainId("mainnet-beta");
        solanaConfig.setRpcUrl("https://api.mainnet-beta.solana.com");
        solanaConfig.setFamily(ChainFamily.SOLANA);
        solanaConfig.setConfirmationsRequired(1);
        solanaConfig.setMinBridgeAmount(BigDecimal.valueOf(0.01));
        solanaConfig.setMaxBridgeAmount(BigDecimal.valueOf(10000.0));
        solanaConfig.setMetadata(new HashMap<>());
    }

    @Test
    void testGetAdapterCreatesWeb3jAdapterForEVM() throws Exception {
        // Arrange
        when(configRepository.findByChainName("ethereum"))
            .thenReturn(Optional.of(ethereumConfig));

        // Act
        ChainAdapter adapter = factory.getAdapter("ethereum");

        // Assert
        assertNotNull(adapter);
        assertInstanceOf(Web3jChainAdapter.class, adapter);
        assertEquals("1", adapter.getChainId());
    }

    @Test
    void testGetAdapterCreatesWeb3jAdapterForPolygon() throws Exception {
        // Arrange
        BridgeChainConfig polygonConfig = new BridgeChainConfig();
        polygonConfig.setChainName("polygon");
        polygonConfig.setChainId("137");
        polygonConfig.setRpcUrl("https://polygon-rpc.example.com");
        polygonConfig.setFamily(ChainFamily.EVM);
        polygonConfig.setConfirmationsRequired(256);
        polygonConfig.setMinBridgeAmount(BigDecimal.ZERO);
        polygonConfig.setMaxBridgeAmount(BigDecimal.TEN);
        polygonConfig.setMetadata(new HashMap<>());

        when(configRepository.findByChainName("polygon"))
            .thenReturn(Optional.of(polygonConfig));

        // Act
        ChainAdapter adapter = factory.getAdapter("polygon");

        // Assert
        assertNotNull(adapter);
        assertInstanceOf(Web3jChainAdapter.class, adapter);
        assertEquals("137", adapter.getChainId());
    }

    @Test
    void testGetAdapterCreatesSolanaAdapter() throws Exception {
        // Arrange
        when(configRepository.findByChainName("solana"))
            .thenReturn(Optional.of(solanaConfig));

        // Act
        ChainAdapter adapter = factory.getAdapter("solana");

        // Assert
        assertNotNull(adapter);
        assertInstanceOf(SolanaChainAdapter.class, adapter);
        assertEquals("mainnet-beta", adapter.getChainId());
    }

    @Test
    void testGetAdapterCreatesCosmosAdapter() throws Exception {
        // Arrange
        BridgeChainConfig cosmosConfig = new BridgeChainConfig();
        cosmosConfig.setChainName("cosmos");
        cosmosConfig.setChainId("cosmoshub-4");
        cosmosConfig.setRpcUrl("https://cosmos-rpc.example.com");
        cosmosConfig.setFamily(ChainFamily.COSMOS);
        cosmosConfig.setConfirmationsRequired(1);
        cosmosConfig.setMinBridgeAmount(BigDecimal.ZERO);
        cosmosConfig.setMaxBridgeAmount(BigDecimal.TEN);
        cosmosConfig.setMetadata(new HashMap<>());

        when(configRepository.findByChainName("cosmos"))
            .thenReturn(Optional.of(cosmosConfig));

        // Act
        ChainAdapter adapter = factory.getAdapter("cosmos");

        // Assert
        assertNotNull(adapter);
        assertInstanceOf(CosmosChainAdapter.class, adapter);
        assertEquals("cosmoshub-4", adapter.getChainId());
    }

    @Test
    void testGetAdapterCreatesSubstrateAdapter() throws Exception {
        // Arrange
        BridgeChainConfig substrateConfig = new BridgeChainConfig();
        substrateConfig.setChainName("polkadot");
        substrateConfig.setChainId("polkadot");
        substrateConfig.setRpcUrl("https://rpc.polkadot.io");
        substrateConfig.setFamily(ChainFamily.SUBSTRATE);
        substrateConfig.setConfirmationsRequired(5);
        substrateConfig.setMinBridgeAmount(BigDecimal.ZERO);
        substrateConfig.setMaxBridgeAmount(BigDecimal.TEN);
        substrateConfig.setMetadata(new HashMap<>());

        when(configRepository.findByChainName("polkadot"))
            .thenReturn(Optional.of(substrateConfig));

        // Act
        ChainAdapter adapter = factory.getAdapter("polkadot");

        // Assert
        assertNotNull(adapter);
        assertInstanceOf(SubstrateChainAdapter.class, adapter);
        assertEquals("polkadot", adapter.getChainId());
    }

    @Test
    void testGetAdapterCreatesLayer2Adapter() throws Exception {
        // Arrange
        BridgeChainConfig layer2Config = new BridgeChainConfig();
        layer2Config.setChainName("arbitrum");
        layer2Config.setChainId("42161");
        layer2Config.setRpcUrl("https://arb-mainnet.example.com");
        layer2Config.setFamily(ChainFamily.LAYER2);
        layer2Config.setConfirmationsRequired(1);
        layer2Config.setMinBridgeAmount(BigDecimal.ZERO);
        layer2Config.setMaxBridgeAmount(BigDecimal.TEN);
        layer2Config.setMetadata(new HashMap<>());

        when(configRepository.findByChainName("arbitrum"))
            .thenReturn(Optional.of(layer2Config));

        // Act
        ChainAdapter adapter = factory.getAdapter("arbitrum");

        // Assert
        assertNotNull(adapter);
        assertInstanceOf(Layer2ChainAdapter.class, adapter);
        assertEquals("42161", adapter.getChainId());
    }

    @Test
    void testGetAdapterCreatesUTXOAdapter() throws Exception {
        // Arrange
        BridgeChainConfig utxoConfig = new BridgeChainConfig();
        utxoConfig.setChainName("bitcoin");
        utxoConfig.setChainId("0");
        utxoConfig.setRpcUrl("https://bitcoin-rpc.example.com");
        utxoConfig.setFamily(ChainFamily.UTXO);
        utxoConfig.setConfirmationsRequired(6);
        utxoConfig.setMinBridgeAmount(BigDecimal.ZERO);
        utxoConfig.setMaxBridgeAmount(BigDecimal.TEN);
        utxoConfig.setMetadata(new HashMap<>());

        when(configRepository.findByChainName("bitcoin"))
            .thenReturn(Optional.of(utxoConfig));

        // Act
        ChainAdapter adapter = factory.getAdapter("bitcoin");

        // Assert
        assertNotNull(adapter);
        assertInstanceOf(UTXOChainAdapter.class, adapter);
        assertEquals("0", adapter.getChainId());
    }

    @Test
    void testGetAdapterCachesResult() throws Exception {
        // Arrange
        when(configRepository.findByChainName("ethereum"))
            .thenReturn(Optional.of(ethereumConfig));

        // Act - get adapter twice
        ChainAdapter adapter1 = factory.getAdapter("ethereum");
        ChainAdapter adapter2 = factory.getAdapter("ethereum");

        // Assert - should be same cached instance
        assertSame(adapter1, adapter2, "Should return same cached adapter instance");
        verify(configRepository, times(1)).findByChainName("ethereum");
    }

    @Test
    void testGetAdapterNormalizesChainName() throws Exception {
        // Arrange
        when(configRepository.findByChainName("ethereum"))
            .thenReturn(Optional.of(ethereumConfig));

        // Act - use different case
        ChainAdapter adapter1 = factory.getAdapter("ETHEREUM");
        ChainAdapter adapter2 = factory.getAdapter("Ethereum");
        ChainAdapter adapter3 = factory.getAdapter("ethereum");

        // Assert - all should use same cached instance
        assertSame(adapter1, adapter2);
        assertSame(adapter2, adapter3);
    }

    @Test
    void testGetAdapterThrowsForUnsupportedChain() throws Exception {
        // Arrange
        when(configRepository.findByChainName("unknown"))
            .thenReturn(Optional.empty());
        when(configRepository.findAllChainNames())
            .thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(Exception.class, () -> factory.getAdapter("unknown"));
    }

    @Test
    void testGetAdapterThrowsForNullChainName() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> factory.getAdapter(null));
    }

    @Test
    void testGetAdapterThrowsForEmptyChainName() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> factory.getAdapter(""));
    }

    @Test
    void testGetSupportedChains() {
        // Arrange
        List<String> expectedChains = List.of("ethereum", "polygon", "solana", "cosmos");
        when(configRepository.findAllChainNames()).thenReturn(expectedChains);

        // Act
        List<String> chains = factory.getSupportedChains();

        // Assert
        assertEquals(expectedChains, chains);
    }

    @Test
    void testGetSupportedChainsByFamily() {
        // Arrange
        List<String> evmChains = List.of("ethereum", "polygon", "arbitrum");
        when(configRepository.findChainNamesByFamily(ChainFamily.EVM))
            .thenReturn(evmChains);

        // Act
        List<String> chains = factory.getSupportedChainsByFamily(ChainFamily.EVM);

        // Assert
        assertEquals(evmChains, chains);
    }

    @Test
    void testInvalidateCacheForChain() throws Exception {
        // Arrange
        when(configRepository.findByChainName("ethereum"))
            .thenReturn(Optional.of(ethereumConfig));

        // Get and cache adapter
        ChainAdapter adapter1 = factory.getAdapter("ethereum");

        // Invalidate cache
        factory.invalidateCache("ethereum");

        // Get adapter again (should reload config)
        when(configRepository.findByChainName("ethereum"))
            .thenReturn(Optional.of(ethereumConfig));
        ChainAdapter adapter2 = factory.getAdapter("ethereum");

        // Assert - should be different instances after cache invalidation
        assertNotSame(adapter1, adapter2, "Should create new instance after cache invalidation");
    }

    @Test
    void testInvalidateAllCache() throws Exception {
        // Arrange
        when(configRepository.findByChainName("ethereum"))
            .thenReturn(Optional.of(ethereumConfig));
        when(configRepository.findByChainName("solana"))
            .thenReturn(Optional.of(solanaConfig));

        // Get and cache adapters
        ChainAdapter ethAdapter1 = factory.getAdapter("ethereum");
        ChainAdapter solAdapter1 = factory.getAdapter("solana");

        // Invalidate all caches
        factory.invalidateAllCache();

        // Get adapters again
        when(configRepository.findByChainName("ethereum"))
            .thenReturn(Optional.of(ethereumConfig));
        when(configRepository.findByChainName("solana"))
            .thenReturn(Optional.of(solanaConfig));
        ChainAdapter ethAdapter2 = factory.getAdapter("ethereum");
        ChainAdapter solAdapter2 = factory.getAdapter("solana");

        // Assert - should be different instances after cache invalidation
        assertNotSame(ethAdapter1, ethAdapter2);
        assertNotSame(solAdapter1, solAdapter2);
    }

    @Test
    void testIsChainSupported() throws Exception {
        // Arrange
        when(configRepository.findByChainName("ethereum"))
            .thenReturn(Optional.of(ethereumConfig));
        when(configRepository.findByChainName("unknown"))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertTrue(factory.isChainSupported("ethereum"));
        assertFalse(factory.isChainSupported("unknown"));
    }

    @Test
    void testGetChainConfiguration() throws Exception {
        // Arrange
        when(configRepository.findByChainName("ethereum"))
            .thenReturn(Optional.of(ethereumConfig));

        // Act
        BridgeChainConfig config = factory.getChainConfiguration("ethereum");

        // Assert
        assertNotNull(config);
        assertEquals("ethereum", config.getChainName());
        assertEquals("1", config.getChainId());
    }

    @Test
    void testGetStatistics() throws Exception {
        // Arrange
        when(configRepository.findByChainName("ethereum"))
            .thenReturn(Optional.of(ethereumConfig));
        when(configRepository.findByChainName("solana"))
            .thenReturn(Optional.of(solanaConfig));

        factory.getAdapter("ethereum");
        factory.getAdapter("solana");

        // Act
        var stats = factory.getStatistics();

        // Assert
        assertEquals(2, stats.get("cached_adapters"));
        assertEquals(2, stats.get("cached_configs"));
    }
}
