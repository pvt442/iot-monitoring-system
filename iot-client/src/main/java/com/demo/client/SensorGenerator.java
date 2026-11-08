package com.demo.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Sinh dữ liệu cảm biến mô phỏng cho một thiết bị (T2.3, T2.8).
 *
 * NORMAL    : "random walk" có hồi quy về giá trị gốc (base) - dao động nhẹ, luôn trong vùng an toàn.
 * SPIKE_*   : tăng dần về mức VƯỢT ngưỡng cấu hình trên Server để minh họa cảnh báo.
 * CSV mode  : đọc lần lượt từng dòng "nhiệt_độ,độ_ẩm" từ file rồi lặp lại.
 */
public final class SensorGenerator {

    public enum Scenario { NORMAL, SPIKE_TEMP, SPIKE_HUM, SPIKE_BOTH }

    /** Một cặp số đo. */
    public record Reading(double temperature, double humidity) {
    }

    private final Random random = new Random();
    private final ClientConfig.DeviceProfile profile;
    private final ServerApi.Thresholds thresholds;
    private final List<double[]> csvRows;
    private int csvIndex;

    private double temp;
    private double hum;
    private volatile Scenario scenario = Scenario.NORMAL;

    public SensorGenerator(ClientConfig.DeviceProfile profile, ServerApi.Thresholds thresholds, List<double[]> csvRows) {
        this.profile = profile;
        this.thresholds = thresholds;
        this.csvRows = csvRows;
        this.temp = profile.baseTemp();
        this.hum = profile.baseHum();
        if (csvRows != null && !csvRows.isEmpty()) {
            this.csvIndex = random.nextInt(csvRows.size()); // mỗi thiết bị bắt đầu ở vị trí khác nhau
        }
    }

    public void setScenario(Scenario s) {
        this.scenario = s;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public synchronized Reading next() {
        Scenario s = scenario;
        boolean spikeTemp = s == Scenario.SPIKE_TEMP || s == Scenario.SPIKE_BOTH;
        boolean spikeHum = s == Scenario.SPIKE_HUM || s == Scenario.SPIKE_BOTH;

        if (csvRows != null && !csvRows.isEmpty()) {
            double[] row = csvRows.get(csvIndex);
            csvIndex = (csvIndex + 1) % csvRows.size();
            // Dữ liệu CSV là "nguồn" của chế độ CSV; kịch bản spike (nếu bật) chỉ ghi đè chỉ số tương ứng
            temp = row[0];
            hum = row[1];
        } else {
            // Chỉ số đang bị "spike" thì KHÔNG bị kéo về giá trị gốc
            if (!spikeTemp) {
                temp += (profile.baseTemp() - temp) * 0.12 + random.nextGaussian() * 0.35;
                temp = softClamp(temp, profile.baseTemp() - 4, profile.baseTemp() + 4, 2.0);
            }
            if (!spikeHum) {
                hum += (profile.baseHum() - hum) * 0.12 + random.nextGaussian() * 0.9;
                hum = softClamp(hum, profile.baseHum() - 8, profile.baseHum() + 8, 5.0);
            }
        }

        if (spikeTemp) {
            temp = approach(temp, thresholds.temperature + 3.0, 1.5);
        }
        if (spikeHum) {
            hum = approach(hum, thresholds.humidity + 5.0, 3.5);
        }
        temp = clamp(temp, -40, 125);
        hum = clamp(hum, 0, 100);
        return new Reading(temp, hum);
    }

    /** Đưa giá trị về trong [lo, hi] nhưng từ từ (tối đa maxStep mỗi lần) để không bị "giật" khi hết spike. */
    private static double softClamp(double v, double lo, double hi, double maxStep) {
        if (v > hi) {
            return Math.max(hi, v - maxStep);
        }
        if (v < lo) {
            return Math.min(lo, v + maxStep);
        }
        return v;
    }

    /** Tiến dần về target, mỗi bước tối đa maxStep, kèm nhiễu nhỏ để trông tự nhiên. */
    private double approach(double current, double target, double maxStep) {
        double diff = target - current;
        double step = Math.max(-maxStep, Math.min(maxStep, diff));
        return current + step + random.nextGaussian() * 0.12;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Đọc CSV: mỗi dòng "temperature,humidity"; bỏ qua dòng trống, dòng bắt đầu bằng # và dòng tiêu đề. */
    public static List<double[]> loadCsv(Path file) throws IOException {
        List<double[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            String[] parts = t.split("[,;\\t]");
            if (parts.length < 2) {
                continue;
            }
            try {
                rows.add(new double[]{
                        Double.parseDouble(parts[0].trim().toLowerCase(Locale.ROOT)),
                        Double.parseDouble(parts[1].trim().toLowerCase(Locale.ROOT))});
            } catch (NumberFormatException ignoredHeader) {
                // dòng tiêu đề hoặc dữ liệu hỏng -> bỏ qua
            }
        }
        return rows;
    }
}
