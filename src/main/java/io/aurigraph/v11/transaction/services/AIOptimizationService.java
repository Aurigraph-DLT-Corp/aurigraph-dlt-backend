package io.aurigraph.v11.transaction.services;

import io.aurigraph.v11.transaction.models.Transaction;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AIOptimizationService {

    private static final BigDecimal OPTIMAL_GAS_PRICE = BigDecimal.valueOf(20);
    private static final BigDecimal OPTIMAL_AMOUNT = BigDecimal.valueOf(100);
    private static final int OPTIMAL_TIME_WINDOW = 60; // seconds

    public BigDecimal calculateOptimizationScore(Transaction transaction) {
        Log.debugf("Calculating optimization score for transaction: %s", transaction.txHash);

        double score = 100.0;

        // Score based on gas price (optimal around 20 wei)
        if (transaction.gasPrice != null) {
            double gasDiff = Math.abs(
                transaction.gasPrice.doubleValue() - OPTIMAL_GAS_PRICE.doubleValue()
            );
            score -= Math.min(gasDiff * 0.5, 20);
        }

        // Score based on transaction amount (prefer moderate amounts)
        double amountDiff = Math.abs(
            transaction.amount.doubleValue() - OPTIMAL_AMOUNT.doubleValue()
        );
        score -= Math.min(amountDiff * 0.1, 15);

        // Score based on transaction type (prefer transfers)
        if (transaction.transactionType != Transaction.TransactionType.TRANSFER) {
            score -= 10;
        }

        // Ensure score is between 0 and 100
        score = Math.max(0, Math.min(100, score));

        return BigDecimal.valueOf(score);
    }

    public List<Transaction> optimizeTransactionOrder(List<Transaction> transactions) {
        Log.infof("Optimizing transaction order for %d transactions", transactions.size());

        List<Transaction> optimized = new ArrayList<>(transactions);

        // Sort by optimization score (highest first)
        optimized.sort((t1, t2) -> {
            BigDecimal score1 = t1.optimizationScore != null
                ? t1.optimizationScore
                : BigDecimal.ZERO;
            BigDecimal score2 = t2.optimizationScore != null
                ? t2.optimizationScore
                : BigDecimal.ZERO;

            return score2.compareTo(score1);
        });

        // Then sort by amount (prefer larger amounts for better utilization)
        optimized.sort((t1, t2) -> t2.amount.compareTo(t1.amount));

        return optimized;
    }

    public boolean isOptimalTimeForSubmission() {
        // Simple heuristic: submit during lower congestion hours
        int hour = java.time.LocalDateTime.now().getHour();
        return hour >= 2 && hour <= 5; // Off-peak hours
    }

    public BigDecimal suggestOptimalGasPrice(long networkLoad) {
        // Suggest gas price based on network load
        // Load is percentage of network capacity
        if (networkLoad < 30) {
            return BigDecimal.valueOf(15); // Low congestion
        } else if (networkLoad < 70) {
            return BigDecimal.valueOf(20); // Normal
        } else {
            return BigDecimal.valueOf(30); // High congestion
        }
    }

    public long estimateFinality(Transaction transaction) {
        // Estimate finality based on optimization score
        double score = transaction.optimizationScore != null
            ? transaction.optimizationScore.doubleValue()
            : 50.0;

        // Better score = faster finality
        // Estimate: 500ms base, -2ms per optimization point
        long baseFinality = 500L;
        long reduction = (long) (score * 2);

        return Math.max(100, baseFinality - reduction);
    }

    public BigDecimal calculateCompressionRatio(List<Transaction> transactions) {
        // Calculate how much transactions can be compressed using ML optimization
        // Assumes ~10% compression per optimization pass
        return BigDecimal.valueOf(0.90);
    }
}
