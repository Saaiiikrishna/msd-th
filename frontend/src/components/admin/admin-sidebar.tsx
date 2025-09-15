'use client';

import { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { motion } from 'framer-motion';
import {
  LayoutDashboard,
  MapPin,
  Users,
  Calendar,
  CreditCard,
  Star,
  Settings,
  BarChart3,
  MessageSquare,
  Bell,
  Shield,
  FileText,
  Package,
  UserCheck,
  TrendingUp,
  Database,
  LogOut,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';

const navigation = [
  {
    name: 'Dashboard',
    href: '/admin',
    icon: LayoutDashboard,
    description: 'Overview and analytics',
  },
  {
    name: 'Adventures',
    href: '/admin/adventures',
    icon: MapPin,
    description: 'Manage treasure hunts',
    children: [
      { name: 'All Adventures', href: '/admin/adventures' },
      { name: 'Create New', href: '/admin/adventures/create' },
      { name: 'Categories', href: '/admin/adventures/categories' },
      { name: 'Locations', href: '/admin/adventures/locations' },
    ],
  },
  {
    name: 'Bookings',
    href: '/admin/bookings',
    icon: Calendar,
    description: 'Manage reservations',
    children: [
      { name: 'All Bookings', href: '/admin/bookings' },
      { name: 'Today\'s Bookings', href: '/admin/bookings/today' },
      { name: 'Upcoming', href: '/admin/bookings/upcoming' },
      { name: 'Cancelled', href: '/admin/bookings/cancelled' },
    ],
  },
  {
    name: 'Users',
    href: '/admin/users',
    icon: Users,
    description: 'User management',
    children: [
      { name: 'All Users', href: '/admin/users' },
      { name: 'Customers', href: '/admin/users/customers' },
      { name: 'Vendors', href: '/admin/users/vendors' },
      { name: 'Admins', href: '/admin/users/admins' },
    ],
  },
  {
    name: 'Payments',
    href: '/admin/payments',
    icon: CreditCard,
    description: 'Financial management',
    children: [
      { name: 'All Payments', href: '/admin/payments' },
      { name: 'Refunds', href: '/admin/payments/refunds' },
      { name: 'Payouts', href: '/admin/payments/payouts' },
      { name: 'Reports', href: '/admin/payments/reports' },
    ],
  },
  {
    name: 'Reviews',
    href: '/admin/reviews',
    icon: Star,
    description: 'Review moderation',
    children: [
      { name: 'All Reviews', href: '/admin/reviews' },
      { name: 'Pending', href: '/admin/reviews/pending' },
      { name: 'Reported', href: '/admin/reviews/reported' },
    ],
  },
  {
    name: 'Analytics',
    href: '/admin/analytics',
    icon: BarChart3,
    description: 'Business insights',
    children: [
      { name: 'Overview', href: '/admin/analytics' },
      { name: 'Revenue', href: '/admin/analytics/revenue' },
      { name: 'Users', href: '/admin/analytics/users' },
      { name: 'A/B Tests', href: '/admin/analytics/ab-tests' },
    ],
  },
  {
    name: 'Communications',
    href: '/admin/communications',
    icon: MessageSquare,
    description: 'Messages and notifications',
    children: [
      { name: 'Chat Support', href: '/admin/communications/chat' },
      { name: 'Notifications', href: '/admin/communications/notifications' },
      { name: 'Email Templates', href: '/admin/communications/emails' },
    ],
  },
  {
    name: 'Content',
    href: '/admin/content',
    icon: FileText,
    description: 'Content management',
    children: [
      { name: 'Pages', href: '/admin/content/pages' },
      { name: 'Blog Posts', href: '/admin/content/blog' },
      { name: 'Media Library', href: '/admin/content/media' },
    ],
  },
  {
    name: 'System',
    href: '/admin/system',
    icon: Database,
    description: 'System management',
    children: [
      { name: 'Logs', href: '/admin/system/logs' },
      { name: 'Backups', href: '/admin/system/backups' },
      { name: 'Health Check', href: '/admin/system/health' },
    ],
  },
  {
    name: 'Settings',
    href: '/admin/settings',
    icon: Settings,
    description: 'Platform configuration',
    children: [
      { name: 'General', href: '/admin/settings' },
      { name: 'Payment', href: '/admin/settings/payment' },
      { name: 'Email', href: '/admin/settings/email' },
      { name: 'Security', href: '/admin/settings/security' },
    ],
  },
];

export function AdminSidebar() {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [expandedItems, setExpandedItems] = useState<string[]>([]);
  const pathname = usePathname();

  const toggleExpanded = (itemName: string) => {
    setExpandedItems(prev =>
      prev.includes(itemName)
        ? prev.filter(name => name !== itemName)
        : [...prev, itemName]
    );
  };

  const isActive = (href: string) => {
    if (!pathname) return false;
    if (href === '/admin') {
      return pathname === '/admin';
    }
    return pathname.startsWith(href);
  };

  const isChildActive = (children: any[]) => {
    if (!pathname) return false;
    return children.some(child => pathname === child.href);
  };

  return (
    <div className={`fixed inset-y-0 left-0 z-50 ${isCollapsed ? 'w-20' : 'w-72'} transition-all duration-300`}>
      <NeumorphicCard className="h-full rounded-none border-r border-neutral-200">
        <div className="flex h-full flex-col">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-neutral-200">
            {!isCollapsed && (
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-lg flex items-center justify-center">
                  <Shield className="w-5 h-5 text-white" />
                </div>
                <div>
                  <h1 className="text-lg font-semibold text-neutral-800">Admin Panel</h1>
                  <p className="text-xs text-neutral-600">Treasure Hunt</p>
                </div>
              </div>
            )}
            
            <button
              onClick={() => setIsCollapsed(!isCollapsed)}
              className="p-2 text-neutral-600 hover:text-neutral-800 hover:bg-neutral-100 rounded-full transition-colors duration-200"
            >
              {isCollapsed ? (
                <ChevronRight className="w-4 h-4" />
              ) : (
                <ChevronLeft className="w-4 h-4" />
              )}
            </button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 overflow-y-auto p-4 space-y-2">
            {navigation.map((item) => {
              const isItemActive = isActive(item.href);
              const hasActiveChild = item.children && isChildActive(item.children);
              const isExpanded = expandedItems.includes(item.name);
              const shouldExpand = isExpanded || hasActiveChild;

              return (
                <div key={item.name}>
                  <Link
                    href={item.href}
                    onClick={() => {
                      if (item.children) {
                        toggleExpanded(item.name);
                      }
                    }}
                    className={`group flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-neumorphic transition-all duration-200 ${
                      isItemActive || hasActiveChild
                        ? 'bg-primary-50 text-primary-700 shadow-neumorphic-pressed'
                        : 'text-neutral-700 hover:bg-neutral-100 hover:text-neutral-900'
                    }`}
                  >
                    <item.icon className={`w-5 h-5 shrink-0 ${
                      isItemActive || hasActiveChild ? 'text-primary-600' : 'text-neutral-500'
                    }`} />
                    
                    {!isCollapsed && (
                      <>
                        <div className="flex-1">
                          <div className="font-medium">{item.name}</div>
                          <div className="text-xs text-neutral-500">{item.description}</div>
                        </div>
                        
                        {item.children && (
                          <motion.div
                            animate={{ rotate: shouldExpand ? 90 : 0 }}
                            transition={{ duration: 0.2 }}
                          >
                            <ChevronRight className="w-4 h-4" />
                          </motion.div>
                        )}
                      </>
                    )}
                  </Link>

                  {/* Submenu */}
                  {item.children && !isCollapsed && (
                    <motion.div
                      initial={false}
                      animate={{
                        height: shouldExpand ? 'auto' : 0,
                        opacity: shouldExpand ? 1 : 0,
                      }}
                      transition={{ duration: 0.2 }}
                      className="overflow-hidden"
                    >
                      <div className="ml-8 mt-2 space-y-1">
                        {item.children.map((child) => (
                          <Link
                            key={child.href}
                            href={child.href}
                            className={`block px-3 py-2 text-sm rounded-lg transition-colors duration-200 ${
                              pathname === child.href
                                ? 'bg-primary-100 text-primary-700 font-medium'
                                : 'text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900'
                            }`}
                          >
                            {child.name}
                          </Link>
                        ))}
                      </div>
                    </motion.div>
                  )}
                </div>
              );
            })}
          </nav>

          {/* Footer */}
          <div className="p-4 border-t border-neutral-200">
            {!isCollapsed && (
              <div className="mb-4">
                <div className="flex items-center gap-3 p-3 bg-neutral-100 rounded-neumorphic">
                  <img
                    src="/images/admin-avatar.jpg"
                    alt="Admin"
                    className="w-8 h-8 rounded-full"
                    onError={(e) => {
                      (e.target as HTMLImageElement).src = '/images/default-avatar.png';
                    }}
                  />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-neutral-800 truncate">
                      Admin User
                    </p>
                    <p className="text-xs text-neutral-600 truncate">
                      admin@treasurehunt.com
                    </p>
                  </div>
                </div>
              </div>
            )}
            
            <NeumorphicButton
              variant="ghost"
              size="sm"
              className={`${isCollapsed ? 'w-12 h-12 p-0' : 'w-full'} text-error-600 hover:bg-error-50`}
            >
              <LogOut className="w-4 h-4" />
              {!isCollapsed && <span className="ml-2">Sign Out</span>}
            </NeumorphicButton>
          </div>
        </div>
      </NeumorphicCard>
    </div>
  );
}
