/**
 * Merges class name strings, filtering out falsy values.
 * Lightweight alternative to clsx — no dependency needed.
 */
export function cn(
  ...args: (string | undefined | null | false | 0)[]
): string {
  return args.filter(Boolean).join(' ');
}
