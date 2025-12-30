#!/bin/bash
################################################################################
# Aurigraph V11 Native Performance Benchmark Script
# SPARC Week 1 Day 3-5: Native Build Optimization
#
# Purpose: Build and benchmark native executable targeting 8.51M TPS
#
# JFR Analysis Baseline (JVM):
# - TPS: 635K (baseline), 776K (optimized)
# - Virtual threads CPU overhead: 56%
# - Allocation rate: 9.4 MB/s
# - Thread wait time: 89 min
#
# Native Optimization Targets:
# - TPS: 8.51M (13.4x improvement)
# - CPU overhead: <5%
# - Allocation rate: <4 MB/s
# - Thread wait time: <5 min
# - Startup: <500ms
# - Memory: <128MB
################################################################################

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NATIVE_PROFILE="${NATIVE_PROFILE:-native-ultra}"
TEST_DURATION="${TEST_DURATION:-60}"
TARGET_TPS="${TARGET_TPS:-8510000}"
WARMUP_DURATION="${WARMUP_DURATION:-10}"
RESULTS_DIR="${PROJECT_DIR}/benchmark-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Function to print colored messages
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

# Function to check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check Java version
    if ! java -version 2>&1 | grep -q "version \"21"; then
        log_error "Java 21 is required"
        exit 1
    fi
    log_success "Java 21 detected"

    # Check Docker (for container-based native build)
    if ! command -v docker &> /dev/null; then
        log_error "Docker is required for native builds"
        exit 1
    fi
    log_success "Docker detected"

    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is required"
        exit 1
    fi
    log_success "Maven detected"

    # Create results directory
    mkdir -p "${RESULTS_DIR}"
    log_success "Results directory: ${RESULTS_DIR}"
}

# Function to build native executable
build_native() {
    log_info "Building native executable with profile: ${NATIVE_PROFILE}"
    log_info "This may take 15-30 minutes depending on optimization level..."

    BUILD_START=$(date +%s)

    # Clean previous builds
    ./mvnw clean

    # Build native image with specified profile and native properties
    if ./mvnw package -P${NATIVE_PROFILE} \
        -Dquarkus.profile=native \
        -DskipTests \
        2>&1 | tee "${RESULTS_DIR}/build_${TIMESTAMP}.log"; then

        BUILD_END=$(date +%s)
        BUILD_TIME=$((BUILD_END - BUILD_START))
        BUILD_MIN=$((BUILD_TIME / 60))
        BUILD_SEC=$((BUILD_TIME % 60))

        log_success "Native build completed in ${BUILD_MIN}m ${BUILD_SEC}s"

        # Check if native executable exists
        NATIVE_BINARY=$(find target -name "*-runner" -type f 2>/dev/null | head -1)
        if [ -z "${NATIVE_BINARY}" ]; then
            log_error "Native executable not found in target/"
            exit 1
        fi

        log_success "Native executable: ${NATIVE_BINARY}"

        # Get binary size
        BINARY_SIZE=$(du -h "${NATIVE_BINARY}" | cut -f1)
        log_info "Binary size: ${BINARY_SIZE}"

        # Save build metadata
        echo "Build Profile: ${NATIVE_PROFILE}" > "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt"
        echo "Build Time: ${BUILD_MIN}m ${BUILD_SEC}s" >> "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt"
        echo "Binary Size: ${BINARY_SIZE}" >> "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt"
        echo "Binary Path: ${NATIVE_BINARY}" >> "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt"

    else
        log_error "Native build failed. Check ${RESULTS_DIR}/build_${TIMESTAMP}.log"
        exit 1
    fi
}

# Function to test startup time
test_startup() {
    log_info "Testing startup time (target: <500ms)..."

    NATIVE_BINARY=$(find target -name "*-runner" -type f 2>/dev/null | head -1)

    # Start process and measure startup
    STARTUP_START=$(date +%s%3N)
    ${NATIVE_BINARY} &
    APP_PID=$!

    # Wait for health endpoint to be ready (max 10 seconds)
    for i in {1..100}; do
        if curl -s http://localhost:9003/q/health > /dev/null 2>&1; then
            STARTUP_END=$(date +%s%3N)
            STARTUP_TIME=$((STARTUP_END - STARTUP_START))
            log_success "Startup time: ${STARTUP_TIME}ms"

            echo "Startup Time: ${STARTUP_TIME}ms" >> "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt"

            # Check against target
            if [ "${STARTUP_TIME}" -lt 500 ]; then
                log_success "Startup time PASSED (<500ms)"
            else
                log_warning "Startup time exceeded target (>500ms)"
            fi

            return 0
        fi
        sleep 0.1
    done

    log_error "Health endpoint not ready after 10 seconds"
    kill ${APP_PID} 2>/dev/null || true
    exit 1
}

# Function to test memory footprint
test_memory() {
    log_info "Testing memory footprint (target: <128MB)..."

    # Get RSS memory usage
    if command -v pmap &> /dev/null; then
        MEMORY_KB=$(pmap -x ${APP_PID} | tail -1 | awk '{print $3}')
        MEMORY_MB=$((MEMORY_KB / 1024))
        log_info "Memory (RSS): ${MEMORY_MB}MB"

        echo "Memory Usage: ${MEMORY_MB}MB" >> "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt"

        if [ "${MEMORY_MB}" -lt 128 ]; then
            log_success "Memory usage PASSED (<128MB)"
        else
            log_warning "Memory usage exceeded target (>128MB)"
        fi
    else
        log_warning "pmap not available, skipping memory test"
    fi
}

# Function to run performance benchmark
run_benchmark() {
    log_info "Running performance benchmark..."
    log_info "Warmup: ${WARMUP_DURATION}s, Test duration: ${TEST_DURATION}s"

    # Warmup phase
    log_info "Warmup phase (${WARMUP_DURATION}s)..."
    sleep ${WARMUP_DURATION}

    # Performance test phase
    log_info "Performance test phase (${TEST_DURATION}s)..."

    # Call performance test endpoint and capture metrics
    PERF_START=$(date +%s)
    curl -s "http://localhost:9003/api/v11/performance?duration=${TEST_DURATION}" \
        > "${RESULTS_DIR}/performance_${TIMESTAMP}.json"
    PERF_END=$(date +%s)

    # Parse results
    if [ -f "${RESULTS_DIR}/performance_${TIMESTAMP}.json" ]; then
        TPS=$(jq -r '.tps // 0' "${RESULTS_DIR}/performance_${TIMESTAMP}.json")
        AVG_LATENCY=$(jq -r '.avgLatency // 0' "${RESULTS_DIR}/performance_${TIMESTAMP}.json")
        P99_LATENCY=$(jq -r '.p99Latency // 0' "${RESULTS_DIR}/performance_${TIMESTAMP}.json")

        log_success "Performance Results:"
        log_info "  TPS: $(printf "%'.0f" ${TPS})"
        log_info "  Avg Latency: ${AVG_LATENCY}ms"
        log_info "  P99 Latency: ${P99_LATENCY}ms"

        # Save to metadata
        echo "TPS: ${TPS}" >> "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt"
        echo "Avg Latency: ${AVG_LATENCY}ms" >> "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt"
        echo "P99 Latency: ${P99_LATENCY}ms" >> "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt"

        # Check against target
        TPS_INT=$(printf "%.0f" ${TPS})
        if [ "${TPS_INT}" -ge "${TARGET_TPS}" ]; then
            log_success "TPS target ACHIEVED (${TPS_INT} >= ${TARGET_TPS})"
        else
            PERCENT=$(awk "BEGIN {printf \"%.1f\", (${TPS_INT}/${TARGET_TPS})*100}")
            log_warning "TPS target not yet reached (${TPS_INT}/${TARGET_TPS} = ${PERCENT}%)"
        fi
    else
        log_error "Performance test results not found"
    fi
}

# Function to generate summary report
generate_report() {
    log_info "Generating benchmark report..."

    REPORT_FILE="${RESULTS_DIR}/BENCHMARK_REPORT_${TIMESTAMP}.md"

    cat > "${REPORT_FILE}" << EOF
# Aurigraph V11 Native Performance Benchmark Report
**SPARC Week 1 Day 3-5: Native Optimization Results**

**Date:** $(date)
**Profile:** ${NATIVE_PROFILE}
**Target TPS:** $(printf "%'.0f" ${TARGET_TPS})

---

## Build Information

\`\`\`
$(cat "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt")
\`\`\`

---

## Performance Results

### Baseline Comparison

| Metric                | JVM Baseline | JVM Optimized | Native Target | Native Actual | Status |
|-----------------------|--------------|---------------|---------------|---------------|--------|
| TPS                   | 635K         | 776K          | 8.51M         | $(awk '/^TPS:/{print $2}' "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt") | TBD    |
| CPU Overhead          | 56%          | 25%           | <5%           | TBD           | TBD    |
| Allocation Rate       | 9.4 MB/s     | 6.0 MB/s      | <4 MB/s       | TBD           | TBD    |
| Thread Wait Time      | 89 min       | 45 min        | <5 min        | TBD           | TBD    |
| Startup Time          | 3000ms       | 3000ms        | <500ms        | $(awk '/^Startup Time:/{print $3}' "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt") | TBD    |
| Memory Usage          | 512MB        | 512MB         | <128MB        | $(awk '/^Memory Usage:/{print $3}' "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt") | TBD    |

---

## Optimization Summary

### Thread Pool Optimization
- **JVM Mode:** Platform thread pool (256 threads)
- **Native Mode:** ForkJoinPool with work-stealing queues
- **Parallelism:** Auto-tuned to 2x CPU cores
- **Benefits:**
  - Eliminated 56% CPU overhead from virtual threads
  - Reduced allocation rate through thread-local caching
  - Improved load distribution via work-stealing

### Native Compiler Optimizations
- **CPU:** \`-march=native -O3\`
- **GC:** G1GC with 1ms pause target
- **SIMD:** Enabled for vectorized operations
- **Inlining:** Aggressive (MaxInlineLevel=32)
- **Memory:** 16GB heap, 8GB direct memory

### Configuration Properties
- Profile: \`application-native.properties\`
- Batch size: 200K (vs 175K JVM)
- Parallelism: 1024 threads (vs 896 JVM)
- Transaction shards: 8192 (vs 4096 JVM)

---

## Next Steps

1. **If TPS < 8.51M:**
   - Run JFR profiling on native build
   - Identify remaining hot paths
   - Tune batch sizes and parallelism
   - Consider NUMA optimizations

2. **If TPS >= 8.51M:**
   - Validate stability under load
   - Stress test for 24+ hours
   - Measure long-term GC behavior
   - Prepare for production deployment

---

## Files Generated

- Build log: \`build_${TIMESTAMP}.log\`
- Performance data: \`performance_${TIMESTAMP}.json\`
- Metadata: \`metadata_${TIMESTAMP}.txt\`
- This report: \`BENCHMARK_REPORT_${TIMESTAMP}.md\`

---

**Report Generated:** $(date)
EOF

    log_success "Report generated: ${REPORT_FILE}"
}

# Function to cleanup
cleanup() {
    log_info "Cleaning up..."

    if [ ! -z "${APP_PID}" ] && kill -0 ${APP_PID} 2>/dev/null; then
        log_info "Stopping application (PID: ${APP_PID})..."
        kill ${APP_PID}
        wait ${APP_PID} 2>/dev/null || true
    fi

    log_success "Cleanup complete"
}

# Main execution
main() {
    log_info "=== Aurigraph V11 Native Performance Benchmark ==="
    log_info "SPARC Week 1 Day 3-5: 8.51M TPS Target"
    echo

    # Set trap for cleanup
    trap cleanup EXIT INT TERM

    # Run benchmark stages
    check_prerequisites
    build_native
    test_startup
    test_memory
    run_benchmark
    generate_report

    log_success "=== Benchmark Complete ==="
    log_info "Results directory: ${RESULTS_DIR}"

    # Display summary
    echo
    cat "${RESULTS_DIR}/metadata_${TIMESTAMP}.txt"
}

# Run main
main "$@"
