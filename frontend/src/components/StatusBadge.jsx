import { AlertTriangle, CheckCircle2, Clock, WifiOff } from 'lucide-react';

const ICONS = { normal: CheckCircle2, warning: AlertTriangle, critical: AlertTriangle, offline: WifiOff, nodata: Clock };

/** Badge trạng thái: luôn có icon + chữ để không phụ thuộc vào màu sắc. state = kết quả của deviceState(). */
export default function StatusBadge({ state }) {
  const Icon = ICONS[state.key] || Clock;
  return (
    <span className={`badge ${state.tone}`}>
      <Icon aria-hidden="true" />
      {state.label}
    </span>
  );
}
