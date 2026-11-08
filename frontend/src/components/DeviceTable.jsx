import { Eye, Trash2 } from 'lucide-react';
import StatusBadge from './StatusBadge';
import { useData } from '../context/DataContext';
import { useNow } from '../hooks/useNow';
import { fmtNum, fmtTime, relTime } from '../utils/format';
import { deviceState, metricState } from '../utils/state';

/** Bảng thiết bị: hàng cảnh báo có nền đỏ nhạt + viền trái đỏ; hàng bình thường không hiệu ứng. */
export default function DeviceTable({ devices, onView, onDelete }) {
  const { ageSeconds } = useData();
  useNow(); // cập nhật "x giây trước" mỗi giây

  return (
    <div className="table-wrap">
      <table className="data">
        <thead>
          <tr>
            <th>Mã thiết bị</th>
            <th>Tên thiết bị</th>
            <th>Vị trí</th>
            <th>Nhiệt độ</th>
            <th>Độ ẩm</th>
            <th>Trạng thái</th>
            <th>Cập nhật cuối</th>
            <th style={{ textAlign: 'right' }}>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {devices.map((d) => {
            const st = deviceState(d);
            const live = d.online && d.latest;
            const tCrit = live && metricState(d.latest, 'TEMPERATURE') === 'critical';
            const hCrit = live && metricState(d.latest, 'HUMIDITY') === 'critical';
            return (
              <tr key={d.deviceId} className={`row-${st.key}`}>
                <td>
                  <div className="cell-id">
                    <span className={`dot ${st.key === 'critical' ? 'err' : d.online ? 'live' : ''}`} aria-hidden="true" />
                    {d.deviceId}
                  </div>
                </td>
                <td>{d.name || <span className="cell-sub">Chưa đặt tên</span>}</td>
                <td>{d.location || <span className="cell-sub">—</span>}</td>
                <td className="num t">
                  {d.latest ? <span className={tCrit ? 'over' : ''}>{fmtNum(d.latest.temperature)} °C</span> : '—'}
                </td>
                <td className="num h">
                  {d.latest ? <span className={hCrit ? 'over' : ''}>{fmtNum(d.latest.humidity)} %</span> : '—'}
                </td>
                <td><StatusBadge state={st} /></td>
                <td>
                  <div className="mono">{d.lastSeen ? fmtTime(d.lastSeen) : '—'}</div>
                  <div className="cell-sub">{d.lastSeen ? relTime(ageSeconds(d.lastSeen)) : 'chưa có dữ liệu'}</div>
                </td>
                <td>
                  <div className="row-actions">
                    {onView && (
                      <button type="button" className="btn btn-icon" onClick={() => onView(d)} aria-label={`Xem chi tiết ${d.deviceId}`} title="Xem chi tiết">
                        <Eye aria-hidden="true" />
                      </button>
                    )}
                    {onDelete && (
                      <button type="button" className="btn btn-icon danger" onClick={() => onDelete(d)} aria-label={`Xóa ${d.deviceId}`} title="Xóa thiết bị">
                        <Trash2 aria-hidden="true" />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
