import { Link } from 'react-router-dom';
import { AlertTriangle, CheckCircle2, Clock, Cpu, MapPin, RefreshCw, Server, Wifi, WifiOff } from 'lucide-react';
import { RANGES, useData } from '../context/DataContext';
import { useNow } from '../hooks/useNow';
import OrbArt from '../components/OrbArt';
import AnimatedNumber from '../components/AnimatedNumber';
import SensorCard from '../components/SensorCard';
import DeviceSelector from '../components/DeviceSelector';
import DeviceTable from '../components/DeviceTable';
import TrendChart from '../components/TrendChart';
import StatusBadge from '../components/StatusBadge';
import { EmptyState, ErrorState, Skeleton } from '../components/States';
import { fmtDate, fmtNum, fmtTime, parseLocal, relTime } from '../utils/format';
import { deviceState } from '../utils/state';

function linkInfo(connection, selected) {
  if (connection === 'offline') return { key: 'offline', label: 'Mất kết nối Server', tone: 'red' };
  if (!selected || !selected.lastSeen) return { key: 'nodata', label: 'Chưa có dữ liệu', tone: 'muted' };
  if (selected.online) return { key: 'normal', label: 'Đang nhận dữ liệu', tone: 'green' };
  return { key: 'offline', label: 'Dữ liệu đã cũ', tone: 'muted' };
}

function DashboardSkeleton() {
  return (
    <div className="grid" aria-busy="true" aria-label="Đang tải dữ liệu">
      <div className="col-8"><Skeleton h={330} style={{ borderRadius: 22 }} /></div>
      <div className="col-4"><Skeleton h={330} style={{ borderRadius: 22 }} /></div>
      <div className="col-12"><Skeleton h={78} /></div>
      <div className="col-4"><Skeleton h={380} style={{ borderRadius: 18 }} /></div>
      <div className="col-4"><Skeleton h={380} style={{ borderRadius: 18 }} /></div>
      <div className="col-4"><Skeleton h={380} style={{ borderRadius: 18 }} /></div>
    </div>
  );
}

export default function DashboardPage() {
  const {
    loaded, overview, connection, error, devices, selected, selectedId, selectDevice, summary, thresholds,
    history, range, setRange, refreshNow, ageSeconds,
  } = useData();
  useNow(); // "x giây trước" cập nhật mỗi giây

  if (!loaded) return <DashboardSkeleton />;
  if (!overview) {
    return (
      <div className="card">
        <ErrorState message={error || 'Không kết nối được Server.'} onRetry={refreshNow} title="Chưa kết nối được Backend" />
      </div>
    );
  }

  const stale = connection === 'offline';
  const timeout = thresholds?.offlineTimeoutSeconds ?? 15;
  const rows = history.deviceId === selectedId ? history.rows : [];
  const rangeLabel = (RANGES.find((r) => r.key === range) || RANGES[1]).label;
  const latest = selected?.latest ?? null;
  const link = linkInfo(connection, selected);
  const selState = deviceState(selected);
  const sensorOffline = !!selected && !selected.online;
  const pctOnline = summary.totalDevices ? Math.round((summary.online / summary.totalDevices) * 100) : 0;

  const recent = devices
    .slice()
    .sort((a, b) => (parseLocal(b.lastSeen) || 0) - (parseLocal(a.lastSeen) || 0))
    .slice(0, 8);

  const avg = (field) => {
    const v = rows.map((r) => r[field]).filter(Number.isFinite);
    return v.length ? v.reduce((a, b) => a + b, 0) / v.length : NaN;
  };

  return (
    <div>
      {stale && (
        <div className="banner error" role="alert">
          <WifiOff aria-hidden="true" />
          Mất kết nối Server – đang hiển thị số liệu cuối cùng nhận được. {error}
          <button type="button" className="btn btn-primary btn-sm" onClick={refreshNow}>
            <RefreshCw aria-hidden="true" /> Thử lại
          </button>
        </div>
      )}

      <div className="grid stagger">
        {/* ===== Hero + thẻ thống kê ===== */}
        <div className="col-8">
          <section className="hero" aria-label="Tổng quan hệ thống">
            <div className="hero-art"><OrbArt /></div>
            <h1 className="hero-title">Giám sát cảm biến thời gian thực</h1>
            <p className="hero-desc">
              Theo dõi nhiệt độ, độ ẩm và tình trạng kết nối của các thiết bị IoT đang gửi dữ liệu về Server.
            </p>

            <div className="hero-stats">
              <div className="glass tone-cyan">
                <div className="glass-top"><span className="glass-ico"><Cpu aria-hidden="true" /></span></div>
                <div className="glass-label">Tổng số thiết bị</div>
                <div className="glass-value"><AnimatedNumber value={summary.totalDevices} /></div>
                <div className="glass-foot">Thiết bị đã đăng ký</div>
              </div>
              <div className="glass tone-green">
                <div className="glass-top">
                  <span className="glass-ico"><Wifi aria-hidden="true" /></span>
                  {summary.online > 0 && <span className="dot live" aria-hidden="true" />}
                </div>
                <div className="glass-label">Đang Online</div>
                <div className="glass-value"><AnimatedNumber value={summary.online} /></div>
                <div className="glass-foot">{pctOnline}% tổng số thiết bị</div>
              </div>
              <div className="glass tone-gray">
                <div className="glass-top"><span className="glass-ico"><WifiOff aria-hidden="true" /></span></div>
                <div className="glass-label">Đang Offline</div>
                <div className="glass-value"><AnimatedNumber value={summary.offline} /></div>
                <div className="glass-foot">Quá {timeout} giây không có dữ liệu</div>
              </div>
              <div className={`glass ${summary.alerting > 0 ? 'tone-red is-critical' : 'tone-green'}`}>
                <div className="glass-top">
                  <span className="glass-ico">
                    {summary.alerting > 0 ? <AlertTriangle aria-hidden="true" /> : <CheckCircle2 aria-hidden="true" />}
                  </span>
                  {summary.alerting > 0 && <span className="badge red">Cần kiểm tra</span>}
                </div>
                <div className="glass-label">Cảnh báo hiện tại</div>
                <div className="glass-value"><AnimatedNumber value={summary.alerting} /></div>
                <div className="glass-foot">
                  {summary.alerting > 0 ? 'Thiết bị đang vượt ngưỡng' : summary.warning > 0 ? `${summary.warning} thiết bị sắp vượt ngưỡng` : 'Mọi thiết bị ổn định'}
                </div>
              </div>
            </div>
          </section>
        </div>

        {/* ===== Panel thiết bị đang xem ===== */}
        <div className="col-4">
          <section className="focus-panel" aria-label="Thiết bị đang xem">
            <div className="focus-art"><OrbArt /></div>
            <div className="focus-label">Thiết bị đang xem</div>
            {selected ? (
              <>
                <div className="focus-code">{selected.deviceId}</div>
                <div style={{ marginTop: 8, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                  <StatusBadge state={link} />
                  {selState.key !== 'normal' && selState.key !== 'nodata' && selState.key !== 'offline' && <StatusBadge state={selState} />}
                </div>
                <div className="focus-time" title={selected.lastSeen ? `${fmtDate(selected.lastSeen)} ${fmtTime(selected.lastSeen)}` : ''}>
                  {fmtTime(selected.lastSeen)}
                </div>
                <div className="focus-rel">
                  Lần nhận dữ liệu gần nhất · {selected.lastSeen ? relTime(ageSeconds(selected.lastSeen)) : 'chưa có dữ liệu'}
                </div>
                <div className="focus-meta">
                  <div><Cpu aria-hidden="true" />{selected.name || 'Chưa đặt tên'}</div>
                  <div><MapPin aria-hidden="true" />{selected.location || 'Chưa có vị trí'}</div>
                  <div><Clock aria-hidden="true" />{selected.lastSeen ? `${fmtDate(selected.lastSeen)} ${fmtTime(selected.lastSeen)}` : '—'}</div>
                </div>
              </>
            ) : (
              <p className="hero-desc" style={{ marginTop: 14 }}>Chưa có thiết bị nào để hiển thị.</p>
            )}
          </section>
        </div>

        {devices.length === 0 ? (
          <div className="col-12 card">
            <EmptyState icon={Server} title="Chưa có thiết bị nào"
              text="Chạy IoT Client (thư mục iot-client) để tự đăng ký thiết bị và gửi dữ liệu, hoặc thêm thiết bị ở trang Devices."
              action={<Link to="/devices" className="btn btn-primary btn-sm">Đến trang Devices</Link>} />
          </div>
        ) : (
          <>
            {/* ===== Chọn thiết bị ===== */}
            <div className="col-12">
              <DeviceSelector devices={devices} selectedId={selectedId} onSelect={selectDevice} />
            </div>

            {/* ===== Số đo hiện tại ===== */}
            <div className="col-4">
              <SensorCard kind="temp" latest={latest} threshold={thresholds?.temperatureThreshold}
                rows={rows} rangeLabel={rangeLabel} offline={sensorOffline} />
            </div>
            <div className="col-4">
              <SensorCard kind="hum" latest={latest} threshold={thresholds?.humidityThreshold}
                rows={rows} rangeLabel={rangeLabel} offline={sensorOffline} />
            </div>
            <div className="col-4">
              <section className="card" style={{ height: '100%' }} aria-label="Tóm tắt thiết bị">
                <div className="card-title">Tóm tắt thiết bị</div>
                <div className="card-sub">Khung thời gian: {rangeLabel} gần nhất</div>
                <div style={{ display: 'grid', gap: 12, marginTop: 16 }}>
                  <div className="mini-stat"><span>Số bản ghi nhận được</span><b><AnimatedNumber value={rows.length} /></b></div>
                  <div className="mini-stat"><span>Nhiệt độ trung bình</span><b style={{ color: 'var(--amber)' }}>{fmtNum(avg('temperature'))} °C</b></div>
                  <div className="mini-stat"><span>Độ ẩm trung bình</span><b style={{ color: 'var(--cyan)' }}>{fmtNum(avg('humidity'))} %</b></div>
                </div>
                {latest && (latest.alerts || []).length > 0 && !sensorOffline && (
                  <div className="note" style={{ borderLeft: '3px solid var(--red)' }}>
                    {latest.alerts.map((a) => <div key={a.metric}>{a.message}</div>)}
                  </div>
                )}
              </section>
            </div>

            {/* ===== Biểu đồ xu hướng ===== */}
            <div className="col-12">
              <section className="card chart-card" aria-label="Biểu đồ xu hướng">
                <div className="card-head">
                  <div>
                    <div className="card-title">Biểu đồ xu hướng – {selectedId}</div>
                    <div className="card-sub">
                      Cập nhật tự động · hiển thị {rowsLabel(rows.length)}
                      {history.error && <span style={{ color: '#ff8f8f' }}> · Lỗi tải lịch sử: {history.error}</span>}
                    </div>
                  </div>
                  <div className="segmented" role="group" aria-label="Khoảng thời gian">
                    {RANGES.map((r) => (
                      <button key={r.key} type="button" aria-pressed={range === r.key} onClick={() => setRange(r.key)}>
                        {r.label}
                      </button>
                    ))}
                  </div>
                </div>
                {history.loading && rows.length === 0 ? (
                  <Skeleton h={270} />
                ) : (
                  <div className="grid">
                    <div className="col-6">
                      <div className="chart-legend"><span><i style={{ background: 'var(--amber)' }} />Nhiệt độ (°C)</span></div>
                      <div className="chart-box">
                        <TrendChart rows={rows} field="temperature" color="#f59e0b" unit="°C" label="Nhiệt độ"
                          threshold={thresholds?.temperatureThreshold} />
                      </div>
                    </div>
                    <div className="col-6">
                      <div className="chart-legend"><span><i style={{ background: 'var(--cyan)' }} />Độ ẩm (%)</span></div>
                      <div className="chart-box">
                        <TrendChart rows={rows} field="humidity" color="#39bde0" unit="%" label="Độ ẩm"
                          threshold={thresholds?.humidityThreshold} />
                      </div>
                    </div>
                  </div>
                )}
              </section>
            </div>

            {/* ===== Bảng thiết bị gần đây ===== */}
            <div className="col-12">
              <section className="card" aria-label="Thiết bị hoạt động gần đây">
                <div className="card-head">
                  <div>
                    <div className="card-title">Thiết bị hoạt động gần đây</div>
                    <div className="card-sub">Sắp xếp theo lần nhận dữ liệu cuối</div>
                  </div>
                  <Link to="/devices" className="btn btn-dark btn-sm">Xem tất cả</Link>
                </div>
                <DeviceTable devices={recent} onView={(d) => { selectDevice(d.deviceId); window.scrollTo({ top: 0, behavior: 'smooth' }); }} />
              </section>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function rowsLabel(n) {
  return `${n.toLocaleString('vi-VN')} bản ghi`;
}
