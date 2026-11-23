# HƯỚNG DẪN CÀI ĐẶT VÀ VẬN HÀNH — PULSE IoT Monitoring System

---

## 1. Mục đích

**PULSE IoT** giám sát các thiết bị cảm biến theo thời gian thực, tiếp nhận nhiệt độ/độ ẩm, lưu dữ liệu và phát hiện vượt ngưỡng.

Toàn bộ mã nguồn được tổ chức trong **một project Java Maven duy nhất** gồm 3 thành phần:

| Thành phần | Package chính | Mô tả |
|---|---|---|
| **Backend (Spring Boot)** | `com.demo` | REST API + WebSocket, kết nối MySQL |
| **IoT Client Simulator** | `com.demo.client` | Mô phỏng thiết bị gửi dữ liệu lên Backend |
| **Desktop UI (Java Swing)** | `com.pulseiot.desktop` | Giao diện dashboard giám sát |

---

## 2. Yêu cầu hệ thống

| Yêu cầu | Phiên bản tối thiểu |
|---|---|
| **Windows** | 10 / 11 |
| **JDK** | 21+ |
| **Apache Maven** | 3.9+ *(hoặc dùng `mvnw.cmd` đi kèm project, không cần cài Maven)* |
| **MySQL Server** | 8+ |
| **NetBeans IDE** *(tùy chọn)* | 24+ |

> **Lưu ý:** Nếu không có Maven trong PATH hệ thống, hãy dùng `mvnw.cmd` ở thư mục gốc — đây là Maven Wrapper đã được đóng gói sẵn.

---

## 3. Cài đặt & cấu hình

### 3.1. Clone repository

```bash
git clone https://github.com/pvt442/iot-monitoring-system.git
cd iot-monitoring-system
```

### 3.2. Khởi tạo cơ sở dữ liệu MySQL

1. Khởi động MySQL Server.
2. Mở **MySQL Workbench** (hoặc terminal MySQL) và chạy toàn bộ file:
   ```
   init.sql
   ```
   File này tạo database `pulse_iot` với đầy đủ bảng và dữ liệu mẫu.

### 3.3. Cấu hình mật khẩu MySQL (bắt buộc)

File `src/main/resources/application.properties` dùng biến môi trường `${DB_PASSWORD}`.  
Mỗi developer tạo file riêng (file này đã được `.gitignore`, **không được commit**):

```
src/main/resources/application-local.properties
```

Nội dung:
```properties
spring.datasource.password=MẬT_KHẨU_MYSQL_CỦA_BẠN
```

> **Ví dụ:** Nếu MySQL của bạn không có mật khẩu: `spring.datasource.password=`  
> Nếu mật khẩu là `1234`: `spring.datasource.password=1234`

### 3.4. Cấu hình thiết bị IoT Simulator

Mở file `client.properties` ở thư mục gốc và điều chỉnh nếu cần:

```properties
server.url=http://localhost:8080
devices=DEV_01,DEV_02,DEV_03
interval=5000
```

---

## 4. Chạy ứng dụng

> **⚠️ QUAN TRỌNG:** Phải khởi động **MySQL** trước khi chạy Backend.

### Cách 1 — Chạy tất cả bằng một lệnh (khuyên dùng)

**Double-click** file `start-all.bat` trong thư mục gốc.

Script sẽ tự động mở **3 cửa sổ CMD riêng biệt**:
1. `Pulse IoT Backend` — Spring Boot khởi động trên cổng **8080**
2. `Pulse IoT Simulator` — IoT Client gửi dữ liệu lên Backend (sau khi Backend sẵn sàng ~12 giây)
3. `Pulse IoT Desktop` — Giao diện Java Swing Dashboard

> ⚠️ **Không chạy `start-all.bat` hai lần** — sẽ mở thêm cửa sổ Dashboard thừa.  
> ⚠️ **Không dùng nút ▶ Run trong VS Code** để chạy file `.bat` — phải double-click từ File Explorer.

### Cách 2 — Chạy từng thành phần riêng lẻ

Mở lần lượt mỗi file bằng cách double-click:

| File | Chức năng |
|---|---|
| `run-backend.bat` | Khởi động Spring Boot Backend (cổng 8080) |
| `run-simulator.bat` | Khởi động IoT Client Simulator |
| `run-desktop.bat` | Khởi động giao diện Java Swing |

> **Thứ tự bắt buộc:** Backend → Simulator → Desktop

### Cách 3 — Chạy trong NetBeans IDE

1. File → **Open Project** → chọn thư mục gốc (nơi có `pom.xml`).
2. Chờ Maven tải thư viện xong.
3. Mở 3 terminal riêng (Window → Terminal) và chạy:

   **Terminal 1 — Backend:**
   ```bash
   mvn spring-boot:run
   ```

   **Terminal 2 — Simulator:**
   ```bash
   mvn -Dexec.mainClass=com.demo.client.ClientApp exec:java
   ```

   **Terminal 3 — Desktop:**
   ```bash
   mvn -Dexec.mainClass=com.pulseiot.desktop.PulseIoTApplication exec:java
   ```

---

## 5. Lệnh điều khiển Simulator

Sau khi cửa sổ **Simulator** mở, nhập các lệnh sau để kiểm thử:

| Lệnh | Mô tả |
|---|---|
| `help` | Hiện danh sách lệnh |
| `spike DEV_01 temp` | Tạo nhiệt độ đột biến cho DEV_01 |
| `spike DEV_01 humid` | Tạo độ ẩm đột biến cho DEV_01 |
| `spike DEV_01 both` | Tạo cả hai vượt ngưỡng cho DEV_01 |
| `spike all both` | Tạo đột biến cho tất cả thiết bị |
| `normal DEV_01` | Khôi phục dữ liệu bình thường cho DEV_01 |
| `normal all` | Khôi phục tất cả thiết bị về bình thường |
| `offline DEV_01` | Ngắt kết nối DEV_01 (thiết bị Offline) |
| `online DEV_01` | Kết nối lại DEV_01 |
| `online all` | Kết nối lại tất cả thiết bị |

---

## 6. Dừng ứng dụng

**Double-click** file `stop-all.bat` hoặc đóng thủ công từng cửa sổ CMD.

> Lưu ý: Đóng cửa sổ Desktop **không** tự động dừng Backend và Simulator.

---

## 7. Thêm / xóa thiết bị và lưu cấu hình

### Thêm thiết bị

- Từ trang **Devices** trong Desktop, thêm thiết bị mới → lưu vào MySQL.
- IoT Simulator tự phát hiện thiết bị mới (kiểm tra mỗi 2 giây) và bắt đầu gửi dữ liệu — **không cần sửa `client.properties`**.

### Xóa thiết bị

- Xóa từ trang **Devices** → xóa khỏi MySQL cùng toàn bộ số đo và nhật ký cảnh báo.
- IoT Simulator tự ngừng gửi cho thiết bị đó.

### Lưu cấu hình ngưỡng

- Cấu hình ngưỡng nhiệt độ/độ ẩm từ trang **Configuration** → ghi vào bảng `alert_config` trong MySQL.
- Backend áp dụng ngưỡng mới cho các số đo tiếp theo; nhật ký cảnh báo ghi thêm khi số đo chuyển từ bình thường sang vượt ngưỡng.

---

## 8. Quy tắc hiển thị trạng thái

| Màu | Ý nghĩa |
|---|---|
| 🟢 Xanh — *Bình thường* | Giá trị nằm trong ngưỡng an toàn |
| 🟡 Vàng — *Cảnh báo* | Giá trị ≥ ngưỡng − biên (sắp vượt) |
| 🔴 Đỏ — *Vượt ngưỡng* | Giá trị > ngưỡng |
| ⚫ Xám — *Offline* | Thiết bị không gửi dữ liệu quá timeout |

- Chấm màu trên mỗi thiết bị là mức cao nhất của hai chỉ số (nhiệt độ / độ ẩm).
- Đồng hồ luôn hiển thị đúng số đo thật; thang đo tự mở rộng khi giá trị vượt mức tối đa.
- Trạng thái và bộ đếm trên mọi màn hình được tính lại từ số đo và ngưỡng hiện hành.

---

## 9. Lỗi thường gặp & cách xử lý

| Lỗi | Nguyên nhân | Cách xử lý |
|---|---|---|
| `mvn` không nhận dạng được | Maven chưa cài hoặc chưa thêm PATH | Dùng `mvnw.cmd` thay vì `mvn` |
| Backend không khởi động | MySQL chưa chạy hoặc sai mật khẩu | Khởi động MySQL; kiểm tra `application-local.properties` |
| Simulator thoát ngay sau 8 giây | Chạy từ VS Code Output tab (không có stdin) | Double-click file `.bat` từ File Explorer |
| Hai cửa sổ Dashboard mở cùng lúc | Chạy `start-all.bat` hai lần hoặc chạy thêm `run-desktop.bat` | Dùng `stop-all.bat` rồi chạy lại `start-all.bat` |
| Không tải được dependency | Mạng bị chặn hoặc chưa có internet | Kiểm tra kết nối mạng; thử lại |
| Lỗi phiên bản Java | JDK < 21 | Cài JDK 21+ và đặt `JAVA_HOME` |
| Không thấy dữ liệu trên Dashboard | Backend chưa chạy hoặc Simulator chưa gửi | Chạy Backend trước → Simulator → Desktop |
| Desktop báo mất kết nối | Backend chưa chạy hoặc đã dừng | Kiểm tra cửa sổ Backend (cổng 8080) |

---

## 10. Cấu trúc thư mục project

```
iot-monitoring-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/demo/              # Backend + IoT Simulator
│   │   │   │   ├── Application.java   # Main class Spring Boot
│   │   │   │   ├── client/            # IoT Simulator (ClientApp.java)
│   │   │   │   ├── controller/        # REST Controllers
│   │   │   │   ├── service/           # Business Logic
│   │   │   │   ├── repository/        # JPA Repositories
│   │   │   │   └── model/             # Entity classes
│   │   │   └── com/pulseiot/desktop/  # Java Swing UI
│   │   │       └── PulseIoTApplication.java
│   │   └── resources/
│   │       ├── application.properties          # Cấu hình chung
│   │       └── application-local.properties   # Mật khẩu DB (gitignore)
│   └── test/
├── .mvn/wrapper/                       # Maven Wrapper files
├── init.sql                            # Script khởi tạo database
├── client.properties                  # Cấu hình IoT Simulator
├── pom.xml                            # Maven build file
├── mvnw.cmd                           # Maven Wrapper (Windows)
├── run-backend.bat                    # Chạy Backend
├── run-simulator.bat                  # Chạy Simulator
├── run-desktop.bat                    # Chạy Desktop UI
├── start-all.bat                      # Chạy cả 3 thành phần
└── stop-all.bat                       # Dừng tất cả
```

---

## 11. Giao diện ứng dụng

Ứng dụng Desktop gồm 5 màn hình chính:

| Màn hình | Chức năng |
|---|---|
| **Dashboard** | Tổng quan: số thiết bị Online/Offline, số cảnh báo, đồng hồ nhiệt độ/độ ẩm, biểu đồ xu hướng |
| **Devices** | Danh sách thiết bị, thêm/xóa thiết bị, xem thông tin chi tiết |
| **Data History** | Lịch sử số đo theo thiết bị và khoảng thời gian |
| **Alert Logs** | Nhật ký cảnh báo khi vượt ngưỡng |
| **Configuration** | Cài đặt ngưỡng nhiệt độ/độ ẩm và biên cảnh báo |
