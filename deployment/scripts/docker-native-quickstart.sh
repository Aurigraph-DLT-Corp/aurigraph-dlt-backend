#!/bin/bash

################################################################################
# docker-native-quickstart.sh
# Quick start script for Aurigraph V11 Native Docker Containers
# Achieves 2M+ TPS with optimized native nodes
################################################################################

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Aurigraph V11 Native Docker Quick Start                   ║"
echo "║     Target: 2M+ TPS | Memory: <256MB | Startup: <1s           ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Function to print colored messages
log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✓${NC} $1"
}

warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

error() {
    echo -e "${RED}✗${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        error "Docker Compose is not installed. Please install Docker Compose."
        exit 1
    fi
    
    success "Prerequisites check passed"
}

# Build native images
build_images() {
    log "Building native Docker images..."
    
    # Build with ultra optimization
    docker build -f Dockerfile.native \
        --build-arg BUILD_PROFILE=native-ultra \
        -t aurigraph/v11-native:latest \
        . || {
        error "Failed to build native image"
        exit 1
    }
    
    success "Native image built successfully"
}

# Start single node
start_single_node() {
    log "Starting single Aurigraph native node..."
    
    docker run -d \
        --name aurigraph-native-single \
        -p 9003:9003 \
        -p 9004:9004 \
        -p 9005:9005 \
        -e AURIGRAPH_TARGET_TPS=2000000 \
        -e AURIGRAPH_NODE_ID=single-node \
        -e AURIGRAPH_AI_OPTIMIZATION=enabled \
        aurigraph/v11-native:latest
    
    success "Single node started on ports 9003-9005"
}

# Start cluster
start_cluster() {
    log "Starting 3-node native cluster..."
    
    docker-compose -f docker-compose-native-cluster.yml up -d
    
    success "Cluster started with load balancer on port 8080"
}

# Health check
health_check() {
    log "Performing health check..."
    
    sleep 5  # Wait for startup
    
    if curl -f http://localhost:9003/q/health/ready &>/dev/null; then
        success "Node is healthy and ready"
    else
        warning "Node may still be starting up..."
    fi
    
    # Show performance endpoint
    log "Checking performance metrics..."
    curl -s http://localhost:9003/api/v11/performance 2>/dev/null | head -20 || true
}

# Show info
show_info() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║                    Deployment Complete!                        ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo ""
    echo "Access Points:"
    echo "  • API:        http://localhost:9003/api/v11/"
    echo "  • Health:     http://localhost:9003/q/health"
    echo "  • Metrics:    http://localhost:9003/q/metrics"
    echo "  • gRPC:       localhost:9004"
    echo ""
    echo "Cluster (if started):"
    echo "  • Load Balancer: http://localhost:8080"
    echo "  • Prometheus:    http://localhost:9090"
    echo "  • Grafana:       http://localhost:3000 (admin/admin)"
    echo ""
    echo "Useful Commands:"
    echo "  • View logs:     docker logs -f aurigraph-native-single"
    echo "  • Stop node:     docker stop aurigraph-native-single"
    echo "  • Remove node:   docker rm aurigraph-native-single"
    echo "  • Stop cluster:  docker-compose -f docker-compose-native-cluster.yml down"
    echo ""
}

# Main menu
main() {
    check_prerequisites
    
    echo ""
    echo "Select deployment option:"
    echo "  1) Build and run single native node"
    echo "  2) Build and run 3-node cluster"
    echo "  3) Build images only"
    echo "  4) Quick test (assumes images exist)"
    echo ""
    read -p "Enter choice [1-4]: " choice
    
    case $choice in
        1)
            build_images
            start_single_node
            health_check
            show_info
            ;;
        2)
            build_images
            start_cluster
            sleep 10
            health_check
            show_info
            ;;
        3)
            build_images
            success "Images built successfully"
            ;;
        4)
            start_single_node
            health_check
            show_info
            ;;
        *)
            error "Invalid choice"
            exit 1
            ;;
    esac
}

# Handle command line arguments
if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --single     Build and run single node"
    echo "  --cluster    Build and run cluster"
    echo "  --build      Build images only"
    echo "  --quick      Quick test with existing images"
    echo "  --help       Show this help message"
    exit 0
fi

if [ "$1" == "--single" ]; then
    check_prerequisites
    build_images
    start_single_node
    health_check
    show_info
elif [ "$1" == "--cluster" ]; then
    check_prerequisites
    build_images
    start_cluster
    sleep 10
    health_check
    show_info
elif [ "$1" == "--build" ]; then
    check_prerequisites
    build_images
elif [ "$1" == "--quick" ]; then
    check_prerequisites
    start_single_node
    health_check
    show_info
else
    main
fi