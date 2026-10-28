package views;

import dao.UserDAO;
import models.Customer;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

import static views.UI.*;

/**
 * RegisterFrame provides the customer account creation window.
 * Constructor: RegisterFrame() (no arguments, as required by team rules).
 * Features:
 *  - Full Name, Email, Password, and Confirm Password fields
 *  - Real-time client-side input validation (email format, password min length 8)
 *  - Handles duplicate email constraint cleanly
 *  - Seamless redirection back to LoginFrame
 *
 * UI: same dark card + animated mosaic background as LoginFrame (shared pieces live in UI.java).
 */
public class RegisterFrame extends JFrame {

    private static final int CONTENT_WIDTH = 360;

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
        setResizable(false);

        // ----- Page background: animated mosaic wave -----
        MosaicPanel page = new MosaicPanel();
        page.setLayout(new GridBagLayout());
        page.setBorder(new EmptyBorder(16, 30, 16, 30));

        // ----- Card -----
        RoundPanel card = new RoundPanel(28, CARD, CARD_BORDER);
        card.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(26, 40, 22, 40));

        // CINEVA pill
        RoundPanel pill = new RoundPanel(14, new Color(24, 27, 33), FIELD_BORDER);
        pill.setLayout(new FlowLayout(FlowLayout.CENTER, 14, 3));
        pill.add(new TrackedLabel("CINEVA", font(Font.BOLD, 12), new Color(190, 194, 203), 3));
        pill.setMaximumSize(pill.getPreferredSize());
        content.add(centered(pill));
        content.add(Box.createVerticalStrut(12));

        // Title + subtitle
        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(font(Font.BOLD, 28));
        titleLabel.setForeground(TEXT);
        content.add(centered(titleLabel));
        content.add(Box.createVerticalStrut(4));

        JLabel subtitleLabel = new JLabel("Join us to book your favorite cinema tickets");
        subtitleLabel.setFont(font(Font.PLAIN, 14));
        subtitleLabel.setForeground(MUTED);
        content.add(centered(subtitleLabel));
        content.add(Box.createVerticalStrut(20));

        // Full Name
        content.add(centered(labelRow("Full Name:", null, CONTENT_WIDTH)));
        content.add(Box.createVerticalStrut(6));
        nameField = new JTextField();
        nameField.putClientProperty("JTextField.placeholderText", "Full name");
        content.add(centered(buildField(IconType.USER, nameField, null, CONTENT_WIDTH)));
        content.add(Box.createVerticalStrut(12));

        // Email
        content.add(centered(labelRow("Email Address:", null, CONTENT_WIDTH)));
        content.add(Box.createVerticalStrut(6));
        emailField = new JTextField();
        emailField.putClientProperty("JTextField.placeholderText", "Email address");
        content.add(centered(buildField(IconType.MAIL, emailField, null, CONTENT_WIDTH)));
        content.add(Box.createVerticalStrut(12));

        // Password
        content.add(centered(labelRow("Password (min 8 chars):", null, CONTENT_WIDTH)));
        content.add(Box.createVerticalStrut(6));
        passwordField = new JPasswordField();
        passwordField.putClientProperty("JTextField.placeholderText", "Password");
        content.add(centered(buildField(IconType.LOCK, passwordField, eyeFor(passwordField), CONTENT_WIDTH)));
        content.add(Box.createVerticalStrut(12));

        // Confirm Password
        content.add(centered(labelRow("Confirm Password:", null, CONTENT_WIDTH)));
        content.add(Box.createVerticalStrut(6));
        confirmPasswordField = new JPasswordField();
        confirmPasswordField.putClientProperty("JTextField.placeholderText", "Confirm password");
        content.add(centered(buildField(IconType.LOCK, confirmPasswordField, eyeFor(confirmPasswordField), CONTENT_WIDTH)));
        content.add(Box.createVerticalStrut(20));

        // Register button
        registerButton = new PrimaryButton("Create Account");
        fixSize(registerButton, CONTENT_WIDTH, 50);
        content.add(centered(registerButton));
        content.add(Box.createVerticalStrut(16));

        // Divider
        JPanel divider = new JPanel();
        divider.setBackground(new Color(34, 37, 44));
        fixSize(divider, CONTENT_WIDTH, 1);
        content.add(centered(divider));
        content.add(Box.createVerticalStrut(14));

        // Already have an account? Log In
        JPanel loginRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        loginRow.setOpaque(false);
        JLabel haveAccountLabel = new JLabel("Already have an account?");
        haveAccountLabel.setFont(font(Font.PLAIN, 15));
        haveAccountLabel.setForeground(MUTED);
        backToLoginButton = linkButton("Log In", font(Font.BOLD, 15), TEXT, true);
        loginRow.add(haveAccountLabel);
        loginRow.add(backToLoginButton);
        fixSize(loginRow, CONTENT_WIDTH, 28);
        content.add(centered(loginRow));

        card.add(content, BorderLayout.CENTER);

        // Footer strip
        JPanel footer = new FooterPanel(FOOTER_BG, new Color(30, 33, 40), 28);
        footer.setLayout(new BorderLayout());
        footer.setBorder(new EmptyBorder(12, 24, 12, 24));
        JPanel footerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        footerLeft.setOpaque(false);
        footerLeft.add(new DotComponent());
        JLabel nodeLabel = new JLabel("Customer Registration Portal Online");
        nodeLabel.setFont(font(Font.PLAIN, 12));
        nodeLabel.setForeground(MUTED);
        footerLeft.add(nodeLabel);
        JLabel versionLabel = new JLabel("v4.8.2 Monolith");
        versionLabel.setFont(font(Font.PLAIN, 12));
        versionLabel.setForeground(MUTED);
        footer.add(footerLeft, BorderLayout.WEST);
        footer.add(versionLabel, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);

        page.add(card);

        setContentPane(page);
        pack();
        setLocationRelativeTo(null);

        // ----- Listeners -----
        registerButton.addActionListener(e -> handleRegistration());
        backToLoginButton.addActionListener(e -> openLoginFrame());

        // Enter moves to the next field; Enter on the last field submits
        nameField.addActionListener(e -> emailField.requestFocusInWindow());
        emailField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> confirmPasswordField.requestFocusInWindow());
        confirmPasswordField.addActionListener(e -> handleRegistration());
        getRootPane().setDefaultButton(registerButton);
    }

    /** Eye button that shows / hides the characters of the given password field. */
    private EyeButton eyeFor(final JPasswordField field) {
        final char defaultEcho = field.getEchoChar();
        final EyeButton eye = new EyeButton();
        eye.addActionListener(e -> {
            boolean reveal = field.getEchoChar() != (char) 0;
            field.setEchoChar(reveal ? (char) 0 : defaultEcho);
            eye.setRevealed(reveal);
        });
        return eye;
    }

    // =====================================================================
    //  Registration logic (unchanged)
    // =====================================================================
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
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }
}