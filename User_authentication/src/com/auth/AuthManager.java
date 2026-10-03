package com.auth;

import java.util.HashMap;
import java.util.Map;

public class AuthManager {
    // Simulated database using a Map (Email -> User)
    private Map<String, User> userDatabase = new HashMap<>();

    // Register a new user
    public boolean register(int userId, String name, String email, String password, String role) {
        if (userDatabase.containsKey(email)) {
            System.out.println("Registration failed: Email already exists.");
            return false;
        }
        User newUser = new User(userId, name, email, password, role);
        userDatabase.put(email, newUser);
        System.out.println("Registration successful for: " + name);
        return true;
    }

    // Authenticate user login
    public User login(String email, String password) {
        User user = userDatabase.get(email);
        if (user != null && user.getPassword().equals(password)) {
            System.out.println("Login successful! Welcome back, " + user.getName());
            return user;
        }
        System.out.println("Login failed: Invalid email or password.");
        return null;
    }
}