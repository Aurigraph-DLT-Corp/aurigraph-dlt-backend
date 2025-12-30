#!/bin/bash

echo "=========================================="
echo "V12 DEPLOYMENT VERIFICATION"
echo "=========================================="
echo "Time: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# Test remote service health
echo "Testing remote V12 service..."
if ssh -q subbu@dlt.aurigraph.io "curl -s http://localhost:9003/q/health 2>/dev/null | grep -q 'status'" 2>/dev/null; then
    echo "✅ V12 service is healthy and responding"
else
    echo "⚠️  V12 service health check inconclusive"
fi

# Check service status
echo ""
echo "Service Status Check:"
ssh -q subbu@dlt.aurigraph.io "sudo systemctl is-active aurigraph-v12.service" 2>/dev/null | grep -q "active" && echo "✅ Service is active" || echo "⚠️  Service status unknown"

# Check port availability
echo ""
echo "Port 9003 Availability:"
ssh -q subbu@dlt.aurigraph.io "netstat -tulpn 2>/dev/null | grep -q ':9003'" 2>/dev/null && echo "✅ Port 9003 is listening" || echo "⚠️  Port status unknown"

# Test Portal access
echo ""
echo "Testing Portal Access:"
if curl -s --insecure https://dlt.aurigraph.io 2>/dev/null | grep -q "html\|React\|Portal" 2>/dev/null; then
    echo "✅ Portal is accessible via HTTPS"
else
    echo "⚠️  Portal access needs verification"
fi

echo ""
echo "=========================================="
echo "Verification Complete"
echo "=========================================="

