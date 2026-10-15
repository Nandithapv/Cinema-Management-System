package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton Database Connection Manager.
 * Designed by Member 1 (Nandana) & Enhanced for Team Integration (Member 6: Nanditha).
 *
 * Rules:
 * - ONE shared Singleton connection across the entire application.
 * - Reconnects automatically if null or closed.
 * - Reads credentials from db.properties (ignored by git), with resilient fallbacks.
 */
public class DatabaseConnection {

    private static Connection connection;

    private DatabaseConnection() {
        // Prevent instantiation (Singleton Pattern)
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                String url = "jdbc:mysql://localhost:3306/CinemaManagement?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                String user = "root";
                String password = "nandana@2007"; // default fallback

                File propsFile = new File("db.properties");
                if (propsFile.exists()) {
                    Properties props = new Properties();
                    try (FileInputStream fis = new FileInputStream(propsFile)) {
                        props.load(fis);
                        if (props.getProperty("db.url") != null) {
                            url = props.getProperty("db.url");
                        }
                        if (props.getProperty("db.user") != null) {
                            user = props.getProperty("db.user");
                        } else if (props.getProperty("db.username") != null) {
                            user = props.getProperty("db.username");
                        }
                        if (props.getProperty("db.password") != null) {
                            password = props.getProperty("db.password");
                        }
                    } catch (IOException e) {
                        System.err.println("[DatabaseConnection] Error reading db.properties: " + e.getMessage());
                    }
                }

                connection = DriverManager.getConnection(url, user, password);
                System.out.println("[DatabaseConnection] Database connected successfully to: " + url);
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Database connection failed: " + e.getMessage());
        }

        return connection;
    }
}
