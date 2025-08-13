-- Add vendor_id to payment_transactions for payout tracking
ALTER TABLE payment_transactions 
ADD COLUMN vendor_id UUID;

-- Add index for vendor queries
CREATE INDEX IF NOT EXISTS idx_payment_transactions_vendor_id ON payment_transactions(vendor_id);

-- Add comment
COMMENT ON COLUMN payment_transactions.vendor_id IS 'Vendor ID for automatic payout processing (null = no vendor payout needed)';
