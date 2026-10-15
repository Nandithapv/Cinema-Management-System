package views;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Shared Theme and UI styling palette for the Cinema Management System.
 * Created by Member 6 (Nanditha P V) for consistent design across all views.
 */
public class Theme {

    // --- Color Palette ---
    public static final Color BG_DARK        = new Color(18, 18, 24);    // Deep dark cinema background
    public static final Color BG_HEADER      = new Color(24, 25, 36);    // Navigation / Top bar background
    public static final Color BG_CARD        = new Color(28, 30, 42);    // Surface / Card background
    public static final Color BG_CARD_HOVER  = new Color(38, 41, 58);    // Hover state for cards
    public static final Color BG_INPUT       = new Color(36, 38, 54);    // Form inputs

    public static final Color PRIMARY        = new Color(229, 9, 20);    // Crimson cinema accent
    public static final Color PRIMARY_HOVER  = new Color(246, 38, 48);   // Button hover
    public static final Color ACCENT_GOLD    = new Color(245, 197, 24);  // Golden amber for ratings & price
    public static final Color SUCCESS        = new Color(46, 204, 113);  // Available / Confirmed
    public static final Color DANGER         = new Color(231, 76, 60);   // Cancel / Error
    public static final Color INFO           = new Color(52, 152, 219);  // Information

    public static final Color TEXT_LIGHT     = new Color(245, 245, 245); // Primary text
    public static final Color TEXT_MUTED     = new Color(160, 163, 177); // Subtitles / details
    public static final Color BORDER_COLOR   = new Color(45, 48, 66);    // Subtle borders

    // --- Typography ---
    public static final Font FONT_BRAND      = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE   = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 11);

    // --- Component Styling Helpers ---

    /** Style a primary action button (e.g., Book Tickets, Login, Confirm) */
    public static void stylePrimaryButton(JButton button) {
        button.setFont(FONT_BODY_BOLD);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(PRIMARY_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(PRIMARY);
            }
        });
    }

    /** Style a secondary button (e.g., Refresh, Back, Browse) */
    public static void styleSecondaryButton(JButton button) {
        button.setFont(FONT_BODY);
        button.setBackground(BG_CARD);
        button.setForeground(TEXT_LIGHT);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(7, 14, 7, 14)
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BG_CARD_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BG_CARD);
            }
        });
    }

    /** Style a danger button (e.g., Cancel Booking, Delete) */
    public static void styleDangerButton(JButton button) {
        button.setFont(FONT_BODY);
        button.setBackground(new Color(60, 25, 28));
        button.setForeground(new Color(255, 120, 120));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
            new LineBorder(DANGER, 1, true),
            new EmptyBorder(7, 14, 7, 14)
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(DANGER);
                button.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(60, 25, 28));
                button.setForeground(new Color(255, 120, 120));
            }
        });
    }

    /** Create a standard card border with subtle outline and padding */
    public static Border createCardBorder(int padding) {
        return new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(padding, padding, padding, padding)
        );
    }

    /** Apply the dark background to any JFrame content pane */
    public static void applyDarkTheme(JFrame frame) {
        frame.getContentPane().setBackground(BG_DARK);
    }

    /** Standalone preview runner: Test this theme palette directly */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Theme Palette Preview — Cinema Management System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(520, 360);
            frame.setLocationRelativeTo(null);

            JPanel panel = new JPanel(new GridLayout(0, 1, 10, 10));
            panel.setBackground(BG_DARK);
            panel.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel title = new JLabel("🎬 Cinema Management UI Theme", SwingConstants.CENTER);
            title.setFont(FONT_BRAND);
            title.setForeground(TEXT_LIGHT);
            panel.add(title);

            JLabel subtitle = new JLabel("Consistent design across all team members' views", SwingConstants.CENTER);
            subtitle.setFont(FONT_BODY);
            subtitle.setForeground(TEXT_MUTED);
            panel.add(subtitle);

            JButton btnPrimary = new JButton("Primary Action (Book Show)");
            stylePrimaryButton(btnPrimary);
            panel.add(btnPrimary);

            JButton btnSecondary = new JButton("Secondary Action (My Bookings)");
            styleSecondaryButton(btnSecondary);
            panel.add(btnSecondary);

            JButton btnDanger = new JButton("Danger Action (Cancel Booking)");
            styleDangerButton(btnDanger);
            panel.add(btnDanger);

            frame.add(panel);
            frame.setVisible(true);
        });
    }
}
