import { Suspense } from 'react';
import { AdminStats } from '@/components/admin/admin-stats';
import { RecentActivity } from '@/components/admin/recent-activity';
import { RevenueChart } from '@/components/admin/revenue-chart';
import { BookingsChart } from '@/components/admin/bookings-chart';
import { TopAdventures } from '@/components/admin/top-adventures';
import { QuickActions } from '@/components/admin/quick-actions';
import { SystemHealth } from '@/components/admin/system-health';

export default function AdminDashboard() {
  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div>
        <h1 className="text-3xl font-bold text-neutral-900">Dashboard</h1>
        <p className="text-neutral-600 mt-2">
          Welcome back! Here's what's happening with your treasure hunt platform.
        </p>
      </div>

      {/* Quick Actions */}
      <Suspense fallback={<div className="h-32 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
        <QuickActions />
      </Suspense>

      {/* Stats Overview */}
      <Suspense fallback={<div className="h-40 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
        <AdminStats />
      </Suspense>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
          <RevenueChart />
        </Suspense>
        
        <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
          <BookingsChart />
        </Suspense>
      </div>

      {/* Content Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
            <RecentActivity />
          </Suspense>
        </div>
        
        <div className="space-y-8">
          <Suspense fallback={<div className="h-64 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
            <TopAdventures />
          </Suspense>
          
          <Suspense fallback={<div className="h-32 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
            <SystemHealth />
          </Suspense>
        </div>
      </div>
    </div>
  );
}
