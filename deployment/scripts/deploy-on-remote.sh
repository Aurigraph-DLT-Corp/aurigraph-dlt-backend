#!/bin/bash
# On-server deployment script
# Run this on the remote server after extracting the package

set -e

PACKAGE_DIR="${1:-.}"
cd "$PACKAGE_DIR"

echo "═══════════════════════════════════════════════════════════════════════════════"
echo "On-Server Deployment Script"
echo "═══════════════════════════════════════════════════════════════════════════════"
echo ""

# Verify artifacts
echo "[1/5] Verifying artifacts..."
if [ ! -f "target/aurigraph-v11-standalone-11.4.4-runner.jar" ]; then
    echo "ERROR: Backend JAR not found"
    exit 1
fi
if [ ! -d "enterprise-portal/dist" ]; then
    echo "ERROR: Frontend build not found"
    exit 1
fi
echo "✓ Artifacts verified"
echo ""

# Create backups
echo "[2/5] Creating backups..."
mkdir -p /opt/aurigraph/v11/backups
if [ -f /opt/aurigraph/v11/aurigraph-v11-standalone-11.4.4-runner.jar ]; then
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    cp /opt/aurigraph/v11/aurigraph-v11-standalone-11.4.4-runner.jar \
       /opt/aurigraph/v11/backups/aurigraph-v11-standalone-11.4.4-runner_${TIMESTAMP}.jar
    echo "✓ Backend backup created"
fi

if [ -d /var/www/dlt.aurigraph.io ]; then
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    cp -r /var/www/dlt.aurigraph.io /var/www/dlt.aurigraph.io.bak_${TIMESTAMP}
    echo "✓ Frontend backup created"
fi
echo ""

# Deploy backend
echo "[3/5] Deploying backend..."
cp target/aurigraph-v11-standalone-11.4.4-runner.jar /opt/aurigraph/v11/
echo "✓ Backend deployed"
echo ""

# Deploy frontend
echo "[4/5] Deploying frontend..."
mkdir -p /var/www/dlt.aurigraph.io
cp -r enterprise-portal/dist/* /var/www/dlt.aurigraph.io/
echo "✓ Frontend deployed"
echo ""

# Restart services
echo "[5/5] Restarting services..."
echo "Restarting backend service..."
sudo systemctl restart aurigraph-v11
sleep 3
if sudo systemctl is-active --quiet aurigraph-v11; then
    echo "✓ Backend service started"
else
    echo "✗ Backend service failed"
    exit 1
fi

echo "Restarting NGINX..."
sudo systemctl restart nginx
sleep 2
if sudo systemctl is-active --quiet nginx; then
    echo "✓ NGINX restarted"
else
    echo "✗ NGINX failed"
    exit 1
fi
echo ""

echo "═══════════════════════════════════════════════════════════════════════════════"
echo "✅ On-Server Deployment Complete!"
echo "═══════════════════════════════════════════════════════════════════════════════"
echo ""
echo "Access Portal: https://dlt.aurigraph.io"
echo "Login: admin/admin"
