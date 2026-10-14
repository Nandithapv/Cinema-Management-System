package models;

import java.util.regex.Pattern;

/**
 * Base abstract class representing a User in the Cinema Management System.
 * Demonstrates:
 *  - Abstraction: Cannot be instantiated directly; defines common user attributes.
 *  - Encapsulation: Private/protected fields accessed via validated getters and setters.
 *  - Inheritance: Subclassed by Customer and Admin.
 */
public abstract class User {
    protected int userId;
    protected String name;
    protected String email;
    protected String role;

    // RFC 5322 compliant simplified email regex
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public User(int userId, String name, String email, String role) {
        setUserId(userId);
        setName(name);
        setEmail(email);
        setRole(role);
    }

    public User(String name, String email, String role) {
        this(0, name, email, role);
    }

    // Encapsulation with Input Validation
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        if (userId < 0) {
            throw new IllegalArgumentException("User ID cannot be negative.");
        }
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format. Example: user@example.com");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if (role == null || (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("CUSTOMER"))) {
            throw new IllegalArgumentException("Role must be strictly 'ADMIN' or 'CUSTOMER'.");
        }
        this.role = role.toUpperCase();
    }

    // Abstract method demonstrating Polymorphism in Viva
    public abstract boolean isAdmin();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
