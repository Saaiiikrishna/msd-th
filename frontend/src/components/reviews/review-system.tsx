'use client';

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Star, 
  ThumbsUp, 
  ThumbsDown, 
  Flag, 
  Camera,
  Video,
  Filter,
  SortAsc,
  MoreHorizontal,
  CheckCircle,
  Shield
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { useReviews } from '@/hooks/use-reviews';
import { formatRelativeTime } from '@/lib/utils';

interface Review {
  id: string;
  userId: string;
  userName: string;
  userAvatar?: string;
  rating: number;
  title: string;
  content: string;
  images?: string[];
  videos?: string[];
  createdAt: Date;
  updatedAt?: Date;
  verified: boolean;
  helpful: number;
  notHelpful: number;
  userVote?: 'helpful' | 'not_helpful';
  response?: {
    content: string;
    author: string;
    createdAt: Date;
  };
  tags: string[];
  adventureDate?: Date;
}

interface ReviewSystemProps {
  adventureId: string;
  allowReviews?: boolean;
  showStats?: boolean;
  maxReviews?: number;
}

export function ReviewSystem({ 
  adventureId, 
  allowReviews = true, 
  showStats = true,
  maxReviews 
}: ReviewSystemProps) {
  const [filter, setFilter] = useState<'all' | 'verified' | 'photos' | 'recent'>('all');
  const [sortBy, setSortBy] = useState<'newest' | 'oldest' | 'rating_high' | 'rating_low' | 'helpful'>('newest');
  const [showReviewForm, setShowReviewForm] = useState(false);

  const { 
    reviews, 
    stats, 
    isLoading, 
    submitReview, 
    voteHelpful,
    reportReview 
  } = useReviews(adventureId, { filter, sortBy, limit: maxReviews });

  const filteredReviews = reviews?.filter((review: Review) => {
    switch (filter) {
      case 'verified':
        return review.verified;
      case 'photos':
        return review.images && review.images.length > 0;
      case 'recent':
        return new Date(review.createdAt) > new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
      default:
        return true;
    }
  }) || [];

  const renderStars = (rating: number, size: 'sm' | 'md' | 'lg' = 'md') => {
    const sizeClasses = {
      sm: 'w-3 h-3',
      md: 'w-4 h-4',
      lg: 'w-5 h-5',
    };

    return (
      <div className="flex items-center gap-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <Star
            key={star}
            className={`${sizeClasses[size]} ${
              star <= rating 
                ? 'text-warning-500 fill-current' 
                : 'text-neutral-300'
            }`}
          />
        ))}
      </div>
    );
  };

  const handleVote = async (reviewId: string, vote: 'helpful' | 'not_helpful') => {
    try {
      await voteHelpful(reviewId, vote);
    } catch (error) {
      console.error('Failed to vote:', error);
    }
  };

  const handleReport = async (reviewId: string, reason: string) => {
    try {
      await reportReview(reviewId, reason);
    } catch (error) {
      console.error('Failed to report:', error);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="animate-pulse">
            <NeumorphicCard>
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 bg-neutral-200 rounded-full"></div>
                <div className="flex-1 space-y-3">
                  <div className="h-4 bg-neutral-200 rounded w-1/4"></div>
                  <div className="h-3 bg-neutral-200 rounded w-1/3"></div>
                  <div className="space-y-2">
                    <div className="h-3 bg-neutral-200 rounded"></div>
                    <div className="h-3 bg-neutral-200 rounded w-3/4"></div>
                  </div>
                </div>
              </div>
            </NeumorphicCard>
          </div>
        ))}
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      className="space-y-6"
    >
      {/* Review Stats */}
      {showStats && stats && (
        <NeumorphicCard>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Overall Rating */}
            <div className="text-center">
              <div className="text-4xl font-bold text-neutral-800 mb-2">
                {stats.averageRating.toFixed(1)}
              </div>
              <div className="flex items-center justify-center mb-2">
                {renderStars(Math.round(stats.averageRating), 'lg')}
              </div>
              <p className="text-neutral-600">
                Based on {stats.totalReviews} review{stats.totalReviews !== 1 ? 's' : ''}
              </p>
            </div>

            {/* Rating Distribution */}
            <div className="space-y-2">
              {[5, 4, 3, 2, 1].map((rating) => {
                const count = stats.ratingDistribution[rating] || 0;
                const percentage = stats.totalReviews > 0 ? (count / stats.totalReviews) * 100 : 0;
                
                return (
                  <div key={rating} className="flex items-center gap-3">
                    <span className="text-sm text-neutral-600 w-8">{rating}★</span>
                    <div className="flex-1 bg-neutral-200 rounded-full h-2">
                      <div
                        className="bg-warning-500 h-2 rounded-full transition-all duration-500"
                        style={{ width: `${percentage}%` }}
                      />
                    </div>
                    <span className="text-sm text-neutral-600 w-8">{count}</span>
                  </div>
                );
              })}
            </div>

            {/* Quick Stats */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-neutral-600">Verified Reviews</span>
                <span className="font-medium">{stats.verifiedCount}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-neutral-600">With Photos</span>
                <span className="font-medium">{stats.withPhotosCount}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-neutral-600">Recent (30 days)</span>
                <span className="font-medium">{stats.recentCount}</span>
              </div>
            </div>
          </div>
        </NeumorphicCard>
      )}

      {/* Filters and Sort */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-neutral-600" />
          <div className="flex gap-2">
            {[
              { key: 'all', label: 'All Reviews' },
              { key: 'verified', label: 'Verified' },
              { key: 'photos', label: 'With Photos' },
              { key: 'recent', label: 'Recent' },
            ].map((filterOption) => (
              <button
                key={filterOption.key}
                onClick={() => setFilter(filterOption.key as any)}
                className={`px-3 py-1 text-sm rounded-full transition-colors duration-200 ${
                  filter === filterOption.key
                    ? 'bg-primary-500 text-white'
                    : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                }`}
              >
                {filterOption.label}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-2">
          <SortAsc className="w-4 h-4 text-neutral-600" />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            className="px-3 py-1 bg-neutral-100 border border-neutral-300 rounded-neumorphic focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="newest">Newest First</option>
            <option value="oldest">Oldest First</option>
            <option value="rating_high">Highest Rated</option>
            <option value="rating_low">Lowest Rated</option>
            <option value="helpful">Most Helpful</option>
          </select>
        </div>
      </div>

      {/* Write Review Button */}
      {allowReviews && (
        <div className="text-center">
          <NeumorphicButton
            variant="primary"
            onClick={() => setShowReviewForm(true)}
          >
            Write a Review
          </NeumorphicButton>
        </div>
      )}

      {/* Reviews List */}
      <div className="space-y-6">
        <AnimatePresence>
          {filteredReviews.map((review: Review, index: number) => (
            <motion.div
              key={review.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.4, delay: index * 0.1 }}
            >
              <NeumorphicCard>
                <div className="space-y-4">
                  {/* Review Header */}
                  <div className="flex items-start justify-between">
                    <div className="flex items-start gap-3">
                      <img
                        src={review.userAvatar || '/images/default-avatar.png'}
                        alt={review.userName}
                        className="w-12 h-12 rounded-full object-cover"
                      />
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="font-medium text-neutral-800">
                            {review.userName}
                          </h4>
                          {review.verified && (
                            <div className="flex items-center gap-1 text-success-600">
                              <CheckCircle className="w-4 h-4" />
                              <span className="text-xs">Verified</span>
                            </div>
                          )}
                        </div>
                        <div className="flex items-center gap-2 mt-1">
                          {renderStars(review.rating)}
                          <span className="text-sm text-neutral-500">
                            {formatRelativeTime(review.createdAt)}
                          </span>
                        </div>
                      </div>
                    </div>

                    <button className="p-1 text-neutral-400 hover:text-neutral-600 transition-colors duration-200">
                      <MoreHorizontal className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Review Content */}
                  <div>
                    {review.title && (
                      <h5 className="font-medium text-neutral-800 mb-2">
                        {review.title}
                      </h5>
                    )}
                    <p className="text-neutral-700 leading-relaxed">
                      {review.content}
                    </p>
                  </div>

                  {/* Review Media */}
                  {(review.images?.length || review.videos?.length) && (
                    <div className="flex gap-2 overflow-x-auto">
                      {review.images?.map((image, idx) => (
                        <img
                          key={idx}
                          src={image}
                          alt={`Review image ${idx + 1}`}
                          className="w-20 h-20 object-cover rounded-lg shrink-0"
                        />
                      ))}
                      {review.videos?.map((video, idx) => (
                        <video
                          key={idx}
                          src={video}
                          className="w-20 h-20 object-cover rounded-lg shrink-0"
                          controls
                        />
                      ))}
                    </div>
                  )}

                  {/* Review Tags */}
                  {review.tags.length > 0 && (
                    <div className="flex flex-wrap gap-2">
                      {review.tags.map((tag) => (
                        <span
                          key={tag}
                          className="px-2 py-1 bg-primary-50 text-primary-700 text-xs rounded-full"
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  )}

                  {/* Review Actions */}
                  <div className="flex items-center justify-between pt-4 border-t border-neutral-200">
                    <div className="flex items-center gap-4">
                      <button
                        onClick={() => handleVote(review.id, 'helpful')}
                        className={`flex items-center gap-1 text-sm transition-colors duration-200 ${
                          review.userVote === 'helpful'
                            ? 'text-success-600'
                            : 'text-neutral-600 hover:text-success-600'
                        }`}
                      >
                        <ThumbsUp className="w-4 h-4" />
                        <span>Helpful ({review.helpful})</span>
                      </button>
                      
                      <button
                        onClick={() => handleVote(review.id, 'not_helpful')}
                        className={`flex items-center gap-1 text-sm transition-colors duration-200 ${
                          review.userVote === 'not_helpful'
                            ? 'text-error-600'
                            : 'text-neutral-600 hover:text-error-600'
                        }`}
                      >
                        <ThumbsDown className="w-4 h-4" />
                        <span>({review.notHelpful})</span>
                      </button>
                    </div>

                    <button
                      onClick={() => handleReport(review.id, 'inappropriate')}
                      className="flex items-center gap-1 text-sm text-neutral-500 hover:text-error-600 transition-colors duration-200"
                    >
                      <Flag className="w-4 h-4" />
                      <span>Report</span>
                    </button>
                  </div>

                  {/* Business Response */}
                  {review.response && (
                    <div className="bg-neutral-50 rounded-neumorphic p-4 mt-4">
                      <div className="flex items-center gap-2 mb-2">
                        <Shield className="w-4 h-4 text-primary-600" />
                        <span className="font-medium text-primary-600">
                          Response from {review.response.author}
                        </span>
                        <span className="text-sm text-neutral-500">
                          {formatRelativeTime(review.response.createdAt)}
                        </span>
                      </div>
                      <p className="text-neutral-700">
                        {review.response.content}
                      </p>
                    </div>
                  )}
                </div>
              </NeumorphicCard>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>

      {/* Load More */}
      {filteredReviews.length > 0 && (!maxReviews || filteredReviews.length >= maxReviews) && (
        <div className="text-center">
          <NeumorphicButton variant="outline">
            Load More Reviews
          </NeumorphicButton>
        </div>
      )}

      {/* No Reviews */}
      {filteredReviews.length === 0 && (
        <NeumorphicCard className="text-center py-12">
          <Star className="w-12 h-12 text-neutral-300 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-neutral-800 mb-2">
            No Reviews Yet
          </h3>
          <p className="text-neutral-600 mb-4">
            Be the first to share your experience with this adventure!
          </p>
          {allowReviews && (
            <NeumorphicButton
              variant="primary"
              onClick={() => setShowReviewForm(true)}
            >
              Write the First Review
            </NeumorphicButton>
          )}
        </NeumorphicCard>
      )}
    </motion.div>
  );
}
