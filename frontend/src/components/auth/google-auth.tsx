'use client';

import { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { Chrome, Shield, User, Mail, ChevronDown, CheckCircle, ArrowRight } from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { useCookieConsent } from '@/hooks/useCookieConsent';
import { useAuth } from '@/hooks/useAuth';

interface GoogleAuthProps {
  onSuccess?: (user: any) => void;
  onError?: (error: string) => void;
  redirectTo?: string;
  buttonText?: string;
  variant?: 'signin' | 'signup' | 'link';
  showBenefits?: boolean;
}

interface DetectedUser {
  name: string;
  email: string;
  picture: string;
  id: string;
}

interface GoogleAccount {
  id: string;
  name: string;
  email: string;
  picture: string;
}

declare global {
  interface Window {
    google: any;
  }
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
  const [isGoogleLoaded, setIsGoogleLoaded] = useState(false);
  const [detectedUser, setDetectedUser] = useState<DetectedUser | null>(null);
  const [availableAccounts, setAvailableAccounts] = useState<GoogleAccount[]>([]);
  const [showAccountDropdown, setShowAccountDropdown] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState<GoogleAccount | null>(null);
  const googleButtonRef = useRef<HTMLDivElement>(null);
  const { getCookieConsentHeader } = useCookieConsent();
  const { refreshUser } = useAuth();

  // Load Google Identity Services and API
  useEffect(() => {
    const loadGoogleScripts = () => {
      let scriptsLoaded = 0;
      const totalScripts = 2;

      const checkAllLoaded = () => {
        scriptsLoaded++;
        if (scriptsLoaded === totalScripts) {
          setIsGoogleLoaded(true);
        }
      };

      // Load Google Identity Services (for authentication)
      if (!window.google) {
        const gsiScript = document.createElement('script');
        gsiScript.src = 'https://accounts.google.com/gsi/client';
        gsiScript.async = true;
        gsiScript.defer = true;
        gsiScript.onload = checkAllLoaded;
        document.head.appendChild(gsiScript);
      } else {
        checkAllLoaded();
      }

      // Load Google API (for account detection)
      if (!window.gapi) {
        const gapiScript = document.createElement('script');
        gapiScript.src = 'https://apis.google.com/js/api.js';
        gapiScript.async = true;
        gapiScript.defer = true;
        gapiScript.onload = checkAllLoaded;
        document.head.appendChild(gapiScript);
      } else {
        checkAllLoaded();
      }
    };

    loadGoogleScripts();
  }, []);

  // Initialize Google Sign-In without auto-prompting
  useEffect(() => {
    if (!isGoogleLoaded || !window.google || !googleButtonRef.current) return;

    try {
      // Initialize Google Identity Services but disable all auto-prompting
      window.google.accounts.id.initialize({
        client_id: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID!,
        callback: handleCredentialResponse,
        auto_select: false,
        cancel_on_tap_outside: true,
        use_fedcm_for_prompt: false, // Disable FedCM popup
        prompt_parent_id: null, // Disable prompt parent
        state_cookie_domain: null, // Disable state cookies
      });

      // Render default Google button (no auto-detection popup)
      if (googleButtonRef.current) {
        window.google.accounts.id.renderButton(googleButtonRef.current, {
          theme: 'outline',
          size: 'large',
          width: '100%',
          text: variant === 'signup' ? 'signup_with' : 'signin_with',
          shape: 'rectangular',
          logo_alignment: 'left',
        });
      }

      // Disable any automatic prompting
      window.google.accounts.id.disableAutoSelect();

    } catch (error) {
      console.error('Google Sign-In initialization error:', error);
      onError?.('Failed to initialize Google Sign-In');
    }
  }, [isGoogleLoaded, variant]);

  // Detect Google accounts without triggering popups
  const detectGoogleAccounts = async () => {
    try {
      // Check if user is signed in to Google without triggering popup
      if (window.gapi && window.gapi.auth2) {
        const authInstance = window.gapi.auth2.getAuthInstance();
        if (authInstance && authInstance.isSignedIn.get()) {
          const currentUser = authInstance.currentUser.get();
          const profile = currentUser.getBasicProfile();

          const account: GoogleAccount = {
            id: profile.getId(),
            name: profile.getName(),
            email: profile.getEmail(),
            picture: profile.getImageUrl(),
          };

          setDetectedUser(account);
          setSelectedAccount(account);
          setAvailableAccounts([account]);
        }
      }

      // Alternative: Check for accounts using the newer API
      if (window.google?.accounts?.oauth2) {
        // This won't trigger popups, just checks for existing sessions
        try {
          const tokenClient = window.google.accounts.oauth2.initTokenClient({
            client_id: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID!,
            scope: 'email profile',
            callback: () => {}, // Empty callback to prevent automatic execution
          });

          // Check if there are any granted scopes without requesting new ones
          // This is a passive check that won't trigger popups
        } catch (error) {
          console.log('No existing Google session detected');
        }
      }
    } catch (error) {
      console.log('No Google accounts detected:', error);
    }
  };

  // Load Google API for account detection
  useEffect(() => {
    const loadGoogleAPI = () => {
      if (window.gapi) {
        window.gapi.load('auth2', () => {
          window.gapi.auth2.init({
            client_id: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID!,
          }).then(() => {
            detectGoogleAccounts();
          });
        });
      }
    };

    if (isGoogleLoaded) {
      loadGoogleAPI();
    }
  }, [isGoogleLoaded]);

  const handleCredentialResponse = async (response: any) => {
    setIsLoading(true);
    
    try {
      // Decode the JWT token to get user info
      const payload = JSON.parse(atob(response.credential.split('.')[1]));
      
      const user = {
        id: payload.sub,
        name: payload.name,
        email: payload.email,
        picture: payload.picture,
        given_name: payload.given_name,
        family_name: payload.family_name
      };

      // Send the credential to backend for verification and authentication
      const backendResponse = await fetch('/api/auth/v1/google-oauth', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Client-Type': 'web',
          'X-Cookie-Consent': getCookieConsentHeader()
        },
        body: JSON.stringify({ credential: response.credential })
      });

      if (backendResponse.ok) {
        const authResult = await backendResponse.json();

        // Update detected user for future visits
        setDetectedUser({
          id: user.id,
          name: user.name,
          email: user.email,
          picture: user.picture
        });

        // Refresh the auth context to update user state
        await refreshUser();

        onSuccess?.(authResult);
      } else {
        const errorData = await backendResponse.json();
        onError?.(errorData.error || 'Google authentication failed');
      }

    } catch (error) {
      console.error('Google authentication error:', error);
      onError?.('Google authentication failed');
    } finally {
      setIsLoading(false);
    }
  };

  // Handle custom button click - use OAuth2 flow instead of prompt
  const handleCustomButtonClick = () => {
    if (window.google?.accounts?.oauth2) {
      // Use OAuth2 popup flow instead of One Tap prompt
      const client = window.google.accounts.oauth2.initTokenClient({
        client_id: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID!,
        scope: 'email profile',
        callback: async (response: any) => {
          if (response.access_token) {
            try {
              // Get user info from Google API
              const userInfoResponse = await fetch(`https://www.googleapis.com/oauth2/v2/userinfo?access_token=${response.access_token}`);
              const userInfo = await userInfoResponse.json();

              // Create a mock credential response for compatibility
              const mockCredential = btoa(JSON.stringify({
                iss: 'https://accounts.google.com',
                sub: userInfo.id,
                email: userInfo.email,
                name: userInfo.name,
                picture: userInfo.picture,
                given_name: userInfo.given_name,
                family_name: userInfo.family_name,
                iat: Math.floor(Date.now() / 1000),
                exp: Math.floor(Date.now() / 1000) + 3600
              }));

              // Call the existing handler with mock credential
              await handleCredentialResponse({ credential: `header.${mockCredential}.signature` });
            } catch (error) {
              console.error('OAuth2 flow error:', error);
              onError?.('Google authentication failed');
            }
          } else {
            onError?.('No access token received from Google');
          }
        },
      });

      client.requestAccessToken();
    }
  };

  const getButtonText = () => {
    if (buttonText) return buttonText;

    // Show detected user name if available
    if (selectedAccount || detectedUser) {
      const user = selectedAccount || detectedUser;
      const firstName = user!.name.split(' ')[0];
      return `Continue as ${firstName}`;
    }

    switch (variant) {
      case 'signup':
        return 'Sign up with Google';
      case 'link':
        return 'Link Google Account';
      default:
        return 'Continue with Google';
    }
  };

  const benefits = [
    {
      icon: Shield,
      title: 'Secure & Fast',
      description: 'Your data is protected with Google\'s security'
    },
    {
      icon: User,
      title: 'Quick Setup',
      description: 'No need to create a new password'
    },
    {
      icon: Mail,
      title: 'Stay Connected',
      description: 'Get updates about your adventures'
    }
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      className="space-y-4"
    >
      {/* Enhanced Google Auth Button */}
      <NeumorphicCard className="overflow-hidden">
        <div className="p-6">
          {detectedUser || selectedAccount ? (
            // Custom button with detected user and dropdown
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.3 }}
              className="space-y-4"
            >
              {/* User Avatar and Info */}
              <div className="flex items-center space-x-4">
                <div className="relative">
                  <img
                    src={(selectedAccount || detectedUser)!.picture}
                    alt={(selectedAccount || detectedUser)!.name}
                    className="w-12 h-12 rounded-full border-2 border-primary-200"
                  />
                  <div className="absolute -bottom-1 -right-1 w-5 h-5 bg-green-500 rounded-full border-2 border-white flex items-center justify-center">
                    <CheckCircle className="w-3 h-3 text-white" />
                  </div>
                </div>
                <div className="flex-1 text-left">
                  <p className="font-semibold text-neutral-800">{(selectedAccount || detectedUser)!.name}</p>
                  <p className="text-sm text-neutral-600">{(selectedAccount || detectedUser)!.email}</p>
                </div>
                {availableAccounts.length > 1 && (
                  <button
                    onClick={() => setShowAccountDropdown(!showAccountDropdown)}
                    className="p-2 hover:bg-neutral-100 rounded-full transition-colors"
                  >
                    <ChevronDown className="w-4 h-4 text-neutral-600" />
                  </button>
                )}
              </div>

              {/* Account Dropdown */}
              {showAccountDropdown && availableAccounts.length > 1 && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="border border-neutral-200 rounded-lg bg-white shadow-lg max-h-48 overflow-y-auto"
                >
                  {availableAccounts.map((account) => (
                    <button
                      key={account.id}
                      onClick={() => {
                        setSelectedAccount(account);
                        setShowAccountDropdown(false);
                      }}
                      className="w-full flex items-center space-x-3 p-3 hover:bg-neutral-50 transition-colors"
                    >
                      <img
                        src={account.picture}
                        alt={account.name}
                        className="w-8 h-8 rounded-full"
                      />
                      <div className="flex-1 text-left">
                        <p className="font-medium text-neutral-800">{account.name}</p>
                        <p className="text-sm text-neutral-600">{account.email}</p>
                      </div>
                      {(selectedAccount?.id === account.id) && (
                        <CheckCircle className="w-4 h-4 text-green-500" />
                      )}
                    </button>
                  ))}
                </motion.div>
              )}

              {/* Enhanced Continue Button */}
              <NeumorphicButton
                onClick={handleCustomButtonClick}
                disabled={isLoading}
                variant="primary"
                size="lg"
                className="w-full"
              >
                <Chrome className="w-5 h-5 mr-3" />
                {isLoading ? 'Signing in...' : getButtonText()}
                <ArrowRight className="w-5 h-5 ml-3" />
              </NeumorphicButton>
            </motion.div>
          ) : (
            // Default Google button when no user detected
            <div className="space-y-4">
              {/* Google Logo */}
              <div className="flex items-center justify-center w-16 h-16 bg-gradient-to-br from-blue-500 via-green-500 to-red-500 rounded-full mx-auto shadow-lg">
                <Chrome className="w-8 h-8 text-white" />
              </div>

              {/* Title */}
              <div className="text-center">
                <h3 className="text-lg font-semibold text-neutral-800 mb-1">
                  {getButtonText()}
                </h3>
                <p className="text-neutral-600 text-sm">
                  {variant === 'signup'
                    ? 'Create your account with Google in seconds'
                    : 'Sign in securely with your Google account'
                  }
                </p>
              </div>

              {/* Google Sign-In Button Container */}
              <div className="w-full">
                {!isGoogleLoaded ? (
                  <NeumorphicButton
                    variant="primary"
                    size="lg"
                    className="w-full"
                    disabled
                  >
                    <div className="flex items-center justify-center">
                      <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2" />
                      Loading Google...
                    </div>
                  </NeumorphicButton>
                ) : (
                  <div ref={googleButtonRef} className="w-full" />
                )}
              </div>

              {isLoading && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="flex items-center justify-center py-2"
                >
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-primary-600 mr-2" />
                  <span className="text-neutral-600">Signing you in...</span>
                </motion.div>
              )}
            </div>
          )}
        </div>
      </NeumorphicCard>

      {/* Benefits Section */}
      {showBenefits && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="grid grid-cols-1 gap-4"
        >
          {benefits.map((benefit, index) => (
            <div key={index} className="flex items-center space-x-3 p-3 bg-white/50 rounded-lg border border-neutral-200">
              <div className="flex items-center justify-center w-10 h-10 bg-primary-100 rounded-full">
                <benefit.icon className="w-5 h-5 text-primary-600" />
              </div>
              <div>
                <h4 className="font-medium text-neutral-800">{benefit.title}</h4>
                <p className="text-sm text-neutral-600">{benefit.description}</p>
              </div>
            </div>
          ))}
        </motion.div>
      )}
    </motion.div>
  );
}
