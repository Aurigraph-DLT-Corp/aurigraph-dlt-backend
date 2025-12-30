package io.aurigraph.v11.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Base LevelDB Repository
 *
 * Provides generic CRUD operations for LevelDB storage.
 * Replaces Panache repository pattern with key-value storage.
 *
 * @param <T> Entity type
 * @param <ID> ID type (typically String)
 *
 * @version 1.0.0 (Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 */
public abstract class LevelDBRepository<T, ID> {

    @Inject
    protected LevelDBService levelDB;

    @Inject
    protected ObjectMapper objectMapper;

    /**
     * Get the entity class for JSON serialization
     */
    protected abstract Class<T> getEntityClass();

    /**
     * Get the key prefix for this repository (e.g., "token:", "channel:")
     */
    protected abstract String getKeyPrefix();

    /**
     * Extract ID from entity
     */
    protected abstract ID getId(T entity);

    // ==================== BASIC CRUD OPERATIONS ====================

    /**
     * Save an entity to LevelDB
     */
    public Uni<T> persist(T entity) {
        return Uni.createFrom().item(() -> {
            try {
                String key = buildKey(getId(entity));
                String value = objectMapper.writeValueAsString(entity);
                levelDB.put(key, value).await().indefinitely();
                return entity;
            } catch (Exception e) {
                throw new RuntimeException("Failed to persist entity", e);
            }
        });
    }

    /**
     * Find entity by ID
     */
    public Uni<Optional<T>> findById(ID id) {
        return Uni.createFrom().item(() -> {
            try {
                String key = buildKey(id);
                String value = levelDB.get(key).await().indefinitely();
                if (value == null) {
                    return Optional.empty();
                }
                T entity = objectMapper.readValue(value, getEntityClass());
                return Optional.of(entity);
            } catch (Exception e) {
                throw new RuntimeException("Failed to find entity by ID", e);
            }
        });
    }

    /**
     * Delete entity by ID
     */
    public Uni<Void> deleteById(ID id) {
        return Uni.createFrom().item(() -> {
            try {
                String key = buildKey(id);
                levelDB.delete(key).await().indefinitely();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete entity", e);
            }
        });
    }

    /**
     * Delete an entity
     */
    public Uni<Void> delete(T entity) {
        return deleteById(getId(entity));
    }

    /**
     * Check if entity exists
     */
    public Uni<Boolean> existsById(ID id) {
        return Uni.createFrom().item(() -> {
            try {
                String key = buildKey(id);
                return levelDB.exists(key).await().indefinitely();
            } catch (Exception e) {
                throw new RuntimeException("Failed to check existence", e);
            }
        });
    }

    /**
     * Count all entities
     */
    public Uni<Long> count() {
        return Uni.createFrom().item(() -> {
            try {
                List<String> keys = levelDB.getKeysByPrefix(getKeyPrefix()).await().indefinitely();
                return (long) keys.size();
            } catch (Exception e) {
                throw new RuntimeException("Failed to count entities", e);
            }
        });
    }

    /**
     * List all entities
     */
    public Uni<List<T>> listAll() {
        return Uni.createFrom().item(() -> {
            try {
                java.util.Map<String, String> entries = levelDB.scanByPrefix(getKeyPrefix()).await().indefinitely();
                List<T> entities = new ArrayList<>();
                for (String value : entries.values()) {
                    entities.add(objectMapper.readValue(value, getEntityClass()));
                }
                return entities;
            } catch (Exception e) {
                throw new RuntimeException("Failed to list all entities", e);
            }
        });
    }

    // ==================== QUERY OPERATIONS ====================

    /**
     * Find entities matching a predicate
     */
    public Uni<List<T>> findBy(Predicate<T> predicate) {
        return listAll().map(entities ->
            entities.stream()
                .filter(predicate)
                .collect(Collectors.toList())
        );
    }

    /**
     * Find first entity matching a predicate
     */
    public Uni<Optional<T>> findFirstBy(Predicate<T> predicate) {
        return listAll().map(entities ->
            entities.stream()
                .filter(predicate)
                .findFirst()
        );
    }

    /**
     * Count entities matching a predicate
     */
    public Uni<Long> countBy(Predicate<T> predicate) {
        return listAll().map(entities ->
            entities.stream()
                .filter(predicate)
                .count()
        );
    }

    // ==================== BATCH OPERATIONS ====================

    /**
     * Save multiple entities
     */
    public Uni<List<T>> persistAll(List<T> entities) {
        return Uni.createFrom().item(() -> {
            try {
                var puts = entities.stream()
                    .collect(Collectors.toMap(
                        e -> buildKey(getId(e)),
                        e -> {
                            try {
                                return objectMapper.writeValueAsString(e);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    ));
                levelDB.batchWrite(puts, null).await().indefinitely();
                return entities;
            } catch (Exception e) {
                throw new RuntimeException("Failed to persist all entities", e);
            }
        });
    }

    /**
     * Delete all entities
     */
    public Uni<Void> deleteAll() {
        return Uni.createFrom().item(() -> {
            try {
                List<String> keys = levelDB.getKeysByPrefix(getKeyPrefix()).await().indefinitely();
                levelDB.batchWrite(null, keys).await().indefinitely();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete all entities", e);
            }
        });
    }

    // ==================== HELPER METHODS ====================

    /**
     * Build storage key from ID
     */
    protected String buildKey(ID id) {
        return getKeyPrefix() + id.toString();
    }

    /**
     * Extract ID from storage key
     */
    protected String extractId(String key) {
        return key.substring(getKeyPrefix().length());
    }
}
