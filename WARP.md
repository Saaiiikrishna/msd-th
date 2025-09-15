# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Platform Overview

MySillyDreams is an enterprise-grade treasure hunt platform built as a microservices architecture with comprehensive security, observability, and GDPR compliance. The system processes payments, manages user data with field-level encryption, and provides a gamified treasure/rewards system.

## Architecture

### Core Services
- **API Gateway** (Port 8080) - Spring Cloud Gateway for routing, rate limiting, and CORS
- **User Service** (Port 8082/8083) - User management with PII encryption, GDPR compliance, and audit trails
- **Payment Service** (Port 8083/8081) - Razorpay integration for payment processing and transaction management
- **Treasure Service** (Port 8084/8082) - Rewards/loyalty system with balance management and leaderboards
- **Auth Service** (Port 8081) - Authentication service with Keycloak integration

### Infrastructure Dependencies
- **PostgreSQL** (4 separate databases) - Primary data storage per service
- **Redis** - Multi-level caching and session management
- **Kafka** - Event-driven architecture with transactional outbox pattern
- **HashiCorp Vault** - Field-level PII encryption and secrets management
- **Keycloak** - OAuth2/JWT authentication and user management
- **Prometheus/Grafana** - Metrics collection and visualization

### Frontend
- **Next.js 14 App** (Port 3000) - React with TypeScript, Tailwind CSS, and React Query

## Development Commands

### Local Development Setup
```bash
# Full stack deployment (recommended)
docker-compose up -d

# Build all services
./gradlew clean build    # For individual Java services
npm install && npm run build    # For frontend
```

### Service Management
```bash
# Start infrastructure only
docker-compose up -d postgres-user postgres-payment postgres-treasure postgres-keycloak redis kafka zookeeper vault

# Start individual services (after infrastructure)
docker-compose up -d user-service payment-service treasure-service api-gateway frontend

# View logs
docker-compose logs -f [service-name]

# Restart services
docker-compose restart [service-name]
```

### Testing
```bash
# Health checks
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8082/actuator/health  # User Service
curl http://localhost:8083/actuator/health  # Payment Service
curl http://localhost:8084/actuator/health  # Treasure Service

# Run integration tests
./scripts/integration-tests.sh
./scripts/integration-tests.sh health      # Health checks only
./scripts/integration-tests.sh api         # API tests only

# Frontend tests
cd frontend && npm test
cd frontend && npm run test:e2e
```

### Database Operations
```bash
# Connect to databases
docker-compose exec postgres-user psql -U userservice -d userservice
docker-compose exec postgres-payment psql -U paymentservice -d paymentservice
docker-compose exec postgres-treasure psql -U treasureservice -d treasureservice

# Run migrations
cd user-service && ./gradlew flywayMigrate
cd payment-service && ./gradlew flywayMigrate
cd Treasure && ./gradlew flywayMigrate
```

### Build Individual Services
```bash
# User Service
cd user-service && ./gradlew clean build && docker build -t user-service .

# Payment Service
cd payment-service && ./gradlew clean build && docker build -t payment-service .

# Treasure Service
cd Treasure && ./gradlew clean build && docker build -t treasure-service .

# API Gateway
cd api-gateway && ./gradlew clean build && docker build -t api-gateway .

# Frontend
cd frontend && npm run build && docker build -t frontend .
```

### Production Deployment
```bash
# Kubernetes deployment
./k8s/deploy.sh --clean    # Full deployment with cleanup

# Individual service deployment
kubectl apply -f k8s/services/[service-name].yaml
```

## Key Architectural Patterns

### Event-Driven Architecture
- **Transactional Outbox Pattern**: All services publish events reliably using database outbox tables
- **Kafka Topics**: `user-events`, `payment-events`, `treasure-events`, `audit-events`
- **Event Versioning**: Events are versioned with backward compatibility

### Security & Compliance
- **Field-Level Encryption**: PII data encrypted using Vault Transit engine with searchable HMAC
- **JWT Authentication**: OAuth2 resource server pattern with Keycloak
- **GDPR Compliance**: Right to be forgotten, data export, consent management, audit trails
- **Role-Based Access Control**: Hierarchical roles with method-level security

### Caching Strategy
- **L1 Cache**: Application-level caching with TTL strategies
- **L2 Cache**: Redis with intelligent cache invalidation
- **Cache Patterns**: User profiles (10min), lookup data (5min), role hierarchy (1hour)

### Data Architecture
- **Database Per Service**: Each microservice owns its data
- **Shared Nothing**: No direct database access between services
- **Event Sourcing**: Audit events stored immutably
- **CQRS Elements**: Separate read/write models for complex queries

## Service Communication Patterns

### Synchronous Communication
- **Internal APIs**: Service-to-service REST calls with circuit breakers
- **API Gateway**: Single entry point with routing, rate limiting, authentication
- **Service Discovery**: Static configuration in containerized environments

### Asynchronous Communication
- **Event Publishing**: Domain events published to Kafka topics
- **Event Handling**: Event listeners with retry and dead letter queues
- **Saga Pattern**: Distributed transactions using choreography-based sagas

## Configuration Management

### Environment Variables
Each service uses environment-specific configuration:
- `SPRING_PROFILES_ACTIVE`: dev, staging, prod, docker
- Database connections: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`
- External services: `REDIS_HOST`, `KAFKA_BOOTSTRAP_SERVERS`, `VAULT_URI`

### Service-Specific Contexts
- **User Service**: `/api/user-service/v1` - Handles PII encryption, GDPR compliance
- **Payment Service**: Direct port access - Integrates with Razorpay, handles webhooks
- **Treasure Service**: Direct port access - Manages rewards, leaderboards, transfers
- **API Gateway**: Routes all external traffic, handles CORS, rate limiting

## Development Workflow

### Adding New Features
1. **Schema Changes**: Use Flyway migrations in `src/main/resources/db/migration`
2. **Event Design**: Design events first, considering backward compatibility
3. **API Design**: Follow OpenAPI/Swagger documentation standards
4. **Security Review**: Consider PII handling, authorization, audit requirements
5. **Caching Strategy**: Define cache keys, TTL, invalidation patterns

### Testing Strategy
- **Unit Tests**: 90%+ coverage required, focus on business logic
- **Integration Tests**: Database and external service mocking
- **Contract Tests**: API contract validation between services
- **End-to-End Tests**: Full user journey testing with Playwright

### Monitoring & Observability
- **Metrics**: Custom business metrics via Micrometer/Prometheus
- **Logging**: Structured JSON logging with correlation IDs
- **Tracing**: Distributed tracing with correlation ID propagation
- **Health Checks**: Comprehensive health endpoints for all dependencies

## Troubleshooting Common Issues

### Service Startup Issues
- Check database connectivity and migrations
- Verify Vault accessibility for encryption services
- Ensure Kafka topics are created and accessible
- Validate Redis connectivity for caching

### Authentication Issues
- Verify Keycloak realm configuration and client setup
- Check JWT token validation and issuer URI configuration
- Validate CORS settings in API Gateway
- Confirm user role assignments and permissions

### Performance Issues
- Check database query performance and indexing
- Monitor cache hit rates and TTL settings
- Validate connection pool configurations
- Review Kafka consumer lag and processing times

### Data Consistency Issues
- Verify event publishing and consumption
- Check outbox table processing for failed events
- Monitor distributed transaction patterns
- Validate eventual consistency requirements

## Business Domain Context

This platform implements a treasure hunt/rewards system where:
- Users register and manage profiles with PII protection
- Payments are processed securely through Razorpay integration
- Treasure points are earned, spent, and transferred between users
- All actions are audited for compliance and business intelligence
- GDPR rights (data export, deletion, consent) are fully automated

The system is designed for production scale with enterprise security, observability, and compliance requirements.
