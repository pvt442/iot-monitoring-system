package com.pulseiot.desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

import static com.pulseiot.desktop.Theme.*;

/** PULSE IoT - giao diện Desktop Java Swing (khung ứng dụng, thanh điều hướng, thanh trên cùng). */
final class Shell extends JLayeredPane implements App {
    private final Preferences prefs = Preferences.userNodeForPackage(PulseIoTApplication.class);
    private final Api api = new Api(System.getProperty("pulse.api", System.getenv("PULSE_API") != null ? System.getenv("PULSE_API") : "http://localhost:8080"));
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final Map<String, Page> pageMap = new LinkedHashMap<>();
    private final Map<String, NavItem> navItems = new LinkedHashMap<>();
    private final Ui.Toasts toasts = new Ui.Toasts();
    private final Ui.Modal modal = new Ui.Modal();
    private final Ui.Field topSearch = new Ui.Field("Tìm thiết bị theo mã hoặc tên...", Ic.SEARCH);
    private final ClockPill clock = new ClockPill();
    private final Ui.IconBtn bell = new Ui.IconBtn(Ic.BELL);
    private final Sidebar sidebar = new Sidebar();
    private final javax.swing.Timer pollTimer, secondTimer, frameTimer, alertTimer;

    private Model.Overview overview;
    private long offsetMs;
    private String current = "dashboard";
    private String selected;
    private int inflight;
    private long pollSeq;
    private final Map<String, Long> deletedAt = new HashMap<>();
    private final Map<String, Model.Dev> addedLocal = new HashMap<>();
    private final Map<String, Long> addedAt = new HashMap<>();
    private final Set<String> watchGone = new HashSet<>();
    private final List<java.util.function.Consumer<Model.Overview>> once = new ArrayList<>();
    private boolean connected = true, everConnected, lostNotified, syncing;
    private int alertsTotal, alertsSeen;
    private int refreshSec;
    private boolean toastOn;
    private final Set<String> critical = new HashSet<>();

    private final Ui.Background content = new Ui.Background();

    Shell() {
        refreshSec = prefs.getInt("refreshInterval", 2);
        toastOn = prefs.getBoolean("toastEnabled", true);
        alertsSeen = prefs.getInt("alertsSeen", 0);
        setOpaque(true);
        Ui.Background root = content;
        root.setLayout(new BorderLayout(22, 0));
        root.setBorder(new EmptyBorder(18, 20, 14, 20));
        add(root, JLayeredPane.DEFAULT_LAYER);
        root.add(sidebar, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(0, 14));
        right.setOpaque(false);
        right.add(topBar(), BorderLayout.NORTH);
        pages.setOpaque(false);
        right.add(pages, BorderLayout.CENTER);
        root.add(right, BorderLayout.CENTER);

        DevicesPage devices = new DevicesPage(this);
        pageMap.put("dashboard", new DashboardPage(this));
        pageMap.put("devices", devices);
        pageMap.put("history", new HistoryPage(this));
        pageMap.put("alerts", new AlertsPage(this));
        pageMap.put("config", new ConfigPage(this));
        pageMap.forEach((k, p) -> pages.add(p.view(), k));

        add(modal, JLayeredPane.MODAL_LAYER);
        modal.setVisible(false);
        add(toasts, JLayeredPane.POPUP_LAYER);

        bell.addActionListener(e -> go("alerts"));
        topSearch.getDocument().addDocumentListener(new DocumentListener() {
            void ch() {
                if (syncing) return;
                devices.setQuery(topSearch.getText());
                if (!topSearch.getText().isBlank() && !"devices".equals(current)) go("devices");
            }
            public void insertUpdate(DocumentEvent e) { ch(); }
            public void removeUpdate(DocumentEvent e) { ch(); }
            public void changedUpdate(DocumentEvent e) { ch(); }
        });

        pollTimer = new javax.swing.Timer(refreshSec * 1000, e -> poll());
        secondTimer = new javax.swing.Timer(1000, e -> { clock.repaint(); sidebar.repaint(); pageMap.get(current).second(); });
        frameTimer = new javax.swing.Timer(33, e -> pageMap.get(current).frame());
        alertTimer = new javax.swing.Timer(5000, e -> pollAlertCount());
        go("dashboard");
        poll();
        pollAlertCount();
        pollTimer.start();
        secondTimer.start();
        frameTimer.start();
        alertTimer.start();
    }

    void frameTick() { pageMap.get(current).frame(); }

    @Override public void doLayout() {
        for (Component c : getComponents()) c.setBounds(0, 0, getWidth(), getHeight());
    }

    // ------------------------------------------------------------------ App
    public Api api() { return api; }
    public LocalDateTime now() { return LocalDateTime.now().plus(Duration.ofMillis(offsetMs)); }
    public long nowMs() { return System.currentTimeMillis() + offsetMs; }
    public Model.Overview overview() { return overview; }
    public boolean connected() { return connected; }
    public int refreshSeconds() { return refreshSec; }
    public boolean toastEnabled() { return toastOn; }

    public void setRefreshSeconds(int s) {
        refreshSec = s;
        prefs.putInt("refreshInterval", s);
        pollTimer.setDelay(s * 1000);
        pollTimer.restart();
    }

    public void setToastEnabled(boolean on) { toastOn = on; prefs.putBoolean("toastEnabled", on); }

    public void toast(String title, String msg, Ui.ToastKind k) { toasts.show(title, msg, k); }

    public void modal(JComponent card, Runnable onEnter, Component focus) { modal.open(card, onEnter, focus); }

    public void closeModal() { modal.close(); }

    public void confirm(String title, String msg, String okText, boolean danger, Runnable ok) {
        Ui.Btn cancel = new Ui.Btn("Hủy", null, Ui.Kind.OUTLINE);
        Ui.Btn yes = new Ui.Btn(okText, null, danger ? Ui.Kind.DANGER : Ui.Kind.LIGHT);
        Runnable run = () -> { modal.close(); ok.run(); };
        cancel.addActionListener(e -> modal.close());
        yes.addActionListener(e -> run.run());
        modal.open(Ui.dialogCard(title, msg, null, cancel, yes), run, yes);
    }

    public String selectedDevice() {
        if (overview == null || overview.devices().isEmpty()) return selected;
        if (selected != null && overview.find(selected) != null) return selected;
        return overview.devices().get(0).id();
    }

    public void selectDevice(String id) {
        selected = id;
        if (overview != null) pageMap.values().forEach(p -> p.onOverview(overview));
    }

    public void refreshNow() { poll(true); pollAlertCount(); }

    public void refreshThen(java.util.function.Consumer<Model.Overview> cb) {
        once.add(cb);
        poll(true);
    }

    public void deviceAdded(Model.Dev dev) {
        deletedAt.remove(dev.id());
        addedLocal.put(dev.id(), dev);
        addedAt.put(dev.id(), System.currentTimeMillis());
        Model.Overview base = overview == null ? new Model.Overview(now(), new Model.Summary(0, 0, 0, 0, 0), Model.Cfg.DEFAULT, List.of()) : overview;
        List<Model.Dev> list = new ArrayList<>(base.devices());
        list.removeIf(d -> d.id().equals(dev.id()));
        list.add(dev);
        overview = base.withDevices(list);
        pageMap.values().forEach(p -> p.onOverview(overview));
        sidebar.repaint();
        poll(true);
    }

    public void deviceDeleted(String id) {
        deletedAt.put(id, System.currentTimeMillis());
        watchGone.add(id);
        addedLocal.remove(id);
        if (overview != null) {
            List<Model.Dev> list = new ArrayList<>(overview.devices());
            list.removeIf(d -> d.id().equals(id));
            overview = overview.withDevices(list);
            if (id.equals(selected)) selected = null;
            pageMap.values().forEach(p -> p.onOverview(overview));
            sidebar.repaint();
        }
        javax.swing.Timer t = new javax.swing.Timer(6500, e -> poll(true));
        t.setRepeats(false);
        t.start();
    }

    public void watchFeed(String id) {
        javax.swing.Timer t = new javax.swing.Timer(9000, e -> refreshThen(o -> {
            Model.Dev d = o.find(id);
            if (d == null) return;
            boolean live = d.online() && d.lastSeen() != null && Duration.between(d.lastSeen(), now()).getSeconds() <= 6;
            if (live) toast(id + " đang gửi dữ liệu liên tục", "IoT Client đã nhận thiết bị mới và cập nhật số đo mỗi vài giây.", Ui.ToastKind.SUCCESS);
            else toast(id + " chưa nhận được dữ liệu liên tục",
                    "Thiết bị đã lưu trên Server nhưng chưa có số đo mới. Hãy bật IoT Client (run-simulator.bat) để thiết bị tự gửi dữ liệu, nếu không sau " + o.cfg().timeout() + " giây thiết bị sẽ chuyển Offline.",
                    Ui.ToastKind.WARN);
        }));
        t.setRepeats(false);
        t.start();
    }

    public void pageSearch(String text) {
        syncing = true;
        if (!topSearch.getText().equals(text)) topSearch.setText(text);
        syncing = false;
    }

    public void go(String key) {
        pageMap.get(current).hidden();
        current = key;
        cards.show(pages, key);
        navItems.forEach((k, n) -> n.setActive(k.equals(key)));
        pageMap.get(key).shown();
        if (overview != null) pageMap.get(key).onOverview(overview);
        if ("alerts".equals(key)) markAlertsSeen();
    }

    // ------------------------------------------------------------------ truy vấn dữ liệu
    private void poll() { poll(false); }

    /** force = true: gửi yêu cầu ngay cả khi đang có lượt tải khác; chỉ kết quả của lượt mới nhất được áp dụng. */
    private void poll(boolean force) {
        if (inflight > 0 && !force) return;
        inflight++;
        long seq = ++pollSeq;
        api.get("/api/dashboard/overview", j -> {
            inflight = Math.max(0, inflight - 1);
            if (seq != pollSeq) return;
            Model.Overview o = patch(Model.overview(j));
            offsetMs = Duration.between(LocalDateTime.now(), o.serverTime()).toMillis();
            if (!connected && everConnected) toast("Đã kết nối lại", "Server đang hoạt động trở lại.", Ui.ToastKind.SUCCESS);
            connected = true;
            everConnected = true;
            lostNotified = false;
            overview = o;
            detectAlerts(o);
            sidebar.repaint();
            pageMap.get(current).onOverview(o);
            List<java.util.function.Consumer<Model.Overview>> run = new ArrayList<>(once);
            once.clear();
            run.forEach(c -> c.accept(o));
        }, err -> {
            inflight = Math.max(0, inflight - 1);
            if (seq != pollSeq) return;
            connected = false;
            sidebar.repaint();
            if (!lostNotified) { lostNotified = true; toast("Mất kết nối Server", err, Ui.ToastKind.ERROR); }
        });
    }

    /** Áp các thay đổi người dùng vừa làm (thêm/xóa) lên kết quả Server để màn hình không bị "giật lùi". */
    private Model.Overview patch(Model.Overview o) {
        long now = System.currentTimeMillis();
        List<Model.Dev> list = new ArrayList<>(o.devices());
        boolean changed = false;
        // thiết bị vừa xóa: ẩn trong 6 giây; sau đó nếu còn xuất hiện lại thì báo rõ nguyên nhân
        for (Iterator<Map.Entry<String, Long>> it = deletedAt.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Long> e = it.next();
            boolean present = list.stream().anyMatch(d -> d.id().equals(e.getKey()));
            if (now - e.getValue() <= 6000) {
                if (present) { list.removeIf(d -> d.id().equals(e.getKey())); changed = true; }
            } else {
                it.remove();
                if (present && watchGone.remove(e.getKey())) {
                    toast(e.getKey() + " xuất hiện lại",
                            "IoT Client (bản cũ) vẫn đang gửi dữ liệu cho thiết bị này nên Server tự đăng ký lại. Hãy tắt và chạy lại IoT Client bản mới để dừng hẳn.",
                            Ui.ToastKind.WARN);
                } else watchGone.remove(e.getKey());
            }
        }
        // thiết bị vừa thêm: giữ lại cho tới khi Server trả về (tối đa 8 giây)
        for (Iterator<Map.Entry<String, Model.Dev>> it = addedLocal.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Model.Dev> e = it.next();
            boolean present = list.stream().anyMatch(d -> d.id().equals(e.getKey()));
            if (present || now - addedAt.getOrDefault(e.getKey(), 0L) > 8000) { it.remove(); addedAt.remove(e.getKey()); }
            else { list.add(e.getValue()); changed = true; }
        }
        return changed ? o.withDevices(list) : o;
    }

    private void detectAlerts(Model.Overview o) {
        Set<String> now = new HashSet<>();
        for (Model.Dev d : o.devices()) if (d.critical()) now.add(d.id());
        if (toastOn && everConnected) {
            for (String id : now) {
                if (critical.contains(id)) continue;
                Model.Dev d = o.find(id);
                StringBuilder sb = new StringBuilder();
                if (d.temp() != null && d.temp() > o.cfg().tempTh()) sb.append("Nhiệt độ ").append(Model.num1(d.temp())).append(" °C (> ").append(Model.num1(o.cfg().tempTh())).append(")  ");
                if (d.hum() != null && d.hum() > o.cfg().humTh()) sb.append("Độ ẩm ").append(Model.num1(d.hum())).append(" % (> ").append(Model.num1(o.cfg().humTh())).append(")");
                toast(id + " vượt ngưỡng", sb.toString().trim(), Ui.ToastKind.WARN);
            }
        }
        critical.clear();
        critical.addAll(now);
    }

    private void pollAlertCount() {
        api.get("/api/alerts?page=0&size=1", j -> {
            alertsTotal = (int) j.path("totalElements").lng(0);
            if ("alerts".equals(current)) markAlertsSeen();
            bell.setBadge(Math.max(0, alertsTotal - alertsSeen));
        }, err -> {});
    }

    private void markAlertsSeen() {
        alertsSeen = alertsTotal;
        prefs.putInt("alertsSeen", alertsSeen);
        bell.setBadge(0);
    }

    // ------------------------------------------------------------------ giao diện khung
    private JComponent topBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setOpaque(false);
        Ui.IconBtn menu = new Ui.IconBtn(Ic.MENU);
        menu.addActionListener(e -> { sidebar.setVisible(!sidebar.isVisible()); content.revalidate(); content.repaint(); });
        bar.add(menu, BorderLayout.WEST);
        topSearch.setPreferredSize(new Dimension(300, 54));
        bar.add(topSearch, BorderLayout.CENTER);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        r.setOpaque(false);
        r.add(clock);
        r.add(bell);
        bar.add(r, BorderLayout.EAST);
        bar.setBorder(new EmptyBorder(0, 0, 0, 6));
        return bar;
    }

    private final class ClockPill extends JComponent {
        ClockPill() { setPreferredSize(new Dimension(268, 54)); }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            Shape s = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 36, 36);
            g.setColor(Theme.CARD);
            g.fill(s);
            g.setColor(Theme.LINE);
            g.draw(s);
            Ic.CLOCK.paint(g, 20, (h - 22) / 2.0, 22, Theme.MUTED);
            Theme.textMid(g, Model.dt(now()), 54, 0, h, Theme.bold(16.5f), Theme.TEXT);
            g.dispose();
        }
    }

    private final class NavItem extends JComponent {
        private final String key, text;
        private final Ic icon;
        private boolean active, hover;

        NavItem(String key, String text, Ic icon) {
            this.key = key; this.text = text; this.icon = icon;
            setPreferredSize(new Dimension(100, 56));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            setAlignmentX(0f);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { go(NavItem.this.key); }
            });
        }

        void setActive(boolean a) { active = a; repaint(); }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            if (active) {
                g.setColor(Theme.TEAL);
                g.fillRoundRect(-4, h / 2 - 12, 8, 24, 6, 6);
                g.setColor(new Color(244, 247, 250));
                g.fillRoundRect(14, 2, w - 28, h - 4, 26, 26);
            } else if (hover) {
                g.setColor(new Color(255, 255, 255, 14));
                g.fillRoundRect(14, 2, w - 28, h - 4, 26, 26);
            }
            Color ic = active ? new Color(13, 148, 136) : Theme.MUTED;
            icon.paint(g, 36, (h - 24) / 2.0, 24, ic, 1.8f);
            Theme.textMid(g, text, 74, 0, h, active ? Theme.bold(16f) : Theme.semi(16f), active ? new Color(15, 22, 30) : new Color(226, 232, 240));
            g.dispose();
        }
    }

    private final class Sidebar extends Ui.Card {
        Sidebar() {
            super(null);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            radius(30);
            fill(new Color(22, 29, 37));
            glow(new Color(20, 110, 104, 90), 0.1f, 0.0f);
            setPreferredSize(new Dimension(282, 100));
            setBorder(new EmptyBorder(26, 0, 20, 0));

            JComponent brand = new JComponent() {
                { setPreferredSize(new Dimension(100, 80)); setMaximumSize(new Dimension(Integer.MAX_VALUE, 80)); setAlignmentX(0f); }

                @Override protected void paintComponent(Graphics gr) {
                    Graphics2D g = Theme.quality(gr);
                    int x = 28, y = 6;
                    g.setColor(new Color(20, 80, 78));
                    g.fillRoundRect(x, y, 56, 56, 20, 20);
                    g.setColor(alpha(TEAL, 140));
                    g.setStroke(new BasicStroke(1.4f));
                    g.drawRoundRect(x, y, 56, 56, 20, 20);
                    Ic.LOGO.paint(g, x + 13, y + 13, 30, TEAL, 2.0f);
                    Theme.text(g, "Pulse IoT", x + 72, y + 24, Theme.bold(26f), Theme.TEXT);
                    Theme.text(g, "Real-time Sensor", x + 72, y + 42, Theme.regular(13f), Theme.MUTED);
                    Theme.text(g, "Monitoring System", x + 72, y + 58, Theme.regular(13f), Theme.MUTED);
                    g.dispose();
                }
            };
            add(brand);
            add(Ui.gap(0, 26));
            JLabel nav = Ui.label("Điều hướng", Theme.regular(14f), Theme.MUTED);
            nav.setBorder(new EmptyBorder(0, 30, 0, 0));
            nav.setAlignmentX(0f);
            add(nav);
            add(Ui.gap(0, 12));
            String[][] items = {{"dashboard", "Dashboard"}, {"devices", "Devices"}, {"history", "Data History"}, {"alerts", "Alert Logs"}, {"config", "Configuration"}};
            Ic[] icons = {Ic.DASH, Ic.CHIP, Ic.HISTORY, Ic.BELL, Ic.GEAR};
            for (int i = 0; i < items.length; i++) {
                NavItem n = new NavItem(items[i][0], items[i][1], icons[i]);
                navItems.put(items[i][0], n);
                add(n);
                add(Ui.gap(0, 6));
            }
            add(Box.createVerticalGlue());
            JComponent status = new JComponent() {
                { setPreferredSize(new Dimension(100, 126)); setMaximumSize(new Dimension(Integer.MAX_VALUE, 126)); setAlignmentX(0f); }

                @Override protected void paintComponent(Graphics gr) {
                    Graphics2D g = Theme.quality(gr);
                    int w = getWidth();
                    g.setColor(new Color(31, 39, 48));
                    g.fillRoundRect(22, 0, w - 44, 78, 28, 28);
                    g.setColor(Theme.LINE);
                    g.drawRoundRect(22, 0, w - 45, 77, 28, 28);
                    boolean ok = connected;
                    Color c = ok ? Theme.TEAL : Theme.RED;
                    g.setColor(alpha(c, 50));
                    g.fill(new Ellipse2D.Double(38, 21, 20, 20));
                    g.setColor(c);
                    g.fill(new Ellipse2D.Double(43, 26, 10, 10));
                    Theme.text(g, ok ? "Server hoạt động" : "Mất kết nối Server", 68, 38, Theme.bold(15.5f), Theme.TEXT);
                    int on = overview == null ? 0 : overview.summary().online(), total = overview == null ? 0 : overview.summary().total();
                    Theme.text(g, ok ? on + "/" + total + " thiết bị đang online" : "Đang thử kết nối lại...", 40, 62, Theme.regular(13.5f), Theme.MUTED);
                    String v = "IoT Data Monitor Server v1.0";
                    FontMetrics fm = g.getFontMetrics(Theme.regular(13f));
                    Theme.text(g, v, (w - fm.stringWidth(v)) / 2, 112, Theme.regular(13f), Theme.MUTED);
                    g.dispose();
                }
            };
            add(status);
        }
    }

    static Image appIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = Theme.quality(img.getGraphics());
        g.setColor(new Color(16, 70, 68));
        g.fillRoundRect(2, 2, 60, 60, 20, 20);
        g.setColor(Theme.TEAL);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(2, 2, 60, 60, 20, 20);
        Ic.LOGO.paint(g, 12, 12, 40, Theme.TEAL, 2.2f);
        g.dispose();
        return img;
    }

}
