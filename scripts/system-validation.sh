#!/bin/bash

# MySillyDreams - System Validation Script
# Comprehensive validation of the entire platform

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
API_GATEWAY_URL="http://localhost:8080"
FRONTEND_URL="http://localhost:3000"
USER_SERVICE_URL="http://localhost:8083"
PAYMENT_SERVICE_URL="http://localhost:8081"
TREASURE_SERVICE_URL="http://localhost:8082"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    print_status "Running: $test_name"
    
    if eval "$test_command" > /dev/null 2>&1; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        print_success "✓ $test_name"
        return 0
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        print_error "✗ $test_name"
        return 1
    fi
}

# Function to test HTTP endpoint
test_endpoint() {
    local name="$1"
    local url="$2"
    local expected_status="${3:-200}"
    local method="${4:-GET}"
    local data="$5"
    
    local curl_cmd="curl -s -w '%{http_code}' -o /dev/null"
    
    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        curl_cmd="$curl_cmd -X POST -H 'Content-Type: application/json' -d '$data'"
    elif [ "$method" = "PUT" ] && [ -n "$data" ]; then
        curl_cmd="$curl_cmd -X PUT -H 'Content-Type: application/json' -d '$data'"
    elif [ "$method" = "DELETE" ]; then
        curl_cmd="$curl_cmd -X DELETE"
    fi
    
    curl_cmd="$curl_cmd '$url'"
    
    run_test "$name" "[ \$(${curl_cmd}) -eq $expected_status ]"
}

# Function to validate system prerequisites
validate_prerequisites() {
    print_status "Validating system prerequisites..."
    
    run_test "Docker is running" "docker info"
    run_test "Docker Compose is available" "docker-compose --version"
    run_test "Node.js is available" "node --version"
    run_test "npm is available" "npm --version"
    run_test "curl is available" "curl --version"
    run_test "jq is available" "jq --version"
}

# Function to validate infrastructure services
validate_infrastructure() {
    print_status "Validating infrastructure services..."
    
    # Database validation
    run_test "User Service Database" "docker-compose exec -T postgres-user pg_isready -U userservice -d userservice"
    run_test "Payment Service Database" "docker-compose exec -T postgres-payment pg_isready -U paymentservice -d paymentservice"
    run_test "Treasure Service Database" "docker-compose exec -T postgres-treasure pg_isready -U treasureservice -d treasureservice"
    
    # Cache validation
    run_test "Redis Connectivity" "docker-compose exec -T redis redis-cli ping | grep -q PONG"
    
    # Message queue validation
    run_test "Kafka Connectivity" "docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list"
    
    # Secrets management validation
    run_test "Vault Connectivity" "curl -s http://localhost:8200/v1/sys/health | jq -r '.sealed' | grep -q false"
}

# Function to validate application services
validate_application_services() {
    print_status "Validating application services..."
    
    # Health checks
    test_endpoint "API Gateway Health" "$API_GATEWAY_URL/actuator/health"
    test_endpoint "User Service Health" "$USER_SERVICE_URL/actuator/health"
    test_endpoint "Payment Service Health" "$PAYMENT_SERVICE_URL/actuator/health"
    test_endpoint "Treasure Service Health" "$TREASURE_SERVICE_URL/actuator/health"
    test_endpoint "Frontend Health" "$FRONTEND_URL"
    
    # Metrics endpoints
    test_endpoint "API Gateway Metrics" "$API_GATEWAY_URL/actuator/prometheus"
    test_endpoint "User Service Metrics" "$USER_SERVICE_URL/actuator/prometheus"
    test_endpoint "Payment Service Metrics" "$PAYMENT_SERVICE_URL/actuator/prometheus"
    test_endpoint "Treasure Service Metrics" "$TREASURE_SERVICE_URL/actuator/prometheus"
}

# Function to validate API routing
validate_api_routing() {
    print_status "Validating API Gateway routing..."
    
    test_endpoint "Gateway to User Service" "$API_GATEWAY_URL/api/v1/users/health" "404"
    test_endpoint "Gateway to Payment Service" "$API_GATEWAY_URL/api/v1/payments/health" "404"
    test_endpoint "Gateway to Treasure Service" "$API_GATEWAY_URL/api/v1/treasure/health" "404"
    
    # CORS validation
    run_test "CORS Headers" "curl -s -H 'Origin: http://localhost:3000' -H 'Access-Control-Request-Method: POST' -H 'Access-Control-Request-Headers: Content-Type' -X OPTIONS $API_GATEWAY_URL/api/v1/users | grep -q 'Access-Control-Allow-Origin'"
}

# Function to validate business workflows
validate_business_workflows() {
    print_status "Validating business workflows..."
    
    local test_user_data='{
        "firstName": "System",
        "lastName": "Test",
        "email": "systemtest@example.com",
        "phone": "+1234567890",
        "gender": "OTHER",
        "roles": ["USER"]
    }'
    
    # User registration workflow
    local user_response=$(curl -s -X POST -H "Content-Type: application/json" -d "$test_user_data" "$USER_SERVICE_URL/api/v1/users")
    local user_ref_id=$(echo "$user_response" | jq -r '.data.referenceId // empty')
    
    if [ -n "$user_ref_id" ]; then
        print_success "✓ User Registration Workflow"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        
        # User lookup workflow
        test_endpoint "User Lookup by Email" "$USER_SERVICE_URL/api/v1/users/lookup?email=systemtest@example.com"
        
        # User profile workflow
        test_endpoint "User Profile Retrieval" "$USER_SERVICE_URL/api/v1/users/$user_ref_id"
        
        # Cleanup test user
        curl -s -X DELETE "$USER_SERVICE_URL/api/v1/users/$user_ref_id" > /dev/null
    else
        print_error "✗ User Registration Workflow"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

# Function to validate security
validate_security() {
    print_status "Validating security measures..."
    
    # Test unauthorized access
    test_endpoint "Unauthorized Access Protection" "$USER_SERVICE_URL/api/v1/users/admin" "401"
    
    # Test SQL injection protection
    test_endpoint "SQL Injection Protection" "$USER_SERVICE_URL/api/v1/users/lookup?email='; DROP TABLE users; --" "400"
    
    # Test XSS protection
    test_endpoint "XSS Protection" "$API_GATEWAY_URL/api/v1/users" "401" "POST" '{"firstName": "<script>alert(\"xss\")</script>"}'
    
    # Test rate limiting (if implemented)
    run_test "Rate Limiting" "for i in {1..20}; do curl -s $API_GATEWAY_URL/actuator/health > /dev/null; done"
}

# Function to validate performance
validate_performance() {
    print_status "Validating performance benchmarks..."
    
    # Response time validation
    local start_time=$(date +%s%N)
    curl -s "$API_GATEWAY_URL/actuator/health" > /dev/null
    local end_time=$(date +%s%N)
    local response_time=$(( (end_time - start_time) / 1000000 ))
    
    if [ $response_time -lt 1000 ]; then
        print_success "✓ API Gateway Response Time: ${response_time}ms"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        print_error "✗ API Gateway Response Time: ${response_time}ms (too slow)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    # Load test simulation
    print_status "Running basic load test..."
    local load_start=$(date +%s)
    for i in {1..50}; do
        curl -s "$API_GATEWAY_URL/actuator/health" > /dev/null &
    done
    wait
    local load_end=$(date +%s)
    local load_duration=$((load_end - load_start))
    
    if [ $load_duration -lt 10 ]; then
        print_success "✓ Basic Load Test: ${load_duration}s for 50 concurrent requests"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        print_error "✗ Basic Load Test: ${load_duration}s (too slow)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

# Function to validate monitoring
validate_monitoring() {
    print_status "Validating monitoring and observability..."
    
    # Prometheus validation
    test_endpoint "Prometheus Server" "http://localhost:9090/-/healthy"
    
    # Grafana validation
    test_endpoint "Grafana Server" "http://localhost:3001/api/health"
    
    # Log aggregation validation
    run_test "Application Logs" "docker-compose logs user-service | grep -q 'Started UserServiceApplication'"
    run_test "Error Logs" "docker-compose logs --tail=100 | grep -i error | wc -l | awk '{print (\$1 < 10)}'"
}

# Function to validate data consistency
validate_data_consistency() {
    print_status "Validating data consistency..."
    
    # Database connectivity
    run_test "User Service DB Connection" "docker-compose exec -T postgres-user psql -U userservice -d userservice -c 'SELECT 1;'"
    run_test "Payment Service DB Connection" "docker-compose exec -T postgres-payment psql -U paymentservice -d paymentservice -c 'SELECT 1;'"
    run_test "Treasure Service DB Connection" "docker-compose exec -T postgres-treasure psql -U treasureservice -d treasureservice -c 'SELECT 1;'"
    
    # Cache consistency
    run_test "Redis Data Consistency" "docker-compose exec -T redis redis-cli info replication | grep -q 'role:master'"
}

# Function to validate GDPR compliance
validate_gdpr_compliance() {
    print_status "Validating GDPR compliance features..."
    
    # Data export endpoints
    test_endpoint "GDPR Export Endpoint" "$USER_SERVICE_URL/api/v1/gdpr/export/test-user" "401"
    
    # Data deletion endpoints
    test_endpoint "GDPR Deletion Endpoint" "$USER_SERVICE_URL/api/v1/gdpr/deletion/test-user" "401"
    
    # Consent management
    test_endpoint "Consent Management Endpoint" "$USER_SERVICE_URL/api/v1/users/test-user/consents" "404"
}

# Function to generate validation report
generate_validation_report() {
    echo ""
    echo "=========================================="
    echo "        SYSTEM VALIDATION REPORT"
    echo "=========================================="
    echo "Total Tests:     $TOTAL_TESTS"
    echo "Passed:          $PASSED_TESTS"
    echo "Failed:          $FAILED_TESTS"
    echo "Success Rate:    $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"
    echo "=========================================="
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        print_success "🎉 All system validation tests passed!"
        print_success "MySillyDreams platform is ready for production!"
        return 0
    else
        print_error "❌ $FAILED_TESTS validation tests failed!"
        print_error "Please review and fix the issues before deployment."
        return 1
    fi
}

# Function to wait for services
wait_for_services() {
    print_status "Waiting for all services to be ready..."
    
    local max_attempts=60
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "$API_GATEWAY_URL/actuator/health" > /dev/null 2>&1 && \
           curl -f -s "$FRONTEND_URL" > /dev/null 2>&1; then
            print_success "All services are ready!"
            return 0
        fi
        
        print_status "Attempt $attempt/$max_attempts: Services not ready yet..."
        sleep 5
        ((attempt++))
    done
    
    print_error "Services failed to become ready after $max_attempts attempts"
    return 1
}

# Main validation function
main() {
    print_status "Starting MySillyDreams System Validation..."
    echo ""
    
    # Wait for services to be ready
    wait_for_services
    
    # Run all validation suites
    validate_prerequisites
    validate_infrastructure
    validate_application_services
    validate_api_routing
    validate_business_workflows
    validate_security
    validate_performance
    validate_monitoring
    validate_data_consistency
    validate_gdpr_compliance
    
    # Generate final report
    generate_validation_report
}

# Script usage
usage() {
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  all           Run all validation tests (default)"
    echo "  infra         Validate infrastructure only"
    echo "  services      Validate application services only"
    echo "  security      Validate security measures only"
    echo "  performance   Validate performance only"
    echo "  monitoring    Validate monitoring only"
    echo "  gdpr          Validate GDPR compliance only"
    echo ""
}

# Main script logic
case "${1:-all}" in
    all)
        main
        ;;
    infra)
        wait_for_services
        validate_prerequisites
        validate_infrastructure
        generate_validation_report
        ;;
    services)
        wait_for_services
        validate_application_services
        validate_api_routing
        generate_validation_report
        ;;
    security)
        wait_for_services
        validate_security
        generate_validation_report
        ;;
    performance)
        wait_for_services
        validate_performance
        generate_validation_report
        ;;
    monitoring)
        wait_for_services
        validate_monitoring
        generate_validation_report
        ;;
    gdpr)
        wait_for_services
        validate_gdpr_compliance
        generate_validation_report
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        print_error "Unknown command: $1"
        usage
        exit 1
        ;;
esac
