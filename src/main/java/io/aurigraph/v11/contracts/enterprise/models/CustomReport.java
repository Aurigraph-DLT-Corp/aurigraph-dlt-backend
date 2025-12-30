package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Custom report model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomReport {
    private String reportId;
    private String organizationId;
    private String reportName;
    private String reportType;
    private Instant createdAt;
    private Instant lastModified;
    private String createdBy;
    private String status; // DRAFT, PUBLISHED, ARCHIVED
    private String template;
    private Map<String, Object> configuration;
    private List<String> sections;
    private List<String> charts;
    private List<String> tables;
    private String description;
    private List<String> tags;
    private Map<String, Object> metadata;
}