$token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJZbTJNMG16SVRRbDBZRmt4WVVNNFZqZnF0bUxIZnNSZFZScXZUZEh6NGZRIn0.eyJleHAiOjE3NTc4NTY5NjEsImlhdCI6MTc1Nzg1NjkwMSwianRpIjoiYWU0MmU4NmUtZDZlMy00MGFlLTk3MmQtOTQ4OTU5MjM4YWNlIiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvcmVhbG1zL21hc3RlciIsInN1YiI6IjIwOGY5N2VlLTZiZmEtNGU3ZC05Yzc5LWI1MWFmNzg5MTVhMiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImFkbWluLWNsaSIsInNlc3Npb25fc3RhdGUiOiI3Y2RhZTJlOC01ZGM4LTRkMDYtODQ5MS0xY2ZhNmQ1ZDcwZTAiLCJhY3IiOiIxIiwic2NvcGUiOiJlbWFpbCBwcm9maWxlIiwic2lkIjoiN2NkYWUyZTgtNWRjOC00ZDA2LTg0OTEtMWNmYTZkNWQ3MGUwIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhZG1pbiJ9.jkjy5UmKb_plBPXVMhlbfHCl5XHutQmDW6nMQ9tyoR49PflEsqoQhjyUYH0DgvslF-B41SL1Eql1Q8DGxD2DuSPE-8rQFiBdmL2keVy6Tp9TG-3uipYfHJXndpf7jpcB57A9hbB33Ed0_4W34Km8v_p-YKAisE0wTotIOrvH8eQG5WGdLLEDW6XBqVjlZsG4jQ1rfZvMhcWRiQhkZGtn_oxdkCXKJF5hllTYK5bHRZq5RRn_X7X3OnLlVFHo_o7kguu7SGV3KB_dcksPjzLn6dbq2uY2YTt3LO-mcKo-GE5cFlee_CeHIEoAkRWlmdWrGQS6w99rM6c9o6A4Tzx3AA"

$headers = @{
    'Authorization' = "Bearer $token"
    'Content-Type' = 'application/json'
}

$body = Get-Content 'create-auth-client.json' -Raw

try {
    $response = Invoke-RestMethod -Uri 'http://localhost:8080/admin/realms/treasure-hunt/clients' -Method Post -Headers $headers -Body $body
    Write-Host "✅ Auth-service client created successfully"
    $response
} catch {
    Write-Host "❌ Error creating client: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody"
    }
}
