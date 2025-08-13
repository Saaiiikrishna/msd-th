'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Chrome, User, Mail, Shield, CheckCircle } from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { useGoogleAuth } from '@/hooks/use-google-auth';
import { analytics } from '@/lib/analytics';

interface GoogleAuthProps {
  onSuccess?: (user: any) => void;
  onError?: (error: string) => void;
  redirectTo?: string;
  buttonText?: string;
  variant?: 'signin' | 'signup' | 'link';
  showBenefits?: boolean;
}

export function GoogleAuth({
  onSuccess,
  onError,
  redirectTo = '/dashboard',
  buttonText,
  variant = 'signin',
  showBenefits = true,
}: GoogleAuthProps) {
  const [isLoading, setIsLoading] = useState(false);
  const { signInWithGoogle, linkGoogleAccount, user, isAuthenticated } = useGoogleAuth();

  const getButtonText = () => {
    if (buttonText) return buttonText;
    
    switch (variant) {
      case 'signup':
        return 'Sign up with Google';
      case 'link':
        return 'Link Google Account';
      default:
        return 'Continue with Google';
    }
  };

  const handleGoogleAuth = async () => {
    setIsLoading(true);
    
    try {
      analytics.event({
        action: 'google_auth_attempt',
        category: 'authentication',
        label: variant,
      });

      let result;
      if (variant === 'link') {
        result = await linkGoogleAccount();
      } else {
        result = await signInWithGoogle();
      }

      if (result.success) {
        analytics.event({
          action: 'google_auth_success',
          category: 'authentication',
          label: variant,
          custom_parameters: {
            user_id: result.user?.id,
            method: 'google',
          },
        });

        onSuccess?.(result.user);
        
        if (variant !== 'link') {
          window.location.href = redirectTo;
        }
      } else {
        throw new Error(result.error || 'Authentication failed');
      }
    } catch (error: any) {
      console.error('Google auth error:', error);
      
      analytics.event({
        action: 'google_auth_error',
        category: 'authentication',
        label: variant,
        custom_parameters: {
          error_message: error.message,
        },
      });

      onError?.(error.message || 'Authentication failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const benefits = [
    {
      icon: Shield,
      title: 'Secure & Safe',
      description: 'Your data is protected with Google\'s security',
    },
    {
      icon: CheckCircle,
      title: 'Quick Setup',
      description: 'No need to remember another password',
    },
    {
      icon: User,
      title: 'Easy Access',
      description: 'Access your bookings from any device',
    },
  ];

  if (isAuthenticated && variant !== 'link') {
    return (
      <NeumorphicCard className="text-center">
        <div className="flex items-center justify-center w-16 h-16 bg-success-100 rounded-full mx-auto mb-4">
          <CheckCircle className="w-8 h-8 text-success-600" />
        </div>
        <h3 className="text-lg font-semibold text-neutral-800 mb-2">
          Already Signed In
        </h3>
        <p className="text-neutral-600 mb-4">
          Welcome back, {user?.name || 'User'}!
        </p>
        <NeumorphicButton
          variant="primary"
          onClick={() => window.location.href = redirectTo}
        >
          Continue to Dashboard
        </NeumorphicButton>
      </NeumorphicCard>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      className="space-y-6"
    >
      {/* Main Auth Button */}
      <NeumorphicCard className="text-center">
        <div className="space-y-6">
          {/* Google Logo */}
          <div className="flex items-center justify-center w-16 h-16 bg-gradient-to-br from-blue-500 to-red-500 rounded-full mx-auto">
            <Chrome className="w-8 h-8 text-white" />
          </div>

          {/* Title */}
          <div>
            <h3 className="text-xl font-semibold text-neutral-800 mb-2">
              {variant === 'signup' ? 'Create Account' : 
               variant === 'link' ? 'Link Account' : 'Welcome Back'}
            </h3>
            <p className="text-neutral-600">
              {variant === 'signup' 
                ? 'Join thousands of adventurers and start your treasure hunt journey'
                : variant === 'link'
                ? 'Link your Google account for easier access'
                : 'Sign in to access your bookings and continue your adventures'
              }
            </p>
          </div>

          {/* Google Auth Button */}
          <NeumorphicButton
            onClick={handleGoogleAuth}
            loading={isLoading}
            disabled={isLoading}
            variant="outline"
            size="lg"
            className="w-full border-neutral-300 hover:border-primary-300 hover:bg-primary-50"
          >
            <div className="flex items-center justify-center gap-3">
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                className="shrink-0"
              >
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
              <span>{getButtonText()}</span>
            </div>
          </NeumorphicButton>

          {/* Privacy Notice */}
          <p className="text-xs text-neutral-500">
            By continuing, you agree to our{' '}
            <a href="/terms" className="text-primary-600 hover:underline">
              Terms of Service
            </a>{' '}
            and{' '}
            <a href="/privacy" className="text-primary-600 hover:underline">
              Privacy Policy
            </a>
          </p>
        </div>
      </NeumorphicCard>

      {/* Benefits Section */}
      {showBenefits && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <NeumorphicCard>
            <h4 className="text-lg font-semibold text-neutral-800 mb-4 text-center">
              Why Sign In with Google?
            </h4>
            <div className="space-y-4">
              {benefits.map((benefit, index) => (
                <motion.div
                  key={benefit.title}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ duration: 0.4, delay: 0.3 + index * 0.1 }}
                  className="flex items-start gap-3"
                >
                  <div className="p-2 bg-primary-100 rounded-lg shrink-0">
                    <benefit.icon className="w-4 h-4 text-primary-600" />
                  </div>
                  <div>
                    <h5 className="font-medium text-neutral-800 mb-1">
                      {benefit.title}
                    </h5>
                    <p className="text-sm text-neutral-600">
                      {benefit.description}
                    </p>
                  </div>
                </motion.div>
              ))}
            </div>
          </NeumorphicCard>
        </motion.div>
      )}

      {/* Alternative Options */}
      {variant !== 'link' && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.4 }}
          className="text-center"
        >
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-neutral-300" />
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-neutral-50 text-neutral-500">
                Or continue with email
              </span>
            </div>
          </div>
          
          <div className="mt-6">
            <NeumorphicButton
              variant="ghost"
              size="lg"
              className="w-full"
              onClick={() => {
                // Navigate to email auth form
                window.location.href = variant === 'signup' ? '/signup' : '/signin';
              }}
            >
              <Mail className="w-4 h-4 mr-2" />
              Use Email Instead
            </NeumorphicButton>
          </div>
        </motion.div>
      )}
    </motion.div>
  );
}
