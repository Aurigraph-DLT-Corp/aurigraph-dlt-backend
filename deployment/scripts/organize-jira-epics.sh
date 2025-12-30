#!/bin/bash

JIRA_USER="subbu@aurigraph.io"
JIRA_TOKEN="ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"
JIRA_URL="https://aurigraphdlt.atlassian.net"

echo "=========================================="
echo "Organizing JIRA Tasks into Epics"
echo "=========================================="
echo ""

# Function to link task to epic
link_task_to_epic() {
  local TASK_KEY="$1"
  local EPIC_KEY="$2"
  local TASK_DESC="$3"

  echo "Linking $TASK_KEY to Epic $EPIC_KEY: $TASK_DESC"

  RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT \
    -u "${JIRA_USER}:${JIRA_TOKEN}" \
    -H "Content-Type: application/json" \
    "${JIRA_URL}/rest/api/3/issue/${TASK_KEY}" \
    -d "{
      \"fields\": {
        \"parent\": {
          \"key\": \"${EPIC_KEY}\"
        }
      }
    }")

  HTTP_CODE=$(echo "$RESPONSE" | tail -1)

  if [ "$HTTP_CODE" -eq 204 ]; then
    echo "  ✅ Successfully linked $TASK_KEY to $EPIC_KEY"
  else
    echo "  ❌ Failed to link $TASK_KEY to $EPIC_KEY (HTTP $HTTP_CODE)"
    echo "$RESPONSE" | head -n -1 | jq '.' 2>/dev/null || echo "$RESPONSE" | head -n -1
  fi
  echo ""
}

echo "=== Linking Sprint 6 Tasks to AV11-146 ==="
echo ""
link_task_to_epic "AV11-161" "AV11-146" "Sprint 6.7: Production Monitoring Dashboards"
link_task_to_epic "AV11-160" "AV11-146" "Sprint 6.6: Load Testing & Benchmarking"
link_task_to_epic "AV11-159" "AV11-146" "Sprint 6.5: Comprehensive Security Audit"
link_task_to_epic "AV11-158" "AV11-146" "Sprint 6.3: Achieve 50% Test Coverage"
link_task_to_epic "AV11-157" "AV11-146" "Sprint 6.2: Re-enable 34 Disabled Tests"
link_task_to_epic "AV11-147" "AV11-146" "Sprint 6.4: Performance Optimization to 1M+ TPS"

echo "=== Linking Sprint 5 Tasks to AV11-143 ==="
echo ""
link_task_to_epic "AV11-145" "AV11-143" "Sprint 5.2: Production Server Deployment"
link_task_to_epic "AV11-144" "AV11-143" "Sprint 5.1: Build and Package Application"

echo "=== Linking Sprint 4 Tasks to AV11-138 ==="
echo ""
link_task_to_epic "AV11-142" "AV11-138" "Sprint 4.4: Post-Quantum Cryptography"
link_task_to_epic "AV11-141" "AV11-138" "Sprint 4.3: gRPC Network Layer"
link_task_to_epic "AV11-140" "AV11-138" "Sprint 4.2: AI/ML Optimization Services"
link_task_to_epic "AV11-139" "AV11-138" "Sprint 4.1: HyperRAFT++ Consensus Implementation"

echo "=== Linking Sprint 1-3 Tasks to AV11-148 ==="
echo ""
link_task_to_epic "AV11-156" "AV11-148" "Sprint 2-4: Virtual Threads, AI Optimization, Test Infrastructure"
link_task_to_epic "AV11-155" "AV11-148" "GraalVM Native Build Configuration - Technical Debt Resolution"
link_task_to_epic "AV11-154" "AV11-148" "Production Deployment v2.0.0 to dlt.aurigraph.io"
link_task_to_epic "AV11-153" "AV11-148" "Infrastructure as Code - Terraform Implementation"
link_task_to_epic "AV11-151" "AV11-148" "Sprint 3: Docker & Infrastructure"
link_task_to_epic "AV11-150" "AV11-148" "Sprint 2: AI/ML Agents & Performance"
link_task_to_epic "AV11-149" "AV11-148" "Sprint 1: Initial Project Setup & Security"

echo "=== Linking Enterprise Portal Tasks to AV11-137 ==="
echo ""
# Link UI implementation tasks (T008-T038)
for i in {106..136}; do
  link_task_to_epic "AV11-${i}" "AV11-137" "Enterprise Portal Task"
done

echo "=========================================="
echo "✅ JIRA Epic Organization Complete!"
echo "=========================================="
echo ""
echo "Summary:"
echo "- Sprint 6 tasks linked to AV11-146"
echo "- Sprint 5 tasks linked to AV11-143"
echo "- Sprint 4 tasks linked to AV11-138"
echo "- Sprint 1-3 tasks linked to AV11-148"
echo "- Enterprise Portal tasks (31 tasks) linked to AV11-137"
echo ""
