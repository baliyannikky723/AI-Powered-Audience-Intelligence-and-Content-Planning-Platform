import React from 'react';
import { Youtube, Instagram, Facebook, Linkedin, Tiktok, Twitter, Globe } from '../components/BrandIcons';

/**
 * Returns the brand icon component for a given platform slug.
 */
export function getPlatformIcon(platform: string, size: number = 16): React.ReactNode {
  const p = platform.toLowerCase();
  switch (p) {
    case 'youtube':
      return <Youtube size={size} className="text-[#FF0000]" />;
    case 'instagram':
      return <Instagram size={size} className="text-[#E1306C]" />;
    case 'facebook':
      return <Facebook size={size} className="text-[#1877F2]" />;
    case 'linkedin':
      return <Linkedin size={size} className="text-[#0A66C2]" />;
    case 'tiktok':
      return <Tiktok size={size} className="text-[#010101]" />;
    case 'twitter':
    case 'x':
      return <Twitter size={size} className="text-[#1DA1F2]" />;
    default:
      return <Globe size={size} className="text-ink-3" />;
  }
}

/**
 * Returns the hex brand color for a platform.
 */
export function platformColor(platform: string): string {
  switch (platform.toLowerCase()) {
    case 'youtube':   return '#FF0000';
    case 'instagram': return '#E1306C';
    case 'facebook':  return '#1877F2';
    case 'linkedin':  return '#0A66C2';
    case 'tiktok':    return '#010101';
    case 'twitter':
    case 'x':         return '#1DA1F2';
    default:          return '#8A857E';
  }
}

/**
 * Returns the display label for a platform.
 */
export function platformLabel(platform: string): string {
  switch (platform.toLowerCase()) {
    case 'youtube':   return 'YouTube';
    case 'instagram': return 'Instagram';
    case 'facebook':  return 'Facebook';
    case 'linkedin':  return 'LinkedIn';
    case 'tiktok':    return 'TikTok';
    case 'twitter':
    case 'x':         return 'Twitter / X';
    default: return platform.charAt(0).toUpperCase() + platform.slice(1);
  }
}

/**
 * Tailwind bg color class for each platform dot indicator.
 */
export function platformDotClass(platform: string): string {
  switch (platform.toLowerCase()) {
    case 'youtube':   return 'bg-[#FF0000]';
    case 'instagram': return 'bg-[#E1306C]';
    case 'facebook':  return 'bg-[#1877F2]';
    case 'linkedin':  return 'bg-[#0A66C2]';
    case 'tiktok':    return 'bg-[#010101]';
    case 'twitter':
    case 'x':         return 'bg-[#1DA1F2]';
    default:          return 'bg-ink-3';
  }
}
