package com.pulseiot.desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

import static com.pulseiot.desktop.Theme.*;

final class HistoryPage extends Page {
    private static final String[] RANGES = {"15 phút gần nhất", "1 giờ gần nhất", "6 giờ gần nhất", "24 giờ gần nhất", "7 ngày gần nhất", "Tất cả"};
    private static final int[] RANGE_MIN = {15, 60, 360, 1440, 10080, 0};

    private final Ui.Combo device = new Ui.Combo("Tất cả thiết bị");
    private final Ui.Combo range = new Ui.Combo(RANGES);
    private final Ui.Combo pageSize = new Ui.Combo("20 dòng/trang", "50 dòng/trang", "100 dòng/trang");
    private final Ui.Grid grid = new Ui.Grid(
            new Ui.Grid.Col("Mã bản ghi", 1.2, Ui.Grid.LEFT, Ui.Grid.text(regular(15f), MUTED)),
            new Ui.Grid.Col("Mã thiết bị", 1.4, Ui.Grid.LEFT, Ui.Grid.text(bold(15f), TEXT)),
            new Ui.Grid.Col("Nhiệt độ (°C)", 1.6, Ui.Grid.LEFT, Ui.Grid.text(semi(15f), ORANGE)),
            new Ui.Grid.Col("Độ ẩm (%)", 1.6, Ui.Grid.LEFT, Ui.Grid.text(semi(15f), CYAN)),
            new Ui.Grid.Col("Thời gian ghi nhận", 2.4, Ui.Grid.LEFT, Ui.Grid.text(regular(15f), TEXT)));
    private final JLabel info = Ui.label("", regular(14.5f), MUTED);
    private final JLabel pageLbl = Ui.label("Trang 1/1", regular(14.5f), MUTED);
    private final Ui.Btn prev = new Ui.Btn("", Ic.CHEVRON_LEFT, Ui.Kind.OUTLINE).size(44, 13);
    private final Ui.Btn next = new Ui.Btn("", Ic.CHEVRON_RIGHT, Ui.Kind.OUTLINE).size(44, 13);
    private int page, pages = 1;
    private long total;
    private boolean ignore, loading;
    private int secs;
    private String deviceSig = "";

    HistoryPage(App app) {
        super(app);
        Ui.Btn refresh = new Ui.Btn("Làm mới", Ic.REFRESH, Ui.Kind.DARK);
        Ui.Btn export = new Ui.Btn("Xuất CSV", Ic.DOWNLOAD, Ui.Kind.LIGHT);
        refresh.addActionListener(e -> load(false));
        export.addActionListener(e -> exportCsv());
        add(header("Lịch sử dữ liệu cảm biến", "Tra cứu các bản ghi đo đã lưu theo thiết bị và khoảng thời gian.", refresh, export), 18);

        Ui.Card card = new Ui.Card(new BorderLayout(0, 14)).pad(26, 22, 20, 22);
        JPanel filters = new JPanel(new GridLayout(1, 2, 20, 0));
        filters.setOpaque(false);
        filters.setBorder(new EmptyBorder(0, 8, 6, 8));
        filters.add(labeled("Thiết bị", device));
        filters.add(labeled("Khoảng thời gian", range));
        range.setSelectedIndex(1);
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

        grid.rowH = 54;
        grid.emptyTitle = "Chưa có bản ghi nào";
        grid.emptySub = "Hãy chạy IoT Client hoặc chọn khoảng thời gian dài hơn.";
        pageLbl.setBorder(new EmptyBorder(0, 6, 0, 6));
        prev.addActionListener(e -> { if (page > 0) { page--; load(false); } });
        next.addActionListener(e -> { if (page + 1 < pages) { page++; load(false); } });
        java.awt.event.ActionListener changed = e -> { if (!ignore) { page = 0; load(false); } };
        device.addActionListener(changed);
        range.addActionListener(changed);
        pageSize.addActionListener(changed);
    }

    @Override void shown() { page = 0; syncDevices(); load(false); }

    @Override void onOverview(Model.Overview o) { syncDevices(); }

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

    private String filters() {
        String id = device.getSelectedIndex() <= 0 ? "" : String.valueOf(device.getSelectedItem());
        StringBuilder sb = new StringBuilder();
        if (!id.isEmpty()) sb.append("&deviceId=").append(Api.enc(id));
        int m = RANGE_MIN[Math.max(0, range.getSelectedIndex())];
        if (m > 0) sb.append("&from=").append(Api.enc(app.now().minusMinutes(m).format(Model.ISO)));
        return sb.toString();
    }

    void load(boolean silent) {
        if (loading) return;
        loading = true;
        int req = page;
        app.api().get("/api/sensor-data/search?page=" + page + "&size=" + size() + filters(), j -> {
            loading = false;
            if (req != page) return;
            total = j.path("totalElements").lng(0);
            pages = Math.max(1, j.path("totalPages").intv(1));
            List<Object[]> rows = new ArrayList<>();
            for (Jn r : j.list("content")) {
                Model.HistoryRow h = Model.history(r);
                rows.add(new Object[]{"#" + h.id(), h.deviceId(), Model.num2(h.temp()), Model.num2(h.hum()), Model.dt(h.at())});
            }
            grid.setRows(rows);
            int from = total == 0 ? 0 : page * size() + 1;
            Ui.setText(info, total == 0 ? "Không có bản ghi" : "Hiển thị " + from + "–" + (from + rows.size() - 1) + " / " + total + " bản ghi");
            Ui.setText(pageLbl, "Trang " + (page + 1) + "/" + pages);
            prev.setEnabled(page > 0);
            next.setEnabled(page + 1 < pages);
        }, err -> { loading = false; if (!silent) app.toast("Không tải được lịch sử", err, Ui.ToastKind.ERROR); });
    }

    private void exportCsv() {
        app.api().get("/api/sensor-data?limit=5000" + filters(), j -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("pulse-iot-lich-su.csv"));
            if (fc.showSaveDialog(SwingUtilities.getWindowAncestor(body)) != JFileChooser.APPROVE_OPTION) return;
            try {
                StringBuilder sb = new StringBuilder("\uFEFFMã bản ghi,Mã thiết bị,Nhiệt độ (°C),Độ ẩm (%),Thời gian ghi nhận\n");
                for (Jn r : j.list()) {
                    Model.HistoryRow h = Model.history(r);
                    sb.append(h.id()).append(',').append(h.deviceId()).append(',').append(Model.num2(h.temp())).append(',')
                            .append(Model.num2(h.hum())).append(',').append(Model.dt(h.at())).append('\n');
                }
                Files.writeString(fc.getSelectedFile().toPath(), sb.toString(), StandardCharsets.UTF_8);
                app.toast("Đã xuất CSV", fc.getSelectedFile().getName(), Ui.ToastKind.SUCCESS);
            } catch (Exception ex) {
                app.toast("Không ghi được file", String.valueOf(ex.getMessage()), Ui.ToastKind.ERROR);
            }
        }, err -> app.toast("Không xuất được CSV", err, Ui.ToastKind.ERROR));
    }
}
