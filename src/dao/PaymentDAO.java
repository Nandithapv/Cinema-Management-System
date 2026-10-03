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
 * Payment, booking history and cancellation queries (Member 5).
 * Kept separate from Shada's BookingDAO to avoid merge conflicts.
 */
public class PaymentDAO {

    /** Saves a payment. Returns false if it could not be saved. */
    public boolean savePayment(Payment p) {
        String sql = "INSERT INTO payments (booking_id, method, amount) VALUES (?, ?, ?)";
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, p.getBookingId());
                ps.setString(2, p.getMethod());
                ps.setDouble(3, p.getAmount());
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.out.println("Payment not saved: " + e.getMessage());
            return false;
        }
    }

    /** Returns {movie title, "date time", "A1, A2"} or null if not found. */
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
            e.printStackTrace();
        }
        return null;
    }

    /** Rows: id, movie, date, time, seats, status, total. */
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
     * Refund policy: 100% if 24h or more before the show, 50% if 1-24h, not allowed under 1h.
     *
     * @return the refund amount
     * @throws IllegalStateException if the booking cannot be cancelled
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
                    if (!rs.next()) throw new IllegalStateException("Booking not found.");
                    status = rs.getString("status");
                    Timestamp ts = rs.getTimestamp("show_dt");
                    showTime = ts.toLocalDateTime();
                    total = rs.getDouble("total");
                }
            }

            if ("CANCELLED".equals(status))
                throw new IllegalStateException("This booking is already cancelled.");

            long minutesLeft = Duration.between(LocalDateTime.now(), showTime).toMinutes();
            if (minutesLeft < 60)
                throw new IllegalStateException("Cancellation is not allowed less than 1 hour before the show.");

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