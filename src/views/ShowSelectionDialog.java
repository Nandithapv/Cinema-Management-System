package views;

import dao.ShowDAO;
import models.Show;
import models.User;
import services.SessionManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Mobile-App-Style Movie Detail & Showtime Selection Dialog.
 * Connects Movie browsing (Member 6: Nanditha) to Seat Selection (Member 4: Shada).
 * 
 * Inspired by mobile cinema apps (Pinterest):
 * - Large cinematic backdrop banner with poster art & gradient overlay
 * - Badges: Rating ★, Genre chip, Duration & Language tags
 * - Movie synopsis overview
 * - Interactive showtime selection table with high-contrast styling
 * - Seamless handoff to SeatSelectionFrame
 */
public class ShowSelectionDialog extends JDialog {

    @FunctionalInterface
    public interface OnShowSelectedListener {
        void onShowSelected(int showId, String movieTitle, String screenName, String dateTime, double price);
    }

    private final int movieId;
    private final String movieTitle;
    private final User currentUser;
    private final OnShowSelectedListener onShowSelected;
    private final List<Show> showsList = new ArrayList<>();

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[] { "Show ID", "Screen Hall", "Date", "Showtime", "Ticket Price" }, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };

    private final JTable table = new JTable(tableModel);

    public ShowSelectionDialog(Frame parent, int movieId, String movieTitle, User user, OnShowSelectedListener listener) {
        super(parent, "Movie Details — " + movieTitle, true);
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.currentUser = (user != null) ? user : SessionManager.getCurrentUser();
        this.onShowSelected = listener;

        initUI();
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

    private void initUI() {
        setSize(780, 620);
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(0, 0));

        // 1. Top Cinematic Hero Banner with Poster & Details
        add(createHeroBanner(), BorderLayout.NORTH);

        // 2. Middle Showtime Selection Table
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(16, 24, 8, 24));

        JLabel lblSection = new JLabel("🎟️  Available Showtimes");
        lblSection.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblSection.setForeground(Theme.TEXT_LIGHT);
        lblSection.setBorder(new EmptyBorder(0, 0, 10, 0));
        centerPanel.add(lblSection, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(38);
        table.setFont(Theme.FONT_BODY);
        table.setBackground(Theme.BG_CARD);
        table.setForeground(Theme.TEXT_LIGHT);
        table.setSelectionBackground(Theme.PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(Theme.BORDER_COLOR);
        table.setShowGrid(true);

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(Theme.BG_HEADER);
        table.getTableHeader().setForeground(Theme.ACCENT_CYAN);
        table.getTableHeader().setFont(Theme.FONT_BODY_BOLD);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1, true));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Action Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Theme.BG_HEADER);
        bottomBar.setBorder(new EmptyBorder(14, 24, 14, 24));

        JLabel lblNote = new JLabel("💡 Pick a showtime and proceed to choose your seats");
        lblNote.setFont(Theme.FONT_SMALL);
        lblNote.setForeground(Theme.TEXT_MUTED);
        bottomBar.add(lblNote, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        JButton btnBack = new JButton("Back");
        Theme.styleSecondaryButton(btnBack);
        btnBack.addActionListener(e -> dispose());
        actions.add(btnBack);

        JButton btnProceed = new JButton("Select Seats →");
        Theme.stylePrimaryButton(btnProceed);
        btnProceed.addActionListener(e -> handleProceed());
        actions.add(btnProceed);

        bottomBar.add(actions, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    /**
     * Creates cinematic top hero banner featuring the movie poster, title,
     * tags, and synopsis.
     */
    private JPanel createHeroBanner() {
        JPanel hero = new JPanel(new BorderLayout(18, 0));
        hero.setBackground(Theme.BG_HEADER);
        hero.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Poster Image Thumbnail
        BufferedImage img = loadPosterImage(movieTitle);
        JPanel posterThumb = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Shape clip = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setClip(clip);

                if (img != null) {
                    g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setColor(new Color(36, 32, 64));
                    g2.fill(clip);
                    g2.setColor(Theme.ACCENT_CYAN);
                    g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("🎬", (getWidth() - fm.stringWidth("🎬")) / 2, (getHeight() / 2) + 12);
                }
                g2.dispose();
            }
        };
        posterThumb.setPreferredSize(new Dimension(110, 150));
        posterThumb.setOpaque(false);
        hero.add(posterThumb, BorderLayout.WEST);

        // Movie Info Stack
        JPanel infoStack = new JPanel();
        infoStack.setLayout(new BoxLayout(infoStack, BoxLayout.Y_AXIS));
        infoStack.setOpaque(false);

        JLabel lblTitle = new JLabel(movieTitle);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(Theme.TEXT_LIGHT);
        infoStack.add(lblTitle);

        infoStack.add(Box.createVerticalStrut(8));

        // Badges Row (Rating, Genre, Quality)
        JPanel badgesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badgesRow.setOpaque(false);

        badgesRow.add(createBadge(getRating(movieTitle), Theme.ACCENT_GOLD, new Color(251, 191, 36, 40)));
        badgesRow.add(createBadge(getGenre(movieTitle), Theme.ACCENT_CYAN, new Color(0, 245, 212, 35)));
        badgesRow.add(createBadge("4K Dolby Atmos", Theme.PRIMARY_HOVER, new Color(127, 86, 217, 40)));
        badgesRow.add(createBadge("English / Sub", Theme.TEXT_MUTED, new Color(255, 255, 255, 20)));

        infoStack.add(badgesRow);
        infoStack.add(Box.createVerticalStrut(10));

        // Synopsis
        JLabel lblDesc = new JLabel("<html><body style='width: 480px; color: #A5A6C2; font-family: Segoe UI; font-size: 11px;'>"
            + getSynopsis(movieTitle) + "</body></html>");
        infoStack.add(lblDesc);

        hero.add(infoStack, BorderLayout.CENTER);
        return hero;
    }

    private JLabel createBadge(String text, Color textColor, Color bgColor) {
        JLabel badge = new JLabel("  " + text + "  ");
        badge.setFont(Theme.FONT_SMALL_BOLD);
        badge.setForeground(textColor);
        badge.setBackground(bgColor);
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createLineBorder(textColor, 1, true));
        return badge;
    }

    private BufferedImage loadPosterImage(String title) {
        if (title == null) return null;
        String lower = title.toLowerCase();
        String file = null;
        if (lower.contains("interstellar")) file = "assets/posters/interstellar.jpg";
        else if (lower.contains("inception")) file = "assets/posters/inception.jpg";
        else if (lower.contains("dark knight")) file = "assets/posters/dark_knight.jpg";
        else if (lower.contains("drishyam")) file = "assets/posters/drishyam.jpg";
        else if (lower.contains("premam")) file = "assets/posters/premam.jpg";
        else if (lower.contains("avatar")) file = "assets/posters/avatar.jpg";
        else if (lower.contains("spider")) file = "assets/posters/spiderman.jpg";

        if (file != null) {
            File f = new File(file);
            if (f.exists()) {
                try {
                    return ImageIO.read(f);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

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

    private void handleProceed() {
        int selectedRow = table.getSelectedRow();
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
        tableModel.setRowCount(0);
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
                    tableModel.addRow(new Object[] {
                        s.getShowId(),
                        s.getScreenName() != null ? s.getScreenName() : "Screen 1 (Laser 4K)",
                        s.getShowDate(),
                        s.getShowTime(),
                        "Rs. " + String.format("%.2f", s.getTicketPrice() != null ? s.getTicketPrice() : BigDecimal.valueOf(150.0))
                    });
                }
            }
        } catch (Throwable t) {
            System.err.println("[ShowSelectionDialog] Error loading shows from DAO: " + t.getMessage());
        }

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
            JFrame dummy = new JFrame();
            Theme.applyDarkTheme(dummy);
            ShowSelectionDialog dialog = new ShowSelectionDialog(dummy, 1, "Interstellar");
            dialog.setVisible(true);
        });
    }
}
