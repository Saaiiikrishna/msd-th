import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';

// Chat types
export interface ChatMessage {
  id: string;
  content: string;
  sender: 'user' | 'agent' | 'bot';
  timestamp: Date;
  status: 'sending' | 'sent' | 'delivered' | 'read';
  type: 'text' | 'image' | 'file' | 'quick_reply';
  metadata?: {
    agentName?: string;
    agentAvatar?: string;
    quickReplies?: string[];
    fileUrl?: string;
    fileName?: string;
  };
}

export interface ChatSession {
  id: string;
  userId: string;
  agentId?: string;
  status: 'active' | 'waiting' | 'closed';
  createdAt: Date;
  updatedAt: Date;
  messages: ChatMessage[];
}

/**
 * Hook for managing chat messages and real-time communication
 */
export function useChatMessages(sessionId?: string) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const queryClient = useQueryClient();

  // Fetch chat history
  const { data: chatSession, isLoading } = useQuery({
    queryKey: ['chat-session', sessionId],
    queryFn: async () => {
      if (!sessionId) return null;
      const response = await apiClient.get<ChatSession>(`/api/chat/sessions/${sessionId}`);
      return response.data;
    },
    enabled: !!sessionId,
    staleTime: 30 * 1000, // 30 seconds
  });

  // Send message mutation
  const sendMessageMutation = useMutation({
    mutationFn: async (message: Omit<ChatMessage, 'id' | 'timestamp' | 'status'>) => {
      const response = await apiClient.post('/api/chat/messages', {
        sessionId,
        ...message,
      });
      return response.data;
    },
    onSuccess: (data) => {
      // Update local messages
      setMessages(prev => [...prev, data]);
      
      // Invalidate chat session query
      queryClient.invalidateQueries({ queryKey: ['chat-session', sessionId] });
    },
  });

  // Initialize WebSocket connection for real-time updates
  useEffect(() => {
    if (!sessionId) return;

    // Mock WebSocket connection
    const ws = {
      onopen: () => setIsConnected(true),
      onclose: () => setIsConnected(false),
      onmessage: (event: any) => {
        const data = JSON.parse(event.data);
        
        switch (data.type) {
          case 'message':
            setMessages(prev => [...prev, data.message]);
            break;
          case 'typing':
            setIsTyping(data.isTyping);
            break;
          case 'agent_joined':
            // Handle agent joining the chat
            break;
          case 'agent_left':
            // Handle agent leaving the chat
            break;
        }
      },
    };

    // Simulate connection
    setTimeout(() => ws.onopen(), 1000);

    return () => {
      setIsConnected(false);
    };
  }, [sessionId]);

  // Update messages when chat session data changes
  useEffect(() => {
    if (chatSession?.messages) {
      setMessages(chatSession.messages);
    }
  }, [chatSession]);

  const sendMessage = async (message: Omit<ChatMessage, 'id' | 'timestamp' | 'status'>) => {
    // Add message to local state immediately
    const tempMessage: ChatMessage = {
      ...message,
      id: `temp-${Date.now()}`,
      timestamp: new Date(),
      status: 'sending',
    };
    
    setMessages(prev => [...prev, tempMessage]);
    
    try {
      await sendMessageMutation.mutateAsync(message);
    } catch (error) {
      // Remove temp message on error
      setMessages(prev => prev.filter(msg => msg.id !== tempMessage.id));
      throw error;
    }
  };

  const markAsRead = async (messageId: string) => {
    try {
      await apiClient.patch(`/api/chat/messages/${messageId}/read`);
      
      setMessages(prev => 
        prev.map(msg => 
          msg.id === messageId ? { ...msg, status: 'read' } : msg
        )
      );
    } catch (error) {
      console.error('Failed to mark message as read:', error);
    }
  };

  return {
    messages,
    sendMessage,
    markAsRead,
    isLoading: isLoading || sendMessageMutation.isPending,
    isConnected,
    isTyping,
    error: sendMessageMutation.error,
  };
}

/**
 * Hook for managing chat sessions
 */
export function useChatSessions() {
  return useQuery({
    queryKey: ['chat-sessions'],
    queryFn: async () => {
      const response = await apiClient.get<ChatSession[]>('/api/chat/sessions');
      return response.data;
    },
    staleTime: 60 * 1000, // 1 minute
  });
}

/**
 * Hook for creating a new chat session
 */
export function useCreateChatSession() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: { subject?: string; priority?: 'low' | 'medium' | 'high' }) => {
      const response = await apiClient.post<ChatSession>('/api/chat/sessions', data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['chat-sessions'] });
    },
  });
}

/**
 * Hook for chat agent availability
 */
export function useChatAgentAvailability() {
  return useQuery({
    queryKey: ['chat-agent-availability'],
    queryFn: async () => {
      const response = await apiClient.get('/api/chat/agent-availability');
      return response.data;
    },
    staleTime: 30 * 1000, // 30 seconds
    refetchInterval: 60 * 1000, // Refetch every minute
  });
}

/**
 * Hook for chat quick replies and suggestions
 */
export function useChatQuickReplies() {
  return useQuery({
    queryKey: ['chat-quick-replies'],
    queryFn: async () => {
      const response = await apiClient.get('/api/chat/quick-replies');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Hook for uploading files in chat
 */
export function useChatFileUpload() {
  return useMutation({
    mutationFn: async ({ file, sessionId }: { file: File; sessionId: string }) => {
      const response = await apiClient.upload('/api/chat/upload', file, { sessionId });
      return response.data;
    },
  });
}

/**
 * Hook for chat analytics and metrics
 */
export function useChatAnalytics() {
  return useQuery({
    queryKey: ['chat-analytics'],
    queryFn: async () => {
      const response = await apiClient.get('/api/chat/analytics');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}
