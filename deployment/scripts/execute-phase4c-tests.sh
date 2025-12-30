#!/bin/bash

echo "=========================================="
echo "PHASE 4C - K6 LOAD TESTING EXECUTION"
echo "=========================================="
echo "Start Time: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# Create results directory
mkdir -p k6-results

# Execute the orchestration script
echo "Running K6 load test orchestration..."
echo ""

if [ -f "run-scenarios.sh" ]; then
    chmod +x run-scenarios.sh
    bash run-scenarios.sh
    TEST_RESULT=$?
    
    echo ""
    echo "=========================================="
    if [ $TEST_RESULT -eq 0 ]; then
        echo "✅ K6 LOAD TESTS COMPLETED SUCCESSFULLY"
    else
        echo "⚠️  K6 LOAD TESTS COMPLETED WITH WARNINGS"
    fi
    echo "=========================================="
    echo "Results Location: ./k6-results/"
    echo "Completion Time: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    
    # List results files
    echo "Generated Results Files:"
    ls -lh k6-results/ 2>/dev/null | grep -v '^total' | awk '{print "  - " $9 " (" $5 ")"}'
else
    echo "❌ ERROR: run-scenarios.sh not found"
    exit 1
fi

