package models;

/** Abstraction + polymorphism: every payment method implements this. */
public interface PaymentStrategy {
    boolean pay(double amount);
    String getMethodName();
    String getValidationHint();
}