#!/bin/bash
################################################################################
# multi-node-launcher.sh
# Aurigraph V11 Multi-Node Container Launcher
# Starts and manages multiple Aurigraph nodes within a single Docker container
################################################################################

set -e

# Configuration from environment variables
NODE_COUNT="${AURIGRAPH_NODE_COUNT:-4}"
BASE_HTTP_PORT="${AURIGRAPH_BASE_HTTP_PORT:-9003}"
BASE_GRPC_PORT="${AURIGRAPH_BASE_GRPC_PORT:-9004}"
BASE_METRICS_PORT="${AURIGRAPH_BASE_METRICS_PORT:-9005}"
CPU_PER_NODE="${AURIGRAPH_CPU_PER_NODE:-8}"
MEMORY_PER_NODE="${AURIGRAPH_MEMORY_PER_NODE:-1024M}"
CONTAINER_ID="${HOSTNAME:-container-1}"
NATIVE_BINARY="${AURIGRAPH_BINARY:-/app/aurigraph-v11}"
LOG_DIR="${AURIGRAPH_LOG_DIR:-/app/logs}"
DATA_DIR="${AURIGRAPH_DATA_DIR:-/app/data}"
PID_DIR="${AURIGRAPH_PID_DIR:-/app/pids}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_debug() {
    if [ "${AURIGRAPH_DEBUG:-false}" = "true" ]; then
        echo -e "${BLUE}[DEBUG]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
    fi
}

# Initialize directories
initialize_directories() {
    log_info "Initializing directories..."
    mkdir -p "${LOG_DIR}" "${DATA_DIR}" "${PID_DIR}"

    for i in $(seq 0 $((NODE_COUNT-1))); do
        mkdir -p "${DATA_DIR}/node-${i}"
        mkdir -p "${LOG_DIR}/node-${i}"
    done

    log_info "Directories initialized successfully"
}

# Check system resources
check_system_resources() {
    log_info "Checking system resources..."

    # Check CPU count
    AVAILABLE_CPUS=$(nproc)
    REQUIRED_CPUS=$((NODE_COUNT * CPU_PER_NODE))

    if [ "$AVAILABLE_CPUS" -lt "$REQUIRED_CPUS" ]; then
        log_warn "Available CPUs ($AVAILABLE_CPUS) < Required CPUs ($REQUIRED_CPUS)"
        log_warn "Will use available CPUs and adjust per-node allocation"
        CPU_PER_NODE=$((AVAILABLE_CPUS / NODE_COUNT))
    fi

    # Check memory
    AVAILABLE_MEMORY=$(free -m | awk 'NR==2 {print $2}')
    REQUIRED_MEMORY=$((NODE_COUNT * ${MEMORY_PER_NODE%M}))

    if [ "$AVAILABLE_MEMORY" -lt "$REQUIRED_MEMORY" ]; then
        log_warn "Available Memory (${AVAILABLE_MEMORY}MB) < Required Memory (${REQUIRED_MEMORY}MB)"
    fi

    log_info "System check complete: ${AVAILABLE_CPUS} CPUs, ${AVAILABLE_MEMORY}MB RAM"
}

# Setup cgroups for resource isolation
setup_cgroups() {
    local node_id=$1
    local node_index=$2

    # Check if cgroup v2 is available
    if [ -d "/sys/fs/cgroup" ] && [ -f "/sys/fs/cgroup/cgroup.controllers" ]; then
        log_debug "Setting up cgroups for node-${node_index}..."

        # Create cgroup for this node
        CGROUP_PATH="/sys/fs/cgroup/aurigraph/node-${node_index}"
        mkdir -p "${CGROUP_PATH}" 2>/dev/null || true

        # Set CPU quota (cpu.max format: quota period, e.g., "800000 100000" = 8 CPUs)
        if [ -f "${CGROUP_PATH}/cpu.max" ]; then
            QUOTA=$((CPU_PER_NODE * 100000))
            echo "${QUOTA} 100000" > "${CGROUP_PATH}/cpu.max" 2>/dev/null || true
        fi

        # Set memory limit
        if [ -f "${CGROUP_PATH}/memory.max" ]; then
            MEMORY_BYTES=$((${MEMORY_PER_NODE%M} * 1024 * 1024))
            echo "${MEMORY_BYTES}" > "${CGROUP_PATH}/memory.max" 2>/dev/null || true
        fi

        log_debug "Cgroups configured for node-${node_index}"
    else
        log_warn "Cgroups v2 not available, using process-level limits only"
    fi
}

# Assign node to cgroup
assign_to_cgroup() {
    local pid=$1
    local node_index=$2

    CGROUP_PATH="/sys/fs/cgroup/aurigraph/node-${node_index}"
    if [ -d "${CGROUP_PATH}" ] && [ -f "${CGROUP_PATH}/cgroup.procs" ]; then
        echo "${pid}" > "${CGROUP_PATH}/cgroup.procs" 2>/dev/null || true
        log_debug "Assigned PID ${pid} to cgroup: ${CGROUP_PATH}"
    fi
}

# Start a single node
start_node() {
    local node_index=$1

    # Node configuration
    local node_id="node-${CONTAINER_ID}-${node_index}"
    local http_port=$((BASE_HTTP_PORT + node_index * 10))
    local grpc_port=$((BASE_GRPC_PORT + node_index * 10))
    local metrics_port=$((BASE_METRICS_PORT + node_index * 10))

    # CPU affinity (pin to specific cores)
    local cpu_start=$((node_index * CPU_PER_NODE))
    local cpu_end=$((cpu_start + CPU_PER_NODE - 1))
    local cpu_range="${cpu_start}-${cpu_end}"

    # Check if CPU range is valid
    local max_cpu=$(($(nproc) - 1))
    if [ "$cpu_end" -gt "$max_cpu" ]; then
        cpu_range="0-${max_cpu}"
        log_warn "Adjusted CPU range for node-${node_index} to ${cpu_range}"
    fi

    log_info "Starting node-${node_index}..."
    log_info "  ├─ Node ID: ${node_id}"
    log_info "  ├─ HTTP Port: ${http_port}"
    log_info "  ├─ gRPC Port: ${grpc_port}"
    log_info "  ├─ Metrics Port: ${metrics_port}"
    log_info "  ├─ CPU Affinity: ${cpu_range}"
    log_info "  └─ Memory Limit: ${MEMORY_PER_NODE}"

    # Setup cgroups
    setup_cgroups "${node_id}" "${node_index}"

    # Build command with taskset for CPU pinning
    local start_cmd="taskset -c ${cpu_range} ${NATIVE_BINARY}"

    # Add JVM options for native binary
    start_cmd="${start_cmd} \
        -Daurigraph.node.id=${node_id} \
        -Daurigraph.node.index=${node_index} \
        -Daurigraph.cluster.enabled=true \
        -Daurigraph.cluster.node-count=${NODE_COUNT} \
        -Dquarkus.http.host=0.0.0.0 \
        -Dquarkus.http.port=${http_port} \
        -Dquarkus.grpc.server.host=0.0.0.0 \
        -Dquarkus.grpc.server.port=${grpc_port} \
        -Dquarkus.management.port=${metrics_port} \
        -Daurigraph.data.dir=${DATA_DIR}/node-${node_index} \
        -Daurigraph.log.dir=${LOG_DIR}/node-${node_index}"

    # Add memory options
    start_cmd="${start_cmd} \
        -Xmx${MEMORY_PER_NODE} \
        -Xms$((${MEMORY_PER_NODE%M} / 2))M"

    # Redirect output to log file
    local log_file="${LOG_DIR}/node-${node_index}/aurigraph.log"
    local pid_file="${PID_DIR}/node-${node_index}.pid"

    # Start the node in background
    nohup ${start_cmd} >> "${log_file}" 2>&1 &
    local node_pid=$!

    # Save PID
    echo "${node_pid}" > "${pid_file}"
    NODE_PIDS[${node_index}]=${node_pid}

    # Assign to cgroup
    assign_to_cgroup "${node_pid}" "${node_index}"

    log_info "Node-${node_index} started successfully (PID: ${node_pid})"

    # Wait a bit to check if process started successfully
    sleep 2
    if ! kill -0 ${node_pid} 2>/dev/null; then
        log_error "Node-${node_index} failed to start! Check logs: ${log_file}"
        return 1
    fi

    log_debug "Node-${node_index} health check passed"
}

# Wait for node to be healthy
wait_for_node_health() {
    local node_index=$1
    local http_port=$((BASE_HTTP_PORT + node_index * 10))
    local max_attempts=30
    local attempt=0

    log_info "Waiting for node-${node_index} to be healthy..."

    while [ $attempt -lt $max_attempts ]; do
        if curl -s -f "http://localhost:${http_port}/q/health/ready" > /dev/null 2>&1; then
            log_info "Node-${node_index} is healthy!"
            return 0
        fi

        attempt=$((attempt + 1))
        sleep 1
    done

    log_warn "Node-${node_index} health check timeout after ${max_attempts}s"
    return 1
}

# Restart a failed node
restart_node() {
    local node_index=$1

    log_warn "Restarting node-${node_index}..."

    # Kill old process if still running
    if [ -n "${NODE_PIDS[${node_index}]}" ]; then
        kill -9 "${NODE_PIDS[${node_index}]}" 2>/dev/null || true
    fi

    # Wait a bit before restarting
    sleep 2

    # Start node again
    start_node "${node_index}"

    # Wait for health
    wait_for_node_health "${node_index}"
}

# Monitor nodes and restart if needed
monitor_nodes() {
    log_info "Starting node monitor..."

    local check_interval="${AURIGRAPH_MONITOR_INTERVAL:-5}"
    local consecutive_failures=0

    while true; do
        local failed_nodes=0

        for i in $(seq 0 $((NODE_COUNT-1))); do
            local pid="${NODE_PIDS[${i}]}"

            if [ -z "$pid" ]; then
                log_error "Node-${i} has no PID recorded!"
                failed_nodes=$((failed_nodes + 1))
                restart_node "${i}"
                continue
            fi

            # Check if process is running
            if ! kill -0 "${pid}" 2>/dev/null; then
                log_error "Node-${i} (PID: ${pid}) has died!"
                failed_nodes=$((failed_nodes + 1))
                restart_node "${i}"
                continue
            fi

            # Check health endpoint
            local http_port=$((BASE_HTTP_PORT + i * 10))
            if ! curl -s -f "http://localhost:${http_port}/q/health/ready" > /dev/null 2>&1; then
                log_warn "Node-${i} health check failed"
                # Don't restart immediately on health check failure
                # Give it a few more chances
            fi
        done

        # Check if too many nodes are failing
        if [ "$failed_nodes" -gt "$((NODE_COUNT / 2))" ]; then
            consecutive_failures=$((consecutive_failures + 1))
            log_error "More than half of nodes are failing! (${failed_nodes}/${NODE_COUNT})"

            if [ "$consecutive_failures" -gt 3 ]; then
                log_error "Too many consecutive failures! Exiting..."
                shutdown_all_nodes 1
            fi
        else
            consecutive_failures=0
        fi

        sleep "${check_interval}"
    done
}

# Graceful shutdown handler
shutdown_all_nodes() {
    local exit_code=${1:-0}

    log_info "Shutting down all nodes..."

    for i in $(seq 0 $((NODE_COUNT-1))); do
        if [ -n "${NODE_PIDS[${i}]}" ]; then
            log_info "Stopping node-${i} (PID: ${NODE_PIDS[${i}]})"

            # Try graceful shutdown first
            kill -TERM "${NODE_PIDS[${i}]}" 2>/dev/null || true

            # Wait for graceful shutdown
            local timeout=10
            while [ $timeout -gt 0 ] && kill -0 "${NODE_PIDS[${i}]}" 2>/dev/null; do
                sleep 1
                timeout=$((timeout - 1))
            done

            # Force kill if still running
            if kill -0 "${NODE_PIDS[${i}]}" 2>/dev/null; then
                log_warn "Force killing node-${i}"
                kill -KILL "${NODE_PIDS[${i}]}" 2>/dev/null || true
            fi

            log_info "Node-${i} stopped"
        fi
    done

    log_info "All nodes stopped. Exiting with code ${exit_code}"
    exit "${exit_code}"
}

# Print startup banner
print_banner() {
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "  Aurigraph V11 Multi-Node Container Launcher"
    echo "  Version: 11.0.0"
    echo "════════════════════════════════════════════════════════════════"
    echo "  Configuration:"
    echo "  ├─ Container ID: ${CONTAINER_ID}"
    echo "  ├─ Node Count: ${NODE_COUNT}"
    echo "  ├─ CPU per Node: ${CPU_PER_NODE} cores"
    echo "  ├─ Memory per Node: ${MEMORY_PER_NODE}"
    echo "  ├─ HTTP Port Range: ${BASE_HTTP_PORT}-$((BASE_HTTP_PORT + (NODE_COUNT-1)*10))"
    echo "  ├─ gRPC Port Range: ${BASE_GRPC_PORT}-$((BASE_GRPC_PORT + (NODE_COUNT-1)*10))"
    echo "  └─ Metrics Port Range: ${BASE_METRICS_PORT}-$((BASE_METRICS_PORT + (NODE_COUNT-1)*10))"
    echo "════════════════════════════════════════════════════════════════"
    echo ""
}

# Main execution
main() {
    # Print banner
    print_banner

    # Setup signal handlers
    trap 'shutdown_all_nodes 0' SIGTERM SIGINT

    # Initialize
    initialize_directories
    check_system_resources

    # Array to store PIDs
    declare -g -A NODE_PIDS

    # Start all nodes
    log_info "Starting ${NODE_COUNT} Aurigraph nodes..."

    for i in $(seq 0 $((NODE_COUNT-1))); do
        start_node "${i}"

        # Stagger startup to avoid resource contention
        if [ $i -lt $((NODE_COUNT-1)) ]; then
            sleep 3
        fi
    done

    log_info "All nodes started successfully!"

    # Wait for all nodes to be healthy
    log_info "Waiting for all nodes to be healthy..."
    for i in $(seq 0 $((NODE_COUNT-1))); do
        wait_for_node_health "${i}"
    done

    log_info "All nodes are healthy and ready!"

    # Display node status
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "  Node Status:"
    for i in $(seq 0 $((NODE_COUNT-1))); do
        local http_port=$((BASE_HTTP_PORT + i * 10))
        local pid="${NODE_PIDS[${i}]}"
        echo "  Node-${i}: ✓ Running (PID: ${pid}, HTTP: ${http_port})"
    done
    echo "════════════════════════════════════════════════════════════════"
    echo ""

    # Start monitoring
    monitor_nodes
}

# Run main
main
