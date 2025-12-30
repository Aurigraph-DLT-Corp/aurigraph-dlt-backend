# Asset Traceability API

Complete implementation of the Asset Traceability API for tracking the full lifecycle of tokenized assets with comprehensive audit trails and ownership history.

## Overview

The Asset Traceability API provides REST endpoints for managing and tracking tokenized assets from creation through ownership transfers to final disposition. The system maintains a complete, immutable audit trail and ownership chain for compliance and transparency.

**Location**: `/io/aurigraph/v11/assettracking/`

## Features

- **Asset Lifecycle Tracking**: Create, manage, and monitor asset traces from inception
- **Ownership Chain**: Complete history of all ownership changes with timestamps and transaction hashes
- **Audit Trail**: Immutable log of all operations (CREATED, TRANSFERRED, VERIFIED, etc.)
- **Fast Lookups**: Reverse indexes for quick searches by asset type, owner, or asset ID
- **Compliance Status**: Track compliance verification status for regulatory requirements
- **Thread-Safe**: All operations use ConcurrentHashMap for safe concurrent access
- **Reactive**: Full Quarkus Uni<> reactive support for non-blocking operations

## Architecture

### Components

1. **AssetTraceabilityResource** (REST API)
   - Handles all HTTP endpoints
   - Input validation and error handling
   - OpenAPI documentation annotations

2. **AssetTraceabilityService** (Business Logic)
   - In-memory storage using ConcurrentHashMap
   - Reverse indexes for fast queries
   - Transaction hash generation (SHA-256)
   - Audit trail management

3. **Data Models**
   - **AssetTrace**: Main asset lifecycle object
   - **OwnershipRecord**: Single ownership entry in the chain
   - **AuditTrailEntry**: Single audit log entry

### Data Storage

```
assetTraces
├── trace_<uuid> -> AssetTrace
│   ├── assetId
│   ├── assetName
│   ├── assetType
│   ├── valuation
│   ├── currentOwner
│   ├── ownershipHistory[] -> OwnershipRecord
│   ├── auditTrail[] -> AuditTrailEntry
│   ├── complianceStatus
│   └── metadata{}

Indexes (for fast lookups)
├── assetTypeIndex: assetType -> Set<traceId>
├── ownerIndex: owner -> Set<traceId>
└── assetIdIndex: assetId -> Set<traceId>
```

## REST API Endpoints

### Create Asset Trace
```
POST /api/v11/assets/traceability/create
  ?assetId=<id>
  &assetName=<name>
  &assetType=<type>
  &valuation=<amount>
  &owner=<owner>

Response: HTTP 201
{
  "traceId": "trace_...",
  "assetId": "asset_...",
  "assetName": "Real Estate Property",
  "assetType": "REAL_ESTATE",
  "valuation": 1500000.0,
  "currencyCode": "USD",
  "currentOwner": "owner1",
  "complianceStatus": "PENDING_VERIFICATION",
  "lastUpdated": "2025-11-14T05:00:00Z",
  "ownershipHistory": [...],
  "auditTrail": [...]
}
```

### Get Asset Trace
```
GET /api/v11/assets/traceability/{traceId}

Response: HTTP 200
{
  "traceId": "trace_...",
  ...complete asset trace details...
}

Error: HTTP 404
{
  "error": "Asset trace not found"
}
```

### Search Assets
```
GET /api/v11/assets/traceability/search
  ?assetType=REAL_ESTATE
  &owner=owner1
  &minVal=100000
  &maxVal=2000000
  &limit=50
  &offset=0

Response: HTTP 200
[
  {...asset trace...},
  {...asset trace...}
]
```

### Transfer Ownership
```
POST /api/v11/assets/traceability/{traceId}/transfer
  ?fromOwner=<old_owner>
  &toOwner=<new_owner>
  &percentage=<100.0 for full transfer>

Response: HTTP 200
{
  "owner": "owner2",
  "acquisitionDate": "2025-11-14T05:01:00Z",
  "disposalDate": null,
  "percentage": 100.0,
  "txHash": "abc123..."
}

Error: HTTP 404 or HTTP 400
```

### Get Ownership History
```
GET /api/v11/assets/traceability/{traceId}/history

Response: HTTP 200
[
  {
    "owner": "owner1",
    "acquisitionDate": "2025-11-14T05:00:00Z",
    "disposalDate": "2025-11-14T05:01:00Z",
    "percentage": 100.0,
    "txHash": "..."
  },
  {
    "owner": "owner2",
    "acquisitionDate": "2025-11-14T05:01:00Z",
    "disposalDate": null,
    "percentage": 100.0,
    "txHash": "..."
  }
]
```

### Get Audit Trail
```
GET /api/v11/assets/traceability/{traceId}/audit

Response: HTTP 200
[
  {
    "entryId": "audit_...",
    "action": "CREATED",
    "actor": "owner1",
    "timestamp": "2025-11-14T05:00:00Z",
    "status": "SUCCESS",
    "details": {
      "assetId": "...",
      "assetName": "...",
      "assetType": "...",
      "valuation": 1500000.0
    }
  },
  {
    "entryId": "audit_...",
    "action": "TRANSFERRED",
    "actor": "owner1",
    "timestamp": "2025-11-14T05:01:00Z",
    "status": "SUCCESS",
    "details": {
      "from": "owner1",
      "to": "owner2",
      "percentage": 100.0,
      "txHash": "..."
    }
  }
]
```

### Health Check
```
GET /api/v11/assets/traceability/health

Response: HTTP 200
{
  "status": "UP",
  "service": "AssetTraceability"
}
```

## Data Models

### AssetTrace
Main object representing a single asset's complete lifecycle.

**Fields**:
- `traceId` (String): Unique trace identifier (generated as UUID)
- `assetId` (String): Asset identifier
- `assetName` (String): Human-readable asset name
- `assetType` (String): Asset type classification (REAL_ESTATE, COMMODITY, SECURITY, ART, CARBON_CREDIT, BOND, EQUITY, etc.)
- `valuation` (Double): Asset valuation in base currency
- `currencyCode` (String): Currency code (default: USD)
- `currentOwner` (String): Current owner identifier
- `ownershipHistory` (List<OwnershipRecord>): Complete ownership chain
- `auditTrail` (List<AuditTrailEntry>): Complete audit log
- `complianceStatus` (String): Compliance state (PENDING_VERIFICATION, VERIFIED, TOKENIZED)
- `lastUpdated` (Instant): Last modification timestamp
- `metadata` (Map<String, Object>): Additional metadata (version, region, custom fields)

### OwnershipRecord
Single entry in an asset's ownership history.

**Fields**:
- `owner` (String): Owner identifier
- `acquisitionDate` (Instant): When ownership was acquired
- `disposalDate` (Instant): When ownership was disposed (null if still owned)
- `percentage` (Double): Ownership percentage (0-100)
- `txHash` (String): Transaction hash (SHA-256)

**Helper Methods**:
- `isActive()`: Check if this ownership is currently active
- `getDurationDays()`: Calculate ownership duration in days

### AuditTrailEntry
Single entry in an asset's audit log.

**Fields**:
- `entryId` (String): Unique entry identifier
- `action` (String): Action type (CREATED, TRANSFERRED, VERIFIED, VALUATED, TOKENIZED, AUDITED, COMPLIANCE_CHECK, ERROR)
- `actor` (String): Actor who performed the action
- `timestamp` (Instant): When the action occurred
- `details` (Map<String, Object>): Action-specific details
- `status` (String): Action status (SUCCESS, FAILED, ERROR)

**Helper Methods**:
- `isSuccessful()`: Check if action succeeded
- `isFailed()`: Check if action failed
- `getActionDescription()`: Get user-friendly description

## Service Methods

### AssetTraceabilityService

```java
// Create new asset trace
public Uni<AssetTrace> createAssetTrace(String assetId, String assetName, String assetType,
                                       Double valuation, String owner)

// Get asset trace by ID
public Uni<Optional<AssetTrace>> getAssetTrace(String traceId)

// Search assets with filters
public Uni<List<AssetTrace>> searchAssets(String assetType, String owner, Double minVal, 
                                         Double maxVal, int limit, int offset)

// Record ownership transfer
public Uni<OwnershipRecord> transferOwnership(String traceId, String fromOwner, String toOwner,
                                             Double percentage)

// Get ownership history
public Uni<Optional<List<OwnershipRecord>>> getOwnershipHistory(String traceId)

// Get audit trail
public Uni<Optional<List<AuditTrailEntry>>> getAuditTrail(String traceId)

// Statistics
public long getTotalAssetTraces()
public long getTotalUniqueOwners()
public long getTotalUniqueAssetTypes()
```

## Performance Characteristics

- **Creation**: O(1) - Direct hash map insertion
- **Lookup by ID**: O(1) - Direct hash map get
- **Search by Type**: O(n) where n is assets of that type
- **Search by Owner**: O(n) where n is assets owned by that owner
- **Transfer**: O(1) - Direct append to ownership history
- **Audit Trail**: O(1) - Direct append

## Thread Safety

All operations are thread-safe:
- `ConcurrentHashMap` for primary storage
- `ConcurrentHashMap.newKeySet()` for reverse indexes
- Atomic reads/writes using `Optional` pattern
- No external synchronization required

## Error Handling

### HTTP Status Codes

- **201 Created**: Asset trace successfully created
- **200 OK**: Operation successful, data returned
- **400 Bad Request**: Invalid parameters (missing required fields, invalid percentage)
- **404 Not Found**: Asset trace not found
- **500 Internal Server Error**: Server error during processing

### Error Response Format

```json
{
  "error": "Description of what went wrong"
}
```

## Usage Examples

### Create a Real Estate Asset
```bash
curl -X POST "http://localhost:9003/api/v11/assets/traceability/create" \
  -H "Content-Type: application/json" \
  -d "assetId=re_001&assetName=Downtown Office Building&assetType=REAL_ESTATE&valuation=5000000&owner=investor1"
```

### Transfer Ownership
```bash
curl -X POST "http://localhost:9003/api/v11/assets/traceability/trace_abc123/transfer" \
  -H "Content-Type: application/json" \
  -d "fromOwner=investor1&toOwner=investor2&percentage=100"
```

### Search by Asset Type
```bash
curl "http://localhost:9003/api/v11/assets/traceability/search?assetType=REAL_ESTATE&limit=10"
```

### Get Complete Audit Trail
```bash
curl "http://localhost:9003/api/v11/assets/traceability/trace_abc123/audit"
```

## Compliance and Regulatory

The Asset Traceability API supports compliance requirements:

- **Immutable Audit Trail**: All operations are logged with timestamps
- **Ownership Chain**: Complete history of all ownership changes
- **Compliance Status**: Track verification status
- **Metadata Tracking**: Custom fields for regulatory compliance
- **Transaction Hash**: SHA-256 hash of each ownership record for integrity verification

## Integration Points

### With RWA Registry
The Asset Traceability API integrates with RWA (Real-World Assets) Registry for:
- Asset valuation tracking
- Compliance verification
- Tokenization management

### With Token Management
Integration with TokenManagementService for:
- Token creation and distribution
- Token balance tracking
- Transfer validation

## Future Enhancements

1. **Event Streaming**: Publish audit trail events to Kafka/Event Bus
2. **Blockchain Integration**: Store hashes on blockchain for immutability proof
3. **Advanced Analytics**: Dashboard for asset portfolio analysis
4. **Compliance Reports**: Automated compliance reporting
5. **Fractional Ownership**: Enhanced support for split ownership records
6. **Multi-Currency**: Support for multiple currencies with automatic conversion

## Testing

Run unit tests:
```bash
cd aurigraph-v11-standalone
./mvnw test -Dtest=AssetTraceabilityTest
```

Run integration tests:
```bash
./mvnw verify
```

## Dependencies

- **Quarkus**: 3.29.0+
- **Jakarta EE**: Latest (REST, CDI)
- **Jackson**: JSON serialization (included with Quarkus)
- **Java**: 21+ (Virtual Threads support)

## Author

Aurigraph V11 Development Team

## Version

1.0.0 (Production-Ready)
