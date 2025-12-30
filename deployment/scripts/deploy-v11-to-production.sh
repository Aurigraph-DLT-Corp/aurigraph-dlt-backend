#!/bin/bash
#
# Aurigraph V11 Production Deployment Script
# Server: dlt.aurigraph.io:2235
# User: subbu
#
# This script deploys the V11 uber-JAR to production server
# Run this script manually from your local machine
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
REMOTE_HOST="dlt.aurigraph.io"
REMOTE_PORT="2235"
REMOTE_USER="subbu"
REMOTE_DIR="/opt/aurigraph/v11"
JAR_FILE="target/aurigraph-v11-standalone-11.0.0-runner.jar"
JAR_NAME="aurigraph-v11-standalone-11.0.0-runner.jar"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Aurigraph V11 Production Deployment${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Step 1: Verify JAR exists
echo -e "${YELLOW}[1/6] Verifying JAR file...${NC}"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}ERROR: JAR file not found: $JAR_FILE${NC}"
    echo "Please build the JAR first:"
    echo "  ./mvnw clean package -Dmaven.test.skip=true -Dquarkus.package.jar.type=uber-jar"
    exit 1
fi

JAR_SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
echo -e "${GREEN}✓ JAR file found: $JAR_FILE ($JAR_SIZE)${NC}"
echo ""

# Step 2: Test SSH connection
echo -e "${YELLOW}[2/6] Testing SSH connection to $REMOTE_HOST:$REMOTE_PORT...${NC}"
if ssh -p "$REMOTE_PORT" -o ConnectTimeout=10 -o BatchMode=no "$REMOTE_USER@$REMOTE_HOST" "echo 'SSH connection successful'" 2>/dev/null; then
    echo -e "${GREEN}✓ SSH connection successful${NC}"
else
    echo -e "${RED}ERROR: Cannot connect to remote server${NC}"
    echo "Please verify:"
    echo "  - Server is accessible: ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST"
    echo "  - Password: subbuFuture@2025"
    echo "  - Or add your SSH key: ssh-copy-id -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST"
    exit 1
fi
echo ""

# Step 3: Create deployment directory
echo -e "${YELLOW}[3/6] Creating deployment directory on remote server...${NC}"
ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" "mkdir -p $REMOTE_DIR && ls -la /opt/aurigraph/" || {
    echo -e "${RED}ERROR: Failed to create directory${NC}"
    exit 1
}
echo -e "${GREEN}✓ Directory created: $REMOTE_DIR${NC}"
echo ""

# Step 4: Stop existing V11 service if running
echo -e "${YELLOW}[4/6] Stopping existing V11 service (if running)...${NC}"
ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" "pkill -f 'aurigraph-v11-standalone' || echo 'No existing service running'"
echo -e "${GREEN}✓ Service stopped${NC}"
echo ""

# Step 5: Copy JAR to remote server
echo -e "${YELLOW}[5/6] Copying JAR to remote server (this may take a few minutes for 1.6GB)...${NC}"
scp -P "$REMOTE_PORT" "$JAR_FILE" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/$JAR_NAME" || {
    echo -e "${RED}ERROR: Failed to copy JAR file${NC}"
    exit 1
}
echo -e "${GREEN}✓ JAR copied successfully${NC}"
echo ""

# Step 6: Start V11 application
echo -e "${YELLOW}[6/6] Starting V11 application...${NC}"

# Create systemd service file
cat > /tmp/aurigraph-v11.service <<EOF
[Unit]
Description=Aurigraph V11 Blockchain Platform
After=network.target

[Service]
Type=simple
User=$REMOTE_USER
WorkingDirectory=$REMOTE_DIR
ExecStart=/usr/bin/java -Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dquarkus.http.port=9003 -Dquarkus.grpc.server.port=9004 -jar $REMOTE_DIR/$JAR_NAME
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# Copy systemd service file to remote
scp -P "$REMOTE_PORT" /tmp/aurigraph-v11.service "$REMOTE_USER@$REMOTE_HOST:/tmp/"

# Install and start service
ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" <<'ENDSSH'
# Install systemd service
sudo mv /tmp/aurigraph-v11.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable aurigraph-v11.service
sudo systemctl start aurigraph-v11.service

# Wait for service to start
sleep 5

# Check service status
sudo systemctl status aurigraph-v11.service --no-pager || true
ENDSSH

echo -e "${GREEN}✓ V11 application started${NC}"
echo ""

# Step 7: Verify deployment
echo -e "${YELLOW}[7/7] Verifying deployment...${NC}"
sleep 10  # Wait for application to fully start

# Test health endpoint
if ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" "curl -s http://localhost:9003/api/v11/health" | grep -q "UP\|healthy\|status"; then
    echo -e "${GREEN}✓ Health check passed${NC}"
else
    echo -e "${YELLOW}⚠ Health check returned unexpected response (application may still be starting)${NC}"
fi
echo ""

# Display deployment summary
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Service Status:"
ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" "sudo systemctl status aurigraph-v11.service --no-pager | head -15"
echo ""
echo "API Endpoints:"
echo "  Health:       http://dlt.aurigraph.io:9003/api/v11/health"
echo "  Status:       http://dlt.aurigraph.io:9003/api/v11/status"
echo "  Info:         http://dlt.aurigraph.io:9003/api/v11/info"
echo "  Performance:  http://dlt.aurigraph.io:9003/api/v11/performance"
echo "  AI Stats:     http://dlt.aurigraph.io:9003/api/v11/ai/stats"
echo "  Crypto:       http://dlt.aurigraph.io:9003/api/v11/crypto/status"
echo "  Metrics:      http://dlt.aurigraph.io:9003/q/metrics"
echo ""
echo "Useful Commands:"
echo "  View logs:    ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'sudo journalctl -u aurigraph-v11 -f'"
echo "  Stop service: ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'sudo systemctl stop aurigraph-v11'"
echo "  Restart:      ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'sudo systemctl restart aurigraph-v11'"
echo ""
echo -e "${GREEN}Next Steps:${NC}"
echo "  1. Monitor logs for any errors"
echo "  2. Test API endpoints from browser"
echo "  3. Run performance benchmarks"
echo "  4. Update monitoring dashboards"
echo ""
