package io.aurigraph.v11.billing.resources;

import io.aurigraph.v11.billing.models.Payment;
import io.aurigraph.v11.billing.services.PaymentService;
import io.aurigraph.v11.billing.dto.ProcessPaymentRequest;
import io.aurigraph.v11.billing.dto.PaymentResponse;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/v11/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Inject
    PaymentService paymentService;

    @POST
    public Response processPayment(ProcessPaymentRequest request) {
        try {
            Log.infof("POST /payments - Processing payment for billing: %d", request.billingId);

            Payment payment = paymentService.processPayment(
                request.subscriptionId,
                request.billingId,
                request.userId,
                request.amount,
                request.paymentMethod,
                request.stripePaymentMethodId
            );

            return Response.status(Response.Status.CREATED)
                .entity(toResponse(payment))
                .build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to process payment");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getPayment(@PathParam("id") Long paymentId) {
        try {
            var payment = paymentService.getPaymentById(paymentId);
            if (payment.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Payment not found"))
                    .build();
            }

            return Response.ok(toResponse(payment.get())).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get payment");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/user/{userId}")
    public Response getUserPayments(@PathParam("userId") String userId) {
        try {
            List<PaymentResponse> payments = paymentService
                .getUserPayments(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

            return Response.ok(payments).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get user payments");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/completed")
    public Response getCompletedPayments() {
        try {
            List<PaymentResponse> payments = paymentService
                .getCompletedPayments()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

            return Response.ok(payments).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get completed payments");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/failed")
    public Response getFailedPayments() {
        try {
            List<PaymentResponse> payments = paymentService
                .getFailedPayments()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

            return Response.ok(payments).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get failed payments");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{id}/refund")
    public Response refundPayment(
        @PathParam("id") Long paymentId,
        @QueryParam("reason") String reason
    ) {
        try {
            Log.infof("POST /payments/{%d}/refund", paymentId);

            paymentService.refundPayment(paymentId, reason);

            return Response.ok(new SuccessResponse("Payment refunded successfully")).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to refund payment");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{id}/retry")
    public Response retryPayment(@PathParam("id") Long paymentId) {
        try {
            Log.infof("POST /payments/{%d}/retry", paymentId);

            paymentService.retryFailedPayment(paymentId);

            return Response.ok(new SuccessResponse("Payment retry initiated")).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to retry payment");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/webhook/stripe")
    @Consumes("application/json")
    public Response handleStripeWebhook(String payload) {
        try {
            Log.infof("POST /payments/webhook/stripe - Processing webhook");

            // TODO: Implement webhook signature verification
            // TODO: Parse event and route to appropriate handler

            return Response.ok(new SuccessResponse("Webhook processed")).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to process webhook");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.id,
            payment.subscriptionId,
            payment.billingId,
            payment.userId,
            payment.amount,
            payment.currency,
            payment.status.name(),
            payment.paymentMethod.name(),
            payment.paymentGateway.name(),
            payment.lastFourDigits,
            payment.cardBrand,
            payment.errorMessage,
            payment.createdAt,
            payment.processedAt
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
