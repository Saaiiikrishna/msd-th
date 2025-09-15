-- Migration to update reference_id format from USR prefix to UUID format
-- This aligns with the architecture specification to use UUIDv7 for user_ref

-- Drop the old constraint that expected USR prefix format
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_reference_id_format;

-- Add new constraint for UUID format
ALTER TABLE users ADD CONSTRAINT chk_reference_id_uuid_format 
    CHECK (reference_id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$');

-- Update the column length to accommodate UUID format (36 characters)
ALTER TABLE users ALTER COLUMN reference_id TYPE VARCHAR(36);

-- Add comment for documentation
COMMENT ON COLUMN users.reference_id IS 'User reference ID in UUID format (UUIDv7 for time-ordered performance)';
