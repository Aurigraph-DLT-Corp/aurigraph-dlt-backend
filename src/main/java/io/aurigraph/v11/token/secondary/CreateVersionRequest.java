package io.aurigraph.v11.token.secondary;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Create Version Request DTO
 *
 * Request body for creating a new token version.
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVersionRequest {

    @NotNull
    @JsonProperty("secondaryTokenId")
    private UUID secondaryTokenId;

    @NotBlank
    @JsonProperty("content")
    private String content;

    @JsonProperty("vvbRequired")
    private Boolean vvbRequired = false;

    @JsonProperty("previousVersionId")
    private UUID previousVersionId;
}
