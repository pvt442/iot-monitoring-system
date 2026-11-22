package com.pulseiot.desktop;

import javax.swing.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Gọi REST API của Backend (HTTP bất đồng bộ, callback chạy trên luồng giao diện). */
final class Api {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "pulse-api");
        t.setDaemon(true);
        return t;
    });
    final String base;

    Api(String base) { this.base = base; }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    void get(String path, Consumer<Jn> ok, Consumer<String> fail) { send("GET", path, null, ok, fail); }

    void send(String method, String path, String body, Consumer<Jn> ok, Consumer<String> fail) {
        pool.execute(() -> {
            try {
                HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(8));
                if (body != null) b.header("Content-Type", "application/json; charset=UTF-8");
                b.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (r.statusCode() < 200 || r.statusCode() >= 300) {
                    String msg = Jn.parse(safeBody(r.body())).path("message").text("");
                    throw new IllegalStateException(msg.isBlank() ? "Server trả về lỗi HTTP " + r.statusCode() : msg);
                }
                Jn json = Jn.parse(r.body());
                SwingUtilities.invokeLater(() -> ok.accept(json));
            } catch (Exception e) {
                String msg = e instanceof java.net.ConnectException || e instanceof HttpTimeoutException
                        || e instanceof java.net.http.HttpConnectTimeoutException
                        ? "Không kết nối được Server tại " + base
                        : (e.getMessage() == null ? "Lỗi không xác định" : e.getMessage());
                SwingUtilities.invokeLater(() -> fail.accept(msg));
            }
        });
    }

    private static String safeBody(String b) {
        try { return b; } catch (Exception e) { return ""; }
    }
}
