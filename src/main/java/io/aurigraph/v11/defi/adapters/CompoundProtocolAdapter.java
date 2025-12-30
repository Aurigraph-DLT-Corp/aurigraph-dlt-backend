package io.aurigraph.v11.defi.adapters;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;

/**
 * Adapter interface for Compound lending protocol integration.
 * 
 * This adapter provides comprehensive integration with Compound's lending protocol
 * including both Compound V2 and V3 (Comet) features. It handles supply, borrowing,
 * liquidation, and governance operations with advanced features like automatic
 * collateral management and yield optimization.
 * 
 * Key Features:
 * - Supply and borrow operations across all cTokens
 * - Compound governance participation (COMP rewards)
 * - Liquidation protection and execution
 * - Interest rate model monitoring
 * - Flash loan integration via Compound V3
 * - Automated yield compounding
 * 
 * Performance Requirements:
 * - Support for 25K+ active positions
 * - Real-time interest rate and collateral monitoring
 * - Sub-second rate calculations and health checks
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface CompoundProtocolAdapter {

    /**
     * Supplies assets to Compound to start earning interest.
     * 
     * @param cTokenAddress the cToken contract address
     * @param underlyingAmount the amount of underlying asset to supply
     * @param enableAsCollateral whether to enable the supplied asset as collateral
     * @return Uni containing the supply operation result
     */
    Uni<CompoundSupplyResult> supply(
        String cTokenAddress,
        BigDecimal underlyingAmount,
        boolean enableAsCollateral
    );

    /**
     * Redeems cTokens for underlying assets.
     * 
     * @param cTokenAddress the cToken contract address
     * @param redeemAmount the amount to redeem (in underlying tokens)
     * @param isRedeemTokens true for cToken amount, false for underlying amount
     * @return Uni containing the redemption result
     */
    Uni<CompoundRedeemResult> redeem(
        String cTokenAddress,
        BigDecimal redeemAmount,
        boolean isRedeemTokens
    );

    /**
     * Borrows assets from Compound using supplied collateral.
     * 
     * @param cTokenAddress the cToken contract address to borrow
     * @param borrowAmount the amount of underlying asset to borrow
     * @return Uni containing the borrow operation result
     */
    Uni<CompoundBorrowResult> borrow(String cTokenAddress, BigDecimal borrowAmount);

    /**
     * Repays borrowed assets to Compound.
     * 
     * @param cTokenAddress the cToken contract address
     * @param repayAmount the amount to repay (use max value for full repayment)
     * @return Uni containing the repay operation result
     */
    Uni<CompoundRepayResult> repayBorrow(String cTokenAddress, BigDecimal repayAmount);

    /**
     * Enters markets to use assets as collateral.
     * 
     * @param cTokenAddresses array of cToken addresses to enter as collateral
     * @return Uni containing the market entry result
     */
    Uni<CompoundMarketResult> enterMarkets(String[] cTokenAddresses);

    /**
     * Exits markets to stop using assets as collateral.
     * 
     * @param cTokenAddress the cToken address to exit
     * @return Uni containing the market exit result
     */
    Uni<CompoundMarketResult> exitMarket(String cTokenAddress);

    /**
     * Gets comprehensive account information including liquidity and collateral.
     * 
     * @param userAddress the user's address
     * @return Uni containing the account liquidity information
     */
    Uni<CompoundAccountLiquidity> getAccountLiquidity(String userAddress);

    /**
     * Gets detailed information about a specific cToken market.
     * 
     * @param cTokenAddress the cToken contract address
     * @return Uni containing the market information
     */
    Uni<CompoundMarketInfo> getMarketInfo(String cTokenAddress);

    /**
     * Gets the current exchange rate for a cToken.
     * 
     * @param cTokenAddress the cToken contract address
     * @return Uni containing the current exchange rate
     */
    Uni<BigDecimal> getExchangeRateStored(String cTokenAddress);

    /**
     * Gets the current supply and borrow rates for a market.
     * 
     * @param cTokenAddress the cToken contract address
     * @return Uni containing the current interest rates
     */
    Uni<CompoundRates> getCurrentRates(String cTokenAddress);

    /**
     * Gets all user balances across Compound markets.
     * 
     * @param userAddress the user's address
     * @return Multi streaming user balances for all markets
     */
    Multi<CompoundUserBalance> getUserBalances(String userAddress);

    /**
     * Claims accumulated COMP rewards for a user.
     * 
     * @param userAddress the user's address
     * @param cTokenAddresses array of cToken addresses to claim from
     * @return Uni containing the claim result
     */
    Uni<CompoundClaimResult> claimComp(String userAddress, String[] cTokenAddresses);

    /**
     * Gets the amount of COMP rewards accrued for a user.
     * 
     * @param userAddress the user's address
     * @return Uni containing the accrued COMP amount
     */
    Uni<BigDecimal> getCompAccrued(String userAddress);

    /**
     * Calculates hypothetical account liquidity after a potential operation.
     * 
     * @param userAddress the user's address
     * @param cTokenModify the cToken being modified
     * @param redeemTokens the amount of cTokens to redeem
     * @param borrowAmount the amount to borrow
     * @return Uni containing the hypothetical liquidity calculation
     */
    Uni<CompoundAccountLiquidity> getHypotheticalAccountLiquidity(
        String userAddress,
        String cTokenModify,
        BigDecimal redeemTokens,
        BigDecimal borrowAmount
    );

    /**
     * Liquidates an undercollateralized borrower.
     * 
     * @param borrower the address of the borrower to liquidate
     * @param cTokenBorrowed the cToken address of the borrowed asset
     * @param cTokenCollateral the cToken address of the collateral asset
     * @param repayAmount the amount of borrowed asset to repay
     * @return Uni containing the liquidation result
     */
    Uni<CompoundLiquidationResult> liquidateBorrow(
        String borrower,
        String cTokenBorrowed,
        String cTokenCollateral,
        BigDecimal repayAmount
    );

    /**
     * Monitors positions for liquidation opportunities.
     * 
     * @param minLiquidationIncentive minimum liquidation incentive threshold
     * @param maxGasPrice maximum gas price for profitable liquidation
     * @return Multi streaming liquidation opportunities
     */
    Multi<CompoundLiquidationOpportunity> monitorLiquidationOpportunities(
        BigDecimal minLiquidationIncentive,
        BigDecimal maxGasPrice
    );

    /**
     * Gets historical interest rates for analysis.
     * 
     * @param cTokenAddress the cToken contract address
     * @param fromBlock starting block number
     * @param toBlock ending block number
     * @return Multi streaming historical rate data
     */
    Multi<CompoundHistoricalRate> getHistoricalRates(
        String cTokenAddress,
        long fromBlock,
        long toBlock
    );

    /**
     * Executes yield optimization by moving funds to higher-yielding markets.
     * 
     * @param userAddress the user's address
     * @param minYieldImprovement minimum yield improvement threshold
     * @return Uni containing yield optimization recommendations
     */
    Uni<List<CompoundYieldOptimization>> optimizeYield(
        String userAddress,
        BigDecimal minYieldImprovement
    );

    /**
     * Sets up automated compound interest reinvestment.
     * 
     * @param userAddress the user's address
     * @param cTokenAddress the cToken to auto-compound
     * @param compoundingFrequency frequency in hours
     * @param minCompBalance minimum COMP balance to trigger compounding
     * @return Uni containing the auto-compound setup result
     */
    Uni<CompoundAutoCompoundSetup> setupAutoCompound(
        String userAddress,
        String cTokenAddress,
        int compoundingFrequency,
        BigDecimal minCompBalance
    );

    // Inner classes for data transfer objects

    /**
     * Result of a supply operation to Compound.
     */
    public static class CompoundSupplyResult {
        public String transactionHash;
        public String cTokenAddress;
        public String underlyingAsset;
        public BigDecimal underlyingAmount;
        public BigDecimal cTokensMinted;
        public BigDecimal newBalance;
        public BigDecimal newExchangeRate;
        public boolean enabledAsCollateral;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of a redeem operation from Compound.
     */
    public static class CompoundRedeemResult {
        public String transactionHash;
        public String cTokenAddress;
        public String underlyingAsset;
        public BigDecimal underlyingAmount;
        public BigDecimal cTokensBurned;
        public BigDecimal remainingBalance;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of a borrow operation from Compound.
     */
    public static class CompoundBorrowResult {
        public String transactionHash;
        public String cTokenAddress;
        public String underlyingAsset;
        public BigDecimal borrowAmount;
        public BigDecimal newBorrowBalance;
        public BigDecimal accountLiquidity;
        public BigDecimal borrowRate;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of a repay operation to Compound.
     */
    public static class CompoundRepayResult {
        public String transactionHash;
        public String cTokenAddress;
        public String underlyingAsset;
        public BigDecimal repayAmount;
        public BigDecimal remainingBorrowBalance;
        public BigDecimal accountLiquidity;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of market entry/exit operations.
     */
    public static class CompoundMarketResult {
        public String transactionHash;
        public String[] cTokenAddresses;
        public boolean[] results; // Success result for each market
        public String operation; // "ENTER" or "EXIT"
        public boolean success;
        public String errorMessage;
    }

    /**
     * Account liquidity information from Compound.
     */
    public static class CompoundAccountLiquidity {
        public String userAddress;
        public BigDecimal liquidity; // USD value of excess collateral
        public BigDecimal shortfall; // USD value of shortfall (if any)
        public BigDecimal totalSupplyValueUSD;
        public BigDecimal totalBorrowValueUSD;
        public BigDecimal healthFactor; // liquidity / total borrow value
        public boolean canBorrow;
        public boolean atRiskOfLiquidation;
        public long lastUpdated;
    }

    /**
     * Market information for a specific cToken.
     */
    public static class CompoundMarketInfo {
        public String cTokenAddress;
        public String underlyingAddress;
        public String symbol;
        public int decimals;
        public BigDecimal exchangeRate;
        public BigDecimal supplyRate;
        public BigDecimal borrowRate;
        public BigDecimal reserveFactor;
        public BigDecimal collateralFactor;
        public BigDecimal totalSupply;
        public BigDecimal totalBorrows;
        public BigDecimal totalReserves;
        public BigDecimal cash;
        public boolean isListed;
        public boolean compSpeeds; // COMP distribution rates
        public long lastUpdated;
    }

    /**
     * Current supply and borrow rates for a market.
     */
    public static class CompoundRates {
        public String cTokenAddress;
        public BigDecimal supplyRate; // Per block
        public BigDecimal borrowRate; // Per block
        public BigDecimal supplyRateAPY; // Annual percentage yield
        public BigDecimal borrowRateAPY; // Annual percentage rate
        public BigDecimal utilizationRate;
        public long blockNumber;
        public long timestamp;
    }

    /**
     * User balance information for a specific cToken.
     */
    public static class CompoundUserBalance {
        public String cTokenAddress;
        public String underlyingAsset;
        public String symbol;
        public BigDecimal cTokenBalance;
        public BigDecimal underlyingBalance;
        public BigDecimal borrowBalance;
        public BigDecimal exchangeRate;
        public boolean isCollateralEnabled;
        public BigDecimal compAccrued;
        public BigDecimal supplyBalanceUSD;
        public BigDecimal borrowBalanceUSD;
    }

    /**
     * Result of claiming COMP rewards.
     */
    public static class CompoundClaimResult {
        public String transactionHash;
        public String userAddress;
        public String[] cTokenAddresses;
        public BigDecimal compClaimed;
        public BigDecimal compBalanceAfter;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of a liquidation operation.
     */
    public static class CompoundLiquidationResult {
        public String transactionHash;
        public String liquidator;
        public String borrower;
        public String cTokenBorrowed;
        public String cTokenCollateral;
        public BigDecimal repayAmount;
        public BigDecimal seizeTokens;
        public BigDecimal liquidationIncentive;
        public BigDecimal liquidatorProfit;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Liquidation opportunity information.
     */
    public static class CompoundLiquidationOpportunity {
        public String borrower;
        public String cTokenBorrowed;
        public String cTokenCollateral;
        public BigDecimal borrowBalance;
        public BigDecimal collateralBalance;
        public BigDecimal maxRepay; // Maximum amount that can be repaid
        public BigDecimal seizeAmount; // Amount of collateral that would be seized
        public BigDecimal liquidationIncentive;
        public BigDecimal estimatedProfit;
        public BigDecimal estimatedGasCost;
        public BigDecimal netProfit;
        public boolean isProfitable;
        public long detectedBlock;
    }

    /**
     * Historical interest rate data.
     */
    public static class CompoundHistoricalRate {
        public String cTokenAddress;
        public BigDecimal supplyRate;
        public BigDecimal borrowRate;
        public BigDecimal utilizationRate;
        public BigDecimal totalSupply;
        public BigDecimal totalBorrows;
        public long blockNumber;
        public long timestamp;
    }

    /**
     * Yield optimization recommendation.
     */
    public static class CompoundYieldOptimization {
        public String currentCToken;
        public String recommendedCToken;
        public BigDecimal currentAPY;
        public BigDecimal recommendedAPY;
        public BigDecimal yieldImprovement;
        public BigDecimal migrationCost;
        public BigDecimal breakEvenDays;
        public String optimizationType; // "MARKET_SWITCH", "COLLATERAL_OPTIMIZATION"
        public String rationale;
    }

    /**
     * Auto-compound setup configuration.
     */
    public static class CompoundAutoCompoundSetup {
        public String userAddress;
        public String cTokenAddress;
        public boolean enabled;
        public int compoundingFrequency; // in hours
        public BigDecimal minCompBalance;
        public BigDecimal estimatedGasCost;
        public long nextCompoundTime;
        public boolean success;
        public String errorMessage;
    }
}