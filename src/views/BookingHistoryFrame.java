package views;

import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import dao.PaymentDAO;
import models.Payment;

/**
 * Booking History and Cancellation Frame (Member 5: Renjitha Raju).
 * Styled with Theme by Member 6 (Nanditha P V) for cohesive dark aesthetic and high contrast.
 */
public class BookingHistoryFrame extends JFrame {

    private static final int COL_ID = 0;
    private static final int COL_MOVIE = 1;
    private static final int COL_STATUS = 5;
    private static final int COL_TOTAL = 6;

    private final int userId;
    private final PaymentDAO dao = new PaymentDAO();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] { "Booking ID", "Movie", "Date", "Time", "Seats", "Status", "Total" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public BookingHistoryFrame(int userId) {
        super("My Bookings & Tickets — CineMagic");
        this.userId = userId;

        Theme.applyDarkTheme(this);
        getContentPane().setLayout(new BorderLayout());

        // 1. Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.BG_HEADER);
        headerPanel.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel("🎟️  My Booking History");
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.TEXT_LIGHT); // High-contrast visible white
        headerPanel.add(lblTitle, BorderLayout.WEST);

        JLabel lblSub = new JLabel("View digital receipts or manage bookings");
        lblSub.setFont(Theme.FONT_BODY);
        lblSub.setForeground(Theme.TEXT_MUTED);
        headerPanel.add(lblSub, BorderLayout.EAST);

        getContentPane().add(headerPanel, BorderLayout.NORTH);

        // 2. Table Setup with Theme Styling
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(34);
        table.setFont(Theme.FONT_BODY);
        table.setBackground(Theme.BG_CARD);
        table.setForeground(Theme.TEXT_LIGHT);
        table.setSelectionBackground(Theme.PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(Theme.BORDER_COLOR);

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(Theme.BG_HEADER);
        table.getTableHeader().setForeground(Theme.ACCENT_CYAN);
        table.getTableHeader().setFont(Theme.FONT_BODY_BOLD);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Action Bar with Styled Buttons
        JButton btnRefresh = new JButton("Refresh");
        Theme.styleSecondaryButton(btnRefresh);

        JButton btnReceipt = new JButton("View E-Ticket");
        Theme.stylePrimaryButton(btnReceipt);

        JButton btnCancel = new JButton("Cancel Booking");
        Theme.styleDangerButton(btnCancel);

        btnRefresh.addActionListener(e -> loadData());
        btnReceipt.addActionListener(e -> viewReceipt());
        btnCancel.addActionListener(e -> cancelSelected());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 14));
        buttons.setBackground(Theme.BG_HEADER);
        buttons.add(btnRefresh);
        buttons.add(btnReceipt);
        buttons.add(btnCancel);

        getContentPane().add(buttons, BorderLayout.SOUTH);

        setSize(920, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        loadData();
    }

    private void loadData() {
        model.setRowCount(0);
        try {
            for (Object[] row : dao.getHistory(userId)) {
                model.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load bookings: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int selectedRow() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a booking from the table first.");
        }
        return row;
    }

    private void viewReceipt() {
        int row = selectedRow();
        if (row < 0) {
            return;
        }

        String status = String.valueOf(model.getValueAt(row, COL_STATUS));
        if (!"CONFIRMED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this,
                    "Receipts can only be generated for confirmed bookings.",
                    "Notice",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int id = (Integer) model.getValueAt(row, COL_ID);
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
                : parseAmount(String.valueOf(model.getValueAt(row, COL_TOTAL)));
        String refCode = (payment != null && payment.getTransactionRef() != null)
                ? payment.getTransactionRef()
                : ("BK-" + String.format("%05d", id));

        new TicketReceiptDialog(this, id, info[0], info[1], info[2], amount, method, refCode).setVisible(true);
    }

    private void cancelSelected() {
        int row = selectedRow();
        if (row < 0) {
            return;
        }

        int id = (Integer) model.getValueAt(row, COL_ID);
        String status = String.valueOf(model.getValueAt(row, COL_STATUS));
        if ("CANCELLED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "This booking has already been cancelled.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel booking #" + id + " (" + model.getValueAt(row, COL_MOVIE) + ")?\n\n"
                + "Refund Policy:\n"
                + " • 100% refund if 24 hours or more before showtime\n"
                + " • 50% refund if between 1 and 24 hours before showtime\n"
                + " • No cancellation permitted within 1 hour of the show",
                "Confirm Ticket Cancellation",
                JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            double refund = dao.cancelBooking(id);
            JOptionPane.showMessageDialog(this,
                    String.format("Booking #%d successfully cancelled.\nRefund Amount: Rs. %.2f", id, refund),
                    "Cancellation Confirmed",
                    JOptionPane.INFORMATION_MESSAGE);
            loadData();
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot Cancel",
                    JOptionPane.WARNING_MESSAGE);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BookingHistoryFrame(1).setVisible(true));
    }
}