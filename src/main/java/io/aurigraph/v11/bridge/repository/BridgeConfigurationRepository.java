package io.aurigraph.v11.bridge.repository;

import io.aurigraph.v11.bridge.factory.ChainFamily;
import io.aurigraph.v11.bridge.model.BridgeChainConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BridgeChainConfig using Jakarta Persistence
 * Provides database operations for chain configuration management
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 1.0.0
 */
@ApplicationScoped
@Transactional
public class BridgeConfigurationRepository {

    private static final Logger logger = LoggerFactory.getLogger(BridgeConfigurationRepository.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Find configuration by chain name (case-insensitive)
     */
    public Optional<BridgeChainConfig> findByChainName(String chainName) {
        try {
            String query = "SELECT c FROM BridgeChainConfig c WHERE LOWER(c.chainName) = LOWER(:chainName)";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            q.setParameter("chainName", chainName);
            return q.getResultList().stream().findFirst();
        } catch (Exception e) {
            logger.warn("Error finding chain by name: {}", chainName, e);
            return Optional.empty();
        }
    }

    /**
     * Find configuration by chain ID
     */
    public Optional<BridgeChainConfig> findByChainId(String chainId) {
        try {
            String query = "SELECT c FROM BridgeChainConfig c WHERE c.chainId = :chainId";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            q.setParameter("chainId", chainId);
            return q.getResultList().stream().findFirst();
        } catch (Exception e) {
            logger.warn("Error finding chain by ID: {}", chainId, e);
            return Optional.empty();
        }
    }

    /**
     * Find all enabled chains
     */
    public List<BridgeChainConfig> findByEnabledTrue() {
        try {
            String query = "SELECT c FROM BridgeChainConfig c WHERE c.enabled = true ORDER BY c.chainName";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding enabled chains", e);
            return List.of();
        }
    }

    /**
     * Find all disabled chains
     */
    public List<BridgeChainConfig> findByEnabledFalse() {
        try {
            String query = "SELECT c FROM BridgeChainConfig c WHERE c.enabled = false ORDER BY c.chainName";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding disabled chains", e);
            return List.of();
        }
    }

    /**
     * Find all configurations for a specific chain family
     */
    public List<BridgeChainConfig> findByFamily(ChainFamily family) {
        try {
            String query = "SELECT c FROM BridgeChainConfig c WHERE c.family = :family ORDER BY c.chainName";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            q.setParameter("family", family);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding chains by family: {}", family, e);
            return List.of();
        }
    }

    /**
     * Find all enabled configurations for a specific chain family
     */
    public List<BridgeChainConfig> findEnabledByFamily(ChainFamily family) {
        try {
            String query = "SELECT c FROM BridgeChainConfig c WHERE c.family = :family AND c.enabled = true ORDER BY c.chainName";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            q.setParameter("family", family);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding enabled chains by family: {}", family, e);
            return List.of();
        }
    }

    /**
     * Get all chain names
     */
    public List<String> findAllChainNames() {
        try {
            String query = "SELECT c.chainName FROM BridgeChainConfig c ORDER BY c.chainName";
            TypedQuery<String> q = entityManager.createQuery(query, String.class);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding all chain names", e);
            return List.of();
        }
    }

    /**
     * Get all enabled chain names
     */
    public List<String> findEnabledChainNames() {
        try {
            String query = "SELECT c.chainName FROM BridgeChainConfig c WHERE c.enabled = true ORDER BY c.chainName";
            TypedQuery<String> q = entityManager.createQuery(query, String.class);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding enabled chain names", e);
            return List.of();
        }
    }

    /**
     * Get all chain names for a specific family
     */
    public List<String> findChainNamesByFamily(ChainFamily family) {
        try {
            String query = "SELECT c.chainName FROM BridgeChainConfig c WHERE c.family = :family ORDER BY c.chainName";
            TypedQuery<String> q = entityManager.createQuery(query, String.class);
            q.setParameter("family", family);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding chain names by family: {}", family, e);
            return List.of();
        }
    }

    /**
     * Get all enabled chain names for a specific family
     */
    public List<String> findEnabledChainNamesByFamily(ChainFamily family) {
        try {
            String query = "SELECT c.chainName FROM BridgeChainConfig c WHERE c.family = :family AND c.enabled = true ORDER BY c.chainName";
            TypedQuery<String> q = entityManager.createQuery(query, String.class);
            q.setParameter("family", family);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding enabled chain names by family: {}", family, e);
            return List.of();
        }
    }

    /**
     * Check if a chain exists by name
     */
    public boolean existsByChainName(String chainName) {
        return findByChainName(chainName).isPresent();
    }

    /**
     * Check if a chain is enabled
     */
    public boolean isChainEnabled(String chainName) {
        try {
            Optional<BridgeChainConfig> config = findByChainName(chainName);
            return config.isPresent() && config.get().getEnabled();
        } catch (Exception e) {
            logger.warn("Error checking if chain is enabled: {}", chainName, e);
            return false;
        }
    }

    /**
     * Save a chain configuration
     */
    public BridgeChainConfig save(BridgeChainConfig config) {
        try {
            if (config.getId() == null) {
                entityManager.persist(config);
                return config;
            } else {
                return entityManager.merge(config);
            }
        } catch (Exception e) {
            logger.error("Error saving chain configuration: {}", config.getChainName(), e);
            throw new RuntimeException("Failed to save chain configuration", e);
        }
    }

    /**
     * Delete configuration by chain name
     */
    public void deleteByChainName(String chainName) {
        try {
            String query = "DELETE FROM BridgeChainConfig c WHERE c.chainName = :chainName";
            jakarta.persistence.Query q = entityManager.createQuery(query);
            q.setParameter("chainName", chainName);
            q.executeUpdate();
        } catch (Exception e) {
            logger.error("Error deleting chain configuration: {}", chainName, e);
            throw new RuntimeException("Failed to delete chain configuration", e);
        }
    }

    /**
     * Find all configurations
     */
    public List<BridgeChainConfig> findAllOrdered() {
        try {
            String query = "SELECT c FROM BridgeChainConfig c ORDER BY c.chainName";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding all configurations", e);
            return List.of();
        }
    }

    /**
     * Count total configured chains
     */
    public long count() {
        try {
            String query = "SELECT COUNT(c) FROM BridgeChainConfig c";
            TypedQuery<Long> q = entityManager.createQuery(query, Long.class);
            return q.getSingleResult();
        } catch (Exception e) {
            logger.warn("Error counting configurations", e);
            return 0;
        }
    }

    /**
     * Count enabled chains
     */
    public long countEnabled() {
        try {
            String query = "SELECT COUNT(c) FROM BridgeChainConfig c WHERE c.enabled = true";
            TypedQuery<Long> q = entityManager.createQuery(query, Long.class);
            return q.getSingleResult();
        } catch (Exception e) {
            logger.warn("Error counting enabled configurations", e);
            return 0;
        }
    }

    /**
     * Count chains by family
     */
    public long countByFamily(ChainFamily family) {
        try {
            String query = "SELECT COUNT(c) FROM BridgeChainConfig c WHERE c.family = :family";
            TypedQuery<Long> q = entityManager.createQuery(query, Long.class);
            q.setParameter("family", family);
            return q.getSingleResult();
        } catch (Exception e) {
            logger.warn("Error counting configurations by family: {}", family, e);
            return 0;
        }
    }

    /**
     * Find chains requiring RPC failover
     */
    public List<BridgeChainConfig> findChainsWithBackupRpc() {
        try {
            String query = "SELECT c FROM BridgeChainConfig c WHERE c.backupRpcUrls IS NOT NULL AND c.backupRpcUrls != '' ORDER BY c.chainName";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding chains with backup RPC", e);
            return List.of();
        }
    }

    /**
     * Search chains by display name
     */
    public List<BridgeChainConfig> searchByDisplayName(String searchTerm) {
        try {
            String query = "SELECT c FROM BridgeChainConfig c WHERE LOWER(c.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY c.chainName";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            q.setParameter("searchTerm", searchTerm);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error searching chains by display name: {}", searchTerm, e);
            return List.of();
        }
    }

    /**
     * Get configurations updated since a specific timestamp
     */
    public List<BridgeChainConfig> findRecentlyUpdated(LocalDateTime timestamp) {
        try {
            String query = "SELECT c FROM BridgeChainConfig c WHERE c.updatedAt > :timestamp ORDER BY c.updatedAt DESC";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            q.setParameter("timestamp", timestamp);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding recently updated configurations", e);
            return List.of();
        }
    }

    /**
     * Get all configurations with status
     */
    public List<BridgeChainConfig> findAllWithStatus() {
        try {
            String query = "SELECT c FROM BridgeChainConfig c ORDER BY c.enabled DESC, c.chainName";
            TypedQuery<BridgeChainConfig> q = entityManager.createQuery(query, BridgeChainConfig.class);
            return q.getResultList();
        } catch (Exception e) {
            logger.warn("Error finding all configurations with status", e);
            return List.of();
        }
    }

    /**
     * Verify all required fields are configured for a chain
     */
    public boolean isChainFullyConfigured(String chainName) {
        try {
            String query = "SELECT COUNT(c) FROM BridgeChainConfig c WHERE c.chainName = :chainName AND c.chainId IS NOT NULL AND c.rpcUrl IS NOT NULL AND c.family IS NOT NULL";
            TypedQuery<Long> q = entityManager.createQuery(query, Long.class);
            q.setParameter("chainName", chainName);
            return q.getSingleResult() > 0;
        } catch (Exception e) {
            logger.warn("Error checking if chain is fully configured: {}", chainName, e);
            return false;
        }
    }
}
