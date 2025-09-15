# MySillyDreams Treasure Hunt - Kubernetes Deployment

This directory contains all the Kubernetes manifests and deployment scripts for the MySillyDreams Treasure Hunt application.

## 🏗️ Architecture Overview

The application consists of:

### Infrastructure Services (msd-infrastructure namespace)
- **PostgreSQL Databases**: Separate databases for User, Payment, Treasure, and Keycloak services
- **Redis**: Caching and session storage
- **Keycloak**: Authentication and authorization server
- **Vault**: Secrets management

### Application Services (msd-treasure-hunt namespace)
- **Auth Service** (Port 8081): Authentication and JWT token management
- **User Service** (Port 8082): User profile and management
- **Payment Service** (Port 8083): Payment processing and transactions
- **Treasure Service** (Port 8084): Treasure hunt adventures and locations
- **API Gateway** (Port 8080): Central API gateway and routing
- **Frontend** (Port 3000): Next.js React application

## 🚀 Prerequisites

### Required Tools
1. **Docker Desktop** with Kubernetes enabled
   - Enable Kubernetes in Docker Desktop settings
   - Ensure Docker Desktop is running

2. **kubectl** - Kubernetes command-line tool
   ```bash
   # Windows (using Chocolatey)
   choco install kubernetes-cli
   
   # macOS (using Homebrew)
   brew install kubectl
   
   # Linux
   curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
   ```

3. **Java 17+** - For building Spring Boot services
4. **Node.js 18+** - For building the frontend

### Alternative: Minikube
If you prefer using Minikube instead of Docker Desktop:

```bash
# Install Minikube
# Windows
choco install minikube

# macOS
brew install minikube

# Start Minikube
minikube start --memory=8192 --cpus=4
```

## 📦 Quick Deployment

### Option 1: Automated Deployment (Recommended)

**Linux/macOS:**
```bash
cd k8s
chmod +x deploy.sh
./deploy.sh --clean
```

**Windows:**
```cmd
cd k8s
deploy.bat --clean
```

### Option 2: Manual Step-by-Step Deployment

1. **Clean existing resources (optional):**
   ```bash
   kubectl delete namespace msd-treasure-hunt --ignore-not-found=true
   kubectl delete namespace msd-infrastructure --ignore-not-found=true
   ```

2. **Build Docker images:**
   ```bash
   # From project root
   cd auth-service && ./gradlew clean build -x test && docker build -t auth-service:latest . && cd ..
   cd user-service && ./gradlew clean build -x test && docker build -t user-service:latest . && cd ..
   cd payment-service && ./gradlew clean build -x test && docker build -t payment-service:latest . && cd ..
   cd Treasure && ./gradlew clean build -x test && docker build -t treasure-service:latest . && cd ..
   cd api-gateway && ./gradlew clean build -x test && docker build -t api-gateway:latest . && cd ..
   cd frontend && docker build -t frontend:latest . && cd ..
   ```

3. **Deploy infrastructure:**
   ```bash
   kubectl apply -f k8s/namespace.yaml
   kubectl apply -f k8s/infrastructure/
   ```

4. **Wait for infrastructure to be ready:**
   ```bash
   kubectl wait --for=condition=ready pod -l app=postgres-user -n msd-infrastructure --timeout=300s
   kubectl wait --for=condition=ready pod -l app=keycloak -n msd-infrastructure --timeout=600s
   ```

5. **Deploy applications:**
   ```bash
   kubectl apply -f k8s/services/
   ```

6. **Wait for applications to be ready:**
   ```bash
   kubectl wait --for=condition=ready pod -l app=frontend -n msd-treasure-hunt --timeout=300s
   ```

## 🔍 Monitoring and Troubleshooting

### Check Deployment Status
```bash
# Check all pods
kubectl get pods -n msd-infrastructure
kubectl get pods -n msd-treasure-hunt

# Check services
kubectl get svc -n msd-infrastructure
kubectl get svc -n msd-treasure-hunt

# Check logs
kubectl logs -f deployment/frontend -n msd-treasure-hunt
kubectl logs -f deployment/api-gateway -n msd-treasure-hunt
```

### Access Applications

**Frontend:**
```bash
# Port forward to access locally
kubectl port-forward svc/frontend -n msd-treasure-hunt 3000:3000
# Then visit: http://localhost:3000
```

**API Gateway:**
```bash
kubectl port-forward svc/api-gateway -n msd-treasure-hunt 8080:8080
# Then visit: http://localhost:8080
```

**Keycloak Admin Console:**
```bash
kubectl port-forward svc/keycloak -n msd-infrastructure 8080:8080
# Then visit: http://localhost:8080
# Username: admin, Password: admin123
```

### Common Issues and Solutions

1. **Pods stuck in Pending state:**
   ```bash
   kubectl describe pod <pod-name> -n <namespace>
   # Check for resource constraints or node issues
   ```

2. **ImagePullBackOff errors:**
   - Ensure Docker images are built locally
   - Check image names match the deployment manifests

3. **Database connection issues:**
   ```bash
   # Check PostgreSQL pods
   kubectl logs -f deployment/postgres-user -n msd-infrastructure
   ```

4. **Service discovery issues:**
   ```bash
   # Test DNS resolution
   kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup keycloak.msd-infrastructure.svc.cluster.local
   ```

## 🧹 Cleanup

To completely remove the deployment:

```bash
kubectl delete namespace msd-treasure-hunt
kubectl delete namespace msd-infrastructure

# Remove Docker images (optional)
docker rmi auth-service:latest user-service:latest payment-service:latest treasure-service:latest api-gateway:latest frontend:latest
```

## 🔧 Configuration

### Environment Variables
All services are configured through ConfigMaps and Secrets. Key configurations:

- **Database URLs**: Point to the appropriate PostgreSQL services
- **Service URLs**: Use Kubernetes DNS for inter-service communication
- **Secrets**: JWT keys, database passwords, API keys

### Scaling
To scale services:
```bash
kubectl scale deployment frontend --replicas=3 -n msd-treasure-hunt
kubectl scale deployment api-gateway --replicas=3 -n msd-treasure-hunt
```

### Resource Limits
Each service has defined resource requests and limits:
- **Requests**: Minimum guaranteed resources
- **Limits**: Maximum allowed resources

## 📊 Monitoring

The deployment includes:
- **Health checks**: Liveness and readiness probes for all services
- **Resource monitoring**: CPU and memory limits
- **Service discovery**: Kubernetes DNS-based service discovery

## 🔐 Security

- **Secrets management**: Sensitive data stored in Kubernetes Secrets
- **Network policies**: Services isolated by namespace
- **RBAC**: Role-based access control (can be extended)
- **TLS**: Can be configured with cert-manager for production

## 🚀 Production Considerations

For production deployment:
1. Use external databases instead of in-cluster PostgreSQL
2. Configure persistent storage classes
3. Set up monitoring with Prometheus/Grafana
4. Configure ingress controllers for external access
5. Implement backup strategies
6. Set up CI/CD pipelines for automated deployments
