package com.auth;

public class SessionManager {
    private static User currentLoggedInUser = null;

    // Start a session
    public static void loginUser(User user) {
        currentLoggedInUser = user;
        System.out.println("Session started for user: " + user.getName() + " [" + user.getRole() + "]");
    }

    // End a session
    public static void logoutUser() {
        if (currentLoggedInUser != null) {
            System.out.println("Session ended for user: " + currentLoggedInUser.getName());
            currentLoggedInUser = null;
        } else {
            System.out.println("No active session to logout.");
        }
    }

    // Check who is logged in
    public static User getCurrentUser() {
        return currentLoggedInUser;
    }

    // Check if someone is logged in
    public static boolean isLoggedIn() {
        return currentLoggedInUser != null;
    }
}