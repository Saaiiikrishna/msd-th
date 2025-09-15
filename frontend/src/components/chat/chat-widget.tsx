'use client';

import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  MessageCircle, 
  X, 
  Send, 
  Paperclip, 
  Smile,
  Phone,
  Video,
  MoreVertical,
  User,
  Bot,
  CheckCheck,
  Clock
} from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { useChatMessages } from '@/hooks/use-chat';

interface Message {
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

const quickReplies = [
  'I need help with booking',
  'Payment issues',
  'Change my booking',
  'Refund request',
  'Adventure details',
  'Safety guidelines'
];

export function ChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [agentOnline, setAgentOnline] = useState(true);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  
  const { messages, sendMessage, isLoading } = useChatMessages();

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Simulate agent typing
  useEffect(() => {
    if (messages.length > 0 && messages[messages.length - 1].sender === 'user') {
      setIsTyping(true);
      const timer = setTimeout(() => {
        setIsTyping(false);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [messages]);

  const handleSendMessage = async () => {
    if (!message.trim()) return;

    const newMessage: Message = {
      id: Date.now().toString(),
      content: message,
      sender: 'user',
      timestamp: new Date(),
      status: 'sending',
      type: 'text'
    };

    await sendMessage(newMessage);
    setMessage('');
  };

  const handleQuickReply = (reply: string) => {
    setMessage(reply);
    handleSendMessage();
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <>
      {/* Chat Toggle Button */}
      <motion.div
        className="fixed bottom-6 right-6 z-50"
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ duration: 0.3, delay: 1 }}
      >
        <NeumorphicButton
          onClick={() => setIsOpen(!isOpen)}
          variant="primary"
          size="lg"
          className="relative w-16 h-16 rounded-full shadow-neumorphic-elevated hover:shadow-neumorphic-hover"
        >
          {isOpen ? (
            <X className="w-6 h-6" />
          ) : (
            <>
              <MessageCircle className="w-6 h-6" />
              {/* Notification Badge */}
              <span className="absolute -top-2 -right-2 w-6 h-6 bg-error-500 text-white text-xs rounded-full flex items-center justify-center">
                3
              </span>
            </>
          )}
        </NeumorphicButton>
      </motion.div>

      {/* Chat Window */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 100, scale: 0.8 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 100, scale: 0.8 }}
            transition={{ duration: 0.3 }}
            className="fixed bottom-24 right-6 w-96 h-[600px] z-50"
          >
            <NeumorphicCard className="h-full flex flex-col shadow-neumorphic-elevated">
              {/* Chat Header */}
              <div className="flex items-center justify-between p-4 border-b border-neutral-200">
                <div className="flex items-center gap-3">
                  <div className="relative">
                    <div className="w-10 h-10 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-full flex items-center justify-center">
                      <User className="w-5 h-5 text-white" />
                    </div>
                    {agentOnline && (
                      <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-success-500 rounded-full border-2 border-white"></div>
                    )}
                  </div>
                  <div>
                    <h3 className="font-semibold text-neutral-800">Support Team</h3>
                    <p className="text-sm text-neutral-600">
                      {agentOnline ? 'Online • Typically replies in minutes' : 'Offline • We\'ll reply soon'}
                    </p>
                  </div>
                </div>
                
                <div className="flex items-center gap-2">
                  <button className="p-2 text-neutral-600 hover:text-neutral-800 hover:bg-neutral-100 rounded-full transition-colors duration-200">
                    <Phone className="w-4 h-4" />
                  </button>
                  <button className="p-2 text-neutral-600 hover:text-neutral-800 hover:bg-neutral-100 rounded-full transition-colors duration-200">
                    <Video className="w-4 h-4" />
                  </button>
                  <button className="p-2 text-neutral-600 hover:text-neutral-800 hover:bg-neutral-100 rounded-full transition-colors duration-200">
                    <MoreVertical className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Messages Area */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {/* Welcome Message */}
                <div className="flex items-start gap-3">
                  <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-full flex items-center justify-center shrink-0">
                    <Bot className="w-4 h-4 text-white" />
                  </div>
                  <div className="flex-1">
                    <div className="bg-neutral-100 rounded-2xl rounded-tl-md p-3 max-w-xs">
                      <p className="text-sm text-neutral-800">
                        Hi! 👋 I'm here to help you with your treasure hunt booking. How can I assist you today?
                      </p>
                    </div>
                    <p className="text-xs text-neutral-500 mt-1">Just now</p>
                  </div>
                </div>

                {/* Quick Replies */}
                <div className="flex flex-wrap gap-2">
                  {quickReplies.slice(0, 3).map((reply) => (
                    <button
                      key={reply}
                      onClick={() => handleQuickReply(reply)}
                      className="px-3 py-2 bg-primary-50 text-primary-700 text-sm rounded-full border border-primary-200 hover:bg-primary-100 transition-colors duration-200"
                    >
                      {reply}
                    </button>
                  ))}
                </div>

                {/* Messages */}
                {messages.map((msg) => (
                  <motion.div
                    key={msg.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3 }}
                    className={`flex items-start gap-3 ${
                      msg.sender === 'user' ? 'flex-row-reverse' : ''
                    }`}
                  >
                    {msg.sender !== 'user' && (
                      <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-full flex items-center justify-center shrink-0">
                        {msg.sender === 'bot' ? (
                          <Bot className="w-4 h-4 text-white" />
                        ) : (
                          <User className="w-4 h-4 text-white" />
                        )}
                      </div>
                    )}
                    
                    <div className={`flex-1 ${msg.sender === 'user' ? 'text-right' : ''}`}>
                      <div
                        className={`inline-block p-3 rounded-2xl max-w-xs ${
                          msg.sender === 'user'
                            ? 'bg-primary-500 text-white rounded-tr-md'
                            : 'bg-neutral-100 text-neutral-800 rounded-tl-md'
                        }`}
                      >
                        <p className="text-sm">{msg.content}</p>
                      </div>
                      
                      <div className={`flex items-center gap-1 mt-1 text-xs text-neutral-500 ${
                        msg.sender === 'user' ? 'justify-end' : ''
                      }`}>
                        <span>{msg.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                        {msg.sender === 'user' && (
                          <div className="flex items-center">
                            {msg.status === 'sending' && <Clock className="w-3 h-3" />}
                            {msg.status === 'sent' && <CheckCheck className="w-3 h-3" />}
                            {msg.status === 'delivered' && <CheckCheck className="w-3 h-3 text-primary-500" />}
                            {msg.status === 'read' && <CheckCheck className="w-3 h-3 text-primary-500" />}
                          </div>
                        )}
                      </div>
                    </div>
                  </motion.div>
                ))}

                {/* Typing Indicator */}
                {isTyping && (
                  <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="flex items-start gap-3"
                  >
                    <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-full flex items-center justify-center shrink-0">
                      <User className="w-4 h-4 text-white" />
                    </div>
                    <div className="bg-neutral-100 rounded-2xl rounded-tl-md p-3">
                      <div className="flex items-center gap-1">
                        <div className="w-2 h-2 bg-neutral-400 rounded-full animate-bounce"></div>
                        <div className="w-2 h-2 bg-neutral-400 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
                        <div className="w-2 h-2 bg-neutral-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                      </div>
                    </div>
                  </motion.div>
                )}

                <div ref={messagesEndRef} />
              </div>

              {/* Message Input */}
              <div className="p-4 border-t border-neutral-200">
                <div className="flex items-end gap-3">
                  <button className="p-2 text-neutral-600 hover:text-neutral-800 hover:bg-neutral-100 rounded-full transition-colors duration-200">
                    <Paperclip className="w-5 h-5" />
                  </button>
                  
                  <div className="flex-1 relative">
                    <textarea
                      value={message}
                      onChange={(e) => setMessage(e.target.value)}
                      onKeyPress={handleKeyPress}
                      placeholder="Type your message..."
                      rows={1}
                      className="w-full p-3 pr-12 border border-neutral-300 rounded-2xl resize-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                      style={{ minHeight: '44px', maxHeight: '120px' }}
                    />
                    <button className="absolute right-3 top-1/2 transform -translate-y-1/2 p-1 text-neutral-600 hover:text-neutral-800 transition-colors duration-200">
                      <Smile className="w-4 h-4" />
                    </button>
                  </div>
                  
                  <NeumorphicButton
                    onClick={handleSendMessage}
                    disabled={!message.trim() || isLoading}
                    variant="primary"
                    size="sm"
                    className="w-11 h-11 rounded-full"
                  >
                    <Send className="w-4 h-4" />
                  </NeumorphicButton>
                </div>
                
                <p className="text-xs text-neutral-500 mt-2 text-center">
                  We typically reply within a few minutes
                </p>
              </div>
            </NeumorphicCard>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
