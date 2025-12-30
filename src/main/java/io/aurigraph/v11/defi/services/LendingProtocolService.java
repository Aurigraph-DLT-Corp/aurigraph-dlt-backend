package io.aurigraph.v11.defi.services;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing lending and borrowing operations across various DeFi protocols.
 * 
 * This service provides a unified interface for interacting with different lending protocols
 * such as Aave, Compound, and other DeFi lending platforms. It handles loan origination,
 * repayment, collateral management, and interest rate calculations.
 * 
 * Key Features:
 * - Multi-protocol lending support
 * - Automated collateralization management
 * - Real-time interest rate tracking
 * - Risk assessment and liquidation protection
 * - Cross-chain lending capabilities
 * 
 * Performance Requirements:
 * - Support for 100K+ concurrent lending positions
 * - Sub-second response time for rate queries
 * - Real-time collateral ratio monitoring
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface LendingProtocolService {

    /**
     * Creates a new lending position by depositing collateral and borrowing assets.
     * 
     * @param collateralAsset the asset to use as collateral
     * @param collateralAmount the amount of collateral to deposit
     * @param borrowAsset the asset to borrow
     * @param borrowAmount the amount to borrow
     * @param protocolId the lending protocol to use
     * @param maxInterestRate maximum acceptable interest rate
     * @return Uni containing the lending position details
     */
    Uni<LendingPosition> createLendingPosition(
        String collateralAsset,
        BigDecimal collateralAmount,
        String borrowAsset,
        BigDecimal borrowAmount,
        String protocolId,
        BigDecimal maxInterestRate
    );

    /**
     * Supplies liquidity to a lending protocol to earn interest.
     * 
     * @param asset the asset to supply
     * @param amount the amount to supply
     * @param protocolId the lending protocol identifier
     * @param minInterestRate minimum acceptable interest rate
     * @return Uni containing the supply position details
     */
    Uni<LendingPosition> supplyLiquidity(
        String asset,
        BigDecimal amount,
        String protocolId,
        BigDecimal minInterestRate
    );

    /**
     * Repays a portion or all of a borrowing position.
     * 
     * @param positionId the lending position identifier
     * @param repayAmount the amount to repay (null for full repayment)
     * @param asset the asset being repaid
     * @return Uni containing the updated position details
     */
    Uni<LendingPosition> repayLoan(String positionId, BigDecimal repayAmount, String asset);

    /**
     * Withdraws supplied liquidity from a lending protocol.
     * 
     * @param positionId the supply position identifier
     * @param withdrawAmount the amount to withdraw (null for full withdrawal)
     * @param asset the asset being withdrawn
     * @return Uni containing the updated position details
     */
    Uni<LendingPosition> withdrawSupply(String positionId, BigDecimal withdrawAmount, String asset);

    /**
     * Adds additional collateral to an existing borrowing position.
     * 
     * @param positionId the lending position identifier
     * @param collateralAsset the collateral asset to add
     * @param amount the amount of collateral to add
     * @return Uni containing the updated position details
     */
    Uni<LendingPosition> addCollateral(String positionId, String collateralAsset, BigDecimal amount);

    /**
     * Removes collateral from a borrowing position (if health factor allows).
     * 
     * @param positionId the lending position identifier
     * @param collateralAsset the collateral asset to remove
     * @param amount the amount of collateral to remove
     * @return Uni containing the updated position details
     */
    Uni<LendingPosition> removeCollateral(String positionId, String collateralAsset, BigDecimal amount);

    /**
     * Retrieves current lending rates for all supported assets across protocols.
     * 
     * @return Uni containing a map of asset -> protocol -> rates
     */
    Uni<Map<String, Map<String, LendingRates>>> getCurrentLendingRates();

    /**
     * Gets the current lending rates for a specific asset across all protocols.
     * 
     * @param asset the asset to get rates for
     * @return Uni containing protocol-specific rates for the asset
     */
    Uni<Map<String, LendingRates>> getLendingRatesForAsset(String asset);

    /**
     * Calculates the current health factor for a lending position.
     * 
     * @param positionId the lending position identifier
     * @return Uni containing the current health factor
     */
    Uni<BigDecimal> calculateHealthFactor(String positionId);

    /**
     * Retrieves all lending positions for a specific user.
     * 
     * @param userAddress the user's wallet address
     * @return Multi streaming all lending positions for the user
     */
    Multi<LendingPosition> getUserLendingPositions(String userAddress);

    /**
     * Gets detailed information about a specific lending position.
     * 
     * @param positionId the lending position identifier
     * @return Uni containing the position details
     */
    Uni<LendingPosition> getLendingPosition(String positionId);

    /**
     * Monitors lending positions for liquidation risk and sends alerts.
     * 
     * @param minHealthFactor minimum health factor threshold for alerts
     * @return Multi streaming positions at risk of liquidation
     */
    Multi<LendingPosition> monitorLiquidationRisk(BigDecimal minHealthFactor);

    /**
     * Executes automatic liquidation protection by adding collateral or repaying loans.
     * 
     * @param positionId the lending position identifier
     * @param protectionStrategy the liquidation protection strategy to use
     * @return Uni containing the protection execution result
     */
    Uni<LiquidationProtectionResult> executeLiquidationProtection(
        String positionId, 
        LiquidationProtectionStrategy protectionStrategy
    );

    /**
     * Optimizes lending positions by moving assets to higher-yield protocols.
     * 
     * @param userAddress the user's wallet address
     * @param minYieldImprovement minimum yield improvement threshold
     * @return Uni containing optimization suggestions
     */
    Uni<List<LendingOptimization>> optimizeLendingPositions(
        String userAddress, 
        BigDecimal minYieldImprovement
    );

    /**
     * Gets historical lending rates for analysis and forecasting.
     * 
     * @param asset the asset to get historical data for
     * @param protocolId the protocol identifier
     * @param fromTimestamp start timestamp for historical data
     * @param toTimestamp end timestamp for historical data
     * @return Multi streaming historical rate data
     */
    Multi<HistoricalLendingRate> getHistoricalLendingRates(
        String asset, 
        String protocolId, 
        long fromTimestamp, 
        long toTimestamp
    );

    /**
     * Calculates projected earnings for a lending position over time.
     * 
     * @param positionId the lending position identifier
     * @param projectionPeriodDays the number of days to project
     * @return Uni containing earnings projection
     */
    Uni<EarningsProjection> calculateEarningsProjection(String positionId, int projectionPeriodDays);

    // Inner classes for data transfer objects
    
    /**
     * Represents a lending or borrowing position in a DeFi protocol.
     */
    public static class LendingPosition {
        public String positionId;
        public String userAddress;
        public String protocolId;
        public String positionType; // "SUPPLY" or "BORROW"
        public String asset;
        public BigDecimal amount;
        public BigDecimal interestRate;
        public BigDecimal accruedInterest;
        public BigDecimal healthFactor;
        public Map<String, BigDecimal> collateralAssets;
        public long createdTimestamp;
        public long lastUpdatedTimestamp;
        public String status; // "ACTIVE", "LIQUIDATED", "CLOSED"
    }

    /**
     * Contains lending and borrowing rates for a specific asset and protocol.
     */
    public static class LendingRates {
        public String asset;
        public String protocolId;
        public BigDecimal supplyRate;
        public BigDecimal borrowRate;
        public BigDecimal utilizationRate;
        public long lastUpdated;
    }

    /**
     * Represents the result of a liquidation protection operation.
     */
    public static class LiquidationProtectionResult {
        public String positionId;
        public String protectionAction; // "COLLATERAL_ADDED", "LOAN_REPAID", "POSITION_CLOSED"
        public BigDecimal amountProtected;
        public BigDecimal newHealthFactor;
        public BigDecimal protectionCost;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Strategy for liquidation protection.
     */
    public enum LiquidationProtectionStrategy {
        ADD_COLLATERAL,
        PARTIAL_REPAYMENT,
        CLOSE_POSITION,
        FLASH_LOAN_REFINANCE
    }

    /**
     * Contains optimization suggestions for lending positions.
     */
    public static class LendingOptimization {
        public String currentPositionId;
        public String recommendedProtocol;
        public BigDecimal currentYield;
        public BigDecimal potentialYield;
        public BigDecimal yieldImprovement;
        public BigDecimal migrationCost;
        public BigDecimal netBenefit;
        public String optimizationType; // "PROTOCOL_MIGRATION", "RATE_OPTIMIZATION"
    }

    /**
     * Historical lending rate data point.
     */
    public static class HistoricalLendingRate {
        public String asset;
        public String protocolId;
        public BigDecimal supplyRate;
        public BigDecimal borrowRate;
        public BigDecimal utilizationRate;
        public long timestamp;
    }

    /**
     * Projected earnings for a lending position.
     */
    public static class EarningsProjection {
        public String positionId;
        public BigDecimal currentPrincipal;
        public BigDecimal projectedEarnings;
        public BigDecimal projectedTotal;
        public BigDecimal averageRate;
        public int projectionDays;
        public long calculatedTimestamp;
    }
}