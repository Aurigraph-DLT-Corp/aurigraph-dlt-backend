#!/bin/bash

JIRA_USER="subbu@aurigraph.io"
JIRA_TOKEN="ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"
JIRA_URL="https://aurigraphdlt.atlassian.net"

echo "=========================================="
echo "Updating V3.6 JIRA Tasks to Done Status"
echo "=========================================="
echo ""

# Transition ID 31 = Done
DONE_TRANSITION="31"

# Function to update task status
update_task_status() {
  local TASK_KEY="$1"
  local TASK_DESC="$2"

  echo "Updating $TASK_KEY: $TASK_DESC"

  RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -u "${JIRA_USER}:${JIRA_TOKEN}" \
    -H "Content-Type: application/json" \
    "${JIRA_URL}/rest/api/3/issue/${TASK_KEY}/transitions" \
    -d "{
      \"transition\": {
        \"id\": \"${DONE_TRANSITION}\"
      }
    }")

  HTTP_CODE=$(echo "$RESPONSE" | tail -1)

  if [ "$HTTP_CODE" -eq 204 ]; then
    echo "  ✅ Successfully updated $TASK_KEY to Done"
  else
    echo "  ❌ Failed to update $TASK_KEY (HTTP $HTTP_CODE)"
    echo "$RESPONSE" | head -n -1
  fi
  echo ""
}

# Update completed tasks
update_task_status "AV11-164" "Upgrade Quarkus to 3.28.2"
update_task_status "AV11-165" "Implement Validator Node Docker Image"
update_task_status "AV11-166" "Implement Business Node Docker Image"
update_task_status "AV11-167" "Implement Slim Node with OAuth2 Integration"
update_task_status "AV11-168" "Create Node-Specific Quarkus Profiles"
update_task_status "AV11-169" "Docker Compose Cluster Orchestration"
update_task_status "AV11-172" "Update PRD Documentation to V3.6"

echo "=========================================="
echo "✅ V3.6 Task Status Update Complete!"
echo "=========================================="
echo ""
echo "Completed: AV11-164, AV11-165, AV11-166, AV11-167, AV11-168, AV11-169, AV11-172"
echo "In Progress: AV11-170 (GraalVM Native Image Optimization)"
echo "Pending: AV11-171 (Production Deployment), AV11-173 (Health Monitoring)"
echo ""
