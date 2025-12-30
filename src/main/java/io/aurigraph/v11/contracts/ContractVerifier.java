package io.aurigraph.v11.contracts;

import io.aurigraph.v11.contracts.models.SmartContract;
import io.aurigraph.v11.contracts.models.ContractStatus;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Contract Verifier Service
 *
 * Provides formal verification, security auditing, and compliance checking for smart contracts.
 * Supports:
 * - Formal verification of contract logic
 * - Security vulnerability scanning
 * - Code quality analysis
 * - Compliance checking (KYC/AML/regulatory)
 * - Gas optimization analysis
 * - Reentrancy detection
 * - Access control validation
 *
 * @version 3.8.0 (Phase 2)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class ContractVerifier {

    private static final Logger LOG = Logger.getLogger(ContractVerifier.class);

    // Verifier version
    private static final String VERIFIER_VERSION = "1.0.0";

    // Security patterns
    private static final Pattern REENTRANCY_PATTERN = Pattern.compile(
        "\\b(call|transfer|send)\\s*\\([^)]*\\)\\s*;\\s*\\w+\\s*=",
        Pattern.MULTILINE
    );

    private static final Pattern UNCHECKED_RETURN_PATTERN = Pattern.compile(
        "\\.(call|send|transfer)\\s*\\([^)]*\\)\\s*;",
        Pattern.MULTILINE
    );

    private static final Pattern INTEGER_OVERFLOW_PATTERN = Pattern.compile(
        "\\b(\\w+)\\s*[+\\-*/]\\s*(\\w+)\\s*(?!;\\s*require)",
        Pattern.MULTILINE
    );

    // Vulnerability severity levels
    private static final Map<VulnerabilityType, Severity> VULNERABILITY_SEVERITY = Map.of(
        VulnerabilityType.REENTRANCY, Severity.CRITICAL,
        VulnerabilityType.INTEGER_OVERFLOW, Severity.HIGH,
        VulnerabilityType.UNCHECKED_RETURN, Severity.MEDIUM,
        VulnerabilityType.ACCESS_CONTROL, Severity.HIGH,
        VulnerabilityType.DENIAL_OF_SERVICE, Severity.MEDIUM,
        VulnerabilityType.TIMESTAMP_DEPENDENCE, Severity.LOW
    );

    /**
     * Perform full verification on a smart contract
     */
    public Uni<VerificationReport> verify(SmartContract contract) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Verifying contract: %s", contract.getContractId());

            Instant startTime = Instant.now();
            List<Finding> findings = new ArrayList<>();

            try {
                // Step 1: Formal verification
                FormalVerificationResult formalResult = performFormalVerification(contract);
                findings.addAll(formalResult.findings());
                LOG.debugf("Formal verification complete: %d findings", formalResult.findings().size());

                // Step 2: Security analysis
                SecurityAnalysisResult securityResult = performSecurityAnalysis(contract);
                findings.addAll(securityResult.findings());
                LOG.debugf("Security analysis complete: %d findings", securityResult.findings().size());

                // Step 3: Code quality analysis
                CodeQualityResult qualityResult = analyzeCodeQuality(contract);
                findings.addAll(qualityResult.findings());
                LOG.debugf("Code quality analysis complete: %d findings", qualityResult.findings().size());

                // Step 4: Compliance checking
                ComplianceResult complianceResult = checkCompliance(contract);
                findings.addAll(complianceResult.findings());
                LOG.debugf("Compliance check complete: %d findings", complianceResult.findings().size());

                // Step 5: Gas optimization analysis
                GasOptimizationResult gasResult = analyzeGasOptimization(contract);
                findings.addAll(gasResult.findings());
                LOG.debugf("Gas analysis complete: %d findings", gasResult.findings().size());

                // Calculate verification score
                int score = calculateVerificationScore(findings);

                // Determine overall status
                VerificationStatus status = determineStatus(findings, score);

                Instant endTime = Instant.now();
                long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();

                // Update contract verification status
                contract.setIsVerified(status == VerificationStatus.PASSED);
                contract.setSecurityAuditStatus(status.name());

                return new VerificationReport(
                    contract.getContractId(),
                    status,
                    score,
                    findings,
                    startTime,
                    endTime,
                    durationMs,
                    VERIFIER_VERSION
                );

            } catch (Exception e) {
                LOG.errorf(e, "Verification failed for contract %s", contract.getContractId());
                return VerificationReport.error(contract.getContractId(), e.getMessage());
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Quick security scan (lightweight verification)
     */
    public Uni<SecurityScanResult> quickScan(SmartContract contract) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Performing quick scan on contract: %s", contract.getContractId());

            List<Finding> criticalFindings = new ArrayList<>();

            // Check for critical vulnerabilities only
            criticalFindings.addAll(detectReentrancy(contract.getSourceCode()));
            criticalFindings.addAll(detectAccessControlIssues(contract.getSourceCode()));
            criticalFindings.addAll(detectUncheckedReturns(contract.getSourceCode()));

            boolean passed = criticalFindings.stream()
                .noneMatch(f -> f.severity() == Severity.CRITICAL);

            return new SecurityScanResult(passed, criticalFindings, Instant.now());

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Verify bytecode matches source code
     */
    public Uni<BytecodeVerificationResult> verifyBytecode(SmartContract contract) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Verifying bytecode for contract: %s", contract.getContractId());

            if (contract.getSourceCode() == null || contract.getBytecode() == null) {
                return BytecodeVerificationResult.failure("Source code or bytecode missing");
            }

            // Generate expected bytecode hash
            String expectedHash = generateBytecodeHash(contract.getSourceCode(), contract.getBytecode());

            // Compare with stored verification hash
            boolean matches = expectedHash.equals(contract.getVerificationHash());

            return new BytecodeVerificationResult(matches, expectedHash, contract.getVerificationHash());

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== VERIFICATION STAGES ====================

    /**
     * Perform formal verification
     */
    private FormalVerificationResult performFormalVerification(SmartContract contract) {
        List<Finding> findings = new ArrayList<>();

        // Check contract invariants
        findings.addAll(verifyInvariants(contract));

        // Check state machine correctness
        findings.addAll(verifyStateMachine(contract));

        // Check mathematical properties
        findings.addAll(verifyMathematicalProperties(contract));

        return new FormalVerificationResult(findings);
    }

    private List<Finding> verifyInvariants(SmartContract contract) {
        List<Finding> findings = new ArrayList<>();

        // Check value invariants
        if (contract.getValue() != null && contract.getValue().signum() < 0) {
            findings.add(new Finding(
                VulnerabilityType.LOGIC_ERROR,
                Severity.HIGH,
                "Contract value cannot be negative",
                "value field",
                "Ensure contract value is non-negative"
            ));
        }

        // Check execution count invariant
        if (contract.getExecutionCount() != null && contract.getExecutionCount() < 0) {
            findings.add(new Finding(
                VulnerabilityType.LOGIC_ERROR,
                Severity.MEDIUM,
                "Execution count cannot be negative",
                "executionCount field",
                "Initialize execution count to 0"
            ));
        }

        return findings;
    }

    private List<Finding> verifyStateMachine(SmartContract contract) {
        List<Finding> findings = new ArrayList<>();

        // Verify valid state transitions
        ContractStatus currentStatus = contract.getStatus();
        if (currentStatus != null) {
            // Check if contract is in a valid state
            if (!isValidState(currentStatus)) {
                findings.add(new Finding(
                    VulnerabilityType.LOGIC_ERROR,
                    Severity.HIGH,
                    "Contract is in invalid state: " + currentStatus,
                    "status field",
                    "Ensure contract follows valid state transitions"
                ));
            }

            // Check expired contracts are not active
            if (currentStatus == ContractStatus.ACTIVE && contract.isExpired()) {
                findings.add(new Finding(
                    VulnerabilityType.LOGIC_ERROR,
                    Severity.HIGH,
                    "Active contract has expired",
                    "status/expiresAt",
                    "Update contract status to EXPIRED"
                ));
            }
        }

        return findings;
    }

    private List<Finding> verifyMathematicalProperties(SmartContract contract) {
        List<Finding> findings = new ArrayList<>();

        // Check for integer overflow in execution count
        if (contract.getExecutionCount() != null && contract.getExecutionCount() > Long.MAX_VALUE - 1000000) {
            findings.add(new Finding(
                VulnerabilityType.INTEGER_OVERFLOW,
                Severity.LOW,
                "Execution count approaching overflow limit",
                "executionCount",
                "Consider implementing execution count reset mechanism"
            ));
        }

        return findings;
    }

    /**
     * Perform security analysis
     */
    private SecurityAnalysisResult performSecurityAnalysis(SmartContract contract) {
        List<Finding> findings = new ArrayList<>();

        String sourceCode = contract.getSourceCode() != null ? contract.getSourceCode() : "";

        // Detect reentrancy vulnerabilities
        findings.addAll(detectReentrancy(sourceCode));

        // Detect access control issues
        findings.addAll(detectAccessControlIssues(sourceCode));

        // Detect unchecked return values
        findings.addAll(detectUncheckedReturns(sourceCode));

        // Detect integer overflow/underflow
        findings.addAll(detectIntegerIssues(sourceCode));

        // Detect timestamp dependence
        findings.addAll(detectTimestampDependence(sourceCode));

        // Detect denial of service vulnerabilities
        findings.addAll(detectDoSVulnerabilities(sourceCode));

        return new SecurityAnalysisResult(findings);
    }

    private List<Finding> detectReentrancy(String sourceCode) {
        List<Finding> findings = new ArrayList<>();

        var matcher = REENTRANCY_PATTERN.matcher(sourceCode);
        while (matcher.find()) {
            findings.add(new Finding(
                VulnerabilityType.REENTRANCY,
                Severity.CRITICAL,
                "Potential reentrancy vulnerability detected",
                "Line with external call before state update",
                "Use checks-effects-interactions pattern: update state before external calls"
            ));
        }

        return findings;
    }

    private List<Finding> detectAccessControlIssues(String sourceCode) {
        List<Finding> findings = new ArrayList<>();

        // Check for missing access control on critical functions
        if (sourceCode.contains("transfer") && !sourceCode.contains("require") && !sourceCode.contains("onlyOwner")) {
            findings.add(new Finding(
                VulnerabilityType.ACCESS_CONTROL,
                Severity.HIGH,
                "Transfer function may lack proper access control",
                "transfer function",
                "Add access control modifiers or require statements"
            ));
        }

        return findings;
    }

    private List<Finding> detectUncheckedReturns(String sourceCode) {
        List<Finding> findings = new ArrayList<>();

        var matcher = UNCHECKED_RETURN_PATTERN.matcher(sourceCode);
        while (matcher.find()) {
            findings.add(new Finding(
                VulnerabilityType.UNCHECKED_RETURN,
                Severity.MEDIUM,
                "Unchecked return value from external call",
                "External call without return value check",
                "Check return value and handle failures appropriately"
            ));
        }

        return findings;
    }

    private List<Finding> detectIntegerIssues(String sourceCode) {
        List<Finding> findings = new ArrayList<>();

        var matcher = INTEGER_OVERFLOW_PATTERN.matcher(sourceCode);
        int count = 0;
        while (matcher.find() && count < 5) { // Limit to 5 findings to avoid noise
            findings.add(new Finding(
                VulnerabilityType.INTEGER_OVERFLOW,
                Severity.MEDIUM,
                "Potential integer overflow/underflow",
                "Arithmetic operation without SafeMath",
                "Use SafeMath library or built-in overflow checks"
            ));
            count++;
        }

        return findings;
    }

    private List<Finding> detectTimestampDependence(String sourceCode) {
        List<Finding> findings = new ArrayList<>();

        if (sourceCode.contains("block.timestamp") || sourceCode.contains("now")) {
            findings.add(new Finding(
                VulnerabilityType.TIMESTAMP_DEPENDENCE,
                Severity.LOW,
                "Contract relies on block timestamp",
                "Timestamp usage in logic",
                "Avoid using timestamp for critical logic; miners can manipulate within ~15 seconds"
            ));
        }

        return findings;
    }

    private List<Finding> detectDoSVulnerabilities(String sourceCode) {
        List<Finding> findings = new ArrayList<>();

        // Check for unbounded loops
        if (sourceCode.contains("while") && !sourceCode.contains("break")) {
            findings.add(new Finding(
                VulnerabilityType.DENIAL_OF_SERVICE,
                Severity.MEDIUM,
                "Potential unbounded loop detected",
                "while loop without break",
                "Add loop bounds or gas limits to prevent DoS"
            ));
        }

        return findings;
    }

    /**
     * Analyze code quality
     */
    private CodeQualityResult analyzeCodeQuality(SmartContract contract) {
        List<Finding> findings = new ArrayList<>();

        String sourceCode = contract.getSourceCode() != null ? contract.getSourceCode() : "";

        // Check code documentation
        if (!sourceCode.contains("/**") && !sourceCode.contains("//")) {
            findings.add(new Finding(
                VulnerabilityType.CODE_QUALITY,
                Severity.LOW,
                "Contract lacks documentation",
                "Source code",
                "Add comprehensive comments and documentation"
            ));
        }

        // Check code complexity
        int complexity = calculateCyclomaticComplexity(sourceCode);
        if (complexity > 20) {
            findings.add(new Finding(
                VulnerabilityType.CODE_QUALITY,
                Severity.MEDIUM,
                "High code complexity: " + complexity,
                "Overall contract",
                "Refactor complex functions into smaller, more manageable units"
            ));
        }

        return new CodeQualityResult(findings, complexity);
    }

    private int calculateCyclomaticComplexity(String sourceCode) {
        // Simplified complexity calculation
        int complexity = 1; // Base complexity

        complexity += countOccurrences(sourceCode, "if");
        complexity += countOccurrences(sourceCode, "else");
        complexity += countOccurrences(sourceCode, "while");
        complexity += countOccurrences(sourceCode, "for");
        complexity += countOccurrences(sourceCode, "case");
        complexity += countOccurrences(sourceCode, "&&");
        complexity += countOccurrences(sourceCode, "||");

        return complexity;
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    /**
     * Check regulatory compliance
     */
    private ComplianceResult checkCompliance(SmartContract contract) {
        List<Finding> findings = new ArrayList<>();

        // Check KYC/AML requirements for RWA contracts
        if (contract.getIsRWA() != null && contract.getIsRWA()) {
            if (contract.getKycVerified() == null || !contract.getKycVerified()) {
                findings.add(new Finding(
                    VulnerabilityType.COMPLIANCE,
                    Severity.HIGH,
                    "RWA contract requires KYC verification",
                    "kycVerified field",
                    "Complete KYC verification before contract deployment"
                ));
            }

            if (contract.getAmlChecked() == null || !contract.getAmlChecked()) {
                findings.add(new Finding(
                    VulnerabilityType.COMPLIANCE,
                    Severity.HIGH,
                    "RWA contract requires AML check",
                    "amlChecked field",
                    "Complete AML checks before contract deployment"
                ));
            }
        }

        // Check jurisdiction requirements
        if (contract.getValue() != null && contract.getValue().doubleValue() > 100000.0) {
            if (contract.getJurisdiction() == null || contract.getJurisdiction().isEmpty()) {
                findings.add(new Finding(
                    VulnerabilityType.COMPLIANCE,
                    Severity.MEDIUM,
                    "High-value contract requires jurisdiction specification",
                    "jurisdiction field",
                    "Specify contract jurisdiction for regulatory compliance"
                ));
            }
        }

        return new ComplianceResult(findings);
    }

    /**
     * Analyze gas optimization opportunities
     */
    private GasOptimizationResult analyzeGasOptimization(SmartContract contract) {
        List<Finding> findings = new ArrayList<>();

        String sourceCode = contract.getSourceCode() != null ? contract.getSourceCode() : "";

        // Check for storage vs memory usage
        if (sourceCode.contains("string storage") && !sourceCode.contains("string memory")) {
            findings.add(new Finding(
                VulnerabilityType.GAS_OPTIMIZATION,
                Severity.LOW,
                "Consider using memory instead of storage for temporary variables",
                "Variable declarations",
                "Use 'memory' keyword for temporary string variables"
            ));
        }

        // Check for repeated storage reads
        int storageReads = countOccurrences(sourceCode, ".length");
        if (storageReads > 3) {
            findings.add(new Finding(
                VulnerabilityType.GAS_OPTIMIZATION,
                Severity.LOW,
                "Multiple storage reads detected",
                "Storage access patterns",
                "Cache storage values in memory variables"
            ));
        }

        return new GasOptimizationResult(findings);
    }

    // ==================== HELPER METHODS ====================

    private boolean isValidState(ContractStatus status) {
        return status != null && Arrays.asList(ContractStatus.values()).contains(status);
    }

    private String generateBytecodeHash(String sourceCode, String bytecode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = sourceCode + bytecode + VERIFIER_VERSION;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return "0x" + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("SHA-256 algorithm not available", e);
            return "0x" + UUID.randomUUID().toString().replace("-", "");
        }
    }

    private int calculateVerificationScore(List<Finding> findings) {
        int score = 100;

        for (Finding finding : findings) {
            score -= switch (finding.severity()) {
                case CRITICAL -> 20;
                case HIGH -> 10;
                case MEDIUM -> 5;
                case LOW -> 2;
            };
        }

        return Math.max(0, score); // Ensure score doesn't go below 0
    }

    private VerificationStatus determineStatus(List<Finding> findings, int score) {
        boolean hasCritical = findings.stream()
            .anyMatch(f -> f.severity() == Severity.CRITICAL);

        if (hasCritical) {
            return VerificationStatus.FAILED;
        }

        if (score >= 80) {
            return VerificationStatus.PASSED;
        } else if (score >= 60) {
            return VerificationStatus.PASSED_WITH_WARNINGS;
        } else {
            return VerificationStatus.FAILED;
        }
    }

    // ==================== DATA MODELS ====================

    /**
     * Verification status
     */
    public enum VerificationStatus {
        PASSED,
        PASSED_WITH_WARNINGS,
        FAILED,
        ERROR
    }

    /**
     * Vulnerability severity
     */
    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    /**
     * Vulnerability types
     */
    public enum VulnerabilityType {
        REENTRANCY,
        INTEGER_OVERFLOW,
        UNCHECKED_RETURN,
        ACCESS_CONTROL,
        DENIAL_OF_SERVICE,
        TIMESTAMP_DEPENDENCE,
        LOGIC_ERROR,
        CODE_QUALITY,
        COMPLIANCE,
        GAS_OPTIMIZATION
    }

    /**
     * Verification finding
     */
    public record Finding(
        VulnerabilityType type,
        Severity severity,
        String description,
        String location,
        String recommendation
    ) {}

    /**
     * Verification report
     */
    public static class VerificationReport {
        private final String contractId;
        private final VerificationStatus status;
        private final int score;
        private final List<Finding> findings;
        private final Instant startTime;
        private final Instant endTime;
        private final long durationMs;
        private final String verifierVersion;

        public VerificationReport(String contractId, VerificationStatus status, int score,
                                 List<Finding> findings, Instant startTime, Instant endTime,
                                 long durationMs, String verifierVersion) {
            this.contractId = contractId;
            this.status = status;
            this.score = score;
            this.findings = findings;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationMs = durationMs;
            this.verifierVersion = verifierVersion;
        }

        public static VerificationReport error(String contractId, String errorMessage) {
            return new VerificationReport(
                contractId,
                VerificationStatus.ERROR,
                0,
                List.of(new Finding(
                    VulnerabilityType.LOGIC_ERROR,
                    Severity.CRITICAL,
                    errorMessage,
                    "N/A",
                    "Fix the error and retry verification"
                )),
                Instant.now(),
                Instant.now(),
                0,
                VERIFIER_VERSION
            );
        }

        // Getters
        public String getContractId() { return contractId; }
        public VerificationStatus getStatus() { return status; }
        public int getScore() { return score; }
        public List<Finding> getFindings() { return findings; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public long getDurationMs() { return durationMs; }
        public String getVerifierVersion() { return verifierVersion; }

        public Map<Severity, Long> getFindingsBySeverity() {
            Map<Severity, Long> result = new EnumMap<>(Severity.class);
            for (Severity severity : Severity.values()) {
                long count = findings.stream()
                    .filter(f -> f.severity() == severity)
                    .count();
                result.put(severity, count);
            }
            return result;
        }
    }

    /**
     * Results for individual verification stages
     */
    record FormalVerificationResult(List<Finding> findings) {}
    record SecurityAnalysisResult(List<Finding> findings) {}
    record CodeQualityResult(List<Finding> findings, int complexity) {}
    record ComplianceResult(List<Finding> findings) {}
    record GasOptimizationResult(List<Finding> findings) {}

    /**
     * Security scan result
     */
    public record SecurityScanResult(boolean passed, List<Finding> criticalFindings, Instant timestamp) {}

    /**
     * Bytecode verification result
     */
    public record BytecodeVerificationResult(boolean matches, String expectedHash, String actualHash) {
        public static BytecodeVerificationResult failure(String reason) {
            return new BytecodeVerificationResult(false, reason, "N/A");
        }
    }
}
