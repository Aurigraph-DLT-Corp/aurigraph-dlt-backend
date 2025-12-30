-- V11__Create_Oracle_Verification_Tables.sql
-- Purpose: Create tables for oracle verification tracking and audit trail
-- Sprint: Sprint 16 - Oracle Verification Database Schema (AV11-494)
-- Date: 2025-11-25

-- ============================================================================
-- TABLE 1: oracle_verifications
-- Purpose: Main results table for oracle verification processes
-- ============================================================================

CREATE TABLE IF NOT EXISTS oracle_verifications (
    verification_id VARCHAR(64) PRIMARY KEY,
    asset_id VARCHAR(128) NOT NULL,
    claimed_value DECIMAL(24, 8) NOT NULL,
    verified_value DECIMAL(24, 8),
    consensus_percent DECIMAL(5, 2),
    is_verified BOOLEAN NOT NULL,
    oracles_consulted INT NOT NULL,
    oracles_responded INT NOT NULL,
    verification_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Validation constraints
    CONSTRAINT chk_consensus_percent CHECK (consensus_percent >= 0 AND consensus_percent <= 100),
    CONSTRAINT chk_oracles_responded CHECK (oracles_responded <= oracles_consulted),
    CONSTRAINT chk_positive_values CHECK (claimed_value >= 0 AND (verified_value IS NULL OR verified_value >= 0))
);

-- Create indexes for optimal query performance
CREATE INDEX idx_oracle_ver_asset_id ON oracle_verifications(asset_id);
CREATE INDEX idx_oracle_ver_verification_timestamp ON oracle_verifications(verification_timestamp);
CREATE INDEX idx_oracle_ver_is_verified ON oracle_verifications(is_verified);
CREATE INDEX idx_oracle_ver_created_at ON oracle_verifications(created_at);

-- ============================================================================
-- TABLE 2: oracle_verification_details
-- Purpose: Detailed audit trail for individual oracle responses
-- ============================================================================

CREATE TABLE IF NOT EXISTS oracle_verification_details (
    detail_id VARCHAR(64) PRIMARY KEY,
    verification_id VARCHAR(64) NOT NULL,
    oracle_id VARCHAR(128) NOT NULL,
    oracle_name VARCHAR(256),
    reported_price DECIMAL(24, 8),
    signature_verified BOOLEAN,
    response_time_ms INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to parent verification
    CONSTRAINT fk_verification_id FOREIGN KEY (verification_id)
        REFERENCES oracle_verifications(verification_id)
        ON DELETE CASCADE,

    -- Validation constraints
    CONSTRAINT chk_response_time CHECK (response_time_ms IS NULL OR response_time_ms >= 0),
    CONSTRAINT chk_reported_price CHECK (reported_price IS NULL OR reported_price >= 0)
);

-- Create indexes for optimal query performance
CREATE INDEX idx_oracle_det_verification_id ON oracle_verification_details(verification_id);
CREATE INDEX idx_oracle_det_oracle_id ON oracle_verification_details(oracle_id);
CREATE INDEX idx_oracle_det_oracle_name ON oracle_verification_details(oracle_name);
CREATE INDEX idx_oracle_det_created_at ON oracle_verification_details(created_at);

-- ============================================================================
-- TABLE COMMENTS AND DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE oracle_verifications IS
'Stores aggregate results of oracle verification processes. Each row represents a complete verification cycle where multiple oracles were consulted to verify an asset value claim.';

COMMENT ON COLUMN oracle_verifications.verification_id IS
'Unique identifier for the verification process (e.g., UUID or hash)';

COMMENT ON COLUMN oracle_verifications.asset_id IS
'Identifier of the asset being verified (e.g., token address, asset symbol)';

COMMENT ON COLUMN oracle_verifications.claimed_value IS
'The value claimed by the submitter that needs verification';

COMMENT ON COLUMN oracle_verifications.verified_value IS
'The consensus value determined by the oracle network (NULL if verification failed)';

COMMENT ON COLUMN oracle_verifications.consensus_percent IS
'Percentage of oracle consensus achieved (0-100)';

COMMENT ON COLUMN oracle_verifications.is_verified IS
'Boolean flag indicating if the verification passed the required threshold';

COMMENT ON COLUMN oracle_verifications.oracles_consulted IS
'Total number of oracles contacted for this verification';

COMMENT ON COLUMN oracle_verifications.oracles_responded IS
'Number of oracles that successfully responded';

COMMENT ON COLUMN oracle_verifications.verification_timestamp IS
'Timestamp when the verification process was initiated';

COMMENT ON COLUMN oracle_verifications.created_at IS
'Record creation timestamp';

COMMENT ON TABLE oracle_verification_details IS
'Audit trail table storing individual oracle responses for each verification. Provides full traceability of which oracles participated and what values they reported.';

COMMENT ON COLUMN oracle_verification_details.detail_id IS
'Unique identifier for this detail record (e.g., UUID)';

COMMENT ON COLUMN oracle_verification_details.verification_id IS
'Foreign key reference to the parent verification process';

COMMENT ON COLUMN oracle_verification_details.oracle_id IS
'Unique identifier of the oracle (e.g., public key, address)';

COMMENT ON COLUMN oracle_verification_details.oracle_name IS
'Human-readable name of the oracle service';

COMMENT ON COLUMN oracle_verification_details.reported_price IS
'The price value reported by this specific oracle (NULL if oracle failed to respond)';

COMMENT ON COLUMN oracle_verification_details.signature_verified IS
'Boolean indicating if the oracle''s cryptographic signature was successfully verified';

COMMENT ON COLUMN oracle_verification_details.response_time_ms IS
'Response time in milliseconds for this oracle (NULL if timeout or failure)';

COMMENT ON COLUMN oracle_verification_details.created_at IS
'Record creation timestamp';
