-- V12: Create WebSocket Subscriptions Table
-- Sprint 16 - AV11-484: WebSocket Authentication & Subscription Management
-- Author: WebSocket Development Agent (WDA)
-- Date: 2025-11-25

-- Create websocket_subscriptions table
CREATE TABLE IF NOT EXISTS websocket_subscriptions (
    subscription_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    channel VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    priority INTEGER NOT NULL DEFAULT 0,
    rate_limit INTEGER NOT NULL DEFAULT 100,
    message_count BIGINT NOT NULL DEFAULT 0,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    metadata TEXT,

    -- Constraints
    CONSTRAINT check_status CHECK (status IN ('ACTIVE', 'PAUSED', 'SUSPENDED', 'EXPIRED')),
    CONSTRAINT check_priority CHECK (priority >= 0 AND priority <= 10),
    CONSTRAINT check_rate_limit CHECK (rate_limit > 0 AND rate_limit <= 10000),

    -- Unique constraint: One subscription per user-channel pair
    CONSTRAINT unique_user_channel UNIQUE (user_id, channel)
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_ws_user_id ON websocket_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_ws_channel ON websocket_subscriptions(channel);
CREATE INDEX IF NOT EXISTS idx_ws_status ON websocket_subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_ws_expires_at ON websocket_subscriptions(expires_at);
CREATE INDEX IF NOT EXISTS idx_ws_user_status ON websocket_subscriptions(user_id, status);

-- Create trigger to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_websocket_subscriptions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_websocket_subscriptions_updated_at
    BEFORE UPDATE ON websocket_subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_websocket_subscriptions_updated_at();

-- Insert default system subscriptions (if needed)
INSERT INTO websocket_subscriptions (subscription_id, user_id, channel, status, priority, rate_limit)
VALUES
    (gen_random_uuid(), 'system', 'system', 'ACTIVE', 10, 1000),
    (gen_random_uuid(), 'system', 'transactions', 'ACTIVE', 8, 1000),
    (gen_random_uuid(), 'system', 'consensus', 'ACTIVE', 9, 500)
ON CONFLICT (user_id, channel) DO NOTHING;

-- Add comments for documentation
COMMENT ON TABLE websocket_subscriptions IS 'Stores WebSocket channel subscriptions for users with persistence across sessions';
COMMENT ON COLUMN websocket_subscriptions.subscription_id IS 'Unique subscription identifier (UUID)';
COMMENT ON COLUMN websocket_subscriptions.user_id IS 'User ID who owns this subscription';
COMMENT ON COLUMN websocket_subscriptions.channel IS 'Channel name (e.g., transactions, consensus, system)';
COMMENT ON COLUMN websocket_subscriptions.status IS 'Subscription status: ACTIVE, PAUSED, SUSPENDED, EXPIRED';
COMMENT ON COLUMN websocket_subscriptions.priority IS 'Message priority for this subscription (0-10, higher = more important)';
COMMENT ON COLUMN websocket_subscriptions.rate_limit IS 'Maximum messages per minute for this subscription';
COMMENT ON COLUMN websocket_subscriptions.message_count IS 'Total number of messages delivered to this subscription';
COMMENT ON COLUMN websocket_subscriptions.last_message_at IS 'Timestamp of last message delivery';
COMMENT ON COLUMN websocket_subscriptions.metadata IS 'Additional subscription metadata (JSON format)';
