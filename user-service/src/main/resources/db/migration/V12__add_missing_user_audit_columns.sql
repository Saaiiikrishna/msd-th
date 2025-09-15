-- V12__add_missing_user_audit_columns.sql
-- Adds missing columns to the user_audit table

-- Add session_id column to user_audit table
ALTER TABLE user_audit 
ADD COLUMN session_id VARCHAR(100);

-- Add correlation_id column to user_audit table
ALTER TABLE user_audit 
ADD COLUMN correlation_id VARCHAR(100);

-- Add indexes for the new columns
CREATE INDEX idx_user_audit_session_id ON user_audit(session_id) WHERE session_id IS NOT NULL;
CREATE INDEX idx_user_audit_correlation_id ON user_audit(correlation_id) WHERE correlation_id IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN user_audit.session_id IS 'Session ID associated with the audit event';
COMMENT ON COLUMN user_audit.correlation_id IS 'Correlation ID for tracing requests across services';
