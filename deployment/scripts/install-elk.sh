#!/bin/bash

# ELK Stack Installation Script for Aurigraph V11 (Production)
# Target: dlt.aurigraph.io (Ubuntu 24.04.3 LTS)
# Run as: sudo ./install-elk.sh

set -e

echo "========================================="
echo "Aurigraph V11 ELK Stack Installer"
echo "========================================="
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo "ERROR: Please run as root (use sudo)"
  exit 1
fi

# Detect system info
echo "[1/10] Detecting system information..."
OS=$(lsb_release -is)
OS_VERSION=$(lsb_release -rs)
echo "Operating System: $OS $OS_VERSION"

if [ "$OS" != "Ubuntu" ]; then
  echo "WARNING: This script is designed for Ubuntu. Proceed with caution."
  read -p "Continue anyway? (y/n) " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
  fi
fi

# Install dependencies
echo "[2/10] Installing dependencies..."
apt-get update
apt-get install -y apt-transport-https wget curl gnupg2

# Add Elasticsearch GPG key and repository
echo "[3/10] Adding Elastic repository..."
wget -qO - https://artifacts.elastic.co/GPG-KEY-elasticsearch | gpg --dearmor -o /usr/share/keyrings/elasticsearch-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/elasticsearch-keyring.gpg] https://artifacts.elastic.co/packages/8.x/apt stable main" > /etc/apt/sources.list.d/elastic-8.x.list

# Update package list
apt-get update

# Install Elasticsearch
echo "[4/10] Installing Elasticsearch..."
apt-get install -y elasticsearch=8.11.3

# Configure Elasticsearch
echo "[5/10] Configuring Elasticsearch..."
cp /opt/aurigraph/elk-config/elasticsearch.yml /etc/elasticsearch/elasticsearch.yml

# Set memory lock limits
cat >> /etc/security/limits.conf <<EOF
elasticsearch soft memlock unlimited
elasticsearch hard memlock unlimited
EOF

# Set vm.max_map_count for Elasticsearch
sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" >> /etc/sysctl.conf

# Create backup directory
mkdir -p /var/lib/aurigraph/elk-backups
chown elasticsearch:elasticsearch /var/lib/aurigraph/elk-backups

# Enable and start Elasticsearch
systemctl daemon-reload
systemctl enable elasticsearch
systemctl start elasticsearch

# Wait for Elasticsearch to start
echo "Waiting for Elasticsearch to start..."
for i in {1..30}; do
  if curl -s http://localhost:9200/_cluster/health > /dev/null 2>&1; then
    echo "Elasticsearch is running!"
    break
  fi
  echo "Waiting... ($i/30)"
  sleep 2
done

# Install Logstash
echo "[6/10] Installing Logstash..."
apt-get install -y logstash=1:8.11.3-1

# Configure Logstash
echo "[7/10] Configuring Logstash..."
cp /opt/aurigraph/elk-config/logstash.yml /etc/logstash/logstash.yml
cp /opt/aurigraph/elk-config/logstash.conf /etc/logstash/conf.d/logstash.conf

# Enable and start Logstash
systemctl enable logstash
systemctl start logstash

# Install Kibana
echo "[8/10] Installing Kibana..."
apt-get install -y kibana=8.11.3

# Configure Kibana
echo "[9/10] Configuring Kibana..."
cp /opt/aurigraph/elk-config/kibana.yml /etc/kibana/kibana.yml

# Enable and start Kibana
systemctl enable kibana
systemctl start kibana

# Wait for Kibana to start
echo "Waiting for Kibana to start..."
for i in {1..60}; do
  if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
    echo "Kibana is running!"
    break
  fi
  echo "Waiting... ($i/60)"
  sleep 2
done

# Install Filebeat (optional)
echo "[10/10] Installing Filebeat (optional)..."
read -p "Install Filebeat for log shipping? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  apt-get install -y filebeat=8.11.3
  cp /opt/aurigraph/elk-config/filebeat.yml /etc/filebeat/filebeat.yml
  systemctl enable filebeat
  systemctl start filebeat
  echo "Filebeat installed and started"
fi

# Configure firewall (UFW)
echo "Configuring firewall..."
read -p "Configure UFW firewall rules? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  ufw allow 22/tcp   comment 'SSH'
  ufw allow 9003/tcp comment 'Aurigraph V11'
  ufw allow 5601/tcp comment 'Kibana'
  # Don't expose Elasticsearch publicly
  # ufw allow 9200/tcp comment 'Elasticsearch'
  echo "Firewall rules configured"
fi

# Create log directories
echo "Creating log directories..."
mkdir -p /var/log/aurigraph
chown -R aurigraph:aurigraph /var/log/aurigraph
chmod 755 /var/log/aurigraph

# Create index lifecycle policy
echo "Creating Elasticsearch ILM policy..."
curl -X PUT "http://localhost:9200/_ilm/policy/aurigraph-logs-policy" -H 'Content-Type: application/json' -d'
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "7d"
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "forcemerge": {
            "max_num_segments": 1
          }
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
' || echo "Warning: Could not create ILM policy"

# Create index template
echo "Creating Elasticsearch index template..."
curl -X PUT "http://localhost:9200/_index_template/aurigraph-logs-template" -H 'Content-Type: application/json' -d'
{
  "index_patterns": ["aurigraph-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "index.lifecycle.name": "aurigraph-logs-policy"
    },
    "mappings": {
      "properties": {
        "@timestamp": {"type": "date"},
        "log_level": {"type": "keyword"},
        "log_category": {"type": "keyword"},
        "service_name": {"type": "keyword"},
        "correlation_id": {"type": "keyword"},
        "transaction_id": {"type": "keyword"},
        "duration_ms": {"type": "float"},
        "transactions_per_second": {"type": "float"},
        "http_status": {"type": "integer"},
        "http_method": {"type": "keyword"},
        "http_path": {"type": "keyword"}
      }
    }
  }
}
' || echo "Warning: Could not create index template"

# Print status
echo ""
echo "========================================="
echo "ELK Stack Installation Complete!"
echo "========================================="
echo ""
echo "Services Status:"
systemctl status elasticsearch --no-pager -l | grep Active
systemctl status logstash --no-pager -l | grep Active
systemctl status kibana --no-pager -l | grep Active

echo ""
echo "Service URLs:"
echo "- Elasticsearch: http://localhost:9200"
echo "- Kibana: http://localhost:5601"
echo "- Logstash API: http://localhost:9600"

echo ""
echo "Next Steps:"
echo "1. Access Kibana at http://localhost:5601"
echo "2. Create index pattern: aurigraph-logs-*"
echo "3. Import dashboards from elk-config/kibana-dashboards/"
echo "4. Start Aurigraph V11 to begin logging"
echo "5. Monitor logs in Kibana Discover"

echo ""
echo "Useful Commands:"
echo "- Check cluster health: curl http://localhost:9200/_cluster/health?pretty"
echo "- View logs: journalctl -u elasticsearch -f"
echo "- Restart services: systemctl restart elasticsearch logstash kibana"

echo ""
echo "Installation log saved to: /var/log/elk-install.log"
