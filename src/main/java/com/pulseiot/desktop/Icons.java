package com.pulseiot.desktop;

import java.awt.*;
import java.awt.geom.*;

/** Bộ icon vector (phong cách Lucide, lưới 24x24) - không phụ thuộc font nên không bao giờ bị lỗi ô vuông. */
enum Ic {
    LOGO, DASH, CHIP, HISTORY, BELL, GEAR, SEARCH, CLOCK, MENU, THERMO, DROP, WIFI, WIFI_OFF, CHECK_CIRCLE,
    EYE, TRASH, PLUS, REFRESH, RESET, DOWNLOAD, SAVE, ALERT, PIN, CHEVRON_DOWN, CHEVRON_LEFT, CHEVRON_RIGHT,
    X, INFO, CHECK;

    void paint(Graphics g, double x, double y, double size, Color c) {
        paint(g, x, y, size, c, 1.9f);
    }

    void paint(Graphics g, double x, double y, double size, Color c, float stroke) {
        Graphics2D g2 = Theme.quality(g);
        g2.translate(x, y);
        g2.scale(size / 24.0, size / 24.0);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(c);
        draw(g2);
        g2.dispose();
    }

    /** Icon dạng javax.swing.Icon, tiện đặt vào JLabel. */
    javax.swing.Icon icon(int size, Color c) {
        return new javax.swing.Icon() {
            public void paintIcon(Component comp, Graphics g, int x, int y) { Ic.this.paint(g, x, y, size, c); }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void poly(Graphics2D g, double... p) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        g.draw(path);
    }

    private static void rr(Graphics2D g, double x, double y, double w, double h, double r) {
        g.draw(new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2));
    }

    private static void circle(Graphics2D g, double cx, double cy, double r) {
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static void dot(Graphics2D g, double cx, double cy, double r) {
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static void arc(Graphics2D g, double cx, double cy, double r, double start, double extent) {
        g.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, start, extent, Arc2D.OPEN));
    }

    private void draw(Graphics2D g) {
        switch (this) {
            case LOGO -> poly(g, 2, 12, 6, 12, 9, 3.2, 15, 20.8, 18, 12, 22, 12);
            case DASH -> {
                rr(g, 3, 3, 7, 7, 1.8);
                rr(g, 14, 3, 7, 7, 1.8);
                rr(g, 14, 14, 7, 7, 1.8);
                rr(g, 3, 14, 7, 7, 1.8);
            }
            case CHIP -> {
                rr(g, 4, 4, 16, 16, 2.4);
                rr(g, 9, 9, 6, 6, 1);
                line(g, 9, 2, 9, 4); line(g, 15, 2, 15, 4);
                line(g, 9, 20, 9, 22); line(g, 15, 20, 15, 22);
                line(g, 2, 9, 4, 9); line(g, 2, 15, 4, 15);
                line(g, 20, 9, 22, 9); line(g, 20, 15, 22, 15);
            }
            case HISTORY -> {
                Path2D.Double p = new Path2D.Double();
                p.append(new Arc2D.Double(3, 3, 18, 18, 180, 315, Arc2D.OPEN), false);
                p.lineTo(3, 8);
                g.draw(p);
                poly(g, 3, 3, 3, 8, 8, 8);
                poly(g, 12, 7, 12, 12, 16, 14);
            }
            case BELL -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(6, 8);
                p.curveTo(6, 4.7, 8.7, 2, 12, 2);
                p.curveTo(15.3, 2, 18, 4.7, 18, 8);
                p.curveTo(18, 15, 21, 17, 21, 17);
                p.lineTo(3, 17);
                p.curveTo(3, 17, 6, 15, 6, 8);
                p.closePath();
                g.draw(p);
                Path2D.Double q = new Path2D.Double();
                q.moveTo(10.3, 21);
                q.curveTo(10.7, 21.6, 11.3, 22, 12, 22);
                q.curveTo(12.7, 22, 13.3, 21.6, 13.7, 21);
                g.draw(q);
            }
            case GEAR -> {
                Path2D.Double p = new Path2D.Double();
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    double[][] pts = {{7.4, a - 0.38}, {9.9, a - 0.2}, {9.9, a + 0.2}, {7.4, a + 0.38}};
                    for (double[] pt : pts) {
                        double px = 12 + pt[0] * Math.cos(pt[1]), py = 12 + pt[0] * Math.sin(pt[1]);
                        if (p.getCurrentPoint() == null) p.moveTo(px, py); else p.lineTo(px, py);
                    }
                }
                p.closePath();
                g.draw(p);
                circle(g, 12, 12, 3);
            }
            case SEARCH -> {
                circle(g, 11, 11, 7.5);
                line(g, 20.5, 20.5, 16.4, 16.4);
            }
            case CLOCK -> {
                circle(g, 12, 12, 9.5);
                poly(g, 12, 6.5, 12, 12, 16, 14);
            }
            case MENU -> {
                line(g, 4, 6.5, 20, 6.5);
                line(g, 4, 12, 20, 12);
                line(g, 4, 17.5, 20, 17.5);
            }
            case THERMO -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(14, 4);
                p.lineTo(14, 14.54);
                p.append(new Arc2D.Double(8, 14, 8, 8, 60, -240, Arc2D.OPEN), true);
                p.lineTo(10, 4);
                p.append(new Arc2D.Double(10, 2, 4, 4, 180, -180, Arc2D.OPEN), true);
                g.draw(p);
                line(g, 12, 9, 12, 18);
            }
            case DROP -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(12, 22);
                p.curveTo(15.9, 22, 19, 18.9, 19, 15);
                p.curveTo(19, 13, 18, 11.1, 16, 9.5);
                p.curveTo(14.5, 8.3, 12.5, 5.5, 12, 3);
                p.curveTo(11.5, 5.5, 10, 7.9, 8, 9.5);
                p.curveTo(6, 11.1, 5, 13, 5, 15);
                p.curveTo(5, 18.9, 8.1, 22, 12, 22);
                p.closePath();
                g.draw(p);
            }
            case WIFI -> wifi(g, false);
            case WIFI_OFF -> wifi(g, true);
            case CHECK_CIRCLE -> {
                circle(g, 12, 12, 9.5);
                poly(g, 7.8, 12.4, 10.8, 15.4, 16.4, 9.2);
            }
            case EYE -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(2, 12);
                p.curveTo(5, 6.5, 8.5, 5, 12, 5);
                p.curveTo(15.5, 5, 19, 6.5, 22, 12);
                p.curveTo(19, 17.5, 15.5, 19, 12, 19);
                p.curveTo(8.5, 19, 5, 17.5, 2, 12);
                g.draw(p);
                circle(g, 12, 12, 3);
            }
            case TRASH -> {
                line(g, 3, 6, 21, 6);
                Path2D.Double p = new Path2D.Double();
                p.moveTo(19, 6); p.lineTo(19, 20); p.quadTo(19, 22, 17, 22); p.lineTo(7, 22); p.quadTo(5, 22, 5, 20); p.lineTo(5, 6);
                g.draw(p);
                Path2D.Double q = new Path2D.Double();
                q.moveTo(8, 6); q.lineTo(8, 4); q.quadTo(8, 2, 10, 2); q.lineTo(14, 2); q.quadTo(16, 2, 16, 4); q.lineTo(16, 6);
                g.draw(q);
                line(g, 10, 11, 10, 17);
                line(g, 14, 11, 14, 17);
            }
            case PLUS -> {
                line(g, 12, 5, 12, 19);
                line(g, 5, 12, 19, 12);
            }
            case REFRESH -> refresh(g, false);
            case RESET -> refresh(g, true);
            case DOWNLOAD -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(21, 15); p.lineTo(21, 19); p.quadTo(21, 21, 19, 21); p.lineTo(5, 21); p.quadTo(3, 21, 3, 19); p.lineTo(3, 15);
                g.draw(p);
                poly(g, 7, 10, 12, 15, 17, 10);
                line(g, 12, 15, 12, 3);
            }
            case SAVE -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(15.2, 3); p.quadTo(16.4, 3, 17.3, 3.9); p.lineTo(20.1, 6.7); p.quadTo(21, 7.6, 21, 8.8);
                p.lineTo(21, 19); p.quadTo(21, 21, 19, 21); p.lineTo(5, 21); p.quadTo(3, 21, 3, 19); p.lineTo(3, 5);
                p.quadTo(3, 3, 5, 3); p.closePath();
                g.draw(p);
                poly(g, 17, 21, 17, 14.2, 16.8, 13.4, 16, 13, 8, 13, 7.2, 13.4, 7, 14.2, 7, 21);
                poly(g, 7, 3, 7, 7.2, 7.4, 7.9, 8.2, 8, 15, 8);
            }
            case ALERT -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(12, 3.2); p.lineTo(21.6, 19.6); p.lineTo(2.4, 19.6); p.closePath();
                g.draw(p);
                line(g, 12, 9.5, 12, 13.5);
                dot(g, 12, 16.6, 0.9);
            }
            case PIN -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(20, 10);
                p.curveTo(20, 15, 14.5, 20.2, 12.6, 21.8);
                p.quadTo(12, 22.3, 11.4, 21.8);
                p.curveTo(9.5, 20.2, 4, 15, 4, 10);
                p.append(new Arc2D.Double(4, 2, 16, 16, 180, -180, Arc2D.OPEN), true);
                g.draw(p);
                circle(g, 12, 10, 3);
            }
            case CHEVRON_DOWN -> poly(g, 6, 9, 12, 15, 18, 9);
            case CHEVRON_LEFT -> poly(g, 15, 18, 9, 12, 15, 6);
            case CHEVRON_RIGHT -> poly(g, 9, 18, 15, 12, 9, 6);
            case X -> {
                line(g, 18, 6, 6, 18);
                line(g, 6, 6, 18, 18);
            }
            case INFO -> {
                circle(g, 12, 12, 9.5);
                line(g, 12, 11, 12, 16.5);
                dot(g, 12, 7.8, 0.9);
            }
            case CHECK -> poly(g, 20, 6, 9, 17, 4, 12);
        }
    }

    private static void wifi(Graphics2D g, boolean off) {
        arc(g, 12, 19, 16.5, 45, 90);
        arc(g, 12, 19, 11, 42, 96);
        arc(g, 12, 19, 5.6, 38, 104);
        dot(g, 12, 19.6, 1.1);
        if (off) {
            Graphics2D h = (Graphics2D) g.create();
            h.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            line(h, 3, 3, 21, 21);
            h.dispose();
        }
    }

    private static void refresh(Graphics2D g, boolean flip) {
        Graphics2D h = (Graphics2D) g.create();
        if (flip) {
            h.translate(24, 0);
            h.scale(-1, 1);
        }
        Path2D.Double p = new Path2D.Double();
        p.append(new Arc2D.Double(3, 3, 18, 18, 0, -270, Arc2D.OPEN), false);
        p.curveTo(14.5, 3, 16.9, 4, 18.7, 5.7);
        p.lineTo(21, 8);
        h.draw(p);
        poly(h, 21, 3, 21, 8, 16, 8);
        h.dispose();
    }
}
