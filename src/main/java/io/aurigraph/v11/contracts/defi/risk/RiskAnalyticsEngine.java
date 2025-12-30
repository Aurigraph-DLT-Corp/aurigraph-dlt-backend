package io.aurigraph.v11.contracts.defi.risk;

import io.aurigraph.v11.contracts.defi.models.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sprint 4 Risk Analytics Engine
 * Comprehensive risk assessment for DeFi positions and portfolios
 */
@ApplicationScoped
public class RiskAnalyticsEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskAnalyticsEngine.class);
    
    // Risk metrics cache
    private final Map<String, BigDecimal> riskCache = new ConcurrentHashMap<>();
    private volatile BigDecimal totalRiskExposure = BigDecimal.ZERO;
    private volatile BigDecimal liquidationThreshold = BigDecimal.valueOf(1.1); // 110%
    
    /**
     * Calculate risk score for a loan position
     */
    public BigDecimal calculatePositionRisk(LoanPosition position) {
        BigDecimal riskScore = BigDecimal.ZERO;
        
        // Health factor risk (primary component)
        BigDecimal healthFactor = position.getHealthFactor();
        if (healthFactor != null) {
            if (healthFactor.compareTo(BigDecimal.valueOf(2.0)) >= 0) {
                riskScore = riskScore.add(BigDecimal.valueOf(0.1)); // Low risk
            } else if (healthFactor.compareTo(BigDecimal.valueOf(1.5)) >= 0) {
                riskScore = riskScore.add(BigDecimal.valueOf(0.3)); // Medium risk
            } else if (healthFactor.compareTo(BigDecimal.valueOf(1.1)) >= 0) {
                riskScore = riskScore.add(BigDecimal.valueOf(0.6)); // High risk
            } else {
                riskScore = riskScore.add(BigDecimal.valueOf(0.9)); // Critical risk
            }
        }
        
        // LTV risk
        BigDecimal ltv = position.getLtv();
        if (ltv != null) {
            if (ltv.compareTo(BigDecimal.valueOf(0.8)) > 0) {
                riskScore = riskScore.add(BigDecimal.valueOf(0.2)); // High LTV risk
            } else if (ltv.compareTo(BigDecimal.valueOf(0.6)) > 0) {
                riskScore = riskScore.add(BigDecimal.valueOf(0.1)); // Medium LTV risk
            }
        }
        
        // Collateral concentration risk
        if (position.getCollateralTokens().size() == 1) {
            riskScore = riskScore.add(BigDecimal.valueOf(0.1)); // Single collateral risk
        }
        
        // Time-based risk (newer positions are riskier)
        long positionAge = Instant.now().getEpochSecond() - position.getCreatedAt().getEpochSecond();
        if (positionAge < 86400) { // Less than 1 day
            riskScore = riskScore.add(BigDecimal.valueOf(0.05)); // New position risk
        }
        
        // Cap risk score at 1.0 (100%)
        riskScore = riskScore.min(BigDecimal.ONE);
        
        // Cache the risk score
        riskCache.put(position.getPositionId(), riskScore);
        
        logger.debug("Calculated risk score {} for position {}", riskScore, position.getPositionId());
        return riskScore;
    }
    
    /**
     * Calculate portfolio-wide risk score
     */
    public BigDecimal calculatePortfolioRisk(DeFiPortfolio portfolio) {
        BigDecimal portfolioRisk = BigDecimal.ZERO;
        BigDecimal totalValue = portfolio.getTotalValue();
        
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // Risk from liquidity positions
        if (portfolio.getLiquidityPositions() != null) {
            for (LiquidityPosition position : portfolio.getLiquidityPositions()) {
                BigDecimal positionRisk = calculateLiquidityPositionRisk(position);
                BigDecimal positionWeight = position.getCurrentValue().divide(totalValue, 8, RoundingMode.HALF_UP);
                portfolioRisk = portfolioRisk.add(positionRisk.multiply(positionWeight));
            }
        }
        
        // Risk from yield farming positions
        if (portfolio.getYieldPositions() != null) {
            for (YieldFarmRewards rewards : portfolio.getYieldPositions()) {
                BigDecimal farmRisk = calculateYieldFarmRisk(rewards);
                BigDecimal positionWeight = rewards.getStakedAmount().divide(totalValue, 8, RoundingMode.HALF_UP);
                portfolioRisk = portfolioRisk.add(farmRisk.multiply(positionWeight));
            }
        }
        
        // Risk from loan positions
        if (portfolio.getLoanPositions() != null) {
            for (LoanPosition position : portfolio.getLoanPositions()) {
                BigDecimal loanRisk = calculatePositionRisk(position);
                BigDecimal positionWeight = position.getTotalCollateralValue().divide(totalValue, 8, RoundingMode.HALF_UP);
                portfolioRisk = portfolioRisk.add(loanRisk.multiply(positionWeight));
            }
        }
        
        // Diversification bonus (reduces risk)
        int positionCount = 0;
        if (portfolio.getLiquidityPositions() != null) positionCount += portfolio.getLiquidityPositions().size();
        if (portfolio.getYieldPositions() != null) positionCount += portfolio.getYieldPositions().size();
        if (portfolio.getLoanPositions() != null) positionCount += portfolio.getLoanPositions().size();
        
        if (positionCount > 1) {
            BigDecimal diversificationBonus = BigDecimal.valueOf(Math.min(0.2, 0.05 * positionCount));
            portfolioRisk = portfolioRisk.subtract(diversificationBonus);
        }
        
        // Ensure risk score is between 0 and 1
        portfolioRisk = portfolioRisk.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        
        logger.debug("Calculated portfolio risk score {} for user {}", portfolioRisk, portfolio.getUserAddress());
        return portfolioRisk;
    }
    
    /**
     * Calculate risk for liquidity position (impermanent loss primarily)
     */
    public BigDecimal calculateLiquidityPositionRisk(LiquidityPosition position) {
        BigDecimal risk = BigDecimal.ZERO;
        
        // Impermanent loss risk
        if (position.getImpermanentLoss() != null) {
            BigDecimal ilRisk = position.getImpermanentLoss().abs();
            risk = risk.add(ilRisk.multiply(BigDecimal.valueOf(2))); // Amplify IL impact
        }
        
        // Pool type risk
        String poolId = position.getPoolId();
        if (poolId.contains("STABLE")) {
            risk = risk.add(BigDecimal.valueOf(0.05)); // Stable pairs - low risk
        } else if (poolId.contains("ETH") || poolId.contains("BTC")) {
            risk = risk.add(BigDecimal.valueOf(0.15)); // Major pairs - medium risk
        } else {
            risk = risk.add(BigDecimal.valueOf(0.25)); // Exotic pairs - high risk
        }
        
        // Fee tier risk (higher fees often indicate more volatile pairs)
        if (position.getPoolFee() != null) {
            if (position.getPoolFee().compareTo(BigDecimal.valueOf(0.01)) > 0) { // > 1%
                risk = risk.add(BigDecimal.valueOf(0.1)); // High fee tier risk
            }
        }
        
        return risk.min(BigDecimal.ONE);
    }
    
    /**
     * Calculate risk for yield farming position
     */
    public BigDecimal calculateYieldFarmRisk(YieldFarmRewards rewards) {
        BigDecimal risk = BigDecimal.ZERO;
        
        // APR-based risk (higher APR = higher risk)
        BigDecimal effectiveApr = rewards.getEffectiveApr();
        if (effectiveApr != null) {
            if (effectiveApr.compareTo(BigDecimal.valueOf(0.20)) > 0) { // > 20%
                risk = risk.add(BigDecimal.valueOf(0.4)); // Very high APR risk
            } else if (effectiveApr.compareTo(BigDecimal.valueOf(0.10)) > 0) { // > 10%
                risk = risk.add(BigDecimal.valueOf(0.2)); // High APR risk
            } else if (effectiveApr.compareTo(BigDecimal.valueOf(0.05)) > 0) { // > 5%
                risk = risk.add(BigDecimal.valueOf(0.1)); // Medium APR risk
            }
        }
        
        // Lockup period risk
        if (rewards.getLockupPeriod() != null) {
            long lockupDays = rewards.getLockupPeriod() / 86400;
            if (lockupDays > 365) {
                risk = risk.add(BigDecimal.valueOf(0.3)); // Very long lockup
            } else if (lockupDays > 90) {
                risk = risk.add(BigDecimal.valueOf(0.15)); // Long lockup
            } else if (lockupDays > 30) {
                risk = risk.add(BigDecimal.valueOf(0.05)); // Medium lockup
            }
        }
        
        // Impermanent loss risk for LP token farms
        if (rewards.getImpermanentLossRisk() != null) {
            risk = risk.add(rewards.getImpermanentLossRisk());
        }
        
        return risk.min(BigDecimal.ONE);
    }
    
    /**
     * Get total risk exposure across all positions
     */
    public BigDecimal getTotalRiskExposure() {
        return totalRiskExposure;
    }
    
    /**
     * Get system liquidation threshold
     */
    public BigDecimal getLiquidationThreshold() {
        return liquidationThreshold;
    }
    
    /**
     * Update total risk exposure (called by other services)
     */
    public void updateTotalRiskExposure(BigDecimal newExposure) {
        this.totalRiskExposure = newExposure;
        logger.debug("Updated total risk exposure to {}", newExposure);
    }
    
    /**
     * Calculate Value at Risk (VaR) for a position
     */
    public BigDecimal calculateVaR(BigDecimal positionValue, BigDecimal riskScore, 
                                  BigDecimal confidenceLevel) {
        // Simple VaR calculation: VaR = Position Value * Risk Score * Z-score
        BigDecimal zScore = getZScore(confidenceLevel);
        return positionValue.multiply(riskScore).multiply(zScore);
    }
    
    /**
     * Calculate Conditional Value at Risk (CVaR)
     */
    public BigDecimal calculateCVaR(BigDecimal var, BigDecimal riskScore) {
        // CVaR is typically 1.2-1.5x VaR for most distributions
        BigDecimal cvarMultiplier = BigDecimal.ONE.add(riskScore.multiply(BigDecimal.valueOf(0.5)));
        return var.multiply(cvarMultiplier);
    }
    
    // Private helper methods
    private BigDecimal getZScore(BigDecimal confidenceLevel) {
        // Z-scores for common confidence levels
        if (confidenceLevel.compareTo(BigDecimal.valueOf(0.95)) >= 0) {
            return BigDecimal.valueOf(1.645); // 95% confidence
        } else if (confidenceLevel.compareTo(BigDecimal.valueOf(0.99)) >= 0) {
            return BigDecimal.valueOf(2.326); // 99% confidence
        } else {
            return BigDecimal.valueOf(1.282); // 90% confidence (default)
        }
    }
}