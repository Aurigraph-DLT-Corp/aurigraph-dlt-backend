package io.aurigraph.v11.billing.repositories;

import io.aurigraph.v11.billing.models.Billing;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class BillingRepository implements PanacheRepository<Billing> {

    public List<Billing> findByUserId(String userId) {
        return list("userId", userId);
    }

    public List<Billing> findBySubscriptionId(Long subscriptionId) {
        return list("subscriptionId", subscriptionId);
    }

    public List<Billing> findByUserIdOrderByBillingDateDesc(String userId) {
        return list("userId", userId);
    }

    public List<Billing> findByStatus(Billing.BillingStatus status) {
        return list("status", status);
    }

    public List<Billing> findPendingBillings() {
        return list("status", Billing.BillingStatus.PENDING);
    }

    public List<Billing> findFailedBillings() {
        return list("status", Billing.BillingStatus.FAILED);
    }

    public List<Billing> findPaidBillings() {
        return list("status", Billing.BillingStatus.PAID);
    }

    public List<Billing> findByBillingDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return list(
            "billingDate >= ?1 and billingDate <= ?2",
            startDate,
            endDate
        );
    }

    public List<Billing> findByUserIdAndDateRange(
        String userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return list(
            "userId = ?1 and billingDate >= ?2 and billingDate <= ?3",
            userId,
            startDate,
            endDate
        );
    }

    public List<Billing> findOverdueBillings() {
        LocalDateTime now = LocalDateTime.now();
        return list(
            "dueDate < ?1 and status != ?2",
            now,
            Billing.BillingStatus.PAID
        );
    }

    public List<Billing> findByChargeType(Billing.ChargeType chargeType) {
        return list("chargeType", chargeType);
    }

    public BigDecimal sumTotalAmountByUserId(String userId) {
        BigDecimal sum = find(
            "select sum(totalAmount) from Billing where userId = ?1",
            userId
        ).project(BigDecimal.class).firstResultOptional().orElse(BigDecimal.ZERO);
        return sum;
    }

    public BigDecimal sumPaidAmountByUserId(String userId) {
        BigDecimal sum = find(
            "select sum(totalAmount) from Billing where userId = ?1 and status = ?2",
            userId,
            Billing.BillingStatus.PAID
        ).project(BigDecimal.class).firstResultOptional().orElse(BigDecimal.ZERO);
        return sum;
    }

    public long countPendingBillingsByUserId(String userId) {
        return count("userId = ?1 and status = ?2", userId, Billing.BillingStatus.PENDING);
    }

    public long countOverdueBillingsByUserId(String userId) {
        LocalDateTime now = LocalDateTime.now();
        return count(
            "userId = ?1 and dueDate < ?2 and status != ?3",
            userId,
            now,
            Billing.BillingStatus.PAID
        );
    }

    public List<Billing> findByStripePaymentIntentId(String intentId) {
        return list("stripePaymentIntentId", intentId);
    }

    public List<Billing> findMonthlyBillings(int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1);
        return list(
            "billingDate >= ?1 and billingDate < ?2",
            startDate,
            endDate
        );
    }

    public List<Billing> findBillingsByInvoiceId(Long invoiceId) {
        return list("invoiceId", invoiceId);
    }
}
