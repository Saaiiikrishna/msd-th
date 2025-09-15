'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { motion } from 'framer-motion';
import { 
  Filter, 
  Search, 
  MapPin, 
  Star, 
  Calendar,
  Users,
  DollarSign,
  X,
  RotateCcw,
  ChevronDown
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';

interface AdventuresFiltersProps {
  searchParams: {
    page?: string;
    search?: string;
    status?: string;
    category?: string;
    location?: string;
    sort?: string;
  };
}

interface FilterOptions {
  statuses: Array<{ label: string; value: string; count: number }>;
  categories: Array<{ label: string; value: string; count: number }>;
  locations: Array<{ label: string; value: string; count: number }>;
  sortOptions: Array<{ label: string; value: string }>;
}

export function AdventuresFilters({ searchParams }: AdventuresFiltersProps) {
  const router = useRouter();
  const currentSearchParams = useSearchParams();
  
  const [searchQuery, setSearchQuery] = useState(searchParams.search || '');
  const [selectedStatus, setSelectedStatus] = useState(searchParams.status || '');
  const [selectedCategory, setSelectedCategory] = useState(searchParams.category || '');
  const [selectedLocation, setSelectedLocation] = useState(searchParams.location || '');
  const [selectedSort, setSelectedSort] = useState(searchParams.sort || 'created_desc');
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const [filterOptions, setFilterOptions] = useState<FilterOptions>({
    statuses: [
      { label: 'All Status', value: '', count: 47 },
      { label: 'Active', value: 'active', count: 42 },
      { label: 'Draft', value: 'draft', count: 5 },
      { label: 'Archived', value: 'archived', count: 0 },
    ],
    categories: [
      { label: 'All Categories', value: '', count: 47 },
      { label: 'Heritage Walk', value: 'heritage', count: 12 },
      { label: 'Food Trail', value: 'food', count: 10 },
      { label: 'Adventure Sports', value: 'adventure', count: 8 },
      { label: 'Nature Trek', value: 'nature', count: 7 },
      { label: 'Cultural Tour', value: 'cultural', count: 6 },
      { label: 'Photography', value: 'photography', count: 4 },
    ],
    locations: [
      { label: 'All Locations', value: '', count: 47 },
      { label: 'Mumbai', value: 'mumbai', count: 15 },
      { label: 'Delhi', value: 'delhi', count: 12 },
      { label: 'Goa', value: 'goa', count: 8 },
      { label: 'Rajasthan', value: 'rajasthan', count: 7 },
      { label: 'Kerala', value: 'kerala', count: 5 },
    ],
    sortOptions: [
      { label: 'Newest First', value: 'created_desc' },
      { label: 'Oldest First', value: 'created_asc' },
      { label: 'Name A-Z', value: 'name_asc' },
      { label: 'Name Z-A', value: 'name_desc' },
      { label: 'Most Bookings', value: 'bookings_desc' },
      { label: 'Highest Rated', value: 'rating_desc' },
      { label: 'Price: Low to High', value: 'price_asc' },
      { label: 'Price: High to Low', value: 'price_desc' },
    ],
  });

  useEffect(() => {
    // Simulate loading filter options
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 500);

    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    // Update URL when filters change
    const params = new URLSearchParams(currentSearchParams?.toString() || '');
    
    if (searchQuery) params.set('search', searchQuery);
    else params.delete('search');
    
    if (selectedStatus) params.set('status', selectedStatus);
    else params.delete('status');
    
    if (selectedCategory) params.set('category', selectedCategory);
    else params.delete('category');
    
    if (selectedLocation) params.set('location', selectedLocation);
    else params.delete('location');
    
    if (selectedSort && selectedSort !== 'created_desc') params.set('sort', selectedSort);
    else params.delete('sort');

    // Reset to first page when filters change
    params.delete('page');

    router.push(`/admin/adventures?${params.toString()}`, { scroll: false });
  }, [searchQuery, selectedStatus, selectedCategory, selectedLocation, selectedSort, router, currentSearchParams]);

  const clearAllFilters = () => {
    setSearchQuery('');
    setSelectedStatus('');
    setSelectedCategory('');
    setSelectedLocation('');
    setSelectedSort('created_desc');
  };

  const hasActiveFilters = searchQuery || selectedStatus || selectedCategory || selectedLocation || selectedSort !== 'created_desc';

  if (isLoading) {
    return (
      <NeumorphicCard className="p-6">
        <div className="animate-pulse">
          <div className="flex gap-4 mb-4">
            <div className="h-10 bg-neutral-200 rounded-lg flex-1"></div>
            <div className="h-10 w-32 bg-neutral-200 rounded-lg"></div>
          </div>
          <div className="flex gap-4">
            <div className="h-10 w-24 bg-neutral-200 rounded-lg"></div>
            <div className="h-10 w-32 bg-neutral-200 rounded-lg"></div>
            <div className="h-10 w-28 bg-neutral-200 rounded-lg"></div>
          </div>
        </div>
      </NeumorphicCard>
    );
  }

  return (
    <NeumorphicCard className="p-6">
      <div className="space-y-4">
        {/* Search and Sort Row */}
        <div className="flex flex-col sm:flex-row gap-4">
          {/* Search */}
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-neutral-400" />
            <input
              type="text"
              placeholder="Search adventures by name, description, or location..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-neutral-100 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>

          {/* Sort */}
          <div className="relative">
            <select
              value={selectedSort}
              onChange={(e) => setSelectedSort(e.target.value)}
              className="appearance-none bg-neutral-100 border border-neutral-200 rounded-neumorphic px-4 py-2 pr-8 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              {filterOptions.sortOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 w-4 h-4 text-neutral-400 pointer-events-none" />
          </div>
        </div>

        {/* Quick Filters Row */}
        <div className="flex flex-wrap gap-3">
          {/* Status Filter */}
          <div className="relative">
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="appearance-none bg-white border border-neutral-200 rounded-neumorphic px-4 py-2 pr-8 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              {filterOptions.statuses.map((status) => (
                <option key={status.value} value={status.value}>
                  {status.label} ({status.count})
                </option>
              ))}
            </select>
            <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 w-3 h-3 text-neutral-400 pointer-events-none" />
          </div>

          {/* Category Filter */}
          <div className="relative">
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="appearance-none bg-white border border-neutral-200 rounded-neumorphic px-4 py-2 pr-8 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              {filterOptions.categories.map((category) => (
                <option key={category.value} value={category.value}>
                  {category.label} ({category.count})
                </option>
              ))}
            </select>
            <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 w-3 h-3 text-neutral-400 pointer-events-none" />
          </div>

          {/* Location Filter */}
          <div className="relative">
            <select
              value={selectedLocation}
              onChange={(e) => setSelectedLocation(e.target.value)}
              className="appearance-none bg-white border border-neutral-200 rounded-neumorphic px-4 py-2 pr-8 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              {filterOptions.locations.map((location) => (
                <option key={location.value} value={location.value}>
                  {location.label} ({location.count})
                </option>
              ))}
            </select>
            <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 w-3 h-3 text-neutral-400 pointer-events-none" />
          </div>

          {/* Advanced Filters Toggle */}
          <NeumorphicButton
            variant="outline"
            size="sm"
            onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
            className="flex items-center gap-2"
          >
            <Filter className="w-4 h-4" />
            Advanced
            <ChevronDown className={`w-4 h-4 transition-transform ${showAdvancedFilters ? 'rotate-180' : ''}`} />
          </NeumorphicButton>

          {/* Clear Filters */}
          {hasActiveFilters && (
            <NeumorphicButton
              variant="outline"
              size="sm"
              onClick={clearAllFilters}
              className="flex items-center gap-2 text-red-600 border-red-200 hover:bg-red-50"
            >
              <RotateCcw className="w-4 h-4" />
              Clear All
            </NeumorphicButton>
          )}
        </div>

        {/* Advanced Filters */}
        {showAdvancedFilters && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="border-t border-neutral-200 pt-4"
          >
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-2">
                  Price Range
                </label>
                <div className="flex gap-2">
                  <input
                    type="number"
                    placeholder="Min"
                    className="w-full px-3 py-2 bg-neutral-100 border border-neutral-200 rounded-neumorphic text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                  <input
                    type="number"
                    placeholder="Max"
                    className="w-full px-3 py-2 bg-neutral-100 border border-neutral-200 rounded-neumorphic text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-2">
                  Duration (hours)
                </label>
                <div className="flex gap-2">
                  <input
                    type="number"
                    placeholder="Min"
                    className="w-full px-3 py-2 bg-neutral-100 border border-neutral-200 rounded-neumorphic text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                  <input
                    type="number"
                    placeholder="Max"
                    className="w-full px-3 py-2 bg-neutral-100 border border-neutral-200 rounded-neumorphic text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-2">
                  Min Rating
                </label>
                <select className="w-full px-3 py-2 bg-neutral-100 border border-neutral-200 rounded-neumorphic text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                  <option value="">Any Rating</option>
                  <option value="4.5">4.5+ Stars</option>
                  <option value="4.0">4.0+ Stars</option>
                  <option value="3.5">3.5+ Stars</option>
                  <option value="3.0">3.0+ Stars</option>
                </select>
              </div>
            </div>
          </motion.div>
        )}

        {/* Active Filters Summary */}
        {hasActiveFilters && (
          <div className="flex flex-wrap gap-2 pt-2 border-t border-neutral-200">
            <span className="text-sm text-neutral-600">Active filters:</span>
            {searchQuery && (
              <span className="inline-flex items-center gap-1 px-2 py-1 bg-primary-100 text-primary-700 rounded-full text-xs">
                Search: "{searchQuery}"
                <button onClick={() => setSearchQuery('')}>
                  <X className="w-3 h-3" />
                </button>
              </span>
            )}
            {selectedStatus && (
              <span className="inline-flex items-center gap-1 px-2 py-1 bg-blue-100 text-blue-700 rounded-full text-xs">
                Status: {filterOptions.statuses.find(s => s.value === selectedStatus)?.label}
                <button onClick={() => setSelectedStatus('')}>
                  <X className="w-3 h-3" />
                </button>
              </span>
            )}
            {selectedCategory && (
              <span className="inline-flex items-center gap-1 px-2 py-1 bg-green-100 text-green-700 rounded-full text-xs">
                Category: {filterOptions.categories.find(c => c.value === selectedCategory)?.label}
                <button onClick={() => setSelectedCategory('')}>
                  <X className="w-3 h-3" />
                </button>
              </span>
            )}
            {selectedLocation && (
              <span className="inline-flex items-center gap-1 px-2 py-1 bg-purple-100 text-purple-700 rounded-full text-xs">
                Location: {filterOptions.locations.find(l => l.value === selectedLocation)?.label}
                <button onClick={() => setSelectedLocation('')}>
                  <X className="w-3 h-3" />
                </button>
              </span>
            )}
          </div>
        )}
      </div>
    </NeumorphicCard>
  );
}
