package com.pulseiot.desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import static com.pulseiot.desktop.Theme.*;

final class DashboardPage extends Page {
    private static final int[] MINUTES = {1, 5, 15, 60};
    private static final String[] RANGE_LABELS = {"1 phút", "5 phút", "15 phút", "1 giờ"};
    private int rangeIdx = 1;

    // hero
    private final StatTile tTotal = new StatTile(Ic.CHIP, "Tổng số thiết bị", TEAL);
    private final StatTile tOnline = new StatTile(Ic.WIFI, "Đang Online", TEAL);
    private final StatTile tOffline = new StatTile(Ic.WIFI_OFF, "Đang Offline", new Color(150, 160, 172));
    private final StatTile tAlert = new StatTile(Ic.CHECK_CIRCLE, "Cảnh báo", TEAL);

    // thiết bị đang xem
    private final JLabel vId = Ui.label("—", bold(34f), TEXT);
    private final Ui.BadgeLabel vBadge = new Ui.BadgeLabel(Ui.offline(), 34);
    private final JLabel vTime = Ui.label("--:--:--", bold(50f), TEXT);
    private final JLabel vAgo = Ui.label("Chưa có dữ liệu", regular(15f), MUTED);
    private final JLabel vName = new JLabel("—", Ic.CHIP.icon(19, MUTED), SwingConstants.LEFT);
    private final JLabel vLoc = new JLabel("—", Ic.PIN.icon(19, MUTED), SwingConstants.LEFT);
    private final JLabel vStamp = new JLabel("—", Ic.CLOCK.icon(19, MUTED), SwingConstants.LEFT);

    // chip thiết bị
    private final JPanel chips = new JPanel(new Ui.WrapLayout(FlowLayout.LEFT, 14, 12));
    private String chipSig = "";

    // đồng hồ + tóm tắt
    private final GaugeCard gTemp = new GaugeCard(true);
    private final GaugeCard gHum = new GaugeCard(false);
    private final SumTile sCount = new SumTile("Số bản ghi nhận được", TEXT);
    private final SumTile sAvgT = new SumTile("Nhiệt độ trung bình", ORANGE);
    private final SumTile sAvgH = new SumTile("Độ ẩm trung bình", CYAN);
    private final JLabel sFrame = Ui.label("Khung thời gian: 5 phút gần nhất", regular(14.5f), MUTED);

    // biểu đồ
    private final Charts.Trend cTemp = new Charts.Trend(true, ORANGE, "°C");
    private final Charts.Trend cHum = new Charts.Trend(false, CYAN, "%");
    private final JLabel trendTitle = Ui.label("Biểu đồ xu hướng", bold(21f), TEXT);
    private final JLabel trendSub = Ui.label("Cập nhật tự động · hiển thị 0 bản ghi", regular(14f), MUTED);
    private final Ui.Segmented range = new Ui.Segmented(RANGE_LABELS, 1, i -> {
        rangeIdx = i;
        resetSeries();
        loadSeries();
    });

    // thiết bị gần đây
    private final Ui.Grid recent = new Ui.Grid(
            new Ui.Grid.Col("Mã thiết bị", 1.25, Ui.Grid.LEFT, Ui.Grid.tag()),
            new Ui.Grid.Col("Tên thiết bị", 2.3, Ui.Grid.LEFT, Ui.Grid.text(regular(15f), TEXT)),
            new Ui.Grid.Col("Vị trí", 1.9, Ui.Grid.LEFT, Ui.Grid.text(regular(15f), TEXT)),
            new Ui.Grid.Col("Nhiệt độ", 1.0, Ui.Grid.LEFT, Ui.Grid.colored(semi(15f))),
            new Ui.Grid.Col("Độ ẩm", 1.0, Ui.Grid.LEFT, Ui.Grid.colored(semi(15f))),
            new Ui.Grid.Col("Trạng thái", 1.4, Ui.Grid.LEFT, Ui.Grid.badge()),
            new Ui.Grid.Col("Cập nhật cuối", 1.4, Ui.Grid.LEFT, Ui.Grid.timeAgo()),
            new Ui.Grid.Col("Thao tác", 0.8, Ui.Grid.RIGHT, Ui.Grid.blank()));
    private List<Model.Dev> recentDevs = List.of();

    private Model.Series series = Model.Series.EMPTY;
    private boolean loading;
    private String loadedFor = "";

    DashboardPage(App app) {
        super(app);
        // ---- hàng 1: hero + thiết bị đang xem
        JPanel top = new JPanel(new Ui.Ratio(20, 330, 2, 1));
        top.setOpaque(false);
        top.add(hero());
        top.add(viewing());
        add(top, 20);

        // ---- hàng 2: chip thiết bị
        chips.setOpaque(false);
        chips.setBorder(new EmptyBorder(0, 0, 0, 0));
        chips.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { chips.revalidate(); }
        });
        add(chips, 8);

        // ---- hàng 3: đồng hồ
        JPanel mid = new JPanel(new Ui.Ratio(20, 0, 1, 1, 1));
        mid.setOpaque(false);
        mid.add(gTemp);
        mid.add(gHum);
        mid.add(summary());
        add(mid, 20);

        add(trend(), 20);
        add(recentCard(), 0);

        recent.actionIcons = new Ic[]{Ic.EYE};
        recent.onAction = (row, icon) -> {
            if (row < recentDevs.size()) { app.selectDevice(recentDevs.get(row).id()); app.go("dashboard"); }
        };
        recent.now = app::now;
        recent.rowH = 62;
        recent.emptyTitle = "Chưa có thiết bị nào";
        recent.emptySub = "Thêm thiết bị ở trang Devices hoặc chạy IoT Client để tự đăng ký.";
        cTemp.configure(app::nowMs);
        cHum.configure(app::nowMs);
        applyRange();
    }

    // ------------------------------------------------------------------ dựng giao diện
    private JComponent hero() {
        Ui.Card c = new Ui.Card(new BorderLayout(0, 22)) {
            @Override void paintDecor(Graphics2D g) {
                g.setPaint(new GradientPaint(0, 0, new Color(18, 70, 70, 190), getWidth(), 0, new Color(24, 31, 40, 0)));
                g.fillRect(0, 0, getWidth(), getHeight());
                Charts.planet(g, getWidth() * 0.9, getHeight() * 0.3, getHeight() * 0.2);
            }
        };
        c.pad(30, 34, 28, 34).glow(alpha(TEAL, 34), 0.05f, 0.0f);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(Ui.label("Giám sát cảm biến thời gian thực", bold(31f), TEXT));
        head.add(Ui.gap(0, 8));
        head.add(Ui.label("Theo dõi nhiệt độ, độ ẩm và tình trạng kết nối của các thiết bị IoT.", regular(15f), MUTED));
        c.add(head, BorderLayout.NORTH);
        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(tTotal);
        tiles.add(tOnline);
        tiles.add(tOffline);
        tiles.add(tAlert);
        c.add(tiles, BorderLayout.SOUTH);
        return c;
    }

    private JComponent viewing() {
        Ui.Card c = new Ui.Card() {
            @Override void paintDecor(Graphics2D g) {
                g.setPaint(new GradientPaint(0, 0, new Color(18, 70, 70, 120), getWidth(), getHeight(), new Color(24, 31, 40, 0)));
                g.fillRect(0, 0, getWidth(), getHeight());
                Charts.planet(g, getWidth() * 0.9, getHeight() * 0.12, getHeight() * 0.2);
            }
        };
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.pad(28, 30, 26, 30).glow(alpha(TEAL, 26), 0.1f, 0.0f);
        left(c, Ui.label("Thiết bị đang xem", regular(15.5f), MUTED));
        c.add(Ui.gap(0, 4));
        left(c, vId);
        c.add(Ui.gap(0, 10));
        left(c, vBadge);
        c.add(Box.createVerticalGlue());
        c.add(Ui.gap(0, 14));
        left(c, vTime);
        c.add(Ui.gap(0, 4));
        left(c, vAgo);
        c.add(Ui.gap(0, 16));
        for (JLabel l : new JLabel[]{vName, vLoc, vStamp}) {
            l.setFont(regular(15f));
            l.setForeground(TEXT);
            l.setIconTextGap(12);
            left(c, l);
            c.add(Ui.gap(0, 9));
        }
        return c;
    }

    private static void left(Container c, JComponent comp) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.add(comp);
    }

    private JComponent summary() {
        Ui.Card c = new Ui.Card(new BorderLayout(0, 16)).pad(26, 26, 26, 26);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(Ui.label("Tóm tắt thiết bị", bold(21f), TEXT));
        head.add(Ui.gap(0, 5));
        head.add(sFrame);
        c.add(head, BorderLayout.NORTH);
        JPanel tiles = new JPanel(new GridLayout(3, 1, 0, 12));
        tiles.setOpaque(false);
        tiles.add(sCount);
        tiles.add(sAvgT);
        tiles.add(sAvgH);
        c.add(tiles, BorderLayout.CENTER);
        return c;
    }

    private JComponent trend() {
        Ui.Card c = new Ui.Card(new BorderLayout(0, 12)).pad(26, 28, 22, 28);
        JPanel head = new JPanel(new BorderLayout(16, 0));
        head.setOpaque(false);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(trendTitle);
        t.add(Ui.gap(0, 5));
        t.add(trendSub);
        head.add(t, BorderLayout.CENTER);
        JPanel r = new JPanel(new GridBagLayout());
        r.setOpaque(false);
        r.add(range);
        head.add(r, BorderLayout.EAST);
        c.add(head, BorderLayout.NORTH);
        JPanel charts = new JPanel(new GridLayout(1, 2, 30, 0));
        charts.setOpaque(false);
        charts.add(chartBox("Nhiệt độ (°C)", ORANGE, cTemp));
        charts.add(chartBox("Độ ẩm (%)", CYAN, cHum));
        c.add(charts, BorderLayout.CENTER);
        return c;
    }

    private static JComponent chartBox(String legend, Color color, Charts.Trend chart) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JComponent lg = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(200, 28); }

            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g = Theme.quality(gr);
                g.setColor(color);
                g.fillRoundRect(0, getHeight() / 2 - 2, 20, 4, 4, 4);
                Theme.textMid(g, legend, 30, 0, getHeight(), Theme.regular(14.5f), Theme.TEXT);
                g.dispose();
            }
        };
        p.add(lg, BorderLayout.NORTH);
        p.add(chart, BorderLayout.CENTER);
        return p;
    }

    private JComponent recentCard() {
        Ui.Card c = new Ui.Card(new BorderLayout(0, 6)).pad(26, 22, 20, 22);
        JPanel head = new JPanel(new BorderLayout(16, 0));
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(0, 8, 8, 8));
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(Ui.label("Thiết bị hoạt động gần đây", bold(21f), TEXT));
        t.add(Ui.gap(0, 5));
        t.add(Ui.label("Sắp xếp theo lần nhận dữ liệu cuối", regular(14f), MUTED));
        head.add(t, BorderLayout.CENTER);
        Ui.Btn all = new Ui.Btn("Xem tất cả", null, Ui.Kind.DARK);
        all.addActionListener(e -> app.go("devices"));
        JPanel r = new JPanel(new GridBagLayout());
        r.setOpaque(false);
        r.add(all);
        head.add(r, BorderLayout.EAST);
        c.add(head, BorderLayout.NORTH);
        c.add(recent, BorderLayout.CENTER);
        return c;
    }

    // ------------------------------------------------------------------ dữ liệu
    private void applyRange() {
        long ms = MINUTES[rangeIdx] * 60_000L;
        cTemp.setWindow(ms);
        cHum.setWindow(ms);
        String lb = RANGE_LABELS[rangeIdx];
        Ui.setText(sFrame, "Khung thời gian: " + lb + " gần nhất");
        gTemp.setRange(lb);
        gHum.setRange(lb);
    }

    private void resetSeries() {
        series = Model.Series.EMPTY;
        cTemp.clear();
        cHum.clear();
        applyRange();
        refreshStats();
    }

    @Override void shown() { loadSeries(); }

    @Override void onOverview(Model.Overview o) {
        Model.Summary s = o.summary();
        tTotal.set(String.valueOf(s.total()), "Thiết bị đã đăng ký", s.total() > 0 ? 1.0 : 0, TEAL);
        int pct = s.total() == 0 ? 0 : Math.round(100f * s.online() / s.total());
        tOnline.set(String.valueOf(s.online()), pct + "% tổng thiết bị", s.total() == 0 ? 0 : (double) s.online() / s.total(), TEAL);
        tOffline.set(String.valueOf(s.offline()), "Quá " + o.cfg().timeout() + " giây",
                s.total() == 0 ? 0 : (double) s.offline() / s.total(), new Color(150, 160, 172));
        boolean bad = s.alerting() > 0, near = s.warning() > 0;
        tAlert.setIcon(bad || near ? Ic.ALERT : Ic.CHECK_CIRCLE);
        tAlert.set(String.valueOf(s.alerting()), bad ? "Cần kiểm tra ngay" : near ? s.warning() + " thiết bị sắp vượt ngưỡng" : "Mọi thiết bị ổn định",
                s.total() == 0 ? 0 : (double) (s.alerting() + s.warning()) / s.total(), bad ? RED : near ? AMBER : TEAL);

        updateChips(o);
        Model.Dev d = o.find(app.selectedDevice());
        updateViewing(d, o);
        gTemp.update(d, o.cfg());
        gHum.update(d, o.cfg());
        cTemp.setThreshold(o.cfg().tempTh());
        cHum.setThreshold(o.cfg().humTh());

        List<Model.Dev> sorted = new ArrayList<>(o.devices());
        sorted.sort(Comparator.comparing(Model.Dev::lastSeen, Comparator.nullsLast(Comparator.reverseOrder())));
        recentDevs = sorted;
        List<Object[]> rows = new ArrayList<>();
        for (Model.Dev x : sorted) {
            Color dim = tempColor(x, o.cfg());
            Color dimH = humColor(x, o.cfg());
            rows.add(new Object[]{new Ui.Grid.Tag(dotColor(x), x.id()), x.label(), x.location() == null ? "—" : x.location(),
                    new Ui.Grid.Tag(dim, tempText(x.temp())), new Ui.Grid.Tag(dimH, humText(x.hum())),
                    statusBadge(x), x.lastSeen(), ""});
        }
        recent.setRows(rows);
        loadSeries();
    }

    private void updateChips(Model.Overview o) {
        StringBuilder sig = new StringBuilder();
        for (Model.Dev d : o.devices()) sig.append(d.id()).append(',');
        if (!sig.toString().equals(chipSig)) {
            chipSig = sig.toString();
            chips.removeAll();
            for (Model.Dev d : o.devices()) chips.add(new DevChip(d.id()));
            chips.revalidate();
            chips.repaint();
        }
        for (Component c : chips.getComponents()) if (c instanceof DevChip dc) dc.set(o.find(dc.id), dc.id.equals(app.selectedDevice()));
    }

    private void updateViewing(Model.Dev d, Model.Overview o) {
        if (d == null) {
            Ui.setText(vId, "Chưa chọn");
            Ui.setText(vTime, "--:--:--");
            Ui.setText(vAgo, "Chưa có thiết bị");
            Ui.setText(vName, "—");
            Ui.setText(vLoc, "—");
            Ui.setText(vStamp, "—");
            vBadge.set(Ui.offline());
            trendTitle.setText("Biểu đồ xu hướng");
            return;
        }
        Ui.setText(vId, d.id());
        Ui.Badge b = !d.online() ? new Ui.Badge("Dữ liệu đã cũ", Ic.WIFI_OFF, MUTED, new Color(255, 255, 255, 20))
                : d.critical() ? Ui.critical() : d.warning() ? Ui.warn()
                : new Ui.Badge("Đang trực tuyến", Ic.WIFI, GREEN, alpha(GREEN, 36));
        vBadge.set(b);
        Ui.setText(vName, d.label());
        Ui.setText(vLoc, d.location() == null ? "—" : d.location());
        Ui.setText(vStamp, Model.dt(d.lastSeen()));
        Ui.setText(trendTitle, "Biểu đồ xu hướng – " + d.id());
        tickTimes();
    }

    private void tickTimes() {
        Model.Dev d = app.overview() == null ? null : app.overview().find(app.selectedDevice());
        if (d == null) return;
        Ui.setText(vTime, Model.hms(d.lastSeen()));
        Ui.setText(vAgo, d.lastSeen() == null ? "Chưa nhận dữ liệu" : "Nhận lần cuối · " + Model.ago(d.lastSeen(), app.now()).toLowerCase());
    }

    @Override void second() {
        tickTimes();
        recent.repaint();
        for (Component c : chips.getComponents()) c.repaint();
    }

    @Override void frame() {
        gTemp.gauge.step();
        gHum.gauge.step();
        cTemp.frame();
        cHum.frame();
    }

    private void loadSeries() {
        String id = app.selectedDevice();
        if (id == null) return;
        if (!id.equals(loadedFor)) {
            loadedFor = id;
            resetSeries();
        }
        if (loading) return;
        loading = true;
        String from = app.now().minusMinutes(MINUTES[rangeIdx]).minusSeconds(10).format(Model.ISO);
        app.api().get("/api/sensor-data/" + Api.enc(id) + "/history?from=" + Api.enc(from) + "&limit=4000", j -> {
            loading = false;
            if (!id.equals(app.selectedDevice())) return;
            series = Model.series(j);
            cTemp.setSeries(series);
            cHum.setSeries(series);
            refreshStats();
        }, e -> loading = false);
    }

    private void refreshStats() {
        long start = app.nowMs() - MINUTES[rangeIdx] * 60_000L;
        int count = 0;
        double[] te = {Double.MAX_VALUE, -Double.MAX_VALUE, 0}, hu = {Double.MAX_VALUE, -Double.MAX_VALUE, 0};
        for (int i = 0; i < series.size(); i++) {
            if (series.t[i] < start) continue;
            count++;
            te[0] = Math.min(te[0], series.temp[i]); te[1] = Math.max(te[1], series.temp[i]); te[2] += series.temp[i];
            hu[0] = Math.min(hu[0], series.hum[i]); hu[1] = Math.max(hu[1], series.hum[i]); hu[2] += series.hum[i];
        }
        Ui.setText(trendSub, "Cập nhật tự động · hiển thị " + count + " bản ghi");
        sCount.set(String.valueOf(count));
        if (count == 0) {
            sAvgT.set("-- °C");
            sAvgH.set("-- %");
            gTemp.setStats(null);
            gHum.setStats(null);
        } else {
            sAvgT.set(Model.num1(te[2] / count) + " °C");
            sAvgH.set(Model.num1(hu[2] / count) + " %");
            gTemp.setStats(new double[]{te[0], te[1]});
            gHum.setStats(new double[]{hu[0], hu[1]});
        }
    }

    // ------------------------------------------------------------------ thành phần con
    private static final class StatTile extends JComponent {
        private Ic icon;
        private final String label;
        private String value = "—", caption = "";
        private double frac;
        private Color accent;

        StatTile(Ic icon, String label, Color accent) {
            this.icon = icon; this.label = label; this.accent = accent;
            setPreferredSize(new Dimension(150, 158));
        }

        void setIcon(Ic i) { if (icon != i) { icon = i; repaint(); } }

        void set(String v, String cap, double f, Color a) {
            if (!v.equals(value) || !cap.equals(caption) || f != frac || !a.equals(accent)) {
                value = v; caption = cap; frac = f; accent = a; repaint();
            }
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            Shape s = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 36, 36);
            g.setColor(new Color(22, 29, 37, 215));
            g.fill(s);
            g.setColor(Theme.LINE);
            g.draw(s);
            g.setColor(alpha(accent, 38));
            g.fillRoundRect(16, 16, 36, 36, 14, 14);
            icon.paint(g, 16 + 9, 16 + 9, 18, accent);
            Theme.text(g, Theme.fit(g.getFontMetrics(Theme.regular(14f)), label, w - 28), 16, 78, Theme.regular(14f), Theme.MUTED);
            Theme.text(g, value, 16, 116, Theme.bold(38f), Theme.TEXT);
            Theme.text(g, Theme.fit(g.getFontMetrics(Theme.regular(12.5f)), caption, w - 26), 16, 138, Theme.regular(12.5f), Theme.MUTED);
            int bw = w - 32;
            g.setColor(new Color(255, 255, 255, 18));
            g.fillRoundRect(16, h - 10, bw, 4, 4, 4);
            g.setColor(alpha(accent, 190));
            g.fillRoundRect(16, h - 10, (int) Math.max(frac > 0 ? 8 : 0, bw * frac), 4, 4, 4);
            g.dispose();
        }
    }

    private final class DevChip extends JComponent {
        final String id;
        private Model.Dev dev;
        private boolean sel, hover;

        DevChip(String id) {
            this.id = id;
            setPreferredSize(new Dimension(214, 84));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    if (!id.equals(app.selectedDevice())) { app.selectDevice(id); }
                }
            });
        }

        void set(Model.Dev d, boolean selected) {
            if (!Objects.equals(d, dev) || sel != selected) { dev = d; sel = selected; repaint(); }
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            Shape s = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 36, 36);
            g.setColor(sel ? new Color(25, 51, 56) : hover ? new Color(36, 44, 54) : new Color(30, 37, 46));
            g.fill(s);
            g.setColor(sel ? alpha(TEAL, 170) : Theme.LINE);
            g.setStroke(new BasicStroke(sel ? 1.5f : 1f));
            g.draw(s);
            Theme.text(g, id, 18, 29, Theme.bold(16f), Theme.TEXT);
            if (dev != null) {
                g.setColor(dotColor(dev));
                g.fill(new Ellipse2D.Double(w - 28, 18, 10, 10));
                String nm = Theme.fit(g.getFontMetrics(Theme.regular(14f)), dev.label(), w - 36);
                Theme.text(g, nm, 18, 51, Theme.regular(14f), Theme.MUTED);
                int x = 18;
                Model.Cfg cf = app.overview() == null ? Model.Cfg.DEFAULT : app.overview().cfg();
                x += Theme.text(g, tempText(dev.temp()), x, 73, Theme.semi(14.5f), tempColor(dev, cf)) + 14;
                Theme.text(g, humText(dev.hum()), x, 73, Theme.semi(14.5f), humColor(dev, cf));
            }
            g.dispose();
        }
    }

    private final class GaugeCard extends Ui.Card {
        final boolean temp;
        final Charts.Gauge gauge;
        final Ui.BadgeLabel chip = new Ui.BadgeLabel(Ui.offline(), 32);
        private final Footer footer;
        private String rangeLabel = "5 phút";
        private double[] stats;

        GaugeCard(boolean temp) {
            super(new BorderLayout(0, 0));
            this.temp = temp;
            gauge = temp ? new Charts.Gauge(0, 60, ORANGE, new Color(251, 191, 36), "°C")
                    : new Charts.Gauge(0, 100, new Color(34, 197, 214), new Color(45, 212, 191), "%");
            footer = new Footer();
            pad(22, 24, 18, 24);
            glow(alpha(temp ? ORANGE : CYAN, 26), 0.12f, 0.0f);
            JPanel head = new JPanel(new BorderLayout(12, 0));
            head.setOpaque(false);
            JPanel l = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
            l.setOpaque(false);
            l.add(new JComponent() {
                { setPreferredSize(new Dimension(44, 44)); }

                @Override protected void paintComponent(Graphics gr) {
                    Graphics2D g = Theme.quality(gr);
                    Color a = temp ? ORANGE : CYAN;
                    g.setColor(alpha(a, 36));
                    g.fillRoundRect(0, 0, 44, 44, 16, 16);
                    (temp ? Ic.THERMO : Ic.DROP).paint(g, 11, 11, 22, a);
                    g.dispose();
                }
            });
            JLabel t = Ui.label(temp ? "Nhiệt độ" : "Độ ẩm", bold(20f), TEXT);
            t.setBorder(new EmptyBorder(9, 0, 0, 0));
            l.add(t);
            head.add(l, BorderLayout.WEST);
            JPanel r = new JPanel(new GridBagLayout());
            r.setOpaque(false);
            r.add(chip);
            head.add(r, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);
            add(gauge, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);
        }

        void setRange(String lb) { rangeLabel = lb; footer.repaint(); }

        void setStats(double[] s) { stats = s; footer.repaint(); }

        void update(Model.Dev d, Model.Cfg cfg) {
            double th = temp ? cfg.tempTh() : cfg.humTh();
            double mg = temp ? cfg.tempMargin() : cfg.humMargin();
            gauge.setLimits(th, mg);
            footer.threshold = "> " + (th == Math.rint(th) ? String.valueOf((long) th) : Model.num1(th)) + (temp ? " °C" : " %");
            footer.repaint();
            if (d == null) { gauge.setValue(null); chip.set(Ui.offline()); return; }
            Double v = temp ? d.temp() : d.hum();
            if (temp) gauge.setMax(Math.max(60, Math.ceil(((v == null ? 0 : v) > th ? Math.max(v, th) : th) / 10 + 0.5) * 10));
            gauge.setValue(v);
            // trạng thái RIÊNG của chỉ số này (không dùng chung với chỉ số còn lại)
            chip.set(metricBadge(d, temp ? d.tempLevel(cfg) : d.humLevel(cfg)));
        }

        private final class Footer extends JComponent {
            String threshold = "";

            Footer() { setPreferredSize(new Dimension(100, 116)); }

            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g = Theme.quality(gr);
                int w = getWidth();
                String a = "Ngưỡng cảnh báo:", unit = temp ? " °C" : " %";
                Font f1 = Theme.regular(14.5f), f2 = Theme.semi(14.5f);
                FontMetrics m1 = g.getFontMetrics(f1), m2 = g.getFontMetrics(f2);
                int tw = m1.stringWidth(a) + 10 + m2.stringWidth(threshold);
                int x = (w - tw) / 2;
                Theme.text(g, a, x, 18, f1, MUTED);
                Theme.text(g, threshold, x + m1.stringWidth(a) + 10, 18, f2, TEXT);
                g.setColor(new Color(255, 255, 255, 18));
                g.fillRect(0, 36, w, 1);
                String lo = "--", hi = "--";
                if (stats != null) { lo = Model.num1(stats[0]) + unit; hi = Model.num1(stats[1]) + unit; }
                Theme.text(g, "Thấp nhất (" + rangeLabel + ")", 0, 64, Theme.regular(14f), MUTED);
                Theme.text(g, lo, 0, 94, Theme.bold(19f), TEXT);
                Theme.text(g, "Cao nhất (" + rangeLabel + ")", w / 2, 64, Theme.regular(14f), MUTED);
                Theme.text(g, hi, w / 2, 94, Theme.bold(19f), TEXT);
                g.dispose();
            }
        }
    }

    private static final class SumTile extends JComponent {
        private final String label;
        private String value = "0";
        private final Color color;

        SumTile(String label, Color color) {
            this.label = label; this.color = color;
            setPreferredSize(new Dimension(100, 96));
        }

        void set(String v) { if (!v.equals(value)) { value = v; repaint(); } }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(31, 38, 48));
            g.fillRoundRect(0, 0, w, h, 28, 28);
            g.setColor(Theme.LINE);
            g.drawRoundRect(0, 0, w - 1, h - 1, 28, 28);
            Theme.text(g, label, 22, h / 2 - 8, Theme.regular(14.5f), Theme.MUTED);
            Font f = Theme.bold(34f);
            String[] parts = value.split(" ", 2);
            int x = 22;
            x += Theme.text(g, parts[0], x, h / 2 + 32, f, color == TEXT ? Theme.TEXT : color);
            if (parts.length > 1) Theme.text(g, " " + parts[1], x, h / 2 + 32, Theme.bold(26f), color == TEXT ? Theme.TEXT : color);
            g.dispose();
        }
    }
}
