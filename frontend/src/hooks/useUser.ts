/**
 * User Management React Hooks
 * Provides user-related state and methods for React components
 */

import { useState, useEffect, useCallback } from 'react';
import { 
  userService, 
  User, 
  CreateUserRequest, 
  UpdateUserRequest,
  UserAddress,
  CreateAddressRequest,
  UserConsent,
  ConsentRequest
} from '../lib/services/user-service';

/**
 * Hook for user profile management
 */
export const useUserProfile = (userReferenceId?: string) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadUser = useCallback(async (refId?: string) => {
    const id = refId || userReferenceId;
    if (!id) return;

    try {
      setIsLoading(true);
      setError(null);
      
      const response = id === 'current' 
        ? await userService.getCurrentUserProfile()
        : await userService.getUserByReferenceId(id);
      
      if (response.status >= 200 && response.status < 300 && response.data) {
        setUser(response.data);
      } else {
        setError(response.statusText || 'Failed to load user');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load user');
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  const updateUser = useCallback(async (updates: UpdateUserRequest, refId?: string) => {
    const id = refId || userReferenceId;
    if (!id) return { success: false, error: 'No user reference ID provided' };

    try {
      setIsLoading(true);
      setError(null);
      
      const response = id === 'current'
        ? await userService.updateCurrentUserProfile(updates)
        : await userService.updateUser(id, updates);
      
      if (response.status >= 200 && response.status < 300 && response.data) {
        setUser(response.data);
        return { success: true };
      } else {
        const errorMsg = response.statusText || 'Failed to update user';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to update user';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  const deleteUser = useCallback(async (refId?: string) => {
    const id = refId || userReferenceId;
    if (!id) return { success: false, error: 'No user reference ID provided' };

    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.deleteUser(id);
      
      if (response.status >= 200 && response.status < 300) {
        setUser(null);
        return { success: true };
      } else {
        const errorMsg = response.statusText || 'Failed to delete user';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to delete user';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  useEffect(() => {
    if (userReferenceId) {
      loadUser();
    }
  }, [loadUser, userReferenceId]);

  return {
    user,
    isLoading,
    error,
    loadUser,
    updateUser,
    deleteUser,
    refreshUser: () => loadUser()
  };
};

/**
 * Hook for user address management
 */
export const useUserAddresses = (userReferenceId: string) => {
  const [addresses, setAddresses] = useState<UserAddress[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadAddresses = useCallback(async () => {
    if (!userReferenceId) return;

    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.getUserAddresses(userReferenceId);
      
      if (response.status >= 200 && response.status < 300 && response.data) {
        setAddresses(response.data);
      } else {
        setError(response.statusText || 'Failed to load addresses');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load addresses');
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  const addAddress = useCallback(async (address: CreateAddressRequest) => {
    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.addUserAddress(userReferenceId, address);
      
      if (response.status >= 200 && response.status < 300 && response.data) {
        setAddresses(prev => [...prev, response.data!]);
        return { success: true, data: response.data };
      } else {
        const errorMsg = response.statusText || 'Failed to add address';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to add address';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  const updateAddress = useCallback(async (addressId: string, updates: Partial<CreateAddressRequest>) => {
    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.updateUserAddress(userReferenceId, addressId, updates);
      
      if (response.status >= 200 && response.status < 300 && response.data) {
        setAddresses(prev => prev.map(addr =>
          addr.id === addressId ? response.data! : addr
        ));
        return { success: true, data: response.data };
      } else {
        const errorMsg = response.statusText || 'Failed to update address';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to update address';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  const deleteAddress = useCallback(async (addressId: string) => {
    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.deleteUserAddress(userReferenceId, addressId);
      
      if (response.status >= 200 && response.status < 300) {
        setAddresses(prev => prev.filter(addr => addr.id !== addressId));
        return { success: true };
      } else {
        const errorMsg = response.statusText || 'Failed to delete address';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to delete address';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  const setPrimaryAddress = useCallback(async (addressId: string) => {
    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.setPrimaryAddress(userReferenceId, addressId);
      
      if (response.status >= 200 && response.status < 300) {
        setAddresses(prev => prev.map(addr => ({
          ...addr,
          isPrimary: addr.id === addressId
        })));
        return { success: true };
      } else {
        const errorMsg = response.statusText || 'Failed to set primary address';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to set primary address';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  useEffect(() => {
    if (userReferenceId) {
      loadAddresses();
    }
  }, [loadAddresses, userReferenceId]);

  return {
    addresses,
    isLoading,
    error,
    loadAddresses,
    addAddress,
    updateAddress,
    deleteAddress,
    setPrimaryAddress,
    primaryAddress: addresses.find(addr => addr.isPrimary)
  };
};

/**
 * Hook for user consent management
 */
export const useUserConsents = (userReferenceId: string) => {
  const [consents, setConsents] = useState<UserConsent[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadConsents = useCallback(async () => {
    if (!userReferenceId) return;

    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.getUserConsents(userReferenceId);
      
      if (response.status >= 200 && response.status < 300 && response.data) {
        setConsents(response.data);
      } else {
        setError(response.statusText || 'Failed to load consents');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load consents');
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  const updateConsent = useCallback(async (consent: ConsentRequest) => {
    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.updateConsent(userReferenceId, consent);
      
      if (response.status >= 200 && response.status < 300 && response.data) {
        setConsents(prev => {
          const existing = prev.find(c => c.consentKey === consent.consentKey);
          if (existing) {
            return prev.map(c => c.consentKey === consent.consentKey ? response.data! : c);
          } else {
            return [...prev, response.data!];
          }
        });
        return { success: true, data: response.data };
      } else {
        const errorMsg = response.statusText || 'Failed to update consent';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to update consent';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  const withdrawConsent = useCallback(async (consentKey: string) => {
    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.withdrawConsent(userReferenceId, consentKey);
      
      if (response.status >= 200 && response.status < 300) {
        setConsents(prev => prev.map(c =>
          c.consentKey === consentKey
            ? { ...c, granted: false, withdrawnAt: new Date().toISOString() }
            : c
        ));
        return { success: true };
      } else {
        const errorMsg = response.statusText || 'Failed to withdraw consent';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to withdraw consent';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setIsLoading(false);
    }
  }, [userReferenceId]);

  const getConsentStatus = useCallback((consentKey: string) => {
    const consent = consents.find(c => c.consentKey === consentKey);
    return consent?.granted || false;
  }, [consents]);

  useEffect(() => {
    if (userReferenceId) {
      loadConsents();
    }
  }, [loadConsents, userReferenceId]);

  return {
    consents,
    isLoading,
    error,
    loadConsents,
    updateConsent,
    withdrawConsent,
    getConsentStatus
  };
};

/**
 * Hook for user lookup functionality
 */
export const useUserLookup = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const lookupByEmail = useCallback(async (email: string) => {
    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.lookupUserByEmail(email);
      
      if (response.status >= 200 && response.status < 300 && response.data) {
        return { success: true, data: response.data };
      } else {
        const errorMsg = response.statusText || 'User not found';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Lookup failed';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setIsLoading(false);
    }
  }, []);

  const lookupByPhone = useCallback(async (phone: string) => {
    try {
      setIsLoading(true);
      setError(null);
      
      const response = await userService.lookupUserByPhone(phone);
      
      if (response.status >= 200 && response.status < 300 && response.data) {
        return { success: true, data: response.data };
      } else {
        const errorMsg = response.statusText || 'User not found';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Lookup failed';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {
    isLoading,
    error,
    lookupByEmail,
    lookupByPhone
  };
};
