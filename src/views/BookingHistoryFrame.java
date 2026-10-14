package views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import dao.PaymentDAO;
import models.Payment;

/**
 * Booking History and Cancellation Frame (Member 5).
 * 
 * Usage:
 *  new BookingHistoryFrame(loggedInUserId).setVisible(true);
 * 
 * Key Features:
 *  - Displays user's past and active bookings in a JTable.
 *  - Opens digital receipt reading exact method/amount from the payments table.
 *  - Allows ticket cancellation with automated refund calculation.
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

    /**
     * Primary constructor required by the team.
     * @param userId The ID of the logged-in user.
     */
    public BookingHistoryFrame(int userId) {
        super("My Bookings & Tickets");
        this.userId = userId;

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);

        JButton btnRefresh = new JButton("Refresh");
        JButton btnReceipt = new JButton("View Receipt");
        JButton btnCancel = new JButton("Cancel Booking");

        btnRefresh.addActionListener(e -> loadData());
        btnReceipt.addActionListener(e -> viewReceipt());
        btnCancel.addActionListener(e -> cancelSelected());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnRefresh);
        buttons.add(btnReceipt);
        buttons.add(btnCancel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        setSize(850, 440);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        loadData();
    }

    /**
     * Loads the booking history rows for the user.
     */
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

    /**
     * Displays the digital receipt for a confirmed booking.
     * Reads the payment method and amount directly from the payments table.
     */
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

        // Query payments table to read the real method, amount, and transaction_ref
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

    /**
     * Cancels the selected booking with transaction protection and tiered refund.
     */
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

    // Standalone test
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BookingHistoryFrame(1).setVisible(true));
    }
}