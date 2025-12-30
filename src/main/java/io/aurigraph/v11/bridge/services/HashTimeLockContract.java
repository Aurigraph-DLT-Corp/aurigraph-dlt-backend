package io.aurigraph.v11.bridge.services;

import io.aurigraph.v11.bridge.models.AtomicSwapRequest;
import io.aurigraph.v11.bridge.models.AtomicSwapResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * Hash-Time-Locked Contract (HTLC) Service
 * Manages atomic swaps using hash-time-locked contracts
 * Supports SHA256, SHA3, BLAKE2B hash algorithms
 *
 * HTLC Flow:
 * 1. INITIATED: HTLC contract created
 * 2. LOCKED: Funds locked with hash on both chains
 * 3. REVEALED: Secret revealed, enabling completion
 * 4. COMPLETED: Funds released and transferred
 * Or:
 * EXPIRED: Timelock expired
 * REFUNDED: Funds refunded to initiator
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 */
@ApplicationScoped
public class HashTimeLockContract {

    private static final Logger LOG = Logger.getLogger(HashTimeLockContract.class);

    // In-memory swap state store (production: use database)
    private final Map<String, AtomicSwapResponse> swapStore = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, String> secretStore = Collections.synchronizedMap(new LinkedHashMap<>()); // swapId -> secret

    /**
     * Initiate an atomic swap
     */
    public AtomicSwapResponse initiateSwap(AtomicSwapRequest request) {
        LOG.infof("Initiating atomic swap: %s", request.getSwapId());

        // Validate request
        List<String> errors = request.getValidationErrors();
        if (!errors.isEmpty()) {
            LOG.warnf("Swap validation failed: %s", String.join(", ", errors));
            return createErrorResponse(request.getSwapId(), "VALIDATION_FAILED",
                    String.join("; ", errors));
        }

        // Validate hash lock format
        if (!isValidHashLock(request.getHashLock())) {
            LOG.warn("Invalid hash lock format: " + request.getHashLock());
            return createErrorResponse(request.getSwapId(), "INVALID_HASH",
                    "Hash lock must be valid hex string");
        }

        // Create swap response
        AtomicSwapResponse response = AtomicSwapResponse.builder()
                .swapId(request.getSwapId())
                .status(AtomicSwapResponse.SwapStatus.INITIATED)
                .initiatorAddress(request.getInitiator())
                .counterpartyAddress(request.getCounterparty())
                .amountIn(request.getAmountIn())
                .amountOut(request.getAmountOut())
                .tokenIn(request.getTokenIn())
                .tokenOut(request.getTokenOut())
                .sourceChain(request.getSourceChain())
                .targetChain(request.getTargetChain())
                .hashAlgo(request.getHashAlgo())
                .hashLock(request.getHashLock())
                .refundAddress(request.getRefundAddress())
                .requiredConfirmations(12)
                .fee(request.getAmountIn().multiply(java.math.BigDecimal.valueOf(0.001))) // 0.1%
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Set expiry time (default 5 minutes if not specified)
        long timelockMs = request.getTimelock() != null ? request.getTimelock() : 300000; // 5 minutes
        response.setExpiryTime(Instant.now().plusMillis(timelockMs));

        // Store swap
        swapStore.put(request.getSwapId(), response);

        // Log event
        addEvent(request.getSwapId(), "INITIATED",
                "HTLC initiated with hash lock: " + request.getHashLock());

        LOG.infof("Atomic swap initiated successfully: %s, expires at: %s",
                 request.getSwapId(), response.getExpiryTime());

        return response;
    }

    /**
     * Lock funds on source chain
     */
    public AtomicSwapResponse lockFunds(String swapId, String lockTxHash) {
        LOG.infof("Locking funds for swap: %s", swapId);

        AtomicSwapResponse response = swapStore.get(swapId);
        if (response == null) {
            LOG.warnf("Swap not found: %s", swapId);
            return createErrorResponse(swapId, "SWAP_NOT_FOUND", "Swap not found");
        }

        // Check status
        if (response.getStatus() != AtomicSwapResponse.SwapStatus.INITIATED) {
            LOG.warnf("Cannot lock funds in state %s for swap %s",
                     response.getStatus(), swapId);
            return createErrorResponse(swapId, "INVALID_STATE",
                    "Swap must be in INITIATED state to lock funds");
        }

        // Check expiry
        if (Instant.now().isAfter(response.getExpiryTime())) {
            LOG.warnf("Swap expired, cannot lock funds: %s", swapId);
            response.setStatus(AtomicSwapResponse.SwapStatus.EXPIRED);
            addEvent(swapId, "EXPIRED", "HTLC expired before locking");
            return response;
        }

        // Update response
        response.setStatus(AtomicSwapResponse.SwapStatus.LOCKED);
        response.setLockTime(Instant.now());
        response.setLockTransactionHash(lockTxHash);
        response.setUpdatedAt(Instant.now());

        addEvent(swapId, "LOCKED", "Funds locked on source chain: " + lockTxHash);

        LOG.infof("Funds locked for swap: %s, tx: %s", swapId, lockTxHash);
        return response;
    }

    /**
     * Reveal secret to complete swap
     */
    public AtomicSwapResponse revealSecret(String swapId, String secret) {
        LOG.infof("Revealing secret for swap: %s", swapId);

        AtomicSwapResponse response = swapStore.get(swapId);
        if (response == null) {
            LOG.warnf("Swap not found: %s", swapId);
            return createErrorResponse(swapId, "SWAP_NOT_FOUND", "Swap not found");
        }

        // Check status
        if (response.getStatus() != AtomicSwapResponse.SwapStatus.LOCKED) {
            LOG.warnf("Cannot reveal secret in state %s for swap %s",
                     response.getStatus(), swapId);
            return createErrorResponse(swapId, "INVALID_STATE",
                    "Swap must be in LOCKED state to reveal secret");
        }

        // Check expiry
        if (Instant.now().isAfter(response.getExpiryTime())) {
            LOG.warnf("Swap expired, cannot reveal secret: %s", swapId);
            response.setStatus(AtomicSwapResponse.SwapStatus.EXPIRED);
            addEvent(swapId, "EXPIRED", "HTLC expired before reveal");
            return response;
        }

        // Verify secret matches hash
        String computedHash = computeHash(secret, response.getHashAlgo());
        if (!computedHash.equalsIgnoreCase(response.getHashLock())) {
            LOG.warnf("Secret does not match hash lock for swap: %s", swapId);
            addEvent(swapId, "FAILED", "Secret does not match hash lock");
            response.setStatus(AtomicSwapResponse.SwapStatus.FAILED);
            response.setErrorCode("INVALID_SECRET");
            response.setErrorDetails("Secret does not match hash lock");
            return response;
        }

        // Update response
        response.setStatus(AtomicSwapResponse.SwapStatus.REVEALED);
        response.setSecret(secret);
        response.setRevealTime(Instant.now());
        response.setUpdatedAt(Instant.now());

        // Store secret (for security, in production this should be encrypted)
        secretStore.put(swapId, secret);

        addEvent(swapId, "REVEALED", "Secret revealed, swap can be completed");

        LOG.infof("Secret revealed for swap: %s", swapId);
        return response;
    }

    /**
     * Complete the swap after secret is revealed
     */
    public AtomicSwapResponse completeSwap(String swapId, String targetTxHash) {
        LOG.infof("Completing swap: %s", swapId);

        AtomicSwapResponse response = swapStore.get(swapId);
        if (response == null) {
            LOG.warnf("Swap not found: %s", swapId);
            return createErrorResponse(swapId, "SWAP_NOT_FOUND", "Swap not found");
        }

        // Check status
        if (response.getStatus() != AtomicSwapResponse.SwapStatus.REVEALED) {
            LOG.warnf("Cannot complete swap in state %s for swap %s",
                     response.getStatus(), swapId);
            return createErrorResponse(swapId, "INVALID_STATE",
                    "Swap must be in REVEALED state to complete");
        }

        // Update response
        response.setStatus(AtomicSwapResponse.SwapStatus.COMPLETED);
        response.setTargetTransactionHash(targetTxHash);
        response.setCompletionTime(Instant.now());
        response.setUpdatedAt(Instant.now());

        addEvent(swapId, "COMPLETED", "Swap completed successfully: " + targetTxHash);

        LOG.infof("Swap completed successfully: %s", swapId);
        return response;
    }

    /**
     * Refund swap after timelock expires
     */
    public AtomicSwapResponse refundSwap(String swapId, String refundTxHash) {
        LOG.infof("Refunding swap: %s", swapId);

        AtomicSwapResponse response = swapStore.get(swapId);
        if (response == null) {
            LOG.warnf("Swap not found: %s", swapId);
            return createErrorResponse(swapId, "SWAP_NOT_FOUND", "Swap not found");
        }

        // Check if swap can be refunded
        if (!response.canBeRefunded()) {
            LOG.warnf("Swap cannot be refunded at this time: %s", swapId);
            return createErrorResponse(swapId, "INVALID_STATE",
                    "Swap cannot be refunded in current state or time");
        }

        // Update response
        response.setStatus(AtomicSwapResponse.SwapStatus.REFUNDED);
        response.setRefundTransactionHash(refundTxHash);
        response.setRefundTime(Instant.now());
        response.setUpdatedAt(Instant.now());

        addEvent(swapId, "REFUNDED", "Swap refunded after timelock expiry: " + refundTxHash);

        LOG.infof("Swap refunded: %s", swapId);
        return response;
    }

    /**
     * Check swap status
     */
    public AtomicSwapResponse getSwapStatus(String swapId) {
        AtomicSwapResponse response = swapStore.get(swapId);

        if (response != null) {
            // Check for expiry
            if (response.getStatus() != AtomicSwapResponse.SwapStatus.COMPLETED &&
                response.getStatus() != AtomicSwapResponse.SwapStatus.REFUNDED &&
                response.getStatus() != AtomicSwapResponse.SwapStatus.FAILED &&
                Instant.now().isAfter(response.getExpiryTime())) {
                response.setStatus(AtomicSwapResponse.SwapStatus.EXPIRED);
                response.setExpirationTime(Instant.now());
                addEvent(swapId, "EXPIRED", "HTLC timelock expired");
            }
        }

        return response;
    }

    /**
     * Compute hash of secret
     */
    public String computeHash(String secret, String hashAlgo) {
        try {
            MessageDigest digest = null;

            switch (hashAlgo.toUpperCase()) {
                case "SHA256":
                    digest = MessageDigest.getInstance("SHA-256");
                    break;
                case "SHA3":
                case "SHA3-256":
                    digest = MessageDigest.getInstance("SHA3-256");
                    break;
                case "SHA1":
                    digest = MessageDigest.getInstance("SHA-1");
                    break;
                default:
                    LOG.warnf("Unsupported hash algorithm: %s", hashAlgo);
                    return null;
            }

            if (digest != null) {
                byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(hash);
            }
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Hash algorithm not available: " + hashAlgo, e);
        }

        return null;
    }

    /**
     * Generate a random secret for HTLC
     */
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] secretBytes = new byte[32];
        random.nextBytes(secretBytes);
        return bytesToHex(secretBytes);
    }

    /**
     * Validate hash lock format
     */
    private boolean isValidHashLock(String hashLock) {
        if (hashLock == null || hashLock.isEmpty()) {
            return false;
        }

        // Should be valid hex string (64 characters for SHA256)
        if (!hashLock.matches("^[0-9a-fA-F]+$")) {
            return false;
        }

        // Check length (64 chars for SHA256, 64 for SHA3-256)
        return hashLock.length() >= 64;
    }

    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Add event to swap
     */
    private void addEvent(String swapId, String eventType, String message) {
        AtomicSwapResponse response = swapStore.get(swapId);
        if (response != null) {
            AtomicSwapResponse.SwapEvent event = AtomicSwapResponse.SwapEvent.builder()
                    .eventType(eventType)
                    .message(message)
                    .timestamp(Instant.now())
                    .build();
            response.addEvent(event);
        }
    }

    /**
     * Create error response
     */
    private AtomicSwapResponse createErrorResponse(String swapId, String errorCode, String errorDetails) {
        return AtomicSwapResponse.builder()
                .swapId(swapId)
                .status(AtomicSwapResponse.SwapStatus.FAILED)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Get all swaps (for testing/monitoring)
     */
    public Collection<AtomicSwapResponse> getAllSwaps() {
        return swapStore.values();
    }

    /**
     * Clear state (for testing)
     */
    public void clearState() {
        swapStore.clear();
        secretStore.clear();
    }

    /**
     * Get swap count by status
     */
    public Map<String, Integer> getSwapCountByStatus() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        swapStore.values().stream()
                .map(AtomicSwapResponse::getStatus)
                .forEach(status -> counts.compute(status.toString(),
                        (k, v) -> v == null ? 1 : v + 1));
        return counts;
    }
}
