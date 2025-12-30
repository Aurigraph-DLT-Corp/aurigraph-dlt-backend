package io.aurigraph.v11.compliance.erc3643;

/**
 * Exception thrown when compliance rules are violated
 */
public class ComplianceException extends RuntimeException {
    private String complianceCode;
    private String violationType;

    public ComplianceException(String message) {
        super(message);
    }

    public ComplianceException(String message, String complianceCode) {
        super(message);
        this.complianceCode = complianceCode;
    }

    public ComplianceException(String message, String complianceCode, String violationType) {
        super(message);
        this.complianceCode = complianceCode;
        this.violationType = violationType;
    }

    public ComplianceException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getComplianceCode() {
        return complianceCode;
    }

    public String getViolationType() {
        return violationType;
    }
}
