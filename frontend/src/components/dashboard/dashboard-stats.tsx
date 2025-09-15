'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  Calendar,
  MapPin,
  Star,
  TrendingUp,
  Clock,
  Users,
  Award,
  Heart
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { getUserDashboard, initializeApiClient } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';

interface UserStats {
  totalBookings: number;
  completedAdventures: number;
  upcomingBookings: number;
  totalSpent: number;
  favoriteAdventures: number;
  averageRating: number;
  memberSince: string;
  loyaltyPoints: number;
  achievements: Array<{
    id: string;
    title: string;
    description: string;
    icon: string;
    unlockedAt: string;
  }>;
}

export function DashboardStats() {
  const [stats, setStats] = useState<UserStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { user } = useAuth();

  useEffect(() => {
    const fetchUserStats = async () => {
      if (!user?.referenceId) {
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        // Initialize API client with stored token
        initializeApiClient();

        // Get real dashboard data
        const dashboardData = await getUserDashboard(user.referenceId);

        // Transform API data to component format
        const transformedStats: UserStats = {
          totalBookings: dashboardData.stats?.totalBookings || 0,
          completedAdventures: dashboardData.stats?.completedAdventures || 0,
          upcomingBookings: dashboardData.stats?.upcomingBookings || 0,
          totalSpent: dashboardData.stats?.totalSpent || 0,
          favoriteAdventures: dashboardData.stats?.favoriteAdventures || 0,
          averageRating: dashboardData.stats?.averageRating || 0,
          memberSince: dashboardData.stats?.memberSince ? `${dashboardData.stats.memberSince}` : 'Recently',
          loyaltyPoints: dashboardData.stats?.loyaltyPoints || 0,
          achievements: [], // TODO: Add achievements when available
        };

        setStats(transformedStats);
      } catch (err) {
        console.error('Failed to fetch dashboard stats:', err);
        setError('Failed to load dashboard data');

        // Fallback to basic stats
        const fallbackStats: UserStats = {
          totalBookings: 0,
          completedAdventures: 0,
          upcomingBookings: 0,
          totalSpent: 0,
          favoriteAdventures: 0,
          averageRating: 0,
          memberSince: 'Recently',
          loyaltyPoints: 0,
          achievements: [],
        };
        setStats(fallbackStats);
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserStats();
  }, [user?.referenceId]);

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {[...Array(8)].map((_, index) => (
          <NeumorphicCard key={index} className="p-6">
            <div className="animate-pulse">
              <div className="flex items-center justify-between mb-4">
                <div className="w-12 h-12 bg-neutral-200 rounded-neumorphic"></div>
                <div className="w-16 h-4 bg-neutral-200 rounded"></div>
              </div>
              <div className="space-y-2">
                <div className="w-20 h-8 bg-neutral-200 rounded"></div>
                <div className="w-24 h-4 bg-neutral-200 rounded"></div>
              </div>
            </div>
          </NeumorphicCard>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <NeumorphicCard className="p-6">
        <div className="text-center text-red-600">
          <p>{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="mt-2 text-sm text-primary-600 hover:text-primary-500"
          >
            Try again
          </button>
        </div>
      </NeumorphicCard>
    );
  }

  if (!stats) {
    return (
      <div className="text-center py-8">
        <p className="text-neutral-600">Failed to load dashboard statistics</p>
      </div>
    );
  }

  const statCards = [
    {
      title: 'Total Bookings',
      value: stats.totalBookings,
      icon: Calendar,
      color: 'text-blue-500',
      bgColor: 'bg-blue-100',
      description: 'All time bookings',
    },
    {
      title: 'Completed Adventures',
      value: stats.completedAdventures,
      icon: MapPin,
      color: 'text-green-500',
      bgColor: 'bg-green-100',
      description: 'Successfully completed',
    },
    {
      title: 'Upcoming Bookings',
      value: stats.upcomingBookings,
      icon: Clock,
      color: 'text-orange-500',
      bgColor: 'bg-orange-100',
      description: 'Scheduled adventures',
    },
    {
      title: 'Total Spent',
      value: `₹${(stats.totalSpent / 1000).toFixed(1)}K`,
      icon: TrendingUp,
      color: 'text-purple-500',
      bgColor: 'bg-purple-100',
      description: 'Lifetime spending',
    },
    {
      title: 'Favorite Adventures',
      value: stats.favoriteAdventures,
      icon: Heart,
      color: 'text-red-500',
      bgColor: 'bg-red-100',
      description: 'Saved to favorites',
    },
    {
      title: 'Average Rating',
      value: stats.averageRating.toFixed(1),
      icon: Star,
      color: 'text-yellow-500',
      bgColor: 'bg-yellow-100',
      description: 'Your ratings given',
    },
    {
      title: 'Loyalty Points',
      value: stats.loyaltyPoints.toLocaleString(),
      icon: Award,
      color: 'text-indigo-500',
      bgColor: 'bg-indigo-100',
      description: 'Reward points earned',
    },
    {
      title: 'Member Since',
      value: stats.memberSince,
      icon: Users,
      color: 'text-emerald-500',
      bgColor: 'bg-emerald-100',
      description: 'Join date',
    },
  ];

  return (
    <div className="space-y-8">
      {/* Main Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((stat, index) => (
          <motion.div
            key={stat.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: index * 0.1 }}
          >
            <NeumorphicCard className="p-6 hover:shadow-lg transition-shadow duration-300">
              <div className="flex items-center justify-between mb-4">
                <div className={`w-12 h-12 ${stat.bgColor} rounded-neumorphic flex items-center justify-center`}>
                  <stat.icon className={`w-6 h-6 ${stat.color}`} />
                </div>
              </div>
              
              <div>
                <h3 className="text-2xl font-bold text-neutral-900 mb-1">
                  {stat.value}
                </h3>
                <p className="text-neutral-600 text-sm mb-1">
                  {stat.title}
                </p>
                <p className="text-neutral-500 text-xs">
                  {stat.description}
                </p>
              </div>
            </NeumorphicCard>
          </motion.div>
        ))}
      </div>

      {/* Achievements Section */}
      {stats.achievements.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.8 }}
        >
          <NeumorphicCard className="p-6">
            <h3 className="text-lg font-semibold text-neutral-900 mb-6">
              Recent Achievements
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {stats.achievements.map((achievement, index) => (
                <motion.div
                  key={achievement.id}
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ duration: 0.3, delay: 0.9 + index * 0.1 }}
                  className="flex items-center gap-3 p-4 bg-gradient-to-r from-primary-50 to-primary-100 rounded-lg border border-primary-200"
                >
                  <div className="text-2xl">{achievement.icon}</div>
                  <div className="flex-1">
                    <h4 className="font-medium text-neutral-900">
                      {achievement.title}
                    </h4>
                    <p className="text-sm text-neutral-600">
                      {achievement.description}
                    </p>
                    <p className="text-xs text-neutral-500 mt-1">
                      Unlocked {new Date(achievement.unlockedAt).toLocaleDateString()}
                    </p>
                  </div>
                </motion.div>
              ))}
            </div>
          </NeumorphicCard>
        </motion.div>
      )}

      {/* Progress Indicators */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 1.0 }}
      >
        <NeumorphicCard className="p-6">
          <h3 className="text-lg font-semibold text-neutral-900 mb-6">
            Your Progress
          </h3>
          <div className="space-y-4">
            <div>
              <div className="flex justify-between text-sm mb-2">
                <span className="text-neutral-600">Adventure Completion Rate</span>
                <span className="font-medium text-neutral-900">
                  {Math.round((stats.completedAdventures / stats.totalBookings) * 100)}%
                </span>
              </div>
              <div className="w-full h-2 bg-neutral-200 rounded-full overflow-hidden">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: `${(stats.completedAdventures / stats.totalBookings) * 100}%` }}
                  transition={{ duration: 1, delay: 1.2 }}
                  className="h-full bg-gradient-to-r from-green-400 to-green-500 rounded-full"
                />
              </div>
            </div>

            <div>
              <div className="flex justify-between text-sm mb-2">
                <span className="text-neutral-600">Next Loyalty Tier</span>
                <span className="font-medium text-neutral-900">
                  {stats.loyaltyPoints}/2000 points
                </span>
              </div>
              <div className="w-full h-2 bg-neutral-200 rounded-full overflow-hidden">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: `${(stats.loyaltyPoints / 2000) * 100}%` }}
                  transition={{ duration: 1, delay: 1.4 }}
                  className="h-full bg-gradient-to-r from-purple-400 to-purple-500 rounded-full"
                />
              </div>
              <p className="text-xs text-neutral-500 mt-1">
                {2000 - stats.loyaltyPoints} points to Gold tier
              </p>
            </div>
          </div>
        </NeumorphicCard>
      </motion.div>
    </div>
  );
}
