-- V7__Create_Auth_Tokens_Table.sql
-- Create auth_tokens table for JWT token storage and lifecycle management

CREATE TABLE IF NOT EXISTS auth_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(36) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    user_email VARCHAR(100) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    token_type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    client_ip VARCHAR(45),
    user_agent VARCHAR(255),
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revocation_reason VARCHAR(255),
    revoked_at TIMESTAMP,
    parent_token_id VARCHAR(36),
    is_refreshed BOOLEAN NOT NULL DEFAULT FALSE,
    refresh_token_id VARCHAR(36),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    metadata TEXT
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_user_id ON auth_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_token_hash ON auth_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_expires_at ON auth_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_is_revoked ON auth_tokens(is_revoked);
CREATE INDEX IF NOT EXISTS idx_created_at ON auth_tokens(created_at);
CREATE INDEX IF NOT EXISTS idx_status ON auth_tokens(status);
CREATE INDEX IF NOT EXISTS idx_user_id_status ON auth_tokens(user_id, status);
CREATE INDEX IF NOT EXISTS idx_user_id_token_type ON auth_tokens(user_id, token_type);
CREATE INDEX IF NOT EXISTS idx_client_ip ON auth_tokens(client_ip);
CREATE INDEX IF NOT EXISTS idx_parent_token_id ON auth_tokens(parent_token_id);

-- Add foreign key constraint to users table (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'fk_auth_tokens_user_id') THEN
        ALTER TABLE auth_tokens
        ADD CONSTRAINT fk_auth_tokens_user_id
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE;
    END IF;
END $$;

-- Create table for token statistics/audit
CREATE TABLE IF NOT EXISTS auth_token_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    token_type VARCHAR(20),
    client_ip VARCHAR(45),
    user_agent VARCHAR(255),
    status VARCHAR(20),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_user_id ON auth_token_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON auth_token_audit(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_action ON auth_token_audit(action);

-- Add foreign key to users table (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'fk_audit_user_id') THEN
        ALTER TABLE auth_token_audit
        ADD CONSTRAINT fk_audit_user_id
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE;
    END IF;
END $$;

-- Create materialized view for token statistics
CREATE VIEW token_statistics AS
SELECT 
    u.id as user_id,
    u.email,
    COUNT(DISTINCT CASE WHEN at.status = 'ACTIVE' THEN at.id END) as active_tokens,
    COUNT(DISTINCT CASE WHEN at.token_type = 'ACCESS' AND at.status = 'ACTIVE' THEN at.id END) as active_access_tokens,
    COUNT(DISTINCT CASE WHEN at.token_type = 'REFRESH' AND at.status = 'ACTIVE' THEN at.id END) as active_refresh_tokens,
    COUNT(DISTINCT CASE WHEN at.is_revoked = true THEN at.id END) as revoked_tokens,
    COUNT(DISTINCT CASE WHEN at.status = 'EXPIRED' THEN at.id END) as expired_tokens,
    MAX(at.last_used_at) as last_token_used,
    MAX(at.created_at) as most_recent_token,
    COUNT(DISTINCT at.client_ip) as unique_ips
FROM users u
LEFT JOIN auth_tokens at ON u.id = at.user_id
GROUP BY u.id, u.email;

-- Log initial creation
-- This migration:
-- 1. Creates auth_tokens table for JWT token storage
-- 2. Adds multiple indexes for efficient queries
-- 3. Adds foreign key constraint to users table
-- 4. Creates audit table for token operations
-- 5. Creates statistics view for monitoring
-- 6. Supports token revocation and lifecycle management
-- 7. Tracks token usage and client information
