'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Eye, 
  Edit, 
  Trash2, 
  MoreHorizontal,
  Star,
  MapPin,
  Calendar,
  Users,
  DollarSign,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import Link from 'next/link';

interface Adventure {
  id: string;
  title: string;
  description: string;
  category: string;
  location: string;
  price: number;
  duration: number;
  maxParticipants: number;
  rating: number;
  reviewCount: number;
  bookingCount: number;
  status: 'active' | 'draft' | 'archived';
  createdAt: string;
  updatedAt: string;
  imageUrl: string;
  tags: string[];
}

interface AdventuresTableProps {
  searchParams: {
    page?: string;
    search?: string;
    status?: string;
    category?: string;
    location?: string;
    sort?: string;
  };
}

export function AdventuresTable({ searchParams }: AdventuresTableProps) {
  const [adventures, setAdventures] = useState<Adventure[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedAdventures, setSelectedAdventures] = useState<string[]>([]);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [adventureToDelete, setAdventureToDelete] = useState<string | null>(null);

  const currentPage = parseInt(searchParams.page || '1');
  const itemsPerPage = 10;

  useEffect(() => {
    fetchAdventures();
  }, [searchParams]);

  const fetchAdventures = async () => {
    try {
      setIsLoading(true);
      setError(null);

      // Mock data for now - replace with actual API call
      const mockAdventures: Adventure[] = [
        {
          id: '1',
          title: 'Mumbai Heritage Walk',
          description: 'Explore the rich history and architecture of South Mumbai',
          category: 'Heritage Walk',
          location: 'Mumbai',
          price: 1500,
          duration: 3,
          maxParticipants: 15,
          rating: 4.8,
          reviewCount: 124,
          bookingCount: 89,
          status: 'active',
          createdAt: '2024-01-15T10:00:00Z',
          updatedAt: '2024-01-20T14:30:00Z',
          imageUrl: '/images/mumbai-heritage.jpg',
          tags: ['history', 'architecture', 'walking'],
        },
        {
          id: '2',
          title: 'Delhi Food Trail',
          description: 'Taste the authentic flavors of Old Delhi street food',
          category: 'Food Trail',
          location: 'Delhi',
          price: 1200,
          duration: 4,
          maxParticipants: 12,
          rating: 4.9,
          reviewCount: 98,
          bookingCount: 156,
          status: 'active',
          createdAt: '2024-01-10T09:00:00Z',
          updatedAt: '2024-01-18T16:45:00Z',
          imageUrl: '/images/delhi-food.jpg',
          tags: ['food', 'culture', 'street food'],
        },
        {
          id: '3',
          title: 'Goa Beach Adventure',
          description: 'Water sports and beach activities in North Goa',
          category: 'Adventure Sports',
          location: 'Goa',
          price: 2500,
          duration: 6,
          maxParticipants: 20,
          rating: 4.6,
          reviewCount: 67,
          bookingCount: 45,
          status: 'draft',
          createdAt: '2024-01-12T11:30:00Z',
          updatedAt: '2024-01-22T10:15:00Z',
          imageUrl: '/images/goa-beach.jpg',
          tags: ['beach', 'water sports', 'adventure'],
        },
      ];

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      setAdventures(mockAdventures);
    } catch (err) {
      console.error('Failed to fetch adventures:', err);
      setError('Failed to load adventures');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelectAdventure = (adventureId: string) => {
    setSelectedAdventures(prev => 
      prev.includes(adventureId)
        ? prev.filter(id => id !== adventureId)
        : [...prev, adventureId]
    );
  };

  const handleSelectAll = () => {
    if (selectedAdventures.length === adventures.length) {
      setSelectedAdventures([]);
    } else {
      setSelectedAdventures(adventures.map(a => a.id));
    }
  };

  const handleDeleteAdventure = async (adventureId: string) => {
    try {
      // Mock delete API call
      await new Promise(resolve => setTimeout(resolve, 500));
      setAdventures(prev => prev.filter(a => a.id !== adventureId));
      setSelectedAdventures(prev => prev.filter(id => id !== adventureId));
      setShowDeleteModal(false);
      setAdventureToDelete(null);
    } catch (err) {
      console.error('Failed to delete adventure:', err);
    }
  };

  const getStatusBadge = (status: Adventure['status']) => {
    const statusConfig = {
      active: {
        icon: CheckCircle,
        className: 'bg-green-100 text-green-700 border-green-200',
        label: 'Active'
      },
      draft: {
        icon: AlertCircle,
        className: 'bg-yellow-100 text-yellow-700 border-yellow-200',
        label: 'Draft'
      },
      archived: {
        icon: XCircle,
        className: 'bg-gray-100 text-gray-700 border-gray-200',
        label: 'Archived'
      }
    };

    const config = statusConfig[status];
    const Icon = config.icon;

    return (
      <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium border ${config.className}`}>
        <Icon className="w-3 h-3" />
        {config.label}
      </span>
    );
  };

  if (isLoading) {
    return (
      <NeumorphicCard className="p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-neutral-200 rounded w-1/4"></div>
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex gap-4 p-4 border border-neutral-200 rounded-lg">
              <div className="w-16 h-16 bg-neutral-200 rounded-lg"></div>
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-neutral-200 rounded w-1/3"></div>
                <div className="h-3 bg-neutral-200 rounded w-1/2"></div>
                <div className="h-3 bg-neutral-200 rounded w-1/4"></div>
              </div>
            </div>
          ))}
        </div>
      </NeumorphicCard>
    );
  }

  if (error) {
    return (
      <NeumorphicCard className="p-6 text-center">
        <p className="text-red-600 mb-4">{error}</p>
        <NeumorphicButton onClick={fetchAdventures} variant="primary">
          Retry
        </NeumorphicButton>
      </NeumorphicCard>
    );
  }

  return (
    <NeumorphicCard className="overflow-hidden">
      {/* Table Header */}
      <div className="p-6 border-b border-neutral-200">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold text-neutral-900">
            Adventures ({adventures.length})
          </h3>
          {selectedAdventures.length > 0 && (
            <div className="flex items-center gap-3">
              <span className="text-sm text-neutral-600">
                {selectedAdventures.length} selected
              </span>
              <NeumorphicButton
                variant="outline"
                size="sm"
                onClick={() => setSelectedAdventures([])}
              >
                Clear
              </NeumorphicButton>
              <NeumorphicButton
                variant="outline"
                size="sm"
                className="text-red-600 border-red-200 hover:bg-red-50"
              >
                Delete Selected
              </NeumorphicButton>
            </div>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-neutral-50 border-b border-neutral-200">
            <tr>
              <th className="w-12 px-6 py-3 text-left">
                <input
                  type="checkbox"
                  checked={selectedAdventures.length === adventures.length && adventures.length > 0}
                  onChange={handleSelectAll}
                  className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
                />
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-neutral-500 uppercase tracking-wider">
                Adventure
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-neutral-500 uppercase tracking-wider">
                Category
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-neutral-500 uppercase tracking-wider">
                Location
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-neutral-500 uppercase tracking-wider">
                Price
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-neutral-500 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-neutral-500 uppercase tracking-wider">
                Performance
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-neutral-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-neutral-200">
            {adventures.map((adventure, index) => (
              <motion.tr
                key={adventure.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3, delay: index * 0.1 }}
                className="hover:bg-neutral-50 transition-colors duration-200"
              >
                <td className="px-6 py-4">
                  <input
                    type="checkbox"
                    checked={selectedAdventures.includes(adventure.id)}
                    onChange={() => handleSelectAdventure(adventure.id)}
                    className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
                  />
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 bg-neutral-200 rounded-lg flex-shrink-0">
                      {/* Adventure image placeholder */}
                      <div className="w-full h-full bg-gradient-to-br from-primary-400 to-primary-600 rounded-lg"></div>
                    </div>
                    <div>
                      <h4 className="font-medium text-neutral-900 hover:text-primary-600 cursor-pointer">
                        {adventure.title}
                      </h4>
                      <p className="text-sm text-neutral-600 line-clamp-1">
                        {adventure.description}
                      </p>
                      <div className="flex items-center gap-2 mt-1">
                        <Clock className="w-3 h-3 text-neutral-400" />
                        <span className="text-xs text-neutral-500">
                          {adventure.duration}h
                        </span>
                        <Users className="w-3 h-3 text-neutral-400 ml-2" />
                        <span className="text-xs text-neutral-500">
                          Max {adventure.maxParticipants}
                        </span>
                      </div>
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4">
                  <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                    {adventure.category}
                  </span>
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-1 text-sm text-neutral-600">
                    <MapPin className="w-4 h-4 text-neutral-400" />
                    {adventure.location}
                  </div>
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-1 text-sm font-medium text-neutral-900">
                    <DollarSign className="w-4 h-4 text-neutral-400" />
                    ₹{adventure.price.toLocaleString()}
                  </div>
                </td>
                <td className="px-6 py-4">
                  {getStatusBadge(adventure.status)}
                </td>
                <td className="px-6 py-4">
                  <div className="space-y-1">
                    <div className="flex items-center gap-1 text-sm">
                      <Star className="w-3 h-3 text-yellow-400 fill-current" />
                      <span className="font-medium">{adventure.rating}</span>
                      <span className="text-neutral-500">({adventure.reviewCount})</span>
                    </div>
                    <div className="text-xs text-neutral-600">
                      {adventure.bookingCount} bookings
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <Link href={`/admin/adventures/${adventure.id}`}>
                      <NeumorphicButton variant="ghost" size="sm">
                        <Eye className="w-4 h-4" />
                      </NeumorphicButton>
                    </Link>
                    <Link href={`/admin/adventures/${adventure.id}/edit`}>
                      <NeumorphicButton variant="ghost" size="sm">
                        <Edit className="w-4 h-4" />
                      </NeumorphicButton>
                    </Link>
                    <NeumorphicButton
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setAdventureToDelete(adventure.id);
                        setShowDeleteModal(true);
                      }}
                      className="text-red-600 hover:bg-red-50"
                    >
                      <Trash2 className="w-4 h-4" />
                    </NeumorphicButton>
                  </div>
                </td>
              </motion.tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Empty State */}
      {adventures.length === 0 && (
        <div className="p-12 text-center">
          <MapPin className="w-12 h-12 text-neutral-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-neutral-900 mb-2">
            No adventures found
          </h3>
          <p className="text-neutral-600 mb-6">
            Get started by creating your first adventure.
          </p>
          <Link href="/admin/adventures/create">
            <NeumorphicButton variant="primary">
              Create Adventure
            </NeumorphicButton>
          </Link>
        </div>
      )}
    </NeumorphicCard>
  );
}
