-- Fix CHAR columns to VARCHAR in saved_payment_methods table to match entity expectations

-- Fix card_last_four from CHAR(4) to VARCHAR(4)
ALTER TABLE saved_payment_methods ALTER COLUMN card_last_four TYPE VARCHAR(4);

-- Check and fix other potential CHAR columns that should be VARCHAR
-- Fix card_brand if it's CHAR
ALTER TABLE saved_payment_methods ALTER COLUMN card_brand TYPE VARCHAR(50);

-- Fix card_type if it's CHAR  
ALTER TABLE saved_payment_methods ALTER COLUMN card_type TYPE VARCHAR(20);

-- Fix card_issuer if it's CHAR
ALTER TABLE saved_payment_methods ALTER COLUMN card_issuer TYPE VARCHAR(100);

-- Fix wallet_provider if it's CHAR
ALTER TABLE saved_payment_methods ALTER COLUMN wallet_provider TYPE VARCHAR(50);

-- Fix wallet_phone if it's CHAR
ALTER TABLE saved_payment_methods ALTER COLUMN wallet_phone TYPE VARCHAR(15);
