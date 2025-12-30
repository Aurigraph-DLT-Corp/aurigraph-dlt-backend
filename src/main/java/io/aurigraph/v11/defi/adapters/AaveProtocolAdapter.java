package io.aurigraph.v11.defi.adapters;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Adapter interface for Aave lending protocol integration.
 * 
 * This adapter provides comprehensive integration with Aave's lending and borrowing
 * ecosystem including deposit, borrowing, liquidation, and governance features.
 * It supports both Aave V2 and V3 protocols with advanced features like
 * flash loans, rate switching, and isolation mode.
 * 
 * Key Features:
 * - Supply and borrow operations across multiple assets
 * - Flash loan integration with callback handling
 * - Interest rate mode switching (stable/variable)
 * - Liquidation protection and health factor monitoring
 * - Isolation mode and efficiency mode (eMode) support
 * - Governance token staking and rewards
 * 
 * Performance Requirements:
 * - Support for 50K+ active positions
 * - Real-time health factor monitoring
 * - Sub-second rate calculations and updates
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface AaveProtocolAdapter {

    /**
     * Supplies assets to the Aave protocol to earn interest.
     * 
     * @param asset the asset address to supply
     * @param amount the amount to supply
     * @param onBehalfOf the address that will receive the aTokens
     * @param referralCode referral code for potential rewards
     * @return Uni containing the supply operation result
     */
    Uni<AaveSupplyResult> supply(
        String asset,
        BigDecimal amount,
        String onBehalfOf,
        int referralCode
    );

    /**
     * Withdraws supplied assets from the Aave protocol.
     * 
     * @param asset the asset address to withdraw
     * @param amount the amount to withdraw (use max value for full withdrawal)
     * @param to the address that will receive the withdrawn assets
     * @return Uni containing the withdrawal operation result
     */
    Uni<AaveWithdrawResult> withdraw(String asset, BigDecimal amount, String to);

    /**
     * Borrows assets from the Aave protocol.
     * 
     * @param asset the asset address to borrow
     * @param amount the amount to borrow
     * @param interestRateMode the interest rate mode (1=stable, 2=variable)
     * @param referralCode referral code for potential rewards
     * @param onBehalfOf the address that will incur the debt
     * @return Uni containing the borrow operation result
     */
    Uni<AaveBorrowResult> borrow(
        String asset,
        BigDecimal amount,
        int interestRateMode,
        int referralCode,
        String onBehalfOf
    );

    /**
     * Repays borrowed assets to the Aave protocol.
     * 
     * @param asset the asset address to repay
     * @param amount the amount to repay (use max value for full repayment)
     * @param rateMode the interest rate mode of the debt being repaid
     * @param onBehalfOf the address for which debt is being repaid
     * @return Uni containing the repay operation result
     */
    Uni<AaveRepayResult> repay(
        String asset,
        BigDecimal amount,
        int rateMode,
        String onBehalfOf
    );

    /**
     * Switches the interest rate mode for a borrow position.
     * 
     * @param asset the asset address
     * @param rateMode the new interest rate mode (1=stable, 2=variable)
     * @return Uni containing the rate switch result
     */
    Uni<AaveRateSwitchResult> swapBorrowRateMode(String asset, int rateMode);

    /**
     * Sets user E-Mode (efficiency mode) for better capital efficiency.
     * 
     * @param categoryId the E-Mode category ID (0 to disable)
     * @return Uni containing the E-Mode setting result
     */
    Uni<AaveEModeResult> setUserEMode(int categoryId);

    /**
     * Uses supplied assets as collateral or removes them from collateral.
     * 
     * @param asset the asset address
     * @param useAsCollateral true to use as collateral, false to disable
     * @return Uni containing the collateral setting result
     */
    Uni<Boolean> setUserUseReserveAsCollateral(String asset, boolean useAsCollateral);

    /**
     * Executes a flash loan operation.
     * 
     * @param assets array of asset addresses to flash loan
     * @param amounts array of amounts to flash loan
     * @param modes array of debt modes (0=no debt, 1=stable, 2=variable)
     * @param onBehalfOf address that will incur debt (if modes > 0)
     * @param params encoded parameters for flash loan callback
     * @param referralCode referral code for potential rewards
     * @return Uni containing the flash loan execution result
     */
    Uni<AaveFlashLoanResult> flashLoan(
        String[] assets,
        BigDecimal[] amounts,
        int[] modes,
        String onBehalfOf,
        byte[] params,
        int referralCode
    );

    /**
     * Gets comprehensive user account data including health factor.
     * 
     * @param userAddress the user's address
     * @return Uni containing the user account data
     */
    Uni<AaveUserAccountData> getUserAccountData(String userAddress);

    /**
     * Gets detailed reserve data for a specific asset.
     * 
     * @param asset the asset address
     * @return Uni containing the reserve configuration and data
     */
    Uni<AaveReserveData> getReserveData(String asset);

    /**
     * Gets the current configuration for a reserve.
     * 
     * @param asset the asset address
     * @return Uni containing the reserve configuration
     */
    Uni<AaveReserveConfiguration> getConfiguration(String asset);

    /**
     * Gets all user's aToken balances across all reserves.
     * 
     * @param userAddress the user's address
     * @return Multi streaming the user's aToken balances
     */
    Multi<AaveUserBalance> getUserReserveData(String userAddress);

    /**
     * Calculates the health factor for a hypothetical operation.
     * 
     * @param userAddress the user's address
     * @param reserveAddress the reserve address for the operation
     * @param amountDelta the amount change (positive for increase, negative for decrease)
     * @param isCollateral whether the operation affects collateral
     * @param isBorrow whether the operation is a borrow
     * @return Uni containing the projected health factor
     */
    Uni<BigDecimal> calculateHealthFactorFromBalances(
        String userAddress,
        String reserveAddress,
        BigDecimal amountDelta,
        boolean isCollateral,
        boolean isBorrow
    );

    /**
     * Monitors user positions for liquidation risk.
     * 
     * @param userAddress the user's address
     * @param healthFactorThreshold the health factor threshold for alerts
     * @return Multi streaming health factor updates and risk alerts
     */
    Multi<AaveLiquidationRisk> monitorLiquidationRisk(
        String userAddress,
        BigDecimal healthFactorThreshold
    );

    /**
     * Executes liquidation of an undercollateralized position.
     * 
     * @param collateralAsset the collateral asset being liquidated
     * @param debtAsset the debt asset being repaid
     * @param userToLiquidate the address of the user being liquidated
     * @param debtToCover the amount of debt to cover
     * @param receiveAToken whether to receive aTokens or underlying asset
     * @return Uni containing the liquidation result
     */
    Uni<AaveLiquidationResult> liquidationCall(
        String collateralAsset,
        String debtAsset,
        String userToLiquidate,
        BigDecimal debtToCover,
        boolean receiveAToken
    );

    /**
     * Gets available liquidity for borrowing across all reserves.
     * 
     * @return Multi streaming liquidity information for all reserves
     */
    Multi<AaveLiquidityData> getAvailableLiquidity();

    /**
     * Gets historical interest rates for analysis.
     * 
     * @param asset the asset address
     * @param fromTimestamp start timestamp
     * @param toTimestamp end timestamp
     * @return Multi streaming historical rate data
     */
    Multi<AaveHistoricalRate> getHistoricalRates(String asset, long fromTimestamp, long toTimestamp);

    /**
     * Stakes AAVE tokens in the Safety Module for rewards.
     * 
     * @param amount the amount of AAVE tokens to stake
     * @return Uni containing the staking result
     */
    Uni<AaveStakingResult> stakeAAVE(BigDecimal amount);

    /**
     * Claims rewards from the Safety Module.
     * 
     * @param to the address to receive rewards
     * @param amount the amount of rewards to claim
     * @return Uni containing the claim result
     */
    Uni<AaveRewardClaim> claimRewards(String to, BigDecimal amount);

    // Inner classes for data transfer objects

    /**
     * Result of a supply operation to Aave.
     */
    public static class AaveSupplyResult {
        public String transactionHash;
        public String asset;
        public BigDecimal amount;
        public BigDecimal aTokensReceived;
        public BigDecimal newSupplyRate;
        public BigDecimal newHealthFactor;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of a withdrawal operation from Aave.
     */
    public static class AaveWithdrawResult {
        public String transactionHash;
        public String asset;
        public BigDecimal amountWithdrawn;
        public BigDecimal aTokensBurned;
        public BigDecimal newHealthFactor;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of a borrow operation from Aave.
     */
    public static class AaveBorrowResult {
        public String transactionHash;
        public String asset;
        public BigDecimal amountBorrowed;
        public int interestRateMode;
        public BigDecimal borrowRate;
        public BigDecimal newHealthFactor;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of a repay operation to Aave.
     */
    public static class AaveRepayResult {
        public String transactionHash;
        public String asset;
        public BigDecimal amountRepaid;
        public BigDecimal remainingDebt;
        public BigDecimal newHealthFactor;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of switching interest rate mode.
     */
    public static class AaveRateSwitchResult {
        public String transactionHash;
        public String asset;
        public int oldRateMode;
        public int newRateMode;
        public BigDecimal newBorrowRate;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of setting E-Mode category.
     */
    public static class AaveEModeResult {
        public String transactionHash;
        public int oldCategoryId;
        public int newCategoryId;
        public BigDecimal newHealthFactor;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of a flash loan operation.
     */
    public static class AaveFlashLoanResult {
        public String transactionHash;
        public String[] assets;
        public BigDecimal[] amounts;
        public BigDecimal[] premiums;
        public BigDecimal totalPremium;
        public boolean success;
        public String errorMessage;
        public Object callbackResult; // Custom result from flash loan callback
    }

    /**
     * Comprehensive user account data from Aave.
     */
    public static class AaveUserAccountData {
        public String userAddress;
        public BigDecimal totalCollateralETH;
        public BigDecimal totalDebtETH;
        public BigDecimal availableBorrowsETH;
        public BigDecimal currentLiquidationThreshold;
        public BigDecimal ltv; // Loan-to-Value ratio
        public BigDecimal healthFactor;
        public int currentEModeCategory;
        public boolean hasDebt;
        public boolean isInIsolationMode;
        public long lastUpdated;
    }

    /**
     * Reserve data for a specific asset in Aave.
     */
    public static class AaveReserveData {
        public String asset;
        public String aTokenAddress;
        public String stableDebtTokenAddress;
        public String variableDebtTokenAddress;
        public String interestRateStrategyAddress;
        public BigDecimal liquidityRate;
        public BigDecimal variableBorrowRate;
        public BigDecimal stableBorrowRate;
        public BigDecimal averageStableBorrowRate;
        public BigDecimal liquidityIndex;
        public BigDecimal variableBorrowIndex;
        public long lastUpdateTimestamp;
        public BigDecimal totalSupply;
        public BigDecimal totalVariableDebt;
        public BigDecimal totalStableDebt;
        public BigDecimal availableLiquidity;
    }

    /**
     * Reserve configuration for a specific asset.
     */
    public static class AaveReserveConfiguration {
        public String asset;
        public BigDecimal ltv;
        public BigDecimal liquidationThreshold;
        public BigDecimal liquidationBonus;
        public int decimals;
        public boolean isActive;
        public boolean isFrozen;
        public boolean borrowingEnabled;
        public boolean stableBorrowRateEnabled;
        public boolean isReserveAsCollateral;
        public boolean isInIsolationMode;
        public BigDecimal isolationModeTotalDebt;
        public BigDecimal reserveFactor;
    }

    /**
     * User balance information for a specific reserve.
     */
    public static class AaveUserBalance {
        public String asset;
        public String aTokenAddress;
        public BigDecimal aTokenBalance;
        public BigDecimal variableDebt;
        public BigDecimal stableDebt;
        public BigDecimal totalDebt;
        public BigDecimal availableToWithdraw;
        public boolean usageAsCollateralEnabled;
        public BigDecimal liquidityRate;
        public BigDecimal variableBorrowRate;
        public BigDecimal stableBorrowRate;
    }

    /**
     * Liquidation risk assessment for a user.
     */
    public static class AaveLiquidationRisk {
        public String userAddress;
        public BigDecimal currentHealthFactor;
        public BigDecimal healthFactorThreshold;
        public RiskLevel riskLevel;
        public BigDecimal recommendedAction; // Amount to repay or add as collateral
        public String recommendedAsset;
        public ActionType recommendedActionType;
        public long assessmentTimestamp;
    }

    /**
     * Risk levels for liquidation assessment.
     */
    public enum RiskLevel {
        SAFE, // Health factor > 2.0
        LOW_RISK, // Health factor > 1.5
        MEDIUM_RISK, // Health factor > 1.2
        HIGH_RISK, // Health factor > 1.05
        CRITICAL // Health factor <= 1.05
    }

    /**
     * Recommended action types for risk mitigation.
     */
    public enum ActionType {
        ADD_COLLATERAL,
        REPAY_DEBT,
        SWITCH_RATE_MODE,
        ENABLE_EMODE
    }

    /**
     * Result of a liquidation operation.
     */
    public static class AaveLiquidationResult {
        public String transactionHash;
        public String collateralAsset;
        public String debtAsset;
        public String liquidatedUser;
        public BigDecimal debtRepaid;
        public BigDecimal collateralLiquidated;
        public BigDecimal liquidationBonus;
        public BigDecimal protocolFee;
        public BigDecimal liquidatorProfit;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Available liquidity data for a reserve.
     */
    public static class AaveLiquidityData {
        public String asset;
        public BigDecimal totalLiquidity;
        public BigDecimal availableLiquidity;
        public BigDecimal utilizationRate;
        public BigDecimal liquidityRate;
        public BigDecimal variableBorrowRate;
        public BigDecimal stableBorrowRate;
        public BigDecimal borrowCap;
        public BigDecimal supplyCap;
        public boolean canBorrow;
        public boolean canSupply;
    }

    /**
     * Historical interest rate data.
     */
    public static class AaveHistoricalRate {
        public String asset;
        public BigDecimal liquidityRate;
        public BigDecimal variableBorrowRate;
        public BigDecimal stableBorrowRate;
        public BigDecimal utilizationRate;
        public long timestamp;
    }

    /**
     * Result of staking AAVE tokens.
     */
    public static class AaveStakingResult {
        public String transactionHash;
        public BigDecimal amountStaked;
        public BigDecimal totalStaked;
        public BigDecimal expectedRewards;
        public long stakingTimestamp;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of claiming rewards from staking.
     */
    public static class AaveRewardClaim {
        public String transactionHash;
        public BigDecimal rewardsClaimed;
        public BigDecimal remainingRewards;
        public String rewardToken;
        public boolean success;
        public String errorMessage;
    }
}