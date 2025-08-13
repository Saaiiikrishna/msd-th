'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { 
  CreditCard, 
  Smartphone, 
  Building, 
  Wallet,
  Shield,
  CheckCircle,
  Clock,
  Gift,
  Calculator
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { PaymentForm } from '@/components/payment/payment-form';
import { EMICalculator } from '@/components/payment/emi-calculator';
import { InternationalPayment } from '@/components/payment/international-payment';

const paymentMethods = [
  {
    id: 'cards',
    name: 'Credit/Debit Cards',
    icon: CreditCard,
    description: 'Visa, Mastercard, RuPay, American Express',
    processingTime: 'Instant',
    fees: 'No additional fees',
    popular: true,
    features: ['Instant confirmation', 'EMI options available', 'Secure encryption']
  },
  {
    id: 'upi',
    name: 'UPI',
    icon: Smartphone,
    description: 'Google Pay, PhonePe, Paytm, BHIM',
    processingTime: 'Instant',
    fees: 'No additional fees',
    popular: true,
    features: ['Quick & easy', 'No card details needed', 'Instant confirmation']
  },
  {
    id: 'netbanking',
    name: 'Net Banking',
    icon: Building,
    description: 'All major banks supported',
    processingTime: 'Instant',
    fees: 'No additional fees',
    popular: false,
    features: ['Direct bank transfer', 'Secure banking', 'Instant confirmation']
  },
  {
    id: 'wallets',
    name: 'Digital Wallets',
    icon: Wallet,
    description: 'Paytm, Amazon Pay, MobiKwik',
    processingTime: 'Instant',
    fees: 'No additional fees',
    popular: false,
    features: ['Quick payment', 'Cashback offers', 'Instant confirmation']
  },
  {
    id: 'international',
    name: 'International Cards',
    icon: CreditCard,
    description: 'For international customers',
    processingTime: 'Instant',
    fees: 'Currency conversion charges apply',
    popular: false,
    features: ['Multi-currency support', 'Secure processing', 'Global acceptance']
  }
];

export function PaymentMethods() {
  const [selectedMethod, setSelectedMethod] = useState('cards');
  const [showEMICalculator, setShowEMICalculator] = useState(false);

  const selectedPaymentMethod = paymentMethods.find(method => method.id === selectedMethod);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      <NeumorphicCard className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-xl font-semibold text-neutral-800 mb-2">
              Choose Payment Method
            </h3>
            <p className="text-neutral-600">
              Select your preferred payment option for a secure checkout
            </p>
          </div>
          
          {/* Security Badge */}
          <div className="flex items-center gap-2 text-success-600">
            <Shield className="w-5 h-5" />
            <span className="text-sm font-medium">256-bit SSL</span>
          </div>
        </div>

        {/* Payment Method Selection */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {paymentMethods.map((method) => (
            <motion.button
              key={method.id}
              onClick={() => setSelectedMethod(method.id)}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className={`relative p-4 rounded-neumorphic text-left transition-all duration-200 ${
                selectedMethod === method.id
                  ? 'bg-primary-50 border-2 border-primary-500 shadow-neumorphic-pressed'
                  : 'border border-neutral-300 hover:border-primary-300 hover:bg-primary-50'
              }`}
            >
              {/* Popular Badge */}
              {method.popular && (
                <span className="absolute -top-2 -right-2 px-2 py-1 bg-secondary-500 text-white text-xs font-semibold rounded-full">
                  Popular
                </span>
              )}

              <div className="flex items-start gap-3">
                <div className={`p-2 rounded-lg ${
                  selectedMethod === method.id ? 'bg-primary-100' : 'bg-neutral-100'
                }`}>
                  <method.icon className={`w-5 h-5 ${
                    selectedMethod === method.id ? 'text-primary-600' : 'text-neutral-600'
                  }`} />
                </div>
                
                <div className="flex-1">
                  <h4 className={`font-medium mb-1 ${
                    selectedMethod === method.id ? 'text-primary-800' : 'text-neutral-800'
                  }`}>
                    {method.name}
                  </h4>
                  <p className="text-sm text-neutral-600 mb-2">
                    {method.description}
                  </p>
                  
                  <div className="flex items-center gap-4 text-xs text-neutral-500">
                    <div className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      <span>{method.processingTime}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <CheckCircle className="w-3 h-3 text-success-500" />
                      <span>{method.fees}</span>
                    </div>
                  </div>
                </div>

                {/* Selection Indicator */}
                {selectedMethod === method.id && (
                  <div className="absolute top-2 right-2">
                    <CheckCircle className="w-5 h-5 text-primary-600 fill-current" />
                  </div>
                )}
              </div>
            </motion.button>
          ))}
        </div>

        {/* Selected Method Features */}
        {selectedPaymentMethod && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            transition={{ duration: 0.3 }}
            className="bg-neutral-50 rounded-neumorphic p-4"
          >
            <h5 className="font-medium text-neutral-800 mb-3">
              {selectedPaymentMethod.name} Features
            </h5>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {selectedPaymentMethod.features.map((feature, index) => (
                <div key={index} className="flex items-center gap-2 text-sm text-neutral-600">
                  <CheckCircle className="w-4 h-4 text-success-500" />
                  <span>{feature}</span>
                </div>
              ))}
            </div>
          </motion.div>
        )}

        {/* EMI Calculator Toggle */}
        {selectedMethod === 'cards' && (
          <div className="flex items-center justify-between p-4 bg-gradient-to-r from-secondary-50 to-accent-50 rounded-neumorphic">
            <div className="flex items-center gap-3">
              <Calculator className="w-5 h-5 text-secondary-600" />
              <div>
                <h5 className="font-medium text-secondary-800">EMI Options Available</h5>
                <p className="text-sm text-secondary-600">
                  Convert your payment into easy monthly installments
                </p>
              </div>
            </div>
            <NeumorphicButton
              variant="ghost"
              size="sm"
              onClick={() => setShowEMICalculator(!showEMICalculator)}
              className="text-secondary-600 border-secondary-300 hover:bg-secondary-100"
            >
              {showEMICalculator ? 'Hide' : 'Calculate'} EMI
            </NeumorphicButton>
          </div>
        )}

        {/* EMI Calculator */}
        {showEMICalculator && selectedMethod === 'cards' && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            transition={{ duration: 0.3 }}
          >
            <EMICalculator amount={2000} />
          </motion.div>
        )}

        {/* Special Offers */}
        <div className="bg-gradient-to-r from-primary-50 to-secondary-50 rounded-neumorphic p-4">
          <div className="flex items-center gap-2 mb-3">
            <Gift className="w-5 h-5 text-primary-600" />
            <h5 className="font-medium text-primary-800">Payment Offers</h5>
          </div>
          <div className="space-y-2 text-sm">
            <div className="flex items-center gap-2 text-primary-700">
              <CheckCircle className="w-4 h-4" />
              <span>Get 5% cashback on UPI payments (up to ₹100)</span>
            </div>
            <div className="flex items-center gap-2 text-primary-700">
              <CheckCircle className="w-4 h-4" />
              <span>No-cost EMI available on credit cards</span>
            </div>
            <div className="flex items-center gap-2 text-primary-700">
              <CheckCircle className="w-4 h-4" />
              <span>Extra 2% off on wallet payments</span>
            </div>
          </div>
        </div>

        {/* Payment Form */}
        <div className="border-t border-neutral-200 pt-6">
          {selectedMethod === 'international' ? (
            <InternationalPayment />
          ) : (
            <PaymentForm paymentMethod={selectedMethod} />
          )}
        </div>

        {/* Security Information */}
        <div className="bg-neutral-50 rounded-neumorphic p-4">
          <div className="flex items-center gap-2 mb-2">
            <Shield className="w-4 h-4 text-success-500" />
            <span className="font-medium text-neutral-800">Your payment is secure</span>
          </div>
          <p className="text-sm text-neutral-600">
            All transactions are encrypted with 256-bit SSL and processed through 
            PCI DSS compliant systems. Your financial information is never stored on our servers.
          </p>
        </div>
      </NeumorphicCard>
    </motion.div>
  );
}
