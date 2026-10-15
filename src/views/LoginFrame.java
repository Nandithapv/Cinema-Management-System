package views;

import dao.UserDAO;
import models.Admin;
import models.Customer;
import models.User;
import services.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

/**
 * LoginFrame provides the primary authentication window.
 * Constructor: LoginFrame() (no arguments, as required by team rules).
 * Features:
 *  - Secure JPasswordField input
 *  - SHA-256 database authentication via UserDAO
 *  - SessionManager tracking
 *  - Dynamic role routing:
 *      Admin    -> AdminDashboardFrame
 *      Customer -> CustomerHomeFrame
 */
public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private UserDAO userDAO;

    // Default no-argument constructor
    public LoginFrame() {
        this.userDAO = new UserDAO();
        initComponents();
    }

    private void initComponents() {
        setTitle("Cinema Management System — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 420);
        setLocationRelativeTo(null); // Center on screen
        setResizable(false);

        // Styling palette
        Color primaryColor = new Color(33, 150, 243);     // Accent Blue
        Color bgColor = new Color(245, 247, 250);          // Soft Light Gray
        Color cardColor = Color.WHITE;
        Font titleFont = new Font("Segoe UI", Font.BOLD, 22);
        Font labelFont = new Font("Segoe UI", Font.PLAIN, 14);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 14);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(bgColor);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Card Panel
        JPanel cardPanel = new JPanel();
        cardPanel.setBackground(cardColor);
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(20, 25, 20, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);

        // Header / Title
        JLabel titleLabel = new JLabel("🎬 Cinema Login", SwingConstants.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(new Color(33, 37, 41));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        cardPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Sign in to your account", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.GRAY);
        gbc.gridy = 1;
        cardPanel.add(subtitleLabel, gbc);

        // Email Label & Field
        JLabel emailLabel = new JLabel("Email Address:");
        emailLabel.setFont(labelFont);
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        cardPanel.add(emailLabel, gbc);

        emailField = new JTextField();
        emailField.setFont(inputFont);
        emailField.setPreferredSize(new Dimension(320, 36));
        gbc.gridy = 3;
        cardPanel.add(emailField, gbc);

        // Password Label & Field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(labelFont);
        gbc.gridy = 4;
        cardPanel.add(passwordLabel, gbc);

        passwordField = new JPasswordField();
        passwordField.setFont(inputFont);
        passwordField.setPreferredSize(new Dimension(320, 36));
        gbc.gridy = 5;
        cardPanel.add(passwordField, gbc);

        // Login Button
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(primaryColor);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setPreferredSize(new Dimension(320, 38));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridy = 6;
        gbc.insets = new Insets(16, 5, 8, 5);
        cardPanel.add(loginButton, gbc);

        // Register Link / Button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(cardColor);
        JLabel noAccountLabel = new JLabel("Don't have an account?");
        noAccountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        registerButton = new JButton("Sign Up");
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        registerButton.setForeground(primaryColor);
        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setFocusPainted(false);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        bottomPanel.add(noAccountLabel);
        bottomPanel.add(registerButton);

        gbc.gridy = 7;
        gbc.insets = new Insets(4, 5, 4, 5);
        cardPanel.add(bottomPanel, gbc);

        mainPanel.add(cardPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Event Listeners
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        // Press Enter to submit
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openRegisterFrame();
            }
        });
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both email and password.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = userDAO.login(email, password);
            if (user != null) {
                // Store in memory session
                SessionManager.loginUser(user);

                // Redirection Logic
                routeUser(user);
            } else {
                // Team requirement: generic error message
                JOptionPane.showMessageDialog(this,
                        "Invalid email or password.",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database connection error: " + ex.getMessage(),
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Redirection logic:
     *  - Admin    -> AdminDashboardFrame
     *  - Customer -> CustomerHomeFrame
     * Uses reflective fallback so LoginFrame works seamlessly both standalone and integrated.
     */
    private void routeUser(User user) {
        if (user instanceof Admin) {
            try {
                Class<?> adminClass = Class.forName("views.AdminDashboardFrame");
                JFrame adminFrame = (JFrame) adminClass.getDeclaredConstructor().newInstance();
                adminFrame.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Welcome Admin: " + user.getName() + "!\n(AdminDashboardFrame will open once Namitha's module is merged)",
                        "Admin Login Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (user instanceof Customer) {
            try {
                Class<?> customerClass = Class.forName("views.CustomerHomeFrame");
                JFrame customerFrame = (JFrame) customerClass.getDeclaredConstructor().newInstance();
                customerFrame.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Welcome: " + user.getName() + "!\n(CustomerHomeFrame will open once Nanditha's module is merged)",
                        "Customer Login Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
        dispose(); // Close login window
    }

    private void openRegisterFrame() {
        new RegisterFrame().setVisible(true);
        dispose();
    }

    // Direct runnable entrypoint for testing LoginFrame
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {}
                new LoginFrame().setVisible(true);
            }
        });
    }
}
