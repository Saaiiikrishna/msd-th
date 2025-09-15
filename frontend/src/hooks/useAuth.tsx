/**
 * Authentication React Hook
 * Provides authentication state and methods for React components
 */

import { useState, useEffect, useCallback, createContext, useContext } from 'react';
import { authService, LoginRequest, RegisterRequest, LoginResponse } from '../lib/services/auth-service';
import { handleApiError, getDetailedErrorInfo } from '../lib/api';

// Auth context type
interface AuthContextType {
  user: LoginResponse['user'] | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest, cookieConsent?: boolean) => Promise<{ success: boolean; error?: string; cookiesSet?: boolean; clientInfo?: any }>;
  register: (userData: RegisterRequest) => Promise<{ success: boolean; error?: string }>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

// Create auth context
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Auth hook
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

// Auth provider component
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<LoginResponse['user'] | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Initialize auth state
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        if (authService.isAuthenticated()) {
          const response = await authService.getCurrentUser();
          if (response.status >= 200 && response.status < 300 && response.data) {
            setUser(response.data);
          }
        }
      } catch (error) {
        console.error('Failed to initialize auth:', error);
      } finally {
        setIsLoading(false);
      }
    };

    initializeAuth();
  }, []);

  // Login function
  const login = useCallback(async (credentials: LoginRequest, cookieConsent: boolean = false): Promise<{ success: boolean; error?: string; cookiesSet?: boolean; clientInfo?: any }> => {
    setIsLoading(true);
    try {
      const response = await authService.login(credentials, cookieConsent);
      if (response.status >= 200 && response.status < 300 && response.data) {
        setUser(response.data.user);
        return {
          success: true,
          cookiesSet: response.data.cookiesSet,
          clientInfo: response.data.clientInfo
        };
      } else {
        return { success: false, error: response.statusText || 'Login failed' };
      }
    } catch (error) {
      const errorInfo = getDetailedErrorInfo(error);
      console.error('Login error:', errorInfo);
      return {
        success: false,
        error: errorInfo.message || 'An unexpected error occurred'
      };
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Register function
  const register = useCallback(async (userData: RegisterRequest): Promise<{ success: boolean; error?: string }> => {
    setIsLoading(true);
    try {
      const response = await authService.register(userData);
      if (response.status >= 200 && response.status < 300) {
        return { success: true };
      } else {
        return { success: false, error: response.statusText || 'Registration failed' };
      }
    } catch (error) {
      const errorInfo = getDetailedErrorInfo(error);
      console.error('Registration error:', errorInfo);
      return {
        success: false,
        error: errorInfo.message || 'An unexpected error occurred'
      };
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Logout function
  const logout = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    try {
      await authService.logout();
      setUser(null);
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Refresh user function
  const refreshUser = useCallback(async (): Promise<void> => {
    try {
      const response = await authService.getCurrentUser();
      if (response.status >= 200 && response.status < 300 && response.data) {
        setUser(response.data);
      }
    } catch (error) {
      console.error('Failed to refresh user:', error);
    }
  }, []);

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user && authService.isAuthenticated(),
    isLoading,
    login,
    register,
    logout,
    refreshUser
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

// Additional auth hooks

/**
 * Hook for checking user roles
 */
export const useUserRole = () => {
  const { user } = useAuth();
  
  const hasRole = useCallback((role: string): boolean => {
    return user?.roles?.includes(role) ?? false;
  }, [user]);

  const hasAnyRole = useCallback((roles: string[]): boolean => {
    return roles.some(role => hasRole(role));
  }, [hasRole]);

  const isAdmin = useCallback((): boolean => {
    return hasRole('ADMIN');
  }, [hasRole]);

  const isUser = useCallback((): boolean => {
    return hasRole('USER');
  }, [hasRole]);

  return {
    hasRole,
    hasAnyRole,
    isAdmin,
    isUser,
    roles: user?.roles ?? []
  };
};

/**
 * Hook for authentication status checks
 */
export const useAuthStatus = () => {
  const { isAuthenticated, isLoading, user } = useAuth();

  return {
    isAuthenticated,
    isLoading,
    isGuest: !isAuthenticated && !isLoading,
    userId: user?.id,
    userEmail: user?.email,
    userName: `${user?.firstName} ${user?.lastName}`.trim() || user?.email
  };
};

/**
 * Hook for protected routes
 */
export const useAuthGuard = (requiredRoles?: string[]) => {
  const { isAuthenticated, isLoading } = useAuth();
  const { hasAnyRole } = useUserRole();

  const canAccess = useCallback((): boolean => {
    if (!isAuthenticated) return false;
    if (!requiredRoles || requiredRoles.length === 0) return true;
    return hasAnyRole(requiredRoles);
  }, [isAuthenticated, requiredRoles, hasAnyRole]);

  return {
    canAccess: canAccess(),
    isLoading,
    isAuthenticated,
    needsAuth: !isAuthenticated && !isLoading,
    needsRole: isAuthenticated && requiredRoles && !hasAnyRole(requiredRoles)
  };
};
