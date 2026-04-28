import React from 'react';
import { motion } from 'framer-motion';
import { theme } from '../theme.js';

// Thin progress bar shown across all ads, communicates ~60s timeline at a glance.
export default function ProgressBar({ progress }) {
  return (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 0, height: 4,
      background: 'rgba(255,255,255,0.08)', zIndex: 50,
    }}>
      <motion.div
        style={{
          height: '100%',
          background: `linear-gradient(90deg, ${theme.red}, ${theme.purple})`,
          width: `${progress * 100}%`,
        }}
      />
    </div>
  );
}
