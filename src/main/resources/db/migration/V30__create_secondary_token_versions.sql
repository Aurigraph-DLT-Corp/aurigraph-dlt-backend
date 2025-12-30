-- ============================================================================
-- Migration V30: Create secondary_token_versions table
-- Purpose: Store versioned snapshots of secondary tokens
-- Author: Implementation Agent (IPA)
-- Date: December 23, 2025
-- ============================================================================

-- Create the secondary_token_versions table
CREATE TABLE secondary_token_versions (
    id UUID PRIMARY KEY,
    secondary_token_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    content JSONB NOT NULL,
    status VARCHAR(50) NOT NULL,
    merkle_hash VARCHAR(64),
    previous_version_id UUID,
    replaced_at TIMESTAMP,
    replaced_by_version_id UUID,
    vvb_required BOOLEAN DEFAULT false,
    vvb_approved_at TIMESTAMP,
    vvb_approved_by VARCHAR(256),
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_stv_secondary_token
        FOREIGN KEY (secondary_token_id)
        REFERENCES secondary_tokens(id),
    CONSTRAINT fk_stv_previous_version
        FOREIGN KEY (previous_version_id)
        REFERENCES secondary_token_versions(id),
    CONSTRAINT fk_stv_replaced_by
        FOREIGN KEY (replaced_by_version_id)
        REFERENCES secondary_token_versions(id),

    -- Unique constraint for version numbering per token
    CONSTRAINT uk_stv_token_version
        UNIQUE (secondary_token_id, version_number)
);

-- Create indexes for query performance
CREATE INDEX idx_stv_secondary_token_id
    ON secondary_token_versions(secondary_token_id);

CREATE INDEX idx_stv_version_num
    ON secondary_token_versions(version_number);

CREATE INDEX idx_stv_status
    ON secondary_token_versions(status);

CREATE INDEX idx_stv_token_status
    ON secondary_token_versions(secondary_token_id, status);

CREATE INDEX idx_stv_created_at
    ON secondary_token_versions(created_at);

-- Partial index for VVB pending approvals (common query)
CREATE INDEX idx_stv_vvb_pending
    ON secondary_token_versions(secondary_token_id, status)
    WHERE status = 'PENDING_VVB';

-- Index for merkle hash lookups
CREATE INDEX idx_stv_merkle_hash
    ON secondary_token_versions(merkle_hash);

-- Index for version chain queries
CREATE INDEX idx_stv_previous_version_id
    ON secondary_token_versions(previous_version_id);

-- Index for replacement tracking
CREATE INDEX idx_stv_replaced_by_version_id
    ON secondary_token_versions(replaced_by_version_id);

-- Index for archived versions (retention/cleanup queries)
CREATE INDEX idx_stv_archived_at
    ON secondary_token_versions(archived_at);

-- Composite index for active version lookup (most common query)
CREATE INDEX idx_stv_active_lookup
    ON secondary_token_versions(secondary_token_id, version_number DESC)
    WHERE status = 'ACTIVE';

-- ============================================================================
-- Additional Constraints and Defaults
-- ============================================================================

-- Comment for documentation
COMMENT ON TABLE secondary_token_versions IS 'Stores versioned snapshots of secondary tokens with full lifecycle management';
COMMENT ON COLUMN secondary_token_versions.secondary_token_id IS 'Reference to parent secondary token';
COMMENT ON COLUMN secondary_token_versions.version_number IS 'Version sequence number (1, 2, 3, ...)';
COMMENT ON COLUMN secondary_token_versions.content IS 'Version content as JSON';
COMMENT ON COLUMN secondary_token_versions.status IS 'Current status: CREATED, PENDING_VVB, ACTIVE, REPLACED, REJECTED, EXPIRED, ARCHIVED';
COMMENT ON COLUMN secondary_token_versions.merkle_hash IS 'SHA-256 hash of content for integrity verification';
COMMENT ON COLUMN secondary_token_versions.vvb_required IS 'Whether this version requires VVB approval';
COMMENT ON COLUMN secondary_token_versions.vvb_approved_at IS 'Timestamp of VVB approval';
COMMENT ON COLUMN secondary_token_versions.vvb_approved_by IS 'Identifier of VVB approver';
COMMENT ON COLUMN secondary_token_versions.rejection_reason IS 'Reason for rejection (if rejected)';

-- ============================================================================
-- Data Validation Checks
-- ============================================================================

-- Check that version numbers are positive
ALTER TABLE secondary_token_versions
ADD CONSTRAINT ck_stv_version_number_positive
CHECK (version_number > 0);

-- Check that status is valid
ALTER TABLE secondary_token_versions
ADD CONSTRAINT ck_stv_status_valid
CHECK (status IN ('CREATED', 'PENDING_VVB', 'ACTIVE', 'REPLACED', 'REJECTED', 'EXPIRED', 'ARCHIVED'));

-- Check that only one ACTIVE version per token
-- (This is enforced at application layer, but documented here)
-- SQL constraint not used due to PostgreSQL limitations with conditional unique
COMMENT ON CONSTRAINT uk_stv_token_version ON secondary_token_versions IS
    'Ensures each secondary token has unique version numbers';
