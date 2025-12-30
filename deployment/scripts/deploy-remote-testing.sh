#!/bin/bash
#
# Remote Testing Deployment Script
# Deploys both backend (Java/Quarkus) and frontend (React/Enterprise Portal)
# to dlt.aurigraph.io for testing
#
# Usage: ./deploy-remote-testing.sh
#

set -e

# Configuration
REMOTE_USER="subbu"
REMOTE_HOST="dlt.aurigraph.io"
REMOTE_PORT="2235"
REMOTE_APP_DIR="/opt/aurigraph/v11"
REMOTE_WWW_DIR="/var/www/dlt.aurigraph.io"

# Local paths
BACKEND_JAR_PATH="./target/aurigraph-v11-standalone-11.4.4-runner.jar"
FRONTEND_BUILD_DIR="./enterprise-portal/dist"

echo "═══════════════════════════════════════════════════════════════════════════════"
echo "Aurigraph V11 Remote Testing Deployment"
echo "═══════════════════════════════════════════════════════════════════════════════"
echo ""

# Step 1: Verify artifacts exist
echo "[1/6] Verifying build artifacts..."
if [ ! -f "$BACKEND_JAR_PATH" ]; then
    echo "ERROR: Backend JAR not found: $BACKEND_JAR_PATH"
    exit 1
fi
BACKEND_SIZE=$(du -h "$BACKEND_JAR_PATH" | cut -f1)
echo "✓ Backend JAR: $BACKEND_JAR_PATH ($BACKEND_SIZE)"

if [ ! -d "$FRONTEND_BUILD_DIR" ]; then
    echo "ERROR: Frontend build directory not found: $FRONTEND_BUILD_DIR"
    exit 1
fi
echo "✓ Frontend build directory: $FRONTEND_BUILD_DIR"
echo ""

# Step 2: Test remote connectivity
echo "[2/6] Testing remote server connectivity..."
if ! ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST "echo 'Connected'" > /dev/null 2>&1; then
    echo "ERROR: Cannot connect to remote server at $REMOTE_HOST:$REMOTE_PORT"
    echo ""
    echo "TROUBLESHOOTING:"
    echo "1. Check if server is online: ping $REMOTE_HOST"
    echo "2. Check SSH accessibility: ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST"
    echo "3. Verify credentials in ~/.ssh/config or use SSH key"
    exit 1
fi
echo "✓ Connected to $REMOTE_HOST:$REMOTE_PORT"
echo ""

# Step 3: Backup current backend
echo "[3/6] Backing up current backend deployment..."
ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST << 'REMOTE_CMD'
mkdir -p /opt/aurigraph/v11/backups
if [ -f /opt/aurigraph/v11/aurigraph-v11-standalone-11.4.4-runner.jar ]; then
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    cp /opt/aurigraph/v11/aurigraph-v11-standalone-11.4.4-runner.jar \
       /opt/aurigraph/v11/backups/aurigraph-v11-standalone-11.4.4-runner_${TIMESTAMP}.jar
    echo "✓ Backend backup created"
else
    echo "ℹ No previous backend deployment found"
fi
REMOTE_CMD
echo ""

# Step 4: Deploy backend JAR
echo "[4/6] Uploading backend JAR to production server..."
scp -P $REMOTE_PORT "$BACKEND_JAR_PATH" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_APP_DIR/"
echo "✓ Backend JAR uploaded successfully"
echo ""

# Step 5: Deploy frontend build
echo "[5/6] Uploading frontend build to web server..."
ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST "mkdir -p $REMOTE_WWW_DIR"
scp -P $REMOTE_PORT -r "$FRONTEND_BUILD_DIR"/* "$REMOTE_USER@$REMOTE_HOST:$REMOTE_WWW_DIR/"
echo "✓ Frontend build uploaded successfully"
echo ""

# Step 6: Restart services
echo "[6/6] Restarting services..."
ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST << 'REMOTE_CMD'
echo "Restarting backend service..."
sudo systemctl restart aurigraph-v11
sleep 3
if sudo systemctl is-active --quiet aurigraph-v11; then
    echo "✓ Backend service started successfully"
else
    echo "✗ Backend service failed to start"
    exit 1
fi

echo ""
echo "Restarting NGINX..."
sudo systemctl restart nginx
sleep 2
if sudo systemctl is-active --quiet nginx; then
    echo "✓ NGINX restarted successfully"
else
    echo "✗ NGINX failed to restart"
    exit 1
fi
REMOTE_CMD
echo ""

echo "═══════════════════════════════════════════════════════════════════════════════"
echo "✅ Deployment Complete!"
echo "═══════════════════════════════════════════════════════════════════════════════"
echo ""
echo "TESTING URLS:"
echo "─────────────"
echo "Frontend: https://dlt.aurigraph.io"
echo "Backend Health: https://dlt.aurigraph.io/api/v11/health"
echo "Backend Info: https://dlt.aurigraph.io/api/v11/info"
echo ""
echo "LOGIN CREDENTIALS (Demo):"
echo "────────────────────────"
echo "Username: admin"
echo "Password: admin"
echo ""
echo "NEXT STEPS:"
echo "──────────"
echo "1. Open https://dlt.aurigraph.io in browser"
echo "2. Login with admin/admin"
echo "3. Test dashboard and features"
echo "4. Check browser console for any errors"
echo "5. Verify API responses in browser DevTools Network tab"
echo ""
