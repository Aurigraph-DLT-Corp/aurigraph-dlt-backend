# Multi-stage build for Aurigraph V11
# Stage 1: Build stage
# Using eclipse-temurin (maintained replacement for deprecated openjdk images)
FROM eclipse-temurin:21-jdk-slim as builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Copy source code
COPY src ./src

# Build the application
RUN chmod +x mvnw && \
    ./mvnw clean package -DskipTests && \
    mkdir -p /app/built && \
    find target -name "*-runner.jar" -exec cp {} /app/built/app.jar \;

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jdk-slim

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from builder stage
COPY --from=builder /app/built/app.jar .

# Create non-root user for security
RUN useradd -m -u 1000 aurigraph && chown -R aurigraph:aurigraph /app
USER aurigraph

# Expose port
EXPOSE 9003

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:9003/api/v11/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD []
