'use client';

import Link from 'next/link';
import Image from 'next/image';
import { motion } from 'framer-motion';
import { 
  MapPin, 
  Phone, 
  Mail, 
  Facebook, 
  Twitter, 
  Instagram, 
  Youtube,
  Shield,
  Award,
  Heart
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';

const footerSections = [
  {
    title: 'Adventures',
    links: [
      { name: 'Mumbai Heritage Hunt', href: '/adventures/mumbai-heritage' },
      { name: 'Delhi Food Trail', href: '/adventures/delhi-food' },
      { name: 'Bangalore Tech Hunt', href: '/adventures/bangalore-tech' },
      { name: 'Goa Beach Adventure', href: '/adventures/goa-beach' },
      { name: 'All Adventures', href: '/adventures' },
    ]
  },
  {
    title: 'Company',
    links: [
      { name: 'About Us', href: '/about' },
      { name: 'How It Works', href: '/how-it-works' },
      { name: 'Our Team', href: '/team' },
      { name: 'Careers', href: '/careers' },
      { name: 'Press', href: '/press' },
    ]
  },
  {
    title: 'Support',
    links: [
      { name: 'Help Center', href: '/help' },
      { name: 'Contact Us', href: '/contact' },
      { name: 'Safety Guidelines', href: '/safety' },
      { name: 'Booking Policy', href: '/booking-policy' },
      { name: 'Refund Policy', href: '/refund-policy' },
    ]
  },
  {
    title: 'Legal',
    links: [
      { name: 'Privacy Policy', href: '/privacy' },
      { name: 'Terms of Service', href: '/terms' },
      { name: 'Cookie Policy', href: '/cookies' },
      { name: 'Data Protection', href: '/data-protection' },
      { name: 'Accessibility', href: '/accessibility' },
    ]
  }
];

const socialLinks = [
  { name: 'Facebook', icon: Facebook, href: 'https://facebook.com/mysillydreams' },
  { name: 'Twitter', icon: Twitter, href: 'https://twitter.com/mysillydreams' },
  { name: 'Instagram', icon: Instagram, href: 'https://instagram.com/mysillydreams' },
  { name: 'YouTube', icon: Youtube, href: 'https://youtube.com/mysillydreams' },
];

const trustBadges = [
  {
    name: 'Secured by Razorpay',
    image: '/images/badges/razorpay-badge.svg',
    alt: 'Secured by Razorpay'
  },
  {
    name: 'PCI DSS Compliant',
    image: '/images/badges/pci-badge.svg',
    alt: 'PCI DSS Compliant'
  },
  {
    name: 'ISO 27001 Certified',
    image: '/images/badges/iso-badge.svg',
    alt: 'ISO 27001 Certified'
  }
];

export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-neutral-900 text-white">
      {/* Main Footer Content */}
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12">
          {/* Company Info */}
          <div className="lg:col-span-4 space-y-6">
            {/* Logo and Description */}
            <div>
              <Link href="/" className="flex items-center space-x-3 mb-4">
                <div className="relative w-12 h-12">
                  <div className="absolute inset-0 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-xl shadow-neumorphic" />
                  <div className="absolute inset-0 flex items-center justify-center">
                    <span className="text-white font-bold text-xl">DR</span>
                  </div>
                </div>
                <div>
                  <div className="font-display font-bold text-2xl text-white">
                    Dream Rider
                  </div>
                  <div className="text-sm text-neutral-400">
                    MySillyDreams
                  </div>
                </div>
              </Link>
              
              <p className="text-neutral-300 leading-relaxed">
                Creating unforgettable treasure hunt experiences across India. 
                We bring people together through adventure, discovery, and fun.
              </p>
            </div>

            {/* Contact Info */}
            <div className="space-y-3">
              <div className="flex items-center gap-3 text-neutral-300">
                <MapPin className="w-5 h-5 text-primary-400" />
                <span>Mumbai, Delhi, Bangalore & 47 more cities</span>
              </div>
              <div className="flex items-center gap-3 text-neutral-300">
                <Phone className="w-5 h-5 text-primary-400" />
                <a href="tel:+919876543210" className="hover:text-primary-400 transition-colors duration-200">
                  +91-9876543210
                </a>
              </div>
              <div className="flex items-center gap-3 text-neutral-300">
                <Mail className="w-5 h-5 text-primary-400" />
                <a href="mailto:hello@mysillydreams.com" className="hover:text-primary-400 transition-colors duration-200">
                  hello@mysillydreams.com
                </a>
              </div>
            </div>

            {/* Social Links */}
            <div className="flex items-center gap-4">
              {socialLinks.map((social) => (
                <motion.a
                  key={social.name}
                  href={social.href}
                  target="_blank"
                  rel="noopener noreferrer"
                  whileHover={{ scale: 1.1 }}
                  whileTap={{ scale: 0.95 }}
                  className="p-3 bg-neutral-800 rounded-full hover:bg-primary-500 transition-all duration-300 group"
                >
                  <social.icon className="w-5 h-5 text-neutral-400 group-hover:text-white transition-colors duration-300" />
                </motion.a>
              ))}
            </div>
          </div>

          {/* Footer Links */}
          <div className="lg:col-span-8 grid grid-cols-2 md:grid-cols-4 gap-8">
            {footerSections.map((section) => (
              <div key={section.title}>
                <h3 className="font-semibold text-lg text-white mb-4">
                  {section.title}
                </h3>
                <ul className="space-y-3">
                  {section.links.map((link) => (
                    <li key={link.name}>
                      <Link
                        href={link.href}
                        className="text-neutral-400 hover:text-primary-400 transition-colors duration-200"
                      >
                        {link.name}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Trust Badges Section */}
      <div className="border-t border-neutral-800">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="text-center md:text-left">
              <h4 className="font-semibold text-white mb-2 flex items-center gap-2">
                <Shield className="w-5 h-5 text-primary-400" />
                Trusted & Secure
              </h4>
              <p className="text-neutral-400 text-sm">
                Your payments and data are protected by industry-leading security standards
              </p>
            </div>
            
            <div className="flex items-center gap-6">
              {trustBadges.map((badge) => (
                <div
                  key={badge.name}
                  className="flex items-center gap-2 px-3 py-2 bg-neutral-800 rounded-lg"
                >
                  <Shield className="w-4 h-4 text-primary-400" />
                  <span className="text-sm text-neutral-300">{badge.name}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Awards & Recognition */}
      <div className="border-t border-neutral-800">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex flex-col md:flex-row items-center justify-center gap-8 text-center">
            <div className="flex items-center gap-2 text-neutral-400">
              <Award className="w-5 h-5 text-secondary-400" />
              <span className="text-sm">Best Adventure Experience 2024</span>
            </div>
            <div className="flex items-center gap-2 text-neutral-400">
              <Heart className="w-5 h-5 text-error-400" />
              <span className="text-sm">10,000+ Happy Customers</span>
            </div>
            <div className="flex items-center gap-2 text-neutral-400">
              <MapPin className="w-5 h-5 text-accent-400" />
              <span className="text-sm">Available in 50+ Cities</span>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Bar */}
      <div className="border-t border-neutral-800">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="text-neutral-400 text-sm text-center md:text-left">
              <p>
                © {currentYear} MySillyDreams. All rights reserved. 
                <span className="mx-2">|</span>
                Made with <Heart className="w-4 h-4 inline text-error-400" /> in India
              </p>
            </div>
            
            <div className="flex items-center gap-6 text-sm text-neutral-400">
              <Link href="/sitemap" className="hover:text-primary-400 transition-colors duration-200">
                Sitemap
              </Link>
              <Link href="/status" className="hover:text-primary-400 transition-colors duration-200">
                System Status
              </Link>
              <div className="flex items-center gap-1">
                <div className="w-2 h-2 bg-success-500 rounded-full animate-pulse" />
                <span>All systems operational</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}
