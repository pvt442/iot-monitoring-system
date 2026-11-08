import { useCountUp } from '../hooks/useCountUp';

/** Hiển thị số với hiệu ứng chuyển mượt khi giá trị đổi. */
export default function AnimatedNumber({ value, decimals = 0, duration = 700 }) {
  const v = useCountUp(Number.isFinite(value) ? value : 0, duration);
  if (!Number.isFinite(value)) return <>--</>;
  return <>{v.toFixed(decimals)}</>;
}
