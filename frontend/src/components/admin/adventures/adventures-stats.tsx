'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  MapPin, 
  Users, 
  Star, 
  TrendingUp, 
  Calendar,
  DollarSign,
  Eye,
  Heart
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';

interface AdventureStats {
  totalAdventures: number;
  activeAdventures: number;
  draftAdventures: number;
  totalBookings: number;
  totalRevenue: number;
  averageRating: number;
  totalViews: number;
  totalFavorites: number;
  monthlyGrowth: {
    adventures: number;
    bookings: number;
    revenue: number;
    rating: number;
  };
  topCategories: Array<{
    name: string;
    count: number;
    percentage: number;
  }>;
  topLocations: Array<{
    name: string;
    count: number;
    percentage: number;
  }>;
}

export function AdventuresStats() {
  const [stats, setStats] = useState<AdventureStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      setIsLoading(true);
      setError(null);

      // Mock data for now - replace with actual API call
      const mockStats: AdventureStats = {
        totalAdventures: 47,
        activeAdventures: 42,
        draftAdventures: 5,
        totalBookings: 1247,
        totalRevenue: 2847500,
        averageRating: 4.7,
        totalViews: 15420,
        totalFavorites: 892,
        monthlyGrowth: {
          adventures: 12.5,
          bookings: 18.3,
          revenue: 22.1,
          rating: 2.4,
        },
        topCategories: [
          { name: 'Heritage Walk', count: 12, percentage: 25.5 },
          { name: 'Food Trail', count: 10, percentage: 21.3 },
          { name: 'Adventure Sports', count: 8, percentage: 17.0 },
          { name: 'Nature Trek', count: 7, percentage: 14.9 },
          { name: 'Cultural Tour', count: 6, percentage: 12.8 },
        ],
        topLocations: [
          { name: 'Mumbai', count: 15, percentage: 31.9 },
          { name: 'Delhi', count: 12, percentage: 25.5 },
          { name: 'Goa', count: 8, percentage: 17.0 },
          { name: 'Rajasthan', count: 7, percentage: 14.9 },
          { name: 'Kerala', count: 5, percentage: 10.6 },
        ],
      };

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      setStats(mockStats);
    } catch (err) {
      console.error('Failed to fetch adventure stats:', err);
      setError('Failed to load statistics');
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {[...Array(8)].map((_, i) => (
          <NeumorphicCard key={i} className="p-6">
            <div className="animate-pulse">
              <div className="w-8 h-8 bg-neutral-200 rounded-lg mb-4"></div>
              <div className="h-8 bg-neutral-200 rounded mb-2"></div>
              <div className="h-4 bg-neutral-200 rounded w-2/3"></div>
            </div>
          </NeumorphicCard>
        ))}
      </div>
    );
  }

  if (error || !stats) {
    return (
      <NeumorphicCard className="p-6 text-center">
        <p className="text-red-600">{error || 'Failed to load statistics'}</p>
        <button 
          onClick={fetchStats}
          className="mt-4 px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600"
        >
          Retry
        </button>
      </NeumorphicCard>
    );
  }

  const statCards = [
    {
      title: 'Total Adventures',
      value: stats.totalAdventures,
      change: stats.monthlyGrowth.adventures,
      icon: MapPin,
      color: 'text-blue-500',
      bgColor: 'bg-blue-100',
    },
    {
      title: 'Total Bookings',
      value: stats.totalBookings.toLocaleString(),
      change: stats.monthlyGrowth.bookings,
      icon: Calendar,
      color: 'text-green-500',
      bgColor: 'bg-green-100',
    },
    {
      title: 'Total Revenue',
      value: `₹${(stats.totalRevenue / 100000).toFixed(1)}L`,
      change: stats.monthlyGrowth.revenue,
      icon: DollarSign,
      color: 'text-purple-500',
      bgColor: 'bg-purple-100',
    },
    {
      title: 'Average Rating',
      value: stats.averageRating.toFixed(1),
      change: stats.monthlyGrowth.rating,
      icon: Star,
      color: 'text-yellow-500',
      bgColor: 'bg-yellow-100',
    },
    {
      title: 'Active Adventures',
      value: stats.activeAdventures,
      change: null,
      icon: TrendingUp,
      color: 'text-emerald-500',
      bgColor: 'bg-emerald-100',
    },
    {
      title: 'Draft Adventures',
      value: stats.draftAdventures,
      change: null,
      icon: Eye,
      color: 'text-orange-500',
      bgColor: 'bg-orange-100',
    },
    {
      title: 'Total Views',
      value: stats.totalViews.toLocaleString(),
      change: null,
      icon: Eye,
      color: 'text-indigo-500',
      bgColor: 'bg-indigo-100',
    },
    {
      title: 'Total Favorites',
      value: stats.totalFavorites.toLocaleString(),
      change: null,
      icon: Heart,
      color: 'text-red-500',
      bgColor: 'bg-red-100',
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
                {stat.change !== null && (
                  <div className={`flex items-center text-sm font-medium ${
                    stat.change > 0 ? 'text-green-600' : 'text-red-600'
                  }`}>
                    <TrendingUp className={`w-4 h-4 mr-1 ${
                      stat.change < 0 ? 'rotate-180' : ''
                    }`} />
                    {Math.abs(stat.change)}%
                  </div>
                )}
              </div>
              <div>
                <h3 className="text-2xl font-bold text-neutral-900 mb-1">
                  {stat.value}
                </h3>
                <p className="text-neutral-600 text-sm">
                  {stat.title}
                </p>
              </div>
            </NeumorphicCard>
          </motion.div>
        ))}
      </div>

      {/* Category and Location Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Top Categories */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.8 }}
        >
          <NeumorphicCard className="p-6">
            <h3 className="text-lg font-semibold text-neutral-900 mb-6">
              Top Categories
            </h3>
            <div className="space-y-4">
              {stats.topCategories.map((category, index) => (
                <div key={category.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center text-xs font-medium text-primary-600">
                      {index + 1}
                    </div>
                    <span className="font-medium text-neutral-800">
                      {category.name}
                    </span>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="w-24 h-2 bg-neutral-200 rounded-full overflow-hidden">
                      <div 
                        className="h-full bg-primary-500 rounded-full transition-all duration-500"
                        style={{ width: `${category.percentage}%` }}
                      />
                    </div>
                    <span className="text-sm text-neutral-600 w-8 text-right">
                      {category.count}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </NeumorphicCard>
        </motion.div>

        {/* Top Locations */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 1.0 }}
        >
          <NeumorphicCard className="p-6">
            <h3 className="text-lg font-semibold text-neutral-900 mb-6">
              Top Locations
            </h3>
            <div className="space-y-4">
              {stats.topLocations.map((location, index) => (
                <div key={location.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center text-xs font-medium text-green-600">
                      {index + 1}
                    </div>
                    <span className="font-medium text-neutral-800">
                      {location.name}
                    </span>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="w-24 h-2 bg-neutral-200 rounded-full overflow-hidden">
                      <div 
                        className="h-full bg-green-500 rounded-full transition-all duration-500"
                        style={{ width: `${location.percentage}%` }}
                      />
                    </div>
                    <span className="text-sm text-neutral-600 w-8 text-right">
                      {location.count}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </NeumorphicCard>
        </motion.div>
      </div>
    </div>
  );
}
