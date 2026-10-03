package models;

public class CardPayment implements PaymentStrategy {

    private final String cardNumber;
    private final String expiry;
    private final String cvv;

    public CardPayment(String cardNumber, String expiry, String cvv) {
        this.cardNumber = cardNumber == null ? "" : cardNumber.replaceAll("\\s+", "");
        this.expiry = expiry == null ? "" : expiry.trim();
        this.cvv = cvv == null ? "" : cvv.trim();
    }

    @Override
    public boolean pay(double amount) {
        if (amount <= 0) return false;
        return cardNumber.matches("\\d{16}")
                && expiry.matches("(0[1-9]|1[0-2])/\\d{2}")
                && cvv.matches("\\d{3}");
    }

    @Override
    public String getMethodName() { return "CARD"; }

    @Override
    public String getValidationHint() {
        return "Enter a 16-digit card number, expiry as MM/YY, and a 3-digit CVV.";
    }
}