#!/bin/bash
################################################################################
# health-check.sh
# Docker health check script for Aurigraph V11 nodes
# Checks both Quarkus HTTP API and LevelDB database health
################################################################################

set -e

# Configuration
HTTP_PORT=${QUARKUS_HTTP_PORT:-9003}
LEVELDB_PATH=${LEVELDB_PATH:-/app/data/leveldb}
TIMEOUT=${HEALTH_CHECK_TIMEOUT:-3}

# Health check exit codes
EXIT_SUCCESS=0
EXIT_UNHEALTHY=1

# Colors for logging (ignored in Docker but useful for debugging)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

################################################################################
# Check Quarkus HTTP API health
################################################################################
check_http_health() {
    local endpoint="http://localhost:${HTTP_PORT}/q/health/ready"

    # Try to reach the health endpoint
    response=$(curl -sf --max-time ${TIMEOUT} "${endpoint}" 2>/dev/null || echo "")

    if [ -z "$response" ]; then
        echo "❌ HTTP health check failed - no response from ${endpoint}"
        return 1
    fi

    # Verify status is UP
    if echo "$response" | grep -q '"status":"UP"'; then
        echo "✅ HTTP health check passed"
        return 0
    else
        echo "❌ HTTP health check failed - status not UP"
        echo "Response: $response"
        return 1
    fi
}

################################################################################
# Check LevelDB database health
################################################################################
check_leveldb_health() {
    # Check if LevelDB path exists
    if [ ! -d "${LEVELDB_PATH}" ]; then
        echo "❌ LevelDB database not found at ${LEVELDB_PATH}"
        return 1
    fi

    # Check if we can read from the database directory
    if [ ! -r "${LEVELDB_PATH}" ]; then
        echo "❌ LevelDB database is not readable"
        return 1
    fi

    # Check for corruption markers
    if [ -f "${LEVELDB_PATH}/LOCK" ]; then
        echo "⚠️  LevelDB lock file present (may be in use)"
        # This is expected during normal operation, not a failure
    fi

    # Check for manifest file (indicates valid database)
    if [ ! -f "${LEVELDB_PATH}/MANIFEST-000001" ] && [ ! -f "${LEVELDB_PATH}/MANIFEST-"* ]; then
        echo "⚠️  LevelDB MANIFEST not found - database may not be initialized yet"
        # Not a failure during startup
    fi

    # Check disk space
    available_space=$(df "${LEVELDB_PATH}" | awk 'NR==2 {print $4}')
    if [ "$available_space" -lt 104857 ]; then  # Less than 100MB
        echo "❌ LevelDB: Insufficient disk space (${available_space}KB available)"
        return 1
    fi

    echo "✅ LevelDB health check passed"
    return 0
}

################################################################################
# Check Process Health
################################################################################
check_process_health() {
    # Verify the Java/native process is still running
    if ! pgrep -f aurigraph >/dev/null 2>&1; then
        echo "❌ Aurigraph process is not running"
        return 1
    fi

    echo "✅ Process health check passed"
    return 0
}

################################################################################
# Check File Descriptors
################################################################################
check_fd_health() {
    # Get current open file descriptors for the process
    if [ -f /proc/sys/fs/file-max ]; then
        max_fds=$(cat /proc/sys/fs/file-max)
        current_fds=$(lsof -p $$ 2>/dev/null | wc -l || echo "0")

        # If using more than 80% of available FDs, warn
        if [ "$current_fds" -gt $((max_fds * 8 / 10)) ]; then
            echo "⚠️  High file descriptor usage: $current_fds / $max_fds"
        fi
    fi

    return 0
}

################################################################################
# Main health check
################################################################################
main() {
    local all_passed=true

    echo "🔍 Running health checks..."
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    # Check process health first
    if ! check_process_health; then
        all_passed=false
    fi

    # Check HTTP health
    if ! check_http_health; then
        all_passed=false
    fi

    # Check LevelDB health
    if ! check_leveldb_health; then
        all_passed=false
    fi

    # Check FD health (warning only)
    check_fd_health

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    if [ "$all_passed" = true ]; then
        echo "✅ All health checks passed"
        exit ${EXIT_SUCCESS}
    else
        echo "❌ Health checks failed"
        exit ${EXIT_UNHEALTHY}
    fi
}

# Run main function
main
