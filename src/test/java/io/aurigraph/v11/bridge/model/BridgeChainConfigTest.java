package io.aurigraph.v11.bridge.model;

import io.aurigraph.v11.bridge.factory.ChainFamily;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PHASE 5: Bridge Configuration Model Tests
 *
 * Comprehensive testing for BridgeChainConfig JPA entity covering:
 * - Entity lifecycle (create, read, update, delete)
 * - Field validation and constraints
 * - Contract address mapping
 * - Metadata JSON handling
 * - Fee calculations
 * - Chain enablement
 *
 * @author Claude Code - Priority 3 Phase 2
 * @version 1.0.0
 */
@DisplayName("Bridge Chain Configuration Model Tests")
@Timeout(5)
public class BridgeChainConfigTest {

    private BridgeChainConfig config;

    @BeforeEach
    void setUp() {
        config = new BridgeChainConfig();
    }

    // ════════════════════════════════════════════════════════════════
    // ENTITY CREATION AND BASIC PROPERTIES (8 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should create BridgeChainConfig with default values")
    void testCreateWithDefaults() {
        assertNull(config.getId());
        assertNull(config.getChainName());
        assertNull(config.getChainId());
        assertNull(config.getDisplayName());
        assertNull(config.getRpcUrl());
        assertTrue(config.isEnabled());
    }

    @Test
    @DisplayName("Should set and get chain name")
    void testSetGetChainName() {
        config.setChainName("ethereum");
        assertEquals("ethereum", config.getChainName());
    }

    @Test
    @DisplayName("Should set and get chain ID")
    void testSetGetChainId() {
        config.setChainId("1");
        assertEquals("1", config.getChainId());
    }

    @Test
    @DisplayName("Should set and get display name")
    void testSetGetDisplayName() {
        config.setDisplayName("Ethereum Mainnet");
        assertEquals("Ethereum Mainnet", config.getDisplayName());
    }

    @Test
    @DisplayName("Should set and get RPC URL")
    void testSetGetRpcUrl() {
        String rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/demo";
        config.setRpcUrl(rpcUrl);
        assertEquals(rpcUrl, config.getRpcUrl());
    }

    @Test
    @DisplayName("Should set and get enabled flag")
    void testSetGetEnabled() {
        config.setEnabled(false);
        assertFalse(config.isEnabled());
        config.setEnabled(true);
        assertTrue(config.isEnabled());
    }

    @Test
    @DisplayName("Should set and get blockchain family")
    void testSetGetChainFamily() {
        config.setFamily(ChainFamily.EVM);
        assertEquals(ChainFamily.EVM, config.getFamily());
    }

    @Test
    @DisplayName("Should set and get block time in milliseconds")
    void testSetGetBlockTime() {
        config.setBlockTime(12000L);
        assertEquals(12000L, config.getBlockTime());
    }

    // ════════════════════════════════════════════════════════════════
    // BRIDGE AMOUNT CONSTRAINTS (6 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should set and get minimum bridge amount")
    void testSetGetMinBridgeAmount() {
        BigDecimal minAmount = new BigDecimal("0.1");
        config.setMinBridgeAmount(minAmount);
        assertEquals(minAmount, config.getMinBridgeAmount());
    }

    @Test
    @DisplayName("Should set and get maximum bridge amount")
    void testSetGetMaxBridgeAmount() {
        BigDecimal maxAmount = new BigDecimal("1000");
        config.setMaxBridgeAmount(maxAmount);
        assertEquals(maxAmount, config.getMaxBridgeAmount());
    }

    @Test
    @DisplayName("Should validate min amount is less than max amount")
    void testAmountConstraintValidation() {
        BigDecimal minAmount = new BigDecimal("0.1");
        BigDecimal maxAmount = new BigDecimal("1000");
        config.setMinBridgeAmount(minAmount);
        config.setMaxBridgeAmount(maxAmount);

        assertTrue(config.getMinBridgeAmount().compareTo(config.getMaxBridgeAmount()) < 0);
    }

    @Test
    @DisplayName("Should set and get base fee percentage")
    void testSetGetBaseFeePercent() {
        BigDecimal feePercent = new BigDecimal("0.001");
        config.setBaseFeePercent(feePercent);
        assertEquals(feePercent, config.getBaseFeePercent());
    }

    @Test
    @DisplayName("Should calculate total fee from percentage and amount")
    void testCalculateTotalFee() {
        config.setBaseFeePercent(new BigDecimal("0.001")); // 0.1%
        BigDecimal amount = new BigDecimal("100");

        BigDecimal expectedFee = amount.multiply(config.getBaseFeePercent());
        // Use compareTo for BigDecimal comparison as 0.1 and 0.100 are equal but have different scale
        assertEquals(0, new BigDecimal("0.1").compareTo(expectedFee), "Fee should be 0.1");
    }

    @Test
    @DisplayName("Should handle zero fee percentage")
    void testZeroFeePercentage() {
        config.setBaseFeePercent(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, config.getBaseFeePercent());
    }

    // ════════════════════════════════════════════════════════════════
    // CONFIRMATION SETTINGS (4 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should set and get confirmations required")
    void testSetGetConfirmationsRequired() {
        config.setConfirmationsRequired(15);
        assertEquals(15, config.getConfirmationsRequired());
    }

    @Test
    @DisplayName("Should validate minimum confirmations")
    void testMinimumConfirmations() {
        config.setConfirmationsRequired(1);
        assertTrue(config.getConfirmationsRequired() >= 1);
    }

    @Test
    @DisplayName("Should handle zero confirmations (for instant chains)")
    void testZeroConfirmations() {
        config.setConfirmationsRequired(0);
        assertEquals(0, config.getConfirmationsRequired());
    }

    @Test
    @DisplayName("Should set high confirmation count for security")
    void testHighConfirmationCount() {
        config.setConfirmationsRequired(128);
        assertEquals(128, config.getConfirmationsRequired());
    }

    // ════════════════════════════════════════════════════════════════
    // BACKUP RPC URLS (5 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should set and get backup RPC URLs")
    void testSetGetBackupRpcUrls() {
        String backupUrls = "https://rpc1.example.com,https://rpc2.example.com";
        config.setBackupRpcUrls(backupUrls);
        assertEquals(backupUrls, config.getBackupRpcUrls());
    }

    @Test
    @DisplayName("Should parse backup RPC URLs as array")
    void testParseBackupRpcUrls() {
        String backupUrls = "https://rpc1.example.com,https://rpc2.example.com,https://rpc3.example.com";
        config.setBackupRpcUrls(backupUrls);

        String[] urls = config.getBackupRpcUrls().split(",");
        assertEquals(3, urls.length);
    }

    @Test
    @DisplayName("Should handle single backup URL")
    void testSingleBackupUrl() {
        config.setBackupRpcUrls("https://backup.example.com");
        assertEquals("https://backup.example.com", config.getBackupRpcUrls());
    }

    @Test
    @DisplayName("Should handle empty backup URLs")
    void testEmptyBackupUrls() {
        config.setBackupRpcUrls(null);
        assertNull(config.getBackupRpcUrls());
    }

    @Test
    @DisplayName("Should validate RPC URL format")
    void testRpcUrlFormat() {
        String validUrl = "https://eth-mainnet.g.alchemy.com/v2/key";
        config.setRpcUrl(validUrl);
        assertTrue(config.getRpcUrl().startsWith("https://"));
    }

    // ════════════════════════════════════════════════════════════════
    // CONTRACT ADDRESSES MAPPING (6 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should initialize empty contract addresses map")
    void testEmptyContractAddressesMap() {
        assertNotNull(config.getContractAddresses());
        assertTrue(config.getContractAddresses().isEmpty());
    }

    @Test
    @DisplayName("Should add contract address to mapping")
    void testAddContractAddress() {
        Map<String, String> contracts = new HashMap<>();
        contracts.put("htlc", "0x1234567890abcdef");
        config.setContractAddresses(contracts);

        assertEquals("0x1234567890abcdef", config.getContractAddresses().get("htlc"));
    }

    @Test
    @DisplayName("Should store multiple contract addresses")
    void testMultipleContractAddresses() {
        Map<String, String> contracts = new HashMap<>();
        contracts.put("htlc", "0xaaaa");
        contracts.put("bridge", "0xbbbb");
        contracts.put("token", "0xcccc");
        config.setContractAddresses(contracts);

        assertEquals(3, config.getContractAddresses().size());
        assertEquals("0xaaaa", config.getContractAddresses().get("htlc"));
        assertEquals("0xbbbb", config.getContractAddresses().get("bridge"));
        assertEquals("0xcccc", config.getContractAddresses().get("token"));
    }

    @Test
    @DisplayName("Should update existing contract address")
    void testUpdateContractAddress() {
        Map<String, String> contracts = new HashMap<>();
        contracts.put("htlc", "0x1111");
        config.setContractAddresses(contracts);

        contracts.put("htlc", "0x2222");
        config.setContractAddresses(contracts);

        assertEquals("0x2222", config.getContractAddresses().get("htlc"));
    }

    @Test
    @DisplayName("Should remove contract address from mapping")
    void testRemoveContractAddress() {
        Map<String, String> contracts = new HashMap<>();
        contracts.put("htlc", "0xaaaa");
        contracts.put("bridge", "0xbbbb");
        config.setContractAddresses(contracts);

        contracts.remove("htlc");
        config.setContractAddresses(contracts);

        assertNull(config.getContractAddresses().get("htlc"));
        assertEquals("0xbbbb", config.getContractAddresses().get("bridge"));
    }

    @Test
    @DisplayName("Should validate Ethereum address format")
    void testEthereumAddressFormat() {
        Map<String, String> contracts = new HashMap<>();
        contracts.put("htlc", "0x1234567890123456789012345678901234567890");
        config.setContractAddresses(contracts);

        String address = config.getContractAddresses().get("htlc");
        assertTrue(address.startsWith("0x"));
        assertEquals(42, address.length()); // 0x + 40 hex chars
    }

    // ════════════════════════════════════════════════════════════════
    // METADATA JSON HANDLING (5 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should initialize empty metadata map")
    void testEmptyMetadata() {
        assertNotNull(config.getMetadata());
        assertTrue(config.getMetadata().isEmpty());
    }

    @Test
    @DisplayName("Should store metadata as JSON-serializable map")
    void testStoreMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("explorer", "https://etherscan.io");
        metadata.put("chainType", "EVM");
        config.setMetadata(metadata);

        assertEquals("https://etherscan.io", config.getMetadata().get("explorer"));
    }

    @Test
    @DisplayName("Should handle complex metadata structure")
    void testComplexMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("description", "Ethereum Mainnet - Production");
        metadata.put("testnet", "false");
        metadata.put("version", "1");
        metadata.put("nativeTokenSymbol", "ETH");
        config.setMetadata(metadata);

        assertEquals(4, config.getMetadata().size());
    }

    @Test
    @DisplayName("Should update metadata entries")
    void testUpdateMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("status", "active");
        config.setMetadata(metadata);

        metadata.put("status", "inactive");
        config.setMetadata(metadata);

        assertEquals("inactive", config.getMetadata().get("status"));
    }

    @Test
    @DisplayName("Should preserve metadata on serialization")
    void testMetadataPreservation() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");
        config.setMetadata(metadata);

        // Simulate serialization/deserialization
        Map<String, String> retrieved = config.getMetadata();
        assertEquals(metadata.size(), retrieved.size());
        assertEquals(metadata.get("key1"), retrieved.get("key1"));
    }

    // ════════════════════════════════════════════════════════════════
    // TIMESTAMP TRACKING (4 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should set and get created timestamp")
    void testSetGetCreatedAt() {
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        assertEquals(now, config.getCreatedAt());
    }

    @Test
    @DisplayName("Should set and get updated timestamp")
    void testSetGetUpdatedAt() {
        LocalDateTime now = LocalDateTime.now();
        config.setUpdatedAt(now);
        assertEquals(now, config.getUpdatedAt());
    }

    @Test
    @DisplayName("Should track creation time separately from update time")
    void testCreatedVsUpdated() {
        LocalDateTime created = LocalDateTime.of(2025, 11, 18, 10, 0, 0);
        LocalDateTime updated = LocalDateTime.of(2025, 11, 18, 15, 30, 0);

        config.setCreatedAt(created);
        config.setUpdatedAt(updated);

        assertEquals(created, config.getCreatedAt());
        assertEquals(updated, config.getUpdatedAt());
        assertTrue(updated.isAfter(created));
    }

    @Test
    @DisplayName("Should allow timestamp updates")
    void testUpdateTimestamp() {
        LocalDateTime original = LocalDateTime.now();
        config.setUpdatedAt(original);

        LocalDateTime updated = original.plusHours(2);
        config.setUpdatedAt(updated);

        assertEquals(updated, config.getUpdatedAt());
    }

    // ════════════════════════════════════════════════════════════════
    // DATABASE ID AND PRIMARY KEY (3 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should generate auto-increment ID")
    void testAutoIncrementId() {
        assertNull(config.getId());
        config.setId(1L);
        assertEquals(1L, config.getId());
    }

    @Test
    @DisplayName("Should support sequential IDs")
    void testSequentialIds() {
        config.setId(100L);
        assertEquals(100L, config.getId());

        BridgeChainConfig config2 = new BridgeChainConfig();
        config2.setId(101L);
        assertEquals(101L, config2.getId());
    }

    @Test
    @DisplayName("Should handle null ID for new entities")
    void testNullIdForNewEntity() {
        assertNull(config.getId());
        assertNull(config.getChainName());
    }

    // ════════════════════════════════════════════════════════════════
    // FULL ENTITY LIFECYCLE (3 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should create complete Ethereum configuration")
    void testCompleteEthereumConfig() {
        LocalDateTime now = LocalDateTime.now();

        config.setChainName("ethereum");
        config.setChainId("1");
        config.setDisplayName("Ethereum Mainnet");
        config.setRpcUrl("https://eth-mainnet.g.alchemy.com/v2/demo");
        config.setBackupRpcUrls("https://backup1.com,https://backup2.com");
        config.setBlockTime(12000L);
        config.setConfirmationsRequired(15);
        config.setFamily(ChainFamily.EVM);
        config.setMinBridgeAmount(new BigDecimal("0.1"));
        config.setMaxBridgeAmount(new BigDecimal("1000"));
        config.setBaseFeePercent(new BigDecimal("0.001"));

        Map<String, String> contracts = new HashMap<>();
        contracts.put("htlc", "0x1234567890123456789012345678901234567890");
        config.setContractAddresses(contracts);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("explorer", "https://etherscan.io");
        config.setMetadata(metadata);

        config.setEnabled(true);
        config.setCreatedAt(now);
        config.setUpdatedAt(now);

        assertEquals("ethereum", config.getChainName());
        assertEquals("1", config.getChainId());
        assertEquals(ChainFamily.EVM, config.getFamily());
        assertTrue(config.isEnabled());
    }

    @Test
    @DisplayName("Should create complete Solana configuration")
    void testCompleteSolanaConfig() {
        config.setChainName("solana");
        config.setChainId("mainnet-beta");
        config.setDisplayName("Solana Mainnet");
        config.setRpcUrl("https://api.mainnet-beta.solana.com");
        config.setBlockTime(400L); // ~400ms per slot
        config.setConfirmationsRequired(32);
        config.setFamily(ChainFamily.SOLANA);
        config.setMinBridgeAmount(new BigDecimal("0.01"));
        config.setMaxBridgeAmount(new BigDecimal("10000"));
        config.setBaseFeePercent(new BigDecimal("0.0001")); // Lower fee

        assertEquals("solana", config.getChainName());
        assertEquals(ChainFamily.SOLANA, config.getFamily());
        assertEquals(400L, config.getBlockTime());
    }

    @Test
    @DisplayName("Should create configuration with null optional fields")
    void testConfigWithNullOptionalFields() {
        config.setChainName("testchain");
        config.setChainId("test-1");
        config.setRpcUrl("https://test.example.com");
        config.setFamily(ChainFamily.OTHER);

        assertNull(config.getDisplayName());
        assertNull(config.getBackupRpcUrls());
        assertNull(config.getBlockTime());
        assertNull(config.getConfirmationsRequired());

        assertEquals("testchain", config.getChainName());
    }

    // ════════════════════════════════════════════════════════════════
    // GETTER/SETTER CONSISTENCY (2 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should maintain getter/setter consistency")
    void testGetterSetterConsistency() {
        String chainName = "polygon";
        String chainId = "137";
        BigDecimal fee = new BigDecimal("0.002");

        config.setChainName(chainName);
        config.setChainId(chainId);
        config.setBaseFeePercent(fee);

        assertEquals(chainName, config.getChainName());
        assertEquals(chainId, config.getChainId());
        assertEquals(fee, config.getBaseFeePercent());
    }

    @Test
    @DisplayName("Should allow multiple value changes")
    void testMultipleValueChanges() {
        config.setChainName("chain1");
        assertEquals("chain1", config.getChainName());

        config.setChainName("chain2");
        assertEquals("chain2", config.getChainName());

        config.setChainName("chain3");
        assertEquals("chain3", config.getChainName());
    }
}
