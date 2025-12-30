#!/bin/bash

#######################################################################
# Aurigraph V11 - JaCoCo Coverage Report Generator
#
# Generates comprehensive test coverage reports and displays summary
#
# Usage: ./generate-coverage-report.sh
#
# Outputs:
#   - target/site/jacoco/index.html (HTML report)
#   - target/site/jacoco/jacoco.xml (XML for CI/CD)
#   - target/site/jacoco/jacoco.csv (CSV for analysis)
#   - Console summary with coverage percentages
#
# Requirements:
#   - Java 21+
#   - Maven 3.8+
#   - All tests must pass
#######################################################################

set -e

echo "========================================="
echo "  Aurigraph V11 Coverage Report"
echo "  V3.7.3 Phase 1 - Test Coverage"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Maven is installed
if ! command -v ./mvnw &> /dev/null; then
    echo -e "${RED}Error: Maven wrapper not found${NC}"
    exit 1
fi

# Clean previous reports
echo -e "${BLUE}Cleaning previous reports...${NC}"
rm -rf target/site/jacoco
rm -f target/jacoco.exec

# Run tests with coverage
echo -e "${BLUE}Running tests with JaCoCo coverage...${NC}"
./mvnw clean test jacoco:report

# Check if report was generated
if [ ! -f "target/site/jacoco/index.html" ]; then
    echo -e "${RED}Error: Coverage report not generated${NC}"
    echo -e "${YELLOW}Tests may have failed. Check test output above.${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ Coverage report generated successfully!${NC}"
echo ""
echo "========================================="
echo "  Coverage Report Locations"
echo "========================================="
echo -e "HTML Report:  ${BLUE}target/site/jacoco/index.html${NC}"
echo -e "XML Report:   ${BLUE}target/site/jacoco/jacoco.xml${NC}"
echo -e "CSV Report:   ${BLUE}target/site/jacoco/jacoco.csv${NC}"
echo ""

# Extract coverage summary from CSV
if [ -f "target/site/jacoco/jacoco.csv" ]; then
    echo "========================================="
    echo "  Coverage Summary"
    echo "========================================="

    # Parse CSV to get overall coverage
    # CSV format: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED

    tail -n 1 target/site/jacoco/jacoco.csv | awk -F',' '{
        line_missed = $8
        line_covered = $9
        branch_missed = $6
        branch_covered = $7
        method_missed = $12
        method_covered = $13

        total_lines = line_missed + line_covered
        total_branches = branch_missed + branch_covered
        total_methods = method_missed + method_covered

        line_coverage = (total_lines > 0) ? (line_covered * 100 / total_lines) : 0
        branch_coverage = (total_branches > 0) ? (branch_covered * 100 / total_branches) : 0
        method_coverage = (total_methods > 0) ? (method_covered * 100 / total_methods) : 0

        printf "Line Coverage:   %.2f%% (%d/%d lines)\n", line_coverage, line_covered, total_lines
        printf "Branch Coverage: %.2f%% (%d/%d branches)\n", branch_coverage, branch_covered, total_branches
        printf "Method Coverage: %.2f%% (%d/%d methods)\n", method_coverage, method_covered, total_methods
    }'

    echo ""
fi

# Display Phase 1 targets
echo "========================================="
echo "  Phase 1 Coverage Targets"
echo "========================================="
echo "Overall Project:   ≥50% line, ≥45% branch"
echo "Crypto Package:    ≥98% line, ≥95% branch"
echo "Consensus Package: ≥95% line, ≥90% branch"
echo "Bridge Package:    ≥85% line, ≥80% branch"
echo ""

# Open HTML report in browser (macOS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo -e "${YELLOW}Opening HTML report in browser...${NC}"
    open target/site/jacoco/index.html
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    if command -v xdg-open &> /dev/null; then
        xdg-open target/site/jacoco/index.html
    fi
fi

echo -e "${GREEN}Done!${NC}"
