package models;

/**
 * Model class representing an individual seat reserved in a booking.
 * 
 * Demonstrates OOP Concepts for Viva:
 * - Encapsulation: Strict price check and status validations.
 * - Granular State Mapping: Connects a booking, show, and specific seat.
 */
public class BookingDetail {
    private int bookingDetailId;
    private int bookingId;
    private int showId;
    private int seatId;
    private double price;
    private String status; // 'CONFIRMED' or 'CANCELLED'

    // Display helpers (not stored in table, but useful for UI and summaries)
    private String seatNumber;

    public BookingDetail() {
        this.status = "CONFIRMED";
    }

    public BookingDetail(int bookingId, int showId, int seatId, double price) {
        this(0, bookingId, showId, seatId, price, "CONFIRMED");
    }

    public BookingDetail(int bookingDetailId, int bookingId, int showId, int seatId, double price, String status) {
        this.bookingDetailId = bookingDetailId;
        this.bookingId = bookingId;
        this.showId = showId;
        this.seatId = seatId;
        setPrice(price);
        setStatus(status);
    }

    public int getBookingDetailId() {
        return bookingDetailId;
    }

    public void setBookingDetailId(int bookingDetailId) {
        if (bookingDetailId < 0) {
            throw new IllegalArgumentException("Booking detail ID cannot be negative.");
        }
        this.bookingDetailId = bookingDetailId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getShowId() {
        return showId;
    }

    public void setShowId(int showId) {
        this.showId = showId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status == null || (!status.equalsIgnoreCase("CONFIRMED") && !status.equalsIgnoreCase("CANCELLED"))) {
            throw new IllegalArgumentException("Booking detail status must be strictly 'CONFIRMED' or 'CANCELLED'.");
        }
        this.status = status.toUpperCase();
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    @Override
    public String toString() {
        return "BookingDetail{" +
                "detailId=" + bookingDetailId +
                ", bookingId=" + bookingId +
                ", showId=" + showId +
                ", seatId=" + seatId +
                (seatNumber != null ? ", seat=" + seatNumber : "") +
                ", price=" + price +
                ", status='" + status + '\'' +
                '}';
    }
}
