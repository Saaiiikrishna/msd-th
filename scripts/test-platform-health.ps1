# MySillyDreams Platform Health Check Script
# Tests the platform components without full infrastructure

Write-Host "=== MySillyDreams Platform Health Check ===" -ForegroundColor Green
Write-Host ""

# Test 1: Check if Java is available for backend services
Write-Host "1. Checking Java availability..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "   ✓ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Java not found - required for backend services" -ForegroundColor Red
}

# Test 2: Check if Node.js is available for frontend
Write-Host "2. Checking Node.js availability..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version
    Write-Host "   ✓ Node.js found: $nodeVersion" -ForegroundColor Green
}
catch {
    Write-Host "   ✗ Node.js not found - required for frontend" -ForegroundColor Red
}

# Test 3: Check if Docker is running
Write-Host "3. Checking Docker availability..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version
    Write-Host "   ✓ Docker found: $dockerVersion" -ForegroundColor Green

    # Check if Docker is running
    $dockerInfo = docker info 2>&1
    if ($dockerInfo -match "error") {
        Write-Host "   ⚠ Docker found but not running" -ForegroundColor Yellow
    } else {
        Write-Host "   ✓ Docker is running" -ForegroundColor Green
    }
}
catch {
    Write-Host "   ✗ Docker not found - required for infrastructure" -ForegroundColor Red
}

# Test 4: Check service configurations
Write-Host "4. Checking service configurations..." -ForegroundColor Yellow

$services = @(
    @{Name="API Gateway"; Path="api-gateway/build.gradle"; Port=8080},
    @{Name="Auth Service"; Path="auth-service/build.gradle"; Port=8082},
    @{Name="User Service"; Path="user-service/src/main/resources/application.yml"; Port=8083},
    @{Name="Payment Service"; Path="payment-service/build.gradle.kts"; Port=8081},
    @{Name="Treasure Service"; Path="Treasure/build.gradle.kts"; Port=8084},
    @{Name="Frontend"; Path="frontend/package.json"; Port=3000}
)

foreach ($service in $services) {
    if (Test-Path $service.Path) {
        Write-Host "   ✓ $($service.Name) configuration found" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $($service.Name) configuration missing: $($service.Path)" -ForegroundColor Red
    }
}

# Test 5: Check database schemas
Write-Host "5. Checking database schemas..." -ForegroundColor Yellow

$schemas = @(
    @{Name="User Service"; Path="user-service/src/main/resources/db/migration"},
    @{Name="Payment Service"; Path="payment-service/create_tables.sql"},
    @{Name="Treasure Service"; Path="Treasure/src/main/resources/db/migration"}
)

foreach ($schema in $schemas) {
    if (Test-Path $schema.Path) {
        Write-Host "   ✓ $($schema.Name) database schema found" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $($schema.Name) database schema missing: $($schema.Path)" -ForegroundColor Red
    }
}

# Test 6: Check Docker Compose files
Write-Host "6. Checking Docker Compose configurations..." -ForegroundColor Yellow

$composeFiles = @(
    "docker-compose-infrastructure.yml",
    "auth-service/docker-compose-keycloak.yml"
)

foreach ($file in $composeFiles) {
    if (Test-Path $file) {
        Write-Host "   ✓ $file found" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $file missing" -ForegroundColor Red
    }
}

# Test 7: Check frontend dependencies
Write-Host "7. Checking frontend setup..." -ForegroundColor Yellow
if (Test-Path "frontend/node_modules") {
    Write-Host "   ✓ Frontend dependencies installed" -ForegroundColor Green
} else {
    Write-Host "   ⚠ Frontend dependencies not installed - run 'npm install' in frontend directory" -ForegroundColor Yellow
}

# Test 8: Architecture validation
Write-Host "8. Validating architecture components..." -ForegroundColor Yellow

# Check for key architecture files
$archFiles = @(
    @{Name="API Gateway Routes"; Path="api-gateway/src/main/resources/application.yml"},
    @{Name="Auth Service Keycloak Config"; Path="auth-service/src/main/resources/application.yml"},
    @{Name="User Service Encryption"; Path="user-service/src/main/java/com/mysillydreams/userservice/service/EncryptionService.java"},
    @{Name="Payment Service Razorpay"; Path="payment-service/src/main/java/com/mysillydreams/paymentservice/service/RazorpayService.java"},
    @{Name="Treasure Service Redis"; Path="Treasure/src/main/java/com/mysillydreams/treasure/config/RedisConfig.java"}
)

foreach ($file in $archFiles) {
    if (Test-Path $file.Path) {
        Write-Host "   ✓ $($file.Name) implementation found" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ $($file.Name) implementation not found: $($file.Path)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== Platform Health Check Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Start infrastructure: docker-compose -f docker-compose-infrastructure.yml up -d" -ForegroundColor White
Write-Host "2. Start Keycloak: docker-compose -f auth-service/docker-compose-keycloak.yml up -d" -ForegroundColor White
Write-Host "3. Start services in order: User Service -> Auth Service -> Payment Service -> Treasure Service -> API Gateway" -ForegroundColor White
Write-Host "4. Start frontend: cd frontend; npm run dev" -ForegroundColor White
Write-Host ""
