package io.aurigraph.v11.contracts.defi.protocols;

import java.math.BigDecimal;

/**
 * Sprint 4 DeFi Protocol Interface
 * Base interface for all DeFi protocol integrations
 */
public interface DeFiProtocol {
    
    String getProtocolId();
    String getProtocolName();
    String getVersion();
    BigDecimal getTotalValueLocked();
    boolean isActive();
    
}