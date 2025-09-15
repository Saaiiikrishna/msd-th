/**
 * Payment Service API client
 * Handles all payment-related API calls through the API Gateway
 */

import { apiClient, ApiResponse, retryRequest } from '../api';

// Payment interfaces
export interface PaymentMethod {
  id: string;
  userReferenceId: string;
  type: 'CREDIT_CARD' | 'DEBIT_CARD' | 'UPI' | 'NET_BANKING' | 'WALLET';
  provider: string;
  maskedDetails: string;
  isDefault: boolean;
  isActive: boolean;
  expiresAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentMethodRequest {
  type: 'CREDIT_CARD' | 'DEBIT_CARD' | 'UPI' | 'NET_BANKING' | 'WALLET';
  provider: string;
  encryptedDetails: string;
  isDefault?: boolean;
}

export interface Payment {
  id: string;
  userReferenceId: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'REFUNDED';
  paymentMethodId: string;
  transactionId?: string;
  gatewayTransactionId?: string;
  description: string;
  metadata?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
  failureReason?: string;
}

export interface CreatePaymentRequest {
  amount: number;
  currency: string;
  paymentMethodId: string;
  description: string;
  metadata?: Record<string, any>;
  returnUrl?: string;
  webhookUrl?: string;
}

export interface PaymentIntent {
  id: string;
  clientSecret: string;
  amount: number;
  currency: string;
  status: 'REQUIRES_PAYMENT_METHOD' | 'REQUIRES_CONFIRMATION' | 'REQUIRES_ACTION' | 'PROCESSING' | 'SUCCEEDED' | 'CANCELLED';
  paymentMethods: string[];
  metadata?: Record<string, any>;
}

export interface CreatePaymentIntentRequest {
  amount: number;
  currency: string;
  description: string;
  paymentMethods?: string[];
  metadata?: Record<string, any>;
}

export interface Refund {
  id: string;
  paymentId: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  reason: string;
  createdAt: string;
  processedAt?: string;
  failureReason?: string;
}

export interface CreateRefundRequest {
  amount?: number; // If not provided, full refund
  reason: string;
  metadata?: Record<string, any>;
}

export interface PaymentHistory {
  content: Payment[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface PaymentStats {
  totalPayments: number;
  totalAmount: number;
  successfulPayments: number;
  failedPayments: number;
  averageAmount: number;
  currency: string;
  period: string;
}

// Payment Service API class
export class PaymentServiceAPI {
  private readonly basePath = '/api/v1/payments';

  /**
   * Get user payment methods
   */
  async getPaymentMethods(userReferenceId: string): Promise<ApiResponse<PaymentMethod[]>> {
    return retryRequest(() => 
      apiClient.get<PaymentMethod[]>(`${this.basePath}/methods?userReferenceId=${userReferenceId}`)
    );
  }

  /**
   * Add payment method
   */
  async addPaymentMethod(userReferenceId: string, paymentMethod: CreatePaymentMethodRequest): Promise<ApiResponse<PaymentMethod>> {
    return retryRequest(() => 
      apiClient.post<PaymentMethod>(`${this.basePath}/methods`, {
        userReferenceId,
        ...paymentMethod
      })
    );
  }

  /**
   * Update payment method
   */
  async updatePaymentMethod(methodId: string, updates: Partial<CreatePaymentMethodRequest>): Promise<ApiResponse<PaymentMethod>> {
    return retryRequest(() => 
      apiClient.put<PaymentMethod>(`${this.basePath}/methods/${methodId}`, updates)
    );
  }

  /**
   * Delete payment method
   */
  async deletePaymentMethod(methodId: string): Promise<ApiResponse<void>> {
    return retryRequest(() => 
      apiClient.delete<void>(`${this.basePath}/methods/${methodId}`)
    );
  }

  /**
   * Set default payment method
   */
  async setDefaultPaymentMethod(methodId: string): Promise<ApiResponse<void>> {
    return retryRequest(() => 
      apiClient.patch<void>(`${this.basePath}/methods/${methodId}/default`)
    );
  }

  /**
   * Create payment intent
   */
  async createPaymentIntent(userReferenceId: string, request: CreatePaymentIntentRequest): Promise<ApiResponse<PaymentIntent>> {
    return retryRequest(() => 
      apiClient.post<PaymentIntent>(`${this.basePath}/intents`, {
        userReferenceId,
        ...request
      })
    );
  }

  /**
   * Get payment intent
   */
  async getPaymentIntent(intentId: string): Promise<ApiResponse<PaymentIntent>> {
    return retryRequest(() => 
      apiClient.get<PaymentIntent>(`${this.basePath}/intents/${intentId}`)
    );
  }

  /**
   * Confirm payment intent
   */
  async confirmPaymentIntent(intentId: string, paymentMethodId: string): Promise<ApiResponse<PaymentIntent>> {
    return retryRequest(() => 
      apiClient.post<PaymentIntent>(`${this.basePath}/intents/${intentId}/confirm`, {
        paymentMethodId
      })
    );
  }

  /**
   * Cancel payment intent
   */
  async cancelPaymentIntent(intentId: string): Promise<ApiResponse<void>> {
    return retryRequest(() => 
      apiClient.post<void>(`${this.basePath}/intents/${intentId}/cancel`)
    );
  }

  /**
   * Create payment
   */
  async createPayment(userReferenceId: string, payment: CreatePaymentRequest): Promise<ApiResponse<Payment>> {
    return retryRequest(() => 
      apiClient.post<Payment>(this.basePath, {
        userReferenceId,
        ...payment
      })
    );
  }

  /**
   * Get payment by ID
   */
  async getPayment(paymentId: string): Promise<ApiResponse<Payment>> {
    return retryRequest(() => 
      apiClient.get<Payment>(`${this.basePath}/${paymentId}`)
    );
  }

  /**
   * Get user payment history
   */
  async getPaymentHistory(
    userReferenceId: string, 
    page: number = 0, 
    size: number = 20,
    status?: string,
    fromDate?: string,
    toDate?: string
  ): Promise<ApiResponse<PaymentHistory>> {
    const params = new URLSearchParams({
      userReferenceId,
      page: page.toString(),
      size: size.toString()
    });

    if (status) params.append('status', status);
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);

    return retryRequest(() => 
      apiClient.get<PaymentHistory>(`${this.basePath}/history?${params.toString()}`)
    );
  }

  /**
   * Cancel payment
   */
  async cancelPayment(paymentId: string, reason?: string): Promise<ApiResponse<Payment>> {
    return retryRequest(() => 
      apiClient.post<Payment>(`${this.basePath}/${paymentId}/cancel`, { reason })
    );
  }

  /**
   * Create refund
   */
  async createRefund(paymentId: string, refund: CreateRefundRequest): Promise<ApiResponse<Refund>> {
    return retryRequest(() => 
      apiClient.post<Refund>(`${this.basePath}/${paymentId}/refunds`, refund)
    );
  }

  /**
   * Get refund by ID
   */
  async getRefund(paymentId: string, refundId: string): Promise<ApiResponse<Refund>> {
    return retryRequest(() => 
      apiClient.get<Refund>(`${this.basePath}/${paymentId}/refunds/${refundId}`)
    );
  }

  /**
   * Get payment refunds
   */
  async getPaymentRefunds(paymentId: string): Promise<ApiResponse<Refund[]>> {
    return retryRequest(() => 
      apiClient.get<Refund[]>(`${this.basePath}/${paymentId}/refunds`)
    );
  }

  /**
   * Get payment statistics
   */
  async getPaymentStats(
    userReferenceId: string,
    period: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY' = 'MONTHLY'
  ): Promise<ApiResponse<PaymentStats>> {
    return retryRequest(() => 
      apiClient.get<PaymentStats>(`${this.basePath}/stats?userReferenceId=${userReferenceId}&period=${period}`)
    );
  }

  /**
   * Verify payment webhook
   */
  async verifyWebhook(payload: string, signature: string): Promise<ApiResponse<{ verified: boolean }>> {
    return retryRequest(() => 
      apiClient.post<{ verified: boolean }>(`${this.basePath}/webhooks/verify`, {
        payload,
        signature
      })
    );
  }

  /**
   * Get supported payment methods
   */
  async getSupportedPaymentMethods(): Promise<ApiResponse<{
    methods: Array<{
      type: string;
      providers: string[];
      currencies: string[];
      minimumAmount: number;
      maximumAmount: number;
    }>;
  }>> {
    return retryRequest(() => 
      apiClient.get(`${this.basePath}/methods/supported`)
    );
  }

  /**
   * Get payment fees
   */
  async getPaymentFees(amount: number, currency: string, paymentMethod: string): Promise<ApiResponse<{
    processingFee: number;
    platformFee: number;
    totalFee: number;
    netAmount: number;
  }>> {
    return retryRequest(() => 
      apiClient.get(`${this.basePath}/fees?amount=${amount}&currency=${currency}&method=${paymentMethod}`)
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
export const paymentService = new PaymentServiceAPI();
