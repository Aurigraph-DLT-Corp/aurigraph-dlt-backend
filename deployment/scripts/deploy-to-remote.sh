#!/bin/bash

# Deployment script for Aurigraph V11 to remote server
# Server: dlt.aurigraph.io
# User: subbu

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
REMOTE_HOST="dlt.aurigraph.io"
REMOTE_USER="subbu"
REMOTE_DIR="/home/subbu/aurigraph-v11"
LOCAL_JAR="target/aurigraph-v11-standalone-11.1.0-runner.jar"
REMOTE_JAR="aurigraph-v11-standalone-11.1.0-runner.jar"
SERVICE_PORT=9003

echo -e "${GREEN}=== Aurigraph V11 Deployment to Remote Server ===${NC}"
echo -e "${YELLOW}Remote: ${REMOTE_USER}@${REMOTE_HOST}${NC}"
echo ""

# Step 1: Verify local JAR exists
if [ ! -f "$LOCAL_JAR" ]; then
    echo -e "${RED}ERROR: JAR file not found at $LOCAL_JAR${NC}"
    echo "Please run: ./mvnw clean package -DskipTests"
    exit 1
fi

echo -e "${GREEN}✓${NC} Local JAR verified: $LOCAL_JAR"
JAR_SIZE=$(du -h "$LOCAL_JAR" | cut -f1)
echo -e "  Size: $JAR_SIZE"
echo ""

# Step 2: Create remote directory if not exists
echo -e "${YELLOW}Creating remote directory...${NC}"
ssh ${REMOTE_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_DIR}"
echo -e "${GREEN}✓${NC} Remote directory ready"
echo ""

# Step 3: Stop existing service if running
echo -e "${YELLOW}Stopping existing service...${NC}"
ssh ${REMOTE_USER}@${REMOTE_HOST} "pkill -f 'aurigraph-v11-standalone.*runner.jar' || echo 'No existing process found'"
sleep 2
echo -e "${GREEN}✓${NC} Existing service stopped"
echo ""

# Step 4: Upload JAR to remote server
echo -e "${YELLOW}Uploading JAR to remote server...${NC}"
scp "$LOCAL_JAR" ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/${REMOTE_JAR}
echo -e "${GREEN}✓${NC} JAR uploaded successfully"
echo ""

# Step 5: Start the service in background
echo -e "${YELLOW}Starting Aurigraph V11 service...${NC}"
ssh ${REMOTE_USER}@${REMOTE_HOST} "cd ${REMOTE_DIR} && nohup java -Xmx4g -Xms2g -XX:+UseG1GC -jar ${REMOTE_JAR} > aurigraph-v11.log 2>&1 &"
echo -e "${GREEN}✓${NC} Service started in background"
echo ""

# Step 6: Wait for service to start
echo -e "${YELLOW}Waiting for service to start (30 seconds)...${NC}"
sleep 30
echo ""

# Step 7: Verify service is running
echo -e "${YELLOW}Verifying service health...${NC}"
HEALTH_CHECK=$(ssh ${REMOTE_USER}@${REMOTE_HOST} "curl -s http://localhost:${SERVICE_PORT}/api/v11/health 2>&1 || echo 'FAILED'")

if [[ $HEALTH_CHECK == *"FAILED"* ]] || [[ $HEALTH_CHECK == *"Connection refused"* ]]; then
    echo -e "${RED}✗${NC} Health check failed"
    echo -e "${YELLOW}Checking logs...${NC}"
    ssh ${REMOTE_USER}@${REMOTE_HOST} "tail -50 ${REMOTE_DIR}/aurigraph-v11.log"
else
    echo -e "${GREEN}✓${NC} Service is healthy!"
    echo -e "  Health response: $HEALTH_CHECK"
fi
echo ""

# Step 8: Display service information
echo -e "${GREEN}=== Deployment Complete ===${NC}"
echo ""
echo -e "Service URL: ${GREEN}http://${REMOTE_HOST}:${SERVICE_PORT}${NC}"
echo -e "Health Check: ${GREEN}http://${REMOTE_HOST}:${SERVICE_PORT}/api/v11/health${NC}"
echo ""
echo -e "${YELLOW}Useful commands:${NC}"
echo "  View logs:    ssh ${REMOTE_USER}@${REMOTE_HOST} 'tail -f ${REMOTE_DIR}/aurigraph-v11.log'"
echo "  Stop service: ssh ${REMOTE_USER}@${REMOTE_HOST} 'pkill -f aurigraph-v11-standalone.*runner.jar'"
echo ""
