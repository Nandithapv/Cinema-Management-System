package views.components;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import views.Theme;

/**
 * Reusable UI Card Component representing an individual movie in the catalog.
 * Built by Member 6 (Nanditha P V) following High Cohesion & Loose Coupling principles.
 */
public class MovieCardPanel extends JPanel {

    @FunctionalInterface
    public interface OnBookListener {
        void onBook(int movieId, String movieTitle);
    }

    private final int movieId;
    private final String title;
    private final String genre;
    private final int duration;
    private final String language;
    private final String posterPath;
    private final OnBookListener listener;

    public MovieCardPanel(int movieId, String title, String genre, int duration,
                          String language, String posterPath, OnBookListener listener) {
        this.movieId = movieId;
        this.title = title != null ? title : "Untitled";
        this.genre = genre != null ? genre : "General";
        this.duration = duration;
        this.language = language != null ? language : "English";
        this.posterPath = posterPath;
        this.listener = listener;

        initUI();
    }

    // Convenience constructor without poster path
    public MovieCardPanel(int movieId, String title, String genre, int duration,
                          String language, OnBookListener listener) {
        this(movieId, title, genre, duration, language, null, listener);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_CARD);
        setBorder(Theme.createCardBorder(10));
        setPreferredSize(new Dimension(220, 360));
        setMaximumSize(new Dimension(240, 380));

        // 1. Poster Display (Image or Stylized Fallback Art)
        JComponent posterComponent = createPosterComponent();
        add(posterComponent, BorderLayout.NORTH);

        // 2. Info Section (Title, Genre, Duration & Language)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(8, 2, 8, 2));

        JLabel lblTitle = new JLabel(truncate(title, 20));
        lblTitle.setFont(Theme.FONT_SUBTITLE);
        lblTitle.setForeground(Theme.TEXT_LIGHT);
        lblTitle.setToolTipText(title);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(lblTitle);

        infoPanel.add(Box.createVerticalStrut(4));

        JLabel lblGenre = new JLabel(genre);
        lblGenre.setFont(Theme.FONT_SMALL);
        lblGenre.setForeground(Theme.TEXT_MUTED);
        lblGenre.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(lblGenre);

        infoPanel.add(Box.createVerticalStrut(4));

        String meta = (duration > 0 ? duration + " min" : "N/A") + " • " + language;
        JLabel lblMeta = new JLabel(meta);
        lblMeta.setFont(Theme.FONT_SMALL_BOLD);
        lblMeta.setForeground(Theme.ACCENT_GOLD);
        lblMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(lblMeta);

        add(infoPanel, BorderLayout.CENTER);

        // 3. Action Section (Book Tickets Button)
        JButton btnBook = new JButton("Book Tickets");
        Theme.stylePrimaryButton(btnBook);
        btnBook.addActionListener(e -> {
            if (listener != null) {
                listener.onBook(movieId, title);
            }
        });

        add(btnBook, BorderLayout.SOUTH);
    }

    /**
     * Creates an image poster panel, or draws a sleek fallback banner with
     * movie clapper art if no poster image is found on disk.
     */
    private JComponent createPosterComponent() {
        final int posterWidth = 200;
        final int posterHeight = 220;

        if (posterPath != null && !posterPath.trim().isEmpty()) {
            File file = new File(posterPath);
            if (file.exists()) {
                try {
                    BufferedImage img = ImageIO.read(file);
                    if (img != null) {
                        Image scaled = img.getScaledInstance(posterWidth, posterHeight, Image.SCALE_SMOOTH);
                        JLabel lblImage = new JLabel(new ImageIcon(scaled));
                        lblImage.setPreferredSize(new Dimension(posterWidth, posterHeight));
                        return lblImage;
                    }
                } catch (Exception ignored) {
                    // Fall back to art component below
                }
            }
        }

        // Sleek Fallback Graphic
        JPanel fallback = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Subtle gradient background
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(42, 45, 62),
                    0, getHeight(), new Color(22, 23, 33)
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Cinema icon representation
                g2.setColor(Theme.ACCENT_GOLD);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
                FontMetrics fm = g2.getFontMetrics();
                String icon = "🎬";
                int x = (getWidth() - fm.stringWidth(icon)) / 2;
                int y = (getHeight() / 2) + 10;
                g2.drawString(icon, x, y);

                // Subtitle
                g2.setColor(Theme.TEXT_MUTED);
                g2.setFont(Theme.FONT_SMALL);
                FontMetrics fm2 = g2.getFontMetrics();
                String text = "NOW SHOWING";
                int tx = (getWidth() - fm2.stringWidth(text)) / 2;
                g2.drawString(text, tx, y + 36);

                g2.dispose();
            }
        };
        fallback.setPreferredSize(new Dimension(posterWidth, posterHeight));
        fallback.setOpaque(false);
        return fallback;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    // Getters
    public int getMovieId() { return movieId; }
    public String getTitle() { return title; }

    /** Standalone Preview Runner */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("MovieCardPanel Preview");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(750, 460);
            frame.setLocationRelativeTo(null);
            Theme.applyDarkTheme(frame);

            JPanel grid = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
            grid.setBackground(Theme.BG_DARK);

            grid.add(new MovieCardPanel(1, "Inception", "Sci-Fi / Action", 148, "English",
                (id, name) -> JOptionPane.showMessageDialog(frame, "Clicked Book for: " + name)));

            grid.add(new MovieCardPanel(2, "Interstellar", "Sci-Fi / Adventure", 169, "English",
                (id, name) -> JOptionPane.showMessageDialog(frame, "Clicked Book for: " + name)));

            grid.add(new MovieCardPanel(3, "The Dark Knight", "Action / Crime", 152, "English",
                (id, name) -> JOptionPane.showMessageDialog(frame, "Clicked Book for: " + name)));

            frame.add(new JScrollPane(grid));
            frame.setVisible(true);
        });
    }
}
