'use client';

import { motion } from 'framer-motion';
import { 
  Calendar,
  Clock,
  MapPin,
  Users,
  Star,
  Shield,
  RefreshCw,
  Tag
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';

interface OrderSummaryProps {
  searchParams: {
    adventure?: string;
    date?: string;
    time?: string;
    participants?: string;
    total?: string;
  };
}

export function OrderSummary({ searchParams }: OrderSummaryProps) {
  // Mock data based on searchParams - in real app, this would come from API
  const adventure = {
    id: searchParams.adventure || '1',
    title: 'Mumbai Heritage Walk',
    description: 'Explore the rich history and architecture of South Mumbai',
    location: 'South Mumbai',
    duration: 3,
    rating: 4.8,
    reviewCount: 124,
    price: 1500,
    imageUrl: '/images/mumbai-heritage.jpg',
    highlights: ['Expert guide', 'Historical sites', 'Photo opportunities'],
    included: ['Professional guide', 'Entry tickets', 'Refreshments', 'Insurance'],
    notIncluded: ['Personal expenses', 'Transportation to meeting point', 'Tips'],
  };

  const booking = {
    date: searchParams.date || '2024-02-15',
    time: searchParams.time || '09:00 AM',
    participants: parseInt(searchParams.participants || '1'),
  };

  const pricing = {
    basePrice: adventure.price * booking.participants,
    discount: 0,
    taxes: Math.round((adventure.price * booking.participants) * 0.18),
    processingFee: 50,
    total: parseInt(searchParams.total || String(adventure.price * booking.participants + Math.round((adventure.price * booking.participants) * 0.18) + 50)),
  };

  const onPromoCodeApply = (code: string) => {
    console.log('Applying promo code:', code);
  };

  const onPromoCodeRemove = () => {
    console.log('Removing promo code');
  };
  const handlePromoSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const code = formData.get('promoCode') as string;
    if (code && onPromoCodeApply) {
      onPromoCodeApply(code);
    }
  };

  return (
    <div className="space-y-6">
      {/* Adventure Summary */}
      <NeumorphicCard className="p-6">
        <div className="flex gap-4">
          <div className="w-20 h-20 bg-gradient-to-br from-primary-400 to-primary-600 rounded-lg flex-shrink-0">
            {/* Adventure image placeholder */}
          </div>
          <div className="flex-1">
            <h3 className="font-semibold text-neutral-900 mb-2">
              {adventure.title}
            </h3>
            <div className="space-y-1 text-sm text-neutral-600">
              <div className="flex items-center gap-2">
                <MapPin className="w-4 h-4" />
                <span>{adventure.location}</span>
              </div>
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4" />
                <span>{adventure.duration} hours</span>
              </div>
              <div className="flex items-center gap-2">
                <Star className="w-4 h-4 text-yellow-500 fill-current" />
                <span>{adventure.rating} ({adventure.reviewCount} reviews)</span>
              </div>
            </div>
          </div>
        </div>
      </NeumorphicCard>

      {/* Booking Details */}
      <NeumorphicCard className="p-6">
        <h3 className="font-semibold text-neutral-900 mb-4">
          Booking Details
        </h3>
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-neutral-600">
              <Calendar className="w-4 h-4" />
              <span>Date</span>
            </div>
            <span className="font-medium text-neutral-900">
              {booking.date}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-neutral-600">
              <Clock className="w-4 h-4" />
              <span>Time</span>
            </div>
            <span className="font-medium text-neutral-900">
              {booking.time}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-neutral-600">
              <Users className="w-4 h-4" />
              <span>Participants</span>
            </div>
            <span className="font-medium text-neutral-900">
              {booking.participants} {booking.participants === 1 ? 'person' : 'people'}
            </span>
          </div>
        </div>


      </NeumorphicCard>



      {/* Price Breakdown */}
      <NeumorphicCard className="p-6">
        <h3 className="font-semibold text-neutral-900 mb-4">
          Price Breakdown
        </h3>
        <div className="space-y-3">
          <div className="flex justify-between">
            <span className="text-neutral-600">
              Base price × {booking.participants}
            </span>
            <span className="text-neutral-900">
              ₹{pricing.basePrice.toLocaleString()}
            </span>
          </div>
          
          {pricing.discount && pricing.discount > 0 && (
            <div className="flex justify-between text-green-600">
              <span>Discount</span>
              <span>-₹{pricing.discount.toLocaleString()}</span>
            </div>
          )}
          
          <div className="flex justify-between">
            <span className="text-neutral-600">Taxes & fees</span>
            <span className="text-neutral-900">
              ₹{pricing.taxes.toLocaleString()}
            </span>
          </div>
          
          <div className="flex justify-between">
            <span className="text-neutral-600">Processing fee</span>
            <span className="text-neutral-900">
              ₹{pricing.processingFee.toLocaleString()}
            </span>
          </div>
          
          <div className="border-t border-neutral-200 pt-3">
            <div className="flex justify-between text-lg font-semibold">
              <span className="text-neutral-900">Total</span>
              <span className="text-neutral-900">
                ₹{pricing.total.toLocaleString()}
              </span>
            </div>
          </div>
        </div>
      </NeumorphicCard>

      {/* What's Included */}
      <NeumorphicCard className="p-6">
        <h3 className="font-semibold text-neutral-900 mb-4">
          What's Included
        </h3>
        <div className="space-y-2">
          {adventure.included.map((item, index) => (
            <div key={index} className="flex items-start gap-2">
              <Shield className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
              <span className="text-sm text-neutral-700">{item}</span>
            </div>
          ))}
        </div>
        
        {adventure.notIncluded.length > 0 && (
          <div className="mt-4 pt-4 border-t border-neutral-200">
            <h4 className="font-medium text-neutral-900 mb-3">
              Not Included
            </h4>
            <div className="space-y-2">
              {adventure.notIncluded.map((item, index) => (
                <div key={index} className="flex items-start gap-2">
                  <div className="w-4 h-4 border border-neutral-400 rounded-full mt-0.5 flex-shrink-0" />
                  <span className="text-sm text-neutral-600">{item}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </NeumorphicCard>

      {/* Security Notice */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.3 }}
        className="bg-blue-50 border border-blue-200 rounded-lg p-4"
      >
        <div className="flex items-start gap-3">
          <Shield className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
          <div>
            <h4 className="font-medium text-blue-900 mb-1">
              Secure Payment
            </h4>
            <p className="text-sm text-blue-700">
              Your payment information is encrypted and secure. We never store your card details.
            </p>
          </div>
        </div>
      </motion.div>

      {/* Cancellation Policy */}
      <NeumorphicCard className="p-6">
        <h3 className="font-semibold text-neutral-900 mb-3">
          Cancellation Policy
        </h3>
        <div className="space-y-2 text-sm text-neutral-600">
          <p>• Free cancellation up to 24 hours before the experience</p>
          <p>• 50% refund for cancellations within 24 hours</p>
          <p>• No refund for no-shows or cancellations after start time</p>
        </div>
      </NeumorphicCard>
    </div>
  );
}
