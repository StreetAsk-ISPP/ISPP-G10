import React from 'react';
import { theme } from '../theme.js';

/*
 * Minimal monochrome icon set. All icons are 24x24 viewBox.
 * Use `color` prop (defaults to currentColor) so they inherit text color.
 * We avoid emojis throughout the ads in favour of these.
 */
export function Icon({ name, size = 24, color = 'currentColor', strokeWidth = 1.8, style }) {
  const Comp = ICONS[name];
  if (!Comp) return null;
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
         stroke={color} strokeWidth={strokeWidth}
         strokeLinecap="round" strokeLinejoin="round" style={style}>
      <Comp />
    </svg>
  );
}

const ICONS = {
  pin: () => (
    <>
      <path d="M12 21s7-7.4 7-12a7 7 0 1 0-14 0c0 4.6 7 12 7 12z" />
      <circle cx="12" cy="9" r="2.6" />
    </>
  ),
  clock: () => (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  chat: () => (
    <>
      <path d="M21 12a8 8 0 0 1-11.6 7.1L4 21l1.9-5.4A8 8 0 1 1 21 12z" />
    </>
  ),
  thread: () => (
    <>
      <rect x="3" y="4" width="18" height="12" rx="3" />
      <path d="M8 20l4-3h6" />
      <path d="M7 9h10M7 12h6" />
    </>
  ),
  star: () => (
    <path d="M12 3l2.6 5.6 6 .7-4.4 4.2 1.2 6L12 16.8 6.6 19.5l1.2-6L3.4 9.3l6-.7z" />
  ),
  shield: () => (
    <>
      <path d="M12 3l8 3v6c0 5-3.6 8.4-8 9-4.4-.6-8-4-8-9V6z" />
      <path d="M9 12l2 2 4-4" />
    </>
  ),
  coin: () => (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M9.5 9.5h3.5a2 2 0 1 1 0 4h-3.5M11 6.5v11" />
    </>
  ),
  bolt: () => (
    <path d="M13 2L4 14h6l-1 8 9-12h-6l1-8z" />
  ),
  search: () => (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="M21 21l-4.3-4.3" />
    </>
  ),
  globe: () => (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18" />
    </>
  ),
  store: () => (
    <>
      <path d="M3 9l1.5-5h15L21 9" />
      <path d="M3 9v11h18V9" />
      <path d="M3 9c0 2 1.5 3 3 3s3-1 3-3 1.5 3 3 3 3-1 3-3 1.5 3 3 3 3-1 3-3" />
      <path d="M9 20v-5h6v5" />
    </>
  ),
  bell: () => (
    <>
      <path d="M6 16V11a6 6 0 1 1 12 0v5l1.5 2H4.5z" />
      <path d="M10 20a2 2 0 0 0 4 0" />
    </>
  ),
  thumbUp: () => (
    <>
      <path d="M7 10v10H4V10z" />
      <path d="M7 10l4-7c1.5 0 2.5 1 2.5 2.5V9h5.5a2 2 0 0 1 2 2.4l-1.5 7A2 2 0 0 1 17.5 20H7" />
    </>
  ),
  check: () => (
    <path d="M5 12l4 4 10-10" />
  ),
  trending: () => (
    <>
      <path d="M3 17l6-6 4 4 8-8" />
      <path d="M14 7h7v7" />
    </>
  ),
  users: () => (
    <>
      <circle cx="9" cy="8" r="3.2" />
      <path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6" />
      <circle cx="17" cy="9" r="2.6" />
      <path d="M15 14c3.3 0 6 2.2 6 5" />
    </>
  ),
  calendar: () => (
    <>
      <rect x="3.5" y="5" width="17" height="15" rx="2.5" />
      <path d="M3.5 10h17M8 3v4M16 3v4" />
    </>
  ),
  euro: () => (
    <>
      <path d="M18 6.5A7 7 0 0 0 8 12a7 7 0 0 0 10 5.5" />
      <path d="M5 10h8M5 14h8" />
    </>
  ),
  rocket: () => (
    <>
      <path d="M14 4c4 0 6 2 6 6 0 5-7 10-7 10s-7-5-7-10c0-4 2-6 6-6h2z" />
      <circle cx="13" cy="10" r="2" />
      <path d="M9 17l-3 3M15 17l3 3" />
    </>
  ),
  music: () => (
    <>
      <path d="M9 18V5l11-2v13" />
      <circle cx="6" cy="18" r="3" />
      <circle cx="17" cy="16" r="3" />
    </>
  ),
  wave: () => (
    <path d="M2 12c2-3 4-3 6 0s4 3 6 0 4-3 6 0 4 3 6 0" />
  ),
  beer: () => (
    <>
      <path d="M5 7h10v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1z" />
      <path d="M15 10h2.5a2 2 0 1 1 0 4H15" />
      <path d="M8 11v6M11 11v6" />
    </>
  ),
  bus: () => (
    <>
      <rect x="4" y="5" width="16" height="12" rx="2" />
      <path d="M4 10h16M8 17v2M16 17v2" />
      <circle cx="8" cy="14.5" r="1.2" />
      <circle cx="16" cy="14.5" r="1.2" />
    </>
  ),
  gift: () => (
    <>
      <rect x="3" y="9" width="18" height="12" rx="2" />
      <path d="M3 14h18M12 9v12" />
      <path d="M12 9c-2.5 0-4-1.5-4-3a2 2 0 0 1 4 0c0 1.5-1.5 3-4 3" />
      <path d="M12 9c2.5 0 4-1.5 4-3a2 2 0 0 0-4 0c0 1.5 1.5 3 4 3" />
    </>
  ),
};

// Convenience colored chip used in scenes.
export function IconChip({ name, color = theme.red, size = 56, bg }) {
  const background = bg ?? `${color}1f`;
  return (
    <div style={{
      width: size, height: size, borderRadius: size * 0.28,
      background, color,
      display: 'grid', placeItems: 'center',
      border: `1px solid ${color}55`,
    }}>
      <Icon name={name} size={size * 0.55} />
    </div>
  );
}
