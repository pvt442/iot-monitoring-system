import { Droplets, Thermometer } from 'lucide-react';
import Gauge from './Gauge';
import StatusBadge from './StatusBadge';
import { fmtNum } from '../utils/format';
import { metricState, STATE_TEXT } from '../utils/state';

const KINDS = {
  temp: {
    title: 'Nhiệt độ', icon: Thermometer, unit: '°C', metric: 'TEMPERATURE', field: 'temperature',
    min: 0, max: 60, colors: ['#f59e0b', '#fb7a3c'], glow: 'rgba(245,158,11,0.55)',
  },
  hum: {
    title: 'Độ ẩm', icon: Droplets, unit: '%', metric: 'HUMIDITY', field: 'humidity',
    min: 0, max: 100, colors: ['#39bde0', '#20c6ae'], glow: 'rgba(57,189,224,0.55)',
  },
};

/** Thẻ số đo lớn: gauge + trạng thái + ngưỡng + thấp nhất/cao nhất trong khung biểu đồ. */
export default function SensorCard({ kind, latest, threshold, rows, rangeLabel, offline }) {
  const k = KINDS[kind];
  const Icon = k.icon;
  const value = latest ? latest[k.field] : NaN;
  const st = metricState(latest, k.metric);
  const critical = st === 'critical' && !offline;

  const values = rows.map((r) => r[k.field]).filter(Number.isFinite);
  const lo = values.length ? Math.min(...values) : NaN;
  const hi = values.length ? Math.max(...values) : NaN;

  const colors = critical ? ['#f05252', '#ff8f6b'] : k.colors;
  const glow = critical ? 'rgba(240,82,82,0.7)' : k.glow;

  const badgeState = offline
    ? { key: 'offline', label: 'Dữ liệu đã cũ', tone: 'muted' }
    : { key: st === 'none' ? 'nodata' : st, label: STATE_TEXT[st],
        tone: st === 'critical' ? 'red' : st === 'warning' ? 'yellow' : st === 'normal' ? 'green' : 'muted' };

  return (
    <section className={`card sensor ${kind} ${critical ? 'is-critical' : ''}`} aria-label={k.title}>
      <div className="sensor-head">
        <div className="sensor-name">
          <span className="ico"><Icon aria-hidden="true" /></span>
          {k.title}
        </div>
        <StatusBadge state={badgeState} />
      </div>
      <Gauge value={value} min={k.min} max={k.max} threshold={threshold} unit={k.unit}
        colors={colors} glow={glow} />
      <div className="sensor-note">
        Ngưỡng cảnh báo: <b>&gt; {Number.isFinite(threshold) ? threshold : '--'} {k.unit}</b>
      </div>
      <div className="sensor-foot">
        <div>Thấp nhất ({rangeLabel})<b>{fmtNum(lo)} {k.unit}</b></div>
        <div>Cao nhất ({rangeLabel})<b>{fmtNum(hi)} {k.unit}</b></div>
      </div>
    </section>
  );
}
