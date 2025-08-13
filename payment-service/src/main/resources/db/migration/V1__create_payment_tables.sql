-- Create payment service tables

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create payment_transactions table
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id UUID NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL,
    razorpay_order_id VARCHAR(64),
    razorpay_payment_id VARCHAR(64),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create invoices table for treasure hunt enrollments
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    enrollment_id UUID NOT NULL,
    registration_id VARCHAR(50) NOT NULL,
    user_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    plan_title VARCHAR(200) NOT NULL,
    enrollment_type VARCHAR(20) NOT NULL,
    team_name VARCHAR(255),
    team_size INTEGER,
    
    -- Pricing details
    base_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    convenience_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    platform_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    
    -- Payment details
    payment_transaction_id UUID,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    razorpay_order_id VARCHAR(64),
    razorpay_payment_id VARCHAR(64),
    
    -- Discount details
    promo_code VARCHAR(50),
    promotion_name VARCHAR(100),
    
    -- Billing information
    billing_name VARCHAR(200) NOT NULL,
    billing_email VARCHAR(200) NOT NULL,
    billing_phone VARCHAR(20),
    billing_address TEXT,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create saved_payment_methods table for secure payment method storage
CREATE TABLE saved_payment_methods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    
    -- Razorpay tokenization
    razorpay_token VARCHAR(100) UNIQUE,
    razorpay_customer_id VARCHAR(100),
    
    -- Card details (masked)
    card_last_four CHAR(4),
    card_brand VARCHAR(20),
    card_type VARCHAR(20),
    card_expiry_month INTEGER,
    card_expiry_year INTEGER,
    card_issuer VARCHAR(100),
    
    -- UPI details
    upi_vpa VARCHAR(100),
    
    -- Wallet details
    wallet_provider VARCHAR(50),
    wallet_phone VARCHAR(15),
    
    -- Metadata
    display_name VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMPTZ,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Create vendor_profiles table for vendor payout management
CREATE TABLE vendor_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vendor_id UUID NOT NULL UNIQUE,
    vendor_name VARCHAR(200) NOT NULL,
    vendor_email VARCHAR(200) NOT NULL,
    vendor_phone VARCHAR(20),
    
    -- Bank account details
    bank_account_number VARCHAR(50) NOT NULL,
    bank_ifsc_code VARCHAR(11) NOT NULL,
    bank_account_holder_name VARCHAR(200) NOT NULL,
    bank_name VARCHAR(200),
    
    -- Razorpay integration
    razorpay_contact_id VARCHAR(50),
    razorpay_fund_account_id VARCHAR(50) NOT NULL,
    
    -- Commission settings
    commission_rate DECIMAL(5,2) NOT NULL,
    minimum_payout_amount DECIMAL(10,2) DEFAULT 100.00,
    maximum_payout_amount DECIMAL(12,2),
    
    -- Status and metadata
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_date TIMESTAMPTZ,
    last_payout_date TIMESTAMPTZ,
    total_payouts_count BIGINT NOT NULL DEFAULT 0,
    total_payouts_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_commission_earned DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_payment_transactions_enrollment ON payment_transactions(enrollment_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX idx_payment_transactions_razorpay_order ON payment_transactions(razorpay_order_id);

CREATE INDEX idx_invoices_enrollment_id ON invoices(enrollment_id);
CREATE INDEX idx_invoices_registration_id ON invoices(registration_id);
CREATE INDEX idx_invoices_user_id ON invoices(user_id);
CREATE INDEX idx_invoices_razorpay_order_id ON invoices(razorpay_order_id);
CREATE INDEX idx_invoices_payment_status ON invoices(payment_status);
CREATE INDEX idx_invoices_created_at ON invoices(created_at);

CREATE INDEX idx_payment_method_user ON saved_payment_methods(user_id);
CREATE INDEX idx_payment_method_token ON saved_payment_methods(razorpay_token);
CREATE INDEX idx_payment_method_default ON saved_payment_methods(user_id, is_default) WHERE is_default = TRUE;

CREATE INDEX idx_vendor_profile_vendor_id ON vendor_profiles(vendor_id);
CREATE INDEX idx_vendor_profile_fund_account ON vendor_profiles(razorpay_fund_account_id);
CREATE INDEX idx_vendor_profile_active ON vendor_profiles(is_active);
CREATE INDEX idx_vendor_profile_commission ON vendor_profiles(commission_rate);

-- Add constraints
ALTER TABLE invoices ADD CONSTRAINT chk_invoice_amounts 
CHECK (base_amount >= 0 AND total_amount >= 0 AND discount_amount >= 0);

ALTER TABLE saved_payment_methods ADD CONSTRAINT chk_payment_method_card_expiry
CHECK (
    (payment_type != 'CARD') OR 
    (card_expiry_month BETWEEN 1 AND 12 AND card_expiry_year >= EXTRACT(YEAR FROM CURRENT_DATE))
);

ALTER TABLE vendor_profiles ADD CONSTRAINT chk_vendor_commission_rate
CHECK (commission_rate >= 0 AND commission_rate <= 100);

ALTER TABLE vendor_profiles ADD CONSTRAINT chk_vendor_payout_amounts
CHECK (minimum_payout_amount >= 0 AND 
       (maximum_payout_amount IS NULL OR maximum_payout_amount >= minimum_payout_amount));

-- Create payout_transactions table for vendor payouts
CREATE TABLE payout_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vendor_id UUID NOT NULL,
    payment_transaction_id UUID,
    amount DECIMAL(12,2) NOT NULL,
    commission_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    net_amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL,
    razorpay_payout_id VARCHAR(64),
    razorpay_fund_account_id VARCHAR(64),
    reference_id VARCHAR(100),
    purpose VARCHAR(100),
    mode VARCHAR(20),
    utr VARCHAR(50),
    failure_reason TEXT,
    error_message TEXT,
    notes TEXT,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for payout_transactions
CREATE INDEX idx_payout_transactions_vendor_id ON payout_transactions(vendor_id);
CREATE INDEX idx_payout_transactions_status ON payout_transactions(status);
CREATE INDEX idx_payout_transactions_razorpay_payout_id ON payout_transactions(razorpay_payout_id);
CREATE INDEX idx_payout_transactions_created_at ON payout_transactions(created_at);

-- Add foreign key constraints
ALTER TABLE invoices ADD CONSTRAINT fk_invoice_payment_transaction
FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions(id);

ALTER TABLE payout_transactions ADD CONSTRAINT fk_payout_payment_transaction
FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions(id);
