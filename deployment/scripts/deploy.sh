#!/bin/bash

# Aurigraph V11 Production Deployment Script
# Automates the complete deployment process with comprehensive validation

set -euo pipefail

# ================================
# Configuration and Variables
# ================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_NAME="aurigraph-v11"
NAMESPACE="aurigraph-system"
BUILD_TYPE="${BUILD_TYPE:-native}"
ENVIRONMENT="${ENVIRONMENT:-production}"
REGISTRY="${REGISTRY:-aurigraph}"
VERSION="${VERSION:-11.0.0}"
DEPLOYMENT_TIMEOUT="${DEPLOYMENT_TIMEOUT:-600}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ================================
# Logging Functions
# ================================

log() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

log_success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

log_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

log_info() {
    echo -e "${BLUE}[INFO] $1${NC}"
}

# ================================
# Utility Functions
# ================================

check_prerequisites() {
    log "Checking deployment prerequisites..."
    
    local missing_tools=()
    
    # Check required tools
    command -v docker >/dev/null 2>&1 || missing_tools+=("docker")
    command -v kubectl >/dev/null 2>&1 || missing_tools+=("kubectl")
    command -v helm >/dev/null 2>&1 || missing_tools+=("helm")
    command -v jq >/dev/null 2>&1 || missing_tools+=("jq")
    
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_error "Please install missing tools and retry"
        exit 1
    fi
    
    # Check Kubernetes connection
    if ! kubectl cluster-info >/dev/null 2>&1; then
        log_error "Cannot connect to Kubernetes cluster"
        log_error "Please check your kubectl configuration"
        exit 1
    fi
    
    # Check Docker daemon
    if ! docker info >/dev/null 2>&1; then
        log_error "Cannot connect to Docker daemon"
        log_error "Please start Docker and retry"
        exit 1
    fi
    
    log_success "All prerequisites met"
}

validate_environment() {
    log "Validating deployment environment..."
    
    # Check if namespace exists, create if not
    if ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
        log_info "Creating namespace: $NAMESPACE"
        kubectl create namespace "$NAMESPACE"
        kubectl label namespace "$NAMESPACE" name="$NAMESPACE"
    fi
    
    # Validate Kubernetes version
    local k8s_version
    k8s_version=$(kubectl version --output=json | jq -r '.serverVersion.gitVersion')
    log_info "Kubernetes version: $k8s_version"
    
    # Check available resources
    local nodes_ready
    nodes_ready=$(kubectl get nodes --no-headers | grep -c "Ready" || echo "0")
    log_info "Available nodes: $nodes_ready"
    
    if [[ $nodes_ready -eq 0 ]]; then
        log_error "No ready nodes found in cluster"
        exit 1
    fi
    
    log_success "Environment validation complete"
}

# ================================
# Build Functions
# ================================

build_application() {
    log "Building Aurigraph V11 application..."
    
    cd "$PROJECT_DIR"
    
    # Determine build profile based on BUILD_TYPE
    local build_profile
    case "$BUILD_TYPE" in
        "native")
            build_profile="native-ultra"
            ;;
        "native-fast")
            build_profile="native-fast"
            ;;
        "jvm")
            build_profile="jvm"
            ;;
        *)
            log_error "Invalid BUILD_TYPE: $BUILD_TYPE. Use 'native', 'native-fast', or 'jvm'"
            exit 1
            ;;
    esac
    
    log_info "Building with profile: $build_profile"
    
    # Clean previous builds
    ./mvnw clean -q
    
    # Build application
    if [[ "$BUILD_TYPE" == "jvm" ]]; then
        ./mvnw package -Dquarkus.package.jar.type=uber-jar -Dmaven.test.skip=true -q
    else
        ./mvnw package -P"$build_profile" -Dmaven.test.skip=true -q
    fi
    
    log_success "Application build complete"
}

build_docker_image() {
    log "Building Docker image..."
    
    cd "$PROJECT_DIR"
    
    local image_tag="$REGISTRY/$PROJECT_NAME-$BUILD_TYPE:$VERSION"
    local latest_tag="$REGISTRY/$PROJECT_NAME-$BUILD_TYPE:latest"
    
    # Build Docker image
    docker build \
        --build-arg BUILD_TYPE="$BUILD_TYPE" \
        --build-arg JAVA_VERSION=21 \
        --target "${BUILD_TYPE}-runtime" \
        -t "$image_tag" \
        -t "$latest_tag" \
        .
    
    log_success "Docker image built: $image_tag"
    
    # Push to registry if configured
    if [[ -n "${PUSH_TO_REGISTRY:-}" && "$PUSH_TO_REGISTRY" == "true" ]]; then
        log "Pushing image to registry..."
        docker push "$image_tag"
        docker push "$latest_tag"
        log_success "Image pushed to registry"
    fi
}

# ================================
# Deployment Functions
# ================================

deploy_secrets() {
    log "Deploying secrets and configuration..."
    
    # Create TLS secret if certificates exist
    if [[ -f "$PROJECT_DIR/config/ssl/tls.crt" && -f "$PROJECT_DIR/config/ssl/tls.key" ]]; then
        kubectl create secret tls aurigraph-v11-tls-cert \
            --cert="$PROJECT_DIR/config/ssl/tls.crt" \
            --key="$PROJECT_DIR/config/ssl/tls.key" \
            --namespace="$NAMESPACE" \
            --dry-run=client -o yaml | kubectl apply -f -
        log_info "TLS certificate deployed"
    fi
    
    # Create application secrets
    if [[ -f "$PROJECT_DIR/config/secrets.env" ]]; then
        kubectl create secret generic aurigraph-v11-secrets \
            --from-env-file="$PROJECT_DIR/config/secrets.env" \
            --namespace="$NAMESPACE" \
            --dry-run=client -o yaml | kubectl apply -f -
        log_info "Application secrets deployed"
    fi
    
    # Create registry secret if configured
    if [[ -n "${REGISTRY_USERNAME:-}" && -n "${REGISTRY_PASSWORD:-}" ]]; then
        kubectl create secret docker-registry aurigraph-registry-secret \
            --docker-server="$REGISTRY" \
            --docker-username="$REGISTRY_USERNAME" \
            --docker-password="$REGISTRY_PASSWORD" \
            --namespace="$NAMESPACE" \
            --dry-run=client -o yaml | kubectl apply -f -
        log_info "Registry credentials deployed"
    fi
    
    log_success "Secrets deployment complete"
}

deploy_configmaps() {
    log "Deploying configuration maps..."
    
    # Apply existing configmap
    if [[ -f "$PROJECT_DIR/k8s/configmap.yaml" ]]; then
        kubectl apply -f "$PROJECT_DIR/k8s/configmap.yaml" -n "$NAMESPACE"
        log_info "Configuration map deployed"
    fi
    
    # Create additional configmaps from config files
    if [[ -d "$PROJECT_DIR/config" ]]; then
        find "$PROJECT_DIR/config" -name "*.properties" -o -name "*.yml" -o -name "*.yaml" | while read -r config_file; do
            local config_name
            config_name="aurigraph-v11-$(basename "$config_file" | sed 's/\.[^.]*$//' | tr '.' '-')"
            
            kubectl create configmap "$config_name" \
                --from-file="$config_file" \
                --namespace="$NAMESPACE" \
                --dry-run=client -o yaml | kubectl apply -f -
        done
    fi
    
    log_success "Configuration deployment complete"
}

deploy_storage() {
    log "Deploying storage components..."
    
    if [[ -f "$PROJECT_DIR/k8s/storage.yaml" ]]; then
        kubectl apply -f "$PROJECT_DIR/k8s/storage.yaml" -n "$NAMESPACE"
        log_info "Storage components deployed"
    fi
    
    log_success "Storage deployment complete"
}

deploy_application() {
    log "Deploying Aurigraph V11 application..."
    
    cd "$PROJECT_DIR"
    
    # Update deployment image
    local image_tag="$REGISTRY/$PROJECT_NAME-$BUILD_TYPE:$VERSION"
    
    # Apply deployment with image update
    kubectl patch deployment aurigraph-v11-native \
        -n "$NAMESPACE" \
        -p "{\"spec\":{\"template\":{\"spec\":{\"containers\":[{\"name\":\"aurigraph-v11\",\"image\":\"$image_tag\"}]}}}}" \
        --dry-run=client -o yaml | kubectl apply -f -
    
    # Apply all Kubernetes manifests
    kubectl apply -f k8s/ -n "$NAMESPACE"
    
    log_success "Application deployment initiated"
}

deploy_services() {
    log "Deploying services and ingress..."
    
    # Apply service definitions
    kubectl apply -f "$PROJECT_DIR/k8s/service.yaml" -n "$NAMESPACE"
    
    # Apply HPA/VPA if exists
    if [[ -f "$PROJECT_DIR/k8s/hpa-vpa.yaml" ]]; then
        kubectl apply -f "$PROJECT_DIR/k8s/hpa-vpa.yaml" -n "$NAMESPACE"
        log_info "Auto-scaling policies deployed"
    fi
    
    log_success "Services deployment complete"
}

# ================================
# Validation Functions
# ================================

wait_for_deployment() {
    log "Waiting for deployment to be ready..."
    
    local deployment_name="aurigraph-v11-native"
    
    if ! kubectl rollout status deployment/"$deployment_name" -n "$NAMESPACE" --timeout="${DEPLOYMENT_TIMEOUT}s"; then
        log_error "Deployment failed to become ready within timeout"
        
        # Get deployment details for debugging
        log_error "Deployment details:"
        kubectl describe deployment "$deployment_name" -n "$NAMESPACE"
        
        # Get pod logs for debugging
        log_error "Pod logs:"
        kubectl logs -l app=aurigraph-v11 -n "$NAMESPACE" --tail=50
        
        exit 1
    fi
    
    log_success "Deployment is ready"
}

validate_deployment() {
    log "Validating deployment health..."
    
    # Check pod status
    local pods_ready
    pods_ready=$(kubectl get pods -l app=aurigraph-v11 -n "$NAMESPACE" --no-headers | grep -c "Running" || echo "0")
    
    if [[ $pods_ready -eq 0 ]]; then
        log_error "No running pods found"
        exit 1
    fi
    
    log_info "Running pods: $pods_ready"
    
    # Check service endpoints
    local service_endpoints
    service_endpoints=$(kubectl get endpoints aurigraph-v11-service -n "$NAMESPACE" -o json | jq -r '.subsets[0].addresses | length // 0')
    
    if [[ $service_endpoints -eq 0 ]]; then
        log_error "No service endpoints available"
        exit 1
    fi
    
    log_info "Service endpoints: $service_endpoints"
    
    # Test health endpoint
    log "Testing health endpoint..."
    
    # Port-forward for testing
    kubectl port-forward -n "$NAMESPACE" svc/aurigraph-v11-service 9003:9003 >/dev/null 2>&1 &
    local port_forward_pid=$!
    
    # Wait for port-forward to be ready
    sleep 5
    
    # Test health endpoint
    if curl -f http://localhost:9003/q/health/ready >/dev/null 2>&1; then
        log_success "Health endpoint accessible"
    else
        log_error "Health endpoint not accessible"
        kill $port_forward_pid >/dev/null 2>&1 || true
        exit 1
    fi
    
    # Clean up port-forward
    kill $port_forward_pid >/dev/null 2>&1 || true
    
    log_success "Deployment validation complete"
}

run_smoke_tests() {
    log "Running smoke tests..."
    
    # Port-forward for testing
    kubectl port-forward -n "$NAMESPACE" svc/aurigraph-v11-service 9003:9003 >/dev/null 2>&1 &
    local port_forward_pid=$!
    
    # Wait for port-forward
    sleep 5
    
    local test_passed=true
    
    # Test basic endpoints
    local endpoints=(
        "/q/health/live"
        "/q/health/ready"
        "/q/metrics"
        "/api/v11/info"
    )
    
    for endpoint in "${endpoints[@]}"; do
        log_info "Testing endpoint: $endpoint"
        
        if curl -f "http://localhost:9003$endpoint" >/dev/null 2>&1; then
            log_success "✓ $endpoint"
        else
            log_error "✗ $endpoint"
            test_passed=false
        fi
    done
    
    # Clean up port-forward
    kill $port_forward_pid >/dev/null 2>&1 || true
    
    if [[ "$test_passed" == "true" ]]; then
        log_success "All smoke tests passed"
    else
        log_error "Some smoke tests failed"
        exit 1
    fi
}

# ================================
# Monitoring and Cleanup
# ================================

deploy_monitoring() {
    log "Setting up monitoring..."
    
    # Apply monitoring configurations if they exist
    if [[ -f "$PROJECT_DIR/config/prometheus/prometheus.yml" ]]; then
        kubectl create configmap prometheus-config \
            --from-file="$PROJECT_DIR/config/prometheus/prometheus.yml" \
            --namespace="$NAMESPACE" \
            --dry-run=client -o yaml | kubectl apply -f -
        log_info "Prometheus configuration deployed"
    fi
    
    if [[ -f "$PROJECT_DIR/config/grafana/dashboards/" ]]; then
        kubectl create configmap grafana-dashboards \
            --from-file="$PROJECT_DIR/config/grafana/dashboards/" \
            --namespace="$NAMESPACE" \
            --dry-run=client -o yaml | kubectl apply -f -
        log_info "Grafana dashboards deployed"
    fi
    
    log_success "Monitoring setup complete"
}

cleanup_old_resources() {
    log "Cleaning up old resources..."
    
    # Remove old replica sets
    kubectl get rs -n "$NAMESPACE" -o json | \
        jq -r '.items[] | select(.spec.replicas == 0) | .metadata.name' | \
        xargs -r kubectl delete rs -n "$NAMESPACE" || true
    
    # Remove completed jobs older than 24 hours
    kubectl get jobs -n "$NAMESPACE" --field-selector=status.successful=1 -o json | \
        jq -r '.items[] | select(.status.completionTime < (now - 86400 | strftime("%Y-%m-%dT%H:%M:%SZ"))) | .metadata.name' | \
        xargs -r kubectl delete jobs -n "$NAMESPACE" || true
    
    log_info "Old resources cleaned up"
}

# ================================
# Main Deployment Flow
# ================================

show_usage() {
    cat << EOF
Aurigraph V11 Production Deployment Script

Usage: $0 [OPTIONS]

Options:
    -t, --type TYPE         Build type: native, native-fast, jvm (default: native)
    -e, --env ENV          Environment: development, staging, production (default: production)
    -n, --namespace NAME   Kubernetes namespace (default: aurigraph-system)
    -r, --registry URL     Docker registry URL (default: aurigraph)
    -v, --version VERSION  Application version (default: 11.0.0)
    -p, --push             Push images to registry
    -s, --skip-build       Skip application and Docker build
    -m, --monitoring       Deploy monitoring stack
    -h, --help             Show this help message

Environment Variables:
    REGISTRY_USERNAME      Docker registry username
    REGISTRY_PASSWORD      Docker registry password
    DEPLOYMENT_TIMEOUT     Deployment timeout in seconds (default: 600)

Examples:
    $0                                    # Deploy with defaults
    $0 -t jvm -e staging                  # Deploy JVM build to staging
    $0 -t native-fast -p -m               # Fast native build with registry push and monitoring
    $0 --skip-build                       # Deploy without rebuilding

EOF
}

main() {
    local skip_build=false
    local deploy_monitoring_stack=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -t|--type)
                BUILD_TYPE="$2"
                shift 2
                ;;
            -e|--env)
                ENVIRONMENT="$2"
                shift 2
                ;;
            -n|--namespace)
                NAMESPACE="$2"
                shift 2
                ;;
            -r|--registry)
                REGISTRY="$2"
                shift 2
                ;;
            -v|--version)
                VERSION="$2"
                shift 2
                ;;
            -p|--push)
                PUSH_TO_REGISTRY=true
                shift
                ;;
            -s|--skip-build)
                skip_build=true
                shift
                ;;
            -m|--monitoring)
                deploy_monitoring_stack=true
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # Print deployment configuration
    log "Starting Aurigraph V11 deployment with the following configuration:"
    log_info "Build Type: $BUILD_TYPE"
    log_info "Environment: $ENVIRONMENT"
    log_info "Namespace: $NAMESPACE"
    log_info "Registry: $REGISTRY"
    log_info "Version: $VERSION"
    log_info "Skip Build: $skip_build"
    log_info "Deploy Monitoring: $deploy_monitoring_stack"
    
    # Execute deployment steps
    check_prerequisites
    validate_environment
    
    if [[ "$skip_build" != "true" ]]; then
        build_application
        build_docker_image
    fi
    
    deploy_secrets
    deploy_configmaps
    deploy_storage
    deploy_application
    deploy_services
    
    if [[ "$deploy_monitoring_stack" == "true" ]]; then
        deploy_monitoring
    fi
    
    wait_for_deployment
    validate_deployment
    run_smoke_tests
    cleanup_old_resources
    
    # Print deployment summary
    log_success "================================"
    log_success "Aurigraph V11 Deployment Complete"
    log_success "================================"
    log_success "Build Type: $BUILD_TYPE"
    log_success "Version: $VERSION"
    log_success "Namespace: $NAMESPACE"
    log_success ""
    log_success "Access URLs:"
    
    # Get external service URL if available
    local external_ip
    external_ip=$(kubectl get svc aurigraph-v11-service -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "pending")
    
    if [[ "$external_ip" != "pending" && -n "$external_ip" ]]; then
        log_success "  HTTP API: http://$external_ip:9003"
        log_success "  gRPC Service: $external_ip:9004"
        log_success "  Health Check: http://$external_ip:9003/q/health/ready"
        log_success "  Metrics: http://$external_ip:9003/q/metrics"
    else
        log_success "  Use kubectl port-forward for local access:"
        log_success "  kubectl port-forward -n $NAMESPACE svc/aurigraph-v11-service 9003:9003"
    fi
    
    log_success ""
    log_success "Useful commands:"
    log_success "  kubectl get pods -n $NAMESPACE -l app=aurigraph-v11"
    log_success "  kubectl logs -n $NAMESPACE -l app=aurigraph-v11 -f"
    log_success "  kubectl describe deployment aurigraph-v11-native -n $NAMESPACE"
    
    log_success "Deployment completed successfully!"
}

# Execute main function
main "$@"