// Analytics utility for tracking user interactions and conversions
declare global {
  interface Window {
    gtag: (...args: any[]) => void;
    dataLayer: any[];
    fbq: (...args: any[]) => void;
    clarity: (...args: any[]) => void;
  }
}

export interface AnalyticsEvent {
  action: string;
  category: string;
  label?: string;
  value?: number;
  custom_parameters?: Record<string, any>;
}

export interface EcommerceEvent {
  transaction_id: string;
  value: number;
  currency: string;
  items: Array<{
    item_id: string;
    item_name: string;
    category: string;
    quantity: number;
    price: number;
  }>;
}

class Analytics {
  private isEnabled: boolean;
  private userId?: string;

  constructor() {
    this.isEnabled = typeof window !== 'undefined' && 
                    process.env.NODE_ENV === 'production' &&
                    !!process.env.NEXT_PUBLIC_GA_MEASUREMENT_ID;
  }

  // Initialize analytics with user data
  initialize(userId?: string) {
    this.userId = userId;
    
    if (!this.isEnabled) return;

    // Set user properties
    if (userId) {
      this.setUserProperties({ user_id: userId });
    }

    // Track page view
    this.pageView(window.location.pathname);
  }

  // Track page views
  pageView(path: string, title?: string) {
    if (!this.isEnabled) return;

    window.gtag?.('config', process.env.NEXT_PUBLIC_GA_MEASUREMENT_ID!, {
      page_path: path,
      page_title: title || document.title,
      user_id: this.userId,
    });

    // Microsoft Clarity
    window.clarity?.('set', 'page_path', path);
  }

  // Track custom events
  event(eventData: AnalyticsEvent) {
    if (!this.isEnabled) return;

    const { action, category, label, value, custom_parameters } = eventData;

    // Google Analytics 4
    window.gtag?.('event', action, {
      event_category: category,
      event_label: label,
      value: value,
      user_id: this.userId,
      ...custom_parameters,
    });

    // Facebook Pixel
    window.fbq?.('track', action, {
      category,
      label,
      value,
      ...custom_parameters,
    });

    console.log('Analytics Event:', eventData);
  }

  // Track ecommerce events
  purchase(data: EcommerceEvent) {
    if (!this.isEnabled) return;

    window.gtag?.('event', 'purchase', {
      transaction_id: data.transaction_id,
      value: data.value,
      currency: data.currency,
      items: data.items,
      user_id: this.userId,
    });

    window.fbq?.('track', 'Purchase', {
      value: data.value,
      currency: data.currency,
      content_ids: data.items.map(item => item.item_id),
      content_type: 'product',
    });
  }

  // Track booking funnel
  beginCheckout(data: { value: number; currency: string; items: any[] }) {
    this.event({
      action: 'begin_checkout',
      category: 'ecommerce',
      value: data.value,
      custom_parameters: {
        currency: data.currency,
        items: data.items,
      },
    });
  }

  addToCart(data: { item_id: string; item_name: string; value: number }) {
    this.event({
      action: 'add_to_cart',
      category: 'ecommerce',
      label: data.item_name,
      value: data.value,
      custom_parameters: {
        item_id: data.item_id,
      },
    });
  }

  // Track user engagement
  selectContent(contentType: string, itemId: string) {
    this.event({
      action: 'select_content',
      category: 'engagement',
      label: contentType,
      custom_parameters: {
        content_type: contentType,
        item_id: itemId,
      },
    });
  }

  search(searchTerm: string, results?: number) {
    this.event({
      action: 'search',
      category: 'engagement',
      label: searchTerm,
      custom_parameters: {
        search_term: searchTerm,
        results_count: results,
      },
    });
  }

  share(method: string, contentType: string, itemId: string) {
    this.event({
      action: 'share',
      category: 'engagement',
      label: method,
      custom_parameters: {
        method,
        content_type: contentType,
        item_id: itemId,
      },
    });
  }

  // Track form interactions
  formStart(formName: string) {
    this.event({
      action: 'form_start',
      category: 'form',
      label: formName,
    });
  }

  formSubmit(formName: string, success: boolean) {
    this.event({
      action: success ? 'form_submit' : 'form_error',
      category: 'form',
      label: formName,
    });
  }

  // Track errors
  exception(description: string, fatal: boolean = false) {
    if (!this.isEnabled) return;

    window.gtag?.('event', 'exception', {
      description,
      fatal,
      user_id: this.userId,
    });
  }

  // Track timing
  timing(name: string, value: number, category: string = 'performance') {
    this.event({
      action: 'timing_complete',
      category,
      label: name,
      value,
    });
  }

  // Set user properties
  setUserProperties(properties: Record<string, any>) {
    if (!this.isEnabled) return;

    window.gtag?.('config', process.env.NEXT_PUBLIC_GA_MEASUREMENT_ID!, {
      custom_map: properties,
      user_id: this.userId,
    });
  }

  // Track scroll depth
  scrollDepth(percentage: number) {
    this.event({
      action: 'scroll',
      category: 'engagement',
      label: `${percentage}%`,
      value: percentage,
    });
  }

  // Track video interactions
  videoPlay(videoId: string, title: string) {
    this.event({
      action: 'video_play',
      category: 'video',
      label: title,
      custom_parameters: { video_id: videoId },
    });
  }

  videoPause(videoId: string, currentTime: number) {
    this.event({
      action: 'video_pause',
      category: 'video',
      value: Math.round(currentTime),
      custom_parameters: { video_id: videoId },
    });
  }

  videoComplete(videoId: string, duration: number) {
    this.event({
      action: 'video_complete',
      category: 'video',
      value: Math.round(duration),
      custom_parameters: { video_id: videoId },
    });
  }

  // Track file downloads
  fileDownload(fileName: string, fileType: string) {
    this.event({
      action: 'file_download',
      category: 'engagement',
      label: fileName,
      custom_parameters: { file_type: fileType },
    });
  }

  // Track outbound links
  outboundClick(url: string, linkText: string) {
    this.event({
      action: 'click',
      category: 'outbound',
      label: url,
      custom_parameters: { link_text: linkText },
    });
  }
}

// Create singleton instance
export const analytics = new Analytics();

// React hook for analytics
export function useAnalytics() {
  return {
    trackEvent: (event: AnalyticsEvent) => analytics.event(event),
    trackPageView: (path: string, title?: string) => analytics.pageView(path, title),
    trackPurchase: (data: EcommerceEvent) => analytics.purchase(data),
    trackBeginCheckout: (data: { value: number; currency: string; items: any[] }) => 
      analytics.beginCheckout(data),
    trackAddToCart: (data: { item_id: string; item_name: string; value: number }) => 
      analytics.addToCart(data),
    trackSearch: (searchTerm: string, results?: number) => 
      analytics.search(searchTerm, results),
    trackShare: (method: string, contentType: string, itemId: string) => 
      analytics.share(method, contentType, itemId),
    trackFormStart: (formName: string) => analytics.formStart(formName),
    trackFormSubmit: (formName: string, success: boolean) => 
      analytics.formSubmit(formName, success),
    trackException: (description: string, fatal?: boolean) => 
      analytics.exception(description, fatal),
    trackTiming: (name: string, value: number, category?: string) => 
      analytics.timing(name, value, category),
    setUserProperties: (properties: Record<string, any>) => 
      analytics.setUserProperties(properties),
  };
}
