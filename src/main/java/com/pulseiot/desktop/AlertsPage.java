package com.pulseiot.desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.pulseiot.desktop.Theme.*;

final class AlertsPage extends Page {
    private final Ui.Combo device = new Ui.Combo("Tất cả thiết bị");
    private final Ui.Combo metric = new Ui.Combo("Nhiệt độ và độ ẩm", "Nhiệt độ", "Độ ẩm");
    private final Ui.Combo pageSize = new Ui.Combo("20 dòng/trang", "50 dòng/trang", "100 dòng/trang");
    private final StatCard cTotal = new StatCard("Đã ghi nhận (theo bộ lọc)", TEXT);
    private final StatCard cBad = new StatCard("Đang vượt ngưỡng", RED);
    private final StatCard cNear = new StatCard("Sắp vượt ngưỡng", AMBER);
    private final Ui.Grid grid = new Ui.Grid(
            new Ui.Grid.Col("Thời gian", 2.1, Ui.Grid.LEFT, Ui.Grid.text(regular(15f), TEXT)),
            new Ui.Grid.Col("Thiết bị", 2.6, Ui.Grid.LEFT, Ui.Grid.two()),
            new Ui.Grid.Col("Chỉ số", 1.3, Ui.Grid.LEFT, Ui.Grid.text(semi(15f), TEXT)),
            new Ui.Grid.Col("Giá trị đo", 1.3, Ui.Grid.LEFT, Ui.Grid.colored(bold(15f))),
            new Ui.Grid.Col("Ngưỡng", 1.3, Ui.Grid.LEFT, Ui.Grid.text(regular(15f), MUTED)),
            new Ui.Grid.Col("Mức độ", 1.8, Ui.Grid.LEFT, Ui.Grid.badge()),
            new Ui.Grid.Col("Nguồn phát hiện", 2.0, Ui.Grid.LEFT, Ui.Grid.badge()));
    private final JLabel info = Ui.label("", regular(14.5f), MUTED);
    private final JLabel pageLbl = Ui.label("Trang 1/1", regular(14.5f), MUTED);
    private final Ui.Btn prev = new Ui.Btn("", Ic.CHEVRON_LEFT, Ui.Kind.OUTLINE).size(44, 13);
    private final Ui.Btn next = new Ui.Btn("", Ic.CHEVRON_RIGHT, Ui.Kind.OUTLINE).size(44, 13);
    private List<Model.AlertRow> rowsData = List.of();
    private int page, pages = 1, secs;
    private boolean ignore, loading;

    AlertsPage(App app) {
        super(app);
        Ui.Btn refresh = new Ui.Btn("Làm mới", Ic.REFRESH, Ui.Kind.DARK);
        refresh.addActionListener(e -> load(false));
        add(header("Nhật ký cảnh báo", "Các lần thiết bị vượt ngưỡng do Server phát hiện và lưu lại.", refresh), 18);

        JPanel stats = new JPanel(new GridLayout(1, 3, 20, 0));
        stats.setOpaque(false);
        stats.add(cTotal);
        stats.add(cBad);
        stats.add(cNear);
        add(stats, 20);

        Ui.Card card = new Ui.Card(new BorderLayout(0, 14)).pad(26, 22, 20, 22);
        JPanel filters = new JPanel(new GridLayout(1, 2, 20, 0));
        filters.setOpaque(false);
        filters.setBorder(new EmptyBorder(0, 8, 6, 8));
        filters.add(labeled("Thiết bị", device));
        filters.add(labeled("Chỉ số", metric));
        card.add(filters, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        JPanel foot = new JPanel(new BorderLayout(14, 0));
        foot.setOpaque(false);
        foot.setBorder(new EmptyBorder(8, 8, 0, 8));
        foot.add(info, BorderLayout.WEST);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        r.setOpaque(false);
        pageSize.setPreferredSize(new Dimension(190, 46));
        r.add(pageSize);
        r.add(prev);
        r.add(pageLbl);
        r.add(next);
        foot.add(r, BorderLayout.EAST);
        card.add(foot, BorderLayout.SOUTH);
        add(card, 0);

        grid.rowH = 66;
        grid.emptyTitle = "Chưa có cảnh báo nào";
        grid.emptySub = "Khi một chỉ số vượt ngưỡng, Server sẽ ghi lại tại đây.";
        grid.style = i -> i < rowsData.size() ? ("CRITICAL".equals(rowsData.get(i).level())
                ? new Ui.Grid.RowStyle(alpha(RED, 20), alpha(RED, 200)) : new Ui.Grid.RowStyle(alpha(AMBER, 16), alpha(AMBER, 200))) : null;
        pageLbl.setBorder(new EmptyBorder(0, 6, 0, 6));
        prev.addActionListener(e -> { if (page > 0) { page--; load(false); } });
        next.addActionListener(e -> { if (page + 1 < pages) { page++; load(false); } });
        java.awt.event.ActionListener changed = e -> { if (!ignore) { page = 0; load(false); } };
        device.addActionListener(changed);
        metric.addActionListener(changed);
        pageSize.addActionListener(changed);
    }

    @Override void shown() { page = 0; syncDevices(); load(false); updateStats(); }

    @Override void onOverview(Model.Overview o) { syncDevices(); updateStats(); }

    private void updateStats() {
        Model.Overview o = app.overview();
        if (o == null) return;
        cBad.set(String.valueOf(o.summary().alerting()));
        cNear.set(String.valueOf(o.summary().warning()));
    }

    private void syncDevices() {
        Model.Overview o = app.overview();
        if (o == null) return;
        List<String> items = new ArrayList<>();
        items.add("Tất cả thiết bị");
        for (Model.Dev d : o.devices()) items.add(d.id());
        if (!device.sameItems(items)) {
            ignore = true;
            device.setItems(items);
            ignore = false;
        }
    }

    @Override void second() {
        if (++secs >= 5) { secs = 0; if (page == 0) load(true); }
    }

    private int size() { return pageSize.getSelectedIndex() == 1 ? 50 : pageSize.getSelectedIndex() == 2 ? 100 : 20; }

    void load(boolean silent) {
        if (loading) return;
        loading = true;
        String id = device.getSelectedIndex() <= 0 ? "" : String.valueOf(device.getSelectedItem());
        String m = metric.getSelectedIndex() == 1 ? "TEMPERATURE" : metric.getSelectedIndex() == 2 ? "HUMIDITY" : "";
        int req = page;
        String q = "/api/alerts?page=" + page + "&size=" + size() + (id.isEmpty() ? "" : "&deviceId=" + Api.enc(id)) + (m.isEmpty() ? "" : "&metric=" + m);
        app.api().get(q, j -> {
            loading = false;
            if (req != page) return;
            long total = j.path("totalElements").lng(0);
            pages = Math.max(1, j.path("totalPages").intv(1));
            List<Model.AlertRow> list = new ArrayList<>();
            List<Object[]> rows = new ArrayList<>();
            for (Jn a : j.list("content")) {
                Model.AlertRow r = Model.alert(a);
                list.add(r);
                boolean crit = "CRITICAL".equals(r.level());
                String u = " " + Model.unit(r.metric());
                rows.add(new Object[]{Model.dt(r.at()), new Ui.Grid.Two(r.deviceId(), r.deviceName() == null ? "" : r.deviceName()),
                        Model.metricName(r.metric()), new Ui.Grid.Tag(crit ? RED : AMBER, Model.num1(r.value()) + u),
                        "> " + Model.num1(r.threshold()) + u, crit ? Ui.critical() : Ui.near(),
                        new Ui.Badge("Backend · MySQL", null, new Color(86, 214, 236), alpha(CYAN, 34))});
            }
            rowsData = list;
            grid.setRows(rows);
            cTotal.set(String.valueOf(total));
            int from = total == 0 ? 0 : page * size() + 1;
            Ui.setText(info, total == 0 ? "Không có bản ghi" : "Hiển thị " + from + "–" + (from + rows.size() - 1) + " / " + total + " bản ghi");
            Ui.setText(pageLbl, "Trang " + (page + 1) + "/" + pages);
            prev.setEnabled(page > 0);
            next.setEnabled(page + 1 < pages);
        }, err -> { loading = false; if (!silent) app.toast("Không tải được nhật ký cảnh báo", err, Ui.ToastKind.ERROR); });
    }

    private static final class StatCard extends Ui.Card {
        private final String label;
        private String value = "0";
        private final Color color;

        StatCard(String label, Color color) {
            this.label = label; this.color = color;
            setPreferredSize(new Dimension(200, 124));
        }

        void set(String v) { if (!v.equals(value)) { value = v; repaint(); } }

        @Override protected void paintComponent(Graphics gr) {
            super.paintComponent(gr);
            Graphics2D g = Theme.quality(gr);
            Theme.text(g, label, 28, 44, Theme.regular(15f), Theme.MUTED);
            Theme.text(g, value, 28, 96, Theme.bold(44f), color);
            g.dispose();
        }
    }
}
