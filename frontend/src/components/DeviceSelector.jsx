import { deviceState } from '../utils/state';
import { fmtNum } from '../utils/format';

/** Thẻ chọn thiết bị (Device Selector). Thiết bị vượt ngưỡng đổi sang nền đỏ để dễ nhận thấy từ xa. */
export default function DeviceSelector({ devices, selectedId, onSelect }) {
  return (
    <div className="device-strip" role="listbox" aria-label="Chọn thiết bị">
      {devices.map((d) => {
        const st = deviceState(d);
        const cls = [
          'dev-chip',
          d.deviceId === selectedId ? 'selected' : '',
          st.key === 'critical' ? 'critical' : '',
          st.key === 'offline' || st.key === 'nodata' ? 'offline' : '',
        ].join(' ');
        return (
          <button key={d.deviceId} type="button" role="option" aria-selected={d.deviceId === selectedId}
            className={cls} onClick={() => onSelect(d.deviceId)}>
            <div className="dev-chip-top">
              <span>{d.deviceId}</span>
              <span className={`dot ${st.key === 'critical' ? 'err' : d.online ? 'live' : ''}`} aria-hidden="true" />
            </div>
            <div className="dev-chip-name">{d.name || 'Chưa đặt tên'}</div>
            <div className="dev-chip-vals">
              <span className="t">{d.latest ? `${fmtNum(d.latest.temperature)} °C` : '-- °C'}</span>
              <span className="h">{d.latest ? `${fmtNum(d.latest.humidity, 0)} %` : '-- %'}</span>
            </div>
          </button>
        );
      })}
    </div>
  );
}
