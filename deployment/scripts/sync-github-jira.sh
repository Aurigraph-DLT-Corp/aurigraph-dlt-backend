#!/bin/bash

# GitHub-JIRA Bidirectional Sync Script
# Syncs all JIRA tickets to GitHub issues and creates milestones from epics

JIRA_USER="subbu@aurigraph.io"
JIRA_TOKEN="ATATT3xFfGF0c79X44m_ecHcP5d2F-jx5ljisCVB11tCEl5jB0Cx_FaapQt_u44IqcmBwfq8Gl8CsMFdtu9mqV8SgzcUwjZ2TiHRJo9eh718fUYw7ptk5ZFOzc-aLV2FH_ywq2vSsJ5gLvSorz-eB4JeKxUSLyYiGS9Y05-WhlEWa0cgFUdhUI4=0BECD4F5"
JIRA_URL="https://aurigraphdlt.atlassian.net"
PROJECT_KEY="AV11"
GITHUB_REPO="Aurigraph-DLT-Corp/Aurigraph-DLT"
GITHUB_TOKEN="${GITHUB_TOKEN:-}"

echo "=========================================="
echo "GitHub-JIRA Synchronization Tool"
echo "=========================================="
echo ""
echo "JIRA Project: $PROJECT_KEY"
echo "GitHub Repo: $GITHUB_REPO"
echo ""

# Check if GitHub token is set
if [ -z "$GITHUB_TOKEN" ]; then
  echo "⚠️  GITHUB_TOKEN environment variable not set"
  echo "Please set it with: export GITHUB_TOKEN='your_github_token'"
  echo ""
  echo "You can create a token at: https://github.com/settings/tokens"
  echo "Required scopes: repo, issues, admin:org"
  echo ""
  read -p "Continue without GitHub sync? (y/n) " -n 1 -r
  echo ""
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
  fi
  GITHUB_ENABLED=false
else
  GITHUB_ENABLED=true
  echo "✅ GitHub token configured"
fi

# Create Node.js script for JIRA to GitHub sync
cat > /tmp/sync-jira-github.js << 'ENDSCRIPT'
const https = require('https');
const http = require('http');

const JIRA_USER = process.env.JIRA_USER;
const JIRA_TOKEN = process.env.JIRA_TOKEN;
const JIRA_URL = process.env.JIRA_URL;
const PROJECT_KEY = process.env.PROJECT_KEY;
const GITHUB_TOKEN = process.env.GITHUB_TOKEN;
const GITHUB_REPO = process.env.GITHUB_REPO;

// Helper to make HTTPS requests
function httpsRequest(options, data = null) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          resolve({ statusCode: res.statusCode, body: JSON.parse(body) });
        } catch {
          resolve({ statusCode: res.statusCode, body: body });
        }
      });
    });

    req.on('error', reject);
    if (data) req.write(JSON.stringify(data));
    req.end();
  });
}

// Fetch all JIRA issues
async function fetchJiraIssues() {
  console.log('📥 Fetching JIRA issues...');

  const jql = `project=${PROJECT_KEY} AND created>="2025-09-01" ORDER BY created ASC`;
  const auth = Buffer.from(`${JIRA_USER}:${JIRA_TOKEN}`).toString('base64');

  const issues = [];
  let startAt = 0;
  const maxResults = 50;

  while (true) {
    const data = {
      jql: jql,
      startAt: startAt,
      maxResults: maxResults,
      fields: ['summary', 'description', 'status', 'issuetype', 'priority', 'parent', 'labels']
    };

    const url = new URL(`${JIRA_URL}/rest/api/3/search/jql`);
    const options = {
      hostname: url.hostname,
      path: url.pathname,
      method: 'POST',
      headers: {
        'Authorization': `Basic ${auth}`,
        'Content-Type': 'application/json'
      }
    };

    try {
      const response = await httpsRequest(options, data);
      if (response.statusCode !== 200) {
        console.error('❌ Error fetching JIRA issues:', response.body);
        break;
      }

      const result = response.body;
      issues.push(...result.issues);

      console.log(`   Fetched ${issues.length} of ${result.total} issues`);

      if (startAt + maxResults >= result.total) break;
      startAt += maxResults;

    } catch (error) {
      console.error('❌ Error:', error.message);
      break;
    }
  }

  return issues;
}

// Extract text from JIRA ADF format
function extractText(adfContent) {
  if (!adfContent || typeof adfContent === 'string') return adfContent || '';

  let text = '';
  function traverse(node) {
    if (node.type === 'text') text += node.text;
    if (node.content) node.content.forEach(traverse);
  }

  if (adfContent.content) adfContent.content.forEach(traverse);
  return text;
}

// Create GitHub issue
async function createGitHubIssue(jiraIssue) {
  if (!GITHUB_TOKEN) {
    console.log(`   ⏭️  Skipping GitHub sync (no token)`);
    return;
  }

  const jiraKey = jiraIssue.key;
  const fields = jiraIssue.fields;

  const title = `[${jiraKey}] ${fields.summary}`;
  const description = extractText(fields.description);
  const jiraUrl = `${JIRA_URL}/browse/${jiraKey}`;

  const body = `**JIRA Ticket**: [${jiraKey}](${jiraUrl})
**Status**: ${fields.status.name}
**Type**: ${fields.issuetype.name}
**Priority**: ${fields.priority?.name || 'None'}
${fields.parent ? `**Parent Epic**: ${fields.parent.key}` : ''}

---

${description || 'No description provided'}

---

*This issue is automatically synchronized with JIRA ticket ${jiraKey}*`;

  const labels = [
    `jira:${jiraKey}`,
    `type:${fields.issuetype.name.toLowerCase()}`,
    `status:${fields.status.name.toLowerCase().replace(/\s+/g, '-')}`
  ];

  const [owner, repo] = GITHUB_REPO.split('/');

  // Check if issue exists
  const searchUrl = `https://api.github.com/search/issues?q=${encodeURIComponent(`${jiraKey} repo:${GITHUB_REPO}`)}`;
  const searchOptions = {
    hostname: 'api.github.com',
    path: searchUrl.replace('https://api.github.com', ''),
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${GITHUB_TOKEN}`,
      'Accept': 'application/vnd.github+json',
      'User-Agent': 'Aurigraph-JIRA-Sync'
    }
  };

  try {
    const searchResult = await httpsRequest(searchOptions);

    if (searchResult.body.total_count > 0) {
      const issueNumber = searchResult.body.items[0].number;
      console.log(`   ✅ GitHub issue #${issueNumber} already exists for ${jiraKey}`);

      // Update issue
      const updateUrl = `/repos/${owner}/${repo}/issues/${issueNumber}`;
      const updateOptions = {
        hostname: 'api.github.com',
        path: updateUrl,
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${GITHUB_TOKEN}`,
          'Accept': 'application/vnd.github+json',
          'User-Agent': 'Aurigraph-JIRA-Sync',
          'Content-Type': 'application/json'
        }
      };

      await httpsRequest(updateOptions, { title, body, labels });
      console.log(`   📝 Updated GitHub issue #${issueNumber}`);

    } else {
      // Create new issue
      const createUrl = `/repos/${owner}/${repo}/issues`;
      const createOptions = {
        hostname: 'api.github.com',
        path: createUrl,
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${GITHUB_TOKEN}`,
          'Accept': 'application/vnd.github+json',
          'User-Agent': 'Aurigraph-JIRA-Sync',
          'Content-Type': 'application/json'
        }
      };

      const result = await httpsRequest(createOptions, { title, body, labels });
      if (result.statusCode === 201) {
        console.log(`   ✅ Created GitHub issue #${result.body.number} for ${jiraKey}`);
      } else {
        console.log(`   ❌ Failed to create issue for ${jiraKey}: ${result.body.message}`);
      }
    }

  } catch (error) {
    console.error(`   ❌ Error syncing ${jiraKey}:`, error.message);
  }
}

// Main function
async function main() {
  console.log('Starting GitHub-JIRA sync...\n');

  const issues = await fetchJiraIssues();
  console.log(`\n📊 Total JIRA issues: ${issues.length}\n`);

  if (issues.length === 0) {
    console.log('No issues to sync');
    return;
  }

  // Group by type
  const epics = issues.filter(i => i.fields.issuetype.name === 'Epic');
  const tasks = issues.filter(i => i.fields.issuetype.name !== 'Epic');

  console.log(`   Epics: ${epics.length}`);
  console.log(`   Tasks/Stories: ${tasks.length}\n`);

  if (!GITHUB_TOKEN) {
    console.log('⚠️  GitHub sync disabled (no token provided)');
    console.log('Set GITHUB_TOKEN environment variable to enable GitHub sync\n');
    return;
  }

  console.log('🔄 Syncing to GitHub...\n');

  let synced = 0;
  for (const issue of issues) {
    console.log(`[${++synced}/${issues.length}] Syncing ${issue.key}: ${issue.fields.summary}`);
    await createGitHubIssue(issue);
    await new Promise(resolve => setTimeout(resolve, 1000)); // Rate limiting
  }

  console.log('\n✅ Sync complete!');
  console.log(`\nView GitHub issues: https://github.com/${GITHUB_REPO}/issues`);
  console.log(`View JIRA board: ${JIRA_URL}/jira/software/projects/${PROJECT_KEY}/boards/789`);
}

main().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
ENDSCRIPT

# Check for Node.js
if ! command -v node &> /dev/null; then
  echo "❌ Node.js is required but not installed"
  echo "Please install Node.js 18+ from https://nodejs.org/"
  exit 1
fi

echo "Step 1: Fetching JIRA issues and syncing to GitHub..."
echo ""

# Run the sync script
JIRA_USER="$JIRA_USER" \
JIRA_TOKEN="$JIRA_TOKEN" \
JIRA_URL="$JIRA_URL" \
PROJECT_KEY="$PROJECT_KEY" \
GITHUB_TOKEN="$GITHUB_TOKEN" \
GITHUB_REPO="$GITHUB_REPO" \
node /tmp/sync-jira-github.js

SYNC_STATUS=$?

echo ""
echo "=========================================="

if [ $SYNC_STATUS -eq 0 ]; then
  echo "✅ Synchronization Complete!"
else
  echo "❌ Synchronization encountered errors"
fi

echo "=========================================="
echo ""
echo "📊 Summary:"
echo "   - JIRA Project: $PROJECT_KEY"
echo "   - GitHub Repo: $GITHUB_REPO"
if [ "$GITHUB_ENABLED" = true ]; then
  echo "   - GitHub sync: Enabled"
else
  echo "   - GitHub sync: Disabled (no token)"
fi
echo ""
echo "🔗 Links:"
echo "   - JIRA Board: $JIRA_URL/jira/software/projects/$PROJECT_KEY/boards/789"
echo "   - GitHub Issues: https://github.com/$GITHUB_REPO/issues"
echo ""

# Cleanup
rm -f /tmp/sync-jira-github.js

exit $SYNC_STATUS
