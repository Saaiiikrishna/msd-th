# 🔐 Google OAuth Setup Guide

## 📋 **OVERVIEW**

This guide will help you configure Google OAuth for your application, including:
- Google Cloud Console setup
- OAuth 2.0 Client ID configuration
- Consent screen configuration
- Keycloak integration
- Environment variables setup

## 🚀 **STEP 1: Google Cloud Console Setup**

### **1.1 Create or Select a Project**

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select an existing project or create a new one:
   - Click "Select a project" dropdown
   - Click "New Project"
   - Enter project name: `msd-treasure-hunt` (or your preferred name)
   - Click "Create"

### **1.2 Enable Google+ API (if needed)**

1. In the Google Cloud Console, go to "APIs & Services" > "Library"
2. Search for "Google+ API" or "People API"
3. Click on it and click "Enable"

## 🔧 **STEP 2: OAuth 2.0 Client ID Configuration**

### **2.1 Configure OAuth Consent Screen**

1. Go to "APIs & Services" > "OAuth consent screen"
2. Choose "External" (for testing) or "Internal" (for organization use)
3. Fill in the required information:

```
App name: MySillyDreams Treasure Hunt
User support email: your-email@example.com
Developer contact information: your-email@example.com

App domain (optional):
- Application home page: http://localhost:3000
- Application privacy policy link: http://localhost:3000/privacy
- Application terms of service link: http://localhost:3000/terms

Authorized domains:
- localhost (for development)
- yourdomain.com (for production)
```

4. **Scopes**: Add the following scopes:
   - `email`
   - `profile`
   - `openid`

5. **Test users** (for External apps in testing):
   - Add your email addresses for testing

### **2.2 Create OAuth 2.0 Client ID**

1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth 2.0 Client ID"
3. Choose "Web application"
4. Configure the client:

```
Name: MySillyDreams Web Client

Authorized JavaScript origins:
- http://localhost:3000 (development)
- https://yourdomain.com (production)

Authorized redirect URIs:
- http://localhost:3000 (for Google Sign-In)
- http://localhost:3000/auth/callback (if using server-side flow)
- http://localhost:8081/auth/v1/callback (backend callback)
```

5. Click "Create"
6. **IMPORTANT**: Copy the Client ID and Client Secret

## 🔑 **STEP 3: Environment Variables Setup**

### **3.1 Frontend Environment Variables**

Create/update `frontend/.env.local`:

```env
# Google OAuth Configuration
NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-google-client-id-here.apps.googleusercontent.com

# API Configuration
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
```

### **3.2 Backend Environment Variables**

Create/update `auth-service/src/main/resources/application.yml`:

```yaml
# Google OAuth Configuration
google:
  oauth:
    client-id: ${GOOGLE_CLIENT_ID:your-google-client-id-here.apps.googleusercontent.com}
    client-secret: ${GOOGLE_CLIENT_SECRET:your-google-client-secret-here}
    redirect-uri: ${GOOGLE_REDIRECT_URI:http://localhost:8081/auth/v1/google/callback}

# Keycloak Configuration
keycloak:
  realm: treasure-hunt
  server-url: http://localhost:8080
  auth-server-url: http://localhost:8080
  client-id: backend-service
  credentials:
    secret: your-keycloak-client-secret
  admin:
    username: admin
    password: admin
    client-id: admin-cli
```

Or set as environment variables:

```bash
export GOOGLE_CLIENT_ID="your-google-client-id-here.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="your-google-client-secret-here"
export GOOGLE_REDIRECT_URI="http://localhost:8081/auth/v1/google/callback"
```

## 🔧 **STEP 4: Keycloak Configuration**

### **4.1 Manual Keycloak Setup (Recommended for Development)**

1. **Access Keycloak Admin Console**:
   - URL: http://localhost:8080
   - Username: admin
   - Password: admin

2. **Configure Google Identity Provider**:
   - Go to "Identity Providers"
   - Click "Add provider" > "Google"
   - Configure:
     ```
     Alias: google
     Display Name: Google
     Client ID: your-google-client-id
     Client Secret: your-google-client-secret
     ```

3. **Configure User Attributes**:
   - Go to "User Attributes"
   - Add custom attributes:
     - `google_id`
     - `google_picture`
     - `registration_method`
     - `account_linking_enabled`

### **4.2 Programmatic Keycloak Setup (Production)**

The auth service can automatically configure Keycloak using the admin API.
This is handled in the `KeycloakService` class.

## 🧪 **STEP 5: Testing the Setup**

### **5.1 Test Google OAuth Flow**

1. Start your application:
   ```bash
   # Backend
   cd auth-service
   ./mvnw spring-boot:run

   # Frontend
   cd frontend
   npm run dev
   ```

2. Navigate to: http://localhost:3000/login
3. Click "Continue with Google"
4. Complete Google authentication
5. Verify user is created in Keycloak

### **5.2 Test Account Linking**

1. **Scenario 1**: Register with email/password first, then login with Google
2. **Scenario 2**: Login with Google first, then try email/password
3. Verify both scenarios use the SAME user account

## 🔒 **SECURITY CONSIDERATIONS**

### **Production Setup**

1. **Use HTTPS**: Always use HTTPS in production
2. **Secure Client Secret**: Store client secret securely (environment variables, secrets manager)
3. **Domain Restrictions**: Limit authorized domains to your actual domains
4. **Scope Minimization**: Only request necessary scopes
5. **Token Validation**: Always validate Google ID tokens on the backend

### **CORS Configuration**

Ensure your backend allows requests from your frontend domain:

```yaml
# application.yml
spring:
  web:
    cors:
      allowed-origins:
        - http://localhost:3000
        - https://yourdomain.com
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowed-headers: "*"
      allow-credentials: true
```

## 🎯 **NEXT STEPS**

1. Complete the Google Cloud Console setup
2. Configure environment variables
3. Test the OAuth flow
4. Configure Keycloak identity provider (optional)
5. Deploy to production with HTTPS

## 🆘 **TROUBLESHOOTING**

### Common Issues:

1. **"redirect_uri_mismatch"**: Check authorized redirect URIs in Google Console
2. **"invalid_client"**: Verify client ID and secret are correct
3. **CORS errors**: Check CORS configuration in backend
4. **Token validation fails**: Ensure Google client ID matches in frontend and backend

### Debug Steps:

1. Check browser console for errors
2. Verify network requests in browser dev tools
3. Check backend logs for authentication errors
4. Verify Keycloak user creation in admin console
