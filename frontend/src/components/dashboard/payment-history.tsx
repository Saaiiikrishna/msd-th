'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  CreditCard,
  Calendar,
  Download,
  Filter,
  Search,
  CheckCircle,
  XCircle,
  Clock,
  RefreshCw
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';

interface Payment {
  id: string;
  bookingId: string;
  adventureTitle: string;
  amount: number;
  status: 'completed' | 'pending' | 'failed' | 'refunded';
  paymentMethod: 'card' | 'upi' | 'netbanking' | 'wallet';
  transactionId: string;
  date: string;
  refundAmount?: number;
  refundDate?: string;
}

export function PaymentHistory() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'completed' | 'pending' | 'failed' | 'refunded'>('all');
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const fetchPaymentHistory = async () => {
      setIsLoading(true);
      
      // Mock data - replace with actual API call
      const mockPayments: Payment[] = [
        {
          id: '1',
          bookingId: 'BK001',
          adventureTitle: 'Mumbai Heritage Walk',
          amount: 3000,
          status: 'completed',
          paymentMethod: 'card',
          transactionId: 'TXN123456789',
          date: '2024-02-01T10:30:00Z',
        },
        {
          id: '2',
          bookingId: 'BK002',
          adventureTitle: 'Delhi Food Trail',
          amount: 1200,
          status: 'completed',
          paymentMethod: 'upi',
          transactionId: 'TXN987654321',
          date: '2024-01-15T14:20:00Z',
        },
        {
          id: '3',
          bookingId: 'BK003',
          adventureTitle: 'Goa Beach Adventure',
          amount: 10000,
          status: 'pending',
          paymentMethod: 'netbanking',
          transactionId: 'TXN456789123',
          date: '2024-02-10T09:15:00Z',
        },
        {
          id: '4',
          bookingId: 'BK004',
          adventureTitle: 'Rajasthan Desert Safari',
          amount: 6000,
          status: 'completed',
          paymentMethod: 'card',
          transactionId: 'TXN789123456',
          date: '2024-01-05T16:45:00Z',
        },
        {
          id: '5',
          bookingId: 'BK005',
          adventureTitle: 'Kerala Backwaters Tour',
          amount: 7500,
          status: 'refunded',
          paymentMethod: 'card',
          transactionId: 'TXN321654987',
          date: '2024-01-10T11:30:00Z',
          refundAmount: 7500,
          refundDate: '2024-01-12T10:00:00Z',
        },
        {
          id: '6',
          bookingId: 'BK006',
          adventureTitle: 'Himachal Mountain Trek',
          amount: 4500,
          status: 'failed',
          paymentMethod: 'wallet',
          transactionId: 'TXN654987321',
          date: '2024-02-05T13:20:00Z',
        },
      ];

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      setPayments(mockPayments);
      setIsLoading(false);
    };

    fetchPaymentHistory();
  }, []);

  const getStatusIcon = (status: Payment['status']) => {
    switch (status) {
      case 'completed':
        return CheckCircle;
      case 'pending':
        return Clock;
      case 'failed':
        return XCircle;
      case 'refunded':
        return RefreshCw;
      default:
        return Clock;
    }
  };

  const getStatusColor = (status: Payment['status']) => {
    switch (status) {
      case 'completed':
        return 'text-green-600 bg-green-100';
      case 'pending':
        return 'text-yellow-600 bg-yellow-100';
      case 'failed':
        return 'text-red-600 bg-red-100';
      case 'refunded':
        return 'text-blue-600 bg-blue-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const getPaymentMethodIcon = (method: Payment['paymentMethod']) => {
    switch (method) {
      case 'card':
        return '💳';
      case 'upi':
        return '📱';
      case 'netbanking':
        return '🏦';
      case 'wallet':
        return '👛';
      default:
        return '💳';
    }
  };

  const filteredPayments = payments.filter(payment => {
    const matchesFilter = filter === 'all' || payment.status === filter;
    const matchesSearch = payment.adventureTitle.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         payment.transactionId.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  const totalAmount = payments
    .filter(p => p.status === 'completed')
    .reduce((sum, payment) => sum + payment.amount, 0);

  const totalRefunded = payments
    .filter(p => p.status === 'refunded')
    .reduce((sum, payment) => sum + (payment.refundAmount || 0), 0);

  if (isLoading) {
    return (
      <NeumorphicCard className="p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-neutral-200 rounded w-1/3 mb-6"></div>
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="flex justify-between items-center p-4 border border-neutral-200 rounded-lg">
                <div className="space-y-2">
                  <div className="h-4 bg-neutral-200 rounded w-48"></div>
                  <div className="h-3 bg-neutral-200 rounded w-32"></div>
                </div>
                <div className="h-6 bg-neutral-200 rounded w-20"></div>
              </div>
            ))}
          </div>
        </div>
      </NeumorphicCard>
    );
  }

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <NeumorphicCard className="p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-green-100 rounded-neumorphic flex items-center justify-center">
              <CheckCircle className="w-5 h-5 text-green-600" />
            </div>
            <div>
              <h3 className="font-semibold text-neutral-900">Total Paid</h3>
              <p className="text-2xl font-bold text-green-600">₹{totalAmount.toLocaleString()}</p>
            </div>
          </div>
        </NeumorphicCard>

        <NeumorphicCard className="p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-blue-100 rounded-neumorphic flex items-center justify-center">
              <RefreshCw className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <h3 className="font-semibold text-neutral-900">Total Refunded</h3>
              <p className="text-2xl font-bold text-blue-600">₹{totalRefunded.toLocaleString()}</p>
            </div>
          </div>
        </NeumorphicCard>

        <NeumorphicCard className="p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-purple-100 rounded-neumorphic flex items-center justify-center">
              <CreditCard className="w-5 h-5 text-purple-600" />
            </div>
            <div>
              <h3 className="font-semibold text-neutral-900">Total Transactions</h3>
              <p className="text-2xl font-bold text-purple-600">{payments.length}</p>
            </div>
          </div>
        </NeumorphicCard>
      </div>

      {/* Payment History */}
      <NeumorphicCard className="p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold text-neutral-900">
            Payment History
          </h2>
          <div className="flex items-center gap-3">
            <NeumorphicButton variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              Export
            </NeumorphicButton>
          </div>
        </div>

        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-neutral-400" />
            <input
              type="text"
              placeholder="Search by adventure or transaction ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-neutral-100 border border-neutral-200 rounded-neumorphic focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          
          <div className="flex bg-neutral-100 rounded-lg p-1">
            {(['all', 'completed', 'pending', 'failed', 'refunded'] as const).map((filterOption) => (
              <button
                key={filterOption}
                onClick={() => setFilter(filterOption)}
                className={`px-3 py-1 text-sm rounded-md transition-colors duration-200 capitalize ${
                  filter === filterOption
                    ? 'bg-white text-neutral-900 shadow-sm'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
              >
                {filterOption}
              </button>
            ))}
          </div>
        </div>

        {/* Payment List */}
        {filteredPayments.length === 0 ? (
          <div className="text-center py-8">
            <CreditCard className="w-12 h-12 text-neutral-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-neutral-900 mb-2">
              No payments found
            </h3>
            <p className="text-neutral-600">
              {searchQuery ? 'Try adjusting your search criteria.' : 'No payment history available.'}
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredPayments.map((payment, index) => {
              const StatusIcon = getStatusIcon(payment.status);
              const statusColor = getStatusColor(payment.status);
              
              return (
                <motion.div
                  key={payment.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.3, delay: index * 0.1 }}
                  className="flex items-center justify-between p-4 border border-neutral-200 rounded-lg hover:border-neutral-300 transition-colors duration-200"
                >
                  <div className="flex items-center gap-4">
                    <div className="text-2xl">
                      {getPaymentMethodIcon(payment.paymentMethod)}
                    </div>
                    
                    <div>
                      <h4 className="font-medium text-neutral-900">
                        {payment.adventureTitle}
                      </h4>
                      <div className="flex items-center gap-4 mt-1 text-sm text-neutral-600">
                        <span>ID: {payment.transactionId}</span>
                        <span>•</span>
                        <span>{new Date(payment.date).toLocaleDateString()}</span>
                        <span>•</span>
                        <span className="capitalize">{payment.paymentMethod}</span>
                      </div>
                      {payment.status === 'refunded' && payment.refundDate && (
                        <p className="text-xs text-blue-600 mt-1">
                          Refunded on {new Date(payment.refundDate).toLocaleDateString()}
                        </p>
                      )}
                    </div>
                  </div>
                  
                  <div className="text-right">
                    <div className="flex items-center gap-3">
                      <div>
                        <div className="font-semibold text-neutral-900">
                          ₹{payment.amount.toLocaleString()}
                        </div>
                        {payment.refundAmount && (
                          <div className="text-sm text-blue-600">
                            -₹{payment.refundAmount.toLocaleString()}
                          </div>
                        )}
                      </div>
                      
                      <div className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${statusColor}`}>
                        <StatusIcon className="w-3 h-3" />
                        <span className="capitalize">{payment.status}</span>
                      </div>
                    </div>
                  </div>
                </motion.div>
              );
            })}
          </div>
        )}
      </NeumorphicCard>
    </div>
  );
}
