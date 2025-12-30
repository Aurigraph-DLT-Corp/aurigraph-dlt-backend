package io.aurigraph.v11.contracts.models;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a party in a Ricardian contract
 */
public class ContractParty {

    private String partyId;
    private String name;
    private String address;
    private String role; // e.g., "BUYER", "SELLER", "VALIDATOR", "WITNESS"
    private String publicKey;
    private boolean kycVerified = false;
    private boolean signatureRequired = true;
    private String jurisdiction;
    private String email;
    private String phone;
    private Instant createdAt;
    private Map<String, String> metadata = new HashMap<>();

    // Default constructor
    public ContractParty() {
        this.createdAt = Instant.now();
        this.metadata = new HashMap<>();
    }

    // Constructor for quick creation
    public ContractParty(String partyId, String name, String address, String role) {
        this.partyId = partyId;
        this.name = name;
        this.address = address;
        this.role = role;
        this.createdAt = Instant.now();
        this.metadata = new HashMap<>();
    }

    // Getters and Setters
    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public boolean isKycVerified() {
        return kycVerified;
    }

    public void setKycVerified(boolean kycVerified) {
        this.kycVerified = kycVerified;
    }

    public boolean isSignatureRequired() {
        return signatureRequired;
    }

    public void setSignatureRequired(boolean signatureRequired) {
        this.signatureRequired = signatureRequired;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    /**
     * Builder for ContractParty
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ContractParty party = new ContractParty();

        public Builder partyId(String partyId) {
            party.partyId = partyId;
            return this;
        }

        public Builder name(String name) {
            party.name = name;
            return this;
        }

        public Builder address(String address) {
            party.address = address;
            return this;
        }

        public Builder role(String role) {
            party.role = role;
            return this;
        }

        public Builder publicKey(String publicKey) {
            party.publicKey = publicKey;
            return this;
        }

        public Builder kycVerified(boolean kycVerified) {
            party.kycVerified = kycVerified;
            return this;
        }

        public Builder signatureRequired(boolean signatureRequired) {
            party.signatureRequired = signatureRequired;
            return this;
        }

        public Builder jurisdiction(String jurisdiction) {
            party.jurisdiction = jurisdiction;
            return this;
        }

        public Builder email(String email) {
            party.email = email;
            return this;
        }

        public Builder phone(String phone) {
            party.phone = phone;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            party.createdAt = createdAt;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            party.metadata = metadata;
            return this;
        }

        public ContractParty build() {
            return party;
        }
    }
}
