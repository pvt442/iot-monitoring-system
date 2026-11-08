package com.demo.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Lớp gọi REST API của IoT Data Server bằng java.net.http.HttpClient. Mọi lỗi mạng được gói vào Result, không ném ra ngoài. */
public final class ServerApi {

    /** Kết quả một lần gọi: status = 0 nghĩa là không kết nối được (khi đó error có mô tả). */
    public record Result(int status, String body, String error) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    /** Ngưỡng cảnh báo lấy từ Server (dùng để tạo dữ liệu vượt ngưỡng đúng cấu hình). */
    public static final class Thresholds {
        public volatile double temperature = 35.0;
        public volatile double humidity = 80.0;
    }

    private final HttpClient client;
    private final String baseUrl;
    private final Duration timeout;
    public final Thresholds thresholds = new Thresholds();

    public ServerApi(String baseUrl, int timeoutMs) {
        this.baseUrl = baseUrl;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public Result postSensor(String jsonPayload) {
        return post("/api/sensor-data", jsonPayload);
    }

    public Result registerDevice(ClientConfig.DeviceProfile d) {
        return post("/api/devices", Json.devicePayload(d.id(), d.name(), d.location()));
    }

    /** Đọc ngưỡng từ GET /api/config; trả true nếu cập nhật được. */
    public boolean refreshThresholds() {
        Result r = get("/api/config");
        if (!r.ok()) {
            return false;
        }
        double t = Json.numberField(r.body(), "temperatureThreshold");
        double h = Json.numberField(r.body(), "humidityThreshold");
        if (Double.isNaN(t) || Double.isNaN(h)) {
            return false;
        }
        thresholds.temperature = t;
        thresholds.humidity = h;
        return true;
    }

    private Result post(String path, String json) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(req);
    }

    private Result get(String path) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        return send(req);
    }

    private Result send(HttpRequest req) {
        try {
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            return new Result(res.statusCode(), res.body(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(0, null, "bị ngắt");
        } catch (IOException e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new Result(0, null, msg);
        } catch (RuntimeException e) { // URI sai, v.v.
            return new Result(0, null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
