#!/bin/bash

###############################################################################
# RBAC V2 Remote Deployment - Corrected Port
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
REMOTE_HOST="dlt.aurigraph.io"
REMOTE_PORT="22"  # Corrected port
REMOTE_USER="subbu"
REMOTE_PASSWORD="subbuFuture@2025"
REMOTE_PATH="/home/subbu/aurigraph-v11-portal"
WEB_PORT="9003"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  RBAC V2 Remote Deployment${NC}"
echo -e "${BLUE}  Aurigraph V11 Enterprise Portal${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${CYAN}Remote Server:${NC} ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
echo -e "${CYAN}Deployment Path:${NC} ${REMOTE_PATH}"
echo -e "${CYAN}Web Port:${NC} ${WEB_PORT}"
echo ""

# Step 1: Pre-deployment Checks
echo -e "${BLUE}▶ Pre-deployment Checks${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

REQUIRED_FILES=(
    "aurigraph-v11-enterprise-portal.html"
    "aurigraph-rbac-system-v2.js"
    "aurigraph-rbac-ui.html"
    "aurigraph-rbac-ui-loader.js"
    "rbac-admin-setup.html"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "  ${GREEN}✓${NC} Found: $file"
    else
        echo -e "  ${RED}✗${NC} Missing: $file"
        exit 1
    fi
done

# Test SSH connection
echo -e "  ${CYAN}ℹ${NC} Testing SSH connection..."
if sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=10 ${REMOTE_USER}@${REMOTE_HOST} "echo 'OK'" &> /dev/null; then
    echo -e "  ${GREEN}✓${NC} SSH connection successful"
else
    echo -e "  ${RED}✗${NC} Cannot connect to remote server"
    exit 1
fi

# Step 2: Create Remote Directory
echo ""
echo -e "${BLUE}▶ Creating Remote Directory${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_PATH}"
echo -e "  ${GREEN}✓${NC} Directory created: ${REMOTE_PATH}"

# Step 3: Transfer Files
echo ""
echo -e "${BLUE}▶ Transferring Files${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo -e "  ${CYAN}ℹ${NC} Transferring files to remote server..."

for file in "${REQUIRED_FILES[@]}"; do
    echo -e "  ${CYAN}→${NC} Transferring: $file"
    sshpass -p "$REMOTE_PASSWORD" scp -P $REMOTE_PORT "$file" ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/
done

# Also transfer verification script
if [ -f "verify-rbac-deployment.sh" ]; then
    echo -e "  ${CYAN}→${NC} Transferring: verify-rbac-deployment.sh"
    sshpass -p "$REMOTE_PASSWORD" scp -P $REMOTE_PORT verify-rbac-deployment.sh ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/
fi

echo -e "  ${GREEN}✓${NC} All files transferred"

# Step 4: Verify Transfer
echo ""
echo -e "${BLUE}▶ Verifying Transfer${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

FILE_COUNT=$(sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} "ls -1 ${REMOTE_PATH} | wc -l")
echo -e "  ${GREEN}✓${NC} Verified $FILE_COUNT files on remote server"

# Step 5: Stop Existing Service
echo ""
echo -e "${BLUE}▶ Stopping Existing Service${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} "lsof -ti:${WEB_PORT} | xargs kill -9 2>/dev/null || true"
echo -e "  ${GREEN}✓${NC} Stopped any existing service on port ${WEB_PORT}"

# Step 6: Start Web Service
echo ""
echo -e "${BLUE}▶ Starting Web Service${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} "cd ${REMOTE_PATH} && nohup python3 -m http.server ${WEB_PORT} > server.log 2>&1 &"
sleep 3
echo -e "  ${GREEN}✓${NC} Web service started on port ${WEB_PORT}"

# Step 7: Verify Service
echo ""
echo -e "${BLUE}▶ Verifying Service${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

PORT_CHECK=$(sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} "lsof -i :${WEB_PORT} | grep LISTEN | wc -l" || echo "0")

if [ "$PORT_CHECK" -gt 0 ]; then
    echo -e "  ${GREEN}✓${NC} Service is running on port ${WEB_PORT}"
else
    echo -e "  ${YELLOW}⚠${NC} Service may not be running - check logs"
fi

# Test HTTP connection
sleep 2
if curl -s -o /dev/null -w "%{http_code}" "http://${REMOTE_HOST}:${WEB_PORT}/" | grep -q "200"; then
    echo -e "  ${GREEN}✓${NC} HTTP connection successful"
else
    echo -e "  ${YELLOW}⚠${NC} HTTP test failed - firewall may need configuration"
fi

# Summary
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  ✅ DEPLOYMENT COMPLETE${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${CYAN}Access URLs:${NC}"
echo -e "  Portal: ${GREEN}http://${REMOTE_HOST}:${WEB_PORT}/${NC}"
echo -e "  Admin:  ${GREEN}http://${REMOTE_HOST}:${WEB_PORT}/rbac-admin-setup.html${NC}"
echo ""
echo -e "${CYAN}Admin Credentials:${NC}"
echo -e "  Email:    ${YELLOW}admin@aurigraph.io${NC}"
echo -e "  Password: ${YELLOW}admin123${NC}"
echo ""
echo -e "${CYAN}Next Steps:${NC}"
echo "  1. Open portal: http://${REMOTE_HOST}:${WEB_PORT}/"
echo "  2. Create admin: http://${REMOTE_HOST}:${WEB_PORT}/rbac-admin-setup.html"
echo "  3. Test guest registration"
echo ""
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo ""
