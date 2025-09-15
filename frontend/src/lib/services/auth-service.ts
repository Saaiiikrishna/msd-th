/**
 * Authentication Service
 * Handles user authentication, session management, and token handling
 */

import { apiClient, ApiResponse, retryRequest } from '../api';

// Auth interfaces
export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: string;
    referenceId: string;
    firstName: string;
    lastName: string;
    email: string;
    roles: string[];
    active: boolean;
  };
  cookiesSet?: boolean;
  clientInfo?: {
    type: string;
    platform: string;
    userAgent: string;
  };
  error?: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  acceptTerms: boolean;
  acceptPrivacy: boolean;
  marketingConsent?: boolean;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordResetConfirmRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface SessionInfo {
  sessionId: string;
  userReferenceId: string;
  ipAddress: string;
  userAgent: string;
  createdAt: string;
  lastAccessedAt: string;
  expiresAt: string;
  active: boolean;
}

export interface TwoFactorSetupResponse {
  qrCodeUrl: string;
  backupCodes: string[];
  secret: string;
}

export interface TwoFactorVerifyRequest {
  code: string;
  backupCode?: string;
}

// Authentication Service class
export class AuthServiceAPI {
  private readonly basePath = '/api/v1/auth';
  private readonly keycloakBasePath = '/api/auth/v1';
  private accessToken: string | null = null;
  private refreshToken: string | null = null;
  private tokenExpiresAt: number | null = null;

  constructor() {
    // Load tokens from localStorage on initialization
    this.loadTokensFromStorage();
  }

  /**
   * Get Keycloak login information
   */
  async getLoginInfo(): Promise<ApiResponse<{
    authUrl: string;
    tokenUrl: string;
    logoutUrl: string;
    clientId: string;
    realm: string;
  }>> {
    return retryRequest(() =>
      apiClient.get(`${this.keycloakBasePath}/login-info`)
    );
  }

  /**
   * Login user with email and password
   */
  async login(credentials: LoginRequest, cookieConsent: boolean = false): Promise<ApiResponse<LoginResponse>> {
    try {
      const response = await retryRequest(() =>
        apiClient.post<LoginResponse>(`${this.keycloakBasePath}/login`, {
          email: credentials.email,
          password: credentials.password,
          rememberMe: credentials.rememberMe || false
        }, {
          headers: {
            'X-Client-Type': 'web',
            'X-Cookie-Consent': cookieConsent.toString()
          }
        })
      );

      // If login is successful, store the tokens
      if (response.status >= 200 && response.status < 300 && response.data) {
        this.setTokens(response.data);
      }

      return response;
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  }

  /**
   * Login user with Google OAuth
   */
  async loginWithGoogle(credential: string, cookieConsent: boolean = false): Promise<ApiResponse<LoginResponse>> {
    try {
      const response = await retryRequest(() =>
        apiClient.post<LoginResponse>(`${this.keycloakBasePath}/google-oauth`, {
          credential
        }, {
          headers: {
            'X-Client-Type': 'web',
            'X-Cookie-Consent': cookieConsent.toString()
          }
        })
      );

      if (response.data?.accessToken) {
        this.setTokens(response.data.accessToken, response.data.refreshToken);
      }

      return response;
    } catch (error) {
      console.error('Google login failed:', error);
      throw error;
    }
  }

  /**
   * Login user with OAuth2 (redirect to Keycloak)
   */
  async loginWithOAuth2(): Promise<void> {
    // For OAuth2 integration, we redirect to Keycloak login page
    const loginInfo = await this.getLoginInfo();
    if (loginInfo.status >= 200 && loginInfo.status < 300 && loginInfo.data) {
      // Redirect to Keycloak OAuth2 authorization endpoint
      const authUrl = new URL(loginInfo.data.authUrl);
      authUrl.searchParams.set('client_id', loginInfo.data.clientId);
      authUrl.searchParams.set('redirect_uri', `${window.location.origin}/auth/callback`);
      authUrl.searchParams.set('response_type', 'code');
      authUrl.searchParams.set('scope', 'openid profile email');

      window.location.href = authUrl.toString();
    }
  }

  /**
   * Register new user
   */
  async register(userData: RegisterRequest): Promise<ApiResponse<{
    message: string;
    userRef: string;
    email: string;
  }>> {
    return retryRequest(() =>
      apiClient.post(`${this.keycloakBasePath}/register`, userData)
    );
  }

  /**
   * Logout user
   */
  async logout(): Promise<ApiResponse<void>> {
    const response = await retryRequest(() => 
      apiClient.post<void>(`${this.basePath}/logout`, {
        refreshToken: this.refreshToken
      })
    );

    this.clearTokens();
    return response;
  }

  /**
   * Refresh access token
   */
  async refreshAccessToken(): Promise<ApiResponse<LoginResponse>> {
    if (!this.refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await retryRequest(() => 
      apiClient.post<LoginResponse>(`${this.basePath}/refresh`, {
        refreshToken: this.refreshToken
      })
    );

    if (response.status >= 200 && response.status < 300 && response.data) {
      this.setTokens(response.data);
    }

    return response;
  }

  /**
   * Request password reset
   */
  async requestPasswordReset(request: PasswordResetRequest): Promise<ApiResponse<{ message: string }>> {
    return retryRequest(() => 
      apiClient.post<{ message: string }>(`${this.basePath}/password-reset`, request)
    );
  }

  /**
   * Confirm password reset
   */
  async confirmPasswordReset(request: PasswordResetConfirmRequest): Promise<ApiResponse<{ message: string }>> {
    return retryRequest(() => 
      apiClient.post<{ message: string }>(`${this.basePath}/password-reset/confirm`, request)
    );
  }

  /**
   * Change password
   */
  async changePassword(request: ChangePasswordRequest): Promise<ApiResponse<{ message: string }>> {
    return retryRequest(() => 
      apiClient.post<{ message: string }>(`${this.basePath}/password-change`, request)
    );
  }

  /**
   * Get current user info
   */
  async getCurrentUser(): Promise<ApiResponse<LoginResponse['user']>> {
    return retryRequest(() =>
      apiClient.get<LoginResponse['user']>(`${this.keycloakBasePath}/me`)
    );
  }

  /**
   * Get user sessions
   */
  async getUserSessions(): Promise<ApiResponse<SessionInfo[]>> {
    return retryRequest(() => 
      apiClient.get<SessionInfo[]>(`${this.basePath}/sessions`)
    );
  }

  /**
   * Revoke session
   */
  async revokeSession(sessionId: string): Promise<ApiResponse<void>> {
    return retryRequest(() => 
      apiClient.delete<void>(`${this.basePath}/sessions/${sessionId}`)
    );
  }

  /**
   * Revoke all sessions except current
   */
  async revokeAllOtherSessions(): Promise<ApiResponse<{ revokedCount: number }>> {
    return retryRequest(() => 
      apiClient.post<{ revokedCount: number }>(`${this.basePath}/sessions/revoke-others`)
    );
  }

  /**
   * Setup two-factor authentication
   */
  async setupTwoFactor(): Promise<ApiResponse<TwoFactorSetupResponse>> {
    return retryRequest(() => 
      apiClient.post<TwoFactorSetupResponse>(`${this.basePath}/2fa/setup`)
    );
  }

  /**
   * Verify two-factor authentication setup
   */
  async verifyTwoFactorSetup(request: TwoFactorVerifyRequest): Promise<ApiResponse<{ enabled: boolean }>> {
    return retryRequest(() => 
      apiClient.post<{ enabled: boolean }>(`${this.basePath}/2fa/verify-setup`, request)
    );
  }

  /**
   * Disable two-factor authentication
   */
  async disableTwoFactor(request: TwoFactorVerifyRequest): Promise<ApiResponse<{ disabled: boolean }>> {
    return retryRequest(() => 
      apiClient.post<{ disabled: boolean }>(`${this.basePath}/2fa/disable`, request)
    );
  }

  /**
   * Verify two-factor authentication code
   */
  async verifyTwoFactor(request: TwoFactorVerifyRequest): Promise<ApiResponse<LoginResponse>> {
    const response = await retryRequest(() => 
      apiClient.post<LoginResponse>(`${this.basePath}/2fa/verify`, request)
    );

    if (response.status >= 200 && response.status < 300 && response.data) {
      this.setTokens(response.data);
    }

    return response;
  }

  /**
   * Generate new backup codes
   */
  async generateBackupCodes(): Promise<ApiResponse<{ backupCodes: string[] }>> {
    return retryRequest(() => 
      apiClient.post<{ backupCodes: string[] }>(`${this.basePath}/2fa/backup-codes`)
    );
  }

  /**
   * Verify email address
   */
  async verifyEmail(token: string): Promise<ApiResponse<{ verified: boolean }>> {
    return retryRequest(() => 
      apiClient.post<{ verified: boolean }>(`${this.basePath}/verify-email`, { token })
    );
  }

  /**
   * Resend email verification
   */
  async resendEmailVerification(): Promise<ApiResponse<{ message: string }>> {
    return retryRequest(() => 
      apiClient.post<{ message: string }>(`${this.basePath}/verify-email/resend`)
    );
  }

  // Token management methods

  /**
   * Get current access token
   */
  getAccessToken(): string | null {
    return this.accessToken;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.accessToken !== null && !this.isTokenExpired();
  }

  /**
   * Check if token is expired
   */
  isTokenExpired(): boolean {
    if (!this.tokenExpiresAt) return true;
    return Date.now() >= this.tokenExpiresAt;
  }

  /**
   * Set authentication tokens
   */
  public setTokens(loginResponse: LoginResponse): void {
    this.accessToken = loginResponse.accessToken;
    this.refreshToken = loginResponse.refreshToken;
    this.tokenExpiresAt = Date.now() + (loginResponse.expiresIn * 1000);

    // Set token in API client for subsequent requests
    apiClient.setAuthToken(this.accessToken);

    // Save to localStorage
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.setItem('accessToken', this.accessToken);
      localStorage.setItem('refreshToken', this.refreshToken);
      localStorage.setItem('tokenExpiresAt', this.tokenExpiresAt.toString());
      localStorage.setItem('user', JSON.stringify(loginResponse.user));
    }
  }

  /**
   * Clear authentication tokens
   */
  private clearTokens(): void {
    this.accessToken = null;
    this.refreshToken = null;
    this.tokenExpiresAt = null;

    // Remove token from API client
    apiClient.removeAuthToken();

    // Clear from localStorage
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('tokenExpiresAt');
      localStorage.removeItem('user');
    }
  }

  /**
   * Load tokens from localStorage
   */
  private loadTokensFromStorage(): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      this.accessToken = localStorage.getItem('accessToken');
      this.refreshToken = localStorage.getItem('refreshToken');
      const expiresAt = localStorage.getItem('tokenExpiresAt');
      this.tokenExpiresAt = expiresAt ? parseInt(expiresAt) : null;
    }
  }

  /**
   * Get stored user info
   */
  getStoredUser(): LoginResponse['user'] | null {
    if (typeof window !== 'undefined' && window.localStorage) {
      const userStr = localStorage.getItem('user');
      return userStr ? JSON.parse(userStr) : null;
    }
    return null;
  }

  /**
   * Auto-refresh token if needed
   */
  async ensureValidToken(): Promise<boolean> {
    if (!this.isAuthenticated()) {
      if (this.refreshToken) {
        try {
          await this.refreshAccessToken();
          return true;
        } catch (error) {
          this.clearTokens();
          return false;
        }
      }
      return false;
    }
    return true;
  }
}

// Export singleton instance
export const authService = new AuthServiceAPI();
