package views;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

import dao.MovieDAO;
import dao.PaymentDAO;
import models.Movie;
import models.Payment;
import models.User;
import services.SessionManager;

/**
 * Booking History and Cancellation Frame (Member 5: Renjitha Raju), restyled to match the
 * Cineva home page: dark, minimal, rounded rows with poster thumbnails and status chips.
 * Constructor, DAO calls, receipt and cancel logic are unchanged.
 */
public class BookingHistoryFrame extends JFrame {

    // column order from PaymentDAO.getHistory()
    private static final int COL_ID = 0, COL_MOVIE = 1, COL_DATE = 2, COL_TIME = 3,
            COL_SEATS = 4, COL_STATUS = 5, COL_TOTAL = 6;

    private static final Color BG    = new Color(8, 8, 10);
    private static final Color LINE  = new Color(70, 70, 76);
    private static final Color MUTED = new Color(140, 140, 148);
    private static final Color CYAN  = new Color(96, 200, 230);
    private static final Color RED   = new Color(232, 96, 96);
    private static final Color GREEN = new Color(96, 214, 144);
    private static final String SANS = "Segoe UI";

    private static final String[] HEADS = {"Booking ID", "Movie", "Date", "Time", "Seats", "Status", "Total"};
    private static final double[] FRAC  = {0.10, 0.26, 0.14, 0.12, 0.14, 0.14, 0.10};
    private static final int ROW_H = 54, GAP = 8, THUMB_W = 30, THUMB_H = 42;

    private final int userId;
    private final PaymentDAO dao = new PaymentDAO();

    private List<Object[]> all = new ArrayList<>();
    private List<Object[]> shown = new ArrayList<>();
    private final Map<String, BufferedImage> thumbs = new HashMap<>();   // lower-case title -> poster
    private int tab = 0;                    // 0 = active bookings, 1 = past history
    private boolean firstLoad = true;

    private final NavToggle nav = new NavToggle();
    private final BookingList list = new BookingList();
    private final ListHeader listHeader = new ListHeader();

    public BookingHistoryFrame(int userId) {
        super("My Bookings & Tickets \u2014 Cineva");
        this.userId = userId;

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(new EmptyBorder(0, 40, 0, 40));
        sp.setBackground(BG);
        sp.getViewport().setBackground(BG);
        sp.setColumnHeaderView(listHeader);
        sp.getColumnHeader().setBackground(BG);
        sp.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sp.getVerticalScrollBar().setUnitIncrement(24);
        add(sp, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1100, screen.width - 40), Math.min(720, screen.height - 40));
        setMinimumSize(new Dimension(Math.min(900, screen.width - 40), Math.min(520, screen.height - 40)));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        loadPosters();
        loadData();
    }

    // ---------- layout ----------

    private JPanel buildHeader() {
        TrackedText title = new TrackedText("MY BOOKINGS \u2014 CINEVA", "Segoe UI Light", 22f,
                new Color(225, 225, 230), 0.06f, true);
        UserPill pill = new UserPill();

        // Title left, pill right. The tab switch sits in the true centre when there is room,
        // otherwise it slides next to the pill and the title shrinks to fit (never clipped).
        JPanel header = new JPanel(null) {
            @Override public void doLayout() {
                int w = getWidth(), h = getHeight(), padX = 40;
                Dimension t = title.getPreferredSize(), n = nav.getPreferredSize(), p = pill.getPreferredSize();
                pill.setBounds(w - padX - p.width, (h - p.height) / 2, p.width, p.height);
                int centred = (w - n.width) / 2;
                int nx = (padX + t.width + 24 <= centred) ? centred : w - padX - p.width - 20 - n.width;
                nav.setBounds(nx, (h - n.height) / 2, n.width, n.height);
                int tw = Math.max(60, Math.min(t.width, nx - 24 - padX));
                title.setBounds(padX, (h - t.height) / 2, tw, t.height);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(100, 86); }
        };
        header.setBackground(BG);
        header.add(title);
        header.add(nav);
        header.add(pill);
        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG);
        footer.setBorder(new EmptyBorder(14, 40, 28, 40));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);
        buttons.add(new PillBtn("REFRESH", 0, this::loadData));
        buttons.add(new PillBtn("VIEW E-TICKET", 1, this::viewReceipt));
        buttons.add(new PillBtn("CANCEL BOOKING", 2, this::cancelSelected));
        JPanel wrap = new JPanel(new GridBagLayout());     // vertically centre the buttons
        wrap.setOpaque(false);
        wrap.add(buttons);

        footer.add(wrap, BorderLayout.EAST);
        return footer;
    }

    // ---------- data ----------

    private void loadData() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() throws Exception {
                return dao.getHistory(userId);
            }
            @Override protected void done() {
                try {
                    all = get();
                    if (firstLoad) {
                        firstLoad = false;
                        boolean anyActive = false;
                        for (Object[] r : all) if (isActiveRow(r)) { anyActive = true; break; }
                        if (!anyActive && !all.isEmpty()) tab = 1;     // nothing upcoming: show history
                        nav.repaint();
                    }
                    applyTab();
                } catch (Exception e) {
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(BookingHistoryFrame.this,
                            "Could not load bookings: " + c.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** Fetches poster thumbnails once, matched to bookings by movie title. */
    private void loadPosters() {
        new SwingWorker<Map<String, BufferedImage>, Void>() {
            @Override protected Map<String, BufferedImage> doInBackground() {
                Map<String, BufferedImage> out = new HashMap<>();
                try {
                    for (Movie m : new MovieDAO().getAll()) {
                    	BufferedImage img = CustomerHomeFrame.PosterStrip.readPoster(m);
                        if (img != null && m.getTitle() != null) {
                            out.put(m.getTitle().toLowerCase(), thumb(img, THUMB_W * 2, THUMB_H * 2));
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("[BookingHistoryFrame] posters unavailable: " + t.getMessage());
                }
                return out;
            }
            @Override protected void done() {
                try { thumbs.putAll(get()); list.repaint(); } catch (Exception ignored) { }
            }
        }.execute();
    }

    /** Reads a saved poster image (png/jpg). Returns null if the file is missing or unreadable. */
    private static BufferedImage loadPoster(String path) {
        if (path == null || path.isBlank()) return null;
        File f = new File(path);
        if (!f.isFile()) {
            String name = f.getName();
            String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            for (String dir : new String[] {"assets/posters/", "resources/posters/"}) {
                for (String ext : new String[] {".png", ".jpg", ".jpeg"}) {
                    File alt = new File(dir + base + ext);
                    if (alt.isFile()) { f = alt; break; }
                }
                if (f.isFile()) break;
            }
        }
        try {
            return f.isFile() ? ImageIO.read(f) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Active = confirmed and the show is still ahead; everything else is history. */
    private boolean isActiveRow(Object[] r) {
        if (!"CONFIRMED".equalsIgnoreCase(String.valueOf(r[COL_STATUS]))) return false;
        try {
            return LocalDateTime.parse(r[COL_DATE] + "T" + r[COL_TIME]).isAfter(LocalDateTime.now());
        } catch (Exception e) {
            return true;
        }
    }

    private void applyTab() {
        shown = new ArrayList<>();
        for (Object[] r : all) if ((tab == 0) == isActiveRow(r)) shown.add(r);
        listHeader.setVisible(!shown.isEmpty());      // no column titles over an empty state
        list.reset();
    }

    // ---------- actions (logic unchanged) ----------

    private Object[] selectedRow() {
        Object[] row = list.selectedRow();
        if (row == null) {
            JOptionPane.showMessageDialog(this, "Please select a booking from the list first.");
        }
        return row;
    }

    private void viewReceipt() {
        Object[] row = selectedRow();
        if (row == null) return;

        String status = String.valueOf(row[COL_STATUS]);
        if (!"CONFIRMED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this,
                    "Receipts can only be generated for confirmed bookings.",
                    "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int id = (Integer) row[COL_ID];
        String[] info = dao.getBookingSummary(id);
        if (info == null) {
            JOptionPane.showMessageDialog(this, "Booking details could not be found.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Payment payment = dao.getPaymentByBookingId(id);
        String method = (payment != null && payment.getMethod() != null) ? payment.getMethod() : "CARD";
        double amount = (payment != null && payment.getAmount() > 0)
                ? payment.getAmount()
                : parseAmount(String.valueOf(row[COL_TOTAL]));
        String refCode = (payment != null && payment.getTransactionRef() != null)
                ? payment.getTransactionRef()
                : ("BK-" + String.format("%05d", id));

        new TicketReceiptDialog(this, id, info[0], info[1], info[2], amount, method, refCode).setVisible(true);
    }

    private void cancelSelected() {
        Object[] row = selectedRow();
        if (row == null) return;

        int id = (Integer) row[COL_ID];
        String status = String.valueOf(row[COL_STATUS]);
        if ("CANCELLED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "This booking has already been cancelled.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel booking #" + id + " (" + row[COL_MOVIE] + ")?\n\n"
                + "Refund Policy:\n"
                + " \u2022 100% refund if 24 hours or more before showtime\n"
                + " \u2022 50% refund if between 1 and 24 hours before showtime\n"
                + " \u2022 No cancellation permitted within 1 hour of the show",
                "Confirm Ticket Cancellation", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        try {
            double refund = dao.cancelBooking(id);
            JOptionPane.showMessageDialog(this,
                    String.format("Booking #%d successfully cancelled.\nRefund Amount: Rs. %.2f", id, refund),
                    "Cancellation Confirmed", JOptionPane.INFORMATION_MESSAGE);
            loadData();
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot Cancel", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double parseAmount(String text) {
        try {
            return Double.parseDouble(text.replace("Rs.", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ---------- helpers ----------

    private static Graphics2D aa(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g;
    }

    private static Font tracked(Font f, float tracking) {
        Map<TextAttribute, Object> m = new HashMap<>(f.getAttributes());
        m.put(TextAttribute.TRACKING, tracking);
        return f.deriveFont(m);
    }

    private static int colX(int c, int w) {
        double s = 0;
        for (int i = 0; i < c; i++) s += FRAC[i];
        return (int) Math.round(s * w);
    }

    private static int colW(int c, int w) { return (int) Math.round(FRAC[c] * w); }

    private static String fit(FontMetrics fm, String s, int max) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= max) return s;
        while (s.length() > 1 && fm.stringWidth(s + "\u2026") > max) s = s.substring(0, s.length() - 1);
        return s + "\u2026";
    }

    private static void drawCentered(Graphics2D g, String s, int x, int w, int h, int y0) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(fit(fm, s, w - 8), x + (w - Math.min(fm.stringWidth(s), w - 8)) / 2,
                y0 + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    private static BufferedImage thumb(BufferedImage src, int w, int h) {
        double s = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
        int sw = (int) Math.round(w / s), sh = (int) Math.round(h / s);
        int sx = Math.max(0, (src.getWidth() - sw) / 2), sy = Math.max(0, (src.getHeight() - sh) / 2);
        sw = Math.min(sw, src.getWidth() - sx);
        sh = Math.min(sh, src.getHeight() - sy);
        Image scaled = src.getSubimage(sx, sy, sw, sh).getScaledInstance(w, h, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return out;
    }

    // ---------- custom components ----------

    /**
     * Single-line tracked text that is measured with the same fractional metrics it is painted with
     * (so the last letter is never clipped) and, when shrink is on, scales down to fit its width.
     */
    private static class TrackedText extends JComponent {
        private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
        private static final int PAD = 6;
        private final String text;
        private final Font base;
        private final float maxSize, trackEm;
        private final Color color;
        private final boolean shrink;

        TrackedText(String text, String family, float size, Color color, float trackEm, boolean shrink) {
            this.text = text; this.maxSize = size; this.color = color; this.trackEm = trackEm; this.shrink = shrink;
            this.base = new Font(family, Font.PLAIN, 12);
            FontMetrics fm = getFontMetrics(base.deriveFont(size));
            setPreferredSize(new Dimension(Math.round(width(size)) + 2 * PAD, fm.getHeight()));
            setMinimumSize(new Dimension(40, fm.getHeight()));
        }

        private float width(float size) {
            Font f = base.deriveFont(size);
            float w = 0, gap = trackEm * size;
            for (int i = 0; i < text.length(); i++) {
                w += (float) f.getStringBounds(text.substring(i, i + 1), FRC).getWidth() + (i < text.length() - 1 ? gap : 0);
            }
            return w;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = aa(g0);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            float size = maxSize;
            if (shrink) {
                float avail = getWidth() - 2 * PAD, w = width(maxSize);
                if (w > avail) size = Math.max(11f, maxSize * avail / w);
            }
            Font f = base.deriveFont(size);
            g.setFont(f);
            g.setColor(color);
            FontMetrics fm = g.getFontMetrics();
            float gap = trackEm * size, x = 0;
            float y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2f;
            for (int i = 0; i < text.length(); i++) {
                String ch = text.substring(i, i + 1);
                g.drawString(ch, x, y);
                x += (float) f.getStringBounds(ch, FRC).getWidth() + gap;
            }
            g.dispose();
        }
    }

    /** Column headings, aligned with BookingList. */
    private class ListHeader extends JComponent {
        @Override public Dimension getPreferredSize() { return new Dimension(100, 40); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = aa(g0);
            g.setColor(BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(tracked(new Font(SANS, Font.PLAIN, 13), 0.04f));
            g.setColor(MUTED);
            int w = getWidth();
            for (int c = 0; c < HEADS.length; c++) {
                if (c == COL_MOVIE) {
                    g.drawString(HEADS[c], colX(c, w) + 4 + THUMB_W + 14, 26);
                } else {
                    drawCentered(g, HEADS[c], colX(c, w), colW(c, w), 40, 0);
                }
            }
            g.dispose();
        }
    }

    /** Rounded booking rows. Click to select, double-click to open the e-ticket. */
    private class BookingList extends JComponent implements Scrollable {
        private int hover = -1, selected = -1;
        private final Rectangle emptyBtn = new Rectangle();
        private boolean emptyHover;

        BookingList() {
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    if (shown.isEmpty()) {
                        boolean over = emptyBtn.contains(e.getPoint());
                        if (over != emptyHover) { emptyHover = over; repaint(); }
                        setCursor(Cursor.getPredefinedCursor(over ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        return;
                    }
                    int i = indexAt(e.getY());
                    if (i != hover) { hover = i; repaint(); }
                    setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
                @Override public void mouseExited(MouseEvent e) { hover = -1; emptyHover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    if (shown.isEmpty()) {
                        if (emptyBtn.contains(e.getPoint())) {
                            if (tab == 0) {
                                dispose();                                  // back to the movie strip
                            } else {
                                tab = 0; nav.repaint(); applyTab();         // show active bookings
                            }
                        }
                        return;
                    }
                    int i = indexAt(e.getY());
                    if (i >= 0) {
                        selected = i;
                        repaint();
                        if (e.getClickCount() == 2) viewReceipt();
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        void reset() { selected = -1; hover = -1; revalidate(); repaint(); }

        Object[] selectedRow() { return (selected >= 0 && selected < shown.size()) ? shown.get(selected) : null; }

        private int indexAt(int y) {
            int i = y / (ROW_H + GAP);
            return (i >= 0 && i < shown.size() && y % (ROW_H + GAP) < ROW_H) ? i : -1;
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(600, Math.max(1, shown.size()) * (ROW_H + GAP));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = aa(g0);
            int w = getWidth();
            if (shown.isEmpty()) {
                paintEmpty(g, w, getHeight());
                g.dispose();
                return;
            }
            for (int i = 0; i < shown.size(); i++) {
                Object[] r = shown.get(i);
                int y = i * (ROW_H + GAP);
                RoundRectangle2D box = new RoundRectangle2D.Float(0.75f, y + 0.75f, w - 1.5f, ROW_H - 1.5f, 18, 18);
                g.setColor(i == hover || i == selected ? new Color(24, 24, 30) : new Color(13, 13, 17));
                g.fill(box);
                g.setStroke(new BasicStroke(i == selected ? 1.5f : 1f));
                g.setColor(i == selected ? CYAN : LINE);
                g.draw(box);

                g.setFont(new Font(SANS, Font.PLAIN, 14));
                for (int c = 0; c < HEADS.length; c++) {
                    int x = colX(c, w), cw = colW(c, w);
                    String v = String.valueOf(r[c]);
                    if (c == COL_MOVIE) {
                        paintMovie(g, v, x, cw, y);
                    } else if (c == COL_STATUS) {
                        paintChip(g, v, x, cw, y);
                    } else {
                        g.setColor(c == COL_ID ? new Color(225, 225, 230) : new Color(200, 200, 206));
                        drawCentered(g, v, x, cw, ROW_H, y);
                    }
                }
            }
            g.dispose();
        }

        /** Empty state: icon, headline, hint and one action. Different for Active and Past. */
        private void paintEmpty(Graphics2D g, int w, int h) {
            boolean active = tab == 0;
            String head = active ? "NO ACTIVE BOOKINGS" : "NOTHING IN YOUR HISTORY YET";
            String hint = active ? "Pick a movie and your tickets will show up here."
                                 : "Past and cancelled bookings will appear here.";
            String btn = active ? "BROWSE MOVIES" : "VIEW ACTIVE BOOKINGS";

            int blockH = 64 + 26 + 30 + 26 + 40;
            int top = Math.max(10, (h - blockH) / 2 - 10);
            int cx = w / 2;

            // icon
            int d = 64;
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(LINE);
            g.draw(new Ellipse2D.Float(cx - d / 2f + 0.5f, top + 0.5f, d - 1, d - 1));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(MUTED);
            float iy = top + d / 2f;
            if (active) {                                   // ticket
                g.draw(new RoundRectangle2D.Float(cx - 15, iy - 9, 30, 18, 5, 5));
                g.draw(new Line2D.Float(cx + 4, iy - 9, cx + 4, iy - 5));
                g.draw(new Line2D.Float(cx + 4, iy - 2, cx + 4, iy + 2));
                g.draw(new Line2D.Float(cx + 4, iy + 5, cx + 4, iy + 9));
            } else {                                        // clock
                g.draw(new Ellipse2D.Float(cx - 11, iy - 11, 22, 22));
                g.draw(new Line2D.Float(cx, iy, cx, iy - 7));
                g.draw(new Line2D.Float(cx, iy, cx + 5, iy + 3));
            }

            // headline (tracked) + hint
            Font hf = new Font("Segoe UI Light", Font.PLAIN, 22);
            g.setFont(hf);
            g.setColor(new Color(225, 225, 230));
            float gap = 22 * 0.06f;
            FontRenderContext frc = new FontRenderContext(null, true, true);
            float tw = 0;
            for (int i = 0; i < head.length(); i++) {
                tw += (float) hf.getStringBounds(head.substring(i, i + 1), frc).getWidth() + (i < head.length() - 1 ? gap : 0);
            }
            float x = cx - tw / 2f;
            int ty = top + d + 38;
            for (int i = 0; i < head.length(); i++) {
                String ch = head.substring(i, i + 1);
                g.drawString(ch, x, ty);
                x += (float) hf.getStringBounds(ch, frc).getWidth() + gap;
            }
            g.setFont(new Font(SANS, Font.PLAIN, 13));
            g.setColor(MUTED);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(hint, cx - fm.stringWidth(hint) / 2, ty + 28);

            // action pill
            Font bf = tracked(new Font(SANS, Font.PLAIN, 12), 0.08f);
            int bw = g.getFontMetrics(bf).stringWidth(btn) + 52, bh = 40;
            int bx = cx - bw / 2, by = ty + 28 + 24;
            emptyBtn.setBounds(bx, by, bw, bh);
            RoundRectangle2D r = new RoundRectangle2D.Float(bx + 0.5f, by + 0.5f, bw - 1, bh - 1, bh, bh);
            Color fg;
            if (active) {
                g.setColor(emptyHover ? new Color(225, 225, 230) : Color.WHITE);
                g.fill(r);
                fg = Color.BLACK;
            } else {
                fg = new Color(225, 225, 230);
                if (emptyHover) { g.setColor(new Color(255, 255, 255, 14)); g.fill(r); }
                g.setStroke(new BasicStroke(1f));
                g.setColor(emptyHover ? fg : LINE);
                g.draw(r);
            }
            g.setFont(bf);
            g.setColor(fg);
            FontMetrics bm = g.getFontMetrics();
            g.drawString(btn, bx + (bw - bm.stringWidth(btn)) / 2, by + (bh + bm.getAscent() - bm.getDescent()) / 2);
        }

        private void paintMovie(Graphics2D g, String title, int x, int cw, int y) {
            int tx = x + 4, ty = y + (ROW_H - THUMB_H) / 2;
            BufferedImage img = thumbs.get(title.toLowerCase());
            Shape old = g.getClip();
            g.setClip(new RoundRectangle2D.Float(tx, ty, THUMB_W, THUMB_H, 6, 6));
            if (img != null) {
                g.drawImage(img, tx, ty, THUMB_W, THUMB_H, null);
            } else {
                g.setColor(new Color(34, 34, 42));
                g.fillRect(tx, ty, THUMB_W, THUMB_H);
            }
            g.setClip(old);
            g.setFont(new Font(SANS, Font.PLAIN, 14));
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(fit(fm, title, cw - THUMB_W - 22), tx + THUMB_W + 14,
                    y + (ROW_H + fm.getAscent() - fm.getDescent()) / 2);
        }

        private void paintChip(Graphics2D g, String status, int x, int cw, int y) {
            boolean ok = "CONFIRMED".equalsIgnoreCase(status);
            Color col = ok ? GREEN : RED;
            g.setFont(tracked(new Font(SANS, Font.PLAIN, 12), 0.06f));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(status) + 26, h = 24;
            int cx = x + (cw - w) / 2, cy = y + (ROW_H - h) / 2;
            RoundRectangle2D chip = new RoundRectangle2D.Float(cx + 0.5f, cy + 0.5f, w - 1, h - 1, h, h);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 28));
            g.fill(chip);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 150));
            g.setStroke(new BasicStroke(1f));
            g.draw(chip);
            g.setColor(col);
            g.drawString(status, cx + 13, cy + (h + fm.getAscent() - fm.getDescent()) / 2);
        }

        // Scrollable: follow the viewport width so rows stretch with the window
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return shown.isEmpty(); }   // empty state centres in the view
    }

    /** ACTIVE BOOKINGS | PAST HISTORY switch. */
    private class NavToggle extends JComponent {
        private final String[] labels = {"ACTIVE BOOKINGS", "PAST HISTORY"};
        private final Font f = tracked(new Font(SANS, Font.PLAIN, 12), 0.08f);

        NavToggle() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int t = e.getX() < getWidth() / 2 ? 0 : 1;
                    if (t != tab) { tab = t; repaint(); applyTab(); }
                }
            });
        }
        @Override public Dimension getPreferredSize() { return new Dimension(290, 40); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = aa(g0);
            int w = getWidth(), h = getHeight(), seg = w / 2;
            g.setColor(LINE);
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(4 + tab * seg, 4, seg - 4, h - 8, h, h));
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < 2; i++) {
                g.setColor(i == tab ? Color.BLACK : MUTED);
                g.drawString(labels[i], i * seg + (seg - fm.stringWidth(labels[i])) / 2,
                        (h + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
        }
    }

    /** "WELCOME, NAME" with avatar. */
    private class UserPill extends JComponent {
        private final String name = currentName();
        private final String label = "WELCOME, " + (name.length() > 16 ? name.substring(0, 15) + "\u2026" : name).toUpperCase();
        private final Font f = tracked(new Font(SANS, Font.PLAIN, 12), 0.08f);

        private String currentName() {
            User u = SessionManager.getCurrentUser();
            return (u != null && u.getName() != null && !u.getName().isEmpty()) ? u.getName() : "Movie Fan";
        }
        @Override public Dimension getPreferredSize() {
            return new Dimension(7 + 30 + 12 + getFontMetrics(f).stringWidth(label) + 20, 44);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = aa(g0);
            int w = getWidth(), h = getHeight();
            g.setColor(LINE);
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));
            g.setPaint(new GradientPaint(0, 7, new Color(236, 196, 96), 0, 37, new Color(176, 116, 40)));
            g.fill(new Ellipse2D.Float(7, 7, 30, 30));
            g.setFont(new Font(SANS, Font.BOLD, 14));
            g.setColor(new Color(40, 24, 8));
            String ini = name.substring(0, 1).toUpperCase();
            FontMetrics fm0 = g.getFontMetrics();
            g.drawString(ini, 7 + (30 - fm0.stringWidth(ini)) / 2, 7 + (30 + fm0.getAscent() - fm0.getDescent()) / 2);
            g.setFont(f);
            g.setColor(new Color(225, 225, 230));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, 7 + 30 + 12, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Pill button: style 0 = outline, 1 = solid white, 2 = outline (danger). */
    private class PillBtn extends JComponent {
        private final String text;
        private final int style;
        private boolean hover;

        PillBtn(String text, int style, Runnable action) {
            this.text = text;
            this.style = style;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { action.run(); }
            });
        }
        private Font font() { return tracked(new Font(SANS, Font.PLAIN, 12), 0.08f); }
        @Override public Dimension getPreferredSize() {
            return new Dimension(getFontMetrics(font()).stringWidth(text) + 48, 40);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = aa(g0);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h);
            Color fg;
            if (style == 1) {
                g.setColor(hover ? new Color(225, 225, 230) : Color.WHITE);
                g.fill(r);
                fg = Color.BLACK;
            } else {
                Color edge = style == 2 ? new Color(120, 60, 60) : LINE;
                fg = style == 2 ? RED : new Color(225, 225, 230);
                if (hover) { g.setColor(new Color(255, 255, 255, 14)); g.fill(r); edge = fg; }
                g.setColor(edge);
                g.draw(r);
            }
            g.setFont(font());
            g.setColor(fg);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (w - fm.stringWidth(text)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Slim dark scrollbar. */
    private static class ThinScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() { thumbColor = new Color(70, 70, 78); trackColor = BG; }
        private JButton none() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        @Override protected JButton createDecreaseButton(int o) { return none(); }
        @Override protected JButton createIncreaseButton(int o) { return none(); }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(BG);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
        @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g = aa(g0);
            g.setColor(thumbColor);
            g.fillRoundRect(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 6, 6);
            g.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BookingHistoryFrame(1).setVisible(true));
    }
}