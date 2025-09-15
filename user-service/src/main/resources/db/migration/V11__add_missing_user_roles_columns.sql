-- V11__add_missing_user_roles_columns.sql
-- Adds missing columns to the user_roles table

-- Add active column to user_roles table
ALTER TABLE user_roles 
ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;

-- Add expires_at column to user_roles table
ALTER TABLE user_roles 
ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;

-- Add index for active roles
CREATE INDEX idx_user_roles_active ON user_roles(user_id, active) WHERE active = true;

-- Add index for expiring roles
CREATE INDEX idx_user_roles_expires_at ON user_roles(expires_at) WHERE expires_at IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN user_roles.active IS 'Whether the role assignment is currently active';
COMMENT ON COLUMN user_roles.expires_at IS 'When the role assignment expires (null for permanent roles)';
