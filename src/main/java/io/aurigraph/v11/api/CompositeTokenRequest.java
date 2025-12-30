package io.aurigraph.v11.api;

import java.util.List;
import java.util.Map;

public class CompositeTokenRequest {
    private String tokenId;
    private String assetId;
    private String name;
    private String symbol;
    private List<String> components;
    private Map<String, Object> metadata;
    private double totalSupply;
    
    public String getTokenId() {
        return tokenId;
    }
    
    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }
    
    public String getAssetId() {
        return assetId != null ? assetId : tokenId;
    }
    
    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public List<String> getComponents() {
        return components;
    }
    
    public void setComponents(List<String> components) {
        this.components = components;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public double getTotalSupply() {
        return totalSupply;
    }
    
    public void setTotalSupply(double totalSupply) {
        this.totalSupply = totalSupply;
    }
}