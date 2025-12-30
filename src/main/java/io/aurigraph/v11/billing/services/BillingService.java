package io.aurigraph.v11.billing.services;

import io.aurigraph.v11.billing.models.Billing;
import io.aurigraph.v11.billing.models.Invoice;
import io.aurigraph.v11.billing.models.Subscription;
import io.aurigraph.v11.billing.repositories.BillingRepository;
import io.aurigraph.v11.billing.repositories.SubscriptionRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class BillingService {

    @Inject
    BillingRepository billingRepository;

    @Inject
    SubscriptionRepository subscriptionRepository;

    @Inject
    InvoiceService invoiceService;

    public Billing chargeSubscription(Long subscriptionId) throws Exception {
        Log.infof("Charging subscription: %d", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription not found");
        }

        if (!subscription.isActive()) {
            throw new IllegalArgumentException("Subscription is not active");
        }

        // Calculate amount with tax and discount
        BigDecimal amount = subscription.currentPrice;
        BigDecimal discount = BigDecimal.ZERO;

        if (subscription.discountPercentage != null && subscription.discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            discount = amount.multiply(subscription.discountPercentage).divide(BigDecimal.valueOf(100));
            amount = amount.subtract(discount);
        }

        BigDecimal taxAmount = calculateTax(amount);
        BigDecimal totalAmount = amount.add(taxAmount);

        // Create billing record
        Billing billing = Billing.builder()
            .subscriptionId(subscriptionId)
            .userId(subscription.userId)
            .amount(amount)
            .taxAmount(taxAmount)
            .totalAmount(totalAmount)
            .chargeType(Billing.ChargeType.SUBSCRIPTION)
            .status(Billing.BillingStatus.PENDING)
            .billingDate(LocalDateTime.now())
            .paymentMethod(subscription.paymentMethodId)
            .description("Subscription charge for plan: " + subscription.plan.name)
            .build();

        billingRepository.persist(billing);

        // Create invoice
        Invoice invoice = invoiceService.createInvoiceFromBilling(billing, subscription);

        // Update billing with invoice ID
        billing.invoiceId = invoice.id;
        billingRepository.persist(billing);

        Log.infof("Billing record created: %d for subscription: %d", billing.id, subscriptionId);

        return billing;
    }

    public Billing chargeUsageOverage(
        Long subscriptionId,
        String userId,
        String description,
        BigDecimal amount
    ) throws Exception {
        Log.infof("Charging usage overage for subscription: %d, amount: %s", subscriptionId, amount);

        BigDecimal taxAmount = calculateTax(amount);
        BigDecimal totalAmount = amount.add(taxAmount);

        Billing billing = Billing.builder()
            .subscriptionId(subscriptionId)
            .userId(userId)
            .amount(amount)
            .taxAmount(taxAmount)
            .totalAmount(totalAmount)
            .chargeType(Billing.ChargeType.OVERAGE)
            .status(Billing.BillingStatus.PENDING)
            .billingDate(LocalDateTime.now())
            .description(description)
            .build();

        billingRepository.persist(billing);
        return billing;
    }

    public BigDecimal calculateTax(BigDecimal amount) {
        // Default US tax rate (9.5%)
        return amount.multiply(BigDecimal.valueOf(0.095));
    }

    public Optional<Billing> getBillingById(Long billingId) {
        return billingRepository.findByIdOptional(billingId);
    }

    public List<Billing> getUserBillings(String userId) {
        return billingRepository.findByUserId(userId);
    }

    public List<Billing> getSubscriptionBillings(Long subscriptionId) {
        return billingRepository.findBySubscriptionId(subscriptionId);
    }

    public List<Billing> getPendingBillings() {
        return billingRepository.findPendingBillings();
    }

    public List<Billing> getFailedBillings() {
        return billingRepository.findFailedBillings();
    }

    public List<Billing> getOverdueBillings() {
        return billingRepository.findOverdueBillings();
    }

    public List<Billing> getBillingsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return billingRepository.findByBillingDateRange(startDate, endDate);
    }

    public List<Billing> getUserBillingsByDateRange(
        String userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return billingRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    public BigDecimal getUserTotalBilled(String userId) {
        return billingRepository.sumTotalAmountByUserId(userId);
    }

    public BigDecimal getUserTotalPaid(String userId) {
        return billingRepository.sumPaidAmountByUserId(userId);
    }

    public long getOverdueBillingCountByUserId(String userId) {
        return billingRepository.countOverdueBillingsByUserId(userId);
    }

    public void markBillingAsPaid(Long billingId, LocalDateTime paidDate) {
        Billing billing = billingRepository.findById(billingId);
        if (billing != null) {
            billing.status = Billing.BillingStatus.PAID;
            billing.paidDate = paidDate;
            billing.updatedAt = LocalDateTime.now();
            billingRepository.persist(billing);
            Log.infof("Billing marked as paid: %d", billingId);
        }
    }

    public void markBillingAsFailed(Long billingId, String reason) {
        Billing billing = billingRepository.findById(billingId);
        if (billing != null) {
            billing.status = Billing.BillingStatus.FAILED;
            billing.description = reason;
            billing.updatedAt = LocalDateTime.now();
            billingRepository.persist(billing);
            Log.infof("Billing marked as failed: %d, reason: %s", billingId, reason);
        }
    }

    public void logSubscriptionChange(
        Long subscriptionId,
        String changeType,
        Long oldPlanId,
        Long newPlanId
    ) {
        Log.infof(
            "Subscription change logged - ID: %d, Type: %s, Old Plan: %d, New Plan: %d",
            subscriptionId,
            changeType,
            oldPlanId,
            newPlanId
        );
    }

    public List<Billing> getMonthlyBillings(int year, int month) {
        return billingRepository.findMonthlyBillings(year, month);
    }

    public long getPendingBillingCount() {
        return billingRepository.count("status", Billing.BillingStatus.PENDING);
    }

    public long getFailedBillingCount() {
        return billingRepository.count("status", Billing.BillingStatus.FAILED);
    }
}
