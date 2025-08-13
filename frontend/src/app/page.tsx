import { Metadata } from 'next';
import { HeroSection } from '@/components/sections/hero-section';
import { TrustIndicators } from '@/components/trust/trust-indicators';
import { FeaturedHunts } from '@/components/sections/featured-hunts';
import { HowItWorks } from '@/components/sections/how-it-works';
import { TestimonialsSection } from '@/components/sections/testimonials-section';
import { CTASection } from '@/components/sections/cta-section';
import { Header } from '@/components/layout/header';
import { Footer } from '@/components/layout/footer';

export const metadata: Metadata = {
  title: 'Home',
  description: 'Discover exciting treasure hunts and adventure experiences. Book your next adventure with MySillyDreams today!',
  openGraph: {
    title: 'Treasure Hunt Adventures - MySillyDreams',
    description: 'Discover exciting treasure hunts and adventure experiences. Book your next adventure with MySillyDreams today!',
    images: ['/og-home.jpg'],
  },
};

export default function HomePage() {
  return (
    <div className="min-h-screen">
      {/* Header */}
      <Header />
      
      {/* Main Content */}
      <main id="main-content" className="relative">
        {/* Hero Section */}
        <HeroSection />

        {/* Featured Hunts - Priority Business Logic */}
        <section className="section bg-gradient-subtle">
          <div className="container">
            <FeaturedHunts />
          </div>
        </section>

        {/* How It Works - Core Business Process */}
        <section className="section bg-neumorphic-light-bg">
          <div className="container">
            <HowItWorks />
          </div>
        </section>

        {/* Testimonials - Social Proof */}
        <section className="section bg-gradient-to-br from-neutral-50 to-neutral-100">
          <div className="container">
            <TestimonialsSection />
          </div>
        </section>

        {/* Call to Action */}
        <section className="section bg-gradient-primary text-white">
          <div className="container">
            <CTASection />
          </div>
        </section>

        {/* Trust Indicators - Moved to Bottom */}
        <section className="py-12 bg-neutral-100 border-t border-neutral-200">
          <div className="container">
            <TrustIndicators />
          </div>
        </section>
      </main>
      
      {/* Footer */}
      <Footer />
    </div>
  );
}
