-- V41: Create CRM Interactions Table
-- Purpose: Log all customer interactions (calls, emails, demos, meetings, notes)
-- Created: December 27, 2025

-- Create ENUM types
CREATE TYPE interaction_type AS ENUM ('email', 'phone_call', 'demo', 'meeting', 'internal_note', 'linkedin_message', 'sms', 'in_person');
CREATE TYPE interaction_direction AS ENUM ('inbound', 'outbound');
CREATE TYPE interaction_status AS ENUM ('draft', 'pending', 'sent', 'delivered', 'opened', 'clicked', 'bounced', 'failed');

-- Create interactions table
CREATE TABLE IF NOT EXISTS interactions (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign Keys
    lead_id UUID NOT NULL,
    related_demo_id UUID,  -- Link to demo if interaction is a demo
    created_by_user_id UUID,
    assigned_to_user_id UUID,

    -- Interaction Details
    interaction_type interaction_type NOT NULL,
    direction interaction_direction NOT NULL,
    status interaction_status DEFAULT 'pending',
    subject VARCHAR(255),
    body TEXT,  -- Email body, call notes, message content, etc.
    summary VARCHAR(500),

    -- Email Specific Fields
    email_to VARCHAR(500),  -- Comma-separated emails
    email_cc VARCHAR(500),
    email_bcc VARCHAR(500),
    email_opened BOOLEAN DEFAULT FALSE,
    email_opened_at TIMESTAMP WITH TIME ZONE,
    email_opened_count INTEGER DEFAULT 0,
    email_clicked BOOLEAN DEFAULT FALSE,
    email_clicked_at TIMESTAMP WITH TIME ZONE,
    email_clicked_count INTEGER DEFAULT 0,

    -- Phone Call Fields
    phone_number VARCHAR(20),
    call_duration_seconds INTEGER,
    call_recording_url VARCHAR(500),
    call_transcript TEXT,

    -- Meeting Fields
    meeting_location VARCHAR(255),
    meeting_video_url VARCHAR(500),
    meeting_notes TEXT,
    meeting_attendees VARCHAR(500),  -- Comma-separated attendee names/emails

    -- Engagement Metrics
    engagement_score INTEGER DEFAULT 0,  -- Points from opens, clicks, replies, etc.

    -- Categorization & Follow-up
    category VARCHAR(100),  -- Sales, Support, Technical, etc.
    priority VARCHAR(50),  -- Low, Medium, High, Urgent
    tags VARCHAR(500),  -- Comma-separated tags for filtering
    requires_follow_up BOOLEAN DEFAULT FALSE,
    follow_up_due_date DATE,
    follow_up_completed BOOLEAN DEFAULT FALSE,

    -- Sentiment Analysis (Optional, for email/notes)
    sentiment_score FLOAT CHECK (sentiment_score >= -1 AND sentiment_score <= 1),  -- -1 (negative) to +1 (positive)
    sentiment_label VARCHAR(20),  -- negative, neutral, positive

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,

    -- Constraints
    CONSTRAINT interactions_lead_fk FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE,
    CONSTRAINT interactions_demo_fk FOREIGN KEY (related_demo_id) REFERENCES demo_requests(id) ON DELETE SET NULL,
    CONSTRAINT interactions_created_by_fk FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT interactions_assigned_to_fk FOREIGN KEY (assigned_to_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for common queries
CREATE INDEX idx_interactions_lead_id ON interactions(lead_id);
CREATE INDEX idx_interactions_created_at ON interactions(created_at DESC);
CREATE INDEX idx_interactions_interaction_type ON interactions(interaction_type);
CREATE INDEX idx_interactions_direction ON interactions(direction);
CREATE INDEX idx_interactions_created_by ON interactions(created_by_user_id);
CREATE INDEX idx_interactions_assigned_to ON interactions(assigned_to_user_id);
CREATE INDEX idx_interactions_follow_up ON interactions(requires_follow_up, follow_up_due_date);
CREATE INDEX idx_interactions_status ON interactions(status);
CREATE INDEX idx_interactions_lead_type_created ON interactions(lead_id, interaction_type, created_at DESC);
CREATE INDEX idx_interactions_sentiment ON interactions(sentiment_score DESC) WHERE sentiment_score IS NOT NULL;

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_interactions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_interactions_update_timestamp
BEFORE UPDATE ON interactions
FOR EACH ROW
EXECUTE FUNCTION update_interactions_updated_at();

-- Create function to calculate engagement score
CREATE OR REPLACE FUNCTION calculate_interaction_engagement_score(p_interaction_id UUID)
RETURNS INTEGER AS $$
DECLARE
    v_score INTEGER := 0;
    v_interaction interactions%ROWTYPE;
BEGIN
    SELECT * INTO v_interaction FROM interactions WHERE id = p_interaction_id;

    -- Email engagement scoring
    IF v_interaction.email_opened THEN
        v_score := v_score + 10;
    END IF;
    v_score := v_score + (COALESCE(v_interaction.email_opened_count, 0) * 2);

    IF v_interaction.email_clicked THEN
        v_score := v_score + 20;
    END IF;
    v_score := v_score + (COALESCE(v_interaction.email_clicked_count, 0) * 5);

    -- Phone call scoring
    IF v_interaction.call_duration_seconds IS NOT NULL AND v_interaction.call_duration_seconds > 0 THEN
        v_score := v_score + LEAST(v_interaction.call_duration_seconds / 60, 30);  -- Max 30 points for call length
    END IF;

    -- Demo scoring
    IF v_interaction.interaction_type = 'demo' THEN
        v_score := v_score + 50;
    END IF;

    -- Meeting scoring
    IF v_interaction.interaction_type = 'meeting' THEN
        v_score := v_score + 30;
    END IF;

    RETURN v_score;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE interactions IS 'Chronological log of all customer interactions: emails, calls, demos, meetings, and internal notes.';
COMMENT ON COLUMN interactions.engagement_score IS 'Calculated score based on email opens, clicks, call duration, and interaction type.';
COMMENT ON COLUMN interactions.sentiment_score IS 'NLP-calculated sentiment (-1 to +1) from email/message content for tone analysis.';
COMMENT ON COLUMN interactions.email_opened_count IS 'Number of times email was opened (tracked via pixel or webhook).';
