'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Calendar,
  User,
  MapPin,
  DollarSign,
  Star,
  MessageSquare,
  UserPlus,
  Edit,
  Trash2,
  Clock
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';

interface Activity {
  id: string;
  type: 'booking' | 'review' | 'user_signup' | 'adventure_created' | 'adventure_updated' | 'payment' | 'cancellation';
  title: string;
  description: string;
  user: {
    name: string;
    avatar?: string;
  };
  timestamp: string;
  metadata?: {
    amount?: number;
    rating?: number;
    adventureName?: string;
  };
}

export function RecentActivity() {
  const [activities, setActivities] = useState<Activity[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchActivities = async () => {
      setIsLoading(true);
      
      // Mock data - replace with actual API call
      const mockActivities: Activity[] = [
        {
          id: '1',
          type: 'booking',
          title: 'New Booking',
          description: 'Mumbai Heritage Walk',
          user: { name: 'Priya Sharma' },
          timestamp: '2 minutes ago',
          metadata: { amount: 1500, adventureName: 'Mumbai Heritage Walk' },
        },
        {
          id: '2',
          type: 'review',
          title: 'New Review',
          description: 'Left a 5-star review for Delhi Food Trail',
          user: { name: 'Rahul Kumar' },
          timestamp: '15 minutes ago',
          metadata: { rating: 5, adventureName: 'Delhi Food Trail' },
        },
        {
          id: '3',
          type: 'user_signup',
          title: 'New User Registration',
          description: 'Joined the platform',
          user: { name: 'Anita Desai' },
          timestamp: '1 hour ago',
        },
        {
          id: '4',
          type: 'payment',
          title: 'Payment Received',
          description: 'Payment for Goa Beach Adventure',
          user: { name: 'Vikram Singh' },
          timestamp: '2 hours ago',
          metadata: { amount: 2500, adventureName: 'Goa Beach Adventure' },
        },
        {
          id: '5',
          type: 'adventure_created',
          title: 'Adventure Created',
          description: 'Created "Rajasthan Desert Safari"',
          user: { name: 'Admin' },
          timestamp: '3 hours ago',
          metadata: { adventureName: 'Rajasthan Desert Safari' },
        },
        {
          id: '6',
          type: 'cancellation',
          title: 'Booking Cancelled',
          description: 'Cancelled Kerala Backwaters Tour',
          user: { name: 'Meera Nair' },
          timestamp: '4 hours ago',
          metadata: { adventureName: 'Kerala Backwaters Tour' },
        },
        {
          id: '7',
          type: 'adventure_updated',
          title: 'Adventure Updated',
          description: 'Updated pricing for Mumbai Heritage Walk',
          user: { name: 'Admin' },
          timestamp: '5 hours ago',
          metadata: { adventureName: 'Mumbai Heritage Walk' },
        },
        {
          id: '8',
          type: 'review',
          title: 'New Review',
          description: 'Left a 4-star review for Goa Beach Adventure',
          user: { name: 'Suresh Patel' },
          timestamp: '6 hours ago',
          metadata: { rating: 4, adventureName: 'Goa Beach Adventure' },
        },
      ];

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      setActivities(mockActivities);
      setIsLoading(false);
    };

    fetchActivities();
  }, []);

  const getActivityIcon = (type: Activity['type']) => {
    switch (type) {
      case 'booking':
        return Calendar;
      case 'review':
        return Star;
      case 'user_signup':
        return UserPlus;
      case 'adventure_created':
        return MapPin;
      case 'adventure_updated':
        return Edit;
      case 'payment':
        return DollarSign;
      case 'cancellation':
        return Trash2;
      default:
        return Clock;
    }
  };

  const getActivityColor = (type: Activity['type']) => {
    switch (type) {
      case 'booking':
        return 'text-green-600 bg-green-100';
      case 'review':
        return 'text-yellow-600 bg-yellow-100';
      case 'user_signup':
        return 'text-blue-600 bg-blue-100';
      case 'adventure_created':
        return 'text-purple-600 bg-purple-100';
      case 'adventure_updated':
        return 'text-indigo-600 bg-indigo-100';
      case 'payment':
        return 'text-emerald-600 bg-emerald-100';
      case 'cancellation':
        return 'text-red-600 bg-red-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  if (isLoading) {
    return (
      <NeumorphicCard className="p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-neutral-200 rounded w-1/3 mb-4"></div>
          <div className="space-y-4">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="flex gap-4">
                <div className="w-10 h-10 bg-neutral-200 rounded-full"></div>
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-neutral-200 rounded w-3/4"></div>
                  <div className="h-3 bg-neutral-200 rounded w-1/2"></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </NeumorphicCard>
    );
  }

  return (
    <NeumorphicCard className="p-6">
      <div className="mb-6">
        <h2 className="text-xl font-semibold text-neutral-900 mb-2">
          Recent Activity
        </h2>
        <p className="text-neutral-600">
          Latest actions and events on your platform.
        </p>
      </div>

      <div className="space-y-4 max-h-96 overflow-y-auto">
        {activities.map((activity, index) => {
          const Icon = getActivityIcon(activity.type);
          const colorClasses = getActivityColor(activity.type);
          
          return (
            <motion.div
              key={activity.id}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.3, delay: index * 0.1 }}
              className="flex items-start gap-4 p-3 rounded-lg hover:bg-neutral-50 transition-colors duration-200"
            >
              <div className={`w-10 h-10 rounded-full flex items-center justify-center ${colorClasses}`}>
                <Icon className="w-5 h-5" />
              </div>
              
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <h4 className="font-medium text-neutral-900">
                      {activity.title}
                    </h4>
                    <p className="text-sm text-neutral-600 mt-1">
                      {activity.description}
                    </p>
                    <div className="flex items-center gap-2 mt-2 text-xs text-neutral-500">
                      <User className="w-3 h-3" />
                      <span>{activity.user.name}</span>
                      <span>•</span>
                      <span>{activity.timestamp}</span>
                    </div>
                  </div>
                  
                  {activity.metadata && (
                    <div className="text-right text-sm">
                      {activity.metadata.amount && (
                        <div className="font-medium text-green-600">
                          ₹{activity.metadata.amount.toLocaleString()}
                        </div>
                      )}
                      {activity.metadata.rating && (
                        <div className="flex items-center gap-1 text-yellow-600">
                          <Star className="w-3 h-3 fill-current" />
                          <span>{activity.metadata.rating}</span>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </motion.div>
          );
        })}
      </div>
      
      <div className="mt-4 pt-4 border-t border-neutral-200">
        <button className="text-primary-600 text-sm font-medium hover:text-primary-700 transition-colors duration-200">
          View all activity →
        </button>
      </div>
    </NeumorphicCard>
  );
}
