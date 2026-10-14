package dao;

import config.DatabaseConnection;
import models.Admin;
import models.Customer;
import models.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

/**
 * Data Access Object for User entities.
 * Handles database queries for customer registration and user authentication.
 * 
 * Rules Adherence:
 *  - Connection is ONE shared Singleton; never closed here.
 *  - PreparedStatements and ResultSets are closed in try-with-resources.
 *  - Passwords are SHA-256 hashed into lowercase hex string.
 *  - User instance does not store password in memory.
 */
public class UserDAO {

    public UserDAO() {
        // Default constructor
    }

    /**
     * Hashes plain text password using SHA-256 to match MySQL SHA2(str, 256).
     * @param plainPassword Raw password string
     * @return 64-character lowercase hexadecimal hash string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cryptographic error hashing password: " + e.getMessage(), e);
        }
    }

    /**
     * Validates password strength (minimum 8 characters).
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    /**
     * Authenticates a user with email and plain password.
     * @param email User email
     * @param plainPassword User plain password
     * @return User (Admin or Customer instance) if credentials match; null otherwise.
     * @throws SQLException If database access fails
     */
    public User login(String email, String plainPassword) throws SQLException {
        if (email == null || plainPassword == null || email.trim().isEmpty() || plainPassword.isEmpty()) {
            return null;
        }

        String hashedPassword = hashPassword(plainPassword);
        String sql = "SELECT user_id, name, email, role FROM users WHERE email = ? AND password = ?";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            throw new SQLException("Database connection is not available.");
        }

        // Only PreparedStatement and ResultSet in try-with-resources; do NOT close conn!
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim().toLowerCase());
            stmt.setString(2, hashedPassword);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String name = rs.getString("name");
                    String userEmail = rs.getString("email");
                    String role = rs.getString("role");

                    // Instantiate specific polymorphic type without storing password
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        return new Admin(userId, name, userEmail);
                    } else {
                        return new Customer(userId, name, userEmail);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Registers a new Customer in the database.
     * @param customer Customer model with name and email
     * @param plainPassword Raw password (minimum 8 characters)
     * @return true if inserted successfully
     * @throws IllegalArgumentException If password too short or email already exists
     * @throws SQLException On database errors
     */
    public boolean register(Customer customer, String plainPassword) throws SQLException {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null.");
        }
        if (!isValidPassword(plainPassword)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }

        String hashedPassword = hashPassword(plainPassword);
        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'CUSTOMER')";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            throw new SQLException("Database connection is not available.");
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getEmail());
            stmt.setString(3, hashedPassword);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        customer.setUserId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("Email already exists. Please choose a different email or log in.");
        } catch (SQLException e) {
            // MySQL error code 1062 is duplicate entry for unique key
            if (e.getErrorCode() == 1062) {
                throw new IllegalArgumentException("Email already exists. Please choose a different email or log in.");
            }
            throw e;
        }
        return false;
    }

    /**
     * Checks if an email is already present in the database.
     */
    public boolean isEmailRegistered(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT user_id FROM users WHERE email = ?";
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            throw new SQLException("Database connection is not available.");
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
