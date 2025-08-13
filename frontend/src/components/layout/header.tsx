'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { motion, AnimatePresence } from 'framer-motion';
import { Menu, X, Search, User, ShoppingBag, Phone, Bell } from 'lucide-react';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NotificationCenter } from '@/components/notifications/notification-center';
import { useNotifications } from '@/hooks/use-notifications';
import { DarkModeToggle } from '@/components/ui/dark-mode-toggle';

const navigation = [
  { name: 'Home', href: '/' },
  { name: 'Adventures', href: '/adventures' },
  { name: 'How It Works', href: '/how-it-works' },
  { name: 'About', href: '/about' },
  { name: 'Contact', href: '/contact' },
];

export function Header() {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);

  const { unreadCount } = useNotifications();

  // Handle scroll effect
  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Close mobile menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (isMobileMenuOpen && !(event.target as Element).closest('.mobile-menu')) {
        setIsMobileMenuOpen(false);
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, [isMobileMenuOpen]);

  // Prevent body scroll when mobile menu is open
  useEffect(() => {
    if (isMobileMenuOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }

    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isMobileMenuOpen]);

  return (
    <>
      <motion.header
        initial={{ y: -100 }}
        animate={{ y: 0 }}
        transition={{ duration: 0.6, ease: 'easeOut' }}
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          isScrolled
            ? 'bg-white/95 backdrop-blur-md shadow-neumorphic border-b border-neutral-200/50'
            : 'bg-transparent'
        }`}
      >
        <nav className="container mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16 lg:h-20">
            {/* Logo */}
            <Link href="/" className="flex items-center space-x-3 group">
              <div className="relative w-10 h-10 lg:w-12 lg:h-12">
                <div className="absolute inset-0 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-xl shadow-neumorphic group-hover:shadow-neumorphic-hover transition-all duration-300" />
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-white font-bold text-lg lg:text-xl">TH</span>
                </div>
              </div>
              <div className="hidden sm:block">
                <div className={`font-display font-bold text-xl lg:text-2xl transition-colors duration-300 ${
                  isScrolled ? 'text-neutral-800' : 'text-white'
                }`}>
                  Treasure Hunt
                </div>
                <div className={`text-xs lg:text-sm transition-colors duration-300 ${
                  isScrolled ? 'text-neutral-600' : 'text-white/80'
                }`}>
                  MySillyDreams
                </div>
              </div>
            </Link>

            {/* Desktop Navigation */}
            <div className="hidden lg:flex items-center space-x-8">
              {navigation.map((item) => (
                <Link
                  key={item.name}
                  href={item.href}
                  className={`font-medium transition-colors duration-300 hover:text-primary-500 ${
                    isScrolled ? 'text-neutral-700' : 'text-white/90'
                  }`}
                >
                  {item.name}
                </Link>
              ))}
            </div>

            {/* Desktop Actions */}
            <div className="hidden lg:flex items-center space-x-4">
              {/* Search Button */}
              <button
                onClick={() => setIsSearchOpen(true)}
                className={`p-2 rounded-full transition-all duration-300 ${
                  isScrolled
                    ? 'text-neutral-600 hover:bg-neutral-100'
                    : 'text-white/80 hover:bg-white/10'
                }`}
              >
                <Search className="w-5 h-5" />
              </button>

              {/* Notifications Button */}
              <button
                onClick={() => setIsNotificationsOpen(true)}
                className={`relative p-2 rounded-full transition-all duration-300 ${
                  isScrolled
                    ? 'text-neutral-600 hover:bg-neutral-100'
                    : 'text-white/80 hover:bg-white/10'
                }`}
              >
                <Bell className="w-5 h-5" />
                {unreadCount > 0 && (
                  <span className="absolute -top-1 -right-1 w-5 h-5 bg-error-500 text-white text-xs rounded-full flex items-center justify-center">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </button>

              {/* Contact Button */}
              <NeumorphicButton
                variant="ghost"
                size="sm"
                className={`${
                  isScrolled
                    ? 'text-neutral-700 border-neutral-300 hover:bg-neutral-50'
                    : 'text-white border-white/30 hover:bg-white/10'
                }`}
              >
                <Phone className="w-4 h-4 mr-2" />
                Call Us
              </NeumorphicButton>

              {/* Login Button */}
              <NeumorphicButton
                variant="ghost"
                size="sm"
                className={`${
                  isScrolled
                    ? 'text-neutral-700 border-neutral-300 hover:bg-neutral-50'
                    : 'text-white border-white/30 hover:bg-white/10'
                }`}
              >
                <User className="w-4 h-4 mr-2" />
                Login
              </NeumorphicButton>

              {/* Book Now Button */}
              <NeumorphicButton variant="primary" size="sm">
                Book Now
              </NeumorphicButton>

              {/* Dark Mode Toggle - Better positioned */}
              <div className="ml-3 pl-3 border-l border-white/20 dark:border-neutral-600">
                <DarkModeToggle />
              </div>
            </div>

            {/* Mobile Menu Button */}
            <div className="lg:hidden flex items-center space-x-2">
              <button
                onClick={() => setIsSearchOpen(true)}
                className={`p-2 rounded-full transition-all duration-300 ${
                  isScrolled
                    ? 'text-neutral-600 hover:bg-neutral-100'
                    : 'text-white/80 hover:bg-white/10'
                }`}
              >
                <Search className="w-5 h-5" />
              </button>



              <button
                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                className={`p-2 rounded-full transition-all duration-300 mobile-menu ${
                  isScrolled
                    ? 'text-neutral-600 hover:bg-neutral-100'
                    : 'text-white/80 hover:bg-white/10'
                }`}
              >
                {isMobileMenuOpen ? (
                  <X className="w-6 h-6" />
                ) : (
                  <Menu className="w-6 h-6" />
                )}
              </button>
            </div>
          </div>
        </nav>
      </motion.header>

      {/* Mobile Menu */}
      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="fixed inset-0 z-40 lg:hidden"
          >
            {/* Backdrop */}
            <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />
            
            {/* Menu Panel */}
            <motion.div
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ duration: 0.3, ease: 'easeInOut' }}
              className="absolute right-0 top-0 h-full w-80 max-w-[90vw] mobile-menu"
            >
              <NeumorphicCard className="h-full rounded-none rounded-l-3xl bg-white shadow-neumorphic-elevated">
                <div className="p-6 space-y-6">
                  {/* Header */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <div className="relative w-10 h-10">
                        <div className="absolute inset-0 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-xl shadow-neumorphic" />
                        <div className="absolute inset-0 flex items-center justify-center">
                          <span className="text-white font-bold text-lg">TH</span>
                        </div>
                      </div>
                      <div>
                        <div className="font-display font-bold text-lg text-neutral-800">
                          Treasure Hunt
                        </div>
                        <div className="text-xs text-neutral-600">
                          MySillyDreams
                        </div>
                      </div>
                    </div>
                    <button
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="p-2 text-neutral-600 hover:bg-neutral-100 rounded-full transition-colors duration-200"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  </div>

                  {/* Navigation Links */}
                  <nav className="space-y-2">
                    {navigation.map((item) => (
                      <Link
                        key={item.name}
                        href={item.href}
                        onClick={() => setIsMobileMenuOpen(false)}
                        className="block px-4 py-3 text-lg font-medium text-neutral-700 hover:bg-neutral-50 hover:text-primary-600 rounded-xl transition-all duration-200"
                      >
                        {item.name}
                      </Link>
                    ))}
                  </nav>

                  {/* Action Buttons */}
                  <div className="space-y-3 pt-6 border-t border-neutral-200">
                    <NeumorphicButton
                      variant="ghost"
                      size="lg"
                      className="w-full justify-start"
                    >
                      <Phone className="w-5 h-5 mr-3" />
                      Call Us: +91-9876543210
                    </NeumorphicButton>
                    
                    <NeumorphicButton
                      variant="ghost"
                      size="lg"
                      className="w-full justify-start"
                    >
                      <User className="w-5 h-5 mr-3" />
                      Login / Sign Up
                    </NeumorphicButton>
                    
                    <NeumorphicButton
                      variant="primary"
                      size="lg"
                      className="w-full"
                    >
                      Book Your Adventure
                    </NeumorphicButton>
                  </div>

                  {/* Contact Info */}
                  <div className="pt-6 border-t border-neutral-200 text-center text-sm text-neutral-600">
                    <p>Need help? We're here 24/7</p>
                    <p className="font-medium text-primary-600">hello@mysillydreams.com</p>
                  </div>
                </div>
              </NeumorphicCard>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Search Modal */}
      <AnimatePresence>
        {isSearchOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="fixed inset-0 z-50 flex items-start justify-center pt-20 px-4"
          >
            {/* Backdrop */}
            <div 
              className="absolute inset-0 bg-black/50 backdrop-blur-sm"
              onClick={() => setIsSearchOpen(false)}
            />
            
            {/* Search Panel */}
            <motion.div
              initial={{ y: -50, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: -50, opacity: 0 }}
              transition={{ duration: 0.3 }}
              className="relative w-full max-w-2xl"
            >
              <NeumorphicCard className="bg-white shadow-neumorphic-elevated">
                <div className="p-6">
                  <div className="flex items-center gap-4">
                    <Search className="w-6 h-6 text-neutral-400" />
                    <input
                      type="text"
                      placeholder="Search for adventures, cities, or experiences..."
                      className="flex-1 text-lg bg-transparent border-none outline-none text-neutral-800 placeholder-neutral-400"
                      autoFocus
                    />
                    <button
                      onClick={() => setIsSearchOpen(false)}
                      className="p-2 text-neutral-400 hover:text-neutral-600 transition-colors duration-200"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  </div>
                  
                  {/* Quick Suggestions */}
                  <div className="mt-6 space-y-2">
                    <p className="text-sm font-medium text-neutral-600">Popular Searches</p>
                    <div className="flex flex-wrap gap-2">
                      {['Mumbai Heritage', 'Delhi Food Trail', 'Bangalore Tech Hunt', 'Goa Beach Adventure'].map((suggestion) => (
                        <button
                          key={suggestion}
                          className="px-3 py-1 bg-neutral-100 text-neutral-700 rounded-full text-sm hover:bg-neutral-200 transition-colors duration-200"
                        >
                          {suggestion}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              </NeumorphicCard>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Notification Center */}
      <NotificationCenter
        isOpen={isNotificationsOpen}
        onClose={() => setIsNotificationsOpen(false)}
      />
    </>
  );
}
