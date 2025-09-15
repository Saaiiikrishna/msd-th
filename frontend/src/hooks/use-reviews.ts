import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { analytics } from '@/lib/analytics';

export interface Review {
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

export interface ReviewStats {
  totalReviews: number;
  averageRating: number;
  ratingDistribution: Record<number, number>;
  verifiedCount: number;
  withPhotosCount: number;
  recentCount: number;
}

export interface ReviewFilters {
  filter?: 'all' | 'verified' | 'photos' | 'recent';
  sortBy?: 'newest' | 'oldest' | 'rating_high' | 'rating_low' | 'helpful';
  limit?: number;
  offset?: number;
}

export interface SubmitReviewData {
  rating: number;
  title: string;
  content: string;
  images?: File[];
  tags?: string[];
  adventureDate?: Date;
}

export function useReviews(adventureId: string, filters: ReviewFilters = {}) {
  const queryClient = useQueryClient();

  // Fetch reviews
  const {
    data: reviewsData,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['reviews', adventureId, filters],
    queryFn: async () => {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params.append(key, value.toString());
        }
      });

      const response = await apiClient.get(`/api/adventures/${adventureId}/reviews?${params.toString()}`);
      return response.data;
    },
    staleTime: 2 * 60 * 1000, // 2 minutes
  });

  // Fetch review statistics
  const { data: stats } = useQuery({
    queryKey: ['review-stats', adventureId],
    queryFn: async () => {
      const response = await apiClient.get(`/api/adventures/${adventureId}/reviews/stats`);
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  // Submit review mutation
  const submitReviewMutation = useMutation({
    mutationFn: async (reviewData: SubmitReviewData) => {
      const formData = new FormData();
      
      formData.append('rating', reviewData.rating.toString());
      formData.append('title', reviewData.title);
      formData.append('content', reviewData.content);
      
      if (reviewData.tags) {
        formData.append('tags', JSON.stringify(reviewData.tags));
      }
      
      if (reviewData.adventureDate) {
        formData.append('adventureDate', reviewData.adventureDate.toISOString());
      }
      
      if (reviewData.images) {
        reviewData.images.forEach((image, index) => {
          formData.append(`images[${index}]`, image);
        });
      }

      const response = await apiClient.post(
        `/api/adventures/${adventureId}/reviews`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      );
      
      return response.data;
    },
    onSuccess: (data) => {
      // Track review submission
      analytics.event({
        action: 'review_submitted',
        category: 'engagement',
        label: adventureId,
        value: data.rating,
        custom_parameters: {
          adventure_id: adventureId,
          rating: data.rating,
          has_images: data.images && data.images.length > 0,
        },
      });

      // Invalidate and refetch reviews
      queryClient.invalidateQueries({ queryKey: ['reviews', adventureId] });
      queryClient.invalidateQueries({ queryKey: ['review-stats', adventureId] });
    },
  });

  // Vote helpful mutation
  const voteHelpfulMutation = useMutation({
    mutationFn: async ({ reviewId, vote }: { reviewId: string; vote: 'helpful' | 'not_helpful' }) => {
      const response = await apiClient.post(`/api/reviews/${reviewId}/vote`, { vote });
      return response.data;
    },
    onSuccess: (_, variables) => {
      // Track vote
      analytics.event({
        action: 'review_vote',
        category: 'engagement',
        label: variables.vote,
        custom_parameters: {
          review_id: variables.reviewId,
          vote_type: variables.vote,
        },
      });

      // Update the specific review in cache
      queryClient.setQueryData(['reviews', adventureId, filters], (oldData: any) => {
        if (!oldData?.reviews) return oldData;
        
        return {
          ...oldData,
          reviews: oldData.reviews.map((review: Review) => {
            if (review.id === variables.reviewId) {
              const updatedReview = { ...review };
              
              // Remove previous vote
              if (review.userVote === 'helpful') {
                updatedReview.helpful = Math.max(0, updatedReview.helpful - 1);
              } else if (review.userVote === 'not_helpful') {
                updatedReview.notHelpful = Math.max(0, updatedReview.notHelpful - 1);
              }
              
              // Add new vote
              if (variables.vote === 'helpful') {
                updatedReview.helpful += 1;
                updatedReview.userVote = 'helpful';
              } else {
                updatedReview.notHelpful += 1;
                updatedReview.userVote = 'not_helpful';
              }
              
              return updatedReview;
            }
            return review;
          }),
        };
      });
    },
  });

  // Report review mutation
  const reportReviewMutation = useMutation({
    mutationFn: async ({ reviewId, reason }: { reviewId: string; reason: string }) => {
      const response = await apiClient.post(`/api/reviews/${reviewId}/report`, { reason });
      return response.data;
    },
    onSuccess: (_, variables) => {
      // Track report
      analytics.event({
        action: 'review_reported',
        category: 'moderation',
        label: variables.reason,
        custom_parameters: {
          review_id: variables.reviewId,
          reason: variables.reason,
        },
      });
    },
  });

  // Update review mutation
  const updateReviewMutation = useMutation({
    mutationFn: async ({ reviewId, updates }: { reviewId: string; updates: Partial<SubmitReviewData> }) => {
      const formData = new FormData();
      
      Object.entries(updates).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          if (key === 'images' && Array.isArray(value)) {
            value.forEach((image, index) => {
              formData.append(`images[${index}]`, image);
            });
          } else if (key === 'tags' && Array.isArray(value)) {
            formData.append('tags', JSON.stringify(value));
          } else if (key === 'adventureDate' && value instanceof Date) {
            formData.append('adventureDate', value.toISOString());
          } else {
            formData.append(key, value.toString());
          }
        }
      });

      const response = await apiClient.patch(`/api/reviews/${reviewId}`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews', adventureId] });
    },
  });

  // Delete review mutation
  const deleteReviewMutation = useMutation({
    mutationFn: async (reviewId: string) => {
      await apiClient.delete(`/api/reviews/${reviewId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews', adventureId] });
      queryClient.invalidateQueries({ queryKey: ['review-stats', adventureId] });
    },
  });

  return {
    reviews: reviewsData?.reviews || [],
    stats,
    pagination: reviewsData?.pagination,
    isLoading,
    error,
    refetch,
    submitReview: submitReviewMutation.mutateAsync,
    voteHelpful: (reviewId: string, vote: 'helpful' | 'not_helpful') =>
      voteHelpfulMutation.mutateAsync({ reviewId, vote }),
    reportReview: (reviewId: string, reason: string) =>
      reportReviewMutation.mutateAsync({ reviewId, reason }),
    updateReview: (reviewId: string, updates: Partial<SubmitReviewData>) =>
      updateReviewMutation.mutateAsync({ reviewId, updates }),
    deleteReview: deleteReviewMutation.mutateAsync,
    isSubmitting: submitReviewMutation.isPending,
    isVoting: voteHelpfulMutation.isPending,
    isReporting: reportReviewMutation.isPending,
    isUpdating: updateReviewMutation.isPending,
    isDeleting: deleteReviewMutation.isPending,
  };
}

// Hook for user's own reviews
export function useUserReviews() {
  const queryClient = useQueryClient();

  const { data: userReviews, isLoading } = useQuery({
    queryKey: ['user-reviews'],
    queryFn: async () => {
      const response = await apiClient.get('/api/user/reviews');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  return {
    reviews: userReviews || [],
    isLoading,
  };
}

// Hook for review moderation (admin)
export function useReviewModeration() {
  const queryClient = useQueryClient();

  const { data: pendingReviews, isLoading } = useQuery({
    queryKey: ['pending-reviews'],
    queryFn: async () => {
      const response = await apiClient.get('/api/admin/reviews/pending');
      return response.data;
    },
    staleTime: 1 * 60 * 1000, // 1 minute
  });

  const moderateReviewMutation = useMutation({
    mutationFn: async ({ 
      reviewId, 
      action, 
      reason 
    }: { 
      reviewId: string; 
      action: 'approve' | 'reject' | 'flag'; 
      reason?: string;
    }) => {
      const response = await apiClient.post(`/api/admin/reviews/${reviewId}/moderate`, {
        action,
        reason,
      });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-reviews'] });
    },
  });

  return {
    pendingReviews: pendingReviews || [],
    isLoading,
    moderateReview: moderateReviewMutation.mutateAsync,
    isModerating: moderateReviewMutation.isPending,
  };
}
