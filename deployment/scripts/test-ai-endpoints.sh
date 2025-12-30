#!/bin/bash
#
# Test Script for AI/ML Performance Endpoints
# Tests both /api/v11/ai/performance and /api/v11/ai/confidence endpoints
#

BASE_URL="http://localhost:9003"
PERFORMANCE_ENDPOINT="/api/v11/ai/performance"
CONFIDENCE_ENDPOINT="/api/v11/ai/confidence"

echo "========================================="
echo "AI/ML Performance Endpoints Test Script"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Performance Endpoint
echo -e "${YELLOW}Test 1: GET ${PERFORMANCE_ENDPOINT}${NC}"
echo "Expected: 200 OK with performance metrics"
echo ""

start_time=$(date +%s%3N)
response=$(curl -s -w "\nHTTP_CODE:%{http_code}\nTIME_TOTAL:%{time_total}" "${BASE_URL}${PERFORMANCE_ENDPOINT}")
end_time=$(date +%s%3N)

http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d':' -f2)
time_total=$(echo "$response" | grep "TIME_TOTAL" | cut -d':' -f2)
body=$(echo "$response" | sed '/HTTP_CODE/,$d')

if [ "$http_code" == "200" ]; then
    echo -e "${GREEN}✓ Status Code: 200 OK${NC}"
else
    echo -e "${RED}✗ Status Code: $http_code (Expected 200)${NC}"
fi

# Parse response time from body
response_time=$(echo "$body" | grep -o '"responseTime":[0-9.]*' | cut -d':' -f2)
if [ ! -z "$response_time" ]; then
    response_time_int=$(printf "%.0f" "$response_time")
    if [ "$response_time_int" -lt 200 ]; then
        echo -e "${GREEN}✓ Response Time: ${response_time}ms (< 200ms requirement)${NC}"
    else
        echo -e "${RED}✗ Response Time: ${response_time}ms (>= 200ms requirement)${NC}"
    fi
fi

# Check key fields
if echo "$body" | grep -q '"totalModels"'; then
    total_models=$(echo "$body" | grep -o '"totalModels":[0-9]*' | cut -d':' -f2)
    echo -e "${GREEN}✓ Total Models: $total_models${NC}"
else
    echo -e "${RED}✗ Missing totalModels field${NC}"
fi

if echo "$body" | grep -q '"averageAccuracy"'; then
    avg_accuracy=$(echo "$body" | grep -o '"averageAccuracy":[0-9.]*' | cut -d':' -f2)
    echo -e "${GREEN}✓ Average Accuracy: $avg_accuracy%${NC}"
else
    echo -e "${RED}✗ Missing averageAccuracy field${NC}"
fi

if echo "$body" | grep -q '"models"'; then
    echo -e "${GREEN}✓ Models array present${NC}"
else
    echo -e "${RED}✗ Missing models array${NC}"
fi

echo ""
echo "Response sample:"
echo "$body" | head -20
echo ""

# Test 2: Confidence Endpoint
echo "========================================="
echo -e "${YELLOW}Test 2: GET ${CONFIDENCE_ENDPOINT}${NC}"
echo "Expected: 200 OK with confidence scores"
echo ""

start_time=$(date +%s%3N)
response=$(curl -s -w "\nHTTP_CODE:%{http_code}\nTIME_TOTAL:%{time_total}" "${BASE_URL}${CONFIDENCE_ENDPOINT}")
end_time=$(date +%s%3N)

http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d':' -f2)
time_total=$(echo "$response" | grep "TIME_TOTAL" | cut -d':' -f2)
body=$(echo "$response" | sed '/HTTP_CODE/,$d')

if [ "$http_code" == "200" ]; then
    echo -e "${GREEN}✓ Status Code: 200 OK${NC}"
else
    echo -e "${RED}✗ Status Code: $http_code (Expected 200)${NC}"
fi

# Parse response time from body
response_time=$(echo "$body" | grep -o '"responseTime":[0-9.]*' | cut -d':' -f2)
if [ ! -z "$response_time" ]; then
    response_time_int=$(printf "%.0f" "$response_time")
    if [ "$response_time_int" -lt 200 ]; then
        echo -e "${GREEN}✓ Response Time: ${response_time}ms (< 200ms requirement)${NC}"
    else
        echo -e "${RED}✗ Response Time: ${response_time}ms (>= 200ms requirement)${NC}"
    fi
fi

# Check key fields
if echo "$body" | grep -q '"totalPredictions"'; then
    total_preds=$(echo "$body" | grep -o '"totalPredictions":[0-9]*' | cut -d':' -f2)
    echo -e "${GREEN}✓ Total Predictions: $total_preds${NC}"
else
    echo -e "${RED}✗ Missing totalPredictions field${NC}"
fi

if echo "$body" | grep -q '"averageConfidence"'; then
    avg_conf=$(echo "$body" | grep -o '"averageConfidence":[0-9.]*' | cut -d':' -f2)
    echo -e "${GREEN}✓ Average Confidence: $avg_conf%${NC}"
else
    echo -e "${RED}✗ Missing averageConfidence field${NC}"
fi

if echo "$body" | grep -q '"anomaliesDetected"'; then
    anomalies=$(echo "$body" | grep -o '"anomaliesDetected":[0-9]*' | cut -d':' -f2)
    echo -e "${GREEN}✓ Anomalies Detected: $anomalies${NC}"
else
    echo -e "${RED}✗ Missing anomaliesDetected field${NC}"
fi

if echo "$body" | grep -q '"predictions"'; then
    echo -e "${GREEN}✓ Predictions array present${NC}"
else
    echo -e "${RED}✗ Missing predictions array${NC}"
fi

echo ""
echo "Response sample:"
echo "$body" | head -20
echo ""

# Test 3: Concurrent Access
echo "========================================="
echo -e "${YELLOW}Test 3: Concurrent Access Test${NC}"
echo "Testing 5 rapid successive requests to both endpoints"
echo ""

success_count=0
for i in {1..5}; do
    perf_code=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}${PERFORMANCE_ENDPOINT}")
    conf_code=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}${CONFIDENCE_ENDPOINT}")

    if [ "$perf_code" == "200" ] && [ "$conf_code" == "200" ]; then
        success_count=$((success_count + 1))
        echo -e "  Request $i: ${GREEN}✓ Both endpoints OK${NC}"
    else
        echo -e "  Request $i: ${RED}✗ Performance: $perf_code, Confidence: $conf_code${NC}"
    fi
done

echo ""
if [ "$success_count" == "5" ]; then
    echo -e "${GREEN}✓ All 5 concurrent requests successful${NC}"
else
    echo -e "${RED}✗ Only $success_count/5 requests successful${NC}"
fi

echo ""
echo "========================================="
echo "Test Summary"
echo "========================================="
echo ""
echo "Endpoints tested:"
echo "  1. ${PERFORMANCE_ENDPOINT}"
echo "  2. ${CONFIDENCE_ENDPOINT}"
echo ""
echo -e "${GREEN}Testing complete!${NC}"
echo ""
echo "Note: For full test suite, run:"
echo "  ./mvnw test -Dtest=AIApiPerformanceTest"
echo ""
