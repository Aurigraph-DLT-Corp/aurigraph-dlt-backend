#!/bin/bash
# Aurigraph V11 - Sprint 11 Phase 3B Startup Script
# Moderate tuning based on Phase 3 regression analysis
# Target: Restore 1.14M TPS baseline + modest improvements

# Find the JAR file
JAR_FILE=$(ls -t target/*-runner.jar | head -1)

if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found. Run './mvnw clean package' first."
    exit 1
fi

echo "🚀 Starting Aurigraph V11 with Sprint 11 Phase 3B optimizations..."
echo "📦 JAR: $JAR_FILE"
echo ""
echo "Optimizations applied:"
echo "  ✅ BlockingQueue: Reverted to 5ms blocking poll (more efficient)"
echo "  ✅ ConcurrentHashMap: 4096 initial capacity with concurrency=16"
echo "  ✅ GC tuning: G1GC with 50ms pause target (moderate)"
echo "  ✅ Heap: 4GB fixed size (no pre-touch)"
echo ""

# Start with moderate JVM tuning
java \
  -Xms4g \
  -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=50 \
  -XX:G1HeapRegionSize=32M \
  -XX:InitiatingHeapOccupancyPercent=35 \
  -XX:+UseStringDeduplication \
  -jar "$JAR_FILE"
