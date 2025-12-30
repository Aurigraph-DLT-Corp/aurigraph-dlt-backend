package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class AuditTrailDTO {
    @JsonProperty("id")
    private String id;
    @JsonProperty("timestamp")
    private Instant timestamp;
    @JsonProperty("event_type")
    private String eventType;
    @JsonProperty("event_category")
    private String eventCategory;
    @JsonProperty("actor")
    private String actor;
    @JsonProperty("action")
    private String action;
    @JsonProperty("resource")
    private String resource;
    @JsonProperty("resource_type")
    private String resourceType;
    @JsonProperty("status")
    private String status;
    @JsonProperty("details")
    private String details;
    @JsonProperty("ip_address")
    private String ipAddress;
    @JsonProperty("severity")
    private String severity;

    public AuditTrailDTO() {}

    private AuditTrailDTO(Builder builder) {
        this.id = builder.id;
        this.timestamp = builder.timestamp;
        this.eventType = builder.eventType;
        this.eventCategory = builder.eventCategory;
        this.actor = builder.actor;
        this.action = builder.action;
        this.resource = builder.resource;
        this.resourceType = builder.resourceType;
        this.status = builder.status;
        this.details = builder.details;
        this.ipAddress = builder.ipAddress;
        this.severity = builder.severity;
    }

    public String getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }
    public String getEventCategory() { return eventCategory; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getResource() { return resource; }
    public String getResourceType() { return resourceType; }
    public String getStatus() { return status; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public String getSeverity() { return severity; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private Instant timestamp;
        private String eventType;
        private String eventCategory;
        private String actor;
        private String action;
        private String resource;
        private String resourceType;
        private String status;
        private String details;
        private String ipAddress;
        private String severity;

        public Builder id(String id) { this.id = id; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder eventCategory(String eventCategory) { this.eventCategory = eventCategory; return this; }
        public Builder actor(String actor) { this.actor = actor; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder resource(String resource) { this.resource = resource; return this; }
        public Builder resourceType(String resourceType) { this.resourceType = resourceType; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }

        public AuditTrailDTO build() { return new AuditTrailDTO(this); }
    }
}
