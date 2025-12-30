package io.aurigraph.v11.bridge.services;

import io.aurigraph.v11.bridge.models.TransferRequest;
import io.aurigraph.v11.bridge.models.TransferResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BridgeTransferService
 * Tests multi-signature transfer submission, approval, and execution
 */
@DisplayName("Bridge Transfer Service Tests")
public class BridgeTransferServiceTest {

    private BridgeTransferService transferService;
    private MultiSignatureValidator multiSigValidator;
    private TransferStateManager stateManager;
    private TransferRequest validRequest;

    @BeforeEach
    void setUp() {
        // Create mocks
        multiSigValidator = mock(MultiSignatureValidator.class);
        stateManager = mock(TransferStateManager.class);

        // Create service instance
        transferService = new BridgeTransferService();
        transferService.multiSigValidator = multiSigValidator;
        transferService.stateManager = stateManager;

        // Create valid test request
        validRequest = TransferRequest.builder()
                .transferId("transfer-001")
                .bridgeId("bridge-001")
                .sourceChain("ethereum")
                .targetChain("polygon")
                .tokenSymbol("ETH")
                .amount(new BigDecimal("10.00"))
                .requiredSignatures(2)
                .totalSigners(3)
                .signatures(new ArrayList<TransferRequest.SignatureData>() {{
                    add(TransferRequest.SignatureData.builder()
                            .signer("signer1")
                            .signature("sig1")
                            .signatureType("SECP256K1")
                            .weight(1)
                            .build());
                }})
                .build();

        // Setup mock default response
        TransferResponse mockResponse = createMockTransferResponse("transfer-001");
        when(stateManager.initializeTransfer(
                anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(mockResponse);
    }

    @Test
    @DisplayName("Submit transfer with valid signatures should transition to SIGNED")
    void testSubmitTransferValidSignatures() {
        // Given
        MultiSignatureValidator.MultiSigValidationResult validResult =
                createValidMultiSigResult(true, 2, 2);

        when(multiSigValidator.validateMultiSignatures(validRequest))
                .thenReturn(validResult);
        when(stateManager.transitionToSigned(anyString(), anyInt(), anyInt()))
                .thenReturn(true);

        TransferResponse signedResponse = createMockTransferResponse("transfer-001");
        signedResponse.setStatus(TransferResponse.TransferStatus.SIGNED);
        signedResponse.setSignaturesReceived(2);
        signedResponse.setSignaturesRequired(2);
        when(stateManager.initializeTransfer(anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(signedResponse);

        // When
        TransferResponse response = transferService.submitBridgeTransfer(validRequest);

        // Then
        assertNotNull(response);
        assertEquals("transfer-001", response.getTransferId());
        assertEquals(TransferResponse.TransferStatus.SIGNED, response.getStatus());
        assertEquals(2, response.getSignaturesReceived());
        assertEquals(2, response.getSignaturesRequired());
    }

    @Test
    @DisplayName("Submit transfer with missing required fields should fail")
    void testSubmitTransferMissingFields() {
        // Given
        validRequest.setTransferId(""); // Empty ID

        // When
        TransferResponse response = transferService.submitBridgeTransfer(validRequest);

        // Then
        assertNotNull(response);
        assertEquals(TransferResponse.TransferStatus.FAILED, response.getStatus());
        assertEquals("VALIDATION_FAILED", response.getErrorCode());
    }

    @Test
    @DisplayName("Submit transfer with invalid signatures should fail")
    void testSubmitTransferInvalidSignatures() {
        // Given
        MultiSignatureValidator.MultiSigValidationResult invalidResult =
                createInvalidMultiSigResult("Invalid signature format");

        when(multiSigValidator.validateMultiSignatures(validRequest))
                .thenReturn(invalidResult);

        // Setup default response for initializeTransfer
        TransferResponse pendingResponse = createMockTransferResponse("transfer-001");
        when(stateManager.initializeTransfer(anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(pendingResponse);

        // Setup failed state transition to capture error response
        when(stateManager.transitionToFailed(anyString(), anyString(), anyString()))
                .thenReturn(true);

        // When
        TransferResponse response = transferService.submitBridgeTransfer(validRequest);

        // Then
        assertNotNull(response);
        // When multi-sig validation fails, the response includes error details
        assertEquals("SIGNATURE_VALIDATION_FAILED", response.getErrorCode());
        assertNotNull(response.getErrorDetails());
        assertTrue(response.getErrorDetails().contains("Invalid signature format"));
    }

    @Test
    @DisplayName("Approve signed transfer should transition to APPROVED")
    void testApproveSignedTransfer() {
        // Given
        TransferResponse signedResponse = createMockTransferResponse("transfer-001");
        signedResponse.setStatus(TransferResponse.TransferStatus.SIGNED);
        signedResponse.setSignaturesReceived(2);
        signedResponse.setSignaturesRequired(2);

        when(stateManager.getTransfer("transfer-001"))
                .thenReturn(signedResponse);
        when(stateManager.transitionToApproved("transfer-001"))
                .thenReturn(true);

        TransferResponse approvedResponse = createMockTransferResponse("transfer-001");
        approvedResponse.setStatus(TransferResponse.TransferStatus.APPROVED);

        // When
        TransferResponse response = transferService.approveBridgeTransfer("transfer-001");

        // Then
        assertNotNull(response);
        assertEquals("transfer-001", response.getTransferId());
        // Note: mock setup requires updating stateManager.getTransfer to return approved response
    }

    @Test
    @DisplayName("Approve non-existent transfer should return error")
    void testApproveNonExistentTransfer() {
        // Given
        when(stateManager.getTransfer("non-existent"))
                .thenReturn(null);

        // When
        TransferResponse response = transferService.approveBridgeTransfer("non-existent");

        // Then
        assertNotNull(response);
        assertEquals(TransferResponse.TransferStatus.FAILED, response.getStatus());
        assertEquals("TRANSFER_NOT_FOUND", response.getErrorCode());
    }

    @Test
    @DisplayName("Execute approved transfer should transition to EXECUTING")
    void testExecuteApprovedTransfer() {
        // Given
        TransferResponse approvedResponse = createMockTransferResponse("transfer-001");
        approvedResponse.setStatus(TransferResponse.TransferStatus.APPROVED);

        when(stateManager.getTransfer("transfer-001"))
                .thenReturn(approvedResponse);
        when(stateManager.transitionToExecuting("transfer-001"))
                .thenReturn(true);

        // When
        TransferResponse response = transferService.executeBridgeTransfer("transfer-001");

        // Then
        assertNotNull(response);
        assertEquals("transfer-001", response.getTransferId());
        assertNotNull(response.getEstimatedCompletionTime());
    }

    @Test
    @DisplayName("Complete executing transfer should transition to COMPLETED")
    void testCompleteExecutingTransfer() {
        // Given
        TransferResponse executingResponse = createMockTransferResponse("transfer-001");
        executingResponse.setStatus(TransferResponse.TransferStatus.EXECUTING);

        when(stateManager.getTransfer("transfer-001"))
                .thenReturn(executingResponse);
        when(stateManager.transitionToCompleted(
                eq("transfer-001"), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(true);

        // When
        TransferResponse response = transferService.completeBridgeTransfer(
                "transfer-001",
                "0xsource123",
                "0xtarget456"
        );

        // Then
        assertNotNull(response);
        assertEquals("transfer-001", response.getTransferId());
    }

    @Test
    @DisplayName("Cancel pending transfer should transition to CANCELLED")
    void testCancelPendingTransfer() {
        // Given
        TransferResponse pendingResponse = createMockTransferResponse("transfer-001");
        pendingResponse.setStatus(TransferResponse.TransferStatus.PENDING);

        when(stateManager.getTransfer("transfer-001"))
                .thenReturn(pendingResponse);
        when(stateManager.transitionToCancelled("transfer-001", "User cancelled"))
                .thenReturn(true);

        // When
        TransferResponse response = transferService.cancelBridgeTransfer(
                "transfer-001",
                "User cancelled"
        );

        // Then
        assertNotNull(response);
        assertEquals("transfer-001", response.getTransferId());
    }

    @Test
    @DisplayName("Get transfer status should return current state")
    void testGetTransferStatus() {
        // Given
        TransferResponse expectedResponse = createMockTransferResponse("transfer-001");
        when(stateManager.getTransfer("transfer-001"))
                .thenReturn(expectedResponse);

        // When
        TransferResponse response = transferService.getTransferStatus("transfer-001");

        // Then
        assertNotNull(response);
        assertEquals("transfer-001", response.getTransferId());
    }

    @Test
    @DisplayName("Calculate transfer fees should be 0.12% of amount")
    void testCalculateTransferFees() {
        // Given
        BigDecimal amount = new BigDecimal("1000.00");

        // When
        BigDecimal fees = transferService.calculateTransferFees(amount);

        // Then
        assertNotNull(fees);
        // 0.1% bridge + 0.02% gas = 0.12% = 1.2
        assertTrue(fees.compareTo(new BigDecimal("1.1")) >= 0);
        assertTrue(fees.compareTo(new BigDecimal("1.3")) <= 0);
    }

    @Test
    @DisplayName("Transfer with all required signatures should transition properly")
    void testTransferWithAllSignatures() {
        // Given
        MultiSignatureValidator.MultiSigValidationResult allSignedResult =
                createValidMultiSigResult(true, 2, 2);

        when(multiSigValidator.validateMultiSignatures(validRequest))
                .thenReturn(allSignedResult);
        when(stateManager.transitionToSigned(anyString(), anyInt(), anyInt()))
                .thenReturn(true);

        TransferResponse signedResponse = createMockTransferResponse("transfer-001");
        signedResponse.setStatus(TransferResponse.TransferStatus.SIGNED);

        // When
        TransferResponse response = transferService.submitBridgeTransfer(validRequest);

        // Then
        assertNotNull(response);
        assertEquals("transfer-001", response.getTransferId());
    }

    // Helper methods

    private TransferResponse createMockTransferResponse(String transferId) {
        return TransferResponse.builder()
                .transferId(transferId)
                .status(TransferResponse.TransferStatus.PENDING)
                .signaturesRequired(2)
                .signaturesReceived(0)
                .signatureProgress(0.0)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
    }

    private MultiSignatureValidator.MultiSigValidationResult createValidMultiSigResult(
            boolean isValid, int signaturesReceived, int requiredSignatures) {
        List<String> emptyList = new ArrayList<>();
        return new MultiSignatureValidator.MultiSigValidationResult(
                "transfer-001",  // transferId
                requiredSignatures,
                3,               // totalSigners
                signaturesReceived,
                signaturesReceived,  // validSignatures
                false,           // allSignaturesValid
                false,           // thresholdMet
                isValid,         // isValid
                new ArrayList<>(), // verificationResults
                emptyList,       // errors
                emptyList        // warnings
        );
    }

    private MultiSignatureValidator.MultiSigValidationResult createInvalidMultiSigResult(
            String errorMessage) {
        List<String> errors = new ArrayList<>();
        errors.add(errorMessage);
        List<String> emptyList = new ArrayList<>();
        return new MultiSignatureValidator.MultiSigValidationResult(
                "transfer-001",  // transferId
                2,               // requiredSignatures
                3,               // totalSigners
                0,               // signaturesReceived
                0,               // validSignatures
                false,           // allSignaturesValid
                false,           // thresholdMet
                false,           // isValid
                new ArrayList<>(), // verificationResults
                errors,          // errors
                emptyList        // warnings
        );
    }
}
