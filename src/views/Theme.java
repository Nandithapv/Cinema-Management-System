package views;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Shared Theme and UI styling palette for the Cinema Management System.
 * Crafted by Member 6 (Nanditha P V) inspired by modern cinema app UI concepts:
 * - Deep luxury midnight-indigo backgrounds
 * - Electric violet & cyan cyber accents
 * - Radiant amber for ratings & badges
 * - High-contrast legible headings and typography
 */
public class Theme {

    // --- Color Palette (Pinterest / Mobile Cinema App inspired) ---
    public static final Color BG_DARK        = new Color(11, 10, 22);     // Deep midnight cinema background
    public static final Color BG_HEADER      = new Color(18, 16, 36);     // Top navigation bar background
    public static final Color BG_CARD        = new Color(26, 24, 50);     // Elevated card surface
    public static final Color BG_CARD_HOVER  = new Color(38, 35, 72);     // Hover state for cards
    public static final Color BG_INPUT       = new Color(22, 20, 42);     // Form input background
    public static final Color BG_PILL        = new Color(32, 29, 60);     // Category filter pill background

    // Vibrant Accents
    public static final Color PRIMARY        = new Color(127, 86, 217);   // Electric Violet / Purple
    public static final Color PRIMARY_HOVER  = new Color(155, 110, 245);  // Violet hover
    public static final Color ACCENT_CYAN    = new Color(0, 245, 212);    // Neon Cyber Cyan (high-visibility)
    public static final Color ACCENT_MAGENTA = new Color(255, 42, 109);   // Neon Cinema Pink / Magenta
    public static final Color ACCENT_GOLD    = new Color(251, 191, 36);   // Radiant amber for ratings ★
    public static final Color SUCCESS        = new Color(34, 197, 94);    // Green (Available / Confirmed)
    public static final Color DANGER         = new Color(239, 68, 68);    // Red (Error / Cancel)
    public static final Color INFO           = new Color(56, 189, 248);   // Sky Blue

    // High-Contrast Typography & Text Colors
    public static final Color TEXT_LIGHT     = new Color(255, 255, 255);  // Crisp white for primary headings
    public static final Color TEXT_HEADINGS  = new Color(243, 244, 246);  // Clear, readable heading color
    public static final Color TEXT_MUTED     = new Color(165, 166, 194);  // Lavender-gray for subtitles/metadata
    public static final Color BORDER_COLOR   = new Color(52, 48, 88);     // Subtle surface borders
    public static final Color BORDER_GLOW    = new Color(127, 86, 217, 180); // Glowing violet border

    // --- Typography ---
    public static final Font FONT_BRAND      = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE   = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 11);

    // --- Component Styling Helpers ---

    /** Style a primary action button (e.g., Book Tickets, Confirm, Select Seats) */
    public static void stylePrimaryButton(JButton button) {
        button.setFont(FONT_BODY_BOLD);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(9, 18, 9, 18));

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

    /** Style a secondary button (e.g., My Bookings, Refresh, Back) with high contrast */
    public static void styleSecondaryButton(JButton button) {
        button.setFont(FONT_BODY_BOLD);
        button.setBackground(new Color(32, 28, 62));
        button.setForeground(ACCENT_CYAN); // Neon cyan ensures it is clearly visible!
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
            new LineBorder(new Color(90, 75, 145), 1, true),
            new EmptyBorder(8, 16, 8, 16)
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(46, 40, 88));
                button.setForeground(Color.WHITE);
                button.setBorder(new CompoundBorder(
                    new LineBorder(PRIMARY_HOVER, 1, true),
                    new EmptyBorder(8, 16, 8, 16)
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(32, 28, 62));
                button.setForeground(ACCENT_CYAN);
                button.setBorder(new CompoundBorder(
                    new LineBorder(new Color(90, 75, 145), 1, true),
                    new EmptyBorder(8, 16, 8, 16)
                ));
            }
        });
    }

    /** Style a danger button (e.g., Cancel Booking, Logout) */
    public static void styleDangerButton(JButton button) {
        button.setFont(FONT_BODY_BOLD);
        button.setBackground(new Color(55, 20, 32));
        button.setForeground(new Color(255, 130, 150));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
            new LineBorder(new Color(180, 45, 75), 1, true),
            new EmptyBorder(8, 14, 8, 14)
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ACCENT_MAGENTA);
                button.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(55, 20, 32));
                button.setForeground(new Color(255, 130, 150));
            }
        });
    }

    /** Create a standard card border with rounded outline and padding */
    public static Border createCardBorder(int padding) {
        return new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(padding, padding, padding, padding)
        );
    }

    /** Apply the dark midnight background to any JFrame content pane */
    public static void applyDarkTheme(JFrame frame) {
        frame.getContentPane().setBackground(BG_DARK);
    }

    /** Standalone preview runner */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Theme Palette Preview — Cinema Management System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(520, 380);
            frame.setLocationRelativeTo(null);
            applyDarkTheme(frame);

            JPanel panel = new JPanel(new GridLayout(0, 1, 12, 12));
            panel.setBackground(BG_DARK);
            panel.setBorder(new EmptyBorder(24, 24, 24, 24));

            JLabel title = new JLabel("🍿 Cinema Management UI Theme", SwingConstants.CENTER);
            title.setFont(FONT_BRAND);
            title.setForeground(TEXT_LIGHT);
            panel.add(title);

            JLabel subtitle = new JLabel("Mobile App Inspired Aesthetic", SwingConstants.CENTER);
            subtitle.setFont(FONT_BODY);
            subtitle.setForeground(TEXT_MUTED);
            panel.add(subtitle);

            JButton btnPrimary = new JButton("Primary Action (Book Show)");
            stylePrimaryButton(btnPrimary);
            panel.add(btnPrimary);

            JButton btnSecondary = new JButton("🎟️ My Bookings");
            styleSecondaryButton(btnSecondary);
            panel.add(btnSecondary);

            JButton btnDanger = new JButton("Logout / Cancel");
            styleDangerButton(btnDanger);
            panel.add(btnDanger);

            frame.add(panel);
            frame.setVisible(true);
        });
    }
}
