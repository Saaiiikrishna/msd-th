/**
 * User Service API client
 * Handles all user-related API calls through the API Gateway
 */

import { apiClient, ApiResponse, retryRequest } from '../api';

// User interfaces
export interface User {
  id: string;
  referenceId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  active: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  roles?: string[];
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
}

export interface UserAddress {
  id: string;
  type: 'HOME' | 'WORK' | 'OTHER';
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  isPrimary: boolean;
}

export interface CreateAddressRequest {
  type: 'HOME' | 'WORK' | 'OTHER';
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  isPrimary?: boolean;
}

export interface UserConsent {
  consentKey: string;
  granted: boolean;
  consentVersion: string;
  grantedAt?: string;
  withdrawnAt?: string;
  source: string;
  legalBasis: string;
}

export interface ConsentRequest {
  consentKey: string;
  granted: boolean;
  source: string;
  legalBasis: string;
}

export interface DataExportRequest {
  exportType: 'FULL_EXPORT' | 'PROFILE_ONLY' | 'ACTIVITY_ONLY';
  format: 'JSON' | 'CSV' | 'PDF';
}

export interface DataDeletionRequest {
  reason: string;
  retainAuditTrail: boolean;
}

// User Service API class
export class UserServiceAPI {
  private readonly basePath = '/api/user-service/v1/users';

  /**
   * Register a new user
   */
  async registerUser(userData: CreateUserRequest): Promise<ApiResponse<User>> {
    return retryRequest(() => 
      apiClient.post<User>(this.basePath, userData)
    );
  }

  /**
   * Get user by reference ID
   */
  async getUserByReferenceId(referenceId: string): Promise<ApiResponse<User>> {
    return retryRequest(() => 
      apiClient.get<User>(`${this.basePath}/${referenceId}`)
    );
  }

  /**
   * Update user profile
   */
  async updateUser(referenceId: string, userData: UpdateUserRequest): Promise<ApiResponse<User>> {
    return retryRequest(() => 
      apiClient.put<User>(`${this.basePath}/${referenceId}`, userData)
    );
  }

  /**
   * Delete user (soft delete)
   */
  async deleteUser(referenceId: string): Promise<ApiResponse<void>> {
    return retryRequest(() => 
      apiClient.delete<void>(`${this.basePath}/${referenceId}`)
    );
  }

  /**
   * Lookup user by email
   */
  async lookupUserByEmail(email: string): Promise<ApiResponse<User>> {
    return retryRequest(() => 
      apiClient.get<User>(`${this.basePath}/lookup?email=${encodeURIComponent(email)}`)
    );
  }

  /**
   * Lookup user by phone
   */
  async lookupUserByPhone(phone: string): Promise<ApiResponse<User>> {
    return retryRequest(() => 
      apiClient.get<User>(`${this.basePath}/lookup?phone=${encodeURIComponent(phone)}`)
    );
  }

  /**
   * Get user addresses
   */
  async getUserAddresses(referenceId: string): Promise<ApiResponse<UserAddress[]>> {
    return retryRequest(() => 
      apiClient.get<UserAddress[]>(`${this.basePath}/${referenceId}/addresses`)
    );
  }

  /**
   * Add user address
   */
  async addUserAddress(referenceId: string, address: CreateAddressRequest): Promise<ApiResponse<UserAddress>> {
    return retryRequest(() => 
      apiClient.post<UserAddress>(`${this.basePath}/${referenceId}/addresses`, address)
    );
  }

  /**
   * Update user address
   */
  async updateUserAddress(
    referenceId: string, 
    addressId: string, 
    address: Partial<CreateAddressRequest>
  ): Promise<ApiResponse<UserAddress>> {
    return retryRequest(() => 
      apiClient.put<UserAddress>(`${this.basePath}/${referenceId}/addresses/${addressId}`, address)
    );
  }

  /**
   * Delete user address
   */
  async deleteUserAddress(referenceId: string, addressId: string): Promise<ApiResponse<void>> {
    return retryRequest(() => 
      apiClient.delete<void>(`${this.basePath}/${referenceId}/addresses/${addressId}`)
    );
  }

  /**
   * Set primary address
   */
  async setPrimaryAddress(referenceId: string, addressId: string): Promise<ApiResponse<void>> {
    return retryRequest(() => 
      apiClient.patch<void>(`${this.basePath}/${referenceId}/addresses/${addressId}/primary`)
    );
  }

  /**
   * Get user consents
   */
  async getUserConsents(referenceId: string): Promise<ApiResponse<UserConsent[]>> {
    return retryRequest(() => 
      apiClient.get<UserConsent[]>(`${this.basePath}/${referenceId}/consents`)
    );
  }

  /**
   * Update user consent
   */
  async updateConsent(referenceId: string, consent: ConsentRequest): Promise<ApiResponse<UserConsent>> {
    return retryRequest(() => 
      apiClient.post<UserConsent>(`${this.basePath}/${referenceId}/consents`, consent)
    );
  }

  /**
   * Withdraw consent
   */
  async withdrawConsent(referenceId: string, consentKey: string): Promise<ApiResponse<void>> {
    return retryRequest(() => 
      apiClient.delete<void>(`${this.basePath}/${referenceId}/consents/${consentKey}`)
    );
  }

  /**
   * Request data export (GDPR Article 20)
   */
  async requestDataExport(referenceId: string, request: DataExportRequest): Promise<ApiResponse<{ exportId: string }>> {
    return retryRequest(() => 
      apiClient.post<{ exportId: string }>(`/api/v1/gdpr/export/${referenceId}`, request)
    );
  }

  /**
   * Get data export status
   */
  async getDataExportStatus(referenceId: string, exportId: string): Promise<ApiResponse<{
    status: 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
    downloadUrl?: string;
    expiresAt?: string;
  }>> {
    return retryRequest(() => 
      apiClient.get(`/api/v1/gdpr/export/${referenceId}/${exportId}`)
    );
  }

  /**
   * Download data export
   */
  async downloadDataExport(referenceId: string, exportId: string): Promise<ApiResponse<Blob>> {
    return retryRequest(() => 
      apiClient.get(`/api/v1/gdpr/export/${referenceId}/${exportId}/download`, {
        headers: { 'Accept': 'application/octet-stream' }
      })
    );
  }

  /**
   * Request data deletion (GDPR Article 17)
   */
  async requestDataDeletion(referenceId: string, request: DataDeletionRequest): Promise<ApiResponse<{ deletionId: string }>> {
    return retryRequest(() => 
      apiClient.post<{ deletionId: string }>(`/api/v1/gdpr/deletion/${referenceId}`, request)
    );
  }

  /**
   * Get data deletion status
   */
  async getDataDeletionStatus(referenceId: string, deletionId: string): Promise<ApiResponse<{
    status: 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
    totalRecordsDeleted?: number;
    deletedRecordsByCategory?: Record<string, number>;
  }>> {
    return retryRequest(() => 
      apiClient.get(`/api/v1/gdpr/deletion/${referenceId}/${deletionId}`)
    );
  }

  /**
   * Get user profile for current authenticated user
   */
  async getCurrentUserProfile(): Promise<ApiResponse<User>> {
    return retryRequest(() => 
      apiClient.get<User>(`${this.basePath}/profile`)
    );
  }

  /**
   * Update current user profile
   */
  async updateCurrentUserProfile(userData: UpdateUserRequest): Promise<ApiResponse<User>> {
    return retryRequest(() => 
      apiClient.put<User>(`${this.basePath}/profile`, userData)
    );
  }

  /**
   * Change user password
   */
  async changePassword(currentPassword: string, newPassword: string): Promise<ApiResponse<void>> {
    return retryRequest(() => 
      apiClient.post<void>(`${this.basePath}/profile/password`, {
        currentPassword,
        newPassword
      })
    );
  }

  /**
   * Get user activity/audit log
   */
  async getUserActivity(referenceId: string, page: number = 0, size: number = 20): Promise<ApiResponse<{
    content: Array<{
      eventType: string;
      description: string;
      timestamp: string;
      ipAddress?: string;
      userAgent?: string;
    }>;
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  }>> {
    return retryRequest(() => 
      apiClient.get(`${this.basePath}/${referenceId}/activity?page=${page}&size=${size}`)
    );
  }

  /**
   * Health check
   */
  async healthCheck(): Promise<ApiResponse<{ status: string }>> {
    return apiClient.get<{ status: string }>(`${this.basePath}/health`);
  }
}

// Export singleton instance
export const userService = new UserServiceAPI();
