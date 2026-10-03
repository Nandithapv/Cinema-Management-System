package views;

import javax.swing.*;

public class AdminDashboardFrame extends JFrame {

    public AdminDashboardFrame() {
        setTitle("Cinema Admin Dashboard");
        setSize(900, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        AdminShowView showView = new AdminShowView();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Movies", new AdminMovieDashboardView());
        tabs.addTab("Shows", showView);
        tabs.addTab("Screens", new AdminScreenView());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == showView) showView.reloadChoices();
        });
        add(tabs);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminDashboardFrame().setVisible(true));
    }
}