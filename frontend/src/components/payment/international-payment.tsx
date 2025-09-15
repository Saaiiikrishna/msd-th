'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Globe, 
  CreditCard, 
  DollarSign, 
  Info, 
  AlertCircle,
  CheckCircle,
  RefreshCw
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { formatCurrency } from '@/lib/utils';

interface CurrencyRate {
  code: string;
  name: string;
  symbol: string;
  rate: number;
  flag: string;
}

const supportedCurrencies: CurrencyRate[] = [
  { code: 'USD', name: 'US Dollar', symbol: '$', rate: 83.25, flag: '🇺🇸' },
  { code: 'EUR', name: 'Euro', symbol: '€', rate: 90.15, flag: '🇪🇺' },
  { code: 'GBP', name: 'British Pound', symbol: '£', rate: 105.50, flag: '🇬🇧' },
  { code: 'CAD', name: 'Canadian Dollar', symbol: 'C$', rate: 61.75, flag: '🇨🇦' },
  { code: 'AUD', name: 'Australian Dollar', symbol: 'A$', rate: 54.20, flag: '🇦🇺' },
  { code: 'SGD', name: 'Singapore Dollar', symbol: 'S$', rate: 61.80, flag: '🇸🇬' },
  { code: 'AED', name: 'UAE Dirham', symbol: 'AED', rate: 22.65, flag: '🇦🇪' },
  { code: 'JPY', name: 'Japanese Yen', symbol: '¥', rate: 0.56, flag: '🇯🇵' },
];

interface InternationalPaymentProps {
  amount?: number;
}

export function InternationalPayment({ amount = 2000 }: InternationalPaymentProps) {
  const [selectedCurrency, setSelectedCurrency] = useState<CurrencyRate>(supportedCurrencies[0]);
  const [cardData, setCardData] = useState({
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    cardholderName: '',
    billingAddress: {
      street: '',
      city: '',
      state: '',
      zipCode: '',
      country: 'US'
    }
  });
  const [exchangeRateLoading, setExchangeRateLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Calculate converted amount
  const convertedAmount = amount / selectedCurrency.rate;
  const conversionFee = convertedAmount * 0.03; // 3% conversion fee
  const totalAmount = convertedAmount + conversionFee;

  // Refresh exchange rates
  const refreshRates = async () => {
    setExchangeRateLoading(true);
    // Simulate API call
    setTimeout(() => {
      setExchangeRateLoading(false);
    }, 1000);
  };

  const handleInputChange = (field: string, value: string) => {
    if (field.startsWith('billingAddress.')) {
      const addressField = field.split('.')[1];
      setCardData(prev => ({
        ...prev,
        billingAddress: {
          ...prev.billingAddress,
          [addressField]: value
        }
      }));
    } else {
      setCardData(prev => ({ ...prev, [field]: value }));
    }

    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  const countries = [
    { code: 'US', name: 'United States' },
    { code: 'GB', name: 'United Kingdom' },
    { code: 'CA', name: 'Canada' },
    { code: 'AU', name: 'Australia' },
    { code: 'DE', name: 'Germany' },
    { code: 'FR', name: 'France' },
    { code: 'SG', name: 'Singapore' },
    { code: 'AE', name: 'United Arab Emirates' },
    { code: 'JP', name: 'Japan' },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      className="space-y-6"
    >
      {/* Currency Selection */}
      <NeumorphicCard>
        <div className="flex items-center gap-3 mb-4">
          <Globe className="w-5 h-5 text-primary-600" />
          <h3 className="text-lg font-semibold text-neutral-800">
            Select Your Currency
          </h3>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {supportedCurrencies.map((currency) => (
            <button
              key={currency.code}
              onClick={() => setSelectedCurrency(currency)}
              className={`p-3 rounded-neumorphic border text-left transition-all duration-200 ${
                selectedCurrency.code === currency.code
                  ? 'border-primary-500 bg-primary-50 shadow-neumorphic-pressed'
                  : 'border-neutral-300 hover:border-primary-300 hover:bg-primary-50'
              }`}
            >
              <div className="flex items-center gap-2 mb-1">
                <span className="text-lg">{currency.flag}</span>
                <span className="font-medium text-neutral-800">{currency.code}</span>
              </div>
              <div className="text-sm text-neutral-600">{currency.name}</div>
              <div className="text-xs text-neutral-500">
                1 {currency.code} = ₹{currency.rate}
              </div>
            </button>
          ))}
        </div>
      </NeumorphicCard>

      {/* Currency Conversion */}
      <NeumorphicCard>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-neutral-800">
            Currency Conversion
          </h3>
          <button
            onClick={refreshRates}
            disabled={exchangeRateLoading}
            className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 transition-colors duration-200"
          >
            <RefreshCw className={`w-4 h-4 ${exchangeRateLoading ? 'animate-spin' : ''}`} />
            Refresh Rates
          </button>
        </div>

        <div className="space-y-4">
          {/* Conversion Details */}
          <div className="bg-neutral-50 rounded-neumorphic p-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <div className="text-sm text-neutral-600">Amount in INR</div>
                <div className="text-2xl font-bold text-neutral-800">
                  ₹{amount.toLocaleString()}
                </div>
              </div>
              <div>
                <div className="text-sm text-neutral-600">
                  Amount in {selectedCurrency.code}
                </div>
                <div className="text-2xl font-bold text-primary-600">
                  {selectedCurrency.symbol}{convertedAmount.toFixed(2)}
                </div>
              </div>
            </div>
          </div>

          {/* Fee Breakdown */}
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-neutral-600">
                Base amount ({selectedCurrency.code})
              </span>
              <span className="text-neutral-800">
                {selectedCurrency.symbol}{convertedAmount.toFixed(2)}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-neutral-600">
                Currency conversion fee (3%)
              </span>
              <span className="text-neutral-800">
                {selectedCurrency.symbol}{conversionFee.toFixed(2)}
              </span>
            </div>
            <div className="border-t border-neutral-200 pt-2 flex justify-between font-semibold">
              <span>Total amount</span>
              <span className="text-lg">
                {selectedCurrency.symbol}{totalAmount.toFixed(2)}
              </span>
            </div>
          </div>

          {/* Exchange Rate Info */}
          <div className="bg-blue-50 rounded-neumorphic p-3">
            <div className="flex items-start gap-2">
              <Info className="w-4 h-4 text-blue-600 mt-0.5" />
              <div className="text-sm text-blue-800">
                <div className="font-medium mb-1">Exchange Rate Information</div>
                <div>
                  Current rate: 1 {selectedCurrency.code} = ₹{selectedCurrency.rate}
                </div>
                <div className="text-xs text-blue-600 mt-1">
                  Rates are updated every 15 minutes and may vary at the time of transaction
                </div>
              </div>
            </div>
          </div>
        </div>
      </NeumorphicCard>

      {/* International Card Form */}
      <NeumorphicCard>
        <div className="flex items-center gap-3 mb-4">
          <CreditCard className="w-5 h-5 text-primary-600" />
          <h3 className="text-lg font-semibold text-neutral-800">
            International Card Details
          </h3>
        </div>

        <div className="space-y-4">
          {/* Card Number */}
          <div>
            <label className="block text-sm font-medium text-neutral-800 mb-2">
              Card Number
            </label>
            <input
              type="text"
              value={cardData.cardNumber}
              onChange={(e) => handleInputChange('cardNumber', e.target.value)}
              placeholder="1234 5678 9012 3456"
              className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            {/* Expiry Date */}
            <div>
              <label className="block text-sm font-medium text-neutral-800 mb-2">
                Expiry Date
              </label>
              <input
                type="text"
                value={cardData.expiryDate}
                onChange={(e) => handleInputChange('expiryDate', e.target.value)}
                placeholder="MM/YY"
                className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>

            {/* CVV */}
            <div>
              <label className="block text-sm font-medium text-neutral-800 mb-2">
                CVV
              </label>
              <input
                type="text"
                value={cardData.cvv}
                onChange={(e) => handleInputChange('cvv', e.target.value)}
                placeholder="123"
                className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>
          </div>

          {/* Cardholder Name */}
          <div>
            <label className="block text-sm font-medium text-neutral-800 mb-2">
              Cardholder Name
            </label>
            <input
              type="text"
              value={cardData.cardholderName}
              onChange={(e) => handleInputChange('cardholderName', e.target.value)}
              placeholder="John Doe"
              className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            />
          </div>

          {/* Billing Address */}
          <div>
            <h4 className="font-medium text-neutral-800 mb-3">Billing Address</h4>
            
            <div className="space-y-3">
              <input
                type="text"
                value={cardData.billingAddress.street}
                onChange={(e) => handleInputChange('billingAddress.street', e.target.value)}
                placeholder="Street Address"
                className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
              
              <div className="grid grid-cols-2 gap-3">
                <input
                  type="text"
                  value={cardData.billingAddress.city}
                  onChange={(e) => handleInputChange('billingAddress.city', e.target.value)}
                  placeholder="City"
                  className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                />
                <input
                  type="text"
                  value={cardData.billingAddress.state}
                  onChange={(e) => handleInputChange('billingAddress.state', e.target.value)}
                  placeholder="State/Province"
                  className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
              
              <div className="grid grid-cols-2 gap-3">
                <input
                  type="text"
                  value={cardData.billingAddress.zipCode}
                  onChange={(e) => handleInputChange('billingAddress.zipCode', e.target.value)}
                  placeholder="ZIP/Postal Code"
                  className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                />
                <select
                  value={cardData.billingAddress.country}
                  onChange={(e) => handleInputChange('billingAddress.country', e.target.value)}
                  className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                >
                  {countries.map((country) => (
                    <option key={country.code} value={country.code}>
                      {country.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>
        </div>
      </NeumorphicCard>

      {/* Important Notes */}
      <NeumorphicCard className="bg-amber-50 border border-amber-200">
        <div className="flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-amber-600 mt-0.5" />
          <div>
            <h4 className="font-medium text-amber-800 mb-2">
              Important Information for International Payments
            </h4>
            <ul className="space-y-1 text-sm text-amber-700">
              <li>• Your card will be charged in {selectedCurrency.code}</li>
              <li>• Additional bank charges may apply from your card issuer</li>
              <li>• Exchange rates may fluctuate between booking and payment processing</li>
              <li>• Ensure your card is enabled for international transactions</li>
              <li>• Processing may take 1-2 business days for verification</li>
            </ul>
          </div>
        </div>
      </NeumorphicCard>

      {/* Payment Button */}
      <NeumorphicButton
        variant="primary"
        size="lg"
        className="w-full"
      >
        <DollarSign className="w-5 h-5 mr-2" />
        Pay {selectedCurrency.symbol}{totalAmount.toFixed(2)}
      </NeumorphicButton>
    </motion.div>
  );
}
