'use client';

import { forwardRef } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-neumorphic font-medium transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default: 'bg-neutral-100 text-neutral-900 shadow-neumorphic hover:shadow-neumorphic-hover active:shadow-neumorphic-pressed',
        primary: 'bg-primary-500 text-white shadow-neumorphic hover:shadow-neumorphic-hover hover:bg-primary-600 active:shadow-neumorphic-pressed',
        secondary: 'bg-secondary-500 text-white shadow-neumorphic hover:shadow-neumorphic-hover hover:bg-secondary-600 active:shadow-neumorphic-pressed',
        accent: 'bg-accent-500 text-white shadow-neumorphic hover:shadow-neumorphic-hover hover:bg-accent-600 active:shadow-neumorphic-pressed',
        ghost: 'bg-transparent text-primary-600 hover:bg-primary-50 hover:text-primary-700',
        outline: 'border border-primary-200 bg-transparent text-primary-600 hover:bg-primary-50 hover:text-primary-700',
        destructive: 'bg-error-500 text-white shadow-neumorphic hover:shadow-neumorphic-hover hover:bg-error-600 active:shadow-neumorphic-pressed',
      },
      size: {
        sm: 'h-9 px-3 text-sm',
        default: 'h-11 px-6 text-base',
        lg: 'h-12 px-8 text-lg',
        xl: 'h-14 px-10 text-xl',
        icon: 'h-11 w-11',
      },
      pressed: {
        true: 'shadow-neumorphic-pressed',
        false: '',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
      pressed: false,
    },
  }
);

export interface NeumorphicButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
  loading?: boolean;
}

const NeumorphicButton = forwardRef<HTMLButtonElement, NeumorphicButtonProps>(
  ({ className, variant, size, pressed, loading, disabled, children, ...props }, ref) => {
    return (
      <button
        className={cn(buttonVariants({ variant, size, pressed, className }))}
        ref={ref}
        disabled={disabled || loading}
        {...props}
      >
        {loading && (
          <svg
            className="mr-2 h-4 w-4 animate-spin"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        {children}
      </button>
    );
  }
);

NeumorphicButton.displayName = 'NeumorphicButton';

export { NeumorphicButton, buttonVariants };
