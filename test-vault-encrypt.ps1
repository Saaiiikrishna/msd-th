# Test Vault encryption endpoint
$headers = @{
    'X-Vault-Token' = 'myroot'
    'Content-Type' = 'application/json'
}

$body = @{
    plaintext = 'dGVzdA=='
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri 'http://localhost:8200/v1/transit/encrypt/user_pii' -Method Post -Headers $headers -Body $body
    Write-Host "Success: $($response | ConvertTo-Json)" -ForegroundColor Green
}
catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Response: $($_.Exception.Response)" -ForegroundColor Red
}
