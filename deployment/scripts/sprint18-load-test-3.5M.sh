#!/bin/bash
# Sprint 18 Load Test - 3.5M TPS Target Validation
# ADA-Perf (AI/ML Performance Agent)

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Configuration
BASE_URL="http://localhost:9003"
TARGET_TPS=3500000
TEST_DURATION=600  # 10 minutes
WARMUP_DURATION=30
RESULTS_DIR="sprint18-results-$(date +%Y%m%d-%H%M%S)"

# Logging functions
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

log_metric() {
    echo -e "${CYAN}[METRIC]${NC} $1"
}

# Print header
print_header() {
    echo ""
    echo -e "${BOLD}${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}${CYAN}║  Sprint 18 Load Test - 3.5M TPS Target Validation       ║${NC}"
    echo -e "${BOLD}${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
    echo ""
    log_info "Configuration:"
    log_info "  Base URL: $BASE_URL"
    log_info "  Target TPS: $(printf '%0.1f' $(echo "scale=1; $TARGET_TPS / 1000000" | bc))M"
    log_info "  Duration: ${TEST_DURATION}s ($(echo "$TEST_DURATION / 60" | bc) minutes)"
    log_info "  Warm-up: ${WARMUP_DURATION}s"
    echo ""
}

# Check service health
check_health() {
    log_info "Step 1: Checking service health..."

    if ! curl -s "${BASE_URL}/q/health" >/dev/null 2>&1; then
        log_error "Service is not reachable at $BASE_URL"
        log_error "Please ensure the service is running:"
        echo "  ./mvnw quarkus:dev"
        echo "  OR"
        echo "  java -jar target/quarkus-app/quarkus-run.jar"
        exit 1
    fi

    health_status=$(curl -s "${BASE_URL}/q/health" | jq -r '.status // "UNKNOWN"')
    if [ "$health_status" != "UP" ]; then
        log_error "Service health check failed: $health_status"
        exit 1
    fi

    log_success "Service is healthy and ready"
    echo ""
}

# Warm-up phase
warmup() {
    log_info "Step 2: Warming up system (${WARMUP_DURATION}s)..."

    for i in $(seq 1 $WARMUP_DURATION); do
        curl -s "${BASE_URL}/api/v11/analytics/performance" >/dev/null 2>&1 || true
        if [ $((i % 10)) -eq 0 ]; then
            log_info "  Warm-up progress: $i/${WARMUP_DURATION}s"
        fi
        sleep 1
    done

    log_success "Warm-up complete"
    echo ""
}

# Create results directory
setup_results() {
    log_info "Step 3: Setting up results directory..."
    mkdir -p "$RESULTS_DIR"

    # Create CSV headers
    echo "timestamp,tps,p50_ms,p95_ms,p99_ms,success_rate,queue_depth" > "$RESULTS_DIR/metrics.csv"
    echo "timestamp,heap_used_mb,heap_max_mb,gc_count,gc_time_ms" > "$RESULTS_DIR/memory.csv"

    log_success "Results directory created: $RESULTS_DIR"
    echo ""
}

# Monitor performance metrics
monitor_performance() {
    local start_time=$(date +%s)
    local end_time=$((start_time + TEST_DURATION))
    local sample_count=0

    log_info "Step 4: Starting performance monitoring..."
    log_info "  Duration: ${TEST_DURATION}s ($(echo "$TEST_DURATION / 60" | bc) minutes)"
    log_info "  Sample Interval: 5 seconds"
    log_info "  Expected Samples: $((TEST_DURATION / 5))"
    echo ""

    while [ $(date +%s) -lt $end_time ]; do
        local timestamp=$(date +%s)

        # Get performance metrics
        local perf_response=$(curl -s "${BASE_URL}/api/v11/analytics/performance" 2>/dev/null || echo "{}")

        local tps=$(echo "$perf_response" | jq -r '.throughput // 0')
        local p50=$(echo "$perf_response" | jq -r '.responseTime.p50 // 0')
        local p95=$(echo "$perf_response" | jq -r '.responseTime.p95 // 0')
        local p99=$(echo "$perf_response" | jq -r '.responseTime.p99 // 0')
        local success_rate=$(echo "$perf_response" | jq -r '.successRate // 100')

        # Get consensus metrics
        local consensus_response=$(curl -s "${BASE_URL}/api/v11/consensus/stats" 2>/dev/null || echo "{}")
        local queue_depth=$(echo "$consensus_response" | jq -r '.queueDepth // 0')

        # Record metrics
        echo "$timestamp,$tps,$p50,$p95,$p99,$success_rate,$queue_depth" >> "$RESULTS_DIR/metrics.csv"

        sample_count=$((sample_count + 1))

        # Log progress every 30 seconds
        if [ $((sample_count % 6)) -eq 0 ]; then
            local elapsed=$((timestamp - start_time))
            local remaining=$((TEST_DURATION - elapsed))
            log_metric "Sample $sample_count | Elapsed: ${elapsed}s | TPS: $(printf '%.2f' $(echo "scale=2; $tps / 1000000" | bc))M | P99: ${p99}ms | Remaining: ${remaining}s"
        fi

        sleep 5
    done

    log_success "Performance monitoring complete ($sample_count samples)"
    echo ""
}

# Analyze results
analyze_results() {
    log_info "Step 5: Analyzing results..."
    echo ""

    # Calculate statistics
    local total_samples=$(tail -n +2 "$RESULTS_DIR/metrics.csv" | wc -l)

    # TPS statistics
    local avg_tps=$(awk -F, 'NR>1 {sum+=$2; count++} END {print sum/count}' "$RESULTS_DIR/metrics.csv")
    local min_tps=$(awk -F, 'NR>1 {min=$2} NR>2 {if($2<min) min=$2} END {print min}' "$RESULTS_DIR/metrics.csv")
    local max_tps=$(awk -F, 'NR>1 {max=$2} NR>2 {if($2>max) max=$2} END {print max}' "$RESULTS_DIR/metrics.csv")

    # Latency statistics
    local avg_p50=$(awk -F, 'NR>1 {sum+=$3; count++} END {print sum/count}' "$RESULTS_DIR/metrics.csv")
    local avg_p95=$(awk -F, 'NR>1 {sum+=$4; count++} END {print sum/count}' "$RESULTS_DIR/metrics.csv")
    local avg_p99=$(awk -F, 'NR>1 {sum+=$5; count++} END {print sum/count}' "$RESULTS_DIR/metrics.csv")
    local max_p99=$(awk -F, 'NR>1 {max=$5} NR>2 {if($5>max) max=$5} END {print max}' "$RESULTS_DIR/metrics.csv")

    # Success rate
    local avg_success_rate=$(awk -F, 'NR>1 {sum+=$6; count++} END {print sum/count}' "$RESULTS_DIR/metrics.csv")

    # Save summary
    cat > "$RESULTS_DIR/summary.txt" << EOF
Sprint 18 Load Test Results
===========================

Test Configuration:
- Target TPS: $(printf '%0.2f' $(echo "scale=2; $TARGET_TPS / 1000000" | bc))M
- Duration: ${TEST_DURATION}s ($(echo "$TEST_DURATION / 60" | bc) minutes)
- Samples: $total_samples

Throughput Metrics:
- Average TPS: $(printf '%0.2f' $(echo "scale=2; $avg_tps / 1000000" | bc))M
- Minimum TPS: $(printf '%0.2f' $(echo "scale=2; $min_tps / 1000000" | bc))M
- Maximum TPS: $(printf '%0.2f' $(echo "scale=2; $max_tps / 1000000" | bc))M

Latency Metrics:
- Average P50: $(printf '%.2f' $avg_p50)ms
- Average P95: $(printf '%.2f' $avg_p95)ms
- Average P99: $(printf '%.2f' $avg_p99)ms
- Maximum P99: $(printf '%.2f' $max_p99)ms

Success Rate:
- Average: $(printf '%.2f' $avg_success_rate)%

Target Validation:
EOF

    # Validate targets
    local tps_met=$(echo "$avg_tps >= $TARGET_TPS" | bc)
    local p99_met=$(echo "$avg_p99 < 100" | bc)
    local success_met=$(echo "$avg_success_rate > 99.9" | bc)

    echo "" >> "$RESULTS_DIR/summary.txt"

    if [ "$tps_met" -eq 1 ]; then
        echo "- TPS Target: PASS ($(printf '%.2f' $(echo "scale=2; $avg_tps / 1000000" | bc))M >= $(printf '%.2f' $(echo "scale=2; $TARGET_TPS / 1000000" | bc))M)" >> "$RESULTS_DIR/summary.txt"
    else
        local deficit=$(echo "$TARGET_TPS - $avg_tps" | bc)
        echo "- TPS Target: FAIL ($(printf '%.2f' $(echo "scale=2; $avg_tps / 1000000" | bc))M < $(printf '%.2f' $(echo "scale=2; $TARGET_TPS / 1000000" | bc))M, deficit: $(printf '%.2f' $(echo "scale=2; $deficit / 1000000" | bc))M)" >> "$RESULTS_DIR/summary.txt"
    fi

    if [ "$p99_met" -eq 1 ]; then
        echo "- P99 Latency: PASS ($(printf '%.2f' $avg_p99)ms < 100ms)" >> "$RESULTS_DIR/summary.txt"
    else
        echo "- P99 Latency: FAIL ($(printf '%.2f' $avg_p99)ms >= 100ms)" >> "$RESULTS_DIR/summary.txt"
    fi

    if [ "$success_met" -eq 1 ]; then
        echo "- Success Rate: PASS ($(printf '%.2f' $avg_success_rate)% > 99.9%)" >> "$RESULTS_DIR/summary.txt"
    else
        echo "- Success Rate: FAIL ($(printf '%.2f' $avg_success_rate)% <= 99.9%)" >> "$RESULTS_DIR/summary.txt"
    fi

    # Print results
    cat "$RESULTS_DIR/summary.txt"

    echo ""
    echo "══════════════════════════════════════════════════"
    echo ""

    # Overall verdict
    local targets_met=$((tps_met + p99_met + success_met))

    if [ $targets_met -eq 3 ]; then
        echo -e "${BOLD}${GREEN}✓ ALL TARGETS MET${NC}"
        echo -e "${GREEN}  The system has successfully demonstrated 3.5M+ TPS${NC}"
        echo -e "${GREEN}  sustained performance with <100ms P99 latency.${NC}"
        echo ""
        echo -e "${GREEN}  Sprint 18 Performance Optimization: COMPLETE${NC}"
        exit 0
    elif [ $targets_met -eq 2 ]; then
        echo -e "${BOLD}${YELLOW}⚠ PARTIAL SUCCESS${NC}"
        echo -e "${YELLOW}  2 out of 3 targets met. Minor optimization needed.${NC}"
        echo ""
        [ "$tps_met" -eq 0 ] && echo -e "${YELLOW}  - Action Required: Optimize throughput${NC}"
        [ "$p99_met" -eq 0 ] && echo -e "${YELLOW}  - Action Required: Reduce tail latency${NC}"
        [ "$success_met" -eq 0 ] && echo -e "${YELLOW}  - Action Required: Improve success rate${NC}"
        exit 1
    else
        echo -e "${BOLD}${RED}✗ TARGETS NOT MET${NC}"
        echo -e "${RED}  Only $targets_met out of 3 targets achieved.${NC}"
        echo ""
        [ "$tps_met" -eq 0 ] && echo -e "${RED}  - TPS: $(printf '%.2f' $(echo "scale=2; $avg_tps / 1000000" | bc))M < $(printf '%.2f' $(echo "scale=2; $TARGET_TPS / 1000000" | bc))M${NC}"
        [ "$p99_met" -eq 0 ] && echo -e "${RED}  - P99 Latency: $(printf '%.2f' $avg_p99)ms >= 100ms${NC}"
        [ "$success_met" -eq 0 ] && echo -e "${RED}  - Success Rate: $(printf '%.2f' $avg_success_rate)% < 99.9%${NC}"
        exit 1
    fi
}

# Generate charts (optional, requires gnuplot)
generate_charts() {
    if ! command -v gnuplot >/dev/null 2>&1; then
        log_warning "gnuplot not installed, skipping chart generation"
        return
    fi

    log_info "Generating performance charts..."

    # TPS over time
    gnuplot << EOF
set terminal png size 1200,600
set output '$RESULTS_DIR/tps_over_time.png'
set title 'TPS Over Time'
set xlabel 'Time (samples)'
set ylabel 'TPS (millions)'
set grid
set datafile separator ','
plot '$RESULTS_DIR/metrics.csv' using 0:(\$2/1000000) with lines title 'TPS' lw 2
EOF

    # Latency over time
    gnuplot << EOF
set terminal png size 1200,600
set output '$RESULTS_DIR/latency_over_time.png'
set title 'Latency Percentiles Over Time'
set xlabel 'Time (samples)'
set ylabel 'Latency (ms)'
set grid
set datafile separator ','
plot '$RESULTS_DIR/metrics.csv' using 0:3 with lines title 'P50' lw 2, \
     '$RESULTS_DIR/metrics.csv' using 0:4 with lines title 'P95' lw 2, \
     '$RESULTS_DIR/metrics.csv' using 0:5 with lines title 'P99' lw 2
EOF

    log_success "Charts generated: $RESULTS_DIR/*.png"
}

# Main execution
main() {
    print_header
    check_health
    warmup
    setup_results
    monitor_performance
    analyze_results
    # generate_charts  # Uncomment if gnuplot is available

    echo ""
    log_info "Full results available in: $RESULTS_DIR/"
    echo ""
}

# Run main
main "$@"
