'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Heart,
  MapPin,
  Star,
  Clock,
  Users,
  Calendar,
  Share2,
  Trash2,
  Filter,
  Search
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import Link from 'next/link';

interface FavoriteAdventure {
  id: string;
  title: string;
  description: string;
  location: string;
  price: number;
  originalPrice?: number;
  duration: number;
  rating: number;
  reviewCount: number;
  imageUrl: string;
  category: string;
  difficulty: 'easy' | 'moderate' | 'challenging';
  availableSlots: number;
  totalSlots: number;
  nextAvailableDate: string;
  addedToFavoritesDate: string;
  tags: string[];
}

export function FavoriteAdventures() {
  const [favorites, setFavorites] = useState<FavoriteAdventure[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<string>('all');

  useEffect(() => {
    const fetchFavoriteAdventures = async () => {
      setIsLoading(true);
      
      // Mock data - replace with actual API call
      const mockFavorites: FavoriteAdventure[] = [
        {
          id: '1',
          title: 'Mumbai Heritage Walk',
          description: 'Explore the rich history and architecture of South Mumbai with expert guides.',
          location: 'South Mumbai',
          price: 1500,
          duration: 3,
          rating: 4.8,
          reviewCount: 124,
          imageUrl: '/images/mumbai-heritage.jpg',
          category: 'Heritage',
          difficulty: 'easy',
          availableSlots: 8,
          totalSlots: 15,
          nextAvailableDate: '2024-02-15',
          addedToFavoritesDate: '2024-01-20',
          tags: ['history', 'architecture', 'walking'],
        },
        {
          id: '2',
          title: 'Rajasthan Desert Safari',
          description: 'Experience the magic of the Thar Desert with camel rides and cultural performances.',
          location: 'Jaisalmer',
          price: 3000,
          duration: 8,
          rating: 4.7,
          reviewCount: 89,
          imageUrl: '/images/rajasthan-desert.jpg',
          category: 'Desert',
          difficulty: 'moderate',
          availableSlots: 6,
          totalSlots: 10,
          nextAvailableDate: '2024-02-25',
          addedToFavoritesDate: '2024-01-25',
          tags: ['desert', 'culture', 'camel ride'],
        },
        {
          id: '3',
          title: 'Himachal Mountain Trek',
          description: 'Challenging trek through the beautiful mountains of Himachal Pradesh.',
          location: 'Manali',
          price: 4500,
          originalPrice: 5000,
          duration: 12,
          rating: 4.9,
          reviewCount: 143,
          imageUrl: '/images/himachal-trek.jpg',
          category: 'Trekking',
          difficulty: 'challenging',
          availableSlots: 3,
          totalSlots: 6,
          nextAvailableDate: '2024-03-05',
          addedToFavoritesDate: '2024-02-01',
          tags: ['mountains', 'trekking', 'adventure'],
        },
        {
          id: '4',
          title: 'Kerala Backwaters Tour',
          description: 'Peaceful houseboat journey through the serene backwaters of Kerala.',
          location: 'Alleppey',
          price: 2800,
          duration: 5,
          rating: 4.5,
          reviewCount: 56,
          imageUrl: '/images/kerala-backwaters.jpg',
          category: 'Nature',
          difficulty: 'easy',
          availableSlots: 4,
          totalSlots: 8,
          nextAvailableDate: '2024-03-01',
          addedToFavoritesDate: '2024-01-30',
          tags: ['nature', 'houseboat', 'peaceful'],
        },
        {
          id: '5',
          title: 'Goa Beach Adventure',
          description: 'Water sports and beach activities in the beautiful beaches of North Goa.',
          location: 'North Goa',
          price: 2500,
          duration: 6,
          rating: 4.6,
          reviewCount: 67,
          imageUrl: '/images/goa-beach.jpg',
          category: 'Adventure',
          difficulty: 'moderate',
          availableSlots: 12,
          totalSlots: 20,
          nextAvailableDate: '2024-02-20',
          addedToFavoritesDate: '2024-02-05',
          tags: ['beach', 'water sports', 'adventure'],
        },
      ];

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      setFavorites(mockFavorites);
      setIsLoading(false);
    };

    fetchFavoriteAdventures();
  }, []);

  const handleRemoveFromFavorites = async (adventureId: string) => {
    // Mock API call to remove from favorites
    setFavorites(prev => prev.filter(fav => fav.id !== adventureId));
  };

  const getDifficultyColor = (difficulty: FavoriteAdventure['difficulty']) => {
    switch (difficulty) {
      case 'easy':
        return 'text-green-600 bg-green-100';
      case 'moderate':
        return 'text-yellow-600 bg-yellow-100';
      case 'challenging':
        return 'text-red-600 bg-red-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const categories = ['all', ...Array.from(new Set(favorites.map(fav => fav.category)))];

  const filteredFavorites = favorites.filter(favorite => {
    const matchesSearch = favorite.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         favorite.location.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         favorite.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesCategory = categoryFilter === 'all' || favorite.category === categoryFilter;
    return matchesSearch && matchesCategory;
  });

  if (isLoading) {
    return (
      <NeumorphicCard className="p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-neutral-200 rounded w-1/3 mb-6"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="space-y-4">
                <div className="h-48 bg-neutral-200 rounded-lg"></div>
                <div className="space-y-2">
                  <div className="h-4 bg-neutral-200 rounded w-3/4"></div>
                  <div className="h-3 bg-neutral-200 rounded w-1/2"></div>
                  <div className="h-3 bg-neutral-200 rounded w-1/3"></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </NeumorphicCard>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <NeumorphicCard className="p-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-xl font-semibold text-neutral-900">
              Favorite Adventures
            </h2>
            <p className="text-neutral-600 mt-1">
              {favorites.length} adventures saved to your favorites
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Heart className="w-5 h-5 text-red-500 fill-current" />
            <span className="text-sm text-neutral-600">{favorites.length}</span>
          </div>
        </div>

        {/* Search and Filters */}
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-neutral-400" />
            <input
              type="text"
              placeholder="Search favorites..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-neutral-100 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="px-4 py-2 bg-neutral-100 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500 capitalize"
          >
            {categories.map(category => (
              <option key={category} value={category}>
                {category === 'all' ? 'All Categories' : category}
              </option>
            ))}
          </select>
        </div>
      </NeumorphicCard>

      {/* Favorites Grid */}
      {filteredFavorites.length === 0 ? (
        <NeumorphicCard className="p-12 text-center">
          <Heart className="w-16 h-16 text-neutral-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-neutral-900 mb-2">
            {searchQuery || categoryFilter !== 'all' ? 'No matching favorites' : 'No favorites yet'}
          </h3>
          <p className="text-neutral-600 mb-6">
            {searchQuery || categoryFilter !== 'all' 
              ? 'Try adjusting your search or filter criteria.'
              : 'Start exploring adventures and add them to your favorites!'}
          </p>
          <Link href="/adventures">
            <NeumorphicButton variant="primary">
              Explore Adventures
            </NeumorphicButton>
          </Link>
        </NeumorphicCard>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredFavorites.map((favorite, index) => (
            <motion.div
              key={favorite.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: index * 0.1 }}
            >
              <NeumorphicCard className="overflow-hidden hover:shadow-lg transition-shadow duration-300 group">
                {/* Adventure Image */}
                <div className="relative h-48 bg-gradient-to-br from-primary-400 to-primary-600">
                  {/* Image placeholder */}
                  <div className="absolute inset-0 bg-black bg-opacity-20"></div>
                  
                  {/* Actions */}
                  <div className="absolute top-3 right-3 flex gap-2">
                    <button
                      onClick={() => handleRemoveFromFavorites(favorite.id)}
                      className="w-8 h-8 bg-red-500 text-white rounded-full flex items-center justify-center hover:bg-red-600 transition-colors duration-200"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                    <button className="w-8 h-8 bg-white bg-opacity-80 rounded-full flex items-center justify-center text-neutral-600 hover:bg-opacity-100 transition-colors duration-200">
                      <Share2 className="w-4 h-4" />
                    </button>
                  </div>
                  
                  {/* Price */}
                  <div className="absolute bottom-3 right-3 bg-white bg-opacity-90 rounded-lg px-3 py-1">
                    <div className="flex items-center gap-2">
                      {favorite.originalPrice && (
                        <span className="text-xs text-neutral-500 line-through">
                          ₹{favorite.originalPrice}
                        </span>
                      )}
                      <span className="font-semibold text-neutral-900">
                        ₹{favorite.price}
                      </span>
                    </div>
                  </div>
                </div>
                
                {/* Adventure Details */}
                <div className="p-4">
                  <div className="flex items-start justify-between mb-2">
                    <Link 
                      href={`/adventures/${favorite.id}`}
                      className="font-semibold text-neutral-900 hover:text-primary-600 transition-colors duration-200 group-hover:text-primary-600"
                    >
                      {favorite.title}
                    </Link>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getDifficultyColor(favorite.difficulty)}`}>
                      {favorite.difficulty}
                    </span>
                  </div>
                  
                  <p className="text-sm text-neutral-600 mb-3 line-clamp-2">
                    {favorite.description}
                  </p>
                  
                  <div className="flex items-center gap-4 mb-3 text-sm text-neutral-600">
                    <div className="flex items-center gap-1">
                      <MapPin className="w-3 h-3" />
                      <span>{favorite.location}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      <span>{favorite.duration}h</span>
                    </div>
                  </div>
                  
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-1">
                      <Star className="w-4 h-4 text-yellow-500 fill-current" />
                      <span className="font-medium text-neutral-900">{favorite.rating}</span>
                      <span className="text-sm text-neutral-500">({favorite.reviewCount})</span>
                    </div>
                    
                    <div className="text-sm text-neutral-600">
                      <span className="font-medium text-green-600">{favorite.availableSlots}</span>
                      /{favorite.totalSlots} slots
                    </div>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <div className="text-xs text-neutral-500">
                      Added {new Date(favorite.addedToFavoritesDate).toLocaleDateString()}
                    </div>
                    
                    <Link href={`/adventures/${favorite.id}`}>
                      <NeumorphicButton variant="primary" size="sm">
                        Book Now
                      </NeumorphicButton>
                    </Link>
                  </div>
                </div>
              </NeumorphicCard>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
