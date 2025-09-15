'use client';

import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { TrendingUp, Users, MapPin, DollarSign } from 'lucide-react';

export function AdminStats() {
  const stats = [
    {
      title: 'Total Users',
      value: '2,847',
      change: '+12.5%',
      icon: Users,
      color: 'text-blue-500',
      bgColor: 'bg-blue-100',
    },
    {
      title: 'Active Adventures',
      value: '42',
      change: '+8.2%',
      icon: MapPin,
      color: 'text-green-500',
      bgColor: 'bg-green-100',
    },
    {
      title: 'Total Revenue',
      value: '₹28.4L',
      change: '+22.1%',
      icon: DollarSign,
      color: 'text-purple-500',
      bgColor: 'bg-purple-100',
    },
    {
      title: 'Growth Rate',
      value: '18.3%',
      change: '+2.4%',
      icon: TrendingUp,
      color: 'text-orange-500',
      bgColor: 'bg-orange-100',
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      {stats.map((stat, index) => (
        <NeumorphicCard key={stat.title} className="p-6">
          <div className="flex items-center justify-between mb-4">
            <div className={`w-12 h-12 ${stat.bgColor} rounded-neumorphic flex items-center justify-center`}>
              <stat.icon className={`w-6 h-6 ${stat.color}`} />
            </div>
            <div className="text-sm font-medium text-green-600">
              {stat.change}
            </div>
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
      ))}
    </div>
  );
}
