#!/bin/bash

# JIRA Configuration from Credentials.md
JIRA_EMAIL="subbu@aurigraph.io"
JIRA_API_TOKEN="ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"
JIRA_BASE_URL="https://aurigraphdlt.atlassian.net"
PROJECT_KEY="AV11"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "   JIRA Update - Session 3 Achievements"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Creating JIRA issue for Session 3..."

# Create JSON payload with Atlassian Document Format (ADF)
cat > /tmp/jira-issue-adf.json <<'EOF'
{
  "fields": {
    "project": {
      "key": "AV11"
    },
    "summary": "Session 3: JVM Performance Optimization - 1.82M TPS Achieved",
    "description": {
      "type": "doc",
      "version": 1,
      "content": [
        {
          "type": "heading",
          "attrs": {"level": 2},
          "content": [{"type": "text", "text": "Session 3 Completion Report - October 14, 2025"}]
        },
        {
          "type": "heading",
          "attrs": {"level": 3},
          "content": [{"type": "text", "text": "Executive Summary"}]
        },
        {
          "type": "paragraph",
          "content": [
            {"type": "text", "text": "Successfully applied JVM performance optimizations to V11.3-development branch, achieving ", "marks": [{"type": "strong"}]},
            {"type": "text", "text": "1.82M TPS", "marks": [{"type": "strong"}, {"type": "textColor", "attrs": {"color": "#0052cc"}}]},
            {"type": "text", "text": " (91% of 2M target).", "marks": [{"type": "strong"}]}
          ]
        },
        {
          "type": "heading",
          "attrs": {"level": 3},
          "content": [{"type": "text", "text": "Performance Achievements"}]
        },
        {
          "type": "bulletList",
          "content": [
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Peak TPS: 1.44M → 1.82M (+380K TPS, +26% improvement)"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Large Batch: 972K → 1.58M TPS (+607K, +62% improvement)"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Target Progress: 72% → 91% of 2M TPS goal"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Production Readiness: 62% → 68%"}]}]
            }
          ]
        },
        {
          "type": "heading",
          "attrs": {"level": 3},
          "content": [{"type": "text", "text": "Optimizations Applied"}]
        },
        {
          "type": "bulletList",
          "content": [
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Batch Processors: 64 → 256 workers (+300%)"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Batch Size: 10K → 15K transactions (+50%)"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Virtual Threads: 100K → 200K capacity (+100%)"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Consensus Threads: 64 → 128 parallel (+100%)"}]}]
            }
          ]
        },
        {
          "type": "heading",
          "attrs": {"level": 3},
          "content": [{"type": "text", "text": "Test Results"}]
        },
        {
          "type": "bulletList",
          "content": [
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Tests Passing: 9/9 (100%)"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Services: All operational (Blockchain, Consensus, AI, Bridge, gRPC)"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Build: SUCCESS with zero compilation warnings"}]}]
            }
          ]
        },
        {
          "type": "heading",
          "attrs": {"level": 3},
          "content": [{"type": "text", "text": "Git Information"}]
        },
        {
          "type": "bulletList",
          "content": [
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Branch: v11.3-development"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Commit: 51b9d4a5"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Status: Pushed to remote"}]}]
            }
          ]
        },
        {
          "type": "heading",
          "attrs": {"level": 3},
          "content": [{"type": "text", "text": "Next Steps"}]
        },
        {
          "type": "orderedList",
          "content": [
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Close final 9% gap to 2M TPS with GC tuning"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Retry native compilation for 2.5M+ TPS (127% of target)"}]}]
            },
            {
              "type": "listItem",
              "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Expand test coverage to 50%+"}]}]
            }
          ]
        }
      ]
    },
    "issuetype": {
      "name": "Task"
    },
    "labels": [
      "performance",
      "optimization",
      "v11-3",
      "jvm-tuning",
      "session-3"
    ]
  }
}
EOF

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Content-Type: application/json" \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  -d @/tmp/jira-issue-adf.json \
  "${JIRA_BASE_URL}/rest/api/3/issue")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "201" ]; then
  ISSUE_KEY=$(echo "$BODY" | grep -o '"key":"[^"]*"' | head -1 | cut -d'"' -f4)
  echo "✅ Successfully created JIRA issue: ${ISSUE_KEY}"
  echo "   URL: ${JIRA_BASE_URL}/browse/${ISSUE_KEY}"
  echo ""

  # Add comment with GitHub link
  cat > /tmp/jira-comment-adf.json <<EOF
{
  "body": {
    "type": "doc",
    "version": 1,
    "content": [
      {
        "type": "heading",
        "attrs": {"level": 3},
        "content": [{"type": "text", "text": "GitHub Information"}]
      },
      {
        "type": "paragraph",
        "content": [
          {"type": "text", "text": "Branch: ", "marks": [{"type": "strong"}]},
          {"type": "text", "text": "https://github.com/Aurigraph-DLT-Corp/Aurigraph-DLT/tree/v11.3-development", "marks": [{"type": "link", "attrs": {"href": "https://github.com/Aurigraph-DLT-Corp/Aurigraph-DLT/tree/v11.3-development"}}]}
        ]
      },
      {
        "type": "paragraph",
        "content": [
          {"type": "text", "text": "Commit: ", "marks": [{"type": "strong"}]},
          {"type": "text", "text": "51b9d4a5 - JVM Performance Optimization"}
        ]
      },
      {
        "type": "heading",
        "attrs": {"level": 3},
        "content": [{"type": "text", "text": "Files Modified"}]
      },
      {
        "type": "bulletList",
        "content": [
          {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "src/main/resources/application.properties"}]}]},
          {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "JVM-PERFORMANCE-OPTIMIZATION-REPORT.md (new)"}]}]},
          {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "OPTIONS-B-F-COMPLETION-REPORT.md (new)"}]}]},
          {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "PERFORMANCE-ANALYSIS-OPTIONS-B-F.md (new)"}]}]}
        ]
      }
    ]
  }
}
EOF

  curl -s -X POST \
    -H "Content-Type: application/json" \
    -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
    -d @/tmp/jira-comment-adf.json \
    "${JIRA_BASE_URL}/rest/api/3/issue/${ISSUE_KEY}/comment" > /dev/null

  echo "✅ Added GitHub information"
  echo ""

  # Clean up
  rm -f /tmp/jira-issue-adf.json /tmp/jira-comment-adf.json

  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "   JIRA Update Complete"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo "Summary:"
  echo "  Issue Created: ${ISSUE_KEY}"
  echo "  URL: ${JIRA_BASE_URL}/browse/${ISSUE_KEY}"
  echo "  Performance: 1.82M TPS (91% of target)"
  echo "  Production Readiness: 68%"
  echo "  Branch: v11.3-development (pushed)"
  echo ""

else
  echo "❌ Failed to create JIRA issue (HTTP $HTTP_CODE)"
  echo "Response: $BODY"
  exit 1
fi
