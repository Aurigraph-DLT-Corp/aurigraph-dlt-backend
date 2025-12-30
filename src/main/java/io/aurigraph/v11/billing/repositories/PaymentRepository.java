package io.aurigraph.v11.billing.repositories;

import io.aurigraph.v11.billing.models.Payment;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PaymentRepository implements PanacheRepository<Payment> {

    public List<Payment> findByUserId(String userId) {
        return list("userId", userId);
    }

    public List<Payment> findBySubscriptionId(Long subscriptionId) {
        return list("subscriptionId", subscriptionId);
    }

    public List<Payment> findByBillingId(Long billingId) {
        return list("billingId", billingId);
    }

    public List<Payment> findByStatus(Payment.PaymentStatus status) {
        return list("status", status);
    }

    public List<Payment> findCompletedPayments() {
        return list("status", Payment.PaymentStatus.COMPLETED);
    }

    public List<Payment> findFailedPayments() {
        return list("status", Payment.PaymentStatus.FAILED);
    }

    public List<Payment> findPendingPayments() {
        return list("status", Payment.PaymentStatus.PENDING);
    }

    public List<Payment> findRefundedPayments() {
        return list("status", Payment.PaymentStatus.REFUNDED);
    }

    public List<Payment> findByPaymentMethod(Payment.PaymentMethod method) {
        return list("paymentMethod", method);
    }

    public List<Payment> findByPaymentGateway(Payment.PaymentGateway gateway) {
        return list("paymentGateway", gateway);
    }

    public List<Payment> findByCreatedAtRange(LocalDateTime startDate, LocalDateTime endDate) {
        return list(
            "createdAt >= ?1 and createdAt <= ?2",
            startDate,
            endDate
        );
    }

    public List<Payment> findByUserIdAndDateRange(
        String userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return list(
            "userId = ?1 and createdAt >= ?2 and createdAt <= ?3",
            userId,
            startDate,
            endDate
        );
    }

    public Optional<Payment> findByStripePaymentIntentId(String intentId) {
        return find("stripePaymentIntentId", intentId).firstResultOptional();
    }

    public Optional<Payment> findByStripeChargeId(String chargeId) {
        return find("stripeChargeId", chargeId).firstResultOptional();
    }

    public Optional<Payment> findByPaypalTransactionId(String txId) {
        return find("paypalTransactionId", txId).firstResultOptional();
    }

    public Optional<Payment> findByGatewayTransactionId(String txId) {
        return find("gatewayTransactionId", txId).firstResultOptional();
    }

    public List<Payment> findFailedPaymentsByUserId(String userId) {
        return list("userId = ?1 and status = ?2", userId, Payment.PaymentStatus.FAILED);
    }

    public List<Payment> findPendingPaymentsByUserId(String userId) {
        return list("userId = ?1 and status = ?2", userId, Payment.PaymentStatus.PENDING);
    }

    public BigDecimal sumCompletedPaymentsByUserId(String userId) {
        BigDecimal sum = find(
            "select sum(amount) from Payment where userId = ?1 and status = ?2",
            userId,
            Payment.PaymentStatus.COMPLETED
        ).project(BigDecimal.class).firstResultOptional().orElse(BigDecimal.ZERO);
        return sum;
    }

    public long countCompletedPayments() {
        return count("status", Payment.PaymentStatus.COMPLETED);
    }

    public long countFailedPayments() {
        return count("status", Payment.PaymentStatus.FAILED);
    }

    public long countPendingPayments() {
        return count("status", Payment.PaymentStatus.PENDING);
    }

    public List<Payment> findRecentPayments(int limit) {
        return find("status = ?1 order by createdAt desc", Payment.PaymentStatus.COMPLETED)
            .page(0, limit)
            .list();
    }

    public List<Payment> findRefundablePayments() {
        return list(
            "status = ?1 and createdAt > ?2",
            Payment.PaymentStatus.COMPLETED,
            LocalDateTime.now().minusDays(90)
        );
    }

    public List<Payment> findUnprocessedPayments() {
        return list(
            "status in ?1",
            List.of(
                Payment.PaymentStatus.PENDING,
                Payment.PaymentStatus.PROCESSING
            )
        );
    }

    public List<Payment> findPaymentsNeedingRetry() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        return list(
            "status = ?1 and updatedAt < ?2",
            Payment.PaymentStatus.FAILED,
            cutoff
        );
    }
}
