package services;

/**
 * Strategy interface for payment processing.
 * Demonstrates OOP Concepts: Abstraction & Polymorphism (Strategy Pattern).
 * Allows dynamic swapping between CardPayment and UPIPayment at runtime.
 */
public interface PaymentStrategy {

    /**
     * Executes payment processing.
     * @param amount The transaction amount to pay.
     * @return true if payment validation and processing succeed, false otherwise.
     */
    boolean pay(double amount);

    /**
     * Returns the name of the payment method (e.g. CARD or UPI).
     */
    String getMethodName();

    /**
     * Provides user-friendly input validation guidance.
     */
    String getValidationHint();
}
