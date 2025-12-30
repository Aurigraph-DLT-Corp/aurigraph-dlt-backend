package io.aurigraph.v11.bridge.services;

import io.aurigraph.v11.bridge.models.AtomicSwapRequest;
import io.aurigraph.v11.bridge.models.AtomicSwapResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HashTimeLockContract (HTLC)
 * Tests atomic swap lifecycle, hash computation, and secret management
 */
@DisplayName("Hash-Time-Locked Contract (HTLC) Tests")
public class HashTimeLockContractTest {

    private HashTimeLockContract htlc;
    private AtomicSwapRequest validRequest;

    @BeforeEach
    void setUp() {
        htlc = new HashTimeLockContract();

        // Create valid test request
        validRequest = AtomicSwapRequest.builder()
                .swapId("swap-001")
                .initiator("0x1111111111111111111111111111111111111111")
                .counterparty("0x2222222222222222222222222222222222222222")
                .sourceChain("ethereum")
                .targetChain("polygon")
                .tokenIn("ETH")
                .tokenOut("WETH")
                .amountIn(new BigDecimal("1.5"))
                .amountOut(new BigDecimal("1.4"))
                .hashAlgo("SHA256")
                .hashLock("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .refundAddress("0x3333333333333333333333333333333333333333")
                .timelock(300000L) // 5 minutes
                .build();
    }

    @Test
    @DisplayName("Initiate valid swap should create INITIATED state")
    void testInitiateValidSwap() {
        // When
        AtomicSwapResponse response = htlc.initiateSwap(validRequest);

        // Then
        assertNotNull(response);
        assertEquals("swap-001", response.getSwapId());
        assertEquals(AtomicSwapResponse.SwapStatus.INITIATED, response.getStatus());
        assertEquals("0x1111111111111111111111111111111111111111", response.getInitiatorAddress());
        assertEquals("0x2222222222222222222222222222222222222222", response.getCounterpartyAddress());
        assertEquals(new BigDecimal("1.5"), response.getAmountIn());
        assertNotNull(response.getExpiryTime());
        assertTrue(response.getExpiryTime().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Initiate swap with missing swapId should fail")
    void testInitiateSwapMissingSwapId() {
        // Given
        validRequest.setSwapId("");

        // When
        AtomicSwapResponse response = htlc.initiateSwap(validRequest);

        // Then
        assertNotNull(response);
        assertEquals(AtomicSwapResponse.SwapStatus.FAILED, response.getStatus());
        assertEquals("VALIDATION_FAILED", response.getErrorCode());
    }

    @Test
    @DisplayName("Initiate swap with invalid hash lock format should fail")
    void testInitiateSwapInvalidHashLock() {
        // Given
        validRequest.setHashLock("INVALID_HASH"); // Not valid hex

        // When
        AtomicSwapResponse response = htlc.initiateSwap(validRequest);

        // Then
        assertNotNull(response);
        assertEquals(AtomicSwapResponse.SwapStatus.FAILED, response.getStatus());
        assertEquals("INVALID_HASH", response.getErrorCode());
    }

    @Test
    @DisplayName("Lock funds should transition to LOCKED state")
    void testLockFunds() {
        // Given
        htlc.initiateSwap(validRequest);

        // When
        AtomicSwapResponse response = htlc.lockFunds(
                "swap-001",
                "0xlock123456789"
        );

        // Then
        assertNotNull(response);
        assertEquals(AtomicSwapResponse.SwapStatus.LOCKED, response.getStatus());
        assertEquals("0xlock123456789", response.getLockTransactionHash());
        assertNotNull(response.getLockTime());
    }

    @Test
    @DisplayName("Lock funds on non-existent swap should return error")
    void testLockFundsNonExistent() {
        // When
        AtomicSwapResponse response = htlc.lockFunds(
                "non-existent-swap",
                "0xlock123456789"
        );

        // Then
        assertNotNull(response);
        assertEquals(AtomicSwapResponse.SwapStatus.FAILED, response.getStatus());
        assertEquals("SWAP_NOT_FOUND", response.getErrorCode());
    }

    @Test
    @DisplayName("Reveal secret with matching hash should transition to REVEALED")
    void testRevealSecretMatching() {
        // Given - Create swap with known secret
        String secret = "test-secret-12345678901234567890123456";
        String sha256Hash = computeSHA256(secret);

        validRequest.setHashLock(sha256Hash);
        htlc.initiateSwap(validRequest);
        htlc.lockFunds("swap-001", "0xlock123456789");

        // When
        AtomicSwapResponse response = htlc.revealSecret("swap-001", secret);

        // Then
        assertNotNull(response);
        assertEquals(AtomicSwapResponse.SwapStatus.REVEALED, response.getStatus());
        assertEquals(secret, response.getSecret());
        assertNotNull(response.getRevealTime());
    }

    @Test
    @DisplayName("Reveal secret with non-matching hash should fail")
    void testRevealSecretNonMatching() {
        // Given
        htlc.initiateSwap(validRequest);
        htlc.lockFunds("swap-001", "0xlock123456789");

        // When
        AtomicSwapResponse response = htlc.revealSecret(
                "swap-001",
                "wrong-secret-987654321"
        );

        // Then
        assertNotNull(response);
        assertEquals(AtomicSwapResponse.SwapStatus.FAILED, response.getStatus());
        assertEquals("INVALID_SECRET", response.getErrorCode());
    }

    @Test
    @DisplayName("Reveal secret in non-LOCKED state should fail")
    void testRevealSecretInvalidState() {
        // Given
        htlc.initiateSwap(validRequest);
        // Swap is still in INITIATED state

        // When
        AtomicSwapResponse response = htlc.revealSecret(
                "swap-001",
                "any-secret"
        );

        // Then
        assertNotNull(response);
        assertEquals(AtomicSwapResponse.SwapStatus.FAILED, response.getStatus());
        assertEquals("INVALID_STATE", response.getErrorCode());
    }

    @Test
    @DisplayName("Complete swap from REVEALED state should transition to COMPLETED")
    void testCompleteSwap() {
        // Given - Set up full HTLC flow
        String secret = "test-secret-12345678901234567890123456";
        String sha256Hash = computeSHA256(secret);
        validRequest.setHashLock(sha256Hash);

        htlc.initiateSwap(validRequest);
        htlc.lockFunds("swap-001", "0xlock123456789");
        htlc.revealSecret("swap-001", secret);

        // When
        AtomicSwapResponse response = htlc.completeSwap("swap-001", "0xtarget987654321");

        // Then
        assertNotNull(response);
        assertEquals(AtomicSwapResponse.SwapStatus.COMPLETED, response.getStatus());
        assertEquals("0xtarget987654321", response.getTargetTransactionHash());
        assertNotNull(response.getCompletionTime());
        assertTrue(response.isCompleted());
    }

    @Test
    @DisplayName("Refund after timelock expiry should transition to REFUNDED")
    void testRefundAfterExpiry() {
        // Given
        validRequest.setTimelock(1L); // 1 millisecond (will expire immediately)
        htlc.initiateSwap(validRequest);
        htlc.lockFunds("swap-001", "0xlock123456789");

        // Wait for expiry
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        AtomicSwapResponse response = htlc.refundSwap("swap-001", "0xrefund123456789");

        // Then
        assertNotNull(response);
        assertEquals(AtomicSwapResponse.SwapStatus.REFUNDED, response.getStatus());
        assertEquals("0xrefund123456789", response.getRefundTransactionHash());
        assertNotNull(response.getRefundTime());
    }

    @Test
    @DisplayName("Get swap status with auto-expiry check")
    void testGetSwapStatusWithAutoExpiry() {
        // Given
        validRequest.setTimelock(1L); // Will expire immediately
        htlc.initiateSwap(validRequest);

        // Wait for expiry
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        AtomicSwapResponse response = htlc.getSwapStatus("swap-001");

        // Then
        assertNotNull(response);
        assertEquals(AtomicSwapResponse.SwapStatus.EXPIRED, response.getStatus());
        assertNotNull(response.getExpirationTime());
    }

    @Test
    @DisplayName("Generate secret should produce 64-char hex string")
    void testGenerateSecret() {
        // When
        String secret = htlc.generateSecret();

        // Then
        assertNotNull(secret);
        assertEquals(64, secret.length()); // 32 bytes = 64 hex chars
        assertTrue(secret.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("Compute SHA256 hash should produce valid hash")
    void testComputeSHA256Hash() {
        // Given
        String secret = "test-secret";

        // When
        String hash = htlc.computeHash(secret, "SHA256");

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA256 produces 64 hex chars
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("Compute SHA3 hash should produce valid hash")
    void testComputeSHA3Hash() {
        // Given
        String secret = "test-secret";

        // When
        String hash = htlc.computeHash(secret, "SHA3");

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA3-256 produces 64 hex chars
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("Compute hash with unsupported algorithm should return null")
    void testComputeHashUnsupportedAlgorithm() {
        // When
        String hash = htlc.computeHash("test-secret", "BLAKE3");

        // Then
        assertNull(hash);
    }

    @Test
    @DisplayName("Multiple swaps should maintain independent state")
    void testMultipleSwapsIndependentState() {
        // Given
        AtomicSwapRequest request2 = AtomicSwapRequest.builder()
                .swapId("swap-002")
                .initiator("0x4444444444444444444444444444444444444444")
                .counterparty("0x5555555555555555555555555555555555555555")
                .sourceChain("polygon")
                .targetChain("avalanche")
                .tokenIn("USDT")
                .tokenOut("USDC")
                .amountIn(new BigDecimal("1000.00"))
                .amountOut(new BigDecimal("999.00"))
                .hashAlgo("SHA256")
                .hashLock("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789")
                .refundAddress("0x6666666666666666666666666666666666666666")
                .timelock(300000L)
                .build();

        // When
        AtomicSwapResponse response1 = htlc.initiateSwap(validRequest);
        AtomicSwapResponse response2 = htlc.initiateSwap(request2);

        // Then
        assertNotEquals(response1.getSwapId(), response2.getSwapId());
        assertEquals("swap-001", response1.getSwapId());
        assertEquals("swap-002", response2.getSwapId());
        assertEquals(AtomicSwapResponse.SwapStatus.INITIATED, response1.getStatus());
        assertEquals(AtomicSwapResponse.SwapStatus.INITIATED, response2.getStatus());
    }

    @Test
    @DisplayName("Event audit trail should track all state transitions")
    void testEventAuditTrail() {
        // Given
        htlc.initiateSwap(validRequest);

        // When
        AtomicSwapResponse response = htlc.getSwapStatus("swap-001");

        // Then
        assertNotNull(response.getEvents());
        assertTrue(response.getEvents().size() > 0);
        assertTrue(response.getEvents().stream()
                .anyMatch(e -> "INITIATED".equals(e.getEventType())));
    }

    @Test
    @DisplayName("Get all swaps should return collection")
    void testGetAllSwaps() {
        // Given
        htlc.initiateSwap(validRequest);

        // When
        java.util.Collection<AtomicSwapResponse> swaps = htlc.getAllSwaps();

        // Then
        assertNotNull(swaps);
        assertEquals(1, swaps.size());
    }

    @Test
    @DisplayName("Clear state should remove all swaps")
    void testClearState() {
        // Given
        htlc.initiateSwap(validRequest);
        assertEquals(1, htlc.getAllSwaps().size());

        // When
        htlc.clearState();

        // Then
        assertEquals(0, htlc.getAllSwaps().size());
    }

    @Test
    @DisplayName("Get swap count by status")
    void testGetSwapCountByStatus() {
        // Given
        String secret = "test-secret-12345678901234567890123456";
        String sha256Hash = computeSHA256(secret);
        validRequest.setHashLock(sha256Hash);

        htlc.initiateSwap(validRequest);
        htlc.lockFunds("swap-001", "0xlock123456789");

        // When
        java.util.Map<String, Integer> counts = htlc.getSwapCountByStatus();

        // Then
        assertNotNull(counts);
        assertEquals(1, counts.get("LOCKED"));
    }

    /**
     * Helper method to compute SHA256 hash (for test setup)
     */
    private String computeSHA256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
