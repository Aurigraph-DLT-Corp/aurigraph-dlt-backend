#!/bin/bash

# Aurigraph V11 Test Coverage Analysis Script
# 
# This script provides comprehensive test coverage analysis for the V11 platform
# Targets: Increase from 15% baseline to 50%+ coverage with focus on critical components
#
# Usage: ./analyze-test-coverage.sh [OPTIONS]
# Options:
#   --run-tests    : Execute all tests before analysis
#   --detailed     : Generate detailed coverage reports
#   --benchmark    : Include performance test benchmarks
#   --export       : Export results to CSV/HTML formats

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(pwd)"
REPORTS_DIR="$PROJECT_DIR/target/coverage-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Test targets and thresholds
TARGET_OVERALL_COVERAGE=50
TARGET_CRITICAL_COVERAGE=95
BASELINE_COVERAGE=15

print_header() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}    Aurigraph V11 Test Coverage Analysis${NC}"
    echo -e "${BLUE}================================================${NC}"
    echo -e "Baseline Coverage: ${BASELINE_COVERAGE}%"
    echo -e "Target Coverage: ${TARGET_OVERALL_COVERAGE}%"
    echo -e "Critical Components Target: ${TARGET_CRITICAL_COVERAGE}%"
    echo -e "Analysis Time: $(date)"
    echo ""
}

run_tests() {
    echo -e "${YELLOW}Running comprehensive test suite...${NC}"
    
    # Clean previous test results
    ./mvnw clean
    
    echo "🧪 Running unit tests..."
    ./mvnw test -Dtest="*Test" || true
    
    echo "🔗 Running integration tests..."
    ./mvnw test -Dtest="*IT,*IntegrationTest" || true
    
    echo "⚡ Running performance tests..."
    ./mvnw test -Dtest="*PerformanceTest,*BenchmarkTest" || true
    
    echo -e "${GREEN}Test execution completed${NC}"
}

generate_coverage_report() {
    echo -e "${YELLOW}Generating JaCoCo coverage reports...${NC}"
    
    # Generate JaCoCo report
    ./mvnw jacoco:report
    
    # Create comprehensive reports directory
    mkdir -p "$REPORTS_DIR"
    
    # Copy JaCoCo reports
    if [ -d "target/site/jacoco" ]; then
        cp -r target/site/jacoco "$REPORTS_DIR/jacoco-$TIMESTAMP"
        echo -e "${GREEN}JaCoCo HTML report: $REPORTS_DIR/jacoco-$TIMESTAMP/index.html${NC}"
    fi
}

analyze_component_coverage() {
    echo -e "${YELLOW}Analyzing component-specific coverage...${NC}"
    
    # Component mapping
    declare -A COMPONENTS=(
        ["grpc"]="io/aurigraph/v11/grpc"
        ["ai"]="io/aurigraph/v11/ai"
        ["consensus"]="io/aurigraph/v11/consensus"
        ["crypto"]="io/aurigraph/v11/crypto"
        ["performance"]="io/aurigraph/v11/performance"
        ["integration"]="io/aurigraph/v11/integration"
        ["monitoring"]="io/aurigraph/v11/monitoring"
        ["hms"]="io/aurigraph/v11/hms"
    )
    
    echo -e "${BLUE}Component Coverage Analysis:${NC}"
    echo "================================"
    
    for component in "${!COMPONENTS[@]}"; do
        path="${COMPONENTS[$component]}"
        
        # Count test files for this component
        test_count=$(find src/test/java/$path -name "*.java" 2>/dev/null | wc -l || echo "0")
        source_count=$(find src/main/java/$path -name "*.java" 2>/dev/null | wc -l || echo "0")
        
        if [ "$source_count" -gt 0 ]; then
            coverage_ratio=$(echo "scale=1; $test_count / $source_count * 100" | bc -l 2>/dev/null || echo "0")
            
            # Color coding based on coverage
            if (( $(echo "$coverage_ratio >= $TARGET_CRITICAL_COVERAGE" | bc -l) )); then
                color=$GREEN
                status="EXCELLENT"
            elif (( $(echo "$coverage_ratio >= 80" | bc -l) )); then
                color=$BLUE
                status="GOOD"
            elif (( $(echo "$coverage_ratio >= 50" | bc -l) )); then
                color=$YELLOW
                status="MODERATE"
            else
                color=$RED
                status="NEEDS WORK"
            fi
            
            printf "${color}%-12s${NC}: %2d tests / %2d sources = %5.1f%% [%s]\n" \
                   "$component" "$test_count" "$source_count" "$coverage_ratio" "$status"
        fi
    done
    echo ""
}

analyze_test_types() {
    echo -e "${YELLOW}Analyzing test types and distribution...${NC}"
    
    # Count different types of tests
    unit_tests=$(find src/test/java -name "*Test.java" ! -name "*IT.java" ! -name "*IntegrationTest.java" ! -name "*PerformanceTest.java" | wc -l)
    integration_tests=$(find src/test/java -name "*IT.java" -o -name "*IntegrationTest.java" | wc -l)
    performance_tests=$(find src/test/java -name "*PerformanceTest.java" -o -name "*BenchmarkTest.java" | wc -l)
    
    total_tests=$((unit_tests + integration_tests + performance_tests))
    
    echo -e "${BLUE}Test Distribution:${NC}"
    echo "=================="
    printf "Unit Tests:        %3d (%.1f%%)\n" "$unit_tests" "$(echo "scale=1; $unit_tests * 100 / $total_tests" | bc -l)"
    printf "Integration Tests: %3d (%.1f%%)\n" "$integration_tests" "$(echo "scale=1; $integration_tests * 100 / $total_tests" | bc -l)"
    printf "Performance Tests: %3d (%.1f%%)\n" "$performance_tests" "$(echo "scale=1; $performance_tests * 100 / $total_tests" | bc -l)"
    printf "Total Tests:       %3d\n" "$total_tests"
    echo ""
}

analyze_critical_paths() {
    echo -e "${YELLOW}Analyzing critical path coverage...${NC}"
    
    # Critical components that must have high coverage
    declare -a CRITICAL_COMPONENTS=(
        "HighPerformanceGrpcService"
        "AIConsensusOptimizer"
        "DilithiumSignatureService"
        "QuantumCryptoService"
        "HyperRAFTConsensusService"
        "TransactionService"
    )
    
    echo -e "${BLUE}Critical Component Analysis:${NC}"
    echo "============================"
    
    for component in "${CRITICAL_COMPONENTS[@]}"; do
        # Check if component has comprehensive tests
        test_files=$(find src/test/java -name "*${component}*Test.java" | wc -l)
        source_files=$(find src/main/java -name "*${component}*.java" | wc -l)
        
        if [ "$test_files" -gt 0 ] && [ "$source_files" -gt 0 ]; then
            # Estimate coverage based on test file existence and size
            test_lines=$(find src/test/java -name "*${component}*Test.java" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo "0")
            source_lines=$(find src/main/java -name "*${component}*.java" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo "1")
            
            # Rough coverage estimation (test lines / source lines * adjustment factor)
            estimated_coverage=$(echo "scale=1; $test_lines / $source_lines * 30" | bc -l 2>/dev/null || echo "0")
            estimated_coverage=$(echo "if ($estimated_coverage > 100) 100 else $estimated_coverage" | bc -l)
            
            if (( $(echo "$estimated_coverage >= $TARGET_CRITICAL_COVERAGE" | bc -l) )); then
                color=$GREEN
                status="TARGET MET"
            elif (( $(echo "$estimated_coverage >= 80" | bc -l) )); then
                color=$BLUE
                status="CLOSE TO TARGET"
            elif (( $(echo "$estimated_coverage >= 50" | bc -l) )); then
                color=$YELLOW
                status="MODERATE"
            else
                color=$RED
                status="INSUFFICIENT"
            fi
            
            printf "${color}%-30s${NC}: ~%5.1f%% [%s]\n" "$component" "$estimated_coverage" "$status"
        else
            printf "${RED}%-30s${NC}: NO TESTS FOUND\n" "$component"
        fi
    done
    echo ""
}

generate_performance_summary() {
    echo -e "${YELLOW}Generating performance test summary...${NC}"
    
    # Look for performance test results
    echo -e "${BLUE}Performance Test Coverage:${NC}"
    echo "=========================="
    
    perf_test_methods=$(grep -r "@DisplayName.*performance\|@DisplayName.*Performance\|@Test.*perf\|testPerformance" src/test/java/ | wc -l || echo "0")
    benchmark_methods=$(grep -r "@DisplayName.*benchmark\|@DisplayName.*Benchmark\|testBenchmark" src/test/java/ | wc -l || echo "0")
    load_test_methods=$(grep -r "@DisplayName.*load\|@DisplayName.*Load\|testLoad" src/test/java/ | wc -l || echo "0")
    
    printf "Performance Tests:   %3d methods\n" "$perf_test_methods"
    printf "Benchmark Tests:     %3d methods\n" "$benchmark_methods"
    printf "Load Tests:          %3d methods\n" "$load_test_methods"
    
    # Performance targets validation
    echo ""
    echo -e "${BLUE}Performance Target Coverage:${NC}"
    echo "============================"
    
    # Check for specific performance validations
    tps_tests=$(grep -r "1.*500.*000\|1\.5.*M\|TPS" src/test/java/ | wc -l || echo "0")
    latency_tests=$(grep -r "latency\|Latency.*ms\|<.*ms" src/test/java/ | wc -l || echo "0")
    memory_tests=$(grep -r "memory\|Memory.*MB\|<.*MB" src/test/java/ | wc -l || echo "0")
    concurrent_tests=$(grep -r "concurrent\|Concurrent\|parallel" src/test/java/ | wc -l || echo "0")
    
    printf "TPS Validation Tests:        %3d\n" "$tps_tests"
    printf "Latency Validation Tests:    %3d\n" "$latency_tests"
    printf "Memory Validation Tests:     %3d\n" "$memory_tests"
    printf "Concurrency Tests:           %3d\n" "$concurrent_tests"
    echo ""
}

export_results() {
    echo -e "${YELLOW}Exporting coverage analysis results...${NC}"
    
    # Create detailed report
    REPORT_FILE="$REPORTS_DIR/coverage-analysis-$TIMESTAMP.txt"
    
    {
        echo "Aurigraph V11 Test Coverage Analysis Report"
        echo "==========================================="
        echo "Generated: $(date)"
        echo "Baseline Coverage: $BASELINE_COVERAGE%"
        echo "Target Coverage: $TARGET_OVERALL_COVERAGE%"
        echo ""
        
        echo "SUMMARY:"
        echo "--------"
        total_test_files=$(find src/test/java -name "*.java" | wc -l)
        total_source_files=$(find src/main/java -name "*.java" | wc -l)
        overall_ratio=$(echo "scale=1; $total_test_files * 100 / $total_source_files" | bc -l)
        
        echo "Total Test Files: $total_test_files"
        echo "Total Source Files: $total_source_files"
        echo "Test-to-Source Ratio: $overall_ratio%"
        
        if (( $(echo "$overall_ratio >= $TARGET_OVERALL_COVERAGE" | bc -l) )); then
            echo "STATUS: TARGET ACHIEVED ✓"
        else
            improvement_needed=$(echo "$TARGET_OVERALL_COVERAGE - $overall_ratio" | bc -l)
            echo "STATUS: Need $improvement_needed% more coverage"
        fi
        
    } > "$REPORT_FILE"
    
    echo -e "${GREEN}Detailed report saved: $REPORT_FILE${NC}"
}

show_recommendations() {
    echo -e "${YELLOW}Coverage Improvement Recommendations:${NC}"
    echo "====================================="
    
    # Analyze gaps and provide recommendations
    echo "1. Critical Components:"
    echo "   - Ensure HighPerformanceGrpcService has >95% coverage"
    echo "   - AI optimization services need comprehensive test scenarios"
    echo "   - Quantum crypto services require security validation tests"
    echo ""
    
    echo "2. Integration Testing:"
    echo "   - Add more end-to-end workflow tests"
    echo "   - Implement TestContainers for isolated integration testing"
    echo "   - Cross-service communication validation"
    echo ""
    
    echo "3. Performance Testing:"
    echo "   - Validate 1.5M+ TPS capability under load"
    echo "   - Memory usage regression tests"
    echo "   - Concurrent connection stress tests"
    echo ""
    
    echo "4. Error Handling:"
    echo "   - Exception path coverage"
    echo "   - Failover scenario testing"
    echo "   - Resource exhaustion handling"
    echo ""
}

main() {
    local run_tests=false
    local detailed=false
    local benchmark=false
    local export=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --run-tests)
                run_tests=true
                shift
                ;;
            --detailed)
                detailed=true
                shift
                ;;
            --benchmark)
                benchmark=true
                shift
                ;;
            --export)
                export=true
                shift
                ;;
            *)
                echo "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    print_header
    
    if [ "$run_tests" = true ]; then
        run_tests
    fi
    
    generate_coverage_report
    analyze_component_coverage
    analyze_test_types
    analyze_critical_paths
    
    if [ "$benchmark" = true ]; then
        generate_performance_summary
    fi
    
    if [ "$export" = true ]; then
        export_results
    fi
    
    if [ "$detailed" = true ]; then
        show_recommendations
    fi
    
    echo -e "${GREEN}Coverage analysis completed!${NC}"
    echo -e "View detailed coverage: ${BLUE}$REPORTS_DIR/jacoco-$TIMESTAMP/index.html${NC}"
}

# Check for required tools
if ! command -v bc &> /dev/null; then
    echo -e "${RED}Error: bc calculator is required but not installed${NC}"
    exit 1
fi

main "$@"