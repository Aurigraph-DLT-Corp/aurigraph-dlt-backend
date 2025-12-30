package io.aurigraph.v11.compliance.erc3643;

import java.time.Instant;

/**
 * Identity Verification Request/Data
 * Contains verified KYC information for an investor
 */
public class IdentityVerification {
    private String address;
    private String kycLevel;           // BASIC, ENHANCED, CERTIFIED
    private String country;
    private String documentHash;       // SHA-256 hash of KYC document
    private String verifierName;       // Name of KYC provider
    private Instant documentExpiryDate;
    private String firstName;
    private String lastName;
    private String documentType;       // PASSPORT, ID, DRIVER_LICENSE, etc.
    private String documentNumber;
    private Instant documentIssueDate;

    // Constructors
    public IdentityVerification() {}

    public IdentityVerification(String address, String kycLevel, String country,
                               String documentHash, String verifierName) {
        this.address = address;
        this.kycLevel = kycLevel;
        this.country = country;
        this.documentHash = documentHash;
        this.verifierName = verifierName;
    }

    // Getters and setters
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getKycLevel() { return kycLevel; }
    public void setKycLevel(String kycLevel) { this.kycLevel = kycLevel; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDocumentHash() { return documentHash; }
    public void setDocumentHash(String documentHash) { this.documentHash = documentHash; }

    public String getVerifierName() { return verifierName; }
    public void setVerifierName(String verifierName) { this.verifierName = verifierName; }

    public Instant getDocumentExpiryDate() { return documentExpiryDate; }
    public void setDocumentExpiryDate(Instant documentExpiryDate) {
        this.documentExpiryDate = documentExpiryDate;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public Instant getDocumentIssueDate() { return documentIssueDate; }
    public void setDocumentIssueDate(Instant documentIssueDate) {
        this.documentIssueDate = documentIssueDate;
    }
}
