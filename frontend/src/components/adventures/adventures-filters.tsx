'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { motion } from 'framer-motion';
import { 
  Filter, 
  MapPin, 
  Clock, 
  Users, 
  Star, 
  DollarSign, 
  Calendar,
  X,
  RotateCcw
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { formatCurrency } from '@/lib/utils';

interface FiltersState {
  location: string[];
  difficulty: string[];
  category: string[];
  priceRange: [number, number];
  duration: string[];
  groupSize: string[];
  rating: number;
  availability: string;
}

const filterOptions = {
  locations: [
    { value: 'mumbai', label: 'Mumbai', count: 12 },
    { value: 'delhi', label: 'Delhi', count: 8 },
    { value: 'bangalore', label: 'Bangalore', count: 6 },
    { value: 'goa', label: 'Goa', count: 4 },
    { value: 'pune', label: 'Pune', count: 5 },
    { value: 'hyderabad', label: 'Hyderabad', count: 3 },
  ],
  difficulties: [
    { value: 'easy', label: 'Easy', count: 15 },
    { value: 'medium', label: 'Medium', count: 18 },
    { value: 'hard', label: 'Hard', count: 5 },
  ],
  categories: [
    { value: 'heritage', label: 'Heritage', count: 12 },
    { value: 'food', label: 'Food & Culture', count: 8 },
    { value: 'technology', label: 'Technology', count: 6 },
    { value: 'adventure', label: 'Adventure', count: 10 },
    { value: 'beach', label: 'Beach', count: 4 },
    { value: 'nature', label: 'Nature', count: 7 },
  ],
  durations: [
    { value: '1-2', label: '1-2 hours', count: 8 },
    { value: '2-4', label: '2-4 hours', count: 20 },
    { value: '4-6', label: '4-6 hours', count: 12 },
    { value: '6+', label: '6+ hours', count: 5 },
  ],
  groupSizes: [
    { value: '1-2', label: '1-2 people', count: 10 },
    { value: '3-5', label: '3-5 people', count: 15 },
    { value: '6-10', label: '6-10 people', count: 18 },
    { value: '10+', label: '10+ people', count: 8 },
  ],
};

interface AdventuresFiltersProps {
  searchParams: Record<string, string | undefined>;
}

export function AdventuresFilters({ searchParams }: AdventuresFiltersProps) {
  const router = useRouter();
  const currentSearchParams = useSearchParams();
  
  const [filters, setFilters] = useState<FiltersState>({
    location: searchParams.location?.split(',') || [],
    difficulty: searchParams.difficulty?.split(',') || [],
    category: searchParams.category?.split(',') || [],
    priceRange: [0, 5000],
    duration: searchParams.duration?.split(',') || [],
    groupSize: searchParams.participants?.split(',') || [],
    rating: Number(searchParams.rating) || 0,
    availability: searchParams.availability || '',
  });

  const [isCollapsed, setIsCollapsed] = useState(false);

  // Update URL when filters change
  useEffect(() => {
    const params = new URLSearchParams(currentSearchParams?.toString() || '');
    
    // Update URL parameters
    Object.entries(filters).forEach(([key, value]) => {
      if (Array.isArray(value) && value.length > 0) {
        params.set(key, value.join(','));
      } else if (typeof value === 'number' && value > 0) {
        params.set(key, value.toString());
      } else if (typeof value === 'string' && value) {
        params.set(key, value);
      } else {
        params.delete(key);
      }
    });

    // Special handling for price range
    if (filters.priceRange[0] > 0 || filters.priceRange[1] < 5000) {
      params.set('priceMin', filters.priceRange[0].toString());
      params.set('priceMax', filters.priceRange[1].toString());
    } else {
      params.delete('priceMin');
      params.delete('priceMax');
    }

    router.push(`/adventures?${params.toString()}`, { scroll: false });
  }, [filters, router, currentSearchParams]);

  const updateFilter = (key: keyof FiltersState, value: any) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const toggleArrayFilter = (key: keyof FiltersState, value: string) => {
    setFilters(prev => ({
      ...prev,
      [key]: (prev[key] as string[]).includes(value)
        ? (prev[key] as string[]).filter(item => item !== value)
        : [...(prev[key] as string[]), value]
    }));
  };

  const clearAllFilters = () => {
    setFilters({
      location: [],
      difficulty: [],
      category: [],
      priceRange: [0, 5000],
      duration: [],
      groupSize: [],
      rating: 0,
      availability: '',
    });
  };

  const hasActiveFilters = Object.values(filters).some(value => 
    Array.isArray(value) ? value.length > 0 : 
    typeof value === 'number' ? value > 0 : 
    Boolean(value)
  );

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.6 }}
    >
      <NeumorphicCard className="sticky top-24">
        {/* Filter Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <Filter className="w-5 h-5 text-primary-600" />
            <h3 className="text-lg font-semibold text-neutral-800">Filters</h3>
            {hasActiveFilters && (
              <span className="px-2 py-1 bg-primary-100 text-primary-700 text-xs rounded-full">
                Active
              </span>
            )}
          </div>
          
          <div className="flex items-center gap-2">
            {hasActiveFilters && (
              <button
                onClick={clearAllFilters}
                className="p-1 text-neutral-500 hover:text-neutral-700 transition-colors duration-200"
                title="Clear all filters"
              >
                <RotateCcw className="w-4 h-4" />
              </button>
            )}
            <button
              onClick={() => setIsCollapsed(!isCollapsed)}
              className="p-1 text-neutral-500 hover:text-neutral-700 transition-colors duration-200 lg:hidden"
            >
              {isCollapsed ? <Filter className="w-4 h-4" /> : <X className="w-4 h-4" />}
            </button>
          </div>
        </div>

        <div className={`space-y-6 ${isCollapsed ? 'hidden lg:block' : ''}`}>
          {/* Location Filter */}
          <div>
            <h4 className="font-medium text-neutral-800 mb-3 flex items-center gap-2">
              <MapPin className="w-4 h-4 text-primary-500" />
              Location
            </h4>
            <div className="space-y-2">
              {filterOptions.locations.map((location) => (
                <label
                  key={location.value}
                  className="flex items-center gap-3 cursor-pointer group"
                >
                  <input
                    type="checkbox"
                    checked={filters.location.includes(location.value)}
                    onChange={() => toggleArrayFilter('location', location.value)}
                    className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
                  />
                  <span className="flex-1 text-neutral-700 group-hover:text-neutral-900 transition-colors duration-200">
                    {location.label}
                  </span>
                  <span className="text-xs text-neutral-500">
                    {location.count}
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* Category Filter */}
          <div>
            <h4 className="font-medium text-neutral-800 mb-3">Category</h4>
            <div className="space-y-2">
              {filterOptions.categories.map((category) => (
                <label
                  key={category.value}
                  className="flex items-center gap-3 cursor-pointer group"
                >
                  <input
                    type="checkbox"
                    checked={filters.category.includes(category.value)}
                    onChange={() => toggleArrayFilter('category', category.value)}
                    className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
                  />
                  <span className="flex-1 text-neutral-700 group-hover:text-neutral-900 transition-colors duration-200">
                    {category.label}
                  </span>
                  <span className="text-xs text-neutral-500">
                    {category.count}
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* Price Range */}
          <div>
            <h4 className="font-medium text-neutral-800 mb-3 flex items-center gap-2">
              <DollarSign className="w-4 h-4 text-primary-500" />
              Price Range
            </h4>
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <span className="text-sm text-neutral-600">
                  {formatCurrency(filters.priceRange[0])}
                </span>
                <span className="text-sm text-neutral-600">-</span>
                <span className="text-sm text-neutral-600">
                  {formatCurrency(filters.priceRange[1])}
                </span>
              </div>
              <input
                type="range"
                min="0"
                max="5000"
                step="100"
                value={filters.priceRange[1]}
                onChange={(e) => updateFilter('priceRange', [filters.priceRange[0], Number(e.target.value)])}
                className="w-full h-2 bg-neutral-200 rounded-lg appearance-none cursor-pointer slider"
              />
            </div>
          </div>

          {/* Difficulty */}
          <div>
            <h4 className="font-medium text-neutral-800 mb-3">Difficulty</h4>
            <div className="space-y-2">
              {filterOptions.difficulties.map((difficulty) => (
                <label
                  key={difficulty.value}
                  className="flex items-center gap-3 cursor-pointer group"
                >
                  <input
                    type="checkbox"
                    checked={filters.difficulty.includes(difficulty.value)}
                    onChange={() => toggleArrayFilter('difficulty', difficulty.value)}
                    className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
                  />
                  <span className="flex-1 text-neutral-700 group-hover:text-neutral-900 transition-colors duration-200">
                    {difficulty.label}
                  </span>
                  <span className="text-xs text-neutral-500">
                    {difficulty.count}
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* Duration */}
          <div>
            <h4 className="font-medium text-neutral-800 mb-3 flex items-center gap-2">
              <Clock className="w-4 h-4 text-primary-500" />
              Duration
            </h4>
            <div className="space-y-2">
              {filterOptions.durations.map((duration) => (
                <label
                  key={duration.value}
                  className="flex items-center gap-3 cursor-pointer group"
                >
                  <input
                    type="checkbox"
                    checked={filters.duration.includes(duration.value)}
                    onChange={() => toggleArrayFilter('duration', duration.value)}
                    className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
                  />
                  <span className="flex-1 text-neutral-700 group-hover:text-neutral-900 transition-colors duration-200">
                    {duration.label}
                  </span>
                  <span className="text-xs text-neutral-500">
                    {duration.count}
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* Group Size */}
          <div>
            <h4 className="font-medium text-neutral-800 mb-3 flex items-center gap-2">
              <Users className="w-4 h-4 text-primary-500" />
              Group Size
            </h4>
            <div className="space-y-2">
              {filterOptions.groupSizes.map((size) => (
                <label
                  key={size.value}
                  className="flex items-center gap-3 cursor-pointer group"
                >
                  <input
                    type="checkbox"
                    checked={filters.groupSize.includes(size.value)}
                    onChange={() => toggleArrayFilter('groupSize', size.value)}
                    className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
                  />
                  <span className="flex-1 text-neutral-700 group-hover:text-neutral-900 transition-colors duration-200">
                    {size.label}
                  </span>
                  <span className="text-xs text-neutral-500">
                    {size.count}
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* Rating Filter */}
          <div>
            <h4 className="font-medium text-neutral-800 mb-3 flex items-center gap-2">
              <Star className="w-4 h-4 text-primary-500" />
              Minimum Rating
            </h4>
            <div className="space-y-2">
              {[4, 3, 2, 1].map((rating) => (
                <label
                  key={rating}
                  className="flex items-center gap-3 cursor-pointer group"
                >
                  <input
                    type="radio"
                    name="rating"
                    checked={filters.rating === rating}
                    onChange={() => updateFilter('rating', rating)}
                    className="w-4 h-4 text-primary-600 border-neutral-300 focus:ring-primary-500"
                  />
                  <div className="flex items-center gap-1">
                    {[...Array(5)].map((_, i) => (
                      <Star
                        key={i}
                        className={`w-4 h-4 ${
                          i < rating ? 'text-warning-500 fill-current' : 'text-neutral-300'
                        }`}
                      />
                    ))}
                    <span className="text-sm text-neutral-600 ml-1">& up</span>
                  </div>
                </label>
              ))}
            </div>
          </div>

          {/* Availability */}
          <div>
            <h4 className="font-medium text-neutral-800 mb-3 flex items-center gap-2">
              <Calendar className="w-4 h-4 text-primary-500" />
              Availability
            </h4>
            <select
              value={filters.availability}
              onChange={(e) => updateFilter('availability', e.target.value)}
              className="w-full p-3 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            >
              <option value="">Any time</option>
              <option value="today">Available today</option>
              <option value="tomorrow">Available tomorrow</option>
              <option value="this-week">This week</option>
              <option value="this-month">This month</option>
            </select>
          </div>
        </div>
      </NeumorphicCard>
    </motion.div>
  );
}
