#!/bin/bash

# Create JIRA tickets for Enterprise Portal Sprint work
# AV11-400 through AV11-423

JIRA_AUTH="subbu@aurigraph.io:ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"
JIRA_URL="https://aurigraphdlt.atlassian.net"

# Function to create a JIRA ticket
create_ticket() {
    local summary="$1"
    local description="$2"
    local story_points="$3"
    local priority="$4"

    curl -s -u "$JIRA_AUTH" \
        -X POST \
        -H "Content-Type: application/json" \
        "$JIRA_URL/rest/api/3/issue" \
        -d "{
            \"fields\": {
                \"project\": {\"key\": \"AV11\"},
                \"summary\": \"$summary\",
                \"description\": {
                    \"type\": \"doc\",
                    \"version\": 1,
                    \"content\": [{
                        \"type\": \"paragraph\",
                        \"content\": [{
                            \"type\": \"text\",
                            \"text\": \"$description\"
                        }]
                    }]
                },
                \"issuetype\": {\"name\": \"Task\"},
                \"priority\": {\"name\": \"$priority\"}
            }
        }" | jq -r '.key // "ERROR"'
}

echo "Creating JIRA tickets for Enterprise Portal work..."
echo ""

# Sprint 1 - Critical Data Integration
echo "=== Sprint 1: Critical Data Integration ==="
create_ticket "Replace Dashboard dummy data with real APIs" "Connect Dashboard.tsx to real backend APIs: /api/v11/stats, /api/v11/performance, /api/v11/system/status, /api/v11/contracts/statistics. Remove all Math.random() and simulated data. Status: COMPLETED" "3" "High"
sleep 1

create_ticket "Connect Transactions page to real blockchain" "Integrate Transactions.tsx with /api/v11/blockchain/transactions API. Implement pagination, real-time updates via WebSocket, and advanced filtering. Status: COMPLETED" "5" "High"
sleep 1

create_ticket "Replace Performance page with real system metrics" "Connect Performance.tsx to /api/v11/analytics/performance for real metrics. Remove all simulated performance data." "5" "High"
sleep 1

create_ticket "Connect Node Management to real validator APIs" "Integrate NodeManagement.tsx with /api/v11/blockchain/validators and /api/v11/live/validators for real-time validator status." "5" "High"
sleep 1

create_ticket "Replace Analytics page dummy data with real metrics" "Connect Analytics.tsx to /api/v11/analytics/dashboard. Replace TPS charts and transaction breakdown with real data." "3" "High"
sleep 1

echo ""
echo "=== Sprint 2: Dashboard Integration ==="
create_ticket "Integrate System Health with real monitoring APIs" "Connect SystemHealth.tsx to /q/health and /api/v11/analytics/performance for real system metrics." "3" "Medium"
sleep 1

create_ticket "Connect Blockchain Operations to real blockchain APIs" "Integrate BlockchainOperations.tsx with /api/v11/blockchain/network/stats and /api/v11/blockchain/blocks/latest." "4" "Medium"
sleep 1

create_ticket "Replace Consensus Monitoring dummy data with real consensus state" "Connect ConsensusMonitoring.tsx to /api/v11/consensus/status and /api/v11/live/consensus for HyperRAFT++ state." "4" "Medium"
sleep 1

create_ticket "Integrate External API dashboard with real oracle services" "Connect ExternalAPIIntegration.tsx to /api/v11/oracles/status and /api/v11/datafeeds." "3" "Medium"
sleep 1

create_ticket "Connect Oracle Service dashboard to real oracle APIs" "Integrate OracleService.tsx with /api/v11/oracles/status and /api/v11/datafeeds/prices." "2" "Medium"
sleep 1

create_ticket "Replace Performance Metrics with real system data" "Connect PerformanceMetrics.tsx to /api/v11/analytics/performance for CPU, memory, network I/O." "2" "Medium"
sleep 1

echo ""
echo "=== Sprint 3: RWA & Security Integration ==="
create_ticket "Integrate Security Audit with real security APIs" "Connect SecurityAudit.tsx to /api/v11/security/status, /api/v11/security/quantum, /api/v11/security/hsm/status." "3" "Medium"
sleep 1

create_ticket "Connect Developer Dashboard to real API metrics" "Integrate DeveloperDashboard.tsx with /api/v11/info for system information and API metrics." "2" "Medium"
sleep 1

create_ticket "Connect Ricardian Contracts to real contract APIs" "Integrate RicardianContracts.tsx with /api/v11/contracts/ricardian endpoints." "3" "Medium"
sleep 1

create_ticket "Implement missing RWA pages and routes" "Create 5 RWA pages: TokenizeAsset, Portfolio, Valuation, Dividends, Compliance. Add routes to App.tsx. Status: COMPLETED" "8" "Medium"
sleep 1

echo ""
echo "=== Sprint 4: WebSocket & Real-Time ==="
create_ticket "Fix nginx WebSocket proxy for real-time updates" "Configure nginx to support WebSocket connections for /ws/* endpoints. Disable HTTP/2 for WebSocket paths." "5" "High"
sleep 1

create_ticket "Integrate WebSocket real-time updates across all dashboards" "Add WebSocket real-time updates to Dashboard, Performance, Analytics, NodeManagement, and other pages." "8" "Medium"
sleep 1

echo ""
echo "=== Sprint 5: Testing & Verification ==="
create_ticket "End-to-end testing of all 29 Enterprise Portal routes" "Test every route in App.tsx for functionality, data loading, error handling, and user interactions." "13" "High"
sleep 1

create_ticket "Comprehensive API integration testing for all 200+ endpoints" "Test all backend API endpoints for response structure, data consistency, and error handling." "8" "Medium"
sleep 1

echo ""
echo "=== Sprint 6: Documentation ==="
create_ticket "Create comprehensive bug/fix report" "Document all issues found, fixes implemented, root cause analysis, and lessons learned." "3" "Low"
sleep 1

create_ticket "Update API integration documentation" "Create API-INTEGRATION-GUIDE-V2.md with complete endpoint list and integration examples." "3" "Low"
sleep 1

create_ticket "Create Enterprise Portal user guide" "Write ENTERPRISE_PORTAL_USER_GUIDE.md with feature overview and troubleshooting." "2" "Low"
sleep 1

create_ticket "Update JIRA with all progress and completion status" "Update all created tickets with evidence, completion status, and link to commits." "2" "Low"
sleep 1

echo ""
echo "=== Backend Track ==="
create_ticket "Audit and document all 200+ backend API endpoints" "Comprehensive backend API audit with endpoint inventory, schema documentation, and performance metrics." "13" "Medium"
sleep 1

create_ticket "Implement missing backend APIs for full portal functionality" "Identify and implement any missing backend APIs required for complete portal integration." "8" "Medium"
sleep 1

echo ""
echo "JIRA ticket creation complete!"
