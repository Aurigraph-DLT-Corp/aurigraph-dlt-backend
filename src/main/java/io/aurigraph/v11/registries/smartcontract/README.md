# Smart Contract Registry API

## Overview

The Smart Contract Registry API provides a comprehensive system for managing smart contract registrations, deployments, and lifecycle tracking on the Aurigraph V11 blockchain platform.

**Location**: `src/main/java/io/aurigraph/v11/registries/smartcontract/`

**Version**: 11.5.0

**API Base Path**: `/api/v11/registries/smart-contract`

## Files

### 1. **ContractStatusEnum.java** (Enum)
Defines the smart contract lifecycle states with validation logic.

**States**:
- `DRAFT` - Contract in development, pending compilation
- `DEPLOYED` - Contract deployed to blockchain, not yet activated
- `ACTIVE` - Contract actively running and accepting transactions
- `AUDITED` - Contract passed security audit verification
- `DEPRECATED` - Contract superseded by newer version
- `FAILED` - Deployment or execution failed

**Methods**:
- `isActive()` - Check if contract can execute transactions
- `isTerminal()` - Check if contract reached terminal state
- `isDeployable()` - Check if contract is ready for deployment
- `fromValue(String)` - Parse status from string
- `isValidTransition(from, to)` - Validate status transitions

### 2. **ContractDeploymentInfo.java** (DTO)
Data Transfer Object for contract deployment information.

**Fields**:
- `deploymentAddress` - Contract address on blockchain
- `deploymentTxHash` - Deployment transaction hash
- `deploymentDate` - When contract was deployed
- `deploymentNetwork` - Network name (mainnet, testnet, etc)
- `gasUsed` - Gas consumed during deployment
- `codeHash` - SHA-256 hash of contract code
- `compilerVersion` - Compiler version used
- `gasPrice` - Gas price at deployment time
- `deployer` - Address that deployed contract
- `totalCost` - Total deployment cost
- `blockHeight` - Block number of deployment
- `confirmations` - Number of block confirmations
- `status` - Deployment status

**Methods**:
- `markAsSuccessful()` - Mark deployment as successful
- `markAsFailed()` - Mark deployment as failed
- `calculateCost()` - Calculate deployment cost
- `hasRequiredConfirmations(int)` - Check confirmation threshold
- `verifyCodeHash(String)` - Verify code hash matches

### 3. **SmartContractRegistryEntry.java** (DTO)
Main registry entry model representing a contract registration.

**Fields**:
- `contractId` - Unique contract identifier
- `contractName` - Human-readable name
- `description` - Contract purpose/description
- `deploymentAddress` - Blockchain address
- `deploymentTxHash` - Deployment transaction hash
- `codeHash` - SHA-256 hash of contract code
- `currentStatus` - Current lifecycle status
- `registeredAt` - Registration timestamp
- `linkedAssetCount` - Count of linked assets
- `linkedAssets` - Set of linked asset IDs
- `deploymentInfo` - Full deployment details
- `metadata` - Additional key-value pairs
- `owner` - Contract owner address
- `version` - Contract version
- `language` - Programming language (SOLIDITY, JAVA, etc)
- `auditStatus` - Audit status (PENDING, PASSED, FAILED)
- `lastModified` - Last modification timestamp
- `verificationHash` - Hash for integrity verification
- `tags` - Classification tags

**Methods**:
- `linkAsset(String)` - Link asset to contract
- `unlinkAsset(String)` - Unlink asset from contract
- `isAssetLinked(String)` - Check if asset is linked
- `addMetadata(key, value)` - Add metadata entry
- `removeMetadata(String)` - Remove metadata entry
- `addTag(String)` - Add classification tag
- `isActive()` - Check if contract is active
- `isTerminal()` - Check if contract reached terminal state
- `updateStatus(ContractStatusEnum)` - Update status with validation

### 4. **SmartContractRegistryService.java** (Service)
Core business logic service for registry operations.

**In-Memory Storage**:
- `ConcurrentHashMap<String, SmartContractRegistryEntry>` - Main registry
- `ConcurrentHashMap<String, List<RegistryAuditEntry>>` - Audit trail
- `ConcurrentHashMap<String, Set<String>>` - Asset-to-contract index

**Methods**:

#### Core Registry Operations
- `registerContract(...)` → `Uni<SmartContractRegistryEntry>`
  - Register new contract with full deployment info
  - Validates contract ID is unique
  - Returns 201 Created status

- `getContractDetails(String contractId)` → `Uni<SmartContractRegistryEntry>`
  - Retrieve complete contract information
  - Throws `ContractRegistryException` if not found

- `searchContracts(name, status, limit, offset)` → `Uni<List<SmartContractRegistryEntry>>`
  - Search by name (substring, case-insensitive)
  - Filter by status
  - Pagination support
  - Sorted by registration date (newest first)

- `updateContractStatus(contractId, newStatus)` → `Uni<SmartContractRegistryEntry>`
  - Update contract status with validation
  - Validates valid transitions
  - Automatic audit trail recording

- `removeContract(contractId)` → `Uni<Boolean>`
  - Remove contract from registry
  - Cleans up all asset links
  - Records audit entry

#### Asset Linking Operations
- `linkAsset(contractId, assetId)` → `Uni<SmartContractRegistryEntry>`
  - Link an asset to a contract
  - Maintains bidirectional index
  - Records audit entry

- `unlinkAsset(contractId, assetId)` → `Uni<SmartContractRegistryEntry>`
  - Remove asset link from contract
  - Updates bidirectional index
  - Records audit entry

- `getLinkedAssets(contractId)` → `Uni<Set<String>>`
  - Retrieve all assets linked to contract

- `getContractsForAsset(assetId)` → `Uni<List<SmartContractRegistryEntry>>`
  - Get all contracts linked to an asset

#### Statistics and Audit
- `getContractStatistics()` → `Uni<Map<String, Object>>`
  - Total contracts
  - Active contracts count
  - Audited contracts count
  - Deprecated contracts count
  - Status breakdown
  - Asset linkage statistics
  - Average assets per contract

- `getAuditTrail(contractId)` → `Uni<List<Map>>`
  - Retrieve complete audit history
  - Operations: REGISTER, STATUS_UPDATE, LINK_ASSET, UNLINK_ASSET, REMOVE

**Exception**:
- `ContractRegistryException` - All registry operation errors

### 5. **SmartContractRegistryResource.java** (REST API)
RESTful API endpoints for registry operations.

**Base Path**: `/api/v11/registries/smart-contract`

#### Endpoints

##### Core Operations
- **POST** `/register` → `201 Created`
  - Register new contract
  - Request: `contractId`, `contractName`, `description`, `deploymentAddress`, `deploymentTxHash`, `codeHash`, `status`
  - Response: Registry entry with full details

- **GET** `/{contractId}` → `200 OK` or `404 Not Found`
  - Get contract details
  - Response: Registry entry

- **GET** `/search?name=&status=&limit=10&offset=0` → `200 OK`
  - Search contracts with filtering and pagination
  - Query parameters: `name`, `status`, `limit`, `offset`
  - Response: List of matching entries with pagination info

- **PUT** `/{contractId}/status` → `200 OK` or `400 Bad Request`
  - Update contract status
  - Request: `{ "status": "ACTIVE" }`
  - Response: Updated registry entry

- **DELETE** `/{contractId}` → `200 OK` or `404 Not Found`
  - Remove contract from registry
  - Response: Confirmation message

##### Asset Linking
- **POST** `/{contractId}/assets/{assetId}` → `200 OK`
  - Link asset to contract
  - Response: Updated registry entry

- **DELETE** `/{contractId}/assets/{assetId}` → `200 OK`
  - Unlink asset from contract
  - Response: Updated registry entry

- **GET** `/{contractId}/assets` → `200 OK` or `404 Not Found`
  - Get all assets linked to contract
  - Response: Asset list with count

- **GET** `/asset/{assetId}/contracts` → `200 OK`
  - Get all contracts for an asset
  - Response: Contract list with count

##### Statistics and Health
- **GET** `/statistics` → `200 OK`
  - Get registry statistics
  - Response: Statistics map

- **GET** `/{contractId}/audit` → `200 OK` or `404 Not Found`
  - Get audit trail for contract
  - Response: Audit entries list

- **GET** `/health` → `200 OK`
  - Check registry health status
  - Response: Health info

- **GET** `/info` → `200 OK`
  - Get API information and available endpoints
  - Response: Service info with endpoint list

## HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | Success | GET contract, update status, get assets |
| 201 | Created | Register new contract |
| 400 | Bad Request | Invalid input, invalid status transition |
| 404 | Not Found | Contract not found |
| 409 | Conflict | Contract ID already exists |
| 500 | Server Error | Unexpected service error |

## Example Usage

### Register Contract
```bash
curl -X POST http://localhost:9003/api/v11/registries/smart-contract/register \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": "sc-001",
    "contractName": "Token Contract",
    "description": "ERC-20 compatible token",
    "deploymentAddress": "0x1234567890abcdef",
    "deploymentTxHash": "0xabcdef1234567890",
    "codeHash": "sha256hash",
    "status": "ACTIVE"
  }'
```

### Search Contracts
```bash
curl http://localhost:9003/api/v11/registries/smart-contract/search?name=token&status=ACTIVE&limit=10
```

### Link Asset
```bash
curl -X POST http://localhost:9003/api/v11/registries/smart-contract/sc-001/assets/asset-123
```

### Get Statistics
```bash
curl http://localhost:9003/api/v11/registries/smart-contract/statistics
```

## Features

- Full contract lifecycle management
- Deployment tracking with transaction hashes
- Code integrity verification (SHA-256 hashing)
- Asset linking with bidirectional indexing
- Comprehensive audit trail
- Search and filtering capabilities
- Pagination support
- Status transition validation
- In-memory concurrent storage
- Reactive Uni<> operations for non-blocking calls
- Structured error handling
- OpenAPI documentation annotations

## Integration Points

This registry integrates with:
- **RWATRegistry** - Real-World Asset Tokens
- **AssetTracking** - Asset management system
- **Transaction Service** - Transaction validation
- **Audit System** - Compliance tracking

## Performance Considerations

- All operations use `ConcurrentHashMap` for thread-safe access
- Search operations use streams for efficient filtering
- Bidirectional asset-to-contract index for fast lookups
- In-memory storage (consider RocksDB/database for production scale)
- Audit trail append-only for integrity

## Future Enhancements

- Database persistence (PostgreSQL/RocksDB)
- Full-text search indexing
- Merkle tree proofs for contract verification
- Contract versioning and rollback
- Formal verification integration
- Gas optimization metrics
- Access control and permissions
- Event streaming for contract changes
- gRPC service interface
