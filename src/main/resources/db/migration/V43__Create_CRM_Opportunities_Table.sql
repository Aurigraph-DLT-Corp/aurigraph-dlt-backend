-- V43: Create CRM Opportunities Table
-- Purpose: Track sales opportunities, deals, and pipeline revenue
-- Created: December 27, 2025

-- Create ENUM types
CREATE TYPE opportunity_stage AS ENUM ('discovery', 'assessment', 'solution_design', 'proposal', 'negotiation', 'closed_won', 'closed_lost');
CREATE TYPE close_reason AS ENUM ('won_deal', 'lost_competitor', 'lost_budget', 'lost_timing', 'lost_no_interest', 'lost_contact_change', 'lost_other');

-- Create opportunities table
CREATE TABLE IF NOT EXISTS opportunities (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign Keys
    lead_id UUID NOT NULL,
    account_id UUID,  -- Can be different from lead (e.g., buying committee)
    created_by_user_id UUID,
    owned_by_user_id UUID NOT NULL,

    -- Opportunity Details
    name VARCHAR(255) NOT NULL,
    description TEXT,
    stage opportunity_stage DEFAULT 'discovery' NOT NULL,
    stage_changed_at TIMESTAMP WITH TIME ZONE,
    probability_percent INTEGER DEFAULT 0 CHECK (probability_percent >= 0 AND probability_percent <= 100),

    -- Revenue
    estimated_value DECIMAL(15, 2),  -- Expected deal value
    actual_value DECIMAL(15, 2),  -- Actual revenue if closed won
    currency VARCHAR(3) DEFAULT 'USD',
    deal_size_category VARCHAR(50),  -- SMB, Mid-Market, Enterprise, etc.

    -- Timing
    expected_close_date DATE,
    actual_close_date DATE,
    quarter_forecast VARCHAR(10),  -- Q1, Q2, Q3, Q4, plus year

    -- Deal Details
    product_interest VARCHAR(255),  -- Platform, Integration, Consulting, etc.
    use_case TEXT,
    business_justification TEXT,
    decision_timeline VARCHAR(50),  -- Immediate, 30-60 days, 60-90 days, 90+ days
    buying_committee_size INTEGER,

    -- Competitive Information
    competing_vendors VARCHAR(500),  -- Comma-separated list of competitors
    competitive_advantage TEXT,

    -- Forecast Considerations
    forecast_category VARCHAR(50),  -- Pipeline, Commit, Closed
    at_risk BOOLEAN DEFAULT FALSE,
    at_risk_reason TEXT,
    risk_probability_percent INTEGER CHECK (risk_probability_percent IS NULL OR (risk_probability_percent >= 0 AND risk_probability_percent <= 100)),

    -- Win/Loss Details
    closed_reason close_reason,
    close_notes TEXT,

    -- Key Activities
    last_activity_at TIMESTAMP WITH TIME ZONE,
    activity_count INTEGER DEFAULT 0,
    demo_count INTEGER DEFAULT 0,
    proposal_sent_date DATE,
    contract_sent_date DATE,

    -- Expansion Opportunities (for existing customers)
    is_expansion BOOLEAN DEFAULT FALSE,
    parent_opportunity_id UUID,  -- Reference to original opportunity
    expansion_type VARCHAR(100),  -- Upsell, Cross-sell, Renewal, etc.

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,

    -- Constraints
    CONSTRAINT opportunities_lead_fk FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE,
    CONSTRAINT opportunities_owned_by_fk FOREIGN KEY (owned_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT opportunities_created_by_fk FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT opportunities_parent_fk FOREIGN KEY (parent_opportunity_id) REFERENCES opportunities(id) ON DELETE SET NULL
);

-- Create indexes for common queries
CREATE INDEX idx_opportunities_lead_id ON opportunities(lead_id);
CREATE INDEX idx_opportunities_stage ON opportunities(stage);
CREATE INDEX idx_opportunities_owned_by ON opportunities(owned_by_user_id);
CREATE INDEX idx_opportunities_expected_close_date ON opportunities(expected_close_date);
CREATE INDEX idx_opportunities_created_at ON opportunities(created_at DESC);
CREATE INDEX idx_opportunities_stage_owned ON opportunities(stage, owned_by_user_id);
CREATE INDEX idx_opportunities_probability ON opportunities(probability_percent DESC);
CREATE INDEX idx_opportunities_estimated_value ON opportunities(estimated_value DESC);
CREATE INDEX idx_opportunities_at_risk ON opportunities(at_risk, owned_by_user_id) WHERE at_risk = TRUE;
CREATE INDEX idx_opportunities_forecast_quarter ON opportunities(quarter_forecast) WHERE forecast_category = 'Commit';
CREATE INDEX idx_opportunities_parent_id ON opportunities(parent_opportunity_id) WHERE is_expansion = TRUE;

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_opportunities_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    IF NEW.stage IS DISTINCT FROM OLD.stage THEN
        NEW.stage_changed_at = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_opportunities_update_timestamp
BEFORE UPDATE ON opportunities
FOR EACH ROW
EXECUTE FUNCTION update_opportunities_updated_at();

-- Create function to calculate weighted pipeline value
CREATE OR REPLACE FUNCTION get_weighted_pipeline_value(p_opportunity_id UUID)
RETURNS DECIMAL AS $$
DECLARE
    v_estimated_value DECIMAL(15, 2);
    v_probability_percent INTEGER;
BEGIN
    SELECT estimated_value, probability_percent
    INTO v_estimated_value, v_probability_percent
    FROM opportunities
    WHERE id = p_opportunity_id;

    RETURN COALESCE(v_estimated_value, 0) * (COALESCE(v_probability_percent, 0) / 100.0);
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE opportunities IS 'Sales opportunities and deals in the pipeline, tracked from discovery through close with revenue forecasting.';
COMMENT ON COLUMN opportunities.stage IS 'Pipeline stage: discovery → assessment → solution_design → proposal → negotiation → closed_won/closed_lost';
COMMENT ON COLUMN opportunities.probability_percent IS 'Sales probability (0-100%) used to calculate weighted pipeline value.';
COMMENT ON COLUMN opportunities.at_risk IS 'Flag indicating deals at risk of lost/delayed, with risk_probability_percent tracking likelihood.';
