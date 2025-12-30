package io.aurigraph.v11.ai;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransactionScoringModel
 * Tests ML-based transaction scoring and ordering
 */
@QuarkusTest
@DisplayName("Transaction Scoring Model Tests")
public class TransactionScoringModelTest {

    private TransactionScoringModel scoringModel;

    @BeforeEach
    void setUp() {
        scoringModel = new TransactionScoringModel();
    }

    @Test
    @DisplayName("Score single transaction should produce valid score and features")
    void testScoreSingleTransaction() {
        // Given
        String txnId = "txn-001";
        String sender = "sender-1";
        long sizeBytes = 256;
        BigDecimal gasPrice = BigDecimal.valueOf(100);
        long createdAtMs = System.currentTimeMillis();
        Set<String> dependencies = Set.of();

        // When
        TransactionScoringModel.ScoredTransaction scored = scoringModel.scoreTransaction(
                txnId, sender, sizeBytes, gasPrice, createdAtMs, dependencies);

        // Then
        assertNotNull(scored);
        assertEquals(txnId, scored.txnId);
        assertEquals(sender, scored.sender);
        assertTrue(scored.score >= 0 && scored.score <= 1.0,
                "Score should be between 0 and 1");
        assertNotNull(scored.featureScores);
        assertFalse(scored.featureScores.isEmpty(),
                "Feature scores should not be empty");
        assertTrue(scored.featureScores.containsKey("size"));
        assertTrue(scored.featureScores.containsKey("senderHotness"));
        assertTrue(scored.featureScores.containsKey("gasPrice"));
        assertTrue(scored.featureScores.containsKey("age"));
        assertTrue(scored.featureScores.containsKey("dependency"));
    }

    @Test
    @DisplayName("Score batch of transactions should return ordered list")
    void testScoreBatch() {
        // Given
        List<TransactionScoringModel.TransactionData> transactions = new ArrayList<>();
        transactions.add(new TransactionScoringModel.TransactionData(
                "txn-001", "sender-1", 1000, BigDecimal.valueOf(50),
                System.currentTimeMillis(), Set.of()));
        transactions.add(new TransactionScoringModel.TransactionData(
                "txn-002", "sender-2", 500, BigDecimal.valueOf(100),
                System.currentTimeMillis(), Set.of()));
        transactions.add(new TransactionScoringModel.TransactionData(
                "txn-003", "sender-1", 256, BigDecimal.valueOf(200),
                System.currentTimeMillis() - 5000, Set.of())); // Old transaction

        // When
        List<TransactionScoringModel.ScoredTransaction> scored =
                scoringModel.scoreAndOrderBatch(transactions);

        // Then
        assertNotNull(scored);
        assertEquals(3, scored.size());

        // Verify ordering (higher score first)
        for (int i = 0; i < scored.size() - 1; i++) {
            assertTrue(scored.get(i).score >= scored.get(i + 1).score,
                    "Transactions should be ordered by score descending");
        }
    }

    @Test
    @DisplayName("Higher gas price should result in higher or equal score")
    void testGasPriceInfluence() {
        // Given
        long createdAtMs = System.currentTimeMillis();

        // When - Score two transactions with different gas prices
        TransactionScoringModel.ScoredTransaction lowGas = scoringModel.scoreTransaction(
                "txn-low", "sender", 256, BigDecimal.valueOf(10),
                createdAtMs, Set.of());

        TransactionScoringModel.ScoredTransaction highGas = scoringModel.scoreTransaction(
                "txn-high", "sender", 256, BigDecimal.valueOf(500),
                createdAtMs, Set.of());

        // Then - highGas should have higher gas score feature
        Double highGasFeature = highGas.featureScores.get("gasPrice");
        Double lowGasFeature = lowGas.featureScores.get("gasPrice");
        assertNotNull(highGasFeature);
        assertNotNull(lowGasFeature);
        assertTrue(highGasFeature >= lowGasFeature,
                "Higher gas price should result in higher gas score");
    }

    @Test
    @DisplayName("Smaller transaction size should result in higher size score")
    void testSizePenalty() {
        // Given
        long createdAtMs = System.currentTimeMillis();

        // When - Score two transactions with different sizes
        TransactionScoringModel.ScoredTransaction smallTxn = scoringModel.scoreTransaction(
                "txn-small", "sender", 100, BigDecimal.valueOf(100),
                createdAtMs, Set.of());

        TransactionScoringModel.ScoredTransaction largeTxn = scoringModel.scoreTransaction(
                "txn-large", "sender", 5000, BigDecimal.valueOf(100),
                createdAtMs, Set.of());

        // Then - smallTxn should have higher size score
        Double smallSize = smallTxn.featureScores.get("size");
        Double largeSize = largeTxn.featureScores.get("size");
        assertNotNull(smallSize);
        assertNotNull(largeSize);
        assertTrue(smallSize >= largeSize,
                "Smaller transactions should score higher");
    }

    @Test
    @DisplayName("Older transactions should have higher age score")
    void testAgeBoost() {
        // Given
        long now = System.currentTimeMillis();

        // When - Score new and old transactions
        TransactionScoringModel.ScoredTransaction newTxn = scoringModel.scoreTransaction(
                "txn-new", "sender", 256, BigDecimal.valueOf(100),
                now, Set.of());

        TransactionScoringModel.ScoredTransaction oldTxn = scoringModel.scoreTransaction(
                "txn-old", "sender", 256, BigDecimal.valueOf(100),
                now - 6000, Set.of()); // 6 seconds old

        // Then - oldTxn should have higher age score
        Double newAge = newTxn.featureScores.get("age");
        Double oldAge = oldTxn.featureScores.get("age");
        assertNotNull(newAge);
        assertNotNull(oldAge);
        assertTrue(oldAge >= newAge,
                "Older transactions should have higher age score");
    }

    @Test
    @DisplayName("Repeated sender should have higher hotness score over time")
    void testSenderHotness() {
        // Given
        String hotSender = "hot-sender";
        long createdAtMs = System.currentTimeMillis();

        // When - Score multiple transactions from same sender
        TransactionScoringModel.ScoredTransaction txn1 = scoringModel.scoreTransaction(
                "txn-1", hotSender, 256, BigDecimal.valueOf(100),
                createdAtMs, Set.of());

        TransactionScoringModel.ScoredTransaction txn2 = scoringModel.scoreTransaction(
                "txn-2", hotSender, 256, BigDecimal.valueOf(100),
                createdAtMs + 10, Set.of());

        TransactionScoringModel.ScoredTransaction txn3 = scoringModel.scoreTransaction(
                "txn-3", hotSender, 256, BigDecimal.valueOf(100),
                createdAtMs + 20, Set.of());

        // Then - Hotness should increase over time
        Double hotness1 = txn1.featureScores.get("senderHotness");
        Double hotness2 = txn2.featureScores.get("senderHotness");
        Double hotness3 = txn3.featureScores.get("senderHotness");

        assertNotNull(hotness1);
        assertNotNull(hotness2);
        assertNotNull(hotness3);
        assertTrue(hotness3 >= hotness2 && hotness2 >= hotness1,
                "Sender hotness should increase with repeated transactions");
    }

    @Test
    @DisplayName("Fewer dependencies should result in higher dependency score")
    void testDependencyInfluence() {
        // Given
        long createdAtMs = System.currentTimeMillis();

        // When - Score transactions with different dependency counts
        TransactionScoringModel.ScoredTransaction noDeps = scoringModel.scoreTransaction(
                "txn-independent", "sender", 256, BigDecimal.valueOf(100),
                createdAtMs, Set.of());

        TransactionScoringModel.ScoredTransaction withDeps = scoringModel.scoreTransaction(
                "txn-dependent", "sender", 256, BigDecimal.valueOf(100),
                createdAtMs, Set.of("dep-1", "dep-2", "dep-3"));

        // Then - noDeps should have higher dependency score
        Double noDepsScore = noDeps.featureScores.get("dependency");
        Double withDepsScore = withDeps.featureScores.get("dependency");
        assertNotNull(noDepsScore);
        assertNotNull(withDepsScore);
        assertTrue(noDepsScore >= withDepsScore,
                "Transactions with fewer dependencies should score higher");
    }

    @Test
    @DisplayName("Get model weights should return all weights")
    void testGetModelWeights() {
        // When
        Map<String, Double> weights = scoringModel.getModelWeights();

        // Then
        assertNotNull(weights);
        assertTrue(weights.containsKey("size"));
        assertTrue(weights.containsKey("senderHotness"));
        assertTrue(weights.containsKey("gasPrice"));
        assertTrue(weights.containsKey("age"));
        assertTrue(weights.containsKey("dependency"));

        // Verify weights sum to reasonable value
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        assertTrue(sum > 0.5 && sum < 2.0, "Weights should be in reasonable range");
    }

    @Test
    @DisplayName("Get statistics should return current metrics")
    void testGetStatistics() {
        // Given
        scoringModel.scoreTransaction("txn-1", "sender-1", 256,
                BigDecimal.valueOf(100), System.currentTimeMillis(), Set.of());

        // When
        Map<String, Object> stats = scoringModel.getStatistics();

        // Then
        assertNotNull(stats);
        assertTrue(stats.containsKey("transactionsScored"));
        assertTrue(stats.containsKey("batchesProcessed"));
        assertTrue(stats.containsKey("sendersCached"));
        assertTrue((Long) stats.get("transactionsScored") > 0);
    }

    @Test
    @DisplayName("Batch ordering should maintain order consistency")
    void testBatchGrouping() {
        // Given
        List<TransactionScoringModel.TransactionData> transactions = new ArrayList<>();
        String hotSender = "hot-sender";

        // Add transactions from hot and cold senders
        for (int i = 0; i < 15; i++) {
            transactions.add(new TransactionScoringModel.TransactionData(
                    "txn-hot-" + i, hotSender, 256, BigDecimal.valueOf(100),
                    System.currentTimeMillis() - (100 - i * 10), Set.of()));
        }

        for (int i = 0; i < 10; i++) {
            transactions.add(new TransactionScoringModel.TransactionData(
                    "txn-cold-" + i, "cold-sender-" + i, 256, BigDecimal.valueOf(100),
                    System.currentTimeMillis(), Set.of()));
        }

        // When
        List<TransactionScoringModel.ScoredTransaction> ordered =
                scoringModel.scoreAndOrderBatch(transactions);

        // Then
        assertNotNull(ordered);
        assertEquals(25, ordered.size());

        // Verify all hot sender transactions are present
        long hotSenderCount = ordered.stream()
                .filter(t -> t.sender.equals(hotSender))
                .count();
        assertEquals(15, hotSenderCount);
    }
}
