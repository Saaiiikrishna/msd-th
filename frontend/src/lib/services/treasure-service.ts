/**
 * Treasure Service API client
 * Handles all treasure/rewards-related API calls through the API Gateway
 */

import { apiClient, ApiResponse, retryRequest } from '../api';

// Treasure interfaces
export interface TreasureBalance {
  userReferenceId: string;
  totalBalance: number;
  availableBalance: number;
  lockedBalance: number;
  currency: string;
  lastUpdated: string;
}

export interface TreasureTransaction {
  id: string;
  userReferenceId: string;
  type: 'EARNED' | 'SPENT' | 'TRANSFERRED' | 'BONUS' | 'REFUND' | 'ADJUSTMENT';
  amount: number;
  currency: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  description: string;
  source: string;
  referenceId?: string;
  metadata?: Record<string, any>;
  createdAt: string;
  processedAt?: string;
  expiresAt?: string;
}

export interface CreateTreasureTransactionRequest {
  type: 'EARNED' | 'SPENT' | 'TRANSFERRED' | 'BONUS' | 'REFUND' | 'ADJUSTMENT';
  amount: number;
  currency: string;
  description: string;
  source: string;
  referenceId?: string;
  metadata?: Record<string, any>;
  expiresAt?: string;
}

export interface TreasureReward {
  id: string;
  name: string;
  description: string;
  category: string;
  cost: number;
  currency: string;
  isActive: boolean;
  isLimited: boolean;
  availableQuantity?: number;
  imageUrl?: string;
  terms?: string;
  validFrom: string;
  validUntil?: string;
  createdAt: string;
}

export interface RedeemRewardRequest {
  rewardId: string;
  quantity?: number;
  deliveryAddress?: {
    addressLine1: string;
    addressLine2?: string;
    city: string;
    state: string;
    postalCode: string;
    country: string;
  };
  notes?: string;
}

export interface RedemptionHistory {
  id: string;
  userReferenceId: string;
  rewardId: string;
  rewardName: string;
  cost: number;
  currency: string;
  quantity: number;
  status: 'PENDING' | 'PROCESSING' | 'FULFILLED' | 'CANCELLED' | 'FAILED';
  redemptionCode?: string;
  deliveryAddress?: any;
  notes?: string;
  redeemedAt: string;
  fulfilledAt?: string;
  expiresAt?: string;
}

export interface TreasureTransfer {
  id: string;
  fromUserReferenceId: string;
  toUserReferenceId: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  message?: string;
  createdAt: string;
  processedAt?: string;
  failureReason?: string;
}

export interface CreateTransferRequest {
  toUserReferenceId: string;
  amount: number;
  currency: string;
  message?: string;
}

export interface TreasureHistory {
  content: TreasureTransaction[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface TreasureStats {
  totalEarned: number;
  totalSpent: number;
  totalTransferred: number;
  totalRedeemed: number;
  currentBalance: number;
  currency: string;
  period: string;
  topCategories: Array<{
    category: string;
    amount: number;
    percentage: number;
  }>;
}

// Trip interfaces for treasure hunt service
export interface Trip {
  id: string;
  title: string;
  summary: string;
  description: string;
  city: string;
  country: string;
  difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  duration: number;
  maxParticipants: number;
  currentParticipants: number;
  price: {
    amount: number;
    currency: string;
  };
  images: string[];
  startDate: string;
  endDate: string;
  isActive: boolean;
  vendor: {
    id: string;
    name: string;
    rating: number;
  };
}

export interface Enrollment {
  id: string;
  tripId: string;
  userId: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
  enrollmentType: 'INDIVIDUAL' | 'TEAM';
  teamName?: string;
  teamSize?: number;
  totalAmount: number;
  currency: string;
  paymentStatus: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
  enrolledAt: string;
  registrationId: string;
}

// Treasure Service API class
export class TreasureServiceAPI {
  private readonly basePath = '/api/treasure';

  /**
   * Get user treasure balance
   */
  async getTreasureBalance(userReferenceId: string): Promise<ApiResponse<TreasureBalance>> {
    return retryRequest(() => 
      apiClient.get<TreasureBalance>(`${this.basePath}/balance/${userReferenceId}`)
    );
  }

  /**
   * Create treasure transaction
   */
  async createTransaction(userReferenceId: string, transaction: CreateTreasureTransactionRequest): Promise<ApiResponse<TreasureTransaction>> {
    return retryRequest(() => 
      apiClient.post<TreasureTransaction>(`${this.basePath}/transactions`, {
        userReferenceId,
        ...transaction
      })
    );
  }

  /**
   * Get transaction by ID
   */
  async getTransaction(transactionId: string): Promise<ApiResponse<TreasureTransaction>> {
    return retryRequest(() => 
      apiClient.get<TreasureTransaction>(`${this.basePath}/transactions/${transactionId}`)
    );
  }

  /**
   * Get user transaction history
   */
  async getTransactionHistory(
    userReferenceId: string,
    page: number = 0,
    size: number = 20,
    type?: string,
    status?: string,
    fromDate?: string,
    toDate?: string
  ): Promise<ApiResponse<TreasureHistory>> {
    const params = new URLSearchParams({
      userReferenceId,
      page: page.toString(),
      size: size.toString()
    });

    if (type) params.append('type', type);
    if (status) params.append('status', status);
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);

    return retryRequest(() => 
      apiClient.get<TreasureHistory>(`${this.basePath}/transactions/history?${params.toString()}`)
    );
  }

  /**
   * Get available rewards
   */
  async getAvailableRewards(
    category?: string,
    minCost?: number,
    maxCost?: number,
    page: number = 0,
    size: number = 20
  ): Promise<ApiResponse<{
    content: TreasureReward[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  }>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString()
    });

    if (category) params.append('category', category);
    if (minCost !== undefined) params.append('minCost', minCost.toString());
    if (maxCost !== undefined) params.append('maxCost', maxCost.toString());

    return retryRequest(() => 
      apiClient.get(`${this.basePath}/rewards?${params.toString()}`)
    );
  }

  /**
   * Get reward by ID
   */
  async getReward(rewardId: string): Promise<ApiResponse<TreasureReward>> {
    return retryRequest(() => 
      apiClient.get<TreasureReward>(`${this.basePath}/rewards/${rewardId}`)
    );
  }

  /**
   * Redeem reward
   */
  async redeemReward(userReferenceId: string, redemption: RedeemRewardRequest): Promise<ApiResponse<RedemptionHistory>> {
    return retryRequest(() => 
      apiClient.post<RedemptionHistory>(`${this.basePath}/redemptions`, {
        userReferenceId,
        ...redemption
      })
    );
  }

  /**
   * Get redemption history
   */
  async getRedemptionHistory(
    userReferenceId: string,
    page: number = 0,
    size: number = 20,
    status?: string
  ): Promise<ApiResponse<{
    content: RedemptionHistory[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  }>> {
    const params = new URLSearchParams({
      userReferenceId,
      page: page.toString(),
      size: size.toString()
    });

    if (status) params.append('status', status);

    return retryRequest(() => 
      apiClient.get(`${this.basePath}/redemptions/history?${params.toString()}`)
    );
  }

  /**
   * Get redemption by ID
   */
  async getRedemption(redemptionId: string): Promise<ApiResponse<RedemptionHistory>> {
    return retryRequest(() => 
      apiClient.get<RedemptionHistory>(`${this.basePath}/redemptions/${redemptionId}`)
    );
  }

  /**
   * Cancel redemption
   */
  async cancelRedemption(redemptionId: string, reason?: string): Promise<ApiResponse<RedemptionHistory>> {
    return retryRequest(() => 
      apiClient.post<RedemptionHistory>(`${this.basePath}/redemptions/${redemptionId}/cancel`, { reason })
    );
  }

  /**
   * Transfer treasure to another user
   */
  async transferTreasure(fromUserReferenceId: string, transfer: CreateTransferRequest): Promise<ApiResponse<TreasureTransfer>> {
    return retryRequest(() => 
      apiClient.post<TreasureTransfer>(`${this.basePath}/transfers`, {
        fromUserReferenceId,
        ...transfer
      })
    );
  }

  /**
   * Get transfer by ID
   */
  async getTransfer(transferId: string): Promise<ApiResponse<TreasureTransfer>> {
    return retryRequest(() => 
      apiClient.get<TreasureTransfer>(`${this.basePath}/transfers/${transferId}`)
    );
  }

  /**
   * Get user transfer history
   */
  async getTransferHistory(
    userReferenceId: string,
    page: number = 0,
    size: number = 20,
    direction?: 'SENT' | 'RECEIVED'
  ): Promise<ApiResponse<{
    content: TreasureTransfer[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  }>> {
    const params = new URLSearchParams({
      userReferenceId,
      page: page.toString(),
      size: size.toString()
    });

    if (direction) params.append('direction', direction);

    return retryRequest(() => 
      apiClient.get(`${this.basePath}/transfers/history?${params.toString()}`)
    );
  }

  /**
   * Get treasure statistics
   */
  async getTreasureStats(
    userReferenceId: string,
    period: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY' = 'MONTHLY'
  ): Promise<ApiResponse<TreasureStats>> {
    return retryRequest(() => 
      apiClient.get<TreasureStats>(`${this.basePath}/stats?userReferenceId=${userReferenceId}&period=${period}`)
    );
  }

  /**
   * Get reward categories
   */
  async getRewardCategories(): Promise<ApiResponse<Array<{
    name: string;
    description: string;
    rewardCount: number;
    minCost: number;
    maxCost: number;
  }>>> {
    return retryRequest(() => 
      apiClient.get(`${this.basePath}/rewards/categories`)
    );
  }

  /**
   * Get leaderboard
   */
  async getLeaderboard(
    period: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ALL_TIME' = 'MONTHLY',
    limit: number = 10
  ): Promise<ApiResponse<Array<{
    rank: number;
    userReferenceId: string;
    displayName: string;
    totalEarned: number;
    currency: string;
  }>>> {
    return retryRequest(() => 
      apiClient.get(`${this.basePath}/leaderboard?period=${period}&limit=${limit}`)
    );
  }

  // Trip Management Methods

  /**
   * Get all available trips
   */
  async getTrips(filters?: {
    city?: string;
    difficulty?: string;
    page?: number;
    size?: number;
  }): Promise<ApiResponse<{
    trips: Trip[];
    totalCount: number;
  }>> {
    const params = new URLSearchParams();
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined) {
          params.append(key, value.toString());
        }
      });
    }

    const queryString = params.toString();
    const endpoint = queryString ? `${this.basePath}/v1/trips?${queryString}` : `${this.basePath}/v1/trips`;

    return retryRequest(() => apiClient.get(endpoint));
  }

  /**
   * Get trip by ID
   */
  async getTripById(tripId: string): Promise<ApiResponse<Trip>> {
    return retryRequest(() =>
      apiClient.get(`${this.basePath}/v1/trips/${tripId}`)
    );
  }

  /**
   * Enroll in a trip
   */
  async enrollInTrip(enrollmentData: {
    tripId: string;
    enrollmentType: 'INDIVIDUAL' | 'TEAM';
    teamName?: string;
    teamSize?: number;
  }): Promise<ApiResponse<Enrollment>> {
    return retryRequest(() =>
      apiClient.post(`${this.basePath}/v1/enrollments`, enrollmentData)
    );
  }

  /**
   * Get user enrollments
   */
  async getUserEnrollments(userId: string): Promise<ApiResponse<Enrollment[]>> {
    return retryRequest(() =>
      apiClient.get(`${this.basePath}/v1/enrollments/user/${userId}`)
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
export const treasureService = new TreasureServiceAPI();
