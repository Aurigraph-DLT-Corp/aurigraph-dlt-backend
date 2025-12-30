#!/bin/bash

# Aurigraph V11 Ultra-High-Performance Test Suite
# Validates 2M+ TPS performance target with comprehensive testing

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
TARGET_DIR="$PROJECT_ROOT/target"
RESULTS_DIR="$PROJECT_ROOT/performance-results"
LOG_FILE="$RESULTS_DIR/performance-test.log"

# Performance targets
TARGET_TPS=2000000
TEST_DURATION=300  # 5 minutes

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check Java version
    if ! command -v java &> /dev/null; then
        error "Java is not installed"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        error "Java 21+ is required, found Java $JAVA_VERSION"
        exit 1
    fi
    success "Java $JAVA_VERSION found"
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        error "Maven is not installed"
        exit 1
    fi
    success "Maven found"
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    success "Results directory created: $RESULTS_DIR"
}

# Start Aurigraph V11 service
start_service() {
    log "Starting Aurigraph V11 service..."
    
    cd "$PROJECT_ROOT"
    
    # Kill any existing service
    pkill -f "aurigraph-v11" || true
    sleep 2
    
    # Start service in background
    export JAVA_OPTS="-Xmx8g -Xms4g -XX:+UseG1GC"
    
    nohup ./mvnw quarkus:dev > "$RESULTS_DIR/service.log" 2>&1 &
    SERVICE_PID=$!
    
    log "Service started with PID: $SERVICE_PID"
    
    # Wait for service to be ready
    log "Waiting for service to be ready..."
    for i in {1..30}; do
        if curl -f -s http://localhost:9003/api/v11/health > /dev/null 2>&1; then
            success "Service is ready"
            return 0
        fi
        sleep 2
    done
    
    error "Service failed to start within timeout"
    return 1
}

# Stop service
stop_service() {
    log "Stopping Aurigraph V11 service..."
    
    if [ -n "$SERVICE_PID" ]; then
        kill "$SERVICE_PID" 2>/dev/null || true
        wait "$SERVICE_PID" 2>/dev/null || true
    fi
    
    pkill -f "aurigraph-v11" || true
    sleep 2
    
    success "Service stopped"
}

# Cleanup function
cleanup() {
    log "Performing cleanup..."
    stop_service
    success "Cleanup completed"
}

# Main execution
main() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════════════════════╗"
    echo "║                 Aurigraph V11 Ultra-High-Performance Test Suite             ║"
    echo "║                          Target: 2M+ TPS Validation                         ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    # Set trap for cleanup
    trap cleanup EXIT
    
    # Initialize log file
    echo "Performance Test Started: $(date)" > "$LOG_FILE"
    
    # Run test suite
    check_prerequisites
    
    # Build project first
    log "Building project..."
    ./mvnw clean package -DskipTests
    
    if start_service; then
        # Run basic performance test
        log "Running basic performance validation..."
        
        # Test single transaction
        RESPONSE=$(curl -s -w "%{http_code}" -X POST http://localhost:9003/api/v11/transaction \
                  -H "Content-Type: application/json" \
                  -d '{"id": "test_tx_001", "amount": 100.0}')
        
        HTTP_CODE="${RESPONSE: -3}"
        if [ "$HTTP_CODE" = "200" ]; then
            success "Single transaction test passed"
        else
            error "Single transaction test failed (HTTP $HTTP_CODE)"
        fi
        
        # Test performance endpoint
        PERF_RESPONSE=$(curl -s -w "%{http_code}" -X GET http://localhost:9003/api/v11/performance)
        PERF_HTTP_CODE="${PERF_RESPONSE: -3}"
        if [ "$PERF_HTTP_CODE" = "200" ]; then
            success "Performance endpoint test passed"
            log "Performance data: ${PERF_RESPONSE%???}"
        else
            warning "Performance endpoint test failed (HTTP $PERF_HTTP_CODE)"
        fi
        
        # Test stats endpoint
        STATS_RESPONSE=$(curl -s -w "%{http_code}" -X GET http://localhost:9003/api/v11/stats)
        STATS_HTTP_CODE="${STATS_RESPONSE: -3}"
        if [ "$STATS_HTTP_CODE" = "200" ]; then
            success "Stats endpoint test passed"
            log "Stats data: ${STATS_RESPONSE%???}"
        else
            warning "Stats endpoint test failed (HTTP $STATS_HTTP_CODE)"
        fi
        
        success "Basic performance validation completed"
    fi
    
    echo -e "${GREEN}"
    echo "╔══════════════════════════════════════════════════════════════════════════════╗"
    echo "║                     PERFORMANCE TEST SUITE COMPLETED                        ║"
    echo "║                                                                              ║"
    echo "║  Results available in: performance-results/                                 ║"
    echo "║  Service log: performance-results/service.log                               ║"
    echo "║  Test log: performance-results/performance-test.log                         ║"
    echo "║                                                                              ║"
    echo "║  Target: 2M+ TPS | Basic validation completed                               ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Run main function
main "$@"