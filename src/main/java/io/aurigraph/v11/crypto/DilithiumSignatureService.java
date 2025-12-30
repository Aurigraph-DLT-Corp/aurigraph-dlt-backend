package io.aurigraph.v11.crypto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;
import org.jboss.logging.Logger;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CRYSTALS-Dilithium Digital Signature Service
 * 
 * Implements NIST Level 5 post-quantum digital signatures using CRYSTALS-Dilithium5
 * Provides high-performance signature generation and verification operations
 * optimized for sub-10ms verification performance targets.
 * 
 * CRYSTALS-Dilithium is a lattice-based signature scheme selected by NIST
 * for post-quantum cryptographic standardization.
 */
@ApplicationScoped
public class DilithiumSignatureService {
    
    private static final Logger LOG = Logger.getLogger(DilithiumSignatureService.class);
    
    // CRYSTALS-Dilithium algorithm constants
    public static final String DILITHIUM_ALGORITHM = "Dilithium";
    public static final String PROVIDER = "BCPQC";
    
    // Dilithium5 parameters (NIST Level 5)
    private static final DilithiumParameterSpec DILITHIUM5 = DilithiumParameterSpec.dilithium5;
    
    // Cryptographic components
    private KeyPairGenerator keyPairGenerator;
    private final ConcurrentHashMap<String, KeyPair> keyPairCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Signature> signerCache = new ConcurrentHashMap<>();
    
    // Performance metrics
    private long keyGenerationCount = 0;
    private long signingCount = 0;
    private long verificationCount = 0;
    private long totalKeyGenTime = 0;
    private long totalSigningTime = 0;
    private long totalVerificationTime = 0;
    
    /**
     * Initialize Dilithium signature service with NIST Level 5 parameters
     */
    @PostConstruct
    public void initialize() {
        try {
            // Ensure BouncyCastle PQC provider is available
            if (Security.getProvider(PROVIDER) == null) {
                Security.addProvider(new BouncyCastlePQCProvider());
            }
            
            // Initialize Dilithium key pair generator with Level 5 parameters
            keyPairGenerator = KeyPairGenerator.getInstance(DILITHIUM_ALGORITHM, PROVIDER);
            keyPairGenerator.initialize(DILITHIUM5, new SecureRandom());
            
            LOG.info("DilithiumSignatureService initialized with CRYSTALS-Dilithium5 (NIST Level 5)");
            
        } catch (Exception e) {
            LOG.error("Failed to initialize DilithiumSignatureService", e);
            throw new RuntimeException("Dilithium initialization failed", e);
        }
    }
    
    /**
     * Generate a new CRYSTALS-Dilithium5 key pair
     * 
     * @return Generated key pair for Level 5 quantum resistance
     */
    public KeyPair generateKeyPair() {
        long startTime = System.nanoTime();
        
        try {
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            // Cache the key pair for potential reuse
            String keyId = generateKeyId();
            keyPairCache.put(keyId, keyPair);
            
            // Update performance metrics
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            synchronized (this) {
                keyGenerationCount++;
                totalKeyGenTime += duration;
            }
            
            LOG.debug("Generated Dilithium5 key pair in " + duration + "ms (keyId: " + keyId + ")");
            
            if (duration > 500) {
                LOG.warn("Dilithium key generation took longer than expected: " + duration + "ms");
            }
            
            return keyPair;
            
        } catch (Exception e) {
            LOG.error("Dilithium key pair generation failed", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }
    
    /**
     * Sign data using CRYSTALS-Dilithium5 digital signature
     * 
     * @param data The data to sign
     * @param privateKey The private key for signing
     * @return Digital signature bytes
     */
    public byte[] sign(byte[] data, PrivateKey privateKey) {
        long startTime = System.nanoTime();

        // Validate inputs BEFORE try block to avoid wrapping validation exceptions
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data to sign cannot be null or empty");
        }

        if (privateKey == null || !validatePrivateKey(privateKey)) {
            throw new IllegalArgumentException("Invalid Dilithium private key");
        }

        try {
            // Get or create signature instance
            Signature signer = getSignatureInstance();
            signer.initSign(privateKey);
            signer.update(data);

            byte[] signature = signer.sign();

            // Update performance metrics
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            synchronized (this) {
                signingCount++;
                totalSigningTime += duration;
            }

            LOG.debug("Dilithium signing completed in " + duration + "ms, signature size: " + signature.length);

            if (duration > 50) {
                LOG.warn("Dilithium signing took longer than expected: " + duration + "ms");
            }

            return signature;

        } catch (Exception e) {
            LOG.error("Dilithium signing failed", e);
            throw new RuntimeException("Signing operation failed", e);
        }
    }
    
    /**
     * Verify CRYSTALS-Dilithium5 digital signature
     * 
     * @param data The original data
     * @param signature The signature to verify
     * @param publicKey The public key for verification
     * @return true if signature is valid, false otherwise
     */
    public boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
        long startTime = System.nanoTime();

        // Validate inputs BEFORE try block to throw validation exceptions properly
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data to verify cannot be null or empty");
        }

        if (signature == null || signature.length == 0) {
            throw new IllegalArgumentException("Signature cannot be null or empty");
        }

        if (publicKey == null || !validatePublicKey(publicKey)) {
            throw new IllegalArgumentException("Invalid Dilithium public key");
        }

        try {
            // Get or create signature instance
            Signature verifier = getSignatureInstance();
            verifier.initVerify(publicKey);
            verifier.update(data);

            boolean isValid = verifier.verify(signature);

            // Update performance metrics
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            synchronized (this) {
                verificationCount++;
                totalVerificationTime += duration;
            }

            LOG.debug("Dilithium verification completed in " + duration + "ms, result: " + isValid);

            if (duration > 10) {
                LOG.warn("Dilithium verification exceeded 10ms target: " + duration + "ms");
            }

            return isValid;

        } catch (Exception e) {
            LOG.debug("Dilithium verification failed with exception: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Batch sign multiple data items for improved performance
     * 
     * @param dataItems Array of data to sign
     * @param privateKey The private key for signing
     * @return Array of signatures corresponding to input data
     */
    public byte[][] batchSign(byte[][] dataItems, PrivateKey privateKey) {
        if (dataItems == null || dataItems.length == 0) {
            return new byte[0][];
        }
        
        long startTime = System.nanoTime();
        byte[][] signatures = new byte[dataItems.length][];
        
        try {
            Signature signer = getSignatureInstance();
            signer.initSign(privateKey);
            
            for (int i = 0; i < dataItems.length; i++) {
                if (dataItems[i] != null) {
                    signer.update(dataItems[i]);
                    signatures[i] = signer.sign();
                    
                    // Reset for next signature
                    signer.initSign(privateKey);
                }
            }
            
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            LOG.debug("Batch signed " + dataItems.length + " items in " + duration + "ms");
            
            return signatures;
            
        } catch (Exception e) {
            LOG.error("Batch signing failed", e);
            throw new RuntimeException("Batch signing failed", e);
        }
    }
    
    /**
     * Batch verify multiple signatures for improved performance
     * 
     * @param dataItems Array of original data
     * @param signatures Array of signatures to verify
     * @param publicKeys Array of public keys for verification
     * @return Array of verification results
     */
    public boolean[] batchVerify(byte[][] dataItems, byte[][] signatures, PublicKey[] publicKeys) {
        if (dataItems == null || signatures == null || publicKeys == null ||
            dataItems.length != signatures.length || dataItems.length != publicKeys.length) {
            throw new IllegalArgumentException("Input arrays must have matching lengths");
        }
        
        long startTime = System.nanoTime();
        boolean[] results = new boolean[dataItems.length];
        
        try {
            for (int i = 0; i < dataItems.length; i++) {
                if (dataItems[i] != null && signatures[i] != null && publicKeys[i] != null) {
                    results[i] = verify(dataItems[i], signatures[i], publicKeys[i]);
                } else {
                    results[i] = false;
                }
            }
            
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            LOG.debug("Batch verified " + dataItems.length + " signatures in " + duration + "ms");
            
            return results;
            
        } catch (Exception e) {
            LOG.error("Batch verification failed", e);
            throw new RuntimeException("Batch verification failed", e);
        }
    }
    
    /**
     * Validate that a public key is a valid Dilithium public key
     *
     * @param publicKey The public key to validate
     * @return true if the key is a valid Dilithium public key
     */
    public boolean validatePublicKey(PublicKey publicKey) {
        try {
            if (publicKey == null) {
                return false;
            }

            // Accept both "Dilithium" and variant names like "DILITHIUM5"
            String algorithm = publicKey.getAlgorithm();
            if (algorithm == null || !algorithm.toUpperCase().contains("DILITHIUM")) {
                return false;
            }

            // Try to encode and decode the key to validate its format
            byte[] encoded = publicKey.getEncoded();
            if (encoded == null || encoded.length == 0) {
                return false;
            }

            KeyFactory keyFactory = KeyFactory.getInstance(DILITHIUM_ALGORITHM, PROVIDER);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey reconstructed = keyFactory.generatePublic(keySpec);

            return reconstructed != null;

        } catch (Exception e) {
            LOG.debug("Public key validation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate that a private key is a valid Dilithium private key
     *
     * @param privateKey The private key to validate
     * @return true if the key is a valid Dilithium private key
     */
    public boolean validatePrivateKey(PrivateKey privateKey) {
        try {
            if (privateKey == null) {
                return false;
            }

            // Accept both "Dilithium" and variant names like "DILITHIUM5"
            String algorithm = privateKey.getAlgorithm();
            if (algorithm == null || !algorithm.toUpperCase().contains("DILITHIUM")) {
                return false;
            }

            // Try to encode and decode the key to validate its format
            byte[] encoded = privateKey.getEncoded();
            if (encoded == null || encoded.length == 0) {
                return false;
            }

            KeyFactory keyFactory = KeyFactory.getInstance(DILITHIUM_ALGORITHM, PROVIDER);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            PrivateKey reconstructed = keyFactory.generatePrivate(keySpec);

            return reconstructed != null;

        } catch (Exception e) {
            LOG.debug("Private key validation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get or create a signature instance for thread-safe operation
     */
    private Signature getSignatureInstance() throws Exception {
        String threadId = String.valueOf(Thread.currentThread().getId());
        
        // Try to reuse cached signature instance for this thread
        Signature signature = signerCache.get(threadId);
        if (signature == null) {
            signature = Signature.getInstance(DILITHIUM_ALGORITHM, PROVIDER);
            signerCache.put(threadId, signature);
        }
        
        return signature;
    }
    
    /**
     * Get Dilithium signature service performance metrics
     * 
     * @return Performance metrics object
     */
    public DilithiumMetrics getMetrics() {
        synchronized (this) {
            return new DilithiumMetrics(
                keyGenerationCount,
                signingCount,
                verificationCount,
                keyGenerationCount > 0 ? totalKeyGenTime / keyGenerationCount : 0,
                signingCount > 0 ? totalSigningTime / signingCount : 0,
                verificationCount > 0 ? totalVerificationTime / verificationCount : 0,
                keyPairCache.size(),
                signerCache.size()
            );
        }
    }
    
    /**
     * Clear caches to free memory
     */
    public void clearCaches() {
        int keyPairsBefore = keyPairCache.size();
        int signersBefore = signerCache.size();
        
        keyPairCache.clear();
        signerCache.clear();
        
        LOG.debug("Dilithium caches cleared: " + keyPairsBefore + " key pairs, " + 
                 signersBefore + " signers removed");
    }
    
    /**
     * Generate a unique key identifier
     */
    private String generateKeyId() {
        return "dilithium5_" + System.currentTimeMillis() + "_" + 
               ThreadLocalRandom.current().nextInt(10000, 99999);
    }
    
    /**
     * Generate multi-party signature (m-of-n threshold signature)
     *
     * @param data The data to sign
     * @param privateKeys Array of private keys for signing
     * @param threshold Required number of signatures (m)
     * @return Multi-signature object containing all signatures
     */
    public MultiSignature generateMultiSignature(byte[] data, PrivateKey[] privateKeys, int threshold) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data to sign cannot be null or empty");
        }

        if (privateKeys == null || privateKeys.length == 0) {
            throw new IllegalArgumentException("Private keys array cannot be null or empty");
        }

        if (threshold < 1 || threshold > privateKeys.length) {
            throw new IllegalArgumentException("Threshold must be between 1 and " + privateKeys.length);
        }

        long startTime = System.nanoTime();

        try {
            List<byte[]> signatures = new ArrayList<>(privateKeys.length);
            List<PublicKey> publicKeys = new ArrayList<>(privateKeys.length);

            // Generate signature with each private key
            for (PrivateKey privateKey : privateKeys) {
                if (privateKey != null && validatePrivateKey(privateKey)) {
                    byte[] signature = sign(data, privateKey);
                    signatures.add(signature);

                    // Derive public key from key pair (in production, would be provided separately)
                    publicKeys.add(null); // Placeholder - would be actual public key
                }
            }

            if (signatures.size() < threshold) {
                throw new RuntimeException("Failed to generate required number of signatures: " +
                                         signatures.size() + " < " + threshold);
            }

            long duration = (System.nanoTime() - startTime) / 1_000_000;
            LOG.debug("Generated multi-signature with " + signatures.size() + " signatures in " + duration + "ms");

            return new MultiSignature(signatures, publicKeys, threshold, System.currentTimeMillis());

        } catch (Exception e) {
            LOG.error("Multi-signature generation failed", e);
            throw new RuntimeException("Multi-signature generation failed", e);
        }
    }

    /**
     * Verify multi-party signature
     *
     * @param data The original data
     * @param multiSig The multi-signature to verify
     * @param publicKeys Array of public keys for verification
     * @return true if threshold number of signatures are valid
     */
    public boolean verifyMultiSignature(byte[] data, MultiSignature multiSig, PublicKey[] publicKeys) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data to verify cannot be null or empty");
        }

        if (multiSig == null) {
            throw new IllegalArgumentException("Multi-signature cannot be null");
        }

        if (publicKeys == null || publicKeys.length == 0) {
            throw new IllegalArgumentException("Public keys array cannot be null or empty");
        }

        long startTime = System.nanoTime();

        try {
            List<byte[]> signatures = multiSig.getSignatures();
            int threshold = multiSig.getThreshold();

            int validSignatures = 0;

            // Verify each signature against corresponding public key
            for (int i = 0; i < Math.min(signatures.size(), publicKeys.length); i++) {
                if (publicKeys[i] != null && signatures.get(i) != null) {
                    if (verify(data, signatures.get(i), publicKeys[i])) {
                        validSignatures++;
                    }
                }

                // Early exit if threshold met
                if (validSignatures >= threshold) {
                    break;
                }
            }

            boolean isValid = validSignatures >= threshold;

            long duration = (System.nanoTime() - startTime) / 1_000_000;
            LOG.debug("Multi-signature verification completed in " + duration + "ms: " +
                     validSignatures + "/" + threshold + " valid signatures");

            return isValid;

        } catch (Exception e) {
            LOG.error("Multi-signature verification failed", e);
            return false;
        }
    }

    /**
     * Aggregate multiple signatures into a single multi-signature
     *
     * @param signatures Array of individual signatures
     * @param threshold Required number of valid signatures
     * @return Aggregated multi-signature
     */
    public MultiSignature aggregateSignatures(byte[][] signatures, int threshold) {
        if (signatures == null || signatures.length == 0) {
            throw new IllegalArgumentException("Signatures array cannot be null or empty");
        }

        if (threshold < 1 || threshold > signatures.length) {
            throw new IllegalArgumentException("Threshold must be between 1 and " + signatures.length);
        }

        List<byte[]> sigList = new ArrayList<>();
        for (byte[] sig : signatures) {
            if (sig != null && sig.length > 0) {
                sigList.add(sig);
            }
        }

        return new MultiSignature(sigList, new ArrayList<>(), threshold, System.currentTimeMillis());
    }

    /**
     * Rotate key pair - generate new key pair and mark old one for deprecation
     *
     * @param oldKeyId The ID of the key pair to rotate
     * @return New key pair with rotation metadata
     */
    public KeyRotationResult rotateKeyPair(String oldKeyId) {
        if (oldKeyId == null || oldKeyId.isEmpty()) {
            throw new IllegalArgumentException("Old key ID cannot be null or empty");
        }

        long startTime = System.nanoTime();

        try {
            // Generate new key pair
            KeyPair newKeyPair = generateKeyPair();
            String newKeyId = generateKeyId();

            // Remove old key from cache (in production, would be archived, not deleted)
            KeyPair oldKeyPair = keyPairCache.remove(oldKeyId);

            long duration = (System.nanoTime() - startTime) / 1_000_000;
            LOG.info("Key rotation completed in " + duration + "ms: " + oldKeyId + " -> " + newKeyId);

            return new KeyRotationResult(newKeyId, newKeyPair, oldKeyId, oldKeyPair,
                                        System.currentTimeMillis(), System.currentTimeMillis() + 2592000000L);

        } catch (Exception e) {
            LOG.error("Key rotation failed", e);
            throw new RuntimeException("Key rotation failed", e);
        }
    }

    /**
     * Batch verify multiple signatures with performance optimization
     *
     * @param dataItems Array of original data
     * @param signatures Array of signatures to verify
     * @param publicKeys Array of public keys for verification
     * @return Count of valid signatures
     */
    public int batchVerifyCount(byte[][] dataItems, byte[][] signatures, PublicKey[] publicKeys) {
        boolean[] results = batchVerify(dataItems, signatures, publicKeys);

        int validCount = 0;
        for (boolean result : results) {
            if (result) {
                validCount++;
            }
        }

        return validCount;
    }

    /**
     * Shutdown the Dilithium signature service
     */
    public void shutdown() {
        try {
            clearCaches();
            keyPairGenerator = null;

            LOG.info("DilithiumSignatureService shutdown completed");

        } catch (Exception e) {
            LOG.error("Error during DilithiumSignatureService shutdown", e);
        }
    }

    // ==================== Multi-Signature Data Classes ====================

    /**
     * Multi-signature container for threshold signatures
     */
    public static class MultiSignature {
        private final List<byte[]> signatures;
        private final List<PublicKey> publicKeys;
        private final int threshold;
        private final long timestamp;

        public MultiSignature(List<byte[]> signatures, List<PublicKey> publicKeys,
                            int threshold, long timestamp) {
            this.signatures = new ArrayList<>(signatures);
            this.publicKeys = new ArrayList<>(publicKeys);
            this.threshold = threshold;
            this.timestamp = timestamp;
        }

        public List<byte[]> getSignatures() {
            return new ArrayList<>(signatures);
        }

        public List<PublicKey> getPublicKeys() {
            return new ArrayList<>(publicKeys);
        }

        public int getThreshold() {
            return threshold;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getSignatureCount() {
            return signatures.size();
        }

        @Override
        public String toString() {
            return "MultiSignature{" +
                   "signatures=" + signatures.size() + ", " +
                   "threshold=" + threshold + ", " +
                   "timestamp=" + timestamp + "}";
        }
    }

    /**
     * Key rotation result containing old and new key information
     */
    public static class KeyRotationResult {
        private final String newKeyId;
        private final KeyPair newKeyPair;
        private final String oldKeyId;
        private final KeyPair oldKeyPair;
        private final long rotationTimestamp;
        private final long oldKeyExpiryTimestamp;

        public KeyRotationResult(String newKeyId, KeyPair newKeyPair,
                               String oldKeyId, KeyPair oldKeyPair,
                               long rotationTimestamp, long oldKeyExpiryTimestamp) {
            this.newKeyId = newKeyId;
            this.newKeyPair = newKeyPair;
            this.oldKeyId = oldKeyId;
            this.oldKeyPair = oldKeyPair;
            this.rotationTimestamp = rotationTimestamp;
            this.oldKeyExpiryTimestamp = oldKeyExpiryTimestamp;
        }

        public String getNewKeyId() { return newKeyId; }
        public KeyPair getNewKeyPair() { return newKeyPair; }
        public String getOldKeyId() { return oldKeyId; }
        public KeyPair getOldKeyPair() { return oldKeyPair; }
        public long getRotationTimestamp() { return rotationTimestamp; }
        public long getOldKeyExpiryTimestamp() { return oldKeyExpiryTimestamp; }

        @Override
        public String toString() {
            return "KeyRotationResult{" +
                   "oldKeyId='" + oldKeyId + "', " +
                   "newKeyId='" + newKeyId + "', " +
                   "rotationTimestamp=" + rotationTimestamp + "}";
        }
    }
    
    /**
     * Dilithium performance metrics
     */
    public static class DilithiumMetrics {
        private final long keyGenerationCount;
        private final long signingCount;
        private final long verificationCount;
        private final long avgKeyGenTime;
        private final long avgSigningTime;
        private final long avgVerificationTime;
        private final int keyPairCacheSize;
        private final int signerCacheSize;
        
        public DilithiumMetrics(long keyGenerationCount, long signingCount, long verificationCount,
                               long avgKeyGenTime, long avgSigningTime, long avgVerificationTime,
                               int keyPairCacheSize, int signerCacheSize) {
            this.keyGenerationCount = keyGenerationCount;
            this.signingCount = signingCount;
            this.verificationCount = verificationCount;
            this.avgKeyGenTime = avgKeyGenTime;
            this.avgSigningTime = avgSigningTime;
            this.avgVerificationTime = avgVerificationTime;
            this.keyPairCacheSize = keyPairCacheSize;
            this.signerCacheSize = signerCacheSize;
        }
        
        // Getters
        public long getKeyGenerationCount() { return keyGenerationCount; }
        public long getSigningCount() { return signingCount; }
        public long getVerificationCount() { return verificationCount; }
        public long getAvgKeyGenTime() { return avgKeyGenTime; }
        public long getAvgSigningTime() { return avgSigningTime; }
        public long getAvgVerificationTime() { return avgVerificationTime; }
        public int getKeyPairCacheSize() { return keyPairCacheSize; }
        public int getSignerCacheSize() { return signerCacheSize; }
        
        @Override
        public String toString() {
            return "DilithiumMetrics{" +
                   "keyGen=" + keyGenerationCount + " (" + avgKeyGenTime + "ms avg), " +
                   "signing=" + signingCount + " (" + avgSigningTime + "ms avg), " +
                   "verify=" + verificationCount + " (" + avgVerificationTime + "ms avg), " +
                   "keyCache=" + keyPairCacheSize + ", signerCache=" + signerCacheSize + "}";
        }
    }
}