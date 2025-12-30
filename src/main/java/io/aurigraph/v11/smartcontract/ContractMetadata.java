package io.aurigraph.v11.smartcontract;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * Smart Contract Metadata
 *
 * Extended metadata for smart contracts including gas fees, permissions,
 * and execution statistics.
 *
 * @version 11.2.1
 * @since 2025-10-12
 */
public class ContractMetadata {

    @JsonProperty("description")
    private String description;

    @JsonProperty("author")
    private String author;

    @JsonProperty("license")
    private String license;

    @JsonProperty("tags")
    private String[] tags;

    @JsonProperty("gasLimit")
    private Long gasLimit;

    @JsonProperty("gasPrice")
    private Long gasPrice;

    @JsonProperty("executionCount")
    private Long executionCount;

    @JsonProperty("lastExecutedAt")
    private String lastExecutedAt;

    @JsonProperty("permissions")
    private ContractPermissions permissions;

    @JsonProperty("customFields")
    private Map<String, Object> customFields;

    // Constructors
    public ContractMetadata() {
        this.executionCount = 0L;
        this.customFields = new HashMap<>();
        this.permissions = new ContractPermissions();
    }

    // Getters and Setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public Long getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(Long gasLimit) {
        this.gasLimit = gasLimit;
    }

    public Long getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(Long gasPrice) {
        this.gasPrice = gasPrice;
    }

    public Long getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Long executionCount) {
        this.executionCount = executionCount;
    }

    public void incrementExecutionCount() {
        this.executionCount++;
    }

    public String getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(String lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public ContractPermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(ContractPermissions permissions) {
        this.permissions = permissions;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    public void addCustomField(String key, Object value) {
        this.customFields.put(key, value);
    }

    /**
     * Contract Permissions
     */
    public static class ContractPermissions {
        @JsonProperty("isPublic")
        private boolean isPublic = true;

        @JsonProperty("allowedCallers")
        private String[] allowedCallers;

        @JsonProperty("requiredRole")
        private String requiredRole;

        @JsonProperty("requiresSignature")
        private boolean requiresSignature = false;

        // Getters and Setters
        public boolean isPublic() {
            return isPublic;
        }

        public void setPublic(boolean isPublic) {
            this.isPublic = isPublic;
        }

        public String[] getAllowedCallers() {
            return allowedCallers;
        }

        public void setAllowedCallers(String[] allowedCallers) {
            this.allowedCallers = allowedCallers;
        }

        public String getRequiredRole() {
            return requiredRole;
        }

        public void setRequiredRole(String requiredRole) {
            this.requiredRole = requiredRole;
        }

        public boolean isRequiresSignature() {
            return requiresSignature;
        }

        public void setRequiresSignature(boolean requiresSignature) {
            this.requiresSignature = requiresSignature;
        }
    }
}
