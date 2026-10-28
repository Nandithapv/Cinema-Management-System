package views;

import com.formdev.flatlaf.FlatDarkLaf;
import dao.MovieDAO;
import models.Movie;
import models.User;
import services.SessionManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * Customer storefront for Cineva.
 *
 * Design: pure black page, slim header (wordmark / pill switch / welcome pill) and one big
 * strip of tall posters. Move the mouse along the strip and the posters slide with it: the
 * poster under the cursor opens up, its neighbours squeeze, and its name lights up above it.
 * When the mouse is away the strip plays itself, moving from poster to poster.
 *
 * Poster images are looked up (in this order):
 *   1. the path stored with the movie (e.g. assets/posters/inception.jpg)
 *   2. any image in assets/posters (or posters, assets, images) whose file name looks like the title
 *
 * Optional font: drop Poppins-Regular.ttf + Poppins-SemiBold.ttf (or the Inter equivalents)
 * into a folder called "fonts" in the project root. Without them Segoe UI is used.
 */
public class CustomerHomeFrame extends JFrame {

    // ---------- palette ----------
    static final Color BG    = new Color(5, 5, 7);
    static final Color TEXT  = new Color(244, 245, 247);
    static final Color DIM   = new Color(120, 124, 134);
    static final Color LINE  = new Color(58, 61, 70);

    /** Shown under "Now Showing" at the bottom left. Change it when your schedule changes; "" hides it. */
    static final String SHOW_DATES = "16 – 19 Oct";

    private int currentUserId = 1;
    private String currentUserName = "Customer";

    private final List<Movie> allMovies = new ArrayList<>();
    private String activeGenreFilter = "All";

    private PosterStrip strip;
    private Heading heading;
    private SearchBox searchBox;
    private final JPanel chipsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final List<Chip> chips = new ArrayList<>();

    public CustomerHomeFrame() {
        super("Cineva — Cinema Management System");
        resolveCurrentUser();
        initUI();
        loadMovies();
    }

    private void resolveCurrentUser() {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            currentUserName = user.getName();
            currentUserId = user.getUserId();
        } else {
            currentUserName = "Movie Fan";
            currentUserId = 1;
        }
    }

    // =====================================================================
    //  Layout
    // =====================================================================
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1200, 640));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        setContentPane(root);

        root.add(createHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(createToolbar(), BorderLayout.NORTH);

        strip = new PosterStrip((movieId, title) -> onMovieBookClicked(movieId, title));
        center.add(strip, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new GridLayout(1, 3));
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(20, 28, 6, 28));

        header.add(anchored(new WordMark(), GridBagConstraints.WEST));

        PillSwitch pills = new PillSwitch(new String[] {"MOVIES", "MY BOOKINGS"}, idx -> {
            if (idx == 1) {
                openBookingHistory();
            }
        });
        header.add(anchored(pills, GridBagConstraints.CENTER));

        WelcomePill welcome = new WelcomePill(currentUserName, this::handleLogout);
        header.add(anchored(welcome, GridBagConstraints.EAST));
        return header;
    }

    private JPanel createToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(22, 28, 14, 28));

        heading = new Heading();
        bar.add(heading, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        searchBox = new SearchBox();
        searchBox.field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        right.add(searchBox);

        chipsPanel.setOpaque(false);
        right.add(chipsPanel);
        bar.add(right, BorderLayout.EAST);
        return bar;
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

    private void rebuildChips() {
        chipsPanel.removeAll();
        chips.clear();
        Set<String> genres = new LinkedHashSet<>();
        for (Movie m : allMovies) {
            if (m.getGenre() != null && !m.getGenre().trim().isEmpty()) {
                genres.add(m.getGenre().trim());
            }
        }
        List<String> names = new ArrayList<>();
        names.add("All");
        for (String g : genres) {
            if (names.size() < 6) {
                names.add(g);
            }
        }
        if (!names.contains(activeGenreFilter)) {
            activeGenreFilter = "All";
        }
        for (String name : names) {
            Chip chip = new Chip(name);
            chip.setActive(name.equalsIgnoreCase(activeGenreFilter));
            chip.onClick = () -> {
                activeGenreFilter = name;
                for (Chip c : chips) {
                    c.setActive(c.text.equalsIgnoreCase(activeGenreFilter));
                }
                applyFilter();
            };
            chips.add(chip);
            chipsPanel.add(chip);
        }
        chipsPanel.revalidate();
        chipsPanel.repaint();
    }

    // =====================================================================
    //  Data (same logic as before)
    // =====================================================================
    /** Loads movies from MovieDAO; falls back to demo movies if the database is empty or offline. */
    public void loadMovies() {
        allMovies.clear();
        boolean loadedFromDAO = false;

        try {
            MovieDAO movieDAO = new MovieDAO();
            List<Movie> list = movieDAO.getActiveMovies();
            if (list == null || list.isEmpty()) {
                list = movieDAO.getAll();
            }
            if (list != null && !list.isEmpty()) {
                allMovies.addAll(list);
                loadedFromDAO = true;
            }
        } catch (Throwable t) {
            System.err.println("[CustomerHomeFrame] MovieDAO unavailable: " + t.getMessage());
        }

        // Demo fallback movies if database not seeded yet
        if (!loadedFromDAO) {
            allMovies.add(new Movie(1, "12th Fail", "Drama", 147, "Hindi", "assets/posters/12th-fail.jpg", true));
            allMovies.add(new Movie(2, "Drishyam", "Thriller", 160, "Malayalam", "assets/posters/drishyam.jpg", true));
            allMovies.add(new Movie(3, "English Vinglish", "Comedy", 134, "Hindi", "assets/posters/english-vinglish.jpg", true));
            allMovies.add(new Movie(4, "Inception", "Sci-Fi", 148, "English", "assets/posters/inception.jpg", true));
            allMovies.add(new Movie(5, "Interstellar", "Sci-Fi", 169, "English", "assets/posters/interstellar.jpg", true));
            allMovies.add(new Movie(6, "Manichitrathazhu", "Horror", 157, "Malayalam", "assets/posters/manichitrathazhu.jpg", true));
            allMovies.add(new Movie(7, "Past Lives", "Romance", 106, "English", "assets/posters/past-lives.jpg", true));
            allMovies.add(new Movie(8, "Premam", "Romance", 156, "Malayalam", "assets/posters/premam.jpg", true));
            allMovies.add(new Movie(9, "The Lunchbox", "Romance", 104, "Hindi", "assets/posters/lunchbox.jpg", true));
        }

        strip.setMovies(allMovies);
        rebuildChips();
        applyFilter();
    }

    /** Applies search text and genre chip: posters that don't match are dimmed. */
    private void applyFilter() {
        if (strip == null) {
            return;
        }
        String query = searchBox.field.getText().trim().toLowerCase();
        boolean[] match = new boolean[allMovies.size()];
        int count = 0;
        for (int i = 0; i < allMovies.size(); i++) {
            Movie m = allMovies.get(i);
            boolean matchesSearch = query.isEmpty()
                    || (m.getTitle() != null && m.getTitle().toLowerCase().contains(query));
            boolean matchesCategory = "All".equalsIgnoreCase(activeGenreFilter)
                    || (m.getGenre() != null && m.getGenre().toLowerCase().contains(activeGenreFilter.toLowerCase()));
            match[i] = matchesSearch && matchesCategory;
            if (match[i]) {
                count++;
            }
        }
        strip.setMatches(match);
        heading.setCount(count);
    }

    /** Triggered when the user clicks 'Book Tickets' on a movie. */
    private void onMovieBookClicked(int movieId, String movieTitle) {
        User user = SessionManager.getCurrentUser();
        ShowSelectionDialog dialog = new ShowSelectionDialog(this, movieId, movieTitle, user);
        dialog.setVisible(true);
    }

    /** Opens BookingHistoryFrame. */
    private void openBookingHistory() {
        User user = SessionManager.getCurrentUser();
        int uid = (user != null) ? user.getUserId() : currentUserId;
        BookingHistoryFrame historyFrame = new BookingHistoryFrame(uid);
        historyFrame.setVisible(true);
    }

    /** Logs out current user and returns to Login. */
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.logoutUser();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    /** Standalone preview runner. */
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new CustomerHomeFrame().setVisible(true));
    }

    // =====================================================================
    //  Fonts & small drawing helpers
    // =====================================================================
    static final class Fonts {
        private static Font reg;
        private static Font semi;
        private static boolean loaded;

        private static synchronized void load() {
            if (loaded) {
                return;
            }
            loaded = true;
            String[][] families = {
                    {"Poppins-Regular.ttf", "Poppins-SemiBold.ttf"},
                    {"Inter-Regular.ttf", "Inter-SemiBold.ttf"},
                    {"Montserrat-Regular.ttf", "Montserrat-SemiBold.ttf"}
            };
            for (String[] f : families) {
                Font r = read(f[0]);
                if (r == null) {
                    continue;
                }
                Font s = read(f[1]);
                reg = r;
                semi = (s != null) ? s : r.deriveFont(Font.BOLD);
                return;
            }
        }

        private static Font read(String name) {
            File file = new File("fonts", name);
            if (!file.isFile()) {
                return null;
            }
            try {
                return Font.createFont(Font.TRUETYPE_FONT, file);
            } catch (Exception e) {
                return null;
            }
        }

        static Font get(boolean bold, float size) {
            load();
            if (reg != null) {
                return (bold ? semi : reg).deriveFont(size);
            }
            return new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, Math.round(size));
        }
    }

    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Color alpha(Color c, float a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, Math.round(a * 255f))));
    }

    static float drawTracked(Graphics2D g, String s, float x, float y, float gap) {
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            g.drawString(String.valueOf(c), x, y);
            x += fm.charWidth(c) + gap;
        }
        return x;
    }

    static float trackedWidth(FontMetrics fm, String s, float gap) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += fm.charWidth(s.charAt(i)) + (i < s.length() - 1 ? gap : 0);
        }
        return w;
    }

    static String fit(String s, FontMetrics fm, int maxW) {
        if (s == null) {
            return "";
        }
        if (fm.stringWidth(s) <= maxW) {
            return s;
        }
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxW) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }

    static String nz(String s) {
        return s == null ? "" : s;
    }

    // =====================================================================
    //  Header pieces
    // =====================================================================

    /** Thin outlined "CINEVA" wordmark + divider + tagline + little popcorn bucket. */
    static final class WordMark extends JComponent {
        WordMark() {
            setPreferredSize(new Dimension(330, 46));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            Font big = Fonts.get(false, 30f);
            FontRenderContext frc = g.getFontRenderContext();
            float x = 0;
            float base = 34;

            g.setColor(new Color(205, 208, 215));
            g.setStroke(new BasicStroke(1.15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (char ch : "CINEVA".toCharArray()) {
                TextLayout tl = new TextLayout(String.valueOf(ch), big, frc);
                Shape outline = tl.getOutline(AffineTransform.getTranslateInstance(x, base));
                g.draw(outline);
                x += tl.getAdvance() + 4;
            }

            x += 10;
            g.setColor(LINE);
            g.fill(new Rectangle2D.Float(x, 9, 1, 28));
            x += 14;

            g.setFont(Fonts.get(true, 11f));
            g.setColor(TEXT);
            drawTracked(g, "CINEVA", x, 21, 1.5f);
            g.setFont(Fonts.get(false, 11f));
            g.setColor(DIM);
            g.drawString("Built on Love", x, 35);
            float tagW = g.getFontMetrics().stringWidth("Built on Love");

            // popcorn bucket
            float px = x + tagW + 14;
            g.setColor(new Color(205, 208, 215));
            g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D bucket = new Path2D.Float();
            bucket.moveTo(px + 1, 17);
            bucket.lineTo(px + 19, 17);
            bucket.lineTo(px + 16.5f, 36);
            bucket.lineTo(px + 3.5f, 36);
            bucket.closePath();
            g.draw(bucket);
            g.draw(new Line2D.Float(px + 7, 17, px + 7.8f, 36));
            g.draw(new Line2D.Float(px + 13, 17, px + 12.2f, 36));
            g.draw(new Ellipse2D.Float(px + 1, 8, 7, 7));
            g.draw(new Ellipse2D.Float(px + 6.5f, 5, 7.5f, 7.5f));
            g.draw(new Ellipse2D.Float(px + 12, 8, 7, 7));
            g.dispose();
        }
    }

    /** Rounded two-segment switch: MOVIES | MY BOOKINGS. */
    static final class PillSwitch extends JComponent {
        private final String[] labels;
        private final IntConsumer onClick;
        private int selected = 0;
        private int hover = -1;

        PillSwitch(String[] labels, IntConsumer onClick) {
            this.labels = labels;
            this.onClick = onClick;
            setPreferredSize(new Dimension(300, 42));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { hover = indexAt(e.getX()); repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    int i = indexAt(e.getX());
                    if (i >= 0) {
                        onClick.accept(i);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private int indexAt(int x) {
            int segW = (getWidth() - 6) / labels.length;
            int i = (x - 3) / Math.max(1, segW);
            return (i < 0 || i >= labels.length) ? -1 : i;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(LINE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));

            int segW = (w - 6) / labels.length;
            g.setFont(Fonts.get(true, 11.5f));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < labels.length; i++) {
                float sx = 3 + i * segW;
                if (i == selected) {
                    g.setColor(Color.WHITE);
                    g.fill(new RoundRectangle2D.Float(sx, 3, segW, h - 6, h, h));
                }
                g.setColor(i == selected ? new Color(12, 12, 14) : (hover == i ? TEXT : DIM));
                float tw = trackedWidth(fm, labels[i], 1.6f);
                float tx = sx + (segW - tw) / 2f;
                float ty = (h + fm.getAscent() - fm.getDescent()) / 2f;
                drawTracked(g, labels[i], tx, ty, 1.6f);
            }
            g.dispose();
        }
    }

    /** Rounded "WELCOME, NAME" pill with an avatar and a logout (power) button. */
    static final class WelcomePill extends JComponent {
        private final String name;
        private final Runnable onLogout;
        private boolean hoverPower = false;
        private final int width;

        WelcomePill(String name, Runnable onLogout) {
            this.name = name == null ? "" : name;
            this.onLogout = onLogout;
            FontMetrics fm = getFontMetrics(Fonts.get(true, 11f));
            float tw = trackedWidth(fm, label(), 1.6f);
            width = Math.round(8 + 30 + 12 + tw + 18 + 30 + 10);
            setPreferredSize(new Dimension(width, 44));
            setToolTipText("Log out");
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    boolean h = e.getX() > getWidth() - 50;
                    if (h != hoverPower) {
                        hoverPower = h;
                        setCursor(h ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                        repaint();
                    }
                }
                @Override public void mouseExited(MouseEvent e) {
                    hoverPower = false;
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
                @Override public void mousePressed(MouseEvent e) {
                    if (e.getX() > getWidth() - 50) {
                        onLogout.run();
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private String label() {
            return "WELCOME, " + name.toUpperCase();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(LINE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));

            // avatar
            float ad = 30;
            float ax = 7;
            float ay = (h - ad) / 2f;
            g.setPaint(new GradientPaint(ax, ay, new Color(255, 221, 120), ax + ad, ay + ad, new Color(235, 140, 80)));
            g.fill(new Ellipse2D.Float(ax, ay, ad, ad));
            String ini = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            g.setFont(Fonts.get(true, 13f));
            g.setColor(new Color(40, 24, 10));
            FontMetrics fm0 = g.getFontMetrics();
            g.drawString(ini, ax + (ad - fm0.stringWidth(ini)) / 2f, ay + (ad + fm0.getAscent() - fm0.getDescent()) / 2f);

            // text
            g.setFont(Fonts.get(true, 11f));
            g.setColor(TEXT);
            FontMetrics fm = g.getFontMetrics();
            drawTracked(g, label(), ax + ad + 12, (h + fm.getAscent() - fm.getDescent()) / 2f, 1.6f);

            // power icon
            float cx = w - 27;
            float cy = h / 2f;
            g.setColor(hoverPower ? new Color(255, 110, 110) : DIM);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Float(cx - 6.5f, cy - 6.5f, 13, 13, 120, 300, Arc2D.OPEN));
            g.draw(new Line2D.Float(cx, cy - 8f, cx, cy - 1f));
            g.dispose();
        }
    }

    // =====================================================================
    //  Toolbar pieces
    // =====================================================================

    /** "▶ NOW SHOWING" heading with a small film counter. */
    static final class Heading extends JComponent {
        private int count = 0;

        Heading() {
            FontMetrics fm = getFontMetrics(Fonts.get(false, 26f));
            setPreferredSize(new Dimension(46 + Math.round(trackedWidth(fm, "NOW SHOWING", 2f)) + 12 + 96, 50));
        }

        void setCount(int c) {
            count = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int h = getHeight();

            // icon: rounded square with play triangle
            float s = 30;
            float iy = (h - s) / 2f;
            g.setColor(new Color(150, 154, 164));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new RoundRectangle2D.Float(1, iy, s, s, 9, 9));
            Path2D tri = new Path2D.Float();
            tri.moveTo(11.5f, iy + 8.5f);
            tri.lineTo(11.5f, iy + s - 8.5f);
            tri.lineTo(23, iy + s / 2f);
            tri.closePath();
            g.fill(tri);

            g.setFont(Fonts.get(false, 26f));
            g.setColor(new Color(150, 154, 164));
            FontMetrics fm = g.getFontMetrics();
            float ty = (h + fm.getAscent() - fm.getDescent()) / 2f;
            float end = drawTracked(g, "NOW SHOWING", 46, ty, 2f);

            // counter pill
            String txt = count + (count == 1 ? " FILM" : " FILMS");
            g.setFont(Fonts.get(true, 10.5f));
            FontMetrics f2 = g.getFontMetrics();
            float tw = trackedWidth(f2, txt, 1.4f);
            float pw = tw + 22;
            float px = end + 12;
            g.setColor(LINE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(px, (h - 24) / 2f, pw, 24, 24, 24));
            g.setColor(DIM);
            drawTracked(g, txt, px + 11, (h + f2.getAscent() - f2.getDescent()) / 2f, 1.4f);
            g.dispose();
        }
    }

    /** Rounded search field with a magnifier icon. */
    static final class SearchBox extends JPanel {
        final JTextField field = new JTextField();
        private boolean focused = false;

        SearchBox() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(210, 38));
            field.setOpaque(false);
            field.setBorder(new EmptyBorder(0, 40, 0, 14));
            field.setFont(Fonts.get(false, 13f));
            field.setForeground(TEXT);
            field.setCaretColor(TEXT);
            field.putClientProperty("JTextField.placeholderText", "Search movies");
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
            add(field, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(14, 14, 18));
            g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, h, h));
            g.setColor(focused ? new Color(150, 154, 164) : LINE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));

            // magnifier
            g.setColor(focused ? TEXT : DIM);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = 22;
            float cy = h / 2f - 1;
            g.draw(new Ellipse2D.Float(cx - 6, cy - 6, 11, 11));
            g.draw(new Line2D.Float(cx + 3, cy + 4, cx + 7, cy + 8));
            g.dispose();
        }
    }

    /** Genre filter chip. */
    static final class Chip extends JComponent {
        final String text;
        Runnable onClick;
        private boolean active;
        private boolean hover;

        Chip(String text) {
            this.text = text;
            FontMetrics fm = getFontMetrics(Fonts.get(true, 11.5f));
            setPreferredSize(new Dimension(Math.round(trackedWidth(fm, text.toUpperCase(), 1.2f)) + 32, 38));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    if (onClick != null) {
                        onClick.run();
                    }
                }
            });
        }

        void setActive(boolean a) {
            active = a;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth();
            int h = getHeight();
            if (active) {
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, h, h));
            } else {
                g.setColor(hover ? new Color(110, 114, 124) : LINE);
                g.setStroke(new BasicStroke(1f));
                g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));
            }
            g.setFont(Fonts.get(true, 11.5f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(active ? new Color(12, 12, 14) : (hover ? TEXT : DIM));
            String t = text.toUpperCase();
            float tw = trackedWidth(fm, t, 1.2f);
            drawTracked(g, t, (w - tw) / 2f, (h + fm.getAscent() - fm.getDescent()) / 2f, 1.2f);
            g.dispose();
        }
    }

    // =====================================================================
    //  The poster strip
    // =====================================================================

    /**
     * Names row + edge-to-edge posters + control bar.
     * The cursor position decides which poster opens up; widths ease towards their targets
     * every frame so everything glides.
     */
    static final class PosterStrip extends JComponent {
        private static final int SIDE = 28;
        private static final int NAMES_H = 82;
        private static final int BAR_H = 100;
        private static final int MIN_W = 96;          // narrowest a poster may get before the strip pans
        private static final float EXPAND = 2.8f;     // how much wider the hovered poster gets
        private static final float FOLLOW = 7f;       // how quickly the strip follows the cursor (lower = slower and smoother)

        private final BiConsumer<Integer, String> onBook;
        private final javax.swing.Timer timer;
        private long lastNanos = System.nanoTime();

        private List<Movie> movies = new ArrayList<>();
        private boolean[] match = new boolean[0];
        private final Map<Integer, BufferedImage> posters = new HashMap<>();
        private final Set<Integer> requested = new HashSet<>();

        // animation state
        private float focus = -1f;       // the "lens": which poster is open (in poster units, 0.5 = middle of the first)
        private float pan = 0f;
        private float[] hot = new float[0];
        private float[] weights = new float[0];
        private int autoIdx = 0;
        private float autoTimer = 0f;
        private boolean playing = true;
        private float disc = 0f;

        private boolean mouseIn = false;
        private int mouseX = -1;
        private int mouseY = -1;
        private int hoverIdx = -1;
        private int btnIdx = -1;          // poster that currently shows the Book Tickets button

        // geometry
        private float[] slotX = new float[0];
        private float[] slotW = new float[0];
        private int viewX;
        private int viewW;
        private int stripY;
        private int stripH;
        private int barY;
        private final Rectangle2D.Float bookR = new Rectangle2D.Float();

        PosterStrip(BiConsumer<Integer, String> onBook) {
            this.onBook = onBook;
            setOpaque(true);
            setBackground(BG);

            timer = new javax.swing.Timer(16, e -> {
                long now = System.nanoTime();
                float dt = Math.min(0.05f, (now - lastNanos) / 1e9f);
                lastNanos = now;
                step(dt);
                repaint();
            });

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { track(e); }
                @Override public void mouseDragged(MouseEvent e) { track(e); }
                @Override public void mouseExited(MouseEvent e) { mouseIn = false; hoverIdx = -1; }
                @Override public void mousePressed(MouseEvent e) { click(e); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        // ---------- public API ----------
        void setMovies(List<Movie> list) {
            movies = new ArrayList<>(list);
            match = new boolean[movies.size()];
            java.util.Arrays.fill(match, true);
            hot = new float[movies.size()];
            weights = new float[movies.size()];
            slotX = new float[movies.size()];
            slotW = new float[movies.size()];
            autoIdx = 0;
            autoTimer = 0f;
            focus = -1f;
            pan = 0f;
            loadPosters();
            repaint();
        }

        void setMatches(boolean[] m) {
            if (m.length != movies.size()) {
                return;
            }
            match = m;
            if (autoIdx < match.length && !match[autoIdx]) {
                autoIdx = nextMatching(1);
            }
            repaint();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            lastNanos = System.nanoTime();
            timer.start();
        }

        @Override
        public void removeNotify() {
            timer.stop();
            super.removeNotify();
        }

        // ---------- poster loading ----------
        private void loadPosters() {
            for (Movie m : movies) {
                final Movie movie = m;
                final int id = movie.getMovieId();
                if (!requested.add(id)) {
                    continue;
                }
                new SwingWorker<BufferedImage, Void>() {
                    @Override
                    protected BufferedImage doInBackground() {
                        return readPoster(movie);
                    }

                    @Override
                    protected void done() {
                        try {
                            BufferedImage img = get();
                            if (img != null) {
                                posters.put(id, img);
                                repaint();
                            }
                        } catch (Exception ignored) {
                            // no poster: the fallback artwork is drawn instead
                        }
                    }
                }.execute();
            }
        }

        private static String norm(String s) {
            return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
        }

        /** IMDb ratings, matched by movie title (no database change needed). */
        private static final String[][] RATINGS = {
                {"12thfail", "8.7"}, {"drishyam", "8.4"}, {"englishvinglish", "7.8"},
                {"inception", "8.8"}, {"interstellar", "8.7"}, {"manichitrathazhu", "8.7"},
                {"pastlives", "7.8"}, {"premam", "8.3"}, {"lunchbox", "7.8"}
        };

        private static String ratingFor(String title) {
            String n = norm(title);
            for (String[] r : RATINGS) {
                if (n.equals(r[0]) || n.contains(r[0])) {
                    return r[1];
                }
            }
            return null;
        }

        private static boolean isImage(String name) {
            String n = name.toLowerCase();
            return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".gif");
        }

        private static List<File> imageIndex;

        /** Every image file in the project folder (built once). Skips build output and libraries. */
        private static synchronized List<File> imageIndex() {
            if (imageIndex == null) {
                imageIndex = new ArrayList<>();
                collect(new File(System.getProperty("user.dir")), 0, imageIndex);
            }
            return imageIndex;
        }

        private static void collect(File dir, int depth, List<File> out) {
            File[] files = dir.listFiles();
            if (files == null || depth > 6 || out.size() > 3000) {
                return;
            }
            for (File f : files) {
                String n = f.getName().toLowerCase();
                if (f.isDirectory()) {
                    if (n.equals("bin") || n.equals("target") || n.equals("build") || n.equals("out")
                            || n.equals("lib") || n.equals("node_modules") || n.startsWith(".")) {
                        continue;
                    }
                    collect(f, depth + 1, out);
                } else if (isImage(n)) {
                    out.add(f);
                }
            }
        }

        static BufferedImage readPoster(Movie m) {
            String title = nz(m.getTitle());
            String stored = nz(m.getPosterPath()).trim();
            try {
                // 1) the exact path stored with the movie
                if (!stored.isEmpty()) {
                    File f = new File(stored);
                    if (f.isFile()) {
                        BufferedImage img = tryLoad(f);
                        if (img != null) {
                            return img;
                        }
                    }
                    java.net.URL u = PosterStrip.class.getResource("/" + stored.replace('\\', '/'));
                    if (u != null) {
                        BufferedImage img = ImageIO.read(u);
                        if (img != null) {
                            return shrink(img);
                        }
                    }
                }

                // 2) search the whole project for a file that looks right
                String wantName = stored.isEmpty() ? "" : norm(new File(stored.replace('\\', '/')).getName());
                String want = norm(title);
                File best = null;
                int bestScore = 0;
                for (File f : imageIndex()) {
                    String nm = f.getName();
                    String base = norm(nm.substring(0, nm.lastIndexOf('.')));
                    int score = 0;
                    if (!wantName.isEmpty() && norm(nm).equals(wantName)) {
                        score = 4;
                    } else if (!want.isEmpty() && base.equals(want)) {
                        score = 3;
                    } else if (!want.isEmpty() && base.length() >= 4 && (want.contains(base) || base.contains(want))) {
                        score = 2;
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        best = f;
                    }
                }
                if (best != null) {
                    BufferedImage img = tryLoad(best);
                    if (img != null) {
                        return img;
                    }
                }
            } catch (Exception ex) {
                System.err.println("[Poster] error for '" + title + "': " + ex);
            }
            System.err.println("[Poster] no usable poster for '" + title + "' (stored path: '" + stored
                    + "'; searched in " + System.getProperty("user.dir") + ")");
            return null;
        }

        /** Reads an image; falls back to the AWT toolkit for JPEGs that ImageIO cannot decode. */
        private static BufferedImage tryLoad(File f) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    return shrink(img);
                }
            } catch (Exception ex) {
                System.err.println("[Poster] ImageIO could not read " + f + ": " + ex.getMessage());
            }
            try {
                ImageIcon icon = new ImageIcon(f.getPath());
                if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                    BufferedImage b = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = b.createGraphics();
                    g.drawImage(icon.getImage(), 0, 0, null);
                    g.dispose();
                    return shrink(b);
                }
            } catch (Exception ex) {
                System.err.println("[Poster] toolkit could not read " + f + ": " + ex.getMessage());
            }
            System.err.println("[Poster] file exists but is not a readable image: " + f
                    + " (re-save it as a normal .jpg or .png)");
            return null;
        }

        /** Keeps memory low: posters never need to be taller than ~900px. */
        private static BufferedImage shrink(BufferedImage src) {
            if (src == null) {
                return null;
            }
            int maxH = 900;
            if (src.getHeight() <= maxH) {
                return src;
            }
            int h = maxH;
            int w = Math.round(src.getWidth() * (h / (float) src.getHeight()));
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();
            return out;
        }

        // ---------- mouse ----------
        private void computeGeometry() {
            int w = getWidth();
            int h = getHeight();
            viewX = SIDE;
            viewW = Math.max(100, w - 2 * SIDE);
            stripY = NAMES_H;
            stripH = Math.max(160, h - NAMES_H - BAR_H);
            barY = stripY + stripH;
        }

        private float prevCx() { return viewX + viewW / 2f - 64; }
        private float playCx() { return viewX + viewW / 2f; }
        private float nextCx() { return viewX + viewW / 2f + 64; }
        private float ctrlCy() { return barY + 42; }

        private boolean inCircle(float px, float py, float cx, float cy, float r) {
            float dx = px - cx;
            float dy = py - cy;
            return dx * dx + dy * dy <= r * r;
        }

        private int controlAt(int x, int y) {
            if (inCircle(x, y, prevCx(), ctrlCy(), 22)) return 0;
            if (inCircle(x, y, playCx(), ctrlCy(), 28)) return 1;
            if (inCircle(x, y, nextCx(), ctrlCy(), 22)) return 2;
            return -1;
        }

        private int slotAt(int x) {
            for (int i = 0; i < slotX.length; i++) {
                if (x >= slotX[i] && x < slotX[i] + slotW[i]) {
                    return i;
                }
            }
            return -1;
        }

        private void track(MouseEvent e) {
            computeGeometry();
            mouseX = e.getX();
            mouseY = e.getY();
            mouseIn = mouseX >= viewX && mouseX <= viewX + viewW && mouseY >= 0 && mouseY <= barY;
            int idx = mouseIn ? slotAt(mouseX) : -1;
            boolean hand = controlAt(mouseX, mouseY) >= 0 || idx >= 0;
            setCursor(hand ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        }

        private void click(MouseEvent e) {
            computeGeometry();
            int c = controlAt(e.getX(), e.getY());
            if (c == 0) {
                autoIdx = nextMatching(-1);
                autoTimer = 0f;
                return;
            }
            if (c == 1) {
                playing = !playing;
                return;
            }
            if (c == 2) {
                autoIdx = nextMatching(1);
                autoTimer = 0f;
                return;
            }
            int idx = (e.getY() <= barY) ? slotAt(e.getX()) : -1;
            if (idx >= 0 && idx < movies.size()) {
                Movie m = movies.get(idx);
                onBook.accept(m.getMovieId(), m.getTitle());
            }
        }

        private int nextMatching(int dir) {
            int n = movies.size();
            int i = autoIdx;
            for (int k = 0; k < n; k++) {
                i = (i + dir + n) % n;
                if (match[i]) {
                    return i;
                }
            }
            return autoIdx;
        }

        // ---------- animation ----------
        private static float ease(float dt, float speed) {
            return 1f - (float) Math.exp(-dt * speed);
        }

        private void step(float dt) {
            computeGeometry();
            int n = movies.size();
            if (n == 0) {
                return;
            }
            if (slotX.length != n) {
                slotX = new float[n];
                slotW = new float[n];
                hot = new float[n];
                weights = new float[n];
            }
            if (match.length != n) {
                match = new boolean[n];
                java.util.Arrays.fill(match, true);
            }
            if (autoIdx >= n) {
                autoIdx = 0;
            }

            float total = Math.max(viewW, n * MIN_W);
            float baseW = total / n;
            float maxPan = Math.max(0f, total - viewW);

            if (!mouseIn && playing) {
                autoTimer += dt;
                if (autoTimer > 3.6f) {
                    autoTimer = 0f;
                    autoIdx = nextMatching(1);
                }
            }

            float frac = mouseIn
                    ? Math.max(0f, Math.min(1f, (mouseX - viewX) / (float) viewW))
                    : (n > 1 ? autoIdx / (float) (n - 1) : 0f);
            pan += (frac * maxPan - pan) * ease(dt, 4f);

            // The "lens" (which poster is open) is tied straight to where the cursor is along the strip:
            // far left = first poster open, far right = last poster open, in between it moves in
            // step with the cursor. A little easing keeps it smooth without lagging behind.
            float focusT;
            if (mouseIn) {
                float mf = Math.max(0f, Math.min(1f, (mouseX - viewX) / (float) viewW));
                focusT = 0.5f + mf * (n - 1);
            } else {
                focusT = autoIdx + 0.5f;
            }
            if (focus < -0.5f) {
                focus = focusT;
            } else {
                focus += (focusT - focus) * ease(dt, FOLLOW);
            }

            float sigma = 0.68f;
            float sum = 0f;
            for (int i = 0; i < n; i++) {
                float d = (i + 0.5f - focus) / sigma;
                weights[i] = 1f + EXPAND * (float) Math.exp(-d * d);
                sum += weights[i];
            }
            float cum = 0f;
            for (int i = 0; i < n; i++) {
                slotW[i] = total * weights[i] / sum;
                slotX[i] = viewX + cum - pan;
                cum += slotW[i];
            }

            hoverIdx = mouseIn ? slotAt(mouseX) : -1;
            int fi = hoverIdx >= 0 ? hoverIdx : (mouseIn ? -1 : autoIdx);
            for (int i = 0; i < n; i++) {
                hot[i] += ((i == fi ? 1f : 0f) - hot[i]) * ease(dt, 6f);
            }
            disc += dt * (playing ? 1.4f : 0.15f);
        }

        // ---------- painting ----------
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(BG);
            g.fillRect(0, 0, w, h);

            computeGeometry();
            int n = movies.size();
            if (n == 0) {
                g.setFont(Fonts.get(false, 16f));
                g.setColor(DIM);
                String msg = "No movies to show yet.";
                g.drawString(msg, (w - g.getFontMetrics().stringWidth(msg)) / 2, h / 2);
                g.dispose();
                return;
            }
            if (slotX.length != n || slotX[0] == 0f && slotW[0] == 0f) {
                step(0f);
            }
            bookR.setRect(0, 0, 0, 0);

            drawNames(g, n);
            drawPosters(g, n);
            drawBar(g);
            g.dispose();
        }

        private void drawNames(Graphics2D g, int n) {
            Shape oldClip = g.getClip();
            for (int i = 0; i < n; i++) {
                float x = slotX[i];
                float sw = slotW[i];
                float l = Math.max(x, viewX);
                float r = Math.min(x + sw, viewX + viewW);
                if (r - l < 40) {
                    continue;
                }
                float hv = hot[i];
                boolean ok = match[i];
                Movie m = movies.get(i);

                g.setClip(oldClip);
                g.clipRect(Math.round(l), 0, Math.round(r - l) - 6, NAMES_H);

                float tx = Math.max(x, viewX) + 12;
                g.setFont(Fonts.get(true, 13.5f + 1.5f * hv));
                FontMetrics fm = g.getFontMetrics();
                Color nameCol = ok ? mix(new Color(138, 142, 152), TEXT, hv) : new Color(62, 64, 72);
                g.setColor(nameCol);
                g.drawString(fit(m.getTitle(), fm, Math.round(r - tx) - 14), tx, 34);

                g.setFont(Fonts.get(false, 11f));
                g.setColor(ok ? mix(new Color(86, 89, 98), new Color(176, 180, 190), hv) : new Color(48, 50, 56));
                g.drawString(fit(nz(m.getGenre()), g.getFontMetrics(), Math.round(r - tx) - 14), tx, 52);

                // little play glyph
                Path2D tri = new Path2D.Float();
                tri.moveTo(tx, 62);
                tri.lineTo(tx, 72);
                tri.lineTo(tx + 8.5f, 67);
                tri.closePath();
                g.setColor(ok ? mix(new Color(70, 72, 80), TEXT, hv) : new Color(44, 46, 52));
                g.fill(tri);
            }
            g.setClip(oldClip);
        }

        private void drawPosters(Graphics2D g, int n) {
            Shape oldClip = g.getClip();
            btnIdx = hoverIdx >= 0 ? hoverIdx : (!mouseIn ? autoIdx : -1);
            RoundRectangle2D shape = new RoundRectangle2D.Float(viewX, stripY, viewW, stripH, 20, 20);
            g.clip(shape);

            for (int i = 0; i < n; i++) {
                float x = slotX[i];
                float sw = slotW[i];
                if (x + sw < viewX || x > viewX + viewW) {
                    continue;
                }
                float hv = hot[i];
                boolean ok = match[i];
                Movie m = movies.get(i);

                float px = x + 1.5f;
                float pw = sw - 3f;
                Shape beforeSlot = g.getClip();
                g.clipRect(Math.round(px), stripY, Math.round(pw), stripH);

                BufferedImage img = posters.get(m.getMovieId());
                if (img != null) {
                    drawCover(g, img, px, stripY, pw, stripH);
                } else {
                    drawFallback(g, m, px, stripY, pw, stripH);
                }

                // shade posters that are not in focus, so the focused one pops
                int shade = Math.round((1f - hv) * (ok ? 70 : 190));
                if (shade > 0) {
                    g.setColor(new Color(5, 5, 7, shade));
                    g.fill(new Rectangle2D.Float(px, stripY, pw, stripH));
                }

                // details on the focused poster
                if (hv > 0.03f && pw > 190f) {
                    drawDetails(g, m, i, px, pw, hv);
                }
                g.setClip(beforeSlot);
            }

            g.setClip(oldClip);
            g.setColor(new Color(255, 255, 255, 22));
            g.setStroke(new BasicStroke(1f));
            g.draw(shape);
        }

        private void drawDetails(Graphics2D g, Movie m, int i, float x, float w, float hv) {
            float bottom = stripY + stripH;
            float gh = 210f;
            g.setPaint(new GradientPaint(0, bottom - gh, new Color(0, 0, 0, 0),
                    0, bottom, new Color(0, 0, 0, Math.round(235 * hv))));
            g.fill(new Rectangle2D.Float(x, bottom - gh, w, gh));

            float bx = x + 22;
            float by = bottom - 24 - 42;
            int a = Math.round(255 * hv);

            g.setFont(Fonts.get(true, 10.5f));
            g.setColor(new Color(150, 205, 255, a));
            float gEnd = drawTracked(g, nz(m.getGenre()).toUpperCase(), bx, by - 50, 2f);

            String rating = ratingFor(m.getTitle());
            if (rating != null) {
                float sx = gEnd + 16;
                float sy = by - 54;
                g.setColor(new Color(255, 196, 62, a));
                g.fill(star(sx + 6, sy, 6.5f));
                g.setFont(Fonts.get(true, 12.5f));
                g.drawString(rating, sx + 17, by - 50);
            }

            g.setFont(Fonts.get(false, 13f));
            g.setColor(new Color(236, 237, 241, a));
            g.drawString(m.getDuration() + " min   ·   " + nz(m.getLanguage()), bx, by - 24);

            if (i == btnIdx && hv > 0.55f) {
                bookR.setRect(bx, by, 156, 42);
                boolean over = bookR.contains(mouseX, mouseY);
                g.setColor(over ? Color.WHITE : new Color(236, 237, 241));
                g.fill(new RoundRectangle2D.Float(bx, by, 156, 42, 42, 42));
                g.setFont(Fonts.get(true, 13f));
                g.setColor(new Color(12, 12, 14));
                FontMetrics fm = g.getFontMetrics();
                String t = "Book Tickets";
                g.drawString(t, bx + (156 - fm.stringWidth(t)) / 2f - 6, by + (42 + fm.getAscent() - fm.getDescent()) / 2f);
                // small arrow
                float ax = bx + (156 + fm.stringWidth(t)) / 2f + 2;
                float ay = by + 21;
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(ax, ay, ax + 9, ay));
                g.draw(new Line2D.Float(ax + 5, ay - 4, ax + 9, ay));
                g.draw(new Line2D.Float(ax + 5, ay + 4, ax + 9, ay));
            }
        }

        private static Shape star(float cx, float cy, float r) {
            Path2D p = new Path2D.Float();
            for (int i = 0; i < 10; i++) {
                double ang = -Math.PI / 2 + i * Math.PI / 5;
                float rad = (i % 2 == 0) ? r : r * 0.42f;
                float px = cx + (float) Math.cos(ang) * rad;
                float py = cy + (float) Math.sin(ang) * rad;
                if (i == 0) {
                    p.moveTo(px, py);
                } else {
                    p.lineTo(px, py);
                }
            }
            p.closePath();
            return p;
        }

        private void drawCover(Graphics2D g, BufferedImage img, float x, float y, float w, float h) {
            float pw = img.getWidth();
            float ph = img.getHeight();
            float scale = Math.max(w / pw, h / ph);
            float sw = w / scale;
            float sh = h / scale;
            float sx = (pw - sw) / 2f;
            float sy = (ph - sh) / 2f;
            g.drawImage(img,
                    Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h),
                    Math.round(sx), Math.round(sy), Math.round(sx + sw), Math.round(sy + sh), null);
        }

        /** Used when a movie has no poster file: a tinted gradient with a big initial. */
        private void drawFallback(Graphics2D g, Movie m, float x, float y, float w, float h) {
            String title = nz(m.getTitle());
            float hue = (Math.abs(title.hashCode()) % 360) / 360f;
            Color c1 = Color.getHSBColor(hue, 0.55f, 0.46f);
            Color c2 = Color.getHSBColor((hue + 0.1f) % 1f, 0.6f, 0.10f);
            g.setPaint(new GradientPaint(x, y, c1, x, y + h, c2));
            g.fill(new Rectangle2D.Float(x, y, w, h));

            String ini = title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase();
            g.setFont(Fonts.get(true, Math.min(w * 0.9f, h * 0.4f)));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(new Color(255, 255, 255, 38));
            g.drawString(ini, x + (w - fm.stringWidth(ini)) / 2f, y + h * 0.5f + fm.getAscent() * 0.3f);
        }

        private void drawBar(Graphics2D g) {
            float top = barY + 14;

            // left: title block
            int f0 = hoverIdx >= 0 ? hoverIdx : autoIdx;
            String meta = "";
            if (f0 >= 0 && f0 < movies.size()) {
                Movie fm0 = movies.get(f0);
                meta = nz(fm0.getGenre()) + "  ·  " + fm0.getDuration() + " min  ·  " + nz(fm0.getLanguage());
            }
            g.setFont(Fonts.get(true, 24f));
            g.setColor(TEXT);
            g.drawString("Now Showing", viewX + 4, top + 28);
            g.setFont(Fonts.get(false, 12.5f));
            g.setColor(DIM);
            String sub = SHOW_DATES.isEmpty() ? meta : (SHOW_DATES + "  ·  " + meta);
            g.drawString(sub, viewX + 4, top + 50);

            // center: controls
            float cy = ctrlCy();
            drawRoundBtn(g, prevCx(), cy, 20, 0);
            drawRoundBtn(g, playCx(), cy, 26, 1);
            drawRoundBtn(g, nextCx(), cy, 20, 2);

            // right: spinning disc + now playing
            float dcx = viewX + viewW - 34;
            float dcy = barY + 42;
            drawDisc(g, dcx, dcy, 26);

            int fi = hoverIdx >= 0 ? hoverIdx : autoIdx;
            if (fi >= 0 && fi < movies.size()) {
                String title = nz(movies.get(fi).getTitle());
                g.setFont(Fonts.get(false, 11f));
                g.setColor(DIM);
                String label = "Now playing";
                float rx = dcx - 44;
                g.drawString(label, rx - g.getFontMetrics().stringWidth(label), dcy - 4);
                g.setFont(Fonts.get(true, 14f));
                g.setColor(TEXT);
                FontMetrics fm = g.getFontMetrics();
                String t = fit(title, fm, 240);
                g.drawString(t, rx - fm.stringWidth(t), dcy + 16);
            }
        }

        private void drawRoundBtn(Graphics2D g, float cx, float cy, float r, int kind) {
            boolean over = controlAt(mouseX, mouseY) == kind;
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(over ? new Color(170, 174, 184) : LINE);
            g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            if (over) {
                g.setColor(new Color(255, 255, 255, 14));
                g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            }
            g.setColor(TEXT);
            if (kind == 1) {
                if (playing) {
                    g.fill(new RoundRectangle2D.Float(cx - 6, cy - 7.5f, 4, 15, 2, 2));
                    g.fill(new RoundRectangle2D.Float(cx + 2, cy - 7.5f, 4, 15, 2, 2));
                } else {
                    Path2D tri = new Path2D.Float();
                    tri.moveTo(cx - 5, cy - 8);
                    tri.lineTo(cx - 5, cy + 8);
                    tri.lineTo(cx + 8, cy);
                    tri.closePath();
                    g.fill(tri);
                }
            } else {
                float d = kind == 0 ? -1 : 1;
                Path2D tri = new Path2D.Float();
                tri.moveTo(cx - d * 1f, cy - 6);
                tri.lineTo(cx - d * 1f, cy + 6);
                tri.lineTo(cx + d * 6f, cy);
                tri.closePath();
                g.fill(tri);
                g.fill(new Rectangle2D.Float(cx - d * 6f - (d > 0 ? 0 : 1.6f), cy - 6, 1.6f, 12));
            }
        }

        private void drawDisc(Graphics2D g, float cx, float cy, float r) {
            AffineTransform old = g.getTransform();
            g.translate(cx, cy);
            g.rotate(disc);
            Color[] cols = {
                    new Color(110, 210, 255), new Color(190, 140, 255), new Color(255, 130, 180),
                    new Color(255, 190, 110), new Color(120, 230, 170), new Color(110, 210, 255)
            };
            for (int i = 0; i < 6; i++) {
                g.setColor(cols[i]);
                g.fill(new Arc2D.Float(-r, -r, r * 2, r * 2, i * 60, 60, Arc2D.PIE));
            }
            g.setColor(new Color(10, 10, 12, 215));
            g.fill(new Ellipse2D.Float(-r * 0.78f, -r * 0.78f, r * 1.56f, r * 1.56f));
            g.setColor(new Color(255, 255, 255, 40));
            g.setStroke(new BasicStroke(1f));
            g.draw(new Ellipse2D.Float(-r * 0.55f, -r * 0.55f, r * 1.1f, r * 1.1f));
            g.setColor(BG);
            g.fill(new Ellipse2D.Float(-r * 0.2f, -r * 0.2f, r * 0.4f, r * 0.4f));
            g.setTransform(old);
        }
    }
}