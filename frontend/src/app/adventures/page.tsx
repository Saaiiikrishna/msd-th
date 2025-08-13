import { Metadata } from 'next';
import { Suspense } from 'react';
import { AdventuresHeader } from '@/components/adventures/adventures-header';
import { AdventuresFilters } from '@/components/adventures/adventures-filters';
import { AdventuresGrid } from '@/components/adventures/adventures-grid';
import { AdventuresMap } from '@/components/adventures/adventures-map';
import { Header } from '@/components/layout/header';
import { Footer } from '@/components/layout/footer';

export const metadata: Metadata = {
  title: 'Adventures',
  description: 'Discover exciting treasure hunts and adventure experiences across India. Filter by location, difficulty, price, and more.',
  openGraph: {
    title: 'Adventures - Treasure Hunt Experiences',
    description: 'Discover exciting treasure hunts and adventure experiences across India.',
    images: ['/og-adventures.jpg'],
  },
};

interface AdventuresPageProps {
  searchParams: {
    location?: string;
    difficulty?: string;
    price?: string;
    category?: string;
    date?: string;
    participants?: string;
    view?: 'grid' | 'map';
    sort?: string;
    page?: string;
  };
}

export default function AdventuresPage({ searchParams }: AdventuresPageProps) {
  return (
    <div className="min-h-screen bg-neutral-50">
      <Header />
      
      <main className="pt-20">
        {/* Page Header */}
        <AdventuresHeader />
        
        {/* Filters and Content */}
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="flex flex-col lg:flex-row gap-8">
            {/* Sidebar Filters */}
            <aside className="lg:w-80 shrink-0">
              <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
                <AdventuresFilters searchParams={searchParams} />
              </Suspense>
            </aside>
            
            {/* Main Content */}
            <div className="flex-1 min-w-0">
              <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
                {searchParams.view === 'map' ? (
                  <AdventuresMap searchParams={searchParams} />
                ) : (
                  <AdventuresGrid searchParams={searchParams} />
                )}
              </Suspense>
            </div>
          </div>
        </div>
      </main>
      
      <Footer />
    </div>
  );
}
