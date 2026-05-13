import React from 'react';
import { motion } from 'framer-motion';

/*
 * Bathroom-sign style human silhouettes. Variants:
 *   - 'man'         classic male signage
 *   - 'womanPony'   girl with pigtails
 *   - 'woman'       skirt silhouette
 *   - 'manCap'      figure with a baseball cap
 *   - 'businessman' figure with briefcase
 *
 * SVG viewBox: 100x180. Use `color` to override fill.
 */
export default function Person({
  variant = 'man',
  color = '#fff',
  size = 180,
  bob = true,
  flip = false,
  style,
}) {
  const w = size * (100 / 180);
  const h = size;
  const Body = BODIES[variant] || BODIES.man;

  return (
    <motion.div
      animate={bob ? { y: [0, -6, 0] } : {}}
      transition={{ duration: 2.4, repeat: Infinity, ease: 'easeInOut' }}
      style={{
        width: w, height: h,
        transform: flip ? 'scaleX(-1)' : 'none',
        ...style,
      }}
    >
      <svg viewBox="0 0 100 180" width={w} height={h}>
        <g fill={color}>
          <Body />
        </g>
      </svg>
    </motion.div>
  );
}

const BODIES = {
  // Classic male sign: round head, broad torso, straight legs.
  man: () => (
    <>
      <circle cx="50" cy="22" r="16" />
      <path d="M28 50
               C 28 44 34 42 50 42
               C 66 42 72 44 72 50
               L 78 105
               L 60 105
               L 58 175
               L 42 175
               L 40 105
               L 22 105 Z" />
    </>
  ),

  // Girl with pigtails (the user explicitly asked for this).
  womanPony: () => (
    <>
      {/* pigtails */}
      <ellipse cx="32" cy="22" rx="6" ry="9" />
      <ellipse cx="68" cy="22" rx="6" ry="9" />
      {/* head */}
      <circle cx="50" cy="22" r="15" />
      {/* fringe */}
      <path d="M36 18 Q50 8 64 18 L62 14 Q50 6 38 14 Z" />
      {/* dress: torso + triangular skirt */}
      <path d="M30 50
               C 30 44 36 42 50 42
               C 64 42 70 44 70 50
               L 76 92
               L 88 130
               L 12 130
               L 24 92 Z" />
      {/* legs */}
      <rect x="36" y="130" width="10" height="45" rx="3" />
      <rect x="54" y="130" width="10" height="45" rx="3" />
    </>
  ),

  // Generic woman: dress silhouette, no pigtails.
  woman: () => (
    <>
      <circle cx="50" cy="22" r="15" />
      <path d="M30 50
               C 30 44 36 42 50 42
               C 64 42 70 44 70 50
               L 76 92
               L 90 132
               L 10 132
               L 24 92 Z" />
      <rect x="36" y="132" width="10" height="43" rx="3" />
      <rect x="54" y="132" width="10" height="43" rx="3" />
    </>
  ),

  // Person with a baseball cap (casual user).
  manCap: () => (
    <>
      <circle cx="50" cy="24" r="15" />
      {/* cap */}
      <path d="M30 22 Q50 4 70 22 L80 22 Q60 12 30 22 Z" />
      <rect x="68" y="22" width="14" height="4" rx="2" />
      <path d="M28 50
               C 28 44 34 42 50 42
               C 66 42 72 44 72 50
               L 78 105
               L 60 105
               L 58 175
               L 42 175
               L 40 105
               L 22 105 Z" />
    </>
  ),

  // Business figure: square shoulders, briefcase silhouette.
  businessman: () => (
    <>
      <circle cx="50" cy="22" r="15" />
      <path d="M26 50
               C 26 44 34 42 50 42
               C 66 42 74 44 74 50
               L 80 110
               L 60 110
               L 58 175
               L 42 175
               L 40 110
               L 20 110 Z" />
      {/* tie */}
      <path d="M48 44 L52 44 L54 60 L50 70 L46 60 Z" />
      {/* briefcase */}
      <rect x="78" y="100" width="18" height="14" rx="2" />
      <rect x="84" y="96" width="6" height="4" rx="1" />
    </>
  ),
};
