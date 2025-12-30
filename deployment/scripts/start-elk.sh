#!/bin/bash

# Start ELK Stack Services for Aurigraph V11
# Usage: ./start-elk.sh [dev|prod]

set -e

MODE="${1:-dev}"

echo "========================================="
echo "Aurigraph V11 ELK Stack Starter"
echo "Mode: $MODE"
echo "========================================="
echo ""

if [ "$MODE" == "dev" ]; then
  # Development mode - Docker Compose
  echo "[DEV MODE] Starting ELK stack with Docker Compose..."

  # Check if Docker is running
  if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker is not running. Please start Docker first."
    exit 1
  fi

  # Check if docker-compose file exists
  if [ ! -f "docker-compose-elk.yml" ]; then
    echo "ERROR: docker-compose-elk.yml not found in current directory"
    exit 1
  fi

  # Create necessary directories
  echo "Creating required directories..."
  mkdir -p logs elk-config

  # Start services
  echo "Starting ELK stack containers..."
  docker-compose -f docker-compose-elk.yml up -d

  # Wait for services to be healthy
  echo ""
  echo "Waiting for services to start..."
  echo "This may take 2-3 minutes..."
  echo ""

  # Wait for Elasticsearch
  echo -n "Waiting for Elasticsearch..."
  for i in {1..60}; do
    if curl -s http://localhost:9200/_cluster/health > /dev/null 2>&1; then
      echo " OK"
      break
    fi
    echo -n "."
    sleep 2
  done

  # Wait for Logstash
  echo -n "Waiting for Logstash..."
  for i in {1..60}; do
    if curl -s http://localhost:9600/_node/stats > /dev/null 2>&1; then
      echo " OK"
      break
    fi
    echo -n "."
    sleep 2
  done

  # Wait for Kibana
  echo -n "Waiting for Kibana..."
  for i in {1..90}; do
    if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
      echo " OK"
      break
    fi
    echo -n "."
    sleep 2
  done

  echo ""
  echo "========================================="
  echo "ELK Stack is running!"
  echo "========================================="
  echo ""
  echo "Service URLs:"
  echo "- Elasticsearch: http://localhost:9200"
  echo "- Kibana: http://localhost:5601"
  echo "- Logstash API: http://localhost:9600"
  echo ""
  echo "Container Status:"
  docker-compose -f docker-compose-elk.yml ps
  echo ""
  echo "View Logs:"
  echo "  docker-compose -f docker-compose-elk.yml logs -f"
  echo ""
  echo "Stop Services:"
  echo "  docker-compose -f docker-compose-elk.yml down"

elif [ "$MODE" == "prod" ]; then
  # Production mode - systemd services
  echo "[PROD MODE] Starting ELK stack services..."

  # Check if running as root
  if [ "$EUID" -ne 0 ]; then
    echo "ERROR: Production mode requires root privileges (use sudo)"
    exit 1
  fi

  # Start Elasticsearch
  echo "Starting Elasticsearch..."
  systemctl start elasticsearch
  systemctl status elasticsearch --no-pager | grep Active

  # Wait for Elasticsearch
  echo -n "Waiting for Elasticsearch to be ready..."
  for i in {1..30}; do
    if curl -s http://localhost:9200/_cluster/health > /dev/null 2>&1; then
      echo " OK"
      break
    fi
    echo -n "."
    sleep 2
  done

  # Start Logstash
  echo "Starting Logstash..."
  systemctl start logstash
  systemctl status logstash --no-pager | grep Active

  # Wait for Logstash
  echo -n "Waiting for Logstash to be ready..."
  for i in {1..30}; do
    if curl -s http://localhost:9600/_node/stats > /dev/null 2>&1; then
      echo " OK"
      break
    fi
    echo -n "."
    sleep 2
  done

  # Start Kibana
  echo "Starting Kibana..."
  systemctl start kibana
  systemctl status kibana --no-pager | grep Active

  # Wait for Kibana
  echo -n "Waiting for Kibana to be ready..."
  for i in {1..60}; do
    if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
      echo " OK"
      break
    fi
    echo -n "."
    sleep 2
  done

  # Start Filebeat (if installed)
  if systemctl is-enabled filebeat > /dev/null 2>&1; then
    echo "Starting Filebeat..."
    systemctl start filebeat
    systemctl status filebeat --no-pager | grep Active
  fi

  echo ""
  echo "========================================="
  echo "ELK Stack is running!"
  echo "========================================="
  echo ""
  echo "Service Status:"
  systemctl status elasticsearch --no-pager -l | grep Active
  systemctl status logstash --no-pager -l | grep Active
  systemctl status kibana --no-pager -l | grep Active

  echo ""
  echo "Service URLs:"
  echo "- Elasticsearch: http://localhost:9200"
  echo "- Kibana: http://localhost:5601"
  echo "- Logstash API: http://localhost:9600"

else
  echo "ERROR: Invalid mode. Use 'dev' or 'prod'"
  echo "Usage: ./start-elk.sh [dev|prod]"
  exit 1
fi

# Check cluster health
echo ""
echo "Cluster Health:"
curl -s http://localhost:9200/_cluster/health?pretty | grep -E "cluster_name|status|number_of_nodes|active_shards"

echo ""
echo "Next Steps:"
echo "1. Access Kibana: http://localhost:5601"
echo "2. Create index pattern: aurigraph-logs-*"
echo "3. Start Aurigraph V11 to generate logs"
echo "4. View logs in Kibana Discover"
