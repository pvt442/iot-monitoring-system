import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class Simulator {
    private static final String SERVER_URL = "http://localhost:8080/api/sensor-data";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Random random = new Random();
    
    // Đổi thành "CSV" để chạy chế độ đọc file
    private static final String MODE = "RANDOM"; 

    public static void main(String[] args) {
        System.out.println("🚀 Khởi động IoT Client (Chế độ: " + MODE + ")");
        float currentTemp = 25.0f; // Nhiệt độ khởi điểm
        float currentHum = 60.0f;  // Độ ẩm khởi điểm

        while (true) {
            try {
                if (MODE.equals("RANDOM")) {
                    // Thuật toán Random Walk: Dao động nhẹ +- 1 độ
                    currentTemp = currentTemp + (float)(random.nextDouble() - 0.5) * 2;
                    currentHum = currentHum + (float)(random.nextDouble() - 0.5) * 5;
                } else if (MODE.equals("CSV")) {
                    // Logic đọc từng dòng CSV sẽ nằm ở đây
                    // currentTemp = readFromCsv(); 
                }

                // Đóng gói JSON chuẩn cấu trúc Model của Backend
                String jsonPayload = String.format(
                    "{\"device\": {\"deviceId\": \"DEV_01\"}, \"temperature\": %.2f, \"humidity\": %.2f}", 
                    currentTemp, currentHum
                );

                // Gửi HTTP POST lên Server
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Đã gửi: " + jsonPayload + " | Trạng thái: " + response.statusCode());

                // Nghỉ 2 giây theo yêu cầu
                Thread.sleep(2000);

            } catch (Exception e) {
                System.out.println("Lỗi kết nối: " + e.getMessage());
            }
        }
    }
}