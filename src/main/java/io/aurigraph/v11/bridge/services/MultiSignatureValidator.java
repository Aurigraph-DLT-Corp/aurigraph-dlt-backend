package io.aurigraph.v11.bridge.services;

import io.aurigraph.v11.bridge.models.TransferRequest;
import io.aurigraph.v11.bridge.security.SignatureVerificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-Signature Validator Service
 * Validates multi-signature requirements for bridge transfers
 * Supports M-of-N multi-sig schemes (e.g., 2-of-3, 3-of-5)
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 */
@ApplicationScoped
public class MultiSignatureValidator {

    private static final Logger LOG = Logger.getLogger(MultiSignatureValidator.class);

    @Inject
    SignatureVerificationService signatureVerifier;

    /**
     * Validate multi-signature requirements for a transfer
     */
    public MultiSigValidationResult validateMultiSignatures(TransferRequest request) {
        LOG.infof("Validating multi-signatures for transfer: %s", request.getTransferId());

        MultiSigValidationResult.Builder resultBuilder =
            MultiSigValidationResult.builder()
                .transferId(request.getTransferId())
                .requiredSignatures(request.getRequiredSignatures())
                .totalSigners(request.getTotalSigners());

        // Validate signature count
        int signatureCount = request.getSignatures() != null ? request.getSignatures().size() : 0;
        boolean thresholdMet = signatureCount >= request.getRequiredSignatures();

        LOG.infof("Signatures received: %d, Required: %d, Threshold met: %b",
                 signatureCount, request.getRequiredSignatures(), thresholdMet);

        resultBuilder.signaturesReceived(signatureCount)
                   .thresholdMet(thresholdMet);

        // Verify each signature
        List<SignatureVerificationResult> verificationResults = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (request.getSignatures() != null) {
            for (TransferRequest.SignatureData sig : request.getSignatures()) {
                SignatureVerificationResult verResult = verifySignature(request, sig);
                verificationResults.add(verResult);

                if (!verResult.isValid()) {
                    errors.add(String.format("Signature from %s is invalid: %s",
                            sig.getSigner(), verResult.getErrorMessage()));
                } else {
                    LOG.debugf("Signature from %s verified successfully", sig.getSigner());
                }
            }
        } else {
            errors.add("No signatures provided");
        }

        // Check for duplicate signers
        if (request.getSignatures() != null) {
            Set<String> signers = new HashSet<>();
            List<String> duplicates = new ArrayList<>();

            for (TransferRequest.SignatureData sig : request.getSignatures()) {
                if (!signers.add(sig.getSigner())) {
                    duplicates.add(sig.getSigner());
                }
            }

            if (!duplicates.isEmpty()) {
                errors.add("Duplicate signers detected: " + duplicates);
            }
        }

        // Count valid signatures
        long validCount = verificationResults.stream()
                .filter(SignatureVerificationResult::isValid)
                .count();

        boolean isValid = validCount >= request.getRequiredSignatures() && errors.isEmpty();

        if (isValid && !errors.isEmpty()) {
            warnings.add("Transfer is valid but has non-critical issues");
        }

        // Check for outdated signatures (older than 1 hour)
        long now = System.currentTimeMillis();
        long oneHourAgo = now - (60 * 60 * 1000);

        for (TransferRequest.SignatureData sig : request.getSignatures()) {
            if (sig.getSignedAt() < oneHourAgo) {
                warnings.add(String.format("Signature from %s is older than 1 hour", sig.getSigner()));
            }
        }

        LOG.infof("Multi-sig validation result: valid=%b, valid signatures=%d, errors=%d",
                 isValid, validCount, errors.size());

        return resultBuilder
                .validSignatures((int) validCount)
                .allSignaturesValid(validCount == signatureCount)
                .verificationResults(verificationResults)
                .errors(errors.isEmpty() ? null : errors)
                .warnings(warnings.isEmpty() ? null : warnings)
                .isValid(isValid)
                .build();
    }

    /**
     * Verify a single signature
     */
    private SignatureVerificationResult verifySignature(TransferRequest request,
                                                        TransferRequest.SignatureData sig) {
        try {
            // Create the data that was signed
            String dataToVerify = createSignableData(request);

            // Verify using the signature verification service
            boolean isValid = signatureVerifier.verifySignature(
                    dataToVerify,
                    sig.getSignature(),
                    sig.getSignatureType()
            );

            return SignatureVerificationResult.builder()
                    .signer(sig.getSigner())
                    .signatureType(sig.getSignatureType())
                    .isValid(isValid)
                    .weight(sig.getWeight() != null ? sig.getWeight() : 1)
                    .build();

        } catch (Exception e) {
            LOG.error("Error verifying signature from " + sig.getSigner(), e);
            return SignatureVerificationResult.builder()
                    .signer(sig.getSigner())
                    .signatureType(sig.getSignatureType())
                    .isValid(false)
                    .errorMessage("Verification failed: " + e.getMessage())
                    .weight(sig.getWeight() != null ? sig.getWeight() : 1)
                    .build();
        }
    }

    /**
     * Create signable data from transfer request
     */
    private String createSignableData(TransferRequest request) {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%d|%d",
                request.getTransferId(),
                request.getSourceChain(),
                request.getTargetChain(),
                request.getSourceAddress(),
                request.getTargetAddress(),
                request.getTokenSymbol(),
                request.getAmount().toPlainString(),
                request.getRequiredSignatures(),
                request.getNonce() != null ? request.getNonce() : 0);
    }

    /**
     * Validate M-of-N multi-signature scheme
     */
    public boolean validateMofNScheme(int m, int n, int signatureCount) {
        return m >= 1 && m <= n && signatureCount >= m;
    }

    /**
     * Multi-signature validation result
     */
    public static class MultiSigValidationResult {
        private String transferId;
        private Integer requiredSignatures;
        private Integer totalSigners;
        private Integer signaturesReceived;
        private Integer validSignatures;
        private Boolean allSignaturesValid;
        private Boolean thresholdMet;
        private Boolean isValid;
        private List<SignatureVerificationResult> verificationResults;
        private List<String> errors;
        private List<String> warnings;

        public MultiSigValidationResult(String transferId, Integer requiredSignatures,
                                       Integer totalSigners, Integer signaturesReceived,
                                       Integer validSignatures, Boolean allSignaturesValid,
                                       Boolean thresholdMet, Boolean isValid,
                                       List<SignatureVerificationResult> verificationResults,
                                       List<String> errors, List<String> warnings) {
            this.transferId = transferId;
            this.requiredSignatures = requiredSignatures;
            this.totalSigners = totalSigners;
            this.signaturesReceived = signaturesReceived;
            this.validSignatures = validSignatures;
            this.allSignaturesValid = allSignaturesValid;
            this.thresholdMet = thresholdMet;
            this.isValid = isValid;
            this.verificationResults = verificationResults;
            this.errors = errors;
            this.warnings = warnings;
        }

        // Builder
        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getTransferId() { return transferId; }
        public Integer getRequiredSignatures() { return requiredSignatures; }
        public Integer getTotalSigners() { return totalSigners; }
        public Integer getSignaturesReceived() { return signaturesReceived; }
        public Integer getValidSignatures() { return validSignatures; }
        public Boolean getAllSignaturesValid() { return allSignaturesValid; }
        public Boolean getThresholdMet() { return thresholdMet; }
        public Boolean getIsValid() { return isValid; }
        public List<SignatureVerificationResult> getVerificationResults() { return verificationResults; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }

        public static class Builder {
            private String transferId;
            private Integer requiredSignatures;
            private Integer totalSigners;
            private Integer signaturesReceived;
            private Integer validSignatures;
            private Boolean allSignaturesValid;
            private Boolean thresholdMet;
            private Boolean isValid;
            private List<SignatureVerificationResult> verificationResults;
            private List<String> errors;
            private List<String> warnings;

            public Builder transferId(String transferId) { this.transferId = transferId; return this; }
            public Builder requiredSignatures(Integer requiredSignatures) { this.requiredSignatures = requiredSignatures; return this; }
            public Builder totalSigners(Integer totalSigners) { this.totalSigners = totalSigners; return this; }
            public Builder signaturesReceived(Integer signaturesReceived) { this.signaturesReceived = signaturesReceived; return this; }
            public Builder validSignatures(Integer validSignatures) { this.validSignatures = validSignatures; return this; }
            public Builder allSignaturesValid(Boolean allSignaturesValid) { this.allSignaturesValid = allSignaturesValid; return this; }
            public Builder thresholdMet(Boolean thresholdMet) { this.thresholdMet = thresholdMet; return this; }
            public Builder isValid(Boolean isValid) { this.isValid = isValid; return this; }
            public Builder verificationResults(List<SignatureVerificationResult> verificationResults) { this.verificationResults = verificationResults; return this; }
            public Builder errors(List<String> errors) { this.errors = errors; return this; }
            public Builder warnings(List<String> warnings) { this.warnings = warnings; return this; }

            public MultiSigValidationResult build() {
                return new MultiSigValidationResult(transferId, requiredSignatures, totalSigners,
                        signaturesReceived, validSignatures, allSignaturesValid, thresholdMet,
                        isValid, verificationResults, errors, warnings);
            }
        }
    }

    /**
     * Individual signature verification result
     */
    public static class SignatureVerificationResult {
        private String signer;
        private String signatureType;
        private Boolean isValid;
        private String errorMessage;
        private Integer weight;

        public SignatureVerificationResult(String signer, String signatureType, Boolean isValid,
                                          String errorMessage, Integer weight) {
            this.signer = signer;
            this.signatureType = signatureType;
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.weight = weight;
        }

        // Builder
        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getSigner() { return signer; }
        public String getSignatureType() { return signatureType; }
        public Boolean getIsValid() { return isValid; }
        public Boolean isValid() { return isValid; }
        public String getErrorMessage() { return errorMessage; }
        public Integer getWeight() { return weight; }

        public static class Builder {
            private String signer;
            private String signatureType;
            private Boolean isValid;
            private String errorMessage;
            private Integer weight;

            public Builder signer(String signer) { this.signer = signer; return this; }
            public Builder signatureType(String signatureType) { this.signatureType = signatureType; return this; }
            public Builder isValid(Boolean isValid) { this.isValid = isValid; return this; }
            public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
            public Builder weight(Integer weight) { this.weight = weight; return this; }

            public SignatureVerificationResult build() {
                return new SignatureVerificationResult(signer, signatureType, isValid, errorMessage, weight);
            }
        }
    }
}
