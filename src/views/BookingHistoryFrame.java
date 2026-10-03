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

/** Usage: new BookingHistoryFrame(loggedInUserId).setVisible(true); */
public class BookingHistoryFrame extends JFrame {

    private static final int COL_ID = 0, COL_MOVIE = 1, COL_STATUS = 5, COL_TOTAL = 6;

    private final int userId;
    private final PaymentDAO dao = new PaymentDAO();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] { "Booking ID", "Movie", "Date", "Time", "Seats", "Status", "Total" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);

    public BookingHistoryFrame(int userId) {
        super("My Bookings");
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

        setSize(820, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        loadData();
    }

    private void loadData() {
        model.setRowCount(0);
        try {
            for (Object[] row : dao.getHistory(userId)) model.addRow(row);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load bookings: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int selectedRow() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a booking first.");
        }
        return row;
    }

    private void viewReceipt() {
        int row = selectedRow();
        if (row < 0) return;
        if (!"CONFIRMED".equals(model.getValueAt(row, COL_STATUS))) {
            JOptionPane.showMessageDialog(this, "Receipts are only available for confirmed bookings.");
            return;
        }
        int id = (Integer) model.getValueAt(row, COL_ID);
        String[] info = dao.getBookingSummary(id);
        if (info == null) {
            JOptionPane.showMessageDialog(this, "Booking details not found.");
            return;
        }
        double amount = parseAmount(String.valueOf(model.getValueAt(row, COL_TOTAL)));
        new TicketReceiptDialog(this, id, info[0], info[1], info[2], amount, "-").setVisible(true);
    }

    private void cancelSelected() {
        int row = selectedRow();
        if (row < 0) return;
        int id = (Integer) model.getValueAt(row, COL_ID);

        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel booking #" + id + " (" + model.getValueAt(row, COL_MOVIE) + ")?\n\n"
                + "Refund: 100% if 24h+ before the show, 50% if 1-24h,\n"
                + "no cancellation under 1 hour.",
                "Confirm cancellation", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        try {
            double refund = dao.cancelBooking(id);
            JOptionPane.showMessageDialog(this,
                    String.format("Booking cancelled. Refund: Rs. %.2f", refund));
            loadData();
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot cancel",
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
            return 0;
        }
    }

    // Quick standalone test: replace 1 with a real user_id from your users table
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BookingHistoryFrame(1).setVisible(true));
    }
}