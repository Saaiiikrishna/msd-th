'use client';

import { useState, useEffect } from 'react';
import Image from 'next/image';
import { motion, AnimatePresence } from 'framer-motion';
import { Star, Quote, ChevronLeft, ChevronRight, Play } from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { useCustomerTestimonials } from '@/hooks/use-trust-widgets';

// Mock testimonials data - will be replaced with API data
const defaultTestimonials = [
  {
    id: '1',
    name: 'Priya Sharma',
    location: 'Mumbai',
    rating: 5,
    comment: 'Amazing treasure hunt experience! The payment process was smooth and secure. Our team had an incredible time exploring Mumbai\'s heritage sites while solving creative clues.',
    avatar: 'https://images.unsplash.com/photo-1494790108755-2616b612b786?w=150&h=150&fit=crop&crop=face',
    date: '2024-12-15',
    huntTitle: 'Mumbai Heritage Hunt',
    videoTestimonial: false
  },
  {
    id: '2',
    name: 'Rahul Gupta',
    location: 'Delhi',
    rating: 5,
    comment: 'Loved the adventure! Quick payment and instant confirmation made it hassle-free. The food trail was absolutely delicious and the clues were challenging yet fun.',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face',
    date: '2024-12-10',
    huntTitle: 'Delhi Food Trail Mystery',
    videoTestimonial: true
  },
  {
    id: '3',
    name: 'Sneha Patel',
    location: 'Bangalore',
    rating: 5,
    comment: 'Great team building activity. The booking process was super easy and secure. Our entire office team participated and it really brought us closer together.',
    avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop&crop=face',
    date: '2024-12-08',
    huntTitle: 'Bangalore Tech Hunt',
    videoTestimonial: false
  },
  {
    id: '4',
    name: 'Arjun Reddy',
    location: 'Hyderabad',
    rating: 5,
    comment: 'Fantastic experience from start to finish! The treasure hunt was well-organized, and the digital clues were innovative. Highly recommend for anyone looking for adventure.',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face',
    date: '2024-12-05',
    huntTitle: 'Hyderabad Heritage Walk',
    videoTestimonial: false
  },
  {
    id: '5',
    name: 'Kavya Nair',
    location: 'Kochi',
    rating: 5,
    comment: 'The Goa beach hunt was incredible! Perfect blend of adventure and relaxation. The payment was secure and the entire experience exceeded our expectations.',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop&crop=face',
    date: '2024-12-01',
    huntTitle: 'Goa Beach Adventure',
    videoTestimonial: true
  }
];

export function TestimonialsSection() {
  const { data: apiTestimonials, isLoading } = useCustomerTestimonials();
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isAutoPlaying, setIsAutoPlaying] = useState(true);

  // Use API data if available, fallback to default
  const testimonials = apiTestimonials || defaultTestimonials;

  // Auto-rotate testimonials
  useEffect(() => {
    if (!isAutoPlaying) return;

    const interval = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % testimonials.length);
    }, 5000);

    return () => clearInterval(interval);
  }, [testimonials.length, isAutoPlaying]);

  const nextTestimonial = () => {
    setCurrentIndex((prev) => (prev + 1) % testimonials.length);
    setIsAutoPlaying(false);
  };

  const prevTestimonial = () => {
    setCurrentIndex((prev) => (prev - 1 + testimonials.length) % testimonials.length);
    setIsAutoPlaying(false);
  };

  const goToTestimonial = (index: number) => {
    setCurrentIndex(index);
    setIsAutoPlaying(false);
  };

  if (isLoading) {
    return (
      <section className="space-y-12">
        <div className="text-center">
          <div className="h-12 bg-neutral-200 rounded animate-pulse mb-4 max-w-md mx-auto" />
          <div className="h-6 bg-neutral-200 rounded animate-pulse max-w-2xl mx-auto" />
        </div>
        <div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />
      </section>
    );
  }

  return (
    <section className="space-y-12">
      {/* Section Header */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.8 }}
        className="text-center"
      >
        <h2 className="text-4xl md:text-5xl font-display font-bold text-neutral-800 mb-4">
          What Our <span className="text-gradient">Adventurers</span> Say
        </h2>
        <p className="text-xl text-neutral-600 max-w-3xl mx-auto leading-relaxed">
          Don't just take our word for it. Hear from thousands of satisfied customers 
          who have experienced the thrill of our treasure hunts.
        </p>
      </motion.div>

      {/* Main Testimonial Carousel */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.8, delay: 0.2 }}
        className="relative"
      >
        <NeumorphicCard className="overflow-hidden bg-gradient-to-br from-white to-neutral-50">
          <div className="relative min-h-[400px] flex items-center">
            <AnimatePresence mode="wait">
              <motion.div
                key={currentIndex}
                initial={{ opacity: 0, x: 100 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -100 }}
                transition={{ duration: 0.5 }}
                className="w-full"
              >
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center p-8">
                  {/* Testimonial Content */}
                  <div className="space-y-6">
                    {/* Quote Icon */}
                    <Quote className="w-12 h-12 text-primary-300" />
                    
                    {/* Rating */}
                    <div className="flex items-center gap-1">
                      {[...Array(testimonials[currentIndex].rating)].map((_, i) => (
                        <Star key={i} className="w-5 h-5 text-warning-500 fill-current" />
                      ))}
                    </div>

                    {/* Comment */}
                    <blockquote className="text-xl md:text-2xl text-neutral-700 leading-relaxed font-medium">
                      "{testimonials[currentIndex].comment}"
                    </blockquote>

                    {/* Hunt Title */}
                    <div className="inline-block px-3 py-1 bg-primary-100 text-primary-700 rounded-full text-sm font-medium">
                      {testimonials[currentIndex].huntTitle}
                    </div>

                    {/* Author Info */}
                    <div className="flex items-center gap-4">
                      <div className="relative w-16 h-16 rounded-full overflow-hidden shadow-neumorphic">
                        <Image
                          src={testimonials[currentIndex].avatar}
                          alt={testimonials[currentIndex].name}
                          fill
                          className="object-cover"
                        />
                      </div>
                      <div>
                        <div className="font-semibold text-lg text-neutral-800">
                          {testimonials[currentIndex].name}
                        </div>
                        <div className="text-neutral-600">
                          {testimonials[currentIndex].location}
                        </div>
                        <div className="text-sm text-neutral-500">
                          {new Date(testimonials[currentIndex].date).toLocaleDateString('en-IN', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric'
                          })}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Video/Image Section */}
                  <div className="relative">
                    <div className="aspect-[4/3] rounded-neumorphic overflow-hidden bg-gradient-to-br from-primary-100 to-secondary-100">
                      {testimonials[currentIndex].videoTestimonial ? (
                        <div className="relative w-full h-full flex items-center justify-center bg-gradient-to-br from-primary-500 to-secondary-500">
                          <NeumorphicButton
                            variant="ghost"
                            size="lg"
                            className="bg-white/20 backdrop-blur-sm text-white border-white/30 hover:bg-white/30"
                          >
                            <Play className="w-8 h-8 mr-2" />
                            Play Video Testimonial
                          </NeumorphicButton>
                        </div>
                      ) : (
                        <div className="w-full h-full bg-gradient-to-br from-primary-200 to-secondary-200 flex items-center justify-center">
                          <div className="text-center text-neutral-600">
                            <Quote className="w-16 h-16 mx-auto mb-4 text-primary-400" />
                            <p className="text-lg font-medium">Written Testimonial</p>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </motion.div>
            </AnimatePresence>

            {/* Navigation Buttons */}
            <button
              onClick={prevTestimonial}
              className="absolute left-4 top-1/2 transform -translate-y-1/2 p-3 bg-white/80 backdrop-blur-sm rounded-full shadow-neumorphic hover:shadow-neumorphic-hover transition-all duration-200"
            >
              <ChevronLeft className="w-6 h-6 text-neutral-600" />
            </button>
            
            <button
              onClick={nextTestimonial}
              className="absolute right-4 top-1/2 transform -translate-y-1/2 p-3 bg-white/80 backdrop-blur-sm rounded-full shadow-neumorphic hover:shadow-neumorphic-hover transition-all duration-200"
            >
              <ChevronRight className="w-6 h-6 text-neutral-600" />
            </button>
          </div>
        </NeumorphicCard>

        {/* Dots Indicator */}
        <div className="flex justify-center gap-2 mt-6">
          {testimonials.map((_, index) => (
            <button
              key={index}
              onClick={() => goToTestimonial(index)}
              className={`w-3 h-3 rounded-full transition-all duration-200 ${
                index === currentIndex
                  ? 'bg-primary-500 shadow-neumorphic-soft'
                  : 'bg-neutral-300 hover:bg-neutral-400'
              }`}
            />
          ))}
        </div>
      </motion.div>

      {/* Stats Section */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.8, delay: 0.4 }}
        className="grid grid-cols-1 md:grid-cols-3 gap-8"
      >
        <NeumorphicCard className="text-center">
          <div className="text-4xl font-bold text-primary-600 mb-2">4.9/5</div>
          <div className="text-lg font-semibold text-neutral-800 mb-1">Average Rating</div>
          <div className="text-neutral-600">Based on 1,200+ reviews</div>
        </NeumorphicCard>

        <NeumorphicCard className="text-center">
          <div className="text-4xl font-bold text-secondary-600 mb-2">98%</div>
          <div className="text-lg font-semibold text-neutral-800 mb-1">Satisfaction Rate</div>
          <div className="text-neutral-600">Customers would recommend us</div>
        </NeumorphicCard>

        <NeumorphicCard className="text-center">
          <div className="text-4xl font-bold text-accent-600 mb-2">10K+</div>
          <div className="text-lg font-semibold text-neutral-800 mb-1">Happy Customers</div>
          <div className="text-neutral-600">Across 50+ cities in India</div>
        </NeumorphicCard>
      </motion.div>
    </section>
  );
}
