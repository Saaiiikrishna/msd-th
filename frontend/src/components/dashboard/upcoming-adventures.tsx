'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Calendar,
  MapPin,
  Clock,
  Users,
  Star,
  Heart,
  Share2,
  Plus,
  TrendingUp,
  Filter
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import Link from 'next/link';

interface Adventure {
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
  isPopular: boolean;
  isNew: boolean;
  isFavorite: boolean;
  tags: string[];
}

export function UpcomingAdventures() {
  const [adventures, setAdventures] = useState<Adventure[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'popular' | 'new' | 'favorites'>('all');

  useEffect(() => {
    const fetchUpcomingAdventures = async () => {
      setIsLoading(true);
      
      // Mock data - replace with actual API call
      const mockAdventures: Adventure[] = [
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
          isPopular: true,
          isNew: false,
          isFavorite: true,
          tags: ['history', 'architecture', 'walking'],
        },
        {
          id: '2',
          title: 'Delhi Food Trail',
          description: 'Taste the authentic flavors of Old Delhi street food on this culinary journey.',
          location: 'Old Delhi',
          price: 1200,
          originalPrice: 1500,
          duration: 4,
          rating: 4.9,
          reviewCount: 98,
          imageUrl: '/images/delhi-food.jpg',
          category: 'Food',
          difficulty: 'easy',
          availableSlots: 5,
          totalSlots: 12,
          nextAvailableDate: '2024-02-18',
          isPopular: true,
          isNew: false,
          isFavorite: false,
          tags: ['food', 'culture', 'street food'],
        },
        {
          id: '3',
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
          isPopular: false,
          isNew: true,
          isFavorite: false,
          tags: ['beach', 'water sports', 'adventure'],
        },
        {
          id: '4',
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
          isPopular: true,
          isNew: false,
          isFavorite: true,
          tags: ['desert', 'culture', 'camel ride'],
        },
        {
          id: '5',
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
          isPopular: false,
          isNew: true,
          isFavorite: false,
          tags: ['nature', 'houseboat', 'peaceful'],
        },
        {
          id: '6',
          title: 'Himachal Mountain Trek',
          description: 'Challenging trek through the beautiful mountains of Himachal Pradesh.',
          location: 'Manali',
          price: 4500,
          duration: 12,
          rating: 4.9,
          reviewCount: 143,
          imageUrl: '/images/himachal-trek.jpg',
          category: 'Trekking',
          difficulty: 'challenging',
          availableSlots: 3,
          totalSlots: 6,
          nextAvailableDate: '2024-03-05',
          isPopular: true,
          isNew: true,
          isFavorite: false,
          tags: ['mountains', 'trekking', 'adventure'],
        },
      ];

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      setAdventures(mockAdventures);
      setIsLoading(false);
    };

    fetchUpcomingAdventures();
  }, []);

  const getDifficultyColor = (difficulty: Adventure['difficulty']) => {
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

  const filteredAdventures = adventures.filter(adventure => {
    if (filter === 'all') return true;
    if (filter === 'popular') return adventure.isPopular;
    if (filter === 'new') return adventure.isNew;
    if (filter === 'favorites') return adventure.isFavorite;
    return true;
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
    <NeumorphicCard className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-neutral-900">
          Recommended Adventures
        </h2>
        <div className="flex items-center gap-3">
          <div className="flex bg-neutral-100 rounded-lg p-1">
            {(['all', 'popular', 'new', 'favorites'] as const).map((filterOption) => (
              <button
                key={filterOption}
                onClick={() => setFilter(filterOption)}
                className={`px-3 py-1 text-sm rounded-md transition-colors duration-200 capitalize ${
                  filter === filterOption
                    ? 'bg-white text-neutral-900 shadow-sm'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
              >
                {filterOption}
              </button>
            ))}
          </div>
          <NeumorphicButton variant="ghost" size="sm">
            <Filter className="w-4 h-4" />
          </NeumorphicButton>
        </div>
      </div>

      {filteredAdventures.length === 0 ? (
        <div className="text-center py-8">
          <MapPin className="w-12 h-12 text-neutral-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-neutral-900 mb-2">
            No adventures found
          </h3>
          <p className="text-neutral-600 mb-6">
            {filter === 'favorites' 
              ? "You haven't added any adventures to your favorites yet." 
              : `No ${filter} adventures available right now.`}
          </p>
          <Link href="/adventures">
            <NeumorphicButton variant="primary">
              Browse All Adventures
            </NeumorphicButton>
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredAdventures.map((adventure, index) => (
            <motion.div
              key={adventure.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: index * 0.1 }}
            >
              <div className="bg-white rounded-neumorphic overflow-hidden hover:shadow-lg transition-shadow duration-300 group">
                {/* Adventure Image */}
                <div className="relative h-48 bg-gradient-to-br from-primary-400 to-primary-600">
                  {/* Image placeholder */}
                  <div className="absolute inset-0 bg-black bg-opacity-20"></div>
                  
                  {/* Badges */}
                  <div className="absolute top-3 left-3 flex gap-2">
                    {adventure.isNew && (
                      <span className="px-2 py-1 bg-green-500 text-white text-xs font-medium rounded-full">
                        New
                      </span>
                    )}
                    {adventure.isPopular && (
                      <span className="px-2 py-1 bg-orange-500 text-white text-xs font-medium rounded-full flex items-center gap-1">
                        <TrendingUp className="w-3 h-3" />
                        Popular
                      </span>
                    )}
                  </div>
                  
                  {/* Actions */}
                  <div className="absolute top-3 right-3 flex gap-2">
                    <button className={`w-8 h-8 rounded-full flex items-center justify-center transition-colors duration-200 ${
                      adventure.isFavorite 
                        ? 'bg-red-500 text-white' 
                        : 'bg-white bg-opacity-80 text-neutral-600 hover:bg-opacity-100'
                    }`}>
                      <Heart className={`w-4 h-4 ${adventure.isFavorite ? 'fill-current' : ''}`} />
                    </button>
                    <button className="w-8 h-8 bg-white bg-opacity-80 rounded-full flex items-center justify-center text-neutral-600 hover:bg-opacity-100 transition-colors duration-200">
                      <Share2 className="w-4 h-4" />
                    </button>
                  </div>
                  
                  {/* Price */}
                  <div className="absolute bottom-3 right-3 bg-white bg-opacity-90 rounded-lg px-3 py-1">
                    <div className="flex items-center gap-2">
                      {adventure.originalPrice && (
                        <span className="text-xs text-neutral-500 line-through">
                          ₹{adventure.originalPrice}
                        </span>
                      )}
                      <span className="font-semibold text-neutral-900">
                        ₹{adventure.price}
                      </span>
                    </div>
                  </div>
                </div>
                
                {/* Adventure Details */}
                <div className="p-4">
                  <div className="flex items-start justify-between mb-2">
                    <Link 
                      href={`/adventures/${adventure.id}`}
                      className="font-semibold text-neutral-900 hover:text-primary-600 transition-colors duration-200 group-hover:text-primary-600"
                    >
                      {adventure.title}
                    </Link>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getDifficultyColor(adventure.difficulty)}`}>
                      {adventure.difficulty}
                    </span>
                  </div>
                  
                  <p className="text-sm text-neutral-600 mb-3 line-clamp-2">
                    {adventure.description}
                  </p>
                  
                  <div className="flex items-center gap-4 mb-3 text-sm text-neutral-600">
                    <div className="flex items-center gap-1">
                      <MapPin className="w-3 h-3" />
                      <span>{adventure.location}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      <span>{adventure.duration}h</span>
                    </div>
                  </div>
                  
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-1">
                      <Star className="w-4 h-4 text-yellow-500 fill-current" />
                      <span className="font-medium text-neutral-900">{adventure.rating}</span>
                      <span className="text-sm text-neutral-500">({adventure.reviewCount})</span>
                    </div>
                    
                    <div className="text-sm text-neutral-600">
                      <span className="font-medium text-green-600">{adventure.availableSlots}</span>
                      /{adventure.totalSlots} slots
                    </div>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <div className="text-sm text-neutral-600">
                      Next: {new Date(adventure.nextAvailableDate).toLocaleDateString()}
                    </div>
                    
                    <Link href={`/adventures/${adventure.id}`}>
                      <NeumorphicButton variant="primary" size="sm">
                        Book Now
                      </NeumorphicButton>
                    </Link>
                  </div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      )}
      
      {filteredAdventures.length > 0 && (
        <div className="mt-8 text-center">
          <Link href="/adventures">
            <NeumorphicButton variant="outline">
              <Plus className="w-4 h-4 mr-2" />
              View All Adventures
            </NeumorphicButton>
          </Link>
        </div>
      )}
    </NeumorphicCard>
  );
}
