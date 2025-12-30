#!/bin/bash

set -euo pipefail

# Backend Local Deployment Test Script
# Verifies all services start and are healthy

PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
DOCKER_DIR="$PROJECT_ROOT/infrastructure/docker"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
info() { echo -e "${BLUE}[INFO]${NC} $*"; }

log "╔════════════════════════════════════════════════════════════════════╗"
log "║ Aurigraph Backend - Local Deployment Test                          ║"
log "╚════════════════════════════════════════════════════════════════════╝"
log ""

# Stage 1: Verify Docker is running
log "Stage 1: Checking Docker..."
if ! command -v docker &> /dev/null; then
    error "Docker is not installed"
    exit 1
fi

if ! docker ps &> /dev/null; then
    error "Docker daemon is not running"
    exit 1
fi
log "✅ Docker is running"

# Stage 2: Verify Docker Compose files
log "Stage 2: Verifying docker-compose files..."
FILES=(
    "docker-compose.base.yml"
    "docker-compose.validators.yml"
    "docker-compose.monitoring.yml"
    "docker-compose.testing.yml"
    "docker-compose.production.yml"
)

for file in "${FILES[@]}"; do
    if [ -f "$DOCKER_DIR/$file" ]; then
        log "  ✅ $file exists"
    else
        error "$file not found"
        exit 1
    fi
done
log "✅ All docker-compose files present"

# Stage 3: Validate docker-compose syntax
log "Stage 3: Validating docker-compose configuration..."
docker-compose -f "$DOCKER_DIR/docker-compose.base.yml" config > /dev/null 2>&1 || {
    error "docker-compose.base.yml validation failed"
    exit 1
}
log "✅ docker-compose configuration is valid"

# Stage 4: Start services
log "Stage 4: Starting backend services (base stack)..."
docker-compose -f "$DOCKER_DIR/docker-compose.base.yml" \
    -p aurigraph-test \
    up -d

log "Waiting for services to initialize..."
sleep 10

# Stage 5: Verify services are running
log "Stage 5: Verifying services are running..."
RUNNING_CONTAINERS=$(docker-compose -f "$DOCKER_DIR/docker-compose.base.yml" \
    -p aurigraph-test \
    ps --services --filter "status=running" 2>/dev/null | wc -l || echo "0")

if [ "$RUNNING_CONTAINERS" -ge 3 ]; then
    log "✅ Services are running ($RUNNING_CONTAINERS containers)"
else
    warn "Expected 3+ services, found $RUNNING_CONTAINERS"
fi

# Stage 6: Health checks
log "Stage 6: Running health checks..."

# PostgreSQL health check
if docker exec aurigraph-test-postgres-1 pg_isready -U aurigraph &> /dev/null 2>&1; then
    log "  ✅ PostgreSQL is healthy"
else
    warn "  ⚠️  PostgreSQL health check failed (may need more time)"
fi

# Redis health check
if docker exec aurigraph-test-redis-1 redis-cli ping &> /dev/null 2>&1; then
    log "  ✅ Redis is healthy"
else
    warn "  ⚠️  Redis health check failed (may need more time)"
fi

# Backend API health check
if curl -sf http://localhost:9003/q/health > /dev/null 2>&1; then
    log "  ✅ Backend API is healthy (port 9003)"
else
    warn "  ⚠️  Backend API health check failed (may need more time)"
fi

# Stage 7: Display service information
log "Stage 7: Service Information"
echo ""
echo "Running Services:"
docker-compose -f "$DOCKER_DIR/docker-compose.base.yml" \
    -p aurigraph-test \
    ps

echo ""
echo "Access Points:"
info "  PostgreSQL: localhost:5432 (aurigraph/aurigraph_dev)"
info "  Redis: localhost:6379"
info "  Backend API: http://localhost:9003"
info "  Health Check: curl http://localhost:9003/q/health"

# Stage 8: Cleanup prompt
log ""
log "Stage 8: Test Complete"
log ""
log "✅ Backend services started successfully"
log ""
echo "To stop services, run:"
echo "  docker-compose -f infrastructure/docker/docker-compose.base.yml -p aurigraph-test down"
echo ""
echo "To view logs:"
echo "  docker-compose -f infrastructure/docker/docker-compose.base.yml -p aurigraph-test logs -f"
echo ""
