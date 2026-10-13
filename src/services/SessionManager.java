package services;

import models.User;

/**
 * SessionManager stores the currently authenticated user in memory.
 * Preserves the exact team method signatures:
 *   - loginUser(User user)
 *   - logoutUser()
 *   - getCurrentUser()
 *   - isLoggedIn()
 */
public class SessionManager {
    private static User currentLoggedInUser = null;

    private SessionManager() {
        // Prevent instantiation
    }

    /**
     * Stores the user in memory upon successful login.
     */
    public static synchronized void loginUser(User user) {
        currentLoggedInUser = user;
        if (user != null) {
            System.out.println("[SessionManager] User logged in: " + user.getName() + " [" + user.getRole() + "]");
        }
    }

    /**
     * Clears the active session upon logout.
     */
    public static synchronized void logoutUser() {
        if (currentLoggedInUser != null) {
            System.out.println("[SessionManager] User logged out: " + currentLoggedInUser.getName());
            currentLoggedInUser = null;
        } else {
            System.out.println("[SessionManager] No active session to log out.");
        }
    }

    /**
     * Returns the currently logged in user, or null if no active session.
     */
    public static synchronized User getCurrentUser() {
        return currentLoggedInUser;
    }

    /**
     * Checks if a user is currently logged in.
     */
    public static synchronized boolean isLoggedIn() {
        return currentLoggedInUser != null;
    }
}
