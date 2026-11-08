import { useId } from 'react';
import AnimatedNumber from './AnimatedNumber';

// Hình học vòng cung 240° (từ 150° đến 390°, chiều kim đồng hồ), tâm (110,105), bán kính 84
const CX = 110;
const CY = 105;
const R = 84;
const START = 150;
const SWEEP = 240;
const rad = (deg) => (deg * Math.PI) / 180;
const pt = (deg, r = R) => [CX + r * Math.cos(rad(deg)), CY + r * Math.sin(rad(deg))];

const [X1, Y1] = pt(START);
const [X2, Y2] = pt(START + SWEEP);
const ARC = `M ${X1.toFixed(2)} ${Y1.toFixed(2)} A ${R} ${R} 0 1 1 ${X2.toFixed(2)} ${Y2.toFixed(2)}`;

/**
 * Đồng hồ vòng cung có mốc ngưỡng. Cung giá trị chạy bằng CSS transition (stroke-dashoffset)
 * nên khi có số liệu mới nó trượt từ giá trị cũ sang giá trị mới, không quay lại 0.
 */
export default function Gauge({ value, min, max, threshold, unit, colors, glow, decimals = 1 }) {
  const uid = useId().replace(/:/g, '');
  const has = Number.isFinite(value);
  const pct = has ? Math.max(0, Math.min(1, (value - min) / (max - min))) : 0;

  const tickPct = Number.isFinite(threshold) ? Math.max(0, Math.min(1, (threshold - min) / (max - min))) : null;
  let tick = null;
  if (tickPct !== null) {
    const a = START + SWEEP * tickPct;
    const [ax, ay] = pt(a, R - 13);
    const [bx, by] = pt(a, R + 13);
    const [lx, ly] = pt(a, R + 25);
    tick = { ax, ay, bx, by, lx, ly };
  }
  const [minX, minY] = pt(START, R);
  const [maxX, maxY] = pt(START + SWEEP, R);

  return (
    <div className="gauge" style={{ '--arc-glow': glow }}>
      <svg viewBox="0 0 220 190" aria-hidden="true" focusable="false">
        <defs>
          <linearGradient id={`${uid}-g`} x1="0" y1="1" x2="1" y2="0">
            <stop offset="0" stopColor={colors[0]} />
            <stop offset="1" stopColor={colors[1]} />
          </linearGradient>
        </defs>
        <path className="track" d={ARC} strokeWidth="14" />
        <path className="value-arc" d={ARC} strokeWidth="14" stroke={`url(#${uid}-g)`} pathLength="100"
          strokeDasharray="100" strokeDashoffset={100 - pct * 100} style={{ opacity: has ? 1 : 0 }} />
        {tick && (
          <>
            <line className="tick" x1={tick.ax} y1={tick.ay} x2={tick.bx} y2={tick.by} />
            <text className="tick-label" x={tick.lx} y={tick.ly + 3}>{threshold}</text>
          </>
        )}
        <text className="tick-label" x={minX} y={minY + 22}>{min}</text>
        <text className="tick-label" x={maxX} y={maxY + 22}>{max}</text>
      </svg>
      <div className="gauge-center">
        <div className="gauge-value">
          <AnimatedNumber value={value} decimals={decimals} />
          <small>{unit}</small>
        </div>
      </div>
    </div>
  );
}
