package io.aurigraph.v11.bridge.services;

import io.aurigraph.v11.bridge.models.TransferRequest;
import io.aurigraph.v11.bridge.models.TransferResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Bridge Transfer Service
 * Orchestrates multi-signature bridge transfers with state management
 * Supports M-of-N multi-sig schemes (e.g., 2-of-3, 3-of-5)
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 */
@ApplicationScoped
public class BridgeTransferService {

    private static final Logger LOG = Logger.getLogger(BridgeTransferService.class);

    @Inject
    MultiSignatureValidator multiSigValidator;

    @Inject
    TransferStateManager stateManager;

    /**
     * Submit a new bridge transfer
     */
    public TransferResponse submitBridgeTransfer(TransferRequest request) {
        LOG.infof("Submitting bridge transfer: %s", request.getTransferId());

        // Validate request
        java.util.List<String> validationErrors = request.getValidationErrors();
        if (!validationErrors.isEmpty()) {
            LOG.warnf("Transfer validation failed with %d errors", validationErrors.size());
            return TransferResponse.builder()
                    .transferId(request.getTransferId())
                    .status(TransferResponse.TransferStatus.FAILED)
                    .errorCode("VALIDATION_FAILED")
                    .errorDetails("Transfer validation failed: " + String.join(", ", validationErrors))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        // Initialize transfer in PENDING state
        TransferResponse response = stateManager.initializeTransfer(
                request.getTransferId(),
                request.getSourceChain(),
                request.getTargetChain(),
                request.getTokenSymbol(),
                request.getAmount()
        );

        // Validate multi-signatures
        MultiSignatureValidator.MultiSigValidationResult multiSigResult =
                multiSigValidator.validateMultiSignatures(request);

        LOG.infof("Multi-sig validation result: valid=%b, received=%d, required=%d",
                 multiSigResult.getIsValid(), multiSigResult.getSignaturesReceived(),
                 multiSigResult.getRequiredSignatures());

        // Update response with signature info
        response.setSignaturesReceived(multiSigResult.getSignaturesReceived());
        response.setSignaturesRequired(multiSigResult.getRequiredSignatures());
        response.setSignatureProgress(multiSigResult.getSignaturesReceived() > 0 ?
                (double) multiSigResult.getSignaturesReceived() / multiSigResult.getRequiredSignatures() * 100 : 0.0);

        // Add approval signatures to response
        if (multiSigResult.getVerificationResults() != null) {
            for (MultiSignatureValidator.SignatureVerificationResult verResult : multiSigResult.getVerificationResults()) {
                TransferResponse.ApprovalSignature sig = TransferResponse.ApprovalSignature.builder()
                        .signer(verResult.getSigner())
                        .signatureType(verResult.getSignatureType())
                        .isVerified(verResult.isValid())
                        .verifiedAt(verResult.isValid() ? Instant.now() : null)
                        .weight(verResult.getWeight())
                        .build();
                response.addApprovalSignature(sig);
            }
        }

        // Handle validation errors
        if (!multiSigResult.getIsValid()) {
            LOG.warnf("Multi-sig validation failed for transfer %s", request.getTransferId());
            if (multiSigResult.getErrors() != null && !multiSigResult.getErrors().isEmpty()) {
                response.setErrorCode("SIGNATURE_VALIDATION_FAILED");
                response.setErrorDetails(String.join("; ", multiSigResult.getErrors()));
            }
            stateManager.transitionToFailed(request.getTransferId(),
                    "SIGNATURE_VALIDATION_FAILED",
                    multiSigResult.getErrors() != null && !multiSigResult.getErrors().isEmpty() ?
                            String.join("; ", multiSigResult.getErrors()) : "Signature validation failed");
            return response;
        }

        // Add warnings to response
        if (multiSigResult.getWarnings() != null && !multiSigResult.getWarnings().isEmpty()) {
            LOG.warnf("Multi-sig validation warnings for transfer %s: %s",
                     request.getTransferId(), String.join(", ", multiSigResult.getWarnings()));
            response.setMetadata(new java.util.HashMap<>());
            response.getMetadata().put("warnings", multiSigResult.getWarnings());
        }

        // Transition to SIGNED state
        boolean signedTransitionSuccess = stateManager.transitionToSigned(
                request.getTransferId(),
                multiSigResult.getSignaturesReceived(),
                multiSigResult.getRequiredSignatures()
        );

        if (!signedTransitionSuccess) {
            LOG.error("Failed to transition transfer to SIGNED state: " + request.getTransferId());
            response.setErrorCode("STATE_TRANSITION_FAILED");
            response.setErrorDetails("Failed to transition to SIGNED state");
            return response;
        }

        // Update response status
        response.setStatus(TransferResponse.TransferStatus.SIGNED);

        // Check if we can immediately approve (all signatures present and valid)
        if (multiSigResult.getThresholdMet() && multiSigResult.getAllSignaturesValid()) {
            LOG.infof("All signatures received and valid, auto-approving transfer: %s", request.getTransferId());
            boolean approvedTransitionSuccess = stateManager.transitionToApproved(request.getTransferId());

            if (approvedTransitionSuccess) {
                response.setStatus(TransferResponse.TransferStatus.APPROVED);
                stateManager.addEvent(request.getTransferId(), "APPROVED",
                        "All signatures verified, transfer auto-approved");
            }
        }

        response.setUpdatedAt(Instant.now());

        LOG.infof("Bridge transfer submitted successfully: %s, status: %s",
                 request.getTransferId(), response.getStatus());

        return response;
    }

    /**
     * Approve an existing transfer
     */
    public TransferResponse approveBridgeTransfer(String transferId) {
        LOG.infof("Approving bridge transfer: %s", transferId);

        TransferResponse response = stateManager.getTransfer(transferId);
        if (response == null) {
            LOG.warnf("Transfer not found: %s", transferId);
            return createErrorResponse(transferId, "TRANSFER_NOT_FOUND", "Transfer not found");
        }

        // Check current status
        if (response.getStatus() != TransferResponse.TransferStatus.SIGNED) {
            LOG.warnf("Cannot approve transfer in state %s: %s", response.getStatus(), transferId);
            return createErrorResponse(transferId, "INVALID_STATE",
                    "Transfer must be in SIGNED state to approve");
        }

        // Check if signatures are complete
        if (!response.areSignaturesComplete()) {
            LOG.warnf("Signatures not complete for transfer %s", transferId);
            return createErrorResponse(transferId, "INCOMPLETE_SIGNATURES",
                    "All required signatures must be collected before approval");
        }

        // Transition to APPROVED
        boolean success = stateManager.transitionToApproved(transferId);

        if (!success) {
            LOG.error("Failed to transition transfer to APPROVED state: " + transferId);
            return createErrorResponse(transferId, "STATE_TRANSITION_FAILED",
                    "Failed to transition to APPROVED state");
        }

        response = stateManager.getTransfer(transferId);
        response.setStatus(TransferResponse.TransferStatus.APPROVED);
        response.setUpdatedAt(Instant.now());

        LOG.infof("Bridge transfer approved: %s", transferId);
        return response;
    }

    /**
     * Execute an approved transfer
     */
    public TransferResponse executeBridgeTransfer(String transferId) {
        LOG.infof("Executing bridge transfer: %s", transferId);

        TransferResponse response = stateManager.getTransfer(transferId);
        if (response == null) {
            LOG.warnf("Transfer not found: %s", transferId);
            return createErrorResponse(transferId, "TRANSFER_NOT_FOUND", "Transfer not found");
        }

        // Check current status
        if (response.getStatus() != TransferResponse.TransferStatus.APPROVED) {
            LOG.warnf("Cannot execute transfer in state %s: %s", response.getStatus(), transferId);
            return createErrorResponse(transferId, "INVALID_STATE",
                    "Transfer must be in APPROVED state to execute");
        }

        // Transition to EXECUTING
        boolean success = stateManager.transitionToExecuting(transferId);

        if (!success) {
            LOG.error("Failed to transition transfer to EXECUTING state: " + transferId);
            return createErrorResponse(transferId, "STATE_TRANSITION_FAILED",
                    "Failed to transition to EXECUTING state");
        }

        response = stateManager.getTransfer(transferId);
        response.setStatus(TransferResponse.TransferStatus.EXECUTING);
        response.setUpdatedAt(Instant.now());

        // Calculate estimated completion time (30 seconds from now)
        long estimatedTime = 30000; // milliseconds
        response.setEstimatedCompletionTime(estimatedTime);

        stateManager.addEvent(transferId, "EXECUTING",
                "Transfer locked on source chain, awaiting confirmation");

        LOG.infof("Bridge transfer execution started: %s", transferId);
        return response;
    }

    /**
     * Complete a transfer
     */
    public TransferResponse completeBridgeTransfer(String transferId, String sourceHash,
                                                   String targetHash) {
        LOG.infof("Completing bridge transfer: %s", transferId);

        TransferResponse response = stateManager.getTransfer(transferId);
        if (response == null) {
            LOG.warnf("Transfer not found: %s", transferId);
            return createErrorResponse(transferId, "TRANSFER_NOT_FOUND", "Transfer not found");
        }

        // Transition to COMPLETED
        boolean success = stateManager.transitionToCompleted(transferId, sourceHash, targetHash, 12, 12);

        if (!success) {
            LOG.error("Failed to transition transfer to COMPLETED state: " + transferId);
            return createErrorResponse(transferId, "STATE_TRANSITION_FAILED",
                    "Failed to transition to COMPLETED state");
        }

        response = stateManager.getTransfer(transferId);
        response.setStatus(TransferResponse.TransferStatus.COMPLETED);

        stateManager.addEvent(transferId, "COMPLETED",
                "Transfer released on target chain and confirmed");

        LOG.infof("Bridge transfer completed successfully: %s", transferId);
        return response;
    }

    /**
     * Cancel a transfer
     */
    public TransferResponse cancelBridgeTransfer(String transferId, String reason) {
        LOG.infof("Cancelling bridge transfer: %s, reason: %s", transferId, reason);

        TransferResponse response = stateManager.getTransfer(transferId);
        if (response == null) {
            LOG.warnf("Transfer not found: %s", transferId);
            return createErrorResponse(transferId, "TRANSFER_NOT_FOUND", "Transfer not found");
        }

        // Can cancel from PENDING or SIGNED states
        if (response.getStatus() != TransferResponse.TransferStatus.PENDING &&
            response.getStatus() != TransferResponse.TransferStatus.SIGNED) {
            LOG.warnf("Cannot cancel transfer in state %s: %s", response.getStatus(), transferId);
            return createErrorResponse(transferId, "INVALID_STATE",
                    "Transfer can only be cancelled from PENDING or SIGNED state");
        }

        boolean success = stateManager.transitionToCancelled(transferId, reason);

        if (!success) {
            LOG.error("Failed to cancel transfer: " + transferId);
            return createErrorResponse(transferId, "STATE_TRANSITION_FAILED",
                    "Failed to cancel transfer");
        }

        response = stateManager.getTransfer(transferId);
        response.setStatus(TransferResponse.TransferStatus.CANCELLED);

        LOG.infof("Bridge transfer cancelled: %s", transferId);
        return response;
    }

    /**
     * Get transfer status
     */
    public TransferResponse getTransferStatus(String transferId) {
        return stateManager.getTransfer(transferId);
    }

    /**
     * Create error response
     */
    private TransferResponse createErrorResponse(String transferId, String errorCode, String errorDetails) {
        return TransferResponse.builder()
                .transferId(transferId)
                .status(TransferResponse.TransferStatus.FAILED)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Calculate transfer fees
     */
    public BigDecimal calculateTransferFees(BigDecimal amount) {
        // 0.1% bridge fee
        BigDecimal bridgeFee = amount.multiply(BigDecimal.valueOf(0.001));
        // 0.02% gas fee
        BigDecimal gasFee = amount.multiply(BigDecimal.valueOf(0.0002));
        return bridgeFee.add(gasFee);
    }
}
