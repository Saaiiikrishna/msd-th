-- Add missing columns to payment_transactions table to match PaymentTransaction entity
-- Note: vendor_id already exists from V3 migration

-- Add invoice_id column (if not exists)
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS invoice_id UUID;

-- Add version column for optimistic locking (if not exists)
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Update status column length to match entity (VARCHAR(32) instead of VARCHAR(20))
ALTER TABLE payment_transactions ALTER COLUMN status TYPE VARCHAR(32);

-- Create indexes for the new columns (if not exists)
CREATE INDEX IF NOT EXISTS idx_payment_transactions_invoice_id ON payment_transactions(invoice_id);

-- Add foreign key constraint for invoice_id (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_payment_transaction_invoice'
    ) THEN
        ALTER TABLE payment_transactions ADD CONSTRAINT fk_payment_transaction_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id);
    END IF;
END $$;
