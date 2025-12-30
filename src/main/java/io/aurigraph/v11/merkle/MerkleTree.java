package io.aurigraph.v11.merkle;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.util.encoders.Hex;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * High-performance Merkle Tree implementation for Aurigraph V11
 *
 * Features:
 * - SHA3-256 cryptographic hashing (quantum-resistant)
 * - Efficient proof generation and verification
 * - Concurrent access support
 * - Optimized for blockchain registry integrity
 *
 * @version 11.3.2
 * @author Backend Development Agent (BDA)
 */
public class MerkleTree<T> {

    private final List<T> leaves;
    private final List<String> leafHashes;
    private final Map<Integer, List<String>> levels;
    private String rootHash;
    private final MerkleHasher<T> hasher;

    /**
     * Create Merkle tree from leaf data
     */
    public MerkleTree(List<T> data, MerkleHasher<T> hasher) {
        this.leaves = new ArrayList<>(data);
        this.hasher = hasher;
        this.levels = new ConcurrentHashMap<>();
        this.leafHashes = new ArrayList<>();

        if (data.isEmpty()) {
            this.rootHash = computeHash("EMPTY_TREE");
        } else {
            buildTree();
        }
    }

    /**
     * Build the Merkle tree from leaves to root
     */
    private void buildTree() {
        // Hash all leaf data
        leafHashes.clear();
        for (T leaf : leaves) {
            leafHashes.add(hasher.hash(leaf));
        }

        // Build tree levels bottom-up
        List<String> currentLevel = new ArrayList<>(leafHashes);
        int levelIndex = 0;
        levels.put(levelIndex, new ArrayList<>(currentLevel));

        while (currentLevel.size() > 1) {
            currentLevel = buildNextLevel(currentLevel);
            levelIndex++;
            levels.put(levelIndex, new ArrayList<>(currentLevel));
        }

        // Root is the single hash at the top level
        rootHash = currentLevel.get(0);
    }

    /**
     * Build next level of tree by pairing and hashing
     */
    private List<String> buildNextLevel(List<String> currentLevel) {
        List<String> nextLevel = new ArrayList<>();

        for (int i = 0; i < currentLevel.size(); i += 2) {
            String left = currentLevel.get(i);
            String right;

            if (i + 1 < currentLevel.size()) {
                right = currentLevel.get(i + 1);
            } else {
                // Odd number of nodes - duplicate the last one
                right = left;
            }

            String combinedHash = computeHash(left + right);
            nextLevel.add(combinedHash);
        }

        return nextLevel;
    }

    /**
     * Generate Merkle proof for a specific leaf
     */
    public MerkleProof generateProof(int leafIndex) {
        if (leafIndex < 0 || leafIndex >= leaves.size()) {
            throw new IllegalArgumentException("Invalid leaf index: " + leafIndex);
        }

        List<MerkleProof.ProofElement> proofPath = new ArrayList<>();
        int currentIndex = leafIndex;

        // Traverse from leaf to root
        for (int level = 0; level < levels.size() - 1; level++) {
            List<String> currentLevel = levels.get(level);

            // Determine sibling index
            int siblingIndex;
            boolean isLeft;

            if (currentIndex % 2 == 0) {
                // Current node is left child
                siblingIndex = currentIndex + 1;
                isLeft = true;
            } else {
                // Current node is right child
                siblingIndex = currentIndex - 1;
                isLeft = false;
            }

            // Get sibling hash (duplicate if odd)
            String siblingHash;
            if (siblingIndex < currentLevel.size()) {
                siblingHash = currentLevel.get(siblingIndex);
            } else {
                siblingHash = currentLevel.get(currentIndex);
            }

            proofPath.add(new MerkleProof.ProofElement(siblingHash, isLeft));

            // Move to parent index
            currentIndex = currentIndex / 2;
        }

        return new MerkleProof(
            leafHashes.get(leafIndex),
            rootHash,
            leafIndex,
            proofPath
        );
    }

    /**
     * Verify a Merkle proof
     */
    public boolean verifyProof(MerkleProof proof) {
        if (proof == null || !proof.getRootHash().equals(this.rootHash)) {
            return false;
        }

        String currentHash = proof.getLeafHash();

        for (MerkleProof.ProofElement element : proof.getProofPath()) {
            if (element.isLeft()) {
                // Sibling is on the left
                currentHash = computeHash(element.getSiblingHash() + currentHash);
            } else {
                // Sibling is on the right
                currentHash = computeHash(currentHash + element.getSiblingHash());
            }
        }

        return currentHash.equals(proof.getRootHash());
    }

    /**
     * Static verification without tree instance
     */
    public static boolean verifyProofStatic(MerkleProof proof) {
        String currentHash = proof.getLeafHash();

        for (MerkleProof.ProofElement element : proof.getProofPath()) {
            if (element.isLeft()) {
                currentHash = computeHashStatic(element.getSiblingHash() + currentHash);
            } else {
                currentHash = computeHashStatic(currentHash + element.getSiblingHash());
            }
        }

        return currentHash.equals(proof.getRootHash());
    }

    /**
     * Update tree with new data (rebuild)
     */
    public void update(List<T> newData) {
        this.leaves.clear();
        this.leaves.addAll(newData);
        this.levels.clear();
        this.leafHashes.clear();

        if (newData.isEmpty()) {
            this.rootHash = computeHash("EMPTY_TREE");
        } else {
            buildTree();
        }
    }

    /**
     * Add single leaf (triggers rebuild)
     */
    public void addLeaf(T leaf) {
        this.leaves.add(leaf);
        buildTree();
    }

    /**
     * Compute SHA3-256 hash
     */
    private String computeHash(String input) {
        return computeHashStatic(input);
    }

    /**
     * Static SHA3-256 hash computation
     */
    private static String computeHashStatic(String input) {
        SHA3Digest digest = new SHA3Digest(256);
        byte[] inputBytes = input.getBytes();
        digest.update(inputBytes, 0, inputBytes.length);
        byte[] hash = new byte[32];
        digest.doFinal(hash, 0);
        return Hex.toHexString(hash);
    }

    // Getters
    public String getRootHash() {
        return rootHash;
    }

    public List<T> getLeaves() {
        return new ArrayList<>(leaves);
    }

    public List<String> getLeafHashes() {
        return new ArrayList<>(leafHashes);
    }

    public int getTreeHeight() {
        return levels.size();
    }

    public int getLeafCount() {
        return leaves.size();
    }

    /**
     * Get tree statistics
     */
    public MerkleTreeStats getStats() {
        return new MerkleTreeStats(
            rootHash,
            leaves.size(),
            levels.size(),
            leafHashes.size()
        );
    }

    /**
     * Functional interface for hashing custom objects
     */
    @FunctionalInterface
    public interface MerkleHasher<T> {
        String hash(T data);
    }

    /**
     * Tree statistics
     */
    public static class MerkleTreeStats {
        private final String rootHash;
        private final int leafCount;
        private final int treeHeight;
        private final int totalHashes;

        public MerkleTreeStats(String rootHash, int leafCount, int treeHeight, int totalHashes) {
            this.rootHash = rootHash;
            this.leafCount = leafCount;
            this.treeHeight = treeHeight;
            this.totalHashes = totalHashes;
        }

        public String getRootHash() { return rootHash; }
        public int getLeafCount() { return leafCount; }
        public int getTreeHeight() { return treeHeight; }
        public int getTotalHashes() { return totalHashes; }

        @Override
        public String toString() {
            return String.format("MerkleTree[root=%s, leaves=%d, height=%d, hashes=%d]",
                rootHash.substring(0, 16) + "...", leafCount, treeHeight, totalHashes);
        }
    }
}
