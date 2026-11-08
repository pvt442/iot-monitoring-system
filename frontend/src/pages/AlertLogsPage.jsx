import { useEffect, useState } from 'react';
import { AlertTriangle, BellRing, RefreshCw } from 'lucide-react';
import { useData } from '../context/DataContext';
import { api } from '../services/api';
import AnimatedNumber from '../components/AnimatedNumber';
import Pagination from '../components/Pagination';
import { EmptyState, ErrorState, Skeleton } from '../components/States';
import { fmtDateTime, fmtNum, fmtTime, metricLabel, metricUnit } from '../utils/format';

export default function AlertLogsPage() {
  const { devices, summary } = useData();
  const [deviceId, setDeviceId] = useState('');
  const [metric, setMetric] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [reload, setReload] = useState(0);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    api.getAlerts({ deviceId, metric, page, size })
      .then((res) => { if (!cancelled) { setData(res); setError(null); } })
      .catch((e) => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [deviceId, metric, page, size, reload]);

  // Trang đầu tự làm mới mỗi 5 giây để thấy cảnh báo mới
  useEffect(() => {
    if (page !== 0) return undefined;
    const t = setInterval(() => setReload((n) => n + 1), 5000);
    return () => clearInterval(t);
  }, [page]);

  const active = devices.filter((d) => d.online && d.alertStatus === 'CRITICAL');
  const rows = data?.content ?? [];

  return (
    <div>
      <div className="page-head">
        <div>
          <h1 className="page-title">Nhật ký cảnh báo</h1>
          <p className="page-desc">Các lần thiết bị vượt ngưỡng do Server phát hiện và lưu vào MySQL (bảng alert_logs). Mỗi lần một chỉ số chuyển từ bình thường sang vượt ngưỡng được ghi một dòng.</p>
        </div>
        <div className="page-actions">
          <button type="button" className="btn btn-dark" onClick={() => setReload((n) => n + 1)} disabled={loading}>
            <RefreshCw className={loading ? 'spin' : ''} aria-hidden="true" /> Làm mới
          </button>
        </div>
      </div>

      <div className="stat-line stagger">
        <div className="mini-stat"><span>Đã ghi nhận (theo bộ lọc)</span><b><AnimatedNumber value={data?.totalElements ?? 0} /></b></div>
        <div className="mini-stat"><span>Đang vượt ngưỡng</span><b style={{ color: summary.alerting ? '#ff8f8f' : undefined }}><AnimatedNumber value={summary.alerting} /></b></div>
        <div className="mini-stat"><span>Sắp vượt ngưỡng</span><b style={{ color: summary.warning ? 'var(--yellow)' : undefined }}><AnimatedNumber value={summary.warning} /></b></div>
      </div>

      {active.length > 0 && (
        <div className="active-alerts" aria-label="Cảnh báo đang diễn ra">
          {active.map((d) => (
            <div key={d.deviceId} className="active-alert" role="alert">
              <AlertTriangle aria-hidden="true" />
              <div>
                <b>{d.deviceId}{d.name ? ` – ${d.name}` : ''}</b>
                {(d.latest?.alerts || []).filter((a) => a.level === 'CRITICAL').map((a) => (
                  <p key={a.metric}>{a.message}</p>
                ))}
              </div>
              <span className="badge red" style={{ marginLeft: 'auto' }}>Đang diễn ra · {fmtTime(d.latest?.timestamp)}</span>
            </div>
          ))}
        </div>
      )}

      <section className="card" aria-label="Nhật ký cảnh báo">
        <div className="filters">
          <label className="field">
            <span>Thiết bị</span>
            <select className="select" value={deviceId} onChange={(e) => { setDeviceId(e.target.value); setPage(0); }}>
              <option value="">Tất cả thiết bị</option>
              {devices.map((d) => <option key={d.deviceId} value={d.deviceId}>{d.deviceId}</option>)}
            </select>
          </label>
          <label className="field">
            <span>Chỉ số</span>
            <select className="select" value={metric} onChange={(e) => { setMetric(e.target.value); setPage(0); }}>
              <option value="">Nhiệt độ và độ ẩm</option>
              <option value="TEMPERATURE">Nhiệt độ</option>
              <option value="HUMIDITY">Độ ẩm</option>
            </select>
          </label>
        </div>

        {error ? (
          <ErrorState message={error} onRetry={() => setReload((n) => n + 1)} />
        ) : loading && !data ? (
          <div style={{ display: 'grid', gap: 10 }}>{[0, 1, 2, 3].map((i) => <Skeleton key={i} h={48} />)}</div>
        ) : rows.length === 0 ? (
          <EmptyState icon={BellRing} title="Chưa có cảnh báo nào"
            text="Khi một thiết bị vượt ngưỡng, Server sẽ ghi lại ở đây. Dùng lệnh “spike DEV_01” trong IoT Client để thử." />
        ) : (
          <>
            <div className="table-wrap">
              <table className="data" style={{ minWidth: 760 }}>
                <thead>
                  <tr>
                    <th>Thời gian</th>
                    <th>Thiết bị</th>
                    <th>Chỉ số</th>
                    <th>Giá trị đo</th>
                    <th>Ngưỡng</th>
                    <th>Mức độ</th>
                    <th>Nguồn phát hiện</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((a) => (
                    <tr key={a.id} className="row-critical">
                      <td className="mono">{fmtDateTime(a.createdAt)}</td>
                      <td>
                        <b>{a.deviceId}</b>
                        {a.deviceName && <div className="cell-sub">{a.deviceName}</div>}
                      </td>
                      <td>{metricLabel(a.metric)}</td>
                      <td className="num"><span className="over">{fmtNum(a.value)} {metricUnit(a.metric)}</span></td>
                      <td className="mono">&gt; {fmtNum(a.threshold)} {metricUnit(a.metric)}</td>
                      <td><span className="badge red"><AlertTriangle aria-hidden="true" />Vượt ngưỡng</span></td>
                      <td><span className="badge cyan">Backend · MySQL</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} size={data.size}
              onPage={setPage} onSize={(s) => { setSize(s); setPage(0); }} sizes={[10, 20, 50]} />
          </>
        )}
      </section>
    </div>
  );
}
