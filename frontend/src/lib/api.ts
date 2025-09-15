/**
 * API client for communicating with the backend services through API Gateway
 */

// API Configuration
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// API Error class
export class ApiError extends Error {
  constructor(
    public status: number,
    public statusText: string,
    message: string,
    public data?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

// Request configuration interface
interface RequestConfig extends RequestInit {
  timeout?: number;
}

// Response wrapper interface
export interface ApiResponse<T = any> {
  data: T;
  status: number;
  statusText: string;
  headers: Headers;
}

/**
 * Enhanced fetch with timeout, error handling, and automatic retries
 */
async function fetchWithTimeout(
  url: string,
  config: RequestConfig = {}
): Promise<Response> {
  const { timeout = 10000, ...fetchConfig } = config;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    const response = await fetch(url, {
      ...fetchConfig,
      signal: controller.signal,
    });

    clearTimeout(timeoutId);
    return response;
  } catch (error) {
    clearTimeout(timeoutId);
    throw error;
  }
}

/**
 * Main API client class
 */
class ApiClient {
  private baseURL: string;
  private defaultHeaders: Record<string, string>;

  constructor(baseURL: string = API_BASE_URL) {
    this.baseURL = baseURL;
    this.defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
  }

  /**
   * Set authentication token
   */
  setAuthToken(token: string) {
    this.defaultHeaders['Authorization'] = `Bearer ${token}`;
  }

  /**
   * Remove authentication token
   */
  removeAuthToken() {
    delete this.defaultHeaders['Authorization'];
  }

  /**
   * Generic request method
   */
  private async request<T = any>(
    endpoint: string,
    config: RequestConfig = {}
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseURL}${endpoint}`;
    
    const requestConfig: RequestConfig = {
      ...config,
      headers: {
        ...this.defaultHeaders,
        ...config.headers,
      },
    };

    try {
      const response = await fetchWithTimeout(url, requestConfig);

      // Handle non-JSON responses
      let data: any;
      const contentType = response.headers.get('content-type');
      
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else {
        data = await response.text();
      }

      // Handle HTTP errors
      if (!response.ok) {
        throw new ApiError(
          response.status,
          response.statusText,
          data?.message || data?.error || `HTTP ${response.status}: ${response.statusText}`,
          data
        );
      }

      return {
        data,
        status: response.status,
        statusText: response.statusText,
        headers: response.headers,
      };
    } catch (error) {
      // Handle network errors
      if (error instanceof ApiError) {
        throw error;
      }

      if (error instanceof Error) {
        if (error.name === 'AbortError') {
          throw new ApiError(408, 'Request Timeout', 'Request timed out');
        }
        throw new ApiError(0, 'Network Error', error.message);
      }

      throw new ApiError(0, 'Unknown Error', 'An unknown error occurred');
    }
  }

  /**
   * GET request
   */
  async get<T = any>(endpoint: string, config?: RequestConfig): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { ...config, method: 'GET' });
  }

  /**
   * POST request
   */
  async post<T = any>(
    endpoint: string,
    data?: any,
    config?: RequestConfig
  ): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      ...config,
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  /**
   * PUT request
   */
  async put<T = any>(
    endpoint: string,
    data?: any,
    config?: RequestConfig
  ): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      ...config,
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  /**
   * PATCH request
   */
  async patch<T = any>(
    endpoint: string,
    data?: any,
    config?: RequestConfig
  ): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      ...config,
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  /**
   * DELETE request
   */
  async delete<T = any>(endpoint: string, config?: RequestConfig): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { ...config, method: 'DELETE' });
  }

  /**
   * Upload file
   */
  async upload<T = any>(
    endpoint: string,
    file: File,
    additionalData?: Record<string, any>,
    config?: RequestConfig
  ): Promise<ApiResponse<T>> {
    const formData = new FormData();
    formData.append('file', file);

    if (additionalData) {
      Object.entries(additionalData).forEach(([key, value]) => {
        formData.append(key, value);
      });
    }

    const uploadConfig = {
      ...config,
      method: 'POST',
      body: formData,
      headers: {
        ...this.defaultHeaders,
        ...config?.headers,
      },
    };

    // Remove Content-Type header to let browser set it with boundary
    delete (uploadConfig.headers as any)['Content-Type'];

    return this.request<T>(endpoint, uploadConfig);
  }
}

// Create and export the default API client instance
export const apiClient = new ApiClient();

// Export the ApiClient class for creating custom instances
export { ApiClient };

// Enhanced error response interface for backend errors
export interface BackendErrorResponse {
  status: number;
  errorCode: string;
  message: string;
  details?: string;
  service: string;
  timestamp: string;
  path: string;
  correlationId?: string;
  fieldErrors?: Array<{
    field: string;
    message: string;
    rejectedValue?: any;
  }>;
  context?: Record<string, any>;
  cause?: BackendErrorResponse;
}

// Utility function to handle API errors in components
export function handleApiError(error: unknown): string {
  if (error instanceof ApiError) {
    // Check if the error data contains a structured backend error response
    if (error.data && typeof error.data === 'object') {
      const backendError = error.data as BackendErrorResponse;

      // Return the backend error message if available
      if (backendError.message) {
        return backendError.message;
      }

      // Handle field errors for validation failures
      if (backendError.fieldErrors && backendError.fieldErrors.length > 0) {
        const fieldMessages = backendError.fieldErrors.map(fe => `${fe.field}: ${fe.message}`);
        return fieldMessages.join(', ');
      }
    }

    return error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'An unexpected error occurred';
}

// Utility function to get detailed error information for debugging
export function getDetailedErrorInfo(error: unknown): {
  message: string;
  service?: string;
  errorCode?: string;
  correlationId?: string;
  fieldErrors?: Array<{ field: string; message: string }>;
} {
  if (error instanceof ApiError && error.data && typeof error.data === 'object') {
    const backendError = error.data as BackendErrorResponse;

    return {
      message: backendError.message || error.message,
      service: backendError.service,
      errorCode: backendError.errorCode,
      correlationId: backendError.correlationId,
      fieldErrors: backendError.fieldErrors,
    };
  }

  return {
    message: handleApiError(error),
  };
}

// Utility function to check if error is a specific HTTP status
export function isApiError(error: unknown, status?: number): error is ApiError {
  if (!(error instanceof ApiError)) {
    return false;
  }
  
  if (status !== undefined) {
    return error.status === status;
  }
  
  return true;
}

// Retry utility for failed requests
export async function retryRequest<T>(
  requestFn: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> {
  let lastError: unknown;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await requestFn();
    } catch (error) {
      lastError = error;
      
      // Don't retry on client errors (4xx) except 408, 429
      if (error instanceof ApiError) {
        const shouldRetry = 
          error.status >= 500 || // Server errors
          error.status === 408 || // Request timeout
          error.status === 429;   // Too many requests
          
        if (!shouldRetry || attempt === maxRetries) {
          throw error;
        }
      }

      // Wait before retrying (exponential backoff)
      if (attempt < maxRetries) {
        await new Promise(resolve => setTimeout(resolve, delay * Math.pow(2, attempt)));
      }
    }
  }

  throw lastError;
}

// ==================== DASHBOARD API FUNCTIONS ====================

/**
 * Get user dashboard data
 */
export async function getUserDashboard(userReferenceId: string) {
  const response = await apiClient.get(`/api/user-service/v1/users/${userReferenceId}/dashboard`);
  return response.data;
}

/**
 * Get user profile
 */
export async function getUserProfile(userReferenceId: string) {
  const response = await apiClient.get(`/api/user-service/v1/users/${userReferenceId}`);
  return response.data;
}

/**
 * Login user and store token
 */
export async function loginUser(email: string, password: string, rememberMe: boolean = false) {
  const response = await apiClient.post('/api/auth/v1/login', {
    email,
    password,
    rememberMe
  });

  // Store token for subsequent requests
  if (response.data?.accessToken) {
    apiClient.setAuthToken(response.data.accessToken);

    // Store in localStorage for persistence (in production, use HttpOnly cookies)
    if (typeof window !== 'undefined') {
      localStorage.setItem('accessToken', response.data.accessToken);
      localStorage.setItem('refreshToken', response.data.refreshToken || '');
      localStorage.setItem('user', JSON.stringify(response.data.user || {}));
    }
  }

  return response.data;
}

/**
 * Initialize API client with stored token
 */
export function initializeApiClient() {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('accessToken');
    if (token) {
      apiClient.setAuthToken(token);
    }
  }
}

/**
 * Get current user from stored data
 */
export function getCurrentUser() {
  if (typeof window !== 'undefined') {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  }
  return null;
}

/**
 * Logout user
 */
export async function logoutUser() {
  try {
    await apiClient.post('/api/auth/v1/logout');
  } catch (error) {
    console.error('Logout API call failed:', error);
  } finally {
    // Clear stored data
    apiClient.removeAuthToken();
    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  }
}
