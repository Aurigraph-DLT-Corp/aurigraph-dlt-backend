#!/bin/bash

###############################################################################
# RBAC V2 Remote Deployment Script
#
# Deploys Aurigraph V11 Enterprise Portal with RBAC V2 to remote server
#
# Remote Server: dlt.aurigraph.io
# SSH Port: 2235
# User: subbu
# Web Port: 9003
#
# Usage: ./deploy-rbac-remote.sh [options]
#
# Options:
#   --dry-run    Show what would be deployed without deploying
#   --full       Deploy everything including all documentation
#   --quick      Deploy only essential files (default)
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
REMOTE_HOST="dlt.aurigraph.io"
REMOTE_PORT="2235"
REMOTE_USER="subbu"
REMOTE_PASSWORD="subbuFuture@2025"
REMOTE_PATH="/home/subbu/aurigraph-v11-portal"
WEB_PORT="9003"

# Deployment mode
DRY_RUN=false
DEPLOY_MODE="quick"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --full)
            DEPLOY_MODE="full"
            shift
            ;;
        --quick)
            DEPLOY_MODE="quick"
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Print header
print_header() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  RBAC V2 Remote Deployment${NC}"
    echo -e "${BLUE}  Aurigraph V11 Enterprise Portal${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${CYAN}Remote Server:${NC} ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
    echo -e "${CYAN}Deployment Path:${NC} ${REMOTE_PATH}"
    echo -e "${CYAN}Web Port:${NC} ${WEB_PORT}"
    echo -e "${CYAN}Deployment Mode:${NC} ${DEPLOY_MODE}"
    if [ "$DRY_RUN" = true ]; then
        echo -e "${YELLOW}DRY RUN MODE - No changes will be made${NC}"
    fi
    echo ""
}

# Print section
print_section() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# Success message
success() {
    echo -e "  ${GREEN}✓${NC} $1"
}

# Info message
info() {
    echo -e "  ${CYAN}ℹ${NC} $1"
}

# Warning message
warn() {
    echo -e "  ${YELLOW}⚠${NC} $1"
}

# Error message
error() {
    echo -e "  ${RED}✗${NC} $1"
}

print_header

###############################################################################
# Step 1: Pre-deployment Checks
###############################################################################
print_section "Pre-deployment Checks"

# Check if required files exist
REQUIRED_FILES=(
    "aurigraph-v11-enterprise-portal.html"
    "aurigraph-rbac-system-v2.js"
    "aurigraph-rbac-ui.html"
    "aurigraph-rbac-ui-loader.js"
    "rbac-admin-setup.html"
)

MISSING_FILES=()
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        success "Found: $file"
    else
        error "Missing: $file"
        MISSING_FILES+=("$file")
    fi
done

if [ ${#MISSING_FILES[@]} -ne 0 ]; then
    error "Missing ${#MISSING_FILES[@]} required file(s)"
    exit 1
fi

# Check if sshpass is available (for automated password login)
if command -v sshpass &> /dev/null; then
    success "sshpass available for automated deployment"
    USE_SSHPASS=true
else
    warn "sshpass not available - you'll need to enter password manually"
    info "Install with: brew install hudochenkov/sshpass/sshpass (macOS)"
    USE_SSHPASS=false
fi

# Check SSH connectivity
info "Testing SSH connection to remote server..."
if [ "$USE_SSHPASS" = true ]; then
    if sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=5 ${REMOTE_USER}@${REMOTE_HOST} "echo 'Connection successful'" &> /dev/null; then
        success "SSH connection successful"
    else
        error "Cannot connect to remote server"
        exit 1
    fi
else
    info "Please verify you can connect manually:"
    info "ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST}"
fi

###############################################################################
# Step 2: Create Deployment Package
###############################################################################
print_section "Creating Deployment Package"

# Create temporary deployment directory
DEPLOY_DIR="rbac-v2-deploy-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$DEPLOY_DIR"
success "Created deployment directory: $DEPLOY_DIR"

# Copy essential files
info "Copying essential files..."
cp aurigraph-v11-enterprise-portal.html "$DEPLOY_DIR/"
cp aurigraph-rbac-system-v2.js "$DEPLOY_DIR/"
cp aurigraph-rbac-ui.html "$DEPLOY_DIR/"
cp aurigraph-rbac-ui-loader.js "$DEPLOY_DIR/"
cp rbac-admin-setup.html "$DEPLOY_DIR/"
cp verify-rbac-deployment.sh "$DEPLOY_DIR/"
success "Copied 6 essential files"

# Copy documentation if full deployment
if [ "$DEPLOY_MODE" = "full" ]; then
    info "Copying documentation files (full mode)..."
    cp RBAC-*.md "$DEPLOY_DIR/" 2>/dev/null || true
    cp SESSION-COMPLETE-*.md "$DEPLOY_DIR/" 2>/dev/null || true
    success "Copied documentation files"
fi

# Create deployment manifest
cat > "$DEPLOY_DIR/DEPLOYMENT-INFO.txt" << EOF
Aurigraph V11 Enterprise Portal - RBAC V2 Deployment

Deployment Date: $(date)
Deployment Mode: $DEPLOY_MODE
Deployed By: $(whoami)
Deployed From: $(hostname)
Git Commit: $(git rev-parse HEAD 2>/dev/null || echo "N/A")
Git Branch: $(git branch --show-current 2>/dev/null || echo "N/A")

Files Deployed:
$(ls -lh "$DEPLOY_DIR" | tail -n +2)

Deployment Package Size: $(du -sh "$DEPLOY_DIR" | cut -f1)
EOF

success "Created deployment manifest"

# Show package contents
info "Deployment package contents:"
ls -lh "$DEPLOY_DIR" | tail -n +2 | awk '{print "  - " $9 " (" $5 ")"}'

PACKAGE_SIZE=$(du -sh "$DEPLOY_DIR" | cut -f1)
info "Total package size: $PACKAGE_SIZE"

###############################################################################
# Step 3: Transfer Files to Remote Server
###############################################################################
print_section "Transferring Files to Remote Server"

if [ "$DRY_RUN" = true ]; then
    info "DRY RUN: Would transfer files to ${REMOTE_HOST}:${REMOTE_PATH}"
    info "DRY RUN: Would use rsync or scp for transfer"
else
    # Create remote directory
    info "Creating remote directory..."
    if [ "$USE_SSHPASS" = true ]; then
        sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_PATH}"
    else
        ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_PATH}"
    fi
    success "Remote directory created/verified"

    # Transfer files using rsync (if available) or scp
    if command -v rsync &> /dev/null; then
        info "Transferring files using rsync..."
        if [ "$USE_SSHPASS" = true ]; then
            sshpass -p "$REMOTE_PASSWORD" rsync -avz -e "ssh -p $REMOTE_PORT" \
                "$DEPLOY_DIR/" \
                ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/
        else
            rsync -avz -e "ssh -p $REMOTE_PORT" \
                "$DEPLOY_DIR/" \
                ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/
        fi
        success "Files transferred using rsync"
    else
        info "Transferring files using scp..."
        if [ "$USE_SSHPASS" = true ]; then
            sshpass -p "$REMOTE_PASSWORD" scp -P $REMOTE_PORT -r \
                "$DEPLOY_DIR/"* \
                ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/
        else
            scp -P $REMOTE_PORT -r \
                "$DEPLOY_DIR/"* \
                ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/
        fi
        success "Files transferred using scp"
    fi

    # Verify transfer
    info "Verifying file transfer..."
    if [ "$USE_SSHPASS" = true ]; then
        FILE_COUNT=$(sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} "ls -1 ${REMOTE_PATH} | wc -l")
    else
        FILE_COUNT=$(ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} "ls -1 ${REMOTE_PATH} | wc -l")
    fi
    success "Verified $FILE_COUNT files on remote server"
fi

###############################################################################
# Step 4: Configure Web Server
###############################################################################
print_section "Configuring Web Server"

# Create nginx configuration
NGINX_CONFIG="
server {
    listen $WEB_PORT;
    server_name $REMOTE_HOST;

    root $REMOTE_PATH;
    index aurigraph-v11-enterprise-portal.html;

    # Security headers
    add_header X-Frame-Options \"SAMEORIGIN\" always;
    add_header X-Content-Type-Options \"nosniff\" always;
    add_header X-XSS-Protection \"1; mode=block\" always;
    add_header Referrer-Policy \"no-referrer-when-downgrade\" always;

    # CORS headers for API calls
    add_header Access-Control-Allow-Origin \"*\" always;
    add_header Access-Control-Allow-Methods \"GET, POST, OPTIONS\" always;
    add_header Access-Control-Allow-Headers \"*\" always;

    location / {
        try_files \$uri \$uri/ /aurigraph-v11-enterprise-portal.html;
    }

    # Admin setup page
    location /admin {
        try_files /rbac-admin-setup.html =404;
    }

    # Static files caching
    location ~* \\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control \"public, immutable\";
    }

    # Security - deny access to sensitive files
    location ~ /\\. {
        deny all;
    }

    location ~ \\.md$ {
        deny all;
    }
}
"

if [ "$DRY_RUN" = true ]; then
    info "DRY RUN: Would create nginx configuration"
    info "DRY RUN: Would configure port $WEB_PORT"
else
    # Save nginx config to remote
    info "Creating nginx configuration..."
    echo "$NGINX_CONFIG" > "$DEPLOY_DIR/aurigraph-portal.conf"

    if [ "$USE_SSHPASS" = true ]; then
        sshpass -p "$REMOTE_PASSWORD" scp -P $REMOTE_PORT \
            "$DEPLOY_DIR/aurigraph-portal.conf" \
            ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/
    else
        scp -P $REMOTE_PORT \
            "$DEPLOY_DIR/aurigraph-portal.conf" \
            ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/
    fi
    success "Nginx configuration created"

    info "Note: Nginx configuration saved to ${REMOTE_PATH}/aurigraph-portal.conf"
    warn "You may need to manually copy this to /etc/nginx/sites-available/ and enable it"
fi

# Create systemd service (for production)
SYSTEMD_SERVICE="
[Unit]
Description=Aurigraph V11 Enterprise Portal
After=network.target

[Service]
Type=simple
User=$REMOTE_USER
WorkingDirectory=$REMOTE_PATH
ExecStart=/usr/bin/python3 -m http.server $WEB_PORT
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
"

if [ "$DRY_RUN" = true ]; then
    info "DRY RUN: Would create systemd service"
else
    info "Creating systemd service file..."
    echo "$SYSTEMD_SERVICE" > "$DEPLOY_DIR/aurigraph-portal.service"
    success "Systemd service file created"
    info "Note: Service file saved to ${REMOTE_PATH}/aurigraph-portal.service"
    warn "You may need to manually copy this to /etc/systemd/system/ and enable it"
fi

###############################################################################
# Step 5: Start/Restart Service
###############################################################################
print_section "Starting Web Service"

if [ "$DRY_RUN" = true ]; then
    info "DRY RUN: Would start web service on port $WEB_PORT"
else
    info "Starting Python HTTP server on port $WEB_PORT..."

    # Create startup script
    STARTUP_SCRIPT="#!/bin/bash
cd $REMOTE_PATH
# Kill existing process on port $WEB_PORT
lsof -ti:$WEB_PORT | xargs kill -9 2>/dev/null || true
# Start new server in background
nohup python3 -m http.server $WEB_PORT > ${REMOTE_PATH}/server.log 2>&1 &
echo \"Server started on port $WEB_PORT (PID: \$!)\"
"

    echo "$STARTUP_SCRIPT" > "$DEPLOY_DIR/start-server.sh"
    chmod +x "$DEPLOY_DIR/start-server.sh"

    if [ "$USE_SSHPASS" = true ]; then
        sshpass -p "$REMOTE_PASSWORD" scp -P $REMOTE_PORT \
            "$DEPLOY_DIR/start-server.sh" \
            ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/

        sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} \
            "cd ${REMOTE_PATH} && chmod +x start-server.sh && ./start-server.sh"
    else
        scp -P $REMOTE_PORT \
            "$DEPLOY_DIR/start-server.sh" \
            ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/

        ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} \
            "cd ${REMOTE_PATH} && chmod +x start-server.sh && ./start-server.sh"
    fi

    success "Web service started"
    sleep 2
fi

###############################################################################
# Step 6: Verify Deployment
###############################################################################
print_section "Verifying Deployment"

if [ "$DRY_RUN" = true ]; then
    info "DRY RUN: Would verify deployment"
else
    # Check if port is listening
    info "Checking if web server is listening on port $WEB_PORT..."
    if [ "$USE_SSHPASS" = true ]; then
        PORT_CHECK=$(sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} \
            "lsof -i :$WEB_PORT | grep LISTEN | wc -l" || echo "0")
    else
        PORT_CHECK=$(ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} \
            "lsof -i :$WEB_PORT | grep LISTEN | wc -l" || echo "0")
    fi

    if [ "$PORT_CHECK" -gt 0 ]; then
        success "Web server is listening on port $WEB_PORT"
    else
        warn "Web server may not be running on port $WEB_PORT"
    fi

    # Test HTTP connection
    info "Testing HTTP connection..."
    sleep 2
    if curl -s -o /dev/null -w "%{http_code}" "http://${REMOTE_HOST}:${WEB_PORT}/" | grep -q "200"; then
        success "HTTP connection successful"
    else
        warn "HTTP connection test failed - server may still be starting"
    fi

    # List deployed files
    info "Deployed files:"
    if [ "$USE_SSHPASS" = true ]; then
        sshpass -p "$REMOTE_PASSWORD" ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} \
            "ls -lh ${REMOTE_PATH}" | tail -n +2 | awk '{print "  - " $9 " (" $5 ")"}'
    else
        ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST} \
            "ls -lh ${REMOTE_PATH}" | tail -n +2 | awk '{print "  - " $9 " (" $5 ")"}'
    fi
fi

###############################################################################
# Step 7: Cleanup
###############################################################################
print_section "Cleanup"

if [ "$DRY_RUN" = false ]; then
    info "Removing temporary deployment directory..."
    rm -rf "$DEPLOY_DIR"
    success "Cleanup complete"
else
    info "DRY RUN: Would remove temporary directory: $DEPLOY_DIR"
fi

###############################################################################
# Summary
###############################################################################
print_section "Deployment Summary"

echo ""
if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}  DRY RUN COMPLETE - No changes made${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
else
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}  ✅ DEPLOYMENT COMPLETE${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
fi
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
echo "  1. Open portal in browser: http://${REMOTE_HOST}:${WEB_PORT}/"
echo "  2. Create admin user: http://${REMOTE_HOST}:${WEB_PORT}/rbac-admin-setup.html"
echo "  3. Test guest registration flow"
echo "  4. Verify all security features work"
echo ""

echo -e "${CYAN}Useful Commands:${NC}"
echo "  SSH to server:    ssh -p $REMOTE_PORT ${REMOTE_USER}@${REMOTE_HOST}"
echo "  View server log:  tail -f ${REMOTE_PATH}/server.log"
echo "  Restart server:   ${REMOTE_PATH}/start-server.sh"
echo "  Check process:    lsof -i :${WEB_PORT}"
echo ""

if [ "$DRY_RUN" = false ]; then
    echo -e "${GREEN}Deployment completed successfully!${NC}"
else
    echo -e "${YELLOW}Run without --dry-run to perform actual deployment${NC}"
fi
echo ""
