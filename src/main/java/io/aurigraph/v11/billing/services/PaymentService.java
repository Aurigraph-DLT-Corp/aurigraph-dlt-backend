package io.aurigraph.v11.billing.services;

import io.aurigraph.v11.billing.models.Billing;
import io.aurigraph.v11.billing.models.Payment;
import io.aurigraph.v11.billing.repositories.PaymentRepository;
import io.aurigraph.v11.billing.repositories.BillingRepository;
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
public class PaymentService {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    BillingRepository billingRepository;

    @Inject
    StripeIntegrationService stripeService;

    public Payment processPayment(
        Long subscriptionId,
        Long billingId,
        String userId,
        BigDecimal amount,
        Payment.PaymentMethod paymentMethod,
        String stripePaymentMethodId
    ) throws Exception {
        Log.infof("Processing payment for billing: %d, amount: %s", billingId, amount);

        Billing billing = billingRepository.findById(billingId);
        if (billing == null) {
            throw new IllegalArgumentException("Billing not found");
        }

        // Create Stripe payment intent
        String stripeIntentId = stripeService.createPaymentIntent(
            userId,
            amount,
            "USD"
        );

        // Create payment record
        Payment payment = Payment.builder()
            .subscriptionId(subscriptionId)
            .billingId(billingId)
            .userId(userId)
            .amount(amount)
            .currency("USD")
            .status(Payment.PaymentStatus.PENDING)
            .paymentMethod(paymentMethod)
            .paymentGateway(Payment.PaymentGateway.STRIPE)
            .stripePaymentIntentId(stripeIntentId)
            .processingFee(amount.multiply(BigDecimal.valueOf(0.029)).add(BigDecimal.valueOf(0.30)))
            .build();

        paymentRepository.persist(payment);

        // Confirm payment in Stripe
        try {
            String chargeId = stripeService.confirmPayment(
                stripeIntentId,
                stripePaymentMethodId
            );
            payment.stripeChargeId = chargeId;
            payment.status = Payment.PaymentStatus.PROCESSING;
            paymentRepository.persist(payment);
        } catch (Exception e) {
            Log.errorf(e, "Payment processing failed");
            payment.status = Payment.PaymentStatus.FAILED;
            payment.errorMessage = e.getMessage();
            paymentRepository.persist(payment);
            throw e;
        }

        return payment;
    }

    public void completePayment(String stripeChargeId, String chargeStatus) throws Exception {
        Log.infof("Completing payment: %s with status: %s", stripeChargeId, chargeStatus);

        Optional<Payment> paymentOpt = paymentRepository.findByStripeChargeId(stripeChargeId);
        if (paymentOpt.isEmpty()) {
            throw new IllegalArgumentException("Payment not found");
        }

        Payment payment = paymentOpt.get();

        if ("succeeded".equals(chargeStatus)) {
            payment.status = Payment.PaymentStatus.COMPLETED;
            payment.processedAt = LocalDateTime.now();

            // Update billing record
            Billing billing = billingRepository.findById(payment.billingId);
            if (billing != null) {
                billing.status = Billing.BillingStatus.PAID;
                billing.paidDate = LocalDateTime.now();
                billingRepository.persist(billing);
            }
        } else if ("failed".equals(chargeStatus)) {
            payment.status = Payment.PaymentStatus.FAILED;
            payment.errorCode = "payment_failed";
            payment.errorMessage = "Payment declined by issuer";
        }

        paymentRepository.persist(payment);
    }

    public void refundPayment(Long paymentId, String reason) throws Exception {
        Log.infof("Refunding payment: %d, reason: %s", paymentId, reason);

        Payment payment = paymentRepository.findById(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found");
        }

        if (!payment.canBeRefunded()) {
            throw new IllegalArgumentException("Payment cannot be refunded");
        }

        // Process refund in Stripe
        String refundId = stripeService.refundPayment(
            payment.stripeChargeId,
            payment.amount,
            reason
        );

        // Update payment
        payment.status = Payment.PaymentStatus.REFUNDED;
        payment.refundReason = reason;
        payment.refundAmount = payment.amount;
        payment.refundedAt = LocalDateTime.now();
        payment.updatedAt = LocalDateTime.now();

        paymentRepository.persist(payment);

        // Update billing
        Billing billing = billingRepository.findById(payment.billingId);
        if (billing != null) {
            billing.status = Billing.BillingStatus.REFUNDED;
            billingRepository.persist(billing);
        }

        Log.infof("Payment refunded successfully: %s", refundId);
    }

    public Optional<Payment> getPaymentById(Long paymentId) {
        return paymentRepository.findByIdOptional(paymentId);
    }

    public List<Payment> getUserPayments(String userId) {
        return paymentRepository.findByUserId(userId);
    }

    public List<Payment> getCompletedPayments() {
        return paymentRepository.findCompletedPayments();
    }

    public List<Payment> getFailedPayments() {
        return paymentRepository.findFailedPayments();
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findPendingPayments();
    }

    public List<Payment> getPaymentsNeedingRetry() {
        return paymentRepository.findPaymentsNeedingRetry();
    }

    public void retryFailedPayment(Long paymentId) throws Exception {
        Log.infof("Retrying failed payment: %d", paymentId);

        Payment payment = paymentRepository.findById(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found");
        }

        if (payment.status != Payment.PaymentStatus.FAILED) {
            throw new IllegalArgumentException("Payment is not in failed status");
        }

        payment.status = Payment.PaymentStatus.PENDING;
        payment.updatedAt = LocalDateTime.now();
        paymentRepository.persist(payment);

        // Re-process payment
        try {
            String chargeId = stripeService.confirmPayment(
                payment.stripePaymentIntentId,
                null
            );
            payment.stripeChargeId = chargeId;
            payment.status = Payment.PaymentStatus.PROCESSING;
            paymentRepository.persist(payment);
        } catch (Exception e) {
            Log.errorf(e, "Retry failed");
            payment.status = Payment.PaymentStatus.FAILED;
            payment.errorMessage = e.getMessage();
            paymentRepository.persist(payment);
            throw e;
        }
    }

    public BigDecimal getTotalCollectedByUserId(String userId) {
        return paymentRepository.sumCompletedPaymentsByUserId(userId);
    }

    public long getTotalCompletedPayments() {
        return paymentRepository.countCompletedPayments();
    }

    public long getTotalFailedPayments() {
        return paymentRepository.countFailedPayments();
    }

    public List<Payment> getRecentPayments(int limit) {
        return paymentRepository.findRecentPayments(limit);
    }
}
