// Lớp gọi REST API Backend (Spring Boot). Mọi lỗi được chuẩn hóa thành ApiError với thông báo tiếng Việt.
const BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.name = 'ApiError';
    this.status = status; // 0 = không kết nối được / timeout
  }
}

async function request(path, { method = 'GET', body, timeout = 6000 } = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeout);
  let res;
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
  } catch (e) {
    throw new ApiError(0, e.name === 'AbortError'
      ? 'Server không phản hồi (quá thời gian chờ).'
      : 'Không kết nối được Server. Hãy kiểm tra Backend đang chạy ở cổng 8080.');
  } finally {
    clearTimeout(timer);
  }

  if (!res.ok) {
    let message = `Lỗi HTTP ${res.status}`;
    try {
      const data = await res.json();
      if (data && data.message) message = data.message;
    } catch { /* body không phải JSON */ }
    // Vite proxy trả 500/502 khi Backend chưa chạy
    if (res.status === 502 || res.status === 503 || res.status === 504) {
      throw new ApiError(0, 'Không kết nối được Server. Hãy kiểm tra Backend đang chạy ở cổng 8080.');
    }
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) return null;
  return res.json();
}

const qs = (params) => {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.set(k, v);
  });
  const s = p.toString();
  return s ? `?${s}` : '';
};

export const api = {
  // Dashboard: 1 lần gọi lấy thống kê + ngưỡng + từng thiết bị kèm số đo mới nhất
  getOverview: () => request('/api/dashboard/overview'),

  // Thiết bị
  getDevices: () => request('/api/devices'),
  createDevice: (device) => request('/api/devices', { method: 'POST', body: device }),
  deleteDevice: (id) => request(`/api/devices/${encodeURIComponent(id)}`, { method: 'DELETE' }),

  // Dữ liệu cảm biến
  getLatest: (id) => request(`/api/sensor-data/${encodeURIComponent(id)}/latest`),
  /** Lịch sử 1 thiết bị, MỚI NHẤT TRƯỚC. */
  getHistory: (id, { from, to, limit } = {}) =>
    request(`/api/sensor-data/${encodeURIComponent(id)}/history${qs({ from, to, limit })}`, { timeout: 10000 }),
  /** Tìm kiếm có phân trang -> { content, page, size, totalElements, totalPages } */
  searchSensorData: ({ deviceId, from, to, page = 0, size = 20 } = {}) =>
    request(`/api/sensor-data/search${qs({ deviceId, from, to, page, size })}`, { timeout: 10000 }),
  /** Danh sách không phân trang (dùng xuất CSV), tối đa 5000 dòng. */
  listSensorData: ({ deviceId, from, to, limit = 5000 } = {}) =>
    request(`/api/sensor-data${qs({ deviceId, from, to, limit })}`, { timeout: 20000 }),

  // Cảnh báo
  getAlerts: ({ deviceId, metric, page = 0, size = 20 } = {}) =>
    request(`/api/alerts${qs({ deviceId, metric, page, size })}`),

  // Cấu hình ngưỡng (lưu trong MySQL, dùng chung)
  getConfig: () => request('/api/config'),
  updateConfig: (cfg) => request('/api/config', { method: 'PUT', body: cfg }),
  resetConfig: () => request('/api/config/reset', { method: 'POST' }),
};
