#!/bin/bash
# Aurigraph V11 Fast Native Build Script
# Optimized for rapid development cycles - 2 min build, <1s startup, ~60MB

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_LOG="$SCRIPT_DIR/build-native-fast.log"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[FAST-BUILD]${NC} $1" | tee -a "$BUILD_LOG"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$BUILD_LOG"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$BUILD_LOG"
}

echo "=== Aurigraph V11 Fast Native Build Started at $(date) ===" > "$BUILD_LOG"

# Pre-build checks
log_info "Starting fast native build for development..."

# Check prerequisites
if ! command -v docker &> /dev/null; then
    log_error "Docker is required for container-based native builds"
    exit 1
fi

# Check available memory
AVAILABLE_MEMORY=$(free -m | awk 'NR==2{printf "%d", $7}' 2>/dev/null || echo "4096")
if [ "$AVAILABLE_MEMORY" -lt 2048 ]; then
    log_error "At least 2GB RAM required for fast native build"
    exit 1
fi

# Set optimal environment
export MAVEN_OPTS="-Xmx4g -XX:+UseG1GC -XX:+UseStringDeduplication"
export DOCKER_BUILDKIT=1

log_info "Environment: MAVEN_OPTS=$MAVEN_OPTS"
log_info "Target: <1s startup, ~60MB binary size"

# Clean previous build
log_info "Cleaning previous builds..."
./mvnw clean -q
rm -rf target/native-image-*

# Fast native build
log_info "Building native image with fast profile..."
start_time=$(date +%s)

if ./mvnw package -Pnative-fast \
    -Dmaven.test.skip=true \
    -Dquarkus.native.native-image-xmx=4g \
    -Dquarkus.native.container-build=true \
    -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:24-java21 \
    --no-transfer-progress \
    -B >> "$BUILD_LOG" 2>&1; then
    
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    log_success "Fast native build completed in ${duration}s"
else
    log_error "Build failed. Check $BUILD_LOG for details"
    exit 1
fi

# Analyze results
BINARY=$(find target -name "*-runner" -type f | head -1)
if [ -n "$BINARY" ]; then
    BINARY_SIZE=$(du -sh "$BINARY" | cut -f1)
    log_success "Binary created: $BINARY"
    log_info "Binary size: $BINARY_SIZE"
    
    # Test binary startup
    log_info "Testing binary startup..."
    if timeout 10s "$BINARY" --help >/dev/null 2>&1; then
        log_success "Binary startup test passed"
    else
        log_error "Binary startup test failed"
    fi
    
    # Quick performance estimate
    log_info "Binary type: $(file "$BINARY" | cut -d: -f2-)"
    
    echo ""
    log_success "Fast native build complete!"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📦 Binary: $BINARY"
    echo "📊 Size: $BINARY_SIZE"
    echo "⏱️ Build time: ${duration}s"
    echo "🏃 Run with: $BINARY"
    echo "📋 Build log: $BUILD_LOG"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
    log_error "Binary not found after successful build"
    exit 1
fi