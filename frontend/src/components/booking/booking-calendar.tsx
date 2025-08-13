'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  ChevronLeft, 
  ChevronRight, 
  Calendar as CalendarIcon,
  Clock,
  Users,
  MapPin,
  Star,
  CheckCircle,
  AlertCircle
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { formatCurrency } from '@/lib/utils';

interface TimeSlot {
  id: string;
  time: string;
  available: boolean;
  spotsLeft: number;
  price: number;
  originalPrice?: number;
}

interface CalendarDay {
  date: Date;
  isCurrentMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
  isAvailable: boolean;
  timeSlots: TimeSlot[];
  specialOffer?: {
    type: 'discount' | 'early_bird' | 'last_minute';
    value: number;
    label: string;
  };
}

interface BookingCalendarProps {
  adventureId: string;
  onDateSelect: (date: Date, timeSlot: TimeSlot) => void;
  selectedDate?: Date;
  selectedTimeSlot?: string;
}

export function BookingCalendar({ 
  adventureId, 
  onDateSelect, 
  selectedDate, 
  selectedTimeSlot 
}: BookingCalendarProps) {
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [calendarDays, setCalendarDays] = useState<CalendarDay[]>([]);
  const [selectedDay, setSelectedDay] = useState<CalendarDay | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Generate calendar days
  useEffect(() => {
    generateCalendarDays();
  }, [currentMonth]);

  const generateCalendarDays = () => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());
    
    const days: CalendarDay[] = [];
    const today = new Date();
    
    for (let i = 0; i < 42; i++) {
      const date = new Date(startDate);
      date.setDate(startDate.getDate() + i);
      
      const isCurrentMonth = date.getMonth() === month;
      const isToday = date.toDateString() === today.toDateString();
      const isSelected = selectedDate?.toDateString() === date.toDateString();
      const isAvailable = date >= today && isCurrentMonth;
      
      // Mock time slots data
      const timeSlots: TimeSlot[] = isAvailable ? [
        {
          id: '09:00',
          time: '9:00 AM',
          available: Math.random() > 0.3,
          spotsLeft: Math.floor(Math.random() * 8) + 1,
          price: 500,
          originalPrice: Math.random() > 0.7 ? 650 : undefined,
        },
        {
          id: '11:00',
          time: '11:00 AM',
          available: Math.random() > 0.2,
          spotsLeft: Math.floor(Math.random() * 8) + 1,
          price: 500,
        },
        {
          id: '14:00',
          time: '2:00 PM',
          available: Math.random() > 0.4,
          spotsLeft: Math.floor(Math.random() * 8) + 1,
          price: 500,
        },
        {
          id: '16:00',
          time: '4:00 PM',
          available: Math.random() > 0.5,
          spotsLeft: Math.floor(Math.random() * 8) + 1,
          price: 500,
        },
      ] : [];

      // Add special offers for some days
      let specialOffer;
      if (isAvailable && Math.random() > 0.8) {
        const offers = [
          { type: 'discount' as const, value: 20, label: '20% OFF' },
          { type: 'early_bird' as const, value: 15, label: 'Early Bird' },
          { type: 'last_minute' as const, value: 25, label: 'Last Minute' },
        ];
        specialOffer = offers[Math.floor(Math.random() * offers.length)];
      }
      
      days.push({
        date,
        isCurrentMonth,
        isToday,
        isSelected,
        isAvailable,
        timeSlots,
        specialOffer,
      });
    }
    
    setCalendarDays(days);
  };

  const navigateMonth = (direction: 'prev' | 'next') => {
    const newMonth = new Date(currentMonth);
    newMonth.setMonth(currentMonth.getMonth() + (direction === 'next' ? 1 : -1));
    setCurrentMonth(newMonth);
  };

  const handleDayClick = (day: CalendarDay) => {
    if (!day.isAvailable) return;
    setSelectedDay(day);
  };

  const handleTimeSlotSelect = (timeSlot: TimeSlot) => {
    if (!timeSlot.available || !selectedDay) return;
    onDateSelect(selectedDay.date, timeSlot);
  };

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      className="space-y-6"
    >
      <NeumorphicCard className="space-y-6">
        {/* Calendar Header */}
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-xl font-semibold text-neutral-800 flex items-center gap-2">
              <CalendarIcon className="w-5 h-5 text-primary-600" />
              Select Date & Time
            </h3>
            <p className="text-neutral-600 mt-1">
              Choose your preferred date and time slot
            </p>
          </div>
          
          <div className="flex items-center gap-2">
            <NeumorphicButton
              variant="ghost"
              size="sm"
              onClick={() => navigateMonth('prev')}
              className="w-10 h-10 rounded-full"
            >
              <ChevronLeft className="w-4 h-4" />
            </NeumorphicButton>
            
            <div className="text-lg font-semibold text-neutral-800 min-w-[140px] text-center">
              {monthNames[currentMonth.getMonth()]} {currentMonth.getFullYear()}
            </div>
            
            <NeumorphicButton
              variant="ghost"
              size="sm"
              onClick={() => navigateMonth('next')}
              className="w-10 h-10 rounded-full"
            >
              <ChevronRight className="w-4 h-4" />
            </NeumorphicButton>
          </div>
        </div>

        {/* Calendar Grid */}
        <div className="space-y-4">
          {/* Day Headers */}
          <div className="grid grid-cols-7 gap-2">
            {dayNames.map((day) => (
              <div key={day} className="text-center text-sm font-medium text-neutral-600 py-2">
                {day}
              </div>
            ))}
          </div>

          {/* Calendar Days */}
          <div className="grid grid-cols-7 gap-2">
            {calendarDays.map((day, index) => (
              <motion.button
                key={index}
                onClick={() => handleDayClick(day)}
                disabled={!day.isAvailable}
                whileHover={day.isAvailable ? { scale: 1.05 } : {}}
                whileTap={day.isAvailable ? { scale: 0.95 } : {}}
                className={`relative aspect-square p-2 rounded-neumorphic text-sm font-medium transition-all duration-200 ${
                  !day.isCurrentMonth
                    ? 'text-neutral-300 cursor-not-allowed'
                    : !day.isAvailable
                    ? 'text-neutral-400 cursor-not-allowed'
                    : day.isSelected
                    ? 'bg-primary-500 text-white shadow-neumorphic-pressed'
                    : day.isToday
                    ? 'bg-secondary-100 text-secondary-700 border-2 border-secondary-500'
                    : 'text-neutral-700 hover:bg-primary-50 hover:text-primary-700'
                }`}
              >
                <span>{day.date.getDate()}</span>
                
                {/* Special Offer Badge */}
                {day.specialOffer && day.isAvailable && (
                  <span className="absolute -top-1 -right-1 w-4 h-4 bg-error-500 text-white text-xs rounded-full flex items-center justify-center">
                    !
                  </span>
                )}
                
                {/* Available Slots Indicator */}
                {day.isAvailable && day.timeSlots.some(slot => slot.available) && (
                  <div className="absolute bottom-1 left-1/2 transform -translate-x-1/2">
                    <div className="w-1 h-1 bg-success-500 rounded-full"></div>
                  </div>
                )}
              </motion.button>
            ))}
          </div>
        </div>

        {/* Legend */}
        <div className="flex items-center justify-center gap-6 text-xs text-neutral-600">
          <div className="flex items-center gap-1">
            <div className="w-3 h-3 bg-secondary-100 border-2 border-secondary-500 rounded"></div>
            <span>Today</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-3 h-3 bg-primary-500 rounded"></div>
            <span>Selected</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-1 h-1 bg-success-500 rounded-full"></div>
            <span>Available</span>
          </div>
        </div>
      </NeumorphicCard>

      {/* Time Slots */}
      {selectedDay && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <NeumorphicCard>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="text-lg font-semibold text-neutral-800">
                  Available Times - {selectedDay.date.toLocaleDateString('en-IN', { 
                    weekday: 'long', 
                    month: 'long', 
                    day: 'numeric' 
                  })}
                </h4>
                
                {selectedDay.specialOffer && (
                  <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                    selectedDay.specialOffer.type === 'discount' 
                      ? 'bg-error-100 text-error-700'
                      : selectedDay.specialOffer.type === 'early_bird'
                      ? 'bg-success-100 text-success-700'
                      : 'bg-warning-100 text-warning-700'
                  }`}>
                    {selectedDay.specialOffer.label}
                  </span>
                )}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {selectedDay.timeSlots.map((slot) => (
                  <motion.button
                    key={slot.id}
                    onClick={() => handleTimeSlotSelect(slot)}
                    disabled={!slot.available}
                    whileHover={slot.available ? { scale: 1.02 } : {}}
                    whileTap={slot.available ? { scale: 0.98 } : {}}
                    className={`p-4 rounded-neumorphic text-left transition-all duration-200 ${
                      !slot.available
                        ? 'bg-neutral-100 text-neutral-400 cursor-not-allowed'
                        : selectedTimeSlot === slot.id
                        ? 'bg-primary-50 border-2 border-primary-500 shadow-neumorphic-pressed'
                        : 'border border-neutral-300 hover:border-primary-300 hover:bg-primary-50'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <Clock className={`w-4 h-4 ${
                          slot.available ? 'text-primary-600' : 'text-neutral-400'
                        }`} />
                        <span className="font-medium">{slot.time}</span>
                      </div>
                      
                      {slot.available ? (
                        <CheckCircle className="w-4 h-4 text-success-500" />
                      ) : (
                        <AlertCircle className="w-4 h-4 text-error-500" />
                      )}
                    </div>
                    
                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-1 text-neutral-600">
                        <Users className="w-3 h-3" />
                        <span>{slot.spotsLeft} spots left</span>
                      </div>
                      
                      <div className="text-right">
                        {slot.originalPrice && (
                          <div className="text-xs text-neutral-500 line-through">
                            {formatCurrency(slot.originalPrice)}
                          </div>
                        )}
                        <div className="font-semibold text-neutral-800">
                          {formatCurrency(slot.price)}
                        </div>
                      </div>
                    </div>
                    
                    {!slot.available && (
                      <div className="mt-2 text-xs text-error-600">
                        Fully booked
                      </div>
                    )}
                  </motion.button>
                ))}
              </div>

              {selectedDay.timeSlots.every(slot => !slot.available) && (
                <div className="text-center py-8">
                  <AlertCircle className="w-12 h-12 text-neutral-300 mx-auto mb-4" />
                  <h5 className="text-lg font-medium text-neutral-800 mb-2">
                    No Available Times
                  </h5>
                  <p className="text-neutral-600">
                    All time slots for this date are fully booked. Please select another date.
                  </p>
                </div>
              )}
            </div>
          </NeumorphicCard>
        </motion.div>
      )}
    </motion.div>
  );
}
