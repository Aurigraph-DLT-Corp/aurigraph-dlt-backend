package io.aurigraph.v11.token.secondary;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reject Version Request DTO
 *
 * Request body for rejecting a version during VVB workflow.
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectVersionRequest {

    @NotBlank
    @JsonProperty("rejectionReason")
    private String rejectionReason;

    @JsonProperty("rejectedBy")
    private String rejectedBy;
}
