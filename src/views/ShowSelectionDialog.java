package views;

import dao.ShowDAO;
import models.Show;
import models.User;
import services.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Show Selection Modal Dialog.
 * Connects Movie browsing (Member 6: Nanditha) to Seat Selection (Member 4: Shada).
 * 
 * Lists upcoming showtimes for the selected movie from ShowDAO,
 * and launches SeatSelectionFrame upon selection.
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
        new String[] { "Show ID", "Screen", "Date", "Time", "Ticket Price" }, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };

    private final JTable table = new JTable(tableModel);

    public ShowSelectionDialog(Frame parent, int movieId, String movieTitle, User user, OnShowSelectedListener listener) {
        super(parent, "Select Showtime — " + movieTitle, true);
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

    private void initUI() {
        setSize(700, 440);
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(15, 15));

        // 1. Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.BG_HEADER);
        headerPanel.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel("🎬 " + movieTitle);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.TEXT_LIGHT);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        JLabel lblHint = new JLabel("Choose an upcoming showtime");
        lblHint.setFont(Theme.FONT_BODY);
        lblHint.setForeground(Theme.TEXT_MUTED);
        headerPanel.add(lblHint, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // 2. Shows Table
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(34);
        table.setFont(Theme.FONT_BODY);
        table.setBackground(Theme.BG_CARD);
        table.setForeground(Theme.TEXT_LIGHT);
        table.setGridColor(Theme.BORDER_COLOR);
        table.getTableHeader().setBackground(Theme.BG_HEADER);
        table.getTableHeader().setForeground(Theme.TEXT_LIGHT);
        table.getTableHeader().setFont(Theme.FONT_BODY_BOLD);

        // Center align table contents
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Action Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        bottomPanel.setBackground(Theme.BG_DARK);

        JButton btnCancel = new JButton("Back");
        Theme.styleSecondaryButton(btnCancel);
        btnCancel.addActionListener(e -> dispose());
        bottomPanel.add(btnCancel);

        JButton btnProceed = new JButton("Select Seats →");
        Theme.stylePrimaryButton(btnProceed);
        btnProceed.addActionListener(e -> handleProceed());
        bottomPanel.add(btnProceed);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void handleProceed() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select a showtime from the list to continue.",
                "No Show Selected", JOptionPane.WARNING_MESSAGE);
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

        // Launch Shada's SeatSelectionFrame directly
        User activeUser = (currentUser != null) ? currentUser : SessionManager.getCurrentUser();
        SeatSelectionFrame seatFrame = new SeatSelectionFrame(chosen, activeUser);
        seatFrame.setVisible(true);
    }

    /**
     * Loads shows from ShowDAO; falls back to sample demo shows if database has no upcoming shows.
     */
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
                        s.getScreenName() != null ? s.getScreenName() : "Screen 1",
                        s.getShowDate(),
                        s.getShowTime(),
                        "Rs. " + String.format("%.2f", s.getTicketPrice() != null ? s.getTicketPrice() : BigDecimal.valueOf(150.0))
                    });
                }
            }
        } catch (Throwable t) {
            System.err.println("[ShowSelectionDialog] Error loading shows from DAO: " + t.getMessage());
        }

        // Fallback demo shows if database has none scheduled for this movie
        if (showsList.isEmpty()) {
            addDemoShow(101, 1, "Screen 1", 1, "10:00:00", 150.00);
            addDemoShow(102, 1, "Screen 1", 1, "14:00:00", 150.00);
            addDemoShow(103, 1, "Screen 1", 2, "18:30:00", 200.00);
            addDemoShow(104, 1, "Screen 1", 2, "21:45:00", 180.00);
        }
    }

    private void addDemoShow(int id, int screenId, String screenName, int daysAhead, String timeStr, double price) {
        Date showDate = Date.valueOf(LocalDate.now().plusDays(daysAhead));
        Time showTime = Time.valueOf(timeStr);
        BigDecimal ticketPrice = BigDecimal.valueOf(price);

        Show s = new Show(id, movieId, screenId, showDate, showTime, ticketPrice);
        s.setMovieTitle(movieTitle);
        s.setScreenName(screenName);

        showsList.add(s);
        tableModel.addRow(new Object[] {
            s.getShowId(),
            s.getScreenName(),
            s.getShowDate(),
            s.getShowTime(),
            String.format("Rs. %.2f", price)
        });
    }

    /** Standalone preview runner */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame dummy = new JFrame();
            Theme.applyDarkTheme(dummy);
            ShowSelectionDialog dialog = new ShowSelectionDialog(dummy, 1, "Interstellar");
            dialog.setVisible(true);
        });
    }
}
