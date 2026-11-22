package com.pulseiot.desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;

/** Giao diện ứng dụng cung cấp cho các trang. */
interface App {
    Api api();
    LocalDateTime now();
    long nowMs();
    Model.Overview overview();
    void toast(String title, String msg, Ui.ToastKind kind);
    void go(String page);
    void confirm(String title, String msg, String okText, boolean danger, Runnable ok);
    void modal(JComponent card, Runnable onEnter, Component focus);
    void closeModal();
    String selectedDevice();
    void selectDevice(String id);
    int refreshSeconds();
    void setRefreshSeconds(int s);
    boolean toastEnabled();
    void setToastEnabled(boolean on);
    void refreshNow();
    void pageSearch(String text);
    boolean connected();
    /** Tải lại tổng quan ngay (kể cả khi đang có lượt tải khác) và gọi cb khi dữ liệu mới về. */
    void refreshThen(java.util.function.Consumer<Model.Overview> cb);
    /** Thiết bị vừa được Server lưu: hiện ngay trên mọi màn hình, không chờ chu kỳ cập nhật. */
    void deviceAdded(Model.Dev dev);
    /** Thiết bị vừa được Server xóa: biến mất ngay trên mọi màn hình. */
    void deviceDeleted(String id);
    /** Sau vài giây kiểm tra thiết bị có đang nhận dữ liệu liên tục không và báo cho người dùng. */
    void watchFeed(String id);
}

/** Lớp cơ sở của một trang nội dung (có thanh cuộn riêng). */
abstract class Page {
    final App app;
    final JPanel body = new Ui.ScrollBody();
    private JScrollPane scroller;

    Page(App app) {
        this.app = app;
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(4, 0, 30, 6));
    }

    JComponent view() {
        if (scroller == null) scroller = Ui.scroll(body);
        return scroller;
    }

    /** Thêm một khối vào trang, kèm khoảng cách phía dưới. */
    void add(JComponent c, int gapBelow) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(c);
        if (gapBelow > 0) body.add(Ui.gap(0, gapBelow));
    }

    /** Tiêu đề trang: tên + mô tả ngắn và các nút hành động bên phải. */
    static JComponent header(String title, String sub, JComponent... actions) {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Ui.label(title, Theme.bold(32f), Theme.TEXT));
        text.add(Ui.gap(0, 6));
        text.add(Ui.label(sub, Theme.regular(15f), Theme.MUTED));
        p.add(text, BorderLayout.CENTER);
        if (actions.length > 0) {
            JPanel a = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            a.setOpaque(false);
            for (JComponent c : actions) a.add(c);
            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setOpaque(false);
            wrap.add(a);
            p.add(wrap, BorderLayout.EAST);
        }
        p.setBorder(new EmptyBorder(6, 0, 4, 0));
        return p;
    }

    /** Nhãn nhỏ phía trên một ô nhập. */
    static JComponent labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.add(Ui.label(label, Theme.semi(14.5f), Theme.TEXT), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    void shown() {}

    void hidden() {}

    void onOverview(Model.Overview o) {}

    /** Gọi mỗi giây khi trang đang hiển thị. */
    void second() {}

    /** Gọi ~30 lần/giây khi trang đang hiển thị (hoạt ảnh). */
    void frame() {}

    static String tempText(Double t) { return t == null ? "—" : Model.num1(t) + " °C"; }

    static String humText(Double h) { return h == null ? "—" : Model.num1(h) + " %"; }

    static Ui.Badge statusBadge(Model.Dev d) {
        if (!d.online()) return Ui.offline();
        if (d.critical()) return Ui.critical();
        if (d.warning()) return Ui.warn();
        return Ui.online();
    }

    static final Color TEMP_DIM = new Color(176, 122, 28), HUM_DIM = new Color(34, 140, 165);

    /** Màu số nhiệt độ: đỏ khi vượt ngưỡng, vàng khi sắp vượt, cam khi bình thường, mờ khi Offline. */
    static Color tempColor(Model.Dev d, Model.Cfg c) {
        if (!d.online()) return TEMP_DIM;
        int l = d.tempLevel(c);
        return l == 2 ? Theme.RED : l == 1 ? Theme.AMBER : Theme.ORANGE;
    }

    static Color humColor(Model.Dev d, Model.Cfg c) {
        if (!d.online()) return HUM_DIM;
        int l = d.humLevel(c);
        return l == 2 ? Theme.RED : l == 1 ? Theme.AMBER : Theme.CYAN;
    }

    /** Nhãn trạng thái riêng cho một chỉ số (level 0/1/2). */
    static Ui.Badge metricBadge(Model.Dev d, int level) {
        if (!d.online()) return new Ui.Badge("Dữ liệu đã cũ", Ic.WIFI_OFF, Theme.MUTED, new Color(255, 255, 255, 20));
        if (level == 2) return Ui.critical();
        if (level == 1) return Ui.warn();
        return new Ui.Badge("Bình thường", Ic.CHECK_CIRCLE, Theme.GREEN, Theme.alpha(Theme.GREEN, 36));
    }

    static Color dotColor(Model.Dev d) {
        if (!d.online()) return new Color(120, 129, 141);
        if (d.critical()) return Theme.RED;
        if (d.warning()) return Theme.AMBER;
        return Theme.GREEN;
    }
}
