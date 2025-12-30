#!/bin/bash
################################################################################
# Aurigraph V11 - Graceful Shutdown Script
################################################################################

set -e

# Configuration
PID_FILE="/var/run/aurigraph/aurigraph-v11.pid"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Stopping Aurigraph V11...${NC}"

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo -e "${RED}ERROR: PID file not found: $PID_FILE${NC}"
    echo "Aurigraph V11 may not be running, or was started differently"
    exit 1
fi

# Read PID
PID=$(cat "$PID_FILE")

# Check if process is running
if ! ps -p "$PID" > /dev/null 2>&1; then
    echo -e "${YELLOW}WARNING: Process $PID is not running${NC}"
    echo "Removing stale PID file..."
    rm -f "$PID_FILE"
    exit 0
fi

# Graceful shutdown (SIGTERM)
echo "Sending SIGTERM to process $PID..."
kill -15 "$PID"

# Wait for graceful shutdown (max 30 seconds)
echo "Waiting for graceful shutdown..."
for i in {1..30}; do
    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Aurigraph V11 stopped gracefully${NC}"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
    echo -n "."
done

echo ""
echo -e "${YELLOW}WARNING: Graceful shutdown timeout${NC}"
echo "Forcing shutdown with SIGKILL..."
kill -9 "$PID"
sleep 2

if ! ps -p "$PID" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Aurigraph V11 stopped (forced)${NC}"
    rm -f "$PID_FILE"
    exit 0
else
    echo -e "${RED}✗ Failed to stop Aurigraph V11${NC}"
    exit 1
fi
