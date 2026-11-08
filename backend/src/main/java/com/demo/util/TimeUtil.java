package com.demo.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.demo.exception.ApiException;

/** Chuẩn hóa định dạng thời gian vào/ra API: ISO 8601, giờ địa phương của Server, ví dụ 2026-11-07T14:35:02.123 */
public final class TimeUtil {

    public static final LocalDateTime MIN = LocalDateTime.of(2000, 1, 1, 0, 0);
    public static final LocalDateTime MAX = LocalDateTime.of(2999, 12, 31, 23, 59, 59);

    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private TimeUtil() {
    }

    public static String format(LocalDateTime t) {
        return t == null ? null : t.format(OUT);
    }

    /** Chấp nhận "2026-11-07", "2026-11-07T14:35:02(.123)" hoặc có múi giờ ("...Z", "...+07:00"). */
    public static LocalDateTime parse(String text, boolean endOfDay) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String s = text.trim();
        try {
            if (s.length() == 10) {
                LocalDate d = LocalDate.parse(s);
                return endOfDay ? d.atTime(LocalTime.of(23, 59, 59, 999_000_000)) : d.atStartOfDay();
            }
            if (s.endsWith("Z") || s.matches(".*[+-]\\d{2}:\\d{2}$")) {
                return OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            }
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ex) {
            throw ApiException.badRequest(
                    "Thời gian '" + text + "' sai định dạng. Dùng ISO 8601, ví dụ 2026-11-07T14:35:02.");
        }
    }

    public static LocalDateTime parseOrDefault(String text, LocalDateTime fallback) {
        LocalDateTime t = parse(text, false);
        return t == null ? fallback : t;
    }
}
