-- User Service Database Schema V1
-- Creates core tables for user identity, roles, addresses, consents, and audit

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table (core identity)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference_id VARCHAR(50) NOT NULL UNIQUE,
    
    -- PII fields (encrypted)
    first_name_enc TEXT,
    last_name_enc TEXT,
    email_enc TEXT,
    phone_enc TEXT,
    dob_enc TEXT,
    
    -- Search HMAC fields (for uniqueness and search)
    email_hmac VARCHAR(64) UNIQUE,
    phone_hmac VARCHAR(64) UNIQUE,
    
    -- Profile fields
    gender VARCHAR(10),
    avatar_url TEXT,
    
    -- Status fields
    active BOOLEAN NOT NULL DEFAULT true,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY')),
    CONSTRAINT chk_active_deleted CHECK (
        (active = true AND deleted_at IS NULL) OR 
        (active = false AND deleted_at IS NOT NULL)
    )
);

-- User roles table (many-to-many relationship)
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID, -- Reference to admin user who assigned the role
    
    PRIMARY KEY (user_id, role)
);

-- Addresses table (one-to-many relationship)
CREATE TABLE addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Address type
    type VARCHAR(20) NOT NULL DEFAULT 'HOME',
    
    -- Address fields (encrypted)
    line1_enc TEXT,
    line2_enc TEXT,
    city_enc TEXT,
    state_enc TEXT,
    postal_code_enc TEXT,
    country_enc TEXT,
    
    -- Metadata
    is_primary BOOLEAN NOT NULL DEFAULT false,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_address_type CHECK (type IN ('HOME', 'WORK', 'BILLING', 'SHIPPING', 'OTHER'))
);

-- Consents table (GDPR/DPDP compliance)
CREATE TABLE consents (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_key VARCHAR(100) NOT NULL,
    granted BOOLEAN NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    withdrawn_at TIMESTAMP WITH TIME ZONE,
    
    -- Metadata
    consent_version VARCHAR(10),
    ip_address INET,
    user_agent TEXT,
    
    PRIMARY KEY (user_id, consent_key),
    
    -- Constraints
    CONSTRAINT chk_consent_state CHECK (
        (granted = true AND withdrawn_at IS NULL) OR 
        (granted = false AND withdrawn_at IS NOT NULL)
    )
);

-- User audit table (audit trail)
CREATE TABLE user_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL,
    details JSONB,
    
    -- Context
    performed_by UUID, -- User who performed the action
    ip_address INET,
    user_agent TEXT,
    
    -- Timestamp
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_event_type CHECK (event_type IN (
        'USER_CREATED', 'USER_UPDATED', 'USER_DELETED', 'USER_ARCHIVED',
        'ROLE_ASSIGNED', 'ROLE_REMOVED', 'CONSENT_GRANTED', 'CONSENT_WITHDRAWN',
        'ADDRESS_ADDED', 'ADDRESS_UPDATED', 'ADDRESS_DELETED',
        'LOGIN_SUCCESS', 'LOGIN_FAILED', 'PASSWORD_CHANGED'
    ))
);

-- User external references table (for cross-service references)
CREATE TABLE user_external_refs (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    system VARCHAR(50) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, system),
    UNIQUE (system, external_id),
    
    -- Constraints
    CONSTRAINT chk_system CHECK (system IN ('PAYMENTS', 'ORDERS', 'NOTIFICATIONS', 'ANALYTICS'))
);

-- Sessions table (for session metadata - actual sessions managed by Auth service)
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Session metadata
    session_token_hash VARCHAR(64) NOT NULL UNIQUE, -- Hash of the actual token
    device_info JSONB,
    ip_address INET,
    user_agent TEXT,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Status
    active BOOLEAN NOT NULL DEFAULT true,
    terminated_at TIMESTAMP WITH TIME ZONE,
    termination_reason VARCHAR(50)
);

-- Outbox table (for transactional outbox pattern)
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    
    -- Processing status
    processed BOOLEAN NOT NULL DEFAULT false,
    processed_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_aggregate_type CHECK (aggregate_type IN ('USER', 'ADDRESS', 'CONSENT')),
    CONSTRAINT chk_retry_count CHECK (retry_count >= 0 AND retry_count <= 10)
);

-- Create indexes for performance
CREATE INDEX idx_users_reference_id ON users(reference_id);
CREATE INDEX idx_users_email_hmac ON users(email_hmac) WHERE email_hmac IS NOT NULL;
CREATE INDEX idx_users_phone_hmac ON users(phone_hmac) WHERE phone_hmac IS NOT NULL;
CREATE INDEX idx_users_active ON users(active);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NOT NULL;

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
CREATE INDEX idx_addresses_type ON addresses(type);
CREATE INDEX idx_addresses_is_primary ON addresses(is_primary) WHERE is_primary = true;

CREATE INDEX idx_consents_user_id ON consents(user_id);
CREATE INDEX idx_consents_key ON consents(consent_key);
CREATE INDEX idx_consents_granted ON consents(granted);

CREATE INDEX idx_user_audit_user_id ON user_audit(user_id);
CREATE INDEX idx_user_audit_event_type ON user_audit(event_type);
CREATE INDEX idx_user_audit_created_at ON user_audit(created_at);

CREATE INDEX idx_user_external_refs_user_id ON user_external_refs(user_id);
CREATE INDEX idx_user_external_refs_system ON user_external_refs(system);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_token_hash ON sessions(session_token_hash);
CREATE INDEX idx_sessions_active ON sessions(active);
CREATE INDEX idx_sessions_expires_at ON sessions(expires_at);

CREATE INDEX idx_outbox_events_processed ON outbox_events(processed, created_at);
CREATE INDEX idx_outbox_events_aggregate ON outbox_events(aggregate_type, aggregate_id);

-- Create trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to relevant tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_addresses_updated_at BEFORE UPDATE ON addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Note: Default roles will be inserted when users are created through the application

-- Create a function to ensure only one primary address per user
CREATE OR REPLACE FUNCTION ensure_single_primary_address()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_primary = true THEN
        -- Set all other addresses for this user to non-primary
        UPDATE addresses 
        SET is_primary = false 
        WHERE user_id = NEW.user_id AND id != NEW.id;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER ensure_single_primary_address_trigger
    BEFORE INSERT OR UPDATE ON addresses
    FOR EACH ROW EXECUTE FUNCTION ensure_single_primary_address();
