package views;

import dao.MovieDAO;
import dao.ShowDAO;
import models.Movie;
import models.Show;
import models.User;
import services.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Movie Detail & Showtime Selection Dialog (Member 6: Nanditha), restyled to match the Cineva home page.
 * Connects Movie browsing to Seat Selection (Member 4: Shada).
 *
 * Layout: poster on the left; thin title, tags and synopsis on the right; showtimes as rounded rows
 * you click to select (double-click goes straight to the seats); pill buttons at the bottom.
 * Constructors, listener interface and the hand-off to SeatSelectionFrame are unchanged.
 */
public class ShowSelectionDialog extends JDialog {

    @FunctionalInterface
    public interface OnShowSelectedListener {
        void onShowSelected(int showId, String movieTitle, String screenName, String dateTime, double price);
    }

    private static final Color GOLD = new Color(255, 196, 62);
    private static final Color SKY = new Color(150, 205, 255);
    private static final int ROW_H = 60, GAP = 8;

    private final int movieId;
    private final String movieTitle;
    private final User currentUser;
    private final OnShowSelectedListener onShowSelected;
    private final List<Show> showsList = new ArrayList<>();

    private Movie movie;                       // real genre / duration / language (null if DB unavailable)
    private BufferedImage poster;
    private final JComponent posterBox = new PosterBox();
    private final ShowList list = new ShowList();

    public ShowSelectionDialog(Frame parent, int movieId, String movieTitle, User user, OnShowSelectedListener listener) {
        super(parent, "Movie Details — " + movieTitle, true);
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.currentUser = (user != null) ? user : SessionManager.getCurrentUser();
        this.onShowSelected = listener;

        try {
            this.movie = new MovieDAO().getById(movieId);
        } catch (Throwable t) {
            System.err.println("[ShowSelectionDialog] movie details unavailable: " + t.getMessage());
        }

        initUI();
        loadPoster();
        loadShows();
    }

    public ShowSelectionDialog(Frame parent, int movieId, String movieTitle, User user) {
        this(parent, movieId, movieTitle, user, null);
    }

    public ShowSelectionDialog(Frame parent, int movieId, String movieTitle, OnShowSelectedListener listener) {
        this(parent, movieId, movieTitle, null, listener);
    }

    public ShowSelectionDialog(Frame parent, int movieId, String movieTitle) {
        this(parent, movieId, movieTitle, null, null);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && showsList.isEmpty()) {
            return; // no shows scheduled, message already displayed
        }
        super.setVisible(visible);
    }

    // =====================================================================
    //  Layout
    // =====================================================================
    private void initUI() {
        setSize(CinevaUI.fit(900, 660));
        setMinimumSize(CinevaUI.fit(760, 560));
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(CinevaUI.BG);
        setLayout(new BorderLayout());

        add(createHero(), BorderLayout.NORTH);

        // showtimes
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(14, 36, 0, 36));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(0, 0, 8, 0));
        head.add(CinevaUI.TrackedText.caps("Available showtimes", CinevaUI.DIM), BorderLayout.WEST);
        center.add(head, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setColumnHeaderView(new ListHeader());
        sp.getColumnHeader().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        center.add(sp, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // footer
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(16, 36, 26, 36));

        JLabel hint = new JLabel("Pick a showtime, then choose your seats");
        hint.setFont(CustomerHomeFrame.Fonts.get(false, 13f));
        hint.setForeground(CinevaUI.DIM);
        bottom.add(hint, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        JButton btnBack = CinevaUI.pill("Back", CinevaUI.OUTLINE);
        btnBack.addActionListener(e -> dispose());
        actions.add(btnBack);
        JButton btnProceed = CinevaUI.pill("Select Seats →", CinevaUI.SOLID);
        btnProceed.addActionListener(e -> handleProceed());
        actions.add(btnProceed);
        bottom.add(actions, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    /** Poster on the left; title, tags and synopsis on the right. */
    private JPanel createHero() {
        JPanel hero = new JPanel(new BorderLayout(28, 0));
        hero.setOpaque(false);
        hero.setBorder(new EmptyBorder(32, 36, 6, 36));

        posterBox.setPreferredSize(new Dimension(150, 220));
        JPanel posterWrap = new JPanel(new BorderLayout());
        posterWrap.setOpaque(false);
        posterWrap.add(posterBox, BorderLayout.NORTH);
        hero.add(posterWrap, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        CinevaUI.TrackedText title = CinevaUI.TrackedText.light(movieTitle == null ? "" : movieTitle, 36f,
                Color.WHITE, 0.03f);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(title);
        info.add(Box.createVerticalStrut(12));

        JPanel chips = new CinevaUI.ChipRow();
        chips.add(new CinevaUI.Chip(getRating(movieTitle), GOLD));
        String genre = (movie != null && movie.getGenre() != null && !movie.getGenre().trim().isEmpty())
                ? movie.getGenre() : getGenre(movieTitle);
        chips.add(new CinevaUI.Chip(genre, SKY));
        if (movie != null && movie.getDuration() > 0) {
            chips.add(new CinevaUI.Chip(movie.getDuration() + " min", CinevaUI.DIM));
        }
        if (movie != null && movie.getLanguage() != null && !movie.getLanguage().trim().isEmpty()) {
            chips.add(new CinevaUI.Chip(movie.getLanguage(), CinevaUI.DIM));
        }
        info.add(chips);
        info.add(Box.createVerticalStrut(18));

        JTextArea synopsis = new JTextArea(getSynopsis(movieTitle));
        synopsis.setEditable(false);
        synopsis.setFocusable(false);
        synopsis.setOpaque(false);
        synopsis.setLineWrap(true);
        synopsis.setWrapStyleWord(true);
        synopsis.setBorder(null);
        synopsis.setFont(CustomerHomeFrame.Fonts.get(false, 13.5f));
        synopsis.setForeground(new Color(168, 172, 182));
        synopsis.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(synopsis);

        hero.add(info, BorderLayout.CENTER);
        return hero;
    }

    // =====================================================================
    //  Poster
    // =====================================================================
    private void loadPoster() {
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                Movie m = movie != null ? movie : new Movie(movieId, movieTitle, "", 1, "");
                return CustomerHomeFrame.PosterStrip.readPoster(m);
            }

            @Override
            protected void done() {
                try {
                    poster = get();
                    posterBox.repaint();
                } catch (Exception ignored) {
                    // placeholder stays
                }
            }
        }.execute();
    }

    private class PosterBox extends JComponent {
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w, h, 18, 18);
            g.clip(shape);
            if (poster != null) {
                float s = Math.max(w / (float) poster.getWidth(), h / (float) poster.getHeight());
                int sw = Math.round(w / s);
                int sh = Math.round(h / s);
                int sx = (poster.getWidth() - sw) / 2;
                int sy = (poster.getHeight() - sh) / 2;
                g.drawImage(poster, 0, 0, w, h, sx, sy, sx + sw, sy + sh, null);
            } else {
                String t = movieTitle == null ? "" : movieTitle;
                float hue = (Math.abs(t.hashCode()) % 360) / 360f;
                g.setPaint(new GradientPaint(0, 0, Color.getHSBColor(hue, 0.55f, 0.46f),
                        0, h, Color.getHSBColor((hue + 0.1f) % 1f, 0.6f, 0.10f)));
                g.fillRect(0, 0, w, h);
                String ini = t.isEmpty() ? "?" : t.substring(0, 1).toUpperCase();
                g.setFont(CustomerHomeFrame.Fonts.get(true, h * 0.4f));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(new Color(255, 255, 255, 40));
                g.drawString(ini, (w - fm.stringWidth(ini)) / 2f, h * 0.5f + fm.getAscent() * 0.3f);
            }
            g.setClip(null);
            g.setColor(new Color(255, 255, 255, 26));
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 18, 18));
            g.dispose();
        }
    }

    // =====================================================================
    //  Showtime list
    // =====================================================================
    private static final double[] FRAC = {0.20, 0.34, 0.26, 0.20};   // time, date, screen, price
    private static final String[] HEADS = {"Time", "Date", "Screen", "Price"};

    private static int colX(int c, int w) {
        double s = 0;
        for (int i = 0; i < c; i++) {
            s += FRAC[i];
        }
        return (int) Math.round(s * w);
    }

    private static int colW(int c, int w) {
        return (int) Math.round(FRAC[c] * w);
    }

    private class ListHeader extends JComponent {
        @Override public Dimension getPreferredSize() { return new Dimension(100, 28); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            g.setFont(CustomerHomeFrame.Fonts.get(true, 10.5f));
            g.setColor(CinevaUI.DIM);
            FontMetrics fm = g.getFontMetrics();
            int w = getWidth();
            for (int c = 0; c < HEADS.length; c++) {
                String t = HEADS[c].toUpperCase();
                CustomerHomeFrame.drawTracked(g, t, colX(c, w) + 26, (28 + fm.getAscent() - fm.getDescent()) / 2f, 1.4f);
            }
            g.dispose();
        }
    }

    /** Rounded showtime rows. Click to select, double-click to go to the seats. */
    private class ShowList extends JComponent implements Scrollable {
        private int hover = -1;
        private int selected = -1;

        ShowList() {
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int i = indexAt(e.getY());
                    if (i != hover) { hover = i; repaint(); }
                    setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
                @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    int i = indexAt(e.getY());
                    if (i >= 0) {
                        selected = i;
                        repaint();
                        if (e.getClickCount() == 2) {
                            handleProceed();
                        }
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        int selectedIndex() { return selected; }

        void reset() { selected = -1; hover = -1; revalidate(); repaint(); }

        private int indexAt(int y) {
            int i = y / (ROW_H + GAP);
            return (i >= 0 && i < showsList.size() && y % (ROW_H + GAP) < ROW_H) ? i : -1;
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(400, Math.max(1, showsList.size()) * (ROW_H + GAP));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            int w = getWidth();
            DateTimeFormatter df = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
            for (int i = 0; i < showsList.size(); i++) {
                Show s = showsList.get(i);
                int y = i * (ROW_H + GAP);
                boolean sel = i == selected;
                RoundRectangle2D box = new RoundRectangle2D.Float(0.75f, y + 0.75f, w - 1.5f, ROW_H - 1.5f, 18, 18);
                g.setColor(sel ? new Color(28, 28, 34) : (i == hover ? new Color(22, 22, 28) : new Color(13, 13, 17)));
                g.fill(box);
                g.setStroke(new BasicStroke(sel ? 1.6f : 1f));
                g.setColor(sel ? Color.WHITE : CinevaUI.LINE);
                g.draw(box);

                // selection dot
                g.setStroke(new BasicStroke(1.3f));
                g.setColor(sel ? Color.WHITE : new Color(88, 92, 102));
                g.drawOval(18, y + ROW_H / 2 - 6, 12, 12);
                if (sel) {
                    g.fillOval(21, y + ROW_H / 2 - 3, 6, 6);
                }

                String time = s.getShowTime() != null ? s.getShowTime().toString().substring(0, 5) : "";
                String date = s.getShowDate() != null ? s.getShowDate().toLocalDate().format(df) : "";
                String screen = s.getScreenName() != null ? s.getScreenName() : "Screen 1";
                BigDecimal p = s.getTicketPrice() != null ? s.getTicketPrice() : BigDecimal.valueOf(150.0);
                String price = "Rs. " + String.format("%.2f", p);

                int base = y + ROW_H / 2;
                g.setFont(CustomerHomeFrame.Fonts.get(true, 17f));
                g.setColor(Color.WHITE);
                text(g, time, colX(0, w) + 40, base);
                g.setFont(CustomerHomeFrame.Fonts.get(false, 14f));
                g.setColor(new Color(214, 216, 222));
                text(g, CustomerHomeFrame.fit(date, g.getFontMetrics(), colW(1, w) - 12), colX(1, w) + 26, base);
                g.setColor(new Color(168, 172, 182));
                text(g, CustomerHomeFrame.fit(screen, g.getFontMetrics(), colW(2, w) - 12), colX(2, w) + 26, base);
                g.setFont(CustomerHomeFrame.Fonts.get(true, 14f));
                g.setColor(Color.WHITE);
                text(g, price, colX(3, w) + 26, base);
            }
            g.dispose();
        }

        private void text(Graphics2D g, String s, int x, int centreY) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, x, centreY + (fm.getAscent() - fm.getDescent()) / 2f);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // =====================================================================
    //  Static movie copy (unchanged)
    // =====================================================================
    private String getRating(String title) {
        if (title == null) return "★ 8.5";
        String l = title.toLowerCase();
        if (l.contains("interstellar")) return "★ 8.7 IMDb";
        if (l.contains("inception")) return "★ 8.8 IMDb";
        if (l.contains("dark knight")) return "★ 9.0 IMDb";
        if (l.contains("drishyam")) return "★ 8.6 IMDb";
        if (l.contains("premam")) return "★ 8.3 IMDb";
        if (l.contains("avatar")) return "★ 7.8 IMDb";
        return "★ 8.5 IMDb";
    }

    private String getGenre(String title) {
        if (title == null) return "Sci-Fi / Adventure";
        String l = title.toLowerCase();
        if (l.contains("drishyam")) return "Thriller / Mystery";
        if (l.contains("premam")) return "Romance / Drama";
        if (l.contains("dark knight")) return "Action / Crime";
        return "Sci-Fi / Adventure";
    }

    private String getSynopsis(String title) {
        if (title == null) return "An unforgettable cinematic journey crafted for the big screen.";
        String l = title.toLowerCase();
        if (l.contains("interstellar")) {
            return "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot, Joseph Cooper, is tasked to pilot a spacecraft along with a team of researchers through a wormhole in search of a new home.";
        } else if (l.contains("inception")) {
            return "A skilled thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a CEO.";
        } else if (l.contains("dark knight")) {
            return "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.";
        } else if (l.contains("drishyam")) {
            return "A man goes to extreme lengths to save his family from the dark claws of the law after they commit an unexpected crime.";
        } else if (l.contains("premam")) {
            return "George, a teenager, loves Mary, but his love remains unrequited. As he matures through university and adulthood, he encounters life-changing connections.";
        }
        return "Experience premium cinema with state-of-the-art laser projection, immersive audio, and luxury recliner seating.";
    }

    // =====================================================================
    //  Actions (logic unchanged)
    // =====================================================================
    private void handleProceed() {
        int selectedRow = list.selectedIndex();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select an upcoming showtime from the list to continue.",
                "No Showtime Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Show chosen = showsList.get(selectedRow);
        dispose();

        if (onShowSelected != null) {
            onShowSelected.onShowSelected(
                chosen.getShowId(),
                chosen.getMovieTitle() != null ? chosen.getMovieTitle() : movieTitle,
                chosen.getScreenName() != null ? chosen.getScreenName() : "Screen 1",
                chosen.getShowDate() + " at " + chosen.getShowTime(),
                chosen.getTicketPrice() != null ? chosen.getTicketPrice().doubleValue() : 150.0
            );
        }

        User activeUser = (currentUser != null) ? currentUser : SessionManager.getCurrentUser();
        SeatSelectionFrame seatFrame = new SeatSelectionFrame(chosen, activeUser);
        seatFrame.setVisible(true);
    }

    public void loadShows() {
        showsList.clear();

        try {
            ShowDAO showDAO = new ShowDAO();
            List<Show> dbShows = showDAO.getShowsByMovie(movieId);

            if (dbShows != null && !dbShows.isEmpty()) {
                for (Show s : dbShows) {
                    if (s.getMovieTitle() == null || s.getMovieTitle().isEmpty()) {
                        s.setMovieTitle(movieTitle);
                    }
                    showsList.add(s);
                }
            }
        } catch (Throwable t) {
            System.err.println("[ShowSelectionDialog] Error loading shows from DAO: " + t.getMessage());
        }
        list.reset();

        if (showsList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No upcoming shows are scheduled for this movie.",
                "No Shows Available",
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            com.formdev.flatlaf.FlatDarkLaf.setup();
            JFrame dummy = new JFrame();
            ShowSelectionDialog dialog = new ShowSelectionDialog(dummy, 1, "Interstellar");
            dialog.setVisible(true);
        });
    }
}