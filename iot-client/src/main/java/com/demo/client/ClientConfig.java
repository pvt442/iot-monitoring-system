package com.demo.client;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Cấu hình Client. Thứ tự ưu tiên (thấp -> cao): giá trị mặc định < client.properties < biến môi trường < tham số dòng lệnh.
 * Cho phép đổi URL, thiết bị, chu kỳ gửi mà không sửa logic chính (T2.2).
 */
public final class ClientConfig {

    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{1,50}$");

    /** Hồ sơ một thiết bị giả lập. */
    public record DeviceProfile(String id, String name, String location, double baseTemp, double baseHum) {
    }

    public final String serverUrl;
    public final int httpTimeoutMs;
    public final String mode;           // RANDOM | CSV
    public final Path csvFile;
    public final boolean autoRegister;
    public final List<DeviceProfile> devices;
    /** Chu kỳ gửi; có thể đổi lúc đang chạy bằng lệnh "interval". */
    public volatile int intervalMs;

    private ClientConfig(String serverUrl, int intervalMs, int httpTimeoutMs, String mode, Path csvFile,
                         boolean autoRegister, List<DeviceProfile> devices) {
        this.serverUrl = serverUrl;
        this.intervalMs = intervalMs;
        this.httpTimeoutMs = httpTimeoutMs;
        this.mode = mode;
        this.csvFile = csvFile;
        this.autoRegister = autoRegister;
        this.devices = devices;
    }

    public static ClientConfig load(String[] args) throws IOException {
        Map<String, String> cli = parseArgs(args);

        Properties p = new Properties();
        Path file = Path.of(cli.getOrDefault("config", "client.properties"));
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            }
            System.out.println("Đã nạp cấu hình: " + file.toAbsolutePath());
        } else {
            System.out.println("Không thấy " + file + " - dùng cấu hình mặc định.");
        }

        String server = firstNonBlank(cli.get("server"), System.getenv("IOT_SERVER_URL"),
                p.getProperty("server.url"), "http://localhost:8080");
        server = server.replaceAll("/+$", "");

        int interval = parseInt(firstNonBlank(cli.get("interval"), System.getenv("IOT_INTERVAL_MS"),
                p.getProperty("send.interval.ms"), "3000"), "send.interval.ms");
        if (interval < 500) {
            throw new IllegalArgumentException("Chu kỳ gửi tối thiểu 500 ms (đang là " + interval + ").");
        }
        int timeout = parseInt(p.getProperty("http.timeout.ms", "5000"), "http.timeout.ms");

        String mode = firstNonBlank(cli.get("mode"), p.getProperty("mode"), "RANDOM").toUpperCase(Locale.ROOT);
        if (!mode.equals("RANDOM") && !mode.equals("CSV")) {
            throw new IllegalArgumentException("mode chỉ nhận RANDOM hoặc CSV (đang là " + mode + ").");
        }
        Path csv = Path.of(p.getProperty("csv.file", "data/sample.csv"));
        boolean autoRegister = Boolean.parseBoolean(p.getProperty("auto.register", "true"));

        String idList = firstNonBlank(cli.get("devices"), System.getenv("IOT_DEVICES"),
                p.getProperty("devices"), "DEV_01");
        List<DeviceProfile> devices = new ArrayList<>();
        int index = 0;
        for (String raw : idList.split(",")) {
            String id = raw.trim();
            if (id.isEmpty()) {
                continue;
            }
            if (!ID_PATTERN.matcher(id).matches()) {
                throw new IllegalArgumentException("Device ID không hợp lệ: '" + id + "' (chỉ chữ, số, _ - .).");
            }
            String prefix = "device." + id + ".";
            devices.add(new DeviceProfile(
                    id,
                    p.getProperty(prefix + "name", id),
                    p.getProperty(prefix + "location", ""),
                    parseDouble(p.getProperty(prefix + "baseTemp"), 27.0 - index * 1.5),
                    parseDouble(p.getProperty(prefix + "baseHum"), 62.0 + index * 3)));
            index++;
        }
        if (devices.isEmpty()) {
            throw new IllegalArgumentException("Chưa khai báo thiết bị nào (devices=...).");
        }
        return new ClientConfig(server, interval, timeout, mode, csv, autoRegister, devices);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String a : args) {
            if (a.startsWith("--") && a.contains("=")) {
                int eq = a.indexOf('=');
                map.put(a.substring(2, eq).trim(), a.substring(eq + 1).trim());
            }
        }
        return map;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static int parseInt(String s, String name) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá trị số nguyên không hợp lệ cho " + name + ": '" + s + "'");
        }
    }

    private static double parseDouble(String s, double fallback) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
