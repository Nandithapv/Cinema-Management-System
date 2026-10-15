package views;

import dao.UserDAO;
import models.Customer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

/**
 * RegisterFrame provides the customer account creation window.
 * Constructor: RegisterFrame() (no arguments, as required by team rules).
 * Features:
 *  - Full Name, Email, Password, and Confirm Password fields
 *  - Real-time client-side input validation (email format, password min length 8)
 *  - Handles duplicate email constraint cleanly
 *  - Seamless redirection back to LoginFrame
 */
public class RegisterFrame extends JFrame {

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton registerButton;
    private JButton backToLoginButton;
    private UserDAO userDAO;

    public RegisterFrame() {
        this.userDAO = new UserDAO();
        initComponents();
    }

    private void initComponents() {
        setTitle("Cinema Management System — Customer Registration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        Color primaryColor = new Color(76, 175, 80);      // Accent Green for signup
        Color linkColor = new Color(33, 150, 243);
        Color bgColor = new Color(245, 247, 250);
        Color cardColor = Color.WHITE;
        Font titleFont = new Font("Segoe UI", Font.BOLD, 22);
        Font labelFont = new Font("Segoe UI", Font.PLAIN, 14);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 14);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(cardColor);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(20, 25, 20, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 5, 6, 5);

        // Header Title
        JLabel titleLabel = new JLabel("✨ Create Account", SwingConstants.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(new Color(33, 37, 41));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        cardPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Join us to book your favorite cinema tickets", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.GRAY);
        gbc.gridy = 1;
        cardPanel.add(subtitleLabel, gbc);

        // Full Name
        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setFont(labelFont);
        gbc.gridy = 2;
        cardPanel.add(nameLabel, gbc);

        nameField = new JTextField();
        nameField.setFont(inputFont);
        nameField.setPreferredSize(new Dimension(340, 34));
        gbc.gridy = 3;
        cardPanel.add(nameField, gbc);

        // Email
        JLabel emailLabel = new JLabel("Email Address:");
        emailLabel.setFont(labelFont);
        gbc.gridy = 4;
        cardPanel.add(emailLabel, gbc);

        emailField = new JTextField();
        emailField.setFont(inputFont);
        emailField.setPreferredSize(new Dimension(340, 34));
        gbc.gridy = 5;
        cardPanel.add(emailField, gbc);

        // Password
        JLabel passwordLabel = new JLabel("Password (min 8 chars):");
        passwordLabel.setFont(labelFont);
        gbc.gridy = 6;
        cardPanel.add(passwordLabel, gbc);

        passwordField = new JPasswordField();
        passwordField.setFont(inputFont);
        passwordField.setPreferredSize(new Dimension(340, 34));
        gbc.gridy = 7;
        cardPanel.add(passwordField, gbc);

        // Confirm Password
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(labelFont);
        gbc.gridy = 8;
        cardPanel.add(confirmLabel, gbc);

        confirmPasswordField = new JPasswordField();
        confirmPasswordField.setFont(inputFont);
        confirmPasswordField.setPreferredSize(new Dimension(340, 34));
        gbc.gridy = 9;
        cardPanel.add(confirmPasswordField, gbc);

        // Register Button
        registerButton = new JButton("Create Account");
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerButton.setBackground(primaryColor);
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setPreferredSize(new Dimension(340, 38));
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridy = 10;
        gbc.insets = new Insets(14, 5, 6, 5);
        cardPanel.add(registerButton, gbc);

        // Already have an account? Login link
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(cardColor);
        JLabel haveAccountLabel = new JLabel("Already have an account?");
        haveAccountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        backToLoginButton = new JButton("Log In");
        backToLoginButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        backToLoginButton.setForeground(linkColor);
        backToLoginButton.setBorderPainted(false);
        backToLoginButton.setContentAreaFilled(false);
        backToLoginButton.setFocusPainted(false);
        backToLoginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        bottomPanel.add(haveAccountLabel);
        bottomPanel.add(backToLoginButton);

        gbc.gridy = 11;
        gbc.insets = new Insets(2, 5, 2, 5);
        cardPanel.add(bottomPanel, gbc);

        mainPanel.add(cardPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Event Listeners
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRegistration();
            }
        });

        backToLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openLoginFrame();
            }
        });
    }

    private void handleRegistration() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // Basic presence checks
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "All fields are required. Please fill in all information.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Password matching
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "Passwords do not match. Please re-enter.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Minimum 8 characters check
        if (!UserDAO.isValidPassword(password)) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters long.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Customer model encapsulates validation on setters
            Customer customer = new Customer(name, email);

            boolean success = userDAO.register(customer, password);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Registration successful! You can now log in.",
                        "Account Created",
                        JOptionPane.INFORMATION_MESSAGE);
                openLoginFrame();
            }
        } catch (IllegalArgumentException ex) {
            // Catches invalid email regex or duplicate email warning
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Registration Failed",
                    JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error: " + ex.getMessage(),
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openLoginFrame() {
        new LoginFrame().setVisible(true);
        dispose();
    }

    // Direct runnable entrypoint for testing RegisterFrame
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {}
                new RegisterFrame().setVisible(true);
            }
        });
    }
}
