package io.aurigraph.v11.crypto;

import org.bouncycastle.pqc.crypto.crystals.dilithium.*;
import org.bouncycastle.pqc.crypto.crystals.kyber.*;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production Quantum Cryptography Provider
 * Sprint 14 - Workstream 1: Quantum Crypto Production Implementation
 *
 * Implements NIST-approved post-quantum cryptographic algorithms:
 * - CRYSTALS-Dilithium (Digital Signatures) - NIST FIPS 204
 * - CRYSTALS-Kyber (Key Encapsulation) - NIST FIPS 203
 *
 * Security Levels:
 * - Level 2 (128-bit quantum security, 256-bit classical)
 * - Level 3 (192-bit quantum security, 384-bit classical)
 * - Level 5 (256-bit quantum security, 512-bit classical)
 */
@ApplicationScoped
public class QuantumCryptoProvider {

    private static final Logger LOG = Logger.getLogger(QuantumCryptoProvider.class);

    private final SecureRandom secureRandom;
    private final Map<String, AsymmetricCipherKeyPair> keyCache;

    // Dilithium parameters for different security levels
    private final DilithiumParameters dilithium2Params = DilithiumParameters.dilithium2;
    private final DilithiumParameters dilithium3Params = DilithiumParameters.dilithium3;
    private final DilithiumParameters dilithium5Params = DilithiumParameters.dilithium5;

    // Kyber parameters for different security levels
    private final KyberParameters kyber512Params = KyberParameters.kyber512;
    private final KyberParameters kyber768Params = KyberParameters.kyber768;
    private final KyberParameters kyber1024Params = KyberParameters.kyber1024;

    public QuantumCryptoProvider() {
        this.secureRandom = new SecureRandom();
        this.keyCache = new ConcurrentHashMap<>();
        LOG.info("Quantum Cryptography Provider initialized with BouncyCastle PQC");
    }

    /**
     * Generate Dilithium key pair for digital signatures
     * Sprint 14 - NIST FIPS 204 compliant
     */
    public DilithiumKeyPair generateDilithiumKeyPair(SecurityLevel level) {
        LOG.infof("Generating Dilithium key pair for security level: %s", level);

        DilithiumParameters params = switch (level) {
            case LEVEL_2 -> dilithium2Params;
            case LEVEL_3 -> dilithium3Params;
            case LEVEL_5 -> dilithium5Params;
        };

        DilithiumKeyPairGenerator keyGen = new DilithiumKeyPairGenerator();
        keyGen.init(new DilithiumKeyGenerationParameters(secureRandom, params));

        AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();

        DilithiumPublicKeyParameters publicKey = (DilithiumPublicKeyParameters) keyPair.getPublic();
        DilithiumPrivateKeyParameters privateKey = (DilithiumPrivateKeyParameters) keyPair.getPrivate();

        LOG.infof("Dilithium key pair generated: public key size=%d bytes, private key size=%d bytes",
            publicKey.getEncoded().length, privateKey.getEncoded().length);

        return new DilithiumKeyPair(publicKey, privateKey, level);
    }

    /**
     * Sign data with Dilithium private key
     * Sprint 14 - Post-quantum digital signature
     */
    public byte[] signDilithium(byte[] data, DilithiumPrivateKeyParameters privateKey) {
        try {
            DilithiumSigner signer = new DilithiumSigner();
            signer.init(true, privateKey);

            byte[] signature = signer.generateSignature(data);

            LOG.debugf("Dilithium signature generated: size=%d bytes", signature.length);
            return signature;

        } catch (Exception e) {
            LOG.error("Dilithium signature generation failed", e);
            throw new RuntimeException("Failed to generate Dilithium signature", e);
        }
    }

    /**
     * Verify Dilithium signature
     * Sprint 14 - Post-quantum signature verification
     */
    public boolean verifyDilithium(byte[] data, byte[] signature, DilithiumPublicKeyParameters publicKey) {
        try {
            DilithiumSigner verifier = new DilithiumSigner();
            verifier.init(false, publicKey);

            boolean valid = verifier.verifySignature(data, signature);

            LOG.debugf("Dilithium signature verification: %s", valid ? "VALID" : "INVALID");
            return valid;

        } catch (Exception e) {
            LOG.error("Dilithium signature verification failed", e);
            return false;
        }
    }

    /**
     * Generate Kyber key pair for key encapsulation
     * Sprint 14 - NIST FIPS 203 compliant
     */
    public KyberKeyPair generateKyberKeyPair(SecurityLevel level) {
        LOG.infof("Generating Kyber key pair for security level: %s", level);

        KyberParameters params = switch (level) {
            case LEVEL_2 -> kyber512Params;
            case LEVEL_3 -> kyber768Params;
            case LEVEL_5 -> kyber1024Params;
        };

        KyberKeyPairGenerator keyGen = new KyberKeyPairGenerator();
        keyGen.init(new KyberKeyGenerationParameters(secureRandom, params));

        AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();

        KyberPublicKeyParameters publicKey = (KyberPublicKeyParameters) keyPair.getPublic();
        KyberPrivateKeyParameters privateKey = (KyberPrivateKeyParameters) keyPair.getPrivate();

        LOG.infof("Kyber key pair generated: public key size=%d bytes, private key size=%d bytes",
            publicKey.getEncoded().length, privateKey.getEncoded().length);

        return new KyberKeyPair(publicKey, privateKey, level);
    }

    /**
     * Encapsulate shared secret with Kyber public key
     * Sprint 14 - Post-quantum key exchange (sender side)
     */
    public KyberEncapsulation encapsulateKyber(KyberPublicKeyParameters publicKey) {
        try {
            KyberKEMGenerator kemGen = new KyberKEMGenerator(secureRandom);
            SecretWithEncapsulation secretWithEnc = kemGen.generateEncapsulated(publicKey);

            byte[] sharedSecret = secretWithEnc.getSecret();
            byte[] ciphertext = secretWithEnc.getEncapsulation();

            LOG.debugf("Kyber encapsulation: shared secret=%d bytes, ciphertext=%d bytes",
                sharedSecret.length, ciphertext.length);

            return new KyberEncapsulation(sharedSecret, ciphertext);

        } catch (Exception e) {
            LOG.error("Kyber encapsulation failed", e);
            throw new RuntimeException("Failed to encapsulate with Kyber", e);
        }
    }

    /**
     * Decapsulate shared secret with Kyber private key
     * Sprint 14 - Post-quantum key exchange (receiver side)
     */
    public byte[] decapsulateKyber(byte[] ciphertext, KyberPrivateKeyParameters privateKey) {
        try {
            KyberKEMExtractor kemExtractor = new KyberKEMExtractor(privateKey);
            byte[] sharedSecret = kemExtractor.extractSecret(ciphertext);

            LOG.debugf("Kyber decapsulation: shared secret=%d bytes recovered", sharedSecret.length);
            return sharedSecret;

        } catch (Exception e) {
            LOG.error("Kyber decapsulation failed", e);
            throw new RuntimeException("Failed to decapsulate with Kyber", e);
        }
    }

    /**
     * Cache key pair for reuse
     */
    public void cacheKeyPair(String keyId, AsymmetricCipherKeyPair keyPair) {
        keyCache.put(keyId, keyPair);
        LOG.debugf("Key pair cached with ID: %s", keyId);
    }

    /**
     * Retrieve cached key pair
     */
    public AsymmetricCipherKeyPair getCachedKeyPair(String keyId) {
        return keyCache.get(keyId);
    }

    /**
     * Clear key cache (security measure)
     */
    public void clearKeyCache() {
        keyCache.clear();
        LOG.info("Key cache cleared");
    }

    // Data transfer objects

    public record DilithiumKeyPair(
        DilithiumPublicKeyParameters publicKey,
        DilithiumPrivateKeyParameters privateKey,
        SecurityLevel securityLevel
    ) {}

    public record KyberKeyPair(
        KyberPublicKeyParameters publicKey,
        KyberPrivateKeyParameters privateKey,
        SecurityLevel securityLevel
    ) {}

    public record KyberEncapsulation(
        byte[] sharedSecret,
        byte[] ciphertext
    ) {}

    public enum SecurityLevel {
        LEVEL_2,  // 128-bit quantum security (NIST Level 2)
        LEVEL_3,  // 192-bit quantum security (NIST Level 3)
        LEVEL_5   // 256-bit quantum security (NIST Level 5)
    }
}
