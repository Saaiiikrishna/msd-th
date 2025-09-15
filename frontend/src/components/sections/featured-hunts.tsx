'use client';

import { useState } from 'react';
import Image from 'next/image';
import { motion } from 'framer-motion';
import { MapPin, Clock, Users, Star, Heart, ArrowRight } from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { formatCurrency } from '@/lib/utils';

// Mock data - will be replaced with API call
const featuredHunts = [
  {
    id: '1',
    title: 'Mumbai Heritage Hunt',
    description: 'Explore the rich colonial heritage of South Mumbai through an exciting treasure hunt adventure.',
    location: 'Mumbai, Maharashtra',
    duration: '3 hours',
    difficulty: 'Easy',
    price: 500,
    originalPrice: 650,
    rating: 4.8,
    reviewCount: 124,
    maxParticipants: 8,
    image: 'https://images.unsplash.com/photo-1570168007204-dfb528c6958f?w=600&h=400&fit=crop',
    tags: ['Heritage', 'Walking', 'Photography'],
    highlights: ['Gateway of India', 'Taj Hotel', 'Colaba Causeway'],
    isPopular: true,
    discount: 23
  },
  {
    id: '2',
    title: 'Delhi Food Trail Mystery',
    description: 'Discover hidden culinary gems in Old Delhi while solving clues and tasting authentic street food.',
    location: 'Delhi, NCR',
    duration: '4 hours',
    difficulty: 'Medium',
    price: 750,
    originalPrice: 900,
    rating: 4.9,
    reviewCount: 89,
    maxParticipants: 6,
    image: 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=600&h=400&fit=crop',
    tags: ['Food', 'Culture', 'History'],
    highlights: ['Chandni Chowk', 'Spice Market', 'Jama Masjid'],
    isPopular: false,
    discount: 17
  },
  {
    id: '3',
    title: 'Bangalore Tech Hunt',
    description: 'A modern treasure hunt through Bangalore\'s tech corridors and startup ecosystem.',
    location: 'Bangalore, Karnataka',
    duration: '2.5 hours',
    difficulty: 'Easy',
    price: 600,
    originalPrice: 750,
    rating: 4.7,
    reviewCount: 156,
    maxParticipants: 10,
    image: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&h=400&fit=crop',
    tags: ['Technology', 'Innovation', 'Modern'],
    highlights: ['UB City', 'Cubbon Park', 'MG Road'],
    isPopular: true,
    discount: 20
  },
  {
    id: '4',
    title: 'Goa Beach Adventure',
    description: 'Combine beach fun with treasure hunting along the beautiful coastline of North Goa.',
    location: 'Goa',
    duration: '5 hours',
    difficulty: 'Medium',
    price: 1200,
    originalPrice: 1500,
    rating: 4.9,
    reviewCount: 203,
    maxParticipants: 12,
    image: 'https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=600&h=400&fit=crop',
    tags: ['Beach', 'Adventure', 'Water Sports'],
    highlights: ['Baga Beach', 'Anjuna Market', 'Fort Aguada'],
    isPopular: true,
    discount: 20
  }
];

const categories = ['All', 'Heritage', 'Food', 'Adventure', 'Technology', 'Beach'];

export function FeaturedHunts() {
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [favorites, setFavorites] = useState<Set<string>>(new Set());

  const filteredHunts = selectedCategory === 'All' 
    ? featuredHunts 
    : featuredHunts.filter(hunt => 
        hunt.tags.some(tag => tag.toLowerCase().includes(selectedCategory.toLowerCase()))
      );

  const toggleFavorite = (huntId: string) => {
    setFavorites(prev => {
      const newFavorites = new Set(prev);
      if (newFavorites.has(huntId)) {
        newFavorites.delete(huntId);
      } else {
        newFavorites.add(huntId);
      }
      return newFavorites;
    });
  };

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
          Featured <span className="text-neon-gradient">Adventures</span>
        </h2>
        <p className="text-xl text-neutral-600 max-w-3xl mx-auto leading-relaxed">
          Discover our most popular treasure hunts across India. Each adventure is carefully 
          crafted to provide unique experiences and unforgettable memories.
        </p>
      </motion.div>

      {/* Category Filter */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.2 }}
        className="flex flex-wrap justify-center gap-3"
      >
        {categories.map((category) => (
          <NeumorphicButton
            key={category}
            variant={selectedCategory === category ? 'primary' : 'ghost'}
            size="sm"
            onClick={() => setSelectedCategory(category)}
            className="transition-all duration-300"
          >
            {category}
          </NeumorphicButton>
        ))}
      </motion.div>

      {/* Hunts Grid - More Compact Layout */}
      <motion.div
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        viewport={{ once: true }}
        transition={{ duration: 0.8, delay: 0.4 }}
        className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"
      >
        {filteredHunts.map((hunt, index) => (
          <motion.div
            key={hunt.id}
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: index * 0.1 }}
            className="group"
          >
            <NeumorphicCard
              interactive
              className="overflow-hidden hover:shadow-neumorphic-hover transition-all duration-300 group-hover:-translate-y-1"
            >
              {/* Image Container - More Compact */}
              <div className="relative aspect-[16/10] overflow-hidden">
                <Image
                  src={hunt.image}
                  alt={hunt.title}
                  fill
                  className="object-cover transition-transform duration-300 group-hover:scale-105"
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 25vw"
                />

                {/* Compact Badges */}
                <div className="absolute top-2 left-2 flex gap-1">
                  {hunt.isPopular && (
                    <span className="px-2 py-1 bg-gradient-neon-purple text-white text-xs font-medium rounded-full backdrop-blur-sm">
                      Popular
                    </span>
                  )}
                  {hunt.discount > 0 && (
                    <span className="px-2 py-1 bg-gradient-neon-pink text-white text-xs font-medium rounded-full backdrop-blur-sm">
                      {hunt.discount}% OFF
                    </span>
                  )}
                </div>

                {/* Favorite Button - Smaller */}
                <button
                  onClick={() => toggleFavorite(hunt.id)}
                  className="absolute top-2 right-2 p-1.5 bg-white/90 backdrop-blur-sm rounded-full shadow-neumorphic-soft hover:bg-white transition-all duration-200"
                >
                  <Heart
                    className={`w-3.5 h-3.5 transition-colors duration-200 ${
                      favorites.has(hunt.id)
                        ? 'text-error-500 fill-current'
                        : 'text-neutral-600'
                    }`}
                  />
                </button>

                {/* Quick Action Button - Smaller */}
                <div className="absolute bottom-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                  <NeumorphicButton size="sm" variant="primary" className="text-xs px-3 py-1.5">
                    Book Now
                  </NeumorphicButton>
                </div>
              </div>

              {/* Content - Compact Design */}
              <div className="p-4 space-y-3">
                {/* Title and Rating */}
                <div>
                  <h3 className="text-lg font-semibold text-neutral-800 mb-1 line-clamp-1">
                    {hunt.title}
                  </h3>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1">
                      <Star className="w-3.5 h-3.5 text-warning-500 fill-current" />
                      <span className="text-sm font-medium text-neutral-700">
                        {hunt.rating}
                      </span>
                      <span className="text-xs text-neutral-500">
                        ({hunt.reviewCount})
                      </span>
                    </div>
                    <div className="flex items-center gap-1 text-xs text-neutral-500">
                      <MapPin className="w-3 h-3" />
                      <span>{hunt.location.split(',')[0]}</span>
                    </div>
                  </div>
                </div>

                {/* Description - Shorter */}
                <p className="text-sm text-neutral-600 line-clamp-2 leading-relaxed">
                  {hunt.description}
                </p>

                {/* Key Details - Compact */}
                <div className="flex items-center justify-between text-xs text-neutral-600">
                  <div className="flex items-center gap-1">
                    <Clock className="w-3 h-3 text-primary-500" />
                    <span>{hunt.duration}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Users className="w-3 h-3 text-primary-500" />
                    <span>Max {hunt.maxParticipants}</span>
                  </div>
                </div>

                {/* Tags - Limited to 2 */}
                <div className="flex gap-1">
                  {hunt.tags.slice(0, 2).map((tag) => (
                    <span
                      key={tag}
                      className="px-2 py-0.5 bg-gradient-subtle text-primary-600 text-xs rounded-full border border-primary-200"
                    >
                      {tag}
                    </span>
                  ))}
                </div>

                {/* Price and CTA - Compact */}
                <div className="flex items-center justify-between pt-2 border-t border-neutral-200">
                  <div>
                    <div className="flex items-center gap-1">
                      <span className="text-lg font-bold text-neutral-800">
                        {formatCurrency(hunt.price)}
                      </span>
                      {hunt.originalPrice > hunt.price && (
                        <span className="text-xs text-neutral-500 line-through">
                          {formatCurrency(hunt.originalPrice)}
                        </span>
                      )}
                    </div>
                    <span className="text-xs text-neutral-500">per person</span>
                  </div>

                  <NeumorphicButton
                    variant="primary"
                    size="sm"
                    className="group/btn text-xs px-3 py-1.5"
                    onClick={() => alert('hello btn clicked')}
                  >
                    <span>Book</span>
                    <ArrowRight className="w-3 h-3 ml-1 transition-transform duration-200 group-hover/btn:translate-x-1" />
                  </NeumorphicButton>
                </div>
              </div>
            </NeumorphicCard>
          </motion.div>
        ))}
      </motion.div>

      {/* View All Button */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.6, delay: 0.6 }}
        className="text-center"
      >
        <NeumorphicButton variant="outline" size="lg">
          View All Adventures
          <ArrowRight className="w-5 h-5 ml-2" />
        </NeumorphicButton>
      </motion.div>
    </section>
  );
}
