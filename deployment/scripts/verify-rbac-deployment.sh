#!/bin/bash

###############################################################################
# RBAC V2 Deployment Verification Script
#
# This script verifies that RBAC V2 has been correctly deployed to the
# Aurigraph V11 Enterprise Portal.
#
# Usage: ./verify-rbac-deployment.sh
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Print header
print_header() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  RBAC V2 Deployment Verification${NC}"
    echo -e "${BLUE}  Aurigraph V11 Enterprise Portal${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

# Print section
print_section() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# Check result
check() {
    local name="$1"
    local condition="$2"

    if [ "$condition" = "true" ]; then
        echo -e "  ${GREEN}✓${NC} $name"
        ((PASSED++))
    else
        echo -e "  ${RED}✗${NC} $name"
        ((FAILED++))
    fi
}

# Warning
warn() {
    local message="$1"
    echo -e "  ${YELLOW}⚠${NC} $message"
    ((WARNINGS++))
}

# Info
info() {
    local message="$1"
    echo -e "  ${BLUE}ℹ${NC} $message"
}

###############################################################################
# File Existence Checks
###############################################################################
print_section "File Existence Checks"

# Check core RBAC system v2
if [ -f "aurigraph-rbac-system-v2.js" ]; then
    check "aurigraph-rbac-system-v2.js exists" "true"
    SIZE=$(du -h aurigraph-rbac-system-v2.js | cut -f1)
    info "Size: $SIZE"
else
    check "aurigraph-rbac-system-v2.js exists" "false"
fi

# Check RBAC UI
if [ -f "aurigraph-rbac-ui.html" ]; then
    check "aurigraph-rbac-ui.html exists" "true"
    SIZE=$(du -h aurigraph-rbac-ui.html | cut -f1)
    info "Size: $SIZE"
else
    check "aurigraph-rbac-ui.html exists" "false"
fi

# Check UI loader
if [ -f "aurigraph-rbac-ui-loader.js" ]; then
    check "aurigraph-rbac-ui-loader.js exists" "true"
    SIZE=$(du -h aurigraph-rbac-ui-loader.js | cut -f1)
    info "Size: $SIZE"
else
    check "aurigraph-rbac-ui-loader.js exists" "false"
fi

# Check portal
if [ -f "aurigraph-v11-enterprise-portal.html" ]; then
    check "aurigraph-v11-enterprise-portal.html exists" "true"
else
    check "aurigraph-v11-enterprise-portal.html exists" "false"
fi

# Check admin setup
if [ -f "rbac-admin-setup.html" ]; then
    check "rbac-admin-setup.html exists" "true"
else
    warn "rbac-admin-setup.html not found (optional)"
fi

###############################################################################
# JavaScript Syntax Validation
###############################################################################
print_section "JavaScript Syntax Validation"

# Check if node is available
if command -v node &> /dev/null; then
    # Validate RBAC system v2
    if node -c aurigraph-rbac-system-v2.js 2>/dev/null; then
        check "aurigraph-rbac-system-v2.js syntax valid" "true"
    else
        check "aurigraph-rbac-system-v2.js syntax valid" "false"
    fi

    # Validate UI loader
    if node -c aurigraph-rbac-ui-loader.js 2>/dev/null; then
        check "aurigraph-rbac-ui-loader.js syntax valid" "true"
    else
        check "aurigraph-rbac-ui-loader.js syntax valid" "false"
    fi
else
    warn "Node.js not found - skipping syntax validation"
fi

###############################################################################
# Portal Integration Checks
###############################################################################
print_section "Portal Integration Checks"

# Check if portal references RBAC v2 system
if grep -q "aurigraph-rbac-system-v2.js" aurigraph-v11-enterprise-portal.html; then
    check "Portal loads RBAC system v2" "true"
    LINE=$(grep -n "aurigraph-rbac-system-v2.js" aurigraph-v11-enterprise-portal.html | cut -d: -f1)
    info "Found at line $LINE"
else
    check "Portal loads RBAC system v2" "false"
fi

# Check if portal references UI loader
if grep -q "aurigraph-rbac-ui-loader.js" aurigraph-v11-enterprise-portal.html; then
    check "Portal loads UI loader" "true"
    LINE=$(grep -n "aurigraph-rbac-ui-loader.js" aurigraph-v11-enterprise-portal.html | cut -d: -f1)
    info "Found at line $LINE"
else
    check "Portal loads UI loader" "false"
fi

# Check if v1 is still referenced (should not be)
if grep -q "aurigraph-rbac-system.js\"" aurigraph-v11-enterprise-portal.html && ! grep -q "aurigraph-rbac-system-v2.js" aurigraph-v11-enterprise-portal.html; then
    warn "Portal still references v1 instead of v2"
fi

###############################################################################
# Security Feature Checks
###############################################################################
print_section "Security Feature Checks"

# Check for Sanitizer
if grep -q "const Sanitizer" aurigraph-rbac-system-v2.js; then
    check "HTML Sanitizer implemented" "true"
else
    check "HTML Sanitizer implemented" "false"
fi

# Check for Validator
if grep -q "const Validator" aurigraph-rbac-system-v2.js; then
    check "Input Validator implemented" "true"
else
    check "Input Validator implemented" "false"
fi

# Check for SecureCrypto
if grep -q "const SecureCrypto" aurigraph-rbac-system-v2.js; then
    check "Secure Crypto implemented" "true"
else
    check "Secure Crypto implemented" "false"
fi

# Check for RateLimiter
if grep -q "class RateLimiter" aurigraph-rbac-system-v2.js; then
    check "Rate Limiter implemented" "true"
else
    check "Rate Limiter implemented" "false"
fi

# Check for Logger
if grep -q "const Logger" aurigraph-rbac-system-v2.js; then
    check "Structured Logger implemented" "true"
else
    check "Structured Logger implemented" "false"
fi

# Check for custom error classes
if grep -q "class ValidationError" aurigraph-rbac-system-v2.js; then
    check "Custom error classes implemented" "true"
else
    check "Custom error classes implemented" "false"
fi

###############################################################################
# Documentation Checks
###############################################################################
print_section "Documentation Checks"

# Check for deployment guide
if [ -f "RBAC-V2-DEPLOYMENT-COMPLETE.md" ]; then
    check "Deployment documentation exists" "true"
else
    warn "RBAC-V2-DEPLOYMENT-COMPLETE.md not found"
fi

# Check for refactoring report
if [ -f "RBAC-REFACTORING-REPORT.md" ]; then
    check "Refactoring report exists" "true"
else
    warn "RBAC-REFACTORING-REPORT.md not found"
fi

# Check for integration guide
if [ -f "RBAC-INTEGRATION-GUIDE.md" ]; then
    check "Integration guide exists" "true"
else
    warn "RBAC-INTEGRATION-GUIDE.md not found"
fi

# Check for code review
if [ -f "RBAC-CODE-REVIEW.md" ]; then
    check "Code review document exists" "true"
else
    warn "RBAC-CODE-REVIEW.md not found"
fi

###############################################################################
# File Permissions Checks
###############################################################################
print_section "File Permissions Checks"

# Check if files are readable
if [ -r "aurigraph-rbac-system-v2.js" ]; then
    check "RBAC system v2 is readable" "true"
else
    check "RBAC system v2 is readable" "false"
fi

if [ -r "aurigraph-rbac-ui.html" ]; then
    check "RBAC UI is readable" "true"
else
    check "RBAC UI is readable" "false"
fi

if [ -r "aurigraph-rbac-ui-loader.js" ]; then
    check "UI loader is readable" "true"
else
    check "UI loader is readable" "false"
fi

###############################################################################
# Web Server Check
###############################################################################
print_section "Web Server Check"

# Check if a web server is running
if lsof -i :8000 &> /dev/null || lsof -i :8080 &> /dev/null || lsof -i :3000 &> /dev/null; then
    check "Web server detected" "true"
else
    warn "No web server detected on common ports (8000, 8080, 3000)"
    info "Start a server with: python3 -m http.server 8000"
fi

###############################################################################
# Git Status Check
###############################################################################
print_section "Git Status Check"

if git rev-parse --git-dir > /dev/null 2>&1; then
    # Check if RBAC files are committed
    if git ls-files --error-unmatch aurigraph-rbac-system-v2.js &> /dev/null; then
        check "RBAC v2 committed to git" "true"
    else
        warn "RBAC v2 not committed to git"
    fi

    # Check for uncommitted changes
    if git diff --quiet aurigraph-rbac-system-v2.js aurigraph-rbac-ui-loader.js aurigraph-v11-enterprise-portal.html 2>/dev/null; then
        check "No uncommitted RBAC changes" "true"
    else
        warn "Uncommitted changes detected in RBAC files"
    fi
else
    warn "Not a git repository"
fi

###############################################################################
# Summary
###############################################################################
print_section "Verification Summary"

echo ""
echo -e "  ${GREEN}✓ Passed:${NC}   $PASSED"
echo -e "  ${RED}✗ Failed:${NC}   $FAILED"
echo -e "  ${YELLOW}⚠ Warnings:${NC} $WARNINGS"
echo ""

# Overall status
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}  ✅ DEPLOYMENT VERIFIED - ALL CHECKS PASSED${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo "  1. Start web server: python3 -m http.server 8000"
    echo "  2. Open portal: http://localhost:8000/aurigraph-v11-enterprise-portal.html"
    echo "  3. Create admin: http://localhost:8000/rbac-admin-setup.html"
    echo "  4. Test guest registration flow"
    echo ""
    exit 0
else
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}  ❌ DEPLOYMENT VERIFICATION FAILED${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${YELLOW}Please fix the failed checks before proceeding.${NC}"
    echo ""
    exit 1
fi
