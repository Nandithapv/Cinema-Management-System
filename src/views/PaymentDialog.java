package views;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import dao.PaymentDAO;
import models.Payment;
import services.CardPayment;
import services.PaymentStrategy;
import services.UPIPayment;

/**
 * Payment Dialog for Checkout (Member 5).
 * 
 * Usage:
 *  PaymentDialog dialog = new PaymentDialog(owner, bookingId, totalAmount);
 *  dialog.setVisible(true);
 *  if (dialog.isPaid()) { ... }
 * 
 * OOP Viva Concepts:
 *  - Polymorphism & Strategy Pattern: Seamlessly executes either CardPayment
 *    or UPIPayment through the PaymentStrategy interface.
 *  - Encapsulation: Input validation and UI state isolation.
 */
public class PaymentDialog extends JDialog {

    private final int bookingId;
    private final double amount;
    private boolean paid = false;

    private final JRadioButton rbCard = new JRadioButton("Credit / Debit Card", true);
    private final JRadioButton rbUpi = new JRadioButton("UPI (GPay / PhonePe / Paytm)");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel formPanel = new JPanel(cardLayout);

    private final JTextField txtCardNo = new JTextField(16);
    private final JTextField txtExpiry = new JTextField(5);
    private final JPasswordField txtCvv = new JPasswordField(3);
    private final JTextField txtUpi = new JTextField(20);

    /**
     * Primary constructor required by the team.
     */
    public PaymentDialog(Frame owner, int bookingId, double amount) {
        super(owner, "Checkout & Payment", true);
        this.bookingId = bookingId;
        this.amount = amount;
        buildUI();
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Overloaded constructor supporting any Window owner (e.g. JFrame).
     */
    public PaymentDialog(Window owner, int bookingId, double amount) {
        super(owner, "Checkout & Payment", ModalityType.APPLICATION_MODAL);
        this.bookingId = bookingId;
        this.amount = amount;
        buildUI();
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void buildUI() {
        JLabel lblAmount = new JLabel(String.format("Total Amount Due: Rs. %.2f", amount));
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblAmount.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));

        ButtonGroup group = new ButtonGroup();
        group.add(rbCard);
        group.add(rbUpi);

        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        methodPanel.setBorder(BorderFactory.createTitledBorder("Select Payment Method"));
        methodPanel.add(rbCard);
        methodPanel.add(rbUpi);

        JPanel top = new JPanel(new BorderLayout());
        top.add(lblAmount, BorderLayout.NORTH);
        top.add(methodPanel, BorderLayout.CENTER);

        // 1. Card Form Panel
        JPanel cardForm = new JPanel(new GridLayout(3, 2, 8, 8));
        cardForm.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        cardForm.add(new JLabel("Card Number (16 digits):"));
        cardForm.add(txtCardNo);
        cardForm.add(new JLabel("Expiry (MM/YY):"));
        cardForm.add(txtExpiry);
        cardForm.add(new JLabel("CVV (3 digits):"));
        cardForm.add(txtCvv);

        // 2. UPI Form Panel
        JPanel upiForm = new JPanel(new GridLayout(2, 2, 8, 8));
        upiForm.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        upiForm.add(new JLabel("UPI ID / VPA:"));
        upiForm.add(txtUpi);
        upiForm.add(new JLabel("Example:"));
        JLabel lblHint = new JLabel("username@okhdfcbank");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        upiForm.add(lblHint);

        formPanel.add(cardForm, "CARD");
        formPanel.add(upiForm, "UPI");

        rbCard.addActionListener(e -> cardLayout.show(formPanel, "CARD"));
        rbUpi.addActionListener(e -> cardLayout.show(formPanel, "UPI"));

        JButton btnPay = new JButton(String.format("Pay Rs. %.2f", amount));
        btnPay.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JButton btnCancel = new JButton("Cancel");

        btnPay.addActionListener(e -> onPay());
        btnCancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnPay);
        buttons.add(btnCancel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(formPanel, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnPay);
    }

    /**
     * Executes the polymorphic payment strategy and verifies persistence.
     */
    private void onPay() {
        // OOP Strategy Pattern: instantiate concrete strategy based on selection
        PaymentStrategy strategy = rbCard.isSelected()
                ? new CardPayment(txtCardNo.getText(), txtExpiry.getText(), new String(txtCvv.getPassword()))
                : new UPIPayment(txtUpi.getText());

        // Validate payment credentials
        if (!strategy.pay(amount)) {
            JOptionPane.showMessageDialog(this,
                    strategy.getValidationHint(),
                    "Payment Validation Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Generate unique reference and record in database
        String txnRef = "TXN-" + System.currentTimeMillis() + "-" + bookingId;
        Payment payment = new Payment(bookingId, amount, strategy.getMethodName(), txnRef);

        boolean saved = new PaymentDAO().savePayment(payment);
        if (!saved) {
            JOptionPane.showMessageDialog(this,
                    "Failed to record payment in the database. Please try again.",
                    "Payment Error",
                    JOptionPane.ERROR_MESSAGE);
            return; // Do not mark as paid if database save failed!
        }

        paid = true;
        Window owner = getOwner();
        dispose();
        showReceipt(owner, strategy.getMethodName(), txnRef);
    }

    /**
     * Automatically launches the digital ticket receipt after successful payment.
     */
    private void showReceipt(Window owner, String method, String txnRef) {
        String[] info = new PaymentDAO().getBookingSummary(bookingId);
        String movie = info != null ? info[0] : "Movie";
        String when = info != null ? info[1] : "Show Date/Time";
        String seats = info != null ? info[2] : "Seats";

        new TicketReceiptDialog(owner, bookingId, movie, when, seats, amount, method, txnRef)
                .setVisible(true);
    }

    /**
     * Returns true if payment was completed and confirmed.
     * Used by Shada's SeatSelectionFrame to decide whether to release seats.
     */
    public boolean isPaid() {
        return paid;
    }

    // Standalone test launcher
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PaymentDialog d = new PaymentDialog((Frame) null, 1, 450.00);
            d.setVisible(true);
            System.exit(0);
        });
    }
}