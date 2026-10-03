package views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.print.PrinterException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class TicketReceiptDialog extends JDialog {

    private final JTextArea area = new JTextArea(14, 34);

    public TicketReceiptDialog(Window owner, int bookingId, String movie,
                               String showDateTime, String seats,
                               double amount, String method) {
        super(owner, "Ticket Receipt", ModalityType.APPLICATION_MODAL);

        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        area.setText(buildText(bookingId, movie, showDateTime, seats, amount, method));

        JButton btnPrint = new JButton("Print");
        JButton btnClose = new JButton("Close");
        btnPrint.addActionListener(e -> {
            try {
                area.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Could not print: " + ex.getMessage());
            }
        });
        btnClose.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnPrint);
        buttons.add(btnClose);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private String buildText(int id, String movie, String when, String seats,
                             double amount, String method) {
        StringBuilder sb = new StringBuilder();
        sb.append("=================================\n");
        sb.append("        CINEMA TICKET RECEIPT\n");
        sb.append("=================================\n");
        sb.append(String.format("Booking ID : %d%n", id));
        sb.append(String.format("Movie      : %s%n", movie));
        sb.append(String.format("Show       : %s%n", when));
        sb.append(String.format("Seats      : %s%n", seats));
        sb.append("---------------------------------\n");
        sb.append(String.format("Paid via   : %s%n", method));
        sb.append(String.format("Amount     : Rs. %.2f%n", amount));
        sb.append("=================================\n");
        sb.append("     Enjoy the show!\n");
        return sb.toString();
    }
}