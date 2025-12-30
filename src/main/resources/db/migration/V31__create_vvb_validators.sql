-- V31__create_vvb_validators.sql
-- VVB Validator Configuration Tables
-- Creates infrastructure for Verified Valuator Board validators and approval rules

CREATE TABLE IF NOT EXISTS vvb_validators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(100) NOT NULL,
    approval_authority VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for vvb_validators
CREATE INDEX idx_vvb_validators_role ON vvb_validators(role);
CREATE INDEX idx_vvb_validators_active ON vvb_validators(active);

CREATE TABLE IF NOT EXISTS vvb_approval_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    change_type VARCHAR(100) NOT NULL UNIQUE,
    requires_vvb BOOLEAN DEFAULT TRUE,
    role_required VARCHAR(100),
    approval_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for vvb_approval_rules
CREATE INDEX idx_vvb_approval_rules_change_type ON vvb_approval_rules(change_type);
CREATE INDEX idx_vvb_approval_rules_role ON vvb_approval_rules(role_required);

-- Insert default validators
INSERT INTO vvb_validators (name, role, approval_authority, active)
VALUES
    ('VVB_VALIDATOR_1', 'VVB_VALIDATOR', 'STANDARD', TRUE),
    ('VVB_VALIDATOR_2', 'VVB_VALIDATOR', 'STANDARD', TRUE),
    ('VVB_ADMIN_1', 'VVB_ADMIN', 'ELEVATED', TRUE),
    ('VVB_ADMIN_2', 'VVB_ADMIN', 'ELEVATED', TRUE)
ON CONFLICT (name) DO NOTHING;

-- Insert default approval rules
INSERT INTO vvb_approval_rules (change_type, requires_vvb, role_required, approval_type)
VALUES
    ('SECONDARY_TOKEN_CREATE', TRUE, 'VVB_VALIDATOR', 'STANDARD'),
    ('SECONDARY_TOKEN_RETIRE', TRUE, 'VVB_ADMIN', 'ELEVATED'),
    ('PRIMARY_TOKEN_RETIRE', TRUE, 'VVB_ADMIN', 'CRITICAL'),
    ('TOKEN_SUSPENSION', TRUE, 'VVB_ADMIN', 'ELEVATED'),
    ('SECONDARY_TOKEN_ACTIVATE', FALSE, NULL, 'STANDARD'),
    ('SECONDARY_TOKEN_REDEEM', FALSE, NULL, 'STANDARD'),
    ('TOKEN_TRANSFER', FALSE, NULL, 'STANDARD')
ON CONFLICT (change_type) DO NOTHING;
