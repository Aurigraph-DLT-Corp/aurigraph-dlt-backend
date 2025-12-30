#!/bin/bash
# Aurigraph V11 Performance Benchmark Script
# Comprehensive testing of native vs JVM performance
# 
# This script validates:
# - <1s startup time requirement
# - 2M+ TPS performance target
# - <100MB memory footprint
# - Response time and throughput comparisons

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BENCHMARK_LOG="$SCRIPT_DIR/performance-benchmark-$(date +%Y%m%d-%H%M%S).log"
RESULTS_DIR="$SCRIPT_DIR/benchmark-results"
NATIVE_BINARY=""
JVM_JAR=""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

# Performance targets
TARGET_STARTUP_TIME=1.0    # seconds
TARGET_MEMORY_MB=100       # MB
TARGET_TPS=2000000         # transactions per second
TARGET_RESPONSE_TIME=0.1   # seconds

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$BENCHMARK_LOG"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$BENCHMARK_LOG"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$BENCHMARK_LOG"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$BENCHMARK_LOG"
}

log_metric() {
    echo -e "${BOLD}[METRIC]${NC} $1" | tee -a "$BENCHMARK_LOG"
}

# Initialize benchmark
initialize_benchmark() {
    log_info "Initializing Aurigraph V11 Performance Benchmark..."
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    
    # Find binaries
    NATIVE_BINARY=$(find target -name "*-runner" -type f 2>/dev/null | head -1)
    JVM_JAR=$(find target -name "*.jar" -type f ! -name "*-runner*" 2>/dev/null | head -1)
    
    if [ -z "$NATIVE_BINARY" ]; then
        log_error "Native binary not found. Run ./build-native.sh first"
        exit 1
    fi
    
    if [ -z "$JVM_JAR" ]; then
        log_warning "JVM JAR not found. Building..."
        ./mvnw package -Dmaven.test.skip=true -q
        JVM_JAR=$(find target -name "*.jar" -type f ! -name "*-runner*" | head -1)
    fi
    
    log_info "Native binary: $NATIVE_BINARY"
    log_info "JVM JAR: $JVM_JAR"
    
    # Check dependencies
    command -v curl >/dev/null 2>&1 || { log_error "curl is required"; exit 1; }
    command -v bc >/dev/null 2>&1 || { log_error "bc is required"; exit 1; }
    
    log_success "Benchmark initialization completed"
}

# Measure startup time
measure_startup_time() {
    local mode="$1"
    local binary="$2"
    local iterations=5
    local total_time=0
    
    log_info "Measuring startup time for $mode mode..."
    
    for i in $(seq 1 $iterations); do
        log_info "Startup test $i/$iterations for $mode..."
        
        local start_time=$(date +%s.%N)
        
        if [ "$mode" = "native" ]; then
            timeout 30s "$binary" &
        else
            timeout 30s java -jar "$binary" &
        fi
        
        local pid=$!
        local startup_time=0
        
        # Wait for application to be ready (check health endpoint)
        for wait_time in {1..30}; do
            if curl -s http://localhost:9003/q/health >/dev/null 2>&1; then
                local end_time=$(date +%s.%N)
                startup_time=$(echo "$end_time - $start_time" | bc)
                break
            fi
            sleep 0.1
        done
        
        # Cleanup
        kill $pid 2>/dev/null || true
        sleep 2
        
        if (( $(echo "$startup_time > 0" | bc -l) )); then
            total_time=$(echo "$total_time + $startup_time" | bc)
            log_info "$mode startup time (iteration $i): ${startup_time}s"
        else
            log_error "$mode startup failed in iteration $i"
            return 1
        fi
    done
    
    local avg_startup=$(echo "scale=3; $total_time / $iterations" | bc)
    log_metric "$mode average startup time: ${avg_startup}s"
    
    # Check against target
    if (( $(echo "$avg_startup < $TARGET_STARTUP_TIME" | bc -l) )); then
        log_success "$mode startup time meets target (<${TARGET_STARTUP_TIME}s)"
    else
        log_warning "$mode startup time exceeds target (${avg_startup}s > ${TARGET_STARTUP_TIME}s)"
    fi
    
    echo "$avg_startup" > "$RESULTS_DIR/startup_time_$mode.txt"
}

# Measure memory usage
measure_memory_usage() {
    local mode="$1"
    local binary="$2"
    
    log_info "Measuring memory usage for $mode mode..."
    
    # Start application
    if [ "$mode" = "native" ]; then
        "$binary" &
    else
        java -Xmx512m -jar "$binary" &
    fi
    
    local pid=$!
    sleep 5  # Allow startup
    
    # Wait for health check
    local max_wait=30
    for i in $(seq 1 $max_wait); do
        if curl -s http://localhost:9003/q/health >/dev/null 2>&1; then
            break
        fi
        sleep 1
        if [ $i -eq $max_wait ]; then
            log_error "$mode failed to start for memory test"
            kill $pid 2>/dev/null || true
            return 1
        fi
    done
    
    # Measure memory usage over time
    local memory_samples=()
    for i in {1..10}; do
        if ps -p $pid >/dev/null 2>&1; then
            local rss=$(ps -o rss= -p $pid | tr -d ' ')
            local memory_mb=$(echo "scale=1; $rss / 1024" | bc)
            memory_samples+=($memory_mb)
            log_info "$mode memory sample $i: ${memory_mb}MB"
        else
            log_error "$mode process died during memory measurement"
            return 1
        fi
        sleep 2
    done
    
    # Calculate average memory usage
    local total_memory=0
    for mem in "${memory_samples[@]}"; do
        total_memory=$(echo "$total_memory + $mem" | bc)
    done
    local avg_memory=$(echo "scale=1; $total_memory / ${#memory_samples[@]}" | bc)
    
    log_metric "$mode average memory usage: ${avg_memory}MB"
    
    # Check against target
    if (( $(echo "$avg_memory < $TARGET_MEMORY_MB" | bc -l) )); then
        log_success "$mode memory usage meets target (<${TARGET_MEMORY_MB}MB)"
    else
        log_warning "$mode memory usage exceeds target (${avg_memory}MB > ${TARGET_MEMORY_MB}MB)"
    fi
    
    # Cleanup
    kill $pid 2>/dev/null || true
    sleep 2
    
    echo "$avg_memory" > "$RESULTS_DIR/memory_usage_$mode.txt"
}

# Measure response time
measure_response_time() {
    local mode="$1"
    local binary="$2"
    
    log_info "Measuring response time for $mode mode..."
    
    # Start application
    if [ "$mode" = "native" ]; then
        "$binary" &
    else
        java -jar "$binary" &
    fi
    
    local pid=$!
    sleep 5  # Allow startup
    
    # Wait for health check
    for i in {1..30}; do
        if curl -s http://localhost:9003/q/health >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    
    # Test various endpoints
    local endpoints=(
        "/q/health"
        "/api/v11/transactions/status"
        "/api/v11/consensus/state"
        "/q/metrics"
    )
    
    local total_response_time=0
    local successful_requests=0
    
    for endpoint in "${endpoints[@]}"; do
        log_info "Testing endpoint: $endpoint"
        
        for i in {1..5}; do
            local response_time=$(curl -w "%{time_total}" -s -o /dev/null "http://localhost:9003$endpoint" || echo "0")
            if (( $(echo "$response_time > 0" | bc -l) )); then
                total_response_time=$(echo "$total_response_time + $response_time" | bc)
                successful_requests=$((successful_requests + 1))
                log_info "Response time: ${response_time}s"
            else
                log_warning "Request to $endpoint failed"
            fi
        done
    done
    
    if [ $successful_requests -gt 0 ]; then
        local avg_response_time=$(echo "scale=4; $total_response_time / $successful_requests" | bc)
        log_metric "$mode average response time: ${avg_response_time}s"
        
        # Check against target
        if (( $(echo "$avg_response_time < $TARGET_RESPONSE_TIME" | bc -l) )); then
            log_success "$mode response time meets target (<${TARGET_RESPONSE_TIME}s)"
        else
            log_warning "$mode response time exceeds target (${avg_response_time}s > ${TARGET_RESPONSE_TIME}s)"
        fi
        
        echo "$avg_response_time" > "$RESULTS_DIR/response_time_$mode.txt"
    else
        log_error "No successful requests for $mode"
    fi
    
    # Cleanup
    kill $pid 2>/dev/null || true
    sleep 2
}

# Stress test for TPS measurement
stress_test_tps() {
    local mode="$1"
    local binary="$2"
    
    log_info "Running TPS stress test for $mode mode..."
    
    # Start application with production settings
    if [ "$mode" = "native" ]; then
        QUARKUS_PROFILE=prod "$binary" &
    else
        QUARKUS_PROFILE=prod java -Xmx2g -XX:+UseG1GC -jar "$binary" &
    fi
    
    local pid=$!
    sleep 10  # Allow startup and warmup
    
    # Wait for health check
    for i in {1..30}; do
        if curl -s http://localhost:9003/q/health >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    
    # Simulate high-frequency transaction requests
    log_info "Starting TPS measurement (30-second test)..."
    
    local start_time=$(date +%s)
    local request_count=0
    local success_count=0
    
    # Run concurrent requests for 30 seconds
    for i in {1..50}; do  # 50 concurrent workers
        (
            local worker_requests=0
            local worker_success=0
            while [ $(($(date +%s) - start_time)) -lt 30 ]; do
                if curl -s -X POST "http://localhost:9003/api/v11/transactions/simulate" \
                   -H "Content-Type: application/json" \
                   -d '{"count":100,"type":"high_frequency"}' >/dev/null 2>&1; then
                    worker_success=$((worker_success + 100))
                fi
                worker_requests=$((worker_requests + 100))
                usleep 1000  # 1ms delay
            done
            echo "$worker_requests $worker_success" > "$RESULTS_DIR/worker_$i.tmp"
        ) &
    done
    
    wait  # Wait for all workers to complete
    
    # Calculate total TPS
    for i in {1..50}; do
        if [ -f "$RESULTS_DIR/worker_$i.tmp" ]; then
            local worker_data=$(cat "$RESULTS_DIR/worker_$i.tmp")
            local worker_requests=$(echo $worker_data | cut -d' ' -f1)
            local worker_success=$(echo $worker_data | cut -d' ' -f2)
            request_count=$((request_count + worker_requests))
            success_count=$((success_count + worker_success))
            rm "$RESULTS_DIR/worker_$i.tmp"
        fi
    done
    
    local actual_duration=$(($(date +%s) - start_time))
    local tps=$(echo "scale=0; $success_count / $actual_duration" | bc)
    
    log_metric "$mode TPS achieved: $tps (${success_count} successful transactions in ${actual_duration}s)"
    
    # Check against target
    if [ $tps -gt $TARGET_TPS ]; then
        log_success "$mode TPS exceeds target ($tps > $TARGET_TPS)"
    else
        log_warning "$mode TPS below target ($tps < $TARGET_TPS)"
    fi
    
    echo "$tps" > "$RESULTS_DIR/tps_$mode.txt"
    
    # Cleanup
    kill $pid 2>/dev/null || true
    sleep 2
}

# Generate comprehensive report
generate_benchmark_report() {
    local report_file="$RESULTS_DIR/benchmark-report-$(date +%Y%m%d-%H%M%S).html"
    
    log_info "Generating comprehensive benchmark report..."
    
    # Read results
    local native_startup=$(cat "$RESULTS_DIR/startup_time_native.txt" 2>/dev/null || echo "N/A")
    local jvm_startup=$(cat "$RESULTS_DIR/startup_time_jvm.txt" 2>/dev/null || echo "N/A")
    local native_memory=$(cat "$RESULTS_DIR/memory_usage_native.txt" 2>/dev/null || echo "N/A")
    local jvm_memory=$(cat "$RESULTS_DIR/memory_usage_jvm.txt" 2>/dev/null || echo "N/A")
    local native_response=$(cat "$RESULTS_DIR/response_time_native.txt" 2>/dev/null || echo "N/A")
    local jvm_response=$(cat "$RESULTS_DIR/response_time_jvm.txt" 2>/dev/null || echo "N/A")
    local native_tps=$(cat "$RESULTS_DIR/tps_native.txt" 2>/dev/null || echo "N/A")
    local jvm_tps=$(cat "$RESULTS_DIR/tps_jvm.txt" 2>/dev/null || echo "N/A")
    
    # Create HTML report
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Aurigraph V11 Performance Benchmark Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .header { background: #2c3e50; color: white; padding: 20px; text-align: center; }
        .metric { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .success { background-color: #d4edda; border-color: #c3e6cb; }
        .warning { background-color: #fff3cd; border-color: #ffeaa7; }
        .error { background-color: #f8d7da; border-color: #f5c6cb; }
        .comparison { display: flex; gap: 20px; }
        .native { background-color: #e8f5e8; }
        .jvm { background-color: #e8f0ff; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { padding: 12px; border: 1px solid #ddd; text-align: left; }
        th { background-color: #f8f9fa; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Aurigraph V11 Performance Benchmark Report</h1>
        <p>Generated: $(date)</p>
    </div>
    
    <h2>Executive Summary</h2>
    <table>
        <tr><th>Metric</th><th>Target</th><th>Native Result</th><th>JVM Result</th><th>Status</th></tr>
        <tr>
            <td>Startup Time</td>
            <td>&lt;1s</td>
            <td>${native_startup}s</td>
            <td>${jvm_startup}s</td>
            <td>$([ "$native_startup" != "N/A" ] && (( $(echo "$native_startup < 1.0" | bc -l) )) && echo "✅ PASS" || echo "⚠️ REVIEW")</td>
        </tr>
        <tr>
            <td>Memory Usage</td>
            <td>&lt;100MB</td>
            <td>${native_memory}MB</td>
            <td>${jvm_memory}MB</td>
            <td>$([ "$native_memory" != "N/A" ] && (( $(echo "$native_memory < 100" | bc -l) )) && echo "✅ PASS" || echo "⚠️ REVIEW")</td>
        </tr>
        <tr>
            <td>Response Time</td>
            <td>&lt;0.1s</td>
            <td>${native_response}s</td>
            <td>${jvm_response}s</td>
            <td>$([ "$native_response" != "N/A" ] && (( $(echo "$native_response < 0.1" | bc -l) )) && echo "✅ PASS" || echo "⚠️ REVIEW")</td>
        </tr>
        <tr>
            <td>TPS Performance</td>
            <td>2M+</td>
            <td>${native_tps}</td>
            <td>${jvm_tps}</td>
            <td>$([ "$native_tps" != "N/A" ] && [ "$native_tps" -gt 2000000 ] && echo "✅ PASS" || echo "⚠️ REVIEW")</td>
        </tr>
    </table>
    
    <h2>Detailed Results</h2>
    
    <div class="comparison">
        <div class="metric native">
            <h3>🔥 Native Performance</h3>
            <p><strong>Startup Time:</strong> ${native_startup}s</p>
            <p><strong>Memory Usage:</strong> ${native_memory}MB</p>
            <p><strong>Response Time:</strong> ${native_response}s</p>
            <p><strong>TPS:</strong> ${native_tps}</p>
        </div>
        
        <div class="metric jvm">
            <h3>☕ JVM Performance</h3>
            <p><strong>Startup Time:</strong> ${jvm_startup}s</p>
            <p><strong>Memory Usage:</strong> ${jvm_memory}MB</p>
            <p><strong>Response Time:</strong> ${jvm_response}s</p>
            <p><strong>TPS:</strong> ${jvm_tps}</p>
        </div>
    </div>
    
    <h2>Recommendations</h2>
    <div class="metric">
        <h4>Production Deployment</h4>
        <ul>
            <li>Use native-ultra profile for maximum performance</li>
            <li>Deploy with Kubernetes using native Docker images</li>
            <li>Allocate minimum 256MB memory for production workloads</li>
            <li>Enable virtual threads for maximum concurrency</li>
        </ul>
    </div>
    
    <h2>Build Configuration</h2>
    <div class="metric">
        <p><strong>Native Binary:</strong> $NATIVE_BINARY</p>
        <p><strong>JVM JAR:</strong> $JVM_JAR</p>
        <p><strong>Test Environment:</strong> $(uname -a)</p>
        <p><strong>Build Profiles:</strong> native, native-fast, native-ultra</p>
    </div>
    
    <footer style="margin-top: 40px; text-align: center; color: #666;">
        <p>Aurigraph DLT V11 Performance Benchmark - $(date)</p>
    </footer>
</body>
</html>
EOF
    
    log_success "Benchmark report generated: $report_file"
}

# Main benchmark execution
main() {
    log_info "Starting comprehensive performance benchmark..."
    
    initialize_benchmark
    
    # Test native binary
    if [ -n "$NATIVE_BINARY" ]; then
        log_info "=== Testing Native Binary Performance ==="
        measure_startup_time "native" "$NATIVE_BINARY"
        measure_memory_usage "native" "$NATIVE_BINARY"
        measure_response_time "native" "$NATIVE_BINARY"
        stress_test_tps "native" "$NATIVE_BINARY"
    fi
    
    # Test JVM JAR for comparison
    if [ -n "$JVM_JAR" ]; then
        log_info "=== Testing JVM Performance (Comparison) ==="
        measure_startup_time "jvm" "$JVM_JAR"
        measure_memory_usage "jvm" "$JVM_JAR"
        measure_response_time "jvm" "$JVM_JAR"
        stress_test_tps "jvm" "$JVM_JAR"
    fi
    
    generate_benchmark_report
    
    log_success "Performance benchmark completed!"
    echo ""
    echo "📊 Results summary:"
    echo "   - Benchmark log: $BENCHMARK_LOG"
    echo "   - Results directory: $RESULTS_DIR"
    echo "   - HTML report: $RESULTS_DIR/benchmark-report-*.html"
    echo ""
    echo "🎯 Production targets:"
    echo "   - Startup time: <1s"
    echo "   - Memory usage: <100MB"
    echo "   - TPS performance: 2M+"
    echo "   - Response time: <100ms"
}

# Cleanup function
cleanup() {
    log_info "Cleaning up benchmark processes..."
    pkill -f aurigraph-v11 2>/dev/null || true
    pkill -f java 2>/dev/null || true
    sleep 2
}

# Set up cleanup trap
trap cleanup EXIT

# Execute main benchmark
main "$@"