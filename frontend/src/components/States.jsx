import { AlertTriangle, Inbox, RefreshCw } from 'lucide-react';

export const Skeleton = ({ h = 20, w = '100%', style }) => (
  <div className="skeleton" style={{ height: h, width: w, ...style }} aria-hidden="true" />
);

export function EmptyState({ icon: Icon = Inbox, title, text, action }) {
  return (
    <div className="state-box">
      <div className="ico"><Icon aria-hidden="true" /></div>
      <h3>{title}</h3>
      {text && <p>{text}</p>}
      {action}
    </div>
  );
}

export function ErrorState({ message, onRetry, title = 'Không tải được dữ liệu' }) {
  return (
    <div className="state-box error" role="alert">
      <div className="ico"><AlertTriangle aria-hidden="true" /></div>
      <h3>{title}</h3>
      <p>{message}</p>
      {onRetry && (
        <button type="button" className="btn btn-primary btn-sm" onClick={onRetry}>
          <RefreshCw aria-hidden="true" /> Thử lại
        </button>
      )}
    </div>
  );
}
