#!/bin/bash

################################################################################
# J4C Agent - Health Monitoring & Endpoint Response Checker
# Purpose: Monitor container/endpoint health and alert on failures
# Status: Active in J4C Agent Framework
# Version: 1.0.0
################################################################################

set -e

# Color codes
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# Report files
HEALTH_REPORT="HEALTH-MONITOR-REPORT.md"
ALERTS_FILE="HEALTH-ALERTS.txt"

# Counters
HEALTHY_COUNT=0
UNHEALTHY_COUNT=0
TIMEOUT_COUNT=0

# Configuration
TIMEOUT=5  # seconds
RETRY_COUNT=2
RETRY_DELAY=1

# Initialize health report
init_health_report() {
    cat > "$HEALTH_REPORT" << 'EOF'
# Health Monitor Report

**Date**: $(date '+%Y-%m-%d %H:%M:%S')
**Status**: Monitoring...

## Container Status

## Endpoint Status

## Response Time Analysis

## Alerts

EOF
}

# Check Docker Container Health
check_container_health() {
    echo -e "${BLUE}🔍 Checking Docker container health...${NC}"

    if ! command -v docker &> /dev/null; then
        echo -e "${YELLOW}⚠️  Docker not found, skipping container checks${NC}"
        return 0
    fi

    if ! docker ps &> /dev/null; then
        echo -e "${YELLOW}⚠️  Docker daemon not responding${NC}"
        return 0
    fi

    # Get all running containers
    local containers=$(docker ps --format "{{.Names}}")

    if [ -z "$containers" ]; then
        echo -e "${YELLOW}⚠️  No running containers found${NC}"
        return 0
    fi

    echo "Found containers:"
    echo "$containers" | while read container; do
        # Get container status
        local status=$(docker inspect "$container" --format='{{.State.Status}}')
        local health=$(docker inspect "$container" --format='{{.State.Health.Status}}' 2>/dev/null || echo "none")

        if [ "$status" = "running" ]; then
            if [ "$health" = "healthy" ] || [ "$health" = "none" ]; then
                echo -e "  ${GREEN}✅${NC} $container (status: $status, health: $health)"
                ((HEALTHY_COUNT++))
            else
                echo -e "  ${RED}❌${NC} $container (status: $status, health: $health)"
                ((UNHEALTHY_COUNT++))
            fi
        else
            echo -e "  ${RED}❌${NC} $container (status: $status - NOT RUNNING)"
            ((UNHEALTHY_COUNT++))
        fi
    done

    return 0
}

# Check Endpoint Responsiveness
check_endpoint_health() {
    echo -e "${BLUE}🔍 Checking endpoint responsiveness...${NC}"

    local endpoints=(
        "http://localhost:9003/q/health"              # Quarkus health
        "http://localhost:9003/api/v11/info"          # Info endpoint
        "http://localhost:9000"                        # NGINX default
        "http://localhost:3000"                        # Grafana
        "http://localhost:9090"                        # Prometheus
    )

    for endpoint in "${endpoints[@]}"; do
        local host=$(echo "$endpoint" | cut -d: -f2 | sed 's/\/\///g')
        local port=$(echo "$endpoint" | cut -d: -f3 | cut -d/ -f1)

        # Skip if port is empty
        if [ -z "$port" ]; then
            port=80
        fi

        echo -n "  Checking $endpoint ... "

        # Try to connect with timeout
        if timeout "$TIMEOUT" bash -c "echo > /dev/tcp/$host/$port" 2>/dev/null; then
            # Try HTTP GET if TCP connected
            if command -v curl &> /dev/null; then
                local response=$(curl -s -o /dev/null -w "%{http_code}" \
                    --max-time "$TIMEOUT" "$endpoint" 2>/dev/null || echo "000")

                if [ "$response" = "200" ] || [ "$response" = "403" ] || [ "$response" = "401" ]; then
                    echo -e "${GREEN}✅ OK${NC} (HTTP $response)"
                    ((HEALTHY_COUNT++))
                else
                    echo -e "${RED}❌ FAILED${NC} (HTTP $response)"
                    ((UNHEALTHY_COUNT++))
                fi
            else
                echo -e "${GREEN}✅ OK${NC} (port reachable)"
                ((HEALTHY_COUNT++))
            fi
        else
            echo -e "${RED}❌ TIMEOUT${NC} (no response)"
            ((UNHEALTHY_COUNT++))
            ((TIMEOUT_COUNT++))
        fi
    done
}

# Check Port Bindings
check_port_status() {
    echo -e "${BLUE}🔍 Checking port bindings...${NC}"

    local ports=(
        "9003"   # Quarkus HTTP
        "9004"   # gRPC
        "5432"   # PostgreSQL
        "3000"   # Grafana
        "9090"   # Prometheus
        "80"     # HTTP
        "443"    # HTTPS
    )

    for port in "${ports[@]}"; do
        echo -n "  Port $port: "

        if command -v netstat &> /dev/null; then
            if netstat -tuln 2>/dev/null | grep -q ":$port "; then
                echo -e "${GREEN}✅ LISTENING${NC}"
                ((HEALTHY_COUNT++))
            else
                echo -e "${YELLOW}⚠️  Not listening${NC}"
            fi
        elif command -v lsof &> /dev/null; then
            if lsof -i ":$port" &> /dev/null; then
                echo -e "${GREEN}✅ LISTENING${NC}"
                ((HEALTHY_COUNT++))
            else
                echo -e "${YELLOW}⚠️  Not listening${NC}"
            fi
        else
            echo -e "${YELLOW}⚠️  Cannot check (netstat/lsof not available)${NC}"
        fi
    done
}

# Response Time Analysis
analyze_response_times() {
    echo -e "${BLUE}🔍 Analyzing response times...${NC}"

    if ! command -v curl &> /dev/null; then
        echo -e "${YELLOW}⚠️  curl not found, skipping response time analysis${NC}"
        return 0
    fi

    local endpoints=(
        "http://localhost:9003/q/health"
        "http://localhost:3000"
    )

    for endpoint in "${endpoints[@]}"; do
        echo -n "  $endpoint: "

        local total_time=0
        local success_count=0

        for i in $(seq 1 3); do
            local time=$(curl -s -o /dev/null -w "%{time_total}" \
                --max-time "$TIMEOUT" "$endpoint" 2>/dev/null || echo "timeout")

            if [ "$time" != "timeout" ]; then
                total_time=$(echo "$total_time + $time" | bc -l)
                ((success_count++))
            fi
        done

        if [ $success_count -gt 0 ]; then
            local avg_time=$(echo "scale=3; $total_time / $success_count" | bc -l)
            echo -e "${GREEN}Avg: ${avg_time}s${NC} ($success_count/3 requests)"
        else
            echo -e "${RED}All requests timed out${NC}"
        fi
    done
}

# Generate Alerts
generate_alerts() {
    local alert_count=0

    > "$ALERTS_FILE"

    if [ $UNHEALTHY_COUNT -gt 0 ]; then
        echo -e "${RED}⚠️  ALERT: $UNHEALTHY_COUNT unhealthy services detected${NC}"
        echo "⚠️  ALERT: $UNHEALTHY_COUNT unhealthy services detected" >> "$ALERTS_FILE"
        ((alert_count++))
    fi

    if [ $TIMEOUT_COUNT -gt 0 ]; then
        echo -e "${RED}⚠️  ALERT: $TIMEOUT_COUNT endpoints timing out${NC}"
        echo "⚠️  ALERT: $TIMEOUT_COUNT endpoints timing out" >> "$ALERTS_FILE"
        ((alert_count++))
    fi

    if [ $alert_count -eq 0 ]; then
        echo -e "${GREEN}✅ No critical alerts${NC}"
        echo "✅ No critical alerts" > "$ALERTS_FILE"
    fi
}

# Generate Report
generate_health_report() {
    cat > "$HEALTH_REPORT" << EOF
# Health Monitor Report

**Date**: $(date '+%Y-%m-%d %H:%M:%S')
**Status**: Monitoring Complete

---

## Summary

- Healthy Services: $HEALTHY_COUNT ✅
- Unhealthy Services: $UNHEALTHY_COUNT ❌
- Timeouts: $TIMEOUT_COUNT ⏱️

---

## Findings

### Container Status
All running containers checked for health status.

### Endpoint Status
Endpoints checked on key ports: 9003, 9000, 3000, 9090, 5432

### Port Bindings
Verified port bindings for expected services.

### Response Time Analysis
Analyzed response times for critical endpoints.

---

## Action Items

$(if [ $UNHEALTHY_COUNT -gt 0 ]; then echo "- [ ] Investigate unhealthy services"; fi)
$(if [ $TIMEOUT_COUNT -gt 0 ]; then echo "- [ ] Check network connectivity for timing out endpoints"; fi)
- [ ] Review detailed logs for failed services
- [ ] Restart any failed containers if needed

---

**Generated**: $(date)
**Next Check**: Recommend running this monitor every 5 minutes in production

EOF
}

# Full Monitoring
run_full_monitoring() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  J4C Agent - Health & Endpoint Monitor${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    # Run checks
    check_container_health
    echo ""
    check_endpoint_health
    echo ""
    check_port_status
    echo ""
    analyze_response_times
    echo ""

    # Generate alerts and reports
    generate_alerts
    generate_health_report

    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  Summary${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    echo "Healthy Services: $HEALTHY_COUNT ✅"

    if [ $UNHEALTHY_COUNT -gt 0 ]; then
        echo -e "${RED}Unhealthy Services: $UNHEALTHY_COUNT ❌${NC}"
    else
        echo "Unhealthy Services: 0 ✅"
    fi

    if [ $TIMEOUT_COUNT -gt 0 ]; then
        echo -e "${RED}Timeouts: $TIMEOUT_COUNT ⏱️${NC}"
    else
        echo "Timeouts: 0 ✅"
    fi

    echo ""
    echo -e "${BLUE}📄 Reports saved:${NC}"
    echo "   • $HEALTH_REPORT (detailed health report)"
    echo "   • $ALERTS_FILE (alert summary)"

    if [ $UNHEALTHY_COUNT -gt 0 ] || [ $TIMEOUT_COUNT -gt 0 ]; then
        return 1
    else
        return 0
    fi
}

# Main
main() {
    case "${1:-all}" in
        containers)
            check_container_health
            ;;
        endpoints)
            check_endpoint_health
            ;;
        ports)
            check_port_status
            ;;
        response-time)
            analyze_response_times
            ;;
        all)
            run_full_monitoring
            ;;
        *)
            echo "Usage: $0 {containers|endpoints|ports|response-time|all}"
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
