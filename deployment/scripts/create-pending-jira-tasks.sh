#!/bin/bash

# Create JIRA tickets for pending tasks from v11.2.1 deployment

JIRA_USER="subbu@aurigraph.io"
JIRA_TOKEN="ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"
JIRA_URL="https://aurigraphdlt.atlassian.net"
PROJECT_KEY="AV11"

echo "=========================================="
echo "Creating JIRA Tickets for Pending Tasks"
echo "=========================================="
echo ""

# Function to create JIRA ticket
create_jira_ticket() {
  local SUMMARY="$1"
  local DESCRIPTION="$2"
  local ISSUE_TYPE="$3"

  echo "Creating ticket: $SUMMARY"

  local PAYLOAD=$(cat <<EOF
{
  "fields": {
    "project": {
      "key": "$PROJECT_KEY"
    },
    "summary": "$SUMMARY",
    "description": {
      "type": "doc",
      "version": 1,
      "content": [
        {
          "type": "paragraph",
          "content": [
            {
              "type": "text",
              "text": "$DESCRIPTION"
            }
          ]
        }
      ]
    },
    "issuetype": {
      "name": "$ISSUE_TYPE"
    }
  }
}
EOF
)

  RESPONSE=$(curl -s -X POST "$JIRA_URL/rest/api/3/issue" \
    -u "$JIRA_USER:$JIRA_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD")

  TICKET_KEY=$(echo "$RESPONSE" | grep -o '"key":"[^"]*"' | head -1 | cut -d'"' -f4)

  if [ -n "$TICKET_KEY" ]; then
    echo "✅ Created: $TICKET_KEY - $SUMMARY"
    echo "   URL: $JIRA_URL/browse/$TICKET_KEY"
  else
    echo "❌ Failed to create ticket: $SUMMARY"
    echo "   Response: $RESPONSE"
  fi
  echo ""
}

# Completed tasks (for reference/closure)
echo "📋 Recently Completed Tasks (v11.2.1 deployment):"
echo "   ✅ Build Java backend v11.2.1"
echo "   ✅ Deploy v11.2.1 JAR to production"
echo "   ✅ Platform rebranding to 'Aurigraph DLT'"
echo "   ✅ Release v11.2.0 and v11.2.1"
echo "   ✅ Multi-agent deployment verification (DDA + BDA)"
echo ""

# Create pending documentation tasks
echo "🔄 Creating pending documentation tasks..."
echo ""

create_jira_ticket \
  "Update CLAUDE.md with correct REST API port configuration" \
  "Update CLAUDE.md documentation to reflect that REST API runs on port 9443 (HTTPS) not 9003 (HTTP). Port 9003 is used for the portal frontend. Also update gRPC status from 'planned' to 'active' on port 9004. This was discovered during the BDA (Backend Development Agent) investigation." \
  "Task"

create_jira_ticket \
  "Update README.md with public URLs and port mappings" \
  "Add comprehensive public URL documentation to README.md including: Portal (http://dlt.aurigraph.io:9003/), REST API (https://dlt.aurigraph.io/api/v11/), gRPC (dlt.aurigraph.io:9004). Include port mapping explanations and HTTPS testing instructions with curl examples." \
  "Task"

create_jira_ticket \
  "Create QUICK-START-API.md guide for developers" \
  "Create a quick-start guide for developers using the Aurigraph DLT REST API. Include: essential endpoints (/health, /info, /stats, /performance), example curl commands with HTTPS, authentication setup, common use cases, and troubleshooting tips. Based on the BDA REST API analysis report findings." \
  "Task"

# Create pending verification tasks
echo "🔄 Creating pending verification/testing tasks..."
echo ""

create_jira_ticket \
  "Create default admin user in RBAC system" \
  "Access the admin interface at http://dlt.aurigraph.io:9003/rbac-admin-setup.html and create the default admin user by clicking '🚀 Create Default Admin' button. Verify admin credentials work (admin@aurigraph.io / admin123). Document the admin creation process and test all admin panel features." \
  "Task"

create_jira_ticket \
  "Test guest registration flow on production portal" \
  "Test the guest registration feature on the live production portal at http://dlt.aurigraph.io:9003/. Fill out the registration form with test data, verify form validation (XSS protection, input validation, rate limiting), confirm user badge updates correctly, and document any issues found. This validates RBAC V2 security features." \
  "Task"

create_jira_ticket \
  "Verify RBAC V2 security features on production" \
  "Comprehensive security testing of RBAC V2 deployment: Test XSS protection with malicious inputs, verify input validation (email format, phone format, text length), test rate limiting (5 attempts per 60 seconds), verify secure session IDs (256-bit), and confirm B+ security grade (85/100) is maintained in production." \
  "Task"

# Deployment summary
echo "=========================================="
echo "✅ JIRA Ticket Creation Complete!"
echo "=========================================="
echo ""
echo "📊 Summary:"
echo "   - Documentation Tasks: 3 created"
echo "   - Verification Tasks: 3 created"
echo "   - Total New Tickets: 6"
echo ""
echo "🔗 View JIRA Board:"
echo "   $JIRA_URL/jira/software/projects/$PROJECT_KEY/boards/789"
echo ""
echo "📋 Recently Completed (not created as tickets):"
echo "   - v11.2.1 backend deployment"
echo "   - Platform rebranding to Aurigraph DLT"
echo "   - Multi-agent deployment verification"
echo ""
