/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      // Neumorphism shadows - Using accent colors instead of pure white
      boxShadow: {
        'neumorphic': '8px 8px 16px #cbd5e1, -8px -8px 16px #f8fafc',
        'neumorphic-hover': '12px 12px 24px #cbd5e1, -12px -12px 24px #f8fafc',
        'neumorphic-elevated': '16px 16px 32px #cbd5e1, -16px -16px 32px #f8fafc',
        'neumorphic-pressed': 'inset 8px 8px 16px #cbd5e1, inset -8px -8px 16px #f8fafc',
        'neumorphic-soft': '4px 4px 8px #cbd5e1, -4px -4px 8px #f8fafc',
        // Dark mode shadows with better contrast
        'neumorphic-dark': '8px 8px 16px #1e2028, -8px -8px 16px #363a4a',
        'neumorphic-dark-hover': '12px 12px 24px #1e2028, -12px -12px 24px #363a4a',
        'neumorphic-dark-pressed': 'inset 8px 8px 16px #1e2028, inset -8px -8px 16px #363a4a',
        // Neon glow effects
        'neon-cyan': '0 0 20px rgba(0, 255, 255, 0.3)',
        'neon-purple': '0 0 20px rgba(139, 92, 246, 0.3)',
        'neon-pink': '0 0 20px rgba(236, 72, 153, 0.3)',
        // Context-aware shadows
        'neumorphic-on-primary': '8px 8px 16px #475569, -8px -8px 16px #94a3b8',
        'neumorphic-on-accent': '8px 8px 16px #0284c7, -8px -8px 16px #38bdf8',
      },
      
      // Neumorphic Color Palette - Subtle and Professional
      colors: {
        primary: {
          50: '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b', // Subtle slate blue
          600: '#475569',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
        },
        secondary: {
          50: '#fafafa',
          100: '#f4f4f5',
          200: '#e4e4e7',
          300: '#d4d4d8',
          400: '#a1a1aa',
          500: '#71717a', // Neutral zinc
          600: '#52525b',
          700: '#3f3f46',
          800: '#27272a',
          900: '#18181b',
        },
        accent: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          200: '#bae6fd',
          300: '#7dd3fc',
          400: '#38bdf8',
          500: '#0ea5e9', // Subtle cyan accent
          600: '#0284c7',
          700: '#0369a1',
          800: '#075985',
          900: '#0c4a6e',
        },
        // Neon accent colors for gradients and highlights
        neon: {
          cyan: '#00ffff',
          purple: '#8b5cf6',
          pink: '#ec4899',
          green: '#10b981',
          blue: '#3b82f6',
          orange: '#f59e0b',
        },
        neutral: {
          50: '#fafafa',
          100: '#f4f4f5',
          200: '#e5e5e5',
          300: '#d4d4d4',
          400: '#a3a3a3',
          500: '#737373',
          600: '#525252',
          700: '#404040',
          800: '#262626',
          900: '#171717',
        },
        // Neumorphic base colors
        neumorphic: {
          light: {
            bg: '#f0f0f3',
            shadow: {
              dark: '#d1d9e6',
              light: '#ffffff',
            },
          },
          dark: {
            bg: '#2a2d3a',
            shadow: {
              dark: '#1e2028',
              light: '#363a4a',
            },
          },
        },
        success: {
          50: '#e8f5e8',
          500: '#4caf50',
          600: '#43a047',
        },
        warning: {
          50: '#fff8e1',
          500: '#ffc107',
          600: '#ffb300',
        },
        error: {
          50: '#ffebee',
          500: '#f44336',
          600: '#e53935',
        },
        info: {
          50: '#e3f2fd',
          500: '#2196f3',
          600: '#1e88e5',
        },
      },
      
      // Typography
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        display: ['Poppins', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      
      fontSize: {
        'xs': ['0.75rem', { lineHeight: '1rem' }],
        'sm': ['0.875rem', { lineHeight: '1.25rem' }],
        'base': ['1rem', { lineHeight: '1.5rem' }],
        'lg': ['1.125rem', { lineHeight: '1.75rem' }],
        'xl': ['1.25rem', { lineHeight: '1.75rem' }],
        '2xl': ['1.5rem', { lineHeight: '2rem' }],
        '3xl': ['1.875rem', { lineHeight: '2.25rem' }],
        '4xl': ['2.25rem', { lineHeight: '2.5rem' }],
        '5xl': ['3rem', { lineHeight: '1' }],
        '6xl': ['3.75rem', { lineHeight: '1' }],
        '7xl': ['4.5rem', { lineHeight: '1' }],
        '8xl': ['6rem', { lineHeight: '1' }],
        '9xl': ['8rem', { lineHeight: '1' }],
      },
      
      // Spacing
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '128': '32rem',
      },
      
      // Border radius
      borderRadius: {
        'neumorphic': '1.5rem',
        'neumorphic-lg': '2rem',
        'neumorphic-xl': '2.5rem',
      },
      
      // Animation
      animation: {
        'fade-in': 'fadeIn 0.5s ease-in-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'slide-down': 'slideDown 0.3s ease-out',
        'scale-in': 'scaleIn 0.2s ease-out',
        'bounce-soft': 'bounceSoft 0.6s ease-in-out',
        'pulse-soft': 'pulseSoft 2s infinite',
      },
      
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
        slideDown: {
          '0%': { transform: 'translateY(-10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
        scaleIn: {
          '0%': { transform: 'scale(0.95)', opacity: '0' },
          '100%': { transform: 'scale(1)', opacity: '1' },
        },
        bounceSoft: {
          '0%, 20%, 53%, 80%, 100%': { transform: 'translate3d(0,0,0)' },
          '40%, 43%': { transform: 'translate3d(0,-8px,0)' },
          '70%': { transform: 'translate3d(0,-4px,0)' },
          '90%': { transform: 'translate3d(0,-2px,0)' },
        },
        pulseSoft: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.8' },
        },
      },
      
      // Backdrop blur
      backdropBlur: {
        'neumorphic': '8px',
      },
      
      // Gradients - Subtle neon gradients for neumorphic design
      backgroundImage: {
        'gradient-neumorphic': 'linear-gradient(145deg, #f0f0f3, #e1e5e9)',
        'gradient-neumorphic-dark': 'linear-gradient(145deg, #2a2d3a, #1e2028)',
        'gradient-primary': 'linear-gradient(135deg, #64748b, #475569)',
        'gradient-secondary': 'linear-gradient(135deg, #71717a, #52525b)',
        'gradient-accent': 'linear-gradient(135deg, #0ea5e9, #0284c7)',
        'gradient-hero': 'linear-gradient(135deg, #64748b 0%, #0ea5e9 50%, #8b5cf6 100%)',
        // Neon gradients
        'gradient-neon-cyan': 'linear-gradient(135deg, rgba(0, 255, 255, 0.1), rgba(0, 255, 255, 0.05))',
        'gradient-neon-purple': 'linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.05))',
        'gradient-neon-pink': 'linear-gradient(135deg, rgba(236, 72, 153, 0.1), rgba(236, 72, 153, 0.05))',
        'gradient-subtle': 'linear-gradient(135deg, #f8fafc, #f1f5f9)',
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/typography'),
    
    // Custom plugin for neumorphic utilities
    function({ addUtilities }) {
      const newUtilities = {
        '.neumorphic-flat': {
          background: '#f0f0f0',
          boxShadow: '8px 8px 16px #d1d9e6, -8px -8px 16px #ffffff',
        },
        '.neumorphic-pressed': {
          background: '#f0f0f0',
          boxShadow: 'inset 8px 8px 16px #d1d9e6, inset -8px -8px 16px #ffffff',
        },
        '.neumorphic-elevated': {
          background: '#f0f0f0',
          boxShadow: '16px 16px 32px #d1d9e6, -16px -16px 32px #ffffff',
        },
        '.text-gradient': {
          background: 'linear-gradient(135deg, #4caf50, #ff9800)',
          '-webkit-background-clip': 'text',
          '-webkit-text-fill-color': 'transparent',
          'background-clip': 'text',
        },
      };
      
      addUtilities(newUtilities);
    },
  ],
  darkMode: 'class',
};
