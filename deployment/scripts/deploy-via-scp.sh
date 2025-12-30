#!/bin/bash
#
# SSH-based Deployment via SCP
# Transfers artifacts to remote server without requiring inbound connectivity
# Can be executed from the remote server via SSH tunnel or jumphost
#
# Usage: ./deploy-via-scp.sh
#

set -e

# Configuration
REMOTE_USER="${REMOTE_USER:-subbu}"
REMOTE_HOST="${REMOTE_HOST:-dlt.aurigraph.io}"
REMOTE_PORT="${REMOTE_PORT:-2235}"
REMOTE_APP_DIR="/opt/aurigraph/v11"
REMOTE_WWW_DIR="/var/www/dlt.aurigraph.io"

# Local paths
BACKEND_JAR="./target/aurigraph-v11-standalone-11.4.4-runner.jar"
FRONTEND_DIR="./enterprise-portal/dist"

echo "═══════════════════════════════════════════════════════════════════════════════"
echo "Aurigraph V11 - SSH/SCP Based Deployment"
echo "═══════════════════════════════════════════════════════════════════════════════"
echo ""

# Step 1: Create tarball of artifacts for transfer
echo "[1/4] Packaging artifacts for transfer..."
PACKAGE_FILE="aurigraph-v11-deployment-$(date +%Y%m%d_%H%M%S).tar.gz"
tar -czf "$PACKAGE_FILE" \
    "$BACKEND_JAR" \
    "$FRONTEND_DIR" \
    -C . deploy-remote-testing.sh \
    -C . REMOTE-TESTING-GUIDE.md 2>/dev/null

PACKAGE_SIZE=$(du -h "$PACKAGE_FILE" | cut -f1)
echo "✓ Package created: $PACKAGE_FILE ($PACKAGE_SIZE)"
echo ""

# Step 2: Display deployment instructions
echo "[2/4] Deployment package ready"
echo ""
echo "To deploy to remote server:"
echo "───────────────────────────"
echo ""
echo "Option A: Direct transfer (if you have SSH access from another machine)"
echo "  scp -P $REMOTE_PORT $PACKAGE_FILE $REMOTE_USER@$REMOTE_HOST:/tmp/"
echo "  ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'cd /tmp && tar -xzf $PACKAGE_FILE'"
echo ""
echo "Option B: Using jumphost/bastion (if direct access blocked)"
echo "  scp -P $REMOTE_PORT -o ProxyCommand='ssh -p 2222 jumphost.example.com nc %h %p' \\"
echo "    $PACKAGE_FILE $REMOTE_USER@$REMOTE_HOST:/tmp/"
echo ""
echo "Option C: Via SSH tunnel (if using tunneling)"
echo "  ssh -p $REMOTE_PORT -L 2235:$REMOTE_HOST:22 $REMOTE_USER@bastion &"
echo "  scp -P 2235 $PACKAGE_FILE localhost:/tmp/"
echo ""

# Step 3: Create on-server deployment script
echo "[3/4] Creating on-server deployment script..."
cat > deploy-on-remote.sh << 'REMOTE_DEPLOY'
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
REMOTE_DEPLOY

chmod +x deploy-on-remote.sh
echo "✓ On-server script created: deploy-on-remote.sh"
echo ""

# Step 4: Summary
echo "[4/4] Deployment preparation complete"
echo ""
echo "═══════════════════════════════════════════════════════════════════════════════"
echo "DEPLOYMENT PACKAGE READY"
echo "═══════════════════════════════════════════════════════════════════════════════"
echo ""
echo "Package: $PACKAGE_FILE ($PACKAGE_SIZE)"
echo "Contents:"
echo "  • Backend JAR (176 MB)"
echo "  • Frontend build (production)"
echo "  • Deployment scripts"
echo "  • Documentation"
echo ""
echo "Next Steps:"
echo "──────────"
echo "1. Transfer package to remote server via SCP"
echo "2. SSH to remote server"
echo "3. Extract package: tar -xzf $PACKAGE_FILE"
echo "4. Run: ./deploy-on-remote.sh"
echo ""
echo "Example (if direct SSH available):"
echo "  scp -P 2235 $PACKAGE_FILE subbu@dlt.aurigraph.io:/tmp/"
echo "  ssh -p 2235 subbu@dlt.aurigraph.io"
echo "  cd /tmp && tar -xzf $PACKAGE_FILE"
echo "  ./deploy-on-remote.sh"
echo ""
echo "═══════════════════════════════════════════════════════════════════════════════"
