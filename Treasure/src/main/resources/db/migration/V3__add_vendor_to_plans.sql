-- Add vendor assignment to plans
ALTER TABLE plan 
ADD COLUMN vendor_id UUID,
ADD COLUMN vendor_commission_rate DECIMAL(5,2);

-- Add index for vendor queries
CREATE INDEX IF NOT EXISTS idx_plan_vendor_id ON plan(vendor_id);

-- Add comments
COMMENT ON COLUMN plan.vendor_id IS 'Vendor assigned to this plan for payouts (null = direct platform plan)';
COMMENT ON COLUMN plan.vendor_commission_rate IS 'Vendor-specific commission rate (overrides default if set)';
