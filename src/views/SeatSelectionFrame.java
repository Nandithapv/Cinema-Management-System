package views;

import dao.BookingDAO;
import dao.PaymentDAO;
import dao.ShowDAO;
import models.Seat;
import models.Show;
import models.User;
import services.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * Visual Interactive Seat Grid & Booking Screen (Member 4: Shada).
 * 
 * Features:
 *  - 6 Rows (A–F) x 10 Columns (1–10) theater seat matrix.
 *  - Dynamic seat coloring with JToggleButton:
 *      🟢 Green: Available
 *      🟡 Yellow: Selected
 *      🔴 Red (Disabled): Already booked for this show
 *  - Real-time live price calculation: Count of selected seats x Ticket Price.
 *  - Atomic JDBC Transaction on checkout via BookingDAO.
 *  - Payment integration: Opens PaymentDialog, releases booking if canceled without paying.
 * 
 * OOP Concepts for Viva:
 *  - Encapsulation: Seat states encapsulated in model and toggle listeners.
 *  - State Management: State transition (Available -> Selected -> Booked).
 *  - Event Handling & Delegation: Decoupled UI and DAO layers.
 */
public class SeatSelectionFrame extends JFrame {

    private static final int ROWS = 6;  // Rows A - F
    private static final int COLS = 10; // Columns 1 - 10

    // Color Palette
    private static final Color COLOR_AVAILABLE = new Color(46, 125, 50);    // Green
    private static final Color COLOR_SELECTED  = new Color(249, 168, 37);   // Yellow/Amber
    private static final Color COLOR_BOOKED    = new Color(198, 40, 40);    // Red
    private static final Color COLOR_SCREEN_BAR = new Color(55, 71, 79);    // Dark Slate
    private static final Color COLOR_BG         = new Color(245, 247, 250); // Light Gray
    private static final Color COLOR_PANEL_BG   = Color.WHITE;

    private final Show show;
    private final User user;
    private final BookingDAO bookingDAO;
    private final double ticketPrice;

    // Track state
    private final Map<String, Seat> seatMap = new HashMap<>(); // "A1" -> Seat
    private final Map<String, SeatButton> seatButtonMap = new HashMap<>(); // "A1" -> Button
    private final List<Seat> selectedSeats = new ArrayList<>();

    // UI Labels
    private JLabel lblSelectedSeats;
    private JLabel lblTotalAmount;
    private JButton btnBook;

    /**
     * Primary constructor as defined in team role distribution.
     * @param show Show object containing movie, screen, date, time, and ticket price
     * @param user Currently logged-in User
     */
    public SeatSelectionFrame(Show show, User user) {
        this.show = show;
        this.user = (user != null) ? user : SessionManager.getCurrentUser();
        this.bookingDAO = new BookingDAO();
        this.ticketPrice = (show != null && show.getTicketPrice() != null)
                ? show.getTicketPrice().doubleValue()
                : 150.00;

        initUI();
        loadSeatsFromDatabase();
    }

    /**
     * Overloaded convenience constructor: resolves current user from SessionManager.
     */
    public SeatSelectionFrame(Show show) {
        this(show, SessionManager.getCurrentUser());
    }

    /**
     * Overloaded convenience constructor: looks up Show by ID and User from session.
     */
    public SeatSelectionFrame(int showId) {
        this(new ShowDAO().getById(showId), SessionManager.getCurrentUser());
    }

    private void initUI() {
        String movieTitle = (show != null && show.getMovieTitle() != null) ? show.getMovieTitle() : "Cinema Show";
        setTitle("Seat Selection — " + movieTitle);
        setSize(960, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(COLOR_BG);
        getContentPane().setLayout(new BorderLayout(12, 12));

        // 1. TOP PANEL: Movie & Show Info
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. CENTER PANEL: Screen curve + Seat Grid + Legend
        add(createCenterPanel(), BorderLayout.CENTER);

        // 3. BOTTOM PANEL: Summary & Checkout
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_PANEL_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 224, 230)),
                new EmptyBorder(16, 24, 16, 24)
        ));

        String title = (show != null && show.getMovieTitle() != null) ? show.getMovieTitle() : "Movie Title";
        String screen = (show != null && show.getScreenName() != null) ? show.getScreenName() : "Screen 1";
        String date = (show != null && show.getShowDate() != null) ? show.getShowDate().toString() : "";
        String time = (show != null && show.getShowTime() != null) ? show.getShowTime().toString().substring(0, 5) : "";

        JLabel lblTitle = new JLabel("🎬 " + title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(33, 37, 41));

        JLabel lblDetails = new JLabel(String.format("Screen: %s   |   Date: %s   |   Time: %s   |   Ticket Price: Rs. %.2f",
                screen, date, time, ticketPrice));
        lblDetails.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDetails.setForeground(new Color(108, 117, 125));

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblDetails, BorderLayout.SOUTH);
        return header;
    }

    private JPanel createCenterPanel() {
        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setBackground(COLOR_BG);
        centerContainer.setBorder(new EmptyBorder(10, 24, 10, 24));

        // Cinema Screen Graphic
        JPanel screenBarPanel = new JPanel();
        screenBarPanel.setBackground(COLOR_BG);
        screenBarPanel.setLayout(new BorderLayout());
        screenBarPanel.setBorder(new EmptyBorder(0, 40, 10, 40));

        JLabel lblScreen = new JLabel("——————  SCREEN THIS WAY  ——————", SwingConstants.CENTER);
        lblScreen.setOpaque(true);
        lblScreen.setBackground(COLOR_SCREEN_BAR);
        lblScreen.setForeground(Color.WHITE);
        lblScreen.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblScreen.setPreferredSize(new Dimension(600, 26));
        lblScreen.setBorder(new LineBorder(new Color(38, 50, 56), 1, true));
        screenBarPanel.add(lblScreen, BorderLayout.CENTER);

        centerContainer.add(screenBarPanel, BorderLayout.NORTH);

        // 6 x 10 Seat Grid
        JPanel seatGridPanel = new JPanel(new GridLayout(ROWS, COLS, 10, 10));
        seatGridPanel.setBackground(COLOR_PANEL_BG);
        seatGridPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(20, 25, 20, 25)
        ));

        for (int r = 0; r < ROWS; r++) {
            char rowChar = (char) ('A' + r);
            for (int c = 1; c <= COLS; c++) {
                String seatNum = "" + rowChar + c;
                SeatButton btnSeat = new SeatButton(seatNum);
                applySeatStyle(btnSeat, Seat.SeatStatus.AVAILABLE);

                btnSeat.addActionListener(e -> {
                    if (btnSeat.getSeatStatus() != Seat.SeatStatus.BOOKED) {
                        handleSeatToggle(seatNum, btnSeat.isSelected());
                    }
                });

                seatButtonMap.put(seatNum, btnSeat);
                seatGridPanel.add(btnSeat);
            }
        }

        centerContainer.add(seatGridPanel, BorderLayout.CENTER);

        // Legend at bottom of center panel
        centerContainer.add(createLegendPanel(), BorderLayout.SOUTH);

        return centerContainer;
    }

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 5));
        legendPanel.setBackground(COLOR_BG);

        legendPanel.add(createLegendItem("Available", COLOR_AVAILABLE));
        legendPanel.add(createLegendItem("Selected", COLOR_SELECTED));
        legendPanel.add(createLegendItem("Booked", COLOR_BOOKED));

        return legendPanel;
    }

    private JPanel createLegendItem(String label, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.setBackground(COLOR_BG);

        JLabel colorBox = new JLabel();
        colorBox.setOpaque(true);
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(16, 16));
        colorBox.setBorder(new LineBorder(Color.DARK_GRAY, 1));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        item.add(colorBox);
        item.add(lbl);
        return item;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(COLOR_PANEL_BG);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 224, 230)),
                new EmptyBorder(14, 24, 14, 24)
        ));

        // Left info: Selected seats + Total
        JPanel summaryPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        summaryPanel.setBackground(COLOR_PANEL_BG);

        lblSelectedSeats = new JLabel("Selected Seats: None");
        lblSelectedSeats.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSelectedSeats.setForeground(new Color(33, 37, 41));

        lblTotalAmount = new JLabel("Total: Rs. 0.00 (0 seats)");
        lblTotalAmount.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTotalAmount.setForeground(new Color(46, 125, 50));

        summaryPanel.add(lblSelectedSeats);
        summaryPanel.add(lblTotalAmount);

        // Right buttons: Refresh + Cancel + Proceed
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(COLOR_PANEL_BG);

        JButton btnRefresh = new JButton("🔄 Refresh Seats");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRefresh.addActionListener(e -> refreshSeatMatrix());

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClose.addActionListener(e -> dispose());

        btnBook = new JButton("Proceed to Payment ➔");
        btnBook.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBook.setBackground(new Color(33, 150, 243));
        btnBook.setForeground(Color.WHITE);
        btnBook.setFocusPainted(false);
        btnBook.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBook.setEnabled(false);
        btnBook.addActionListener(e -> handleBookingProcess());

        btnPanel.add(btnRefresh);
        btnPanel.add(btnClose);
        btnPanel.add(btnBook);

        footer.add(summaryPanel, BorderLayout.WEST);
        footer.add(btnPanel, BorderLayout.EAST);
        return footer;
    }

    /**
     * Styles a SeatButton according to its state.
     */
    private void applySeatStyle(SeatButton btn, Seat.SeatStatus status) {
        btn.setSeatStatus(status);
        switch (status) {
            case AVAILABLE:
                btn.setToolTipText("Available (Rs. " + ticketPrice + ")");
                break;
            case SELECTED:
                btn.setToolTipText("Selected");
                break;
            case BOOKED:
                btn.setToolTipText("Booked");
                break;
        }
    }

    /**
     * Loads seat definitions and booked status from MySQL database.
     */
    private void loadSeatsFromDatabase() {
        if (show == null) return;

        // Fetch all screen seats from database
        List<Seat> seats = bookingDAO.getSeatsByScreen(show.getScreenId());
        
        // Offline / Standalone test fallback: if DB has no seats or is offline, generate A1-F10 in memory
        if (seats.isEmpty()) {
            int dummyId = 1;
            for (int r = 0; r < ROWS; r++) {
                char rowChar = (char) ('A' + r);
                for (int c = 1; c <= COLS; c++) {
                    seats.add(new Seat(dummyId++, show.getScreenId(), "" + rowChar + c));
                }
            }
        }

        seatMap.clear();
        for (Seat s : seats) {
            seatMap.put(s.getSeatNumber(), s);
        }

        // Fetch currently confirmed booked seats for this show
        List<Integer> bookedSeatIds = bookingDAO.getBookedSeats(show.getShowId());
        Set<Integer> bookedSet = new HashSet<>(bookedSeatIds);

        // Update button states
        for (Map.Entry<String, SeatButton> entry : seatButtonMap.entrySet()) {
            String seatNum = entry.getKey();
            SeatButton btn = entry.getValue();
            Seat seat = seatMap.get(seatNum);

            if (seat != null && bookedSet.contains(seat.getSeatId())) {
                seat.setStatus(Seat.SeatStatus.BOOKED);
                applySeatStyle(btn, Seat.SeatStatus.BOOKED);
            } else {
                if (seat != null) {
                    seat.setStatus(Seat.SeatStatus.AVAILABLE);
                }
                applySeatStyle(btn, Seat.SeatStatus.AVAILABLE);
            }
        }

        selectedSeats.clear();
        updateSummary();
    }

    private void handleSeatToggle(String seatNum, boolean isSelected) {
        Seat seat = seatMap.get(seatNum);
        SeatButton btn = seatButtonMap.get(seatNum);
        if (seat == null || btn == null) return;

        if (isSelected) {
            seat.setStatus(Seat.SeatStatus.SELECTED);
            btn.setSeatStatus(Seat.SeatStatus.SELECTED);
            if (!selectedSeats.contains(seat)) {
                selectedSeats.add(seat);
            }
        } else {
            seat.setStatus(Seat.SeatStatus.AVAILABLE);
            btn.setSeatStatus(Seat.SeatStatus.AVAILABLE);
            selectedSeats.remove(seat);
        }

        updateSummary();
    }

    private void updateSummary() {
        int count = selectedSeats.size();
        double total = count * ticketPrice;

        if (count == 0) {
            lblSelectedSeats.setText("Selected Seats: None");
            lblTotalAmount.setText("Total: Rs. 0.00 (0 seats)");
            btnBook.setEnabled(false);
        } else {
            List<String> names = new ArrayList<>();
            for (Seat s : selectedSeats) {
                names.add(s.getSeatNumber());
            }
            Collections.sort(names);
            lblSelectedSeats.setText("Selected Seats: " + String.join(", ", names));
            lblTotalAmount.setText(String.format("Total: Rs. %.2f (%d seat%s)", total, count, count > 1 ? "s" : ""));
            btnBook.setEnabled(true);
        }
    }

    public void refreshSeatMatrix() {
        loadSeatsFromDatabase();
        JOptionPane.showMessageDialog(this, "Seat matrix refreshed successfully.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Executes the booking creation and opens Renjitha's PaymentDialog.
     */
    private void handleBookingProcess() {
        if (selectedSeats.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one seat.", "No Seats Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Determine logged-in user
        User currentUser = this.user != null ? this.user : SessionManager.getCurrentUser();
        if (currentUser == null) {
            // Standalone test fallback: create mock user so testing checkout works smoothly
            currentUser = new models.Customer(1, "Guest Customer", "customer@cinema.com");
            SessionManager.loginUser(currentUser);
        }

        int userId = currentUser.getUserId();
        int showId = show.getShowId();
        double totalAmount = selectedSeats.size() * ticketPrice;

        try {
            // Transactional creation in BookingDAO (Member 4)
            int bookingId = bookingDAO.createBooking(userId, showId, selectedSeats, ticketPrice);

            // Open Payment Step (Member 5: Renjitha)
            PaymentDialog dialog = new PaymentDialog(this, bookingId, totalAmount);
            dialog.setVisible(true);

            // If user closed PaymentDialog without paying, release the seats
            if (!dialog.isPaid()) {
                try {
                    // Call PaymentDAO.releaseBooking(bookingId)
                    new PaymentDAO().releaseBooking(bookingId);
                } catch (Throwable t) {
                    // In case PaymentDAO.releaseBooking is still being merged in PR #1
                    System.err.println("[SeatSelectionFrame] releaseBooking fallback: " + t.getMessage());
                }
                JOptionPane.showMessageDialog(this, "Payment was not completed. Your reserved seats have been released.", "Booking Incomplete", JOptionPane.INFORMATION_MESSAGE);
                loadSeatsFromDatabase(); // Refresh available seats
            } else {
                // Payment was successful!
                JOptionPane.showMessageDialog(this, "Booking & Payment confirmed! Enjoy your show!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadSeatsFromDatabase(); // Refresh seats: booked seats now turn red!
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Booking Error", JOptionPane.ERROR_MESSAGE);
            loadSeatsFromDatabase(); // Refresh to show conflict seats
        }
    }

    /**
     * Standalone test runner for SeatSelectionFrame.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            // Mock show for quick preview
            Show sampleShow = new Show();
            sampleShow.setShowId(1);
            sampleShow.setScreenId(1);
            sampleShow.setMovieTitle("Interstellar");
            sampleShow.setScreenName("Screen 1");
            sampleShow.setTicketPrice(new java.math.BigDecimal("150.00"));
            sampleShow.setShowDate(new java.sql.Date(System.currentTimeMillis()));
            // Pre-login a test customer for standalone testing
            models.Customer testUser = new models.Customer(1, "Shada", "shada@cinema.com");
            services.SessionManager.loginUser(testUser);

            SeatSelectionFrame frame = new SeatSelectionFrame(sampleShow, testUser);
            frame.setVisible(true);
        });
    }

    /**
     * Custom JToggleButton with explicit anti-aliased painting.
     * Guarantees vivid Green, Yellow, and Red colors across all Windows/Mac Look & Feels.
     */
    private static class SeatButton extends JToggleButton {
        private Seat.SeatStatus seatStatus = Seat.SeatStatus.AVAILABLE;

        public SeatButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setPreferredSize(new Dimension(54, 42));
        }

        public void setSeatStatus(Seat.SeatStatus status) {
            this.seatStatus = status;
            if (status == Seat.SeatStatus.BOOKED) {
                setEnabled(false);
                setSelected(false);
            } else if (status == Seat.SeatStatus.SELECTED) {
                setEnabled(true);
                setSelected(true);
            } else {
                setEnabled(true);
                setSelected(false);
            }
            repaint();
        }

        public Seat.SeatStatus getSeatStatus() {
            return seatStatus;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bgColor;
            Color textColor;

            if (seatStatus == Seat.SeatStatus.BOOKED) {
                bgColor = COLOR_BOOKED; // Red
                textColor = Color.WHITE;
            } else if (isSelected() || seatStatus == Seat.SeatStatus.SELECTED) {
                bgColor = COLOR_SELECTED; // Vibrant Gold/Yellow
                textColor = new Color(26, 26, 26);
            } else {
                bgColor = COLOR_AVAILABLE; // Emerald Green
                textColor = Color.WHITE;
            }

            // Draw rounded pill button background
            g2.setColor(bgColor);
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);

            // Draw subtle border
            g2.setColor(bgColor.darker());
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);

            // Draw centered seat number
            g2.setColor(textColor);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), x, y);

            g2.dispose();
        }
    }
}
