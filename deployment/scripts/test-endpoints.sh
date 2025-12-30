#!/bin/bash

# Sprint 19: Endpoint Testing Script
# Tests authentication and demo API endpoints
# Usage: ./test-endpoints.sh [base_url] [username] [password]

# Configuration
BASE_URL="${1:-http://localhost:9003}"
USERNAME="${2:-admin}"
PASSWORD="${3:-password}"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "═════════════════════════════════════════════════════════════════════════════"
echo "                   SPRINT 19: ENDPOINT VALIDATION TEST SUITE"
echo "═════════════════════════════════════════════════════════════════════════════"
echo "Base URL:  $BASE_URL"
echo "Username:  $USERNAME"
echo "Timestamp: $(date)"
echo ""

# Test counter
PASSED=0
FAILED=0

# Helper function
test_endpoint() {
  local name=$1
  local method=$2
  local path=$3
  local data=$4
  local headers=$5
  local expected_code=$6

  echo -n "[$method] $path ... "

  if [ -z "$data" ]; then
    response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$path" $headers)
  else
    response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$path" \
      -H "Content-Type: application/json" $headers \
      -d "$data")
  fi

  http_code=$(echo "$response" | tail -1)
  body=$(echo "$response" | sed '$d')

  if [ "$http_code" = "$expected_code" ]; then
    echo -e "${GREEN}✅ PASSED${NC} (HTTP $http_code)"
    ((PASSED++))
    echo "$body"
  else
    echo -e "${RED}❌ FAILED${NC} (Expected HTTP $expected_code, got $http_code)"
    echo "$body"
    ((FAILED++))
  fi
  echo ""
}

# ============================================================================
# TEST 1: AUTHENTICATION
# ============================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "TEST 1: AUTHENTICATION SERVICE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Test 1.1: Successful Authentication
echo -n "[POST] /api/v11/users/authenticate (valid credentials) ... "
AUTH_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v11/users/authenticate" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

http_code=$(echo "$AUTH_RESPONSE" | tail -n1)
body=$(echo "$AUTH_RESPONSE" | head -n-1)

JWT_TOKEN=$(echo "$body" | jq -r '.token // empty' 2>/dev/null)
USER_ID=$(echo "$body" | jq -r '.user.id // empty' 2>/dev/null)

if [ "$http_code" = "200" ] && [ ! -z "$JWT_TOKEN" ]; then
  echo -e "${GREEN}✅ PASSED${NC} (HTTP $http_code)"
  echo "  JWT Token: ${JWT_TOKEN:0:50}..."
  echo "  User ID: $USER_ID"
  ((PASSED++))
else
  echo -e "${RED}❌ FAILED${NC} (Expected 200 with token)"
  echo "  Response: $body"
  ((FAILED++))
  exit 1
fi
echo ""

# Test 1.2: Invalid Credentials
echo -n "[POST] /api/v11/users/authenticate (invalid password) ... "
INVALID_AUTH=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v11/users/authenticate" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"wrongpassword\"}")

invalid_code=$(echo "$INVALID_AUTH" | tail -n1)

if [ "$invalid_code" = "401" ]; then
  echo -e "${GREEN}✅ PASSED${NC} (HTTP 401)"
  ((PASSED++))
else
  echo -e "${YELLOW}⚠️  UNEXPECTED${NC} (HTTP $invalid_code)"
fi
echo ""

# ============================================================================
# TEST 2: DEMO API
# ============================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "TEST 2: DEMO API ENDPOINTS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Test 2.1: Create Demo
echo -n "[POST] /api/v11/demos (create) ... "
CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v11/demos" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d "{\"demoName\":\"Test Demo $(date +%s)\",\"userName\":\"$USERNAME\",\"userEmail\":\"test@aurigraph.io\",\"description\":\"Automated test\"}")

create_code=$(echo "$CREATE_RESPONSE" | tail -n1)
create_body=$(echo "$CREATE_RESPONSE" | head -n-1)
DEMO_ID=$(echo "$create_body" | jq -r '.id // empty' 2>/dev/null)

if [ "$create_code" = "201" ] && [ ! -z "$DEMO_ID" ]; then
  echo -e "${GREEN}✅ PASSED${NC} (HTTP 201)"
  echo "  Demo ID: $DEMO_ID"
  ((PASSED++))
else
  echo -e "${RED}❌ FAILED${NC} (Expected 201 Created)"
  echo "  Response: $create_body"
  ((FAILED++))
fi
echo ""

# Test 2.2: List Demos
echo -n "[GET] /api/v11/demos (list) ... "
LIST_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/v11/demos" \
  -H "Authorization: Bearer $JWT_TOKEN")

list_code=$(echo "$LIST_RESPONSE" | tail -n1)
list_body=$(echo "$LIST_RESPONSE" | head -n-1)

if [ "$list_code" = "200" ]; then
  demo_count=$(echo "$list_body" | jq 'length' 2>/dev/null || echo "?")
  echo -e "${GREEN}✅ PASSED${NC} (HTTP 200)"
  echo "  Demos found: $demo_count"
  ((PASSED++))
else
  echo -e "${RED}❌ FAILED${NC} (Expected 200)"
  echo "  Response: $list_body"
  ((FAILED++))
fi
echo ""

# Test 2.3: Get Demo Details
if [ ! -z "$DEMO_ID" ]; then
  echo -n "[GET] /api/v11/demos/{id} (details) ... "
  GET_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/v11/demos/$DEMO_ID" \
    -H "Authorization: Bearer $JWT_TOKEN")

  get_code=$(echo "$GET_RESPONSE" | tail -n1)
  get_body=$(echo "$GET_RESPONSE" | head -n-1)

  if [ "$get_code" = "200" ]; then
    demo_status=$(echo "$get_body" | jq -r '.status // "unknown"' 2>/dev/null)
    echo -e "${GREEN}✅ PASSED${NC} (HTTP 200)"
    echo "  Demo Status: $demo_status"
    ((PASSED++))
  else
    echo -e "${RED}❌ FAILED${NC} (Expected 200)"
    echo "  Response: $get_body"
    ((FAILED++))
  fi
  echo ""

  # Test 2.4: Start Demo
  echo -n "[POST] /api/v11/demos/{id}/start ... "
  START_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v11/demos/$DEMO_ID/start" \
    -H "Authorization: Bearer $JWT_TOKEN")

  start_code=$(echo "$START_RESPONSE" | tail -n1)
  start_body=$(echo "$START_RESPONSE" | head -n-1)

  if [ "$start_code" = "200" ]; then
    start_status=$(echo "$start_body" | jq -r '.status // "unknown"' 2>/dev/null)
    echo -e "${GREEN}✅ PASSED${NC} (HTTP 200)"
    echo "  Demo Status: $start_status"
    ((PASSED++))
  else
    echo -e "${YELLOW}⚠️  ${NC} (HTTP $start_code)"
    ((FAILED++))
  fi
  echo ""

  # Test 2.5: Stop Demo
  echo -n "[POST] /api/v11/demos/{id}/stop ... "
  STOP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v11/demos/$DEMO_ID/stop" \
    -H "Authorization: Bearer $JWT_TOKEN")

  stop_code=$(echo "$STOP_RESPONSE" | tail -n1)
  stop_body=$(echo "$STOP_RESPONSE" | head -n-1)

  if [ "$stop_code" = "200" ]; then
    stop_status=$(echo "$stop_body" | jq -r '.status // "unknown"' 2>/dev/null)
    duration=$(echo "$stop_body" | jq -r '.durationSeconds // "unknown"' 2>/dev/null)
    echo -e "${GREEN}✅ PASSED${NC} (HTTP 200)"
    echo "  Demo Status: $stop_status (Duration: ${duration}s)"
    ((PASSED++))
  else
    echo -e "${YELLOW}⚠️  ${NC} (HTTP $stop_code)"
    ((FAILED++))
  fi
  echo ""

  # Test 2.6: Delete Demo
  echo -n "[DELETE] /api/v11/demos/{id} ... "
  DELETE_RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/api/v11/demos/$DEMO_ID" \
    -H "Authorization: Bearer $JWT_TOKEN")

  delete_code=$(echo "$DELETE_RESPONSE" | tail -n1)

  if [ "$delete_code" = "204" ]; then
    echo -e "${GREEN}✅ PASSED${NC} (HTTP 204)"
    ((PASSED++))
  else
    echo -e "${YELLOW}⚠️  ${NC} (HTTP $delete_code)"
    ((FAILED++))
  fi
  echo ""
fi

# ============================================================================
# TEST SUMMARY
# ============================================================================
echo "═════════════════════════════════════════════════════════════════════════════"
echo "                            TEST SUMMARY"
echo "═════════════════════════════════════════════════════════════════════════════"
echo -e "${GREEN}✅ PASSED: $PASSED${NC}"
echo -e "${RED}❌ FAILED: $FAILED${NC}"
TOTAL=$((PASSED + FAILED))
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Total: $TOTAL tests"

if [ $FAILED -eq 0 ]; then
  echo -e "\n${GREEN}🎉 ALL TESTS PASSED!${NC}\n"
  exit 0
else
  echo -e "\n${RED}⚠️  Some tests failed${NC}\n"
  exit 1
fi
