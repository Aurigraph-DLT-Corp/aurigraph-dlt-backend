#!/bin/bash
# Aurigraph V11 API Health Check Script
# Backend Development Agent (BDA)
# Date: October 12, 2025

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Aurigraph V11 REST API Health Check              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}\n"

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}Warning: jq not installed. Install for better output: brew install jq${NC}\n"
    HAS_JQ=false
else
    HAS_JQ=true
fi

TESTS_PASSED=0
TESTS_FAILED=0

# Test 1: Local HTTPS API
echo -e "${BLUE}[1/6] Testing Local HTTPS API (port 9443)...${NC}"
if RESPONSE=$(curl -ks https://localhost:9443/api/v11/health 2>/dev/null); then
    if [ "$HAS_JQ" = true ]; then
        STATUS=$(echo "$RESPONSE" | jq -r '.status')
        VERSION=$(echo "$RESPONSE" | jq -r '.version')
        UPTIME=$(echo "$RESPONSE" | jq -r '.uptimeSeconds')
        REQUESTS=$(echo "$RESPONSE" | jq -r '.totalRequests')

        echo -e "${GREEN}✓ Local HTTPS API: Accessible${NC}"
        echo -e "  Status: ${GREEN}$STATUS${NC}"
        echo -e "  Version: $VERSION"
        echo -e "  Uptime: ${UPTIME}s ($(($UPTIME / 3600))h $(($UPTIME % 3600 / 60))m)"
        echo -e "  Total Requests: $REQUESTS"
    else
        echo -e "${GREEN}✓ Local HTTPS API: Accessible${NC}"
        echo -e "  Response: $RESPONSE"
    fi
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ Local HTTPS API: Not accessible${NC}"
    echo -e "${YELLOW}  Troubleshooting:${NC}"
    echo -e "  - Check if application is running: ps aux | grep aurigraph-v11"
    echo -e "  - Check if port 9443 is listening: netstat -tuln | grep 9443"
    echo -e "  - Check application logs: tail -f logs/aurigraph-v11.log"
    ((TESTS_FAILED++))
fi

# Test 2: Quarkus Health
echo -e "\n${BLUE}[2/6] Testing Quarkus Health Endpoint...${NC}"
if RESPONSE=$(curl -ks https://localhost:9443/q/health 2>/dev/null); then
    if [ "$HAS_JQ" = true ]; then
        STATUS=$(echo "$RESPONSE" | jq -r '.status')
        CHECKS=$(echo "$RESPONSE" | jq '.checks | length')

        echo -e "${GREEN}✓ Quarkus Health: Accessible${NC}"
        echo -e "  Overall Status: ${GREEN}$STATUS${NC}"
        echo -e "  Health Checks: $CHECKS active"

        # List all checks
        echo -e "\n  Service Status:"
        echo "$RESPONSE" | jq -r '.checks[] | "  - \(.name): \(.status)"' 2>/dev/null || true
    else
        echo -e "${GREEN}✓ Quarkus Health: Accessible${NC}"
    fi
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ Quarkus Health: Not accessible${NC}"
    ((TESTS_FAILED++))
fi

# Test 3: System Info
echo -e "\n${BLUE}[3/6] Testing System Info Endpoint...${NC}"
if RESPONSE=$(curl -ks https://localhost:9443/api/v11/info 2>/dev/null); then
    if [ "$HAS_JQ" = true ]; then
        PLATFORM=$(echo "$RESPONSE" | jq -r '.platform.name')
        VERSION=$(echo "$RESPONSE" | jq -r '.platform.version')
        JAVA=$(echo "$RESPONSE" | jq -r '.runtime.java_version')
        QUARKUS=$(echo "$RESPONSE" | jq -r '.runtime.quarkus_version')

        echo -e "${GREEN}✓ System Info: Accessible${NC}"
        echo -e "  Platform: $PLATFORM $VERSION"
        echo -e "  Java: $JAVA"
        echo -e "  Quarkus: $QUARKUS"
    else
        echo -e "${GREEN}✓ System Info: Accessible${NC}"
    fi
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ System Info: Not accessible${NC}"
    ((TESTS_FAILED++))
fi

# Test 4: Transaction Stats
echo -e "\n${BLUE}[4/6] Testing Transaction Statistics...${NC}"
if RESPONSE=$(curl -ks https://localhost:9443/api/v11/stats 2>/dev/null); then
    if [ "$HAS_JQ" = true ]; then
        TOTAL=$(echo "$RESPONSE" | jq -r '.totalProcessed')
        STORED=$(echo "$RESPONSE" | jq -r '.storedTransactions')
        CONSENSUS=$(echo "$RESPONSE" | jq -r '.consensusAlgorithm')
        TPS=$(echo "$RESPONSE" | jq -r '.currentThroughputMeasurement')

        echo -e "${GREEN}✓ Transaction Stats: Accessible${NC}"
        echo -e "  Total Processed: $TOTAL"
        echo -e "  Stored: $STORED"
        echo -e "  Consensus: $CONSENSUS"
        echo -e "  Current TPS: $TPS"
    else
        echo -e "${GREEN}✓ Transaction Stats: Accessible${NC}"
    fi
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ Transaction Stats: Not accessible${NC}"
    ((TESTS_FAILED++))
fi

# Test 5: gRPC Port
echo -e "\n${BLUE}[5/6] Testing gRPC Server (port 9004)...${NC}"
if nc -z localhost 9004 2>/dev/null || timeout 1 bash -c "</dev/tcp/localhost/9004" 2>/dev/null; then
    echo -e "${GREEN}✓ gRPC Port: Open and listening${NC}"
    echo -e "  Port: 9004"
    echo -e "  Protocol: gRPC (separate server)"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ gRPC Port: Closed or not responding${NC}"
    echo -e "${YELLOW}  Note: gRPC may be disabled or port may be different${NC}"
    ((TESTS_FAILED++))
fi

# Test 6: Public API
echo -e "\n${BLUE}[6/6] Testing Public API (dlt.aurigraph.io)...${NC}"
if RESPONSE=$(curl -s --max-time 5 https://dlt.aurigraph.io/api/v11/health 2>/dev/null); then
    if [ "$HAS_JQ" = true ]; then
        STATUS=$(echo "$RESPONSE" | jq -r '.status')
        UPTIME=$(echo "$RESPONSE" | jq -r '.uptimeSeconds')

        echo -e "${GREEN}✓ Public API: Accessible${NC}"
        echo -e "  Status: ${GREEN}$STATUS${NC}"
        echo -e "  Uptime: ${UPTIME}s ($(($UPTIME / 3600))h $(($UPTIME % 3600 / 60))m)"
        echo -e "  URL: https://dlt.aurigraph.io/api/v11/"
    else
        echo -e "${GREEN}✓ Public API: Accessible${NC}"
    fi
    ((TESTS_PASSED++))
else
    echo -e "${YELLOW}⚠ Public API: Not accessible${NC}"
    echo -e "${YELLOW}  This is normal if not deployed to production server${NC}"
    echo -e "  Expected URL: https://dlt.aurigraph.io/api/v11/health"
    # Don't count as failure for local testing
fi

# Summary
echo -e "\n${BLUE}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Health Check Summary                              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}\n"

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
SUCCESS_RATE=$((TESTS_PASSED * 100 / TOTAL_TESTS))

echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo -e "Success Rate: ${GREEN}$SUCCESS_RATE%${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✓ All critical services are healthy!${NC}"
    exit 0
else
    echo -e "\n${RED}✗ Some services are not healthy. Please review the errors above.${NC}"
    exit 1
fi
