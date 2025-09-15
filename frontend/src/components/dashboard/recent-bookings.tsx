'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Calendar,
  MapPin,
  Clock,
  Users,
  Star,
  Eye,
  MessageSquare,
  MoreHorizontal,
  CheckCircle,
  XCircle,
  AlertCircle
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import Link from 'next/link';

interface Booking {
  id: string;
  adventure: {
    id: string;
    title: string;
    location: string;
    imageUrl: string;
    rating: number;
    reviewCount: number;
  };
  date: string;
  time: string;
  participants: number;
  totalAmount: number;
  status: 'confirmed' | 'completed' | 'cancelled' | 'pending';
  bookingDate: string;
  hasReview: boolean;
  canCancel: boolean;
  canReschedule: boolean;
}

export function RecentBookings() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'upcoming' | 'completed' | 'cancelled'>('all');

  useEffect(() => {
    const fetchRecentBookings = async () => {
      setIsLoading(true);
      
      // Mock data - replace with actual API call
      const mockBookings: Booking[] = [
        {
          id: '1',
          adventure: {
            id: 'adv-1',
            title: 'Mumbai Heritage Walk',
            location: 'South Mumbai',
            imageUrl: '/images/mumbai-heritage.jpg',
            rating: 4.8,
            reviewCount: 124,
          },
          date: '2024-02-15',
          time: '09:00 AM',
          participants: 2,
          totalAmount: 3000,
          status: 'confirmed',
          bookingDate: '2024-02-01',
          hasReview: false,
          canCancel: true,
          canReschedule: true,
        },
        {
          id: '2',
          adventure: {
            id: 'adv-2',
            title: 'Delhi Food Trail',
            location: 'Old Delhi',
            imageUrl: '/images/delhi-food.jpg',
            rating: 4.9,
            reviewCount: 98,
          },
          date: '2024-01-20',
          time: '06:00 PM',
          participants: 1,
          totalAmount: 1200,
          status: 'completed',
          bookingDate: '2024-01-10',
          hasReview: true,
          canCancel: false,
          canReschedule: false,
        },
        {
          id: '3',
          adventure: {
            id: 'adv-3',
            title: 'Goa Beach Adventure',
            location: 'North Goa',
            imageUrl: '/images/goa-beach.jpg',
            rating: 4.6,
            reviewCount: 67,
          },
          date: '2024-03-10',
          time: '08:00 AM',
          participants: 4,
          totalAmount: 10000,
          status: 'confirmed',
          bookingDate: '2024-02-20',
          hasReview: false,
          canCancel: true,
          canReschedule: true,
        },
        {
          id: '4',
          adventure: {
            id: 'adv-4',
            title: 'Rajasthan Desert Safari',
            location: 'Jaisalmer',
            imageUrl: '/images/rajasthan-desert.jpg',
            rating: 4.7,
            reviewCount: 89,
          },
          date: '2024-01-05',
          time: '04:00 PM',
          participants: 2,
          totalAmount: 6000,
          status: 'completed',
          bookingDate: '2023-12-20',
          hasReview: false,
          canCancel: false,
          canReschedule: false,
        },
        {
          id: '5',
          adventure: {
            id: 'adv-5',
            title: 'Kerala Backwaters Tour',
            location: 'Alleppey',
            imageUrl: '/images/kerala-backwaters.jpg',
            rating: 4.5,
            reviewCount: 56,
          },
          date: '2024-01-15',
          time: '10:00 AM',
          participants: 3,
          totalAmount: 7500,
          status: 'cancelled',
          bookingDate: '2024-01-01',
          hasReview: false,
          canCancel: false,
          canReschedule: false,
        },
      ];

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 800));
      setBookings(mockBookings);
      setIsLoading(false);
    };

    fetchRecentBookings();
  }, []);

  const getStatusIcon = (status: Booking['status']) => {
    switch (status) {
      case 'confirmed':
        return CheckCircle;
      case 'completed':
        return CheckCircle;
      case 'cancelled':
        return XCircle;
      case 'pending':
        return AlertCircle;
      default:
        return AlertCircle;
    }
  };

  const getStatusColor = (status: Booking['status']) => {
    switch (status) {
      case 'confirmed':
        return 'text-blue-600 bg-blue-100';
      case 'completed':
        return 'text-green-600 bg-green-100';
      case 'cancelled':
        return 'text-red-600 bg-red-100';
      case 'pending':
        return 'text-yellow-600 bg-yellow-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const filteredBookings = bookings.filter(booking => {
    if (filter === 'all') return true;
    if (filter === 'upcoming') return booking.status === 'confirmed';
    if (filter === 'completed') return booking.status === 'completed';
    if (filter === 'cancelled') return booking.status === 'cancelled';
    return true;
  });

  if (isLoading) {
    return (
      <NeumorphicCard className="p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-neutral-200 rounded w-1/3 mb-6"></div>
          <div className="space-y-4">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="flex gap-4 p-4 border border-neutral-200 rounded-lg">
                <div className="w-16 h-16 bg-neutral-200 rounded-lg"></div>
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-neutral-200 rounded w-2/3"></div>
                  <div className="h-3 bg-neutral-200 rounded w-1/2"></div>
                  <div className="h-3 bg-neutral-200 rounded w-1/3"></div>
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
        <h2 className="text-xl font-semibold text-neutral-900">
          Recent Bookings
        </h2>
        <div className="flex bg-neutral-100 rounded-lg p-1">
          {(['all', 'upcoming', 'completed', 'cancelled'] as const).map((filterOption) => (
            <button
              key={filterOption}
              onClick={() => setFilter(filterOption)}
              className={`px-3 py-1 text-sm rounded-md transition-colors duration-200 capitalize ${
                filter === filterOption
                  ? 'bg-white text-neutral-900 shadow-sm'
                  : 'text-neutral-600 hover:text-neutral-900'
              }`}
            >
              {filterOption}
            </button>
          ))}
        </div>
      </div>

      {filteredBookings.length === 0 ? (
        <div className="text-center py-8">
          <Calendar className="w-12 h-12 text-neutral-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-neutral-900 mb-2">
            No bookings found
          </h3>
          <p className="text-neutral-600 mb-6">
            {filter === 'all' 
              ? "You haven't made any bookings yet." 
              : `No ${filter} bookings found.`}
          </p>
          <Link href="/adventures">
            <NeumorphicButton variant="primary">
              Explore Adventures
            </NeumorphicButton>
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredBookings.map((booking, index) => {
            const StatusIcon = getStatusIcon(booking.status);
            const statusColor = getStatusColor(booking.status);
            
            return (
              <motion.div
                key={booking.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3, delay: index * 0.1 }}
                className="flex gap-4 p-4 border border-neutral-200 rounded-lg hover:border-neutral-300 transition-colors duration-200"
              >
                {/* Adventure Image */}
                <div className="w-16 h-16 bg-gradient-to-br from-primary-400 to-primary-600 rounded-lg flex-shrink-0">
                  {/* Adventure image placeholder */}
                </div>
                
                {/* Booking Details */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <Link 
                        href={`/adventures/${booking.adventure.id}`}
                        className="font-medium text-neutral-900 hover:text-primary-600 transition-colors duration-200"
                      >
                        {booking.adventure.title}
                      </Link>
                      <div className="flex items-center gap-4 mt-1 text-sm text-neutral-600">
                        <div className="flex items-center gap-1">
                          <MapPin className="w-3 h-3" />
                          <span>{booking.adventure.location}</span>
                        </div>
                        <div className="flex items-center gap-1">
                          <Star className="w-3 h-3 text-yellow-500 fill-current" />
                          <span>{booking.adventure.rating}</span>
                        </div>
                      </div>
                    </div>
                    
                    <div className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${statusColor}`}>
                      <StatusIcon className="w-3 h-3" />
                      <span className="capitalize">{booking.status}</span>
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-4 mt-3 text-sm text-neutral-600">
                    <div className="flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      <span>{new Date(booking.date).toLocaleDateString()}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      <span>{booking.time}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Users className="w-3 h-3" />
                      <span>{booking.participants} {booking.participants === 1 ? 'person' : 'people'}</span>
                    </div>
                  </div>
                  
                  <div className="flex items-center justify-between mt-3">
                    <div className="font-semibold text-neutral-900">
                      ₹{booking.totalAmount.toLocaleString()}
                    </div>
                    
                    <div className="flex items-center gap-2">
                      {booking.status === 'completed' && !booking.hasReview && (
                        <NeumorphicButton variant="outline" size="sm">
                          <MessageSquare className="w-4 h-4 mr-1" />
                          Write Review
                        </NeumorphicButton>
                      )}
                      
                      <Link href={`/bookings/${booking.id}`}>
                        <NeumorphicButton variant="ghost" size="sm">
                          <Eye className="w-4 h-4" />
                        </NeumorphicButton>
                      </Link>
                      
                      <NeumorphicButton variant="ghost" size="sm">
                        <MoreHorizontal className="w-4 h-4" />
                      </NeumorphicButton>
                    </div>
                  </div>
                </div>
              </motion.div>
            );
          })}
        </div>
      )}
      
      {filteredBookings.length > 0 && (
        <div className="mt-6 pt-4 border-t border-neutral-200 text-center">
          <Link 
            href="/bookings"
            className="text-primary-600 text-sm font-medium hover:text-primary-700 transition-colors duration-200"
          >
            View all bookings →
          </Link>
        </div>
      )}
    </NeumorphicCard>
  );
}
