-- V42: Create CRM Communications Table
-- Purpose: Track email and communication campaigns, templates, and delivery status
-- Created: December 27, 2025

-- Create ENUM types
CREATE TYPE communication_channel AS ENUM ('email', 'sms', 'push_notification', 'in_app_message', 'linkedin', 'whatsapp');
CREATE TYPE communication_type AS ENUM ('inquiry_response', 'demo_confirmation', 'demo_reminder', 'follow_up', 'nurture_sequence', 'marketing', 'transactional');
CREATE TYPE delivery_status AS ENUM ('draft', 'queued', 'sent', 'delivered', 'opened', 'clicked', 'bounced', 'failed', 'spam_reported', 'unsubscribed');

-- Create communications table
CREATE TABLE IF NOT EXISTS communications (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign Keys
    lead_id UUID NOT NULL,
    created_by_user_id UUID,
    template_id UUID,

    -- Communication Details
    channel communication_channel DEFAULT 'email' NOT NULL,
    communication_type communication_type NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    html_body TEXT,
    plain_text_body TEXT,

    -- Recipient Information
    recipient_email VARCHAR(255),
    recipient_name VARCHAR(255),
    recipient_phone VARCHAR(20),
    reply_to_email VARCHAR(255),

    -- Delivery Status
    status delivery_status DEFAULT 'draft' NOT NULL,
    message_id VARCHAR(500),  -- External service message ID (for SendGrid, Mailgun, Twilio, etc.)
    external_provider VARCHAR(100),  -- sendgrid, mailgun, twilio, etc.

    -- Delivery Tracking
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    opened_at TIMESTAMP WITH TIME ZONE,
    first_click_at TIMESTAMP WITH TIME ZONE,
    last_click_at TIMESTAMP WITH TIME ZONE,
    bounced_at TIMESTAMP WITH TIME ZONE,
    spam_reported_at TIMESTAMP WITH TIME ZONE,
    unsubscribed_at TIMESTAMP WITH TIME ZONE,

    -- Engagement Metrics
    open_count INTEGER DEFAULT 0,
    click_count INTEGER DEFAULT 0,
    bounce_type VARCHAR(50),  -- soft, hard, complaint
    bounce_reason TEXT,

    -- Links Tracking
    tracked_links_json JSONB,  -- JSON array of {url, click_count, first_click_at, last_click_at}

    -- Campaign Information
    campaign_name VARCHAR(255),
    campaign_id UUID,
    sequence_position INTEGER,  -- Position in automation sequence
    sequence_id UUID,  -- Reference to automation sequence

    -- A/B Testing
    variant_id VARCHAR(50),  -- For A/B testing (A, B, etc.)
    control_group BOOLEAN DEFAULT FALSE,  -- For A/B testing control group

    -- Scheduling
    scheduled_at TIMESTAMP WITH TIME ZONE,
    send_later BOOLEAN DEFAULT FALSE,

    -- UTM Parameters
    utm_source VARCHAR(100),
    utm_medium VARCHAR(100),
    utm_campaign VARCHAR(100),
    utm_content VARCHAR(100),
    utm_term VARCHAR(100),

    -- Personalization & Dynamic Content
    personalization_applied BOOLEAN DEFAULT FALSE,
    personalization_tokens JSONB,  -- {first_name, company_name, etc.}
    dynamic_content_blocks JSONB,

    -- Opt-out Information
    can_resend BOOLEAN DEFAULT TRUE,
    do_not_send_until DATE,
    suppress_reason VARCHAR(255),

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,

    -- Constraints
    CONSTRAINT communications_lead_fk FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE,
    CONSTRAINT communications_created_by_fk FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for common queries
CREATE INDEX idx_communications_lead_id ON communications(lead_id);
CREATE INDEX idx_communications_status ON communications(status);
CREATE INDEX idx_communications_channel ON communications(channel);
CREATE INDEX idx_communications_communication_type ON communications(communication_type);
CREATE INDEX idx_communications_created_at ON communications(created_at DESC);
CREATE INDEX idx_communications_sent_at ON communications(sent_at DESC) WHERE sent_at IS NOT NULL;
CREATE INDEX idx_communications_opened_at ON communications(opened_at DESC) WHERE opened_at IS NOT NULL;
CREATE INDEX idx_communications_recipient_email ON communications(recipient_email);
CREATE INDEX idx_communications_status_delivered ON communications(status, delivered_at DESC);
CREATE INDEX idx_communications_campaign_name ON communications(campaign_name);
CREATE INDEX idx_communications_sequence_id ON communications(sequence_id);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_communications_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_communications_update_timestamp
BEFORE UPDATE ON communications
FOR EACH ROW
EXECUTE FUNCTION update_communications_updated_at();

-- Add comments for documentation
COMMENT ON TABLE communications IS 'Tracks all outbound and inbound communications: emails, SMS, push notifications, in-app messages with full engagement and delivery tracking.';
COMMENT ON COLUMN communications.message_id IS 'ID from external email service provider (SendGrid, Mailgun, etc.) for tracking and compliance.';
COMMENT ON COLUMN communications.tracked_links_json IS 'JSON array storing click tracking data: [{url: string, click_count: int, first_click_at: timestamp, last_click_at: timestamp}]';
COMMENT ON COLUMN communications.sequence_id IS 'Links to email automation sequence (e.g., welcome series, nurture campaign) for tracking campaign performance.';
COMMENT ON COLUMN communications.variant_id IS 'For A/B testing variants (A, B, C, etc.) to track which version performs better.';
