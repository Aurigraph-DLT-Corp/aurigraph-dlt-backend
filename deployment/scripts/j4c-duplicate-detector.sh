#!/bin/bash

################################################################################
# J4C Agent - Duplicate & Blocker Detection System
# Purpose: Detect duplicate endpoints, containers, files, and blockers
# Status: Active in J4C Agent Framework
# Version: 1.0.0
################################################################################

set -e

# Color codes for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Report file
REPORT_FILE="DUPLICATE-DETECTION-REPORT.md"
BLOCKERS_FILE="BUILD-BLOCKERS.md"

# Counters
CRITICAL_COUNT=0
WARNING_COUNT=0

# Initialize report
init_report() {
    cat > "$REPORT_FILE" << 'EOF'
# Duplicate & Blocker Detection Report

**Date**: $(date '+%Y-%m-%d %H:%M:%S')
**Severity**: Will be updated with findings
**Status**: Scanning...

---

## Critical Issues (Block Build)

## Warning Issues (May cause runtime problems)

## Action Items

EOF
}

# REST Endpoints Scanning
scan_endpoints() {
    echo -e "${BLUE}🔍 Scanning REST endpoints...${NC}"

    local duplicates=$(grep -r "@Path(" src/main/java --include="*.java" 2>/dev/null | \
        awk -F: '{print $NF}' | \
        sort | uniq -d)

    if [ -n "$duplicates" ]; then
        echo -e "${RED}⚠️  DUPLICATE ENDPOINTS DETECTED${NC}"
        echo "$duplicates" | while read path; do
            local files=$(grep -r "$path" src/main/java --include="*.java" 2>/dev/null | cut -d: -f1 | sort -u)
            echo -e "${YELLOW}  Path: $path${NC}"
            echo "  Files:"
            echo "$files" | sed 's/^/    • /'
            echo "  Severity: CRITICAL - Will cause build failure"
            echo ""
            ((CRITICAL_COUNT++))
        done
        return 1
    else
        echo -e "${GREEN}✅ No duplicate endpoints found${NC}"
        return 0
    fi
}

# Docker Container Scanning
scan_containers() {
    echo -e "${BLUE}🔍 Scanning Docker containers...${NC}"

    if [ ! -f "docker-compose.yml" ]; then
        echo -e "${YELLOW}⚠️  docker-compose.yml not found${NC}"
        return 0
    fi

    # Check for duplicate service names
    local services=$(grep "^[a-z].*:" docker-compose.yml | grep -v "services:" | sed 's/:.*//' | sort)
    local dup_services=$(echo "$services" | sort | uniq -d)

    if [ -n "$dup_services" ]; then
        echo -e "${RED}⚠️  DUPLICATE CONTAINERS DETECTED${NC}"
        echo "$dup_services" | while read svc; do
            echo -e "${YELLOW}  Service: $svc${NC}"
            echo "  Severity: CRITICAL - Container won't start"
            ((CRITICAL_COUNT++))
        done
        return 1
    else
        echo -e "${GREEN}✅ No duplicate containers found${NC}"
    fi

    # Check for duplicate ports
    local ports=$(grep -E "^\s*-\s*\"?[0-9]+:" docker-compose.yml | \
        grep -oE "[0-9]+:" | sed 's/:$//' | sort)
    local dup_ports=$(echo "$ports" | sort | uniq -d)

    if [ -n "$dup_ports" ]; then
        echo -e "${RED}⚠️  DUPLICATE PORT BINDINGS DETECTED${NC}"
        echo "$dup_ports" | while read port; do
            echo -e "${YELLOW}  Port: $port${NC}"
            echo "  Severity: CRITICAL - Port conflict"
            ((CRITICAL_COUNT++))
        done
        return 1
    else
        echo -e "${GREEN}✅ No port conflicts found${NC}"
    fi
}

# File Declaration Scanning
scan_files() {
    echo -e "${BLUE}🔍 Scanning file declarations...${NC}"

    local java_files=$(find src -name "*.java" -type f 2>/dev/null | \
        xargs basename -a | \
        sort | uniq -d)

    if [ -n "$java_files" ]; then
        echo -e "${RED}⚠️  DUPLICATE FILE DECLARATIONS DETECTED${NC}"
        echo "$java_files" | while read file; do
            local locations=$(find src -name "$file" -type f)
            echo -e "${YELLOW}  File: $file${NC}"
            echo "  Locations:"
            echo "$locations" | sed 's/^/    • /'
            echo "  Severity: WARNING - May cause compilation issues"
            echo ""
            ((WARNING_COUNT++))
        done
        return 0 # Return 0 for warnings (not critical)
    else
        echo -e "${GREEN}✅ No duplicate files found${NC}"
        return 0
    fi
}

# Circular Dependency Scanning
scan_dependencies() {
    echo -e "${BLUE}🔍 Scanning for circular dependencies...${NC}"

    # Simple check: look for mutual @Inject patterns
    local mutual=$(grep -r "@Inject" src/main/java --include="*.java" -A2 2>/dev/null | \
        grep -E "private|public" | \
        awk '{print $NF}' | \
        sort | uniq -c | awk '$1 > 1 {print $2}')

    if [ -n "$mutual" ]; then
        echo -e "${YELLOW}⚠️  POTENTIAL CIRCULAR DEPENDENCIES DETECTED${NC}"
        echo "$mutual" | while read dep; do
            echo -e "  Dependency: $dep"
            echo "  Severity: WARNING - Monitor in integration testing"
            ((WARNING_COUNT++))
        done
        echo ""
        return 0
    else
        echo -e "${GREEN}✅ No obvious circular dependencies found${NC}"
        return 0
    fi
}

# Port Conflict Scanning
scan_ports() {
    echo -e "${BLUE}🔍 Scanning port assignments...${NC}"

    # Scan application.properties
    local ports=$(grep -E "quarkus\.(http|grpc)\..*port=" src/main/resources/application.properties 2>/dev/null | \
        grep -oE "[0-9]+$" | sort -u)

    echo "Found ports in application.properties: $ports"

    if echo "$ports" | grep -q "9003"; then
        echo -e "${GREEN}✅ Port 9003 assigned to Quarkus HTTP${NC}"
    fi

    if echo "$ports" | grep -q "9004"; then
        echo -e "${GREEN}✅ Port 9004 assigned to gRPC${NC}"
    fi

    return 0
}

# Full Scan
run_full_scan() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  J4C Agent - Duplicate & Blocker Detection${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    # Run scans
    ENDPOINTS_RESULT=$(scan_endpoints 2>&1 || echo "FAILED")
    CONTAINERS_RESULT=$(scan_containers 2>&1 || echo "FAILED")
    FILES_RESULT=$(scan_files 2>&1 || echo "FAILED")
    DEPS_RESULT=$(scan_dependencies 2>&1 || echo "FAILED")
    PORTS_RESULT=$(scan_ports 2>&1 || echo "FAILED")

    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  Summary${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    if [ $CRITICAL_COUNT -gt 0 ]; then
        echo -e "${RED}❌ Critical Issues: $CRITICAL_COUNT${NC}"
        echo "   These issues MUST be fixed before building"
    else
        echo -e "${GREEN}✅ No critical issues${NC}"
    fi

    if [ $WARNING_COUNT -gt 0 ]; then
        echo -e "${YELLOW}⚠️  Warnings: $WARNING_COUNT${NC}"
        echo "   These may cause runtime issues"
    else
        echo -e "${GREEN}✅ No warnings${NC}"
    fi

    # Generate report
    generate_report

    echo ""
    echo -e "${BLUE}📄 Full report saved to: $REPORT_FILE${NC}"
    echo ""

    # Exit with appropriate code
    if [ $CRITICAL_COUNT -gt 0 ]; then
        return 1
    else
        return 0
    fi
}

# Generate Markdown Report
generate_report() {
    cat > "$REPORT_FILE" << EOF
# Duplicate & Blocker Detection Report

**Date**: $(date '+%Y-%m-%d %H:%M:%S')
**Total Critical Issues**: $CRITICAL_COUNT
**Total Warnings**: $WARNING_COUNT

---

## Scan Results

### REST Endpoints
$ENDPOINTS_RESULT

### Docker Containers
$CONTAINERS_RESULT

### File Declarations
$FILES_RESULT

### Dependencies
$DEPS_RESULT

### Port Assignments
$PORTS_RESULT

---

## Action Items

- [ ] Review all critical issues above
- [ ] Consolidate duplicate endpoints if found
- [ ] Fix container port conflicts if found
- [ ] Remove duplicate file declarations if found
- [ ] Review circular dependencies if found

---

**Report Generated**: $(date)
**Severity Level**: $([ $CRITICAL_COUNT -gt 0 ] && echo "CRITICAL" || echo "OK")

EOF
}

# Main
main() {
    case "${1:-all}" in
        endpoints)
            scan_endpoints
            ;;
        containers)
            scan_containers
            ;;
        files)
            scan_files
            ;;
        dependencies)
            scan_dependencies
            ;;
        ports)
            scan_ports
            ;;
        all)
            run_full_scan
            ;;
        *)
            echo "Usage: $0 {endpoints|containers|files|dependencies|ports|all}"
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
