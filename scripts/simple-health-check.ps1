Write-Host "=== MySillyDreams Platform Health Check ===" -ForegroundColor Green

# Check Java
Write-Host "Checking Java..." -ForegroundColor Yellow
$javaCheck = Get-Command java -ErrorAction SilentlyContinue
if ($javaCheck) {
    Write-Host "✓ Java found" -ForegroundColor Green
} else {
    Write-Host "✗ Java not found" -ForegroundColor Red
}

# Check Node.js
Write-Host "Checking Node.js..." -ForegroundColor Yellow
$nodeCheck = Get-Command node -ErrorAction SilentlyContinue
if ($nodeCheck) {
    Write-Host "✓ Node.js found" -ForegroundColor Green
} else {
    Write-Host "✗ Node.js not found" -ForegroundColor Red
}

# Check Docker
Write-Host "Checking Docker..." -ForegroundColor Yellow
$dockerCheck = Get-Command docker -ErrorAction SilentlyContinue
if ($dockerCheck) {
    Write-Host "✓ Docker found" -ForegroundColor Green
} else {
    Write-Host "✗ Docker not found" -ForegroundColor Red
}

# Check service files
Write-Host "Checking service configurations..." -ForegroundColor Yellow

$services = @(
    "api-gateway/build.gradle",
    "auth-service/build.gradle", 
    "user-service/src/main/resources/application.yml",
    "payment-service/build.gradle.kts",
    "Treasure/build.gradle.kts",
    "frontend/package.json"
)

foreach ($service in $services) {
    if (Test-Path $service) {
        Write-Host "✓ $service found" -ForegroundColor Green
    } else {
        Write-Host "✗ $service missing" -ForegroundColor Red
    }
}

Write-Host "=== Health Check Complete ===" -ForegroundColor Green
