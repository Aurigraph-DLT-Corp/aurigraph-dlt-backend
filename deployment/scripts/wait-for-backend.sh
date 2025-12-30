#!/bin/bash
# ============================================================================
# BACKEND HEALTH CHECK MONITOR
# November 4, 2025
# ============================================================================

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
BACKEND_URL="${1:-http://localhost:9003}"
HEALTH_PATH="/q/health"
TIMEOUT="${2:-120}"
CHECK_INTERVAL=2

START_TIME=$(date +%s)
COUNTER=0

echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}🏥 BACKEND HEALTH CHECK MONITOR${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo ""
echo -e "📍 Target: ${BLUE}${BACKEND_URL}${NC}"
echo -e "⏱️  Timeout: ${TIMEOUT}s"
echo -e "🔄 Interval: ${CHECK_INTERVAL}s"
echo ""

while true; do
  ELAPSED=$(($(date +%s) - START_TIME))
  COUNTER=$((COUNTER + 1))

  # Check timeout
  if [ $ELAPSED -gt $TIMEOUT ]; then
    echo ""
    echo -e "${RED}❌ TIMEOUT: Backend did not respond in ${TIMEOUT}s${NC}"
    echo ""
    echo -e "${YELLOW}Troubleshooting:${NC}"
    echo "  1. Check if port 9003 is available: lsof -i :9003"
    echo "  2. Kill any hanging processes: pkill -9 java"
    echo "  3. Check logs: tail -100 logs/application.log"
    echo "  4. Verify database: psql -U aurigraph aurigraph_demos -c 'SELECT 1'"
    echo ""
    exit 1
  fi

  # Make health check request
  RESPONSE=$(curl -s -w "\n%{http_code}" "${BACKEND_URL}${HEALTH_PATH}" 2>/dev/null || echo "")
  HTTP_CODE=$(echo "$RESPONSE" | tail -1)
  BODY=$(echo "$RESPONSE" | sed '$d')

  # Progress bar
  PERCENT=$((ELAPSED * 100 / TIMEOUT))
  BAR_LENGTH=30
  FILLED=$((PERCENT * BAR_LENGTH / 100))
  BAR=""
  for ((i=0; i<FILLED; i++)); do BAR="${BAR}▓"; done
  for ((i=FILLED; i<BAR_LENGTH; i++)); do BAR="${BAR}░"; done

  if [ "$HTTP_CODE" = "200" ]; then
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ BACKEND ONLINE${NC}"
    echo -e "${GREEN}═══════════════════════════════════════${NC}"
    echo ""
    echo -e "${BLUE}📊 Response:${NC}"
    if command -v jq &> /dev/null; then
      echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    else
      echo "$BODY"
    fi
    echo ""
    echo -e "${YELLOW}⏱️  Total startup time: ${ELAPSED}s${NC}"
    echo -e "${YELLOW}🔄 Health checks performed: ${COUNTER}${NC}"
    echo ""
    exit 0
  fi

  # Print progress
  echo -ne "\r${BLUE}[${BAR}]${NC} ${PERCENT}% (${ELAPSED}/${TIMEOUT}s) Attempt #${COUNTER}"

  if [ "$HTTP_CODE" != "" ] && [ "$HTTP_CODE" != "000" ]; then
    echo -ne " (HTTP ${HTTP_CODE})"
  else
    echo -ne " (Waiting...)"
  fi

  sleep $CHECK_INTERVAL
done
