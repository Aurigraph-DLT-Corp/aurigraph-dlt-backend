package io.aurigraph.v11.crypto;

import jakarta.enterprise.context.ApplicationScoped;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.SPHINCSPlusParameterSpec;
import org.jboss.logging.Logger;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SPHINCS+ Hash-Based Signature Service
 * 
 * Implements NIST Level 5 post-quantum hash-based digital signatures using SPHINCS+-SHA2-256f-robust
 * Provides secure backup signature capability with stateless operation and extreme quantum resistance.
 * 
 * SPHINCS+ is a hash-based signature scheme selected by NIST for post-quantum standardization,
 * offering the highest level of security assurance based on well-understood cryptographic assumptions.
 * 
 * This service is designed as a backup to CRYSTALS-Dilithium for critical operations requiring
 * maximum security guarantees.
 */
@ApplicationScoped
public class SphincsPlusService {
    
    private static final Logger LOG = Logger.getLogger(SphincsPlusService.class);
    
    // SPHINCS+ algorithm constants
    public static final String SPHINCS_PLUS_ALGORITHM = "SPHINCS+";
    public static final String PROVIDER = "BCPQC";
    
    // SPHINCS+-SHA2-256f-robust parameters (NIST Level 5, fast variant)
    private static final SPHINCSPlusParameterSpec SPHINCS_PLUS_256F = SPHINCSPlusParameterSpec.sha2_256f_robust;
    
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
     * Initialize SPHINCS+ signature service with NIST Level 5 parameters
     */
    public void initialize() {
        try {
            // Ensure BouncyCastle PQC provider is available
            if (Security.getProvider(PROVIDER) == null) {
                Security.addProvider(new BouncyCastlePQCProvider());
            }
            
            // Initialize SPHINCS+ key pair generator with SHA2-256f-robust parameters
            keyPairGenerator = KeyPairGenerator.getInstance(SPHINCS_PLUS_ALGORITHM, PROVIDER);
            keyPairGenerator.initialize(SPHINCS_PLUS_256F, new SecureRandom());
            
            LOG.info("SphincsPlusService initialized with SPHINCS+-SHA2-256f-robust (NIST Level 5)");
            
        } catch (Exception e) {
            LOG.error("Failed to initialize SphincsPlusService", e);
            throw new RuntimeException("SPHINCS+ initialization failed", e);
        }
    }
    
    /**
     * Generate a new SPHINCS+-SHA2-256f-robust key pair
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
            
            LOG.debug("Generated SPHINCS+-256f key pair in " + duration + "ms (keyId: " + keyId + ")");
            
            // Note: SPHINCS+ key generation is typically slower than other schemes
            if (duration > 2000) {
                LOG.warn("SPHINCS+ key generation took longer than expected: " + duration + "ms");
            }
            
            return keyPair;
            
        } catch (Exception e) {
            LOG.error("SPHINCS+ key pair generation failed", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }
    
    /**
     * Sign data using SPHINCS+-SHA2-256f-robust digital signature
     * 
     * @param data The data to sign
     * @param privateKey The private key for signing
     * @return Digital signature bytes
     */
    public byte[] sign(byte[] data, PrivateKey privateKey) {
        long startTime = System.nanoTime();
        
        try {
            // Validate inputs
            if (data == null || data.length == 0) {
                throw new IllegalArgumentException("Data to sign cannot be null or empty");
            }
            
            if (privateKey == null || !validatePrivateKey(privateKey)) {
                throw new IllegalArgumentException("Invalid SPHINCS+ private key");
            }
            
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
            
            LOG.debug("SPHINCS+ signing completed in " + duration + "ms, signature size: " + signature.length);
            
            // SPHINCS+ signing can be slower than other schemes, especially for security
            if (duration > 200) {
                LOG.warn("SPHINCS+ signing took longer than expected: " + duration + "ms");
            }
            
            return signature;
            
        } catch (Exception e) {
            LOG.error("SPHINCS+ signing failed", e);
            throw new RuntimeException("Signing operation failed", e);
        }
    }
    
    /**
     * Verify SPHINCS+-SHA2-256f-robust digital signature
     * 
     * @param data The original data
     * @param signature The signature to verify
     * @param publicKey The public key for verification
     * @return true if signature is valid, false otherwise
     */
    public boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
        long startTime = System.nanoTime();
        
        try {
            // Validate inputs
            if (data == null || data.length == 0) {
                LOG.debug("Verification failed: data is null or empty");
                return false;
            }
            
            if (signature == null || signature.length == 0) {
                LOG.debug("Verification failed: signature is null or empty");
                return false;
            }
            
            if (publicKey == null || !validatePublicKey(publicKey)) {
                LOG.debug("Verification failed: invalid SPHINCS+ public key");
                return false;
            }
            
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
            
            LOG.debug("SPHINCS+ verification completed in " + duration + "ms, result: " + isValid);
            
            if (duration > 10) {
                LOG.warn("SPHINCS+ verification exceeded 10ms target: " + duration + "ms");
            }
            
            return isValid;
            
        } catch (Exception e) {
            LOG.debug("SPHINCS+ verification failed with exception: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate multiple signatures for different data items using the same key
     * SPHINCS+ is stateless, so this is safe and efficient
     * 
     * @param dataItems Array of data to sign
     * @param privateKey The private key for signing
     * @return Array of signatures corresponding to input data
     */
    public byte[][] signMultiple(byte[][] dataItems, PrivateKey privateKey) {
        if (dataItems == null || dataItems.length == 0) {
            return new byte[0][];
        }
        
        long startTime = System.nanoTime();
        byte[][] signatures = new byte[dataItems.length][];
        
        try {
            Signature signer = getSignatureInstance();
            
            for (int i = 0; i < dataItems.length; i++) {
                if (dataItems[i] != null) {
                    signer.initSign(privateKey);
                    signer.update(dataItems[i]);
                    signatures[i] = signer.sign();
                }
            }
            
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            LOG.debug("SPHINCS+ signed " + dataItems.length + " items in " + duration + "ms");
            
            return signatures;
            
        } catch (Exception e) {
            LOG.error("SPHINCS+ multiple signing failed", e);
            throw new RuntimeException("Multiple signing failed", e);
        }
    }
    
    /**
     * Verify multiple SPHINCS+ signatures efficiently
     * 
     * @param dataItems Array of original data
     * @param signatures Array of signatures to verify
     * @param publicKeys Array of public keys for verification
     * @return Array of verification results
     */
    public boolean[] verifyMultiple(byte[][] dataItems, byte[][] signatures, PublicKey[] publicKeys) {
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
            LOG.debug("SPHINCS+ verified " + dataItems.length + " signatures in " + duration + "ms");
            
            return results;
            
        } catch (Exception e) {
            LOG.error("SPHINCS+ multiple verification failed", e);
            throw new RuntimeException("Multiple verification failed", e);
        }
    }
    
    /**
     * Validate that a public key is a valid SPHINCS+ public key
     * 
     * @param publicKey The public key to validate
     * @return true if the key is a valid SPHINCS+ public key
     */
    public boolean validatePublicKey(PublicKey publicKey) {
        try {
            if (publicKey == null) {
                return false;
            }
            
            if (!SPHINCS_PLUS_ALGORITHM.equals(publicKey.getAlgorithm())) {
                return false;
            }
            
            // Try to encode and decode the key to validate its format
            byte[] encoded = publicKey.getEncoded();
            if (encoded == null || encoded.length == 0) {
                return false;
            }
            
            KeyFactory keyFactory = KeyFactory.getInstance(SPHINCS_PLUS_ALGORITHM, PROVIDER);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey reconstructed = keyFactory.generatePublic(keySpec);
            
            return reconstructed != null;
            
        } catch (Exception e) {
            LOG.debug("Public key validation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate that a private key is a valid SPHINCS+ private key
     * 
     * @param privateKey The private key to validate
     * @return true if the key is a valid SPHINCS+ private key
     */
    public boolean validatePrivateKey(PrivateKey privateKey) {
        try {
            if (privateKey == null) {
                return false;
            }
            
            if (!SPHINCS_PLUS_ALGORITHM.equals(privateKey.getAlgorithm())) {
                return false;
            }
            
            // Try to encode and decode the key to validate its format
            byte[] encoded = privateKey.getEncoded();
            if (encoded == null || encoded.length == 0) {
                return false;
            }
            
            KeyFactory keyFactory = KeyFactory.getInstance(SPHINCS_PLUS_ALGORITHM, PROVIDER);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            PrivateKey reconstructed = keyFactory.generatePrivate(keySpec);
            
            return reconstructed != null;
            
        } catch (Exception e) {
            LOG.debug("Private key validation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get signature size for SPHINCS+ signatures (informational)
     * 
     * @return Typical signature size in bytes
     */
    public int getSignatureSize() {
        // SPHINCS+-SHA2-256f-robust typically produces ~49KB signatures
        // This is much larger than Dilithium but provides maximum security
        return 49856; // Approximate size in bytes
    }
    
    /**
     * Get public key size for SPHINCS+ keys (informational)
     * 
     * @return Public key size in bytes
     */
    public int getPublicKeySize() {
        // SPHINCS+-SHA2-256f-robust public key size
        return 64; // bytes
    }
    
    /**
     * Check if SPHINCS+ is suitable for the given performance requirements
     * 
     * @param maxSignatureSize Maximum acceptable signature size in bytes
     * @param maxSigningTimeMs Maximum acceptable signing time in milliseconds
     * @return true if SPHINCS+ meets the requirements
     */
    public boolean isSuitableForRequirements(int maxSignatureSize, long maxSigningTimeMs) {
        return getSignatureSize() <= maxSignatureSize && 
               (totalSigningTime / Math.max(1, signingCount)) <= maxSigningTimeMs;
    }
    
    /**
     * Get or create a signature instance for thread-safe operation
     */
    private Signature getSignatureInstance() throws Exception {
        String threadId = String.valueOf(Thread.currentThread().threadId());
        
        // Try to reuse cached signature instance for this thread
        Signature signature = signerCache.get(threadId);
        if (signature == null) {
            signature = Signature.getInstance(SPHINCS_PLUS_ALGORITHM, PROVIDER);
            signerCache.put(threadId, signature);
        }
        
        return signature;
    }
    
    /**
     * Get SPHINCS+ service performance metrics
     * 
     * @return Performance metrics object
     */
    public SphincsPlusMetrics getMetrics() {
        synchronized (this) {
            return new SphincsPlusMetrics(
                keyGenerationCount,
                signingCount,
                verificationCount,
                keyGenerationCount > 0 ? totalKeyGenTime / keyGenerationCount : 0,
                signingCount > 0 ? totalSigningTime / signingCount : 0,
                verificationCount > 0 ? totalVerificationTime / verificationCount : 0,
                keyPairCache.size(),
                signerCache.size(),
                getSignatureSize(),
                getPublicKeySize()
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
        
        LOG.debug("SPHINCS+ caches cleared: " + keyPairsBefore + " key pairs, " + 
                 signersBefore + " signers removed");
    }
    
    /**
     * Generate a unique key identifier
     */
    private String generateKeyId() {
        return "sphincsplus_256f_" + System.currentTimeMillis() + "_" + 
               ThreadLocalRandom.current().nextInt(10000, 99999);
    }
    
    /**
     * Shutdown the SPHINCS+ signature service
     */
    public void shutdown() {
        try {
            clearCaches();
            keyPairGenerator = null;
            
            LOG.info("SphincsPlusService shutdown completed");
            
        } catch (Exception e) {
            LOG.error("Error during SphincsPlusService shutdown", e);
        }
    }
    
    /**
     * SPHINCS+ performance metrics
     */
    public static class SphincsPlusMetrics {
        private final long keyGenerationCount;
        private final long signingCount;
        private final long verificationCount;
        private final long avgKeyGenTime;
        private final long avgSigningTime;
        private final long avgVerificationTime;
        private final int keyPairCacheSize;
        private final int signerCacheSize;
        private final int signatureSize;
        private final int publicKeySize;
        
        public SphincsPlusMetrics(long keyGenerationCount, long signingCount, long verificationCount,
                                 long avgKeyGenTime, long avgSigningTime, long avgVerificationTime,
                                 int keyPairCacheSize, int signerCacheSize,
                                 int signatureSize, int publicKeySize) {
            this.keyGenerationCount = keyGenerationCount;
            this.signingCount = signingCount;
            this.verificationCount = verificationCount;
            this.avgKeyGenTime = avgKeyGenTime;
            this.avgSigningTime = avgSigningTime;
            this.avgVerificationTime = avgVerificationTime;
            this.keyPairCacheSize = keyPairCacheSize;
            this.signerCacheSize = signerCacheSize;
            this.signatureSize = signatureSize;
            this.publicKeySize = publicKeySize;
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
        public int getSignatureSize() { return signatureSize; }
        public int getPublicKeySize() { return publicKeySize; }
        
        @Override
        public String toString() {
            return "SphincsPlusMetrics{" +
                   "keyGen=" + keyGenerationCount + " (" + avgKeyGenTime + "ms avg), " +
                   "signing=" + signingCount + " (" + avgSigningTime + "ms avg), " +
                   "verify=" + verificationCount + " (" + avgVerificationTime + "ms avg), " +
                   "keyCache=" + keyPairCacheSize + ", signerCache=" + signerCacheSize + ", " +
                   "sigSize=" + signatureSize + "B, pubKeySize=" + publicKeySize + "B}";
        }
    }
}