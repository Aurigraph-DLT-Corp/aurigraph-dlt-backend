#!/bin/bash

################################################################################
# build-node-releases.sh - Comprehensive Node Build and Release Script
# Purpose: Build multiple variants of validator, business, and integration nodes
# Usage: ./build-node-releases.sh [validator|business|integration|all] [dev|staging|prod|all]
# Example: ./build-node-releases.sh all all
################################################################################

set -e

# Configuration
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
VERSION="11.0.0"
BUILD_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REGISTRY="${DOCKER_REGISTRY:-}"
LOG_DIR="$SCRIPT_DIR/build-logs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Create log directory
mkdir -p "$LOG_DIR"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

build_image() {
    local node_type=$1
    local build_type=$2
    local dockerfile="Dockerfile.${node_type}-${build_type}"
    local image_name="aurigraph-${node_type}:${VERSION}-${build_type}"
    local log_file="$LOG_DIR/${node_type}-${build_type}-${BUILD_TIMESTAMP}.log"

    log_info "Building $node_type ($build_type variant)..."

    if [ ! -f "$SCRIPT_DIR/$dockerfile" ]; then
        log_error "Dockerfile not found: $dockerfile"
        return 1
    fi

    if docker build \
        -f "$SCRIPT_DIR/$dockerfile" \
        -t "$image_name" \
        --build-arg VERSION="$VERSION" \
        --build-arg BUILD_TYPE="$build_type" \
        "$SCRIPT_DIR" > "$log_file" 2>&1; then
        log_success "Built $image_name"

        # Log image info
        docker inspect "$image_name" | grep -E '"Id"|"RepoTags"|"Size"' >> "$log_file"
        return 0
    else
        log_error "Failed to build $image_name (see $log_file)"
        tail -20 "$log_file"
        return 1
    fi
}

push_image() {
    local node_type=$1
    local build_type=$2
    local image_name="aurigraph-${node_type}:${VERSION}-${build_type}"

    if [ -z "$REGISTRY" ]; then
        log_warn "DOCKER_REGISTRY not set, skipping push for $image_name"
        return 0
    fi

    local remote_image="${REGISTRY}/${image_name}"
    log_info "Pushing $remote_image..."

    if docker tag "$image_name" "$remote_image" && docker push "$remote_image"; then
        log_success "Pushed $remote_image"
        return 0
    else
        log_error "Failed to push $remote_image"
        return 1
    fi
}

test_image() {
    local node_type=$1
    local build_type=$2
    local image_name="aurigraph-${node_type}:${VERSION}-${build_type}"

    log_info "Testing $image_name..."

    # Run basic health check
    if docker run --rm --entrypoint="/bin/sh" "$image_name" -c "[ -f /app/aurigraph-v11 ]"; then
        log_success "Image validation passed for $image_name"
        return 0
    else
        log_error "Image validation failed for $image_name"
        return 1
    fi
}

build_all_variants() {
    local node_type=$1
    local build_types=("dev" "staging" "prod")
    local failed=0

    log_info "=========================================="
    log_info "Building all variants for $node_type nodes"
    log_info "=========================================="

    for build_type in "${build_types[@]}"; do
        if ! build_image "$node_type" "$build_type"; then
            ((failed++))
        else
            if [ "$ENABLE_TESTING" = "true" ]; then
                test_image "$node_type" "$build_type"
            fi

            if [ -n "$REGISTRY" ]; then
                push_image "$node_type" "$build_type"
            fi
        fi
        echo ""
    done

    return $failed
}

# Main execution
main() {
    local node_type="${1:-all}"
    local build_type="${2:-all}"

    log_info "Starting Aurigraph Node Release Build"
    log_info "Version: $VERSION"
    log_info "Timestamp: $(date)"
    log_info "Log Directory: $LOG_DIR"
    echo ""

    local total_failed=0

    case "$node_type" in
        validator|business|integration)
            if [ "$build_type" = "all" ]; then
                build_all_variants "$node_type" || ((total_failed++))
            else
                build_image "$node_type" "$build_type" || ((total_failed++))
            fi
            ;;
        all)
            for nt in validator business integration; do
                build_all_variants "$nt" || ((total_failed++))
            done
            ;;
        *)
            log_error "Invalid node type: $node_type"
            echo "Usage: $0 [validator|business|integration|all] [dev|staging|prod|all]"
            exit 1
            ;;
    esac

    echo ""
    log_info "=========================================="
    if [ $total_failed -eq 0 ]; then
        log_success "All builds completed successfully!"
        log_info "Release builds ready for deployment"
        exit 0
    else
        log_error "Some builds failed ($total_failed errors)"
        exit 1
    fi
}

# Run main
main "$@"
