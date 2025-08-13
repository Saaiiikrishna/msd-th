'use client';

import { useState, useEffect } from 'react';
import Image from 'next/image';
import { motion } from 'framer-motion';
import { Search, MapPin, Users, Calendar } from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';

const heroImages = [
  {
    src: '/images/hero/treasure-hunt-1.jpg',
    alt: 'Team solving treasure hunt clues in Mumbai',
    fallback: 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=1200&h=800&fit=crop'
  },
  {
    src: '/images/hero/treasure-hunt-2.jpg', 
    alt: 'Adventure seekers exploring Delhi heritage',
    fallback: 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=1200&h=800&fit=crop'
  },
  {
    src: '/images/hero/treasure-hunt-3.jpg',
    alt: 'Corporate team building treasure hunt',
    fallback: 'https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?w=1200&h=800&fit=crop'
  }
];

// Removed duplicate stats - these are now only in Trust Indicators section

export function HeroSection() {
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');

  // Auto-rotate hero images
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentImageIndex((prev) => (prev + 1) % heroImages.length);
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    // TODO: Implement search functionality
    console.log('Searching for:', searchQuery);
  };

  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden bg-gradient-hero">
      {/* Background Image with Parallax Effect */}
      <div className="absolute inset-0 z-0">
        {heroImages.map((image, index) => (
          <motion.div
            key={index}
            className="absolute inset-0"
            initial={{ opacity: 0 }}
            animate={{ 
              opacity: index === currentImageIndex ? 0.3 : 0,
              scale: index === currentImageIndex ? 1.1 : 1
            }}
            transition={{ duration: 1.5, ease: 'easeInOut' }}
          >
            <Image
              src={image.fallback}
              alt={image.alt}
              fill
              className="object-cover"
              priority={index === 0}
              sizes="100vw"
            />
          </motion.div>
        ))}
        <div className="absolute inset-0 bg-gradient-to-br from-primary-900/40 via-primary-800/30 to-secondary-900/40" />
      </div>

      {/* Floating Elements */}
      <div className="absolute inset-0 z-10">
        <motion.div
          className="absolute top-20 left-10 w-20 h-20 bg-secondary-400/20 rounded-full blur-xl"
          animate={{
            y: [0, -20, 0],
            x: [0, 10, 0],
          }}
          transition={{
            duration: 6,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
        />
        <motion.div
          className="absolute bottom-32 right-16 w-32 h-32 bg-primary-400/20 rounded-full blur-xl"
          animate={{
            y: [0, 20, 0],
            x: [0, -15, 0],
          }}
          transition={{
            duration: 8,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
        />
      </div>

      {/* Main Content */}
      <div className="relative z-20 container mx-auto px-4 text-center text-white">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.2 }}
        >
          {/* Main Heading */}
          <h1 className="text-5xl md:text-7xl lg:text-8xl font-display font-bold mb-6 leading-tight">
            <span className="block">Discover</span>
            <span className="block text-gradient bg-gradient-to-r from-secondary-300 to-accent-300 bg-clip-text text-transparent">
              Adventure
            </span>
            <span className="block">Awaits</span>
          </h1>

          {/* Subtitle */}
          <motion.p
            className="text-xl md:text-2xl lg:text-3xl mb-8 max-w-4xl mx-auto leading-relaxed text-neutral-100"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
          >
            Embark on thrilling treasure hunts across India. 
            <br className="hidden md:block" />
            Team building, adventure, and memories that last forever.
          </motion.p>

          {/* Search Bar */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.6 }}
            className="mb-12"
          >
            <NeumorphicCard className="max-w-2xl mx-auto bg-white/10 backdrop-blur-md border border-white/20">
              <form onSubmit={handleSearch} className="flex items-center gap-4">
                <div className="flex-1 flex items-center gap-3">
                  <Search className="w-5 h-5 text-white/70" />
                  <input
                    type="text"
                    placeholder="Search by city, activity, or experience..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="flex-1 bg-transparent text-white placeholder-white/70 border-none outline-none text-lg"
                  />
                </div>
                <NeumorphicButton
                  type="submit"
                  variant="secondary"
                  size="lg"
                  className="shrink-0"
                >
                  Find Adventures
                </NeumorphicButton>
              </form>
            </NeumorphicCard>
          </motion.div>

          {/* CTA Buttons */}
          <motion.div
            className="flex flex-col sm:flex-row gap-4 justify-center mb-16"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.8 }}
          >
            <NeumorphicButton
              variant="primary"
              size="xl"
              className="bg-white text-primary-600 hover:bg-neutral-50"
            >
              Book Your Adventure
            </NeumorphicButton>
            <NeumorphicButton
              variant="ghost"
              size="xl"
              className="text-white border-white/30 hover:bg-white/10"
            >
              Watch Demo
            </NeumorphicButton>
          </motion.div>
        </motion.div>

        {/* Stats removed - consolidated in Trust Indicators section */}
      </div>

      {/* Scroll Indicator */}
      <motion.div
        className="absolute bottom-8 left-1/2 transform -translate-x-1/2 z-20"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 1, delay: 1.5 }}
      >
        <motion.div
          className="w-6 h-10 border-2 border-white/50 rounded-full flex justify-center"
          animate={{ y: [0, 10, 0] }}
          transition={{ duration: 2, repeat: Infinity }}
        >
          <motion.div
            className="w-1 h-3 bg-white/70 rounded-full mt-2"
            animate={{ y: [0, 12, 0] }}
            transition={{ duration: 2, repeat: Infinity }}
          />
        </motion.div>
      </motion.div>
    </section>
  );
}
