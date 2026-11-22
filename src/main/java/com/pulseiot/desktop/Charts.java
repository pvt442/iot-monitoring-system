package com.pulseiot.desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.*;
import java.util.function.LongSupplier;

/** Đồng hồ đo, biểu đồ xu hướng (hoạt ảnh mượt, vẽ đệm kép nên không nhấp nháy) và hình trang trí. */
final class Charts {
    private Charts() {}

    // =====================================================================
    //  Đồng hồ cung 270°
    // =====================================================================
    static final class Gauge extends JComponent {
        private final double min;
        private double max;
        private final Color c1, c2;
        private final String unit;
        private double target, shown;
        private boolean has;
        private double threshold = Double.NaN, margin = 0;

        Gauge(double min, double max, Color c1, Color c2, String unit) {
            this.min = min; this.max = max; this.c1 = c1; this.c2 = c2; this.unit = unit;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 262));
        }

        void setMax(double m) { if (m != max) { max = m; repaint(); } }

        void setLimits(double threshold, double margin) {
            if (this.threshold != threshold || this.margin != margin) {
                this.threshold = threshold; this.margin = margin; repaint();
            }
        }

        void setValue(Double v) {
            if (v == null) { if (has) { has = false; repaint(); } return; }
            if (!has) { shown = v; has = true; }
            target = v;
            repaint();
        }

        /** Gọi mỗi khung hình: trả true nếu còn đang chuyển động. */
        boolean step() {
            if (!has || Math.abs(target - shown) < 0.004) {
                if (has && shown != target) { shown = target; repaint(); }
                return false;
            }
            shown += (target - shown) * 0.16;
            repaint();
            return true;
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            double r = Math.min((w - 104) / 2.0, (h - 70) / 1.71);
            r = Math.max(60, r);
            double cx = w / 2.0, cy = r + 38;
            double f = has ? Math.max(0, Math.min(1, (shown - min) / (max - min))) : 0;

            // vòng nền
            g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(62, 70, 82));
            g.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 225, -270, Arc2D.OPEN));

            // cung giá trị
            if (has && f > 0.002) {
                Color a = c1, b = c2;
                if (!Double.isNaN(threshold)) {
                    if (shown > threshold) { a = new Color(244, 90, 90); b = new Color(251, 140, 120); }
                    else if (margin > 0 && shown >= threshold - margin) { a = new Color(245, 158, 11); b = new Color(251, 205, 70); }
                }
                Arc2D arc = new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 225, -270 * f, Arc2D.OPEN);
                g.setStroke(new BasicStroke(30f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Theme.alpha(a, 28));
                g.draw(arc);
                g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setPaint(new LinearGradientPaint((float) (cx - r), 0, (float) (cx + r), 0, new float[]{0f, 1f}, new Color[]{a, b}));
                g.draw(arc);
            }

            // vạch ngưỡng
            if (!Double.isNaN(threshold) && threshold >= min && threshold <= max) {
                double ang = Math.toRadians(225 - 270 * (threshold - min) / (max - min));
                double ca = Math.cos(ang), sa = -Math.sin(ang);
                g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(248, 90, 90));
                g.draw(new Line2D.Double(cx + (r - 15) * ca, cy + (r - 15) * sa, cx + (r + 15) * ca, cy + (r + 15) * sa));
                String t = trim(threshold);
                Font tf = Theme.semi(14f);
                FontMetrics fm = g.getFontMetrics(tf);
                double lx = cx + (r + 28) * ca, ly = cy + (r + 28) * sa;
                Theme.text(g, t, (int) (lx - fm.stringWidth(t) / 2.0), (int) (ly + fm.getAscent() / 2.0 - 2), tf, Theme.MUTED);
            }

            // nhãn min / max
            Font lf = Theme.regular(13.5f);
            FontMetrics lm = g.getFontMetrics(lf);
            double ex = r * Math.cos(Math.toRadians(45)), ey = cy + r * Math.sin(Math.toRadians(45)) + 28;
            String smin = trim(min), smax = trim(max);
            Theme.text(g, smin, (int) (cx - ex - lm.stringWidth(smin) / 2.0), (int) ey, lf, Theme.MUTED);
            Theme.text(g, smax, (int) (cx + ex - lm.stringWidth(smax) / 2.0), (int) ey, lf, Theme.MUTED);

            // số giữa
            String val = has ? String.format(java.util.Locale.US, "%.1f", shown) : "--";
            Font bf = Theme.bold((float) Math.max(34, r * 0.56));
            Font uf = Theme.semi((float) Math.max(16, r * 0.2));
            FontMetrics bm = g.getFontMetrics(bf), um = g.getFontMetrics(uf);
            int tw = bm.stringWidth(val) + 6 + um.stringWidth(unit);
            int bx = (int) (cx - tw / 2.0), by = (int) (cy + bm.getAscent() * 0.34);
            Theme.text(g, val, bx, by, bf, Theme.TEXT);
            Theme.text(g, unit, bx + bm.stringWidth(val) + 6, by, uf, Theme.MUTED);
            g.dispose();
        }

        private static String trim(double d) {
            return d == Math.rint(d) ? String.valueOf((long) d) : String.format(java.util.Locale.US, "%.1f", d);
        }
    }

    // =====================================================================
    //  Biểu đồ xu hướng theo thời gian thực
    // =====================================================================
    static final class Trend extends JComponent {
        private final boolean temperature;
        private final Color color;
        private final String unit;
        private Model.Series series = Model.Series.EMPTY;
        private long windowMs = 5 * 60_000L;
        private LongSupplier nowMs = System::currentTimeMillis;
        private double threshold = Double.NaN;
        private double yLo, yHi, tgtLo, tgtHi;
        private boolean init;
        private double hoverX = -1;

        Trend(boolean temperature, Color color, String unit) {
            this.temperature = temperature; this.color = color; this.unit = unit;
            setOpaque(false);
            setPreferredSize(new Dimension(400, 270));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { hoverX = e.getX(); repaint(); }
                @Override public void mouseExited(MouseEvent e) { hoverX = -1; repaint(); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        void configure(LongSupplier now) { this.nowMs = now; }

        void setWindow(long ms) { if (ms != windowMs) { windowMs = ms; init = false; repaint(); } }

        void setThreshold(double t) { if (Double.compare(threshold, t) != 0) { threshold = t; repaint(); } }

        void setSeries(Model.Series s) {
            if (s == series || s.sig.equals(series.sig)) return;
            series = s;
            repaint();
        }

        void clear() { series = Model.Series.EMPTY; init = false; repaint(); }

        private double[] vals() { return temperature ? series.temp : series.hum; }

        private boolean empty(long start, long end) {
            for (long t : series.t) if (t >= start && t <= end + 5000) return false;
            return true;
        }

        private static double niceStep(double raw) {
            double p = Math.pow(10, Math.floor(Math.log10(raw)));
            double n = raw / p;
            double s = n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 5 ? 5 : 10;
            return s * p;
        }

        /** Tính khoảng trục Y mục tiêu theo các điểm đang hiển thị. */
        private void retarget(long start, long end) {
            double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
            double[] v = vals();
            for (int i = 0; i < v.length; i++) {
                if (series.t[i] < start || series.t[i] > end + 5000) continue;
                lo = Math.min(lo, v[i]);
                hi = Math.max(hi, v[i]);
            }
            if (lo > hi) return;
            double span = hi - lo;
            double minSpan = temperature ? 4 : 8;
            if (span < minSpan) { double mid = (hi + lo) / 2; lo = mid - minSpan / 2; hi = mid + minSpan / 2; span = minSpan; }
            double pad = span * 0.15;
            lo -= pad; hi += pad;
            double step = niceStep((hi - lo) / 4);
            tgtLo = Math.floor(lo / step) * step;
            tgtHi = Math.ceil(hi / step) * step;
            if (!temperature) { tgtLo = Math.max(0, tgtLo); tgtHi = Math.min(100, Math.max(tgtHi, tgtLo + step)); }
            if (!init) { yLo = tgtLo; yHi = tgtHi; init = true; }
        }

        /** Gọi mỗi khung hình để làm mượt trục và cho biểu đồ trôi theo thời gian. */
        void frame() {
            long end = nowMs.getAsLong();
            if (init) {
                yLo += (tgtLo - yLo) * 0.14;
                yHi += (tgtHi - yHi) * 0.14;
                if (Math.abs(yLo - tgtLo) < 0.005) yLo = tgtLo;
                if (Math.abs(yHi - tgtHi) < 0.005) yHi = tgtHi;
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            int L = 50, R = 16, T = 12, B = 30;
            int pw = w - L - R, ph = h - T - B;
            long end = nowMs.getAsLong(), start = end - windowMs;

            if (empty(start, end)) {
                Font tf = Theme.bold(15.5f), sf = Theme.regular(14f);
                String a = "Chưa có dữ liệu trong khoảng thời gian này";
                String b = "Hãy chạy IoT Client hoặc chọn khoảng thời gian dài hơn.";
                FontMetrics fa = g.getFontMetrics(tf), fb = g.getFontMetrics(sf);
                Theme.text(g, Theme.fit(fa, a, w - 20), Math.max(8, (w - fa.stringWidth(a)) / 2), h / 2 - 4, tf, Theme.TEXT);
                Theme.text(g, Theme.fit(fb, b, w - 20), Math.max(8, (w - fb.stringWidth(b)) / 2), h / 2 + 22, sf, Theme.MUTED);
                g.dispose();
                return;
            }
            retarget(start, end);
            double lo = yLo, hi = yHi;
            if (hi - lo < 1e-6) hi = lo + 1;

            // lưới ngang + nhãn trục Y
            double step = niceStep((tgtHi - tgtLo) / 4);
            Font af = Theme.regular(12f);
            g.setStroke(new BasicStroke(1f));
            for (double v = Math.ceil(lo / step - 1e-9) * step; v <= hi + 1e-9; v += step) {
                double y = T + ph - (v - lo) / (hi - lo) * ph;
                g.setColor(new Color(255, 255, 255, 18));
                g.draw(new Line2D.Double(L, y, L + pw, y));
                String s = step < 1 ? String.format(java.util.Locale.US, "%.1f", v) : String.valueOf(Math.round(v));
                FontMetrics fm = g.getFontMetrics(af);
                Theme.text(g, s, L - 10 - fm.stringWidth(s), (int) (y + fm.getAscent() / 2.0 - 1), af, Theme.MUTED);
            }

            // nhãn thời gian (mốc tròn, trôi theo thời gian)
            long[] steps = {5_000, 10_000, 15_000, 30_000, 60_000, 120_000, 300_000, 600_000, 900_000, 1_800_000};
            long iv = steps[steps.length - 1];
            for (long s : steps) if (windowMs / s <= 6) { iv = s; break; }
            ZoneId zone = ZoneId.systemDefault();
            java.time.format.DateTimeFormatter tf = iv < 60_000 ? Model.HMS : Model.HM;
            FontMetrics tm = g.getFontMetrics(af);
            long firstOffset = zone.getRules().getOffset(Instant.ofEpochMilli(start)).getTotalSeconds() * 1000L;
            for (long t = ((start + firstOffset) / iv + 1) * iv - firstOffset; t <= end; t += iv) {
                double x = L + (t - start) / (double) windowMs * pw;
                if (x < L + 14 || x > L + pw - 14) continue;
                g.setColor(new Color(255, 255, 255, 10));
                g.draw(new Line2D.Double(x, T, x, T + ph));
                String s = LocalDateTime.ofInstant(Instant.ofEpochMilli(t), zone).format(tf);
                Theme.text(g, s, (int) (x - tm.stringWidth(s) / 2.0), h - 8, af, Theme.MUTED);
            }

            // đường ngưỡng
            if (!Double.isNaN(threshold) && threshold > lo && threshold < hi) {
                double y = T + ph - (threshold - lo) / (hi - lo) * ph;
                g.setColor(Theme.alpha(Theme.RED, 140));
                g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 5f}, 0f));
                g.draw(new Line2D.Double(L, y, L + pw, y));
                g.setStroke(new BasicStroke(1f));
                String s = "Ngưỡng " + (threshold == Math.rint(threshold) ? String.valueOf((long) threshold) : Model.num1(threshold));
                Theme.text(g, s, L + pw - tm.stringWidth(s) - 4, (int) y - 6, Theme.semi(12f), Theme.alpha(Theme.RED, 220));
            }

            // dựng đường cong
            double[] v = vals();
            int n = v.length;
            int first = 0;
            while (first < n - 1 && series.t[first + 1] < start) first++;
            int stride = Math.max(1, (int) ((n - first) / Math.max(1.0, pw / 1.6)));
            java.util.List<double[]> pts = new java.util.ArrayList<>();
            for (int i = first; i < n; i += stride) pts.add(pt(i, start, L, T, pw, ph, lo, hi));
            if ((n - 1 - first) % stride != 0) pts.add(pt(n - 1, start, L, T, pw, ph, lo, hi));
            Graphics2D cg = (Graphics2D) g.create();
            cg.clip(new Rectangle(L - 2, T - 6, pw + 4, ph + 12));
            Path2D.Double line = smooth(pts);
            if (pts.size() > 1) {
                Path2D.Double area = new Path2D.Double(line);
                double[] last = pts.get(pts.size() - 1), firstP = pts.get(0);
                area.lineTo(last[0], T + ph);
                area.lineTo(firstP[0], T + ph);
                area.closePath();
                cg.setPaint(new GradientPaint(0, T, Theme.alpha(color, 70), 0, T + ph, Theme.alpha(color, 0)));
                cg.fill(area);
                cg.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                cg.setColor(color);
                cg.draw(line);
            }
            double[] lp = pts.get(pts.size() - 1);
            cg.setColor(Theme.alpha(color, 50));
            cg.fill(new Ellipse2D.Double(lp[0] - 9, lp[1] - 9, 18, 18));
            cg.setColor(color);
            cg.fill(new Ellipse2D.Double(lp[0] - 4.5, lp[1] - 4.5, 9, 9));
            cg.setColor(new Color(37, 44, 54));
            cg.fill(new Ellipse2D.Double(lp[0] - 2, lp[1] - 2, 4, 4));
            cg.dispose();

            // con trỏ rê chuột + tooltip
            if (hoverX >= L && hoverX <= L + pw) {
                int best = -1;
                double bd = 1e9;
                for (int i = first; i < n; i++) {
                    double x = L + (series.t[i] - start) / (double) windowMs * pw;
                    double d = Math.abs(x - hoverX);
                    if (x >= L && d < bd) { bd = d; best = i; }
                }
                if (best >= 0 && bd < 40) {
                    double[] p = pt(best, start, L, T, pw, ph, lo, hi);
                    g.setColor(new Color(255, 255, 255, 50));
                    g.setStroke(new BasicStroke(1f));
                    g.draw(new Line2D.Double(p[0], T, p[0], T + ph));
                    g.setColor(color);
                    g.fill(new Ellipse2D.Double(p[0] - 5, p[1] - 5, 10, 10));
                    String l1 = Model.num1(v[best]) + " " + unit;
                    String l2 = LocalDateTime.ofInstant(Instant.ofEpochMilli(series.t[best]), zone).format(Model.HMS);
                    Font f1 = Theme.bold(14f), f2 = Theme.regular(12f);
                    int bw = Math.max(g.getFontMetrics(f1).stringWidth(l1), g.getFontMetrics(f2).stringWidth(l2)) + 24, bh = 50;
                    int bx = (int) Math.min(Math.max(L, p[0] + 12), L + pw - bw), by = (int) Math.max(T, Math.min(p[1] - bh - 10, T + ph - bh));
                    g.setColor(new Color(20, 26, 33, 235));
                    g.fillRoundRect(bx, by, bw, bh, 14, 14);
                    g.setColor(Theme.LINE2);
                    g.drawRoundRect(bx, by, bw, bh, 14, 14);
                    Theme.text(g, l1, bx + 12, by + 22, f1, color);
                    Theme.text(g, l2, bx + 12, by + 40, f2, Theme.MUTED);
                }
            }
            g.dispose();
        }

        private double[] pt(int i, long start, int L, int T, int pw, int ph, double lo, double hi) {
            double x = L + (series.t[i] - start) / (double) windowMs * pw;
            double y = T + ph - (vals()[i] - lo) / (hi - lo) * ph;
            return new double[]{x, y};
        }

        /** Catmull-Rom → Bézier, có giới hạn để không vọt quá giá trị. */
        private static Path2D.Double smooth(java.util.List<double[]> p) {
            Path2D.Double path = new Path2D.Double();
            if (p.isEmpty()) return path;
            path.moveTo(p.get(0)[0], p.get(0)[1]);
            for (int i = 0; i < p.size() - 1; i++) {
                double[] p0 = p.get(Math.max(0, i - 1)), p1 = p.get(i), p2 = p.get(i + 1), p3 = p.get(Math.min(p.size() - 1, i + 2));
                double c1x = p1[0] + (p2[0] - p0[0]) / 6, c1y = p1[1] + (p2[1] - p0[1]) / 6;
                double c2x = p2[0] - (p3[0] - p1[0]) / 6, c2y = p2[1] - (p3[1] - p1[1]) / 6;
                double lo = Math.min(p1[1], p2[1]), hi = Math.max(p1[1], p2[1]);
                c1y = Math.max(lo, Math.min(hi, c1y));
                c2y = Math.max(lo, Math.min(hi, c2y));
                path.curveTo(c1x, c1y, c2x, c2y, p2[0], p2[1]);
            }
            return path;
        }
    }

    // =====================================================================
    //  Hình quả cầu có vành đai (trang trí)
    // =====================================================================
    static void planet(Graphics2D g0, double cx, double cy, double r) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(cx, cy);
        ring(g, r, 2.5 * r, 0.82 * r, -64, 0.045 * r, new Color(80, 200, 190, 90), true);
        ring(g, r, 1.9 * r, 0.52 * r, -30, 0.10 * r, new Color(150, 185, 190, 150), true);
        Ellipse2D sphere = new Ellipse2D.Double(-r, -r, 2 * r, 2 * r);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(-0.35 * r, -0.4 * r), (float) (1.35 * r),
                new float[]{0f, 0.5f, 1f}, new Color[]{new Color(72, 84, 96), new Color(22, 28, 36), new Color(5, 8, 11)}));
        g.fill(sphere);
        Graphics2D c = (Graphics2D) g.create();
        c.clip(sphere);
        c.setPaint(new RadialGradientPaint(new Point2D.Double(-0.45 * r, -0.5 * r), (float) (0.55 * r),
                new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 50), new Color(255, 255, 255, 0)}));
        c.fill(sphere);
        c.dispose();
        g.setColor(new Color(45, 212, 191, 38));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(sphere);
        ring(g, r, 1.9 * r, 0.52 * r, -30, 0.10 * r, new Color(150, 185, 190, 170), false);
        ring(g, r, 2.5 * r, 0.82 * r, -64, 0.045 * r, new Color(80, 200, 190, 110), false);
        g.dispose();
    }

    private static void ring(Graphics2D g0, double r, double rx, double ry, double rot, double sw, Color c, boolean back) {
        Graphics2D g = (Graphics2D) g0.create();
        g.rotate(Math.toRadians(rot));
        g.setStroke(new BasicStroke((float) sw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        g.draw(new Arc2D.Double(-rx, -ry, 2 * rx, 2 * ry, back ? 0 : 180, 180, Arc2D.OPEN));
        g.dispose();
    }
}
