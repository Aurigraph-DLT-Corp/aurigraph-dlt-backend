# Contributing to Aurigraph DLT Backend

Thank you for your interest in contributing to the Aurigraph DLT Backend! This document provides guidelines and instructions for contributing to the project.

## Code of Conduct

We are committed to providing a welcoming and inspiring community for all. Please be respectful and constructive in all interactions.

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- Git

### Development Setup

```bash
# Clone the repository
git clone https://github.com/Aurigraph-DLT-Corp/aurigraph-dlt-backend.git
cd aurigraph-dlt-backend

# Navigate to the backend
cd aurigraph-v11-standalone

# Start local development environment
docker-compose -f infrastructure/docker/docker-compose.base.yml up -d

# Run in dev mode with hot reload
./mvnw quarkus:dev

# Backend will be available at http://localhost:9003
```

## Making Changes

### Branch Strategy

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Keep branches focused on a single feature or fix

3. Use descriptive branch names:
   - `feature/consensus-optimization` - for new features
   - `bugfix/transaction-timeout` - for bug fixes
   - `refactor/api-layer` - for refactoring
   - `docs/architecture-guide` - for documentation

### Code Style

- Follow Google Java Style Guide conventions
- Use 4-space indentation (not tabs)
- Maximum line length: 120 characters
- Add JavaDoc comments for public APIs
- Include inline comments for complex logic

Example:
```java
/**
 * Processes a transaction and adds it to the blockchain.
 *
 * @param transaction the transaction to process
 * @return the transaction hash
 * @throws TransactionException if the transaction is invalid
 */
public String processTransaction(Transaction transaction) {
    // Implementation here
}
```

### Testing Requirements

All contributions must include tests:

**Unit Tests** (minimum 80% coverage for new code):
```bash
./mvnw test
```

**Integration Tests**:
```bash
./mvnw verify -Pintegration-tests
```

**Performance Tests** (for performance-critical code):
```bash
./mvnw test -Pperformance-tests
```

Example test:
```java
@QuarkusTest
public class TransactionServiceTest {

    @Inject
    TransactionService transactionService;

    @Test
    public void shouldProcessValidTransaction() {
        // Arrange
        Transaction transaction = new Transaction("data");

        // Act
        String hash = transactionService.processTransaction(transaction);

        // Assert
        assertNotNull(hash);
    }
}
```

### Commit Messages

Use clear, descriptive commit messages following this format:

```
<type>: <subject>

<body>

<footer>
```

**Type** should be one of:
- `feat` - New feature
- `fix` - Bug fix
- `refactor` - Code refactoring
- `perf` - Performance improvement
- `test` - Adding or updating tests
- `docs` - Documentation changes
- `chore` - Build process, dependencies, etc.

**Subject** (50 characters max):
- Use imperative mood ("add" not "added" or "adds")
- Don't capitalize first letter
- No period at the end

**Body** (72 characters per line):
- Explain what and why, not how
- Reference any related issues

**Footer**:
- Close related issues with `Closes #123`
- Reference related PRs with `Related-To: #456`

**Example:**
```
feat: add AI-driven transaction optimization

Implement machine learning model to optimize transaction ordering
based on gas fees and execution dependencies. This improves overall
throughput by up to 3M TPS in lab benchmarks.

- Added AIOptimizationService with ML model loading
- Added transaction scoring algorithm
- Added performance metrics tracking
- Updated TransactionService to use optimization

Closes #456
```

## Pull Request Process

### Before Submitting

1. **Sync with main branch**:
   ```bash
   git fetch origin
   git rebase origin/main
   ```

2. **Run all tests locally**:
   ```bash
   ./mvnw clean verify
   ```

3. **Check code style**:
   ```bash
   ./mvnw checkstyle:check
   ```

4. **Build native image** (if applicable):
   ```bash
   ./mvnw package -Pnative-fast -Dquarkus.native.container-build=true
   ```

### Submitting a Pull Request

1. Push your branch to GitHub:
   ```bash
   git push origin feature/your-feature-name
   ```

2. Open a Pull Request with:
   - Clear title describing the change
   - Detailed description of what changed and why
   - Reference to any related issues
   - Test coverage information
   - Performance impact (if applicable)

3. PR Template (use this format):
   ```markdown
   ## Description
   [Describe your changes here]

   ## Type of Change
   - [ ] Bug fix
   - [ ] New feature
   - [ ] Breaking change
   - [ ] Documentation update

   ## Testing
   - [ ] Added unit tests
   - [ ] Added integration tests
   - [ ] Added performance tests
   - [ ] All tests passing locally

   ## Checklist
   - [ ] Code follows style guidelines
   - [ ] Self-review completed
   - [ ] Documentation updated
   - [ ] No new warnings generated
   - [ ] Coverage maintained (≥80%)

   ## Related Issues
   Closes #[issue number]
   ```

### CI/CD Pipeline

All PRs must pass:
1. **backend-ci.yml** - Build + test + security scan (required)
2. **Code review** - At least 1 approval (required)
3. **Test coverage** - Maintain ≥80% (required)
4. **Performance benchmarks** - No regression (for performance code)

## Development Workflow

### Feature Development

```bash
# 1. Create feature branch
git checkout -b feature/consensus-optimization

# 2. Make changes and commit regularly
git add src/main/java/io/aurigraph/v12/consensus/
git commit -m "feat: add parallel log replication to HyperRAFT++"

# 3. Run tests frequently
./mvnw test

# 4. Push and open PR
git push origin feature/consensus-optimization
```

### Bug Fix Workflow

```bash
# 1. Create bugfix branch from issue
git checkout -b bugfix/transaction-timeout

# 2. Write failing test first
# 3. Fix the bug
./mvnw test

# 4. Commit with reference to issue
git commit -m "fix: add timeout protection to transaction processing

Timeout was causing hangs on slow connections. Added 30-second
timeout with proper error handling.

Closes #789"

# 5. Push and create PR
git push origin bugfix/transaction-timeout
```

## Documentation

### Code Documentation

- All public classes and methods require JavaDoc
- Include `@param`, `@return`, and `@throws` tags
- Document complex algorithms with inline comments

### Repository Documentation

Update these files when relevant:
- `README.md` - Quick start and overview
- `ARCHITECTURE.md` - System design and architecture
- `DEVELOPMENT.md` - Development setup guide
- `.md` files in `docs/` directory

Example:
```bash
# After adding a new API endpoint
# Update docs/api/endpoints.md with new endpoint documentation
# Update README.md if it affects quick start
```

## Performance Considerations

### Benchmarking

```bash
# Run performance tests
./mvnw test -Pperformance-tests

# Performance test example
@QuarkusTest
public class TransactionProcessingPerformanceTest {
    @Test
    public void shouldProcess1MillionTransactionsPerSecond() {
        // Benchmark code
    }
}
```

### Optimization Guidelines

- Profile before optimizing (use JProfiler or async-profiler)
- Document performance implications in PR description
- Include benchmark results for performance-critical changes
- Consider memory overhead vs. speed trade-offs

## Security

### Reporting Security Vulnerabilities

Do **NOT** open a public issue for security vulnerabilities. Instead:

1. Email: `security@aurigraph.io`
2. Include:
   - Description of vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if available)

### Security Best Practices

- Never commit secrets, API keys, or credentials
- Use environment variables for sensitive data
- Validate all user input
- Follow OWASP guidelines
- Keep dependencies up to date

## Review Process

### What to Expect

1. **Automated Checks** (≈5 minutes):
   - Build passes
   - Tests pass
   - Code style verified

2. **Code Review** (≈24 hours):
   - At least 1 approval required
   - Suggestions or changes requested (common)
   - Performance impact reviewed

3. **Final Checks**:
   - All comments addressed
   - Rebase on latest main
   - Squash commits if requested

### Providing Feedback

- Be respectful and constructive
- Explain the "why" not just the "what"
- Suggest improvements, don't demand changes
- Ask questions if unclear

## Release Process

### Version Numbering

We follow Semantic Versioning: `MAJOR.MINOR.PATCH`

- **MAJOR**: Breaking API changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

### Release Steps

1. Create release branch: `release/v12.1.0`
2. Update version in `pom.xml` and documentation
3. Run full test suite
4. Tag release: `git tag v12.1.0`
5. Create GitHub release with changelog

## Common Issues

### Build Fails

```bash
# Clean and rebuild
./mvnw clean compile

# Clear Maven cache
rm -rf ~/.m2/repository
```

### Tests Fail Locally

```bash
# Run specific test with debug output
./mvnw test -Dtest=AurigraphResourceTest -X

# Check test logs
cat target/surefire-reports/*.txt
```

### Docker Issues

```bash
# Restart services
docker-compose down
docker-compose up -d

# Check logs
docker-compose logs -f
```

## Getting Help

- **Documentation**: See [DEVELOPMENT.md](./DEVELOPMENT.md)
- **Architecture**: See [ARCHITECTURE.md](./ARCHITECTURE.md)
- **Issues**: Check [GitHub Issues](https://github.com/Aurigraph-DLT-Corp/aurigraph-dlt-backend/issues)
- **Discussions**: Start a [GitHub Discussion](https://github.com/Aurigraph-DLT-Corp/aurigraph-dlt-backend/discussions)
- **Email**: `support@aurigraph.io`

## Recognition

Contributors will be recognized in:
- Release notes
- Contributors page
- Team documentation

Thank you for contributing to Aurigraph DLT! 🚀
