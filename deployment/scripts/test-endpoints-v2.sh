#!/bin/bash

# Sprint 19: Endpoint Testing Script (v2 - macOS compatible)
# Tests authentication and demo API endpoints
# Usage: ./test-endpoints-v2.sh [base_url] [username] [password]

# Configuration
BASE_URL="${1:-http://localhost:9003}"
USERNAME="${2:-testuser}"
PASSWORD="${3:-Test@12345}"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "═════════════════════════════════════════════════════════════════════════════"
echo "                   SPRINT 19: ENDPOINT VALIDATION TEST SUITE (v2)"
echo "═════════════════════════════════════════════════════════════════════════════"
echo "Base URL:  $BASE_URL"
echo "Username:  $USERNAME"
echo "Timestamp: $(date)"
echo ""

# Test counter
PASSED=0
FAILED=0

# ============================================================================
# TEST 1: AUTHENTICATION
# ============================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "TEST 1: AUTHENTICATION SERVICE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Test 1.1: Successful Authentication
echo -n "[POST] /api/v11/users/authenticate (valid credentials) ... "
AUTH_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v11/users/authenticate" \
  -H "Content-Type: application/json" \
  --data "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

JWT_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.token // empty' 2>/dev/null)
USER_ID=$(echo "$AUTH_RESPONSE" | jq -r '.user.id // empty' 2>/dev/null)

if [ ! -z "$JWT_TOKEN" ] && [ ! -z "$USER_ID" ]; then
  echo -e "${GREEN}✅ PASSED${NC}"
  echo "  JWT Token: ${JWT_TOKEN:0:50}..."
  echo "  User ID: $USER_ID"
  ((PASSED++))
else
  echo -e "${RED}❌ FAILED${NC}"
  echo "  Response: $AUTH_RESPONSE"
  ((FAILED++))
fi
echo ""

# Test 1.2: Invalid Credentials
echo -n "[POST] /api/v11/users/authenticate (invalid password) ... "
INVALID_AUTH=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v11/users/authenticate" \
  -H "Content-Type: application/json" \
  --data "{\"username\":\"$USERNAME\",\"password\":\"wrongpassword\"}")

invalid_code=$(echo "$INVALID_AUTH" | tail -1)

if [ "$invalid_code" = "401" ]; then
  echo -e "${GREEN}✅ PASSED${NC} (HTTP 401 Unauthorized)"
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
  --data "{\"demoName\":\"Test Demo $(date +%s)\",\"userName\":\"$USERNAME\",\"userEmail\":\"test@aurigraph.io\",\"description\":\"Automated test\"}")

create_code=$(echo "$CREATE_RESPONSE" | tail -1)
create_body=$(echo "$CREATE_RESPONSE" | sed '$d')
DEMO_ID=$(echo "$create_body" | jq -r '.id // empty' 2>/dev/null)

if [ "$create_code" = "201" ] && [ ! -z "$DEMO_ID" ]; then
  echo -e "${GREEN}✅ PASSED${NC} (HTTP 201 Created)"
  echo "  Demo ID: $DEMO_ID"
  ((PASSED++))
else
  echo -e "${RED}❌ FAILED${NC} (Expected HTTP 201, got $create_code)"
  echo "  Response: $create_body"
  ((FAILED++))
fi
echo ""

# Test 2.2: List Demos
echo -n "[GET] /api/v11/demos (list) ... "
LIST_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/v11/demos" \
  -H "Authorization: Bearer $JWT_TOKEN")

list_code=$(echo "$LIST_RESPONSE" | tail -1)
list_body=$(echo "$LIST_RESPONSE" | sed '$d')

if [ "$list_code" = "200" ]; then
  demo_count=$(echo "$list_body" | jq 'length' 2>/dev/null || echo "?")
  echo -e "${GREEN}✅ PASSED${NC} (HTTP 200)"
  echo "  Demos found: $demo_count"
  ((PASSED++))
else
  echo -e "${RED}❌ FAILED${NC} (Expected HTTP 200, got $list_code)"
  ((FAILED++))
fi
echo ""

# Test 2.3: Get Demo Details
if [ ! -z "$DEMO_ID" ]; then
  echo -n "[GET] /api/v11/demos/{id} (details) ... "
  GET_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/v11/demos/$DEMO_ID" \
    -H "Authorization: Bearer $JWT_TOKEN")

  get_code=$(echo "$GET_RESPONSE" | tail -1)
  get_body=$(echo "$GET_RESPONSE" | sed '$d')

  if [ "$get_code" = "200" ]; then
    demo_status=$(echo "$get_body" | jq -r '.status // "unknown"' 2>/dev/null)
    echo -e "${GREEN}✅ PASSED${NC} (HTTP 200)"
    echo "  Demo Status: $demo_status"
    ((PASSED++))
  else
    echo -e "${RED}❌ FAILED${NC} (Expected HTTP 200, got $get_code)"
    ((FAILED++))
  fi
  echo ""

  # Test 2.4: Start Demo
  echo -n "[POST] /api/v11/demos/{id}/start ... "
  START_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v11/demos/$DEMO_ID/start" \
    -H "Authorization: Bearer $JWT_TOKEN")

  start_code=$(echo "$START_RESPONSE" | tail -1)
  start_body=$(echo "$START_RESPONSE" | sed '$d')

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

  stop_code=$(echo "$STOP_RESPONSE" | tail -1)
  stop_body=$(echo "$STOP_RESPONSE" | sed '$d')

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

  delete_code=$(echo "$DELETE_RESPONSE" | tail -1)

  if [ "$delete_code" = "204" ]; then
    echo -e "${GREEN}✅ PASSED${NC} (HTTP 204 No Content)"
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
