#!/bin/bash

################################################################################
# deploy-remote.sh - Remote Deployment Script for Aurigraph V11
# Purpose: Deploy built Docker images to remote server (dlt.aurigraph.io)
# Usage: ./deploy-remote.sh [validator|business|integration|all] [dev|staging|prod|all]
################################################################################

set -e

# Configuration
REMOTE_USER="${REMOTE_USER:-subbu}"
REMOTE_HOST="${REMOTE_HOST:-dlt.aurigraph.io}"
REMOTE_PORT="${REMOTE_PORT:-22}"
REMOTE_DIR="${REMOTE_DIR:-~/Aurigraph-DLT/aurigraph-av10-7/aurigraph-v11-standalone}"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-ghcr.io}"
VERSION="11.0.0"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

check_ssh_connection() {
    log_info "Checking SSH connection to ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}..."

    if ssh -q -p ${REMOTE_PORT} ${REMOTE_USER}@${REMOTE_HOST} "echo 'SSH connection successful'" > /dev/null 2>&1; then
        log_success "SSH connection established"
        return 0
    else
        log_error "SSH connection failed. Ensure SSH keys are configured."
        return 1
    fi
}

deploy_node() {
    local node_type=$1
    local build_variant=$2

    log_info "Deploying ${node_type} (${build_variant} variant)..."

    local image_name="aurigraph-${node_type}:${VERSION}-${build_variant}"
    local registry_image="${DOCKER_REGISTRY}/$(echo ${GITHUB_REPOSITORY:-Aurigraph-DLT-Corp/Aurigraph-DLT} | tr '[:upper:]' '[:lower:]')/aurigraph-${node_type}:${build_variant}"

    ssh -p ${REMOTE_PORT} ${REMOTE_USER}@${REMOTE_HOST} << DEPLOY_CMD
cd ${REMOTE_DIR}

echo "========== Deploying ${node_type} (${build_variant}) =========="
echo "Timestamp: \$(date)"
echo ""

# Pull image
echo "[STEP 1] Pulling Docker image..."
docker pull ${registry_image} || {
    echo "Failed to pull image: ${registry_image}"
    exit 1
}

# Tag image
echo "[STEP 2] Tagging image locally..."
docker tag ${registry_image} ${image_name}

# Stop existing container if running
echo "[STEP 3] Stopping existing containers..."
docker-compose -f docker-compose.prod.yml down --remove-orphans 2>/dev/null || true

# Start new container
echo "[STEP 4] Starting new container..."
docker-compose -f docker-compose.prod.yml up -d

# Wait for health checks
echo "[STEP 5] Waiting for health checks..."
sleep 15

# Verify deployment
echo "[STEP 6] Verifying deployment..."
HEALTH_CHECK=false
for i in {1..10}; do
    if curl -s -f http://localhost:8080/q/health/ready > /dev/null 2>&1 || curl -s -f http://localhost:9003/q/health/ready > /dev/null 2>&1; then
        HEALTH_CHECK=true
        break
    fi
    echo "  Attempt \$i/10: Health check pending..."
    sleep 3
done

if [ "\$HEALTH_CHECK" = true ]; then
    echo "✓ Deployment successful - health checks passed"
else
    echo "⚠ Health check failed - please investigate"
    exit 1
fi

echo ""
echo "========== ${node_type} (${build_variant}) Deployment Complete =========="
DEPLOY_CMD

    if [ $? -eq 0 ]; then
        log_success "Deployed ${node_type} (${build_variant}) successfully"
        return 0
    else
        log_error "Failed to deploy ${node_type} (${build_variant})"
        return 1
    fi
}

deploy_all_variants() {
    local node_type=$1
    local build_variants=(dev staging prod)
    local failed=0

    log_info "==========================================="
    log_info "Deploying all variants for ${node_type} nodes"
    log_info "==========================================="

    for build_variant in "${build_variants[@]}"; do
        if ! deploy_node "${node_type}" "${build_variant}"; then
            ((failed++))
        fi
        echo ""
    done

    return $failed
}

# Main execution
main() {
    local node_type="${1:-all}"
    local build_variant="${2:-prod}"

    log_info "Starting Aurigraph V11 Remote Deployment"
    log_info "Target: ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
    log_info "Remote Directory: ${REMOTE_DIR}"
    log_info "Node Type: ${node_type}"
    log_info "Build Variant: ${build_variant}"
    echo ""

    # Check SSH connection
    if ! check_ssh_connection; then
        exit 1
    fi

    echo ""
    local total_failed=0

    case "${node_type}" in
        validator|business|integration)
            if [ "${build_variant}" = "all" ]; then
                deploy_all_variants "${node_type}" || ((total_failed++))
            else
                deploy_node "${node_type}" "${build_variant}" || ((total_failed++))
            fi
            ;;
        all)
            for nt in validator business integration; do
                if [ "${build_variant}" = "all" ]; then
                    deploy_all_variants "${nt}" || ((total_failed++))
                else
                    deploy_node "${nt}" "${build_variant}" || ((total_failed++))
                fi
                echo ""
            done
            ;;
        *)
            log_error "Invalid node type: ${node_type}"
            echo "Usage: $0 [validator|business|integration|all] [dev|staging|prod|all]"
            exit 1
            ;;
    esac

    echo ""
    log_info "==========================================="
    if [ ${total_failed} -eq 0 ]; then
        log_success "All deployments completed successfully!"
        log_info "Aurigraph V11 nodes are now running on ${REMOTE_HOST}"
        exit 0
    else
        log_error "Some deployments failed (${total_failed} errors)"
        exit 1
    fi
}

# Run main
main "$@"

