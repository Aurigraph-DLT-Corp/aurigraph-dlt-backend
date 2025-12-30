package io.aurigraph.v11.storage;

import io.aurigraph.v11.security.LevelDBEncryptionService;
import io.aurigraph.v11.security.LevelDBValidator;
import io.aurigraph.v11.security.LevelDBAccessControl;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.iq80.leveldb.*;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LevelDB Storage Service for Aurigraph V11
 *
 * Each node has its own embedded LevelDB instance for local persistence.
 * Provides key-value storage for blockchain data, state, and transactions.
 *
 * Features:
 * - Fast key-value storage
 * - Atomic batch writes
 * - Snapshot isolation
 * - Compression support
 * - Per-node data isolation
 *
 * @version 1.0.0 (Oct 7, 2025)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class LevelDBService {

    private static final Logger LOG = Logger.getLogger(LevelDBService.class);

    @ConfigProperty(name = "leveldb.data.path", defaultValue = "/opt/aurigraph-v11/data/leveldb")
    String dataPath;

    @ConfigProperty(name = "leveldb.cache.size.mb", defaultValue = "256")
    int cacheSizeMB;

    @ConfigProperty(name = "leveldb.write.buffer.mb", defaultValue = "64")
    int writeBufferMB;

    @ConfigProperty(name = "leveldb.compression.enabled", defaultValue = "true")
    boolean compressionEnabled;

    @Inject
    LevelDBEncryptionService encryptionService;

    @Inject
    LevelDBValidator validator;

    @Inject
    LevelDBAccessControl accessControl;

    private DB db;
    private Options options;

    // Performance metrics
    private final AtomicLong readCount = new AtomicLong(0);
    private final AtomicLong writeCount = new AtomicLong(0);
    private final AtomicLong deleteCount = new AtomicLong(0);
    private final AtomicLong batchCount = new AtomicLong(0);

    @PostConstruct
    void init() {
        try {
            LOG.infof("Initializing LevelDB at: %s", dataPath);

            // Create data directory if it doesn't exist
            File dbDir = new File(dataPath);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
                LOG.infof("Created LevelDB directory: %s", dataPath);
            }

            // Configure LevelDB options
            options = new Options();
            options.createIfMissing(true);
            options.cacheSize(cacheSizeMB * 1024 * 1024L);
            options.writeBufferSize(writeBufferMB * 1024 * 1024);
            options.compressionType(compressionEnabled ? CompressionType.SNAPPY : CompressionType.NONE);
            options.maxOpenFiles(1000);
            options.blockSize(4 * 1024);

            // Open database
            db = Iq80DBFactory.factory.open(dbDir, options);

            LOG.infof("✅ LevelDB initialized successfully");
            LOG.infof("   - Path: %s", dataPath);
            LOG.infof("   - Cache: %d MB", cacheSizeMB);
            LOG.infof("   - Write Buffer: %d MB", writeBufferMB);
            LOG.infof("   - Compression: %s", compressionEnabled ? "SNAPPY" : "NONE");

        } catch (IOException e) {
            LOG.errorf(e, "Failed to initialize LevelDB");
            throw new RuntimeException("LevelDB initialization failed", e);
        }
    }

    @PreDestroy
    void cleanup() {
        if (db != null) {
            try {
                LOG.info("Closing LevelDB...");
                db.close();
                LOG.info("✅ LevelDB closed successfully");
            } catch (IOException e) {
                LOG.errorf(e, "Error closing LevelDB");
            }
        }
    }

    // ==================== BASIC OPERATIONS ====================

    /**
     * Put a key-value pair (with encryption and validation)
     */
    public Uni<Void> put(String key, String value) {
        return Uni.createFrom().item(() -> {
            // Validate input
            validator.validateKey(key);
            validator.validateValue(value);

            // Check write permission
            accessControl.checkWritePermission(key);

            return null;
        }).flatMap(v ->
            // Encrypt value
            encryptionService.encryptString(value)
        ).flatMap(encryptedValue ->
            // Store encrypted value
            Uni.createFrom().item(() -> {
                db.put(bytes(key), encryptedValue);
                writeCount.incrementAndGet();
                return null;
            })
        );
    }

    /**
     * Put a key-value pair (bytes with encryption)
     */
    public Uni<Void> put(byte[] key, byte[] value) {
        return Uni.createFrom().item(() -> {
            // Validate input
            String keyStr = asString(key);
            validator.validateKey(keyStr);
            validator.validateValueBytes(value);

            // Check write permission
            accessControl.checkWritePermission(keyStr);

            return null;
        }).flatMap(v ->
            // Encrypt value
            encryptionService.encrypt(value)
        ).flatMap(encryptedValue ->
            // Store encrypted value
            Uni.createFrom().item(() -> {
                db.put(key, encryptedValue);
                writeCount.incrementAndGet();
                return null;
            })
        );
    }

    /**
     * Get value by key (with decryption and access control)
     */
    public Uni<String> get(String key) {
        return Uni.createFrom().item(() -> {
            // Check read permission
            accessControl.checkReadPermission(key);

            // Retrieve encrypted value
            byte[] encryptedValue = db.get(bytes(key));
            readCount.incrementAndGet();
            return encryptedValue;
        }).flatMap(encryptedValue ->
            // Decrypt value if present
            encryptedValue != null
                ? encryptionService.decryptString(encryptedValue)
                : Uni.createFrom().nullItem()
        );
    }

    /**
     * Get value by key (bytes with decryption)
     */
    public Uni<byte[]> getBytes(byte[] key) {
        return Uni.createFrom().item(() -> {
            // Check read permission
            String keyStr = asString(key);
            accessControl.checkReadPermission(keyStr);

            // Retrieve encrypted value
            byte[] encryptedValue = db.get(key);
            readCount.incrementAndGet();
            return encryptedValue;
        }).flatMap(encryptedValue ->
            // Decrypt value if present
            encryptedValue != null
                ? encryptionService.decrypt(encryptedValue)
                : Uni.createFrom().nullItem()
        );
    }

    /**
     * Delete a key
     */
    public Uni<Void> delete(String key) {
        return Uni.createFrom().item(() -> {
            db.delete(bytes(key));
            deleteCount.incrementAndGet();
            return null;
        });
    }

    /**
     * Delete a key (bytes)
     */
    public Uni<Void> delete(byte[] key) {
        return Uni.createFrom().item(() -> {
            db.delete(key);
            deleteCount.incrementAndGet();
            return null;
        });
    }

    /**
     * Check if key exists
     */
    public Uni<Boolean> exists(String key) {
        return Uni.createFrom().item(() -> {
            byte[] value = db.get(bytes(key));
            readCount.incrementAndGet();
            return value != null;
        });
    }

    // ==================== BATCH OPERATIONS ====================

    /**
     * Execute batch write operations
     */
    public Uni<Void> batchWrite(Map<String, String> puts, List<String> deletes) {
        return Uni.createFrom().item(() -> {
            WriteBatch batch = db.createWriteBatch();
            try {
                // Add puts
                if (puts != null) {
                    puts.forEach((k, v) -> batch.put(bytes(k), bytes(v)));
                }

                // Add deletes
                if (deletes != null) {
                    deletes.forEach(k -> batch.delete(bytes(k)));
                }

                // Write batch
                db.write(batch);
                batchCount.incrementAndGet();

                return null;
            } finally {
                try {
                    batch.close();
                } catch (IOException e) {
                    LOG.warnf(e, "Error closing batch");
                }
            }
        });
    }

    // ==================== RANGE QUERIES ====================

    /**
     * Get all keys with a prefix
     */
    public Uni<List<String>> getKeysByPrefix(String prefix) {
        return Uni.createFrom().item(() -> {
            List<String> keys = new ArrayList<>();
            DBIterator iterator = db.iterator();
            try {
                byte[] prefixBytes = bytes(prefix);
                iterator.seek(prefixBytes);

                while (iterator.hasNext()) {
                    Map.Entry<byte[], byte[]> entry = iterator.next();
                    String key = asString(entry.getKey());
                    if (key.startsWith(prefix)) {
                        keys.add(key);
                    } else {
                        break;
                    }
                }
            } finally {
                try {
                    iterator.close();
                } catch (IOException e) {
                    LOG.warnf(e, "Error closing iterator");
                }
            }
            return keys;
        });
    }

    /**
     * Scan all keys and values with prefix
     */
    public Uni<Map<String, String>> scanByPrefix(String prefix) {
        return Uni.createFrom().item(() -> {
            Map<String, String> result = new HashMap<>();
            DBIterator iterator = db.iterator();
            try {
                byte[] prefixBytes = bytes(prefix);
                iterator.seek(prefixBytes);

                while (iterator.hasNext()) {
                    Map.Entry<byte[], byte[]> entry = iterator.next();
                    String key = asString(entry.getKey());
                    if (key.startsWith(prefix)) {
                        result.put(key, asString(entry.getValue()));
                    } else {
                        break;
                    }
                }
            } finally {
                try {
                    iterator.close();
                } catch (IOException e) {
                    LOG.warnf(e, "Error closing iterator");
                }
            }
            return result;
        });
    }

    // ==================== SNAPSHOTS ====================

    /**
     * Create a snapshot for consistent reads
     */
    public Snapshot createSnapshot() {
        return db.getSnapshot();
    }

    /**
     * Get value using snapshot
     */
    public Uni<String> getWithSnapshot(Snapshot snapshot, String key) {
        return Uni.createFrom().item(() -> {
            ReadOptions options = new ReadOptions();
            options.snapshot(snapshot);
            byte[] value = db.get(bytes(key), options);
            readCount.incrementAndGet();
            return value != null ? asString(value) : null;
        });
    }

    // ==================== STATISTICS ====================

    /**
     * Get storage statistics
     */
    public Uni<StorageStats> getStats() {
        return Uni.createFrom().item(() -> {
            String stats = db.getProperty("leveldb.stats");
            String sstables = db.getProperty("leveldb.sstables");

            return new StorageStats(
                    readCount.get(),
                    writeCount.get(),
                    deleteCount.get(),
                    batchCount.get(),
                    dataPath,
                    cacheSizeMB,
                    writeBufferMB,
                    compressionEnabled,
                    stats,
                    sstables
            );
        });
    }

    // ==================== HELPER METHODS ====================

    private byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private String asString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // ==================== DATA MODELS ====================

    public record StorageStats(
            long readCount,
            long writeCount,
            long deleteCount,
            long batchCount,
            String dataPath,
            int cacheSizeMB,
            int writeBufferMB,
            boolean compressionEnabled,
            String internalStats,
            String sstables
    ) {}
}
