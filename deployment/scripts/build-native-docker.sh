#!/bin/bash

################################################################################
# build-native-docker.sh
# Build and Deploy Native Quarkus/GraalVM Docker Containers for Aurigraph V11
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REGISTRY="${DOCKER_REGISTRY:-docker.io}"
NAMESPACE="${DOCKER_NAMESPACE:-aurigraph}"
IMAGE_NAME="v11-native"
VERSION="${VERSION:-11.0.0}"
BUILD_PROFILE="${BUILD_PROFILE:-native-ultra}"
PLATFORMS="${PLATFORMS:-linux/amd64,linux/arm64}"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo ""
    echo "════════════════════════════════════════════════════════════════════"
    echo "  Aurigraph V11 Native Docker Build System"
    echo "  Profile: ${BUILD_PROFILE} | Version: ${VERSION}"
    echo "════════════════════════════════════════════════════════════════════"
    echo ""
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi
    
    # Check Docker Buildx for multi-arch builds
    if ! docker buildx version &> /dev/null; then
        log_warning "Docker Buildx not found. Installing..."
        docker buildx create --use
    fi
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_warning "Java not found. Native build will use container-based build"
    else
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -lt 21 ]; then
            log_warning "Java 21+ required. Current version: $JAVA_VERSION"
        fi
    fi
    
    log_success "Prerequisites check completed"
}

build_native_executable() {
    log_info "Building native executable with profile: ${BUILD_PROFILE}..."
    
    # Clean previous builds
    ./mvnw clean
    
    # Build native executable
    if [ "$USE_CONTAINER_BUILD" = "true" ]; then
        log_info "Using container-based native build..."
        ./mvnw package -P${BUILD_PROFILE} \
            -Dquarkus.native.container-build=true \
            -Dquarkus.native.container-runtime=docker \
            -DskipTests
    else
        log_info "Using local native build..."
        ./mvnw package -P${BUILD_PROFILE} \
            -DskipTests
    fi
    
    # Verify build
    if [ -f target/*-runner ]; then
        log_success "Native executable built successfully"
        ls -lh target/*-runner
    else
        log_error "Native build failed"
        exit 1
    fi
}

build_docker_images() {
    log_info "Building Docker images..."
    
    # Build standard native image
    log_info "Building standard native container..."
    docker build \
        -f Dockerfile.native \
        --build-arg BUILD_PROFILE=${BUILD_PROFILE} \
        -t ${NAMESPACE}/${IMAGE_NAME}:${VERSION} \
        -t ${NAMESPACE}/${IMAGE_NAME}:latest \
        .
    
    # Build distroless image
    log_info "Building distroless native container..."
    docker build \
        -f Dockerfile.native-distroless \
        --build-arg BUILD_PROFILE=${BUILD_PROFILE} \
        -t ${NAMESPACE}/${IMAGE_NAME}-distroless:${VERSION} \
        -t ${NAMESPACE}/${IMAGE_NAME}-distroless:latest \
        .
    
    log_success "Docker images built successfully"
}

build_multiarch_images() {
    log_info "Building multi-architecture images for: ${PLATFORMS}..."
    
    # Create buildx builder
    docker buildx create --name aurigraph-builder --use 2>/dev/null || true
    
    # Build and push multi-arch image
    docker buildx build \
        --platform ${PLATFORMS} \
        -f Dockerfile.native \
        --build-arg BUILD_PROFILE=${BUILD_PROFILE} \
        -t ${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}:${VERSION} \
        -t ${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}:latest \
        --push \
        .
    
    log_success "Multi-architecture images built and pushed"
}

test_native_container() {
    log_info "Testing native container..."
    
    # Start container
    docker run -d \
        --name aurigraph-test \
        -p 9003:9003 \
        -e AURIGRAPH_TARGET_TPS=2000000 \
        ${NAMESPACE}/${IMAGE_NAME}:latest
    
    # Wait for startup
    log_info "Waiting for container startup..."
    sleep 5
    
    # Check health
    if curl -f http://localhost:9003/q/health/ready 2>/dev/null; then
        log_success "Container is healthy"
        
        # Check performance endpoint
        curl -s http://localhost:9003/api/v11/performance | jq .
    else
        log_error "Container health check failed"
        docker logs aurigraph-test
    fi
    
    # Cleanup
    docker stop aurigraph-test
    docker rm aurigraph-test
    
    log_success "Container test completed"
}

start_cluster() {
    log_info "Starting native node cluster..."
    
    # Start cluster with docker-compose
    docker-compose -f docker-compose-native-cluster.yml up -d
    
    # Wait for cluster formation
    log_info "Waiting for cluster formation..."
    sleep 10
    
    # Check cluster health
    for i in 1 2 3; do
        PORT=$((9003 + (i-1)*10))
        if curl -f http://localhost:${PORT}/q/health/ready 2>/dev/null; then
            log_success "Node ${i} is healthy"
        else
            log_warning "Node ${i} health check failed"
        fi
    done
    
    log_success "Cluster started successfully"
}

show_info() {
    echo ""
    echo "════════════════════════════════════════════════════════════════════"
    echo "  Build Complete!"
    echo "════════════════════════════════════════════════════════════════════"
    echo ""
    echo "Images built:"
    echo "  - ${NAMESPACE}/${IMAGE_NAME}:${VERSION}"
    echo "  - ${NAMESPACE}/${IMAGE_NAME}:latest"
    echo "  - ${NAMESPACE}/${IMAGE_NAME}-distroless:${VERSION}"
    echo "  - ${NAMESPACE}/${IMAGE_NAME}-distroless:latest"
    echo ""
    echo "Quick start commands:"
    echo "  # Run single node:"
    echo "  docker run -p 9003:9003 ${NAMESPACE}/${IMAGE_NAME}:latest"
    echo ""
    echo "  # Start cluster:"
    echo "  docker-compose -f docker-compose-native-cluster.yml up"
    echo ""
    echo "  # View logs:"
    echo "  docker-compose -f docker-compose-native-cluster.yml logs -f"
    echo ""
    echo "  # Access services:"
    echo "  - API Gateway: http://localhost:8080"
    echo "  - Node 1: http://localhost:9003"
    echo "  - Node 2: http://localhost:9013"
    echo "  - Node 3: http://localhost:9023"
    echo "  - Prometheus: http://localhost:9090"
    echo "  - Grafana: http://localhost:3000 (admin/admin)"
    echo ""
}

# Main execution
main() {
    print_header
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --profile)
                BUILD_PROFILE="$2"
                shift 2
                ;;
            --version)
                VERSION="$2"
                shift 2
                ;;
            --registry)
                REGISTRY="$2"
                shift 2
                ;;
            --multiarch)
                BUILD_MULTIARCH="true"
                shift
                ;;
            --container-build)
                USE_CONTAINER_BUILD="true"
                shift
                ;;
            --skip-test)
                SKIP_TEST="true"
                shift
                ;;
            --start-cluster)
                START_CLUSTER="true"
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    # Execute build steps
    check_prerequisites
    build_native_executable
    build_docker_images
    
    if [ "$BUILD_MULTIARCH" = "true" ]; then
        build_multiarch_images
    fi
    
    if [ "$SKIP_TEST" != "true" ]; then
        test_native_container
    fi
    
    if [ "$START_CLUSTER" = "true" ]; then
        start_cluster
    fi
    
    show_info
}

# Run main function
main "$@"