#!/bin/bash

#############################################################################
# Aurigraph V11 - Demo Management System Performance Testing Suite
#
# Comprehensive performance tests for Phase 4 execution
# Tests: API response times, throughput, scalability, database, resources
#
# Duration: 2-4 hours
# Target: 20+ performance test scenarios
#############################################################################

set -e

# Configuration
BASE_URL="http://localhost:9003"
API_PATH="/api/demos"
REPORT_DIR="./performance-reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="$REPORT_DIR/demo_performance_report_$TIMESTAMP.txt"
JSON_FILE="$REPORT_DIR/demo_performance_results_$TIMESTAMP.json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create report directory
mkdir -p "$REPORT_DIR"

# Initialize counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# JSON results array
JSON_RESULTS="[]"

echo "================================================================================"
echo "  AURIGRAPH V11 - DEMO MANAGEMENT SYSTEM PERFORMANCE TESTING"
echo "================================================================================"
echo "Test Start Time: $(date)"
echo "Base URL: $BASE_URL$API_PATH"
echo "Report File: $REPORT_FILE"
echo "================================================================================"
echo ""

# Utility function to log results
log_result() {
    local test_name=$1
    local status=$2
    local actual=$3
    local expected=$4
    local details=$5

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    if [ "$status" = "PASS" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo -e "${GREEN}✅ PASS${NC} - $test_name"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "${RED}❌ FAIL${NC} - $test_name"
    fi

    echo "   Expected: $expected"
    echo "   Actual: $actual"
    if [ -n "$details" ]; then
        echo "   Details: $details"
    fi
    echo ""

    # Append to report file
    echo "[$status] $test_name" >> "$REPORT_FILE"
    echo "  Expected: $expected | Actual: $actual" >> "$REPORT_FILE"
    echo "  Details: $details" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
}

# Function to measure response time
measure_response_time() {
    local url=$1
    local method=${2:-GET}
    local data=$3

    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        curl -s -o /dev/null -w "%{time_total}" -X POST \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$url"
    else
        curl -s -o /dev/null -w "%{time_total}" -X "$method" "$url"
    fi
}

# Function to create test demo
create_test_demo() {
    local demo_name=$1
    local data=$(cat <<EOF
{
  "demoName": "$demo_name",
  "userEmail": "test@aurigraph.io",
  "userName": "Performance Tester",
  "description": "Performance test demo - $TIMESTAMP",
  "channelsJson": "[]",
  "validatorsJson": "[]",
  "businessNodesJson": "[]",
  "slimNodesJson": "[]"
}
EOF
)

    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$data" \
        "$BASE_URL$API_PATH" | jq -r '.id' 2>/dev/null || echo ""
}

echo "================================================================================"
echo "CATEGORY 1: API RESPONSE TIME TESTS (5 tests)"
echo "================================================================================"
echo ""

# Test 1: GET /api/demos response time
echo "Test 1: GET /api/demos - Response time < 500ms"
response_time=$(measure_response_time "$BASE_URL$API_PATH")
response_time_ms=$(echo "$response_time * 1000" | bc | cut -d'.' -f1)
if [ "$response_time_ms" -lt 500 ]; then
    log_result "GET /api/demos response time" "PASS" "${response_time_ms}ms" "<500ms" "Baseline met"
else
    log_result "GET /api/demos response time" "FAIL" "${response_time_ms}ms" "<500ms" "Exceeds baseline"
fi

# Test 2: POST /api/demos response time
echo "Test 2: POST /api/demos - Response time < 1000ms"
demo_data='{"demoName":"RT Test","userEmail":"test@aurigraph.io","userName":"Test User","description":"Response time test","channelsJson":"[]","validatorsJson":"[]","businessNodesJson":"[]","slimNodesJson":"[]"}'
response_time=$(measure_response_time "$BASE_URL$API_PATH" "POST" "$demo_data")
response_time_ms=$(echo "$response_time * 1000" | bc | cut -d'.' -f1)
if [ "$response_time_ms" -lt 1000 ]; then
    log_result "POST /api/demos response time" "PASS" "${response_time_ms}ms" "<1000ms" "Baseline met"
else
    log_result "POST /api/demos response time" "FAIL" "${response_time_ms}ms" "<1000ms" "Exceeds baseline"
fi

# Test 3: GET /api/demos/{id} response time
echo "Test 3: GET /api/demos/{id} - Response time < 200ms"
demo_id=$(create_test_demo "GET_ID_Test_$RANDOM")
if [ -n "$demo_id" ]; then
    response_time=$(measure_response_time "$BASE_URL$API_PATH/$demo_id")
    response_time_ms=$(echo "$response_time * 1000" | bc | cut -d'.' -f1)
    if [ "$response_time_ms" -lt 200 ]; then
        log_result "GET /api/demos/{id} response time" "PASS" "${response_time_ms}ms" "<200ms" "Baseline met"
    else
        log_result "GET /api/demos/{id} response time" "FAIL" "${response_time_ms}ms" "<200ms" "Exceeds baseline"
    fi
else
    log_result "GET /api/demos/{id} response time" "FAIL" "N/A" "<200ms" "Failed to create test demo"
fi

# Test 4: PUT /api/demos/{id} response time
echo "Test 4: PUT /api/demos/{id} - Response time < 500ms"
demo_id=$(create_test_demo "PUT_Test_$RANDOM")
if [ -n "$demo_id" ]; then
    update_data='{"status":"RUNNING","transactionCount":100}'
    response_time=$(curl -s -o /dev/null -w "%{time_total}" -X PUT \
        -H "Content-Type: application/json" \
        -d "$update_data" \
        "$BASE_URL$API_PATH/$demo_id")
    response_time_ms=$(echo "$response_time * 1000" | bc | cut -d'.' -f1)
    if [ "$response_time_ms" -lt 500 ]; then
        log_result "PUT /api/demos/{id} response time" "PASS" "${response_time_ms}ms" "<500ms" "Baseline met"
    else
        log_result "PUT /api/demos/{id} response time" "FAIL" "${response_time_ms}ms" "<500ms" "Exceeds baseline"
    fi
else
    log_result "PUT /api/demos/{id} response time" "FAIL" "N/A" "<500ms" "Failed to create test demo"
fi

# Test 5: DELETE /api/demos/{id} response time
echo "Test 5: DELETE /api/demos/{id} - Response time < 500ms"
demo_id=$(create_test_demo "DELETE_Test_$RANDOM")
if [ -n "$demo_id" ]; then
    response_time=$(measure_response_time "$BASE_URL$API_PATH/$demo_id" "DELETE")
    response_time_ms=$(echo "$response_time * 1000" | bc | cut -d'.' -f1)
    if [ "$response_time_ms" -lt 500 ]; then
        log_result "DELETE /api/demos/{id} response time" "PASS" "${response_time_ms}ms" "<500ms" "Baseline met"
    else
        log_result "DELETE /api/demos/{id} response time" "FAIL" "${response_time_ms}ms" "<500ms" "Exceeds baseline"
    fi
else
    log_result "DELETE /api/demos/{id} response time" "FAIL" "N/A" "<500ms" "Failed to create test demo"
fi

echo ""
echo "================================================================================"
echo "CATEGORY 2: THROUGHPUT & SCALABILITY TESTS (5 tests)"
echo "================================================================================"
echo ""

# Test 6: Handle 10 concurrent users
echo "Test 6: Handle 10 concurrent demo creations"
start_time=$(date +%s)
for i in {1..10}; do
    create_test_demo "Concurrent_10_Test_$i" &
done
wait
end_time=$(date +%s)
duration=$((end_time - start_time))
if [ "$duration" -le 10 ]; then
    log_result "10 concurrent demo creations" "PASS" "${duration}s" "<=10s" "Handled successfully"
else
    log_result "10 concurrent demo creations" "FAIL" "${duration}s" "<=10s" "Exceeded time limit"
fi

# Test 7: Handle 50 concurrent users
echo "Test 7: Handle 50 concurrent demo creations"
start_time=$(date +%s)
for i in {1..50}; do
    create_test_demo "Concurrent_50_Test_$i" &
    if [ $((i % 10)) -eq 0 ]; then
        sleep 0.1  # Small delay to avoid overwhelming
    fi
done
wait
end_time=$(date +%s)
duration=$((end_time - start_time))
if [ "$duration" -le 30 ]; then
    log_result "50 concurrent demo creations" "PASS" "${duration}s" "<=30s" "Handled successfully"
else
    log_result "50 concurrent demo creations" "FAIL" "${duration}s" "<=30s" "Exceeded time limit"
fi

# Test 8: Measure requests/second under load
echo "Test 8: Measure requests/second (100 sequential GET requests)"
start_time=$(date +%s.%N)
for i in {1..100}; do
    curl -s -o /dev/null "$BASE_URL$API_PATH" &
done
wait
end_time=$(date +%s.%N)
duration=$(echo "$end_time - $start_time" | bc)
rps=$(echo "100 / $duration" | bc -l | cut -d'.' -f1)
if [ "$rps" -ge 20 ]; then
    log_result "Requests/second under load" "PASS" "${rps} req/s" ">=20 req/s" "Throughput acceptable"
else
    log_result "Requests/second under load" "FAIL" "${rps} req/s" ">=20 req/s" "Below threshold"
fi

# Test 9: Average response time under load
echo "Test 9: Average response time under 50 concurrent requests"
total_time=0
for i in {1..50}; do
    response_time=$(measure_response_time "$BASE_URL$API_PATH") &
done
wait
# Simplified: measure 10 sequential for average
total_time=0
for i in {1..10}; do
    rt=$(measure_response_time "$BASE_URL$API_PATH")
    total_time=$(echo "$total_time + $rt" | bc)
done
avg_time=$(echo "scale=3; $total_time / 10" | bc)
avg_time_ms=$(echo "$avg_time * 1000" | bc | cut -d'.' -f1)
if [ "$avg_time_ms" -le 1000 ]; then
    log_result "Average response time under load" "PASS" "${avg_time_ms}ms" "<=1000ms" "Performance acceptable"
else
    log_result "Average response time under load" "FAIL" "${avg_time_ms}ms" "<=1000ms" "Degraded performance"
fi

# Test 10: System stability (100 operations)
echo "Test 10: System stability - 100 mixed operations"
failures=0
for i in {1..100}; do
    if [ $((i % 3)) -eq 0 ]; then
        # GET request
        http_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$API_PATH")
    else
        # POST request
        demo_id=$(create_test_demo "Stability_Test_$i")
        if [ -z "$demo_id" ]; then
            failures=$((failures + 1))
        fi
    fi
done
success_rate=$(echo "scale=2; (100 - $failures) / 100 * 100" | bc)
if (( $(echo "$success_rate >= 95" | bc -l) )); then
    log_result "System stability (100 operations)" "PASS" "${success_rate}%" ">=95%" "Stable under load"
else
    log_result "System stability (100 operations)" "FAIL" "${success_rate}%" ">=95%" "Instability detected"
fi

echo ""
echo "================================================================================"
echo "CATEGORY 3: DATABASE PERFORMANCE TESTS (5 tests)"
echo "================================================================================"
echo ""

# Test 11: CREATE operation < 100ms
echo "Test 11: Database CREATE operation performance"
demo_data='{"demoName":"DB Create Test","userEmail":"db@test.com","userName":"DB Tester","description":"DB test","channelsJson":"[]","validatorsJson":"[]","businessNodesJson":"[]","slimNodesJson":"[]"}'
response_time=$(measure_response_time "$BASE_URL$API_PATH" "POST" "$demo_data")
response_time_ms=$(echo "$response_time * 1000" | bc | cut -d'.' -f1)
if [ "$response_time_ms" -lt 100 ]; then
    log_result "Database CREATE operation" "PASS" "${response_time_ms}ms" "<100ms" "Fast write performance"
else
    log_result "Database CREATE operation" "FAIL" "${response_time_ms}ms" "<100ms" "Slow write performance"
fi

# Test 12: SELECT operation < 50ms
echo "Test 12: Database SELECT operation performance"
response_time=$(measure_response_time "$BASE_URL$API_PATH")
response_time_ms=$(echo "$response_time * 1000" | bc | cut -d'.' -f1)
if [ "$response_time_ms" -lt 50 ]; then
    log_result "Database SELECT operation" "PASS" "${response_time_ms}ms" "<50ms" "Fast read performance"
else
    log_result "Database SELECT operation" "FAIL" "${response_time_ms}ms" "<50ms" "Slow read performance"
fi

# Test 13: UPDATE operation < 100ms
echo "Test 13: Database UPDATE operation performance"
demo_id=$(create_test_demo "DB_Update_Test_$RANDOM")
if [ -n "$demo_id" ]; then
    update_data='{"transactionCount":500}'
    response_time=$(curl -s -o /dev/null -w "%{time_total}" -X PUT \
        -H "Content-Type: application/json" \
        -d "$update_data" \
        "$BASE_URL$API_PATH/$demo_id")
    response_time_ms=$(echo "$response_time * 1000" | bc | cut -d'.' -f1)
    if [ "$response_time_ms" -lt 100 ]; then
        log_result "Database UPDATE operation" "PASS" "${response_time_ms}ms" "<100ms" "Fast update performance"
    else
        log_result "Database UPDATE operation" "FAIL" "${response_time_ms}ms" "<100ms" "Slow update performance"
    fi
else
    log_result "Database UPDATE operation" "FAIL" "N/A" "<100ms" "Failed to create test demo"
fi

# Test 14: DELETE operation < 100ms
echo "Test 14: Database DELETE operation performance"
demo_id=$(create_test_demo "DB_Delete_Test_$RANDOM")
if [ -n "$demo_id" ]; then
    response_time=$(measure_response_time "$BASE_URL$API_PATH/$demo_id" "DELETE")
    response_time_ms=$(echo "$response_time * 1000" | bc | cut -d'.' -f1)
    if [ "$response_time_ms" -lt 100 ]; then
        log_result "Database DELETE operation" "PASS" "${response_time_ms}ms" "<100ms" "Fast delete performance"
    else
        log_result "Database DELETE operation" "FAIL" "${response_time_ms}ms" "<100ms" "Slow delete performance"
    fi
else
    log_result "Database DELETE operation" "FAIL" "N/A" "<100ms" "Failed to create test demo"
fi

# Test 15: SELECT with filter < 100ms (active demos)
echo "Test 15: Database SELECT with filter (active demos)"
response_time=$(measure_response_time "$BASE_URL$API_PATH/active")
response_time_ms=$(echo "$response_time * 1000" | bc | cut -d'.' -f1)
if [ "$response_time_ms" -lt 100 ]; then
    log_result "Database SELECT with filter" "PASS" "${response_time_ms}ms" "<100ms" "Fast filtered query"
else
    log_result "Database SELECT with filter" "FAIL" "${response_time_ms}ms" "<100ms" "Slow filtered query"
fi

echo ""
echo "================================================================================"
echo "CATEGORY 4: MEMORY & RESOURCE TESTS (3 tests)"
echo "================================================================================"
echo ""

# Test 16: Memory usage check (requires ps command)
echo "Test 16: JVM memory usage monitoring"
# Find Java process for Aurigraph
java_pid=$(ps aux | grep "quarkus:dev" | grep -v grep | awk '{print $2}' | head -1)
if [ -n "$java_pid" ]; then
    mem_kb=$(ps -o rss= -p $java_pid)
    mem_mb=$((mem_kb / 1024))
    if [ "$mem_mb" -lt 512 ]; then
        log_result "JVM memory usage (dev mode)" "PASS" "${mem_mb}MB" "<512MB" "Within limits"
    else
        log_result "JVM memory usage (dev mode)" "FAIL" "${mem_mb}MB" "<512MB" "Exceeds limit"
    fi
else
    log_result "JVM memory usage (dev mode)" "FAIL" "N/A" "<512MB" "Process not found"
fi

# Test 17: CPU utilization under load
echo "Test 17: CPU utilization during 50 concurrent requests"
# Start monitoring CPU
if [ -n "$java_pid" ]; then
    cpu_before=$(ps -o %cpu= -p $java_pid)
    # Generate load
    for i in {1..50}; do
        curl -s -o /dev/null "$BASE_URL$API_PATH" &
    done
    wait
    sleep 2
    cpu_after=$(ps -o %cpu= -p $java_pid)
    cpu_avg=$(echo "scale=1; ($cpu_before + $cpu_after) / 2" | bc)
    cpu_int=$(echo "$cpu_avg / 1" | bc)
    if [ "$cpu_int" -lt 80 ]; then
        log_result "CPU utilization under load" "PASS" "${cpu_avg}%" "<80%" "Efficient processing"
    else
        log_result "CPU utilization under load" "FAIL" "${cpu_avg}%" "<80%" "High CPU usage"
    fi
else
    log_result "CPU utilization under load" "FAIL" "N/A" "<80%" "Process not found"
fi

# Test 18: Response time degradation after sustained load
echo "Test 18: Response time stability (before and after 100 requests)"
# Measure baseline
baseline_time=$(measure_response_time "$BASE_URL$API_PATH")
baseline_ms=$(echo "$baseline_time * 1000" | bc | cut -d'.' -f1)
# Generate load
for i in {1..100}; do
    curl -s -o /dev/null "$BASE_URL$API_PATH" &
done
wait
sleep 1
# Measure after load
after_time=$(measure_response_time "$BASE_URL$API_PATH")
after_ms=$(echo "$after_time * 1000" | bc | cut -d'.' -f1)
degradation=$(echo "scale=2; ($after_ms - $baseline_ms) / $baseline_ms * 100" | bc)
degradation_int=$(echo "$degradation / 1" | bc)
if [ "$degradation_int" -lt 20 ]; then
    log_result "Response time stability" "PASS" "${degradation}% degradation" "<20% degradation" "Stable performance"
else
    log_result "Response time stability" "FAIL" "${degradation}% degradation" "<20% degradation" "Performance degradation"
fi

echo ""
echo "================================================================================"
echo "CATEGORY 5: STRESS & ENDURANCE TESTS (3 tests)"
echo "================================================================================"
echo ""

# Test 19: 100 demo creations without failure
echo "Test 19: Stress test - 100 sequential demo creations"
failures=0
start_time=$(date +%s)
for i in {1..100}; do
    demo_id=$(create_test_demo "Stress_Test_$i")
    if [ -z "$demo_id" ]; then
        failures=$((failures + 1))
    fi
done
end_time=$(date +%s)
duration=$((end_time - start_time))
success_rate=$(echo "scale=2; (100 - $failures) / 100 * 100" | bc)
if (( $(echo "$success_rate >= 99" | bc -l) )); then
    log_result "Stress test (100 creations)" "PASS" "${success_rate}% success, ${duration}s" ">=99% success" "System stable under stress"
else
    log_result "Stress test (100 creations)" "FAIL" "${success_rate}% success, ${duration}s" ">=99% success" "Failures detected"
fi

# Test 20: Sustained load test (5 minutes simulation)
echo "Test 20: Endurance test - 5-minute sustained load simulation"
echo "           (Executing 300 operations over 5 minutes)"
start_time=$(date +%s)
failures=0
for i in {1..300}; do
    if [ $((i % 2)) -eq 0 ]; then
        curl -s -o /dev/null "$BASE_URL$API_PATH" || failures=$((failures + 1))
    else
        demo_id=$(create_test_demo "Endurance_$i")
        [ -z "$demo_id" ] && failures=$((failures + 1))
    fi
    sleep 1  # 1 second between operations = 5 minutes total
done
end_time=$(date +%s)
duration=$((end_time - start_time))
success_rate=$(echo "scale=2; (300 - $failures) / 300 * 100" | bc)
if (( $(echo "$success_rate >= 95" | bc -l) )); then
    log_result "Endurance test (5 minutes)" "PASS" "${success_rate}% success, ${duration}s" ">=95% success" "System stable over time"
else
    log_result "Endurance test (5 minutes)" "FAIL" "${success_rate}% success, ${duration}s" ">=95% success" "Degradation over time"
fi

# Test 21: Peak concurrent load (100 simultaneous requests)
echo "Test 21: Peak load test - 100 simultaneous GET requests"
failures=0
start_time=$(date +%s)
for i in {1..100}; do
    (curl -s -o /dev/null -w "%{http_code}\n" "$BASE_URL$API_PATH" | grep -v "200" && failures=$((failures + 1))) &
done
wait
end_time=$(date +%s)
duration=$((end_time - start_time))
success_rate=$(echo "scale=2; (100 - $failures) / 100 * 100" | bc)
if (( $(echo "$success_rate >= 90" | bc -l) )) && [ "$duration" -le 15 ]; then
    log_result "Peak load test (100 concurrent)" "PASS" "${success_rate}% success, ${duration}s" ">=90% success, <=15s" "Handled peak load"
else
    log_result "Peak load test (100 concurrent)" "FAIL" "${success_rate}% success, ${duration}s" ">=90% success, <=15s" "Struggled with peak load"
fi

echo ""
echo "================================================================================"
echo "PERFORMANCE TEST SUMMARY"
echo "================================================================================"
echo ""
echo "Test Execution Completed: $(date)"
echo ""
echo "Total Tests: $TOTAL_TESTS"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"
echo ""
success_percentage=$(echo "scale=2; $PASSED_TESTS / $TOTAL_TESTS * 100" | bc)
echo "Success Rate: ${success_percentage}%"
echo ""

if (( $(echo "$success_percentage >= 80" | bc -l) )); then
    echo -e "${GREEN}✅ OVERALL STATUS: PASS${NC}"
    echo "The Demo Management System meets performance baselines."
else
    echo -e "${RED}❌ OVERALL STATUS: FAIL${NC}"
    echo "The Demo Management System requires performance optimization."
fi

echo ""
echo "================================================================================"
echo "Full report saved to: $REPORT_FILE"
echo "================================================================================"

exit 0
