package io.aurigraph.v11.token.vvb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * VVB Validation Request DTO
 * Request model for submitting tokens for VVB validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VVBValidationRequest {
    private String changeType;
    private String description;
    private Map<String, Object> metadata;
    private String submitterId;

    public VVBValidationRequest(String changeType) {
        this.changeType = changeType;
    }
}
