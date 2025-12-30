package io.aurigraph.v11.bridge.services;

import io.aurigraph.v11.bridge.models.TransferResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * Transfer State Manager
 * Manages state transitions for bridge transfers
 * State machine: PENDING → SIGNED → APPROVED → EXECUTING → COMPLETED
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 */
@ApplicationScoped
public class TransferStateManager {

    private static final Logger LOG = Logger.getLogger(TransferStateManager.class);

    // State transition rules (immutable)
    private static final Map<TransferResponse.TransferStatus, Set<TransferResponse.TransferStatus>> VALID_TRANSITIONS =
            Collections.unmodifiableMap(new HashMap<TransferResponse.TransferStatus, Set<TransferResponse.TransferStatus>>() {{
                put(TransferResponse.TransferStatus.PENDING, Set.of(
                        TransferResponse.TransferStatus.SIGNED,
                        TransferResponse.TransferStatus.FAILED,
                        TransferResponse.TransferStatus.CANCELLED
                ));
                put(TransferResponse.TransferStatus.SIGNED, Set.of(
                        TransferResponse.TransferStatus.APPROVED,
                        TransferResponse.TransferStatus.FAILED,
                        TransferResponse.TransferStatus.CANCELLED
                ));
                put(TransferResponse.TransferStatus.APPROVED, Set.of(
                        TransferResponse.TransferStatus.EXECUTING,
                        TransferResponse.TransferStatus.FAILED,
                        TransferResponse.TransferStatus.CANCELLED
                ));
                put(TransferResponse.TransferStatus.EXECUTING, Set.of(
                        TransferResponse.TransferStatus.COMPLETED,
                        TransferResponse.TransferStatus.FAILED
                ));
                put(TransferResponse.TransferStatus.COMPLETED, Set.of());
                put(TransferResponse.TransferStatus.FAILED, Set.of(
                        TransferResponse.TransferStatus.PENDING  // Can retry
                ));
                put(TransferResponse.TransferStatus.CANCELLED, Set.of());
            }});

    // In-memory state store (in production, use database)
    private final Map<String, TransferResponse> stateStore = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Initialize transfer in PENDING state
     */
    public TransferResponse initializeTransfer(String transferId, String sourceChain, String targetChain,
                                              String tokenSymbol, java.math.BigDecimal amount) {
        LOG.infof("Initializing transfer: %s", transferId);

        if (stateStore.containsKey(transferId)) {
            LOG.warnf("Transfer already exists: %s", transferId);
            return stateStore.get(transferId);
        }

        TransferResponse response = TransferResponse.builder()
                .transferId(transferId)
                .status(TransferResponse.TransferStatus.PENDING)
                .signaturesReceived(0)
                .signaturesRequired(0)
                .signatureProgress(0.0)
                .amount(amount)
                .tokenSymbol(tokenSymbol)
                .sourceConfirmations(0)
                .targetConfirmations(0)
                .requiredConfirmations(12) // Default
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        stateStore.put(transferId, response);
        logStateChange(transferId, null, TransferResponse.TransferStatus.PENDING, "Transfer initialized");

        return response;
    }

    /**
     * Transition transfer to SIGNED state
     */
    public boolean transitionToSigned(String transferId, int signaturesReceived, int signaturesRequired) {
        return transitionState(transferId, TransferResponse.TransferStatus.SIGNED,
                state -> {
                    state.setSignaturesReceived(signaturesReceived);
                    state.setSignaturesRequired(signaturesRequired);
                    state.setSignatureProgress(signaturesRequired > 0 ?
                            (double) signaturesReceived / signaturesRequired * 100 : 0.0);
                }, "Signatures collected and verified");
    }

    /**
     * Transition transfer to APPROVED state
     */
    public boolean transitionToApproved(String transferId) {
        return transitionState(transferId, TransferResponse.TransferStatus.APPROVED,
                state -> state.setUpdatedAt(Instant.now()),
                "Multi-signature threshold met, transfer approved");
    }

    /**
     * Transition transfer to EXECUTING state
     */
    public boolean transitionToExecuting(String transferId) {
        return transitionState(transferId, TransferResponse.TransferStatus.EXECUTING,
                state -> state.setUpdatedAt(Instant.now()),
                "Transfer execution started on blockchain");
    }

    /**
     * Transition transfer to COMPLETED state
     */
    public boolean transitionToCompleted(String transferId, String sourceHash, String targetHash,
                                        Integer sourceConfirmations, Integer targetConfirmations) {
        return transitionState(transferId, TransferResponse.TransferStatus.COMPLETED,
                state -> {
                    state.setSourceTransactionHash(sourceHash);
                    state.setTargetTransactionHash(targetHash);
                    state.setSourceConfirmations(sourceConfirmations);
                    state.setTargetConfirmations(targetConfirmations);
                    state.setCompletedAt(Instant.now());
                    state.setUpdatedAt(Instant.now());

                    // Calculate actual completion time
                    if (state.getCreatedAt() != null && state.getCompletedAt() != null) {
                        long actualTime = state.getCompletedAt().toEpochMilli() - state.getCreatedAt().toEpochMilli();
                        state.setActualCompletionTime(actualTime);
                    }
                }, "Transfer completed successfully");
    }

    /**
     * Transition transfer to FAILED state
     */
    public boolean transitionToFailed(String transferId, String errorCode, String errorMessage) {
        return transitionState(transferId, TransferResponse.TransferStatus.FAILED,
                state -> {
                    state.setErrorCode(errorCode);
                    state.setErrorDetails(errorMessage);
                    state.setUpdatedAt(Instant.now());
                }, "Transfer failed: " + errorMessage);
    }

    /**
     * Transition transfer to CANCELLED state
     */
    public boolean transitionToCancelled(String transferId, String reason) {
        return transitionState(transferId, TransferResponse.TransferStatus.CANCELLED,
                state -> {
                    state.setErrorCode("CANCELLED");
                    state.setErrorDetails(reason);
                    state.setUpdatedAt(Instant.now());
                }, "Transfer cancelled: " + reason);
    }

    /**
     * Generic state transition method
     */
    private synchronized boolean transitionState(String transferId,
                                                 TransferResponse.TransferStatus newStatus,
                                                 java.util.function.Consumer<TransferResponse> updateFn,
                                                 String eventMessage) {
        TransferResponse response = stateStore.get(transferId);
        if (response == null) {
            LOG.warnf("Transfer not found: %s", transferId);
            return false;
        }

        TransferResponse.TransferStatus currentStatus = response.getStatus();

        // Validate transition
        if (!isValidTransition(currentStatus, newStatus)) {
            LOG.warnf("Invalid state transition for transfer %s: %s → %s",
                     transferId, currentStatus, newStatus);
            return false;
        }

        // Apply update function
        try {
            updateFn.accept(response);
        } catch (Exception e) {
            LOG.error("Error applying state update for transfer " + transferId, e);
            return false;
        }

        // Update status
        response.setStatus(newStatus);
        response.setUpdatedAt(Instant.now());

        // Log event
        logStateChange(transferId, currentStatus, newStatus, eventMessage);

        // Add to event log
        addEvent(transferId, newStatus.toString(), eventMessage);

        return true;
    }

    /**
     * Check if state transition is valid
     */
    private boolean isValidTransition(TransferResponse.TransferStatus from,
                                     TransferResponse.TransferStatus to) {
        if (from == null) {
            return to == TransferResponse.TransferStatus.PENDING;
        }

        Set<TransferResponse.TransferStatus> validNextStates = VALID_TRANSITIONS.get(from);
        return validNextStates != null && validNextStates.contains(to);
    }

    /**
     * Get transfer state
     */
    public TransferResponse getTransfer(String transferId) {
        return stateStore.get(transferId);
    }

    /**
     * Update signature progress
     */
    public void updateSignatureProgress(String transferId, int received, int required) {
        TransferResponse response = stateStore.get(transferId);
        if (response != null) {
            response.setSignaturesReceived(received);
            response.setSignaturesRequired(required);
            response.setSignatureProgress(required > 0 ? (double) received / required * 100 : 0.0);
            response.setUpdatedAt(Instant.now());
        }
    }

    /**
     * Update confirmation counts
     */
    public void updateConfirmations(String transferId, Integer sourceConf, Integer targetConf) {
        TransferResponse response = stateStore.get(transferId);
        if (response != null) {
            if (sourceConf != null) response.setSourceConfirmations(sourceConf);
            if (targetConf != null) response.setTargetConfirmations(targetConf);
            response.setUpdatedAt(Instant.now());
        }
    }

    /**
     * Add event to transfer
     */
    public void addEvent(String transferId, String eventType, String message) {
        TransferResponse response = stateStore.get(transferId);
        if (response != null) {
            TransferResponse.TransferEvent event = TransferResponse.TransferEvent.builder()
                    .eventType(eventType)
                    .message(message)
                    .timestamp(Instant.now())
                    .build();
            response.addEvent(event);
        }
    }

    /**
     * Log state transition
     */
    private void logStateChange(String transferId,
                               TransferResponse.TransferStatus fromStatus,
                               TransferResponse.TransferStatus toStatus,
                               String reason) {
        if (fromStatus == null) {
            LOG.infof("Transfer %s initialized in state %s: %s", transferId, toStatus, reason);
        } else {
            LOG.infof("Transfer %s transitioned: %s → %s. Reason: %s", transferId, fromStatus, toStatus, reason);
        }
    }

    /**
     * Get all transfers (for testing/monitoring)
     */
    public Collection<TransferResponse> getAllTransfers() {
        return stateStore.values();
    }

    /**
     * Clear state (for testing)
     */
    public void clearState() {
        stateStore.clear();
    }

    /**
     * Get state summary
     */
    public Map<String, Integer> getStateSummary() {
        Map<String, Integer> summary = new LinkedHashMap<>();
        stateStore.values().stream()
                .map(TransferResponse::getStatus)
                .forEach(status -> summary.compute(status.toString(),
                        (k, v) -> v == null ? 1 : v + 1));
        return summary;
    }
}
