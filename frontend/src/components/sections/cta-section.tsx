'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { ArrowRight, Mail, Phone, MapPin, Gift, Clock, Users } from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';

const urgencyOffers = [
  {
    icon: Gift,
    title: 'Limited Time Offer',
    description: '20% off on all bookings this month',
    highlight: 'Ends in 5 days'
  },
  {
    icon: Users,
    title: 'Group Discounts',
    description: 'Book for 8+ people and save 15%',
    highlight: 'Perfect for teams'
  },
  {
    icon: Clock,
    title: 'Early Bird Special',
    description: 'Book 7 days in advance for extra savings',
    highlight: 'Save up to ₹500'
  }
];

// Contact methods removed - available in footer to avoid duplication

export function CTASection() {
  const [email, setEmail] = useState('');
  const [isSubscribing, setIsSubscribing] = useState(false);

  const handleNewsletterSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubscribing(true);
    
    // TODO: Implement newsletter signup
    setTimeout(() => {
      setIsSubscribing(false);
      setEmail('');
      // Show success message
    }, 2000);
  };

  return (
    <section className="relative overflow-hidden">
      {/* Background Elements */}
      <div className="absolute inset-0">
        <div className="absolute top-10 left-10 w-32 h-32 bg-white/10 rounded-full blur-xl" />
        <div className="absolute bottom-20 right-16 w-40 h-40 bg-white/10 rounded-full blur-xl" />
        <div className="absolute top-1/2 left-1/3 w-24 h-24 bg-white/5 rounded-full blur-lg" />
      </div>

      <div className="relative z-10 space-y-16">
        {/* Main CTA */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
          className="text-center space-y-8"
        >
          <div>
            <h2 className="text-4xl md:text-6xl font-display font-bold text-white mb-6 leading-tight">
              Ready for Your Next
              <br />
              <span className="text-secondary-300">Adventure?</span>
            </h2>
            <p className="text-xl md:text-2xl text-white/90 max-w-3xl mx-auto leading-relaxed">
              Join thousands of adventurers who have discovered the thrill of treasure hunting. 
              Book your experience today and create memories that will last a lifetime.
            </p>
          </div>

          {/* Primary CTA Buttons */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <motion.div
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              <NeumorphicButton
                variant="secondary"
                size="xl"
                className="bg-white text-primary-600 hover:bg-neutral-50 shadow-neumorphic-elevated"
              >
                <span className="mr-2">Book Your Adventure</span>
                <ArrowRight className="w-5 h-5" />
              </NeumorphicButton>
            </motion.div>
            
            <motion.div
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              <NeumorphicButton
                variant="ghost"
                size="xl"
                className="text-white border-white/30 hover:bg-white/10 backdrop-blur-sm"
              >
                Browse All Hunts
              </NeumorphicButton>
            </motion.div>
          </div>
        </motion.div>

        {/* Urgency Offers */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8, delay: 0.2 }}
          className="grid grid-cols-1 md:grid-cols-3 gap-6"
        >
          {urgencyOffers.map((offer, index) => (
            <motion.div
              key={offer.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: 0.4 + index * 0.1 }}
            >
              <NeumorphicCard className="text-center bg-white/10 backdrop-blur-md border border-white/20 hover:bg-white/20 transition-all duration-300">
                <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-secondary-500 mb-4">
                  <offer.icon className="w-6 h-6 text-white" />
                </div>
                <h3 className="text-lg font-semibold text-white mb-2">
                  {offer.title}
                </h3>
                <p className="text-white/80 mb-2">
                  {offer.description}
                </p>
                <span className="inline-block px-3 py-1 bg-secondary-500 text-white text-sm font-medium rounded-full">
                  {offer.highlight}
                </span>
              </NeumorphicCard>
            </motion.div>
          ))}
        </motion.div>

        {/* Newsletter Signup */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8, delay: 0.4 }}
        >
          <NeumorphicCard className="max-w-2xl mx-auto bg-white/10 backdrop-blur-md border border-white/20">
            <div className="text-center space-y-6">
              <div>
                <h3 className="text-2xl font-semibold text-white mb-2">
                  Stay Updated with New Adventures
                </h3>
                <p className="text-white/80">
                  Get notified about new treasure hunts, special offers, and exclusive events.
                </p>
              </div>

              <form onSubmit={handleNewsletterSignup} className="flex flex-col sm:flex-row gap-4">
                <input
                  type="email"
                  placeholder="Enter your email address"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="flex-1 px-4 py-3 rounded-neumorphic bg-white/20 backdrop-blur-sm text-white placeholder-white/70 border border-white/30 focus:border-white/50 focus:outline-none"
                />
                <NeumorphicButton
                  type="submit"
                  variant="secondary"
                  size="lg"
                  loading={isSubscribing}
                  className="bg-secondary-500 text-white hover:bg-secondary-600"
                >
                  {isSubscribing ? 'Subscribing...' : 'Subscribe'}
                </NeumorphicButton>
              </form>

              <p className="text-sm text-white/60">
                No spam, unsubscribe at any time. We respect your privacy.
              </p>
            </div>
          </NeumorphicCard>
        </motion.div>

        {/* Contact section removed - available in footer */}

        {/* Trust indicators removed - consolidated in dedicated Trust Indicators section */}
      </div>
    </section>
  );
}
