-- User Service Database Schema V2
-- Adds additional constraints and non-concurrent indices for performance optimization

-- Note: Concurrent indices are handled in a separate migration (V2_1) to avoid mixed transactional/non-transactional statements

ALTER TABLE users ADD CONSTRAINT chk_email_hmac_format 
    CHECK (email_hmac IS NULL OR email_hmac ~ '^[a-f0-9]{64}$');

ALTER TABLE users ADD CONSTRAINT chk_phone_hmac_format 
    CHECK (phone_hmac IS NULL OR phone_hmac ~ '^[a-f0-9]{64}$');

-- Add constraint to ensure at least one contact method
ALTER TABLE users ADD CONSTRAINT chk_contact_method 
    CHECK (email_enc IS NOT NULL OR phone_enc IS NOT NULL);

-- Add constraint for role format
ALTER TABLE user_roles ADD CONSTRAINT chk_role_format 
    CHECK (role ~ '^ROLE_[A-Z_]+$');

-- Add constraint for consent key format
ALTER TABLE consents ADD CONSTRAINT chk_consent_key_format 
    CHECK (consent_key ~ '^[a-z_]+$');

-- Add constraint for external system format
ALTER TABLE user_external_refs ADD CONSTRAINT chk_external_id_not_empty 
    CHECK (LENGTH(TRIM(external_id)) > 0);

-- Add constraint for session token hash format
ALTER TABLE sessions ADD CONSTRAINT chk_session_token_hash_format 
    CHECK (session_token_hash ~ '^[a-f0-9]{64}$');

-- Add constraint for session expiry
ALTER TABLE sessions ADD CONSTRAINT chk_session_expiry 
    CHECK (expires_at > created_at);

-- Add constraint for outbox event data
ALTER TABLE outbox_events ADD CONSTRAINT chk_event_data_not_empty 
    CHECK (jsonb_typeof(event_data) = 'object' AND event_data != '{}'::jsonb);

-- Create function to clean up expired sessions
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    UPDATE sessions 
    SET active = false, 
        terminated_at = CURRENT_TIMESTAMP,
        termination_reason = 'EXPIRED'
    WHERE active = true 
      AND expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to clean up old audit records (for GDPR compliance)
CREATE OR REPLACE FUNCTION cleanup_old_audit_records(retention_days INTEGER DEFAULT 365)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM user_audit 
    WHERE created_at < CURRENT_TIMESTAMP - (retention_days || ' days')::INTERVAL
      AND event_type NOT IN ('USER_DELETED', 'CONSENT_WITHDRAWN'); -- Keep important events longer
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to anonymize deleted user data
CREATE OR REPLACE FUNCTION anonymize_deleted_user(user_uuid UUID)
RETURNS BOOLEAN AS $$
BEGIN
    -- Update user record to remove PII
    UPDATE users 
    SET first_name_enc = 'DELETED_USER',
        last_name_enc = 'DELETED_USER',
        email_enc = NULL,
        phone_enc = NULL,
        dob_enc = NULL,
        email_hmac = NULL,
        phone_hmac = NULL,
        avatar_url = NULL,
        reference_id = uuid_generate_v4()::TEXT
    WHERE id = user_uuid;
    
    -- Remove addresses
    DELETE FROM addresses WHERE user_id = user_uuid;
    
    -- Keep consents for legal compliance but anonymize
    UPDATE consents 
    SET ip_address = NULL,
        user_agent = NULL
    WHERE user_id = user_uuid;
    
    -- Anonymize audit records
    UPDATE user_audit 
    SET ip_address = NULL,
        user_agent = NULL,
        details = CASE 
            WHEN details ? 'email' THEN jsonb_set(details, '{email}', '"[REDACTED]"')
            WHEN details ? 'phone' THEN jsonb_set(details, '{phone}', '"[REDACTED]"')
            ELSE details
        END
    WHERE user_id = user_uuid;
    
    -- Remove sessions
    DELETE FROM sessions WHERE user_id = user_uuid;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE users IS 'Core user identity and profile information with encrypted PII';
COMMENT ON TABLE user_roles IS 'User role assignments for authorization';
COMMENT ON TABLE addresses IS 'User addresses with encrypted fields';
COMMENT ON TABLE consents IS 'GDPR/DPDP consent management';
COMMENT ON TABLE user_audit IS 'Audit trail for user-related actions';
COMMENT ON TABLE user_external_refs IS 'Cross-service user references';
COMMENT ON TABLE sessions IS 'Session metadata (actual sessions managed by Auth service)';
COMMENT ON TABLE outbox_events IS 'Transactional outbox for reliable event publishing';

COMMENT ON COLUMN users.email_hmac IS 'HMAC-SHA256 hash of normalized email for uniqueness and search';
COMMENT ON COLUMN users.phone_hmac IS 'HMAC-SHA256 hash of normalized phone for uniqueness and search';
COMMENT ON COLUMN users.reference_id IS 'Business reference ID for external API usage';
COMMENT ON COLUMN sessions.session_token_hash IS 'SHA-256 hash of session token for lookup without storing actual token';
