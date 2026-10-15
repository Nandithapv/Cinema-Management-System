import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import views.LoginFrame;

/**
 * Main Application Entry Point.
 * Created & Integrated by Member 6 (Nanditha P V: Master Integration & QA Lead).
 * 
 * Sets the system Look & Feel, initializes configuration,
 * and launches LoginFrame to begin the application lifecycle.
 */
public class Main {

    public static void main(String[] args) {
        // Enable anti-aliased font rendering across all Swing controls
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Set modern system Look & Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Note: Using default Look & Feel: " + e.getMessage());
        }

        // Launch application on Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            System.out.println("==================================================");
            System.out.println("  🎬 CineMagic — Cinema Management System");
            System.out.println("  Integration & UI Module by Nanditha P V");
            System.out.println("==================================================");

            new LoginFrame().setVisible(true);
        });
    }
}
