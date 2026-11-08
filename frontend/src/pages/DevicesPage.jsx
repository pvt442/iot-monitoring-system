import { useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Cpu, Plus, Search, Trash2 } from 'lucide-react';
import { useData } from '../context/DataContext';
import { api } from '../services/api';
import DeviceTable from '../components/DeviceTable';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, Skeleton } from '../components/States';
import { parseLocal } from '../utils/format';

const ID_RE = /^[A-Za-z0-9_.-]{1,50}$/;
const FILTERS = [
  { key: 'all', label: 'Tất cả' },
  { key: 'online', label: 'Online' },
  { key: 'offline', label: 'Offline' },
  { key: 'alert', label: 'Cảnh báo' },
];

export default function DevicesPage() {
  const { loaded, overview, error, devices, summary, selectDevice, refreshNow, notify } = useData();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const q = params.get('q') || '';
  const [filter, setFilter] = useState('all');
  const [sort, setSort] = useState('recent');

  const [adding, setAdding] = useState(false);
  const [form, setForm] = useState({ deviceId: '', name: '', location: '' });
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);
  const [removing, setRemoving] = useState(null);

  const list = useMemo(() => {
    const needle = q.trim().toLowerCase();
    let out = devices.filter((d) => {
      if (needle && !`${d.deviceId} ${d.name || ''}`.toLowerCase().includes(needle)) return false;
      if (filter === 'online') return d.online;
      if (filter === 'offline') return !d.online;
      if (filter === 'alert') return d.online && d.alertStatus === 'CRITICAL';
      return true;
    });
    out = out.slice();
    if (sort === 'id') out.sort((a, b) => a.deviceId.localeCompare(b.deviceId));
    else out.sort((a, b) => (parseLocal(b.lastSeen) || 0) - (parseLocal(a.lastSeen) || 0));
    return out;
  }, [devices, q, filter, sort]);

  const setQuery = (v) => {
    const next = new URLSearchParams(params);
    if (v) next.set('q', v); else next.delete('q');
    setParams(next, { replace: true });
  };

  const openAdd = () => {
    setForm({ deviceId: '', name: '', location: '' });
    setFormError('');
    setAdding(true);
  };

  const submitAdd = async (e) => {
    e.preventDefault();
    const id = form.deviceId.trim();
    if (!ID_RE.test(id)) {
      setFormError('Mã thiết bị bắt buộc, tối đa 50 ký tự, chỉ gồm chữ, số và các ký tự _ - . (ví dụ DEV_04).');
      return;
    }
    setBusy(true);
    try {
      await api.createDevice({ deviceId: id, name: form.name.trim() || undefined, location: form.location.trim() || undefined });
      notify('success', 'Đã lưu thiết bị', `Thiết bị ${id} đã được đăng ký trong hệ thống.`, { persist: false });
      setAdding(false);
      refreshNow();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const confirmRemove = async () => {
    setBusy(true);
    try {
      await api.deleteDevice(removing.deviceId);
      notify('success', 'Đã xóa thiết bị', `Đã xóa ${removing.deviceId} cùng toàn bộ dữ liệu cảm biến liên quan.`, { persist: false });
      setRemoving(null);
      refreshNow();
    } catch (err) {
      notify('warning', 'Không xóa được thiết bị', err.message, { persist: false });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <div className="page-head">
        <div>
          <h1 className="page-title">Quản lý thiết bị</h1>
          <p className="page-desc">Danh sách thiết bị IoT, trạng thái kết nối và số đo mới nhất. Online nghĩa là Server vừa nhận dữ liệu trong khoảng timeout đã cấu hình.</p>
        </div>
        <div className="page-actions">
          <button type="button" className="btn btn-primary" onClick={openAdd}>
            <Plus aria-hidden="true" /> Thêm thiết bị
          </button>
        </div>
      </div>

      <section className="card stagger-none" aria-label="Danh sách thiết bị">
        <div className="toolbar">
          <div className="segmented" role="group" aria-label="Lọc trạng thái">
            {FILTERS.map((f) => (
              <button key={f.key} type="button" aria-pressed={filter === f.key} onClick={() => setFilter(f.key)}>
                {f.label}
                {f.key === 'online' && ` (${summary.online})`}
                {f.key === 'offline' && ` (${summary.offline})`}
                {f.key === 'alert' && ` (${summary.alerting})`}
              </button>
            ))}
          </div>
          <div className="spacer" />
          <div className="input-wrap">
            <Search aria-hidden="true" />
            <input className="input pill-input" placeholder="Tìm theo mã hoặc tên…" value={q}
              onChange={(e) => setQuery(e.target.value)} aria-label="Tìm thiết bị" />
          </div>
          <select className="select" style={{ width: 'auto', borderRadius: 999, background: '#0c0f12' }} value={sort}
            onChange={(e) => setSort(e.target.value)} aria-label="Sắp xếp">
            <option value="recent">Cập nhật gần nhất</option>
            <option value="id">Mã thiết bị (A–Z)</option>
          </select>
        </div>

        {!loaded ? (
          <div style={{ display: 'grid', gap: 10 }}>{[0, 1, 2, 3].map((i) => <Skeleton key={i} h={54} />)}</div>
        ) : !overview ? (
          <ErrorState message={error} onRetry={refreshNow} title="Chưa kết nối được Backend" />
        ) : devices.length === 0 ? (
          <EmptyState icon={Cpu} title="Chưa có thiết bị nào"
            text="Thêm thiết bị bằng nút “Thêm thiết bị”, hoặc chạy IoT Client để thiết bị tự đăng ký."
            action={<button type="button" className="btn btn-primary btn-sm" onClick={openAdd}>Thêm thiết bị</button>} />
        ) : list.length === 0 ? (
          <EmptyState icon={Search} title="Không có thiết bị phù hợp" text="Thử đổi bộ lọc hoặc từ khóa tìm kiếm." />
        ) : (
          <DeviceTable devices={list}
            onView={(d) => { selectDevice(d.deviceId); navigate('/'); }}
            onDelete={(d) => setRemoving(d)} />
        )}
      </section>

      {adding && (
        <Modal title="Thêm thiết bị" description="Đăng ký thiết bị để Server nhận dữ liệu từ Device ID này." onClose={() => setAdding(false)}
          actions={(
            <>
              <button type="button" className="btn btn-ghost" onClick={() => setAdding(false)}>Hủy</button>
              <button type="submit" form="add-device" className="btn btn-emerald" disabled={busy}>Lưu thiết bị</button>
            </>
          )}>
          <form id="add-device" className="form" onSubmit={submitAdd} noValidate>
            <label className="field">
              <span>Mã thiết bị *</span>
              <input className={`input ${formError ? 'invalid' : ''}`} value={form.deviceId} autoFocus placeholder="DEV_04"
                onChange={(e) => setForm({ ...form, deviceId: e.target.value })} />
            </label>
            <label className="field">
              <span>Tên thiết bị</span>
              <input className="input" value={form.name} placeholder="Cảm biến phòng máy chủ"
                onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </label>
            <label className="field">
              <span>Vị trí</span>
              <input className="input" value={form.location} placeholder="Phòng máy chủ A"
                onChange={(e) => setForm({ ...form, location: e.target.value })} />
            </label>
            {formError && <div className="field-error" role="alert">{formError}</div>}
          </form>
        </Modal>
      )}

      {removing && (
        <Modal title={`Xóa thiết bị ${removing.deviceId}?`}
          description="Toàn bộ dữ liệu cảm biến và nhật ký cảnh báo của thiết bị này cũng sẽ bị xóa khỏi MySQL. Không thể hoàn tác."
          onClose={() => setRemoving(null)}
          actions={(
            <>
              <button type="button" className="btn btn-ghost" onClick={() => setRemoving(null)}>Hủy</button>
              <button type="button" className="btn btn-danger" disabled={busy} onClick={confirmRemove}>
                <Trash2 aria-hidden="true" /> Xóa thiết bị
              </button>
            </>
          )} />
      )}
    </div>
  );
}
