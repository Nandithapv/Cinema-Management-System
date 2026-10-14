package services;

/**
 * Concrete Strategy for UPI payments (Google Pay, PhonePe, Paytm, etc.).
 * Demonstrates OOP Concepts: Polymorphism, Encapsulation & Input Validation.
 */
public class UPIPayment implements PaymentStrategy {

    private final String upiId;

    public UPIPayment(String upiId) {
        this.upiId = upiId == null ? "" : upiId.trim();
    }

    @Override
    public boolean pay(double amount) {
        if (amount <= 0) {
            return false;
        }
        // Validate standard VPA format: username@bank
        return upiId.matches("^[a-zA-Z0-9.\\-_]{2,49}@[a-zA-Z]{2,}$");
    }

    @Override
    public String getMethodName() {
        return "UPI";
    }

    @Override
    public String getValidationHint() {
        return "Please enter a valid UPI ID (e.g., username@okhdfcbank or 9876543210@paytm).";
    }
}
