# 🚀 Aurigraph Unified Deployment Guide

## Overview

This guide documents the complete deployment procedures for all three Aurigraph repositories (backend, portal, website) to staging and production environments.

**Last Updated**: December 31, 2025
**Status**: Production Ready ✅
**Version**: 1.0 (SPARC Consolidation Complete)

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Repository Structure](#repository-structure)
3. [Staging Deployment](#staging-deployment)
4. [Production Deployment](#production-deployment)
5. [Health Checks](#health-checks)
6. [Rollback Procedures](#rollback-procedures)
7. [Monitoring & Observability](#monitoring--observability)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### System Requirements

**All Environments**:
- Docker & Docker Compose 2.0+
- Git 2.30+
- Bash 4.0+

**Development/Staging**:
- Java 21+ (for local Maven builds)
- Maven 3.9+
- Node.js 20+ (for portal/website)

**Production**:
- Kubernetes 1.25+ (for container orchestration)
- PostgreSQL 16+
- Redis 7+
- NGINX 1.20+ (reverse proxy)

### Infrastructure Access

For production deployments, ensure you have:
- SSH access to production servers
- GitHub Actions secrets configured
- Domain DNS access (for blue-green deployment switching)
- SSL/TLS certificates for HTTPS endpoints

### GitHub Secrets Configuration

Create these secrets in GitHub (Settings → Secrets and variables → Repository secrets):

```
# Backend Deployment
BACKEND_DEPLOY_KEY           (SSH private key for dlt.aurigraph.io)
BACKEND_STAGING_HOST         (Staging server hostname/IP)
BACKEND_PROD_HOST            (Production server hostname/IP)

# Portal Deployment
PORTAL_DEPLOY_KEY            (SSH private key for portal server)
PORTAL_REGISTRY_USERNAME     (Docker registry username)
PORTAL_REGISTRY_PASSWORD     (Docker registry password)

# Website Deployment
WEBSITE_DEPLOY_KEY           (SSH private key for website server)
HUBSPOT_API_KEY              (HubSpot v3 API key for contact sync)

# Monitoring
SLACK_WEBHOOK                (Slack webhook for notifications)
DATADOG_API_KEY              (Datadog for monitoring/alerting)
```

---

## Repository Structure

```
aurigraph-dlt-backend/
├── scripts/
│   ├── deploy-unified.sh              # Main orchestration script
│   ├── test-local-deployment.sh       # Local testing validation
│   ├── deployment/
│   │   ├── deploy-backend.sh          # Backend-specific deployment
│   │   ├── deploy-portal.sh           # Portal-specific deployment
│   │   └── deploy-website.sh          # Website-specific deployment
│   └── monitoring/
│       ├── prometheus-export.sh       # Export metrics
│       └── health-check.sh            # Validate services
│
├── infrastructure/
│   └── docker/
│       ├── docker-compose.base.yml           # Base services (DB, Redis, API)
│       ├── docker-compose.validators.yml     # Validator nodes
│       ├── docker-compose.monitoring.yml     # Monitoring stack
│       ├── docker-compose.testing.yml        # Test environment
│       └── docker-compose.production.yml     # Production overrides
│
└── deployment/
    ├── docker-compose.yml             # Main production stack
    └── nginx.conf                     # API gateway configuration
```

---

## Staging Deployment

### Quick Start (Staging)

```bash
# 1. Clone backend repository
git clone https://github.com/Aurigraph-DLT-Corp/aurigraph-dlt-backend.git
cd aurigraph-dlt-backend

# 2. Test locally (optional, validates Docker setup)
bash scripts/test-local-deployment.sh

# 3. Deploy to staging environment
bash scripts/deploy-unified.sh staging

# 4. Verify services are running
curl http://localhost:9003/q/health        # Backend health
curl http://localhost:3000/health          # Portal health
curl http://localhost:3001/health          # Website health
```

### Step-by-Step Staging Deployment

#### Stage 1: Backend Deployment

```bash
# Build backend Docker image
cd aurigraph-dlt-backend
mvn clean package -DskipTests -Pnative-fast \
  -Dquarkus.native.container-build=true

# Start backend services (Docker Compose)
docker-compose -f infrastructure/docker/docker-compose.base.yml up -d

# Verify backend is running
docker-compose ps
curl http://localhost:9003/q/health
curl http://localhost:9003/q/metrics | grep tps_current
```

#### Stage 2: Portal Deployment

```bash
# Build portal Docker image
cd ../aurigraph-enterprise-portal
npm install
npm run build

docker build -t aurigraph-portal:latest .

# Start portal services
docker-compose -f infrastructure/docker/docker-compose.portal.yml up -d

# Verify portal is running
curl http://localhost:3000/health
curl http://localhost:3000/api/system/status
```

#### Stage 3: Website Deployment

```bash
# Build website Docker image
cd ../aurigraph-website
npm install
npm run build

docker build -t aurigraph-website:latest .

# Start website services (staging mode)
docker-compose -f infrastructure/docker/docker-compose.staging.yml up -d

# Verify website is running
curl http://localhost:3001/health
curl http://localhost:3001/api/hubspot/test
```

### Staging Verification

```bash
# Check all services are running
docker ps --filter "label=app=aurigraph"

# Check logs for errors
docker-compose logs -f

# Run integration tests
mvn verify -P integration-tests

# Perform load testing (optional)
ab -n 1000 -c 10 http://localhost:9003/api/v11/health
```

---

## Production Deployment

### Prerequisites

- All staging tests passing ✅
- Production database initialized
- SSL/TLS certificates installed
- DNS configured for domains
- Monitoring/alerting configured
- Team approval for deployment

### Blue-Green Deployment Strategy (Website)

Aurigraph website uses **blue-green deployment** for zero-downtime updates:

```
Current State (Blue):
  Port 3000: Running www.aurigraph.io (live)
  Port 3001: Idle (available)

Deployment Process:
  1. Start green (new version) on port 3001
  2. Health check green for 30 seconds
  3. Switch NGINX traffic: 3000 → 3001
  4. Keep blue (old version) on port 3000 for 24h rollback
  5. If issues, switch back: 3001 → 3000 (< 1 second)

Downtime: 0 seconds
Rollback time: < 1 second
```

### Production Deployment Command

```bash
# 1. Deploy to staging first (validation)
bash scripts/deploy-unified.sh staging

# 2. Run full integration tests
mvn verify -P production-tests

# 3. Deploy to production (requires manual approval)
bash scripts/deploy-unified.sh production

# 4. Monitor health checks
bash scripts/monitoring/health-check.sh production

# 5. Verify all endpoints
curl https://dlt.aurigraph.io/api/v11/health
curl https://dlt.aurigraph.io/health
curl https://www.aurigraph.io/health
curl https://www.aurigraph.io/api/hubspot/test
```

### GitHub Actions Production Deployment

Alternatively, trigger production deployments via GitHub Actions:

```bash
# List available workflows
gh workflow list

# Trigger production deployment
gh workflow run backend-deploy-prod.yml --ref main
gh workflow run portal-deploy-prod.yml --ref main
gh workflow run website-deploy-prod.yml --ref main   # Uses blue-green

# Monitor deployment progress
gh run list --workflow backend-deploy-prod.yml --limit 1
gh run view <RUN_ID> --log

# Check deployment status
gh run view <RUN_ID> --exit-status
```

---

## Health Checks

### Backend API Health

```bash
# Basic health (all services)
curl https://dlt.aurigraph.io/api/v11/health

# Detailed health with metrics
curl https://dlt.aurigraph.io/q/health/details

# Prometheus metrics
curl https://dlt.aurigraph.io/q/metrics \
  | grep -E "tps_current|finality_ms|validators_active"

# Transaction statistics
curl https://dlt.aurigraph.io/api/v11/stats | jq '.'

# Consensus status
curl https://dlt.aurigraph.io/api/v11/consensus/status | jq '.'
```

### Portal Health

```bash
# Portal API health
curl https://dlt.aurigraph.io/health

# System status
curl https://dlt.aurigraph.io/api/system/status

# Database connectivity
curl https://dlt.aurigraph.io/api/db/ping

# Authentication service
curl -H "Authorization: Bearer $TOKEN" \
  https://dlt.aurigraph.io/api/auth/validate
```

### Website Health

```bash
# Website health check
curl https://www.aurigraph.io/health

# HubSpot integration test
curl https://www.aurigraph.io/api/hubspot/test

# Performance metrics
curl https://www.aurigraph.io/api/metrics | jq '.performance'

# CDN edge cache status
curl -I https://www.aurigraph.io | grep -i "cache\|x-served-by"
```

### Full Health Check Script

```bash
#!/bin/bash
echo "🏥 Aurigraph Production Health Check"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Backend
echo "Backend API..."
if curl -sf https://dlt.aurigraph.io/api/v11/health > /dev/null; then
    echo "  ✅ Backend API operational"
else
    echo "  ❌ Backend API unreachable"
fi

# Portal
echo "Enterprise Portal..."
if curl -sf https://dlt.aurigraph.io/health > /dev/null; then
    echo "  ✅ Portal operational"
else
    echo "  ❌ Portal unreachable"
fi

# Website
echo "Marketing Website..."
if curl -sf https://www.aurigraph.io/health > /dev/null; then
    echo "  ✅ Website operational"
else
    echo "  ❌ Website unreachable"
fi

# HubSpot Integration
echo "HubSpot Integration..."
if curl -sf https://www.aurigraph.io/api/hubspot/test > /dev/null; then
    echo "  ✅ HubSpot sync operational"
else
    echo "  ❌ HubSpot sync failed"
fi

echo ""
echo "🎯 All systems operational" || echo "⚠️  Some systems need attention"
```

---

## Rollback Procedures

### Website Blue-Green Rollback

If issues occur after website deployment:

```bash
# Switch traffic back to blue (instant, < 1 second)
cd aurigraph-website
./deployment/rollback-blue-green.sh

# Verify blue is serving traffic
curl https://www.aurigraph.io -I | grep "Server:"

# Investigate green (old version still running)
docker logs website-green | tail -50

# Rollback complete
echo "✅ Rollback successful - blue is now serving traffic"
```

### Backend Rollback

```bash
# Get previous image version from Docker registry
docker pull aurigraph/backend:v11-prod-previous

# Update docker-compose.yml to use previous version
vim deployment/docker-compose.yml

# Restart services
docker-compose -f deployment/docker-compose.yml restart

# Verify rollback
curl https://dlt.aurigraph.io/api/v11/health
```

### Database Rollback

```bash
# Restore from most recent backup
pg_restore -d aurigraph_prod backups/aurigraph_prod_$(date -d yesterday +%Y%m%d).dump

# Verify data integrity
psql -d aurigraph_prod -c "SELECT COUNT(*) FROM transactions;"

# Run migration checks
./scripts/db-migration-check.sh
```

---

## Monitoring & Observability

### Prometheus Metrics

```
# Access Prometheus dashboard
https://prometheus.aurigraph.io

# Query transaction throughput
rate(transactions_total[5m])

# Query consensus finality
histogram_quantile(0.95, finality_seconds_bucket)

# Query active validators
validators_active{region="aws"}
```

### Grafana Dashboards

```
# Access Grafana
https://grafana.aurigraph.io

# Key dashboards:
- Overview (all systems)
- Backend Performance (TPS, finality)
- Portal Metrics (API latency, errors)
- Website Performance (PageSpeed, HubSpot sync)
- Infrastructure (CPU, memory, disk)
```

### Loki Log Aggregation

```bash
# Query backend logs
curl 'https://loki.aurigraph.io/loki/api/v1/query' \
  --data-urlencode 'query={app="backend",level="error"}' \
  | jq '.data.result[].values'

# Query portal errors
curl 'https://loki.aurigraph.io/loki/api/v1/query' \
  --data-urlencode 'query={app="portal",severity="error"}' \
  | jq '.data.result[].values'
```

### Slack Alerts

All deployments and health checks post to Slack:
- ✅ Successful deployments
- ❌ Failed deployments
- ⚠️ Health check warnings
- 📊 Performance metrics (hourly)

---

## Troubleshooting

### Backend Won't Start

```bash
# Check logs
docker-compose logs backend-api -f

# Common issues:
# 1. Port already in use
lsof -i :9003
kill -9 <PID>

# 2. Database not accessible
psql -h localhost -U aurigraph -d aurigraph_dev

# 3. Insufficient memory
docker stats backend-api
# Need: 512MB+ RAM

# 4. Configuration error
cat src/main/resources/application.properties | grep "quarkus\|consensus"
```

### Portal Can't Connect to Backend

```bash
# Check backend availability from portal container
docker exec portal curl http://backend-api:9003/health

# Check environment variables
docker inspect portal | grep -A 20 "Env"

# Check CORS configuration
curl -H "Origin: http://localhost:3000" \
  http://localhost:9003/api/v11/health -v

# Fix: Update docker-compose.portal.yml
API_BASE_URL=http://backend-api:9003
```

### Website HubSpot Integration Fails

```bash
# Check HubSpot API key
echo $HUBSPOT_API_KEY

# Test HubSpot connectivity
curl -H "Authorization: Bearer $HUBSPOT_API_KEY" \
  https://api.hubapi.com/crm/v3/objects/contacts

# Check retry logic
docker logs website | grep -i "hubspot\|retry"

# Check database has contact records
psql -d aurigraph_website -c "SELECT COUNT(*) FROM hubspot_contacts;"
```

### Slow Performance During Peak Hours

```bash
# Monitor TPS
curl https://dlt.aurigraph.io/q/metrics | grep tps_current

# Check CPU/Memory
docker stats --no-stream

# Analyze slow queries
docker exec postgres psql -U aurigraph -d aurigraph_prod \
  -c "SELECT query, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"

# Scale validators if needed
docker-compose up -d --scale api-validator=5
```

---

## Post-Deployment Checklist

After any production deployment:

- [ ] All health checks passing
- [ ] No error logs in past 5 minutes
- [ ] Prometheus metrics showing expected values
- [ ] Slack deployment notification posted
- [ ] Team notified of successful deployment
- [ ] Monitor for 30 minutes for issues
- [ ] Update deployment log with timestamp
- [ ] Archive deployment artifacts

---

## Support

For deployment issues or questions:

1. Check this guide's troubleshooting section
2. Review service logs: `docker-compose logs -f`
3. Check GitHub Actions workflow logs
4. Contact DevOps team on Slack: #deployment-support

---

**Last Updated**: December 31, 2025
**Next Review**: January 15, 2026
**Maintained By**: DevOps Team
**Version**: 1.0 (SPARC Consolidation Complete)
