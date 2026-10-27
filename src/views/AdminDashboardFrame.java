package views;

import com.formdev.flatlaf.FlatDarkLaf;
import services.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Admin Portal Dashboard.
 * Created by Member 3 (Namitha) & Updated by Member 6 (Nanditha) for session logout and window closing.
 *
 * UI redesign: black page matching the customer storefront, thin "ADMIN PORTAL — CINEVA" wordmark,
 * pill tab switch in the middle and an outlined LOGOUT pill on the right.
 */
public class AdminDashboardFrame extends JFrame {

    private static final Color BG = CustomerHomeFrame.BG;
    private static final Color TEXT = CustomerHomeFrame.TEXT;
    private static final Color DIM = CustomerHomeFrame.DIM;
    private static final Color LINE = CustomerHomeFrame.LINE;

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);

    public AdminDashboardFrame() {
        AdminSkin.installDefaults();
        setTitle("Cinema Admin Dashboard — Operations Management");
        setSize(1280, 800);
        setMinimumSize(new Dimension(1180, 640));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        setContentPane(root);

        AdminShowView showView = new AdminShowView();
        String[] names = {"MOVIES CATALOG", "SHOW SCHEDULING", "SCREENS & CAPACITY"};

        body.setOpaque(false);
        body.add(new AdminMovieDashboardView(), "0");
        JPanel showCard = wrap(showView);
        JPanel screenCard = wrap(new AdminScreenView());
        AdminSkin.apply(showCard);
        AdminSkin.apply(screenCard);
        body.add(showCard, "1");
        body.add(screenCard, "2");

        // Header: wordmark (left) | tabs (true centre) | logout (right)
        Brand brand = new Brand();
        TabPills tabsBar = new TabPills(names, idx -> {
            cards.show(body, String.valueOf(idx));
            if (idx == 1) {
                showView.reloadChoices();
            }
            AdminSkin.apply(showCard);      // re-skin anything the screen rebuilt
            AdminSkin.apply(screenCard);
        });
        LogoutPill logout = new LogoutPill(this::handleLogout);
        JPanel header = new JPanel(null) {
            @Override public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                Dimension b = brand.getPreferredSize();
                Dimension t = tabsBar.getPreferredSize();
                Dimension l = logout.getPreferredSize();
                brand.setBounds(28, (h - b.height) / 2, b.width, b.height);
                logout.setBounds(w - 28 - l.width, (h - l.height) / 2, l.width, l.height);
                tabsBar.setBounds((w - t.width) / 2, (h - t.height) / 2, t.width, t.height);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(100, 82); }
        };
        header.setBackground(BG);
        header.add(brand);
        header.add(tabsBar);
        header.add(logout);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
    }

    private static JPanel wrap(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(22, 28, 22, 28));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private static JPanel anchored(JComponent c, int anchor) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1;
        gc.anchor = anchor;
        p.add(c, gc);
        return p;
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Logout",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.logoutUser();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new AdminDashboardFrame().setVisible(true));
    }

    // =====================================================================
    //  Header pieces
    // =====================================================================

    /** Film-camera icon + thin "ADMIN PORTAL — CINEVA". */
    static final class Brand extends JComponent {
        private static final String TEXT_LINE = "ADMIN PORTAL \u2014 CINEVA";

        Brand() {
            FontMetrics fm = getFontMetrics(CustomerHomeFrame.Fonts.get(false, 21f));
            int w = 40 + Math.round(CustomerHomeFrame.trackedWidth(fm, TEXT_LINE, 1.2f)) + 14;
            setPreferredSize(new Dimension(w, 40));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            Color c = new Color(205, 208, 215);
            g.setColor(c);
            g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float x = 1;
            float y = 9;
            g.draw(new Ellipse2D.Float(x + 1, y, 8, 8));
            g.draw(new Ellipse2D.Float(x + 10, y, 8, 8));
            g.draw(new RoundRectangle2D.Float(x, y + 9, 17, 13, 4, 4));
            Path2D lens = new Path2D.Float();
            lens.moveTo(x + 17, y + 13);
            lens.lineTo(x + 24, y + 10);
            lens.lineTo(x + 24, y + 21);
            lens.lineTo(x + 17, y + 18);
            g.draw(lens);

            g.setFont(CustomerHomeFrame.Fonts.get(false, 21f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(new Color(214, 217, 224));
            float ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2f;
            CustomerHomeFrame.drawTracked(g, "ADMIN PORTAL — CINEVA", 40, ty, 1.2f);
            g.dispose();
        }
    }

    /** Rounded tab switch; the selected tab is the white pill. */
    static final class TabPills extends JComponent {
        private final String[] labels;
        private final java.util.function.IntConsumer onChange;
        private final int[] segW;
        private int selected = 0;
        private int hover = -1;

        TabPills(String[] labels, java.util.function.IntConsumer onChange) {
            this.labels = labels;
            this.onChange = onChange;
            FontMetrics fm = getFontMetrics(CustomerHomeFrame.Fonts.get(true, 10.5f));
            segW = new int[labels.length];
            int total = 6;
            for (int i = 0; i < labels.length; i++) {
                segW[i] = Math.round(CustomerHomeFrame.trackedWidth(fm, labels[i], 1.3f)) + 32;
                total += segW[i];
            }
            setPreferredSize(new Dimension(total, 40));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { hover = indexAt(e.getX()); repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    int i = indexAt(e.getX());
                    if (i >= 0 && i != selected) {
                        selected = i;
                        repaint();
                        onChange.accept(i);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private int indexAt(int x) {
            int sx = 3;
            for (int i = 0; i < segW.length; i++) {
                if (x >= sx && x < sx + segW[i]) {
                    return i;
                }
                sx += segW[i];
            }
            return -1;
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
            g.setFont(CustomerHomeFrame.Fonts.get(true, 10.5f));
            FontMetrics fm = g.getFontMetrics();
            float sx = 3;
            for (int i = 0; i < labels.length; i++) {
                if (i == selected) {
                    g.setColor(Color.WHITE);
                    g.fill(new RoundRectangle2D.Float(sx, 3, segW[i], h - 6, h, h));
                }
                g.setColor(i == selected ? new Color(12, 12, 14) : (hover == i ? TEXT : DIM));
                float tw = CustomerHomeFrame.trackedWidth(fm, labels[i], 1.3f);
                CustomerHomeFrame.drawTracked(g, labels[i], sx + (segW[i] - tw) / 2f,
                        (h + fm.getAscent() - fm.getDescent()) / 2f, 1.3f);
                sx += segW[i];
            }
            g.dispose();
        }
    }

    /** Outlined LOGOUT pill with a faint red edge. */
    static final class LogoutPill extends JComponent {
        private final Runnable action;
        private boolean hover;

        LogoutPill(Runnable action) {
            this.action = action;
            setPreferredSize(new Dimension(104, 38));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { LogoutPill.this.action.run(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(hover ? new Color(70, 22, 30) : new Color(34, 14, 18));
            g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, h, h));
            g.setColor(hover ? new Color(225, 90, 105) : new Color(122, 52, 62));
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));
            g.setFont(CustomerHomeFrame.Fonts.get(true, 11f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(hover ? new Color(255, 190, 196) : new Color(238, 170, 178));
            float tw = CustomerHomeFrame.trackedWidth(fm, "LOGOUT", 1.6f);
            CustomerHomeFrame.drawTracked(g, "LOGOUT", (w - tw) / 2f, (h + fm.getAscent() - fm.getDescent()) / 2f, 1.6f);
            g.dispose();
        }
    }
}