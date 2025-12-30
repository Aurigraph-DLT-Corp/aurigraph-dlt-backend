package io.aurigraph.v11.testing.builders;

import io.aurigraph.v11.token.secondary.SecondaryTokenVersion;
import io.aurigraph.v11.token.secondary.SecondaryTokenVersionStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Fluent builder for SecondaryTokenVersion test objects.
 *
 * Provides a readable way to construct test token version objects for version management testing.
 *
 * Usage Example:
 * <pre>
 * SecondaryTokenVersion version = new SecondaryTokenVersionTestBuilder()
 *     .withId(versionId)
 *     .withTokenId(tokenId)
 *     .withStatus(SecondaryTokenVersionStatus.PENDING_VVB)
 *     .withVersionNumber(2)
 *     .build();
 * </pre>
 *
 * Convenience Methods:
 * <pre>
 * // Pending approval
 * new SecondaryTokenVersionTestBuilder().pendingVVB().build()
 *
 * // Active version
 * new SecondaryTokenVersionTestBuilder().active().build()
 *
 * // Retired version
 * new SecondaryTokenVersionTestBuilder().retired().build()
 * </pre>
 */
public class SecondaryTokenVersionTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID tokenId = UUID.randomUUID();
    private SecondaryTokenVersionStatus status = SecondaryTokenVersionStatus.PENDING_VVB;
    private int versionNumber = 1;
    private String metadata = "{}";
    private String merkleHash = "";
    private List<String> propertyUpdates = List.of();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private Instant expiresAt = Instant.now().plusSeconds(86400); // 24 hours
    private int retirementVotesRequired = 2;
    private boolean requiresVoting = true;

    /**
     * Set the version ID.
     */
    public SecondaryTokenVersionTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Set the token ID this version belongs to.
     */
    public SecondaryTokenVersionTestBuilder withTokenId(UUID tokenId) {
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Set the version status.
     */
    public SecondaryTokenVersionTestBuilder withStatus(SecondaryTokenVersionStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Set the version number (1, 2, 3, etc.).
     */
    public SecondaryTokenVersionTestBuilder withVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
        return this;
    }

    /**
     * Set the metadata JSON string.
     */
    public SecondaryTokenVersionTestBuilder withMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Set the Merkle hash for version verification.
     */
    public SecondaryTokenVersionTestBuilder withMerkleHash(String merkleHash) {
        this.merkleHash = merkleHash;
        return this;
    }

    /**
     * Set the list of property updates in this version.
     */
    public SecondaryTokenVersionTestBuilder withPropertyUpdates(String... updates) {
        this.propertyUpdates = List.of(updates);
        return this;
    }

    /**
     * Set the list of property updates (list variant).
     */
    public SecondaryTokenVersionTestBuilder withPropertyUpdates(List<String> updates) {
        this.propertyUpdates = updates;
        return this;
    }

    /**
     * Set the creation timestamp.
     */
    public SecondaryTokenVersionTestBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * Set the expiration time.
     */
    public SecondaryTokenVersionTestBuilder withExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    /**
     * Set the number of retirement votes required.
     */
    public SecondaryTokenVersionTestBuilder withRetirementVotesRequired(int votes) {
        this.retirementVotesRequired = votes;
        return this;
    }

    /**
     * Set whether this version requires VVB voting.
     */
    public SecondaryTokenVersionTestBuilder requiresVoting(boolean requires) {
        this.requiresVoting = requires;
        return this;
    }

    /**
     * Convenience: Set status to PENDING_VVB.
     */
    public SecondaryTokenVersionTestBuilder pendingVVB() {
        this.status = SecondaryTokenVersionStatus.PENDING_VVB;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Convenience: Set status to ACTIVE.
     */
    public SecondaryTokenVersionTestBuilder active() {
        this.status = SecondaryTokenVersionStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Convenience: Set status to REJECTED.
     */
    public SecondaryTokenVersionTestBuilder rejected() {
        this.status = SecondaryTokenVersionStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Convenience: Set status to RETIRED.
     */
    public SecondaryTokenVersionTestBuilder retired() {
        this.status = SecondaryTokenVersionStatus.RETIRED;
        this.expiresAt = Instant.now().minusSeconds(1);
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Convenience: Set status to SUPERSEDED.
     */
    public SecondaryTokenVersionTestBuilder superseded() {
        this.status = SecondaryTokenVersionStatus.SUPERSEDED;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Build the SecondaryTokenVersion object.
     */
    public SecondaryTokenVersion build() {
        SecondaryTokenVersion version = new SecondaryTokenVersion();
        version.id = id;
        version.tokenId = tokenId;
        version.status = status;
        version.versionNumber = versionNumber;
        version.metadata = metadata;
        version.merkleHash = merkleHash;
        version.propertyUpdates = propertyUpdates;
        version.createdAt = createdAt;
        version.updatedAt = updatedAt;
        version.expiresAt = expiresAt;
        version.retirementVotesRequired = retirementVotesRequired;
        version.requiresVVBApproval = requiresVoting;
        return version;
    }

}
