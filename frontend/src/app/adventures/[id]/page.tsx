import { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { Suspense } from 'react';
import { AdventureHero } from '@/components/adventure-detail/adventure-hero';
import { AdventureInfo } from '@/components/adventure-detail/adventure-info';
import { AdventureBooking } from '@/components/adventure-detail/adventure-booking';
import { AdventureReviews } from '@/components/adventure-detail/adventure-reviews';
import { SimilarAdventures } from '@/components/adventure-detail/similar-adventures';
import { Header } from '@/components/layout/header';
import { Footer } from '@/components/layout/footer';

interface AdventureDetailPageProps {
  params: {
    id: string;
  };
  searchParams: {
    date?: string;
    participants?: string;
  };
}

// This would typically fetch from API in a real app
async function getAdventure(id: string) {
  // Mock data for now
  return {
    id,
    title: 'Mumbai Heritage Hunt',
    description: 'Explore the rich colonial heritage of South Mumbai through an exciting treasure hunt adventure.',
    location: 'Mumbai, Maharashtra',
    duration: '3 hours',
    difficulty: 'Easy' as const,
    price: 500,
    originalPrice: 650,
    rating: 4.8,
    reviewCount: 124,
    maxParticipants: 8,
    image: 'https://images.unsplash.com/photo-1570168007204-dfb528c6958f?w=1200&h=800&fit=crop',
    images: [
      'https://images.unsplash.com/photo-1570168007204-dfb528c6958f?w=1200&h=800&fit=crop',
      'https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=1200&h=800&fit=crop',
      'https://images.unsplash.com/photo-1566552881560-0be862a7c445?w=1200&h=800&fit=crop',
    ],
    tags: ['Heritage', 'Walking', 'Photography'],
    highlights: ['Gateway of India', 'Taj Hotel', 'Colaba Causeway'],
    isPopular: true,
    discount: 23,
    instantBooking: true,
    safetyVerified: true,
  };
}

export async function generateMetadata({ params }: AdventureDetailPageProps): Promise<Metadata> {
  const adventure = await getAdventure(params.id);
  
  if (!adventure) {
    return {
      title: 'Adventure Not Found',
    };
  }

  return {
    title: adventure.title,
    description: adventure.description,
    openGraph: {
      title: `${adventure.title} - Treasure Hunt Adventure`,
      description: adventure.description,
      images: [adventure.image],
    },
  };
}

export default async function AdventureDetailPage({ 
  params, 
  searchParams 
}: AdventureDetailPageProps) {
  const adventure = await getAdventure(params.id);

  if (!adventure) {
    notFound();
  }

  return (
    <div className="min-h-screen bg-neutral-50">
      <Header />
      
      <main className="pt-20">
        {/* Adventure Hero */}
        <AdventureHero adventure={adventure} />
        
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
            {/* Main Content */}
            <div className="lg:col-span-2 space-y-12">
              <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
                <AdventureInfo adventure={adventure} />
              </Suspense>
              
              <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
                <AdventureReviews adventureId={adventure.id} />
              </Suspense>
            </div>
            
            {/* Booking Sidebar */}
            <div className="lg:col-span-1">
              <div className="sticky top-24">
                <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
                  <AdventureBooking 
                    adventure={adventure} 
                    searchParams={searchParams}
                  />
                </Suspense>
              </div>
            </div>
          </div>
          
          {/* Similar Adventures */}
          <div className="mt-16">
            <Suspense fallback={<div className="h-64 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
              <SimilarAdventures adventureId={adventure.id} />
            </Suspense>
          </div>
        </div>
      </main>
      
      <Footer />
    </div>
  );
}
