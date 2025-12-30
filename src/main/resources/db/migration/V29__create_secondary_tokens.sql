-- ============================================================================
-- Migration V29: Create secondary_tokens table (parent table for versions)
-- Purpose: Base table for secondary tokens that versioning depends on
-- Author: Implementation Agent (IPA)
-- Date: December 26, 2025
-- ============================================================================

-- Create the base secondary_tokens table
CREATE TABLE secondary_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    primary_token_id UUID NOT NULL,
    token_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Indexes for common queries
    CONSTRAINT ck_st_status_valid
        CHECK (status IN ('CREATED', 'ACTIVE', 'SUSPENDED', 'RETIRED', 'ARCHIVED'))
);

-- Create indexes for query performance
CREATE INDEX idx_st_primary_token_id
    ON secondary_tokens(primary_token_id);

CREATE INDEX idx_st_token_type
    ON secondary_tokens(token_type);

CREATE INDEX idx_st_status
    ON secondary_tokens(status);

CREATE INDEX idx_st_created_at
    ON secondary_tokens(created_at);

-- Comment for documentation
COMMENT ON TABLE secondary_tokens IS 'Base table for secondary tokens that can have multiple versions';
COMMENT ON COLUMN secondary_tokens.primary_token_id IS 'Reference to primary token';
COMMENT ON COLUMN secondary_tokens.token_type IS 'Type of secondary token (e.g., WRAPPER, MIRROR, DERIVATIVE)';
COMMENT ON COLUMN secondary_tokens.status IS 'Current status: CREATED, ACTIVE, SUSPENDED, RETIRED, ARCHIVED';
