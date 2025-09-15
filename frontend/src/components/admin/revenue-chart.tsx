'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  TrendingUp, 
  TrendingDown,
  DollarSign,
  Calendar,
  MoreHorizontal
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';

interface RevenueData {
  month: string;
  revenue: number;
  bookings: number;
}

interface ChartStats {
  totalRevenue: number;
  monthlyGrowth: number;
  averageBookingValue: number;
  totalBookings: number;
}

export function RevenueChart() {
  const [data, setData] = useState<RevenueData[]>([]);
  const [stats, setStats] = useState<ChartStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [timeRange, setTimeRange] = useState<'6m' | '12m' | '24m'>('12m');

  useEffect(() => {
    const fetchRevenueData = async () => {
      setIsLoading(true);
      
      // Mock data - replace with actual API call
      const mockData: RevenueData[] = [
        { month: 'Jan', revenue: 185000, bookings: 124 },
        { month: 'Feb', revenue: 220000, bookings: 147 },
        { month: 'Mar', revenue: 195000, bookings: 130 },
        { month: 'Apr', revenue: 275000, bookings: 183 },
        { month: 'May', revenue: 310000, bookings: 207 },
        { month: 'Jun', revenue: 285000, bookings: 190 },
        { month: 'Jul', revenue: 340000, bookings: 227 },
        { month: 'Aug', revenue: 365000, bookings: 243 },
        { month: 'Sep', revenue: 320000, bookings: 213 },
        { month: 'Oct', revenue: 380000, bookings: 253 },
        { month: 'Nov', revenue: 420000, bookings: 280 },
        { month: 'Dec', revenue: 450000, bookings: 300 },
      ];

      const totalRevenue = mockData.reduce((sum, item) => sum + item.revenue, 0);
      const totalBookings = mockData.reduce((sum, item) => sum + item.bookings, 0);
      const averageBookingValue = totalRevenue / totalBookings;
      
      // Calculate growth (comparing last month to previous month)
      const lastMonth = mockData[mockData.length - 1];
      const previousMonth = mockData[mockData.length - 2];
      const monthlyGrowth = ((lastMonth.revenue - previousMonth.revenue) / previousMonth.revenue) * 100;

      const mockStats: ChartStats = {
        totalRevenue,
        monthlyGrowth,
        averageBookingValue,
        totalBookings,
      };

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 800));
      setData(mockData);
      setStats(mockStats);
      setIsLoading(false);
    };

    fetchRevenueData();
  }, [timeRange]);

  const maxRevenue = Math.max(...data.map(d => d.revenue));

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
            Revenue Overview
          </h2>
          <p className="text-neutral-600 text-sm">
            Monthly revenue and booking trends
          </p>
        </div>
        
        <div className="flex items-center gap-2">
          <select
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value as '6m' | '12m' | '24m')}
            className="text-sm border border-neutral-200 rounded-lg px-3 py-1 bg-white focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="6m">Last 6 months</option>
            <option value="12m">Last 12 months</option>
            <option value="24m">Last 24 months</option>
          </select>
          
          <NeumorphicButton variant="ghost" size="sm">
            <MoreHorizontal className="w-4 h-4" />
          </NeumorphicButton>
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="bg-neutral-50 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <DollarSign className="w-4 h-4 text-green-600" />
              <span className="text-sm font-medium text-neutral-700">Total Revenue</span>
            </div>
            <div className="text-2xl font-bold text-neutral-900">
              ₹{(stats.totalRevenue / 100000).toFixed(1)}L
            </div>
          </div>
          
          <div className="bg-neutral-50 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <TrendingUp className="w-4 h-4 text-blue-600" />
              <span className="text-sm font-medium text-neutral-700">Monthly Growth</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-2xl font-bold text-neutral-900">
                {stats.monthlyGrowth.toFixed(1)}%
              </span>
              {stats.monthlyGrowth > 0 ? (
                <TrendingUp className="w-4 h-4 text-green-600" />
              ) : (
                <TrendingDown className="w-4 h-4 text-red-600" />
              )}
            </div>
          </div>
          
          <div className="bg-neutral-50 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <Calendar className="w-4 h-4 text-purple-600" />
              <span className="text-sm font-medium text-neutral-700">Avg. Booking Value</span>
            </div>
            <div className="text-2xl font-bold text-neutral-900">
              ₹{Math.round(stats.averageBookingValue).toLocaleString()}
            </div>
          </div>
        </div>
      )}

      {/* Simple Bar Chart */}
      <div className="relative">
        <div className="flex items-end justify-between h-64 gap-2">
          {data.map((item, index) => (
            <motion.div
              key={item.month}
              initial={{ height: 0 }}
              animate={{ height: `${(item.revenue / maxRevenue) * 100}%` }}
              transition={{ duration: 0.8, delay: index * 0.1 }}
              className="flex-1 bg-gradient-to-t from-primary-500 to-primary-400 rounded-t-lg min-h-[20px] relative group cursor-pointer hover:from-primary-600 hover:to-primary-500 transition-colors duration-200"
            >
              {/* Tooltip */}
              <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none">
                <div className="bg-neutral-900 text-white text-xs rounded-lg px-3 py-2 whitespace-nowrap">
                  <div className="font-medium">₹{(item.revenue / 1000).toFixed(0)}K</div>
                  <div className="text-neutral-300">{item.bookings} bookings</div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
        
        {/* X-axis labels */}
        <div className="flex justify-between mt-4 text-sm text-neutral-600">
          {data.map((item) => (
            <span key={item.month} className="flex-1 text-center">
              {item.month}
            </span>
          ))}
        </div>
      </div>

      <div className="mt-6 pt-4 border-t border-neutral-200">
        <button className="text-primary-600 text-sm font-medium hover:text-primary-700 transition-colors duration-200">
          View detailed revenue report →
        </button>
      </div>
    </NeumorphicCard>
  );
}
