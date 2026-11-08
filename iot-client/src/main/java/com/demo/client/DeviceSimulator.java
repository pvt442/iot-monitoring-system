package com.demo.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Một thiết bị IoT giả lập chạy trong luồng riêng (T2.5, T2.6, T2.7).
 * Chu trình mỗi lần: sinh số đo -> đóng gói JSON -> HTTP POST -> in kết quả -> chờ đến chu kỳ kế tiếp.
 * Lỗi kết nối/HTTP chỉ được ghi log, KHÔNG làm chương trình dừng.
 */
public final class DeviceSimulator implements Runnable {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final ClientConfig.DeviceProfile profile;
    private final ClientConfig config;
    private final ServerApi api;
    private final SensorGenerator generator;
    private final long startDelayMs;

    private volatile boolean running = true;
    /** true = thiết bị "ngắt kết nối" (không gửi) để thử trạng thái Offline. */
    private volatile boolean paused = false;

    private final AtomicLong sentOk = new AtomicLong();
    private final AtomicLong sentFail = new AtomicLong();
    private volatile boolean registered = false;
    private int consecutiveFailures = 0;
    private int registerFailures = 0;
    private volatile SensorGenerator.Reading last;
    private volatile String lastAlert = "-";

    public DeviceSimulator(ClientConfig.DeviceProfile profile, ClientConfig config, ServerApi api,
                           SensorGenerator generator, long startDelayMs) {
        this.profile = profile;
        this.config = config;
        this.api = api;
        this.generator = generator;
        this.startDelayMs = startDelayMs;
    }

    public String id() {
        return profile.id();
    }

    public void stop() {
        running = false;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public SensorGenerator generator() {
        return generator;
    }

    public String describe() {
        SensorGenerator.Reading r = last;
        String values = r == null ? "chưa gửi"
                : String.format(Locale.ROOT, "%.1f°C / %.1f%%", r.temperature(), r.humidity());
        return String.format(Locale.ROOT, "%-10s %-10s %-9s %-18s ok=%-5d lỗi=%-4d cảnh báo=%s",
                profile.id(), generator.getScenario(), paused ? "NGẮT" : "ĐANG GỬI", values,
                sentOk.get(), sentFail.get(), lastAlert);
    }

    @Override
    public void run() {
        sleep(startDelayMs);
        while (running) {
            long began = System.currentTimeMillis();
            if (!paused) {
                sendOnce();
            }
            long wait = config.intervalMs - (System.currentTimeMillis() - began);
            sleep(Math.max(wait, 50));
        }
    }

    private void sendOnce() {
        if (config.autoRegister && !registered) {
            tryRegister();
        }

        SensorGenerator.Reading r = generator.next();
        last = r;
        String payload = Json.sensorPayload(profile.id(), r.temperature(), r.humidity(), LocalDateTime.now().format(TS));
        ServerApi.Result res = api.postSensor(payload);

        // Server báo thiết bị chưa đăng ký (404) -> đăng ký rồi gửi lại đúng một lần
        if (res.status() == 404 && config.autoRegister) {
            registered = false;
            if (tryRegister()) {
                res = api.postSensor(payload);
            }
        }
        report(r, res);
    }

    private boolean tryRegister() {
        ServerApi.Result res = api.registerDevice(profile);
        if (res.ok()) {
            registered = true;
            registerFailures = 0;
            Log.info(String.format("%s đã đăng ký lên Server (%d) - %s", profile.id(), res.status(), profile.name()));
            return true;
        }
        registerFailures++;
        if (res.status() == 0) {
            // Chỉ in lần đầu và mỗi 10 lần để Terminal không bị tràn khi Server chưa bật
            if (registerFailures == 1 || registerFailures % 10 == 0) {
                Log.info(profile.id() + " chưa đăng ký được: Server không phản hồi (" + res.error() + "). Sẽ thử lại.");
            }
        } else {
            Log.info(profile.id() + " đăng ký thất bại: HTTP " + res.status() + " " + Json.stringField(res.body(), "message"));
        }
        return false;
    }

    private void report(SensorGenerator.Reading r, ServerApi.Result res) {
        String values = String.format(Locale.ROOT, "%5.2f°C %5.2f%%", r.temperature(), r.humidity());

        if (res.ok()) {
            sentOk.incrementAndGet();
            if (consecutiveFailures > 0) {
                Log.info(profile.id() + " đã kết nối lại Server sau " + consecutiveFailures + " lần lỗi.");
                consecutiveFailures = 0;
            }
            String status = Json.stringField(res.body(), "alertStatus");
            lastAlert = status == null ? "-" : status;
            String extra = "";
            if ("CRITICAL".equals(status)) {
                String msg = Json.stringField(res.body(), "message");
                extra = "  ⚠ VƯỢT NGƯỠNG" + (msg == null ? "" : ": " + msg);
            } else if ("WARNING".equals(status)) {
                extra = "  ! sắp vượt ngưỡng";
            }
            Log.info(String.format("%-7s ✓ %s  -> HTTP %d  [%s]%s", profile.id(), values, res.status(),
                    status == null ? "?" : status, extra));
            return;
        }

        sentFail.incrementAndGet();
        consecutiveFailures++;
        if (res.status() == 0) {
            // Mất kết nối: chỉ in lần đầu và mỗi 10 lần để Terminal không bị tràn
            if (consecutiveFailures == 1 || consecutiveFailures % 10 == 0) {
                Log.info(String.format("%-7s ✗ %s  -> KHÔNG KẾT NỐI ĐƯỢC Server (%s). Lần lỗi liên tiếp: %d, sẽ thử lại.",
                        profile.id(), values, res.error(), consecutiveFailures));
            }
        } else {
            String msg = Json.stringField(res.body(), "message");
            Log.info(String.format("%-7s ✗ %s  -> HTTP %d %s", profile.id(), values, res.status(), msg == null ? "" : msg));
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
