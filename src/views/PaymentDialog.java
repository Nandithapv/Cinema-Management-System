package views;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
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
import models.CardPayment;
import models.Payment;
import models.PaymentStrategy;
import models.UPIPayment;

/**
 * Usage: new PaymentDialog(ownerFrame, bookingId, totalAmount).setVisible(true);
 * On success it saves the payment and opens the receipt automatically.
 */
public class PaymentDialog extends JDialog {

    private final int bookingId;
    private final double amount;
    private boolean paid = false;

    private final JRadioButton rbCard = new JRadioButton("Card", true);
    private final JRadioButton rbUpi = new JRadioButton("UPI");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel formPanel = new JPanel(cardLayout);

    private final JTextField txtCardNo = new JTextField(16);
    private final JTextField txtExpiry = new JTextField(5);
    private final JPasswordField txtCvv = new JPasswordField(3);
    private final JTextField txtUpi = new JTextField(18);

    public PaymentDialog(Frame owner, int bookingId, double amount) {
        super(owner, "Payment", true);
        this.bookingId = bookingId;
        this.amount = amount;
        buildUI();
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void buildUI() {
        JLabel lblAmount = new JLabel(String.format("Amount to pay: Rs. %.2f", amount));
        lblAmount.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        ButtonGroup group = new ButtonGroup();
        group.add(rbCard);
        group.add(rbUpi);
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        methodPanel.add(new JLabel("Pay with:"));
        methodPanel.add(rbCard);
        methodPanel.add(rbUpi);

        JPanel top = new JPanel(new BorderLayout());
        top.add(lblAmount, BorderLayout.NORTH);
        top.add(methodPanel, BorderLayout.CENTER);

        JPanel cardForm = new JPanel(new GridLayout(3, 2, 8, 8));
        cardForm.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        cardForm.add(new JLabel("Card number:"));
        cardForm.add(txtCardNo);
        cardForm.add(new JLabel("Expiry (MM/YY):"));
        cardForm.add(txtExpiry);
        cardForm.add(new JLabel("CVV:"));
        cardForm.add(txtCvv);

        JPanel upiForm = new JPanel(new GridLayout(1, 2, 8, 8));
        upiForm.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        upiForm.add(new JLabel("UPI ID:"));
        upiForm.add(txtUpi);

        formPanel.add(cardForm, "CARD");
        formPanel.add(upiForm, "UPI");

        rbCard.addActionListener(e -> cardLayout.show(formPanel, "CARD"));
        rbUpi.addActionListener(e -> cardLayout.show(formPanel, "UPI"));

        JButton btnPay = new JButton("Pay");
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

    private void onPay() {
        // Polymorphism: same call, different behaviour per strategy
        PaymentStrategy strategy = rbCard.isSelected()
                ? new CardPayment(txtCardNo.getText(), txtExpiry.getText(),
                                  new String(txtCvv.getPassword()))
                : new UPIPayment(txtUpi.getText());

        if (!strategy.pay(amount)) {
            JOptionPane.showMessageDialog(this, strategy.getValidationHint(),
                    "Payment failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Payment payment = new Payment(bookingId, amount, strategy.getMethodName(), "SUCCESS");
        new PaymentDAO().savePayment(payment);
        paid = true;

        Window owner = getOwner();
        dispose();
        showReceipt(owner, strategy.getMethodName());
    }

    private void showReceipt(Window owner, String method) {
        String[] info = new PaymentDAO().getBookingSummary(bookingId);
        String movie = info != null ? info[0] : "N/A";
        String when = info != null ? info[1] : "N/A";
        String seats = info != null ? info[2] : "N/A";
        new TicketReceiptDialog(owner, bookingId, movie, when, seats, amount, method)
                .setVisible(true);
    }

    public boolean isPaid() { return paid; }

    // Quick standalone test (no database needed to see the dialog)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PaymentDialog d = new PaymentDialog(null, 1, 450.00);
            d.setVisible(true);
            System.exit(0);
        });
    }
}