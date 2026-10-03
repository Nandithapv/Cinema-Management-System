package models;

public class UPIPayment implements PaymentStrategy {

    private final String upiId;

    public UPIPayment(String upiId) {
        this.upiId = upiId == null ? "" : upiId.trim();
    }

    @Override
    public boolean pay(double amount) {
        if (amount <= 0) return false;
        return upiId.matches("[\\w.\\-]{2,}@[a-zA-Z]{2,}");
    }

    @Override
    public String getMethodName() { return "UPI"; }

    @Override
    public String getValidationHint() {
        return "Enter a valid UPI ID, for example name@upi.";
    }
}