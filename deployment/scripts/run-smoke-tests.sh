#!/bin/bash

# Smoke Tests for Aurigraph V11
# Critical path validation - runs on every commit
# Duration: ~5 minutes
# Must Pass: 100%

set -e

echo "🚬 Running Smoke Tests for Aurigraph V11..."
echo "================================================"
echo ""

# Configuration
BASE_URL="${BASE_URL:-http://localhost:9003}"
TIMEOUT=10

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Helper function to run test
run_test() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    TEST_NAME=$1
    TEST_CMD=$2

    echo -n "  [TEST $TOTAL_TESTS] $TEST_NAME... "

    if eval "$TEST_CMD" > /dev/null 2>&1; then
        echo -e "${GREEN}PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}FAIL${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Wait for service to be ready
echo "⏳ Waiting for service to be ready at $BASE_URL..."
RETRIES=30
until curl -s "$BASE_URL/q/health" > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    sleep 2
    RETRIES=$((RETRIES - 1))
done

if [ $RETRIES -eq 0 ]; then
    echo -e "\n${RED}❌ Service failed to start${NC}"
    exit 1
fi

echo -e "\n${GREEN}✅ Service is ready${NC}\n"

# ====================
# SMOKE TESTS
# ====================

echo "📋 Running Critical Path Tests..."
echo ""

# TC-API-001: Health Check
run_test "Health Check Endpoint" \
    "curl -s -f -m $TIMEOUT $BASE_URL/q/health | grep -q 'UP'"

# TC-API-002: System Info
run_test "System Info Endpoint" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/info | grep -q 'version'"

# TC-API-003: Stats Endpoint
run_test "Stats Endpoint" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/stats | grep -q 'timestamp'"

# TC-API-004: Live Validators
run_test "Live Validators Endpoint" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/live/validators | grep -q 'validators'"

# TC-API-005: Live Consensus
run_test "Live Consensus Endpoint" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/live/consensus | grep -q 'nodeId'"

# TC-API-006: Live Channels
run_test "Live Channels Endpoint" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/live/channels | grep -q 'channels'"

# TC-RC-010: Gas Fee Query
run_test "Gas Fee Rates Endpoint" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/contracts/ricardian/gas-fees | grep -q 'DOCUMENT_UPLOAD'"

# TC-API-007: Performance Endpoint
run_test "Performance Metrics Endpoint" \
    "curl -s -f -m $TIMEOUT $BASE_URL/api/v11/performance | grep -q 'tps'"

# TC-API-008: Quarkus Health
run_test "Quarkus Health Checks" \
    "curl -s -f -m $TIMEOUT $BASE_URL/q/health/live | grep -q 'UP'"

# TC-API-009: Quarkus Metrics
run_test "Prometheus Metrics Endpoint" \
    "curl -s -f -m $TIMEOUT $BASE_URL/q/metrics | grep -q 'jvm'"

echo ""
echo "================================================"
echo "📊 Smoke Test Results"
echo "================================================"
echo -e "Total Tests:  $TOTAL_TESTS"
echo -e "Passed:       ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:       ${RED}$FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ All smoke tests passed!${NC}"
    echo ""
    exit 0
else
    PASS_RATE=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)
    echo -e "${RED}❌ $FAILED_TESTS test(s) failed (Pass rate: ${PASS_RATE}%)${NC}"
    echo ""
    echo "🚨 SMOKE TESTS MUST HAVE 100% PASS RATE"
    echo ""
    exit 1
fi
