package io.aurigraph.v11.billing.repositories;

import io.aurigraph.v11.billing.models.Subscription;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SubscriptionRepository implements PanacheRepository<Subscription> {

    public Optional<Subscription> findByUserId(String userId) {
        return find("userId", userId).firstResultOptional();
    }

    public List<Subscription> findAllByUserId(String userId) {
        return list("userId", userId);
    }

    public List<Subscription> findActiveSubscriptions() {
        return list("status in ?1", List.of(
            Subscription.SubscriptionStatus.ACTIVE,
            Subscription.SubscriptionStatus.TRIAL
        ));
    }

    public List<Subscription> findActiveSubscriptionsByUserId(String userId) {
        return list(
            "userId = ?1 and status in ?2",
            userId,
            List.of(
                Subscription.SubscriptionStatus.ACTIVE,
                Subscription.SubscriptionStatus.TRIAL
            )
        );
    }

    public List<Subscription> findExpiredSubscriptions() {
        return list(
            "endDate < ?1 and status != ?2",
            LocalDateTime.now(),
            Subscription.SubscriptionStatus.CANCELLED
        );
    }

    public List<Subscription> findCancelledSubscriptions() {
        return list("status", Subscription.SubscriptionStatus.CANCELLED);
    }

    public List<Subscription> findPendingPaymentSubscriptions() {
        return list("status", Subscription.SubscriptionStatus.PENDING_PAYMENT);
    }

    public List<Subscription> findSubscriptionsNeedingRenewal(LocalDateTime before) {
        return list(
            "renewalDate <= ?1 and status = ?2 and autoRenew = true",
            before,
            Subscription.SubscriptionStatus.ACTIVE
        );
    }

    public List<Subscription> findSubscriptionsWithPlanId(Long planId) {
        return list("plan.id", planId);
    }

    public long countActiveSubscriptions() {
        return count("status in ?1", List.of(
            Subscription.SubscriptionStatus.ACTIVE,
            Subscription.SubscriptionStatus.TRIAL
        ));
    }

    public long countActiveSubscriptionsByUserId(String userId) {
        return count(
            "userId = ?1 and status in ?2",
            userId,
            List.of(
                Subscription.SubscriptionStatus.ACTIVE,
                Subscription.SubscriptionStatus.TRIAL
            )
        );
    }

    public Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return find("stripeSubscriptionId", stripeSubscriptionId).firstResultOptional();
    }

    public List<Subscription> findByStripeCustomerId(String stripeCustomerId) {
        return list("stripeCustomerId", stripeCustomerId);
    }

    public List<Subscription> findTrialSubscriptions() {
        return list("status", Subscription.SubscriptionStatus.TRIAL);
    }

    public List<Subscription> findTrialEndingSoon(LocalDateTime before) {
        return list(
            "status = ?1 and trialEndDate <= ?2",
            Subscription.SubscriptionStatus.TRIAL,
            before
        );
    }

    public void cancelSubscription(Long subscriptionId, String reason) {
        Subscription sub = findById(subscriptionId);
        if (sub != null) {
            sub.status = Subscription.SubscriptionStatus.CANCELLED;
            sub.cancelledAt = LocalDateTime.now();
            sub.cancellationReason = reason;
            persist(sub);
        }
    }
}
