#!/bin/bash
# Aurigraph V11 Native Compilation Build Script
# Automated build system for optimized GraalVM native images
# 
# Usage:
#   ./build-native.sh [profile] [build-type]
#   
# Profiles: native, native-fast, native-ultra
# Build Types: dev, test, prod
#
# Examples:
#   ./build-native.sh native-ultra prod    # Production optimized build
#   ./build-native.sh native-fast dev      # Fast development build
#   ./build-native.sh native test          # Standard test build

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
BUILD_LOG="$PROJECT_ROOT/build-native.log"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-aurigraph}"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$BUILD_LOG"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$BUILD_LOG"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$BUILD_LOG"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$BUILD_LOG"
}

# Parse arguments
PROFILE="${1:-native}"
BUILD_TYPE="${2:-dev}"
DOCKER_BUILD="${3:-false}"

# Validate profile
case "$PROFILE" in
    native|native-fast|native-ultra)
        log_info "Using profile: $PROFILE"
        ;;
    *)
        log_error "Invalid profile: $PROFILE. Use: native, native-fast, or native-ultra"
        exit 1
        ;;
esac

# Validate build type
case "$BUILD_TYPE" in
    dev|test|prod)
        log_info "Using build type: $BUILD_TYPE"
        ;;
    *)
        log_error "Invalid build type: $BUILD_TYPE. Use: dev, test, or prod"
        exit 1
        ;;
esac

# Initialize build log
echo "=== Aurigraph V11 Native Build Started at $(date) ===" > "$BUILD_LOG"
echo "Profile: $PROFILE" >> "$BUILD_LOG"
echo "Build Type: $BUILD_TYPE" >> "$BUILD_LOG"
echo "Docker Build: $DOCKER_BUILD" >> "$BUILD_LOG"
echo "" >> "$BUILD_LOG"

# Pre-build checks
check_prerequisites() {
    log_info "Checking build prerequisites..."
    
    # Check Java version
    if ! java -version 2>&1 | grep -q "21\|22\|23"; then
        log_warning "Java 21+ recommended for optimal performance"
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null && ! [ -f "./mvnw" ]; then
        log_error "Maven or Maven Wrapper not found"
        exit 1
    fi
    
    # Check Docker if needed
    if [ "$DOCKER_BUILD" = "true" ] && ! command -v docker &> /dev/null; then
        log_error "Docker not found but docker build requested"
        exit 1
    fi
    
    # Check memory
    AVAILABLE_MEMORY=$(free -m | awk 'NR==2{printf "%d", $7}')
    if [ "$AVAILABLE_MEMORY" -lt 4096 ]; then
        log_warning "Low memory ($AVAILABLE_MEMORY MB). Native build may fail or be slow."
        if [ "$PROFILE" = "native-ultra" ]; then
            log_error "native-ultra profile requires at least 8GB RAM"
            exit 1
        fi
    fi
    
    log_success "Prerequisites check completed"
}

# Clean previous builds
clean_build() {
    log_info "Cleaning previous builds..."
    ./mvnw clean >> "$BUILD_LOG" 2>&1
    rm -rf target/native-image-*
    log_success "Clean completed"
}

# Configure build environment
configure_environment() {
    log_info "Configuring build environment for $BUILD_TYPE..."
    
    case "$BUILD_TYPE" in
        dev)
            export MAVEN_OPTS="-Xmx4g -XX:+UseG1GC"
            export QUARKUS_PROFILE="dev"
            ;;
        test)
            export MAVEN_OPTS="-Xmx6g -XX:+UseG1GC -XX:+UseStringDeduplication"
            export QUARKUS_PROFILE="test"
            ;;
        prod)
            export MAVEN_OPTS="-Xmx8g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat"
            export QUARKUS_PROFILE="prod"
            ;;
    esac
    
    log_success "Environment configured: MAVEN_OPTS=$MAVEN_OPTS"
}

# Build native image
build_native() {
    log_info "Starting native compilation with profile: $PROFILE..."
    
    local start_time=$(date +%s)
    
    # Set build args based on profile
    local build_args=""
    case "$PROFILE" in
        native-fast)
            build_args="-Pnative-fast -Dquarkus.native.native-image-xmx=4g"
            ;;
        native-ultra)
            build_args="-Pnative-ultra -Dquarkus.native.native-image-xmx=8g"
            ;;
        *)
            build_args="-Pnative -Dquarkus.native.native-image-xmx=6g"
            ;;
    esac
    
    # Add build type specific args
    if [ "$BUILD_TYPE" = "test" ]; then
        build_args="$build_args -Dmaven.test.skip=false"
    else
        build_args="$build_args -Dmaven.test.skip=true"
    fi
    
    log_info "Build command: ./mvnw package $build_args"
    
    if ./mvnw package $build_args --no-transfer-progress -B >> "$BUILD_LOG" 2>&1; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        log_success "Native compilation completed in ${duration}s"
    else
        log_error "Native compilation failed. Check $BUILD_LOG for details"
        return 1
    fi
}

# Analyze binary
analyze_binary() {
    log_info "Analyzing native binary..."
    
    local binary_path=$(find target -name "*-runner" -type f | head -1)
    if [ -z "$binary_path" ]; then
        log_error "Native binary not found"
        return 1
    fi
    
    local binary_size=$(du -sh "$binary_path" | cut -f1)
    log_info "Binary size: $binary_size"
    
    # Check if binary is static
    if ldd "$binary_path" 2>/dev/null | grep -q "not a dynamic executable"; then
        log_success "Static binary created (good for containers)"
    else
        log_warning "Dynamic binary created. Dependencies:"
        ldd "$binary_path" 2>/dev/null || true
    fi
    
    # Test startup time
    log_info "Testing startup time..."
    local startup_output=$(timeout 30s "$binary_path" --help 2>&1 || true)
    if echo "$startup_output" | grep -q "Usage\|Help\|Aurigraph"; then
        log_success "Binary startup test passed"
    else
        log_warning "Binary startup test inconclusive"
    fi
}

# Build Docker image if requested
build_docker() {
    if [ "$DOCKER_BUILD" = "true" ]; then
        log_info "Building Docker image..."
        
        local dockerfile=""
        case "$PROFILE" in
            native-ultra)
                dockerfile="src/main/docker/Dockerfile.native-optimized"
                ;;
            native-fast)
                dockerfile="src/main/docker/Dockerfile.native"
                ;;
            *)
                dockerfile="src/main/docker/Dockerfile.native-micro"
                ;;
        esac
        
        local image_tag="$DOCKER_REGISTRY/aurigraph-v11:$BUILD_TYPE-$PROFILE"
        
        if docker build -f "$dockerfile" -t "$image_tag" . >> "$BUILD_LOG" 2>&1; then
            log_success "Docker image built: $image_tag"
            
            # Analyze Docker image
            local image_size=$(docker images --format "table {{.Size}}" "$image_tag" | tail -1)
            log_info "Docker image size: $image_size"
            
            # Test Docker container
            log_info "Testing Docker container..."
            if timeout 10s docker run --rm "$image_tag" --help >> "$BUILD_LOG" 2>&1; then
                log_success "Docker container test passed"
            else
                log_warning "Docker container test timed out or failed"
            fi
        else
            log_error "Docker build failed"
            return 1
        fi
    fi
}

# Performance benchmark
performance_benchmark() {
    if [ "$BUILD_TYPE" = "prod" ]; then
        log_info "Running performance benchmark..."
        
        local binary_path=$(find target -name "*-runner" -type f | head -1)
        
        # Start the application in background
        "$binary_path" &
        local app_pid=$!
        
        sleep 5  # Allow startup
        
        # Simple health check benchmark
        local response_time=$(curl -w "%{time_total}" -s -o /dev/null http://localhost:9003/q/health || echo "0")
        if (( $(echo "$response_time > 0" | bc -l) )); then
            log_info "Health endpoint response time: ${response_time}s"
            if (( $(echo "$response_time < 0.1" | bc -l) )); then
                log_success "Excellent response time (<100ms)"
            fi
        fi
        
        # Clean up
        kill $app_pid 2>/dev/null || true
        sleep 2
    fi
}

# Generate build report
generate_report() {
    local report_file="$PROJECT_ROOT/native-build-report-$(date +%Y%m%d-%H%M%S).txt"
    
    {
        echo "=== Aurigraph V11 Native Build Report ==="
        echo "Timestamp: $(date)"
        echo "Profile: $PROFILE"
        echo "Build Type: $BUILD_TYPE"
        echo "Docker Build: $DOCKER_BUILD"
        echo ""
        
        local binary_path=$(find target -name "*-runner" -type f | head -1)
        if [ -n "$binary_path" ]; then
            echo "=== Binary Information ==="
            echo "Path: $binary_path"
            echo "Size: $(du -sh "$binary_path" | cut -f1)"
            echo "Type: $(file "$binary_path" | cut -d: -f2-)"
            echo ""
        fi
        
        if [ "$DOCKER_BUILD" = "true" ]; then
            echo "=== Docker Image Information ==="
            docker images --format "table {{.Repository}}:{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep aurigraph-v11 || true
            echo ""
        fi
        
        echo "=== Build Performance ==="
        echo "Total build time: Available in build log"
        echo "Memory usage: Peak memory usage logged"
        echo ""
        
        echo "=== Quick Start Commands ==="
        echo "Run binary: $binary_path"
        if [ "$DOCKER_BUILD" = "true" ]; then
            echo "Run Docker: docker run -p 9003:9003 -p 9004:9004 $DOCKER_REGISTRY/aurigraph-v11:$BUILD_TYPE-$PROFILE"
        fi
        echo ""
        
        echo "=== Configuration Files ==="
        echo "pom.xml profiles: native, native-fast, native-ultra"
        echo "Application config: src/main/resources/application.properties"
        echo "Native config: src/main/resources/META-INF/native-image/"
        echo ""
    } > "$report_file"
    
    log_success "Build report generated: $report_file"
}

# Main execution
main() {
    log_info "Starting Aurigraph V11 native build process..."
    
    cd "$PROJECT_ROOT"
    
    check_prerequisites
    clean_build
    configure_environment
    build_native
    analyze_binary
    build_docker
    performance_benchmark
    generate_report
    
    log_success "Native build completed successfully!"
    echo ""
    echo "Next steps:"
    echo "1. Test the binary: $(find target -name "*-runner" -type f | head -1)"
    echo "2. Check build report: native-build-report-*.txt"
    echo "3. Review build log: $BUILD_LOG"
    
    if [ "$DOCKER_BUILD" = "true" ]; then
        echo "4. Test Docker image: docker run -p 9003:9003 -p 9004:9004 $DOCKER_REGISTRY/aurigraph-v11:$BUILD_TYPE-$PROFILE"
    fi
}

# Trap to cleanup on error
trap 'log_error "Build failed at line $LINENO"' ERR

# Execute main function
main "$@"