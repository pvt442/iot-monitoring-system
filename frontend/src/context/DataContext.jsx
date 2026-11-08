import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { api } from '../services/api';
import { parseLocal, shiftIso, fmtTime } from '../utils/format';

export const RANGES = [
  { key: '1m', label: '1 phút', ms: 60 * 1000 },
  { key: '5m', label: '5 phút', ms: 5 * 60 * 1000 },
  { key: '15m', label: '15 phút', ms: 15 * 60 * 1000 },
  { key: '1h', label: '1 giờ', ms: 60 * 60 * 1000 },
];

const DEFAULT_PREFS = { refreshSec: 2, toast: true };
const PREFS_KEY = 'pulse.prefs';
const SELECTED_KEY = 'pulse.selectedDevice';

// LocalStorage có thể bị chặn -> luôn bọc try/catch
const readStorage = (key, fallback) => {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
};
const writeStorage = (key, value) => {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch { /* bỏ qua */ }
};

const DataContext = createContext(null);
export const useData = () => useContext(DataContext);

function pickDefaultDevice(devices) {
  if (!devices.length) return null;
  return (
    devices.find((d) => d.online && d.alertStatus === 'CRITICAL') ||
    devices.find((d) => d.online) ||
    devices[0]
  ).deviceId;
}

export function DataProvider({ children }) {
  const [overview, setOverview] = useState(null);
  const [connection, setConnection] = useState('connecting'); // connecting | online | offline
  const [error, setError] = useState(null);
  const [loaded, setLoaded] = useState(false);

  const [selectedId, setSelectedId] = useState(() => readStorage(SELECTED_KEY, null));
  const [range, setRange] = useState('5m');
  const [history, setHistory] = useState({ deviceId: null, rows: [], loading: true, error: null });

  const [prefs, setPrefsState] = useState(() => ({ ...DEFAULT_PREFS, ...readStorage(PREFS_KEY, {}) }));
  const [toasts, setToasts] = useState([]);
  const [notifications, setNotifications] = useState([]);

  // Ref giữ giá trị mới nhất cho vòng polling (tránh khởi động lại vòng lặp mỗi lần đổi chọn)
  const selectedRef = useRef(selectedId);
  const rangeRef = useRef(range);
  const prefsRef = useRef(prefs);
  selectedRef.current = selectedId;
  rangeRef.current = range;
  prefsRef.current = prefs;

  const clock = useRef({ serverMs: NaN, fetchedAt: 0 });
  const prevCrit = useRef(new Map()); // deviceId -> Set(metric đang vượt ngưỡng)
  const initialized = useRef(false);
  const prevConn = useRef('connecting');
  const idSeq = useRef(0);

  // ---------- Thông báo ----------
  const dismissToast = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), []);

  const notify = useCallback((type, title, message, { persist = true, timeIso } = {}) => {
    const id = ++idSeq.current;
    const time = timeIso || null;
    if (prefsRef.current.toast) {
      setToasts((t) => [...t.slice(-3), { id, type, title, message, time }]);
    }
    if (persist) {
      setNotifications((n) => [{ id, type, title, message, time, read: false }, ...n].slice(0, 30));
    }
  }, []);

  const markAllRead = useCallback(() => setNotifications((n) => n.map((x) => ({ ...x, read: true }))), []);
  const clearNotifications = useCallback(() => setNotifications([]), []);

  // ---------- Phát hiện cảnh báo MỚI (chống spam toast) ----------
  const detectAlerts = useCallback((ov) => {
    const seen = new Set();
    ov.devices.forEach((d) => {
      seen.add(d.deviceId);
      const now = new Set();
      if (d.online && d.latest) {
        (d.latest.alerts || []).forEach((a) => {
          if (a.level === 'CRITICAL') now.add(a.metric);
        });
      }
      const before = prevCrit.current.get(d.deviceId) || new Set();
      if (initialized.current) {
        (d.latest?.alerts || []).forEach((a) => {
          if (a.level === 'CRITICAL' && !before.has(a.metric)) {
            notify('critical', a.metric === 'TEMPERATURE' ? 'Cảnh báo nhiệt độ' : 'Cảnh báo độ ẩm', a.message,
              { timeIso: d.latest.timestamp });
          }
        });
      }
      prevCrit.current.set(d.deviceId, now);
    });
    prevCrit.current.forEach((_, id) => {
      if (!seen.has(id)) prevCrit.current.delete(id);
    });
    initialized.current = true;
  }, [notify]);

  // ---------- Một lần polling ----------
  const pollOnce = useCallback(async () => {
    let ov;
    try {
      ov = await api.getOverview();
    } catch (e) {
      setConnection('offline');
      setError(e.message);
      setLoaded(true);
      if (prevConn.current === 'online') {
        notify('warning', 'Mất kết nối Server', 'Không nhận được dữ liệu mới. Giao diện đang hiển thị số liệu cuối cùng.');
      }
      prevConn.current = 'offline';
      return;
    }

    clock.current = { serverMs: parseLocal(ov.serverTime), fetchedAt: Date.now() };
    setOverview(ov);
    setConnection('online');
    setError(null);
    setLoaded(true);
    if (prevConn.current === 'offline') {
      notify('success', 'Đã kết nối lại Server', 'Dữ liệu đang được cập nhật trở lại.', { timeIso: ov.serverTime });
    }
    prevConn.current = 'online';
    detectAlerts(ov);

    // Chọn thiết bị hợp lệ
    let sel = selectedRef.current;
    if (!sel || !ov.devices.some((d) => d.deviceId === sel)) {
      sel = pickDefaultDevice(ov.devices);
      selectedRef.current = sel;
      setSelectedId(sel);
      if (sel) writeStorage(SELECTED_KEY, sel);
    }

    // Lịch sử của thiết bị đang xem (cho biểu đồ)
    if (sel) {
      try {
        const spanMs = (RANGES.find((r) => r.key === rangeRef.current) || RANGES[1]).ms;
        const rows = await api.getHistory(sel, { from: shiftIso(ov.serverTime, -spanMs), limit: 1500 });
        setHistory({ deviceId: sel, rows: rows.slice().reverse(), loading: false, error: null });
      } catch (e) {
        setHistory((h) => ({ ...h, deviceId: sel, loading: false, error: e.message }));
      }
    } else {
      setHistory({ deviceId: null, rows: [], loading: false, error: null });
    }
  }, [detectAlerts, notify]);

  // Không cho hai lần poll chồng nhau; nếu có yêu cầu mới trong lúc đang chạy thì chạy lại ngay sau đó
  const inflight = useRef(false);
  const again = useRef(false);
  const poll = useCallback(async () => {
    if (inflight.current) {
      again.current = true;
      return;
    }
    inflight.current = true;
    try {
      do {
        again.current = false;
        await pollOnce();
      } while (again.current);
    } finally {
      inflight.current = false;
    }
  }, [pollOnce]);

  // Vòng lặp polling (HTTP Polling); chỉ khởi động lại khi đổi chu kỳ
  useEffect(() => {
    let stop = false;
    let timer;
    const loop = async () => {
      await poll();
      if (!stop) timer = setTimeout(loop, prefs.refreshSec * 1000);
    };
    loop();
    return () => {
      stop = true;
      clearTimeout(timer);
    };
  }, [poll, prefs.refreshSec]);

  // Đổi thiết bị hoặc khoảng thời gian -> lấy dữ liệu ngay, không chờ chu kỳ
  const firstRun = useRef(true);
  useEffect(() => {
    if (firstRun.current) {
      firstRun.current = false;
      return;
    }
    setHistory((h) => ({ ...h, loading: true }));
    poll();
  }, [selectedId, range, poll]);

  // ---------- API cho component ----------
  const selectDevice = useCallback((id) => {
    setSelectedId(id);
    writeStorage(SELECTED_KEY, id);
  }, []);

  const setPrefs = useCallback((patch) => {
    setPrefsState((p) => {
      const next = { ...p, ...patch };
      writeStorage(PREFS_KEY, next);
      return next;
    });
  }, []);

  /** Giờ hiện tại theo đồng hồ Server (không phụ thuộc đồng hồ/múi giờ máy khách). */
  const serverNow = useCallback(() => {
    const { serverMs, fetchedAt } = clock.current;
    return Number.isNaN(serverMs) ? NaN : serverMs + (Date.now() - fetchedAt);
  }, []);

  const ageSeconds = useCallback((iso) => {
    const t = parseLocal(iso);
    const n = serverNow();
    if (Number.isNaN(t) || Number.isNaN(n)) return NaN;
    return Math.max(0, (n - t) / 1000);
  }, [serverNow]);

  const devices = overview?.devices ?? [];
  const selected = devices.find((d) => d.deviceId === selectedId) || null;
  const unread = notifications.filter((n) => !n.read).length;

  const value = useMemo(() => ({
    overview, devices, selected, selectedId, selectDevice,
    summary: overview?.summary ?? { totalDevices: 0, online: 0, offline: 0, alerting: 0, warning: 0 },
    thresholds: overview?.thresholds ?? null,
    connection, error, loaded,
    range, setRange, history,
    prefs, setPrefs,
    toasts, dismissToast, notify,
    notifications, unread, markAllRead, clearNotifications,
    refreshNow: poll, serverNow, ageSeconds, fmtTime,
  }), [overview, devices, selected, selectedId, selectDevice, connection, error, loaded, range, history,
    prefs, setPrefs, toasts, dismissToast, notify, notifications, unread, markAllRead, clearNotifications, poll,
    serverNow, ageSeconds]);

  return <DataContext.Provider value={value}>{children}</DataContext.Provider>;
}
