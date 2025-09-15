import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { analytics } from '@/lib/analytics';

declare global {
  interface Window {
    google: any;
    gapi: any;
  }
}

interface GoogleUser {
  id: string;
  email: string;
  name: string;
  picture: string;
  verified_email: boolean;
}

interface AuthResult {
  success: boolean;
  user?: GoogleUser;
  token?: string;
  error?: string;
}

export function useGoogleAuth() {
  const [isLoaded, setIsLoaded] = useState(false);
  const [user, setUser] = useState<GoogleUser | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const queryClient = useQueryClient();

  // Load Google API
  useEffect(() => {
    const loadGoogleAPI = async () => {
      if (window.google) {
        setIsLoaded(true);
        return;
      }

      try {
        // Load Google Identity Services
        const script = document.createElement('script');
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        
        script.onload = () => {
          window.google.accounts.id.initialize({
            client_id: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID,
            callback: handleCredentialResponse,
            auto_select: false,
            cancel_on_tap_outside: true,
          });
          setIsLoaded(true);
        };

        document.head.appendChild(script);
      } catch (error) {
        console.error('Failed to load Google API:', error);
      }
    };

    loadGoogleAPI();
  }, []);

  // Check existing authentication
  const { data: authData } = useQuery({
    queryKey: ['auth-status'],
    queryFn: async () => {
      try {
        const response = await apiClient.get('/api/auth/me');
        return response.data;
      } catch (error) {
        return null;
      }
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  useEffect(() => {
    if (authData?.user) {
      setUser(authData.user);
      setIsAuthenticated(true);
      analytics.setUserProperties({
        user_id: authData.user.id,
        email: authData.user.email,
        name: authData.user.name,
      });
    }
  }, [authData]);

  const handleCredentialResponse = async (response: any) => {
    try {
      const result = await signInWithCredential(response.credential);
      if (result.success) {
        setUser(result.user!);
        setIsAuthenticated(true);
        queryClient.invalidateQueries({ queryKey: ['auth-status'] });
      }
    } catch (error) {
      console.error('Credential response error:', error);
    }
  };

  const signInWithCredential = async (credential: string): Promise<AuthResult> => {
    try {
      const response = await apiClient.post('/api/auth/google', {
        credential,
      });

      return {
        success: true,
        user: response.data.user,
        token: response.data.token,
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.response?.data?.message || 'Authentication failed',
      };
    }
  };

  const signInMutation = useMutation({
    mutationFn: async (): Promise<AuthResult> => {
      return new Promise((resolve) => {
        if (!window.google) {
          resolve({
            success: false,
            error: 'Google API not loaded',
          });
          return;
        }

        window.google.accounts.id.prompt((notification: any) => {
          if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
            // Fallback to popup
            window.google.accounts.oauth2.initTokenClient({
              client_id: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID,
              scope: 'email profile',
              callback: async (response: any) => {
                if (response.access_token) {
                  try {
                    const userInfo = await fetchGoogleUserInfo(response.access_token);
                    const authResult = await authenticateWithBackend(userInfo);
                    resolve(authResult);
                  } catch (error) {
                    resolve({
                      success: false,
                      error: 'Failed to authenticate',
                    });
                  }
                } else {
                  resolve({
                    success: false,
                    error: 'No access token received',
                  });
                }
              },
            }).requestAccessToken();
          }
        });
      });
    },
    onSuccess: (result) => {
      if (result.success) {
        setUser(result.user!);
        setIsAuthenticated(true);
        queryClient.invalidateQueries({ queryKey: ['auth-status'] });
      }
    },
  });

  const fetchGoogleUserInfo = async (accessToken: string): Promise<GoogleUser> => {
    const response = await fetch(
      `https://www.googleapis.com/oauth2/v2/userinfo?access_token=${accessToken}`
    );
    
    if (!response.ok) {
      throw new Error('Failed to fetch user info');
    }
    
    return response.json();
  };

  const authenticateWithBackend = async (googleUser: GoogleUser): Promise<AuthResult> => {
    try {
      const response = await apiClient.post('/api/auth/google-oauth', {
        user: googleUser,
      });

      return {
        success: true,
        user: response.data.user,
        token: response.data.token,
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.response?.data?.message || 'Authentication failed',
      };
    }
  };

  const linkAccountMutation = useMutation({
    mutationFn: async (): Promise<AuthResult> => {
      if (!isAuthenticated) {
        return {
          success: false,
          error: 'Must be logged in to link account',
        };
      }

      return signInMutation.mutateAsync();
    },
    onSuccess: (result) => {
      if (result.success) {
        queryClient.invalidateQueries({ queryKey: ['auth-status'] });
      }
    },
  });

  const signOutMutation = useMutation({
    mutationFn: async () => {
      await apiClient.post('/api/auth/signout');
      
      // Sign out from Google
      if (window.google) {
        window.google.accounts.id.disableAutoSelect();
      }
    },
    onSuccess: () => {
      setUser(null);
      setIsAuthenticated(false);
      queryClient.clear();
      
      // Redirect to home page
      window.location.href = '/';
    },
  });

  return {
    isLoaded,
    user,
    isAuthenticated,
    signInWithGoogle: signInMutation.mutateAsync,
    linkGoogleAccount: linkAccountMutation.mutateAsync,
    signOut: signOutMutation.mutateAsync,
    isSigningIn: signInMutation.isPending,
    isLinking: linkAccountMutation.isPending,
    isSigningOut: signOutMutation.isPending,
  };
}
