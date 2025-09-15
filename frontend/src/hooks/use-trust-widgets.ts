import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';

// Trust widget types
export interface TrustIndicator {
  type: string;
  title: string;
  icon: string;
  value: string;
  description: string;
}

export interface MoneyBackGuarantee {
  enabled: boolean;
  title: string;
  description: string;
  guaranteePeriodDays: number;
  iconUrl: string;
  termsUrl: string;
  displayPosition: string;
  style: Record<string, string>;
}

export interface SecurityBadge {
  enabled: boolean;
  title: string;
  description: string;
  badgeUrl: string;
  securityFeatures: string[];
  certifications: string[];
  displayPosition: string;
  style: Record<string, string>;
}

export interface PaymentMethodWidget {
  enabled: boolean;
  title: string;
  supportedMethods: Array<{
    type: string;
    name: string;
    icon: string;
    brands?: string[];
    providers?: string[];
    banks?: string[];
  }>;
  displayPosition: string;
  style: Record<string, string>;
}

export interface CustomerTestimonial {
  name: string;
  location: string;
  rating: number;
  comment: string;
  avatar: string;
  date: string;
  huntTitle?: string;
  videoTestimonial?: boolean;
}

export interface TrustWidgetConfig {
  moneyBackGuarantee: MoneyBackGuarantee;
  securityBadge: SecurityBadge;
  paymentMethods: PaymentMethodWidget;
  trustIndicators: TrustIndicator[];
  customerTestimonials: CustomerTestimonial[];
}

/**
 * Hook to fetch complete trust widget configuration
 */
export function useTrustWidgets() {
  return useQuery({
    queryKey: ['trust-widgets'],
    queryFn: async () => {
      const response = await apiClient.get<TrustWidgetConfig>('/api/trust/config');
      return response.data;
    },
    staleTime: 10 * 60 * 1000, // 10 minutes
    gcTime: 30 * 60 * 1000, // 30 minutes
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });
}

/**
 * Hook to fetch money back guarantee widget
 */
export function useMoneyBackGuarantee() {
  return useQuery({
    queryKey: ['money-back-guarantee'],
    queryFn: async () => {
      const response = await apiClient.get<{ moneyBackGuarantee: MoneyBackGuarantee }>('/api/trust/money-back-guarantee');
      return response.data.moneyBackGuarantee;
    },
    staleTime: 15 * 60 * 1000, // 15 minutes
  });
}

/**
 * Hook to fetch security badge widget
 */
export function useSecurityBadge() {
  return useQuery({
    queryKey: ['security-badge'],
    queryFn: async () => {
      const response = await apiClient.get<{ securityBadge: SecurityBadge }>('/api/trust/security-badge');
      return response.data.securityBadge;
    },
    staleTime: 15 * 60 * 1000, // 15 minutes
  });
}

/**
 * Hook to fetch payment methods widget
 */
export function usePaymentMethods() {
  return useQuery({
    queryKey: ['payment-methods'],
    queryFn: async () => {
      const response = await apiClient.get<{ paymentMethods: PaymentMethodWidget }>('/api/trust/payment-methods');
      return response.data.paymentMethods;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Hook to fetch trust indicators
 */
export function useTrustIndicators() {
  return useQuery({
    queryKey: ['trust-indicators'],
    queryFn: async () => {
      const response = await apiClient.get<{ trustIndicators: TrustIndicator[] }>('/api/trust/indicators');
      return response.data.trustIndicators;
    },
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
}

/**
 * Hook to fetch customer testimonials
 */
export function useCustomerTestimonials() {
  return useQuery({
    queryKey: ['customer-testimonials'],
    queryFn: async () => {
      const response = await apiClient.get<{ testimonials: CustomerTestimonial[] }>('/api/trust/testimonials');
      return response.data.testimonials;
    },
    staleTime: 30 * 60 * 1000, // 30 minutes
  });
}

/**
 * Hook to fetch Razorpay trust elements
 */
export function useRazorpayTrustElements() {
  return useQuery({
    queryKey: ['razorpay-trust-elements'],
    queryFn: async () => {
      const response = await apiClient.get('/api/trust/razorpay-elements');
      return response.data;
    },
    staleTime: 60 * 60 * 1000, // 1 hour
  });
}

/**
 * Hook to fetch dynamic trust score
 */
export function useDynamicTrustScore() {
  return useQuery({
    queryKey: ['dynamic-trust-score'],
    queryFn: async () => {
      const response = await apiClient.get('/api/trust/score');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchInterval: 5 * 60 * 1000, // Refetch every 5 minutes
  });
}

/**
 * Hook to fetch payment security information
 */
export function usePaymentSecurityInfo() {
  return useQuery({
    queryKey: ['payment-security-info'],
    queryFn: async () => {
      const response = await apiClient.get('/api/trust/security-info');
      return response.data;
    },
    staleTime: 60 * 60 * 1000, // 1 hour
  });
}

/**
 * Hook to fetch refund policy widget
 */
export function useRefundPolicyWidget() {
  return useQuery({
    queryKey: ['refund-policy-widget'],
    queryFn: async () => {
      const response = await apiClient.get('/api/trust/refund-policy');
      return response.data;
    },
    staleTime: 60 * 60 * 1000, // 1 hour
  });
}
