package com.pulseiot.desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.function.*;

/** Các thành phần giao diện tự vẽ: không phụ thuộc Look&Feel nên không bao giờ bị trắng chữ trắng nền. */
final class Ui {
    private Ui() {}

    // =====================================================================
    //  Nền ứng dụng
    // =====================================================================
    static final class Background extends JPanel {
        private final TexturePaint dots;

        Background() {
            setOpaque(true);
            BufferedImage img = new BufferedImage(26, 26, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255, 255, 255, 14));
            g.fill(new Ellipse2D.Double(12, 12, 1.6, 1.6));
            g.dispose();
            dots = new TexturePaint(img, new Rectangle(0, 0, 26, 26));
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            g.setColor(Theme.BG);
            g.fillRect(0, 0, w, h);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.25f, -40), Math.max(w, h) * 0.75f,
                    new float[]{0f, 1f}, new Color[]{new Color(18, 120, 112, 70), new Color(18, 120, 112, 0)}));
            g.fillRect(0, 0, w, h);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.95f, h * 1.05f), Math.max(w, h) * 0.6f,
                    new float[]{0f, 1f}, new Color[]{new Color(28, 58, 96, 55), new Color(28, 58, 96, 0)}));
            g.fillRect(0, 0, w, h);
            g.setPaint(dots);
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
    }

    // =====================================================================
    //  Card
    // =====================================================================
    static class Card extends JPanel {
        int radius = 24;
        Color fill = Theme.CARD;
        Color border = Theme.LINE;
        Color glow;
        float glowX = 0.1f, glowY = 0f;

        Card() { setOpaque(false); }

        Card(LayoutManager lm) { this(); setLayout(lm); }

        Card radius(int r) { radius = r; return this; }

        Card fill(Color c) { fill = c; return this; }

        Card border(Color c) { border = c; return this; }

        Card glow(Color c, float fx, float fy) { glow = c; glowX = fx; glowY = fy; return this; }

        Card pad(int t, int l, int b, int r) { setBorder(new EmptyBorder(t, l, b, r)); return this; }

        Shape shape() { return new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, radius * 2f, radius * 2f); }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            Shape s = shape();
            g.setColor(fill);
            g.fill(s);
            if (glow != null) {
                Graphics2D c = (Graphics2D) g.create();
                c.clip(s);
                float r = Math.max(getWidth(), getHeight()) * 0.7f;
                c.setPaint(new RadialGradientPaint(new Point2D.Float(getWidth() * glowX, getHeight() * glowY), r,
                        new float[]{0f, 1f}, new Color[]{glow, Theme.alpha(glow, 0)}));
                c.fillRect(0, 0, getWidth(), getHeight());
                c.dispose();
            }
            Graphics2D d = (Graphics2D) g.create();
            d.clip(s);
            paintDecor(d);
            d.dispose();
            if (border != null) {
                g.setColor(border);
                g.setStroke(new BasicStroke(1f));
                g.draw(s);
            }
            g.dispose();
        }

        void paintDecor(Graphics2D g) {}
    }

    // =====================================================================
    //  Nút bấm
    // =====================================================================
    enum Kind { LIGHT, DARK, TEAL, OUTLINE, DANGER }

    static final class Btn extends JButton {
        private final Ic icon;
        private final Kind kind;
        private int padX = 22, height = 46;

        Btn(String text, Ic icon, Kind kind) {
            super(text);
            this.icon = icon;
            this.kind = kind;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setFont(Theme.semi(14.5f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(0, 0, 0, 0));
        }

        Btn size(int h, int px) { height = h; padX = px; return this; }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = padX * 2 + fm.stringWidth(getText()) + (icon != null ? (getText().isEmpty() ? 18 : 28) : 0);
            return new Dimension(w, height);
        }

        @Override public Dimension getMinimumSize() { return getPreferredSize(); }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            boolean hover = getModel().isRollover(), press = getModel().isPressed();
            Color bg, fg, line = null;
            switch (kind) {
                case LIGHT -> { bg = press ? new Color(214, 222, 232) : hover ? Color.WHITE : new Color(238, 243, 248); fg = new Color(15, 22, 30); }
                case DARK -> { bg = press ? new Color(8, 11, 15) : hover ? new Color(24, 31, 39) : new Color(14, 19, 25); fg = Theme.TEXT; line = Theme.LINE2; }
                case TEAL -> { bg = press ? new Color(17, 98, 92) : hover ? new Color(28, 140, 130) : Theme.TEAL_D; fg = new Color(230, 253, 249); }
                case DANGER -> { bg = press ? new Color(160, 40, 50) : hover ? new Color(220, 72, 80) : new Color(196, 56, 66); fg = Color.WHITE; }
                default -> { bg = hover ? new Color(255, 255, 255, press ? 26 : 16) : new Color(255, 255, 255, 0); fg = Theme.TEXT; line = new Color(255, 255, 255, 60); }
            }
            if (!isEnabled()) g.setComposite(AlphaComposite.SrcOver.derive(0.45f));
            Shape s = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h);
            g.setColor(bg);
            g.fill(s);
            if (line != null) { g.setColor(line); g.setStroke(new BasicStroke(1f)); g.draw(s); }
            FontMetrics fm = g.getFontMetrics(getFont());
            int tw = fm.stringWidth(getText());
            int iw = icon != null ? 18 : 0, gap = icon != null && tw > 0 ? 10 : 0;
            int x = (w - (iw + gap + tw)) / 2;
            if (icon != null) icon.paint(g, x, (h - 18) / 2.0, 18, fg);
            g.setFont(getFont());
            g.setColor(fg);
            g.drawString(getText(), x + iw + gap, (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    /** Nút vuông bo góc chỉ có icon (hamburger, chuông...). */
    static final class IconBtn extends JButton {
        private final Ic icon;
        int badge = 0;

        IconBtn(Ic icon) {
            this.icon = icon;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(54, 54));
        }

        void setBadge(int n) { if (n != badge) { badge = n; repaint(); } }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            Shape s = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 36, 36);
            g.setColor(getModel().isRollover() ? new Color(44, 52, 63) : Theme.CARD);
            g.fill(s);
            g.setColor(Theme.LINE);
            g.draw(s);
            icon.paint(g, (w - 22) / 2.0, (h - 22) / 2.0, 22, Theme.TEXT);
            if (badge > 0) {
                String t = badge > 99 ? "99+" : String.valueOf(badge);
                g.setFont(Theme.bold(11f));
                FontMetrics fm = g.getFontMetrics();
                int bw = Math.max(20, fm.stringWidth(t) + 10);
                int bx = w - bw - 3, by = 3;
                g.setColor(new Color(239, 68, 68));
                g.fillRoundRect(bx, by, bw, 20, 20, 20);
                g.setColor(Color.WHITE);
                g.drawString(t, bx + (bw - fm.stringWidth(t)) / 2, by + (20 - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }

    // =====================================================================
    //  Ô nhập liệu
    // =====================================================================
    static class Field extends JTextField {
        private final String placeholder;
        private final Ic trailing;
        private final boolean leading;
        boolean invalid;

        Field(String placeholder, Ic trailing) { this(placeholder, trailing, false); }

        Field(String placeholder, Ic trailing, boolean leading) {
            this.placeholder = placeholder;
            this.trailing = trailing;
            this.leading = leading;
            setOpaque(false);
            setFont(Theme.regular(15f));
            setForeground(Theme.TEXT);
            setCaretColor(Theme.TEXT);
            setSelectionColor(Theme.alpha(Theme.TEAL, 90));
            setSelectedTextColor(Theme.TEXT);
            setDisabledTextColor(Theme.DIM);
            setBorder(new EmptyBorder(0, leading && trailing != null ? 50 : 18, 0, !leading && trailing != null ? 48 : 18));
            setPreferredSize(new Dimension(240, 48));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
            addKeyListener(new KeyAdapter() {
                @Override public void keyTyped(KeyEvent e) { if (invalid) { invalid = false; repaint(); } }
            });
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            Shape s = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 32, 32);
            g.setColor(Theme.INPUT);
            g.fill(s);
            g.setStroke(new BasicStroke(1.3f));
            g.setColor(invalid ? Theme.alpha(Theme.RED, 220) : isFocusOwner() ? Theme.alpha(Theme.TEAL, 200) : Theme.LINE);
            g.draw(s);
            g.dispose();
            super.paintComponent(gr);
            Graphics2D h = Theme.quality(gr);
            if (getText().isEmpty() && placeholder != null) {
                h.setFont(getFont());
                h.setColor(Theme.DIM);
                FontMetrics fm = h.getFontMetrics();
                h.drawString(placeholder, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            if (trailing != null) trailing.paint(h, leading ? 18 : getWidth() - 36, (getHeight() - 20) / 2.0, 20, Theme.MUTED);
            h.dispose();
        }
    }

    // =====================================================================
    //  Combobox
    // =====================================================================
    static final class Combo extends JComboBox<String> {
        Combo(String... items) {
            super(items);
            setUI(new ComboUI());
            setOpaque(false);
            setFont(Theme.regular(15f));
            setForeground(Theme.TEXT);
            setMaximumRowCount(8);
            setPreferredSize(new Dimension(220, 48));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRenderer(new ListCellRenderer<String>() {
                public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                              boolean selected, boolean focus) {
                    return new JComponent() {
                        @Override public Dimension getPreferredSize() { return new Dimension(100, 40); }

                        @Override protected void paintComponent(Graphics gr) {
                            Graphics2D g = Theme.quality(gr);
                            if (selected) {
                                g.setColor(Theme.alpha(Theme.TEAL, 46));
                                g.fillRoundRect(2, 1, getWidth() - 4, getHeight() - 2, 14, 14);
                            }
                            Theme.textMid(g, Theme.fit(g.getFontMetrics(Theme.regular(15f)), value, getWidth() - 28), 14, 0, getHeight(),
                                    Theme.regular(15f), selected ? Theme.TEAL : Theme.TEXT);
                            g.dispose();
                        }
                    };
                }
            });
        }

        void setItems(List<String> items) {
            Object keep = getSelectedItem();
            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>(items.toArray(new String[0]));
            setModel(m);
            if (keep != null && items.contains(keep)) setSelectedItem(keep);
        }

        boolean sameItems(List<String> items) {
            if (getItemCount() != items.size()) return false;
            for (int i = 0; i < items.size(); i++) if (!items.get(i).equals(getItemAt(i))) return false;
            return true;
        }
    }

    private static final class ComboUI extends BasicComboBoxUI {
        @Override protected JButton createArrowButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setBorder(null);
            b.setFocusable(false);
            b.setContentAreaFilled(false);
            return b;
        }

        @Override public void paint(Graphics gr, JComponent c) {
            Graphics2D g = Theme.quality(gr);
            int w = c.getWidth(), h = c.getHeight();
            Shape s = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 32, 32);
            g.setColor(Theme.INPUT);
            g.fill(s);
            g.setColor(comboBox.isPopupVisible() || comboBox.hasFocus() ? Theme.alpha(Theme.TEAL, 170) : Theme.LINE);
            g.setStroke(new BasicStroke(1.3f));
            g.draw(s);
            Object sel = comboBox.getSelectedItem();
            if (sel != null) {
                Font f = comboBox.getFont();
                String t = Theme.fit(g.getFontMetrics(f), sel.toString(), w - 62);
                Theme.textMid(g, t, 18, 0, h, f, Theme.TEXT);
            }
            Ic.CHEVRON_DOWN.paint(g, w - 34, (h - 16) / 2.0, 16, Theme.TEXT);
            g.dispose();
        }

        @Override protected ComboPopup createPopup() {
            BasicComboPopup p = new BasicComboPopup(comboBox) {
                @Override protected JScrollPane createScroller() {
                    JScrollPane sp = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    sp.setBorder(null);
                    sp.getViewport().setBackground(Theme.POPUP);
                    sp.setBackground(Theme.POPUP);
                    sp.getVerticalScrollBar().setUI(new SlimScrollUI());
                    sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
                    return sp;
                }
            };
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 80, 94), 1),
                    new EmptyBorder(5, 5, 5, 5)));
            p.setOpaque(true);
            p.setBackground(Theme.POPUP);
            p.getList().setBackground(Theme.POPUP);
            p.getList().setForeground(Theme.TEXT);
            return p;
        }
    }

    // =====================================================================
    //  Thanh cuộn mảnh
    // =====================================================================
    static final class SlimScrollUI extends BasicScrollBarUI {
        private boolean hover;

        @Override protected void installListeners() {
            super.installListeners();
            scrollbar.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; scrollbar.repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; scrollbar.repaint(); }
            });
        }

        private JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override protected JButton createDecreaseButton(int o) { return zero(); }

        @Override protected JButton createIncreaseButton(int o) { return zero(); }

        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}

        @Override protected void paintThumb(Graphics gr, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = Theme.quality(gr);
            g.setColor(new Color(255, 255, 255, hover || isDragging ? 90 : 48));
            g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, r.width, r.width);
            g.dispose();
        }
    }

    /** Vùng cuộn trong suốt dùng cho từng trang. */
    static JScrollPane scroll(JComponent view) {
        JScrollPane sp = new JScrollPane(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setViewportBorder(null);
        sp.getVerticalScrollBar().setUI(new SlimScrollUI());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        sp.getVerticalScrollBar().setUnitIncrement(30);
        sp.getVerticalScrollBar().setOpaque(false);
        sp.setWheelScrollingEnabled(true);
        return sp;
    }

    /** Panel cuộn theo bề rộng khung nhìn. */
    static class ScrollBody extends JPanel implements Scrollable {
        ScrollBody() { setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 30; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(60, r.height - 60); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // =====================================================================
    //  Segmented tabs
    // =====================================================================
    static final class Segmented extends JComponent {
        private final List<String> labels = new ArrayList<>();
        private int selected;
        private int hover = -1;
        private final IntConsumer onChange;

        Segmented(String[] items, int selected, IntConsumer onChange) {
            labels.addAll(Arrays.asList(items));
            this.selected = selected;
            this.onChange = onChange;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { int i = indexAt(e.getX()); if (i != hover) { hover = i; repaint(); } }
                @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    int i = indexAt(e.getX());
                    if (i >= 0 && i != Segmented.this.selected) { select(i); Segmented.this.onChange.accept(i); }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        void select(int i) { selected = i; repaint(); }

        int selected() { return selected; }

        void setLabel(int i, String t) {
            if (!labels.get(i).equals(t)) { labels.set(i, t); revalidate(); repaint(); }
        }

        private int itemW(int i) { return getFontMetrics(Theme.semi(14.5f)).stringWidth(labels.get(i)) + 40; }

        private int indexAt(int x) {
            int cx = 5;
            for (int i = 0; i < labels.size(); i++) {
                int w = itemW(i);
                if (x >= cx && x < cx + w) return i;
                cx += w;
            }
            return -1;
        }

        @Override public Dimension getPreferredSize() {
            int w = 10;
            for (int i = 0; i < labels.size(); i++) w += itemW(i);
            return new Dimension(w, 52);
        }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int h = getHeight();
            Shape s = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, h - 1, h, h);
            g.setColor(new Color(17, 22, 28));
            g.fill(s);
            g.setColor(Theme.LINE);
            g.draw(s);
            int x = 5;
            Font f = Theme.semi(14.5f);
            for (int i = 0; i < labels.size(); i++) {
                int w = itemW(i);
                if (i == selected) {
                    g.setColor(new Color(238, 243, 248));
                    g.fillRoundRect(x, 5, w, h - 10, h - 10, h - 10);
                }
                FontMetrics fm = g.getFontMetrics(f);
                Color c = i == selected ? new Color(15, 22, 30) : (i == hover ? Theme.TEXT : Theme.MUTED);
                Theme.textMid(g, labels.get(i), x + (w - fm.stringWidth(labels.get(i))) / 2, 0, h, f, c);
                x += w;
            }
            g.dispose();
        }
    }

    // =====================================================================
    //  Công tắc bật/tắt
    // =====================================================================
    static final class Switch extends JComponent {
        private boolean on;
        private final Consumer<Boolean> onChange;

        Switch(boolean on, Consumer<Boolean> onChange) {
            this.on = on;
            this.onChange = onChange;
            setPreferredSize(new Dimension(60, 34));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { Switch.this.on = !Switch.this.on; repaint(); Switch.this.onChange.accept(Switch.this.on); }
            });
        }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth(), h = getHeight();
            g.setColor(on ? new Color(20, 184, 166) : new Color(70, 78, 90));
            g.fillRoundRect(0, 0, w, h, h, h);
            g.setColor(Color.WHITE);
            int d = h - 8;
            g.fill(new Ellipse2D.Double(on ? w - d - 4 : 4, 4, d, d));
            g.dispose();
        }
    }

    // =====================================================================
    //  Badge (nhãn trạng thái)
    // =====================================================================
    record Badge(String text, Ic icon, Color fg, Color bg) {}

    static int badgeWidth(Badge b) {
        FontMetrics fm = new Canvas().getFontMetrics(Theme.semi(13f));
        return 24 + (b.icon() != null ? 20 : 0) + fm.stringWidth(b.text());
    }

    static int paintBadge(Graphics2D g, int x, int y, Badge b, int h) {
        Font f = Theme.semi(13f);
        FontMetrics fm = g.getFontMetrics(f);
        int w = 24 + (b.icon() != null ? 20 : 0) + fm.stringWidth(b.text());
        g.setColor(b.bg());
        g.fillRoundRect(x, y, w, h, h, h);
        int tx = x + 12;
        if (b.icon() != null) { b.icon().paint(g, tx, y + (h - 15) / 2.0, 15, b.fg(), 2.1f); tx += 20; }
        Theme.textMid(g, b.text(), tx, y, h, f, b.fg());
        return w;
    }

    /** Badge dạng component, dùng ở tiêu đề thẻ. */
    static final class BadgeLabel extends JComponent {
        private Badge badge;
        private final int h;

        BadgeLabel(Badge b, int h) { this.badge = b; this.h = h; }

        void set(Badge b) { if (!b.equals(badge)) { badge = b; revalidate(); repaint(); } }

        @Override public Dimension getPreferredSize() { return new Dimension(badgeWidth(badge), h); }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            paintBadge(g, 0, 0, badge, h);
            g.dispose();
        }
    }

    static Badge online() { return new Badge("Online", Ic.WIFI, Theme.GREEN, Theme.alpha(Theme.GREEN, 36)); }

    static Badge offline() { return new Badge("Offline", Ic.WIFI_OFF, Theme.MUTED, new Color(255, 255, 255, 18)); }

    static Badge warn() { return new Badge("Cảnh báo", Ic.ALERT, Theme.AMBER, Theme.alpha(Theme.AMBER, 36)); }

    static Badge critical() { return new Badge("Vượt ngưỡng", Ic.ALERT, Theme.RED, Theme.alpha(Theme.RED, 40)); }

    static Badge near() { return new Badge("Sắp vượt", Ic.ALERT, Theme.AMBER, Theme.alpha(Theme.AMBER, 36)); }

    // =====================================================================
    //  Nhãn văn bản
    // =====================================================================
    static JLabel label(String s, Font f, Color c) {
        JLabel l = new JLabel(s);
        l.setFont(f);
        l.setForeground(c);
        return l;
    }

    static void setText(JLabel l, String s) { if (!Objects.equals(l.getText(), s)) l.setText(s); }

    static JComponent gap(int w, int h) { return (JComponent) Box.createRigidArea(new Dimension(w, h)); }

    /** Đoạn chữ tự xuống dòng theo bề rộng thực tế (giữ chỗ cố định số dòng). */
    static final class Wrapped extends JComponent {
        private final String text;
        private final Font font;
        private final Color color;
        private final int lines;

        Wrapped(String text, Font font, Color color, int lines) {
            this.text = text; this.font = font; this.color = color; this.lines = lines;
            setAlignmentX(0f);
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(60, lines * (getFontMetrics(font).getHeight() + 2) + 2);
        }

        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            FontMetrics fm = g.getFontMetrics(font);
            int y = fm.getAscent() + 1;
            for (String l : wrap(fm, text, Math.max(40, getWidth()), lines)) {
                Theme.text(g, l, 0, y, font, color);
                y += fm.getHeight() + 2;
            }
            g.dispose();
        }
    }

    /** Ngắt dòng theo bề rộng (tối đa maxLines dòng). */
    static List<String> wrap(FontMetrics fm, String s, int maxW, int maxLines) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
            String t = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(t) <= maxW || cur.length() == 0) cur = new StringBuilder(t);
            else { out.add(cur.toString()); cur = new StringBuilder(word); }
        }
        if (cur.length() > 0) out.add(cur.toString());
        if (out.size() > maxLines) {
            List<String> cut = new ArrayList<>(out.subList(0, maxLines));
            cut.set(maxLines - 1, Theme.fit(fm, cut.get(maxLines - 1) + "…", maxW));
            return cut;
        }
        return out;
    }

    // =====================================================================
    //  WrapLayout: xếp các thành phần theo hàng, tự xuống dòng
    // =====================================================================
    static final class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container t) { return size(t, true); }

        @Override public Dimension minimumLayoutSize(Container t) { return size(t, false); }

        private Dimension size(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int tw = target.getWidth();
                if (tw == 0) tw = Integer.MAX_VALUE / 4;
                Insets in = target.getInsets();
                int max = tw - in.left - in.right - getHgap() * 2;
                Dimension dim = new Dimension(0, 0);
                int rowW = 0, rowH = 0;
                for (Component c : target.getComponents()) {
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (rowW + d.width > max && rowW > 0) {
                        dim.width = Math.max(dim.width, rowW);
                        dim.height += rowH + getVgap();
                        rowW = 0; rowH = 0;
                    }
                    rowW += d.width + getHgap();
                    rowH = Math.max(rowH, d.height);
                }
                dim.width = Math.max(dim.width, rowW);
                dim.height += rowH;
                dim.width += in.left + in.right + getHgap() * 2;
                dim.height += in.top + in.bottom + getVgap() * 2;
                return dim;
            }
        }
    }

    // =====================================================================
    //  Chia cột theo tỉ lệ chính xác (không phụ thuộc kích thước ưa thích)
    // =====================================================================
    static final class Ratio implements LayoutManager {
        private final double[] weights;
        private final int gap;
        private final int minHeight;

        Ratio(int gap, int minHeight, double... weights) { this.gap = gap; this.minHeight = minHeight; this.weights = weights; }

        public void addLayoutComponent(String n, Component c) {}
        public void removeLayoutComponent(Component c) {}

        public Dimension preferredLayoutSize(Container p) {
            int h = minHeight;
            for (Component c : p.getComponents()) h = Math.max(h, c.getPreferredSize().height);
            Insets in = p.getInsets();
            return new Dimension(200, h + in.top + in.bottom);
        }

        public Dimension minimumLayoutSize(Container p) { return new Dimension(100, preferredLayoutSize(p).height); }

        public void layoutContainer(Container p) {
            Insets in = p.getInsets();
            Component[] cs = p.getComponents();
            int n = cs.length;
            double total = 0;
            for (int i = 0; i < n; i++) total += weights[Math.min(i, weights.length - 1)];
            double avail = p.getWidth() - in.left - in.right - gap * (n - 1);
            double x = in.left;
            int h = p.getHeight() - in.top - in.bottom;
            for (int i = 0; i < n; i++) {
                double w = avail * weights[Math.min(i, weights.length - 1)] / total;
                cs[i].setBounds((int) Math.round(x), in.top, (int) Math.round(w), h);
                x += w + gap;
            }
        }
    }

    // =====================================================================
    //  Bảng dữ liệu tự vẽ (cập nhật tại chỗ, không nhấp nháy)
    // =====================================================================
    static final class Grid extends JComponent {
        interface Cell { void paint(Graphics2D g, Object v, int x, int y, int w, int h, int align, Grid grid); }

        record Col(String title, double weight, int align, Cell cell) {}

        record RowStyle(Color tint, Color accent) {}

        record Tag(Color color, String text) {}

        record Two(String a, String b) {}

        static final int LEFT = 0, CENTER = 1, RIGHT = 2;

        private final List<Col> cols;
        private List<Object[]> rows = new ArrayList<>();
        int rowH = 56, headH = 48, padX = 22;
        int hoverRow = -1, hoverIcon = -1;
        IntFunction<RowStyle> style = i -> null;
        Supplier<LocalDateTime> now = LocalDateTime::now;
        BiConsumer<Integer, Integer> onAction;
        Ic[] actionIcons = {};
        String emptyTitle = "Chưa có dữ liệu", emptySub = "";

        Grid(Col... cols) {
            this.cols = List.of(cols);
            setOpaque(false);
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { hover(e.getPoint()); }
                @Override public void mouseExited(MouseEvent e) { hoverRow = -1; hoverIcon = -1; setCursor(Cursor.getDefaultCursor()); repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    int r = rowAt(e.getY()), ic = iconAt(e.getPoint(), r);
                    if (r >= 0 && ic >= 0 && onAction != null) onAction.accept(r, ic);
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        void setRows(List<Object[]> nr) {
            boolean same = nr.size() == rows.size();
            if (same) for (int i = 0; i < nr.size(); i++) if (!Arrays.deepEquals(nr.get(i), rows.get(i))) { same = false; break; }
            if (same) return;
            int old = rows.size();
            rows = nr;
            if (old != nr.size()) { revalidate(); }
            repaint();
        }

        int rowCount() { return rows.size(); }

        private int rowAt(int y) {
            int r = (y - headH) / rowH;
            return y >= headH && r >= 0 && r < rows.size() ? r : -1;
        }

        private double[] colX() {
            double total = 0;
            for (Col c : cols) total += c.weight();
            double avail = getWidth() - padX * 2;
            double[] xs = new double[cols.size() + 1];
            double x = padX;
            for (int i = 0; i < cols.size(); i++) { xs[i] = x; x += avail * cols.get(i).weight() / total; }
            xs[cols.size()] = x;
            return xs;
        }

        private Rectangle actionRect(int row, int idx) {
            double[] xs = colX();
            int last = cols.size() - 1;
            int right = (int) xs[last + 1];
            int bw = 38, gap = 8;
            int n = actionIcons.length;
            int x = right - n * bw - (n - 1) * gap + idx * (bw + gap);
            return new Rectangle(x, headH + row * rowH + (rowH - bw) / 2, bw, bw);
        }

        private int iconAt(Point p, int row) {
            if (row < 0) return -1;
            for (int i = 0; i < actionIcons.length; i++) if (actionRect(row, i).contains(p)) return i;
            return -1;
        }

        private void hover(Point p) {
            int r = rowAt(p.y), ic = iconAt(p, r);
            if (r != hoverRow || ic != hoverIcon) {
                hoverRow = r; hoverIcon = ic;
                setCursor(ic >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                repaint();
            }
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(600, headH + (rows.isEmpty() ? 150 : rows.size() * rowH) + 6);
        }

        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            int w = getWidth();
            double[] xs = colX();
            Font hf = Theme.semi(13.5f);
            for (int i = 0; i < cols.size(); i++) {
                Col c = cols.get(i);
                align(g, c.title(), (int) xs[i], 0, (int) (xs[i + 1] - xs[i]) - 8, headH, c.align(), hf, Theme.MUTED);
            }
            g.setColor(new Color(255, 255, 255, 16));
            g.fillRect(8, headH - 1, w - 16, 1);
            for (int r = 0; r < rows.size(); r++) {
                int y = headH + r * rowH;
                RowStyle rs = style.apply(r);
                if (rs != null && rs.tint() != null) {
                    g.setColor(rs.tint());
                    g.fillRect(8, y, w - 16, rowH);
                }
                if (r == hoverRow) {
                    g.setColor(new Color(255, 255, 255, 11));
                    g.fillRect(8, y, w - 16, rowH);
                }
                if (rs != null && rs.accent() != null) {
                    g.setColor(rs.accent());
                    g.fillRect(8, y, 4, rowH);
                }
                if (r < rows.size() - 1) {
                    g.setColor(new Color(255, 255, 255, 12));
                    g.fillRect(8, y + rowH - 1, w - 16, 1);
                }
                Object[] row = rows.get(r);
                for (int i = 0; i < cols.size(); i++) {
                    Col c = cols.get(i);
                    int cx = (int) xs[i], cw = (int) (xs[i + 1] - xs[i]) - 8;
                    Graphics2D cg = (Graphics2D) g.create(cx - 2, y, cw + 8, rowH);
                    cg.translate(-(cx - 2), -y);
                    c.cell().paint(cg, row[i], cx, y, cw, rowH, c.align(), this);
                    cg.dispose();
                }
                if (actionIcons.length > 0) {
                    for (int i = 0; i < actionIcons.length; i++) {
                        Rectangle b = actionRect(r, i);
                        boolean hv = r == hoverRow && i == hoverIcon;
                        boolean danger = actionIcons[i] == Ic.TRASH;
                        g.setColor(hv ? (danger ? Theme.alpha(Theme.RED, 50) : new Color(255, 255, 255, 34)) : new Color(255, 255, 255, 14));
                        g.fillRoundRect(b.x, b.y, b.width, b.height, 14, 14);
                        g.setColor(new Color(255, 255, 255, 14));
                        g.drawRoundRect(b.x, b.y, b.width - 1, b.height - 1, 14, 14);
                        actionIcons[i].paint(g, b.x + (b.width - 18) / 2.0, b.y + (b.height - 18) / 2.0, 18,
                                hv ? (danger ? Theme.RED : Theme.TEXT) : Theme.MUTED);
                    }
                }
            }
            if (rows.isEmpty()) {
                int cy = headH + 62;
                Font tf = Theme.bold(16f), sf = Theme.regular(14f);
                FontMetrics f1 = g.getFontMetrics(tf), f2 = g.getFontMetrics(sf);
                Theme.text(g, emptyTitle, (w - f1.stringWidth(emptyTitle)) / 2, cy, tf, Theme.TEXT);
                if (!emptySub.isEmpty()) Theme.text(g, emptySub, (w - f2.stringWidth(emptySub)) / 2, cy + 26, sf, Theme.MUTED);
            }
            g.dispose();
        }

        // ---- cell painters ----
        static void align(Graphics2D g, String s, int x, int y, int w, int h, int align, Font f, Color c) {
            FontMetrics fm = g.getFontMetrics(f);
            String t = Theme.fit(fm, s, w);
            int tx = align == RIGHT ? x + w - fm.stringWidth(t) : align == CENTER ? x + (w - fm.stringWidth(t)) / 2 : x;
            Theme.textMid(g, t, tx, y, h, f, c);
        }

        static Cell text(Font f, Function<Object, Color> color) {
            return (g, v, x, y, w, h, al, grid) -> align(g, String.valueOf(v), x, y, w, h, al, f, color.apply(v));
        }

        static Cell text(Font f, Color c) { return text(f, v -> c); }

        static Cell two() {
            return (g, v, x, y, w, h, al, grid) -> {
                Two t = (Two) v;
                boolean sub = t.b() != null && !t.b().isEmpty();
                align(g, t.a(), x, y - (sub ? 10 : 0), w, h, al, Theme.bold(15f), Theme.TEXT);
                if (sub) align(g, t.b(), x, y + 11, w, h, al, Theme.regular(13f), Theme.MUTED);
            };
        }

        /** Văn bản có màu riêng cho từng ô (value là Tag). */
        static Cell colored(Font f) {
            return (g, v, x, y, w, h, al, grid) -> { Tag t = (Tag) v; align(g, t.text(), x, y, w, h, al, f, t.color()); };
        }

        static Cell tag() {
            return (g, v, x, y, w, h, al, grid) -> {
                Tag t = (Tag) v;
                g.setColor(t.color());
                g.fill(new Ellipse2D.Double(x + 2, y + h / 2.0 - 5, 10, 10));
                align(g, t.text(), x + 26, y, w - 26, h, LEFT, Theme.bold(15f), Theme.TEXT);
            };
        }

        static Cell badge() {
            return (g, v, x, y, w, h, al, grid) -> {
                Badge b = (Badge) v;
                int bw = badgeWidth(b);
                paintBadge(g, al == RIGHT ? x + w - bw : x, y + (h - 32) / 2, b, 32);
            };
        }

        /** Hai dòng: giờ và "x phút trước" (tính lúc vẽ nên luôn đúng mà không cần cập nhật bảng). */
        static Cell timeAgo() {
            return (g, v, x, y, w, h, al, grid) -> {
                LocalDateTime t = (LocalDateTime) v;
                align(g, Model.hms(t), x, y - 10, w, h, al, Theme.semi(14.5f), Theme.TEXT);
                align(g, Model.ago(t, grid.now.get()), x, y + 11, w, h, al, Theme.regular(13f), Theme.MUTED);
            };
        }

        static Cell blank() { return (g, v, x, y, w, h, al, grid) -> {}; }
    }

    // =====================================================================
    //  Thông báo nổi (toast)
    // =====================================================================
    enum ToastKind { SUCCESS, ERROR, WARN, INFO }

    static final class Toasts extends JComponent {
        private static final class T {
            String title, msg;
            ToastKind kind;
            long born = System.currentTimeMillis();
            int life = 4600;
            Rectangle bounds = new Rectangle();
        }

        private final List<T> list = new ArrayList<>();
        private final javax.swing.Timer timer;
        
        Toasts() {
            setOpaque(false);
            timer = new javax.swing.Timer(30, e -> tick());
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    for (T t : new ArrayList<>(list)) if (t.bounds.contains(e.getPoint())) t.born = System.currentTimeMillis() - t.life + 250;
                }
            });
        }

        @Override public boolean contains(int x, int y) {
            for (T t : list) if (t.bounds.contains(x, y)) return true;
            return false;
        }

        void show(String title, String msg, ToastKind kind) {
            T t = new T();
            t.title = title; t.msg = msg; t.kind = kind;
            t.life = Math.min(11000, 4500 + (msg == null ? 0 : msg.length()) * 38 + (kind == ToastKind.ERROR || kind == ToastKind.WARN ? 1500 : 0));
            list.add(0, t);
            if (list.size() > 4) list.remove(list.size() - 1);
            timer.start();
            repaint();
        }

        private void tick() {
            long now = System.currentTimeMillis();
            list.removeIf(t -> now - t.born > t.life);
            if (list.isEmpty()) timer.stop();
            repaint();
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = Theme.quality(gr);
            long now = System.currentTimeMillis();
            int tw = 420, y = 92;
            Font tf = Theme.bold(15f), mf = Theme.regular(13.5f);
            for (T t : list) {
                long age = now - t.born;
                float a = (float) Math.min(1, Math.min(age / 220.0, (t.life - age) / 320.0));
                a = Math.max(0, a);
                List<String> lines = wrap(g.getFontMetrics(mf), t.msg == null ? "" : t.msg, tw - 100, 5);
                int th = 30 + 22 + lines.size() * 19 + (lines.isEmpty() ? -10 : 0);
                int x = getWidth() - tw - 30 + (int) ((1 - a) * 24);
                t.bounds.setBounds(x, y, tw, th);
                Graphics2D c = (Graphics2D) g.create();
                c.setComposite(AlphaComposite.SrcOver.derive(a));
                c.setColor(new Color(0, 0, 0, 90));
                c.fillRoundRect(x + 2, y + 6, tw, th, 26, 26);
                c.setColor(new Color(44, 52, 63));
                c.fillRoundRect(x, y, tw, th, 26, 26);
                Color k = switch (t.kind) { case SUCCESS -> Theme.GREEN; case ERROR -> Theme.RED; case WARN -> Theme.AMBER; default -> Theme.CYAN; };
                Ic ic = switch (t.kind) { case SUCCESS -> Ic.CHECK_CIRCLE; case ERROR, WARN -> Ic.ALERT; default -> Ic.INFO; };
                c.setColor(Theme.alpha(k, 40));
                c.fill(new Ellipse2D.Double(x + 18, y + 16, 40, 40));
                ic.paint(c, x + 27, y + 25, 22, k);
                c.setColor(Theme.alpha(k, 150));
                c.setStroke(new BasicStroke(1.2f));
                c.drawRoundRect(x, y, tw - 1, th - 1, 26, 26);
                Theme.text(c, Theme.fit(c.getFontMetrics(tf), t.title, tw - 100), x + 72, y + 34, tf, Theme.TEXT);
                int ly = y + 56;
                for (String s : lines) { Theme.text(c, s, x + 72, ly, mf, Theme.MUTED); ly += 19; }
                c.dispose();
                y += th + 12;
            }
            g.dispose();
        }
    }

    // =====================================================================
    //  Hộp thoại (glass pane)
    // =====================================================================
    static final class Modal extends JPanel {
        private Runnable onEnter;

        Modal() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            MouseAdapter eat = new MouseAdapter() {};
            addMouseListener(eat);
            addMouseMotionListener(eat);
            addMouseWheelListener(e -> {});
            registerKeyboardAction(e -> close(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW);
            registerKeyboardAction(e -> { if (onEnter != null) onEnter.run(); },
                    KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), WHEN_IN_FOCUSED_WINDOW);
        }

        void open(JComponent card, Runnable enter, Component focus) {
            removeAll();
            add(card, new GridBagConstraints());
            onEnter = enter;
            setVisible(true);
            revalidate();
            repaint();
            SwingUtilities.invokeLater(() -> (focus != null ? focus : this).requestFocusInWindow());
        }

        void close() {
            setVisible(false);
            removeAll();
            onEnter = null;
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(4, 8, 12, 165));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /** Thẻ hộp thoại chuẩn: tiêu đề, nội dung, hàng nút. */
    static Card dialogCard(String title, String subtitle, JComponent body, JComponent... buttons) {
        Card c = new Card(new BorderLayout(0, 18)).radius(26).fill(new Color(36, 43, 53)).border(Theme.LINE2);
        c.pad(28, 30, 26, 30);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(label(title, Theme.bold(21f), Theme.TEXT));
        if (subtitle != null) { head.add(gap(0, 6)); head.add(label("<html><body style='width:340px'>" + subtitle + "</body></html>", Theme.regular(14f), Theme.MUTED)); }
        c.add(head, BorderLayout.NORTH);
        if (body != null) c.add(body, BorderLayout.CENTER);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        row.setOpaque(false);
        for (JComponent b : buttons) row.add(b);
        c.add(row, BorderLayout.SOUTH);
        c.setPreferredSize(new Dimension(450, c.getPreferredSize().height));
        return c;
    }
}
