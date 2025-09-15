'use client';

import { forwardRef } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const cardVariants = cva(
  'rounded-neumorphic bg-neutral-100 transition-all duration-300',
  {
    variants: {
      variant: {
        default: 'shadow-neumorphic',
        elevated: 'shadow-neumorphic-elevated',
        pressed: 'shadow-neumorphic-pressed',
        soft: 'shadow-neumorphic-soft',
      },
      padding: {
        none: 'p-0',
        sm: 'p-4',
        default: 'p-6',
        lg: 'p-8',
        xl: 'p-10',
      },
      interactive: {
        true: 'cursor-pointer hover:shadow-neumorphic-hover',
        false: '',
      },
    },
    defaultVariants: {
      variant: 'default',
      padding: 'default',
      interactive: false,
    },
  }
);

export interface NeumorphicCardProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof cardVariants> {
  asChild?: boolean;
}

const NeumorphicCard = forwardRef<HTMLDivElement, NeumorphicCardProps>(
  ({ className, variant, padding, interactive, children, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(cardVariants({ variant, padding, interactive, className }))}
        {...props}
      >
        {children}
      </div>
    );
  }
);

NeumorphicCard.displayName = 'NeumorphicCard';

// Card Header Component
const NeumorphicCardHeader = forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn('flex flex-col space-y-1.5 p-6', className)}
    {...props}
  />
));
NeumorphicCardHeader.displayName = 'NeumorphicCardHeader';

// Card Title Component
const NeumorphicCardTitle = forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => (
  <h3
    ref={ref}
    className={cn('text-2xl font-semibold leading-none tracking-tight', className)}
    {...props}
  />
));
NeumorphicCardTitle.displayName = 'NeumorphicCardTitle';

// Card Description Component
const NeumorphicCardDescription = forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p
    ref={ref}
    className={cn('text-sm text-neutral-600', className)}
    {...props}
  />
));
NeumorphicCardDescription.displayName = 'NeumorphicCardDescription';

// Card Content Component
const NeumorphicCardContent = forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div ref={ref} className={cn('p-6 pt-0', className)} {...props} />
));
NeumorphicCardContent.displayName = 'NeumorphicCardContent';

// Card Footer Component
const NeumorphicCardFooter = forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn('flex items-center p-6 pt-0', className)}
    {...props}
  />
));
NeumorphicCardFooter.displayName = 'NeumorphicCardFooter';

export {
  NeumorphicCard,
  NeumorphicCardHeader,
  NeumorphicCardFooter,
  NeumorphicCardTitle,
  NeumorphicCardDescription,
  NeumorphicCardContent,
};
