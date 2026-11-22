# HƯỚNG DẪN CÀI ĐẶT VÀ VẬN HÀNH PULSE IoT

## 1. Mục đích

PULSE IoT giám sát các thiết bị cảm biến, tiếp nhận nhiệt độ/độ ẩm, lưu dữ liệu và phát hiện vượt ngưỡng. Toàn bộ mã nguồn được tổ chức trong **một project Java duy nhất**; giao diện người dùng là Java Swing.

## 2. Yêu cầu

- Windows 10/11.
- JDK 21+, Apache Maven 3.9+.
- MySQL Server 8+.
- NetBeans IDE 24 hoặc IntelliJ IDEA (không bắt buộc).

## 3. Mở project trong NetBeans

1. Mở NetBeans → File → Open Project.
2. Chọn đúng thư mục gốc có file `pom.xml`.
3. Chờ Maven tải thư viện hoàn tất.

## 4. Cấu hình MySQL

1. Khởi động MySQL.
2. Chạy toàn bộ file `init.sql` trong MySQL Workbench.
3. Mở `src/main/resources/application.properties`.
4. Cấu hình `spring.datasource.username` và `spring.datasource.password` hoặc đặt biến môi trường `DB_USERNAME`, `DB_PASSWORD`.

## 5. Chạy ứng dụng

Các thành phần chạy trong cùng một project, nhưng cần chạy thành các tiến trình riêng để Backend tiếp nhận dữ liệu trong khi Swing hiển thị.

### Backend

Tại thư mục chứa `pom.xml`, chạy `mvn spring-boot:run` hoặc chạy lớp `com.demo.Application` trong NetBeans.

### IoT Simulator

Chạy lệnh `mvn -Dexec.mainClass=com.demo.client.ClientApp exec:java` hoặc chạy lớp `com.demo.client.ClientApp`. Cấu hình thiết bị ở `client.properties`.

### Desktop Java Swing

Chạy `mvn exec:java` hoặc chạy lớp `com.pulseiot.desktop.PulseIoTApplication` trong NetBeans.

Có thể mở ba terminal trong NetBeans/Windows để chạy ba tiến trình. Không cần tạo thêm project nào.

## 6. Chạy bằng file BAT

- `start-all.bat`: khởi động Backend, đợi 15 giây, sau đó mở Simulator và Swing.
- `run-backend.bat`, `run-simulator.bat`, `run-desktop.bat`: chạy riêng từng thành phần.
- `stop-all.bat`: đóng các cửa sổ do launcher mở.

## 7. Kiểm thử

1. Đảm bảo MySQL và Backend hoạt động.
2. Chạy Simulator, kiểm tra log có phản hồi gửi dữ liệu thành công.
3. Mở Swing, kiểm tra Dashboard, danh sách thiết bị, lịch sử và cảnh báo.
4. Dùng lệnh `help` trong cửa sổ Simulator để xem các lệnh hỗ trợ; dùng `spike DEV_01` để tạo tình huống vượt ngưỡng (nếu Simulator hỗ trợ lệnh này).
5. Chạy `mvn test` để chạy unit test.

## 8. Lỗi thường gặp

- Không tải được dependency: kiểm tra mạng và Maven.
- Lỗi phiên bản Java: đặt JDK 21 làm Java Platform của NetBeans và `JAVA_HOME`.
- Lỗi kết nối database: kiểm tra MySQL, tên database, user/password.
- Không thấy dữ liệu: Backend phải chạy trước Simulator; kiểm tra URL Server trong `client.properties`.
- Desktop báo mất kết nối: kiểm tra Backend đang chạy tại cổng 8080.

## 9. Giao diện

Ứng dụng Desktop gồm Dashboard, Devices, Data History, Alert Logs và Configuration. Dashboard hiển thị tổng số thiết bị, trạng thái Online/Offline, số cảnh báo, đồng hồ nhiệt độ/độ ẩm và biểu đồ xu hướng.

## 10. Đóng ứng dụng

Nhấn Ctrl+C ở từng terminal hoặc Stop trong NetBeans. Đóng Swing không tự động dừng Backend/Simulator. MySQL cần được dừng riêng nếu muốn.

## 12. Thêm / xóa thiết bị và lưu cấu hình (cập nhật mới)
- **Thêm thiết bị** (trang Devices): lưu vào MySQL, hiện ngay trên Dashboard / Devices và gửi luôn số đo đầu tiên. IoT Client tự phát hiện thiết bị mới (kiểm tra mỗi 2 giây) và gửi dữ liệu liên tục, không cần sửa `client.properties`. Nếu IoT Client chưa chạy, ứng dụng sẽ báo rõ và thiết bị sẽ Offline sau khi hết timeout.
- **Xóa thiết bị**: xóa khỏi MySQL cùng toàn bộ số đo và nhật ký cảnh báo, biến mất ngay trên mọi màn hình. IoT Client tự ngừng gửi cho thiết bị đó (không còn tự đăng ký lại). **Cần chạy lại IoT Client bản mới** nếu đang dùng bản cũ.
- Thiết bị khai báo trong `client.properties` sẽ được đăng ký lại mỗi lần khởi động IoT Client; muốn bỏ hẳn hãy xóa tên khỏi dòng `devices=`.
- **Lưu cấu hình**: ghi vào MySQL (`alert_config`), ứng dụng đọc lại từ Server để xác nhận, liệt kê giá trị đã đổi (ví dụ `35 → 25 °C`) và báo số thiết bị vượt ngưỡng / sắp vượt / Offline theo cấu hình mới. Backend dùng ngưỡng mới cho mọi số đo đến sau đó; nhật ký cảnh báo ghi thêm khi số đo chuyển từ bình thường sang vượt ngưỡng.

## 13. Quy tắc hiển thị trạng thái (đã sửa)
- Mỗi thẻ **Nhiệt độ** / **Độ ẩm** có nhãn riêng: *Bình thường* / *Cảnh báo* (sắp vượt) / *Vượt ngưỡng* / *Dữ liệu đã cũ* (Offline), chỉ tính theo chỉ số của thẻ đó.
- Quy tắc: giá trị > ngưỡng → Vượt ngưỡng (đỏ); giá trị ≥ ngưỡng − biên → Cảnh báo (vàng); còn lại → Bình thường. Thiết bị Offline luôn hiển thị chấm xám.
- Chấm màu trên từng thiết bị là mức cao nhất của hai chỉ số; số nhiệt độ/độ ẩm trên thẻ và bảng cũng đổi màu theo đúng chỉ số đó.
- Đồng hồ luôn hiển thị đúng số đo thật (ví dụ 63.5 °C); thang đo tự mở rộng khi giá trị hoặc ngưỡng vượt mức tối đa.
- Trạng thái và bộ đếm trên mọi màn hình được tính lại từ số đo và ngưỡng hiện hành nên luôn khớp nhau.
