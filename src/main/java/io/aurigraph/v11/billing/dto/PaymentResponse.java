package io.aurigraph.v11.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {
    public Long id;
    public Long subscriptionId;
    public Long billingId;
    public String userId;
    public BigDecimal amount;
    public String currency;
    public String status;
    public String paymentMethod;
    public String paymentGateway;
    public String lastFourDigits;
    public String cardBrand;
    public String errorMessage;
    public LocalDateTime createdAt;
    public LocalDateTime processedAt;

    public PaymentResponse(
        Long id,
        Long subscriptionId,
        Long billingId,
        String userId,
        BigDecimal amount,
        String currency,
        String status,
        String paymentMethod,
        String paymentGateway,
        String lastFourDigits,
        String cardBrand,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime processedAt
    ) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.billingId = billingId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.paymentGateway = paymentGateway;
        this.lastFourDigits = lastFourDigits;
        this.cardBrand = cardBrand;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }
}
