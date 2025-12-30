#!/bin/bash

# Phase 2 Medium-Priority Endpoints Test Script
# Tests all 15 implemented endpoints from Phase 2

BASE_URL="http://localhost:9003/api/v11"
PASSED=0
FAILED=0

echo "========================================="
echo "PHASE 2: MEDIUM-PRIORITY ENDPOINTS TEST"
echo "========================================="
echo ""

# Helper function to test endpoint
test_endpoint() {
    local METHOD=$1
    local ENDPOINT=$2
    local DATA=$3
    local DESC=$4

    echo -n "Testing: $DESC ... "

    if [ "$METHOD" = "GET" ]; then
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$ENDPOINT")
    else
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X "$METHOD" -H "Content-Type: application/json" -d "$DATA" "$BASE_URL$ENDPOINT")
    fi

    if [[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "201" ]]; then
        echo "✓ PASSED (HTTP $HTTP_CODE)"
        ((PASSED++))
    else
        echo "✗ FAILED (HTTP $HTTP_CODE)"
        ((FAILED++))
    fi
}

# AI Endpoints (3)
echo "=== AI ENDPOINTS (3) ==="
test_endpoint "GET" "/ai/status" "" "GET /api/v11/ai/status"
test_endpoint "GET" "/ai/training/status" "" "GET /api/v11/ai/training/status"
test_endpoint "POST" "/ai/models/model123/config" '{"modelType":"neural","batchSize":64}' "POST /api/v11/ai/models/{id}/config"

# Security Endpoints (3)
echo ""
echo "=== SECURITY ENDPOINTS (3) ==="
test_endpoint "GET" "/security/keys/key123" "" "GET /api/v11/security/keys/{id}"
test_endpoint "DELETE" "/security/keys/key123" "" "DELETE /api/v11/security/keys/{id}"
test_endpoint "GET" "/security/vulnerabilities" "" "GET /api/v11/security/vulnerabilities"

# RWA Endpoints (5)
echo ""
echo "=== RWA ENDPOINTS (5) ==="
test_endpoint "GET" "/rwa/valuation" "" "GET /api/v11/rwa/valuation"
test_endpoint "POST" "/rwa/portfolio" '{"portfolioName":"Portfolio1","assets":["ASSET1","ASSET2"]}' "POST /api/v11/rwa/portfolio"
test_endpoint "GET" "/rwa/compliance/token123" "" "GET /api/v11/rwa/compliance/{tokenId}"
test_endpoint "POST" "/rwa/fractional" '{"tokenId":"token123","shares":1000}' "POST /api/v11/rwa/fractional"
test_endpoint "GET" "/rwa/dividends" "" "GET /api/v11/rwa/dividends"

# Bridge Endpoints (3)
echo ""
echo "=== BRIDGE ENDPOINTS (3) ==="
test_endpoint "GET" "/bridge/liquidity" "" "GET /api/v11/bridge/liquidity"
test_endpoint "GET" "/bridge/fees" "" "GET /api/v11/bridge/fees"
test_endpoint "GET" "/bridge/transfers/tx123" "" "GET /api/v11/bridge/transfers/{txId}"

# Summary
echo ""
echo "========================================="
echo "PHASE 2 TEST SUMMARY"
echo "========================================="
echo "Total Tests: $((PASSED + FAILED))"
echo "Passed: $PASSED"
echo "Failed: $FAILED"
if [ $FAILED -eq 0 ]; then
    echo "Status: ✓ ALL TESTS PASSED"
    exit 0
else
    echo "Status: ✗ SOME TESTS FAILED"
    exit 1
fi
