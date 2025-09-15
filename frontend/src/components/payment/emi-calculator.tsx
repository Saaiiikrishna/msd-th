'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Calculator, CreditCard, TrendingDown, Info, CheckCircle } from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { formatCurrency } from '@/lib/utils';

interface EMICalculatorProps {
  amount: number;
  onEMISelect?: (emiOption: EMIOption) => void;
}

interface EMIOption {
  tenure: number;
  monthlyEMI: number;
  totalAmount: number;
  interestRate: number;
  processingFee: number;
  totalInterest: number;
  isNoCost: boolean;
  bankName: string;
  cardType: string;
}

const emiOptions: EMIOption[] = [
  {
    tenure: 3,
    monthlyEMI: 700,
    totalAmount: 2100,
    interestRate: 0,
    processingFee: 100,
    totalInterest: 0,
    isNoCost: true,
    bankName: 'HDFC Bank',
    cardType: 'Credit Card'
  },
  {
    tenure: 6,
    monthlyEMI: 360,
    totalAmount: 2160,
    interestRate: 12,
    processingFee: 100,
    totalInterest: 60,
    isNoCost: false,
    bankName: 'ICICI Bank',
    cardType: 'Credit Card'
  },
  {
    tenure: 9,
    monthlyEMI: 250,
    totalAmount: 2250,
    interestRate: 15,
    processingFee: 150,
    totalInterest: 100,
    isNoCost: false,
    bankName: 'Axis Bank',
    cardType: 'Credit Card'
  },
  {
    tenure: 12,
    monthlyEMI: 195,
    totalAmount: 2340,
    interestRate: 18,
    processingFee: 200,
    totalInterest: 140,
    isNoCost: false,
    bankName: 'SBI Card',
    cardType: 'Credit Card'
  }
];

export function EMICalculator({ amount, onEMISelect }: EMICalculatorProps) {
  const [selectedEMI, setSelectedEMI] = useState<EMIOption | null>(null);
  const [showDetails, setShowDetails] = useState<number | null>(null);

  // Calculate EMI based on actual amount
  const calculateEMI = (principal: number, rate: number, tenure: number) => {
    if (rate === 0) return principal / tenure;
    const monthlyRate = rate / 100 / 12;
    const emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenure)) / 
                 (Math.pow(1 + monthlyRate, tenure) - 1);
    return Math.round(emi);
  };

  // Recalculate EMI options based on actual amount
  const actualEMIOptions = emiOptions.map(option => {
    const monthlyEMI = calculateEMI(amount, option.interestRate, option.tenure);
    const totalAmount = monthlyEMI * option.tenure + option.processingFee;
    const totalInterest = totalAmount - amount - option.processingFee;

    return {
      ...option,
      monthlyEMI,
      totalAmount,
      totalInterest: Math.max(0, totalInterest)
    };
  });

  const handleEMISelect = (emiOption: EMIOption) => {
    setSelectedEMI(emiOption);
    onEMISelect?.(emiOption);
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      <NeumorphicCard className="space-y-6">
        {/* Header */}
        <div className="flex items-center gap-3">
          <div className="p-2 bg-secondary-100 rounded-lg">
            <Calculator className="w-5 h-5 text-secondary-600" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-neutral-800">
              EMI Calculator
            </h3>
            <p className="text-sm text-neutral-600">
              Convert your payment of {formatCurrency(amount)} into easy monthly installments
            </p>
          </div>
        </div>

        {/* EMI Options */}
        <div className="space-y-3">
          {actualEMIOptions.map((option, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.4, delay: index * 0.1 }}
            >
              <div
                className={`relative p-4 rounded-neumorphic border cursor-pointer transition-all duration-200 ${
                  selectedEMI?.tenure === option.tenure
                    ? 'border-secondary-500 bg-secondary-50 shadow-neumorphic-pressed'
                    : 'border-neutral-300 hover:border-secondary-300 hover:bg-secondary-50'
                }`}
                onClick={() => handleEMISelect(option)}
              >
                {/* No Cost EMI Badge */}
                {option.isNoCost && (
                  <span className="absolute -top-2 -right-2 px-2 py-1 bg-success-500 text-white text-xs font-semibold rounded-full">
                    No Cost EMI
                  </span>
                )}

                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <CreditCard className="w-4 h-4 text-secondary-600" />
                      <span className="font-medium text-neutral-800">
                        {option.tenure} Months EMI
                      </span>
                      <span className="text-sm text-neutral-600">
                        • {option.bankName}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span className="text-neutral-600">Monthly EMI</span>
                        <div className="font-semibold text-lg text-neutral-800">
                          {formatCurrency(option.monthlyEMI)}
                        </div>
                      </div>
                      
                      <div>
                        <span className="text-neutral-600">Total Amount</span>
                        <div className="font-semibold text-neutral-800">
                          {formatCurrency(option.totalAmount)}
                        </div>
                      </div>
                      
                      <div>
                        <span className="text-neutral-600">Interest Rate</span>
                        <div className={`font-semibold ${
                          option.isNoCost ? 'text-success-600' : 'text-neutral-800'
                        }`}>
                          {option.isNoCost ? '0%' : `${option.interestRate}% p.a.`}
                        </div>
                      </div>
                      
                      <div>
                        <span className="text-neutral-600">Processing Fee</span>
                        <div className="font-semibold text-neutral-800">
                          {formatCurrency(option.processingFee)}
                        </div>
                      </div>
                    </div>

                    {/* Interest Savings */}
                    {option.isNoCost && (
                      <div className="mt-2 flex items-center gap-2 text-success-600">
                        <TrendingDown className="w-4 h-4" />
                        <span className="text-sm font-medium">
                          Save {formatCurrency(option.totalInterest)} in interest
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Selection Indicator */}
                  <div className="ml-4">
                    {selectedEMI?.tenure === option.tenure ? (
                      <CheckCircle className="w-6 h-6 text-secondary-600 fill-current" />
                    ) : (
                      <div className="w-6 h-6 border-2 border-neutral-300 rounded-full"></div>
                    )}
                  </div>
                </div>

                {/* Details Toggle */}
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowDetails(showDetails === index ? null : index);
                  }}
                  className="mt-3 flex items-center gap-1 text-sm text-secondary-600 hover:text-secondary-700"
                >
                  <Info className="w-4 h-4" />
                  <span>{showDetails === index ? 'Hide' : 'Show'} Details</span>
                </button>

                {/* Detailed Breakdown */}
                {showDetails === index && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    transition={{ duration: 0.3 }}
                    className="mt-4 pt-4 border-t border-neutral-200"
                  >
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <span className="text-neutral-600">Principal Amount:</span>
                        <div className="font-medium">{formatCurrency(amount)}</div>
                      </div>
                      <div>
                        <span className="text-neutral-600">Total Interest:</span>
                        <div className="font-medium">
                          {option.isNoCost ? formatCurrency(0) : formatCurrency(option.totalInterest)}
                        </div>
                      </div>
                      <div>
                        <span className="text-neutral-600">Processing Fee:</span>
                        <div className="font-medium">{formatCurrency(option.processingFee)}</div>
                      </div>
                      <div>
                        <span className="text-neutral-600">Total Payable:</span>
                        <div className="font-medium text-lg">{formatCurrency(option.totalAmount)}</div>
                      </div>
                    </div>

                    {/* Payment Schedule Preview */}
                    <div className="mt-4">
                      <h5 className="font-medium text-neutral-800 mb-2">Payment Schedule</h5>
                      <div className="space-y-1 max-h-32 overflow-y-auto">
                        {Array.from({ length: Math.min(option.tenure, 3) }, (_, i) => (
                          <div key={i} className="flex justify-between text-sm">
                            <span className="text-neutral-600">
                              {new Date(Date.now() + (i + 1) * 30 * 24 * 60 * 60 * 1000).toLocaleDateString('en-IN', { month: 'short', year: 'numeric' })}
                            </span>
                            <span className="font-medium">{formatCurrency(option.monthlyEMI)}</span>
                          </div>
                        ))}
                        {option.tenure > 3 && (
                          <div className="text-center text-neutral-500 text-xs">
                            ... and {option.tenure - 3} more payments
                          </div>
                        )}
                      </div>
                    </div>
                  </motion.div>
                )}
              </div>
            </motion.div>
          ))}
        </div>

        {/* Selected EMI Summary */}
        {selectedEMI && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
            className="bg-gradient-to-r from-secondary-50 to-accent-50 rounded-neumorphic p-4"
          >
            <h4 className="font-medium text-secondary-800 mb-3">Selected EMI Plan</h4>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <span className="text-secondary-600">Monthly Payment</span>
                <div className="font-bold text-lg text-secondary-800">
                  {formatCurrency(selectedEMI.monthlyEMI)}
                </div>
              </div>
              <div>
                <span className="text-secondary-600">Duration</span>
                <div className="font-bold text-lg text-secondary-800">
                  {selectedEMI.tenure} months
                </div>
              </div>
              <div>
                <span className="text-secondary-600">Total Amount</span>
                <div className="font-bold text-lg text-secondary-800">
                  {formatCurrency(selectedEMI.totalAmount)}
                </div>
              </div>
              <div>
                <span className="text-secondary-600">Interest</span>
                <div className={`font-bold text-lg ${
                  selectedEMI.isNoCost ? 'text-success-600' : 'text-secondary-800'
                }`}>
                  {selectedEMI.isNoCost ? 'No Cost' : formatCurrency(selectedEMI.totalInterest)}
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {/* Terms and Conditions */}
        <div className="bg-neutral-50 rounded-neumorphic p-4">
          <h5 className="font-medium text-neutral-800 mb-2">EMI Terms & Conditions</h5>
          <ul className="space-y-1 text-sm text-neutral-600">
            <li>• EMI facility is subject to bank approval</li>
            <li>• Processing fees are non-refundable</li>
            <li>• Interest rates may vary based on your credit profile</li>
            <li>• No-cost EMI means the interest is absorbed by the merchant</li>
            <li>• Late payment charges may apply as per bank policies</li>
          </ul>
        </div>

        {/* Action Button */}
        {selectedEMI && (
          <NeumorphicButton
            variant="secondary"
            size="lg"
            className="w-full"
          >
            Proceed with {selectedEMI.tenure}-Month EMI
          </NeumorphicButton>
        )}
      </NeumorphicCard>
    </motion.div>
  );
}
