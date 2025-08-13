'use client';

import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Shield, Users, Clock, Award, Star, CheckCircle } from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { useTrustWidgets } from '@/hooks/use-trust-widgets';

const defaultIndicators = [
  {
    icon: Users,
    value: '10,000+',
    title: 'Happy Customers',
    description: 'Treasure hunters have enjoyed our experiences',
    color: 'text-accent-500'
  },
  {
    icon: CheckCircle,
    value: '99.9%',
    title: 'Success Rate',
    description: 'Successful payment processing',
    color: 'text-success-500'
  },
  {
    icon: Clock,
    value: '24/7',
    title: 'Support',
    description: 'Customer support available round the clock',
    color: 'text-primary-500'
  },
  {
    icon: Award,
    value: 'Instant',
    title: 'Confirmation',
    description: 'Immediate booking confirmation',
    color: 'text-secondary-500'
  }
];

const trustBadges = [
  {
    icon: Shield,
    title: 'Secured by Razorpay',
    description: 'Bank-level security',
    badge: '/images/badges/razorpay-badge.svg'
  },
  {
    icon: Star,
    title: 'PCI DSS Compliant',
    description: 'Industry standard security',
    badge: '/images/badges/pci-badge.svg'
  }
];

export function TrustIndicators() {
  // Temporarily disable API call to fix SVG error
  // const { data: trustConfig, isLoading } = useTrustWidgets();
  const [animatedValues, setAnimatedValues] = useState<Record<string, number>>({});

  // Use default indicators for now
  const indicators = defaultIndicators;
  const isLoading = false;

  // Animate numbers on mount
  useEffect(() => {
    const animateValue = (key: string, start: number, end: number, duration: number) => {
      const startTime = Date.now();
      const animate = () => {
        const elapsed = Date.now() - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const current = Math.floor(start + (end - start) * progress);
        
        setAnimatedValues(prev => ({ ...prev, [key]: current }));
        
        if (progress < 1) {
          requestAnimationFrame(animate);
        }
      };
      requestAnimationFrame(animate);
    };

    // Extract numbers from indicator values and animate them
    indicators.forEach((indicator, index) => {
      const numericValue = indicator.value.replace(/[^\d]/g, '');
      if (numericValue) {
        const endValue = parseInt(numericValue);
        animateValue(`indicator-${index}`, 0, endValue, 2000 + index * 200);
      }
    });
  }, [indicators]);

  const formatAnimatedValue = (originalValue: string, animatedValue: number) => {
    if (originalValue.includes('+')) {
      return `${animatedValue.toLocaleString()}+`;
    }
    if (originalValue.includes('%')) {
      return `${animatedValue}%`;
    }
    if (originalValue.includes('/')) {
      return originalValue; // Keep original for "24/7"
    }
    return animatedValue.toLocaleString();
  };

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="animate-pulse">
            <NeumorphicCard className="h-32 bg-neutral-200" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Main Trust Indicators - More Compact for Bottom Placement */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {indicators.map((indicator, index) => (
          <motion.div
            key={indicator.title}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: index * 0.1 }}
          >
            <NeumorphicCard
              interactive
              className="text-center group hover:scale-102 transition-transform duration-300 py-4"
            >
              <div className={`inline-flex items-center justify-center w-10 h-10 rounded-full bg-gradient-subtle mb-3 group-hover:shadow-neumorphic-soft transition-all duration-300`}>
                <indicator.icon className={`w-5 h-5 ${indicator.color}`} />
              </div>

              <div className="text-2xl md:text-3xl font-bold text-neutral-700 mb-1">
                {animatedValues[`indicator-${index}`] !== undefined
                  ? formatAnimatedValue(indicator.value, animatedValues[`indicator-${index}`])
                  : indicator.value
                }
              </div>

              <div className="text-base font-semibold text-neutral-600 mb-1">
                {indicator.title}
              </div>

              <div className="text-xs text-neutral-500 leading-relaxed">
                {indicator.description}
              </div>
            </NeumorphicCard>
          </motion.div>
        ))}
      </div>

      {/* Trust Badges - Simplified for Bottom */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.3 }}
        className="flex flex-col md:flex-row items-center justify-center gap-6"
      >
        <div className="text-center">
          <h3 className="text-lg font-semibold text-neutral-700 mb-1">
            Trusted & Secure
          </h3>
          <p className="text-sm text-neutral-500 max-w-md">
            Your payments and data are protected by industry-leading security standards
          </p>
        </div>

        <div className="flex items-center gap-4">
          {trustBadges.map((badge, index) => (
            <motion.div
              key={badge.title}
              initial={{ opacity: 0, scale: 0.9 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: 0.4 + index * 0.1 }}
              className="group"
            >
              <NeumorphicCard className="flex items-center gap-2 px-3 py-2 hover:shadow-neumorphic-soft transition-all duration-300">
                <div className="flex items-center justify-center w-8 h-8 rounded-full bg-gradient-subtle">
                  <badge.icon className="w-4 h-4 text-primary-500" />
                </div>
                <div>
                  <div className="font-medium text-xs text-neutral-700">
                    {badge.title}
                  </div>
                  <div className="text-xs text-neutral-500">
                    {badge.description}
                  </div>
                </div>
              </NeumorphicCard>
            </motion.div>
          ))}
        </div>
      </motion.div>

      {/* Security Features - Compact Version */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.5 }}
      >
        <NeumorphicCard className="bg-gradient-subtle border border-neutral-200">
          <div className="text-center py-4">
            <div className="flex items-center justify-center gap-2 mb-3">
              <Shield className="w-6 h-6 text-primary-500" />
              <h3 className="text-base font-semibold text-neutral-700">
                100% Secure Payments
              </h3>
            </div>
            <div className="flex flex-wrap justify-center gap-3 text-xs text-neutral-600">
              <span className="flex items-center gap-1">
                <CheckCircle className="w-3 h-3 text-success-500" />
                256-bit SSL Encryption
              </span>
              <span className="flex items-center gap-1">
                <CheckCircle className="w-3 h-3 text-success-500" />
                PCI DSS Compliant
              </span>
              <span className="flex items-center gap-1">
                <CheckCircle className="w-3 h-3 text-success-500" />
                Fraud Detection
              </span>
              <span className="flex items-center gap-1">
                <CheckCircle className="w-3 h-3 text-success-500" />
                Bank-level Security
              </span>
            </div>
          </div>
        </NeumorphicCard>
      </motion.div>
    </div>
  );
}
