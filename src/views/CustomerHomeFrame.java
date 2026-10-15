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
 * Features:
 * - Brand Navigation Header with User Greeting, "My Bookings" & "Logout"
 * - Live Title Search & Category/Genre Filtering
 * - Dynamic Grid of Movie Cards with Posters & Metadata
 * - Direct routing into ShowSelectionDialog -> SeatSelectionFrame -> Payment -> History
 * 
 * OOP Viva Concept:
 * - Loose Coupling & High Cohesion:
 *   High cohesion inside presentation components; loose coupling with DAOs & Service managers.
 */
public class CustomerHomeFrame extends JFrame {

    private int currentUserId = 1;
    private String currentUserName = "Customer";

    private final List<Movie> allMovies = new ArrayList<>();
    private final JPanel cardsPanel = new JPanel();
    private final JTextField txtSearch = new JTextField(16);
    private final JComboBox<String> cmbGenre = new JComboBox<>(new String[] {
        "All Genres", "Action", "Sci-Fi", "Drama", "Comedy", "Romance", "Thriller", "Horror"
    });

    /** No-arg constructor required by team specs and LoginFrame redirection */
    public CustomerHomeFrame() {
        super("CineMagic — Cinema Management System");
        resolveCurrentUser();
        initUI();
        loadMovies();
    }

    /**
     * Resolves currently authenticated user from SessionManager.
     */
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
        setSize(1020, 720);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        Theme.applyDarkTheme(this);
        getContentPane().setLayout(new BorderLayout());

        // 1. Top Brand & Profile Header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Main Content Container (Filter bar + Movies Scroll Grid)
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);
        centerContainer.add(createFilterBar(), BorderLayout.NORTH);

        // Grid for Movie Cards
        cardsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));
        cardsPanel.setBackground(Theme.BG_DARK);
        cardsPanel.setBorder(new EmptyBorder(10, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        centerContainer.add(scrollPane, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);
    }

    /** Creates top brand banner with user profile and action buttons */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(Theme.BG_HEADER);
        header.setBorder(new EmptyBorder(14, 24, 14, 24));

        // Brand Logo & Title
        JLabel brand = new JLabel("🎬 CINEMAGIC");
        brand.setFont(Theme.FONT_BRAND);
        brand.setForeground(Theme.PRIMARY);
        header.add(brand, BorderLayout.WEST);

        // User Greeting & Right Action Buttons
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightActions.setOpaque(false);

        JLabel lblGreeting = new JLabel("Welcome, " + currentUserName);
        lblGreeting.setFont(Theme.FONT_BODY_BOLD);
        lblGreeting.setForeground(Theme.TEXT_LIGHT);
        rightActions.add(lblGreeting);

        JButton btnBookings = new JButton("🎟️ My Bookings");
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

    /** Creates search and category filter controls */
    private JPanel createFilterBar() {
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        filterBar.setBackground(Theme.BG_DARK);
        filterBar.setBorder(new EmptyBorder(5, 20, 5, 20));

        JLabel lblSearch = new JLabel("Search Movies:");
        lblSearch.setFont(Theme.FONT_BODY_BOLD);
        lblSearch.setForeground(Theme.TEXT_LIGHT);
        filterBar.add(lblSearch);

        txtSearch.setFont(Theme.FONT_BODY);
        txtSearch.setBackground(Theme.BG_INPUT);
        txtSearch.setForeground(Theme.TEXT_LIGHT);
        txtSearch.setCaretColor(Color.WHITE);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1),
            new EmptyBorder(6, 10, 6, 10)
        ));
        txtSearch.addActionListener(e -> applyFilter());
        filterBar.add(txtSearch);

        JLabel lblGenre = new JLabel("Genre:");
        lblGenre.setFont(Theme.FONT_BODY_BOLD);
        lblGenre.setForeground(Theme.TEXT_LIGHT);
        filterBar.add(lblGenre);

        cmbGenre.setFont(Theme.FONT_BODY);
        cmbGenre.setBackground(Theme.BG_INPUT);
        cmbGenre.setForeground(Theme.TEXT_LIGHT);
        cmbGenre.addActionListener(e -> applyFilter());
        filterBar.add(cmbGenre);

        JButton btnFilter = new JButton("Filter");
        Theme.styleSecondaryButton(btnFilter);
        btnFilter.addActionListener(e -> applyFilter());
        filterBar.add(btnFilter);

        JButton btnReset = new JButton("Reset");
        Theme.styleSecondaryButton(btnReset);
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            cmbGenre.setSelectedIndex(0);
            applyFilter();
        });
        filterBar.add(btnReset);

        return filterBar;
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
            allMovies.add(new Movie(1, "Interstellar", "Sci-Fi", 169, "English", "default_poster.png", true));
            allMovies.add(new Movie(2, "Inception", "Sci-Fi", 148, "English", "default_poster.png", true));
            allMovies.add(new Movie(3, "Drishyam", "Thriller", 160, "Malayalam", "default_poster.png", true));
            allMovies.add(new Movie(4, "Premam", "Romance", 156, "Malayalam", "default_poster.png", true));
            allMovies.add(new Movie(5, "The Dark Knight", "Action / Crime", 152, "English", "default_poster.png", true));
            allMovies.add(new Movie(6, "Avatar: The Way of Water", "Sci-Fi / Adventure", 192, "English", "default_poster.png", true));
        }

        renderMovieCards(allMovies);
    }

    private void renderMovieCards(List<Movie> movies) {
        cardsPanel.removeAll();

        if (movies.isEmpty()) {
            JLabel lblEmpty = new JLabel("No movies match your filter criteria.", SwingConstants.CENTER);
            lblEmpty.setFont(Theme.FONT_SUBTITLE);
            lblEmpty.setForeground(Theme.TEXT_MUTED);
            cardsPanel.add(lblEmpty);
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
        String selectedGenre = (String) cmbGenre.getSelectedItem();

        List<Movie> filtered = new ArrayList<>();
        for (Movie m : allMovies) {
            boolean matchesSearch = query.isEmpty() || m.getTitle().toLowerCase().contains(query);
            boolean matchesGenre = "All Genres".equals(selectedGenre) ||
                (m.getGenre() != null && m.getGenre().toLowerCase().contains(selectedGenre.toLowerCase()));

            if (matchesSearch && matchesGenre) {
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
