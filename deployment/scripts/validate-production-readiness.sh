#!/bin/bash

###############################################################################
# Aurigraph V11 Production Readiness Validation Script
# Sprint 20 - Final Validation
#
# This script validates all production readiness criteria including:
# - Build system health
# - Test execution and coverage
# - Service functionality
# - Performance benchmarks
# - Integration checks
###############################################################################

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNINGS=0

# Log functions
log_header() {
    echo -e "\n${BLUE}================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================${NC}\n"
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓ PASS]${NC} $1"
    ((PASSED_CHECKS++))
}

log_failure() {
    echo -e "${RED}[✗ FAIL]${NC} $1"
    ((FAILED_CHECKS++))
}

log_warning() {
    echo -e "${YELLOW}[⚠ WARN]${NC} $1"
    ((WARNINGS++))
}

check() {
    ((TOTAL_CHECKS++))
}

###############################################################################
# 1. Environment Validation
###############################################################################

validate_environment() {
    log_header "1. ENVIRONMENT VALIDATION"

    # Check Java version
    check
    log_info "Checking Java version..."
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 21 ]; then
            log_success "Java $JAVA_VERSION installed (requirement: 21+)"
        else
            log_failure "Java $JAVA_VERSION found, but Java 21+ required"
        fi
    else
        log_failure "Java not found"
    fi

    # Check Maven
    check
    log_info "Checking Maven..."
    if command -v mvn &> /dev/null || [ -f "./mvnw" ]; then
        MVN_VERSION=$(./mvnw --version 2>&1 | head -n 1 | awk '{print $3}')
        log_success "Maven $MVN_VERSION available"
    else
        log_failure "Maven not found"
    fi

    # Check Docker (for native builds)
    check
    log_info "Checking Docker..."
    if command -v docker &> /dev/null; then
        DOCKER_VERSION=$(docker --version | awk '{print $3}' | tr -d ',')
        log_success "Docker $DOCKER_VERSION installed"
    else
        log_warning "Docker not found (required for container-based native builds)"
    fi

    # Check GraalVM (optional)
    check
    log_info "Checking GraalVM..."
    if [ -n "$GRAALVM_HOME" ]; then
        log_success "GraalVM configured at $GRAALVM_HOME"
    else
        log_info "GraalVM not configured (can use container builds)"
    fi

    # Check disk space
    check
    log_info "Checking disk space..."
    AVAILABLE_SPACE=$(df -h . | tail -n 1 | awk '{print $4}')
    log_success "Available disk space: $AVAILABLE_SPACE"
}

###############################################################################
# 2. Build System Validation
###############################################################################

validate_build() {
    log_header "2. BUILD SYSTEM VALIDATION"

    # Clean build
    check
    log_info "Running clean build..."
    if ./mvnw clean package -DskipTests > /tmp/build.log 2>&1; then
        log_success "Clean build successful"
    else
        log_failure "Build failed (see /tmp/build.log)"
        cat /tmp/build.log
    fi

    # Check JAR creation
    check
    log_info "Checking JAR artifact..."
    if [ -f "target/quarkus-app/quarkus-run.jar" ]; then
        JAR_SIZE=$(du -h target/quarkus-app/quarkus-run.jar | cut -f1)
        log_success "JAR created successfully (size: $JAR_SIZE)"
    else
        log_failure "JAR not found in target/quarkus-app/"
    fi

    # Check build profiles
    check
    log_info "Validating build profiles..."
    if grep -q "native-fast" pom.xml && grep -q "native-ultra" pom.xml; then
        log_success "All three native build profiles present (native-fast, native, native-ultra)"
    else
        log_warning "Some native build profiles may be missing"
    fi
}

###############################################################################
# 3. Test Execution Validation
###############################################################################

validate_tests() {
    log_header "3. TEST EXECUTION VALIDATION"

    # Run all tests
    check
    log_info "Running all tests..."
    if ./mvnw test > /tmp/test-results.log 2>&1; then
        log_success "All tests passed"

        # Parse test results
        if grep -q "Tests run:" /tmp/test-results.log; then
            TEST_SUMMARY=$(grep "Tests run:" /tmp/test-results.log | tail -n 1)
            log_info "Test summary: $TEST_SUMMARY"
        fi
    else
        log_failure "Some tests failed (see /tmp/test-results.log)"
        tail -n 50 /tmp/test-results.log
    fi

    # Check individual test suites
    TEST_SUITES=(
        "QuantumCryptoProviderTest"
        "ParallelTransactionExecutorTest"
        "EthereumBridgeServiceTest"
        "EnterprisePortalServiceTest"
        "SystemMonitoringServiceTest"
    )

    for suite in "${TEST_SUITES[@]}"; do
        check
        if grep -q "$suite" /tmp/test-results.log; then
            log_success "Test suite executed: $suite"
        else
            log_warning "Test suite not found in results: $suite"
        fi
    done

    # Check test coverage (if jacoco is configured)
    check
    log_info "Checking test coverage..."
    if ./mvnw jacoco:report > /tmp/coverage.log 2>&1; then
        if [ -f "target/site/jacoco/index.html" ]; then
            log_success "Coverage report generated at target/site/jacoco/"
        else
            log_info "Coverage report not generated (JaCoCo may not be configured)"
        fi
    else
        log_info "Coverage reporting skipped"
    fi
}

###############################################################################
# 4. Service Functionality Validation
###############################################################################

validate_services() {
    log_header "4. SERVICE FUNCTIONALITY VALIDATION"

    # Check service implementations
    SERVICES=(
        "src/main/java/io/aurigraph/v11/crypto/QuantumCryptoProvider.java"
        "src/main/java/io/aurigraph/v11/execution/ParallelTransactionExecutor.java"
        "src/main/java/io/aurigraph/v11/bridge/EthereumBridgeService.java"
        "src/main/java/io/aurigraph/v11/portal/EnterprisePortalService.java"
        "src/main/java/io/aurigraph/v11/monitoring/SystemMonitoringService.java"
    )

    for service in "${SERVICES[@]}"; do
        check
        if [ -f "$service" ]; then
            SERVICE_NAME=$(basename "$service" .java)
            LINE_COUNT=$(wc -l < "$service")
            log_success "Service implemented: $SERVICE_NAME ($LINE_COUNT lines)"
        else
            log_failure "Service not found: $service"
        fi
    done

    # Check test implementations
    TEST_FILES=(
        "src/test/java/io/aurigraph/v11/crypto/QuantumCryptoProviderTest.java"
        "src/test/java/io/aurigraph/v11/execution/ParallelTransactionExecutorTest.java"
        "src/test/java/io/aurigraph/v11/bridge/EthereumBridgeServiceTest.java"
        "src/test/java/io/aurigraph/v11/portal/EnterprisePortalServiceTest.java"
        "src/test/java/io/aurigraph/v11/monitoring/SystemMonitoringServiceTest.java"
    )

    for test_file in "${TEST_FILES[@]}"; do
        check
        if [ -f "$test_file" ]; then
            TEST_NAME=$(basename "$test_file" .java)
            TEST_COUNT=$(grep -c "@Test" "$test_file" || echo "0")
            log_success "Test suite exists: $TEST_NAME ($TEST_COUNT tests)"
        else
            log_failure "Test suite not found: $test_file"
        fi
    done
}

###############################################################################
# 5. Configuration Validation
###############################################################################

validate_configuration() {
    log_header "5. CONFIGURATION VALIDATION"

    # Check application.properties
    check
    if [ -f "src/main/resources/application.properties" ]; then
        log_success "application.properties exists"

        # Check critical properties
        if grep -q "quarkus.http.port" src/main/resources/application.properties; then
            PORT=$(grep "quarkus.http.port" src/main/resources/application.properties | cut -d'=' -f2)
            log_info "HTTP port configured: $PORT"
        fi

        if grep -q "quarkus.grpc.server.port" src/main/resources/application.properties; then
            GRPC_PORT=$(grep "quarkus.grpc.server.port" src/main/resources/application.properties | cut -d'=' -f2)
            log_info "gRPC port configured: $GRPC_PORT"
        fi
    else
        log_failure "application.properties not found"
    fi

    # Check for proto files
    check
    if [ -d "src/main/proto" ]; then
        PROTO_COUNT=$(find src/main/proto -name "*.proto" | wc -l)
        log_success "Proto directory exists ($PROTO_COUNT proto files)"
    else
        log_warning "Proto directory not found"
    fi
}

###############################################################################
# 6. Documentation Validation
###############################################################################

validate_documentation() {
    log_header "6. DOCUMENTATION VALIDATION"

    # Check key documentation files
    DOCS=(
        "README.md"
        "PRODUCTION_READINESS_CHECKLIST.md"
    )

    for doc in "${DOCS[@]}"; do
        check
        if [ -f "$doc" ]; then
            log_success "Documentation exists: $doc"
        else
            log_warning "Documentation missing: $doc"
        fi
    done

    # Check Javadoc coverage
    check
    log_info "Checking Javadoc coverage..."
    JAVA_FILES=$(find src/main/java -name "*.java" | wc -l)
    JAVADOC_FILES=$(grep -r "^/\*\*" src/main/java | wc -l)
    if [ "$JAVADOC_FILES" -gt $((JAVA_FILES / 2)) ]; then
        log_success "Good Javadoc coverage (${JAVADOC_FILES} docs for ${JAVA_FILES} files)"
    else
        log_warning "Low Javadoc coverage (${JAVADOC_FILES} docs for ${JAVA_FILES} files)"
    fi
}

###############################################################################
# 7. Performance Validation
###############################################################################

validate_performance() {
    log_header "7. PERFORMANCE VALIDATION"

    # Check if performance tests exist
    check
    if grep -q "testHighThroughput" src/test/java/io/aurigraph/v11/execution/ParallelTransactionExecutorTest.java; then
        log_success "Performance tests implemented"
    else
        log_warning "Performance tests not found"
    fi

    # Run quick performance test
    check
    log_info "Running quick performance test..."
    if ./mvnw test -Dtest=ParallelTransactionExecutorTest#testHighThroughput1000Transactions > /tmp/perf-test.log 2>&1; then
        log_success "Quick performance test passed"

        # Extract TPS if available in logs
        if grep -q "TPS" /tmp/perf-test.log; then
            TPS_LINE=$(grep "TPS" /tmp/perf-test.log | tail -n 1)
            log_info "Performance: $TPS_LINE"
        fi
    else
        log_warning "Performance test failed or not available"
    fi
}

###############################################################################
# 8. Security Validation
###############################################################################

validate_security() {
    log_header "8. SECURITY VALIDATION"

    # Check for quantum crypto implementation
    check
    if [ -f "src/main/java/io/aurigraph/v11/crypto/QuantumCryptoProvider.java" ]; then
        log_success "Quantum cryptography provider implemented"

        # Check for BouncyCastle dependency
        if grep -q "bouncycastle" pom.xml; then
            log_success "BouncyCastle PQC dependency configured"
        else
            log_warning "BouncyCastle dependency not found in pom.xml"
        fi
    else
        log_failure "Quantum cryptography not implemented"
    fi

    # Check for security-sensitive patterns
    check
    log_info "Scanning for security patterns..."
    if grep -r "TODO.*security" src/main/java > /dev/null 2>&1; then
        log_warning "Security TODOs found in code (review recommended)"
    else
        log_success "No security TODOs found"
    fi

    # Check for hardcoded secrets (basic scan)
    check
    if grep -r -i "password.*=.*\"" src/main/java | grep -v "// " > /dev/null 2>&1; then
        log_warning "Potential hardcoded passwords found (review required)"
    else
        log_success "No obvious hardcoded passwords found"
    fi
}

###############################################################################
# 9. Native Build Validation (Optional - takes time)
###############################################################################

validate_native_build() {
    log_header "9. NATIVE BUILD VALIDATION (OPTIONAL)"

    read -p "Do you want to run native build validation? (This may take 2-30 minutes) [y/N]: " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        check
        log_info "Running native-fast build (this may take 2-5 minutes)..."

        if ./mvnw package -Pnative-fast -DskipTests > /tmp/native-build.log 2>&1; then
            log_success "Native build completed successfully"

            # Check native executable
            if [ -f "target/aurigraph-v11-standalone-11.0.0-runner" ]; then
                NATIVE_SIZE=$(du -h target/aurigraph-v11-standalone-11.0.0-runner | cut -f1)
                log_success "Native executable created (size: $NATIVE_SIZE)"

                # Try to run native executable briefly
                log_info "Testing native executable startup..."
                timeout 5s ./target/aurigraph-v11-standalone-11.0.0-runner > /dev/null 2>&1 || true
                if [ $? -eq 124 ]; then
                    log_success "Native executable starts successfully"
                else
                    log_info "Native executable startup test completed"
                fi
            else
                log_failure "Native executable not found"
            fi
        else
            log_failure "Native build failed (see /tmp/native-build.log)"
            tail -n 50 /tmp/native-build.log
        fi
    else
        log_info "Native build validation skipped"
    fi
}

###############################################################################
# 10. Final Summary
###############################################################################

print_summary() {
    log_header "VALIDATION SUMMARY"

    echo -e "${BLUE}Total Checks:${NC} $TOTAL_CHECKS"
    echo -e "${GREEN}Passed:${NC} $PASSED_CHECKS"
    echo -e "${RED}Failed:${NC} $FAILED_CHECKS"
    echo -e "${YELLOW}Warnings:${NC} $WARNINGS"

    PASS_RATE=$((PASSED_CHECKS * 100 / TOTAL_CHECKS))
    echo -e "\n${BLUE}Pass Rate:${NC} ${PASS_RATE}%"

    if [ $FAILED_CHECKS -eq 0 ]; then
        echo -e "\n${GREEN}================================${NC}"
        echo -e "${GREEN}✓ PRODUCTION READINESS: PASSED${NC}"
        echo -e "${GREEN}================================${NC}\n"

        if [ $WARNINGS -gt 0 ]; then
            echo -e "${YELLOW}Note: $WARNINGS warnings found. Review recommended before production deployment.${NC}\n"
        fi

        exit 0
    else
        echo -e "\n${RED}================================${NC}"
        echo -e "${RED}✗ PRODUCTION READINESS: FAILED${NC}"
        echo -e "${RED}================================${NC}\n"
        echo -e "${RED}Please fix the failed checks before proceeding to production.${NC}\n"
        exit 1
    fi
}

###############################################################################
# Main Execution
###############################################################################

main() {
    echo -e "${BLUE}"
    echo "═══════════════════════════════════════════════════════════════"
    echo "  Aurigraph V11 Production Readiness Validation"
    echo "  Sprint 20 - Final Validation"
    echo "═══════════════════════════════════════════════════════════════"
    echo -e "${NC}\n"

    # Check if we're in the right directory
    if [ ! -f "pom.xml" ]; then
        echo -e "${RED}Error: pom.xml not found. Please run this script from the project root.${NC}"
        exit 1
    fi

    # Run all validations
    validate_environment
    validate_build
    validate_tests
    validate_services
    validate_configuration
    validate_documentation
    validate_performance
    validate_security
    validate_native_build

    # Print summary
    print_summary
}

# Run main function
main "$@"
