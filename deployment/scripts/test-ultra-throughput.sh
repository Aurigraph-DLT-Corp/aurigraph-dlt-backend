#!/bin/bash
#
# Ultra-High-Throughput Performance Testing Script
# Tests Aurigraph V11 with advanced optimizations targeting 3M+ TPS
#

echo "🚀 Aurigraph V11 Ultra-High-Throughput Performance Test"
echo "======================================================"

# Test configuration
BASE_URL="http://localhost:9003/api/v11"
TEST_ITERATIONS=(1000 10000 50000 100000 250000 500000)

echo "📋 Test Configuration:"
echo "   - Target: 3M+ TPS (minimum 2M+ TPS for success)"
echo "   - Optimizations: Virtual Threads + Lock-Free + Adaptive Batching"
echo "   - Base URL: $BASE_URL"
echo ""

# Function to run performance test
run_performance_test() {
    local iterations=$1
    echo "🧪 Testing with $iterations transactions..."
    
    # Create test request JSON
    local request_json="{\"iterations\": $iterations}"
    
    # Execute performance test
    local result=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$request_json" \
        "$BASE_URL/performance/ultra-throughput" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$result" ]; then
        # Parse results using basic string processing
        local tps=$(echo "$result" | grep -o '"transactionsPerSecond":[0-9.]*' | cut -d':' -f2)
        local duration=$(echo "$result" | grep -o '"durationMs":[0-9.]*' | cut -d':' -f2)
        local grade=$(echo "$result" | grep -o '"performanceGrade":"[^"]*' | cut -d':' -f2 | tr -d '"')
        
        if [ -n "$tps" ] && [ -n "$duration" ]; then
            printf "   ✅ %'d TPS in %.2f ms - %s\n" "${tps%.*}" "${duration}" "$grade"
            
            # Check if we achieved 2M+ TPS
            if (( $(echo "$tps >= 2000000" | bc -l) )); then
                echo "   🎉 ULTRA-HIGH PERFORMANCE ACHIEVED: 2M+ TPS TARGET MET!"
            fi
        else
            echo "   ❌ Failed to parse results"
        fi
    else
        echo "   ❌ Test failed or service not responding"
    fi
    
    echo ""
}

# Check if service is available
echo "🔍 Checking if Aurigraph V11 service is running..."
curl -s "$BASE_URL/health" > /dev/null
if [ $? -ne 0 ]; then
    echo "❌ Service not available at $BASE_URL"
    echo "   Please start the service with: ./mvnw quarkus:dev"
    exit 1
fi

echo "✅ Service is running"
echo ""

# Get current system stats
echo "📊 Current System Statistics:"
stats_result=$(curl -s "$BASE_URL/stats" 2>/dev/null)
if [ $? -eq 0 ] && [ -n "$stats_result" ]; then
    echo "   $stats_result"
else
    echo "   Stats not available"
fi
echo ""

# Run progressive performance tests
echo "🏃 Running Ultra-High-Throughput Tests:"
echo "======================================"

for iterations in "${TEST_ITERATIONS[@]}"; do
    run_performance_test $iterations
    sleep 1  # Brief pause between tests
done

echo "🏁 Performance Testing Complete!"
echo ""
echo "📈 Performance Targets:"
echo "   - 🥇 Exceptional: 3M+ TPS"
echo "   - 🥈 Excellent: 2M+ TPS (Sprint 2 Target)"
echo "   - 🥉 Very Good: 1M+ TPS"
echo ""
echo "🔧 To optimize performance further:"
echo "   - Increase JVM heap: -Xmx8g"
echo "   - Use G1GC: -XX:+UseG1GC"
echo "   - Native compilation: ./mvnw package -Pnative-ultra"
echo ""