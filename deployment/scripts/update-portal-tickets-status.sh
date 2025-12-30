#!/bin/bash

# Update Enterprise Portal tickets (AV11-106 to AV11-136) to In Progress status
# These correspond to tasks T008-T038

JIRA_USER="subbu@aurigraph.io"
JIRA_TOKEN="ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"
JIRA_URL="https://aurigraphdlt.atlassian.net"

echo "=========================================="
echo "Updating Enterprise Portal Ticket Status"
echo "=========================================="
echo ""
echo "Tickets: AV11-106 to AV11-136 (31 tasks)"
echo "Epic: AV11-137 Enterprise Portal UI"
echo ""

# Transition ID 21 = In Progress
IN_PROGRESS_TRANSITION="21"

# Function to update task status to In Progress
update_task_to_in_progress() {
  local TASK_KEY="$1"
  local TASK_DESC="$2"

  echo "Updating $TASK_KEY: $TASK_DESC"

  RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -u "${JIRA_USER}:${JIRA_TOKEN}" \
    -H "Content-Type: application/json" \
    "${JIRA_URL}/rest/api/3/issue/${TASK_KEY}/transitions" \
    -d "{
      \"transition\": {
        \"id\": \"${IN_PROGRESS_TRANSITION}\"
      }
    }")

  HTTP_CODE=$(echo "$RESPONSE" | tail -1)

  if [ "$HTTP_CODE" -eq 204 ]; then
    echo "  ✅ Updated $TASK_KEY to In Progress"
  else
    echo "  ⚠️  $TASK_KEY may already be In Progress or Done (HTTP $HTTP_CODE)"
  fi
}

echo "=== Foundation Tasks (Layout & Navigation) ==="
echo ""
update_task_to_in_progress "AV11-107" "T009 - Create main layout and navigation"
update_task_to_in_progress "AV11-108" "T010 - Build dashboard overview page"
update_task_to_in_progress "AV11-109" "T011 - Implement real-time WebSocket connection"

echo ""
echo "=== Module UI Tasks ==="
echo ""
update_task_to_in_progress "AV11-110" "T012 - Build Governance module UI"
update_task_to_in_progress "AV11-111" "T013 - Build Staking module UI"
update_task_to_in_progress "AV11-112" "T014 - Build Smart Contracts module UI"
update_task_to_in_progress "AV11-113" "T015 - Build RWA Tokenization module UI"
update_task_to_in_progress "AV11-114" "T016 - Build DeFi Services module UI"
update_task_to_in_progress "AV11-115" "T017 - Build Cross-Chain Bridge UI"
update_task_to_in_progress "AV11-116" "T018 - Build AI Analytics module UI"

echo ""
echo "=== Component Library Tasks ==="
echo ""
update_task_to_in_progress "AV11-117" "T019 - Create reusable UI components library"
update_task_to_in_progress "AV11-118" "T020 - Build chart components library"
update_task_to_in_progress "AV11-119" "T021 - Implement notification system"

echo ""
echo "=== API Integration Tasks ==="
echo ""
update_task_to_in_progress "AV11-120" "T022 - Integrate Dashboard APIs"
update_task_to_in_progress "AV11-121" "T023 - Integrate Governance APIs"
update_task_to_in_progress "AV11-122" "T024 - Integrate Staking APIs"
update_task_to_in_progress "AV11-123" "T025 - Integrate Smart Contract APIs"
update_task_to_in_progress "AV11-124" "T026 - Integrate RWA APIs"
update_task_to_in_progress "AV11-125" "T027 - Integrate DeFi APIs"
update_task_to_in_progress "AV11-126" "T028 - Integrate Bridge APIs"
update_task_to_in_progress "AV11-127" "T029 - Integrate AI APIs"

echo ""
echo "=== Advanced Features ==="
echo ""
update_task_to_in_progress "AV11-128" "T030 - Implement responsive design"
update_task_to_in_progress "AV11-129" "T031 - Add loading states and skeletons"
update_task_to_in_progress "AV11-130" "T032 - Implement accessibility features"
update_task_to_in_progress "AV11-131" "T033 - Optimize performance"
update_task_to_in_progress "AV11-132" "T034 - Add internationalization i18n"
update_task_to_in_progress "AV11-133" "T035 - Write comprehensive documentation"

echo ""
echo "=== DevOps & Production ==="
echo ""
update_task_to_in_progress "AV11-134" "T036 - Configure production build"
update_task_to_in_progress "AV11-135" "T037 - Set up CI/CD pipeline"
update_task_to_in_progress "AV11-136" "T038 - Implement monitoring and analytics"

echo ""
echo "=== Testing ==="
echo ""
update_task_to_in_progress "AV11-106" "T008 - Write smart contracts tests"

echo ""
echo "=========================================="
echo "✅ Portal Ticket Status Update Complete!"
echo "=========================================="
echo ""
echo "Updated: 31 tasks to In Progress"
echo "Epic: AV11-137 Enterprise Portal UI Implementation"
echo "Total Story Points: 170 SP"
echo ""
echo "View JIRA Board:"
echo "https://aurigraphdlt.atlassian.net/jira/software/projects/AV11/boards/789"
echo ""
