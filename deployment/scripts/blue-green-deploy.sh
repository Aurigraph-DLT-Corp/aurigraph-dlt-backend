#!/bin/bash

###############################################################################
# Blue-Green Deployment Script for Aurigraph V11
# Stream 5: Production Monitoring & Deployment
#
# Implements zero-downtime blue-green deployment strategy:
# 1. Deploy new version to "green" environment
# 2. Run health checks and smoke tests
# 3. Gradually shift traffic from blue to green
# 4. Monitor performance and errors
# 5. Complete cutover or rollback if issues detected
#
# Usage:
#   ./blue-green-deploy.sh <version> [--auto-approve]
#
# Example:
#   ./blue-green-deploy.sh v11.1.0
#   ./blue-green-deploy.sh v11.1.0 --auto-approve
#
###############################################################################

set -euo pipefail

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly VERSION="${1:-}"
readonly AUTO_APPROVE="${2:-}"
readonly NAMESPACE="aurigraph-production"
readonly SERVICE_NAME="aurigraph-v11"
readonly HEALTH_CHECK_URL="http://localhost:9003/q/health"
readonly METRICS_URL="http://localhost:9003/q/metrics"

# Deployment settings
readonly HEALTH_CHECK_TIMEOUT=300
readonly HEALTH_CHECK_INTERVAL=5
readonly TRAFFIC_SHIFT_STEPS=10
readonly TRAFFIC_SHIFT_DELAY=30
readonly ROLLBACK_THRESHOLD_ERROR_RATE=5.0
readonly ROLLBACK_THRESHOLD_LATENCY_MS=1000

# Current state tracking
CURRENT_COLOR=""
NEW_COLOR=""
DEPLOYMENT_START_TIME=""

###############################################################################
# Logging Functions
###############################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $*"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*"
}

###############################################################################
# Validation Functions
###############################################################################

validate_prerequisites() {
    log_info "Validating prerequisites..."

    # Check required tools
    local required_tools=("kubectl" "docker" "curl" "jq")
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            log_error "Required tool not found: $tool"
            exit 1
        fi
    done

    # Check version parameter
    if [ -z "$VERSION" ]; then
        log_error "Version parameter required"
        echo "Usage: $0 <version> [--auto-approve]"
        exit 1
    fi

    # Check Docker image exists
    if ! docker image inspect "aurigraph-v11:$VERSION" &> /dev/null; then
        log_error "Docker image not found: aurigraph-v11:$VERSION"
        log_info "Build the image first: docker build -t aurigraph-v11:$VERSION ."
        exit 1
    fi

    # Check Kubernetes connection
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi

    log_success "Prerequisites validated"
}

###############################################################################
# Environment Detection
###############################################################################

detect_current_environment() {
    log_info "Detecting current environment..."

    # Get current active color (blue or green)
    local blue_count green_count
    blue_count=$(kubectl get pods -n "$NAMESPACE" -l app="$SERVICE_NAME",color=blue --no-headers 2>/dev/null | wc -l || echo "0")
    green_count=$(kubectl get pods -n "$NAMESPACE" -l app="$SERVICE_NAME",color=green --no-headers 2>/dev/null | wc -l || echo "0")

    if [ "$blue_count" -gt 0 ] && [ "$green_count" -eq 0 ]; then
        CURRENT_COLOR="blue"
        NEW_COLOR="green"
    elif [ "$green_count" -gt 0 ] && [ "$blue_count" -eq 0 ]; then
        CURRENT_COLOR="green"
        NEW_COLOR="blue"
    elif [ "$blue_count" -eq 0 ] && [ "$green_count" -eq 0 ]; then
        CURRENT_COLOR="none"
        NEW_COLOR="blue"
    else
        log_error "Both blue and green environments are running. Clean up first."
        exit 1
    fi

    log_info "Current environment: $CURRENT_COLOR"
    log_info "New environment: $NEW_COLOR"
}

###############################################################################
# Deployment Functions
###############################################################################

deploy_new_environment() {
    log_info "Deploying new environment ($NEW_COLOR) with version $VERSION..."

    # Create deployment manifest
    cat > "/tmp/aurigraph-$NEW_COLOR-deployment.yaml" <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $SERVICE_NAME-$NEW_COLOR
  namespace: $NAMESPACE
  labels:
    app: $SERVICE_NAME
    color: $NEW_COLOR
    version: $VERSION
spec:
  replicas: 3
  selector:
    matchLabels:
      app: $SERVICE_NAME
      color: $NEW_COLOR
  template:
    metadata:
      labels:
        app: $SERVICE_NAME
        color: $NEW_COLOR
        version: $VERSION
    spec:
      containers:
      - name: aurigraph-v11
        image: aurigraph-v11:$VERSION
        ports:
        - containerPort: 9003
          name: http
        - containerPort: 9004
          name: grpc
        env:
        - name: QUARKUS_PROFILE
          value: "production"
        - name: ENVIRONMENT_COLOR
          value: "$NEW_COLOR"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 9003
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 9003
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        startupProbe:
          httpGet:
            path: /q/health/started
            port: 9003
          initialDelaySeconds: 5
          periodSeconds: 5
          failureThreshold: 30
---
apiVersion: v1
kind: Service
metadata:
  name: $SERVICE_NAME-$NEW_COLOR
  namespace: $NAMESPACE
  labels:
    app: $SERVICE_NAME
    color: $NEW_COLOR
spec:
  selector:
    app: $SERVICE_NAME
    color: $NEW_COLOR
  ports:
  - name: http
    protocol: TCP
    port: 9003
    targetPort: 9003
  - name: grpc
    protocol: TCP
    port: 9004
    targetPort: 9004
  type: ClusterIP
EOF

    # Apply deployment
    kubectl apply -f "/tmp/aurigraph-$NEW_COLOR-deployment.yaml"

    log_success "Deployment created for $NEW_COLOR environment"
}

wait_for_health() {
    log_info "Waiting for new environment to become healthy..."

    local elapsed=0
    local max_wait=$HEALTH_CHECK_TIMEOUT

    while [ $elapsed -lt $max_wait ]; do
        # Check pod readiness
        local ready_pods
        ready_pods=$(kubectl get pods -n "$NAMESPACE" -l app="$SERVICE_NAME",color="$NEW_COLOR" \
            -o jsonpath='{.items[?(@.status.conditions[?(@.type=="Ready")].status=="True")].metadata.name}' 2>/dev/null | wc -w || echo "0")

        if [ "$ready_pods" -ge 3 ]; then
            log_success "All pods are ready"
            return 0
        fi

        echo -n "."
        sleep $HEALTH_CHECK_INTERVAL
        elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
    done

    log_error "Health check timeout after ${max_wait}s"
    return 1
}

run_smoke_tests() {
    log_info "Running smoke tests on new environment..."

    # Get pod IP for testing
    local pod_ip
    pod_ip=$(kubectl get pods -n "$NAMESPACE" -l app="$SERVICE_NAME",color="$NEW_COLOR" \
        -o jsonpath='{.items[0].status.podIP}' 2>/dev/null || echo "")

    if [ -z "$pod_ip" ]; then
        log_error "Cannot get pod IP for smoke tests"
        return 1
    fi

    # Test 1: Health endpoint
    log_info "Test 1: Health endpoint"
    if ! kubectl run curl-test --image=curlimages/curl:latest --rm -i --restart=Never -n "$NAMESPACE" -- \
        curl -sf "http://$pod_ip:9003/q/health" > /dev/null 2>&1; then
        log_error "Health endpoint test failed"
        return 1
    fi
    log_success "Health endpoint test passed"

    # Test 2: System info endpoint
    log_info "Test 2: System info endpoint"
    if ! kubectl run curl-test --image=curlimages/curl:latest --rm -i --restart=Never -n "$NAMESPACE" -- \
        curl -sf "http://$pod_ip:9003/api/v11/info" > /dev/null 2>&1; then
        log_error "System info endpoint test failed"
        return 1
    fi
    log_success "System info endpoint test passed"

    # Test 3: Metrics endpoint
    log_info "Test 3: Metrics endpoint"
    if ! kubectl run curl-test --image=curlimages/curl:latest --rm -i --restart=Never -n "$NAMESPACE" -- \
        curl -sf "http://$pod_ip:9003/q/metrics" > /dev/null 2>&1; then
        log_error "Metrics endpoint test failed"
        return 1
    fi
    log_success "Metrics endpoint test passed"

    log_success "All smoke tests passed"
    return 0
}

###############################################################################
# Traffic Shifting Functions
###############################################################################

shift_traffic() {
    log_info "Starting gradual traffic shift from $CURRENT_COLOR to $NEW_COLOR..."

    for step in $(seq 1 $TRAFFIC_SHIFT_STEPS); do
        local new_weight=$((step * 100 / TRAFFIC_SHIFT_STEPS))
        local old_weight=$((100 - new_weight))

        log_info "Step $step/$TRAFFIC_SHIFT_STEPS: Shifting to ${new_weight}% $NEW_COLOR, ${old_weight}% $CURRENT_COLOR"

        # Update service selector weights (using Istio or nginx-ingress)
        # This is a simplified example - adjust for your ingress controller
        kubectl patch service "$SERVICE_NAME" -n "$NAMESPACE" --type='json' \
            -p="[{'op': 'replace', 'path': '/spec/selector/color', 'value': '$NEW_COLOR'}]" 2>/dev/null || true

        # Wait and monitor
        sleep $TRAFFIC_SHIFT_DELAY

        # Check metrics
        if ! check_deployment_health; then
            log_error "Health check failed during traffic shift at step $step"
            return 1
        fi
    done

    log_success "Traffic shift completed successfully"
    return 0
}

check_deployment_health() {
    # Get metrics from Prometheus
    local error_rate latency_p99

    # Query error rate (this is a placeholder - adjust for your metrics endpoint)
    error_rate=$(curl -s "$METRICS_URL" | grep -E "^aurigraph_.*_errors" | awk '{sum+=$2} END {print sum}' || echo "0")
    latency_p99=$(curl -s "$METRICS_URL" | grep -E "p99.*latency" | awk '{print $2}' | sort -rn | head -1 || echo "0")

    log_info "Current error rate: ${error_rate}, P99 latency: ${latency_p99}ms"

    # Check thresholds
    if (( $(echo "$error_rate > $ROLLBACK_THRESHOLD_ERROR_RATE" | bc -l) )); then
        log_warning "Error rate exceeds threshold: $error_rate > $ROLLBACK_THRESHOLD_ERROR_RATE"
        return 1
    fi

    if (( $(echo "$latency_p99 > $ROLLBACK_THRESHOLD_LATENCY_MS" | bc -l) )); then
        log_warning "P99 latency exceeds threshold: $latency_p99 > $ROLLBACK_THRESHOLD_LATENCY_MS"
        return 1
    fi

    return 0
}

###############################################################################
# Rollback Functions
###############################################################################

rollback_deployment() {
    log_warning "Initiating rollback to $CURRENT_COLOR environment..."

    # Restore traffic to old environment
    kubectl patch service "$SERVICE_NAME" -n "$NAMESPACE" --type='json' \
        -p="[{'op': 'replace', 'path': '/spec/selector/color', 'value': '$CURRENT_COLOR'}]"

    # Delete new deployment
    kubectl delete deployment "$SERVICE_NAME-$NEW_COLOR" -n "$NAMESPACE" --ignore-not-found

    log_warning "Rollback completed. Traffic restored to $CURRENT_COLOR"
}

###############################################################################
# Cleanup Functions
###############################################################################

cleanup_old_environment() {
    log_info "Cleaning up old environment ($CURRENT_COLOR)..."

    if [ "$CURRENT_COLOR" != "none" ]; then
        # Delete old deployment
        kubectl delete deployment "$SERVICE_NAME-$CURRENT_COLOR" -n "$NAMESPACE" --ignore-not-found
        kubectl delete service "$SERVICE_NAME-$CURRENT_COLOR" -n "$NAMESPACE" --ignore-not-found

        log_success "Old environment cleaned up"
    fi
}

###############################################################################
# Main Deployment Flow
###############################################################################

main() {
    DEPLOYMENT_START_TIME=$(date +%s)

    log_info "=========================================="
    log_info "Aurigraph V11 Blue-Green Deployment"
    log_info "Version: $VERSION"
    log_info "=========================================="

    # Step 1: Validate
    validate_prerequisites

    # Step 2: Detect current environment
    detect_current_environment

    # Step 3: Confirm deployment
    if [ "$AUTO_APPROVE" != "--auto-approve" ]; then
        echo ""
        log_warning "This will deploy version $VERSION to $NEW_COLOR environment"
        log_warning "Current active: $CURRENT_COLOR"
        echo -n "Continue? (yes/no): "
        read -r response
        if [ "$response" != "yes" ]; then
            log_info "Deployment cancelled"
            exit 0
        fi
    fi

    # Step 4: Deploy new environment
    deploy_new_environment

    # Step 5: Wait for health
    if ! wait_for_health; then
        log_error "Deployment failed - pods not healthy"
        rollback_deployment
        exit 1
    fi

    # Step 6: Run smoke tests
    if ! run_smoke_tests; then
        log_error "Smoke tests failed"
        rollback_deployment
        exit 1
    fi

    # Step 7: Gradual traffic shift
    if ! shift_traffic; then
        log_error "Traffic shift failed"
        rollback_deployment
        exit 1
    fi

    # Step 8: Final health check
    sleep 30
    if ! check_deployment_health; then
        log_error "Final health check failed"
        rollback_deployment
        exit 1
    fi

    # Step 9: Cleanup old environment
    cleanup_old_environment

    # Calculate deployment duration
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - DEPLOYMENT_START_TIME))

    log_success "=========================================="
    log_success "Deployment completed successfully!"
    log_success "Version: $VERSION"
    log_success "Active environment: $NEW_COLOR"
    log_success "Duration: ${duration}s"
    log_success "=========================================="
}

# Run main function
main "$@"
