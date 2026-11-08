import { useEffect, useRef, useState } from 'react';
import { AlertTriangle, CheckCircle2, Info, X } from 'lucide-react';
import { useData } from '../context/DataContext';
import { fmtTime } from '../utils/format';

const ICON = { critical: AlertTriangle, warning: AlertTriangle, success: CheckCircle2, info: Info };

function Toast({ toast, onClose }) {
  const [paused, setPaused] = useState(false);
  const duration = toast.type === 'critical' ? 8000 : 6000;

  // Giữ onClose trong ref: nếu không, mỗi lần polling render lại sẽ reset bộ đếm và toast không bao giờ tự tắt
  const closeRef = useRef(onClose);
  closeRef.current = onClose;

  useEffect(() => {
    if (paused) return undefined;
    const t = setTimeout(() => closeRef.current(), duration);
    return () => clearTimeout(t);
  }, [paused, duration]);

  const Icon = ICON[toast.type] || Info;
  return (
    <div className={`toast ${toast.type}`} role={toast.type === 'critical' ? 'alert' : 'status'}
      onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}>
      <div className="t-ico"><Icon aria-hidden="true" /></div>
      <div>
        <div className="t-title">{toast.title}</div>
        <div className="t-msg">{toast.message}</div>
        {toast.time && <div className="t-time">Lúc {fmtTime(toast.time)}</div>}
      </div>
      <button type="button" className="t-close" onClick={onClose} aria-label="Đóng thông báo">
        <X size={16} aria-hidden="true" />
      </button>
      <span className="t-bar" style={{ animationDuration: `${duration}ms`, animationPlayState: paused ? 'paused' : 'running' }} />
    </div>
  );
}

/** Toast trượt vào từ góc phải; tạm dừng đếm giờ khi rê chuột; tối đa 4 thông báo cùng lúc. */
export default function ToastHost() {
  const { toasts, dismissToast } = useData();
  return (
    <div className="toast-host" aria-live="polite">
      {toasts.map((t) => (
        <Toast key={t.id} toast={t} onClose={() => dismissToast(t.id)} />
      ))}
    </div>
  );
}
