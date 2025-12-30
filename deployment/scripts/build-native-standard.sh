#!/bin/bash
# Aurigraph V11 Standard Native Build Script
# Balanced optimization - 12 min build, <800ms startup, ~75MB

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_LOG="$SCRIPT_DIR/build-native-standard.log"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[STANDARD-BUILD]${NC} $1" | tee -a "$BUILD_LOG"
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

echo "=== Aurigraph V11 Standard Native Build Started at $(date) ===" > "$BUILD_LOG"

# Pre-build checks
log_info "Starting standard native build (balanced optimization)..."
log_info "Target: <800ms startup, ~75MB binary, optimal for most use cases"

# Check prerequisites
check_prerequisites() {
    log_info "Checking build prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker is required for container-based native builds"
        exit 1
    fi
    
    # Check available memory (minimum 4GB for standard build)
    AVAILABLE_MEMORY=$(free -m | awk 'NR==2{printf "%d", $7}' 2>/dev/null || echo "4096")
    if [ "$AVAILABLE_MEMORY" -lt 3072 ]; then
        log_error "Standard build requires at least 3GB available RAM (found: ${AVAILABLE_MEMORY}MB)"
        exit 1
    fi
    
    # Check CPU cores
    CPU_CORES=$(nproc 2>/dev/null || echo "4")
    log_info "System: ${CPU_CORES} CPU cores, ${AVAILABLE_MEMORY}MB available RAM"
    
    log_success "Prerequisites check passed"
}

# Configure standard build environment
configure_environment() {
    log_info "Configuring standard build environment..."
    
    # Optimal memory allocation for standard builds
    local maven_heap=$((AVAILABLE_MEMORY > 6144 ? 6144 : AVAILABLE_MEMORY - 1024))
    
    export MAVEN_OPTS="-Xmx${maven_heap}m -XX:+UseG1GC -XX:+UseStringDeduplication"
    export DOCKER_BUILDKIT=1
    
    log_info "Maven heap configured: ${maven_heap}MB"
    log_success "Environment configured for balanced performance"
}

# Execute standard build
execute_build() {
    log_info "Executing standard native build..."
    
    # Clean previous builds
    log_info "Cleaning previous builds..."
    ./mvnw clean -q
    rm -rf target/native-image-*
    
    # Record build timing
    local start_time=$(date +%s)
    local start_timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    log_info "Build started at: $start_timestamp"
    log_info "Expected duration: 8-15 minutes"
    
    # Execute standard build
    if ./mvnw package -Pnative \
        -Dmaven.test.skip=true \
        -Dquarkus.native.native-image-xmx=8g \
        -Dquarkus.native.container-build=true \
        -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:24-java21 \
        --no-transfer-progress \
        -B >> "$BUILD_LOG" 2>&1; then
        
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        local minutes=$((duration / 60))
        local seconds=$((duration % 60))
        
        log_success "Standard build completed in ${minutes}m ${seconds}s"
        
    else
        log_error "Build failed. Check $BUILD_LOG for details"
        return 1
    fi
}

# Analyze build results
analyze_binary() {
    log_info "Analyzing standard native binary..."
    
    local binary_path=$(find target -name "*-runner" -type f | head -1)
    if [ -z "$binary_path" ]; then
        log_error "Native binary not found"
        return 1
    fi
    
    # Binary analysis
    local binary_size=$(du -sh "$binary_path" | cut -f1)
    local binary_size_bytes=$(du -b "$binary_path" | cut -f1)
    local binary_size_mb=$((binary_size_bytes / 1024 / 1024))
    
    log_info "Binary path: $binary_path"
    log_info "Binary size: $binary_size (${binary_size_mb}MB)"
    
    # Binary type
    local binary_type=$(file "$binary_path" | cut -d: -f2-)
    log_info "Binary type: $binary_type"
    
    # Check linking
    if ldd "$binary_path" 2>/dev/null | grep -q "not a dynamic executable"; then
        log_success "Static binary created (good for containers)"
    else
        log_info "Dynamic binary created. Key dependencies:"
        ldd "$binary_path" 2>/dev/null | head -5 || true
    fi
    
    # Startup performance test
    log_info "Testing startup performance..."
    local startup_test_start=$(date +%s%3N)
    
    if timeout 20s "$binary_path" --help >/dev/null 2>&1; then
        local startup_test_end=$(date +%s%3N)
        local startup_time=$((startup_test_end - startup_test_start))
        log_info "Startup test time: ${startup_time}ms"
        
        if [ "$startup_time" -lt 800 ]; then
            log_success "Excellent startup performance: ${startup_time}ms < 800ms target ✓"
        elif [ "$startup_time" -lt 1200 ]; then
            log_success "Good startup performance: ${startup_time}ms < 1200ms"
        else
            log_warning "Startup time ${startup_time}ms higher than expected"
        fi
    else
        log_warning "Startup test timed out or failed"
    fi
    
    # Size evaluation
    if [ "$binary_size_mb" -lt 80 ]; then
        log_success "Excellent binary size: ${binary_size_mb}MB < 80MB target ✓"
    elif [ "$binary_size_mb" -lt 100 ]; then
        log_success "Good binary size: ${binary_size_mb}MB < 100MB"
    else
        log_warning "Binary size ${binary_size_mb}MB larger than expected"
    fi
    
    return 0
}

# Generate build report
generate_report() {
    local report_file="$SCRIPT_DIR/standard-build-report-$(date +%Y%m%d-%H%M%S).txt"
    local binary_path=$(find target -name "*-runner" -type f | head -1)
    
    {
        echo "═══════════════════════════════════════════════════════════════"
        echo "    AURIGRAPH V11 STANDARD NATIVE BUILD REPORT"
        echo "═══════════════════════════════════════════════════════════════"
        echo "Build completed: $(date '+%Y-%m-%d %H:%M:%S')"
        echo "Profile: native (standard optimized)"
        echo ""
        
        if [ -n "$binary_path" ]; then
            echo "📦 Binary Information"
            echo "─────────────────────────────────────────────────────────────"
            echo "Path: $binary_path"
            echo "Size: $(du -sh "$binary_path" | cut -f1)"
            echo "Size (MB): $(($(du -b "$binary_path" | cut -f1) / 1024 / 1024))"
            echo "Type: $(file "$binary_path" | cut -d: -f2-)"
            echo ""
        fi
        
        echo "🎯 Optimization Targets"
        echo "─────────────────────────────────────────────────────────────"
        echo "Startup time: <800ms"
        echo "Binary size: <75MB"
        echo "Memory usage: <256MB"
        echo "Build time: 8-15 minutes"
        echo ""
        
        echo "⚙️ Build Configuration"
        echo "─────────────────────────────────────────────────────────────"
        echo "Profile: native (standard)"
        echo "Optimization level: -O2"
        echo "GC: G1 with optimized settings"
        echo "Linking: Static with dynamic LibC"
        echo "Compression: Level 8 with debug stripping"
        echo ""
        
        echo "💻 System Information"
        echo "─────────────────────────────────────────────────────────────"
        echo "CPU cores: $CPU_CORES"
        echo "Available memory: ${AVAILABLE_MEMORY}MB"
        echo "Maven configuration: $(echo $MAVEN_OPTS | tr ' ' '\n' | grep Xmx)"
        echo ""
        
        echo "🚀 Usage Instructions"
        echo "─────────────────────────────────────────────────────────────"
        echo "Run binary: $binary_path"
        echo "Health check: curl http://localhost:9003/q/health"
        echo "API endpoint: curl http://localhost:9003/api/v11/info"
        echo "Metrics: curl http://localhost:9003/q/metrics"
        echo ""
        
        echo "📁 Generated Files"
        echo "─────────────────────────────────────────────────────────────"
        echo "Native binary: $binary_path"
        echo "Build log: $BUILD_LOG"
        echo "Build report: $report_file"
        echo ""
        
    } > "$report_file"
    
    log_success "Build report generated: $report_file"
}

# Main execution
main() {
    cd "$SCRIPT_DIR"
    
    check_prerequisites
    configure_environment
    execute_build
    analyze_binary
    generate_report
    
    local binary_path=$(find target -name "*-runner" -type f | head -1)
    
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "✅ AURIGRAPH V11 STANDARD BUILD COMPLETED!"
    echo "═══════════════════════════════════════════════════════════════"
    echo "📦 Binary: $binary_path"
    echo "📊 Size: $(du -sh "$binary_path" | cut -f1)"
    echo "🏃 Run: $binary_path"
    echo "📋 Report: standard-build-report-*.txt"
    echo "📝 Log: $BUILD_LOG"
    echo ""
    echo "🎯 Balanced optimization complete!"
    echo "   Target: <800ms startup, ~75MB binary, production-ready"
    echo "═══════════════════════════════════════════════════════════════"
}

# Trap to cleanup on error
trap 'log_error "Standard build failed at line $LINENO"' ERR

# Execute main function
main "$@"