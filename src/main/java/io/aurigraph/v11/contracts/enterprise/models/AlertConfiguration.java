package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Alert configuration model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlertConfiguration {
    private String alertId;
    private String organizationId;
    private String alertName;
    private String alertType; // PRICE, VOLUME, RISK, PERFORMANCE, SYSTEM
    private String metric; // The metric to monitor
    private String condition; // ABOVE, BELOW, EQUALS, PERCENT_CHANGE
    private BigDecimal threshold;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private boolean enabled;
    private List<String> channels; // EMAIL, SMS, SLACK, WEBHOOK
    private List<String> recipients;
    private String frequency; // REAL_TIME, HOURLY, DAILY
    private Instant createdAt;
    private Instant lastTriggered;
    private Map<String, Object> metadata;
}