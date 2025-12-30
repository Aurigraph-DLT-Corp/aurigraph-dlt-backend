#!/bin/bash

# Deployment Script for Aurigraph V11 to Remote Server
# Target: dlt.aurigraph.io:2235
# Version: 11.3.0

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REMOTE_HOST="dlt.aurigraph.io"
REMOTE_PORT="22"
REMOTE_USER="subbu"
REMOTE_DIR="/home/subbu/aurigraph-v11"
JAR_FILE="target/aurigraph-v11-standalone-11.3.0-runner.jar"
APP_PORT="9003"
GRPC_PORT="9004"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}Aurigraph V11 Deployment Script${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}❌ JAR file not found: $JAR_FILE${NC}"
    echo "Please run: ./mvnw clean package -DskipTests"
    exit 1
fi

echo -e "${GREEN}✅ JAR file found: $JAR_FILE ($(du -h $JAR_FILE | cut -f1))${NC}"
echo ""

# Create remote directory structure
echo -e "${YELLOW}📦 Creating remote directory structure...${NC}"
ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST << 'EOF'
    mkdir -p ~/aurigraph-v11/logs
    mkdir -p ~/aurigraph-v11/data
    echo "✅ Directories created"
EOF

# Stop existing service if running
echo -e "${YELLOW}🛑 Stopping existing V11 service (if running)...${NC}"
ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST << 'EOF'
    pkill -f "aurigraph-v11-standalone.*\.jar" || echo "No existing process found"
EOF

# Split JAR file for transfer (if needed)
JAR_SIZE=$(stat -f%z "$JAR_FILE" 2>/dev/null || stat -c%s "$JAR_FILE")
MAX_SIZE=$((100 * 1024 * 1024))  # 100MB

if [ $JAR_SIZE -gt $MAX_SIZE ]; then
    echo -e "${YELLOW}📦 JAR is large ($(du -h $JAR_FILE | cut -f1)), splitting for transfer...${NC}"
    split -b 90M "$JAR_FILE" "${JAR_FILE}.part"

    # Transfer parts
    for part in ${JAR_FILE}.part*; do
        echo -e "${YELLOW}📤 Uploading $(basename $part)...${NC}"
        scp -P $REMOTE_PORT "$part" $REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/
    done

    # Reassemble on remote
    echo -e "${YELLOW}🔧 Reassembling JAR on remote server...${NC}"
    ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST << EOF
        cd $REMOTE_DIR
        cat aurigraph-v11-standalone-11.3.0-runner.jar.part* > aurigraph-v11-standalone-11.3.0-runner.jar
        rm -f aurigraph-v11-standalone-11.3.0-runner.jar.part*
        chmod +x aurigraph-v11-standalone-11.3.0-runner.jar
        echo "✅ JAR reassembled"
EOF

    # Cleanup local parts
    rm -f ${JAR_FILE}.part*
else
    # Direct transfer for smaller JARs
    echo -e "${YELLOW}📤 Uploading JAR file to remote server...${NC}"
    scp -P $REMOTE_PORT "$JAR_FILE" $REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/
fi

# Transfer start script
echo -e "${YELLOW}📤 Uploading start script...${NC}"
cat > /tmp/start-v11.sh << 'EOFSCRIPT'
#!/bin/bash
cd ~/aurigraph-v11
nohup java -Xms1g -Xmx4g \
    -Dquarkus.http.port=9003 \
    -Dquarkus.grpc.server.port=9004 \
    -Dquarkus.log.level=INFO \
    -Dquarkus.log.file.enable=true \
    -Dquarkus.log.file.path=logs/aurigraph-v11.log \
    -jar aurigraph-v11-standalone-11.3.0-runner.jar \
    > logs/console.log 2>&1 &
echo $! > v11.pid
echo "✅ Aurigraph V11 started (PID: $(cat v11.pid))"
EOFSCRIPT

scp -P $REMOTE_PORT /tmp/start-v11.sh $REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/
rm /tmp/start-v11.sh

# Make start script executable and start service
echo -e "${YELLOW}🚀 Starting Aurigraph V11 service...${NC}"
ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST << 'EOF'
    cd ~/aurigraph-v11
    chmod +x start-v11.sh
    ./start-v11.sh
EOF

# Wait for service to start
echo -e "${YELLOW}⏳ Waiting for service to start...${NC}"
sleep 10

# Check service status
echo -e "${YELLOW}🔍 Checking service status...${NC}"
ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST << 'EOF'
    if [ -f ~/aurigraph-v11/v11.pid ]; then
        PID=$(cat ~/aurigraph-v11/v11.pid)
        if ps -p $PID > /dev/null; then
            echo "✅ Service is running (PID: $PID)"

            # Test health endpoint
            sleep 5
            if curl -s http://localhost:9003/q/health > /dev/null; then
                echo "✅ Health endpoint responding"
            else
                echo "⚠️  Health endpoint not responding yet"
            fi
        else
            echo "❌ Service process not found"
            exit 1
        fi
    else
        echo "❌ PID file not found"
        exit 1
    fi
EOF

echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}✅ Deployment Complete!${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""
echo -e "Service endpoints (from within server):"
echo -e "  Health:  http://localhost:9003/q/health"
echo -e "  API:     http://localhost:9003/api/v11/"
echo -e "  Metrics: http://localhost:9003/q/metrics"
echo ""
echo -e "To view logs:"
echo -e "  ${YELLOW}ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST \"tail -f ~/aurigraph-v11/logs/aurigraph-v11.log\"${NC}"
echo ""
echo -e "To stop service:"
echo -e "  ${YELLOW}ssh -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST \"kill \$(cat ~/aurigraph-v11/v11.pid)\"${NC}"
echo ""
