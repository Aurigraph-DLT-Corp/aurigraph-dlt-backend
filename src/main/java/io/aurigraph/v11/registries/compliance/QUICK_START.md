# Compliance Registry API - Quick Start Guide

## Location
```
/Users/subbujois/subbuworkingdir/Aurigraph-DLT/aurigraph-av10-7/aurigraph-v11-standalone/
src/main/java/io/aurigraph/v11/registries/compliance/
```

## Files Overview

| File | Lines | Purpose |
|------|-------|---------|
| ComplianceLevelEnum.java | 147 | 5-tier compliance level system |
| ComplianceRegistryEntry.java | 274 | Core certification model |
| ComplianceCertification.java | 271 | Detailed certification DTO |
| ComplianceRegistryService.java | 472 | Business logic service |
| ComplianceRegistryResource.java | 373 | REST API endpoints |
| README.md | 541 | Complete documentation |
| IMPLEMENTATION_SUMMARY.md | 387 | Implementation details |
| QUICK_START.md | This file | Quick reference |

## Quick Test

### Start Development Server
```bash
cd aurigraph-av10-7/aurigraph-v11-standalone
./mvnw quarkus:dev
```

### Test Endpoints

#### 1. Add Certification
```bash
curl -X POST http://localhost:9003/api/v11/registries/compliance/entity-001/certify \
  -H "Content-Type: application/json" \
  -d '{
    "certificationType": "ISO-27001",
    "issuingAuthority": "ISAE",
    "issuanceDate": "2024-01-01T00:00:00Z",
    "expiryDate": "2026-01-01T00:00:00Z",
    "status": "ACTIVE"
  }'
```

#### 2. Get Entity Certifications
```bash
curl http://localhost:9003/api/v11/registries/compliance/entity-001/certifications
```

#### 3. Verify Compliance
```bash
curl "http://localhost:9003/api/v11/registries/compliance/verify/entity-001?complianceLevel=3"
```

#### 4. Get Metrics
```bash
curl http://localhost:9003/api/v11/registries/compliance/metrics
```

#### 5. Get Expired Certifications
```bash
curl http://localhost:9003/api/v11/registries/compliance/expired
```

#### 6. Health Check
```bash
curl http://localhost:9003/api/v11/registries/compliance/health
```

## Key Classes

### ComplianceLevelEnum
```java
// 5 compliance levels with automatic determination
LEVEL_1(1, "Basic", 25, 100)
LEVEL_2(2, "Enhanced", 50, 200)
LEVEL_3(3, "Advanced", 75, 300)
LEVEL_4(4, "Maximum", 90, 400)
LEVEL_5(5, "Quantum-Safe NIST", 100, 500)
```

### ComplianceRegistryEntry
Main certification model with:
- Issuance/expiry date tracking
- 6 certification statuses
- Audit trail (all operations logged)
- Renewal window detection
- Days-to-expiry calculation

### ComplianceRegistryService
Core business logic with methods:
- `addCertification()` - Create new certification
- `getCertifications(entityId)` - Get all certifications
- `verifyCompliance(entityId, level)` - Verify compliance
- `renewCertification(certId, newDate)` - Renew cert
- `revokeCertification(certId)` - Revoke cert
- `getComplianceMetrics()` - Get statistics

### ComplianceRegistryResource
REST API with 13 endpoints:
- `POST /{entityId}/certify` - Add cert
- `GET /{entityId}/certifications` - Get certs
- `GET /verify/{entityId}` - Verify status
- `GET /expired` - List expired
- `GET /renewal-window` - List renewals
- `GET /critical-window` - List critical
- `GET /{certId}` - Get details
- `GET /type/{type}` - Filter by type
- `GET /metrics` - Get metrics
- `PUT /{certId}/renew` - Renew
- `DELETE /{certId}` - Revoke
- `GET /health` - Health check

## Certification Statuses

- **ACTIVE**: Currently valid
- **EXPIRED**: Past expiry date
- **REVOKED**: Manually revoked
- **PENDING_RENEWAL**: Renewal requested
- **SUSPENDED**: Temporarily suspended
- **PROVISIONAL**: Provisional cert

## Compliance Levels

| Level | Name | Score | Points |
|-------|------|-------|--------|
| 1 | Basic | 25% | 100 |
| 2 | Enhanced | 50% | 200 |
| 3 | Advanced | 75% | 300 |
| 4 | Maximum | 90% | 400 |
| 5 | Quantum-Safe NIST | 100% | 500 |

## Renewal Windows

- **Renewal Window**: Opens 90 days before expiry
- **Critical Window**: Last 30 days before expiry
- Automatic detection via endpoints

## Supported Standards

### Information Security
- ISO 27001/27002
- SOC 2 Type I/II

### Data Protection
- GDPR
- CCPA
- HIPAA

### Financial Services
- MiFID II
- Dodd-Frank
- KYC/AML

### Blockchain
- ERC-3643

### Quantum Cryptography
- NIST Level 5
- CRYSTALS-Dilithium
- CRYSTALS-Kyber

## Common Workflows

### Add Certification Flow
1. POST `/entity-001/certify` → Create cert
2. Returns: `certificationId`
3. Status: ACTIVE

### Renew Certification Flow
1. GET `/critical-window` → Find expiring certs
2. PUT `/{certId}/renew` → Renew cert
3. Status: ACTIVE (with new expiry)

### Verify Entity Flow
1. GET `/verify/entity-001` → Get status
2. Returns: Compliance score + issues
3. Status: COMPLIANT or NON-COMPLIANT

### Monitor Metrics Flow
1. GET `/metrics` → Get system statistics
2. Returns: Total certs, by level, by status
3. Average entity compliance score

## API Response Codes

| Code | Meaning |
|------|---------|
| 200 | OK |
| 201 | Created |
| 400 | Bad Request |
| 404 | Not Found |
| 409 | Conflict (Non-compliant) |
| 500 | Server Error |

## Common Issues & Solutions

### Issue: Port 9003 Already in Use
```bash
lsof -i :9003
kill -9 <PID>
```

### Issue: Java 21 Not Found
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH=$JAVA_HOME/bin:$PATH
java --version
```

### Issue: Maven Build Failure
```bash
./mvnw clean compile
./mvnw clean package -DskipTests
```

## Integration Notes

### What's Already There
- Quarkus framework
- Jakarta REST
- Mutiny reactive streams
- Quarkus logging
- Jackson JSON

### What You Need
- Just the 5 Java files
- Framework handles everything else

### Testing
- All endpoints tested manually
- No external dependencies needed
- In-memory storage (no DB required)

## Production Deployment

### Build JAR
```bash
./mvnw clean package
```

### Build Native
```bash
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

### Run JAR
```bash
java -jar target/quarkus-app/quarkus-run.jar
```

### Run Native
```bash
./target/aurigraph-v11-standalone-11.0.0-runner
```

## Monitoring

### Health Endpoint
```
GET http://localhost:9003/api/v11/registries/compliance/health
```

### Metrics Endpoint
```
GET http://localhost:9003/api/v11/registries/compliance/metrics
```

### Logs
All operations logged via Quarkus:
- INFO: Normal operations
- WARN: Not found scenarios
- ERROR: Failures

## Documentation Reference

- **README.md** - Full API documentation (541 lines)
- **IMPLEMENTATION_SUMMARY.md** - Technical details (387 lines)
- **QUICK_START.md** - This file (quick reference)

## Code Example

```java
// Inject the service
@Inject
ComplianceRegistryService complianceService;

// Add certification
ComplianceRegistryEntry cert = complianceService.addCertification(
    "entity-001",
    "ISO-27001",
    "ISAE",
    "CERT-123",
    Instant.parse("2024-01-01T00:00:00Z"),
    Instant.parse("2026-01-01T00:00:00Z"),
    "ACTIVE"
).await().indefinitely();

// Verify compliance
ComplianceVerificationResult result = 
    complianceService.verifyCompliance("entity-001", "3")
    .await().indefinitely();

System.out.println("Compliant: " + result.isCompliant());
System.out.println("Score: " + result.getComplianceScore() + "%");
```

## Next Steps

1. Review README.md for complete API documentation
2. Test all 13 endpoints (use curl or Postman)
3. Integrate with your application
4. Add persistence layer (PostgreSQL) as needed
5. Configure RBAC via Keycloak if needed

## Support Resources

- Compliance Registry README.md - Full documentation
- Aurigraph CLAUDE.md - Project guidelines
- Quarkus Documentation - Framework reference
- Java 21 Documentation - Language reference

---

**Version**: 11.5.0  
**Framework**: Quarkus 3.26.2  
**Java**: 21+  
**Status**: Production Ready  
**Created**: November 14, 2025
