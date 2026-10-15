package views;

import services.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Admin Portal Dashboard.
 * Created by Member 3 (Namitha) & Updated by Member 6 (Nanditha) for session logout and window closing.
 */
public class AdminDashboardFrame extends JFrame {

    public AdminDashboardFrame() {
        setTitle("Cinema Admin Dashboard — Operations Management");
        setSize(960, 580);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());

        // Header Panel with title and Logout button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(33, 37, 41));
        headerPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel titleLabel = new JLabel("🎬 Admin Portal — Cineva", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton btnLogout = new JButton("Logout");
        Theme.styleDangerButton(btnLogout);
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to log out?", "Logout",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                SessionManager.logoutUser();
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
        headerPanel.add(btnLogout, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        AdminShowView showView = new AdminShowView();

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_BODY_BOLD);
        tabs.addTab("Movies Catalog", new AdminMovieDashboardView());
        tabs.addTab("Show Scheduling", showView);
        tabs.addTab("Screens & Capacity", new AdminScreenView());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == showView) showView.reloadChoices();
        });
        add(tabs, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminDashboardFrame().setVisible(true));
    }
}