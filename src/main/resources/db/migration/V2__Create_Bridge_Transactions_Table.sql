-- =========================================================================
-- V2__Create_Bridge_Transactions_Table.sql
-- =========================================================================
-- Liquibase database migration for Aurigraph V11 Bridge Persistence
--
-- Creates the main bridge_transactions table for persistent storage of
-- cross-chain bridge transactions with full lifecycle tracking.
--
-- Key Features:
-- - UUID-based transaction IDs (unique, indexed)
-- - Full transaction lifecycle tracking (PENDING -> CONFIRMING -> COMPLETED/FAILED)
-- - HTLC (Hash Time-Locked Contract) state management
-- - Multi-signature validation tracking
-- - Optimistic locking for concurrent access control
-- - Comprehensive indexing for query performance
--
-- Changelog:
-- 2025-10-29: Initial creation for Sprint 14 database persistence
-- =========================================================================

-- Create the main bridge_transactions table
CREATE TABLE IF NOT EXISTS bridge_transactions (
    id BIGSERIAL PRIMARY KEY,

    -- Business Keys
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    source_chain VARCHAR(32) NOT NULL,
    target_chain VARCHAR(32) NOT NULL,

    -- Addresses
    source_address VARCHAR(128) NOT NULL,
    target_address VARCHAR(128) NOT NULL,

    -- Token Information
    token_contract VARCHAR(128),
    token_symbol VARCHAR(16) NOT NULL,

    -- Amounts (precision 36, scale 18 for crypto precision)
    amount NUMERIC(36, 18) NOT NULL CHECK (amount > 0),
    bridge_fee NUMERIC(36, 18) NOT NULL CHECK (bridge_fee >= 0),

    -- Transaction Status
    status VARCHAR(32) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,

    -- HTLC Fields (Hash Time-Locked Contracts)
    htlc_hash VARCHAR(64),
    htlc_secret VARCHAR(64),
    htlc_timeout BIGINT,

    -- On-Chain Transaction Hashes
    source_tx_hash VARCHAR(128),
    target_tx_hash VARCHAR(128),

    -- Confirmations
    confirmations INTEGER DEFAULT 0 CHECK (confirmations >= 0),
    required_confirmations INTEGER DEFAULT 12 CHECK (required_confirmations > 0),

    -- Error Handling
    error_message VARCHAR(512),

    -- Retry Mechanism
    retry_count INTEGER DEFAULT 0 CHECK (retry_count >= 0),
    max_retries INTEGER DEFAULT 3 CHECK (max_retries > 0),

    -- Multi-Signature Validation
    multi_sig_validated BOOLEAN DEFAULT FALSE,
    validator_count INTEGER DEFAULT 0 CHECK (validator_count >= 0),

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    -- Optimistic Locking
    version BIGINT DEFAULT 0
);

-- =========================================================================
-- Indexes for Query Performance
-- =========================================================================

-- Primary lookup index (transaction ID)
CREATE UNIQUE INDEX IF NOT EXISTS idx_tx_id
    ON bridge_transactions (transaction_id);


-- Status filtering index
CREATE INDEX IF NOT EXISTS idx_status
    ON bridge_transactions (status);


-- Time-based queries (for stuck transfer detection)
CREATE INDEX IF NOT EXISTS idx_created
    ON bridge_transactions (created_at);


-- Address-based lookups
CREATE INDEX IF NOT EXISTS idx_source_address
    ON bridge_transactions (source_address);


CREATE INDEX IF NOT EXISTS idx_target_address
    ON bridge_transactions (target_address);


-- Chain-based lookups
CREATE INDEX IF NOT EXISTS idx_source_chain
    ON bridge_transactions (source_chain);


CREATE INDEX IF NOT EXISTS idx_target_chain
    ON bridge_transactions (target_chain);


-- Composite index for common queries (status + time)
CREATE INDEX IF NOT EXISTS idx_status_created
    ON bridge_transactions (status, created_at);


-- HTLC-related queries
CREATE INDEX IF NOT EXISTS idx_htlc_hash
    ON bridge_transactions (htlc_hash)
    WHERE htlc_hash IS NOT NULL;


-- Multi-sig validation queries
CREATE INDEX IF NOT EXISTS idx_multi_sig_validated
    ON bridge_transactions (multi_sig_validated, status);


-- =========================================================================
-- Table Comments and Documentation
-- =========================================================================

COMMENT ON TABLE bridge_transactions IS
'Bridge transactions table for cross-chain transfer persistence.
Stores the complete lifecycle of bridge operations including HTLC state,
multi-signature validation, and recovery information.';

COMMENT ON COLUMN bridge_transactions.transaction_id IS
'Unique transaction identifier (UUID format) - business key for tracking';

COMMENT ON COLUMN bridge_transactions.status IS
'Transaction status lifecycle: PENDING, CONFIRMING, COMPLETED, FAILED, REFUNDED';

COMMENT ON COLUMN bridge_transactions.htlc_hash IS
'SHA-256 hash of HTLC secret (commitment used in atomic swaps)';

COMMENT ON COLUMN bridge_transactions.htlc_secret IS
'HTLC secret revealed when claiming funds (null until revealed)';

COMMENT ON COLUMN bridge_transactions.htlc_timeout IS
'Unix epoch milliseconds - timeout for HTLC (when funds can be refunded)';

COMMENT ON COLUMN bridge_transactions.multi_sig_validated IS
'Flag indicating if validator network quorum (4/7) has been reached';

COMMENT ON COLUMN bridge_transactions.version IS
'Optimistic locking version - incremented on each update to detect conflicts';

-- =========================================================================
-- Trigger for Updated At Timestamp
-- =========================================================================

CREATE OR REPLACE FUNCTION update_bridge_transactions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS bridge_transactions_updated_at_trigger ON bridge_transactions;

CREATE TRIGGER bridge_transactions_updated_at_trigger
BEFORE UPDATE ON bridge_transactions
FOR EACH ROW
EXECUTE FUNCTION update_bridge_transactions_updated_at();

-- =========================================================================
-- Grant Permissions
-- =========================================================================

-- Application user read/write access
-- NOTE: Roles must exist before granting permissions
-- Create roles in initialization script or database setup
-- GRANT SELECT, INSERT, UPDATE ON bridge_transactions TO aurigraph_app;
-- GRANT SELECT ON bridge_transactions TO aurigraph_readonly;

-- Sequence access for ID generation
-- GRANT USAGE, SELECT ON SEQUENCE bridge_transactions_id_seq TO aurigraph_app;

-- =========================================================================
-- End of Migration
-- =========================================================================
