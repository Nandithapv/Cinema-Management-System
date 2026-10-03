package models;

import java.time.LocalDateTime;

public class Payment {

    private int paymentId;
    private int bookingId;
    private double amount;
    private String method;   // CARD or UPI
    private String status;   // SUCCESS or FAILED
    private LocalDateTime paymentDate;

    public Payment(int bookingId, double amount, String method, String status) {
        this.bookingId = bookingId;
        setAmount(amount);
        this.method = method;
        this.status = status;
        this.paymentDate = LocalDateTime.now();
    }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
        this.amount = amount;
    }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPaymentDate() { return paymentDate; }

    @Override
    public String toString() {
        return "Payment[booking=" + bookingId + ", " + method + ", Rs." + amount + ", " + status + "]";
    }
}