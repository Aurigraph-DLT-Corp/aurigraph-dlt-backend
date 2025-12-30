package io.aurigraph.v11.billing.resources;

import io.aurigraph.v11.billing.models.Subscription;
import io.aurigraph.v11.billing.services.SubscriptionService;
import io.aurigraph.v11.billing.dto.CreateSubscriptionRequest;
import io.aurigraph.v11.billing.dto.SubscriptionResponse;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/v11/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubscriptionResource {

    @Inject
    SubscriptionService subscriptionService;

    @POST
    public Response createSubscription(CreateSubscriptionRequest request) {
        try {
            Log.infof("POST /subscriptions - Creating subscription for user: %s", request.userId);

            Subscription subscription = subscriptionService.createSubscription(
                request.userId,
                request.planId,
                request.billingCycle,
                request.stripeCustomerId,
                request.paymentMethodId
            );

            return Response.status(Response.Status.CREATED)
                .entity(toResponse(subscription))
                .build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to create subscription");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getSubscription(@PathParam("id") Long id) {
        try {
            var subscription = subscriptionService.getSubscriptionById(id);
            if (subscription.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Subscription not found"))
                    .build();
            }

            return Response.ok(toResponse(subscription.get())).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get subscription");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/user/{userId}")
    public Response getUserSubscriptions(@PathParam("userId") String userId) {
        try {
            List<SubscriptionResponse> subscriptions = subscriptionService
                .getUserSubscriptions(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

            return Response.ok(subscriptions).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get user subscriptions");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/user/{userId}/active")
    public Response getActiveSubscription(@PathParam("userId") String userId) {
        try {
            var subscription = subscriptionService.getActiveSubscriptionByUserId(userId);
            if (subscription.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("No active subscription found"))
                    .build();
            }

            return Response.ok(toResponse(subscription.get())).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get active subscription");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{id}/upgrade")
    public Response upgradeSubscription(
        @PathParam("id") Long subscriptionId,
        @QueryParam("planId") Long newPlanId
    ) {
        try {
            Log.infof("POST /subscriptions/{%d}/upgrade to plan {%d}", subscriptionId, newPlanId);

            Subscription subscription = subscriptionService.upgradeSubscription(subscriptionId, newPlanId);

            return Response.ok(toResponse(subscription)).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to upgrade subscription");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{id}/downgrade")
    public Response downgradeSubscription(
        @PathParam("id") Long subscriptionId,
        @QueryParam("planId") Long newPlanId
    ) {
        try {
            Log.infof("POST /subscriptions/{%d}/downgrade to plan {%d}", subscriptionId, newPlanId);

            Subscription subscription = subscriptionService.downgradeSubscription(subscriptionId, newPlanId);

            return Response.ok(toResponse(subscription)).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to downgrade subscription");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{id}/cancel")
    public Response cancelSubscription(
        @PathParam("id") Long subscriptionId,
        @QueryParam("reason") String reason
    ) {
        try {
            Log.infof("POST /subscriptions/{%d}/cancel", subscriptionId);

            subscriptionService.cancelSubscription(subscriptionId, reason);

            return Response.ok(new SuccessResponse("Subscription cancelled successfully")).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to cancel subscription");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{id}/pause")
    public Response pauseSubscription(@PathParam("id") Long subscriptionId) {
        try {
            Log.infof("POST /subscriptions/{%d}/pause", subscriptionId);

            subscriptionService.pauseSubscription(subscriptionId);

            return Response.ok(new SuccessResponse("Subscription paused")).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to pause subscription");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{id}/resume")
    public Response resumeSubscription(@PathParam("id") Long subscriptionId) {
        try {
            Log.infof("POST /subscriptions/{%d}/resume", subscriptionId);

            subscriptionService.resumeSubscription(subscriptionId);

            return Response.ok(new SuccessResponse("Subscription resumed")).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to resume subscription");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    public Response getActiveSubscriptions() {
        try {
            Log.infof("GET /subscriptions - Fetching all active subscriptions");

            List<SubscriptionResponse> subscriptions = subscriptionService
                .getActiveSubscriptions()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

            return Response.ok(subscriptions).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get active subscriptions");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
            subscription.id,
            subscription.userId,
            subscription.plan.name,
            subscription.status.name(),
            subscription.billingCycle.name(),
            subscription.currentPrice,
            subscription.startDate,
            subscription.endDate,
            subscription.renewalDate,
            subscription.trialEndDate,
            subscription.autoRenew,
            subscription.discountCode,
            subscription.createdAt,
            subscription.updatedAt
        );
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    public static class SuccessResponse {
        public String message;

        public SuccessResponse(String message) {
            this.message = message;
        }
    }
}
