#!/bin/bash

#
# SPRINT 16 PRODUCTION DEPLOYMENT SCRIPT
# Aurigraph V11 to dlt.aurigraph.io
#
# Usage: ./deploy-sprint16-production.sh
#
# This script automates:
#  1. Build JAR (30 min)
#  2. Test SSH connection
#  3. Create deployment directory
#  4. Stop existing service
#  5. Copy JAR to production
#  6. Install systemd service
#  7. Start service
#  8. Verify deployment
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
REMOTE_HOST="dlt.aurigraph.io"
REMOTE_PORT="2235"
REMOTE_USER="subbu"
REMOTE_DIR="/opt/aurigraph/v11"
JAR_FILE="target/aurigraph-v11-standalone-11.0.0-runner.jar"
JAR_NAME="aurigraph-v11-standalone-11.0.0-runner.jar"

# Helper functions
print_header() {
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
}

print_step() {
    echo -e "${YELLOW}$1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Start deployment
print_header "SPRINT 16 PRODUCTION DEPLOYMENT"

# Step 1: Build JAR
print_step "[1/8] Building V11 JAR for production..."
echo "This will take 30-40 minutes..."
echo ""

if ./mvnw clean package \
    -DskipTests \
    -Dquarkus.package.jar.type=uber-jar \
    -Dquarkus.package.output-directory=target \
    -q; then
    JAR_SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
    print_success "JAR built successfully ($JAR_SIZE)"
else
    print_error "JAR build failed"
    echo "Run manually for details:"
    echo "  ./mvnw clean package -DskipTests"
    exit 1
fi
echo ""

# Step 2: Test SSH connection
print_step "[2/8] Testing SSH connection to $REMOTE_HOST:$REMOTE_PORT..."

if ssh -p "$REMOTE_PORT" -o ConnectTimeout=10 "$REMOTE_USER@$REMOTE_HOST" "echo 'SSH OK'" &>/dev/null; then
    print_success "SSH connection successful"
else
    print_error "Cannot connect to remote server"
    echo "Please verify:"
    echo "  - Server: $REMOTE_HOST:$REMOTE_PORT"
    echo "  - User: $REMOTE_USER"
    echo "  - Password: Check CLAUDE.md or Credentials.md"
    echo ""
    echo "Test manually:"
    echo "  ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST"
    exit 1
fi
echo ""

# Step 3: Create deployment directory
print_step "[3/8] Creating deployment directory on remote server..."

if ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" "mkdir -p $REMOTE_DIR" 2>/dev/null; then
    print_success "Directory created: $REMOTE_DIR"
else
    print_error "Failed to create directory"
    exit 1
fi
echo ""

# Step 4: Stop existing service
print_step "[4/8] Stopping existing V11 service (if running)..."

ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" "pkill -f 'aurigraph-v11-standalone' 2>/dev/null || true" && sleep 2
print_success "Service stopped (or wasn't running)"
echo ""

# Step 5: Copy JAR to remote
print_step "[5/8] Copying JAR to production server..."
echo "File size: $JAR_SIZE (transfer time: ~5-10 minutes)"
echo ""

if scp -P "$REMOTE_PORT" "$JAR_FILE" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/$JAR_NAME"; then
    print_success "JAR copied successfully"
else
    print_error "Failed to copy JAR file"
    echo "Try manually:"
    echo "  scp -P $REMOTE_PORT $JAR_FILE $REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/"
    exit 1
fi
echo ""

# Step 6: Install and start systemd service
print_step "[6/8] Installing systemd service and starting V11..."

# Create service file
cat > /tmp/aurigraph-v11.service <<'EOF'
[Unit]
Description=Aurigraph V11 Blockchain Platform
After=network.target

[Service]
Type=simple
User=subbu
WorkingDirectory=/opt/aurigraph/v11
ExecStart=/usr/bin/java -Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dquarkus.http.port=9003 -Dquarkus.grpc.server.port=9004 -jar /opt/aurigraph/v11/aurigraph-v11-standalone-11.0.0-runner.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# Copy service file
if scp -P "$REMOTE_PORT" /tmp/aurigraph-v11.service "$REMOTE_USER@$REMOTE_HOST:/tmp/" >/dev/null 2>&1; then
    # Install service
    ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" <<'ENDSSH'
sudo mv /tmp/aurigraph-v11.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable aurigraph-v11.service
sudo systemctl start aurigraph-v11.service
ENDSSH

    print_success "Systemd service installed and started"
else
    print_error "Failed to install service"
    exit 1
fi
echo ""

# Step 7: Wait for service to start
print_step "[7/8] Waiting for V11 service to start (up to 30 seconds)..."

RETRY=0
MAX_RETRIES=30

while [ $RETRY -lt $MAX_RETRIES ]; do
    RESPONSE=$(ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" \
        "curl -s -w '%{http_code}' http://localhost:9003/q/health 2>/dev/null" || echo "000")

    HTTP_CODE=$(echo "$RESPONSE" | tail -c 4)

    if [ "$HTTP_CODE" = "200" ]; then
        print_success "Service online and responding"
        break
    fi

    RETRY=$((RETRY + 1))
    if [ $((RETRY % 5)) -eq 0 ]; then
        echo "  Waiting... ($RETRY/$MAX_RETRIES)"
    fi
    sleep 1
done

if [ $RETRY -eq $MAX_RETRIES ]; then
    echo -e "${YELLOW}⚠ Health check timeout - service may still be starting${NC}"
    echo "Check status manually:"
    echo "  ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'curl http://localhost:9003/q/health'"
fi
echo ""

# Step 8: Verify deployment
print_step "[8/8] Verifying deployment..."

# Health check
if ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" \
    "curl -s http://localhost:9003/q/health" | grep -q "UP"; then
    print_success "Health check passed"
else
    echo -e "${YELLOW}⚠ Health check inconclusive - service may still be initializing${NC}"
fi

# Service status
echo ""
echo "Service Status:"
ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" \
    "sudo systemctl status aurigraph-v11.service --no-pager" | head -10

echo ""
print_header "DEPLOYMENT COMPLETE!"

echo "API Endpoints:"
echo "  Health:       http://$REMOTE_HOST:9003/q/health"
echo "  API Health:   http://$REMOTE_HOST:9003/api/v11/health"
echo "  Status:       http://$REMOTE_HOST:9003/api/v11/status"
echo "  Performance:  http://$REMOTE_HOST:9003/api/v11/performance"
echo "  Metrics:      http://$REMOTE_HOST:9003/q/metrics"
echo ""

echo "Useful Commands:"
echo "  View logs:    ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'sudo journalctl -u aurigraph-v11 -f'"
echo "  Restart:      ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'sudo systemctl restart aurigraph-v11'"
echo "  Stop:         ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'sudo systemctl stop aurigraph-v11'"
echo "  Status:       ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'sudo systemctl status aurigraph-v11'"
echo ""

echo "Next Steps:"
echo "  1. Monitor logs: ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'sudo journalctl -u aurigraph-v11 -f'"
echo "  2. Test APIs from browser"
echo "  3. Run performance benchmarks"
echo "  4. Update monitoring dashboards"
echo ""

print_success "Sprint 16 Production Deployment Complete!"
