-- User Service Database Schema V4
-- Enhanced audit trail and compliance features

-- Create audit configuration table
CREATE TABLE audit_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(50) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT true,
    retention_days INTEGER NOT NULL DEFAULT 2555, -- 7 years default
    pii_logging BOOLEAN NOT NULL DEFAULT false,
    
    -- Notification settings
    notify_on_event BOOLEAN NOT NULL DEFAULT false,
    notification_channels TEXT[], -- 'EMAIL', 'SLACK', 'WEBHOOK'
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_retention_days_positive CHECK (retention_days > 0)
);

-- Insert default audit configurations
INSERT INTO audit_config (event_type, enabled, retention_days, pii_logging, notify_on_event, notification_channels) VALUES
    ('USER_CREATED', true, 2555, false, false, ARRAY['EMAIL']),
    ('USER_UPDATED', true, 2555, false, false, NULL),
    ('USER_DELETED', true, 2555, false, true, ARRAY['EMAIL', 'SLACK']),
    ('USER_ARCHIVED', true, 2555, false, true, ARRAY['EMAIL']),
    ('ROLE_ASSIGNED', true, 2555, false, true, ARRAY['EMAIL']),
    ('ROLE_REMOVED', true, 2555, false, true, ARRAY['EMAIL']),
    ('CONSENT_GRANTED', true, 3650, false, false, NULL), -- 10 years for consent
    ('CONSENT_WITHDRAWN', true, 3650, false, true, ARRAY['EMAIL']),
    ('ADDRESS_ADDED', true, 2555, false, false, NULL),
    ('ADDRESS_UPDATED', true, 2555, false, false, NULL),
    ('ADDRESS_DELETED', true, 2555, false, false, NULL),
    ('LOGIN_SUCCESS', true, 365, false, false, NULL), -- 1 year for login events
    ('LOGIN_FAILED', true, 365, false, false, NULL),
    ('PASSWORD_CHANGED', true, 2555, false, true, ARRAY['EMAIL']),
    ('DATA_EXPORT_REQUESTED', true, 2555, false, true, ARRAY['EMAIL']),
    ('DATA_DELETION_REQUESTED', true, 2555, false, true, ARRAY['EMAIL', 'SLACK']),
    ('PRIVACY_SETTINGS_CHANGED', true, 2555, false, false, NULL),
    ('SUSPICIOUS_ACTIVITY', true, 2555, false, true, ARRAY['EMAIL', 'SLACK', 'WEBHOOK']);

-- Create compliance reports table
CREATE TABLE compliance_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_type VARCHAR(50) NOT NULL,
    report_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    report_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Report data
    report_data JSONB NOT NULL,
    summary JSONB,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    generated_by UUID, -- Admin user who generated the report
    
    -- File storage
    file_path TEXT,
    file_size_bytes BIGINT,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_report_type CHECK (report_type IN ('GDPR_COMPLIANCE', 'DATA_RETENTION', 'CONSENT_AUDIT', 'USER_ACTIVITY', 'SECURITY_AUDIT')),
    CONSTRAINT chk_report_status CHECK (status IN ('GENERATED', 'REVIEWED', 'APPROVED', 'ARCHIVED')),
    CONSTRAINT chk_report_period CHECK (report_period_end > report_period_start)
);

-- Create data breach incidents table
CREATE TABLE data_breach_incidents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incident_id VARCHAR(50) NOT NULL UNIQUE, -- Business incident ID
    
    -- Incident details
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL, -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    
    -- Affected data
    affected_users_count INTEGER,
    data_types_affected TEXT[],
    pii_involved BOOLEAN NOT NULL DEFAULT false,
    
    -- Timeline
    discovered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE,
    contained_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    
    -- Regulatory reporting
    regulatory_notification_required BOOLEAN NOT NULL DEFAULT false,
    regulatory_notification_sent_at TIMESTAMP WITH TIME ZONE,
    user_notification_required BOOLEAN NOT NULL DEFAULT false,
    user_notification_sent_at TIMESTAMP WITH TIME ZONE,
    
    -- Response
    response_actions TEXT[],
    lessons_learned TEXT,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to UUID, -- Admin user handling the incident
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_breach_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_breach_status CHECK (status IN ('OPEN', 'INVESTIGATING', 'CONTAINED', 'RESOLVED', 'CLOSED')),
    CONSTRAINT chk_affected_users_count CHECK (affected_users_count IS NULL OR affected_users_count >= 0)
);

-- Create user data requests table (GDPR Article 15, 17, 20)
CREATE TABLE user_data_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id VARCHAR(50) NOT NULL UNIQUE, -- Business request ID
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Request details
    request_type VARCHAR(30) NOT NULL, -- 'ACCESS', 'RECTIFICATION', 'ERASURE', 'PORTABILITY', 'RESTRICTION'
    description TEXT,
    
    -- Status and processing
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL', -- 'LOW', 'NORMAL', 'HIGH', 'URGENT'
    
    -- Timeline (GDPR requires response within 30 days)
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days'),
    processed_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    -- Processing details
    processed_by UUID, -- Admin user who processed the request
    processing_notes TEXT,
    
    -- Response
    response_data JSONB,
    response_file_path TEXT,
    response_sent_at TIMESTAMP WITH TIME ZONE,
    
    -- Contact information
    contact_email TEXT,
    contact_method VARCHAR(20) DEFAULT 'EMAIL',
    
    -- Verification
    identity_verified BOOLEAN NOT NULL DEFAULT false,
    verification_method VARCHAR(50),
    verification_date TIMESTAMP WITH TIME ZONE,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_request_type CHECK (request_type IN ('ACCESS', 'RECTIFICATION', 'ERASURE', 'PORTABILITY', 'RESTRICTION')),
    CONSTRAINT chk_request_status CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_request_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT chk_contact_method CHECK (contact_method IN ('EMAIL', 'PHONE', 'POST', 'IN_PERSON'))
);

-- Create system health monitoring table
CREATE TABLE system_health_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_name VARCHAR(50) NOT NULL,
    metric_value DECIMAL(15,4) NOT NULL,
    metric_unit VARCHAR(20),
    
    -- Context
    component VARCHAR(50), -- 'DATABASE', 'CACHE', 'ENCRYPTION', 'API'
    environment VARCHAR(20), -- 'DEV', 'STAGING', 'PROD'
    
    -- Thresholds
    warning_threshold DECIMAL(15,4),
    critical_threshold DECIMAL(15,4),
    status VARCHAR(20) DEFAULT 'OK', -- 'OK', 'WARNING', 'CRITICAL'
    
    -- Timestamp
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_metric_status CHECK (status IN ('OK', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_environment CHECK (environment IN ('DEV', 'STAGING', 'PROD'))
);

-- Create indices for audit and compliance tables
CREATE INDEX idx_audit_config_event_type ON audit_config(event_type);
CREATE INDEX idx_audit_config_enabled ON audit_config(enabled) WHERE enabled = true;

CREATE INDEX idx_compliance_reports_type_period ON compliance_reports(report_type, report_period_start, report_period_end);
CREATE INDEX idx_compliance_reports_status ON compliance_reports(status);
CREATE INDEX idx_compliance_reports_created_at ON compliance_reports(created_at);

CREATE INDEX idx_data_breach_incidents_severity ON data_breach_incidents(severity);
CREATE INDEX idx_data_breach_incidents_status ON data_breach_incidents(status);
CREATE INDEX idx_data_breach_incidents_discovered_at ON data_breach_incidents(discovered_at);
CREATE INDEX idx_data_breach_incidents_regulatory_notification ON data_breach_incidents(regulatory_notification_required) WHERE regulatory_notification_required = true;

CREATE INDEX idx_user_data_requests_user_id ON user_data_requests(user_id);
CREATE INDEX idx_user_data_requests_type ON user_data_requests(request_type);
CREATE INDEX idx_user_data_requests_status ON user_data_requests(status);
CREATE INDEX idx_user_data_requests_due_date ON user_data_requests(due_date);
CREATE INDEX idx_user_data_requests_submitted_at ON user_data_requests(submitted_at);

CREATE INDEX idx_system_health_metrics_component ON system_health_metrics(component, recorded_at);
CREATE INDEX idx_system_health_metrics_status ON system_health_metrics(status) WHERE status != 'OK';
CREATE INDEX idx_system_health_metrics_recorded_at ON system_health_metrics(recorded_at);

-- Create triggers for updated_at columns
CREATE TRIGGER update_audit_config_updated_at BEFORE UPDATE ON audit_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_data_breach_incidents_updated_at BEFORE UPDATE ON data_breach_incidents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_data_requests_updated_at BEFORE UPDATE ON user_data_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to generate compliance report
CREATE OR REPLACE FUNCTION generate_compliance_report(
    report_type_param VARCHAR,
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE
)
RETURNS UUID AS $$
DECLARE
    report_id UUID;
    report_data JSONB;
    summary_data JSONB;
BEGIN
    report_id := uuid_generate_v4();
    
    CASE report_type_param
        WHEN 'GDPR_COMPLIANCE' THEN
            -- Generate GDPR compliance report
            SELECT jsonb_build_object(
                'total_users', COUNT(*),
                'active_users', COUNT(*) FILTER (WHERE active = true),
                'deleted_users', COUNT(*) FILTER (WHERE deleted_at IS NOT NULL),
                'consent_records', (SELECT COUNT(*) FROM consents WHERE granted_at BETWEEN start_date AND end_date),
                'data_requests', (SELECT COUNT(*) FROM user_data_requests WHERE submitted_at BETWEEN start_date AND end_date)
            ) INTO report_data
            FROM users
            WHERE created_at BETWEEN start_date AND end_date;
            
        WHEN 'DATA_RETENTION' THEN
            -- Generate data retention report
            SELECT jsonb_build_object(
                'audit_records_count', COUNT(*),
                'oldest_record', MIN(created_at),
                'records_due_for_deletion', COUNT(*) FILTER (WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '7 years')
            ) INTO report_data
            FROM user_audit
            WHERE created_at BETWEEN start_date AND end_date;
            
        WHEN 'CONSENT_AUDIT' THEN
            -- Generate consent audit report
            SELECT jsonb_build_object(
                'total_consents', COUNT(*),
                'granted_consents', COUNT(*) FILTER (WHERE granted = true),
                'withdrawn_consents', COUNT(*) FILTER (WHERE granted = false),
                'consent_changes', (SELECT COUNT(*) FROM consent_history WHERE granted_at BETWEEN start_date AND end_date)
            ) INTO report_data
            FROM consents;
            
        ELSE
            RAISE EXCEPTION 'Unknown report type: %', report_type_param;
    END CASE;
    
    -- Generate summary
    summary_data := jsonb_build_object(
        'report_type', report_type_param,
        'period_start', start_date,
        'period_end', end_date,
        'generated_at', CURRENT_TIMESTAMP
    );
    
    -- Insert report record
    INSERT INTO compliance_reports (
        id, report_type, report_period_start, report_period_end,
        report_data, summary, status
    ) VALUES (
        report_id, report_type_param, start_date, end_date,
        report_data, summary_data, 'GENERATED'
    );
    
    RETURN report_id;
END;
$$ LANGUAGE plpgsql;

-- Create function to check data retention compliance
CREATE OR REPLACE FUNCTION check_data_retention_compliance()
RETURNS TABLE(
    table_name TEXT,
    expired_records_count BIGINT,
    oldest_record_date TIMESTAMP WITH TIME ZONE,
    retention_policy_days INTEGER
) AS $$
BEGIN
    -- Check user_audit table
    RETURN QUERY
    SELECT 'user_audit'::TEXT,
           COUNT(*)::BIGINT,
           MIN(ua.created_at),
           drp.retention_period_days
    FROM user_audit ua
    CROSS JOIN data_retention_policies drp
    WHERE drp.data_type = 'AUDIT_LOGS'
      AND drp.active = true
      AND ua.created_at < CURRENT_TIMESTAMP - (drp.retention_period_days || ' days')::INTERVAL
    GROUP BY drp.retention_period_days;
    
    -- Check sessions table
    RETURN QUERY
    SELECT 'sessions'::TEXT,
           COUNT(*)::BIGINT,
           MIN(s.created_at),
           drp.retention_period_days
    FROM sessions s
    CROSS JOIN data_retention_policies drp
    WHERE drp.data_type = 'SESSION_DATA'
      AND drp.active = true
      AND s.created_at < CURRENT_TIMESTAMP - (drp.retention_period_days || ' days')::INTERVAL
    GROUP BY drp.retention_period_days;
    
    -- Add more tables as needed
END;
$$ LANGUAGE plpgsql;

-- Create function to handle data subject requests
CREATE OR REPLACE FUNCTION process_data_subject_request(request_uuid UUID)
RETURNS BOOLEAN AS $$
DECLARE
    request_record RECORD;
    user_data JSONB;
BEGIN
    -- Get request details
    SELECT * INTO request_record
    FROM user_data_requests
    WHERE id = request_uuid AND status = 'SUBMITTED';
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Request not found or not in SUBMITTED status';
    END IF;
    
    -- Update status to in progress
    UPDATE user_data_requests
    SET status = 'IN_PROGRESS',
        processed_at = CURRENT_TIMESTAMP
    WHERE id = request_uuid;
    
    -- Process based on request type
    CASE request_record.request_type
        WHEN 'ACCESS' THEN
            -- Generate data export
            user_data := export_user_data(request_record.user_id);
            
            UPDATE user_data_requests
            SET response_data = user_data,
                status = 'COMPLETED',
                completed_at = CURRENT_TIMESTAMP
            WHERE id = request_uuid;
            
        WHEN 'ERASURE' THEN
            -- Perform user data anonymization
            PERFORM anonymize_deleted_user(request_record.user_id);
            
            UPDATE user_data_requests
            SET status = 'COMPLETED',
                completed_at = CURRENT_TIMESTAMP,
                processing_notes = 'User data anonymized successfully'
            WHERE id = request_uuid;
            
        ELSE
            -- For other request types, mark as requiring manual processing
            UPDATE user_data_requests
            SET status = 'UNDER_REVIEW',
                processing_notes = 'Request requires manual processing'
            WHERE id = request_uuid;
    END CASE;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE audit_config IS 'Configuration for audit event logging and retention';
COMMENT ON TABLE compliance_reports IS 'Generated compliance reports for regulatory requirements';
COMMENT ON TABLE data_breach_incidents IS 'Data breach incident tracking and management';
COMMENT ON TABLE user_data_requests IS 'GDPR data subject requests (access, erasure, portability, etc.)';
COMMENT ON TABLE system_health_metrics IS 'System health and performance metrics for monitoring';
