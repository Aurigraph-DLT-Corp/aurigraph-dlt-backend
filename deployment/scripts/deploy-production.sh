#!/bin/bash
#
# Deployment Script for Aurigraph V11 Portal v4.9.0
# Deploys built JAR to production server at dlt.aurigraph.io
#
# Usage: ./deploy-production.sh
#

set -e

# Configuration
REMOTE_USER="subbu"
REMOTE_HOST="dlt.aurigraph.io"
REMOTE_PORT="2235"
REMOTE_APP_DIR="/opt/aurigraph/v11"

JAR_NAME="aurigraph-v11-standalone-11.4.4-runner.jar"
JAR_PATH="./target/${JAR_NAME}"

echo "=========================================="
echo "Aurigraph V11 Portal v4.9.0 Deployment"
echo "=========================================="
echo ""

# Step 1: Verify JAR exists
echo "[1/5] Verifying build artifact..."
if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: JAR file not found: $JAR_PATH"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_PATH" | cut -f1)
echo "✓ Found JAR: $JAR_PATH ($JAR_SIZE)"
echo ""

# Step 2: Check remote server connectivity
echo "[2/5] Testing remote server connectivity..."
if ! ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST "echo 'Connected'" > /dev/null 2>&1; then
    echo "ERROR: Cannot connect to remote server"
    exit 1
fi
echo "✓ Connected to $REMOTE_HOST:$REMOTE_PORT"
echo ""

# Step 3: Create backup
echo "[3/5] Creating backup of current deployment..."
ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST << 'REMOTE_CMD'
mkdir -p /opt/aurigraph/v11/backups
if [ -f /opt/aurigraph/v11/aurigraph-v11-standalone-11.4.4-runner.jar ]; then
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    cp /opt/aurigraph/v11/aurigraph-v11-standalone-11.4.4-runner.jar \
       /opt/aurigraph/v11/backups/aurigraph-v11-standalone-11.4.4-runner_${TIMESTAMP}.jar
    echo "✓ Backup created"
else
    echo "ℹ No previous deployment found"
fi
REMOTE_CMD
echo ""

# Step 4: Deploy JAR
echo "[4/5] Uploading JAR to production server..."
scp -P $REMOTE_PORT "$JAR_PATH" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_APP_DIR/"
echo "✓ JAR uploaded successfully"
echo ""

# Step 5: Restart service
echo "[5/5] Restarting Aurigraph V11 service..."
ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST << 'REMOTE_CMD'
echo "Restarting service..."
sudo systemctl restart aurigraph-v11
sleep 3
if sudo systemctl is-active --quiet aurigraph-v11; then
    echo "✓ Service started successfully"
else
    echo "✗ Service failed to start"
    exit 1
fi
REMOTE_CMD
echo ""

echo "=========================================="
echo "✅ Deployment Complete!"
echo "=========================================="
