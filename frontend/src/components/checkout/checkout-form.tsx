'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { 
  CreditCard, 
  User, 
  Mail, 
  Phone, 
  MapPin,
  Calendar,
  Users,
  Shield
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';

interface CheckoutFormProps {
  searchParams: {
    adventure?: string;
    date?: string;
    time?: string;
    participants?: string;
    total?: string;
  };
}

export function CheckoutForm({ searchParams }: CheckoutFormProps) {
  // Mock adventure data based on searchParams
  const adventure = {
    id: searchParams.adventure || '1',
    title: 'Mumbai Heritage Walk', // This would normally come from API
    price: parseInt(searchParams.total || '1500'),
    date: searchParams.date || '2024-02-15',
    participants: parseInt(searchParams.participants || '1'),
  };

  const onSubmit = async (formData: any) => {
    // Handle form submission
    console.log('Checkout form submitted:', formData);
  };
  const [formData, setFormData] = useState({
    // Personal Information
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    
    // Emergency Contact
    emergencyName: '',
    emergencyPhone: '',
    
    // Payment Information
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    cardholderName: '',
    
    // Billing Address
    address: '',
    city: '',
    state: '',
    zipCode: '',
    
    // Special Requirements
    dietaryRestrictions: '',
    medicalConditions: '',
    specialRequests: '',
    
    // Terms
    agreeToTerms: false,
    agreeToMarketing: false,
  });

  const [currentStep, setCurrentStep] = useState(1);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleInputChange = (field: string, value: string | boolean) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    
    try {
      await onSubmit(formData);
    } catch (error) {
      console.error('Checkout failed:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const steps = [
    { id: 1, title: 'Personal Info', icon: User },
    { id: 2, title: 'Payment', icon: CreditCard },
    { id: 3, title: 'Review', icon: Shield },
  ];

  const renderStepIndicator = () => (
    <div className="flex items-center justify-center mb-8">
      {steps.map((step, index) => (
        <div key={step.id} className="flex items-center">
          <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
            currentStep >= step.id 
              ? 'bg-primary-500 text-white' 
              : 'bg-neutral-200 text-neutral-500'
          }`}>
            <step.icon className="w-5 h-5" />
          </div>
          {index < steps.length - 1 && (
            <div className={`w-16 h-1 mx-4 ${
              currentStep > step.id ? 'bg-primary-500' : 'bg-neutral-200'
            }`} />
          )}
        </div>
      ))}
    </div>
  );

  const renderPersonalInfoStep = () => (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-neutral-700 mb-2">
            First Name *
          </label>
          <input
            type="text"
            required
            value={formData.firstName}
            onChange={(e) => handleInputChange('firstName', e.target.value)}
            className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-neutral-700 mb-2">
            Last Name *
          </label>
          <input
            type="text"
            required
            value={formData.lastName}
            onChange={(e) => handleInputChange('lastName', e.target.value)}
            className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-neutral-700 mb-2">
            Email Address *
          </label>
          <input
            type="email"
            required
            value={formData.email}
            onChange={(e) => handleInputChange('email', e.target.value)}
            className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-neutral-700 mb-2">
            Phone Number *
          </label>
          <input
            type="tel"
            required
            value={formData.phone}
            onChange={(e) => handleInputChange('phone', e.target.value)}
            className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
      </div>

      <div className="border-t border-neutral-200 pt-6">
        <h3 className="text-lg font-semibold text-neutral-900 mb-4">
          Emergency Contact
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2">
              Contact Name *
            </label>
            <input
              type="text"
              required
              value={formData.emergencyName}
              onChange={(e) => handleInputChange('emergencyName', e.target.value)}
              className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2">
              Contact Phone *
            </label>
            <input
              type="tel"
              required
              value={formData.emergencyPhone}
              onChange={(e) => handleInputChange('emergencyPhone', e.target.value)}
              className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        </div>
      </div>

      <div className="border-t border-neutral-200 pt-6">
        <h3 className="text-lg font-semibold text-neutral-900 mb-4">
          Special Requirements (Optional)
        </h3>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2">
              Dietary Restrictions
            </label>
            <input
              type="text"
              value={formData.dietaryRestrictions}
              onChange={(e) => handleInputChange('dietaryRestrictions', e.target.value)}
              placeholder="e.g., Vegetarian, Vegan, Allergies"
              className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2">
              Medical Conditions
            </label>
            <input
              type="text"
              value={formData.medicalConditions}
              onChange={(e) => handleInputChange('medicalConditions', e.target.value)}
              placeholder="Any medical conditions we should be aware of"
              className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2">
              Special Requests
            </label>
            <textarea
              value={formData.specialRequests}
              onChange={(e) => handleInputChange('specialRequests', e.target.value)}
              placeholder="Any special requests or accommodations needed"
              rows={3}
              className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        </div>
      </div>
    </div>
  );

  const renderPaymentStep = () => (
    <div className="space-y-6">
      <div>
        <label className="block text-sm font-medium text-neutral-700 mb-2">
          Card Number *
        </label>
        <input
          type="text"
          required
          value={formData.cardNumber}
          onChange={(e) => handleInputChange('cardNumber', e.target.value)}
          placeholder="1234 5678 9012 3456"
          className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-neutral-700 mb-2">
            Expiry Date *
          </label>
          <input
            type="text"
            required
            value={formData.expiryDate}
            onChange={(e) => handleInputChange('expiryDate', e.target.value)}
            placeholder="MM/YY"
            className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-neutral-700 mb-2">
            CVV *
          </label>
          <input
            type="text"
            required
            value={formData.cvv}
            onChange={(e) => handleInputChange('cvv', e.target.value)}
            placeholder="123"
            className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-neutral-700 mb-2">
          Cardholder Name *
        </label>
        <input
          type="text"
          required
          value={formData.cardholderName}
          onChange={(e) => handleInputChange('cardholderName', e.target.value)}
          className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
      </div>

      <div className="border-t border-neutral-200 pt-6">
        <h3 className="text-lg font-semibold text-neutral-900 mb-4">
          Billing Address
        </h3>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2">
              Address *
            </label>
            <input
              type="text"
              required
              value={formData.address}
              onChange={(e) => handleInputChange('address', e.target.value)}
              className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-2">
                City *
              </label>
              <input
                type="text"
                required
                value={formData.city}
                onChange={(e) => handleInputChange('city', e.target.value)}
                className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-2">
                State *
              </label>
              <input
                type="text"
                required
                value={formData.state}
                onChange={(e) => handleInputChange('state', e.target.value)}
                className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-2">
                ZIP Code *
              </label>
              <input
                type="text"
                required
                value={formData.zipCode}
                onChange={(e) => handleInputChange('zipCode', e.target.value)}
                className="w-full px-4 py-3 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  const renderReviewStep = () => (
    <div className="space-y-6">
      <div className="bg-neutral-50 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-neutral-900 mb-4">
          Booking Summary
        </h3>
        <div className="space-y-3">
          <div className="flex justify-between">
            <span className="text-neutral-600">Adventure:</span>
            <span className="font-medium">{adventure.title}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-neutral-600">Date:</span>
            <span className="font-medium">{adventure.date}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-neutral-600">Participants:</span>
            <span className="font-medium">{adventure.participants}</span>
          </div>
          <div className="flex justify-between text-lg font-semibold border-t border-neutral-200 pt-3">
            <span>Total:</span>
            <span>₹{(adventure.price * adventure.participants).toLocaleString()}</span>
          </div>
        </div>
      </div>

      <div className="space-y-4">
        <label className="flex items-start gap-3">
          <input
            type="checkbox"
            checked={formData.agreeToTerms}
            onChange={(e) => handleInputChange('agreeToTerms', e.target.checked)}
            className="mt-1 w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
          />
          <span className="text-sm text-neutral-700">
            I agree to the <a href="/terms" className="text-primary-600 hover:underline">Terms and Conditions</a> and <a href="/privacy" className="text-primary-600 hover:underline">Privacy Policy</a> *
          </span>
        </label>

        <label className="flex items-start gap-3">
          <input
            type="checkbox"
            checked={formData.agreeToMarketing}
            onChange={(e) => handleInputChange('agreeToMarketing', e.target.checked)}
            className="mt-1 w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
          />
          <span className="text-sm text-neutral-700">
            I would like to receive marketing communications about new adventures and special offers
          </span>
        </label>
      </div>
    </div>
  );

  return (
    <NeumorphicCard className="p-8">
      <form onSubmit={handleSubmit}>
        {renderStepIndicator()}

        <motion.div
          key={currentStep}
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: -20 }}
          transition={{ duration: 0.3 }}
        >
          {currentStep === 1 && renderPersonalInfoStep()}
          {currentStep === 2 && renderPaymentStep()}
          {currentStep === 3 && renderReviewStep()}
        </motion.div>

        <div className="flex justify-between mt-8 pt-6 border-t border-neutral-200">
          {currentStep > 1 && (
            <NeumorphicButton
              type="button"
              variant="outline"
              onClick={() => setCurrentStep(currentStep - 1)}
            >
              Previous
            </NeumorphicButton>
          )}
          
          <div className="ml-auto">
            {currentStep < 3 ? (
              <NeumorphicButton
                type="button"
                variant="primary"
                onClick={() => setCurrentStep(currentStep + 1)}
              >
                Next
              </NeumorphicButton>
            ) : (
              <NeumorphicButton
                type="submit"
                variant="primary"
                disabled={!formData.agreeToTerms || isSubmitting}
                className="min-w-32"
              >
                {isSubmitting ? 'Processing...' : 'Complete Booking'}
              </NeumorphicButton>
            )}
          </div>
        </div>
      </form>
    </NeumorphicCard>
  );
}
