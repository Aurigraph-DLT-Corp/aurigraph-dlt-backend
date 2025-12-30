package io.aurigraph.v11.token.secondary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CDI Event: Version Rejected
 *
 * Fired when VVB rejects a version.
 * Allows subscribers to notify users and log rejection reason.
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionRejectedEvent {
    private UUID versionId;
    private UUID secondaryTokenId;
    private Integer versionNumber;
    private String rejectionReason;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
}
