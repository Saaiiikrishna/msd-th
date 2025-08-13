'use client';

import { motion } from 'framer-motion';
import { Search, Calendar, CreditCard, MapPin, Users, Trophy } from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';

const steps = [
  {
    step: 1,
    icon: Search,
    title: 'Discover Adventures',
    description: 'Browse through our curated collection of treasure hunts across India. Filter by location, difficulty, or theme to find your perfect adventure.',
    details: ['50+ unique experiences', 'Multiple cities', 'All skill levels'],
    color: 'from-primary-400 to-primary-600'
  },
  {
    step: 2,
    icon: Calendar,
    title: 'Choose Your Date',
    description: 'Select your preferred date and time slot. Our flexible booking system allows you to plan your adventure at your convenience.',
    details: ['Flexible scheduling', 'Real-time availability', 'Easy rescheduling'],
    color: 'from-secondary-400 to-secondary-600'
  },
  {
    step: 3,
    icon: CreditCard,
    title: 'Secure Payment',
    description: 'Complete your booking with our secure payment system. Multiple payment options available with instant confirmation.',
    details: ['Multiple payment methods', 'Secure transactions', 'Instant confirmation'],
    color: 'from-accent-400 to-accent-600'
  },
  {
    step: 4,
    icon: MapPin,
    title: 'Get Ready to Explore',
    description: 'Receive detailed instructions and meeting point information. Our team will guide you through an unforgettable adventure.',
    details: ['Detailed instructions', 'Expert guidance', 'Safety assured'],
    color: 'from-success-400 to-success-600'
  }
];

const features = [
  {
    icon: Users,
    title: 'Team Building',
    description: 'Perfect for corporate teams, friends, and families'
  },
  {
    icon: Trophy,
    title: 'Rewards & Prizes',
    description: 'Win exciting prizes and certificates for completing hunts'
  },
  {
    icon: MapPin,
    title: 'Local Insights',
    description: 'Discover hidden gems and local stories in each city'
  }
];

export function HowItWorks() {
  return (
    <section className="space-y-16">
      {/* Section Header */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.8 }}
        className="text-center"
      >
        <h2 className="text-4xl md:text-5xl font-display font-bold text-neutral-800 mb-4">
          How It <span className="text-gradient">Works</span>
        </h2>
        <p className="text-xl text-neutral-600 max-w-3xl mx-auto leading-relaxed">
          Getting started with your treasure hunt adventure is simple. 
          Follow these easy steps to book your next unforgettable experience.
        </p>
      </motion.div>

      {/* Steps */}
      <div className="relative">
        {/* Connection Line */}
        <div className="hidden lg:block absolute top-1/2 left-0 right-0 h-0.5 bg-gradient-to-r from-primary-200 via-secondary-200 to-accent-200 transform -translate-y-1/2 z-0" />
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 relative z-10">
          {steps.map((step, index) => (
            <motion.div
              key={step.step}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: index * 0.2 }}
              className="relative"
            >
              <NeumorphicCard className="text-center group hover:shadow-neumorphic-elevated transition-all duration-500 hover:-translate-y-2">
                {/* Step Number */}
                <div className="absolute -top-4 left-1/2 transform -translate-x-1/2">
                  <div className={`w-8 h-8 rounded-full bg-gradient-to-r ${step.color} flex items-center justify-center text-white font-bold text-sm shadow-lg`}>
                    {step.step}
                  </div>
                </div>

                {/* Icon */}
                <div className="mt-6 mb-6">
                  <div className={`inline-flex items-center justify-center w-16 h-16 rounded-full bg-gradient-to-r ${step.color} shadow-neumorphic group-hover:shadow-neumorphic-elevated transition-all duration-300`}>
                    <step.icon className="w-8 h-8 text-white" />
                  </div>
                </div>

                {/* Content */}
                <div className="space-y-4">
                  <h3 className="text-xl font-semibold text-neutral-800">
                    {step.title}
                  </h3>
                  
                  <p className="text-neutral-600 leading-relaxed">
                    {step.description}
                  </p>

                  {/* Details */}
                  <div className="space-y-2">
                    {step.details.map((detail, detailIndex) => (
                      <div
                        key={detailIndex}
                        className="flex items-center justify-center gap-2 text-sm text-neutral-500"
                      >
                        <div className={`w-1.5 h-1.5 rounded-full bg-gradient-to-r ${step.color}`} />
                        <span>{detail}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </NeumorphicCard>

              {/* Arrow for larger screens */}
              {index < steps.length - 1 && (
                <div className="hidden lg:block absolute top-1/2 -right-4 transform -translate-y-1/2 z-20">
                  <motion.div
                    initial={{ opacity: 0, x: -10 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6, delay: index * 0.2 + 0.3 }}
                    className={`w-8 h-8 rounded-full bg-gradient-to-r ${step.color} flex items-center justify-center shadow-lg`}
                  >
                    <svg
                      className="w-4 h-4 text-white"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 5l7 7-7 7"
                      />
                    </svg>
                  </motion.div>
                </div>
              )}
            </motion.div>
          ))}
        </div>
      </div>

      {/* Additional Features */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.8, delay: 0.4 }}
        className="space-y-8"
      >
        <div className="text-center">
          <h3 className="text-2xl md:text-3xl font-display font-semibold text-neutral-800 mb-4">
            Why Choose Our Adventures?
          </h3>
          <p className="text-lg text-neutral-600 max-w-2xl mx-auto">
            We provide more than just treasure hunts - we create memorable experiences 
            that bring people together and showcase the beauty of India.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {features.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: 0.6 + index * 0.1 }}
            >
              <NeumorphicCard className="text-center group hover:shadow-neumorphic-elevated transition-all duration-300">
                <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-gradient-to-br from-primary-100 to-primary-200 mb-4 group-hover:shadow-neumorphic transition-all duration-300">
                  <feature.icon className="w-6 h-6 text-primary-600" />
                </div>
                <h4 className="text-lg font-semibold text-neutral-800 mb-2">
                  {feature.title}
                </h4>
                <p className="text-neutral-600">
                  {feature.description}
                </p>
              </NeumorphicCard>
            </motion.div>
          ))}
        </div>
      </motion.div>

      {/* Interactive Demo Section */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.8, delay: 0.8 }}
      >
        <NeumorphicCard className="bg-gradient-to-br from-primary-50 via-white to-secondary-50 border border-primary-100">
          <div className="text-center space-y-6">
            <div>
              <h3 className="text-2xl md:text-3xl font-display font-semibold text-neutral-800 mb-4">
                Ready to Start Your Adventure?
              </h3>
              <p className="text-lg text-neutral-600 max-w-2xl mx-auto">
                Join thousands of adventurers who have already discovered the thrill of our treasure hunts. 
                Your next great adventure is just a click away!
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="btn-primary px-8 py-4 text-lg font-semibold"
              >
                Browse Adventures
              </motion.button>
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="btn-ghost px-8 py-4 text-lg font-semibold"
              >
                Watch Demo Video
              </motion.button>
            </div>

            {/* Trust Indicators */}
            <div className="flex flex-wrap justify-center gap-6 text-sm text-neutral-600 pt-4 border-t border-neutral-200">
              <span className="flex items-center gap-1">
                <Users className="w-4 h-4 text-primary-500" />
                10,000+ Happy Customers
              </span>
              <span className="flex items-center gap-1">
                <Trophy className="w-4 h-4 text-secondary-500" />
                500+ Successful Hunts
              </span>
              <span className="flex items-center gap-1">
                <MapPin className="w-4 h-4 text-accent-500" />
                50+ Cities Covered
              </span>
            </div>
          </div>
        </NeumorphicCard>
      </motion.div>
    </section>
  );
}
