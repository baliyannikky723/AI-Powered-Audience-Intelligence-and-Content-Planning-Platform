/**
 * Format a number to compact notation.
 * @example formatCompact(142000) → "142K"
 */
export function formatCompact(n: number): string {
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace(/\.0$/, '') + 'M';
  if (n >= 1_000)     return (n / 1_000).toFixed(1).replace(/\.0$/, '') + 'K';
  return n.toLocaleString();
}

/**
 * Format a number as a percentage string.
 * @example formatPercent(38.4) → "38.4%"
 */
export function formatPercent(n: number, decimals = 1): string {
  return n.toFixed(decimals) + '%';
}

/**
 * Format a Date or ISO string as a relative time.
 * @example formatRelative(date) → "2h ago"
 */
export function formatRelative(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  const diff = Date.now() - d.getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 1)   return 'just now';
  if (mins < 60)  return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30)  return `${days}d ago`;
  return formatDate(d);
}

/**
 * Format a Date or ISO string as a readable date.
 * @example formatDate(date) → "Oct 5, 2026"
 */
export function formatDate(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  return d.toLocaleDateString('en-US', {
    month: 'short',
    day:   'numeric',
    year:  'numeric',
  });
}

/**
 * Format a Date or ISO string as date + time.
 * @example formatDateTime(date) → "Oct 5 · 14:32"
 */
export function formatDateTime(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  const dateStr = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  const timeStr = d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  return `${dateStr} · ${timeStr}`;
}

/**
 * Clamp a number between min and max.
 */
export function clamp(n: number, min: number, max: number): number {
  return Math.min(Math.max(n, min), max);
}
