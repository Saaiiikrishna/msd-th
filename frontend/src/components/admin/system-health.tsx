'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  CheckCircle,
  AlertTriangle,
  XCircle,
  Server,
  Database,
  Wifi,
  Shield,
  RefreshCw
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';

interface SystemService {
  name: string;
  status: 'healthy' | 'warning' | 'error';
  uptime: string;
  responseTime: number;
  lastChecked: string;
  icon: React.ElementType;
}

interface SystemMetrics {
  overallHealth: 'healthy' | 'warning' | 'error';
  uptime: string;
  totalRequests: number;
  errorRate: number;
}

export function SystemHealth() {
  const [services, setServices] = useState<SystemService[]>([]);
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const fetchSystemHealth = async () => {
    setIsRefreshing(true);
    
    // Mock data - replace with actual API call
    const mockServices: SystemService[] = [
      {
        name: 'API Gateway',
        status: 'healthy',
        uptime: '99.9%',
        responseTime: 45,
        lastChecked: '2 min ago',
        icon: Server,
      },
      {
        name: 'Database',
        status: 'healthy',
        uptime: '99.8%',
        responseTime: 12,
        lastChecked: '1 min ago',
        icon: Database,
      },
      {
        name: 'Payment Service',
        status: 'warning',
        uptime: '98.5%',
        responseTime: 156,
        lastChecked: '3 min ago',
        icon: Shield,
      },
      {
        name: 'CDN',
        status: 'healthy',
        uptime: '99.9%',
        responseTime: 23,
        lastChecked: '1 min ago',
        icon: Wifi,
      },
    ];

    const mockMetrics: SystemMetrics = {
      overallHealth: 'healthy',
      uptime: '99.7%',
      totalRequests: 45672,
      errorRate: 0.3,
    };

    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 1000));
    setServices(mockServices);
    setMetrics(mockMetrics);
    setIsLoading(false);
    setIsRefreshing(false);
  };

  useEffect(() => {
    fetchSystemHealth();
  }, []);

  const getStatusIcon = (status: SystemService['status']) => {
    switch (status) {
      case 'healthy':
        return CheckCircle;
      case 'warning':
        return AlertTriangle;
      case 'error':
        return XCircle;
      default:
        return CheckCircle;
    }
  };

  const getStatusColor = (status: SystemService['status']) => {
    switch (status) {
      case 'healthy':
        return 'text-green-600';
      case 'warning':
        return 'text-yellow-600';
      case 'error':
        return 'text-red-600';
      default:
        return 'text-gray-600';
    }
  };

  const getStatusBgColor = (status: SystemService['status']) => {
    switch (status) {
      case 'healthy':
        return 'bg-green-100';
      case 'warning':
        return 'bg-yellow-100';
      case 'error':
        return 'bg-red-100';
      default:
        return 'bg-gray-100';
    }
  };

  const getOverallHealthColor = (health: SystemMetrics['overallHealth']) => {
    switch (health) {
      case 'healthy':
        return 'text-green-600';
      case 'warning':
        return 'text-yellow-600';
      case 'error':
        return 'text-red-600';
      default:
        return 'text-gray-600';
    }
  };

  if (isLoading) {
    return (
      <NeumorphicCard className="p-6">
        <div className="animate-pulse">
          <div className="flex items-center justify-between mb-4">
            <div className="h-6 bg-neutral-200 rounded w-1/2"></div>
            <div className="w-8 h-8 bg-neutral-200 rounded-full"></div>
          </div>
          <div className="space-y-3">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="flex items-center gap-3">
                <div className="w-6 h-6 bg-neutral-200 rounded-full"></div>
                <div className="flex-1 space-y-1">
                  <div className="h-4 bg-neutral-200 rounded w-2/3"></div>
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
            System Health
          </h2>
          <p className="text-neutral-600 text-sm">
            Real-time system status and metrics
          </p>
        </div>
        
        <NeumorphicButton
          variant="ghost"
          size="sm"
          onClick={fetchSystemHealth}
          disabled={isRefreshing}
        >
          <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
        </NeumorphicButton>
      </div>

      {metrics && (
        <div className="mb-6 p-4 bg-neutral-50 rounded-lg">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-medium text-neutral-900">Overall Status</h3>
              <p className={`text-sm font-medium capitalize ${getOverallHealthColor(metrics.overallHealth)}`}>
                {metrics.overallHealth}
              </p>
            </div>
            <div className="text-right">
              <div className="text-sm text-neutral-600">Uptime</div>
              <div className="font-bold text-neutral-900">{metrics.uptime}</div>
            </div>
          </div>
          
          <div className="grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-neutral-200">
            <div>
              <div className="text-xs text-neutral-500">Total Requests</div>
              <div className="font-semibold text-neutral-900">
                {metrics.totalRequests.toLocaleString()}
              </div>
            </div>
            <div>
              <div className="text-xs text-neutral-500">Error Rate</div>
              <div className="font-semibold text-neutral-900">
                {metrics.errorRate}%
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="space-y-3">
        {services.map((service, index) => {
          const StatusIcon = getStatusIcon(service.status);
          const statusColor = getStatusColor(service.status);
          const statusBgColor = getStatusBgColor(service.status);
          
          return (
            <motion.div
              key={service.name}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.3, delay: index * 0.1 }}
              className="flex items-center gap-3 p-3 rounded-lg hover:bg-neutral-50 transition-colors duration-200"
            >
              <div className={`w-8 h-8 ${statusBgColor} rounded-full flex items-center justify-center`}>
                <service.icon className="w-4 h-4 text-neutral-600" />
              </div>
              
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <h4 className="font-medium text-neutral-900">
                    {service.name}
                  </h4>
                  <div className="flex items-center gap-1">
                    <StatusIcon className={`w-4 h-4 ${statusColor}`} />
                    <span className={`text-xs font-medium capitalize ${statusColor}`}>
                      {service.status}
                    </span>
                  </div>
                </div>
                
                <div className="flex items-center justify-between mt-1 text-xs text-neutral-500">
                  <span>Uptime: {service.uptime}</span>
                  <span>{service.responseTime}ms</span>
                </div>
                
                <div className="text-xs text-neutral-400 mt-1">
                  Last checked: {service.lastChecked}
                </div>
              </div>
            </motion.div>
          );
        })}
      </div>
      
      <div className="mt-6 pt-4 border-t border-neutral-200">
        <button className="text-primary-600 text-sm font-medium hover:text-primary-700 transition-colors duration-200">
          View detailed system logs →
        </button>
      </div>
    </NeumorphicCard>
  );
}
