# Test final login with corrected client secret
$headers = @{
    'Content-Type' = 'application/json'
}

$loginBody = @{
    email = "testuser@example.com"
    password = "SecurePassword123"
} | ConvertTo-Json

Write-Host "Testing login with corrected client secret..."
Write-Host "Login body: $loginBody"

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/v1/login" -Method Post -Headers $headers -Body $loginBody
    Write-Host "Login successful!"
    Write-Host "Response: $($response | ConvertTo-Json -Depth 10)"
} catch {
    Write-Host "Login failed!"
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response body: $responseBody"
    }
}
