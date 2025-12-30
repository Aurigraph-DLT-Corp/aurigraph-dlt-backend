#!/bin/bash

# JIRA Configuration
JIRA_URL="https://aurigraphdlt.atlassian.net"
JIRA_EMAIL="subbu@aurigraph.io"
JIRA_API_TOKEN="ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"
PROJECT_KEY="AV11"

# Create JIRA issue function
create_issue() {
    local summary="$1"
    local description="$2"
    local priority="$3"
    local issue_type="${4:-Task}"

    curl -X POST "${JIRA_URL}/rest/api/3/issue" \
        -H "Content-Type: application/json" \
        -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
        -d "{
            \"fields\": {
                \"project\": {
                    \"key\": \"${PROJECT_KEY}\"
                },
                \"summary\": \"${summary}\",
                \"description\": {
                    \"type\": \"doc\",
                    \"version\": 1,
                    \"content\": [
                        {
                            \"type\": \"paragraph\",
                            \"content\": [
                                {
                                    \"type\": \"text\",
                                    \"text\": \"${description}\"
                                }
                            ]
                        }
                    ]
                },
                \"issuetype\": {
                    \"name\": \"${issue_type}\"
                }
            }
        }"
}

echo "=== Creating JIRA Issues for Aurigraph V11 Bugs ==="
echo ""

# BUG-001: Lombok Annotation Processing Failures
echo "Creating BUG-001: Lombok Annotation Processing Failures..."
create_issue \
    "[BUG-001] Lombok Annotation Processing Failures - 402 Compilation Errors" \
    "Maven compilation fails with 402 errors due to Lombok annotations not generating getter/setter methods. Affects 26 API endpoints (Contract Registry, RWAT Registry, Token Management). Partial fix applied: 12 contract model classes fixed manually. Remaining: service classes, bridge models, gRPC services, token models. Estimated fix time: 6-8 hours." \
    "Highest"

echo ""

# BUG-002: Missing ContractStatus Enum Values
echo "Creating BUG-002: Missing ContractStatus Enum Values..."
create_issue \
    "[BUG-002] Missing ContractStatus Enum Values (PAUSED, DEPLOYED)" \
    "Code references ContractStatus.PAUSED and ContractStatus.DEPLOYED which don't exist in enum definition. Locations: ActiveContractService.java:392, 410, 516. Current enum only has: DRAFT, ACTIVE, TERMINATED, INVALID. Fix: Add PAUSED and DEPLOYED values. Estimated fix time: 10 minutes." \
    "High"

echo ""

# BUG-003: ExecutionResult Constructor Mismatch
echo "Creating BUG-003: ExecutionResult Constructor Mismatch..."
create_issue \
    "[BUG-003] ExecutionResult Constructor Mismatch - Wrong Parameter Count" \
    "Code calls ExecutionResult constructor with 4 parameters but constructor expects 6 parameters. Location: ActiveContractService.java:221. Current call passes (executionId, errorMessage, timestamp, message). Expected: (executionId, status, message, result, executionTimeMs, gasUsed). Fix: Add overloaded constructor for error cases. Estimated fix time: 15 minutes." \
    "Medium"

echo ""

# BUG-004: Private Method Access
echo "Creating BUG-004: Private Method Access in QuantumCryptoService..."
create_issue \
    "[BUG-004] verifyDilithiumSignature() Has Private Access" \
    "verifyDilithiumSignature() method in QuantumCryptoService has private access but is called from ActiveContractService.java:288. Impact: Contract signatures cannot be verified, blocking contract validation. Fix: Change method visibility from private to public. Estimated fix time: 5 minutes." \
    "Medium"

echo ""

# BUG-005: gRPC TransactionStatus Enum Mismatch
echo "Creating BUG-005: gRPC TransactionStatus Enum Mismatch..."
create_issue \
    "[BUG-005] gRPC TransactionStatus Enum Missing Values" \
    "Code references TransactionStatus.PENDING, CONFIRMED, REJECTED which don't exist in the enum. Locations: TransactionServiceImpl.java:112, 155, 254. Impact: gRPC transaction endpoints non-functional. Fix: Add missing enum values to TransactionStatus. Estimated fix time: 30 minutes." \
    "Medium"

echo ""

# BUG-006: Duplicate validateTransaction Method
echo "Creating BUG-006: Duplicate validateTransaction Method..."
create_issue \
    "[BUG-006] Duplicate validateTransaction Method Definition" \
    "Method validateTransaction(TransactionRequest) is defined twice in TransactionServiceImpl. Location: TransactionServiceImpl.java:235. Fix: Remove duplicate method definition. Estimated fix time: 5 minutes." \
    "Low"

echo ""

# BUG-007: Missing Fields in RicardianContract
echo "Creating BUG-007: Missing Fields in RicardianContract..."
create_issue \
    "[BUG-007] Missing Methods in RicardianContract Class" \
    "RicardianContractConversionService references methods that don't exist in RicardianContract: getLegalText(), getExecutableCode(), getContractHash(), getContractAddress(). Locations: RicardianContractConversionService.java:63,75,76,79,82,85. Impact: Cannot convert between Ricardian and Active contracts. Estimated fix time: 1 hour." \
    "Medium"

echo ""

# BUG-008: Missing Token AssetType Definition
echo "Creating BUG-008: Missing Token AssetType Definition..."
create_issue \
    "[BUG-008] Missing AssetType Enum Definition" \
    "Multiple classes reference AssetType which should be in contracts.models but is missing or not accessible. Affected: Token.java, TokenManagementService.java, TokenRepository.java. Impact: Cannot create or manage tokens with asset type classification. Fix: Locate or create AssetType enum in contracts.models package. Estimated fix time: 30 minutes." \
    "High"

echo ""

# BUG-009: Missing KYC Compliance Entities
echo "Creating BUG-009: Missing KYC Compliance Entities..."
create_issue \
    "[BUG-009] Missing KYC Compliance Entity Classes" \
    "Repository classes reference entities in contracts.rwa.compliance.entities package which don't exist. Missing: KYCVerificationRecord.java, AMLScreeningRecord.java. Affected: KYCVerificationRepository.java, AMLScreeningRepository.java. Impact: Cannot perform KYC verification or AML screening for users. Estimated fix time: 2 hours." \
    "Medium"

echo ""

# BUG-010: Bridge ValidatorNetworkStats Lombok Issues
echo "Creating BUG-010: Bridge ValidatorNetworkStats Lombok Issues..."
create_issue \
    "[BUG-010] ValidatorNetworkStats Lombok Annotation Failures" \
    "ValidatorNetworkStats class has 10+ Lombok annotation failures preventing getter/setter generation. Location: io.aurigraph.v11.bridge.models.ValidatorNetworkStats. Impact: Cannot monitor cross-chain bridge validator network health. Fix: Apply manual getter/setter generation (same as BUG-001 partial fix). Estimated fix time: 1 hour." \
    "Medium"

echo ""
echo "=== JIRA Issue Creation Complete ==="
echo ""
echo "Summary:"
echo "- 10 Bug tickets created in project ${PROJECT_KEY}"
echo "- Priority breakdown:"
echo "  * Highest: 1 issue (BUG-001)"
echo "  * High: 2 issues (BUG-002, BUG-008)"
echo "  * Medium: 6 issues (BUG-003, BUG-004, BUG-005, BUG-007, BUG-009, BUG-010)"
echo "  * Low: 1 issue (BUG-006)"
echo ""
echo "Total estimated fix time: 10-12 hours"
echo ""
echo "View issues at: ${JIRA_URL}/jira/software/projects/${PROJECT_KEY}"
