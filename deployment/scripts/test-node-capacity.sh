#!/bin/bash
################################################################################
# test-node-capacity.sh
# Test maximum number of nodes per container type
# Progressively increases node count until resource limits are reached
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Test configuration
NODE_TYPE="${1:-validator}"  # validator, business, or slim
START_NODE_COUNT="${2:-1}"
MAX_NODE_COUNT="${3:-20}"
TEST_DURATION="${4:-60}"  # seconds per test

# Results file
RESULTS_FILE="capacity-test-${NODE_TYPE}-$(date +%Y%m%d-%H%M%S).csv"

# Print header
print_header() {
    echo "════════════════════════════════════════════════════════════════"
    echo "  Aurigraph V11 Node Capacity Testing"
    echo "════════════════════════════════════════════════════════════════"
    echo "  Node Type: ${NODE_TYPE}"
    echo "  Start Count: ${START_NODE_COUNT}"
    echo "  Max Count: ${MAX_NODE_COUNT}"
    echo "  Test Duration: ${TEST_DURATION}s per test"
    echo "  Results File: ${RESULTS_FILE}"
    echo "════════════════════════════════════════════════════════════════"
    echo ""
}

# Initialize results file
init_results() {
    echo "node_count,status,cpu_usage,memory_mb,startup_time,tps,latency_p99,failed_nodes" > "${RESULTS_FILE}"
    log_info "Initialized results file: ${RESULTS_FILE}"
}

# Get system resources
get_system_resources() {
    local total_cpu=$(nproc)
    local total_memory=$(free -m | awk 'NR==2 {print $2}')

    log_info "System Resources: ${total_cpu} CPUs, ${total_memory}MB RAM"
    echo "${total_cpu},${total_memory}"
}

# Start container with N nodes
start_container() {
    local node_count=$1

    log_info "Starting container with ${node_count} nodes..."

    # Build image if not exists
    if ! docker image inspect "aurigraph/v11-${NODE_TYPE}:latest" > /dev/null 2>&1; then
        log_info "Building ${NODE_TYPE} image..."
        docker build -f "Dockerfile.${NODE_TYPE}" -t "aurigraph/v11-${NODE_TYPE}:latest" .
    fi

    # Start container
    local container_name="aurigraph-capacity-test"

    # Set resources based on node type
    local cpu_limit memory_limit
    case "${NODE_TYPE}" in
        validator)
            cpu_limit=$((node_count * 8))
            memory_limit=$((node_count * 1024))M
            ;;
        business)
            cpu_limit=$((node_count * 4))
            memory_limit=$((node_count * 512))M
            ;;
        slim)
            cpu_limit=$((node_count * 2))
            memory_limit=$((node_count * 256))M
            ;;
    esac

    docker run -d \
        --name "${container_name}" \
        --cpus="${cpu_limit}" \
        --memory="${memory_limit}" \
        -e AURIGRAPH_NODE_COUNT="${node_count}" \
        -e AURIGRAPH_DEBUG=false \
        -e AURIGRAPH_LOG_LEVEL=WARN \
        -p 9003-$((9003 + node_count * 10)):9003-$((9003 + node_count * 10)) \
        "aurigraph/v11-${NODE_TYPE}:latest"

    log_info "Container started with ${node_count} nodes (CPU: ${cpu_limit}, Memory: ${memory_limit})"
}

# Wait for nodes to be healthy
wait_for_health() {
    local node_count=$1
    local max_wait=300  # 5 minutes
    local elapsed=0

    log_info "Waiting for nodes to be healthy..."

    local start_time=$(date +%s)
    local healthy_count=0

    while [ $elapsed -lt $max_wait ]; do
        healthy_count=0

        for i in $(seq 0 $((node_count - 1))); do
            local port=$((9003 + i * 10))
            if curl -s -f "http://localhost:${port}/q/health/ready" > /dev/null 2>&1; then
                healthy_count=$((healthy_count + 1))
            fi
        done

        if [ $healthy_count -eq $node_count ]; then
            local startup_time=$(($(date +%s) - start_time))
            log_info "All ${node_count} nodes are healthy! (Startup time: ${startup_time}s)"
            echo "${startup_time}"
            return 0
        fi

        sleep 2
        elapsed=$(($(date +%s) - start_time))
    done

    log_error "Health check timeout! Only ${healthy_count}/${node_count} nodes healthy"
    echo "-1"
    return 1
}

# Measure container performance
measure_performance() {
    local node_count=$1
    local duration=$2

    log_info "Measuring performance for ${duration}s..."

    # Get container stats
    local container_name="aurigraph-capacity-test"
    local stats=$(docker stats --no-stream --format "{{.CPUPerc}},{{.MemUsage}}" "${container_name}")

    local cpu_usage=$(echo "${stats}" | cut -d',' -f1 | tr -d '%')
    local memory_usage=$(echo "${stats}" | cut -d',' -f2 | cut -d'/' -f1 | sed 's/MiB//g' | tr -d ' ')

    log_info "Container Stats: CPU ${cpu_usage}%, Memory ${memory_usage}MB"

    # Test TPS for each node
    local total_tps=0
    local max_latency=0
    local failed_nodes=0

    for i in $(seq 0 $((node_count - 1))); do
        local port=$((9003 + i * 10))

        # Simple performance test
        if curl -s -f "http://localhost:${port}/api/v11/performance" > /dev/null 2>&1; then
            local node_tps=$(curl -s "http://localhost:${port}/api/v11/stats" | jq -r '.tps // 0' 2>/dev/null || echo "0")
            local node_latency=$(curl -s "http://localhost:${port}/api/v11/stats" | jq -r '.latency_p99 // 0' 2>/dev/null || echo "0")

            total_tps=$(echo "${total_tps} + ${node_tps}" | bc)

            if [ $(echo "${node_latency} > ${max_latency}" | bc) -eq 1 ]; then
                max_latency=${node_latency}
            fi
        else
            failed_nodes=$((failed_nodes + 1))
        fi
    done

    log_info "Performance: ${total_tps} TPS, P99 Latency: ${max_latency}ms, Failed: ${failed_nodes}"

    echo "${cpu_usage},${memory_usage},${total_tps},${max_latency},${failed_nodes}"
}

# Stop and remove container
cleanup() {
    local container_name="aurigraph-capacity-test"

    if docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
        log_info "Cleaning up container..."
        docker stop "${container_name}" > /dev/null 2>&1
        docker rm "${container_name}" > /dev/null 2>&1
    fi
}

# Run capacity test for N nodes
test_capacity() {
    local node_count=$1

    log_info "════════════════════════════════════════════════════════════════"
    log_info "Testing ${NODE_TYPE} with ${node_count} nodes..."
    log_info "════════════════════════════════════════════════════════════════"

    # Cleanup previous test
    cleanup

    # Start container
    if ! start_container "${node_count}"; then
        log_error "Failed to start container"
        echo "${node_count},failed_to_start,0,0,-1,0,0,${node_count}" >> "${RESULTS_FILE}"
        return 1
    fi

    # Wait for health
    local startup_time
    startup_time=$(wait_for_health "${node_count}")

    if [ "${startup_time}" = "-1" ]; then
        log_error "Nodes failed to become healthy"
        echo "${node_count},unhealthy,0,0,${startup_time},0,0,${node_count}" >> "${RESULTS_FILE}"
        cleanup
        return 1
    fi

    # Measure performance
    local perf_data
    perf_data=$(measure_performance "${node_count}" "${TEST_DURATION}")

    local cpu_usage=$(echo "${perf_data}" | cut -d',' -f1)
    local memory_mb=$(echo "${perf_data}" | cut -d',' -f2)
    local tps=$(echo "${perf_data}" | cut -d',' -f3)
    local latency=$(echo "${perf_data}" | cut -d',' -f4)
    local failed=$(echo "${perf_data}" | cut -d',' -f5)

    # Check if test passed
    local status="success"
    if [ "${failed}" -gt 0 ]; then
        status="degraded"
        log_warn "Test degraded: ${failed} nodes failed"
    fi

    if [ $(echo "${cpu_usage} > 95" | bc) -eq 1 ]; then
        status="cpu_saturated"
        log_warn "CPU saturated: ${cpu_usage}%"
    fi

    # Record results
    echo "${node_count},${status},${cpu_usage},${memory_mb},${startup_time},${tps},${latency},${failed}" >> "${RESULTS_FILE}"

    log_info "Test Results: ${status} (TPS: ${tps}, Latency: ${latency}ms, Failed: ${failed})"

    # Cleanup
    cleanup

    # Determine if we should continue
    if [ "${status}" = "cpu_saturated" ] || [ "${failed}" -gt $((node_count / 2)) ]; then
        log_warn "Capacity limit reached at ${node_count} nodes"
        return 1
    fi

    return 0
}

# Generate summary report
generate_summary() {
    log_info "════════════════════════════════════════════════════════════════"
    log_info "Capacity Test Summary"
    log_info "════════════════════════════════════════════════════════════════"

    # Find maximum successful node count
    local max_success=$(awk -F',' '$2=="success" {print $1}' "${RESULTS_FILE}" | tail -1)
    local max_degraded=$(awk -F',' '$2=="degraded" {print $1}' "${RESULTS_FILE}" | tail -1)

    log_info "Node Type: ${NODE_TYPE}"
    log_info "Maximum Successful Nodes: ${max_success:-0}"
    log_info "Maximum Degraded Nodes: ${max_degraded:-0}"

    # Display results table
    echo ""
    log_info "Detailed Results:"
    column -t -s',' "${RESULTS_FILE}"

    # Calculate recommendations
    local recommended_nodes=$((max_success - 1))
    if [ "${recommended_nodes}" -lt 1 ]; then
        recommended_nodes=1
    fi

    echo ""
    log_info "════════════════════════════════════════════════════════════════"
    log_info "Recommendations:"
    log_info "  - Maximum nodes: ${max_success}"
    log_info "  - Recommended nodes: ${recommended_nodes} (with safety margin)"
    log_info "  - Node type: ${NODE_TYPE}"
    log_info "════════════════════════════════════════════════════════════════"

    # Update Dockerfile label
    log_info "Updating Dockerfile.${NODE_TYPE} with max_nodes label..."
    sed -i.bak "s/max-nodes=\"[0-9]*\"/max-nodes=\"${max_success}\"/" "Dockerfile.${NODE_TYPE}" 2>/dev/null || true
}

# Main execution
main() {
    print_header
    init_results

    # Run progressive capacity tests
    for node_count in $(seq "${START_NODE_COUNT}" "${MAX_NODE_COUNT}"); do
        if ! test_capacity "${node_count}"; then
            log_warn "Capacity limit reached, stopping tests"
            break
        fi

        # Cool down between tests
        sleep 5
    done

    # Generate summary
    generate_summary

    log_info "Capacity testing complete! Results saved to: ${RESULTS_FILE}"
}

# Cleanup on exit
trap cleanup EXIT

# Run main
main
