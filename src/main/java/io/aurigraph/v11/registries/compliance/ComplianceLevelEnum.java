package io.aurigraph.v11.registries.compliance;

/**
 * Compliance Level Enumeration
 *
 * Defines multi-tier compliance standards supporting various regulatory frameworks
 * and security certifications. Each level represents increasing stringency and
 * requirements.
 *
 * Levels:
 * - LEVEL_1: Basic compliance (Standard regulatory requirements)
 * - LEVEL_2: Enhanced compliance (KYC/AML, financial regulations)
 * - LEVEL_3: Advanced compliance (Industry-specific certifications - ISO, SOC2)
 * - LEVEL_4: Maximum compliance (Institutional & institutional-grade standards)
 * - LEVEL_5: Quantum-Safe NIST (Highest security - NIST Level 5, quantum-resistant)
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
public enum ComplianceLevelEnum {
    /**
     * LEVEL_1: Basic Compliance
     * - Minimal KYC requirements
     * - Standard AML checks
     * - Basic risk assessment
     * Compliance Score: 25%
     */
    LEVEL_1(1, "Basic", 25, 100),

    /**
     * LEVEL_2: Enhanced Compliance
     * - Enhanced KYC/AML
     * - Financial regulations (MiFID II, Dodd-Frank)
     * - Transaction monitoring
     * Compliance Score: 50%
     */
    LEVEL_2(2, "Enhanced", 50, 200),

    /**
     * LEVEL_3: Advanced Compliance
     * - ISO 27001/27002 (Information Security)
     * - SOC 2 Type II (Service organization audits)
     * - Industry certifications
     * Compliance Score: 75%
     */
    LEVEL_3(3, "Advanced", 75, 300),

    /**
     * LEVEL_4: Maximum Compliance
     * - Institutional-grade security
     * - Multi-jurisdiction compliance (GDPR, CCPA, etc.)
     * - Enhanced audit trails
     * Compliance Score: 90%
     */
    LEVEL_4(4, "Maximum", 90, 400),

    /**
     * LEVEL_5: Quantum-Safe NIST
     * - NIST Level 5 quantum-resistant cryptography
     * - CRYSTALS-Dilithium digital signatures
     * - CRYSTALS-Kyber key encapsulation
     * - TLS 1.3 with post-quantum algorithms
     * Compliance Score: 100%
     */
    LEVEL_5(5, "Quantum-Safe NIST", 100, 500);

    private final int level;
    private final String description;
    private final int complianceScore;  // 0-100
    private final int points;            // For scoring systems

    ComplianceLevelEnum(int level, String description, int complianceScore, int points) {
        this.level = level;
        this.description = description;
        this.complianceScore = complianceScore;
        this.points = points;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public int getComplianceScore() {
        return complianceScore;
    }

    public int getPoints() {
        return points;
    }

    /**
     * Check if this level meets or exceeds the required level
     */
    public boolean meetsOrExceeds(ComplianceLevelEnum required) {
        return this.level >= required.level;
    }

    /**
     * Get the next higher compliance level
     */
    public ComplianceLevelEnum getNextLevel() {
        if (this.level < LEVEL_5.level) {
            return ComplianceLevelEnum.values()[this.level];
        }
        return this;
    }

    /**
     * Get the previous lower compliance level
     */
    public ComplianceLevelEnum getPreviousLevel() {
        if (this.level > LEVEL_1.level) {
            return ComplianceLevelEnum.values()[this.level - 2];
        }
        return this;
    }

    /**
     * Get compliance level by numeric value
     */
    public static ComplianceLevelEnum fromLevel(int level) {
        for (ComplianceLevelEnum c : ComplianceLevelEnum.values()) {
            if (c.level == level) {
                return c;
            }
        }
        throw new IllegalArgumentException("Invalid compliance level: " + level);
    }

    /**
     * Calculate weighted compliance score
     * Higher levels receive exponential weighting
     */
    public double getWeightedScore() {
        return Math.pow(this.complianceScore / 100.0, 1.5) * 100;
    }

    @Override
    public String toString() {
        return String.format("ComplianceLevel{level=%d, name=%s, score=%d%%}",
                level, description, complianceScore);
    }
}
