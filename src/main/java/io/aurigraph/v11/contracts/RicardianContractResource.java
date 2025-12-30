package io.aurigraph.v11.contracts;

import io.aurigraph.v11.contracts.models.ContractParty;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;

/**
 * Ricardian Contract REST API Resource
 *
 * Provides RESTful endpoints for Ricardian contract operations including:
 * - Document upload and conversion
 * - Party management
 * - Signature collection
 * - Contract activation
 * - Audit trail queries
 * - Compliance reporting
 *
 * All contract activities go through:
 * 1. Consensus validation (HyperRAFT++)
 * 2. AURI token gas fee charging
 * 3. LevelDB ledger logging
 * 4. Audit trail recording
 *
 * @version 1.0.0 (Oct 10, 2025)
 * @author Aurigraph V11 Development Team
 */
@Path("/api/v11/contracts/ricardian")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RicardianContractResource {

    private static final Logger LOG = Logger.getLogger(RicardianContractResource.class);

    @Inject
    RicardianContractConversionService conversionService;

    @Inject
    WorkflowConsensusService consensusService;

    @Inject
    LedgerAuditService auditService;

    @Inject
    ActiveContractService activeContractService;

    // In-memory storage for demo (in production, use LevelDB repository)
    private final Map<String, RicardianContract> contracts = new HashMap<>();

    /**
     * Upload document and convert to Ricardian contract
     *
     * POST /api/v11/contracts/ricardian/upload
     *
     * AV11-289: Enhanced validation for contract upload
     * Updated to use Jakarta EE 10 multipart handling (removed deprecated @MultipartForm)
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> uploadDocument(DocumentUploadForm form) {
        LOG.infof("Uploading document: %s, type: %s, jurisdiction: %s",
                form.fileName, form.contractType, form.jurisdiction);

        try {
            // AV11-289: Comprehensive Upload Validation
            ValidationResult validation = validateUpload(form);
            if (!validation.isValid()) {
                LOG.warnf("Upload validation failed: %s", validation.getErrors());
                return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "error", "Upload validation failed",
                                "validationErrors", validation.getErrors()
                        ))
                        .build());
            }

            // Read file content
            byte[] fileContent = form.file.readAllBytes();

            // Step 1: Submit document upload activity to consensus
            Map<String, Object> uploadPayload = Map.of(
                    "fileName", form.fileName,
                    "fileSize", fileContent.length,
                    "contractType", form.contractType,
                    "jurisdiction", form.jurisdiction
            );

            WorkflowConsensusService.ConsensusRequest uploadConsensus =
                    new WorkflowConsensusService.ConsensusRequest(
                            "PENDING", // Will be assigned after conversion
                            WorkflowConsensusService.ActivityType.DOCUMENT_UPLOAD,
                            form.submitterAddress,
                            uploadPayload
                    );

            return consensusService.submitActivity(uploadConsensus)
                    .flatMap(consensusResult -> {
                        if (!consensusResult.success()) {
                            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                                    .entity(Map.of(
                                            "error", "Consensus failed for document upload",
                                            "message", consensusResult.message()
                                    ))
                                    .build());
                        }

                        // Step 2: Convert document to Ricardian contract
                        List<ContractParty> suggestedParties = parseSuggestedParties(form.suggestedParties);

                        return conversionService.convertDocumentToContract(
                                        form.fileName,
                                        fileContent,
                                        form.contractType,
                                        form.jurisdiction,
                                        suggestedParties
                                )
                                .flatMap(contract -> {
                                    // Store contract
                                    contracts.put(contract.getContractId(), contract);

                                    // Step 3: Submit conversion activity to consensus
                                    Map<String, Object> conversionPayload = Map.of(
                                            "contractId", contract.getContractId(),
                                            "partiesDetected", contract.getParties().size(),
                                            "termsExtracted", contract.getTerms().size()
                                    );

                                    WorkflowConsensusService.ConsensusRequest conversionConsensus =
                                            new WorkflowConsensusService.ConsensusRequest(
                                                    contract.getContractId(),
                                                    WorkflowConsensusService.ActivityType.CONTRACT_CONVERSION,
                                                    form.submitterAddress,
                                                    conversionPayload
                                            );

                                    return consensusService.submitActivity(conversionConsensus)
                                            .flatMap(conversionResult -> {
                                                // Step 4: Log to audit trail
                                                LedgerAuditService.AuditLogRequest auditRequest =
                                                        new LedgerAuditService.AuditLogRequest(
                                                                contract.getContractId(),
                                                                "CONTRACT_CONVERSION",
                                                                form.submitterAddress,
                                                                "Document converted to Ricardian contract",
                                                                Map.of(
                                                                        "fileName", form.fileName,
                                                                        "parties", contract.getParties().size(),
                                                                        "terms", contract.getTerms().size()
                                                                ),
                                                                conversionResult.transactionHash(),
                                                                conversionResult.blockNumber()
                                                        );

                                                return auditService.logActivity(auditRequest)
                                                        .map(auditEntry -> {
                                                            LOG.infof("✅ Contract created: %s, txHash: %s, block: %d",
                                                                    contract.getContractId(),
                                                                    conversionResult.transactionHash(),
                                                                    conversionResult.blockNumber());

                                                            return Response.ok(Map.of(
                                                                    "success", true,
                                                                    "contractId", contract.getContractId(),
                                                                    "transactionHash", conversionResult.transactionHash(),
                                                                    "blockNumber", conversionResult.blockNumber(),
                                                                    "gasCharged", conversionResult.gasFeeCharged(),
                                                                    "contract", serializeContract(contract),
                                                                    "auditEntryId", auditEntry.entryId()
                                                            )).build();
                                                        });
                                            });
                                });
                    });

        } catch (Exception e) {
            LOG.errorf(e, "Error uploading document: %s", form.fileName);
            return Uni.createFrom().item(Response.serverError()
                    .entity(Map.of("error", "Document upload failed", "message", e.getMessage()))
                    .build());
        }
    }

    /**
     * List all contracts with pagination
     *
     * GET /api/v11/contracts/ricardian
     *
     * Query params:
     * - page: page number (default: 0)
     * - size: page size (default: 20)
     * - status: filter by status (optional)
     * - jurisdiction: filter by jurisdiction (optional)
     */
    @GET
    public Uni<Response> listContracts(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String status,
            @QueryParam("jurisdiction") String jurisdiction
    ) {
        LOG.infof("Listing contracts: page=%d, size=%d, status=%s, jurisdiction=%s",
                page, size, status, jurisdiction);

        // Filter contracts
        List<RicardianContract> filteredContracts = contracts.values().stream()
                .filter(c -> status == null || c.getStatus().toString().equalsIgnoreCase(status))
                .filter(c -> jurisdiction == null || c.getJurisdiction().equalsIgnoreCase(jurisdiction))
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt())) // newest first
                .toList();

        // Calculate pagination
        int totalContracts = filteredContracts.size();
        int totalPages = (int) Math.ceil((double) totalContracts / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalContracts);

        List<Map<String, Object>> paginatedContracts = new ArrayList<>();
        if (fromIndex < totalContracts) {
            paginatedContracts = filteredContracts.subList(fromIndex, toIndex).stream()
                    .map(this::serializeContract)
                    .toList();
        }

        Map<String, Object> response = Map.of(
                "contracts", paginatedContracts,
                "pagination", Map.of(
                        "page", page,
                        "size", size,
                        "totalContracts", totalContracts,
                        "totalPages", totalPages,
                        "hasNext", (page + 1) < totalPages,
                        "hasPrevious", page > 0
                )
        );

        return Uni.createFrom().item(Response.ok(response).build());
    }

    /**
     * Get contract by ID
     *
     * GET /api/v11/contracts/ricardian/{contractId}
     */
    @GET
    @Path("/{contractId}")
    public Uni<Response> getContract(@PathParam("contractId") String contractId) {
        LOG.infof("Getting contract: %s", contractId);

        RicardianContract contract = contracts.get(contractId);
        if (contract == null) {
            return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Contract not found", "contractId", contractId))
                    .build());
        }

        return Uni.createFrom().item(Response.ok(serializeContract(contract)).build());
    }

    /**
     * Add party to contract
     *
     * POST /api/v11/contracts/ricardian/{contractId}/parties
     */
    @POST
    @Path("/{contractId}/parties")
    public Uni<Response> addParty(
            @PathParam("contractId") String contractId,
            AddPartyRequest request
    ) {
        LOG.infof("Adding party to contract %s: %s (%s)",
                contractId, request.name, request.role);

        RicardianContract contract = contracts.get(contractId);
        if (contract == null) {
            return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Contract not found"))
                    .build());
        }

        // Create party
        ContractParty party = ContractParty.builder()
                .partyId("PARTY_" + UUID.randomUUID().toString().substring(0, 8))
                .name(request.name)
                .address(request.address)
                .role(request.role)
                .signatureRequired(request.signatureRequired)
                .kycVerified(request.kycVerified)
                .createdAt(Instant.now())
                .build();

        contract.addParty(party);

        // Submit to consensus
        Map<String, Object> payload = Map.of(
                "partyId", party.getPartyId(),
                "name", party.getName(),
                "role", party.getRole()
        );

        WorkflowConsensusService.ConsensusRequest consensusRequest =
                new WorkflowConsensusService.ConsensusRequest(
                        contractId,
                        WorkflowConsensusService.ActivityType.PARTY_ADDITION,
                        request.submitterAddress,
                        payload
                );

        return consensusService.submitActivity(consensusRequest)
                .flatMap(result -> {
                    // Log to audit trail
                    LedgerAuditService.AuditLogRequest auditRequest =
                            new LedgerAuditService.AuditLogRequest(
                                    contractId,
                                    "PARTY_ADDITION",
                                    request.submitterAddress,
                                    "Added party: " + party.getName() + " (" + party.getRole() + ")",
                                    Map.of("partyId", party.getPartyId()),
                                    result.transactionHash(),
                                    result.blockNumber()
                            );

                    return auditService.logActivity(auditRequest)
                            .map(auditEntry -> Response.ok(Map.of(
                                    "success", true,
                                    "party", party,
                                    "transactionHash", result.transactionHash(),
                                    "blockNumber", result.blockNumber(),
                                    "gasCharged", result.gasFeeCharged()
                            )).build());
                });
    }

    /**
     * Submit signature for contract
     *
     * POST /api/v11/contracts/ricardian/{contractId}/sign
     */
    @POST
    @Path("/{contractId}/sign")
    public Uni<Response> signContract(
            @PathParam("contractId") String contractId,
            SignatureRequest request
    ) {
        LOG.infof("Signing contract %s by %s", contractId, request.signerAddress);

        RicardianContract contract = contracts.get(contractId);
        if (contract == null) {
            return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Contract not found"))
                    .build());
        }

        // Add signature (TODO: Verify with CRYSTALS-Dilithium)
        // ContractSignature is package-private, so we'll use reflection or create via factory
        // For now, create a simple implementation
        Object sig = createSignature(
                request.signerAddress,
                request.signature,
                request.publicKey,
                Instant.now()
        );
        if (sig != null) {
            @SuppressWarnings("unchecked")
            List<Object> sigs = (List<Object>)(List<?>)contract.getSignatures();
            sigs.add(sig);
        }

        // Submit to consensus
        Map<String, Object> payload = Map.of(
                "signerAddress", request.signerAddress,
                "signatureAlgorithm", "CRYSTALS-Dilithium"
        );

        WorkflowConsensusService.ConsensusRequest consensusRequest =
                new WorkflowConsensusService.ConsensusRequest(
                        contractId,
                        WorkflowConsensusService.ActivityType.SIGNATURE_SUBMISSION,
                        request.signerAddress,
                        payload
                );

        return consensusService.submitActivity(consensusRequest)
                .flatMap(result -> {
                    // Log to audit trail
                    LedgerAuditService.AuditLogRequest auditRequest =
                            new LedgerAuditService.AuditLogRequest(
                                    contractId,
                                    "SIGNATURE_SUBMISSION",
                                    request.signerAddress,
                                    "Contract signed by " + request.signerAddress,
                                    Map.of("algorithm", "CRYSTALS-Dilithium"),
                                    result.transactionHash(),
                                    result.blockNumber()
                            );

                    return auditService.logActivity(auditRequest)
                            .map(auditEntry -> Response.ok(Map.of(
                                    "success", true,
                                    "isFullySigned", contract.isFullySigned(),
                                    "signatures", contract.getSignatures().size(),
                                    "requiredSignatures", contract.getParties().stream()
                                            .filter(ContractParty::isSignatureRequired).count(),
                                    "transactionHash", result.transactionHash(),
                                    "blockNumber", result.blockNumber(),
                                    "gasCharged", result.gasFeeCharged()
                            )).build());
                });
    }

    /**
     * Activate contract
     *
     * POST /api/v11/contracts/ricardian/{contractId}/activate
     */
    @POST
    @Path("/{contractId}/activate")
    public Uni<Response> activateContract(
            @PathParam("contractId") String contractId,
            ActivationRequest request
    ) {
        LOG.infof("Activating contract: %s", contractId);

        RicardianContract contract = contracts.get(contractId);
        if (contract == null) {
            return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Contract not found"))
                    .build());
        }

        if (!contract.isFullySigned()) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Contract is not fully signed"))
                    .build());
        }

        // Activate contract
        contract.setStatus(io.aurigraph.v11.contracts.ContractStatus.ACTIVE);
        contract.setActivatedAt(Instant.now());

        // Submit to consensus
        Map<String, Object> payload = Map.of(
                "activatedBy", request.activatorAddress,
                "activatedAt", Instant.now().toString()
        );

        WorkflowConsensusService.ConsensusRequest consensusRequest =
                new WorkflowConsensusService.ConsensusRequest(
                        contractId,
                        WorkflowConsensusService.ActivityType.CONTRACT_ACTIVATION,
                        request.activatorAddress,
                        payload
                );

        return consensusService.submitActivity(consensusRequest)
                .flatMap(result -> {
                    // Log to audit trail
                    LedgerAuditService.AuditLogRequest auditRequest =
                            new LedgerAuditService.AuditLogRequest(
                                    contractId,
                                    "CONTRACT_ACTIVATION",
                                    request.activatorAddress,
                                    "Contract activated",
                                    Map.of("status", "ACTIVE"),
                                    result.transactionHash(),
                                    result.blockNumber()
                            );

                    return auditService.logActivity(auditRequest)
                            .map(auditEntry -> Response.ok(Map.of(
                                    "success", true,
                                    "contractId", contractId,
                                    "status", "ACTIVE",
                                    "transactionHash", result.transactionHash(),
                                    "blockNumber", result.blockNumber(),
                                    "gasCharged", result.gasFeeCharged()
                            )).build());
                });
    }

    /**
     * Get contract audit trail
     *
     * GET /api/v11/contracts/ricardian/{contractId}/audit
     */
    @GET
    @Path("/{contractId}/audit")
    public Uni<Response> getAuditTrail(@PathParam("contractId") String contractId) {
        LOG.infof("Getting audit trail for contract: %s", contractId);

        return auditService.getContractAuditTrail(contractId)
                .map(auditTrail -> Response.ok(Map.of(
                        "contractId", contractId,
                        "totalEntries", auditTrail.size(),
                        "auditTrail", auditTrail
                )).build());
    }

    /**
     * Get compliance report
     *
     * GET /api/v11/contracts/ricardian/{contractId}/compliance/{framework}
     */
    @GET
    @Path("/{contractId}/compliance/{framework}")
    public Uni<Response> getComplianceReport(
            @PathParam("contractId") String contractId,
            @PathParam("framework") String framework
    ) {
        LOG.infof("Generating %s compliance report for contract: %s", framework, contractId);

        return auditService.generateComplianceReport(contractId, framework)
                .map(report -> Response.ok(report).build());
    }

    /**
     * Get gas fee rates
     *
     * GET /api/v11/contracts/ricardian/gas-fees
     */
    @GET
    @Path("/gas-fees")
    public Response getGasFees() {
        return Response.ok(consensusService.getAllGasFees()).build();
    }

    // ==================== HELPER METHODS ====================

    private List<ContractParty> parseSuggestedParties(String suggestedPartiesJson) {
        // TODO: Parse JSON properly with Jackson
        // For now, return empty list
        return new ArrayList<>();
    }

    private Map<String, Object> serializeContract(RicardianContract contract) {
        Map<String, Object> data = new HashMap<>();
        data.put("contractId", contract.getContractId());
        data.put("name", contract.getName());
        data.put("version", contract.getVersion());
        data.put("contractType", contract.getContractType());
        data.put("jurisdiction", contract.getJurisdiction());
        data.put("status", contract.getStatus());
        data.put("parties", contract.getParties());
        data.put("terms", contract.getTerms());
        data.put("signatures", contract.getSignatures());
        data.put("isFullySigned", contract.isFullySigned());
        data.put("enforceabilityScore", contract.getEnforceabilityScore());
        data.put("createdAt", contract.getCreatedAt());
        data.put("activatedAt", contract.getActivatedAt());
        return data;
    }

    // ==================== DATA MODELS ====================

    public static class DocumentUploadForm {
        @RestForm
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public InputStream file;

        @RestForm
        public String fileName;

        @RestForm
        public String contractType;

        @RestForm
        public String jurisdiction;

        @RestForm
        public String submitterAddress;

        @RestForm
        public String suggestedParties; // JSON string
    }

    public record AddPartyRequest(
            String name,
            String address,
            String role,
            boolean signatureRequired,
            boolean kycVerified,
            String submitterAddress
    ) {}

    public record SignatureRequest(
            String signerAddress,
            String signature,
            String publicKey
    ) {}

    public record ActivationRequest(
            String activatorAddress
    ) {}

    // Helper method to create ContractSignature (package-private workaround)
    private Object createSignature(String signerAddress, String signature, String publicKey, Instant signedAt) {
        // This is a workaround - ideally ContractSignature should be public
        try {
            Class<?> sigClass = Class.forName("io.aurigraph.v11.contracts.ContractSignature");
            Object sig = sigClass.getDeclaredConstructor().newInstance();

            java.lang.reflect.Method setSignerAddress = sigClass.getDeclaredMethod("setSignerAddress", String.class);
            setSignerAddress.invoke(sig, signerAddress);

            java.lang.reflect.Method setSignature = sigClass.getDeclaredMethod("setSignature", String.class);
            setSignature.invoke(sig, signature);

            java.lang.reflect.Method setPublicKey = sigClass.getDeclaredMethod("setPublicKey", String.class);
            setPublicKey.invoke(sig, publicKey);

            java.lang.reflect.Method setSignedAt = sigClass.getDeclaredMethod("setSignedAt", Instant.class);
            setSignedAt.invoke(sig, signedAt);

            return sig;
        } catch (Exception e) {
            LOG.error("Failed to create signature via reflection", e);
            return null;
        }
    }

    /**
     * AV11-289: Comprehensive upload validation
     *
     * Validates:
     * - File presence and size
     * - File name and extension
     * - Required fields
     * - Contract type validity
     * - Jurisdiction validity
     * - Submitter address format
     */
    private ValidationResult validateUpload(DocumentUploadForm form) {
        List<String> errors = new ArrayList<>();

        // 1. File validation
        if (form.file == null) {
            errors.add("File is required");
        } else {
            try {
                byte[] content = form.file.readAllBytes();

                // File size validation (max 10MB)
                if (content.length > 10 * 1024 * 1024) {
                    errors.add("File size exceeds maximum of 10MB");
                }

                // Minimum file size (1KB)
                if (content.length < 1024) {
                    errors.add("File size is too small (minimum 1KB)");
                }
            } catch (Exception e) {
                errors.add("Failed to read file: " + e.getMessage());
            }
        }

        // 2. File name validation
        if (form.fileName == null || form.fileName.trim().isEmpty()) {
            errors.add("File name is required");
        } else {
            // Check for valid file extensions
            String lowerFileName = form.fileName.toLowerCase();
            List<String> validExtensions = Arrays.asList(".pdf", ".docx", ".doc", ".txt", ".md");
            boolean hasValidExtension = validExtensions.stream()
                    .anyMatch(lowerFileName::endsWith);

            if (!hasValidExtension) {
                errors.add("Invalid file type. Supported: PDF, DOCX, DOC, TXT, MD");
            }

            // File name length check
            if (form.fileName.length() > 255) {
                errors.add("File name is too long (maximum 255 characters)");
            }
        }

        // 3. Contract type validation
        if (form.contractType == null || form.contractType.trim().isEmpty()) {
            errors.add("Contract type is required");
        } else {
            List<String> validTypes = Arrays.asList(
                "SALE_AGREEMENT", "SERVICE_AGREEMENT", "NDA",
                "EMPLOYMENT", "PARTNERSHIP", "LICENSING"
            );
            if (!validTypes.contains(form.contractType.toUpperCase())) {
                errors.add("Invalid contract type. Valid types: " + String.join(", ", validTypes));
            }
        }

        // 4. Jurisdiction validation
        if (form.jurisdiction == null || form.jurisdiction.trim().isEmpty()) {
            errors.add("Jurisdiction is required");
        } else {
            List<String> validJurisdictions = Arrays.asList(
                "US", "UK", "EU", "CA", "AU", "SG", "JP", "INTERNATIONAL"
            );
            if (!validJurisdictions.contains(form.jurisdiction.toUpperCase())) {
                errors.add("Invalid jurisdiction. Valid jurisdictions: " + String.join(", ", validJurisdictions));
            }
        }

        // 5. Submitter address validation
        if (form.submitterAddress == null || form.submitterAddress.trim().isEmpty()) {
            errors.add("Submitter address is required");
        } else {
            // Basic blockchain address format validation (0x + 40 hex chars)
            if (!form.submitterAddress.matches("^0x[a-fA-F0-9]{40}$")) {
                errors.add("Invalid submitter address format (expected: 0x followed by 40 hex characters)");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validation result container
     */
    private static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
