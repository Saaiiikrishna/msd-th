'use client';

import { useState, useEffect } from 'react';
import { useTheme } from 'next-themes';
import { Sun, Moon } from 'lucide-react';

export function DarkModeToggle() {
  const [mounted, setMounted] = useState(false);
  const { theme, setTheme, resolvedTheme } = useTheme();

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    return (
      <div className="w-10 h-10 rounded-full bg-neutral-200 animate-pulse" />
    );
  }

  const isDark = resolvedTheme === 'dark';

  return (
    <button
      onClick={() => setTheme(isDark ? 'light' : 'dark')}
      className={`
        relative p-2.5 rounded-full transition-all duration-300
        ${isDark
          ? 'bg-neutral-800 shadow-neumorphic-dark hover:shadow-neumorphic-dark-hover'
          : 'bg-neutral-100 shadow-neumorphic hover:shadow-neumorphic-hover'
        }
        border border-neutral-200 dark:border-neutral-700
        hover:scale-105 active:scale-95
      `}
      aria-label={`Switch to ${isDark ? 'light' : 'dark'} mode`}
    >
      <div className="relative w-5 h-5">
        {isDark ? (
          <Sun className="w-5 h-5 text-yellow-400 transition-all duration-300" />
        ) : (
          <Moon className="w-5 h-5 text-neutral-600 transition-all duration-300" />
        )}
      </div>
    </button>
  );
}
