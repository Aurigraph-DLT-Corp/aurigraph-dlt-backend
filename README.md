# Aurigraph DLT Backend

**Production-grade blockchain platform built on Java 21/Quarkus/GraalVM**

- 🚀 **2M+ TPS** - High-performance consensus with HyperRAFT++
- 🔐 **Quantum-Resistant** - NIST Level 5 cryptography (CRYSTALS)
- 🤖 **AI-Optimized** - Machine learning-driven transaction prioritization
- ⚡ **Enterprise-Ready** - Docker, Kubernetes, Terraform infrastructure included
- 🌍 **Cross-Chain** - Multi-blockchain bridge with validator consensus
- 📊 **Observable** - Complete monitoring stack (Prometheus, Grafana, Loki)

## Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.9+
- Git

### Development

```bash
# Clone repository
git clone https://github.com/Aurigraph-DLT-Corp/aurigraph-dlt-backend.git
cd aurigraph-dlt-backend

# Start local development environment
docker-compose -f infrastructure/docker/docker-compose.base.yml up -d

# Build and run backend (hot reload enabled)
cd aurigraph-v11-standalone
./mvnw quarkus:dev

# Backend available at http://localhost:9003
curl http://localhost:9003/q/health
```

### Production Deployment

```bash
# Full production stack (backend + validators + monitoring)
docker-compose -f infrastructure/docker/docker-compose.base.yml \
               -f infrastructure/docker/docker-compose.validators.yml \
               -f infrastructure/docker/docker-compose.monitoring.yml \
               -f infrastructure/docker/docker-compose.production.yml up -d
```

## Repository Structure

```
aurigraph-dlt-backend/
├── aurigraph-v11-standalone/        # Backend codebase (Java/Quarkus)
│   ├── src/main/java/              # Source code (67 packages)
│   ├── src/test/java/              # Unit & integration tests
│   └── pom.xml                     # Maven configuration
│
├── infrastructure/                  # Deployment infrastructure
│   ├── docker/                     # Docker Compose files (5 modular configs)
│   ├── kubernetes/                 # Kubernetes manifests
│   └── terraform/                  # Infrastructure as Code
│
├── deployment/                     # Deployment automation
│   ├── deploy-backend.sh           # Backend deployment script
│   ├── deploy-unified.sh           # Unified deployment orchestration
│   └── rollback.sh                 # Emergency rollback
│
├── monitoring/                     # Observability stack
│   ├── prometheus/                 # Metrics collection
│   ├── grafana/                    # Dashboards
│   └── loki/                       # Log aggregation
│
├── scripts/                        # Utility scripts
│   ├── setup/                      # Initial setup
│   ├── testing/                    # Test utilities
│   └── utilities/                  # General utilities
│
├── .github/
│   └── workflows/                  # CI/CD pipelines (9 workflows)
│
└── docs/
    ├── architecture/               # System design
    ├── api/                        # API reference
    └── deployment/                 # Deployment guides
```

## API Endpoints

**Base URL**: `https://dlt.aurigraph.io/api/v11`

### Health & Metrics
- `GET /health` - Health check (99.99% uptime SLA)
- `GET /info` - System information
- `GET /stats` - Transaction statistics (TPS, finality, validators)
- `GET /analytics/dashboard` - Real-time metrics

### Blockchain
- `GET /blockchain/transactions` - Transaction history (paginated)
- `GET /blockchain/blocks` - Block list with consensus data
- `GET /nodes` - Active validator nodes
- `GET /consensus/status` - HyperRAFT++ consensus state

### Smart Contracts & Assets
- `POST /contracts/deploy` - Deploy smart contracts
- `POST /rwa/tokenize` - Tokenize real-world assets
- `POST /rwa/transfer` - Transfer RWA with cross-chain bridges

### Governance
- `GET /governance/proposals` - Active proposals
- `POST /governance/vote` - Cast governance votes
- `GET /validators/{id}/rewards` - Validator rewards

## Technology Stack

- **Runtime**: Java 21 with Virtual Threads
- **Framework**: Quarkus 3.26+ (cloud-native, GraalVM-optimized)
- **Build Tool**: Maven 3.9+
- **Communication**: REST API (HTTP/2), gRPC (planned), WebSocket (planned)
- **Database**: PostgreSQL 16 + Panache ORM, RocksDB for local state
- **Reactive**: Mutiny for async/non-blocking streams
- **Cryptography**: CRYSTALS-Dilithium/Kyber (NIST Level 5)
- **Consensus**: HyperRAFT++ with parallel log replication
- **Infrastructure**: Docker, Kubernetes, Terraform
- **Monitoring**: Prometheus, Grafana, Loki

## CI/CD Workflows

Automated pipelines ensure code quality and reliability:

1. **backend-ci.yml** - Build + test + security scan on every PR
2. **backend-deploy-staging.yml** - Auto-deploy to staging on main merge
3. **backend-deploy-prod.yml** - Manual production deployment
4. **validator-deploy.yml** - Validator node deployment
5. **infrastructure-lint.yml** - Terraform/K8s validation
6. **docker-build.yml** - Docker image build and push
7. **security-scan.yml** - Weekly dependency and CVE scanning
8. **performance-test.yml** - Nightly load testing (2M TPS benchmarks)
9. **backup.yml** - Daily database backup automation

## Documentation

- [**ARCHITECTURE.md**](./ARCHITECTURE.md) - Complete system architecture
- [**DEVELOPMENT.md**](./DEVELOPMENT.md) - Development setup guide
- [**docs/deployment/**](./docs/deployment/) - Deployment procedures
- [**docs/api/**](./docs/api/) - API reference documentation
- [**docs/monitoring/**](./docs/monitoring/) - Monitoring & observability

## Development & Contributing

### Getting Started
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make changes and commit: `git commit -am 'Add your feature'`
4. Push to branch: `git push origin feature/your-feature`
5. Submit a pull request

### Contribution Guidelines
See [CONTRIBUTING.md](./CONTRIBUTING.md) for detailed guidelines on:
- Code style and conventions
- Commit message format
- Testing requirements
- Review process

### Testing

```bash
cd aurigraph-v11-standalone

# Unit tests
./mvnw test

# Integration tests
./mvnw verify -Pintegration-tests

# Performance tests
./mvnw test -Pperformance-tests

# Coverage reports
./mvnw jacoco:report
# View: target/site/jacoco/index.html
```

## Performance Benchmarks

**V11 Current Performance (December 2025)**:
- **TPS Baseline**: 776K+ (production-verified)
- **TPS with ML Optimization**: 3.0M (lab benchmarks)
- **Startup**: <1s (native), ~3s (JVM with hot reload)
- **Memory**: <256MB (native), ~512MB (JVM)
- **Finality**: <500ms current, <100ms target
- **Block Confirmation**: ~2 seconds average

## Production Deployment

### Prerequisites
- Production server with Docker & Docker Compose
- PostgreSQL 16 database
- Redis 7 cache
- Prometheus for metrics (optional)

### Deployment Steps

```bash
# 1. Deploy with blue-green strategy (zero downtime)
./deployment/deploy-unified.sh production

# 2. Verify health
curl https://dlt.aurigraph.io/api/v11/health

# 3. Check metrics
curl https://dlt.aurigraph.io/api/v11/stats | jq .

# 4. Monitor logs
docker-compose logs -f backend-api
```

### Monitoring & Observability

- **Metrics**: Prometheus at http://localhost:9090
- **Dashboards**: Grafana at http://localhost:3001
- **Logs**: Loki at http://localhost:3100
- **Alerts**: Configured in `monitoring/prometheus/alert-rules.yml`

## Rollback

If issues arise:

```bash
# Emergency rollback to previous version
./deployment/rollback.sh

# Expected: Blue deployment active again in <30 seconds
```

## Support & Issues

- 🐛 **Report Bugs**: [GitHub Issues](https://github.com/Aurigraph-DLT-Corp/aurigraph-dlt-backend/issues)
- 💬 **Discuss**: [GitHub Discussions](https://github.com/Aurigraph-DLT-Corp/aurigraph-dlt-backend/discussions)
- 📧 **Email**: support@aurigraph.io
- 🔗 **Documentation**: [Full docs](./docs/)

## License

Apache License 2.0 - See [LICENSE](./LICENSE) for details

## Acknowledgments

Built with ❤️ by the Aurigraph team, powered by Quarkus and GraalVM.

---

**Ready to build the future of enterprise blockchain?** 🚀

[Get Started →](./DEVELOPMENT.md) | [API Docs →](./docs/api/) | [Deploy →](./docs/deployment/)
