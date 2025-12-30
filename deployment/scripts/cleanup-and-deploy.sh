#!/bin/bash

# Cleanup and Deploy script for Aurigraph V11
# This script stops all existing services and redeploys

set -e

REMOTE_USER="subbu"
REMOTE_HOST="dlt.aurigraph.io"
REMOTE_PASSWORD="subbuFuture@2025"
REMOTE_DIR="/home/subbu/aurigraph-v11"
LOCAL_JAR="target/aurigraph-v11-standalone-11.1.0-runner.jar"

echo "🧹 Cleanup and Deploy Script"
echo "=============================="
echo ""

# Step 1: Stop all Aurigraph services
echo "🛑 Step 1: Stopping all Aurigraph services..."
sshpass -p "$REMOTE_PASSWORD" ssh -p 22 -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} << 'CLEANUP'
#!/bin/bash

echo "Killing all Java processes with 'aurigraph' in the name..."
pkill -9 -f 'aurigraph.*jar' 2>/dev/null || echo "No Aurigraph processes found"

echo "Waiting for processes to terminate..."
sleep 3

echo "Checking for remaining processes..."
ps aux | grep -i aurigraph | grep -v grep || echo "All Aurigraph processes stopped"

echo "Checking port usage..."
netstat -tlnp 2>/dev/null | grep -E ':(9003|9004|8443)' || echo "Ports are free"

echo "✅ Cleanup complete"
CLEANUP

echo ""
echo "✅ Step 1 Complete: All services stopped"
echo ""

# Step 2: Upload JAR
echo "📤 Step 2: Uploading JAR to remote server..."
if [ ! -f "$LOCAL_JAR" ]; then
    echo "❌ Error: JAR not found at $LOCAL_JAR"
    exit 1
fi

sshpass -p "$REMOTE_PASSWORD" ssh -p 22 -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_DIR} && rm -f ${REMOTE_DIR}/*.jar"

sshpass -p "$REMOTE_PASSWORD" scp -P 22 -o StrictHostKeyChecking=no \
    $LOCAL_JAR ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/

echo "✅ Step 2 Complete: JAR uploaded"
echo ""

# Step 3: Start service with disabled gRPC and TLS
echo "🚀 Step 3: Starting Aurigraph V11 service..."
sshpass -p "$REMOTE_PASSWORD" ssh -p 22 -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} << 'START'
#!/bin/bash

cd /home/subbu/aurigraph-v11

JAR_FILE=$(ls -1 aurigraph-v11-standalone-*.jar | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ Error: JAR file not found"
    exit 1
fi

echo "🚀 Starting Aurigraph V11..."
echo "JAR: $JAR_FILE"

# Start with gRPC disabled and custom ports
nohup java -Xms512m -Xmx2g \
    -Dquarkus.http.host=0.0.0.0 \
    -Dquarkus.http.port=9003 \
    -Dquarkus.grpc.server.enabled=false \
    -Dquarkus.http.ssl-port=0 \
    -Dquarkus.http.insecure-requests=enabled \
    -jar $JAR_FILE \
    > aurigraph-v11.log 2>&1 &

SERVICE_PID=$!
echo "✅ Service started with PID: $SERVICE_PID"

# Wait for startup
sleep 8

# Check if process is still running
if ps -p $SERVICE_PID > /dev/null 2>&1; then
    echo "✅ Service is running"

    # Test health endpoint
    echo "🏥 Testing health endpoint..."
    sleep 3
    curl -s http://localhost:9003/q/health || echo "⏳ Health check pending..."

    echo ""
    echo "📊 Service Status:"
    echo "PID: $SERVICE_PID"
    echo "Port: 9003"
    echo "Logs: tail -f ~/aurigraph-v11/aurigraph-v11.log"
else
    echo "❌ Service failed to start"
    echo "Last 30 lines of log:"
    tail -30 aurigraph-v11.log
    exit 1
fi
START

echo ""
echo "✅ Step 3 Complete: Service started"
echo ""

# Step 4: Verify deployment
echo "🧪 Step 4: Verifying deployment..."
sleep 5

echo "Testing API endpoints..."
sshpass -p "$REMOTE_PASSWORD" ssh -p 22 -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} \
    "curl -s http://localhost:9003/q/health" 2>/dev/null || echo "⏳ Service still starting..."

echo ""
echo "🎉 Deployment Complete!"
echo "======================"
echo ""
echo "📡 Access URLs:"
echo "   Health:      http://dlt.aurigraph.io:9003/q/health"
echo "   System Info: http://dlt.aurigraph.io:9003/api/v11/info"
echo "   Swagger UI:  http://dlt.aurigraph.io:9003/q/swagger-ui"
echo ""
echo "📝 View Logs:"
echo "   ssh subbu@dlt.aurigraph.io"
echo "   tail -f ~/aurigraph-v11/aurigraph-v11.log"
echo ""
