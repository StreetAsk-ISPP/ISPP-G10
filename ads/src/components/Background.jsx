import React from 'react';
import { motion } from 'framer-motion';
import { theme } from '../theme.js';

// Animated gradient + subtle grid + floating orbs. Used as the canvas backdrop.
export default function Background({ variant = 'investors' }) {
  const palette = variant === 'investors'
    ? [theme.purple, theme.bg0, theme.red]
    : [theme.red, theme.bg0, theme.purple];

  return (
    <div style={{ position: 'absolute', inset: 0, overflow: 'hidden' }}>
      <motion.div
        style={{
          position: 'absolute', inset: 0,
          background: `radial-gradient(1200px 800px at 20% 20%, ${palette[0]}22, transparent 60%),
                       radial-gradient(1200px 800px at 80% 80%, ${palette[2]}22, transparent 60%),
                       linear-gradient(160deg, ${theme.bg0}, ${theme.bg1})`,
        }}
        animate={{ opacity: [0.9, 1, 0.9] }}
        transition={{ duration: 8, repeat: Infinity }}
      />
      {/* grid */}
      <div style={{
        position: 'absolute', inset: 0,
        backgroundImage: `linear-gradient(rgba(14,14,24,0.05) 1px, transparent 1px),
                          linear-gradient(90deg, rgba(14,14,24,0.05) 1px, transparent 1px)`,
        backgroundSize: '80px 80px',
        maskImage: 'radial-gradient(ellipse at center, #000 50%, transparent 80%)',
      }} />
      {/* orbs */}
      {[0,1,2,3,4].map(i => (
        <motion.div
          key={i}
          style={{
            position: 'absolute',
            width: 320, height: 320,
            borderRadius: '50%',
            filter: 'blur(80px)',
            background: i % 2 ? palette[0] : palette[2],
            opacity: 0.12,
            left: `${(i * 23) % 100}%`,
            top: `${(i * 37) % 100}%`,
          }}
          animate={{ x: [0, 60, -40, 0], y: [0, -50, 30, 0] }}
          transition={{ duration: 14 + i * 2, repeat: Infinity, ease: 'easeInOut' }}
        />
      ))}
    </div>
  );
}
