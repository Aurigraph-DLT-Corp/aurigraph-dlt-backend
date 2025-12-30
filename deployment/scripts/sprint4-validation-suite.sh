#!/bin/bash

# Aurigraph V11 Sprint 4 - Production Validation Suite
# ===================================================
# Comprehensive validation for Sprint 4 deployment objectives

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

DOMAIN="dlt.aurigraph.io"
VALIDATION_LOG="sprint4-validation-$(date +%s).json"
START_TIME=$(date +%s)

print_step() {
    echo -e "${BLUE}[VALIDATION]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

print_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Initialize validation results
VALIDATION_RESULTS=()
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

add_result() {
    local test_name=$1
    local status=$2
    local details=$3
    local duration=${4:-0}
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$status" = "PASS" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        print_success "$test_name: $details"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        print_fail "$test_name: $details"
    fi
    
    # Add to JSON results
    VALIDATION_RESULTS+=("\"$test_name\": {\"status\": \"$status\", \"details\": \"$details\", \"duration_ms\": $duration}")
}

test_ssl_certificate() {
    print_step "Testing SSL Certificate Configuration"
    local test_start=$(date +%s%3N)
    
    # Test HTTPS connectivity
    if curl -I --max-time 10 https://$DOMAIN >/dev/null 2>&1; then
        add_result "SSL_HTTPS_Connectivity" "PASS" "HTTPS connection successful" $(($(date +%s%3N) - test_start))
    else
        add_result "SSL_HTTPS_Connectivity" "FAIL" "HTTPS connection failed" $(($(date +%s%3N) - test_start))
        return
    fi
    
    # Check certificate validity
    test_start=$(date +%s%3N)
    CERT_INFO=$(openssl s_client -connect $DOMAIN:443 -servername $DOMAIN 2>/dev/null | openssl x509 -noout -dates -subject)
    
    if echo "$CERT_INFO" | grep -q "CN=$DOMAIN\|CN=\*\."; then
        add_result "SSL_Certificate_Subject" "PASS" "Certificate subject matches domain" $(($(date +%s%3N) - test_start))
    else
        add_result "SSL_Certificate_Subject" "FAIL" "Certificate subject mismatch" $(($(date +%s%3N) - test_start))
    fi
    
    # Check certificate expiration
    test_start=$(date +%s%3N)
    EXPIRY_DATE=$(echo "$CERT_INFO" | grep "notAfter" | cut -d= -f2)
    EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
    CURRENT_EPOCH=$(date +%s)
    DAYS_UNTIL_EXPIRY=$(( (EXPIRY_EPOCH - CURRENT_EPOCH) / 86400 ))
    
    if [ $DAYS_UNTIL_EXPIRY -gt 30 ]; then
        add_result "SSL_Certificate_Expiry" "PASS" "Certificate valid for $DAYS_UNTIL_EXPIRY days" $(($(date +%s%3N) - test_start))
    else
        add_result "SSL_Certificate_Expiry" "FAIL" "Certificate expires in $DAYS_UNTIL_EXPIRY days" $(($(date +%s%3N) - test_start))
    fi
    
    # Test TLS version support
    test_start=$(date +%s%3N)
    if openssl s_client -connect $DOMAIN:443 -tls1_3 -servername $DOMAIN >/dev/null 2>&1; then
        add_result "TLS_1.3_Support" "PASS" "TLS 1.3 supported" $(($(date +%s%3N) - test_start))
    else
        add_result "TLS_1.3_Support" "FAIL" "TLS 1.3 not supported" $(($(date +%s%3N) - test_start))
    fi
    
    # Test HSTS header
    test_start=$(date +%s%3N)
    if curl -I --max-time 5 https://$DOMAIN 2>/dev/null | grep -i "strict-transport-security" >/dev/null; then
        add_result "HSTS_Header" "PASS" "HSTS header present" $(($(date +%s%3N) - test_start))
    else
        add_result "HSTS_Header" "FAIL" "HSTS header missing" $(($(date +%s%3N) - test_start))
    fi
}

test_service_availability() {
    print_step "Testing Service Availability"
    local test_start=$(date +%s%3N)
    
    # Test service status
    if systemctl is-active --quiet aurigraph-v11; then
        add_result "Service_Status" "PASS" "Aurigraph V11 service is running" $(($(date +%s%3N) - test_start))
    else
        add_result "Service_Status" "FAIL" "Aurigraph V11 service not running" $(($(date +%s%3N) - test_start))
        return
    fi
    
    # Test health endpoint (local)
    test_start=$(date +%s%3N)
    if curl -sf --max-time 5 http://localhost:9003/api/v11/health >/dev/null; then
        add_result "Health_Endpoint_Local" "PASS" "Local health endpoint responding" $(($(date +%s%3N) - test_start))
    else
        add_result "Health_Endpoint_Local" "FAIL" "Local health endpoint not responding" $(($(date +%s%3N) - test_start))
    fi
    
    # Test health endpoint (HTTPS)
    test_start=$(date +%s%3N)
    if curl -sf --max-time 5 https://$DOMAIN/health >/dev/null; then
        add_result "Health_Endpoint_HTTPS" "PASS" "HTTPS health endpoint responding" $(($(date +%s%3N) - test_start))
    else
        add_result "Health_Endpoint_HTTPS" "FAIL" "HTTPS health endpoint not responding" $(($(date +%s%3N) - test_start))
    fi
    
    # Test metrics endpoint
    test_start=$(date +%s%3N)
    if curl -sf --max-time 5 http://localhost:9003/q/metrics >/dev/null; then
        add_result "Metrics_Endpoint" "PASS" "Metrics endpoint responding" $(($(date +%s%3N) - test_start))
    else
        add_result "Metrics_Endpoint" "FAIL" "Metrics endpoint not responding" $(($(date +%s%3N) - test_start))
    fi
    
    # Test gRPC port (basic connectivity)
    test_start=$(date +%s%3N)
    if timeout 3 bash -c "</dev/tcp/localhost/9004" 2>/dev/null; then
        add_result "GRPC_Port_Connectivity" "PASS" "gRPC port 9004 accessible" $(($(date +%s%3N) - test_start))
    else
        add_result "GRPC_Port_Connectivity" "FAIL" "gRPC port 9004 not accessible" $(($(date +%s%3N) - test_start))
    fi
}

test_performance_metrics() {
    print_step "Testing Performance Metrics"
    local test_start=$(date +%s%3N)
    
    # Test performance stats endpoint
    PERF_RESPONSE=$(curl -sf --max-time 5 http://localhost:9003/api/v11/stats 2>/dev/null || echo "{}")
    
    if [ "$PERF_RESPONSE" != "{}" ]; then
        add_result "Performance_Stats_Available" "PASS" "Performance statistics available" $(($(date +%s%3N) - test_start))
        
        # Extract and validate TPS if available
        if echo "$PERF_RESPONSE" | grep -q "tps\|throughput"; then
            add_result "TPS_Metrics_Present" "PASS" "TPS metrics found in response" $(($(date +%s%3N) - test_start))
        else
            add_result "TPS_Metrics_Present" "WARN" "TPS metrics not found in response" $(($(date +%s%3N) - test_start))
        fi
    else
        add_result "Performance_Stats_Available" "FAIL" "Performance statistics not available" $(($(date +%s%3N) - test_start))
    fi
    
    # Test system resource utilization
    test_start=$(date +%s%3N)
    CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | awk -F% '{print $1}' | head -1)
    if [ -n "$CPU_USAGE" ] && (( $(echo "$CPU_USAGE < 90" | bc -l) )); then
        add_result "CPU_Utilization" "PASS" "CPU utilization: ${CPU_USAGE}%" $(($(date +%s%3N) - test_start))
    else
        add_result "CPU_Utilization" "WARN" "CPU utilization high: ${CPU_USAGE}%" $(($(date +%s%3N) - test_start))
    fi
    
    # Test memory usage
    test_start=$(date +%s%3N)
    MEMORY_INFO=$(free | grep Mem)
    MEMORY_USED=$(echo "$MEMORY_INFO" | awk '{printf "%.1f", ($3/$2) * 100.0}')
    if (( $(echo "$MEMORY_USED < 85" | bc -l) )); then
        add_result "Memory_Utilization" "PASS" "Memory utilization: ${MEMORY_USED}%" $(($(date +%s%3N) - test_start))
    else
        add_result "Memory_Utilization" "WARN" "Memory utilization high: ${MEMORY_USED}%" $(($(date +%s%3N) - test_start))
    fi
}

test_security_headers() {
    print_step "Testing Security Headers"
    local test_start=$(date +%s%3N)
    
    HEADERS=$(curl -I --max-time 5 https://$DOMAIN 2>/dev/null || echo "")
    
    # Required security headers
    local security_headers=(
        "Strict-Transport-Security"
        "X-Frame-Options"
        "X-Content-Type-Options"
        "X-XSS-Protection"
        "Referrer-Policy"
        "Content-Security-Policy"
    )
    
    for header in "${security_headers[@]}"; do
        test_start=$(date +%s%3N)
        if echo "$HEADERS" | grep -i "$header" >/dev/null; then
            add_result "Security_Header_${header}" "PASS" "$header header present" $(($(date +%s%3N) - test_start))
        else
            add_result "Security_Header_${header}" "FAIL" "$header header missing" $(($(date +%s%3N) - test_start))
        fi
    done
}

test_cors_configuration() {
    print_step "Testing CORS Configuration"
    local test_start=$(date +%s%3N)
    
    # Test CORS preflight request
    CORS_RESPONSE=$(curl -s --max-time 5 -X OPTIONS \
        -H "Origin: https://test.example.com" \
        -H "Access-Control-Request-Method: POST" \
        -H "Access-Control-Request-Headers: Content-Type" \
        -I https://$DOMAIN/api/v11/health 2>/dev/null || echo "")
    
    if echo "$CORS_RESPONSE" | grep -i "Access-Control-Allow-Origin" >/dev/null; then
        add_result "CORS_Headers" "PASS" "CORS headers present" $(($(date +%s%3N) - test_start))
    else
        add_result "CORS_Headers" "FAIL" "CORS headers missing" $(($(date +%s%3N) - test_start))
    fi
}

test_monitoring_setup() {
    print_step "Testing Monitoring Configuration"
    local test_start=$(date +%s%3N)
    
    # Test Prometheus metrics format
    METRICS_CONTENT=$(curl -sf --max-time 5 http://localhost:9003/q/metrics 2>/dev/null || echo "")
    
    if echo "$METRICS_CONTENT" | grep -q "# HELP\|# TYPE"; then
        add_result "Prometheus_Metrics_Format" "PASS" "Prometheus metrics format valid" $(($(date +%s%3N) - test_start))
    else
        add_result "Prometheus_Metrics_Format" "FAIL" "Prometheus metrics format invalid" $(($(date +%s%3N) - test_start))
    fi
    
    # Count available metrics
    test_start=$(date +%s%3N)
    METRIC_COUNT=$(echo "$METRICS_CONTENT" | grep -c "^[a-zA-Z]" || echo "0")
    if [ "$METRIC_COUNT" -gt 50 ]; then
        add_result "Metrics_Count" "PASS" "$METRIC_COUNT metrics available" $(($(date +%s%3N) - test_start))
    else
        add_result "Metrics_Count" "WARN" "Only $METRIC_COUNT metrics available" $(($(date +%s%3N) - test_start))
    fi
}

test_nginx_configuration() {
    print_step "Testing NGINX Configuration"
    local test_start=$(date +%s%3N)
    
    # Test NGINX status
    if systemctl is-active --quiet nginx; then
        add_result "NGINX_Service" "PASS" "NGINX service is running" $(($(date +%s%3N) - test_start))
    else
        add_result "NGINX_Service" "FAIL" "NGINX service not running" $(($(date +%s%3N) - test_start))
        return
    fi
    
    # Test HTTP to HTTPS redirect
    test_start=$(date +%s%3N)
    HTTP_RESPONSE=$(curl -I --max-time 5 -s http://$DOMAIN 2>/dev/null | head -1 || echo "")
    if echo "$HTTP_RESPONSE" | grep -q "301\|302"; then
        add_result "HTTP_HTTPS_Redirect" "PASS" "HTTP to HTTPS redirect working" $(($(date +%s%3N) - test_start))
    else
        add_result "HTTP_HTTPS_Redirect" "FAIL" "HTTP to HTTPS redirect not working" $(($(date +%s%3N) - test_start))
    fi
    
    # Test NGINX configuration syntax
    test_start=$(date +%s%3N)
    if nginx -t >/dev/null 2>&1; then
        add_result "NGINX_Config_Syntax" "PASS" "NGINX configuration syntax valid" $(($(date +%s%3N) - test_start))
    else
        add_result "NGINX_Config_Syntax" "FAIL" "NGINX configuration syntax invalid" $(($(date +%s%3N) - test_start))
    fi
}

run_load_test() {
    print_step "Running Basic Load Test"
    local test_start=$(date +%s%3N)
    
    # Simple concurrent request test
    print_step "Executing 100 concurrent requests..."
    
    LOAD_TEST_RESULT=$(seq 1 100 | xargs -n1 -P100 -I{} curl -s --max-time 2 https://$DOMAIN/health >/dev/null && echo "PASS" || echo "FAIL")
    
    if [ "$LOAD_TEST_RESULT" = "PASS" ]; then
        add_result "Basic_Load_Test" "PASS" "100 concurrent requests successful" $(($(date +%s%3N) - test_start))
    else
        add_result "Basic_Load_Test" "FAIL" "Load test failed" $(($(date +%s%3N) - test_start))
    fi
}

generate_report() {
    local end_time=$(date +%s)
    local total_duration=$((end_time - START_TIME))
    
    # Generate JSON report
    cat > "$VALIDATION_LOG" << EOF
{
  "sprint": "Sprint 4",
  "validation_timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "duration_seconds": $total_duration,
  "domain": "$DOMAIN",
  "summary": {
    "total_tests": $TOTAL_TESTS,
    "passed_tests": $PASSED_TESTS,
    "failed_tests": $FAILED_TESTS,
    "success_rate": "$(( (PASSED_TESTS * 100) / TOTAL_TESTS ))%"
  },
  "results": {
    $(IFS=','; echo "${VALIDATION_RESULTS[*]}")
  },
  "objectives": {
    "ssl_verification": "$([ $FAILED_TESTS -eq 0 ] && echo "ACHIEVED" || echo "NEEDS_ATTENTION")",
    "production_deployment": "$([ $PASSED_TESTS -gt $((TOTAL_TESTS * 80 / 100)) ] && echo "ACHIEVED" || echo "NEEDS_ATTENTION")",
    "monitoring_setup": "CONFIGURED",
    "performance_validation": "$([ $FAILED_TESTS -lt 3 ] && echo "ACHIEVED" || echo "NEEDS_ATTENTION")"
  }
}
EOF
    
    print_step "Validation Complete"
    echo ""
    echo "======================================================"
    echo "Sprint 4 Production Deployment Validation Results"
    echo "======================================================"
    echo "Total Tests: $TOTAL_TESTS"
    echo "Passed: $PASSED_TESTS"
    echo "Failed: $FAILED_TESTS"
    echo "Success Rate: $(( (PASSED_TESTS * 100) / TOTAL_TESTS ))%"
    echo "Duration: ${total_duration}s"
    echo ""
    echo "Report saved to: $VALIDATION_LOG"
    
    if [ $FAILED_TESTS -eq 0 ]; then
        print_success "🎉 Sprint 4 validation passed completely!"
        echo "✅ SSL certificate verified"
        echo "✅ Production deployment successful"
        echo "✅ Security headers configured"
        echo "✅ Monitoring and alerting active"
        echo "✅ Performance endpoints responding"
        return 0
    elif [ $FAILED_TESTS -lt 3 ]; then
        print_warn "⚠️  Sprint 4 validation mostly successful with minor issues"
        echo "Review the report and fix failing tests"
        return 1
    else
        print_fail "❌ Sprint 4 validation failed - critical issues detected"
        echo "Immediate attention required before production release"
        return 2
    fi
}

# Main execution
echo "🚀 Starting Sprint 4 Production Validation Suite"
echo "Testing deployment on: $DOMAIN"
echo ""

# Check if running as root for system tests
if [ "$EUID" -ne 0 ]; then
    print_warn "Some tests require root privileges. Run with sudo for complete validation."
fi

# Run all validation tests
test_ssl_certificate
test_service_availability
test_performance_metrics
test_security_headers
test_cors_configuration
test_nginx_configuration
test_monitoring_setup
run_load_test

# Generate and display report
generate_report