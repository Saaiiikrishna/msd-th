@echo off
REM MySillyDreams Treasure Hunt - Kubernetes Deployment Script (Windows)
REM This script deploys the entire application stack to Kubernetes

setlocal enabledelayedexpansion

REM Configuration
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set NAMESPACE_APP=msd-treasure-hunt
set NAMESPACE_INFRA=msd-infrastructure

REM Colors (limited in Windows batch)
set INFO=[INFO]
set SUCCESS=[SUCCESS]
set WARNING=[WARNING]
set ERROR=[ERROR]

echo %INFO% Starting MySillyDreams Treasure Hunt Kubernetes Deployment...
echo.

REM Check prerequisites
echo %INFO% Checking prerequisites...

kubectl version >nul 2>&1
if errorlevel 1 (
    echo %ERROR% kubectl is not installed. Please install kubectl first.
    exit /b 1
)

docker version >nul 2>&1
if errorlevel 1 (
    echo %ERROR% Docker is not installed. Please install Docker first.
    exit /b 1
)

kubectl cluster-info >nul 2>&1
if errorlevel 1 (
    echo %ERROR% Cannot connect to Kubernetes cluster. Please check your kubeconfig.
    exit /b 1
)

echo %SUCCESS% Prerequisites check passed

REM Clean up existing resources if requested
if "%1"=="--clean" (
    echo %INFO% Cleaning up existing resources...
    kubectl delete namespace %NAMESPACE_APP% --ignore-not-found=true
    kubectl delete namespace %NAMESPACE_INFRA% --ignore-not-found=true
    echo %SUCCESS% Cleanup completed
)

REM Build Docker images
echo %INFO% Building Docker images...

cd /d "%PROJECT_ROOT%"

echo %INFO% Building Auth Service...
cd auth-service
call gradlew.bat clean build -x test
docker build -t auth-service:latest .
cd ..

echo %INFO% Building User Service...
cd user-service
call gradlew.bat clean build -x test
docker build -t user-service:latest .
cd ..

echo %INFO% Building Payment Service...
cd payment-service
call gradlew.bat clean build -x test
docker build -t payment-service:latest .
cd ..

echo %INFO% Building Treasure Service...
cd Treasure
call gradlew.bat clean build -x test
docker build -t treasure-service:latest .
cd ..

echo %INFO% Building API Gateway...
cd api-gateway
call gradlew.bat clean build -x test
docker build -t api-gateway:latest .
cd ..

echo %INFO% Building Frontend...
cd frontend
docker build -t frontend:latest .
cd ..

echo %SUCCESS% All images built successfully

REM Deploy infrastructure services
echo %INFO% Deploying infrastructure services...

kubectl apply -f "%SCRIPT_DIR%namespace.yaml"
kubectl apply -f "%SCRIPT_DIR%infrastructure\postgresql.yaml"
kubectl apply -f "%SCRIPT_DIR%infrastructure\redis.yaml"
kubectl apply -f "%SCRIPT_DIR%infrastructure\kafka.yaml"
kubectl apply -f "%SCRIPT_DIR%infrastructure\prometheus.yaml"
kubectl apply -f "%SCRIPT_DIR%infrastructure\grafana.yaml"
kubectl apply -f "%SCRIPT_DIR%infrastructure\zipkin.yaml"
kubectl apply -f "%SCRIPT_DIR%infrastructure\vault.yaml"
kubectl apply -f "%SCRIPT_DIR%infrastructure\keycloak.yaml"

echo %SUCCESS% Infrastructure services deployed

REM Wait for infrastructure to be ready
echo %INFO% Waiting for infrastructure services to be ready...
kubectl wait --for=condition=ready pod -l app=postgres-user -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=postgres-payment -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=postgres-treasure -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=postgres-keycloak -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=zookeeper -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=prometheus -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=grafana -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=zipkin -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=vault -n %NAMESPACE_INFRA% --timeout=300s
kubectl wait --for=condition=ready pod -l app=keycloak -n %NAMESPACE_INFRA% --timeout=600s

echo %SUCCESS% Infrastructure services are ready

REM Deploy application services
echo %INFO% Deploying application services...
kubectl apply -f "%SCRIPT_DIR%services\auth-service.yaml"
kubectl apply -f "%SCRIPT_DIR%services\user-service.yaml"
kubectl apply -f "%SCRIPT_DIR%services\payment-service.yaml"
kubectl apply -f "%SCRIPT_DIR%services\treasure-service.yaml"
kubectl apply -f "%SCRIPT_DIR%services\api-gateway.yaml"
kubectl apply -f "%SCRIPT_DIR%services\frontend.yaml"

echo %SUCCESS% Application services deployed

REM Wait for applications to be ready
echo %INFO% Waiting for application services to be ready...
kubectl wait --for=condition=ready pod -l app=auth-service -n %NAMESPACE_APP% --timeout=300s
kubectl wait --for=condition=ready pod -l app=user-service -n %NAMESPACE_APP% --timeout=300s
kubectl wait --for=condition=ready pod -l app=payment-service -n %NAMESPACE_APP% --timeout=300s
kubectl wait --for=condition=ready pod -l app=treasure-service -n %NAMESPACE_APP% --timeout=300s
kubectl wait --for=condition=ready pod -l app=api-gateway -n %NAMESPACE_APP% --timeout=300s
kubectl wait --for=condition=ready pod -l app=frontend -n %NAMESPACE_APP% --timeout=300s

echo %SUCCESS% Application services are ready

REM Display deployment status
echo.
echo %SUCCESS% 🎉 Deployment completed successfully!
echo.
echo %INFO% Deployment Status:
echo.
echo %INFO% Infrastructure Services:
kubectl get pods -n %NAMESPACE_INFRA% -o wide
echo.
echo %INFO% Application Services:
kubectl get pods -n %NAMESPACE_APP% -o wide
echo.
echo %INFO% Services:
kubectl get svc -n %NAMESPACE_INFRA%
kubectl get svc -n %NAMESPACE_APP%
echo.
echo %INFO% Frontend: http://localhost:3000 (use kubectl port-forward)
echo %INFO% To access frontend: kubectl port-forward svc/frontend -n %NAMESPACE_APP% 3000:3000
echo %INFO% API Gateway: http://localhost:8080 (use kubectl port-forward)
echo %INFO% To access API Gateway: kubectl port-forward svc/api-gateway -n %NAMESPACE_APP% 8080:8080

pause
