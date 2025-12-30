package io.aurigraph.v11.tokenization.fractionalization;

import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;

/**
 * Breaking Change Detector
 * Detects and prevents breaking changes in fractionalized assets
 *
 * Protection Thresholds:
 * - >50%: Breaking change (PROHIBITED)
 * - 10-50%: Restricted change (requires verification)
 * - <10%: Allowed (automatic)
 *
 * @author Backend Development Agent (BDA)
 * @since Phase 1 Foundation
 */
@ApplicationScoped
public class BreakingChangeDetector {

    // Threshold constants
    private static final BigDecimal BREAKING_CHANGE_THRESHOLD = BigDecimal.valueOf(0.50);      // 50%
    private static final BigDecimal RESTRICTED_CHANGE_THRESHOLD = BigDecimal.valueOf(0.10);    // 10%

    /**
     * Detect change severity
     */
    public ChangeSeverity detectSeverity(BigDecimal originalValue, BigDecimal newValue) {
        if (originalValue == null || originalValue.signum() == 0) {
            return ChangeSeverity.UNKNOWN;
        }

        BigDecimal changePercent = newValue
            .subtract(originalValue)
            .divide(originalValue, 4, java.math.RoundingMode.HALF_UP);

        if (changePercent.abs().compareTo(BREAKING_CHANGE_THRESHOLD) > 0) {
            return ChangeSeverity.BREAKING_CHANGE;
        } else if (changePercent.abs().compareTo(RESTRICTED_CHANGE_THRESHOLD) > 0) {
            return ChangeSeverity.RESTRICTED_CHANGE;
        } else {
            return ChangeSeverity.ALLOWED;
        }
    }

    /**
     * Check if change is breaking
     */
    public boolean isBreakingChange(BigDecimal originalValue, BigDecimal newValue) {
        return detectSeverity(originalValue, newValue) == ChangeSeverity.BREAKING_CHANGE;
    }

    /**
     * Check if change requires verification
     */
    public boolean requiresVerification(BigDecimal originalValue, BigDecimal newValue) {
        ChangeSeverity severity = detectSeverity(originalValue, newValue);
        return severity == ChangeSeverity.BREAKING_CHANGE || severity == ChangeSeverity.RESTRICTED_CHANGE;
    }

    /**
     * Calculate percent change
     */
    public BigDecimal calculatePercentChange(BigDecimal originalValue, BigDecimal newValue) {
        if (originalValue == null || originalValue.signum() == 0) {
            return BigDecimal.ZERO;
        }

        return newValue
            .subtract(originalValue)
            .divide(originalValue, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Change Severity Enum
     */
    public enum ChangeSeverity {
        ALLOWED,              // <10% change
        RESTRICTED_CHANGE,    // 10-50% change (requires verification)
        BREAKING_CHANGE,      // >50% change (prohibited)
        UNKNOWN               // Unknown state
    }
}
