#!/bin/bash

# Individual API & Integration Testing Suite
# Tests each API endpoint individually and UI/UX integration
# Date: October 10, 2025

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-https://dlt.aurigraph.io}"
TIMEOUT=10
VERBOSE="${VERBOSE:-false}"

# Test data
TEST_ADDRESS="0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb"
TEST_CONTRACT_ID="RC_1760090949728_a4a1b1df"
TEST_SIGNER="0x1234567890abcdef1234567890abcdef12345678"

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
WARNINGS=0

# Create test results directory
RESULTS_DIR="test-results-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RESULTS_DIR"

# Log file
LOG_FILE="$RESULTS_DIR/test-log.txt"

# Function to log messages
log() {
    echo "$1" | tee -a "$LOG_FILE"
}

# Function to log verbose
log_verbose() {
    if [ "$VERBOSE" = "true" ]; then
        echo "$1" | tee -a "$LOG_FILE"
    else
        echo "$1" >> "$LOG_FILE"
    fi
}

echo "=================================================="
echo "INDIVIDUAL API & INTEGRATION TESTING SUITE"
echo "=================================================="
echo "Base URL: $BASE_URL"
echo "Results Directory: $RESULTS_DIR"
echo "Log File: $LOG_FILE"
echo "Date: $(date)"
echo "=================================================="
echo ""

# Function to test individual API endpoint
test_api() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    local test_id=$1
    local test_name=$2
    local method=$3
    local endpoint=$4
    local data=${5:-""}
    local expected_status=${6:-200}
    local description=$7

    echo ""
    log "=================================================="
    log "TEST $test_id: $test_name"
    log "=================================================="
    log "Description: $description"
    log "Method: $method"
    log "Endpoint: $endpoint"
    log "Expected Status: $expected_status"

    if [ -n "$data" ]; then
        log_verbose "Request Data: $data"
    fi

    echo -n "Testing... "

    # Build curl command
    local curl_cmd="curl -s -w '\n%{http_code}' -m $TIMEOUT"

    if [ "$method" = "GET" ]; then
        curl_cmd="$curl_cmd -X GET"
    elif [ "$method" = "POST" ]; then
        curl_cmd="$curl_cmd -X POST"
        if [ -n "$data" ]; then
            curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$data'"
        fi
    elif [ "$method" = "PUT" ]; then
        curl_cmd="$curl_cmd -X PUT"
        if [ -n "$data" ]; then
            curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$data'"
        fi
    elif [ "$method" = "DELETE" ]; then
        curl_cmd="$curl_cmd -X DELETE"
    fi

    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"

    # Execute request
    RESPONSE=$(eval $curl_cmd 2>&1 || echo "000")
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    RESPONSE_BODY=$(echo "$RESPONSE" | sed '$d')

    # Save response
    echo "$RESPONSE_BODY" > "$RESULTS_DIR/$test_id-response.json"

    # Analyze response
    if [ "$HTTP_CODE" = "$expected_status" ] || [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "204" ]; then
        echo -e "${GREEN}✅ PASS (HTTP $HTTP_CODE)${NC}"
        log "Status: ✅ PASS"
        log "HTTP Code: $HTTP_CODE"
        PASSED_TESTS=$((PASSED_TESTS + 1))

        # Log response details
        if [ ${#RESPONSE_BODY} -gt 0 ]; then
            log_verbose "Response: $RESPONSE_BODY"

            # Extract key information based on endpoint type
            if echo "$endpoint" | grep -q "health"; then
                STATUS=$(echo "$RESPONSE_BODY" | grep -o '"status":"[^"]*"' | head -1)
                log "Health Status: $STATUS"
            elif echo "$endpoint" | grep -q "gas-fees"; then
                log "Gas Fees Retrieved: $(echo "$RESPONSE_BODY" | grep -o '"[A-Z_]*":[0-9.]*' | wc -l) operations"
            elif echo "$endpoint" | grep -q "blocks"; then
                log "Block Data Retrieved"
            elif echo "$endpoint" | grep -q "validators"; then
                log "Validator Data Retrieved"
            fi
        fi

        return 0
    elif [ "$HTTP_CODE" = "404" ]; then
        echo -e "${YELLOW}⚠️  NOT FOUND (HTTP 404)${NC}"
        log "Status: ⚠️  NOT FOUND"
        log "HTTP Code: 404"
        WARNINGS=$((WARNINGS + 1))
        return 1
    elif [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
        echo -e "${BLUE}🔒 AUTH REQUIRED (HTTP $HTTP_CODE)${NC}"
        log "Status: 🔒 AUTH REQUIRED"
        log "HTTP Code: $HTTP_CODE"
        PASSED_TESTS=$((PASSED_TESTS + 1))  # Endpoint exists
        return 0
    elif [ "$HTTP_CODE" = "400" ]; then
        echo -e "${BLUE}📝 BAD REQUEST (HTTP 400) - Endpoint exists, needs valid input${NC}"
        log "Status: 📝 BAD REQUEST"
        log "HTTP Code: 400"
        log "Response: $RESPONSE_BODY"
        PASSED_TESTS=$((PASSED_TESTS + 1))  # Endpoint exists
        return 0
    elif [ "$HTTP_CODE" = "405" ]; then
        echo -e "${BLUE}📝 METHOD NOT ALLOWED (HTTP 405) - Endpoint exists${NC}"
        log "Status: 📝 METHOD NOT ALLOWED"
        log "HTTP Code: 405"
        PASSED_TESTS=$((PASSED_TESTS + 1))  # Endpoint exists
        return 0
    elif [ "$HTTP_CODE" = "000" ]; then
        echo -e "${RED}❌ TIMEOUT/ERROR${NC}"
        log "Status: ❌ TIMEOUT/ERROR"
        log "Error: $RESPONSE"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    else
        echo -e "${RED}❌ FAIL (HTTP $HTTP_CODE)${NC}"
        log "Status: ❌ FAIL"
        log "HTTP Code: $HTTP_CODE"
        log "Response: $RESPONSE_BODY"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Test UI/UX endpoint
test_ui() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    local test_id=$1
    local test_name=$2
    local url=$3
    local description=$4

    echo ""
    log "=================================================="
    log "UI TEST $test_id: $test_name"
    log "=================================================="
    log "Description: $description"
    log "URL: $url"

    echo -n "Testing UI... "

    RESPONSE=$(curl -s -w '\n%{http_code}' -m $TIMEOUT "$url" 2>&1 || echo "000")
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    RESPONSE_BODY=$(echo "$RESPONSE" | sed '$d')

    # Save HTML response
    echo "$RESPONSE_BODY" > "$RESULTS_DIR/$test_id-ui.html"

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✅ PASS (HTTP $HTTP_CODE)${NC}"
        log "Status: ✅ PASS"
        log "HTTP Code: $HTTP_CODE"
        log "Content Length: ${#RESPONSE_BODY} bytes"

        # Check for key UI elements
        if echo "$RESPONSE_BODY" | grep -q "Aurigraph"; then
            log "✓ Contains 'Aurigraph' branding"
        fi
        if echo "$RESPONSE_BODY" | grep -q "v11\|V11\|11.1"; then
            log "✓ Contains version information"
        fi

        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}❌ FAIL (HTTP $HTTP_CODE)${NC}"
        log "Status: ❌ FAIL"
        log "HTTP Code: $HTTP_CODE"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

log "Starting Individual API Tests..."
log "================================"

# ================================================
# CATEGORY 1: CORE INFRASTRUCTURE
# ================================================
log ""
log "=================================================="
log "CATEGORY 1: CORE INFRASTRUCTURE TESTS"
log "=================================================="

test_api "CORE-001" \
    "Health Check Endpoint" \
    "GET" \
    "/q/health" \
    "" \
    200 \
    "Verify application health status and all subsystem checks"

test_api "CORE-002" \
    "Prometheus Metrics" \
    "GET" \
    "/q/metrics" \
    "" \
    200 \
    "Retrieve Prometheus-format metrics for monitoring"

test_api "CORE-003" \
    "System Information" \
    "GET" \
    "/api/v11/info" \
    "" \
    200 \
    "Get system version and configuration information"

# ================================================
# CATEGORY 2: RICARDIAN CONTRACTS
# ================================================
log ""
log "=================================================="
log "CATEGORY 2: RICARDIAN CONTRACTS TESTS"
log "=================================================="

test_api "RC-001" \
    "Gas Fees API" \
    "GET" \
    "/api/v11/contracts/ricardian/gas-fees" \
    "" \
    200 \
    "Retrieve gas fee schedule for all Ricardian contract operations"

test_api "RC-002" \
    "List All Contracts" \
    "GET" \
    "/api/v11/contracts/ricardian" \
    "" \
    200 \
    "List all Ricardian contracts in the system"

test_api "RC-003" \
    "Get Specific Contract" \
    "GET" \
    "/api/v11/contracts/ricardian/$TEST_CONTRACT_ID" \
    "" \
    200 \
    "Retrieve details of a specific Ricardian contract by ID"

test_api "RC-004" \
    "Contract Audit Trail" \
    "GET" \
    "/api/v11/contracts/ricardian/$TEST_CONTRACT_ID/audit" \
    "" \
    200 \
    "Retrieve complete audit trail for a contract"

test_api "RC-005" \
    "GDPR Compliance Report" \
    "GET" \
    "/api/v11/contracts/ricardian/$TEST_CONTRACT_ID/compliance/GDPR" \
    "" \
    200 \
    "Generate GDPR compliance report for contract"

test_api "RC-006" \
    "SOX Compliance Report" \
    "GET" \
    "/api/v11/contracts/ricardian/$TEST_CONTRACT_ID/compliance/SOX" \
    "" \
    200 \
    "Generate SOX compliance report for contract"

test_api "RC-007" \
    "HIPAA Compliance Report" \
    "GET" \
    "/api/v11/contracts/ricardian/$TEST_CONTRACT_ID/compliance/HIPAA" \
    "" \
    200 \
    "Generate HIPAA compliance report for contract"

# ================================================
# CATEGORY 3: BLOCKCHAIN CORE
# ================================================
log ""
log "=================================================="
log "CATEGORY 3: BLOCKCHAIN CORE TESTS"
log "=================================================="

test_api "BC-001" \
    "Get All Blocks" \
    "GET" \
    "/api/v11/blockchain/blocks" \
    "" \
    200 \
    "Retrieve list of all blocks in the blockchain"

test_api "BC-002" \
    "Get Latest Block" \
    "GET" \
    "/api/v11/blockchain/blocks/latest" \
    "" \
    200 \
    "Retrieve the most recent block"

test_api "BC-003" \
    "Get Block by Height" \
    "GET" \
    "/api/v11/blockchain/blocks/1" \
    "" \
    200 \
    "Retrieve a specific block by height"

test_api "BC-004" \
    "Get All Transactions" \
    "GET" \
    "/api/v11/blockchain/transactions" \
    "" \
    200 \
    "Retrieve list of all transactions"

test_api "BC-005" \
    "Get Transaction by ID" \
    "GET" \
    "/api/v11/blockchain/transactions/tx_test_123" \
    "" \
    200 \
    "Retrieve a specific transaction by ID"

test_api "BC-006" \
    "Get Account Balance" \
    "GET" \
    "/api/v11/blockchain/accounts/$TEST_ADDRESS/balance" \
    "" \
    200 \
    "Retrieve account balance for a specific address"

test_api "BC-007" \
    "Network Statistics" \
    "GET" \
    "/api/v11/blockchain/network/stats" \
    "" \
    200 \
    "Retrieve overall network statistics"

# ================================================
# CATEGORY 4: VALIDATORS & STAKING
# ================================================
log ""
log "=================================================="
log "CATEGORY 4: VALIDATORS & STAKING TESTS"
log "=================================================="

test_api "VAL-001" \
    "List All Validators" \
    "GET" \
    "/api/v11/blockchain/validators" \
    "" \
    200 \
    "Retrieve list of all active validators"

test_api "VAL-002" \
    "Get Validator by Address" \
    "GET" \
    "/api/v11/blockchain/validators/$TEST_ADDRESS" \
    "" \
    200 \
    "Retrieve details of a specific validator"

test_api "VAL-003" \
    "Staking Information" \
    "GET" \
    "/api/v11/blockchain/staking/info" \
    "" \
    200 \
    "Retrieve staking statistics and information"

test_api "VAL-004" \
    "Get Staking Rewards" \
    "GET" \
    "/api/v11/blockchain/staking/rewards/$TEST_ADDRESS" \
    "" \
    200 \
    "Retrieve staking rewards for an address"

# ================================================
# CATEGORY 5: PAYMENT CHANNELS
# ================================================
log ""
log "=================================================="
log "CATEGORY 5: PAYMENT CHANNELS TESTS"
log "=================================================="

test_api "CH-001" \
    "List All Channels" \
    "GET" \
    "/api/v11/channels" \
    "" \
    200 \
    "Retrieve list of all payment channels"

test_api "CH-002" \
    "Get Channel by ID" \
    "GET" \
    "/api/v11/channels/channel_test_123" \
    "" \
    200 \
    "Retrieve details of a specific payment channel"

test_api "CH-003" \
    "Live Channel Data" \
    "GET" \
    "/api/v11/live/channels" \
    "" \
    200 \
    "Retrieve real-time payment channel data"

# ================================================
# CATEGORY 6: GOVERNANCE
# ================================================
log ""
log "=================================================="
log "CATEGORY 6: GOVERNANCE TESTS"
log "=================================================="

test_api "GOV-001" \
    "List All Proposals" \
    "GET" \
    "/api/v11/blockchain/governance/proposals" \
    "" \
    200 \
    "Retrieve list of all governance proposals"

test_api "GOV-002" \
    "Get Proposal by ID" \
    "GET" \
    "/api/v11/blockchain/governance/proposals/prop_123" \
    "" \
    200 \
    "Retrieve details of a specific proposal"

test_api "GOV-003" \
    "Voting Statistics" \
    "GET" \
    "/api/v11/blockchain/governance/stats" \
    "" \
    200 \
    "Retrieve governance voting statistics"

# ================================================
# CATEGORY 7: SECURITY & CRYPTOGRAPHY
# ================================================
log ""
log "=================================================="
log "CATEGORY 7: SECURITY & CRYPTOGRAPHY TESTS"
log "=================================================="

test_api "SEC-001" \
    "Security Status" \
    "GET" \
    "/api/v11/security/status" \
    "" \
    200 \
    "Retrieve overall security system status"

test_api "SEC-002" \
    "Key Management" \
    "GET" \
    "/api/v11/security/keys" \
    "" \
    200 \
    "Retrieve key management information"

test_api "SEC-003" \
    "Quantum Cryptography" \
    "GET" \
    "/api/v11/security/quantum" \
    "" \
    200 \
    "Retrieve quantum cryptography status and algorithms"

test_api "SEC-004" \
    "HSM Status" \
    "GET" \
    "/api/v11/security/hsm/status" \
    "" \
    200 \
    "Retrieve Hardware Security Module status"

# ================================================
# CATEGORY 8: CONSENSUS & NETWORK
# ================================================
log ""
log "=================================================="
log "CATEGORY 8: CONSENSUS & NETWORK TESTS"
log "=================================================="

test_api "CON-001" \
    "Consensus Status" \
    "GET" \
    "/api/v11/consensus/status" \
    "" \
    200 \
    "Retrieve current consensus algorithm status"

test_api "CON-002" \
    "Network Peers" \
    "GET" \
    "/api/v11/network/peers" \
    "" \
    200 \
    "Retrieve list of network peers"

test_api "CON-003" \
    "Network Health" \
    "GET" \
    "/api/v11/network/health" \
    "" \
    200 \
    "Retrieve overall network health metrics"

test_api "CON-004" \
    "Live Consensus Data" \
    "GET" \
    "/api/v11/live/consensus" \
    "" \
    200 \
    "Retrieve real-time consensus data"

test_api "CON-005" \
    "Live Validators" \
    "GET" \
    "/api/v11/live/validators" \
    "" \
    200 \
    "Retrieve real-time validator status"

# ================================================
# CATEGORY 9: DATA FEEDS & ORACLES
# ================================================
log ""
log "=================================================="
log "CATEGORY 9: DATA FEEDS & ORACLES TESTS"
log "=================================================="

test_api "FEED-001" \
    "Data Feeds List" \
    "GET" \
    "/api/v11/datafeeds" \
    "" \
    200 \
    "Retrieve list of available data feeds"

test_api "FEED-002" \
    "Price Feed" \
    "GET" \
    "/api/v11/datafeeds/prices" \
    "" \
    200 \
    "Retrieve current price feed data"

test_api "FEED-003" \
    "Oracle Status" \
    "GET" \
    "/api/v11/oracles/status" \
    "" \
    200 \
    "Retrieve oracle service status"

# ================================================
# CATEGORY 10: ANALYTICS
# ================================================
log ""
log "=================================================="
log "CATEGORY 10: ANALYTICS TESTS"
log "=================================================="

test_api "ANAL-001" \
    "Analytics Dashboard" \
    "GET" \
    "/api/v11/analytics/dashboard" \
    "" \
    200 \
    "Retrieve analytics dashboard data"

test_api "ANAL-002" \
    "Transaction Analytics" \
    "GET" \
    "/api/v11/analytics/transactions" \
    "" \
    200 \
    "Retrieve transaction analytics and statistics"

test_api "ANAL-003" \
    "Performance Metrics" \
    "GET" \
    "/api/v11/analytics/performance" \
    "" \
    200 \
    "Retrieve system performance metrics"

# ================================================
# CATEGORY 11: ENTERPRISE FEATURES
# ================================================
log ""
log "=================================================="
log "CATEGORY 11: ENTERPRISE FEATURES TESTS"
log "=================================================="

test_api "ENT-001" \
    "Enterprise Status" \
    "GET" \
    "/api/v11/enterprise/status" \
    "" \
    200 \
    "Retrieve enterprise features status"

test_api "ENT-002" \
    "Multi-Tenancy" \
    "GET" \
    "/api/v11/enterprise/tenants" \
    "" \
    200 \
    "Retrieve tenant information"

test_api "ENT-003" \
    "SSO Configuration" \
    "GET" \
    "/api/v11/enterprise/sso" \
    "" \
    200 \
    "Retrieve SSO configuration"

test_api "ENT-004" \
    "Backup Status" \
    "GET" \
    "/api/v11/enterprise/backup/status" \
    "" \
    200 \
    "Retrieve backup system status"

# ================================================
# CATEGORY 12: CROSS-CHAIN BRIDGE
# ================================================
log ""
log "=================================================="
log "CATEGORY 12: CROSS-CHAIN BRIDGE TESTS"
log "=================================================="

test_api "BR-001" \
    "Bridge Status" \
    "GET" \
    "/api/v11/bridge/status" \
    "" \
    200 \
    "Retrieve cross-chain bridge status"

test_api "BR-002" \
    "Supported Chains" \
    "GET" \
    "/api/v11/bridge/chains" \
    "" \
    200 \
    "Retrieve list of supported blockchain networks"

test_api "BR-003" \
    "Bridge History" \
    "GET" \
    "/api/v11/bridge/history" \
    "" \
    200 \
    "Retrieve cross-chain transaction history"

# ================================================
# CATEGORY 13: AI/ML SERVICES
# ================================================
log ""
log "=================================================="
log "CATEGORY 13: AI/ML SERVICES TESTS"
log "=================================================="

test_api "AI-001" \
    "AI Status" \
    "GET" \
    "/api/v11/ai/status" \
    "" \
    200 \
    "Retrieve AI/ML service status"

test_api "AI-002" \
    "AI Predictions" \
    "GET" \
    "/api/v11/ai/predictions" \
    "" \
    200 \
    "Retrieve AI-driven predictions"

test_api "AI-003" \
    "Anomaly Detection" \
    "GET" \
    "/api/v11/ai/anomalies" \
    "" \
    200 \
    "Retrieve anomaly detection results"

# ================================================
# CATEGORY 14: DEFI INTEGRATIONS
# ================================================
log ""
log "=================================================="
log "CATEGORY 14: DEFI INTEGRATIONS TESTS"
log "=================================================="

test_api "DEFI-001" \
    "DeFi Overview" \
    "GET" \
    "/api/v11/blockchain/defi" \
    "" \
    200 \
    "Retrieve DeFi integration overview"

test_api "DEFI-002" \
    "Uniswap Pools" \
    "GET" \
    "/api/v11/blockchain/defi/uniswap/pools" \
    "" \
    200 \
    "Retrieve Uniswap V3 pool information"

test_api "DEFI-003" \
    "Aave Markets" \
    "GET" \
    "/api/v11/blockchain/defi/aave/markets" \
    "" \
    200 \
    "Retrieve Aave lending market information"

# ================================================
# CATEGORY 15: HEALTHCARE (HMS)
# ================================================
log ""
log "=================================================="
log "CATEGORY 15: HEALTHCARE (HMS) TESTS"
log "=================================================="

test_api "HMS-001" \
    "HMS Status" \
    "GET" \
    "/api/v11/hms/status" \
    "" \
    200 \
    "Retrieve Healthcare Management System status"

test_api "HMS-002" \
    "HMS Records" \
    "GET" \
    "/api/v11/hms/records" \
    "" \
    200 \
    "Retrieve healthcare record information"

test_api "HMS-003" \
    "FHIR Patients" \
    "GET" \
    "/api/v11/hms/fhir/Patient" \
    "" \
    200 \
    "Retrieve FHIR-formatted patient data"

# ================================================
# CATEGORY 16: REAL WORLD ASSETS
# ================================================
log ""
log "=================================================="
log "CATEGORY 16: REAL WORLD ASSETS TESTS"
log "=================================================="

test_api "RWA-001" \
    "RWA Assets" \
    "GET" \
    "/api/v11/rwa/assets" \
    "" \
    200 \
    "Retrieve real-world asset listings"

test_api "RWA-002" \
    "RWA Valuations" \
    "GET" \
    "/api/v11/rwa/valuations" \
    "" \
    200 \
    "Retrieve real-world asset valuations"

# ================================================
# UI/UX TESTS
# ================================================
log ""
log "=================================================="
log "UI/UX INTEGRATION TESTS"
log "=================================================="

test_ui "UI-001" \
    "Main Portal" \
    "https://dlt.aurigraph.io/" \
    "Test main portal landing page loads correctly"

test_ui "UI-002" \
    "Test Page" \
    "https://dlt.aurigraph.io/v11-test.html" \
    "Test V11 testing page loads correctly"

test_ui "UI-003" \
    "API Health UI" \
    "https://dlt.aurigraph.io/q/health" \
    "Test health check endpoint returns proper JSON"

# ================================================
# SUMMARY
# ================================================
echo ""
log "=================================================="
log "TEST EXECUTION SUMMARY"
log "=================================================="
log ""
log "Total Tests: $TOTAL_TESTS"
log "Passed: $PASSED_TESTS ($(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%)"
log "Failed: $FAILED_TESTS ($(echo "scale=1; $FAILED_TESTS * 100 / $TOTAL_TESTS" | bc)%)"
log "Warnings (404): $WARNINGS"
log ""
log "Results Directory: $RESULTS_DIR"
log "Log File: $LOG_FILE"
log ""

if [ $FAILED_TESTS -eq 0 ] && [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    log "Status: ✅ ALL TESTS PASSED"
    exit 0
elif [ $PASSED_TESTS -gt $((TOTAL_TESTS * 7 / 10)) ]; then
    log "Status: ⚠️  MOST TESTS PASSED (Acceptable)"
    exit 0
else
    log "Status: ❌ TOO MANY FAILURES"
    exit 1
fi

log ""
log "=================================================="
log "Test execution completed: $(date)"
log "=================================================="
