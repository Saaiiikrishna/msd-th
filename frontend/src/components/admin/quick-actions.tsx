'use client';

import { motion } from 'framer-motion';
import { 
  Plus, 
  Users, 
  FileText, 
  Settings,
  Download,
  Upload,
  Mail,
  BarChart3
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import Link from 'next/link';

interface QuickAction {
  title: string;
  description: string;
  icon: React.ElementType;
  href?: string;
  onClick?: () => void;
  color: string;
  bgColor: string;
}

export function QuickActions() {
  const actions: QuickAction[] = [
    {
      title: 'Create Adventure',
      description: 'Add a new treasure hunt adventure',
      icon: Plus,
      href: '/admin/adventures/create',
      color: 'text-blue-600',
      bgColor: 'bg-blue-100',
    },
    {
      title: 'Manage Users',
      description: 'View and manage user accounts',
      icon: Users,
      href: '/admin/users',
      color: 'text-green-600',
      bgColor: 'bg-green-100',
    },
    {
      title: 'Generate Report',
      description: 'Create performance and analytics reports',
      icon: BarChart3,
      href: '/admin/reports',
      color: 'text-purple-600',
      bgColor: 'bg-purple-100',
    },
    {
      title: 'Export Data',
      description: 'Download bookings and user data',
      icon: Download,
      onClick: () => handleExportData(),
      color: 'text-orange-600',
      bgColor: 'bg-orange-100',
    },
    {
      title: 'Import Content',
      description: 'Bulk import adventures and locations',
      icon: Upload,
      onClick: () => handleImportContent(),
      color: 'text-indigo-600',
      bgColor: 'bg-indigo-100',
    },
    {
      title: 'Send Newsletter',
      description: 'Send updates to all subscribers',
      icon: Mail,
      href: '/admin/newsletter',
      color: 'text-pink-600',
      bgColor: 'bg-pink-100',
    },
    {
      title: 'View Logs',
      description: 'Check system logs and errors',
      icon: FileText,
      href: '/admin/logs',
      color: 'text-gray-600',
      bgColor: 'bg-gray-100',
    },
    {
      title: 'System Settings',
      description: 'Configure platform settings',
      icon: Settings,
      href: '/admin/settings',
      color: 'text-red-600',
      bgColor: 'bg-red-100',
    },
  ];

  const handleExportData = () => {
    // Mock export functionality
    console.log('Exporting data...');
    // In a real app, this would trigger a download
  };

  const handleImportContent = () => {
    // Mock import functionality
    console.log('Opening import dialog...');
    // In a real app, this would open a file picker
  };

  return (
    <NeumorphicCard className="p-6">
      <div className="mb-6">
        <h2 className="text-xl font-semibold text-neutral-900 mb-2">
          Quick Actions
        </h2>
        <p className="text-neutral-600">
          Common tasks and shortcuts to help you manage your platform efficiently.
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {actions.map((action, index) => (
          <motion.div
            key={action.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: index * 0.1 }}
          >
            {action.href ? (
              <Link href={action.href}>
                <ActionCard action={action} />
              </Link>
            ) : (
              <button onClick={action.onClick} className="w-full text-left">
                <ActionCard action={action} />
              </button>
            )}
          </motion.div>
        ))}
      </div>
    </NeumorphicCard>
  );
}

function ActionCard({ action }: { action: QuickAction }) {
  return (
    <div className="p-4 bg-white rounded-neumorphic hover:shadow-md transition-all duration-200 border border-neutral-200 hover:border-neutral-300 group cursor-pointer">
      <div className="flex items-start gap-3">
        <div className={`w-10 h-10 ${action.bgColor} rounded-lg flex items-center justify-center flex-shrink-0 group-hover:scale-110 transition-transform duration-200`}>
          <action.icon className={`w-5 h-5 ${action.color}`} />
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="font-medium text-neutral-900 group-hover:text-primary-600 transition-colors duration-200">
            {action.title}
          </h3>
          <p className="text-sm text-neutral-600 mt-1 line-clamp-2">
            {action.description}
          </p>
        </div>
      </div>
    </div>
  );
}
