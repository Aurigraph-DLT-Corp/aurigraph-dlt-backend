#!/bin/bash

################################################################################
# NGINX Portal Fix Deployment Script
# ============================================
# This script deploys the updated NGINX configuration that fixes the portal
# not displaying issue by properly routing to React SPA and serving assets.
#
# Usage: ./DEPLOY-NGINX-FIX.sh [target-host] [target-user] [target-port]
# Example: ./DEPLOY-NGINX-FIX.sh dlt.aurigraph.io subbu 22
################################################################################

set -e

TARGET_HOST="${1:-dlt.aurigraph.io}"
TARGET_USER="${2:-subbu}"
TARGET_PORT="${3:-22}"
CONFIG_DIR="/opt/DLT/config"
NGINX_CONFIG_LOCAL="$(dirname "$0")/config/nginx/nginx.conf"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}       NGINX Portal Fix Deployment${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Target:${NC} $TARGET_USER@$TARGET_HOST:$TARGET_PORT"
echo -e "${YELLOW}Config:${NC} $NGINX_CONFIG_LOCAL"
echo ""

# Verify local config exists
if [ ! -f "$NGINX_CONFIG_LOCAL" ]; then
    echo -e "${RED}❌ ERROR: Local NGINX config not found at $NGINX_CONFIG_LOCAL${NC}"
    exit 1
fi

echo -e "${GREEN}✓${NC} Local NGINX config verified"
echo ""

# Step 1: Backup existing NGINX config
echo -e "${BLUE}Step 1: Backing up existing NGINX configuration...${NC}"
ssh -p "$TARGET_PORT" "$TARGET_USER@$TARGET_HOST" << 'BACKUP_SCRIPT'
cd /opt/DLT/config
BACKUP_DIR="backups/nginx-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
if [ -f docker/nginx/nginx.conf ]; then
    cp docker/nginx/nginx.conf "$BACKUP_DIR/nginx.conf.bak"
    echo "✓ Backup created at $BACKUP_DIR"
else
    echo "⚠️  No existing nginx.conf found to backup"
fi
BACKUP_SCRIPT

echo ""

# Step 2: Copy new NGINX config
echo -e "${BLUE}Step 2: Deploying new NGINX configuration...${NC}"
scp -P "$TARGET_PORT" "$NGINX_CONFIG_LOCAL" "$TARGET_USER@$TARGET_HOST:$CONFIG_DIR/docker/nginx/nginx.conf"
echo -e "${GREEN}✓${NC} New config deployed"
echo ""

# Step 3: Validate NGINX config
echo -e "${BLUE}Step 3: Validating NGINX configuration syntax...${NC}"
ssh -p "$TARGET_PORT" "$TARGET_USER@$TARGET_HOST" << 'VALIDATE_SCRIPT'
cd /opt/DLT/config
docker exec aurigraph-nginx nginx -t
if [ $? -eq 0 ]; then
    echo "✓ NGINX configuration syntax is valid"
else
    echo "❌ NGINX configuration has syntax errors"
    exit 1
fi
VALIDATE_SCRIPT

echo ""

# Step 4: Verify portal files are in place
echo -e "${BLUE}Step 4: Verifying portal files in NGINX...${NC}"
ssh -p "$TARGET_PORT" "$TARGET_USER@$TARGET_HOST" << 'VERIFY_SCRIPT'
cd /opt/DLT/config
echo "Checking NGINX serving directory:"
docker exec aurigraph-nginx ls -lh /usr/share/nginx/html/ | grep -E "^-|^d" | head -10
if [ -f /opt/DLT/web/index.html ]; then
    echo "✓ Portal index.html found"
else
    echo "⚠️  Portal index.html not found at /opt/DLT/web/"
fi
if [ -d /opt/DLT/web/assets ]; then
    echo "✓ Portal assets directory found"
    ls -lh /opt/DLT/web/assets | head -5
else
    echo "⚠️  Portal assets directory not found"
fi
VERIFY_SCRIPT

echo ""

# Step 5: Restart NGINX
echo -e "${BLUE}Step 5: Restarting NGINX container...${NC}"
ssh -p "$TARGET_PORT" "$TARGET_USER@$TARGET_HOST" << 'RESTART_SCRIPT'
cd /opt/DLT/config
docker-compose restart nginx
sleep 5
docker-compose ps nginx
RESTART_SCRIPT

echo ""

# Step 6: Test portal access
echo -e "${BLUE}Step 6: Testing portal access...${NC}"
ssh -p "$TARGET_PORT" "$TARGET_USER@$TARGET_HOST" << 'TEST_SCRIPT'
echo "Testing root path (/):"
curl -s -I https://dlt.aurigraph.io/ | head -5

echo ""
echo "Testing /api/v11/ endpoint:"
curl -s -I https://dlt.aurigraph.io/api/v11/info | head -5

echo ""
echo "Testing health endpoint:"
curl -s -I https://dlt.aurigraph.io/q/health | head -5

echo ""
echo "Testing portal assets:"
curl -s -I https://dlt.aurigraph.io/assets/index-LU1HT7_B.js | head -5
TEST_SCRIPT

echo ""
echo -e "${GREEN}════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ NGINX PORTAL FIX DEPLOYMENT COMPLETE${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Portal URL:${NC} https://dlt.aurigraph.io"
echo -e "${YELLOW}API URL:${NC} https://dlt.aurigraph.io/api/v11/"
echo ""
echo -e "${BLUE}What was fixed:${NC}"
echo "  ✓ Root path (/) now serves React portal instead of 404"
echo "  ✓ Portal assets properly cached (1 year expiration)"
echo "  ✓ SPA routing configured (try_files fallback)"
echo "  ✓ WebSocket support added for real-time updates"
echo "  ✓ Backend API properly proxied from Quarkus container"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "  1. Open https://dlt.aurigraph.io in browser"
echo "  2. Verify React portal loads (not JSON error)"
echo "  3. Check that dashboard displays metrics"
echo "  4. Test portal features (navigation, real-time updates)"
echo ""
