package io.aurigraph.v11.crm.dto;

import io.aurigraph.v11.crm.entity.DemoRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for scheduling a demo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDemoRequest {
    private UUID leadId;
    private DemoRequest.DemoType demoType;
    private ZonedDateTime startTime;
    private Integer durationMinutes;
    private String preferredTimezone;
    private String notes;
}
