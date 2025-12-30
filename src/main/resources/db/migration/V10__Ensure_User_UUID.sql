-- V10__Ensure_User_UUID.sql
-- Purpose: Ensure users table exists and id column is UUID type
-- Created: November 24, 2025

-- 1. Ensure pgcrypto extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Create users table if it doesn't exist (Defensive)
-- Note: We assume roles table exists as it is referenced in User.java
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP
);

-- 3. If table exists, ensure ID is UUID
-- This block handles the case where the table existed with VARCHAR id
DO $$
BEGIN
    -- Check if id column is not uuid
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users'
        AND column_name = 'id'
        AND data_type != 'uuid'
    ) THEN
        -- Alter column to UUID
        ALTER TABLE users ALTER COLUMN id TYPE UUID USING id::uuid;
        ALTER TABLE users ALTER COLUMN id SET DEFAULT gen_random_uuid();
    END IF;
END $$;
