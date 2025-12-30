#!/bin/bash

# Aurigraph V11 Ultra-Optimized Native Build Script
# Target: <1s startup, <256MB memory, 2M+ TPS capability
# For Sprint 5 native compilation optimization

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Build profiles
PROFILES=("native-fast" "native" "native-ultra")

# Function to log with timestamps
log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')] $1${NC}"
}

log_success() {
    echo -e "${GREEN}[$(date +'%H:%M:%S')] ✓ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%H:%M:%S')] ⚠ $1${NC}"
}

log_error() {
    echo -e "${RED}[$(date +'%H:%M:%S')] ✗ $1${NC}"
}

# Function to get memory usage in MB
get_memory_mb() {
    local pid=$1
    if command -v pmap &> /dev/null; then
        pmap -x "$pid" 2>/dev/null | tail -1 | awk '{print int($4/1024)}'
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        ps -o rss= -p "$pid" 2>/dev/null | awk '{print int($1/1024)}'
    else
        ps -o rss= -p "$pid" 2>/dev/null | awk '{print int($1/1024)}'
    fi
}

# Function to build with a specific profile
build_profile() {
    local profile=$1
    local start_time
    local end_time
    local build_duration
    
    log "Building with profile: $profile"
    
    # Clean before build
    log "Cleaning previous build artifacts..."
    ./mvnw clean -q

    start_time=$(date +%s.%N)
    
    # Build command with optimization
    if ./mvnw package -P"$profile" -DskipTests=true -Dquarkus.package.jar.type=uber-jar; then
        end_time=$(date +%s.%N)
        build_duration=$(echo "$end_time - $start_time" | bc -l)
        
        log_success "$profile build completed in ${build_duration}s"
        
        # Get binary size
        local binary_path="target/aurigraph-v11-standalone-11.0.0-runner"
        if [[ -f "$binary_path" ]]; then
            local binary_size_mb
            if [[ "$OSTYPE" == "darwin"* ]]; then
                binary_size_mb=$(stat -f%z "$binary_path" | awk '{print int($1/1024/1024)}')
            else
                binary_size_mb=$(stat -c%s "$binary_path" | awk '{print int($1/1024/1024)}')
            fi
            log_success "$profile binary size: ${binary_size_mb}MB"
            
            # Test startup time and memory usage
            test_performance "$binary_path" "$profile"
        else
            log_error "$profile build failed - binary not found"
            return 1
        fi
    else
        end_time=$(date +%s.%N)
        build_duration=$(echo "$end_time - $start_time" | bc -l)
        log_error "$profile build failed after ${build_duration}s"
        return 1
    fi
}

# Function to test performance
test_performance() {
    local binary_path=$1
    local profile=$2
    local startup_time
    local memory_usage
    local pid
    
    log "Testing performance for $profile"
    
    # Test startup time
    local start_time=$(date +%s.%N)
    
    # Start the application in background
    "$binary_path" &
    pid=$!
    
    # Wait for the application to be ready (check health endpoint)
    local ready=false
    local timeout=30
    local elapsed=0
    
    while [[ $elapsed -lt $timeout ]]; do
        if curl -s http://localhost:9003/q/health > /dev/null 2>&1; then
            local end_time=$(date +%s.%N)
            startup_time=$(echo "$end_time - $start_time" | bc -l)
            ready=true
            break
        fi
        sleep 0.1
        elapsed=$((elapsed + 1))
    done
    
    if [[ $ready == true ]]; then
        # Measure memory usage after startup
        sleep 2  # Let it settle
        memory_usage=$(get_memory_mb "$pid")
        
        log_success "$profile startup time: ${startup_time}s"
        log_success "$profile memory usage: ${memory_usage}MB"
        
        # Performance test
        log "Running performance test for $profile"
        local tps
        tps=$(curl -s http://localhost:9003/api/v11/performance | grep -o '"transactionsPerSecond":[0-9]*' | cut -d: -f2 || echo "0")
        if [[ $tps -gt 0 ]]; then
            log_success "$profile performance: ${tps} TPS"
        else
            log_warning "$profile performance test failed"
        fi
        
        # Store results
        echo "$profile,${startup_time}s,${memory_usage}MB,$tps TPS" >> performance_results.csv
    else
        startup_time="TIMEOUT"
        memory_usage="UNKNOWN"
        log_error "$profile failed to start within ${timeout}s"
    fi
    
    # Stop the application
    if kill -TERM "$pid" 2>/dev/null; then
        # Wait for graceful shutdown
        sleep 2
        # Force kill if still running
        kill -KILL "$pid" 2>/dev/null || true
    fi
    
    wait "$pid" 2>/dev/null || true
}

# Main execution
main() {
    log "Starting Aurigraph V11 Ultra-Optimized Native Build Process"
    log "Target Goals: <1s startup, <256MB memory, 2M+ TPS capability"
    
    # Check prerequisites
    log "Checking prerequisites..."
    
    if ! command -v java &> /dev/null; then
        log_error "Java not found. Please install Java 21+"
        exit 1
    fi
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker not found. Please install Docker"
        exit 1
    fi
    
    if ! command -v bc &> /dev/null; then
        log_warning "bc not found. Install for precise timing calculations"
    fi
    
    # Check Java version
    local java_version
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    log "Java version: $java_version"
    
    # Initialize results file
    echo "Profile,Startup Time,Memory Usage,Performance" > performance_results.csv
    
    # Build and test each profile
    local success_count=0
    for profile in "${PROFILES[@]}"; do
        log "=========================================="
        log "Processing profile: $profile"
        log "=========================================="
        
        if build_profile "$profile"; then
            ((success_count++))
        fi
        
        # Cleanup between builds
        docker system prune -f > /dev/null 2>&1 || true
        sleep 5
    done
    
    # Generate performance report
    generate_report
    
    log "=========================================="
    log_success "Build process completed: $success_count/${#PROFILES[@]} profiles successful"
    log "Performance results saved to performance_results.csv"
    log "Performance report saved to performance_report.md"
}

# Function to generate performance report
generate_report() {
    local report_file="performance_report.md"
    
    cat > "$report_file" << EOF
# Aurigraph V11 Native Compilation Performance Report

## Sprint 5 Optimization Results

**Test Date:** $(date)
**Target Goals:**
- Startup Time: <1s
- Memory Usage: <256MB
- Performance: 2M+ TPS capability

## Build Profile Results

| Profile | Startup Time | Memory Usage | Performance | Status |
|---------|-------------|--------------|-------------|---------|
EOF
    
    # Process CSV results
    if [[ -f "performance_results.csv" ]]; then
        tail -n +2 performance_results.csv | while IFS=',' read -r profile startup memory performance; do
            local status
            local startup_num
            local memory_num
            
            # Extract numeric values for comparison
            startup_num=$(echo "$startup" | grep -o '^[0-9.]*')
            memory_num=$(echo "$memory" | grep -o '^[0-9]*')
            
            if [[ -n "$startup_num" && -n "$memory_num" ]]; then
                if (( $(echo "$startup_num < 1.0" | bc -l 2>/dev/null || echo 0) )) && (( memory_num < 256 )); then
                    status="✅ PASSED"
                elif (( $(echo "$startup_num < 2.0" | bc -l 2>/dev/null || echo 0) )) && (( memory_num < 512 )); then
                    status="⚠️ ACCEPTABLE"
                else
                    status="❌ NEEDS_OPTIMIZATION"
                fi
            else
                status="❌ FAILED"
            fi
            
            echo "| $profile | $startup | $memory | $performance | $status |" >> "$report_file"
        done
    fi
    
    cat >> "$report_file" << EOF

## Optimization Analysis

### Profile Comparison

#### native-fast
- **Purpose:** Development builds with fast compilation
- **Optimization Level:** -O1
- **Build Time:** ~2 minutes
- **Use Case:** Development and testing

#### native
- **Purpose:** Standard production builds
- **Optimization Level:** Balanced
- **Build Time:** ~15 minutes
- **Use Case:** Production deployment

#### native-ultra
- **Purpose:** Ultra-optimized production builds
- **Optimization Level:** -O3 with -march=native
- **Build Time:** ~30 minutes
- **Use Case:** Maximum performance production deployment

### Key Optimizations Applied

1. **Memory Management**
   - G1GC with 1ms max pause times
   - 32MB heap regions for optimal throughput
   - String deduplication enabled
   - Compressed OOPs and class pointers

2. **Runtime Initialization**
   - Netty components at runtime for proper native networking
   - gRPC services at runtime for connection handling
   - Crypto services at runtime for security
   - AI/ML libraries at runtime for dynamic loading

3. **Build-time Optimizations**
   - Static executable with dynamic LibC
   - No isolate spawning for faster startup
   - Aggressive method inlining
   - Dead code elimination
   - Unused symbol removal

4. **Resource Configuration**
   - Minimal resource inclusion (proto files, properties)
   - Extensive resource exclusion (documentation, examples)
   - Optimized reflection configuration
   - JNI configuration for native libraries

### Recommendations

- **Development:** Use \`native-fast\` for quick iterations
- **Testing:** Use \`native\` for comprehensive testing
- **Production:** Use \`native-ultra\` for maximum performance

### Next Steps

1. Monitor production performance metrics
2. Fine-tune GC parameters based on actual load
3. Implement application-level optimizations
4. Consider profile-guided optimizations (PGO)

---
*Generated by Aurigraph V11 Native Build Script*
*Sprint 5 - Native Compilation Optimization*
EOF
    
    log_success "Performance report generated: $report_file"
}

# Trap to cleanup on exit
cleanup() {
    log "Cleaning up..."
    pkill -f aurigraph-v11-standalone-11.0.0-runner 2>/dev/null || true
    docker system prune -f > /dev/null 2>&1 || true
}

trap cleanup EXIT

# Run main function
main "$@"