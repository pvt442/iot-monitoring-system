import { useEffect, useId, useMemo, useState } from 'react';
import { Area, AreaChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { parseLocal } from '../utils/format';

const MAX_POINTS = 240;
const pad = (n) => String(n).padStart(2, '0');
const clock = (ms) => {
  const d = new Date(ms);
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

/** Giảm số điểm để biểu đồ luôn mượt (dữ liệu đầy đủ xem ở Data History). Luôn giữ điểm cuối. */
function downsample(points) {
  if (points.length <= MAX_POINTS) return points;
  const step = Math.ceil(points.length / MAX_POINTS);
  const out = points.filter((_, i) => i % step === 0);
  if (out[out.length - 1] !== points[points.length - 1]) out.push(points[points.length - 1]);
  return out;
}

function Tip({ active, payload, unit, label }) {
  if (!active || !payload || !payload.length) return null;
  const p = payload[0].payload;
  return (
    <div className="chart-tip">
      <span>{clock(p.t)}</span>
      <b>{p.v.toFixed(2)} {unit}</b>
      <span>{label}</span>
    </div>
  );
}

/**
 * Smooth Area Chart (monotone) có gradient mờ dần + đường ngưỡng.
 * Chỉ chạy animation vẽ đường ở lần đầu; các lần polling sau cập nhật trực tiếp, không giật.
 */
export default function TrendChart({ rows, field, color, unit, label, threshold }) {
  const uid = useId().replace(/:/g, '');
  const data = useMemo(
    () => downsample(rows.map((r) => ({ t: parseLocal(r.timestamp), v: r[field] })).filter((p) => Number.isFinite(p.t) && Number.isFinite(p.v))),
    [rows, field],
  );

  const [animate, setAnimate] = useState(true);
  const hasData = data.length > 0;
  useEffect(() => {
    if (!hasData) return undefined;
    const t = setTimeout(() => setAnimate(false), 1400);
    return () => clearTimeout(t);
  }, [hasData]);

  if (!hasData) {
    return (
      <div className="chart-empty">
        <div>
          <b style={{ color: 'var(--text)' }}>Chưa có dữ liệu trong khoảng thời gian này</b>
          <p>Hãy chạy IoT Client hoặc chọn khoảng thời gian dài hơn.</p>
        </div>
      </div>
    );
  }

  const vals = data.map((d) => d.v);
  let lo = Math.min(...vals);
  let hi = Math.max(...vals);
  if (Number.isFinite(threshold)) {
    hi = Math.max(hi, threshold);
    lo = Math.min(lo, threshold);
  }
  const span = Math.max(hi - lo, 4);
  const domain = [Math.floor(lo - span * 0.15), Math.ceil(hi + span * 0.15)];

  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={data} margin={{ top: 10, right: 16, left: -8, bottom: 0 }}>
        <defs>
          <linearGradient id={`${uid}-fill`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity={0.45} />
            <stop offset="100%" stopColor={color} stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid vertical={false} stroke="rgba(255,255,255,0.06)" strokeDasharray="3 5" />
        <XAxis dataKey="t" type="number" scale="time" domain={['dataMin', 'dataMax']} tickFormatter={clock}
          tickCount={5} axisLine={false} tickLine={false} tick={{ fill: '#7b8691', fontSize: 11 }} minTickGap={36} />
        <YAxis domain={domain} axisLine={false} tickLine={false} width={44} tick={{ fill: '#7b8691', fontSize: 11 }}
          tickFormatter={(v) => `${v}`} />
        <Tooltip content={<Tip unit={unit} label={label} />} cursor={{ stroke: 'rgba(255,255,255,0.25)', strokeDasharray: '3 4' }} />
        {Number.isFinite(threshold) && (
          <ReferenceLine y={threshold} stroke="#f05252" strokeDasharray="6 5" strokeOpacity={0.85}
            label={{ value: `Ngưỡng ${threshold}${unit}`, position: 'insideTopRight', fill: '#ff8f8f', fontSize: 11 }} />
        )}
        <Area type="monotone" dataKey="v" stroke={color} strokeWidth={2.4} fill={`url(#${uid}-fill)`}
          dot={false} activeDot={{ r: 5, stroke: '#151a1f', strokeWidth: 2, fill: color }}
          isAnimationActive={animate} animationDuration={1100} />
      </AreaChart>
    </ResponsiveContainer>
  );
}
