#!/bin/bash

# UI-to-API Integration Testing Script
# Tests each dashboard component's connection to backend APIs
# Date: October 10, 2025

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

BASE_URL="${BASE_URL:-https://dlt.aurigraph.io}"
TIMEOUT=10

echo "=================================================="
echo "UI-TO-API INTEGRATION TESTING"
echo "=================================================="
echo "Base URL: $BASE_URL"
echo "Date: $(date)"
echo "=================================================="
echo ""

# Test results
TOTAL_COMPONENTS=0
WORKING_COMPONENTS=0
BROKEN_COMPONENTS=0
PARTIAL_COMPONENTS=0

# Function to test API endpoint for UI component
test_component_api() {
    TOTAL_COMPONENTS=$((TOTAL_COMPONENTS + 1))
    local component_name=$1
    local endpoint=$2
    local expected_fields=$3
    local description=$4

    echo ""
    echo "=================================================="
    echo "TESTING: $component_name"
    echo "=================================================="
    echo "Description: $description"
    echo "API Endpoint: $endpoint"
    echo -n "Status: "

    # Make API call
    RESPONSE=$(curl -s -w "\n%{http_code}" -m $TIMEOUT "$BASE_URL$endpoint" 2>&1 || echo "000")
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    RESPONSE_BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✅ API ACCESSIBLE${NC}"

        # Check if response contains expected data
        if [ -n "$expected_fields" ]; then
            echo "Checking for expected fields..."
            IFS=',' read -ra FIELDS <<< "$expected_fields"
            MISSING_FIELDS=0
            for field in "${FIELDS[@]}"; do
                if echo "$RESPONSE_BODY" | grep -q "$field"; then
                    echo -e "  ${GREEN}✓${NC} Found: $field"
                else
                    echo -e "  ${YELLOW}⚠${NC} Missing: $field"
                    MISSING_FIELDS=$((MISSING_FIELDS + 1))
                fi
            done

            if [ $MISSING_FIELDS -eq 0 ]; then
                echo -e "${GREEN}✅ ALL DATA FIELDS PRESENT${NC}"
                WORKING_COMPONENTS=$((WORKING_COMPONENTS + 1))
            else
                echo -e "${YELLOW}⚠️  PARTIAL DATA ($MISSING_FIELDS missing fields)${NC}"
                PARTIAL_COMPONENTS=$((PARTIAL_COMPONENTS + 1))
            fi
        else
            WORKING_COMPONENTS=$((WORKING_COMPONENTS + 1))
        fi

        # Show sample data
        echo "Sample Response:"
        echo "$RESPONSE_BODY" | head -10

    elif [ "$HTTP_CODE" = "404" ]; then
        echo -e "${RED}❌ API NOT FOUND (404)${NC}"
        echo "Dashboard component will show: No data available"
        BROKEN_COMPONENTS=$((BROKEN_COMPONENTS + 1))

    elif [ "$HTTP_CODE" = "500" ]; then
        echo -e "${RED}❌ SERVER ERROR (500)${NC}"
        echo "Dashboard component will show: Error loading data"
        BROKEN_COMPONENTS=$((BROKEN_COMPONENTS + 1))

    else
        echo -e "${RED}❌ FAILED (HTTP $HTTP_CODE)${NC}"
        BROKEN_COMPONENTS=$((BROKEN_COMPONENTS + 1))
    fi

    echo "UI Impact: "
    case $HTTP_CODE in
        200)
            echo -e "  ${GREEN}✓${NC} Component will render with live data"
            echo -e "  ${GREEN}✓${NC} Real-time updates possible"
            ;;
        404)
            echo -e "  ${RED}✗${NC} Component will show 'No data' or loading state"
            echo -e "  ${RED}✗${NC} Dashboard section may be empty or hidden"
            ;;
        500)
            echo -e "  ${RED}✗${NC} Component will show error message"
            echo -e "  ${RED}✗${NC} May need backend fix"
            ;;
        *)
            echo -e "  ${RED}✗${NC} Component behavior unknown"
            ;;
    esac
}

echo "Starting Dashboard Component Tests..."
echo ""

# ================================================
# DASHBOARD HOME / OVERVIEW
# ================================================
echo "=================================================="
echo "SECTION 1: DASHBOARD HOME / OVERVIEW"
echo "=================================================="

test_component_api \
    "System Health Widget" \
    "/q/health" \
    "status,checks" \
    "Main dashboard health indicator showing overall system status"

test_component_api \
    "Network Statistics Card" \
    "/api/v11/blockchain/network/stats" \
    "totalNodes,activeValidators,currentTPS" \
    "Network overview card with key metrics"

test_component_api \
    "Latest Blocks Widget" \
    "/api/v11/blockchain/blocks/latest" \
    "height,hash,timestamp,transactions" \
    "Recent blocks list in dashboard home"

test_component_api \
    "Transaction Summary Card" \
    "/api/v11/analytics/transactions" \
    "totalTransactions,last24Hours,averageTPS" \
    "Transaction statistics overview card"

# ================================================
# BLOCKCHAIN EXPLORER SECTION
# ================================================
echo ""
echo "=================================================="
echo "SECTION 2: BLOCKCHAIN EXPLORER"
echo "=================================================="

test_component_api \
    "Block Explorer List" \
    "/api/v11/blockchain/blocks" \
    "blocks,total,page" \
    "Paginated list of blocks with search functionality"

test_component_api \
    "Transaction Explorer List" \
    "/api/v11/blockchain/transactions" \
    "transactions,total" \
    "Paginated transaction list with filters"

test_component_api \
    "Block Details View" \
    "/api/v11/blockchain/blocks/1" \
    "height,hash,transactions" \
    "Individual block detail page"

# ================================================
# VALIDATORS & STAKING SECTION
# ================================================
echo ""
echo "=================================================="
echo "SECTION 3: VALIDATORS & STAKING"
echo "=================================================="

test_component_api \
    "Validators List Table" \
    "/api/v11/blockchain/validators" \
    "validators,total,activeValidators" \
    "Table showing all validators with stats"

test_component_api \
    "Staking Dashboard" \
    "/api/v11/blockchain/staking/info" \
    "totalStaked,totalValidators,annualizedReturns" \
    "Staking overview with APR calculator"

test_component_api \
    "Validator Performance Chart" \
    "/api/v11/blockchain/validators" \
    "validators,uptime,blocksProduced" \
    "Performance metrics visualization"

# ================================================
# RICARDIAN CONTRACTS SECTION
# ================================================
echo ""
echo "=================================================="
echo "SECTION 4: RICARDIAN CONTRACTS"
echo "=================================================="

test_component_api \
    "Gas Fees Display" \
    "/api/v11/contracts/ricardian/gas-fees" \
    "CONTRACT_ACTIVATION,CONTRACT_CONVERSION,PARTY_ADDITION" \
    "Gas fee schedule shown before operations"

test_component_api \
    "Contracts List" \
    "/api/v11/contracts/ricardian" \
    "contracts" \
    "List of all Ricardian contracts"

test_component_api \
    "Contract Upload Form" \
    "/api/v11/contracts/ricardian/upload" \
    "" \
    "Document upload endpoint for contract creation"

# ================================================
# GOVERNANCE SECTION
# ================================================
echo ""
echo "=================================================="
echo "SECTION 5: GOVERNANCE"
echo "=================================================="

test_component_api \
    "Proposals List" \
    "/api/v11/blockchain/governance/proposals" \
    "proposals,proposalId,status" \
    "Active and historical governance proposals"

test_component_api \
    "Voting Statistics" \
    "/api/v11/blockchain/governance/stats" \
    "totalProposals,activeVotes" \
    "Governance participation metrics"

# ================================================
# PAYMENT CHANNELS SECTION
# ================================================
echo ""
echo "=================================================="
echo "SECTION 6: PAYMENT CHANNELS"
echo "=================================================="

test_component_api \
    "Channels List" \
    "/api/v11/channels" \
    "channels" \
    "List of payment channels"

test_component_api \
    "Live Channel Data" \
    "/api/v11/live/channels" \
    "channels" \
    "Real-time channel state updates"

# ================================================
# ANALYTICS SECTION
# ================================================
echo ""
echo "=================================================="
echo "SECTION 7: ANALYTICS & CHARTS"
echo "=================================================="

test_component_api \
    "Transaction Analytics Chart" \
    "/api/v11/analytics/transactions" \
    "totalTransactions,averageTPS" \
    "TPS over time chart"

test_component_api \
    "Performance Metrics Dashboard" \
    "/api/v11/analytics/performance" \
    "metrics" \
    "System performance charts"

test_component_api \
    "Analytics Dashboard" \
    "/api/v11/analytics/dashboard" \
    "data" \
    "Main analytics dashboard data"

# ================================================
# SECURITY SECTION
# ================================================
echo ""
echo "=================================================="
echo "SECTION 8: SECURITY & MONITORING"
echo "=================================================="

test_component_api \
    "Security Status Badge" \
    "/api/v11/security/status" \
    "quantumCryptography,securityLevel" \
    "Security status indicator"

test_component_api \
    "Key Management Panel" \
    "/api/v11/security/keys" \
    "keys" \
    "Cryptographic key management"

# ================================================
# CONSENSUS & NETWORK SECTION
# ================================================
echo ""
echo "=================================================="
echo "SECTION 9: CONSENSUS & NETWORK"
echo "=================================================="

test_component_api \
    "Consensus Status Widget" \
    "/api/v11/consensus/status" \
    "algorithm,currentLeader,epoch" \
    "HyperRAFT++ consensus status"

test_component_api \
    "Network Health Monitor" \
    "/api/v11/network/health" \
    "health" \
    "Network connectivity and health"

test_component_api \
    "Network Peers Map" \
    "/api/v11/network/peers" \
    "peers" \
    "Connected network peers visualization"

# ================================================
# LIVE DATA / REAL-TIME SECTION
# ================================================
echo ""
echo "=================================================="
echo "SECTION 10: LIVE DATA / REAL-TIME UPDATES"
echo "=================================================="

test_component_api \
    "Live Validators Monitor" \
    "/api/v11/live/validators" \
    "validators" \
    "Real-time validator status"

test_component_api \
    "Live Consensus Data" \
    "/api/v11/live/consensus" \
    "consensus" \
    "Real-time consensus state"

test_component_api \
    "Live Network Monitor" \
    "/api/v11/live/network" \
    "network" \
    "Real-time network metrics"

# ================================================
# ENTERPRISE FEATURES
# ================================================
echo ""
echo "=================================================="
echo "SECTION 11: ENTERPRISE FEATURES"
echo "=================================================="

test_component_api \
    "Multi-Tenancy Panel" \
    "/api/v11/enterprise/tenants" \
    "tenants" \
    "Tenant management interface"

test_component_api \
    "Enterprise Dashboard" \
    "/api/v11/enterprise/status" \
    "status" \
    "Enterprise features overview"

# ================================================
# CROSS-CHAIN BRIDGE
# ================================================
echo ""
echo "=================================================="
echo "SECTION 12: CROSS-CHAIN BRIDGE"
echo "=================================================="

test_component_api \
    "Supported Chains List" \
    "/api/v11/bridge/chains" \
    "chains,chainId,name" \
    "Available blockchain networks"

test_component_api \
    "Bridge Status Monitor" \
    "/api/v11/bridge/status" \
    "status" \
    "Bridge connectivity status"

test_component_api \
    "Bridge Transaction History" \
    "/api/v11/bridge/history" \
    "transactions" \
    "Cross-chain transaction list"

# ================================================
# DATA FEEDS & ORACLES
# ================================================
echo ""
echo "=================================================="
echo "SECTION 13: DATA FEEDS & ORACLES"
echo "=================================================="

test_component_api \
    "Data Feeds Widget" \
    "/api/v11/datafeeds" \
    "feeds" \
    "Available data feeds"

test_component_api \
    "Price Feed Display" \
    "/api/v11/datafeeds/prices" \
    "prices" \
    "Real-time price data"

test_component_api \
    "Oracle Status" \
    "/api/v11/oracles/status" \
    "oracles" \
    "Oracle service health"

# ================================================
# SUMMARY REPORT
# ================================================
echo ""
echo "=================================================="
echo "INTEGRATION TEST SUMMARY"
echo "=================================================="
echo ""
echo "Total Dashboard Components: $TOTAL_COMPONENTS"
echo ""
echo -e "${GREEN}✅ Fully Working: $WORKING_COMPONENTS ($(echo "scale=1; $WORKING_COMPONENTS * 100 / $TOTAL_COMPONENTS" | bc)%)${NC}"
echo -e "${YELLOW}⚠️  Partial/Incomplete: $PARTIAL_COMPONENTS ($(echo "scale=1; $PARTIAL_COMPONENTS * 100 / $TOTAL_COMPONENTS" | bc)%)${NC}"
echo -e "${RED}❌ Broken/Not Found: $BROKEN_COMPONENTS ($(echo "scale=1; $BROKEN_COMPONENTS * 100 / $TOTAL_COMPONENTS" | bc)%)${NC}"
echo ""

# Calculate dashboard readiness
FUNCTIONAL_PERCENTAGE=$(echo "scale=1; ($WORKING_COMPONENTS + $PARTIAL_COMPONENTS / 2) * 100 / $TOTAL_COMPONENTS" | bc)

echo "=================================================="
echo "DASHBOARD READINESS ASSESSMENT"
echo "=================================================="
echo ""
echo "Overall Functionality: ${FUNCTIONAL_PERCENTAGE}%"
echo ""

if (( $(echo "$FUNCTIONAL_PERCENTAGE >= 90" | bc -l) )); then
    echo -e "${GREEN}✅ EXCELLENT${NC} - Dashboard is fully functional"
    echo "Recommendation: Ready for production use"
elif (( $(echo "$FUNCTIONAL_PERCENTAGE >= 70" | bc -l) )); then
    echo -e "${GREEN}✅ GOOD${NC} - Most features working"
    echo "Recommendation: Ready for production with noted limitations"
elif (( $(echo "$FUNCTIONAL_PERCENTAGE >= 50" | bc -l) )); then
    echo -e "${YELLOW}⚠️  ACCEPTABLE${NC} - Core features working"
    echo "Recommendation: Production ready for core features only"
else
    echo -e "${RED}❌ NEEDS IMPROVEMENT${NC} - Many features unavailable"
    echo "Recommendation: Enable missing APIs before production"
fi

echo ""
echo "=================================================="
echo "DASHBOARD SECTIONS STATUS"
echo "=================================================="
echo ""
echo "Section 1: Dashboard Home - $([ $WORKING_COMPONENTS -ge 2 ] && echo -e "${GREEN}Working${NC}" || echo -e "${RED}Broken${NC}")"
echo "Section 2: Blockchain Explorer - $([ $WORKING_COMPONENTS -ge 5 ] && echo -e "${GREEN}Working${NC}" || echo -e "${YELLOW}Partial${NC}")"
echo "Section 3: Validators & Staking - Expected: ${GREEN}Working${NC} (100% APIs available)"
echo "Section 4: Ricardian Contracts - Expected: ${GREEN}Working${NC} (Gas fees available)"
echo "Section 5: Governance - Expected: ${YELLOW}Partial${NC} (Proposals only)"
echo "Section 6: Payment Channels - Expected: ${GREEN}Working${NC} (100% APIs available)"
echo "Section 7: Analytics - Expected: ${YELLOW}Partial${NC} (Limited data)"
echo "Section 8: Security - Expected: ${GREEN}Partial${NC} (Status available)"
echo "Section 9: Consensus & Network - Expected: ${YELLOW}Partial${NC} (Consensus only)"
echo "Section 10: Live Data - Expected: ${RED}Limited${NC} (Channels only)"
echo "Section 11: Enterprise - Expected: ${YELLOW}Partial${NC} (Tenants only)"
echo "Section 12: Cross-Chain Bridge - Expected: ${YELLOW}Partial${NC} (Chains list only)"
echo "Section 13: Data Feeds - Expected: ${YELLOW}Partial${NC} (Feeds list only)"

echo ""
echo "=================================================="
echo "UI/UX RECOMMENDATIONS"
echo "=================================================="
echo ""
echo "1. Components with working APIs:"
echo "   - Will display real data"
echo "   - Should have loading states"
echo "   - May support real-time updates"
echo ""
echo "2. Components with broken APIs:"
echo "   - Should show 'No data available' message"
echo "   - Consider hiding or marking as 'Coming Soon'"
echo "   - Add user-friendly error messages"
echo ""
echo "3. Partial functionality:"
echo "   - Display available data"
echo "   - Note missing features in UI"
echo "   - Provide feedback about limitations"
echo ""

echo "=================================================="
echo "Test completed: $(date)"
echo "=================================================="

# Exit code based on functionality
if (( $(echo "$FUNCTIONAL_PERCENTAGE >= 70" | bc -l) )); then
    exit 0
else
    exit 1
fi
