-- V45: CRM Seed Data and Configuration
-- Purpose: Initialize CRM system with demo templates, statuses, and configuration
-- Created: December 27, 2025

-- Create demo slot templates table
CREATE TABLE IF NOT EXISTS demo_slot_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    duration_minutes INTEGER DEFAULT 30 NOT NULL,
    day_of_week INTEGER CHECK (day_of_week >= 0 AND day_of_week <= 6),  -- 0=Sunday, 6=Saturday
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    timezone VARCHAR(50) DEFAULT 'America/New_York',
    max_demos_per_slot INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index on active slots
CREATE INDEX idx_demo_slot_templates_active ON demo_slot_templates(is_active, day_of_week, start_time);

-- Insert default demo slot templates
INSERT INTO demo_slot_templates (name, description, duration_minutes, day_of_week, start_time, end_time, timezone, max_demos_per_slot, is_active)
VALUES
    ('Monday Morning', 'Monday 9:00 AM - 12:00 PM EST', 30, 1, '09:00:00', '12:00:00', 'America/New_York', 3, TRUE),
    ('Monday Afternoon', 'Monday 1:00 PM - 4:00 PM EST', 30, 1, '13:00:00', '16:00:00', 'America/New_York', 3, TRUE),
    ('Tuesday Morning', 'Tuesday 9:00 AM - 12:00 PM EST', 30, 2, '09:00:00', '12:00:00', 'America/New_York', 3, TRUE),
    ('Tuesday Afternoon', 'Tuesday 1:00 PM - 4:00 PM EST', 30, 2, '13:00:00', '16:00:00', 'America/New_York', 3, TRUE),
    ('Wednesday Morning', 'Wednesday 9:00 AM - 12:00 PM EST', 30, 3, '09:00:00', '12:00:00', 'America/New_York', 3, TRUE),
    ('Wednesday Afternoon', 'Wednesday 1:00 PM - 4:00 PM EST', 30, 3, '13:00:00', '16:00:00', 'America/New_York', 3, TRUE),
    ('Thursday Morning', 'Thursday 9:00 AM - 12:00 PM EST', 30, 4, '09:00:00', '12:00:00', 'America/New_York', 3, TRUE),
    ('Thursday Afternoon', 'Thursday 1:00 PM - 4:00 PM EST', 30, 4, '13:00:00', '16:00:00', 'America/New_York', 3, TRUE),
    ('Friday Morning', 'Friday 9:00 AM - 12:00 PM EST', 30, 5, '09:00:00', '12:00:00', 'America/New_York', 2, TRUE),
    ('Friday Afternoon', 'Friday 1:00 PM - 3:00 PM EST', 30, 5, '13:00:00', '15:00:00', 'America/New_York', 2, TRUE);

-- Create inquiry form templates table
CREATE TABLE IF NOT EXISTS inquiry_form_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    fields_json JSONB NOT NULL,  -- JSON schema for form fields
    success_message TEXT,
    auto_response_template_id UUID,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert default inquiry form template
INSERT INTO inquiry_form_templates (name, description, fields_json, success_message, is_active)
VALUES
    ('Default Inquiry Form', 'Standard inquiry form for website',
     '[
        {"name": "first_name", "label": "First Name", "type": "text", "required": true},
        {"name": "last_name", "label": "Last Name", "type": "text", "required": false},
        {"name": "email", "label": "Email Address", "type": "email", "required": true},
        {"name": "phone_number", "label": "Phone Number", "type": "tel", "required": false},
        {"name": "company_name", "label": "Company Name", "type": "text", "required": false},
        {"name": "inquiry_type", "label": "Inquiry Type", "type": "select", "required": true, "options": ["Platform Demo", "Integration Help", "Partnership", "Security Question", "Other"]},
        {"name": "inquiry_message", "label": "Tell us more", "type": "textarea", "required": true},
        {"name": "budget_range", "label": "Budget Range", "type": "select", "required": false, "options": ["Under $10K", "$10K-50K", "$50K-100K", "$100K+"]},
        {"name": "timeline", "label": "Timeline", "type": "select", "required": false, "options": ["Immediate", "1-3 months", "3-6 months", "6+ months"]},
        {"name": "gdpr_consent", "label": "I agree to be contacted", "type": "checkbox", "required": true}
      ]',
     'Thank you! We have received your inquiry and will get back to you within 24 hours.',
     TRUE);

-- Create email template table
CREATE TABLE IF NOT EXISTS email_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(100) NOT NULL,  -- inquiry_response, demo_confirmation, reminder, follow_up, etc.
    subject_line VARCHAR(255) NOT NULL,
    preview_text VARCHAR(255),
    html_template TEXT NOT NULL,
    plain_text_template TEXT,
    variables_required JSONB,  -- JSON array of required variables
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert default email templates
INSERT INTO email_templates (name, category, subject_line, preview_text, html_template, variables_required, is_active)
VALUES
    (
        'Inquiry Confirmation',
        'inquiry_response',
        'Thank you for your interest in Aurigraph!',
        'We''ve received your inquiry and will be in touch soon.',
        '<html><body><p>Hi {{first_name}},</p><p>Thank you for your interest in Aurigraph! We have received your inquiry about {{inquiry_type}}.</p><p>Our team will review your request and reach out within 24 hours to discuss how we can help.</p><p>Best regards,<br>The Aurigraph Team</p></body></html>',
        '["first_name", "inquiry_type"]',
        TRUE
    ),
    (
        'Demo Confirmation',
        'demo_confirmation',
        'Your Aurigraph Demo is Confirmed - {{demo_date}}',
        'Join us for an exclusive demo on {{demo_date}} at {{demo_time}}',
        '<html><body><p>Hi {{first_name}},</p><p>Great! Your demo is confirmed for {{demo_date}} at {{demo_time}} {{timezone}}.</p><p>Meeting Details:<br>Platform: {{meeting_platform}}<br>Link: {{meeting_url}}</p><p>We look forward to showing you what Aurigraph can do!</p></body></html>',
        '["first_name", "demo_date", "demo_time", "timezone", "meeting_platform", "meeting_url"]',
        TRUE
    ),
    (
        'Demo Reminder 24h',
        'demo_reminder',
        'Reminder: Your Aurigraph Demo is Tomorrow!',
        'Your demo is tomorrow at {{demo_time}}. Join us online!',
        '<html><body><p>Hi {{first_name}},</p><p>This is a friendly reminder that your Aurigraph demo is tomorrow at {{demo_time}} {{timezone}}.</p><p>Meeting link: {{meeting_url}}</p><p>See you then!</p></body></html>',
        '["first_name", "demo_time", "timezone", "meeting_url"]',
        TRUE
    ),
    (
        'Demo Reminder 1h',
        'demo_reminder',
        'Your Demo Starts in 1 Hour!',
        'Join now: {{meeting_url}}',
        '<html><body><p>Hi {{first_name}},</p><p>Your Aurigraph demo starts in 1 hour!</p><p>Join the demo: {{meeting_url}}</p><p>See you soon!</p></body></html>',
        '["first_name", "meeting_url"]',
        TRUE
    ),
    (
        'Demo Follow-up',
        'follow_up',
        'Your Aurigraph Demo Recording & Next Steps',
        'Access your recording and learn about next steps',
        '<html><body><p>Hi {{first_name}},</p><p>Thank you for joining the Aurigraph demo! We hope you found it valuable.</p><p>Recording: {{recording_url}}</p><p>We''d love your feedback: {{feedback_form_url}}</p><p>Questions? Reach out anytime!</p></body></html>',
        '["first_name", "recording_url", "feedback_form_url"]',
        TRUE
    );

-- Create CRM settings/configuration table
CREATE TABLE IF NOT EXISTS crm_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value TEXT,
    setting_type VARCHAR(50),  -- string, integer, boolean, json
    description TEXT,
    is_mutable BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert default CRM settings
INSERT INTO crm_settings (setting_key, setting_value, setting_type, description, is_mutable)
VALUES
    ('lead_scoring_enabled', 'true', 'boolean', 'Enable automatic lead scoring', TRUE),
    ('lead_enrichment_enabled', 'true', 'boolean', 'Enable automatic lead enrichment from third-party APIs', TRUE),
    ('demo_reminder_enabled', 'true', 'boolean', 'Enable automatic demo reminders', TRUE),
    ('demo_reminder_24h_enabled', 'true', 'boolean', 'Send 24-hour before demo reminder', TRUE),
    ('demo_reminder_1h_enabled', 'true', 'boolean', 'Send 1-hour before demo reminder', TRUE),
    ('inquiry_auto_response_enabled', 'true', 'boolean', 'Send auto-response to inquiry submissions', TRUE),
    ('follow_up_email_enabled', 'true', 'boolean', 'Send follow-up email after demo', TRUE),
    ('inquiry_response_sla_hours', '24', 'integer', 'SLA for responding to inquiries (hours)', TRUE),
    ('default_demo_duration_minutes', '30', 'integer', 'Default demo duration in minutes', TRUE),
    ('demo_confirmation_required', 'true', 'boolean', 'Require demo confirmation before meeting link is sent', TRUE),
    ('default_timezone', 'America/New_York', 'string', 'Default timezone for scheduling', TRUE),
    ('max_demos_per_day', '10', 'integer', 'Maximum demos schedulable per day', TRUE),
    ('min_demo_notice_hours', '1', 'integer', 'Minimum hours notice before demo can be scheduled', TRUE),
    ('lead_encryption_enabled', 'true', 'boolean', 'Encrypt sensitive lead data (phone, email)', TRUE),
    ('gdpr_compliance_mode', 'true', 'boolean', 'Enforce GDPR compliance checks', TRUE),
    ('email_provider', 'sendgrid', 'string', 'Email service provider (sendgrid, mailgun, etc.)', TRUE),
    ('video_platform', 'zoom', 'string', 'Video conferencing platform (zoom, teams, google_meet)', TRUE),
    ('lead_scoring_weights', '{"demo_completed": 25, "email_opened": 10, "demo_requested": 20, "inquiry_submitted": 5}', 'json', 'Lead scoring weights for different activities', TRUE);

-- Create CRM audit log table for compliance
CREATE TABLE IF NOT EXISTS crm_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,  -- leads, demo_requests, communications, etc.
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,  -- create, update, delete, view
    changed_fields JSONB,  -- JSON of {field: {old_value, new_value}}
    performed_by_user_id UUID,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for audit log
CREATE INDEX idx_crm_audit_log_entity ON crm_audit_log(entity_type, entity_id);
CREATE INDEX idx_crm_audit_log_user ON crm_audit_log(performed_by_user_id);
CREATE INDEX idx_crm_audit_log_action ON crm_audit_log(action);
CREATE INDEX idx_crm_audit_log_created ON crm_audit_log(created_at DESC);

-- Add comments
COMMENT ON TABLE demo_slot_templates IS 'Predefined available demo time slots for scheduling';
COMMENT ON TABLE inquiry_form_templates IS 'Customizable inquiry form templates for different use cases';
COMMENT ON TABLE email_templates IS 'Email templates for automated communications (confirmations, reminders, follow-ups)';
COMMENT ON TABLE crm_settings IS 'Global CRM configuration settings (SLAs, integrations, feature flags)';
COMMENT ON TABLE crm_audit_log IS 'Audit log for GDPR compliance and security tracking of all CRM data access and modifications';
