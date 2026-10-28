package views;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
 * Payment Dialog for Checkout (Member 5), restyled to match the Cineva home page.
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
 *
 * Only the look changed: the payment strategy, validation, saving and receipt logic are as before.
 */
public class PaymentDialog extends JDialog {

    private final int bookingId;
    private final double amount;
    private boolean paid = false;

    // The card / UPI switch drives these two (they decide which strategy onPay() uses).
    private final JRadioButton rbCard = new JRadioButton("Credit / Debit Card", true);
    private final JRadioButton rbUpi = new JRadioButton("UPI (GPay / PhonePe / Paytm)");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel formPanel = new JPanel(cardLayout);

    private final JTextField txtCardNo = new CinevaUI.RoundField(16);
    private final JTextField txtExpiry = new CinevaUI.RoundField(5);
    private final JPasswordField txtCvv = new CinevaUI.RoundPassword(3);
    private final JTextField txtUpi = new CinevaUI.RoundField(20);

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
        ButtonGroup group = new ButtonGroup();
        group.add(rbCard);
        group.add(rbUpi);

        txtCardNo.putClientProperty("JTextField.placeholderText", "1234 5678 9012 3456");
        txtExpiry.putClientProperty("JTextField.placeholderText", "MM/YY");
        txtCvv.putClientProperty("JTextField.placeholderText", "123");
        txtUpi.putClientProperty("JTextField.placeholderText", "username@okhdfcbank");

        // ---- top: amount + method switch
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));

        CinevaUI.TrackedText cap = CinevaUI.TrackedText.caps("Total due", CinevaUI.DIM);
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);
        CinevaUI.TrackedText amt = CinevaUI.TrackedText.light(String.format("Rs. %.2f", amount), 42f, Color.WHITE, 0.02f);
        amt.setAlignmentX(Component.LEFT_ALIGNMENT);

        CinevaUI.Switch method = new CinevaUI.Switch(new String[] {"Card", "UPI"}, idx -> {
            if (idx == 0) {
                rbCard.setSelected(true);
                cardLayout.show(formPanel, "CARD");
            } else {
                rbUpi.setSelected(true);
                cardLayout.show(formPanel, "UPI");
            }
        });
        method.setAlignmentX(Component.LEFT_ALIGNMENT);
        method.setMaximumSize(new Dimension(320, 42));
        method.setPreferredSize(new Dimension(320, 42));

        top.add(cap);
        top.add(javax.swing.Box.createVerticalStrut(2));
        top.add(amt);
        top.add(javax.swing.Box.createVerticalStrut(18));
        top.add(method);
        top.add(javax.swing.Box.createVerticalStrut(22));

        // ---- 1. Card form
        JPanel cardForm = new JPanel(new GridBagLayout());
        cardForm.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 6, 0);
        cardForm.add(CinevaUI.TrackedText.caps("Card number (16 digits)", CinevaUI.DIM), gc);
        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 16, 0);
        cardForm.add(txtCardNo, gc);

        gc.gridwidth = 1;
        gc.gridy = 2;
        gc.insets = new Insets(0, 0, 6, 8);
        cardForm.add(CinevaUI.TrackedText.caps("Expiry (MM/YY)", CinevaUI.DIM), gc);
        gc.gridx = 1;
        gc.insets = new Insets(0, 8, 6, 0);
        cardForm.add(CinevaUI.TrackedText.caps("CVV (3 digits)", CinevaUI.DIM), gc);
        gc.gridx = 0;
        gc.gridy = 3;
        gc.insets = new Insets(0, 0, 0, 8);
        cardForm.add(txtExpiry, gc);
        gc.gridx = 1;
        gc.insets = new Insets(0, 8, 0, 0);
        cardForm.add(txtCvv, gc);

        // ---- 2. UPI form
        JPanel upiForm = new JPanel(new GridBagLayout());
        upiForm.setOpaque(false);
        GridBagConstraints uc = new GridBagConstraints();
        uc.fill = GridBagConstraints.HORIZONTAL;
        uc.weightx = 1;
        uc.gridx = 0;
        uc.anchor = GridBagConstraints.NORTHWEST;
        uc.gridy = 0;
        uc.insets = new Insets(0, 0, 6, 0);
        upiForm.add(CinevaUI.TrackedText.caps("UPI ID / VPA", CinevaUI.DIM), uc);
        uc.gridy = 1;
        uc.insets = new Insets(0, 0, 8, 0);
        upiForm.add(txtUpi, uc);
        uc.gridy = 2;
        uc.weighty = 1;
        uc.insets = new Insets(0, 2, 0, 0);
        JLabel lblHint = new JLabel("Example: username@okhdfcbank");
        lblHint.setFont(CustomerHomeFrame.Fonts.get(false, 12f));
        lblHint.setForeground(CinevaUI.DIM);
        upiForm.add(lblHint, uc);

        formPanel.setOpaque(false);
        formPanel.add(cardForm, "CARD");
        formPanel.add(upiForm, "UPI");

        // ---- buttons
        JButton btnPay = CinevaUI.pill(String.format("Pay Rs. %.2f", amount), CinevaUI.SOLID);
        JButton btnCancel = CinevaUI.pill("Cancel", CinevaUI.OUTLINE);

        btnPay.addActionListener(e -> onPay());
        btnCancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(26, 0, 0, 0));
        buttons.add(btnCancel);
        buttons.add(btnPay);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CinevaUI.BG);
        root.setBorder(BorderFactory.createEmptyBorder(30, 36, 28, 36));
        root.add(top, BorderLayout.NORTH);
        root.add(formPanel, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        root.setPreferredSize(new Dimension(520, root.getPreferredSize().height));

        setContentPane(root);
        setResizable(false);
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
            com.formdev.flatlaf.FlatDarkLaf.setup();
            PaymentDialog d = new PaymentDialog((Frame) null, 1, 450.00);
            d.setVisible(true);
            System.exit(0);
        });
    }
}