'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { motion } from 'framer-motion';
import {
  MapPin,
  Navigation,
  Layers,
  Search,
  Filter,
  Star,
  Clock,
  Users,
  Maximize2,
  Minimize2
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { formatCurrency } from '@/lib/utils';
import { useGoogleMaps } from '@/hooks/use-google-maps';

interface Adventure {
  id: string;
  title: string;
  location: string;
  coordinates: {
    lat: number;
    lng: number;
  };
  price: number;
  rating: number;
  duration: string;
  maxParticipants: number;
  image: string;
  difficulty: 'Easy' | 'Medium' | 'Hard';
  category: string;
  nextAvailableDate?: string;
}

interface AdventureMapProps {
  adventures: Adventure[];
  selectedAdventure?: string;
  onAdventureSelect?: (adventure: Adventure) => void;
  onLocationChange?: (bounds: any) => void;
  center?: { lat: number; lng: number };
  zoom?: number;
  height?: string;
  showSearch?: boolean;
  showFilters?: boolean;
  clustered?: boolean;
}

export function AdventureMap({
  adventures,
  selectedAdventure,
  onAdventureSelect,
  onLocationChange,
  center = { lat: 19.0760, lng: 72.8777 }, // Mumbai
  zoom = 10,
  height = '500px',
  showSearch = true,
  showFilters = true,
  clustered = true,
}: AdventureMapProps) {
  const mapRef = useRef<HTMLDivElement>(null);
  const [map, setMap] = useState<any>(null);
  const [markers, setMarkers] = useState<any[]>([]);
  const [infoWindow, setInfoWindow] = useState<any>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [mapType, setMapType] = useState<'roadmap' | 'satellite' | 'hybrid' | 'terrain'>('roadmap');
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);

  const { isLoaded, loadError } = useGoogleMaps();

  // Initialize map
  useEffect(() => {
    if (!isLoaded || !mapRef.current) return;

    const mapInstance = new (window as any).google.maps.Map(mapRef.current, {
      center,
      zoom,
      mapTypeId: mapType,
      styles: [
        {
          featureType: 'poi',
          elementType: 'labels',
          stylers: [{ visibility: 'off' }],
        },
      ],
      mapTypeControl: false,
      streetViewControl: false,
      fullscreenControl: false,
    });

    setMap(mapInstance);

    // Create info window
    const infoWindowInstance = new (window as any).google.maps.InfoWindow();
    setInfoWindow(infoWindowInstance);

    // Listen for bounds changes
    mapInstance.addListener('bounds_changed', () => {
      const bounds = mapInstance.getBounds();
      if (bounds) {
        onLocationChange?.(bounds);
      }
    });

    return () => {
      markers.forEach(marker => marker.setMap(null));
      setMarkers([]);
    };
  }, [isLoaded, mapType]);

  // Update markers when adventures change
  useEffect(() => {
    if (!map || !adventures.length) return;

    // Clear existing markers
    markers.forEach(marker => marker.setMap(null));

    const newMarkers: any[] = [];

    adventures.forEach((adventure) => {
      const marker = new (window as any).google.maps.Marker({
        position: adventure.coordinates,
        map,
        title: adventure.title,
        icon: {
          url: getMarkerIcon(adventure),
          scaledSize: new (window as any).google.maps.Size(40, 40),
          anchor: new (window as any).google.maps.Point(20, 40),
        },
        animation: (window as any).google.maps.Animation.DROP,
      });

      // Add click listener
      marker.addListener('click', () => {
        if (infoWindow) {
          infoWindow.setContent(createInfoWindowContent(adventure));
          infoWindow.open(map, marker);
        }
        onAdventureSelect?.(adventure);
      });

      // Highlight selected adventure
      if (selectedAdventure === adventure.id) {
        marker.setAnimation((window as any).google.maps.Animation.BOUNCE);
        setTimeout(() => marker.setAnimation(null), 2000);
      }

      newMarkers.push(marker);
    });

    setMarkers(newMarkers);

    // Fit bounds to show all markers
    if (newMarkers.length > 0) {
      const bounds = new (window as any).google.maps.LatLngBounds();
      newMarkers.forEach(marker => {
        const position = marker.getPosition();
        if (position) bounds.extend(position);
      });
      map.fitBounds(bounds);
    }
  }, [map, adventures, selectedAdventure, infoWindow]);

  // Get user location
  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const location = {
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          };
          setUserLocation(location);
        },
        (error) => {
          console.warn('Geolocation error:', error);
        }
      );
    }
  }, []);

  const getMarkerIcon = (adventure: Adventure) => {
    const difficulty = adventure.difficulty.toLowerCase();
    const baseUrl = '/images/markers';
    
    switch (difficulty) {
      case 'easy':
        return `${baseUrl}/marker-green.png`;
      case 'medium':
        return `${baseUrl}/marker-yellow.png`;
      case 'hard':
        return `${baseUrl}/marker-red.png`;
      default:
        return `${baseUrl}/marker-blue.png`;
    }
  };

  const createInfoWindowContent = (adventure: Adventure) => {
    return `
      <div class="p-4 max-w-xs">
        <img src="${adventure.image}" alt="${adventure.title}" class="w-full h-32 object-cover rounded-lg mb-3" />
        <h3 class="font-semibold text-lg mb-2">${adventure.title}</h3>
        <div class="space-y-2 text-sm text-gray-600">
          <div class="flex items-center gap-2">
            <span class="text-yellow-500">★</span>
            <span>${adventure.rating}</span>
            <span>•</span>
            <span>${adventure.difficulty}</span>
          </div>
          <div class="flex items-center gap-2">
            <span>⏱️</span>
            <span>${adventure.duration}</span>
            <span>•</span>
            <span>👥 Max ${adventure.maxParticipants}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="font-semibold text-lg">${formatCurrency(adventure.price)}</span>
            <button 
              onclick="window.location.href='/adventures/${adventure.id}'"
              class="bg-blue-500 text-white px-3 py-1 rounded text-sm hover:bg-blue-600"
            >
              View Details
            </button>
          </div>
        </div>
      </div>
    `;
  };

  const handleSearch = useCallback((query: string) => {
    if (!map || !query.trim()) return;

    const service = new (window as any).google.maps.places.PlacesService(map);
    const request = {
      query,
      fields: ['name', 'geometry'],
    };

    service.textSearch(request, (results: any, status: any) => {
      if (status === (window as any).google.maps.places.PlacesServiceStatus.OK && results?.[0]) {
        const place = results[0];
        if (place.geometry?.location) {
          map.setCenter(place.geometry.location);
          map.setZoom(12);
        }
      }
    });
  }, [map]);

  const goToUserLocation = () => {
    if (userLocation && map) {
      map.setCenter(userLocation);
      map.setZoom(15);
    }
  };

  const toggleFullscreen = () => {
    setIsFullscreen(!isFullscreen);
  };

  if (loadError) {
    return (
      <NeumorphicCard className="text-center py-12">
        <MapPin className="w-12 h-12 text-neutral-400 mx-auto mb-4" />
        <h3 className="text-lg font-semibold text-neutral-800 mb-2">
          Map Loading Error
        </h3>
        <p className="text-neutral-600">
          Unable to load the map. Please check your internet connection and try again.
        </p>
      </NeumorphicCard>
    );
  }

  if (!isLoaded) {
    return (
      <NeumorphicCard className="animate-pulse" style={{ height }}>
        <div className="w-full h-full bg-neutral-200 rounded-neumorphic flex items-center justify-center">
          <div className="text-center">
            <MapPin className="w-8 h-8 text-neutral-400 mx-auto mb-2 animate-bounce" />
            <p className="text-neutral-600">Loading map...</p>
          </div>
        </div>
      </NeumorphicCard>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      className={`relative ${isFullscreen ? 'fixed inset-0 z-50 bg-white' : ''}`}
    >
      <NeumorphicCard className={`overflow-hidden ${isFullscreen ? 'h-full rounded-none' : ''}`}>
        {/* Map Controls */}
        <div className="absolute top-4 left-4 right-4 z-10 flex items-center justify-between gap-4">
          {/* Search */}
          {showSearch && (
            <div className="flex-1 max-w-md">
              <div className="relative">
                <input
                  type="text"
                  placeholder="Search locations..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyPress={(e) => {
                    if (e.key === 'Enter') {
                      handleSearch(searchQuery);
                    }
                  }}
                  className="w-full pl-10 pr-4 py-2 bg-white/90 backdrop-blur-sm border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                />
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-neutral-400" />
              </div>
            </div>
          )}

          {/* Map Controls */}
          <div className="flex items-center gap-2">
            {/* Map Type */}
            <select
              value={mapType}
              onChange={(e) => setMapType(e.target.value as any)}
              className="px-3 py-2 bg-white/90 backdrop-blur-sm border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            >
              <option value="roadmap">Map</option>
              <option value="satellite">Satellite</option>
              <option value="hybrid">Hybrid</option>
              <option value="terrain">Terrain</option>
            </select>

            {/* User Location */}
            {userLocation && (
              <NeumorphicButton
                variant="ghost"
                size="sm"
                onClick={goToUserLocation}
                className="bg-white/90 backdrop-blur-sm"
                title="Go to my location"
              >
                <Navigation className="w-4 h-4" />
              </NeumorphicButton>
            )}

            {/* Fullscreen Toggle */}
            <NeumorphicButton
              variant="ghost"
              size="sm"
              onClick={toggleFullscreen}
              className="bg-white/90 backdrop-blur-sm"
              title={isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}
            >
              {isFullscreen ? (
                <Minimize2 className="w-4 h-4" />
              ) : (
                <Maximize2 className="w-4 h-4" />
              )}
            </NeumorphicButton>
          </div>
        </div>

        {/* Map Legend */}
        <div className="absolute bottom-4 left-4 z-10">
          <NeumorphicCard className="bg-white/90 backdrop-blur-sm">
            <h4 className="font-medium text-neutral-800 mb-2 text-sm">Difficulty</h4>
            <div className="space-y-1 text-xs">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 bg-success-500 rounded-full"></div>
                <span>Easy</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 bg-warning-500 rounded-full"></div>
                <span>Medium</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 bg-error-500 rounded-full"></div>
                <span>Hard</span>
              </div>
            </div>
          </NeumorphicCard>
        </div>

        {/* Adventure Count */}
        <div className="absolute bottom-4 right-4 z-10">
          <NeumorphicCard className="bg-white/90 backdrop-blur-sm">
            <div className="text-center">
              <div className="text-lg font-bold text-primary-600">{adventures.length}</div>
              <div className="text-xs text-neutral-600">Adventures</div>
            </div>
          </NeumorphicCard>
        </div>

        {/* Map Container */}
        <div
          ref={mapRef}
          style={{ height: isFullscreen ? '100vh' : height }}
          className="w-full rounded-neumorphic"
        />
      </NeumorphicCard>
    </motion.div>
  );
}
