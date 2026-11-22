# PULSE IoT – Hệ thống giám sát thiết bị IoT

Hệ thống gộp **Backend Spring Boot + giao diện Desktop Java Swing + IoT Simulator** vào **một Maven/Java project duy nhất**. Khi mở bằng NetBeans, chỉ cần mở thư mục chứa `pom.xml` ở cấp gốc.

## 1. Công nghệ

- Java 21 cho toàn bộ mã nguồn.
- Java Swing: giao diện Desktop (không dùng ReactJS, JavaScript, HTML, CSS, Node.js/npm).
- Spring Boot + Spring Data JPA: REST API và xử lý nghiệp vụ.
- MySQL 8+: lưu thiết bị, dữ liệu cảm biến, nhật ký cảnh báo và cấu hình.
- Java HttpClient: IoT Simulator gửi dữ liệu qua HTTP.
- Maven: quản lý thư viện và biên dịch một project.

## 2. Cấu trúc project

```text
PULSE_IoT_JAVA_SWING_MOT_PROJECT/
├── pom.xml                         # Chỉ một project Maven
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/demo/            # Backend + IoT Simulator
│   │   │   └── com/pulseiot/        # Frontend Java Swing
│   │   └── resources/               # application.properties
│   └── test/java/                   # Unit test
├── data/sample.csv                  # Dữ liệu mẫu (nếu chọn chế độ CSV)
├── client.properties                # Cấu hình IoT Simulator
├── init.sql                         # Khởi tạo MySQL
├── README.md
├── HUONG_DAN_CAI_DAT_VA_VAN_HANH.md
├── run-backend.bat
├── run-desktop.bat
├── run-simulator.bat
├── start-all.bat
└── stop-all.bat
```

## 3. Chuẩn bị

1. Cài JDK 21 và Apache Maven 3.9+; kiểm tra `java -version`, `mvn -version`.
2. Cài và khởi động MySQL 8+.
3. Mở `init.sql` bằng MySQL Workbench và chạy để tạo database `iot_monitoring`.
4. Mở `src/main/resources/application.properties`. Mặc định: host `localhost:3306`, database `iot_monitoring`, user `root`, mật khẩu rỗng. Nếu có mật khẩu, đặt biến môi trường `DB_PASSWORD`.
5. Mở thư mục gốc project bằng NetBeans: **File → Open Project → chọn thư mục có pom.xml**. NetBeans chỉ nhận một project Maven.

## 4. Cách chạy

### Cách A – chạy từng phần (khuyến nghị)

Mở terminal tại thư mục gốc project.

**1) Backend:**

```bash
mvn spring-boot:run
```

Backend chạy tại `http://localhost:8080`.

**2) IoT Simulator:** mở terminal thứ hai:

```bash
mvn -Dexec.mainClass=com.demo.client.ClientApp exec:java
```

Simulator đọc cấu hình từ `client.properties` ở thư mục gốc. Có thể truyền tham số:

```bash
mvn -Dexec.mainClass=com.demo.client.ClientApp -Dexec.args="--server=http://localhost:8080 --interval=3000 --devices=DEV_01,DEV_02" exec:java
```

**3) Giao diện Java Swing:** mở terminal thứ ba:

```bash
mvn -Dexec.mainClass=com.pulseiot.desktop.PulseIoTApplication exec:java
```

Hoặc chạy `mvn exec:java` vì main class mặc định của exec plugin là giao diện Swing.

### Cách B – chạy bằng file BAT (Windows)

- `run-backend.bat`: chạy Backend.
- `run-simulator.bat`: chạy IoT Simulator.
- `run-desktop.bat`: chạy giao diện Swing.
- `start-all.bat`: mở cả ba cửa sổ tiến trình. MySQL phải chạy trước.
- `stop-all.bat`: đóng các cửa sổ đã mở từ launcher.

## 5. Chức năng

- Dashboard: thẻ tổng quan, đồng hồ nhiệt độ/độ ẩm dạng vòng cung, thiết bị đang chọn và biểu đồ xu hướng.
- Devices: danh sách thiết bị, số đo mới nhất, trạng thái kết nối, thêm và xóa.
- Lịch sử cảm biến: lọc theo thiết bị/thời gian, phân trang, xuất CSV.
- Alert Logs: lọc theo thiết bị/chỉ số, xem giá trị đo và ngưỡng cảnh báo.
- Configuration: chỉnh ngưỡng nhiệt độ/độ ẩm, biên cảnh báo và timeout Offline.
- IoT Simulator: sinh dữ liệu nhiệt độ/độ ẩm và gửi liên tục lên Backend.

## 6. Kiểm tra và đóng project

Biên dịch toàn bộ:

```bash
mvn clean package
```

Chạy kiểm thử:

```bash
mvn test
```

Dừng tiến trình chạy trong terminal bằng `Ctrl + C` hoặc nút Stop trong NetBeans. MySQL không bị dừng theo.

## 7. Xử lý lỗi thường gặp

| Lỗi                                | Cách xử lý                                                                                                               |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| `mvn is not recognized`            | Cài Maven, thêm `bin` vào PATH, mở terminal mới.                                                                         |
| `Unsupported class file / release` | Đảm bảo NetBeans/Maven đang dùng JDK 21.                                                                                 |
| Backend không kết nối MySQL        | Kiểm tra MySQL, cổng 3306, database và `DB_USERNAME`/`DB_PASSWORD`.                                                      |
| Swing không kết nối được Server    | Chạy Backend trước, kiểm tra địa chỉ API `http://localhost:8080`.                                                        |
| Không có dữ liệu                   | Chạy Simulator và kiểm tra cửa sổ log.                                                                                   |
| Port 8080 đang được dùng           | Dừng tiến trình đang chiếm cổng hoặc đổi `server.port` trong `application.properties`; đồng bộ URL ở Simulator và Swing. |

## 8. Giao diện Desktop

Giao diện Java Swing sử dụng tông màu tối xanh than, thanh điều hướng bên trái, thẻ thống kê, đồng hồ đo dạng vòng cung, biểu đồ và bảng dữ liệu. Nội dung trên màn hình được rút gọn, ưu tiên tên mục, số liệu và trạng thái.

## 9. Quy ước quan trọng

Đây là **một project Java/Maven**. Các package vẫn được tách theo nhiệm vụ để mã nguồn dễ quản lý: `com.demo` cho Backend/Simulator và `com.pulseiot.desktop` cho Swing.
