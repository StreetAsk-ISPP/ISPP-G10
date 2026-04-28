import React, { useEffect, useState } from 'react';
import { STAGE } from '../theme.js';

// Renders a fixed 1920x1080 stage centered and scaled to fit the viewport.
// All ads are designed at 1920x1080 so video output is consistent.
export default function Stage({ children }) {
  const [scale, setScale] = useState(1);

  useEffect(() => {
    const compute = () => {
      const sx = window.innerWidth / STAGE.width;
      const sy = window.innerHeight / STAGE.height;
      setScale(Math.min(sx, sy));
    };
    compute();
    window.addEventListener('resize', compute);
    return () => window.removeEventListener('resize', compute);
  }, []);

  return (
    <div style={{
      position: 'fixed', inset: 0, display: 'grid', placeItems: 'center',
      background: '#000',
    }}>
      <div style={{
        width: STAGE.width,
        height: STAGE.height,
        transform: `scale(${scale})`,
        transformOrigin: 'center center',
        position: 'relative',
        overflow: 'hidden',
        background: '#0a0a14',
        boxShadow: '0 30px 120px rgba(0,0,0,0.8)',
      }}>
        {children}
      </div>
    </div>
  );
}
