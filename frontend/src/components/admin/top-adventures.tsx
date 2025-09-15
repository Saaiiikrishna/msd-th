'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Star,
  MapPin,
  Users,
  TrendingUp,
  Eye,
  Calendar
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import Link from 'next/link';

interface Adventure {
  id: string;
  title: string;
  location: string;
  rating: number;
  reviewCount: number;
  bookings: number;
  revenue: number;
  views: number;
  trend: 'up' | 'down' | 'stable';
  trendValue: number;
}

export function TopAdventures() {
  const [adventures, setAdventures] = useState<Adventure[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [sortBy, setSortBy] = useState<'bookings' | 'revenue' | 'rating'>('bookings');

  useEffect(() => {
    const fetchTopAdventures = async () => {
      setIsLoading(true);
      
      // Mock data - replace with actual API call
      const mockAdventures: Adventure[] = [
        {
          id: '1',
          title: 'Mumbai Heritage Walk',
          location: 'Mumbai',
          rating: 4.8,
          reviewCount: 124,
          bookings: 89,
          revenue: 133500,
          views: 1250,
          trend: 'up',
          trendValue: 12.5,
        },
        {
          id: '2',
          title: 'Delhi Food Trail',
          location: 'Delhi',
          rating: 4.9,
          reviewCount: 98,
          bookings: 156,
          revenue: 187200,
          views: 1890,
          trend: 'up',
          trendValue: 18.3,
        },
        {
          id: '3',
          title: 'Goa Beach Adventure',
          location: 'Goa',
          rating: 4.6,
          reviewCount: 67,
          bookings: 45,
          revenue: 112500,
          views: 890,
          trend: 'down',
          trendValue: -5.2,
        },
        {
          id: '4',
          title: 'Rajasthan Desert Safari',
          location: 'Rajasthan',
          rating: 4.7,
          reviewCount: 89,
          bookings: 73,
          revenue: 219000,
          views: 1340,
          trend: 'up',
          trendValue: 8.7,
        },
        {
          id: '5',
          title: 'Kerala Backwaters Tour',
          location: 'Kerala',
          rating: 4.5,
          reviewCount: 56,
          bookings: 34,
          revenue: 85000,
          views: 670,
          trend: 'stable',
          trendValue: 1.2,
        },
      ];

      // Sort adventures based on selected criteria
      const sortedAdventures = [...mockAdventures].sort((a, b) => {
        switch (sortBy) {
          case 'revenue':
            return b.revenue - a.revenue;
          case 'rating':
            return b.rating - a.rating;
          default:
            return b.bookings - a.bookings;
        }
      });

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 800));
      setAdventures(sortedAdventures);
      setIsLoading(false);
    };

    fetchTopAdventures();
  }, [sortBy]);

  const getTrendIcon = (trend: Adventure['trend']) => {
    switch (trend) {
      case 'up':
        return <TrendingUp className="w-3 h-3 text-green-600" />;
      case 'down':
        return <TrendingUp className="w-3 h-3 text-red-600 rotate-180" />;
      default:
        return <div className="w-3 h-3 bg-neutral-400 rounded-full" />;
    }
  };

  const getTrendColor = (trend: Adventure['trend']) => {
    switch (trend) {
      case 'up':
        return 'text-green-600';
      case 'down':
        return 'text-red-600';
      default:
        return 'text-neutral-600';
    }
  };

  if (isLoading) {
    return (
      <NeumorphicCard className="p-6">
        <div className="animate-pulse">
          <div className="flex items-center justify-between mb-4">
            <div className="h-6 bg-neutral-200 rounded w-1/2"></div>
            <div className="h-8 bg-neutral-200 rounded w-20"></div>
          </div>
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="flex items-center gap-3">
                <div className="w-8 h-8 bg-neutral-200 rounded-full"></div>
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
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-semibold text-neutral-900 mb-1">
            Top Adventures
          </h2>
          <p className="text-neutral-600 text-sm">
            Best performing adventures this month
          </p>
        </div>
        
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value as 'bookings' | 'revenue' | 'rating')}
          className="text-sm border border-neutral-200 rounded-lg px-3 py-1 bg-white focus:outline-none focus:ring-2 focus:ring-primary-500"
        >
          <option value="bookings">By Bookings</option>
          <option value="revenue">By Revenue</option>
          <option value="rating">By Rating</option>
        </select>
      </div>

      <div className="space-y-4">
        {adventures.map((adventure, index) => (
          <motion.div
            key={adventure.id}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3, delay: index * 0.1 }}
          >
            <Link href={`/admin/adventures/${adventure.id}`}>
              <div className="flex items-center gap-4 p-3 rounded-lg hover:bg-neutral-50 transition-colors duration-200 cursor-pointer group">
                {/* Rank */}
                <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <span className="text-sm font-bold text-primary-600">
                    {index + 1}
                  </span>
                </div>
                
                {/* Adventure Info */}
                <div className="flex-1 min-w-0">
                  <h4 className="font-medium text-neutral-900 group-hover:text-primary-600 transition-colors duration-200 truncate">
                    {adventure.title}
                  </h4>
                  
                  <div className="flex items-center gap-4 mt-1 text-sm text-neutral-600">
                    <div className="flex items-center gap-1">
                      <MapPin className="w-3 h-3" />
                      <span>{adventure.location}</span>
                    </div>
                    
                    <div className="flex items-center gap-1">
                      <Star className="w-3 h-3 text-yellow-500 fill-current" />
                      <span>{adventure.rating}</span>
                      <span className="text-neutral-400">({adventure.reviewCount})</span>
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-4 mt-2 text-xs text-neutral-500">
                    <div className="flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      <span>{adventure.bookings} bookings</span>
                    </div>
                    
                    <div className="flex items-center gap-1">
                      <Eye className="w-3 h-3" />
                      <span>{adventure.views} views</span>
                    </div>
                    
                    <div className="flex items-center gap-1">
                      <span>₹{(adventure.revenue / 1000).toFixed(0)}K</span>
                    </div>
                  </div>
                </div>
                
                {/* Trend */}
                <div className="flex items-center gap-1 text-sm">
                  {getTrendIcon(adventure.trend)}
                  <span className={getTrendColor(adventure.trend)}>
                    {Math.abs(adventure.trendValue).toFixed(1)}%
                  </span>
                </div>
              </div>
            </Link>
          </motion.div>
        ))}
      </div>
      
      <div className="mt-6 pt-4 border-t border-neutral-200">
        <Link 
          href="/admin/adventures"
          className="text-primary-600 text-sm font-medium hover:text-primary-700 transition-colors duration-200"
        >
          View all adventures →
        </Link>
      </div>
    </NeumorphicCard>
  );
}
