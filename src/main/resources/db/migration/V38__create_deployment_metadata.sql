-- V40__create_deployment_metadata.sql
-- Track deployment history and metadata for rollback support

-- Deployment history table
CREATE TABLE IF NOT EXISTS deployment_history (
    id VARCHAR(255) PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    deployment_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    deployer VARCHAR(255) NOT NULL,
    duration_seconds INTEGER,
    notes TEXT,
    CONSTRAINT deployment_history_version UNIQUE (version, environment)
);

-- Deployment artifacts (for rollback support)
CREATE TABLE IF NOT EXISTS deployment_artifacts (
    id VARCHAR(255) PRIMARY KEY,
    deployment_id VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    artifact_type VARCHAR(50) NOT NULL,
    artifact_location VARCHAR(2048) NOT NULL,
    checksum VARCHAR(255),
    size_bytes BIGINT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (deployment_id) REFERENCES deployment_history(id) ON DELETE CASCADE
);

CREATE INDEX idx_artifact_version ON deployment_artifacts(version);
CREATE INDEX idx_artifact_type ON deployment_artifacts(artifact_type);

-- Deployment logs
CREATE TABLE IF NOT EXISTS deployment_logs (
    id SERIAL PRIMARY KEY,
    deployment_id VARCHAR(255) NOT NULL,
    log_level VARCHAR(20) NOT NULL,
    log_message VARCHAR(4096),
    log_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (deployment_id) REFERENCES deployment_history(id) ON DELETE CASCADE
);

CREATE INDEX idx_deployment_logs ON deployment_logs(deployment_id);
CREATE INDEX idx_log_timestamp ON deployment_logs(log_timestamp);

-- Health check results after deployment
CREATE TABLE IF NOT EXISTS deployment_health_checks (
    id VARCHAR(255) PRIMARY KEY,
    deployment_id VARCHAR(255) NOT NULL,
    check_type VARCHAR(100) NOT NULL,
    check_name VARCHAR(255) NOT NULL,
    check_status VARCHAR(20) NOT NULL,
    response_time_ms INTEGER,
    check_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (deployment_id) REFERENCES deployment_history(id) ON DELETE CASCADE
);

CREATE INDEX idx_health_check_status ON deployment_health_checks(check_status);
CREATE INDEX idx_health_check_time ON deployment_health_checks(check_timestamp);

-- Performance baseline after deployment
CREATE TABLE IF NOT EXISTS deployment_performance (
    id VARCHAR(255) PRIMARY KEY,
    deployment_id VARCHAR(255) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    metric_value DOUBLE PRECISION,
    metric_unit VARCHAR(50),
    measured_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (deployment_id) REFERENCES deployment_history(id) ON DELETE CASCADE
);

CREATE INDEX idx_deployment_perf ON deployment_performance(deployment_id);
CREATE INDEX idx_metric_name ON deployment_performance(metric_name);

-- Rollback history
CREATE TABLE IF NOT EXISTS rollback_history (
    id VARCHAR(255) PRIMARY KEY,
    from_deployment_id VARCHAR(255) NOT NULL,
    to_deployment_id VARCHAR(255) NOT NULL,
    rollback_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(2048),
    status VARCHAR(50) NOT NULL,
    initiated_by VARCHAR(255),
    duration_seconds INTEGER,
    FOREIGN KEY (from_deployment_id) REFERENCES deployment_history(id) ON DELETE RESTRICT,
    FOREIGN KEY (to_deployment_id) REFERENCES deployment_history(id) ON DELETE RESTRICT
);

CREATE INDEX idx_rollback_time ON rollback_history(rollback_time);
CREATE INDEX idx_rollback_status ON rollback_history(status);

-- Deployment pre-checks
CREATE TABLE IF NOT EXISTS deployment_prechecks (
    id VARCHAR(255) PRIMARY KEY,
    deployment_id VARCHAR(255) NOT NULL,
    check_name VARCHAR(255) NOT NULL,
    check_result VARCHAR(50) NOT NULL,
    details TEXT,
    check_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (deployment_id) REFERENCES deployment_history(id) ON DELETE CASCADE
);

CREATE INDEX idx_precheck_result ON deployment_prechecks(check_result);

-- Incident tracking during deployments
CREATE TABLE IF NOT EXISTS deployment_incidents (
    id VARCHAR(255) PRIMARY KEY,
    deployment_id VARCHAR(255) NOT NULL,
    incident_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description VARCHAR(4096),
    resolved BOOLEAN DEFAULT FALSE,
    incident_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (deployment_id) REFERENCES deployment_history(id) ON DELETE CASCADE
);

CREATE INDEX idx_incident_severity ON deployment_incidents(severity);
CREATE INDEX idx_incident_resolved ON deployment_incidents(resolved);

-- Deployment config snapshot (for reproducibility)
CREATE TABLE IF NOT EXISTS deployment_config_snapshot (
    id VARCHAR(255) PRIMARY KEY,
    deployment_id VARCHAR(255) NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    config_value TEXT NOT NULL,
    config_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (deployment_id) REFERENCES deployment_history(id) ON DELETE CASCADE
);

CREATE INDEX idx_config_key ON deployment_config_snapshot(config_key);

-- Create views for deployment reporting
CREATE VIEW deployment_summary AS
SELECT
    DATE(deployment_time) as deployment_date,
    environment,
    COUNT(*) as deployment_count,
    SUM(CASE WHEN status = 'SUCCESSFUL' THEN 1 ELSE 0 END) as successful,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed,
    AVG(duration_seconds) as avg_duration_seconds
FROM deployment_history
GROUP BY DATE(deployment_time), environment;

CREATE VIEW recent_deployments AS
SELECT
    version,
    environment,
    deployment_time,
    status,
    deployer,
    duration_seconds
FROM deployment_history
ORDER BY deployment_time DESC
LIMIT 20;

CREATE VIEW deployment_quality AS
SELECT
    dh.version,
    dh.environment,
    COUNT(DISTINCT CASE WHEN dhc.check_status = 'PASSED' THEN dhc.id END) as health_checks_passed,
    COUNT(DISTINCT CASE WHEN dhc.check_status = 'FAILED' THEN dhc.id END) as health_checks_failed,
    AVG(dhc.response_time_ms) as avg_response_time_ms,
    COUNT(DISTINCT di.id) as total_incidents
FROM deployment_history dh
LEFT JOIN deployment_health_checks dhc ON dh.id = dhc.deployment_id
LEFT JOIN deployment_incidents di ON dh.id = di.deployment_id
GROUP BY dh.version, dh.environment;
