-- Migration: V1__create_bridge_chain_config_table.sql
-- Purpose: Create the bridge_chain_config table for storing blockchain chain configurations
-- Date: 2025-11-18

CREATE TABLE IF NOT EXISTS bridge_chain_config (
    id BIGSERIAL PRIMARY KEY,
    chain_name VARCHAR(100) NOT NULL UNIQUE,
    chain_id VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    rpc_url VARCHAR(2048) NOT NULL,
    backup_rpc_urls VARCHAR(5000),
    block_time_ms BIGINT NOT NULL,
    confirmations_required INTEGER NOT NULL,
    chain_family VARCHAR(50) NOT NULL,
    min_bridge_amount NUMERIC(38, 18) NOT NULL,
    max_bridge_amount NUMERIC(38, 18) NOT NULL,
    base_fee_percent NUMERIC(5, 4) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    contract_addresses TEXT,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

-- Create indexes for optimal query performance
CREATE INDEX idx_chain_name ON bridge_chain_config(chain_name);
CREATE INDEX idx_chain_family ON bridge_chain_config(chain_family);
CREATE INDEX idx_enabled ON bridge_chain_config(enabled);
CREATE INDEX idx_created_at ON bridge_chain_config(created_at);
CREATE INDEX idx_updated_at ON bridge_chain_config(updated_at);

-- Insert sample chain configurations for initial setup
INSERT INTO bridge_chain_config (
    chain_name, chain_id, display_name, rpc_url, block_time_ms, confirmations_required,
    chain_family, min_bridge_amount, max_bridge_amount, base_fee_percent, notes
) VALUES
-- Ethereum Mainnet (EVM)
(
    'ethereum',
    '1',
    'Ethereum Mainnet',
    'https://eth-mainnet.g.alchemy.com/v2/demo',
    12000,
    15,
    'EVM',
    '0.1',
    '1000',
    '0.001',
    'Ethereum mainnet configuration for testing. Replace RPC URL with valid credentials.'
),
-- Polygon (EVM)
(
    'polygon',
    '137',
    'Polygon (Matic)',
    'https://polygon-rpc.com/',
    2000,
    256,
    'EVM',
    '10',
    '10000',
    '0.0005',
    'Polygon mainnet configuration. Uses Polygon RPC endpoint.'
),
-- Solana (SOLANA)
(
    'solana',
    'mainnet-beta',
    'Solana Mainnet',
    'https://api.mainnet-beta.solana.com',
    400,
    32,
    'SOLANA',
    '0.01',
    '100',
    '0.0002',
    'Solana mainnet configuration. Uses official Solana RPC endpoint.'
);

-- Add comment explaining the table
COMMENT ON TABLE bridge_chain_config IS 'Stores blockchain chain configurations for cross-chain bridge operations. Each row represents a supported blockchain with its RPC endpoint, fees, and operational parameters.';

COMMENT ON COLUMN bridge_chain_config.chain_name IS 'Unique identifier for the chain (e.g., ethereum, polygon, solana). Lowercase, hyphen-separated.';
COMMENT ON COLUMN bridge_chain_config.chain_id IS 'Network identifier for the chain (e.g., 1 for Ethereum mainnet, 137 for Polygon)';
COMMENT ON COLUMN bridge_chain_config.rpc_url IS 'Primary RPC endpoint URL for connecting to the blockchain';
COMMENT ON COLUMN bridge_chain_config.backup_rpc_urls IS 'Backup RPC URLs separated by semicolons for failover support';
COMMENT ON COLUMN bridge_chain_config.block_time_ms IS 'Average block time in milliseconds (e.g., 12000 for Ethereum, 400 for Solana)';
COMMENT ON COLUMN bridge_chain_config.confirmations_required IS 'Number of block confirmations required for transaction finality';
COMMENT ON COLUMN bridge_chain_config.chain_family IS 'Chain family classification (EVM, SOLANA, COSMOS, SUBSTRATE, LAYER2, UTXO, OTHER)';
COMMENT ON COLUMN bridge_chain_config.min_bridge_amount IS 'Minimum bridge amount in native token units (prevents dust transactions)';
COMMENT ON COLUMN bridge_chain_config.max_bridge_amount IS 'Maximum bridge amount in native token units (limits single transaction concentration)';
COMMENT ON COLUMN bridge_chain_config.base_fee_percent IS 'Base fee as percentage (e.g., 0.001 = 0.1%) applied to all bridge transactions';
COMMENT ON COLUMN bridge_chain_config.enabled IS 'Whether this chain is currently enabled for bridging operations';
COMMENT ON COLUMN bridge_chain_config.contract_addresses IS 'JSON object mapping contract identifiers to addresses (e.g., {\"htlc\": \"0x...\", \"bridge\": \"0x...\"})';
COMMENT ON COLUMN bridge_chain_config.metadata IS 'Extensible JSON object for chain-specific metadata (e.g., programId for Solana, derivationPath for HD wallets)';
COMMENT ON COLUMN bridge_chain_config.notes IS 'Free-form notes for documentation and troubleshooting';
