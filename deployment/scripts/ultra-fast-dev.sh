#!/bin/bash
# ============================================================================
# ULTRA-FAST DEV LAUNCHER - 60-90 Second Startup
# November 4, 2025
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Show progress bar
show_progress() {
  local duration=$1
  local message=$2
  echo -ne "${BLUE}${message}${NC}"

  for i in $(seq 1 $duration); do
    echo -ne "▓"
    sleep 1
  done
  echo -e "${GREEN} ✅${NC}"
}

cd "$(dirname "$0")"

echo "=========================================="
echo "🚀 AURIGRAPH V11 - ULTRA-FAST DEV MODE"
echo "=========================================="
echo ""
echo "📊 Expected startup time: 60-90 seconds"
echo "📊 Features enabled: REST API, Database, Health checks"
echo "📊 Features disabled: Flyway, gRPC, Prometheus, OpenAPI"
echo ""

# Step 1: Kill existing processes
echo -ne "${YELLOW}[1/5]${NC} Stopping existing processes..."
pkill -9 java 2>/dev/null || true
sleep 2
echo -e "${GREEN} ✅${NC}"

# Step 2: Clean target directory
echo -ne "${YELLOW}[2/5]${NC} Cleaning build directory..."
rm -rf target/classes 2>/dev/null || true
echo -e "${GREEN} ✅${NC}"

# Step 3: Compile
echo -ne "${YELLOW}[3/5]${NC} Compiling (30-45 seconds)..."
echo ""
./mvnw clean compile \
  -q \
  -DskipTests \
  -Dquarkus.package.type=jar \
  -Dmaven.compiler.fork=true \
  -Dmaven.compiler.maxmem=2048m
echo -e "${BLUE}[3/5]${NC} ${GREEN}Compilation complete ✅${NC}"
echo ""

# Step 4: Start Quarkus
echo -ne "${YELLOW}[4/5]${NC} Starting Quarkus (30-45 seconds)..."
echo ""
./mvnw quarkus:dev \
  -Dquarkus.profile=dev-ultra \
  -Dquarkus.flyway.migrate-at-start=false \
  -Dquarkus.grpc.server.enabled=false \
  -Dquarkus.log.level=WARN \
  -Dquarkus.micrometer.export.prometheus.enabled=false \
  -DskipTests &

BACKEND_PID=$!
echo -e "${BLUE}[4/5]${NC} Starting backend (PID: $BACKEND_PID)${NC}"

# Step 5: Wait for health check
echo -ne "${YELLOW}[5/5]${NC} Waiting for health check..."
START_TIME=$(date +%s)
TIMEOUT=120  # 2 minute timeout

while true; do
  ELAPSED=$(($(date +%s) - START_TIME))

  if [ $ELAPSED -gt $TIMEOUT ]; then
    echo -e "${RED} ❌ TIMEOUT${NC}"
    echo "Backend did not come online in 2 minutes."
    echo "Check logs: tail -50 logs/application.log"
    kill $BACKEND_PID 2>/dev/null || true
    exit 1
  fi

  RESPONSE=$(curl -s -w "\n%{http_code}" http://localhost:9003/q/health 2>/dev/null || echo "")
  STATUS=$(echo "$RESPONSE" | tail -1)

  if [ "$STATUS" = "200" ]; then
    echo -e "${GREEN} ✅${NC}"
    echo ""
    echo "=========================================="
    echo "✅ AURIGRAPH V11 READY"
    echo "=========================================="
    echo ""
    echo "📍 Server: http://localhost:9003"
    echo "📍 Health: http://localhost:9003/q/health"
    echo "📍 API: http://localhost:9003/api/v11/"
    echo ""
    echo "⏱️  Startup time: ${ELAPSED}s"
    echo ""
    echo "💡 Next steps:"
    echo "   1. Run tests: npm run test:run -- sprint-14-backend-integration.test.ts"
    echo "   2. View logs: tail -f logs/application.log"
    echo "   3. Stop server: Ctrl+C"
    echo ""
    echo "=========================================="
    echo ""
    break
  fi

  PERCENT=$((ELAPSED * 100 / TIMEOUT))
  echo -ne "\r${YELLOW}[5/5]${NC} Waiting for health check... ${PERCENT}% (${ELAPSED}s)${NC}"
  sleep 1
done

# Keep the script running (let the backend process continue)
wait $BACKEND_PID
