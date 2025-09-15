import type { Metadata } from 'next';
import { Inter, Poppins } from 'next/font/google';
import './globals.css';
import { Providers } from './providers';
import { Toaster } from 'react-hot-toast';
import { ChatWidget } from '@/components/chat/chat-widget';

const inter = Inter({ 
  subsets: ['latin'],
  variable: '--font-inter',
  display: 'swap',
});

const poppins = Poppins({ 
  subsets: ['latin'],
  weight: ['300', '400', '500', '600', '700'],
  variable: '--font-poppins',
  display: 'swap',
});

export const metadata: Metadata = {
  title: {
    default: 'Treasure Hunt - MySillyDreams',
    template: '%s | Treasure Hunt - MySillyDreams',
  },
  description: 'Embark on exciting treasure hunts and adventure experiences with MySillyDreams. Book your next adventure today!',
  keywords: ['treasure hunt', 'adventure', 'team building', 'outdoor activities', 'Mumbai', 'Delhi', 'Bangalore'],
  authors: [{ name: 'MySillyDreams Team' }],
  creator: 'MySillyDreams',
  publisher: 'MySillyDreams',
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
  metadataBase: new URL(process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:3000'),
  alternates: {
    canonical: '/',
  },
  openGraph: {
    type: 'website',
    locale: 'en_IN',
    url: '/',
    title: 'Treasure Hunt - MySillyDreams',
    description: 'Embark on exciting treasure hunts and adventure experiences with MySillyDreams.',
    siteName: 'Treasure Hunt - MySillyDreams',
    images: [
      {
        url: '/og-image.jpg',
        width: 1200,
        height: 630,
        alt: 'Treasure Hunt - MySillyDreams',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Treasure Hunt - MySillyDreams',
    description: 'Embark on exciting treasure hunts and adventure experiences with MySillyDreams.',
    images: ['/og-image.jpg'],
    creator: '@mysillydreams',
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-video-preview': -1,
      'max-image-preview': 'large',
      'max-snippet': -1,
    },
  },
  verification: {
    google: process.env.GOOGLE_SITE_VERIFICATION,
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={`${inter.variable} ${poppins.variable}`}>
      <head>
        {/* Preload critical fonts */}
        <link
          rel="preload"
          href="/fonts/inter-var.woff2"
          as="font"
          type="font/woff2"
          crossOrigin="anonymous"
        />
        
        {/* Favicon */}
        <link rel="icon" href="/favicon.ico" sizes="any" />
        <link rel="icon" href="/favicon.svg" type="image/svg+xml" />
        <link rel="apple-touch-icon" href="/apple-touch-icon.png" />
        <link rel="manifest" href="/manifest.json" />
        
        {/* Theme color */}
        <meta name="theme-color" content="#64748b" />
        <meta name="msapplication-TileColor" content="#64748b" />
        
        {/* Preconnect to external domains */}
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link rel="preconnect" href="https://checkout.razorpay.com" />
        <link rel="preconnect" href="https://api.razorpay.com" />
        
        {/* DNS prefetch for performance */}
        <link rel="dns-prefetch" href="https://images.unsplash.com" />
        <link rel="dns-prefetch" href="https://via.placeholder.com" />
      </head>
      <body className={`${inter.className} antialiased bg-neutral-50 text-neutral-900`}>
        <Providers>
          {/* Skip to main content for accessibility */}
          <a
            href="#main-content"
            className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 bg-primary-500 text-white px-4 py-2 rounded-md z-50"
          >
            Skip to main content
          </a>
          
          {/* Main application */}
          <div id="root" className="min-h-screen">
            {children}
          </div>

          {/* Global Interactive Components */}
          <ChatWidget />

          {/* Toast notifications */}
          <Toaster
            position="top-right"
            toastOptions={{
              duration: 4000,
              style: {
                background: '#ffffff',
                color: '#374151',
                boxShadow: '8px 8px 16px #d1d9e6, -8px -8px 16px #ffffff',
                border: 'none',
                borderRadius: '1rem',
              },
              success: {
                iconTheme: {
                  primary: '#4caf50',
                  secondary: '#ffffff',
                },
              },
              error: {
                iconTheme: {
                  primary: '#f44336',
                  secondary: '#ffffff',
                },
              },
            }}
          />
        </Providers>
        
        {/* Service Worker Registration */}
        <script
          dangerouslySetInnerHTML={{
            __html: `
              if ('serviceWorker' in navigator) {
                window.addEventListener('load', function() {
                  navigator.serviceWorker.register('/sw.js')
                    .then(function(registration) {
                      console.log('SW registered: ', registration);
                    })
                    .catch(function(registrationError) {
                      console.log('SW registration failed: ', registrationError);
                    });
                });
              }
            `,
          }}
        />
      </body>
    </html>
  );
}
