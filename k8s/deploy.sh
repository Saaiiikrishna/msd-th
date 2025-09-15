#!/bin/bash

# MySillyDreams Treasure Hunt - Kubernetes Deployment Script
# This script deploys the entire application stack to Kubernetes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
NAMESPACE_APP="msd-treasure-hunt"
NAMESPACE_INFRA="msd-infrastructure"

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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    
    # Check if Docker is installed
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    # Check if Kubernetes cluster is accessible
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Clean up existing resources
cleanup_existing() {
    log_info "Cleaning up existing resources..."
    
    # Delete application namespace (this will delete all resources in it)
    kubectl delete namespace $NAMESPACE_APP --ignore-not-found=true
    kubectl delete namespace $NAMESPACE_INFRA --ignore-not-found=true
    
    # Wait for namespaces to be fully deleted
    log_info "Waiting for namespaces to be deleted..."
    kubectl wait --for=delete namespace/$NAMESPACE_APP --timeout=120s || true
    kubectl wait --for=delete namespace/$NAMESPACE_INFRA --timeout=120s || true
    
    log_success "Cleanup completed"
}

# Build Docker images
build_images() {
    log_info "Building Docker images..."
    
    cd "$PROJECT_ROOT"
    
    # Build Auth Service
    log_info "Building Auth Service..."
    cd auth-service
    ./gradlew clean build -x test
    docker build -t auth-service:latest .
    cd ..
    
    # Build User Service
    log_info "Building User Service..."
    cd user-service
    ./gradlew clean build -x test
    docker build -t user-service:latest .
    cd ..
    
    # Build Payment Service
    log_info "Building Payment Service..."
    cd payment-service
    ./gradlew clean build -x test
    docker build -t payment-service:latest .
    cd ..
    
    # Build Treasure Service
    log_info "Building Treasure Service..."
    cd Treasure
    ./gradlew clean build -x test
    docker build -t treasure-service:latest .
    cd ..
    
    # Build API Gateway
    log_info "Building API Gateway..."
    cd api-gateway
    ./gradlew clean build -x test
    docker build -t api-gateway:latest .
    cd ..
    
    # Build Frontend
    log_info "Building Frontend..."
    cd frontend
    docker build -t frontend:latest .
    cd ..
    
    log_success "All images built successfully"
}

# Deploy infrastructure services
deploy_infrastructure() {
    log_info "Deploying infrastructure services..."
    
    # Create namespaces
    kubectl apply -f "$SCRIPT_DIR/namespace.yaml"
    
    # Deploy PostgreSQL databases
    log_info "Deploying PostgreSQL databases..."
    kubectl apply -f "$SCRIPT_DIR/infrastructure/postgresql.yaml"

    # Deploy Redis
    log_info "Deploying Redis..."
    kubectl apply -f "$SCRIPT_DIR/infrastructure/redis.yaml"

    # Deploy Kafka and Zookeeper
    log_info "Deploying Kafka and Zookeeper..."
    kubectl apply -f "$SCRIPT_DIR/infrastructure/kafka.yaml"

    # Deploy Prometheus
    log_info "Deploying Prometheus..."
    kubectl apply -f "$SCRIPT_DIR/infrastructure/prometheus.yaml"

    # Deploy Grafana
    log_info "Deploying Grafana..."
    kubectl apply -f "$SCRIPT_DIR/infrastructure/grafana.yaml"

    # Deploy Zipkin
    log_info "Deploying Zipkin..."
    kubectl apply -f "$SCRIPT_DIR/infrastructure/zipkin.yaml"

    # Deploy Vault
    log_info "Deploying Vault..."
    kubectl apply -f "$SCRIPT_DIR/infrastructure/vault.yaml"

    # Deploy Keycloak
    log_info "Deploying Keycloak..."
    kubectl apply -f "$SCRIPT_DIR/infrastructure/keycloak.yaml"
    
    log_success "Infrastructure services deployed"
}

# Wait for infrastructure to be ready
wait_for_infrastructure() {
    log_info "Waiting for infrastructure services to be ready..."
    
    # Wait for PostgreSQL databases
    log_info "Waiting for PostgreSQL databases..."
    kubectl wait --for=condition=ready pod -l app=postgres-user -n $NAMESPACE_INFRA --timeout=300s
    kubectl wait --for=condition=ready pod -l app=postgres-payment -n $NAMESPACE_INFRA --timeout=300s
    kubectl wait --for=condition=ready pod -l app=postgres-treasure -n $NAMESPACE_INFRA --timeout=300s
    kubectl wait --for=condition=ready pod -l app=postgres-keycloak -n $NAMESPACE_INFRA --timeout=300s

    # Wait for Redis
    log_info "Waiting for Redis..."
    kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE_INFRA --timeout=300s

    # Wait for Zookeeper and Kafka
    log_info "Waiting for Zookeeper..."
    kubectl wait --for=condition=ready pod -l app=zookeeper -n $NAMESPACE_INFRA --timeout=300s

    log_info "Waiting for Kafka..."
    kubectl wait --for=condition=ready pod -l app=kafka -n $NAMESPACE_INFRA --timeout=300s

    # Wait for monitoring services
    log_info "Waiting for Prometheus..."
    kubectl wait --for=condition=ready pod -l app=prometheus -n $NAMESPACE_INFRA --timeout=300s

    log_info "Waiting for Grafana..."
    kubectl wait --for=condition=ready pod -l app=grafana -n $NAMESPACE_INFRA --timeout=300s

    log_info "Waiting for Zipkin..."
    kubectl wait --for=condition=ready pod -l app=zipkin -n $NAMESPACE_INFRA --timeout=300s

    # Wait for Vault
    log_info "Waiting for Vault..."
    kubectl wait --for=condition=ready pod -l app=vault -n $NAMESPACE_INFRA --timeout=300s

    # Wait for Keycloak
    log_info "Waiting for Keycloak..."
    kubectl wait --for=condition=ready pod -l app=keycloak -n $NAMESPACE_INFRA --timeout=600s
    
    log_success "Infrastructure services are ready"
}

# Deploy application services
deploy_applications() {
    log_info "Deploying application services..."
    
    # Deploy in dependency order
    log_info "Deploying Auth Service..."
    kubectl apply -f "$SCRIPT_DIR/services/auth-service.yaml"
    
    log_info "Deploying User Service..."
    kubectl apply -f "$SCRIPT_DIR/services/user-service.yaml"
    
    log_info "Deploying Payment Service..."
    kubectl apply -f "$SCRIPT_DIR/services/payment-service.yaml"
    
    log_info "Deploying Treasure Service..."
    kubectl apply -f "$SCRIPT_DIR/services/treasure-service.yaml"
    
    log_info "Deploying API Gateway..."
    kubectl apply -f "$SCRIPT_DIR/services/api-gateway.yaml"
    
    log_info "Deploying Frontend..."
    kubectl apply -f "$SCRIPT_DIR/services/frontend.yaml"
    
    log_success "Application services deployed"
}

# Wait for applications to be ready
wait_for_applications() {
    log_info "Waiting for application services to be ready..."
    
    # Wait for each service in dependency order
    log_info "Waiting for Auth Service..."
    kubectl wait --for=condition=ready pod -l app=auth-service -n $NAMESPACE_APP --timeout=300s
    
    log_info "Waiting for User Service..."
    kubectl wait --for=condition=ready pod -l app=user-service -n $NAMESPACE_APP --timeout=300s
    
    log_info "Waiting for Payment Service..."
    kubectl wait --for=condition=ready pod -l app=payment-service -n $NAMESPACE_APP --timeout=300s
    
    log_info "Waiting for Treasure Service..."
    kubectl wait --for=condition=ready pod -l app=treasure-service -n $NAMESPACE_APP --timeout=300s
    
    log_info "Waiting for API Gateway..."
    kubectl wait --for=condition=ready pod -l app=api-gateway -n $NAMESPACE_APP --timeout=300s
    
    log_info "Waiting for Frontend..."
    kubectl wait --for=condition=ready pod -l app=frontend -n $NAMESPACE_APP --timeout=300s
    
    log_success "Application services are ready"
}

# Display deployment status
show_status() {
    log_info "Deployment Status:"
    echo
    
    log_info "Infrastructure Services:"
    kubectl get pods -n $NAMESPACE_INFRA -o wide
    echo
    
    log_info "Application Services:"
    kubectl get pods -n $NAMESPACE_APP -o wide
    echo
    
    log_info "Services:"
    kubectl get svc -n $NAMESPACE_INFRA
    kubectl get svc -n $NAMESPACE_APP
    echo
    
    # Get frontend URL
    FRONTEND_URL=$(kubectl get svc frontend -n $NAMESPACE_APP -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "localhost")
    if [ "$FRONTEND_URL" = "localhost" ]; then
        log_info "Frontend will be available at: http://localhost:3000 (use kubectl port-forward)"
        log_info "To access frontend: kubectl port-forward svc/frontend -n $NAMESPACE_APP 3000:3000"
    else
        log_info "Frontend is available at: http://$FRONTEND_URL:3000"
    fi
    
    log_info "API Gateway: http://localhost:8080 (use kubectl port-forward)"
    log_info "To access API Gateway: kubectl port-forward svc/api-gateway -n $NAMESPACE_APP 8080:8080"
}

# Main deployment function
main() {
    log_info "Starting MySillyDreams Treasure Hunt Kubernetes Deployment..."
    echo
    
    check_prerequisites
    
    if [ "$1" = "--clean" ]; then
        cleanup_existing
    fi
    
    build_images
    deploy_infrastructure
    wait_for_infrastructure
    deploy_applications
    wait_for_applications
    
    echo
    log_success "🎉 Deployment completed successfully!"
    echo
    show_status
}

# Handle script arguments
case "$1" in
    --help|-h)
        echo "MySillyDreams Treasure Hunt Kubernetes Deployment Script"
        echo
        echo "Usage: $0 [OPTIONS]"
        echo
        echo "Options:"
        echo "  --clean    Clean up existing resources before deployment"
        echo "  --help     Show this help message"
        echo
        exit 0
        ;;
    *)
        main "$@"
        ;;
esac
