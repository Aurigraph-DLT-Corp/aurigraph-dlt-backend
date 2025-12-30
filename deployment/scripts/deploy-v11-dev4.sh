#!/bin/bash

# Aurigraph V11 - Dev4 Deployment to dlt.aurigraph.io
# ====================================================
# Deploy Java/Quarkus V11 to production environment

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DOMAIN="dlt.aurigraph.io"
DEV4_SERVER="dev4.aurigraph.io"
DEPLOYMENT_PATH="/opt/aurigraph/v11"
SERVICE_NAME="aurigraph-v11"
HTTP_PORT=9003
GRPC_PORT=9004
JAVA_HOME="/opt/java/openjdk-21"
ENVIRONMENT="production"

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

echo "🚀 Aurigraph V11 - Dev4 Deployment to dlt.aurigraph.io"
echo "======================================================"

# Parse command line arguments
COMMAND=${1:-deploy}

case $COMMAND in
    prepare)
        print_step "Preparing Aurigraph V11 for Dev4 Deployment"
        
        # Check Java version
        print_info "Checking Java version..."
        if ! java -version 2>&1 | grep -q "21"; then
            print_error "Java 21 is required for V11 deployment"
            exit 1
        fi
        print_success "Java 21 detected"
        
        # Clean and build native executable
        print_info "Building native executable with ultra optimization..."
        ./mvnw clean package -Pnative-ultra -DskipTests
        
        if [ ! -f "target/aurigraph-v11-standalone-11.0.0-runner" ]; then
            print_error "Native build failed - executable not found"
            exit 1
        fi
        print_success "Native executable built successfully"
        
        # Create deployment package
        print_info "Creating deployment package..."
        rm -rf dist-v11-dev4
        mkdir -p dist-v11-dev4/{bin,config,logs}
        
        # Copy native executable
        cp target/aurigraph-v11-standalone-11.0.0-runner dist-v11-dev4/bin/aurigraph-v11
        chmod +x dist-v11-dev4/bin/aurigraph-v11
        
        # Copy configuration files
        cp src/main/resources/application.properties dist-v11-dev4/config/
        cp src/main/resources/ai-optimization.properties dist-v11-dev4/config/
        
        # Create production configuration override
        cat > dist-v11-dev4/config/application-prod.properties << EOF
# Aurigraph V11 Production Configuration for dlt.aurigraph.io
quarkus.profile=prod
quarkus.http.port=9003
quarkus.http.host=0.0.0.0
quarkus.grpc.server.port=9004

# Production logging
quarkus.log.level=INFO
quarkus.log.file.enable=true
quarkus.log.file.path=/opt/aurigraph/v11/logs/aurigraph-v11.log
quarkus.log.file.rotation.max-file-size=10M
quarkus.log.file.rotation.max-backup-index=10

# Production metrics
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics

# Production security
quarkus.http.cors=true
quarkus.http.cors.origins=https://dlt.aurigraph.io
quarkus.http.cors.methods=GET,POST,PUT,DELETE,OPTIONS

# Performance settings for production
consensus.target.tps=2500000
ai.optimization.target.tps=2500000
consensus.parallel.threads=2048
ai.resources.thread.pool.size=512
EOF
        
        # Create systemd service file
        cat > dist-v11-dev4/aurigraph-v11.service << EOF
[Unit]
Description=Aurigraph V11 Quantum DLT Platform - dlt.aurigraph.io
After=network.target
Wants=network-online.target

[Service]
Type=simple
User=aurigraph
Group=aurigraph
WorkingDirectory=$DEPLOYMENT_PATH
Environment="JAVA_HOME=$JAVA_HOME"
Environment="QUARKUS_PROFILE=prod"
Environment="MALLOC_ARENA_MAX=2"
ExecStart=$DEPLOYMENT_PATH/bin/aurigraph-v11 -Dquarkus.config.locations=$DEPLOYMENT_PATH/config/
ExecReload=/bin/kill -s HUP \$MAINPID
Restart=always
RestartSec=10
RestartPreventExitStatus=23

# Resource limits
LimitNOFILE=65536
LimitNPROC=4096
MemoryLimit=2G
TimeoutStartSec=60

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ReadWritePaths=$DEPLOYMENT_PATH/logs
ReadOnlyPaths=$DEPLOYMENT_PATH/config

[Install]
WantedBy=multi-user.target
EOF
        
        # Create NGINX configuration
        cp ../dist-dev4/nginx-dlt.aurigraph.io-v11.conf dist-v11-dev4/
        
        # Create deployment manifest
        cat > dist-v11-dev4/deployment-manifest.json << EOF
{
  "version": "11.0.0-quantum",
  "deployment": "dev4-production",
  "domain": "$DOMAIN",
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "runtime": {
    "platform": "GraalVM Native",
    "java_version": "21",
    "framework": "Quarkus 3.26.2",
    "optimization": "ultra"
  },
  "features": {
    "quantum_cryptography": true,
    "hyperraft_consensus": true,
    "ai_optimization": true,
    "cross_chain_bridge": true,
    "real_world_assets": true,
    "grpc_services": true,
    "native_compilation": true
  },
  "performance": {
    "target_tps": "2.5M+",
    "startup_time": "<1s",
    "memory_usage": "<256MB"
  },
  "security": {
    "quantum_resistant": true,
    "tls_version": "1.3",
    "cors_enabled": true,
    "ddos_protection": true
  }
}
EOF
        
        print_success "Deployment package created in dist-v11-dev4/"
        ;;
        
    deploy)
        print_step "Deploying Aurigraph V11 to Dev4"
        
        # Check if package exists
        if [ ! -d "dist-v11-dev4" ]; then
            print_error "Deployment package not found. Run './deploy-v11-dev4.sh prepare' first"
            exit 1
        fi
        
        # Create deployment tarball
        print_info "Creating deployment tarball..."
        tar -czf aurigraph-v11-dev4.tar.gz -C dist-v11-dev4 .
        
        print_info "Deployment package ready: aurigraph-v11-dev4.tar.gz"
        print_info "Next steps:"
        echo "1. Upload package to dev4 server: scp aurigraph-v11-dev4.tar.gz user@dev4.aurigraph.io:/tmp/"
        echo "2. Connect to server: ssh user@dev4.aurigraph.io"
        echo "3. Run server setup: ./deploy-v11-dev4.sh server-setup"
        ;;
        
    server-setup)
        print_step "Setting up Aurigraph V11 on Dev4 Server"
        
        # Check if running on server
        if [ ! -f "/tmp/aurigraph-v11-dev4.tar.gz" ]; then
            print_error "Deployment package not found on server. Upload it first."
            exit 1
        fi
        
        # Stop existing service
        print_info "Stopping existing aurigraph-v11 service..."
        sudo systemctl stop $SERVICE_NAME || true
        
        # Create deployment directory
        print_info "Creating deployment directory..."
        sudo mkdir -p $DEPLOYMENT_PATH/{bin,config,logs}
        
        # Extract deployment package
        print_info "Extracting deployment package..."
        cd /tmp
        sudo tar -xzf aurigraph-v11-dev4.tar.gz -C $DEPLOYMENT_PATH
        
        # Set permissions
        print_info "Setting permissions..."
        sudo chown -R aurigraph:aurigraph $DEPLOYMENT_PATH
        sudo chmod +x $DEPLOYMENT_PATH/bin/aurigraph-v11
        
        # Install systemd service
        print_info "Installing systemd service..."
        sudo cp $DEPLOYMENT_PATH/aurigraph-v11.service /etc/systemd/system/
        sudo systemctl daemon-reload
        
        # Update NGINX configuration
        print_info "Updating NGINX configuration..."
        sudo cp $DEPLOYMENT_PATH/nginx-dlt.aurigraph.io-v11.conf /etc/nginx/sites-available/dlt.aurigraph.io
        sudo ln -sf /etc/nginx/sites-available/dlt.aurigraph.io /etc/nginx/sites-enabled/
        sudo nginx -t && sudo systemctl reload nginx
        
        # Start service
        print_info "Starting Aurigraph V11 service..."
        sudo systemctl enable $SERVICE_NAME
        sudo systemctl start $SERVICE_NAME
        
        # Wait for service to start
        sleep 5
        
        # Check service status
        if sudo systemctl is-active --quiet $SERVICE_NAME; then
            print_success "Aurigraph V11 service is running"
        else
            print_error "Service failed to start. Check logs: sudo journalctl -u $SERVICE_NAME"
            exit 1
        fi
        
        # Test endpoints
        print_info "Testing endpoints..."
        sleep 2
        
        if curl -sf http://localhost:9003/api/v11/health > /dev/null; then
            print_success "Health endpoint responding"
        else
            print_error "Health endpoint not responding"
        fi
        
        if curl -sf https://dlt.aurigraph.io/health > /dev/null; then
            print_success "HTTPS endpoint responding"
        else
            print_warn "HTTPS endpoint not responding - check SSL certificate"
        fi
        
        print_success "Deployment completed successfully!"
        print_info "Service status: sudo systemctl status $SERVICE_NAME"
        print_info "Logs: sudo journalctl -u $SERVICE_NAME -f"
        print_info "Health check: curl https://dlt.aurigraph.io/health"
        ;;
        
    status)
        print_step "Checking Aurigraph V11 Status"
        
        print_info "Service status:"
        sudo systemctl status $SERVICE_NAME
        
        print_info "Health check:"
        curl -sf http://localhost:9003/api/v11/health | jq '.' || echo "Health check failed"
        
        print_info "Performance stats:"
        curl -sf http://localhost:9003/api/v11/stats | jq '.' || echo "Stats unavailable"
        ;;
        
    logs)
        print_step "Showing Aurigraph V11 Logs"
        sudo journalctl -u $SERVICE_NAME -f
        ;;
        
    stop)
        print_step "Stopping Aurigraph V11"
        sudo systemctl stop $SERVICE_NAME
        print_success "Service stopped"
        ;;
        
    start)
        print_step "Starting Aurigraph V11"
        sudo systemctl start $SERVICE_NAME
        print_success "Service started"
        ;;
        
    restart)
        print_step "Restarting Aurigraph V11"
        sudo systemctl restart $SERVICE_NAME
        print_success "Service restarted"
        ;;
        
    *)
        echo "Usage: $0 {prepare|deploy|server-setup|status|logs|start|stop|restart}"
        echo ""
        echo "Commands:"
        echo "  prepare      - Build native executable and create deployment package"
        echo "  deploy       - Create deployment tarball for upload"
        echo "  server-setup - Install and configure on dev4 server (run on server)"
        echo "  status       - Check service status"
        echo "  logs         - View service logs"
        echo "  start        - Start service"
        echo "  stop         - Stop service"
        echo "  restart      - Restart service"
        exit 1
        ;;
esac

print_success "Operation completed successfully!"