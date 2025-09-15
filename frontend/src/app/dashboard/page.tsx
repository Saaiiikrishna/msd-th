import { Metadata } from 'next';
import { Suspense } from 'react';
import { DashboardHeader } from '@/components/dashboard/dashboard-header';
import { DashboardStats } from '@/components/dashboard/dashboard-stats';
import { RecentBookings } from '@/components/dashboard/recent-bookings';
import { UpcomingAdventures } from '@/components/dashboard/upcoming-adventures';
import { PaymentHistory } from '@/components/dashboard/payment-history';
import { FavoriteAdventures } from '@/components/dashboard/favorite-adventures';
import { ProfileSettings } from '@/components/dashboard/profile-settings';
import { Header } from '@/components/layout/header';
import { Footer } from '@/components/layout/footer';

export const metadata: Metadata = {
  title: 'Dashboard',
  description: 'Manage your treasure hunt bookings, view payment history, and discover new adventures.',
};

interface DashboardPageProps {
  searchParams: {
    tab?: string;
  };
}

export default function DashboardPage({ searchParams }: DashboardPageProps) {
  const activeTab = searchParams.tab || 'overview';

  return (
    <div className="min-h-screen bg-neutral-50">
      <Header />

      <main className="pt-20">
        {/* Dashboard Header */}
        <DashboardHeader activeTab={activeTab} />
        
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {activeTab === 'overview' && (
            <div className="space-y-8">
              {/* Stats Overview */}
              <Suspense fallback={<div className="h-32 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
                <DashboardStats />
              </Suspense>
              
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* Upcoming Adventures */}
                <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
                  <UpcomingAdventures />
                </Suspense>
                
                {/* Recent Bookings */}
                <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
                  <RecentBookings />
                </Suspense>
              </div>
            </div>
          )}

          {activeTab === 'bookings' && (
            <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
              <RecentBookings />
            </Suspense>
          )}

          {activeTab === 'payments' && (
            <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
              <PaymentHistory />
            </Suspense>
          )}

          {activeTab === 'favorites' && (
            <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
              <FavoriteAdventures />
            </Suspense>
          )}

          {activeTab === 'profile' && (
            <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
              <ProfileSettings />
            </Suspense>
          )}
        </div>
      </main>
      
      <Footer />
    </div>
  );
}
