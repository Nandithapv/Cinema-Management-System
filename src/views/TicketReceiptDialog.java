package views;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Modern Dark-Themed Digital E-Ticket Dialog (Member 5: Renjitha Raju).
 * 
 * Styled specifically to match the mobile-ready digital E-Ticket interface:
 *  - Navy Header with system branding and E-ticket icon.
 *  - Movie Title and formatted 2x2 details grid (Date, Time, Screen, Seats).
 *  - Cinema ticket perforated divider with side ticket cut-out notches.
 *  - Summary rows (Booking Reference, Payment Method, Total Amount Paid).
 *  - 'Save as text file' and 'Close' styled action buttons.
 */
public class TicketReceiptDialog extends JDialog {

    // Theme Palette matching the reference UI
    private static final Color BG_DARK = new Color(18, 20, 26);
    private static final Color CARD_BG = new Color(24, 26, 32);
    private static final Color HEADER_BG = new Color(11, 38, 70);
    private static final Color HEADER_SUBTEXT = new Color(110, 160, 215);
    private static final Color HEADER_TITLE = new Color(66, 153, 225);
    private static final Color TEXT_MUTED = new Color(156, 163, 175);
    private static final Color TEXT_WHITE = new Color(245, 247, 250);
    private static final Color BTN_BG = new Color(34, 38, 48);
    private static final Color BTN_BORDER = new Color(55, 62, 78);
    private static final Color DIVIDER_COLOR = new Color(60, 66, 82);

    private final int bookingId;
    private final String movie;
    private final String formattedDate;
    private final String formattedTime;
    private final String screenName;
    private final String seats;
    private final double amount;
    private final String method;
    private final String referenceCode;

    /**
     * Standard constructor matching earlier calls in the codebase.
     */
    public TicketReceiptDialog(Window owner, int bookingId, String movie,
                               String showDateTime, String seats,
                               double amount, String method) {
        this(owner, bookingId, movie, showDateTime, seats, amount, method,
             "CIN" + (bookingId % 90 + 10) + "-" + (1000 + (Math.abs((movie + seats).hashCode()) % 9000)));
    }

    /**
     * Constructor accepting explicit booking reference code.
     */
    public TicketReceiptDialog(Window owner, int bookingId, String movie,
                               String showDateTime, String seats,
                               double amount, String method, String referenceCode) {
        super(owner, "E-ticket", ModalityType.APPLICATION_MODAL);

        this.bookingId = bookingId;
        this.movie = (movie == null || movie.trim().isEmpty()) ? "Sample Movie Title" : movie.trim();
        this.seats = (seats == null || seats.trim().isEmpty()) ? "A1" : seats.trim();
        this.amount = amount;
        this.method = (method == null || method.trim().isEmpty() || "-".equals(method)) ? "UPI" : method.trim();

        // Format clean reference code e.g. CIN12-4821
        if (referenceCode == null || referenceCode.trim().isEmpty() || "-".equals(referenceCode)) {
            this.referenceCode = "CIN" + (bookingId % 90 + 10) + "-" + (1000 + (Math.abs((movie + seats).hashCode()) % 9000));
        } else if (referenceCode.startsWith("TXN-")) {
            this.referenceCode = "CIN" + (bookingId % 90 + 10) + "-" + referenceCode.substring(Math.max(0, referenceCode.length() - 4));
        } else {
            this.referenceCode = referenceCode;
        }

        // Parse date, time, and screen from showDateTime
        String[] parsed = parseDateTime(showDateTime);
        this.formattedDate = parsed[0];
        this.formattedTime = parsed[1];
        this.screenName = parsed[2];

        buildTicketUI();

        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Builds the exact E-ticket card UI as shown in the screenshot.
     */
    private void buildTicketUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));

        // Outer Card Container with rounded look
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(40, 44, 56), 1, true));

        // 1. TOP HEADER (Deep Navy Blue Banner)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(HEADER_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel lblSystem = new JLabel("CINEMA MANAGEMENT SYSTEM", SwingConstants.CENTER);
        lblSystem.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSystem.setForeground(HEADER_SUBTEXT);
        lblSystem.setAlignmentX(CENTER_ALIGNMENT);

        JLabel lblETicket = new JLabel("🎟  E-ticket", SwingConstants.CENTER);
        lblETicket.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblETicket.setForeground(HEADER_TITLE);
        lblETicket.setAlignmentX(CENTER_ALIGNMENT);

        headerPanel.add(lblSystem);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(lblETicket);
        card.add(headerPanel);

        // 2. MOVIE DETAILS SECTION
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(CARD_BG);
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));

        JLabel lblMovieHeader = new JLabel("Movie");
        lblMovieHeader.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMovieHeader.setForeground(TEXT_MUTED);

        JLabel lblMovieTitle = new JLabel(movie);
        lblMovieTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblMovieTitle.setForeground(TEXT_WHITE);

        bodyPanel.add(lblMovieHeader);
        bodyPanel.add(Box.createVerticalStrut(4));
        bodyPanel.add(lblMovieTitle);
        bodyPanel.add(Box.createVerticalStrut(16));

        // 2x2 Grid: Date, Time, Screen, Seats
        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 12));
        grid.setBackground(CARD_BG);

        grid.add(createInfoBlock("Date", formattedDate));
        grid.add(createInfoBlock("Time", formattedTime));
        grid.add(createInfoBlock("Screen", screenName));
        grid.add(createInfoBlock("Seats", seats));

        bodyPanel.add(grid);
        card.add(bodyPanel);

        // 3. CINEMA TICKET CUTOUT & DASHED DIVIDER
        card.add(new TicketDivider(BG_DARK, CARD_BG, DIVIDER_COLOR));

        // 4. LOWER PAYMENT & REFERENCE SUMMARY
        JPanel summaryPanel = new JPanel(new GridBagLayout());
        summaryPanel.setBackground(CARD_BG);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(14, 22, 18, 22));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);

        // Row 1: Booking reference
        addSummaryRow(summaryPanel, gbc, 0, "Booking reference", referenceCode, new Font("Segoe UI", Font.BOLD, 14), TEXT_WHITE);

        // Row 2: Payment method
        addSummaryRow(summaryPanel, gbc, 1, "Payment method", method, new Font("Segoe UI", Font.BOLD, 14), TEXT_WHITE);

        // Row 3: Amount paid (with large bold currency)
        String amountText = String.format(Locale.ENGLISH, "₹%.2f", amount);
        addSummaryRow(summaryPanel, gbc, 2, "Amount paid", amountText, new Font("Segoe UI", Font.BOLD, 22), TEXT_WHITE);

        card.add(summaryPanel);
        root.add(card, BorderLayout.CENTER);

        // 5. BOTTOM ACTION BUTTONS
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 14));
        actionPanel.setBackground(BG_DARK);

        JButton btnSave = createStyledButton("⤓  Save as text file", 165);
        JButton btnClose = createStyledButton("Close", 120);

        btnSave.addActionListener(e -> saveAsTextFile());
        btnClose.addActionListener(e -> dispose());

        actionPanel.add(btnSave);
        actionPanel.add(btnClose);

        root.add(actionPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    /**
     * Creates an individual info block with small muted label above bold white text.
     */
    private JPanel createInfoBlock(String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD_BG);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 14));
        val.setForeground(TEXT_WHITE);

        p.add(lbl);
        p.add(Box.createVerticalStrut(3));
        p.add(val);
        return p;
    }

    /**
     * Adds a key-value row with left label and right-aligned value.
     */
    private void addSummaryRow(JPanel panel, GridBagConstraints gbc, int row,
                               String label, String value, Font valFont, Color valColor) {
        gbc.gridy = row;

        gbc.gridx = 0;
        gbc.weightx = 0.5;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_MUTED);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        JLabel val = new JLabel(value, SwingConstants.RIGHT);
        val.setFont(valFont);
        val.setForeground(valColor);
        panel.add(val, gbc);
    }

    /**
     * Creates a styled modern dark button with hover highlights.
     */
    private JButton createStyledButton(String text, int width) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(width, 40));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(TEXT_WHITE);
        btn.setBackground(BTN_BG);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BTN_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(48, 54, 68));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(90, 100, 125), 1, true),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(BTN_BG);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BTN_BORDER, 1, true),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }
        });
        return btn;
    }

    /**
     * Saves ticket details into a clean text file on the user's computer.
     */
    private void saveAsTextFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save E-ticket Receipt");
        chooser.setSelectedFile(new File("Ticket_" + referenceCode + ".txt"));

        int choice = chooser.showSaveDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File targetFile = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(targetFile)) {
                writer.write(generateReceiptText());
                JOptionPane.showMessageDialog(this,
                        "E-ticket saved successfully:\n" + targetFile.getAbsolutePath(),
                        "Ticket Saved",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not save ticket file: " + ex.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Generates clean formatted receipt text matching the ticket details.
     */
    private String generateReceiptText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("       CINEMA MANAGEMENT SYSTEM          \n");
        sb.append("               E-TICKET                  \n");
        sb.append("=========================================\n");
        sb.append(String.format("Movie       : %s%n", movie));
        sb.append(String.format("Date        : %s%n", formattedDate));
        sb.append(String.format("Time        : %s%n", formattedTime));
        sb.append(String.format("Screen      : %s%n", screenName));
        sb.append(String.format("Seats       : %s%n", seats));
        sb.append("-----------------------------------------\n");
        sb.append(String.format("Booking Ref : %s%n", referenceCode));
        sb.append(String.format("Booking ID  : %d%n", bookingId));
        sb.append(String.format("Method      : %s%n", method));
        sb.append(String.format("Amount Paid : Rs. %.2f%n", amount));
        sb.append("Status      : CONFIRMED\n");
        sb.append("=========================================\n");
        sb.append("     Please show this at entrance.       \n");
        sb.append("           Enjoy the movie!              \n");
        sb.append("=========================================\n");
        return sb.toString();
    }

    /**
     * Smart date & time parser: formats "2026-10-15 18:30:00" into "15 Oct 2026" & "06:30 PM".
     */
    private String[] parseDateTime(String showDateTime) {
        String dateStr = "15 Oct 2026";
        String timeStr = "06:30 PM";
        String screen = "Screen 2";

        if (showDateTime == null || showDateTime.trim().isEmpty() || "-".equals(showDateTime)) {
            return new String[] { dateStr, timeStr, screen };
        }

        try {
            String[] parts = showDateTime.trim().split("\\s+");
            if (parts.length >= 1) {
                try {
                    LocalDate ld = LocalDate.parse(parts[0]);
                    dateStr = ld.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
                } catch (Exception ex) {
                    dateStr = parts[0];
                }
            }
            if (parts.length >= 2) {
                try {
                    LocalTime lt = LocalTime.parse(parts[1]);
                    timeStr = lt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
                } catch (Exception ex) {
                    timeStr = parts[1];
                }
            }
            if (parts.length >= 3) {
                // If screen name was concatenated
                screen = parts[2];
            }
        } catch (Exception e) {
            // fallback gracefully
        }
        return new String[] { dateStr, timeStr, screen };
    }

    /**
     * Custom component that draws a dashed perforation line with left and right semicircular cutouts.
     */
    private static class TicketDivider extends JPanel {
        private final Color outerBg;
        private final Color cardBg;
        private final Color lineColor;

        public TicketDivider(Color outerBg, Color cardBg, Color lineColor) {
            this.outerBg = outerBg;
            this.cardBg = cardBg;
            this.lineColor = lineColor;
            setPreferredSize(new Dimension(340, 26));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int radius = 18;
            int cy = h / 2;

            // Fill card background
            g2.setColor(cardBg);
            g2.fillRect(0, 0, w, h);

            // Left cutout (semicircle inward)
            g2.setColor(outerBg);
            g2.fillArc(-radius / 2, cy - radius / 2, radius, radius, -90, 180);

            // Right cutout (semicircle inward)
            g2.fillArc(w - radius / 2, cy - radius / 2, radius, radius, 90, 180);

            // Dashed horizontal perforation line
            g2.setColor(lineColor);
            float[] dash = { 5.0f, 5.0f };
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.drawLine(radius + 4, cy, w - radius - 4, cy);

            g2.dispose();
        }
    }

    /**
     * Standalone UI preview launcher.
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            TicketReceiptDialog dlg = new TicketReceiptDialog(
                    null,
                    12,
                    "Sample Movie Title",
                    "2026-10-15 18:30:00",
                    "C4, C5, C6",
                    540.00,
                    "UPI",
                    "CIN12-4821"
            );
            dlg.setVisible(true);
            System.exit(0);
        });
    }
}