-- V6__Add_GDPR_Fields.sql
-- Adds GDPR/DPDP compliance fields for data deletion and anonymization

-- Add GDPR fields to users table
ALTER TABLE users 
ADD COLUMN anonymized BOOLEAN DEFAULT FALSE,
ADD COLUMN anonymized_at TIMESTAMP,
ADD COLUMN deletion_reason VARCHAR(100);

-- Add index for anonymized users
CREATE INDEX idx_users_anonymized ON users(anonymized) WHERE anonymized = TRUE;

-- Add index for deletion reason
CREATE INDEX idx_users_deletion_reason ON users(deletion_reason) WHERE deletion_reason IS NOT NULL;

-- Add GDPR fields to user_audit table
ALTER TABLE user_audit 
ADD COLUMN anonymized BOOLEAN DEFAULT FALSE,
ADD COLUMN anonymized_at TIMESTAMP;

-- Add index for anonymized audit records
CREATE INDEX idx_user_audit_anonymized ON user_audit(anonymized) WHERE anonymized = TRUE;

-- Add index for GDPR-related events
CREATE INDEX idx_user_audit_gdpr_events ON user_audit(event_type) 
WHERE event_type LIKE 'GDPR_%' OR event_type LIKE 'DATA_%';

-- Create data deletion tracking table
CREATE TABLE data_deletion_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    deletion_request_id UUID NOT NULL UNIQUE,
    user_reference_id VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    reason TEXT NOT NULL,
    retain_audit_trail BOOLEAN NOT NULL DEFAULT TRUE,
    requested_by UUID,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    total_records_deleted INTEGER DEFAULT 0,
    deleted_records_by_category JSONB,
    error_message TEXT,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for data deletion requests
CREATE INDEX idx_data_deletion_requests_user_id ON data_deletion_requests(user_id);
CREATE INDEX idx_data_deletion_requests_status ON data_deletion_requests(status);
CREATE INDEX idx_data_deletion_requests_started_at ON data_deletion_requests(started_at);
CREATE UNIQUE INDEX idx_data_deletion_requests_deletion_id ON data_deletion_requests(deletion_request_id);

-- Create data export tracking table
CREATE TABLE data_export_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    export_id UUID NOT NULL UNIQUE,
    user_reference_id VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    requested_by UUID,
    export_type VARCHAR(50) NOT NULL DEFAULT 'FULL_EXPORT',
    total_records INTEGER DEFAULT 0,
    file_path VARCHAR(500),
    file_size_bytes BIGINT,
    expires_at TIMESTAMP,
    downloaded_at TIMESTAMP,
    download_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for data export requests
CREATE INDEX idx_data_export_requests_user_id ON data_export_requests(user_id);
CREATE INDEX idx_data_export_requests_status ON data_export_requests(status);
CREATE INDEX idx_data_export_requests_expires_at ON data_export_requests(expires_at);
CREATE UNIQUE INDEX idx_data_export_requests_export_id ON data_export_requests(export_id);

-- Create retention policy tracking table
CREATE TABLE retention_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    retention_days INTEGER NOT NULL,
    applies_to VARCHAR(50) NOT NULL, -- 'DELETED_USERS', 'AUDIT_RECORDS', etc.
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_executed_at TIMESTAMP,
    next_execution_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default retention policies
INSERT INTO retention_policies (policy_name, description, retention_days, applies_to) VALUES
('DELETED_USERS_RETENTION', 'Purge deleted users after 90 days', 90, 'DELETED_USERS'),
('AUDIT_RECORDS_RETENTION', 'Archive audit records after 7 years', 2555, 'AUDIT_RECORDS'),
('EXPORT_FILES_RETENTION', 'Delete export files after 30 days', 30, 'EXPORT_FILES');

-- Create GDPR compliance log table
CREATE TABLE gdpr_compliance_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    user_reference_id VARCHAR(20),
    compliance_action VARCHAR(50) NOT NULL, -- 'RIGHT_TO_BE_FORGOTTEN', 'DATA_EXPORT', etc.
    action_details JSONB,
    performed_by UUID,
    ip_address INET,
    user_agent TEXT,
    correlation_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for GDPR compliance log
CREATE INDEX idx_gdpr_compliance_log_user_id ON gdpr_compliance_log(user_id);
CREATE INDEX idx_gdpr_compliance_log_action ON gdpr_compliance_log(compliance_action);
CREATE INDEX idx_gdpr_compliance_log_created_at ON gdpr_compliance_log(created_at);
CREATE INDEX idx_gdpr_compliance_log_correlation_id ON gdpr_compliance_log(correlation_id);

-- Add comments for documentation
COMMENT ON TABLE data_deletion_requests IS 'Tracks GDPR Article 17 right to be forgotten requests';
COMMENT ON TABLE data_export_requests IS 'Tracks GDPR Article 20 right to data portability requests';
COMMENT ON TABLE retention_policies IS 'Defines data retention policies for GDPR compliance';
COMMENT ON TABLE gdpr_compliance_log IS 'Comprehensive log of all GDPR compliance actions';

COMMENT ON COLUMN users.anonymized IS 'Whether user data has been anonymized for GDPR compliance';
COMMENT ON COLUMN users.anonymized_at IS 'When user data was anonymized';
COMMENT ON COLUMN users.deletion_reason IS 'Reason for user deletion (GDPR, user request, etc.)';

COMMENT ON COLUMN user_audit.anonymized IS 'Whether audit record has been anonymized';
COMMENT ON COLUMN user_audit.anonymized_at IS 'When audit record was anonymized';

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Add triggers for updated_at columns
CREATE TRIGGER update_data_deletion_requests_updated_at 
    BEFORE UPDATE ON data_deletion_requests 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_data_export_requests_updated_at 
    BEFORE UPDATE ON data_export_requests 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_retention_policies_updated_at 
    BEFORE UPDATE ON retention_policies 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create view for GDPR compliance dashboard
CREATE VIEW gdpr_compliance_dashboard AS
SELECT 
    'deletion_requests' as metric_type,
    COUNT(*) as total_count,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_count,
    COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as in_progress_count,
    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_count,
    AVG(EXTRACT(EPOCH FROM (completed_at - started_at))/60) as avg_processing_time_minutes
FROM data_deletion_requests
WHERE started_at >= CURRENT_DATE - INTERVAL '30 days'

UNION ALL

SELECT 
    'export_requests' as metric_type,
    COUNT(*) as total_count,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_count,
    COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as in_progress_count,
    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_count,
    NULL as avg_processing_time_minutes
FROM data_export_requests
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'

UNION ALL

SELECT 
    'anonymized_users' as metric_type,
    COUNT(*) as total_count,
    NULL as completed_count,
    NULL as in_progress_count,
    NULL as failed_count,
    NULL as avg_processing_time_minutes
FROM users
WHERE anonymized = TRUE;

-- Grant permissions (adjust as needed for your security model)
-- GRANT SELECT, INSERT, UPDATE ON data_deletion_requests TO user_service_app;
-- GRANT SELECT, INSERT, UPDATE ON data_export_requests TO user_service_app;
-- GRANT SELECT ON retention_policies TO user_service_app;
-- GRANT SELECT, INSERT ON gdpr_compliance_log TO user_service_app;
-- GRANT SELECT ON gdpr_compliance_dashboard TO user_service_app;
