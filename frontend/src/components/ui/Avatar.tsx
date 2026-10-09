import React from 'react';
import { cn } from '../../lib/cn';

export interface AvatarProps {
  src?:      string;
  name:      string;
  size?:     'xs' | 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeMap = {
  xs: 'h-5 w-5 text-[8px]',
  sm: 'h-7 w-7 text-xs',
  md: 'h-9 w-9 text-sm',
  lg: 'h-11 w-11 text-base',
};

function initials(name: string): string {
  return name
    .split(' ')
    .map(w => w[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

export const Avatar: React.FC<AvatarProps> = ({ src, name, size = 'md', className }) => {
  const [imgError, setImgError] = React.useState(false);
  const showFallback = !src || imgError;

  return (
    <span
      className={cn(
        'inline-flex items-center justify-center rounded-full flex-shrink-0 select-none overflow-hidden',
        'bg-accent-soft text-accent-ink font-semibold',
        'ring-1 ring-border',
        sizeMap[size],
        className
      )}
      role="img"
      aria-label={name}
    >
      {showFallback ? (
        <span>{initials(name)}</span>
      ) : (
        <img
          src={src}
          alt={name}
          className="h-full w-full object-cover"
          onError={() => setImgError(true)}
        />
      )}
    </span>
  );
};
