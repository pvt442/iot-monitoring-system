package com.pulseiot.desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Locale;

import static com.pulseiot.desktop.Theme.*;

final class ConfigPage extends Page {
    private final Ui.Field fTemp = new Ui.Field(null, null), fHum = new Ui.Field(null, null),
            fTempM = new Ui.Field(null, null), fHumM = new Ui.Field(null, null), fTimeout = new Ui.Field(null, null);
    private final JLabel updated = Ui.label("Cập nhật lần cuối: —", regular(14.5f), MUTED);
    private final Ui.Combo interval = new Ui.Combo("1 giây", "2 giây", "3 giây", "5 giây", "10 giây");
    private static final int[] SECS = {1, 2, 3, 5, 10};
    private boolean loaded;
    private Model.Cfg saved;

    ConfigPage(App app) {
        super(app);
        Ui.Btn reset = new Ui.Btn("Khôi phục mặc định", Ic.RESET, Ui.Kind.OUTLINE);
        reset.addActionListener(e -> app.confirm("Khôi phục cấu hình mặc định?",
                "Ngưỡng 35 °C / 80 %, biên 3 °C / 5 %, timeout offline 15 giây.", "Khôi phục", false, this::doReset));
        add(header("Cài đặt cấu hình", "Thiết lập ngưỡng cảnh báo cho cả hệ thống và tùy chọn hiển thị.", reset), 18);

        JPanel row = new JPanel(new Ui.Ratio(22, 0, 2, 1));
        row.setOpaque(false);
        row.add(thresholdCard());
        row.add(displayCard());
        add(row, 0);
    }

    private JComponent thresholdCard() {
        Ui.Card c = new Ui.Card(new BorderLayout(0, 18)).pad(30, 32, 30, 32);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JPanel t = new JPanel();
        t.setLayout(new BoxLayout(t, BoxLayout.X_AXIS));
        t.setOpaque(false);
        t.setAlignmentX(0f);
        t.add(Ui.label("Ngưỡng cảnh báo & trạng thái thiết bị", bold(21f), TEXT));
        t.add(Ui.gap(14, 0));
        t.add(new Ui.BadgeLabel(new Ui.Badge("Lưu trong MySQL · dùng chung", null, new Color(86, 214, 236), alpha(CYAN, 34)), 32));
        t.add(Box.createHorizontalGlue());
        head.add(t);
        head.add(Ui.gap(0, 8));
        updated.setAlignmentX(0f);
        head.add(updated);
        c.add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 28, 22));
        grid.setOpaque(false);
        grid.add(field("Ngưỡng nhiệt độ (°C)", fTemp, "Cảnh báo khi nhiệt độ > giá trị này. Mặc định 35."));
        grid.add(field("Ngưỡng độ ẩm (%)", fHum, "Cảnh báo khi độ ẩm > giá trị này. Mặc định 80."));
        grid.add(field("Biên “sắp vượt ngưỡng” – nhiệt độ (°C)", fTempM, "Vùng cảnh báo sớm = ngưỡng − biên. Mặc định 3."));
        grid.add(field("Biên “sắp vượt ngưỡng” – độ ẩm (%)", fHumM, "Vùng cảnh báo sớm = ngưỡng − biên. Mặc định 5."));
        grid.add(field("Timeout Offline (giây)", fTimeout, "Quá thời gian này không có dữ liệu thì thiết bị Offline. Mặc định 15."));
        c.add(grid, BorderLayout.CENTER);

        Ui.Btn save = new Ui.Btn("Lưu cấu hình", Ic.SAVE, Ui.Kind.TEAL);
        save.addActionListener(e -> save());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        foot.setOpaque(false);
        foot.add(save);
        c.add(foot, BorderLayout.SOUTH);
        return c;
    }

    private static JComponent field(String label, Ui.Field f, String hint) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = Ui.label(label, semi(15f), TEXT);
        l.setAlignmentX(0f);
        p.add(l);
        p.add(Ui.gap(0, 9));
        f.setAlignmentX(0f);
        f.setPreferredSize(new Dimension(200, 50));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        p.add(f);
        p.add(Ui.gap(0, 8));
        p.add(new Ui.Wrapped(hint, regular(13.5f), MUTED, 2));
        return p;
    }

    private JComponent displayCard() {
        Ui.Card c = new Ui.Card(new BorderLayout(0, 14)).pad(30, 30, 30, 30);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel t = Ui.label("Tùy chọn hiển thị", bold(21f), TEXT);
        t.setAlignmentX(0f);
        head.add(t);
        head.add(Ui.gap(0, 10));
        Ui.BadgeLabel b = new Ui.BadgeLabel(new Ui.Badge("Chỉ áp dụng trên máy này", null, MUTED, new Color(255, 255, 255, 20)), 32);
        b.setAlignmentX(0f);
        head.add(b);
        c.add(head, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        interval.setPreferredSize(new Dimension(130, 48));
        interval.setSelectedIndex(indexOf(app.refreshSeconds()));
        interval.addActionListener(e -> app.setRefreshSeconds(SECS[interval.getSelectedIndex()]));
        list.add(option("Chu kỳ cập nhật", "Tần suất Dashboard hỏi Server.", interval));
        JComponent line = new JComponent() {
            { setPreferredSize(new Dimension(10, 1)); setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); }

            @Override protected void paintComponent(Graphics g) { g.setColor(Theme.LINE); g.fillRect(0, 0, getWidth(), 1); }
        };
        line.setAlignmentX(0f);
        list.add(Ui.gap(0, 18));
        list.add(line);
        list.add(Ui.gap(0, 18));
        list.add(option("Thông báo nổi (Toast)", "Hiện hộp thông báo khi có thiết bị mới vượt ngưỡng.",
                new Ui.Switch(app.toastEnabled(), app::setToastEnabled)));
        c.add(list, BorderLayout.CENTER);
        return c;
    }

    private static int indexOf(int s) {
        for (int i = 0; i < SECS.length; i++) if (SECS[i] == s) return i;
        return 1;
    }

    private static JComponent option(String title, String desc, JComponent control) {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);
        p.setAlignmentX(0f);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(Ui.label(title, bold(16f), TEXT));
        t.add(Ui.gap(0, 5));
        t.add(new Ui.Wrapped(desc, regular(13.5f), MUTED, 2));
        p.add(t, BorderLayout.CENTER);
        JPanel r = new JPanel(new GridBagLayout());
        r.setOpaque(false);
        r.add(control);
        p.add(r, BorderLayout.EAST);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        return p;
    }

    @Override void shown() { load(); }

    private void load() {
        app.api().get("/api/config", j -> fill(Model.cfg(j)), err -> app.toast("Không tải được cấu hình", err, Ui.ToastKind.ERROR));
    }

    private static String n(double d) { return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.US, "%.1f", d); }

    private void fill(Model.Cfg c) {
        saved = c;
        fTemp.setText(n(c.tempTh()));
        fHum.setText(n(c.humTh()));
        fTempM.setText(n(c.tempMargin()));
        fHumM.setText(n(c.humMargin()));
        fTimeout.setText(String.valueOf(c.timeout()));
        updated.setText("Cập nhật lần cuối: " + Model.dt(c.updatedAt()));
        loaded = true;
    }

    private double num(Ui.Field f, double lo, double hi, String name) {
        try {
            double v = Double.parseDouble(f.getText().trim().replace(',', '.'));
            if (v < lo || v > hi) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            f.invalid = true;
            f.repaint();
            throw new IllegalArgumentException(name + " phải nằm trong khoảng " + n(lo) + " – " + n(hi) + ".");
        }
    }

    private static boolean same(double a, double b) { return Math.abs(a - b) < 0.0005; }

    /** Liệt kê những mục thay đổi: "Ngưỡng nhiệt độ 35 → 30 °C · ...". */
    private static String diff(Model.Cfg a, Model.Cfg b) {
        java.util.List<String> l = new java.util.ArrayList<>();
        if (!same(a.tempTh(), b.tempTh())) l.add("Ngưỡng nhiệt độ " + n(a.tempTh()) + " → " + n(b.tempTh()) + " °C");
        if (!same(a.humTh(), b.humTh())) l.add("Ngưỡng độ ẩm " + n(a.humTh()) + " → " + n(b.humTh()) + " %");
        if (!same(a.tempMargin(), b.tempMargin())) l.add("Biên nhiệt độ " + n(a.tempMargin()) + " → " + n(b.tempMargin()) + " °C");
        if (!same(a.humMargin(), b.humMargin())) l.add("Biên độ ẩm " + n(a.humMargin()) + " → " + n(b.humMargin()) + " %");
        if (a.timeout() != b.timeout()) l.add("Timeout Offline " + a.timeout() + " → " + b.timeout() + " giây");
        return String.join(" · ", l);
    }

    private void save() {
        try {
            double t = num(fTemp, -40, 125, "Ngưỡng nhiệt độ"), h = num(fHum, 0, 100, "Ngưỡng độ ẩm");
            double tm = num(fTempM, 0, 50, "Biên nhiệt độ"), hm = num(fHumM, 0, 50, "Biên độ ẩm");
            int to = (int) Math.round(num(fTimeout, 3, 3600, "Timeout Offline"));
            Model.Cfg wanted = new Model.Cfg(t, h, tm, hm, to, null);
            Model.Cfg before = saved == null ? Model.Cfg.DEFAULT : saved;
            if (saved != null && diff(before, wanted).isEmpty()) {
                app.toast("Chưa có thay đổi nào", "Các giá trị đang giống cấu hình đã lưu trên Server.", Ui.ToastKind.INFO);
                return;
            }
            String body = String.format(Locale.US, "{\"temperatureThreshold\":%s,\"humidityThreshold\":%s,\"temperatureWarningMargin\":%s,"
                    + "\"humidityWarningMargin\":%s,\"offlineTimeoutSeconds\":%d}", t, h, tm, hm, to);
            app.api().send("PUT", "/api/config", body, j -> verify(before, wanted, "Đã lưu cấu hình vào Server"),
                    err -> app.toast("Không lưu được cấu hình", err + " Cấu hình trên Server vẫn giữ nguyên giá trị cũ.", Ui.ToastKind.ERROR));
        } catch (IllegalArgumentException ex) {
            app.toast("Giá trị không hợp lệ", ex.getMessage(), Ui.ToastKind.WARN);
        }
    }

    /** Đọc lại cấu hình từ Server để chắc chắn đã lưu thật, rồi báo rõ hệ thống đang chạy theo giá trị nào. */
    private void verify(Model.Cfg before, Model.Cfg wanted, String title) {
        app.api().get("/api/config", j -> {
            Model.Cfg now = Model.cfg(j);
            fill(now);
            if (wanted != null && !diff(wanted, now).isEmpty()) {
                app.toast("Server lưu giá trị khác với đã nhập", diff(wanted, now), Ui.ToastKind.ERROR);
                return;
            }
            String d = diff(before, now);
            app.toast(title, d.isEmpty() ? "Cấu hình không thay đổi so với trước." : d + ". Đã xác nhận lại từ MySQL lúc " + Model.hms(app.now()) + ".", Ui.ToastKind.SUCCESS);
            app.refreshThen(o -> {
                Model.Summary s = o.summary();
                app.toast("Hệ thống đang chạy theo cấu hình mới",
                        s.alerting() + " thiết bị vượt ngưỡng · " + s.warning() + " sắp vượt ngưỡng · " + s.offline() + " Offline (timeout "
                                + o.cfg().timeout() + " giây). Nhật ký cảnh báo sẽ ghi thêm khi số đo mới chuyển sang vượt ngưỡng.",
                        s.alerting() > 0 ? Ui.ToastKind.WARN : Ui.ToastKind.INFO);
            });
        }, err -> app.toast("Đã gửi lưu nhưng chưa xác nhận được", err, Ui.ToastKind.WARN));
    }

    private void doReset() {
        Model.Cfg before = saved == null ? Model.Cfg.DEFAULT : saved;
        app.api().send("POST", "/api/config/reset", null, j -> verify(before, Model.Cfg.DEFAULT, "Đã khôi phục cấu hình mặc định"),
                err -> app.toast("Không khôi phục được", err, Ui.ToastKind.ERROR));
    }
}
