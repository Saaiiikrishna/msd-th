'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { Search, MapPin, Calendar, Users, Filter } from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';

export function AdventuresHeader() {
  const [searchQuery, setSearchQuery] = useState('');
  const [quickFilters, setQuickFilters] = useState({
    location: '',
    date: '',
    participants: ''
  });

  const handleQuickSearch = (e: React.FormEvent) => {
    e.preventDefault();
    // TODO: Implement search functionality
    console.log('Quick search:', { searchQuery, ...quickFilters });
  };

  return (
    <section className="relative bg-gradient-to-br from-primary-600 via-primary-700 to-secondary-600 text-white overflow-hidden">
      {/* Background Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute top-10 left-10 w-32 h-32 bg-white rounded-full blur-xl" />
        <div className="absolute bottom-20 right-16 w-40 h-40 bg-white rounded-full blur-xl" />
        <div className="absolute top-1/2 left-1/3 w-24 h-24 bg-white rounded-full blur-lg" />
      </div>

      <div className="relative z-10 container mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="text-center space-y-8"
        >
          {/* Header Content */}
          <div>
            <h1 className="text-4xl md:text-6xl font-display font-bold mb-4 leading-tight">
              Discover Your Next
              <br />
              <span className="text-secondary-300">Adventure</span>
            </h1>
            <p className="text-xl md:text-2xl text-white/90 max-w-3xl mx-auto leading-relaxed">
              Explore treasure hunts across India. From heritage walks to tech challenges, 
              find the perfect adventure for your team or family.
            </p>
          </div>

          {/* Quick Search */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            className="max-w-4xl mx-auto"
          >
            <NeumorphicCard className="bg-white/10 backdrop-blur-md border border-white/20">
              <form onSubmit={handleQuickSearch} className="space-y-4">
                {/* Main Search */}
                <div className="flex items-center gap-3 p-2 bg-white/10 rounded-neumorphic">
                  <Search className="w-5 h-5 text-white/70 ml-3" />
                  <input
                    type="text"
                    placeholder="Search adventures, cities, or experiences..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="flex-1 bg-transparent text-white placeholder-white/70 border-none outline-none text-lg py-2"
                  />
                  <NeumorphicButton
                    type="submit"
                    variant="secondary"
                    size="sm"
                    className="mr-2"
                  >
                    Search
                  </NeumorphicButton>
                </div>

                {/* Quick Filters */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="flex items-center gap-3 p-3 bg-white/5 rounded-neumorphic">
                    <MapPin className="w-4 h-4 text-white/70" />
                    <select
                      value={quickFilters.location}
                      onChange={(e) => setQuickFilters(prev => ({ ...prev, location: e.target.value }))}
                      className="flex-1 bg-transparent text-white border-none outline-none"
                    >
                      <option value="" className="text-neutral-800">Any Location</option>
                      <option value="mumbai" className="text-neutral-800">Mumbai</option>
                      <option value="delhi" className="text-neutral-800">Delhi</option>
                      <option value="bangalore" className="text-neutral-800">Bangalore</option>
                      <option value="goa" className="text-neutral-800">Goa</option>
                    </select>
                  </div>

                  <div className="flex items-center gap-3 p-3 bg-white/5 rounded-neumorphic">
                    <Calendar className="w-4 h-4 text-white/70" />
                    <input
                      type="date"
                      value={quickFilters.date}
                      onChange={(e) => setQuickFilters(prev => ({ ...prev, date: e.target.value }))}
                      className="flex-1 bg-transparent text-white border-none outline-none"
                      min={new Date().toISOString().split('T')[0]}
                    />
                  </div>

                  <div className="flex items-center gap-3 p-3 bg-white/5 rounded-neumorphic">
                    <Users className="w-4 h-4 text-white/70" />
                    <select
                      value={quickFilters.participants}
                      onChange={(e) => setQuickFilters(prev => ({ ...prev, participants: e.target.value }))}
                      className="flex-1 bg-transparent text-white border-none outline-none"
                    >
                      <option value="" className="text-neutral-800">Any Group Size</option>
                      <option value="1-2" className="text-neutral-800">1-2 People</option>
                      <option value="3-5" className="text-neutral-800">3-5 People</option>
                      <option value="6-10" className="text-neutral-800">6-10 People</option>
                      <option value="10+" className="text-neutral-800">10+ People</option>
                    </select>
                  </div>
                </div>
              </form>
            </NeumorphicCard>
          </motion.div>

          {/* Quick Stats */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
            className="flex flex-wrap justify-center gap-8 text-white/80"
          >
            <div className="text-center">
              <div className="text-2xl font-bold text-white">50+</div>
              <div className="text-sm">Adventures</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-white">25+</div>
              <div className="text-sm">Cities</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-white">10K+</div>
              <div className="text-sm">Happy Customers</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-white">4.9★</div>
              <div className="text-sm">Average Rating</div>
            </div>
          </motion.div>
        </motion.div>
      </div>
    </section>
  );
}
