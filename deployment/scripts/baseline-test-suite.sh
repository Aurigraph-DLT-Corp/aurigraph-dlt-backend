#!/bin/bash

# Aurigraph DLT Baseline Test Suite
# Version: baseline-v1.1-V11.3.0
# Date: October 15, 2025
#
# This script verifies the baseline release is properly deployed and functional.
# All tests must PASS for the baseline to be considered valid.

set -e

API_BASE="https://dlt.aurigraph.io/api/v11"
QUARKUS_BASE="https://dlt.aurigraph.io/q"
PORTAL_URL="https://dlt.aurigraph.io/enterprise"

PASSED=0
FAILED=0
TOTAL=11

echo "=========================================="
echo "  Aurigraph DLT Baseline Test Suite"
echo "  baseline-v1.1-V11.3.0"
echo "=========================================="
echo ""
echo "Testing Production Deployment..."
echo "API Base: $API_BASE"
echo "Portal: $PORTAL_URL"
echo ""

# Helper function to run tests
run_test() {
    local test_num=$1
    local test_name="$2"
    local test_command="$3"
    local expected="$4"

    echo "[$test_num/$TOTAL] $test_name"

    result=$(eval $test_command 2>&1)

    if echo "$result" | grep -q "$expected"; then
        echo "    ✅ PASSED"
        echo "    Result: $result"
        PASSED=$((PASSED + 1))
    else
        echo "    ❌ FAILED"
        echo "    Expected: $expected"
        echo "    Got: $result"
        FAILED=$((FAILED + 1))
    fi
    echo ""
}

echo "=========================================="
echo " BASELINE TESTS"
echo "=========================================="
echo ""

# Test 1: Backend Health
run_test 1 "Backend Health Check" \
    "curl -s $API_BASE/health | jq -r '.status'" \
    "HEALTHY"

# Test 2: Backend Version
run_test 2 "Backend Version Verification" \
    "curl -s $API_BASE/info | jq -r '.build.version'" \
    "11.3.0"

# Test 3: Portal Version
run_test 3 "Portal Version Verification" \
    "curl -s $PORTAL_URL | grep -o 'Release 1.1.0' | head -1" \
    "Release 1.1.0"

# Test 4: Performance Baseline
echo "[4/$TOTAL] Performance Baseline Test"
TPS=$(curl -s "$API_BASE/performance?iterations=1000&threads=5" | jq -r '.transactionsPerSecond')
if (( $(echo "$TPS > 100000" | bc -l) )); then
    echo "    ✅ PASSED"
    echo "    Result: $TPS TPS (>100K required)"
    PASSED=$((PASSED + 1))
else
    echo "    ❌ FAILED"
    echo "    Result: $TPS TPS (<100K)"
    FAILED=$((FAILED + 1))
fi
echo ""

# Test 5: Consensus State
run_test 5 "Consensus State Verification" \
    "curl -s $API_BASE/consensus/status | jq -r '.state'" \
    "LEADER\|FOLLOWER"

# Test 6: Quantum Crypto
run_test 6 "Quantum Cryptography Verification" \
    "curl -s $API_BASE/crypto/status | jq -r '.quantumCryptoEnabled'" \
    "true"

# Test 7: Bridge Health
run_test 7 "Cross-Chain Bridge Health" \
    "curl -s $API_BASE/bridge/stats | jq -r '.healthy'" \
    "true"

# Test 8: System Status
run_test 8 "System Status Verification" \
    "curl -s $API_BASE/system/status | jq -r '.healthy'" \
    "true"

# Test 9: Transaction Stats
echo "[9/$TOTAL] Transaction Stats Availability"
TOTAL_TX=$(curl -s $API_BASE/stats | jq -r '.totalProcessed')
if (( $(echo "$TOTAL_TX > 0" | bc -l) )); then
    echo "    ✅ PASSED"
    echo "    Result: $TOTAL_TX transactions"
    PASSED=$((PASSED + 1))
else
    echo "    ❌ FAILED"
    echo "    Result: $TOTAL_TX transactions"
    FAILED=$((FAILED + 1))
fi
echo ""

# Test 10: Prometheus Metrics
echo "[10/$TOTAL] Prometheus Metrics Availability"
METRIC_COUNT=$(curl -s $QUARKUS_BASE/metrics | grep -c "jvm_memory" || echo "0")
if [ "$METRIC_COUNT" -gt 0 ]; then
    echo "    ✅ PASSED"
    echo "    Result: $METRIC_COUNT metrics found"
    PASSED=$((PASSED + 1))
else
    echo "    ❌ FAILED"
    echo "    Result: No metrics found"
    FAILED=$((FAILED + 1))
fi
echo ""

# Test 11: OpenAPI Spec
echo "[11/$TOTAL] OpenAPI Specification Availability"
OPENAPI_COUNT=$(curl -s $QUARKUS_BASE/openapi | grep -c "openapi" || echo "0")
if [ "$OPENAPI_COUNT" -gt 0 ]; then
    echo "    ✅ PASSED"
    echo "    Result: OpenAPI spec available"
    PASSED=$((PASSED + 1))
else
    echo "    ❌ FAILED"
    echo "    Result: OpenAPI spec not found"
    FAILED=$((FAILED + 1))
fi
echo ""

echo "=========================================="
echo " BASELINE TEST RESULTS"
echo "=========================================="
echo ""
echo "Total Tests: $TOTAL"
echo "Passed: $PASSED ✅"
echo "Failed: $FAILED ❌"
echo ""

if [ $FAILED -eq 0 ]; then
    echo "Status: ✅ BASELINE VERIFIED"
    echo ""
    echo "All baseline tests passed successfully."
    echo "The baseline release is properly deployed and functional."
    echo ""
    exit 0
else
    echo "Status: ❌ BASELINE VERIFICATION FAILED"
    echo ""
    echo "Some baseline tests failed."
    echo "Please check the deployment and ensure all services are running."
    echo ""
    exit 1
fi
