#!/bin/bash

# Vault Initialization Script for MySillyDreams Treasure Hunt
# This script sets up Vault with the required transit engine and encryption keys

set -e

# Configuration
VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"
VAULT_TOKEN="${VAULT_TOKEN:-myroot}"
TRANSIT_BACKEND="${TRANSIT_BACKEND:-transit}"
ENCRYPTION_KEY="${ENCRYPTION_KEY:-user_pii}"
HMAC_KEY="${HMAC_KEY:-user_search_hmac}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Vault is accessible
check_vault_status() {
    log_info "Checking Vault status at $VAULT_ADDR..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$VAULT_ADDR/v1/sys/health" > /dev/null 2>&1; then
            log_success "Vault is accessible"
            return 0
        fi
        
        log_info "Attempt $attempt/$max_attempts: Waiting for Vault to be ready..."
        sleep 2
        ((attempt++))
    done
    
    log_error "Vault is not accessible after $max_attempts attempts"
    return 1
}

# Function to check if transit engine is enabled
check_transit_engine() {
    log_info "Checking if transit engine is enabled..."
    
    local response=$(curl -s -H "X-Vault-Token: $VAULT_TOKEN" \
        "$VAULT_ADDR/v1/sys/mounts" 2>/dev/null)
    
    if echo "$response" | grep -q "\"$TRANSIT_BACKEND/\""; then
        log_success "Transit engine is already enabled at $TRANSIT_BACKEND/"
        return 0
    else
        log_info "Transit engine is not enabled"
        return 1
    fi
}

# Function to enable transit engine
enable_transit_engine() {
    log_info "Enabling transit engine at $TRANSIT_BACKEND/..."
    
    local response=$(curl -s -w "%{http_code}" -o /tmp/vault_response.json \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -X POST \
        -d '{"type":"transit","description":"Transit engine for PII encryption"}' \
        "$VAULT_ADDR/v1/sys/mounts/$TRANSIT_BACKEND")
    
    if [ "$response" = "204" ]; then
        log_success "Transit engine enabled successfully"
        return 0
    else
        log_error "Failed to enable transit engine. HTTP status: $response"
        cat /tmp/vault_response.json 2>/dev/null || true
        return 1
    fi
}

# Function to check if a key exists
check_key_exists() {
    local key_name="$1"
    log_info "Checking if key '$key_name' exists..."
    
    local response=$(curl -s -w "%{http_code}" -o /tmp/vault_key_response.json \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        "$VAULT_ADDR/v1/$TRANSIT_BACKEND/keys/$key_name")
    
    if [ "$response" = "200" ]; then
        log_success "Key '$key_name' already exists"
        return 0
    else
        log_info "Key '$key_name' does not exist"
        return 1
    fi
}

# Function to create an encryption key
create_encryption_key() {
    local key_name="$1"
    local key_type="${2:-aes256-gcm96}"
    
    log_info "Creating encryption key '$key_name' with type '$key_type'..."
    
    local response=$(curl -s -w "%{http_code}" -o /tmp/vault_create_response.json \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -X POST \
        -d "{\"type\":\"$key_type\"}" \
        "$VAULT_ADDR/v1/$TRANSIT_BACKEND/keys/$key_name")
    
    if [ "$response" = "204" ]; then
        log_success "Encryption key '$key_name' created successfully"
        return 0
    else
        log_error "Failed to create encryption key '$key_name'. HTTP status: $response"
        cat /tmp/vault_create_response.json 2>/dev/null || true
        return 1
    fi
}

# Function to test encryption/decryption
test_encryption() {
    local key_name="$1"
    local test_data="test-data-$(date +%s)"
    
    log_info "Testing encryption/decryption with key '$key_name'..."
    
    # Encrypt test data
    local encrypt_response=$(curl -s -w "%{http_code}" -o /tmp/vault_encrypt_response.json \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -X POST \
        -d "{\"plaintext\":\"$(echo -n "$test_data" | base64)\"}" \
        "$VAULT_ADDR/v1/$TRANSIT_BACKEND/encrypt/$key_name")
    
    if [ "$encrypt_response" != "200" ]; then
        log_error "Encryption test failed. HTTP status: $encrypt_response"
        return 1
    fi
    
    local ciphertext=$(cat /tmp/vault_encrypt_response.json | grep -o '"ciphertext":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$ciphertext" ]; then
        log_error "No ciphertext returned from encryption"
        return 1
    fi
    
    # Decrypt test data
    local decrypt_response=$(curl -s -w "%{http_code}" -o /tmp/vault_decrypt_response.json \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -X POST \
        -d "{\"ciphertext\":\"$ciphertext\"}" \
        "$VAULT_ADDR/v1/$TRANSIT_BACKEND/decrypt/$key_name")
    
    if [ "$decrypt_response" != "200" ]; then
        log_error "Decryption test failed. HTTP status: $decrypt_response"
        return 1
    fi
    
    local decrypted_base64=$(cat /tmp/vault_decrypt_response.json | grep -o '"plaintext":"[^"]*"' | cut -d'"' -f4)
    local decrypted_data=$(echo "$decrypted_base64" | base64 -d)
    
    if [ "$decrypted_data" = "$test_data" ]; then
        log_success "Encryption/decryption test passed for key '$key_name'"
        return 0
    else
        log_error "Encryption/decryption test failed: data mismatch"
        return 1
    fi
}

# Function to create HMAC key
create_hmac_key() {
    local key_name="$1"
    
    log_info "Creating HMAC key '$key_name'..."
    
    local response=$(curl -s -w "%{http_code}" -o /tmp/vault_hmac_response.json \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -X POST \
        -d '{"type":"hmac"}' \
        "$VAULT_ADDR/v1/$TRANSIT_BACKEND/keys/$key_name")
    
    if [ "$response" = "204" ]; then
        log_success "HMAC key '$key_name' created successfully"
        return 0
    else
        log_error "Failed to create HMAC key '$key_name'. HTTP status: $response"
        cat /tmp/vault_hmac_response.json 2>/dev/null || true
        return 1
    fi
}

# Function to test HMAC
test_hmac() {
    local key_name="$1"
    local test_data="test-hmac-data-$(date +%s)"
    
    log_info "Testing HMAC with key '$key_name'..."
    
    local hmac_response=$(curl -s -w "%{http_code}" -o /tmp/vault_hmac_test_response.json \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -X POST \
        -d "{\"input\":\"$(echo -n "$test_data" | base64)\"}" \
        "$VAULT_ADDR/v1/$TRANSIT_BACKEND/hmac/$key_name")
    
    if [ "$hmac_response" = "200" ]; then
        local hmac_value=$(cat /tmp/vault_hmac_test_response.json | grep -o '"hmac":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$hmac_value" ]; then
            log_success "HMAC test passed for key '$key_name'"
            return 0
        fi
    fi
    
    log_error "HMAC test failed for key '$key_name'. HTTP status: $hmac_response"
    return 1
}

# Main initialization function
main() {
    log_info "Starting Vault initialization for MySillyDreams Treasure Hunt..."
    log_info "Vault Address: $VAULT_ADDR"
    log_info "Transit Backend: $TRANSIT_BACKEND"
    log_info "Encryption Key: $ENCRYPTION_KEY"
    log_info "HMAC Key: $HMAC_KEY"
    echo
    
    # Check Vault status
    if ! check_vault_status; then
        log_error "Cannot proceed without Vault access"
        exit 1
    fi
    
    # Enable transit engine if not already enabled
    if ! check_transit_engine; then
        if ! enable_transit_engine; then
            log_error "Failed to enable transit engine"
            exit 1
        fi
    fi
    
    # Create encryption key if it doesn't exist
    if ! check_key_exists "$ENCRYPTION_KEY"; then
        if ! create_encryption_key "$ENCRYPTION_KEY" "aes256-gcm96"; then
            log_error "Failed to create encryption key"
            exit 1
        fi
    fi
    
    # Create HMAC key if it doesn't exist
    if ! check_key_exists "$HMAC_KEY"; then
        if ! create_hmac_key "$HMAC_KEY"; then
            log_error "Failed to create HMAC key"
            exit 1
        fi
    fi
    
    # Test encryption functionality
    if ! test_encryption "$ENCRYPTION_KEY"; then
        log_error "Encryption test failed"
        exit 1
    fi
    
    # Test HMAC functionality
    if ! test_hmac "$HMAC_KEY"; then
        log_error "HMAC test failed"
        exit 1
    fi
    
    echo
    log_success "🎉 Vault initialization completed successfully!"
    log_info "Transit engine is ready at: $VAULT_ADDR/v1/$TRANSIT_BACKEND"
    log_info "Encryption key: $ENCRYPTION_KEY"
    log_info "HMAC key: $HMAC_KEY"
    
    # Cleanup temporary files
    rm -f /tmp/vault_*.json
}

# Cleanup function
cleanup() {
    rm -f /tmp/vault_*.json
}

# Set trap for cleanup
trap cleanup EXIT

# Run main function
main "$@"
