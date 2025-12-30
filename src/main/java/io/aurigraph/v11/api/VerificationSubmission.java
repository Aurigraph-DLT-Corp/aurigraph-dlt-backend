package io.aurigraph.v11.api;

import java.util.Map;

public class VerificationSubmission {
    private String tokenId;
    private String verifierId;
    private boolean verified;
    private Map<String, Object> details;
    
    public String getTokenId() {
        return tokenId;
    }
    
    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }
    
    public String getVerifierId() {
        return verifierId;
    }
    
    public void setVerifierId(String verifierId) {
        this.verifierId = verifierId;
    }
    
    public boolean isVerified() {
        return verified;
    }
    
    public void setVerified(boolean verified) {
        this.verified = verified;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}