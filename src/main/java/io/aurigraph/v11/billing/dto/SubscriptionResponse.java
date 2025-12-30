package io.aurigraph.v11.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SubscriptionResponse {
    public Long id;
    public String userId;
    public String planName;
    public String status;
    public String billingCycle;
    public BigDecimal currentPrice;
    public LocalDateTime startDate;
    public LocalDateTime endDate;
    public LocalDateTime renewalDate;
    public LocalDateTime trialEndDate;
    public Boolean autoRenew;
    public String discountCode;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public SubscriptionResponse(
        Long id,
        String userId,
        String planName,
        String status,
        String billingCycle,
        BigDecimal currentPrice,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime renewalDate,
        LocalDateTime trialEndDate,
        Boolean autoRenew,
        String discountCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.planName = planName;
        this.status = status;
        this.billingCycle = billingCycle;
        this.currentPrice = currentPrice;
        this.startDate = startDate;
        this.endDate = endDate;
        this.renewalDate = renewalDate;
        this.trialEndDate = trialEndDate;
        this.autoRenew = autoRenew;
        this.discountCode = discountCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
