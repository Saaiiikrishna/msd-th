'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { 
  Calendar, 
  Users, 
  Clock, 
  Shield, 
  Star, 
  Gift,
  CreditCard,
  CheckCircle,
  AlertCircle,
  Minus,
  Plus
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { formatCurrency } from '@/lib/utils';
import { useAdventureAvailability } from '@/hooks/use-adventures';

interface Adventure {
  id: string;
  title: string;
  price: number;
  originalPrice?: number;
  maxParticipants: number;
  duration: string;
  rating: number;
  reviewCount: number;
  instantBooking: boolean;
  safetyVerified: boolean;
  discount?: number;
}

interface AdventureBookingProps {
  adventure: Adventure;
  searchParams: {
    date?: string;
    participants?: string;
  };
}

export function AdventureBooking({ adventure, searchParams }: AdventureBookingProps) {
  const router = useRouter();
  const [selectedDate, setSelectedDate] = useState(
    searchParams.date || new Date().toISOString().split('T')[0]
  );
  const [participants, setParticipants] = useState(
    parseInt(searchParams.participants || '2')
  );
  const [selectedTimeSlot, setSelectedTimeSlot] = useState<string>('');
  const [isBooking, setIsBooking] = useState(false);

  // Fetch availability for selected date
  const { data: availability, isLoading: availabilityLoading } = useAdventureAvailability(
    adventure.id,
    selectedDate
  );

  // Calculate pricing
  const basePrice = adventure.price * participants;
  const discount = adventure.originalPrice 
    ? (adventure.originalPrice - adventure.price) * participants 
    : 0;
  const taxes = Math.round(basePrice * 0.18); // 18% GST
  const totalPrice = basePrice + taxes;

  // Available time slots (mock data)
  const timeSlots = [
    { id: '09:00', time: '9:00 AM', available: true, spots: 6 },
    { id: '11:00', time: '11:00 AM', available: true, spots: 3 },
    { id: '14:00', time: '2:00 PM', available: true, spots: 8 },
    { id: '16:00', time: '4:00 PM', available: false, spots: 0 },
  ];

  const handleBookNow = async () => {
    if (!selectedTimeSlot) {
      alert('Please select a time slot');
      return;
    }

    setIsBooking(true);
    
    try {
      // Navigate to checkout page with booking details
      const bookingData = {
        adventureId: adventure.id,
        date: selectedDate,
        timeSlot: selectedTimeSlot,
        participants,
        totalPrice,
      };

      const params = new URLSearchParams({
        adventure: adventure.id,
        date: selectedDate,
        time: selectedTimeSlot,
        participants: participants.toString(),
        total: totalPrice.toString(),
      });

      router.push(`/checkout?${params.toString()}`);
    } catch (error) {
      console.error('Booking error:', error);
      alert('Something went wrong. Please try again.');
    } finally {
      setIsBooking(false);
    }
  };

  const updateParticipants = (change: number) => {
    const newCount = Math.max(1, Math.min(adventure.maxParticipants, participants + change));
    setParticipants(newCount);
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      <NeumorphicCard className="space-y-6">
        {/* Price Header */}
        <div className="text-center border-b border-neutral-200 pb-6">
          <div className="flex items-center justify-center gap-3 mb-2">
            <span className="text-3xl font-bold text-neutral-800">
              {formatCurrency(adventure.price)}
            </span>
            {adventure.originalPrice && adventure.originalPrice > adventure.price && (
              <span className="text-lg text-neutral-500 line-through">
                {formatCurrency(adventure.originalPrice)}
              </span>
            )}
            {adventure.discount && (
              <span className="px-2 py-1 bg-error-500 text-white text-xs font-semibold rounded-full">
                {adventure.discount}% OFF
              </span>
            )}
          </div>
          <p className="text-sm text-neutral-600">per person</p>
          
          {/* Trust Indicators */}
          <div className="flex items-center justify-center gap-4 mt-4 text-sm">
            <div className="flex items-center gap-1 text-warning-500">
              <Star className="w-4 h-4 fill-current" />
              <span className="font-medium">{adventure.rating}</span>
              <span className="text-neutral-500">({adventure.reviewCount})</span>
            </div>
            {adventure.instantBooking && (
              <div className="flex items-center gap-1 text-success-600">
                <CheckCircle className="w-4 h-4" />
                <span>Instant Booking</span>
              </div>
            )}
          </div>
        </div>

        {/* Date Selection */}
        <div>
          <label className="block text-sm font-medium text-neutral-800 mb-3">
            <Calendar className="w-4 h-4 inline mr-2" />
            Select Date
          </label>
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            min={new Date().toISOString().split('T')[0]}
            className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
        </div>

        {/* Participants Selection */}
        <div>
          <label className="block text-sm font-medium text-neutral-800 mb-3">
            <Users className="w-4 h-4 inline mr-2" />
            Participants
          </label>
          <div className="flex items-center justify-between p-3 border border-neutral-300 rounded-neumorphic">
            <span className="text-neutral-700">Number of people</span>
            <div className="flex items-center gap-3">
              <button
                onClick={() => updateParticipants(-1)}
                disabled={participants <= 1}
                className="p-1 rounded-full bg-neutral-100 hover:bg-neutral-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
              >
                <Minus className="w-4 h-4" />
              </button>
              <span className="font-semibold text-lg w-8 text-center">{participants}</span>
              <button
                onClick={() => updateParticipants(1)}
                disabled={participants >= adventure.maxParticipants}
                className="p-1 rounded-full bg-neutral-100 hover:bg-neutral-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
              >
                <Plus className="w-4 h-4" />
              </button>
            </div>
          </div>
          <p className="text-xs text-neutral-500 mt-1">
            Maximum {adventure.maxParticipants} participants
          </p>
        </div>

        {/* Time Slot Selection */}
        <div>
          <label className="block text-sm font-medium text-neutral-800 mb-3">
            <Clock className="w-4 h-4 inline mr-2" />
            Select Time
          </label>
          {availabilityLoading ? (
            <div className="space-y-2">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="h-12 bg-neutral-200 rounded animate-pulse" />
              ))}
            </div>
          ) : (
            <div className="space-y-2">
              {timeSlots.map((slot) => (
                <button
                  key={slot.id}
                  onClick={() => setSelectedTimeSlot(slot.id)}
                  disabled={!slot.available}
                  className={`w-full p-3 rounded-neumorphic text-left transition-all duration-200 ${
                    selectedTimeSlot === slot.id
                      ? 'bg-primary-50 border-2 border-primary-500 text-primary-700'
                      : slot.available
                      ? 'border border-neutral-300 hover:border-primary-300 hover:bg-primary-50'
                      : 'border border-neutral-200 bg-neutral-100 text-neutral-400 cursor-not-allowed'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-medium">{slot.time}</span>
                    <span className="text-sm">
                      {slot.available ? `${slot.spots} spots left` : 'Fully booked'}
                    </span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Price Breakdown */}
        <div className="border-t border-neutral-200 pt-6">
          <h4 className="font-medium text-neutral-800 mb-4">Price Breakdown</h4>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-neutral-600">
                {formatCurrency(adventure.price)} × {participants} participants
              </span>
              <span className="text-neutral-800">{formatCurrency(basePrice)}</span>
            </div>
            {discount > 0 && (
              <div className="flex justify-between text-success-600">
                <span>Discount</span>
                <span>-{formatCurrency(discount)}</span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-neutral-600">Taxes & fees</span>
              <span className="text-neutral-800">{formatCurrency(taxes)}</span>
            </div>
            <div className="border-t border-neutral-200 pt-2 flex justify-between font-semibold text-lg">
              <span>Total</span>
              <span>{formatCurrency(totalPrice)}</span>
            </div>
          </div>
        </div>

        {/* Safety & Trust */}
        <div className="bg-neutral-50 rounded-neumorphic p-4">
          <div className="flex items-center gap-3 mb-3">
            <Shield className="w-5 h-5 text-success-500" />
            <span className="font-medium text-neutral-800">Safety Guaranteed</span>
          </div>
          <ul className="space-y-1 text-sm text-neutral-600">
            <li className="flex items-center gap-2">
              <CheckCircle className="w-3 h-3 text-success-500" />
              <span>Verified safety protocols</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle className="w-3 h-3 text-success-500" />
              <span>Professional guides</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle className="w-3 h-3 text-success-500" />
              <span>Emergency support</span>
            </li>
          </ul>
        </div>

        {/* Special Offers */}
        <div className="bg-gradient-to-r from-secondary-50 to-accent-50 rounded-neumorphic p-4">
          <div className="flex items-center gap-2 mb-2">
            <Gift className="w-4 h-4 text-secondary-600" />
            <span className="font-medium text-secondary-800">Special Offer</span>
          </div>
          <p className="text-sm text-secondary-700">
            Book now and get a complimentary photo session worth ₹500!
          </p>
        </div>

        {/* Book Now Button */}
        <NeumorphicButton
          variant="primary"
          size="lg"
          className="w-full"
          onClick={handleBookNow}
          loading={isBooking}
          disabled={!selectedTimeSlot || isBooking}
        >
          <CreditCard className="w-5 h-5 mr-2" />
          {isBooking ? 'Processing...' : 'Book Now'}
        </NeumorphicButton>

        {/* Additional Info */}
        <div className="text-center text-xs text-neutral-500 space-y-1">
          <p>Free cancellation up to 24 hours before the experience</p>
          <p>Instant confirmation • Mobile ticket accepted</p>
        </div>
      </NeumorphicCard>
    </motion.div>
  );
}
