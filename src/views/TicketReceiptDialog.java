package views;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Modern Mobile E-Ticket Dialog (Member 5: Renjitha Raju & Member 6: Nanditha P V).
 * 
 * Styled specifically to combine Pinterest Reference 1 & 2:
 *  - Floating cinema ticket card with circular side notches & perforated tear line
 *  - Top section: Cinematic movie poster banner with gradient overlay & verified pass badge
 *  - High-contrast stub surface for crystal-clear readability of Date, Time, Screen & Seats
 *  - High-resolution realistic Barcode graphic with human-readable ticket serial
 *  - Download / Save text receipt and Close action buttons
 */
public class TicketReceiptDialog extends JDialog {

    // Cineva palette: black page, clean light stub for readability
    private static final Color APP_BG          = CinevaUI.BG;
    private static final Color STUB_BG         = new Color(240, 240, 243);
    private static final Color STUB_TEXT_DARK  = new Color(14, 14, 18);
    private static final Color STUB_TEXT_MUTED = new Color(112, 114, 124);
    private static final Color ACCENT_GOLD     = new Color(255, 196, 62);

    private volatile BufferedImage poster;     // loaded in the background (see loadPoster)
    private JPanel topBannerRef;

    private final int bookingId;
    private final String movie;
    private final String formattedDate;
    private final String formattedTime;
    private final String screenName;
    private final String seats;
    private final double amount;
    private final String method;
    private final String referenceCode;

    public TicketReceiptDialog(Window owner, int bookingId, String movie,
                               String showDateTime, String seats,
                               double amount, String method) {
        this(owner, bookingId, movie, showDateTime, seats, amount, method,
             "CIN" + (bookingId % 90 + 10) + "-" + (1000 + (Math.abs((movie + seats).hashCode()) % 9000)));
    }

    public TicketReceiptDialog(Window owner, int bookingId, String movie,
                               String showDateTime, String seats,
                               double amount, String method, String referenceCode) {
        super(owner, "Digital Mobile Ticket", ModalityType.APPLICATION_MODAL);

        this.bookingId = bookingId;
        this.movie = (movie == null || movie.trim().isEmpty()) ? "Interstellar" : movie.trim();
        this.seats = (seats == null || seats.trim().isEmpty()) ? "A1, A2" : seats.trim();
        this.amount = amount;
        this.method = (method == null || method.trim().isEmpty() || "-".equals(method)) ? "UPI" : method.trim();

        if (referenceCode == null || referenceCode.trim().isEmpty() || "-".equals(referenceCode)) {
            this.referenceCode = "CIN" + (bookingId % 90 + 10) + "-" + (1000 + (Math.abs((movie + seats).hashCode()) % 9000));
        } else if (referenceCode.startsWith("TXN-")) {
            this.referenceCode = "CIN" + (bookingId % 90 + 10) + "-" + referenceCode.substring(Math.max(0, referenceCode.length() - 4));
        } else {
            this.referenceCode = referenceCode;
        }

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

    private void buildTicketUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(APP_BG);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Subtitle indicator like mobile app
        JPanel topIndicator = new JPanel();
        topIndicator.setLayout(new BoxLayout(topIndicator, BoxLayout.Y_AXIS));
        topIndicator.setOpaque(false);
        topIndicator.setBorder(new EmptyBorder(0, 0, 14, 0));

        CinevaUI.TrackedText lblHeader = CinevaUI.TrackedText.light("MOBILE CINEMA PASS", 22f, Color.WHITE, 0.08f);
        lblHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Scan the barcode at the entrance", SwingConstants.CENTER);
        lblSub.setFont(CustomerHomeFrame.Fonts.get(false, 12.5f));
        lblSub.setForeground(CinevaUI.DIM);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        topIndicator.add(lblHeader);
        topIndicator.add(Box.createVerticalStrut(4));
        topIndicator.add(lblSub);
        root.add(topIndicator, BorderLayout.NORTH);

        // ----------------- TICKET CONTAINER -----------------
        JPanel ticketCard = new JPanel();
        ticketCard.setLayout(new BoxLayout(ticketCard, BoxLayout.Y_AXIS));
        ticketCard.setOpaque(false);
        ticketCard.setPreferredSize(new Dimension(380, 520));

        // 1. Top Section: Cinematic Movie Poster Card (Pinterest 1 style)
        JPanel topBanner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();

                // Draw top rounded clip
                Shape clip = new RoundRectangle2D.Float(0, 0, w, h + 16, 20, 20);
                g2.setClip(clip);

                BufferedImage posterImg = poster;
                if (posterImg != null) {
                    float sc = Math.max(w / (float) posterImg.getWidth(), h / (float) posterImg.getHeight());
                    int sw = Math.round(w / sc), sh = Math.round(h / sc);
                    int sx = (posterImg.getWidth() - sw) / 2, sy = (posterImg.getHeight() - sh) / 2;
                    g2.drawImage(posterImg, 0, 0, w, h, sx, sy, sx + sw, sy + sh, null);
                    // Dark atmospheric gradient overlay
                    GradientPaint grad = new GradientPaint(
                        0, 0, new Color(5, 5, 7, 60),
                        0, h, new Color(5, 5, 7, 240)
                    );
                    g2.setPaint(grad);
                    g2.fillRect(0, 0, w, h);
                } else {
                    GradientPaint grad = new GradientPaint(
                        0, 0, new Color(36, 36, 44),
                        0, h, new Color(8, 8, 10)
                    );
                    g2.setPaint(grad);
                    g2.fillRect(0, 0, w, h);
                }

                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                // Confirmed badge (outlined pill)
                String badge = "CONFIRMED";
                g2.setFont(CustomerHomeFrame.Fonts.get(true, 10f));
                FontMetrics bfm = g2.getFontMetrics();
                float bw = CustomerHomeFrame.trackedWidth(bfm, badge, 1.6f) + 30;
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fill(new RoundRectangle2D.Float(16, 16, bw, 26, 26, 26));
                g2.setColor(new Color(255, 255, 255, 170));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(16.5f, 16.5f, bw - 1, 25, 26, 26));
                g2.setColor(Color.WHITE);
                CustomerHomeFrame.drawTracked(g2, badge, 16 + 20, 16 + (26 + bfm.getAscent() - bfm.getDescent()) / 2f, 1.6f);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(16 + 9, 29, 16 + 12, 32);
                g2.drawLine(16 + 12, 32, 16 + 16, 25);

                // Movie title (thin) - shrinks to fit the card
                float ts = 28f;
                Font tf = new Font("Segoe UI Light", Font.PLAIN, 12).deriveFont(ts);
                while (ts > 16f && g2.getFontMetrics(tf).stringWidth(movie) > w - 34) {
                    ts -= 1f;
                    tf = tf.deriveFont(ts);
                }
                g2.setColor(Color.WHITE);
                g2.setFont(tf);
                g2.drawString(movie, 18, h - 40);

                // Hall subtitle
                g2.setColor(new Color(200, 204, 214));
                g2.setFont(CustomerHomeFrame.Fonts.get(true, 10.5f));
                CustomerHomeFrame.drawTracked(g2, ("CINEVA   \u00b7   " + screenName).toUpperCase(), 18, h - 18, 1.8f);

                g2.dispose();
            }
        };
        topBannerRef = topBanner;
        loadPoster();
        topBanner.setPreferredSize(new Dimension(380, 220));
        topBanner.setMaximumSize(new Dimension(380, 220));
        topBanner.setOpaque(false);
        ticketCard.add(topBanner);

        // 2. Middle Section: Perforated Tear Notch Line (Pinterest 1 & 2 combination)
        ticketCard.add(new ModernTicketDivider(APP_BG, STUB_BG, new Color(190, 190, 210)));

        // 3. Bottom Section: High-contrast Clean Stub with Details & Barcode
        JPanel stubPanel = new JPanel();
        stubPanel.setLayout(new BoxLayout(stubPanel, BoxLayout.Y_AXIS));
        stubPanel.setBackground(STUB_BG);
        stubPanel.setBorder(new EmptyBorder(14, 20, 16, 20));

        // Details 2x2 Grid
        JPanel detailsGrid = new JPanel(new GridLayout(2, 2, 14, 10));
        detailsGrid.setOpaque(false);

        detailsGrid.add(createStubField("DATE", formattedDate));
        detailsGrid.add(createStubField("TIME", formattedTime));
        detailsGrid.add(createStubField("HALL / SCREEN", screenName));
        detailsGrid.add(createStubField("SEATS", seats));

        stubPanel.add(detailsGrid);
        stubPanel.add(Box.createVerticalStrut(10));

        // Summary Line: Booking ID & Payment Info
        JPanel summaryRow = new JPanel(new BorderLayout());
        summaryRow.setOpaque(false);
        summaryRow.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(225, 222, 235)));

        JLabel lblRef = new JLabel("Ref: " + referenceCode + " (" + method + ")");
        lblRef.setFont(CustomerHomeFrame.Fonts.get(true, 11f));
        lblRef.setForeground(STUB_TEXT_MUTED);

        JLabel lblPrice = new JLabel(String.format(Locale.ENGLISH, "Rs. %.2f", amount));
        lblPrice.setFont(CustomerHomeFrame.Fonts.get(true, 17f));
        lblPrice.setForeground(STUB_TEXT_DARK);

        summaryRow.add(lblRef, BorderLayout.WEST);
        summaryRow.add(lblPrice, BorderLayout.EAST);
        summaryRow.setBorder(new EmptyBorder(6, 0, 6, 0));
        stubPanel.add(summaryRow);

        stubPanel.add(Box.createVerticalStrut(10));

        // High-Resolution Vertical/Horizontal Barcode Graphic (From Pinterest 2)
        JPanel barcodePanel = createBarcodeComponent(referenceCode);
        stubPanel.add(barcodePanel);

        ticketCard.add(stubPanel);
        root.add(ticketCard, BorderLayout.CENTER);

        // ----------------- ACTION BUTTONS -----------------
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 12));
        actionPanel.setBackground(APP_BG);

        JButton btnSave = CinevaUI.pill("Save as text", CinevaUI.OUTLINE);
        btnSave.addActionListener(e -> saveAsTextFile());

        JButton btnClose = CinevaUI.pill("Done", CinevaUI.SOLID);
        btnClose.addActionListener(e -> dispose());

        actionPanel.add(btnSave);
        actionPanel.add(btnClose);

        root.add(actionPanel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createStubField(String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        CinevaUI.TrackedText lbl = CinevaUI.TrackedText.caps(label, STUB_TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(CustomerHomeFrame.Fonts.get(true, 15f));
        val.setForeground(STUB_TEXT_DARK);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lbl);
        p.add(Box.createVerticalStrut(1));
        p.add(val);
        return p;
    }

    /**
     * Draws authentic, crisp cinema barcode stripes and serial text.
     */
    private JPanel createBarcodeComponent(String serial) {
        JPanel barcode = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight() - 14;

                g2.setColor(new Color(25, 25, 35));

                // Deterministic stripe pattern based on serial
                int[] pattern = { 3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 1, 2, 3, 1, 4, 2, 1, 3, 2, 1, 3, 4, 1, 2, 3 };
                int x = 20;
                int idx = 0;
                while (x < w - 24) {
                    int barW = pattern[idx % pattern.length];
                    int gapW = (pattern[(idx + 1) % pattern.length] % 3) + 1;
                    g2.fillRect(x, 2, barW, h);
                    x += barW + gapW;
                    idx++;
                }

                // Serial Text below
                g2.setColor(STUB_TEXT_DARK);
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                String codeStr = "* " + serial + " *";
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(codeStr)) / 2;
                g2.drawString(codeStr, tx, getHeight() - 2);

                g2.dispose();
            }
        };
        barcode.setPreferredSize(new Dimension(340, 48));
        barcode.setMaximumSize(new Dimension(340, 48));
        barcode.setOpaque(false);
        return barcode;
    }

    /** Finds the poster the same way the home page does, in the background. */
    private void loadPoster() {
        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() {
                return CustomerHomeFrame.PosterStrip.readPoster(new models.Movie(0, movie, "", 1, ""));
            }
            @Override protected void done() {
                try {
                    poster = get();
                    if (topBannerRef != null) topBannerRef.repaint();
                } catch (Exception ignored) {
                    // gradient placeholder stays
                }
            }
        }.execute();
    }

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

    private String generateReceiptText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("       CINEMA MANAGEMENT SYSTEM          \n");
        sb.append("            DIGITAL E-TICKET             \n");
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

    private String[] parseDateTime(String showDateTime) {
        String dateStr = "15 Oct 2026";
        String timeStr = "06:30 PM";
        String screen = "Screen 1";

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
                screen = parts[2];
            }
        } catch (Exception e) {}
        return new String[] { dateStr, timeStr, screen };
    }

    /**
     * Modern ticket perforated tear divider with side cutout notches.
     */
    private static class ModernTicketDivider extends JPanel {
        private final Color outerBg;
        private final Color stubBg;
        private final Color dashColor;

        public ModernTicketDivider(Color outerBg, Color stubBg, Color dashColor) {
            this.outerBg = outerBg;
            this.stubBg = stubBg;
            this.dashColor = dashColor;
            setPreferredSize(new Dimension(380, 24));
            setMaximumSize(new Dimension(380, 24));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int radius = 22;
            int cy = h / 2;

            // Fill stub background
            g2.setColor(stubBg);
            g2.fillRect(0, 0, w, h);

            // Left cutout (semicircle inward)
            g2.setColor(outerBg);
            g2.fillArc(-radius / 2, cy - radius / 2, radius, radius, -90, 180);

            // Right cutout (semicircle inward)
            g2.fillArc(w - radius / 2, cy - radius / 2, radius, radius, 90, 180);

            // Dashed horizontal perforation line
            g2.setColor(dashColor);
            float[] dash = { 5.0f, 5.0f };
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.drawLine(radius + 6, cy, w - radius - 6, cy);

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            com.formdev.flatlaf.FlatDarkLaf.setup();
            TicketReceiptDialog dlg = new TicketReceiptDialog(
                    null,
                    12,
                    "Interstellar",
                    "2026-10-15 18:30:00",
                    "B4, B5",
                    300.00,
                    "CARD",
                    "CIN12-4821"
            );
            dlg.setVisible(true);
            System.exit(0);
        });
    }
}