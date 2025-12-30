#!/bin/bash
#
# Aurigraph V11 Chunked JAR Upload and Deployment Script
# Chunks the 1.6GB uber JAR, uploads to remote server, reassembles, and deploys
#
# Usage: ./chunk-and-deploy.sh
#

set -e  # Exit on any error

# Configuration
REMOTE_HOST="dlt.aurigraph.io"
REMOTE_USER="subbu"
REMOTE_PORT="2235"
REMOTE_PASS="subbuFuture@2025"
JAR_FILE="target/aurigraph-v11-standalone-11.0.0-runner.jar"
CHUNK_SIZE_MB=50  # 50MB chunks for reliable upload
REMOTE_DIR="/opt/aurigraph"
SERVICE_NAME="aurigraph-v11"

# Colors for output
GREEN='\033[0.32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Aurigraph V11 Chunked Deployment${NC}"
echo -e "${GREEN}========================================${NC}"

# Step 1: Verify JAR exists
echo -e "\n${YELLOW}Step 1: Verifying JAR file...${NC}"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}ERROR: JAR file not found: $JAR_FILE${NC}"
    echo "Please run: ./mvnw clean package -DskipTests -Djacoco.skip=true -Dquarkus.package.jar.type=uber-jar"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
echo -e "${GREEN}✓ Found JAR file: $JAR_FILE ($JAR_SIZE)${NC}"

# Step 2: Create chunks directory
echo -e "\n${YELLOW}Step 2: Creating chunks...${NC}"
CHUNKS_DIR="target/chunks"
rm -rf "$CHUNKS_DIR"
mkdir -p "$CHUNKS_DIR"

# Split JAR into chunks
split -b ${CHUNK_SIZE_MB}M "$JAR_FILE" "$CHUNKS_DIR/jar.part."
CHUNK_COUNT=$(ls -1 "$CHUNKS_DIR" | wc -l | tr -d ' ')
echo -e "${GREEN}✓ Created $CHUNK_COUNT chunks of ${CHUNK_SIZE_MB}MB each${NC}"

# Generate MD5 checksum of original JAR
echo -e "\n${YELLOW}Step 3: Generating checksum...${NC}"
MD5_ORIGINAL=$(md5 -q "$JAR_FILE" 2>/dev/null || md5sum "$JAR_FILE" | awk '{print $1}')
echo "$MD5_ORIGINAL" > "$CHUNKS_DIR/jar.md5"
echo -e "${GREEN}✓ Original JAR MD5: $MD5_ORIGINAL${NC}"

# Step 4: Create reassembly script for remote server
echo -e "\n${YELLOW}Step 4: Creating remote reassembly script...${NC}"
cat > "$CHUNKS_DIR/reassemble.sh" << 'REASSEMBLE_SCRIPT'
#!/bin/bash
# Reassemble JAR from chunks and verify
set -e

JAR_NAME="aurigraph-v11-standalone-11.0.0-runner.jar"
CHUNKS_DIR="/tmp/aurigraph-chunks"

echo "Reassembling $JAR_NAME..."
cd "$CHUNKS_DIR"

# Concatenate all chunks in order
cat jar.part.* > "$JAR_NAME"

# Verify MD5
echo "Verifying checksum..."
MD5_ORIGINAL=$(cat jar.md5)
MD5_ASSEMBLED=$(md5sum "$JAR_NAME" | awk '{print $1}')

if [ "$MD5_ORIGINAL" != "$MD5_ASSEMBLED" ]; then
    echo "ERROR: Checksum mismatch!"
    echo "Original:  $MD5_ORIGINAL"
    echo "Assembled: $MD5_ASSEMBLED"
    exit 1
fi

echo "✓ Checksum verified: $MD5_ASSEMBLED"

# Move JAR to deployment directory
sudo mkdir -p /opt/aurigraph
sudo mv "$JAR_NAME" /opt/aurigraph/
sudo chown aurigraph:aurigraph /opt/aurigraph/"$JAR_NAME"

echo "✓ JAR deployed to /opt/aurigraph/$JAR_NAME"

# Cleanup chunks
rm -rf "$CHUNKS_DIR"
echo "✓ Cleanup complete"
REASSEMBLE_SCRIPT

chmod +x "$CHUNKS_DIR/reassemble.sh"
echo -e "${GREEN}✓ Reassembly script created${NC}"

# Step 5: Create systemd service file
echo -e "\n${YELLOW}Step 5: Creating systemd service configuration...${NC}"
cat > "$CHUNKS_DIR/aurigraph-v11.service" << 'SERVICE_FILE'
[Unit]
Description=Aurigraph DLT V11 High-Performance Platform
After=network.target postgresql.service redis.service
Wants=postgresql.service redis.service

[Service]
Type=simple
User=aurigraph
Group=aurigraph
WorkingDirectory=/opt/aurigraph

# Java 21 with optimized JVM settings for 2M+ TPS
ExecStart=/usr/bin/java \
    -Xms8g -Xmx12g \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:MaxGCPauseMillis=200 \
    -XX:ConcGCThreads=4 \
    -XX:ParallelGCThreads=8 \
    -XX:+AlwaysPreTouch \
    -XX:+UseLargePages \
    -Djava.net.preferIPv4Stack=true \
    -Dquarkus.http.ssl-port=8443 \
    -Dquarkus.http.port=8080 \
    -jar /opt/aurigraph/aurigraph-v11-standalone-11.0.0-runner.jar

# Resource limits
LimitNOFILE=1048576
LimitNPROC=512000

# Security
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/aurigraph /var/log/aurigraph

# Restart policy
Restart=always
RestartSec=10
StartLimitInterval=0

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=aurigraph-v11

[Install]
WantedBy=multi-user.target
SERVICE_FILE

echo -e "${GREEN}✓ Systemd service file created${NC}"

# Step 6: Create NGINX configuration
echo -e "\n${YELLOW}Step 6: Creating NGINX configuration...${NC}"
cat > "$CHUNKS_DIR/nginx-aurigraph.conf" << 'NGINX_CONF'
# Aurigraph V11 NGINX Configuration
# Backend on port 8443 (HTTPS), proxied through NGINX

upstream aurigraph_backend {
    server localhost:8443;
    keepalive 64;
}

server {
    listen 80;
    listen [::]:80;
    server_name dlt.aurigraph.io;

    # Redirect all HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name dlt.aurigraph.io;

    # SSL Configuration (Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/dlt.aurigraph.io/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/dlt.aurigraph.io/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Portal (static files)
    location /portal/ {
        alias /var/www/html/portal/;
        index index.html;
        try_files $uri $uri/ /portal/index.html;

        # CORS headers for portal
        add_header Access-Control-Allow-Origin "*" always;
        add_header Access-Control-Allow-Methods "GET, POST, OPTIONS" always;
        add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization" always;
    }

    # API Proxy to backend (port 8443)
    location /api/v11/ {
        proxy_pass https://localhost:8443/api/v11/;
        proxy_ssl_verify off;  # Self-signed cert on backend
        proxy_http_version 1.1;

        # Headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;

        # WebSocket support
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # Timeouts for long-running requests
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # Buffering
        proxy_buffering off;
        proxy_request_buffering off;

        # CORS headers
        add_header Access-Control-Allow-Origin "*" always;
        add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" always;
        add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization" always;

        # OPTIONS preflight
        if ($request_method = 'OPTIONS') {
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS";
            add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization";
            add_header Access-Control-Max-Age 1728000;
            add_header Content-Type "text/plain; charset=utf-8";
            add_header Content-Length 0;
            return 204;
        }
    }

    # Health checks and metrics
    location /q/ {
        proxy_pass https://localhost:8443/q/;
        proxy_ssl_verify off;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Logs
    access_log /var/log/nginx/aurigraph-access.log;
    error_log /var/log/nginx/aurigraph-error.log;
}
NGINX_CONF

echo -e "${GREEN}✓ NGINX configuration created${NC}"

# Step 7: Create deployment script for remote server
echo -e "\n${YELLOW}Step 7: Creating remote deployment script...${NC}"
cat > "$CHUNKS_DIR/deploy.sh" << 'DEPLOY_SCRIPT'
#!/bin/bash
# Remote deployment script
set -e

echo "======================================"
echo "Aurigraph V11 Remote Deployment"
echo "======================================"

# Create aurigraph user if doesn't exist
if ! id "aurigraph" &>/dev/null; then
    echo "Creating aurigraph user..."
    sudo useradd -r -s /bin/false aurigraph
fi

# Create directories
echo "Creating directories..."
sudo mkdir -p /opt/aurigraph
sudo mkdir -p /var/log/aurigraph
sudo chown -R aurigraph:aurigraph /opt/aurigraph
sudo chown -R aurigraph:aurigraph /var/log/aurigraph

# Install systemd service
echo "Installing systemd service..."
sudo cp /tmp/aurigraph-chunks/aurigraph-v11.service /etc/systemd/system/
sudo systemctl daemon-reload

# Stop existing service if running
if systemctl is-active --quiet aurigraph-v11; then
    echo "Stopping existing service..."
    sudo systemctl stop aurigraph-v11
fi

# Install NGINX configuration
echo "Installing NGINX configuration..."
sudo cp /tmp/aurigraph-chunks/nginx-aurigraph.conf /etc/nginx/sites-available/aurigraph
sudo ln -sf /etc/nginx/sites-available/aurigraph /etc/nginx/sites-enabled/aurigraph

# Test NGINX configuration
echo "Testing NGINX configuration..."
sudo nginx -t

# Reload NGINX
echo "Reloading NGINX..."
sudo systemctl reload nginx

# Enable and start Aurigraph service
echo "Starting Aurigraph V11 service..."
sudo systemctl enable aurigraph-v11
sudo systemctl start aurigraph-v11

# Wait for service to start
echo "Waiting for service to start..."
sleep 10

# Check service status
echo "Service status:"
sudo systemctl status aurigraph-v11 --no-pager || true

# Verify API is responding
echo "Verifying API endpoint..."
sleep 5
curl -k https://localhost:8443/api/v11/health || echo "Health check pending..."

echo "======================================"
echo "✓ Deployment complete!"
echo "======================================"
echo ""
echo "Service URLs:"
echo "  Portal: https://dlt.aurigraph.io/portal/"
echo "  API:    https://dlt.aurigraph.io/api/v11/"
echo "  Health: https://dlt.aurigraph.io/api/v11/health"
echo ""
echo "Service commands:"
echo "  Status:  sudo systemctl status aurigraph-v11"
echo "  Logs:    sudo journalctl -u aurigraph-v11 -f"
echo "  Restart: sudo systemctl restart aurigraph-v11"
echo ""
DEPLOY_SCRIPT

chmod +x "$CHUNKS_DIR/deploy.sh"
echo -e "${GREEN}✓ Deployment script created${NC}"

# Step 8: Display upload instructions
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Chunks created successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "\nChunk statistics:"
echo -e "  Total chunks: ${CHUNK_COUNT}"
echo -e "  Chunk size:   ${CHUNK_SIZE_MB}MB"
echo -e "  Chunks dir:   $CHUNKS_DIR"
echo -e "  MD5 checksum: $MD5_ORIGINAL"

echo -e "\n${YELLOW}Manual upload instructions:${NC}"
echo -e "Since SSH port 2235 may be blocked, use alternative upload methods:"
echo -e ""
echo -e "Option 1: SCP/SFTP (if port 2235 becomes available):"
echo -e "  scp -P 2235 -r $CHUNKS_DIR $REMOTE_USER@$REMOTE_HOST:/tmp/aurigraph-chunks/"
echo -e "  ssh -p 2235 $REMOTE_USER@$REMOTE_HOST"
echo -e "  cd /tmp/aurigraph-chunks && ./reassemble.sh && ./deploy.sh"
echo -e ""
echo -e "Option 2: FTP/FTPS upload:"
echo -e "  Upload contents of $CHUNKS_DIR to /tmp/aurigraph-chunks/ on server"
echo -e "  Then run: ssh -p 2235 $REMOTE_USER@$REMOTE_HOST"
echo -e "  cd /tmp/aurigraph-chunks && ./reassemble.sh && ./deploy.sh"
echo -e ""
echo -e "Option 3: HTTP/HTTPS upload to server:"
echo -e "  Contact hosting provider to enable file upload mechanism"
echo -e ""
echo -e "${YELLOW}Files ready in: $CHUNKS_DIR${NC}"
echo -e ""
echo -e "${GREEN}After upload, run on server:${NC}"
echo -e "  cd /tmp/aurigraph-chunks"
echo -e "  chmod +x reassemble.sh deploy.sh"
echo -e "  ./reassemble.sh"
echo -e "  ./deploy.sh"
echo -e ""
