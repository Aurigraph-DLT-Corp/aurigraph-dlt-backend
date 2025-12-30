#!/bin/bash

################################################################################
# Sprint 15 Phase 1: JVM Optimization Validation Script
# Validates that all JVM optimizations are correctly applied
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}Sprint 15 Phase 1 Validation${NC}"
echo -e "${BLUE}JVM Optimization Deployment Check${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

VALIDATION_PASSED=true

# Function to check result
check_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
        VALIDATION_PASSED=false
    fi
}

# 1. Check if application is running
echo -e "${YELLOW}[1/8] Checking if Aurigraph V11 is running...${NC}"
JVM_PID=$(jps | grep quarkus-run | awk '{print $1}')
if [ -z "$JVM_PID" ]; then
    echo -e "${RED}✗ Application not running. Start with ./start-optimized-jvm.sh${NC}"
    exit 1
else
    check_result 0 "Application running (PID: $JVM_PID)"
fi
echo ""

# 2. Check Java version
echo -e "${YELLOW}[2/8] Validating Java version...${NC}"
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1}')
if [ "$JAVA_VERSION" -ge 21 ]; then
    check_result 0 "Java version: $JAVA_VERSION (>= 21 required)"
else
    check_result 1 "Java version: $JAVA_VERSION (upgrade to 21+ required)"
fi
echo ""

# 3. Check G1GC is enabled
echo -e "${YELLOW}[3/8] Validating G1GC configuration...${NC}"
JVM_FLAGS=$(jps -v | grep quarkus-run)
if echo "$JVM_FLAGS" | grep -q "UseG1GC"; then
    check_result 0 "G1GC enabled"
else
    check_result 1 "G1GC NOT enabled (expected -XX:+UseG1GC)"
fi

if echo "$JVM_FLAGS" | grep -q "MaxGCPauseMillis=100"; then
    check_result 0 "MaxGCPauseMillis=100"
else
    check_result 1 "MaxGCPauseMillis NOT set (expected 100ms)"
fi
echo ""

# 4. Check heap memory settings
echo -e "${YELLOW}[4/8] Validating heap memory configuration...${NC}"
if echo "$JVM_FLAGS" | grep -q "Xms2G"; then
    check_result 0 "Initial heap: 2GB (-Xms2G)"
else
    check_result 1 "Initial heap NOT set to 2GB"
fi

if echo "$JVM_FLAGS" | grep -q "Xmx2G"; then
    check_result 0 "Maximum heap: 2GB (-Xmx2G)"
else
    check_result 1 "Maximum heap NOT set to 2GB"
fi
echo ""

# 5. Check virtual thread configuration
echo -e "${YELLOW}[5/8] Validating virtual thread configuration...${NC}"
if echo "$JVM_FLAGS" | grep -q "virtualThreadScheduler.parallelism=32"; then
    check_result 0 "Virtual thread parallelism: 32"
else
    check_result 1 "Virtual thread parallelism NOT set to 32"
fi

if echo "$JVM_FLAGS" | grep -q "virtualThreadScheduler.maxPoolSize=32"; then
    check_result 0 "Virtual thread max pool size: 32"
else
    check_result 1 "Virtual thread max pool size NOT set to 32"
fi
echo ""

# 6. Check JIT compilation settings
echo -e "${YELLOW}[6/8] Validating JIT compiler configuration...${NC}"
if echo "$JVM_FLAGS" | grep -q "TieredCompilation"; then
    check_result 0 "Tiered compilation enabled"
else
    check_result 1 "Tiered compilation NOT enabled"
fi

if echo "$JVM_FLAGS" | grep -q "TieredStopAtLevel=4"; then
    check_result 0 "Tiered compilation level: 4 (C2 compiler)"
else
    check_result 1 "Tiered compilation level NOT set to 4"
fi
echo ""

# 7. Check memory management settings
echo -e "${YELLOW}[7/8] Validating memory management configuration...${NC}"
if echo "$JVM_FLAGS" | grep -q "DisableExplicitGC"; then
    check_result 0 "Explicit GC disabled"
else
    check_result 1 "Explicit GC NOT disabled"
fi

if echo "$JVM_FLAGS" | grep -q "UseCompressedOops"; then
    check_result 0 "Compressed OOPs enabled"
else
    check_result 1 "Compressed OOPs NOT enabled"
fi
echo ""

# 8. Check monitoring configuration
echo -e "${YELLOW}[8/8] Validating monitoring configuration...${NC}"
if [ -f "logs/gc.log" ]; then
    check_result 0 "GC log file exists: logs/gc.log"
else
    check_result 1 "GC log file NOT found: logs/gc.log"
fi

# Check if JMX port is open
if nc -z localhost 9099 2>/dev/null; then
    check_result 0 "JMX monitoring port open: 9099"
else
    check_result 1 "JMX monitoring port NOT accessible: 9099"
fi
echo ""

# 9. Check application health
echo -e "${YELLOW}[BONUS] Checking application health endpoint...${NC}"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9003/q/health 2>/dev/null || echo "000")
if [ "$HTTP_STATUS" = "200" ]; then
    check_result 0 "Health endpoint responding: HTTP $HTTP_STATUS"
else
    check_result 1 "Health endpoint NOT responding: HTTP $HTTP_STATUS"
fi
echo ""

# Final result
echo -e "${BLUE}================================================${NC}"
if [ "$VALIDATION_PASSED" = true ]; then
    echo -e "${GREEN}✅ VALIDATION PASSED${NC}"
    echo -e "${GREEN}All Sprint 15 Phase 1 optimizations are correctly applied!${NC}"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo -e "1. Monitor TPS: curl http://localhost:9003/api/v11/performance"
    echo -e "2. Watch GC logs: tail -f logs/gc.log"
    echo -e "3. Connect JMX: jconsole localhost:9099"
    echo -e "4. Run load test: ./performance-benchmark.sh"
    echo ""
    echo -e "${BLUE}Expected Performance:${NC}"
    echo -e "  • TPS: 3.54M (+18% from 3.0M baseline)"
    echo -e "  • GC Pause: ~20ms average"
    echo -e "  • Memory: 1.6-1.8GB stable"
    echo ""
    exit 0
else
    echo -e "${RED}❌ VALIDATION FAILED${NC}"
    echo -e "${RED}Some optimizations are not correctly applied.${NC}"
    echo ""
    echo -e "${YELLOW}Troubleshooting:${NC}"
    echo -e "1. Stop current instance: kill $JVM_PID"
    echo -e "2. Rebuild: ./mvnw clean package"
    echo -e "3. Restart with optimizations: ./start-optimized-jvm.sh"
    echo ""
    exit 1
fi
echo -e "${BLUE}================================================${NC}"
