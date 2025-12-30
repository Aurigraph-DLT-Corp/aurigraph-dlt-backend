package io.aurigraph.v11.contracts.composite;

public class CompositeTokenRequest {
    public final String tokenId;
    public final String composition;
    
    public CompositeTokenRequest(String tokenId, String composition) {
        this.tokenId = tokenId;
        this.composition = composition;
    }
}
