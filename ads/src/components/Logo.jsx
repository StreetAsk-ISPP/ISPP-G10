import React from 'react';
import { motion } from 'framer-motion';
import { theme } from '../theme.js';

/*
 * StreetAsk wordmark + pin.
 * The pin is a red Google-Maps-style teardrop with a stylised person silhouette
 * carved out in white (round head + drop-shaped body), matching frontend/assets/logo.png.
 */
export default function Logo({ size = 64, showText = true }) {
  const pinW = size;
  const pinH = size * 1.5;

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
      <motion.div
        animate={{ y: [0, -3, 0] }}
        transition={{ duration: 3.5, repeat: Infinity, ease: 'easeInOut' }}
        style={{ width: pinW, height: pinH, filter: `drop-shadow(0 8px 18px ${theme.red}55)` }}
      >
        <svg viewBox="0 0 100 150" width={pinW} height={pinH}>
          <defs>
            <linearGradient id={`pinGrad-${size}`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#e84a4a" />
              <stop offset="100%" stopColor={theme.redDeep} />
            </linearGradient>
            <mask id={`pinMask-${size}`}>
              <rect width="100" height="150" fill="#fff" />
              {/* head */}
              <circle cx="50" cy="38" r="14" fill="#000" />
              {/* body teardrop */}
              <path d="M50 60
                       C 32 60 28 82 32 96
                       C 36 110 50 112 50 112
                       C 50 112 64 110 68 96
                       C 72 82 68 60 50 60 Z"
                    fill="#000" />
            </mask>
          </defs>
          <path d="M50 0
                   C 22 0 0 22 0 50
                   C 0 80 50 150 50 150
                   C 50 150 100 80 100 50
                   C 100 22 78 0 50 0 Z"
                fill={`url(#pinGrad-${size})`}
                mask={`url(#pinMask-${size})`} />
        </svg>
      </motion.div>
      {showText && (
        <div style={{
          fontFamily: 'Space Grotesk', fontWeight: 700,
          fontSize: size * 0.55, letterSpacing: -1,
        }}>
          Street<span style={{ color: theme.red }}>Ask</span>
        </div>
      )}
    </div>
  );
}
