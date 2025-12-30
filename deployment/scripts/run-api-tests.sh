#!/bin/bash

# API Tests for Aurigraph V11 Ricardian Contracts
# Comprehensive API endpoint testing
# Duration: ~15 minutes

set -e

echo "🔧 Running API Tests for Aurigraph V11..."
echo "================================================"
echo ""

# Configuration
BASE_URL="${BASE_URL:-http://localhost:9003}"
TIMEOUT=30

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Test data
TEST_CONTRACT_ID=""
TEST_PDF_FILE="/tmp/test_contract.txt"

# Helper function
run_test() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    TEST_NAME=$1
    TEST_CMD=$2

    echo -n "  [API-$TOTAL_TESTS] $TEST_NAME... "

    if eval "$TEST_CMD" > /dev/null 2>&1; then
        echo -e "${GREEN}PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}FAIL${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "    Command: $TEST_CMD"
        return 1
    fi
}

# Setup test data
setup_test_data() {
    echo "📦 Setting up test data..."

    # Create test contract document
    cat > "$TEST_PDF_FILE" <<EOF
REAL ESTATE PURCHASE AGREEMENT

This Agreement is made on October 10, 2025

BETWEEN:
Buyer: Alice Johnson, address 0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb
Seller: Bob Smith, address 0x8a91DC2D28B689474298D91899f0c1baF62cB85E

WHEREAS the Seller is the legal owner of the property located at:
123 Blockchain Street, DeFi City, Web3 State

The Parties agree as follows:

1. PURCHASE PRICE
The purchase price for the property is \$500,000 USD.

2. PAYMENT TERMS
- Deposit: \$50,000 due upon signing
- Balance: \$450,000 due at closing within 30 days

3. CLOSING DATE
Closing shall occur on or before November 10, 2025.
EOF

    echo -e "${GREEN}✅ Test data ready${NC}\n"
}

# Cleanup test data
cleanup_test_data() {
    rm -f "$TEST_PDF_FILE"
}

# Wait for service
echo "⏳ Waiting for service at $BASE_URL..."
RETRIES=30
until curl -s "$BASE_URL/q/health" > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    sleep 2
    RETRIES=$((RETRIES - 1))
done

if [ $RETRIES -eq 0 ]; then
    echo -e "\n${RED}❌ Service not available${NC}"
    exit 1
fi

echo -e "\n${GREEN}✅ Service ready${NC}\n"

# Setup
setup_test_data

# ====================
# API TESTS
# ====================

echo "🧪 Running Ricardian Contract API Tests..."
echo ""

# TC-RC-010: Get Gas Fees
echo -e "${BLUE}=== Gas Fee Tests ===${NC}"
run_test "Get Gas Fee Rates" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/contracts/ricardian/gas-fees | jq -e '.DOCUMENT_UPLOAD' > /dev/null"

# TC-RC-001: Document Upload
echo -e "\n${BLUE}=== Document Upload Tests ===${NC}"

# Test valid document upload
TEST_UPLOAD_RESPONSE=$(mktemp)
run_test "Upload Valid Document" \
    "curl -s -f -m $TIMEOUT -X POST $BASE_URL/api/v11/contracts/ricardian/upload \
    -F 'file=@$TEST_PDF_FILE' \
    -F 'fileName=test_contract.txt' \
    -F 'contractType=REAL_ESTATE' \
    -F 'jurisdiction=California' \
    -F 'submitterAddress=0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb' \
    -o $TEST_UPLOAD_RESPONSE"

# Extract contract ID if upload succeeded
if [ $? -eq 0 ]; then
    TEST_CONTRACT_ID=$(jq -r '.contractId // empty' "$TEST_UPLOAD_RESPONSE" 2>/dev/null)
    if [ -n "$TEST_CONTRACT_ID" ]; then
        echo -e "    ${GREEN}Contract ID: $TEST_CONTRACT_ID${NC}"
    fi
fi

rm -f "$TEST_UPLOAD_RESPONSE"

# TC-RC-002: Invalid file type
run_test "Reject Invalid File Type" \
    "! curl -s -f -m $TIMEOUT -X POST $BASE_URL/api/v11/contracts/ricardian/upload \
    -F 'file=@/bin/ls' \
    -F 'fileName=malicious.exe' \
    -F 'contractType=REAL_ESTATE' \
    -F 'jurisdiction=California' \
    -F 'submitterAddress=0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb'"

# Only run these tests if we have a contract ID
if [ -n "$TEST_CONTRACT_ID" ]; then
    echo -e "\n${BLUE}=== Contract Query Tests ===${NC}"

    # TC-RC-GET: Get Contract by ID
    run_test "Get Contract by ID" \
        "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/contracts/ricardian/$TEST_CONTRACT_ID | jq -e '.contractId' > /dev/null"

    echo -e "\n${BLUE}=== Party Management Tests ===${NC}"

    # TC-RC-004: Add Party
    run_test "Add Party to Contract" \
        "curl -s -f -m $TIMEOUT -X POST $BASE_URL/api/v11/contracts/ricardian/$TEST_CONTRACT_ID/parties \
        -H 'Content-Type: application/json' \
        -d '{
            \"name\": \"Charlie Witness\",
            \"address\": \"0x1234567890abcdef1234567890abcdef12345678\",
            \"role\": \"WITNESS\",
            \"signatureRequired\": false,
            \"kycVerified\": true,
            \"submitterAddress\": \"0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb\"
        }' | jq -e '.success' > /dev/null"

    echo -e "\n${BLUE}=== Signature Tests ===${NC}"

    # TC-RC-005: Submit Signature
    run_test "Submit Signature" \
        "curl -s -f -m $TIMEOUT -X POST $BASE_URL/api/v11/contracts/ricardian/$TEST_CONTRACT_ID/sign \
        -H 'Content-Type: application/json' \
        -d '{
            \"signerAddress\": \"0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb\",
            \"signature\": \"0xabcdef1234567890\",
            \"publicKey\": \"0x9876543210fedcba\"
        }' | jq -e '.success' > /dev/null"

    echo -e "\n${BLUE}=== Audit Trail Tests ===${NC}"

    # TC-RC-008: Get Audit Trail
    run_test "Get Contract Audit Trail" \
        "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/contracts/ricardian/$TEST_CONTRACT_ID/audit | jq -e '.auditTrail' > /dev/null"

    # TC-RC-009: Compliance Report - GDPR
    run_test "Generate GDPR Compliance Report" \
        "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/contracts/ricardian/$TEST_CONTRACT_ID/compliance/GDPR | jq -e '.regulatoryFramework' > /dev/null"

    # TC-RC-009: Compliance Report - SOX
    run_test "Generate SOX Compliance Report" \
        "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/contracts/ricardian/$TEST_CONTRACT_ID/compliance/SOX | jq -e '.regulatoryFramework' > /dev/null"

    # TC-RC-009: Compliance Report - HIPAA
    run_test "Generate HIPAA Compliance Report" \
        "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/contracts/ricardian/$TEST_CONTRACT_ID/compliance/HIPAA | jq -e '.regulatoryFramework' > /dev/null"
else
    echo -e "${YELLOW}⚠️  Skipping contract-dependent tests (no contract ID)${NC}"
fi

echo -e "\n${BLUE}=== Live Data Tests ===${NC}"

# Live Validators
run_test "Get Live Validators" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/live/validators | jq -e '.validators | length > 0' > /dev/null"

# Live Consensus
run_test "Get Live Consensus Data" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/live/consensus | jq -e '.nodeId' > /dev/null"

# Live Channels
run_test "Get All Live Channels" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/live/channels | jq -e '.channels | length > 0' > /dev/null"

# Channel with participants
run_test "Get Channel with Participants" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/live/channels/CH_MAIN_001 | jq -e '.channel' > /dev/null"

# Cleanup
cleanup_test_data

echo ""
echo "================================================"
echo "📊 API Test Results"
echo "================================================"
echo -e "Total Tests:  $TOTAL_TESTS"
echo -e "Passed:       ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:       ${RED}$FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ All API tests passed!${NC}"
    echo ""
    exit 0
else
    PASS_RATE=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)
    echo -e "${YELLOW}⚠️  $FAILED_TESTS test(s) failed (Pass rate: ${PASS_RATE}%)${NC}"
    echo ""
    exit 1
fi
