'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Calendar,
  TrendingUp,
  Users,
  Clock,
  MoreHorizontal
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';

interface BookingData {
  date: string;
  bookings: number;
  completed: number;
  cancelled: number;
}

interface BookingStats {
  totalBookings: number;
  completionRate: number;
  averageDaily: number;
  weeklyGrowth: number;
}

export function BookingsChart() {
  const [data, setData] = useState<BookingData[]>([]);
  const [stats, setStats] = useState<BookingStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [viewType, setViewType] = useState<'daily' | 'weekly'>('daily');

  useEffect(() => {
    const fetchBookingsData = async () => {
      setIsLoading(true);
      
      // Mock data - replace with actual API call
      const mockData: BookingData[] = [
        { date: 'Mon', bookings: 45, completed: 42, cancelled: 3 },
        { date: 'Tue', bookings: 52, completed: 48, cancelled: 4 },
        { date: 'Wed', bookings: 38, completed: 35, cancelled: 3 },
        { date: 'Thu', bookings: 61, completed: 57, cancelled: 4 },
        { date: 'Fri', bookings: 73, completed: 68, cancelled: 5 },
        { date: 'Sat', bookings: 89, completed: 84, cancelled: 5 },
        { date: 'Sun', bookings: 67, completed: 62, cancelled: 5 },
      ];

      const totalBookings = mockData.reduce((sum, item) => sum + item.bookings, 0);
      const totalCompleted = mockData.reduce((sum, item) => sum + item.completed, 0);
      const completionRate = (totalCompleted / totalBookings) * 100;
      const averageDaily = totalBookings / mockData.length;
      
      // Mock weekly growth
      const weeklyGrowth = 12.5;

      const mockStats: BookingStats = {
        totalBookings,
        completionRate,
        averageDaily,
        weeklyGrowth,
      };

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 800));
      setData(mockData);
      setStats(mockStats);
      setIsLoading(false);
    };

    fetchBookingsData();
  }, [viewType]);

  const maxBookings = Math.max(...data.map(d => d.bookings));

  if (isLoading) {
    return (
      <NeumorphicCard className="p-6">
        <div className="animate-pulse">
          <div className="flex items-center justify-between mb-6">
            <div className="h-6 bg-neutral-200 rounded w-1/3"></div>
            <div className="h-8 bg-neutral-200 rounded w-20"></div>
          </div>
          <div className="grid grid-cols-3 gap-4 mb-6">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="space-y-2">
                <div className="h-4 bg-neutral-200 rounded w-2/3"></div>
                <div className="h-6 bg-neutral-200 rounded w-1/2"></div>
              </div>
            ))}
          </div>
          <div className="h-64 bg-neutral-200 rounded"></div>
        </div>
      </NeumorphicCard>
    );
  }

  return (
    <NeumorphicCard className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-semibold text-neutral-900 mb-1">
            Bookings Overview
          </h2>
          <p className="text-neutral-600 text-sm">
            Daily booking trends and completion rates
          </p>
        </div>
        
        <div className="flex items-center gap-2">
          <div className="flex bg-neutral-100 rounded-lg p-1">
            <button
              onClick={() => setViewType('daily')}
              className={`px-3 py-1 text-sm rounded-md transition-colors duration-200 ${
                viewType === 'daily'
                  ? 'bg-white text-neutral-900 shadow-sm'
                  : 'text-neutral-600 hover:text-neutral-900'
              }`}
            >
              Daily
            </button>
            <button
              onClick={() => setViewType('weekly')}
              className={`px-3 py-1 text-sm rounded-md transition-colors duration-200 ${
                viewType === 'weekly'
                  ? 'bg-white text-neutral-900 shadow-sm'
                  : 'text-neutral-600 hover:text-neutral-900'
              }`}
            >
              Weekly
            </button>
          </div>
          
          <NeumorphicButton variant="ghost" size="sm">
            <MoreHorizontal className="w-4 h-4" />
          </NeumorphicButton>
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="bg-neutral-50 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <Calendar className="w-4 h-4 text-blue-600" />
              <span className="text-sm font-medium text-neutral-700">Total Bookings</span>
            </div>
            <div className="text-2xl font-bold text-neutral-900">
              {stats.totalBookings}
            </div>
            <div className="text-xs text-neutral-500 mt-1">
              This week
            </div>
          </div>
          
          <div className="bg-neutral-50 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <Users className="w-4 h-4 text-green-600" />
              <span className="text-sm font-medium text-neutral-700">Completion Rate</span>
            </div>
            <div className="text-2xl font-bold text-neutral-900">
              {stats.completionRate.toFixed(1)}%
            </div>
            <div className="text-xs text-neutral-500 mt-1">
              {stats.weeklyGrowth > 0 ? '+' : ''}{stats.weeklyGrowth}% vs last week
            </div>
          </div>
          
          <div className="bg-neutral-50 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <Clock className="w-4 h-4 text-purple-600" />
              <span className="text-sm font-medium text-neutral-700">Daily Average</span>
            </div>
            <div className="text-2xl font-bold text-neutral-900">
              {Math.round(stats.averageDaily)}
            </div>
            <div className="text-xs text-neutral-500 mt-1">
              Bookings per day
            </div>
          </div>
        </div>
      )}

      {/* Stacked Bar Chart */}
      <div className="relative">
        <div className="flex items-end justify-between h-64 gap-3">
          {data.map((item, index) => (
            <div key={item.date} className="flex-1 flex flex-col items-center">
              <div className="w-full relative group cursor-pointer">
                {/* Completed bookings */}
                <motion.div
                  initial={{ height: 0 }}
                  animate={{ height: `${(item.completed / maxBookings) * 240}px` }}
                  transition={{ duration: 0.8, delay: index * 0.1 }}
                  className="w-full bg-gradient-to-t from-green-500 to-green-400 rounded-t-lg"
                />
                
                {/* Cancelled bookings */}
                <motion.div
                  initial={{ height: 0 }}
                  animate={{ height: `${(item.cancelled / maxBookings) * 240}px` }}
                  transition={{ duration: 0.8, delay: index * 0.1 + 0.2 }}
                  className="w-full bg-gradient-to-t from-red-500 to-red-400"
                />
                
                {/* Tooltip */}
                <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none z-10">
                  <div className="bg-neutral-900 text-white text-xs rounded-lg px-3 py-2 whitespace-nowrap">
                    <div className="font-medium">{item.bookings} total</div>
                    <div className="text-green-300">{item.completed} completed</div>
                    <div className="text-red-300">{item.cancelled} cancelled</div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
        
        {/* X-axis labels */}
        <div className="flex justify-between mt-4 text-sm text-neutral-600">
          {data.map((item) => (
            <span key={item.date} className="flex-1 text-center">
              {item.date}
            </span>
          ))}
        </div>
      </div>

      {/* Legend */}
      <div className="flex items-center justify-center gap-6 mt-6 pt-4 border-t border-neutral-200">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-green-500 rounded-full"></div>
          <span className="text-sm text-neutral-600">Completed</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-red-500 rounded-full"></div>
          <span className="text-sm text-neutral-600">Cancelled</span>
        </div>
      </div>

      <div className="mt-4">
        <button className="text-primary-600 text-sm font-medium hover:text-primary-700 transition-colors duration-200">
          View detailed booking analytics →
        </button>
      </div>
    </NeumorphicCard>
  );
}
