package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Export request model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportRequest {
    private String exportType; // PDF, EXCEL, CSV, JSON
    private String reportId;
    private Instant periodStart;
    private Instant periodEnd;
    private List<String> includeCharts;
    private List<String> includeTables;
    private boolean includeRawData;
    private String compressionLevel;
    private Map<String, Object> formatOptions;
    private String deliveryMethod; // EMAIL, DOWNLOAD, FTP
    private String deliveryAddress;
    private String password; // For encrypted exports
    private boolean watermark;
}