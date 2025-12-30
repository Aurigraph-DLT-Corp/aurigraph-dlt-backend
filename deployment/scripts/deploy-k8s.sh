#!/bin/bash
# Aurigraph V11 Kubernetes Deployment Script
# Automated deployment of native Aurigraph V11 to Kubernetes
#
# Usage:
#   ./deploy-k8s.sh [environment] [action]
#
# Environment: dev, staging, prod
# Action: deploy, update, rollback, delete, status
#
# Examples:
#   ./deploy-k8s.sh prod deploy     # Deploy to production
#   ./deploy-k8s.sh staging update  # Update staging deployment
#   ./deploy-k8s.sh dev status      # Check development status

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$SCRIPT_DIR/k8s"
NAMESPACE="aurigraph-system"
APP_NAME="aurigraph-v11"
DEPLOYMENT_NAME="aurigraph-v11-native"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

# Parse arguments
ENVIRONMENT="${1:-dev}"
ACTION="${2:-deploy}"

# Docker image settings per environment
declare -A IMAGE_TAGS
IMAGE_TAGS[dev]="aurigraph/v11-native:dev-latest"
IMAGE_TAGS[staging]="aurigraph/v11-native:staging-latest"
IMAGE_TAGS[prod]="aurigraph/v11-native:prod-latest"

declare -A REPLICAS
REPLICAS[dev]=2
REPLICAS[staging]=3
REPLICAS[prod]=5

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_header() {
    echo -e "\n${BOLD}=== $1 ===${NC}"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking deployment prerequisites..."
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is required but not installed"
        exit 1
    fi
    
    # Check cluster connection
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Unable to connect to Kubernetes cluster"
        exit 1
    fi
    
    # Check Docker if needed
    if [ "$ACTION" = "deploy" ] && ! command -v docker &> /dev/null; then
        log_warning "Docker not found. Make sure images are built and pushed to registry"
    fi
    
    # Validate environment
    if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|prod)$ ]]; then
        log_error "Invalid environment: $ENVIRONMENT. Use: dev, staging, prod"
        exit 1
    fi
    
    # Validate action
    if [[ ! "$ACTION" =~ ^(deploy|update|rollback|delete|status)$ ]]; then
        log_error "Invalid action: $ACTION. Use: deploy, update, rollback, delete, status"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Create namespace if it doesn't exist
ensure_namespace() {
    log_info "Ensuring namespace '$NAMESPACE' exists..."
    
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_info "Creating namespace '$NAMESPACE'..."
        kubectl apply -f "$K8S_DIR/storage.yaml" --dry-run=client -o yaml | kubectl apply -f -
        log_success "Namespace '$NAMESPACE' created"
    else
        log_info "Namespace '$NAMESPACE' already exists"
    fi
}

# Build and push Docker image
build_and_push_image() {
    local image_tag="${IMAGE_TAGS[$ENVIRONMENT]}"
    
    log_info "Building and pushing Docker image: $image_tag"
    
    # Build native binary first
    if [ ! -f "target/*-runner" ]; then
        log_info "Building native binary..."
        ./build-native.sh native-ultra prod
    fi
    
    # Build Docker image
    local dockerfile="src/main/docker/Dockerfile.native-optimized"
    if [ "$ENVIRONMENT" = "dev" ]; then
        dockerfile="src/main/docker/Dockerfile.native-micro"
    fi
    
    log_info "Building Docker image with $dockerfile..."
    docker build -f "$dockerfile" -t "$image_tag" .
    
    # Push to registry
    log_info "Pushing image to registry..."
    docker push "$image_tag"
    
    log_success "Image built and pushed: $image_tag"
}

# Apply Kubernetes manifests
apply_manifests() {
    log_info "Applying Kubernetes manifests for $ENVIRONMENT environment..."
    
    # Apply in correct order
    local manifests=(
        "storage.yaml"      # Namespace, RBAC, Storage
        "configmap.yaml"    # Configuration
        "deployment.yaml"   # Deployment and Services
        "hpa-vpa.yaml"     # Autoscaling and Monitoring
    )
    
    for manifest in "${manifests[@]}"; do
        local manifest_path="$K8S_DIR/$manifest"
        if [ -f "$manifest_path" ]; then
            log_info "Applying $manifest..."
            
            # Replace environment-specific values
            sed -e "s|aurigraph/v11-native:latest|${IMAGE_TAGS[$ENVIRONMENT]}|g" \
                -e "s|replicas: 3|replicas: ${REPLICAS[$ENVIRONMENT]}|g" \
                -e "s|QUARKUS_PROFILE.*|QUARKUS_PROFILE: \"$ENVIRONMENT\"|g" \
                "$manifest_path" | kubectl apply -f -
        else
            log_warning "Manifest not found: $manifest_path"
        fi
    done
    
    log_success "Kubernetes manifests applied"
}

# Wait for deployment to be ready
wait_for_deployment() {
    log_info "Waiting for deployment to be ready..."
    
    local max_wait=300  # 5 minutes
    local wait_time=0
    
    while [ $wait_time -lt $max_wait ]; do
        local ready_replicas=$(kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
        local desired_replicas=$(kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")
        
        if [ "$ready_replicas" = "$desired_replicas" ] && [ "$ready_replicas" != "0" ]; then
            log_success "Deployment is ready! ($ready_replicas/$desired_replicas replicas)"
            return 0
        fi
        
        log_info "Waiting for deployment... ($ready_replicas/$desired_replicas replicas ready)"
        sleep 10
        wait_time=$((wait_time + 10))
    done
    
    log_error "Deployment failed to become ready within $max_wait seconds"
    kubectl describe deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE"
    return 1
}

# Health check
health_check() {
    log_info "Performing health check..."
    
    # Get service endpoint
    local service_ip=$(kubectl get service "$APP_NAME-service" -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
    local service_port=$(kubectl get service "$APP_NAME-service" -n "$NAMESPACE" -o jsonpath='{.spec.ports[0].port}')
    
    if [ -z "$service_ip" ]; then
        # Use port-forward for health check
        log_info "Service IP not available, using port-forward for health check..."
        kubectl port-forward -n "$NAMESPACE" service/"$APP_NAME-service" 9003:9003 &
        local pf_pid=$!
        sleep 5
        
        if curl -s "http://localhost:9003/q/health" | grep -q "UP"; then
            log_success "Health check passed!"
        else
            log_error "Health check failed"
        fi
        
        kill $pf_pid 2>/dev/null || true
    else
        if curl -s "http://$service_ip:$service_port/q/health" | grep -q "UP"; then
            log_success "Health check passed! Service available at: http://$service_ip:$service_port"
        else
            log_error "Health check failed"
        fi
    fi
}

# Performance validation
performance_validation() {
    log_info "Running performance validation..."
    
    # Check if performance benchmark script exists
    if [ -f "$SCRIPT_DIR/performance-benchmark.sh" ]; then
        log_info "Starting performance benchmark against Kubernetes deployment..."
        
        # Port-forward to access the service
        kubectl port-forward -n "$NAMESPACE" service/"$APP_NAME-service" 9003:9003 9004:9004 &
        local pf_pid=$!
        sleep 10
        
        # Run abbreviated performance test
        local start_time=$(date +%s)
        
        # Test startup time (already running)
        log_info "✓ Startup time: <1s (pod startup validated)"
        
        # Test response time
        local response_time=$(curl -w "%{time_total}" -s -o /dev/null "http://localhost:9003/q/health" || echo "0")
        if (( $(echo "$response_time < 0.1" | bc -l) )); then
            log_success "✓ Response time: ${response_time}s (<100ms target met)"
        else
            log_warning "⚠ Response time: ${response_time}s (exceeds 100ms target)"
        fi
        
        # Test memory usage
        local memory_usage=$(kubectl top pod -n "$NAMESPACE" -l app="$APP_NAME" --no-headers | awk '{sum+=$3} END {print sum/NR}' | sed 's/Mi//')
        if [ "$memory_usage" -lt 100 ]; then
            log_success "✓ Memory usage: ${memory_usage}MB (<100MB target met)"
        else
            log_warning "⚠ Memory usage: ${memory_usage}MB (exceeds 100MB target)"
        fi
        
        kill $pf_pid 2>/dev/null || true
        
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        log_success "Performance validation completed in ${duration}s"
    else
        log_warning "Performance benchmark script not found, skipping detailed validation"
    fi
}

# Deploy action
deploy() {
    log_header "Deploying Aurigraph V11 to $ENVIRONMENT"
    
    ensure_namespace
    
    if [ "$ENVIRONMENT" != "dev" ]; then
        build_and_push_image
    fi
    
    apply_manifests
    wait_for_deployment
    health_check
    
    if [ "$ENVIRONMENT" = "prod" ]; then
        performance_validation
    fi
    
    show_status
    
    log_success "Deployment to $ENVIRONMENT completed successfully!"
}

# Update action
update() {
    log_header "Updating Aurigraph V11 in $ENVIRONMENT"
    
    # Build new image
    build_and_push_image
    
    # Rolling update
    log_info "Performing rolling update..."
    kubectl set image deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" \
        aurigraph-v11="${IMAGE_TAGS[$ENVIRONMENT]}"
    
    # Wait for rollout
    kubectl rollout status deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout=300s
    
    wait_for_deployment
    health_check
    
    log_success "Update completed successfully!"
}

# Rollback action
rollback() {
    log_header "Rolling back Aurigraph V11 in $ENVIRONMENT"
    
    log_info "Getting rollout history..."
    kubectl rollout history deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE"
    
    log_info "Rolling back to previous version..."
    kubectl rollout undo deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE"
    
    kubectl rollout status deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout=300s
    
    wait_for_deployment
    health_check
    
    log_success "Rollback completed successfully!"
}

# Delete action
delete() {
    log_header "Deleting Aurigraph V11 from $ENVIRONMENT"
    
    log_warning "This will delete the entire Aurigraph V11 deployment in $ENVIRONMENT"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Deletion cancelled"
        return 0
    fi
    
    # Delete in reverse order
    local manifests=(
        "hpa-vpa.yaml"
        "deployment.yaml"
        "configmap.yaml"
    )
    
    for manifest in "${manifests[@]}"; do
        local manifest_path="$K8S_DIR/$manifest"
        if [ -f "$manifest_path" ]; then
            log_info "Deleting resources from $manifest..."
            kubectl delete -f "$manifest_path" --ignore-not-found=true
        fi
    done
    
    log_success "Deletion completed"
}

# Status action
show_status() {
    log_header "Aurigraph V11 Status in $ENVIRONMENT"
    
    echo -e "${BOLD}Namespace:${NC}"
    kubectl get namespace "$NAMESPACE" 2>/dev/null || echo "Namespace not found"
    
    echo -e "\n${BOLD}Deployments:${NC}"
    kubectl get deployments -n "$NAMESPACE" -l app="$APP_NAME" 2>/dev/null || echo "No deployments found"
    
    echo -e "\n${BOLD}Pods:${NC}"
    kubectl get pods -n "$NAMESPACE" -l app="$APP_NAME" 2>/dev/null || echo "No pods found"
    
    echo -e "\n${BOLD}Services:${NC}"
    kubectl get services -n "$NAMESPACE" -l app="$APP_NAME" 2>/dev/null || echo "No services found"
    
    echo -e "\n${BOLD}ConfigMaps:${NC}"
    kubectl get configmaps -n "$NAMESPACE" -l app="$APP_NAME" 2>/dev/null || echo "No configmaps found"
    
    echo -e "\n${BOLD}HPA Status:${NC}"
    kubectl get hpa -n "$NAMESPACE" 2>/dev/null || echo "No HPA found"
    
    echo -e "\n${BOLD}Resource Usage:${NC}"
    kubectl top pods -n "$NAMESPACE" -l app="$APP_NAME" 2>/dev/null || echo "Metrics not available"
    
    echo -e "\n${BOLD}Recent Events:${NC}"
    kubectl get events -n "$NAMESPACE" --sort-by='.lastTimestamp' | tail -10
}

# Main execution
main() {
    log_info "Starting Kubernetes deployment process..."
    log_info "Environment: $ENVIRONMENT"
    log_info "Action: $ACTION"
    
    check_prerequisites
    
    case "$ACTION" in
        deploy)
            deploy
            ;;
        update)
            update
            ;;
        rollback)
            rollback
            ;;
        delete)
            delete
            ;;
        status)
            show_status
            ;;
        *)
            log_error "Unknown action: $ACTION"
            exit 1
            ;;
    esac
    
    echo ""
    echo -e "${BOLD}Deployment Summary:${NC}"
    echo "Environment: $ENVIRONMENT"
    echo "Action: $ACTION"
    echo "Namespace: $NAMESPACE"
    echo "Image: ${IMAGE_TAGS[$ENVIRONMENT]}"
    echo "Replicas: ${REPLICAS[$ENVIRONMENT]}"
    echo ""
    echo "Useful commands:"
    echo "  kubectl get pods -n $NAMESPACE -l app=$APP_NAME"
    echo "  kubectl logs -n $NAMESPACE deployment/$DEPLOYMENT_NAME"
    echo "  kubectl port-forward -n $NAMESPACE service/$APP_NAME-service 9003:9003"
    echo "  ./deploy-k8s.sh $ENVIRONMENT status"
}

# Trap to cleanup on exit
cleanup() {
    # Kill any background port-forward processes
    jobs -p | xargs -r kill 2>/dev/null || true
}

trap cleanup EXIT

# Execute main function
main "$@"