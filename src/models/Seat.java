package models;

/**
 * Model class representing a cinema seat.
 * 
 * Demonstrates OOP Concepts for Viva:
 * - Encapsulation: Private fields with validated getters and setters.
 * - State Management: Dynamic status transitions (AVAILABLE -> SELECTED -> BOOKED).
 */
public class Seat {
    private int seatId;
    private int screenId;
    private String seatNumber; // e.g., "A1", "B5", "F10"
    private SeatStatus status;

    public enum SeatStatus {
        AVAILABLE,
        SELECTED,
        BOOKED
    }

    public Seat() {
        this.status = SeatStatus.AVAILABLE;
    }

    public Seat(int seatId, int screenId, String seatNumber) {
        this(seatId, screenId, seatNumber, SeatStatus.AVAILABLE);
    }

    public Seat(int seatId, int screenId, String seatNumber, SeatStatus status) {
        this.seatId = seatId;
        this.screenId = screenId;
        setSeatNumber(seatNumber);
        this.status = (status != null) ? status : SeatStatus.AVAILABLE;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        if (seatId < 0) {
            throw new IllegalArgumentException("Seat ID cannot be negative.");
        }
        this.seatId = seatId;
    }

    public int getScreenId() {
        return screenId;
    }

    public void setScreenId(int screenId) {
        if (screenId < 0) {
            throw new IllegalArgumentException("Screen ID cannot be negative.");
        }
        this.screenId = screenId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        if (seatNumber == null || seatNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Seat number cannot be empty.");
        }
        this.seatNumber = seatNumber.trim().toUpperCase();
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Seat status cannot be null.");
        }
        this.status = status;
    }

    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    public boolean isBooked() {
        return this.status == SeatStatus.BOOKED;
    }

    public boolean isSelected() {
        return this.status == SeatStatus.SELECTED;
    }

    /**
     * Extracts row letter (e.g. "A" from "A5").
     */
    public String getRowLetter() {
        if (seatNumber != null && !seatNumber.isEmpty()) {
            return seatNumber.substring(0, 1);
        }
        return "";
    }

    /**
     * Extracts column number (e.g. 5 from "A5").
     */
    public int getSeatColNumber() {
        if (seatNumber != null && seatNumber.length() > 1) {
            try {
                return Integer.parseInt(seatNumber.substring(1));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    @Override
    public String toString() {
        return seatNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return seatId == seat.seatId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(seatId);
    }
}
