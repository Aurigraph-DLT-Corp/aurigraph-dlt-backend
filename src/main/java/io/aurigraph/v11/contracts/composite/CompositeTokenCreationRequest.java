package io.aurigraph.v11.contracts.composite;

import java.util.*;
import java.math.BigDecimal;

/**
 * Request model for creating composite tokens
 */
public class CompositeTokenCreationRequest {
    private String assetId;
    private String assetType;
    private String ownerAddress;
    private String legalTitle;
    private String jurisdiction;
    private String coordinates;
    private boolean fractionalizable;
    private VerificationLevel requiredVerificationLevel;
    private Map<String, Object> metadata;
    private BigDecimal assetValue;

    // Getters and setters
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    
    public String getOwnerAddress() { return ownerAddress; }
    public void setOwnerAddress(String ownerAddress) { this.ownerAddress = ownerAddress; }
    
    public String getLegalTitle() { return legalTitle; }
    public void setLegalTitle(String legalTitle) { this.legalTitle = legalTitle; }
    
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    
    public String getCoordinates() { return coordinates; }
    public void setCoordinates(String coordinates) { this.coordinates = coordinates; }
    
    public boolean isFractionalizable() { return fractionalizable; }
    public void setFractionalizable(boolean fractionalizable) { this.fractionalizable = fractionalizable; }
    
    public VerificationLevel getRequiredVerificationLevel() { return requiredVerificationLevel; }
    public void setRequiredVerificationLevel(VerificationLevel requiredVerificationLevel) { 
        this.requiredVerificationLevel = requiredVerificationLevel; 
    }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public BigDecimal getAssetValue() { return assetValue; }
    public void setAssetValue(BigDecimal assetValue) { this.assetValue = assetValue; }
}