import { Metadata } from 'next';
import { Suspense } from 'react';
import { CheckoutForm } from '@/components/checkout/checkout-form';
import { OrderSummary } from '@/components/checkout/order-summary';
import { TrustIndicators } from '@/components/trust/trust-indicators';
import { Header } from '@/components/layout/header';
import { Footer } from '@/components/layout/footer';

export const metadata: Metadata = {
  title: 'Checkout',
  description: 'Complete your treasure hunt booking with secure payment processing.',
};

interface CheckoutPageProps {
  searchParams: {
    adventure?: string;
    date?: string;
    time?: string;
    participants?: string;
    total?: string;
  };
}

export default function CheckoutPage({ searchParams }: CheckoutPageProps) {
  // Validate required parameters
  if (!searchParams.adventure || !searchParams.date || !searchParams.participants) {
    return (
      <div className="min-h-screen bg-neutral-50 flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-neutral-800 mb-4">Invalid Booking</h1>
          <p className="text-neutral-600 mb-6">
            Missing required booking information. Please start your booking again.
          </p>
          <a
            href="/adventures"
            className="inline-flex items-center px-6 py-3 bg-primary-500 text-white rounded-neumorphic hover:bg-primary-600 transition-colors duration-200"
          >
            Browse Adventures
          </a>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-neutral-50">
      <Header />
      
      <main className="pt-20">
        {/* Checkout Header */}
        <section className="bg-gradient-to-r from-primary-600 to-secondary-600 text-white py-12">
          <div className="container mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center">
              <h1 className="text-3xl md:text-4xl font-display font-bold mb-4">
                Complete Your Booking
              </h1>
              <p className="text-xl text-white/90 max-w-2xl mx-auto">
                You're just one step away from your amazing treasure hunt adventure!
              </p>
            </div>
          </div>
        </section>

        {/* Checkout Content */}
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
            {/* Main Checkout Form */}
            <div className="lg:col-span-2 space-y-8">
              {/* Customer Information */}
              <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
                <CheckoutForm searchParams={searchParams} />
              </Suspense>
            </div>
            
            {/* Order Summary Sidebar */}
            <div className="lg:col-span-1">
              <div className="sticky top-24 space-y-6">
                <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
                  <OrderSummary searchParams={searchParams} />
                </Suspense>
                
                {/* Trust Indicators */}
                <div className="scale-90 origin-top">
                  <TrustIndicators />
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
      
      <Footer />
    </div>
  );
}
