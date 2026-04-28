import React, { useEffect, useState } from 'react';
import { motion, useInView, useMotionValue, useTransform, animate } from 'framer-motion';

// Animated number counter that counts up from 0 to `to`.
export default function Counter({ to, duration = 1.6, prefix = '', suffix = '', decimals = 0, start = true }) {
  const value = useMotionValue(0);
  const display = useTransform(value, (v) => {
    const n = decimals > 0 ? v.toFixed(decimals) : Math.round(v).toLocaleString('es-ES');
    return `${prefix}${n}${suffix}`;
  });

  useEffect(() => {
    if (!start) return;
    const ctrl = animate(value, to, { duration, ease: [0.22, 1, 0.36, 1] });
    return () => ctrl.stop();
  }, [start, to, duration]);

  return <motion.span>{display}</motion.span>;
}
