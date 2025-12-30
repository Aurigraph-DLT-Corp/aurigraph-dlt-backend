package io.aurigraph.v11.merkle;

import java.util.ArrayList;
import java.util.List;

/**
 * Merkle Proof for cryptographic verification
 *
 * Contains the path from a leaf to the root, enabling efficient verification
 * without needing the entire tree.
 *
 * @version 11.3.2
 * @author Backend Development Agent (BDA)
 */
public class MerkleProof {

    private final String leafHash;
    private final String rootHash;
    private final int leafIndex;
    private final List<ProofElement> proofPath;

    public MerkleProof(String leafHash, String rootHash, int leafIndex, List<ProofElement> proofPath) {
        this.leafHash = leafHash;
        this.rootHash = rootHash;
        this.leafIndex = leafIndex;
        this.proofPath = new ArrayList<>(proofPath);
    }

    /**
     * Proof element containing sibling hash and position
     */
    public static class ProofElement {
        private final String siblingHash;
        private final boolean isLeft;

        public ProofElement(String siblingHash, boolean isLeft) {
            this.siblingHash = siblingHash;
            this.isLeft = isLeft;
        }

        public String getSiblingHash() {
            return siblingHash;
        }

        public boolean isLeft() {
            return isLeft;
        }

        @Override
        public String toString() {
            return String.format("ProofElement[%s, %s]",
                siblingHash.substring(0, 16) + "...",
                isLeft ? "left" : "right");
        }
    }

    // Getters
    public String getLeafHash() {
        return leafHash;
    }

    public String getRootHash() {
        return rootHash;
    }

    public int getLeafIndex() {
        return leafIndex;
    }

    public List<ProofElement> getProofPath() {
        return new ArrayList<>(proofPath);
    }

    public int getProofLength() {
        return proofPath.size();
    }

    /**
     * Convert to JSON-compatible format
     */
    public ProofData toProofData() {
        List<String> siblingHashes = new ArrayList<>();
        List<Boolean> positions = new ArrayList<>();

        for (ProofElement element : proofPath) {
            siblingHashes.add(element.getSiblingHash());
            positions.add(element.isLeft());
        }

        return new ProofData(leafHash, rootHash, leafIndex, siblingHashes, positions);
    }

    /**
     * JSON-serializable proof data
     */
    public static class ProofData {
        public String leafHash;
        public String rootHash;
        public int leafIndex;
        public List<String> siblingHashes;
        public List<Boolean> positions;

        public ProofData() {}

        public ProofData(String leafHash, String rootHash, int leafIndex,
                        List<String> siblingHashes, List<Boolean> positions) {
            this.leafHash = leafHash;
            this.rootHash = rootHash;
            this.leafIndex = leafIndex;
            this.siblingHashes = siblingHashes;
            this.positions = positions;
        }

        /**
         * Convert back to MerkleProof
         */
        public MerkleProof toMerkleProof() {
            List<ProofElement> elements = new ArrayList<>();
            for (int i = 0; i < siblingHashes.size(); i++) {
                elements.add(new ProofElement(siblingHashes.get(i), positions.get(i)));
            }
            return new MerkleProof(leafHash, rootHash, leafIndex, elements);
        }
    }

    @Override
    public String toString() {
        return String.format("MerkleProof[leaf=%s, root=%s, index=%d, pathLength=%d]",
            leafHash.substring(0, 16) + "...",
            rootHash.substring(0, 16) + "...",
            leafIndex,
            proofPath.size());
    }
}
