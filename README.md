# Pulse IoT – IoT Data Server

```
iot-monitoring-system/
├── init.sql            ← MySQL: 4 bảng (devices, sensor_data, alert_logs, alert_config)
├── backend/            ← Spring Boot (Java 21)  – cổng 8080      [Thành viên 1 – đã hoàn thiện lại]
├── iot-client/         ← Java IoT Client Simulator               [Thành viên 2 – Phần 1]
└── frontend/           ← React + Vite Dashboard "Pulse IoT" – cổng 5173   [Thành viên 2 – Phần 2]
```

## 1. Các lưu ý khi khởi chạy

1. Xóa thư mục `backend/target` cũ rồi build lại.
2. **Mật khẩu MySQL** không còn nằm trong `application.properties`. Đặt biến môi trường `DB_PASSWORD`
   _hoặc_ sao chép `backend/src/main/resources/application-local.properties.example` → `application-local.properties` và điền mật khẩu (file này đã được `.gitignore`).

## 2. Chạy hệ thống (thứ tự)

| Bước           | Lệnh                                                                         |
| -------------- | ---------------------------------------------------------------------------- |
| MySQL + schema | `mysql -u root -p < init.sql`                                                |
| Backend        | `cd backend && ./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`) |
| IoT Client     | `cd iot-client && run.bat` (Linux/macOS: `./run.sh`)                         |
| Web            | `cd frontend && npm install && npm run dev` → mở http://localhost:5173       |

Client tự đăng ký DEV_01..DEV_03 nên không cần gọi `POST /api/devices` bằng tay.

## 3. Backend đã bổ sung

- **Kiểm tra dữ liệu**: thiếu deviceId/temperature/humidity → 400; nhiệt độ ngoài −40…125 °C, độ ẩm ngoài 0…100 % → 400; thiết bị chưa đăng ký → 404; lỗi trả JSON `{timestamp,status,error,message}`.
- **Cảnh báo cả nhiệt độ lẫn độ ẩm** (`>` ngưỡng = CRITICAL; vùng sát ngưỡng = WARNING), có nhật ký bền vững trong `alert_logs`, chỉ ghi khi chỉ số _mới_ vượt ngưỡng (không spam).
- **Cấu hình ngưỡng lưu MySQL** (`alert_config`): `GET/PUT /api/config`, `POST /api/config/reset`.
- **Online/Offline**: cột `devices.last_seen` cập nhật mỗi lần nhận dữ liệu; Offline khi quá `offlineTimeoutSeconds` (mặc định 15 s).
- **Lọc + phân trang**: `from`, `to`, `limit`, `/search?page&size`.
- **Tương thích ngược**: vẫn nhận payload cũ `{"device":{"deviceId":"DEV_01"},...}`; response có cả `deviceId` (phẳng) lẫn `device.deviceId` (lồng); các endpoint cũ giữ nguyên đường dẫn.
- CORS cấu hình chung cho `/api/**`; xóa thiết bị `DELETE /api/devices/{id}`; `GET /api/dashboard/overview` gom dữ liệu Dashboard trong 1 lần gọi.

### Danh sách API

| Method        | Endpoint                                             | Ghi chú                                    |
| ------------- | ---------------------------------------------------- | ------------------------------------------ |
| POST          | `/api/sensor-data`                                   | 201 + `alertStatus`, `alerts[]`            |
| GET           | `/api/sensor-data?deviceId&from&to&limit`            | mới nhất trước (mặc định 500, tối đa 5000) |
| GET           | `/api/sensor-data/search?deviceId&from&to&page&size` | phân trang                                 |
| GET           | `/api/sensor-data/{id}/history?from&to&limit`        | vẽ biểu đồ                                 |
| GET           | `/api/sensor-data/{id}/latest`                       | 404 nếu chưa có dữ liệu                    |
| GET/POST      | `/api/devices`                                       | POST = thêm/cập nhật (201/200)             |
| GET/DELETE    | `/api/devices/{id}`                                  |                                            |
| GET           | `/api/alerts?deviceId&metric&page&size`              | nhật ký cảnh báo                           |
| GET/PUT, POST | `/api/config`, `/api/config/reset`                   | ngưỡng + timeout                           |
| GET           | `/api/dashboard/overview`, `/api/health`             |                                            |

Thời gian dùng ISO 8601 theo giờ Server, ví dụ `2026-11-07T14:35:02.123`.

## 4. Kịch bản demo

1. Chạy 3 thành phần → Dashboard hiện 3 thiết bị Online, biểu đồ chạy.
2. Trong cửa sổ IoT Client gõ `spike DEV_01 both` → số đo tăng dần: **Sắp vượt ngưỡng → Vượt ngưỡng**, thẻ đỏ + toast + dòng mới ở _Alert Logs_.
3. `normal all` → trở về bình thường. `offline DEV_02` → sau 15 s thiết bị chuyển **Offline**. Tắt Backend → banner "Mất kết nối Server".

## 5. Lưu ý trung thực (quan trọng khi báo cáo)

- Dữ liệu là **mô phỏng** bởi Client, không phải cảm biến vật lý.
- Cập nhật Dashboard bằng **HTTP Polling** (mặc định 2 s, chỉnh ở Configuration); chưa dùng WebSocket.
- Ngưỡng/timeout lưu trong **MySQL**; chu kỳ polling và bật/tắt toast chỉ lưu **LocalStorage** của trình duyệt.
- Chưa có đăng nhập/phân quyền.
