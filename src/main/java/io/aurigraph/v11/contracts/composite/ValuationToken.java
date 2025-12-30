package io.aurigraph.v11.contracts.composite;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Valuation Token (ERC-20) - Tracks asset valuations and price history
 */
public class ValuationToken extends SecondaryToken {
    private BigDecimal currentValue;
    private List<PricePoint> priceHistory;

    public ValuationToken(String tokenId, String compositeId, BigDecimal currentValue,
                         List<PricePoint> priceHistory) {
        super(tokenId, compositeId, SecondaryTokenType.VALUATION);
        this.currentValue = currentValue;
        this.priceHistory = new ArrayList<>(priceHistory);
    }

    @Override
    public void updateData(Map<String, Object> updateData) {
        if (updateData.containsKey("currentValue")) {
            BigDecimal newValue = (BigDecimal) updateData.get("currentValue");
            updateValue(newValue);
        }
        this.lastUpdated = Instant.now();
        this.data.putAll(updateData);
    }

    public void updateValue(BigDecimal newValue) {
        // Add current value to history
        if (currentValue != null && currentValue.compareTo(BigDecimal.ZERO) > 0) {
            priceHistory.add(new PricePoint(currentValue, Instant.now()));
        }
        this.currentValue = newValue;
        this.lastUpdated = Instant.now();
    }

    public BigDecimal getValueChange(int daysBack) {
        if (priceHistory.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        Instant cutoff = Instant.now().minusSeconds(daysBack * 24 * 60 * 60);
        Optional<PricePoint> historicalPrice = priceHistory.stream()
            .filter(point -> point.getTimestamp().isAfter(cutoff))
            .min((p1, p2) -> p1.getTimestamp().compareTo(p2.getTimestamp()));
        
        if (historicalPrice.isPresent() && currentValue != null) {
            return currentValue.subtract(historicalPrice.get().getPrice());
        }
        
        return BigDecimal.ZERO;
    }

    // Getters
    public BigDecimal getCurrentValue() { return currentValue; }
    public List<PricePoint> getPriceHistory() { return List.copyOf(priceHistory); }

    /**
     * Price point in valuation history
     */
    public static class PricePoint {
        private final BigDecimal price;
        private final Instant timestamp;

        public PricePoint(BigDecimal price, Instant timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }

        public BigDecimal getPrice() { return price; }
        public Instant getTimestamp() { return timestamp; }
    }
}