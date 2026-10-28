package models;

import java.time.LocalDateTime;

/**
 * Model representing a Payment transaction.
 * Demonstrates OOP Concepts: Encapsulation (private fields, validated getters/setters).
 * Matches Member 1's MySQL table `payments`.
 */
public class Payment {

    private int paymentId;
    private int bookingId;
    private double amount;
    private String method;           // CARD, UPI, CASH
    private String transactionRef;   // Unique payment gateway reference
    private LocalDateTime paymentDate;
    private String status;           // UI-only status (e.g. SUCCESS / FAILED)

    /**
     * Standard constructor for creating a new payment.
     */
    public Payment(int bookingId, double amount, String method) {
        this(bookingId, amount, method, "TXN-" + System.currentTimeMillis() + "-" + bookingId);
    }

    /**
     * Constructor with custom transaction reference.
     */
    public Payment(int bookingId, double amount, String method, String transactionRef) {
        this.bookingId = bookingId;
        setAmount(amount);
        this.method = method;
        this.transactionRef = transactionRef;
        this.status = "SUCCESS";
        this.paymentDate = LocalDateTime.now();
    }

    /**
     * Full constructor (including status for UI/history compatibility).
     */
    public Payment(int bookingId, double amount, String method, String status, String transactionRef) {
        this.bookingId = bookingId;
        setAmount(amount);
        this.method = method;
        this.status = status;
        this.transactionRef = transactionRef;
        this.paymentDate = LocalDateTime.now();
    }

    // Getters and Setters with Encapsulation & Validation

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative");
        }
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Payment [bookingId=" + bookingId + ", method=" + method + ", amount=Rs." + amount
                + ", ref=" + transactionRef + ", status=" + status + "]";
    }
}