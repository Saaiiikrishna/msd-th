'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { Eye, EyeOff, Mail, Lock, ArrowRight, AlertCircle, ArrowLeft, Cookie } from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { useAuth } from '@/hooks/useAuth';
import { useCookieConsent } from '@/hooks/useCookieConsent';
import CookieConsentDialog from '@/components/auth/cookie-consent-dialog';

interface LoginForm {
  email: string;
  password: string;
  rememberMe: boolean;
}

export default function EmailLoginPage() {
  const router = useRouter();
  const { login, isLoading } = useAuth();
  const {
    showDialog,
    consents,
    updateConsents,
    hideConsentDialog,
    hasFunctionalConsent,
    getCookieConsentHeader
  } = useCookieConsent();

  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loginSuccess, setLoginSuccess] = useState<{ cookiesSet?: boolean; clientInfo?: any } | null>(null);
  const [form, setForm] = useState<LoginForm>({
    email: '',
    password: '',
    rememberMe: false,
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!form.email || !form.password) {
      setError('Please fill in all fields');
      return;
    }

    try {
      // Use cookie consent for enhanced security
      const cookieConsent = hasFunctionalConsent();

      const result = await login({
        email: form.email,
        password: form.password,
        rememberMe: form.rememberMe,
      }, cookieConsent);

      if (result.success) {
        setLoginSuccess({
          cookiesSet: result.cookiesSet,
          clientInfo: result.clientInfo
        });
        router.push('/dashboard');
      } else {
        setError(result.error || 'Login failed. Please try again.');
      }
    } catch (err) {
      setError('An unexpected error occurred. Please try again.');
    }
  };

  const handleInputChange = (field: keyof LoginForm, value: string | boolean) => {
    setForm(prev => ({ ...prev, [field]: value }));
    if (error) setError(null);
  };

  return (
    <>
      <div className="min-h-screen bg-gradient-to-br from-neumorphic-light-bg via-neumorphic-light-bg to-primary-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="space-y-8"
        >
          {/* Back Button */}
          <div className="flex items-center">
            <Link href="/login" className="flex items-center text-neutral-600 hover:text-neutral-800 transition-colors">
              <ArrowLeft className="w-4 h-4 mr-2" />
              Back to login options
            </Link>
          </div>

          {/* Header */}
          <div className="text-center">
            <Link href="/" className="inline-block mb-6">
              <div className="flex items-center justify-center space-x-3">
                <div className="w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-600 rounded-xl flex items-center justify-center shadow-lg">
                  <span className="text-white font-bold text-lg">DR</span>
                </div>
                <div>
                  <div className="text-xl font-bold text-neutral-800">Dream Rider</div>
                  <div className="text-sm text-neutral-600">MySillyDreams</div>
                </div>
              </div>
            </Link>
            <h1 className="text-3xl font-bold text-neutral-800 mb-2">Sign In with Email</h1>
            <p className="text-neutral-600">Enter your credentials to continue</p>
          </div>

          {/* Login Form */}
          <NeumorphicCard>
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Error Message */}
              {error && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex items-center space-x-2 p-3 bg-red-50 border border-red-200 rounded-lg"
                >
                  <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0" />
                  <span className="text-red-700 text-sm">{error}</span>
                </motion.div>
              )}

              {/* Email Field */}
              <div className="space-y-2">
                <label htmlFor="email" className="block text-sm font-medium text-neutral-700">
                  Email Address
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Mail className="h-5 w-5 text-neutral-400" />
                  </div>
                  <input
                    id="email"
                    type="email"
                    value={form.email}
                    onChange={(e) => handleInputChange('email', e.target.value)}
                    className="block w-full pl-10 pr-3 py-3 border border-neutral-200 rounded-lg bg-white/50 backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200"
                    placeholder="Enter your email"
                    required
                  />
                </div>
              </div>

              {/* Password Field */}
              <div className="space-y-2">
                <label htmlFor="password" className="block text-sm font-medium text-neutral-700">
                  Password
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Lock className="h-5 w-5 text-neutral-400" />
                  </div>
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    value={form.password}
                    onChange={(e) => handleInputChange('password', e.target.value)}
                    className="block w-full pl-10 pr-12 py-3 border border-neutral-200 rounded-lg bg-white/50 backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200"
                    placeholder="Enter your password"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center"
                  >
                    {showPassword ? (
                      <EyeOff className="h-5 w-5 text-neutral-400 hover:text-neutral-600" />
                    ) : (
                      <Eye className="h-5 w-5 text-neutral-400 hover:text-neutral-600" />
                    )}
                  </button>
                </div>
              </div>

              {/* Remember Me & Forgot Password */}
              <div className="flex items-center justify-between">
                <label className="flex items-center">
                  <input
                    type="checkbox"
                    checked={form.rememberMe}
                    onChange={(e) => handleInputChange('rememberMe', e.target.checked)}
                    className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-neutral-300 rounded"
                  />
                  <span className="ml-2 text-sm text-neutral-600">Remember me</span>
                </label>
                <Link
                  href="/forgot-password"
                  className="text-sm text-primary-600 hover:text-primary-500 transition-colors"
                >
                  Forgot password?
                </Link>
              </div>

              {/* Submit Button */}
              <NeumorphicButton
                type="submit"
                variant="primary"
                size="lg"
                className="w-full"
                disabled={isLoading}
              >
                {isLoading ? (
                  <div className="flex items-center justify-center">
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2" />
                    Signing in...
                  </div>
                ) : (
                  <div className="flex items-center justify-center">
                    Sign In
                    <ArrowRight className="ml-2 h-5 w-5" />
                  </div>
                )}
              </NeumorphicButton>
            </form>
          </NeumorphicCard>

          {/* Sign Up Link */}
          <div className="text-center">
            <p className="text-neutral-600">
              Don't have an account?{' '}
              <Link
                href="/register"
                className="text-primary-600 hover:text-primary-500 font-medium transition-colors"
              >
                Sign up for free
              </Link>
            </p>
          </div>

          {/* Cookie Consent Success Message */}
          {loginSuccess?.cookiesSet && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="mt-4 p-4 bg-green-50 border border-green-200 rounded-lg"
            >
              <div className="flex items-center gap-2 text-green-800">
                <Cookie className="h-4 w-4" />
                <span className="text-sm font-medium">
                  Enhanced Security Enabled
                </span>
              </div>
              <p className="text-xs text-green-600 mt-1">
                Secure cookies have been set for improved authentication security.
                Client: {loginSuccess.clientInfo?.platform || 'web'}
              </p>
            </motion.div>
          )}
        </motion.div>
      </div>
    </div>

    {/* Cookie Consent Dialog */}
    <CookieConsentDialog
      isOpen={showDialog}
      onConsentChange={updateConsents}
      onClose={hideConsentDialog}
    />
    </>
  );
}
