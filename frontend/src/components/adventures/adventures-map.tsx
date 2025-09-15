'use client';

import { NeumorphicCard } from '@/components/ui/neumorphic-card';

interface AdventuresMapProps {
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

export function AdventuresMap({ searchParams }: AdventuresMapProps) {
  return (
    <NeumorphicCard className="p-6">
      <div className="h-96 bg-neutral-200 rounded-lg flex items-center justify-center">
        <div className="text-center">
          <h3 className="text-lg font-semibold text-neutral-700 mb-2">
            Map View
          </h3>
          <p className="text-neutral-600">
            Interactive map coming soon
          </p>
        </div>
      </div>
    </NeumorphicCard>
  );
}
