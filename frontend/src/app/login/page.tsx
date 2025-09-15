'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { Mail, ArrowRight, Chrome, ArrowLeft } from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { GoogleAuth } from '@/components/auth/google-auth';

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  const handleGoogleSuccess = () => {
    router.push('/dashboard');
  };

  const handleGoogleError = (error: string) => {
    setError(error);
  };

  return (
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
            <Link href="/" className="flex items-center text-neutral-600 hover:text-neutral-800 transition-colors">
              <ArrowLeft className="w-4 h-4 mr-2" />
              Back to home
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
            <h1 className="text-2xl font-bold text-neutral-800 mb-2">Welcome Back</h1>
            <p className="text-neutral-600 text-sm">Choose your preferred sign-in method</p>
          </div>

          {/* Google Auth */}
          <GoogleAuth
            variant="signin"
            onSuccess={handleGoogleSuccess}
            onError={handleGoogleError}
            showBenefits={false}
          />

          {/* Divider */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-neutral-200" />
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-4 bg-neumorphic-light-bg text-neutral-500">or</span>
            </div>
          </div>

          {/* Email Login Button */}
          <NeumorphicCard>
            <div className="p-4 text-center space-y-3">
              <div className="flex items-center justify-center w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full mx-auto">
                <Mail className="w-6 h-6 text-white" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-neutral-800 mb-1">
                  Email & Password
                </h3>
                <p className="text-neutral-600 text-xs mb-3">
                  Sign in with your email and password
                </p>
              </div>
              <Link href="/login/email">
                <NeumorphicButton
                  variant="secondary"
                  size="md"
                  className="w-full"
                >
                  <Mail className="w-4 h-4 mr-2" />
                  Continue with Email
                  <ArrowRight className="ml-2 h-4 w-4" />
                </NeumorphicButton>
              </Link>
            </div>
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
        </motion.div>
      </div>
    </div>
  );
}
