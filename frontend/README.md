# Pulse IoT – Web Dashboard

React 18 + Vite + React Router + Recharts + Lucide. CSS thuần (design tokens trong `src/styles/theme.css`).

```
npm install
npm run dev      # http://localhost:5173  (proxy /api -> http://localhost:8080)
npm run build    # xuất ra dist/
```
Backend ở địa chỉ khác: đặt `VITE_PROXY_TARGET=http://IP:8080` khi chạy dev, hoặc `VITE_API_URL=http://IP:8080` khi build (xem `.env.example`).

## Cấu trúc
- `context/DataContext.jsx` – vòng HTTP Polling, phát hiện cảnh báo mới (chống spam toast), đồng hồ theo giờ Server.
- `services/api.js` – toàn bộ lời gọi REST, lỗi chuẩn hóa tiếng Việt.
- `pages/` – Dashboard, Devices, Data History, Alert Logs, Configuration (đúng 5 mục Sidebar).
- `components/` – Gauge SVG, TrendChart (Recharts area), OrbArt (quả cầu 3D), Toast, DeviceTable…

Tôn trọng `prefers-reduced-motion`. Không có số liệu giả: mọi con số lấy từ API.
