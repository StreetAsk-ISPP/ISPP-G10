import React from 'react';
import { motion } from 'framer-motion';
import { theme } from '../theme.js';

/*
 * A rounded speech bubble with a tail pointing toward `tailSide`
 * ('left' = tail on bottom-left → speaker is to the left of the bubble,
 *  'right' = tail on bottom-right → speaker is to the right of the bubble).
 */
export default function SpeechBubble({
  children,
  tailSide = 'left',
  delay = 0,
  color,
  style,
}) {
  const bg = color ? `linear-gradient(135deg, ${color}, ${theme.purple})` : '#ffffff';
  const fg = color ? '#fff' : '#0e0e18';

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.6, y: 20 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      transition={{ delay, duration: 0.5, type: 'spring', stiffness: 220, damping: 18 }}
      style={{
        position: 'relative',
        background: bg, color: fg,
        padding: '26px 36px',
        borderRadius: 32,
        fontSize: 40, fontWeight: 700, lineHeight: 1.2,
        maxWidth: 620,
        boxShadow: '0 24px 60px rgba(0,0,0,0.55)',
        ...style,
      }}
    >
      {children}
      <Tail side={tailSide} bg={color || '#ffffff'} solid={!color} />
    </motion.div>
  );
}

function Tail({ side, bg, solid }) {
  const left = side === 'left';
  return (
    <svg
      width="34" height="28" viewBox="0 0 34 28"
      style={{
        position: 'absolute',
        bottom: -16,
        [left ? 'left' : 'right']: 26,
        transform: left ? 'none' : 'scaleX(-1)',
      }}
    >
      <path
        d="M0 0 C 6 18 18 24 30 26 L 8 12 Z"
        fill={solid ? bg : bg}
      />
    </svg>
  );
}
