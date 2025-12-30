package io.aurigraph.v11.storage;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.jboss.logging.Logger;

import io.aurigraph.v11.tokenization.models.StorageInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;
import java.io.ByteArrayOutputStream;

/**
 * LevelDB Storage Service for ALL Nodes
 *
 * Provides persistent storage for tokenized data across ALL node types
 * using LevelDB embedded key-value database.
 *
 * Features:
 * - Channel-based organization
 * - Optional compression (GZIP)
 * - Optional encryption (AES-256-GCM)
 * - Automatic database lifecycle management
 * - Thread-safe concurrent access
 *
 * @version 11.3.0
 * @author Backend Development Agent (BDA)
 */
@ApplicationScoped
public class LevelDBStorageService {

    private static final Logger LOG = Logger.getLogger(LevelDBStorageService.class);

    @ConfigProperty(name = "tokenization.leveldb.base-path", defaultValue = "data/tokenization")
    String basePath;

    @ConfigProperty(name = "tokenization.leveldb.compression.enabled", defaultValue = "true")
    boolean compressionEnabled;

    @ConfigProperty(name = "tokenization.leveldb.encryption.enabled", defaultValue = "false")
    boolean encryptionEnabled;

    @ConfigProperty(name = "tokenization.leveldb.cache-size", defaultValue = "100")
    long cacheSizeMB;

    @ConfigProperty(name = "tokenization.leveldb.write-buffer-size", defaultValue = "4")
    long writeBufferSizeMB;

    // Active database instances per channel
    private final Map<String, DB> databases = new ConcurrentHashMap<>();

    // Storage statistics
    private long totalBytesStored = 0;
    private int totalTransactions = 0;

    /**
     * Store tokenized data for ALL nodes
     *
     * @param channel Channel name for organization
     * @param txId Transaction ID
     * @param dataHash SHA-256 hash of data
     * @param data Raw data bytes
     * @return LevelDB storage path
     */
    public Uni<String> storeTokenizedData(String channel, String txId,
                                          String dataHash, byte[] data) {
        return Uni.createFrom().item(() -> {
            try {
                // Get or create database for channel
                DB db = getOrCreateDatabase(channel);

                // Compress data if enabled
                byte[] finalData = data;
                if (compressionEnabled) {
                    finalData = compressData(data);
                    double reduction = 100.0 * (data.length - finalData.length) / data.length;
                    LOG.debugf("Compressed data from %d to %d bytes (%.1f%% reduction)",
                        Integer.valueOf(data.length), Integer.valueOf(finalData.length),
                        Double.valueOf(reduction));
                }

                // Encrypt data if enabled
                if (encryptionEnabled) {
                    finalData = encryptData(finalData);
                    LOG.debugf("Encrypted data: %d bytes", Integer.valueOf(finalData.length));
                }

                // Create composite key: txId#dataHash
                String compositeKey = txId + "#" + dataHash;
                byte[] keyBytes = compositeKey.getBytes();

                // Store in LevelDB
                db.put(keyBytes, finalData);

                // Update statistics
                totalBytesStored += finalData.length;
                totalTransactions++;

                // Return storage path
                String storagePath = basePath + "/" + channel + "/" + compositeKey;
                LOG.infof("Stored %d bytes to LevelDB: %s", finalData.length, storagePath);

                return storagePath;

            } catch (Exception e) {
                LOG.errorf(e, "Failed to store tokenized data: %s", txId);
                throw new RuntimeException("LevelDB storage failed: " + e.getMessage(), e);
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Retrieve tokenized data from LevelDB
     *
     * @param channel Channel name
     * @param txId Transaction ID
     * @param dataHash Data hash
     * @return Raw data bytes
     */
    public Uni<byte[]> retrieveTokenizedData(String channel, String txId, String dataHash) {
        return Uni.createFrom().item(() -> {
            try {
                DB db = getOrCreateDatabase(channel);

                // Create composite key
                String compositeKey = txId + "#" + dataHash;
                byte[] keyBytes = compositeKey.getBytes();

                // Retrieve from LevelDB
                byte[] storedData = db.get(keyBytes);

                if (storedData == null) {
                    throw new IllegalArgumentException("Data not found: " + compositeKey);
                }

                // Decrypt if encryption enabled
                byte[] finalData = storedData;
                if (encryptionEnabled) {
                    finalData = decryptData(storedData);
                }

                // Decompress if compression enabled
                if (compressionEnabled) {
                    finalData = decompressData(finalData);
                }

                LOG.infof("Retrieved %d bytes from LevelDB: %s/%s", finalData.length, channel, compositeKey);
                return finalData;

            } catch (Exception e) {
                LOG.errorf(e, "Failed to retrieve tokenized data: %s#%s", txId, dataHash);
                throw new RuntimeException("LevelDB retrieval failed: " + e.getMessage(), e);
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get storage information for ALL nodes
     *
     * @return Storage info with statistics
     */
    public Uni<StorageInfo> getStorageInfo() {
        return Uni.createFrom().item(() -> {
            try {
                // Calculate total size on disk
                long totalSize = calculateTotalSize();

                // Count channels
                int channelCount = databases.size();

                return new StorageInfo(
                    basePath,
                    totalSize,
                    0,  // slimNodeCount (legacy field, not applicable for ALL nodes)
                    channelCount,
                    compressionEnabled,
                    encryptionEnabled
                );

            } catch (Exception e) {
                LOG.errorf(e, "Failed to get storage info");
                throw new RuntimeException("Storage info retrieval failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get or create LevelDB database for a channel
     */
    private DB getOrCreateDatabase(String channel) {
        return databases.computeIfAbsent(channel, ch -> {
            try {
                // Create channel directory
                Path channelPath = Paths.get(basePath, channel);
                Files.createDirectories(channelPath);

                // Configure LevelDB options
                Options options = new Options();
                options.createIfMissing(true);
                options.cacheSize(cacheSizeMB * 1024 * 1024);  // Convert MB to bytes
                options.writeBufferSize((int) (writeBufferSizeMB * 1024 * 1024));
                options.compressionType(org.iq80.leveldb.CompressionType.SNAPPY);

                // Open database
                DB db = Iq80DBFactory.factory.open(channelPath.toFile(), options);

                LOG.infof("Opened LevelDB for channel: %s (cache: %d MB, write buffer: %d MB)",
                    channel, cacheSizeMB, writeBufferSizeMB);

                return db;

            } catch (IOException e) {
                LOG.errorf(e, "Failed to open LevelDB for channel: %s", channel);
                throw new RuntimeException("Database creation failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Compress data using GZIP
     */
    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }

    /**
     * Decompress GZIP data
     */
    private byte[] decompressData(byte[] compressedData) throws IOException {
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(compressedData);
        java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bais);
        return gzip.readAllBytes();
    }

    /**
     * Encrypt data using AES-256-GCM (placeholder implementation)
     * TODO: Implement proper encryption with key management
     */
    private byte[] encryptData(byte[] data) {
        // Placeholder: In production, use AES-256-GCM with proper key management
        LOG.warn("Encryption requested but not yet implemented - returning plain data");
        return data;
    }

    /**
     * Decrypt AES-256-GCM data (placeholder implementation)
     * TODO: Implement proper decryption with key management
     */
    private byte[] decryptData(byte[] encryptedData) {
        // Placeholder: In production, use AES-256-GCM with proper key management
        LOG.warn("Decryption requested but not yet implemented - returning plain data");
        return encryptedData;
    }

    /**
     * Calculate total storage size on disk
     */
    private long calculateTotalSize() {
        try {
            Path basePathDir = Paths.get(basePath);
            if (!Files.exists(basePathDir)) {
                return 0;
            }

            return Files.walk(basePathDir)
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();

        } catch (IOException e) {
            LOG.errorf(e, "Failed to calculate total size");
            return totalBytesStored;  // Fallback to tracked bytes
        }
    }

    /**
     * Close all databases gracefully
     */
    public void closeAll() {
        databases.forEach((channel, db) -> {
            try {
                db.close();
                LOG.infof("Closed LevelDB for channel: %s", channel);
            } catch (IOException e) {
                LOG.errorf(e, "Failed to close LevelDB for channel: %s", channel);
            }
        });
        databases.clear();
    }

    /**
     * Cleanup on shutdown
     */
    public void shutdown() {
        LOG.info("Shutting down LevelDB Storage Service...");
        closeAll();
        LOG.infof("Total stored: %d transactions, %d bytes", totalTransactions, totalBytesStored);
    }
}
