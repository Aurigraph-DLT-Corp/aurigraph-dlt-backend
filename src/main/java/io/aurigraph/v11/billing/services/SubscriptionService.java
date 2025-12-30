package io.aurigraph.v11.billing.services;

import io.aurigraph.v11.billing.models.Plan;
import io.aurigraph.v11.billing.models.Subscription;
import io.aurigraph.v11.billing.repositories.SubscriptionRepository;
import io.aurigraph.v11.billing.repositories.PlanRepository;
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
public class SubscriptionService {

    @Inject
    SubscriptionRepository subscriptionRepository;

    @Inject
    PlanRepository planRepository;

    @Inject
    BillingService billingService;

    @Inject
    StripeIntegrationService stripeService;

    public Subscription createSubscription(
        String userId,
        Long planId,
        Subscription.BillingCycle billingCycle,
        String stripeCustomerId,
        String paymentMethodId
    ) throws Exception {
        Log.infof("Creating subscription for user: %s with plan: %d", userId, planId);

        Plan plan = planRepository.findById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planId);
        }

        if (!plan.isActive) {
            throw new IllegalArgumentException("Plan is not active");
        }

        // Check if user already has active subscription
        List<Subscription> existingActive = subscriptionRepository
            .findActiveSubscriptionsByUserId(userId);
        if (!existingActive.isEmpty()) {
            throw new IllegalArgumentException("User already has active subscription");
        }

        Subscription subscription = Subscription.builder()
            .userId(userId)
            .plan(plan)
            .status(Subscription.SubscriptionStatus.TRIAL)
            .billingCycle(billingCycle)
            .startDate(LocalDateTime.now())
            .trialEndDate(LocalDateTime.now().plusDays(plan.trialDays != null ? plan.trialDays : 14))
            .stripeCustomerId(stripeCustomerId)
            .paymentMethodId(paymentMethodId)
            .autoRenew(true)
            .build();

        // Create Stripe subscription
        String stripeSubId = stripeService.createSubscription(
            stripeCustomerId,
            plan.id,
            billingCycle
        );
        subscription.stripeSubscriptionId = stripeSubId;

        // Set current price based on billing cycle
        BigDecimal price = billingCycle == Subscription.BillingCycle.ANNUAL
            ? plan.annualPrice
            : plan.monthlyPrice;
        subscription.currentPrice = price;

        subscriptionRepository.persist(subscription);
        Log.infof("Subscription created successfully: %d", subscription.id);

        return subscription;
    }

    public Subscription upgradeSubscription(Long subscriptionId, Long newPlanId) throws Exception {
        Log.infof("Upgrading subscription: %d to plan: %d", subscriptionId, newPlanId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription not found");
        }

        Plan newPlan = planRepository.findById(newPlanId);
        if (newPlan == null) {
            throw new IllegalArgumentException("Plan not found: " + newPlanId);
        }

        Plan oldPlan = subscription.plan;

        // Update Stripe subscription
        stripeService.updateSubscription(
            subscription.stripeSubscriptionId,
            newPlan.id,
            subscription.billingCycle
        );

        // Update subscription
        subscription.plan = newPlan;
        BigDecimal price = subscription.billingCycle == Subscription.BillingCycle.ANNUAL
            ? newPlan.annualPrice
            : newPlan.monthlyPrice;
        subscription.currentPrice = price;
        subscription.updatedAt = LocalDateTime.now();

        subscriptionRepository.persist(subscription);

        // Log upgrade in billing service
        try {
            billingService.logSubscriptionChange(subscriptionId, "UPGRADE", oldPlan.id, newPlan.id);
        } catch (Exception e) {
            Log.warnf("Failed to log subscription upgrade: %s", e.getMessage());
        }

        Log.infof("Subscription upgraded successfully");
        return subscription;
    }

    public Subscription downgradeSubscription(Long subscriptionId, Long newPlanId) throws Exception {
        Log.infof("Downgrading subscription: %d to plan: %d", subscriptionId, newPlanId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription not found");
        }

        Plan newPlan = planRepository.findById(newPlanId);
        if (newPlan == null) {
            throw new IllegalArgumentException("Plan not found");
        }

        // Schedule downgrade at next billing period
        stripeService.scheduleSubscriptionChange(
            subscription.stripeSubscriptionId,
            newPlan.id,
            subscription.renewalDate
        );

        subscription.plan = newPlan;
        subscription.updatedAt = LocalDateTime.now();

        subscriptionRepository.persist(subscription);
        Log.infof("Subscription downgrade scheduled");
        return subscription;
    }

    public void cancelSubscription(Long subscriptionId, String reason) throws Exception {
        Log.infof("Cancelling subscription: %d, reason: %s", subscriptionId, reason);

        Subscription subscription = subscriptionRepository.findById(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription not found");
        }

        // Cancel in Stripe
        stripeService.cancelSubscription(subscription.stripeSubscriptionId);

        // Update subscription
        subscription.status = Subscription.SubscriptionStatus.CANCELLED;
        subscription.cancelledAt = LocalDateTime.now();
        subscription.cancellationReason = reason;
        subscription.updatedAt = LocalDateTime.now();

        subscriptionRepository.persist(subscription);
        Log.infof("Subscription cancelled successfully");
    }

    public Optional<Subscription> getSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findByIdOptional(subscriptionId);
    }

    public Optional<Subscription> getActiveSubscriptionByUserId(String userId) {
        return subscriptionRepository.findActiveSubscriptionsByUserId(userId)
            .stream()
            .findFirst();
    }

    public List<Subscription> getUserSubscriptions(String userId) {
        return subscriptionRepository.findAllByUserId(userId);
    }

    public List<Subscription> getActiveSubscriptions() {
        return subscriptionRepository.findActiveSubscriptions();
    }

    public List<Subscription> getSubscriptionsNeedingRenewal() {
        return subscriptionRepository.findSubscriptionsNeedingRenewal(LocalDateTime.now().plusDays(3));
    }

    public long getActiveSubscriptionCount() {
        return subscriptionRepository.countActiveSubscriptions();
    }

    public void renewSubscription(Long subscriptionId) throws Exception {
        Log.infof("Renewing subscription: %d", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription not found");
        }

        // Charge for renewal
        billingService.chargeSubscription(subscriptionId);

        // Update renewal date
        subscription.renewalDate = subscription.renewalDate.plusMonths(1);
        subscription.endDate = subscription.renewalDate.plusMonths(1);
        subscription.updatedAt = LocalDateTime.now();

        subscriptionRepository.persist(subscription);
        Log.infof("Subscription renewed successfully");
    }

    public void pauseSubscription(Long subscriptionId) throws Exception {
        Log.infof("Pausing subscription: %d", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription not found");
        }

        subscription.status = Subscription.SubscriptionStatus.PAUSED;
        subscription.updatedAt = LocalDateTime.now();

        subscriptionRepository.persist(subscription);
        Log.infof("Subscription paused");
    }

    public void resumeSubscription(Long subscriptionId) throws Exception {
        Log.infof("Resuming subscription: %d", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription not found");
        }

        subscription.status = Subscription.SubscriptionStatus.ACTIVE;
        subscription.updatedAt = LocalDateTime.now();

        subscriptionRepository.persist(subscription);
        Log.infof("Subscription resumed");
    }

    public void applyDiscount(Long subscriptionId, String discountCode, BigDecimal discountPercentage) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription not found");
        }

        subscription.discountCode = discountCode;
        subscription.discountPercentage = discountPercentage;
        subscription.updatedAt = LocalDateTime.now();

        subscriptionRepository.persist(subscription);
        Log.infof("Discount applied to subscription: %s", discountCode);
    }
}
