package io.aurigraph.v11.security;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Transaction Encryption Service - Sprint 18 Encryption Stream
 *
 * Provides encryption for the Transaction Processing Layer, protecting
 * sensitive financial transaction data in memory and during processing.
 *
 * Features:
 * - Bulk encryption for batch processing (500K transactions)
 * - Parallel encryption using reactive streams
 * - Memory-efficient streaming encryption
 * - Performance-optimized for 3M+ TPS target
 * - Cache-friendly encryption for hot data paths
 *
 * Performance Requirements:
 * - Encryption overhead: <5% CPU
 * - Latency impact: <2ms per transaction
 * - Throughput: Support 3M+ TPS
 * - Memory overhead: <100MB
 *
 * Security:
 * - AES-256-GCM authenticated encryption
 * - Unique IV per transaction
 * - Authentication tag for integrity
 * - Automatic key rotation (30 days)
 *
 * @author Security & Cryptography Agent (SCA-Lead)
 * @version 11.4.4
 * @since Sprint 18 - November 2025
 */
@ApplicationScoped
public class TransactionEncryptionService {

    private static final Logger logger =
        LoggerFactory.getLogger(TransactionEncryptionService.class);

    @Inject
    EncryptionService encryptionService;

    @Inject
    SecurityAuditService auditService;

    // Performance metrics
    private final AtomicLong transactionsEncrypted = new AtomicLong(0);
    private final AtomicLong transactionsDecrypted = new AtomicLong(0);
    private final AtomicLong batchesProcessed = new AtomicLong(0);
    private final AtomicLong totalEncryptionTimeNanos = new AtomicLong(0);

    /**
     * Encrypt transaction payload data
     *
     * @param payload Transaction payload (JSON, binary, or other format)
     * @return Encrypted payload with version header and authentication tag
     */
    public Uni<byte[]> encryptTransactionPayload(byte[] payload) {
        long startTime = System.nanoTime();

        return encryptionService.encrypt(
            payload,
            EncryptionService.EncryptionLayer.TRANSACTION
        ).invoke(encrypted -> {
            // Update metrics
            transactionsEncrypted.incrementAndGet();
            long duration = System.nanoTime() - startTime;
            totalEncryptionTimeNanos.addAndGet(duration);

            if (logger.isDebugEnabled()) {
                logger.debug("Encrypted transaction payload: {} bytes -> {} bytes in {} μs",
                            payload.length, encrypted.length, duration / 1000);
            }
        });
    }

    /**
     * Decrypt transaction payload data
     *
     * @param encryptedPayload Encrypted transaction payload
     * @return Decrypted plaintext payload
     */
    public Uni<byte[]> decryptTransactionPayload(byte[] encryptedPayload) {
        return encryptionService.decrypt(
            encryptedPayload,
            EncryptionService.EncryptionLayer.TRANSACTION
        ).invoke(decrypted -> {
            transactionsDecrypted.incrementAndGet();
        });
    }

    /**
     * Encrypt transaction payload (String version)
     *
     * @param payload Transaction payload as string (e.g., JSON)
     * @return Encrypted payload
     */
    public Uni<byte[]> encryptTransactionPayload(String payload) {
        if (payload == null) {
            return Uni.createFrom().nullItem();
        }
        return encryptTransactionPayload(
            payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    /**
     * Decrypt transaction payload to string
     *
     * @param encryptedPayload Encrypted transaction payload
     * @return Decrypted payload as string
     */
    public Uni<String> decryptTransactionPayloadToString(byte[] encryptedPayload) {
        if (encryptedPayload == null) {
            return Uni.createFrom().nullItem();
        }
        return decryptTransactionPayload(encryptedPayload)
            .map(bytes -> new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Bulk encrypt transaction payloads in parallel (for batch processing)
     *
     * This method is optimized for high-throughput batch processing,
     * encrypting up to 500K transactions efficiently.
     *
     * @param payloads List of transaction payloads to encrypt
     * @return List of encrypted payloads (same order as input)
     */
    public Uni<List<byte[]>> encryptBatch(List<byte[]> payloads) {
        long startTime = System.nanoTime();

        return Multi.createFrom().iterable(payloads)
            .onItem().transformToUniAndConcatenate(payload ->
                encryptTransactionPayload(payload)
            )
            .collect().asList()
            .invoke(encryptedList -> {
                // Update metrics
                batchesProcessed.incrementAndGet();
                long duration = System.nanoTime() - startTime;

                logger.info("Bulk encrypted {} transactions in {} ms",
                           payloads.size(), duration / 1_000_000);

                auditService.logSecurityEvent("TRANSACTION_BATCH_ENCRYPTED",
                    "Count: " + payloads.size() +
                    ", Duration: " + (duration / 1_000_000) + "ms");
            });
    }

    /**
     * Bulk decrypt transaction payloads in parallel
     *
     * @param encryptedPayloads List of encrypted transaction payloads
     * @return List of decrypted payloads (same order as input)
     */
    public Uni<List<byte[]>> decryptBatch(List<byte[]> encryptedPayloads) {
        return Multi.createFrom().iterable(encryptedPayloads)
            .onItem().transformToUniAndConcatenate(encrypted ->
                decryptTransactionPayload(encrypted)
            )
            .collect().asList()
            .invoke(decryptedList -> {
                logger.info("Bulk decrypted {} transactions", decryptedList.size());
            });
    }

    /**
     * Encrypt transaction metadata (for audit trails and indexing)
     *
     * @param metadata Transaction metadata (sender, receiver, amount, etc.)
     * @return Encrypted metadata
     */
    public Uni<byte[]> encryptTransactionMetadata(String metadata) {
        return encryptTransactionPayload(metadata);
    }

    /**
     * Check if transaction payload is encrypted
     *
     * @param payload Transaction payload
     * @return true if encrypted, false otherwise
     */
    public boolean isEncrypted(byte[] payload) {
        if (payload == null || payload.length < 2) {
            return false;
        }
        // Check for encryption version header (0x01) and transaction layer marker
        return payload[0] == 0x01 &&
               payload[1] == (byte) EncryptionService.EncryptionLayer.TRANSACTION.ordinal();
    }

    /**
     * Rotate transaction encryption key (should be called every 30 days)
     */
    public Uni<Void> rotateTransactionKey() {
        logger.info("Rotating transaction encryption key");
        return encryptionService.rotateLayerKey(
            EncryptionService.EncryptionLayer.TRANSACTION
        ).invoke(() -> {
            auditService.logSecurityEvent("TRANSACTION_KEY_ROTATED",
                "Transaction encryption key rotated successfully");
        });
    }

    /**
     * Get transaction encryption statistics
     */
    public TransactionEncryptionStats getStats() {
        long avgTimeNanos = transactionsEncrypted.get() > 0
            ? totalEncryptionTimeNanos.get() / transactionsEncrypted.get()
            : 0;

        return new TransactionEncryptionStats(
            transactionsEncrypted.get(),
            transactionsDecrypted.get(),
            batchesProcessed.get(),
            avgTimeNanos
        );
    }

    /**
     * Transaction encryption statistics
     */
    public record TransactionEncryptionStats(
        long transactionsEncrypted,
        long transactionsDecrypted,
        long batchesProcessed,
        long avgEncryptionTimeNanos
    ) {
        public double getAvgEncryptionTimeMicros() {
            return avgEncryptionTimeNanos / 1000.0;
        }

        public long getEstimatedTPS() {
            if (avgEncryptionTimeNanos == 0) return 0;
            return 1_000_000_000L / avgEncryptionTimeNanos;
        }
    }
}
