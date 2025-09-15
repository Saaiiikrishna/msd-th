'use client';

import React, { useState } from 'react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { Shield, Cookie, Lock, BarChart3, X } from 'lucide-react';

interface CookieConsentDialogProps {
  isOpen: boolean;
  onConsentChange: (consents: CookieConsents) => void;
  onClose: () => void;
}

export interface CookieConsents {
  functional: boolean;
  analytics: boolean;
  marketing: boolean;
}

const CookieConsentDialog: React.FC<CookieConsentDialogProps> = ({
  isOpen,
  onConsentChange,
  onClose
}) => {
  const [consents, setConsents] = useState<CookieConsents>({
    functional: false,
    analytics: false,
    marketing: false
  });

  const handleConsentChange = (type: keyof CookieConsents, value: boolean) => {
    setConsents(prev => ({
      ...prev,
      [type]: value
    }));
  };

  const handleAcceptAll = () => {
    const allConsents = { functional: true, analytics: true, marketing: true };
    setConsents(allConsents);
    onConsentChange(allConsents);
    onClose();
  };

  const handleAcceptSelected = () => {
    onConsentChange(consents);
    onClose();
  };

  const handleRejectAll = () => {
    const noConsents = { functional: false, analytics: false, marketing: false };
    setConsents(noConsents);
    onConsentChange(noConsents);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="w-full max-w-2xl max-h-[80vh] overflow-y-auto m-4">
        <NeumorphicCard className="p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Cookie className="h-6 w-6 text-primary-600" />
              <h2 className="text-xl font-semibold">Cookie Preferences</h2>
            </div>
            <button
              onClick={onClose}
              className="p-1 hover:bg-gray-100 rounded-full transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
          <p className="text-gray-600 mb-6">
            We use cookies to enhance your experience and provide personalized content. 
            Please choose which types of cookies you'd like to allow.
          </p>

          <div className="space-y-4">
            {/* Functional Cookies */}
            <div className="border border-gray-200 rounded-lg p-4">
              <div className="flex items-start gap-3">
                <Shield className="h-5 w-5 text-green-600 mt-1" />
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <h3 className="font-semibold">Functional Cookies</h3>
                    <input
                      type="checkbox"
                      checked={consents.functional}
                      onChange={(e) => handleConsentChange('functional', e.target.checked)}
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                    />
                  </div>
                  <p className="text-sm text-gray-600 mt-1">
                    Essential for website functionality, including authentication and security. 
                    These cookies enable secure login and protect your account.
                  </p>
                </div>
              </div>
            </div>

            {/* Analytics Cookies */}
            <div className="border border-gray-200 rounded-lg p-4">
              <div className="flex items-start gap-3">
                <BarChart3 className="h-5 w-5 text-blue-600 mt-1" />
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <h3 className="font-semibold">Analytics Cookies</h3>
                    <input
                      type="checkbox"
                      checked={consents.analytics}
                      onChange={(e) => handleConsentChange('analytics', e.target.checked)}
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                    />
                  </div>
                  <p className="text-sm text-gray-600 mt-1">
                    Help us understand how you use our website to improve performance and user experience.
                    All data is anonymized and used for statistical purposes only.
                  </p>
                </div>
              </div>
            </div>

            {/* Marketing Cookies */}
            <div className="border border-gray-200 rounded-lg p-4">
              <div className="flex items-start gap-3">
                <Lock className="h-5 w-5 text-purple-600 mt-1" />
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <h3 className="font-semibold">Marketing Cookies</h3>
                    <input
                      type="checkbox"
                      checked={consents.marketing}
                      onChange={(e) => handleConsentChange('marketing', e.target.checked)}
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                    />
                  </div>
                  <p className="text-sm text-gray-600 mt-1">
                    Used to deliver personalized advertisements and track campaign effectiveness.
                    These help us show you relevant content and offers.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-col sm:flex-row gap-3 mt-6">
            <NeumorphicButton
              onClick={handleAcceptAll}
              variant="primary"
              className="flex-1"
            >
              Accept All Cookies
            </NeumorphicButton>
            <NeumorphicButton
              onClick={handleAcceptSelected}
              variant="secondary"
              className="flex-1"
            >
              Accept Selected
            </NeumorphicButton>
            <NeumorphicButton
              onClick={handleRejectAll}
              variant="secondary"
              className="flex-1"
            >
              Reject All
            </NeumorphicButton>
          </div>

          {/* Cookie Policy Link */}
          <div className="mt-4 text-center text-sm text-gray-500">
            For more information, please read our{' '}
            <a 
              href="/privacy-policy" 
              className="text-primary-600 hover:text-primary-700 underline"
              target="_blank"
              rel="noopener noreferrer"
            >
              Cookie Policy
            </a>.
          </div>
        </NeumorphicCard>
      </div>
    </div>
  );
};

export default CookieConsentDialog;
