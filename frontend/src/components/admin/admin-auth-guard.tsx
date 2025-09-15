'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { Shield, AlertTriangle, Loader2 } from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';

interface AdminAuthGuardProps {
  children: React.ReactNode;
}

interface User {
  id: string;
  email: string;
  role: string;
  permissions: string[];
}

export function AdminAuthGuard({ children }: AdminAuthGuardProps) {
  const [isLoading, setIsLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isAuthorized, setIsAuthorized] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    checkAuthAndPermissions();
  }, []);

  const checkAuthAndPermissions = async () => {
    try {
      setIsLoading(true);
      setError(null);

      // Check if user is authenticated
      const token = localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token');
      
      if (!token) {
        setIsAuthenticated(false);
        setIsLoading(false);
        return;
      }

      // Validate token and get user info
      const response = await fetch('/api/auth/me', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        if (response.status === 401) {
          // Token is invalid or expired
          localStorage.removeItem('auth_token');
          sessionStorage.removeItem('auth_token');
          setIsAuthenticated(false);
          setIsLoading(false);
          return;
        }
        throw new Error('Failed to verify authentication');
      }

      const userData = await response.json();
      setUser(userData);
      setIsAuthenticated(true);

      // Check if user has admin permissions
      const hasAdminRole = userData.role === 'admin' || userData.role === 'super_admin';
      const hasAdminPermissions = userData.permissions?.includes('admin_access') || 
                                 userData.permissions?.includes('admin_dashboard');

      if (hasAdminRole || hasAdminPermissions) {
        setIsAuthorized(true);
      } else {
        setIsAuthorized(false);
        setError('You do not have permission to access the admin panel.');
      }

    } catch (err) {
      console.error('Auth check failed:', err);
      setError('Failed to verify authentication. Please try again.');
      setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogin = () => {
    // Redirect to login page with return URL
    const returnUrl = encodeURIComponent(window.location.pathname);
    router.push(`/auth/login?returnUrl=${returnUrl}`);
  };

  const handleRetry = () => {
    checkAuthAndPermissions();
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="min-h-screen bg-neutral-100 flex items-center justify-center">
        <NeumorphicCard className="p-8 text-center">
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
            className="inline-block mb-4"
          >
            <Loader2 className="w-8 h-8 text-primary-500" />
          </motion.div>
          <h2 className="text-xl font-semibold text-neutral-800 mb-2">
            Verifying Access
          </h2>
          <p className="text-neutral-600">
            Please wait while we verify your permissions...
          </p>
        </NeumorphicCard>
      </div>
    );
  }

  // Not authenticated
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-neutral-100 flex items-center justify-center">
        <NeumorphicCard className="p-8 text-center max-w-md">
          <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Shield className="w-8 h-8 text-red-500" />
          </div>
          <h2 className="text-xl font-semibold text-neutral-800 mb-2">
            Authentication Required
          </h2>
          <p className="text-neutral-600 mb-6">
            You need to be logged in to access the admin panel.
          </p>
          <div className="space-y-3">
            <NeumorphicButton 
              onClick={handleLogin}
              className="w-full"
              variant="primary"
            >
              Sign In
            </NeumorphicButton>
            <NeumorphicButton 
              onClick={handleRetry}
              className="w-full"
              variant="outline"
            >
              Retry
            </NeumorphicButton>
          </div>
        </NeumorphicCard>
      </div>
    );
  }

  // Not authorized
  if (!isAuthorized) {
    return (
      <div className="min-h-screen bg-neutral-100 flex items-center justify-center">
        <NeumorphicCard className="p-8 text-center max-w-md">
          <div className="w-16 h-16 bg-yellow-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <AlertTriangle className="w-8 h-8 text-yellow-500" />
          </div>
          <h2 className="text-xl font-semibold text-neutral-800 mb-2">
            Access Denied
          </h2>
          <p className="text-neutral-600 mb-6">
            {error || 'You do not have permission to access this area.'}
          </p>
          <div className="space-y-3">
            <NeumorphicButton 
              onClick={() => router.push('/')}
              className="w-full"
              variant="primary"
            >
              Go to Home
            </NeumorphicButton>
            <NeumorphicButton 
              onClick={handleRetry}
              className="w-full"
              variant="outline"
            >
              Retry
            </NeumorphicButton>
          </div>
          {user && (
            <div className="mt-6 p-4 bg-neutral-50 rounded-lg text-left">
              <h3 className="font-medium text-neutral-800 mb-2">Current User:</h3>
              <p className="text-sm text-neutral-600">Email: {user.email}</p>
              <p className="text-sm text-neutral-600">Role: {user.role}</p>
              {user.permissions && user.permissions.length > 0 && (
                <p className="text-sm text-neutral-600">
                  Permissions: {user.permissions.join(', ')}
                </p>
              )}
            </div>
          )}
        </NeumorphicCard>
      </div>
    );
  }

  // Error state
  if (error && isAuthenticated) {
    return (
      <div className="min-h-screen bg-neutral-100 flex items-center justify-center">
        <NeumorphicCard className="p-8 text-center max-w-md">
          <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <AlertTriangle className="w-8 h-8 text-red-500" />
          </div>
          <h2 className="text-xl font-semibold text-neutral-800 mb-2">
            Something went wrong
          </h2>
          <p className="text-neutral-600 mb-6">
            {error}
          </p>
          <div className="space-y-3">
            <NeumorphicButton 
              onClick={handleRetry}
              className="w-full"
              variant="primary"
            >
              Try Again
            </NeumorphicButton>
            <NeumorphicButton 
              onClick={() => router.push('/')}
              className="w-full"
              variant="outline"
            >
              Go to Home
            </NeumorphicButton>
          </div>
        </NeumorphicCard>
      </div>
    );
  }

  // Authorized - render children
  return <>{children}</>;
}
