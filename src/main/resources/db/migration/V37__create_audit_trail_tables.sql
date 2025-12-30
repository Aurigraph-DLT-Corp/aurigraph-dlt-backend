-- V39__create_audit_trail_tables.sql
-- Create immutable audit trail tables for compliance

-- Main audit records table
CREATE TABLE IF NOT EXISTS audit_records (
    id VARCHAR(255) PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    created_by VARCHAR(255),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    merkle_hash VARCHAR(255),
    archived BOOLEAN DEFAULT FALSE,
    CONSTRAINT audit_records_entity_idx UNIQUE (entity_type, entity_id, operation, timestamp)
);

-- Audit record details (key-value pairs)
CREATE TABLE IF NOT EXISTS audit_record_details (
    id SERIAL PRIMARY KEY,
    audit_record_id VARCHAR(255) NOT NULL,
    detail_key VARCHAR(255) NOT NULL,
    detail_value VARCHAR(2048),
    FOREIGN KEY (audit_record_id) REFERENCES audit_records(id) ON DELETE CASCADE
);

-- Immutability verification chain
CREATE TABLE IF NOT EXISTS audit_chain (
    id VARCHAR(255) PRIMARY KEY,
    previous_record_id VARCHAR(255),
    current_record_id VARCHAR(255) NOT NULL,
    merkle_hash VARCHAR(255),
    chain_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (current_record_id) REFERENCES audit_records(id) ON DELETE CASCADE,
    FOREIGN KEY (previous_record_id) REFERENCES audit_records(id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_audit_entity_id ON audit_records(entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_records(actor);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_records(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_merkle_hash ON audit_records(merkle_hash);
CREATE INDEX IF NOT EXISTS idx_audit_archived ON audit_records(archived);
CREATE INDEX IF NOT EXISTS idx_audit_operation ON audit_records(operation);

-- Performance monitoring
CREATE INDEX IF NOT EXISTS idx_audit_details_key ON audit_record_details(detail_key);
CREATE INDEX IF NOT EXISTS idx_audit_chain_timestamp ON audit_chain(chain_timestamp);

-- Audit summary view (for reporting)
CREATE VIEW audit_summary AS
SELECT
    DATE(timestamp) as audit_date,
    operation,
    entity_type,
    COUNT(*) as record_count,
    COUNT(DISTINCT actor) as unique_actors
FROM audit_records
WHERE archived = FALSE
GROUP BY DATE(timestamp), operation, entity_type;

-- Audit access log (for tracking access to audit data)
CREATE TABLE IF NOT EXISTS audit_access_log (
    id SERIAL PRIMARY KEY,
    accessor VARCHAR(255) NOT NULL,
    accessed_record_id VARCHAR(255),
    access_type VARCHAR(50) NOT NULL,
    access_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (accessed_record_id) REFERENCES audit_records(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_access_timestamp ON audit_access_log(access_timestamp);
CREATE INDEX IF NOT EXISTS idx_accessor ON audit_access_log(accessor);

-- Compliance certifications table
CREATE TABLE IF NOT EXISTS compliance_certifications (
    id VARCHAR(255) PRIMARY KEY,
    entity_id VARCHAR(255) NOT NULL,
    certification_type VARCHAR(100) NOT NULL,
    certified_date TIMESTAMP NOT NULL,
    expires_date TIMESTAMP,
    certifier VARCHAR(255) NOT NULL,
    details JSON,
    CONSTRAINT compliance_cert_unique UNIQUE (entity_id, certification_type, certified_date)
);

-- Create index for compliance lookups
CREATE INDEX IF NOT EXISTS idx_compliance_entity ON compliance_certifications(entity_id);
CREATE INDEX IF NOT EXISTS idx_compliance_type ON compliance_certifications(certification_type);
CREATE INDEX IF NOT EXISTS idx_compliance_expires ON compliance_certifications(expires_date);
