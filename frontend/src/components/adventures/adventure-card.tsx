'use client';

import { useState } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { 
  MapPin, 
  Clock, 
  Users, 
  Star, 
  Heart, 
  Share2, 
  ArrowRight,
  Calendar,
  Trophy,
  Shield
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { formatCurrency } from '@/lib/utils';

interface Adventure {
  id: string;
  title: string;
  description: string;
  location: string;
  duration: string;
  difficulty: 'Easy' | 'Medium' | 'Hard';
  price: number;
  originalPrice?: number;
  rating: number;
  reviewCount: number;
  maxParticipants: number;
  image: string;
  tags: string[];
  highlights: string[];
  isPopular?: boolean;
  discount?: number;
  nextAvailableDate?: string;
  instantBooking?: boolean;
  safetyVerified?: boolean;
}

interface AdventureCardProps {
  adventure: Adventure;
  viewMode?: 'grid' | 'list';
  showQuickActions?: boolean;
  className?: string;
}

export function AdventureCard({ 
  adventure, 
  viewMode = 'grid', 
  showQuickActions = false,
  className = ''
}: AdventureCardProps) {
  const [isFavorited, setIsFavorited] = useState(false);
  const [isSharing, setIsSharing] = useState(false);

  const handleFavorite = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsFavorited(!isFavorited);
    // TODO: Implement favorite functionality
  };

  const handleShare = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsSharing(true);
    
    try {
      if (navigator.share) {
        await navigator.share({
          title: adventure.title,
          text: adventure.description,
          url: `/adventures/${adventure.id}`,
        });
      } else {
        // Fallback to clipboard
        await navigator.clipboard.writeText(
          `${window.location.origin}/adventures/${adventure.id}`
        );
        // TODO: Show toast notification
      }
    } catch (error) {
      console.error('Error sharing:', error);
    } finally {
      setIsSharing(false);
    }
  };

  const difficultyColors = {
    Easy: 'bg-success-100 text-success-700',
    Medium: 'bg-warning-100 text-warning-700',
    Hard: 'bg-error-100 text-error-700',
  };

  if (viewMode === 'list') {
    return (
      <Link href={`/adventures/${adventure.id}`}>
        <NeumorphicCard 
          interactive
          className={`group hover:shadow-neumorphic-elevated transition-all duration-300 ${className}`}
        >
          <div className="flex flex-col md:flex-row gap-6">
            {/* Image */}
            <div className="relative md:w-80 aspect-[4/3] md:aspect-[3/2] overflow-hidden rounded-neumorphic shrink-0">
              <Image
                src={adventure.image}
                alt={adventure.title}
                fill
                className="object-cover transition-transform duration-500 group-hover:scale-105"
                sizes="(max-width: 768px) 100vw, 320px"
              />
              
              {/* Badges */}
              <div className="absolute top-3 left-3 flex gap-2">
                {adventure.isPopular && (
                  <span className="px-2 py-1 bg-secondary-500 text-white text-xs font-semibold rounded-full">
                    Popular
                  </span>
                )}
                {adventure.discount && (
                  <span className="px-2 py-1 bg-error-500 text-white text-xs font-semibold rounded-full">
                    {adventure.discount}% OFF
                  </span>
                )}
                {adventure.instantBooking && (
                  <span className="px-2 py-1 bg-primary-500 text-white text-xs font-semibold rounded-full">
                    Instant
                  </span>
                )}
              </div>

              {/* Quick Actions */}
              {showQuickActions && (
                <div className="absolute top-3 right-3 flex gap-2">
                  <button
                    onClick={handleFavorite}
                    className="p-2 bg-white/90 backdrop-blur-sm rounded-full shadow-lg hover:bg-white transition-all duration-200"
                  >
                    <Heart 
                      className={`w-4 h-4 transition-colors duration-200 ${
                        isFavorited ? 'text-error-500 fill-current' : 'text-neutral-600'
                      }`} 
                    />
                  </button>
                  <button
                    onClick={handleShare}
                    disabled={isSharing}
                    className="p-2 bg-white/90 backdrop-blur-sm rounded-full shadow-lg hover:bg-white transition-all duration-200"
                  >
                    <Share2 className="w-4 h-4 text-neutral-600" />
                  </button>
                </div>
              )}
            </div>

            {/* Content */}
            <div className="flex-1 space-y-4">
              {/* Header */}
              <div>
                <div className="flex items-start justify-between gap-4 mb-2">
                  <h3 className="text-xl font-semibold text-neutral-800 group-hover:text-primary-600 transition-colors duration-200 line-clamp-2">
                    {adventure.title}
                  </h3>
                  <div className="text-right shrink-0">
                    <div className="flex items-center gap-2">
                      <span className="text-2xl font-bold text-neutral-800">
                        {formatCurrency(adventure.price)}
                      </span>
                      {adventure.originalPrice && adventure.originalPrice > adventure.price && (
                        <span className="text-sm text-neutral-500 line-through">
                          {formatCurrency(adventure.originalPrice)}
                        </span>
                      )}
                    </div>
                    <span className="text-xs text-neutral-500">per person</span>
                  </div>
                </div>

                <div className="flex items-center gap-4 text-sm text-neutral-600">
                  <div className="flex items-center gap-1">
                    <Star className="w-4 h-4 text-warning-500 fill-current" />
                    <span className="font-medium">{adventure.rating}</span>
                    <span>({adventure.reviewCount})</span>
                  </div>
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${difficultyColors[adventure.difficulty]}`}>
                    {adventure.difficulty}
                  </span>
                </div>
              </div>

              {/* Description */}
              <p className="text-neutral-600 line-clamp-2 leading-relaxed">
                {adventure.description}
              </p>

              {/* Details */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                <div className="flex items-center gap-2 text-neutral-600">
                  <MapPin className="w-4 h-4 text-primary-500" />
                  <span>{adventure.location}</span>
                </div>
                <div className="flex items-center gap-2 text-neutral-600">
                  <Clock className="w-4 h-4 text-primary-500" />
                  <span>{adventure.duration}</span>
                </div>
                <div className="flex items-center gap-2 text-neutral-600">
                  <Users className="w-4 h-4 text-primary-500" />
                  <span>Max {adventure.maxParticipants}</span>
                </div>
                {adventure.nextAvailableDate && (
                  <div className="flex items-center gap-2 text-neutral-600">
                    <Calendar className="w-4 h-4 text-primary-500" />
                    <span>{new Date(adventure.nextAvailableDate).toLocaleDateString()}</span>
                  </div>
                )}
              </div>

              {/* Tags */}
              <div className="flex flex-wrap gap-2">
                {adventure.tags.slice(0, 4).map((tag) => (
                  <span
                    key={tag}
                    className="px-2 py-1 bg-primary-50 text-primary-700 text-xs rounded-full"
                  >
                    {tag}
                  </span>
                ))}
              </div>

              {/* Action Button */}
              <div className="flex items-center justify-between pt-2">
                <div className="flex items-center gap-3">
                  {adventure.safetyVerified && (
                    <div className="flex items-center gap-1 text-xs text-success-600">
                      <Shield className="w-3 h-3" />
                      <span>Safety Verified</span>
                    </div>
                  )}
                  {adventure.isPopular && (
                    <div className="flex items-center gap-1 text-xs text-secondary-600">
                      <Trophy className="w-3 h-3" />
                      <span>Popular Choice</span>
                    </div>
                  )}
                </div>
                
                <NeumorphicButton
                  variant="primary"
                  size="sm"
                  className="group/btn"
                >
                  <span>Book Now</span>
                  <ArrowRight className="w-4 h-4 ml-1 transition-transform duration-200 group-hover/btn:translate-x-1" />
                </NeumorphicButton>
              </div>
            </div>
          </div>
        </NeumorphicCard>
      </Link>
    );
  }

  // Grid view (default)
  return (
    <Link href={`/adventures/${adventure.id}`}>
      <NeumorphicCard 
        interactive
        className={`group hover:shadow-neumorphic-elevated transition-all duration-500 hover:-translate-y-2 overflow-hidden ${className}`}
      >
        {/* Image Container */}
        <div className="relative aspect-[4/3] overflow-hidden rounded-t-neumorphic">
          <Image
            src={adventure.image}
            alt={adventure.title}
            fill
            className="object-cover transition-transform duration-500 group-hover:scale-110"
            sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
          />
          
          {/* Overlay */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
          
          {/* Badges */}
          <div className="absolute top-3 left-3 flex gap-2">
            {adventure.isPopular && (
              <span className="px-2 py-1 bg-secondary-500 text-white text-xs font-semibold rounded-full">
                Popular
              </span>
            )}
            {adventure.discount && (
              <span className="px-2 py-1 bg-error-500 text-white text-xs font-semibold rounded-full">
                {adventure.discount}% OFF
              </span>
            )}
          </div>

          {/* Quick Actions */}
          {showQuickActions && (
            <div className="absolute top-3 right-3 flex gap-2">
              <button
                onClick={handleFavorite}
                className="p-2 bg-white/90 backdrop-blur-sm rounded-full shadow-lg hover:bg-white transition-all duration-200"
              >
                <Heart 
                  className={`w-4 h-4 transition-colors duration-200 ${
                    isFavorited ? 'text-error-500 fill-current' : 'text-neutral-600'
                  }`} 
                />
              </button>
              <button
                onClick={handleShare}
                disabled={isSharing}
                className="p-2 bg-white/90 backdrop-blur-sm rounded-full shadow-lg hover:bg-white transition-all duration-200"
              >
                <Share2 className="w-4 h-4 text-neutral-600" />
              </button>
            </div>
          )}

          {/* Quick Book Button */}
          <div className="absolute bottom-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
            <NeumorphicButton size="sm" variant="primary">
              Quick Book
            </NeumorphicButton>
          </div>
        </div>

        {/* Content */}
        <div className="p-6 space-y-4">
          {/* Title and Rating */}
          <div>
            <h3 className="text-xl font-semibold text-neutral-800 mb-2 line-clamp-2 group-hover:text-primary-600 transition-colors duration-200">
              {adventure.title}
            </h3>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="flex items-center gap-1">
                  <Star className="w-4 h-4 text-warning-500 fill-current" />
                  <span className="text-sm font-medium text-neutral-700">
                    {adventure.rating}
                  </span>
                </div>
                <span className="text-sm text-neutral-500">
                  ({adventure.reviewCount})
                </span>
              </div>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${difficultyColors[adventure.difficulty]}`}>
                {adventure.difficulty}
              </span>
            </div>
          </div>

          {/* Description */}
          <p className="text-sm text-neutral-600 line-clamp-2 leading-relaxed">
            {adventure.description}
          </p>

          {/* Details */}
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm text-neutral-600">
              <MapPin className="w-4 h-4 text-primary-500" />
              <span>{adventure.location}</span>
            </div>
            <div className="flex items-center gap-4 text-sm text-neutral-600">
              <div className="flex items-center gap-1">
                <Clock className="w-4 h-4 text-primary-500" />
                <span>{adventure.duration}</span>
              </div>
              <div className="flex items-center gap-1">
                <Users className="w-4 h-4 text-primary-500" />
                <span>Max {adventure.maxParticipants}</span>
              </div>
            </div>
          </div>

          {/* Tags */}
          <div className="flex flex-wrap gap-1">
            {adventure.tags.slice(0, 3).map((tag) => (
              <span
                key={tag}
                className="px-2 py-1 bg-primary-50 text-primary-700 text-xs rounded-full"
              >
                {tag}
              </span>
            ))}
          </div>

          {/* Price and CTA */}
          <div className="flex items-center justify-between pt-4 border-t border-neutral-200">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-2xl font-bold text-neutral-800">
                  {formatCurrency(adventure.price)}
                </span>
                {adventure.originalPrice && adventure.originalPrice > adventure.price && (
                  <span className="text-sm text-neutral-500 line-through">
                    {formatCurrency(adventure.originalPrice)}
                  </span>
                )}
              </div>
              <span className="text-xs text-neutral-500">per person</span>
            </div>
            
            <NeumorphicButton
              variant="primary"
              size="sm"
              className="group/btn"
            >
              <span>Book</span>
              <ArrowRight className="w-4 h-4 ml-1 transition-transform duration-200 group-hover/btn:translate-x-1" />
            </NeumorphicButton>
          </div>
        </div>
      </NeumorphicCard>
    </Link>
  );
}
