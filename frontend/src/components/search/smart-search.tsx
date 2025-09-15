'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, MapPin, Calendar, Users, Clock, Star, ArrowRight, X } from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { useDebounce } from '@/hooks/use-debounce';

interface SearchSuggestion {
  id: string;
  type: 'city' | 'adventure' | 'category' | 'experience';
  title: string;
  subtitle?: string;
  icon: React.ComponentType<any>;
  metadata?: {
    rating?: number;
    price?: number;
    duration?: string;
    participants?: number;
  };
  score?: number;
}

// Component to highlight matching text
interface HighlightedTextProps {
  text: string;
  query: string;
  className?: string;
}

function HighlightedText({ text, query, className = '' }: HighlightedTextProps) {
  if (!query.trim()) {
    return <span className={className}>{text}</span>;
  }

  const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
  const parts = text.split(regex);

  return (
    <span className={className}>
      {parts.map((part, index) =>
        regex.test(part) ? (
          <mark key={index} className="bg-yellow-200 text-yellow-900 font-medium px-0.5 rounded">
            {part}
          </mark>
        ) : (
          <span key={index}>{part}</span>
        )
      )}
    </span>
  );
}

interface SmartSearchProps {
  placeholder?: string;
  variant?: 'hero' | 'header' | 'page';
  onSearch?: (query: string) => void;
  className?: string;
  autoFocus?: boolean;
}

export function SmartSearch({ 
  placeholder = "Search by city, activity, or experience...",
  variant = 'hero',
  onSearch,
  className = '',
  autoFocus = false
}: SmartSearchProps) {
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState<SearchSuggestion[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);

  const inputRef = useRef<HTMLInputElement>(null);
  const suggestionsRef = useRef<HTMLDivElement>(null);
  
  const debouncedQuery = useDebounce(query, 200);

  // Enhanced mock suggestions data with more comprehensive search terms
  const mockSuggestions: SearchSuggestion[] = [
    {
      id: '1',
      type: 'city',
      title: 'Mumbai',
      subtitle: '15 adventures available',
      icon: MapPin,
      metadata: { rating: 4.8 }
    },
    {
      id: '2',
      type: 'city',
      title: 'Mumbai Heritage District',
      subtitle: 'Historical area with 8 adventures',
      icon: MapPin,
      metadata: { rating: 4.9 }
    },
    {
      id: '3',
      type: 'adventure',
      title: 'Mumbai Street Food Hunt',
      subtitle: 'Culinary treasure hunt',
      icon: Clock,
      metadata: { rating: 4.7, price: 900, duration: '2.5 hours' }
    },
    {
      id: '4',
      type: 'adventure',
      title: 'Heritage Walk in Old Delhi',
      subtitle: 'Historical exploration',
      icon: Clock,
      metadata: { rating: 4.9, price: 1200, duration: '3 hours' }
    },
    {
      id: '5',
      type: 'category',
      title: 'Heritage Tours',
      subtitle: '12 historical adventures',
      icon: Users,
      metadata: { participants: 25 }
    },
    {
      id: '6',
      type: 'adventure',
      title: 'Delhi Heritage Trail',
      subtitle: 'Ancient monuments exploration',
      icon: Clock,
      metadata: { rating: 4.8, price: 1100, duration: '4 hours' }
    },
    {
      id: '7',
      type: 'category',
      title: 'Team Building',
      subtitle: '8 corporate adventures',
      icon: Users,
      metadata: { participants: 50 }
    },
    {
      id: '8',
      type: 'city',
      title: 'Bangalore',
      subtitle: '12 adventures available',
      icon: MapPin,
      metadata: { rating: 4.7 }
    },
    {
      id: '9',
      type: 'adventure',
      title: 'Tech Park Treasure Hunt',
      subtitle: 'Modern city exploration',
      icon: Clock,
      metadata: { rating: 4.6, price: 800, duration: '2 hours' }
    },
    {
      id: '10',
      type: 'city',
      title: 'Goa Beach',
      subtitle: '6 coastal adventures',
      icon: MapPin,
      metadata: { rating: 4.9 }
    },
    {
      id: '11',
      type: 'adventure',
      title: 'Food Trail Mumbai',
      subtitle: 'Street food discovery',
      icon: Clock,
      metadata: { rating: 4.5, price: 750, duration: '3 hours' }
    },
    {
      id: '12',
      type: 'adventure',
      title: 'Corporate Team Building Delhi',
      subtitle: 'Professional team activities',
      icon: Users,
      metadata: { rating: 4.6, price: 1500, duration: '5 hours' }
    }
  ];

  // Enhanced search algorithm with partial matching and scoring
  const searchSuggestions = useCallback((searchQuery: string, suggestions: SearchSuggestion[]) => {
    const query = searchQuery.toLowerCase().trim();

    // Score each suggestion based on relevance
    const scoredSuggestions = suggestions.map(suggestion => {
      const title = suggestion.title.toLowerCase();
      const subtitle = suggestion.subtitle?.toLowerCase() || '';

      let score = 0;
      let hasMatch = false;

      // Exact match gets highest score
      if (title === query) {
        score += 100;
        hasMatch = true;
      }
      if (subtitle === query) {
        score += 90;
        hasMatch = true;
      }

      // Starts with query gets high score
      if (title.startsWith(query)) {
        score += 80;
        hasMatch = true;
      }
      if (subtitle.startsWith(query)) {
        score += 70;
        hasMatch = true;
      }

      // Contains query gets medium score
      if (title.includes(query)) {
        score += 60;
        hasMatch = true;
      }
      if (subtitle.includes(query)) {
        score += 50;
        hasMatch = true;
      }

      // Word boundary matches get bonus points
      const titleWords = title.split(' ');
      const subtitleWords = subtitle.split(' ');

      titleWords.forEach(word => {
        if (word.startsWith(query)) {
          score += 40;
          hasMatch = true;
        }
        if (word.includes(query) && query.length >= 2) {
          score += 20;
          hasMatch = true;
        }
      });

      subtitleWords.forEach(word => {
        if (word.startsWith(query)) {
          score += 30;
          hasMatch = true;
        }
        if (word.includes(query) && query.length >= 2) {
          score += 15;
          hasMatch = true;
        }
      });

      // Only add type and rating bonuses if there's a text match
      if (hasMatch) {
        // Type-based scoring (cities and adventures get priority)
        if (suggestion.type === 'city') score += 10;
        if (suggestion.type === 'adventure') score += 8;

        // Rating-based bonus
        if (suggestion.metadata?.rating) {
          score += suggestion.metadata.rating * 2;
        }
      }

      return { ...suggestion, score, hasMatch };
    });

    // Filter out suggestions with no matches and sort by score
    return scoredSuggestions
      .filter(suggestion => suggestion.hasMatch && suggestion.score > 0)
      .sort((a, b) => b.score - a.score)
      .slice(0, 5); // Limit to top 5 results
  }, []);

  // Fetch suggestions based on query
  const fetchSuggestions = useCallback(async (searchQuery: string) => {
    if (!searchQuery.trim() || searchQuery.length < 1) {
      setSuggestions([]);
      return;
    }

    setIsLoading(true);

    try {
      // TODO: Replace with actual API call to treasure service
      // const response = await treasureService.searchSuggestions(searchQuery);

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 150));

      // Use enhanced search algorithm
      const filtered = searchSuggestions(searchQuery, mockSuggestions);

      setSuggestions(filtered);
    } catch (error) {
      console.error('Error fetching suggestions:', error);
      setSuggestions([]);
    } finally {
      setIsLoading(false);
    }
  }, [searchSuggestions]);

  // Debounced search effect
  useEffect(() => {
    if (debouncedQuery) {
      fetchSuggestions(debouncedQuery);
      setShowSuggestions(true);
    } else {
      setSuggestions([]);
      setShowSuggestions(false);
    }
  }, [debouncedQuery, fetchSuggestions]);

  // Handle input change
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);
    setSelectedIndex(-1);
  };

  // Handle form submission
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      performSearch(query.trim());
    }
  };

  // Perform search
  const performSearch = (searchQuery: string) => {
    setShowSuggestions(false);
    onSearch?.(searchQuery);
    router.push(`/search?q=${encodeURIComponent(searchQuery)}`);
  };

  // Handle suggestion click
  const handleSuggestionClick = (suggestion: SearchSuggestion) => {
    setQuery(suggestion.title);
    setShowSuggestions(false);
    
    if (suggestion.type === 'adventure') {
      router.push(`/adventures/${suggestion.id}`);
    } else {
      performSearch(suggestion.title);
    }
  };

  // Handle keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!showSuggestions || suggestions.length === 0) return;

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setSelectedIndex(prev => 
          prev < suggestions.length - 1 ? prev + 1 : prev
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setSelectedIndex(prev => prev > 0 ? prev - 1 : -1);
        break;
      case 'Enter':
        e.preventDefault();
        if (selectedIndex >= 0) {
          handleSuggestionClick(suggestions[selectedIndex]);
        } else {
          handleSubmit(e);
        }
        break;
      case 'Escape':
        setShowSuggestions(false);
        setSelectedIndex(-1);
        inputRef.current?.blur();
        break;
    }
  };

  // Close suggestions when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        suggestionsRef.current &&
        !suggestionsRef.current.contains(event.target as Node) &&
        !inputRef.current?.contains(event.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const getVariantStyles = () => {
    switch (variant) {
      case 'hero':
        return {
          container: 'max-w-2xl mx-auto',
          card: 'bg-white/10 backdrop-blur-md border border-white/20',
          input: 'text-white placeholder-white/70 text-lg',
          icon: 'text-white/70'
        };
      case 'header':
        return {
          container: 'w-full max-w-md',
          card: 'bg-white shadow-neumorphic',
          input: 'text-neutral-800 placeholder-neutral-400',
          icon: 'text-neutral-400'
        };
      case 'page':
        return {
          container: 'w-full max-w-4xl mx-auto',
          card: 'bg-white shadow-neumorphic',
          input: 'text-neutral-800 placeholder-neutral-400 text-lg',
          icon: 'text-neutral-400'
        };
      default:
        return {
          container: 'max-w-2xl mx-auto',
          card: 'bg-white shadow-neumorphic',
          input: 'text-neutral-800 placeholder-neutral-400',
          icon: 'text-neutral-400'
        };
    }
  };

  const styles = getVariantStyles();

  return (
    <div className={`relative ${styles.container} ${className}`}>
      <NeumorphicCard className={styles.card}>
        <form onSubmit={handleSubmit} className="flex items-center gap-4 p-4">
          <Search className={`w-5 h-5 ${styles.icon}`} />
          <input
            ref={inputRef}
            type="text"
            placeholder={placeholder}
            value={query}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            onFocus={() => query && setShowSuggestions(true)}
            className={`flex-1 bg-transparent border-none outline-none ${styles.input}`}
            autoFocus={autoFocus}
          />
          {query && (
            <button
              type="button"
              onClick={() => {
                setQuery('');
                setSuggestions([]);
                setShowSuggestions(false);
                inputRef.current?.focus();
              }}
              className={`p-1 rounded-full hover:bg-black/10 transition-colors ${styles.icon}`}
            >
              <X className="w-4 h-4" />
            </button>
          )}
          <NeumorphicButton
            type="submit"
            variant={variant === 'hero' ? 'secondary' : 'primary'}
            size="sm"
            disabled={!query.trim()}
          >
            <span className="hidden sm:inline">Find Adventures</span>
            <ArrowRight className="w-4 h-4 sm:ml-2" />
          </NeumorphicButton>
        </form>
      </NeumorphicCard>

      {/* Suggestions Dropdown */}
      <AnimatePresence>
        {showSuggestions && (suggestions.length > 0 || isLoading || (debouncedQuery && suggestions.length === 0)) && (
          <motion.div
            ref={suggestionsRef}
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            className="absolute top-full left-0 right-0 z-[99999] mt-2"
          >
            <NeumorphicCard className="bg-white shadow-neumorphic-elevated max-h-96 overflow-y-auto border border-neutral-200">
              {isLoading ? (
                <div className="p-4 text-center">
                  <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600 mx-auto mb-2" />
                  <span className="text-neutral-600 text-sm">Searching...</span>
                </div>
              ) : suggestions.length === 0 && debouncedQuery ? (
                <div className="p-4 text-center">
                  <Search className="w-8 h-8 text-neutral-400 mx-auto mb-2" />
                  <div className="text-neutral-800 font-medium mb-1">No results found</div>
                  <div className="text-sm text-neutral-600">
                    No suggestions found for "{debouncedQuery}"
                  </div>
                  <div className="mt-3 text-xs text-neutral-500">
                    Try different keywords or check spelling
                  </div>
                </div>
              ) : (
                <div className="py-2">
                  {suggestions.map((suggestion, index) => (
                    <button
                      key={suggestion.id}
                      onClick={() => handleSuggestionClick(suggestion)}
                      className={`w-full px-4 py-3 text-left hover:bg-neutral-50 transition-colors flex items-center gap-3 ${
                        index === selectedIndex ? 'bg-primary-50' : ''
                      }`}
                    >
                      <div className="flex items-center justify-center w-8 h-8 bg-primary-100 rounded-full">
                        <suggestion.icon className="w-4 h-4 text-primary-600" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-medium text-neutral-800 truncate">
                          <HighlightedText
                            text={suggestion.title}
                            query={query}
                          />
                        </div>
                        {suggestion.subtitle && (
                          <div className="text-sm text-neutral-600 truncate">
                            <HighlightedText
                              text={suggestion.subtitle}
                              query={query}
                            />
                          </div>
                        )}
                      </div>
                      {suggestion.metadata && (
                        <div className="flex items-center gap-2 text-sm text-neutral-500">
                          {suggestion.metadata.rating && (
                            <div className="flex items-center gap-1">
                              <Star className="w-3 h-3 fill-yellow-400 text-yellow-400" />
                              <span>{suggestion.metadata.rating}</span>
                            </div>
                          )}
                          {suggestion.metadata.price && (
                            <span>₹{suggestion.metadata.price}</span>
                          )}
                        </div>
                      )}
                    </button>
                  ))}
                </div>
              )}
            </NeumorphicCard>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
