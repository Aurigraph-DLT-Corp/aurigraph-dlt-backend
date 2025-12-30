-- ============================================================================
-- Migration V32: Approval Execution Metadata & Audit Trail
-- Purpose: Create approval execution tracking and audit infrastructure
-- Story: Story 6 - Approval Execution & State Transitions
-- Author: Implementation Agent
-- Date: December 23, 2025
-- ============================================================================

BEGIN;

-- ============================================================================
-- PART 1: Create vvb_approval_requests table (Story 5 DDL)
-- ============================================================================

CREATE TABLE IF NOT EXISTS vvb_approval_requests (
    id BIGSERIAL PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    token_version_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    voting_window_end TIMESTAMP NOT NULL,
    voting_window_seconds BIGINT NOT NULL,
    approval_threshold DOUBLE PRECISION NOT NULL DEFAULT 66.67,
    total_validators INTEGER NOT NULL,
    approval_count INTEGER NOT NULL DEFAULT 0,
    rejection_count INTEGER NOT NULL DEFAULT 0,
    abstain_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    merkle_proof TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_var_voting_window_positive
        CHECK (voting_window_seconds > 0),
    CONSTRAINT ck_var_threshold_valid
        CHECK (approval_threshold > 0 AND approval_threshold <= 100),
    CONSTRAINT ck_var_validators_positive
        CHECK (total_validators > 0),
    CONSTRAINT ck_var_counts_non_negative
        CHECK (approval_count >= 0 AND rejection_count >= 0 AND abstain_count >= 0),
    CONSTRAINT ck_var_status_valid
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'))
);

-- Indexes for vvb_approval_requests
CREATE INDEX IF NOT EXISTS idx_var_request_id
    ON vvb_approval_requests(request_id);

CREATE INDEX IF NOT EXISTS idx_var_token_version_id
    ON vvb_approval_requests(token_version_id);

CREATE INDEX IF NOT EXISTS idx_var_status
    ON vvb_approval_requests(status);

CREATE INDEX IF NOT EXISTS idx_var_voting_window_end
    ON vvb_approval_requests(voting_window_end);

CREATE INDEX IF NOT EXISTS idx_var_token_status
    ON vvb_approval_requests(token_version_id, status);

CREATE INDEX IF NOT EXISTS idx_var_created_at
    ON vvb_approval_requests(created_at);

CREATE INDEX IF NOT EXISTS idx_var_pending_open
    ON vvb_approval_requests(status, voting_window_end)
    WHERE status = 'PENDING';

-- ============================================================================
-- PART 2: Add approval execution columns to secondary_token_versions
-- ============================================================================

ALTER TABLE secondary_token_versions
ADD COLUMN IF NOT EXISTS approval_request_id UUID,
ADD CONSTRAINT fk_stv_approval_request
    FOREIGN KEY (approval_request_id)
    REFERENCES vvb_approval_requests(request_id)
    ON DELETE SET NULL;

ALTER TABLE secondary_token_versions
ADD COLUMN IF NOT EXISTS approval_threshold_percentage DECIMAL(5,2),
ADD CONSTRAINT ck_stv_threshold_valid
    CHECK (approval_threshold_percentage IS NULL OR 
           (approval_threshold_percentage > 0 AND approval_threshold_percentage <= 100));

ALTER TABLE secondary_token_versions
ADD COLUMN IF NOT EXISTS approved_by_count INTEGER DEFAULT 0,
ADD CONSTRAINT ck_stv_approved_count_non_negative
    CHECK (approved_by_count >= 0);

ALTER TABLE secondary_token_versions
ADD COLUMN IF NOT EXISTS approval_timestamp TIMESTAMP;

ALTER TABLE secondary_token_versions
ADD COLUMN IF NOT EXISTS approvers_list TEXT;

ALTER TABLE secondary_token_versions
ADD COLUMN IF NOT EXISTS approval_expiry_deadline TIMESTAMP;

ALTER TABLE secondary_token_versions
ADD COLUMN IF NOT EXISTS previous_version_retired_at TIMESTAMP;

ALTER TABLE secondary_token_versions
ADD COLUMN IF NOT EXISTS activated_at TIMESTAMP;

-- ============================================================================
-- PART 3: Create indexes for approval execution queries
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_stv_approval_request_id
    ON secondary_token_versions(approval_request_id)
    WHERE approval_request_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_stv_activated_at
    ON secondary_token_versions(activated_at)
    WHERE activated_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_stv_status_activated
    ON secondary_token_versions(status, activated_at);

CREATE INDEX IF NOT EXISTS idx_stv_approval_expiry
    ON secondary_token_versions(approval_expiry_deadline)
    WHERE approval_expiry_deadline IS NOT NULL AND status = 'PENDING_VVB';

-- ============================================================================
-- PART 4: Create approval_execution_audit table
-- ============================================================================

CREATE TABLE IF NOT EXISTS approval_execution_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL,
    approval_request_id UUID,
    execution_phase VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50),
    executed_by VARCHAR(100),
    execution_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT,
    metadata JSONB,

    CONSTRAINT fk_aea_version
        FOREIGN KEY (version_id)
        REFERENCES secondary_token_versions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_aea_approval_request
        FOREIGN KEY (approval_request_id)
        REFERENCES vvb_approval_requests(request_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_aea_execution_phase_valid
        CHECK (execution_phase IN (
            'INITIATED', 
            'VALIDATED', 
            'TRANSITIONED', 
            'COMPLETED', 
            'FAILED', 
            'ROLLED_BACK'
        ))
);

-- Indexes for approval_execution_audit
CREATE INDEX IF NOT EXISTS idx_aea_version_id
    ON approval_execution_audit(version_id);

CREATE INDEX IF NOT EXISTS idx_aea_approval_request_id
    ON approval_execution_audit(approval_request_id);

CREATE INDEX IF NOT EXISTS idx_aea_execution_timestamp
    ON approval_execution_audit(execution_timestamp);

CREATE INDEX IF NOT EXISTS idx_aea_execution_phase
    ON approval_execution_audit(execution_phase);

CREATE INDEX IF NOT EXISTS idx_aea_version_timestamp
    ON approval_execution_audit(version_id, execution_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_aea_failures
    ON approval_execution_audit(execution_phase, execution_timestamp)
    WHERE execution_phase IN ('FAILED', 'ROLLED_BACK');

-- ============================================================================
-- End of Migration V32
-- ============================================================================

COMMIT;
