#!/bin/bash

# Backup ELK Stack Data for Aurigraph V11
# Creates snapshots of Elasticsearch indices
# Usage: ./backup-elk.sh [dev|prod]

set -e

MODE="${1:-prod}"
BACKUP_DIR="/var/lib/aurigraph/elk-backups"
DATE=$(date +%Y%m%d-%H%M%S)
SNAPSHOT_NAME="snapshot_$DATE"

echo "========================================="
echo "Aurigraph V11 ELK Stack Backup"
echo "Mode: $MODE"
echo "Date: $DATE"
echo "========================================="
echo ""

# Determine Elasticsearch host
if [ "$MODE" == "dev" ]; then
  ES_HOST="http://localhost:9200"
  BACKUP_DIR="./elk-backups"
else
  ES_HOST="http://localhost:9200"
  BACKUP_DIR="/var/lib/aurigraph/elk-backups"
fi

# Create backup directory if it doesn't exist
echo "Creating backup directory: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"

if [ "$MODE" == "prod" ]; then
  # Set ownership for production
  chown -R elasticsearch:elasticsearch "$BACKUP_DIR"
fi

# Check Elasticsearch connection
echo "Checking Elasticsearch connection..."
if ! curl -s "$ES_HOST/_cluster/health" > /dev/null 2>&1; then
  echo "ERROR: Cannot connect to Elasticsearch at $ES_HOST"
  exit 1
fi
echo "Connected to Elasticsearch successfully"

# Register snapshot repository (one-time setup)
echo ""
echo "Registering snapshot repository..."
curl -X PUT "$ES_HOST/_snapshot/elk_backup" -H 'Content-Type: application/json' -d"
{
  \"type\": \"fs\",
  \"settings\": {
    \"location\": \"$BACKUP_DIR\",
    \"compress\": true,
    \"max_snapshot_bytes_per_sec\": \"100mb\",
    \"max_restore_bytes_per_sec\": \"100mb\"
  }
}
" || echo "Repository may already exist"

# Get current cluster health
echo ""
echo "Current cluster health:"
curl -s "$ES_HOST/_cluster/health?pretty" | grep -E "cluster_name|status|number_of_nodes|active_shards"

# List current indices
echo ""
echo "Current indices:"
curl -s "$ES_HOST/_cat/indices/aurigraph-*?v&h=index,docs.count,store.size"

# Create snapshot
echo ""
echo "Creating snapshot: $SNAPSHOT_NAME"
echo "This may take several minutes depending on data size..."

SNAPSHOT_RESPONSE=$(curl -X PUT "$ES_HOST/_snapshot/elk_backup/$SNAPSHOT_NAME?wait_for_completion=true" -H 'Content-Type: application/json' -d'
{
  "indices": "aurigraph-logs-*,aurigraph-errors-*,aurigraph-performance-*,aurigraph-security-*",
  "ignore_unavailable": true,
  "include_global_state": false,
  "metadata": {
    "taken_by": "backup-elk.sh",
    "taken_because": "scheduled backup"
  }
}
' 2>&1)

# Check if snapshot was successful
if echo "$SNAPSHOT_RESPONSE" | grep -q '"state":"SUCCESS"'; then
  echo ""
  echo "========================================="
  echo "Backup completed successfully!"
  echo "========================================="
  echo ""
  echo "Snapshot name: $SNAPSHOT_NAME"
  echo "Backup location: $BACKUP_DIR"

  # Get snapshot info
  echo ""
  echo "Snapshot details:"
  curl -s "$ES_HOST/_snapshot/elk_backup/$SNAPSHOT_NAME?pretty" | grep -E "snapshot|state|start_time|end_time|duration|indices|shards"

else
  echo ""
  echo "========================================="
  echo "Backup FAILED!"
  echo "========================================="
  echo ""
  echo "Error response:"
  echo "$SNAPSHOT_RESPONSE"
  exit 1
fi

# List all snapshots
echo ""
echo "All available snapshots:"
curl -s "$ES_HOST/_snapshot/elk_backup/_all?pretty" | grep -E "snapshot|state|start_time" | head -20

# Calculate backup size
echo ""
echo "Backup directory size:"
du -sh "$BACKUP_DIR"

# Cleanup old backups (keep last 30 days)
echo ""
echo "Cleaning up old backups (keeping last 30 days)..."
CUTOFF_DATE=$(date -d '30 days ago' +%Y%m%d 2>/dev/null || date -v-30d +%Y%m%d)

# Get list of snapshots
SNAPSHOTS=$(curl -s "$ES_HOST/_snapshot/elk_backup/_all" | grep -oP 'snapshot_\d{8}-\d{6}' | sort -u)

for snapshot in $SNAPSHOTS; do
  # Extract date from snapshot name (format: snapshot_YYYYMMDD-HHMMSS)
  snapshot_date=$(echo "$snapshot" | grep -oP '\d{8}' | head -1)

  if [ "$snapshot_date" -lt "$CUTOFF_DATE" ]; then
    echo "Deleting old snapshot: $snapshot (date: $snapshot_date)"
    curl -X DELETE "$ES_HOST/_snapshot/elk_backup/$snapshot" || echo "Failed to delete $snapshot"
  fi
done

# Export Kibana dashboards
echo ""
echo "Exporting Kibana dashboards..."
KIBANA_HOST="http://localhost:5601"
KIBANA_BACKUP_DIR="$BACKUP_DIR/kibana_$DATE"
mkdir -p "$KIBANA_BACKUP_DIR"

# Export all saved objects
curl -X POST "$KIBANA_HOST/api/saved_objects/_export" \
  -H 'kbn-xsrf: true' \
  -H 'Content-Type: application/json' \
  -d '{"type":"dashboard","includeReferencesDeep":true}' \
  -o "$KIBANA_BACKUP_DIR/dashboards.ndjson" 2>/dev/null || echo "Could not export dashboards"

curl -X POST "$KIBANA_HOST/api/saved_objects/_export" \
  -H 'kbn-xsrf: true' \
  -H 'Content-Type: application/json' \
  -d '{"type":"index-pattern"}' \
  -o "$KIBANA_BACKUP_DIR/index-patterns.ndjson" 2>/dev/null || echo "Could not export index patterns"

curl -X POST "$KIBANA_HOST/api/saved_objects/_export" \
  -H 'kbn-xsrf: true' \
  -H 'Content-Type: application/json' \
  -d '{"type":"visualization","includeReferencesDeep":true}' \
  -o "$KIBANA_BACKUP_DIR/visualizations.ndjson" 2>/dev/null || echo "Could not export visualizations"

echo "Kibana configuration backed up to: $KIBANA_BACKUP_DIR"

# Create backup manifest
echo ""
echo "Creating backup manifest..."
cat > "$BACKUP_DIR/manifest_$DATE.txt" <<EOF
Aurigraph V11 ELK Stack Backup Manifest
========================================
Date: $(date)
Snapshot Name: $SNAPSHOT_NAME
Backup Directory: $BACKUP_DIR
Mode: $MODE

Elasticsearch Cluster:
$(curl -s "$ES_HOST/_cluster/health?pretty" 2>/dev/null)

Indices Backed Up:
$(curl -s "$ES_HOST/_cat/indices/aurigraph-*?v" 2>/dev/null)

Snapshot Status:
$(curl -s "$ES_HOST/_snapshot/elk_backup/$SNAPSHOT_NAME?pretty" 2>/dev/null)

Backup Size:
$(du -sh "$BACKUP_DIR" 2>/dev/null)
EOF

echo "Manifest created: $BACKUP_DIR/manifest_$DATE.txt"

# Summary
echo ""
echo "========================================="
echo "Backup Summary"
echo "========================================="
echo "Snapshot: $SNAPSHOT_NAME"
echo "Location: $BACKUP_DIR"
echo "Kibana exports: $KIBANA_BACKUP_DIR"
echo "Manifest: $BACKUP_DIR/manifest_$DATE.txt"
echo ""
echo "To restore this backup, run:"
echo "  ./restore-elk.sh $SNAPSHOT_NAME"
echo ""
echo "To list all backups, run:"
echo "  curl http://localhost:9200/_snapshot/elk_backup/_all?pretty"
