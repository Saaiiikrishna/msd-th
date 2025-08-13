import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';

// Adventure types
export interface Adventure {
  id: string;
  title: string;
  description: string;
  location: string;
  city: string;
  state: string;
  country: string;
  duration: string;
  difficulty: 'Easy' | 'Medium' | 'Hard';
  price: number;
  originalPrice?: number;
  currency: string;
  rating: number;
  reviewCount: number;
  maxParticipants: number;
  minParticipants: number;
  image: string;
  images: string[];
  tags: string[];
  categories: string[];
  highlights: string[];
  inclusions: string[];
  exclusions: string[];
  requirements: string[];
  safetyGuidelines: string[];
  isPopular: boolean;
  isFeatured: boolean;
  isActive: boolean;
  discount?: number;
  nextAvailableDate?: string;
  instantBooking: boolean;
  safetyVerified: boolean;
  coordinates: {
    latitude: number;
    longitude: number;
  };
  vendor: {
    id: string;
    name: string;
    rating: number;
    verified: boolean;
  };
  createdAt: string;
  updatedAt: string;
}

export interface AdventuresResponse {
  adventures: Adventure[];
  totalCount: number;
  currentPage: number;
  totalPages: number;
  hasNextPage: boolean;
  hasPreviousPage: boolean;
}

export interface AdventuresFilters {
  location?: string;
  difficulty?: string;
  category?: string;
  priceMin?: number;
  priceMax?: number;
  duration?: string;
  participants?: string;
  rating?: number;
  availability?: string;
  sort?: string;
  page?: number;
  limit?: number;
  search?: string;
}

/**
 * Hook to fetch adventures with filters
 */
export function useAdventures(filters: Record<string, string | undefined> = {}) {
  const queryKey = ['adventures', filters];
  
  return useQuery({
    queryKey,
    queryFn: async () => {
      // Convert filters to query parameters
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value && value !== '') {
          params.append(key, value);
        }
      });

      const response = await apiClient.get<AdventuresResponse>(`/api/adventures?${params.toString()}`);
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    select: (data) => ({
      ...data,
      // Add computed properties
      adventures: data.adventures.map(adventure => ({
        ...adventure,
        hasDiscount: adventure.originalPrice && adventure.originalPrice > adventure.price,
        discountPercentage: adventure.originalPrice 
          ? Math.round(((adventure.originalPrice - adventure.price) / adventure.originalPrice) * 100)
          : 0,
        isAvailableToday: adventure.nextAvailableDate 
          ? new Date(adventure.nextAvailableDate).toDateString() === new Date().toDateString()
          : false,
      }))
    }),
  });
}

/**
 * Hook to fetch a single adventure by ID
 */
export function useAdventure(id: string) {
  return useQuery({
    queryKey: ['adventure', id],
    queryFn: async () => {
      const response = await apiClient.get<Adventure>(`/api/adventures/${id}`);
      return response.data;
    },
    enabled: !!id,
    staleTime: 10 * 60 * 1000, // 10 minutes
    retry: 3,
  });
}

/**
 * Hook to fetch featured adventures
 */
export function useFeaturedAdventures(limit: number = 8) {
  return useQuery({
    queryKey: ['adventures', 'featured', limit],
    queryFn: async () => {
      const response = await apiClient.get<Adventure[]>(`/api/adventures/featured?limit=${limit}`);
      return response.data;
    },
    staleTime: 15 * 60 * 1000, // 15 minutes
  });
}

/**
 * Hook to fetch popular adventures
 */
export function usePopularAdventures(limit: number = 8) {
  return useQuery({
    queryKey: ['adventures', 'popular', limit],
    queryFn: async () => {
      const response = await apiClient.get<Adventure[]>(`/api/adventures/popular?limit=${limit}`);
      return response.data;
    },
    staleTime: 15 * 60 * 1000, // 15 minutes
  });
}

/**
 * Hook to fetch adventures by location
 */
export function useAdventuresByLocation(location: string, limit: number = 12) {
  return useQuery({
    queryKey: ['adventures', 'location', location, limit],
    queryFn: async () => {
      const response = await apiClient.get<Adventure[]>(`/api/adventures/location/${location}?limit=${limit}`);
      return response.data;
    },
    enabled: !!location,
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
}

/**
 * Hook to fetch similar adventures
 */
export function useSimilarAdventures(adventureId: string, limit: number = 4) {
  return useQuery({
    queryKey: ['adventures', 'similar', adventureId, limit],
    queryFn: async () => {
      const response = await apiClient.get<Adventure[]>(`/api/adventures/${adventureId}/similar?limit=${limit}`);
      return response.data;
    },
    enabled: !!adventureId,
    staleTime: 30 * 60 * 1000, // 30 minutes
  });
}

/**
 * Hook to search adventures
 */
export function useSearchAdventures(query: string, filters: AdventuresFilters = {}) {
  return useQuery({
    queryKey: ['adventures', 'search', query, filters],
    queryFn: async () => {
      const params = new URLSearchParams({ q: query });
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await apiClient.get<AdventuresResponse>(`/api/adventures/search?${params.toString()}`);
      return response.data;
    },
    enabled: !!query && query.length >= 2,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Hook to fetch adventure availability
 */
export function useAdventureAvailability(adventureId: string, date?: string) {
  return useQuery({
    queryKey: ['adventure', adventureId, 'availability', date],
    queryFn: async () => {
      const params = date ? `?date=${date}` : '';
      const response = await apiClient.get(`/api/adventures/${adventureId}/availability${params}`);
      return response.data;
    },
    enabled: !!adventureId,
    staleTime: 2 * 60 * 1000, // 2 minutes
    refetchInterval: 5 * 60 * 1000, // Refetch every 5 minutes
  });
}

/**
 * Hook to fetch adventure reviews
 */
export function useAdventureReviews(adventureId: string, page: number = 1, limit: number = 10) {
  return useQuery({
    queryKey: ['adventure', adventureId, 'reviews', page, limit],
    queryFn: async () => {
      const response = await apiClient.get(`/api/adventures/${adventureId}/reviews?page=${page}&limit=${limit}`);
      return response.data;
    },
    enabled: !!adventureId,
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
}

/**
 * Hook to fetch adventure locations for map view
 */
export function useAdventureLocations(filters: AdventuresFilters = {}) {
  return useQuery({
    queryKey: ['adventures', 'locations', filters],
    queryFn: async () => {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await apiClient.get(`/api/adventures/locations?${params.toString()}`);
      return response.data;
    },
    staleTime: 15 * 60 * 1000, // 15 minutes
  });
}

/**
 * Hook to fetch adventure categories
 */
export function useAdventureCategories() {
  return useQuery({
    queryKey: ['adventures', 'categories'],
    queryFn: async () => {
      const response = await apiClient.get('/api/adventures/categories');
      return response.data;
    },
    staleTime: 60 * 60 * 1000, // 1 hour
  });
}

/**
 * Hook to fetch adventure statistics
 */
export function useAdventureStats() {
  return useQuery({
    queryKey: ['adventures', 'stats'],
    queryFn: async () => {
      const response = await apiClient.get('/api/adventures/stats');
      return response.data;
    },
    staleTime: 30 * 60 * 1000, // 30 minutes
  });
}
