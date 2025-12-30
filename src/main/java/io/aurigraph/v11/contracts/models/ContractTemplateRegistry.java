package io.aurigraph.v11.contracts.models;

import io.aurigraph.v11.merkle.MerkleProof;
import io.aurigraph.v11.merkle.MerkleTreeRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contract Template Registry Service with Merkle Tree Support
 *
 * Registry for managing contract templates with cryptographic verification.
 * Provides template storage, retrieval, and Merkle tree-based integrity.
 *
 * Features:
 * - Contract template management
 * - Template verification and validation
 * - Merkle tree cryptographic verification
 * - Proof generation and verification
 * - Root hash tracking for template integrity
 * - Category-based template organization
 *
 * @version 11.5.0
 * @since 2025-10-25 - AV11-458: ContractTemplateRegistry Merkle Tree
 */
@ApplicationScoped
public class ContractTemplateRegistry extends MerkleTreeRegistry<ContractTemplate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContractTemplateRegistry.class);

    public ContractTemplateRegistry() {
        super();
        initializeDefaultTemplates();
    }

    @Override
    protected String serializeValue(ContractTemplate template) {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%b",
            template.getTemplateId(),
            template.getName(),
            template.getCategory(),
            template.getContractType(),
            template.getAssetType(),
            template.getVersion(),
            template.getVerificationHash(),
            template.isVerified()
        );
    }

    /**
     * Register a new contract template
     */
    public Uni<ContractTemplate> registerTemplate(ContractTemplate template) {
        return Uni.createFrom().item(() -> {
            LOGGER.info("Registering contract template: {} ({})",
                template.getName(), template.getTemplateId());

            // Set timestamps
            if (template.getCreatedAt() == null) {
                template.setCreatedAt(Instant.now());
            }
            template.setUpdatedAt(Instant.now());

            // Generate verification hash if not set
            if (template.getVerificationHash() == null) {
                template.setVerificationHash(generateVerificationHash(template));
            }

            return template;
        }).flatMap(t -> add(t.getTemplateId(), t).map(success -> t));
    }

    /**
     * Get template by ID
     */
    public Uni<ContractTemplate> getTemplate(String templateId) {
        return get(templateId).onItem().ifNull().failWith(() ->
            new TemplateNotFoundException("Template not found: " + templateId));
    }

    /**
     * Get all templates
     */
    public Uni<List<ContractTemplate>> getAllTemplates() {
        return getAll();
    }

    /**
     * Get templates by category
     */
    public Uni<List<ContractTemplate>> getTemplatesByCategory(String category) {
        return getAll().map(templates ->
            templates.stream()
                .filter(t -> category.equals(t.getCategory()))
                .collect(Collectors.toList())
        );
    }

    /**
     * Get templates by contract type
     */
    public Uni<List<ContractTemplate>> getTemplatesByType(String contractType) {
        return getAll().map(templates ->
            templates.stream()
                .filter(t -> contractType.equals(t.getContractType()))
                .collect(Collectors.toList())
        );
    }

    /**
     * Get verified templates only
     */
    public Uni<List<ContractTemplate>> getVerifiedTemplates() {
        return getAll().map(templates ->
            templates.stream()
                .filter(ContractTemplate::isVerified)
                .collect(Collectors.toList())
        );
    }

    /**
     * Update template
     */
    public Uni<ContractTemplate> updateTemplate(String templateId, ContractTemplate updatedTemplate) {
        return getTemplate(templateId).map(existing -> {
            updatedTemplate.setUpdatedAt(Instant.now());

            // Regenerate verification hash on update
            updatedTemplate.setVerificationHash(generateVerificationHash(updatedTemplate));

            registry.put(templateId, updatedTemplate);
            LOGGER.info("Template updated: {}", templateId);
            return updatedTemplate;
        }).flatMap(t -> add(templateId, t).map(success -> t));
    }

    /**
     * Remove template
     */
    public Uni<Boolean> removeTemplate(String templateId) {
        return remove(templateId).map(success -> {
            if (success) {
                LOGGER.info("Template removed: {}", templateId);
            }
            return success;
        });
    }

    /**
     * Verify template integrity
     */
    public Uni<Boolean> verifyTemplate(String templateId) {
        return getTemplate(templateId).map(template -> {
            String currentHash = generateVerificationHash(template);
            boolean valid = currentHash.equals(template.getVerificationHash());

            if (valid) {
                template.setVerified(true);
                registry.put(templateId, template);
            }

            LOGGER.info("Template {} verification: {}", templateId, valid ? "PASSED" : "FAILED");
            return valid;
        });
    }

    /**
     * Check if template exists
     */
    public Uni<Boolean> templateExists(String templateId) {
        return get(templateId).map(Objects::nonNull);
    }

    /**
     * Generate Merkle proof for a template
     */
    public Uni<MerkleProof.ProofData> getProof(String templateId) {
        return generateProof(templateId).map(MerkleProof::toProofData);
    }

    /**
     * Verify a Merkle proof
     */
    public Uni<VerificationResponse> verifyMerkleProof(MerkleProof.ProofData proofData) {
        return verifyProof(proofData.toMerkleProof()).map(valid ->
            new VerificationResponse(valid, valid ? "Proof verified successfully" : "Invalid proof")
        );
    }

    /**
     * Get current Merkle root hash
     */
    public Uni<RootHashResponse> getMerkleRootHash() {
        return getRootHash().flatMap(rootHash ->
            getTreeStats().map(stats -> new RootHashResponse(
                rootHash,
                Instant.now(),
                stats.getEntryCount(),
                stats.getTreeHeight()
            ))
        );
    }

    /**
     * Get Merkle tree statistics
     */
    public Uni<MerkleTreeStats> getMerkleTreeStats() {
        return getTreeStats();
    }

    /**
     * Get registry statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalTemplates", registry.size());
        stats.put("verifiedTemplates", registry.values().stream()
                .filter(ContractTemplate::isVerified).count());

        // Count by category
        Map<String, Long> byCategory = registry.values().stream()
                .collect(Collectors.groupingBy(
                        ContractTemplate::getCategory,
                        Collectors.counting()
                ));
        stats.put("templatesByCategory", byCategory);

        // Count by contract type
        Map<String, Long> byType = registry.values().stream()
                .collect(Collectors.groupingBy(
                        ContractTemplate::getContractType,
                        Collectors.counting()
                ));
        stats.put("templatesByType", byType);

        return stats;
    }

    // Private helper methods
    private String generateVerificationHash(ContractTemplate template) {
        String data = String.format("%s|%s|%s|%s|%s",
            template.getTemplateId(),
            template.getSourceCode(),
            template.getBytecode(),
            template.getVersion(),
            template.getCreatedAt()
        );
        return MerkleHashUtil.sha3Hash(data);
    }

    private void initializeDefaultTemplates() {
        // RWA Template
        ContractTemplate rwaTemplate = new ContractTemplate();
        rwaTemplate.setTemplateId("RWA_STANDARD_V1");
        rwaTemplate.setName("Real World Asset Tokenization");
        rwaTemplate.setCategory("RWA");
        rwaTemplate.setContractType("RWA_TOKENIZATION");
        rwaTemplate.setAssetType("REAL_ESTATE");
        rwaTemplate.setJurisdiction("US_DELAWARE");
        rwaTemplate.setLegalText("This Ricardian Contract represents the tokenization of a real-world asset...");
        rwaTemplate.setDescription("Standard template for tokenizing real-world assets");
        rwaTemplate.setVersion("1.0.0");
        rwaTemplate.setCreatedAt(Instant.now());
        rwaTemplate.setVerified(true);
        registerTemplate(rwaTemplate).await().indefinitely();

        // Carbon Credit Template
        ContractTemplate carbonTemplate = new ContractTemplate();
        carbonTemplate.setTemplateId("CARBON_CREDIT_V1");
        carbonTemplate.setName("Carbon Credit Trading");
        carbonTemplate.setCategory("ENVIRONMENTAL");
        carbonTemplate.setContractType("CARBON_TRADING");
        carbonTemplate.setAssetType("CARBON_CREDIT");
        carbonTemplate.setJurisdiction("INTERNATIONAL");
        carbonTemplate.setLegalText("This Ricardian Contract governs the trading of verified carbon credits...");
        carbonTemplate.setDescription("Template for carbon credit trading and verification");
        carbonTemplate.setVersion("1.0.0");
        carbonTemplate.setCreatedAt(Instant.now());
        carbonTemplate.setVerified(true);
        registerTemplate(carbonTemplate).await().indefinitely();

        // Supply Chain Template
        ContractTemplate supplyTemplate = new ContractTemplate();
        supplyTemplate.setTemplateId("SUPPLY_CHAIN_V1");
        supplyTemplate.setName("Supply Chain Tracking");
        supplyTemplate.setCategory("LOGISTICS");
        supplyTemplate.setContractType("SUPPLY_CHAIN");
        supplyTemplate.setAssetType("SUPPLY_CHAIN");
        supplyTemplate.setJurisdiction("INTERNATIONAL");
        supplyTemplate.setLegalText("This Ricardian Contract tracks goods through the supply chain...");
        supplyTemplate.setDescription("Template for supply chain tracking and verification");
        supplyTemplate.setVersion("1.0.0");
        supplyTemplate.setCreatedAt(Instant.now());
        supplyTemplate.setVerified(true);
        registerTemplate(supplyTemplate).await().indefinitely();

        LOGGER.info("Initialized {} default contract templates", registry.size());
    }

    // Custom Exception
    public static class TemplateNotFoundException extends RuntimeException {
        public TemplateNotFoundException(String message) {
            super(message);
        }
    }

    // Response Classes
    public static class VerificationResponse {
        private final boolean valid;
        private final String message;

        public VerificationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class RootHashResponse {
        private final String rootHash;
        private final Instant timestamp;
        private final int entryCount;
        private final int treeHeight;

        public RootHashResponse(String rootHash, Instant timestamp, int entryCount, int treeHeight) {
            this.rootHash = rootHash;
            this.timestamp = timestamp;
            this.entryCount = entryCount;
            this.treeHeight = treeHeight;
        }

        public String getRootHash() {
            return rootHash;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public int getEntryCount() {
            return entryCount;
        }

        public int getTreeHeight() {
            return treeHeight;
        }
    }
}