'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { motion } from 'framer-motion';
import { Search, Filter, MapPin, Clock, Users, Star, ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { SmartSearch } from '@/components/search/smart-search';
import { AdventureCard } from '@/components/adventures/adventure-card';

interface SearchResult {
  id: string;
  type: 'adventure' | 'city' | 'category';
  title: string;
  description: string;
  image: string;
  location: string;
  duration: string;
  price: number;
  rating: number;
  reviewCount: number;
  difficulty: string;
  maxParticipants: number;
  tags: string[];
}

function SearchPageContent() {
  const searchParams = useSearchParams();
  const query = searchParams?.get('q') || '';
  
  const [results, setResults] = useState<SearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [totalResults, setTotalResults] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [sortBy, setSortBy] = useState<'relevance' | 'price' | 'rating' | 'duration'>('relevance');
  const [showFilters, setShowFilters] = useState(false);

  // Mock search results - replace with actual API call
  const mockResults: SearchResult[] = [
    {
      id: '1',
      type: 'adventure',
      title: 'Mumbai Heritage Walk',
      description: 'Explore the historic streets of Mumbai and discover hidden gems in the bustling city.',
      image: '/images/adventures/mumbai-heritage.jpg',
      location: 'Mumbai, Maharashtra',
      duration: '3 hours',
      price: 1200,
      rating: 4.8,
      reviewCount: 156,
      difficulty: 'Easy',
      maxParticipants: 20,
      tags: ['Heritage', 'Walking', 'History', 'Culture']
    },
    {
      id: '2',
      type: 'adventure',
      title: 'Tech Park Treasure Hunt',
      description: 'Navigate through Bangalore\'s tech corridors in this modern treasure hunting experience.',
      image: '/images/adventures/bangalore-tech.jpg',
      location: 'Bangalore, Karnataka',
      duration: '2 hours',
      price: 800,
      rating: 4.6,
      reviewCount: 89,
      difficulty: 'Medium',
      maxParticipants: 15,
      tags: ['Technology', 'Modern', 'Team Building', 'Corporate']
    },
    {
      id: '3',
      type: 'adventure',
      title: 'Old Delhi Food Trail',
      description: 'Discover the culinary secrets of Old Delhi while solving clues and finding treasures.',
      image: '/images/adventures/delhi-food.jpg',
      location: 'Delhi, NCR',
      duration: '4 hours',
      price: 1500,
      rating: 4.9,
      reviewCount: 203,
      difficulty: 'Easy',
      maxParticipants: 12,
      tags: ['Food', 'Culture', 'Heritage', 'Walking']
    }
  ];

  // Fetch search results
  useEffect(() => {
    if (!query) return;

    const fetchResults = async () => {
      setIsLoading(true);
      
      try {
        // TODO: Replace with actual API call
        // const response = await treasureService.search(query, { page: currentPage, sortBy });
        
        // Simulate API delay
        await new Promise(resolve => setTimeout(resolve, 500));
        
        // Filter mock results based on query
        const filtered = mockResults.filter(result =>
          result.title.toLowerCase().includes(query.toLowerCase()) ||
          result.description.toLowerCase().includes(query.toLowerCase()) ||
          result.location.toLowerCase().includes(query.toLowerCase()) ||
          result.tags.some(tag => tag.toLowerCase().includes(query.toLowerCase()))
        );
        
        setResults(filtered);
        setTotalResults(filtered.length);
      } catch (error) {
        console.error('Error fetching search results:', error);
        setResults([]);
        setTotalResults(0);
      } finally {
        setIsLoading(false);
      }
    };

    fetchResults();
  }, [query, currentPage, sortBy]);

  const handleSortChange = (newSortBy: typeof sortBy) => {
    setSortBy(newSortBy);
    setCurrentPage(1);
  };

  if (!query) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-neumorphic-light-bg via-neumorphic-light-bg to-primary-50">
        <div className="container mx-auto px-4 py-20">
          <div className="text-center max-w-2xl mx-auto">
            <h1 className="text-4xl font-bold text-neutral-800 mb-6">Search Adventures</h1>
            <p className="text-xl text-neutral-600 mb-8">
              Find your perfect treasure hunting experience
            </p>
            <SmartSearch variant="page" placeholder="Search by city, activity, or experience..." />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-neumorphic-light-bg via-neumorphic-light-bg to-primary-50">
      {/* Header */}
      <section className="py-8 bg-white/50 border-b border-neutral-200">
        <div className="container mx-auto px-4">
          <div className="flex items-center gap-4 mb-6">
            <Link href="/" className="flex items-center text-neutral-600 hover:text-neutral-800 transition-colors">
              <ArrowLeft className="w-4 h-4 mr-2" />
              Back to home
            </Link>
          </div>
          
          <div className="flex flex-col lg:flex-row lg:items-center gap-6">
            <div className="flex-1">
              <SmartSearch 
                variant="page" 
                placeholder="Search by city, activity, or experience..."
                className="max-w-none"
              />
            </div>
            
            <div className="flex items-center gap-4">
              <NeumorphicButton
                variant="ghost"
                onClick={() => setShowFilters(!showFilters)}
                className="flex items-center gap-2"
              >
                <Filter className="w-4 h-4" />
                Filters
              </NeumorphicButton>
              
              <select
                value={sortBy}
                onChange={(e) => handleSortChange(e.target.value as typeof sortBy)}
                className="px-4 py-2 border border-neutral-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value="relevance">Most Relevant</option>
                <option value="price">Price: Low to High</option>
                <option value="rating">Highest Rated</option>
                <option value="duration">Duration</option>
              </select>
            </div>
          </div>
        </div>
      </section>

      {/* Results */}
      <section className="py-8">
        <div className="container mx-auto px-4">
          {/* Results Header */}
          <div className="flex items-center justify-between mb-8">
            <div>
              <h1 className="text-2xl font-bold text-neutral-800 mb-2">
                Search Results for "{query}"
              </h1>
              {!isLoading && (
                <p className="text-neutral-600">
                  {totalResults} {totalResults === 1 ? 'result' : 'results'} found
                </p>
              )}
            </div>
          </div>

          {/* Loading State */}
          {isLoading && (
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
              {[...Array(6)].map((_, index) => (
                <NeumorphicCard key={index} className="p-6">
                  <div className="animate-pulse">
                    <div className="aspect-video bg-neutral-200 rounded-lg mb-4" />
                    <div className="h-4 bg-neutral-200 rounded mb-2" />
                    <div className="h-4 bg-neutral-200 rounded w-3/4 mb-4" />
                    <div className="flex justify-between">
                      <div className="h-4 bg-neutral-200 rounded w-1/4" />
                      <div className="h-4 bg-neutral-200 rounded w-1/4" />
                    </div>
                  </div>
                </NeumorphicCard>
              ))}
            </div>
          )}

          {/* Results Grid */}
          {!isLoading && results.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
              className="grid md:grid-cols-2 lg:grid-cols-3 gap-6"
            >
              {results.map((result, index) => (
                <motion.div
                  key={result.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.6, delay: index * 0.1 }}
                >
                  <AdventureCard
                    adventure={{
                      id: result.id,
                      title: result.title,
                      description: result.description,
                      image: result.image,
                      location: result.location,
                      duration: result.duration,
                      price: result.price,
                      rating: result.rating,
                      reviewCount: result.reviewCount,
                      difficulty: result.difficulty as 'Easy' | 'Medium' | 'Hard',
                      maxParticipants: result.maxParticipants,
                      tags: result.tags,
                      highlights: [],
                      isPopular: false,
                      originalPrice: undefined,
                      discount: undefined,
                      nextAvailableDate: undefined,
                      instantBooking: false,
                      safetyVerified: true
                    }}
                  />
                </motion.div>
              ))}
            </motion.div>
          )}

          {/* No Results */}
          {!isLoading && results.length === 0 && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
              className="text-center py-16"
            >
              <NeumorphicCard className="max-w-md mx-auto p-8">
                <Search className="w-16 h-16 text-neutral-400 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-neutral-800 mb-2">
                  No results found
                </h3>
                <p className="text-neutral-600 mb-6">
                  We couldn't find any adventures matching "{query}". Try adjusting your search terms.
                </p>
                <div className="space-y-2">
                  <p className="text-sm text-neutral-500 font-medium">Suggestions:</p>
                  <ul className="text-sm text-neutral-600 space-y-1">
                    <li>• Try different keywords</li>
                    <li>• Check your spelling</li>
                    <li>• Use more general terms</li>
                    <li>• Browse our popular adventures</li>
                  </ul>
                </div>
                <Link href="/adventures" className="mt-6 inline-block">
                  <NeumorphicButton variant="primary">
                    Browse All Adventures
                  </NeumorphicButton>
                </Link>
              </NeumorphicCard>
            </motion.div>
          )}
        </div>
      </section>
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-gradient-to-br from-neumorphic-light-bg via-neumorphic-light-bg to-primary-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    }>
      <SearchPageContent />
    </Suspense>
  );
}
