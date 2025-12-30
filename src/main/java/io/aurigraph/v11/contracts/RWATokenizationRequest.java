package io.aurigraph.v11.contracts;

public class RWATokenizationRequest {
    public final String assetId;
    public final long value;
    
    public RWATokenizationRequest(String assetId, long value) {
        this.assetId = assetId;
        this.value = value;
    }
}
