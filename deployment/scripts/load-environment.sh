#!/bin/bash

echo "🔄 Loading Aurigraph V11 Environment..."
echo "========================================"
echo ""

BASE_DIR="/Users/subbujois/Documents/GitHub/Aurigraph-DLT/aurigraph-av10-7/aurigraph-v11-standalone"

echo "📋 Phase 1: Current Status (TODO.md)"
echo "================================"
if [ -f "$BASE_DIR/TODO.md" ]; then
    head -150 "$BASE_DIR/TODO.md"
else
    echo "❌ ERROR: TODO.md not found!"
fi
echo ""
echo "Press Enter to continue..."
read

echo "📅 Phase 2: Sprint Plan"
echo "================================"
if [ -f "$BASE_DIR/SPRINT_PLAN.md" ]; then
    head -150 "$BASE_DIR/SPRINT_PLAN.md"
else
    echo "❌ ERROR: SPRINT_PLAN.md not found!"
fi
echo ""
echo "Press Enter to continue..."
read

echo "🧪 Phase 3: Comprehensive Test Plan"
echo "================================"
if [ -f "$BASE_DIR/COMPREHENSIVE-TEST-PLAN.md" ]; then
    head -150 "$BASE_DIR/COMPREHENSIVE-TEST-PLAN.md"
else
    echo "❌ ERROR: COMPREHENSIVE-TEST-PLAN.md not found!"
fi
echo ""
echo "Press Enter to continue..."
read

echo "⚡ Phase 4: Parallel Sprint Execution"
echo "================================"
if [ -f "$BASE_DIR/PARALLEL-SPRINT-EXECUTION-PLAN.md" ]; then
    head -100 "$BASE_DIR/PARALLEL-SPRINT-EXECUTION-PLAN.md"
else
    echo "❌ ERROR: PARALLEL-SPRINT-EXECUTION-PLAN.md not found!"
fi
echo ""
echo "Press Enter to continue..."
read

echo "🎯 Phase 5: SPARC Framework Plan"
echo "================================"
if [ -f "$BASE_DIR/SPARC-PROJECT-PLAN.md" ]; then
    head -150 "$BASE_DIR/SPARC-PROJECT-PLAN.md"
else
    echo "❌ ERROR: SPARC-PROJECT-PLAN.md not found!"
fi
echo ""
echo "Press Enter to continue..."
read

echo "📊 Phase 6: Latest Sprint Report"
echo "================================"
LATEST_SPRINT=$(ls -t "$BASE_DIR"/SPRINT*REPORT*.md "$BASE_DIR"/SPRINT*EXECUTION*.md 2>/dev/null | head -1)
if [ -n "$LATEST_SPRINT" ]; then
    echo "Reading: $LATEST_SPRINT"
    head -150 "$LATEST_SPRINT"
else
    echo "⚠️  WARNING: No sprint report found, checking for completion reports..."
    LATEST_SPRINT=$(ls -t "$BASE_DIR"/SPRINT*COMPLETION*.md 2>/dev/null | head -1)
    if [ -n "$LATEST_SPRINT" ]; then
        echo "Reading: $LATEST_SPRINT"
        head -150 "$LATEST_SPRINT"
    else
        echo "❌ No sprint reports found!"
    fi
fi
echo ""

echo "✅ Environment Loading Complete!"
echo "================================"
echo ""
echo "📊 Quick Summary:"
echo "  - TODO.md: Current status loaded"
echo "  - SPRINT_PLAN.md: Sprint objectives loaded"
echo "  - COMPREHENSIVE-TEST-PLAN.md: Testing requirements loaded"
echo "  - PARALLEL-SPRINT-EXECUTION-PLAN.md: Multi-team coordination loaded"
echo "  - SPARC-PROJECT-PLAN.md: SPARC framework roadmap loaded"
echo "  - Latest sprint report: Progress reviewed"
echo ""
echo "🎯 You are now ready to resume development!"
echo "================================"
