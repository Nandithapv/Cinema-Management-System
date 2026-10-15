package views;

import dao.MovieDAO;
import models.Movie;
import models.User;
import services.SessionManager;
import views.components.MovieCardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Customer Storefront & Catalog Dashboard.
 * Created by Member 6 (Nanditha P V: Customer Portal & Master Integration Lead).
 * 
 * Redesigned to match modern mobile cinema app inspiration:
 * - Brand Navigation Header with crisp visible headings & "🎟️ My Bookings" pill
 * - Live Title Search & Quick Category Pills (All, Sci-Fi, Thriller, Romance, Action)
 * - Horizontal Featured Movie Lineup showcasing posters side-by-side
 * - Direct routing into ShowSelectionDialog -> SeatSelectionFrame -> Payment -> Receipt
 */
public class CustomerHomeFrame extends JFrame {

    private int currentUserId = 1;
    private String currentUserName = "Customer";

    private final List<Movie> allMovies = new ArrayList<>();
    private final JPanel cardsPanel = new JPanel();
    private final JTextField txtSearch = new JTextField(18);
    private final JComboBox<String> cmbGenre = new JComboBox<>(new String[] {
        "All Genres", "Action", "Sci-Fi", "Drama", "Comedy", "Romance", "Thriller", "Horror"
    });

    private String activeGenreFilter = "All";
    private final List<JButton> filterPillButtons = new ArrayList<>();

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

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 760);
        setMinimumSize(new Dimension(880, 600));
        setLocationRelativeTo(null);
        Theme.applyDarkTheme(this);
        getContentPane().setLayout(new BorderLayout());

        // 1. Top Brand & Profile Header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Content Container
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);

        // Filter / Search Bar (Search + Quick Genre Pills)
        centerContainer.add(createFilterBar(), BorderLayout.NORTH);

        // Movie Showcase Section
        JPanel showcasePanel = new JPanel(new BorderLayout());
        showcasePanel.setOpaque(false);
        showcasePanel.setBorder(new EmptyBorder(12, 24, 18, 24));

        // Section Title: "NOW SHOWING"
        JPanel sectionTitleRow = new JPanel(new BorderLayout());
        sectionTitleRow.setOpaque(false);
        sectionTitleRow.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel lblSection = new JLabel("🔥  NOW SHOWING");
        lblSection.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblSection.setForeground(Theme.TEXT_LIGHT); // Crisp white heading

        JLabel lblSub = new JLabel("Explore movies & book your experience");
        lblSub.setFont(Theme.FONT_BODY);
        lblSub.setForeground(Theme.TEXT_MUTED);

        sectionTitleRow.add(lblSection, BorderLayout.WEST);
        sectionTitleRow.add(lblSub, BorderLayout.EAST);
        showcasePanel.add(sectionTitleRow, BorderLayout.NORTH);

        // Movie Lineup Cards Grid
        cardsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 24, 24));
        cardsPanel.setBackground(Theme.BG_DARK);
        cardsPanel.setBorder(new EmptyBorder(4, 4, 16, 4));

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(18);

        showcasePanel.add(scrollPane, BorderLayout.CENTER);
        centerContainer.add(showcasePanel, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);
    }

    /** Creates top brand banner with user profile and action buttons */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(Theme.BG_HEADER);
        header.setBorder(new EmptyBorder(16, 26, 16, 26));

        // Brand Logo & Title
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandPanel.setOpaque(false);

        JLabel brandIcon = new JLabel("🍿");
        brandIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        brandPanel.add(brandIcon);

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel brand = new JLabel("CINEVA");
        brand.setFont(Theme.FONT_BRAND);
        brand.setForeground(Theme.TEXT_LIGHT);
        textStack.add(brand);

        JLabel tag = new JLabel("EXPERIENCE CINEMA");
        tag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tag.setForeground(Theme.ACCENT_CYAN);
        textStack.add(tag);

        brandPanel.add(textStack);
        header.add(brandPanel, BorderLayout.WEST);

        // Right Action Area (Greeting, My Bookings, Logout)
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightActions.setOpaque(false);

        JLabel lblGreeting = new JLabel("👋 Welcome, " + currentUserName);
        lblGreeting.setFont(Theme.FONT_BODY_BOLD);
        lblGreeting.setForeground(Theme.TEXT_LIGHT);
        rightActions.add(lblGreeting);

        // "My Bookings" button with clear visible styling
        JButton btnBookings = new JButton("🎟️  My Bookings");
        Theme.styleSecondaryButton(btnBookings);
        btnBookings.addActionListener(e -> openBookingHistory());
        rightActions.add(btnBookings);

        JButton btnLogout = new JButton("Logout");
        Theme.styleDangerButton(btnLogout);
        btnLogout.addActionListener(e -> handleLogout());
        rightActions.add(btnLogout);

        header.add(rightActions, BorderLayout.EAST);
        return header;
    }

    /** Creates modern search bar and category pill chips */
    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BG_DARK);
        bar.setBorder(new EmptyBorder(16, 26, 4, 26));

        // Search Input (Left)
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBox.setOpaque(false);

        JLabel lblSearchIcon = new JLabel("🔍");
        lblSearchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        lblSearchIcon.setForeground(Theme.TEXT_MUTED);
        searchBox.add(lblSearchIcon);

        txtSearch.setFont(Theme.FONT_BODY);
        txtSearch.setBackground(Theme.BG_INPUT);
        txtSearch.setForeground(Theme.TEXT_LIGHT);
        txtSearch.setCaretColor(Color.WHITE);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        txtSearch.setToolTipText("Search movie titles...");
        txtSearch.addActionListener(e -> applyFilter());
        searchBox.add(txtSearch);

        JButton btnSearch = new JButton("Search");
        Theme.stylePrimaryButton(btnSearch);
        btnSearch.addActionListener(e -> applyFilter());
        searchBox.add(btnSearch);

        bar.add(searchBox, BorderLayout.WEST);

        // Quick Category Filter Pills (Right)
        JPanel pillsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pillsPanel.setOpaque(false);

        String[] categories = { "All", "Action", "Sci-Fi", "Thriller", "Romance" };
        for (String cat : categories) {
            JButton pill = createPillButton(cat);
            filterPillButtons.add(pill);
            pillsPanel.add(pill);
        }

        bar.add(pillsPanel, BorderLayout.EAST);
        return bar;
    }

    private JButton createPillButton(String category) {
        JButton pill = new JButton(category);
        pill.setFont(Theme.FONT_SMALL_BOLD);
        pill.setFocusPainted(false);
        pill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updatePillStyle(pill, category.equalsIgnoreCase(activeGenreFilter));

        pill.addActionListener(e -> {
            activeGenreFilter = category;
            for (JButton b : filterPillButtons) {
                updatePillStyle(b, b.getText().equalsIgnoreCase(activeGenreFilter));
            }
            applyFilter();
        });
        return pill;
    }

    private void updatePillStyle(JButton pill, boolean isActive) {
        if (isActive) {
            pill.setBackground(Theme.PRIMARY);
            pill.setForeground(Color.WHITE);
            pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.PRIMARY_HOVER, 1, true),
                new EmptyBorder(6, 14, 6, 14)
            ));
        } else {
            pill.setBackground(Theme.BG_PILL);
            pill.setForeground(Theme.TEXT_MUTED);
            pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 14, 6, 14)
            ));
        }
    }

    /**
     * Loads movies from MovieDAO; populates realistic fallback movies if database is empty or offline.
     */
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
            allMovies.add(new Movie(1, "Interstellar", "Sci-Fi", 169, "English", "assets/posters/interstellar.jpg", true));
            allMovies.add(new Movie(2, "Inception", "Sci-Fi", 148, "English", "assets/posters/inception.jpg", true));
            allMovies.add(new Movie(3, "Drishyam", "Thriller", 160, "Malayalam", "assets/posters/drishyam.jpg", true));
            allMovies.add(new Movie(4, "Premam", "Romance", 156, "Malayalam", "assets/posters/premam.jpg", true));
            allMovies.add(new Movie(5, "The Dark Knight", "Action", 152, "English", "assets/posters/dark_knight.jpg", true));
            allMovies.add(new Movie(6, "Avatar: The Way of Water", "Sci-Fi", 192, "English", "assets/posters/avatar.jpg", true));
        }

        renderMovieCards(allMovies);
    }

    private void renderMovieCards(List<Movie> movies) {
        cardsPanel.removeAll();

        if (movies.isEmpty()) {
            JPanel emptyBox = new JPanel();
            emptyBox.setLayout(new BoxLayout(emptyBox, BoxLayout.Y_AXIS));
            emptyBox.setOpaque(false);
            emptyBox.setBorder(new EmptyBorder(60, 40, 60, 40));

            JLabel lblEmpty = new JLabel("🎬 No movies found matching your filter.");
            lblEmpty.setFont(Theme.FONT_TITLE);
            lblEmpty.setForeground(Theme.TEXT_LIGHT);
            lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblHelp = new JLabel("Try searching another keyword or select 'All'.");
            lblHelp.setFont(Theme.FONT_BODY);
            lblHelp.setForeground(Theme.TEXT_MUTED);
            lblHelp.setAlignmentX(Component.CENTER_ALIGNMENT);

            emptyBox.add(lblEmpty);
            emptyBox.add(Box.createVerticalStrut(8));
            emptyBox.add(lblHelp);
            cardsPanel.add(emptyBox);
        } else {
            for (Movie m : movies) {
                MovieCardPanel card = new MovieCardPanel(
                    m.getMovieId(), m.getTitle(), m.getGenre(), m.getDuration(), m.getLanguage(), m.getPosterPath(),
                    (movieId, movieTitle) -> onMovieBookClicked(movieId, movieTitle)
                );
                cardsPanel.add(card);
            }
        }

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    /** Triggered when the user clicks 'Book Tickets' on a movie card */
    private void onMovieBookClicked(int movieId, String movieTitle) {
        User user = SessionManager.getCurrentUser();
        ShowSelectionDialog dialog = new ShowSelectionDialog(this, movieId, movieTitle, user);
        dialog.setVisible(true);
    }

    /** Applies search text & category filter */
    private void applyFilter() {
        String query = txtSearch.getText().trim().toLowerCase();
        String selectedPill = activeGenreFilter;

        List<Movie> filtered = new ArrayList<>();
        for (Movie m : allMovies) {
            boolean matchesSearch = query.isEmpty() || m.getTitle().toLowerCase().contains(query);
            boolean matchesCategory = "All".equalsIgnoreCase(selectedPill) ||
                (m.getGenre() != null && m.getGenre().toLowerCase().contains(selectedPill.toLowerCase()));

            if (matchesSearch && matchesCategory) {
                filtered.add(m);
            }
        }
        renderMovieCards(filtered);
    }

    /** Opens Renjitha's BookingHistoryFrame */
    private void openBookingHistory() {
        User user = SessionManager.getCurrentUser();
        int uid = (user != null) ? user.getUserId() : currentUserId;
        BookingHistoryFrame historyFrame = new BookingHistoryFrame(uid);
        historyFrame.setVisible(true);
    }

    /** Logs out current user and returns to Login */
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

    /** Standalone preview runner */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CustomerHomeFrame().setVisible(true));
    }
}
