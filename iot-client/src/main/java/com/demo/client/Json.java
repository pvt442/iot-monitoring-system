package com.demo.client;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xử lý JSON tối thiểu, không cần thư viện ngoài.
 * Lưu ý: số luôn được định dạng bằng Locale.ROOT để dấu thập phân là "." (tránh lỗi "25,00" trên Windows tiếng Việt).
 */
public final class Json {

    private Json() {
    }

    /** Payload gửi lên: {"deviceId":"DEV_01","temperature":25.00,"humidity":60.00,"timestamp":"2026-11-07T10:30:00.123"} */
    public static String sensorPayload(String deviceId, double temperature, double humidity, String timestamp) {
        return String.format(Locale.ROOT,
                "{\"deviceId\":\"%s\",\"temperature\":%.2f,\"humidity\":%.2f,\"timestamp\":\"%s\"}",
                escape(deviceId), temperature, humidity, escape(timestamp));
    }

    public static String devicePayload(String deviceId, String name, String location) {
        return String.format(Locale.ROOT,
                "{\"deviceId\":\"%s\",\"name\":\"%s\",\"location\":\"%s\",\"status\":\"ACTIVE\"}",
                escape(deviceId), escape(name), escape(location));
    }

    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /** Lấy giá trị chuỗi đầu tiên của khóa (không phân tích cây JSON). null nếu không có. */
    public static String stringField(String json, String key) {
        if (json == null) {
            return null;
        }
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? unescape(m.group(1)) : null;
    }

    /** Lấy giá trị số đầu tiên của khóa. NaN nếu không có. */
    public static double numberField(String json, String key) {
        if (json == null) {
            return Double.NaN;
        }
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)")
                .matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : Double.NaN;
    }

    private static String unescape(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) {
                sb.append(c);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'u' -> {
                    if (i + 4 < s.length()) {
                        sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                }
                default -> sb.append(n);
            }
        }
        return sb.toString();
    }
}
