# Vault Initialization Script for MySillyDreams Treasure Hunt (PowerShell)
# This script sets up Vault with the required transit engine and encryption keys

param(
    [string]$VaultAddr = "http://localhost:8200",
    [string]$VaultToken = "myroot",
    [string]$TransitBackend = "transit",
    [string]$EncryptionKey = "user_pii",
    [string]$HmacKey = "user_search_hmac"
)

# Function to write colored output
function Write-Info($message) {
    Write-Host "[INFO] $message" -ForegroundColor Blue
}

function Write-Success($message) {
    Write-Host "[SUCCESS] $message" -ForegroundColor Green
}

function Write-Warning($message) {
    Write-Host "[WARNING] $message" -ForegroundColor Yellow
}

function Write-Error($message) {
    Write-Host "[ERROR] $message" -ForegroundColor Red
}

# Function to check if Vault is accessible
function Test-VaultStatus {
    Write-Info "Checking Vault status at $VaultAddr..."
    
    $maxAttempts = 30
    $attempt = 1
    
    while ($attempt -le $maxAttempts) {
        try {
            $response = Invoke-RestMethod -Uri "$VaultAddr/v1/sys/health" -Method Get -TimeoutSec 5
            Write-Success "Vault is accessible"
            return $true
        }
        catch {
            Write-Info "Attempt $attempt/$maxAttempts`: Waiting for Vault to be ready..."
            Start-Sleep -Seconds 2
            $attempt++
        }
    }
    
    Write-Error "Vault is not accessible after $maxAttempts attempts"
    return $false
}

# Function to check if transit engine is enabled
function Test-TransitEngine {
    Write-Info "Checking if transit engine is enabled..."
    
    try {
        $headers = @{ "X-Vault-Token" = $VaultToken }
        $response = Invoke-RestMethod -Uri "$VaultAddr/v1/sys/mounts" -Method Get -Headers $headers
        
        if ($response.PSObject.Properties.Name -contains "$TransitBackend/") {
            Write-Success "Transit engine is already enabled at $TransitBackend/"
            return $true
        }
        else {
            Write-Info "Transit engine is not enabled"
            return $false
        }
    }
    catch {
        Write-Error "Failed to check transit engine status: $($_.Exception.Message)"
        return $false
    }
}

# Function to enable transit engine
function Enable-TransitEngine {
    Write-Info "Enabling transit engine at $TransitBackend/..."
    
    try {
        $headers = @{ 
            "X-Vault-Token" = $VaultToken
            "Content-Type" = "application/json"
        }
        $body = @{
            type = "transit"
            description = "Transit engine for PII encryption"
        } | ConvertTo-Json
        
        Invoke-RestMethod -Uri "$VaultAddr/v1/sys/mounts/$TransitBackend" -Method Post -Headers $headers -Body $body
        Write-Success "Transit engine enabled successfully"
        return $true
    }
    catch {
        Write-Error "Failed to enable transit engine: $($_.Exception.Message)"
        return $false
    }
}

# Function to check if a key exists
function Test-KeyExists($keyName) {
    Write-Info "Checking if key '$keyName' exists..."
    
    try {
        $headers = @{ "X-Vault-Token" = $VaultToken }
        Invoke-RestMethod -Uri "$VaultAddr/v1/$TransitBackend/keys/$keyName" -Method Get -Headers $headers
        Write-Success "Key '$keyName' already exists"
        return $true
    }
    catch {
        Write-Info "Key '$keyName' does not exist"
        return $false
    }
}

# Function to create an encryption key
function New-EncryptionKey($keyName, $keyType = "aes256-gcm96") {
    Write-Info "Creating encryption key '$keyName' with type '$keyType'..."
    
    try {
        $headers = @{ 
            "X-Vault-Token" = $VaultToken
            "Content-Type" = "application/json"
        }
        $body = @{ type = $keyType } | ConvertTo-Json
        
        Invoke-RestMethod -Uri "$VaultAddr/v1/$TransitBackend/keys/$keyName" -Method Post -Headers $headers -Body $body
        Write-Success "Encryption key '$keyName' created successfully"
        return $true
    }
    catch {
        Write-Error "Failed to create encryption key '$keyName': $($_.Exception.Message)"
        return $false
    }
}

# Function to create HMAC key
function New-HmacKey($keyName) {
    Write-Info "Creating HMAC key '$keyName'..."
    
    try {
        $headers = @{ 
            "X-Vault-Token" = $VaultToken
            "Content-Type" = "application/json"
        }
        $body = @{ type = "hmac" } | ConvertTo-Json
        
        Invoke-RestMethod -Uri "$VaultAddr/v1/$TransitBackend/keys/$keyName" -Method Post -Headers $headers -Body $body
        Write-Success "HMAC key '$keyName' created successfully"
        return $true
    }
    catch {
        Write-Error "Failed to create HMAC key '$keyName': $($_.Exception.Message)"
        return $false
    }
}

# Function to test encryption/decryption
function Test-Encryption($keyName) {
    Write-Info "Testing encryption/decryption with key '$keyName'..."
    
    try {
        $testData = "test-data-$(Get-Date -Format 'yyyyMMddHHmmss')"
        $testDataBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($testData))
        
        $headers = @{ 
            "X-Vault-Token" = $VaultToken
            "Content-Type" = "application/json"
        }
        
        # Encrypt
        $encryptBody = @{ plaintext = $testDataBase64 } | ConvertTo-Json
        $encryptResponse = Invoke-RestMethod -Uri "$VaultAddr/v1/$TransitBackend/encrypt/$keyName" -Method Post -Headers $headers -Body $encryptBody
        
        if (-not $encryptResponse.data.ciphertext) {
            Write-Error "No ciphertext returned from encryption"
            return $false
        }
        
        # Decrypt
        $decryptBody = @{ ciphertext = $encryptResponse.data.ciphertext } | ConvertTo-Json
        $decryptResponse = Invoke-RestMethod -Uri "$VaultAddr/v1/$TransitBackend/decrypt/$keyName" -Method Post -Headers $headers -Body $decryptBody
        
        $decryptedData = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($decryptResponse.data.plaintext))
        
        if ($decryptedData -eq $testData) {
            Write-Success "Encryption/decryption test passed for key '$keyName'"
            return $true
        }
        else {
            Write-Error "Encryption/decryption test failed: data mismatch"
            return $false
        }
    }
    catch {
        Write-Error "Encryption test failed: $($_.Exception.Message)"
        return $false
    }
}

# Function to test HMAC
function Test-Hmac($keyName) {
    Write-Info "Testing HMAC with key '$keyName'..."
    
    try {
        $testData = "test-hmac-data-$(Get-Date -Format 'yyyyMMddHHmmss')"
        $testDataBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($testData))
        
        $headers = @{ 
            "X-Vault-Token" = $VaultToken
            "Content-Type" = "application/json"
        }
        $body = @{ input = $testDataBase64 } | ConvertTo-Json
        
        $response = Invoke-RestMethod -Uri "$VaultAddr/v1/$TransitBackend/hmac/$keyName" -Method Post -Headers $headers -Body $body
        
        if ($response.data.hmac) {
            Write-Success "HMAC test passed for key '$keyName'"
            return $true
        }
        else {
            Write-Error "HMAC test failed: no HMAC value returned"
            return $false
        }
    }
    catch {
        Write-Error "HMAC test failed: $($_.Exception.Message)"
        return $false
    }
}

# Main function
function Main {
    Write-Info "Starting Vault initialization for MySillyDreams Treasure Hunt..."
    Write-Info "Vault Address: $VaultAddr"
    Write-Info "Transit Backend: $TransitBackend"
    Write-Info "Encryption Key: $EncryptionKey"
    Write-Info "HMAC Key: $HmacKey"
    Write-Host ""
    
    # Check Vault status
    if (-not (Test-VaultStatus)) {
        Write-Error "Cannot proceed without Vault access"
        exit 1
    }
    
    # Enable transit engine if not already enabled
    if (-not (Test-TransitEngine)) {
        if (-not (Enable-TransitEngine)) {
            Write-Error "Failed to enable transit engine"
            exit 1
        }
    }
    
    # Create encryption key if it doesn't exist
    if (-not (Test-KeyExists $EncryptionKey)) {
        if (-not (New-EncryptionKey $EncryptionKey "aes256-gcm96")) {
            Write-Error "Failed to create encryption key"
            exit 1
        }
    }
    
    # Create HMAC key if it doesn't exist
    if (-not (Test-KeyExists $HmacKey)) {
        if (-not (New-HmacKey $HmacKey)) {
            Write-Error "Failed to create HMAC key"
            exit 1
        }
    }
    
    # Test encryption functionality
    if (-not (Test-Encryption $EncryptionKey)) {
        Write-Error "Encryption test failed"
        exit 1
    }
    
    # Test HMAC functionality
    if (-not (Test-Hmac $HmacKey)) {
        Write-Error "HMAC test failed"
        exit 1
    }
    
    Write-Host ""
    Write-Success "🎉 Vault initialization completed successfully!"
    Write-Info "Transit engine is ready at: $VaultAddr/v1/$TransitBackend"
    Write-Info "Encryption key: $EncryptionKey"
    Write-Info "HMAC key: $HmacKey"
}

# Run main function
Main
