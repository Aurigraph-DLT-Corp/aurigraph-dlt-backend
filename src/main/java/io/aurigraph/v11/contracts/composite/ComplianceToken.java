package io.aurigraph.v11.contracts.composite;

import java.time.Instant;
import java.util.*;

/**
 * Compliance Token (ERC-721) - Tracks regulatory compliance status
 */
public class ComplianceToken extends SecondaryToken {
    private ComplianceStatus complianceStatus;
    private Map<String, Object> complianceData;

    public ComplianceToken(String tokenId, String compositeId, ComplianceStatus complianceStatus,
                          Map<String, Object> complianceData) {
        super(tokenId, compositeId, SecondaryTokenType.COMPLIANCE);
        this.complianceStatus = complianceStatus;
        this.complianceData = new HashMap<>(complianceData);
    }

    @Override
    public void updateData(Map<String, Object> updateData) {
        if (updateData.containsKey("complianceStatus")) {
            this.complianceStatus = (ComplianceStatus) updateData.get("complianceStatus");
        }
        this.complianceData.putAll(updateData);
        this.lastUpdated = Instant.now();
        this.data.putAll(updateData);
    }

    public void updateComplianceStatus(ComplianceStatus newStatus, String reason) {
        this.complianceStatus = newStatus;
        this.complianceData.put("lastStatusChange", Instant.now());
        this.complianceData.put("statusChangeReason", reason);
        this.lastUpdated = Instant.now();
    }

    public boolean isCompliant() {
        return complianceStatus == ComplianceStatus.COMPLIANT;
    }

    // Getters
    public ComplianceStatus getComplianceStatus() { return complianceStatus; }
    public Map<String, Object> getComplianceData() { return Map.copyOf(complianceData); }
}