'use client';

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Bell, 
  X, 
  Check, 
  Calendar, 
  CreditCard, 
  MapPin, 
  Gift,
  AlertCircle,
  Info,
  CheckCircle,
  Clock,
  Trash2,
  Settings
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { useNotifications } from '@/hooks/use-notifications';
import { formatRelativeTime } from '@/lib/utils';

interface Notification {
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

const notificationIcons = {
  booking: Calendar,
  payment: CreditCard,
  reminder: Clock,
  offer: Gift,
  system: Info,
  chat: Bell,
};

const priorityColors = {
  low: 'text-neutral-600',
  medium: 'text-warning-600',
  high: 'text-error-600',
};

interface NotificationCenterProps {
  isOpen: boolean;
  onClose: () => void;
}

export function NotificationCenter({ isOpen, onClose }: NotificationCenterProps) {
  const [filter, setFilter] = useState<'all' | 'unread' | 'important'>('all');
  const { 
    notifications, 
    unreadCount, 
    markAsRead, 
    markAllAsRead, 
    deleteNotification,
    isLoading 
  } = useNotifications();

  const filteredNotifications = notifications.filter(notification => {
    if (filter === 'unread') return !notification.read;
    if (filter === 'important') return notification.priority === 'high';
    return true;
  });

  const handleNotificationClick = (notification: Notification) => {
    if (!notification.read) {
      markAsRead(notification.id);
    }
    
    if (notification.actionUrl) {
      window.location.href = notification.actionUrl;
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/20 backdrop-blur-sm z-40"
            onClick={onClose}
          />

          {/* Notification Panel */}
          <motion.div
            initial={{ opacity: 0, x: 400 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 400 }}
            transition={{ duration: 0.3 }}
            className="fixed top-0 right-0 h-full w-96 bg-white shadow-neumorphic-elevated z-50 flex flex-col"
          >
            {/* Header */}
            <div className="p-6 border-b border-neutral-200">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-xl font-semibold text-neutral-800">
                    Notifications
                  </h2>
                  {unreadCount > 0 && (
                    <p className="text-sm text-neutral-600">
                      {unreadCount} unread notification{unreadCount !== 1 ? 's' : ''}
                    </p>
                  )}
                </div>
                
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => markAllAsRead()}
                    className="p-2 text-neutral-600 hover:text-neutral-800 hover:bg-neutral-100 rounded-full transition-colors duration-200"
                    title="Mark all as read"
                  >
                    <Check className="w-4 h-4" />
                  </button>
                  <button
                    className="p-2 text-neutral-600 hover:text-neutral-800 hover:bg-neutral-100 rounded-full transition-colors duration-200"
                    title="Notification settings"
                  >
                    <Settings className="w-4 h-4" />
                  </button>
                  <button
                    onClick={onClose}
                    className="p-2 text-neutral-600 hover:text-neutral-800 hover:bg-neutral-100 rounded-full transition-colors duration-200"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Filter Tabs */}
              <div className="flex gap-2">
                {[
                  { key: 'all', label: 'All' },
                  { key: 'unread', label: 'Unread' },
                  { key: 'important', label: 'Important' },
                ].map((tab) => (
                  <button
                    key={tab.key}
                    onClick={() => setFilter(tab.key as any)}
                    className={`px-3 py-1 text-sm rounded-full transition-colors duration-200 ${
                      filter === tab.key
                        ? 'bg-primary-500 text-white'
                        : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                    }`}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Notifications List */}
            <div className="flex-1 overflow-y-auto">
              {isLoading ? (
                <div className="p-6 space-y-4">
                  {[...Array(5)].map((_, i) => (
                    <div key={i} className="animate-pulse">
                      <div className="flex items-start gap-3">
                        <div className="w-10 h-10 bg-neutral-200 rounded-full"></div>
                        <div className="flex-1 space-y-2">
                          <div className="h-4 bg-neutral-200 rounded w-3/4"></div>
                          <div className="h-3 bg-neutral-200 rounded w-1/2"></div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : filteredNotifications.length === 0 ? (
                <div className="p-6 text-center">
                  <Bell className="w-12 h-12 text-neutral-300 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-neutral-800 mb-2">
                    No notifications
                  </h3>
                  <p className="text-neutral-600">
                    {filter === 'unread' 
                      ? "You're all caught up!" 
                      : "We'll notify you when something important happens."}
                  </p>
                </div>
              ) : (
                <div className="p-4 space-y-2">
                  {filteredNotifications.map((notification) => {
                    const IconComponent = notificationIcons[notification.type];
                    
                    return (
                      <motion.div
                        key={notification.id}
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -20 }}
                        transition={{ duration: 0.2 }}
                      >
                        <NeumorphicCard
                          interactive
                          className={`group cursor-pointer transition-all duration-200 ${
                            !notification.read 
                              ? 'bg-primary-50 border-primary-200' 
                              : 'hover:bg-neutral-50'
                          }`}
                          onClick={() => handleNotificationClick(notification)}
                        >
                          <div className="flex items-start gap-3">
                            {/* Icon */}
                            <div className={`p-2 rounded-full ${
                              !notification.read 
                                ? 'bg-primary-100' 
                                : 'bg-neutral-100'
                            }`}>
                              <IconComponent className={`w-4 h-4 ${
                                !notification.read 
                                  ? 'text-primary-600' 
                                  : 'text-neutral-600'
                              }`} />
                            </div>

                            {/* Content */}
                            <div className="flex-1 min-w-0">
                              <div className="flex items-start justify-between gap-2">
                                <h4 className={`font-medium text-sm ${
                                  !notification.read 
                                    ? 'text-neutral-900' 
                                    : 'text-neutral-800'
                                }`}>
                                  {notification.title}
                                </h4>
                                
                                {/* Priority Indicator */}
                                {notification.priority === 'high' && (
                                  <AlertCircle className="w-4 h-4 text-error-500 shrink-0" />
                                )}
                              </div>
                              
                              <p className="text-sm text-neutral-600 mt-1 line-clamp-2">
                                {notification.message}
                              </p>
                              
                              <div className="flex items-center justify-between mt-2">
                                <span className="text-xs text-neutral-500">
                                  {formatRelativeTime(notification.timestamp)}
                                </span>
                                
                                {notification.actionText && (
                                  <span className="text-xs text-primary-600 font-medium">
                                    {notification.actionText}
                                  </span>
                                )}
                              </div>
                            </div>

                            {/* Actions */}
                            <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                              {!notification.read && (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    markAsRead(notification.id);
                                  }}
                                  className="p-1 text-neutral-500 hover:text-primary-600 transition-colors duration-200"
                                  title="Mark as read"
                                >
                                  <CheckCircle className="w-4 h-4" />
                                </button>
                              )}
                              
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  deleteNotification(notification.id);
                                }}
                                className="p-1 text-neutral-500 hover:text-error-600 transition-colors duration-200"
                                title="Delete notification"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </div>
                          </div>
                        </NeumorphicCard>
                      </motion.div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Footer */}
            {filteredNotifications.length > 0 && (
              <div className="p-4 border-t border-neutral-200">
                <NeumorphicButton
                  variant="ghost"
                  size="sm"
                  className="w-full"
                  onClick={() => markAllAsRead()}
                >
                  Mark All as Read
                </NeumorphicButton>
              </div>
            )}
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
