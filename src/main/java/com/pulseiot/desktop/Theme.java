package com.pulseiot.desktop;

import java.awt.*;
import java.util.Arrays;

/** Bảng màu, font và các hàm vẽ dùng chung - mô phỏng giao diện web Pulse IoT (nền tối, accent teal). */
final class Theme {
    private Theme() {}

    static final Color BG = new Color(13, 18, 24);
    static final Color CARD = new Color(37, 44, 54);
    static final Color CARD2 = new Color(30, 37, 46);
    static final Color POPUP = new Color(33, 40, 50);
    static final Color INPUT = new Color(22, 27, 33);
    static final Color LINE = new Color(255, 255, 255, 18);
    static final Color LINE2 = new Color(255, 255, 255, 34);

    static final Color TEXT = new Color(240, 244, 248);
    static final Color MUTED = new Color(158, 168, 180);
    static final Color DIM = new Color(115, 125, 137);

    static final Color TEAL = new Color(45, 212, 191);
    static final Color TEAL_D = new Color(22, 118, 110);
    static final Color ORANGE = new Color(245, 166, 35);
    static final Color CYAN = new Color(38, 188, 222);
    static final Color RED = new Color(248, 113, 113);
    static final Color GREEN = new Color(52, 211, 153);
    static final Color AMBER = new Color(251, 191, 36);

    static final String FAMILY = pick("Segoe UI", "Inter", "SF Pro Text", "Roboto", "Helvetica Neue", "Arial", Font.SANS_SERIF);
    static final String FAMILY_SEMI = pick("Segoe UI Semibold", null);

    private static String pick(String... names) {
        String[] all = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        java.util.Set<String> set = new java.util.HashSet<>(Arrays.asList(all));
        for (String n : names) {
            if (n == null) return null;
            if (set.contains(n)) return n;
        }
        return Font.SANS_SERIF;
    }

    static Font font(int style, float size) {
        return new Font(FAMILY, style, 1).deriveFont(size);
    }

    static Font regular(float size) { return font(Font.PLAIN, size); }

    static Font bold(float size) { return font(Font.BOLD, size); }

    /** Chữ đậm vừa (Segoe UI Semibold nếu có, nếu không dùng Bold). */
    static Font semi(float size) {
        return FAMILY_SEMI != null ? new Font(FAMILY_SEMI, Font.PLAIN, 1).deriveFont(size) : bold(size);
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    static Graphics2D quality(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g2;
    }

    /** Cắt chuỗi và thêm "…" nếu vượt quá bề rộng cho phép. */
    static String fit(FontMetrics fm, String s, int maxW) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= maxW) return s;
        String e = "…";
        int n = s.length();
        while (n > 0 && fm.stringWidth(s.substring(0, n)) + fm.stringWidth(e) > maxW) n--;
        return s.substring(0, Math.max(0, n)) + e;
    }

    /** Vẽ chuỗi theo đường cơ sở, trả về bề rộng. */
    static int text(Graphics2D g, String s, int x, int baseline, Font f, Color c) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, x, baseline);
        return g.getFontMetrics().stringWidth(s);
    }

    /** Vẽ chuỗi căn giữa theo chiều dọc trong ô [y, y+h]. */
    static int textMid(Graphics2D g, String s, int x, int y, int h, Font f, Color c) {
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(c);
        g.drawString(s, x, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        return fm.stringWidth(s);
    }
}
