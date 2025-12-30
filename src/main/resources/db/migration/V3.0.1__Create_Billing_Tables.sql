-- Sprint 3: Create Billing and Subscription Tables

-- Create PLAN table
CREATE TABLE IF NOT EXISTS plan (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    tier_type VARCHAR(50) NOT NULL,
    monthly_price NUMERIC(19, 4),
    annual_price NUMERIC(19, 4),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    feature_limits JSONB,
    max_transactions_per_month INTEGER,
    max_transaction_amount NUMERIC(19, 4),
    api_rate_limit_per_second INTEGER,
    storage_limit_gb INTEGER,
    support_level VARCHAR(100),
    trial_days INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_plan_tier_type ON plan(tier_type);
CREATE INDEX idx_plan_is_active ON plan(is_active);
CREATE INDEX idx_plan_created_at ON plan(created_at);

-- Create SUBSCRIPTION table
CREATE TABLE IF NOT EXISTS subscription (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    plan_id BIGINT NOT NULL REFERENCES plan(id),
    status VARCHAR(50) NOT NULL,
    billing_cycle VARCHAR(50) NOT NULL,
    current_price NUMERIC(19, 4),
    subscription_start_date TIMESTAMP,
    subscription_end_date TIMESTAMP,
    renewal_date TIMESTAMP,
    trial_end_date TIMESTAMP,
    auto_renew BOOLEAN DEFAULT true,
    payment_method_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    stripe_customer_id VARCHAR(255),
    discount_percentage NUMERIC(5, 2),
    discount_code VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT
);

CREATE INDEX idx_subscription_user_id ON subscription(user_id);
CREATE INDEX idx_subscription_status ON subscription(status);
CREATE INDEX idx_subscription_created_at ON subscription(created_at);
CREATE INDEX idx_subscription_stripe_subscription_id ON subscription(stripe_subscription_id);
CREATE INDEX idx_subscription_stripe_customer_id ON subscription(stripe_customer_id);

-- Create BILLING table
CREATE TABLE IF NOT EXISTS billing (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(id),
    user_id VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    tax_amount NUMERIC(19, 4),
    total_amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    charge_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    billing_date TIMESTAMP NOT NULL,
    due_date TIMESTAMP,
    paid_date TIMESTAMP,
    payment_method VARCHAR(50),
    stripe_payment_intent_id VARCHAR(255),
    invoice_id BIGINT,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_billing_subscription_id ON billing(subscription_id);
CREATE INDEX idx_billing_user_id ON billing(user_id);
CREATE INDEX idx_billing_status ON billing(status);
CREATE INDEX idx_billing_billing_date ON billing(billing_date);
CREATE INDEX idx_billing_stripe_payment_intent_id ON billing(stripe_payment_intent_id);

-- Create INVOICE table
CREATE TABLE IF NOT EXISTS invoice (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL REFERENCES subscription(id),
    user_id VARCHAR(255) NOT NULL,
    subtotal NUMERIC(19, 4) NOT NULL,
    tax_rate NUMERIC(5, 4),
    tax_amount NUMERIC(19, 4),
    discount_amount NUMERIC(19, 4),
    total_amount NUMERIC(19, 4) NOT NULL,
    amount_paid NUMERIC(19, 4) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    due_date TIMESTAMP,
    paid_date TIMESTAMP,
    sent_date TIMESTAMP,
    line_items JSONB,
    notes TEXT,
    payment_terms VARCHAR(100),
    billing_address JSONB,
    pdf_url TEXT,
    stripe_invoice_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_invoice_subscription_id ON invoice(subscription_id);
CREATE INDEX idx_invoice_user_id ON invoice(user_id);
CREATE INDEX idx_invoice_invoice_number ON invoice(invoice_number);
CREATE INDEX idx_invoice_status ON invoice(status);
CREATE INDEX idx_invoice_stripe_invoice_id ON invoice(stripe_invoice_id);

-- Create PAYMENT table
CREATE TABLE IF NOT EXISTS payment (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(id),
    billing_id BIGINT NOT NULL REFERENCES billing(id),
    user_id VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_gateway VARCHAR(50) NOT NULL,
    gateway_transaction_id VARCHAR(255),
    stripe_payment_intent_id VARCHAR(255),
    stripe_charge_id VARCHAR(255),
    paypal_transaction_id VARCHAR(255),
    last_four_digits VARCHAR(4),
    card_brand VARCHAR(50),
    payout_status VARCHAR(50),
    error_code VARCHAR(100),
    error_message TEXT,
    refund_reason TEXT,
    refund_amount NUMERIC(19, 4),
    processing_fee NUMERIC(19, 4),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    processed_at TIMESTAMP,
    refunded_at TIMESTAMP
);

CREATE INDEX idx_payment_subscription_id ON payment(subscription_id);
CREATE INDEX idx_payment_billing_id ON payment(billing_id);
CREATE INDEX idx_payment_user_id ON payment(user_id);
CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_payment_created_at ON payment(created_at);
CREATE INDEX idx_payment_stripe_charge_id ON payment(stripe_charge_id);
CREATE INDEX idx_payment_stripe_payment_intent_id ON payment(stripe_payment_intent_id);

-- Create WEBHOOK_LOG table for tracking Stripe webhooks
CREATE TABLE IF NOT EXISTS webhook_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    payload JSONB,
    processed BOOLEAN DEFAULT false,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE INDEX idx_webhook_log_event_type ON webhook_log(event_type);
CREATE INDEX idx_webhook_log_processed ON webhook_log(processed);
CREATE INDEX idx_webhook_log_created_at ON webhook_log(created_at);

-- Add RLS (Row Level Security) policies for multi-tenancy
ALTER TABLE subscription ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment ENABLE ROW LEVEL SECURITY;

-- Create audit triggers
CREATE OR REPLACE FUNCTION audit_subscription_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'UPDATE') THEN
        INSERT INTO subscription_audit (subscription_id, old_status, new_status, changed_at)
        VALUES (NEW.id, OLD.status, NEW.status, NOW());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER subscription_audit_trigger
AFTER UPDATE ON subscription
FOR EACH ROW
EXECUTE FUNCTION audit_subscription_changes();

-- Create audit table for subscription changes
CREATE TABLE IF NOT EXISTS subscription_audit (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(id),
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    changed_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_subscription_audit_subscription_id ON subscription_audit(subscription_id);
