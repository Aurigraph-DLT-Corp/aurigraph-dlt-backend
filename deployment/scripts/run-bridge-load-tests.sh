#!/bin/bash

###############################################################################
# Aurigraph Sprint 14 - Tier 3: Bridge Load Testing Orchestration Script
#
# Purpose: Execute comprehensive load testing for bridge transactions with
#          progressive load patterns (50, 100, 250, 500-1000 VUs)
#
# Expected Outcomes:
#   - Scenario 1 (50 VUs):   ~388K TPS (50% of baseline 776K)
#   - Scenario 2 (100 VUs):  ~776K TPS (100% baseline)
#   - Scenario 3 (250 VUs):  ~1.4M TPS (180% of baseline - peak load)
#   - Scenario 4 (500 VUs):  Test system breaking point
#   - Scenario 5 (1000 VUs): Stress test with high concurrency
#
# Test Criteria:
#   - Success Rate: 99%+ at 100 concurrent, <1% failure at 1000 concurrent
#   - Latency: P95 < 200ms, P99 < 400ms for baseline scenarios
#   - Error Rate: < 0.01% for normal load, < 5% for extreme load
#
# Run: ./run-bridge-load-tests.sh [scenario]
#      ./run-bridge-load-tests.sh all          # Run all scenarios
#      ./run-bridge-load-tests.sh 1            # Run scenario 1 only
#      ./run-bridge-load-tests.sh baseline     # Run baseline scenario
#
###############################################################################

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/test-results/bridge-load-tests"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BASE_URL="${BASE_URL:-http://localhost:9003}"
K6_SCENARIOS=(
    "k6-scenario-baseline.js"
    "k6-scenario-progressive.js"
    "k6-scenario-cache-performance.js"
    "k6-scenario-peak-load.js"
)

# Create results directory
mkdir -p "${RESULTS_DIR}"

###############################################################################
# Utility Functions
###############################################################################

print_header() {
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC} $1"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_section() {
    echo -e "${YELLOW}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if K6 is installed
check_k6_installation() {
    if ! command -v k6 &> /dev/null; then
        print_error "K6 is not installed"
        echo "Install K6 with: brew install k6 (macOS) or visit https://k6.io/docs/getting-started/installation/"
        exit 1
    fi
    print_success "K6 is installed: $(k6 version)"
}

# Check if service is running
check_service_health() {
    print_section "Checking service health at ${BASE_URL}..."

    if curl -s "${BASE_URL}/q/health" > /dev/null 2>&1; then
        local health_response=$(curl -s "${BASE_URL}/q/health" | grep -q '"status":"UP"' && echo "UP" || echo "DOWN")
        if [ "$health_response" = "UP" ]; then
            print_success "Service is healthy and responding"
            return 0
        fi
    fi

    print_error "Service is not responding at ${BASE_URL}"
    print_info "Start the service with: cd ${SCRIPT_DIR} && ./mvnw quarkus:dev"
    exit 1
}

# Run single K6 scenario
run_scenario() {
    local scenario_file="$1"
    local scenario_num="$2"
    local scenario_name="$3"

    if [ ! -f "${SCRIPT_DIR}/${scenario_file}" ]; then
        print_error "Scenario file not found: ${scenario_file}"
        return 1
    fi

    print_header "SCENARIO ${scenario_num}: ${scenario_name}"

    local results_file="${RESULTS_DIR}/scenario-${scenario_num}-${TIMESTAMP}.json"
    local log_file="${RESULTS_DIR}/scenario-${scenario_num}-${TIMESTAMP}.log"

    print_section "Running K6 scenario..."
    print_info "Results will be saved to: ${results_file}"

    # Run K6 with JSON output and metrics
    if k6 run "${SCRIPT_DIR}/${scenario_file}" \
        --out "json=${results_file}" \
        --env BASE_URL="${BASE_URL}" \
        2>&1 | tee "${log_file}"; then

        print_success "Scenario ${scenario_num} completed successfully"

        # Extract key metrics from results
        extract_metrics "${results_file}" "${scenario_num}"

        return 0
    else
        print_error "Scenario ${scenario_num} failed"
        return 1
    fi
}

# Extract and display key metrics from K6 JSON results
extract_metrics() {
    local results_file="$1"
    local scenario_num="$2"

    if [ ! -f "${results_file}" ]; then
        print_error "Results file not found: ${results_file}"
        return 1
    fi

    print_section "Metrics Summary - Scenario ${scenario_num}"

    # Extract metrics from JSON results (requires jq)
    if command -v jq &> /dev/null; then

        # Try to extract useful metrics from K6 JSON output
        local total_requests=$(jq '[.metrics[] | select(.type=="Counter" and .metric=="http_reqs") | .value] | add' "${results_file}" 2>/dev/null || echo "N/A")
        local error_rate=$(jq '[.metrics[] | select(.type=="Rate" and .metric=="http_req_failed") | .value] | add / length' "${results_file}" 2>/dev/null || echo "N/A")

        echo "  Total Requests: ${total_requests}"
        echo "  Error Rate: ${error_rate}%"

    else
        print_info "Install 'jq' for detailed metrics parsing: brew install jq"
    fi

    # Also show last few lines of K6 output (usually contains summary)
    local log_file="${RESULTS_DIR}/scenario-${scenario_num}-${TIMESTAMP}.log"
    if [ -f "${log_file}" ]; then
        print_section "K6 Output Summary:"
        tail -20 "${log_file}" | grep -E "(checks|iterations|requests|errors|duration)" || true
    fi
}

# Run all scenarios in sequence
run_all_scenarios() {
    print_header "BRIDGE LOAD TESTING - ALL SCENARIOS"
    print_info "Test Start Time: $(date)"
    print_info "Base URL: ${BASE_URL}"
    print_info "Results Directory: ${RESULTS_DIR}"

    check_service_health

    local failed_scenarios=()
    local passed_scenarios=()

    # Scenario 1: Baseline (50 VUs)
    if run_scenario "k6-scenario-baseline.js" "1" "Baseline Sanity Check (50 VUs)"; then
        passed_scenarios+=("1")
    else
        failed_scenarios+=("1")
    fi

    sleep 10  # Wait between scenarios

    # Scenario 2: Progressive (100 VUs)
    if run_scenario "k6-scenario-progressive.js" "2" "Progressive Load (100 VUs)"; then
        passed_scenarios+=("2")
    else
        failed_scenarios+=("2")
    fi

    sleep 10

    # Scenario 3: Cache Performance (250 VUs)
    if run_scenario "k6-scenario-cache-performance.js" "3" "Cache Performance (250 VUs)"; then
        passed_scenarios+=("3")
    else
        failed_scenarios+=("3")
    fi

    sleep 10

    # Scenario 4: Peak Load (500-1000 VUs)
    if run_scenario "k6-scenario-peak-load.js" "4" "Peak Load Test (500-1000 VUs)"; then
        passed_scenarios+=("4")
    else
        failed_scenarios+=("4")
    fi

    # Print final summary
    print_header "LOAD TESTING SUMMARY"
    echo "Test Completion Time: $(date)"
    echo ""
    echo "Results Location: ${RESULTS_DIR}"
    echo ""

    if [ ${#passed_scenarios[@]} -gt 0 ]; then
        print_success "Passed Scenarios: ${passed_scenarios[*]}"
    fi

    if [ ${#failed_scenarios[@]} -gt 0 ]; then
        print_error "Failed Scenarios: ${failed_scenarios[*]}"
        return 1
    else
        print_success "All scenarios completed successfully!"
        return 0
    fi
}

# Run specific scenario by number
run_specific_scenario() {
    local scenario_num="$1"

    check_service_health

    case "${scenario_num}" in
        1|baseline)
            run_scenario "k6-scenario-baseline.js" "1" "Baseline Sanity Check (50 VUs)"
            ;;
        2|progressive)
            run_scenario "k6-scenario-progressive.js" "2" "Progressive Load (100 VUs)"
            ;;
        3|cache)
            run_scenario "k6-scenario-cache-performance.js" "3" "Cache Performance (250 VUs)"
            ;;
        4|peak)
            run_scenario "k6-scenario-peak-load.js" "4" "Peak Load Test (500-1000 VUs)"
            ;;
        *)
            print_error "Invalid scenario number: ${scenario_num}"
            echo "Valid options: 1, 2, 3, 4, all, baseline, progressive, cache, peak"
            exit 1
            ;;
    esac
}

# Main entry point
main() {
    print_header "AURIGRAPH SPRINT 14 - TIER 3: BRIDGE LOAD TESTING"

    check_k6_installation

    local scenario="${1:-all}"

    case "${scenario}" in
        all)
            run_all_scenarios
            ;;
        1|2|3|4|baseline|progressive|cache|peak)
            run_specific_scenario "${scenario}"
            ;;
        --help|-h)
            echo "Usage: $0 [scenario]"
            echo ""
            echo "Scenarios:"
            echo "  all              Run all load test scenarios"
            echo "  1, baseline      Baseline sanity check (50 VUs)"
            echo "  2, progressive   Progressive load test (100 VUs)"
            echo "  3, cache         Cache performance test (250 VUs)"
            echo "  4, peak          Peak load test (500-1000 VUs)"
            echo ""
            echo "Examples:"
            echo "  $0 all              # Run all scenarios"
            echo "  $0 1                # Run scenario 1 only"
            echo "  $0 baseline         # Run baseline scenario"
            echo ""
            exit 0
            ;;
        *)
            print_error "Unknown option: ${scenario}"
            echo "Run '$0 --help' for usage information"
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"
exit $?
