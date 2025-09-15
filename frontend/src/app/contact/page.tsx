'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import Link from 'next/link';
import { 
  Mail, 
  Phone, 
  MapPin, 
  Clock, 
  Send,
  MessageCircle,
  Users,
  Calendar,
  ArrowRight,
  CheckCircle,
  AlertCircle
} from 'lucide-react';
import { NeumorphicCard } from '@/components/ui/neumorphic-card';
import { NeumorphicButton } from '@/components/ui/neumorphic-button';

interface ContactForm {
  name: string;
  email: string;
  phone: string;
  subject: string;
  message: string;
  inquiryType: 'general' | 'booking' | 'corporate' | 'support';
}

export default function ContactPage() {
  const [form, setForm] = useState<ContactForm>({
    name: '',
    email: '',
    phone: '',
    subject: '',
    message: '',
    inquiryType: 'general'
  });
  const [isLoading, setIsLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const contactInfo = [
    {
      icon: Phone,
      title: 'Phone',
      details: ['+91-9876543210', '+91-9876543211'],
      description: 'Call us for immediate assistance'
    },
    {
      icon: Mail,
      title: 'Email',
      details: ['hello@mysillydreams.com', 'support@mysillydreams.com'],
      description: 'Send us your questions anytime'
    },
    {
      icon: MapPin,
      title: 'Office',
      details: ['123 Adventure Street', 'Mumbai, Maharashtra 400001'],
      description: 'Visit our headquarters'
    },
    {
      icon: Clock,
      title: 'Hours',
      details: ['Mon - Fri: 9:00 AM - 7:00 PM', 'Sat - Sun: 10:00 AM - 6:00 PM'],
      description: 'We\'re here to help'
    }
  ];

  const inquiryTypes = [
    { value: 'general', label: 'General Inquiry', icon: MessageCircle },
    { value: 'booking', label: 'Booking Support', icon: Calendar },
    { value: 'corporate', label: 'Corporate Events', icon: Users },
    { value: 'support', label: 'Technical Support', icon: Phone }
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    // Basic validation
    if (!form.name || !form.email || !form.message) {
      setError('Please fill in all required fields');
      setIsLoading(false);
      return;
    }

    if (!/\S+@\S+\.\S+/.test(form.email)) {
      setError('Please enter a valid email address');
      setIsLoading(false);
      return;
    }

    try {
      // TODO: Implement contact form submission to backend
      // const response = await fetch('/api/contact', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(form)
      // });

      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      setSuccess(true);
      setForm({
        name: '',
        email: '',
        phone: '',
        subject: '',
        message: '',
        inquiryType: 'general'
      });
    } catch (err) {
      setError('Failed to send message. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleInputChange = (field: keyof ContactForm, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }));
    if (error) setError(null);
  };

  if (success) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-neumorphic-light-bg via-neumorphic-light-bg to-primary-50 flex items-center justify-center p-4">
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          className="text-center max-w-md mx-auto"
        >
          <NeumorphicCard className="p-8">
            <div className="flex items-center justify-center w-16 h-16 bg-success-100 rounded-full mx-auto mb-4">
              <CheckCircle className="w-8 h-8 text-success-600" />
            </div>
            <h2 className="text-2xl font-bold text-neutral-800 mb-2">Message Sent!</h2>
            <p className="text-neutral-600 mb-6">
              Thank you for contacting us. We'll get back to you within 24 hours.
            </p>
            <div className="space-y-3">
              <NeumorphicButton
                variant="primary"
                onClick={() => setSuccess(false)}
                className="w-full"
              >
                Send Another Message
              </NeumorphicButton>
              <Link href="/">
                <NeumorphicButton variant="ghost" className="w-full">
                  Back to Home
                </NeumorphicButton>
              </Link>
            </div>
          </NeumorphicCard>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-neumorphic-light-bg via-neumorphic-light-bg to-primary-50">
      {/* Hero Section */}
      <section className="py-20">
        <div className="container mx-auto px-4">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="text-center max-w-3xl mx-auto mb-16"
          >
            <h1 className="text-4xl md:text-6xl font-bold text-neutral-800 mb-6">
              Get in{' '}
              <span className="bg-gradient-to-r from-primary-600 to-primary-500 bg-clip-text text-transparent">
                Touch
              </span>
            </h1>
            <p className="text-xl text-neutral-600 leading-relaxed">
              Have questions about our adventures? Need help with booking? Want to plan a corporate event? 
              We're here to help make your treasure hunting dreams come true.
            </p>
          </motion.div>

          {/* Contact Info Cards */}
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-16">
            {contactInfo.map((info, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
              >
                <NeumorphicCard className="p-6 text-center h-full">
                  <div className="flex items-center justify-center w-12 h-12 bg-primary-100 rounded-full mx-auto mb-4">
                    <info.icon className="w-6 h-6 text-primary-600" />
                  </div>
                  <h3 className="text-lg font-semibold text-neutral-800 mb-2">{info.title}</h3>
                  <div className="space-y-1 mb-3">
                    {info.details.map((detail, idx) => (
                      <p key={idx} className="text-neutral-700 font-medium">{detail}</p>
                    ))}
                  </div>
                  <p className="text-neutral-600 text-sm">{info.description}</p>
                </NeumorphicCard>
              </motion.div>
            ))}
          </div>

          {/* Contact Form */}
          <div className="grid lg:grid-cols-2 gap-12">
            {/* Form */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.8 }}
            >
              <NeumorphicCard className="p-8">
                <h2 className="text-2xl font-bold text-neutral-800 mb-6">Send us a Message</h2>
                
                <form onSubmit={handleSubmit} className="space-y-6">
                  {/* Error Message */}
                  {error && (
                    <motion.div
                      initial={{ opacity: 0, y: -10 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="flex items-center space-x-2 p-3 bg-red-50 border border-red-200 rounded-lg"
                    >
                      <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0" />
                      <span className="text-red-700 text-sm">{error}</span>
                    </motion.div>
                  )}

                  {/* Inquiry Type */}
                  <div className="space-y-2">
                    <label className="block text-sm font-medium text-neutral-700">
                      What can we help you with? *
                    </label>
                    <div className="grid grid-cols-2 gap-2">
                      {inquiryTypes.map((type) => (
                        <label key={type.value} className="flex items-center cursor-pointer">
                          <input
                            type="radio"
                            name="inquiryType"
                            value={type.value}
                            checked={form.inquiryType === type.value}
                            onChange={(e) => handleInputChange('inquiryType', e.target.value)}
                            className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-neutral-300"
                          />
                          <div className="ml-2 flex items-center">
                            <type.icon className="w-4 h-4 text-neutral-500 mr-1" />
                            <span className="text-sm text-neutral-700">{type.label}</span>
                          </div>
                        </label>
                      ))}
                    </div>
                  </div>

                  {/* Name and Email */}
                  <div className="grid md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <label htmlFor="name" className="block text-sm font-medium text-neutral-700">
                        Full Name *
                      </label>
                      <input
                        id="name"
                        type="text"
                        value={form.name}
                        onChange={(e) => handleInputChange('name', e.target.value)}
                        className="block w-full px-3 py-3 border border-neutral-200 rounded-lg bg-white/50 backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200"
                        placeholder="Your full name"
                        required
                      />
                    </div>
                    <div className="space-y-2">
                      <label htmlFor="email" className="block text-sm font-medium text-neutral-700">
                        Email Address *
                      </label>
                      <input
                        id="email"
                        type="email"
                        value={form.email}
                        onChange={(e) => handleInputChange('email', e.target.value)}
                        className="block w-full px-3 py-3 border border-neutral-200 rounded-lg bg-white/50 backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200"
                        placeholder="your@email.com"
                        required
                      />
                    </div>
                  </div>

                  {/* Phone and Subject */}
                  <div className="grid md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <label htmlFor="phone" className="block text-sm font-medium text-neutral-700">
                        Phone Number
                      </label>
                      <input
                        id="phone"
                        type="tel"
                        value={form.phone}
                        onChange={(e) => handleInputChange('phone', e.target.value)}
                        className="block w-full px-3 py-3 border border-neutral-200 rounded-lg bg-white/50 backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200"
                        placeholder="+91 9876543210"
                      />
                    </div>
                    <div className="space-y-2">
                      <label htmlFor="subject" className="block text-sm font-medium text-neutral-700">
                        Subject
                      </label>
                      <input
                        id="subject"
                        type="text"
                        value={form.subject}
                        onChange={(e) => handleInputChange('subject', e.target.value)}
                        className="block w-full px-3 py-3 border border-neutral-200 rounded-lg bg-white/50 backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200"
                        placeholder="Brief subject"
                      />
                    </div>
                  </div>

                  {/* Message */}
                  <div className="space-y-2">
                    <label htmlFor="message" className="block text-sm font-medium text-neutral-700">
                      Message *
                    </label>
                    <textarea
                      id="message"
                      rows={5}
                      value={form.message}
                      onChange={(e) => handleInputChange('message', e.target.value)}
                      className="block w-full px-3 py-3 border border-neutral-200 rounded-lg bg-white/50 backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200 resize-none"
                      placeholder="Tell us how we can help you..."
                      required
                    />
                  </div>

                  {/* Submit Button */}
                  <NeumorphicButton
                    type="submit"
                    variant="primary"
                    size="lg"
                    className="w-full"
                    disabled={isLoading}
                  >
                    {isLoading ? (
                      <div className="flex items-center justify-center">
                        <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2" />
                        Sending Message...
                      </div>
                    ) : (
                      <div className="flex items-center justify-center">
                        Send Message
                        <Send className="ml-2 h-5 w-5" />
                      </div>
                    )}
                  </NeumorphicButton>
                </form>
              </NeumorphicCard>
            </motion.div>

            {/* Additional Info */}
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.8, delay: 0.2 }}
              className="space-y-8"
            >
              {/* FAQ */}
              <NeumorphicCard className="p-8">
                <h3 className="text-xl font-bold text-neutral-800 mb-4">Quick Answers</h3>
                <div className="space-y-4">
                  <div>
                    <h4 className="font-semibold text-neutral-800 mb-1">How do I book an adventure?</h4>
                    <p className="text-neutral-600 text-sm">Browse our adventures, select your preferred date, and complete the secure payment process.</p>
                  </div>
                  <div>
                    <h4 className="font-semibold text-neutral-800 mb-1">Can I reschedule my booking?</h4>
                    <p className="text-neutral-600 text-sm">Yes! You can reschedule up to 24 hours before your adventure starts.</p>
                  </div>
                  <div>
                    <h4 className="font-semibold text-neutral-800 mb-1">Do you offer group discounts?</h4>
                    <p className="text-neutral-600 text-sm">Absolutely! Groups of 8+ people get 15% off. Contact us for corporate rates.</p>
                  </div>
                </div>
              </NeumorphicCard>

              {/* CTA */}
              <NeumorphicCard className="p-8 bg-gradient-to-br from-primary-50 to-primary-100">
                <h3 className="text-xl font-bold text-neutral-800 mb-4">Ready to Adventure?</h3>
                <p className="text-neutral-600 mb-6">
                  Don't wait! Browse our exciting treasure hunts and book your next adventure today.
                </p>
                <Link href="/adventures">
                  <NeumorphicButton variant="primary" className="w-full">
                    Explore Adventures
                    <ArrowRight className="ml-2 h-5 w-5" />
                  </NeumorphicButton>
                </Link>
              </NeumorphicCard>
            </motion.div>
          </div>
        </div>
      </section>
    </div>
  );
}
