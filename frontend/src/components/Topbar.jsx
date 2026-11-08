import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, Bell, CheckCircle2, Clock, Info, Menu, Search } from 'lucide-react';
import { useData } from '../context/DataContext';
import { useNow } from '../hooks/useNow';
import { fmtTime } from '../utils/format';

const pad = (n) => String(n).padStart(2, '0');
const ICON = { critical: AlertTriangle, warning: AlertTriangle, success: CheckCircle2, info: Info };

export default function Topbar({ onMenu }) {
  const navigate = useNavigate();
  const now = new Date(useNow());
  const { notifications, unread, markAllRead, clearNotifications } = useData();
  const [q, setQ] = useState('');
  const [open, setOpen] = useState(false);
  const boxRef = useRef(null);

  // Đóng dropdown khi bấm ra ngoài hoặc nhấn Escape
  useEffect(() => {
    if (!open) return undefined;
    const onDown = (e) => boxRef.current && !boxRef.current.contains(e.target) && setOpen(false);
    const onKey = (e) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const submit = (e) => {
    e.preventDefault();
    navigate(`/devices${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ''}`);
  };

  const toggleBell = () => {
    setOpen((o) => !o);
    if (!open) markAllRead();
  };

  const clockText = `${pad(now.getDate())}/${pad(now.getMonth() + 1)}/${now.getFullYear()} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;

  return (
    <header className="topbar">
      <button type="button" className="icon-btn menu-btn" onClick={onMenu} aria-label="Mở menu">
        <Menu aria-hidden="true" />
      </button>

      <form className="search" role="search" onSubmit={submit}>
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Tìm thiết bị theo mã hoặc tên…" aria-label="Tìm thiết bị" />
        <button type="submit" className="btn-icon" style={{ background: 'none', border: 0, color: 'inherit', display: 'grid' }} aria-label="Tìm kiếm">
          <Search aria-hidden="true" />
        </button>
      </form>

      <div className="pill clock" title="Giờ trên máy của bạn">
        <Clock aria-hidden="true" />
        <span className="pill-text">{clockText}</span>
      </div>

      <div ref={boxRef} style={{ position: 'relative' }}>
        <button type="button" className="icon-btn" onClick={toggleBell} aria-label={`Thông báo${unread ? `, ${unread} chưa đọc` : ''}`} aria-expanded={open}>
          <Bell aria-hidden="true" />
          {unread > 0 && <span className="badge-dot">{unread > 9 ? '9+' : unread}</span>}
        </button>
        {open && (
          <div className="dropdown" role="dialog" aria-label="Thông báo">
            <div className="dd-head">
              Thông báo
              {notifications.length > 0 && (
                <button type="button" className="btn btn-ghost btn-sm" onClick={clearNotifications}>Xóa tất cả</button>
              )}
            </div>
            {notifications.length === 0 ? (
              <div className="state-box" style={{ padding: '28px 10px' }}>
                <p>Chưa có thông báo nào trong phiên này.</p>
              </div>
            ) : (
              notifications.map((n) => {
                const Icon = ICON[n.type] || Info;
                return (
                  <div key={n.id} className={`dd-item ${n.type} ${n.read ? '' : 'unread'}`}>
                    <div className="t-ico"><Icon aria-hidden="true" /></div>
                    <div>
                      <b>{n.title}</b>
                      <p>{n.message}</p>
                      {n.time && <small>Lúc {fmtTime(n.time)}</small>}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        )}
      </div>
    </header>
  );
}
