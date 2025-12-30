-- V39: Create CRM Leads Table
-- Purpose: Store customer lead information with enrichment data
-- Created: December 27, 2025

-- Create ENUM types for lead status and source
CREATE TYPE lead_status AS ENUM ('new', 'engaged', 'qualified', 'proposal_sent', 'converted', 'lost', 'archived');
CREATE TYPE lead_source AS ENUM ('website_inquiry', 'demo_request', 'partner_referral', 'direct_sales', 'webinar', 'event', 'other');
CREATE TYPE contact_preference AS ENUM ('email', 'phone', 'sms', 'linkedin', 'none');

-- Create leads table
CREATE TABLE IF NOT EXISTS leads (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Personal Information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    phone_number_encrypted BYTEA,  -- Encrypted phone number for GDPR compliance

    -- Company Information
    company_name VARCHAR(255),
    company_domain VARCHAR(255),
    company_size_range VARCHAR(50),  -- 1-50, 51-200, 201-500, 500+
    industry VARCHAR(100),
    country VARCHAR(100),
    state_province VARCHAR(100),
    city VARCHAR(100),

    -- Enrichment Data (from third-party APIs)
    company_logo_url VARCHAR(500),
    company_linkedin_url VARCHAR(500),
    company_website VARCHAR(500),
    annual_revenue VARCHAR(100),
    company_description TEXT,
    sic_code VARCHAR(20),

    -- Lead Management
    status lead_status DEFAULT 'new' NOT NULL,
    lead_score INTEGER DEFAULT 0 CHECK (lead_score >= 0),
    assigned_to_user_id UUID,
    assigned_at TIMESTAMP WITH TIME ZONE,

    -- Inquiry Details
    inquiry_type VARCHAR(100),  -- Platform inquiry, Demo request, Partnership, etc.
    inquiry_message TEXT,
    budget_range VARCHAR(50),  -- Under 10K, 10K-50K, 50K-100K, 100K+
    timeline VARCHAR(50),  -- Immediate, 1-3 months, 3-6 months, 6+ months

    -- Contact Preferences
    preferred_contact_method contact_preference DEFAULT 'email',
    do_not_contact BOOLEAN DEFAULT FALSE,

    -- GDPR & Compliance
    gdpr_consent_given BOOLEAN DEFAULT FALSE,
    gdpr_consent_timestamp TIMESTAMP WITH TIME ZONE,
    gdpr_consent_version VARCHAR(20),
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_timestamp TIMESTAMP WITH TIME ZONE,

    -- Tracking
    source lead_source,
    utm_source VARCHAR(100),
    utm_medium VARCHAR(100),
    utm_campaign VARCHAR(100),
    utm_content VARCHAR(100),
    session_id VARCHAR(100),
    referrer_url VARCHAR(500),

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,

    -- Indexes for Performance
    CONSTRAINT leads_email_unique UNIQUE (email),
    CONSTRAINT leads_assigned_to_fk FOREIGN KEY (assigned_to_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for common queries
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_created_at ON leads(created_at DESC);
CREATE INDEX idx_leads_updated_at ON leads(updated_at DESC);
CREATE INDEX idx_leads_assigned_to_user_id ON leads(assigned_to_user_id);
CREATE INDEX idx_leads_company_name ON leads(company_name);
CREATE INDEX idx_leads_lead_score ON leads(lead_score DESC);
CREATE INDEX idx_leads_inquiry_type ON leads(inquiry_type);
CREATE INDEX idx_leads_status_assigned ON leads(status, assigned_to_user_id);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_leads_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_leads_update_timestamp
BEFORE UPDATE ON leads
FOR EACH ROW
EXECUTE FUNCTION update_leads_updated_at();

-- Add comment for documentation
COMMENT ON TABLE leads IS 'Customer leads captured from inquiries, demos, and other sources. Includes enrichment data and lead scoring.';
COMMENT ON COLUMN leads.lead_score IS 'Lead scoring: 0-100+ (unbounded). Calculated based on engagement activities.';
COMMENT ON COLUMN leads.status IS 'Lead lifecycle status: new → engaged → qualified → proposal_sent → converted/lost → archived';
