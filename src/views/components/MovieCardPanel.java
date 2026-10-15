package views.components;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import views.Theme;

/**
 * Reusable Mobile-App-Style Movie Card Component.
 * Designed by Member 6 (Nanditha P V) following High Cohesion & Loose Coupling.
 * 
 * Inspired by modern mobile cinema apps:
 * - High-resolution rounded movie poster
 * - Floating rating badge (★ 8.8)
 * - Translucent genre chip & metadata
 * - Glowing border hover effect
 * - Direct click & "Book Tickets" action
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

    private boolean isHovered = false;

    public MovieCardPanel(int movieId, String title, String genre, int duration,
                          String language, String posterPath, OnBookListener listener) {
        this.movieId = movieId;
        this.title = title != null ? title : "Untitled";
        this.genre = genre != null ? genre : "General";
        this.duration = duration;
        this.language = language != null ? language : "English";
        this.posterPath = resolvePosterFile(title, posterPath);
        this.listener = listener;

        initUI();
    }

    public MovieCardPanel(int movieId, String title, String genre, int duration,
                          String language, OnBookListener listener) {
        this(movieId, title, genre, duration, language, null, listener);
    }

    /**
     * Smart poster file resolver: Checks given path first, then checks assets/posters/<title>.jpg
     */
    private static String resolvePosterFile(String title, String path) {
        if (path != null && !path.trim().isEmpty() && new File(path).exists()) {
            return path;
        }
        if (title != null) {
            String lower = title.toLowerCase();
            String matched = null;
            if (lower.contains("interstellar")) matched = "assets/posters/interstellar.jpg";
            else if (lower.contains("inception")) matched = "assets/posters/inception.jpg";
            else if (lower.contains("drishyam")) matched = "assets/posters/drishyam.jpg";
            else if (lower.contains("premam")) matched = "assets/posters/premam.jpg";
            else if (lower.contains("dark knight")) matched = "assets/posters/dark_knight.jpg";
            else if (lower.contains("avatar")) matched = "assets/posters/avatar.jpg";
            else if (lower.contains("spider") || lower.contains("spiderman")) matched = "assets/posters/spiderman.jpg";

            if (matched != null && new File(matched).exists()) {
                return matched;
            }
        }
        return path;
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(220, 390));
        setMaximumSize(new Dimension(240, 410));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Outer rounded card body
        JPanel innerCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background surface
                g2.setColor(isHovered ? Theme.BG_CARD_HOVER : Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                
                // Glowing border
                if (isHovered) {
                    g2.setColor(Theme.PRIMARY);
                    g2.setStroke(new BasicStroke(2.0f));
                } else {
                    g2.setColor(Theme.BORDER_COLOR);
                    g2.setStroke(new BasicStroke(1.0f));
                }
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 16, 16));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        innerCard.setOpaque(false);
        innerCard.setBorder(new EmptyBorder(10, 10, 12, 10));

        // 1. Poster Display with floating rating badge
        JComponent posterComponent = createPosterComponent();
        innerCard.add(posterComponent, BorderLayout.NORTH);

        // 2. Info Section (Title, Genre Pill, Duration & Language)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(10, 4, 10, 4));

        JLabel lblTitle = new JLabel(truncate(title, 20));
        lblTitle.setFont(Theme.FONT_SUBTITLE);
        lblTitle.setForeground(Theme.TEXT_LIGHT);
        lblTitle.setToolTipText(title);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(lblTitle);

        infoPanel.add(Box.createVerticalStrut(6));

        // Genre pill
        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblGenre = new JLabel(" " + genre + " ");
        lblGenre.setFont(Theme.FONT_SMALL_BOLD);
        lblGenre.setForeground(Theme.ACCENT_CYAN);
        lblGenre.setBackground(new Color(0, 245, 212, 35));
        lblGenre.setOpaque(true);
        lblGenre.setBorder(BorderFactory.createLineBorder(new Color(0, 245, 212, 80), 1, true));
        metaRow.add(lblGenre);

        infoPanel.add(metaRow);
        infoPanel.add(Box.createVerticalStrut(6));

        String meta = (duration > 0 ? "⏱ " + duration + " min" : "⏱ N/A") + "  •  " + language;
        JLabel lblMeta = new JLabel(meta);
        lblMeta.setFont(Theme.FONT_SMALL);
        lblMeta.setForeground(Theme.TEXT_MUTED);
        lblMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(lblMeta);

        innerCard.add(infoPanel, BorderLayout.CENTER);

        // 3. Action Section (Book Tickets Pill Button)
        JButton btnBook = new JButton("Book Tickets");
        Theme.stylePrimaryButton(btnBook);
        btnBook.addActionListener(e -> triggerBook());

        innerCard.add(btnBook, BorderLayout.SOUTH);

        // Hover listener on the whole card
        MouseAdapter hoverAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                innerCard.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                innerCard.repaint();
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                triggerBook();
            }
        };

        innerCard.addMouseListener(hoverAdapter);
        posterComponent.addMouseListener(hoverAdapter);
        infoPanel.addMouseListener(hoverAdapter);
        lblTitle.addMouseListener(hoverAdapter);

        add(innerCard, BorderLayout.CENTER);
    }

    private void triggerBook() {
        if (listener != null) {
            listener.onBook(movieId, title);
        }
    }

    /**
     * Creates an image poster panel with rounded corners and a floating rating chip.
     */
    private JComponent createPosterComponent() {
        final int posterWidth = 200;
        final int posterHeight = 220;

        BufferedImage loadedImage = null;
        if (posterPath != null && !posterPath.trim().isEmpty()) {
            File file = new File(posterPath);
            if (file.exists()) {
                try {
                    loadedImage = ImageIO.read(file);
                } catch (Exception ignored) {}
            }
        }

        final BufferedImage finalImg = loadedImage;
        final String ratingText = getMovieRating(title);

        JPanel posterPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                Shape clip = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setClip(clip);

                if (finalImg != null) {
                    g2.drawImage(finalImg, 0, 0, getWidth(), getHeight(), null);
                    // Subtle dark gradient at bottom of poster
                    GradientPaint grad = new GradientPaint(
                        0, getHeight() * 0.65f, new Color(0, 0, 0, 0),
                        0, getHeight(), new Color(11, 10, 22, 190)
                    );
                    g2.setPaint(grad);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    // Sleek Fallback Art
                    GradientPaint gp = new GradientPaint(
                        0, 0, new Color(42, 38, 75),
                        0, getHeight(), new Color(18, 16, 36)
                    );
                    g2.setPaint(gp);
                    g2.fill(clip);

                    g2.setColor(Theme.ACCENT_CYAN);
                    g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
                    FontMetrics fm = g2.getFontMetrics();
                    String icon = "🎬";
                    int x = (getWidth() - fm.stringWidth(icon)) / 2;
                    int y = (getHeight() / 2) + 10;
                    g2.drawString(icon, x, y);

                    g2.setColor(Theme.TEXT_MUTED);
                    g2.setFont(Theme.FONT_SMALL_BOLD);
                    FontMetrics fm2 = g2.getFontMetrics();
                    String text = "NOW SHOWING";
                    int tx = (getWidth() - fm2.stringWidth(text)) / 2;
                    g2.drawString(text, tx, y + 36);
                }

                // Reset clip for floating badges
                g2.setClip(null);

                // Draw floating Star Rating Badge at top-right
                if (ratingText != null) {
                    int badgeW = 56;
                    int badgeH = 22;
                    int bx = getWidth() - badgeW - 8;
                    int by = 8;
                    
                    g2.setColor(new Color(18, 16, 36, 210));
                    g2.fill(new RoundRectangle2D.Float(bx, by, badgeW, badgeH, 10, 10));
                    g2.setColor(Theme.ACCENT_GOLD);
                    g2.draw(new RoundRectangle2D.Float(bx, by, badgeW, badgeH, 10, 10));

                    g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    FontMetrics rFm = g2.getFontMetrics();
                    int rx = bx + (badgeW - rFm.stringWidth(ratingText)) / 2;
                    int ry = by + ((badgeH - rFm.getHeight()) / 2) + rFm.getAscent();
                    g2.drawString(ratingText, rx, ry);
                }

                g2.dispose();
            }
        };

        posterPanel.setPreferredSize(new Dimension(posterWidth, posterHeight));
        posterPanel.setOpaque(false);
        return posterPanel;
    }

    private static String getMovieRating(String movieTitle) {
        if (movieTitle == null) return "★ 8.0";
        String lower = movieTitle.toLowerCase();
        if (lower.contains("interstellar")) return "★ 8.7";
        if (lower.contains("inception")) return "★ 8.8";
        if (lower.contains("dark knight")) return "★ 9.0";
        if (lower.contains("drishyam")) return "★ 8.6";
        if (lower.contains("premam")) return "★ 8.3";
        if (lower.contains("avatar")) return "★ 7.8";
        if (lower.contains("spider")) return "★ 8.2";
        return "★ 8.5";
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
            frame.setSize(800, 480);
            frame.setLocationRelativeTo(null);
            Theme.applyDarkTheme(frame);

            JPanel grid = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
            grid.setBackground(Theme.BG_DARK);

            grid.add(new MovieCardPanel(1, "Interstellar", "Sci-Fi", 169, "English",
                (id, name) -> JOptionPane.showMessageDialog(frame, "Clicked Book for: " + name)));

            grid.add(new MovieCardPanel(2, "Inception", "Sci-Fi", 148, "English",
                (id, name) -> JOptionPane.showMessageDialog(frame, "Clicked Book for: " + name)));

            grid.add(new MovieCardPanel(3, "The Dark Knight", "Action", 152, "English",
                (id, name) -> JOptionPane.showMessageDialog(frame, "Clicked Book for: " + name)));

            frame.add(new JScrollPane(grid));
            frame.setVisible(true);
        });
    }
}
