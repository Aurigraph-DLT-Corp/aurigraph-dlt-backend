#!/bin/bash

# Simple, focused performance tests for Demo Management System
set -e

BASE_URL="http://localhost:9003/api/demos"
REPORT_FILE="demo-performance-report-$(date +%Y%m%d_%H%M%S).txt"

echo "=================================================="
echo "Demo Management System - Performance Test Report"
echo "Test Time: $(date)"
echo "=================================================="
echo "" | tee "$REPORT_FILE"

# Test 1: GET /api/demos response time
echo "Test 1: GET /api/demos - Response Time" | tee -a "$REPORT_FILE"
time1=$(curl -s -o /dev/null -w "%{time_total}" "$BASE_URL" 2>/dev/null)
time1_ms=$(echo "$time1 * 1000 / 1" | bc)
status1=$([ "$time1_ms" -lt 500 ] && echo "PASS" || echo "FAIL")
echo "  Result: $time1_ms ms (Baseline: <500ms) - $status1" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

# Test 2: POST /api/demos response time
echo "Test 2: POST /api/demos - Create Demo Response Time" | tee -a "$REPORT_FILE"
demo_json='{"demoName":"PerfTest","userEmail":"test@aurigraph.io","userName":"Tester","description":"Performance test","channelsJson":"[]","validatorsJson":"[]","businessNodesJson":"[]","slimNodesJson":"[]"}'
time2=$(curl -s -o /dev/null -w "%{time_total}" -X POST -H "Content-Type: application/json" -d "$demo_json" "$BASE_URL" 2>/dev/null)
time2_ms=$(echo "$time2 * 1000 / 1" | bc)
status2=$([ "$time2_ms" -lt 1000 ] && echo "PASS" || echo "FAIL")
echo "  Result: $time2_ms ms (Baseline: <1000ms) - $status2" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

# Test 3: GET demo by ID
echo "Test 3: GET /api/demos/{id} - Single Demo Response Time" | tee -a "$REPORT_FILE"
# Get first demo ID
demo_id=$(curl -s "$BASE_URL" 2>/dev/null | jq -r '.[0].id' 2>/dev/null || echo "")
if [ -n "$demo_id" ]; then
    time3=$(curl -s -o /dev/null -w "%{time_total}" "$BASE_URL/$demo_id" 2>/dev/null)
    time3_ms=$(echo "$time3 * 1000 / 1" | bc)
    status3=$([ "$time3_ms" -lt 200 ] && echo "PASS" || echo "FAIL")
    echo "  Result: $time3_ms ms (Baseline: <200ms) - $status3" | tee -a "$REPORT_FILE"
else
    echo "  Result: SKIPPED - No demos found" | tee -a "$REPORT_FILE"
    status3="SKIP"
fi
echo "" | tee -a "$REPORT_FILE"

# Test 4: 10 concurrent requests
echo "Test 4: Throughput - 10 Concurrent GET Requests" | tee -a "$REPORT_FILE"
start=$(date +%s)
for i in {1..10}; do
    curl -s -o /dev/null "$BASE_URL" &
done
wait
end=$(date +%s)
duration=$((end - start))
status4=$([ "$duration" -le 5 ] && echo "PASS" || echo "FAIL")
echo "  Result: ${duration}s (Baseline: <=5s) - $status4" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

# Test 5: Database query with filter
echo "Test 5: Database Filter - GET /api/demos/active" | tee -a "$REPORT_FILE"
time5=$(curl -s -o /dev/null -w "%{time_total}" "$BASE_URL/active" 2>/dev/null)
time5_ms=$(echo "$time5 * 1000 / 1" | bc)
status5=$([ "$time5_ms" -lt 100 ] && echo "PASS" || echo "FAIL")
echo "  Result: $time5_ms ms (Baseline: <100ms) - $status5" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

# Test 6: Memory check
echo "Test 6: Memory Usage - JVM Process" | tee -a "$REPORT_FILE"
java_pid=$(ps aux | grep "quarkus:dev" | grep -v grep | awk '{print $2}' | head -1)
if [ -n "$java_pid" ]; then
    mem_kb=$(ps -o rss= -p $java_pid 2>/dev/null || echo "0")
    mem_mb=$((mem_kb / 1024))
    status6=$([ "$mem_mb" -lt 512 ] && echo "PASS" || echo "FAIL")
    echo "  Result: ${mem_mb}MB (Baseline: <512MB) - $status6" | tee -a "$REPORT_FILE"
else
    echo "  Result: SKIP - Process not found" | tee -a "$REPORT_FILE"
    status6="SKIP"
fi
echo "" | tee -a "$REPORT_FILE"

# Test 7: Stress test - 50 sequential operations
echo "Test 7: Stress Test - 50 Sequential GET Requests" | tee -a "$REPORT_FILE"
failures=0
start=$(date +%s)
for i in {1..50}; do
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL" 2>/dev/null)
    [ "$http_code" != "200" ] && failures=$((failures + 1))
done
end=$(date +%s)
duration=$((end - start))
success_rate=$(echo "scale=1; (50 - $failures) * 100 / 50" | bc)
status7=$(echo "$success_rate >= 95" | bc -l | grep -q 1 && echo "PASS" || echo "FAIL")
echo "  Result: ${success_rate}% success rate in ${duration}s (Baseline: >=95%) - $status7" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

# Count results
total=7
passed=$(echo "$status1 $status2 $status3 $status4 $status5 $status6 $status7" | grep -o "PASS" | wc -l)
failed=$(echo "$status1 $status2 $status3 $status4 $status5 $status6 $status7" | grep -o "FAIL" | wc -l)
skipped=$(echo "$status1 $status2 $status3 $status4 $status5 $status6 $status7" | grep -o "SKIP" | wc -l)

echo "=================================================="  | tee -a "$REPORT_FILE"
echo "SUMMARY" | tee -a "$REPORT_FILE"
echo "=================================================="  | tee -a "$REPORT_FILE"
echo "Total Tests: $total" | tee -a "$REPORT_FILE"
echo "Passed: $passed" | tee -a "$REPORT_FILE"
echo "Failed: $failed" | tee -a "$REPORT_FILE"
echo "Skipped: $skipped" | tee -a "$REPORT_FILE"

success_percent=$(echo "scale=1; $passed * 100 / $total" | bc)
echo "Success Rate: ${success_percent}%" | tee -a "$REPORT_FILE"

if [ "$passed" -ge 5 ]; then
    echo "Overall Status: ✅ PASS" | tee -a "$REPORT_FILE"
else
    echo "Overall Status: ❌ FAIL" | tee -a "$REPORT_FILE"
fi

echo "=================================================="  | tee -a "$REPORT_FILE"
echo "Report saved to: $REPORT_FILE"
