#!/bin/bash
# Aurigraph V11 Quick Native Build Script
# Simple, fast native compilation for development

set -e

echo "🚀 Starting quick native build..."

# Set optimal Maven options
export MAVEN_OPTS="-Xmx6g -XX:+UseG1GC"

# Clean and build with native-fast profile
echo "📦 Cleaning previous build..."
./mvnw clean -q

echo "⚡ Building native image (fast profile)..."
time ./mvnw package -Pnative-fast \
    -Dmaven.test.skip=true \
    -Dquarkus.native.native-image-xmx=6g \
    --no-transfer-progress \
    -B

# Check results
BINARY=$(find target -name "*-runner" -type f | head -1)
if [ -n "$BINARY" ]; then
    echo "✅ Native build completed!"
    echo "📊 Binary size: $(du -sh "$BINARY" | cut -f1)"
    echo "📍 Location: $BINARY"
    echo ""
    echo "🏃 Run with: $BINARY"
    echo "🐳 Or build Docker: docker build -f src/main/docker/Dockerfile.native-micro -t aurigraph-v11:latest ."
else
    echo "❌ Build failed - binary not found"
    exit 1
fi