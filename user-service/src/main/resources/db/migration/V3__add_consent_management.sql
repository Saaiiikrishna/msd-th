-- User Service Database Schema V3
-- Enhanced consent management for GDPR/DPDP compliance

-- Create consent categories table
CREATE TABLE consent_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_key VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    required BOOLEAN NOT NULL DEFAULT false,
    default_granted BOOLEAN NOT NULL DEFAULT false,
    
    -- Versioning
    version VARCHAR(10) NOT NULL DEFAULT '1.0',
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_until TIMESTAMP WITH TIME ZONE,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_consent_category_key CHECK (category_key ~ '^[a-z_]+$'),
    CONSTRAINT chk_effective_dates CHECK (effective_until IS NULL OR effective_until > effective_from)
);

-- Insert default consent categories
INSERT INTO consent_categories (category_key, name, description, required, default_granted) VALUES
    ('marketing_emails', 'Marketing Emails', 'Receive promotional emails and newsletters', false, false),
    ('marketing_sms', 'Marketing SMS', 'Receive promotional SMS messages', false, false),
    ('analytics', 'Analytics & Performance', 'Allow collection of usage analytics to improve service', false, true),
    ('personalization', 'Personalization', 'Use data to personalize user experience', false, true),
    ('third_party_sharing', 'Third Party Sharing', 'Share data with trusted partners for enhanced services', false, false),
    ('data_processing', 'Data Processing', 'Process personal data for service delivery', true, true),
    ('cookies_functional', 'Functional Cookies', 'Essential cookies for website functionality', true, true),
    ('cookies_analytics', 'Analytics Cookies', 'Cookies for website analytics and improvement', false, true),
    ('cookies_marketing', 'Marketing Cookies', 'Cookies for targeted advertising', false, false);

-- Create consent history table for audit trail
CREATE TABLE consent_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_key VARCHAR(100) NOT NULL,
    
    -- Consent details
    granted BOOLEAN NOT NULL,
    previous_state BOOLEAN,
    
    -- Context
    consent_version VARCHAR(10),
    ip_address INET,
    user_agent TEXT,
    source VARCHAR(50), -- 'WEB', 'MOBILE', 'API', 'ADMIN'
    
    -- Timestamps
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Legal basis (GDPR Article 6)
    legal_basis VARCHAR(50), -- 'CONSENT', 'CONTRACT', 'LEGAL_OBLIGATION', 'VITAL_INTERESTS', 'PUBLIC_TASK', 'LEGITIMATE_INTERESTS'
    
    CONSTRAINT chk_consent_source CHECK (source IN ('WEB', 'MOBILE', 'API', 'ADMIN', 'SYSTEM')),
    CONSTRAINT chk_legal_basis CHECK (legal_basis IN ('CONSENT', 'CONTRACT', 'LEGAL_OBLIGATION', 'VITAL_INTERESTS', 'PUBLIC_TASK', 'LEGITIMATE_INTERESTS'))
);

-- Update existing consents table to reference categories
ALTER TABLE consents ADD COLUMN consent_category_id UUID REFERENCES consent_categories(id);
ALTER TABLE consents ADD COLUMN legal_basis VARCHAR(50) DEFAULT 'CONSENT';
ALTER TABLE consents ADD COLUMN source VARCHAR(50) DEFAULT 'WEB';

-- Add constraints to updated consents table
ALTER TABLE consents ADD CONSTRAINT chk_consents_legal_basis 
    CHECK (legal_basis IN ('CONSENT', 'CONTRACT', 'LEGAL_OBLIGATION', 'VITAL_INTERESTS', 'PUBLIC_TASK', 'LEGITIMATE_INTERESTS'));

ALTER TABLE consents ADD CONSTRAINT chk_consents_source 
    CHECK (source IN ('WEB', 'MOBILE', 'API', 'ADMIN', 'SYSTEM'));

-- Create data retention policies table
CREATE TABLE data_retention_policies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    data_type VARCHAR(50) NOT NULL,
    retention_period_days INTEGER NOT NULL,
    description TEXT,
    legal_basis TEXT,
    
    -- Status
    active BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_retention_period CHECK (retention_period_days > 0),
    CONSTRAINT chk_data_type CHECK (data_type ~ '^[A-Z_]+$')
);

-- Insert default retention policies
INSERT INTO data_retention_policies (data_type, retention_period_days, description, legal_basis) VALUES
    ('USER_PROFILE', 2555, 'User profile data (7 years)', 'Legal requirement for financial records'),
    ('AUDIT_LOGS', 2555, 'Audit and security logs (7 years)', 'Legal requirement and security compliance'),
    ('MARKETING_DATA', 1095, 'Marketing preferences and history (3 years)', 'Legitimate business interest'),
    ('ANALYTICS_DATA', 730, 'Usage analytics and performance data (2 years)', 'Legitimate business interest'),
    ('SESSION_DATA', 90, 'Session and authentication data (3 months)', 'Security and fraud prevention'),
    ('CONSENT_RECORDS', 3650, 'Consent records (10 years)', 'Legal requirement for consent proof'),
    ('DELETED_USER_AUDIT', 2555, 'Audit trail for deleted users (7 years)', 'Legal requirement and compliance');

-- Create privacy settings table
CREATE TABLE privacy_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    
    -- Data processing preferences
    data_minimization BOOLEAN NOT NULL DEFAULT true,
    pseudonymization BOOLEAN NOT NULL DEFAULT true,
    
    -- Communication preferences
    email_frequency VARCHAR(20) DEFAULT 'WEEKLY', -- 'DAILY', 'WEEKLY', 'MONTHLY', 'NEVER'
    sms_frequency VARCHAR(20) DEFAULT 'NEVER',
    
    -- Data sharing preferences
    allow_analytics BOOLEAN NOT NULL DEFAULT true,
    allow_personalization BOOLEAN NOT NULL DEFAULT true,
    allow_third_party_sharing BOOLEAN NOT NULL DEFAULT false,
    
    -- Export and deletion preferences
    auto_export_frequency VARCHAR(20) DEFAULT 'NEVER', -- 'MONTHLY', 'QUARTERLY', 'YEARLY', 'NEVER'
    deletion_request_pending BOOLEAN NOT NULL DEFAULT false,
    deletion_scheduled_at TIMESTAMP WITH TIME ZONE,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_email_frequency CHECK (email_frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'NEVER')),
    CONSTRAINT chk_sms_frequency CHECK (sms_frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'NEVER')),
    CONSTRAINT chk_auto_export_frequency CHECK (auto_export_frequency IN ('MONTHLY', 'QUARTERLY', 'YEARLY', 'NEVER'))
);

-- Create data processing activities table (GDPR Article 30)
CREATE TABLE data_processing_activities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    activity_name VARCHAR(100) NOT NULL,
    purpose TEXT NOT NULL,
    legal_basis VARCHAR(50) NOT NULL,
    data_categories TEXT[], -- Array of data categories
    data_subjects TEXT[], -- Array of data subject categories
    recipients TEXT[], -- Array of recipient categories
    
    -- International transfers
    third_country_transfers BOOLEAN NOT NULL DEFAULT false,
    adequacy_decision BOOLEAN,
    safeguards TEXT,
    
    -- Retention
    retention_period_days INTEGER,
    
    -- Security measures
    security_measures TEXT[],
    
    -- Status
    active BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_dpa_legal_basis CHECK (legal_basis IN ('CONSENT', 'CONTRACT', 'LEGAL_OBLIGATION', 'VITAL_INTERESTS', 'PUBLIC_TASK', 'LEGITIMATE_INTERESTS'))
);

-- Insert default processing activities
INSERT INTO data_processing_activities (
    activity_name, purpose, legal_basis, data_categories, data_subjects, recipients, retention_period_days, security_measures
) VALUES
    ('User Registration', 'Create and manage user accounts', 'CONTRACT', 
     ARRAY['Identity data', 'Contact data'], ARRAY['Customers'], ARRAY['Internal staff'], 2555,
     ARRAY['Encryption at rest', 'Encryption in transit', 'Access controls']),
    ('Service Delivery', 'Provide core platform services', 'CONTRACT',
     ARRAY['Identity data', 'Usage data', 'Technical data'], ARRAY['Customers'], ARRAY['Internal staff', 'Service providers'], 2555,
     ARRAY['Encryption at rest', 'Encryption in transit', 'Access controls', 'Audit logging']),
    ('Marketing Communications', 'Send promotional materials', 'CONSENT',
     ARRAY['Contact data', 'Marketing preferences'], ARRAY['Customers'], ARRAY['Internal staff', 'Marketing partners'], 1095,
     ARRAY['Encryption at rest', 'Access controls', 'Opt-out mechanisms']);

-- Create indices for consent management
CREATE INDEX idx_consent_categories_key ON consent_categories(category_key);
CREATE INDEX idx_consent_categories_effective ON consent_categories(effective_from, effective_until);

CREATE INDEX idx_consent_history_user_id ON consent_history(user_id);
CREATE INDEX idx_consent_history_consent_key ON consent_history(consent_key);
CREATE INDEX idx_consent_history_granted_at ON consent_history(granted_at);

CREATE INDEX idx_privacy_settings_deletion_pending ON privacy_settings(deletion_request_pending) WHERE deletion_request_pending = true;
CREATE INDEX idx_privacy_settings_deletion_scheduled ON privacy_settings(deletion_scheduled_at) WHERE deletion_scheduled_at IS NOT NULL;

CREATE INDEX idx_data_retention_policies_active ON data_retention_policies(active) WHERE active = true;
CREATE INDEX idx_data_processing_activities_active ON data_processing_activities(active) WHERE active = true;

-- Create trigger for consent history
CREATE OR REPLACE FUNCTION log_consent_change()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO consent_history (
        user_id, consent_key, granted, previous_state, consent_version, 
        ip_address, user_agent, source, legal_basis
    ) VALUES (
        NEW.user_id, NEW.consent_key, NEW.granted, 
        CASE WHEN TG_OP = 'UPDATE' THEN OLD.granted ELSE NULL END,
        NEW.consent_version, NEW.ip_address, NEW.user_agent, 
        COALESCE(NEW.source, 'SYSTEM'), COALESCE(NEW.legal_basis, 'CONSENT')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER consent_change_trigger
    AFTER INSERT OR UPDATE ON consents
    FOR EACH ROW EXECUTE FUNCTION log_consent_change();

-- Create trigger for privacy settings updates
CREATE TRIGGER update_privacy_settings_updated_at BEFORE UPDATE ON privacy_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to check consent validity
CREATE OR REPLACE FUNCTION is_consent_valid(user_uuid UUID, consent_key_param VARCHAR)
RETURNS BOOLEAN AS $$
DECLARE
    consent_record RECORD;
    category_record RECORD;
BEGIN
    -- Get current consent
    SELECT * INTO consent_record 
    FROM consents 
    WHERE user_id = user_uuid AND consent_key = consent_key_param;
    
    -- If no consent record exists, check if it's required
    IF NOT FOUND THEN
        SELECT * INTO category_record 
        FROM consent_categories 
        WHERE category_key = consent_key_param AND required = true;
        
        -- If required consent is missing, return false
        IF FOUND THEN
            RETURN false;
        ELSE
            -- If not required, assume default
            RETURN true;
        END IF;
    END IF;
    
    -- Return the granted status
    RETURN consent_record.granted;
END;
$$ LANGUAGE plpgsql;

-- Create function for GDPR data export
CREATE OR REPLACE FUNCTION export_user_data(user_uuid UUID)
RETURNS JSONB AS $$
DECLARE
    user_data JSONB;
    addresses_data JSONB;
    consents_data JSONB;
    audit_data JSONB;
BEGIN
    -- Get user profile (decrypt in application layer)
    SELECT jsonb_build_object(
        'id', id,
        'reference_id', reference_id,
        'created_at', created_at,
        'updated_at', updated_at,
        'active', active
    ) INTO user_data
    FROM users WHERE id = user_uuid;
    
    -- Get addresses
    SELECT jsonb_agg(jsonb_build_object(
        'id', id,
        'type', type,
        'is_primary', is_primary,
        'created_at', created_at
    )) INTO addresses_data
    FROM addresses WHERE user_id = user_uuid;
    
    -- Get consents
    SELECT jsonb_agg(jsonb_build_object(
        'consent_key', consent_key,
        'granted', granted,
        'granted_at', granted_at,
        'consent_version', consent_version
    )) INTO consents_data
    FROM consents WHERE user_id = user_uuid;
    
    -- Get audit trail (limited)
    SELECT jsonb_agg(jsonb_build_object(
        'event_type', event_type,
        'created_at', created_at
    )) INTO audit_data
    FROM user_audit 
    WHERE user_id = user_uuid 
    ORDER BY created_at DESC 
    LIMIT 100;
    
    RETURN jsonb_build_object(
        'user', user_data,
        'addresses', COALESCE(addresses_data, '[]'::jsonb),
        'consents', COALESCE(consents_data, '[]'::jsonb),
        'audit_trail', COALESCE(audit_data, '[]'::jsonb),
        'exported_at', CURRENT_TIMESTAMP
    );
END;
$$ LANGUAGE plpgsql;
