import { useEffect, useRef, useState } from 'react';

export const prefersReducedMotion = () =>
  typeof window !== 'undefined' && !!window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/**
 * Chuyển mượt từ giá trị hiển thị hiện tại sang giá trị mới (không quay lại 0 mỗi lần polling).
 * Tôn trọng prefers-reduced-motion.
 */
export function useCountUp(target, duration = 700) {
  const [value, setValue] = useState(Number.isFinite(target) ? target : 0);
  const current = useRef(value);

  useEffect(() => {
    if (!Number.isFinite(target)) return undefined;
    if (prefersReducedMotion()) {
      current.current = target;
      setValue(target);
      return undefined;
    }
    const from = current.current;
    const delta = target - from;
    if (Math.abs(delta) < 1e-6) return undefined;
    let raf;
    const t0 = performance.now();
    const step = (t) => {
      const p = Math.min(1, (t - t0) / duration);
      const eased = 1 - Math.pow(1 - p, 3);
      const v = from + delta * eased;
      current.current = v;
      setValue(v);
      if (p < 1) raf = requestAnimationFrame(step);
    };
    raf = requestAnimationFrame(step);
    return () => cancelAnimationFrame(raf);
  }, [target, duration]);

  return value;
}
