-- Aurigraph V11 Demos Table Migration
-- Created: October 24, 2025
-- Purpose: Persistent storage for demo configurations and data

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

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_demos_status ON demos(status);
CREATE INDEX IF NOT EXISTS idx_demos_expires_at ON demos(expires_at);
CREATE INDEX IF NOT EXISTS idx_demos_user_email ON demos(user_email);
CREATE INDEX IF NOT EXISTS idx_demos_created_at ON demos(created_at);

-- Create composite index for active demos query
CREATE INDEX IF NOT EXISTS idx_demos_active ON demos(status, expires_at);

-- Grant permissions (adjust as needed)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON demos TO aurigraph;

-- Insert sample demo data for initialization (disabled temporarily - use separate V2 migration)
-- INSERT INTO demos (id, demo_name, user_email, user_name, description, status, created_at, last_activity, expires_at, duration_minutes, channels_json, validators_json, business_nodes_json, slim_nodes_json)
-- VALUES
-- (
--     'demo_init_supply_chain_001',
--     'Supply Chain Tracking Demo',
--     'alice.johnson@enterprise.com',
--     'Alice Johnson',
--     'End-to-end supply chain visibility with real-time tracking',
--     'PENDING',
--     CURRENT_TIMESTAMP,
--     CURRENT_TIMESTAMP,
--     CURRENT_TIMESTAMP + INTERVAL '10 minutes',
--     10,
--     '[{"id":"ch1","name":"Production Channel","type":"PRIVATE"},{"id":"ch2","name":"Logistics Channel","type":"CONSORTIUM"}]',
--     '[{"id":"v1","name":"Validator Node 1","type":"VALIDATOR","endpoint":"https://validator1.demo","channelId":"ch1"},{"id":"v2","name":"Validator Node 2","type":"VALIDATOR","endpoint":"https://validator2.demo","channelId":"ch2"}]',
--     '[{"id":"b1","name":"Manufacturer Node","type":"BUSINESS","endpoint":"https://manufacturer.demo","channelId":"ch1"},{"id":"b2","name":"Distributor Node","type":"BUSINESS","endpoint":"https://distributor.demo","channelId":"ch2"}]',
--     '[{"id":"s1","name":"Retailer Node","type":"SLIM","endpoint":"https://retailer.demo","channelId":"ch2"}]'
-- );
--
-- INSERT INTO demos (id, demo_name, user_email, user_name, description, status, created_at, last_activity, expires_at, duration_minutes, channels_json, validators_json, business_nodes_json, slim_nodes_json)
-- VALUES
-- (
--     'demo_init_healthcare_001',
--     'Healthcare Records Management',
--     'robert.chen@healthorg.com',
--     'Dr. Robert Chen',
--     'Secure patient data sharing across healthcare providers',
--     'PENDING',
--     CURRENT_TIMESTAMP,
--     CURRENT_TIMESTAMP,
--     CURRENT_TIMESTAMP + INTERVAL '10 minutes',
--     10,
--     '[{"id":"hc1","name":"Patient Records","type":"PRIVATE"}]',
--     '[{"id":"hv1","name":"Hospital Validator","type":"VALIDATOR","endpoint":"https://hospital-val.demo","channelId":"hc1"}]',
--     '[{"id":"hb1","name":"Primary Care","type":"BUSINESS","endpoint":"https://primary-care.demo","channelId":"hc1"},{"id":"hb2","name":"Specialist Clinic","type":"BUSINESS","endpoint":"https://specialist.demo","channelId":"hc1"}]',
--     '[]'
-- );
--
-- INSERT INTO demos (id, demo_name, user_email, user_name, description, status, created_at, last_activity, expires_at, duration_minutes, channels_json, validators_json, business_nodes_json, slim_nodes_json)
-- VALUES
-- (
--     'demo_init_financial_001',
--     'Financial Settlement Network',
--     'sarah.martinez@fintech.com',
--     'Sarah Martinez',
--     'Real-time cross-border payment settlement',
--     'PENDING',
--     CURRENT_TIMESTAMP,
--     CURRENT_TIMESTAMP,
--     CURRENT_TIMESTAMP + INTERVAL '10 minutes',
--     10,
--     '[{"id":"fc1","name":"Payment Channel","type":"CONSORTIUM"}]',
--     '[{"id":"fv1","name":"Bank Validator 1","type":"VALIDATOR","endpoint":"https://bank1-val.demo","channelId":"fc1"},{"id":"fv2","name":"Bank Validator 2","type":"VALIDATOR","endpoint":"https://bank2-val.demo","channelId":"fc1"}]',
--     '[{"id":"fb1","name":"Bank A Node","type":"BUSINESS","endpoint":"https://banka.demo","channelId":"fc1"},{"id":"fb2","name":"Bank B Node","type":"BUSINESS","endpoint":"https://bankb.demo","channelId":"fc1"}]',
--     '[{"id":"fs1","name":"Payment Provider","type":"SLIM","endpoint":"https://payment.demo","channelId":"fc1"}]'
-- );

