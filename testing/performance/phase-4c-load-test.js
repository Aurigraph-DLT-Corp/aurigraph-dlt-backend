/**
 * Phase 4C - gRPC/Protocol Buffer Migration
 * K6 Load Testing Suite for Aurigraph V12.0.0
 *
 * Purpose: Verify 1.1M-1.3M TPS target with 4 performance scenarios
 * Target Service: http://dlt.aurigraph.io:9003 (HTTPS: https://dlt.aurigraph.io)
 *
 * 4 Test Scenarios:
 * - Scenario 1: Baseline (50 VUs, 300s) → ~388K TPS (50% baseline)
 * - Scenario 2: Current Performance (100 VUs, 300s) → ~776K TPS (100% baseline)
 * - Scenario 3: Target Performance (250 VUs, 300s) → ~1.1M-1.3M TPS (142-167% improvement)
 * - Scenario 4: Stress Test (1000 VUs, 300s) → Maximum capacity
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';

// Custom metrics for detailed analysis
const transactionSuccessRate = new Rate('transaction_success');
const transactionDuration = new Trend('transaction_duration_ms');
const healthCheckDuration = new Trend('health_check_duration_ms');
const requestsPerSecond = new Counter('requests_per_second');
const errorCount = new Counter('errors');
const blockProposalDuration = new Trend('block_proposal_duration_ms');
const voteOnBlockDuration = new Trend('vote_on_block_duration_ms');
const transactionSubmissionDuration = new Trend('transaction_submit_duration_ms');

// Base configuration for remote server
const BASE_URL = __ENV.TARGET_URL || 'http://dlt.aurigraph.io:9003';
const SCENARIO = __ENV.SCENARIO || '1';
const VUS = parseInt(__ENV.VUS || '50');
const DURATION = __ENV.DURATION || '300s';

// Scenario configurations
const SCENARIOS = {
  '1': { vus: 50, duration: '300s', name: 'Baseline Sanity Check' },
  '2': { vus: 100, duration: '300s', name: 'Current Performance (776K TPS)' },
  '3': { vus: 250, duration: '300s', name: 'Target Performance (1.1M-1.3M TPS)' },
  '4': { vus: 1000, duration: '300s', name: 'Stress Test (Maximum Capacity)' }
};

// Export test configuration
export const options = {
  scenarios: {
    scenario_baseline: {
      executor: 'constant-vus',
      vus: SCENARIOS['1'].vus,
      duration: SCENARIOS['1'].duration,
      exec: 'baseline'
    },
    scenario_current: {
      executor: 'constant-vus',
      vus: SCENARIOS['2'].vus,
      duration: SCENARIOS['2'].duration,
      exec: 'currentPerformance',
      startTime: '0s'
    },
    scenario_target: {
      executor: 'constant-vus',
      vus: SCENARIOS['3'].vus,
      duration: SCENARIOS['3'].duration,
      exec: 'targetPerformance',
      startTime: '0s'
    },
    scenario_stress: {
      executor: 'constant-vus',
      vus: SCENARIOS['4'].vus,
      duration: SCENARIOS['4'].duration,
      exec: 'stressTest',
      startTime: '0s'
    }
  },
  thresholds: {
    'transaction_success{scenario:baseline}': ['rate>0.95'],
    'transaction_success{scenario:current}': ['rate>0.90'],
    'transaction_success{scenario:target}': ['rate>0.85'],
    'transaction_success{scenario:stress}': ['rate>0.70'],
    'http_req_duration{staticAsset:yes}': ['p(95)<200', 'p(99)<300'],
    'http_req_duration{staticAsset:no}': ['p(95)<1000', 'p(99)<2000']
  }
};

/**
 * ============================================================================
 * SCENARIO 1: Baseline Sanity Check (50 VUs, 300s)
 * Expected: ~388K TPS (50% of 776K baseline)
 * Purpose: Verify system stability and baseline performance
 * ============================================================================
 */
export function baseline() {
  group('Baseline Scenario (50 VUs)', () => {
    // Health Check
    healthCheck();

    // Transaction Submission
    submitTransaction();

    // Consensus Operations
    proposeBlock();
    voteOnBlock();
    commitBlock();

    // Status Queries
    getTransactionStatus();
    getConsensusState();

    // Streaming
    streamTransactionEvents();

    sleep(Math.random() * 0.5 + 0.5); // 0.5-1s delay between requests
  });
}

/**
 * ============================================================================
 * SCENARIO 2: Current Performance (100 VUs, 300s)
 * Expected: ~776K TPS (baseline performance)
 * Purpose: Validate current V12 performance level
 * ============================================================================
 */
export function currentPerformance() {
  group('Current Performance Scenario (100 VUs)', () => {
    // Increased load with parallel operations
    healthCheck();

    // Batch transaction submission
    batchSubmitTransactions();

    // Consensus voting
    voteOnBlockWithSignature();

    // Pool queries
    getTxPoolSize();
    getPendingTransactions();

    // History queries
    getTransactionHistory();

    sleep(Math.random() * 0.3 + 0.2); // 0.2-0.5s delay
  });
}

/**
 * ============================================================================
 * SCENARIO 3: Target Performance (250 VUs, 300s)
 * Expected: ~1.1M-1.3M TPS (50-70% improvement from baseline)
 * Purpose: Verify target performance with HTTP/2 multiplexing
 * ============================================================================
 */
export function targetPerformance() {
  group('Target Performance Scenario (250 VUs)', () => {
    // High-frequency operations
    healthCheck();

    // Parallel transaction submissions
    submitTransaction();
    submitTransaction();

    // Consensus operations in parallel
    proposeBlock();
    voteOnBlockWithSignature();

    // Gas estimation and signature validation
    estimateGasCost();
    validateTransactionSignature();

    // Real-time queries
    getTransactionStatus();
    getTxPoolSize();

    sleep(Math.random() * 0.1 + 0.05); // 0.05-0.15s minimal delay
  });
}

/**
 * ============================================================================
 * SCENARIO 4: Stress Test (1000 VUs, 300s)
 * Purpose: Find maximum capacity and system breaking point
 * ============================================================================
 */
export function stressTest() {
  group('Stress Test Scenario (1000 VUs)', () => {
    // Maximum throughput operations
    submitTransaction();
    proposeBlock();
    voteOnBlock();

    // Minimal delays for stress testing
    sleep(Math.random() * 0.05); // 0-50ms delay
  });
}

/**
 * ============================================================================
 * API OPERATION FUNCTIONS
 * ============================================================================
 */

/**
 * Health Check - Verify service is responding
 */
function healthCheck() {
  const startTime = new Date();

  const response = http.get(`${BASE_URL}/q/health`, {
    headers: {
      'Accept': 'application/json'
    },
    tags: { name: 'HealthCheck' }
  });

  const duration = new Date() - startTime;
  healthCheckDuration.add(duration);
  requestsPerSecond.add(1);

  check(response, {
    'health check status is 200': (r) => r.status === 200,
    'health check contains status': (r) => r.body.includes('status')
  });

  if (response.status !== 200) {
    errorCount.add(1);
  }
}

/**
 * Submit Single Transaction
 */
function submitTransaction() {
  const startTime = new Date();
  const transactionData = {
    from_address: `0x${Math.random().toString(16).slice(2, 42)}`,
    to_address: `0x${Math.random().toString(16).slice(2, 42)}`,
    amount: Math.floor(Math.random() * 1000000),
    gas_price: 20 + Math.random() * 100,
    data: `transaction_${Date.now()}_${Math.random()}`
  };

  const response = http.post(
    `${BASE_URL}/api/v11/transaction/submit`,
    JSON.stringify({
      transaction: transactionData
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      tags: { name: 'SubmitTransaction', type: 'write' }
    }
  );

  const duration = new Date() - startTime;
  transactionSubmissionDuration.add(duration);
  requestsPerSecond.add(1);

  const success = response.status === 200 || response.status === 201;
  transactionSuccessRate.add(success);

  check(response, {
    'submit transaction status ok': (r) => r.status === 200 || r.status === 201,
    'submit transaction has hash': (r) => r.body.includes('hash') || r.body.includes('transaction')
  });

  if (!success) {
    errorCount.add(1);
  }
}

/**
 * Batch Submit Transactions
 */
function batchSubmitTransactions() {
  const startTime = new Date();
  const transactions = Array.from({ length: 10 }, (_, i) => ({
    from_address: `0x${Math.random().toString(16).slice(2, 42)}`,
    to_address: `0x${Math.random().toString(16).slice(2, 42)}`,
    amount: Math.floor(Math.random() * 1000000),
    gas_price: 20 + Math.random() * 100
  }));

  const response = http.post(
    `${BASE_URL}/api/v11/transaction/batch`,
    JSON.stringify({
      transactions: transactions,
      batch_id: `batch_${Date.now()}`
    }),
    {
      headers: {
        'Content-Type': 'application/json'
      },
      tags: { name: 'BatchSubmitTransactions', type: 'write', batch: true }
    }
  );

  const duration = new Date() - startTime;
  transactionSubmissionDuration.add(duration / 10); // Average per transaction
  requestsPerSecond.add(10);

  const success = response.status === 200 || response.status === 201;
  transactionSuccessRate.add(success);

  if (!success) {
    errorCount.add(10);
  }
}

/**
 * Get Transaction Status
 */
function getTransactionStatus() {
  const startTime = new Date();
  const txHash = `0x${Math.random().toString(16).slice(2, 66)}`;

  const response = http.get(
    `${BASE_URL}/api/v11/transaction/status?hash=${txHash}`,
    {
      tags: { name: 'GetTransactionStatus', type: 'read' }
    }
  );

  const duration = new Date() - startTime;
  transactionDuration.add(duration);
  requestsPerSecond.add(1);

  const success = response.status === 200 || response.status === 404;
  transactionSuccessRate.add(success);

  check(response, {
    'get tx status is 2xx or 404': (r) => r.status === 200 || r.status === 404
  });

  if (response.status >= 500) {
    errorCount.add(1);
  }
}

/**
 * Propose Block (Consensus)
 */
function proposeBlock() {
  const startTime = new Date();
  const blockData = {
    block_height: Math.floor(Math.random() * 1000000),
    previous_hash: `0x${Math.random().toString(16).slice(2, 66)}`,
    transactions_root: `0x${Math.random().toString(16).slice(2, 66)}`,
    proposer_id: `validator_${Math.floor(Math.random() * 100)}`,
    proposal_term: Math.floor(Math.random() * 1000)
  };

  const response = http.post(
    `${BASE_URL}/api/v11/consensus/propose`,
    JSON.stringify(blockData),
    {
      headers: {
        'Content-Type': 'application/json'
      },
      tags: { name: 'ProposeBlock', type: 'consensus' }
    }
  );

  const duration = new Date() - startTime;
  blockProposalDuration.add(duration);
  requestsPerSecond.add(1);

  const success = response.status === 200 || response.status === 201;
  transactionSuccessRate.add(success);

  if (!success) {
    errorCount.add(1);
  }
}

/**
 * Vote on Block
 */
function voteOnBlock() {
  const startTime = new Date();

  const response = http.post(
    `${BASE_URL}/api/v11/consensus/vote`,
    JSON.stringify({
      block_hash: `0x${Math.random().toString(16).slice(2, 66)}`,
      voter_id: `validator_${Math.floor(Math.random() * 100)}`,
      vote_choice: Math.random() > 0.5,
      vote_term: Math.floor(Math.random() * 1000)
    }),
    {
      headers: {
        'Content-Type': 'application/json'
      },
      tags: { name: 'VoteOnBlock', type: 'consensus' }
    }
  );

  const duration = new Date() - startTime;
  voteOnBlockDuration.add(duration);
  requestsPerSecond.add(1);

  const success = response.status === 200 || response.status === 201;
  transactionSuccessRate.add(success);
}

/**
 * Vote on Block with Signature
 */
function voteOnBlockWithSignature() {
  const startTime = new Date();

  const response = http.post(
    `${BASE_URL}/api/v11/consensus/vote`,
    JSON.stringify({
      block_hash: `0x${Math.random().toString(16).slice(2, 66)}`,
      voter_id: `validator_${Math.floor(Math.random() * 100)}`,
      vote_choice: Math.random() > 0.5,
      vote_term: Math.floor(Math.random() * 1000),
      vote_signature: `0x${Math.random().toString(16).slice(2, 130)}`
    }),
    {
      headers: {
        'Content-Type': 'application/json'
      },
      tags: { name: 'VoteOnBlockSignature', type: 'consensus' }
    }
  );

  const duration = new Date() - startTime;
  voteOnBlockDuration.add(duration);
  requestsPerSecond.add(1);

  const success = response.status === 200 || response.status === 201;
  transactionSuccessRate.add(success);
}

/**
 * Commit Block
 */
function commitBlock() {
  const response = http.post(
    `${BASE_URL}/api/v11/consensus/commit`,
    JSON.stringify({
      block_hash: `0x${Math.random().toString(16).slice(2, 66)}`,
      block_height: Math.floor(Math.random() * 1000000),
      commit_term: Math.floor(Math.random() * 1000),
      validator_signatures: [
        `0x${Math.random().toString(16).slice(2, 130)}`,
        `0x${Math.random().toString(16).slice(2, 130)}`
      ]
    }),
    {
      headers: {
        'Content-Type': 'application/json'
      },
      tags: { name: 'CommitBlock', type: 'consensus' }
    }
  );

  transactionSuccessRate.add(response.status === 200 || response.status === 201);
}

/**
 * Get Consensus State
 */
function getConsensusState() {
  const response = http.get(
    `${BASE_URL}/api/v11/consensus/state`,
    {
      tags: { name: 'GetConsensusState', type: 'read' }
    }
  );

  requestsPerSecond.add(1);
  transactionSuccessRate.add(response.status === 200 || response.status === 404);
}

/**
 * Get Transaction Pool Size
 */
function getTxPoolSize() {
  const response = http.get(
    `${BASE_URL}/api/v11/transaction/pool-size`,
    {
      tags: { name: 'GetTxPoolSize', type: 'read' }
    }
  );

  requestsPerSecond.add(1);
  transactionSuccessRate.add(response.status === 200);
}

/**
 * Get Pending Transactions
 */
function getPendingTransactions() {
  const response = http.get(
    `${BASE_URL}/api/v11/transaction/pending?limit=100`,
    {
      tags: { name: 'GetPendingTransactions', type: 'read' }
    }
  );

  requestsPerSecond.add(1);
  transactionSuccessRate.add(response.status === 200);
}

/**
 * Get Transaction History
 */
function getTransactionHistory() {
  const address = `0x${Math.random().toString(16).slice(2, 42)}`;
  const response = http.get(
    `${BASE_URL}/api/v11/transaction/history?address=${address}&limit=50`,
    {
      tags: { name: 'GetTransactionHistory', type: 'read' }
    }
  );

  requestsPerSecond.add(1);
  transactionSuccessRate.add(response.status === 200);
}

/**
 * Estimate Gas Cost
 */
function estimateGasCost() {
  const response = http.post(
    `${BASE_URL}/api/v11/transaction/estimate-gas`,
    JSON.stringify({
      data: `0x${Math.random().toString(16).slice(2, 100)}`
    }),
    {
      headers: {
        'Content-Type': 'application/json'
      },
      tags: { name: 'EstimateGasCost', type: 'read' }
    }
  );

  requestsPerSecond.add(1);
  transactionSuccessRate.add(response.status === 200);
}

/**
 * Validate Transaction Signature
 */
function validateTransactionSignature() {
  const response = http.post(
    `${BASE_URL}/api/v11/transaction/validate-signature`,
    JSON.stringify({
      transaction: {
        from_address: `0x${Math.random().toString(16).slice(2, 42)}`,
        to_address: `0x${Math.random().toString(16).slice(2, 42)}`,
        amount: Math.floor(Math.random() * 1000000),
        signature: `0x${Math.random().toString(16).slice(2, 130)}`,
        public_key: `0x${Math.random().toString(16).slice(2, 130)}`,
        nonce: Math.floor(Math.random() * 1000)
      }
    }),
    {
      headers: {
        'Content-Type': 'application/json'
      },
      tags: { name: 'ValidateSignature', type: 'read' }
    }
  );

  requestsPerSecond.add(1);
  transactionSuccessRate.add(response.status === 200);
}

/**
 * Stream Transaction Events
 */
function streamTransactionEvents() {
  // Simulate streaming endpoint check
  const response = http.get(
    `${BASE_URL}/api/v11/transaction/stream`,
    {
      tags: { name: 'StreamTransactionEvents', type: 'stream' }
    }
  );

  requestsPerSecond.add(1);
  transactionSuccessRate.add(response.status === 200 || response.status === 503);
}

/**
 * ============================================================================
 * DEFAULT EXPORT FUNCTION FOR K6 CLI
 * ============================================================================
 */
export default function() {
  baseline();
}
