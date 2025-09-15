'use client';

import { motion } from 'framer-motion';
import Image from 'next/image';
import Link from 'next/link';
import { 
  Users, 
  Target, 
  Heart, 
  Award, 
  MapPin, 
  Calendar,
  Star,
  ArrowRight,
  CheckCircle,
  Globe,
  Shield,
  Zap
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';

export default function AboutPage() {
  const stats = [
    { icon: Users, value: '10,000+', label: 'Happy Customers' },
    { icon: MapPin, value: '50+', label: 'Cities Covered' },
    { icon: Calendar, value: '500+', label: 'Adventures Completed' },
    { icon: Award, value: '4.9', label: 'Average Rating' },
  ];

  const values = [
    {
      icon: Heart,
      title: 'Passion for Adventure',
      description: 'We believe every journey should be filled with excitement, discovery, and unforgettable moments.'
    },
    {
      icon: Users,
      title: 'Community First',
      description: 'Building connections and bringing people together through shared experiences and teamwork.'
    },
    {
      icon: Shield,
      title: 'Safety & Trust',
      description: 'Your safety is our priority. We ensure secure, well-planned adventures with expert guidance.'
    },
    {
      icon: Globe,
      title: 'Cultural Heritage',
      description: 'Celebrating India\'s rich culture and history through immersive, educational experiences.'
    },
  ];

  const team = [
    {
      name: 'Arjun Sharma',
      role: 'Founder & CEO',
      image: '/images/team/arjun.jpg',
      description: 'Adventure enthusiast with 10+ years in tourism industry'
    },
    {
      name: 'Priya Patel',
      role: 'Head of Operations',
      image: '/images/team/priya.jpg',
      description: 'Expert in logistics and customer experience management'
    },
    {
      name: 'Rahul Kumar',
      role: 'Lead Adventure Designer',
      image: '/images/team/rahul.jpg',
      description: 'Creative mind behind our most popular treasure hunts'
    },
    {
      name: 'Sneha Gupta',
      role: 'Customer Success Manager',
      image: '/images/team/sneha.jpg',
      description: 'Ensuring every customer has an amazing experience'
    },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-neumorphic-light-bg via-neumorphic-light-bg to-primary-50">
      {/* Hero Section */}
      <section className="relative py-20 overflow-hidden">
        <div className="container mx-auto px-4">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="text-center max-w-4xl mx-auto"
          >
            <h1 className="text-4xl md:text-6xl font-bold text-neutral-800 mb-6">
              About{' '}
              <span className="bg-gradient-to-r from-primary-600 to-primary-500 bg-clip-text text-transparent">
                Dream Rider
              </span>
            </h1>
            <p className="text-xl text-neutral-600 mb-8 leading-relaxed">
              We're passionate about creating unforgettable treasure hunt experiences that bring people together, 
              celebrate India's rich heritage, and turn ordinary days into extraordinary adventures.
            </p>
            <div className="flex flex-wrap justify-center gap-4">
              <Link href="/adventures">
                <NeumorphicButton variant="primary" size="lg">
                  Explore Adventures
                  <ArrowRight className="ml-2 h-5 w-5" />
                </NeumorphicButton>
              </Link>
              <Link href="/contact">
                <NeumorphicButton variant="ghost" size="lg">
                  Get in Touch
                </NeumorphicButton>
              </Link>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-16 bg-white/50">
        <div className="container mx-auto px-4">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {stats.map((stat, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
                className="text-center"
              >
                <NeumorphicCard className="p-6">
                  <div className="flex items-center justify-center w-12 h-12 bg-primary-100 rounded-full mx-auto mb-4">
                    <stat.icon className="w-6 h-6 text-primary-600" />
                  </div>
                  <div className="text-3xl font-bold text-neutral-800 mb-2">{stat.value}</div>
                  <div className="text-neutral-600">{stat.label}</div>
                </NeumorphicCard>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Our Story Section */}
      <section className="py-20">
        <div className="container mx-auto px-4">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.8 }}
            >
              <h2 className="text-3xl md:text-4xl font-bold text-neutral-800 mb-6">
                Our Story
              </h2>
              <div className="space-y-4 text-neutral-600 leading-relaxed">
                <p>
                  Dream Rider was born from a simple idea: what if exploring your city could be as exciting 
                  as discovering a new country? Founded in 2020 by a group of adventure enthusiasts, we set 
                  out to transform the way people experience their surroundings.
                </p>
                <p>
                  Starting with just three treasure hunts in Mumbai, we've grown to cover over 50 cities 
                  across India. Our team of local experts, historians, and adventure designers work together 
                  to create experiences that are not just fun, but educational and meaningful.
                </p>
                <p>
                  Today, we're proud to have helped over 10,000 people discover hidden gems in their cities, 
                  build stronger teams, and create lasting memories with friends and family.
                </p>
              </div>
            </motion.div>
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.8, delay: 0.2 }}
              className="relative"
            >
              <NeumorphicCard className="p-8">
                <div className="aspect-video bg-gradient-to-br from-primary-100 to-primary-200 rounded-lg flex items-center justify-center">
                  <div className="text-center">
                    <div className="w-16 h-16 bg-primary-500 rounded-full flex items-center justify-center mx-auto mb-4">
                      <Heart className="w-8 h-8 text-white" />
                    </div>
                    <h3 className="text-xl font-semibold text-neutral-800 mb-2">Our Mission</h3>
                    <p className="text-neutral-600">
                      To make every adventure meaningful, memorable, and magical.
                    </p>
                  </div>
                </div>
              </NeumorphicCard>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Values Section */}
      <section className="py-20 bg-white/50">
        <div className="container mx-auto px-4">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="text-center mb-16"
          >
            <h2 className="text-3xl md:text-4xl font-bold text-neutral-800 mb-4">
              Our Values
            </h2>
            <p className="text-xl text-neutral-600 max-w-3xl mx-auto">
              These core values guide everything we do, from designing adventures to serving our customers.
            </p>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {values.map((value, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
              >
                <NeumorphicCard className="p-6 h-full">
                  <div className="flex items-center justify-center w-12 h-12 bg-primary-100 rounded-full mb-4">
                    <value.icon className="w-6 h-6 text-primary-600" />
                  </div>
                  <h3 className="text-xl font-semibold text-neutral-800 mb-3">{value.title}</h3>
                  <p className="text-neutral-600">{value.description}</p>
                </NeumorphicCard>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Team Section */}
      <section className="py-20">
        <div className="container mx-auto px-4">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="text-center mb-16"
          >
            <h2 className="text-3xl md:text-4xl font-bold text-neutral-800 mb-4">
              Meet Our Team
            </h2>
            <p className="text-xl text-neutral-600 max-w-3xl mx-auto">
              The passionate individuals behind every amazing adventure experience.
            </p>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {team.map((member, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
              >
                <NeumorphicCard className="p-6 text-center">
                  <div className="w-24 h-24 bg-gradient-to-br from-primary-100 to-primary-200 rounded-full mx-auto mb-4 flex items-center justify-center">
                    <Users className="w-12 h-12 text-primary-600" />
                  </div>
                  <h3 className="text-xl font-semibold text-neutral-800 mb-1">{member.name}</h3>
                  <p className="text-primary-600 font-medium mb-3">{member.role}</p>
                  <p className="text-neutral-600 text-sm">{member.description}</p>
                </NeumorphicCard>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-gradient-to-r from-primary-500 to-primary-600">
        <div className="container mx-auto px-4 text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="max-w-3xl mx-auto"
          >
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-6">
              Ready to Start Your Adventure?
            </h2>
            <p className="text-xl text-primary-100 mb-8">
              Join thousands of adventurers who have discovered the magic of treasure hunting with Dream Rider.
            </p>
            <div className="flex flex-wrap justify-center gap-4">
              <Link href="/adventures">
                <NeumorphicButton variant="secondary" size="lg">
                  Browse Adventures
                  <ArrowRight className="ml-2 h-5 w-5" />
                </NeumorphicButton>
              </Link>
              <Link href="/contact">
                <NeumorphicButton variant="ghost" size="lg" className="text-white border-white hover:bg-white/10">
                  Contact Us
                </NeumorphicButton>
              </Link>
            </div>
          </motion.div>
        </div>
      </section>
    </div>
  );
}
