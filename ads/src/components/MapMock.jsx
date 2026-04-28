import React from 'react';
import { motion } from 'framer-motion';
import { theme } from '../theme.js';

// Stylised "neighborhood map" mockup with animated red question pins and gold event pins.
export default function MapMock({ width = 900, height = 560, showEvents = false, pulse = true }) {
  const pins = [
    { x: 180, y: 160, type: 'q' },
    { x: 360, y: 110, type: 'q' },
    { x: 520, y: 230, type: 'q' },
    { x: 680, y: 170, type: 'q' },
    { x: 250, y: 340, type: 'q' },
    { x: 470, y: 410, type: 'q' },
    { x: 730, y: 380, type: 'q' },
    { x: 600, y: 470, type: 'q' },
    { x: 320, y: 470, type: 'event' },
    { x: 760, y: 260, type: 'event' },
  ];

  return (
    <div style={{
      width, height, position: 'relative',
      borderRadius: 28, overflow: 'hidden',
      background: 'linear-gradient(160deg, #1a2238, #0e1426)',
      border: `1px solid ${theme.border}`,
      boxShadow: '0 30px 80px rgba(0,0,0,0.6)',
    }}>
      {/* Streets */}
      <svg viewBox={`0 0 ${width} ${height}`} width={width} height={height} style={{ position: 'absolute', inset: 0 }}>
        <defs>
          <linearGradient id="street" x1="0" x2="1">
            <stop offset="0%" stopColor="#2a3556" />
            <stop offset="100%" stopColor="#3b4a78" />
          </linearGradient>
        </defs>
        {/* parks */}
        <rect x="60" y="60" width="220" height="160" rx="20" fill="#1f3a2a" opacity="0.7" />
        <rect x={width - 260} y={height - 200} width="200" height="160" rx="20" fill="#1f3a2a" opacity="0.7" />
        {/* avenues */}
        {[100, 250, 400, 540].map((y, i) => (
          <motion.line key={`h${i}`} x1="0" y1={y} x2={width} y2={y}
            stroke="url(#street)" strokeWidth="14" strokeLinecap="round"
            initial={{ pathLength: 0 }} animate={{ pathLength: 1 }}
            transition={{ duration: 1.2, delay: i * 0.1 }} />
        ))}
        {[160, 360, 560, 760].map((x, i) => (
          <motion.line key={`v${i}`} x1={x} y1="0" x2={x} y2={height}
            stroke="url(#street)" strokeWidth="14" strokeLinecap="round"
            initial={{ pathLength: 0 }} animate={{ pathLength: 1 }}
            transition={{ duration: 1.2, delay: 0.4 + i * 0.1 }} />
        ))}
        {/* buildings */}
        {Array.from({ length: 30 }).map((_, i) => {
          const x = 30 + (i * 73) % (width - 80);
          const y = 30 + (i * 113) % (height - 60);
          const w = 26 + (i % 3) * 8;
          const h = 26 + (i % 4) * 8;
          return <rect key={i} x={x} y={y} width={w} height={h} rx="4" fill="#2a3148" opacity="0.55" />;
        })}
      </svg>

      {/* Pins */}
      {pins.map((p, i) => {
        if (p.type === 'event' && !showEvents) return null;
        const color = p.type === 'event' ? theme.gold : theme.red;
        return (
          <motion.div
            key={i}
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ delay: 0.6 + i * 0.08, type: 'spring', stiffness: 200, damping: 12 }}
            style={{ position: 'absolute', left: p.x, top: p.y, transform: 'translate(-50%, -100%)' }}
          >
            {pulse && (
              <motion.div
                style={{
                  position: 'absolute', left: '50%', top: '100%',
                  width: 60, height: 60, marginLeft: -30, marginTop: -30,
                  borderRadius: '50%', background: color, opacity: 0.4,
                }}
                animate={{ scale: [1, 2.4], opacity: [0.5, 0] }}
                transition={{ duration: 1.8, repeat: Infinity, delay: i * 0.15 }}
              />
            )}
            <svg width="34" height="44" viewBox="0 0 24 32">
              <path d="M12 0C5.4 0 0 5.4 0 12c0 8 12 20 12 20s12-12 12-20C24 5.4 18.6 0 12 0z"
                    fill={color} stroke="#fff" strokeWidth="1.2" />
              <circle cx="12" cy="12" r="4.5" fill="#fff" />
              {p.type === 'event' && (
                <text x="12" y="15" textAnchor="middle" fontSize="7" fontWeight="800" fill={theme.gold}>★</text>
              )}
            </svg>
          </motion.div>
        );
      })}

      {/* User dot */}
      <motion.div
        style={{
          position: 'absolute', left: width / 2, top: height / 2,
          width: 22, height: 22, borderRadius: '50%',
          background: '#3b82f6', border: '3px solid #fff',
          boxShadow: '0 0 0 6px rgba(59,130,246,0.3)',
          transform: 'translate(-50%, -50%)',
        }}
        animate={{ boxShadow: ['0 0 0 6px rgba(59,130,246,0.3)', '0 0 0 18px rgba(59,130,246,0)'] }}
        transition={{ duration: 1.6, repeat: Infinity }}
      />
    </div>
  );
}
