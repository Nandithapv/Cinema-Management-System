package views;

import com.formdev.flatlaf.FlatDarkLaf;
import dao.BookingDAO;
import dao.PaymentDAO;
import dao.ShowDAO;
import models.Seat;
import models.Show;
import models.User;
import services.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * Visual Interactive Seat Grid & Booking Screen (Member 4: Shada), restyled to match the Cineva home page.
 *
 * Features:
 *  - 6 Rows (A–F) x 10 Columns (1–10) theater seat matrix.
 *  - Seat states drawn on a dark page:
 *      outlined  : Available
 *      white     : Selected
 *      dimmed    : Already booked for this show
 *  - Real-time live price calculation: Count of selected seats x Ticket Price.
 *  - Atomic JDBC Transaction on checkout via BookingDAO.
 *  - Payment integration: Opens PaymentDialog, releases booking if canceled without paying.
 *
 * OOP Concepts for Viva:
 *  - Encapsulation: Seat states encapsulated in model and toggle listeners.
 *  - State Management: State transition (Available -> Selected -> Booked).
 *  - Event Handling & Delegation: Decoupled UI and DAO layers.
 *
 * Only the look changed: seat, booking and payment logic is exactly as before.
 */
public class SeatSelectionFrame extends JFrame {

    private static final int ROWS = 6;  // Rows A - F
    private static final int COLS = 10; // Columns 1 - 10

    private static final Color BG = CinevaUI.BG;
    private static final Color TEXT = CinevaUI.TEXT;
    private static final Color DIM = CinevaUI.DIM;
    private static final Color LINE = CinevaUI.LINE;

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
        setSize(CinevaUI.fit(1000, 740));
        setMinimumSize(CinevaUI.fit(860, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG);
        getContentPane().setLayout(new BorderLayout());

        // 1. TOP PANEL: Movie & Show Info
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. CENTER PANEL: Screen curve + Seat Grid + Legend
        add(createCenterPanel(), BorderLayout.CENTER);

        // 3. BOTTOM PANEL: Summary & Checkout
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(28, 40, 6, 40));

        String title = (show != null && show.getMovieTitle() != null) ? show.getMovieTitle() : "Movie Title";
        String screen = (show != null && show.getScreenName() != null) ? show.getScreenName() : "Screen 1";
        String date = (show != null && show.getShowDate() != null) ? show.getShowDate().toString() : "";
        String time = (show != null && show.getShowTime() != null) ? show.getShowTime().toString().substring(0, 5) : "";

        CinevaUI.TrackedText lblTitle = CinevaUI.TrackedText.light(title, 32f, Color.WHITE, 0.03f);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(lblTitle);
        header.add(Box.createVerticalStrut(10));

        JPanel chips = new CinevaUI.ChipRow();
        chips.add(new CinevaUI.Chip(screen, new Color(150, 205, 255)));
        if (!date.isEmpty()) {
            chips.add(new CinevaUI.Chip(date, DIM));
        }
        if (!time.isEmpty()) {
            chips.add(new CinevaUI.Chip(time, DIM));
        }
        chips.add(new CinevaUI.Chip(String.format("Rs. %.2f per seat", ticketPrice), new Color(255, 196, 62)));
        header.add(chips);
        return header;
    }

    private JPanel createCenterPanel() {
        JPanel centerContainer = new JPanel(new BorderLayout(0, 8));
        centerContainer.setOpaque(false);
        centerContainer.setBorder(new EmptyBorder(8, 40, 6, 40));

        // Cinema Screen Graphic
        centerContainer.add(new ScreenCurve(), BorderLayout.NORTH);

        // 6 x 10 Seat Grid (with a row letter at the start of each row)
        CinevaUI.RoundPanel card = new CinevaUI.RoundPanel(new BorderLayout(), new Color(10, 10, 13), new Color(34, 35, 42), 26);
        card.setBorder(new EmptyBorder(22, 30, 22, 30));

        JPanel seatGridPanel = new JPanel(new GridLayout(ROWS, COLS + 1, 8, 8));
        seatGridPanel.setOpaque(false);

        for (int r = 0; r < ROWS; r++) {
            char rowChar = (char) ('A' + r);
            JLabel rowLabel = new JLabel("" + rowChar, SwingConstants.CENTER);
            rowLabel.setFont(CustomerHomeFrame.Fonts.get(true, 12f));
            rowLabel.setForeground(DIM);
            seatGridPanel.add(rowLabel);
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
        card.add(seatGridPanel, BorderLayout.CENTER);
        centerContainer.add(card, BorderLayout.CENTER);

        // Legend at bottom of center panel
        centerContainer.add(createLegendPanel(), BorderLayout.SOUTH);

        return centerContainer;
    }

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 28, 8));
        legendPanel.setOpaque(false);

        legendPanel.add(createLegendItem("Available", 0));
        legendPanel.add(createLegendItem("Selected", 1));
        legendPanel.add(createLegendItem("Booked", 2));

        return legendPanel;
    }

    private JPanel createLegendItem(String label, int kind) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        item.setOpaque(false);

        JComponent swatch = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                CustomerHomeFrame.aa(g);
                paintSeatShape(g, 0, 0, getWidth(), getHeight(), kind, false);
                g.dispose();
            }
        };
        swatchSize(swatch);

        JLabel lbl = new JLabel(label);
        lbl.setFont(CustomerHomeFrame.Fonts.get(false, 12.5f));
        lbl.setForeground(new Color(168, 172, 182));

        item.add(swatch);
        item.add(lbl);
        return item;
    }

    private static void swatchSize(JComponent c) {
        c.setPreferredSize(new Dimension(20, 16));
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 40, 26, 40));

        // Left info: Selected seats + Total
        JPanel summaryPanel = new JPanel();
        summaryPanel.setOpaque(false);
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));

        CinevaUI.TrackedText cap = CinevaUI.TrackedText.caps("Your seats", DIM);
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblSelectedSeats = new JLabel("None");
        lblSelectedSeats.setFont(CustomerHomeFrame.Fonts.get(true, 16f));
        lblSelectedSeats.setForeground(Color.WHITE);
        lblSelectedSeats.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblTotalAmount = new JLabel("Rs. 0.00  ·  0 seats");
        lblTotalAmount.setFont(CustomerHomeFrame.Fonts.get(false, 13f));
        lblTotalAmount.setForeground(new Color(168, 172, 182));
        lblTotalAmount.setAlignmentX(Component.LEFT_ALIGNMENT);

        summaryPanel.add(cap);
        summaryPanel.add(Box.createVerticalStrut(2));
        summaryPanel.add(lblSelectedSeats);
        summaryPanel.add(Box.createVerticalStrut(2));
        summaryPanel.add(lblTotalAmount);

        // Right buttons: Refresh + Cancel + Proceed
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        JButton btnRefresh = CinevaUI.pill("Refresh seats", CinevaUI.OUTLINE);
        btnRefresh.addActionListener(e -> refreshSeatMatrix());

        JButton btnClose = CinevaUI.pill("Close", CinevaUI.OUTLINE);
        btnClose.addActionListener(e -> dispose());

        btnBook = CinevaUI.pill("Proceed to payment →", CinevaUI.SOLID);
        btnBook.setEnabled(false);
        btnBook.addActionListener(e -> handleBookingProcess());

        btnPanel.add(btnRefresh);
        btnPanel.add(btnClose);
        btnPanel.add(btnBook);

        JPanel btnWrap = new JPanel(new GridBagLayout());     // vertically centre the buttons
        btnWrap.setOpaque(false);
        btnWrap.add(btnPanel);

        footer.add(summaryPanel, BorderLayout.WEST);
        footer.add(btnWrap, BorderLayout.EAST);
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
            lblSelectedSeats.setText("None");
            lblTotalAmount.setText("Rs. 0.00  ·  0 seats");
            btnBook.setEnabled(false);
        } else {
            List<String> names = new ArrayList<>();
            for (Seat s : selectedSeats) {
                names.add(s.getSeatNumber());
            }
            Collections.sort(names);
            lblSelectedSeats.setText(String.join(", ", names));
            lblTotalAmount.setText(String.format("Rs. %.2f  ·  %d seat%s", total, count, count > 1 ? "s" : ""));
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
                loadSeatsFromDatabase(); // Refresh seats: booked seats now turn dim!
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
            FlatDarkLaf.setup();

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

    // =====================================================================
    //  Drawing
    // =====================================================================

    /** kind: 0 = available (outline), 1 = selected (white), 2 = booked (dim). */
    private static void paintSeatShape(Graphics2D g, int x, int y, int w, int h, int kind, boolean hover) {
        float arc = Math.min(w, h) * 0.38f;
        RoundRectangle2D r = new RoundRectangle2D.Float(x + 1.5f, y + 1.5f, w - 3, h - 3, arc, arc);
        if (kind == 1) {
            g.setColor(Color.WHITE);
            g.fill(r);
        } else if (kind == 2) {
            g.setColor(new Color(20, 20, 25));
            g.fill(r);
            g.setColor(new Color(34, 35, 42));
            g.setStroke(new BasicStroke(1f));
            g.draw(r);
        } else {
            g.setColor(hover ? new Color(30, 30, 38) : new Color(15, 15, 20));
            g.fill(r);
            g.setColor(hover ? new Color(190, 194, 204) : new Color(88, 92, 102));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(r);
        }
    }

    /** Glowing curved screen above the seats. */
    private static class ScreenCurve extends JComponent {
        ScreenCurve() {
            setPreferredSize(new Dimension(100, 62));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            int w = getWidth();
            float x0 = w * 0.14f;
            float x1 = w * 0.86f;
            QuadCurve2D curve = new QuadCurve2D.Float(x0, 40, w / 2f, 4, x1, 40);

            // soft light falling towards the seats
            Path2D cone = new Path2D.Float();
            cone.moveTo(x0, 40);
            cone.quadTo(w / 2f, 4, x1, 40);
            cone.lineTo(x1 + 30, 62);
            cone.lineTo(x0 - 30, 62);
            cone.closePath();
            g.setPaint(new GradientPaint(0, 22, new Color(255, 255, 255, 34), 0, 62, new Color(255, 255, 255, 0)));
            g.fill(cone);

            for (int i = 3; i >= 1; i--) {
                g.setColor(new Color(255, 255, 255, 14 * (4 - i)));
                g.setStroke(new BasicStroke(2f + i * 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(curve);
            }
            g.setColor(new Color(235, 238, 245));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(curve);

            g.setFont(CustomerHomeFrame.Fonts.get(true, 10.5f));
            FontMetrics fm = g.getFontMetrics();
            String s = "SCREEN";
            float tw = CustomerHomeFrame.trackedWidth(fm, s, 3f);
            g.setColor(DIM);
            CustomerHomeFrame.drawTracked(g, s, (w - tw) / 2f, 40 + fm.getAscent() + 4, 3f);
            g.dispose();
        }
    }

    /**
     * Custom JToggleButton with explicit anti-aliased painting: outlined = available,
     * white = selected, dimmed = booked. Works the same on every look and feel.
     */
    private static class SeatButton extends JToggleButton {
        private Seat.SeatStatus seatStatus = Seat.SeatStatus.AVAILABLE;
        private final String label;

        public SeatButton(String text) {
            super(text);
            this.label = text.length() > 1 ? text.substring(1) : text;   // "A5" -> "5" (the row letter is on the left)
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(CustomerHomeFrame.Fonts.get(true, 12.5f));
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
            CustomerHomeFrame.aa(g2);

            int kind;
            Color textColor;
            if (seatStatus == Seat.SeatStatus.BOOKED) {
                kind = 2;
                textColor = new Color(70, 72, 80);
            } else if (isSelected() || seatStatus == Seat.SeatStatus.SELECTED) {
                kind = 1;
                textColor = new Color(12, 12, 14);
            } else {
                kind = 0;
                textColor = getModel().isRollover() ? Color.WHITE : new Color(190, 194, 202);
            }
            paintSeatShape(g2, 0, 0, getWidth(), getHeight(), kind, getModel().isRollover());

            g2.setColor(textColor);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(label)) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(label, x, y);

            g2.dispose();
        }
    }
}