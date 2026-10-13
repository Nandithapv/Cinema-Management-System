package views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Digital Cinema Ticket Receipt Dialog (Member 5).
 * 
 * Key Features:
 *  - Displays full ticket details with official Booking Reference Code.
 *  - 'Save as Text file' button to download receipt locally.
 *  - 'Print' button for standard printer output.
 */
public class TicketReceiptDialog extends JDialog {

    private final JTextArea area = new JTextArea(17, 36);
    private final int bookingId;
    private final String receiptText;

    /**
     * Standard constructor matching earlier team conventions.
     */
    public TicketReceiptDialog(Window owner, int bookingId, String movie,
                               String showDateTime, String seats,
                               double amount, String method) {
        this(owner, bookingId, movie, showDateTime, seats, amount, method,
             "BK-" + String.format("%05d", bookingId) + "-" + System.currentTimeMillis() % 10000);
    }

    /**
     * Overloaded constructor accepting an explicit reference code.
     */
    public TicketReceiptDialog(Window owner, int bookingId, String movie,
                               String showDateTime, String seats,
                               double amount, String method, String referenceCode) {
        super(owner, "Ticket Receipt", ModalityType.APPLICATION_MODAL);
        this.bookingId = bookingId;

        this.receiptText = buildText(bookingId, movie, showDateTime, seats, amount, method, referenceCode);

        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        area.setText(receiptText);

        JButton btnSave = new JButton("Save as Text file");
        JButton btnPrint = new JButton("Print");
        JButton btnClose = new JButton("Close");

        btnSave.addActionListener(e -> saveToFile());
        btnPrint.addActionListener(e -> printReceipt());
        btnClose.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnSave);
        buttons.add(btnPrint);
        buttons.add(btnClose);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Saves the ticket text to a .txt file on the user's filesystem.
     */
    private void saveToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Ticket Receipt");
        fileChooser.setSelectedFile(new File("Ticket_Booking_" + bookingId + ".txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(fileToSave)) {
                writer.write(receiptText);
                JOptionPane.showMessageDialog(this,
                        "Receipt saved successfully to:\n" + fileToSave.getAbsolutePath(),
                        "Saved Successfully",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error saving file: " + ex.getMessage(),
                        "File Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Prints the receipt text using standard Java Print API.
     */
    private void printReceipt() {
        try {
            boolean complete = area.print();
            if (complete) {
                JOptionPane.showMessageDialog(this, "Printing complete.", "Print", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Could not print: " + ex.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Builds the formatted ticket receipt text.
     */
    private String buildText(int id, String movie, String when, String seats,
                             double amount, String method, String referenceCode) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("        CINEMA TICKET RECEIPT            \n");
        sb.append("=========================================\n");
        sb.append(String.format("Reference Code : %s%n", referenceCode));
        sb.append(String.format("Booking ID     : %d%n", id));
        sb.append(String.format("Issue Date     : %s%n", timestamp));
        sb.append("-----------------------------------------\n");
        sb.append(String.format("Movie          : %s%n", movie));
        sb.append(String.format("Show Schedule  : %s%n", when));
        sb.append(String.format("Seats Reserved : %s%n", seats));
        sb.append("-----------------------------------------\n");
        sb.append(String.format("Payment Method : %s%n", method));
        sb.append(String.format("Total Amount   : Rs. %.2f%n", amount));
        sb.append("Status         : CONFIRMED\n");
        sb.append("=========================================\n");
        sb.append(" Please present this ticket at entry.   \n");
        sb.append("           Enjoy the movie!             \n");
        sb.append("=========================================\n");
        return sb.toString();
    }
}