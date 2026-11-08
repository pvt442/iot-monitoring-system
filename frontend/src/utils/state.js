// Quy đổi dữ liệu Backend thành trạng thái hiển thị thống nhất cho toàn giao diện.
// Ưu tiên: chưa có dữ liệu > Offline > Vượt ngưỡng > Sắp vượt ngưỡng > Bình thường.
export function deviceState(dev) {
  if (!dev || !dev.lastSeen) return { key: 'nodata', label: 'Chưa có dữ liệu', tone: 'muted' };
  if (!dev.online) return { key: 'offline', label: 'Offline', tone: 'muted' };
  if (dev.alertStatus === 'CRITICAL') return { key: 'critical', label: 'Vượt ngưỡng', tone: 'red' };
  if (dev.alertStatus === 'WARNING') return { key: 'warning', label: 'Sắp vượt ngưỡng', tone: 'yellow' };
  return { key: 'normal', label: 'Bình thường', tone: 'green' };
}

/** Trạng thái của MỘT chỉ số (nhiệt độ/độ ẩm) từ danh sách alerts của số đo mới nhất. */
export function metricState(latest, metric) {
  if (!latest) return 'none';
  const item = (latest.alerts || []).find((a) => a.metric === metric);
  if (!item) return 'normal';
  return item.level === 'CRITICAL' ? 'critical' : 'warning';
}

export const STATE_TEXT = {
  normal: 'Bình thường',
  warning: 'Sắp vượt ngưỡng',
  critical: 'Vượt ngưỡng',
  none: 'Chưa có dữ liệu',
};
