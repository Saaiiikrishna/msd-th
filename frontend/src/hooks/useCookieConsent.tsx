'use client';

import { useState, useEffect, useCallback } from 'react';
import { CookieConsents } from '@/components/auth/cookie-consent-dialog';

interface CookieConsentState {
  consents: CookieConsents;
  hasConsented: boolean;
  showDialog: boolean;
}

const COOKIE_CONSENT_KEY = 'cookie-consents';
const COOKIE_CONSENT_VERSION = '1.0';

export const useCookieConsent = () => {
  const [state, setState] = useState<CookieConsentState>({
    consents: {
      functional: false,
      analytics: false,
      marketing: false
    },
    hasConsented: false,
    showDialog: false
  });

  // Load saved consents on mount
  useEffect(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem(COOKIE_CONSENT_KEY);
      if (saved) {
        try {
          const parsed = JSON.parse(saved);
          if (parsed.version === COOKIE_CONSENT_VERSION) {
            setState({
              consents: parsed.consents,
              hasConsented: true,
              showDialog: false
            });
            return;
          }
        } catch (error) {
          console.warn('Failed to parse saved cookie consents:', error);
        }
      }
      
      // Show dialog if no valid consents found
      setState(prev => ({ ...prev, showDialog: true }));
    }
  }, []);

  const updateConsents = useCallback((newConsents: CookieConsents) => {
    const consentData = {
      version: COOKIE_CONSENT_VERSION,
      consents: newConsents,
      timestamp: new Date().toISOString()
    };

    // Save to localStorage
    if (typeof window !== 'undefined') {
      localStorage.setItem(COOKIE_CONSENT_KEY, JSON.stringify(consentData));
    }

    setState({
      consents: newConsents,
      hasConsented: true,
      showDialog: false
    });
  }, []);

  const showConsentDialog = useCallback(() => {
    setState(prev => ({ ...prev, showDialog: true }));
  }, []);

  const hideConsentDialog = useCallback(() => {
    setState(prev => ({ ...prev, showDialog: false }));
  }, []);

  const resetConsents = useCallback(() => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(COOKIE_CONSENT_KEY);
    }
    setState({
      consents: {
        functional: false,
        analytics: false,
        marketing: false
      },
      hasConsented: false,
      showDialog: true
    });
  }, []);

  // Helper functions for specific consent checks
  const hasFunctionalConsent = useCallback(() => {
    return state.hasConsented && state.consents.functional;
  }, [state.hasConsented, state.consents.functional]);

  const hasAnalyticsConsent = useCallback(() => {
    return state.hasConsented && state.consents.analytics;
  }, [state.hasConsented, state.consents.analytics]);

  const hasMarketingConsent = useCallback(() => {
    return state.hasConsented && state.consents.marketing;
  }, [state.hasConsented, state.consents.marketing]);

  // Get consent header value for API requests
  const getCookieConsentHeader = useCallback(() => {
    return hasFunctionalConsent() ? 'true' : 'false';
  }, [hasFunctionalConsent]);

  return {
    // State
    consents: state.consents,
    hasConsented: state.hasConsented,
    showDialog: state.showDialog,

    // Actions
    updateConsents,
    showConsentDialog,
    hideConsentDialog,
    resetConsents,

    // Helpers
    hasFunctionalConsent,
    hasAnalyticsConsent,
    hasMarketingConsent,
    getCookieConsentHeader
  };
};
