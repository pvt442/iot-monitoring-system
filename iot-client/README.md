# IoT Client Simulator

Chương trình Java giả lập một hoặc nhiều thiết bị IoT, gửi nhiệt độ/độ ẩm về **IoT Data Server** bằng HTTP POST theo chu kỳ.
Không cần thư viện ngoài (chỉ dùng `java.net.http` của JDK 21).

## Chạy
```
# Windows
run.bat
# Linux / macOS
./run.sh
# hoặc dùng Maven
mvn package && java -jar target/iot-client-1.0.0.jar
```
Điều kiện: MySQL + Backend (cổng 8080) đang chạy. Client tự đăng ký thiết bị (`POST /api/devices`) nên **không cần** đăng ký tay.

Tham số (ghi đè `client.properties`):
`--server=http://IP:8080  --interval=3000  --devices=DEV_01,DEV_02  --mode=RANDOM|CSV  --config=duong_dan.properties`

## Hợp đồng API đã thống nhất với Backend
`POST /api/sensor-data` – `Content-Type: application/json`
```json
{ "deviceId": "DEV_01", "temperature": 25.00, "humidity": 60.00, "timestamp": "2026-11-07T10:30:00.123" }
```
Phản hồi `201` kèm `alertStatus` (`NORMAL` | `WARNING` | `CRITICAL`) và `alerts[]`; Client in kết quả ra Terminal.
Lỗi: `400` (dữ liệu sai), `404` (thiết bị chưa đăng ký → Client tự đăng ký rồi gửi lại).

## Lệnh khi đang chạy (dùng để demo)
| Lệnh | Tác dụng |
|---|---|
| `status` | Xem trạng thái, số lần gửi OK/lỗi của từng thiết bị |
| `spike DEV_01 [temp\|hum\|both]` | Tăng dần số đo tới mức **vượt ngưỡng** (lấy ngưỡng từ `GET /api/config`) |
| `normal all` | Quay lại dữ liệu bình thường |
| `offline DEV_02` / `online DEV_02` | Ngừng/gửi lại để thử trạng thái **Offline** |
| `interval 2000` | Đổi chu kỳ gửi (≥ 500 ms) |
| `quit` | Thoát |

## Thuật toán sinh dữ liệu
- **RANDOM**: *random walk có hồi quy về giá trị gốc*: `x += (base − x)·0,12 + N(0, σ)`, giới hạn quanh base nên không tự vượt ngưỡng.
- **Spike**: tiến dần về `ngưỡng + 3 °C` (hoặc `+ 5 %` độ ẩm), mỗi bước tối đa 1,5 °C / 3,5 %, đi qua vùng WARNING rồi CRITICAL.
- **CSV**: đọc lần lượt `nhiệt_độ,độ_ẩm` trong `data/sample.csv` rồi lặp lại.

## Xử lý lỗi
Mất kết nối / timeout / HTTP lỗi chỉ được ghi log, chương trình **không dừng** và tự gửi lại ở chu kỳ sau; khi Server hoạt động lại sẽ có thông báo "đã kết nối lại".
Số được định dạng bằng `Locale.ROOT` nên không bị lỗi dấu phẩy thập phân (`25,00`) trên Windows tiếng Việt.
