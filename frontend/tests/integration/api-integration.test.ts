/**
 * Frontend API Integration Tests
 * Tests the integration between frontend services and backend APIs
 */

import { describe, test, expect, beforeAll, afterAll } from '@jest/globals';
import { userService } from '../../src/lib/services/user-service';
import { paymentService } from '../../src/lib/services/payment-service';
import { treasureService } from '../../src/lib/services/treasure-service';
import { authService } from '../../src/lib/services/auth-service';

// Test configuration
const TEST_CONFIG = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  testUser: {
    firstName: 'Test',
    lastName: 'User',
    email: 'test@example.com',
    password: 'TestPassword123!',
    phone: '+1234567890',
    gender: 'OTHER' as const,
    acceptTerms: true,
    acceptPrivacy: true
  }
};

describe('API Integration Tests', () => {
  let testUserReferenceId: string;
  let authToken: string;

  beforeAll(async () => {
    // Setup test environment
    console.log('Setting up integration tests...');
    
    // Wait for services to be available
    await waitForServices();
  });

  afterAll(async () => {
    // Cleanup test data
    if (testUserReferenceId) {
      try {
        await userService.deleteUser(testUserReferenceId);
      } catch (error) {
        console.warn('Failed to cleanup test user:', error);
      }
    }
  });

  describe('Authentication Service', () => {
    test('should register a new user', async () => {
      const response = await authService.register(TEST_CONFIG.testUser);
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.user.email).toBe(TEST_CONFIG.testUser.email);
      
      if (response.data) {
        testUserReferenceId = response.data.user.referenceId;
        authToken = response.data.accessToken;
      }
    });

    test('should login with valid credentials', async () => {
      const response = await authService.login({
        email: TEST_CONFIG.testUser.email,
        password: TEST_CONFIG.testUser.password
      });
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.accessToken).toBeDefined();
    });

    test('should get current user info', async () => {
      const response = await authService.getCurrentUser();
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.email).toBe(TEST_CONFIG.testUser.email);
    });

    test('should refresh access token', async () => {
      const response = await authService.refreshAccessToken();
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.accessToken).toBeDefined();
    });
  });

  describe('User Service', () => {
    test('should get user by reference ID', async () => {
      const response = await userService.getUserByReferenceId(testUserReferenceId);
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.referenceId).toBe(testUserReferenceId);
    });

    test('should update user profile', async () => {
      const updates = {
        firstName: 'Updated',
        lastName: 'Name'
      };
      
      const response = await userService.updateUser(testUserReferenceId, updates);
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.firstName).toBe(updates.firstName);
      expect(response.data?.lastName).toBe(updates.lastName);
    });

    test('should lookup user by email', async () => {
      const response = await userService.lookupUserByEmail(TEST_CONFIG.testUser.email);
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.email).toBe(TEST_CONFIG.testUser.email);
    });

    test('should manage user addresses', async () => {
      const address = {
        type: 'HOME' as const,
        addressLine1: '123 Test Street',
        city: 'Test City',
        state: 'Test State',
        postalCode: '12345',
        country: 'Test Country',
        isPrimary: true
      };

      // Add address
      const addResponse = await userService.addUserAddress(testUserReferenceId, address);
      expect(addResponse.success).toBe(true);
      expect(addResponse.data).toBeDefined();
      
      const addressId = addResponse.data!.id;

      // Get addresses
      const getResponse = await userService.getUserAddresses(testUserReferenceId);
      expect(getResponse.success).toBe(true);
      expect(getResponse.data).toBeDefined();
      expect(getResponse.data!.length).toBeGreaterThan(0);

      // Update address
      const updateResponse = await userService.updateUserAddress(
        testUserReferenceId, 
        addressId, 
        { addressLine1: '456 Updated Street' }
      );
      expect(updateResponse.success).toBe(true);

      // Delete address
      const deleteResponse = await userService.deleteUserAddress(testUserReferenceId, addressId);
      expect(deleteResponse.success).toBe(true);
    });

    test('should manage user consents', async () => {
      const consent = {
        consentKey: 'marketing',
        granted: true,
        source: 'web',
        legalBasis: 'consent'
      };

      // Update consent
      const updateResponse = await userService.updateConsent(testUserReferenceId, consent);
      expect(updateResponse.success).toBe(true);
      expect(updateResponse.data).toBeDefined();

      // Get consents
      const getResponse = await userService.getUserConsents(testUserReferenceId);
      expect(getResponse.success).toBe(true);
      expect(getResponse.data).toBeDefined();

      // Withdraw consent
      const withdrawResponse = await userService.withdrawConsent(testUserReferenceId, consent.consentKey);
      expect(withdrawResponse.success).toBe(true);
    });
  });

  describe('Payment Service', () => {
    test('should get supported payment methods', async () => {
      const response = await paymentService.getSupportedPaymentMethods();
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.methods).toBeDefined();
    });

    test('should calculate payment fees', async () => {
      const response = await paymentService.getPaymentFees(100, 'USD', 'CREDIT_CARD');
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.totalFee).toBeDefined();
    });

    test('should manage payment methods', async () => {
      const paymentMethod = {
        type: 'CREDIT_CARD' as const,
        provider: 'VISA',
        encryptedDetails: 'encrypted_card_data',
        isDefault: true
      };

      // Add payment method
      const addResponse = await paymentService.addPaymentMethod(testUserReferenceId, paymentMethod);
      expect(addResponse.success).toBe(true);
      expect(addResponse.data).toBeDefined();

      const methodId = addResponse.data!.id;

      // Get payment methods
      const getResponse = await paymentService.getPaymentMethods(testUserReferenceId);
      expect(getResponse.success).toBe(true);
      expect(getResponse.data).toBeDefined();

      // Delete payment method
      const deleteResponse = await paymentService.deletePaymentMethod(methodId);
      expect(deleteResponse.success).toBe(true);
    });

    test('should create and manage payment intents', async () => {
      const paymentIntent = {
        amount: 100,
        currency: 'USD',
        description: 'Test payment',
        paymentMethods: ['CREDIT_CARD']
      };

      // Create payment intent
      const createResponse = await paymentService.createPaymentIntent(testUserReferenceId, paymentIntent);
      expect(createResponse.success).toBe(true);
      expect(createResponse.data).toBeDefined();

      const intentId = createResponse.data!.id;

      // Get payment intent
      const getResponse = await paymentService.getPaymentIntent(intentId);
      expect(getResponse.success).toBe(true);
      expect(getResponse.data).toBeDefined();

      // Cancel payment intent
      const cancelResponse = await paymentService.cancelPaymentIntent(intentId);
      expect(cancelResponse.success).toBe(true);
    });
  });

  describe('Treasure Service', () => {
    test('should get treasure balance', async () => {
      const response = await treasureService.getTreasureBalance(testUserReferenceId);
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.userReferenceId).toBe(testUserReferenceId);
    });

    test('should get available rewards', async () => {
      const response = await treasureService.getAvailableRewards();
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.content).toBeDefined();
    });

    test('should get reward categories', async () => {
      const response = await treasureService.getRewardCategories();
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
    });

    test('should get transaction history', async () => {
      const response = await treasureService.getTransactionHistory(testUserReferenceId);
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.content).toBeDefined();
    });

    test('should get treasure statistics', async () => {
      const response = await treasureService.getTreasureStats(testUserReferenceId);
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
      expect(response.data?.currentBalance).toBeDefined();
    });

    test('should get leaderboard', async () => {
      const response = await treasureService.getLeaderboard();
      
      expect(response.success).toBe(true);
      expect(response.data).toBeDefined();
    });
  });

  describe('Health Checks', () => {
    test('should check user service health', async () => {
      const response = await userService.healthCheck();
      expect(response.success).toBe(true);
    });

    test('should check payment service health', async () => {
      const response = await paymentService.healthCheck();
      expect(response.success).toBe(true);
    });

    test('should check treasure service health', async () => {
      const response = await treasureService.healthCheck();
      expect(response.success).toBe(true);
    });
  });
});

// Helper functions

async function waitForServices(maxAttempts = 30, delay = 2000): Promise<void> {
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      // Test if API Gateway is responding
      const response = await fetch(`${TEST_CONFIG.apiBaseUrl}/actuator/health`);
      if (response.ok) {
        console.log('Services are ready!');
        return;
      }
    } catch (error) {
      // Service not ready yet
    }

    if (attempt === maxAttempts) {
      throw new Error('Services failed to become ready within timeout');
    }

    console.log(`Waiting for services... (attempt ${attempt}/${maxAttempts})`);
    await new Promise(resolve => setTimeout(resolve, delay));
  }
}

// Test utilities for mocking API responses
export const mockApiResponse = <T>(data: T, success = true) => ({
  success,
  data: success ? data : undefined,
  error: success ? undefined : { message: 'Mock error', code: 'MOCK_ERROR' },
  timestamp: new Date().toISOString(),
  requestId: 'mock-request-id'
});

// Test data factories
export const createTestUser = (overrides = {}) => ({
  ...TEST_CONFIG.testUser,
  ...overrides
});

export const createTestAddress = (overrides = {}) => ({
  type: 'HOME' as const,
  addressLine1: '123 Test Street',
  city: 'Test City',
  state: 'Test State',
  postalCode: '12345',
  country: 'Test Country',
  isPrimary: true,
  ...overrides
});

export const createTestPaymentMethod = (overrides = {}) => ({
  type: 'CREDIT_CARD' as const,
  provider: 'VISA',
  encryptedDetails: 'encrypted_card_data',
  isDefault: true,
  ...overrides
});
