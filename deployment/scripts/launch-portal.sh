#!/bin/bash

# Aurigraph V11 Enterprise Portal Launcher
# This script starts the portal using a local HTTP server

set -e

PORTAL_FILE="aurigraph-v11-enterprise-portal.html"
PORT=8080

echo "======================================"
echo "Aurigraph V11 Enterprise Portal"
echo "======================================"
echo ""

# Check if portal file exists
if [ ! -f "$PORTAL_FILE" ]; then
    echo "Error: Portal file not found: $PORTAL_FILE"
    exit 1
fi

echo "Portal file: $PORTAL_FILE"
echo "Portal size: $(du -h $PORTAL_FILE | cut -f1)"
echo "Server port: $PORT"
echo ""

# Function to start server based on available tools
start_server() {
    if command -v python3 &> /dev/null; then
        echo "Starting Python HTTP server..."
        echo "Portal URL: http://localhost:$PORT/$PORTAL_FILE"
        echo ""
        echo "Press Ctrl+C to stop the server"
        echo ""
        python3 -m http.server $PORT
    elif command -v python &> /dev/null; then
        echo "Starting Python HTTP server..."
        echo "Portal URL: http://localhost:$PORT/$PORTAL_FILE"
        echo ""
        echo "Press Ctrl+C to stop the server"
        echo ""
        python -m SimpleHTTPServer $PORT
    elif command -v npx &> /dev/null; then
        echo "Starting Node.js HTTP server..."
        echo "Portal URL: http://localhost:$PORT/$PORTAL_FILE"
        echo ""
        echo "Press Ctrl+C to stop the server"
        echo ""
        npx http-server -p $PORT
    else
        echo "Error: No HTTP server available"
        echo "Please install Python or Node.js to run the portal"
        echo ""
        echo "Alternative: Open the file directly in your browser:"
        echo "  open $PORTAL_FILE"
        exit 1
    fi
}

# Check if V11 backend is running
check_backend() {
    echo "Checking V11 backend status..."
    if curl -s http://localhost:9003/api/v11/health > /dev/null 2>&1; then
        echo "✓ V11 backend is running on port 9003"
        echo ""
    else
        echo "✗ V11 backend not detected on port 9003"
        echo "  The portal will work in demo mode"
        echo ""
        echo "  To start the backend:"
        echo "    cd aurigraph-v11-standalone"
        echo "    ./mvnw quarkus:dev"
        echo ""
    fi
}

# Check backend status
check_backend

# Start the server
start_server
