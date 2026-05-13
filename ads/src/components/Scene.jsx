import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';

// Wraps a scene so it fades + slides in/out cleanly when its `active` prop changes.
export default function Scene({ active, children }) {
  return (
    <AnimatePresence mode="wait">
      {active && (
        <motion.div
          key="scene"
          initial={{ opacity: 0, y: 30, filter: 'blur(8px)' }}
          animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
          exit={{ opacity: 0, y: -30, filter: 'blur(8px)' }}
          transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
          style={{ position: 'absolute', inset: 0 }}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
}
