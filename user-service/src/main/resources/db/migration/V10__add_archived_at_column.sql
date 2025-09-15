-- V10__add_archived_at_column.sql
-- Adds the missing archived_at column to the users table

-- Add archived_at column to users table
ALTER TABLE users 
ADD COLUMN archived_at TIMESTAMP WITH TIME ZONE;

-- Add index for archived users
CREATE INDEX idx_users_archived_at ON users(archived_at) WHERE archived_at IS NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN users.archived_at IS 'When user was archived (soft archive for compliance)';
