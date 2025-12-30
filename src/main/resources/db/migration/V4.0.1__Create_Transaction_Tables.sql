-- Sprint 4: Create Transaction and Performance Tables

-- Create TRANSACTION table (optimized for 2M+ TPS)
CREATE TABLE IF NOT EXISTS transaction (
    id BIGSERIAL PRIMARY KEY,
    tx_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    from_address VARCHAR(255) NOT NULL,
    to_address VARCHAR(255),
    amount NUMERIC(38, 18) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    gas_limit BIGINT,
    gas_used BIGINT,
    gas_price NUMERIC(38, 18) NOT NULL,
    total_fee NUMERIC(38, 18) NOT NULL,
    nonce BIGINT,
    data JSONB,
    block_number BIGINT,
    block_hash VARCHAR(255),
    confirmation_count INTEGER DEFAULT 0,
    finality_time_ms BIGINT,
    error_code VARCHAR(100),
    error_message TEXT,
    optimization_score NUMERIC(5, 2),
    created_at TIMESTAMP NOT NULL,
    submitted_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    finalized_at TIMESTAMP,
    updated_at TIMESTAMP
) PARTITION BY RANGE (CAST(EXTRACT(EPOCH FROM created_at) as BIGINT));

-- Create partitions by date for better performance
CREATE TABLE transaction_2025_01 PARTITION OF transaction
    FOR VALUES FROM (1735689600) TO (1738281600); -- Jan 2025

CREATE TABLE transaction_2025_02 PARTITION OF transaction
    FOR VALUES FROM (1738281600) TO (1740873600); -- Feb 2025

CREATE TABLE transaction_2025_03 PARTITION OF transaction
    FOR VALUES FROM (1740873600) TO (1743465600); -- Mar 2025

CREATE TABLE transaction_2025_04 PARTITION OF transaction
    FOR VALUES FROM (1743465600) TO (1746057600); -- Apr 2025

-- Create BRIN indexes for time-series queries (optimized for high TPS)
CREATE INDEX idx_transaction_created_at_brin ON transaction
    USING BRIN (created_at) WITH (pages_per_range = 32);

CREATE INDEX idx_transaction_user_id ON transaction(user_id);
CREATE INDEX idx_transaction_status ON transaction(status);
CREATE INDEX idx_transaction_tx_hash ON transaction(tx_hash);
CREATE INDEX idx_transaction_block_number ON transaction(block_number);
CREATE INDEX idx_transaction_from_address ON transaction(from_address);

-- Create TRANSACTION_LOG table for audit trail
CREATE TABLE IF NOT EXISTS transaction_log (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transaction(id),
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    status_change_reason TEXT,
    changed_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_transaction_log_transaction_id ON transaction_log(transaction_id);
CREATE INDEX idx_transaction_log_created_at ON transaction_log(created_at);

-- Create BATCH_PROCESS table
CREATE TABLE IF NOT EXISTS batch_process (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    transaction_count INTEGER NOT NULL,
    successful_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    total_amount NUMERIC(38, 18),
    total_fee NUMERIC(38, 18),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_batch_process_user_id ON batch_process(user_id);
CREATE INDEX idx_batch_process_status ON batch_process(status);
CREATE INDEX idx_batch_process_created_at ON batch_process(created_at);

-- Create OPTIMIZATION table for ML model tracking
CREATE TABLE IF NOT EXISTS optimization (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transaction(id),
    model_version VARCHAR(100),
    optimization_type VARCHAR(100),
    score NUMERIC(5, 2),
    input_data JSONB,
    output_data JSONB,
    execution_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_optimization_transaction_id ON optimization(transaction_id);
CREATE INDEX idx_optimization_created_at ON optimization(created_at);

-- Create PERFORMANCE_METRICS table (time-series optimized)
CREATE TABLE IF NOT EXISTS performance_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_type VARCHAR(100) NOT NULL,
    value NUMERIC(19, 4) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    period_seconds INTEGER,
    min_value NUMERIC(19, 4),
    max_value NUMERIC(19, 4),
    avg_value NUMERIC(19, 4),
    percentile_95 NUMERIC(19, 4),
    percentile_99 NUMERIC(19, 4),
    sample_count BIGINT,
    tags JSONB,
    created_at TIMESTAMP NOT NULL
) PARTITION BY RANGE (CAST(EXTRACT(EPOCH FROM timestamp) as BIGINT));

-- Create partitions for metrics by month
CREATE TABLE metrics_2025_01 PARTITION OF performance_metrics
    FOR VALUES FROM (1735689600) TO (1738281600);

CREATE TABLE metrics_2025_02 PARTITION OF performance_metrics
    FOR VALUES FROM (1738281600) TO (1740873600);

CREATE TABLE metrics_2025_03 PARTITION OF performance_metrics
    FOR VALUES FROM (1740873600) TO (1743465600);

CREATE TABLE metrics_2025_04 PARTITION OF performance_metrics
    FOR VALUES FROM (1743465600) TO (1746057600);

-- Create BRIN index for time-series metric queries
CREATE INDEX idx_performance_metrics_timestamp_brin ON performance_metrics
    USING BRIN (timestamp) WITH (pages_per_range = 32);

CREATE INDEX idx_performance_metrics_metric_type ON performance_metrics(metric_type);

-- Create PERFORMANCE_STATS table for aggregated stats
CREATE TABLE IF NOT EXISTS performance_stats (
    id BIGSERIAL PRIMARY KEY,
    hour TIMESTAMP NOT NULL,
    metric_type VARCHAR(100) NOT NULL,
    avg_value NUMERIC(19, 4),
    min_value NUMERIC(19, 4),
    max_value NUMERIC(19, 4),
    percentile_95 NUMERIC(19, 4),
    percentile_99 NUMERIC(19, 4),
    sample_count BIGINT,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_performance_stats_hour_metric ON performance_stats(hour, metric_type);
CREATE INDEX idx_performance_stats_created_at ON performance_stats(created_at);

-- Add RLS (Row Level Security) policies
ALTER TABLE transaction ENABLE ROW LEVEL SECURITY;
ALTER TABLE transaction_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE batch_process ENABLE ROW LEVEL SECURITY;
ALTER TABLE optimization ENABLE ROW LEVEL SECURITY;

-- Create audit trigger for transaction status changes
CREATE OR REPLACE FUNCTION audit_transaction_status_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'UPDATE' AND OLD.status != NEW.status) THEN
        INSERT INTO transaction_log (transaction_id, old_status, new_status, status_change_reason, changed_at)
        VALUES (NEW.id, OLD.status, NEW.status, 'Status update', NOW());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER transaction_audit_trigger
AFTER UPDATE ON transaction
FOR EACH ROW
EXECUTE FUNCTION audit_transaction_status_changes();

-- Create function to aggregate metrics hourly
CREATE OR REPLACE FUNCTION aggregate_hourly_metrics()
RETURNS void AS $$
BEGIN
    INSERT INTO performance_stats (hour, metric_type, avg_value, min_value, max_value, percentile_95, percentile_99, sample_count, created_at)
    SELECT
        DATE_TRUNC('hour', timestamp) as hour,
        metric_type,
        AVG(value) as avg_value,
        MIN(value) as min_value,
        MAX(value) as max_value,
        PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY value) as percentile_95,
        PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY value) as percentile_99,
        COUNT(*) as sample_count,
        NOW() as created_at
    FROM performance_metrics
    WHERE timestamp >= NOW() - INTERVAL '2 hours'
    GROUP BY hour, metric_type
    ON CONFLICT (hour, metric_type) DO UPDATE
    SET avg_value = EXCLUDED.avg_value,
        min_value = EXCLUDED.min_value,
        max_value = EXCLUDED.max_value,
        percentile_95 = EXCLUDED.percentile_95,
        percentile_99 = EXCLUDED.percentile_99,
        sample_count = EXCLUDED.sample_count;
END;
$$ LANGUAGE plpgsql;

-- Create vacuum and analyze strategy for high throughput
ALTER TABLE transaction SET (autovacuum_vacuum_scale_factor = 0.01, autovacuum_analyze_scale_factor = 0.005);
ALTER TABLE performance_metrics SET (autovacuum_vacuum_scale_factor = 0.01, autovacuum_analyze_scale_factor = 0.005);
