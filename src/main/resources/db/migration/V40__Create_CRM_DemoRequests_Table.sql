-- V40: Create CRM Demo Requests Table
-- Purpose: Track demo scheduling, execution, and follow-up
-- Created: December 27, 2025

-- Create ENUM types for demo status and type
CREATE TYPE demo_status AS ENUM ('requested', 'scheduled', 'confirmed', 'in_progress', 'completed', 'cancelled', 'no_show');
CREATE TYPE demo_type AS ENUM ('quick_intro', 'standard_demo', 'enterprise_demo', 'technical_deep_dive', 'custom');
CREATE TYPE demo_outcome AS ENUM ('very_interested', 'interested', 'neutral', 'not_interested', 'no_show');

-- Create demo_requests table
CREATE TABLE IF NOT EXISTS demo_requests (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign Keys
    lead_id UUID NOT NULL,
    scheduled_by_user_id UUID,
    conducted_by_user_id UUID,

    -- Demo Details
    demo_type demo_type DEFAULT 'standard_demo' NOT NULL,
    title VARCHAR(255),
    description TEXT,
    status demo_status DEFAULT 'requested' NOT NULL,
    notes TEXT,

    -- Scheduling
    requested_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    requested_datetime_preference VARCHAR(255),  -- Natural language preference: "Next Monday afternoon", etc.
    scheduled_at TIMESTAMP WITH TIME ZONE,
    rescheduled_at TIMESTAMP WITH TIME ZONE,
    reschedule_count INTEGER DEFAULT 0,

    -- Time Zone
    requester_timezone VARCHAR(50) DEFAULT 'America/New_York',
    demo_timezone VARCHAR(50) DEFAULT 'America/New_York',

    -- Demo Duration Configuration
    duration_minutes INTEGER DEFAULT 30 CHECK (duration_minutes > 0),
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,

    -- Meeting Platform Integration
    meeting_platform VARCHAR(50),  -- zoom, teams, google_meet, custom
    meeting_url VARCHAR(500),
    meeting_id VARCHAR(255),
    meeting_passcode VARCHAR(100),
    meeting_join_url VARCHAR(500),

    -- Attendees
    attendee_emails VARCHAR(500),  -- JSON array of attendee emails
    calendar_invite_sent BOOLEAN DEFAULT FALSE,
    calendar_invite_sent_at TIMESTAMP WITH TIME ZONE,

    -- Reminders
    reminder_24h_sent BOOLEAN DEFAULT FALSE,
    reminder_24h_sent_at TIMESTAMP WITH TIME ZONE,
    reminder_1h_sent BOOLEAN DEFAULT FALSE,
    reminder_1h_sent_at TIMESTAMP WITH TIME ZONE,

    -- Demo Execution
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    recording_url VARCHAR(500),
    recording_available_at TIMESTAMP WITH TIME ZONE,
    demo_outcome demo_outcome,

    -- Follow-up
    follow_up_email_sent BOOLEAN DEFAULT FALSE,
    follow_up_email_sent_at TIMESTAMP WITH TIME ZONE,
    feedback_form_sent BOOLEAN DEFAULT FALSE,
    feedback_form_sent_at TIMESTAMP WITH TIME ZONE,
    feedback_form_completed BOOLEAN DEFAULT FALSE,
    feedback_form_completed_at TIMESTAMP WITH TIME ZONE,

    -- Feedback Details
    customer_satisfaction_rating INTEGER CHECK (customer_satisfaction_rating >= 1 AND customer_satisfaction_rating <= 5),
    customer_feedback_text TEXT,
    interest_level INTEGER CHECK (interest_level >= 1 AND interest_level >= 5),
    next_steps VARCHAR(255),

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,

    -- Constraints
    CONSTRAINT demo_requests_lead_fk FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE,
    CONSTRAINT demo_requests_scheduled_by_fk FOREIGN KEY (scheduled_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT demo_requests_conducted_by_fk FOREIGN KEY (conducted_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT demo_requests_dates_logic CHECK (end_time IS NULL OR start_time <= end_time)
);

-- Create indexes for common queries
CREATE INDEX idx_demo_requests_lead_id ON demo_requests(lead_id);
CREATE INDEX idx_demo_requests_status ON demo_requests(status);
CREATE INDEX idx_demo_requests_scheduled_at ON demo_requests(scheduled_at);
CREATE INDEX idx_demo_requests_created_at ON demo_requests(created_at DESC);
CREATE INDEX idx_demo_requests_conducted_by ON demo_requests(conducted_by_user_id);
CREATE INDEX idx_demo_requests_meeting_platform ON demo_requests(meeting_platform);
CREATE INDEX idx_demo_requests_demo_outcome ON demo_requests(demo_outcome);
CREATE INDEX idx_demo_requests_status_scheduled ON demo_requests(status, scheduled_at);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_demo_requests_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_demo_requests_update_timestamp
BEFORE UPDATE ON demo_requests
FOR EACH ROW
EXECUTE FUNCTION update_demo_requests_updated_at();

-- Add comments for documentation
COMMENT ON TABLE demo_requests IS 'Tracks all demo requests from leads, including scheduling, execution, recording, and follow-up activities.';
COMMENT ON COLUMN demo_requests.demo_outcome IS 'Customer interest level: very_interested, interested, neutral, not_interested, no_show';
COMMENT ON COLUMN demo_requests.status IS 'Demo lifecycle: requested → scheduled → confirmed → in_progress → completed/cancelled/no_show';
COMMENT ON COLUMN demo_requests.recording_url IS 'URL to recorded demo (stored in video service, accessible to customer for 30 days)';
