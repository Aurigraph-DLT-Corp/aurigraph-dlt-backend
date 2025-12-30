package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Export result model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportResult {
    private String exportId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private Instant requestedAt;
    private Instant completedAt;
    private String downloadUrl;
    private String fileName;
    private Long fileSizeBytes;
    private String checksumMd5;
    private String errorMessage;
    private Integer expiresInHours;
    private String deliveryStatus;
}