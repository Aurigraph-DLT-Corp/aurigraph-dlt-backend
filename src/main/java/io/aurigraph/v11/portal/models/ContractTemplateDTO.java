package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class ContractTemplateDTO {
    @JsonProperty("template_id")
    private String templateId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("language")
    private String language;
    @JsonProperty("version")
    private String version;
    @JsonProperty("status")
    private String status;
    @JsonProperty("deploys_count")
    private Integer deploysCount;
    @JsonProperty("gas_estimate")
    private Integer gasEstimate;
    @JsonProperty("parameters")
    private java.util.List<String> parameters;
    @JsonProperty("features")
    private java.util.List<String> features;
    @JsonProperty("audit_status")
    private String auditStatus;
    @JsonProperty("verified_by")
    private String verifiedBy;

    public ContractTemplateDTO() {}

    private ContractTemplateDTO(Builder builder) {
        this.templateId = builder.templateId;
        this.name = builder.name;
        this.description = builder.description;
        this.language = builder.language;
        this.version = builder.version;
        this.status = builder.status;
        this.deploysCount = builder.deploysCount;
        this.gasEstimate = builder.gasEstimate;
        this.parameters = builder.parameters;
        this.features = builder.features;
        this.auditStatus = builder.auditStatus;
        this.verifiedBy = builder.verifiedBy;
    }

    public String getTemplateId() { return templateId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLanguage() { return language; }
    public String getVersion() { return version; }
    public String getStatus() { return status; }
    public Integer getDeploysCount() { return deploysCount; }
    public Integer getGasEstimate() { return gasEstimate; }
    public java.util.List<String> getParameters() { return parameters; }
    public java.util.List<String> getFeatures() { return features; }
    public String getAuditStatus() { return auditStatus; }
    public String getVerifiedBy() { return verifiedBy; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String templateId;
        private String name;
        private String description;
        private String language;
        private String version;
        private String status;
        private Integer deploysCount;
        private Integer gasEstimate;
        private java.util.List<String> parameters;
        private java.util.List<String> features;
        private String auditStatus;
        private String verifiedBy;

        public Builder templateId(String templateId) { this.templateId = templateId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder deploysCount(Integer deploysCount) { this.deploysCount = deploysCount; return this; }
        public Builder gasEstimate(Integer gasEstimate) { this.gasEstimate = gasEstimate; return this; }
        public Builder parameters(java.util.List<String> parameters) { this.parameters = parameters; return this; }
        public Builder features(java.util.List<String> features) { this.features = features; return this; }
        public Builder auditStatus(String auditStatus) { this.auditStatus = auditStatus; return this; }
        public Builder verifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; return this; }

        public ContractTemplateDTO build() { return new ContractTemplateDTO(this); }
    }
}
