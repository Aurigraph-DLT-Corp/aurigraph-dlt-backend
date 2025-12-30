#!/bin/bash

# Check for existing JIRA tickets in AV11-400 to AV11-423 range

JIRA_AUTH="subbu@aurigraph.io:ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"

echo "Checking for existing tickets AV11-400 to AV11-423..."
curl -s -u "$JIRA_AUTH" \
  'https://aurigraphdlt.atlassian.net/rest/api/3/search?jql=project=AV11+AND+key>=AV11-400+AND+key<=AV11-423&fields=key,summary,status&maxResults=50' | \
  jq -r '.issues[] | "\(.key): \(.fields.summary) [\(.fields.status.name)]"'

echo ""
echo "Total existing tickets:"
curl -s -u "$JIRA_AUTH" \
  'https://aurigraphdlt.atlassian.net/rest/api/3/search?jql=project=AV11+AND+key>=AV11-400+AND+key<=AV11-423&fields=key&maxResults=1' | \
  jq -r '.total'
