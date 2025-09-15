import { useState, useEffect } from 'react';

interface GoogleMapsConfig {
  apiKey: string;
  libraries?: string[];
  version?: string;
  language?: string;
  region?: string;
}

interface UseGoogleMapsReturn {
  isLoaded: boolean;
  loadError: Error | null;
  google: any;
}

const defaultConfig: GoogleMapsConfig = {
  apiKey: process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY || '',
  libraries: ['places', 'geometry'],
  version: 'weekly',
  language: 'en',
  region: 'IN',
};

let isLoadingPromise: Promise<void> | null = null;
let isLoaded = false;
let loadError: Error | null = null;

export function useGoogleMaps(config: Partial<GoogleMapsConfig> = {}): UseGoogleMapsReturn {
  const [state, setState] = useState({
    isLoaded,
    loadError,
    google: typeof window !== 'undefined' && window.google ? window.google : null,
  });

  const finalConfig = { ...defaultConfig, ...config };

  useEffect(() => {
    if (!finalConfig.apiKey) {
      const error = new Error('Google Maps API key is required');
      setState(prev => ({ ...prev, loadError: error }));
      return;
    }

    if (typeof window === 'undefined') {
      return;
    }

    // If already loaded, update state
    if (window.google && window.google.maps) {
      setState({
        isLoaded: true,
        loadError: null,
        google: window.google,
      });
      return;
    }

    // If already loading, wait for it
    if (isLoadingPromise) {
      isLoadingPromise
        .then(() => {
          setState({
            isLoaded: true,
            loadError: null,
            google: window.google,
          });
        })
        .catch((error) => {
          setState(prev => ({ ...prev, loadError: error }));
        });
      return;
    }

    // Start loading
    isLoadingPromise = loadGoogleMapsAPI(finalConfig);
    
    isLoadingPromise
      .then(() => {
        isLoaded = true;
        setState({
          isLoaded: true,
          loadError: null,
          google: window.google,
        });
      })
      .catch((error) => {
        loadError = error;
        setState(prev => ({ ...prev, loadError: error }));
      });
  }, [finalConfig.apiKey]);

  return state;
}

function loadGoogleMapsAPI(config: GoogleMapsConfig): Promise<void> {
  return new Promise((resolve, reject) => {
    // Check if script already exists
    const existingScript = document.querySelector(
      `script[src*="maps.googleapis.com/maps/api/js"]`
    );
    
    if (existingScript) {
      // Wait for existing script to load
      if (window.google && window.google.maps) {
        resolve();
      } else {
        existingScript.addEventListener('load', () => resolve());
        existingScript.addEventListener('error', () => 
          reject(new Error('Failed to load Google Maps API'))
        );
      }
      return;
    }

    // Create callback function
    const callbackName = `googleMapsCallback_${Date.now()}`;
    (window as any)[callbackName] = () => {
      delete (window as any)[callbackName];
      resolve();
    };

    // Build URL
    const params = new URLSearchParams({
      key: config.apiKey,
      callback: callbackName,
      v: config.version || 'weekly',
      libraries: config.libraries?.join(',') || '',
      language: config.language || 'en',
      region: config.region || 'IN',
    });

    // Create and append script
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?${params.toString()}`;
    script.async = true;
    script.defer = true;
    
    script.onerror = () => {
      delete (window as any)[callbackName];
      reject(new Error('Failed to load Google Maps API'));
    };

    document.head.appendChild(script);
  });
}

// Utility hooks for specific Google Maps features

export function useGeocoding() {
  const { isLoaded, google } = useGoogleMaps();

  const geocode = async (address: string): Promise<any[]> => {
    if (!isLoaded || !google) {
      throw new Error('Google Maps not loaded');
    }

    const geocoder = new (window as any).google.maps.Geocoder();

    return new Promise((resolve, reject) => {
      geocoder.geocode({ address }, (results: any, status: any) => {
        if (status === (window as any).google.maps.GeocoderStatus.OK && results) {
          resolve(results);
        } else {
          reject(new Error(`Geocoding failed: ${status}`));
        }
      });
    });
  };

  const reverseGeocode = async (
    location: any
  ): Promise<any[]> => {
    if (!isLoaded || !google) {
      throw new Error('Google Maps not loaded');
    }

    const geocoder = new (window as any).google.maps.Geocoder();

    return new Promise((resolve, reject) => {
      geocoder.geocode({ location }, (results: any, status: any) => {
        if (status === (window as any).google.maps.GeocoderStatus.OK && results) {
          resolve(results);
        } else {
          reject(new Error(`Reverse geocoding failed: ${status}`));
        }
      });
    });
  };

  return {
    isLoaded,
    geocode,
    reverseGeocode,
  };
}

export function usePlacesAutocomplete() {
  const { isLoaded, google } = useGoogleMaps({ libraries: ['places'] });

  const getPlacePredictions = async (
    input: string,
    options?: any
  ): Promise<any[]> => {
    if (!isLoaded || !google) {
      throw new Error('Google Maps not loaded');
    }

    const service = new (window as any).google.maps.places.AutocompleteService();
    
    return new Promise((resolve, reject) => {
      service.getPlacePredictions(
        {
          input,
          componentRestrictions: { country: 'in' },
          types: ['establishment', 'geocode'],
          ...options,
        },
        (predictions: any, status: any) => {
          if (status === (window as any).google.maps.places.PlacesServiceStatus.OK && predictions) {
            resolve(predictions);
          } else if (status === (window as any).google.maps.places.PlacesServiceStatus.ZERO_RESULTS) {
            resolve([]);
          } else {
            reject(new Error(`Places autocomplete failed: ${status}`));
          }
        }
      );
    });
  };

  const getPlaceDetails = async (
    placeId: string,
    fields?: string[]
  ): Promise<any> => {
    if (!isLoaded || !google) {
      throw new Error('Google Maps not loaded');
    }

    // Create a temporary map element for the service
    const mapDiv = document.createElement('div');
    const map = new (window as any).google.maps.Map(mapDiv);
    const service = new (window as any).google.maps.places.PlacesService(map);
    
    return new Promise((resolve, reject) => {
      service.getDetails(
        {
          placeId,
          fields: fields || ['name', 'geometry', 'formatted_address', 'place_id'],
        },
        (place: any, status: any) => {
          if (status === (window as any).google.maps.places.PlacesServiceStatus.OK && place) {
            resolve(place);
          } else {
            reject(new Error(`Place details failed: ${status}`));
          }
        }
      );
    });
  };

  return {
    isLoaded,
    getPlacePredictions,
    getPlaceDetails,
  };
}

export function useDistanceMatrix() {
  const { isLoaded, google } = useGoogleMaps();

  const calculateDistance = async (
    origins: any[],
    destinations: any[],
    options?: any
  ): Promise<any> => {
    if (!isLoaded || !google) {
      throw new Error('Google Maps not loaded');
    }

    const service = new (window as any).google.maps.DistanceMatrixService();
    
    return new Promise((resolve, reject) => {
      service.getDistanceMatrix(
        {
          origins,
          destinations,
          travelMode: (window as any).google.maps.TravelMode.DRIVING,
          unitSystem: (window as any).google.maps.UnitSystem.METRIC,
          avoidHighways: false,
          avoidTolls: false,
          ...options,
        },
        (response: any, status: any) => {
          if (status === (window as any).google.maps.DistanceMatrixStatus.OK && response) {
            resolve(response);
          } else {
            reject(new Error(`Distance matrix failed: ${status}`));
          }
        }
      );
    });
  };

  return {
    isLoaded,
    calculateDistance,
  };
}

export function useDirections() {
  const { isLoaded, google } = useGoogleMaps();

  const getDirections = async (
    origin: any,
    destination: any,
    options?: any
  ): Promise<any> => {
    if (!isLoaded || !google) {
      throw new Error('Google Maps not loaded');
    }

    const service = new (window as any).google.maps.DirectionsService();
    
    return new Promise((resolve, reject) => {
      service.route(
        {
          origin,
          destination,
          travelMode: (window as any).google.maps.TravelMode.DRIVING,
          ...options,
        },
        (result: any, status: any) => {
          if (status === (window as any).google.maps.DirectionsStatus.OK && result) {
            resolve(result);
          } else {
            reject(new Error(`Directions failed: ${status}`));
          }
        }
      );
    });
  };

  return {
    isLoaded,
    getDirections,
  };
}
