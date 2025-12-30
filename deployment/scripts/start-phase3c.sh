#!/bin/bash
# Aurigraph V11 - Sprint 11 Phase 3C Startup Script
# Conservative single-variable tuning based on Phase 3/3B failure analysis
# Target: Incremental improvement over Phase 1 baseline (1.14M TPS)

# Find the JAR file
JAR_FILE=$(ls -t target/*-runner.jar | head -1)

if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found. Run './mvnw clean package' first."
    exit 1
fi

echo "🚀 Starting Aurigraph V11 with Sprint 11 Phase 3C conservative tuning..."
echo "📦 JAR: $JAR_FILE"
echo ""
echo "Phase 3C Optimization (SINGLE conservative change from Phase 1):"
echo "  ✅ Code: Phase 1 baseline (10ms poll, 2048 map capacity)"
echo "  ✅ JVM: G1HeapRegionSize = 16M (changed from default)"
echo "  ℹ️  Rationale: Better match for typical transaction object sizes"
echo ""
echo "Expected outcome: +5-10% TPS improvement (1.14M → 1.2-1.25M TPS)"
echo ""

# Start with SINGLE conservative JVM change
java \
  -XX:+UseG1GC \
  -XX:G1HeapRegionSize=16M \
  -jar "$JAR_FILE"
