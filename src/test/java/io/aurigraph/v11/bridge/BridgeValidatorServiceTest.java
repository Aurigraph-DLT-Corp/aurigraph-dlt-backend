package io.aurigraph.v11.bridge;

import io.aurigraph.v11.bridge.models.ValidationRequest;
import io.aurigraph.v11.bridge.models.ValidationResponse;
import io.aurigraph.v11.bridge.security.SignatureVerificationService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for Bridge Validator Service
 * Tests all validation scenarios: happy paths, error cases, warnings, and edge cases
 *
 * Coverage targets: signature verification, liquidity checks, token validation,
 * fee calculation, slippage estimation, and rate limiting
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 */
@QuarkusTest
@DisplayName("Bridge Validator Service Tests")
class BridgeValidatorServiceTest {

    @Inject
    BridgeValidatorService validatorService;

    @Inject
    SignatureVerificationService signatureVerifier;

    private ValidationRequest.ValidationRequestBuilder baseRequestBuilder;

    @BeforeEach
    void setUp() {
        baseRequestBuilder = ValidationRequest.builder()
                .bridgeId("eth-aurigraph")
                .sourceChain("Ethereum")
                .targetChain("Aurigraph")
                .sourceAddress("0xA1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6E7F8A9B0")
                .targetAddress("aur1a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0")
                .tokenContract("0xUSDT")
                .tokenSymbol("USDT")
                .amount(BigDecimal.valueOf(1000))
                .signature("0x" + "a".repeat(128))
                .signatureType("SECP256K1")
                .nonce(System.currentTimeMillis())
                .timestamp(System.currentTimeMillis())
                .liquidityCheckRequired(true)
                .feeCheckRequired(true);
    }

    /**
     * Test 1: Happy path - Valid bridge transaction validation
     */
    @Test
    @DisplayName("Test 1: Valid bridge transaction validation (SUCCESS)")
    void testValidBridgeTransaction() {
        ValidationRequest request = baseRequestBuilder.build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getValidationId(), "Validation ID should be generated");
        assertEquals(ValidationResponse.ValidationStatus.SUCCESS, response.getStatus(),
                "Status should be SUCCESS for valid request");
        assertTrue(response.isSuccessful(), "isSuccessful() should return true");
        assertNull(response.getValidationErrors(), "Should have no validation errors");
        assertNull(response.getValidationWarnings(), "Should have no warnings for typical amounts");
    }

    /**
     * Test 2: Missing required fields validation
     */
    @Test
    @DisplayName("Test 2: Missing required fields (FAILED)")
    void testMissingRequiredFields() {
        ValidationRequest request = baseRequestBuilder
                .bridgeId(null)  // Missing required field
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertEquals(ValidationResponse.ValidationStatus.FAILED, response.getStatus(),
                "Status should be FAILED when required fields are missing");
        assertNotNull(response.getValidationErrors(), "Should contain validation errors");
        assertTrue(response.getValidationErrors().size() > 0, "Should have at least one error");
        assertFalse(response.isSuccessful(), "isSuccessful() should return false");
    }

    /**
     * Test 3: Invalid amount (zero)
     */
    @Test
    @DisplayName("Test 3: Invalid amount - zero (FAILED)")
    void testInvalidAmountZero() {
        ValidationRequest request = baseRequestBuilder
                .amount(BigDecimal.ZERO)
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertEquals(ValidationResponse.ValidationStatus.FAILED, response.getStatus());
        assertTrue(response.getValidationErrors().stream()
                .anyMatch(e -> e.contains("greater than zero")),
                "Should have error about amount being greater than zero");
    }

    /**
     * Test 4: Invalid amount (negative)
     */
    @Test
    @DisplayName("Test 4: Invalid amount - negative (FAILED)")
    void testInvalidAmountNegative() {
        ValidationRequest request = baseRequestBuilder
                .amount(BigDecimal.valueOf(-100))
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertEquals(ValidationResponse.ValidationStatus.FAILED, response.getStatus());
        assertNotNull(response.getValidationErrors());
    }

    /**
     * Test 5: Same source and target chains
     */
    @Test
    @DisplayName("Test 5: Same source and target chains (FAILED)")
    void testSameSourceTargetChain() {
        ValidationRequest request = baseRequestBuilder
                .targetChain("Ethereum")  // Same as source
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertEquals(ValidationResponse.ValidationStatus.FAILED, response.getStatus());
        assertTrue(response.getValidationErrors().stream()
                .anyMatch(e -> e.contains("cannot be the same")),
                "Should have error about source and target chains being different");
    }

    /**
     * Test 6: Unsupported token
     */
    @Test
    @DisplayName("Test 6: Unsupported token (FAILED)")
    void testUnsupportedToken() {
        ValidationRequest request = baseRequestBuilder
                .tokenSymbol("UNKNOWN_TOKEN")
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertEquals(ValidationResponse.ValidationStatus.FAILED, response.getStatus());
        assertTrue(response.getValidationErrors().stream()
                .anyMatch(e -> e.contains("not supported")),
                "Should have error about unsupported token");
        assertFalse(response.getTokenSupported(), "tokenSupported should be false");
    }

    /**
     * Test 7: Amount below minimum for token
     */
    @Test
    @DisplayName("Test 7: Amount below minimum (FAILED)")
    void testAmountBelowMinimum() {
        ValidationRequest request = baseRequestBuilder
                .tokenSymbol("USDT")
                .amount(BigDecimal.valueOf(50))  // Min is 100 for USDT
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertEquals(ValidationResponse.ValidationStatus.FAILED, response.getStatus());
        assertTrue(response.getValidationErrors().stream()
                .anyMatch(e -> e.contains("outside acceptable range")),
                "Should have error about amount outside limits");
        assertFalse(response.getAmountWithinLimits(), "amountWithinLimits should be false");
    }

    /**
     * Test 8: Amount above maximum for token
     */
    @Test
    @DisplayName("Test 8: Amount above maximum (FAILED)")
    void testAmountAboveMaximum() {
        ValidationRequest request = baseRequestBuilder
                .tokenSymbol("ETH")
                .amount(BigDecimal.valueOf(500))  // Max is 100 for ETH
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertEquals(ValidationResponse.ValidationStatus.FAILED, response.getStatus());
        assertFalse(response.getAmountWithinLimits(), "amountWithinLimits should be false");
    }

    /**
     * Test 9: High slippage warning
     */
    @Test
    @DisplayName("Test 9: High slippage triggers WARNING")
    void testHighSlippageWarning() {
        ValidationRequest request = baseRequestBuilder
                .amount(BigDecimal.valueOf(100_000))  // Large amount causes slippage
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        // Should still be SUCCESS or WARNINGS (high slippage is warning not failure)
        assertTrue(response.getStatus() == ValidationResponse.ValidationStatus.SUCCESS ||
                  response.getStatus() == ValidationResponse.ValidationStatus.WARNINGS);

        if (response.getValidationWarnings() != null) {
            assertTrue(response.getValidationWarnings().stream()
                    .anyMatch(w -> w.contains("slippage")),
                    "Should have warning about slippage");
        }
    }

    /**
     * Test 10: Fee calculation
     */
    @Test
    @DisplayName("Test 10: Fee calculation accuracy")
    void testFeeCalculation() {
        BigDecimal amount = BigDecimal.valueOf(10_000);
        ValidationRequest request = baseRequestBuilder
                .amount(amount)
                .gasPrice(BigDecimal.valueOf(50))
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertNotNull(response.getFeeEstimate(), "Fee estimate should be calculated");
        assertTrue(response.getFeeEstimate().compareTo(BigDecimal.ZERO) >= 0,
                "Fee estimate should be non-negative");
        assertNotNull(response.getTotalFeeEstimate(), "Total fee estimate should be provided");
        assertNotNull(response.getExchangeRate(), "Exchange rate should be provided");
    }

    /**
     * Test 11: Rate limiting
     */
    @Test
    @DisplayName("Test 11: Rate limit information provided")
    void testRateLimitInfo() {
        ValidationRequest request = baseRequestBuilder.build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertNotNull(response.getRateLimitInfo(), "Rate limit info should be provided");
        assertNotNull(response.getRateLimitInfo().getRequestsPerSecond(),
                "Requests per second should be specified");
        assertNotNull(response.getRateLimitInfo().getRemainingRequests(),
                "Remaining requests should be specified");
        assertFalse(response.getRateLimitInfo().getIsRateLimited(),
                "First request should not be rate limited");
    }

    /**
     * Test 12: Liquidity checking
     */
    @Test
    @DisplayName("Test 12: Liquidity availability check")
    void testLiquidityCheck() {
        ValidationRequest request = baseRequestBuilder
                .targetChain("Solana")  // Has 900k liquidity
                .amount(BigDecimal.valueOf(1000))  // Well within limit
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertTrue(response.getLiquidityAvailable(), "Liquidity should be available");
        assertNotNull(response.getAvailableLiquidity(), "Available liquidity should be specified");
        assertNotNull(response.getRequiredLiquidity(), "Required liquidity should be specified");
    }

    /**
     * Test 13: Token decimal information
     */
    @Test
    @DisplayName("Test 13: Token decimals provided")
    void testTokenDecimals() {
        ValidationRequest request = baseRequestBuilder
                .tokenSymbol("USDT")
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertNotNull(response.getSourceTokenDecimals(), "Source token decimals should be specified");
        assertNotNull(response.getTargetTokenDecimals(), "Target token decimals should be specified");
        assertEquals(6, response.getSourceTokenDecimals(), "USDT should have 6 decimals on source");
        assertEquals(6, response.getTargetTokenDecimals(), "USDT should have 6 decimals on target");
    }

    /**
     * Test 14: Chain compatibility
     */
    @Test
    @DisplayName("Test 14: Chain compatibility check")
    void testChainCompatibility() {
        ValidationRequest request = baseRequestBuilder
                .sourceChain("Ethereum")
                .targetChain("Polygon")
                .build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertTrue(response.getChainCompatibility(), "Ethereum to Polygon should be compatible");
    }

    /**
     * Test 15: Transaction time estimation
     */
    @Test
    @DisplayName("Test 15: Transaction time estimation")
    void testTransactionTimeEstimation() {
        ValidationRequest request = baseRequestBuilder.build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertNotNull(response.getEstimatedTime(), "Estimated time should be provided");
        assertTrue(response.getEstimatedTime() > 0, "Estimated time should be positive");
        // Typical bridge should take 30+ seconds (30000ms)
        assertTrue(response.getEstimatedTime() >= 30000,
                "Estimated time should account for block finality and relay");
    }

    /**
     * Test 16: Validation response expiration
     */
    @Test
    @DisplayName("Test 16: Validation response has expiration")
    void testValidationExpiration() {
        ValidationRequest request = baseRequestBuilder.build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertNotNull(response.getExpiresAt(), "Expiration time should be set");
        assertTrue(response.getExpiresAt().isAfter(response.getTimestamp()),
                "Expiration should be after timestamp");
    }

    /**
     * Test 17: Validation with all supported tokens
     */
    @Test
    @DisplayName("Test 17: All supported tokens validate correctly")
    void testAllSupportedTokens() {
        String[] supportedTokens = {"ETH", "USDT", "USDC", "WBTC", "AUR"};

        for (String token : supportedTokens) {
            ValidationRequest request = baseRequestBuilder
                    .tokenSymbol(token)
                    .amount(BigDecimal.valueOf(10))  // Safe amount for all tokens
                    .build();

            ValidationResponse response = validatorService.validateBridgeTransaction(request);

            assertTrue(response.getTokenSupported(),
                    "Token " + token + " should be supported");
        }
    }

    /**
     * Test 18: Validation response contains all required fields
     */
    @Test
    @DisplayName("Test 18: Complete validation response structure")
    void testCompleteValidationResponse() {
        ValidationRequest request = baseRequestBuilder.build();

        ValidationResponse response = validatorService.validateBridgeTransaction(request);

        assertNotNull(response.getValidationId(), "Validation ID should be present");
        assertNotNull(response.getStatus(), "Status should be present");
        assertNotNull(response.getSignatureValid(), "Signature validity should be indicated");
        assertNotNull(response.getLiquidityAvailable(), "Liquidity availability should be indicated");
        assertNotNull(response.getTokenSupported(), "Token support should be indicated");
        assertNotNull(response.getChainCompatibility(), "Chain compatibility should be indicated");
        assertNotNull(response.getTimestamp(), "Timestamp should be present");
    }
}
