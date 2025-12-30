package io.aurigraph.v11.merkle;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base Merkle Tree Registry for cryptographically verifiable data storage
 *
 * Provides:
 * - Automatic Merkle tree construction and maintenance
 * - Proof generation and verification
 * - Root hash tracking for tamper detection
 * - Thread-safe concurrent access
 *
 * @param <T> The type of data stored in the registry
 * @version 11.3.2
 * @author Backend Development Agent (BDA)
 */
public abstract class MerkleTreeRegistry<T> {

    private static final Logger LOG = Logger.getLogger(MerkleTreeRegistry.class);

    protected final Map<String, T> registry = new ConcurrentHashMap<>();
    protected MerkleTree<Map.Entry<String, T>> merkleTree;
    protected final ReadWriteLock treeLock = new ReentrantReadWriteLock();

    protected String currentRootHash;
    protected Instant lastTreeUpdate;
    protected long treeRebuildCount = 0;

    /**
     * Initialize registry with empty Merkle tree
     */
    public MerkleTreeRegistry() {
        rebuildMerkleTree();
    }

    /**
     * Add entry to registry and update Merkle tree
     */
    public Uni<Boolean> add(String key, T value) {
        return Uni.createFrom().item(() -> {
            treeLock.writeLock().lock();
            try {
                registry.put(key, value);
                rebuildMerkleTree();
                LOG.debugf("Added entry %s to registry, new root: %s", key, currentRootHash.substring(0, 16));
                return true;
            } finally {
                treeLock.writeLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Remove entry from registry and update Merkle tree
     */
    public Uni<Boolean> remove(String key) {
        return Uni.createFrom().item(() -> {
            treeLock.writeLock().lock();
            try {
                T removed = registry.remove(key);
                if (removed != null) {
                    rebuildMerkleTree();
                    LOG.debugf("Removed entry %s from registry, new root: %s", key, currentRootHash.substring(0, 16));
                    return true;
                }
                return false;
            } finally {
                treeLock.writeLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get entry from registry
     */
    public Uni<T> get(String key) {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                return registry.get(key);
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all entries
     */
    public Uni<List<T>> getAll() {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                List<T> result = new ArrayList<>(registry.values());
                return result;
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Generate Merkle proof for an entry
     */
    public Uni<MerkleProof> generateProof(String key) {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                if (!registry.containsKey(key)) {
                    throw new IllegalArgumentException("Key not found in registry: " + key);
                }

                // Find index of this entry
                List<Map.Entry<String, T>> entries = new ArrayList<>(registry.entrySet());
                int index = -1;
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).getKey().equals(key)) {
                        index = i;
                        break;
                    }
                }

                if (index == -1) {
                    throw new IllegalStateException("Failed to find entry index");
                }

                return merkleTree.generateProof(index);
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Verify a Merkle proof
     */
    public Uni<Boolean> verifyProof(MerkleProof proof) {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                return merkleTree.verifyProof(proof);
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get current Merkle root hash
     */
    public Uni<String> getRootHash() {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                return currentRootHash;
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get Merkle tree statistics
     */
    public Uni<MerkleTreeStats> getTreeStats() {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                return new MerkleTreeStats(
                    currentRootHash,
                    registry.size(),
                    merkleTree.getTreeHeight(),
                    lastTreeUpdate,
                    treeRebuildCount
                );
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Rebuild Merkle tree from current registry state
     */
    protected void rebuildMerkleTree() {
        List<Map.Entry<String, T>> entries = new ArrayList<>(registry.entrySet());

        // Sort entries by key for consistent ordering
        entries.sort(Comparator.comparing(Map.Entry::getKey));

        // Create new Merkle tree
        merkleTree = new MerkleTree<>(entries, this::hashEntry);

        currentRootHash = merkleTree.getRootHash();
        lastTreeUpdate = Instant.now();
        treeRebuildCount++;
    }

    /**
     * Hash a registry entry (key + value)
     * Subclasses can override for custom hashing
     */
    protected String hashEntry(Map.Entry<String, T> entry) {
        String data = entry.getKey() + serializeValue(entry.getValue());
        return MerkleHashUtil.sha3Hash(data);
    }

    /**
     * Serialize value for hashing
     * Subclasses MUST override this method
     */
    protected abstract String serializeValue(T value);

    /**
     * Registry statistics including Merkle tree info
     */
    public static class MerkleTreeStats {
        private final String rootHash;
        private final int entryCount;
        private final int treeHeight;
        private final Instant lastUpdate;
        private final long rebuildCount;

        public MerkleTreeStats(String rootHash, int entryCount, int treeHeight,
                             Instant lastUpdate, long rebuildCount) {
            this.rootHash = rootHash;
            this.entryCount = entryCount;
            this.treeHeight = treeHeight;
            this.lastUpdate = lastUpdate;
            this.rebuildCount = rebuildCount;
        }

        public String getRootHash() { return rootHash; }
        public int getEntryCount() { return entryCount; }
        public int getTreeHeight() { return treeHeight; }
        public Instant getLastUpdate() { return lastUpdate; }
        public long getRebuildCount() { return rebuildCount; }

        @Override
        public String toString() {
            return String.format("MerkleRegistry[entries=%d, height=%d, root=%s, rebuilds=%d]",
                entryCount, treeHeight, rootHash.substring(0, 16) + "...", rebuildCount);
        }
    }

    /**
     * Utility class for SHA3 hashing
     */
    protected static class MerkleHashUtil {
        public static String sha3Hash(String input) {
            org.bouncycastle.crypto.digests.SHA3Digest digest =
                new org.bouncycastle.crypto.digests.SHA3Digest(256);
            byte[] inputBytes = input.getBytes();
            digest.update(inputBytes, 0, inputBytes.length);
            byte[] hash = new byte[32];
            digest.doFinal(hash, 0);
            return org.bouncycastle.util.encoders.Hex.toHexString(hash);
        }
    }
}
