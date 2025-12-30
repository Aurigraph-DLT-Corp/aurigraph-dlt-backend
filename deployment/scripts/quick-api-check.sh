#!/bin/bash

# Quick API Check - Tests key integration endpoints
# Date: October 10, 2025

BASE_URL="${BASE_URL:-https://dlt.aurigraph.io}"
TIMEOUT=5

echo "Quick API Integration Check"
echo "Base URL: $BASE_URL"
echo ""

check() {
    local name="$1"
    local endpoint="$2"
    echo -n "$name: "
    code=$(curl -s -o /dev/null -w "%{http_code}" -m $TIMEOUT "$BASE_URL$endpoint" 2>/dev/null || echo "000")
    if [ "$code" = "200" ] || [ "$code" = "204" ]; then
        echo "✅ $code"
        return 0
    elif [ "$code" = "404" ]; then
        echo "❌ NOT FOUND"
        return 1
    elif [ "$code" = "401" ] || [ "$code" = "403" ]; then
        echo "🔒 AUTH REQUIRED ($code)"
        return 0
    elif [ "$code" = "405" ] || [ "$code" = "400" ]; then
        echo "📝 EXISTS ($code)"
        return 0
    else
        echo "❌ ERROR ($code)"
        return 1
    fi
}

echo "=== CORE ==="
check "Health" "/q/health"
check "Metrics" "/q/metrics"
check "System Info" "/api/v11/info"

echo ""
echo "=== RICARDIAN CONTRACTS ==="
check "Gas Fees" "/api/v11/contracts/ricardian/gas-fees"
check "List Contracts" "/api/v11/contracts/ricardian"
check "Upload" "/api/v11/contracts/ricardian/upload"

echo ""
echo "=== BLOCKCHAIN CORE ==="
check "Blocks" "/api/v11/blockchain/blocks"
check "Transactions" "/api/v11/blockchain/transactions"
check "Network Stats" "/api/v11/blockchain/network/stats"

echo ""
echo "=== LIVE DATA ==="
check "Validators" "/api/v11/live/validators"
check "Consensus" "/api/v11/live/consensus"
check "Channels" "/api/v11/live/channels"
check "Network" "/api/v11/live/network"

echo ""
echo "=== CROSS-CHAIN BRIDGE ==="
check "Bridge Status" "/api/v11/bridge/status"
check "Bridge Chains" "/api/v11/bridge/chains"
check "Bridge History" "/api/v11/bridge/history"

echo ""
echo "=== DEFI ==="
check "DeFi Overview" "/api/v11/blockchain/defi"
check "Uniswap Pools" "/api/v11/blockchain/defi/uniswap/pools"
check "Aave Markets" "/api/v11/blockchain/defi/aave/markets"

echo ""
echo "=== HEALTHCARE (HMS) ==="
check "HMS Status" "/api/v11/hms/status"
check "HMS Records" "/api/v11/hms/records"
check "FHIR Patients" "/api/v11/hms/fhir/Patient"

echo ""
echo "=== AI/ML ==="
check "AI Status" "/api/v11/ai/status"
check "AI Predictions" "/api/v11/ai/predictions"
check "Anomaly Detection" "/api/v11/ai/anomalies"

echo ""
echo "=== REAL WORLD ASSETS ==="
check "RWA Assets" "/api/v11/rwa/assets"
check "RWA Valuations" "/api/v11/rwa/valuations"

echo ""
echo "=== GOVERNANCE ==="
check "Proposals" "/api/v11/blockchain/governance/proposals"
check "Voting Stats" "/api/v11/blockchain/governance/stats"

echo ""
echo "=== SECURITY & CRYPTO ==="
check "Security Status" "/api/v11/security/status"
check "Key Management" "/api/v11/security/keys"
check "Quantum Crypto" "/api/v11/security/quantum"

echo ""
echo "=== ENTERPRISE ==="
check "Enterprise Status" "/api/v11/enterprise/status"
check "Tenants" "/api/v11/enterprise/tenants"
check "SSO Config" "/api/v11/enterprise/sso"

echo ""
echo "=== VALIDATORS & STAKING ==="
check "Validators List" "/api/v11/blockchain/validators"
check "Staking Info" "/api/v11/blockchain/staking/info"

echo ""
echo "=== CONSENSUS & NETWORK ==="
check "Consensus Status" "/api/v11/consensus/status"
check "Network Peers" "/api/v11/network/peers"
check "Network Health" "/api/v11/network/health"

echo ""
echo "=== DATA FEEDS & ORACLES ==="
check "Data Feeds" "/api/v11/datafeeds"
check "Price Feed" "/api/v11/datafeeds/prices"
check "Oracle Status" "/api/v11/oracles/status"

echo ""
echo "=== ANALYTICS ==="
check "Analytics Dashboard" "/api/v11/analytics/dashboard"
check "Transaction Analytics" "/api/v11/analytics/transactions"
check "Performance Metrics" "/api/v11/analytics/performance"

echo ""
echo "=== PAYMENT CHANNELS ==="
check "Channels List" "/api/v11/channels"
check "Create Channel" "/api/v11/channels"

echo ""
echo "Check complete!"
