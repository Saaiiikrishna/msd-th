'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  CreditCard, 
  Lock, 
  Eye, 
  EyeOff,
  CheckCircle,
  AlertCircle,
  Smartphone,
  Building,
  Wallet
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { usePaymentProcessing } from '@/hooks/use-payment';

interface PaymentFormProps {
  paymentMethod: string;
}

export function PaymentForm({ paymentMethod }: PaymentFormProps) {
  const [formData, setFormData] = useState({
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    cardholderName: '',
    upiId: '',
    bankCode: '',
    walletProvider: '',
    saveCard: false,
  });
  
  const [showCVV, setShowCVV] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isProcessing, setIsProcessing] = useState(false);

  const { processPayment, isLoading } = usePaymentProcessing();

  // Card number formatting
  const formatCardNumber = (value: string) => {
    const v = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    const matches = v.match(/\d{4,16}/g);
    const match = matches && matches[0] || '';
    const parts = [];
    for (let i = 0, len = match.length; i < len; i += 4) {
      parts.push(match.substring(i, i + 4));
    }
    if (parts.length) {
      return parts.join(' ');
    } else {
      return v;
    }
  };

  // Expiry date formatting
  const formatExpiryDate = (value: string) => {
    const v = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    if (v.length >= 2) {
      return v.substring(0, 2) + '/' + v.substring(2, 4);
    }
    return v;
  };

  // Card type detection
  const getCardType = (number: string) => {
    const num = number.replace(/\s/g, '');
    if (/^4/.test(num)) return 'visa';
    if (/^5[1-5]/.test(num)) return 'mastercard';
    if (/^3[47]/.test(num)) return 'amex';
    if (/^6/.test(num)) return 'rupay';
    return 'unknown';
  };

  const handleInputChange = (field: string, value: string) => {
    let formattedValue = value;
    
    if (field === 'cardNumber') {
      formattedValue = formatCardNumber(value);
    } else if (field === 'expiryDate') {
      formattedValue = formatExpiryDate(value);
    } else if (field === 'cvv') {
      formattedValue = value.replace(/[^0-9]/g, '').substring(0, 4);
    }
    
    setFormData(prev => ({ ...prev, [field]: formattedValue }));
    
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (paymentMethod === 'cards') {
      if (!formData.cardNumber || formData.cardNumber.replace(/\s/g, '').length < 13) {
        newErrors.cardNumber = 'Please enter a valid card number';
      }
      if (!formData.expiryDate || !/^\d{2}\/\d{2}$/.test(formData.expiryDate)) {
        newErrors.expiryDate = 'Please enter a valid expiry date (MM/YY)';
      }
      if (!formData.cvv || formData.cvv.length < 3) {
        newErrors.cvv = 'Please enter a valid CVV';
      }
      if (!formData.cardholderName.trim()) {
        newErrors.cardholderName = 'Please enter the cardholder name';
      }
    } else if (paymentMethod === 'upi') {
      if (!formData.upiId || !/^[\w.-]+@[\w.-]+$/.test(formData.upiId)) {
        newErrors.upiId = 'Please enter a valid UPI ID';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) return;
    
    setIsProcessing(true);
    
    try {
      await processPayment({
        method: paymentMethod,
        data: formData,
      });
    } catch (error) {
      console.error('Payment error:', error);
    } finally {
      setIsProcessing(false);
    }
  };

  // Render different forms based on payment method
  const renderPaymentForm = () => {
    switch (paymentMethod) {
      case 'cards':
        return (
          <div className="space-y-4">
            {/* Card Number */}
            <div>
              <label className="block text-sm font-medium text-neutral-800 mb-2">
                Card Number
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={formData.cardNumber}
                  onChange={(e) => handleInputChange('cardNumber', e.target.value)}
                  placeholder="1234 5678 9012 3456"
                  maxLength={19}
                  className={`w-full p-3 pl-12 border rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500 ${
                    errors.cardNumber ? 'border-error-500' : 'border-neutral-300'
                  }`}
                />
                <CreditCard className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-neutral-400" />
                {formData.cardNumber && (
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                    <img
                      src={`/images/cards/${getCardType(formData.cardNumber)}.svg`}
                      alt="Card type"
                      className="w-8 h-5"
                      onError={(e) => {
                        (e.target as HTMLImageElement).style.display = 'none';
                      }}
                    />
                  </div>
                )}
              </div>
              {errors.cardNumber && (
                <p className="text-sm text-error-500 mt-1 flex items-center gap-1">
                  <AlertCircle className="w-4 h-4" />
                  {errors.cardNumber}
                </p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              {/* Expiry Date */}
              <div>
                <label className="block text-sm font-medium text-neutral-800 mb-2">
                  Expiry Date
                </label>
                <input
                  type="text"
                  value={formData.expiryDate}
                  onChange={(e) => handleInputChange('expiryDate', e.target.value)}
                  placeholder="MM/YY"
                  maxLength={5}
                  className={`w-full p-3 border rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500 ${
                    errors.expiryDate ? 'border-error-500' : 'border-neutral-300'
                  }`}
                />
                {errors.expiryDate && (
                  <p className="text-sm text-error-500 mt-1">{errors.expiryDate}</p>
                )}
              </div>

              {/* CVV */}
              <div>
                <label className="block text-sm font-medium text-neutral-800 mb-2">
                  CVV
                </label>
                <div className="relative">
                  <input
                    type={showCVV ? 'text' : 'password'}
                    value={formData.cvv}
                    onChange={(e) => handleInputChange('cvv', e.target.value)}
                    placeholder="123"
                    maxLength={4}
                    className={`w-full p-3 pr-12 border rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500 ${
                      errors.cvv ? 'border-error-500' : 'border-neutral-300'
                    }`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowCVV(!showCVV)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-neutral-400 hover:text-neutral-600"
                  >
                    {showCVV ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {errors.cvv && (
                  <p className="text-sm text-error-500 mt-1">{errors.cvv}</p>
                )}
              </div>
            </div>

            {/* Cardholder Name */}
            <div>
              <label className="block text-sm font-medium text-neutral-800 mb-2">
                Cardholder Name
              </label>
              <input
                type="text"
                value={formData.cardholderName}
                onChange={(e) => handleInputChange('cardholderName', e.target.value)}
                placeholder="John Doe"
                className={`w-full p-3 border rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500 ${
                  errors.cardholderName ? 'border-error-500' : 'border-neutral-300'
                }`}
              />
              {errors.cardholderName && (
                <p className="text-sm text-error-500 mt-1">{errors.cardholderName}</p>
              )}
            </div>

            {/* Save Card Option */}
            <div className="flex items-center gap-3">
              <input
                type="checkbox"
                id="saveCard"
                checked={formData.saveCard}
                onChange={(e) => setFormData(prev => ({ ...prev, saveCard: e.target.checked }))}
                className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
              />
              <label htmlFor="saveCard" className="text-sm text-neutral-700">
                Save this card for future payments
              </label>
            </div>
          </div>
        );

      case 'upi':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-neutral-800 mb-2">
                UPI ID
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={formData.upiId}
                  onChange={(e) => handleInputChange('upiId', e.target.value)}
                  placeholder="yourname@paytm"
                  className={`w-full p-3 pl-12 border rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500 ${
                    errors.upiId ? 'border-error-500' : 'border-neutral-300'
                  }`}
                />
                <Smartphone className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-neutral-400" />
              </div>
              {errors.upiId && (
                <p className="text-sm text-error-500 mt-1">{errors.upiId}</p>
              )}
            </div>
            
            <div className="bg-primary-50 rounded-neumorphic p-4">
              <h5 className="font-medium text-primary-800 mb-2">Popular UPI Apps</h5>
              <div className="grid grid-cols-4 gap-3">
                {['Google Pay', 'PhonePe', 'Paytm', 'BHIM'].map((app) => (
                  <div key={app} className="text-center">
                    <div className="w-12 h-12 bg-white rounded-lg shadow-neumorphic-soft mx-auto mb-1 flex items-center justify-center">
                      <span className="text-xs font-medium">{app.slice(0, 2)}</span>
                    </div>
                    <span className="text-xs text-primary-700">{app}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        );

      case 'netbanking':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-neutral-800 mb-2">
                Select Your Bank
              </label>
              <div className="relative">
                <select
                  value={formData.bankCode}
                  onChange={(e) => handleInputChange('bankCode', e.target.value)}
                  className="w-full p-3 pl-12 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                >
                  <option value="">Choose your bank</option>
                  <option value="SBI">State Bank of India</option>
                  <option value="HDFC">HDFC Bank</option>
                  <option value="ICICI">ICICI Bank</option>
                  <option value="AXIS">Axis Bank</option>
                  <option value="KOTAK">Kotak Mahindra Bank</option>
                  <option value="PNB">Punjab National Bank</option>
                </select>
                <Building className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-neutral-400" />
              </div>
            </div>
          </div>
        );

      case 'wallets':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-neutral-800 mb-2">
                Select Wallet
              </label>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                {['Paytm', 'Amazon Pay', 'MobiKwik'].map((wallet) => (
                  <button
                    key={wallet}
                    type="button"
                    onClick={() => setFormData(prev => ({ ...prev, walletProvider: wallet }))}
                    className={`p-4 rounded-neumorphic border transition-all duration-200 ${
                      formData.walletProvider === wallet
                        ? 'border-primary-500 bg-primary-50'
                        : 'border-neutral-300 hover:border-primary-300'
                    }`}
                  >
                    <Wallet className="w-6 h-6 mx-auto mb-2 text-neutral-600" />
                    <span className="text-sm font-medium">{wallet}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <motion.form
      onSubmit={handleSubmit}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      className="space-y-6"
    >
      {renderPaymentForm()}

      {/* Submit Button */}
      <NeumorphicButton
        type="submit"
        variant="primary"
        size="lg"
        className="w-full"
        loading={isProcessing || isLoading}
        disabled={isProcessing || isLoading}
      >
        <Lock className="w-5 h-5 mr-2" />
        {isProcessing || isLoading ? 'Processing Payment...' : 'Pay Securely'}
      </NeumorphicButton>

      {/* Security Notice */}
      <div className="flex items-center justify-center gap-2 text-sm text-neutral-600">
        <Lock className="w-4 h-4 text-success-500" />
        <span>Your payment information is encrypted and secure</span>
      </div>
    </motion.form>
  );
}
