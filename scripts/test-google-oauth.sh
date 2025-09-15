#!/bin/bash

# Google OAuth Integration Test Script
# This script tests the complete Google OAuth flow

echo "🧪 Testing Google OAuth Integration"
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
AUTH_SERVICE_URL="http://localhost:8081"
FRONTEND_URL="http://localhost:3000"

echo -e "${YELLOW}1. Testing Auth Service Health...${NC}"
curl -s -o /dev/null -w "%{http_code}" $AUTH_SERVICE_URL/actuator/health
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Auth Service is running${NC}"
else
    echo -e "${RED}❌ Auth Service is not running${NC}"
    exit 1
fi

echo -e "${YELLOW}2. Testing Google Configuration Status...${NC}"
GOOGLE_CONFIG=$(curl -s $AUTH_SERVICE_URL/auth/v1/google-config-status)
echo "Google Config Status: $GOOGLE_CONFIG"

echo -e "${YELLOW}3. Testing Frontend Environment...${NC}"
if [ -f "frontend/.env.local" ]; then
    echo -e "${GREEN}✅ Frontend .env.local exists${NC}"
    if grep -q "NEXT_PUBLIC_GOOGLE_CLIENT_ID=546347981310" frontend/.env.local; then
        echo -e "${GREEN}✅ Google Client ID configured${NC}"
    else
        echo -e "${RED}❌ Google Client ID not configured${NC}"
    fi
else
    echo -e "${RED}❌ Frontend .env.local not found${NC}"
fi

echo -e "${YELLOW}4. Testing Backend Environment...${NC}"
if [ -f "auth-service/.env" ]; then
    echo -e "${GREEN}✅ Backend .env exists${NC}"
    if grep -q "GOOGLE_CLIENT_ID=546347981310" auth-service/.env; then
        echo -e "${GREEN}✅ Google Client ID configured${NC}"
    else
        echo -e "${RED}❌ Google Client ID not configured${NC}"
    fi
    if grep -q "GOOGLE_CLIENT_SECRET=GOCSPX" auth-service/.env; then
        echo -e "${GREEN}✅ Google Client Secret configured${NC}"
    else
        echo -e "${RED}❌ Google Client Secret not configured${NC}"
    fi
else
    echo -e "${RED}❌ Backend .env not found${NC}"
fi

echo -e "${YELLOW}5. Testing Google OAuth Endpoint...${NC}"
OAUTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $AUTH_SERVICE_URL/auth/v1/google-oauth \
    -H "Content-Type: application/json" \
    -d '{"credential":"test"}')

if [ "$OAUTH_RESPONSE" = "401" ] || [ "$OAUTH_RESPONSE" = "400" ]; then
    echo -e "${GREEN}✅ Google OAuth endpoint is accessible (expected 401/400 for invalid token)${NC}"
else
    echo -e "${RED}❌ Google OAuth endpoint returned unexpected status: $OAUTH_RESPONSE${NC}"
fi

echo ""
echo -e "${YELLOW}🎯 Manual Testing Instructions:${NC}"
echo "1. Start the auth service: cd auth-service && ./mvnw spring-boot:run"
echo "2. Start the frontend: cd frontend && npm run dev"
echo "3. Navigate to: $FRONTEND_URL/login"
echo "4. Click 'Continue with Google'"
echo "5. Complete Google authentication"
echo "6. Verify successful login and token generation"

echo ""
echo -e "${YELLOW}🔍 Debugging Commands:${NC}"
echo "- Check Google config: curl $AUTH_SERVICE_URL/auth/v1/google-config-status"
echo "- Check auth service logs: docker logs auth-service (if using Docker)"
echo "- Check Keycloak users: http://localhost:8080 (admin/admin)"

echo ""
echo -e "${GREEN}🎉 Google OAuth Integration Test Complete!${NC}"
