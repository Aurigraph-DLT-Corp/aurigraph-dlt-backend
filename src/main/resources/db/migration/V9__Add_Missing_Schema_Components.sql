-- V9__Add_Missing_Schema_Components.sql
-- Purpose: Create missing demos table and add missing columns to existing tables
-- Created: November 21, 2025
-- Context: V1 migration (demos table) wasn't executed properly, creating blocker for V11 startup

-- =========================================================================
-- 1. Create demos table (from V1__Create_Demos_Table.sql which wasn't executed)
-- =========================================================================
CREATE TABLE IF NOT EXISTS demos (
    id VARCHAR(64) NOT NULL,
    demo_name VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 10,
    is_admin_demo BOOLEAN NOT NULL DEFAULT FALSE,
    transaction_count BIGINT NOT NULL DEFAULT 0,
    merkle_root VARCHAR(64),
    channels_json TEXT,
    validators_json TEXT,
    business_nodes_json TEXT,
    slim_nodes_json TEXT,
    CONSTRAINT demos_pk PRIMARY KEY (id)
);

-- Create indexes for common queries on demos
CREATE INDEX IF NOT EXISTS idx_demos_status ON demos(status);
CREATE INDEX IF NOT EXISTS idx_demos_expires_at ON demos(expires_at);
CREATE INDEX IF NOT EXISTS idx_demos_user_email ON demos(user_email);
CREATE INDEX IF NOT EXISTS idx_demos_created_at ON demos(created_at);
CREATE INDEX IF NOT EXISTS idx_demos_active ON demos(status, expires_at);

-- =========================================================================
-- 2. Add missing is_system_role column to roles table
-- =========================================================================
ALTER TABLE roles ADD COLUMN IF NOT EXISTS is_system_role BOOLEAN DEFAULT FALSE;

-- =========================================================================
-- 3. Add missing permission and user_count columns if needed
-- =========================================================================
ALTER TABLE roles ADD COLUMN IF NOT EXISTS permissions TEXT;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS user_count INTEGER DEFAULT 0;

-- =========================================================================
-- 4. Update system roles to mark them as system roles
-- =========================================================================
UPDATE roles
SET is_system_role = TRUE
WHERE name IN ('ADMIN', 'USER', 'DEVOPS', 'API_USER', 'READONLY')
  AND is_system_role IS NOT TRUE;

-- Ensure default system roles exist
INSERT INTO roles (id, name, description, is_system_role, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'API_USER', 'API user - Application integration access', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'READONLY', 'Read-only user - View-only access', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO UPDATE SET
    is_system_role = TRUE,
    updated_at = CURRENT_TIMESTAMP;
