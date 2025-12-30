#!/bin/bash

################################################################################
# Aurigraph V11 Production Deployment Script
# Deploys native image to dlt.aurigraph.io
#
# Usage: ./deploy-to-production.sh [native|jvm] [dev4|prod]
# Example: ./deploy-to-production.sh native prod
################################################################################

set -e

# Configuration
BUILD_TYPE=${1:-native}        # native or jvm
ENVIRONMENT=${2:-prod}        # dev4 or prod
VERSION=$(grep '<version>' pom.xml | head -1 | sed 's/.*<version>\([^<]*\)<\/version>.*/\1/')
REGISTRY="registry.aurigraph.io"
IMAGE_NAME="aurigraph-v11"
IMAGE_TAG="${VERSION}-${BUILD_TYPE}"
FULL_IMAGE="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

# Server configuration
if [ "$ENVIRONMENT" = "prod" ]; then
    REMOTE_HOST="dlt.aurigraph.io"
    REMOTE_PORT="2235"
    REMOTE_USER="subbu"
    DEPLOY_DIR="/opt/aurigraph/v11"
    SERVICE_NAME="aurigraph-v11"
else
    REMOTE_HOST="dev4.aurigraph.io"
    REMOTE_PORT="22"
    REMOTE_USER="ubuntu"
    DEPLOY_DIR="/home/ubuntu/aurigraph/v11"
    SERVICE_NAME="aurigraph-v11-dev"
fi

echo "========================================================================"
echo "Aurigraph V11 Production Deployment"
echo "========================================================================"
echo "Build Type:  $BUILD_TYPE"
echo "Environment: $ENVIRONMENT"
echo "Version:     $VERSION"
echo "Image:       $FULL_IMAGE"
echo "Target:      ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
echo "========================================================================="
echo ""

# Step 1: Verify build exists
echo "[1/7] Verifying build artifacts..."
if [ "$BUILD_TYPE" = "native" ]; then
    if [ ! -f "target/aurigraph-v11-standalone-${VERSION}-runner" ]; then
        echo "ERROR: Native executable not found. Run: ./mvnw package -Pnative"
        exit 1
    fi
    BINARY_PATH="target/aurigraph-v11-standalone-${VERSION}-runner"
    BINARY_SIZE=$(du -h "$BINARY_PATH" | cut -f1)
    echo "✓ Native binary found: $BINARY_SIZE"
else
    if [ ! -f "target/quarkus-app/quarkus-run.jar" ]; then
        echo "ERROR: JAR not found. Run: ./mvnw clean package"
        exit 1
    fi
    echo "✓ JAR build found"
fi

# Step 2: Build Docker image
echo ""
echo "[2/7] Building Docker image: $FULL_IMAGE"
docker build \
    --build-arg BUILD_TYPE=$BUILD_TYPE \
    -t "$FULL_IMAGE" \
    -f Dockerfile \
    . || { echo "ERROR: Docker build failed"; exit 1; }
echo "✓ Docker image built successfully"

# Step 3: Get Docker image size
IMAGE_SIZE=$(docker images "$FULL_IMAGE" --format "{{.Size}}")
echo "  Image size: $IMAGE_SIZE"

# Step 4: Tag for push (optional - if using local registry)
echo ""
echo "[3/7] Docker image ready at: $FULL_IMAGE"
echo "  (Local registry available at localhost:5000)"

# Step 5: SSH connectivity check
echo ""
echo "[4/7] Checking SSH connectivity to ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}..."
if timeout 5 ssh -p "${REMOTE_PORT}" "${REMOTE_USER}@${REMOTE_HOST}" "echo ✓ SSH connection successful" 2>/dev/null; then
    echo "✓ SSH connection verified"
else
    echo "⚠ WARNING: Could not verify SSH connection"
    echo "  Make sure your SSH key is added to ssh-agent:"
    echo "  ssh-add ~/.ssh/id_rsa"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Step 6: Create deployment manifest
echo ""
echo "[5/7] Creating deployment manifest..."
cat > /tmp/aurigraph-v11-manifest.yaml <<EOF
apiVersion: v1
kind: DeploymentManifest
metadata:
  name: aurigraph-v11
  version: $VERSION
  timestamp: $(date -u +'%Y-%m-%dT%H:%M:%SZ')
  build-type: $BUILD_TYPE
spec:
  image: $FULL_IMAGE
  service: $SERVICE_NAME
  port: 9003
  grpc-port: 9004
  replicas: 1
  resources:
    memory: 512Mi
    cpu: 2000m
  health-check:
    path: /api/v11/health
    interval: 30s
    timeout: 10s
    retries: 3
EOF
echo "✓ Deployment manifest created"

# Step 7: Generate deployment report
echo ""
echo "[6/7] Generating deployment report..."
cat > DEPLOYMENT-REPORT.md <<EOF
# Aurigraph V11 Production Deployment Report

**Date**: $(date)
**Version**: $VERSION
**Build Type**: $BUILD_TYPE
**Environment**: $ENVIRONMENT
**Image**: $FULL_IMAGE
**Image Size**: $IMAGE_SIZE

## Deployment Instructions

### 1. Load Docker Image
\`\`\`bash
# Option A: Using local registry
docker tag $FULL_IMAGE localhost:5000/$IMAGE_NAME:$IMAGE_TAG
docker push localhost:5000/$IMAGE_NAME:$IMAGE_TAG

# Option B: Using Docker save/load
docker save $FULL_IMAGE | ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST 'docker load'
\`\`\`

### 2. Deploy to Server
\`\`\`bash
# SSH into server
ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST

# Create deployment directory
sudo mkdir -p $DEPLOY_DIR
cd $DEPLOY_DIR

# Stop running service
sudo systemctl stop $SERVICE_NAME || true

# Pull and run Docker image
docker run -d \\
  --name $SERVICE_NAME \\
  --restart unless-stopped \\
  -p 9003:9003 \\
  -p 9004:9004 \\
  -v $DEPLOY_DIR/config:/deployments/config \\
  -v $DEPLOY_DIR/data:/deployments/data \\
  -v $DEPLOY_DIR/logs:/deployments/logs \\
  -e QUARKUS_PROFILE=prod \\
  -e QUARKUS_HTTP_HOST=0.0.0.0 \\
  $FULL_IMAGE

# Verify deployment
curl http://localhost:9003/api/v11/health
\`\`\`

### 3. Update NGINX
\`\`\`bash
# Update NGINX configuration
cd /opt/nginx
sudo ./deploy-nginx.sh --update-backend $SERVICE_NAME 9003

# Test NGINX config
sudo nginx -t

# Reload NGINX
sudo systemctl reload nginx
\`\`\`

## Rollback Procedure

\`\`\`bash
# Stop current deployment
docker stop $SERVICE_NAME
docker rm $SERVICE_NAME

# Restart previous version (if available)
docker run -d --name $SERVICE_NAME-backup ... (previous command)

# Or restore from backup
sudo systemctl start ${SERVICE_NAME}-backup
\`\`\`

## Performance Targets

- **Throughput**: 776K+ TPS (current), 2M+ TPS (target)
- **Latency**: < 3ms average, < 50ms P99
- **Memory**: < 256MB (native), < 512MB (JVM)
- **Startup**: < 1s (native), ~3s (JVM)

## Health Checks

- **Service Health**: http://dlt.aurigraph.io/api/v11/health
- **Metrics**: http://dlt.aurigraph.io/q/metrics
- **OpenAPI Docs**: http://dlt.aurigraph.io/q/swagger-ui
- **Logs**: tail -f $DEPLOY_DIR/logs/aurigraph-v11.log

## Post-Deployment Validation

- [ ] Service is running: \`docker ps | grep $SERVICE_NAME\`
- [ ] Health endpoint responds: \`curl http://localhost:9003/api/v11/health\`
- [ ] All 26+ endpoints accessible
- [ ] NGINX reverse proxy working
- [ ] SSL/TLS certificates valid
- [ ] Rate limiting enabled
- [ ] Monitoring and alerting active

---

**Deployment Status**: Ready for deployment
**Date**: $(date)
EOF

echo "✓ Deployment report generated: DEPLOYMENT-REPORT.md"

# Step 8: Display summary
echo ""
echo "========================================================================"
echo "Deployment Summary"
echo "========================================================================"
echo "✓ Build verified"
echo "✓ Docker image: $FULL_IMAGE"
echo "✓ Image size: $IMAGE_SIZE"
echo "✓ Deployment manifest created"
echo "✓ Deployment report generated"
echo ""
echo "Next Steps:"
echo "1. Review deployment report: cat DEPLOYMENT-REPORT.md"
echo "2. Load Docker image on production server"
echo "3. Run docker container with deployment script"
echo "4. Verify health endpoints"
echo "5. Update NGINX configuration"
echo "6. Monitor logs and metrics"
echo ""
echo "Quick Deploy Command:"
echo "  docker load < image.tar && docker run -d -p 9003:9003 $FULL_IMAGE"
echo ""
echo "========================================================================"
echo "✓ Deployment preparation complete"
echo "========================================================================"
