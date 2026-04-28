import { useEffect, useRef, useState } from 'react';

/*
 * Drives a deterministic timeline of scenes.
 * `scenes` is [{ id, duration }] in seconds. Returns:
 *   - currentIndex
 *   - localTime (seconds inside current scene)
 *   - elapsed (seconds since timeline started)
 *   - progress 0..1 across whole timeline
 *   - finished
 *
 * Deterministic playback so Playwright recordings line up frame-for-frame.
 */
export function useTimeline(scenes, { autoStart = true, loop = false } = {}) {
  const [tick, setTick] = useState(0);
  const startRef = useRef(null);
  const rafRef = useRef(null);

  const total = scenes.reduce((acc, s) => acc + s.duration, 0);

  useEffect(() => {
    if (!autoStart) return;
    startRef.current = performance.now();
    const loopFn = (t) => {
      setTick(t);
      rafRef.current = requestAnimationFrame(loopFn);
    };
    rafRef.current = requestAnimationFrame(loopFn);
    return () => cancelAnimationFrame(rafRef.current);
  }, [autoStart]);

  const elapsedRaw = startRef.current ? (tick - startRef.current) / 1000 : 0;
  const elapsed = loop ? elapsedRaw % total : Math.min(elapsedRaw, total);
  const finished = !loop && elapsedRaw >= total;

  let acc = 0;
  let currentIndex = 0;
  for (let i = 0; i < scenes.length; i++) {
    if (elapsed < acc + scenes[i].duration) { currentIndex = i; break; }
    acc += scenes[i].duration;
    currentIndex = i;
  }
  const localTime = elapsed - acc;
  const progress = total > 0 ? elapsed / total : 0;

  return { currentIndex, localTime, elapsed, progress, finished, total };
}
