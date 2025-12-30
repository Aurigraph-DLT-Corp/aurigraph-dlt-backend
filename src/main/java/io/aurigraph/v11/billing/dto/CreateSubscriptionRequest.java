package io.aurigraph.v11.billing.dto;

import io.aurigraph.v11.billing.models.Subscription;

public class CreateSubscriptionRequest {
    public String userId;
    public Long planId;
    public Subscription.BillingCycle billingCycle;
    public String stripeCustomerId;
    public String paymentMethodId;
}
