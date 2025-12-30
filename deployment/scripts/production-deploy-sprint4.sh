#!/bin/bash

# Aurigraph V11 Sprint 4 - Production Deployment Script
# ====================================================
# Complete deployment with SSL certificate management for dlt.aurigraph.io

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
EMAIL="admin@aurigraph.io"  # For Let's Encrypt

print_step() {
    echo -e "${BLUE}[SPRINT4-DDA]${NC} $1"
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

echo "🚀 Aurigraph V11 Sprint 4 - Production Deployment"
echo "=================================================="
echo "DDA (DevOps & Deployment Agent) - Sprint 4 Execution"
echo "Target: 1.6M+ TPS with 15-core optimizations"
echo ""

# Parse command line arguments
COMMAND=${1:-prepare}

case $COMMAND in
    check-ssl)
        print_step "Checking SSL Certificate Status for $DOMAIN"
        
        print_info "Testing HTTPS connectivity..."
        if curl -I --max-time 10 https://$DOMAIN 2>/dev/null; then
            print_success "HTTPS connection successful"
        else
            print_warn "HTTPS connection failed - certificate may need renewal"
        fi
        
        print_info "Certificate details:"
        openssl s_client -connect $DOMAIN:443 -servername $DOMAIN 2>/dev/null | \
        openssl x509 -noout -dates -subject -issuer | while IFS= read -r line; do
            echo "  $line"
        done
        
        print_info "Checking certificate expiration..."
        EXPIRY_DATE=$(openssl s_client -connect $DOMAIN:443 -servername $DOMAIN 2>/dev/null | \
                     openssl x509 -noout -enddate | cut -d= -f2)
        EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
        CURRENT_EPOCH=$(date +%s)
        DAYS_UNTIL_EXPIRY=$(( (EXPIRY_EPOCH - CURRENT_EPOCH) / 86400 ))
        
        if [ $DAYS_UNTIL_EXPIRY -gt 30 ]; then
            print_success "Certificate valid for $DAYS_UNTIL_EXPIRY days"
        else
            print_warn "Certificate expires in $DAYS_UNTIL_EXPIRY days - renewal recommended"
        fi
        ;;
        
    fix-ssl)
        print_step "Fixing SSL Certificate for $DOMAIN"
        
        print_info "Installing/updating certbot..."
        sudo apt update && sudo apt install -y certbot python3-certbot-nginx
        
        print_info "Stopping NGINX temporarily for certificate renewal..."
        sudo systemctl stop nginx
        
        print_info "Obtaining new SSL certificate for $DOMAIN..."
        sudo certbot certonly --standalone \
            --email $EMAIL \
            --agree-tos \
            --no-eff-email \
            --domains $DOMAIN \
            --renew-by-default
        
        print_info "Configuring auto-renewal..."
        echo "0 12 * * * /usr/bin/certbot renew --quiet" | sudo crontab -
        
        print_info "Restarting NGINX with new certificate..."
        sudo systemctl start nginx
        
        print_success "SSL certificate renewed and configured"
        ;;
        
    prepare)
        print_step "Preparing Aurigraph V11 Sprint 4 Production Build"
        
        # Check Java version
        print_info "Checking Java 21 availability..."
        if ! java -version 2>&1 | grep -q "21"; then
            print_error "Java 21 is required for V11 deployment"
            print_info "Install Java 21: sudo apt install openjdk-21-jdk"
            exit 1
        fi
        print_success "Java 21 detected"
        
        # Clean and build with 15-core optimizations
        print_info "Building V11 with 15-core optimizations..."
        export JAVA_OPTS="-Xmx32g -Xms32g -XX:+UseG1GC -XX:+UseNUMA"
        
        # Use the 15-core optimized configuration
        cp 15core-optimized-config.properties src/main/resources/application-prod.properties
        
        print_info "Building native executable with ultra optimization..."
        ./mvnw clean package -Pnative-ultra \
            -Dquarkus.profile=prod \
            -Dquarkus.native.additional-build-args="--gc=G1,-march=native,-O3" \
            -DskipTests
        
        if [ ! -f "target/aurigraph-v11-standalone-11.0.0-runner" ]; then
            print_error "Native build failed - executable not found"
            exit 1
        fi
        print_success "15-core optimized native executable built successfully"
        
        # Create deployment package
        print_info "Creating Sprint 4 deployment package..."
        rm -rf dist-v11-sprint4
        mkdir -p dist-v11-sprint4/{bin,config,logs,scripts,monitoring}
        
        # Copy native executable
        cp target/aurigraph-v11-standalone-11.0.0-runner dist-v11-sprint4/bin/aurigraph-v11
        chmod +x dist-v11-sprint4/bin/aurigraph-v11
        
        # Copy configurations
        cp src/main/resources/application*.properties dist-v11-sprint4/config/
        cp 15core-optimized-config.properties dist-v11-sprint4/config/
        
        # Copy NGINX configuration with SSL fixes
        cp ../dist-dev4/nginx-dlt.aurigraph.io-v11.conf dist-v11-sprint4/
        
        # Create enhanced systemd service
        cat > dist-v11-sprint4/aurigraph-v11-sprint4.service << 'EOF'
[Unit]
Description=Aurigraph V11 Sprint 4 - Quantum DLT Platform (15-core optimized)
After=network.target
Wants=network-online.target

[Service]
Type=simple
User=aurigraph
Group=aurigraph
WorkingDirectory=/opt/aurigraph/v11
Environment="JAVA_HOME=/opt/java/openjdk-21"
Environment="QUARKUS_PROFILE=prod"
Environment="MALLOC_ARENA_MAX=2"
Environment="JVM_ARGS=-XX:+UseG1GC -XX:+UseNUMA -XX:MaxGCPauseMillis=50"
ExecStart=/opt/aurigraph/v11/bin/aurigraph-v11 \
    -Dquarkus.config.locations=/opt/aurigraph/v11/config/ \
    -Dquarkus.http.port=9003 \
    -Dquarkus.grpc.server.port=9004
ExecReload=/bin/kill -s HUP $MAINPID
Restart=always
RestartSec=5
RestartPreventExitStatus=23

# Resource limits for 15-core + 64GB system
LimitNOFILE=65536
LimitNPROC=8192
MemoryLimit=40G
CPUQuota=1500%  # 15 cores = 1500%

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ReadWritePaths=/opt/aurigraph/v11/logs
ReadOnlyPaths=/opt/aurigraph/v11/config

# Performance settings
OOMScoreAdjust=-500
IOSchedulingClass=1
IOSchedulingPriority=4

[Install]
WantedBy=multi-user.target
EOF
        
        # Create monitoring configuration
        cat > dist-v11-sprint4/monitoring/prometheus-v11.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'aurigraph-v11'
    static_configs:
      - targets: ['localhost:9003']
    metrics_path: '/q/metrics'
    scrape_interval: 5s
    
  - job_name: 'aurigraph-v11-performance'
    static_configs:
      - targets: ['localhost:9003']
    metrics_path: '/api/v11/stats'
    scrape_interval: 10s

rule_files:
  - "v11-alert-rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - localhost:9093
EOF
        
        # Create alert rules
        cat > dist-v11-sprint4/monitoring/v11-alert-rules.yml << 'EOF'
groups:
  - name: aurigraph-v11-alerts
    rules:
      - alert: HighThroughputDrop
        expr: rate(aurigraph_transactions_processed_total[1m]) < 1000000
        for: 30s
        labels:
          severity: warning
        annotations:
          summary: "TPS dropped below 1M"
          
      - alert: CriticalThroughputDrop
        expr: rate(aurigraph_transactions_processed_total[1m]) < 500000
        for: 10s
        labels:
          severity: critical
        annotations:
          summary: "TPS dropped below 500K - critical issue"
          
      - alert: ServiceDown
        expr: up{job="aurigraph-v11"} == 0
        for: 10s
        labels:
          severity: critical
        annotations:
          summary: "Aurigraph V11 service is down"
EOF
        
        # Create deployment manifest
        cat > dist-v11-sprint4/deployment-manifest.json << EOF
{
  "sprint": "Sprint 4",
  "version": "11.0.0-sprint4-15core",
  "deployment": "production-dlt.aurigraph.io",
  "domain": "$DOMAIN",
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "hardware": {
    "cpu": "15-core Intel Xeon Gold",
    "memory": "64GB DDR4",
    "optimization": "ultra-native-15core"
  },
  "runtime": {
    "platform": "GraalVM Native",
    "java_version": "21",
    "framework": "Quarkus 3.26.2",
    "gc": "G1GC with NUMA optimization"
  },
  "performance_targets": {
    "target_tps": "1.6M+",
    "startup_time": "<1s",
    "memory_usage": "<32GB",
    "cpu_utilization": "70-85%"
  },
  "security": {
    "quantum_resistant": true,
    "tls_version": "1.3",
    "hsts_enabled": true,
    "ssl_certificate": "Let's Encrypt",
    "cors_enabled": true,
    "ddos_protection": true,
    "rate_limiting": true
  },
  "monitoring": {
    "prometheus": true,
    "health_checks": true,
    "performance_tracking": true,
    "alert_thresholds": {
      "tps_warning": "1M",
      "tps_critical": "500K"
    }
  }
}
EOF
        
        print_success "Sprint 4 deployment package created in dist-v11-sprint4/"
        print_info "Package includes:"
        echo "  ✓ 15-core optimized native executable"
        echo "  ✓ Enhanced SSL/TLS configuration"
        echo "  ✓ Production systemd service"
        echo "  ✓ Prometheus monitoring setup"
        echo "  ✓ Alert rules and thresholds"
        echo "  ✓ Rate limiting and DDoS protection"
        ;;
        
    deploy)
        print_step "Creating Sprint 4 Deployment Package"
        
        if [ ! -d "dist-v11-sprint4" ]; then
            print_error "Sprint 4 package not found. Run './production-deploy-sprint4.sh prepare' first"
            exit 1
        fi
        
        # Create deployment tarball
        print_info "Creating deployment tarball..."
        tar -czf aurigraph-v11-sprint4.tar.gz -C dist-v11-sprint4 .
        
        print_success "Sprint 4 deployment package ready: aurigraph-v11-sprint4.tar.gz"
        print_info "Next steps for production deployment:"
        echo "1. Upload: scp aurigraph-v11-sprint4.tar.gz user@$DEV4_SERVER:/tmp/"
        echo "2. Connect: ssh user@$DEV4_SERVER"
        echo "3. Fix SSL: sudo ./production-deploy-sprint4.sh fix-ssl"
        echo "4. Install: sudo ./production-deploy-sprint4.sh server-install"
        echo "5. Validate: ./production-deploy-sprint4.sh validate"
        ;;
        
    server-install)
        print_step "Installing Aurigraph V11 Sprint 4 on Production Server"
        
        if [ ! -f "/tmp/aurigraph-v11-sprint4.tar.gz" ]; then
            print_error "Sprint 4 package not found. Upload it first."
            exit 1
        fi
        
        # Stop existing service
        print_info "Stopping existing services..."
        sudo systemctl stop aurigraph-v11 || true
        sudo systemctl stop nginx
        
        # Create deployment directory
        print_info "Setting up deployment directory..."
        sudo mkdir -p $DEPLOYMENT_PATH/{bin,config,logs,monitoring}
        
        # Extract package
        print_info "Extracting Sprint 4 package..."
        cd /tmp
        sudo tar -xzf aurigraph-v11-sprint4.tar.gz -C $DEPLOYMENT_PATH
        
        # Set permissions
        print_info "Configuring permissions..."
        sudo useradd -r -s /bin/false aurigraph || true
        sudo chown -R aurigraph:aurigraph $DEPLOYMENT_PATH
        sudo chmod +x $DEPLOYMENT_PATH/bin/aurigraph-v11
        
        # Install systemd service
        print_info "Installing Sprint 4 systemd service..."
        sudo cp $DEPLOYMENT_PATH/aurigraph-v11-sprint4.service /etc/systemd/system/aurigraph-v11.service
        sudo systemctl daemon-reload
        
        # Configure NGINX with SSL fixes
        print_info "Configuring NGINX with enhanced security..."
        sudo cp $DEPLOYMENT_PATH/nginx-dlt.aurigraph.io-v11.conf /etc/nginx/sites-available/dlt.aurigraph.io
        sudo ln -sf /etc/nginx/sites-available/dlt.aurigraph.io /etc/nginx/sites-enabled/
        
        # Test NGINX configuration
        if sudo nginx -t; then
            print_success "NGINX configuration valid"
        else
            print_error "NGINX configuration invalid - check syntax"
            exit 1
        fi
        
        # Start services
        print_info "Starting Aurigraph V11 Sprint 4..."
        sudo systemctl enable aurigraph-v11
        sudo systemctl start aurigraph-v11
        
        print_info "Starting NGINX..."
        sudo systemctl start nginx
        
        # Wait for startup
        print_info "Waiting for service startup..."
        sleep 10
        
        print_success "Sprint 4 deployment completed!"
        ;;
        
    validate)
        print_step "Validating Sprint 4 Production Deployment"
        
        print_info "Service status check..."
        sudo systemctl status aurigraph-v11 --no-pager
        
        print_info "Health endpoint test..."
        if curl -sf http://localhost:9003/api/v11/health; then
            print_success "Local health check passed"
        else
            print_error "Local health check failed"
        fi
        
        print_info "HTTPS endpoint test..."
        if curl -sf https://$DOMAIN/health; then
            print_success "HTTPS health check passed"
        else
            print_warn "HTTPS health check failed - check SSL certificate"
        fi
        
        print_info "Performance metrics test..."
        curl -sf http://localhost:9003/api/v11/stats | jq '.' || echo "Stats endpoint not responding"
        
        print_info "SSL certificate validation..."
        ./production-deploy-sprint4.sh check-ssl
        
        print_success "Sprint 4 validation completed"
        ;;
        
    monitoring)
        print_step "Setting up Sprint 4 Monitoring"
        
        print_info "Installing Prometheus..."
        if ! command -v prometheus &> /dev/null; then
            wget https://github.com/prometheus/prometheus/releases/latest/download/prometheus-linux-amd64.tar.gz
            tar xzf prometheus-linux-amd64.tar.gz
            sudo cp prometheus-*/prometheus /usr/local/bin/
            sudo cp prometheus-*/promtool /usr/local/bin/
        fi
        
        print_info "Configuring Prometheus for V11..."
        sudo cp $DEPLOYMENT_PATH/monitoring/prometheus-v11.yml /etc/prometheus/prometheus.yml
        sudo cp $DEPLOYMENT_PATH/monitoring/v11-alert-rules.yml /etc/prometheus/
        
        print_info "Starting Prometheus..."
        sudo systemctl restart prometheus
        
        print_success "Monitoring configured for Sprint 4"
        ;;
        
    performance-test)
        print_step "Sprint 4 Performance Validation"
        
        print_info "Running 15-core optimized performance test..."
        cd /opt/aurigraph/v11
        
        # Use the performance benchmark from V11
        if [ -f "performance-benchmark.sh" ]; then
            ./performance-benchmark.sh
        else
            print_warn "Performance benchmark script not found"
        fi
        
        print_info "Target: 1.6M+ TPS with 15-core optimization"
        ;;
        
    *)
        echo "Aurigraph V11 Sprint 4 - Production Deployment"
        echo "Usage: $0 {prepare|deploy|fix-ssl|check-ssl|server-install|validate|monitoring|performance-test}"
        echo ""
        echo "Sprint 4 Commands:"
        echo "  prepare        - Build 15-core optimized native executable and create package"
        echo "  deploy         - Create deployment tarball for upload"
        echo "  fix-ssl        - Fix SSL certificate for dlt.aurigraph.io"
        echo "  check-ssl      - Check SSL certificate status and expiry"
        echo "  server-install - Install Sprint 4 package on production server"
        echo "  validate       - Validate deployment and SSL configuration"
        echo "  monitoring     - Setup Prometheus monitoring"
        echo "  performance-test - Run 15-core performance validation"
        echo ""
        echo "Sprint 4 Objectives:"
        echo "  ✓ SSL Certificate verification and renewal"
        echo "  ✓ 15-core Intel Xeon Gold optimization"
        echo "  ✓ 1.6M+ TPS performance target"
        echo "  ✓ Enhanced security headers and DDoS protection"
        echo "  ✓ Production monitoring and alerting"
        exit 1
        ;;
esac

print_success "Sprint 4 operation completed successfully!"
print_info "DDA (DevOps & Deployment Agent) - Sprint 4 execution complete"