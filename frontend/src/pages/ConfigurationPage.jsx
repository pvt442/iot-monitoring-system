import { useEffect, useState } from 'react';
import { RotateCcw, Save } from 'lucide-react';
import { useData } from '../context/DataContext';
import { api } from '../services/api';
import Modal from '../components/Modal';
import { ErrorState, Skeleton } from '../components/States';
import { fmtDateTime } from '../utils/format';

const FIELDS = [
  { key: 'temperatureThreshold', label: 'Ngưỡng nhiệt độ (°C)', hint: 'Cảnh báo khi nhiệt độ > giá trị này. Mặc định 35.', min: -40, max: 125, step: 0.5 },
  { key: 'humidityThreshold', label: 'Ngưỡng độ ẩm (%)', hint: 'Cảnh báo khi độ ẩm > giá trị này. Mặc định 80.', min: 0, max: 100, step: 0.5 },
  { key: 'temperatureWarningMargin', label: 'Biên “sắp vượt ngưỡng” – nhiệt độ (°C)', hint: 'Từ (ngưỡng − biên) đến ngưỡng hiển thị mức “Sắp vượt ngưỡng”. Mặc định 3.', min: 0, max: 50, step: 0.5 },
  { key: 'humidityWarningMargin', label: 'Biên “sắp vượt ngưỡng” – độ ẩm (%)', hint: 'Mặc định 5.', min: 0, max: 50, step: 0.5 },
  { key: 'offlineTimeoutSeconds', label: 'Timeout Offline (giây)', hint: 'Quá thời gian này không nhận dữ liệu thì thiết bị là Offline. Mặc định 15.', min: 3, max: 3600, step: 1 },
];

const toForm = (c) => Object.fromEntries(FIELDS.map((f) => [f.key, String(c[f.key])]));

export default function ConfigurationPage() {
  const { prefs, setPrefs, refreshNow, notify } = useData();
  const [form, setForm] = useState(null);
  const [saved, setSaved] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);

  const load = () => {
    setLoadError(null);
    api.getConfig()
      .then((c) => { setSaved(c); setForm(toForm(c)); })
      .catch((e) => setLoadError(e.message));
  };
  useEffect(load, []);

  const validate = () => {
    const out = {};
    FIELDS.forEach((f) => {
      const raw = form[f.key].trim().replace(',', '.');
      const v = Number(raw);
      if (raw === '' || Number.isNaN(v)) out[f.key] = 'Hãy nhập một số.';
      else if (v < f.min || v > f.max) out[f.key] = `Giá trị phải trong khoảng ${f.min} đến ${f.max}.`;
    });
    setErrors(out);
    return Object.keys(out).length === 0;
  };

  const save = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setBusy(true);
    try {
      const body = Object.fromEntries(FIELDS.map((f) => [f.key, Number(form[f.key].trim().replace(',', '.'))]));
      body.offlineTimeoutSeconds = Math.round(body.offlineTimeoutSeconds);
      const c = await api.updateConfig(body);
      setSaved(c);
      setForm(toForm(c));
      notify('success', 'Đã lưu cấu hình', 'Ngưỡng mới đã được ghi vào MySQL và áp dụng cho mọi thiết bị.', { persist: false });
      refreshNow();
    } catch (err) {
      notify('warning', 'Không lưu được cấu hình', err.message, { persist: false });
    } finally {
      setBusy(false);
    }
  };

  const reset = async () => {
    setBusy(true);
    try {
      const c = await api.resetConfig();
      setSaved(c);
      setForm(toForm(c));
      setErrors({});
      setPrefs({ refreshSec: 2, toast: true });
      setConfirmReset(false);
      notify('success', 'Đã khôi phục mặc định', 'Ngưỡng, timeout và tùy chọn hiển thị đã về giá trị ban đầu.', { persist: false });
      refreshNow();
    } catch (err) {
      notify('warning', 'Không khôi phục được', err.message, { persist: false });
    } finally {
      setBusy(false);
    }
  };

  const dirty = form && saved && FIELDS.some((f) => Number(form[f.key].replace(',', '.')) !== saved[f.key]);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1 className="page-title">Cài đặt cấu hình</h1>
          <p className="page-desc">Thiết lập ngưỡng cảnh báo cho cả hệ thống và tùy chọn hiển thị của trình duyệt này.</p>
        </div>
        <div className="page-actions">
          <button type="button" className="btn btn-ghost" onClick={() => setConfirmReset(true)} disabled={busy || !form}>
            <RotateCcw aria-hidden="true" /> Khôi phục mặc định
          </button>
        </div>
      </div>

      <div className="grid stagger">
        <div className="col-8">
          <section className="card" aria-label="Ngưỡng cảnh báo">
            <div className="card-head">
              <div>
                <div className="card-title">
                  Ngưỡng cảnh báo &amp; trạng thái thiết bị
                  <span className="badge cyan scope-tag">Lưu trong MySQL · dùng chung</span>
                </div>
                <div className="card-sub">
                  {saved ? `Cập nhật lần cuối: ${fmtDateTime(saved.updatedAt)}` : 'Đang tải cấu hình…'}
                </div>
              </div>
            </div>

            {loadError ? (
              <ErrorState message={loadError} onRetry={load} />
            ) : !form ? (
              <div style={{ display: 'grid', gap: 12 }}>{[0, 1, 2].map((i) => <Skeleton key={i} h={64} />)}</div>
            ) : (
              <form onSubmit={save} noValidate>
                <div className="config-grid">
                  {FIELDS.map((f) => (
                    <label key={f.key} className="field">
                      <span>{f.label}</span>
                      <input className={`input ${errors[f.key] ? 'invalid' : ''}`} inputMode="decimal" value={form[f.key]}
                        onChange={(e) => setForm({ ...form, [f.key]: e.target.value })} />
                      {errors[f.key] ? <span className="field-error" role="alert">{errors[f.key]}</span> : <span className="field-hint">{f.hint}</span>}
                    </label>
                  ))}
                </div>
                <div className="form-actions">
                  <button type="submit" className="btn btn-emerald" disabled={busy || !dirty}>
                    <Save aria-hidden="true" /> Lưu cấu hình
                  </button>
                  {dirty && <button type="button" className="btn btn-ghost" onClick={() => { setForm(toForm(saved)); setErrors({}); }}>Hoàn tác thay đổi</button>}
                </div>
                <div className="note">
                  Backend so sánh nghiêm ngặt: nhiệt độ <b>&gt;</b> ngưỡng hoặc độ ẩm <b>&gt;</b> ngưỡng là “Vượt ngưỡng”. Cảnh báo chỉ được ghi vào nhật ký khi chỉ số mới chuyển từ bình thường sang vượt ngưỡng.
                </div>
              </form>
            )}
          </section>
        </div>

        <div className="col-4">
          <section className="card" style={{ height: '100%' }} aria-label="Tùy chọn hiển thị">
            <div className="card-title">Tùy chọn hiển thị</div>
            <div className="card-sub"><span className="badge muted">Chỉ áp dụng trên trình duyệt này</span></div>

            <div className="setting-row" style={{ marginTop: 10 }}>
              <div>
                <b>Chu kỳ cập nhật</b>
                <small>Tần suất Dashboard hỏi Server (HTTP Polling).</small>
              </div>
              <select className="select" style={{ width: 'auto' }} value={prefs.refreshSec}
                onChange={(e) => setPrefs({ refreshSec: Number(e.target.value) })} aria-label="Chu kỳ cập nhật">
                {[1, 2, 3, 5, 10].map((s) => <option key={s} value={s}>{s} giây</option>)}
              </select>
            </div>
            <div className="setting-row">
              <div>
                <b>Thông báo nổi (Toast)</b>
                <small>Hiện hộp thông báo khi có thiết bị mới vượt ngưỡng.</small>
              </div>
              <button type="button" role="switch" className="switch" aria-checked={prefs.toast} aria-label="Bật tắt thông báo nổi"
                onClick={() => setPrefs({ toast: !prefs.toast })} />
            </div>
            <div className="note">
              Hai tùy chọn này được lưu bằng LocalStorage của trình duyệt, không ghi vào MySQL và không ảnh hưởng người dùng khác.
            </div>
          </section>
        </div>
      </div>

      {confirmReset && (
        <Modal title="Khôi phục cấu hình mặc định?"
          description="Ngưỡng 35 °C / 80 %, biên 3 °C / 5 %, timeout 15 giây và chu kỳ cập nhật 2 giây sẽ được áp dụng lại cho toàn hệ thống."
          onClose={() => setConfirmReset(false)}
          actions={(
            <>
              <button type="button" className="btn btn-ghost" onClick={() => setConfirmReset(false)}>Hủy</button>
              <button type="button" className="btn btn-primary" onClick={reset} disabled={busy}>Khôi phục</button>
            </>
          )} />
      )}
    </div>
  );
}
