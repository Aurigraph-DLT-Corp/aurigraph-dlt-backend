# Registry Management API

## Overview

The Registry Management API provides a unified interface for searching, listing, and managing entries across multiple registry types in the Aurigraph V11 platform. This API aggregates data from distinct registry systems into a single cohesive interface.

**Version**: 11.5.0  
**Base Path**: `/api/v11/registries`  
**Status**: Production-Ready

## Supported Registry Types

The API manages 5 distinct registry types:

| Type | ID | Description |
|------|-----|-------------|
| Smart Contracts | `smart-contract` | ActiveContracts, Ricardian contracts, contract templates |
| Tokens | `token` | ERC20, ERC721, ERC1155 tokens and RWA-backed tokens |
| Real-World Assets | `rwa` | Tokenized real-world assets (RWATs) |
| Merkle Trees | `merkle-tree` | Cryptographic proofs and Merkle tree states |
| Compliance | `compliance` | Compliance attestations, certifications, audit trails |

## Features

### Multi-Registry Search
- Unified keyword search across all registry types
- Optional type filtering to scope searches
- Pagination support (limit/offset)
- Aggregated results with metadata

### Statistics and Analytics
- Aggregated statistics across all registries
- Per-registry-type detailed statistics
- Verification coverage metrics
- Search analytics and trends
- Health status monitoring

### Entry Management
- List entries by type with pagination
- Verify entry existence across registries
- Retrieve registry summaries
- Get supported registry types

## API Endpoints

### Search Endpoints

#### Unified Multi-Registry Search
```
GET /api/v11/registries/search?keyword=&types=&limit=50&offset=0
```

**Query Parameters**:
- `keyword` (optional): Search term to match against entry names and descriptions
- `types` (optional): Comma-separated list of registry types (default: all types)
- `limit` (optional): Maximum number of results (default: 50, max: 500)
- `offset` (optional): Pagination offset (default: 0)

**Example**:
```bash
curl "https://dlt.aurigraph.io/api/v11/registries/search?keyword=carbon&types=token,rwa&limit=20"
```

**Response** (200 OK):
```json
[
  {
    "entryId": "token-carbon-001",
    "entryType": "TokenRegistry",
    "name": "Carbon Credit Token",
    "registryType": "token",
    "verificationStatus": "VERIFIED",
    "lastUpdated": "2025-11-14T05:00:00Z",
    "metadata": {
      "symbol": "CCT",
      "supply": "1000000"
    }
  }
]
```

### Statistics Endpoints

#### Get Aggregated Statistics
```
GET /api/v11/registries/stats
```

Returns combined statistics across all 5 registry types.

**Response** (200 OK):
```json
{
  "totalRegistries": 5,
  "totalEntries": 15234,
  "totalVerifiedEntries": 12456,
  "verificationCoverage": 81.75,
  "healthStatus": "HEALTHY",
  "avgVerificationTime": 2400.5,
  "lastUpdatedTimestamp": "2025-11-14T05:10:00Z",
  "registryTypeStats": {
    "smart-contract": {
      "registryType": "Smart Contracts",
      "entryCount": 1234,
      "verifiedCount": 1100,
      "percentageOfTotal": 8.08
    },
    "token": {
      "registryType": "Tokens",
      "entryCount": 5678,
      "verifiedCount": 4500,
      "percentageOfTotal": 37.24
    }
  }
}
```

#### Get Type-Specific Statistics
```
GET /api/v11/registries/stats/{type}
```

**Path Parameters**:
- `type`: Registry type ID (smart-contract, token, rwa, merkle-tree, compliance)

**Response** (200 OK):
```json
{
  "registryType": "token",
  "totalEntries": 5678,
  "verifiedEntries": 4500,
  "pendingEntries": 987,
  "rejectedEntries": 123,
  "activeEntries": 5432,
  "avgVerificationTime": 2500.0,
  "lastUpdatedTimestamp": "2025-11-14T05:10:00Z",
  "searchMetrics": {
    "totalSearches": 10234,
    "uniqueSearchTerms": 2345,
    "avgSearchResultCount": 12.5
  },
  "categoryBreakdown": {
    "DeFi": 1234,
    "Stablecoin": 890,
    "Gaming": 456
  }
}
```

### Listing Endpoints

#### List Entries by Type
```
GET /api/v11/registries/list/{type}?limit=50&offset=0
```

**Path Parameters**:
- `type`: Registry type ID (smart-contract, token, rwa, merkle-tree, compliance)

**Query Parameters**:
- `limit` (optional): Maximum number of results (default: 50, max: 500)
- `offset` (optional): Pagination offset (default: 0)

**Response** (200 OK):
Same format as search endpoint results

### Verification Endpoints

#### Verify Entry Across Registries
```
GET /api/v11/registries/verify/{entryId}
```

Checks if an entry ID exists in any of the registered registries.

**Path Parameters**:
- `entryId`: The entry identifier to verify

**Response** (200 OK):
```json
{
  "entryId": "token-carbon-001",
  "timestamp": "2025-11-14T05:12:00Z",
  "verificationStatus": {
    "found_in_contracts": false,
    "found_in_tokens": true,
    "found_in_rwats": false,
    "found_in_compliance": true
  },
  "verified": true
}
```

### Summary Endpoints

#### Get Registry Summary
```
GET /api/v11/registries/summary
```

High-level overview of all registries.

**Response** (200 OK):
```json
{
  "totalRegistries": 5,
  "totalEntries": 15234,
  "verificationCoverage": "81.75%",
  "healthStatus": "HEALTHY",
  "lastUpdated": "2025-11-14T05:10:00Z",
  "registryBreakdown": {
    "smart-contract": {
      "entries": 1234,
      "verified": 1100,
      "verification_rate": "89.15%"
    },
    "token": {
      "entries": 5678,
      "verified": 4500,
      "verification_rate": "79.21%"
    }
  }
}
```

### Information Endpoints

#### Get Supported Registry Types
```
GET /api/v11/registries/info/types
```

**Response** (200 OK):
```json
{
  "count": 5,
  "types": [
    {
      "id": "smart-contract",
      "displayName": "Smart Contracts"
    },
    {
      "id": "token",
      "displayName": "Tokens"
    },
    {
      "id": "rwa",
      "displayName": "Real-World Assets"
    },
    {
      "id": "merkle-tree",
      "displayName": "Merkle Trees"
    },
    {
      "id": "compliance",
      "displayName": "Compliance"
    }
  ]
}
```

#### Health Check
```
GET /api/v11/registries/health
```

**Response** (200 OK):
```json
{
  "status": "UP",
  "service": "Registry Management API",
  "version": "11.5.0"
}
```

## Error Handling

All endpoints return standard HTTP status codes with error messages in JSON format:

```json
{
  "error": "Invalid registry type: xyz. Valid types: smart-contract, token, rwa, merkle-tree, compliance"
}
```

### Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 400 | Bad request (invalid parameters) |
| 404 | Entry not found |
| 500 | Internal server error |

## Search Examples

### Search for all carbon-related entries
```bash
curl "https://dlt.aurigraph.io/api/v11/registries/search?keyword=carbon"
```

### Search only in RWA and Token registries
```bash
curl "https://dlt.aurigraph.io/api/v11/registries/search?keyword=gold&types=rwa,token"
```

### Get verified smart contracts
```bash
curl "https://dlt.aurigraph.io/api/v11/registries/list/smart-contract?limit=10&offset=0"
```

### Verify an entry exists
```bash
curl "https://dlt.aurigraph.io/api/v11/registries/verify/RWAT-12345-67890"
```

### Get detailed token statistics
```bash
curl "https://dlt.aurigraph.io/api/v11/registries/stats/token"
```

## Implementation Details

### RegistryType Enum
Defines all supported registry types with:
- Type ID (e.g., "smart-contract")
- Display name (e.g., "Smart Contracts")
- Validation methods
- Reverse lookup functionality

### RegistrySearchResult DTO
Represents a single search result with:
- Entry ID and type
- Name and description
- Registry type and status
- Verification status
- Last updated timestamp
- Flexible metadata map

### RegistryStatistics DTO
Provides statistics for a specific registry including:
- Entry counts (total, verified, pending, rejected, active)
- Average verification time
- Category and status breakdowns
- Search metrics

### RegistryAggregation DTO
Combines statistics across all registries with:
- Total entries and verification coverage
- Health status determination
- Per-registry-type data
- Top categories tracking

### RegistryManagementService
Core business logic service handling:
- Multi-registry search with aggregation
- Statistics calculation and caching
- Verification across registries
- Type-specific filtering
- Reactive operations with Mutiny Uni

### RegistryManagementResource
REST API resource providing:
- All endpoints with OpenAPI documentation
- Input validation
- Error handling
- Logging at all entry points
- Proper HTTP status codes

## Performance Characteristics

- **Search Performance**: O(n) per registry type, linearly combined
- **Statistics**: Cached with on-demand calculation
- **Pagination**: Efficient with limit/offset
- **Verification**: Parallel queries across registries with Uni.combine()
- **Default Result Limit**: 50 entries (max: 500)

## Reactive Architecture

All endpoints return `Uni<Response>` for non-blocking async operations:

```java
// Search completes asynchronously
registryManagementService.searchAllRegistries(keyword, types, limit, offset)
    .map(results -> Response.ok(results).build())
    .onFailure().recoverWithItem(error -> errorResponse);
```

Failures are gracefully handled with recovery to default/empty results.

## Security Considerations

- Input validation on all parameters
- Type validation against whitelist
- Pagination limits to prevent resource exhaustion
- OpenAPI documentation for API discovery
- Logging of all operations for audit trails
- Error messages don't expose internal details

## Future Enhancements

1. **Caching**: TTL-based cache for statistics
2. **Advanced Filtering**: Support for filters beyond keywords
3. **Sorting**: Results sorting by relevance, date, or status
4. **Export**: CSV/JSON export of search results
5. **Webhooks**: Real-time notifications for registry changes
6. **Scheduled Updates**: Periodic cache refresh
7. **Analytics Dashboard**: Visualization of registry metrics

## Related Components

- `ActiveContractRegistryService`: Smart contract registry
- `TokenRegistryService`: Token registry
- `RWATRegistryService`: Real-world asset registry
- `ComplianceRegistry`: Compliance registry
- `MerkleTreeRegistry`: Merkle tree registry

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 11.5.0 | 2025-11-14 | Initial release - unified multi-registry API |
