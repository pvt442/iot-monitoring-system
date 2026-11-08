import { useEffect, useMemo, useState } from 'react';
import { Database, Download, RefreshCw } from 'lucide-react';
import { useData } from '../context/DataContext';
import { api } from '../services/api';
import Pagination from '../components/Pagination';
import { EmptyState, ErrorState, Skeleton } from '../components/States';
import { fmtDateTime, fmtNum, toLocalIso } from '../utils/format';

const PRESETS = [
  { key: '15m', label: '15 phút', ms: 15 * 60e3 },
  { key: '1h', label: '1 giờ', ms: 60 * 60e3 },
  { key: '6h', label: '6 giờ', ms: 6 * 60 * 60e3 },
  { key: '24h', label: '24 giờ', ms: 24 * 60 * 60e3 },
  { key: '7d', label: '7 ngày', ms: 7 * 24 * 60 * 60e3 },
  { key: 'all', label: 'Tất cả', ms: 0 },
  { key: 'custom', label: 'Tùy chọn', ms: 0 },
];

export default function HistoryPage() {
  const { devices, serverNow, notify } = useData();
  const [deviceId, setDeviceId] = useState('');
  const [preset, setPreset] = useState('1h');
  const [customFrom, setCustomFrom] = useState('');
  const [customTo, setCustomTo] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [reload, setReload] = useState(0);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [exporting, setExporting] = useState(false);

  /** Tính from/to theo giờ của Server (không lệch múi giờ máy khách). */
  const range = () => {
    if (preset === 'custom') {
      return { from: customFrom ? `${customFrom}:00` : undefined, to: customTo ? `${customTo}:59` : undefined };
    }
    const p = PRESETS.find((x) => x.key === preset);
    if (!p || !p.ms) return {};
    const base = Number.isFinite(serverNow()) ? serverNow() : Date.now();
    return { from: toLocalIso(base - p.ms) };
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    api.searchSensorData({ deviceId, ...range(), page, size })
      .then((res) => { if (!cancelled) { setData(res); setError(null); } })
      .catch((e) => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deviceId, preset, customFrom, customTo, page, size, reload]);

  const reset = (fn) => (v) => { fn(v); setPage(0); };

  const exportCsv = async () => {
    setExporting(true);
    try {
      const rows = await api.listSensorData({ deviceId, ...range(), limit: 5000 });
      const head = 'Mã bản ghi,Mã thiết bị,Nhiệt độ (°C),Độ ẩm (%),Thời gian ghi nhận\n';
      const body = rows.map((r) => `${r.dataId},${r.deviceId},${r.temperature},${r.humidity},${r.timestamp.replace('T', ' ')}`).join('\n');
      const blob = new Blob([`\uFEFF${head}${body}`], { type: 'text/csv;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `pulse-iot-lich-su-${toLocalIso(Date.now()).slice(0, 19).replace(/[-:T]/g, '')}.csv`;
      a.click();
      URL.revokeObjectURL(url);
      notify('success', 'Đã xuất CSV', rows.length >= 5000 ? 'Đã xuất tối đa 5.000 bản ghi gần nhất theo bộ lọc.' : `Đã xuất ${rows.length} bản ghi.`, { persist: false });
    } catch (e) {
      notify('warning', 'Không xuất được CSV', e.message, { persist: false });
    } finally {
      setExporting(false);
    }
  };

  const rows = data?.content ?? [];
  const deviceOptions = useMemo(() => devices.map((d) => d.deviceId), [devices]);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1 className="page-title">Lịch sử dữ liệu cảm biến</h1>
          <p className="page-desc">Tra cứu các bản ghi đo đã lưu trong MySQL theo thiết bị và khoảng thời gian. Mới nhất hiển thị trước.</p>
        </div>
        <div className="page-actions">
          <button type="button" className="btn btn-dark" onClick={() => setReload((n) => n + 1)} disabled={loading}>
            <RefreshCw className={loading ? 'spin' : ''} aria-hidden="true" /> Làm mới
          </button>
          <button type="button" className="btn btn-primary" onClick={exportCsv} disabled={exporting || !rows.length}>
            <Download aria-hidden="true" /> Xuất CSV
          </button>
        </div>
      </div>

      <section className="card" aria-label="Bảng lịch sử">
        <div className="filters">
          <label className="field">
            <span>Thiết bị</span>
            <select className="select" value={deviceId} onChange={(e) => reset(setDeviceId)(e.target.value)}>
              <option value="">Tất cả thiết bị</option>
              {deviceOptions.map((id) => <option key={id} value={id}>{id}</option>)}
            </select>
          </label>
          <label className="field">
            <span>Khoảng thời gian</span>
            <select className="select" value={preset} onChange={(e) => reset(setPreset)(e.target.value)}>
              {PRESETS.map((p) => <option key={p.key} value={p.key}>{p.key === 'all' || p.key === 'custom' ? p.label : `${p.label} gần nhất`}</option>)}
            </select>
          </label>
          {preset === 'custom' && (
            <>
              <label className="field">
                <span>Từ</span>
                <input type="datetime-local" className="input" value={customFrom} onChange={(e) => reset(setCustomFrom)(e.target.value)} />
              </label>
              <label className="field">
                <span>Đến</span>
                <input type="datetime-local" className="input" value={customTo} onChange={(e) => reset(setCustomTo)(e.target.value)} />
              </label>
            </>
          )}
        </div>

        {error ? (
          <ErrorState message={error} onRetry={() => setReload((n) => n + 1)} />
        ) : loading && !data ? (
          <div style={{ display: 'grid', gap: 10 }}>{[0, 1, 2, 3, 4].map((i) => <Skeleton key={i} h={48} />)}</div>
        ) : rows.length === 0 ? (
          <EmptyState icon={Database} title="Không có bản ghi phù hợp"
            text="Chưa có dữ liệu trong bộ lọc này. Hãy chạy IoT Client hoặc mở rộng khoảng thời gian." />
        ) : (
          <>
            <div className="table-wrap" style={{ opacity: loading ? 0.6 : 1, transition: 'opacity .2s' }}>
              <table className="data" style={{ minWidth: 620 }}>
                <thead>
                  <tr>
                    <th>Mã bản ghi</th>
                    <th>Mã thiết bị</th>
                    <th>Nhiệt độ (°C)</th>
                    <th>Độ ẩm (%)</th>
                    <th>Thời gian ghi nhận</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => {
                    const tOver = (r.alerts || []).some((a) => a.metric === 'TEMPERATURE' && a.level === 'CRITICAL');
                    const hOver = (r.alerts || []).some((a) => a.metric === 'HUMIDITY' && a.level === 'CRITICAL');
                    return (
                      <tr key={r.dataId} className={tOver || hOver ? 'row-critical' : ''}>
                        <td className="mono">#{r.dataId}</td>
                        <td><b>{r.deviceId}</b></td>
                        <td className="num t"><span className={tOver ? 'over' : ''}>{fmtNum(r.temperature, 2)}{tOver ? ' ▲' : ''}</span></td>
                        <td className="num h"><span className={hOver ? 'over' : ''}>{fmtNum(r.humidity, 2)}{hOver ? ' ▲' : ''}</span></td>
                        <td className="mono">{fmtDateTime(r.timestamp)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} size={data.size}
              onPage={setPage} onSize={(s) => { setSize(s); setPage(0); }} />
            <div className="note">▲ đánh dấu giá trị vượt ngưỡng cảnh báo hiện hành. Số đo được làm tròn 2 chữ số khi hiển thị, dữ liệu gốc trong MySQL không bị thay đổi.</div>
          </>
        )}
      </section>
    </div>
  );
}
