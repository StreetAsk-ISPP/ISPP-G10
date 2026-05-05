import React from 'react';
import { theme } from '../theme.js';

// A polished phone frame to wrap mockups (used in client ad).
export default function PhoneFrame({ children, width = 380 }) {
  const height = width * 2.05;
  return (
    <div style={{
      width, height, padding: 14,
      borderRadius: 56,
      background: 'linear-gradient(160deg, #2a2a3a, #0e0e18)',
      boxShadow: '0 40px 100px rgba(14,14,24,0.45), inset 0 0 0 1px rgba(255,255,255,0.08)',
      position: 'relative',
    }}>
      <div style={{
        width: '100%', height: '100%',
        borderRadius: 44,
        background: '#ffffff',
        color: theme.text,
        overflow: 'hidden',
        position: 'relative',
      }}>
        {/* Notch */}
        <div style={{
          position: 'absolute', top: 12, left: '50%', transform: 'translateX(-50%)',
          width: 110, height: 28, borderRadius: 16, background: '#0e0e18', zIndex: 5,
        }} />
        {children}
      </div>
    </div>
  );
}
