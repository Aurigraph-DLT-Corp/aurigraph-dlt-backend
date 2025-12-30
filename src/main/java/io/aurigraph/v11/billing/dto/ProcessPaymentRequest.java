package io.aurigraph.v11.billing.dto;

import io.aurigraph.v11.billing.models.Payment;
import java.math.BigDecimal;

public class ProcessPaymentRequest {
    public Long subscriptionId;
    public Long billingId;
    public String userId;
    public BigDecimal amount;
    public Payment.PaymentMethod paymentMethod;
    public String stripePaymentMethodId;
}
