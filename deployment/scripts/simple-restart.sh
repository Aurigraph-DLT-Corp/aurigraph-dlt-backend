#!/bin/bash

# Simple restart script using sshpass
set -e

echo "=== Restarting Aurigraph V11 on Remote Server ==="
echo ""

# Install sshpass if not available (macOS)
if ! command -v sshpass &> /dev/null; then
    echo "Installing sshpass..."
    brew install sshpass 2>/dev/null || echo "Please install sshpass manually"
fi

# Remote commands
REMOTE_CMD="cd ~/aurigraph-v11 && \
pkill -9 -f 'aurigraph-v11-standalone.*jar' || echo 'No old processes' && \
sleep 2 && \
./start-v11.sh && \
sleep 15 && \
if ps aux | grep -v grep | grep aurigraph-v11-standalone > /dev/null; then \
    echo '✅ Service is running' && \
    curl -s http://localhost:9003/q/health > /dev/null && echo '✅ Health endpoint OK' || echo '⚠️ Health endpoint not responding' && \
    ps aux | grep -v grep | grep aurigraph-v11-standalone; \
else \
    echo '❌ Service failed to start - logs:' && \
    tail -50 logs/console.log; \
fi"

# Execute via SSH
sshpass -p 'subbuFuture@2025' ssh -p 22 -o StrictHostKeyChecking=no subbu@dlt.aurigraph.io "$REMOTE_CMD"

echo ""
echo "=== Done ==="
