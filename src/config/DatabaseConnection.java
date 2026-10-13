package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton Database Connection manager.
 * Reads credentials from db.properties (or falls back to defaults).
 * Provides a single shared connection across the entire application.
 */
public class DatabaseConnection {
    private static Connection connection = null;

    private DatabaseConnection() {
        // Singleton - prevent direct instantiation
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Properties props = new Properties();

                // Look for db.properties in the working directory
                File propFile = new File("db.properties");
                if (propFile.exists()) {
                    try (InputStream input = new FileInputStream(propFile)) {
                        props.load(input);
                    }
                } else {
                    // Fall back to classpath lookup
                    InputStream cpStream = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties");
                    if (cpStream != null) {
                        try (cpStream) {
                            props.load(cpStream);
                        }
                    }
                }

                String url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/cinema_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
                String user = props.getProperty("db.user", "root");
                String password = props.getProperty("db.password", "admin123");

                connection = DriverManager.getConnection(url, user, password);
                System.out.println("[DatabaseConnection] Connected to MySQL database.");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Connection failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[DatabaseConnection] Error loading properties: " + e.getMessage());
        }
        return connection;
    }
}
