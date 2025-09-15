#!/bin/bash

# Treasure Hunt Frontend Deployment Script
# Usage: ./scripts/deploy.sh [environment] [options]
# Environments: dev, staging, production
# Options: --build-only, --no-cache, --rollback

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="dev"
BUILD_ONLY=false
NO_CACHE=false
ROLLBACK=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    dev|staging|production)
      ENVIRONMENT="$1"
      shift
      ;;
    --build-only)
      BUILD_ONLY=true
      shift
      ;;
    --no-cache)
      NO_CACHE=true
      shift
      ;;
    --rollback)
      ROLLBACK=true
      shift
      ;;
    -h|--help)
      echo "Usage: $0 [environment] [options]"
      echo "Environments: dev, staging, production"
      echo "Options:"
      echo "  --build-only    Only build, don't deploy"
      echo "  --no-cache      Build without cache"
      echo "  --rollback      Rollback to previous version"
      echo "  -h, --help      Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option $1"
      exit 1
      ;;
  esac
done

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

# Environment configurations
setup_environment() {
  case $ENVIRONMENT in
    dev)
      DOCKER_REGISTRY="treasurehunt.azurecr.io"
      KUBERNETES_NAMESPACE="treasure-hunt-dev"
      DOMAIN="dev.treasurehunt.mysillydreams.com"
      REPLICAS=1
      RESOURCES_REQUESTS_CPU="100m"
      RESOURCES_REQUESTS_MEMORY="128Mi"
      RESOURCES_LIMITS_CPU="500m"
      RESOURCES_LIMITS_MEMORY="512Mi"
      ;;
    staging)
      DOCKER_REGISTRY="treasurehunt.azurecr.io"
      KUBERNETES_NAMESPACE="treasure-hunt-staging"
      DOMAIN="staging.treasurehunt.mysillydreams.com"
      REPLICAS=2
      RESOURCES_REQUESTS_CPU="250m"
      RESOURCES_REQUESTS_MEMORY="256Mi"
      RESOURCES_LIMITS_CPU="1000m"
      RESOURCES_LIMITS_MEMORY="1Gi"
      ;;
    production)
      DOCKER_REGISTRY="treasurehunt.azurecr.io"
      KUBERNETES_NAMESPACE="treasure-hunt-prod"
      DOMAIN="treasurehunt.mysillydreams.com"
      REPLICAS=3
      RESOURCES_REQUESTS_CPU="500m"
      RESOURCES_REQUESTS_MEMORY="512Mi"
      RESOURCES_LIMITS_CPU="2000m"
      RESOURCES_LIMITS_MEMORY="2Gi"
      ;;
    *)
      log_error "Unknown environment: $ENVIRONMENT"
      exit 1
      ;;
  esac

  IMAGE_TAG="${ENVIRONMENT}-$(date +%Y%m%d-%H%M%S)-$(git rev-parse --short HEAD)"
  IMAGE_NAME="${DOCKER_REGISTRY}/frontend:${IMAGE_TAG}"
  
  log_info "Environment: $ENVIRONMENT"
  log_info "Image: $IMAGE_NAME"
  log_info "Namespace: $KUBERNETES_NAMESPACE"
  log_info "Domain: $DOMAIN"
}

# Pre-deployment checks
pre_deployment_checks() {
  log_info "Running pre-deployment checks..."

  # Check if required tools are installed
  command -v docker >/dev/null 2>&1 || { log_error "Docker is required but not installed."; exit 1; }
  command -v kubectl >/dev/null 2>&1 || { log_error "kubectl is required but not installed."; exit 1; }
  command -v node >/dev/null 2>&1 || { log_error "Node.js is required but not installed."; exit 1; }
  command -v npm >/dev/null 2>&1 || { log_error "npm is required but not installed."; exit 1; }

  # Check if we're in the right directory
  if [[ ! -f "$PROJECT_ROOT/package.json" ]]; then
    log_error "package.json not found. Are you in the right directory?"
    exit 1
  fi

  # Check if git working directory is clean (for production)
  if [[ $ENVIRONMENT == "production" ]]; then
    if [[ -n $(git status --porcelain) ]]; then
      log_error "Git working directory is not clean. Commit your changes before deploying to production."
      exit 1
    fi
  fi

  # Check if environment file exists
  ENV_FILE="$PROJECT_ROOT/.env.$ENVIRONMENT"
  if [[ ! -f "$ENV_FILE" ]]; then
    log_warning "Environment file $ENV_FILE not found. Using .env.example as template."
    cp "$PROJECT_ROOT/.env.example" "$ENV_FILE"
  fi

  log_success "Pre-deployment checks passed"
}

# Build application
build_application() {
  log_info "Building application for $ENVIRONMENT..."

  cd "$PROJECT_ROOT"

  # Install dependencies
  log_info "Installing dependencies..."
  npm ci --production=false

  # Run tests
  log_info "Running tests..."
  npm run test:ci || {
    log_error "Tests failed. Deployment aborted."
    exit 1
  }

  # Run linting
  log_info "Running linter..."
  npm run lint || {
    log_error "Linting failed. Deployment aborted."
    exit 1
  }

  # Build application
  log_info "Building Next.js application..."
  if [[ $NO_CACHE == true ]]; then
    rm -rf .next
  fi
  
  NODE_ENV=production npm run build || {
    log_error "Build failed. Deployment aborted."
    exit 1
  }

  log_success "Application built successfully"
}

# Build Docker image
build_docker_image() {
  log_info "Building Docker image..."

  cd "$PROJECT_ROOT"

  # Build arguments
  BUILD_ARGS=""
  if [[ $NO_CACHE == true ]]; then
    BUILD_ARGS="--no-cache"
  fi

  # Build image
  docker build $BUILD_ARGS -t "$IMAGE_NAME" . || {
    log_error "Docker build failed"
    exit 1
  }

  # Tag as latest for environment
  docker tag "$IMAGE_NAME" "${DOCKER_REGISTRY}/frontend:${ENVIRONMENT}-latest"

  log_success "Docker image built: $IMAGE_NAME"
}

# Push Docker image
push_docker_image() {
  log_info "Pushing Docker image to registry..."

  # Login to Azure Container Registry
  az acr login --name treasurehunt || {
    log_error "Failed to login to Azure Container Registry"
    exit 1
  }

  # Push image
  docker push "$IMAGE_NAME" || {
    log_error "Failed to push Docker image"
    exit 1
  }

  # Push latest tag
  docker push "${DOCKER_REGISTRY}/frontend:${ENVIRONMENT}-latest" || {
    log_error "Failed to push latest tag"
    exit 1
  }

  log_success "Docker image pushed successfully"
}

# Deploy to Kubernetes
deploy_to_kubernetes() {
  log_info "Deploying to Kubernetes..."

  # Create namespace if it doesn't exist
  kubectl create namespace "$KUBERNETES_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

  # Generate Kubernetes manifests
  envsubst < "$PROJECT_ROOT/k8s/deployment.template.yaml" > "/tmp/deployment-${ENVIRONMENT}.yaml"

  # Apply manifests
  kubectl apply -f "/tmp/deployment-${ENVIRONMENT}.yaml" -n "$KUBERNETES_NAMESPACE" || {
    log_error "Failed to apply Kubernetes manifests"
    exit 1
  }

  # Wait for rollout to complete
  kubectl rollout status deployment/frontend -n "$KUBERNETES_NAMESPACE" --timeout=600s || {
    log_error "Deployment rollout failed"
    exit 1
  }

  log_success "Deployed to Kubernetes successfully"
}

# Health check
health_check() {
  log_info "Performing health check..."

  # Wait for pods to be ready
  kubectl wait --for=condition=ready pod -l app=frontend -n "$KUBERNETES_NAMESPACE" --timeout=300s || {
    log_error "Pods failed to become ready"
    exit 1
  }

  # Check if application is responding
  for i in {1..30}; do
    if curl -f "https://$DOMAIN/api/health" >/dev/null 2>&1; then
      log_success "Health check passed"
      return 0
    fi
    log_info "Waiting for application to respond... ($i/30)"
    sleep 10
  done

  log_error "Health check failed"
  exit 1
}

# Rollback deployment
rollback_deployment() {
  log_info "Rolling back deployment..."

  kubectl rollout undo deployment/frontend -n "$KUBERNETES_NAMESPACE" || {
    log_error "Rollback failed"
    exit 1
  }

  kubectl rollout status deployment/frontend -n "$KUBERNETES_NAMESPACE" --timeout=300s || {
    log_error "Rollback rollout failed"
    exit 1
  }

  log_success "Rollback completed successfully"
}

# Cleanup
cleanup() {
  log_info "Cleaning up..."

  # Remove temporary files
  rm -f "/tmp/deployment-${ENVIRONMENT}.yaml"

  # Remove old Docker images (keep last 5)
  docker images "${DOCKER_REGISTRY}/frontend" --format "table {{.Tag}}" | grep "^${ENVIRONMENT}-" | tail -n +6 | xargs -r docker rmi "${DOCKER_REGISTRY}/frontend:" 2>/dev/null || true

  log_success "Cleanup completed"
}

# Main deployment function
main() {
  log_info "Starting deployment to $ENVIRONMENT environment..."

  setup_environment

  if [[ $ROLLBACK == true ]]; then
    rollback_deployment
    exit 0
  fi

  pre_deployment_checks
  build_application
  
  if [[ $BUILD_ONLY == true ]]; then
    log_success "Build completed. Skipping deployment."
    exit 0
  fi

  build_docker_image
  push_docker_image
  deploy_to_kubernetes
  health_check
  cleanup

  log_success "Deployment to $ENVIRONMENT completed successfully!"
  log_info "Application is available at: https://$DOMAIN"
}

# Trap errors and cleanup
trap cleanup EXIT

# Run main function
main "$@"
