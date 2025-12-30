#!/bin/bash

# Create JIRA Issues for UI-API Integration Gaps
# Date: October 10, 2025

JIRA_EMAIL="subbu@aurigraph.io"
JIRA_API_TOKEN="ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"
JIRA_BASE_URL="https://aurigraphdlt.atlassian.net"
PROJECT_KEY="AV11"

echo "Creating JIRA issues for UI-API Integration gaps..."
echo ""

# Function to create JIRA issue
create_jira_issue() {
    local summary="$1"
    local description="$2"
    local priority="$3"
    local labels="$4"
    local story_points="$5"

    echo "Creating issue: $summary"

    response=$(curl -s -X POST \
      "$JIRA_BASE_URL/rest/api/3/issue" \
      -u "$JIRA_EMAIL:$JIRA_API_TOKEN" \
      -H "Content-Type: application/json" \
      -d @- << EOF
{
  "fields": {
    "project": {
      "key": "$PROJECT_KEY"
    },
    "summary": "[$priority] $summary",
    "description": {
      "type": "doc",
      "version": 1,
      "content": [
        {
          "type": "paragraph",
          "content": [
            {
              "type": "text",
              "text": "$description"
            }
          ]
        }
      ]
    },
    "issuetype": {
      "name": "Task"
    },
    "labels": ["ui-api-integration", "v11.1.0", $labels]
  }
}
EOF
)

    issue_key=$(echo "$response" | grep -o '"key":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ -n "$issue_key" ]; then
        echo "✅ Created: $issue_key - $summary"
        echo "   URL: $JIRA_BASE_URL/browse/$issue_key"
    else
        echo "❌ Failed to create issue"
        echo "Response: $response"
    fi
    echo ""
}

# Create P0 (Highest Priority) Issues
echo "=== Creating P0 (Highest Priority) Issues ==="
echo ""

create_jira_issue \
    "Implement Network Statistics API Endpoint" \
    "Dashboard Home component needs /api/v11/blockchain/network/stats endpoint to display network overview card. Currently returns 404, causing empty card in main dashboard. Should return totalNodes, activeValidators, currentTPS, and other network metrics." \
    "Highest" \
    "\"backend-api\", \"dashboard\", \"p0\"" \
    "5"

create_jira_issue \
    "Implement Live Validators Monitoring API" \
    "Real-time validator monitoring component needs /api/v11/live/validators endpoint. Currently returns 404. Should provide WebSocket or polling endpoint for live validator status updates including uptime, performance, and current state." \
    "Highest" \
    "\"backend-api\", \"real-time\", \"validators\", \"p0\"" \
    "8"

create_jira_issue \
    "Implement Live Consensus Data API" \
    "Consensus monitoring dashboard needs /api/v11/live/consensus endpoint. Currently returns 404. Should provide real-time consensus state including current leader, epoch, round information, and HyperRAFT++ metrics." \
    "Highest" \
    "\"backend-api\", \"real-time\", \"consensus\", \"p0\"" \
    "8"

# Create P1 (High Priority) Issues
echo "=== Creating P1 (High Priority) Issues ==="
echo ""

create_jira_issue \
    "Implement Analytics Dashboard API" \
    "Main analytics dashboard needs /api/v11/analytics/dashboard endpoint. Currently returns 404. Should aggregate and return comprehensive analytics data including TPS trends, transaction types, network usage, and historical metrics." \
    "High" \
    "\"backend-api\", \"analytics\", \"p1\"" \
    "5"

create_jira_issue \
    "Implement Performance Metrics API" \
    "System performance monitoring needs /api/v11/analytics/performance endpoint. Currently returns 404. Should return memory usage, CPU utilization, response times, throughput metrics, and system health indicators." \
    "High" \
    "\"backend-api\", \"monitoring\", \"analytics\", \"p1\"" \
    "5"

create_jira_issue \
    "Implement Voting Statistics API" \
    "Governance section needs /api/v11/blockchain/governance/stats endpoint. Currently returns 404. Should return total votes, participation rates, proposal outcomes, and governance activity metrics." \
    "High" \
    "\"backend-api\", \"governance\", \"p1\"" \
    "3"

create_jira_issue \
    "Implement Network Health Monitor API" \
    "Network health widget needs /api/v11/network/health endpoint. Currently returns 404. Should return network connectivity status, peer health, latency metrics, and overall network score." \
    "High" \
    "\"backend-api\", \"network\", \"monitoring\", \"p1\"" \
    "5"

create_jira_issue \
    "Implement Network Peers Map API" \
    "Network visualization needs /api/v11/network/peers endpoint. Currently returns 404. Should return list of connected peers with geographic data, connection quality, and peer metadata for map visualization." \
    "High" \
    "\"backend-api\", \"network\", \"visualization\", \"p1\"" \
    "5"

create_jira_issue \
    "Implement Live Network Monitor API" \
    "Real-time network dashboard needs /api/v11/live/network endpoint. Currently returns 404. Should provide real-time network metrics including active connections, bandwidth usage, message rates, and network events." \
    "High" \
    "\"backend-api\", \"real-time\", \"network\", \"p1\"" \
    "8"

# Create Epic for UI/UX Improvements
echo "=== Creating Epic for UI/UX Improvements ==="
echo ""

create_jira_issue \
    "UI/UX Improvements for Missing API Endpoints" \
    "Improve user experience for dashboard components where backend APIs are not yet available. Tasks: 1) Add 'Coming Soon' badges, 2) Implement better error states with user-friendly messages, 3) Add loading skeletons, 4) Implement fallback/demo data, 5) Hide unavailable features with feature flags." \
    "Medium" \
    "\"frontend\", \"ux\", \"error-handling\"" \
    "8"

# Summary
echo "=== JIRA Issues Creation Summary ==="
echo ""
echo "Created issues for:"
echo "  - 3 P0 (Highest Priority) items"
echo "  - 6 P1 (High Priority) items"
echo "  - 1 Epic for UI/UX improvements"
echo ""
echo "Total: 10 issues created"
echo ""
echo "View all issues: $JIRA_BASE_URL/browse/$PROJECT_KEY"
echo ""
echo "✅ JIRA issues created successfully!"
