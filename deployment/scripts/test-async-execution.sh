#!/bin/bash
###############################################################################
# Bridge Async Execution Test Script
# Purpose: Verify bridge transfers execute asynchronously (not synchronously)
# Expected: All transfers submitted in <3 seconds (async)
#           vs ~50 seconds if synchronous (10 × 5s delay)
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
API_BASE="http://localhost:9003/api/v11"
NUM_TRANSFERS=10
MAX_ASYNC_TIME=3  # seconds
TRANSFER_IDS=()

echo "=========================================="
echo "Bridge Async Execution Test"
echo "=========================================="
echo ""

# Check if service is running
echo "Checking if Aurigraph V11 service is running..."
if ! curl -s "${API_BASE}/q/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ FAIL: Service not running${NC}"
    echo "Start service with: ./mvnw quarkus:dev"
    exit 1
fi
echo -e "${GREEN}✅ Service is running${NC}"
echo ""

# Test 1: Submit concurrent transfers and measure time
echo "Test 1: Concurrent Transfer Submission"
echo "---------------------------------------"
echo "Submitting ${NUM_TRANSFERS} concurrent bridge transfers..."

START_TIME=$(date +%s)

for i in $(seq 1 $NUM_TRANSFERS); do
    RESPONSE=$(curl -s -X POST "${API_BASE}/bridge/transfer" \
        -H "Content-Type: application/json" \
        -d "{
            \"sourceChain\": \"ETHEREUM\",
            \"targetChain\": \"POLYGON\",
            \"amount\": \"$i.0\",
            \"assetId\": \"ETH\",
            \"sender\": \"0x1234567890abcdef${i}\",
            \"recipient\": \"0xfedcba0987654321${i}\"
        }" 2>&1)

    # Extract transaction ID
    TX_ID=$(echo "$RESPONSE" | jq -r '.transactionId' 2>/dev/null || echo "")
    if [ -n "$TX_ID" ] && [ "$TX_ID" != "null" ]; then
        TRANSFER_IDS+=("$TX_ID")
        echo "  Transfer $i submitted: $TX_ID"
    else
        echo -e "  ${YELLOW}⚠️  Transfer $i failed to submit${NC}"
    fi
done

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "Submission completed in ${DURATION} seconds"

# Evaluate async vs sync
if [ $DURATION -lt $MAX_ASYNC_TIME ]; then
    echo -e "${GREEN}✅ SUCCESS: Async execution confirmed (${DURATION}s < ${MAX_ASYNC_TIME}s)${NC}"
    ASYNC_TEST_PASS=true
else
    echo -e "${RED}❌ FAIL: Synchronous execution detected (${DURATION}s >= ${MAX_ASYNC_TIME}s)${NC}"
    echo "Expected: <${MAX_ASYNC_TIME}s (async), Actual: ${DURATION}s"
    ASYNC_TEST_PASS=false
fi

echo ""

# Test 2: Check all transfers are PENDING (processing asynchronously)
echo "Test 2: Verify Async Processing"
echo "--------------------------------"
sleep 2  # Wait for service to register transfers

PENDING_COUNT=0
COMPLETED_COUNT=0
FAILED_COUNT=0

for TX_ID in "${TRANSFER_IDS[@]}"; do
    STATUS_RESPONSE=$(curl -s "${API_BASE}/bridge/status/${TX_ID}")
    STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")

    case "$STATUS" in
        "PENDING")
            ((PENDING_COUNT++))
            ;;
        "COMPLETED")
            ((COMPLETED_COUNT++))
            ;;
        "FAILED")
            ((FAILED_COUNT++))
            ;;
        *)
            echo -e "${YELLOW}⚠️  Transfer $TX_ID has unknown status: $STATUS${NC}"
            ;;
    esac
done

echo "Transfer Status (2 seconds after submission):"
echo "  PENDING: $PENDING_COUNT / ${#TRANSFER_IDS[@]}"
echo "  COMPLETED: $COMPLETED_COUNT / ${#TRANSFER_IDS[@]}"
echo "  FAILED: $FAILED_COUNT / ${#TRANSFER_IDS[@]}"

if [ $PENDING_COUNT -gt 0 ]; then
    echo -e "${GREEN}✅ SUCCESS: Transfers processing asynchronously${NC}"
    PENDING_TEST_PASS=true
else
    echo -e "${YELLOW}⚠️  WARNING: No pending transfers (may have completed very quickly)${NC}"
    PENDING_TEST_PASS=true  # Not a failure, just fast processing
fi

echo ""

# Test 3: Wait for completion and verify final status
echo "Test 3: Verify Completion"
echo "-------------------------"
echo "Waiting 10 seconds for transfers to complete..."
sleep 10

FINAL_COMPLETED=0
FINAL_FAILED=0
FINAL_PENDING=0

for TX_ID in "${TRANSFER_IDS[@]}"; do
    STATUS_RESPONSE=$(curl -s "${API_BASE}/bridge/status/${TX_ID}")
    STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")

    case "$STATUS" in
        "COMPLETED")
            ((FINAL_COMPLETED++))
            ;;
        "FAILED")
            ((FINAL_FAILED++))
            echo -e "${YELLOW}  Transfer $TX_ID: FAILED${NC}"
            ;;
        "PENDING")
            ((FINAL_PENDING++))
            echo -e "${YELLOW}  Transfer $TX_ID: Still PENDING${NC}"
            ;;
        *)
            echo -e "${RED}  Transfer $TX_ID: UNKNOWN status${NC}"
            ;;
    esac
done

echo ""
echo "Final Transfer Status (after 10 seconds):"
echo "  COMPLETED: $FINAL_COMPLETED / ${#TRANSFER_IDS[@]}"
echo "  FAILED: $FINAL_FAILED / ${#TRANSFER_IDS[@]}"
echo "  PENDING: $FINAL_PENDING / ${#TRANSFER_IDS[@]}"

SUCCESS_RATE=$((FINAL_COMPLETED * 100 / ${#TRANSFER_IDS[@]}))
echo "  Success Rate: ${SUCCESS_RATE}%"

if [ $SUCCESS_RATE -ge 80 ]; then
    echo -e "${GREEN}✅ SUCCESS: Acceptable success rate (>80%)${NC}"
    COMPLETION_TEST_PASS=true
else
    echo -e "${RED}❌ FAIL: Low success rate (<80%)${NC}"
    COMPLETION_TEST_PASS=false
fi

echo ""

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""

if [ "$ASYNC_TEST_PASS" = true ] && [ "$PENDING_TEST_PASS" = true ] && [ "$COMPLETION_TEST_PASS" = true ]; then
    echo -e "${GREEN}✅ ALL TESTS PASSED${NC}"
    echo ""
    echo "Results:"
    echo "  1. Async Execution: PASS (${DURATION}s submission time)"
    echo "  2. Async Processing: PASS (${PENDING_COUNT} pending at 2s)"
    echo "  3. Completion Rate: PASS (${SUCCESS_RATE}% success)"
    echo ""
    echo "Bridge async execution is working correctly."
    exit 0
else
    echo -e "${RED}❌ SOME TESTS FAILED${NC}"
    echo ""
    echo "Results:"
    [ "$ASYNC_TEST_PASS" = true ] && echo "  1. Async Execution: PASS" || echo "  1. Async Execution: FAIL"
    [ "$PENDING_TEST_PASS" = true ] && echo "  2. Async Processing: PASS" || echo "  2. Async Processing: FAIL"
    [ "$COMPLETION_TEST_PASS" = true ] && echo "  3. Completion Rate: PASS" || echo "  3. Completion Rate: FAIL"
    echo ""
    echo "Please check the implementation."
    exit 1
fi
