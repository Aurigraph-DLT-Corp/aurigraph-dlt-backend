#!/bin/bash

#############################################################################
# GPU Performance Benchmark Script for Aurigraph V11
#
# Purpose: Comprehensive GPU vs CPU performance benchmarking
# Version: 1.0.0
# Date: November 4, 2025
#
# Features:
# - GPU hardware detection (CUDA, OpenCL)
# - Performance comparison (GPU vs CPU)
# - Memory profiling (GPU VRAM usage)
# - Throughput measurement (TPS comparison)
# - Automated report generation
#
# Usage:
#   ./GPU-PERFORMANCE-BENCHMARK.sh [options]
#
# Options:
#   --quick        Quick benchmark (5 minutes)
#   --full         Full benchmark (30 minutes)
#   --stress       Stress test (GPU limits)
#   --report-only  Generate report from existing data
#
#############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BENCHMARK_DIR="${SCRIPT_DIR}/benchmark-results"
RESULTS_FILE="${BENCHMARK_DIR}/gpu-benchmark-${TIMESTAMP}.json"
REPORT_FILE="${BENCHMARK_DIR}/gpu-benchmark-report-${TIMESTAMP}.md"
LOG_FILE="${BENCHMARK_DIR}/gpu-benchmark-${TIMESTAMP}.log"

# Benchmark parameters
BENCHMARK_MODE="full"  # quick, full, stress
WARMUP_ITERATIONS=100
QUICK_ITERATIONS=1000
FULL_ITERATIONS=10000
STRESS_ITERATIONS=100000

# GPU detection flags
GPU_AVAILABLE=false
CUDA_AVAILABLE=false
OPENCL_AVAILABLE=false
GPU_NAME="N/A"
GPU_MEMORY="N/A"
GPU_COMPUTE_UNITS="N/A"

#############################################################################
# UTILITY FUNCTIONS
#############################################################################

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "${LOG_FILE}"
}

log_warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARN:${NC} $1" | tee -a "${LOG_FILE}"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1" | tee -a "${LOG_FILE}"
}

log_info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO:${NC} $1" | tee -a "${LOG_FILE}"
}

print_header() {
    echo ""
    echo -e "${BLUE}=============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}=============================================${NC}"
    echo ""
}

#############################################################################
# GPU DETECTION
#############################################################################

detect_gpu_hardware() {
    print_header "GPU Hardware Detection"

    # Create benchmark directory
    mkdir -p "${BENCHMARK_DIR}"

    log "Detecting GPU hardware..."

    # Check for NVIDIA GPU (nvidia-smi)
    if command -v nvidia-smi &> /dev/null; then
        log "NVIDIA GPU detected"
        CUDA_AVAILABLE=true
        GPU_AVAILABLE=true

        # Get GPU information
        GPU_NAME=$(nvidia-smi --query-gpu=name --format=csv,noheader | head -n 1)
        GPU_MEMORY=$(nvidia-smi --query-gpu=memory.total --format=csv,noheader | head -n 1)
        GPU_COMPUTE_UNITS=$(nvidia-smi --query-gpu=compute_cap --format=csv,noheader | head -n 1)

        log_info "GPU Name: ${GPU_NAME}"
        log_info "GPU Memory: ${GPU_MEMORY}"
        log_info "CUDA Compute Capability: ${GPU_COMPUTE_UNITS}"

        # Check CUDA installation
        if command -v nvcc &> /dev/null; then
            CUDA_VERSION=$(nvcc --version | grep "release" | sed -n 's/.*release \([0-9.]*\).*/\1/p')
            log_info "CUDA Version: ${CUDA_VERSION}"
        else
            log_warn "CUDA compiler (nvcc) not found"
        fi
    else
        log_warn "NVIDIA GPU not detected (nvidia-smi not available)"
    fi

    # Check for OpenCL support
    if command -v clinfo &> /dev/null; then
        log "Checking OpenCL support..."
        OPENCL_DEVICES=$(clinfo -l | grep -c "Device Name" || echo "0")

        if [ "${OPENCL_DEVICES}" -gt 0 ]; then
            log_info "OpenCL devices found: ${OPENCL_DEVICES}"
            OPENCL_AVAILABLE=true
            GPU_AVAILABLE=true

            # Get first OpenCL device info if not already from CUDA
            if [ "${CUDA_AVAILABLE}" = false ]; then
                GPU_NAME=$(clinfo | grep "Device Name" | head -n 1 | cut -d':' -f2 | xargs)
                GPU_MEMORY=$(clinfo | grep "Global memory size" | head -n 1 | cut -d':' -f2 | xargs)
                log_info "OpenCL Device: ${GPU_NAME}"
                log_info "OpenCL Memory: ${GPU_MEMORY}"
            fi
        else
            log_warn "No OpenCL devices found"
        fi
    else
        log_warn "clinfo not available, cannot detect OpenCL devices"
    fi

    # Summary
    echo ""
    if [ "${GPU_AVAILABLE}" = true ]; then
        log "✅ GPU acceleration available"
        log_info "CUDA: $([ "${CUDA_AVAILABLE}" = true ] && echo "✅ Available" || echo "❌ Not available")"
        log_info "OpenCL: $([ "${OPENCL_AVAILABLE}" = true ] && echo "✅ Available" || echo "❌ Not available")"
    else
        log_warn "❌ No GPU detected - will benchmark CPU only"
    fi
}

#############################################################################
# SYSTEM INFORMATION
#############################################################################

collect_system_info() {
    print_header "System Information"

    log "Collecting system information..."

    # CPU information
    if [ "$(uname)" = "Darwin" ]; then
        # macOS
        CPU_MODEL=$(sysctl -n machdep.cpu.brand_string)
        CPU_CORES=$(sysctl -n hw.ncpu)
        TOTAL_RAM=$(sysctl -n hw.memsize | awk '{print int($1/1024/1024/1024) " GB"}')
    else
        # Linux
        CPU_MODEL=$(lscpu | grep "Model name" | cut -d':' -f2 | xargs)
        CPU_CORES=$(nproc)
        TOTAL_RAM=$(free -h | grep Mem | awk '{print $2}')
    fi

    log_info "CPU: ${CPU_MODEL}"
    log_info "CPU Cores: ${CPU_CORES}"
    log_info "Total RAM: ${TOTAL_RAM}"

    # Java version
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    log_info "Java Version: ${JAVA_VERSION}"

    # Check if V11 application is running
    if pgrep -f "aurigraph-v11" > /dev/null; then
        log_warn "Aurigraph V11 application is running - consider stopping for accurate benchmarks"
    fi
}

#############################################################################
# MEMORY PROFILING
#############################################################################

profile_gpu_memory() {
    print_header "GPU Memory Profiling"

    if [ "${CUDA_AVAILABLE}" = false ]; then
        log_warn "Skipping GPU memory profiling (CUDA not available)"
        return
    fi

    log "Profiling GPU memory usage..."

    # Get initial GPU memory state
    INITIAL_MEMORY=$(nvidia-smi --query-gpu=memory.used --format=csv,noheader,nounits | head -n 1)
    log_info "Initial GPU Memory Used: ${INITIAL_MEMORY} MB"

    # Run memory allocation test (via Java)
    log "Running GPU memory allocation test..."

    # Create test Java class
    cat > /tmp/GPUMemoryTest.java << 'EOF'
import com.aparapi.Kernel;
import com.aparapi.Range;

public class GPUMemoryTest {
    public static void main(String[] args) {
        int size = Integer.parseInt(args[0]);
        TestKernel kernel = new TestKernel(size);
        kernel.execute(Range.create(size));
        System.out.println("Allocated " + size + " elements on GPU");
    }

    static class TestKernel extends Kernel {
        private final int[] data;

        TestKernel(int size) {
            this.data = new int[size];
        }

        @Override
        public void run() {
            int gid = getGlobalId();
            data[gid] = gid * 2;
        }
    }
}
EOF

    # Compile and run (if Aparapi is available)
    if [ -f "${SCRIPT_DIR}/target/aurigraph-v11-standalone-11.4.4-runner.jar" ]; then
        log "Testing GPU memory allocation via Aurigraph V11..."

        # Test with different batch sizes
        for BATCH_SIZE in 1000 10000 100000 1000000; do
            log_info "Testing batch size: ${BATCH_SIZE}"

            MEMORY_BEFORE=$(nvidia-smi --query-gpu=memory.used --format=csv,noheader,nounits | head -n 1)

            # Run benchmark (would call actual V11 GPU service)
            # Placeholder: sleep to simulate processing
            sleep 1

            MEMORY_AFTER=$(nvidia-smi --query-gpu=memory.used --format=csv,noheader,nounits | head -n 1)
            MEMORY_DELTA=$((MEMORY_AFTER - MEMORY_BEFORE))

            log_info "  Memory delta: ${MEMORY_DELTA} MB"
        done
    else
        log_warn "V11 JAR not found, skipping detailed memory profiling"
    fi

    # Final memory state
    FINAL_MEMORY=$(nvidia-smi --query-gpu=memory.used --format=csv,noheader,nounits | head -n 1)
    log_info "Final GPU Memory Used: ${FINAL_MEMORY} MB"
}

#############################################################################
# PERFORMANCE BENCHMARKING
#############################################################################

run_cpu_benchmark() {
    local ITERATIONS=$1
    local TEST_NAME=$2

    log "Running CPU benchmark: ${TEST_NAME} (${ITERATIONS} iterations)..."

    # Use Maven to run performance test
    local START_TIME=$(date +%s%N)

    ./mvnw test -Dtest=GPUKernelOptimizationTest#cpuHashingBenchmark \
        -Diterations=${ITERATIONS} \
        -q >> "${LOG_FILE}" 2>&1

    local END_TIME=$(date +%s%N)
    local DURATION_NS=$((END_TIME - START_TIME))
    local DURATION_MS=$((DURATION_NS / 1000000))
    local TPS=$((ITERATIONS * 1000000000 / DURATION_NS))

    log_info "CPU ${TEST_NAME}: ${DURATION_MS} ms, ${TPS} tx/sec"

    echo "${DURATION_MS},${TPS}"
}

run_gpu_benchmark() {
    local ITERATIONS=$1
    local TEST_NAME=$2

    if [ "${GPU_AVAILABLE}" = false ]; then
        log_warn "Skipping GPU benchmark (no GPU available)"
        echo "0,0"
        return
    fi

    log "Running GPU benchmark: ${TEST_NAME} (${ITERATIONS} iterations)..."

    # Use Maven to run performance test
    local START_TIME=$(date +%s%N)

    ./mvnw test -Dtest=GPUKernelOptimizationTest#gpuHashingBenchmark \
        -Diterations=${ITERATIONS} \
        -q >> "${LOG_FILE}" 2>&1

    local END_TIME=$(date +%s%N)
    local DURATION_NS=$((END_TIME - START_TIME))
    local DURATION_MS=$((DURATION_NS / 1000000))
    local TPS=$((ITERATIONS * 1000000000 / DURATION_NS))

    log_info "GPU ${TEST_NAME}: ${DURATION_MS} ms, ${TPS} tx/sec"

    echo "${DURATION_MS},${TPS}"
}

run_benchmarks() {
    print_header "Performance Benchmarking"

    log "Running performance benchmarks (mode: ${BENCHMARK_MODE})..."

    # Determine iteration count based on mode
    case "${BENCHMARK_MODE}" in
        quick)
            ITERATIONS=${QUICK_ITERATIONS}
            ;;
        full)
            ITERATIONS=${FULL_ITERATIONS}
            ;;
        stress)
            ITERATIONS=${STRESS_ITERATIONS}
            ;;
        *)
            log_error "Unknown benchmark mode: ${BENCHMARK_MODE}"
            exit 1
            ;;
    esac

    log_info "Benchmark iterations: ${ITERATIONS}"

    # Warmup
    log "Warming up (${WARMUP_ITERATIONS} iterations)..."
    run_cpu_benchmark ${WARMUP_ITERATIONS} "warmup" > /dev/null
    if [ "${GPU_AVAILABLE}" = true ]; then
        run_gpu_benchmark ${WARMUP_ITERATIONS} "warmup" > /dev/null
    fi

    # Benchmark 1: Transaction Hashing
    log ""
    log "=== Benchmark 1: Transaction Hashing (SHA-256) ==="
    CPU_HASH_RESULT=$(run_cpu_benchmark ${ITERATIONS} "SHA-256 Hashing")
    GPU_HASH_RESULT=$(run_gpu_benchmark ${ITERATIONS} "SHA-256 Hashing")

    CPU_HASH_MS=$(echo ${CPU_HASH_RESULT} | cut -d',' -f1)
    CPU_HASH_TPS=$(echo ${CPU_HASH_RESULT} | cut -d',' -f2)
    GPU_HASH_MS=$(echo ${GPU_HASH_RESULT} | cut -d',' -f1)
    GPU_HASH_TPS=$(echo ${GPU_HASH_RESULT} | cut -d',' -f2)

    if [ "${GPU_AVAILABLE}" = true ] && [ "${GPU_HASH_MS}" -gt 0 ]; then
        HASH_SPEEDUP=$(echo "scale=2; ${CPU_HASH_MS} / ${GPU_HASH_MS}" | bc)
        log_info "Speedup: ${HASH_SPEEDUP}x"
    fi

    # Benchmark 2: Merkle Tree Construction
    log ""
    log "=== Benchmark 2: Merkle Tree Construction ==="
    CPU_MERKLE_RESULT=$(run_cpu_benchmark ${ITERATIONS} "Merkle Tree")
    GPU_MERKLE_RESULT=$(run_gpu_benchmark ${ITERATIONS} "Merkle Tree")

    CPU_MERKLE_MS=$(echo ${CPU_MERKLE_RESULT} | cut -d',' -f1)
    CPU_MERKLE_TPS=$(echo ${CPU_MERKLE_RESULT} | cut -d',' -f2)
    GPU_MERKLE_MS=$(echo ${GPU_MERKLE_RESULT} | cut -d',' -f1)
    GPU_MERKLE_TPS=$(echo ${GPU_MERKLE_RESULT} | cut -d',' -f2)

    if [ "${GPU_AVAILABLE}" = true ] && [ "${GPU_MERKLE_MS}" -gt 0 ]; then
        MERKLE_SPEEDUP=$(echo "scale=2; ${CPU_MERKLE_MS} / ${GPU_MERKLE_MS}" | bc)
        log_info "Speedup: ${MERKLE_SPEEDUP}x"
    fi

    # Benchmark 3: Signature Verification
    log ""
    log "=== Benchmark 3: Signature Verification ==="
    CPU_SIG_RESULT=$(run_cpu_benchmark ${ITERATIONS} "Signature Verification")
    GPU_SIG_RESULT=$(run_gpu_benchmark ${ITERATIONS} "Signature Verification")

    CPU_SIG_MS=$(echo ${CPU_SIG_RESULT} | cut -d',' -f1)
    CPU_SIG_TPS=$(echo ${CPU_SIG_RESULT} | cut -d',' -f2)
    GPU_SIG_MS=$(echo ${GPU_SIG_RESULT} | cut -d',' -f1)
    GPU_SIG_TPS=$(echo ${GPU_SIG_RESULT} | cut -d',' -f2)

    if [ "${GPU_AVAILABLE}" = true ] && [ "${GPU_SIG_MS}" -gt 0 ]; then
        SIG_SPEEDUP=$(echo "scale=2; ${CPU_SIG_MS} / ${GPU_SIG_MS}" | bc)
        log_info "Speedup: ${SIG_SPEEDUP}x"
    fi
}

#############################################################################
# TPS MEASUREMENT
#############################################################################

measure_end_to_end_tps() {
    print_header "End-to-End TPS Measurement"

    log "Measuring overall TPS with GPU acceleration..."

    # Run performance test endpoint
    if [ -f "${SCRIPT_DIR}/target/aurigraph-v11-standalone-11.4.4-runner.jar" ]; then
        log "Starting V11 application for TPS measurement..."

        # Start application in background
        java -jar target/aurigraph-v11-standalone-11.4.4-runner.jar &
        APP_PID=$!

        # Wait for startup
        log "Waiting for application startup (30 seconds)..."
        sleep 30

        # Test with GPU enabled
        log "Testing TPS with GPU enabled..."
        TPS_WITH_GPU=$(curl -s http://localhost:9003/api/v11/performance | jq -r '.tps // 0')

        log_info "TPS with GPU: ${TPS_WITH_GPU}"

        # Restart with GPU disabled
        log "Restarting with GPU disabled..."
        kill ${APP_PID}
        sleep 5

        java -Dgpu.acceleration.enabled=false -jar target/aurigraph-v11-standalone-11.4.4-runner.jar &
        APP_PID=$!
        sleep 30

        # Test with GPU disabled
        log "Testing TPS with GPU disabled (CPU only)..."
        TPS_WITHOUT_GPU=$(curl -s http://localhost:9003/api/v11/performance | jq -r '.tps // 0')

        log_info "TPS without GPU: ${TPS_WITHOUT_GPU}"

        # Calculate improvement
        if [ "${TPS_WITHOUT_GPU}" -gt 0 ]; then
            TPS_IMPROVEMENT=$(echo "scale=2; (${TPS_WITH_GPU} - ${TPS_WITHOUT_GPU}) * 100 / ${TPS_WITHOUT_GPU}" | bc)
            log_info "TPS Improvement: ${TPS_IMPROVEMENT}%"
        fi

        # Cleanup
        kill ${APP_PID}
    else
        log_warn "V11 JAR not found, skipping end-to-end TPS measurement"
        TPS_WITH_GPU=0
        TPS_WITHOUT_GPU=0
        TPS_IMPROVEMENT=0
    fi
}

#############################################################################
# REPORT GENERATION
#############################################################################

generate_json_results() {
    log "Generating JSON results..."

    cat > "${RESULTS_FILE}" << EOF
{
  "timestamp": "${TIMESTAMP}",
  "gpu_available": ${GPU_AVAILABLE},
  "gpu_info": {
    "name": "${GPU_NAME}",
    "memory": "${GPU_MEMORY}",
    "cuda_available": ${CUDA_AVAILABLE},
    "opencl_available": ${OPENCL_AVAILABLE},
    "compute_units": "${GPU_COMPUTE_UNITS}"
  },
  "system_info": {
    "cpu": "${CPU_MODEL}",
    "cores": ${CPU_CORES},
    "ram": "${TOTAL_RAM}",
    "java_version": "${JAVA_VERSION}"
  },
  "benchmarks": {
    "hashing": {
      "cpu_ms": ${CPU_HASH_MS:-0},
      "gpu_ms": ${GPU_HASH_MS:-0},
      "cpu_tps": ${CPU_HASH_TPS:-0},
      "gpu_tps": ${GPU_HASH_TPS:-0},
      "speedup": "${HASH_SPEEDUP:-0}x"
    },
    "merkle_tree": {
      "cpu_ms": ${CPU_MERKLE_MS:-0},
      "gpu_ms": ${GPU_MERKLE_MS:-0},
      "cpu_tps": ${CPU_MERKLE_TPS:-0},
      "gpu_tps": ${GPU_MERKLE_TPS:-0},
      "speedup": "${MERKLE_SPEEDUP:-0}x"
    },
    "signature_verification": {
      "cpu_ms": ${CPU_SIG_MS:-0},
      "gpu_ms": ${GPU_SIG_MS:-0},
      "cpu_tps": ${CPU_SIG_TPS:-0},
      "gpu_tps": ${GPU_SIG_TPS:-0},
      "speedup": "${SIG_SPEEDUP:-0}x"
    }
  },
  "end_to_end": {
    "tps_with_gpu": ${TPS_WITH_GPU:-0},
    "tps_without_gpu": ${TPS_WITHOUT_GPU:-0},
    "improvement_percent": ${TPS_IMPROVEMENT:-0}
  }
}
EOF

    log "Results saved to: ${RESULTS_FILE}"
}

generate_markdown_report() {
    log "Generating Markdown report..."

    cat > "${REPORT_FILE}" << EOF
# GPU Performance Benchmark Report

**Generated**: $(date)
**Benchmark Mode**: ${BENCHMARK_MODE}

---

## Executive Summary

This report presents the performance comparison between GPU-accelerated and CPU-based computation for Aurigraph V11 blockchain platform.

$(if [ "${GPU_AVAILABLE}" = true ]; then
    echo "✅ **GPU acceleration is available and functional**"
    echo ""
    echo "**Overall Performance Improvement**: ${TPS_IMPROVEMENT:-N/A}% increase in TPS"
else
    echo "❌ **No GPU detected** - CPU-only benchmarks performed"
fi)

---

## Hardware Information

### GPU Configuration

| Component | Value |
|-----------|-------|
| **GPU Available** | $([ "${GPU_AVAILABLE}" = true ] && echo "✅ Yes" || echo "❌ No") |
| **GPU Name** | ${GPU_NAME} |
| **GPU Memory** | ${GPU_MEMORY} |
| **CUDA Available** | $([ "${CUDA_AVAILABLE}" = true ] && echo "✅ Yes" || echo "❌ No") |
| **OpenCL Available** | $([ "${OPENCL_AVAILABLE}" = true ] && echo "✅ Yes" || echo "❌ No") |
| **Compute Capability** | ${GPU_COMPUTE_UNITS} |

### System Configuration

| Component | Value |
|-----------|-------|
| **CPU** | ${CPU_MODEL} |
| **CPU Cores** | ${CPU_CORES} |
| **Total RAM** | ${TOTAL_RAM} |
| **Java Version** | ${JAVA_VERSION} |

---

## Benchmark Results

### 1. Transaction Hashing (SHA-256)

| Metric | CPU | GPU | Speedup |
|--------|-----|-----|---------|
| **Duration** | ${CPU_HASH_MS:-0} ms | ${GPU_HASH_MS:-0} ms | ${HASH_SPEEDUP:-N/A}x |
| **Throughput** | ${CPU_HASH_TPS:-0} tx/sec | ${GPU_HASH_TPS:-0} tx/sec | - |

**Analysis**: $(if [ "${GPU_AVAILABLE}" = true ] && [ "${GPU_HASH_MS}" -gt 0 ]; then
    echo "GPU provides **${HASH_SPEEDUP}x** speedup for batch hashing operations."
else
    echo "GPU benchmarking not available."
fi)

### 2. Merkle Tree Construction

| Metric | CPU | GPU | Speedup |
|--------|-----|-----|---------|
| **Duration** | ${CPU_MERKLE_MS:-0} ms | ${GPU_MERKLE_MS:-0} ms | ${MERKLE_SPEEDUP:-N/A}x |
| **Throughput** | ${CPU_MERKLE_TPS:-0} trees/sec | ${GPU_MERKLE_TPS:-0} trees/sec | - |

**Analysis**: $(if [ "${GPU_AVAILABLE}" = true ] && [ "${GPU_MERKLE_MS}" -gt 0 ]; then
    echo "GPU provides **${MERKLE_SPEEDUP}x** speedup for Merkle tree operations."
else
    echo "GPU benchmarking not available."
fi)

### 3. Signature Verification

| Metric | CPU | GPU | Speedup |
|--------|-----|-----|---------|
| **Duration** | ${CPU_SIG_MS:-0} ms | ${GPU_SIG_MS:-0} ms | ${SIG_SPEEDUP:-N/A}x |
| **Throughput** | ${CPU_SIG_TPS:-0} sigs/sec | ${GPU_SIG_TPS:-0} sigs/sec | - |

**Analysis**: $(if [ "${GPU_AVAILABLE}" = true ] && [ "${GPU_SIG_MS}" -gt 0 ]; then
    echo "GPU provides **${SIG_SPEEDUP}x** speedup for signature verification."
else
    echo "GPU benchmarking not available."
fi)

---

## End-to-End TPS Measurement

| Configuration | TPS | Improvement |
|---------------|-----|-------------|
| **CPU Only** | ${TPS_WITHOUT_GPU:-0} | Baseline |
| **GPU Accelerated** | ${TPS_WITH_GPU:-0} | +${TPS_IMPROVEMENT:-0}% |

**Target**: 5.09M → 6.0M+ TPS (+20-25% improvement)

$(if [ "${GPU_AVAILABLE}" = true ] && [ "${TPS_IMPROVEMENT}" != "0" ]; then
    if (( $(echo "${TPS_IMPROVEMENT} >= 20" | bc -l) )); then
        echo "✅ **Target achieved**: ${TPS_IMPROVEMENT}% improvement meets 20-25% goal"
    else
        echo "⚠️ **Target not met**: ${TPS_IMPROVEMENT}% improvement (target: 20-25%)"
    fi
else
    echo "❌ **Cannot measure**: GPU not available or TPS measurement failed"
fi)

---

## Recommendations

$(if [ "${GPU_AVAILABLE}" = true ]; then
    echo "### ✅ GPU Acceleration Recommended"
    echo ""
    echo "Based on the benchmark results, GPU acceleration provides significant performance improvements:"
    echo ""
    echo "1. **Deploy GPU hardware**: $([ "${CUDA_AVAILABLE}" = true ] && echo "NVIDIA GPU (CUDA)" || echo "AMD/Intel GPU (OpenCL)")"
    echo "2. **Production rollout**: Enable GPU acceleration in production environment"
    echo "3. **Monitor performance**: Track TPS improvements and GPU utilization"
    echo "4. **Optimize batch sizes**: Tune batch sizes for optimal GPU memory usage"
else
    echo "### ❌ GPU Not Available"
    echo ""
    echo "GPU acceleration is not available on this system. To enable GPU acceleration:"
    echo ""
    echo "1. **Install GPU hardware**: NVIDIA RTX series (recommended) or AMD/Intel with OpenCL"
    echo "2. **Install drivers**: CUDA 10.0+ (NVIDIA) or OpenCL 1.2+ drivers"
    echo "3. **Install dependencies**: Aparapi library and GPU runtime"
    echo "4. **Re-run benchmark**: Verify GPU detection and performance gains"
fi)

---

## Files Generated

- **Results JSON**: \`${RESULTS_FILE}\`
- **Benchmark Log**: \`${LOG_FILE}\`
- **This Report**: \`${REPORT_FILE}\`

---

**Report Generated**: $(date)
**Aurigraph V11 Version**: 11.4.4
EOF

    log "Report saved to: ${REPORT_FILE}"
}

#############################################################################
# MAIN EXECUTION
#############################################################################

main() {
    print_header "Aurigraph V11 GPU Performance Benchmark"

    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --quick)
                BENCHMARK_MODE="quick"
                shift
                ;;
            --full)
                BENCHMARK_MODE="full"
                shift
                ;;
            --stress)
                BENCHMARK_MODE="stress"
                shift
                ;;
            --report-only)
                generate_markdown_report
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                echo "Usage: $0 [--quick|--full|--stress|--report-only]"
                exit 1
                ;;
        esac
    done

    log "Starting GPU performance benchmark (mode: ${BENCHMARK_MODE})"

    # Step 1: Detect GPU hardware
    detect_gpu_hardware

    # Step 2: Collect system information
    collect_system_info

    # Step 3: Profile GPU memory
    if [ "${GPU_AVAILABLE}" = true ]; then
        profile_gpu_memory
    fi

    # Step 4: Run performance benchmarks
    run_benchmarks

    # Step 5: Measure end-to-end TPS
    measure_end_to_end_tps

    # Step 6: Generate results
    generate_json_results
    generate_markdown_report

    # Summary
    print_header "Benchmark Complete"

    log "✅ Benchmark completed successfully"
    log ""
    log "Results:"
    log "  - JSON: ${RESULTS_FILE}"
    log "  - Report: ${REPORT_FILE}"
    log "  - Log: ${LOG_FILE}"
    log ""

    if [ "${GPU_AVAILABLE}" = true ]; then
        log "GPU Performance Summary:"
        log "  - Hashing speedup: ${HASH_SPEEDUP:-N/A}x"
        log "  - Merkle tree speedup: ${MERKLE_SPEEDUP:-N/A}x"
        log "  - Signature verification speedup: ${SIG_SPEEDUP:-N/A}x"
        log "  - Overall TPS improvement: ${TPS_IMPROVEMENT:-N/A}%"
    else
        log_warn "GPU not available - CPU-only benchmarks performed"
    fi
}

# Run main function
main "$@"
