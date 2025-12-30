package io.aurigraph.v11.tokenization.fractionalization;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Primary Token Service
 * Manages primary token creation and validation for fractionalized assets
 *
 * @author Backend Development Agent (BDA)
 * @since Phase 1 Foundation
 */
@ApplicationScoped
public class PrimaryTokenService {

    private final Map<String, PrimaryTokenMetadata> tokens = new ConcurrentHashMap<>();

    /**
     * Create primary token for fractionalized asset
     */
    public String createPrimaryToken(String assetId, String tokenType, BigDecimal assetValue) {
        String tokenId = "primary-token-" + assetId + "-" + System.currentTimeMillis();
        PrimaryTokenMetadata metadata = new PrimaryTokenMetadata(tokenId, assetId, tokenType, assetValue);
        tokens.put(tokenId, metadata);
        Log.info("Primary token created: " + tokenId);
        return tokenId;
    }

    /**
     * Verify primary token exists
     */
    public boolean tokenExists(String tokenId) {
        return tokens.containsKey(tokenId);
    }

    /**
     * Get primary token metadata
     */
    public PrimaryTokenMetadata getToken(String tokenId) {
        return tokens.get(tokenId);
    }

    /**
     * Primary Token Metadata
     */
    public static class PrimaryTokenMetadata {
        public final String tokenId;
        public final String assetId;
        public final String tokenType;
        public final BigDecimal assetValue;
        public final long createdAt;

        public PrimaryTokenMetadata(String tokenId, String assetId, String tokenType, BigDecimal assetValue) {
            this.tokenId = tokenId;
            this.assetId = assetId;
            this.tokenType = tokenType;
            this.assetValue = assetValue;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
