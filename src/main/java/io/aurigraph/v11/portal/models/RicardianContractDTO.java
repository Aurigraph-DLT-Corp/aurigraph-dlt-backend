package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class RicardianContractDTO {
    @JsonProperty("contract_id")
    private String contractId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("type")
    private String type;
    @JsonProperty("status")
    private String status;
    @JsonProperty("version")
    private String version;
    @JsonProperty("contract_hash")
    private String contractHash;
    @JsonProperty("legal_jurisdiction")
    private String legalJurisdiction;
    @JsonProperty("effective_date")
    private Instant effectiveDate;
    @JsonProperty("expiry_date")
    private Instant expiryDate;
    @JsonProperty("signatories")
    private java.util.List<String> signatories;
    @JsonProperty("total_executions")
    private Integer totalExecutions;
    @JsonProperty("verification_status")
    private String verificationStatus;
    @JsonProperty("verified_by")
    private String verifiedBy;
    @JsonProperty("last_modified")
    private Instant lastModified;
    @JsonProperty("error")
    private String error;

    public RicardianContractDTO() {}

    private RicardianContractDTO(Builder builder) {
        this.contractId = builder.contractId;
        this.name = builder.name;
        this.description = builder.description;
        this.type = builder.type;
        this.status = builder.status;
        this.version = builder.version;
        this.contractHash = builder.contractHash;
        this.legalJurisdiction = builder.legalJurisdiction;
        this.effectiveDate = builder.effectiveDate;
        this.expiryDate = builder.expiryDate;
        this.signatories = builder.signatories;
        this.totalExecutions = builder.totalExecutions;
        this.verificationStatus = builder.verificationStatus;
        this.verifiedBy = builder.verifiedBy;
        this.lastModified = builder.lastModified;
        this.error = builder.error;
    }

    public String getContractId() { return contractId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getVersion() { return version; }
    public String getContractHash() { return contractHash; }
    public String getLegalJurisdiction() { return legalJurisdiction; }
    public Instant getEffectiveDate() { return effectiveDate; }
    public Instant getExpiryDate() { return expiryDate; }
    public java.util.List<String> getSignatories() { return signatories; }
    public Integer getTotalExecutions() { return totalExecutions; }
    public String getVerificationStatus() { return verificationStatus; }
    public String getVerifiedBy() { return verifiedBy; }
    public Instant getLastModified() { return lastModified; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String contractId;
        private String name;
        private String description;
        private String type;
        private String status;
        private String version;
        private String contractHash;
        private String legalJurisdiction;
        private Instant effectiveDate;
        private Instant expiryDate;
        private java.util.List<String> signatories;
        private Integer totalExecutions;
        private String verificationStatus;
        private String verifiedBy;
        private Instant lastModified;
        private String error;

        public Builder contractId(String contractId) { this.contractId = contractId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder contractHash(String contractHash) { this.contractHash = contractHash; return this; }
        public Builder legalJurisdiction(String legalJurisdiction) { this.legalJurisdiction = legalJurisdiction; return this; }
        public Builder effectiveDate(Instant effectiveDate) { this.effectiveDate = effectiveDate; return this; }
        public Builder expiryDate(Instant expiryDate) { this.expiryDate = expiryDate; return this; }
        public Builder signatories(java.util.List<String> signatories) { this.signatories = signatories; return this; }
        public Builder totalExecutions(Integer totalExecutions) { this.totalExecutions = totalExecutions; return this; }
        public Builder verificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; return this; }
        public Builder verifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; return this; }
        public Builder lastModified(Instant lastModified) { this.lastModified = lastModified; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public RicardianContractDTO build() { return new RicardianContractDTO(this); }
    }
}
