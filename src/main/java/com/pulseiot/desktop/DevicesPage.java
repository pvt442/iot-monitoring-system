package com.pulseiot.desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.pulseiot.desktop.Theme.*;

final class DevicesPage extends Page {
    private final Ui.Segmented tabs = new Ui.Segmented(new String[]{"Tất cả", "Online (0)", "Offline (0)", "Cảnh báo (0)"}, 0, i -> rebuild());
    private final Ui.Field search = new Ui.Field("Tìm theo mã hoặc tên...", Ic.SEARCH, true);
    private final Ui.Combo sort = new Ui.Combo("Cập nhật gần nhất", "Mã thiết bị", "Nhiệt độ cao nhất", "Độ ẩm cao nhất");
    private final Ui.Grid grid = new Ui.Grid(
            new Ui.Grid.Col("Mã thiết bị", 1.25, Ui.Grid.LEFT, Ui.Grid.tag()),
            new Ui.Grid.Col("Tên thiết bị", 2.3, Ui.Grid.LEFT, Ui.Grid.text(regular(15f), TEXT)),
            new Ui.Grid.Col("Vị trí", 1.9, Ui.Grid.LEFT, Ui.Grid.text(regular(15f), TEXT)),
            new Ui.Grid.Col("Nhiệt độ", 1.0, Ui.Grid.LEFT, Ui.Grid.colored(semi(15f))),
            new Ui.Grid.Col("Độ ẩm", 1.0, Ui.Grid.LEFT, Ui.Grid.colored(semi(15f))),
            new Ui.Grid.Col("Trạng thái", 1.4, Ui.Grid.LEFT, Ui.Grid.badge()),
            new Ui.Grid.Col("Cập nhật cuối", 1.4, Ui.Grid.LEFT, Ui.Grid.timeAgo()),
            new Ui.Grid.Col("Thao tác", 1.0, Ui.Grid.RIGHT, Ui.Grid.blank()));
    private List<Model.Dev> shownDevs = List.of();
    private boolean syncing;

    DevicesPage(App app) {
        super(app);
        Ui.Btn add = new Ui.Btn("Thêm thiết bị", Ic.PLUS, Ui.Kind.LIGHT);
        add.addActionListener(e -> addDialog());
        add(header("Quản lý thiết bị", "Danh sách thiết bị IoT, trạng thái kết nối và số đo mới nhất.", add), 18);

        Ui.Card card = new Ui.Card(new BorderLayout(0, 10)).pad(24, 22, 18, 22);
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 6, 8, 6));
        JPanel l = new JPanel(new GridBagLayout());
        l.setOpaque(false);
        l.add(tabs);
        bar.add(l, BorderLayout.WEST);
        JPanel r = new JPanel(new BorderLayout(14, 0));
        r.setOpaque(false);
        search.setPreferredSize(new Dimension(200, 50));
        sort.setPreferredSize(new Dimension(210, 50));
        r.add(search, BorderLayout.CENTER);
        r.add(sort, BorderLayout.EAST);
        bar.add(r, BorderLayout.CENTER);
        card.add(bar, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        add(card, 0);

        grid.actionIcons = new Ic[]{Ic.EYE, Ic.TRASH};
        grid.rowH = 66;
        grid.now = app::now;
        grid.emptyTitle = "Không có thiết bị phù hợp";
        grid.emptySub = "Thử đổi bộ lọc hoặc thêm thiết bị mới.";
        grid.onAction = (row, icon) -> {
            if (row >= shownDevs.size()) return;
            Model.Dev d = shownDevs.get(row);
            if (icon == 0) { app.selectDevice(d.id()); app.go("dashboard"); }
            else app.confirm("Xóa thiết bị " + d.id() + "?", "Thiết bị và toàn bộ dữ liệu cảm biến, cảnh báo liên quan sẽ bị xóa vĩnh viễn.",
                    "Xóa thiết bị", true, () -> delete(d.id()));
        };
        search.getDocument().addDocumentListener(new DocumentListener() {
            void ch() { if (!syncing) { app.pageSearch(search.getText()); rebuild(); } }
            public void insertUpdate(DocumentEvent e) { ch(); }
            public void removeUpdate(DocumentEvent e) { ch(); }
            public void changedUpdate(DocumentEvent e) { ch(); }
        });
        sort.addActionListener(e -> rebuild());
    }

    void setQuery(String q) {
        if (!search.getText().equals(q)) {
            syncing = true;
            search.setText(q);
            syncing = false;
            rebuild();
        }
    }

    @Override void shown() { rebuild(); }

    @Override void onOverview(Model.Overview o) { rebuild(); }

    @Override void second() { grid.repaint(); }

    private void rebuild() {
        Model.Overview o = app.overview();
        if (o == null) return;
        int on = 0, off = 0, warn = 0;
        for (Model.Dev d : o.devices()) {
            if (d.online()) on++; else off++;
            if (d.critical() || d.warning()) warn++;
        }
        tabs.setLabel(1, "Online (" + on + ")");
        tabs.setLabel(2, "Offline (" + off + ")");
        tabs.setLabel(3, "Cảnh báo (" + warn + ")");
        String q = search.getText().trim().toLowerCase();
        List<Model.Dev> list = new ArrayList<>();
        for (Model.Dev d : o.devices()) {
            boolean ok = switch (tabs.selected()) {
                case 1 -> d.online();
                case 2 -> !d.online();
                case 3 -> d.critical() || d.warning();
                default -> true;
            };
            if (!ok) continue;
            if (!q.isEmpty() && !(d.id().toLowerCase().contains(q) || d.label().toLowerCase().contains(q)
                    || (d.location() != null && d.location().toLowerCase().contains(q)))) continue;
            list.add(d);
        }
        Comparator<Model.Dev> cmp = switch (sort.getSelectedIndex()) {
            case 1 -> Comparator.comparing(Model.Dev::id);
            case 2 -> Comparator.comparing((Model.Dev d) -> d.temp() == null ? -1e9 : d.temp()).reversed();
            case 3 -> Comparator.comparing((Model.Dev d) -> d.hum() == null ? -1e9 : d.hum()).reversed();
            default -> Comparator.comparing(Model.Dev::lastSeen, Comparator.nullsLast(Comparator.reverseOrder()));
        };
        list.sort(cmp);
        shownDevs = list;
        List<Object[]> rows = new ArrayList<>();
        for (Model.Dev d : list) {
            Color t = tempColor(d, o.cfg()), h = humColor(d, o.cfg());
            rows.add(new Object[]{new Ui.Grid.Tag(dotColor(d), d.id()), d.label(), d.location() == null ? "—" : d.location(),
                    new Ui.Grid.Tag(t, tempText(d.temp())), new Ui.Grid.Tag(h, humText(d.hum())), statusBadge(d), d.lastSeen(), ""});
        }
        grid.setRows(rows);
    }

    private void delete(String id) {
        app.api().send("DELETE", "/api/devices/" + Api.enc(id), null, j -> {
            app.deviceDeleted(id);
            app.toast("Đã xóa thiết bị " + id,
                    "Thiết bị cùng toàn bộ số đo và nhật ký cảnh báo của nó đã bị xóa khỏi MySQL và biến mất khỏi mọi màn hình. IoT Client sẽ ngừng gửi dữ liệu cho thiết bị này.",
                    Ui.ToastKind.SUCCESS);
        }, err -> {
            if (err.contains("Không tìm thấy")) {
                app.deviceDeleted(id);
                app.toast("Thiết bị " + id + " đã không còn trên Server", "Danh sách đã được cập nhật lại.", Ui.ToastKind.INFO);
            } else {
                app.toast("Không xóa được thiết bị " + id, err, Ui.ToastKind.ERROR);
            }
        });
    }

    private void addDialog() {
        Ui.Field id = new Ui.Field("Ví dụ: DEV_04", null), name = new Ui.Field("Ví dụ: Cảm biến phòng họp", null), loc = new Ui.Field("Ví dụ: Tầng 2", null);
        Model.Overview o = app.overview();
        int n = o == null ? 1 : o.devices().size() + 1;
        String suggest = String.format("DEV_%02d", n);
        while (o != null && o.find(suggest) != null) suggest = String.format("DEV_%02d", ++n);
        id.setText(suggest);
        id.selectAll();
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 14));
        form.setOpaque(false);
        form.add(labeled("Mã thiết bị *", id));
        form.add(labeled("Tên thiết bị", name));
        form.add(labeled("Vị trí", loc));
        Ui.Btn cancel = new Ui.Btn("Hủy", null, Ui.Kind.OUTLINE);
        Ui.Btn ok = new Ui.Btn("Thêm thiết bị", Ic.PLUS, Ui.Kind.LIGHT);
        Runnable submit = () -> {
            String code = id.getText().trim();
            if (!code.matches("^[A-Za-z0-9_.-]{1,50}$")) {
                id.invalid = true;
                id.repaint();
                app.toast("Mã thiết bị không hợp lệ", "Chỉ dùng chữ, số và các ký tự _ - . (tối đa 50 ký tự), ví dụ DEV_04.", Ui.ToastKind.WARN);
                return;
            }
            Model.Overview cur = app.overview();
            if (cur != null && cur.find(code) != null) {
                id.invalid = true;
                id.repaint();
                app.toast("Mã thiết bị đã tồn tại", "Đã có thiết bị " + code + ". Hãy chọn mã khác để không ghi đè thiết bị cũ.", Ui.ToastKind.WARN);
                return;
            }
            String nm = name.getText().trim().isEmpty() ? code : name.getText().trim();
            String lc = loc.getText().trim();
            String body = "{\"deviceId\":" + Jn.quote(code) + ",\"name\":" + Jn.quote(nm)
                    + ",\"location\":" + Jn.quote(lc) + ",\"status\":\"ACTIVE\"}";
            ok.setEnabled(false);
            app.api().send("POST", "/api/devices", body, j -> {
                app.closeModal();
                Model.Dev base = new Model.Dev(code, nm, lc.isEmpty() ? null : lc, false, "NORMAL", null, null, null);
                app.deviceAdded(base);
                app.selectDevice(code);
                app.toast("Đã thêm thiết bị " + code, "Đã lưu vào MySQL" + (lc.isEmpty() ? "" : " (" + lc + ")") + ". Đang gửi số đo đầu tiên…", Ui.ToastKind.INFO);
                firstReading(code, nm, lc);
            }, err -> {
                ok.setEnabled(true);
                app.toast("Không thêm được thiết bị", err, Ui.ToastKind.ERROR);
            });
        };
        cancel.addActionListener(e -> app.closeModal());
        ok.addActionListener(e -> submit.run());
        app.modal(Ui.dialogCard("Thêm thiết bị", "Thiết bị sẽ được lưu vào MySQL, hiện ngay trên Dashboard và tự nhận dữ liệu từ IoT Client.", form, cancel, ok), submit, id);
    }

    /** Gửi ngay một số đo đầu tiên để thiết bị Online và có dữ liệu trên màn hình, rồi theo dõi dữ liệu liên tục. */
    private void firstReading(String code, String name, String loc) {
        double t = Math.round((23 + Math.random() * 5) * 10) / 10.0, h = Math.round((55 + Math.random() * 10) * 10) / 10.0;
        String ts = app.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        String body = String.format(java.util.Locale.US, "{\"deviceId\":%s,\"temperature\":%.1f,\"humidity\":%.1f,\"timestamp\":\"%s\"}",
                Jn.quote(code), t, h, ts);
        app.api().send("POST", "/api/sensor-data", body, j -> {
            String st = j.path("alertStatus").text("NORMAL");
            Model.Dev dev = new Model.Dev(code, name, loc.isEmpty() ? null : loc, true, st, t, h, app.now());
            app.deviceAdded(dev);
            app.toast(code + " đã có dữ liệu", "Số đo đầu tiên: " + Model.num1(t) + " °C · " + Model.num1(h)
                    + " %. Thiết bị đang Online và đã hiện trên Dashboard.", Ui.ToastKind.SUCCESS);
            app.watchFeed(code);
        }, err -> app.toast("Đã thêm " + code + " nhưng chưa gửi được số đo đầu tiên", err, Ui.ToastKind.WARN));
    }
}
