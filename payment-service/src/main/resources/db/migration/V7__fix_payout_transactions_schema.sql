-- Fix payout_transactions table to match PayoutTransaction entity

-- Add missing error_code column
ALTER TABLE payout_transactions ADD COLUMN IF NOT EXISTS error_code VARCHAR(64);

-- Rename amount to gross_amount to match entity
ALTER TABLE payout_transactions RENAME COLUMN amount TO gross_amount;

-- Rename payment_transaction_id to payment_id to match entity
ALTER TABLE payout_transactions RENAME COLUMN payment_transaction_id TO payment_id;

-- Update status column length to match entity (VARCHAR(32) instead of VARCHAR(20))
ALTER TABLE payout_transactions ALTER COLUMN status TYPE VARCHAR(32);

-- Update foreign key constraint name to match the new column name
ALTER TABLE payout_transactions DROP CONSTRAINT IF EXISTS fk_payout_payment_transaction;
ALTER TABLE payout_transactions ADD CONSTRAINT fk_payout_payment_transaction
FOREIGN KEY (payment_id) REFERENCES payment_transactions(id);

-- Update index to use new column name
DROP INDEX IF EXISTS idx_payout_transactions_payment_transaction_id;
CREATE INDEX IF NOT EXISTS idx_payout_transactions_payment_id ON payout_transactions(payment_id);
