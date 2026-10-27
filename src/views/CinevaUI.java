package views;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;

/**
 * Small shared kit for the booking-flow screens (movie details, seats, payment), so they all look
 * like the home page: black page, hairline outlines, pill buttons, rounded fields, thin tracked text.
 */
final class CinevaUI {

    static final Color BG = CustomerHomeFrame.BG;
    static final Color TEXT = CustomerHomeFrame.TEXT;
    static final Color DIM = CustomerHomeFrame.DIM;
    static final Color LINE = CustomerHomeFrame.LINE;
    static final Color FIELD = new Color(14, 14, 18);

    static final int OUTLINE = 0, SOLID = 1, DANGER = 2;

    private CinevaUI() {
    }

    // ------------------------------------------------------------------ buttons
    /** A normal JButton (so enable/disable, default button and listeners all keep working) drawn as a pill. */
    static JButton pill(String text, int style) {
        JButton b = new JButton(text);
        b.setUI(new PillUI(style));
        return b;
    }

    static final class PillUI extends BasicButtonUI {
        private final int style;

        PillUI(int style) {
            this.style = style;
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(false);
            b.setBorder(new EmptyBorder(0, 0, 0, 0));
            b.setRolloverEnabled(true);
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        private static String label(AbstractButton b) {
            return b.getText() == null ? "" : b.getText().toUpperCase();
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            FontMetrics fm = c.getFontMetrics(CustomerHomeFrame.Fonts.get(true, 11.5f));
            int w = Math.round(CustomerHomeFrame.trackedWidth(fm, label((AbstractButton) c), 1.4f)) + 52;
            return new Dimension(Math.max(96, w), 40);
        }

        @Override
        public Dimension getMinimumSize(JComponent c) {
            return getPreferredSize(c);
        }

        @Override
        public Dimension getMaximumSize(JComponent c) {
            return getPreferredSize(c);
        }

        @Override
        public void paint(Graphics g0, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            int w = c.getWidth();
            int h = c.getHeight();
            boolean on = b.isEnabled();
            boolean hover = on && b.getModel().isRollover();
            boolean down = on && b.getModel().isArmed() && b.getModel().isPressed();
            RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h);
            Color fg;
            if (style == SOLID) {
                g.setColor(!on ? new Color(38, 40, 46) : (hover || down ? new Color(222, 224, 230) : Color.WHITE));
                g.fill(r);
                fg = on ? new Color(12, 12, 14) : new Color(104, 107, 116);
            } else {
                boolean bad = style == DANGER;
                Color edge = !on ? new Color(48, 50, 58)
                        : hover ? (bad ? new Color(235, 100, 110) : new Color(190, 194, 204))
                        : (bad ? new Color(120, 60, 64) : new Color(88, 92, 102));
                fg = !on ? new Color(90, 93, 102) : (bad ? new Color(238, 120, 128) : (hover ? Color.WHITE : TEXT));
                if (down) {
                    g.setColor(new Color(255, 255, 255, 22));
                    g.fill(r);
                }
                g.setColor(edge);
                g.setStroke(new BasicStroke(1f));
                g.draw(r);
            }
            g.setFont(CustomerHomeFrame.Fonts.get(true, 11.5f));
            FontMetrics fm = g.getFontMetrics();
            String t = label(b);
            g.setColor(fg);
            float tw = CustomerHomeFrame.trackedWidth(fm, t, 1.4f);
            CustomerHomeFrame.drawTracked(g, t, (w - tw) / 2f, (h + fm.getAscent() - fm.getDescent()) / 2f, 1.4f);
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ text fields
    private static void paintBox(Graphics g0, JComponent c, boolean focused) {
        Graphics2D g = (Graphics2D) g0.create();
        CustomerHomeFrame.aa(g);
        int w = c.getWidth();
        int h = c.getHeight();
        g.setColor(FIELD);
        g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 14, 14));
        g.setColor(focused ? new Color(150, 154, 164) : LINE);
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 14, 14));
        g.dispose();
    }

    static final class RoundField extends JTextField {
        private boolean focused;

        RoundField(int cols) {
            super(cols);
            setOpaque(false);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            setFont(CustomerHomeFrame.Fonts.get(false, 14f));
            setForeground(TEXT);
            setCaretColor(TEXT);
            Dimension d = getPreferredSize();
            setPreferredSize(new Dimension(d.width, 40));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintBox(g, this, focused);
            super.paintComponent(g);
        }
    }

    static final class RoundPassword extends JPasswordField {
        private boolean focused;

        RoundPassword(int cols) {
            super(cols);
            setOpaque(false);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            setFont(CustomerHomeFrame.Fonts.get(false, 14f));
            setForeground(TEXT);
            setCaretColor(TEXT);
            Dimension d = getPreferredSize();
            setPreferredSize(new Dimension(d.width, 40));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintBox(g, this, focused);
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------------ text
    /**
     * Single-line tracked text, measured with the same fractional metrics it is painted with (so the
     * last letter is never clipped). With shrink on it scales down to fit the width it is given.
     */
    static final class TrackedText extends JComponent {
        private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
        private static final int PAD = 6;
        private final String text;
        private final Font base;
        private final float maxSize;
        private final float trackEm;
        private final Color color;
        private final boolean shrink;

        TrackedText(String text, String family, int style, float size, Color color, float trackEm, boolean shrink) {
            this.text = text;
            this.maxSize = size;
            this.color = color;
            this.trackEm = trackEm;
            this.shrink = shrink;
            this.base = new Font(family, style, 12);
            FontMetrics fm = getFontMetrics(base.deriveFont(size));
            setPreferredSize(new Dimension(Math.round(width(size)) + 2 * PAD, fm.getHeight()));
            setMinimumSize(new Dimension(40, fm.getHeight()));
            setMaximumSize(getPreferredSize());          // so BoxLayout alignment (centre / left) works
        }

        /** Thin display title (Segoe UI Light). */
        static TrackedText light(String text, float size, Color color, float trackEm) {
            return new TrackedText(text, "Segoe UI Light", Font.PLAIN, size, color, trackEm, true);
        }

        /** Small uppercase caption. */
        static TrackedText caps(String text, Color color) {
            return new TrackedText(text.toUpperCase(), "Segoe UI", Font.BOLD, 10.5f, color, 0.14f, false);
        }

        private float width(float size) {
            Font f = base.deriveFont(size);
            float w = 0;
            float gap = trackEm * size;
            for (int i = 0; i < text.length(); i++) {
                w += (float) f.getStringBounds(text.substring(i, i + 1), FRC).getWidth() + (i < text.length() - 1 ? gap : 0);
            }
            return w;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            float size = maxSize;
            if (shrink) {
                float avail = getWidth() - 2 * PAD;
                float w = width(maxSize);
                if (w > avail) {
                    size = Math.max(12f, maxSize * avail / w);
                }
            }
            Font f = base.deriveFont(size);
            g.setFont(f);
            g.setColor(color);
            FontMetrics fm = g.getFontMetrics();
            float gap = trackEm * size;
            float x = 0;
            float y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2f;
            for (int i = 0; i < text.length(); i++) {
                String ch = text.substring(i, i + 1);
                g.drawString(ch, x, y);
                x += (float) f.getStringBounds(ch, FRC).getWidth() + gap;
            }
            g.dispose();
        }
    }

    /** Horizontal row of chips, flush with the left edge of whatever it sits under. */
    static final class ChipRow extends JPanel {
        ChipRow() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Component add(Component c) {
            if (getComponentCount() > 0) {
                super.add(Box.createHorizontalStrut(8));
            }
            return super.add(c);
        }
    }

    /** Small outlined tag, e.g. "SCI-FI" or "169 MIN". */
    static final class Chip extends JComponent {
        private final String text;
        private final Color color;

        Chip(String text, Color color) {
            this.text = text.toUpperCase();
            this.color = color;
            FontMetrics fm = getFontMetrics(CustomerHomeFrame.Fonts.get(true, 10.5f));
            setPreferredSize(new Dimension(Math.round(CustomerHomeFrame.trackedWidth(fm, this.text, 1.3f)) + 28, 28));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 18));
            g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, h, h));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));
            g.setFont(CustomerHomeFrame.Fonts.get(true, 10.5f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(color);
            float tw = CustomerHomeFrame.trackedWidth(fm, text, 1.3f);
            CustomerHomeFrame.drawTracked(g, text, (w - tw) / 2f, (h + fm.getAscent() - fm.getDescent()) / 2f, 1.3f);
            g.dispose();
        }
    }

    /** Rounded dark card. */
    static class RoundPanel extends JPanel {
        private final Color fill;
        private final Color edge;
        private final int arc;

        RoundPanel(LayoutManager lm, Color fill, Color edge, int arc) {
            super(lm);
            this.fill = fill;
            this.edge = edge;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc));
            g.setColor(edge);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, arc, arc));
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Rounded segmented switch (white pill on the selected segment). */
    static final class Switch extends JComponent {
        private final String[] labels;
        private final IntConsumer onChange;
        private int selected;
        private int hover = -1;

        Switch(String[] labels, IntConsumer onChange) {
            this.labels = labels;
            this.onChange = onChange;
            setPreferredSize(new Dimension(labels.length * 150 + 6, 42));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { hover = indexAt(e.getX()); repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    int i = indexAt(e.getX());
                    if (i >= 0 && i != selected) {
                        selected = i;
                        repaint();
                        Switch.this.onChange.accept(i);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private int indexAt(int x) {
            int seg = (getWidth() - 6) / labels.length;
            int i = (x - 3) / Math.max(1, seg);
            return (i < 0 || i >= labels.length) ? -1 : i;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(LINE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));
            int seg = (w - 6) / labels.length;
            g.setFont(CustomerHomeFrame.Fonts.get(true, 11.5f));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < labels.length; i++) {
                float sx = 3 + i * seg;
                if (i == selected) {
                    g.setColor(Color.WHITE);
                    g.fill(new RoundRectangle2D.Float(sx, 3, seg, h - 6, h, h));
                }
                g.setColor(i == selected ? new Color(12, 12, 14) : (hover == i ? TEXT : DIM));
                String t = labels[i].toUpperCase();
                float tw = CustomerHomeFrame.trackedWidth(fm, t, 1.5f);
                CustomerHomeFrame.drawTracked(g, t, sx + (seg - tw) / 2f, (h + fm.getAscent() - fm.getDescent()) / 2f, 1.5f);
            }
            g.dispose();
        }
    }

    /** Screen size for dialogs: never taller or wider than the usable desktop (Windows display scaling). */
    static Dimension fit(int w, int h) {
        Rectangle s = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        return new Dimension(Math.min(w, s.width - 40), Math.min(h, s.height - 40));
    }
}
