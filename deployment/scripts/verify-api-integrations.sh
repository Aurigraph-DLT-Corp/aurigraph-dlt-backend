#!/bin/bash

# API Integration Verification Script
# Tests all documented API endpoints from API-INTEGRATIONS-GUIDE.md
# Date: October 10, 2025

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-https://dlt.aurigraph.io}"
TIMEOUT=10

# Counters
TOTAL_ENDPOINTS=0
AVAILABLE_ENDPOINTS=0
NOT_FOUND_ENDPOINTS=0
ERROR_ENDPOINTS=0

echo "=================================================="
echo "API Integration Verification"
echo "=================================================="
echo "Base URL: $BASE_URL"
echo "Timeout: ${TIMEOUT}s"
echo "Date: $(date)"
echo "=================================================="
echo ""

# Function to test endpoint
test_endpoint() {
    TOTAL_ENDPOINTS=$((TOTAL_ENDPOINTS + 1))
    local CATEGORY=$1
    local ENDPOINT=$2
    local METHOD=${3:-GET}
    local EXPECTED_STATUS=${4:-200}

    echo -n "[$TOTAL_ENDPOINTS] $CATEGORY - $METHOD $ENDPOINT ... "

    if [ "$METHOD" = "GET" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}" -m $TIMEOUT "$BASE_URL$ENDPOINT" 2>&1 || echo "000")
        HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    else
        # For non-GET methods, just check if endpoint exists
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X OPTIONS -m $TIMEOUT "$BASE_URL$ENDPOINT" 2>&1 || echo "000")
    fi

    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "204" ]; then
        echo -e "${GREEN}✅ AVAILABLE (HTTP $HTTP_CODE)${NC}"
        AVAILABLE_ENDPOINTS=$((AVAILABLE_ENDPOINTS + 1))
        return 0
    elif [ "$HTTP_CODE" = "404" ]; then
        echo -e "${YELLOW}⚠️  NOT FOUND (HTTP 404)${NC}"
        NOT_FOUND_ENDPOINTS=$((NOT_FOUND_ENDPOINTS + 1))
        return 1
    elif [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
        echo -e "${BLUE}🔒 AUTH REQUIRED (HTTP $HTTP_CODE)${NC}"
        AVAILABLE_ENDPOINTS=$((AVAILABLE_ENDPOINTS + 1))  # Endpoint exists, just needs auth
        return 0
    elif [ "$HTTP_CODE" = "405" ]; then
        echo -e "${BLUE}📝 METHOD NOT ALLOWED (HTTP 405) - Endpoint exists${NC}"
        AVAILABLE_ENDPOINTS=$((AVAILABLE_ENDPOINTS + 1))  # Endpoint exists
        return 0
    elif [ "$HTTP_CODE" = "400" ]; then
        echo -e "${BLUE}📝 BAD REQUEST (HTTP 400) - Endpoint exists, needs valid input${NC}"
        AVAILABLE_ENDPOINTS=$((AVAILABLE_ENDPOINTS + 1))  # Endpoint exists
        return 0
    elif [ "$HTTP_CODE" = "000" ]; then
        echo -e "${RED}❌ TIMEOUT/ERROR${NC}"
        ERROR_ENDPOINTS=$((ERROR_ENDPOINTS + 1))
        return 1
    else
        echo -e "${RED}❌ ERROR (HTTP $HTTP_CODE)${NC}"
        ERROR_ENDPOINTS=$((ERROR_ENDPOINTS + 1))
        return 1
    fi
}

echo "=================================================="
echo "CATEGORY 1: CORE INFRASTRUCTURE"
echo "=================================================="
test_endpoint "Health" "/q/health"
test_endpoint "Metrics" "/q/metrics"
test_endpoint "System Info" "/api/v11/info"

echo ""
echo "=================================================="
echo "CATEGORY 2: RICARDIAN CONTRACTS (PRODUCTION READY)"
echo "=================================================="
test_endpoint "Gas Fees" "/api/v11/contracts/ricardian/gas-fees"
test_endpoint "Upload Contract" "/api/v11/contracts/ricardian/upload" "POST"
test_endpoint "List Contracts" "/api/v11/contracts/ricardian"
test_endpoint "Get Contract" "/api/v11/contracts/ricardian/test-id"
test_endpoint "Add Party" "/api/v11/contracts/ricardian/test-id/parties" "POST"
test_endpoint "Submit Signature" "/api/v11/contracts/ricardian/test-id/sign" "POST"
test_endpoint "Audit Trail" "/api/v11/contracts/ricardian/test-id/audit"
test_endpoint "Compliance GDPR" "/api/v11/contracts/ricardian/test-id/compliance/GDPR"
test_endpoint "Compliance SOX" "/api/v11/contracts/ricardian/test-id/compliance/SOX"
test_endpoint "Compliance HIPAA" "/api/v11/contracts/ricardian/test-id/compliance/HIPAA"

echo ""
echo "=================================================="
echo "CATEGORY 3: BLOCKCHAIN CORE"
echo "=================================================="
test_endpoint "Blocks" "/api/v11/blockchain/blocks"
test_endpoint "Latest Block" "/api/v11/blockchain/blocks/latest"
test_endpoint "Transactions" "/api/v11/blockchain/transactions"
test_endpoint "Submit Tx" "/api/v11/blockchain/transactions" "POST"
test_endpoint "Account Balance" "/api/v11/blockchain/accounts/test-address/balance"
test_endpoint "Network Stats" "/api/v11/blockchain/network/stats"

echo ""
echo "=================================================="
echo "CATEGORY 4: LIVE DATA APIS"
echo "=================================================="
test_endpoint "Live Validators" "/api/v11/live/validators"
test_endpoint "Live Consensus" "/api/v11/live/consensus"
test_endpoint "Live Channels" "/api/v11/live/channels"
test_endpoint "Live Network" "/api/v11/live/network"

echo ""
echo "=================================================="
echo "CATEGORY 5: CROSS-CHAIN BRIDGE"
echo "=================================================="
test_endpoint "Bridge Status" "/api/v11/bridge/status"
test_endpoint "Bridge Chains" "/api/v11/bridge/chains"
test_endpoint "Bridge Transfer" "/api/v11/bridge/transfer" "POST"
test_endpoint "Bridge History" "/api/v11/bridge/history"

echo ""
echo "=================================================="
echo "CATEGORY 6: DEFI INTEGRATIONS"
echo "=================================================="
test_endpoint "DeFi Overview" "/api/v11/blockchain/defi"
test_endpoint "Uniswap Pools" "/api/v11/blockchain/defi/uniswap/pools"
test_endpoint "Aave Markets" "/api/v11/blockchain/defi/aave/markets"
test_endpoint "Lending Stats" "/api/v11/blockchain/defi/lending/stats"

echo ""
echo "=================================================="
echo "CATEGORY 7: HEALTHCARE (HMS/HL7/FHIR)"
echo "=================================================="
test_endpoint "HMS Status" "/api/v11/hms/status"
test_endpoint "HMS Records" "/api/v11/hms/records"
test_endpoint "FHIR Patients" "/api/v11/hms/fhir/Patient"
test_endpoint "HL7 Messages" "/api/v11/hms/hl7/messages"

echo ""
echo "=================================================="
echo "CATEGORY 8: AI/ML SERVICES"
echo "=================================================="
test_endpoint "AI Status" "/api/v11/ai/status"
test_endpoint "AI Optimize" "/api/v11/ai/optimize" "POST"
test_endpoint "AI Predictions" "/api/v11/ai/predictions"
test_endpoint "Anomaly Detection" "/api/v11/ai/anomalies"

echo ""
echo "=================================================="
echo "CATEGORY 9: REAL WORLD ASSETS"
echo "=================================================="
test_endpoint "RWA Assets" "/api/v11/rwa/assets"
test_endpoint "RWA Tokenize" "/api/v11/rwa/tokenize" "POST"
test_endpoint "RWA Valuations" "/api/v11/rwa/valuations"

echo ""
echo "=================================================="
echo "CATEGORY 10: GOVERNANCE"
echo "=================================================="
test_endpoint "Proposals" "/api/v11/blockchain/governance/proposals"
test_endpoint "Create Proposal" "/api/v11/blockchain/governance/proposals" "POST"
test_endpoint "Vote" "/api/v11/blockchain/governance/proposals/test-id/vote" "POST"
test_endpoint "Voting Stats" "/api/v11/blockchain/governance/stats"

echo ""
echo "=================================================="
echo "CATEGORY 11: SECURITY & CRYPTOGRAPHY"
echo "=================================================="
test_endpoint "Security Status" "/api/v11/security/status"
test_endpoint "Key Management" "/api/v11/security/keys"
test_endpoint "Quantum Crypto" "/api/v11/security/quantum"
test_endpoint "HSM Status" "/api/v11/security/hsm/status"

echo ""
echo "=================================================="
echo "CATEGORY 12: ENTERPRISE FEATURES"
echo "=================================================="
test_endpoint "Enterprise Status" "/api/v11/enterprise/status"
test_endpoint "Tenants" "/api/v11/enterprise/tenants"
test_endpoint "SSO Config" "/api/v11/enterprise/sso"
test_endpoint "Backup Status" "/api/v11/enterprise/backup/status"

echo ""
echo "=================================================="
echo "CATEGORY 13: VALIDATORS & STAKING"
echo "=================================================="
test_endpoint "Validators List" "/api/v11/blockchain/validators"
test_endpoint "Validator Info" "/api/v11/blockchain/validators/test-id"
test_endpoint "Staking Info" "/api/v11/blockchain/staking/info"
test_endpoint "Stake" "/api/v11/blockchain/staking/stake" "POST"

echo ""
echo "=================================================="
echo "CATEGORY 14: CONSENSUS & NETWORK"
echo "=================================================="
test_endpoint "Consensus Status" "/api/v11/consensus/status"
test_endpoint "Network Peers" "/api/v11/network/peers"
test_endpoint "Network Health" "/api/v11/network/health"

echo ""
echo "=================================================="
echo "CATEGORY 15: DATA FEEDS & ORACLES"
echo "=================================================="
test_endpoint "Data Feeds" "/api/v11/datafeeds"
test_endpoint "Price Feed" "/api/v11/datafeeds/prices"
test_endpoint "Oracle Status" "/api/v11/oracles/status"

echo ""
echo "=================================================="
echo "CATEGORY 16: ANALYTICS"
echo "=================================================="
test_endpoint "Analytics Dashboard" "/api/v11/analytics/dashboard"
test_endpoint "Transaction Analytics" "/api/v11/analytics/transactions"
test_endpoint "Performance Metrics" "/api/v11/analytics/performance"

echo ""
echo "=================================================="
echo "CATEGORY 17: PAYMENT CHANNELS"
echo "=================================================="
test_endpoint "Channels List" "/api/v11/channels"
test_endpoint "Create Channel" "/api/v11/channels" "POST"
test_endpoint "Channel Status" "/api/v11/channels/test-id"

echo ""
echo "=================================================="
echo "VERIFICATION SUMMARY"
echo "=================================================="
echo ""
echo "Total Endpoints Tested: $TOTAL_ENDPOINTS"
echo ""
echo -e "${GREEN}✅ Available: $AVAILABLE_ENDPOINTS${NC}"
echo -e "${YELLOW}⚠️  Not Found: $NOT_FOUND_ENDPOINTS${NC}"
echo -e "${RED}❌ Errors: $ERROR_ENDPOINTS${NC}"
echo ""

AVAILABILITY_RATE=$(echo "scale=1; $AVAILABLE_ENDPOINTS * 100 / $TOTAL_ENDPOINTS" | bc)
echo "Availability Rate: ${AVAILABILITY_RATE}%"
echo ""

if [ $AVAILABLE_ENDPOINTS -eq $TOTAL_ENDPOINTS ]; then
    echo -e "${GREEN}✅ ALL INTEGRATIONS AVAILABLE${NC}"
    exit 0
elif [ $AVAILABLE_ENDPOINTS -gt $((TOTAL_ENDPOINTS * 7 / 10)) ]; then
    echo -e "${YELLOW}⚠️  PARTIAL INTEGRATION AVAILABILITY${NC}"
    echo ""
    echo "Recommendations:"
    echo "1. Enable missing endpoints in production configuration"
    echo "2. Review authentication requirements for protected endpoints"
    echo "3. Check service deployment status for unavailable features"
    exit 0
else
    echo -e "${RED}❌ LOW INTEGRATION AVAILABILITY${NC}"
    echo ""
    echo "Critical Issues:"
    echo "1. Many documented endpoints are not available"
    echo "2. Check deployment configuration"
    echo "3. Verify all services are running"
    exit 1
fi

echo ""
echo "=================================================="
echo "Verification Complete: $(date)"
echo "=================================================="
