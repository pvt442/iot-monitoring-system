import { NavLink } from 'react-router-dom';
import { BellRing, Cpu, History, LayoutDashboard, Settings } from 'lucide-react';
import { useData } from '../context/DataContext';

// Đúng 5 mục, giữ nguyên tên tiếng Anh
const ITEMS = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/devices', label: 'Devices', icon: Cpu },
  { to: '/history', label: 'Data History', icon: History },
  { to: '/alerts', label: 'Alert Logs', icon: BellRing },
  { to: '/config', label: 'Configuration', icon: Settings },
];

export default function Sidebar({ open, onNavigate }) {
  const { connection, summary, error } = useData();

  const conn = {
    online: { cls: 'live', title: 'Server hoạt động', detail: `${summary.online}/${summary.totalDevices} thiết bị đang online` },
    offline: { cls: 'err', title: 'Mất kết nối Server', detail: error || 'Không nhận được dữ liệu' },
    connecting: { cls: 'wait', title: 'Đang kết nối…', detail: 'Chờ phản hồi từ Backend' },
  }[connection];

  return (
    <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Điều hướng chính">
      <div className="brand">
        <div className="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="#20c6ae" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
            <path d="M2 12h4l2.5-7 4 14 3-9 1.5 2H22" />
          </svg>
        </div>
        <div>
          <div className="brand-name">Pulse IoT</div>
          <div className="brand-sub">Real-time Sensor Monitoring System</div>
        </div>
      </div>

      <div className="nav-group">Điều hướng</div>
      <nav className="nav">
        {ITEMS.map(({ to, label, icon: Icon, end }) => (
          <NavLink key={to} to={to} end={end} className="nav-link" onClick={onNavigate}>
            <Icon aria-hidden="true" />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-foot">
        <div className="conn-card" role="status">
          <div className="conn-row">
            <span className={`dot ${conn.cls}`} aria-hidden="true" />
            {conn.title}
          </div>
          <div className="conn-detail">{conn.detail}</div>
        </div>
        <div className="version">IoT Data Monitor Server v1.0</div>
      </div>
    </aside>
  );
}
