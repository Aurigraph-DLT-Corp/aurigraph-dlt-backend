package io.aurigraph.v11.contracts.defi.risk;

import io.aurigraph.v11.contracts.defi.models.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Sprint 4 Impermanent Loss Calculator
 * Advanced impermanent loss calculation with protection mechanisms
 */
@ApplicationScoped
public class ImpermanentLossCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(ImpermanentLossCalculator.class);
    
    /**
     * Calculate potential impermanent loss for a liquidity position
     */
    public BigDecimal calculatePotentialLoss(LiquidityPosition position) {
        if (position.getToken0EntryPrice() == null || position.getToken1EntryPrice() == null) {
            logger.warn("Cannot calculate IL - missing entry prices for position {}", position.getPositionId());
            return BigDecimal.ZERO;
        }
        
        // Get current prices (mock implementation)
        BigDecimal currentPrice0 = getCurrentPrice(position.getToken0Address());
        BigDecimal currentPrice1 = getCurrentPrice(position.getToken1Address());
        
        // Calculate price ratios
        BigDecimal entryRatio = position.getToken0EntryPrice().divide(position.getToken1EntryPrice(), 8, RoundingMode.HALF_UP);
        BigDecimal currentRatio = currentPrice0.divide(currentPrice1, 8, RoundingMode.HALF_UP);
        
        // Calculate price change ratio
        BigDecimal priceChangeRatio = currentRatio.divide(entryRatio, 8, RoundingMode.HALF_UP);
        
        // Calculate impermanent loss using the standard formula
        BigDecimal il = calculateIL(priceChangeRatio);
        
        // Apply pool-specific adjustments
        il = applyPoolAdjustments(il, position);
        
        logger.debug("Calculated IL {} for position {} (price change ratio: {})", 
                   il, position.getPositionId(), priceChangeRatio);
        
        return il;
    }
    
    /**
     * Calculate impermanent loss using the standard AMM formula
     * IL = 2 * sqrt(r) / (1 + r) - 1, where r is the price change ratio
     */
    private BigDecimal calculateIL(BigDecimal priceChangeRatio) {
        if (priceChangeRatio.equals(BigDecimal.ONE)) {
            return BigDecimal.ZERO; // No price change, no IL
        }
        
        // Calculate sqrt(r)
        BigDecimal sqrtR = sqrt(priceChangeRatio);
        
        // Calculate 2 * sqrt(r)
        BigDecimal numerator = sqrtR.multiply(BigDecimal.valueOf(2));
        
        // Calculate (1 + r)
        BigDecimal denominator = BigDecimal.ONE.add(priceChangeRatio);
        
        // Calculate the full formula: 2 * sqrt(r) / (1 + r) - 1
        BigDecimal result = numerator.divide(denominator, 8, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
        
        // IL is always negative (loss), so we return the absolute value
        return result.abs();
    }
    
    /**
     * Apply pool-specific adjustments to impermanent loss calculation
     */
    private BigDecimal applyPoolAdjustments(BigDecimal baseIL, LiquidityPosition position) {
        BigDecimal adjustedIL = baseIL;
        
        // Stable pair adjustment (lower IL risk)
        if (isStablePair(position)) {
            adjustedIL = adjustedIL.multiply(BigDecimal.valueOf(0.1)); // 10% of calculated IL
        }
        
        // Correlated asset adjustment (ETH/altcoins)
        else if (isCorrelatedPair(position)) {
            adjustedIL = adjustedIL.multiply(BigDecimal.valueOf(0.6)); // 60% of calculated IL
        }
        
        // High volatility pairs
        else if (isHighVolatilityPair(position)) {
            adjustedIL = adjustedIL.multiply(BigDecimal.valueOf(1.3)); // 130% of calculated IL
        }
        
        // Fee compensation (higher fees offset some IL)
        BigDecimal feeOffset = calculateFeeOffset(position);
        adjustedIL = adjustedIL.subtract(feeOffset).max(BigDecimal.ZERO);
        
        return adjustedIL;
    }
    
    /**
     * Calculate fee offset for impermanent loss
     */
    private BigDecimal calculateFeeOffset(LiquidityPosition position) {
        if (position.getFeesEarned0() == null || position.getFeesEarned1() == null) {
            return BigDecimal.ZERO;
        }
        
        // Get current prices
        BigDecimal price0 = getCurrentPrice(position.getToken0Address());
        BigDecimal price1 = getCurrentPrice(position.getToken1Address());
        
        // Calculate total fee value in USD
        BigDecimal feeValue0 = position.getFeesEarned0().multiply(price0);
        BigDecimal feeValue1 = position.getFeesEarned1().multiply(price1);
        BigDecimal totalFeeValue = feeValue0.add(feeValue1);
        
        // Calculate fee offset as percentage of position value
        BigDecimal positionValue = position.getCurrentValue();
        if (positionValue != null && positionValue.compareTo(BigDecimal.ZERO) > 0) {
            return totalFeeValue.divide(positionValue, 8, RoundingMode.HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate impermanent loss protection premium
     */
    public BigDecimal calculateILProtectionPremium(LiquidityPosition position, BigDecimal coverageAmount) {
        BigDecimal baseIL = calculatePotentialLoss(position);
        
        // Premium calculation based on:
        // 1. Historical volatility
        // 2. Time to maturity
        // 3. Coverage amount
        
        BigDecimal volatilityFactor = getVolatilityFactor(position);
        BigDecimal timeFactor = getTimeFactor(position);
        BigDecimal coverageFactor = coverageAmount.divide(position.getCurrentValue(), 8, RoundingMode.HALF_UP);
        
        BigDecimal premium = baseIL
            .multiply(volatilityFactor)
            .multiply(timeFactor)
            .multiply(coverageFactor)
            .multiply(BigDecimal.valueOf(0.1)); // 10% of calculated risk
        
        logger.debug("IL protection premium {} for position {} (coverage: {})", 
                   premium, position.getPositionId(), coverageAmount);
        
        return premium;
    }
    
    /**
     * Estimate maximum potential loss for position
     */
    public BigDecimal estimateMaxLoss(LiquidityPosition position, BigDecimal confidenceLevel) {
        // Simulate various price scenarios
        BigDecimal maxLoss = BigDecimal.ZERO;
        
        // Test various price change scenarios
        BigDecimal[] priceMultipliers = {
            BigDecimal.valueOf(0.1),  // 90% drop
            BigDecimal.valueOf(0.5),  // 50% drop
            BigDecimal.valueOf(2.0),  // 100% gain
            BigDecimal.valueOf(5.0),  // 400% gain
            BigDecimal.valueOf(10.0)  // 900% gain
        };
        
        for (BigDecimal multiplier : priceMultipliers) {
            BigDecimal scenarioLoss = calculateIL(multiplier);
            maxLoss = maxLoss.max(scenarioLoss);
        }
        
        // Apply confidence level adjustment
        BigDecimal confidenceAdjustment = confidenceLevel.subtract(BigDecimal.valueOf(0.5)).multiply(BigDecimal.valueOf(0.2));
        maxLoss = maxLoss.multiply(BigDecimal.ONE.add(confidenceAdjustment));
        
        return maxLoss;
    }
    
    // Private helper methods
    private BigDecimal getCurrentPrice(String token) {
        // Mock price feed - in production would use real oracles
        Map<String, BigDecimal> prices = Map.of(
            "ETH", BigDecimal.valueOf(2100), // Slightly different from entry
            "BTC", BigDecimal.valueOf(31000),
            "USDC", BigDecimal.ONE,
            "USDT", BigDecimal.valueOf(0.999),
            "DAI", BigDecimal.valueOf(1.001),
            "AURI", BigDecimal.valueOf(5.2)
        );
        return prices.getOrDefault(token, BigDecimal.ONE);
    }
    
    private boolean isStablePair(LiquidityPosition position) {
        String poolId = position.getPoolId().toUpperCase();
        return poolId.contains("USDC_USDT") || 
               poolId.contains("DAI_USDC") || 
               poolId.contains("STABLE");
    }
    
    private boolean isCorrelatedPair(LiquidityPosition position) {
        String poolId = position.getPoolId().toUpperCase();
        return poolId.contains("ETH") && !poolId.contains("USDC") && !poolId.contains("USDT");
    }
    
    private boolean isHighVolatilityPair(LiquidityPosition position) {
        String poolId = position.getPoolId().toUpperCase();
        return poolId.contains("AURI") || poolId.contains("MEME") || poolId.contains("NEW");
    }
    
    private BigDecimal getVolatilityFactor(LiquidityPosition position) {
        if (isStablePair(position)) {
            return BigDecimal.valueOf(0.2); // Low volatility
        } else if (isCorrelatedPair(position)) {
            return BigDecimal.valueOf(0.8); // Medium volatility
        } else {
            return BigDecimal.valueOf(1.5); // High volatility
        }
    }
    
    private BigDecimal getTimeFactor(LiquidityPosition position) {
        // Time decay factor - longer positions have lower time premium
        long positionAge = System.currentTimeMillis() - position.getCreatedAt().toEpochMilli();
        long days = positionAge / (1000 * 60 * 60 * 24);
        
        if (days < 7) {
            return BigDecimal.valueOf(1.2); // High time premium
        } else if (days < 30) {
            return BigDecimal.valueOf(1.0); // Normal time premium
        } else {
            return BigDecimal.valueOf(0.8); // Lower time premium
        }
    }
    
    private BigDecimal sqrt(BigDecimal value) {
        if (value.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        // Newton's method for square root
        BigDecimal x = value;
        BigDecimal two = BigDecimal.valueOf(2);
        
        for (int i = 0; i < 20; i++) { // 20 iterations for precision
            BigDecimal newX = x.add(value.divide(x, 16, RoundingMode.HALF_UP)).divide(two, 16, RoundingMode.HALF_UP);
            if (newX.subtract(x).abs().compareTo(BigDecimal.valueOf(0.0000001)) < 0) {
                break;
            }
            x = newX;
        }
        
        return x;
    }
}