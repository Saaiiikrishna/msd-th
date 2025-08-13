import { analytics } from './analytics';

export interface ABTest {
  id: string;
  name: string;
  description: string;
  variants: ABVariant[];
  traffic: number; // Percentage of users to include (0-100)
  status: 'draft' | 'running' | 'paused' | 'completed';
  startDate: Date;
  endDate?: Date;
  targetMetric: string;
  segments?: string[]; // User segments to target
}

export interface ABVariant {
  id: string;
  name: string;
  description: string;
  weight: number; // Traffic allocation percentage
  config: Record<string, any>;
}

export interface ABTestResult {
  testId: string;
  variantId: string;
  userId: string;
  timestamp: Date;
  converted: boolean;
  conversionValue?: number;
}

class ABTestingManager {
  private tests: Map<string, ABTest> = new Map();
  private userAssignments: Map<string, Map<string, string>> = new Map();
  private localStorage: Storage | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      this.localStorage = window.localStorage;
      this.loadUserAssignments();
      this.initializeTests();
    }
  }

  // Initialize default tests
  private initializeTests() {
    const defaultTests: ABTest[] = [
      {
        id: 'hero_cta_test',
        name: 'Hero CTA Button Test',
        description: 'Test different CTA button texts on homepage hero',
        variants: [
          {
            id: 'control',
            name: 'Control - Book Now',
            description: 'Original "Book Now" button',
            weight: 50,
            config: { buttonText: 'Book Now', buttonColor: 'primary' }
          },
          {
            id: 'variant_a',
            name: 'Variant A - Start Adventure',
            description: 'Alternative "Start Adventure" button',
            weight: 50,
            config: { buttonText: 'Start Adventure', buttonColor: 'secondary' }
          }
        ],
        traffic: 100,
        status: 'running',
        startDate: new Date(),
        targetMetric: 'booking_conversion',
      },
      {
        id: 'pricing_display_test',
        name: 'Pricing Display Test',
        description: 'Test different ways to display pricing',
        variants: [
          {
            id: 'control',
            name: 'Control - Standard',
            description: 'Standard pricing display',
            weight: 33,
            config: { showOriginalPrice: true, emphasizeDiscount: false }
          },
          {
            id: 'variant_a',
            name: 'Variant A - Emphasized Discount',
            description: 'Emphasize discount savings',
            weight: 33,
            config: { showOriginalPrice: true, emphasizeDiscount: true }
          },
          {
            id: 'variant_b',
            name: 'Variant B - Clean Price',
            description: 'Show only final price',
            weight: 34,
            config: { showOriginalPrice: false, emphasizeDiscount: false }
          }
        ],
        traffic: 50,
        status: 'running',
        startDate: new Date(),
        targetMetric: 'add_to_cart',
      },
      {
        id: 'checkout_flow_test',
        name: 'Checkout Flow Test',
        description: 'Test single-page vs multi-step checkout',
        variants: [
          {
            id: 'control',
            name: 'Control - Multi-step',
            description: 'Traditional multi-step checkout',
            weight: 50,
            config: { checkoutType: 'multi-step', steps: 3 }
          },
          {
            id: 'variant_a',
            name: 'Variant A - Single Page',
            description: 'Single-page checkout',
            weight: 50,
            config: { checkoutType: 'single-page', steps: 1 }
          }
        ],
        traffic: 30,
        status: 'running',
        startDate: new Date(),
        targetMetric: 'purchase_completion',
      }
    ];

    defaultTests.forEach(test => this.tests.set(test.id, test));
  }

  // Load user assignments from localStorage
  private loadUserAssignments() {
    if (!this.localStorage) return;

    try {
      const stored = this.localStorage.getItem('ab_test_assignments');
      if (stored) {
        const assignments = JSON.parse(stored);
        this.userAssignments = new Map(
          Object.entries(assignments).map(([userId, tests]) => [
            userId,
            new Map(Object.entries(tests as Record<string, string>))
          ])
        );
      }
    } catch (error) {
      console.error('Failed to load AB test assignments:', error);
    }
  }

  // Save user assignments to localStorage
  private saveUserAssignments() {
    if (!this.localStorage) return;

    try {
      const assignments: Record<string, Record<string, string>> = {};
      this.userAssignments.forEach((tests, userId) => {
        assignments[userId] = Object.fromEntries(tests);
      });
      this.localStorage.setItem('ab_test_assignments', JSON.stringify(assignments));
    } catch (error) {
      console.error('Failed to save AB test assignments:', error);
    }
  }

  // Generate user ID if not provided
  private getUserId(): string {
    if (!this.localStorage) return 'anonymous';

    let userId = this.localStorage.getItem('ab_test_user_id');
    if (!userId) {
      userId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      this.localStorage.setItem('ab_test_user_id', userId);
    }
    return userId;
  }

  // Hash function for consistent assignment
  private hash(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return Math.abs(hash);
  }

  // Assign user to variant
  private assignVariant(testId: string, userId: string): string | null {
    const test = this.tests.get(testId);
    if (!test || test.status !== 'running') return null;

    // Check if user should be included in test
    const userHash = this.hash(userId + testId);
    const trafficThreshold = (test.traffic / 100) * 0xFFFFFFFF;
    if (userHash > trafficThreshold) return null;

    // Assign to variant based on weight
    const variantHash = this.hash(userId + testId + 'variant') / 0xFFFFFFFF;
    let cumulativeWeight = 0;

    for (const variant of test.variants) {
      cumulativeWeight += variant.weight;
      if (variantHash * 100 <= cumulativeWeight) {
        return variant.id;
      }
    }

    return test.variants[0]?.id || null;
  }

  // Get variant for user and test
  getVariant(testId: string, userId?: string): string | null {
    const actualUserId = userId || this.getUserId();
    
    // Check existing assignment
    const userTests = this.userAssignments.get(actualUserId);
    if (userTests?.has(testId)) {
      return userTests.get(testId) || null;
    }

    // Assign new variant
    const variantId = this.assignVariant(testId, actualUserId);
    if (variantId) {
      if (!this.userAssignments.has(actualUserId)) {
        this.userAssignments.set(actualUserId, new Map());
      }
      this.userAssignments.get(actualUserId)!.set(testId, variantId);
      this.saveUserAssignments();

      // Track assignment
      analytics.event({
        action: 'ab_test_assignment',
        category: 'experiment',
        label: `${testId}:${variantId}`,
        custom_parameters: {
          test_id: testId,
          variant_id: variantId,
          user_id: actualUserId,
        },
      });
    }

    return variantId;
  }

  // Get variant configuration
  getVariantConfig(testId: string, userId?: string): Record<string, any> | null {
    const variantId = this.getVariant(testId, userId);
    if (!variantId) return null;

    const test = this.tests.get(testId);
    const variant = test?.variants.find(v => v.id === variantId);
    return variant?.config || null;
  }

  // Track conversion
  trackConversion(testId: string, conversionValue?: number, userId?: string) {
    const actualUserId = userId || this.getUserId();
    const variantId = this.getVariant(testId, actualUserId);
    
    if (!variantId) return;

    analytics.event({
      action: 'ab_test_conversion',
      category: 'experiment',
      label: `${testId}:${variantId}`,
      value: conversionValue,
      custom_parameters: {
        test_id: testId,
        variant_id: variantId,
        user_id: actualUserId,
        conversion_value: conversionValue,
      },
    });
  }

  // Get all active tests for user
  getActiveTests(userId?: string): Record<string, string> {
    const actualUserId = userId || this.getUserId();
    const activeTests: Record<string, string> = {};

    this.tests.forEach((test, testId) => {
      if (test.status === 'running') {
        const variantId = this.getVariant(testId, actualUserId);
        if (variantId) {
          activeTests[testId] = variantId;
        }
      }
    });

    return activeTests;
  }

  // Add new test
  addTest(test: ABTest) {
    this.tests.set(test.id, test);
  }

  // Update test status
  updateTestStatus(testId: string, status: ABTest['status']) {
    const test = this.tests.get(testId);
    if (test) {
      test.status = status;
      this.tests.set(testId, test);
    }
  }

  // Get test results (mock implementation)
  getTestResults(testId: string): Promise<any> {
    return new Promise((resolve) => {
      // In a real implementation, this would fetch from analytics API
      setTimeout(() => {
        resolve({
          testId,
          variants: [
            { id: 'control', conversions: 45, visitors: 1000, conversionRate: 4.5 },
            { id: 'variant_a', conversions: 52, visitors: 1000, conversionRate: 5.2 },
          ],
          significance: 0.85,
          winner: 'variant_a',
        });
      }, 1000);
    });
  }
}

// Create singleton instance
export const abTesting = new ABTestingManager();

// React hook for A/B testing
export function useABTest(testId: string) {
  const variantId = abTesting.getVariant(testId);
  const config = abTesting.getVariantConfig(testId);

  return {
    variantId,
    config,
    isControl: variantId === 'control',
    trackConversion: (value?: number) => abTesting.trackConversion(testId, value),
  };
}

// React component for A/B testing
export function ABTestProvider({ 
  testId, 
  children 
}: { 
  testId: string; 
  children: (variant: { id: string | null; config: any }) => React.ReactNode;
}) {
  const variantId = abTesting.getVariant(testId);
  const config = abTesting.getVariantConfig(testId);

  return <>{children({ id: variantId, config })}</>;
}
