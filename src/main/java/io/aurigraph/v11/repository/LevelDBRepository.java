package io.aurigraph.v11.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generic LevelDB Repository
 * Provides CRUD operations for LevelDB-stored entities with JSON serialization
 *
 * @param <T> Entity type
 * @version 1.0.0
 * @since Phase 4 - LevelDB Migration (Oct 8, 2025)
 */
public abstract class LevelDBRepository<T> {

    protected DB db;
    protected final ObjectMapper objectMapper;
    protected final Class<T> entityClass;
    protected final String dbPath;

    /**
     * Constructor
     *
     * @param objectMapper Jackson ObjectMapper for JSON serialization
     * @param entityClass Entity class for deserialization
     * @param dbName Database name (subdirectory)
     */
    public LevelDBRepository(ObjectMapper objectMapper, Class<T> entityClass, String dbName) {
        this.objectMapper = objectMapper;
        this.entityClass = entityClass;
        this.dbPath = System.getProperty("leveldb.path", "./data/leveldb/") + dbName;
    }

    /**
     * Initialize LevelDB connection
     */
    @PostConstruct
    public void init() {
        try {
            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                dbFile.mkdirs();
                Log.infof("Created LevelDB directory: %s", dbPath);
            }

            Options options = new Options();
            options.createIfMissing(true);
            options.cacheSize(100 * 1048576); // 100MB cache

            db = Iq80DBFactory.factory.open(dbFile, options);
            Log.infof("Initialized LevelDB repository: %s", dbPath);

        } catch (IOException e) {
            Log.errorf(e, "Failed to initialize LevelDB at: %s", dbPath);
            throw new RuntimeException("LevelDB initialization failed", e);
        }
    }

    /**
     * Close LevelDB connection
     */
    @PreDestroy
    public void close() {
        if (db != null) {
            try {
                db.close();
                Log.infof("Closed LevelDB repository: %s", dbPath);
            } catch (IOException e) {
                Log.errorf(e, "Failed to close LevelDB: %s", dbPath);
            }
        }
    }

    /**
     * Save entity to LevelDB
     *
     * @param key Primary key
     * @param entity Entity to save
     */
    public void save(String key, T entity) {
        try {
            String json = objectMapper.writeValueAsString(entity);
            db.put(key.getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));
            Log.debugf("Saved entity with key: %s", key);
        } catch (Exception e) {
            Log.errorf(e, "Failed to save entity with key: %s", key);
            throw new RuntimeException("Save operation failed", e);
        }
    }

    /**
     * Find entity by key
     *
     * @param key Primary key
     * @return Optional containing entity if found
     */
    public Optional<T> findByKey(String key) {
        try {
            byte[] value = db.get(key.getBytes(StandardCharsets.UTF_8));
            if (value == null) {
                return Optional.empty();
            }

            String json = new String(value, StandardCharsets.UTF_8);
            T entity = objectMapper.readValue(json, entityClass);
            return Optional.of(entity);

        } catch (Exception e) {
            Log.errorf(e, "Failed to find entity with key: %s", key);
            return Optional.empty();
        }
    }

    /**
     * Delete entity by key
     *
     * @param key Primary key
     */
    public void delete(String key) {
        try {
            db.delete(key.getBytes(StandardCharsets.UTF_8));
            Log.debugf("Deleted entity with key: %s", key);
        } catch (Exception e) {
            Log.errorf(e, "Failed to delete entity with key: %s", key);
            throw new RuntimeException("Delete operation failed", e);
        }
    }

    /**
     * Find all entities with key prefix
     * Useful for composite keys like "kyc:userId"
     *
     * @param keyPrefix Key prefix to search
     * @return List of matching entities
     */
    public List<T> findByKeyPrefix(String keyPrefix) {
        List<T> results = new ArrayList<>();

        try (DBIterator iterator = db.iterator()) {
            byte[] prefixBytes = keyPrefix.getBytes(StandardCharsets.UTF_8);
            iterator.seek(prefixBytes);

            while (iterator.hasNext()) {
                var entry = iterator.next();
                String key = new String(entry.getKey(), StandardCharsets.UTF_8);

                // Check if key starts with prefix
                if (!key.startsWith(keyPrefix)) {
                    break; // LevelDB keys are sorted, so we can stop
                }

                String json = new String(entry.getValue(), StandardCharsets.UTF_8);
                T entity = objectMapper.readValue(json, entityClass);
                results.add(entity);
            }

        } catch (Exception e) {
            Log.errorf(e, "Failed to find entities with prefix: %s", keyPrefix);
        }

        return results;
    }

    /**
     * Count all entities
     *
     * @return Total entity count
     */
    public long count() {
        long count = 0;
        try (DBIterator iterator = db.iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
        } catch (Exception e) {
            Log.errorf(e, "Failed to count entities");
        }
        return count;
    }

    /**
     * Check if key exists
     *
     * @param key Primary key
     * @return true if exists
     */
    public boolean exists(String key) {
        byte[] value = db.get(key.getBytes(StandardCharsets.UTF_8));
        return value != null;
    }

    /**
     * Get all entities (use with caution on large datasets)
     *
     * @return List of all entities
     */
    public List<T> findAll() {
        List<T> results = new ArrayList<>();

        try (DBIterator iterator = db.iterator()) {
            iterator.seekToFirst();

            while (iterator.hasNext()) {
                var entry = iterator.next();
                String json = new String(entry.getValue(), StandardCharsets.UTF_8);
                T entity = objectMapper.readValue(json, entityClass);
                results.add(entity);
            }

        } catch (Exception e) {
            Log.errorf(e, "Failed to find all entities");
        }

        return results;
    }

    /**
     * Batch save operation
     *
     * @param entities Map of key-entity pairs
     */
    public void saveAll(java.util.Map<String, T> entities) {
        try {
            for (var entry : entities.entrySet()) {
                save(entry.getKey(), entry.getValue());
            }
            Log.infof("Batch saved %d entities", entities.size());
        } catch (Exception e) {
            Log.errorf(e, "Failed to batch save entities");
            throw new RuntimeException("Batch save operation failed", e);
        }
    }
}
