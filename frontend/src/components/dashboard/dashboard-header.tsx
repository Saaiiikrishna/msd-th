'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { 
  User, 
  Calendar, 
  CreditCard, 
  Heart, 
  Settings,
  Bell,
  Search,
  Plus
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { useAuth } from '@/hooks/useAuth';

const tabs = [
  { id: 'overview', label: 'Overview', icon: User },
  { id: 'bookings', label: 'My Bookings', icon: Calendar },
  { id: 'payments', label: 'Payments', icon: CreditCard },
  { id: 'favorites', label: 'Favorites', icon: Heart },
  { id: 'profile', label: 'Profile', icon: Settings },
];

interface DashboardHeaderProps {
  activeTab: string;
}

export function DashboardHeader({ activeTab }: DashboardHeaderProps) {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');
  const { user: authUser } = useAuth();

  // Use real user data from auth context
  const user = {
    name: authUser ? `${authUser.firstName} ${authUser.lastName}`.trim() : 'User',
    email: authUser?.email || '',
    avatar: 'https://images.unsplash.com/photo-1494790108755-2616b612b786?w=150&h=150&fit=crop&crop=face', // Default avatar since avatarUrl is not available
    memberSince: '2025', // Default since createdAt is not available in auth user type
    totalBookings: 0, // TODO: Get from dashboard API
    upcomingBookings: 0, // TODO: Get from dashboard API
  };

  const handleTabChange = (tabId: string) => {
    router.push(`/dashboard?tab=${tabId}`);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    // TODO: Implement search functionality
    console.log('Searching:', searchQuery);
  };

  return (
    <section className="bg-gradient-to-br from-primary-600 via-primary-700 to-secondary-600 text-white">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="space-y-8"
        >
          {/* User Welcome */}
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
            <div className="flex items-center gap-4">
              <div className="relative">
                <img
                  src={user.avatar}
                  alt={user.name}
                  className="w-16 h-16 rounded-full border-4 border-white/20 shadow-neumorphic"
                />
                <div className="absolute -bottom-1 -right-1 w-6 h-6 bg-success-500 rounded-full border-2 border-white flex items-center justify-center">
                  <div className="w-2 h-2 bg-white rounded-full"></div>
                </div>
              </div>
              <div>
                <h1 className="text-2xl md:text-3xl font-display font-bold">
                  Welcome back, {user.name}!
                </h1>
                <p className="text-white/80">
                  Member since {user.memberSince} • {user.totalBookings} adventures completed
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              {/* Notifications */}
              <button className="relative p-3 bg-white/10 backdrop-blur-sm rounded-full hover:bg-white/20 transition-all duration-200">
                <Bell className="w-5 h-5" />
                <span className="absolute -top-1 -right-1 w-5 h-5 bg-error-500 text-white text-xs rounded-full flex items-center justify-center">
                  3
                </span>
              </button>

              {/* Quick Book */}
              <Link href="/adventures">
                <NeumorphicButton
                  variant="secondary"
                  className="bg-white text-primary-600 hover:bg-neutral-50"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Book Adventure
                </NeumorphicButton>
              </Link>
            </div>
          </div>

          {/* Quick Stats */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <NeumorphicCard className="text-center bg-white/10 backdrop-blur-md border border-white/20">
              <div className="text-2xl font-bold text-white">{user.totalBookings}</div>
              <div className="text-sm text-white/80">Total Adventures</div>
            </NeumorphicCard>
            <NeumorphicCard className="text-center bg-white/10 backdrop-blur-md border border-white/20">
              <div className="text-2xl font-bold text-white">{user.upcomingBookings}</div>
              <div className="text-sm text-white/80">Upcoming</div>
            </NeumorphicCard>
            <NeumorphicCard className="text-center bg-white/10 backdrop-blur-md border border-white/20">
              <div className="text-2xl font-bold text-white">4.9</div>
              <div className="text-sm text-white/80">Avg Rating</div>
            </NeumorphicCard>
            <NeumorphicCard className="text-center bg-white/10 backdrop-blur-md border border-white/20">
              <div className="text-2xl font-bold text-white">₹15,000</div>
              <div className="text-sm text-white/80">Total Spent</div>
            </NeumorphicCard>
          </div>

          {/* Search Bar */}
          <NeumorphicCard className="bg-white/10 backdrop-blur-md border border-white/20">
            <form onSubmit={handleSearch} className="flex items-center gap-3">
              <Search className="w-5 h-5 text-white/70" />
              <input
                type="text"
                placeholder="Search your bookings, adventures, or payments..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="flex-1 bg-transparent text-white placeholder-white/70 border-none outline-none text-lg"
              />
              <NeumorphicButton
                type="submit"
                variant="secondary"
                size="sm"
                className="bg-white/20 text-white border-white/30 hover:bg-white/30"
              >
                Search
              </NeumorphicButton>
            </form>
          </NeumorphicCard>

          {/* Navigation Tabs */}
          <div className="flex flex-wrap gap-2">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => handleTabChange(tab.id)}
                className={`flex items-center gap-2 px-4 py-2 rounded-neumorphic transition-all duration-200 ${
                  activeTab === tab.id
                    ? 'bg-white text-primary-600 shadow-neumorphic'
                    : 'bg-white/10 text-white hover:bg-white/20'
                }`}
              >
                <tab.icon className="w-4 h-4" />
                <span className="font-medium">{tab.label}</span>
              </button>
            ))}
          </div>
        </motion.div>
      </div>
    </section>
  );
}
