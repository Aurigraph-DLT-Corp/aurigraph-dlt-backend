#!/bin/bash

# Comprehensive E2E Testing Script for Aurigraph V11.3.0
# Date: October 15, 2025
# Purpose: Test all endpoints and features

SERVER="https://dlt.aurigraph.io"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

declare -a ISSUES=()

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_test() {
    echo -e "${YELLOW}[TEST]${NC} $1"
}

log_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((PASSED_TESTS++))
}

log_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((FAILED_TESTS++))
    ISSUES+=("$1")
}

test_endpoint() {
    local name="$1"
    local url="$2"
    local expected_status="$3"
    local check_field="$4"

    ((TOTAL_TESTS++))
    log_test "$name"

    response=$(curl -sk -w "\n%{http_code}" "$url" 2>&1)
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "$expected_status" ]; then
        if [ -n "$check_field" ]; then
            if echo "$body" | jq -e "$check_field" > /dev/null 2>&1; then
                log_pass "$name - HTTP $http_code, field $check_field present"
                return 0
            else
                log_fail "$name - HTTP $http_code but field $check_field missing"
                return 1
            fi
        else
            log_pass "$name - HTTP $http_code"
            return 0
        fi
    else
        log_fail "$name - Expected $expected_status, got $http_code"
        return 1
    fi
}

echo "========================================="
echo "Aurigraph V11.3.0 Comprehensive E2E Tests"
echo "========================================="
echo "Server: $SERVER"
echo "Date: $(date)"
echo "========================================="
echo ""

# ==================== CORE HEALTH TESTS ====================
echo "### Core Health & Info Tests ###"

test_endpoint "Health Check" "$SERVER/api/v11/health" "200" ".status"
test_endpoint "System Info" "$SERVER/api/v11/info" "200" ".platform.version"
test_endpoint "System Status" "$SERVER/api/v11/system/status" "200" ".healthy"
test_endpoint "Transaction Stats" "$SERVER/api/v11/stats" "200" ".totalProcessed"

# ==================== PERFORMANCE TESTS ====================
echo ""
echo "### Performance Tests ###"

test_endpoint "Performance Endpoint" "$SERVER/api/v11/performance" "200" ".transactionsPerSecond"
test_endpoint "Performance Reactive" "$SERVER/api/v11/performance/reactive" "200" ""

# ==================== CONSENSUS TESTS ====================
echo ""
echo "### Consensus Tests ###"

test_endpoint "Consensus Status" "$SERVER/api/v11/consensus/status" "200" ".state"
test_endpoint "Consensus Metrics" "$SERVER/api/v11/consensus/metrics" "200" ""

# ==================== BLOCKCHAIN TESTS ====================
echo ""
echo "### Blockchain Tests ###"

test_endpoint "Latest Block" "$SERVER/api/v11/blockchain/latest" "200" ""
test_endpoint "Block Info" "$SERVER/api/v11/blockchain/block/0" "200" ""
test_endpoint "Blockchain Stats" "$SERVER/api/v11/blockchain/stats" "200" ""

# ==================== QUANTUM CRYPTO TESTS ====================
echo ""
echo "### Quantum Cryptography Tests ###"

test_endpoint "Crypto Status" "$SERVER/api/v11/crypto/status" "200" ".quantumCryptoEnabled"
test_endpoint "Crypto Metrics" "$SERVER/api/v11/crypto/metrics" "200" ""

# ==================== CROSS-CHAIN BRIDGE TESTS ====================
echo ""
echo "### Cross-Chain Bridge Tests ###"

test_endpoint "Bridge Status" "$SERVER/api/v11/bridge/status" "200" ".overall_status"
test_endpoint "Bridge Stats" "$SERVER/api/v11/bridge/stats" "200" ""
test_endpoint "Supported Chains" "$SERVER/api/v11/bridge/supported-chains" "200" ""

# ==================== AI OPTIMIZATION TESTS ====================
echo ""
echo "### AI Optimization Tests ###"

test_endpoint "AI Status" "$SERVER/api/v11/ai/status" "200" ""
test_endpoint "AI Metrics" "$SERVER/api/v11/ai/metrics" "200" ""
test_endpoint "AI Predictions" "$SERVER/api/v11/ai/predictions" "200" ""

# ==================== SECURITY TESTS ====================
echo ""
echo "### Security & Audit Tests ###"

test_endpoint "Security Audit Status" "$SERVER/api/v11/security/audit/status" "200" ".auditEnabled"

# ==================== MONITORING TESTS ====================
echo ""
echo "### Monitoring & Metrics Tests ###"

test_endpoint "Prometheus Metrics" "$SERVER/q/metrics" "200" ""
test_endpoint "OpenAPI Spec" "$SERVER/q/openapi" "200" ""

# ==================== RWA TESTS ====================
echo ""
echo "### Real-World Asset Tests ###"

test_endpoint "RWA Status" "$SERVER/api/v11/rwa/status" "200" ""

# ==================== PORTAL TEST ====================
echo ""
echo "### Enterprise Portal Test ###"

((TOTAL_TESTS++))
log_test "Enterprise Portal Access"
portal_response=$(curl -sk -w "\n%{http_code}" "$SERVER/enterprise" 2>&1)
portal_code=$(echo "$portal_response" | tail -1)
portal_body=$(echo "$portal_response" | sed '$d')

if [ "$portal_code" = "200" ]; then
    if echo "$portal_body" | grep -q "Release 11.3.0"; then
        log_pass "Enterprise Portal - v11.3.0 accessible"
    else
        log_fail "Enterprise Portal - Accessible but wrong version"
    fi
else
    log_fail "Enterprise Portal - HTTP $portal_code"
fi

# ==================== STRESS TESTS ====================
echo ""
echo "### Stress & Load Tests ###"

((TOTAL_TESTS++))
log_test "Performance Stress Test (1000 iterations, 10 threads)"
stress_response=$(curl -sk "$SERVER/api/v11/performance?iterations=1000&threads=10" 2>&1)
if echo "$stress_response" | jq -e '.transactionsPerSecond' > /dev/null 2>&1; then
    tps=$(echo "$stress_response" | jq -r '.transactionsPerSecond')
    tps_int=$(printf "%.0f" "$tps")
    if [ "$tps_int" -gt 50000 ]; then
        log_pass "Stress Test - Achieved $tps_int TPS (> 50K baseline)"
    else
        log_fail "Stress Test - Only $tps_int TPS (< 50K baseline)"
    fi
else
    log_fail "Stress Test - Invalid response"
fi

# ==================== SUMMARY ====================
echo ""
echo "========================================="
echo "TEST SUMMARY"
echo "========================================="
echo "Total Tests: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"
if [ $TOTAL_TESTS -gt 0 ]; then
    success_rate=$(awk "BEGIN {printf \"%.1f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")
    echo "Success Rate: ${success_rate}%"
else
    echo "Success Rate: N/A"
fi
echo "========================================="

if [ $FAILED_TESTS -gt 0 ]; then
    echo ""
    echo "ISSUES FOUND:"
    for issue in "${ISSUES[@]}"; do
        echo "  - $issue"
    done
    exit 1
else
    echo ""
    echo "ALL TESTS PASSED ✅"
    exit 0
fi
