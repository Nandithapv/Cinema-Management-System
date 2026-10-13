package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import config.DatabaseConnection;
import models.Payment;

/**
 * Data Access Object for Payment, Billing, and Ticket Cancellation (Member 5).
 * 
 * Key Responsibilities:
 *  - Save confirmed payments matching MySQL payments table.
 *  - Release incomplete/unpaid bookings (invoked by Shada if checkout cancelled).
 *  - Cancel confirmed bookings with tiered refund calculation.
 *  - Fetch booking summaries and user booking history.
 * 
 * OOP Viva Concepts:
 *  - JDBC Transactions & Atomicity (setAutoCommit(false), rollback, autoCommit restoration).
 *  - Singleton Connection usage (never closing the shared connection).
 *  - Encapsulated database access layer (DAO Pattern).
 */
public class PaymentDAO {

    /**
     * Saves a payment record in the database.
     * Matches Member 1's schema: payments(booking_id, method, amount, transaction_ref).
     *
     * @param p The Payment object containing details.
     * @return true if successfully saved, false otherwise.
     */
    public boolean savePayment(Payment p) {
        String sql = "INSERT INTO payments (booking_id, method, amount, transaction_ref) VALUES (?, ?, ?, ?)";
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, p.getBookingId());
                ps.setString(2, p.getMethod());
                ps.setDouble(3, p.getAmount());
                String txnRef = p.getTransactionRef();
                if (txnRef == null || txnRef.trim().isEmpty()) {
                    txnRef = "TXN-" + System.currentTimeMillis() + "-" + p.getBookingId();
                }
                ps.setString(4, txnRef);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] Payment not saved: " + e.getMessage());
            return false;
        }
    }

    /**
     * Releases a booking when a user cancels out of the PaymentDialog without paying.
     * Transactionally sets booking status to 'CANCELLED' and removes booking_details rows,
     * freeing up the seats without applying any refund.
     *
     * @param bookingId The booking to release.
     * @return true if successfully released.
     * @throws SQLException If a database error occurs.
     */
    public boolean releaseBooking(int bookingId) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        boolean oldAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);

            // 1. Mark the booking as CANCELLED
            try (PreparedStatement psUpdate = con.prepareStatement(
                    "UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ?")) {
                psUpdate.setInt(1, bookingId);
                psUpdate.executeUpdate();
            }

            // 2. Remove booking_details rows so seats become available immediately
            try (PreparedStatement psDelete = con.prepareStatement(
                    "DELETE FROM booking_details WHERE booking_id = ?")) {
                psDelete.setInt(1, bookingId);
                psDelete.executeUpdate();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    /**
     * Retrieves the payment details for a given booking.
     * Used by TicketReceiptDialog and BookingHistoryFrame to display exact payment info.
     *
     * @param bookingId The booking ID.
     * @return Payment object or null if not found.
     */
    public Payment getPaymentByBookingId(int bookingId) {
        String sql = "SELECT payment_id, booking_id, method, amount, transaction_ref, payment_date "
                + "FROM payments WHERE booking_id = ? ORDER BY payment_date DESC LIMIT 1";
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Payment p = new Payment(
                                rs.getInt("booking_id"),
                                rs.getDouble("amount"),
                                rs.getString("method"),
                                rs.getString("transaction_ref")
                        );
                        p.setPaymentId(rs.getInt("payment_id"));
                        Timestamp ts = rs.getTimestamp("payment_date");
                        if (ts != null) {
                            p.setPaymentDate(ts.toLocalDateTime());
                        }
                        return p;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] Error retrieving payment: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a summary of a booking: { movie title, "date time", "A1, A2" }.
     *
     * @param bookingId The booking ID.
     * @return String array with 3 elements, or null if not found.
     */
    public String[] getBookingSummary(int bookingId) {
        String sql = "SELECT m.title, s.show_date, s.show_time, "
                + "GROUP_CONCAT(st.seat_number ORDER BY st.seat_id SEPARATOR ', ') AS seats "
                + "FROM bookings b "
                + "JOIN shows s ON b.show_id = s.show_id "
                + "JOIN movies m ON s.movie_id = m.movie_id "
                + "LEFT JOIN booking_details bd ON b.booking_id = bd.booking_id "
                + "LEFT JOIN seats st ON bd.seat_id = st.seat_id "
                + "WHERE b.booking_id = ? "
                + "GROUP BY b.booking_id, m.title, s.show_date, s.show_time";
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String seats = rs.getString("seats");
                        return new String[] {
                            rs.getString("title"),
                            rs.getString("show_date") + " " + rs.getString("show_time"),
                            seats == null ? "-" : seats
                        };
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] Error getting booking summary: " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetches booking history rows for a customer:
     * Columns: booking_id, title, show_date, show_time, seats, status, total.
     *
     * @param userId Logged-in customer ID.
     * @return List of row object arrays for JTable.
     */
    public List<Object[]> getHistory(int userId) throws SQLException {
        String sql = "SELECT b.booking_id, m.title, s.show_date, s.show_time, b.status, "
                + "GROUP_CONCAT(st.seat_number ORDER BY st.seat_id SEPARATOR ', ') AS seats, "
                + "COALESCE(SUM(bd.price), 0) AS total "
                + "FROM bookings b "
                + "JOIN shows s ON b.show_id = s.show_id "
                + "JOIN movies m ON s.movie_id = m.movie_id "
                + "LEFT JOIN booking_details bd ON b.booking_id = bd.booking_id "
                + "LEFT JOIN seats st ON bd.seat_id = st.seat_id "
                + "WHERE b.user_id = ? "
                + "GROUP BY b.booking_id, m.title, s.show_date, s.show_time, b.status, b.booking_date "
                + "ORDER BY b.booking_date DESC";
        List<Object[]> rows = new ArrayList<>();
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String seats = rs.getString("seats");
                    double total = rs.getDouble("total");
                    rows.add(new Object[] {
                        rs.getInt("booking_id"),
                        rs.getString("title"),
                        rs.getString("show_date"),
                        rs.getString("show_time"),
                        seats == null ? "-" : seats,
                        rs.getString("status"),
                        total == 0 ? "-" : String.format("Rs. %.2f", total)
                    });
                }
            }
        }
        return rows;
    }

    /**
     * Cancels a booking in one transaction: marks it CANCELLED and deletes its
     * booking_details rows so the seats become free again.
     * Refund policy:
     *  - 100% refund if 24h or more before the show
     *  - 50% refund if 1-24h before the show
     *  - Cancellation not allowed less than 1 hour before show
     *
     * @param bookingId The booking ID to cancel.
     * @return The refund amount.
     * @throws IllegalStateException If cancellation policy is violated.
     * @throws SQLException If database update fails.
     */
    public double cancelBooking(int bookingId) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        boolean oldAutoCommit = con.getAutoCommit();
        String infoSql = "SELECT b.status, TIMESTAMP(s.show_date, s.show_time) AS show_dt, "
                + "COALESCE(SUM(bd.price), 0) AS total "
                + "FROM bookings b "
                + "JOIN shows s ON b.show_id = s.show_id "
                + "LEFT JOIN booking_details bd ON b.booking_id = bd.booking_id "
                + "WHERE b.booking_id = ? "
                + "GROUP BY b.booking_id, b.status, s.show_date, s.show_time";
        try {
            con.setAutoCommit(false);

            String status;
            LocalDateTime showTime;
            double total;
            try (PreparedStatement ps = con.prepareStatement(infoSql)) {
                ps.setInt(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("Booking not found.");
                    }
                    status = rs.getString("status");
                    Timestamp ts = rs.getTimestamp("show_dt");
                    showTime = ts.toLocalDateTime();
                    total = rs.getDouble("total");
                }
            }

            if ("CANCELLED".equals(status)) {
                throw new IllegalStateException("This booking is already cancelled.");
            }

            long minutesLeft = Duration.between(LocalDateTime.now(), showTime).toMinutes();
            if (minutesLeft < 60) {
                throw new IllegalStateException("Cancellation is not allowed less than 1 hour before the show.");
            }

            double refund = minutesLeft >= 24 * 60 ? total : total * 0.5;

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ?")) {
                ps.setInt(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM booking_details WHERE booking_id = ?")) {
                ps.setInt(1, bookingId);
                ps.executeUpdate();
            }

            con.commit();
            return refund;

        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }
}