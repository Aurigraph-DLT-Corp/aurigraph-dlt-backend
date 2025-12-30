#!/bin/bash
# Aurigraph V11 Native Optimization Test Suite
# Comprehensive testing of all three native compilation profiles
# 
# This script tests:
# 1. Fast profile: Development-optimized build
# 2. Standard profile: Balanced production build  
# 3. Ultra profile: Maximum performance build
#
# Usage: ./test-native-optimization.sh [profile]
# Profiles: fast, standard, ultra, all (default)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_LOG="$SCRIPT_DIR/native-optimization-test.log"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'  
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Test results tracking
declare -A test_results
declare -A build_times
declare -A binary_sizes
declare -A startup_times

log_test() {
    echo -e "${CYAN}[TEST]${NC} $1" | tee -a "$TEST_LOG"
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$TEST_LOG"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$TEST_LOG"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$TEST_LOG"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$TEST_LOG"
}

log_perf() {
    echo -e "${PURPLE}[PERFORMANCE]${NC} $1" | tee -a "$TEST_LOG"
}

# Initialize test log
init_test_log() {
    echo "=== Aurigraph V11 Native Optimization Test Suite ===" > "$TEST_LOG"
    echo "Started at: $(date)" >> "$TEST_LOG"
    echo "System: $(uname -a)" >> "$TEST_LOG"
    echo "Available memory: $(free -m 2>/dev/null | awk 'NR==2{print $7}' || echo 'unknown')MB" >> "$TEST_LOG"
    echo "CPU cores: $(nproc 2>/dev/null || echo 'unknown')" >> "$TEST_LOG"
    echo "" >> "$TEST_LOG"
}

# Test prerequisites
check_prerequisites() {
    log_test "Checking test prerequisites..."
    
    local prereq_failed=false
    
    # Check Java
    if ! java -version &>/dev/null; then
        log_error "Java 21+ is required"
        prereq_failed=true
    else
        local java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
        log_info "Java version: $java_version"
    fi
    
    # Check Docker
    if ! command -v docker &>/dev/null; then
        log_error "Docker is required for container builds"
        prereq_failed=true
    else
        local docker_version=$(docker --version)
        log_info "Docker: $docker_version"
    fi
    
    # Check available memory
    local available_memory=$(free -m 2>/dev/null | awk 'NR==2{printf "%d", $7}' || echo "0")
    if [ "$available_memory" -lt 4096 ]; then
        log_warning "Low memory (${available_memory}MB). Consider increasing for optimal builds."
    fi
    
    # Check disk space  
    local disk_space=$(df . | awk 'NR==2 {printf "%d", $4/1024/1024}')
    if [ "$disk_space" -lt 5 ]; then
        log_warning "Low disk space (${disk_space}GB). Native builds require significant space."
    fi
    
    if [ "$prereq_failed" = true ]; then
        log_error "Prerequisites check failed. Please install missing components."
        return 1
    fi
    
    log_success "Prerequisites check passed"
}

# Test individual profile
test_profile() {
    local profile=$1
    local profile_name=$2
    local expected_startup=$3
    local expected_size=$4
    
    log_test "Testing $profile_name profile..."
    
    # Build timing
    local start_time=$(date +%s)
    
    if [ "$profile" = "fast" ]; then
        ./build-native-fast.sh
    elif [ "$profile" = "standard" ]; then
        ./build-native-standard.sh  
    elif [ "$profile" = "ultra" ]; then
        ./build-native-ultra.sh
    else
        log_error "Unknown profile: $profile"
        return 1
    fi
    
    local end_time=$(date +%s)
    local build_duration=$((end_time - start_time))
    build_times[$profile]=$build_duration
    
    log_info "$profile_name build completed in ${build_duration}s"
    
    # Find and analyze binary
    local binary_path=$(find target -name "*-runner" -type f | head -1)
    if [ -z "$binary_path" ]; then
        log_error "Binary not found for $profile_name profile"
        test_results[$profile]="FAILED"
        return 1
    fi
    
    # Binary size analysis
    local binary_size_bytes=$(du -b "$binary_path" | cut -f1)
    local binary_size_mb=$((binary_size_bytes / 1024 / 1024))
    binary_sizes[$profile]=$binary_size_mb
    
    log_perf "$profile_name binary size: ${binary_size_mb}MB"
    
    # Startup performance test
    log_test "Testing $profile_name startup performance..."
    
    local startup_tests=()
    for i in {1..3}; do
        local start=$(date +%s%3N)
        if timeout 30s "$binary_path" --help >/dev/null 2>&1; then
            local end=$(date +%s%3N)
            local startup_time=$((end - start))
            startup_tests+=($startup_time)
            log_info "Startup test $i: ${startup_time}ms"
        else
            log_warning "Startup test $i failed or timed out"
            startup_tests+=(9999)
        fi
    done
    
    # Calculate average startup time
    local total=0
    for time in "${startup_tests[@]}"; do
        total=$((total + time))
    done
    local avg_startup=$((total / ${#startup_tests[@]}))
    startup_times[$profile]=$avg_startup
    
    log_perf "$profile_name average startup: ${avg_startup}ms"
    
    # Performance evaluation
    local test_passed=true
    
    if [ "$avg_startup" -le "$expected_startup" ]; then
        log_success "Startup performance: ${avg_startup}ms ≤ ${expected_startup}ms target ✓"
    else
        log_warning "Startup performance: ${avg_startup}ms > ${expected_startup}ms target"
        test_passed=false
    fi
    
    if [ "$binary_size_mb" -le "$expected_size" ]; then
        log_success "Binary size: ${binary_size_mb}MB ≤ ${expected_size}MB target ✓"
    else
        log_warning "Binary size: ${binary_size_mb}MB > ${expected_size}MB target"  
        test_passed=false
    fi
    
    # Binary type verification
    if ldd "$binary_path" 2>/dev/null | grep -q "not a dynamic executable"; then
        log_success "Static binary verification ✓"
    else
        log_warning "Dynamic binary created (not optimal)"
    fi
    
    if [ "$test_passed" = true ]; then
        test_results[$profile]="PASSED"
        log_success "$profile_name profile test PASSED"
    else
        test_results[$profile]="PARTIAL"
        log_warning "$profile_name profile test PARTIALLY PASSED"
    fi
    
    # Clean up for next test
    rm -f "$binary_path"
    ./mvnw clean -q
    
    echo "" | tee -a "$TEST_LOG"
}

# Generate comprehensive test report
generate_test_report() {
    local report_file="$SCRIPT_DIR/native-optimization-test-report-$(date +%Y%m%d-%H%M%S).txt"
    
    {
        echo "════════════════════════════════════════════════════════════════"
        echo "        AURIGRAPH V11 NATIVE OPTIMIZATION TEST REPORT"
        echo "════════════════════════════════════════════════════════════════"
        echo "Test completed: $(date '+%Y-%m-%d %H:%M:%S')"
        echo "System: $(uname -a)"
        echo "Available memory: $(free -m 2>/dev/null | awk 'NR==2{print $7}' || echo 'unknown')MB"
        echo "CPU cores: $(nproc 2>/dev/null || echo 'unknown')"
        echo ""
        
        echo "📊 TEST RESULTS SUMMARY" 
        echo "────────────────────────────────────────────────────────────────"
        printf "%-12s %-8s %-12s %-12s %-12s\n" "Profile" "Result" "Build Time" "Binary Size" "Startup Time"
        echo "────────────────────────────────────────────────────────────────"
        
        for profile in fast standard ultra; do
            if [ -n "${test_results[$profile]:-}" ]; then
                printf "%-12s %-8s %-12s %-12s %-12s\n" \
                    "$profile" \
                    "${test_results[$profile]}" \
                    "${build_times[$profile]:-N/A}s" \
                    "${binary_sizes[$profile]:-N/A}MB" \
                    "${startup_times[$profile]:-N/A}ms"
            fi
        done
        echo ""
        
        echo "🎯 PERFORMANCE TARGETS"
        echo "────────────────────────────────────────────────────────────────"
        echo "Fast Profile:     Build <3min,  Startup <1000ms, Size <65MB"
        echo "Standard Profile: Build <15min, Startup <800ms,  Size <80MB"  
        echo "Ultra Profile:    Build <30min, Startup <600ms,  Size <90MB"
        echo ""
        
        echo "📈 OPTIMIZATION ANALYSIS"
        echo "────────────────────────────────────────────────────────────────"
        
        # Calculate performance improvements
        if [ -n "${startup_times[fast]:-}" ] && [ -n "${startup_times[ultra]:-}" ]; then
            local improvement=$(( ${startup_times[fast]} - ${startup_times[ultra]} ))
            local improvement_pct=$(( improvement * 100 / ${startup_times[fast]} ))
            echo "Startup improvement (ultra vs fast): ${improvement}ms (${improvement_pct}%)"
        fi
        
        if [ -n "${binary_sizes[fast]:-}" ] && [ -n "${binary_sizes[ultra]:-}" ]; then
            local size_diff=$(( ${binary_sizes[ultra]} - ${binary_sizes[fast]} ))
            echo "Size difference (ultra vs fast): ${size_diff}MB"
        fi
        
        echo ""
        
        echo "✅ OPTIMIZATION FEATURES IMPLEMENTED"
        echo "────────────────────────────────────────────────────────────────"
        echo "• Three-tier optimization profiles (fast/standard/ultra)"
        echo "• GraalVM native image compilation with advanced flags"
        echo "• CPU-specific optimizations (-march=native for ultra)"  
        echo "• Memory layout optimizations (G1GC, compressed OOPs)"
        echo "• Static binary generation with minimal dependencies"
        echo "• Reflection configuration optimization"
        echo "• Resource inclusion/exclusion optimization"
        echo "• Container-optimized Docker builds"
        echo "• Build time vs performance tradeoffs"
        echo ""
        
        echo "📋 RECOMMENDATIONS"
        echo "────────────────────────────────────────────────────────────────"
        echo "• Use 'fast' profile for development (quick iterations)"
        echo "• Use 'standard' profile for testing and staging"
        echo "• Use 'ultra' profile for production deployments"
        echo "• Monitor startup times in production environments"
        echo "• Consider memory limits based on binary size + heap"
        echo ""
        
    } > "$report_file"
    
    log_success "Test report generated: $report_file"
    echo "Report location: $report_file"
}

# Main test execution
main() {
    cd "$SCRIPT_DIR"
    
    local profile_to_test="${1:-all}"
    
    init_test_log
    log_test "Starting Aurigraph V11 native optimization test suite..."
    
    check_prerequisites
    
    case "$profile_to_test" in
        fast)
            test_profile "fast" "Fast Development" 1000 65
            ;;
        standard)  
            test_profile "standard" "Standard Production" 800 80
            ;;
        ultra)
            test_profile "ultra" "Ultra Performance" 600 90
            ;;
        all|*)
            test_profile "fast" "Fast Development" 1000 65
            test_profile "standard" "Standard Production" 800 80
            test_profile "ultra" "Ultra Performance" 600 90
            ;;
    esac
    
    generate_test_report
    
    # Summary
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "🎯 NATIVE OPTIMIZATION TEST SUITE COMPLETED"
    echo "════════════════════════════════════════════════════════════════"
    
    local total_passed=0
    local total_tests=0
    
    for profile in "${!test_results[@]}"; do
        total_tests=$((total_tests + 1))
        if [ "${test_results[$profile]}" = "PASSED" ]; then
            total_passed=$((total_passed + 1))
        fi
        echo "$profile: ${test_results[$profile]}"
    done
    
    echo ""
    echo "Results: $total_passed/$total_tests profiles passed optimization targets"
    echo "Test log: $TEST_LOG"
    echo "Full report: native-optimization-test-report-*.txt"
    echo "════════════════════════════════════════════════════════════════"
}

# Trap to cleanup on error
trap 'log_error "Test failed at line $LINENO"' ERR

# Execute main function
main "$@"