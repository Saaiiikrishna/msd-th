import Head from 'next/head';
import { useRouter } from 'next/router';

interface SEOProps {
  title?: string;
  description?: string;
  keywords?: string[];
  image?: string;
  url?: string;
  type?: 'website' | 'article' | 'product';
  publishedTime?: string;
  modifiedTime?: string;
  author?: string;
  section?: string;
  tags?: string[];
  price?: {
    amount: number;
    currency: string;
  };
  availability?: 'InStock' | 'OutOfStock' | 'PreOrder';
  rating?: {
    value: number;
    count: number;
  };
  noIndex?: boolean;
  noFollow?: boolean;
  canonical?: string;
  alternates?: Array<{
    hreflang: string;
    href: string;
  }>;
}

const defaultSEO = {
  title: 'Treasure Hunt Adventures - MySillyDreams',
  description: 'Embark on exciting treasure hunts and adventure experiences across India. Book your next adventure with secure payments and instant confirmation.',
  keywords: ['treasure hunt', 'adventure', 'team building', 'India', 'booking', 'experiences', 'outdoor activities'],
  image: '/og-image.jpg',
  type: 'website' as const,
};

export function SEOHead({
  title,
  description,
  keywords = [],
  image,
  url,
  type = 'website',
  publishedTime,
  modifiedTime,
  author,
  section,
  tags = [],
  price,
  availability,
  rating,
  noIndex = false,
  noFollow = false,
  canonical,
  alternates = [],
}: SEOProps) {
  const router = useRouter();
  const baseUrl = process.env.NEXT_PUBLIC_BASE_URL || 'https://treasurehunt.mysillydreams.com';
  
  const seo = {
    title: title ? `${title} | ${defaultSEO.title}` : defaultSEO.title,
    description: description || defaultSEO.description,
    keywords: [...defaultSEO.keywords, ...keywords],
    image: image ? `${baseUrl}${image}` : `${baseUrl}${defaultSEO.image}`,
    url: url || `${baseUrl}${router.asPath}`,
    type,
  };

  const robotsContent = [
    noIndex ? 'noindex' : 'index',
    noFollow ? 'nofollow' : 'follow',
    'max-image-preview:large',
    'max-snippet:-1',
    'max-video-preview:-1',
  ].join(', ');

  // Generate JSON-LD structured data
  const generateStructuredData = () => {
    const baseData = {
      '@context': 'https://schema.org',
      '@type': type === 'product' ? 'Product' : 'WebPage',
      name: seo.title,
      description: seo.description,
      url: seo.url,
      image: seo.image,
    };

    if (type === 'product' && price) {
      return {
        ...baseData,
        '@type': 'Product',
        offers: {
          '@type': 'Offer',
          price: price.amount,
          priceCurrency: price.currency,
          availability: `https://schema.org/${availability || 'InStock'}`,
          url: seo.url,
        },
        aggregateRating: rating ? {
          '@type': 'AggregateRating',
          ratingValue: rating.value,
          reviewCount: rating.count,
        } : undefined,
      };
    }

    if (type === 'article') {
      return {
        ...baseData,
        '@type': 'Article',
        headline: seo.title,
        author: author ? {
          '@type': 'Person',
          name: author,
        } : undefined,
        publisher: {
          '@type': 'Organization',
          name: 'MySillyDreams',
          logo: {
            '@type': 'ImageObject',
            url: `${baseUrl}/logo.png`,
          },
        },
        datePublished: publishedTime,
        dateModified: modifiedTime || publishedTime,
        articleSection: section,
        keywords: tags.join(', '),
      };
    }

    return baseData;
  };

  const structuredData = generateStructuredData();

  return (
    <Head>
      {/* Basic Meta Tags */}
      <title>{seo.title}</title>
      <meta name="description" content={seo.description} />
      <meta name="keywords" content={seo.keywords.join(', ')} />
      <meta name="robots" content={robotsContent} />
      
      {/* Canonical URL */}
      <link rel="canonical" href={canonical || seo.url} />
      
      {/* Alternate Languages */}
      {alternates.map((alt) => (
        <link
          key={alt.hreflang}
          rel="alternate"
          hrefLang={alt.hreflang}
          href={alt.href}
        />
      ))}
      
      {/* Open Graph */}
      <meta property="og:type" content={seo.type} />
      <meta property="og:title" content={seo.title} />
      <meta property="og:description" content={seo.description} />
      <meta property="og:image" content={seo.image} />
      <meta property="og:url" content={seo.url} />
      <meta property="og:site_name" content="MySillyDreams" />
      <meta property="og:locale" content="en_IN" />
      
      {/* Article specific Open Graph */}
      {type === 'article' && (
        <>
          {publishedTime && <meta property="article:published_time" content={publishedTime} />}
          {modifiedTime && <meta property="article:modified_time" content={modifiedTime} />}
          {author && <meta property="article:author" content={author} />}
          {section && <meta property="article:section" content={section} />}
          {tags.map((tag) => (
            <meta key={tag} property="article:tag" content={tag} />
          ))}
        </>
      )}
      
      {/* Twitter Card */}
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:site" content="@mysillydreams" />
      <meta name="twitter:creator" content="@mysillydreams" />
      <meta name="twitter:title" content={seo.title} />
      <meta name="twitter:description" content={seo.description} />
      <meta name="twitter:image" content={seo.image} />
      
      {/* Additional Meta Tags */}
      <meta name="author" content={author || 'MySillyDreams'} />
      <meta name="publisher" content="MySillyDreams" />
      <meta name="copyright" content="MySillyDreams" />
      <meta name="language" content="English" />
      <meta name="revisit-after" content="7 days" />
      <meta name="distribution" content="global" />
      <meta name="rating" content="general" />
      
      {/* Mobile Optimization */}
      <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=5" />
      <meta name="format-detection" content="telephone=no" />
      <meta name="mobile-web-app-capable" content="yes" />
      <meta name="apple-mobile-web-app-capable" content="yes" />
      <meta name="apple-mobile-web-app-status-bar-style" content="default" />
      
      {/* Favicon and Icons */}
      <link rel="icon" href="/favicon.ico" sizes="any" />
      <link rel="icon" href="/favicon.svg" type="image/svg+xml" />
      <link rel="apple-touch-icon" href="/apple-touch-icon.png" />
      <link rel="manifest" href="/manifest.json" />
      
      {/* Theme Colors */}
      <meta name="theme-color" content="#3B82F6" />
      <meta name="msapplication-TileColor" content="#3B82F6" />
      <meta name="msapplication-config" content="/browserconfig.xml" />
      
      {/* Preconnect to External Domains */}
      <link rel="preconnect" href="https://fonts.googleapis.com" />
      <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
      <link rel="preconnect" href="https://images.unsplash.com" />
      <link rel="preconnect" href="https://checkout.razorpay.com" />
      <link rel="preconnect" href="https://www.google-analytics.com" />
      
      {/* DNS Prefetch */}
      <link rel="dns-prefetch" href="//fonts.googleapis.com" />
      <link rel="dns-prefetch" href="//images.unsplash.com" />
      <link rel="dns-prefetch" href="//checkout.razorpay.com" />
      
      {/* Structured Data */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(structuredData),
        }}
      />
      
      {/* Organization Schema */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'Organization',
            name: 'MySillyDreams',
            url: baseUrl,
            logo: `${baseUrl}/logo.png`,
            description: 'Leading provider of treasure hunt adventures and team building experiences across India',
            address: {
              '@type': 'PostalAddress',
              addressCountry: 'IN',
            },
            contactPoint: {
              '@type': 'ContactPoint',
              telephone: '+91-9876543210',
              contactType: 'customer service',
              availableLanguage: ['English', 'Hindi'],
            },
            sameAs: [
              'https://facebook.com/mysillydreams',
              'https://twitter.com/mysillydreams',
              'https://instagram.com/mysillydreams',
              'https://linkedin.com/company/mysillydreams',
            ],
          }),
        }}
      />
      
      {/* Website Schema */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'WebSite',
            name: 'MySillyDreams Treasure Hunt',
            url: baseUrl,
            description: seo.description,
            potentialAction: {
              '@type': 'SearchAction',
              target: `${baseUrl}/adventures?search={search_term_string}`,
              'query-input': 'required name=search_term_string',
            },
          }),
        }}
      />
    </Head>
  );
}
