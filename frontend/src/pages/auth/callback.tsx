/**
 * OAuth2 Callback Handler for Keycloak Authentication
 * Handles the authorization code exchange and token storage
 */

import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { authService } from '../../lib/services/auth-service';

interface CallbackState {
  loading: boolean;
  error: string | null;
  success: boolean;
}

export default function AuthCallback() {
  const router = useRouter();
  const [state, setState] = useState<CallbackState>({
    loading: true,
    error: null,
    success: false
  });

  useEffect(() => {
    const handleCallback = async () => {
      try {
        const { code, error, error_description } = router.query;

        // Handle OAuth2 error responses
        if (error) {
          setState({
            loading: false,
            error: error_description as string || error as string,
            success: false
          });
          return;
        }

        // Handle authorization code
        if (code && typeof code === 'string') {
          // Exchange authorization code for tokens
          const tokenResponse = await exchangeCodeForTokens(code);
          
          if (tokenResponse.success) {
            setState({
              loading: false,
              error: null,
              success: true
            });

            // Redirect to intended destination or dashboard
            const returnUrl = sessionStorage.getItem('auth_return_url') || '/dashboard';
            sessionStorage.removeItem('auth_return_url');
            
            setTimeout(() => {
              router.replace(returnUrl);
            }, 1000);
          } else {
            setState({
              loading: false,
              error: tokenResponse.error || 'Authentication failed',
              success: false
            });
          }
        } else {
          setState({
            loading: false,
            error: 'No authorization code received',
            success: false
          });
        }
      } catch (error) {
        console.error('Auth callback error:', error);
        setState({
          loading: false,
          error: 'Authentication failed',
          success: false
        });
      }
    };

    if (router.isReady) {
      handleCallback();
    }
  }, [router.isReady, router.query]);

  /**
   * Exchange authorization code for access tokens
   */
  const exchangeCodeForTokens = async (code: string): Promise<{
    success: boolean;
    error?: string;
  }> => {
    try {
      // Get login info for token endpoint
      const loginInfoResponse = await authService.getLoginInfo();
      if (!(loginInfoResponse.status >= 200 && loginInfoResponse.status < 300) || !loginInfoResponse.data) {
        return { success: false, error: 'Failed to get login configuration' };
      }

      const { tokenUrl, clientId } = loginInfoResponse.data;

      // Exchange code for tokens
      const tokenResponse = await fetch(tokenUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
          grant_type: 'authorization_code',
          client_id: clientId,
          code: code,
          redirect_uri: `${window.location.origin}/auth/callback`,
        }),
      });

      if (!tokenResponse.ok) {
        const errorData = await tokenResponse.json().catch(() => ({}));
        return { 
          success: false, 
          error: errorData.error_description || 'Token exchange failed' 
        };
      }

      const tokens = await tokenResponse.json();

      // Store tokens using auth service
      authService.setTokens({
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
        tokenType: 'Bearer',
        expiresIn: tokens.expires_in,
        user: {
          id: '', // Will be populated from token introspection
          referenceId: '', // Will be populated from token introspection
          email: '',
          firstName: '',
          lastName: '',
          roles: [],
          active: true
        }
      });

      return { success: true };
    } catch (error) {
      console.error('Token exchange error:', error);
      return { success: false, error: 'Token exchange failed' };
    }
  };

  if (state.loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <h2 className="text-xl font-semibold text-gray-900 mb-2">
            Completing Authentication...
          </h2>
          <p className="text-gray-600">
            Please wait while we log you in.
          </p>
        </div>
      </div>
    );
  }

  if (state.error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center max-w-md mx-auto">
          <div className="bg-red-100 rounded-full h-12 w-12 flex items-center justify-center mx-auto mb-4">
            <svg className="h-6 w-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-gray-900 mb-2">
            Authentication Failed
          </h2>
          <p className="text-gray-600 mb-6">
            {state.error}
          </p>
          <button
            onClick={() => router.push('/auth/login')}
            className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700 transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  if (state.success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="bg-green-100 rounded-full h-12 w-12 flex items-center justify-center mx-auto mb-4">
            <svg className="h-6 w-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-gray-900 mb-2">
            Authentication Successful
          </h2>
          <p className="text-gray-600">
            Redirecting you to your dashboard...
          </p>
        </div>
      </div>
    );
  }

  return null;
}
