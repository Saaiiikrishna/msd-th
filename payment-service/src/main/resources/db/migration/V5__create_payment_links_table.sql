-- Create payment_links table for Razorpay Payment Links
CREATE TABLE payment_links (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    razorpay_payment_link_id VARCHAR(64) NOT NULL UNIQUE,
    short_url VARCHAR(500) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for payment_links
CREATE INDEX idx_payment_links_invoice_id ON payment_links(invoice_id);
CREATE INDEX idx_payment_links_razorpay_id ON payment_links(razorpay_payment_link_id);
CREATE INDEX idx_payment_links_status ON payment_links(status);
CREATE INDEX idx_payment_links_created_at ON payment_links(created_at);
CREATE INDEX idx_payment_links_expires_at ON payment_links(expires_at);

-- Add foreign key constraint
ALTER TABLE payment_links ADD CONSTRAINT fk_payment_link_invoice
FOREIGN KEY (invoice_id) REFERENCES invoices(id);
