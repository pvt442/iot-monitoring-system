package com.pulseiot.desktop;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Mô hình dữ liệu nhận từ Backend và các hàm định dạng thời gian. */
final class Model {
    private Model() {}

    record Cfg(double tempTh, double humTh, double tempMargin, double humMargin, int timeout, LocalDateTime updatedAt) {
        static final Cfg DEFAULT = new Cfg(35, 80, 3, 5, 15, null);
    }

    record Dev(String id, String name, String location, boolean online, String alert,
               Double temp, Double hum, LocalDateTime lastSeen) {
        String label() { return name == null || name.isBlank() ? id : name; }
        /** Mức của từng chỉ số theo cấu hình hiện hành: 0 bình thường, 1 sắp vượt ngưỡng, 2 vượt ngưỡng (giống quy tắc của Backend). */
        static int level(Double v, double threshold, double margin) {
            if (v == null) return 0;
            double r = Math.round(v * 100) / 100.0;
            if (r > threshold) return 2;
            return margin > 0 && r >= threshold - margin ? 1 : 0;
        }

        int tempLevel(Cfg c) { return level(temp, c.tempTh(), c.tempMargin()); }

        int humLevel(Cfg c) { return level(hum, c.humTh(), c.humMargin()); }

        /** Bản sao với trạng thái cảnh báo tính lại từ số đo mới nhất và cấu hình hiện hành. */
        Dev eval(Cfg c) {
            int l = Math.max(tempLevel(c), humLevel(c));
            return new Dev(id, name, location, online, l == 2 ? "CRITICAL" : l == 1 ? "WARNING" : "NORMAL", temp, hum, lastSeen);
        }

        boolean critical() { return online && "CRITICAL".equals(alert); }
        boolean warning() { return online && "WARNING".equals(alert); }
    }

    record Summary(int total, int online, int offline, int alerting, int warning) {}

    record Overview(LocalDateTime serverTime, Summary summary, Cfg cfg, List<Dev> devices) {
        Dev find(String id) {
            for (Dev d : devices) if (d.id().equals(id)) return d;
            return null;
        }

        /** Bản sao với danh sách thiết bị mới; tổng hợp (online/offline/cảnh báo) được tính lại. */
        Overview withDevices(List<Dev> list) {
            List<Dev> sorted = new ArrayList<>();
            for (Dev d : list) sorted.add(d.eval(cfg));
            sorted.sort(Comparator.comparing(Dev::id));
            int on = 0, off = 0, bad = 0, near = 0;
            for (Dev d : sorted) {
                if (d.online()) on++; else off++;
                if (d.critical()) bad++;
                if (d.warning()) near++;
            }
            return new Overview(serverTime, new Summary(sorted.size(), on, off, bad, near), cfg, sorted);
        }
    }

    record HistoryRow(int id, String deviceId, double temp, double hum, LocalDateTime at) {}

    record AlertRow(long id, String deviceId, String deviceName, String metric, String level,
                    double value, double threshold, LocalDateTime at, String source) {}

    /** Chuỗi số đo (tăng dần theo thời gian) dùng cho biểu đồ. */
    static final class Series {
        static final Series EMPTY = new Series(new long[0], new double[0], new double[0], "");
        final long[] t;
        final double[] temp, hum;
        final String sig;

        Series(long[] t, double[] temp, double[] hum, String sig) {
            this.t = t; this.temp = temp; this.hum = hum; this.sig = sig;
        }

        int size() { return t.length; }

        double[] stats(boolean temperature) {
            double[] v = temperature ? temp : hum;
            if (v.length == 0) return null;
            double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE, sum = 0;
            for (double d : v) { lo = Math.min(lo, d); hi = Math.max(hi, d); sum += d; }
            return new double[]{lo, hi, sum / v.length};
        }
    }

    // ---------- Phân tích JSON ----------
    static LocalDateTime time(Jn n) { return time(n.text(null)); }

    static LocalDateTime time(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.length() > 23 ? s.substring(0, 23) : s);
        } catch (Exception e) {
            try { return OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(); }
            catch (Exception e2) { return null; }
        }
    }

    static Cfg cfg(Jn j) {
        return new Cfg(j.path("temperatureThreshold").dbl(35), j.path("humidityThreshold").dbl(80),
                j.path("temperatureWarningMargin").dbl(3), j.path("humidityWarningMargin").dbl(5),
                j.path("offlineTimeoutSeconds").intv(15), time(j.path("updatedAt")));
    }

    static Overview overview(Jn j) {
        Jn s = j.path("summary");
        List<Dev> devs = new ArrayList<>();
        for (Jn d : j.list("devices")) {
            Jn l = d.path("latest");
            boolean has = !l.isNull();
            devs.add(new Dev(d.path("deviceId").text(), d.path("name").text(null), d.path("location").text(null),
                    d.path("online").bool(), d.path("alertStatus").text("NORMAL"),
                    has ? l.path("temperature").dbl(0) : null, has ? l.path("humidity").dbl(0) : null,
                    time(d.path("lastSeen")) != null ? time(d.path("lastSeen")) : (has ? time(l.path("timestamp")) : null)));
        }
        LocalDateTime st = time(j.path("serverTime"));
        // Tổng hợp và trạng thái được tính lại từ số đo + ngưỡng hiện hành để mọi màn hình luôn khớp nhau
        return new Overview(st == null ? LocalDateTime.now() : st, new Summary(0, 0, 0, 0, 0),
                cfg(j.path("thresholds")), devs).withDevices(devs);
    }

    static HistoryRow history(Jn r) {
        return new HistoryRow(r.path("dataId").intv(0), r.path("deviceId").text(), r.path("temperature").dbl(0),
                r.path("humidity").dbl(0), time(r.path("timestamp")));
    }

    static AlertRow alert(Jn a) {
        return new AlertRow(a.path("id").lng(0), a.path("deviceId").text(), a.path("deviceName").text(null),
                a.path("metric").text(), a.path("level").text("CRITICAL"), a.path("value").dbl(0),
                a.path("threshold").dbl(0), time(a.path("createdAt")), a.path("source").text("BACKEND"));
    }

    /** Server trả về mới nhất trước; đảo lại thành tăng dần theo thời gian. */
    static Series series(Jn arr) {
        List<Jn> rows = arr.list();
        int n = rows.size();
        long[] t = new long[n];
        double[] te = new double[n], hu = new double[n];
        ZoneId z = ZoneId.systemDefault();
        long lastId = 0;
        for (int i = 0; i < n; i++) {
            Jn r = rows.get(n - 1 - i);
            LocalDateTime at = time(r.path("timestamp"));
            t[i] = at == null ? 0 : at.atZone(z).toInstant().toEpochMilli();
            te[i] = r.path("temperature").dbl(0);
            hu[i] = r.path("humidity").dbl(0);
            lastId = r.path("dataId").lng(0);
        }
        return new Series(t, te, hu, n + ":" + lastId + ":" + (n > 0 ? t[n - 1] : 0));
    }

    // ---------- Định dạng hiển thị ----------
    static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");
    static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    static String dt(LocalDateTime t) { return t == null ? "—" : t.format(DT); }

    static String hms(LocalDateTime t) { return t == null ? "--:--:--" : t.format(HMS); }

    static String num1(double v) { return String.format(Locale.US, "%.1f", v); }

    static String num2(double v) { return String.format(Locale.US, "%.2f", v); }

    /** "vừa xong", "12 giây trước", "5 phút trước", "3 giờ trước", "2 ngày trước". */
    static String ago(LocalDateTime t, LocalDateTime now) {
        if (t == null) return "Chưa có dữ liệu";
        long s = Math.max(0, Duration.between(t, now).getSeconds());
        if (s < 3) return "Vừa xong";
        if (s < 60) return s + " giây trước";
        if (s < 3600) return (s / 60) + " phút trước";
        if (s < 86400) return (s / 3600) + " giờ trước";
        return (s / 86400) + " ngày trước";
    }

    static String metricName(String m) { return "TEMPERATURE".equals(m) ? "Nhiệt độ" : "Độ ẩm"; }

    static String unit(String m) { return "TEMPERATURE".equals(m) ? "°C" : "%"; }
}
