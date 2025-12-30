package io.aurigraph.v11.token.secondary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CDI Event: Version Archived
 *
 * Fired when a version is archived (moved to terminal state).
 * Allows subscribers to clean up related data or update statistics.
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionArchivedEvent {
    private UUID versionId;
    private UUID secondaryTokenId;
    private Integer versionNumber;
    private String previousStatus;
    private LocalDateTime archivedAt;
}
