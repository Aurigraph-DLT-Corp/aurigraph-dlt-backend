#!/bin/bash

# JIRA Ticket Creation Script for Sprints 15-18
# Creates 40 tickets (4 epics + 36 stories)

# JIRA Configuration
export JIRA_EMAIL="subbu@aurigraph.io"
export JIRA_API_TOKEN="ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"
export JIRA_BASE_URL="https://aurigraphdlt.atlassian.net"
export JIRA_PROJECT_KEY="AV11"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Counter
CREATED_COUNT=0
FAILED_COUNT=0

# Function to create JIRA ticket
create_jira_ticket() {
    local ticket_key="$1"
    local ticket_type="$2"
    local summary="$3"
    local description="$4"
    local story_points="${5:-0}"
    local parent_key="${6:-}"
    local priority="${7:-Medium}"
    local labels="${8:-}"

    echo -e "${YELLOW}Creating ${ticket_type}: ${ticket_key} - ${summary}${NC}"

    # Build JSON payload
    local json_payload=$(cat <<EOF
{
  "fields": {
    "project": {
      "key": "${JIRA_PROJECT_KEY}"
    },
    "summary": "${summary}",
    "description": {
      "type": "doc",
      "version": 1,
      "content": [
        {
          "type": "paragraph",
          "content": [
            {
              "type": "text",
              "text": "${description}"
            }
          ]
        }
      ]
    },
    "issuetype": {
      "name": "${ticket_type}"
    },
    "priority": {
      "name": "${priority}"
    }
    $(if [ -n "$parent_key" ]; then echo ",\"parent\": {\"key\": \"${parent_key}\"}"; fi)
    $(if [ -n "$labels" ]; then echo ",\"labels\": [\"${labels}\"]"; fi)
  }
}
EOF
)

    # Create ticket
    response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Accept: application/json" \
        -H "Content-Type: application/json" \
        -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
        "${JIRA_BASE_URL}/rest/api/3/issue" \
        -d "${json_payload}")

    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "201" ]; then
        created_key=$(echo "$response_body" | grep -o '"key":"[^"]*"' | cut -d'"' -f4)
        echo -e "${GREEN}✓ Created: ${created_key}${NC}"
        ((CREATED_COUNT++))

        # Add story points if applicable (custom field)
        if [ "$story_points" != "0" ]; then
            update_story_points "$created_key" "$story_points"
        fi

        echo "$created_key"
    else
        echo -e "${RED}✗ Failed: ${ticket_key} (HTTP ${http_code})${NC}"
        echo "$response_body" | head -5
        ((FAILED_COUNT++))
        echo "FAILED"
    fi
}

# Function to update story points
update_story_points() {
    local issue_key="$1"
    local points="$2"

    # Try to set story points using customfield_10016 (common field ID)
    curl -s -X PUT \
        -H "Accept: application/json" \
        -H "Content-Type: application/json" \
        -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
        "${JIRA_BASE_URL}/rest/api/3/issue/${issue_key}" \
        -d "{\"fields\":{\"customfield_10016\":${points}}}" > /dev/null 2>&1
}

# Create tickets
echo "========================================="
echo "JIRA Ticket Creation - Sprints 15-18"
echo "========================================="
echo ""

# ====================================
# SPRINT 15: Core Node Implementation
# ====================================
echo -e "${YELLOW}=== SPRINT 15: Core Node Implementation ===${NC}"

EPIC_208=$(create_jira_ticket "AV11-208" "Epic" \
    "Sprint 15: Core Node Implementation" \
    "Complete implementation of Channel, Validator, Business, and API Integration nodes. All nodes must implement the base Node interface and integrate with the V11 backend." \
    89 "" "Highest" "node-implementation,backend,core")

sleep 2

create_jira_ticket "AV11-209" "Story" \
    "Complete Channel Node Service Implementation" \
    "Implement complete Channel Node system including ChannelNode.java, ChannelNodeService.java, ChannelNodeResource.java. Performance: 500K msg/sec, 10K+ concurrent channels, <5ms routing latency." \
    13 "$EPIC_208" "Highest" "channel-node,backend"

sleep 1

create_jira_ticket "AV11-210" "Story" \
    "Complete Validator Node Service Implementation" \
    "Implement complete Validator Node with HyperRAFT++ consensus integration. Performance: 200K TPS per validator, <500ms block proposal, <1s finality." \
    13 "$EPIC_208" "Highest" "validator-node,consensus,backend"

sleep 1

create_jira_ticket "AV11-211" "Story" \
    "Complete Business Node Service Implementation" \
    "Implement complete Business Node with smart contract execution. Performance: 100K tx/sec, <100ms contract execution, <200ms workflow processing." \
    13 "$EPIC_208" "High" "business-node,smart-contracts,backend"

sleep 1

create_jira_ticket "AV11-212" "Story" \
    "Complete API Integration Node - Alpaca Markets" \
    "Implement API Integration Node for Alpaca Markets API. Performance: 10K API calls/sec, >90% cache hit rate, <5s data freshness, <100ms latency." \
    13 "$EPIC_208" "High" "api-node,alpaca,external-api"

sleep 1

create_jira_ticket "AV11-213" "Story" \
    "Complete API Integration Node - Weather Service" \
    "Implement API Integration Node for Weather.com API. Performance: 10K API calls/sec, 5min cache TTL, <10s data freshness." \
    8 "$EPIC_208" "Medium" "api-node,weather,external-api"

sleep 1

create_jira_ticket "AV11-214" "Story" \
    "Implement Base Node Interface and Abstract Class" \
    "Create base Node interface, AbstractNode base class, NodeFactory, and NodeRegistry. Foundation for all node types." \
    8 "$EPIC_208" "Highest" "node-interface,foundation,backend"

sleep 1

create_jira_ticket "AV11-215" "Story" \
    "Implement Node Configuration Management System" \
    "Create dynamic node configuration system with validation, hot reload, and persistence. Support global, node-type, and instance-level configs." \
    8 "$EPIC_208" "High" "configuration,backend"

sleep 1

create_jira_ticket "AV11-216" "Story" \
    "Implement Node State Management and Persistence" \
    "Create node state management with LevelDB persistence, state sync, recovery, and snapshots. Handle config, runtime, cache, and metrics state." \
    8 "$EPIC_208" "High" "state-management,persistence,backend"

sleep 1

create_jira_ticket "AV11-217" "Story" \
    "Implement Node Metrics and Monitoring" \
    "Create comprehensive metrics collection with Prometheus export, real-time streaming, and alerting. Track TPS, latency, errors, and resources." \
    5 "$EPIC_208" "Medium" "metrics,monitoring,observability"

sleep 2

# ====================================
# SPRINT 16: Real-Time Infrastructure & Visualization
# ====================================
echo ""
echo -e "${YELLOW}=== SPRINT 16: Real-Time Infrastructure & Visualization ===${NC}"

EPIC_218=$(create_jira_ticket "AV11-218" "Epic" \
    "Sprint 16: Real-Time Infrastructure & Visualization" \
    "Implement WebSocket layer and Vizro graph visualization for real-time node monitoring and interaction." \
    102 "" "High" "real-time,visualization,frontend,infrastructure")

sleep 2

create_jira_ticket "AV11-219" "Story" \
    "Implement WebSocket Server Infrastructure" \
    "Create WebSocket server for real-time communication. Performance: 10K concurrent connections, <50ms latency, 50K events/sec." \
    13 "$EPIC_218" "Highest" "websocket,real-time,backend"

sleep 1

create_jira_ticket "AV11-220" "Story" \
    "Implement Real-Time Event System" \
    "Create event system for real-time node updates. Support channel, consensus, transaction, metric, and system events with filtering and replay." \
    13 "$EPIC_218" "High" "events,real-time,backend"

sleep 1

create_jira_ticket "AV11-221" "Story" \
    "Create Vizro Graph Visualization Component" \
    "Build Vizro graph visualization for node network. Features: force-directed layout, drag/drop, zoom/pan, real-time updates, performance overlays." \
    21 "$EPIC_218" "High" "vizro,visualization,frontend"

sleep 1

create_jira_ticket "AV11-222" "Story" \
    "Create Node Panel UI Components" \
    "Build node control panel UI with filtering, sorting, search, and real-time updates. Components: NodeList, NodeCard, NodeStatus, NodeMetrics." \
    13 "$EPIC_218" "High" "ui,frontend,react"

sleep 1

create_jira_ticket "AV11-223" "Story" \
    "Implement Node Configuration UI" \
    "Build node configuration interface with validation, presets, import/export, and history. Support all node types and configuration levels." \
    13 "$EPIC_218" "Medium-High" "ui,configuration,frontend"

sleep 1

create_jira_ticket "AV11-224" "Story" \
    "Implement Real-Time Data Streaming to UI" \
    "Create data streaming pipeline from nodes to UI via WebSocket. Include throttling, reconnection, and error handling." \
    8 "$EPIC_218" "High" "streaming,real-time,frontend"

sleep 1

create_jira_ticket "AV11-225" "Story" \
    "Create Scalability Demo Mode" \
    "Build auto-scaling demonstration with load generator, performance visualization, and capacity metrics. Demo scenarios: gradual load, spike, failover." \
    13 "$EPIC_218" "Medium" "demo,scalability,frontend"

sleep 1

create_jira_ticket "AV11-226" "Story" \
    "Implement Performance Dashboard" \
    "Create performance monitoring dashboard with TPS charts, latency distribution, resource utilization, error tracking, and historical views." \
    8 "$EPIC_218" "Medium-High" "dashboard,monitoring,frontend"

sleep 1

create_jira_ticket "AV11-227" "Story" \
    "Create Node Health Monitoring UI" \
    "Build health monitoring and alerting UI with health dashboard, notifications, history timeline, and diagnostic tools." \
    5 "$EPIC_218" "Medium" "health,monitoring,frontend"

sleep 2

# ====================================
# SPRINT 17: Advanced Features & Integration
# ====================================
echo ""
echo -e "${YELLOW}=== SPRINT 17: Advanced Features & Integration ===${NC}"

EPIC_228=$(create_jira_ticket "AV11-228" "Epic" \
    "Sprint 17: Advanced Features & Integration" \
    "Advanced node features and V11 backend integration including transaction service, consensus, gRPC, and cross-chain bridge." \
    95 "" "Medium-High" "integration,advanced-features,backend")

sleep 2

create_jira_ticket "AV11-229" "Story" \
    "Integrate Nodes with V11 Transaction Service" \
    "Connect Business nodes with TransactionService. Support transaction submission, status tracking, batch operations, and error handling." \
    13 "$EPIC_228" "High" "integration,transactions,backend"

sleep 1

create_jira_ticket "AV11-230" "Story" \
    "Integrate Nodes with HyperRAFT++ Consensus" \
    "Connect Validator nodes with HyperRAFT++ consensus. Support leader election, block proposal, voting, and state synchronization." \
    13 "$EPIC_228" "High" "integration,consensus,backend"

sleep 1

create_jira_ticket "AV11-231" "Story" \
    "Implement Inter-Node gRPC Communication" \
    "Create gRPC communication layer between nodes. Support RPC calls, streaming, load balancing, circuit breaker, and mTLS." \
    13 "$EPIC_228" "High" "grpc,communication,backend"

sleep 1

create_jira_ticket "AV11-232" "Story" \
    "Implement Node Discovery and Service Registry" \
    "Create automatic node discovery system with Kubernetes and DNS integration. Support auto-registration and health-based de-registration." \
    8 "$EPIC_228" "Medium-High" "discovery,service-registry,backend"

sleep 1

create_jira_ticket "AV11-233" "Story" \
    "Implement Cross-Chain Bridge Integration for Nodes" \
    "Connect nodes with cross-chain bridge service. Support Ethereum, Solana, transfer initiation, status tracking, and fee calculation." \
    13 "$EPIC_228" "Medium-High" "cross-chain,bridge,integration"

sleep 1

create_jira_ticket "AV11-234" "Story" \
    "Implement Node Security and Access Control" \
    "Add RBAC with JWT authentication, permission management, audit logging, rate limiting, and IP whitelisting. 4 roles: ADMIN, OPERATOR, MONITOR, CLIENT." \
    13 "$EPIC_228" "Highest" "security,rbac,authentication"

sleep 1

create_jira_ticket "AV11-235" "Story" \
    "Implement Node Backup and Recovery" \
    "Create backup and recovery mechanisms. Support automated backups, point-in-time recovery, snapshots, and disaster recovery procedures." \
    8 "$EPIC_228" "Medium" "backup,recovery,operations"

sleep 1

create_jira_ticket "AV11-236" "Story" \
    "Implement Node Logging and Diagnostics" \
    "Add comprehensive logging and diagnostic tools. Structured JSON logging, Elasticsearch integration, thread/heap dumps, profiling." \
    8 "$EPIC_228" "Medium" "logging,diagnostics,observability"

sleep 1

create_jira_ticket "AV11-237" "Story" \
    "Implement Node Resource Management" \
    "Add resource management and limits. CPU/memory limits, disk monitoring, bandwidth limiting, connection pooling, auto-scaling based on resources." \
    8 "$EPIC_228" "Medium" "resource-management,operations"

sleep 2

# ====================================
# SPRINT 18: Testing, Documentation & Deployment
# ====================================
echo ""
echo -e "${YELLOW}=== SPRINT 18: Testing, Documentation & Deployment ===${NC}"

EPIC_238=$(create_jira_ticket "AV11-238" "Epic" \
    "Sprint 18: Testing, Documentation & Deployment" \
    "Complete testing suite, documentation, and production deployment with CI/CD pipeline." \
    78 "" "High" "testing,documentation,deployment,production")

sleep 2

create_jira_ticket "AV11-239" "Story" \
    "Create Comprehensive Unit Test Suite" \
    "Write complete unit tests for all node components. Target: 95%+ line coverage, 90%+ branch coverage. Use JUnit 5, Mockito, AssertJ." \
    13 "$EPIC_238" "High" "testing,unit-tests,quality"

sleep 1

create_jira_ticket "AV11-240" "Story" \
    "Create Integration Test Suite" \
    "Write integration tests for node interactions. Test node-to-node, node-to-service, WebSocket, database, and external APIs." \
    13 "$EPIC_238" "High" "testing,integration-tests,quality"

sleep 1

create_jira_ticket "AV11-241" "Story" \
    "Create Performance Test Suite" \
    "Build performance and load testing suite. Target: 2M+ network TPS, 500K channel msg/sec, 200K validator TPS, 10K WebSocket connections." \
    13 "$EPIC_238" "High" "testing,performance,load-testing"

sleep 1

create_jira_ticket "AV11-242" "Story" \
    "Create API Documentation (OpenAPI/Swagger)" \
    "Generate comprehensive API documentation with OpenAPI 3.0, Swagger UI, examples, authentication guide, and error codes." \
    8 "$EPIC_238" "Medium-High" "documentation,api,openapi"

sleep 1

create_jira_ticket "AV11-243" "Story" \
    "Create User Guide and Tutorials" \
    "Write comprehensive user guide with Getting Started, deployment tutorials, configuration guide, troubleshooting, best practices, and FAQ." \
    8 "$EPIC_238" "Medium" "documentation,user-guide,tutorials"

sleep 1

create_jira_ticket "AV11-244" "Story" \
    "Create Architecture Documentation" \
    "Document system architecture with diagrams (system, component, sequence, data flow, deployment, security). Include ADRs for major decisions." \
    5 "$EPIC_238" "Medium" "documentation,architecture,adr"

sleep 1

create_jira_ticket "AV11-245" "Story" \
    "Create Docker Images and Compose Files" \
    "Build production Docker images with multi-stage builds. Target: <500MB image size. Include Docker Compose for dev and production." \
    8 "$EPIC_238" "High" "docker,containers,devops"

sleep 1

create_jira_ticket "AV11-246" "Story" \
    "Create Kubernetes Manifests and Helm Charts" \
    "Build Kubernetes deployment with HPA (2-20 replicas), rolling updates, health probes, and resource limits. Create installable Helm chart." \
    13 "$EPIC_238" "High" "kubernetes,helm,devops,deployment"

sleep 1

create_jira_ticket "AV11-247" "Story" \
    "Deploy to Production and Create CI/CD Pipeline" \
    "Create GitHub Actions CI/CD pipeline with automated testing, Docker builds, staging/production deployment, monitoring, and logging." \
    13 "$EPIC_238" "Highest" "cicd,deployment,production,github-actions"

sleep 1

# Summary
echo ""
echo "========================================="
echo "TICKET CREATION SUMMARY"
echo "========================================="
echo -e "${GREEN}Successfully created: ${CREATED_COUNT} tickets${NC}"
if [ $FAILED_COUNT -gt 0 ]; then
    echo -e "${RED}Failed to create: ${FAILED_COUNT} tickets${NC}"
fi
echo "========================================="
echo ""
echo "Next steps:"
echo "1. Verify tickets in JIRA: ${JIRA_BASE_URL}/browse/${JIRA_PROJECT_KEY}"
echo "2. Assign tickets to team members"
echo "3. Set sprint dates and start sprints"
echo "4. Update story points via JIRA UI if needed"
echo ""
