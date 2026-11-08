// Thời gian từ Backend có dạng "2026-11-07T14:35:02.123" (giờ của Server, không có múi giờ).
// Chỉ cắt chuỗi để hiển thị -> không bị lệch múi giờ trình duyệt.
export const fmtTime = (s) => (s ? s.slice(11, 19) : '--:--:--');
export const fmtDate = (s) => (s ? `${s.slice(8, 10)}/${s.slice(5, 7)}/${s.slice(0, 4)}` : '');
export const fmtDateTime = (s) => (s ? `${fmtDate(s)} ${fmtTime(s)}` : '—');

/** "2026-11-07T14:35:02.123" (giờ Server) -> ms, coi như giờ địa phương để mọi phép so sánh cùng một hệ quy chiếu. */
export const parseLocal = (s) => (s ? new Date(s).getTime() : NaN);

const pad = (n, l = 2) => String(n).padStart(l, '0');
export const toLocalIso = (ms) => {
  const d = new Date(ms);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`;
};
export const shiftIso = (serverIso, deltaMs) => toLocalIso(parseLocal(serverIso) + deltaMs);

export const fmtNum = (v, d = 1) => (v === null || v === undefined || Number.isNaN(v) ? '--' : Number(v).toFixed(d));

export function relTime(sec) {
  if (sec === null || sec === undefined || Number.isNaN(sec)) return 'chưa có dữ liệu';
  if (sec < 2) return 'vừa cập nhật';
  if (sec < 60) return `${Math.floor(sec)} giây trước`;
  if (sec < 3600) return `${Math.floor(sec / 60)} phút trước`;
  if (sec < 86400) return `${Math.floor(sec / 3600)} giờ trước`;
  return `${Math.floor(sec / 86400)} ngày trước`;
}

export const metricLabel = (m) => (m === 'TEMPERATURE' ? 'Nhiệt độ' : m === 'HUMIDITY' ? 'Độ ẩm' : m);
export const metricUnit = (m) => (m === 'TEMPERATURE' ? '°C' : '%');
