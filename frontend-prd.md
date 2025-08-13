# Treasure Hunt Platform - Frontend PRD

## 🎯 **Project Overview**

### **Product Vision**
A comprehensive, modern web application for the Treasure Hunt platform featuring advanced payment processing, user management, and seamless treasure hunt experiences with Material Design and Neumorphism aesthetics.

### **Technology Stack**
- **Framework**: Next.js 14+ (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS + Material Design 3
- **UI Components**: Custom Neumorphic components
- **State Management**: Zustand + React Query
- **Authentication**: NextAuth.js
- **Payment Integration**: Razorpay SDK
- **Deployment**: Azure Kubernetes Service (AKS)
- **Container**: Docker with multi-stage builds
- **Orchestration**: Kubernetes with Helm charts

---

## 🏗️ **Architecture & Technical Requirements**

### **Next.js Optimization Features**
- **Server-Side Rendering (SSR)** for SEO and performance
- **Static Site Generation (SSG)** for marketing pages
- **Incremental Static Regeneration (ISR)** for dynamic content
- **Edge Functions** for geolocation-based features
- **Image Optimization** with Next.js Image component
- **Code Splitting** for optimal bundle sizes
- **Service Workers** for offline capabilities

### **Performance Requirements**
- **Core Web Vitals**: LCP < 2.5s, FID < 100ms, CLS < 0.1
- **Lighthouse Score**: 95+ for Performance, Accessibility, SEO
- **Bundle Size**: < 250KB initial load
- **API Response Time**: < 500ms average
- **Mobile Performance**: 90+ Lighthouse score

### **Design System**
- **Material Design 3** principles
- **Neumorphism** for interactive elements
- **Responsive Design** (Mobile-first approach)
- **Dark/Light Mode** support
- **Accessibility** (WCAG 2.1 AA compliance)

---

## 🎨 **Design & User Experience**

### **Visual Design Principles**

#### **Material Design 3 Implementation**
```typescript
// Color Palette
const colorPalette = {
  primary: {
    50: '#e8f5e8',
    100: '#c8e6c9',
    500: '#4caf50', // Treasure Hunt Green
    900: '#1b5e20'
  },
  secondary: {
    50: '#fff3e0',
    500: '#ff9800', // Adventure Orange
    900: '#e65100'
  },
  surface: {
    light: '#fafafa',
    dark: '#121212'
  }
}
```

#### **Neumorphism Components**
- **Soft shadows** for depth perception
- **Subtle gradients** for interactive elements
- **Rounded corners** (8px, 16px, 24px)
- **Elevated surfaces** for important actions
- **Pressed states** for tactile feedback

### **Typography System**
```css
/* Material Design Typography Scale */
.display-large { font-size: 57px; line-height: 64px; }
.display-medium { font-size: 45px; line-height: 52px; }
.headline-large { font-size: 32px; line-height: 40px; }
.title-large { font-size: 22px; line-height: 28px; }
.body-large { font-size: 16px; line-height: 24px; }
.label-large { font-size: 14px; line-height: 20px; }
```

---

## 🔐 **Authentication & User Service Requirements**

### **Authentication Service Integration**

#### **NextAuth.js Configuration**
```typescript
// auth.config.ts
export const authConfig = {
  providers: [
    CredentialsProvider({
      name: "credentials",
      credentials: {
        email: { label: "Email", type: "email" },
        password: { label: "Password", type: "password" }
      },
      async authorize(credentials) {
        // Integration with Auth Service API
        const response = await fetch(`${AUTH_SERVICE_URL}/api/auth/v1/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(credentials)
        });
        
        if (response.ok) {
          const user = await response.json();
          return {
            id: user.id,
            email: user.email,
            name: user.name,
            role: user.role,
            accessToken: user.accessToken,
            refreshToken: user.refreshToken
          };
        }
        return null;
      }
    }),
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET
    }),
    FacebookProvider({
      clientId: process.env.FACEBOOK_CLIENT_ID,
      clientSecret: process.env.FACEBOOK_CLIENT_SECRET
    })
  ],
  session: { strategy: "jwt" },
  pages: {
    signIn: '/auth/signin',
    signUp: '/auth/signup',
    error: '/auth/error'
  }
}
```

#### **Required Auth Service APIs**
```typescript
interface AuthServiceAPI {
  // Authentication
  login(credentials: LoginCredentials): Promise<AuthResponse>;
  register(userData: RegisterData): Promise<AuthResponse>;
  logout(token: string): Promise<void>;
  refreshToken(refreshToken: string): Promise<TokenResponse>;
  
  // Password Management
  forgotPassword(email: string): Promise<void>;
  resetPassword(token: string, newPassword: string): Promise<void>;
  changePassword(oldPassword: string, newPassword: string): Promise<void>;
  
  // Profile Management
  getProfile(userId: string): Promise<UserProfile>;
  updateProfile(userId: string, data: ProfileUpdate): Promise<UserProfile>;
  uploadAvatar(userId: string, file: File): Promise<string>;
  
  // Account Verification
  sendVerificationEmail(email: string): Promise<void>;
  verifyEmail(token: string): Promise<void>;
  sendPhoneOTP(phone: string): Promise<void>;
  verifyPhone(phone: string, otp: string): Promise<void>;
}
```

### **User Service Integration**

#### **Required User Service APIs**
```typescript
interface UserServiceAPI {
  // User Management
  getUser(userId: string): Promise<User>;
  updateUser(userId: string, data: UserUpdate): Promise<User>;
  deleteUser(userId: string): Promise<void>;
  
  // Preferences
  getUserPreferences(userId: string): Promise<UserPreferences>;
  updatePreferences(userId: string, prefs: UserPreferences): Promise<void>;
  
  // Activity & History
  getUserActivity(userId: string, pagination: Pagination): Promise<Activity[]>;
  getBookingHistory(userId: string, pagination: Pagination): Promise<Booking[]>;
  getPaymentHistory(userId: string, pagination: Pagination): Promise<Payment[]>;
  
  // Notifications
  getNotifications(userId: string): Promise<Notification[]>;
  markNotificationRead(notificationId: string): Promise<void>;
  updateNotificationSettings(userId: string, settings: NotificationSettings): Promise<void>;
  
  // Social Features
  getFriends(userId: string): Promise<User[]>;
  sendFriendRequest(userId: string, targetUserId: string): Promise<void>;
  acceptFriendRequest(requestId: string): Promise<void>;
}
```

---

## 💳 **Payment Integration Requirements**

### **Razorpay Integration Architecture**

#### **Payment Service Integration**
```typescript
// payment.service.ts
export class PaymentService {
  private razorpay: any;
  
  constructor() {
    this.razorpay = new window.Razorpay({
      key: process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID,
      name: 'Treasure Hunt',
      description: 'MySillyDreams',
      image: '/logo.png',
      theme: {
        color: '#4caf50'
      }
    });
  }

  // Payment Processing
  async processPayment(paymentData: PaymentRequest): Promise<PaymentResponse>;
  async createPaymentLink(linkData: PaymentLinkRequest): Promise<PaymentLink>;
  async processRefund(refundData: RefundRequest): Promise<Refund>;
  
  // Subscription Management
  async createSubscription(subData: SubscriptionRequest): Promise<Subscription>;
  async cancelSubscription(subscriptionId: string): Promise<void>;
  async pauseSubscription(subscriptionId: string): Promise<void>;
  
  // EMI & Advanced Features
  async getEmiOptions(amount: number): Promise<EmiOption[]>;
  async createEmiPlan(emiData: EmiRequest): Promise<EmiPlan>;
  async setupUpiAutoPay(mandateData: UpiMandateRequest): Promise<UpiMandate>;
  
  // International Payments
  async getExchangeRates(currency: string): Promise<ExchangeRate>;
  async processInternationalPayment(intlData: InternationalPaymentRequest): Promise<Payment>;
}
```

#### **Trust Widgets Integration**
```typescript
// trust-widgets.component.tsx
export const TrustWidgets = () => {
  const { data: trustConfig } = useTrustWidgets();
  
  return (
    <div className="trust-widgets">
      <MoneyBackGuarantee config={trustConfig.moneyBackGuarantee} />
      <SecurityBadge config={trustConfig.securityBadge} />
      <PaymentMethods config={trustConfig.paymentMethods} />
      <TrustIndicators indicators={trustConfig.trustIndicators} />
      <CustomerTestimonials testimonials={trustConfig.customerTestimonials} />
    </div>
  );
};
```

---

## 📱 **Core Application Features**

### **1. Landing & Marketing Pages**

#### **Homepage** (`/`)
- **Hero Section**: Neumorphic design with treasure hunt imagery
- **Trust Indicators**: Customer count, success rate, testimonials
- **Featured Hunts**: SSG with ISR for real-time updates
- **How It Works**: Interactive step-by-step guide
- **Trust Widgets**: Money-back guarantee, security badges

#### **Treasure Hunt Catalog** (`/hunts`)
- **Filter & Search**: Location, difficulty, price, date
- **Map Integration**: Interactive map with hunt locations
- **Real-time Availability**: WebSocket updates
- **Wishlist**: Save favorite hunts
- **Social Proof**: Reviews and ratings

### **2. User Dashboard**

#### **Dashboard Layout**
```typescript
// dashboard/layout.tsx
export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="dashboard-layout">
      <Sidebar />
      <main className="dashboard-main">
        <Header />
        <div className="dashboard-content">
          {children}
        </div>
      </main>
    </div>
  );
}
```

#### **Dashboard Sections**
- **Overview**: Upcoming hunts, recent activity, quick stats
- **My Bookings**: Active, completed, cancelled bookings
- **Payment History**: Transactions, refunds, EMI plans
- **Profile Settings**: Personal info, preferences, notifications
- **Subscription Management**: Active plans, billing, usage

### **3. Booking & Payment Flow**

#### **Booking Process**
1. **Hunt Selection** → **Date/Time** → **Participants** → **Add-ons**
2. **Guest Information** → **Payment Method** → **Review** → **Confirm**

#### **Payment Components**
```typescript
// components/payment/PaymentForm.tsx
export const PaymentForm = ({ bookingData }: { bookingData: BookingData }) => {
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('card');
  const [emiOptions, setEmiOptions] = useState<EmiOption[]>([]);
  
  return (
    <div className="payment-form neumorphic-card">
      <PaymentMethodSelector 
        selected={paymentMethod}
        onChange={setPaymentMethod}
      />
      
      {paymentMethod === 'card' && <CardPayment />}
      {paymentMethod === 'upi' && <UpiPayment />}
      {paymentMethod === 'netbanking' && <NetBankingPayment />}
      {paymentMethod === 'emi' && <EmiPayment options={emiOptions} />}
      
      <TrustWidgets />
      <PaymentSummary />
      <PayButton />
    </div>
  );
};
```

### **4. Advanced Payment Features**

#### **EMI Calculator**
```typescript
// components/payment/EmiCalculator.tsx
export const EmiCalculator = ({ amount }: { amount: number }) => {
  const { data: emiOptions } = useEmiOptions(amount);
  
  return (
    <div className="emi-calculator">
      <h3>EMI Options Available</h3>
      {emiOptions?.map(option => (
        <div key={option.tenure} className="emi-option neumorphic-card">
          <div className="emi-details">
            <span className="tenure">{option.tenure} months</span>
            <span className="emi-amount">₹{option.emiAmount}/month</span>
            <span className="interest-rate">{option.interestRate}% p.a.</span>
          </div>
          <button className="select-emi-btn">Select</button>
        </div>
      ))}
    </div>
  );
};
```

#### **International Payment Support**
```typescript
// components/payment/InternationalPayment.tsx
export const InternationalPayment = () => {
  const [currency, setCurrency] = useState('USD');
  const { data: conversion } = useCurrencyConversion(currency, amount);
  
  return (
    <div className="international-payment">
      <CurrencySelector 
        selected={currency}
        onChange={setCurrency}
      />
      <ConversionDisplay conversion={conversion} />
      <PaymentMethodsForCountry country={userCountry} />
    </div>
  );
};
```

---

## 🎯 **State Management Architecture**

### **Zustand Store Structure**
```typescript
// stores/index.ts
export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  isAuthenticated: false,
  login: async (credentials) => { /* ... */ },
  logout: () => { /* ... */ },
  refreshToken: async () => { /* ... */ }
}));

export const usePaymentStore = create<PaymentState>((set, get) => ({
  currentBooking: null,
  paymentMethod: 'card',
  savedMethods: [],
  setPaymentMethod: (method) => set({ paymentMethod: method }),
  savePaymentMethod: async (method) => { /* ... */ }
}));

export const useUIStore = create<UIState>((set, get) => ({
  theme: 'light',
  sidebarOpen: false,
  notifications: [],
  toggleTheme: () => set(state => ({ 
    theme: state.theme === 'light' ? 'dark' : 'light' 
  }))
}));
```

### **React Query Integration**
```typescript
// hooks/usePayment.ts
export const usePaymentMethods = () => {
  return useQuery({
    queryKey: ['payment-methods'],
    queryFn: () => paymentService.getPaymentMethods(),
    staleTime: 5 * 60 * 1000 // 5 minutes
  });
};

export const useCreatePayment = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: paymentService.createPayment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
    }
  });
};
```

---

## 📊 **Performance Optimization**

### **Next.js Optimizations**
```typescript
// next.config.js
const nextConfig = {
  experimental: {
    appDir: true,
    serverComponentsExternalPackages: ['razorpay']
  },
  images: {
    domains: ['images.unsplash.com', 'razorpay.com'],
    formats: ['image/webp', 'image/avif']
  },
  compress: true,
  poweredByHeader: false,
  generateEtags: false,
  swcMinify: true
};
```

### **Code Splitting Strategy**
```typescript
// Dynamic imports for heavy components
const PaymentForm = dynamic(() => import('@/components/PaymentForm'), {
  loading: () => <PaymentFormSkeleton />,
  ssr: false
});

const EmiCalculator = dynamic(() => import('@/components/EmiCalculator'), {
  loading: () => <div>Loading EMI options...</div>
});
```

### **Caching Strategy**
- **Static Assets**: CDN caching (1 year)
- **API Responses**: Redis caching (5-15 minutes)
- **User Data**: React Query caching (stale-while-revalidate)
- **Images**: Next.js Image optimization with WebP/AVIF

---

## 🔒 **Security Requirements**

### **Frontend Security**
- **CSP Headers**: Strict Content Security Policy
- **XSS Protection**: Input sanitization and validation
- **CSRF Protection**: Built-in Next.js CSRF protection
- **Secure Headers**: HSTS, X-Frame-Options, etc.
- **Environment Variables**: Secure handling of sensitive data

### **Payment Security**
- **PCI Compliance**: No card data storage on frontend
- **Tokenization**: Use Razorpay tokens for saved methods
- **SSL/TLS**: HTTPS enforcement
- **Input Validation**: Client and server-side validation
- **Rate Limiting**: API request throttling

---

## 📱 **Mobile Responsiveness**

### **Responsive Breakpoints**
```css
/* Tailwind CSS breakpoints */
sm: '640px',   /* Small devices */
md: '768px',   /* Medium devices */
lg: '1024px',  /* Large devices */
xl: '1280px',  /* Extra large devices */
2xl: '1536px'  /* 2X large devices */
```

### **Mobile-First Components**
```typescript
// components/responsive/MobilePayment.tsx
export const MobilePayment = () => {
  return (
    <div className="mobile-payment">
      {/* Mobile-optimized payment flow */}
      <div className="sticky-bottom">
        <PaymentSummary />
        <PayButton />
      </div>
    </div>
  );
};
```

### **Progressive Web App (PWA)**
- **Service Worker**: Offline functionality
- **App Manifest**: Install prompt
- **Push Notifications**: Booking reminders
- **Background Sync**: Offline payment queue

---

## 🧪 **Testing Strategy**

### **Testing Framework**
```typescript
// jest.config.js
const config = {
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  moduleNameMapping: {
    '^@/(.*)$': '<rootDir>/src/$1'
  },
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts'
  ]
};
```

### **Test Types**
- **Unit Tests**: Jest + React Testing Library
- **Integration Tests**: API integration testing
- **E2E Tests**: Playwright for payment flows
- **Visual Tests**: Chromatic for UI regression
- **Performance Tests**: Lighthouse CI

---

## 🚀 **Azure Kubernetes Deployment & DevOps**

### **AKS Cluster Architecture**
```yaml
# kubernetes/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: treasure-hunt
  labels:
    name: treasure-hunt
---
# kubernetes/frontend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: treasure-hunt
spec:
  replicas: 3
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: treasurehunt.azurecr.io/frontend:latest
        ports:
        - containerPort: 3000
        env:
        - name: NODE_ENV
          value: "production"
        - name: API_GATEWAY_URL
          value: "http://api-gateway:8080"
        - name: NEXTAUTH_SECRET
          valueFrom:
            secretKeyRef:
              name: frontend-secrets
              key: nextauth-secret
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: frontend
  namespace: treasure-hunt
spec:
  selector:
    app: frontend
  ports:
  - port: 80
    targetPort: 3000
  type: ClusterIP
```

### **Docker Configuration**
```dockerfile
# Dockerfile
FROM node:18-alpine AS base

# Install dependencies only when needed
FROM base AS deps
RUN apk add --no-cache libc6-compat
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --only=production

# Rebuild the source code only when needed
FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

ENV NEXT_TELEMETRY_DISABLED 1

RUN npm run build

# Production image, copy all the files and run next
FROM base AS runner
WORKDIR /app

ENV NODE_ENV production
ENV NEXT_TELEMETRY_DISABLED 1

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public

# Set the correct permission for prerender cache
RUN mkdir .next
RUN chown nextjs:nodejs .next

# Automatically leverage output traces to reduce image size
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT 3000
ENV HOSTNAME "0.0.0.0"

CMD ["node", "server.js"]
```

### **Helm Chart Configuration**
```yaml
# helm/frontend/values.yaml
replicaCount: 3

image:
  repository: treasurehunt.azurecr.io/frontend
  pullPolicy: IfNotPresent
  tag: "latest"

service:
  type: ClusterIP
  port: 80
  targetPort: 3000

ingress:
  enabled: true
  className: "nginx"
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
  hosts:
    - host: treasurehunt.mysillydreams.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: frontend-tls
      hosts:
        - treasurehunt.mysillydreams.com

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80

nodeSelector: {}
tolerations: []
affinity: {}
```

### **CI/CD Pipeline with Azure DevOps**
```yaml
# azure-pipelines.yml
trigger:
  branches:
    include:
    - main
    - develop
  paths:
    include:
    - frontend/*

variables:
  dockerRegistryServiceConnection: 'treasurehunt-acr'
  imageRepository: 'frontend'
  containerRegistry: 'treasurehunt.azurecr.io'
  dockerfilePath: 'frontend/Dockerfile'
  tag: '$(Build.BuildId)'
  kubernetesServiceConnection: 'treasurehunt-aks'

stages:
- stage: Build
  displayName: Build and push stage
  jobs:
  - job: Build
    displayName: Build
    pool:
      vmImage: ubuntu-latest
    steps:
    - task: Docker@2
      displayName: Build and push an image to container registry
      inputs:
        command: buildAndPush
        repository: $(imageRepository)
        dockerfile: $(dockerfilePath)
        containerRegistry: $(dockerRegistryServiceConnection)
        tags: |
          $(tag)
          latest

- stage: Deploy
  displayName: Deploy stage
  dependsOn: Build
  jobs:
  - deployment: Deploy
    displayName: Deploy
    pool:
      vmImage: ubuntu-latest
    environment: 'production'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: HelmDeploy@0
            displayName: Helm upgrade
            inputs:
              connectionType: 'Kubernetes Service Connection'
              kubernetesServiceConnection: $(kubernetesServiceConnection)
              namespace: 'treasure-hunt'
              command: 'upgrade'
              chartType: 'FilePath'
              chartPath: 'helm/frontend'
              releaseName: 'frontend'
              valueFile: 'helm/frontend/values.yaml'
              arguments: '--set image.tag=$(tag)'
```

### **Environment Management**
- **Development**: AKS dev cluster with reduced resources
- **Staging**: AKS staging cluster with production-like setup
- **Production**: AKS production cluster with high availability
- **Monitoring**: Azure Monitor + Application Insights + Prometheus

---

## 📈 **Analytics & Monitoring**

### **Analytics Integration**
- **Google Analytics 4**: User behavior tracking
- **Vercel Analytics**: Performance monitoring
- **Hotjar**: User session recordings
- **Payment Analytics**: Conversion funnel analysis

### **Error Monitoring**
```typescript
// sentry.config.js
import * as Sentry from '@sentry/nextjs';

Sentry.init({
  dsn: process.env.SENTRY_DSN,
  tracesSampleRate: 1.0,
  integrations: [
    new Sentry.BrowserTracing({
      tracingOrigins: ['localhost', /^https:\/\/yourapp\.vercel\.app/]
    })
  ]
});
```

---

## 🎯 **Success Metrics**

### **Key Performance Indicators (KPIs)**
- **Conversion Rate**: Booking completion rate > 85%
- **Payment Success Rate**: > 98%
- **Page Load Speed**: < 2 seconds
- **Mobile Performance**: Lighthouse score > 90
- **User Retention**: 30-day retention > 60%
- **Customer Satisfaction**: NPS > 70

### **Business Metrics**
- **Revenue Growth**: Month-over-month growth
- **Average Order Value**: Increase through upselling
- **Customer Lifetime Value**: Long-term engagement
- **Refund Rate**: < 2% of total transactions

---

## 🎨 **Component Library & Design System**

### **Neumorphic Component Examples**

#### **Button Components**
```typescript
// components/ui/NeumorphicButton.tsx
interface NeumorphicButtonProps {
  variant: 'primary' | 'secondary' | 'ghost';
  size: 'sm' | 'md' | 'lg';
  pressed?: boolean;
  children: React.ReactNode;
}

export const NeumorphicButton: React.FC<NeumorphicButtonProps> = ({
  variant,
  size,
  pressed = false,
  children,
  ...props
}) => {
  const baseClasses = "rounded-2xl font-medium transition-all duration-200";
  const sizeClasses = {
    sm: "px-4 py-2 text-sm",
    md: "px-6 py-3 text-base",
    lg: "px-8 py-4 text-lg"
  };

  const variantClasses = {
    primary: pressed
      ? "bg-primary-100 shadow-inner-neumorphic text-primary-800"
      : "bg-primary-500 shadow-neumorphic text-white hover:shadow-neumorphic-hover",
    secondary: pressed
      ? "bg-gray-100 shadow-inner-neumorphic text-gray-800"
      : "bg-gray-200 shadow-neumorphic text-gray-700 hover:shadow-neumorphic-hover",
    ghost: "bg-transparent text-primary-600 hover:bg-primary-50"
  };

  return (
    <button
      className={`${baseClasses} ${sizeClasses[size]} ${variantClasses[variant]}`}
      {...props}
    >
      {children}
    </button>
  );
};
```

#### **Card Components**
```typescript
// components/ui/NeumorphicCard.tsx
export const NeumorphicCard: React.FC<{
  children: React.ReactNode;
  elevated?: boolean;
  interactive?: boolean;
}> = ({ children, elevated = false, interactive = false }) => {
  return (
    <div className={`
      rounded-3xl p-6 bg-white
      ${elevated ? 'shadow-neumorphic-elevated' : 'shadow-neumorphic'}
      ${interactive ? 'hover:shadow-neumorphic-hover cursor-pointer transition-shadow duration-300' : ''}
    `}>
      {children}
    </div>
  );
};
```

### **Custom Tailwind Configuration**
```javascript
// tailwind.config.js
module.exports = {
  content: ['./src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      boxShadow: {
        'neumorphic': '8px 8px 16px #d1d9e6, -8px -8px 16px #ffffff',
        'neumorphic-hover': '12px 12px 24px #d1d9e6, -12px -12px 24px #ffffff',
        'neumorphic-elevated': '16px 16px 32px #d1d9e6, -16px -16px 32px #ffffff',
        'inner-neumorphic': 'inset 8px 8px 16px #d1d9e6, inset -8px -8px 16px #ffffff'
      },
      colors: {
        primary: {
          50: '#e8f5e8',
          100: '#c8e6c9',
          200: '#a5d6a7',
          300: '#81c784',
          400: '#66bb6a',
          500: '#4caf50',
          600: '#43a047',
          700: '#388e3c',
          800: '#2e7d32',
          900: '#1b5e20'
        }
      }
    }
  }
};
```

---

## 🔄 **API Integration Patterns**

### **Service Layer Architecture**
```typescript
// services/api.service.ts
class ApiService {
  private baseURL: string;
  private authToken: string | null = null;

  constructor(baseURL: string) {
    this.baseURL = baseURL;
  }

  setAuthToken(token: string) {
    this.authToken = token;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`;
    const headers = {
      'Content-Type': 'application/json',
      ...(this.authToken && { Authorization: `Bearer ${this.authToken}` }),
      ...options.headers
    };

    const response = await fetch(url, { ...options, headers });

    if (!response.ok) {
      throw new ApiError(response.status, await response.text());
    }

    return response.json();
  }

  // Payment Service APIs
  async createPayment(data: PaymentRequest): Promise<PaymentResponse> {
    return this.request('/api/payments/v1/treasure/process-enrollment', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  async getPaymentMethods(): Promise<PaymentMethod[]> {
    return this.request('/api/payments/v1/methods');
  }

  async getTrustWidgets(): Promise<TrustWidgetConfig> {
    return this.request('/api/payments/v1/trust/config');
  }

  // User Service APIs
  async getUserProfile(userId: string): Promise<UserProfile> {
    return this.request(`/api/users/v1/profile/${userId}`);
  }

  async updateUserProfile(userId: string, data: ProfileUpdate): Promise<UserProfile> {
    return this.request(`/api/users/v1/profile/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }
}
```

### **React Query Hooks**
```typescript
// hooks/usePayments.ts
export const useCreatePayment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: PaymentRequest) => apiService.createPayment(data),
    onSuccess: (data) => {
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['bookings'] });

      // Show success notification
      toast.success('Payment processed successfully!');
    },
    onError: (error: ApiError) => {
      toast.error(`Payment failed: ${error.message}`);
    }
  });
};

export const usePaymentMethods = () => {
  return useQuery({
    queryKey: ['payment-methods'],
    queryFn: () => apiService.getPaymentMethods(),
    staleTime: 10 * 60 * 1000, // 10 minutes
    cacheTime: 30 * 60 * 1000  // 30 minutes
  });
};
```

---

## 🎭 **Advanced UI Components**

### **Payment Flow Components**

#### **Payment Method Selector**
```typescript
// components/payment/PaymentMethodSelector.tsx
export const PaymentMethodSelector = ({
  selected,
  onChange,
  amount
}: {
  selected: PaymentMethod;
  onChange: (method: PaymentMethod) => void;
  amount: number;
}) => {
  const { data: methods } = usePaymentMethods();
  const { data: emiOptions } = useEmiOptions(amount);

  return (
    <div className="payment-method-selector">
      <h3 className="text-lg font-semibold mb-4">Choose Payment Method</h3>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {methods?.map(method => (
          <NeumorphicCard
            key={method.type}
            interactive
            onClick={() => onChange(method.type)}
          >
            <div className={`text-center p-4 ${
              selected === method.type ? 'bg-primary-50 border-2 border-primary-500' : ''
            }`}>
              <img src={method.icon} alt={method.name} className="w-8 h-8 mx-auto mb-2" />
              <span className="text-sm font-medium">{method.name}</span>
            </div>
          </NeumorphicCard>
        ))}
      </div>

      {amount >= 3000 && (
        <div className="mt-6">
          <h4 className="text-md font-medium mb-3">EMI Options Available</h4>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {emiOptions?.map(option => (
              <div key={option.tenure} className="emi-option p-3 border rounded-lg">
                <div className="text-sm text-gray-600">{option.tenure} months</div>
                <div className="text-lg font-semibold">₹{option.emiAmount}/month</div>
                <div className="text-xs text-gray-500">{option.interestRate}% p.a.</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
```

#### **Trust Indicators Component**
```typescript
// components/trust/TrustIndicators.tsx
export const TrustIndicators = () => {
  const { data: trustConfig } = useTrustWidgets();

  return (
    <div className="trust-indicators py-8">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
        {trustConfig?.trustIndicators.map((indicator, index) => (
          <NeumorphicCard key={index}>
            <div className="text-center">
              <img
                src={indicator.icon}
                alt={indicator.title}
                className="w-12 h-12 mx-auto mb-3"
              />
              <div className="text-2xl font-bold text-primary-600 mb-1">
                {indicator.value}
              </div>
              <div className="text-sm font-medium text-gray-700 mb-1">
                {indicator.title}
              </div>
              <div className="text-xs text-gray-500">
                {indicator.description}
              </div>
            </div>
          </NeumorphicCard>
        ))}
      </div>
    </div>
  );
};
```

### **Advanced Form Components**

#### **Smart Form with Validation**
```typescript
// components/forms/SmartForm.tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const bookingSchema = z.object({
  huntId: z.string().uuid(),
  date: z.string().min(1, 'Date is required'),
  participants: z.number().min(1).max(20),
  contactName: z.string().min(2, 'Name must be at least 2 characters'),
  contactEmail: z.string().email('Invalid email address'),
  contactPhone: z.string().regex(/^[6-9]\d{9}$/, 'Invalid phone number'),
  specialRequests: z.string().optional()
});

type BookingFormData = z.infer<typeof bookingSchema>;

export const BookingForm = ({ onSubmit }: { onSubmit: (data: BookingFormData) => void }) => {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    watch
  } = useForm<BookingFormData>({
    resolver: zodResolver(bookingSchema)
  });

  const participants = watch('participants');

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <NeumorphicCard>
        <h3 className="text-lg font-semibold mb-4">Booking Details</h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            label="Date"
            type="date"
            {...register('date')}
            error={errors.date?.message}
          />

          <FormField
            label="Number of Participants"
            type="number"
            min={1}
            max={20}
            {...register('participants', { valueAsNumber: true })}
            error={errors.participants?.message}
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
          <FormField
            label="Contact Name"
            {...register('contactName')}
            error={errors.contactName?.message}
          />

          <FormField
            label="Email"
            type="email"
            {...register('contactEmail')}
            error={errors.contactEmail?.message}
          />
        </div>

        <FormField
          label="Phone Number"
          {...register('contactPhone')}
          error={errors.contactPhone?.message}
          className="mt-4"
        />

        <FormField
          label="Special Requests (Optional)"
          as="textarea"
          rows={3}
          {...register('specialRequests')}
          className="mt-4"
        />
      </NeumorphicCard>

      <NeumorphicButton
        type="submit"
        variant="primary"
        size="lg"
        disabled={isSubmitting}
        className="w-full"
      >
        {isSubmitting ? 'Processing...' : 'Proceed to Payment'}
      </NeumorphicButton>
    </form>
  );
};
```

---

## 📊 **Dashboard & Analytics Components**

### **User Dashboard Layout**
```typescript
// app/dashboard/layout.tsx
export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-gray-50">
      <DashboardHeader />
      <div className="flex">
        <DashboardSidebar />
        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
```

### **Analytics Dashboard**
```typescript
// components/dashboard/AnalyticsDashboard.tsx
export const AnalyticsDashboard = () => {
  const { data: analytics } = useAnalytics();

  return (
    <div className="analytics-dashboard space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <MetricCard
          title="Total Bookings"
          value={analytics?.totalBookings}
          change={analytics?.bookingsChange}
          icon="📅"
        />
        <MetricCard
          title="Revenue"
          value={`₹${analytics?.totalRevenue}`}
          change={analytics?.revenueChange}
          icon="💰"
        />
        <MetricCard
          title="Success Rate"
          value={`${analytics?.successRate}%`}
          change={analytics?.successRateChange}
          icon="✅"
        />
        <MetricCard
          title="Avg. Rating"
          value={analytics?.averageRating}
          change={analytics?.ratingChange}
          icon="⭐"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <NeumorphicCard>
          <h3 className="text-lg font-semibold mb-4">Booking Trends</h3>
          <BookingChart data={analytics?.bookingTrends} />
        </NeumorphicCard>

        <NeumorphicCard>
          <h3 className="text-lg font-semibold mb-4">Payment Methods</h3>
          <PaymentMethodChart data={analytics?.paymentMethods} />
        </NeumorphicCard>
      </div>
    </div>
  );
};
```

---

## 🔐 **Security Implementation**

### **Content Security Policy**
```typescript
// next.config.js
const securityHeaders = [
  {
    key: 'Content-Security-Policy',
    value: `
      default-src 'self';
      script-src 'self' 'unsafe-eval' 'unsafe-inline' checkout.razorpay.com;
      style-src 'self' 'unsafe-inline' fonts.googleapis.com;
      img-src 'self' data: https:;
      font-src 'self' fonts.gstatic.com;
      connect-src 'self' api.razorpay.com;
      frame-src checkout.razorpay.com;
    `.replace(/\s{2,}/g, ' ').trim()
  },
  {
    key: 'X-Frame-Options',
    value: 'DENY'
  },
  {
    key: 'X-Content-Type-Options',
    value: 'nosniff'
  },
  {
    key: 'Referrer-Policy',
    value: 'origin-when-cross-origin'
  }
];

module.exports = {
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: securityHeaders,
      },
    ];
  },
};
```

### **Input Sanitization**
```typescript
// utils/sanitization.ts
import DOMPurify from 'isomorphic-dompurify';

export const sanitizeInput = (input: string): string => {
  return DOMPurify.sanitize(input, { ALLOWED_TAGS: [] });
};

export const sanitizeHTML = (html: string): string => {
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'br'],
    ALLOWED_ATTR: ['href', 'target']
  });
};
```

---

## 🚀 **Deployment & Performance**

### **Build Optimization**
```typescript
// next.config.js
const nextConfig = {
  experimental: {
    optimizeCss: true,
    optimizeServerReact: true,
  },
  compiler: {
    removeConsole: process.env.NODE_ENV === 'production',
  },
  webpack: (config, { isServer }) => {
    if (!isServer) {
      config.resolve.fallback = {
        ...config.resolve.fallback,
        fs: false,
      };
    }
    return config;
  },
  async rewrites() {
    return [
      {
        source: '/api/payments/:path*',
        destination: `${process.env.PAYMENT_SERVICE_URL}/api/payments/:path*`,
      },
      {
        source: '/api/users/:path*',
        destination: `${process.env.USER_SERVICE_URL}/api/users/:path*`,
      },
    ];
  },
};
```

### **Performance Monitoring**
```typescript
// lib/performance.ts
export const reportWebVitals = (metric: any) => {
  if (process.env.NODE_ENV === 'production') {
    // Send to analytics
    gtag('event', metric.name, {
      event_category: 'Web Vitals',
      value: Math.round(metric.value),
      event_label: metric.id,
      non_interaction: true,
    });
  }
};

// pages/_app.tsx
export { reportWebVitals };
```

---

This comprehensive PRD provides the complete foundation for building a world-class treasure hunt platform with advanced payment capabilities, modern design, optimal performance, and enterprise-grade security.
