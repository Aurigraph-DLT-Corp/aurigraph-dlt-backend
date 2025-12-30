#!/bin/bash

# Aurigraph V11 - 2M+ TPS Performance Benchmark Script
# This script runs comprehensive performance tests to validate 2M+ TPS capability

echo "========================================"
echo "🚀 Aurigraph V11 - 2M+ TPS Benchmark"
echo "========================================"
echo

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    local status=$1
    local message=$2
    if [ "$status" == "OK" ]; then
        echo -e "${GREEN}✓${NC} $message"
    elif [ "$status" == "INFO" ]; then
        echo -e "${BLUE}ℹ${NC} $message"
    elif [ "$status" == "WARN" ]; then
        echo -e "${YELLOW}⚠${NC} $message"
    else
        echo -e "${RED}✗${NC} $message"
    fi
}

# Check Java installation
echo "📋 Checking Prerequisites..."
echo "----------------------------"

if ! command -v java &> /dev/null; then
    print_status "ERROR" "Java is not installed. Please install Java 21+"
    echo "Visit: https://adoptium.net/temurin/releases/?version=21"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f 2 | cut -d'.' -f 1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    print_status "ERROR" "Java 21+ required (found Java $JAVA_VERSION)"
    exit 1
fi
print_status "OK" "Java $JAVA_VERSION detected"

# Check Maven Wrapper
if [ ! -f "./mvnw" ]; then
    print_status "ERROR" "Maven wrapper not found"
    exit 1
fi
print_status "OK" "Maven wrapper found"

# Check if performance files exist
if [ ! -f "src/main/java/io/aurigraph/v11/performance/AdvancedPerformanceService.java" ]; then
    print_status "ERROR" "Advanced Performance Service not found"
    exit 1
fi
print_status "OK" "Performance service files found"

echo
echo "🔧 System Information..."
echo "------------------------"
print_status "INFO" "CPU Cores: $(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 'Unknown')"
print_status "INFO" "Total Memory: $(( $(sysctl -n hw.memsize 2>/dev/null || free -b | awk '/Mem:/ {print $2}' 2>/dev/null || echo 0) / 1024 / 1024 / 1024 )) GB"
print_status "INFO" "OS: $(uname -s) $(uname -r)"

echo
echo "🏗️ Building Project..."
echo "---------------------"
print_status "INFO" "Compiling Java sources..."
./mvnw clean compile -DskipTests

if [ $? -ne 0 ]; then
    print_status "ERROR" "Build failed"
    exit 1
fi
print_status "OK" "Build successful"

echo
echo "⚡ Running Performance Tests..."
echo "-------------------------------"

# Test 1: High Throughput (100K TPS)
echo
print_status "INFO" "Test 1: High Throughput (100K TPS target)"
echo "----------------------------------------"
./mvnw test -Dtest=AdvancedPerformanceTest#testHighThroughputPerformance 2>&1 | \
    grep -E "(Achieved|TPS|target|PASSED|FAILED)"

# Test 2: Ultra High Throughput (1M+ TPS)
echo
print_status "INFO" "Test 2: Ultra High Throughput (1M+ TPS target)"
echo "----------------------------------------------"
./mvnw test -Dtest=AdvancedPerformanceTest#testUltraHighThroughputPerformance 2>&1 | \
    grep -E "(Achieved|TPS|target|PASSED|FAILED)"

# Test 3: Maximum Performance (2M+ TPS)
echo
print_status "INFO" "Test 3: Maximum Performance (2M+ TPS target)"
echo "--------------------------------------------"
./mvnw test -Dtest=AdvancedPerformanceTest#testMaximumPerformance 2>&1 | \
    grep -E "(Achieved|TPS|target|PASSED|FAILED|ACHIEVED)"

# Test 4: Sustained Performance
echo
print_status "INFO" "Test 4: Sustained Performance (60s duration)"
echo "--------------------------------------------"
./mvnw test -Dtest=AdvancedPerformanceTest#testSustainedPerformance 2>&1 | \
    grep -E "(Sustained|TPS|seconds|PASSED|FAILED)"

echo
echo "🎯 Running Live Benchmark via REST API..."
echo "-----------------------------------------"

# Start the application in background
print_status "INFO" "Starting Quarkus application..."
./mvnw quarkus:dev -DskipTests > /tmp/quarkus.log 2>&1 &
QUARKUS_PID=$!

# Wait for application to start
sleep 10

# Check if application is running
if ! curl -s http://localhost:9003/api/v11/performance/advanced/health > /dev/null 2>&1; then
    print_status "WARN" "Application not responding on port 9003"
    kill $QUARKUS_PID 2>/dev/null
else
    print_status "OK" "Application started successfully"
    
    # Start performance service
    print_status "INFO" "Starting performance service..."
    curl -X POST http://localhost:9003/api/v11/performance/advanced/start 2>/dev/null
    
    sleep 2
    
    # Run 2M+ TPS benchmark
    print_status "INFO" "Running 2M+ TPS benchmark (30 seconds)..."
    echo
    BENCHMARK_RESULT=$(curl -s -X POST http://localhost:9003/api/v11/performance/advanced/benchmark \
        -H "Content-Type: application/json" \
        -d '{"duration": 30, "targetTPS": 2000000}' 2>/dev/null)
    
    if [ ! -z "$BENCHMARK_RESULT" ]; then
        echo "$BENCHMARK_RESULT" | python3 -m json.tool 2>/dev/null || echo "$BENCHMARK_RESULT"
    fi
    
    # Get final metrics
    print_status "INFO" "Getting final metrics..."
    curl -s http://localhost:9003/api/v11/performance/advanced/metrics 2>/dev/null | \
        python3 -m json.tool 2>/dev/null | grep -E "(currentTPS|peakTPS|totalTransactions)"
    
    # Stop the application
    print_status "INFO" "Stopping application..."
    kill $QUARKUS_PID 2>/dev/null
fi

echo
echo "📊 Benchmark Summary"
echo "--------------------"

# Check test results
if [ -f "target/surefire-reports/TEST-io.aurigraph.v11.performance.AdvancedPerformanceTest.xml" ]; then
    TESTS_RUN=$(grep -oP 'tests="\K[^"]+' target/surefire-reports/TEST-io.aurigraph.v11.performance.AdvancedPerformanceTest.xml 2>/dev/null || echo "0")
    FAILURES=$(grep -oP 'failures="\K[^"]+' target/surefire-reports/TEST-io.aurigraph.v11.performance.AdvancedPerformanceTest.xml 2>/dev/null || echo "0")
    ERRORS=$(grep -oP 'errors="\K[^"]+' target/surefire-reports/TEST-io.aurigraph.v11.performance.AdvancedPerformanceTest.xml 2>/dev/null || echo "0")
    
    print_status "INFO" "Tests Run: $TESTS_RUN"
    print_status "INFO" "Failures: $FAILURES"
    print_status "INFO" "Errors: $ERRORS"
    
    if [ "$FAILURES" -eq 0 ] && [ "$ERRORS" -eq 0 ]; then
        print_status "OK" "All performance tests passed!"
    else
        print_status "WARN" "Some tests failed - review results above"
    fi
fi

echo
echo "========================================"
echo "🏁 Performance Benchmark Complete"
echo "========================================"
echo
echo "📋 Next Steps:"
echo "  1. Review test results above"
echo "  2. Check logs in target/surefire-reports/"
echo "  3. For native build: ./mvnw package -Pnative-ultra"
echo "  4. For production: Deploy native executable"
echo
echo "🎯 Performance Targets:"
echo "  • Minimum: 1,000,000 TPS"
echo "  • Target:  2,000,000 TPS"
echo "  • Stretch: 3,000,000+ TPS"
echo