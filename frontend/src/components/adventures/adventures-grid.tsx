'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { 
  Grid, 
  List, 
  Map, 
  SortAsc, 
  Filter,
  ChevronDown,
  Heart,
  Share2
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { AdventureCard } from '@/components/adventures/adventure-card';
import { useAdventures } from '@/hooks/use-adventures';

const sortOptions = [
  { value: 'relevance', label: 'Most Relevant' },
  { value: 'price-low', label: 'Price: Low to High' },
  { value: 'price-high', label: 'Price: High to Low' },
  { value: 'rating', label: 'Highest Rated' },
  { value: 'newest', label: 'Newest First' },
  { value: 'popular', label: 'Most Popular' },
];

const viewModes = [
  { value: 'grid', icon: Grid, label: 'Grid View' },
  { value: 'list', icon: List, label: 'List View' },
  { value: 'map', icon: Map, label: 'Map View' },
];

interface AdventuresGridProps {
  searchParams: Record<string, string | undefined>;
}

export function AdventuresGrid({ searchParams }: AdventuresGridProps) {
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [sortBy, setSortBy] = useState(searchParams.sort || 'relevance');
  const [showSortDropdown, setShowSortDropdown] = useState(false);

  // Fetch adventures data
  const { 
    data: adventures, 
    isLoading, 
    error,
    totalCount 
  } = useAdventures(searchParams);

  const handleSortChange = (newSort: string) => {
    setSortBy(newSort);
    setShowSortDropdown(false);
    // Update URL with new sort parameter
    const params = new URLSearchParams(window.location.search);
    params.set('sort', newSort);
    window.history.pushState({}, '', `${window.location.pathname}?${params}`);
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        {/* Loading Header */}
        <div className="flex items-center justify-between">
          <div className="h-6 bg-neutral-200 rounded animate-pulse w-48" />
          <div className="h-10 bg-neutral-200 rounded animate-pulse w-32" />
        </div>
        
        {/* Loading Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(9)].map((_, i) => (
            <div key={i} className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <NeumorphicCard className="text-center py-12">
        <div className="text-error-500 mb-4">
          <Filter className="w-12 h-12 mx-auto" />
        </div>
        <h3 className="text-lg font-semibold text-neutral-800 mb-2">
          Unable to load adventures
        </h3>
        <p className="text-neutral-600 mb-4">
          There was an error loading the adventures. Please try again.
        </p>
        <NeumorphicButton 
          variant="primary" 
          onClick={() => window.location.reload()}
        >
          Try Again
        </NeumorphicButton>
      </NeumorphicCard>
    );
  }

  if (!adventures || adventures.length === 0) {
    return (
      <NeumorphicCard className="text-center py-12">
        <div className="text-neutral-400 mb-4">
          <Filter className="w-12 h-12 mx-auto" />
        </div>
        <h3 className="text-lg font-semibold text-neutral-800 mb-2">
          No adventures found
        </h3>
        <p className="text-neutral-600 mb-4">
          Try adjusting your filters or search criteria to find more adventures.
        </p>
        <NeumorphicButton variant="primary">
          Clear All Filters
        </NeumorphicButton>
      </NeumorphicCard>
    );
  }

  return (
    <div className="space-y-6">
      {/* Results Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
      >
        <div>
          <h2 className="text-2xl font-semibold text-neutral-800">
            {totalCount} Adventures Found
          </h2>
          <p className="text-neutral-600">
            Discover amazing treasure hunt experiences
          </p>
        </div>

        <div className="flex items-center gap-3">
          {/* View Mode Toggle */}
          <div className="flex items-center bg-neutral-100 rounded-neumorphic p-1">
            {viewModes.slice(0, 2).map((mode) => (
              <button
                key={mode.value}
                onClick={() => setViewMode(mode.value as 'grid' | 'list')}
                className={`p-2 rounded-lg transition-all duration-200 ${
                  viewMode === mode.value
                    ? 'bg-white shadow-neumorphic-soft text-primary-600'
                    : 'text-neutral-600 hover:text-neutral-800'
                }`}
                title={mode.label}
              >
                <mode.icon className="w-4 h-4" />
              </button>
            ))}
          </div>

          {/* Sort Dropdown */}
          <div className="relative">
            <NeumorphicButton
              variant="ghost"
              onClick={() => setShowSortDropdown(!showSortDropdown)}
              className="flex items-center gap-2"
            >
              <SortAsc className="w-4 h-4" />
              <span className="hidden sm:inline">
                {sortOptions.find(opt => opt.value === sortBy)?.label}
              </span>
              <ChevronDown className="w-4 h-4" />
            </NeumorphicButton>

            {showSortDropdown && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 10 }}
                className="absolute right-0 top-full mt-2 z-20"
              >
                <NeumorphicCard className="w-48 py-2">
                  {sortOptions.map((option) => (
                    <button
                      key={option.value}
                      onClick={() => handleSortChange(option.value)}
                      className={`w-full text-left px-4 py-2 text-sm transition-colors duration-200 ${
                        sortBy === option.value
                          ? 'text-primary-600 bg-primary-50'
                          : 'text-neutral-700 hover:bg-neutral-50'
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </NeumorphicCard>
              </motion.div>
            )}
          </div>
        </div>
      </motion.div>

      {/* Adventures Grid/List */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.6, delay: 0.2 }}
        className={
          viewMode === 'grid'
            ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6'
            : 'space-y-6'
        }
      >
        {adventures.map((adventure, index) => (
          <motion.div
            key={adventure.id}
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: index * 0.1 }}
          >
            <AdventureCard 
              adventure={adventure} 
              viewMode={viewMode}
              showQuickActions
            />
          </motion.div>
        ))}
      </motion.div>

      {/* Load More / Pagination */}
      {adventures.length < totalCount && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.4 }}
          className="text-center pt-8"
        >
          <NeumorphicButton variant="outline" size="lg">
            Load More Adventures
          </NeumorphicButton>
          <p className="text-sm text-neutral-600 mt-2">
            Showing {adventures.length} of {totalCount} adventures
          </p>
        </motion.div>
      )}
    </div>
  );
}
