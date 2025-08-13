import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';

// Payment types
export interface PaymentMethod {
  id: string;
  type: 'card' | 'upi' | 'netbanking' | 'wallet';
  provider: string;
  last4?: string;
  expiryMonth?: number;
  expiryYear?: number;
  isDefault: boolean;
  metadata?: Record<string, any>;
}

export interface PaymentIntent {
  id: string;
  amount: number;
  currency: string;
  status: 'pending' | 'processing' | 'succeeded' | 'failed' | 'cancelled';
  clientSecret: string;
  metadata?: Record<string, any>;
}

export interface PaymentResult {
  success: boolean;
  paymentId?: string;
  error?: string;
  redirectUrl?: string;
}

/**
 * Hook for processing payments
 */
export function usePaymentProcessing() {
  const [isProcessing, setIsProcessing] = useState(false);
  const queryClient = useQueryClient();

  const processPaymentMutation = useMutation({
    mutationFn: async (paymentData: {
      method: string;
      data: any;
      amount?: number;
      currency?: string;
      metadata?: Record<string, any>;
    }) => {
      setIsProcessing(true);
      
      try {
        // Create payment intent
        const intentResponse = await apiClient.post<PaymentIntent>('/api/payments/create-intent', {
          amount: paymentData.amount,
          currency: paymentData.currency || 'INR',
          metadata: paymentData.metadata,
        });

        const paymentIntent = intentResponse.data;

        // Process payment based on method
        let result: PaymentResult;
        
        switch (paymentData.method) {
          case 'cards':
            result = await processCardPayment(paymentIntent, paymentData.data);
            break;
          case 'upi':
            result = await processUPIPayment(paymentIntent, paymentData.data);
            break;
          case 'netbanking':
            result = await processNetBankingPayment(paymentIntent, paymentData.data);
            break;
          case 'wallets':
            result = await processWalletPayment(paymentIntent, paymentData.data);
            break;
          case 'international':
            result = await processInternationalPayment(paymentIntent, paymentData.data);
            break;
          default:
            throw new Error('Unsupported payment method');
        }

        return result;
      } finally {
        setIsProcessing(false);
      }
    },
    onSuccess: (result) => {
      if (result.success) {
        // Invalidate relevant queries
        queryClient.invalidateQueries({ queryKey: ['payment-history'] });
        queryClient.invalidateQueries({ queryKey: ['bookings'] });
        
        // Redirect if needed
        if (result.redirectUrl) {
          window.location.href = result.redirectUrl;
        }
      }
    },
  });

  const processCardPayment = async (intent: PaymentIntent, cardData: any): Promise<PaymentResult> => {
    // Simulate Razorpay card payment
    return new Promise((resolve) => {
      setTimeout(() => {
        const success = Math.random() > 0.1; // 90% success rate
        resolve({
          success,
          paymentId: success ? `pay_${Date.now()}` : undefined,
          error: success ? undefined : 'Card payment failed. Please try again.',
        });
      }, 2000);
    });
  };

  const processUPIPayment = async (intent: PaymentIntent, upiData: any): Promise<PaymentResult> => {
    // Simulate UPI payment
    return new Promise((resolve) => {
      setTimeout(() => {
        const success = Math.random() > 0.05; // 95% success rate
        resolve({
          success,
          paymentId: success ? `upi_${Date.now()}` : undefined,
          error: success ? undefined : 'UPI payment failed. Please check your UPI ID.',
        });
      }, 1500);
    });
  };

  const processNetBankingPayment = async (intent: PaymentIntent, bankData: any): Promise<PaymentResult> => {
    // Simulate net banking redirect
    return {
      success: true,
      redirectUrl: `/payment/netbanking?intent=${intent.id}&bank=${bankData.bankCode}`,
    };
  };

  const processWalletPayment = async (intent: PaymentIntent, walletData: any): Promise<PaymentResult> => {
    // Simulate wallet payment
    return new Promise((resolve) => {
      setTimeout(() => {
        const success = Math.random() > 0.08; // 92% success rate
        resolve({
          success,
          paymentId: success ? `wallet_${Date.now()}` : undefined,
          error: success ? undefined : 'Wallet payment failed. Please check your wallet balance.',
        });
      }, 1000);
    });
  };

  const processInternationalPayment = async (intent: PaymentIntent, cardData: any): Promise<PaymentResult> => {
    // Simulate international card payment with additional verification
    return new Promise((resolve) => {
      setTimeout(() => {
        const success = Math.random() > 0.15; // 85% success rate (lower due to international)
        resolve({
          success,
          paymentId: success ? `intl_${Date.now()}` : undefined,
          error: success ? undefined : 'International payment failed. Please verify your card details.',
        });
      }, 3000);
    });
  };

  return {
    processPayment: processPaymentMutation.mutate,
    isLoading: processPaymentMutation.isPending || isProcessing,
    error: processPaymentMutation.error,
    data: processPaymentMutation.data,
  };
}

/**
 * Hook for managing saved payment methods
 */
export function usePaymentMethods() {
  const queryClient = useQueryClient();

  const { data: paymentMethods, isLoading } = useQuery({
    queryKey: ['payment-methods'],
    queryFn: async () => {
      const response = await apiClient.get<PaymentMethod[]>('/api/payments/methods');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  const addPaymentMethodMutation = useMutation({
    mutationFn: async (methodData: Omit<PaymentMethod, 'id'>) => {
      const response = await apiClient.post('/api/payments/methods', methodData);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payment-methods'] });
    },
  });

  const removePaymentMethodMutation = useMutation({
    mutationFn: async (methodId: string) => {
      await apiClient.delete(`/api/payments/methods/${methodId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payment-methods'] });
    },
  });

  const setDefaultPaymentMethodMutation = useMutation({
    mutationFn: async (methodId: string) => {
      await apiClient.patch(`/api/payments/methods/${methodId}/default`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payment-methods'] });
    },
  });

  return {
    paymentMethods,
    isLoading,
    addPaymentMethod: addPaymentMethodMutation.mutate,
    removePaymentMethod: removePaymentMethodMutation.mutate,
    setDefaultPaymentMethod: setDefaultPaymentMethodMutation.mutate,
    isAdding: addPaymentMethodMutation.isPending,
    isRemoving: removePaymentMethodMutation.isPending,
  };
}

/**
 * Hook for payment history
 */
export function usePaymentHistory() {
  return useQuery({
    queryKey: ['payment-history'],
    queryFn: async () => {
      const response = await apiClient.get('/api/payments/history');
      return response.data;
    },
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

/**
 * Hook for refund processing
 */
export function useRefundProcessing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (refundData: {
      paymentId: string;
      amount?: number;
      reason: string;
    }) => {
      const response = await apiClient.post('/api/payments/refund', refundData);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payment-history'] });
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
    },
  });
}

/**
 * Hook for payment verification
 */
export function usePaymentVerification() {
  return useMutation({
    mutationFn: async (paymentId: string) => {
      const response = await apiClient.get(`/api/payments/${paymentId}/verify`);
      return response.data;
    },
  });
}

/**
 * Hook for payment analytics
 */
export function usePaymentAnalytics() {
  return useQuery({
    queryKey: ['payment-analytics'],
    queryFn: async () => {
      const response = await apiClient.get('/api/payments/analytics');
      return response.data;
    },
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
}

/**
 * Hook for currency conversion rates
 */
export function useCurrencyRates() {
  return useQuery({
    queryKey: ['currency-rates'],
    queryFn: async () => {
      const response = await apiClient.get('/api/payments/currency-rates');
      return response.data;
    },
    staleTime: 15 * 60 * 1000, // 15 minutes
    refetchInterval: 15 * 60 * 1000, // Refetch every 15 minutes
  });
}

/**
 * Hook for EMI options
 */
export function useEMIOptions(amount: number) {
  return useQuery({
    queryKey: ['emi-options', amount],
    queryFn: async () => {
      const response = await apiClient.get(`/api/payments/emi-options?amount=${amount}`);
      return response.data;
    },
    enabled: amount > 0,
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
}
