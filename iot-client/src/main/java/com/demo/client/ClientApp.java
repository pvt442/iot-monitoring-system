package com.demo.client;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * IoT Client Simulator - điểm khởi chạy.
 *
 * Chạy:  java -cp out com.demo.client.ClientApp [--config=...] [--server=URL] [--devices=DEV_01,DEV_02] [--interval=3000] [--mode=RANDOM|CSV]
 * Trong lúc chạy có thể gõ lệnh (gõ "help" để xem) - dùng để tạo tình huống vượt ngưỡng / Offline khi demo.
 */
public final class ClientApp {

    private ClientApp() {
    }

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        ClientConfig config;
        try {
            config = ClientConfig.load(args);
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Lỗi cấu hình: " + e.getMessage());
            System.exit(2);
            return;
        }

        List<double[]> csvRows = null;
        if (config.mode.equals("CSV")) {
            try {
                csvRows = SensorGenerator.loadCsv(config.csvFile);
            } catch (IOException e) {
                System.err.println("Không đọc được file CSV " + config.csvFile + ": " + e.getMessage());
                System.exit(2);
                return;
            }
            if (csvRows.isEmpty()) {
                System.err.println("File CSV không có dòng dữ liệu hợp lệ: " + config.csvFile);
                System.exit(2);
                return;
            }
        }

        System.out.println("==============================================================");
        System.out.println(" IoT Client Simulator  |  Server: " + config.serverUrl);
        System.out.println(" Chế độ: " + config.mode + "  |  Chu kỳ: " + config.intervalMs + " ms  |  Thiết bị: " + config.devices.size());
        System.out.println(" Gõ 'help' để xem lệnh, 'quit' để thoát.");
        System.out.println("==============================================================");

        ServerApi api = new ServerApi(config.serverUrl, config.httpTimeoutMs);
        if (api.refreshThresholds()) {
            System.out.printf(Locale.ROOT, "Đã lấy ngưỡng từ Server: nhiệt độ > %.1f °C, độ ẩm > %.1f %%%n",
                    api.thresholds.temperature, api.thresholds.humidity);
        } else {
            System.out.println("Chưa lấy được ngưỡng từ Server - tạm dùng 35 °C / 80 % (sẽ thử lại khi dùng lệnh spike).");
        }

        List<DeviceSimulator> sims = new ArrayList<>();
        int n = config.devices.size();
        for (int i = 0; i < n; i++) {
            ClientConfig.DeviceProfile p = config.devices.get(i);
            SensorGenerator gen = new SensorGenerator(p, api.thresholds, csvRows);
            // Rải đều thời điểm bắt đầu để các thiết bị không gửi đồng loạt cùng một lúc
            DeviceSimulator sim = new DeviceSimulator(p, config, api, gen, (long) i * config.intervalMs / n);
            sims.add(sim);
            Thread t = new Thread(sim, "device-" + p.id());
            t.setDaemon(true);
            t.start();
        }

        CountDownLatch quit = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> sims.forEach(DeviceSimulator::stop)));

        Thread console = new Thread(() -> runConsole(config, api, sims, quit), "console");
        console.setDaemon(true);
        console.start();

        quit.await(); // chờ lệnh quit hoặc Ctrl+C (khi không có bàn phím/stdin đóng, chương trình vẫn chạy tiếp)
        sims.forEach(DeviceSimulator::stop);
        System.out.println("Đã dừng IoT Client.");
    }

    private static void runConsole(ClientConfig config, ServerApi api, List<DeviceSimulator> sims, CountDownLatch quit) {
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
        try {
            while (in.hasNextLine()) {
                String line = in.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] w = line.split("\\s+");
                String cmd = w[0].toLowerCase(Locale.ROOT);
                switch (cmd) {
                    case "help", "?" -> printHelp();
                    case "status" -> sims.forEach(s -> System.out.println("  " + s.describe()));
                    case "quit", "exit" -> {
                        quit.countDown();
                        return;
                    }
                    case "interval" -> setInterval(config, w);
                    case "spike" -> spike(api, sims, w);
                    case "normal" -> forEach(sims, arg(w, 1), s -> s.generator().setScenario(SensorGenerator.Scenario.NORMAL),
                            "về chế độ BÌNH THƯỜNG");
                    case "offline" -> forEach(sims, arg(w, 1), s -> s.setPaused(true), "NGẮT gửi dữ liệu (giả lập mất kết nối)");
                    case "online" -> forEach(sims, arg(w, 1), s -> s.setPaused(false), "gửi dữ liệu TRỞ LẠI");
                    default -> System.out.println("Lệnh không hợp lệ: " + cmd + " (gõ 'help').");
                }
            }
        } catch (IllegalStateException ignoreClosed) {
            // stdin đóng - bỏ qua, các thiết bị vẫn chạy
        }
    }

    private static void printHelp() {
        System.out.println("""
                Lệnh:
                  status                         xem trạng thái các thiết bị
                  spike <DEV_ID|all> [temp|hum|both]   tăng dần số đo tới mức VƯỢT ngưỡng (mặc định temp)
                  normal <DEV_ID|all>            quay về dữ liệu bình thường
                  offline <DEV_ID|all>           ngừng gửi (thử trạng thái Offline trên Dashboard)
                  online <DEV_ID|all>            gửi lại
                  interval <ms>                  đổi chu kỳ gửi (tối thiểu 500)
                  quit                           thoát""");
    }

    private static void setInterval(ClientConfig config, String[] w) {
        try {
            int v = Integer.parseInt(arg(w, 1));
            if (v < 500) {
                System.out.println("Chu kỳ tối thiểu 500 ms.");
                return;
            }
            config.intervalMs = v;
            System.out.println("Đã đổi chu kỳ gửi: " + v + " ms");
        } catch (NumberFormatException e) {
            System.out.println("Cú pháp: interval <ms>");
        }
    }

    private static void spike(ServerApi api, List<DeviceSimulator> sims, String[] w) {
        api.refreshThresholds(); // dùng đúng ngưỡng hiện hành trên Server
        String kind = w.length > 2 ? w[2].toLowerCase(Locale.ROOT) : "temp";
        SensorGenerator.Scenario sc = switch (kind) {
            case "hum", "humidity" -> SensorGenerator.Scenario.SPIKE_HUM;
            case "both" -> SensorGenerator.Scenario.SPIKE_BOTH;
            default -> SensorGenerator.Scenario.SPIKE_TEMP;
        };
        forEach(sims, arg(w, 1), s -> s.generator().setScenario(sc), "chế độ " + sc);
    }

    private static void forEach(List<DeviceSimulator> sims, String target, java.util.function.Consumer<DeviceSimulator> action,
                                String what) {
        if (target.isEmpty()) {
            System.out.println("Thiếu tên thiết bị: dùng DEV_ID hoặc 'all'.");
            return;
        }
        int count = 0;
        for (DeviceSimulator s : sims) {
            if (target.equalsIgnoreCase("all") || s.id().equalsIgnoreCase(target)) {
                action.accept(s);
                System.out.println("  " + s.id() + " -> " + what);
                count++;
            }
        }
        if (count == 0) {
            System.out.println("Không có thiết bị '" + target + "'.");
        }
    }

    private static String arg(String[] w, int i) {
        return i < w.length ? w[i] : "";
    }
}
