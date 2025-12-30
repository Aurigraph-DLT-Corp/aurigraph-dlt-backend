# Compliance Registry API

## Overview

The Compliance Registry API manages compliance certifications, regulatory status, and compliance verification across entities in the Aurigraph V11 blockchain platform. It provides a comprehensive framework for tracking multi-standard compliance certifications with support for lifecycle management, automatic expiry detection, and compliance scoring.

## Features

- **Multi-Standard Support**: ISO, SOC2, NIST, ERC-3643, GDPR, CCPA, HIPAA, PCI DSS
- **Certification Lifecycle Management**: Creation, renewal, expiration, and revocation
- **Compliance Level Scoring**: 5-tier system from Basic (LEVEL_1) to Quantum-Safe NIST (LEVEL_5)
- **Automatic Expiry Detection**: Identify expired and expiring certifications
- **Renewal Window Management**: 90-day renewal windows with critical window alerts
- **Audit Trail Tracking**: Complete history of all compliance operations
- **Entity Compliance Verification**: Multi-criteria compliance checking
- **Compliance Metrics**: Statistical analysis and compliance scoring

## Project Structure

```
compliance/
├── ComplianceLevelEnum.java           # Compliance level definitions (5 tiers)
├── ComplianceRegistryEntry.java       # Certification registry entry model
├── ComplianceCertification.java        # Detailed certification representation
├── ComplianceRegistryService.java      # Core business logic service
├── ComplianceRegistryResource.java     # REST API endpoints
└── README.md                           # This file
```

### File Statistics
- **Total Lines**: 1,537 lines of production-ready code
- **ComplianceLevelEnum.java**: 147 lines
- **ComplianceRegistryEntry.java**: 274 lines
- **ComplianceCertification.java**: 271 lines
- **ComplianceRegistryService.java**: 472 lines
- **ComplianceRegistryResource.java**: 373 lines

## API Endpoints

### Base Path
```
/api/v11/registries/compliance
```

### POST Endpoints

#### Add Certification
```http
POST /api/v11/registries/compliance/{entityId}/certify
Content-Type: application/json

{
  "certificationType": "ISO-27001",
  "issuingAuthority": "ISAE",
  "issuanceDate": "2024-01-01T00:00:00Z",
  "expiryDate": "2026-01-01T00:00:00Z",
  "status": "ACTIVE"
}

Response: 201 CREATED
{
  "certificationId": "CERT-uuid",
  "entityId": "entity-123",
  "certificationType": "ISO-27001",
  "currentStatus": "ACTIVE",
  "complianceLevel": "LEVEL_3",
  "createdAt": "2025-11-14T05:00:00Z"
}
```

### GET Endpoints

#### Get All Certifications for Entity
```http
GET /api/v11/registries/compliance/{entityId}/certifications

Response: 200 OK
{
  "entityId": "entity-123",
  "certifications": [...],
  "count": 5
}
```

#### Verify Compliance Status
```http
GET /api/v11/registries/compliance/verify/{entityId}?complianceLevel=3

Response: 200 OK or 409 CONFLICT
{
  "compliant": true,
  "message": "Entity meets required compliance level",
  "achievedLevel": "LEVEL_3",
  "complianceScore": 75.5,
  "issues": []
}
```

#### Get Expired Certifications
```http
GET /api/v11/registries/compliance/expired

Response: 200 OK
{
  "expired_certifications": [...],
  "count": 2
}
```

#### Get Certifications in Renewal Window
```http
GET /api/v11/registries/compliance/renewal-window

Response: 200 OK
{
  "renewal_certifications": [...],
  "count": 3
}
```

#### Get Certifications in Critical Window (Last 30 Days)
```http
GET /api/v11/registries/compliance/critical-window

Response: 200 OK
{
  "critical_certifications": [...],
  "count": 1
}
```

#### Get Specific Certification
```http
GET /api/v11/registries/compliance/{certId}

Response: 200 OK
{
  "certificationId": "CERT-xyz",
  "entityId": "entity-123",
  "certificationType": "ISO-27001",
  ...
}
```

#### Get Certifications by Type
```http
GET /api/v11/registries/compliance/type/ISO-27001

Response: 200 OK
{
  "type": "ISO-27001",
  "certifications": [...],
  "count": 5
}
```

#### Get Compliance Metrics
```http
GET /api/v11/registries/compliance/metrics

Response: 200 OK
{
  "totalCertifications": 100,
  "activeCertifications": 85,
  "expiredCertifications": 15,
  "renewalCount": 20,
  "totalEntities": 25,
  "averageComplianceScore": 72.5,
  "certificationsByLevel": {
    "LEVEL_1": 10,
    "LEVEL_2": 20,
    "LEVEL_3": 40,
    "LEVEL_4": 20,
    "LEVEL_5": 10
  },
  "certificationsByStatus": {
    "ACTIVE": 85,
    "EXPIRED": 15,
    "REVOKED": 5,
    "PENDING_RENEWAL": 3
  }
}
```

### PUT Endpoints

#### Renew Certification
```http
PUT /api/v11/registries/compliance/{certId}/renew
Content-Type: application/json

{
  "newExpiryDate": "2027-01-01T00:00:00Z",
  "reason": "Annual renewal"
}

Response: 200 OK
{
  "message": "Certification renewed successfully",
  "certification": {
    "certificationId": "CERT-xyz",
    "expiryDate": "2027-01-01T00:00:00Z",
    "currentStatus": "ACTIVE",
    "lastRenewalDate": "2025-11-14T05:00:00Z"
  }
}
```

### DELETE Endpoints

#### Revoke Certification
```http
DELETE /api/v11/registries/compliance/{certId}

Response: 200 OK
{
  "message": "Certification revoked successfully",
  "certification": {
    "certificationId": "CERT-xyz",
    "currentStatus": "REVOKED"
  }
}
```

### Health Check

#### Service Health
```http
GET /api/v11/registries/compliance/health

Response: 200 OK
{
  "status": "healthy",
  "service": "Compliance Registry",
  "timestamp": "2025-11-14T05:00:00Z"
}
```

## Compliance Levels

### LEVEL_1: Basic Compliance (25%)
- Minimal KYC requirements
- Standard AML checks
- Basic risk assessment
- Point Value: 100

### LEVEL_2: Enhanced Compliance (50%)
- Enhanced KYC/AML
- Financial regulations (MiFID II, Dodd-Frank)
- Transaction monitoring
- Point Value: 200

### LEVEL_3: Advanced Compliance (75%)
- ISO 27001/27002 (Information Security)
- SOC 2 Type II (Service organization audits)
- Industry certifications
- ERC-3643 compliance
- Point Value: 300

### LEVEL_4: Maximum Compliance (90%)
- Institutional-grade security
- Multi-jurisdiction compliance (GDPR, CCPA, etc.)
- Enhanced audit trails
- Point Value: 400

### LEVEL_5: Quantum-Safe NIST (100%)
- NIST Level 5 quantum-resistant cryptography
- CRYSTALS-Dilithium digital signatures
- CRYSTALS-Kyber key encapsulation
- TLS 1.3 with post-quantum algorithms
- Point Value: 500

## Certification Status

- **ACTIVE**: Currently valid and in effect
- **EXPIRED**: Past expiry date
- **REVOKED**: Manually revoked
- **PENDING_RENEWAL**: Renewal request submitted
- **SUSPENDED**: Temporarily suspended
- **PROVISIONAL**: Provisional certification

## Data Models

### ComplianceRegistryEntry
Main model for storing certification information:
- `certificationId`: Unique identifier
- `entityId`: Entity owning the certification
- `certificationType`: Type of certification (ISO, SOC2, etc.)
- `issuingAuthority`: Authority that issued the certificate
- `issuanceDate`: Date issued
- `expiryDate`: Date of expiration
- `currentStatus`: Current status
- `complianceLevel`: Compliance level (LEVEL_1-5)
- `certificateNumber`: Official certificate number
- `auditTrail`: History of all operations
- `verificationMetadata`: Additional verification data

### ComplianceCertification
Detailed representation of certification:
- `certificateType`: Type of certificate
- `certificateHash`: SHA-256 hash of document
- `documentUrl`: URL to certificate document
- `verificationStatus`: Verification status
- `verificationMetadata`: Detailed verification data
- `complianceStandards`: Applicable standards
- `jurisdiction`: Geographic/regulatory jurisdiction

## Service Methods

### ComplianceRegistryService

```java
// Add certification
Uni<ComplianceRegistryEntry> addCertification(
    String entityId,
    String certificationType,
    String issuingAuthority,
    String certificationId,
    Instant issuanceDate,
    Instant expiryDate,
    String status
)

// Get certifications for entity
Uni<List<ComplianceRegistryEntry>> getCertifications(String entityId)

// Verify compliance
Uni<ComplianceVerificationResult> verifyCompliance(
    String entityId,
    String complianceLevelStr
)

// Get expired certifications
Uni<List<ComplianceRegistryEntry>> getExpiredCertifications()

// Renew certification
Uni<ComplianceRegistryEntry> renewCertification(
    String certificationId,
    Instant newExpiryDate
)

// Revoke certification
Uni<ComplianceRegistryEntry> revokeCertification(String certificationId)

// Get compliance metrics
Uni<ComplianceMetrics> getComplianceMetrics()

// Get certifications in renewal window
Uni<List<ComplianceRegistryEntry>> getCertificationsInRenewalWindow()

// Get certifications in critical window
Uni<List<ComplianceRegistryEntry>> getCertificationsInCriticalWindow()
```

## Supported Compliance Standards

### Information Security
- ISO 27001 (Information Security Management)
- ISO 27002 (Security Controls)
- SOC 2 Type I & II

### Data Protection
- GDPR (General Data Protection Regulation)
- CCPA (California Consumer Privacy Act)
- HIPAA (Health Insurance Portability and Accountability)

### Financial Services
- MiFID II (Markets in Financial Instruments)
- Dodd-Frank Act
- KYC (Know Your Customer)
- AML (Anti-Money Laundering)

### Blockchain/Token Standards
- ERC-3643 (Compliant Token Standard)

### Quality Management
- ISO 9001 (Quality Management Systems)

### Environmental
- ISO 14001 (Environmental Management)

### Cybersecurity
- NIST SP 800-53 (Security Controls)
- NIST SP 800-171 (Protecting CUI)
- PCI DSS (Payment Card Industry)

### Quantum Cryptography
- NIST Level 5 Post-Quantum Cryptography
- CRYSTALS-Dilithium
- CRYSTALS-Kyber

## Error Handling

The API returns appropriate HTTP status codes:

- `201 Created`: Certification successfully added
- `200 OK`: Successful GET/PUT request
- `400 Bad Request`: Invalid input or missing required fields
- `404 Not Found`: Resource not found
- `409 Conflict`: Compliance verification failed
- `500 Internal Server Error`: Server error

## Audit Trail

Every certification operation is tracked in an audit trail with:
- Timestamp of the event
- Type of event (CREATED, RENEWED, REVOKED, VERIFIED, etc.)
- Description of the action
- User/system that performed the action

Example audit trail entry:
```json
{
  "timestamp": "2025-11-14T05:00:00Z",
  "eventType": "RENEWED",
  "description": "Certification renewed until 2027-01-01T00:00:00Z",
  "performedBy": "SYSTEM"
}
```

## Compliance Scoring

### Entity Compliance Score Calculation
- Scores range from 0-100%
- Based on active certifications
- Expired certifications do not contribute
- Each certification contributes based on its compliance level
- Average is normalized across all certifications

### Formula
```
Entity Score = (Sum of active cert compliance levels / number of certs) × 100
```

## Renewal Management

- **Renewal Window**: Opens 90 days before expiry
- **Critical Window**: Last 30 days before expiry
- **Automatic Detection**: API identifies all certifications needing renewal
- **Renewal Notifications**: Available via critical-window endpoint

## Usage Examples

### Add ISO-27001 Certification
```bash
curl -X POST http://localhost:9003/api/v11/registries/compliance/entity-123/certify \
  -H "Content-Type: application/json" \
  -d '{
    "certificationType": "ISO-27001",
    "issuingAuthority": "ISAE",
    "issuanceDate": "2024-01-01T00:00:00Z",
    "expiryDate": "2026-01-01T00:00:00Z",
    "status": "ACTIVE"
  }'
```

### Verify Entity Compliance
```bash
curl -X GET "http://localhost:9003/api/v11/registries/compliance/verify/entity-123?complianceLevel=3"
```

### Get All Expired Certifications
```bash
curl -X GET http://localhost:9003/api/v11/registries/compliance/expired
```

### Renew a Certification
```bash
curl -X PUT http://localhost:9003/api/v11/registries/compliance/CERT-xyz/renew \
  -H "Content-Type: application/json" \
  -d '{
    "newExpiryDate": "2027-01-01T00:00:00Z",
    "reason": "Annual renewal"
  }'
```

### Get Compliance Metrics
```bash
curl -X GET http://localhost:9003/api/v11/registries/compliance/metrics
```

## Performance Characteristics

- **In-Memory Storage**: ConcurrentHashMap for fast O(1) lookups
- **Reactive Implementation**: Uni<> for non-blocking operations
- **Thread-Safe**: All operations are synchronized for concurrent access
- **Scalability**: Efficient for thousands of certifications

## Security Considerations

1. **Audit Trail**: All operations are logged for accountability
2. **Status Tracking**: Complete lifecycle management
3. **Expiry Alerts**: Automatic detection of expiring certifications
4. **Verification Metadata**: Blockchain-compatible verification data
5. **Revocation Support**: Immediate certification revocation capability

## Testing

The implementation includes comprehensive:
- Input validation
- Error handling
- Null checks
- Status validation
- Date validation (expiry after issuance)

## Logging

All operations are logged using Quarkus logging:
- INFO: Normal operations (add, verify, renew)
- WARN: Not found scenarios
- ERROR: Failures and exceptions

## Future Enhancements

1. **Persistence Layer**: Database integration (PostgreSQL)
2. **Blockchain Verification**: On-chain certification verification
3. **Webhook Notifications**: Real-time alerts for renewal windows
4. **Advanced Filtering**: Complex queries and aggregations
5. **Report Generation**: PDF/Excel compliance reports
6. **Integration with IAM**: Keycloak integration for access control
7. **GraphQL API**: GraphQL endpoint for flexible queries

## Version Information

- **API Version**: 11.5.0
- **Framework**: Quarkus 3.26.2
- **Java Version**: Java 21+
- **Build Tool**: Maven
- **Release Date**: 2025-11-14

## Related Documentation

- [ComplianceLevelEnum.java](ComplianceLevelEnum.java) - Compliance level definitions
- [ComplianceRegistryEntry.java](ComplianceRegistryEntry.java) - Entry model documentation
- [ComplianceCertification.java](ComplianceCertification.java) - Certification model documentation
- [ComplianceRegistryService.java](ComplianceRegistryService.java) - Service implementation
- [ComplianceRegistryResource.java](ComplianceRegistryResource.java) - REST API implementation

## Support

For issues, questions, or suggestions, please refer to the main Aurigraph V11 documentation or create an issue in the project repository.
