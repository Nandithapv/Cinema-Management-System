package dao;

import config.DatabaseConnection;
import models.Booking;
import models.BookingDetail;
import models.Seat;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data Access Object for Seats and Bookings (Member 4: Shada).
 * 
 * Rules Adherence:
 *  1. Shared Singleton DatabaseConnection: NEVER closed in this DAO.
 *  2. PreparedStatement & ResultSet strictly closed in try-with-resources.
 *  3. Transaction management: autoCommit set to false for atomic multi-seat reservation,
 *     and restored in a finally block.
 *  4. Strict status strings: 'CONFIRMED' and 'CANCELLED'.
 *  5. Catch duplicate-key / concurrency constraint violations and provide clear feedback.
 * 
 * Note: Member 5 (Renjitha) manages getHistory() and cancelBooking() in PaymentDAO.
 */
public class BookingDAO {

    public BookingDAO() {
        // Default constructor
    }

    /**
     * Retrieves all seats belonging to a specific screen.
     * Ordered by row (A-F) and column (1-10).
     */
    public List<Seat> getSeatsByScreen(int screenId) {
        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT seat_id, screen_id, seat_number FROM seats " +
                     "WHERE screen_id = ? " +
                     "ORDER BY SUBSTRING(seat_number, 1, 1), CAST(SUBSTRING(seat_number, 2) AS UNSIGNED)";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.err.println("[BookingDAO] Database connection is null.");
            return seats;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, screenId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Seat seat = new Seat(
                            rs.getInt("seat_id"),
                            rs.getInt("screen_id"),
                            rs.getString("seat_number")
                    );
                    seats.add(seat);
                }
            }
        } catch (SQLException e) {
            System.err.println("[BookingDAO] Error loading seats: " + e.getMessage());
            e.printStackTrace();
        }
        return seats;
    }

    /**
     * Retrieves list of seat IDs already booked for a specific show with CONFIRMED status.
     * Team rule: getBookedSeats(showId) for CONFIRMED bookings.
     */
    public List<Integer> getBookedSeats(int showId) {
        List<Integer> bookedSeatIds = new ArrayList<>();
        // Joins bookings table to verify show_id and status='CONFIRMED' safely across schema versions
        String sql = "SELECT bd.seat_id FROM booking_details bd " +
                     "JOIN bookings b ON bd.booking_id = b.booking_id " +
                     "WHERE b.show_id = ? AND b.status = 'CONFIRMED'";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.err.println("[BookingDAO] Database connection is null.");
            return bookedSeatIds;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, showId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bookedSeatIds.add(rs.getInt("seat_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BookingDAO] Error loading booked seats: " + e.getMessage());
            e.printStackTrace();
        }
        return bookedSeatIds;
    }

    /**
     * Creates a new booking and reserves all selected seats in ONE atomic JDBC transaction.
     * 
     * Concurrency & ACID Integrity:
     *  - Checks if any chosen seat is already booked.
     *  - Inserts booking header record (CONFIRMED).
     *  - Inserts individual booking_details records.
     *  - Catches duplicate-key errors (e.g. MySQL 1062 on uq_active_seat) if two users pick simultaneously.
     *  - Commits on success, rolls back on error.
     *  - Restores autoCommit in finally block.
     * 
     * @param userId ID of the customer booking tickets
     * @param showId ID of the show
     * @param selectedSeats List of Seat objects to book
     * @param ticketPrice Price per seat
     * @return generated bookingId
     * @throws SQLException If database error occurs or seats are already taken
     */
    public int createBooking(int userId, int showId, List<Seat> selectedSeats, double ticketPrice) throws SQLException {
        if (selectedSeats == null || selectedSeats.isEmpty()) {
            throw new IllegalArgumentException("No seats selected for booking.");
        }

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            throw new SQLException("Database connection is not available.");
        }

        boolean originalAutoCommit = conn.getAutoCommit();
        double totalAmount = selectedSeats.size() * ticketPrice;
        int generatedBookingId = -1;

        try {
            conn.setAutoCommit(false);

            // Step 1: Pre-check if any selected seat is already booked for this show
            Set<Integer> currentBooked = new HashSet<>(getBookedSeats(showId));
            for (Seat seat : selectedSeats) {
                if (currentBooked.contains(seat.getSeatId())) {
                    throw new SQLException("Seat " + seat.getSeatNumber() + " was just taken by another customer. Please choose different seats.");
                }
            }

            // Step 2: Insert header into bookings table (dynamically adapts to schema)
            Set<String> bookingCols = new HashSet<>();
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "bookings", null)) {
                while (rs.next()) {
                    bookingCols.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            } catch (SQLException ignored) {}

            boolean hasTotalAmount = bookingCols.contains("total_amount");
            boolean hasBookingDate = bookingCols.contains("booking_date");

            StringBuilder bSql = new StringBuilder("INSERT INTO bookings (user_id, show_id, status");
            if (hasBookingDate) bSql.append(", booking_date");
            if (hasTotalAmount) bSql.append(", total_amount");
            bSql.append(") VALUES (?, ?, 'CONFIRMED'");
            if (hasBookingDate) bSql.append(", NOW()");
            if (hasTotalAmount) bSql.append(", ?");
            bSql.append(")");

            try (PreparedStatement bookingStmt = conn.prepareStatement(bSql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int bIdx = 1;
                bookingStmt.setInt(bIdx++, userId);
                bookingStmt.setInt(bIdx++, showId);
                if (hasTotalAmount) {
                    bookingStmt.setDouble(bIdx++, totalAmount);
                }

                int rowsAffected = bookingStmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Failed to create booking header.");
                }

                try (ResultSet generatedKeys = bookingStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedBookingId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to obtain generated booking ID.");
                    }
                }
            }

            // Step 3: Insert each seat into booking_details table (dynamically adapts to schema)
            Set<String> bdCols = new HashSet<>();
            try (ResultSet colRs = conn.getMetaData().getColumns(null, null, "booking_details", null)) {
                while (colRs.next()) {
                    bdCols.add(colRs.getString("COLUMN_NAME").toLowerCase());
                }
            } catch (SQLException ignored) {}

            boolean hasShowId = bdCols.contains("show_id");
            boolean hasStatus = bdCols.contains("status");

            StringBuilder sqlBuilder = new StringBuilder("INSERT INTO booking_details (booking_id, seat_id, price");
            if (hasShowId) sqlBuilder.append(", show_id");
            if (hasStatus) sqlBuilder.append(", status");
            sqlBuilder.append(") VALUES (?, ?, ?");
            if (hasShowId) sqlBuilder.append(", ?");
            if (hasStatus) sqlBuilder.append(", ?");
            sqlBuilder.append(")");

            try (PreparedStatement detailStmt = conn.prepareStatement(sqlBuilder.toString())) {
                for (Seat seat : selectedSeats) {
                    int idx = 1;
                    detailStmt.setInt(idx++, generatedBookingId);
                    detailStmt.setInt(idx++, seat.getSeatId());
                    detailStmt.setDouble(idx++, ticketPrice);
                    if (hasShowId) detailStmt.setInt(idx++, showId);
                    if (hasStatus) detailStmt.setString(idx++, "CONFIRMED");
                    detailStmt.addBatch();
                }
                detailStmt.executeBatch();
            }

            // Step 4: Commit transaction
            conn.commit();
            return generatedBookingId;

        } catch (SQLIntegrityConstraintViolationException e) {
            conn.rollback();
            // Catches uq_active_seat conflict when two users book the same seat simultaneously
            throw new SQLException("Seat collision: One or more selected seats were just taken. Please refresh and select again.", e);
        } catch (SQLException e) {
            conn.rollback();
            if (e.getErrorCode() == 1062) { // MySQL Duplicate Key error code
                throw new SQLException("Seat collision: One or more selected seats were just taken. Please refresh and select again.", e);
            }
            throw e;
        } catch (Exception e) {
            conn.rollback();
            throw new SQLException("Booking failed: " + e.getMessage(), e);
        } finally {
            // Restore autoCommit safely
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Fetches a booking header by booking ID.
     */
    public Booking getBookingById(int bookingId) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return null;

        Set<String> bookingCols = new HashSet<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "bookings", null)) {
            while (rs.next()) {
                bookingCols.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        } catch (SQLException ignored) {}

        boolean hasTotalAmount = bookingCols.contains("total_amount");
        String sql = hasTotalAmount
                ? "SELECT booking_id, user_id, show_id, booking_date, total_amount, status FROM bookings WHERE booking_id = ?"
                : "SELECT booking_id, user_id, show_id, booking_date, status FROM bookings WHERE booking_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("booking_date");
                    double amt = hasTotalAmount ? rs.getDouble("total_amount") : 0.0;
                    return new Booking(
                            rs.getInt("booking_id"),
                            rs.getInt("user_id"),
                            rs.getInt("show_id"),
                            ts != null ? ts.toLocalDateTime() : null,
                            amt,
                            rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[BookingDAO] Error fetching booking #" + bookingId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
