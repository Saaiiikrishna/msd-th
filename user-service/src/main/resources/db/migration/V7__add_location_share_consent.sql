-- Migration to add location_share consent category for Treasure Service integration
-- This consent is required by the architecture for location tracking during trips

-- Insert location_share consent category if it doesn't exist
INSERT INTO consent_categories (id, category_key, name, description, required, default_granted, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'location_share',
    'Location Sharing',
    'Permission to share location data for trip tracking and treasure hunt features',
    false,  -- Not required by default
    false,  -- Default to false as specified in architecture
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM consent_categories WHERE category_key = 'location_share'
);

-- Add comment for documentation
COMMENT ON TABLE consent_categories IS 'Consent categories for GDPR/DPDP compliance. location_share is required for Treasure Service integration.';
