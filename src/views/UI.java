package views;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Shared look-and-feel pieces for the Cineva screens (same design as LoginFrame):
 * mosaic background, rounded panels, line icons, buttons and small helpers.
 */
final class UI {

    private UI() { }

    // ---------- Palette ----------
    static final Color CARD         = new Color(15, 17, 21, 226);
    static final Color CARD_BORDER  = new Color(40, 43, 51);
    static final Color FOOTER_BG    = new Color(11, 12, 15);
    static final Color FIELD_BG     = new Color(27, 30, 38);
    static final Color FIELD_BORDER = new Color(45, 49, 59);
    static final Color FIELD_FOCUS  = new Color(120, 126, 142);
    static final Color TEXT         = new Color(243, 244, 246);
    static final Color MUTED        = new Color(139, 145, 158);

    // ---------- helpers ----------
    static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    static <T extends JComponent> T centered(T c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }

    static void fixSize(JComponent c, int w, int h) {
        Dimension d = new Dimension(w, h);
        c.setPreferredSize(d);
        c.setMinimumSize(d);
        c.setMaximumSize(d);
    }

    static void aa(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static JPanel labelRow(String text, JComponent trailing, int width) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 14));
        l.setForeground(new Color(225, 228, 233));
        row.add(l, BorderLayout.WEST);
        if (trailing != null) {
            row.add(trailing, BorderLayout.EAST);
        }
        fixSize(row, width, 22);
        return row;
    }

    static JButton linkButton(String text, Font f, Color c, boolean underline) {
        JButton b = new LinkButton(text, underline);
        b.setFont(f);
        b.setForeground(c);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setBorder(new EmptyBorder(0, 0, 0, 0));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Rounded input box with a left icon, the text field in the middle and an optional right component. */
    static RoundPanel buildField(IconType icon, JTextField input, JComponent trailing, int width) {
        final RoundPanel p = new RoundPanel(16, FIELD_BG, FIELD_BORDER);
        p.setLayout(new BorderLayout());
        p.setBorder(new EmptyBorder(2, 0, 2, 10));
        p.add(new IconComponent(icon, MUTED, 46, 40), BorderLayout.WEST);

        input.setFont(font(Font.PLAIN, 15));
        input.setForeground(TEXT);
        input.setCaretColor(TEXT);
        input.setBackground(FIELD_BG);
        input.setOpaque(false);
        input.setBorder(new EmptyBorder(0, 0, 0, 8));
        input.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                p.setBorderColor(FIELD_FOCUS);
            }

            @Override
            public void focusLost(FocusEvent e) {
                p.setBorderColor(FIELD_BORDER);
            }
        });
        p.add(input, BorderLayout.CENTER);
        if (trailing != null) {
            p.add(trailing, BorderLayout.EAST);
        }
        fixSize(p, width, 46);
        return p;
    }

    // =====================================================================
    //  Components
    // =====================================================================

    /** Rounded panel with a fill and a 1px border. The shape is cached (no per-frame allocation). */
    static class RoundPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private Color border;
        private boolean clipChildren = false;
        private final RoundRectangle2D.Float shape = new RoundRectangle2D.Float();
        private int shapeW = -1;
        private int shapeH = -1;

        RoundPanel(int radius, Color fill, Color border) {
            this.radius = radius;
            this.fill = fill;
            this.border = border;
            setOpaque(false);
        }

        RoundPanel clipChildren() {
            this.clipChildren = true;
            return this;
        }

        void setBorderColor(Color c) {
            this.border = c;
            repaint();
        }

        private RoundRectangle2D shape() {
            if (shapeW != getWidth() || shapeH != getHeight()) {
                shapeW = getWidth();
                shapeH = getHeight();
                shape.setRoundRect(0, 0, shapeW - 1, shapeH - 1, radius, radius);
            }
            return shape;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            Object old = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(shape());
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    old == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : old);
        }

        @Override
        protected void paintChildren(Graphics g) {
            if (!clipChildren) {
                super.paintChildren(g);
                return;
            }
            Shape oldClip = g.getClip();
            ((Graphics2D) g).clip(shape());
            super.paintChildren(g);
            g.setClip(oldClip);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            Object old = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(border);
            g2.draw(shape());
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    old == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : old);
        }
    }

    /** Card footer strip with its own rounded bottom corners. */
    static class FooterPanel extends JPanel {
        private final Color fill;
        private final Color topLine;
        private final int arc;
        private final RoundRectangle2D.Float shape = new RoundRectangle2D.Float();
        private int shapeW = -1;
        private int shapeH = -1;

        FooterPanel(Color fill, Color topLine, int arc) {
            this.fill = fill;
            this.topLine = topLine;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();
            if (w != shapeW || h != shapeH) {
                shapeW = w;
                shapeH = h;
                shape.setRoundRect(1, 0, w - 2, h - 1, arc, arc);
            }
            Object old = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(shape);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    old == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : old);
            g2.fillRect(1, 0, w - 2, arc / 2 + 1);
            g2.setColor(topLine);
            g2.fillRect(1, 0, w - 2, 1);
        }
    }

    /** Letter-spaced text drawn by hand. */
    static class TrackedLabel extends JComponent {
        private final String text;
        private final Color color;
        private final int gap;

        TrackedLabel(String text, Font f, Color color, int gap) {
            this.text = text;
            this.color = color;
            this.gap = gap;
            setFont(f);
            FontMetrics fm = getFontMetrics(f);
            int w = 0;
            for (int i = 0; i < text.length(); i++) {
                w += fm.charWidth(text.charAt(i)) + (i < text.length() - 1 ? gap : 0);
            }
            Dimension d = new Dimension(w + 10, fm.getHeight());   // extra room so the last letter isn't clipped
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            g2.setColor(color);
            FontMetrics fm = g2.getFontMetrics();
            int x = 5;
            int y = fm.getAscent();
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                g2.drawString(String.valueOf(ch), x, y);
                x += fm.charWidth(ch) + gap;
            }
        }
    }

    /** Text button whose underline is drawn by hand. */
    static class LinkButton extends JButton {
        private final boolean underline;

        LinkButton(String text, boolean underline) {
            super(text);
            this.underline = underline;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(getText());
            int x = (getWidth() - tw) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            if (underline) {
                g2.fillRect(x, y + 2, tw, 1);
            }
        }
    }

    enum IconType { TV, MAIL, LOCK, USER }

    /** Line icons drawn with Java2D (no image files needed). */
    static class IconComponent extends JComponent {
        private final IconType type;
        private final Color color;

        IconComponent(IconType type, Color color, int w, int h) {
            this.type = type;
            this.color = color;
            Dimension d = new Dimension(w, h);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;
            switch (type) {
                case MAIL:
                    g2.draw(new RoundRectangle2D.Double(cx - 9, cy - 6.5, 18, 13, 4, 4));
                    Path2D flap = new Path2D.Double();
                    flap.moveTo(cx - 8, cy - 4.5);
                    flap.lineTo(cx, cy + 1.5);
                    flap.lineTo(cx + 8, cy - 4.5);
                    g2.draw(flap);
                    break;
                case LOCK:
                    g2.draw(new RoundRectangle2D.Double(cx - 8, cy - 1, 16, 11, 4, 4));
                    g2.draw(new Arc2D.Double(cx - 5, cy - 9, 10, 14, 0, 180, Arc2D.OPEN));
                    break;
                case USER:
                    g2.draw(new Ellipse2D.Double(cx - 4.5, cy - 9, 9, 9));
                    g2.draw(new Arc2D.Double(cx - 8, cy + 1, 16, 16, 0, 180, Arc2D.OPEN));
                    break;
                case TV:
                    g2.draw(new RoundRectangle2D.Double(cx - 12, cy - 7, 24, 19, 6, 6));
                    g2.draw(new Line2D.Double(cx - 5, cy - 12, cx, cy - 7));
                    g2.draw(new Line2D.Double(cx + 5, cy - 12, cx, cy - 7));
                    g2.draw(new Ellipse2D.Double(cx - 3.5, cy - 0.5, 7, 7));
                    break;
            }
            g2.dispose();
        }
    }

    /** Small white status dot for the footer. */
    static class DotComponent extends JComponent {
        DotComponent() {
            Dimension d = new Dimension(8, 8);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(0, 0, 7, 7));
            g2.dispose();
        }
    }

    /** Show / hide password eye. */
    static class EyeButton extends JButton {
        private boolean revealed = false;

        EyeButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFocusable(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            Dimension d = new Dimension(40, 40);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        void setRevealed(boolean revealed) {
            this.revealed = revealed;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(MUTED);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;
            Path2D eye = new Path2D.Double();
            eye.moveTo(cx - 10, cy);
            eye.quadTo(cx, cy - 11, cx + 10, cy);
            eye.quadTo(cx, cy + 11, cx - 10, cy);
            g2.draw(eye);
            g2.draw(new Ellipse2D.Double(cx - 3, cy - 3, 6, 6));
            if (revealed) {
                g2.draw(new Line2D.Double(cx - 9, cy + 8, cx + 9, cy - 8));
            }
            g2.dispose();
        }
    }

    /** White gradient pill button. */
    static class PrimaryButton extends JButton {
        private boolean hover = false;

        PrimaryButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth();
            int h = getHeight();
            Color top = hover ? Color.WHITE : new Color(250, 250, 251);
            Color bottom = hover ? new Color(236, 238, 242) : new Color(226, 228, 234);
            if (getModel().isPressed()) {
                top = new Color(225, 227, 232);
                bottom = new Color(210, 213, 220);
            }
            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 24, 24));
            g2.setColor(new Color(15, 17, 21));
            g2.setFont(font(Font.BOLD, 17));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    /**
     * Animated mosaic background: a slow silver wave seen through ribbed (fluted) glass and
     * cut into small square tiles with a thin dark grid. Identical to the one in LoginFrame.
     */
    static class MosaicPanel extends JPanel {
        // ---- look (tweak these) ----
        private static final int CELL = 5;
        private static final float RIBS = 24f;
        private static final float LENS = 4.2f;
        private static final float SLOPE = 0.95f;
        private static final float BANDS = 1.50f;
        private static final float SPEED = 0.55f;
        private static final float SLOPE2 = 0.60f;
        private static final float BANDS2 = 3.00f;
        private static final float SPEED2 = -0.40f;
        private static final float FRINGE = 0.0012f;
        private static final float RIB_JITTER = 0.10f;
        private static final float PEAK = 0.74f;
        private static final float FLOOR = 0.04f;
        private static final float[] TINT = {0.96f, 0.97f, 1.00f};
        private static final int FRAME_MS = 33;

        private static final float TWO_PI = 6.2831855f;

        private static float smooth(float e0, float e1, float x) {
            float t = (x - e0) / (e1 - e0);
            t = t < 0f ? 0f : (t > 1f ? 1f : t);
            return t * t * (3f - 2f * t);
        }

        private final javax.swing.Timer timer;
        private final long startNanos = System.nanoTime();

        private BufferedImage canvas;
        private int[] px;
        private int cols = -1;
        private int rows = -1;
        private int canvasW;

        private float[][] colUU;
        private float[][] colEdge;
        private float[][] colAdd;
        private float[] colJit;
        private float[] rowV;

        private float[][] colSinB, colCosB, colSinB2, colCosB2;
        private float[] rowSinA, rowCosA, rowSinA2, rowCosA2;
        private float[] colEnv, rowEnv;
        private final float[] g = new float[3];

        MosaicPanel() {
            setOpaque(true);
            timer = new javax.swing.Timer(FRAME_MS, e -> {
                update();
                repaint();
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, 980), d.height + 30);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            timer.start();
        }

        @Override
        public void removeNotify() {
            timer.stop();
            super.removeNotify();
        }

        private void rebuild(int w, int h) {
            cols = w / CELL + 1;
            rows = h / CELL + 1;
            canvasW = cols * CELL;
            canvas = new BufferedImage(canvasW, rows * CELL, BufferedImage.TYPE_INT_RGB);
            px = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

            colUU = new float[3][cols];
            colEdge = new float[3][cols];
            colAdd = new float[3][cols];
            colJit = new float[cols];
            colSinB = new float[3][cols];
            colCosB = new float[3][cols];
            colSinB2 = new float[3][cols];
            colCosB2 = new float[3][cols];
            float[] disp = {-FRINGE, 0f, FRINGE};
            for (int k = 0; k < 3; k++) {
                for (int c = 0; c < cols; c++) {
                    float u = (c + 0.5f) / cols;
                    float sx = (u + disp[k]) * RIBS;
                    float cell = (float) Math.floor(sx);
                    float lens = (sx - cell) - 0.5f;
                    colUU[k][c] = (cell + 0.5f) / RIBS + lens * LENS / RIBS;
                    if (k == 1) {
                        int hsh = (int) cell * 0x9E3779B1;
                        hsh ^= hsh >>> 15;
                        hsh *= 0x85EBCA6B;
                        hsh ^= hsh >>> 13;
                        colJit[c] = (((hsh & 0xFFFF) / 65535f) - 0.5f) * 2f * RIB_JITTER;
                    }
                    float zl = smooth(-0.30f, -0.46f, lens);
                    float zr = smooth(0.30f, 0.46f, lens);
                    colEdge[k][c] = 1f - 0.55f * Math.max(zl, zr);
                    colAdd[k][c] = k == 0 ? 0.075f * zl : (k == 1 ? 0.03f * zl + 0.03f * zr : 0.085f * zr);
                }
            }
            rowV = new float[rows];
            rowSinA = new float[rows];
            rowCosA = new float[rows];
            rowSinA2 = new float[rows];
            rowCosA2 = new float[rows];
            colEnv = new float[cols];
            rowEnv = new float[rows];
            for (int r = 0; r < rows; r++) {
                rowV[r] = (r + 0.5f) / rows;
            }
        }

        private void update() {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            if (canvas == null || cols != w / CELL + 1 || rows != h / CELL + 1) {
                rebuild(w, h);
            }
            float t = (System.nanoTime() - startNanos) / 1e9f;

            for (int k = 0; k < 3; k++) {
                for (int c = 0; c < cols; c++) {
                    float uu = colUU[k][c];
                    float ux = -SLOPE * uu + 0.10f * (float) Math.sin(3.1f * uu + 0.5f * t) + colJit[c];
                    double ph = TWO_PI * BANDS * ux;
                    colSinB[k][c] = (float) Math.sin(ph);
                    colCosB[k][c] = (float) Math.cos(ph);
                    double ph2 = TWO_PI * BANDS2 * (-SLOPE2 * uu + 0.05f * (float) Math.sin(7f * uu - 0.3f * t));
                    colSinB2[k][c] = (float) Math.sin(ph2);
                    colCosB2[k][c] = (float) Math.cos(ph2);
                }
            }
            for (int c = 0; c < cols; c++) {
                float u = (c + 0.5f) / cols;
                colEnv[c] = 0.5f + 0.5f * (float) Math.sin(2.3f * u + 0.9f + 0.17f * t);
            }
            for (int r = 0; r < rows; r++) {
                float v = rowV[r];
                rowEnv[r] = 0.5f + 0.5f * (float) Math.sin(2.9f * v - 0.7f - 0.13f * t);
                float vx = v + 0.06f * (float) Math.sin(5.3f * v - 0.4f * t);
                double ph = TWO_PI * BANDS * vx + SPEED * t;
                rowSinA[r] = (float) Math.sin(ph);
                rowCosA[r] = (float) Math.cos(ph);
                double ph2 = TWO_PI * BANDS2 * v + SPEED2 * t;
                rowSinA2[r] = (float) Math.sin(ph2);
                rowCosA2[r] = (float) Math.cos(ph2);
            }

            final float span = PEAK - FLOOR;
            for (int r = 0; r < rows; r++) {
                final float sA = rowSinA[r];
                final float cA = rowCosA[r];
                final float sA2 = rowSinA2[r];
                final float cA2 = rowCosA2[r];
                final int yBase = r * CELL;
                final float envR = rowEnv[r];
                for (int c = 0; c < cols; c++) {
                    final float env = 0.50f + 0.50f * (0.35f + 0.65f * colEnv[c] * envR * 1.6f > 1f ? 1f : 0.35f + 0.65f * colEnv[c] * envR * 1.6f);
                    for (int k = 0; k < 3; k++) {
                        float s1 = colSinB[k][c] * cA + colCosB[k][c] * sA;
                        float s2 = colSinB2[k][c] * cA2 + colCosB2[k][c] * sA2;
                        float s = 0.70f * s1 + 0.30f * s2;
                        float x = (s + 0.70f) * (1f / 1.55f);
                        x = x < 0f ? 0f : (x > 1f ? 1f : x);
                        float val = x * x * (3f - 2f * x);
                        g[k] = (FLOOR + span * val * env) * colEdge[k][c] + colAdd[k][c];
                    }
                    int rgb = 0;
                    for (int k = 0; k < 3; k++) {
                        float v = g[k] * TINT[k] * 255f;
                        int ch = v < 0f ? 0 : (int) v;
                        rgb = (rgb << 8) | (ch > 255 ? 255 : ch);
                    }
                    int line = ((((rgb >> 16) & 255) * 7 >> 4) << 16)
                            | ((((rgb >> 8) & 255) * 7 >> 4) << 8)
                            | ((rgb & 255) * 7 >> 4);

                    int x0 = c * CELL;
                    for (int j = 0; j < CELL - 1; j++) {
                        int o = (yBase + j) * canvasW + x0;
                        for (int i = 0; i < CELL - 1; i++) {
                            px[o + i] = rgb;
                        }
                        px[o + CELL - 1] = line;
                    }
                    int o = (yBase + CELL - 1) * canvasW + x0;
                    for (int i = 0; i < CELL; i++) {
                        px[o + i] = line;
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (canvas == null) {
                update();
            }
            if (canvas != null) {
                g.drawImage(canvas, 0, 0, null);
            }
        }
    }
}