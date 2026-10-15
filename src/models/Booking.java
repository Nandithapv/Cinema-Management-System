package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a ticket booking header in the system.
 * 
 * Demonstrates OOP Concepts for Viva:
 * - Encapsulation: Field validation for positive amounts and strict status enums.
 * - Aggregation ("Has-A" relationship): A Booking has multiple BookingDetail items.
 */
public class Booking {
    private int bookingId;
    private int userId;
    private int showId;
    private LocalDateTime bookingDate;
    private double totalAmount;
    private String status; // Strictly 'CONFIRMED' or 'CANCELLED'

    // Aggregated list of booked details
    private List<BookingDetail> bookingDetails = new ArrayList<>();

    public Booking() {
        this.bookingDate = LocalDateTime.now();
        this.status = "CONFIRMED";
    }

    public Booking(int userId, int showId, double totalAmount) {
        this(0, userId, showId, LocalDateTime.now(), totalAmount, "CONFIRMED");
    }

    public Booking(int bookingId, int userId, int showId, LocalDateTime bookingDate, double totalAmount, String status) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.showId = showId;
        this.bookingDate = (bookingDate != null) ? bookingDate : LocalDateTime.now();
        setTotalAmount(totalAmount);
        setStatus(status);
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        if (bookingId < 0) {
            throw new IllegalArgumentException("Booking ID cannot be negative.");
        }
        this.bookingId = bookingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getShowId() {
        return showId;
    }

    public void setShowId(int showId) {
        this.showId = showId;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        if (totalAmount < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative.");
        }
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status == null || (!status.equalsIgnoreCase("CONFIRMED") && !status.equalsIgnoreCase("CANCELLED"))) {
            throw new IllegalArgumentException("Booking status must be strictly 'CONFIRMED' or 'CANCELLED'.");
        }
        this.status = status.toUpperCase();
    }

    public List<BookingDetail> getBookingDetails() {
        return bookingDetails;
    }

    public void setBookingDetails(List<BookingDetail> bookingDetails) {
        this.bookingDetails = (bookingDetails != null) ? bookingDetails : new ArrayList<>();
    }

    public void addBookingDetail(BookingDetail detail) {
        if (detail != null) {
            this.bookingDetails.add(detail);
        }
    }

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId=" + bookingId +
                ", userId=" + userId +
                ", showId=" + showId +
                ", bookingDate=" + bookingDate +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", detailsCount=" + bookingDetails.size() +
                '}';
    }
}
