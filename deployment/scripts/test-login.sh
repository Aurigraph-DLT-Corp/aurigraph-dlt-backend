#!/bin/bash

# Test login endpoint
echo "Testing login endpoint at localhost:8080..."
echo ""

# Test 1: Admin with password admin123
echo "Test 1: Login with admin/admin123"
curl -X POST http://localhost:8080/api/v11/users/authenticate -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}" -s | head -50

echo ""
echo "---"
echo ""

# Test 2: Health endpoint check
echo "Test 2: Health check at port 8080"
curl -s http://localhost:8080/q/health | head -30
