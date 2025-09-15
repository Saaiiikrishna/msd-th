import { Suspense } from 'react';
import { AdventuresTable } from '@/components/admin/adventures/adventures-table';
import { AdventuresFilters } from '@/components/admin/adventures/adventures-filters';
import { AdventuresStats } from '@/components/admin/adventures/adventures-stats';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { Plus, Download, Upload } from 'lucide-react';
import Link from 'next/link';

interface AdventuresPageProps {
  searchParams: {
    page?: string;
    search?: string;
    status?: string;
    category?: string;
    location?: string;
    sort?: string;
  };
}

export default function AdventuresPage({ searchParams }: AdventuresPageProps) {
  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-neutral-900">Adventures</h1>
          <p className="text-neutral-600 mt-2">
            Manage your treasure hunt adventures and experiences.
          </p>
        </div>
        
        <div className="flex items-center gap-3">
          <NeumorphicButton variant="outline" size="sm">
            <Upload className="w-4 h-4 mr-2" />
            Import
          </NeumorphicButton>
          
          <NeumorphicButton variant="outline" size="sm">
            <Download className="w-4 h-4 mr-2" />
            Export
          </NeumorphicButton>
          
          <Link href="/admin/adventures/create">
            <NeumorphicButton variant="primary">
              <Plus className="w-4 h-4 mr-2" />
              Create Adventure
            </NeumorphicButton>
          </Link>
        </div>
      </div>

      {/* Stats */}
      <Suspense fallback={<div className="h-32 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
        <AdventuresStats />
      </Suspense>

      {/* Filters */}
      <Suspense fallback={<div className="h-20 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
        <AdventuresFilters searchParams={searchParams} />
      </Suspense>

      {/* Adventures Table */}
      <Suspense fallback={<div className="h-96 bg-neutral-200 rounded-neumorphic animate-pulse" />}>
        <AdventuresTable searchParams={searchParams} />
      </Suspense>
    </div>
  );
}
