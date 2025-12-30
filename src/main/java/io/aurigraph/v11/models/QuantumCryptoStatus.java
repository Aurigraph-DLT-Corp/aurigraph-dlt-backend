package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Quantum cryptography status model
 * Used by /api/v11/security/quantum endpoint
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
public class QuantumCryptoStatus {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("algorithms")
    private AlgorithmsInfo algorithms;

    @JsonProperty("key_generation")
    private KeyGenerationStats keyGeneration;

    @JsonProperty("signatures")
    private SignatureStats signatures;

    @JsonProperty("performance")
    private PerformanceMetrics performance;

    @JsonProperty("security_level")
    private SecurityLevel securityLevel;

    @JsonProperty("status")
    private String status; // "active", "degraded", "offline"

    // Constructor
    public QuantumCryptoStatus() {
        this.timestamp = Instant.now();
        this.status = "active";
    }

    // Getters and Setters
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public AlgorithmsInfo getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(AlgorithmsInfo algorithms) {
        this.algorithms = algorithms;
    }

    public KeyGenerationStats getKeyGeneration() {
        return keyGeneration;
    }

    public void setKeyGeneration(KeyGenerationStats keyGeneration) {
        this.keyGeneration = keyGeneration;
    }

    public SignatureStats getSignatures() {
        return signatures;
    }

    public void setSignatures(SignatureStats signatures) {
        this.signatures = signatures;
    }

    public PerformanceMetrics getPerformance() {
        return performance;
    }

    public void setPerformance(PerformanceMetrics performance) {
        this.performance = performance;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Post-quantum algorithms information
     */
    public static class AlgorithmsInfo {
        @JsonProperty("key_encapsulation")
        private AlgorithmDetails keyEncapsulation; // Kyber

        @JsonProperty("digital_signature")
        private AlgorithmDetails digitalSignature; // Dilithium

        @JsonProperty("supported_algorithms")
        private List<String> supportedAlgorithms;

        public AlgorithmsInfo() {}

        // Getters and setters
        public AlgorithmDetails getKeyEncapsulation() { return keyEncapsulation; }
        public void setKeyEncapsulation(AlgorithmDetails keyEncapsulation) { this.keyEncapsulation = keyEncapsulation; }

        public AlgorithmDetails getDigitalSignature() { return digitalSignature; }
        public void setDigitalSignature(AlgorithmDetails digitalSignature) { this.digitalSignature = digitalSignature; }

        public List<String> getSupportedAlgorithms() { return supportedAlgorithms; }
        public void setSupportedAlgorithms(List<String> supportedAlgorithms) { this.supportedAlgorithms = supportedAlgorithms; }
    }

    /**
     * Algorithm details
     */
    public static class AlgorithmDetails {
        @JsonProperty("name")
        private String name;

        @JsonProperty("variant")
        private String variant;

        @JsonProperty("nist_level")
        private int nistLevel; // 1-5

        @JsonProperty("key_size_bits")
        private int keySizeBits;

        @JsonProperty("enabled")
        private boolean enabled;

        @JsonProperty("description")
        private String description;

        public AlgorithmDetails() {}

        public AlgorithmDetails(String name, String variant, int nistLevel, int keySizeBits, boolean enabled, String description) {
            this.name = name;
            this.variant = variant;
            this.nistLevel = nistLevel;
            this.keySizeBits = keySizeBits;
            this.enabled = enabled;
            this.description = description;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVariant() { return variant; }
        public void setVariant(String variant) { this.variant = variant; }

        public int getNistLevel() { return nistLevel; }
        public void setNistLevel(int nistLevel) { this.nistLevel = nistLevel; }

        public int getKeySizeBits() { return keySizeBits; }
        public void setKeySizeBits(int keySizeBits) { this.keySizeBits = keySizeBits; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * Key generation statistics
     */
    public static class KeyGenerationStats {
        @JsonProperty("total_keys_generated")
        private long totalKeysGenerated;

        @JsonProperty("keys_per_second")
        private double keysPerSecond;

        @JsonProperty("average_generation_time_ms")
        private double averageGenerationTimeMs;

        @JsonProperty("last_generation")
        private Instant lastGeneration;

        @JsonProperty("key_types")
        private Map<String, Long> keyTypes; // e.g., {"kyber": 1000, "dilithium": 800}

        public KeyGenerationStats() {}

        // Getters and setters
        public long getTotalKeysGenerated() { return totalKeysGenerated; }
        public void setTotalKeysGenerated(long totalKeysGenerated) { this.totalKeysGenerated = totalKeysGenerated; }

        public double getKeysPerSecond() { return keysPerSecond; }
        public void setKeysPerSecond(double keysPerSecond) { this.keysPerSecond = keysPerSecond; }

        public double getAverageGenerationTimeMs() { return averageGenerationTimeMs; }
        public void setAverageGenerationTimeMs(double averageGenerationTimeMs) { this.averageGenerationTimeMs = averageGenerationTimeMs; }

        public Instant getLastGeneration() { return lastGeneration; }
        public void setLastGeneration(Instant lastGeneration) { this.lastGeneration = lastGeneration; }

        public Map<String, Long> getKeyTypes() { return keyTypes; }
        public void setKeyTypes(Map<String, Long> keyTypes) { this.keyTypes = keyTypes; }
    }

    /**
     * Digital signature statistics
     */
    public static class SignatureStats {
        @JsonProperty("total_signatures")
        private long totalSignatures;

        @JsonProperty("total_verifications")
        private long totalVerifications;

        @JsonProperty("signatures_per_second")
        private double signaturesPerSecond;

        @JsonProperty("verifications_per_second")
        private double verificationsPerSecond;

        @JsonProperty("average_sign_time_ms")
        private double averageSignTimeMs;

        @JsonProperty("average_verify_time_ms")
        private double averageVerifyTimeMs;

        @JsonProperty("verification_success_rate")
        private double verificationSuccessRate; // 0.0 - 1.0

        public SignatureStats() {}

        // Getters and setters
        public long getTotalSignatures() { return totalSignatures; }
        public void setTotalSignatures(long totalSignatures) { this.totalSignatures = totalSignatures; }

        public long getTotalVerifications() { return totalVerifications; }
        public void setTotalVerifications(long totalVerifications) { this.totalVerifications = totalVerifications; }

        public double getSignaturesPerSecond() { return signaturesPerSecond; }
        public void setSignaturesPerSecond(double signaturesPerSecond) { this.signaturesPerSecond = signaturesPerSecond; }

        public double getVerificationsPerSecond() { return verificationsPerSecond; }
        public void setVerificationsPerSecond(double verificationsPerSecond) { this.verificationsPerSecond = verificationsPerSecond; }

        public double getAverageSignTimeMs() { return averageSignTimeMs; }
        public void setAverageSignTimeMs(double averageSignTimeMs) { this.averageSignTimeMs = averageSignTimeMs; }

        public double getAverageVerifyTimeMs() { return averageVerifyTimeMs; }
        public void setAverageVerifyTimeMs(double averageVerifyTimeMs) { this.averageVerifyTimeMs = averageVerifyTimeMs; }

        public double getVerificationSuccessRate() { return verificationSuccessRate; }
        public void setVerificationSuccessRate(double verificationSuccessRate) { this.verificationSuccessRate = verificationSuccessRate; }
    }

    /**
     * Performance metrics
     */
    public static class PerformanceMetrics {
        @JsonProperty("throughput_ops_per_second")
        private double throughputOpsPerSecond;

        @JsonProperty("cpu_usage_percent")
        private double cpuUsagePercent;

        @JsonProperty("memory_usage_mb")
        private double memoryUsageMb;

        @JsonProperty("latency_p50_ms")
        private double latencyP50Ms;

        @JsonProperty("latency_p95_ms")
        private double latencyP95Ms;

        @JsonProperty("latency_p99_ms")
        private double latencyP99Ms;

        public PerformanceMetrics() {}

        // Getters and setters
        public double getThroughputOpsPerSecond() { return throughputOpsPerSecond; }
        public void setThroughputOpsPerSecond(double throughputOpsPerSecond) { this.throughputOpsPerSecond = throughputOpsPerSecond; }

        public double getCpuUsagePercent() { return cpuUsagePercent; }
        public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }

        public double getMemoryUsageMb() { return memoryUsageMb; }
        public void setMemoryUsageMb(double memoryUsageMb) { this.memoryUsageMb = memoryUsageMb; }

        public double getLatencyP50Ms() { return latencyP50Ms; }
        public void setLatencyP50Ms(double latencyP50Ms) { this.latencyP50Ms = latencyP50Ms; }

        public double getLatencyP95Ms() { return latencyP95Ms; }
        public void setLatencyP95Ms(double latencyP95Ms) { this.latencyP95Ms = latencyP95Ms; }

        public double getLatencyP99Ms() { return latencyP99Ms; }
        public void setLatencyP99Ms(double latencyP99Ms) { this.latencyP99Ms = latencyP99Ms; }
    }

    /**
     * Security level information
     */
    public static class SecurityLevel {
        @JsonProperty("nist_level")
        private int nistLevel; // 1-5

        @JsonProperty("quantum_resistance")
        private String quantumResistance; // "high", "medium", "low"

        @JsonProperty("classical_equivalent_bits")
        private int classicalEquivalentBits; // e.g., 256 bits

        @JsonProperty("compliance")
        private List<String> compliance; // ["NIST", "ISO", "FIPS"]

        @JsonProperty("certification_status")
        private String certificationStatus; // "certified", "pending", "in_review"

        public SecurityLevel() {}

        public SecurityLevel(int nistLevel, String quantumResistance, int classicalEquivalentBits) {
            this.nistLevel = nistLevel;
            this.quantumResistance = quantumResistance;
            this.classicalEquivalentBits = classicalEquivalentBits;
        }

        // Getters and setters
        public int getNistLevel() { return nistLevel; }
        public void setNistLevel(int nistLevel) { this.nistLevel = nistLevel; }

        public String getQuantumResistance() { return quantumResistance; }
        public void setQuantumResistance(String quantumResistance) { this.quantumResistance = quantumResistance; }

        public int getClassicalEquivalentBits() { return classicalEquivalentBits; }
        public void setClassicalEquivalentBits(int classicalEquivalentBits) { this.classicalEquivalentBits = classicalEquivalentBits; }

        public List<String> getCompliance() { return compliance; }
        public void setCompliance(List<String> compliance) { this.compliance = compliance; }

        public String getCertificationStatus() { return certificationStatus; }
        public void setCertificationStatus(String certificationStatus) { this.certificationStatus = certificationStatus; }
    }
}
