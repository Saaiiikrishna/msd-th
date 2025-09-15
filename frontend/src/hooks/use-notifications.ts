import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';

// Notification types
export interface Notification {
  id: string;
  type: 'booking' | 'payment' | 'reminder' | 'offer' | 'system' | 'chat';
  title: string;
  message: string;
  timestamp: Date;
  read: boolean;
  priority: 'low' | 'medium' | 'high';
  actionUrl?: string;
  actionText?: string;
  metadata?: {
    bookingId?: string;
    paymentId?: string;
    adventureId?: string;
    amount?: number;
  };
}

export interface NotificationPreferences {
  email: {
    bookingUpdates: boolean;
    paymentConfirmations: boolean;
    promotions: boolean;
    reminders: boolean;
  };
  push: {
    bookingUpdates: boolean;
    paymentConfirmations: boolean;
    promotions: boolean;
    reminders: boolean;
    chat: boolean;
  };
  sms: {
    bookingConfirmations: boolean;
    paymentConfirmations: boolean;
    emergencyAlerts: boolean;
  };
}

/**
 * Hook for managing notifications
 */
export function useNotifications() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const queryClient = useQueryClient();

  // Fetch notifications
  const { data, isLoading, error } = useQuery({
    queryKey: ['notifications'],
    queryFn: async () => {
      const response = await apiClient.get<Notification[]>('/api/notifications');
      return response.data;
    },
    staleTime: 30 * 1000, // 30 seconds
    refetchInterval: 60 * 1000, // Refetch every minute
  });

  // Update local state when data changes
  useEffect(() => {
    if (data) {
      setNotifications(data);
    }
  }, [data]);

  // Mark as read mutation
  const markAsReadMutation = useMutation({
    mutationFn: async (notificationId: string) => {
      await apiClient.patch(`/api/notifications/${notificationId}/read`);
    },
    onSuccess: (_, notificationId) => {
      setNotifications(prev =>
        prev.map(notification =>
          notification.id === notificationId
            ? { ...notification, read: true }
            : notification
        )
      );
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Mark all as read mutation
  const markAllAsReadMutation = useMutation({
    mutationFn: async () => {
      await apiClient.patch('/api/notifications/read-all');
    },
    onSuccess: () => {
      setNotifications(prev =>
        prev.map(notification => ({ ...notification, read: true }))
      );
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Delete notification mutation
  const deleteNotificationMutation = useMutation({
    mutationFn: async (notificationId: string) => {
      await apiClient.delete(`/api/notifications/${notificationId}`);
    },
    onSuccess: (_, notificationId) => {
      setNotifications(prev =>
        prev.filter(notification => notification.id !== notificationId)
      );
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Real-time notifications via WebSocket
  useEffect(() => {
    // Mock WebSocket connection for real-time notifications
    const ws = {
      onmessage: (event: any) => {
        const data = JSON.parse(event.data);
        
        if (data.type === 'notification') {
          setNotifications(prev => [data.notification, ...prev]);
          
          // Show browser notification if permission granted
          if (Notification.permission === 'granted') {
            new Notification(data.notification.title, {
              body: data.notification.message,
              icon: '/favicon.ico',
              badge: '/favicon.ico',
            });
          }
        }
      },
    };

    // Simulate real-time notifications
    const interval = setInterval(() => {
      if (Math.random() > 0.95) { // 5% chance every interval
        const mockNotification: Notification = {
          id: `mock-${Date.now()}`,
          type: 'system',
          title: 'New Adventure Available!',
          message: 'Check out our latest treasure hunt in Goa',
          timestamp: new Date(),
          read: false,
          priority: 'medium',
          actionUrl: '/adventures/goa-beach',
          actionText: 'View Adventure',
        };
        
        ws.onmessage({ data: JSON.stringify({ type: 'notification', notification: mockNotification }) });
      }
    }, 30000); // Every 30 seconds

    return () => clearInterval(interval);
  }, []);

  // Request notification permission
  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }, []);

  const unreadCount = notifications.filter(n => !n.read).length;

  return {
    notifications,
    unreadCount,
    isLoading,
    error,
    markAsRead: markAsReadMutation.mutate,
    markAllAsRead: markAllAsReadMutation.mutate,
    deleteNotification: deleteNotificationMutation.mutate,
  };
}

/**
 * Hook for notification preferences
 */
export function useNotificationPreferences() {
  const queryClient = useQueryClient();

  const { data: preferences, isLoading } = useQuery({
    queryKey: ['notification-preferences'],
    queryFn: async () => {
      const response = await apiClient.get<NotificationPreferences>('/api/notifications/preferences');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  const updatePreferencesMutation = useMutation({
    mutationFn: async (newPreferences: Partial<NotificationPreferences>) => {
      const response = await apiClient.patch('/api/notifications/preferences', newPreferences);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-preferences'] });
    },
  });

  return {
    preferences,
    isLoading,
    updatePreferences: updatePreferencesMutation.mutate,
    isUpdating: updatePreferencesMutation.isPending,
  };
}

/**
 * Hook for creating custom notifications
 */
export function useCreateNotification() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (notification: Omit<Notification, 'id' | 'timestamp' | 'read'>) => {
      const response = await apiClient.post('/api/notifications', notification);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
}

/**
 * Hook for notification analytics
 */
export function useNotificationAnalytics() {
  return useQuery({
    queryKey: ['notification-analytics'],
    queryFn: async () => {
      const response = await apiClient.get('/api/notifications/analytics');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Hook for push notification subscription
 */
export function usePushNotificationSubscription() {
  const queryClient = useQueryClient();

  const subscribeMutation = useMutation({
    mutationFn: async () => {
      if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        throw new Error('Push notifications not supported');
      }

      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: process.env.NEXT_PUBLIC_VAPID_PUBLIC_KEY,
      });

      await apiClient.post('/api/notifications/push-subscription', {
        subscription: subscription.toJSON(),
      });

      return subscription;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['push-subscription'] });
    },
  });

  const unsubscribeMutation = useMutation({
    mutationFn: async () => {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      
      if (subscription) {
        await subscription.unsubscribe();
        await apiClient.delete('/api/notifications/push-subscription');
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['push-subscription'] });
    },
  });

  const { data: isSubscribed } = useQuery({
    queryKey: ['push-subscription'],
    queryFn: async () => {
      if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        return false;
      }

      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      return !!subscription;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  return {
    isSubscribed,
    subscribe: subscribeMutation.mutate,
    unsubscribe: unsubscribeMutation.mutate,
    isSubscribing: subscribeMutation.isPending,
    isUnsubscribing: unsubscribeMutation.isPending,
  };
}
